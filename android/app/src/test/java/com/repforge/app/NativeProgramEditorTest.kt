package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
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
class NativeProgramEditorTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore

    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
    }
    @After fun teardown() {
        db.close()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun initial() = NativeProgramEditor.addDay(
        NativeProgramEditor.Draft("Egyéni terv", "Védelem", emptyList()), "A")
    private suspend fun seedExercise() {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" })
    }
    private suspend fun create(): String =
        NativeProgramEditor.save(context, db, null, initial(), "hu")

    @Test fun newProgramCreatesStableIdAndMainCompatibleBackup() = runBlocking {
        seedExercise()
        val id = create()
        assertTrue(id.startsWith("custom-program-"))
        val original = db.programDao().getProgramWithDaysById(id)!!
        assertFalse(original.program.builtin)
        assertEquals(1, original.days.size)
        val raw = JSONObject(original.program.rawJson)
        assertEquals(id, raw.getString("id"))
        assertEquals("Egyéni terv", raw.getString("name"))
        assertEquals("A", raw.getJSONArray("days").getJSONObject(0).getString("name"))
        val backup = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        assertEquals(id, backup.getJSONArray("programs").getJSONObject(0).getString("id"))
    }

    @Test fun addRemoveAndReorderExercisesPreservesTheirIdentifiers() = runBlocking {
        seedExercise()
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        val first = NativeProgramEditor.from(opened, "hu")
        val plus = first.copy(days = first.days.map {
            NativeProgramEditor.addExercise(NativeProgramEditor.addExercise(it, "goblet-squat"), "goblet-squat")
        })
        NativeProgramEditor.save(context, db, opened, plus, "hu")
        val fresh = db.programDao().getProgramWithDaysById(id)!!
        assertEquals(listOf("goblet-squat", "goblet-squat"),
            JSONArray(fresh.days[0].exercisesJson).let { a -> (0 until a.length()).map { a.getString(it) } })
        val revised = NativeProgramEditor.from(fresh, "hu")
        val shorter = revised.copy(days = revised.days.map { it.copy(entries = it.entries.take(1)) })
        NativeProgramEditor.save(context, db, fresh, shorter, "hu")
        assertEquals(1, JSONArray(db.programDao().getProgramWithDaysById(id)!!.days[0].exercisesJson).length())
        assertEquals(listOf("x", "y", "z"), NativeProgramEditor.move(listOf("x", "z", "y"), 2, -1))
    }

    @Test fun copyOfBuiltinHasDifferentIdAndLeavesReferenceUntouched() = runBlocking {
        seedExercise()
        val source = TrainPilotDatabase.defaultPrograms()
        val template = source.first.first { it.id == "home-basic" }
        db.programDao().saveProgramWithDays(template, source.second.filter { it.programId == template.id })
        val built = db.programDao().getProgramWithDaysById(template.id)!!
        val clone = NativeProgramEditor.save(context, db, built, NativeProgramEditor.from(built, "hu"), "hu")
        assertNotEquals(template.id, clone)
        assertTrue(db.programDao().getProgramWithDaysById(template.id)!!.program.builtin)
        assertFalse(db.programDao().getProgramWithDaysById(clone)!!.program.builtin)
        assertEquals(built.days.map { it.exercisesJson },
            db.programDao().getProgramWithDaysById(template.id)!!.days.map { it.exercisesJson })
    }

    @Test fun concurrentChangeBlocksStaleSaveAndDelete() = runBlocking {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        NativeProgramEditor.save(context, db, opened, initial().copy(name = "Friss név"), "hu")
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.save(context, db, opened, initial().copy(name = "Elavult"), "hu") } }
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.delete(context, db, opened, settings) } }
        assertEquals("Friss név", db.programDao().getProgramWithDaysById(id)!!.program.nameHu)
    }

    @Test fun removingScheduledDayIsRejectedWithoutLosingTheOtherDay() = runBlocking {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        val second = NativeProgramEditor.addDay(NativeProgramEditor.from(opened, "hu"), "B")
        NativeProgramEditor.save(context, db, opened, second, "hu")
        val before = db.programDao().getProgramWithDaysById(id)!!
        val day = before.days.first()
        db.calendarScheduleDao().upsertEntry(CalendarScheduleEntity("scheduled-plan-1", "event", "2026-11-01",
            "2026-11-01T08:00:00Z", "2026-11-01T09:00:00Z", id, "Plan", day.dayId,
            "Day", "planned", 1, "{}"))
        val draft = NativeProgramEditor.from(before, "hu").copy(days = listOf(
            NativeProgramEditor.from(before, "hu").days.last()))
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.save(context, db, before, draft, "hu") } }
        assertEquals(2, db.programDao().getProgramWithDaysById(id)!!.days.size)
    }

    @Test fun activeOrReferencedProgramCannotBeDeleted() = runBlocking {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        settings.setActiveProgramId(id)
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.delete(context, db, opened, settings) } }
        settings.setActiveProgramId("home-basic")
        val history = JournalTestFixtures.row(4).copy(programId = id)
        db.workoutHistoryDao().insertWorkout(history)
        assertThrows(IllegalStateException::class.java) { runBlocking {
            NativeProgramEditor.delete(context, db, opened, settings) } }
        assertNotNull(db.programDao().getProgramWithDaysById(id))
    }

    @Test fun deleteUnusedProgramDoesNotAlterWorkoutHistoryOrExercises() = runBlocking {
        seedExercise()
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        NativeProgramEditor.delete(context, db, opened, settings)
        assertNull(db.programDao().getProgramWithDaysById(id))
        assertNotNull(db.exerciseLibraryDao().getExerciseById("goblet-squat"))
    }

    @Test fun replacementFenceRejectsProgramWrites() = runBlocking {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        try {
            assertThrows(IllegalStateException::class.java) { runBlocking { create() } }
            assertTrue(db.programDao().getProgramIds().isEmpty())
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test fun invalidExercisesFailBeforeAnyProgramIsWritten() = runBlocking {
        val invalid = initial().copy(days = initial().days.map {
            NativeProgramEditor.addExercise(it, "unknown")
        })
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            NativeProgramEditor.save(context, db, null, invalid, "hu") } }
        assertTrue(db.programDao().getProgramIds().isEmpty())
    }

    @Test fun embeddedExercisePrescriptionsAreNotFlattenedOnEdit() = runBlocking {
        seedExercise()
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        val custom = NativeProgramEditor.from(opened, "hu").let { original ->
            original.copy(days = original.days.map {
                it.copy(entries = listOf(JSONObject().put("id", "goblet-squat")
                    .put("sets", 5).put("progressionRule", "double").toString()))
            })
        }
        NativeProgramEditor.save(context, db, opened, custom, "hu")
        val actual = db.programDao().getProgramWithDaysById(id)!!
        val row = JSONArray(actual.days[0].exercisesJson).getJSONObject(0)
        assertEquals(5, row.getInt("sets"))
        assertEquals("double", row.getString("progressionRule"))
    }
}
