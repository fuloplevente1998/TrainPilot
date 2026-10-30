package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
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
class NativeCustomExerciseEditorTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun cleanup() {
        Dispatchers.resetMain()
        db.close()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private suspend fun <T : Throwable> rejects(type: Class<T>, operation: suspend () -> Unit) {
        try { operation() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Throwable) {
            assertTrue("Expected ${type.simpleName}, got $failure", type.isInstance(failure))
            return
        }
        fail("Expected ${type.simpleName}")
    }

    private fun draft() = NativeCustomExerciseEditor.Draft("Saját edzés", "Kéz a falon", "arms",
        "reps", "bodyweight", "ism.", 3, "12", 0.0, 90,
        "https://www.youtube.com/watch?v=v_c67Omje48")

    @Test fun createAndEditKeepIdentityAndCanonicalPortableFields() = runTest(dispatcher) {
        val id = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        assertTrue(id.matches(Regex("custom-[a-f0-9-]{36}")))
        var ex = db.exerciseLibraryDao().getExerciseById(id)!!
        assertTrue(ex.custom)
        assertEquals("Saját edzés", JSONObject(ex.rawJson).getString("hu"))
        assertEquals("bodyweight", ex.loadType)
        assertEquals("https://www.youtube.com/watch?v=v_c67Omje48", NativeExerciseDemo.resolve(ex)?.url)
        val change = draft().copy(name = "Új név", sets = 4, reps = "8", note = "Más leírás")
        assertEquals(id, NativeCustomExerciseEditor.save(context, db, id, change, "hu"))
        ex = db.exerciseLibraryDao().getExerciseById(id)!!
        assertEquals("Új név", ex.nameHu)
        assertEquals(4, ex.defaultSets)
        assertEquals("8", ex.defaultReps)
        assertEquals(1, db.exerciseLibraryDao().getAllExercises().size)
    }

    @Test fun editingOneLanguagePreservesOtherNamesAndLegacyMetadata() {
        val base = NativeCustomExerciseEditor.update(null, "custom-example", draft(), "hu")
        val restored = base.copy(nameEn = "Own exercise",
            muscleGroupsJson = JSONArray().put("arms").put("back").toString(),
            rawJson = JSONObject(base.rawJson).put("legacyExtra", "preserved").put("en", "Own exercise").toString())
        val edited = NativeCustomExerciseEditor.update(restored, "custom-example",
            draft().copy(name = "Eigener Name"), "de")
        assertEquals("Saját edzés", edited.nameHu)
        assertEquals("Own exercise", edited.nameEn)
        assertEquals("Eigener Name", edited.nameDe)
        assertEquals("preserved", JSONObject(edited.rawJson).getString("legacyExtra"))
        assertEquals("Own exercise", JSONObject(edited.rawJson).getString("en"))
        assertEquals(listOf("arms", "back"), (0 until JSONArray(edited.muscleGroupsJson).length())
            .map { JSONArray(edited.muscleGroupsJson).getString(it) })
    }

    @Test fun missingVideoOnEditClearsOldDemoInsteadOfResurrectingIt() {
        val old = NativeCustomExerciseEditor.update(null, "custom-a", draft(), "hu")
        val original = old.copy(rawJson = JSONObject(old.rawJson).put("demo", JSONObject()
            .put("provider", "youtube").put("videoId", "v_c67Omje48")).toString())
        val newer = NativeCustomExerciseEditor.update(original, original.id, draft().copy(videoUrl = ""), "hu")
        assertNull(newer.videoUrl)
        assertNull(NativeExerciseDemo.resolve(newer))
        assertFalse(JSONObject(newer.rawJson).has("demo"))
    }

    @Test fun invalidVideoAndNumericFieldsNeverPersist() = runTest(dispatcher) {
        for (bad in listOf("javascript:alert(1)", "https://youtu.be/unsafe",
            "https://www.youtube.com/watch?v=v_c67Omje48&unexpected=1")) {
            assertThrows(IllegalArgumentException::class.java) { NativeCustomExerciseEditor.validate(draft().copy(videoUrl = bad)) }
        }
        for (bad in listOf(draft().copy(sets = 0), draft().copy(weight = Double.NaN),
            draft().copy(restSeconds = -1), draft().copy(name = ""))) {
            rejects(IllegalArgumentException::class.java) {
                NativeCustomExerciseEditor.save(context, db, null, bad, "hu") }
        }
        assertTrue(db.exerciseLibraryDao().getAllExercises().isEmpty())
    }

