package com.repforge.app.wear

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Resume intent is durable even when the command queue cannot commit at the first attempt. */
class WearOwnershipResumePlatform193Test {
    @Test fun sourceFreezeUsesTheDurableValuesAndStopsManualTimeBeforeClosureOrTransfer() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val now=System.currentTimeMillis();val id="freeze193-"+java.util.UUID.randomUUID().toString()
        val old=WorkoutSnapshotStore.parse("""{"workoutId":"$id","started":"${java.time.Instant.ofEpochMilli(now-20000)}","ownershipProtocol193":1,"ownerEpoch193":1,"timerOwner":"watch","workoutPaused":false,"pauseRevision":4,"activeIntervals":[{"start":"${java.time.Instant.ofEpochMilli(now-20000)}","end":null}],"exercises":[{"id":"plank","repUnit":"mp","sets":[{"set":1,"setId":"done-$id","reps":"14","done":true},{"set":2,"setId":"current-$id","reps":"4","done":false}]}]}""")!!
        try {
            instrumentation.runOnMainSync {
                WearAutomaticService.stop(context);WearHealthStore.enable(context,false);WorkoutSnapshotStore.clear(context);WearStopwatchStore.clear(context)
                WorkoutSnapshotStore.saveWorkout(context,old.copy(exercises=listOf(old.exercises[0].copy(sets=listOf(old.exercises[0].sets[0],old.exercises[0].sets[1].copy(reps="7"))))))
                WearStopwatchStore.save(context,WearStopwatch("$id:plank:2:reps",7300,now-2000))
                val frozen=WearOwnership193.freeze(context,id)!!
                assertTrue(frozen.workoutPaused);assertEquals(1,frozen.completedSets);assertEquals("14",frozen.exercises[0].sets[0].reps)
                assertTrue(frozen.exercises[0].sets[1].reps.toInt()>=9);assertEquals("4",old.exercises[0].sets[1].reps)
                assertEquals(0L,WearStopwatchStore.load(context,"$id:plank:2:reps").startedAt)
                assertTrue(frozen.activeIntervals!!.all { it.end>0 });assertTrue(frozen.pauseRevision>old.pauseRevision)
                assertEquals(frozen.elapsedSeconds(),frozen.elapsedSeconds(now+600000))
                val repeated=WearOwnership193.freeze(context,id)!!
                assertEquals(frozen.pauseRevision,repeated.pauseRevision);assertEquals(frozen.activeIntervals,repeated.activeIntervals)
                assertNull(WearOwnership193.freeze(context,"another-workout"))
            }
        } finally { instrumentation.runOnMainSync { WorkoutSnapshotStore.clear(context);WearStopwatchStore.clear(context) } }
    }
    @Test fun failedOutboxWriteRetainsOneDurableResumeIntentAndRecoveryReusesItsIdentity() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val id="resume193-"+java.util.UUID.randomUUID().toString()
        val workout=WorkoutSnapshotStore.parse("""{"active":true,"workoutId":"$id","started":"2026-10-18T10:00:00Z","ownershipProtocol193":1,"ownerEpoch193":1,"timerOwner":"watch","workoutPaused":true,"pauseRevision":4,"handoff193":{"protocol":1,"id":"handoff-$id","state":"committed","epoch":1,"source":"phone","target":"watch"},"activeIntervals":[{"start":"2026-10-18T10:00:00Z","end":"2026-10-18T10:00:10Z"}],"exercises":[{"id":"press","sets":[{"setId":"set-$id","reps":"5","done":false}]}]}""")!!
        val real=context.getSharedPreferences("trainpilot_wear_outbox",0)
        val failing=java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader,arrayOf(SharedPreferences::class.java)) { _,method,args ->
            if(method.name!="edit")method.invoke(real,*(args ?: emptyArray())) else {
                val editor=real.edit()
                java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader,arrayOf(SharedPreferences.Editor::class.java)) { proxy,m,a ->
                    val result=m.invoke(editor,*(a ?: emptyArray()))
                    if(m.name=="commit")false else if(result===editor)proxy else result
                }
            }
        } as SharedPreferences
        val wrapper=object:ContextWrapper(context) {
            override fun getSharedPreferences(name: String,mode: Int):SharedPreferences = if(name=="trainpilot_wear_outbox")failing else super.getSharedPreferences(name,mode)
        }
        try {
            instrumentation.runOnMainSync {
                WearAutomaticService.stop(context);WearHealthStore.enable(context,false);WorkoutSnapshotStore.clear(context)
                WorkoutSnapshotStore.saveWorkout(context,workout);WearOwnership193.resume(wrapper)
            }
            val pending=WorkoutSnapshotStore.load(context)!!
            assertFalse(pending.workoutPaused);assertNotNull(pending.resumeIntent193)
            val intent=JSONObject(pending.resumeIntent193!!);val commandId=intent.getString("commandId")
            assertEquals(WearSavePhase.ERROR,WearCommandOutbox.state(context,id)!!.phase)
            // Parse/encode represents an app reload; the recovery key is in the actual snapshot file.
            assertEquals(commandId,JSONObject(WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(pending).toString())!!.resumeIntent193!!).getString("commandId"))
            instrumentation.runOnMainSync { WearOwnership193.recover(context) }
            val ready=WorkoutSnapshotStore.load(context)!!
            assertNull(ready.resumeIntent193);assertEquals(pending.pauseRevision,ready.pauseRevision)
            val rows=JSONArray(real.getString("pending","[]"));val command=(0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.single { it.optString("commandId")==commandId }
            assertEquals("resumeWorkout",command.getString("action"));assertEquals(1L,command.getLong("ownerEpoch193"))
            val sequence=command.getLong("sequence");assertTrue(sequence>0)
            instrumentation.runOnMainSync { WearOwnership193.recover(context) }
            val repeated=JSONArray(real.getString("pending","[]"));assertEquals(1,(0 until repeated.length()).count { repeated.optJSONObject(it)?.optString("commandId")==commandId })
            val receipt=JSONObject().put("schema",1).put("commandId",commandId).put("workoutId",id).put("action","resumeWorkout")
                .put("sequence",sequence).put("status","applied").put("receivedAt",System.currentTimeMillis())
            assertTrue(WearCommandOutbox.acceptReceipt(context,WearCommandOutbox.RECEIPT_PREFIX+commandId,receipt.toString()))
            assertEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        } finally { instrumentation.runOnMainSync { WorkoutSnapshotStore.clear(context) } }
    }
}
