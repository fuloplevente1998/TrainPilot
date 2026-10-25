package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeLocalizationSafetyUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario)
    private fun visible(text: String) =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test fun invalidLocalizedProfileAndDestructiveCancelNeverWriteOrEraseRealData() {
        val originalProfile = JSONObject().put("age", 27)
            .put("gear", JSONArray().put("dumbbell"))
        runBlocking(Dispatchers.IO) { fixture.settings.saveTrainingProfile(originalProfile) }
        val originalProgramIds =
            runBlocking(Dispatchers.IO) { fixture.database.programDao().getProgramIds() }
        val originalWorkouts =
            runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() }
        val storedProfileBefore = fixture.profileJson().toString()

        scenario = fixture.launch()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("navigation_settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("navigation_settings").performClick()
        fixture.selectLanguageThroughUi(compose, "de")
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("training_profile_open"))
        compose.waitUntil(15_000) { fixture.selectedLanguage() == "de" && visible("Trainingsprofil") }
        compose.onNodeWithTag("training_profile_open").performScrollTo().performClick()
        compose.onNodeWithTag("profile_field_weight").performTextReplacement("Infinity")
        compose.onNodeWithTag("profile_save").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("profile_write_error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("profile_write_error").assertTextEquals(
            "Speichern fehlgeschlagen: ungültiges Feld oder neueres Profil.")
        assertEquals(27, fixture.profileJson().getInt("age"))
        assertEquals("dumbbell", fixture.profileJson().getJSONArray("gear").getString(0))
        assertFalse(fixture.profileJson().has("weight"))
        compose.onNodeWithText("Abbrechen").performClick()

        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("Alle lokalen Daten löschen (Erase Local)"))
        compose.onNodeWithText("Alle lokalen Daten löschen (Erase Local)").performClick()
        compose.waitUntil(15_000) { visible("Alle lokalen Daten löschen") }
        compose.onNodeWithText("Alle Daten löschen").assertExists()
        compose.onNodeWithText("Abbrechen").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Alle Daten löschen").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(originalProgramIds,
            runBlocking(Dispatchers.IO) { fixture.database.programDao().getProgramIds() })
        assertEquals(originalWorkouts,
            runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() })
        assertEquals(storedProfileBefore, fixture.profileJson().toString())
        assertFalse(fixture.currentWorkout()?.optBoolean("active") == true)
        assertEquals("de", fixture.selectedLanguage())
    }
}
