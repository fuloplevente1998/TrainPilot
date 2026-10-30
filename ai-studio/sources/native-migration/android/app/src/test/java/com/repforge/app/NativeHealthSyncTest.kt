package com.repforge.app

import com.repforge.app.ui.health.NativeHealthAutoRefreshGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Provider-boundary fakes exercise orchestration; no SDK or physical success is claimed. */
class NativeHealthSyncTest {
    private val today = LocalDate.parse("2026-10-26")
    private val at = Instant.parse("2026-10-26T12:00:00Z")
    private class Expired : IllegalArgumentException()
    private class Fake : NativeHealthSync.Provider, NativeHealthSync.Journal {
        var saved = NativeHealthSync.Checkpoint()
        var supportsChanges = true
        var created = 0
        var attempts = 0
        var commits = 0
        var committedFull = false
        val reads = mutableListOf<LocalDate>()
        val polled = mutableListOf<String>()
        val dirty = mutableSetOf<LocalDate>()
        val events = mutableListOf<String>()
        var onRead: suspend (LocalDate) -> Boolean = { true }
        var onIdentity: () -> Unit = {}
        var onPoll: suspend (String) -> NativeHealthSync.ChangePage = {
            NativeHealthSync.ChangePage("next", false, emptySet())
        }
        override val changesSupported get() = supportsChanges
        override suspend fun createToken(): String { events.add("token"); return "new-${++created}" }
        override suspend fun pollPage(token: String): NativeHealthSync.ChangePage {
            polled.add(token)
            events.add("apply")
            return onPoll(token).also { dirty.addAll(it.days) }
        }
        override fun invalidToken(error: Throwable) = error is Expired
        override suspend fun readDay(day: LocalDate): Boolean {
            reads.add(day); events.add("read"); return onRead(day)
        }
        override suspend fun requireIdentity() = onIdentity()
        override suspend fun checkpoint() = saved
        override suspend fun dirtyDays(): Set<LocalDate> = dirty
        override suspend fun attempt(at: Instant) { attempts++; events.add("attempt") }
        override suspend fun commit(expected: NativeHealthSync.Checkpoint, next: NativeHealthSync.Checkpoint,
            full: Boolean, at: Instant) {
            check(expected === saved) { "Checkpoint CAS" }
            saved = next; commits++; committedFull = full; events.add("commit")
        }
    }
    private suspend fun sync(f: Fake, full: Boolean = false, scope: String = "scope", guard: () -> Unit = {},
        progress: suspend (NativeHealthSync.Progress) -> Unit = {}): NativeHealthSync.Report =
        NativeHealthSync.run(f, f, scope, today, full, guard, progress) { at }

    @Test fun thirtyLocalDatesIncludeTodayAndSpringAndAutumnDst() {
        val zone = ZoneId.of("Europe/Budapest")
        for ((date, hours) in listOf("2026-03-29" to 23L, "2026-10-25" to 25L)) {
            val day = LocalDate.parse(date)
            val dates = NativeHealthSync.recentDays(day.plusDays(1), 30)
            assertEquals(30, dates.distinct().size)
            assertEquals(day.minusDays(28), dates.first())
            assertEquals(day.plusDays(1), dates.last())
            val window = NativeHealthSync.window(day, zone, day.plusDays(2).atStartOfDay(zone).toInstant())
            assertEquals(hours, Duration.between(window.first, window.second).toHours())
        }
        assertEquals(at, NativeHealthSync.window(today, zone, at).second)
    }

    @Test fun firstFullCapturesTokenBeforeSnapshotAndCommitsAfterCatchup() = runTest {
        val f = Fake()
        val report = sync(f)
        assertTrue(report.successful); assertTrue(report.full)
        assertEquals(NativeHealthSync.recentDays(today, 30), f.reads)
        assertTrue(f.events.indexOf("token") < f.events.indexOf("read"))
        assertTrue(f.events.indexOf("apply") < f.events.indexOf("commit"))
        assertEquals("next", f.saved.token); assertEquals(1, f.commits)
    }

