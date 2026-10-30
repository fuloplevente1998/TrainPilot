package com.repforge

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LegacyMigrationAndIntegrityTest {

    @Test
    fun testLegacyWorkoutJsonParsingAndConversion() {
        val legacyJson = """
        [
            {
                "id": "legacy_101",
                "title": "Mell-Tricepsz Web",
                "duration": 3600,
                "timestamp": 1700000000000,
                "exercises": [
                    {
                        "name": "Fekvenyomás",
                        "sets": [
                            {"reps": 10, "weight": 80.0, "rir": 2, "completed": true},
                            {"reps": 8, "weight": 85.0, "rir": 1, "completed": true}
                        ]
                    }
                ]
            }
        ]
        """.trimIndent()

        val jsonArray = JSONArray(legacyJson)
        assertEquals(1, jsonArray.length())

        val workoutObj = jsonArray.getJSONObject(0)
        assertEquals("Mell-Tricepsz Web", workoutObj.getString("title"))
        assertEquals(3600L, workoutObj.getLong("duration"))

        val exercises = workoutObj.getJSONArray("exercises")
        assertEquals(1, exercises.length())

        val bench = exercises.getJSONObject(0)
        assertEquals("Fekvenyomás", bench.getString("name"))

        val sets = bench.getJSONArray("sets")
        assertEquals(2, sets.length())

        val set1 = sets.getJSONObject(0)
        assertEquals(10, set1.getInt("reps"))
        assertEquals(80.0, set1.getDouble("weight"), 0.001)
        assertEquals(2, set1.getInt("rir"))
        assertTrue(set1.getBoolean("completed"))
    }

    @Test
    fun testOutboxSpoolSerializationIntegrity() {
        val action = JSONObject().apply {
            put("actionType", "SET_COMPLETED")
            put("setId", 402L)
            put("weight", 110.0)
            put("reps", 5)
            put("rir", 0)
            put("timestamp", 1710000000000L)
        }

        val serialized = action.toString()
        val deserialized = JSONObject(serialized)

        assertEquals("SET_COMPLETED", deserialized.getString("actionType"))
        assertEquals(402L, deserialized.getLong("setId"))
        assertEquals(110.0, deserialized.getDouble("weight"), 0.001)
        assertEquals(5, deserialized.getInt("reps"))
        assertEquals(0, deserialized.getInt("rir"))
    }

    @Test
    fun testOwnershipTransitionProtocolSerialization() {
        val handoffPayload = JSONObject().apply {
            put("action", "HANDOFF_REQUEST")
            put("source", "PHONE")
            put("target", "WATCH")
            put("sequence", 42L)
            put("workoutId", 1001L)
        }

        val bytes = handoffPayload.toString().toByteArray(Charsets.UTF_8)
        val decoded = JSONObject(String(bytes, Charsets.UTF_8))

        assertEquals("HANDOFF_REQUEST", decoded.getString("action"))
        assertEquals("PHONE", decoded.getString("source"))
        assertEquals("WATCH", decoded.getString("target"))
        assertEquals(42L, decoded.getLong("sequence"))
    }
}
