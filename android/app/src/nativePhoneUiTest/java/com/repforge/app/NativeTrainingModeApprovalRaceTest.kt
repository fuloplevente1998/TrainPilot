package com.repforge.app

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.ui.programs.TrainingProgramsViewModel
import com.repforge.app.ui.settings.NativeTrainingModeComparison
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real database, execution admission and shared metadata lease on an isolated emulator. */
@RunWith(AndroidJUnit4::class)
class NativeTrainingModeApprovalRaceTest {
    private val fixture = NativeLocalizationEmulatorFixture()
    private fun current() = runBlocking(Dispatchers.IO) { fixture.settings.current() }
    private fun vm() = TrainingProgramsViewModel(fixture.context.applicationContext as Application)

    @Before fun reset() = fixture.reset()
    @After fun close() = fixture.closeWithoutActivity()

    @Test fun queuedProgramEditorSaveInvalidatesComparisonBeforeApprovalCas() = runBlocking(Dispatchers.IO) {
        val builtin = fixture.database.programDao().getProgramsWithDays().first { it.program.builtin }
        val id = NativeProgramEditor.save(fixture.context, fixture.database, builtin,
            NativeProgramEditor.from(builtin, "hu"), "hu")
        fixture.settings.setActiveProgramId(id)
        val initial = fixture.settings.current().trainingModes197Json
        val proposal = NativeTrainingModeChange.prepare(initial, id, "program", id, "hit", "rir")
        val snapshot = NativeTrainingModeComparison.read(fixture.context, proposal)
        assertTrue(snapshot.rows.isNotEmpty())

        val opened = fixture.database.programDao().getProgramWithDaysById(id)!!
        val updated = NativeProgramEditor.from(opened, "hu").let {
            it.copy(description = it.description + " new prescription revision")
        }
        val writerHasLease = CompletableDeferred<Unit>()
        val allowWriterToSave = CompletableDeferred<Unit>()
        val writer = async(Dispatchers.IO) {
            NativeDataReplacement.mutate(fixture.context) {
                writerHasLease.complete(Unit)
                allowWriterToSave.await()
                NativeProgramEditor.save(fixture.context, fixture.database, opened, updated, "hu")
            }
        }
        writerHasLease.await()
        val approvalQueued = CompletableDeferred<Unit>()
        val approve = async(Dispatchers.IO) {
            approvalQueued.complete(Unit)
            vm().commitComparedTrainingMode(proposal, snapshot)
        }
        approvalQueued.await()
        allowWriterToSave.complete(Unit)
        assertEquals(id, writer.await())
        val edited = fixture.database.programDao().getProgramWithDaysById(id)!!.program.rawJson
        assertNotEquals(snapshot.programRaw, edited)
        assertFalse("A queued approval cannot commit stale recipes", approve.await())
        assertEquals(initial, fixture.settings.current().trainingModes197Json)
        assertEquals(edited, fixture.database.programDao().getProgramWithDaysById(id)!!.program.rawJson)
    }

    @Test fun activeAndPausedNativeWorkoutBothDenyComparedApproval() {
        val settings = current()
        val proposal = NativeTrainingModeChange.prepare(settings.trainingModes197Json,
            settings.activeProgramId, "program", settings.activeProgramId, "hit", "rir")
        val snapshot = runBlocking(Dispatchers.IO) {
            NativeTrainingModeComparison.read(fixture.context, proposal)
        }
        val model = vm()
        val active = fixture.startQuick("goblet-squat")
        assertTrue(active.optBoolean("active"))
        assertTrue(runCatching {
            runBlocking(Dispatchers.IO) { model.commitComparedTrainingMode(proposal, snapshot) }
        }.isFailure)
        assertEquals(settings.trainingModes197Json, current().trainingModes197Json)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val before = requireNotNull(PhoneWorkoutExecution.store(fixture.context).snapshot())
            val pause = JSONObject(before.toString())
                .put("workoutPaused", true)
                .put("pauseRevision", before.optLong("pauseRevision") + 1)
            PhoneWorkoutExecution.setWorkoutPause(fixture.context, pause)
            assertTrue(PhoneWorkoutExecution.store(fixture.context)
                .snapshot()?.optBoolean("workoutPaused") == true)
        }
        assertTrue(runCatching {
            runBlocking(Dispatchers.IO) { model.commitComparedTrainingMode(proposal, snapshot) }
        }.isFailure)
        assertEquals(settings.trainingModes197Json, current().trainingModes197Json)
    }
}
