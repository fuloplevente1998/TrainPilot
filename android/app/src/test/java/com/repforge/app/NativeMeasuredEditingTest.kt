package com.repforge.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.time.Duration
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeMeasuredEditingTest {
    private lateinit var context: Context
    private lateinit var controller: ServiceController<IntervalTimerService>
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private fun store() = PhoneWorkoutExecution.store(context)
    private fun frame() = store().snapshot()!!
    private fun target(ex: Int = 0, set: Int = 0): NativeWorkoutTarget {
        val f = frame(); val e = f.getJSONArray("exercises").getJSONObject(ex)
        return NativeWorkoutTarget(f.getString("workoutId"), f.getLong("generation"), e.getString("id"), e.getJSONArray("sets").getJSONObject(set).getString("setId"))
    }
    private fun set(f: JSONObject = frame(), ex: Int = 0, index: Int = 0) = f.getJSONArray("exercises").getJSONObject(ex).getJSONArray("sets").getJSONObject(index)
    private fun edit(ex: Int = 0, change: (JSONObject) -> Unit) {
        val next = NativeWorkoutEditing.project(context); change(set(next, ex)); NativeWorkoutEditing.commitEdit(context, next)
    }
    private fun advance(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    private fun timer() = IntervalTimerService.snapshot(context)
    private fun timerTarget() = timer().let { NativeTimerTarget(it.getString("workoutId"), it.getLong("generation"), it.getString("key")) }
    private fun totals() = NativeMeasuredTime.summary(frame())
    private fun command() = frame().let { f -> JSONObject().put("nativeExecutionProtocol", 2).put("action", "completeMeasuredSet")
        .put("commandId", UUID.randomUUID().toString()).put("workoutId", f.getString("workoutId")).put("generation", f.getLong("generation"))
        .put("ownerEpoch193", f.optLong("ownerEpoch193")).put("executionRevision", f.getLong("executionRevision"))
        .put("exerciseId", "plank").put("setId", "a").put("measuredValues", PhoneWorkoutExecutionStore.recordedBase(set(f)).toString()) }

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(IntervalTimerService.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        NativeWorkoutSession.installPrepared(context, JSONObject("""{
          "active":true,"timerOwner":"phone","workoutId":"measured","id":"measured","started":"2026-10-23T12:00:00Z",
          "currentExercise":0,"automaticEnabled":true,"timerConfig":{"work":2,"rest":3,"rounds":2,"sound":false,"vibration":false},"restSeconds":0,
          "exercises":[{"id":"plank","repUnit":"mp","measurementType":"time","loadType":"bodyweight","sets":[
          {"setId":"a","set":1,"done":false,"reps":"","weight":0},{"setId":"b","set":2,"done":false,"reps":"","weight":0}]},
          {"id":"side-plank","repUnit":"mp / oldal","measurementType":"time","loadType":"bodyweight","sets":[{"setId":"side","set":1,"done":false,"reps":"","weight":0}]},
          {"id":"run","repUnit":"km","measurementType":"distance","loadType":"bodyweight","sets":[{"setId":"run-a","set":1,"done":false,"reps":"","weight":0}]}]}"""))
        controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        controller.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM; putExtra("workoutId", "measured"); putExtra("generation", frame().getLong("generation"))
        }, 0, 1)
    }

    @After fun close() {
        controller.destroy(); (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
    }

    @Test fun manualTimeOkCreatesNativeUndoWithoutManufacturingObservedTime() {
        edit { it.put("reps", "35") }
        val saved = NativeWorkoutEditing.completeMeasuredSet(context, target())
        assertTrue(set().getBoolean("done")); assertEquals("35", set().getString("reps")); assertEquals(0L, totals().workMs)
        assertEquals(0, store().pendingEvents("measured", target().generation, 0).length())
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set().getBoolean("done")); assertEquals("35", set().getString("reps"))
    }

    @Test fun bilateralManualOkRequiresBothSidesAndKeepsThemAfterUndo() {
        edit(1) { it.put("leftSeconds", 15) }
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target(1)) }
        edit(1) { it.put("rightSeconds", 20) }
        val saved = NativeWorkoutEditing.completeMeasuredSet(context, target(1))
        assertTrue(set(ex = 1).getBoolean("done")); assertEquals("15", set(ex = 1).getString("reps"))
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set(ex = 1).getBoolean("done")); assertEquals(15, set(ex = 1).getInt("leftSeconds")); assertEquals(20, set(ex = 1).getInt("rightSeconds"))
    }

    @Test fun manualDistanceNeedsTimeAndDistanceAndItsUndoKeepsTheEnteredValues() {
        edit(2) { it.put("reps", "60") }
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target(2)) }
        edit(2) { it.put("distanceMeters", 125.5).put("distanceSource", "manual") }
        val saved = NativeWorkoutEditing.completeMeasuredSet(context, target(2))
        assertTrue(set(ex = 2).getBoolean("done")); assertEquals(0L, totals().workMs)
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set(ex = 2).getBoolean("done")); assertEquals(125.5, set(ex = 2).getDouble("distanceMeters"), 0.0)
    }

    @Test fun invalidManualDurationsCannotProduceARecording() {
        for (value in listOf("", "0", "-1", "1.5", "NaN", "86401", "99999999999999999999999")) {
            edit { it.put("reps", value) }
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target()) }
            assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        }
    }

    @Test fun manualRecordStorageFailureRemainsUnfinishedAndRetryRecordsOnce() {
        edit { it.put("reps", "12") }
        store().writableDatabase.execSQL("CREATE TRIGGER manual_disk_full BEFORE INSERT ON recordings BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        assertThrows(Exception::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target()) }
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        store().writableDatabase.execSQL("DROP TRIGGER manual_disk_full")
        NativeWorkoutEditing.completeMeasuredSet(context, target())
        store().readableDatabase.rawQuery("SELECT COUNT(*) FROM recordings", null).use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
    }

    @Test fun manualCommandReplayIsIdempotentButChangingItsValuesIsACollision() {
        edit { it.put("reps", "12") }; val command = command()
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command).getString("status"))
        val revision = frame().getLong("executionRevision")
        assertEquals("duplicate", PhoneWorkoutExecution.execute(context, command).getString("status"))
        assertEquals(revision, frame().getLong("executionRevision"))
        assertEquals("rejected", PhoneWorkoutExecution.execute(context, JSONObject(command.toString()).put("measuredValues", "changed")).getString("status"))
    }

    @Test fun staleTargetsPausedWorkoutAndLegacyFramesCannotManuallyComplete() {
        edit { it.put("reps", "12") }; val target = target()
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target.copy(generation = target.generation + 1)) }
        assertThrows(IllegalArgumentException::class.java) { PhoneWorkoutRecording.record(JSONObject(frame().toString()).put("nativeEditorProjection", 0), command()) }
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", true).put("pauseRevision", 1))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target) }
        assertFalse(set().getBoolean("done"))
    }

    @Test fun runningStopwatchBlocksManualOkAndStaleMeasurementValuesAreRejected() {
        edit { it.put("reps", "12") }; val command = command()
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.completeMeasuredSet(context, target()) }
        NativeWorkoutEditing.resetStopwatch(context, target())
        edit { it.put("reps", "13") }
        assertEquals("rejected", PhoneWorkoutExecution.execute(context, command).getString("status")); assertFalse(set().getBoolean("done"))
    }

    @Test fun stopwatchResumeDoesNotDoubleCountEarlierWorkAndMeasuresOnlyObservedPause() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single"); advance(5)
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", true).put("pauseRevision", 1))
        advance(20)
        assertEquals(5000L, totals().workMs); assertEquals(20000L, totals().pausedMs)
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", false).put("pauseRevision", 2))
        NativeWorkoutEditing.startStopwatch(context, target(), "single"); advance(4)
        val saved = NativeWorkoutEditing.stopStopwatch(context, target())
        assertEquals("9", set().getString("reps")); assertEquals(9000L, totals().workMs); assertEquals(20000L, totals().pausedMs)
        advance(10); assertEquals(20000L, totals().pausedMs)
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertEquals(9000L, totals().workMs); assertEquals(20000L, totals().pausedMs)
    }

    @Test fun resetRetainsObservedWorkButDoesNotCompleteOrManufactureAnUndo() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single"); advance(3)
        NativeWorkoutEditing.resetStopwatch(context, target()); advance(20)
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        assertEquals(3000L, totals().workMs); assertEquals(0L, totals().pausedMs)
    }

    @Test fun anotherProcessCannotCreditTheUnobservedTimeAfterItsLastCheckpoint() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single"); advance(5)
        val recovered = frame(); val watch = JSONObject(recovered.toString()).put("timerOwner", "watch")
        recovered.getJSONObject(NativeSetStopwatch.FIELD).put("process", "older-process")
        advance(20)
        assertEquals(5000L, NativeMeasuredTime.summary(recovered).workMs)
        assertEquals(5000L, NativeMeasuredTime.summary(watch).workMs)
    }

    @Test fun intervalCompletionRecordsWorkOnlyAndItsUndoKeepsActualWorkAndRest() {
        NativeWorkoutEditing.startIntervals(context, target()); advance(10)
        assertTrue(set().getBoolean("done")); assertEquals("4", set().getString("reps"))
        assertFalse(set(index = 1).getBoolean("done")); assertEquals(4000L, totals().workMs); assertEquals(6000L, totals().restMs)
        advance(7)
        assertTrue(set(index = 1).getBoolean("done")); assertEquals("4", set(index = 1).getString("reps"))
        assertEquals(8000L, totals().workMs); assertEquals(9000L, totals().restMs)
        val saved = NativeWorkoutEditing.project(context)
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set(index = 1).getBoolean("done")); assertEquals(8000L, totals().workMs); assertEquals(9000L, totals().restMs)
    }

    @Test fun explicitIntervalStopPreservesPartialTimeWithoutCompletingAndRetiresLateTicks() {
        NativeWorkoutEditing.startIntervals(context, target()); advance(1)
        val old = timer(); val key = timerTarget()
        NativeWorkoutEditing.stopIntervals(context, key)
        assertFalse(set().getBoolean("done")); assertEquals("1", set().getString("reps")); assertEquals(1000L, totals().workMs)
        assertFalse(store().checkpointTimer(old, android.os.SystemClock.elapsedRealtime()))
        NativeWorkoutEditing.completeMeasuredSet(context, target())
        assertTrue(set().getBoolean("done")); assertEquals(1000L, totals().workMs)
    }

    @Test fun activeIntervalProtectsItsMeasuredRowsButFutureConfigurationDoesNotResetItsPlan() {
        NativeWorkoutEditing.startIntervals(context, target()); advance(1)
        val before = timer(); val old = NativeWorkoutEditing.project(context)
        set(old).put("reps", "99")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.commitEdit(context, old) }
        NativeWorkoutEditing.configureIntervals(context, target(), JSONObject().put("work", 4).put("rest", 8).put("rounds", 3))
        assertEquals(before.getString("key"), timer().getString("key")); assertEquals(before.getLong("remainingMs"), timer().getLong("remainingMs"))
        assertEquals(2, timer().getJSONArray("steps").getJSONObject(0).getInt("work")); assertEquals(4, frame().getJSONObject("timerConfig").getInt("work"))
    }

    @Test fun invalidConfigurationOrOldTargetCannotChangeTheRunningWorkout() {
        val previous = frame().getJSONObject("timerConfig").toString()
        for (config in listOf(JSONObject().put("work", 0).put("rest", 3).put("rounds", 2), JSONObject().put("work", 2).put("rest", -1).put("rounds", 2),
            JSONObject().put("work", 2).put("rest", 3).put("rounds", 101))) {
            assertThrows(Exception::class.java) { NativeWorkoutEditing.configureIntervals(context, target(), config) }
            assertEquals(previous, frame().getJSONObject("timerConfig").toString())
        }
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.configureIntervals(context, target().copy(workoutId = "old"), JSONObject().put("work", 2).put("rest", 3).put("rounds", 2)) }
    }

    @Test fun bilateralAndDistanceExercisesKeepTheirOwnControlsInsteadOfAnAutomaticInterval() {
        for (index in listOf(1, 2)) {
            NativeWorkoutEditing.selectExercise(context, target(index))
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.startIntervals(context, target(index)) }
            assertFalse(set(ex = index).getBoolean("done"))
        }
    }

    @Test fun stoppedOldIntervalCannotStopOrReconcileAReplacement() {
        NativeWorkoutEditing.startIntervals(context, target()); advance(1); val old = timerTarget()
        NativeWorkoutEditing.stopIntervals(context, old)
        NativeWorkoutEditing.startIntervals(context, target()); val current = timerTarget()
        assertNotEquals(old.key, current.key)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.stopIntervals(context, old) }
        assertEquals(current.key, timer().getString("key")); assertTrue(timer().getBoolean("running"))
    }

    @Test fun liveSummaryDoesNotWriteOrMutateItsDurableInputAndNextCheckpointDoesNotDoubleCredit() {
        NativeWorkoutEditing.startStopwatch(context, target(), "single")
        org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        val snapshot = frame(); val before = snapshot.toString()
        assertEquals(5000L, NativeMeasuredTime.summary(snapshot).workMs)
        assertEquals(before, snapshot.toString()); assertEquals(before, frame().toString())
        store().checkpointStopwatch("measured", target().generation, android.os.SystemClock.elapsedRealtime())
        assertEquals(5000L, totals().workMs)
    }

    @Test fun earlierWatchTimeRemainsSeparateFromThePhoneStopwatch() {
        val copy = frame()
        val log = JSONObject().put("schema", 1).put("key", "watch-earlier").put("owner", "watch")
            .put("positionMs", 5000L).put("workMs", 3000L).put("restMs", 2000L).put("pausedMs", 1000L)
        copy.put("automaticTime162", JSONObject().put("schema", 1).put("logs", JSONObject().put("watch-earlier", log)))
        NativeWorkoutEditing.commitEdit(context, copy)
        NativeWorkoutEditing.startStopwatch(context, target(), "single"); advance(2)
        assertEquals(5000L, totals().workMs); assertEquals(2000L, totals().restMs); assertEquals(1000L, totals().pausedMs)
    }
    private fun configureSingleSetInterval() {
        NativeWorkoutEditing.configureIntervals(context, target(), JSONObject().put("work", 2).put("rest", 3)
            .put("rounds", 2).put("autoAdvance", false).put("sound", false).put("vibration", false))
    }
    @Test fun nonAdvancingIntervalCreditsOnlyCurrentSetAndRequiresExplicitConfirmation() {
        configureSingleSetInterval()
        NativeWorkoutEditing.startIntervals(context, target())
        assertFalse(timer().getBoolean("autoAdvance"))
        assertEquals(1, timer().getJSONArray("steps").length())
        advance(7)
        assertEquals("complete", timer().getString("phase"))
        assertEquals("4", set().getString("reps")); assertFalse(set().getBoolean("done"))
        assertFalse(set(index = 1).getBoolean("done")); assertEquals("", set(index = 1).getString("reps"))
        assertEquals(0, frame().getInt("currentExercise"))
        assertEquals(4000L, totals().workMs); assertEquals(3000L, totals().restMs)
        assertFalse(frame().has("lastRecord188"))
        val saved = NativeWorkoutEditing.completeMeasuredSet(context, target())
        assertTrue(set().getBoolean("done"))
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set().getBoolean("done")); assertEquals("4", set().getString("reps"))
        assertEquals(4000L, totals().workMs)
    }
    @Test fun nonAdvancingExplicitStopThenRestartAddsOnlyNewObservedWork() {
        configureSingleSetInterval()
        NativeWorkoutEditing.startIntervals(context, target()); advance(1)
        NativeWorkoutEditing.stopIntervals(context, timerTarget())
        assertEquals("1", set().getString("reps")); assertFalse(set().getBoolean("done"))
        NativeWorkoutEditing.startIntervals(context, target()); advance(7)
        assertEquals("5", set().getString("reps")); assertFalse(set().getBoolean("done"))
        assertFalse(set(index = 1).getBoolean("done"))
        assertEquals(5000L, totals().workMs); assertEquals(3000L, totals().restMs)
    }
    @Test fun changingNextIntervalModeDoesNotChangeAnAlreadyInstalledAutomaticPlan() {
        NativeWorkoutEditing.startIntervals(context, target()); advance(1)
        val before = timer()
        configureSingleSetInterval()
        assertFalse(frame().getJSONObject("timerConfig").getBoolean("autoAdvance"))
        assertTrue(timer().getBoolean("autoAdvance"))
        assertEquals(before.getString("key"), timer().getString("key"))
        advance(9)
        assertTrue(set().getBoolean("done")); assertFalse(set(index = 1).getBoolean("done"))
    }

    @Test fun workoutIntervalPresetAppliesToAllQueuedTimedExercisesDespiteTheirManualRestDefaults() {
        val source = frame()
        val first = source.getJSONArray("exercises").getJSONObject(0).put("restSeconds162", 99)
        val next = JSONObject(first.toString()).put("id", "another-plank").put("restSeconds162", 88)
            .put("sets", org.json.JSONArray().put(JSONObject().put("setId", "another-set").put("done", false).put("reps", "")))
        source.put("exercises", org.json.JSONArray().put(first).put(next))
        val plan = PhoneWorkoutPlans.startTimed(source).getJSONObject("plan")
        val rows = plan.getJSONArray("steps")
        assertEquals(3, rows.length())
        for (index in 0 until rows.length()) assertEquals(3, rows.getJSONObject(index).getInt("rest"))
        assertEquals("another-plank", rows.getJSONObject(2).getString("exerciseId"))
    }

}
