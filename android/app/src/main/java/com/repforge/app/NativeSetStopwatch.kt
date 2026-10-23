package com.repforge.app

import android.os.SystemClock
import com.repforge.shared.WorkoutStatsCalculator
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

/** One lower set stopwatch, persisted inside the canonical native execution frame. */
internal object NativeSetStopwatch {
    const val FIELD = "setStopwatch"
    private val process = UUID.randomUUID().toString()

    fun timed(exercise: JSONObject): Boolean = exercise.optString("measurementType") != "distance" &&
        WorkoutStatsCalculator.isTimedUnit(exercise.optString("repUnit"), exercise.optString("id"))

    fun bilateral(exercise: JSONObject) = WorkoutStatsCalculator.isBilateralUnit(exercise.optString("repUnit"), exercise.optString("id"))

    fun elapsed(clock: JSONObject, now: Long = SystemClock.elapsedRealtime()): Long =
        (clock.optLong("elapsedMs").coerceIn(0, IntervalTimeline.MAX_MS) +
            if (running(clock)) (now - clock.optLong("began")).coerceIn(0, IntervalTimeline.MAX_MS) else 0L).coerceAtMost(IntervalTimeline.MAX_MS)

    fun running(clock: JSONObject) = clock.optBoolean("running") && clock.optString("process") == process

    fun pausedElapsed(clock: JSONObject, now: Long = SystemClock.elapsedRealtime()): Long =
        if (clock.optString("process") == process && !clock.optBoolean("running") && clock.has("pauseBegan"))
            (now - clock.getLong("pauseBegan")).coerceIn(0L, IntervalTimeline.MAX_MS) else 0L

    fun sealPause(frame: JSONObject, now: Long = SystemClock.elapsedRealtime()) {
        NativeMeasuredTime.stopwatch(frame, now)
        frame.optJSONObject(FIELD)?.remove("pauseBegan")
    }

    fun pause(frame: JSONObject, now: Long = SystemClock.elapsedRealtime(), trackPause: Boolean = true) {
        val clock = frame.optJSONObject(FIELD) ?: return
        if (running(clock) && trackPause) clock.put("pauseBegan", now)
        if (!trackPause) clock.remove("pauseBegan")
        clock.put("elapsedMs", elapsed(clock, now)).put("began", now).put("running", false)
        NativeMeasuredTime.stopwatch(frame, now)
    }

    fun checkpoint(frame: JSONObject, now: Long) {
        val clock = frame.optJSONObject(FIELD) ?: return
        val live = running(clock)
        clock.put("elapsedMs", elapsed(clock, now)).put("began", now).put("running", live)
        NativeMeasuredTime.stopwatch(frame, now)
    }

    fun start(frame: JSONObject, target: NativeWorkoutTarget, side: String, now: Long = SystemClock.elapsedRealtime()) {
        val resolved = target.resolve(frame)
        check(!frame.optBoolean("workoutPaused") && !resolved.set.optBoolean("done") && timed(resolved.exercise)) {
            "Ehhez a sorozathoz nem indítható stopper."
        }
        require(side in if (bilateral(resolved.exercise)) setOf("left", "right") else setOf("single"))
        val previous = frame.optJSONObject(FIELD)
        check(previous == null || !running(previous)) { "Már fut egy sorozatstopper."
        }
        val same = previous != null && previous.optString("setId") == target.setId && previous.optString("side") == side
        NativeMeasuredTime.stopwatch(frame, now)
        val baseline = if (same) elapsed(previous!!, now) else 0L
        frame.put(FIELD, JSONObject().put("id", UUID.randomUUID().toString()).put("exerciseId", target.exerciseId)
            .put("setId", target.setId).put("side", side).put("elapsedMs", baseline).put("creditBaseMs", baseline)
            .put("process", process).put("began", now).put("running", true))
    }

    /** An implicit pause/reset never completes a set. Only explicit positive stop enters this function. */
    fun stopped(frame: JSONObject, command: JSONObject, now: Long = SystemClock.elapsedRealtime()): JSONObject {
        val next = JSONObject(frame.toString())
        val target = NativeWorkoutTarget(next.getString("workoutId"), next.getLong("generation"), command.getString("exerciseId"), command.getString("setId"))
        val resolved = target.resolve(next)
        val clock = next.getJSONObject(FIELD)
        check(clock.getString("id") == command.getString("clockId") && clock.getString("setId") == target.setId &&
            clock.getString("exerciseId") == target.exerciseId && !resolved.set.optBoolean("done") && timed(resolved.exercise)) { "A stopper célpontja megváltozott." }
        // A process restart recovers the last durable checkpoint in pause. It cannot silently complete.
        check(running(clock)) { "A stopper megszakadt. Folytasd vagy nullázd a mérést." }
        val seconds = elapsed(clock, now) / 1000L
        pause(next, now, trackPause = false)
        if (seconds <= 0L) return next
        if (bilateral(resolved.exercise)) {
            val field = when (clock.getString("side")) { "left" -> "leftSeconds"; "right" -> "rightSeconds"; else -> error("Érvénytelen oldal.") }
            resolved.set.put(field, seconds)
            val left = resolved.set.optLong("leftSeconds")
            val right = resolved.set.optLong("rightSeconds")
            if (left > 0 && right > 0) resolved.set.put("reps", min(left, right).toString()).put("done", true)
        } else {
            check(clock.getString("side") == "single")
            resolved.set.put("reps", seconds.toString()).put("done", true)
        }
        return next
    }
}
