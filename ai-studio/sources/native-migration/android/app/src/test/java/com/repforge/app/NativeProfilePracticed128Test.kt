package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProfilePracticed128Test {
    @get:org.junit.Rule val mainDispatcherRule = NativeMainDispatcherRule()
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private lateinit var source: ProgramWithDays
    private val time = Instant.now().minusSeconds(10)
    private val exerciseId = "barbell-row"
    private fun profile() = JSONObject().put("minutes", 90).put("experience", "beginner").put("goal", "fitness")
        .put("gear", JSONArray(listOf("dumbbells", "barbell", "bench")))

    @Before fun open() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context); settings.resetAllSettingsForEraseLocal()
        settings.saveTrainingProfile(profile()); settings.setActiveProgramId("home-basic")
        db.exerciseLibraryDao().insertMissingExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        source = ProgramWithDays(programs.first { it.id == "home-basic" }, days.filter { it.programId == "home-basic" })
        db.programDao().saveProgramWithDays(source.program, source.days)
        importer = LegacyWebViewMigrator(context, db, settings)
    }
    @After fun close() { db.close() }

    private suspend fun workout(id: String = "practiced", end: Instant = time.minusSeconds(86400), done: Boolean = true,
        effort: String = "good", feedback: JSONObject = JSONObject(), unit: String = "ism.",
        load: String = "total", variation: String = "", weight: Any = 20) {
        val ex = JSONObject().put("id", exerciseId).put("loadType", load).put("repUnit", unit)
            .put("effort", effort).put("prescription", JSONObject().put("variation", variation))
            .put("sets", JSONArray((0..1).map { i -> JSONObject().put("setId", "$id-$i").put("done", done)
                .put("weight", weight).put("reps", "12") }))
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", id).put("programId", "unrelated-workout")
            .put("dayId", "custom").put("started", end.minusSeconds(600).toString()).put("finished", end.toString())
            .put("feedback", feedback).put("exercises", JSONArray().put(ex)))
    }
    private suspend fun preview(p: JSONObject = profile()) = NativeProfileHistory128.preview(db, source,
        p.toString(), "hu", time)
    private fun includes(p: NativeProfileProgramPreview.Preview) = p.draft.days.any { day ->
        day.entries.any { NativeProgramEditor.exerciseId(it) == exerciseId }
    }
    private suspend fun rejected(block: suspend () -> Unit) {
        var caught = false
        try { block() } catch (_: Exception) { caught = true }
        assertTrue("Expected stale/invalid preview to be rejected", caught)
    }

    @Test fun provenCompletionFromAnotherProgramPermitsExerciseWithoutInheritingItsWeight() = runTest(mainDispatcherRule.dispatcher) {
        assertFalse(includes(preview()))
        workout()
        val before = db.programDao().getProgramWithDaysById("home-basic")
        val p = preview()
        assertTrue(includes(p))
        assertEquals("unrelated-workout", p.history128.getJSONObject("practiced").getJSONObject(exerciseId).getString("programId"))
        assertEquals("practiced", p.history128.getJSONObject("practiced").getJSONObject(exerciseId).getString("key"))
        assertFalse(p.history128.getJSONObject("entries").has(exerciseId))
        assertTrue(p.prescriptions.getJSONObject(exerciseId).getBoolean("trial"))
        assertEquals(0.0, p.prescriptions.getJSONObject(exerciseId).getDouble("weight"), 0.0)
        assertEquals(before, db.programDao().getProgramWithDaysById("home-basic"))
    }
    @Test fun newerPartialOccurrenceBlocksOlderSuccess() = runTest(mainDispatcherRule.dispatcher) {
        workout("older", time.minusSeconds(172800)); workout("partial", done = false)
        assertFalse(includes(preview()))
    }
    @Test fun anyCurrentPainSignalBlocksOlderSuccess() = runTest(mainDispatcherRule.dispatcher) {
        workout("older", time.minusSeconds(172800))
        for ((effort, feedback) in listOf("pain" to JSONObject(), "good" to JSONObject().put("rating", "pain"),
            "good" to JSONObject().put("effort", "pain"))) {
            workout("pain", effort = effort, feedback = feedback)
            assertFalse(includes(preview()))
        }
    }
    @Test fun mismatchedUnitsLoadOrVariationCannotProveTheBaseMovement() = runTest(mainDispatcherRule.dispatcher) {
        for ((unit, load, variation) in listOf(Triple("mp", "total", ""), Triple("ism.", "per_hand", ""),
            Triple("ism.", "total", "paused"))) {
            workout(unit = unit, load = load, variation = variation)
            assertFalse(includes(preview()))
        }
    }
    @Test fun invalidOrUnknownSetValuesNeverProveExperience() = runTest(mainDispatcherRule.dispatcher) {
        for (value in listOf(JSONObject.NULL, "", "NaN", -1)) {
            workout(weight = value)
            assertFalse(includes(preview()))
        }
    }
    @Test fun oldAndFutureRecordsDoNotAuthorizeBeginnerException() = runTest(mainDispatcherRule.dispatcher) {
        workout("old", time.minus(Duration.ofDays(91))); workout("future", time.plusSeconds(86400))
        assertFalse(includes(preview()))
    }
    @Test fun deletedAndTombstonedProofsAreExcluded() = runTest(mainDispatcherRule.dispatcher) {
        workout(); assertTrue(includes(preview()))
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("practiced")
        assertFalse(includes(preview()))
        workout("marked")
        val row = db.workoutHistoryDao().getWorkoutWithDetails("marked")!!.workout
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(row.id, row.workoutKey,
            JSONArray().put(row.id).toString(), System.currentTimeMillis()))
        assertFalse(includes(preview()))
    }
    @Test fun proofDoesNotOverrideMissingEquipmentExclusionsOrAvoidedAreas() = runTest(mainDispatcherRule.dispatcher) {
        workout()
        for (p in listOf(profile().put("gear", JSONArray(listOf("dumbbells", "bench"))),
            profile().put("excluded", JSONArray().put(exerciseId)), profile().put("avoidAreas", JSONArray().put("back")))) {
            assertFalse(includes(preview(p)))
        }
    }
    @Test fun deletingEvidenceAfterOpeningPreviewRejectsSaveWithoutCreatingProgram() = runTest(mainDispatcherRule.dispatcher) {
        workout(); val p = preview(); val before = db.programDao().getProgramIds()
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("practiced")
        rejected { NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p)) }
        assertEquals(before, db.programDao().getProgramIds())
    }
    @Test fun painArrivingAfterPreviewTimestampRejectsApproval() = runTest(mainDispatcherRule.dispatcher) {
        workout(); val p = preview(); val before = db.programDao().getProgramIds()
        workout("new-pain", time.plusSeconds(1), effort = "pain")
        rejected { NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p)) }
        assertEquals(before, db.programDao().getProgramIds())
    }
    @Test fun acceptedProofSurvivesCanonicalBackupRoundTrip() = runTest(mainDispatcherRule.dispatcher) {
        workout(); val p = preview()
        val id = NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p))
        val backup = importer.exportBackupJson(false); importer.importBackupJson(backup, false)
        val history = JSONObject(db.programDao().getProgramWithDaysById(id)!!.program.rawJson).getJSONObject("history128")
        assertEquals(p.history128.getJSONObject("practiced").toString(), history.getJSONObject("practiced").toString())
    }
    @Test fun malformedPortableProofIsRejectedBeforeReplacingRoomData() = runTest(mainDispatcherRule.dispatcher) {
        workout(); val p = preview(); val before = db.programDao().getProgramIds()
        p.history128.getJSONObject("practiced").getJSONObject(exerciseId).put("evidenceHash", "invalid")
        rejected { importer.importBackupJson(JSONObject().put("programs", JSONArray().put(
            JSONObject(source.program.rawJson).put("history128", p.history128))), true) }
        assertEquals(before, db.programDao().getProgramIds())
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("practiced"))
    }
    @Test fun fabricatedEligibilityInUiMetadataIsNotTrustedAtAcceptance() = runTest(mainDispatcherRule.dispatcher) {
        val p = preview()
        p.history128.getJSONObject("practiced").put(exerciseId, JSONObject().put("programId", "unrelated-workout")
            .put("key", "fake").put("evidenceHash", "a".repeat(64)))
        val before = db.programDao().getProgramIds()
        rejected { NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p)) }
        assertEquals(before, db.programDao().getProgramIds())
    }
    @Test fun pinnedMainPracticedFixturesAgreeThroughTheActualRoomPreviewPath() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = JSONObject(requireNotNull(javaClass.getResourceAsStream("/native-practiced-main-7473.json"))
            .bufferedReader().use { it.readText() })
        assertEquals("b5567576bed9a8f93608a6bf1ed8db1dee4723f4", fixture.getString("blob"))
        val cases = fixture.getJSONArray("cases")
        for (i in 0 until cases.length()) {
            db.workoutHistoryDao().deleteAllWorkouts()
            val row = cases.getJSONObject(i)
            importer.saveWorkoutJsonToRoom(row.getJSONObject("workout"))
            val p = NativeProfileHistory128.preview(db, source, profile().toString(), "hu",
                Instant.parse(fixture.getString("asOf")))
            assertEquals(row.getString("name"), row.getBoolean("expected"), includes(p))
        }
    }
}
