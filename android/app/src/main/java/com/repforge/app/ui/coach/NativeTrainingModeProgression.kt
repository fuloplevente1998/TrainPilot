package com.repforge.app.ui.coach

import com.repforge.app.data.store.NativeTrainingModes
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.round

/**
 * Explicit #197 mode proposals. No automatic writes. The existing
 * NativeCoachExerciseAdvice.recommend remains the sole legacy policy.
 */
internal object NativeTrainingModeProgression {
    data class Preview(
        val action: String, val reason: String, val baselineWeight: Double,
        val proposedWeight: Double, val workingSets: Int, val nextReps: List<Double>,
        val historyCount: Int, val mode: String, val trainingStyle: String,
        val previewOnly: Boolean = true, val autoApply: Boolean = false
    )

    internal fun observedRir(ex: JSONObject): Double? {
        fun parsed(value: Any?): Double? = when (value) {
            is Number -> value.toDouble().takeIf { it.isFinite() && it in 0.0..10.0 }
            is String -> value.trim().takeIf { it.matches(Regex("^(?:10|[0-9])$")) }?.toDouble()
            else -> null
        }
        val sets = ex.optJSONArray("sets")
        val values = (0 until (sets?.length() ?: 0)).mapNotNull { sets?.optJSONObject(it) }
            .filter { it.optBoolean("done") }.mapNotNull { parsed(it.opt("actualRir")) }
        if (values.isNotEmpty()) return values.minOrNull()
        if (ex.optBoolean("setRir229") || (0 until (sets?.length() ?: 0))
            .any { index -> sets?.optJSONObject(index)?.has("actualRir") == true }) return null
        return parsed(ex.opt("actualRir")) ?: parsed(ex.optJSONObject("feedback")?.opt("rir"))
    }

    fun compute(mode: NativeTrainingModes.Effective, lib: JSONObject,
        target: NativeCoachExerciseAdvice.Target, recent: List<NativeCoachExerciseAdvice.Occurrence>,
        readiness: NativeCoachReadiness.Result, step: Double): Preview? {
        if (!mode.explicitChoice || mode.source !in setOf("global", "program")) return null
        val rows = recent.take(4)
        val latest = rows.firstOrNull()
        val base = latest?.weight ?: 0.0
        val body = lib.optString("loadType") == "bodyweight"
        val sets = if (mode.trainingStyle == "hit") minOf(target.sets, if (lib.optString("id") in
            setOf("lateral-raise", "db-reverse-fly", "face-pull", "db-curl", "hammer-curl",
                "cable-curl", "oh-triceps", "cable-triceps", "leg-extension", "leg-curl", "calf-raise"))
            1 else 2) else target.sets
        fun result(action: String, reason: String, weight: Double = base,
            reps: List<Double> = emptyList()) = Preview(action, reason, base, weight,
                sets.coerceAtLeast(0), reps, rows.size, mode.progressionMode, mode.trainingStyle)
        if (lib.optString("measurementType") in setOf("distance", "time", "gps") ||
            lib.optString("repUnit").trim().lowercase() in setOf("km", "m", "mp", "sec", "second", "s") ||
            target.kind != "reps") return result("hold", "non-strength-activity")
        val lo = target.min
        val hi = target.max
        if (target.sets <= 0 || lo == null || hi == null || lo <= 0 || hi < lo)
            return result("hold", "invalid-target")
        if (latest == null) return result("hold", "insufficient-history")
        fun hasPain(item: NativeCoachExerciseAdvice.Occurrence) =
            item.effort == "pain" || item.workout.optJSONObject("feedback")?.optString("rating") == "pain"
        if (readiness.pain || rows.take(3).any(::hasPain)) return result("hold", "pain")
        if (readiness.mode == "light" || readiness.mode == "reduced") return result("hold", "low-readiness")
        if (!latest.allDone) return result("hold", "incomplete")
        if (latest.adverse || latest.workout.optJSONObject("feedback")?.optString("rating") == "hard")
            return result("hold", "high-effort")
        if (latest.belowMin) return result("hold", "below-target")
        fun safe(item: NativeCoachExerciseAdvice.Occurrence) =
            item.allDone && item.safe && !item.adverse && !hasPain(item) && !item.belowMin &&
                item.workout.optJSONObject("feedback")?.optString("rating") !in setOf("hard", "pain")
        fun same(item: NativeCoachExerciseAdvice.Occurrence) = body || base > 0 && abs(item.weight - base) < .01
        fun streak(count: Int, predicate: (NativeCoachExerciseAdvice.Occurrence) -> Boolean): Boolean =
            rows.size >= count && rows.take(count).all { safe(it) && same(it) && predicate(it) }
        fun increase(reason: String): Preview {
            if (body) return result("variation", reason, 0.0)
            if (base <= 0 || !step.isFinite() || step !in .25..10.0) return result("hold", "missing-load-step")
            val next = round((base + step) * 4) / 4.0
            return if (next > base && next - base <= 10.0) result("increase", reason, next)
                else result("hold", "missing-load-step")
        }
        when (mode.progressionMode) {
            "double" -> {
                val low = lo ?: return result("hold", "invalid-target")
                val high = hi ?: return result("hold", "invalid-target")
                if (streak(3) { it.hitTop && it.reps.isNotEmpty() && it.reps.min() >= high })
                    return increase("double-stable-top")
                if (latest.hitTop) return result("hold", "double-await-stability")
                if (!safe(latest)) return result("hold", "uncertain-effort")
                if (latest.reps.isEmpty()) return result("hold", "missing-repetitions")
                val out = latest.reps.map { it.coerceIn(low, high) }.toMutableList()
                val least = out.minOrNull() ?: return result("hold", "missing-repetitions")
                if (least >= high) return result("hold", "double-await-stability")
                out[out.indexOf(least)] = (least + 1).coerceAtMost(high)
                return result("reps", "double-next-repetition", reps = out)
            }
            "linear" -> {
                val low = lo ?: return result("hold", "invalid-target")
                return if (streak(2) { it.reps.isNotEmpty() && it.reps.min() >= low })
                    increase("linear-stable-performance") else result("hold", "linear-await-stability")
            }
            "rir" -> {
                val rirLow = target.rirMin
                val rirHigh = target.rirMax
                if (rirLow == null || rirHigh == null || rirLow < 0 || rirHigh > 10 || rirLow > rirHigh)
                    return result("hold", "rir-target-missing")
                val actual = observedRir(latest.exercise) ?: return result("hold", "rir-feedback-missing")
                if (actual < rirLow) return result("hold", "rir-overexertion")
                if (streak(2) {
                    val observed = observedRir(it.exercise)
                    observed != null && observed > rirHigh + 1 &&
                        it.reps.isNotEmpty() && it.reps.all { reps ->
                            reps > 0 && (lo == null || reps >= lo)
                        }
                }) return increase("rir-stable-reserve")
                return result("hold", if (actual > rirHigh) "rir-await-stability" else "rir-on-target")
            }
            else -> return result("hold", "invalid-mode")
        }
    }
}
