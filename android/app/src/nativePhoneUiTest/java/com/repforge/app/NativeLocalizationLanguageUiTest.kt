package com.repforge.app

import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real MainActivity and persistent settings on a wiped, debuggable emulator; no fake screens. */
@RunWith(AndroidJUnit4::class)
class NativeLocalizationLanguageUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario)

    private fun launch() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) { nodesWithTag("navigation_home") }
    }
    private fun nodesWithTag(tag: String) =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun textVisible(text: String) =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { textVisible(text) }
        compose.onNodeWithText(text).assertExists()
    }
    private fun selectLanguageInSettings(language: String, expectedHeading: String) {
        // No language picker exists in SettingsScreen at this SHA. Invoke its real
        // DataStore setting path while Settings remains rendered, not a mock locale.
        fixture.selectLanguage(language)
        compose.waitUntil(15_000) { fixture.selectedLanguage() == language && textVisible(expectedHeading) }
        compose.onNodeWithText(expectedHeading).assertExists()
    }
    private fun localized(lang: String, resourceId: Int): String =
        nativeResources(fixture.context, lang).getString(resourceId)

    @Test fun settingsDataStoreSwitchHuEnDeRoRecomposesSettingsAndHomeIndependentlyOfSystemLocale() {
        launch()
        compose.onNodeWithTag("navigation_settings").performClick()
        val systemLanguage = fixture.context.resources.configuration.locales[0].language
        val cases = listOf(
            Triple("hu", "Edzésprofil", "Lépések"),
            Triple("en", "Training profile", "Steps"),
            Triple("de", "Trainingsprofil", "Schritte"),
            Triple("ro", "Profil de antrenament", "Pași")
        )
        for ((language, profileLabel, stepsLabel) in cases) {
            selectLanguageInSettings(language, profileLabel)
            compose.onNodeWithTag("navigation_back").performClick()
            awaitText(stepsLabel)
            // These localized metadata resources exist but currently are NOT painted
            // by TodayMetricsGrid (the third Triple field is ignored in production).
            val expectedUnits = mapOf(
                "hu" to Triple("lépés", "edzés", "készenlét"),
                "en" to Triple("steps", "workouts", "readiness"),
                "de" to Triple("Schritte", "Trainingseinheiten", "Trainingsbereitschaft"),
                "ro" to Triple("pași", "antrenamente", "pregătire")
            ).getValue(language)
            assertEquals(expectedUnits.first, localized(language, R.string.native_home_unit_steps))
            assertEquals(expectedUnits.second, localized(language, R.string.native_home_unit_workouts))
            assertEquals(expectedUnits.third, localized(language, R.string.native_home_unit_readiness))
            compose.onNodeWithTag("navigation_settings").performClick()
        }
        val appLanguageUnlikeSystem = cases.map { it.first }.first { it != systemLanguage }
        selectLanguageInSettings(appLanguageUnlikeSystem,
            cases.first { it.first == appLanguageUnlikeSystem }.second)
        assertNotEquals("App locale must be demonstrably different from system locale",
            systemLanguage, fixture.selectedLanguage())
        assertEquals(systemLanguage, fixture.context.resources.configuration.locales[0].language)
    }

    @Test fun trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation() {
        launch()
        compose.onNodeWithTag("navigation_settings").performClick()
        selectLanguageInSettings("ro", "Profil de antrenament")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("navigation_training").performClick()
        awaitText("Antrenament rapid")
        compose.onNodeWithTag("navigation_home").performClick()
        compose.onNodeWithTag("shortcut_calendar").performClick()
        awaitText("Calendar")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("shortcut_coach").performClick()
        compose.waitUntil(15_000) { nodesWithTag("coach_ready") }
        awaitText("Coach")
        compose.onNodeWithTag("navigation_back").performClick()

        compose.onNodeWithTag("navigation_settings").performClick()
        selectLanguageInSettings("de", "Trainingsprofil")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("shortcut_calendar").performClick()
        awaitText("Kalender")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("navigation_training").performClick()
        awaitText("Schnelltraining")
        assertEquals("de", fixture.selectedLanguage())
    }
}
