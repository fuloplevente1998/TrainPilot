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
            spool(context).recover(queue(context),sealActive)
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
