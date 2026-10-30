package com.repforge.app

import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Distinguish invalid exclusions from a valid but genuinely unfillable A/B plan. */
class NativePlannerExclusionSafetyTest {
    private fun caseProfile(name: String): JSONObject {
        val stream = requireNotNull(javaClass.getResourceAsStream("/native-planner-cases-main-7473.json"))
        val cases = JSONObject(stream.bufferedReader().use { it.readText() }).getJSONArray("cases")
        return (0 until cases.length()).map { cases.getJSONObject(it) }
            .first { it.getString("name") == name }.getJSONObject("profile")
    }

    private fun preview(profile: JSONObject): NativeProfileProgramPreview.Preview {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val id = profile.getString("templateId")
        val source = ProgramWithDays(programs.first { it.id == id }, days.filter { it.programId == id })
        return NativeProfileProgramPreview.preview(
            source, profile.toString(), TrainPilotDatabase.defaultExercises(), "en"
        )
    }

    @Test fun rejectsUnknownExcludedIdsEvenWhenSafeSubstitutesExist() {
        val profile = caseProfile("main-plan-7")
        val error = assertThrows(IllegalArgumentException::class.java) { preview(profile) }
        assertEquals("Invalid exercise exclusion", error.message)
        // The same main fixture succeeds without the two nonexistent IDs.
        profile.put("excluded", JSONArray(listOf("pushup", "modified-pushup", "incline-pushup")))
        val valid = preview(profile)
        assertEquals(setOf("A", "B"), valid.draft.days.map { it.id }.toSet())
        assertTrue(valid.draft.days.all { it.entries.size >= 3 })
        assertTrue(valid.estimateMinutes <= profile.getInt("minutes"))
    }

    @Test fun rejectsAnUnfillableProfileWithOnlyRealExerciseIds() {
        val profile = caseProfile("main-plan-4")
        // The full catalog contains 106 IDs, while valid profiles allow at most 100.
        // Exclude all currently eligible movements, so the generator rather than the
        // profile array-size validator must reject this genuinely unfillable plan.
        val eligibleIds = TrainPilotDatabase.defaultExercises()
            .filter { NativeProfileProgramPreview.variationRejection(it, profile).isEmpty() }.map { it.id }
        assertTrue(eligibleIds.isNotEmpty()); assertTrue(eligibleIds.size <= 100)
        profile.put("excluded", JSONArray(eligibleIds))
        NativeTrainingProfile.verified(profile)
        val error = assertThrows(IllegalArgumentException::class.java) { preview(profile) }
        assertTrue(error.message.orEmpty().contains("Fewer than three eligible exercises"))
    }

    @Test fun validExclusionStillGetsSafeSubstitutionOnDayB() {
        val profile = caseProfile("main-plan-4")
        profile.put("excluded", JSONArray(listOf("pushup")))
        val result = preview(profile)
        val ids = result.draft.days.flatMap { day ->
            day.entries.map(NativeProgramEditor::exerciseId)
        }
        assertFalse(ids.contains("pushup"))
        assertTrue(result.reductions.any { it.dayId == "B" && it.exerciseId == "pushup" &&
            it.reason.startsWith("substitution:") })
        assertEquals(setOf("A", "B"), result.draft.days.map { it.id }.toSet())
        assertTrue(result.draft.days.all { it.entries.size >= 3 })
        assertTrue(result.estimateMinutes <= profile.getInt("minutes"))
    }
}
