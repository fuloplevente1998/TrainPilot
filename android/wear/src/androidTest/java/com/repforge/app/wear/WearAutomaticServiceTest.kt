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
