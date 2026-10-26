package com.repforge.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.ui.workout.NativeTrainingModeProposal
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Real app DataStore and PhoneWorkoutExecution, not a fake dispatcher/editor.
 * All tests run only on a disposable debuggable emulator.
 */
@RunWith(AndroidJUnit4::class)
class NativeTrainingModeCommitUiTest {
    private val fixture = NativeLocalizationEmulatorFixture()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun prepare() = fixture.reset()
    @After fun close() = fixture.close(null)

    @Test fun settingsApprovalAndUndoEnteredFromIoUseMainOnlyExecutionAdmission() {
        val activeProgram = runBlocking(Dispatchers.IO) { fixture.settings.current().activeProgramId }
        val proposal = NativeTrainingModeChange.prepare(
            null, activeProgram, "global", null, "hit", "linear")
        assertTrue(runBlocking(Dispatchers.IO) {
            NativeTrainingModeChange.approve(fixture.settings, proposal)
        })
        val saved = runBlocking(Dispatchers.IO) { fixture.settings.current().trainingModes197Json }
        assertEquals("hit", JSONObject(saved!!).getJSONObject("defaults").getString("trainingStyle"))
        assertTrue(runBlocking(Dispatchers.IO) {
            NativeTrainingModeChange.undo(fixture.settings, "global", null)
        })
        assertNull(runBlocking(Dispatchers.IO) { fixture.settings.current().trainingModes197Json })
    }

    @Test fun acceptedWeightTicketCommitsOnMainAndRejectsStaleOrChangedSettings() {
        val exercise = JSONObject().put("id", "goblet-squat")
            .put("measurementType", "reps").put("repUnit", "ism.")
            .put("loadType", "total")
            .put("sets", NativeWorkoutStartValues.blankSets(2, 0.0))
        val settings = runBlocking(Dispatchers.IO) { fixture.settings.current() }
        val candidate = NativeWorkoutSession.quickCandidate(exercise, settings)
            .put("programId", settings.activeProgramId).put("dayId", "A")
        val installed = AtomicReference<JSONObject>()
        instrumentation.runOnMainSync {
            installed.set(NativeWorkoutSession.installPrepared(fixture.context, candidate))
        }
        val initial = installed.get()
        val forbiddenModeChange = NativeTrainingModeChange.prepare(
            settings.trainingModes197Json, settings.activeProgramId,
            "global", null, "hit", "rir")
        // The same IO-entry admission must deny an active native workout,
        // not silently write a settings change during a running session.
        assertTrue(runCatching {
            runBlocking(Dispatchers.IO) {
                NativeTrainingModeChange.approve(fixture.settings, forbiddenModeChange)
            }
        }.isFailure)
        assertEquals(settings.trainingModes197Json,
            runBlocking(Dispatchers.IO) { fixture.settings.current().trainingModes197Json })
        val ticket = NativeTrainingModeProposal.ticket(initial, "goblet-squat",
            2.5, "checked-history-and-readiness", settings.trainingModes197Json)
        assertNotNull(ticket)

        // A stale settings snapshot cannot reach commitEdit.
        val wrong = ticket!!.copy(settingsRaw = "stale")
        val blocked = runBlocking(Dispatchers.IO) {
            NativeTrainingModeProposal.commitApprovedOnMain(fixture.context, wrong) {
                fixture.settings.current().trainingModes197Json
            }
        }
        assertNull(blocked)

        // This is the exact canonical main-thread commit function called by
        // ActiveWorkoutViewModel after it recomputes the Coach fingerprint.
        val saved = runBlocking(Dispatchers.IO) {
            NativeTrainingModeProposal.commitApprovedOnMain(fixture.context, ticket) {
                fixture.settings.current().trainingModes197Json
            }
        }
        assertNotNull(saved)
        val sets = saved!!.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertEquals(2.5, sets.getJSONObject(0).getDouble("weight"), 0.0)
        assertEquals(2.5, sets.getJSONObject(1).getDouble("weight"), 0.0)
        assertFalse(sets.getJSONObject(0).getBoolean("done"))

        val canonical = AtomicReference<JSONObject>()
        instrumentation.runOnMainSync {
            canonical.set(PhoneWorkoutExecution.store(fixture.context).snapshot())
        }
        assertEquals(saved.optLong("executionRevision"), canonical.get().optLong("executionRevision"))
        assertNull(runBlocking(Dispatchers.IO) {
            NativeTrainingModeProposal.commitApprovedOnMain(fixture.context, ticket) {
                fixture.settings.current().trainingModes197Json
            }
        })
    }
}
