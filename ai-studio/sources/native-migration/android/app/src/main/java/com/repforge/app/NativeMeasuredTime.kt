package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject

/** Observed running time only. Manual reps/seconds never manufacture clock credit. */
internal object NativeMeasuredTime {
    data class Totals(val workMs: Long, val restMs: Long, val pausedMs: Long)

    fun summary(frame: JSONObject, timer: JSONObject? = null, now: Long = android.os.SystemClock.elapsedRealtime()): Totals {
        val copy = JSONObject(frame.toString())
        stopwatch(copy, now)
        NativeWorkoutEventMerge.mergeTime(copy, copy.optJSONObject("timeLogs162") ?: JSONObject())
        if (copy.optBoolean("active") && copy.optString("timerOwner") == "phone" && timer != null && timer.optString("workoutId") == copy.optString("workoutId") &&
            timer.optLong("generation") == copy.optLong("generation") && timer.optJSONObject("ledger") != null) {
            val retired = copy.optJSONArray("retiredTimers188")
            if (retired == null || (0 until retired.length()).none { retired.optString(it) == timer.optString("key") }) {
                NativeWorkoutEventMerge.mergeTime(copy, JSONObject().put(timer.getString("key"),
                    PhoneOwnershipResume193.localLog(timer, timer.getJSONObject("ledger"))))
            }
        }
        val total = copy.getJSONObject("automaticTime162")
        return Totals(total.optLong("workMs"), total.optLong("restMs"), total.optLong("pausedMs"))
    }

    fun stopwatch(frame: JSONObject, now: Long) {
        if (!frame.optBoolean("active") || frame.optString("timerOwner") != "phone") return
        val clock = frame.optJSONObject(NativeSetStopwatch.FIELD) ?: return
        val work = (NativeSetStopwatch.elapsed(clock, now) - clock.optLong("creditBaseMs")).coerceAtLeast(0L)
        add(frame, "set|${frame.getLong("generation")}|${clock.getString("id")}", clock, work, NativeSetStopwatch.pausedElapsed(clock, now))
    }

    fun gps(frame: JSONObject, capture: JSONObject) {
        val binding = frame.getJSONObject(NativeGpsMeasurement.FIELD)
        add(frame, "gps|${frame.getLong("generation")}|${binding.getString("id")}", binding, capture.getLong("elapsedSeconds") * 1000L)
    }

    private fun add(frame: JSONObject, key: String, target: JSONObject, work: Long, paused: Long = 0L) {
        if (work <= 0L && paused <= 0L) return
        require(work <= IntervalTimeline.MAX_MS && paused <= IntervalTimeline.MAX_MS)
        val logs = frame.optJSONObject("timeLogs162") ?: JSONObject()
        val previous = logs.optJSONObject(key)
        if (previous != null && previous.getLong("workMs") >= work && previous.getLong("pausedMs") >= paused) return
        val row = JSONObject().put("exerciseId", target.getString("exerciseId")).put("setId", target.getString("setId"))
            .put("workMs", work).put("restMs", 0L).put("pausedMs", paused)
        logs.put(key, JSONObject().put("schema", 1).put("owner", "phone").put("key", key).put("positionMs", work)
            .put("workMs", work).put("restMs", 0L).put("pausedMs", paused).put("steps", JSONArray().put(row)))
        frame.put("timeLogs162", logs)
    }
}
