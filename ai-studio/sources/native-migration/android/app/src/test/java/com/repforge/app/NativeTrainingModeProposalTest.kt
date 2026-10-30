package com.repforge.app

import com.repforge.app.ui.workout.NativeTrainingModeProposal
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeTrainingModeProposalTest {
    private fun frame() = JSONObject("""{
        "active":true,"timerOwner":"phone","nativeEditorProjection":1,
        "workoutId":"workout-1","generation":2,"executionRevision":10,
        "ownerEpoch193":5,"programId":"p","dayId":"A",
        "exercises":[{"id":"press","loadType":"total","repUnit":"ism.","measurementType":"reps",
            "sets":[
              {"setId":"s1","weight":10,"reps":"8","done":false,
               "previousValues228":{"weight":10,"reps":"8"}},
              {"setId":"s2","weight":10,"reps":"8","done":false,
               "previousValues228":{"weight":10,"reps":"8"}}
            ]}]
    }""")

    @Test fun validProposalChangesOnlyUntouchedSetWeights() {
        val old = frame()
        val ticket = NativeTrainingModeProposal.ticket(old, "press", 12.5, "matching-history", "settings-v1")!!
        val updated = NativeTrainingModeProposal.editedFrame(old, ticket)
        val sets = updated.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertEquals(12.5, sets.getJSONObject(0).getDouble("weight"), .0)
        assertEquals("8", sets.getJSONObject(1).getString("reps"))
        assertFalse(sets.getJSONObject(1).getBoolean("done"))
        assertEquals(10.0, old.getJSONArray("exercises").getJSONObject(0)
            .getJSONArray("sets").getJSONObject(0).getDouble("weight"), .0)
    }

    @Test fun ownerGenerationRevisionOrFrameChangeBlocksStaleApproval() {
        val base = frame()
        val ticket = NativeTrainingModeProposal.ticket(base, "press", 12.5, "source", "{}")!!
        for (key in listOf("ownerEpoch193", "generation", "executionRevision")) {
            val stale = JSONObject(base.toString()).put(key, base.getLong(key) + 1)
            assertFalse(NativeTrainingModeProposal.permitted(stale, ticket))
        }
        assertFalse(NativeTrainingModeProposal.permitted(
            JSONObject(base.toString()).put("workoutId", "new-workout"), ticket))
        assertFalse(NativeTrainingModeProposal.permitted(
            JSONObject(base.toString()).put("workoutPaused", true), ticket))
        assertFalse(NativeTrainingModeProposal.permitted(
            JSONObject(base.toString()).put("timerOwner", "watch"), ticket))
    }

    @Test fun completedManuallyChangedOrMeasuredTargetsAreBlocked() {
        val base = frame()
        val sets = base.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        sets.getJSONObject(0).put("done", true)
        assertNull(NativeTrainingModeProposal.ticket(base, "press", 12.5, "source", null))
        sets.getJSONObject(0).put("done", false).put("weight", 11.0)
        assertNull(NativeTrainingModeProposal.ticket(base, "press", 12.5, "source", null))
        sets.getJSONObject(0).put("weight", 10).put("reps", "9")
        assertNull(NativeTrainingModeProposal.ticket(base, "press", 12.5, "source", null))
        sets.getJSONObject(0).put("reps", "8").put("distanceMeters", 1000)
        assertNull(NativeTrainingModeProposal.ticket(base, "press", 12.5, "source", null))
    }

    @Test fun wrongExerciseBodyweightTimeDistanceOrInvalidWeightCannotApply() {
        val base = frame()
        assertFalse(NativeTrainingModeProposal.eligible(base, "different", 12.5))
        assertFalse(NativeTrainingModeProposal.eligible(base, "press", Double.NaN))
        val exercise = base.getJSONArray("exercises").getJSONObject(0)
        exercise.put("loadType", "bodyweight")
        assertFalse(NativeTrainingModeProposal.eligible(base, "press", 12.5))
        exercise.put("loadType", "total").put("measurementType", "distance")
        assertFalse(NativeTrainingModeProposal.eligible(base, "press", 12.5))
    }
}
