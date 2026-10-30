package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject

data class WatchWorkoutFeedback(val workoutId: String, val started: String) {
    companion object {
        fun parse(value: JSONObject?): WatchWorkoutFeedback? {
            value ?: return null
            val id = value.optString("workoutId")
            val started = value.optString("started")
            if (id.isBlank() || id.length > 96 || started.length > 40 ||
                runCatching { java.time.Instant.parse(started) }.isFailure) return null
            return WatchWorkoutFeedback(id, started)
        }
    }
}

object WearWorkoutFeedback {
    val choices = listOf(
        "easy" to "Túl könnyű", "light" to "Kicsit könnyű", "good" to "Pont jó",
        "challenging" to "Kicsit nehéz", "hard" to "Túl nehéz"
    )
    fun answer(effort: String?): JSONObject {
        require(effort == null || effort == "pain" || choices.any { it.first == effort })
        return if (effort == null) JSONObject().put("skipped", true)
        else JSONObject().put("effort", effort)
    }
    // The answer is already in the durable outbox. An older home DataItem must
    // not ask again while delivery to the phone is still pending.
    fun answered(context: Context, id: String): Boolean =
        context.getSharedPreferences("trainpilot_workout_feedback", Context.MODE_PRIVATE)
            .getString("answered", "") == id

    fun markAnswered(context: Context, id: String) {
        context.getSharedPreferences("trainpilot_workout_feedback", Context.MODE_PRIVATE)
            .edit().putString("answered", id).commit()
    }
}
