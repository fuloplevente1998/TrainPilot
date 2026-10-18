package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject

enum class WearSavePhase { SAVING, LOCAL, WAITING, SYNCED, ERROR }
data class WearSaveSyncState(val workoutId: String, val phase: WearSavePhase, val pending: Int = 0,
    val retryable: Boolean = false, val reason: String = "")

/** Delivery metadata only: the existing outbox remains the sole command queue. */
object WearSaveSyncModel {
    val tracked=setOf("startWorkout","startQuickWorkout","addQuickExercise","updateSet","completeSet","undoSet",
        "finishWorkout","discardWorkout","saveWorkoutFeedback","healthSummary","selectExercise",
        "nextExercise","prevExercise","automaticControl","toggleAutomatic","skipRest","resumeWorkout","gpsRouteChunk193")
    fun tracked(command: JSONObject)=command.optString("action") in tracked &&
        command.optString("commandId").length in 1..128 && command.optString("workoutId").length in 1..(if(command.optString("action")=="gpsRouteChunk193")512 else 96)

    fun view(pending: JSONArray, results: JSONArray, workoutId: String, now: Long): WearSaveSyncState? {
        val rows=(0 until pending.length()).mapNotNull { pending.optJSONObject(it) }
            .filter { tracked(it)&&it.optString("workoutId")==workoutId }
        val receipts=(0 until results.length()).mapNotNull { results.optJSONObject(it) }.filter { it.optString("workoutId")==workoutId }
        val terminal=receipts.filter { it.optString("status") in setOf("saved","discarded") }.maxOfOrNull { it.optLong("sequence") } ?: 0L
        val rejected=receipts.filter { it.optLong("sequence")>terminal && it.optString("status") !in setOf("applied","saved","discarded") }.maxByOrNull { it.optLong("sequence") }
        val last=rejected ?: receipts.maxByOrNull { it.optLong("sequence") }
        if(rows.isNotEmpty()) {
            val failed=rows.any { it.optString("_error191").isNotBlank() }
            val sent=rows.all { it.optLong("_sent191")>0 }
            val expired=rows.any { it.optLong("_sent191")>0&&now-it.optLong("_sent191")>=60000 }
            return WearSaveSyncState(workoutId,if(failed||expired)WearSavePhase.ERROR else if(sent)WearSavePhase.WAITING else WearSavePhase.LOCAL,
                rows.size,true,if(failed)"transport" else if(expired)"timeout" else "")
        }
        return last?.let { WearSaveSyncState(workoutId,
            if(it.optString("status") in setOf("applied","saved","discarded"))WearSavePhase.SYNCED else WearSavePhase.ERROR,
            reason=it.optString("status")) }
    }

    fun sameReceipt(a: JSONObject,b: JSONObject)=listOf("schema","commandId","workoutId","action","sequence","status","receivedAt")
        .all { a.opt(it)?.toString()==b.opt(it)?.toString() } && (a.optString("action")!="gpsRouteChunk193"||WearGps193.samePointer(a.optJSONObject("route193"),b.optJSONObject("route193"))&&a.optBoolean("routeDeleted193")==b.optBoolean("routeDeleted193"))

    fun accept(pending: JSONArray, results: JSONArray, receipt: JSONObject): Pair<JSONArray,JSONArray>? {
        if(receipt.optInt("schema")!=1||receipt.optString("status") !in setOf("applied","saved","discarded","error","superseded","processed"))return null
        val id=receipt.optString("commandId")
        val command=(0 until pending.length()).mapNotNull { pending.optJSONObject(it) }.firstOrNull { it.optString("commandId")==id } ?: return null
        if(!tracked(command)||receipt.optString("workoutId")!=command.optString("workoutId")||
            receipt.optString("action")!=command.optString("action")||receipt.optLong("sequence")!=command.optLong("sequence")||receipt.optLong("sequence")<=0)return null
        if(command.optString("action")=="gpsRouteChunk193"&&(!WearGps193.samePointer(command.optJSONObject("route193"),receipt.optJSONObject("route193"))||receipt.optString("status")!="applied"))return null
        val remaining=JSONArray();for(i in 0 until pending.length())pending.optJSONObject(i)?.takeUnless { it.optString("commandId")==id }?.let { remaining.put(it) }
        // Retain bounded command receipts, including earlier rejected operations.
        // A later ordinary edit must not conceal a rejected earlier edit.
        val next=JSONArray(results.toString()).put(JSONObject(receipt.toString()))
        while(next.length()>256) {
            // Keep an unresolved rejection even after hundreds of successful
            // edits in the same workout. Resolved/older receipts may expire.
            val rows=(0 until next.length()).mapNotNull { next.optJSONObject(it) }
            val removable=rows.indexOfFirst { row ->
                row.optString("status") in setOf("applied","saved","discarded") || rows.any {
                    it.optString("workoutId")==row.optString("workoutId") && it.optLong("sequence")>row.optLong("sequence") &&
                        (it.optString("status") in setOf("saved","discarded") || it.optString("status") !in setOf("applied","saved","discarded"))
                }
            }
            next.remove(if(removable>=0)removable else 0)
        }
        return remaining to next
    }
}
