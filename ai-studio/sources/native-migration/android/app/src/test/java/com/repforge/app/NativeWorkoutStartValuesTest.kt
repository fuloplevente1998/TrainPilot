package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class NativeWorkoutStartValuesTest {
    private val now = Instant.parse("2026-10-23T12:00:00Z").toEpochMilli()
    private fun exercise() = JSONObject("""{"id":"press","loadType":"dumbbell","repUnit":"ism.","measurementType":"reps"}""")
    private fun history(sets: String) = JSONObject("""{"started":"2026-10-22T10:00:00Z","finished":"2026-10-22T11:00:00Z",
        "programId":"another-program","dayId":"other-day","exercises":[]} """)
        .put("exercises", JSONArray().put(exercise().put("sets", JSONArray(sets))))

    @Test fun loadsEachPositionFromLatestValidPerformanceWithoutCopyingCompletionOrMeasurements() {
        val sets = NativeWorkoutStartValues.blankSets(2, 5.0)
        val identity = sets.getJSONObject(0).getString("setId")
        NativeWorkoutStartValues.prefill(exercise(), sets, history("""[
            {"done":true,"weight":"12,5","reps":"008","actualRir":2,"setId":"old","gpsRouteKey193":"private"},
            {"done":false,"weight":99,"reps":"99"}]"""), now)
        NativeWorkoutStartValues.prefill(exercise(), sets, history("""[
            {"done":true,"weight":8,"reps":"10"},{"done":true,"weight":9,"reps":"9"}]"""), now)
        val first = sets.getJSONObject(0)
        assertEquals(12.5, first.getDouble("weight"), 0.0)
        assertEquals("008", first.getString("reps"))
        assertEquals(9.0, sets.getJSONObject(1).getDouble("weight"), 0.0)
        assertEquals(identity, first.getString("setId"))
        assertFalse(first.getBoolean("done"))
        for (key in listOf("actualRir", "gpsRouteKey193", "leftSeconds", "distanceMeters")) assertFalse(first.has(key))
        assertTrue(first.has("previousValues228"))
    }

    @Test fun rejectsInvalidDeletedFutureAndAmbiguousHistory() {
        val valid = history("""[{"done":true,"weight":10,"reps":"8"}]""")
        val invalid = listOf(
            JSONObject(valid.toString()).put("deleted", true),
            JSONObject(valid.toString()).put("deletedAt", "2026-10-22"),
            JSONObject(valid.toString()).put("cancelled", true),
            JSONObject(valid.toString()).put("status", "cancelled"),
            JSONObject(valid.toString()).put("finished", "2026-10-24T11:00:00Z"),
            JSONObject(valid.toString()).put("started", "bad"),
            JSONObject(valid.toString()).put("finished", valid.getString("started")),
            JSONObject(valid.toString()).also { it.getJSONArray("exercises").put(it.getJSONArray("exercises").getJSONObject(0)) }
        )
        for (row in invalid) {
            val sets = NativeWorkoutStartValues.blankSets(1, 5.0)
            NativeWorkoutStartValues.prefill(exercise(), sets, row, now)
            assertEquals("", sets.getJSONObject(0).getString("reps"))
            assertEquals(5.0, sets.getJSONObject(0).getDouble("weight"), 0.0)
        }
    }

    @Test fun timedGpsAndBilateralValuesAreNeverPrefilled() {
        for (unit in listOf("mp", "mp/oldal", "km")) {
            val target = exercise().put("repUnit", unit)
            val sets = NativeWorkoutStartValues.blankSets(1, 0.0)
            NativeWorkoutStartValues.prefill(target, sets, history("""[{"done":true,"weight":0,"reps":"45"}]"""), now)
            assertFalse(sets.getJSONObject(0).has("previousValues228"))
        }
        for (extra in listOf("\"leftSeconds\":1", "\"rightSeconds\":1", "\"distanceMeters\":1")) {
            val sets = NativeWorkoutStartValues.blankSets(1, 5.0)
            NativeWorkoutStartValues.prefill(exercise(), sets, history("""[{"done":true,"weight":10,"reps":"8",$extra}]"""), now)
            assertFalse(sets.getJSONObject(0).has("previousValues228"))
        }
    }

    @Test fun invalidValuesAndChangedLoadTypeDoNotTransfer() {
        for (row in listOf("""{"done":true,"weight":-1,"reps":"8"}""", """{"done":true,"weight":10,"reps":"8–12"}""",
            """{"done":true,"weight":10,"reps":"0"}""", """{"done":true,"weight":100001,"reps":"8"}""")) {
            val sets = NativeWorkoutStartValues.blankSets(1, 5.0)
            NativeWorkoutStartValues.prefill(exercise(), sets, history("[$row]"), now)
            assertFalse(sets.getJSONObject(0).has("previousValues228"))
        }
        val sets = NativeWorkoutStartValues.blankSets(1, 5.0)
        NativeWorkoutStartValues.prefill(exercise().put("loadType", "barbell"), sets,
            history("""[{"done":true,"weight":10,"reps":"8"}]"""), now)
        assertEquals(5.0, sets.getJSONObject(0).getDouble("weight"), 0.0)
    }

    @Test fun everyNewSetHasItsOwnStableIdentity() {
        val a = NativeWorkoutStartValues.blankSets(3, 0.0)
        val b = NativeWorkoutStartValues.blankSets(3, 0.0)
        val ids = listOf(a, b).flatMap { rows -> (0 until rows.length()).map { rows.getJSONObject(it).getString("setId") } }
        assertEquals(6, ids.distinct().size)
    }
}
