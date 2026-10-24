package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProfileProgramPreviewTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var store: TrainPilotSettingsStore

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        store = TrainPilotSettingsStore(context)
    }
    @After fun close() { db.close() }

    private fun profile(minutes: Int = 60, gear: List<String> = listOf("dumbbells", "barbell")) =
        JSONObject().put("minutes", minutes).put("experience", "beginner").put("goal", "fitness")
            .put("activity", "mixed").put("gear", JSONArray().apply { gear.forEach { put(it) } })
            .put("excluded", JSONArray()).put("avoidAreas", JSONArray())

    private fun template(id: String = "home-basic"): ProgramWithDays {
        val (ps, ds) = TrainPilotDatabase.defaultPrograms()
        return ProgramWithDays(ps.first { it.id == id }, ds.filter { it.programId == id })
    }
    private fun exercises() = TrainPilotDatabase.defaultExercises()
    private fun preview(profile: JSONObject = profile(), id: String = "home-basic") =
        NativeProfileProgramPreview.preview(template(id), profile.toString(), exercises(), "hu")

    @Test fun previewCannotRunWithoutExplicitMinutesAndGear() {
        val noTime = profile().apply { remove("minutes") }
        assertThrows(IllegalArgumentException::class.java) { preview(noTime) }
        val noGear = profile().apply { remove("gear") }
        assertThrows(IllegalArgumentException::class.java) { preview(noGear) }
    }

    @Test fun basicAbUsesRealCatalogAndDoesNotMutateTemplate() {
        val source = template()
        val before = source.days.map { it.exercisesJson }
        val result = preview()
        assertEquals(2, result.draft.days.size)
        assertTrue(result.draft.days.all { it.entries.isNotEmpty() })
        val known = exercises().map { it.id }.toSet()
        assertTrue(result.draft.days.flatMap { it.entries }.all {
            NativeProgramEditor.exerciseId(it) in known
        })
        assertEquals(before, source.days.map { it.exercisesJson })
    }

    @Test fun timeConstraintCuts20MinuteDayWithoutAddingExercises() {
        val result = preview(profile(minutes = 20))
        assertTrue(result.draft.days.all { it.entries.size <= 3 })
        assertTrue(result.reductions.any { it.reason == "time" })
        assertTrue(result.prescriptions.keys().asSequence().all { result.prescriptions
            .getJSONObject(it).optInt("sets") == 2 })
    }

    @Test fun unavailableBarbellAndExplicitExclusionNeverAppear() {
        val source = profile(60, listOf("dumbbells"))
            .put("excluded", JSONArray().put("goblet-squat"))
        val result = preview(source)
        val ids = result.draft.days.flatMap { d -> d.entries.map(NativeProgramEditor::exerciseId) }
        assertFalse("goblet-squat" in ids)
        assertFalse("barbell-row" in ids)
        assertTrue(result.reductions.any { it.reason == "equipment" && it.exerciseId == "barbell-row" })
        assertTrue(result.reductions.any { it.reason == "excluded" && it.exerciseId == "goblet-squat" })
    }

    @Test fun noGearNeverAssumesDumbbellsOrBarbell() {
        val result = preview(profile(90, emptyList()))
        val ids = result.draft.days.flatMap { d -> d.entries.map(NativeProgramEditor::exerciseId) }
        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { id -> exercises().first { it.id == id }.loadType == "bodyweight" })
    }

    @Test fun muscleGoalHigherSetRuleRequiresExperienceMinutesAndNonPhysicalWork() {
        val strong = profile(75).put("goal", "muscle")
            .put("experience", "intermediate").put("activity", "office")
        val result = preview(strong)
        assertTrue(result.prescriptions.keys().asSequence().any {
            result.prescriptions.getJSONObject(it).getInt("sets") == 3
        })
        for (variant in listOf(strong.put("activity", "physical"),
            profile(45).put("goal", "muscle").put("experience", "advanced"))) {
            val short = preview(variant)
            assertTrue(short.prescriptions.keys().asSequence().all {
                short.prescriptions.getJSONObject(it).getInt("sets") == 2
            })
        }
    }

    @Test fun allNonBodyweightLoadsRemainTrialWithoutBodyStatsInference() {
        val result = preview()
        val known = exercises().associateBy { it.id }
        for (id in result.prescriptions.keys()) {
            val rx = result.prescriptions.getJSONObject(id)
            assertEquals(0, rx.getInt("weight"))
            assertEquals(known.getValue(id).loadType != "bodyweight", rx.getBoolean("trial"))
            assertEquals(2, rx.getInt("rir"))
        }
    }

    @Test fun rejectsUnknownOrUserProgramsAsProfileTemplate() {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val other = ProgramWithDays(programs.first { it.id == "gym-ab" },
            days.filter { it.programId == "gym-ab" })
        assertThrows(IllegalArgumentException::class.java) {
            NativeProfileProgramPreview.preview(other, profile().toString(), exercises(), "hu")
        }
    }

    @Test fun approvedPreviewPersistsProfileAndTrialPrescriptionsButLeavesBuiltinUntouched() = runBlocking {
        db.exerciseLibraryDao().insertMissingExercises(exercises())
        val source = template()
        db.programDao().saveProgramWithDays(source.program, source.days)
        val inputs = profile()
        store.saveTrainingProfile(inputs)
        val p = NativeProfileProgramPreview.preview(source, inputs.toString(), exercises(), "hu")
        val id = NativeProgramEditor.save(context, db, source, p.draft, "hu",
            NativeProfileProgramPreview.metadata(p))
        assertNotEquals(source.program.id, id)
        val created = db.programDao().getProgramWithDaysById(id)!!
        assertEquals("profile-template", created.program.sourceType)
        assertEquals(2, created.program.templateVersion)
        assertEquals("home-basic", created.program.sourceTemplateId)
        val raw = JSONObject(created.program.rawJson)
        assertTrue(raw.getJSONObject("prescriptions").keys().hasNext())
        assertEquals(60, raw.getJSONObject("profileSnapshot").getInt("minutes"))
        assertTrue(db.programDao().getProgramWithDaysById("home-basic")!!.program.builtin)
        assertEquals("home-basic", store.current().activeProgramId)
    }

    @Test fun staleProfileStopsGenerationBeforeAnyProgramIsWritten() = runBlocking {
        db.exerciseLibraryDao().insertMissingExercises(exercises())
        val source = template()
        db.programDao().saveProgramWithDays(source.program, source.days)
        val original = profile()
        store.saveTrainingProfile(original)
        val p = preview(original)
        store.saveTrainingProfile(profile(minutes = 30))
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.save(context, db, source, p.draft, "hu",
                NativeProfileProgramPreview.metadata(p))
        } }
        assertEquals(listOf("home-basic"), db.programDao().getProgramIds())
    }
}
