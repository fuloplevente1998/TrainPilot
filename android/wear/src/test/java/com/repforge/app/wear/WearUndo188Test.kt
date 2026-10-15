package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearUndo188Test {
    private fun before()=WearWorkout(1,"p","Program","A","","2026-10-15T08:00:00Z","w",0,30,0,
        listOf(WearExercise("press","Nyomás","weight","ism.","strength",listOf(
            WearSet(1,"5","5.5",false,0,0,0.0,"s1"),WearSet(2,"","5.5",false,0,0,0.0,"s2")))),
        timerOwner="watch",automation=WearAutomatic(enabled=true,key="run",phase="ready",anchorMs=1000,boot=1,workMs=9000))
    private fun recorded(): WearWorkout {
        val b=before();val e=b.exercises[0]
        val after=b.copy(exercises=listOf(e.copy(sets=listOf(e.sets[0].copy(done=true),e.sets[1]))))
        return WearUndo188.capture(b,WearAutomaticEngine.afterRecord(after,1000,1),0,0,"record",2000)
    }
    @Test fun restUndoRestoresOnlyTargetAndPreservesActualTime() {
        val recorded=recorded();val next=WearUndo188.inverse(recorded,"undo",2000,1)!!
        assertFalse(next.exercises[0].sets[0].done);assertEquals("5",next.exercises[0].sets[0].reps)
        assertEquals(before().exercises[0].sets[1],next.exercises[0].sets[1]);assertEquals(0L,next.restEndAt)
        assertTrue(next.automation!!.paused);assertEquals("ready",next.automation.phase)
        assertEquals(recorded.automation!!.key,next.automation.key);assertEquals(9000L,next.automation.workMs);assertEquals(1000L,next.automation.restMs)
        assertNull(WearUndo188.inverse(next,"double-tap",2000,1))
        val resumed=WearAutomaticEngine.resume(next,3000,1);assertFalse(resumed.automation!!.paused)
        assertEquals(0,resumed.completedSets)
    }
    @Test fun expiryRemovesOnlyQuickActionAndMenuSurvivesNavigationAndRestart() {
        val w=recorded();assertTrue(WearUndo188.quick(w,9999));assertFalse(WearUndo188.quick(w,10001))
        val restored=WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(w).toString())!!
        assertTrue(WearUndo188.available(restored));assertEquals("record",WearUndo188.record(restored)!!.getString("recordId"))
        assertEquals(0,WearUndo188.inverse(restored,"undo",2000,1)!!.currentExercise)
    }
    @Test fun laterSetEditCompletionReplacementOrWorkoutPauseBlocksOlderInverse() {
        val w=recorded();val e=w.exercises[0]
        for(nextSet in listOf(e.sets[1].copy(reps="1"),e.sets[1].copy(done=true),e.sets[1].copy(setId="replacement"))){
            val changed=w.copy(exercises=listOf(e.copy(sets=listOf(e.sets[0],nextSet))))
            assertFalse(WearUndo188.available(changed));assertNull(WearUndo188.inverse(changed,"undo",2000,1))
        }
        assertFalse(WearUndo188.available(w.copy(workoutId="other")));assertFalse(WearUndo188.available(w.copy(workoutPaused=true)))
    }
    @Test fun numberFormattingDoesNotInvalidateGuardAndDeliveryMarkerRoundTrips() {
        val w=recorded();val encoded=WorkoutSnapshotStore.encode(w)
        encoded.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("weight",5.500)
        assertTrue(WearUndo188.available(WorkoutSnapshotStore.parse(encoded.toString())!!))
        val next=WearUndo188.inverse(w,"undo",2000,1)!!
        assertEquals("undo",JSONObject(WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(next).toString())!!.undo188!!).getString("commandId"))
        assertTrue(WearUndo188.blocked(next.copy(timerOwner="phone")));assertFalse(WearUndo188.blocked(next))
    }
}
