package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Read-only, bounded projection of saved canonical phone workouts. Never a second workout store. */
data class MiniJournalSet(
    val number: Int,
    val reps: String,
    val weight: Double?,
    val leftSeconds: Double?,
    val rightSeconds: Double?,
    val distanceMeters: Double?
)

data class MiniJournalExercise(val name: String, val sets: List<MiniJournalSet>)

data class MiniJournalMetric(
    val source: String,
    val partial: Boolean = false,
    val averageHeartRate: Double? = null,
    val maxHeartRate: Double? = null,
    val totalCalories: Double? = null,
    val workoutCalories: Double? = null,
    val activeCalories: Double? = null,
    val steps: Double? = null,
    val activeDurationSeconds: Double? = null,
    val exerciseMinutes: Double? = null
)

data class MiniJournalWorkout(
    val workoutId: String,
    val started: String,
    val finished: String,
    val dayName: String,
    val quickWorkout: Boolean,
    val durationSeconds: Int,
    val exercises: List<MiniJournalExercise>,
    val wear: MiniJournalMetric?,
    val healthConnect: MiniJournalMetric?
)

object WatchMiniJournal {
    private fun number(value: JSONObject, key: String, min: Double = 0.0, max: Double = 1e7): Double? {
        val raw = value.opt(key)
        if (raw !is Number) return null
        return raw.toDouble().takeIf { it.isFinite() && it >= min && it <= max }
    }

    private fun metric(value: JSONObject?, kind: String): MiniJournalMetric? {
        value ?: return null
        val source = value.optString("source")
        if (kind == "wear" && source != "wear_health_services") return null
        if (kind == "health" && source !in listOf("health_connect", "samsung_health")) return null
        return MiniJournalMetric(
            source = source,
            partial = value.optBoolean("partial", false),
            averageHeartRate = number(value, "averageHeartRate", 20.0, 250.0),
            maxHeartRate = number(value, "maxHeartRate", 20.0, 250.0),
            totalCalories = number(value, "totalCalories", 0.0, 100000.0),
            workoutCalories = number(value, "workoutCalories", 0.0, 100000.0),
            activeCalories = number(value, "activeCalories", 0.0, 100000.0),
            steps = number(value, "steps", 0.0, 1000000.0),
            activeDurationSeconds = number(value, "activeDurationSeconds", 0.0, 604800.0),
            exerciseMinutes = number(value, "exerciseMinutes", 0.0, 10080.0)
        )
    }

    fun parse(entries: JSONArray?): List<MiniJournalWorkout> {
        if (entries == null) return emptyList()
        val seen = mutableSetOf<String>()
        return buildList {
            for (i in 0 until entries.length().coerceAtMost(8)) {
                val row = entries.optJSONObject(i) ?: continue
                val id = row.optString("workoutId").take(96)
                val start = row.optString("started").take(40)
                val finish = row.optString("finished").take(40)
                if (id.isBlank() || !seen.add(id)) continue
                val started = try { Instant.parse(start) } catch (_: Exception) { continue }
                val finished = try { Instant.parse(finish) } catch (_: Exception) { continue }
                if (finished < started) continue
                val exercises = buildList {
                    val raw = row.optJSONArray("exercises")
                    if (raw != null) for (j in 0 until raw.length().coerceAtMost(14)) {
                        val exercise = raw.optJSONObject(j) ?: continue
                        val sets = buildList {
                            val array = exercise.optJSONArray("sets")
                            if (array != null) for (k in 0 until array.length().coerceAtMost(12)) {
                                val set = array.optJSONObject(k) ?: continue
                                add(MiniJournalSet(
                                    number = set.optInt("number", k + 1).coerceIn(1, 1000),
                                    reps = set.optString("reps").take(16),
                                    weight = number(set, "weight", 0.0, 100000.0),
                                    leftSeconds = number(set, "leftSeconds", 0.0, 604800.0),
                                    rightSeconds = number(set, "rightSeconds", 0.0, 604800.0),
                                    distanceMeters = number(set, "distanceMeters", 0.0, 1000000.0)
                                ))
                            }
                        }
                        if (sets.isNotEmpty()) add(MiniJournalExercise(
                            name = exercise.optString("name", "Gyakorlat").take(48),
                            sets = sets
                        ))
                    }
                }
                val duration = ((finished.toEpochMilli() - started.toEpochMilli()) / 1000)
                    .coerceIn(0L, 604800L).toInt()
                add(MiniJournalWorkout(
                    workoutId = id,
                    started = start, finished = finish,
                    dayName = row.optString("dayName", "Edzés").take(48),
                    quickWorkout = row.optBoolean("quickWorkout", false),
                    durationSeconds = duration,
                    exercises = exercises,
                    wear = metric(row.optJSONObject("wear"), "wear"),
                    healthConnect = metric(row.optJSONObject("healthConnect"), "health")
                ))
            }
        }
    }
}
