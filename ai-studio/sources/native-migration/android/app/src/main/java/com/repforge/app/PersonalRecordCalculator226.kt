package com.repforge.app

import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.PersonalRecordSet226
import androidx.room.withTransaction
import com.repforge.shared.SetEditorRules212
import com.repforge.shared.WorkoutStatsCalculator
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * #226 / #234: Personal Record detector for finished workouts.
 * Compares the new workout's completed sets against prior history and produces
 * the canonical prRecords226 array displayed by the Journal PR badge (.tp226-pr-badge).
 */
internal object PersonalRecordCalculator226 {
    private const val PAGE = 256

    private fun workoutId(workout: JSONObject): String =
        workout.optString("id", workout.optString("workoutId", workout.optString("started")))

    /** Reuses the finalizer's save/calendar transaction; standalone callers also get one snapshot. */
    suspend fun evaluateAndAttachPrs(newWorkout: JSONObject, database: TrainPilotDatabase): JSONArray = database.withTransaction {
        val ids = newWorkout.optJSONArray("exercises")?.let { exercises ->
            (0 until exercises.length()).mapNotNull { exercises.optJSONObject(it)?.optString("id") }
                .filter { it.isNotEmpty() }.distinct()
        }.orEmpty()
        val prior = PriorRecords()
        // Chunk IDs to stay below SQLite bind limits, without truncating a large imported session.
        for (chunk in ids.chunked(128)) {
            var after = Long.MIN_VALUE
            while (true) {
                val rows = database.workoutHistoryDao().scanPersonalRecordSets(chunk,
                    workoutId(newWorkout), newWorkout.optString("started"), after, PAGE)
                for (row in rows) prior.accept(row)
                if (rows.size < PAGE) break
                after = rows.last().setRowId
            }
        }
        evaluate(newWorkout, prior)
    }

    /** Compatibility/reference input; both input paths use one PR accumulator and evaluator. */
    fun evaluateAndAttachPrs(
        newWorkout: JSONObject,
        priorHistory: List<WorkoutHistoryWithDetails>
    ): JSONArray {
        val prior = PriorRecords()
        for (item in priorHistory) {
            if (item.workout.id == workoutId(newWorkout) || item.workout.started == newWorkout.optString("started")) continue
            for (exWithSets in item.exercises) {
                val ex = exWithSets.exercise
                for (s in exWithSets.sets) {
                    if (!s.done) continue
                    prior.accept(PersonalRecordSet226(s.rowId, ex.exerciseId, ex.repUnit,
                        s.weight, s.reps, s.leftSeconds, s.rightSeconds, s.distanceMeters))
                }
            }
        }
        return evaluate(newWorkout, prior)
    }

    private class PriorRecords {
        val weight = HashMap<String, Double>()
        val e1rm = HashMap<String, Double>()
        val reps = HashMap<String, Int>()
        val seconds = HashMap<String, Int>()
        val distance = HashMap<String, Double>()

        fun accept(s: PersonalRecordSet226) {
            val exId = s.exerciseId
            val isBilateral = WorkoutStatsCalculator.isBilateralUnit(s.repUnit, exId)
            val isTimed = WorkoutStatsCalculator.isTimedUnit(s.repUnit, exId)
            val w = s.weight ?: 0.0
            val r = s.reps.replace(',', '.').toDoubleOrNull()?.roundToInt() ?: 0
            val dist = s.distanceMeters ?: 0.0
            if (dist > 0.0) {
                distance[exId] = max(distance[exId] ?: 0.0, dist)
            } else if (isTimed) {
                val sec = if (isBilateral) max(s.leftSeconds ?: 0, max(s.rightSeconds ?: 0, r)) else r
                if (sec > 0) seconds[exId] = max(seconds[exId] ?: 0, sec)
            } else if (w > 0.0 && r > 0) {
                weight[exId] = max(weight[exId] ?: 0.0, w)
                val estimate = w * (1.0 + r / 30.0)
                e1rm[exId] = max(e1rm[exId] ?: 0.0, estimate)
            } else if (r > 0) {
                reps[exId] = max(reps[exId] ?: 0, r)
            }
        }
    }

