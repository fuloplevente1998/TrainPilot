package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProgramActionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        android.util.AtomicFile(java.io.File(context.noBackupFilesDir, "ownership193.json")).delete()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val original = programs.single { it.id == "home-basic" }
        db.programDao().saveProgramWithDays(original, days.filter { it.programId == original.id })
    }

    @After fun cleanup() {
        db.close()
        ActiveWorkoutStore.clear(context)
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private suspend fun factory(): ProgramWithDays =
        db.programDao().getProgramWithDaysById("home-basic")!!

    private suspend fun copy(token: String = "factory-copy-123"): ProgramWithDays {
        val source = factory()
        val id = NativeProgramEditor.save(context, db, source, NativeProgramEditor.from(source, "hu"),
            "hu", actionToken = token)
        return db.programDao().getProgramWithDaysById(id)!!
    }

    @Test fun repeatCopyRequestReusesItsOwnIdentityAndNeverActivatesSourceOrCreatesWorkoutState() = runTest(dispatcher) {
        val before = factory()
        val copy = copy()
        val second = copy()
        assertEquals(copy.program.id, second.program.id)
        assertNotEquals(before.program.id, copy.program.id)
        assertEquals("home-basic", copy.program.sourceTemplateId)
        assertFalse(copy.program.builtin)
        assertEquals(before, factory())
        assertEquals(2, db.programDao().getProgramIds().size)
        assertTrue(copy.days.all { it.compositeId.startsWith(copy.program.id + ":") })
        assertFalse(copy.days.any { day -> before.days.any { it.compositeId == day.compositeId } })
        assertEquals("home-basic", com.repforge.app.data.store.TrainPilotSettingsStore(context)
            .current().activeProgramId)
        assertNull(ActiveWorkoutStore.current(context)?.takeIf { it.optBoolean("active") })
    }

    @Test fun previewAndCancelAreReadOnlyAndShowOnlyTheFactoryABDay() = runTest(dispatcher) {
        val source = copy()
        val before = db.programDao().getProgramsWithDays()
        val preview = NativeProgramActions.preview(context, db, source, "A")
        assertEquals("A", preview.dayId)
        assertEquals(source.days.first { it.dayId == "A" }.exercisesJson,
            JSONArray().apply { preview.before.forEach { put(org.json.JSONTokener(it).nextValue()) } }.toString())
        assertTrue(preview.after.isNotEmpty())
        assertEquals(before, db.programDao().getProgramsWithDays())
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            NativeProgramActions.preview(context, db, source, "C")
        } }
    }

    @Test fun restoreSelectedDayAndRecipePreservesOtherDayAndUnknownRawFields() = runTest(dispatcher) {
        val copy = copy()
        val extra = JSONObject(copy.program.rawJson)
        val a = extra.getJSONArray("days").getJSONObject(0).put("unknownDayField", "keep")
        val sourceId = NativeProgramEditor.exerciseId(NativeProgramEditor.from(copy, "hu").days.first().entries.first())
        extra.put("unknownProgramField", JSONObject().put("future", true))
            .put("prescriptions", JSONObject().put(sourceId,
                JSONObject().put("sets", 5).put("futureRecipeField", "drop-on-factory-reset")))
            .put("days", JSONArray().put(a).put(extra.getJSONArray("days").getJSONObject(1)))
        val altered = copy.program.copy(rawJson = extra.toString())
        val modifiedA = copy.days.first { it.dayId == "A" }.copy(
            exercisesJson = JSONArray().put("goblet-squat").toString())
        db.programDao().saveProgramWithDays(altered,
            copy.days.map { if (it.dayId == "A") modifiedA else it })
        val opened = db.programDao().getProgramWithDaysById(copy.program.id)!!
        val other = opened.days.single { it.dayId == "B" }
        val preview = NativeProgramActions.preview(context, db, opened, "A")
        val id = NativeProgramActions.restore(context, db, preview)
        assertEquals(opened.program.id, id)
        val saved = db.programDao().getProgramWithDaysById(id)!!
        assertEquals(factory().days.single { it.dayId == "A" }.exercisesJson,
            saved.days.single { it.dayId == "A" }.exercisesJson)
        assertEquals(other, saved.days.single { it.dayId == "B" })
        val raw = JSONObject(saved.program.rawJson)
        assertTrue(raw.getJSONObject("unknownProgramField").getBoolean("future"))
        assertEquals("keep", raw.getJSONArray("days").getJSONObject(0).getString("unknownDayField"))
        val restoredRx = raw.getJSONObject("prescriptions").getJSONObject(sourceId)
        assertFalse(restoredRx.has("sets"))
        assertEquals("drop-on-factory-reset", restoredRx.getString("futureRecipeField"))
        assertEquals(factory(), db.programDao().getProgramWithDaysById("home-basic"))
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramActions.restore(context, db, preview)
        } }
    }

    @Test fun sourceProgramChangeAndFactoryChangeBlockStaleRestoreWithoutWrite() = runTest(dispatcher) {
        val original = copy()
        val preview = NativeProgramActions.preview(context, db, original, "A")
        NativeProgramEditor.save(context, db, original,
            NativeProgramEditor.from(original, "hu").copy(name = "Changed"), "hu")
        val updated = db.programDao().getProgramWithDaysById(original.program.id)!!
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramActions.restore(context, db, preview)
        } }
        assertEquals(updated, db.programDao().getProgramWithDaysById(original.program.id))
        val again = NativeProgramActions.preview(context, db, updated, "A")
        val originalFactory = factory()
        db.programDao().saveProgramWithDays(originalFactory.program.copy(nameHu = "Changed factory"), originalFactory.days)
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramActions.restore(context, db, again)
        } }
        assertEquals(updated, db.programDao().getProgramWithDaysById(original.program.id))
    }

    @Test fun activeWorkoutAndReplacementFenceBlockRestore() = runTest(dispatcher) {
        val opened = copy()
        val preview = NativeProgramActions.preview(context, db, opened, "A")
        ActiveWorkoutStore.saveOwned(context, JSONObject().put("workoutId", "guarded-active"))
        try {
            assertThrows(IllegalStateException::class.java) { runBlocking {
                NativeProgramActions.restore(context, db, preview)
            } }
        } finally { ActiveWorkoutStore.clear(context) }
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramActions.restore(context, db, preview)
        } }
    }

    @Test fun unrelatedCustomProgramCannotClaimFactoryDayRestore() = runTest(dispatcher) {
        val custom = NativeProgramEditor.Draft("Own", "", listOf(
            NativeProgramEditor.DayDraft("A", "A", listOf(JSONObject.quote("goblet-squat")))))
        val id = NativeProgramEditor.save(context, db, null, custom, "hu")
        val opened = db.programDao().getProgramWithDaysById(id)!!
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramActions.preview(context, db, opened, "A")
        } }
    }
}
