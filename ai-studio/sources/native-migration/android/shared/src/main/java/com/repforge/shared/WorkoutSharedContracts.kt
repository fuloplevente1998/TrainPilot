package com.repforge.shared

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Canonical Data Layer paths shared between Phone (:app) and Watch (:wear). */
object WearDataLayerPaths {
    const val ACTIVE_WORKOUT_PATH = "/trainpilot/active-workout"
    const val WATCH_HOME_PATH = "/trainpilot/watch-home"
    const val PHONE_AUTOMATIC_PATH = "/trainpilot/phone-automatic"
    const val WATCH_AUTOMATIC_PATH = "/trainpilot/watch-automatic"
    const val WATCH_HEALTH_PATH = "/trainpilot/watch-health"
    const val WORKOUT_REQUEST_PATH = "/trainpilot/workout-request"
    const val WORKOUT_REPLY_PATH = "/trainpilot/workout-reply"
    const val COMMAND_PATH_PREFIX = "/trainpilot/workout-command/"
    const val RECEIPT_PATH_PREFIX = "/trainpilot/command-receipt/"
    const val AUTOMATIC_COMMAND_PREFIX = "/trainpilot/automatic-command/"
    const val OWNERSHIP_PATH_PREFIX = WorkoutOwnership193.PATH
}

/** Shared #188 Undo Set guard and inverse logic used by both Phone and Watch. */
object WorkoutUndo188Shared {
    const val QUICK_MILLIS: Long = 8000L

