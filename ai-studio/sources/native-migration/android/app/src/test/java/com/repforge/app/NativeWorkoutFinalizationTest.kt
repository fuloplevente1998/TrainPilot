package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutFinalizationTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private val dispatcher = StandardTestDispatcher()
    private val queries = ConcurrentLinkedQueue<String>()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> queries.add(sql) }, Executor { it.run() }).build()
    }

    @After fun close() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }

    private fun draft(id: String = "finish-me") = JSONObject("""{
        "active":true,"timerOwner":"phone","workoutId":"$id","id":"$id","syncId":"$id",
        "started":"2026-10-22T10:00:00Z","currentExercise":0,"generation":1,"executionRevision":0,
        "scheduleId":"planned","activeIntervals":[{"start":"2026-10-22T10:00:00Z","end":null}],
        "exercises":[{"id":"press","loadType":"dumbbell","repUnit":"ism.","sets":[{"setId":"set-a","done":false,"reps":"8","weight":10}]}]
    }""")

    private fun prepare(): JSONObject {
        val source = draft()
        PhoneWorkoutExecution.store(context).prepare(source, 0)
        return ActiveWorkoutStore.saveOwned(context, source)
    }

    private fun record(): JSONObject {
        val execution = PhoneWorkoutExecution.store(context)
        val frame = execution.snapshot()!!
        return execution.record(JSONObject().put("commandId", "native-record").put("action", "completeSet")
            .put("workoutId", "finish-me").put("generation", 1).put("executionRevision", frame.getLong("executionRevision"))
            .put("exerciseId", "press").put("setId", "set-a").put("reps", "9").put("weight", 12.5))
    }

    private suspend fun schedule() {
        db.calendarScheduleDao().upsertEntry(CalendarScheduleEntity("planned", "", "2026-10-22",
            "2026-10-22T10:00:00Z", "2026-10-22T11:00:00Z", "program", "Program", "A", "A", "planned", 1L, "{}"))
    }

    private suspend fun priorPress() {
        com.repforge.app.data.migration.LegacyWebViewMigrator(context, db,
            com.repforge.app.data.store.TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(
            JSONObject().put("id", "earlier-press").put("programId", "program").put("dayId", "A")
                .put("started", "2026-10-20T10:00:00Z").put("finished", "2026-10-20T11:00:00Z")
                .put("exercises", JSONArray().put(JSONObject().put("id", "press").put("repUnit", "ism.")
                    .put("sets", JSONArray().put(JSONObject().put("done", true).put("weight", 10).put("reps", "8"))))))
    }

    @Test fun largeHistoryPrEvaluationStillReconcilesRecordsCompletesCalendarAndClosesNativeFrame() = runTest(dispatcher) {
        db.withTransaction { repeat(1500) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it)) } }
        priorPress(); val input = prepare(); record(); schedule(); queries.clear()
        TrainPilotWorkoutFinalization(context, db).finishWorkout(input)
        val saved = db.workoutHistoryDao().getWorkoutWithDetails("finish-me")!!
        assertTrue(saved.workout.hasPr226)
        assertEquals("maxWeight", JSONArray(saved.workout.prRecordsJson226).getJSONObject(0).getString("kind"))
        assertEquals(1502, db.workoutHistoryDao().getWorkoutCount())
        assertEquals("completed", db.calendarScheduleDao().getEntry("planned")!!.status)
        assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.getBoolean("active"))
        assertEquals(0, PhoneWorkoutExecution.store(context).pendingEvents("finish-me", 1, 0).length())
        assertTrue(queries.none { it.contains("SELECT * FROM workout_history ORDER BY started DESC", true) })
        assertTrue(queries.any { it.contains("AS setRowId") && it.contains("LIMIT") })
    }

    @Test fun pendingRecordingIsSavedBeforeNativeCloseAndCalendarCompletion() = runTest(dispatcher) {
        val input = prepare()
        record()
        schedule()
        TrainPilotWorkoutFinalization(context, db).finishWorkout(input)
        val row = db.workoutHistoryDao().getWorkoutWithDetails("finish-me")!!
        assertEquals(1, row.workout.completedSetsCount)
        assertEquals("9", row.exercises.single().sets.single().reps)
        assertEquals(12.5, row.exercises.single().sets.single().weight!!, 0.0)
        assertEquals("completed", db.calendarScheduleDao().getEntry("planned")!!.status)
        val execution = PhoneWorkoutExecution.store(context)
        assertEquals(0, execution.pendingEvents("finish-me", 1, 0).length())
        assertFalse(execution.snapshot()!!.getBoolean("active"))
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertFalse(execution.commandsSuspended())
    }

    @Test fun calendarFailureRollsBackHistoryKeepsRecordedDraftAndRetryDoesNotDuplicate() = runTest(dispatcher) {
        priorPress()
        val input = prepare()
        record()
        schedule()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER calendar_full BEFORE INSERT ON calendar_entries
            WHEN NEW.status = 'completed' BEGIN SELECT RAISE(ABORT, 'injected disk failure'); END""")
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(input) } catch (_: Exception) { failed = true }
        assertTrue(failed)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertFalse(db.workoutHistoryDao().getWorkoutWithDetails("earlier-press")!!.workout.hasPr226)
        assertEquals("planned", db.calendarScheduleDao().getEntry("planned")!!.status)
        val pending = ActiveWorkoutStore.current(context)!!
        assertTrue(pending.getBoolean("active"))
        assertTrue(pending.getBoolean("finalizationPending"))
        assertTrue(pending.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"))
        val execution = PhoneWorkoutExecution.store(context)
        assertTrue(execution.snapshot()!!.getBoolean("active"))
        assertTrue(execution.commandsSuspended())
        val finished = pending.getString("finished")
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER calendar_full")
        TrainPilotWorkoutFinalization(context, db).finishWorkout(input)
        assertEquals(2, db.workoutHistoryDao().getWorkoutCount())
        assertEquals("maxWeight", JSONArray(db.workoutHistoryDao().getWorkoutWithDetails("finish-me")!!.workout.prRecordsJson226)
            .getJSONObject(0).getString("kind"))
        assertEquals(finished, db.workoutHistoryDao().getWorkoutWithDetails("finish-me")!!.workout.finished)
        assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
    }

    @Test fun conflictingPhoneEditKeepsNativeEventUnacknowledged() = runTest(dispatcher) {
        prepare()
        record()
        val conflict = ActiveWorkoutStore.current(context)!!
        conflict.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps", "77")
        ActiveWorkoutStore.saveOwned(context, conflict)
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(conflict) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
        assertEquals(1, PhoneWorkoutExecution.store(context).pendingEvents("finish-me", 1, 0).length())
        assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
    }

    @Test fun lateWatchFinishCannotClearANewerPhoneWorkoutAndRetryHasOneHistoryRow() = runTest(dispatcher) {
        val active = ActiveWorkoutStore.saveOwned(context, draft("new-phone"))
        val old = draft("old-watch").put("timerOwner", "watch").put("finished", "2026-10-22T11:00:00Z")
        val finalizer = TrainPilotWorkoutFinalization(context, db)
        assertNull(finalizer.finishWorkout(old, fromWatch = true).closedSnapshot)
        assertNull(finalizer.finishWorkout(old, fromWatch = true).closedSnapshot)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertEquals(active.toString(), ActiveWorkoutStore.current(context).toString())
    }

    @Test fun tombstonedWatchWorkoutCannotBeResurrectedOrReportedSaved() = runTest(dispatcher) {
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity("old-watch", "old-watch", JSONArray().put("old-watch").toString(), 1))
        val old = draft("old-watch").put("timerOwner", "watch")
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(old, fromWatch = true) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun pendingDraftRejectsLateSnapshotAndNewWorkoutReplacement() {
        val current = ActiveWorkoutStore.saveOwned(context, draft().put("finalizationPending", true))
        assertThrows(IllegalStateException::class.java) { ActiveWorkoutStore.saveOwned(context, draft()) }
        assertThrows(IllegalStateException::class.java) { ActiveWorkoutStore.saveOwned(context, draft("new")) }
        assertThrows(IllegalStateException::class.java) {
            ActiveWorkoutStore.saveOwned(context, JSONObject(current.toString()).put("currentExercise", 99))
        }
        assertEquals(current.toString(), ActiveWorkoutStore.current(context).toString())
    }

    @Test fun pendingOwnershipTransferCannotBeFinished() = runTest(dispatcher) {
        val input = prepare().put("handoff193", JSONObject().put("state", "prepared"))
        ActiveWorkoutStore.saveOwned(context, input)
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(input) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
        assertTrue(PhoneWorkoutExecution.store(context).snapshot()!!.getBoolean("active"))
        assertFalse(ActiveWorkoutStore.current(context)!!.optBoolean("finalizationPending"))
    }

    @Test fun activeRestoreCannotSaveADetachedWatchWorkout() = runTest(dispatcher) {
        PhoneWorkoutExecution.store(context).setRestoreBlocked(true)
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(draft("watch"), fromWatch = true) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun deletionBeyondRecentTombstoneWindowStillBlocksLateWatchFinish() = runTest(dispatcher) {
        // Alias is not the primary key; the deletion is older than the former 2000-row window.
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity("old-alias", "different-key", "[\"watch\"]", 1))
        db.workoutHistoryDao().upsertTombstones((1..2001).map {
            HistoryTombstoneEntity("newer-$it", "newer-$it", "[\"newer-$it\"]", it + 1L)
        })
        assertFalse(db.workoutHistoryDao().getTombstones().any { it.primaryId == "old-alias" })
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(draft("watch"), fromWatch = true) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }
    @Test fun importedDraftCleanupFailureKeepsNativeSessionRetryableAndArchiveIsRetained() = runTest(dispatcher) {
        val raw="{\"fixture\":\"imported-source\"}"
        val input=prepare().put("legacyDraftSha256", NativeLegacyDraft.hash(raw))
        ActiveWorkoutStore.saveOwned(context,input)
        db.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity("draft",raw,1L))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER cleanup_full BEFORE DELETE ON kv_documents BEGIN SELECT RAISE(ABORT,'synthetic cleanup failure'); END")
        var failed=false
        try { TrainPilotWorkoutFinalization(context,db).finishWorkout(input) } catch (_: Exception) { failed=true }
        assertTrue(failed);assertTrue(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertNotNull(db.keyValueDocumentDao().getDocument("draft"))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER cleanup_full")
        TrainPilotWorkoutFinalization(context,db).finishWorkout(input)
        assertNull(db.keyValueDocumentDao().getDocument("draft"))
        assertEquals(raw,db.keyValueDocumentDao().getDocument("legacyDraftArchive|"+NativeLegacyDraft.hash(raw))!!.json)
        assertEquals(1,db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun newerImportedDraftIsNeverRemovedByOldWorkoutFinalization() = runTest(dispatcher) {
        val input=prepare().put("legacyDraftSha256", NativeLegacyDraft.hash("old-source"))
        ActiveWorkoutStore.saveOwned(context,input)
        val newer="{\"fixture\":\"newer-draft\"}"
        db.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity("draft",newer,2L))
        TrainPilotWorkoutFinalization(context,db).finishWorkout(input)
        assertEquals(newer,db.keyValueDocumentDao().getDocument("draft")!!.json)
    }
}
