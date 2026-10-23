package com.repforge.app

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeSetStopwatchTest {
    private lateinit var context: Context
    private var controller: ServiceController<IntervalTimerService>? = null
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private fun store() = PhoneWorkoutExecution.store(context)
    private fun target() = NativeWorkoutTarget("timed", store().snapshot()!!.getLong("generation"), "plank", "first")
    private fun frame() = store().snapshot()!!
    private fun set(frame: JSONObject = frame()) = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun clock() = frame().getJSONObject(NativeSetStopwatch.FIELD)
    private fun advance(seconds: Long) = ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds))

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        NativeWorkoutSession.installPrepared(context, JSONObject("""{
            "active":true,"timerOwner":"phone","workoutId":"timed","id":"timed","started":"2026-10-23T10:00:00Z",
            "currentExercise":0,"automaticEnabled":true,"timerConfig":{"work":30,"rest":90,"rounds":1},"restSeconds":90,
            "exercises":[{"id":"plank","loadType":"bodyweight","repUnit":"mp","measurementType":"time","sets":[
            {"setId":"first","set":1,"done":false,"reps":"30–60","weight":0},
            {"setId":"second","set":2,"done":false,"reps":"30–60","weight":0}]}]}"""))
        val frame = frame()
        controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        controller!!.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", "timed"); putExtra("generation", frame.getLong("generation"))
        }, 0, 1)
    }

    @After fun close() {
        controller?.destroy()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
    }

    @Test fun explicitPositiveStopRecordsActualTimeWithoutOkAndCreatesNativeUndo() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(8)
        val saved = NativeWorkoutEditing.stopStopwatch(context, target())
        assertTrue(set(saved).getBoolean("done"))
        assertEquals("8", set(saved).getString("reps"))
        assertFalse(NativeSetStopwatch.running(clock()))
        assertTrue(frame().getJSONObject("transition").getJSONObject("plan").getBoolean("restOnly"))
        assertFalse(frame().getJSONObject("transition").getJSONObject("plan").getBoolean("autoAdvance"))
        assertEquals(0, store().pendingEvents("timed", saved.getLong("generation"), 0).length())
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set().getBoolean("done"))
        assertEquals("30–60", set().getString("reps"))
        assertFalse(NativeSetStopwatch.running(clock()))
    }

    @Test fun zeroStopAndResetNeverCompleteOrCreateARecording() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        NativeWorkoutEditing.stopStopwatch(context, target())
        assertFalse(set().getBoolean("done"))
        assertFalse(frame().has("lastRecord188"))
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(5)
        NativeWorkoutEditing.resetStopwatch(context, target())
        assertFalse(set().getBoolean("done"))
        assertEquals("30–60", set().getString("reps"))
        assertFalse(frame().has(NativeSetStopwatch.FIELD))
        assertFalse(frame().has("lastRecord188"))
    }

    @Test fun explicitWorkoutPausePreservesElapsedWithoutCompletionOrPauseTimeCredit() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(4)
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", true).put("pauseRevision", 1))
        assertFalse(set().getBoolean("done"))
        assertFalse(NativeSetStopwatch.running(clock()))
        advance(100)
        assertEquals(4000L, NativeSetStopwatch.elapsed(clock()))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.stopStopwatch(context, target()) }
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", false).put("pauseRevision", 2))
        // Resuming the workout does not silently resume or finish the set stopwatch.
        assertFalse(NativeSetStopwatch.running(clock()))
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(3)
        NativeWorkoutEditing.stopStopwatch(context, target())
        assertEquals("7", set().getString("reps"))
    }

    @Test fun bilateralFirstSideRemainsIncompleteAndSecondPositiveSideCompletes() {
        val edit = ActiveWorkoutStore.current(context)!!
        edit.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp / oldal")
        NativeWorkoutEditing.commitEdit(context, edit)
        NativeWorkoutEditing.startStopwatch(context, target(), "left")
        advance(12)
        NativeWorkoutEditing.stopStopwatch(context, target())
        assertFalse(set().getBoolean("done"))
        assertEquals(12, set().getInt("leftSeconds"))
        assertFalse(frame().has("lastRecord188"))
        NativeWorkoutEditing.startStopwatch(context, target(), "right")
        advance(9)
        val saved = NativeWorkoutEditing.stopStopwatch(context, target())
        assertTrue(set().getBoolean("done"))
        assertEquals("9", set().getString("reps"))
        assertEquals(12, set().getInt("leftSeconds"))
        assertEquals(9, set().getInt("rightSeconds"))
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set().getBoolean("done"))
        assertEquals(12, set().getInt("leftSeconds"))
        assertEquals(0, set().optInt("rightSeconds"))
    }

    @Test fun checkpointAndBackgroundDoNotCompleteAndOrdinaryEditCannotReplaceClock() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        val staleEdit = ActiveWorkoutStore.current(context)!!
        advance(5)
        store().checkpointStopwatch("timed", target().generation, SystemClock.elapsedRealtime())
        staleEdit.getJSONObject(NativeSetStopwatch.FIELD).put("running", false).put("elapsedMs", 0)
        NativeWorkoutEditing.commitEdit(context, staleEdit)
        assertTrue(NativeSetStopwatch.running(clock()))
        assertEquals(5000L, NativeSetStopwatch.elapsed(clock()))
        assertFalse(set().getBoolean("done"))
        assertFalse(frame().has("lastRecord188"))
    }

    @Test fun controllerDestructionPausesDurablyWithoutInventingCompletion() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(6)
        controller!!.destroy(); controller = null
        advance(20)
        assertFalse(NativeSetStopwatch.running(clock()))
        assertEquals(6000L, NativeSetStopwatch.elapsed(clock()))
        assertFalse(set().getBoolean("done"))
        assertFalse(frame().has("lastRecord188"))
    }

    @Test fun stoppedClockAfterProcessRestartCanOnlyRecoverCheckpointInPause() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(5)
        store().checkpointStopwatch("timed", target().generation, SystemClock.elapsedRealtime())
        val oldProcessClock = JSONObject(clock().toString()).put("process", "retired-process")
        advance(90)
        assertFalse(NativeSetStopwatch.running(oldProcessClock))
        assertEquals(5000L, NativeSetStopwatch.elapsed(oldProcessClock))
        assertFalse(set().getBoolean("done"))
    }

    @Test fun secondTargetCannotStealRunningStopwatchAndStaleControllerCannotPauseIt() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        val other = NativeWorkoutTarget("timed", target().generation, "plank", "second")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.startStopwatch(context, other, "single") }
        store().checkpointStopwatch("older-workout", target().generation, SystemClock.elapsedRealtime(), pause = true)
        store().checkpointStopwatch("timed", target().generation + 1, SystemClock.elapsedRealtime(), pause = true)
        assertTrue(NativeSetStopwatch.running(clock()))
    }

    @Test fun originalCompleteSetStillRejectsTimedMeasurement() {
        val frame = frame()
        val command = JSONObject().put("commandId", "legacy").put("action", "completeSet").put("workoutId", "timed")
            .put("generation", frame.getLong("generation")).put("executionRevision", frame.getLong("executionRevision"))
            .put("exerciseId", "plank").put("setId", "first").put("reps", "8")
        assertEquals("unsupportedMeasurement", store().record(command).getString("reason"))
        assertFalse(set().getBoolean("done"))
    }

    @Test fun recordingStorageFailureKeepsUnfinishedTargetAndClockAvailableForRetry() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(5)
        store().writableDatabase.execSQL("CREATE TRIGGER fail_timed_record BEFORE INSERT ON recordings BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        assertThrows(Exception::class.java) { NativeWorkoutEditing.stopStopwatch(context, target()) }
        assertFalse(set().getBoolean("done"))
        assertTrue(NativeSetStopwatch.running(clock()))
        assertFalse(frame().has("lastRecord188"))
        assertFalse(set(ActiveWorkoutStore.current(context)!!).getBoolean("done"))
        store().writableDatabase.execSQL("DROP TRIGGER fail_timed_record")
        NativeWorkoutEditing.stopStopwatch(context, target())
        assertTrue(set().getBoolean("done"))
        assertEquals("5", set().getString("reps"))
    }

    @Test fun nativeMeasuredCommandReplayRetainsOriginalElapsedAndOneReceipt() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        advance(6)
        val command = JSONObject().put("nativeExecutionProtocol", 2).put("action", "completeTimedSet").put("commandId", "same-stop")
            .put("workoutId", "timed").put("generation", target().generation).put("ownerEpoch193", frame().optLong("ownerEpoch193"))
            .put("executionRevision", frame().getLong("executionRevision")).put("exerciseId", "plank").put("setId", "first")
            .put("clockId", clock().getString("id"))
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command).getString("status"))
        advance(100)
        assertEquals("duplicate", PhoneWorkoutExecution.execute(context, command).getString("status"))
        assertEquals("6", set().getString("reps"))
        store().readableDatabase.rawQuery("SELECT COUNT(*) FROM recordings WHERE command_id='same-stop'", null).use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
        assertEquals(0, store().pendingEvents("timed", target().generation, 0).length())
    }
}
