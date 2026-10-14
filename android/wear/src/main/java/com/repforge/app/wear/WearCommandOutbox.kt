package com.repforge.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Tracked commands stay here until a durable, identity-matched phone receipt. */
object WearCommandOutbox {
    private const val PREFS="trainpilot_wear_outbox"
    const val RECEIPT_PREFIX="/trainpilot/command-receipt/"
    private val sending=mutableMapOf<String,Long>()
    private val failedWrites=linkedMapOf<String,JSONObject>()
    private val writing=mutableSetOf<String>()
    private fun prefs(context: Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    private fun read(context: Context,key: String)=try { JSONArray(prefs(context).getString(key,"[]")) }catch(_:Exception){JSONArray()}

    fun enqueue(context: Context, command: JSONObject): Long {
        // Persist the draft before taking the outbox lock: workout editing takes
        // SnapshotStore -> outbox locks, so the reverse order would deadlock.
        val localReady=try {
            !WearSaveSyncModel.tracked(command) || command.optString("action") in setOf("healthSummary","saveWorkoutFeedback") ||
                WorkoutSnapshotStore.persist(context,command.optString("workoutId")) ||
                WearClosureStore.persist(context,command.optString("workoutId"))
        } catch(_:Exception) { false }
        return synchronized(this) { enqueuePersisted(context,command,localReady) }
    }

    private fun enqueuePersisted(context: Context, command: JSONObject,localReady: Boolean): Long {
        val p=prefs(context);val old=read(context,"pending");val before=old.toString();val oldSequence=p.getLong("sequence",0)
        val id=command.optString("commandId");val workoutId=command.optString("workoutId")
        val existing=(0 until old.length()).mapNotNull { old.optJSONObject(it) }.firstOrNull { it.optString("commandId")==id }
        val floor=maxOf(command.optLong("watchSequence"),command.optJSONObject("finalSnapshot")?.optLong("localSequence") ?: 0L)
        val sequence=existing?.optLong("sequence") ?: (maxOf(oldSequence,floor)+1)
        command.put("sequence",sequence)
        val tracked=WearSaveSyncModel.tracked(command)
        if(tracked)writing.add(workoutId)
        try {
            if(!localReady)throw IllegalStateException("local")
            if(existing==null) {
                if(old.length()>=512)throw IllegalStateException("capacity")
                if(command.optString("action")=="watchAutomationState")for(i in old.length()-1 downTo 0)
                    if(old.optJSONObject(i)?.optString("action")=="watchAutomationState")old.remove(i)
                old.put(JSONObject(command.toString()))
            }
            if(!p.edit().putLong("sequence",maxOf(sequence,oldSequence)).putString("pending",old.toString()).commit()) {
                // SharedPreferences changes memory even when its disk commit fails.
                p.edit().putLong("sequence",oldSequence).putString("pending",before).commit()
                throw IllegalStateException("local")
            }
            failedWrites.remove(id)
        }catch(_:Exception) {
            if(tracked)failedWrites[id]=JSONObject(command.toString())
            return 0L
        }finally { writing.remove(workoutId) }
        flush(context)
        return sequence
    }

    @Synchronized fun state(context: Context,workoutId: String?,now: Long=System.currentTimeMillis()): WearSaveSyncState? {
        val id=workoutId?.takeIf { it.isNotBlank() } ?: latestWorkoutId(context) ?: return null
        if(id in writing)return WearSaveSyncState(id,WearSavePhase.SAVING)
        if(failedWrites.values.any { it.optString("workoutId")==id })return WearSaveSyncState(id,WearSavePhase.ERROR,retryable=true,reason="local")
        return WearSaveSyncModel.view(read(context,"pending"),read(context,"results191"),id,now)
    }

    private fun latestWorkoutId(context: Context): String? {
        val pending=read(context,"pending");val results=read(context,"results191")
        val all=(0 until pending.length()).mapNotNull { pending.optJSONObject(it) }.filter { WearSaveSyncModel.tracked(it) }+
            (0 until results.length()).mapNotNull { results.optJSONObject(it) }+failedWrites.values
        return all.maxByOrNull { it.optLong("createdAt",it.optLong("receivedAt")) }?.optString("workoutId")
    }

    fun retry(context: Context,workoutId: String) {
        val failures=synchronized(this) { failedWrites.values.filter { it.optString("workoutId")==workoutId }.map { JSONObject(it.toString()) } }
        for(command in failures)enqueue(context,command)
        synchronized(this) {
        val rows=read(context,"pending");val before=rows.toString()
        for(i in 0 until rows.length())rows.optJSONObject(i)?.takeIf { it.optString("workoutId")==workoutId }?.apply { remove("_sent191");remove("_error191");remove("_attempt191") }
        if(prefs(context).edit().putString("pending",rows.toString()).commit())flush(context,true,workoutId)
        else prefs(context).edit().putString("pending",before).commit()
        }
    }

    @Synchronized fun acceptReceipt(context: Context,path: String?,raw: String?): Boolean {
        if(raw==null||raw.length>4096||path==null||!path.startsWith(RECEIPT_PREFIX))return false
        val receipt=try { JSONObject(raw) }catch(_:Exception){return false}
        if(path!=RECEIPT_PREFIX+receipt.optString("commandId"))return false
        val pending=read(context,"pending");val results=read(context,"results191")
        val next=WearSaveSyncModel.accept(pending,results,receipt)
        if(next==null)return (0 until results.length()).mapNotNull { results.optJSONObject(it) }.any {
            WearSaveSyncModel.sameReceipt(it,receipt)
        }
        if(!prefs(context).edit().putString("pending",next.first.toString()).putString("results191",next.second.toString()).commit()) {
            prefs(context).edit().putString("pending",pending.toString()).putString("results191",results.toString()).commit()
            return false
        }
        return true
    }

    @Synchronized fun flush(context: Context,force: Boolean=false,workoutId: String?=null) {
        val pending=read(context,"pending");val now=System.currentTimeMillis()
        for(i in 0 until pending.length()) {
            val command=pending.optJSONObject(i) ?: continue;val id=command.optString("commandId")
            val attempt=android.os.SystemClock.elapsedRealtime()
            if(id.isBlank()||failedWrites.containsKey(id)||workoutId!=null&&command.optString("workoutId")!=workoutId)continue
            if(sending[id]?.let { attempt-it<30000 }==true)continue
            if(!force&&command.optLong("_attempt191",command.optLong("_sent191"))>0&&now-command.optLong("_attempt191",command.optLong("_sent191"))<60000)continue
            val path=if(command.optString("action")=="watchAutomationState")"/trainpilot/watch-automatic" else "${WearDataListenerService.COMMAND_PATH_PREFIX}$id"
            val payload=JSONObject(command.toString());payload.remove("_sent191");payload.remove("_error191");payload.remove("_attempt191")
            val map=PutDataMapRequest.create(path);map.dataMap.putString("command",payload.toString());map.dataMap.putLong("createdAt",command.optLong("createdAt"))
            map.dataMap.putLong("deliveryAttempt191",now)
            sending[id]=attempt
            try { Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
                .addOnSuccessListener { finishSend(context,id,true,attempt) }
                .addOnFailureListener { finishSend(context,id,false,attempt) }
            } catch(_:Exception) { finishSend(context,id,false,attempt) }
        }
    }

    @Synchronized private fun finishSend(context: Context,id: String,success: Boolean,attempt: Long) {
        if(sending[id]!=attempt)return
        sending.remove(id);val pending=read(context,"pending");val before=pending.toString();val next=JSONArray()
        for(i in 0 until pending.length()) {
            val row=pending.optJSONObject(i) ?: continue
            if(row.optString("commandId")==id) {
                row.put("_attempt191",System.currentTimeMillis())
                if(success&&!WearSaveSyncModel.tracked(row))continue
                if(success){if(row.optLong("_sent191")==0L)row.put("_sent191",System.currentTimeMillis());row.remove("_error191")}
                else row.put("_error191","transport")
            }
            next.put(row)
        }
        if(!prefs(context).edit().putString("pending",next.toString()).commit()) {
            prefs(context).edit().putString("pending",before).commit()
        }
    }
}
