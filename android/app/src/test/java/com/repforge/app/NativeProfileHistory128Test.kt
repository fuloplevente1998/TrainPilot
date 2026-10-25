package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import com.repforge.app.ui.coach.NativeCoachExerciseAdvice
import com.repforge.app.ui.coach.NativeCoachReadiness
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
import java.time.Instant
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeProfileHistory128Test {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var store: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private val now get() = Instant.now()
    private val source = "home-basic"
    private val old = "legacy-home"
    private val id = "goblet-squat"

    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        store = TrainPilotSettingsStore(context)
        store.resetAllSettingsForEraseLocal()
        importer = LegacyWebViewMigrator(context, db, store)
        db.exerciseLibraryDao().insertMissingExercises(TrainPilotDatabase.defaultExercises())
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val builtin = programs.first { it.id == source }
        db.programDao().saveProgramWithDays(builtin, days.filter { it.programId == source })
        // Same A/B structure as the source with a different user-owned program ID.
        val legacy = builtin.copy(id = old, builtin = false,
            rawJson = JSONObject(builtin.rawJson).put("id", old).put("builtin", false).toString())
        db.programDao().saveProgramWithDays(legacy, days.filter { it.programId == source }.map {
            it.copy(compositeId = "$old:${it.dayId}", programId = old)
        })
    }
    @After fun close() { db.close() }

    private fun profile() = JSONObject().put("minutes", 90)
        .put("experience", "intermediate").put("goal", "fitness")
        .put("gear", JSONArray().put("dumbbells").put("barbell"))
        .put("excluded", JSONArray()).put("avoidAreas", JSONArray())

    private suspend fun preview(time: Instant = now): NativeProfileProgramPreview.Preview {
        val loaded = db.programDao().getProgramWithDaysById(source)!!
        val base = NativeProfileProgramPreview.preview(loaded, profile().toString(),
            db.exerciseLibraryDao().getAllExercises(), "hu")
        return NativeProfileHistory128.apply(db, base, time)
    }

    private suspend fun workout(label: String, ageDays: Long = 1, pid: String = old,
        weight: Any? = 5.0, reps: List<String> = listOf("12", "12", "12"),
        done: Boolean = true, effort: String = "good", variation: String = "",
        loadType: String = "single_dumbbell", day: String = "A") {
        val end = now.minus(Duration.ofDays(ageDays))
        val sets = JSONArray()
        reps.forEachIndexed { index, count ->
            sets.put(JSONObject().put("setId", "$label-$index")
                .put("weight", weight ?: JSONObject.NULL).put("reps", count).put("done", done))
        }
        val exercise = JSONObject().put("id", id)
            .put("loadType", loadType).put("repUnit", "ism.")
            .put("effort", effort).put("prescription", JSONObject().put("variation", variation))
            .put("sets", sets)
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", label)
            .put("syncId", label).put("workoutId", label).put("programId", pid)
            .put("workout", day).put("dayId", day)
            .put("started", end.minusSeconds(3600).toString()).put("finished", end.toString())
            .put("exercises", JSONArray().put(exercise)))
    }
    @Test fun weightedBodyweightMovementInheritsItsExplicitDumbbellLoadAndVariation() = runBlocking {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        val level2 = programs.first { it.id == "home-level2" }
        db.programDao().saveProgramWithDays(level2, days.filter { it.programId == level2.id })
        val end = now.minusSeconds(86400)
        val sets = JSONArray((0..1).map { i -> JSONObject().put("setId", "weighted-$i")
            .put("done", true).put("weight", 4).put("reps", "40") })
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", "weighted-core").put("programId", level2.id).put("dayId", "A")
            .put("started", end.minusSeconds(60).toString()).put("finished", end.toString())
            .put("exercises", JSONArray().put(JSONObject().put("id", "side-plank").put("loadType", "single_dumbbell")
                .put("repUnit", "mp").put("effort", "good").put("prescription", JSONObject().put("variation", "weighted")).put("sets", sets))))
        val base = NativeProfileProgramPreview.preview(db.programDao().getProgramWithDaysById(level2.id)!!,
            profile().toString(), db.exerciseLibraryDao().getAllExercises(), "hu")
        val result = NativeProfileHistory128.apply(db, base, now)
        val rx = result.prescriptions.getJSONObject("side-plank")
        assertEquals("single_dumbbell", rx.getString("loadType")); assertEquals("weighted", rx.getString("variation"))
        assertEquals(4.0, rx.getDouble("weight"), .0001); assertFalse(rx.getBoolean("trial"))
        assertEquals("weighted-core", result.history128.getJSONObject("entries").getJSONObject("side-plank")
            .getJSONArray("sources").getJSONObject(0).getString("key"))
    }

    @Test fun inheritsLastCompletedWeightAndBoundedExplicitLineage() = runBlocking {
        workout("old-first", 4, weight = 5.0)
        workout("old-second", 2, weight = 12.5)
        val p = preview()
        val rx = p.prescriptions.getJSONObject(id)
        assertEquals(12.5, rx.getDouble("weight"), 0.0001)
        assertFalse(rx.getBoolean("trial"))
        assertEquals(3, rx.getInt("sets"))
        val basis = p.history128.getJSONObject("entries").getJSONObject(id)
        assertEquals("single_dumbbell", basis.getString("loadType"))
        assertEquals("reps", basis.getString("metric"))
        assertEquals("inherited", basis.getString("status"))
        val sources = basis.getJSONArray("sources")
        assertEquals(2, sources.length())
        assertEquals(old, sources.getJSONObject(0).getString("programId"))
        assertEquals("old-second", sources.getJSONObject(0).getString("key"))
        assertTrue(basis.getString("evidenceHash").length == 64)
    }

    @Test fun preservesSeparatePerHandAndTotalUnitsWithoutWeightMultiplication() = runBlocking {
        workout("unit", weight = 5.0, loadType = "total")
        assertTrue(preview().prescriptions.getJSONObject(id).getBoolean("trial"))
        workout("valid", weight = 5.0)
        assertEquals(5.0, preview().prescriptions.getJSONObject(id).getDouble("weight"), 0.001)
    }

    @Test fun unsafeOrIncompleteRecordsNeverProvideStartingWeight() = runBlocking {
        workout("partial", ageDays = 1, done = false)
        workout("pain", ageDays = 2, effort = "pain")
        workout("wrong-variation", ageDays = 3, variation = "weighted")
        workout("null-weight", ageDays = 4, weight = null)
        workout("wrong-reps", ageDays = 5, reps = listOf("10", "bad"))
        val p = preview()
        assertTrue(p.prescriptions.getJSONObject(id).getBoolean("trial"))
        assertEquals(0, p.history128.getJSONObject("entries").length())
    }

    @Test fun newerPainOrPartialSessionBlocksOlderOtherwiseValidLoad() = runBlocking {
        workout("old-good", ageDays = 3, weight = 5.0)
        workout("newer-pain", ageDays = 1, weight = 5.0, effort = "pain")
        assertTrue(preview().prescriptions.getJSONObject(id).getBoolean("trial"))
        assertEquals(0, preview().history128.getJSONObject("entries").length())
    }

    @Test fun oldFutureAndTombstonedSourcesNeverInherit() = runBlocking {
        workout("stale", ageDays = 91)
        workout("future", ageDays = -1)
        workout("deleted", ageDays = 1)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            "deleted", "deleted", "[\"deleted\"]", now.toEpochMilli()))
        assertEquals(0, preview().history128.getJSONObject("entries").length())
    }

    @Test fun unrelatedProgramAndMismatchedDayCannotBecomeInheritedSources() = runBlocking {
        workout("unrelated", pid = "gym-ab")
        workout("wrong-day", day = "D")
        assertEquals(0, preview().history128.getJSONObject("entries").length())
    }

    @Test fun atMostFourConcreteSourceReferencesWithNoDuplicateWorkouts() = runBlocking {
        for (i in 1..7) workout("match-$i", ageDays = i.toLong())
        val p = preview()
        val sources = p.history128.getJSONObject("entries").getJSONObject(id).getJSONArray("sources")
        assertEquals(4, sources.length())
        assertEquals(listOf("match-1", "match-2", "match-3", "match-4"),
            (0 until sources.length()).map { sources.getJSONObject(it).getString("key") })
    }

    @Test fun approvedLineageCarriesContinuityIntoNewProgramButNotUnrelatedWorkouts() = runBlocking {
        workout("approved", weight = 5.0)
        workout("unrelated", pid = "gym-ab", weight = 20.0)
        val p = preview()
        store.saveTrainingProfile(profile())
        val programId = NativeProgramEditor.save(context, db, p.source, p.draft, "hu",
            NativeProfileProgramPreview.metadata(p))
        val created = db.programDao().getProgramWithDaysById(programId)!!
        val raw = JSONObject(created.program.rawJson)
        val entries = raw.getJSONObject("history128").getJSONObject("entries")
        assertEquals("approved", entries.getJSONObject(id).getJSONArray("sources")
            .getJSONObject(0).getString("key"))
        val exercise = db.exerciseLibraryDao().getExerciseById(id)!!
        val lib = JSONObject(exercise.rawJson).put("id", id)
            .put("loadType", exercise.loadType).put("repUnit", exercise.repUnit)
        val rx = raw.getJSONObject("prescriptions").getJSONObject(id)
        val seen = NativeCoachAdviceRepository(db).recent(id, programId, lib, rx, raw, now)
        assertEquals(1, seen.size)
        assertEquals("approved", seen.single().first.getString("id"))
    }

    @Test fun threeInheritedStableTopSessionsOnlySuggestIncreaseNeverChangeSavedWeight() = runBlocking {
        workout("approved-1", ageDays = 1, weight = 5.0,
            reps = listOf("15", "15", "15"))
        workout("approved-2", ageDays = 3, weight = 5.0,
            reps = listOf("15", "15", "15"))
        workout("approved-3", ageDays = 5, weight = 5.0,
            reps = listOf("15", "15", "15"))
        val p = preview()
        store.saveTrainingProfile(profile())
        val programId = NativeProgramEditor.save(context, db, p.source, p.draft, "hu",
            NativeProfileProgramPreview.metadata(p))
        val created = db.programDao().getProgramWithDaysById(programId)!!
        val raw = JSONObject(created.program.rawJson)
        val rx = raw.getJSONObject("prescriptions").getJSONObject(id)
        val library = db.exerciseLibraryDao().getExerciseById(id)!!
        val lib = JSONObject(library.rawJson).put("id", id)
            .put("loadType", library.loadType).put("repUnit", library.repUnit)
            .put("measurementType", library.measurementType)
        val target = NativeCoachExerciseAdvice.target(lib, rx)
        val history = NativeCoachAdviceRepository(db).recent(id, programId, lib, rx, raw, now)
        assertEquals(3, history.size)
        val events = history.map { NativeCoachExerciseAdvice.occurrence(it.first, it.second, target) }
        val good = NativeCoachReadiness.Result(75.0, null, null, null, null,
            null, null, null, null, 2, false)
        val rec = NativeCoachExerciseAdvice.recommend(lib, rx, events, target, good, .5)
        assertEquals("increase", rec.action)
        assertEquals(5.5, rec.weight, .00001)
        assertTrue(rec.autoApply)
        val coach = NativeCoachExerciseAdvice.forCoach(rec, "good", good)
        assertFalse(coach.autoApply)
        // A proposed 5.5 kg is not silently written back to the new program.
        assertEquals(5.0, JSONObject(db.programDao().getProgramWithDaysById(programId)!!.program.rawJson)
            .getJSONObject("prescriptions").getJSONObject(id).getDouble("weight"), .00001)
        val onlyTwo = NativeCoachExerciseAdvice.recommend(lib, rx, events.take(2), target, good, .5)
        assertFalse(onlyTwo.autoApply)
    }

    @Test fun portableBackupRoundTripsFourSourceLineageWithoutRewritingHistory() = runBlocking {
        for (i in 1..4) workout("reference-$i", i.toLong())
        val p = preview()
        store.saveTrainingProfile(profile())
        val id = NativeProgramEditor.save(context, db, p.source, p.draft, "hu",
            NativeProfileProgramPreview.metadata(p))
        val backup = importer.exportBackupJson(false)
        val saved = backup.getJSONArray("programs").let { rows ->
            (0 until rows.length()).map(rows::getJSONObject).first { it.getString("id") == id }
        }
        NativeProfileHistory128.validatePortable(saved)
        assertEquals(4, saved.getJSONObject("history128").getJSONObject("entries")
            .getJSONObject(this@NativeProfileHistory128Test.id).getJSONArray("sources").length())
        assertEquals(4, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun invalidDuplicateHistorySourcesRejectRestoreBeforeDeletingExistingProgram() = runBlocking {
        val row = JSONObject().put("id", "imported")
            .put("history128", JSONObject().put("entries",
                JSONObject().put(id, JSONObject().put("metric", "reps")
                    .put("sources", JSONArray()
                        .put(JSONObject().put("programId", old).put("key", "same"))
                        .put(JSONObject().put("programId", old).put("key", "same"))))))
        val backup = JSONObject().put("programs", JSONArray().put(row))
        val before = db.programDao().getProgramIds().toSet()
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            importer.importBackupJson(backup, true)
        } }
        assertEquals(before, db.programDao().getProgramIds().toSet())
    }

    @Test fun changedHistoryInvalidatesPreviewWithoutPublishingNewProgram() = runBlocking {
        workout("approved", weight = 5.0)
        val p = preview()
        store.saveTrainingProfile(profile())
        val changed = db.workoutHistoryDao().getCoachExerciseCandidates(id,
            now.minus(Duration.ofDays(90)).toEpochMilli(), now.toEpochMilli(), null, "", 5).first()
        val edit = JSONObject(changed.rawJson)
        edit.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            .getJSONObject(0).put("reps", "10")
        db.workoutHistoryDao().insertWorkout(changed.copy(rawJson = edit.toString()))
        val before = db.programDao().getProgramIds().toSet()
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            NativeProgramEditor.save(context, db, p.source, p.draft, "hu",
                NativeProfileProgramPreview.metadata(p))
        } }
        assertEquals(before, db.programDao().getProgramIds().toSet())
    }
}
