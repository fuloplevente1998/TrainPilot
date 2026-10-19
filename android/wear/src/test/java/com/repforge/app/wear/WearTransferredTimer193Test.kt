package com.repforge.app.wear

import com.repforge.shared.IntervalTimeline193
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearTransferredTimer193Test {
    private fun frozen(position: Long=18000): WearWorkout {
        val rows=JSONArray("""[{"exerciseId":"plank","setId":"one","work":10,"rest":5,"rounds":2,"baseSeconds":7},{"exerciseId":"plank","setId":"two","work":10,"rest":5,"rounds":2,"baseSeconds":2}]""")
        val timeline=IntervalTimeline193(listOf(IntervalTimeline193.Step("plank","one","",10,5,2,""),IntervalTimeline193.Step("plank","two","",10,5,2,"")),true,3,false)
        val p=timeline.at(position)
        val rest=position-10000
        val parts=JSONArray().put(JSONObject("""{"exerciseId":"plank","setId":"one","workMs":10000,"restMs":$rest,"pausedMs":9000}"""))
            .put(JSONObject("""{"exerciseId":"plank","setId":"two","workMs":0,"restMs":0,"pausedMs":0}"""))
        val timer=JSONObject().put("key","native|4|9").put("workoutId","w193").put("autoAdvance",true).put("restOnly",false).put("initialRest",3).put("steps",rows)
            .put("elapsedMs",position).put("phase",p.phase).put("stepIndex",p.step).put("round",p.round).put("remainingMs",p.remainingMs)
            .put("stepElapsedMs",p.stepElapsedMs).put("totalMs",p.totalMs).put("completed",p.completed)
            .put("ledger",JSONObject().put("schema",1).put("owner","phone").put("positionMs",position).put("workMs",10000).put("restMs",rest).put("pausedMs",9000).put("steps",parts))
        val root=JSONObject("""{"workoutId":"w193","timerOwner":"watch","ownershipProtocol193":1,"ownerEpoch193":1,"workoutPaused":true,"exercises":[{"id":"plank","repUnit":"mp","sets":[{"setId":"one","reps":"7","done":false},{"setId":"two","reps":"2","done":false}]}],"automation":{"enabled":true,"owner":"watch","phase":"work"}}""")
        root.put("timerCheckpoint193",JSONObject().put("schema",1).put("source","phone").put("timer",timer))
        return WearTransferredTimer193.install(WorkoutSnapshotStore.parse(root.toString())!!,1000,3,"ownership193:one")
    }
    private fun running(position: Long=18000)=WearAutomaticEngine.resume(frozen(position).copy(workoutPaused=false),5000,3)
    @Test fun consentWaitIsPausedAndResumeUsesFreshLocalAnchor() {
        val frozen=frozen();assertTrue(frozen.automation!!.paused)
        assertEquals(10000L,frozen.automation.remainingMs);assertEquals(2,frozen.automation.round)
        assertEquals(frozen,WearAutomaticEngine.tick(frozen,999999,3).workout)
        val current=WearAutomaticEngine.tick(running(),7000,3).workout
        assertEquals(8000L,current.automation!!.remainingMs);assertEquals("27",current.exercises[0].sets[0].reps)
        assertEquals(2000L,current.automation.workMs);assertEquals(0L,current.automation.pausedMs)
        val checkpoint=JSONObject(current.timerCheckpoint193!!)
        assertEquals(18000L,checkpoint.getJSONObject("timeLogs193").getJSONObject("native|4|9").getLong("positionMs"))
        assertEquals("watch",checkpoint.getJSONObject("timeLogs193").getJSONObject("ownership193:one").getString("owner"))
    }
    @Test fun skippingRestDoesNotTurnSkippedTimeIntoRepetitionsOrRestLog() {
        val current=WearAutomaticEngine.skipRest(running(14000),5000,3)
        assertEquals("work",current.automation!!.phase);assertEquals(2,current.automation.round)
        val counted=WearAutomaticEngine.tick(current,7000,3).workout
        assertEquals("23",counted.exercises[0].sets[0].reps)
        assertEquals(0L,counted.automation!!.restMs);assertEquals(2000L,counted.automation.workMs)
        assertEquals(20000L,JSONObject(counted.timerCheckpoint193!!).getJSONObject("timer").getLong("elapsedMs"))
    }
    @Test fun delayedCallbackFollowsSourcePlanAcrossRoundsAndSetsWithoutBaselineDuplication() {
        val transition=WearAutomaticEngine.tick(running(),45000,3)
        val current=transition.workout
        assertTrue(current.exercises[0].sets.all { it.done });assertEquals("40",current.exercises[0].sets[0].reps);assertEquals("27",current.exercises[0].sets[1].reps)
        assertEquals(30000L,current.automation!!.workMs);assertEquals(10000L,current.automation.restMs)
        assertEquals("complete",current.automation.phase);assertNull(current.timerCheckpoint193)
        assertEquals(current,WearAutomaticEngine.tick(current,50000,3).workout)
    }
    @Test fun pauseIsMeasuredSeparatelyAndCannotConsumeWork() {
        val paused=WearAutomaticEngine.pause(running(),7000,3)
        val resumed=WearAutomaticEngine.resume(paused,17000,3)
        assertEquals(10000L,resumed.automation!!.pausedMs);assertEquals(8000L,resumed.automation.remainingMs)
        val current=WearAutomaticEngine.tick(resumed,18000,3).workout
        assertEquals(3000L,current.automation!!.workMs);assertEquals("28",current.exercises[0].sets[0].reps)
    }
    @Test fun changedBootCannotReplayMissedWorkAndExplicitResumeKeepsPosition() {
        val before=running();val interrupted=WearAutomaticEngine.tick(before,999999,4).workout
        assertTrue(interrupted.automation!!.interrupted);assertTrue(interrupted.automation.paused)
        assertEquals(before.exercises,interrupted.exercises)
        val resumed=WearAutomaticEngine.resume(interrupted,1000000,4)
        val current=WearAutomaticEngine.tick(resumed,1001000,4).workout
        assertEquals(1000L,current.automation!!.workMs);assertEquals(9000L,current.automation.remainingMs)
    }
    @Test fun typedSaveAndReloadContinuesFromExactFractionalCheckpoint() {
        val before=WearAutomaticEngine.tick(running(),6123,3).workout
        val restored=WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(before).toString())!!
        val next=WearAutomaticEngine.tick(restored,7000,3).workout
        assertEquals(2000L,next.automation!!.workMs);assertEquals(8000L,next.automation.remainingMs)
    }
    @Test fun corruptPlanStopsAndDisablingRetiresCheckpoint() {
        val before=running();val corrupt=JSONObject(before.timerCheckpoint193!!)
        corrupt.getJSONObject("timer").getJSONArray("steps").getJSONObject(0).put("setId","missing")
        val interrupted=WearAutomaticEngine.tick(before.copy(timerCheckpoint193=corrupt.toString()),6000,3).workout
        assertTrue(interrupted.automation!!.interrupted);assertEquals(before.exercises,interrupted.exercises)
        val disabled=WearAutomaticEngine.disable(before,6000,3)
        assertNull(disabled.timerCheckpoint193);assertFalse(disabled.automation!!.enabled)
    }
}
