package com.repforge.app.wear

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real foreground timer with Activity stopped, no phone and no health permission. */
@RunWith(AndroidJUnit4::class)
class WearAutomaticServiceTest {
    @Test fun offlineUndoAndReceiptRecoveryCannotReviveAnAcknowledgedCommand() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        instrumentation.runOnMainSync {
            WearAutomaticService.stop(context);WorkoutSnapshotStore.clear(context)
            val delivery=context.getSharedPreferences("trainpilot_wear_outbox",android.content.Context.MODE_PRIVATE)
            delivery.edit().clear().commit()
            val before=WearWorkout(1,"p","Program","a","",java.time.Instant.now().toString(),"offline-undo188",0,30,0,
                listOf(WearExercise("press","Nyomás","weight","ism.","strength",listOf(WearSet(1,"5","5.5",false,0,0,0.0,"set188")))),timerOwner="watch")
            val after=before.copy(exercises=listOf(before.exercises[0].copy(sets=listOf(before.exercises[0].sets[0].copy(done=true)))))
            WorkoutSnapshotStore.saveWorkout(context,WearUndo188.capture(before,after,0,0,"record188",System.currentTimeMillis()))
            val measured=context.getSharedPreferences("trainpilot_wear_health",android.content.Context.MODE_PRIVATE).all.toMap()
            val undone=WearUndo188.undo(context,"record188")!!
            assertFalse(undone.exercises[0].sets[0].done)
            assertEquals("pending",WearUndo188.marker(undone)!!.getString("status"))
            val command=org.json.JSONArray(delivery.getString("pending","[]")).getJSONObject(0)
            assertEquals("undoSet",command.getString("action"))
            val receipt=JSONObject().put("schema",1).put("commandId",command.getString("commandId"))
                .put("workoutId",before.workoutId).put("action","undoSet").put("sequence",command.getLong("sequence"))
                .put("status","applied").put("receivedAt",System.currentTimeMillis())
            assertTrue(WearCommandOutbox.acceptReceipt(context,WearCommandOutbox.RECEIPT_PREFIX+command.getString("commandId"),receipt.toString()))
            assertEquals(command.getLong("sequence"),WearCommandOutbox.enqueue(context,command))
            assertEquals(0,org.json.JSONArray(delivery.getString("pending","[]")).length())
            // Simulate death after receipt storage but before snapshot reconciliation.
            WorkoutSnapshotStore.saveWorkout(context,undone)
            WearUndo188.recover(context)
            assertEquals("applied",WearUndo188.marker(WorkoutSnapshotStore.load(context)!!)!!.getString("status"))
            assertEquals(0,org.json.JSONArray(delivery.getString("pending","[]")).length())
            assertFalse(WorkoutSnapshotStore.load(context)!!.exercises[0].sets[0].done)
            assertEquals(measured,context.getSharedPreferences("trainpilot_wear_health",android.content.Context.MODE_PRIVATE).all.toMap())
            WorkoutSnapshotStore.clear(context);delivery.edit().clear().commit()
        }
    }
    @Test fun backgroundWorkAndRestPauseResumeRecordEachSetOnce() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        fun main(action: ()->Unit)=instrumentation.runOnMainSync(action)
        fun await(predicate: (WearWorkout)->Boolean): WearWorkout {
            val deadline=android.os.SystemClock.elapsedRealtime()+10000
            while(android.os.SystemClock.elapsedRealtime()<deadline){
                val current=WorkoutSnapshotStore.load(context)
                if(current!=null&&predicate(current))return current
                Thread.sleep(100)
            }
            throw AssertionError("Timed out: ${WorkoutSnapshotStore.load(context)?.automation}")
        }
        main {
            WearAutomaticService.stop(context);WorkoutSnapshotStore.clear(context)
            WearHealthStore.enable(context,false)
            WatchHomeStore.save(context,JSONObject().put("automaticProtocol",1).put("publishedAt",System.currentTimeMillis()).toString())
            WorkoutSnapshotStore.saveWorkout(context,WearWorkout(1,"p","Program","a","",
                java.time.Instant.now().toString(),"background-auto-test",0,3,0,
                listOf(WearExercise("plank","Plank","bodyweight","mp","time",listOf(
                    WearSet(1,"","0",false,0,0,0.0,"one"),WearSet(2,"","0",false,0,0,0.0,"two")))),
                timerOwner="watch",automation=WearAutomatic(workSeconds=2,restSeconds=3,vibration=false)))
        }
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity {
                WearAutomaticController.toggle(it,true)
                WearAutomaticController.command(it,"start","background-auto-test")
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            val rest=await { it.completedSets==1&&it.automation?.phase=="rest" }
            assertEquals("2",rest.exercises[0].sets[0].reps)
            main { WearAutomaticController.command(context,"pause","background-auto-test") }
            val paused=WorkoutSnapshotStore.load(context)!!
            assertTrue(paused.automation!!.paused)
            Thread.sleep(1200)
            assertEquals(paused.automation.remainingMs,WorkoutSnapshotStore.load(context)!!.automation!!.remainingMs)
            assertEquals(1,WorkoutSnapshotStore.load(context)!!.completedSets)
            main { WearAutomaticController.command(context,"resume","background-auto-test") }
            val complete=await { it.completedSets==2&&it.automation?.phase=="complete" }
            assertEquals(listOf("2","2"),complete.exercises[0].sets.map { it.reps })
            assertEquals(0,complete.automation!!.remainingMs)
            Thread.sleep(400)
            assertEquals(2,WorkoutSnapshotStore.load(context)!!.completedSets)
            assertFalse(WearHealthStore.enabled(context))
        } finally {
            main { WearAutomaticService.stop(context) }
            scenario.close()
            main { WorkoutSnapshotStore.clear(context) }
        }
    }
}
