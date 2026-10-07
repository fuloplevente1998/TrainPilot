package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject

data class WearSet(
    val number: Int,
    val reps: String,
    val weight: String,
    val done: Boolean,
    val leftSeconds: Int,
    val rightSeconds: Int,
    val distanceMeters: Double
)

data class WearExercise(
    val id: String,
    val name: String,
    val loadType: String,
    val repUnit: String,
    val measurementType: String,
    val sets: List<WearSet>
)

data class WearWorkout(
    val revision: Long,
    val programName: String,
    val workoutId: String,
    val currentExercise: Int,
    val restSeconds: Int,
    val restEndAt: Long,
    val exercises: List<WearExercise>
) {
    val exercise: WearExercise?
        get() = exercises.getOrNull(currentExercise.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)))

    val currentSetIndex: Int
        get() {
            val sets = exercise?.sets.orEmpty()
            val firstOpen = sets.indexOfFirst { !it.done }
            return if (firstOpen >= 0) firstOpen else (sets.size - 1).coerceAtLeast(0)
        }

    val currentSet: WearSet?
        get() = exercise?.sets?.getOrNull(currentSetIndex)
}

object WorkoutSnapshotStore {
    private const val PREFS = "trainpilot_wear_snapshot"
    private const val SNAPSHOT = "snapshot"

    fun save(context: Context, raw: String?): WearWorkout? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (raw.isNullOrBlank()) {
            prefs.edit().remove(SNAPSHOT).apply()
            return null
        }
        val parsed = parse(raw)
        if (parsed == null) {
            prefs.edit().remove(SNAPSHOT).apply()
            return null
        }
        prefs.edit().putString(SNAPSHOT, raw).apply()
        return parsed
    }

    fun load(context: Context): WearWorkout? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SNAPSHOT, null)
        return raw?.let(::parse)
    }

    fun parse(raw: String): WearWorkout? {
        return try {
            val root = JSONObject(raw)
            if (!root.optBoolean("active", true)) return null
            val source = root.optJSONArray("exercises") ?: return null
            val exercises = buildList {
                for (i in 0 until source.length()) {
                    val exercise = source.optJSONObject(i) ?: continue
                    val setArray = exercise.optJSONArray("sets")
                    val sets = buildList {
                        if (setArray != null) for (j in 0 until setArray.length()) {
                            val set = setArray.optJSONObject(j) ?: continue
                            add(
                                WearSet(
                                    number = set.optInt("set", j + 1),
                                    reps = set.opt("reps")?.toString().orEmpty(),
                                    weight = set.opt("weight")?.toString().orEmpty(),
                                    done = set.optBoolean("done", false),
                                    leftSeconds = set.optInt("leftSeconds", 0),
                                    rightSeconds = set.optInt("rightSeconds", 0),
                                    distanceMeters = set.optDouble("distanceMeters", 0.0)
                                )
                            )
                        }
                    }
                    add(
                        WearExercise(
                            id = exercise.optString("id"),
                            name = exercise.optString("name", exercise.optString("id")),
                            loadType = exercise.optString("loadType"),
                            repUnit = exercise.optString("repUnit"),
                            measurementType = exercise.optString("measurementType"),
                            sets = sets
                        )
                    )
                }
            }
            if (exercises.isEmpty()) return null
            WearWorkout(
                revision = root.optLong("revision", 0L),
                programName = root.optString("programName"),
                workoutId = root.optString("workoutId"),
                currentExercise = root.optInt("currentExercise", 0).coerceIn(0, exercises.lastIndex),
                restSeconds = root.optInt("restSeconds", 90).coerceAtLeast(1),
                restEndAt = root.optLong("restEndAt", 0L),
                exercises = exercises
            )
        } catch (_: Exception) { null }
    }
}
