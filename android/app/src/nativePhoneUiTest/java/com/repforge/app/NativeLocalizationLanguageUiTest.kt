package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        compose.onAllNodesWithText(text).onFirst().assertExists()
    }
    private fun selectLanguageInSettings(language: String, expectedHeading: String) {
        fixture.selectLanguageThroughUi(compose, language)
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("training_profile_open"))
        compose.waitUntil(15_000) { fixture.selectedLanguage() == language && textVisible(expectedHeading) }
        compose.onNodeWithText(expectedHeading).assertExists()
    }

    @Test fun settingsPickerSwitchHuEnDeRoRecomposesSettingsAndHomeIndependentlyOfSystemLocale() {
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
            val expectedUnits = mapOf(
                "hu" to Pair("lépés", "edzés"),
                "en" to Pair("steps", "workouts"),
                "de" to Pair("Schritte", "Trainingseinheiten"),
                "ro" to Pair("pași", "antrenamente")
            ).getValue(language)
            for ((key, expectedUnit) in listOf("steps" to expectedUnits.first,
                "workouts" to expectedUnits.second)) {
                compose.onNodeWithTag("home_metric_${key}_unit").performScrollTo()
                    .assertIsDisplayed().assertTextEquals(expectedUnit)
            }
            // #263 replaced the misleading generic readiness unit with actual
            // source/freshness. This wiped fixture has no measurements/history:
            // verify the translated absence and no fabricated recovery score.
            val expectedRecoverySource = mapOf("hu" to "Nincs adat", "en" to "No data",
                "de" to "Keine Daten", "ro" to "Fără date").getValue(language)
            compose.onNodeWithTag("home_metric_recovery_source").performScrollTo()
                .assertIsDisplayed().assertTextEquals(expectedRecoverySource)
            compose.onNodeWithTag("home_metric_recovery_value").performScrollTo()
                .assertIsDisplayed().assertTextEquals("—")
            val program = runBlocking(Dispatchers.IO) {
                val active = fixture.settings.current().activeProgramId
                val programs = fixture.database.programDao().getProgramsWithDays()
                programs.firstOrNull { it.program.id == active } ?: programs.first()
            }
            val expectedProgram = when (language) {
                "en" -> program.program.nameEn; "de" -> program.program.nameDe
                "ro" -> program.program.nameRo; else -> program.program.nameHu
            }.ifBlank { program.program.nameHu }
            val day = program.days.first()
            val expectedDay = when (language) {
                "en" -> day.nameEn; "de" -> day.nameDe; "ro" -> day.nameRo; else -> day.nameHu
            }.ifBlank { day.nameHu }
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasTestTag("home_program_name") and hasText(expectedProgram))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home_program_name").performScrollTo().assertTextEquals(expectedProgram)
            compose.onNodeWithTag("home_day_name").assertTextEquals(expectedDay)
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
