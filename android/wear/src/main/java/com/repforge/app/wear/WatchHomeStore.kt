package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject

data class WatchHomeSet(
    val number: Int,
    val weight: String
)

data class WatchHomeExercise(
    val id: String,
    val name: String,
    val loadType: String,
    val repUnit: String,
    val measurementType: String,
    val targetReps: String,
    val sets: List<WatchHomeSet>
)

data class WatchHomeDay(
    val id: String,
    val name: String,
    val programId: String,
    val programName: String,
    val exercises: List<WatchHomeExercise>
)

data class WatchHomeRecommendation(
    val scheduleId: String,
    val plannedStart: String,
    val day: WatchHomeDay
)

data class WatchHomeSnapshot(
    val activeProgramId: String,
    val activeProgramName: String,
    val restSeconds: Int,
    val hasDraft: Boolean,
    val recommended: WatchHomeRecommendation?,
    val days: List<WatchHomeDay>
) {
    fun createWorkout(day: WatchHomeDay, scheduleId: String, started: String): WearWorkout {
        return WearWorkout(
            revision = 0L,
            programId = day.programId,
            programName = day.programName,
            dayId = day.id,
            scheduleId = scheduleId,
            started = started,
            workoutId = started,
            currentExercise = 0,
            restSeconds = restSeconds,
            restEndAt = 0L,
            exercises = day.exercises.map { exercise ->
                WearExercise(
                    id = exercise.id,
                    name = exercise.name,
                    loadType = exercise.loadType,
                    repUnit = exercise.repUnit,
                    measurementType = exercise.measurementType,
                    sets = exercise.sets.map { set ->
                        WearSet(
                            number = set.number,
                            reps = "",
                            weight = set.weight,
                            done = false,
                            leftSeconds = 0,
                            rightSeconds = 0,
                            distanceMeters = 0.0
                        )
                    }
                )
            }
        )
    }
}

object WatchHomeStore {
    private const val PREFS = "trainpilot_wear_home"
    private const val SNAPSHOT = "snapshot"

    fun save(context: Context, raw: String?): WatchHomeSnapshot? {
        if (raw.isNullOrBlank()) return null
        val parsed = parse(raw) ?: return null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SNAPSHOT, raw)
            .apply()
        return parsed
    }

    fun load(context: Context): WatchHomeSnapshot? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SNAPSHOT, null)
        return raw?.let(::parse)
    }

    fun parse(raw: String): WatchHomeSnapshot? {
        return try {
            val root = JSONObject(raw)
            val daysArray = root.optJSONArray("days")
            val days = buildList {
                if (daysArray != null) for (i in 0 until daysArray.length()) {
                    parseDay(daysArray.optJSONObject(i))?.let(::add)
                }
            }
            val recommendationObject = root.optJSONObject("recommended")
            val recommendationDay = parseDay(recommendationObject?.optJSONObject("day"))
            val recommendation = if (recommendationObject != null && recommendationDay != null) {
                WatchHomeRecommendation(
                    scheduleId = recommendationObject.optString("scheduleId"),
                    plannedStart = recommendationObject.optString("plannedStart"),
                    day = recommendationDay
                )
            } else null
            WatchHomeSnapshot(
                activeProgramId = root.optString("activeProgramId"),
                activeProgramName = root.optString("activeProgramName", "TrainPilot"),
                restSeconds = root.optInt("restSeconds", 90).coerceAtLeast(1),
                hasDraft = root.optBoolean("hasDraft", false),
                recommended = recommendation,
                days = days
            )
        } catch (_: Exception) { null }
    }

    private fun parseDay(source: JSONObject?): WatchHomeDay? {
        source ?: return null
        val exerciseArray = source.optJSONArray("exercises") ?: return null
        val exercises = buildList {
            for (i in 0 until exerciseArray.length()) {
                val exercise = exerciseArray.optJSONObject(i) ?: continue
                val setArray = exercise.optJSONArray("sets")
                val sets = buildList {
                    if (setArray != null) for (j in 0 until setArray.length()) {
                        val set = setArray.optJSONObject(j) ?: continue
                        add(WatchHomeSet(set.optInt("set", j + 1), set.opt("weight")?.toString().orEmpty()))
                    }
                }
                add(
                    WatchHomeExercise(
                        id = exercise.optString("id"),
                        name = exercise.optString("name", exercise.optString("id")),
                        loadType = exercise.optString("loadType"),
                        repUnit = exercise.optString("repUnit"),
                        measurementType = exercise.optString("measurementType"),
                        targetReps = exercise.optString("targetReps"),
                        sets = sets
                    )
                )
            }
        }
        if (exercises.isEmpty()) return null
        return WatchHomeDay(
            id = source.optString("id"),
            name = source.optString("name", source.optString("id")),
            programId = source.optString("programId"),
            programName = source.optString("programName", "TrainPilot"),
            exercises = exercises
        )
    }
}
