package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearQuickWorkoutTest {
    private fun home(): WatchHomeSnapshot {
        val pushup = JSONObject().put("id", "pushup").put("name", "Fekvőtámasz")
            .put("loadType", "bodyweight").put("repUnit", "ism.")
            .put("targetReps", "15").put("sets", JSONArray()
                .put(JSONObject().put("set", 1).put("weight", 0))
                .put(JSONObject().put("set", 2).put("weight", 0)))
        val plank = JSONObject().put("id", "plank").put("name", "Plank")
            .put("loadType", "bodyweight").put("repUnit", "mp")
            .put("sets", JSONArray().put(JSONObject().put("set", 1).put("weight", 0)))
        return WatchHomeStore.parse(JSONObject()
            .put("schema", 1).put("restSeconds", 60)
            .put("activeProgramName", "TrainPilot")
            .put("quickExercises", JSONArray().put(pushup).put(plank))
            .toString())!!
    }

    @Test fun ownershipCapabilityStartsOnlyAdvertisedWatchWorkoutsWithTheirOriginalActiveInterval() {
        val legacy=home();val enabled=legacy.copy(ownershipProtocol193=1);val started="2026-10-18T10:00:00Z"
        val day=WatchHomeDay("A","A","program","Program",listOf(enabled.quickExercises.first()))
        for(w in listOf(enabled.createQuickWorkout(enabled.quickExercises.first(),started),enabled.createWorkout(day,"",started))) {
            assertEquals(1,w.ownershipProtocol193);assertEquals(0L,w.ownerEpoch193);assertEquals("watch",w.timerOwner)
            assertEquals(listOf(WearActiveInterval(java.time.Instant.parse(started).toEpochMilli())),w.activeIntervals)
            assertNull(w.handoff193);assertEquals(w,WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(w).toString()))
        }
        assertEquals(0,legacy.createQuickWorkout(legacy.quickExercises.first(),started).ownershipProtocol193)
        assertNull(legacy.createWorkout(day,"",started).activeIntervals)
        assertEquals(1,WatchHomeStore.parse("{\"ownershipProtocol193\":1}")!!.ownershipProtocol193)
        assertEquals(0,WatchHomeStore.parse("{\"ownershipProtocol193\":2}")!!.ownershipProtocol193)
    }

    @Test fun quickCatalogIsParsedWithoutAProgram() {
        val home = home()
        assertEquals(2, home.quickExercises.size)
        assertEquals("pushup", home.quickExercises.first().id)
        assertEquals("Fekvőtámasz", home.quickExercises.first().name)
        assertEquals(2, home.quickExercises.first().sets.size)
    }

    @Test fun quickWorkoutHasOwnIdentityAndKeepsAllSetsOnRoundTrip() {
        val home = home()
        val started = "2026-10-09T19:00:00Z"
        val quick = home.createQuickWorkout(home.quickExercises[0], started)
        assertTrue(quick.quickWorkout)
        assertEquals("", quick.programId)
        assertEquals("quick", quick.dayId)
        assertEquals(started, quick.workoutId)
        assertEquals(60, quick.restSeconds)
        assertEquals(2, quick.exercises[0].sets.size)
        assertFalse(quick.exercises[0].sets[0].done)
        val first = quick.exercises[0]
        val recorded = quick.copy(exercises = listOf(first.copy(sets = first.sets.mapIndexed { i, set ->
            if (i == 0) set.copy(reps = "15", done = true) else set
        })))
        val roundTrip = WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(recorded).toString())!!
        assertTrue(roundTrip.quickWorkout)
        assertEquals(recorded, roundTrip)
        assertEquals(1, roundTrip.completedSets)
        assertEquals("15", roundTrip.exercises[0].sets[0].reps)
    }

    @Test fun addingExerciseKeepsWorkoutIdAndCurrentMeasurements() {
        val home = home()
        val active = home.createQuickWorkout(home.quickExercises[0], "2026-10-09T19:00:00Z")
        val additional = home.toWearExercise(home.quickExercises[1])
        val extended = active.copy(exercises = active.exercises + additional, currentExercise = 1)
        assertEquals(active.workoutId, extended.workoutId)
        assertTrue(extended.quickWorkout)
        assertEquals(listOf("pushup", "plank"), extended.exercises.map { it.id })
        assertEquals(1, extended.currentExercise)
    }

    @Test fun oldProgramSnapshotIsNotMistakenForQuickWorkout() {
        val home = home()
        val day = WatchHomeDay("A", "Első nap", "program", "Program",
            listOf(home.quickExercises.first()))
        val normal = home.createWorkout(day, "", "2026-10-09T19:00:00Z")
        assertFalse(normal.quickWorkout)
        assertFalse(WorkoutSnapshotStore.parse(WorkoutSnapshotStore.encode(normal).toString())!!.quickWorkout)
    }
}
