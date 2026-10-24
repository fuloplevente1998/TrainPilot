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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeBuiltinCatalogTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    @Before fun open() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db.close()
    }
    @Test fun completeReferenceBundleHasFourLanguagesAndEveryProgramExerciseExists() {
        val exercises = TrainPilotDatabase.defaultExercises()
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        assertEquals(106, exercises.size); assertEquals(106, exercises.map { it.id }.distinct().size)
        assertEquals(13, programs.size)
        assertTrue(exercises.all { listOf(it.nameHu, it.nameEn, it.nameDe, it.nameRo, it.notesHu, it.notesEn, it.notesDe, it.notesRo).all(String::isNotBlank) })
        for (day in days) {
            assertTrue(listOf(day.nameHu, day.nameEn, day.nameDe, day.nameRo).all(String::isNotBlank))
            val rows = JSONArray(day.exercisesJson)
            for (index in 0 until rows.length()) assertTrue(exercises.any { it.id == rows.getString(index) })
        }
        assertEquals("single_dumbbell", exercises.first { it.id == "goblet-squat" }.loadType)
        val running = exercises.first { it.id == "running" }
        assertEquals("distance", running.measurementType)
        assertEquals("mp", running.repUnit)
        assertNull("Search pages cannot masquerade as embedded videos", running.videoUrl)
    }
    @Test fun partiallySeededDatabaseReceivesEveryMissingExerciseAndProgram() = runBlocking {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first())
        val first = TrainPilotDatabase.defaultPrograms().first.first()
        db.programDao().upsertProgram(first)
        TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        assertEquals(106, db.exerciseLibraryDao().getExerciseIds().size)
        assertEquals(13, db.programDao().getProgramIds().size)
        // Existing program IDs are preserved, including an intentionally empty program.
        assertTrue(db.programDao().getProgramWithDaysById(first.id)!!.days.isEmpty())
    }
    @Test fun customizedRowsAndOldNativeAliasesSurviveCatalogExpansion() = runBlocking {
        val source = TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" }
        val customized = source.copy(nameHu = "Saját név", defaultWeight = 123.0, defaultReps = "5", notesHu = "Saját megjegyzés", custom = true,
            rawJson = "{\"id\":\"goblet-squat\",\"custom\":true,\"weight\":123}")
        val alias = customized.copy(id = "old-native-alias", custom = false)
        db.exerciseLibraryDao().upsertExercises(listOf(customized, alias))
        val program = TrainPilotDatabase.defaultPrograms().first.first().copy(nameHu = "Saját program", rawJson = "{\"kept\":true}")
        val day = ProgramDayEntity("${program.id}:X", program.id, "X", "Saját nap", "Custom day", "Eigener Tag", "Zi proprie", 0, "[\"old-native-alias\"]")
        db.programDao().saveProgramWithDays(program, listOf(day))
        TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        assertEquals(customized, db.exerciseLibraryDao().getExerciseById(customized.id))
        assertEquals(alias, db.exerciseLibraryDao().getExerciseById(alias.id))
        assertEquals(program, db.programDao().getProgramWithDaysById(program.id)!!.program)
        assertEquals(listOf(day), db.programDao().getProgramWithDaysById(program.id)!!.days)
    }
    @Test fun repeatedSeedingIsIdempotentWithoutDeletingHistoryOrChangingExistingRawData() = runBlocking {
        TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(0))
        val before = db.exerciseLibraryDao().getAllExercises()
        val programs = db.programDao().getProgramsWithDays()
        TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        assertEquals(before, db.exerciseLibraryDao().getAllExercises())
        assertEquals(programs, db.programDao().getProgramsWithDays())
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
    }
    @Test fun oldBuiltinImportsGainMissingTranslationsAndPreserveMeasuredDistanceClassification() = runBlocking {
        val data = JSONObject().put("exercises", JSONArray().put(JSONObject().put("id", "running").put("hu", "Saját futásnév")
            .put("sets", 2).put("reps", "45").put("weight", 0)))
        val base = NativeBuiltinCatalog.programs.first.first()
        data.put("programs", JSONArray().put(JSONObject().put("id", base.id).put("name", "Saját programnév").put("builtin", true)
            .put("days", JSONArray().put(JSONObject().put("id", "A").put("name", "A").put("exercises", JSONArray().put("running"))))))
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).importBackupJson(data, false)
        val row = db.exerciseLibraryDao().getExerciseById("running")!!
        assertEquals("Saját futásnév", row.nameHu); assertEquals(2, row.defaultSets); assertEquals("45", row.defaultReps)
        assertEquals("distance", row.measurementType); assertFalse(row.custom)
        assertEquals(NativeBuiltinCatalog.exercises.first { it.id == "running" }.nameDe, row.nameDe)
        assertTrue(row.notesDe.isNotBlank()); assertTrue(row.notesRo.isNotBlank())
        assertEquals("Saját programnév", db.programDao().getProgramWithDaysById(base.id)!!.program.nameHu)
        assertEquals(base.nameDe, db.programDao().getProgramWithDaysById(base.id)!!.program.nameDe)
    }
    @Test fun explicitlyCustomTranslationsAndMeasurementsAreNotReplacedByBuiltinDefaults() = runBlocking {
        val custom = JSONObject().put("id", "running").put("custom", true).put("hu", "Saját").put("en", "Custom")
            .put("de", "Eigene Übersetzung").put("ro", "Traducere proprie").put("notesDe", "Eigene Notiz")
            .put("loadType", "total").put("repUnit", "ism.").put("measurementType", "reps").put("weight", 12)
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).importBackupJson(JSONObject().put("exercises", JSONArray().put(custom)), false)
        val row = db.exerciseLibraryDao().getExerciseById("running")!!
        assertTrue(row.custom); assertEquals("Eigene Übersetzung", row.nameDe); assertEquals("Eigene Notiz", row.notesDe)
        assertEquals("reps", row.measurementType); assertEquals(12.0, row.defaultWeight, 0.0)
    }
    @Test fun dataReplacementFenceRejectsLateCatalogWritesBeforeAnyRowsAppear() = runBlocking {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().putBoolean("blocked", true).commit()
        var rejected = false
        try { TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context) } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
        assertTrue(db.exerciseLibraryDao().getExerciseIds().isEmpty())
        assertTrue(db.programDao().getProgramIds().isEmpty())
    }
}
