package com.repforge.app.wear

import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.shared.GpsRoutePacket193
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearGpsRoutePlatform193Test {
    @Test fun coordinatesStayPrivateUntilWireSendAndOnlyTheMatchingPhoneReceiptErasesThem() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val id="route193-"+java.util.UUID.randomUUID().toString();val key=GpsRoutePacket193.key(id,"set193")
        val meta=JSONObject().put("routeKey",key).put("workoutId",id).put("exerciseId","run").put("setId","set193")
            .put("sourceNode","watch193").put("targetNode","phone193").put("ownerEpoch193",0)
        try { instrumentation.runOnMainSync {
            WearAutomaticService.stop(context);WorkoutSnapshotStore.clear(context)
            WearGps193.spool(context).begin(meta,WearGps193.queue(context))
            WearGps193.spool(context).append(fix(1),WearGps193.queue(context));WearGps193.spool(context).close(false,WearGps193.queue(context))
            val prefs=context.getSharedPreferences("trainpilot_wear_outbox",0)
            val rows=JSONArray(prefs.getString("pending","[]"));val pointer=(0 until rows.length()).map { rows.getJSONObject(it) }.single { it.optString("workoutId")==id }
            assertFalse(pointer.toString().contains("latitude"));assertFalse(pointer.has("gpsRoutePacket193"))
            val wire=JSONObject(pointer.toString());WearGps193.attach(context,wire)
            assertEquals(1,wire.getJSONObject("gpsRoutePacket193").getJSONArray("points").length());assertFalse(pointer.has("gpsRoutePacket193"))
            val receipt=JSONObject().put("schema",1).put("commandId",pointer.getString("commandId")).put("action",WearGps193.ACTION).put("workoutId",id)
                .put("sequence",pointer.getLong("sequence")).put("status","applied").put("receivedAt",System.currentTimeMillis()).put("route193",JSONObject(pointer.getJSONObject("route193").toString()))
            val path=WearCommandOutbox.RECEIPT_PREFIX+pointer.getString("commandId")
            assertFalse(WearCommandOutbox.acceptReceipt(context,path,receipt.toString(),"anotherPhone"));assertNotNull(WearGps193.spool(context).payload(pointer.getString("commandId")))
            assertTrue(WearCommandOutbox.acceptReceipt(context,path,receipt.toString(),"phone193"));assertNull(WearGps193.spool(context).payload(pointer.getString("commandId")))
            assertTrue(WearCommandOutbox.acceptReceipt(context,path,receipt.toString(),"phone193"));assertNull(WorkoutSnapshotStore.load(context))
            assertFalse(prefs.getAll().toString().contains("latitude"))
        }} finally { instrumentation.runOnMainSync { WearGps193.spool(context).revoke(key);WearCommandOutbox.dropGpsRoute193(context,key) } }
    }
    @Test fun confirmedPhoneDeletionPurgesPendingRoutePacketsWithoutTouchingOtherWorkoutCommands() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val id="delete-route193-"+java.util.UUID.randomUUID().toString();val key=GpsRoutePacket193.key(id,"set193")
        val meta=JSONObject().put("routeKey",key).put("workoutId",id).put("exerciseId","run").put("setId","set193")
            .put("sourceNode","watch193").put("targetNode","phone193").put("ownerEpoch193",0)
        try { instrumentation.runOnMainSync {
            WearGps193.spool(context).begin(meta,WearGps193.queue(context));for(i in 1..33)WearGps193.spool(context).append(fix(i),WearGps193.queue(context))
            WearGps193.spool(context).close(false,WearGps193.queue(context))
            val prefs=context.getSharedPreferences("trainpilot_wear_outbox",0);val rows=JSONArray(prefs.getString("pending","[]"))
            val owned=(0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.optString("workoutId")==id };assertEquals(2,owned.size)
            val unrelated=(0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.optString("workoutId")!=id }.map { it.getString("commandId") }.toSet()
            val first=owned.first();val receipt=JSONObject().put("schema",1).put("commandId",first.getString("commandId")).put("workoutId",id).put("action",WearGps193.ACTION)
                .put("sequence",first.getLong("sequence")).put("status","applied").put("receivedAt",System.currentTimeMillis()).put("route193",first.getJSONObject("route193")).put("routeDeleted193",true)
            assertTrue(WearCommandOutbox.acceptReceipt(context,WearCommandOutbox.RECEIPT_PREFIX+first.getString("commandId"),receipt.toString(),"phone193"))
            assertTrue(WearGps193.spool(context).revoked(key));for(p in owned)assertNull(WearGps193.spool(context).payload(p.getString("commandId")))
            val after=JSONArray(prefs.getString("pending","[]"));assertEquals(unrelated,(0 until after.length()).map { after.getJSONObject(it).getString("commandId") }.toSet())
            try { WearGps193.spool(context).begin(meta,WearGps193.queue(context));fail("Deleted route must stay retired") }catch(_:IllegalStateException){}
        }} finally { instrumentation.runOnMainSync { WearGps193.spool(context).revoke(key);WearCommandOutbox.dropGpsRoute193(context,key) } }
    }
    private fun fix(i: Int)=JSONObject().put("latitude",47+i*.00005).put("longitude",19).put("accuracyMeters",5)
        .put("elapsedMillis",i*4000L).put("epochMillis",1800000000000L+i*4000L).put("segment",1)
}
