package com.repforge.app.wear

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.shared.GpsRoutePacket193
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

/** Real Android foreground service; callbacks are synthetic, not evidence of outdoor GNSS reception. */
class WearGpsRecorderPlatform193Test {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }
    private fun current(id: String): WearWorkout {
        val now=java.time.Instant.now().toString()
        return WorkoutSnapshotStore.parse("""{"workoutId":"$id","started":"$now","ownershipProtocol193":1,"timerOwner":"watch","activeIntervals":[{"start":"$now","end":null}],"exercises":[{"id":"running","measurementType":"distance","repUnit":"km","sets":[{"setId":"$id-set","done":false,"reps":"17","distanceMeters":200}]}]}""")!!
    }
    private fun waitFor(predicate: () -> Boolean) {
        val until=SystemClock.elapsedRealtime()+5000
        while(SystemClock.elapsedRealtime()<until) {
            var ready=false;instrumentation.runOnMainSync { ready=predicate() };if(ready)return
            SystemClock.sleep(50)
        }
        fail("GPS foreground service did not reach the expected state")
    }
    private fun live(): WearGpsRouteService193 {
        val field=WearGpsRouteService193::class.java.getDeclaredField("instance");field.isAccessible=true
        return field.get(null) as WearGpsRouteService193
    }
    private fun cleanup(id: String) {
        instrumentation.runOnMainSync {
            WearGpsRouteService193.freeze(context);WearGps193.arm(context,id,false)
            val key=GpsRoutePacket193.key(id,"$id-set");WearGps193.spool(context).revoke(key);WearCommandOutbox.dropGpsRoute193(context,key)
            WorkoutSnapshotStore.clear(context)
        }
    }
    @Test fun acceptedCallbacksStayPrivateAndPauseRejectsLateFixesWithoutCompletingTheSet() {
        val id="recorder193-"+java.util.UUID.randomUUID();val workout=current(id)
        shell("cmd location set-location-enabled true")
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        try {
            instrumentation.runOnMainSync {
                WearAutomaticService.stop(context);WearHealthStore.enable(context,false);WorkoutSnapshotStore.clear(context)
                WorkoutSnapshotStore.saveWorkout(context,workout);WearGps193.arm(context,id,true,"watch193","phone193")
                WearGpsRouteService193.start(context,workout,"watch193","phone193")
            }
            waitFor { WearGps193.status(context,id).optString("state")=="acquiring"&&WearGps193.spool(context).active().has("meta") }
            instrumentation.runOnMainSync {
                val service=live();val elapsed=SystemClock.elapsedRealtime();val wall=System.currentTimeMillis()
                for(i in 0..2)service.onLocationChanged(Location("gps").apply {
                    latitude=47+i*.00005;longitude=19.0;accuracy=5f
                    elapsedRealtimeNanos=(elapsed-8000+i*4000)*1000000;time=wall-8000+i*4000
                })
                assertEquals(3,WearGps193.spool(context).active().getJSONArray("points").length())
                assertEquals(3,WearGps193.status(context,id).getInt("receivedFixes"))
                assertEquals(3,WearGps193.summary(context,id).getInt("savedPoints"))
                service.onLocationChanged(Location("gps").apply {
                    latitude=47.0002;longitude=19.0;accuracy=5f
                    elapsedRealtimeNanos=(SystemClock.elapsedRealtime()-20000)*1000000;time=System.currentTimeMillis()-20000
                })
                assertEquals(4,WearGps193.status(context,id).getInt("receivedFixes"))
                assertEquals("stale",WearGps193.status(context,id).getString("lastRejection"))
                val health=WearHealthStore.begin(context,id);WearHealthStore.save(context,health)
                val report=WearHealthAccumulator(health).snapshot().getJSONObject("gpsRoute193")
                assertEquals(4,report.getInt("receivedFixes"));assertEquals(3,report.getInt("savedPoints"))
                assertFalse(report.toString().contains("latitude"))
                assertFalse(context.getSharedPreferences("trainpilot_wear_route193",Context.MODE_PRIVATE).all.toString().contains("latitude"))
                val saved=WorkoutSnapshotStore.load(context)!!
                assertFalse(saved.currentSet!!.done);assertEquals("17",saved.currentSet!!.reps);assertEquals(200.0,saved.currentSet!!.distanceMeters,0.0)
                WorkoutSnapshotStore.saveWorkout(context,saved.copy(workoutPaused=true))
                service.onLocationChanged(Location("gps").apply { latitude=48.0;longitude=20.0;accuracy=5f;elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos();time=System.currentTimeMillis() })
                assertFalse(WearGpsRouteService193.active(id));assertFalse(WearGps193.spool(context).active().has("meta"))
                assertEquals(4,WearGps193.status(context,id).getInt("receivedFixes"))
                val rows=JSONArray(context.getSharedPreferences("trainpilot_wear_outbox",0).getString("pending","[]"))
                val pointers=(0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.optString("workoutId")==id }
                assertEquals(1,pointers.size);assertFalse(pointers[0].toString().contains("latitude"))
                assertEquals(3,WearGps193.spool(context).wirePayload(pointers[0].getString("commandId"))!!.getJSONArray("points").length())
            }
        } finally { cleanup(id) }
    }
    @Test fun disabledSystemLocationStopsOnlyTheGpsServiceAndLeavesWorkoutValuesUntouched() {
        val id="location-off193-"+java.util.UUID.randomUUID();val workout=current(id)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_COARSE_LOCATION)
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.ACCESS_FINE_LOCATION)
        shell("cmd location set-location-enabled false")
        try {
            instrumentation.runOnMainSync {
                WorkoutSnapshotStore.clear(context);WorkoutSnapshotStore.saveWorkout(context,workout)
                WearGps193.arm(context,id,true,"watch193","phone193");WearGpsRouteService193.start(context,workout,"watch193","phone193")
            }
            waitFor { WearGps193.status(context,id).optString("state")=="locationOff" }
            instrumentation.runOnMainSync {
                val saved=WorkoutSnapshotStore.load(context)!!;assertFalse(saved.workoutPaused);assertFalse(saved.currentSet!!.done)
                assertEquals("17",saved.currentSet!!.reps);assertFalse(WearGpsRouteService193.active(id));assertFalse(WearGps193.spool(context).active().has("meta"))
            }
        } finally { cleanup(id);shell("cmd location set-location-enabled true") }
    }
}