    private fun evaluate(newWorkout: JSONObject, prior: PriorRecords): JSONArray {
        val priorMaxWeight = prior.weight
        val priorMax1Rm = prior.e1rm
        val priorMaxReps = prior.reps
        val priorMaxSeconds = prior.seconds
        val priorMaxDistance = prior.distance

        val prRecords = JSONArray()
        val exercises = newWorkout.optJSONArray("exercises") ?: return prRecords
        for (i in 0 until exercises.length()) {
            val ex = exercises.optJSONObject(i) ?: continue
            val exId = ex.optString("id")
            if (exId.isEmpty()) continue
            val exName = ex.optString("hu", ex.optString("name", exId))
            val repUnit = ex.optString("repUnit", "ism.")
            val isDistance = ex.optString("measurementType") == "distance" || SetEditorRules212.normalizeUnit(repUnit) in listOf("km", "m")
            val isBilateral = WorkoutStatsCalculator.isBilateralUnit(repUnit, exId)
            val isTimed = WorkoutStatsCalculator.isTimedUnit(repUnit, exId)

            var sessionBestWeight = 0.0
            var sessionBest1Rm = 0.0
            var sessionBestSetText = ""
            var sessionBestReps = 0
            var sessionBestSec = 0
            var sessionBestDist = 0.0

            val sets = ex.optJSONArray("sets") ?: continue
            for (j in 0 until sets.length()) {
                val s = sets.optJSONObject(j) ?: continue
                if (!s.optBoolean("done", false)) continue
                val w = s.optString("weight", "0").replace(',', '.').toDoubleOrNull() ?: 0.0
                val r = s.optString("reps", "0").replace(',', '.').toDoubleOrNull()?.roundToInt() ?: 0
                val dist = s.optDouble("distanceMeters", 0.0)
                if (isDistance && dist > sessionBestDist) {
                    sessionBestDist = dist
                } else if (isTimed) {
                    val left = s.optInt("leftSeconds", 0)
                    val right = s.optInt("rightSeconds", 0)
                    val sec = if (isBilateral) max(left, max(right, r)) else r
                    if (sec > sessionBestSec) sessionBestSec = sec
                } else if (w > 0.0 && r > 0) {
                    if (w > sessionBestWeight) sessionBestWeight = w
                    val e1rm = w * (1.0 + r / 30.0)
                    if (e1rm > sessionBest1Rm) {
                        sessionBest1Rm = e1rm
                        sessionBestSetText = "${WorkoutStatsCalculator.roundOneDecimal(w)} kg × $r"
                    }
                } else if (r > sessionBestReps) {
                    sessionBestReps = r
                }
            }

            // Compare against prior records (only when at least 1 prior session existed for this exercise!)
            if (isDistance && priorMaxDistance.containsKey(exId) && sessionBestDist > (priorMaxDistance[exId] ?: 0.0)) {
                prRecords.put(
                    JSONObject()
                        .put("exerciseId", exId)
                        .put("exerciseName", exName)
                        .put("kind", "distance")
                        .put("value", "${WorkoutStatsCalculator.roundOneDecimal(sessionBestDist / 1000.0)} km")
                        .put("previous", "${WorkoutStatsCalculator.roundOneDecimal((priorMaxDistance[exId] ?: 0.0) / 1000.0)} km")
                )
            } else if (isTimed && priorMaxSeconds.containsKey(exId) && sessionBestSec > (priorMaxSeconds[exId] ?: 0)) {
                prRecords.put(
                    JSONObject()
                        .put("exerciseId", exId)
                        .put("exerciseName", exName)
                        .put("kind", "time")
                        .put("value", "$sessionBestSec mp")
                        .put("previous", "${priorMaxSeconds[exId]} mp")
                )
            } else if (sessionBestWeight > 0.0 && priorMaxWeight.containsKey(exId)) {
                val prevW = priorMaxWeight[exId] ?: 0.0
                val prev1Rm = priorMax1Rm[exId] ?: 0.0
                if (sessionBestWeight > prevW) {
                    prRecords.put(
                        JSONObject()
                            .put("exerciseId", exId)
                            .put("exerciseName", exName)
                            .put("kind", "maxWeight")
                            .put("value", "${WorkoutStatsCalculator.roundOneDecimal(sessionBestWeight)} kg ($sessionBestSetText)")
                            .put("previous", "${WorkoutStatsCalculator.roundOneDecimal(prevW)} kg")
                    )
                } else if (sessionBest1Rm > prev1Rm + 0.2) {
                    prRecords.put(
                        JSONObject()
                            .put("exerciseId", exId)
                            .put("exerciseName", exName)
                            .put("kind", "e1rm")
                            .put("value", "${WorkoutStatsCalculator.roundOneDecimal(sessionBest1Rm)} kg e1RM ($sessionBestSetText)")
                            .put("previous", "${WorkoutStatsCalculator.roundOneDecimal(prev1Rm)} kg e1RM")
                    )
                }
            } else if (sessionBestReps > 0 && priorMaxReps.containsKey(exId) && sessionBestReps > (priorMaxReps[exId] ?: 0)) {
                prRecords.put(
                    JSONObject()
                        .put("exerciseId", exId)
                        .put("exerciseName", exName)
                        .put("kind", "reps")
                        .put("value", "$sessionBestReps ism.")
                        .put("previous", "${priorMaxReps[exId]} ism.")
                )
            }
        }

        if (prRecords.length() > 0) {
            newWorkout.put("prRecords226", prRecords)
        }
        return prRecords
    }
}
