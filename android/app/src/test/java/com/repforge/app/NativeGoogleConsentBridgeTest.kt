package com.repforge.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeGoogleConsentBridgeTest {
    private fun pending(): PendingIntent {
        val context: Context = RuntimeEnvironment.getApplication()
        return PendingIntent.getActivity(context, 0, Intent("com.repforge.app.TEST_CONSENT"), PendingIntent.FLAG_IMMUTABLE)
    }

    @Test fun onlyAnExplicitlyLaunchedMatchingPromptCanAcceptAResult() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val prompt = bridge.prompt.value!!
        assertFalse(bridge.complete(prompt.id, Intent()))
        assertNull(bridge.takeForLaunch("other"))
        assertSame(prompt, bridge.takeForLaunch(prompt.id))
        val verifiedResult = Intent("verified-result")
        assertTrue(bridge.complete(prompt.id, verifiedResult))
        assertSame(verifiedResult, result.await())
        assertNull(bridge.prompt.value)
    }

    @Test fun recreationCanObservePendingPromptWithoutLaunchingConsentAgain() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val prompt = bridge.prompt.value!!
        assertNotNull(bridge.takeForLaunch(prompt.id))
        assertEquals(prompt, bridge.prompt.value)
        assertNull(bridge.takeForLaunch(prompt.id))
        assertTrue(bridge.complete(prompt.id, null))
        assertNull(result.await()); assertNull(bridge.prompt.value)
    }

    @Test fun userCancellationReturnsNoTokenOrSuccessfulConsent() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val id = bridge.prompt.value!!.id
        bridge.takeForLaunch(id)
        assertTrue(bridge.complete(id, null)); assertNull(result.await())
        assertFalse(bridge.complete(id, Intent()))
    }

    @Test fun cancelledViewModelAttemptCannotCompleteOrEraseANewerAttempt() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val first = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val oldId = bridge.prompt.value!!.id
        bridge.takeForLaunch(oldId)
        first.cancelAndJoin(); assertNull(bridge.prompt.value)
        val next = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val newId = bridge.prompt.value!!.id
        assertNotEquals(oldId, newId)
        bridge.takeForLaunch(newId)
        assertFalse(bridge.complete(oldId, Intent()))
        assertFalse(bridge.fail(oldId, IOException("late old failure")))
        assertEquals(newId, bridge.prompt.value!!.id)
        assertTrue(bridge.complete(newId, Intent("new-result")))
        assertEquals("new-result", next.await()!!.action)
    }

    @Test fun duplicateResultsCannotOverwriteTheAcceptedResult() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val id = bridge.prompt.value!!.id
        bridge.takeForLaunch(id)
        assertTrue(bridge.complete(id, Intent("first")))
        assertFalse(bridge.complete(id, Intent("duplicate")))
        assertEquals("first", result.await()!!.action)
    }

    @Test fun launchFailurePropagatesAndReleasesPendingStateForRetry() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val error = IOException("launcher unavailable")
        var actual: Exception? = null
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            try { bridge.resolve(pending()) } catch (failure: IOException) { actual = failure; null }
        }
        val id = bridge.prompt.value!!.id
        bridge.takeForLaunch(id); assertTrue(bridge.fail(id, error))
        assertNull(result.await()); assertSame(error, actual); assertNull(bridge.prompt.value)
    }

    @Test fun competingConsentRequestIsRejectedWithoutDisturbingTheFirst() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val first = async(start = CoroutineStart.UNDISPATCHED) { bridge.resolve(pending()) }
        val id = bridge.prompt.value!!.id
        var rejected = false
        try { bridge.resolve(pending()) } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected); assertEquals(id, bridge.prompt.value!!.id)
        bridge.takeForLaunch(id); bridge.complete(id, null); first.await()
    }

    @Test fun accountOperationAdmissionRemainsHeldUntilConsentIsResolvedOrCancelled() = runTest {
        val bridge = NativeGoogleConsentBridge()
        val gate = NativeGoogleOperationGate()
        val result = async(start = CoroutineStart.UNDISPATCHED) { gate.run { bridge.resolve(pending()) } }
        assertThrows(NativeGoogleOperationGate.Busy::class.java) { gate.runNow { fail("Disconnect must wait") } }
        val id = bridge.prompt.value!!.id
        bridge.takeForLaunch(id); bridge.complete(id, null); result.await()
        assertTrue(gate.runNow { true })
        val retry = async(start = CoroutineStart.UNDISPATCHED) { gate.run { bridge.resolve(pending()) } }
        retry.cancelAndJoin()
        assertNull(bridge.prompt.value); assertTrue(gate.runNow { true })
    }

    @Test fun recreatedProcessHasNoAuthorityToAcceptAnOldActivityResult() = runTest {
        val previous = NativeGoogleConsentBridge()
        val old = async(start = CoroutineStart.UNDISPATCHED) { previous.resolve(pending()) }
        val id = previous.prompt.value!!.id
        previous.takeForLaunch(id); old.cancelAndJoin()
        val recreated = NativeGoogleConsentBridge()
        assertNull(recreated.prompt.value)
        assertFalse(recreated.complete(id, Intent("old-result")))
        assertNull(recreated.takeForLaunch(id))
    }
}
