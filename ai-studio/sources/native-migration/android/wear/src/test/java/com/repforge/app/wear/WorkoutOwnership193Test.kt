package com.repforge.app.wear

import com.repforge.shared.WorkoutOwnership193 as Ownership
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WorkoutOwnership193Test {
    @Test fun frozenTimerCheckpointSurvivesTypedSnapshotAndConsentRoundTrip() {
        val checkpoint=JSONObject("""{"schema":1,"source":"phone","sourceEpoch":0,"resumeRequested":false,"timer":{"key":"native|4|9","elapsedMs":96000,"remainingMs":14000,"round":2,"phase":"rest","steps":[{"exerciseId":"plank","setId":"set:1","baseSeconds":7}],"ledger":{"workMs":60000,"restMs":36000,"pausedMs":24000}}}""")
        val source=current().put("timerCheckpoint193",checkpoint)
        val offered=Ownership.offer(source,"phone","phone-a","watch-a","timer193",0,10000)
        val consent=Ownership.accept(source,offered,"watch-a","phone-a")
        val committed=Ownership.commit(offered,consent,"phone-a","watch-a")
        val workout=WorkoutSnapshotStore.parse(committed.toString())!!
        val saved=WorkoutSnapshotStore.encode(workout).getJSONObject("timerCheckpoint193")
        assertEquals(checkpoint.toString(),saved.toString())
        assertEquals(14000,saved.getJSONObject("timer").getInt("remainingMs"))
        assertTrue(workout.workoutPaused)
    }
    private fun current()=JSONObject("""{"active":true,"workoutId":"workout:193","timerOwner":"phone","ownershipProtocol193":1,"ownerEpoch193":0,"pauseRevision":4,"restEndAt":12000,"exercises":[{"id":"plank","sets":[{"setId":"set:1","reps":"12","done":true}]}],"activeIntervals":[{"start":"1970-01-01T00:00:01Z","end":null}],"automation":{"owner":"phone","phase":"work","remainingMs":9000,"deadlineAt":18000,"paused":false}}""")
    private fun offer()=Ownership.offer(current(),"phone","phone-a","watch-a","handoff:1",0,10000)
    private fun accepted()=Ownership.accept(current(),offer(),"watch-a","phone-a")
    private fun committed()=Ownership.commit(offer(),accepted(),"phone-a","watch-a")
    private fun rejected(action:()->Unit) { try { action();fail("unsafe transfer accepted") }catch(expected:IllegalStateException){} }

    @Test fun sourceFreezesBeforeOfferAndTargetCannotRunBeforeCommit() {
        val offered=offer()
        assertTrue(offered.getBoolean("workoutPaused"))
        assertEquals(5,offered.getInt("pauseRevision"))
        assertEquals(2000,offered.getInt("pausedRestMillis"))
        assertEquals(8000,offered.getJSONObject("automation").getInt("remainingMs"))
        assertEquals("1970-01-01T00:00:10Z",offered.getJSONArray("activeIntervals").getJSONObject(0).getString("end"))
        assertFalse(Ownership.owns(offered,"phone"))
        assertFalse(Ownership.owns(accepted(),"watch"))
        assertEquals(0L,Ownership.epoch(accepted()))
        assertTrue(Ownership.owns(committed(),"watch"))
        assertTrue(committed().getBoolean("workoutPaused"))
        assertEquals(1L,Ownership.epoch(committed()))
        assertEquals(current().getJSONArray("exercises").toString(),committed().getJSONArray("exercises").toString())
    }
    @Test fun lostAcceptCanReplayAfterProcessRestartWithoutNewEpoch() {
        val durableOffer=JSONObject(offer().toString())
        val durableAccepted=JSONObject(accepted().toString())
        assertEquals(durableAccepted.toString(),Ownership.accept(durableAccepted,durableOffer,"watch-a","phone-a").toString())
        val committed=Ownership.commit(durableOffer,durableAccepted,"phone-a","watch-a")
        assertEquals(committed.toString(),Ownership.commit(committed,durableAccepted,"phone-a","watch-a").toString())
    }
    @Test fun duplicateCommitPreservesOfflineSetChanges() {
        val finished=Ownership.finish(accepted(),committed(),"watch-a","phone-a")
        finished.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","25")
        finished.put("workoutPaused",false)
        val replay=Ownership.finish(finished,committed(),"watch-a","phone-a")
        assertEquals("25",replay.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"))
        assertFalse(replay.getBoolean("workoutPaused"))
    }
    @Test fun changedPeerEpochWorkoutOrTransferCannotStealOwnership() {
        rejected { Ownership.accept(current(),offer(),"watch-b","phone-a") }
        rejected { Ownership.accept(current(),offer(),"watch-a","phone-b") }
        rejected { Ownership.accept(current().put("workoutId","other"),offer(),"watch-a","phone-a") }
        rejected { Ownership.accept(current().put("ownerEpoch193",1),offer(),"watch-a","phone-a") }
        val other=accepted();other.getJSONObject("handoff193").put("id","different")
        rejected { Ownership.commit(offer(),other,"phone-a","watch-a") }
        rejected { Ownership.finish(current(),committed(),"watch-a","phone-a") }
        rejected { Ownership.offer(offer(),"phone","phone-a","watch-a","handoff:2",0,11000) }
        assertTrue(Ownership.stale(committed(),current()))
    }
    @Test fun malformedOrNegativeEpochOfferIsRejected() {
        rejected { Ownership.offer(current(),"phone","phone-a","watch-a","handoff:1",-1,10000) }
        rejected { Ownership.offer(current().put("workoutId",""),"phone","phone-a","watch-a","handoff:1",0,10000) }
        val invalid=offer();invalid.getJSONObject("handoff193").put("target","phone")
        rejected { Ownership.accept(null,invalid,"watch-a","phone-a") }
        val invalidOwner=offer();invalidOwner.put("timerOwner","watch")
        rejected { Ownership.accept(null,invalidOwner,"watch-a","phone-a") }
    }
    @Test fun reconnectDoesNotReturnOwnerButExplicitReverseTransferDoes() {
        val watch=Ownership.finish(accepted(),committed(),"watch-a","phone-a")
        val reverse=Ownership.offer(watch,"watch","watch-a","phone-a","handoff:2",1,20000)
        val accepted=Ownership.accept(committed(),reverse,"phone-a","watch-a")
        val committed=Ownership.commit(reverse,accepted,"watch-a","phone-a")
        val returned=Ownership.finish(accepted,committed,"phone-a","watch-a")
        assertEquals(2L,Ownership.epoch(returned))
        assertTrue(Ownership.owns(returned,"phone"))
        assertTrue(returned.getBoolean("workoutPaused"))
        rejected { Ownership.offer(returned,"phone","phone-a","watch-a","handoff:3",0,30000) }
    }
    @Test fun sameEpochCannotChangeOwnerAndAnotherWorkoutCannotEraseTransferredDraft() {
        val current=committed()
        assertTrue(Ownership.stale(current,Ownership.copy(current).put("timerOwner","phone")))
        assertTrue(Ownership.stale(current,current().put("workoutId","different")))
        assertFalse(Ownership.stale(current,Ownership.copy(current)))
        val corrupt=offer().put("ownerEpoch193",5)
        rejected { Ownership.commit(corrupt,accepted(),"phone-a","watch-a") }
    }
    @Test fun unsupportedProtocolTombstoneAndCounterOverflowFailClosed() {
        val invalid=accepted();invalid.getJSONObject("handoff193").put("protocol",2)
        rejected { Ownership.commit(offer(),invalid,"phone-a","watch-a") }
        val invalidCommit=committed();invalidCommit.getJSONObject("handoff193").put("protocol",2)
        rejected { Ownership.finish(accepted(),invalidCommit,"watch-a","phone-a") }
        rejected { Ownership.accept(current(),offer().put("active",false),"watch-a","phone-a") }
        rejected { Ownership.finish(accepted(),committed().put("active",false),"watch-a","phone-a") }
        rejected { Ownership.offer(current().put("pauseRevision",Long.MAX_VALUE),"phone","phone-a","watch-a","handoff:1",0,10000) }
        rejected { Ownership.offer(committed(),"watch","watch-a","phone-a","handoff:1",1,20000) }
    }
}
