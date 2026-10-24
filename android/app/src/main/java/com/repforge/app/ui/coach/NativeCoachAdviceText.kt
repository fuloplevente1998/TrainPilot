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
    fun reason(text: Resources, reason: String): String = text.getString(when (reason) {
        "start" -> R.string.coach_reason_start; "pain" -> R.string.coach_reason_pain
        "partial" -> R.string.coach_reason_partial; "recovery" -> R.string.coach_reason_recovery
        "reduced" -> R.string.coach_reason_reduced; "distance" -> R.string.coach_reason_distance
        "declining" -> R.string.coach_reason_declining; "stable" -> R.string.coach_reason_stable
        "candidate" -> R.string.coach_reason_candidate; "variation" -> R.string.coach_reason_variation
        "rir_reps" -> R.string.coach_reason_rir_reps; "rir" -> R.string.coach_reason_rir
        "repeat_top" -> R.string.coach_reason_repeat_top; "reps" -> R.string.coach_reason_reps
        "open" -> R.string.coach_reason_open; else -> R.string.coach_reason_hold
    })
    fun variation(text: Resources, variation: NativeCoachExerciseAdvice.Variation): String = text.getString(when {
        variation.name == "longLever" -> R.string.coach_variation_long_lever
        variation.name == "paused" -> R.string.coach_variation_paused
        variation.name == "weighted" -> R.string.coach_variation_weighted
        variation.exerciseId == "decline-pushup" -> R.string.coach_variation_decline_pushup
        else -> R.string.coach_reason_variation
    })
}
