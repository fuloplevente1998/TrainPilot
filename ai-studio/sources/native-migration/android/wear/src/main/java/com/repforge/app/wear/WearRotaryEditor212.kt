package com.repforge.app.wear

/** One physical detent uses the same increment as the editor's touch controls. */
internal class WearRotaryEditor212 {
    private var remainder = 0.0
    fun reset() { remainder = 0.0 }
    fun steps(pixels: Float, pixelsPerDetent: Float): Int {
        if (!pixels.isFinite() || !pixelsPerDetent.isFinite() || pixelsPerDetent <= 0f) { reset(); return 0 }
        val delta = pixels.toDouble() / pixelsPerDetent
        if (delta * remainder < 0) reset()
        val total = (remainder + delta).coerceIn(-20.0, 20.0)
        val whole = total.toInt()
        remainder = total - whole
        return whole
    }
    companion object {
        fun step(field: String, unit: String): Double = when {
            field == "weight" -> .5
            field.endsWith("Seconds") || (field == "reps" && normalizeUnit(unit).startsWith("mp")) -> 5.0
            else -> 1.0
        }
        // The existing editor displays whole values through Int. Avoid overflow
        // and non-finite values at that representational boundary, also by touch.
        fun adjust(value: Double, step: Double, count: Int): Double =
            if (!value.isFinite() || !step.isFinite() || step <= 0) 0.0
            else (value + step * count).coerceIn(0.0, Int.MAX_VALUE.toDouble())
        fun target(workout: WearWorkout): String = listOf(workout.workoutId, workout.started,
            workout.currentExercise.toString(), workout.exercise?.id.orEmpty(),
            workout.currentSetIndex.toString(), WearAutomaticEngine.targetId(workout, workout.currentExercise, workout.currentSetIndex)).joinToString("|")
        fun editable(workout: WearWorkout, expectedTarget: String): Boolean =
            expectedTarget == target(workout) && workout.currentSet?.done == false && !workout.workoutPaused &&
                !WearUndo188.blocked(workout) &&
                (workout.automation?.counting != true || workout.automation.paused)
    }
}