    @JvmStatic
    fun number(set: JSONObject, key: String): Double {
        val text = set.optString(key, "").replace(',', '.')
        return if (text.isEmpty()) 0.0 else text.toDoubleOrNull() ?: 0.0
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun guard(frame: JSONObject): JSONArray {
        val rows = JSONArray()
        val exercises = frame.getJSONArray("exercises")
        for (e in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(e)
            val sets = exercise.getJSONArray("sets")
            for (s in 0 until sets.length()) {
                val set = sets.getJSONObject(s)
                val row = JSONArray()
                    .put(exercise.getString("id"))
                    .put(set.getString("setId"))
                    .put(set.getBoolean("done"))
                for (key in arrayOf("reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
                    row.put(number(set, key))
                }
                rows.put(row)
            }
        }
        return rows
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun same(left: JSONArray?, right: JSONArray?): Boolean {
        if (left == null || right == null || left.length() != right.length()) return false
        for (i in 0 until left.length()) {
            val a = left.getJSONArray(i)
            val b = right.getJSONArray(i)
            if (a.length() != 8 || b.length() != 8) return false
            for (j in 0..2) {
                if (a.get(j) != b.get(j)) return false
            }
            for (j in 3..7) {
                if (a.getDouble(j) != b.getDouble(j)) return false
            }
        }
        return true
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun values(set: JSONObject): JSONObject {
        val out = JSONObject().put("done", set.getBoolean("done"))
        for (key in arrayOf("reps", "weight", "leftSeconds", "rightSeconds", "distanceMeters")) {
            val raw = set.opt(key)
            out.put(key, raw ?: if ("reps" == key) "" else 0)
        }
        return out
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun find(frame: JSONObject, target: JSONObject): JSONObject {
        val exercises = frame.getJSONArray("exercises")
        val targetExerciseId = target.getString("exerciseId")
        val targetSetId = target.getString("setId")
        for (e in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(e)
            if (exercise.getString("id") != targetExerciseId) continue
            val sets = exercise.getJSONArray("sets")
            for (s in 0 until sets.length()) {
                val set = sets.getJSONObject(s)
                if (set.getString("setId") == targetSetId) return set
            }
        }
        throw IllegalArgumentException("undoTargetChanged")
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun capture(before: JSONObject, after: JSONObject, target: JSONObject, recordId: String): JSONObject {
        return JSONObject()
            .put("recordId", recordId)
            .put("workoutId", before.getString("workoutId"))
            .put("native188", true)
            .put("exerciseId", target.getString("exerciseId"))
            .put("setId", target.getString("setId"))
            .put("before", values(find(before, target)))
            .put("expected", guard(after))
            .put("recordedAt", System.currentTimeMillis())
    }
}

/** Shared #212 Rotary/Touch Editor increments and #229 per-set RIR validation rules. */
object SetEditorRules212 {
    @JvmStatic
    fun normalizeUnit(unit: String?): String {
        return unit.orEmpty().replace("\\s+".toRegex(), "").lowercase()
    }

    @JvmStatic
    fun step(field: String, unit: String?): Double {
        return when {
            field == "weight" -> 0.5
            field.endsWith("Seconds") || (field == "reps" && normalizeUnit(unit).startsWith("mp")) -> 5.0
            else -> 1.0
        }
    }

    @JvmStatic
    fun adjust(value: Double, step: Double, count: Int): Double {
        return if (!value.isFinite() || !step.isFinite() || step <= 0.0) {
            0.0
        } else {
            (value + step * count).coerceIn(0.0, Int.MAX_VALUE.toDouble())
        }
    }

    /** #229: Missing RIR is distinct from zero; only finite integers in 0..10 are valid. */
    @JvmStatic
    fun parseActualRir229(data: JSONObject?): Int? {
        if (data == null) return null
        val value = data.opt("actualRir") as? Number ?: return null
        val number = value.toDouble()
        return if (number.isFinite() && number in 0.0..10.0 && number == number.toInt().toDouble()) {
            number.toInt()
        } else {
            null
        }
    }

    @JvmStatic
    fun isValidActualRir229(raw: Any?): Boolean {
        if (raw == null || raw === JSONObject.NULL) return true
        if (raw !is Number) return false
        val d = raw.toDouble()
        return d.isFinite() && d in 0.0..10.0 && d == raw.toInt().toDouble()
    }
}

/** Shared Phase 4 Exercise Statistics & Epley 1RM Calculator. */
object WorkoutStatsCalculator {
    data class SetObservation(
        val weightKg: Double,
        val reps: Int,
        val leftSeconds: Int,
        val rightSeconds: Int,
        val distanceMeters: Double,
        val isTimed: Boolean,
        val isBilateral: Boolean,
        val timestampMs: Long
    ) {
        val seconds: Int
            get() = when {
                isBilateral -> max(leftSeconds, max(rightSeconds, reps))
                isTimed -> reps
                else -> 0
            }

        /** Epley 1RM formula from Phase 4: w * (1 + r / 30.0) */
        val estimatedOneRepMax: Double
            get() = if (!isTimed && weightKg > 0.0 && reps > 0) weightKg * (1.0 + reps / 30.0) else 0.0

        val volumeKg: Double
            get() = if (!isTimed && weightKg > 0.0 && reps > 0) weightKg * reps else 0.0
    }

    data class ExerciseSummaryStats(
        val kind: String, // "distance" | "bilateral" | "timed" | "weighted" | "reps"
        val maxWeightKg: Double,
        val estimated1RmKg: Double,
        val bestSetWeightKg: Double,
        val bestSetReps: Int,
        val maxSessionVolumeKg: Double,
        val maxReps: Int,
        val maxSeconds: Int,
        val maxLeftSeconds: Int,
        val maxRightSeconds: Int,
        val maxDistanceMeters: Double,
        val totalSets: Int,
        val totalSessions: Int
    )

    @JvmStatic
    fun isBilateralUnit(repUnit: String?, exerciseId: String?): Boolean {
        val unit = repUnit.orEmpty().lowercase()
        return unit.contains("oldal") || unit.contains("side") || exerciseId == "side-plank"
    }

    @JvmStatic
    fun isTimedUnit(repUnit: String?, exerciseId: String?): Boolean {
        if (isBilateralUnit(repUnit, exerciseId)) return true
        val unit = repUnit.orEmpty().lowercase()
        return Regex("\\bmp\\b|sec|second").containsMatchIn(unit)
    }

    @JvmStatic
    fun roundOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }
}
