package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearWorkoutTest {
    private fun sample() = WearWorkout(1L, "p", "Otthoni A/B", "A", "", "2026-10-07T08:00:00Z", "id", 0, 90, 0,
        listOf(WearExercise("plank", "Plank", "bodyweight", "mp", "time", listOf(
            WearSet(1, "30", "0", true, 0, 0, 0.0), WearSet(2, "", "0", false, 0, 0, 0.0)), "30–45")), "Alap A", 17L)

    @Test fun firstIncompleteSetIsSelected() { assertEquals(1, sample().currentSetIndex); assertFalse(sample().exerciseComplete) }
    @Test fun completedExerciseStillHasItsLastSet() {
        val value = sample(); val ex = value.exercises.first()
        val finished = value.copy(exercises = listOf(ex.copy(sets = ex.sets.map { it.copy(done = true) })))
        assertEquals(2, finished.completedSets); assertTrue(finished.exerciseComplete); assertEquals(1, finished.currentSetIndex)
    }
    @Test fun snapshotRoundTripKeepsSequenceTargetAndSideTimes() {
        val value = sample(); val ex = value.exercises.first()
        val sided = value.copy(exercises = listOf(ex.copy(repUnit = "mp/oldal", sets = listOf(ex.sets.first().copy(leftSeconds = 30, rightSeconds = 35)))))
        val parsed = WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(sided).toString())!!
        assertEquals(sided, parsed); assertEquals(17L, parsed.localSequence)
    }
    @Test fun inactiveSnapshotIsNotAnActiveWorkout() { assertNull(WorkoutSnapshotStore.parse("{\"active\":false,\"revision\":8}")) }
    @Test fun durationCannotBeNegative() { assertEquals(0, sample().elapsedSeconds(0L)); assertEquals(60, sample().elapsedSeconds(java.time.Instant.parse("2026-10-07T08:01:00Z").toEpochMilli())) }
    @Test fun stopwatchPauseResumeDoesNotCountPausedTime() {
        val started = WearStopwatch("set").toggle(1000)
        assertEquals(5000L, started.elapsed(6000))
        val paused = started.toggle(6000)
        assertEquals(5000L, paused.elapsed(12000))
        assertEquals(7000L, paused.toggle(12000).elapsed(14000))
    }
    @Test fun restoredRunningStopwatchKeepsItsAnchor() { assertEquals(9000L, WearStopwatch("set", 2000, 1000).elapsed(8000)) }

    @Test fun endingRestNeverRecordsOrNavigates() {
        for (allDone in listOf(false, true)) {
            val value = sample()
            val resting = value.copy(restEndAt = 999999L, exercises = value.exercises.map { ex ->
                if (allDone) ex.copy(sets = ex.sets.map { it.copy(done = true) }) else ex
            })
            val ended = resting.endRest()
            assertEquals(0L, ended.restEndAt)
            assertEquals(resting.currentExercise, ended.currentExercise)
            assertEquals(resting.currentSetIndex, ended.currentSetIndex)
            assertEquals(resting.completedSets, ended.completedSets)
            assertEquals(resting.exercises, ended.exercises)
            assertEquals(resting.localSequence, ended.localSequence)
        }
    }

    @Test fun explicitExerciseSelectionEndsRestAndPreservesMeasurements() {
        val value = sample()
        val exercise = value.exercises.first()
        val resting = value.copy(restEndAt = 999999L, exercises = listOf(exercise, exercise.copy(id = "other")))
        val next = resting.selectExercise(1)
        assertEquals(1, next.currentExercise)
        assertEquals(0L, next.restEndAt)
        assertEquals(resting.exercises, next.exercises)
        assertEquals(resting.completedSets, next.completedSets)
        assertEquals(resting.localSequence, next.localSequence)
        val previous = next.selectExercise(0)
        assertEquals(resting.currentSet, previous.currentSet)
    }

    @Test fun unavailableNavigationDoesNotSkipRest() {
        val resting = sample().copy(restEndAt = 999999L)
        assertSame(resting, resting.selectExercise(-1))
        assertSame(resting, resting.selectExercise(1))
        assertSame(resting, resting.selectExercise(0))
    }
}
