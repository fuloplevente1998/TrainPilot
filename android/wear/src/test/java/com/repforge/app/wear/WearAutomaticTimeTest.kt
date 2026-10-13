package com.repforge.app.wear
import org.junit.Assert.*
import org.junit.Test
class WearAutomaticTimeTest {
    private fun fixture(timed:Boolean)=WearWorkout(0,"p","Program","A","","2026-10-13T10:00:00Z","workout",0,5,0,
        listOf(WearExercise("press","Press","dumbbell",if(timed)"mp" else "ism.","",listOf(
            WearSet(1,"7","5.5",false,0,0,0.0,"a"),WearSet(2,"7","5.5",false,0,0,0.0,"b")))))
    private fun start(timed:Boolean)=WearAutomaticEngine.enable(fixture(timed),"watch-clock",2,5,false,1000,1)
    @Test fun skippedRestCreditsOnlyActualElapsedRest(){
        val work=WearAutomaticEngine.ready(start(true),1000,1)
        val rest=WearAutomaticEngine.tick(work,3000,1).workout
        val next=WearAutomaticEngine.skipRest(rest,4000,1)
        val finished=WearAutomaticEngine.tick(next,6000,1).workout
        assertEquals(4000L,finished.automation!!.workMs);assertEquals(1000L,finished.automation.restMs)
        assertEquals(2,finished.completedSets)
        assertEquals(finished.automation,WearAutomatic.parse(finished.automation.json()))
    }
    @Test fun pauseAddsBreakTimeWithoutAddingWork(){
        val work=WearAutomaticEngine.ready(start(true),1000,1)
        val paused=WearAutomaticEngine.pause(work,2000,1)
        val resumed=WearAutomaticEngine.resume(paused,8000,1)
        val rest=WearAutomaticEngine.tick(resumed,9000,1).workout
        assertEquals(2000L,rest.automation!!.workMs);assertEquals(6000L,rest.automation.pausedMs)
        assertEquals(0L,rest.automation.restMs)
    }
    @Test fun delayedCallbackCannotInventUnexecutedPhases(){
        val work=WearAutomaticEngine.ready(start(true),1000,1)
        val late=WearAutomaticEngine.tick(work,999999,1).workout
        assertEquals(2000L,late.automation!!.workMs);assertEquals(0L,late.automation.restMs)
        assertEquals(1,late.completedSets)
    }
    @Test fun manualRecordingAccountsReadyWorkWithoutInventingRepetitions(){
        val ready=start(false)
        val completed=ready.copy(exercises=ready.exercises.map{it.copy(sets=it.sets.mapIndexed{i,s->if(i==0)s.copy(done=true)else s})})
        val rest=WearAutomaticEngine.afterRecord(completed,4000,1)
        assertEquals(3000L,rest.automation!!.workMs);assertEquals("7",rest.exercises[0].sets[0].reps)
        val next=WearAutomaticEngine.tick(rest,9000,1).workout
        val working=WearAutomaticEngine.tick(next,11000,1).workout
        assertEquals(5000L,working.automation!!.workMs);assertEquals(5000L,working.automation.restMs)
        assertFalse(working.exercises[0].sets[1].done)
    }
}
