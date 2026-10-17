package com.repforge.app.wear

import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class WearBilateral192Test {
    private fun workout(first: String="left",left: Int=30,right: Int=40,swap: Int=10,rest: Int=60,sets: Int=2,rounds: Int=1): WearWorkout {
        val exercise=WearExercise("side-plank","Oldalsó plank","bodyweight","mp/oldal","time",(1..sets).map { WearSet(it,"0","0",false,0,0,0.0,"set$it") },bilateral192=WearBilateralConfig192(first,left,right,swap,rest))
        val initial=WearWorkout(1,"p","Program","a","","2026-10-17T00:00:00Z","side192",0,rest,0,listOf(exercise),timerOwner="watch",bilateralProtocol192=1)
        val enabled=WearAutomaticEngine.enable(initial,"clock192",left,rest,false,0,1).let { it.copy(automation=it.automation!!.copy(rounds=rounds)) }
        return WearAutomaticEngine.ready(enabled,0,1)
    }
    private fun finish(initial: WearWorkout): WearWorkout {var w=initial;var now=w.automation!!.anchorMs;repeat(1000){val a=w.automation!!;if(a.phase=="complete")return w;now+=a.remainingMs;w=WearAutomaticEngine.tick(w,now,1).workout};error("Timeline did not finish")}
    @Test fun bothSideOrdersMatchThe220SecondPhoneExampleExactly() {
        for(first in listOf("left","right")){
            val initial=workout(first);assertEquals(first,initial.automation!!.side192)
            val firstEnd=WearAutomaticEngine.tick(initial,initial.automation!!.remainingMs,1).workout
            assertEquals("rest",firstEnd.automation!!.phase);assertEquals("side",firstEnd.automation!!.restKind192);assertEquals(if(first=="left")"right" else "left",firstEnd.automation!!.side192)
            val done=finish(initial);val a=done.automation!!
            assertEquals("complete",a.phase);assertEquals(140000L,a.workMs);assertEquals(80000L,a.restMs);assertEquals(220000L,a.workMs+a.restMs);assertEquals(2,done.completedSets)
            done.exercises[0].sets.forEach { assertEquals(30,it.leftSeconds);assertEquals(40,it.rightSeconds);assertEquals("30",it.reps);assertTrue(it.done) }
            assertEquals(2,a.json().getJSONObject("timeLog").getInt("schema"))
        }
    }
    @Test fun multipleRoundsAndZeroRestFinishOnlyAfterBothSides() {
        val zero=workout("right",2,3,0,0,1,2);val second=WearAutomaticEngine.tick(zero,3000,1).workout
        assertEquals("left",second.automation!!.side192);assertEquals("work",second.automation!!.phase);assertFalse(second.currentSet!!.done)
        val done=finish(zero);assertEquals(10000L,done.automation!!.workMs);assertEquals(0L,done.automation!!.restMs);assertEquals(4,done.currentSet!!.leftSeconds);assertEquals(6,done.currentSet!!.rightSeconds);assertTrue(done.currentSet!!.done)
    }
    @Test fun pauseSkipAndRestartNeverInventWorkOrRest() {
        var w=workout("left",2,3,1,0,1)
        w=WearAutomaticEngine.tick(w,1000,1).workout;w=WearAutomaticEngine.pause(w,1000,1);w=WearAutomaticEngine.tick(w,5000,1).workout
        assertEquals(4000L,w.automation!!.pausedMs);assertEquals(1,w.currentSet!!.leftSeconds)
        w=WearAutomaticEngine.resume(w,5000,1);w=WearAutomaticEngine.tick(w,6000,1).workout;w=WearAutomaticEngine.skipRest(w,6000,1);w=WearAutomaticEngine.tick(w,9000,1).workout
        assertEquals(5000L,w.automation!!.workMs);assertEquals(0L,w.automation!!.restMs);assertTrue(w.currentSet!!.done)
        val partial=WearAutomaticEngine.tick(workout("left",2,3,1,0,1),1000,1).workout
        val saved=WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(WearAutomaticEngine.interrupt(partial,partial.automation!!.anchorMs)).toString())!!
        assertEquals(1000L,saved.automation!!.remainingMs);assertEquals("left",saved.automation!!.side192);assertEquals(1,saved.currentSet!!.leftSeconds)
        val resumed=WearAutomaticEngine.resume(saved,90000,2);val next=WearAutomaticEngine.tick(resumed,91000,2).workout
        assertEquals(2000L,next.automation!!.workMs);assertEquals(0L,next.automation!!.pausedMs);assertEquals("rest",next.automation!!.phase)
    }
    @Test fun pausedFutureEditKeepsCurrentWorkAndActualPriorTime() {
        var w=WearAutomaticEngine.tick(workout("left",2,3,1,0,1),1000,1).workout
        w=WearAutomaticEngine.pause(w,1000,1)
        w=w.copy(exercises=w.exercises.map { it.copy(bilateral192=it.bilateral192!!.copy(leftSeconds=20,rightSeconds=4)) })
        w=WearAutomaticEngine.resume(w,1000,1);w=WearAutomaticEngine.tick(w,2000,1).workout
        assertEquals(2,w.currentSet!!.leftSeconds);w=WearAutomaticEngine.skipRest(w,2000,1);assertEquals(4000L,w.automation!!.remainingMs)
        w=WearAutomaticEngine.tick(w,6000,1).workout;assertTrue(w.currentSet!!.done);assertEquals(2,w.currentSet!!.leftSeconds);assertEquals(4,w.currentSet!!.rightSeconds)
        assertEquals("complete",JSONObject(w.currentSet!!.sideState192!!).getString("left"))
    }
    @Test fun extendingRestChangesOnlyTheActivePhase() {
        var w=workout("left",2,3,1,0,1);w=WearAutomaticEngine.tick(w,2000,1).workout
        w=WearAutomaticEngine.adjustRest192(w,5,2000,1);assertEquals(6000L,w.automation!!.remainingMs);assertEquals(1,w.exercise!!.bilateral192!!.sideRestSeconds)
        w=WearAutomaticEngine.tick(w,4000,1).workout;w=WearAutomaticEngine.skipRest(w,4000,1);w=WearAutomaticEngine.tick(w,7000,1).workout
        assertEquals(5000L,w.automation!!.workMs);assertEquals(2000L,w.automation!!.restMs)
    }
    @Test fun skippingSidePreservesPartialWorkAndNeverCompletesThePair() {
        var w=workout("left",2,3,1,0,1);w=WearAutomaticEngine.tick(w,1000,1).workout;w=WearAutomaticEngine.skipSide192(w,1000,1)
        w=WearAutomaticEngine.tick(w,2000,1).workout;w=WearAutomaticEngine.tick(w,5000,1).workout
        assertEquals("complete",w.automation!!.phase);assertFalse(w.currentSet!!.done);assertEquals(1,w.currentSet!!.leftSeconds);assertEquals(3,w.currentSet!!.rightSeconds);assertTrue(WearBilateral192.skipped(w.currentSet!!))
    }
    @Test fun undoAndRepeatedPairRetainAllActuallyMeasuredTime() {
        val initial=workout("left",2,3,1,0,1)
        val lastWork=WearAutomaticEngine.tick(WearAutomaticEngine.tick(initial,2000,1).workout,3000,1).workout
        val done=WearAutomaticEngine.tick(lastWork,6000,1).workout
        val captured=WearUndo188.capture(lastWork,done,0,0,"record192",6000)
        val inverse=WearUndo188.inverse(captured,"undo192",6000,1)!!
        assertFalse(inverse.currentSet!!.done);assertEquals(5000L,inverse.automation!!.workMs)
        val restarted=WearAutomaticEngine.ready(WearAutomaticEngine.resume(inverse,6000,1),6000,1)
        val repeated=finish(restarted)
        assertEquals(10000L,repeated.automation!!.workMs);assertEquals(2000L,repeated.automation!!.restMs)
        val row=repeated.automation!!.json().getJSONObject("timeLog").getJSONArray("steps").getJSONObject(0)
        assertEquals(4000L,row.getLong("leftWorkMs"));assertEquals(6000L,row.getLong("rightWorkMs"));assertTrue(repeated.currentSet!!.done)
    }
    @Test fun completelySkippedSideStaysSkippedAndProtocolSchemaIsStrict() {
        var w=WearAutomaticEngine.skipSide192(workout("left",2,3,0,0,1),0,1)
        w=WearAutomaticEngine.tick(w,3000,1).workout
        assertEquals("complete",w.automation!!.phase);assertFalse(w.currentSet!!.done)
        assertEquals("skipped",JSONObject(w.currentSet!!.sideState192!!).getString("left"))
        for(schema in listOf<Any>("1",1.5,2)) {
            try { WearBilateralConfig192.parse(WearBilateralConfig192().json().put("schema",schema));fail("Invalid protocol schema accepted") } catch(_: IllegalArgumentException) {}
        }
    }
    @Test fun skippedPairKeepsItsOwnHeadingUntilItsSecondSideFinishes() {
        var w=workout("left",2,3,1,0,2)
        w=WearAutomaticEngine.skipSide192(w,0,1)
        assertEquals(0,w.currentSetIndex);assertEquals("set1",w.currentSet!!.setId)
        w=WearAutomaticEngine.tick(w,1000,1).workout;assertEquals(0,w.currentSetIndex)
        w=WearAutomaticEngine.tick(w,4000,1).workout
        assertEquals(1,w.currentSetIndex);assertFalse(w.exercises[0].sets[0].done)
    }
    @Test fun phoneProjectionUsesMeasuredSidesInsteadOfVirtualElapsed() {
        val current=workout("left",2,3,1,0,1).copy(timerOwner="phone")
        val a=current.automation!!.copy(owner="phone",side192="right",phase="rest",restKind192="side")
        val value=JSONObject().put("autoAdvance",true).put("stepIndex",0).put("stepElapsedMs",3000).put("completed",0)
            .put("steps",JSONArray().put(JSONObject().put("exerciseId","side-plank").put("setId","set1").put("work",2).put("rest",0).put("rounds",1).put("bilateral192",current.exercise!!.bilateral192!!.json()).put("baseLeftSeconds",0).put("baseRightSeconds",0)))
            .put("ledger",JSONObject().put("steps",JSONArray().put(JSONObject().put("leftWorkMs",1000).put("rightWorkMs",0))))
        val projected=projectPhoneAutomatic(current,value,a);assertEquals(1,projected.currentSet!!.leftSeconds);assertEquals(0,projected.currentSet!!.rightSeconds);assertFalse(projected.currentSet!!.done)
        assertEquals(1f,wearRestProgress(projected.copy(automation=a.copy(phaseSeconds192=10)),10),.001f)
    }
}
