package com.repforge.app

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeGoogleRepositoryAdmissionTest {
    private lateinit var context: Context
    private val dispatcher = StandardTestDispatcher()
    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
    }
    @After fun close() {
        context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).edit().clear().commit()
        Dispatchers.resetMain()
    }
    private fun connectFixture() {
        val profile = JSONObject().put("sub", "subject-a").put("email", "a@example.invalid")
        assertTrue(context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).edit()
            .putString("profile", profile.toString()).putString("sub", "subject-a")
            .putString("email", "a@example.invalid").putString("calendar", "owned-calendar").commit())
    }

    @Test fun everyPublicGoogleOperationRejectsAnotherRepositoryLeaseBeforeNetworkOrAccountChange() = runTest(dispatcher) {
        connectFixture()
        val gate = NativeGoogleOperationGate()
        val first = GoogleSyncRepository(context, gate)
        val second = GoogleSyncRepository(context, gate)
        gate.run {
            val results = listOf(first.syncDriveCloud("must-not-leave-test"), second.syncCalendarCloud("must-not-leave-test"),
                second.deleteAllDriveAppData("must-not-leave-test"), first.deleteManagedCalendars("must-not-leave-test"),
                second.completeConnectWithToken("must-not-leave-test"), first.revokeAccess(Activity()), second.disconnect())
            results.forEach { assertTrue(it.exceptionOrNull() is NativeGoogleOperationGate.Busy) }
            assertTrue(second.authorizeOperation(Activity(), "connect") is GoogleAuthOutcome.Failed)
            assertEquals("subject-a", context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).getString("sub", ""))
            assertFalse(first.uiState.value.isBusy); assertFalse(second.uiState.value.isBusy)
        }
        assertTrue(second.disconnect().isSuccess)
    }

    @Test fun disconnectedDataOperationsFailBeforeUserInfoOrDataHttpAndClearBusyFeedback() = runTest(dispatcher) {
        val repo = GoogleSyncRepository(context)
        for (result in listOf(repo.syncDriveCloud("not-a-token"), repo.syncCalendarCloud("not-a-token"),
            repo.deleteAllDriveAppData("not-a-token"), repo.deleteManagedCalendars("not-a-token"))) {
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.cause is IllegalStateException)
        }
        assertFalse(repo.uiState.value.isBusy); assertEquals("", repo.uiState.value.activeOperation)
        assertNotNull(repo.uiState.value.errorMessage)
        assertTrue(repo.disconnect().isSuccess)
    }

    @Test fun disconnectedAuthorizationCannotOpenAccountConsentForADataOperation() = runTest(dispatcher) {
        val repo = GoogleSyncRepository(context)
        assertTrue(repo.authorizeOperation(Activity(), "drive") is GoogleAuthOutcome.Failed)
        assertFalse(repo.uiState.value.isBusy)
    }

    @Test fun successfulDisconnectRemovesPersistedIdentityAndCalendarCacheAcrossInstances() = runBlocking {
        connectFixture()
        val first = GoogleSyncRepository(context)
        val second = GoogleSyncRepository(context)
        assertEquals("subject-a", second.uiState.value.connectedSub)
        assertTrue(first.disconnect().isSuccess)
        second.refreshProfileState()
        assertEquals("", second.uiState.value.connectedSub)
        assertEquals("", second.uiState.value.connectedEmail)
        assertFalse(context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).contains("calendar"))
        assertTrue(context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE).all.isEmpty())
    }
}
