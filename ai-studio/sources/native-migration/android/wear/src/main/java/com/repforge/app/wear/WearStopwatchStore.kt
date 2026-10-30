package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject

/** Wall-clock anchor keeps a running set timer alive when the activity is paused/killed. */
data class WearStopwatch(val key: String, val elapsedMillis: Long = 0L, val startedAt: Long = 0L) {
    fun elapsed(now: Long = System.currentTimeMillis()): Long = elapsedMillis +
        if (startedAt > 0) (now - startedAt).coerceAtLeast(0L) else 0L
    fun toggle(now: Long = System.currentTimeMillis()) = if (startedAt > 0)
        copy(elapsedMillis = elapsed(now), startedAt = 0L) else copy(startedAt = now)
}

object WearStopwatchStore {
    fun freezeIntoWorkout193(context: Context,workout: WearWorkout): WearWorkout {
        pause(context)
        val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("timer",null) ?: return workout
        val timer=JSONObject(raw);val key=timer.optString("key");val seconds=(timer.optLong("elapsed")/1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if(seconds<=0)return workout
        return workout.copy(exercises=workout.exercises.map { ex -> ex.copy(sets=ex.sets.map { set ->
            if(set.done)set else when(key) {
                "${workout.workoutId}:${ex.id}:${set.number}:reps" -> set.copy(reps=seconds.toString())
                "${workout.workoutId}:${ex.id}:${set.number}:leftSeconds" -> set.copy(leftSeconds=seconds,reps=if(set.rightSeconds>0)minOf(seconds,set.rightSeconds).toString()else set.reps)
                "${workout.workoutId}:${ex.id}:${set.number}:rightSeconds" -> set.copy(rightSeconds=seconds,reps=if(set.leftSeconds>0)minOf(seconds,set.leftSeconds).toString()else set.reps)
                else -> set
            }
        }) })
    }
    private const val PREFS = "trainpilot_wear_stopwatch"
    fun currentField(context: Context, prefix: String): String? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("timer", null) ?: return null
        val key = JSONObject(raw).optString("key")
        return if (key.startsWith(prefix + ":")) key.substringAfterLast(":") else null
    }
    fun load(context: Context, key: String): WearStopwatch {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("timer", null)
        val root = raw?.let(::JSONObject)
        return if (root?.optString("key") == key) WearStopwatch(key, root.optLong("elapsed"), root.optLong("started")) else WearStopwatch(key)
    }
    fun save(context: Context, timer: WearStopwatch): WearStopwatch {
        val value = JSONObject().put("key", timer.key).put("elapsed", timer.elapsedMillis).put("started", timer.startedAt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("timer", value.toString()).apply()
        return timer
    }
    fun pause(context: Context) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("timer", null) ?: return
        val root = JSONObject(raw)
        val timer = WearStopwatch(root.optString("key"), root.optLong("elapsed"), root.optLong("started"))
        if (timer.startedAt > 0) save(context, timer.toggle())
    }
    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("timer").apply()
}
