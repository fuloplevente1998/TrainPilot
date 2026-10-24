package com.repforge.app.ui.coach

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.floor

/** Read-only port of main 7473b30: tp140/tp128 recommendation, followed by final rf233 advice. */
internal object NativeCoachExerciseAdvice {
    data class Target(val kind: String, val min: Double? = null, val max: Double? = null,
        val rirMin: Double? = null, val rirMax: Double? = null, val sets: Int = 0, val text: String = "")
    data class Occurrence(val workout: JSONObject, val exercise: JSONObject, val reps: List<Double>,
        val weight: Double, val allDone: Boolean, val effort: String, val hitTop: Boolean,
        val belowMin: Boolean, val average: Double?) {
        val safe get() = effort in setOf("easy", "light", "good")
        val adverse get() = effort in setOf("challenging", "hard", "pain")
    }
    data class Variation(val exerciseId: String, val name: String, val level: Int)
    data class Advice(val action: String, val reason: String, val weight: Double, val baseWeight: Double,
        val autoApply: Boolean, val historyCount: Int, val target: Target,
        val nextReps: List<Double> = emptyList(), val variation: Variation? = null)

    private val isolation = setOf("lateral-raise", "db-reverse-fly", "face-pull", "db-curl", "hammer-curl",
        "cable-curl", "oh-triceps", "cable-triceps", "leg-extension", "leg-curl", "calf-raise")
    fun target(library: JSONObject, prescription: JSONObject): Target {
        val text = prescription.optString("reps", library.optString("reps"))
        val value = text.trim().lowercase().replace('−', '-').replace('–', '-')
        val decimal = "(\\d+(?:[.,]\\d+)?)"
        val rir = Regex("$decimal\\s*(?:-\\s*$decimal\\s*)?rir\\b").find(value)
        val a = rir?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: number(prescription, "rir")
        val b = rir?.groupValues?.get(2)?.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDoubleOrNull() ?: a
        val low = if (a != null && b != null) minOf(a, b) else a
        val high = if (a != null && b != null) maxOf(a, b) else b
        val sets = number(prescription, "sets")?.toInt() ?: number(library, "sets")?.toInt() ?: 0
        if (Regex("$decimal(?:\\s*-\\s*$decimal)?\\s*rir").matches(value))
            return Target("rir", rirMin = low, rirMax = high, sets = sets, text = text)
        val timed = Regex("\\b(mp|másodperc|sec|second)\\b").containsMatchIn(value)
        val range = Regex("(\\d+)\\s*-\\s*(\\d+)").find(value)
        val single = Regex("\\d+").find(value)?.value?.toDoubleOrNull()
        val first = range?.groupValues?.get(1)?.toDoubleOrNull() ?: single
        val second = range?.groupValues?.get(2)?.toDoubleOrNull() ?: single
        return Target(if (first != null) if (timed) "time" else "reps" else if (value.contains("rir")) "rir" else if (timed) "time" else "open",
            if (first != null && second != null) minOf(first, second) else null,
            if (first != null && second != null) maxOf(first, second) else null, low, high, sets, text)
    }
    fun occurrence(workout: JSONObject, exercise: JSONObject, target: Target): Occurrence {
        val rows = exercise.optJSONArray("sets")
        val sets = (0 until (rows?.length() ?: 0)).mapNotNull { rows?.optJSONObject(it) }
        val done = sets.filter { it.optBoolean("done") }
        val reps = done.mapNotNull { number(it, "reps") }
        val weights = done.mapNotNull { number(it, "weight") }.sorted()
        val weight = if (weights.isEmpty()) 0.0 else if (weights.size % 2 == 1) weights[weights.size / 2]
            else (weights[weights.size / 2 - 1] + weights[weights.size / 2]) / 2
        val all = sets.isNotEmpty() && done.size == sets.size && (target.sets <= 0 || done.size >= target.sets)
        val range = target.kind in setOf("reps", "time")
        return Occurrence(workout, exercise, reps, weight, all,
            exercise.optString("effort").ifBlank { workout.optJSONObject("feedback")?.optString("effort").orEmpty() },
            range && all && target.max != null && reps.isNotEmpty() && reps.min() >= target.max,
            range && target.min != null && reps.isNotEmpty() && reps.min() < target.min, reps.takeIf { it.isNotEmpty() }?.average())
    }
    fun step(library: JSONObject, base: Double, overrides: Map<String, Double>, observed: Double?): Double {
        val id = library.optString("id"); val load = library.optString("loadType")
        val isolated = id in isolation
        val fallback = when {
            load == "bodyweight" -> 0.0
            isolated -> if (load == "total") 1.0 else .5
            load in setOf("per_hand", "single_dumbbell") -> if (base < 5) .5 else 1.0
            load == "total" -> if (base < 20) 2.0 else 2.5
            else -> 1.0
        }
        val configured = overrides[if (isolated) "isolation" else load]
        if (configured != null && configured.isFinite() && configured in .25..10.0) return configured
        if (observed != null && observed.isFinite() && observed >= .25 && observed <= (if (isolated) fallback else maxOf(fallback * 2, 1.0))) return observed
        return fallback
    }
    fun recommend(library: JSONObject, prescription: JSONObject, recent: List<Occurrence>, target: Target,
        readiness: NativeCoachReadiness.Result, step: Double): Advice {
        val rows = recent.take(4); val latest = rows.firstOrNull(); val count = rows.size
        val body = prescription.optString("loadType", library.optString("loadType")) == "bodyweight"
        val base = latest?.weight ?: 0.0
        fun answer(action: String, reason: String, weight: Double = base, auto: Boolean = false,
            next: List<Double> = emptyList(), variation: Variation? = null) =
            Advice(action, reason, weight, base, auto, count, target, next, variation)
        fun nextWeight(direction: Int) = maxOf(0.0, floor((base + direction * step) * 4 + .5) / 4)
        if (library.optString("measurementType") == "distance" || library.optString("repUnit").trim().lowercase() in setOf("km", "m"))
            return answer("hold", "distance", 0.0).copy(historyCount = 0)
        if (latest == null) return answer("start", "start", if (body) 0.0 else number(library, "weight") ?: 0.0)
        if (rows.take(3).any { it.effort == "pain" || it.workout.optJSONObject("feedback")?.optString("rating") == "pain" })
            return answer("pain", "pain")
        if (readiness.trainpilotObservations > 0 && (readiness.score ?: Double.POSITIVE_INFINITY) < 55)
            return answer("hold", "recovery")
        if (!latest.allDone || latest.effort == "challenging" || latest.workout.optJSONObject("feedback")?.optString("rating") == "hard")
            return answer("hold", if (!latest.allDone) "partial" else "hold")
        val trend = rows.take(3)
        val same = trend.all { abs(it.weight - base) < .01 }
        val declining = trend.size == 3 && same && trend.all { it.allDone && (it.effort == "hard" || it.belowMin) } &&
            trend.zipWithNext().all { (a, b) -> a.average != null && b.average != null && a.average <= b.average }
        if (latest.effort == "hard" || latest.belowMin) {
            if (declining && !body && base > 0) return answer("reduce", "declining", nextWeight(-1))
            return answer("hold", "hold")
        }
        val stable = trend.size == 3 && same && trend.all { it.hitTop && it.safe && !it.adverse }
        if (body && stable) {
            val id = library.getString("id")
            val variation = when {
                id in setOf("pushup", "modified-pushup", "incline-pushup") -> Variation("decline-pushup", "", 1)
                id == "decline-pushup" -> Variation(id, "paused", 2)
                id in setOf("plank", "side-plank") && prescription.optString("variation").isBlank() -> Variation(id, "longLever", 1)
                else -> Variation(id, "weighted", 2)
            }
            return answer("variation", "variation", 0.0, variation = variation)
        }
        if (!body && stable) return answer("increase", "stable", nextWeight(1), auto = true)
        // The underlying tp140 recommendation uses the first three occurrences, never unrelated programs.
        if (body) {
            if (target.kind == "rir") return if (latest.effort in setOf("easy", "light")) answer("reps", "rir_reps", 0.0)
                else answer("hold", "rir", 0.0)
            val twiceTop = trend.filter { abs(it.weight - base) < .01 }.count { it.hitTop && it.safe } >= 2
            if (twiceTop || latest.hitTop) return answer("hold", "repeat_top", 0.0)
            if (target.kind in setOf("reps", "time") && latest.reps.isNotEmpty()) return answer("reps", "reps", 0.0, next = nextReps(latest.reps, target))
            return answer("hold", "hold", 0.0)
        }
        if (target.kind == "rir") return answer("hold", "rir")
        if (target.kind !in setOf("reps", "time")) return answer("hold", "open")
        val sameLoad = trend.filter { abs(it.weight - base) < .01 }
        if (latest.hitTop && latest.safe && sameLoad.take(2).none { it.adverse }) return answer("increase", "candidate", nextWeight(1))
        if (latest.reps.isNotEmpty()) return answer("reps", "reps", next = nextReps(latest.reps, target))
        return answer("hold", "hold")
    }
    /** Final Coach action can limit the recommendation without changing the underlying policy or workout. */
    fun forCoach(advice: Advice, lastEffort: String?, readiness: NativeCoachReadiness.Result): Advice {
        if (advice.reason == "distance") return advice.copy(autoApply = false)
        if (advice.action == "pain" || lastEffort == "pain") return advice.copy(action = "pain", reason = "pain", autoApply = false)
        if (readiness.mode == "light") return advice.copy(action = "easy", reason = "recovery", autoApply = false)
        if (readiness.mode == "reduced") {
            if (lastEffort in setOf("hard", "challenging") || advice.action == "increase")
                return advice.copy(action = "hold", reason = "reduced", autoApply = false)
            if (advice.action !in setOf("increase", "reps", "hold", "start")) return advice.copy(action = "unknown", autoApply = false)
        }
        return advice.copy(autoApply = false) // UI is advice, not permission to mutate an active/imported draft.
    }
    private fun nextReps(reps: List<Double>, target: Target): List<Double> {
        val top = target.max ?: return emptyList()
        val clamped = reps.map { it.coerceIn(target.min ?: 0.0, top) }
        val smallest = clamped.minOrNull() ?: return emptyList()
        return clamped.map { if (it == smallest) minOf(top, it + 1) else it }
    }
    internal fun number(row: JSONObject, field: String): Double? {
        if (!row.has(field)) return null
        val value = row.opt(field)
        return when { value == JSONObject.NULL -> 0.0; value is Number -> value.toDouble()
            value is String -> if (value.trim().isEmpty()) 0.0 else value.toDoubleOrNull(); else -> null }?.takeIf { it.isFinite() }
    }
}
