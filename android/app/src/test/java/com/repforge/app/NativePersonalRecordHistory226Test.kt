package com.repforge.app

import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativePersonalRecordHistory226Test {
    private lateinit var db: TrainPilotDatabase
    private val queries = ConcurrentLinkedQueue<String>()
    private val base = Instant.parse("2026-10-20T00:00:00Z")
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> queries.add(sql) }, Executor { it.run() }).build()
    }
    @After fun close() { db.close() }

    private suspend fun prior(i: Int, id: String = "press", unit: String = "ism.", weight: Double? = 10.0,
        reps: String = "8", done: Boolean = true, distance: Double? = null, left: Int? = null, right: Int? = null,
        workoutId: String? = null, started: String? = null): WorkoutHistoryEntity {
        val row = JournalTestFixtures.row(i).let { it.copy(id = workoutId ?: it.id,
            started = started ?: base.plusSeconds(i.toLong()).toString()) }
        val exercise = WorkoutHistoryExerciseEntity(workoutHistoryId = row.id, exerciseId = id, nameHu = id, nameEn = id,
            loadType = "total", repUnit = unit, measurementType = if (distance != null) "distance" else "reps",
            orderIndex = 0, actualRir = null, setRir229 = false)
        val set = WorkoutHistorySetEntity(setId = "s-$i", workoutHistoryId = row.id, historyExerciseRowId = 0,
            exerciseId = id, setNumber = 0, weight = weight, reps = reps, leftSeconds = left, rightSeconds = right,
            distanceMeters = distance, done = done, actualRir = null, gpsRouteKey163 = null, gpsRouteKey193 = null)
        db.workoutHistoryDao().saveFullWorkout(row, listOf(exercise to listOf(set)), emptyList())
        return row
    }
    private fun exercise(id: String = "press", unit: String = "ism.", weight: Any = 12.0, reps: String = "8",
        distance: Double = 0.0, left: Int = 0, right: Int = 0) = JSONObject().put("id", id).put("hu", id)
        .put("repUnit", unit).put("measurementType", if (unit in listOf("km", "m")) "distance" else "reps")
        .put("sets", JSONArray().put(JSONObject().put("done", true).put("weight", weight).put("reps", reps)
            .put("distanceMeters", distance).put("leftSeconds", left).put("rightSeconds", right)))
    private fun workout(vararg exercises: JSONObject) = JSONObject().put("id", "current")
        .put("started", "2026-10-25T12:00:00Z").put("exercises", JSONArray(exercises.toList()))
    private suspend fun records(input: JSONObject) = PersonalRecordCalculator226.evaluateAndAttachPrs(input, db)
    private suspend fun equivalent(input: JSONObject): JSONArray {
        val referenceInput = JSONObject(input.toString())
        val reference = PersonalRecordCalculator226.evaluateAndAttachPrs(referenceInput, db.workoutHistoryDao().getAllWorkoutsWithDetails())
        queries.clear()
        val actual = records(input)
        assertEquals(reference.toString(), actual.toString()); assertEquals(referenceInput.toString(), input.toString())
        return actual
    }

    @Test fun zeroTwoHundredAndFifteenHundredScopeHistoryAndKeepEachProjectionReadBounded() = runBlocking {
        for (count in listOf(0, 200, 1500)) {
            db.workoutHistoryDao().deleteAllWorkouts()
            db.withTransaction { repeat(count) { prior(it, id = if (it % 2 == 0) "press" else "unrelated") } }
            val result = equivalent(workout(exercise()))
            assertEquals(if (count == 0) 0 else 1, result.length())
            val reads = queries.filter { it.trimStart().startsWith("SELECT", true) && "workout_history" in it }
            assertTrue(reads.isNotEmpty())
            assertTrue(reads.all { it.contains("LIMIT", true) && !it.contains("rawJson") && !it.contains("SELECT *") })
            assertTrue(reads.none { "workout_photos" in it })
        }
    }
    @Test fun recordFromTheOldestRelevantPageStillSuppressesANewerLowerWeight() = runBlocking {
        db.withTransaction { repeat(1500) { prior(it, weight = if (it == 0) 100.0 else 10.0) } }
        assertEquals(0, equivalent(workout(exercise(weight = 99.0))).length())
        assertEquals("maxWeight", equivalent(workout(exercise(weight = 101.0))).getJSONObject(0).getString("kind"))
    }
    @Test fun maximumWeightTakesPriorityOverTheIndependentE1RmRecord() = runBlocking {
        prior(0, weight = 10.0, reps = "20"); prior(1, weight = 15.0, reps = "1")
        val result = equivalent(workout(exercise(weight = 16.0, reps = "1"))).getJSONObject(0)
        assertEquals("maxWeight", result.getString("kind")); assertEquals("15.0 kg", result.getString("previous"))
    }
    @Test fun e1RmUsesTheExistingPointTwoMarginAndDoesNotCelebrateATie() = runBlocking {
        prior(0, weight = 10.0, reps = "10")
        assertEquals(0, equivalent(workout(exercise(weight = 10.0, reps = "10"))).length())
        assertEquals(0, equivalent(workout(exercise(weight = 9.9, reps = "11"))).length())
        assertEquals("e1rm", equivalent(workout(exercise(weight = 9.91, reps = "11"))).getJSONObject(0).getString("kind"))
        assertEquals("e1rm", equivalent(workout(exercise(weight = 10.0, reps = "11"))).getJSONObject(0).getString("kind"))
    }
    @Test fun repetitionRecordsAndCommaDecimalsUseTheSameKotlinRounding() = runBlocking {
        prior(0, id = "pushup", weight = null, reps = "10,5")
        assertEquals(0, equivalent(workout(exercise("pushup", weight = 0, reps = "11"))).length())
        val result = equivalent(workout(exercise("pushup", weight = 0, reps = "11,6"))).getJSONObject(0)
        assertEquals("reps", result.getString("kind")); assertEquals("11 ism.", result.getString("previous"))
    }
    @Test fun timedAndBilateralRecordsPreserveSideMaximaAndLegacyRepsFallback() = runBlocking {
        prior(0, id = "plank", unit = "mp", weight = 0.0, reps = "30")
        prior(1, id = "side-plank", unit = "mp / oldal", weight = 0.0, reps = "35", left = 20, right = 40)
        val result = equivalent(workout(exercise("plank", "mp", 0, "31"),
            exercise("side-plank", "mp / oldal", 0, "20", left = 41, right = 39)))
        assertEquals(listOf("time", "time"), (0 until result.length()).map { result.getJSONObject(it).getString("kind") })
        assertEquals("40 mp", result.getJSONObject(1).getString("previous"))
    }
    @Test fun distanceTakesPrecedenceOverTimeAndRemainsIndependentOfAnUnrelatedExercise() = runBlocking {
        prior(0, id = "run", unit = "km", weight = 0.0, reps = "600", distance = 1000.0)
        prior(1, id = "other-run", unit = "km", weight = 0.0, distance = 10000.0)
        val record = equivalent(workout(exercise("run", "km", 0, "1", distance = 1100.0))).getJSONObject(0)
        assertEquals("distance", record.getString("kind")); assertEquals("1.0 km", record.getString("previous"))
    }
    @Test fun incompletePriorAndCurrentSetsDoNotCreateOrSuppressRecords() = runBlocking {
        prior(0, weight = 100.0, done = false); prior(1, weight = 10.0)
        assertEquals("maxWeight", equivalent(workout(exercise(weight = 12.0))).getJSONObject(0).getString("kind"))
        val current = exercise(weight = 200.0)
        current.getJSONArray("sets").getJSONObject(0).put("done", false)
        assertEquals(0, equivalent(workout(current)).length())
    }
    @Test fun firstOccurrenceDoesNotAwardARecordAndDuplicateExerciseEntriesKeepOutputOrder() = runBlocking {
        assertEquals(0, equivalent(workout(exercise())).length())
        prior(0)
        val result = equivalent(workout(exercise(weight = 11.0), exercise(weight = 12.0)))
        assertEquals(2, result.length()); assertTrue(result.getJSONObject(0).getString("value").startsWith("11.0"))
        assertTrue(result.getJSONObject(1).getString("value").startsWith("12.0"))
    }
    @Test fun currentIdentityAndEqualStartTimestampAreExcludedOnRepeatedEvaluation() = runBlocking {
        prior(0, weight = 100.0, workoutId = "current")
        prior(1, weight = 200.0, started = "2026-10-25T12:00:00Z"); prior(2, weight = 10.0)
        val first = equivalent(workout(exercise()))
        assertEquals("maxWeight", first.getJSONObject(0).getString("kind"))
        assertEquals(first.toString(), equivalent(workout(exercise())).toString())
    }
    @Test fun tombstonedRowsCannotSuppressANewPersonalRecordEvenWhenTheRowStillExists() = runBlocking {
        val marked = prior(0, weight = 100.0); prior(1, weight = 10.0)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(marked.id, marked.workoutKey, "[]", 1))
        assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails(marked.id))
        val record = records(workout(exercise())).getJSONObject(0)
        assertEquals("maxWeight", record.getString("kind")); assertEquals("10.0 kg", record.getString("previous"))
    }
    @Test fun onlyTombstonedHistoryStillCountsAsAFirstOccurrence() = runBlocking {
        val marked = prior(0)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(marked.syncId, "", "[]", 1))
        assertEquals(0, records(workout(exercise())).length())
    }
    @Test fun moreThanOneSqliteIdChunkIsHandledWithoutDroppingExercises() = runBlocking {
        db.withTransaction { repeat(260) { prior(it, id = "exercise-$it") } }
        val result = equivalent(workout(*(0 until 260).map { exercise("exercise-$it") }.toTypedArray()))
        assertEquals(260, result.length())
        assertEquals("exercise-259", result.getJSONObject(259).getString("exerciseId"))
    }
    @Test fun parentExerciseIdentityWinsOverAnInconsistentRedundantSetExerciseId() = runBlocking {
        prior(0)
        db.openHelper.writableDatabase.execSQL("UPDATE workout_history_sets SET exerciseId='inconsistent'")
        assertEquals("maxWeight", equivalent(workout(exercise())).getJSONObject(0).getString("kind"))
    }
    @Test fun deletionAndSameCountReplacementAreReevaluatedWithoutACountCache() = runBlocking {
        prior(0, weight = 100.0); prior(1, weight = 10.0)
        assertEquals(0, equivalent(workout(exercise())).length())
        db.workoutHistoryDao().deleteWorkoutByIdOrKey("w-0000"); prior(0, weight = 11.0)
        val result = equivalent(workout(exercise()))
        assertEquals("maxWeight", result.getJSONObject(0).getString("kind"))
        assertEquals(2, db.workoutHistoryDao().getWorkoutCount())
    }
    @Test fun missingExercisesSkipsHistoricalReadsAndPreservesExistingUnknownFields() = runBlocking {
        val input = JSONObject().put("id", "current").put("unknown", "keep")
        queries.clear(); assertEquals(0, records(input).length())
        assertEquals("keep", input.getString("unknown"))
        assertTrue(queries.none { it.trimStart().startsWith("SELECT", true) && it.contains("FROM workout_history", true) })
    }
}
