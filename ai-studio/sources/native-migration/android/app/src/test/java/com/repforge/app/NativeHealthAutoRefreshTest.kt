package com.repforge.app

import com.repforge.app.ui.health.NativeHealthAutoRefreshGate
import org.junit.Assert.*
import org.junit.Test

/** Pure admission tests, not a physical HC or Samsung SDK test. */
class NativeHealthAutoRefreshTest {
    private val today = NativeHealthAutoRefreshGate.Snapshot(
        provider = HealthJournalStore.HC,
        day = "2026-10-25",
        datasetGeneration = 4L,
        screenGeneration = 1L
    )

    @Test fun optOutNeverAutomaticallyStartsButManualStillCan() {
        val gate = NativeHealthAutoRefreshGate()
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 0, false, true, true, today))
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 0, false, true, true, today))
    }

    @Test fun firstOpenStartsAndTenMinutesThrottlesSubsequentAuto() {
        val gate = NativeHealthAutoRefreshGate()
        val first = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 1_000, true, true, true, today)
        assertNotNull(first)
        gate.finish(first!!)
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 600_999, true, true, true, today))
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 601_000, true, true, true, today))
    }

    @Test fun manualBypassesThrottleWithoutExtendingAutoThrottle() {
        val gate = NativeHealthAutoRefreshGate()
        val first = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 200, true, true, true, today)!!
        gate.finish(first)
        val manual = gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 250, false, true, true, today)!!
        gate.finish(manual)
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 599_999, true, true, true, today))
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 600_200, true, true, true, today))
    }

    @Test fun partialReadGrantIsAdmittedWithoutInventingMissingMetrics() {
        // permitted means at least one real provider read permission; partial fields remain the provider's responsibility.
        val gate = NativeHealthAutoRefreshGate()
        val partial = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 10L, true, true, true, today)
        assertNotNull(partial)
        gate.finish(partial!!)
        val missing = gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 20L, true, false, true, today)
        assertNull(missing)
    }

    @Test fun failedAttemptStillThrottlesAutomaticRetryButManualIsImmediate() {
        val gate = NativeHealthAutoRefreshGate()
        val attempt = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 5_000L, true, true, true, today)!!
        gate.finish(attempt) // failure is reported by the provider; it must never be fabricated as success
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 5_001L, true, true, true, today))
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 5_002L, true, true, true, today))
    }

    @Test fun cooldownIsScopedToActualProviderAndLocalReadDay() {
        val gate = NativeHealthAutoRefreshGate()
        val first = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 500L, true, true, true, today)!!
        gate.finish(first)
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 600L, true, true, true, today))
        val nextDay = today.copy(day = "2026-10-26")
        val newDayTicket = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 600L, true, true, true, nextDay)!!
        gate.finish(newDayTicket)
        val newSource = today.copy(provider = HealthJournalStore.SAMSUNG)
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 601L, true, true, true, newSource))
    }

    @Test fun concurrentTriggersCannotStartTwoProviderRequests() {
        val gate = NativeHealthAutoRefreshGate()
        val auto = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 1L, true, true, true, today)!!
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 2L, true, true, true, today))
        gate.invalidate()
        assertFalse(gate.isCurrent(auto, today, true, true))
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 3L, true, true, true, today))
        gate.finish(auto)
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 4L, true, true, true, today))
    }

    @Test fun absentPermissionOfflineOrActiveSessionBlockAutoWithoutConsumingThrottle() {
        val gate = NativeHealthAutoRefreshGate()
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 0, true, false, true, today))
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 10, true, true, false, today))
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 20, true, false, true, today))
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 30, true, true, true, today))
    }

    @Test fun lateCompletionCannotClaimAnotherProviderDateOrDataset() {
        val gate = NativeHealthAutoRefreshGate()
        val ticket = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 0, true, true, true, today)!!
        assertTrue(gate.isCurrent(ticket, today, true, true))
        assertFalse(gate.isCurrent(ticket, today.copy(provider = HealthJournalStore.SAMSUNG), true, true))
        assertFalse(gate.isCurrent(ticket, today.copy(day = "2026-10-24"), true, true))
        assertFalse(gate.isCurrent(ticket, today.copy(datasetGeneration = 5), true, true))
        assertFalse(gate.isCurrent(ticket, today.copy(screenGeneration = 2), true, true))
        assertFalse(gate.isCurrent(ticket, today, false, true))
        assertFalse(gate.isCurrent(ticket, today, true, false))
        gate.invalidate() // disposed screen / activity recreation / navigation
        assertFalse(gate.isCurrent(ticket, today, true, true))
        gate.finish(ticket)
    }
}
