package com.repforge.app.wear

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local closure survives offline delivery. "saved" only follows the phone's receipt. */
data class WearClosure(
    val commandId: String,
    val workoutId: String,
    val action: String,
    val status: String,
    val message: String,
    val durationSeconds: Int,
    val exerciseCount: Int,
    val completedSets: Int
)

object WearClosureStore {
    private const val PREFS = "trainpilot_wear_closure"

    fun close(context: Context, workout: WearWorkout, command: JSONObject): WearClosure {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val closed = JSONArray(prefs.getString("closed", "[]"))
        closed.put(workout.workoutId)
        while (closed.length() > 128) closed.remove(0)
        val result = JSONObject()
            .put("commandId", command.optString("commandId"))
            .put("workoutId", workout.workoutId)
            .put("action", command.optString("action"))
            .put("status", "pending")
            .put("durationSeconds", workout.elapsedSeconds())
            .put("exerciseCount", workout.exercises.count { ex -> ex.sets.any { it.done } })
            .put("completedSets", workout.exercises.sumOf { ex -> ex.sets.count { it.done } })
            .put("command", command)
        prefs.edit().putString("closed", closed.toString()).putString("result", result.toString()).commit()
        return parse(result)
    }

    fun isClosed(context: Context, workoutId: String): Boolean {
        val closed = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("closed", "[]"))
        return (0 until closed.length()).any { closed.optString(it) == workoutId }
    }

    fun acceptResult(context: Context, result: JSONObject?) {
        result ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return
        if (local.optString("commandId") != result.optString("commandId")) return
        local.put("status", result.optString("status", "pending"))
        local.put("message", result.optString("message"))
        prefs.edit().putString("result", local.toString()).apply()
    }

    fun load(context: Context): WearClosure? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("result", null)?.let { parse(JSONObject(it)) }

    fun retry(context: Context): JSONObject? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return null
        val command = local.optJSONObject("command") ?: return null
        val id = java.util.UUID.randomUUID().toString()
        command.put("commandId", id).put("createdAt", System.currentTimeMillis())
        local.put("commandId", id).put("status", "pending").put("message", "").put("command", command)
        prefs.edit().putString("result", local.toString()).commit()
        return command
    }

    fun dismiss(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("result").apply()
    }

    private fun parse(value: JSONObject) = WearClosure(
        commandId = value.optString("commandId"), workoutId = value.optString("workoutId"),
        action = value.optString("action"), status = value.optString("status", "pending"),
        message = value.optString("message"), durationSeconds = value.optInt("durationSeconds"),
        exerciseCount = value.optInt("exerciseCount"), completedSets = value.optInt("completedSets")
    )
}
