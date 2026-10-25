package com.repforge.app

import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Reference days come from the final main generator, with empty synthetic history. */
@RunWith(Parameterized::class)
class NativePlannerTemplateParityTest(private val name: String, private val data: JSONObject) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): List<Array<Any>> {
            val input = requireNotNull(NativePlannerTemplateParityTest::class.java.getResourceAsStream("/native-planner-cases-main-7473.json"))
            val root = JSONObject(input.bufferedReader().use { it.readText() })
            require(root.getJSONObject("source").getString("appBlob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
            val cases = root.getJSONArray("cases")
            return (0 until cases.length()).map { cases.getJSONObject(it).let { row -> arrayOf<Any>(row.getString("name"), row) } }
        }
    }
    @Test fun preservesFinalTemplateOrderEligibilitySubstitutionsAndEstimate() {
        val profile = data.getJSONObject("profile")
        val id = profile.getString("templateId")
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val source = ProgramWithDays(programs.first { it.id == id }, days.filter { it.programId == id })
        if (data.optBoolean("rejected")) {
            assertThrows(IllegalArgumentException::class.java) {
                NativeProfileProgramPreview.preview(source, profile.toString(), TrainPilotDatabase.defaultExercises(), "en")
            }
            return
        }
        val result = NativeProfileProgramPreview.preview(source, profile.toString(), TrainPilotDatabase.defaultExercises(), "en")
        val expected = data.getJSONArray("days")
        assertEquals(expected.length(), result.draft.days.size)
        for (i in 0 until expected.length()) {
            val day = expected.getJSONObject(i)
            assertEquals(day.getString("id"), result.draft.days[i].id)
            val ids = day.getJSONArray("exercises")
            assertEquals((0 until ids.length()).map { ids.getString(it) }, result.draft.days[i].entries.map(NativeProgramEditor::exerciseId))
        }
        assertEquals(data.getInt("estimate"), result.estimateMinutes)
    }
}
