package com.repforge.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import org.json.JSONArray
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeRestControlTest {
    private lateinit var context: Context
    private lateinit var controller: ServiceController<IntervalTimerService>
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private fun store() = PhoneWorkoutExecution.store(context)
    private fun frame() = store().snapshot()!!
    private fun timer() = IntervalTimerService.snapshot(context)
    private fun target() = timer().let { NativeTimerTarget(it.getString("workoutId"), it.getLong("generation"), it.getString("key")) }
    private fun restMs() = frame().getJSONObject("timeLogs162").let { logs -> logs.keys().asSequence().sumOf { logs.getJSONObject(it).getLong("restMs") } }
    private fun advance(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    private fun firstSet() = frame().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)

    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(IntervalTimerService.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        NativeWorkoutSession.installPrepared(context, JSONObject("""{
          "active":true,"timerOwner":"phone","workoutId":"rest-workout","id":"rest-workout","currentExercise":0,
          "automaticEnabled":true,"timerConfig":{"work":30,"rest":120,"rounds":1},"restSeconds":120,
          "exercises":[{"id":"press","repUnit":"ism.","loadType":"dumbbell","sets":[
          {"setId":"a","set":1,"done":false,"reps":"8","weight":10},
          {"setId":"b","set":2,"done":false,"reps":"8","weight":10},
          {"setId":"c","set":3,"done":false,"reps":"8","weight":10}]},
          {"id":"row","repUnit":"ism.","loadType":"dumbbell","sets":[{"setId":"row-a","set":1,"done":false,"reps":"8","weight":10}]}]}"""))
        controller = Robolectric.buildService(IntervalTimerService::class.java).create()
        controller.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM; putExtra("workoutId", "rest-workout"); putExtra("generation", frame().getLong("generation"))
        }, 0, 1)
    }

    @After fun close() {
        controller.destroy()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
    }

    private fun record() { NativeWorkoutEditing.completeStrengthSet(context, "press", "a") }

    @Test fun skipControlsTheRealClockAndOnlyAccountsActualRest() {
        record(); advance(30)
        NativeWorkoutEditing.controlRest(context, target())
        assertEquals("complete", timer().getString("phase"))
        assertEquals(30_000L, restMs())
        assertFalse(frame().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).getBoolean("done"))
        assertEquals("ready", PhoneWorkoutExecution.display(context)!!.getJSONObject("automation").getString("phase"))
    }

    @Test fun plusAndMinusChangeRealRemainingTimeAndDurablePlanWithoutInventedCredit() {
        record(); advance(30); val target = target()
        NativeWorkoutEditing.controlRest(context, target, 15)
        assertEquals(105_000L, timer().getLong("remainingMs")); assertEquals(30_000L, restMs())
        assertEquals(135, frame().getJSONObject("transition").getJSONObject("plan").getJSONArray("steps").getJSONObject(0).getInt("rest"))
        NativeWorkoutEditing.controlRest(context, target, -15)
        assertEquals(90_000L, timer().getLong("remainingMs")); assertEquals(30_000L, restMs())
        assertEquals(120, frame().getJSONObject("transition").getJSONObject("plan").getJSONArray("steps").getJSONObject(0).getInt("rest"))
        advance(5); assertEquals(85_000L, timer().getLong("remainingMs")); assertEquals(35_000L, restMs())
    }

    @Test fun subtractingBeyondRemainingRestSkipsWithoutCountingRemovedSeconds() {
        record(); advance(115)
        NativeWorkoutEditing.controlRest(context, target(), -15)
        assertEquals("complete", timer().getString("phase")); assertEquals(115_000L, restMs())
    }

    @Test fun oldKeyWorkoutAndGenerationCannotControlReplacementClock() {
        record(); val old = target()
        NativeWorkoutEditing.controlRest(context, old)
        NativeWorkoutEditing.completeStrengthSet(context, "press", "b")
        val current = target(); assertNotEquals(old.key, current.key)
        for (stale in listOf(old, current.copy(workoutId = "old"), current.copy(generation = current.generation + 1))) {
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, stale) }
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, stale, 15) }
        }
        assertEquals(current.key, timer().getString("key")); assertEquals(120_000L, timer().getLong("remainingMs"))
    }

    @Test fun explicitWorkoutPauseRejectsAdjustAndSkipUntilResume() {
        record(); advance(10); val target = target()
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", true).put("pauseRevision", 1))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, target) }
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, target, 15) }
        advance(20); assertEquals(110_000L, timer().getLong("remainingMs")); assertEquals(10_000L, restMs())
    }

    @Test fun exerciseSelectionPreservesRestAndCannotJumpBackWhenRestFinishes() {
        record(); advance(30); val key = target().key
        val row = NativeWorkoutTarget("rest-workout", frame().getLong("generation"), "row", "row-a")
        NativeWorkoutEditing.selectExercise(context, row)
        assertEquals(1, frame().getInt("currentExercise")); assertEquals(key, timer().getString("key"))
        assertEquals(90_000L, timer().getLong("remainingMs"))
        advance(90)
        assertEquals(1, PhoneWorkoutExecution.display(context)!!.getInt("currentExercise"))
        assertFalse(frame().getJSONArray("exercises").getJSONObject(1).getJSONArray("sets").getJSONObject(0).getBoolean("done"))
    }

    @Test fun changingExercisePausesLowerStopwatchWithoutCompletion() {
        val edit = ActiveWorkoutStore.current(context)!!
        edit.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp")
        NativeWorkoutEditing.commitEdit(context, edit)
        val first = NativeWorkoutTarget("rest-workout", frame().getLong("generation"), "press", "a")
        NativeWorkoutEditing.startStopwatch(context, first, "single")
        advance(5)
        NativeWorkoutEditing.selectExercise(context, NativeWorkoutTarget("rest-workout", first.generation, "row", "row-a"))
        assertFalse(firstSet().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        assertFalse(NativeSetStopwatch.running(frame().getJSONObject(NativeSetStopwatch.FIELD)))
        assertEquals(5000L, NativeSetStopwatch.elapsed(frame().getJSONObject(NativeSetStopwatch.FIELD)))
    }

    @Test fun adjustedClockProducesAValidPausedOwnershipCheckpoint() {
        record(); advance(30); NativeWorkoutEditing.controlRest(context, target(), 15)
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!.put("workoutPaused", true).put("pauseRevision", 1))
        val frozen = ActiveWorkoutStore.current(context)!!
        val status = PhoneWorkoutExecution.status(context)
        PhoneTimerCheckpoint193.verifyCanonical(frozen, status)
        val checkpoint = PhoneTimerCheckpoint193.create(frozen, status.getJSONObject("timer"))!!
        assertEquals(135, checkpoint.getJSONObject("timer").getJSONArray("steps").getJSONObject(0).getInt("rest"))
        assertEquals(30_000L, checkpoint.getJSONObject("timer").getJSONObject("ledger").getLong("restMs"))
        assertEquals(105_000L, checkpoint.getJSONObject("timer").getLong("remainingMs"))
    }

    @Test fun failedDurationCommitRestoresThePreviousClockAndCanRetry() {
        record(); advance(30); val target = target()
        store().writableDatabase.execSQL("CREATE TRIGGER rest_disk_full BEFORE INSERT ON execution BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        assertThrows(Exception::class.java) { NativeWorkoutEditing.controlRest(context, target, 15) }
        store().writableDatabase.execSQL("DROP TRIGGER rest_disk_full")
        assertEquals(90_000L, timer().getLong("remainingMs"))
        assertEquals(120, frame().getJSONObject("transition").getJSONObject("plan").getJSONArray("steps").getJSONObject(0).getInt("rest"))
        NativeWorkoutEditing.controlRest(context, target, 15)
        assertEquals(105_000L, timer().getLong("remainingMs")); assertEquals(30_000L, restMs())
    }

    @Test fun watchMirrorAndFrozenDraftCannotControlPhoneRest() {
        record(); val target = target(); val current = ActiveWorkoutStore.current(context)!!
        ActiveWorkoutStore.saveOwned(context, JSONObject(current.toString()).put("timerOwner", "watch"))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, target) }
        ActiveWorkoutStore.saveOwned(context, JSONObject(current.toString()).put("finalizationPending", true))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.controlRest(context, target, 15) }
        assertEquals(120_000L, timer().getLong("remainingMs"))
    }
}
