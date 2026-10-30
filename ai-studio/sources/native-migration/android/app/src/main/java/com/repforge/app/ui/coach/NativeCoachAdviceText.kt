package com.repforge.app.ui.coach

import android.content.res.Resources
import com.repforge.app.R

internal object NativeCoachAdviceText {
    fun label(text: Resources, action: String): String = text.getString(when (action) {
        "pain" -> R.string.coach_action_pain; "easy" -> R.string.coach_action_easy
        "increase" -> R.string.coach_action_increase; "reduce" -> R.string.coach_action_reduce
        "variation" -> R.string.coach_action_variation; "reps" -> R.string.coach_action_reps
        "start" -> R.string.coach_action_start; "hold" -> R.string.coach_action_hold
        else -> R.string.coach_action_unknown
    })
    fun reason(text: Resources, reason: String): String {
        if (reason.startsWith("mode197:")) {
            val parts = reason.split(":", limit = 3)
            val description = text.getString(when (parts.getOrNull(2)) {
            "double-stable-top" -> R.string.native_modes_reason_double_stable_top
            "linear-stable-performance" -> R.string.native_modes_reason_linear_stable_performance
            "rir-stable-reserve" -> R.string.native_modes_reason_rir_stable_reserve
            "double-next-repetition" -> R.string.native_modes_reason_double_next_repetition
            "non-strength-activity" -> R.string.native_modes_reason_non_strength_activity
            "pain" -> R.string.native_modes_reason_pain
            "low-readiness" -> R.string.native_modes_reason_low_readiness
            "high-effort" -> R.string.native_modes_reason_high_effort
            "incomplete" -> R.string.native_modes_reason_incomplete
            "below-target" -> R.string.native_modes_reason_below_target
            "double-await-stability" -> R.string.native_modes_reason_double_await_stability
            "linear-await-stability" -> R.string.native_modes_reason_linear_await_stability
            "rir-target-missing" -> R.string.native_modes_reason_rir_target_missing
            "rir-feedback-missing" -> R.string.native_modes_reason_rir_feedback_missing
            "rir-overexertion" -> R.string.native_modes_reason_rir_overexertion
            "rir-await-stability" -> R.string.native_modes_reason_rir_await_stability
            "rir-on-target" -> R.string.native_modes_reason_rir_on_target
            "insufficient-history" -> R.string.native_modes_reason_insufficient_history
            "invalid-target" -> R.string.native_modes_reason_invalid_target
            "uncertain-effort" -> R.string.native_modes_reason_uncertain_effort
            "missing-repetitions" -> R.string.native_modes_reason_missing_repetitions
            "missing-load-step" -> R.string.native_modes_reason_missing_load_step
            "invalid-mode" -> R.string.native_modes_reason_invalid_mode
                else -> R.string.coach_reason_hold
            })
            return if (parts.getOrNull(1) == "hit")
                text.getString(R.string.native_modes_reason_hit) + " " + description else description
        }
        return text.getString(when (reason) {
        "start" -> R.string.coach_reason_start; "pain" -> R.string.coach_reason_pain
        "partial" -> R.string.coach_reason_partial; "recovery" -> R.string.coach_reason_recovery
        "reduced" -> R.string.coach_reason_reduced; "distance" -> R.string.coach_reason_distance
        "declining" -> R.string.coach_reason_declining; "stable" -> R.string.coach_reason_stable
        "candidate" -> R.string.coach_reason_candidate; "variation" -> R.string.coach_reason_variation
        "rir_reps" -> R.string.coach_reason_rir_reps; "rir" -> R.string.coach_reason_rir
        "repeat_top" -> R.string.coach_reason_repeat_top; "reps" -> R.string.coach_reason_reps
        "open" -> R.string.coach_reason_open; else -> R.string.coach_reason_hold
    })
    }
    fun variation(text: Resources, variation: NativeCoachExerciseAdvice.Variation): String = text.getString(when {
        variation.name == "longLever" -> R.string.coach_variation_long_lever
        variation.name == "paused" -> R.string.coach_variation_paused
        variation.name == "weighted" -> R.string.coach_variation_weighted
        variation.exerciseId == "decline-pushup" -> R.string.coach_variation_decline_pushup
        else -> R.string.coach_reason_variation
    })
}
