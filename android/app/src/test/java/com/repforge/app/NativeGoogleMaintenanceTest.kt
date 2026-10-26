package com.repforge.app

import android.app.Activity
import android.content.Context
import com.repforge.app.ui.settings.NativeGoogleMaintenance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Fake deletion transport is confined to tests. No real Google account or HTTP is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeGoogleMaintenanceTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private fun request() = NativeGoogleMaintenance.Request(NativeGoogleMaintenance.Kind.DRIVE,
        "owner", "a@example.invalid", NativeDataReplacement.continuationForCurrentData(context))

    @Test fun accountSwitchOrEmailChangeRejectsOldConfirmation() {
        val request = request()
        assertThrows(IllegalStateException::class.java) { request.requireAccount("other", request.email) }
        assertThrows(IllegalStateException::class.java) { request.requireAccount(request.sub, "b@example.invalid") }
        request.requireAccount(request.sub, request.email)
    }
    @Test fun disconnectReconnectToSameAccountInvalidatesPreview() {
        val request = request()
        assertThrows(IllegalStateException::class.java) {
            request.requireAccount(request.sub, request.email, request.accountEpoch + 1)
        }
    }
    @Test fun replacementFenceRejectsOldConfirmation() {
        val request = request()
        val prefs = context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
        try {
            prefs.edit().putBoolean("blocked", true).commit()
            assertThrows(IllegalStateException::class.java) { request.dataset.requireCurrent(context) }
        } finally { prefs.edit().remove("blocked").commit() }
    }
    @Test fun deniedAdmissionNeverDeletesAnything() = runTest {
        var removed = 0
        val report = NativeGoogleMaintenance.deleteSequentially(listOf("a"), { error("denied") }) { removed++ }
        assertEquals(0, removed); assertFalse(report.complete); assertEquals(0, report.completed)
        assertFalse(report.uncertain)
    }
    @Test fun cancelStopsWithoutTurningIntoSuccess() = runTest {
        var removed = 0
        val error = runCatching {
            NativeGoogleMaintenance.deleteSequentially(listOf("a"), { throw CancellationException("cancel") }) { removed++ }
        }.exceptionOrNull()
        assertTrue(error is CancellationException); assertEquals(0, removed)
    }
    @Test fun partialDeleteReportsConfirmedCountAndUncertainRequestAndStops() = runTest {
        val removed = mutableListOf<String>()
        val report = NativeGoogleMaintenance.deleteSequentially(listOf("a", "b", "c"), {}) {
            if (it == "b") error("timeout") else removed.add(it)
        }
        assertEquals(listOf("a"), removed); assertEquals(1, report.completed)
        assertFalse(report.complete); assertTrue(report.uncertain)
    }
    @Test fun newAdmissionFailureRetainsEarlierConfirmedCount() = runTest {
        var checks = 0
        val report = NativeGoogleMaintenance.deleteSequentially(listOf("a", "b"), { check(++checks == 1) }) {}
        assertEquals(1, report.completed); assertFalse(report.complete); assertFalse(report.uncertain)
    }
    @Test fun retryOnlyRunsWhenExplicitlyInvokedAndDeduplicatesIds() = runTest {
        var failed = true
        var calls = 0
        val remove: suspend (String) -> Unit = { calls++; if (failed) error("offline") }
        val first = NativeGoogleMaintenance.deleteSequentially(listOf("a", "a"), {}, remove)
        assertFalse(first.complete); assertEquals(1, calls)
        failed = false
        val second = NativeGoogleMaintenance.deleteSequentially(listOf("a", "a"), {}, remove)
        assertTrue(second.complete); assertEquals(1, second.completed); assertEquals(2, calls)
    }
    @Test fun sharedGateRejectsAllMaintenanceBeforeNetworkWithoutNestedLock() = runTest {
        val gate = NativeGoogleOperationGate()
        val repo = GoogleSyncRepository(context, gate)
        gate.run {
            assertTrue(repo.deleteAllDriveAppData("not-a-token").exceptionOrNull() is NativeGoogleOperationGate.Busy)
            assertTrue(repo.deleteManagedCalendars("not-a-token").exceptionOrNull() is NativeGoogleOperationGate.Busy)
            assertTrue(repo.revokeAccess(Activity()).exceptionOrNull() is NativeGoogleOperationGate.Busy)
        }
        assertTrue(repo.disconnect().isSuccess)
    }
    @Test fun disconnectedMaintenanceReturnsFailureAndNeverEntersGoogleSdk() = runTest {
        val repo = GoogleSyncRepository(context, NativeGoogleOperationGate())
        repo.disconnect()
        assertTrue(repo.deleteAllDriveAppData("not-a-token").isFailure)
        assertTrue(repo.deleteManagedCalendars("not-a-token").isFailure)
        assertTrue(repo.revokeAccess(Activity()).isFailure)
        assertFalse(repo.uiState.value.isBusy)
    }
    @Test fun pausedMaintenanceCannotBeImmediatelyResurrectedByTokenSync() = runTest {
        val prefs = context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("maintenancePaused", true).commit()
        try {
            val repo = GoogleSyncRepository(context, NativeGoogleOperationGate())
            assertTrue(repo.syncDriveCloud("must-not-leave-test").isFailure)
            assertTrue(repo.syncCalendarCloud("must-not-leave-test").isFailure)
            assertTrue(prefs.getBoolean("maintenancePaused", false))
        } finally { prefs.edit().clear().commit() }
    }
}
