package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.ui.progress.NativeProgressQuery
import kotlinx.coroutines.runBlocking
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
class NativeProgressQueryTest {
    private lateinit var db: TrainPilotDatabase

    @Before fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun finish() { db.close() }

    private suspend fun insert(i: Int, weight: Double?, reps: String,
        completed: Boolean = true, id: String = "goblet-squat", epoch: Long = i.toLong()) {
        val history = JournalTestFixtures.row(i).copy(
            startedEpochMs = epoch, finishedEpochMs = epoch + 60_000,
            rawJson = "{}")
        db.workoutHistoryDao().insertWorkout(history)
        val parent = db.workoutHistoryDao().insertExercise(WorkoutHistoryExerciseEntity(
            workoutHistoryId = history.id, exerciseId = id,
            nameHu = "Test", nameEn = "Test", loadType = "single_dumbbell",
            repUnit = "ism.", measurementType = "reps", orderIndex = 0,
            actualRir = null, setRir229 = false))
        db.workoutHistoryDao().insertSets(listOf(WorkoutHistorySetEntity(
            setId = "set-$i", workoutHistoryId = history.id, historyExerciseRowId = parent,
            exerciseId = id, setNumber = 1, weight = weight, reps = reps,
            leftSeconds = null, rightSeconds = null, distanceMeters = null,
            done = completed, actualRir = null, gpsRouteKey163 = null, gpsRouteKey193 = null)))
    }

    @Test fun noHistoryReturnsZeroMusclesAndEmptyTrend() = runBlocking {
        val trend = NativeProgressQuery.trend(db.workoutHistoryDao(), "goblet-squat")
        assertTrue(trend.isEmpty())
        val muscle = NativeProgressQuery.muscle(db.workoutHistoryDao()
            .getProgressMuscleSummary(0, Long.MAX_VALUE), "hu")
        assertEquals(6, muscle.size)
        assertTrue(muscle.all { it.setsCount == 0 && it.loadPercentage == 0f })
    }

    @Test fun strengthTrendUsesCompletedPositiveNumericSetsOnly() = runBlocking {
        insert(1, 20.0, "10")
        insert(2, 40.0, "8")
        insert(3, 50.0, "not a rep")
        insert(4, 0.0, "10")
        insert(5, 90.0, "5", completed = false)
        val trend = NativeProgressQuery.trend(db.workoutHistoryDao(), "goblet-squat")
        assertEquals(2, trend.size)
        assertEquals(20.0 * (1.0 + 10.0 / 30.0), trend.first().e1rmKg, 1e-6)
        assertEquals(40.0, trend.last().weightKg, 1e-6)
    }

    @Test fun tombstonesHideProgressRatherThanRevivingDeletedHistory() = runBlocking {
        insert(1, 20.0, "10")
        insert(2, 30.0, "12")
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0002", workoutKey = "key-2", idsJson = "[\"w-0002\"]", deletedAt = 1))
        val trend = NativeProgressQuery.trend(db.workoutHistoryDao(), "goblet-squat")
        assertEquals(1, trend.size)
        assertEquals(20.0, trend.single().weightKg, 1e-6)
    }

    @Test fun projectionCrossesPageBoundaryWithoutDuplicatesOrMissingRows() = runBlocking {
        for (i in 1..600) insert(i, 20.0 + i / 10.0, "8")
        val result = NativeProgressQuery.trend(db.workoutHistoryDao(), "goblet-squat")
        assertEquals(600, result.size)
        assertEquals(20.1, result.first().weightKg, 1e-6)
        assertEquals(80.0, result.last().weightKg, 1e-6)
    }

    @Test fun large1500WorkoutHistoryOnlyReturnsExerciseSpecificProjection() = runBlocking {
        for (i in 1..1500) insert(i, 25.0, "5", id = if (i % 2 == 0) "goblet-squat" else "db-squat")
        val selected = NativeProgressQuery.trend(db.workoutHistoryDao(), "goblet-squat")
        assertEquals(750, selected.size)
        assertTrue(selected.all { it.e1rmKg > 25.0 })
    }

    @Test fun muscleAggregateUsesLast30DaysAndCompletedSetsOnly() = runBlocking {
        val exercise = TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" }
        db.exerciseLibraryDao().upsertExercise(exercise)
        val now = System.currentTimeMillis()
        insert(1, 20.0, "10", epoch = now - 1000)
        insert(2, 25.0, "12", completed = false, epoch = now - 2000)
        insert(3, 30.0, "10", epoch = now - 90L * 86_400_000L)
        val rows = db.workoutHistoryDao().getProgressMuscleSummary(now - 30L * 86_400_000L, now)
        val result = NativeProgressQuery.muscle(rows, "hu")
        assertEquals(1, result.first { it.muscleKey == "legs" }.setsCount)
    }

    @Test fun muscleLabelsAreLocalizedAndCardioMapsToLegs() {
        val raw = listOf(ProgressMuscleSummary("cardio", 4), ProgressMuscleSummary("legs", 2))
        for (lang in listOf("hu", "en", "de", "ro")) {
            val result = NativeProgressQuery.muscle(raw, lang)
            assertEquals(6, result.first { it.muscleKey == "legs" }.setsCount)
            assertTrue(result.all { it.loadPercentage in 0f..1f })
        }
        assertEquals("Beine", NativeProgressQuery.muscle(raw, "de").last().label)
    }
}
