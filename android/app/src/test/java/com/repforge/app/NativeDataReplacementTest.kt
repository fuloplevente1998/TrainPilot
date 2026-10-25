package com.repforge.app

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeDataReplacementTest {
    private lateinit var context: Context
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun close() {
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }
    private fun frame() = JSONObject("""{"active":true,"workoutId":"replacement-test","timerOwner":"phone",
        "started":"2026-10-24T10:00:00Z","currentExercise":0,"exercises":[{"id":"plank","repUnit":"mp",
        "sets":[{"setId":"one","reps":"30","done":false}]}]}""")
    private suspend fun reject(block: suspend () -> Unit) {
        var error: Throwable? = null
        try { block() } catch (failure: IllegalStateException) { error = failure }
        assertNotNull(error)
    }
    @Test fun normalLeaseBlocksNativeStartAndBackgroundPublicationThenReleasesBothFences() = runTest {
        NativeDataReplacement.exclusively(context) {
            assertTrue(NativeDataReplacement.blocked(context))
            assertTrue(PhoneWorkoutExecution.store(context).restoreBlocked())
            reject { NativeWorkoutSession.installPrepared(context, frame()) }
            reject { withContext(Dispatchers.IO) { ActiveWorkoutStore.saveOwned(context, frame()) } }
            assertNull(PhoneWorkoutExecution.store(context).snapshot())
        }
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
        assertNotNull(NativeWorkoutSession.installPrepared(context, frame()))
    }
    @Test fun phonePausedAndWatchSessionsRejectReplacementBeforeCallingTheOperation() = runTest {
        for (owner in listOf("phone", "watch")) {
            val saved = ActiveWorkoutStore.saveOwned(context, frame().put("timerOwner", owner).put("workoutPaused", true))
            var called = false
            reject { NativeDataReplacement.exclusively(context) { called = true } }
            assertFalse(called)
            assertEquals(saved.toString(), ActiveWorkoutStore.current(context).toString())
            assertFalse(NativeDataReplacement.blocked(context))
        }
    }
    @Test fun nativeOnlyUnpublishedSessionAlsoBlocksReplacement() = runTest {
        PhoneWorkoutExecution.store(context).prepare(frame().put("generation", 1).put("executionRevision", 0), 0)
        reject { NativeDataReplacement.exclusively(context) { fail("Must not run") } }
        assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun failureReleasesTheLeaseAndNeverConvertsFailureToSuccess() = runTest {
        var caught = false
        try { NativeDataReplacement.exclusively(context) { throw IOException("injected failure") } }
        catch (_: IOException) { caught = true }
        assertTrue(caught)
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
        NativeDataReplacement.exclusively(context) { assertTrue(NativeDataReplacement.blocked(context)) }
    }
    @Test fun cancellationReleasesNativeAndPublicationFencesBeforeTheJobCompletes() = runTest {
        val entered = CompletableDeferred<Unit>()
        val job = launch {
            NativeDataReplacement.exclusively(context) { entered.complete(Unit); awaitCancellation() }
        }
        entered.await()
        assertTrue(NativeDataReplacement.blocked(context))
        job.cancelAndJoin()
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(PhoneWorkoutExecution.store(context).restoreBlocked())
    }
    @Test fun concurrentOrNestedReplacementIsRejectedWithoutReleasingTheOuterFence() = runTest {
        NativeDataReplacement.exclusively(context) {
            reject { NativeDataReplacement.exclusively(context) { fail("Must not run") } }
            assertTrue(NativeDataReplacement.blocked(context))
        }
        assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun interruptedDurableFenceFailsClosedInsteadOfSilentlyStartingOrOverwritingData() = runTest {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        reject { NativeDataReplacement.exclusively(context) { fail("Recovery required") } }
        reject { NativeWorkoutSession.installPrepared(context, frame()) }
        reject { ActiveWorkoutStore.saveOwned(context, frame()) }
        assertTrue(NativeDataReplacement.blocked(context))
    }
    @Test fun ownershipMessagesCannotAdvanceTheJournalInsideAReplacementLease() = runTest {
        val journal = PhoneOwnership193.journal(context)
        val before = journal.read().toString()
        NativeDataReplacement.exclusively(context) {
            reject { PhoneOwnership193.receive(context, JSONObject(), "watch", "phone") }
            assertEquals(before, journal.read().toString())
        }
    }

    @Test fun continuationCannotBeIssuedWithoutAnActiveReplacementLease() = runTest {
        reject { NativeDataReplacement.continuationAfterReplacement() }
        NativeDataReplacement.mutate(context) {
            reject { NativeDataReplacement.continuationAfterReplacement() }
        }
    }

    @Test fun continuationSurvivesOrdinaryEditsButNotTheNextRestoreOrErase() = runTest {
        val continuation = NativeDataReplacement.exclusively(context) {
            NativeDataReplacement.continuationAfterReplacement().also {
                assertThrows(IllegalStateException::class.java) { it.requireCurrent(context) }
            }
        }
        NativeDataReplacement.mutate(context) { continuation.requireCurrent(context) }
        NativeDataReplacement.exclusively(context) {
            assertThrows(IllegalStateException::class.java) { continuation.requireCurrent(context) }
        }
        assertThrows(IllegalStateException::class.java) { continuation.requireCurrent(context) }
    }
}
