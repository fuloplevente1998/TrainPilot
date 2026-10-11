package com.repforge.app.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

internal fun wearHandoffReplyMatches(start: WearMeasurementStart?, expiresAt: Long, now: Long, data: JSONObject): Boolean =
    start!=null && expiresAt>=now && data.optBoolean("active") && start.matches(data.optString("workoutId"),data.optLong("revision"))

/** Visible phone-start handoff only; messages never start sensors from a background receiver. */
internal object WearWorkoutHandoff {
    const val REQUEST="/trainpilot/workout-request"
    const val REPLY="/trainpilot/workout-reply"
    fun request(context: Context, start: WearMeasurementStart) {
        val now=System.currentTimeMillis()
        context.getSharedPreferences("trainpilot_wear_handoff",0).edit()
            .putString("workoutId",start.workoutId).putLong("minimumRevision",start.minimumRevision).putLong("expiresAt",now+15000L).commit()
        val payload=JSONObject().put("workoutId",start.workoutId).put("minimumRevision",start.minimumRevision).put("sentAt",now).toString().toByteArray(Charsets.UTF_8)
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            for(node in nodes)Wearable.getMessageClient(context).sendMessage(node.id,REQUEST,payload)
        }
    }
    fun accept(context: Context, raw: String): Boolean {
        val prefs=context.getSharedPreferences("trainpilot_wear_handoff",0)
        if(System.currentTimeMillis()>prefs.getLong("expiresAt",0))return false
        val start=WearMeasurementStart.parse(prefs.getString("workoutId",null),prefs.getLong("minimumRevision",0).toString()) ?: return false
        val data=try { JSONObject(raw) }catch(_:Exception){return false}
        if(!wearHandoffReplyMatches(start,prefs.getLong("expiresAt",0),System.currentTimeMillis(),data))return false
        val workout=WorkoutSnapshotStore.save(context,raw)
        return start.matches(workout?.workoutId,workout?.revision ?: 0L)
    }
}
