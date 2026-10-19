package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearRotaryEditor212Test {
    private fun workout() = WorkoutSnapshotStore.parse("""{"workoutId":"rotary212","started":"2026-10-19T00:00:00Z","exercises":[{"id":"press","repUnit":"ism.","sets":[{"setId":"one","reps":"5","weight":"5.5","done":false},{"setId":"two","reps":"6","done":false}]}]}""")!!
    @Test fun detentsKeepFractionsAndReverseImmediatelyWithoutAcceleration() {
        val r=WearRotaryEditor212()
        assertEquals(0,r.steps(24f,48f));assertEquals(1,r.steps(24f,48f))
        assertEquals(0,r.steps(24f,48f));assertEquals(-1,r.steps(-48f,48f))
        assertEquals(20,r.steps(48000f,48f));assertEquals(0,r.steps(0f,48f))
        assertEquals(-20,r.steps(-48000f,48f))
        r.steps(24f,48f);r.reset();assertEquals(0,r.steps(24f,48f))
        assertEquals(0,r.steps(Float.NaN,48f));assertEquals(0,r.steps(48f,0f))
    }
    @Test fun rotaryAndTouchShareIncrementsAndSafeBounds() {
        assertEquals(.5,WearRotaryEditor212.step("weight","ism."),0.0)
        assertEquals(1.0,WearRotaryEditor212.step("reps","ism."),0.0)
        assertEquals(5.0,WearRotaryEditor212.step("reps","mp"),0.0)
        assertEquals(5.0,WearRotaryEditor212.step("leftSeconds","mp/oldal"),0.0)
        assertEquals(0.0,WearRotaryEditor212.adjust(0.0,.5,-1),0.0)
        assertEquals(6.0,WearRotaryEditor212.adjust(5.5,.5,1),0.0)
        assertEquals(Int.MAX_VALUE.toDouble(),WearRotaryEditor212.adjust(Int.MAX_VALUE.toDouble(),1.0,20),0.0)
        assertEquals(0.0,WearRotaryEditor212.adjust(Double.NaN,1.0,1),0.0)
    }
    @Test fun staleEditorsNeverFollowSetExerciseOrWorkoutChangesButReconnectKeepsTarget() {
        val w=workout();val target=WearRotaryEditor212.target(w)
        assertTrue(WearRotaryEditor212.editable(w,target))
        assertTrue(WearRotaryEditor212.editable(w.copy(revision=w.revision+1),target))
        assertFalse(WearRotaryEditor212.editable(w.copy(workoutId="other"),target))
        assertFalse(WearRotaryEditor212.editable(w.copy(started="other"),target))
        assertFalse(WearRotaryEditor212.editable(w.copy(exercises=w.exercises.map{it.copy(id="other")}),target))
        val completed=w.copy(exercises=w.exercises.map{it.copy(sets=it.sets.map{s->s.copy(done=true)})})
        assertFalse(WearRotaryEditor212.editable(completed,target))
        val next=w.copy(exercises=w.exercises.map{it.copy(sets=it.sets.mapIndexed{i,s->s.copy(done=i==0)})})
        assertFalse(WearRotaryEditor212.editable(next,target))
        assertFalse(WearRotaryEditor212.editable(w.copy(workoutPaused=true),target))
    }
}
