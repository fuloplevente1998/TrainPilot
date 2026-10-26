package com.repforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/** Real SQLite durable state, token privacy, tombstones and restore CAS; no provider fake. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NativeHealthSyncJournalTest {
    private lateinit var store: HealthJournalStore
    private lateinit var context: Context
    private val day = LocalDate.parse("2026-10-24")
    private val at get() = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("health-journal.db")
        store = HealthJournalStore(context)
    }
    @After fun close() { store.close() }
    private fun checkpoint(token: String = "private-test-token", full: Boolean = true) {
        val old = store.nativeSyncState(HealthJournalStore.HC, store.generation())
        store.nativeSyncCheckpoint(HealthJournalStore.HC, store.generation(), old.revision,
            NativeHealthSync.Checkpoint("scope", token, true), full, "2026-10-26T12:00:00Z")
    }
    private fun record(modified: Long = at): JSONObject = JSONObject()
        .put("channel", HealthJournalStore.HC).put("source", "com.example.provider")
        .put("type", "StepsRecord").put("recordId", "stable-id").put("lastModifiedMs", modified)
        .put("startMs", at).put("endMs", at + 1000).put("day", day.toString())
        .put("data", JSONObject().put("value", 123).put("unit", "steps"))

    @Test fun privateTokenSurvivesReopenButNeverProjectionPageOrExport() {
        checkpoint()
        assertFalse(store.exportSnapshot().toString().contains("private-test-token"))
        assertFalse(store.projection().toString().contains("private-test-token"))
        assertFalse(store.page("2026-10-01", "2026-10-31", "journal", "", 0, 0, 10).toString().contains("private-test-token"))
        store.close(); store = HealthJournalStore(context)
        assertEquals("private-test-token", store.nativeSyncState(HealthJournalStore.HC, store.generation()).token)
        assertEquals(1L, store.nativeSyncState(HealthJournalStore.HC, store.generation()).revision)
    }

    @Test fun revisionCasRejectsLateCheckpointWithoutChangingSuccessTimes() {
        checkpoint()
        val before = store.exportSnapshot().getJSONObject("syncMeta").toString()
        assertThrows(IllegalStateException::class.java) {
            store.nativeSyncCheckpoint(HealthJournalStore.HC, store.generation(), 0,
                NativeHealthSync.Checkpoint("scope", "late"), true, "2026-10-27T12:00:00Z")
        }
        assertEquals(before, store.exportSnapshot().getJSONObject("syncMeta").toString())
        assertEquals("private-test-token", store.nativeSyncState(HealthJournalStore.HC, store.generation()).token)
    }

    @Test fun attemptOrPartialReadDoesNotClearLastGoodSuccessOrFullTime() {
        checkpoint()
        val before = store.exportSnapshot().getJSONObject("syncMeta")
        store.nativeSyncAttempt(store.generation(), "2026-10-27T12:00:00Z")
        store.healthDay(day.toString(), JSONObject().put("steps", 123)
            .put("warnings", JSONArray().put("Partial permission")), store.generation())
        val after = store.exportSnapshot().getJSONObject("syncMeta")
        assertEquals(before.getString("lastSyncAt"), after.getString("lastSyncAt"))
        assertEquals(before.getString("lastFullSyncAt"), after.getString("lastFullSyncAt"))
        assertEquals("2026-10-27T12:00:00Z", after.getString("lastAttemptAt"))
    }

    @Test fun successfulIncrementalAdvancesLastSyncButRetainsLastFull() {
        checkpoint()
        store.nativeSyncCheckpoint(HealthJournalStore.HC, store.generation(), 1,
            NativeHealthSync.Checkpoint("scope", "incremental"), false, "2026-10-27T12:00:00Z")
        val meta = store.exportSnapshot().getJSONObject("syncMeta")
        assertEquals("2026-10-26T12:00:00Z", meta.getString("lastFullSyncAt"))
        assertEquals("2026-10-27T12:00:00Z", meta.getString("lastSyncAt"))
    }

    @Test fun restorePendingAndCompletedRejectOldGenerationAndDiscardItsToken() {
        checkpoint()
        val generation = store.generation()
        val backup = store.exportSnapshot()
        val restore = store.prepareRestore(backup)
        assertThrows(IllegalStateException::class.java) {
            store.nativeSyncCheckpoint(HealthJournalStore.HC, generation, 1,
                NativeHealthSync.Checkpoint("scope", "late"), false, "2026-10-27T12:00:00Z")
        }
        store.installRestore(restore); store.finishRestore(restore, true)
        assertEquals("", store.nativeSyncState(HealthJournalStore.HC, store.generation()).token)
        assertThrows(IllegalStateException::class.java) { store.nativeSyncAttempt(generation, "late") }
    }

    @Test fun providerSwitchAndEraseInvalidatePrivateCheckpoint() {
        checkpoint()
        store.setPreferences(JSONObject().put("provider", HealthJournalStore.SAMSUNG))
        assertFalse(store.nativeSyncState(HealthJournalStore.HC, store.generation()).initialized)
        store.erase()
        assertEquals("", store.nativeSyncState(HealthJournalStore.HC, store.generation()).token)
    }

    @Test fun replayedChangePagesAreIdempotentAndDeletedRecordDoesNotResurrect() {
        val epoch = store.generation()
        val records = JSONArray().put(record())
        store.applyChanges(records, JSONArray(), epoch)
        store.applyChanges(records, JSONArray(), epoch)
        assertEquals(1, store.exportSnapshot().getJSONArray("records").length())
        val deletion = JSONArray().put(JSONObject().put("recordId", "stable-id").put("deletedMs", at + 2000))
        store.applyChanges(JSONArray(), deletion, epoch)
        store.applyChanges(records, JSONArray(), epoch) // Older SDK response arrives after deletion.
        store.applyChanges(JSONArray(), deletion, epoch)
        assertEquals(0, store.exportSnapshot().getJSONArray("records").length())
        assertEquals(1, store.exportSnapshot().getJSONArray("deletions").length())
        assertTrue(store.nativeSyncDirtyDays().contains(day))
    }

    @Test fun deletionAndMovedIntervalsDirtyAllPreviouslyCoveredDays() {
        val epoch = store.generation()
        val end = day.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val interval = record().put("type", "SleepSessionRecord").put("endMs", end)
        store.applyChanges(JSONArray().put(interval), JSONArray(), epoch)
        for (d in NativeHealthSync.recentDays(day.plusDays(2), 3))
            store.healthDay(d.toString(), JSONObject().put("steps", 1), epoch) // Clear initial dirty marks.
        assertTrue(store.nativeSyncDirtyDays().isEmpty())
        store.applyChanges(JSONArray(), JSONArray().put(JSONObject().put("recordId", "stable-id")
            .put("deletedMs", end + 1000)), epoch)
        assertEquals(NativeHealthSync.recentDays(day.plusDays(2), 3).toSet(), store.nativeSyncDirtyDays())
    }

    @Test fun failedPageTransactionRollsBackBothUpsertsAndDeletionMarks() {
        val epoch = store.generation()
        store.applyChanges(JSONArray().put(record()), JSONArray(), epoch)
        val bad = record(at + 1).put("channel", "invalid")
        assertThrows(IllegalArgumentException::class.java) {
            store.applyChanges(JSONArray().put(bad), JSONArray().put(JSONObject()
                .put("recordId", "stable-id").put("deletedMs", at + 2000)), epoch)
        }
        assertEquals(1, store.exportSnapshot().getJSONArray("records").length())
        assertEquals(0, store.exportSnapshot().getJSONArray("deletions").length())
    }

    @Test fun preDeleteDailyResponseCannotReinsertRawDataOrPublishStaleAggregate() {
        val epoch = store.generation()
        store.applyChanges(JSONArray().put(record()), JSONArray(), epoch)
        val revision = store.nativeChangeRevision(epoch)
        store.applyChanges(JSONArray(), JSONArray().put(JSONObject().put("recordId", "stable-id")
            .put("deletedMs", at + 2000)), epoch)
        assertThrows(IllegalStateException::class.java) {
            store.nativePublishAtChangeRevision(epoch, revision) {
                store.reconcileRecords(JSONArray().put(record()), "StepsRecord", at, at + 1000, at + 3000, epoch)
                store.healthDay(day.toString(), JSONObject().put("steps", 123), epoch)
            }
        }
        assertEquals(0, store.exportSnapshot().getJSONArray("records").length())
        assertEquals(0, store.exportSnapshot().getJSONArray("days").length())
    }

    @Test fun existingPollPublisherRejectsInvalidationBeforeApplyingChangePage() {
        val epoch = store.generation()
        val access = HealthConnectRepository.Access(context, epoch, Runnable {})
        access.readCommit = { writer -> store.erase(); writer() }
        assertThrows(IllegalStateException::class.java) {
            access.publishRead { store.applyChanges(JSONArray().put(record()), JSONArray(), epoch) }
        }
        assertEquals(0, store.exportSnapshot().getJSONArray("records").length())
    }
}
