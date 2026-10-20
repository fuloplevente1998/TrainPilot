package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WorkoutPrefill228Test {
    private fun home(reps: Any? = "10", unit: String = "ism.", measurement: String = ""): WatchHomeSnapshot {
        val set = JSONObject().put("set", 1).put("weight", 12.5).put("reps", reps)
            .put("done", true).put("actualRir", 0).put("setId", "old")
        val exercise = JSONObject().put("id", "press").put("loadType", "total")
            .put("repUnit", unit).put("measurementType", measurement)
            .put("sets", org.json.JSONArray().put(set))
        val day = JSONObject().put("id", "A").put("programId", "p")
            .put("exercises", org.json.JSONArray().put(exercise))
        return WatchHomeStore.parse(JSONObject().put("days", org.json.JSONArray().put(day)).toString())!!
    }

    @Test fun plannedWatchStartRetainsValuesButCreatesFreshUncompletedSets() {
        val home = home()
        val first = home.createWorkout(home.days.single(), "", "2020-01-01T10:00:00Z").exercises.single().sets.single()
        val next = home.createWorkout(home.days.single(), "", "2020-01-05T10:00:00Z").exercises.single().sets.single()
        assertEquals("10", first.reps)
        assertEquals("12.5", first.weight)
        assertFalse(first.done)
        assertNull(first.actualRir)
        assertEquals(0, first.leftSeconds)
        assertEquals(0.0, first.distanceMeters, 0.0)
        assertNotEquals("old", first.setId)
        assertNotEquals(first.setId, next.setId)
    }

    @Test fun legacyInvalidAndTimedTemplatesNeverInventRepetitions() {
        for (value in listOf<Any?>(null, "", "bad", -1, 0, 2.5, 10001)) {
            val home = home(value)
            assertEquals("", home.createWorkout(home.days.single(), "", "2020-01-01T10:00:00Z").exercises.single().sets.single().reps)
        }
        for ((unit, measurement) in listOf("mp" to "", "ism." to "distance", "ism." to "time")) {
            val home = home("10", unit, measurement)
            assertEquals("", home.createWorkout(home.days.single(), "", "2020-01-01T10:00:00Z").exercises.single().sets.single().reps)
        }
    }

    @Test fun watchQuickWorkoutRetainsJournalValuesWithoutCompletion() {
        val set = JSONObject().put("set", 1).put("weight", 17.5).put("reps", "8")
        val exercise = JSONObject().put("id", "press").put("loadType", "total")
            .put("repUnit", "ism.").put("sets", org.json.JSONArray().put(set))
        val home = WatchHomeStore.parse(JSONObject().put("quickExercises",
            org.json.JSONArray().put(exercise)).toString())!!
        val first = home.createQuickWorkout(home.quickExercises.single(), "2020-01-01T10:00:00Z")
            .exercises.single().sets.single()
        val next = home.createQuickWorkout(home.quickExercises.single(), "2020-01-02T10:00:00Z")
            .exercises.single().sets.single()
        assertEquals("17.5", first.weight)
        assertEquals("8", first.reps)
        assertFalse(first.done)
        assertNull(first.actualRir)
        assertNotEquals(first.setId, next.setId)
        assertEquals(0, first.leftSeconds)
        assertEquals(0, first.rightSeconds)
        assertEquals(0.0, first.distanceMeters, 0.0)
        assertNotEquals("old", first.setId)
    }

    @Test fun watchQuickTimedAndDistanceTemplatesDoNotInventRepetitions() {
        for ((unit, measurement) in listOf("mp" to "", "ism." to "time", "ism." to "distance", "ism." to "gps")) {
            val template = JSONObject().put("id", "test").put("loadType", "bodyweight")
                .put("repUnit", unit).put("measurementType", measurement)
                .put("sets", org.json.JSONArray().put(JSONObject().put("set", 1)
                    .put("weight", 0).put("reps", "12")))
            val home = WatchHomeStore.parse(JSONObject().put("quickExercises",
                org.json.JSONArray().put(template)).toString())!!
            val result = home.createQuickWorkout(home.quickExercises.single(),
                "2020-01-01T10:00:00Z").exercises.single().sets.single()
            assertEquals("", result.reps)
            assertFalse(result.done)
        }
    }
}
