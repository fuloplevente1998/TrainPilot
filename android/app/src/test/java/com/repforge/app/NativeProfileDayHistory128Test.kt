package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProfileDayHistory128Test {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var store: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private lateinit var source: ProgramWithDays
    private lateinit var full: NativeProfileProgramPreview.Preview
    private val active = "practiced-active"
    private val time = Instant.now().minusSeconds(10)
    private fun profile(minutes: Int = 40) = JSONObject().put("minutes", minutes).put("experience", "intermediate")
        .put("goal", "fitness").put("gear", JSONArray(listOf("dumbbells", "barbell", "bench")))

    @Before fun open() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        store = TrainPilotSettingsStore(context); store.resetAllSettingsForEraseLocal()
        store.saveTrainingProfile(profile()); store.setActiveProgramId(active)
        db.exerciseLibraryDao().insertMissingExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        source = ProgramWithDays(programs.first { it.id == "home-basic" }, days.filter { it.programId == "home-basic" })
        db.programDao().saveProgramWithDays(source.program, source.days)
        full = NativeProfileProgramPreview.preview(source, profile(90).toString(), TrainPilotDatabase.defaultExercises(), "hu", true)
        db.programDao().saveProgramWithDays(source.program.copy(id = active, builtin = false, sourceType = "own",
            rawJson = JSONObject(source.program.rawJson).put("id", active).put("builtin", false).toString()),
            full.draft.days.mapIndexed { i, day -> ProgramDayEntity("$active:${day.id}", active, day.id,
                day.name, day.name, day.name, day.name, i, JSONArray(day.entries.map(NativeProgramEditor::exerciseId)).toString()) })
        importer = LegacyWebViewMigrator(context, db, store)
    }
    @After fun close() { db.close() }

    private suspend fun workout(id: String = "complete", minutes: Int = 30, pid: String = active, dayId: String = "A",
        complete: Boolean = true, pain: Boolean = false, end: Instant = time.minus(Duration.ofDays(1)), missingLast: Boolean = false,
        feedbackEffort: String = "") {
        val ids = full.draft.days.first { it.id == dayId }.entries.map(NativeProgramEditor::exerciseId)
            .let { if (missingLast) it.dropLast(1) else it }
        val catalog = TrainPilotDatabase.defaultExercises().associateBy { it.id }
        val exercises = JSONArray(ids.map { exId ->
            val lib = catalog.getValue(exId)
            val rx = full.prescriptions.getJSONObject(exId)
            val top = com.repforge.app.ui.coach.NativeCoachExerciseAdvice.target(
                JSONObject(lib.rawJson).put("reps", lib.defaultReps), rx).max!!.toInt()
            JSONObject().put("id", exId).put("loadType", lib.loadType).put("repUnit", lib.repUnit)
                .put("effort", if (pain) "pain" else "good").put("prescription", JSONObject(rx.toString()))
                .put("sets", JSONArray((0..1).map { i -> JSONObject().put("setId", "$id-$exId-$i")
                    .put("done", complete).put("reps", "$top").put("weight", if (lib.loadType == "bodyweight") 0 else 5) }))
        })
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", id).put("syncId", id).put("workoutId", id)
            .put("programId", pid).put("dayId", dayId).put("workout", dayId)
            .put("feedback", JSONObject().put("effort", feedbackEffort))
            .put("started", end.minusSeconds(minutes * 60L).toString()).put("finished", end.toString()).put("exercises", exercises))
    }
    private suspend fun result(minutes: Int = 40, at: Instant = time) = NativeProfileHistory128.apply(db,
        NativeProfileProgramPreview.preview(source, profile(minutes).toString(), TrainPilotDatabase.defaultExercises(), "hu"),
        at, activeProgramId = active)
    private fun retained(result: NativeProfileProgramPreview.Preview) = result.history128.getJSONObject("days").getJSONObject("A").getBoolean("retained")
    private suspend fun reject(block: suspend () -> Unit) {
        var error: Exception? = null
        try { block() } catch (failure: Exception) { error = failure }
        assertNotNull(error)
    }

    @Test fun measuredAndEstimatedFitRetainsSevenExercisesInsteadOfStaticFour() = runBlocking {
        workout()
        val before = db.programDao().getProgramWithDaysById(active)
        val p = result()
        assertTrue(retained(p)); assertEquals(7, p.draft.days.first { it.id == "A" }.entries.size)
        assertEquals("complete", p.history128.getJSONObject("days").getJSONObject("A").getString("key"))
        assertEquals(4, p.draft.days.first { it.id == "B" }.entries.size)
        assertTrue(p.estimateMinutes <= 40)
        assertEquals(before, db.programDao().getProgramWithDaysById(active))
    }
    @Test fun slowLatestCompleteDayCannotFallBackToAnOlderFastDay() = runBlocking {
        workout("older", end = time.minus(Duration.ofDays(2)))
        workout("slow", minutes = 50)
        val p = result()
        assertFalse(retained(p)); assertEquals(4, p.draft.days.first { it.id == "A" }.entries.size)
        val reduction = p.history128.getJSONArray("reductions").let { rows ->
            (0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getString("dayId") == "A" }
        }
        assertEquals(7, reduction.getInt("fromExercises")); assertEquals(4, reduction.getInt("toExercises"))
        assertEquals(14, reduction.getInt("fromSets")); assertEquals(8, reduction.getInt("toSets"))
        assertEquals("time", reduction.getString("reason"))
    }
    @Test fun measuredSpeedAloneCannotOverrideAnOverBudgetPrescriptionEstimate() = runBlocking {
        workout(minutes = 10)
        val p = result(25)
        assertFalse(retained(p)); assertEquals(3, p.draft.days.first { it.id == "A" }.entries.size)
    }
    @Test fun newPartialDayBlocksOlderCompleteDay() = runBlocking {
        workout("older", end = time.minus(Duration.ofDays(2)))
        workout("partial", complete = false)
        assertFalse(retained(result()))
    }
    @Test fun newPainfulDayBlocksOlderCompleteDay() = runBlocking {
        workout("older", end = time.minus(Duration.ofDays(2)))
        workout("pain", pain = true)
        assertFalse(retained(result()))
    }
    @Test fun workoutLevelPainFeedbackAlsoBlocksRetention() = runBlocking {
        workout("older", end = time.minus(Duration.ofDays(2)))
        workout("pain-feedback", feedbackEffort = "pain")
        assertFalse(retained(result()))
    }
    @Test fun missingExerciseInLatestDayIsNotACompleteFullDay() = runBlocking {
        workout("older", end = time.minus(Duration.ofDays(2)))
        workout("missing", missingLast = true)
        assertFalse(retained(result()))
    }
    @Test fun otherProgramAndOtherDayDoNotAuthorizeRetention() = runBlocking {
        workout("other-program", pid = "unrelated")
        workout("other-day", dayId = "B")
        assertFalse(retained(result()))
    }
    @Test fun futureAndOlderThanNinetyDaysDoNotAuthorizeRetention() = runBlocking {
        workout("old", end = time.minus(Duration.ofDays(91)))
        workout("future", end = time.plus(Duration.ofDays(1)))
        assertFalse(retained(result()))
    }
    @Test fun deletedEvidenceStopsAuthorizingRetention() = runBlocking {
        workout()
        assertTrue(retained(result()))
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("complete")
        assertFalse(retained(result()))
    }
    @Test fun tombstonedEvidenceDoesNotAuthorizeRetentionEvenWhenItsRowRemains() = runBlocking {
        workout()
        val row = db.workoutHistoryDao().getWorkoutWithDetails("complete")!!.workout
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(row.id, row.workoutKey,
            JSONArray().put(row.id).toString(), System.currentTimeMillis()))
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("complete"))
        assertFalse(retained(result()))
    }
    @Test fun tiedTimestampsPagePastThirtyTwoPartialRowsForPriorCountsOnly() = runBlocking {
        workout("a-prior")
        for (i in 0 until 45) workout("p-${i.toString().padStart(2, '0')}", complete = false)
        val p = result()
        assertFalse(retained(p))
        assertEquals(46, p.history128.getJSONObject("days").getJSONObject("A").getInt("sampled"))
    }
    @Test fun newlyArrivingWorkoutAfterPreviewTimestampInvalidatesApproval() = runBlocking {
        workout()
        val p = result()
        val before = db.programDao().getProgramIds()
        workout("arrived-later", end = time.plusSeconds(1))
        reject { NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p)) }
        assertEquals(before, db.programDao().getProgramIds())
    }
    @Test fun changingActiveProgramInvalidatesApproval() = runBlocking {
        workout()
        val p = result()
        store.setActiveProgramId("home-basic")
        reject { NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p)) }
    }
    @Test fun dayEvidenceAndReductionSurviveCanonicalBackupRoundTrip() = runBlocking {
        workout()
        workout("b-slow", dayId = "B", minutes = 50)
        val p = result()
        val id = NativeProgramEditor.save(context, db, source, p.draft, "hu", NativeProfileProgramPreview.metadata(p))
        val backup = importer.exportBackupJson(false)
        importer.importBackupJson(backup, false)
        val history = JSONObject(db.programDao().getProgramWithDaysById(id)!!.program.rawJson).getJSONObject("history128")
        assertTrue(history.getJSONObject("days").getJSONObject("A").getBoolean("retained"))
        assertEquals("complete", history.getJSONObject("days").getJSONObject("A").getString("key"))
        assertTrue(history.getJSONArray("reductions").length() > 0)
    }
    @Test fun malformedDayCountIsRejectedBeforeReplacingRoomData() = runBlocking {
        workout()
        val program = JSONObject(source.program.rawJson).put("history128", result().history128)
        program.getJSONObject("history128").getJSONObject("days").getJSONObject("A").put("sampled", "many")
        val before = db.programDao().getProgramIds()
        reject { importer.importBackupJson(JSONObject().put("programs", JSONArray().put(program)), true) }
        assertEquals(before, db.programDao().getProgramIds()); assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("complete"))
    }
    @Test fun originalMainReductionShapeWithoutNativeDayProofsRemainsValid() {
        NativeProfileHistory128.validatePortable(JSONObject().put("history128", JSONObject().put("entries", JSONObject())
            .put("reductions", JSONArray().put(JSONObject().put("dayId", "A").put("reason", "time")
                .put("fromExercises", 7).put("toExercises", 4).put("fromSets", 14).put("toSets", 8)))))
    }
    @Test fun malformedSetsAndUnknownCompletionDoNotBecomeProof() {
        for (bad in listOf(JSONObject.NULL, "", "NaN", -1)) {
            assertFalse(NativeProfileDayHistory128.complete(JSONObject().put("sets", JSONArray().put(
                JSONObject().put("done", true).put("reps", 12).put("weight", bad)))))
        }
        assertFalse(NativeProfileDayHistory128.complete(JSONObject().put("sets", JSONArray().put(
            JSONObject().put("done", "true").put("reps", 12).put("weight", 0)))))
    }
}
