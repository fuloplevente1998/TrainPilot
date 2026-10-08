package com.repforge.app.wear

import android.content.Context
import org.json.JSONArray
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
    val sets: List<WearSet>,
    val targetReps: String = ""
)

data class WearWorkout(
    val revision: Long,
    val programId: String,
    val programName: String,
    val dayId: String,
    val scheduleId: String,
    val started: String,
    val workoutId: String,
    val currentExercise: Int,
    val restSeconds: Int,
    val restEndAt: Long,
    val exercises: List<WearExercise>,
    val dayName: String = "",
    val localSequence: Long = 0L
) {
    fun elapsedSeconds(now: Long = System.currentTimeMillis()): Int = try {
        ((now - java.time.Instant.parse(started).toEpochMilli()).coerceAtLeast(0L) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    } catch (_: Exception) { 0 }

    val completedSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.done } }
    val exerciseComplete: Boolean get() = exercise?.sets?.let { it.isNotEmpty() && it.all { set -> set.done } } ?: false
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

    /** Rest never completes a set or changes the selected exercise. */
    fun endRest(): WearWorkout = if (restEndAt == 0L) this else copy(restEndAt = 0L)

    /** Explicit navigation preserves all measurements and completion flags. */
    fun selectExercise(index: Int): WearWorkout = if (index !in exercises.indices || index == currentExercise) this
        else copy(currentExercise = index, restEndAt = 0L)
}

object WorkoutSnapshotStore {
    private const val PREFS = "trainpilot_wear_snapshot"
    private const val SNAPSHOT = "snapshot"

    @Synchronized
    fun save(context: Context, raw: String?): WearWorkout? {
        val current = load(context)
        if (raw.isNullOrBlank()) return current
        val root = try { JSONObject(raw) } catch (_: Exception) { return current }
        val revision = root.optLong("revision", 0L)
        if (revision < (current?.revision ?: 0L)) return current
        if (!root.optBoolean("active", true)) {
            // Do not let an old phone tombstone erase a newly started offline workout.
            val localStarted = try { java.time.Instant.parse(current?.started).toEpochMilli() } catch (_: Exception) { 0L }
            if (current?.revision == 0L && (current.localSequence > 0L || root.optLong("syncedAt") < localStarted)) return current
            clear(context)
            return null
        }
        val parsed = parse(raw) ?: return current
        if (WearClosureStore.isClosed(context, parsed.workoutId)) return current
        if (current?.revision == 0L && current.localSequence > 0L && parsed.workoutId != current.workoutId) return current
        if (current?.workoutId == parsed.workoutId && parsed.localSequence < current.localSequence) return current
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(SNAPSHOT, raw).apply()
        return parsed
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(SNAPSHOT).apply()
    }

    @Synchronized
    fun saveWorkout(context: Context, workout: WearWorkout): WearWorkout {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SNAPSHOT, encode(workout).toString())
            .apply()
        return workout
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
                            sets = sets,
                            targetReps = exercise.optString("targetReps")
                        )
                    )
                }
            }
            if (exercises.isEmpty()) return null
            WearWorkout(
                revision = root.optLong("revision", 0L),
                programId = root.optString("programId"),
                programName = root.optString("programName"),
                dayId = root.optString("dayId"),
                scheduleId = root.optString("scheduleId"),
                started = root.optString("started"),
                workoutId = root.optString("workoutId", root.optString("started")),
                currentExercise = root.optInt("currentExercise", 0).coerceIn(0, exercises.lastIndex),
                restSeconds = root.optInt("restSeconds", 90).coerceAtLeast(1),
                restEndAt = root.optLong("restEndAt", 0L),
                exercises = exercises,
                dayName = root.optString("dayName", root.optString("dayId")),
                localSequence = root.optLong("localSequence", root.optLong("watchSequence", 0L))
            )
        } catch (_: Exception) { null }
    }

    fun encode(workout: WearWorkout): JSONObject {
        val root = JSONObject()
            .put("active", true)
            .put("revision", workout.revision)
            .put("programId", workout.programId)
            .put("dayName", workout.dayName)
            .put("localSequence", workout.localSequence)
            .put("programName", workout.programName)
            .put("dayId", workout.dayId)
            .put("scheduleId", workout.scheduleId)
            .put("started", workout.started)
            .put("workoutId", workout.workoutId)
            .put("currentExercise", workout.currentExercise)
            .put("restSeconds", workout.restSeconds)
            .put("restEndAt", workout.restEndAt)
        val exercises = JSONArray()
        workout.exercises.forEach { exercise ->
            val sets = JSONArray()
            exercise.sets.forEach { set ->
                sets.put(
                    JSONObject()
                        .put("set", set.number)
                        .put("reps", set.reps)
                        .put("weight", set.weight)
                        .put("done", set.done)
                        .put("leftSeconds", set.leftSeconds)
                        .put("rightSeconds", set.rightSeconds)
                        .put("distanceMeters", set.distanceMeters)
                )
            }
            exercises.put(
                JSONObject()
                    .put("id", exercise.id)
                    .put("name", exercise.name)
                    .put("loadType", exercise.loadType)
                    .put("repUnit", exercise.repUnit)
                    .put("measurementType", exercise.measurementType)
                    .put("targetReps", exercise.targetReps)
                    .put("sets", sets)
            )
        }
        return root.put("exercises", exercises)
    }
}
