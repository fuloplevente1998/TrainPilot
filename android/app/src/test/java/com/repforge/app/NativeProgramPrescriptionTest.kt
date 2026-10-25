package com.repforge.app

import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProgramPrescriptionTest {
    private fun exercise() = TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" }.copy(loadType = "total")
    private fun fields() = NativeProgramPrescription.Fields("4", "6–8", "12,5", "75")

    @Test fun effectiveFieldsUseTheSameGlobalThenInlineOrderAndPendingEditsWinOnlyExplicitFields() {
        val ex = exercise()
        val original = JSONObject().put("prescriptions", JSONObject().put(ex.id,
            JSONObject().put("sets", 2).put("rest", 100).put("rir", 3).put("legacyFlag", "keep")))
        val program = TrainPilotDatabase.defaultPrograms().first.first().copy(rawJson = original.toString())
        val entry = JSONObject().put("id", ex.id).put("sets", 3).put("variation", "paused").toString()
        val before = NativeProgramPrescription.effective(program, entry, ex, emptyMap(), 60)
        assertEquals(3, before.getInt("sets")); assertEquals(100, before.getInt("rest"))
        val patch = NativeProgramPrescription.edit(fields(), ex.loadType)
        val pending = mapOf(ex.id to NativeProgramPrescription.Edit(patch.toString(), NativeProgramPrescription.source(ex)))
        val after = NativeProgramPrescription.effective(program, entry, ex, pending, 60)
        assertEquals(4, after.getInt("sets")); assertEquals(75, after.getInt("rest"))
        assertEquals("paused", after.getString("variation")); assertEquals(3, after.getInt("rir"))
        assertEquals("keep", after.getString("legacyFlag"))
        assertEquals(2, JSONObject(program.rawJson).getJSONObject("prescriptions").getJSONObject(ex.id).getInt("sets"))
    }

    @Test fun decimalCommaBlankTrialAndBodyweightHaveExplicitDistinctMeanings() {
        val normal = NativeProgramPrescription.edit(fields(), "total")
        assertEquals(12.5, normal.getDouble("weight"), 0.0); assertFalse(normal.getBoolean("trial"))
        val trial = NativeProgramPrescription.edit(fields().copy(weight = ""), "per_hand")
        assertEquals(0.0, trial.getDouble("weight"), 0.0); assertTrue(trial.getBoolean("trial"))
        val zero = NativeProgramPrescription.edit(fields().copy(weight = "0"), "total")
        assertFalse(zero.getBoolean("trial"))
        val bodyweight = NativeProgramPrescription.edit(fields(), "bodyweight")
        assertEquals(0.0, bodyweight.getDouble("weight"), 0.0); assertFalse(bodyweight.getBoolean("trial"))
    }

    @Test fun invalidLimitsFractionsNonfiniteWeightsAndTargetsCannotBecomeAPatch() {
        val invalid = listOf(fields().copy(sets = "0"), fields().copy(sets = "11"), fields().copy(sets = "1.5"),
            fields().copy(rest = "29"), fields().copy(rest = "301"), fields().copy(reps = ""),
            fields().copy(reps = "x".repeat(41)), fields().copy(reps = "8\n10"), fields().copy(weight = "NaN"),
            fields().copy(weight = "Infinity"), fields().copy(weight = "-1"), fields().copy(weight = "10001"))
        for (value in invalid) assertThrows(IllegalArgumentException::class.java) { NativeProgramPrescription.edit(value, "total") }
        assertThrows(IllegalArgumentException::class.java) { NativeProgramPrescription.edit(fields().copy(weight = ""), "machine") }
    }

    @Test fun encodedPatchCannotOverwriteIdentityOrSmuggleInvalidTrialOrStringNumbers() {
        val good = NativeProgramPrescription.edit(fields(), "total")
        for (bad in listOf(JSONObject(good.toString()).put("id", "another"), JSONObject(good.toString()).put("sets", "4"),
            JSONObject(good.toString()).put("rest", 30.5), JSONObject(good.toString()).put("trial", true)))
            assertThrows(IllegalArgumentException::class.java) { NativeProgramPrescription.validate(bad) }
        val trial = NativeProgramPrescription.edit(fields().copy(weight = ""), "total")
        assertThrows(IllegalArgumentException::class.java) { NativeProgramPrescription.validate(trial, "bodyweight") }
    }

    @Test fun applyingToExistingInlineEntryPreservesOpaqueFieldsAndDoesNotConvertStringIds() {
        val entry = JSONObject().put("id", "goblet-squat").put("sets", 2).put("variation", "slow")
            .put("extension", JSONObject().put("future", 1)).toString()
        val patch = NativeProgramPrescription.edit(fields(), "total")
        val revised = NativeProgramPrescription.applyToInline(entry, patch) as JSONObject
        assertEquals(4, revised.getInt("sets")); assertEquals("slow", revised.getString("variation"))
        assertEquals(1, revised.getJSONObject("extension").getInt("future"))
        assertEquals(2, JSONObject(entry).getInt("sets"))
        assertEquals("goblet-squat", NativeProgramPrescription.applyToInline(JSONObject.quote("goblet-squat"), patch))
    }

}
