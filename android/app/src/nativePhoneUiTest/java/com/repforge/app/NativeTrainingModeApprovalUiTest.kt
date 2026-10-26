package com.repforge.app

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.NativeTrainingModes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real program picker, persistent DataStore and Room on a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class NativeTrainingModeApprovalUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun reset() = fixture.reset()
    @After fun close() = fixture.close(scenario)

    private fun current() = runBlocking(Dispatchers.IO) { fixture.settings.current() }

    private fun openProgramPicker() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("shortcut_programs").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.onNodeWithTag("program_list")
            .performScrollToNode(hasTestTag("training_modes_program_preview"))
    }

    private fun select(tag: String, option: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.onNodeWithTag("${tag}_${option}").performClick()
    }

    private fun awaitReady() {
        compose.waitUntil(30_000) {
            check(compose.onAllNodesWithTag("training_modes_program_comparison_error")
                .fetchSemanticsNodes().isEmpty()) {
                "Real program comparison failed; inspect NativeModeApproval logcat"
            }
            compose.onAllNodesWithTag("training_modes_program_approve").fetchSemanticsNodes()
                .any { it.config.getOrNull(SemanticsProperties.Disabled) == null } &&
                compose.onAllNodesWithTag("training_modes_program_comparison_0")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("training_modes_program_approve").assertIsEnabled()
    }

    @Test fun programPreviewAndCancelLeaveSettingsAndRecipesUnchanged() {
        val initial = current().trainingModes197Json
        val pid = current().activeProgramId
        val rawRecipe = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.program.rawJson
        }
        openProgramPicker()
        select("training_modes_program_style", "hit")
        select("training_modes_program_progression", "rir")
        compose.onNodeWithTag("training_modes_program_preview").performClick()
        awaitReady()
        assertEquals(initial, current().trainingModes197Json)
        compose.onNodeWithTag("training_modes_program_cancel").performClick()
        assertEquals(initial, current().trainingModes197Json)
        assertEquals(rawRecipe, runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.program.rawJson
        })
        // Cancelled proposal cannot be approved without a new preview.
        compose.onAllNodesWithTag("training_modes_program_approve").assertCountEquals(0)
        compose.onNodeWithTag("program_list")
            .performScrollToNode(hasTestTag("training_modes_program_preview"))
    }

    @Test fun programApprovalRejectsSettingsChangedAfterCompletedComparison() {
        val pid = current().activeProgramId
        val initial = JSONObject().put("schemaVersion", 1).put("explicitChoice", true)
            .put("defaults", JSONObject().put("trainingStyle", "normal")
                .put("progressionMode", "double"))
            .put("byProgram", JSONObject()).put("revision", 1)
        runBlocking(Dispatchers.IO) {
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", initial)))
        }
        openProgramPicker()
        select("training_modes_program_style", "hit")
        select("training_modes_program_progression", "rir")
        compose.onNodeWithTag("training_modes_program_preview").performClick()
        awaitReady()
        val competing = JSONObject(initial.toString()).put("revision", 2)
        runBlocking(Dispatchers.IO) {
            // An external concurrent settings update, not a substitute for a UI approval.
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", competing)))
        }
        val concurrentRaw = current().trainingModes197Json
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        compose.waitUntil(15_000) {
            // The pending dialog has been consumed and the VM has completed the reject.
            compose.onAllNodesWithTag("training_modes_program_approve")
                .fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("training_modes_program_preview")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(concurrentRaw, current().trainingModes197Json)
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, pid).source)
    }
}
