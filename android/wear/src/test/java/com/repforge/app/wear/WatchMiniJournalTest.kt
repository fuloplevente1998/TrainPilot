package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WatchMiniJournalTest {
    private fun sample(id: String = "2026-10-09T20:00:00Z"): JSONObject {
        val set = JSONObject().put("number", 1).put("reps", "15").put("weight", 0)
            .put("leftSeconds", JSONObject.NULL).put("rightSeconds", JSONObject.NULL)
        val exercise = JSONObject().put("name", "Fekvőtámasz")
            .put("sets", JSONArray().put(set))
        val wear = JSONObject().put("source", "wear_health_services").put("averageHeartRate", 121.2)
            .put("maxHeartRate", 158).put("totalCalories", 390.0).put("steps", 0)
            .put("activeDurationSeconds", 2700)
        val health = JSONObject().put("source", "health_connect").put("activeCalories", 1)
            .put("totalCalories", 49).put("exerciseMinutes", 0)
        return JSONObject().put("workoutId", id).put("started", "2026-10-09T20:00:00Z")
            .put("finished", "2026-10-09T20:45:00Z").put("dayName", "Gyors edzés")
            .put("quickWorkout", true).put("exercises", JSONArray().put(exercise))
            .put("wear", wear).put("healthConnect", health)
    }

    @Test fun measuredWearAndHealthConnectNeverMix() {
        val result = WatchMiniJournal.parse(JSONArray().put(sample())).single()
        assertTrue(result.quickWorkout)
        assertEquals(2700, result.durationSeconds)
        assertEquals("15", result.exercises.single().sets.single().reps)
        assertEquals(390.0, result.wear!!.totalCalories!!, 0.001)
        assertEquals(121.2, result.wear!!.averageHeartRate!!, 0.001)
        assertEquals(49.0, result.healthConnect!!.totalCalories!!, 0.001)
        assertEquals(1.0, result.healthConnect!!.activeCalories!!, 0.001)
        assertNull(result.healthConnect!!.workoutCalories)
        assertEquals(0.0, result.wear!!.steps!!, 0.001)
    }

    @Test fun missingAndInvalidSourcesDoNotMakeUpMetrics() {
        val row = sample().put("wear", JSONObject()
            .put("source", "health_connect").put("totalCalories", 400))
            .put("healthConnect", JSONObject()
                .put("source", "health_connect")
                .put("workoutCalories", JSONObject.NULL)
                .put("totalCalories", -10)
                .put("averageHeartRate", Double.NaN))
        val data = WatchMiniJournal.parse(JSONArray().put(row)).single()
        assertNull(data.wear)
        assertNull(data.healthConnect!!.workoutCalories)
        assertNull(data.healthConnect!!.totalCalories)
        assertNull(data.healthConnect!!.averageHeartRate)
    }

    @Test fun corruptRowsAndDuplicatesAreRejected() {
        val rows = JSONArray().put(sample()).put(sample())
            .put(JSONObject().put("workoutId", "bad").put("started", "invalid")
                .put("finished", "2026-10-09T20:45:00Z"))
            .put(sample("another"))
        val parsed = WatchMiniJournal.parse(rows)
        assertEquals(2, parsed.size)
        assertEquals(listOf("2026-10-09T20:00:00Z", "another"), parsed.map { it.workoutId })
    }

    @Test fun boundedListAndSetsStaySmall() {
        val rows = JSONArray()
        for (i in 0 until 50) {
            val row = sample("workout-$i")
            val hugeSets = JSONArray()
            for (n in 1..50) hugeSets.put(JSONObject().put("number", n).put("reps", "15"))
            row.put("exercises", JSONArray().put(JSONObject()
                .put("name", "Fekvőtámasz").put("sets", hugeSets)))
            rows.put(row)
        }
        val result = WatchMiniJournal.parse(rows)
        assertEquals(8, result.size)
        assertEquals(12, result.first().exercises.first().sets.size)
    }
}
