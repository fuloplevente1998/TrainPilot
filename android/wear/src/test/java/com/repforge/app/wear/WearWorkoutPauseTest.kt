package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearWorkoutPauseTest {
    private val wall=java.time.Instant.parse("2026-10-14T10:00:00Z").toEpochMilli()
    private fun sample()=WearWorkout(1,"p","Program","A","","2026-10-14T10:00:00Z","workout",0,120,wall+120000,
        listOf(WearExercise("press","Nyomás","dumbbell","ism.","reps",listOf(WearSet(1,"5","5.5",false,0,0,0.0,"a")))),
        timerOwner="watch",automation=WearAutomatic(enabled=true,owner="watch",key="clock",phase="rest",remainingMs=120000,anchorMs=1000,boot=1,targetExercise=0,targetSet=0,targetId="a"),
        activeIntervals=listOf(WearActiveInterval(wall)))
    @Test fun twentyMinutePauseFreezesTimeAndTheExistingClock() {
        val before=sample();val incoming=before.copy(pauseRevision=1,workoutPaused=true,activeIntervals=listOf(WearActiveInterval(wall,wall+10000)))
        val paused=mergeWorkoutPause(before,incoming,11000,wall+10000)
        assertTrue(paused.workoutPaused);assertTrue(paused.automation!!.paused)
        assertEquals(110000L,paused.automation.remainingMs);assertEquals(10,paused.elapsedSeconds(wall+1210000))
        val later=WearAutomaticEngine.tick(paused,1211000,1).workout
        assertEquals(paused,later);assertFalse(later.currentSet!!.done)
        val resumed=mergeWorkoutPause(paused,incoming.copy(workoutPaused=false,pauseRevision=2,activeIntervals=incoming.activeIntervals!!+WearActiveInterval(wall+1210000)),1211000,wall+1210000)
        assertFalse(resumed.automation!!.paused);assertEquals(110000L,resumed.automation.remainingMs)
        assertEquals(15,resumed.elapsedSeconds(wall+1215000));assertEquals(105000L,resumed.automation.remaining(1216000))
    }
    @Test fun staleActiveSnapshotAndLocalEditorsCannotResumeThePause() {
        val before=sample();val paused=mergeWorkoutPause(before,before.copy(workoutPaused=true,pauseRevision=3),11000,wall+10000)
        val old=before.copy(revision=50,localSequence=0)
        assertTrue(mergeWorkoutPause(paused,old,1211000,wall+1210000).workoutPaused)
        val edited=paused.copy(localSequence=20,exercises=paused.exercises.map { it.copy(sets=it.sets.map { s->s.copy(reps="8") }) })
        val fresh=mergeWorkoutPause(edited,old.copy(pauseRevision=4,workoutPaused=true),1211000,wall+1210000)
        val preserved=preservePendingWearEdits(edited,fresh)
        assertTrue(preserved.workoutPaused);assertEquals("8",preserved.currentSet!!.reps);assertEquals(4L,preserved.pauseRevision)
    }
    @Test fun alreadyPausedAutomaticClockStaysPausedAfterWorkoutContinuation() {
        val before=sample().copy(automation=sample().automation!!.copy(paused=true))
        val paused=mergeWorkoutPause(before,before.copy(pauseRevision=1,workoutPaused=true),11000,wall+10000)
        val resumed=mergeWorkoutPause(paused,paused.copy(pauseRevision=2,workoutPaused=false),1211000,wall+1210000)
        assertTrue(resumed.automation!!.paused)
    }
    @Test fun pauseRoundTripSurvivesRestart() {
        val before=sample();val paused=mergeWorkoutPause(before,before.copy(pauseRevision=1,workoutPaused=true,activeIntervals=listOf(WearActiveInterval(wall,wall+10000))),11000,wall+10000)
        assertEquals(paused,WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(paused).toString()))
    }
    @Test fun differentWorkoutDoesNotInheritPause() {
        val paused=sample().copy(pauseRevision=10,workoutPaused=true)
        assertFalse(mergeWorkoutPause(paused,sample().copy(workoutId="next"),1000,wall).workoutPaused)
    }
}
