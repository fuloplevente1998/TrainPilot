package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearPendingHandoffTest {
    private fun workout(revision: Long, sequence: Long, reps: String, done: Boolean) = WearWorkout(
        revision,"p","Program","d","","2026-10-11T09:00:00Z","current",0,60,0,
        listOf(WearExercise("e","Gyakorlat","bodyweight","ism.","reps",listOf(WearSet(1,reps,"",done,0,0,0.0)))),localSequence=sequence)

    @Test fun newerPhoneReceiptUnblocksMeasurementWithoutDiscardingPendingWatchEdits() {
        val watch=workout(20,70,"12",true)
        val phone=workout(21,60,"10",false)
        val start=WearMeasurementStart("current",21)
        assertFalse(start.matches(watch.workoutId,watch.revision))
        val accepted=preservePendingWearEdits(watch,phone)
        assertTrue(start.matches(accepted.workoutId,accepted.revision))
        assertEquals(70L,accepted.localSequence)
        assertEquals("12",accepted.currentSet!!.reps)
        assertTrue(accepted.currentSet!!.done)
        assertEquals(20L,watch.revision)
    }

    @Test fun acknowledgedPhoneSnapshotCanApplyAndAnOlderRevisionCannotRegressOwnership() {
        val watch=workout(21,70,"12",true)
        val acknowledged=workout(22,70,"12",true)
        assertSame(acknowledged,preservePendingWearEdits(watch,acknowledged))
        val late=preservePendingWearEdits(watch,workout(19,60,"10",false))
        assertEquals(21L,late.revision)
        assertEquals("12",late.currentSet!!.reps)
    }
}
