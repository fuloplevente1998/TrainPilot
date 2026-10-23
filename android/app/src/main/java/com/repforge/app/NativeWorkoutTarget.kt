package com.repforge.app

import org.json.JSONObject
import java.util.UUID

/** Captured when a control is rendered. Positions may change; this identity must not. */
data class NativeWorkoutTarget(val workoutId: String, val generation: Long, val exerciseId: String, val setId: String) {
    internal fun resolve(frame: JSONObject): PhoneWorkoutRecording.Target {
        check(workoutId.isNotBlank() && generation > 0 && exerciseId.isNotBlank() && setId.isNotBlank() &&
            workoutId == frame.optString("workoutId") && generation == frame.optLong("generation")) {
            "Az edzés időközben megváltozott."
        }
        val matches = ArrayList<PhoneWorkoutRecording.Target>()
        val exercises = frame.getJSONArray("exercises")
        for (e in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(e)
            val sets = exercise.getJSONArray("sets")
            for (s in 0 until sets.length()) if (sets.getJSONObject(s).optString("setId") == setId) {
                check(exercise.optString("id") == exerciseId) { "A sorozat célpontja megváltozott." }
                matches.add(PhoneWorkoutRecording.Target(exercise, sets.getJSONObject(s), e))
            }
        }
        check(matches.size == 1) { "A sorozat már nem elérhető vagy azonosítója nem egyértelmű." }
        return matches.single()
    }

    /** Removing a row must never allow its identity to be reused by its replacement. */
    internal fun appendSet(frame: JSONObject) {
        val exercise = resolve(frame).exercise
        val sets = exercise.getJSONArray("sets")
        check(sets.length() < 10) { "Legfeljebb 10 sorozat adható meg." }
        val last = sets.optJSONObject(sets.length() - 1)
        val added = JSONObject().put("set", sets.length() + 1).put("setId", UUID.randomUUID().toString())
            .put("weight", last?.optString("weight", "0") ?: "0").put("reps", "").put("done", false)
        if (last?.has("leftSeconds") == true) added.put("leftSeconds", "")
        if (last?.has("rightSeconds") == true) added.put("rightSeconds", "")
        sets.put(added)
    }
}
