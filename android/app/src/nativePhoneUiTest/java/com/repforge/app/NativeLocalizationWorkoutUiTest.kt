package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeLocalizationWorkoutUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario)

    private fun launchActive() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("navigation_training").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("navigation_training").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("workout_discard").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun snapshot(): JSONObject = requireNotNull(fixture.currentWorkout()) { "Active workout missing" }
    private fun waitForText(value: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(value, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(value, useUnmergedTree = true).assertExists()
    }
    private fun switchWhileInSettings(language: String, expectedLabel: String) {
        fixture.selectLanguageThroughUi(compose, language)
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("training_profile_open"))
        compose.waitUntil(15_000) {
            fixture.selectedLanguage() == language &&
                compose.onAllNodesWithText(expectedLabel).fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun identity(before: JSONObject, after: JSONObject) {
        assertEquals(before.getString("workoutId"), after.getString("workoutId"))
        assertEquals(before.getLong("generation"), after.getLong("generation"))
        assertEquals(before.optString("timerOwner"), after.optString("timerOwner"))
        assertEquals(before.optLong("ownerEpoch193"), after.optLong("ownerEpoch193"))
        assertTrue(after.getBoolean("active"))
        assertFalse(after.optBoolean("workoutPaused"))
    }
    private fun exerciseSets(frame: JSONObject) =
        frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")

    @Test fun settingsRoundTripPreservesRecordedSetsOwnerIdentityAndAdvancingStopwatch() {
        fixture.startQuick("plank")
        launchActive()
        assertTrue("Fixture needs two real timed sets", exerciseSets(snapshot()).length() >= 2)
        // Complete a genuine positive-time set via the real UI, not a fabricated DB row.
        compose.onNodeWithText("Indítás", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(15_000) {
            val clock = fixture.currentWorkout()?.optJSONObject(NativeSetStopwatch.FIELD)
            clock != null && NativeSetStopwatch.running(clock) && NativeSetStopwatch.elapsed(clock) >= 1_100L
        }
        compose.onNodeWithText("Leállítás", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(15_000) {
            fixture.currentWorkout()?.optJSONArray("exercises")?.optJSONObject(0)
                ?.optJSONArray("sets")?.optJSONObject(0)?.optBoolean("done") == true
        }
        assertTrue(exerciseSets(snapshot()).getJSONObject(0).optDouble("reps", 0.0) > 0.0)
        // A recorded set starts a real rest. End it through its UI before asking
        // the single-owner engine to run the next set's stopwatch.
        compose.onNodeWithTag("workout_rest_skip").performClick()
        // Second set stopwatch is active during Settings; its elapsed time must never reset.
        compose.onNodeWithText("Indítás", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitUntil(15_000) {
            fixture.currentWorkout()?.optJSONObject(NativeSetStopwatch.FIELD)
                ?.let(NativeSetStopwatch::running) == true
        }
        val before = snapshot()
        val beforeClock = before.getJSONObject(NativeSetStopwatch.FIELD)
        val beforeElapsed = NativeSetStopwatch.elapsed(beforeClock)
        val beforeSets = exerciseSets(before).toString()
        compose.onNodeWithTag("navigation_settings").performClick()
        switchWhileInSettings("en", "Training profile")
        identity(before, snapshot())
        switchWhileInSettings("ro", "Profil de antrenament")
        compose.onNodeWithTag("navigation_back").performClick()
        waitForText("Cronometru · seria 2")
        val after = snapshot()
        identity(before, after)
        assertEquals(beforeSets, exerciseSets(after).toString())
        assertEquals(beforeClock.getString("id"), after.getJSONObject(NativeSetStopwatch.FIELD).getString("id"))
        assertTrue(NativeSetStopwatch.running(after.getJSONObject(NativeSetStopwatch.FIELD)))
        assertTrue("A running timer must advance or remain monotonic",
            NativeSetStopwatch.elapsed(after.getJSONObject(NativeSetStopwatch.FIELD)) >= beforeElapsed)
        assertEquals(0, runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() })
    }

    @Test fun stopwatchAndIntervalRestLabelsRecomposeWithoutStartingOrCompletingSets() {
        fixture.startQuick("plank")
        launchActive()
        val before = snapshot()
        val sets = exerciseSets(before).toString()
        val owner = before.optString("timerOwner")
        waitForText("Stopper · 1. sorozat")
        assertFalse(before.optJSONObject(NativeSetStopwatch.FIELD)?.let(NativeSetStopwatch::running) == true)
        compose.onNodeWithTag("navigation_settings").performClick()
        switchWhileInSettings("de", "Trainingsprofil")
        compose.onNodeWithTag("navigation_back").performClick()
        waitForText("Stoppuhr · Satz 1")
        compose.onNodeWithTag("active_workout_exercises")
            .performScrollToNode(hasTestTag("interval_options"))
        compose.onNodeWithTag("interval_options").performScrollTo().performClick()
        compose.onNodeWithTag("active_workout_exercises")
            .performScrollToNode(hasTestTag("interval_rest"))
        compose.onNodeWithTag("interval_rest").performScrollTo()
        waitForText("Pause (s)")
        assertEquals(sets, exerciseSets(snapshot()).toString())
        assertEquals(owner, snapshot().optString("timerOwner"))
        assertFalse(snapshot().optJSONObject(NativeSetStopwatch.FIELD)?.let(NativeSetStopwatch::running) == true)
        assertFalse(snapshot().optJSONObject("timer")?.optBoolean("running") == true)
        assertEquals(0, runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() })
    }

    @Test fun gpsReadyAndActionLabelsTranslateWithoutPermissionOrMeasurementSuccess() {
        val distanceId = runBlocking(Dispatchers.IO) {
            fixture.database.exerciseLibraryDao().getAllExercises()
                .first { it.measurementType == "distance" ||
                    it.repUnit.trim().lowercase() in setOf("m", "km") }.id
        }
        fixture.startQuick(distanceId) // Bundled distance entry; explicit GPS selection is false.
        val initial = snapshot()
        val exercise = initial.getJSONArray("exercises").getJSONObject(0)
        assertTrue("Requires an actual distance exercise",
            exercise.optString("measurementType") == "distance" ||
                exercise.optString("repUnit") in listOf("m", "km"))
        val sets = exerciseSets(initial).toString()
        launchActive()
        waitForText("Mérésre kész")
        waitForText("GPS-mérés indítása")
        compose.onNodeWithTag("navigation_settings").performClick()
        switchWhileInSettings("en", "Training profile")
        compose.onNodeWithTag("navigation_back").performClick()
        waitForText("Ready to measure")
        waitForText("Start GPS measurement")
        compose.onNodeWithTag("navigation_settings").performClick()
        switchWhileInSettings("ro", "Profil de antrenament")
        compose.onNodeWithTag("navigation_back").performClick()
        waitForText("Pregătit de măsurare")
        waitForText("Pornește măsurarea GPS")
        val after = snapshot()
        identity(initial, after)
        assertEquals(sets, exerciseSets(after).toString())
        assertFalse(exerciseSets(after).getJSONObject(0).optBoolean("done"))
        assertEquals(0.0, exerciseSets(after).getJSONObject(0).optDouble("distanceMeters", 0.0), 0.0)
        assertFalse("A locale change must not activate a GPS measurement",
            after.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("state").orEmpty() in listOf("running", "prepared", "explicitStopped"))
        assertEquals(0, runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() })
    }
}
