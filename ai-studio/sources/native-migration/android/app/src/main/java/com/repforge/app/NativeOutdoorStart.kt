package com.repforge.app

import org.json.JSONObject

/** Accepted #216 outdoor allowlist, independent from mere distance/speed health summaries. */
internal object NativeOutdoorStart {
    private val outdoor = setOf("running", "jogging", "walking", "hiking", "cycling", "inline-skating")
    fun eligible(id: String, measurementType: String, repUnit: String): Boolean =
        id in outdoor && (measurementType == "distance" || repUnit in setOf("km", "m"))

    /** Revalidated after service readiness and again by the normal GPS command after permission. */
    fun target(frame: JSONObject, exerciseId: String): NativeWorkoutTarget {
        check(frame.optBoolean("active") && frame.optString("timerOwner") == "phone" &&
            !frame.optBoolean("workoutPaused") && !frame.optBoolean("finalizationPending") && !com.repforge.shared.WorkoutOwnership193.pending(frame) &&
            frame.optInt("nativeEditorProjection") == 1) { "Az edzés már nem indíthat GPS-mérést." }
        val exercises = frame.getJSONArray("exercises")
        val exercise = exercises.getJSONObject(frame.optInt("currentExercise"))
        check(exercise.optString("id") == exerciseId && eligible(exerciseId,
            exercise.optString("measurementType"), exercise.optString("repUnit"))) { "A GPS célgyakorlata megváltozott." }
        val rows = exercise.getJSONArray("sets")
        val set = (0 until rows.length()).map { rows.getJSONObject(it) }.firstOrNull { !it.optBoolean("done") }
            ?: error("Nincs mérhető sorozat.")
        return NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exerciseId, set.getString("setId"))
            .also { it.resolve(frame) }
    }
}
