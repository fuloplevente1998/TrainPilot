package com.repforge.app

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.NativeDataReplacement
import com.repforge.app.ui.settings.NativeTrainingModeComparison
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
        try {
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
        } catch (failure: Throwable) {
            throw AssertionError("Program picker after preview: " +
                compose.onRoot(useUnmergedTree = true).printToString().take(8500), failure)
        }
        compose.onNodeWithTag("training_modes_program_approve").assertIsEnabled()
    }

    private fun selectedGlobal() = JSONObject().put("schemaVersion", 1)
        .put("explicitChoice", true).put("revision", 1)
        .put("defaults", JSONObject().put("trainingStyle", "normal")
            .put("progressionMode", "double"))
        .put("byProgram", JSONObject())

    private fun editableProgram(): String = runBlocking(Dispatchers.IO) {
        val builtin = fixture.database.programDao().getProgramsWithDays()
            .first { it.program.builtin }
        val copied = NativeProgramEditor.save(fixture.context, fixture.database, builtin,
            NativeProgramEditor.from(builtin, "hu"), "hu")
        fixture.settings.setActiveProgramId(copied)
        fixture.settings.importPortableSettings(JSONObject().put("settings",
            JSONObject().put("trainingModes197", selectedGlobal())))
        copied
    }

    private fun previewHitRir() {
        openProgramPicker()
        select("training_modes_program_style", "hit")
        select("training_modes_program_progression", "rir")
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_preview"))
        compose.onNodeWithTag("training_modes_program_preview").performScrollTo().performClick()
        awaitReady()
    }

    private fun awaitRejected() {
        val label = nativeResources(fixture.context, current().language)
            .getString(R.string.native_modes_error)
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Genuine program editor save, never direct Room writes or an approved-mode shortcut. */
    private fun editProgram(programId: String, changeDayRecipes: Boolean): String =
        runBlocking(Dispatchers.IO) {
            val original = fixture.database.programDao().getProgramWithDaysById(programId)!!
            val draft = NativeProgramEditor.from(original, "hu")
            val edited = if (changeDayRecipes) {
                val day = draft.days.first { it.entries.isNotEmpty() }
                draft.copy(days = draft.days.map {
                    if (it.id == day.id) it.copy(entries = it.entries.dropLast(1)) else it
                })
            } else {
                draft.copy(description = draft.description + " revision")
            }
            assertEquals(programId, NativeProgramEditor.save(fixture.context, fixture.database,
                original, edited, "hu"))
            fixture.database.programDao().getProgramWithDaysById(programId)!!.program.rawJson
        }

    @Test fun uiEmptyComparisonNeverEnablesApproval() {
        val pid = runBlocking(Dispatchers.IO) {
            val blank = NativeProgramEditor.Draft("Empty mode comparison", "",
                listOf(NativeProgramEditor.DayDraft("A", "Day A", emptyList())))
            val id = NativeProgramEditor.save(fixture.context, fixture.database,
                null, blank, "hu")
            fixture.settings.setActiveProgramId(id)
            id
        }
        val raw = current().trainingModes197Json
        openProgramPicker()
        select("training_modes_program_style", "hit")
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_preview"))
        compose.onNodeWithTag("training_modes_program_preview").performScrollTo().performClick()
        try {
            compose.waitUntil(30_000) {
                compose.onAllNodesWithTag("training_modes_program_comparison_error")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            throw AssertionError("Empty program comparison dialog: " +
                compose.onRoot(useUnmergedTree = true).printToString().take(8500), failure)
        }
        compose.onNodeWithTag("training_modes_program_approve").assertIsNotEnabled()
        assertEquals(raw, current().trainingModes197Json)
        assertEquals("legacy", NativeTrainingModes.resolveSavedJson(raw, pid).source)
        compose.onNodeWithTag("training_modes_program_cancel").performClick()
        assertEquals(raw, current().trainingModes197Json)
    }

    @Test fun uiApprovalRejectsProgramRawChangeAfterShownComparison() {
        val pid = editableProgram()
        previewHitRir()
        val expectedSettings = current().trainingModes197Json
        val savedRecipe = editProgram(pid, false)
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        awaitRejected()
        assertEquals(expectedSettings, current().trainingModes197Json)
        assertEquals(savedRecipe, runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.program.rawJson
        })
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, pid).source)
    }

    @Test fun uiApprovalRejectsProgramDayRecipeChangeAfterShownComparison() {
        val pid = editableProgram()
        previewHitRir()
        val expectedSettings = current().trainingModes197Json
        val (changedDayId, originalDay) = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.days
                .first { org.json.JSONArray(it.exercisesJson).length() > 0 }
                .let { it.dayId to it.exercisesJson }
        }
        val savedRecipe = editProgram(pid, true)
        val changedDay = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.days
                .first { it.dayId == changedDayId }.exercisesJson
        }
        // A real editor save changed the day, and approval must not undo it.
        assertNotEquals(originalDay, changedDay)
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        awaitRejected()
        assertEquals(expectedSettings, current().trainingModes197Json)
        assertEquals(savedRecipe, runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramWithDaysById(pid)!!.program.rawJson
        })
    }

    @Test fun uiApprovalRejectsSameBytesAfterReplacementGenerationChanges() {
        editableProgram()
        previewHitRir()
        val raw = current().trainingModes197Json
        runBlocking(Dispatchers.IO) {
            // An empty successful replacement lease advances the actual epoch.
            // This specifically tests ABA: all persisted bytes stay identical.
            NativeDataReplacement.exclusively(fixture.context) {
                assertEquals(raw, fixture.settings.current().trainingModes197Json)
            }
        }
        assertEquals(raw, current().trainingModes197Json)
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        awaitRejected()
        assertEquals(raw, current().trainingModes197Json)
    }

    @Test fun doubleClickCannotApproveProgramTwice() {
        val pid = editableProgram()
        previewHitRir()
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        compose.onAllNodesWithTag("training_modes_program_approve").assertCountEquals(0)
        compose.waitUntil(15_000) {
            NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, pid).source == "program"
        }
        val approved = current().trainingModes197Json
        val revision = JSONObject(approved!!).getLong("revision")
        assertEquals(2L, revision)
        // No second confirm surface remains; no second settings write occurs.
        compose.onAllNodesWithTag("training_modes_program_approve").assertCountEquals(0)
        assertEquals(approved, current().trainingModes197Json)
    }

    @Test fun hitRirProgramComparisonUsesRealRoomRowsWithoutMutatingSettings() {
        val pid = current().activeProgramId
        val raw = JSONObject().put("schemaVersion", 1).put("explicitChoice", true)
            .put("defaults", JSONObject().put("trainingStyle", "normal")
                .put("progressionMode", "double"))
            .put("byProgram", JSONObject())
        runBlocking(Dispatchers.IO) {
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", raw)))
        }
        val before = current().trainingModes197Json
        val proposed = NativeTrainingModeChange.prepare(
            before, pid, "program", pid, "hit", "rir")
        val snapshot = runBlocking(Dispatchers.IO) {
            NativeTrainingModeComparison.read(fixture.context, proposed)
        }
        assertEquals(pid, snapshot.programId)
        assertTrue("Coach comparison must cover real program exercises", snapshot.rows.isNotEmpty())
        assertTrue(runBlocking(Dispatchers.IO) {
            NativeTrainingModeComparison.isCurrent(fixture.context, proposed, snapshot)
        })
        assertEquals(before, current().trainingModes197Json)
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
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_preview"))
        compose.onNodeWithTag("training_modes_program_preview").performScrollTo().performClick()
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
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("training_modes_program_preview"))
        compose.onNodeWithTag("training_modes_program_preview").performScrollTo().performClick()
        awaitReady()
        val competing = JSONObject(initial.toString()).put("revision", 2)
        runBlocking(Dispatchers.IO) {
            // An external concurrent settings update, not a substitute for a UI approval.
            fixture.settings.importPortableSettings(JSONObject().put("settings",
                JSONObject().put("trainingModes197", competing)))
        }
        val concurrentRaw = current().trainingModes197Json
        compose.onNodeWithTag("training_modes_program_approve").performClick()
        val denied = nativeResources(fixture.context, current().language)
            .getString(R.string.native_modes_error)
        compose.waitUntil(15_000) {
            // Wait for the actual rejection result, not just a dismissed dialog.
            compose.onAllNodesWithText(denied).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(concurrentRaw, current().trainingModes197Json)
        assertEquals("global", NativeTrainingModes.resolveSavedJson(current().trainingModes197Json, pid).source)
    }
}
