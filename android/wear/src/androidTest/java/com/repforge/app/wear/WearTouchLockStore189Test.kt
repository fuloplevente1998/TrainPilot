package com.repforge.app.wear

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class WearTouchLockStore189Test {
    @Test fun durableLockNeverChangesWorkoutHealthOrOutboxAndDoesNotCarryToAnotherWorkout() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val exercise=WearExercise("press","Nyomás","weight","ism.","strength",listOf(WearSet(1,"5","5",false,0,0,0.0,"set")))
        val workout=WearWorkout(1,"p","Program","a","","2026-10-16T10:00:00Z","store-touch189",0,60,0,listOf(exercise))
        val prefs=listOf("trainpilot_wear_snapshot","trainpilot_wear_health","trainpilot_wear_outbox")
        val before=prefs.associateWith { context.getSharedPreferences(it,0).all.toMap() }
        try {
            assertTrue(WearTouchLock189.set(context,workout,true));assertTrue(WearTouchLock189.locked(context,workout.copy(revision=3)))
            assertFalse(WearTouchLock189.locked(context,workout.copy(workoutId="other")))
            assertFalse(WearTouchLock189.locked(context,workout.copy(started="2026-10-16T11:00:00Z")))
            WearTouchLock189.retain(context,workout.copy(workoutId="other"));assertFalse(WearTouchLock189.locked(context,workout))
            WearTouchLock189.set(context,workout,true);WearTouchLock189.retain(context,null);assertFalse(WearTouchLock189.locked(context,workout))
            assertEquals(before,prefs.associateWith { context.getSharedPreferences(it,0).all.toMap() })
        }finally{context.getSharedPreferences("trainpilot_wear_touch_lock189",0).edit().clear().commit()}
    }
}
