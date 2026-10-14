package com.repforge.app.wear
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
class WearPhoneAutomaticProjectionTest {
    private fun workout()=WearWorkout(1,"","","","","2026-10-13T08:00:00Z","phone",0,5,0,
        listOf("s1","s2","s3").mapIndexed { i,id -> WearExercise(if(i<2)"run" else "press","Exercise","bodyweight",if(i<2)"mp" else "ism.","time",listOf(WearSet(1,"","0",false,0,0,0.0,id))) },automation=WearAutomatic(enabled=true,owner="phone",phase="work",key="run"))
    @Test fun nativePhoneCompletionUsesStableSetIdentityAndOnlyMeasuredSteps() {
        val rows=JSONArray().put(JSONObject().put("exerciseId","run").put("setId","s1").put("work",2).put("rest",1).put("rounds",1).put("baseSeconds",10))
            .put(JSONObject().put("exerciseId","run").put("setId","s2").put("work",2).put("rest",1).put("rounds",2).put("baseSeconds",0))
        val wire=JSONObject().put("steps",rows).put("stepIndex",1).put("autoAdvance",true).put("completed",2).put("stepElapsedMs",5000)
        val next=projectPhoneAutomatic(workout(),wire,workout().automation!!.copy(phase="complete"))
        assertEquals("13",next.exercises[0].sets[0].reps);assertEquals("5",next.exercises[1].sets[0].reps)
        assertEquals(2,next.completedSets);assertFalse(next.exercises[2].sets[0].done);assertEquals(2,next.currentExercise)
        assertEquals("ready",next.automation!!.phase)
    }
    @Test fun repetitionRestDoesNotInventAWorkSet() {
        val w=workout();val wire=JSONObject().put("steps",JSONArray().put(JSONObject().put("exerciseId","press").put("setId","s3"))).put("restOnly",true).put("completed",1).put("stepIndex",0)
        val next=projectPhoneAutomatic(w,wire,w.automation!!.copy(phase="rest"))
        assertEquals(0,next.completedSets);assertEquals(w.exercises,next.exercises)
    }
    @Test fun stoppedPhoneTimerCannotReplayCompletedSets() {
        val w=workout();val next=projectPhoneAutomatic(w,JSONObject().put("stopped",true),w.automation!!.copy(enabled=false))
        assertEquals(w.exercises,next.exercises);assertEquals(0,next.completedSets)
    }
    @Test fun completedRepetitionRestReturnsToRecordingRatherThanResume() {
        val w=workout().copy(currentExercise=2)
        val wire=JSONObject().put("steps",JSONArray().put(JSONObject().put("exerciseId","press").put("setId","s3")))
            .put("restOnly",true).put("autoAdvance",false).put("completed",1).put("stepIndex",0)
        val next=projectPhoneAutomatic(w,wire,w.automation!!.copy(phase="complete",paused=true,remainingMs=0))
        assertEquals("ready",next.automation!!.phase)
        assertFalse("A finished rest is not a paused timer",next.automation!!.paused)
        assertFalse(next.currentSet!!.done);assertEquals(w.exercises,next.exercises)
    }
    @Test fun realPauseAndInterruptedRestAreNotSilentlyResumed() {
        val w=workout().copy(currentExercise=2)
        val wire=JSONObject().put("steps",JSONArray()).put("restOnly",true)
        for(a in listOf(w.automation!!.copy(phase="rest",paused=true,remainingMs=5000),
            w.automation!!.copy(phase="complete",paused=true,interrupted=true))) {
            val next=projectPhoneAutomatic(w,wire,a)
            assertTrue(next.automation!!.paused);assertEquals(w.exercises,next.exercises)
        }
    }
}
