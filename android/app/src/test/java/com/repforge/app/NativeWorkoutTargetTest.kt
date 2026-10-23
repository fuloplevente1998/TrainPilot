package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutTargetTest {
    private fun frame() = JSONObject("""{"workoutId":"w","generation":7,"exercises":[
        {"id":"plank","sets":[{"setId":"a","set":1,"done":false},{"setId":"b","set":2,"done":false}]},
        {"id":"plank","sets":[{"setId":"c","set":1,"done":false}]}]}""")
    private fun target(id: String) = NativeWorkoutTarget("w", 7, "plank", id)

    @Test fun repeatedExerciseUsesSetIdentityAfterExerciseAndSetReordering() {
        val frame = frame()
        val old = frame.getJSONArray("exercises")
        val repeated = old.getJSONObject(1)
        val first = old.getJSONObject(0)
        first.put("sets", JSONArray().put(first.getJSONArray("sets").getJSONObject(1)).put(first.getJSONArray("sets").getJSONObject(0)))
        frame.put("exercises", JSONArray().put(repeated).put(first))
        val resolved = target("a").resolve(frame)
        assertEquals(1, resolved.exerciseIndex)
        resolved.set.put("reps", "17")
        assertFalse(repeated.getJSONArray("sets").getJSONObject(0).has("reps"))
        assertEquals("17", first.getJSONArray("sets").getJSONObject(1).getString("reps"))
    }

    @Test fun deletedControlCannotRetargetReplacementAtSamePosition() {
        val frame = frame()
        val sets = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        sets.remove(0)
        target("b").appendSet(frame)
        assertThrows(IllegalStateException::class.java) { target("a").resolve(frame) }
        assertEquals(2, sets.length())
        assertNotEquals("a", sets.getJSONObject(1).getString("setId"))
    }

    @Test fun removedAndReaddedSetAlwaysGetsFreshIdentityAndNoOldMeasurements() {
        val frame = frame()
        val sets = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        target("a").appendSet(frame)
        val firstId = sets.getJSONObject(2).getString("setId")
        sets.remove(2)
        sets.getJSONObject(1).put("done", true).put("actualRir", 0).put("leftSeconds", 30).put("rightSeconds", 30)
        target("a").appendSet(frame)
        val fresh = sets.getJSONObject(2)
        assertNotEquals(firstId, fresh.getString("setId"))
        assertFalse(fresh.getBoolean("done"))
        assertFalse(fresh.has("actualRir"))
        assertEquals("", fresh.getString("leftSeconds"))
        assertEquals("", fresh.getString("rightSeconds"))
        assertEquals("", fresh.getString("reps"))
    }

    @Test fun oldWorkoutOrGenerationCannotUseAReusedImportedSetId() {
        val frame = frame()
        assertThrows(IllegalStateException::class.java) { target("a").resolve(JSONObject(frame.toString()).put("workoutId", "new")) }
        assertThrows(IllegalStateException::class.java) { target("a").resolve(JSONObject(frame.toString()).put("generation", 8)) }
    }

    @Test fun ambiguousOrReassignedSetIdentityIsRejected() {
        val frame = frame()
        frame.getJSONArray("exercises").getJSONObject(1).getJSONArray("sets").getJSONObject(0).put("setId", "a")
        assertThrows(IllegalStateException::class.java) { target("a").resolve(frame) }
        val reassigned = frame().apply { getJSONArray("exercises").getJSONObject(0).put("id", "other") }
        assertThrows(IllegalStateException::class.java) { target("a").resolve(reassigned) }
    }
}
