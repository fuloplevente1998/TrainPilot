package com.repforge.app.wear

import android.content.Context
import com.repforge.shared.GpsRouteLedger193
import com.repforge.shared.GpsRouteSpool193
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

/** The ordinary durable outbox carries references; only its Data Layer wire copy gets coordinates. */
internal object WearGps193 {
    const val ACTION="gpsRouteChunk193"
    private fun prefs(context: Context)=context.getSharedPreferences("trainpilot_wear_route193",0)
    fun distance(exercise: WearExercise?)=exercise!=null&&(exercise.measurementType=="distance"||exercise.repUnit.trim().lowercase() in listOf("km","m"))
    fun owns(workout: WearWorkout?): Boolean {
        if(workout==null||workout.ownershipProtocol193!=1||workout.timerOwner!="watch"||workout.workoutPaused||WearOwnership193.pending(workout))return false
        if(workout.ownerEpoch193==0L)return workout.handoff193==null
        return try { val h=JSONObject(workout.handoff193 ?: return false)
            h.optInt("protocol")==1&&h.optLong("epoch")==workout.ownerEpoch193&&h.optString("state")=="committed"&&h.optString("source")=="phone"&&h.optString("target")=="watch"&&
                com.repforge.shared.GpsRoutePacket193.id(h.optString("sourceNode"))&&com.repforge.shared.GpsRoutePacket193.id(h.optString("targetNode"))&&h.optString("sourceNode")!=h.optString("targetNode")
        }catch(_:Exception){false}
    }
    fun scope(context: Context)=try{JSONObject(prefs(context).getString("scope","{}")!!)}catch(_:Exception){JSONObject()}
    fun armed(context: Context,id: String)=scope(context).let { it.optString("workoutId")==id&&it.optBoolean("enabled") }
    fun arm(context: Context,id: String,enabled: Boolean,sourceNode: String?=null,targetNode: String?=null) {
        check(id.isNotBlank())
        val p=prefs(context);val old=p.getString("scope","{}");val next=if(scope(context).optString("workoutId")==id)scope(context)else JSONObject()
        next.put("workoutId",id).put("enabled",enabled)
        if(sourceNode!=null)next.put("sourceNode",sourceNode);if(targetNode!=null)next.put("targetNode",targetNode)
        if(!p.edit().putString("scope",next.toString()).commit()){p.edit().putString("scope",old).commit();error("GPS intent could not be saved")}
    }
    fun status(context: Context,id: String): JSONObject {
        val result=try{JSONObject(prefs(context).getString("status","{}")!!)}catch(_:Exception){JSONObject()}
        return (if(result.optString("workoutId")==id)result else JSONObject().put("workoutId",id).put("state","idle").put("points",0)).put("armed",armed(context,id)).put("running",WearGpsRouteService193.active(id))
    }
    fun update(context: Context,id: String,state: String,points: Int=0,accuracy: Double?=null) {
        try{val previous=status(context,id);val value=JSONObject().put("workoutId",id).put("state",state).put("points",points)
                .put("receivedFixes",previous.optInt("receivedFixes")).put("lastRejection",if(state=="recording")"" else previous.optString("lastRejection"))
            if(accuracy!=null)value.put("accuracyMeters",accuracy);val p=prefs(context);if(p.getString("status",null)!=value.toString())p.edit().putString("status",value.toString()).apply()
        }catch(_:Exception){/* Status is not the authoritative route store. */}
    }
    fun received(context: Context,id: String,rejection: String="") {
        val value=status(context,id)
        value.remove("armed");value.remove("running")
        value.put("receivedFixes",(value.optInt("receivedFixes")+1).coerceAtMost(10000000)).put("lastRejection",rejection)
        prefs(context).edit().putString("status",value.toString()).apply()
    }
    /** Counts and state only; coordinates never enter Health summaries or generic commands. */
    fun summary(context: Context,id: String): JSONObject? {
        val raw=try{JSONObject(prefs(context).getString("status","{}")!!)}catch(_:Exception){return null}
        if(raw.optString("workoutId")!=id)return null
        val value=status(context,id)
        return JSONObject().put("schema",1).put("state",value.optString("state","idle"))
            .put("receivedFixes",value.optInt("receivedFixes")).put("savedPoints",value.optInt("points"))
            .put("lastRejection",value.optString("lastRejection"))
    }
    fun resumeIfArmed(context: Context,workout: WearWorkout) {
        if(!armed(context,workout.workoutId))return
        try {
            val h=workout.handoff193?.let(::JSONObject);val saved=scope(context)
            val source=h?.takeIf { it.optString("target")=="watch"&&it.optString("state")=="committed" }?.optString("targetNode") ?: saved.optString("sourceNode")
            val target=h?.takeIf { it.optString("target")=="watch"&&it.optString("state")=="committed" }?.optString("sourceNode") ?: saved.optString("targetNode")
            WearGpsRouteService193.start(context,workout,source,target)
        }catch(_:SecurityException){update(context,workout.workoutId,"permission")}catch(_:Exception){update(context,workout.workoutId,"unavailable")}
    }
    fun discard(context: Context,workout: WearWorkout) {
        WearGpsRouteService193.freeze(context);arm(context,workout.workoutId,false)
        for(exercise in workout.exercises.filter(::distance))for(set in exercise.sets)if(set.setId.isNotBlank()) {
            val key=com.repforge.shared.GpsRoutePacket193.key(workout.workoutId,set.setId);spool(context).revoke(key);check(WearCommandOutbox.dropGpsRoute193(context,key))
        }
        cleanupPublished(context)
    }
    fun spool(context: Context)=GpsRouteSpool193.open(context.noBackupFilesDir)
    fun queue(context: Context)=GpsRouteSpool193.Queue { pointer -> WearCommandOutbox.enqueue(context,pointer)>0 }
    fun ready(context: Context,command: JSONObject)=spool(context).ready(command)
    fun attach(context: Context,command: JSONObject) {
        if(command.optString("action")!=ACTION)return
        val packet=spool(context).wirePayload(command.getString("commandId")) ?: error("Private GPS chunk unavailable")
        check(GpsRouteLedger193.fingerprint(packet)==command.getJSONObject("route193").getString("hash"))
        command.put("gpsRoutePacket193",packet)
    }
    fun receiptOrigin(context: Context,receipt: JSONObject,origin: String?): Boolean {
        if(receipt.optString("action")!=ACTION)return true
        if(origin.isNullOrBlank())return false
        return try {
            val packet=spool(context).wirePayload(receipt.getString("commandId"))
            val expected=packet?.let { GpsRouteSpool193.pointer(it).getJSONObject("route193") } ?: run {
                val results=JSONArray(context.getSharedPreferences("trainpilot_wear_outbox",0).getString("results191","[]"))
                (0 until results.length()).mapNotNull { results.optJSONObject(it) }.firstOrNull { it.optString("commandId")==receipt.optString("commandId") }?.optJSONObject("route193")
            } ?: return false
            origin==expected.getString("targetNode")&&samePointer(expected,receipt.optJSONObject("route193"))
        }catch(_:Exception){false}
    }
    fun samePointer(a: JSONObject?,b: JSONObject?)=a!=null&&b!=null&&listOf("routeKey","streamId","sequence","sourceNode","targetNode","hash")
        .all { a.opt(it)?.toString()==b.opt(it)?.toString() }
    fun receipt(context: Context,receipt: JSONObject) {
        if(receipt.optString("action")!=ACTION||receipt.optString("status")!="applied")return
        if(receipt.optBoolean("routeDeleted193")) {
            val key=receipt.getJSONObject("route193").getString("routeKey")
            spool(context).revoke(key);check(WearCommandOutbox.dropGpsRoute193(context,key))
            cleanupPublished(context)
        }else spool(context).acknowledged(receipt.getString("commandId"))
    }
    fun recover(context: Context,sealActive: Boolean=false) {
        try {
            val old=if(sealActive)spool(context).active()else null
            spool(context).recover(queue(context),sealActive)
            val id=old?.optJSONObject("meta")?.optString("workoutId")
            if(id!=null&&armed(context,id))update(context,id,"interrupted",status(context,id).optInt("points"))
            val rows=JSONArray(context.getSharedPreferences("trainpilot_wear_outbox",0).getString("results191","[]"))
            for(i in 0 until rows.length())rows.optJSONObject(i)?.let { receipt(context,it) }
            cleanupPublished(context)
        }catch(_:Exception){/* Real unacknowledged fixes remain private and retryable. */}
    }
    private fun cleanupPublished(context: Context) {
        Wearable.getDataClient(context).dataItems.addOnSuccessListener { items ->
            try { for(item in items) {
                if(item.uri.path?.startsWith(WearDataListenerService.COMMAND_PATH_PREFIX)!=true)continue
                try {
                    val raw=DataMapItem.fromDataItem(item).dataMap.getString("command") ?: continue
                    if(raw.length>16000)continue
                    val command=JSONObject(raw);if(command.optString("action")!=ACTION)continue
                    val key=command.optJSONObject("route193")?.optString("routeKey") ?: continue
                    if(spool(context).revoked(key))Wearable.getDataClient(context).deleteDataItems(item.uri)
                }catch(_:Exception){/* Only locally retired GPS routes may be erased. */}
            }}finally{items.release()}
        }
    }
}
