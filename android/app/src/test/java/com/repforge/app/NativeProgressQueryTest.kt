package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.*
import com.repforge.app.ui.progress.NativeProgressQuery
import com.repforge.app.ui.progress.NativeVolumeProjection
import com.repforge.app.ui.progress.NativeVolumeRange
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
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

    @Test fun thirtyDayCalendarWindowHonorsBudapestDstAndExcludesFutureWorkouts() {
        val zone = ZoneId.of("Europe/Budapest")
        val now = LocalDate.of(2026, 10, 27).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (start, until) = NativeProgressQuery.thirtyDayWindow(now, zone)
        assertEquals(LocalDate.of(2026, 9, 28).atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(now + 1, until)
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),
            java.time.Instant.ofEpochMilli(until - 1).atZone(zone).toLocalDate()) + 1)
        // DST ended Oct 25, so 30 calendar dates are NOT exactly 30 * 24 hours.
        assertEquals(29L * 86_400_000L + 3_600_000L + 12L * 3_600_000L + 1L,
            until - start)
    }

    @Test fun periodTotalsArePersistedTombstoneAwareAndIgnoreFutureOrIncomplete() = runBlocking {
        val zone = ZoneId.of("Europe/Budapest")
        val now = LocalDate.of(2026, 10, 27).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (start, until) = NativeProgressQuery.thirtyDayWindow(now, zone)
        suspend fun add(
            i: Int,
            finished: Long,
            volume: Double,
            sets: Int,
            pr: Boolean,
            duration: Int = 60,
            prRecords: String = "[]",
            strength: Boolean = true,
            complete: Boolean = true
        ) {
            val history = JournalTestFixtures.row(i).copy(
                startedEpochMs = if (complete) finished - duration * 1000L else finished,
                finishedEpochMs = finished,
                durationSeconds = if (complete) duration else 0,
                totalVolumeKg = volume,
                completedSetsCount = sets,
                hasPr226 = pr,
                prRecordsJson226 = prRecords
            )
            db.workoutHistoryDao().insertWorkout(history)
            db.workoutHistoryDao().insertExercise(
                WorkoutHistoryExerciseEntity(
                    workoutHistoryId = history.id,
                    exerciseId = "progress-$i",
                    nameHu = "Teszt",
                    nameEn = "Test",
                    loadType = if (strength) "dumbbell" else "bodyweight",
                    repUnit = if (strength) "ism." else "km",
                    measurementType = if (strength) "strength" else "distance",
                    orderIndex = 0,
                    actualRir = null,
                    setRir229 = false
                )
            )
        }
        add(1, start + 90_000, 125.0, 4, true, duration = 1800,
            prRecords = """[{"id":"pr-a"},{"id":"pr-b"}]""")
        add(2, now - 90_000, 225.5, 6, false, duration = 3600)
        add(3, now - 180_000, 999.0, 5, true, duration = 2400,
            prRecords = """[{"id":"deleted"}]""")
        add(4, start - 1, 800.0, 8, true)
        add(5, now + 1000, 700.0, 7, true)
        add(6, now - 300_000, 300.0, 3, true, complete = false)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0003", workoutKey = "key-3", idsJson = "[\"w-0003\"]", deletedAt = 1))
        val totals = db.workoutHistoryDao().getProgressPeriodTotals(start, until)
        assertEquals(2, totals.workoutCount)
        assertEquals(350.5, totals.totalVolumeKg, 1e-6)
        assertEquals(10, totals.completedSets)
        assertEquals(1, totals.prWorkouts)
        assertEquals(2, totals.strengthWorkoutCount)
        assertEquals(2, totals.durationWorkoutCount)
        assertEquals(5400L, totals.totalDurationSeconds)
        val prRows = db.workoutHistoryDao().getProgressPeriodPrRecordJson(start, until)
        assertEquals(
            2,
            com.repforge.app.ui.progress.NativeProgressPeriodProjection.countPrRecords(prRows)
        )
        assertEquals(
            2,
            com.repforge.app.ui.progress.NativeProgressPeriodProjection.countPrRecords(
                listOf("""[{"id":"a"},{"id":"b"}]""", "not-json", "[]")
            )
        )
    }

    @Test fun volumeRangesRespectLocalCalendarAndDstChange() {
        val zone = ZoneId.of("Europe/Budapest")
        val today = LocalDate.of(2026, 10, 27)
        val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(today, NativeVolumeProjection.dateBounds(NativeVolumeRange.DAY, now, zone).first)
        assertEquals(today.minusDays(6),
            NativeVolumeProjection.dateBounds(NativeVolumeRange.WEEK, now, zone).first)
        assertEquals(today.minusDays(29),
            NativeVolumeProjection.dateBounds(NativeVolumeRange.MONTH, now, zone).first)
        assertEquals(today.minusMonths(3).plusDays(1),
            NativeVolumeProjection.dateBounds(NativeVolumeRange.THREE_MONTHS, now, zone).first)
        // The local DST-end day Oct 25 was 25 hours long.
        val dstDay = LocalDate.of(2026, 10, 25)
        val dstEnd = dstDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val bounds = NativeVolumeProjection.queryBounds(NativeVolumeRange.DAY, dstEnd, zone)
        assertEquals(25L * 60 * 60 * 1000, bounds.second - bounds.first)
        assertEquals(12, NativeVolumeRange.THREE_MONTHS.barCount)
    }

    @Test fun volumeChartUsesSavedNonDeletedFinishesAndNeverFakeBars() = runBlocking {
        val zone = ZoneId.of("Europe/Budapest")
        val now = LocalDate.of(2026, 10, 27).atTime(12, 0)
            .atZone(zone).toInstant().toEpochMilli()
        val start = LocalDate.of(2026, 10, 21).atStartOfDay(zone).toInstant().toEpochMilli()
        suspend fun record(i: Int, whenFinished: Long, kg: Double, complete: Boolean = true) {
            db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(i).copy(
                startedEpochMs = if (complete) whenFinished - 120_000 else whenFinished,
                finishedEpochMs = whenFinished, totalVolumeKg = kg
            ))
        }
        record(100, start + 1000, 100.0)
        record(101, now - 9000, 250.5)
        record(102, now - 8000, 999.0)
        record(103, start - 1, 888.0)
        record(104, now + 1, 777.0)
        record(105, now - 6000, 555.0, complete = false)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0102", workoutKey = "key-102", idsJson = "[\"w-0102\"]", deletedAt = 1))
        val (from, until) = NativeVolumeProjection.queryBounds(NativeVolumeRange.WEEK, now, zone)
        val rows = db.workoutHistoryDao().getProgressVolumeRows(from, until)
        assertEquals(2, rows.size)
        val snapshot = NativeVolumeProjection.summarize(NativeVolumeRange.WEEK, now, zone, rows)
        assertEquals(7, snapshot.buckets.size)
        assertEquals(2, snapshot.workoutCount)
        assertEquals(350.5, snapshot.totalVolumeKg, 0.000001)
        assertEquals(100.0, snapshot.buckets.first().volumeKg, 0.000001)
        assertEquals(250.5, snapshot.buckets.last().volumeKg, 0.000001)
        assertEquals(snapshot.totalVolumeKg, snapshot.buckets.sumOf { it.volumeKg }, 0.000001)
        val blank = NativeVolumeProjection.summarize(
            NativeVolumeRange.WEEK, now, zone, emptyList())
        assertEquals(0.0, blank.totalVolumeKg, 0.0)
        assertTrue(blank.buckets.all { it.volumeKg == 0.0 })
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

    @Test fun studioMusclePeriodsUseRealSetsCalendarBoundariesAndTombstones() = runBlocking {
        db.exerciseLibraryDao().upsertExercise(TrainPilotDatabase.defaultExercises().first { it.id == "goblet-squat" })
        val zone = ZoneId.of("Europe/Budapest")
        val today = LocalDate.of(2026, 10, 27)
        val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        suspend fun add(i: Int, days: Long, done: Boolean = true) = insert(i, 20.0, "10", done,
            epoch = today.minusDays(days).atTime(1, 0).atZone(zone).toInstant().toEpochMilli())
        add(1, 0); add(2, 6); add(3, 29); add(4, 60); add(5, 150)
        add(6, 0, false); add(7, 0); add(8, -1)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            primaryId = "w-0007", workoutKey = "key-7", idsJson = "[\"w-0007\"]", deletedAt = 1))
        for ((range, expected) in listOf(NativeVolumeRange.DAY to 1, NativeVolumeRange.WEEK to 2,
            NativeVolumeRange.MONTH to 3, NativeVolumeRange.THREE_MONTHS to 4)) {
            val (from, until) = NativeVolumeProjection.queryBounds(range, now, zone)
            val loads = NativeProgressQuery.muscle(db.workoutHistoryDao()
                .getProgressMuscleSummary(from, until - 1), "de")
            assertEquals(expected, loads.first { it.muscleKey == "legs" }.setsCount)
            assertEquals(1f, loads.first { it.muscleKey == "legs" }.loadPercentage)
            assertTrue(loads.filter { it.muscleKey != "legs" }.all { it.setsCount == 0 })
        }
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
    @Test fun selectedWindowFiltersBoundaryDaysAndFutureBadDatesWithoutChangingFormula() = runBlocking {
        val zone = ZoneId.of("Europe/Budapest")
        val day = LocalDate.of(2026, 10, 26)
        val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val first30 = day.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
        insert(1, 30.0, "10", epoch = first30)
        insert(2, 50.0, "10", epoch = first30 - 1)
        insert(3, 60.0, "10", epoch = now + 60_000)
        insert(4, 70.0, "10", epoch = now - 60_000)
        val thirty = NativeProgressQuery.trend(db.workoutHistoryDao(),
            "goblet-squat", com.repforge.app.ui.progress.NativeStrengthRange.DAYS_30, now, zone)
        assertEquals(2, thirty.size)
        assertEquals(listOf(30.0, 70.0), thirty.map { it.weightKg })
        val all = NativeProgressQuery.trend(db.workoutHistoryDao(),
            "goblet-squat", com.repforge.app.ui.progress.NativeStrengthRange.ALL, now, zone)
        assertEquals(3, all.size) // one future measurement is excluded even for ALL
        assertEquals(70.0 * (1 + 10.0 / 30.0), thirty.last().e1rmKg, 0.000001)
    }

    @Test fun twoHundredRecentSetsRemainedVisibleWhenThirteenHundredOlderSetsExist() = runBlocking {
        val zone = ZoneId.of("Europe/Budapest")
        val now = LocalDate.of(2026, 10, 26).atTime(12, 0)
            .atZone(zone).toInstant().toEpochMilli()
        for (i in 1..1500) {
            val epoch = if (i <= 200) now - i * 10_000L
                        else now - 150L * 86_400_000L - i * 1000L
            insert(i, 25.0, "5", epoch = epoch)
        }
        val filtered = NativeProgressQuery.trend(db.workoutHistoryDao(),
            "goblet-squat", com.repforge.app.ui.progress.NativeStrengthRange.DAYS_90, now, zone)
        assertEquals(200, filtered.size)
    }

}
