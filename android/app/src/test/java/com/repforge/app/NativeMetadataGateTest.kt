package com.repforge.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeMetadataGateTest {
    private lateinit var context: Context
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    @Before fun open() {
        Dispatchers.setMain(StandardTestDispatcher())
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        NativeReplacementJournal(context).clear()
    }
    @After fun close() {
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    @Test fun alreadyAdmittedWriterCannotBeReplacedHalfwayThroughItsWork() = runTest {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var finished = false
        val writer = launch { NativeDataReplacement.mutate(context) { entered.complete(Unit); release.await(); finished = true } }
        entered.await()
        var replaced = false
        try { NativeDataReplacement.exclusively(context) { replaced = true } } catch (_: IllegalStateException) { }
        assertFalse(replaced); assertFalse(finished)
        release.complete(Unit); writer.join()
        NativeDataReplacement.exclusively(context) { assertTrue(finished) }
    }
    @Test fun independentWriterDuringReplacementIsRejectedWithoutRunning() = runTest {
        var called = false
        NativeDataReplacement.exclusively(context) {
            val writer = backgroundScope.async {
                try { NativeDataReplacement.mutate(context) { called = true } } catch (_: IllegalStateException) { }
            }
            writer.await()
            assertFalse(called)
        }
        assertFalse(called)
    }
    @Test fun nestedImporterAndSettingsCallbacksShareTheSameAdmittedWriterWithoutDeadlock() = runTest {
        var count = 0
        withTimeout(5000) {
            NativeDataReplacement.mutate(context) {
                NativeDataReplacement.mutate(context) { count++ }
                withContext(Dispatchers.IO) { NativeDataReplacement.mutate(context) { count++ } }
            }
        }
        assertEquals(2, count)
    }
    @Test fun replacementOwnImporterCanWriteWhileIndependentCallbacksStayBlocked() = runTest {
        var own = false
        NativeDataReplacement.exclusively(context) {
            NativeDataReplacement.mutate(context) { own = true }
            assertTrue(NativeDataReplacement.blocked(context))
        }
        assertTrue(own); assertFalse(NativeDataReplacement.blocked(context))
    }
    @Test fun writerCancellationReleasesAdmissionForLaterReplacement() = runTest {
        val entered = CompletableDeferred<Unit>()
        val writer = launch { NativeDataReplacement.mutate(context) { entered.complete(Unit); awaitCancellation() } }
        entered.await(); writer.cancelAndJoin()
        NativeDataReplacement.exclusively(context) { assertTrue(NativeDataReplacement.blocked(context)) }
        assertFalse(NativeDataReplacement.blocked(context))
    }
}
