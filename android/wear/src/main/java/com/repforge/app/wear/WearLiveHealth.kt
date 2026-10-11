package com.repforge.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.util.UUID

/** One replaceable DataItem, separate from the durable workout command queue. */
internal object WearLiveHealth {
    const val PATH = "/trainpilot/watch-health"
    @Synchronized fun publish(context: Context, workoutId: String, state: String, message: String = "", summary: JSONObject? = null) {
        if(workoutId.isBlank())return
        val prefs=context.getSharedPreferences("trainpilot_wear_live_health",0)
        val watchId=prefs.getString("watchId",null) ?: UUID.randomUUID().toString()
        val revision=prefs.getLong("revision",0)+1
        prefs.edit().putString("watchId",watchId).putLong("revision",revision).commit()
        val value=JSONObject().put("schema",1).put("watchId",watchId).put("workoutId",workoutId)
            .put("revision",revision).put("state",state).put("message",message.take(240)).put("updatedAt",System.currentTimeMillis())
        summary?.let { value.put("summary",WearHealthAccumulator(it).snapshot()) }
        val map=PutDataMapRequest.create(PATH)
        map.dataMap.putString("health",value.toString())
        Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
    }
}
