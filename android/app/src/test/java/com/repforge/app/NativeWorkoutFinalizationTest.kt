package com.repforge.app

import android.content.Context
import androidx.room.Room
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutFinalizationTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
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
        val input = prepare()
        record()
        schedule()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER calendar_full BEFORE INSERT ON calendar_entries
            WHEN NEW.status = 'completed' BEGIN SELECT RAISE(ABORT, 'injected disk failure'); END""")
        var failed = false
        try { TrainPilotWorkoutFinalization(context, db).finishWorkout(input) } catch (_: Exception) { failed = true }
        assertTrue(failed)
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
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
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
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
        assertEquals(current.toString(), ActiveWorkoutStore.current(context).toString())
    }
}
