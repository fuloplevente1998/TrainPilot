package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.migration.LegacyWebViewMigrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the production MainActivity, Programs VM, native Room and actual Compose dialogs. */
@RunWith(AndroidJUnit4::class)
class NativeProgramActionsUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun setup() = fixture.reset()
    @After fun cleanup() = fixture.close(scenario)

    private fun launchPrograms() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("navigation_home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("shortcut_programs").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("program_list").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun program(id: String): ProgramWithDays = runBlocking(Dispatchers.IO) {
        fixture.database.programDao().getProgramWithDaysById(id)!!
    }

    private fun newFactoryCopy(): String = runBlocking(Dispatchers.IO) {
        val source = fixture.database.programDao().getProgramWithDaysById("home-basic")!!
        NativeProgramEditor.save(fixture.context, fixture.database,
            source, NativeProgramEditor.from(source, "hu"), "hu",
            actionToken = "ui-restore-case-${java.util.UUID.randomUUID()}")
    }

    private fun capture(name: String) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val path = "/sdcard/a07-$name.png"
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        // UiAutomation starts a command, not a shell: &&/echo are not evaluated.
        val bytes = device.executeShellCommand("stat -c %s $path").trim().toLongOrNull()
        assertTrue("Expected nonempty raw screenshot at $path, bytes=$bytes",
            bytes != null && bytes > 8L)
    }

    private fun importLegacyCustomExercise(id: String) = runBlocking(Dispatchers.IO) {
        val custom = JSONObject()
            .put("id", id).put("custom", true)
            .put("hu", "Örökölt saját").put("en", "Legacy custom")
            .put("de", "Importierte eigene Übung").put("ro", "Exercițiu importat")
            .put("notesDe", "A07").put("loadType", "total").put("repUnit", "ism.")
            .put("measurementType", "reps").put("sets", 3).put("reps", "8").put("weight", 12)
        LegacyWebViewMigrator(fixture.context, fixture.database, fixture.settings)
            .importBackupJson(JSONObject().put("exercises", JSONArray().put(custom)), false)
    }

    private fun changeDayA(id: String) = runBlocking(Dispatchers.IO) {
        val before = fixture.database.programDao().getProgramWithDaysById(id)!!
        val changed = before.days.map {
            if (it.dayId == "A") it.copy(exercisesJson = JSONArray().put("goblet-squat").toString())
            else it
        }
        fixture.database.programDao().saveProgramWithDays(before.program, changed)
    }

    private fun openDayRestore(id: String) {
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_$id"))
        // Cancel keeps the card expanded. A second toggle would collapse it,
        // making the original restore A button disappear despite valid Room rows.
        if (compose.onAllNodesWithTag("program_restore_${id}_A").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("program_expand_$id").performScrollTo().assertIsDisplayed().performClick()
        }
        try {
            compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_restore_${id}_A"))
        } catch (failure: Throwable) {
            val stored = program(id)
            throw AssertionError("Restore day selector missing: sourceTemplateId=" +
                stored.program.sourceTemplateId + ", days=" + stored.days.map { it.dayId } +
                "; UI=" + compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString().take(8500), failure)
        }
        compose.onNodeWithTag("program_restore_${id}_A").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("program_restore_factory_exercises").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun builtinCopyOnEditPublishesExactlyOneCustomProgramWithoutActivatingIt() {
        val factory = program("home-basic")
        val active = runBlocking { fixture.settings.current().activeProgramId }
        launchPrograms()
        compose.onNodeWithTag("program_list").performScrollToNode(hasTestTag("program_edit_home-basic"))
        compose.onNodeWithTag("program_edit_home-basic").performClick()
        compose.onNodeWithTag("program_editor_save").performClick()
        compose.waitUntil(15_000) { runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramIds().size == 14
        } }
        val copy = runBlocking(Dispatchers.IO) {
            fixture.database.programDao().getProgramsWithDays().single { !it.program.builtin }
        }
        assertNotEquals(factory.program.id, copy.program.id)
        assertEquals("home-basic", copy.program.sourceTemplateId)
        assertEquals(factory, program("home-basic"))
        assertEquals(active, runBlocking { fixture.settings.current().activeProgramId })
    }

    @Test fun cancelPreservesDayThenConfirmedResetPersistsOnlyChosenDay() {
        val copyId = newFactoryCopy()
        changeDayA(copyId)
        val before = program(copyId)
        val factory = program("home-basic")
        launchPrograms()
        openDayRestore(copyId)
        capture("preview-cancel")
        compose.onNodeWithTag("program_restore_cancel").performClick()
        assertEquals(before, program(copyId))
        openDayRestore(copyId)
        compose.onNodeWithTag("program_restore_confirm").performClick()
        compose.waitUntil(15_000) {
            program(copyId).days.single { it.dayId == "A" }.exercisesJson ==
                factory.days.single { it.dayId == "A" }.exercisesJson
        }
        capture("confirmed")
        val saved = program(copyId)
        assertEquals(before.days.single { it.dayId == "B" }, saved.days.single { it.dayId == "B" })
        assertEquals(factory, program("home-basic"))
    }

    @Test fun migratedCustomCatalogRowSurvivesConfirmedFactoryDayRestore() {
        val importedId = "legacy-a07-ui-custom"
        importLegacyCustomExercise(importedId)
        val importedBefore = runBlocking(Dispatchers.IO) {
            fixture.database.exerciseLibraryDao().getExerciseById(importedId)!!
        }
        val copyId = newFactoryCopy()
        changeDayA(copyId)
        launchPrograms()
        openDayRestore(copyId)
        capture("migrated-preview")
        compose.onNodeWithTag("program_restore_confirm").performClick()
        compose.waitUntil(15_000) {
            program(copyId).days.single { it.dayId == "A" }.exercisesJson ==
                program("home-basic").days.single { it.dayId == "A" }.exercisesJson
        }
        val importedAfter = runBlocking(Dispatchers.IO) {
            fixture.database.exerciseLibraryDao().getExerciseById(importedId)!!
        }
        assertEquals(importedBefore, importedAfter)
        assertTrue(importedAfter.custom)
    }

    @Test fun staleProgramAfterPreviewIsRejectedAndLeavesLaterEditUntouched() {
        val copyId = newFactoryCopy()
        changeDayA(copyId)
        launchPrograms()
        openDayRestore(copyId)
        val late = program(copyId)
        runBlocking(Dispatchers.IO) {
            fixture.database.programDao().saveProgramWithDays(
                late.program.copy(nameHu = "Changed after preview"), late.days)
        }
        compose.onNodeWithTag("program_restore_confirm").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("program_restore_error").fetchSemanticsNodes().isNotEmpty()
        }
        capture("stale-error")
        assertEquals("Changed after preview", program(copyId).program.nameHu)
        assertEquals(late.days.single { it.dayId == "A" }.exercisesJson,
            program(copyId).days.single { it.dayId == "A" }.exercisesJson)
    }
}
