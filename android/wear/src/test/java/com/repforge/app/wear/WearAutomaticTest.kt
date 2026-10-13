package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearAutomaticTest {
    private fun fixture(timed: Boolean=false)=WearWorkout(0,"p","Program","A","","2026-10-13T10:00:00Z","workout",0,5,0,
        listOf(WearExercise("press","Nyomás","dumbbell",if(timed)"mp" else "ism.","time",listOf(
            WearSet(1,if(timed)"" else "5","5.5",false,0,0,0.0,"s1"),
            WearSet(2,"","5.5",false,0,0,0.0,"s2"))),
            WearExercise("plank","Plank","bodyweight","mp","time",listOf(WearSet(1,"","0",false,0,0,0.0,"s3")))))
    private fun enable(w: WearWorkout=fixture(),rest: Int=5)=WearAutomaticEngine.enable(w,"run",2,rest,true,1000,1)
    private fun record(w: WearWorkout): WearWorkout { val e=w.exercises[0];return w.copy(exercises=listOf(e.copy(sets=listOf(e.sets[0].copy(done=true),e.sets[1])))+w.exercises.drop(1)) }
    @Test fun enablingNeverInventsASetOrStartsUnrequestedWork() {
        val w=enable(fixture(true));assertEquals("ready",w.automation!!.phase);assertEquals(0,w.completedSets)
        assertEquals(0,w.automation.remainingMs);assertEquals(w,WearAutomaticEngine.tick(w,999999,1).workout)
    }
    @Test fun repetitionRestExpiresWithoutRecordingNextSet() {
        val w=WearAutomaticEngine.afterRecord(record(enable()),1000,1)
        assertEquals("rest",w.automation!!.phase);assertEquals(1,w.automation.targetSet)
        val next=WearAutomaticEngine.tick(w,6000,1).workout
        assertEquals("ready",next.automation!!.phase);assertEquals(1,next.completedSets)
        assertFalse(next.exercises[0].sets[1].done);assertEquals("",next.exercises[0].sets[1].reps)
    }
    @Test fun lastRepetitionRestStartsNextTimedExercise() {
        val w=enable();val recorded=w.copy(exercises=w.exercises.mapIndexed { i,e->if(i==0)e.copy(sets=e.sets.map { it.copy(done=true) })else e })
        val rest=WearAutomaticEngine.afterRecord(recorded,1000,1)
        val work=WearAutomaticEngine.tick(rest,6000,1).workout
        assertEquals(1,work.currentExercise);assertEquals("work",work.automation!!.phase);assertEquals(2000,work.automation.remainingMs)
        assertFalse(work.exercises[1].sets[0].done)
    }
    @Test fun timedWorkRecordsExactlyItsWorkThenRests() {
        val work=WearAutomaticEngine.ready(enable(fixture(true)),1000,1)
        val first=WearAutomaticEngine.tick(work,2500,1);assertEquals("1",first.workout.exercises[0].sets[0].reps)
        val end=WearAutomaticEngine.tick(first.workout,3000,1)
        assertEquals(0,end.completedExercise);assertEquals(0,end.completedSet);assertEquals("2",end.workout.exercises[0].sets[0].reps)
        assertEquals("rest",end.workout.automation!!.phase);assertEquals(1,end.workout.completedSets)
        assertNull(WearAutomaticEngine.tick(end.workout,3000,1).completedSet)
    }
    @Test fun delayedCallbackDoesNotFabricateMultipleTimedSets() {
        val work=WearAutomaticEngine.ready(enable(fixture(true)),1000,1)
        val end=WearAutomaticEngine.tick(work,100000,1)
        assertEquals(1,end.workout.completedSets);assertEquals(5000,end.workout.automation!!.remainingMs)
        assertEquals("rest",end.workout.automation.phase)
    }
    @Test fun zeroRestStartsNextWorkWithoutFinalRest() {
        val work=WearAutomaticEngine.ready(enable(fixture(true),0),1000,1)
        val second=WearAutomaticEngine.tick(work,3000,1).workout
        assertEquals("work",second.automation!!.phase);assertEquals(1,second.automation.targetSet)
        val last=second.copy(exercises=listOf(second.exercises[0]))
        val complete=WearAutomaticEngine.tick(last,5000,1).workout
        assertEquals("complete",complete.automation!!.phase);assertEquals(2,complete.completedSets)
        assertEquals(0,complete.automation.remainingMs)
    }
    @Test fun pauseDoesNotCountPausedTimeOrReplayARecord() {
        val work=WearAutomaticEngine.ready(enable(fixture(true)),1000,1)
        val paused=WearAutomaticEngine.pause(work,2000,1)
        assertEquals(1000,paused.automation!!.remainingMs);assertEquals(paused,WearAutomaticEngine.tick(paused,100000,1).workout)
        val resumed=WearAutomaticEngine.resume(paused,100000,1)
        assertEquals(1,WearAutomaticEngine.tick(resumed,101000,1).workout.completedSets)
    }
    @Test fun rebootOrChangedTargetPausesInsteadOfAdvancing() {
        val work=WearAutomaticEngine.ready(enable(fixture(true)),1000,1)
        assertTrue(WearAutomaticEngine.tick(work,5000,2).workout.automation!!.interrupted)
        val changed=work.copy(exercises=work.exercises.map { e->e.copy(sets=e.sets.map { it.copy(setId="replacement") }) })
        assertTrue(WearAutomaticEngine.tick(changed,5000,1).workout.automation!!.interrupted)
        assertEquals(0,WearAutomaticEngine.tick(changed,5000,1).workout.completedSets)
    }
    @Test fun phoneOwnedTimelineIsNeverAdvancedByWatch() {
        val w=WearAutomaticEngine.ready(enable(fixture(true)),1000,1)
        val phone=w.copy(automation=w.automation!!.copy(owner="phone"))
        assertEquals(phone,WearAutomaticEngine.tick(phone,999999,1).workout)
    }
    @Test fun hapticsCountDownThreeTimesAndFinishOnce() {
        val rest=WearAutomaticEngine.afterRecord(record(enable()),1000,1).automation!!
        val h=WearAutomaticHaptics();assertEquals(0,h.next(rest,1000))
        assertEquals(80,h.next(rest,3000));assertEquals(0,h.next(rest,3100));assertEquals(80,h.next(rest,4000))
        assertEquals(0,h.next(rest.copy(paused=true),5000));assertEquals(80,h.next(rest,5000))
        assertEquals(1000,h.next(rest.copy(phase="ready",phaseNumber=rest.phaseNumber+1),6000))
        assertEquals(0,h.next(rest.copy(phase="ready",phaseNumber=rest.phaseNumber+1),6100))
    }
    @Test fun automaticStateRoundTripPreservesPausedAnchorAndOwnership() {
        val a=WearAutomatic(enabled=true,key="run",phase="rest",paused=true,remainingMs=4321,anchorMs=1234,boot=7,targetId="s2",phaseNumber=9)
        assertEquals(a,WearAutomatic.parse(a.json()))
    }
}
