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
    val completedSets: Int,
    val started: String = "",
    val needsFeedback: Boolean = false
)

object WearClosureStore {
    private const val PREFS = "trainpilot_wear_closure"

    @Synchronized fun persist(context: Context,workoutId: String): Boolean {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val raw=prefs.getString("result",null) ?: return false
        if(JSONObject(raw).optString("workoutId")!=workoutId)return false
        return prefs.edit().putString("result",raw).putString("closed",prefs.getString("closed","[]")).commit()
    }

    @Synchronized
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
            .put("started", workout.started)
            .put("needsFeedback", command.optString("action") == "finishWorkout" && !command.has("feedback"))
            .put("command", command)
        val beforeClosed=prefs.getString("closed","[]");val beforeResult=prefs.getString("result",null)
        if(!prefs.edit().putString("closed", closed.toString()).putString("result", result.toString()).commit()) {
            prefs.edit().putString("closed",beforeClosed).putString("result",beforeResult).commit()
            return parse(result.put("status","error").put("message","A helyi mentés nem sikerült."))
        }
        return parse(result)
    }

    fun isClosed(context: Context, workoutId: String): Boolean {
        val closed = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("closed", "[]"))
        return (0 until closed.length()).any { closed.optString(it) == workoutId }
    }

    @Synchronized
    fun acceptResult(context: Context, result: JSONObject?) {
        result ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return
        if (local.optString("commandId") != result.optString("commandId")) return
        local.put("status", result.optString("status", "pending"))
        local.put("message", result.optString("message"))
        if (result.optBoolean("feedbackAnswered")) local.put("needsFeedback", false)
        prefs.edit().putString("result", local.toString()).apply()
    }

    fun load(context: Context): WearClosure? {
        val local=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("result",null)?.let(::JSONObject) ?: return null
        // The outbox receipt and command removal commit atomically. Reconstruct
        // closure status from that proof after a crash between preference stores.
        val receipts=JSONArray(context.getSharedPreferences("trainpilot_wear_outbox",Context.MODE_PRIVATE).getString("results191","[]"))
        for(i in 0 until receipts.length())receipts.optJSONObject(i)?.let {
            if(it.optString("commandId")==local.optString("commandId") && it.optString("workoutId")==local.optString("workoutId") &&
                it.optString("status") in setOf("saved","discarded","error"))local.put("status",it.optString("status"))
        }
        return parse(local)
    }

    @Synchronized
    fun answer(context: Context, workoutId: String, feedback: JSONObject) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return
        if (local.optString("workoutId") != workoutId) return
        local.put("needsFeedback", false)
        // A later explicit retry of the terminal command carries the answer too.
        local.optJSONObject("command")?.put("feedback", feedback)
        prefs.edit().putString("result", local.toString()).commit()
    }

    @Synchronized
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

    @Synchronized
    fun dismiss(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("result").apply()
    }

    private fun parse(value: JSONObject) = WearClosure(
        commandId = value.optString("commandId"), workoutId = value.optString("workoutId"),
        action = value.optString("action"), status = value.optString("status", "pending"),
        message = value.optString("message"), durationSeconds = value.optInt("durationSeconds"),
        exerciseCount = value.optInt("exerciseCount"), completedSets = value.optInt("completedSets"),
        started = value.optString("started"), needsFeedback = value.optBoolean("needsFeedback")
    )
}
