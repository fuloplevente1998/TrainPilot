package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
    @get:org.junit.Rule val mainDispatcherRule = NativeMainDispatcherRule()
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
        assertTrue(result.reductions.any { it.exerciseId == "barbell-row" })
        assertTrue(result.reductions.any { it.exerciseId == "goblet-squat" })
    }

    @Test fun noGearNeverAssumesDumbbellsOrBarbell() {
        val result = preview(profile(90, emptyList()))
        val ids = result.draft.days.flatMap { d -> d.entries.map(NativeProgramEditor::exerciseId) }
        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { id -> exercises().first { it.id == id }.loadType == "bodyweight" })
    }
    @Test fun timeBudgetIsAnEstimateOfTheGeneratedDaysRatherThanTheInputMinutes() {
        val result = preview(profile(90, listOf("dumbbells", "barbell", "bench")).put("experience", "intermediate"))
        assertTrue(result.estimateMinutes in 1 until 90)
        assertEquals(NativeProfileProgramPreview.estimatedMinutes(result.draft.days, result.prescriptions), result.estimateMinutes)
    }
    @Test fun timeAdaptationReducesSetsWithoutChangingEarnedLoadOrInputPreview() {
        val base = preview(profile(20))
        val recipes = JSONObject(base.prescriptions.toString())
        for (id in recipes.keys()) recipes.getJSONObject(id).put("sets", 3).put("weight", 7.5).put("trial", false)
        val opened = base.copy(prescriptions = recipes)
        val fitted = NativeProfileProgramPreview.fitTime(opened)
        assertTrue(fitted.estimateMinutes <= 20)
        assertTrue(fitted.reductions.any { it.reason == "time-sets" })
        assertTrue(recipes.keys().asSequence().all { recipes.getJSONObject(it).getInt("sets") == 3 })
        for (id in fitted.prescriptions.keys()) assertEquals(7.5, fitted.prescriptions.getJSONObject(id).getDouble("weight"), .0001)
        assertEquals(fitted.draft.days.sumOf { day -> day.entries.sumOf {
            fitted.prescriptions.getJSONObject(NativeProgramEditor.exerciseId(it)).getInt("sets")
        } }, fitted.workSets)
    }
    @Test fun fewerThanThreeEligibleMovementsRejectsTheWholePreview() {
        val onlyOne = exercises().filter { it.id == "pushup" }
        assertThrows(IllegalArgumentException::class.java) {
            NativeProfileProgramPreview.preview(template(), profile(90, emptyList()).toString(), onlyOne, "hu")
        }
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
            assertTrue(rx.getBoolean("trial"))
            assertEquals(known.getValue(id).loadType != "bodyweight", NativeTrainingPrescription.needsLoad(rx))
            assertEquals(2, rx.getInt("rir"))
        }
    }

    @Test fun bDayStartsWithCompoundAndPushupThirdWhenEquipmentExists() {
        val result = preview(profile(90, listOf("dumbbells", "barbell", "bench")).put("experience", "intermediate"))
        val b = result.draft.days.single { it.id == "B" }
        assertEquals(listOf("bulgarian-split-squat", "barbell-row", "pushup"),
            b.entries.take(3).map(NativeProgramEditor::exerciseId))
    }

    @Test fun level2DeclineVariationRequiresExplicitBench() {
        val withoutBench = preview(profile(90).put("experience", "intermediate"), "home-level2")
        val b = withoutBench.draft.days.single { it.id == "B" }
        assertTrue(b.entries.none { NativeProgramEditor.exerciseId(it) == "decline-pushup" })
        val withBench = preview(profile(90, listOf("dumbbells", "barbell", "bench"))
            .put("experience", "intermediate"), "home-level2")
        assertEquals("decline-pushup",
            NativeProgramEditor.exerciseId(withBench.draft.days.single { it.id == "B" }.entries[2]))
    }

    @Test fun beginnerNeverGetsExplicitlyUnsafeIntermediateExercises() {
        val result = preview(profile(90))
        val ids = result.draft.days.flatMap { day -> day.entries.map(NativeProgramEditor::exerciseId) }
        assertFalse("bulgarian-split-squat" in ids)
        assertFalse("barbell-row" in ids)
        assertTrue(result.reductions.any { it.exerciseId in setOf("bulgarian-split-squat", "barbell-row") })
    }
    @Test fun explicitLevel2UsesNewTargetsAndEquipmentBoundWeightedCore() {
        val result = preview(profile(90).put("experience", "intermediate"), "home-level2")
        assertEquals("6–10", result.prescriptions.getJSONObject("db-floor-press").getString("reps"))
        val core = result.prescriptions.getJSONObject("side-plank")
        assertEquals("weighted", core.getString("variation"))
        assertEquals("single_dumbbell", core.getString("loadType"))
        assertEquals("20–40 mp", core.getString("reps"))
        assertTrue(NativeTrainingPrescription.needsLoad(core))
        // Without a bench the final main substitutes a different eligible horizontal push.
        val push = result.draft.days.single { it.id == "B" }.entries.map(NativeProgramEditor::exerciseId)
            .first { it in setOf("pushup", "diamond-pushup", "pseudo-planche-pushup", "modified-pushup", "incline-pushup", "db-floor-press") }
        assertTrue(result.prescriptions.has(push))
    }
    @Test fun level2WithoutDumbbellsDoesNotAddExternalLoadToBodyweightCore() {
        val result = preview(profile(90, listOf("barbell")).put("experience", "intermediate"), "home-level2")
        val core = result.prescriptions.getJSONObject("side-plank")
        assertEquals("bodyweight", core.getString("loadType"))
        assertFalse(core.has("variation")); assertFalse(NativeTrainingPrescription.needsLoad(core))
    }
    @Test fun beginnerSelectingLevel2DoesNotReceiveIntermediateWeightedCore() {
        val result = preview(profile(90), "home-level2")
        assertTrue(result.prescriptions.keys().asSequence().none {
            result.prescriptions.getJSONObject(it).optString("variation") == "weighted"
        })
    }

    @Test fun replacementVariationCannotBypassBeginnerEligibility() {
        val catalog = exercises().map { ex -> if (ex.id == "decline-pushup")
            ex.copy(rawJson = JSONObject(ex.rawJson).put("beginnerSafe", false).toString()) else ex }
        val result = NativeProfileProgramPreview.preview(template("home-level2"),
            profile(90, listOf("dumbbells", "barbell", "bench")).toString(), catalog, "hu")
        val selected = result.draft.days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }
        assertTrue(result.reductions.any { it.exerciseId == "decline-pushup" })
        assertFalse("decline-pushup" in selected)
    }

    @Test fun replacementVariationCannotBypassAvoidedMuscleFilter() {
        val catalog = exercises().map { ex -> if (ex.id == "decline-pushup")
            ex.copy(primaryMuscle = "legs", rawJson = JSONObject(ex.rawJson).put("movementPattern", "lunge").toString()) else ex }
        val config = profile(90, listOf("dumbbells", "barbell", "bench"))
            .put("experience", "intermediate").put("avoidAreas", JSONArray().put("legs"))
        val result = NativeProfileProgramPreview.preview(template("home-level2"), config.toString(), catalog, "hu")
        val selected = result.draft.days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }
        assertTrue(result.reductions.any { it.exerciseId == "decline-pushup" })
        assertFalse("decline-pushup" in selected)
    }

    @Test fun catalogChangeAfterOpeningPreviewRejectsApprovalWithoutCreatingProgram() = runTest(mainDispatcherRule.dispatcher) {
        db.exerciseLibraryDao().insertMissingExercises(exercises())
        val source = template()
        db.programDao().saveProgramWithDays(source.program, source.days)
        val config = profile(90)
        store.saveTrainingProfile(config)
        val opened = preview(config)
        val chosen = opened.draft.days.first().entries.first().let(NativeProgramEditor::exerciseId)
        val exercise = db.exerciseLibraryDao().getExerciseById(chosen)!!
        db.exerciseLibraryDao().upsertExercise(exercise.copy(rawJson =
            JSONObject(exercise.rawJson).put("beginnerSafe", false).toString()))
        val before = db.programDao().getProgramIds()
        var rejected = false
        try { NativeProgramEditor.save(context, db, source, opened.draft, "hu", NativeProfileProgramPreview.metadata(opened)) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(before, db.programDao().getProgramIds())
        assertEquals(source, db.programDao().getProgramWithDaysById(source.program.id))
    }

    @Test fun alteredPrescriptionWithoutCanonicalEvidenceCannotBeApproved() = runTest(mainDispatcherRule.dispatcher) {
        db.exerciseLibraryDao().insertMissingExercises(exercises())
        val source = template()
        db.programDao().saveProgramWithDays(source.program, source.days)
        val config = profile(90)
        store.saveTrainingProfile(config)
        val opened = preview(config)
        val metadata = NativeProfileProgramPreview.metadata(opened)
        val prescriptions = metadata.getJSONObject("prescriptions")
        val key = prescriptions.keys().asSequence().first { prescriptions.getJSONObject(it).optBoolean("trial") }
        prescriptions.getJSONObject(key).put("weight", 50).put("trial", false)
        val before = db.programDao().getProgramIds()
        var rejected = false
        try { NativeProgramEditor.save(context, db, source, opened.draft, "hu", metadata) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(before, db.programDao().getProgramIds())
    }

    @Test fun avoidedShoulderBlocksShoulderExercisesAndUnknownPainAreaFailsClosed() {
        val p = profile(90).put("avoidAreas", JSONArray().put("left-shoulder"))
        val result = preview(p)
        val chosen = result.draft.days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }.toSet()
        assertFalse("db-ohp" in chosen)
        assertFalse("lateral-raise" in chosen)
        val unknown = profile(90).put("avoidAreas", JSONArray().put("unrecognized-body-part"))
        assertThrows(IllegalArgumentException::class.java) { preview(unknown) }
    }

    @Test fun rejectsUnknownOrUserProgramsAsProfileTemplate() {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val other = ProgramWithDays(programs.first { it.id == "gym-ab" },
            days.filter { it.programId == "gym-ab" })
        assertThrows(IllegalArgumentException::class.java) {
            NativeProfileProgramPreview.preview(other, profile().toString(), exercises(), "hu")
        }
    }

    @Test fun approvedPreviewPersistsProfileAndTrialPrescriptionsButLeavesBuiltinUntouched() = runTest(mainDispatcherRule.dispatcher) {
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

    @Test fun staleProfileStopsGenerationBeforeAnyProgramIsWritten() = runTest(mainDispatcherRule.dispatcher) {
        db.exerciseLibraryDao().insertMissingExercises(exercises())
        val source = template()
        db.programDao().saveProgramWithDays(source.program, source.days)
        val original = profile()
        store.saveTrainingProfile(original)
        val p = preview(original)
        store.saveTrainingProfile(profile(minutes = 30))
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.save(context, db, source, p.draft, "hu",
                NativeProfileProgramPreview.metadata(p))
        }
        assertEquals(listOf("home-basic"), db.programDao().getProgramIds())
    }
}
