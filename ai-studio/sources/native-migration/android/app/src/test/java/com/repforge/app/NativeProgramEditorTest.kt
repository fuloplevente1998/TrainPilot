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
    @get:org.junit.Rule val mainDispatcherRule = NativeMainDispatcherRule()
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

    private suspend fun targeted(): ProgramWithDays {
        seedExercise()
        val base = initial()
        val draft = base.copy(days = base.days.map { NativeProgramEditor.addExercise(it, "goblet-squat") })
        val id = NativeProgramEditor.save(context, db, null, draft, "hu")
        return db.programDao().getProgramWithDaysById(id)!!
    }
    private fun edited(opened: ProgramWithDays, source: ExerciseLibraryEntity) =
        NativeProgramEditor.from(opened, "hu").copy(prescriptionEdits = mapOf(source.id to
            NativeProgramPrescription.Edit(NativeProgramPrescription.edit(
                NativeProgramPrescription.Fields("4", "6–8", "22,5", "75"), source.loadType).toString(),
                NativeProgramPrescription.source(source))))

    @Test fun prescriptionEditPersistsAcrossAllDaysAndPreservesGlobalAndInlineExtensions() = runTest(mainDispatcherRule.dispatcher) {
        val first = targeted()
        val extra = NativeProgramEditor.addDay(NativeProgramEditor.from(first, "hu"), "B")
        val withRows = extra.copy(days = extra.days.mapIndexed { index, day ->
            if (index == 0) day.copy(entries = listOf(JSONObject().put("id", "goblet-squat")
                .put("sets", 1).put("reps", "99").put("variation", "slow").toString()))
            else NativeProgramEditor.addExercise(day, "goblet-squat")
        })
        NativeProgramEditor.save(context, db, first, withRows, "hu")
        val before = db.programDao().getProgramWithDaysById(first.program.id)!!
        val raw = JSONObject(before.program.rawJson).put("prescriptions", JSONObject()
            .put("goblet-squat", JSONObject().put("rir", 3).put("future", JSONObject().put("keep", true)))
            .put("legacy-exercise", JSONObject().put("reps", "preserve")))
        db.programDao().saveProgramWithDays(before.program.copy(rawJson = raw.toString()), before.days)
        val opened = db.programDao().getProgramWithDaysById(first.program.id)!!
        val source = db.exerciseLibraryDao().getExerciseById("goblet-squat")!!
        NativeProgramEditor.save(context, db, opened, edited(opened, source), "hu")
        val saved = db.programDao().getProgramWithDaysById(first.program.id)!!
        val rx = JSONObject(saved.program.rawJson).getJSONObject("prescriptions")
        assertEquals(4, rx.getJSONObject(source.id).getInt("sets"))
        assertEquals(22.5, rx.getJSONObject(source.id).getDouble("weight"), 0.0)
        assertEquals(3, rx.getJSONObject(source.id).getInt("rir"))
        assertTrue(rx.getJSONObject(source.id).getJSONObject("future").getBoolean("keep"))
        assertFalse(rx.getJSONObject(source.id).has("variation"))
        assertEquals("preserve", rx.getJSONObject("legacy-exercise").getString("reps"))
        val inline = JSONArray(saved.days.first { it.dayId == withRows.days.first().id }.exercisesJson).getJSONObject(0)
        assertEquals(4, inline.getInt("sets")); assertEquals("6–8", inline.getString("reps"))
        assertEquals("slow", inline.getString("variation"))
        assertEquals("goblet-squat", JSONArray(saved.days.first { it.dayId != withRows.days.first().id }.exercisesJson).getString(0))
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun malformedPrescriptionCannotPartiallySaveProgramNameOrDayChanges() = runTest(mainDispatcherRule.dispatcher) {
        val opened = targeted()
        val source = db.exerciseLibraryDao().getExerciseById("goblet-squat")!!
        val valid = edited(opened, source)
        val malformed = JSONObject(valid.prescriptionEdits.getValue(source.id).patch).put("sets", 1.5)
        val draft = valid.copy(name = "Must not persist", prescriptionEdits = mapOf(source.id to
            NativeProgramPrescription.Edit(malformed.toString(), NativeProgramPrescription.source(source))))
        assertNativeSuspendThrows(IllegalArgumentException::class.java) {
            NativeProgramEditor.save(context, db, opened, draft, "hu")
        }
        assertEquals(opened, db.programDao().getProgramWithDaysById(opened.program.id))
    }

    @Test fun exerciseUnitChangeAfterOpeningRejectsPrescriptionWithoutWritingAnything() = runTest(mainDispatcherRule.dispatcher) {
        val opened = targeted()
        val source = db.exerciseLibraryDao().getExerciseById("goblet-squat")!!
        val draft = edited(opened, source)
        db.exerciseLibraryDao().upsertExercise(source.copy(loadType = "bodyweight"))
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.save(context, db, opened, draft, "hu")
        }
        assertEquals(opened, db.programDao().getProgramWithDaysById(opened.program.id))
    }

    @Test fun builtinPrescriptionEditingCreatesACloneAndLeavesTheTemplateUntouched() = runTest(mainDispatcherRule.dispatcher) {
        db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val template = programs.first { it.id == "home-basic" }
        db.programDao().saveProgramWithDays(template, days.filter { it.programId == template.id })
        val opened = db.programDao().getProgramWithDaysById(template.id)!!
        val id = NativeProgramEditor.exerciseId(NativeProgramEditor.from(opened, "hu").days.first().entries.first())
        val source = db.exerciseLibraryDao().getExerciseById(id)!!
        val patch = NativeProgramPrescription.edit(NativeProgramPrescription.Fields("4", "9–11", "10", "75"), source.loadType)
        val draft = NativeProgramEditor.from(opened, "hu").copy(prescriptionEdits = mapOf(id to
            NativeProgramPrescription.Edit(patch.toString(), NativeProgramPrescription.source(source))))
        val clone = NativeProgramEditor.save(context, db, opened, draft, "hu")
        assertNotEquals(template.id, clone)
        assertEquals(opened, db.programDao().getProgramWithDaysById(template.id))
        assertEquals(4, JSONObject(db.programDao().getProgramWithDaysById(clone)!!.program.rawJson)
            .getJSONObject("prescriptions").getJSONObject(id).getInt("sets"))
    }

    @Test fun editedPrescriptionsSurvivePortableExportAndReimport() = runTest(mainDispatcherRule.dispatcher) {
        val opened = targeted()
        val source = db.exerciseLibraryDao().getExerciseById("goblet-squat")!!
        NativeProgramEditor.save(context, db, opened, edited(opened, source), "hu")
        val migrator = LegacyWebViewMigrator(context, db, settings)
        val backup = migrator.exportBackupJson(false)
        val expected = JSONObject(db.programDao().getProgramWithDaysById(opened.program.id)!!.program.rawJson)
            .getJSONObject("prescriptions").toString()
        db.programDao().deleteAllPrograms()
        migrator.importBackupJson(backup, true)
        val actual = JSONObject(db.programDao().getProgramWithDaysById(opened.program.id)!!.program.rawJson)
            .getJSONObject("prescriptions")
        assertEquals(com.repforge.shared.LegacySyncIdentity18.canonical(JSONObject(expected)),
            com.repforge.shared.LegacySyncIdentity18.canonical(actual))
    }

    @Test fun newProgramCreatesStableIdAndMainCompatibleBackup() = runTest(mainDispatcherRule.dispatcher) {
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

    @Test fun addRemoveAndReorderExercisesPreservesTheirIdentifiers() = runTest(mainDispatcherRule.dispatcher) {
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

    @Test fun copyOfBuiltinHasDifferentIdAndLeavesReferenceUntouched() = runTest(mainDispatcherRule.dispatcher) {
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

    @Test fun concurrentChangeBlocksStaleSaveAndDelete() = runTest(mainDispatcherRule.dispatcher) {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        NativeProgramEditor.save(context, db, opened, initial().copy(name = "Friss név"), "hu")
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.save(context, db, opened, initial().copy(name = "Elavult"), "hu") }
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.delete(context, db, opened, settings) }
        assertEquals("Friss név", db.programDao().getProgramWithDaysById(id)!!.program.nameHu)
    }

    @Test fun removingScheduledDayIsRejectedWithoutLosingTheOtherDay() = runTest(mainDispatcherRule.dispatcher) {
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
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.save(context, db, before, draft, "hu") }
        assertEquals(2, db.programDao().getProgramWithDaysById(id)!!.days.size)
    }

    @Test fun activeOrReferencedProgramCannotBeDeleted() = runTest(mainDispatcherRule.dispatcher) {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        settings.setActiveProgramId(id)
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.delete(context, db, opened, settings) }
        settings.setActiveProgramId("home-basic")
        val history = JournalTestFixtures.row(4).copy(programId = id)
        db.workoutHistoryDao().insertWorkout(history)
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.delete(context, db, opened, settings) }
        assertNotNull(db.programDao().getProgramWithDaysById(id))
    }

    @Test fun deleteUnusedProgramDoesNotAlterWorkoutHistoryOrExercises() = runTest(mainDispatcherRule.dispatcher) {
        seedExercise()
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        NativeProgramEditor.delete(context, db, opened, settings)
        assertNull(db.programDao().getProgramWithDaysById(id))
        assertNotNull(db.exerciseLibraryDao().getExerciseById("goblet-squat"))
    }

    @Test fun legacyDayMetadataAndOtherLanguageNamesArePreservedOnEdit() = runTest(mainDispatcherRule.dispatcher) {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        val previous = opened.days.single()
        val translated = previous.copy(nameEn = "Original English", nameDe = "Original Deutsch",
            nameRo = "Original Română")
        val original = JSONObject(opened.program.rawJson)
        val rawDay = original.getJSONArray("days").getJSONObject(0)
            .put("specialRestSeconds", 55)
        original.put("legacyPriority", "keep").put("days", JSONArray().put(rawDay))
        db.programDao().saveProgramWithDays(opened.program.copy(rawJson = original.toString()),
            listOf(translated))
        val latest = db.programDao().getProgramWithDaysById(id)!!
        NativeProgramEditor.save(context, db, latest,
            NativeProgramEditor.from(latest, "hu").copy(name = "Módosítva"), "hu")
        val persisted = db.programDao().getProgramWithDaysById(id)!!
        assertEquals("Original English", persisted.days.single().nameEn)
        assertEquals("Original Deutsch", persisted.days.single().nameDe)
        val saved = JSONObject(persisted.program.rawJson)
        assertEquals("keep", saved.getString("legacyPriority"))
        assertEquals(55, saved.getJSONArray("days").getJSONObject(0).getInt("specialRestSeconds"))
        val backup = LegacyWebViewMigrator(context, db, settings).exportBackupJson(false)
        val portable = backup.getJSONArray("programs").getJSONObject(0).getJSONArray("days").getJSONObject(0)
        assertEquals("Original English", portable.getString("en"))
        assertEquals("Original Deutsch", portable.getString("de"))
        assertEquals(55, portable.getInt("specialRestSeconds"))
    }

    @Test fun scheduledProgramCannotBeDeletedEvenWhenInactive() = runTest(mainDispatcherRule.dispatcher) {
        val id = create()
        val opened = db.programDao().getProgramWithDaysById(id)!!
        db.calendarScheduleDao().upsertEntry(CalendarScheduleEntity("scheduled-plan-2", "event", "2026-11-01",
            "2026-11-01T08:00:00Z", "2026-11-01T09:00:00Z", id, "Plan",
            opened.days.first().dayId, "Day", "planned", 1, "{}"))
        assertNativeSuspendThrows(IllegalStateException::class.java) {
            NativeProgramEditor.delete(context, db, opened, settings) }
        assertNotNull(db.programDao().getProgramWithDaysById(id))
    }

    @Test fun replacementFenceRejectsProgramWrites() = runTest(mainDispatcherRule.dispatcher) {
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        try {
            assertNativeSuspendThrows(IllegalStateException::class.java) { create() }
            assertTrue(db.programDao().getProgramIds().isEmpty())
        } finally {
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    @Test fun invalidExercisesFailBeforeAnyProgramIsWritten() = runTest(mainDispatcherRule.dispatcher) {
        val invalid = initial().copy(days = initial().days.map {
            NativeProgramEditor.addExercise(it, "unknown")
        })
        assertNativeSuspendThrows(IllegalArgumentException::class.java) {
            NativeProgramEditor.save(context, db, null, invalid, "hu") }
        assertTrue(db.programDao().getProgramIds().isEmpty())
    }

    @Test fun embeddedExercisePrescriptionsAreNotFlattenedOnEdit() = runTest(mainDispatcherRule.dispatcher) {
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
