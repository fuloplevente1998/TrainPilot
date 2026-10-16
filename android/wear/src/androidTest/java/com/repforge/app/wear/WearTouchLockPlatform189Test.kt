package com.repforge.app.wear

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Real Android input and uptime, independent of the Compose test's synthetic event clock. */
class WearTouchLockPlatform189Test {
    @Test fun defaultClockUnlocksOnlyAfterTapThenRealThreeSecondHoldAndRelease() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val exercise=WearExercise("press","Nyomás","weight","ism.","strength",listOf(WearSet(1,"5","5",false,0,0,0.0,"platform-set189")))
        val workout=WearWorkout(1,"p","Program","a","",java.time.Instant.now().toString(),"platform-touch189",0,60,0,listOf(exercise))
        instrumentation.runOnMainSync {
            WearAutomaticService.stop(context);WearHealthStore.enable(context,false)
            WorkoutSnapshotStore.saveWorkout(context,workout);assertTrue(WearTouchLock189.set(context,workout,true))
        }
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.recreate();instrumentation.waitForIdleSync()
            var x=0f;var y=0f
            scenario.onActivity { activity ->
                assertTrue(WearTouchLock189.locked(activity,WorkoutSnapshotStore.load(activity)))
                val content=activity.findViewById<View>(android.R.id.content);val location=IntArray(2);content.getLocationOnScreen(location)
                val density=activity.resources.displayMetrics.density
                val layout=wearFixedPageLayout(content.width/density,content.height/density,false,1)
                x=location[0]+content.width/2f;y=location[1]+(layout.footerTop+24f)*density
            }
            fun press(duration: Long) {
                val down=SystemClock.uptimeMillis()
                fun event(action: Int) {
                    val motion=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0)
                    try{motion.source=InputDevice.SOURCE_TOUCHSCREEN;instrumentation.sendPointerSync(motion)}finally{motion.recycle()}
                }
                event(MotionEvent.ACTION_DOWN);Thread.sleep(duration);event(MotionEvent.ACTION_UP);instrumentation.waitForIdleSync()
            }
            press(3100);assertTrue("Unarmed long touch stays locked",WearTouchLock189.locked(context,workout))
            press(80);Thread.sleep(100);press(3100)
            assertFalse("Real platform hold unlocks with the production clock",WearTouchLock189.locked(context,workout))
            assertFalse(WorkoutSnapshotStore.load(context)!!.exercises[0].sets[0].done)
        }finally{
            scenario.close();instrumentation.runOnMainSync { WearAutomaticService.stop(context);WorkoutSnapshotStore.clear(context);WearTouchLock189.retain(context,null) }
        }
    }
}
