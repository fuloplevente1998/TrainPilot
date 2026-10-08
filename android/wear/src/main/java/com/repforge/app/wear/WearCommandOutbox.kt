package com.repforge.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Keep commands until Data Layer has durably accepted them, even across process death. */
object WearCommandOutbox {
    private const val PREFS = "trainpilot_wear_outbox"

    @Synchronized
    fun enqueue(context: Context, command: JSONObject): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val floor = maxOf(command.optLong("watchSequence"), command.optJSONObject("finalSnapshot")?.optLong("localSequence") ?: 0L)
        val sequence = maxOf(prefs.getLong("sequence", 0L), floor) + 1L
        command.put("sequence", sequence)
        val pending = JSONArray(prefs.getString("pending", "[]"))
        pending.put(command)
        prefs.edit().putLong("sequence", sequence).putString("pending", pending.toString()).commit()
        flush(context)
        return sequence
    }

    fun flush(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = JSONArray(prefs.getString("pending", "[]"))
        for (i in 0 until pending.length()) {
            val command = pending.optJSONObject(i) ?: continue
            val id = command.optString("commandId")
            val map = PutDataMapRequest.create("${WearDataListenerService.COMMAND_PATH_PREFIX}$id")
            map.dataMap.putString("command", command.toString())
            map.dataMap.putLong("createdAt", command.optLong("createdAt"))
            Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
                .addOnSuccessListener { remove(context, id) }
        }
    }

    @Synchronized
    private fun remove(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = JSONArray(prefs.getString("pending", "[]"))
        val remaining = JSONArray()
        for (i in 0 until pending.length()) {
            val command = pending.optJSONObject(i) ?: continue
            if (command.optString("commandId") != id) remaining.put(command)
        }
        prefs.edit().putString("pending", remaining.toString()).apply()
    }
}
