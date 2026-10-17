package com.repforge.app.wear

import com.repforge.shared.WorkoutOwnership193 as Ownership
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WorkoutOwnership193Test {
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
}