    @Test fun incrementalAppliesAllPagesRereadsDirtyAndDeduplicatesDays() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        val changed = today.minusDays(10)
        f.dirty.add(changed)
        f.onPoll = { token ->
            if (token == "old") NativeHealthSync.ChangePage("page2", true, setOf(changed))
            else NativeHealthSync.ChangePage("end", false, setOf(changed, today))
        }
        val report = sync(f)
        assertFalse(report.full); assertEquals(listOf("old", "page2"), f.polled)
        assertEquals(1, f.reads.count { it == changed })
        assertEquals(3, f.reads.size); assertEquals("end", f.saved.token)
    }

    @Test fun fullCatchesADeletionArrivingDuringSnapshotBeforeCheckpoint() = runTest {
        val f = Fake()
        f.onPoll = { NativeHealthSync.ChangePage("after-delete", false, setOf(today.minusDays(20))) }
        sync(f)
        assertEquals(2, f.reads.count { it == today.minusDays(20) })
        assertEquals("after-delete", f.saved.token)
    }

    @Test fun expiredTokenFallsBackToFreshThirtyDaySnapshot() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "expired", true) }
        f.onPoll = { token -> if (token == "expired") throw Expired() else NativeHealthSync.ChangePage("valid", false, emptySet()) }
        assertTrue(sync(f).full)
        assertEquals(30, f.reads.size); assertEquals(1, f.created); assertEquals("valid", f.saved.token)
    }

    @Test fun changedPermissionTypeZoneOrProviderScopeNeverPollsOldToken() = runTest {
        for (scope in listOf("new-grants", "new-types", "new-zone", "new-provider")) {
            val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
            assertTrue(sync(f, scope = scope).full)
            assertFalse(f.polled.contains("old")); assertEquals(scope, f.saved.scope)
        }
    }

    @Test fun missingTokenAndExplicitFullEachReadThirtyDays() = runTest {
        val missing = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "", true) }
        assertTrue(sync(missing).full)
        val explicit = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        assertTrue(sync(explicit, full = true).full)
        assertEquals(30, explicit.reads.size); assertFalse(explicit.polled.contains("old"))
    }

    @Test fun middlePageFailureDoesNotAdvanceAndRetryReplaysAppliedPage() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        var failed = false
        f.onPoll = { token ->
            if (token == "old") NativeHealthSync.ChangePage("page2", true, setOf(today.minusDays(5)))
            else if (!failed) { failed = true; error("network failure") }
            else NativeHealthSync.ChangePage("end", false, emptySet())
        }
        assertTrue(runCatching { sync(f) }.isFailure)
        assertEquals("old", f.saved.token); assertEquals(0, f.commits)
        assertTrue(sync(f).successful)
        assertEquals(listOf("old", "page2", "old", "page2"), f.polled)
        assertTrue(f.reads.contains(today.minusDays(5)))
    }

    @Test fun partialGrantOrFailedDayKeepsOldCheckpointAndAllowsRetry() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        f.onRead = { it != today }
        val report = sync(f)
        assertEquals(listOf(today), report.failedDays); assertEquals(0, f.commits)
        assertEquals("old", f.saved.token); assertEquals(1, f.attempts)
        f.onRead = { true }
        assertTrue(sync(f).successful); assertEquals(1, f.commits)
    }

    @Test fun cancellationAfterDurablePageNeverAcknowledgesIt() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        f.onRead = { throw CancellationException("cancel") }
        assertTrue(runCatching { sync(f) }.exceptionOrNull() is CancellationException)
        assertEquals("old", f.saved.token); assertEquals(0, f.commits)
    }

    @Test fun oldResponseAfterSceneRestoreOrWorkoutChangeCannotCommit() = runTest {
        val f = Fake()
        var current = true
        f.onRead = { current = false; true }
        assertTrue(runCatching { sync(f, guard = { check(current) }) }.isFailure)
        assertEquals(0, f.commits)
    }

    @Test fun permissionChangeBeforeCheckpointRejectsOtherwiseSuccessfulReads() = runTest {
        val f = Fake().apply { supportsChanges = false }
        f.onIdentity = { check(f.reads.size < 30) { "permissions revoked" } }
        assertTrue(runCatching { sync(f) }.isFailure); assertEquals(0, f.commits)
    }

    @Test fun samsungFirstFullThenTwoDaysWithoutAnyChangeLogCall() = runTest {
        val f = Fake().apply { supportsChanges = false }
        assertTrue(sync(f).full); assertTrue(f.committedFull); assertEquals(30, f.reads.size)
        f.reads.clear()
        assertFalse(sync(f).full); assertEquals(NativeHealthSync.recentDays(today, 2), f.reads)
        assertEquals(0, f.created); assertTrue(f.polled.isEmpty()); assertEquals("", f.saved.token)
    }

    @Test fun progressCountsOnlyFinishedProviderWork() = runTest {
        val f = Fake().apply { supportsChanges = false }
        val values = mutableListOf<NativeHealthSync.Progress>()
        sync(f, progress = { p ->
            if (p.stage == NativeHealthSync.Stage.DAYS) assertEquals(f.reads.size, p.completed)
            values.add(p)
        })
        assertEquals(0, values.first { it.stage == NativeHealthSync.Stage.DAYS }.completed)
        assertEquals(30, values.last().completed)
    }

    @Test fun historySelectionChangeInvalidatesFlightWithoutOverwritingTheNewQuery() = runTest {
        val f = Fake()
        val selected = today.minusDays(50)
        val initial = com.repforge.app.ui.health.NativeHealthQuery(selected,
            java.time.YearMonth.from(selected), "health_connect")
        var query = initial
        f.onRead = {
            query = query.copy(day = selected.minusDays(1), channel = "journal")
            true
        }
        assertTrue(runCatching { sync(f, guard = { check(query == initial) }) }.isFailure)
        assertEquals(selected.minusDays(1), query.day)
        assertEquals("journal", query.channel)
        assertEquals(0, f.commits)
    }

    @Test fun productionGateSharesAutoManualFlightUntilCancelledSdkHasUnwound() {
        val gate = NativeHealthAutoRefreshGate()
        val snapshot = NativeHealthAutoRefreshGate.Snapshot("health_connect", today.toString(), 4, 2)
        val auto = gate.begin(NativeHealthAutoRefreshGate.Trigger.AUTO, 0, true, true, true, snapshot)!!
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 1, true, true, true, snapshot))
        gate.invalidate()
        assertFalse(gate.isCurrent(auto, snapshot, true, true))
        assertNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 2, true, true, true, snapshot))
        gate.finish(auto)
        assertNotNull(gate.begin(NativeHealthAutoRefreshGate.Trigger.MANUAL, 3, true, true, true, snapshot))
    }

    @Test fun deletionWithoutKnownLocalDayFallsBackToFullWindow() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        f.onPoll = { NativeHealthSync.ChangePage("after-delete", false, emptySet(), deleted = true) }
        assertTrue(sync(f).full)
        assertEquals(30, f.reads.size); assertEquals(1, f.created)
        assertEquals("after-delete", f.saved.token)
    }

    @Test fun nonAdvancingPageDoesNotCommit() = runTest {
        val f = Fake().apply { saved = NativeHealthSync.Checkpoint("scope", "old", true) }
        f.onPoll = { NativeHealthSync.ChangePage(it, true, emptySet()) }
        assertTrue(runCatching { sync(f) }.isFailure); assertEquals(0, f.commits)
    }
}
