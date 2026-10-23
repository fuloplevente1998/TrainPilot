package com.repforge.app

import org.json.JSONObject
import kotlin.math.min

/** Explicit manual OK uses the same transaction/receipt/undo as measured STOP. */
internal object NativeMeasuredSet {
    fun supported(exercise: JSONObject) = NativeSetStopwatch.timed(exercise) || NativeGpsMeasurement.distance(exercise)

    fun record(frame: JSONObject, command: JSONObject): JSONObject {
        val next = JSONObject(frame.toString())
        val target = NativeWorkoutTarget(next.getString("workoutId"), next.getLong("generation"),
            command.getString("exerciseId"), command.getString("setId")).resolve(next)
        check(supported(target.exercise) && !target.set.getBoolean("done")) { "unsupportedMeasurement" }
        check(command.getString("measuredValues") == PhoneWorkoutExecutionStore.recordedBase(target.set).toString()) { "measurementChanged" }
        val seconds = if (NativeSetStopwatch.bilateral(target.exercise) && !NativeGpsMeasurement.distance(target.exercise)) {
            val left = seconds(target.set.opt("leftSeconds")); val right = seconds(target.set.opt("rightSeconds"))
            target.set.put("leftSeconds", left).put("rightSeconds", right)
            min(left, right)
        } else seconds(target.set.opt("reps"))
        if (NativeGpsMeasurement.distance(target.exercise)) {
            val meters = target.set.optDouble("distanceMeters", Double.NaN)
            require(meters.isFinite() && meters > 0.0 && meters <= 2_000_000.0) { "invalidDistance" }
        }
        target.set.put("reps", seconds.toString()).put("done", true)
        NativeSetStopwatch.sealPause(next)
        val clock = next.optJSONObject(NativeSetStopwatch.FIELD)
        if (clock?.optString("setId") == target.set.getString("setId")) next.remove(NativeSetStopwatch.FIELD)
        next.put("currentExercise", target.exerciseIndex)
            .put("executionRevision", PhoneWorkoutRecording.advance(frame.getLong("executionRevision")))
        return next
    }

    private fun seconds(value: Any?): Long {
        val text = value?.toString().orEmpty()
        require(text.matches(Regex("[0-9]+"))) { "invalidMeasuredTime" }
        val seconds = text.toLongOrNull()
        require(seconds != null && seconds in 1L..86_400L) { "invalidMeasuredTime" }
        return seconds
    }
}
