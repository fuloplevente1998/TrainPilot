package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class NativeA07CatalogResetParityTest {
    private val dispatcher = StandardTestDispatcher()
    private val executionField = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply {
        isAccessible = true
    }
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore

    @Before fun setup() = runBlocking {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (executionField.get(null) as? PhoneWorkoutExecutionStore)?.close()
        executionField.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        android.util.AtomicFile(java.io.File(context.noBackupFilesDir, "ownership193.json")).delete()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)

        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        for (program in programs) {
            db.programDao().saveProgramWithDays(program, days.filter { it.programId == program.id })
        }
        settings = TrainPilotSettingsStore(context)
    }

    @After fun cleanup() {
        db.close()
        ActiveWorkoutStore.clear(context)
        (executionField.get(null) as? PhoneWorkoutExecutionStore)?.close()
        executionField.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private suspend fun factory(): ProgramWithDays =
        requireNotNull(db.programDao().getProgramWithDaysById("home-basic"))

    private suspend fun factoryCopy(token: String): ProgramWithDays {
        val source = factory()
        val id = NativeProgramEditor.save(
            context, db, source, NativeProgramEditor.from(source, "hu"), "hu", actionToken = token
        )
        return requireNotNull(db.programDao().getProgramWithDaysById(id))
    }

    private suspend fun modifyDayA(source: ProgramWithDays): ProgramWithDays {
        val changed = source.days.map { day ->
            if (day.dayId == "A") day.copy(exercisesJson = JSONArray().put("goblet-squat").toString())
            else day
        }
        db.programDao().saveProgramWithDays(source.program, changed)
        return requireNotNull(db.programDao().getProgramWithDaysById(source.program.id))
    }

    private suspend fun <T : Throwable> rejects(type: Class<T>, operation: suspend () -> Unit) {
        try {
            operation()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            assertTrue("Expected ${type.simpleName}, got $failure", type.isInstance(failure))
            return
        }
        fail("Expected ${type.simpleName}")
    }

    @Test fun fullCatalogFavoritesCustomExerciseAndActiveProgramStayOutsideFactoryDayRestore() =
        runTest(dispatcher) {
            assertEquals(106, db.exerciseLibraryDao().getExerciseIds().size)
            assertEquals(13, db.programDao().getProgramIds().size)

            val seed = TrainPilotDatabase.defaultExercises().first()
            val custom = seed.copy(
                id = "a07-custom-exercise",
                nameHu = "A07 saját gyakorlat",
                nameEn = "A07 custom exercise",
                nameDe = "A07 eigene Übung",
                nameRo = "Exercițiu personalizat A07",
                custom = true,
                rawJson = JSONObject().put("id", "a07-custom-exercise").put("custom", true).toString()
            )
            db.exerciseLibraryDao().upsertExercise(custom)
            NativeExerciseFavorites.toggle(context, db, custom.id)

            val opened = modifyDayA(factoryCopy("a07-parity-copy"))
            settings.setActiveProgramId(opened.program.id)

            val beforeCatalog = db.exerciseLibraryDao().getAllExercises()
            val beforeFavorites = NativeExerciseFavorites.load(db)
            val beforeOtherPrograms = db.programDao().getProgramsWithDays()
                .filterNot { it.program.id == opened.program.id }
            val beforeDayB = opened.days.single { it.dayId == "B" }
            val factory = factory()

            val preview = NativeProgramActions.preview(context, db, opened, "A")
            NativeProgramActions.restore(context, db, preview)

            val restored = requireNotNull(db.programDao().getProgramWithDaysById(opened.program.id))
            assertEquals(factory.days.single { it.dayId == "A" }, restored.days.single { it.dayId == "A" })
            assertEquals(beforeDayB, restored.days.single { it.dayId == "B" })
            assertEquals(beforeCatalog, db.exerciseLibraryDao().getAllExercises())
            assertEquals(beforeFavorites, NativeExerciseFavorites.load(db))
            assertEquals(opened.program.id, settings.current().activeProgramId)
            assertEquals(
                beforeOtherPrograms,
                db.programDao().getProgramsWithDays().filterNot { it.program.id == opened.program.id }
            )
        }

    @Test fun legacyImportedCustomCatalogRowSurvivesFactoryDayRestore() = runTest(dispatcher) {
        val imported = JSONObject()
            .put("id", "legacy-a07-custom")
            .put("custom", true)
            .put("hu", "Örökölt saját")
            .put("en", "Legacy custom")
            .put("de", "Importierte eigene Übung")
            .put("ro", "Exercițiu importat")
            .put("notesDe", "A07")
            .put("loadType", "total")
            .put("repUnit", "ism.")
            .put("measurementType", "reps")
            .put("sets", 3)
            .put("reps", "8")
            .put("weight", 12)

        LegacyWebViewMigrator(context, db, settings).importBackupJson(
            JSONObject().put("exercises", JSONArray().put(imported)), false
        )
        val importedBefore = requireNotNull(db.exerciseLibraryDao().getExerciseById("legacy-a07-custom"))
        assertTrue(importedBefore.custom)

        val opened = modifyDayA(factoryCopy("a07-legacy-copy"))
        val preview = NativeProgramActions.preview(context, db, opened, "A")
        NativeProgramActions.restore(context, db, preview)

        assertEquals(importedBefore, db.exerciseLibraryDao().getExerciseById("legacy-a07-custom"))
        assertEquals(107, db.exerciseLibraryDao().getExerciseIds().size)
    }

    @Test fun missingCatalogDependencyFailsAtomicallyWithoutPartialProgramWrite() = runTest(dispatcher) {
        val opened = modifyDayA(factoryCopy("a07-rollback-copy"))
        val preview = NativeProgramActions.preview(context, db, opened, "A")
        val factoryIds = preview.after.map(NativeProgramEditor::exerciseId)
        val missingId = factoryIds.first { it != "goblet-squat" }
        val row = requireNotNull(db.exerciseLibraryDao().getExerciseById(missingId))
        db.exerciseLibraryDao().upsertExercise(row.copy(custom = true))
        db.exerciseLibraryDao().deleteCustomExercise(missingId)

        val beforeProgram = requireNotNull(db.programDao().getProgramWithDaysById(opened.program.id))
        val beforeCatalog = db.exerciseLibraryDao().getAllExercises()

        rejects(IllegalArgumentException::class.java) {
            NativeProgramActions.restore(context, db, preview)
        }

        assertEquals(beforeProgram, db.programDao().getProgramWithDaysById(opened.program.id))
        assertEquals(beforeCatalog, db.exerciseLibraryDao().getAllExercises())
    }
}
