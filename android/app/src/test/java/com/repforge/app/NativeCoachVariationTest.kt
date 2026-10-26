package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCoachVariationTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private lateinit var variations: NativeCoachVariation
    private val target = CoachTarget("p", "A", "Program")
    private val clock = Instant.now()
    private val profile get() = JSONObject().put("minutes", 60).put("gear", JSONArray().put("bench"))
        .put("experience", "intermediate").put("goal", "fitness").put("activity", "office")
    @Before fun open() = runBlocking {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        HealthJournalStore.get(context).erase()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context); settings.resetAllSettingsForEraseLocal()
        settings.saveTrainingProfile(profile); settings.setActiveProgramId("p")
        db.exerciseLibraryDao().insertMissingExercises(TrainPilotDatabase.defaultExercises())
        val rx = JSONObject().put("sets", 2).put("reps", "8–15").put("loadType", "bodyweight")
        val raw = JSONObject().put("id", "p").put("name", "Program").put("builtin", false)
            .put("prescriptions", JSONObject().put("pushup", rx))
        db.programDao().upsertProgram(TrainPilotDatabase.defaultPrograms().first.first().copy(
            id = "p", builtin = false, generated = false, sourceType = "own", rawJson = raw.toString()))
        db.programDao().upsertProgramDays(listOf(ProgramDayEntity("p:A", "p", "A", "Nap", "Day", "Tag", "Zi", 0, "[\"pushup\"]")))
        importer = LegacyWebViewMigrator(context, db, settings)
        variations = NativeCoachVariation(context, db)
        for (i in 1..3) workout("w-$i", i)
    }
    @After fun close() {
        db.close()
        Dispatchers.resetMain()
    }
    private suspend fun workout(id: String, days: Int, effort: String = "good") {
        val end = clock.minus(Duration.ofDays(days.toLong()))
        val sets = JSONArray((0..1).map { i -> JSONObject().put("setId", "$id-$i").put("done", true).put("reps", "15").put("weight", 0) })
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", id).put("syncId", id).put("workoutId", id)
            .put("programId", "p").put("dayId", "A").put("started", end.minusSeconds(60).toString()).put("finished", end.toString())
            .put("exercises", JSONArray().put(JSONObject().put("id", "pushup").put("loadType", "bodyweight")
                .put("repUnit", "ism.").put("effort", effort).put("sets", sets))))
    }
    private suspend fun rejects(block: suspend () -> Unit) {
        var failure: Exception? = null
        try { block() } catch (error: Exception) { failure = error }
        assertNotNull(failure)
    }
    @Test fun stableThreeSessionsOfferReadOnlyPreviewWithoutChangingAnyProgram() = runTest(dispatcher) {
        val source = db.programDao().getProgramWithDaysById("p")
        val p = variations.preview(target, "pushup")
        assertEquals("decline-pushup", p.next.exerciseId)
        assertEquals(source, db.programDao().getProgramWithDaysById("p"))
        assertEquals(listOf("p"), db.programDao().getProgramIds())
        assertEquals("p", settings.current().activeProgramId)
    }
    @Test fun confirmationCreatesSeparateCoachProgramEvenForAnOwnSourceProgram() = runTest(dispatcher) {
        val source = db.programDao().getProgramWithDaysById("p")
        // Room's transaction executes on its own thread and must return to the real
        // main-confined execution snapshot. runTest pumps that dispatcher instead
        // of blocking Robolectric's paused main looper (CI previously hung here).
        val opened = variations.preview(target, "pushup")
        val id = withContext(Dispatchers.IO) {
            assertNotEquals(android.os.Looper.getMainLooper(), android.os.Looper.myLooper())
            variations.save(opened)
        }
        assertNotEquals("p", id)
        assertEquals(source, db.programDao().getProgramWithDaysById("p"))
        val copy = db.programDao().getProgramWithDaysById(id)!!
        assertEquals("coach", copy.program.sourceType)
        assertEquals("p", JSONObject(copy.program.rawJson).getString("parentProgramId"))
        assertEquals("decline-pushup", JSONArray(copy.days.single().exercisesJson).getString(0))
        assertEquals(0, JSONObject(copy.program.rawJson).getJSONObject("prescriptions").getJSONObject("decline-pushup").getInt("weight"))
        assertEquals("p", settings.current().activeProgramId) // Activation is explicitly separate in this UI.
        for (i in 1..3) assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("w-$i"))
    }
    @Test fun twoSessionsCannotAuthorizeVariation() = runTest(dispatcher) {
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("w-3")
        rejects { variations.preview(target, "pushup") }
    }
    @Test fun freshPainBlocksVariationAndOldPreviewApproval() = runTest(dispatcher) {
        val opened = variations.preview(target, "pushup")
        workout("w-1", 1, "pain")
        rejects { variations.preview(target, "pushup") }
        rejects { variations.save(opened) }
        assertEquals(listOf("p"), db.programDao().getProgramIds())
    }
    @Test fun changedProfileInvalidatesPreviewEvenWhenSuggestionIsStillValid() = runTest(dispatcher) {
        val opened = variations.preview(target, "pushup")
        settings.saveTrainingProfile(profile.put("minutes", 90))
        rejects { variations.save(opened) }
        assertEquals(listOf("p"), db.programDao().getProgramIds())
    }
    @Test fun missingBenchUsesPausedOriginalInsteadOfUnavailableVariation() = runTest(dispatcher) {
        settings.saveTrainingProfile(profile.put("gear", JSONArray()))
        val p = variations.preview(target, "pushup")
        assertEquals("pushup", p.next.exerciseId); assertEquals("paused", p.next.name)
    }
    @Test fun excludedCurrentMovementCannotBeProgressed() = runTest(dispatcher) {
        settings.saveTrainingProfile(profile.put("excluded", JSONArray().put("pushup")))
        rejects { variations.preview(target, "pushup") }
    }
    @Test fun sourceProgramChangeRejectsAnAlreadyOpenedPreview() = runTest(dispatcher) {
        val p = variations.preview(target, "pushup")
        val row = db.programDao().getProgramWithDaysById("p")!!.program
        db.programDao().upsertProgram(row.copy(nameHu = "Changed"))
        rejects { variations.save(p) }
        assertEquals(listOf("p"), db.programDao().getProgramIds())
    }
    @Test fun restoreFenceBlocksPreviewAndApproval() = runTest(dispatcher) {
        val p = variations.preview(target, "pushup")
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        try { rejects { variations.preview(target, "pushup") }; rejects { variations.save(p) } }
        finally { context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit() }
    }
    @Test fun canonicalVariationRecipeResetsLoadAndDoesNotInheritCompletion() {
        val rx = NativeCoachVariation.recipe(NativeCoachExerciseAdvice.Variation("plank", "longLever", 1),
            JSONObject().put("repUnit", "mp"), profile)
        assertEquals("20–40 mp", rx.getString("reps")); assertEquals(0, rx.getInt("weight"))
        assertTrue(rx.getBoolean("trial")); assertFalse(rx.has("done")); assertFalse(rx.has("actualRir"))
        val weighted = NativeCoachVariation.recipe(NativeCoachExerciseAdvice.Variation("plank", "weighted", 2),
            JSONObject().put("repUnit", "mp"), profile)
        assertEquals("single_dumbbell", weighted.getString("loadType"))
    }
}
