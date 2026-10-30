package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCoachAdviceRepositoryTest {
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var importer: LegacyWebViewMigrator
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val lib = JSONObject("""{"id":"press","loadType":"per_hand","repUnit":"ism.","measurementType":"reps","reps":"8–12","sets":2,"weight":5}""")
    private val rx get() = JSONObject().put("reps", "8–12").put("sets", 2)
    private fun program(basis: JSONObject? = null): JSONObject = JSONObject().put("id", "p")
        .put("prescriptions", JSONObject().put("press", rx)).also {
            if (basis != null) it.put("history128", JSONObject().put("entries", JSONObject().put("press", basis)))
        }
    @Before fun open() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context); settings.resetAllSettingsForEraseLocal()
        importer = LegacyWebViewMigrator(context, db, settings)
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first().copy(id = "press",
            nameHu = "Nyomás", nameEn = "Press", nameDe = "Drücken", nameRo = "Împins", loadType = "per_hand",
            repUnit = "ism.", measurementType = "reps", defaultSets = 2, defaultReps = "8–12", defaultWeight = 5.0, rawJson = lib.toString()))
        val base = TrainPilotDatabase.defaultPrograms().first.first()
        db.programDao().upsertProgram(base.copy(id = "p", rawJson = program().toString()))
        db.programDao().upsertProgramDays(listOf(ProgramDayEntity("p:A", "p", "A", "Nap", "Day", "Tag", "Zi", 0, "[\"press\"]")))
    }
    @After fun close() { db.close() }
    private suspend fun workout(id: String, days: Long, pid: String = "p", variation: String = "", weights: List<Double> = listOf(10.0,10.0)) {
        val end = now.minus(Duration.ofDays(days))
        val sets = JSONArray()
        weights.forEachIndexed { index, weight -> sets.put(JSONObject().put("setId", "$id-$index").put("done", true).put("weight", weight).put("reps", "12")) }
        val exercise = JSONObject(lib.toString()).put("effort", "good").put("sets", sets)
            .put("prescription", JSONObject().put("variation", variation))
        importer.saveWorkoutJsonToRoom(JSONObject().put("id", id).put("syncId", id).put("workoutId", id).put("programId", pid)
            .put("started", end.minusSeconds(60).toString()).put("finished", end.toString())
            .put("exercises", JSONArray().put(exercise)))
    }
    private fun ready() = NativeCoachReadiness.calculate(JSONObject(), JSONObject(), emptyList(), now, ZoneId.of("UTC"),
        JSONObject().put("energyScore", 87).put("energyScoreSource", "samsung_health").put("energyScoreTime", now.toString()).put("day", "2026-10-24"), true)
    @Test fun boundedPagesSkipUnrelatedAndIncompatibleHistoryAndStopAfterFourCompatibleOccurrences() = runBlocking {
        for (i in 0 until 40) workout("skip-$i", i + 1L, if (i % 2 == 0) "other" else "p", if (i % 2 == 0) "" else "paused")
        for (i in 0 until 5) workout("match-$i", 41 + i.toLong())
        val rows = NativeCoachAdviceRepository(db).recent("press", "p", lib, rx, program(), now)
        assertEquals(listOf("match-0", "match-1", "match-2", "match-3"), rows.map { it.first.getString("id") })
        assertEquals(45, db.workoutHistoryDao().getWorkoutCount())
    }
    @Test fun inheritedHistoryRequiresBothCompatibleBasisAndExactApprovedSourceIdentity() = runBlocking {
        workout("approved", 1, "old"); workout("unapproved", 2, "old")
        val basis = JSONObject().put("loadType", "per_hand").put("variation", "").put("metric", "reps")
            .put("sources", JSONArray().put(JSONObject().put("programId", "old").put("key", "approved")))
        val repository = NativeCoachAdviceRepository(db)
        val accepted = repository.recent("press", "p", lib, rx, program(basis), now)
        assertEquals(listOf("approved"), accepted.map { it.first.getString("id") })
        basis.put("variation", "paused")
        assertTrue(repository.recent("press", "p", lib, rx, program(basis), now).isEmpty())
    }
    @Test fun recentHistoryExcludesFutureOldAndDeletedWorkoutsBeforeAdvice() = runBlocking {
        workout("old", 91); workout("future", -1); workout("deleted", 1); workout("valid", 2)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity("deleted", "deleted", "[\"deleted\"]", now.toEpochMilli()))
        val rows = NativeCoachAdviceRepository(db).recent("press", "p", lib, rx, program(), now)
        assertEquals(listOf("valid"), rows.map { it.first.getString("id") })
    }
    @Test fun customMissingLoadAndDifferentMetricCannotBeMistakenForCompatibleMeasurements() {
        val exercise = JSONObject().put("id", "press").put("sets", JSONArray().put(JSONObject().put("done", true)))
        assertTrue(NativeCoachAdviceRepository.compatible(exercise, lib, rx))
        assertFalse(NativeCoachAdviceRepository.compatible(exercise, JSONObject(lib.toString()).put("custom", true), rx))
        exercise.put("loadType", "per_hand").put("repUnit", "mp")
        assertFalse(NativeCoachAdviceRepository.compatible(exercise, lib, rx))
    }
    @Test fun observedWeightsArePagedAcross128DistinctValuesWithoutImportingOtherProgramPerformance() = runBlocking {
        workout("observation", 1, "other", weights = (0..128).map { 5.0 + it * .5 })
        workout("one", 2); workout("two", 3); workout("three", 4)
        val advice = NativeCoachAdviceRepository(db).load(CoachTarget("p", "A", "Day"), settings.current(), ready(), now).single()
        assertEquals("increase", advice.advice.action)
        assertEquals(10.5, advice.advice.weight, .00001)
        assertEquals(3, advice.advice.historyCount)
        assertFalse(advice.advice.autoApply)
    }
    @Test fun keysetPagesWithEqualFinishTimesHaveNoDuplicatesOrMissingWorkouts() = runBlocking {
        for (i in 0 until 65) workout("same-%03d".format(i), 1)
        val ids = mutableListOf<String>(); var before: Long? = null; var key = ""
        while (true) {
            val page = db.workoutHistoryDao().getCoachExerciseCandidates("press", now.minus(Duration.ofDays(90)).toEpochMilli(), now.toEpochMilli(), before, key, 32)
            assertTrue(page.size <= 32)
            ids.addAll(page.map { it.id })
            if (page.size < 32) break
            before = page.last().finishedEpochMs; key = page.last().id
        }
        assertEquals(65, ids.size); assertEquals(65, ids.distinct().size)
    }
}