    @Test fun builtinRowsCannotBeEditedOrDeleted() = runTest(dispatcher) {
        val builtin = TrainPilotDatabase.defaultExercises().first()
        db.exerciseLibraryDao().upsertExercise(builtin)
        rejects(IllegalArgumentException::class.java) {
                NativeCustomExerciseEditor.save(context, db, builtin.id, draft(), "hu") }
        rejects(IllegalArgumentException::class.java) {
                NativeCustomExerciseEditor.delete(context, db, builtin.id) }
        assertEquals(builtin, db.exerciseLibraryDao().getExerciseById(builtin.id))
    }

    @Test fun deleteFromIoRemovesFavoriteButLeavesOtherExercisesUntouched() = runTest(dispatcher) {
        val a = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        val b = NativeCustomExerciseEditor.save(context, db, null, draft().copy(name = "Második"), "hu")
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("exerciseFavorites",
            JSONArray().put(a).put(b).toString(), 1))
        // Exercise the real ViewModel IO entry without blocking the Main test dispatcher.
        withContext(Dispatchers.IO) { NativeCustomExerciseEditor.delete(context, db, a) }
        assertNull(db.exerciseLibraryDao().getExerciseById(a))
        assertNotNull(db.exerciseLibraryDao().getExerciseById(b))
        assertEquals(listOf(b), NativeExerciseFavorites.load(db))
    }

    @Test fun referencedProgramPreventsDeleteWithoutChangingAnyData() = runTest(dispatcher) {
        val id = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        val program = TrainPilotDatabase.defaultPrograms().first.first().copy(
            id = "custom-plan", builtin = false, rawJson = """{"id":"custom-plan"}""")
        val day = ProgramDayEntity("custom-plan:A", "custom-plan", "A",
            "Saját nap", "Custom day", "Tag", "Zi", 0, JSONArray()
                .put(JSONObject().put("id", id)).toString())
        db.programDao().saveProgramWithDays(program, listOf(day))
        rejects(IllegalStateException::class.java) {
                NativeCustomExerciseEditor.delete(context, db, id) }
        assertNotNull(db.exerciseLibraryDao().getExerciseById(id))
        assertEquals(id, JSONArray(db.programDao().getProgramWithDaysById("custom-plan")!!.days[0].exercisesJson)
            .getJSONObject(0).getString("id"))
    }

    @Test fun referencedWorkoutHistoryPreventsDeletion() = runTest(dispatcher) {
        val id = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        val history = JournalTestFixtures.row(1)
        db.workoutHistoryDao().insertWorkout(history)
        db.workoutHistoryDao().insertExercise(WorkoutHistoryExerciseEntity(
            workoutHistoryId = history.id, exerciseId = id, nameHu = "Saját", nameEn = "Custom",
            loadType = "bodyweight", repUnit = "ism.", measurementType = "reps", orderIndex = 0,
            actualRir = null, setRir229 = false))
        rejects(IllegalStateException::class.java) {
                NativeCustomExerciseEditor.delete(context, db, id) }
        assertNotNull(db.exerciseLibraryDao().getExerciseById(id))
    }

    @Test fun dataReplacementFenceBlocksCustomWritesWithoutPartialRows() = runTest(dispatcher) {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        try {
            rejects(IllegalStateException::class.java) {
                NativeCustomExerciseEditor.save(context, db, null, draft(), "hu") }
            assertTrue(db.exerciseLibraryDao().getAllExercises().isEmpty())
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test fun staleEditorCannotOverwriteOrDeleteConcurrentlyChangedExercise() = runTest(dispatcher) {
        val id = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        val opened = db.exerciseLibraryDao().getExerciseById(id)!!.rawJson
        NativeCustomExerciseEditor.save(context, db, id, draft().copy(name = "Frissebb"), "hu")
        rejects(IllegalStateException::class.java) {
                NativeCustomExerciseEditor.save(context, db, id, draft().copy(name = "Elavult"), "hu", opened) }
        rejects(IllegalStateException::class.java) {
                NativeCustomExerciseEditor.delete(context, db, id, opened) }
        assertEquals("Frissebb", db.exerciseLibraryDao().getExerciseById(id)!!.nameHu)
    }

    @Test fun portableBackupKeepsEditedCustomExerciseAndVideo() = runTest(dispatcher) {
        val id = NativeCustomExerciseEditor.save(context, db, null, draft(), "hu")
        val backup = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).exportBackupJson(false)
        val exported = backup.getJSONArray("exercises").getJSONObject(0)
        assertEquals(id, exported.getString("id"))
        assertEquals("https://www.youtube.com/watch?v=v_c67Omje48", exported.getString("videoUrl"))
    }
}
