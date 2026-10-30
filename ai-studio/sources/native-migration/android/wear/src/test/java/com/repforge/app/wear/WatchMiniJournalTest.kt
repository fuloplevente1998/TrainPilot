package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WatchMiniJournalTest {
    @Test fun measurementHistoryUsesOnlyWearAndOrdersRecentWorkoutsFirst() {
        val old = sample("older").put("started", "2026-10-09T19:00:00Z")
        val recent = sample("recent").put("started", "2026-10-10T08:00:00Z")
            .put("finished", "2026-10-10T08:01:00Z")
        val noWear = sample("phone-only").put("wear", JSONObject.NULL)
        val wrongSource = sample("external").put("wear", JSONObject()
            .put("source", "samsung_health").put("totalCalories", 9))
        val parsed = WatchMiniJournal.parse(JSONArray().put(old).put(noWear)
            .put(recent).put(wrongSource))
        val history = WatchMiniJournal.measurementHistory(parsed)
        assertEquals(listOf("recent", "older"), history.map { it.workoutId })
        assertEquals("Fekvőtámasz", history.first().exercises.single().name)
        assertEquals(60, history.first().durationSeconds)
        assertEquals(49.0, parsed.first().healthConnect!!.totalCalories!!, 0.001)
        assertEquals(4, parsed.size) // Filtering history never removes saved Journal entries.
    }

    @Test fun measurementHistoryPreservesZeroMissingAndPartialMeasurements() {
        val row = sample().put("wear", JSONObject().put("source", "wear_health_services")
            .put("totalCalories", 0).put("partial", true))
        val entry = WatchMiniJournal.parse(JSONArray().put(row)).single()
        val history = WatchMiniJournal.measurementHistory(listOf(entry, entry))
        assertEquals(1, history.size)
        assertEquals(0.0, history.single().wear!!.totalCalories!!, 0.001)
        assertNull(history.single().wear!!.averageHeartRate)
        assertTrue(history.single().wear!!.partial)
    }

    @Test fun oldPhoneSnapshotIsDifferentFromAnEmptySyncedJournal() {
        val old = WatchHomeStore.parse("{\"days\":[]}")!!
        assertFalse(old.journalAvailable)
        val empty = WatchHomeStore.parse("{\"days\":[],\"journalSchema\":1,\"phoneVersion\":\"1.2.15\",\"recentWorkouts\":[]}")!!
        assertTrue(empty.journalAvailable)
        assertEquals("1.2.15", empty.phoneVersion)
        assertTrue(empty.recentWorkouts.isEmpty())
    }

    @Test fun anOlderPhoneDataItemCannotReplaceTheSyncedJournal() {
        val latest = WatchHomeStore.parse(JSONObject().put("publishedAt", 2000)
            .put("journalSchema", 1).put("phoneVersion", "1.2.15")
            .put("recentWorkouts", JSONArray().put(sample())).toString())!!
        val stale = WatchHomeStore.parse("{\"publishedAt\":1000,\"days\":[]}")!!
        assertEquals(1, WatchHomeStore.newest(latest, stale).recentWorkouts.size)
        assertEquals(1, WatchHomeStore.newest(stale, latest).recentWorkouts.size)
        assertTrue(WatchHomeStore.newest(latest, stale).journalAvailable)
    }

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
                .put("averageHeartRate", "not-a-number"))
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
