package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.migration.LegacyWebViewMigrator
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
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class NativeJournalFeedbackUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(scenario)
    private fun stored() = runBlocking(Dispatchers.IO) {
        fixture.database.workoutHistoryDao().getWorkoutWithDetailsById("feedback-ui")!!
    }
    @Test fun journalFeedbackCancelAndSaveUseTheSameDurableWorkout() {
        val end = Instant.now().minusSeconds(60)
        runBlocking(Dispatchers.IO) {
            LegacyWebViewMigrator(fixture.context, fixture.database, fixture.settings).saveWorkoutJsonToRoom(
                JSONObject().put("id", "feedback-ui").put("syncId", "feedback-ui-sync")
                    .put("key", "feedback-ui-key").put("started", end.minusSeconds(60).toString())
                    .put("finished", end.toString()).put("feedback", JSONObject().put("effort", "good"))
                    .put("exercises", JSONArray().put(JSONObject().put("id", "plank").put("repUnit", "mp")
                        .put("loadType", "bodyweight").put("sets", JSONArray().put(JSONObject()
                            .put("setId", "feedback-ui-set").put("reps", "30").put("done", true))))))
        }
        val before = stored()
        scenario = fixture.launch()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("navigation_journal").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("navigation_journal").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("journal_workout_feedback-ui").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("journal_workout_feedback-ui").performClick()
        fun edit() {
            compose.onNodeWithTag("journal_details_list").performScrollToNode(hasTestTag("journal_edit"))
            compose.onNodeWithTag("journal_edit").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("journal_editor_save").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_feedback_pain"))
            compose.onNodeWithTag("journal_editor_feedback_pain").performClick().assertIsSelected()
        }
        edit()
        assertEquals(before, stored())
        compose.onNodeWithTag("journal_editor_cancel").performClick()
        assertEquals(before, stored())
        edit()
        compose.onNodeWithTag("journal_editor_save").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("journal_editor_save").fetchSemanticsNodes().isEmpty() }
        val saved = stored()
        assertEquals("pain", saved.workout.effortFeedback)
        assertEquals(before.workout.id, saved.workout.id)
        assertEquals(before.workout.syncId, saved.workout.syncId)
        assertEquals(before.workout.workoutKey, saved.workout.workoutKey)
        assertEquals("feedback-ui-set", JSONObject(saved.workout.rawJson).getJSONArray("exercises")
            .getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("setId"))
        assertEquals(1, runBlocking(Dispatchers.IO) { fixture.database.workoutHistoryDao().getWorkoutCount() })
    }
}
