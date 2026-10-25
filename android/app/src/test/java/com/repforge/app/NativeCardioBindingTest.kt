package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.WorkoutHistorySetEntity
import com.repforge.app.ui.progress.NativeCardioProgressAdapter
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Genuine Room + canonical cardio binding regressions, not mockup/UI data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeCardioBindingTest {
    private lateinit var db: TrainPilotDatabase
    private val dao get() = db.workoutHistoryDao()
    private val zone = ZoneId.of("Europe/Budapest")
    private val now = Instant.parse("2026-10-25T20:00:00Z").toEpochMilli()

    @Before fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }

    @After fun tearDown() { db.close() }

    private suspend fun insert(
        i: Int,
        exerciseId: String = "running",
        measurementType: String = "distance",
        meters: Double? = 1000.0,
        reps: String = "60",
        done: Boolean = true,
        rawJson: String = "{}",
        route163: String? = null,
        route193: String? = null,
        finishedIsoOverride: String? = null,
        finishedEpochOverride: Long? = null
    ): Long {
        val finishedEpoch = finishedEpochOverride ?: now - i * 1_000L
        val startedEpoch = finishedEpoch - 60_000L
        val history = JournalTestFixtures.row(i).copy(
            startedEpochMs = startedEpoch,
            finishedEpochMs = finishedEpoch,
            started = Instant.ofEpochMilli(startedEpoch).toString(),
            finished = finishedIsoOverride ?: Instant.ofEpochMilli(finishedEpoch).toString(),
            rawJson = rawJson
        )
        dao.insertWorkout(history)
        val parent = dao.insertExercise(WorkoutHistoryExerciseEntity(
            workoutHistoryId = history.id,
            exerciseId = exerciseId,
            nameHu = if (exerciseId == "running") "Futás" else "Guggolás",
            nameEn = if (exerciseId == "running") "Running" else "Squat",
            loadType = "bodyweight",
            repUnit = "mp",
            measurementType = measurementType,
            orderIndex = 0,
            actualRir = null,
            setRir229 = false
        ))
        dao.insertSets(listOf(WorkoutHistorySetEntity(
            setId = "set-$i", workoutHistoryId = history.id,
            historyExerciseRowId = parent,
            exerciseId = exerciseId, setNumber = 1,
            weight = null, reps = reps, leftSeconds = null, rightSeconds = null,
            distanceMeters = meters, done = done,
            actualRir = null, gpsRouteKey163 = route163, gpsRouteKey193 = route193
        )))
        return parent
    }

    @Test fun zero200And1500RowsLoadAcrossEveryPage() = runBlocking {
        assertEquals(0, NativeCardioProgressAdapter.load(dao, now, zone).totalSetCount)
        for (i in 1..200) insert(i)
        val first = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(200, first.totalSetCount)
        assertEquals(200_000.0, first.totalDistanceMeters, 0.0001)
        for (i in 201..1500) insert(i, reps = if (i == 1500) "20" else "60")
        val all = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(1500, all.totalSetCount)
        assertEquals(1500, all.totalWorkoutCount)
        assertEquals(1_500_000.0, all.totalDistanceMeters, 0.0001)
        assertEquals(1500, all.records.single().count)
        assertEquals("w-1500", all.records.single().best.workoutId) // oldest beats every newer attempt
    }

    @Test fun cursorMovesPastFullPageOfRowsTheModelRejects() = runBlocking {
        for (i in 1..256) insert(i, "goblet-squat", "reps", null, "10")
        insert(257, meters = 750.0)
        val result = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(1, result.totalSetCount)
        assertEquals(750.0, result.totalDistanceMeters, 0.0001)
        assertEquals("w-0257", result.records.single().best.workoutId)
    }

    @Test fun explicitRawSourceAndRoutesDoNotInventMeasurementsAndTombstonesExcludeRows() = runBlocking {
        val sourceJson = """{"exercises":[{"id":"running","sets":[{"set":1,"setId":"set-1","distanceSource":"wear"}]}],"photos":[{"id":"photo","payload":"should-not-be-loaded"}]}"""
        insert(1, meters = 5000.0, reps = "200", rawJson = sourceJson, route193 = "wear193:route")
        insert(2, meters = 5000.0, reps = "100", route163 = "phone163:route")
        insert(3, meters = null, reps = "60")

        val before = NativeCardioProgressAdapter.load(dao, now, zone)
        val record = before.records.single()
        assertEquals("manual", record.best.source)
        assertEquals("wear", record.second!!.source)
        assertEquals("wear193:route", record.second!!.gpsRouteKey193)
        assertEquals(60.0, before.timeOnlyDurationSeconds, 0.0001)

        dao.upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0002", workoutKey = "key-2",
            idsJson = "[\"w-0002\"]", deletedAt = 1
        ))
        val after = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(2, after.totalSetCount)
        assertEquals("wear", after.records.single().best.source)
        assertEquals(5000.0, after.totalDistanceMeters, 0.0001)
        assertEquals(60.0, after.timeOnlyDurationSeconds, 0.0001)
    }


    @Test fun sameCountUpdateChangesDistanceAndSourceWithoutCountCache() = runBlocking {
        insert(1, meters = 4000.0, reps = "1200", rawJson =
            """{"exercises":[{"sets":[{"setId":"set-1","distanceSource":"gps"}]}]}""")
        val old = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(4000.0, old.totalDistanceMeters, 0.0001)
        assertEquals("gps", old.records.single().best.source)
        val projection = dao.pageCardioSets(null, "", Long.MAX_VALUE, 5).single()
        val current = dao.getWorkoutWithDetailsById(projection.workoutId)!!.exercises.single().sets.single()
        dao.insertSets(listOf(current.copy(distanceMeters = 5000.0)))
        val edited = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(old.totalSetCount, edited.totalSetCount)
        assertEquals(5000.0, edited.totalDistanceMeters, 0.0001)
        assertEquals("gps", edited.records.single().best.source)
        assertEquals(projection.setRowId, edited.records.single().best.setRowId)
    }

    @Test fun positiveUndoneSiblingOutsideTheRawFirstPageQualifiesWholeExercise() = runBlocking {
        val library = TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" }
        db.exerciseLibraryDao().upsertExercise(library)
        val parent = insert(1, exerciseId = "goblet-squat", measurementType = "reps",
            meters = null, reps = "45")
        val undone = WorkoutHistorySetEntity(
            setId = "sibling-undone", workoutHistoryId = "w-0001",
            historyExerciseRowId = parent, exerciseId = "goblet-squat", setNumber = 2,
            weight = null, reps = "0", leftSeconds = null, rightSeconds = null,
            distanceMeters = 1600.0, done = false, actualRir = null,
            gpsRouteKey163 = null, gpsRouteKey193 = null
        )
        dao.insertSets(listOf(undone))
        for (i in 3..260) {
            dao.insertSets(listOf(undone.copy(
                rowId = 0, setId = "completed-$i", setNumber = i,
                distanceMeters = null, done = true, reps = "30")))
        }
        // The undone distance sibling is below the 256 most recently inserted raw sets.
        val first = dao.pageCardioSets(null, "", Long.MAX_VALUE, 256)
        assertEquals(256, first.size)
        assertTrue(first.all { it.exerciseHasPositiveDistance })
        val result = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(259, result.totalSetCount) // 1 initial + 258 later done
        assertEquals(45.0 + 258 * 30.0, result.timeOnlyDurationSeconds, 0.0001)
        assertTrue(result.records.isEmpty())
    }

    @Test fun undoneSiblingFlagAppliesAcrossSetsAndBadOrFutureFinishesAreRejected() = runBlocking {
        val lift = TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" }
        db.exerciseLibraryDao().upsertExercise(lift)
        val parent = insert(1, exerciseId = "goblet-squat", measurementType = "reps",
            meters = null, reps = "45")
        dao.insertSets(listOf(WorkoutHistorySetEntity(
            setId = "undone-distance", workoutHistoryId = "w-0001",
            historyExerciseRowId = parent, exerciseId = "goblet-squat", setNumber = 2,
            weight = null, reps = "0", leftSeconds = null, rightSeconds = null,
            distanceMeters = 1200.0, done = false, actualRir = null,
            gpsRouteKey163 = null, gpsRouteKey193 = null
        )))
        insert(2, finishedIsoOverride = "invalid-finish")
        insert(3, finishedEpochOverride = now + 10000L)
        val result = NativeCardioProgressAdapter.load(dao, now, zone)
        assertEquals(1, result.totalSetCount)
        assertEquals(45.0, result.timeOnlyDurationSeconds, 0.0001)
        assertEquals(0.0, result.totalDistanceMeters, 0.0001)
        assertTrue(result.records.isEmpty())
    }
}
