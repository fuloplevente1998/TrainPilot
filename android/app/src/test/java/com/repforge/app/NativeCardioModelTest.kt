package com.repforge.app

import com.repforge.app.ui.progress.CardioSetRow
import com.repforge.app.ui.progress.NativeCardioModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NativeCardioModelTest {

    private val budapestZone: ZoneId = ZoneId.of("Europe/Budapest")
    private val utcZone: ZoneId = ZoneId.of("UTC")

    @Test
    fun emptyInput_returnsZeroTotalsNullPaceAndEightEmptyCalendarWeeks() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 21, 14, 30, 0, 0, budapestZone).toInstant().toEpochMilli()
        val result = NativeCardioModel.compute(emptyList(), nowEpochMs = nowEpochMs, zoneId = budapestZone)

        assertNull(result.selectedExerciseId)
        assertEquals(0.0, result.totalDistanceMeters, 1e-6)
        assertEquals(0.0, result.totalDurationSeconds, 1e-6)
        assertEquals(0.0, result.timeOnlyDurationSeconds, 1e-6)
        assertNull(result.overallPaceSecondsPerKm)
        assertEquals(0, result.totalSetCount)
        assertEquals(0, result.totalWorkoutCount)
        assertTrue(result.records.isEmpty())
        assertTrue(result.activities.isEmpty())

        assertEquals(8, result.weeks.size)
        assertEquals(7, result.weeks.first().weeksAgo)
        assertEquals(0, result.weeks.last().weeksAgo)
        assertEquals(result.currentWeek, result.weeks.last())
        assertEquals(result.previousWeek, result.weeks[6])
        assertEquals(LocalDate.of(2026, 10, 19), result.currentWeek.weekStartDate)
        assertEquals(LocalDate.of(2026, 10, 12), result.previousWeek.weekStartDate)
        assertEquals(LocalDate.of(2026, 8, 31), result.weeks.first().weekStartDate)
        for (week in result.weeks) {
            assertEquals(0.0, week.meters, 1e-6)
            assertEquals(0.0, week.seconds, 1e-6)
            assertNull(week.pace)
        }
        assertNull(NativeCardioModel.tp107DistanceStats("running", emptyList(), nowEpochMs, budapestZone))
    }

    @Test
    fun multipleWeeksAndActivities_aggregatesAllTimeAndEightWeekWindowAndFiltersByActivity() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 22, 18, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        fun epochAt(y: Int, m: Int, d: Int, h: Int = 10): Long =
            ZonedDateTime.of(y, m, d, h, 0, 0, 0, budapestZone).toInstant().toEpochMilli()

        val rows = listOf(
            CardioSetRow("w-curr-run", "s-1", startedEpochMs = epochAt(2026, 10, 20), exerciseId = "running",
                exerciseNameHu = "Futás", distanceMeters = 5000.0, reps = "1500"),
            CardioSetRow("w-curr-bike", "s-2", startedEpochMs = epochAt(2026, 10, 21), exerciseId = "cycling",
                exerciseNameHu = "Kerékpár", distanceMeters = 20000.0, reps = "2400"),
            CardioSetRow("w-prev-run", "s-3", startedEpochMs = epochAt(2026, 10, 14), exerciseId = "running",
                distanceMeters = 10000.0, reps = "3100"),
            CardioSetRow("w-3w-walk", "s-4", startedEpochMs = epochAt(2026, 9, 30), exerciseId = "walking",
                distanceMeters = 2000.0, reps = "1200"),
            CardioSetRow("w-7w-bike", "s-5", startedEpochMs = epochAt(2026, 9, 2), exerciseId = "cycling",
                distanceMeters = 15000.0, reps = "1800"),
            CardioSetRow("w-10w-run", "s-6", startedEpochMs = epochAt(2026, 8, 12), exerciseId = "running",
                distanceMeters = 3000.0, reps = "840")
        )

        val allResult = NativeCardioModel.compute(rows, nowEpochMs, budapestZone, NativeCardioModel.ALL_ACTIVITIES)
        assertEquals(55000.0, allResult.totalDistanceMeters, 1e-6)
        assertEquals(10840.0, allResult.totalDurationSeconds, 1e-6)
        assertEquals(10840.0 / 55.0, allResult.overallPaceSecondsPerKm ?: 0.0, 1e-6)
        assertEquals(25000.0, allResult.currentWeek.meters, 1e-6)
        assertEquals(10000.0, allResult.previousWeek.meters, 1e-6)
        assertEquals(2000.0, allResult.weekByOffset(3)!!.meters, 1e-6)
        assertEquals(15000.0, allResult.weekByOffset(7)!!.meters, 1e-6)
        assertEquals(52000.0, allResult.weeks.sumOf { it.meters }, 1e-6)

        val runResult = NativeCardioModel.compute(rows, nowEpochMs, budapestZone, "running")
        assertEquals("running", runResult.selectedExerciseId)
        assertEquals(18000.0, runResult.totalDistanceMeters, 1e-6)
        assertEquals(5440.0, runResult.totalDurationSeconds, 1e-6)
        assertEquals(5440.0 / 18.0, runResult.overallPaceSecondsPerKm ?: 0.0, 1e-6)
        assertEquals(5000.0, runResult.currentWeek.meters, 1e-6)
        assertEquals(10000.0, runResult.previousWeek.meters, 1e-6)
        assertEquals(3, runResult.records.size)
        assertEquals(listOf("running", "cycling", "walking"), runResult.activities.map { it.exerciseId })

        val runStats = NativeCardioModel.tp107DistanceStats("running", rows, nowEpochMs, budapestZone)
        assertNotNull(runStats)
        assertEquals(3, runStats!!.count)
        assertEquals(3, runStats.sessions)
        assertEquals("w-curr-run", runStats.latest.workoutId)
        assertEquals("w-10w-run", runStats.best.workoutId)
    }

    @Test
    fun weightedPace_usesSummedTimeOverSummedKilometersOfPacedRowsOnly() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val rows = listOf(
            CardioSetRow("w-1", "s-1", startedEpochMs = nowEpochMs - 3_600_000L, exerciseId = "running",
                distanceMeters = 1000.0, reps = "360"),
            CardioSetRow("w-1", "s-2", startedEpochMs = nowEpochMs - 3_600_000L, exerciseId = "running",
                distanceMeters = 9000.0, reps = "2340"),
            CardioSetRow("w-2", "s-3", startedEpochMs = nowEpochMs - 1_800_000L, exerciseId = "running",
                distanceMeters = 5000.0, reps = "0"),
            CardioSetRow("w-3", "s-4", startedEpochMs = nowEpochMs - 900_000L, exerciseId = "running",
                distanceMeters = null, reps = "1800")
        )

        val result = NativeCardioModel.compute(rows, nowEpochMs = nowEpochMs, zoneId = budapestZone)
        assertEquals(15000.0, result.totalDistanceMeters, 1e-6)
        assertEquals(4500.0, result.totalDurationSeconds, 1e-6)
        assertEquals(10000.0, result.pacedDistanceMeters, 1e-6)
        assertEquals(2700.0, result.pacedDurationSeconds, 1e-6)
        assertEquals(1800.0, result.timeOnlyDurationSeconds, 1e-6)
        assertEquals(270.0, result.overallPaceSecondsPerKm ?: 0.0, 1e-6)
        assertEquals("4:30 /km", NativeCardioModel.tp107FormatPace(result.overallPaceSecondsPerKm))
        assertEquals(270.0, result.currentWeek.pace ?: 0.0, 1e-6)
    }

    @Test
    fun timeOnlyRows_accumulateTimeOnlyAndTotalDurationWithoutInventingPaceOrRecords() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 21, 15, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val rows = listOf(
            CardioSetRow("w-1", "s-1", startedEpochMs = nowEpochMs - 7_200_000L, exerciseId = "cycling",
                distanceMeters = null, reps = "1200"),
            CardioSetRow("w-1", "s-2", startedEpochMs = nowEpochMs - 7_200_000L, exerciseId = "cycling",
                distanceMeters = 0.0, reps = "600"),
            CardioSetRow("w-2", "s-3", startedEpochMs = nowEpochMs - 3_600_000L, exerciseId = "running",
                distanceMeters = null, durationSeconds = 300.0)
        )

        val result = NativeCardioModel.compute(rows, nowEpochMs = nowEpochMs, zoneId = budapestZone)
        assertEquals(0.0, result.totalDistanceMeters, 1e-6)
        assertEquals(2100.0, result.timeOnlyDurationSeconds, 1e-6)
        assertEquals(2100.0, result.totalDurationSeconds, 1e-6)
        assertNull(result.overallPaceSecondsPerKm)
        assertEquals("—", NativeCardioModel.tp107FormatPace(result.overallPaceSecondsPerKm))
        assertNull(result.currentWeek.pace)
        assertTrue(result.records.isEmpty())
    }

    @Test
    fun distanceRecords_groupsByActivityAndRoundedMetersWithMainTieBreakingAndSourcePreservation() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 21, 18, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val rows = listOf(
            // Newer 1200s attempt encountered first in list (e.g. DESC query order)
            CardioSetRow("w-tie-newer", "set-b", setRowId = 20L, startedEpochMs = nowEpochMs - 20_000L,
                exerciseId = "running", distanceMeters = 5000.4, reps = "1200",
                distanceSource = "gps", gpsRouteKey193 = "route-watch-193-b"),
            // Older 1200s attempt encountered second: `a.seconds - b.seconds || a.date - b.date` picks older date first
            CardioSetRow("w-tie-older", "set-a", setRowId = 10L, startedEpochMs = nowEpochMs - 50_000L,
                exerciseId = "running", distanceMeters = 4999.6, reps = "1200",
                distanceSource = "gps", gpsRouteKey163 = "route-phone-163-a"),
            // Same timestamp AND same seconds as w-tie-newer -> stable encounter order keeps w-tie-newer ahead
            CardioSetRow("w-tie-same-ts", "set-c", setRowId = 30L, startedEpochMs = nowEpochMs - 20_000L,
                exerciseId = "running", distanceMeters = 5000.0, reps = "1200",
                distanceSource = "manual"),
            // Slower 5000m attempt
            CardioSetRow("w-slower", "set-d", setRowId = 40L, startedEpochMs = nowEpochMs - 60_000L,
                exerciseId = "running", distanceMeters = 5000.0, reps = "1260"),
            // 5000.5m rounds to 5001m -> separate bucket
            CardioSetRow("w-5001", "set-e", setRowId = 50L, startedEpochMs = nowEpochMs - 10_000L,
                exerciseId = "running", distanceMeters = 5000.5, reps = "1190",
                gpsRouteKey163 = "route-5001"),
            // Different activity ("cycling") at 5000m -> separate group
            CardioSetRow("w-bike-5000", "set-f", setRowId = 60L, startedEpochMs = nowEpochMs - 5_000L,
                exerciseId = "cycling", distanceMeters = 5000.0, reps = "600",
                distanceSource = "gps", gpsRouteKey163 = "bike-route-163")
        )

        val records = NativeCardioModel.distanceStats(rows, nowEpochMs = nowEpochMs, zoneId = budapestZone)
        assertEquals(3, records.size)

        val bike5000 = records[0]
        assertEquals("cycling", bike5000.exerciseId)
        assertEquals(5000, bike5000.roundedDistanceMeters)
        assertEquals(1, bike5000.count)
        assertEquals("w-bike-5000", bike5000.best.workoutId)
        assertEquals("set-f", bike5000.best.setId)
        assertEquals("gps", bike5000.best.source)
        assertEquals("bike-route-163", bike5000.best.gpsRouteKey163)
        assertNull(bike5000.second)

        val run5000 = records[1]
        assertEquals("running", run5000.exerciseId)
        assertEquals(5000, run5000.roundedDistanceMeters)
        assertEquals(4, run5000.count)
        assertEquals("w-tie-older", run5000.best.workoutId)
        assertEquals("set-a", run5000.best.setId)
        assertEquals("route-phone-163-a", run5000.best.gpsRouteKey163)
        assertNotNull(run5000.second)
        assertEquals("w-tie-newer", run5000.second!!.workoutId)
        assertEquals("set-b", run5000.second!!.setId)
        assertEquals("route-watch-193-b", run5000.second!!.gpsRouteKey193)

        val run5001 = records[2]
        assertEquals(5001, run5001.roundedDistanceMeters)
        assertEquals("w-5001", run5001.best.workoutId)
        assertEquals("route-5001", run5001.best.source)
    }

    @Test
    fun invalidNonFiniteIncompleteNonDistanceAndFutureRows_followMainRulesWithoutMutatingInput() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 21, 18, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val validRow = CardioSetRow(
            workoutId = "w-valid", setId = "s-valid", startedEpochMs = nowEpochMs - 10_000L,
            exerciseId = "running", distanceMeters = 5000.0, reps = "1500", distanceSource = "gps"
        )
        // Known strength/timed exercises with stale distance flag or 0m distance must be rejected by tp106IsDistance
        val staleLiftRow = CardioSetRow(
            workoutId = "w-stale-lift", setId = "s-lift", startedEpochMs = nowEpochMs - 10_000L,
            exerciseId = "goblet-squat", measurementType = "distance", libraryMeasurementType = "reps",
            isKnownLibraryExercise = true, distanceMeters = 0.0, reps = "12"
        )
        assertFalse(NativeCardioModel.tp106IsDistance(staleLiftRow))

        val inputRows = mutableListOf(
            validRow,
            staleLiftRow,
            validRow.copy(workoutId = "w-undone", setId = "s-1", done = false),
            validRow.copy(workoutId = "w-future-1", setId = "s-2", startedEpochMs = nowEpochMs + 1L),
            validRow.copy(workoutId = "w-future-finished", setId = "s-3",
                startedEpochMs = nowEpochMs - 60_000L, finishedEpochMs = nowEpochMs + 60_000L),
            validRow.copy(workoutId = "w-bad-ts-1", setId = "s-4", startedEpochMs = 0L, startedIso = null),
            validRow.copy(workoutId = "w-bad-ts-2", setId = "s-5", startedEpochMs = -500L),
            validRow.copy(workoutId = " ", setId = "s-6", startedIso = null),
            validRow.copy(workoutId = "w-blank-ex", setId = "s-7", exerciseId = " "),
            CardioSetRow("w-plank", "s-8", startedEpochMs = nowEpochMs - 20_000L, exerciseId = "plank",
                measurementType = "time", libraryMeasurementType = "time", isKnownLibraryExercise = true,
                distanceMeters = null, reps = "60"),
            // Zero/invalid/non-finite/over-2000km on both dimensions -> skipped
            validRow.copy(workoutId = "w-zero", setId = "s-9", distanceMeters = 0.0, reps = "0"),
            validRow.copy(workoutId = "w-neg", setId = "s-10", distanceMeters = -1000.0, reps = "-60"),
            validRow.copy(workoutId = "w-nan", setId = "s-11", distanceMeters = Double.NaN, reps = "NaN"),
            validRow.copy(workoutId = "w-inf", setId = "s-12",
                distanceMeters = Double.POSITIVE_INFINITY, durationSeconds = Double.POSITIVE_INFINITY),
            validRow.copy(workoutId = "w-over-max", setId = "s-13", distanceMeters = 2_000_000.1, reps = "0")
        )

        val snapshotBefore = inputRows.toList()
        val result = NativeCardioModel.compute(inputRows, nowEpochMs = nowEpochMs, zoneId = budapestZone)

        assertEquals(snapshotBefore, inputRows)
        assertEquals(5000.0, result.totalDistanceMeters, 1e-6)
        assertEquals(1500.0, result.totalDurationSeconds, 1e-6)
        assertEquals(0.0, result.timeOnlyDurationSeconds, 1e-6)
        assertEquals(300.0, result.overallPaceSecondsPerKm ?: 0.0, 1e-6)
        assertEquals(1, result.records.size)
        assertEquals("w-valid", result.records.single().best.workoutId)
    }

    @Test
    fun timezoneWeekTransition_assignsSundayUtcNightToMondayWeekInBudapest() {
        val boundaryWorkoutEpochMs = Instant.parse("2026-10-18T22:30:00Z").toEpochMilli()
        val nowEpochMs = Instant.parse("2026-10-21T12:00:00Z").toEpochMilli()
        val rows = listOf(
            CardioSetRow("w-tz", "s-tz", startedEpochMs = boundaryWorkoutEpochMs,
                exerciseId = "running", distanceMeters = 5000.0, reps = "1500")
        )

        val utcResult = NativeCardioModel.compute(rows, nowEpochMs = nowEpochMs, zoneId = utcZone)
        assertEquals(LocalDate.of(2026, 10, 12), NativeCardioModel.tp107WeekStart(boundaryWorkoutEpochMs, utcZone))
        assertEquals(0.0, utcResult.currentWeek.meters, 1e-6)
        assertEquals(5000.0, utcResult.previousWeek.meters, 1e-6)

        val budapestResult = NativeCardioModel.compute(rows, nowEpochMs = nowEpochMs, zoneId = budapestZone)
        assertEquals(LocalDate.of(2026, 10, 19), NativeCardioModel.tp107WeekStart(boundaryWorkoutEpochMs, budapestZone))
        assertEquals(5000.0, budapestResult.currentWeek.meters, 1e-6)
        assertEquals(0.0, budapestResult.previousWeek.meters, 1e-6)
    }

    @Test
    fun dstTransitions_usesCalendarDaysFor167HourSpringAnd169HourAutumnWeeks() {
        // Spring forward: Sunday 2026-03-29 in Europe/Budapest (week 2026-03-23 .. 2026-03-30 is 167 hours)
        val springNowMs = ZonedDateTime.of(2026, 3, 31, 12, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val sunMar22LateMs = ZonedDateTime.of(2026, 3, 22, 23, 30, 0, 0, budapestZone).toInstant().toEpochMilli()
        val monMar23EarlyMs = ZonedDateTime.of(2026, 3, 23, 0, 30, 0, 0, budapestZone).toInstant().toEpochMilli()

        val springResult = NativeCardioModel.compute(
            rows = listOf(
                CardioSetRow("w-sun-mar22", "s-1", startedEpochMs = sunMar22LateMs,
                    exerciseId = "running", distanceMeters = 3000.0, reps = "900"),
                CardioSetRow("w-mon-mar23", "s-2", startedEpochMs = monMar23EarlyMs,
                    exerciseId = "running", distanceMeters = 7000.0, reps = "2100")
            ),
            nowEpochMs = springNowMs,
            zoneId = budapestZone
        )
        val springWeek = springResult.previousWeek
        assertEquals(LocalDate.of(2026, 3, 23), springWeek.weekStartDate)
        assertEquals(167L * 3_600_000L, springWeek.weekEndExclusiveEpochMs - springWeek.weekStartEpochMs)
        assertEquals(7000.0, springWeek.meters, 1e-6)
        assertEquals(3000.0, springResult.weekByOffset(2)!!.meters, 1e-6)

        // Autumn fall-back: Sunday 2026-10-25 in Europe/Budapest (week 2026-10-19 .. 2026-10-26 is 169 hours)
        val autumnNowMs = ZonedDateTime.of(2026, 10, 27, 12, 0, 0, 0, budapestZone).toInstant().toEpochMilli()
        val monOct19EarlyMs = ZonedDateTime.of(2026, 10, 19, 0, 30, 0, 0, budapestZone).toInstant().toEpochMilli()
        val sunOct25LateMs = ZonedDateTime.of(2026, 10, 25, 23, 30, 0, 0, budapestZone).toInstant().toEpochMilli()

        val autumnResult = NativeCardioModel.compute(
            rows = listOf(
                CardioSetRow("w-mon-oct19", "s-3", startedEpochMs = monOct19EarlyMs,
                    exerciseId = "running", distanceMeters = 4000.0, reps = "1200"),
                CardioSetRow("w-sun-oct25", "s-4", startedEpochMs = sunOct25LateMs,
                    exerciseId = "running", distanceMeters = 6000.0, reps = "1800")
            ),
            nowEpochMs = autumnNowMs,
            zoneId = budapestZone
        )
        val autumnWeek = autumnResult.previousWeek
        assertEquals(LocalDate.of(2026, 10, 19), autumnWeek.weekStartDate)
        assertEquals(169L * 3_600_000L, autumnWeek.weekEndExclusiveEpochMs - autumnWeek.weekStartEpochMs)
        assertEquals(10000.0, autumnWeek.meters, 1e-6)
        assertEquals(2, autumnWeek.workoutCount)
    }

    @Test
    fun largeInputs_200And1500Workouts_matchBetweenDirectComputeAndPagedAccumulator() {
        val nowEpochMs = ZonedDateTime.of(2026, 10, 25, 18, 0, 0, 0, budapestZone).toInstant().toEpochMilli()

        val rows200 = generateDeterministicWorkoutRows(200, nowEpochMs)
        val direct200 = NativeCardioModel.compute(rows200, nowEpochMs, budapestZone)
        val paged200 = computeViaPagedAccumulator(rows200, nowEpochMs, budapestZone)
        assertEquals(direct200, paged200)
        assertEquals(200, direct200.totalWorkoutCount)
        assertEquals(400, direct200.totalSetCount)
        assertEquals(2_500_000.0, direct200.totalDistanceMeters, 1e-6)
        assertEquals(555_800.0, direct200.totalDurationSeconds, 1e-6)
        assertEquals(45_000.0, direct200.timeOnlyDurationSeconds, 1e-6)
        assertEquals(204.32, direct200.overallPaceSecondsPerKm ?: 0.0, 1e-6)

        // Ties at 1200s occur at i = 0, 50, 100, 150; oldest date (largest i) wins tie under `a.date - b.date`
        val runRec200 = direct200.records.first { it.exerciseId == "running" && it.roundedDistanceMeters == 5000 }
        assertEquals(200, runRec200.count)
        assertEquals("w-150", runRec200.best.workoutId)
        assertEquals("w-100", runRec200.second!!.workoutId)

        val rows1500 = generateDeterministicWorkoutRows(1500, nowEpochMs)
        val direct1500 = NativeCardioModel.compute(rows1500.asSequence(), nowEpochMs, budapestZone)
        val paged1500 = computeViaPagedAccumulator(rows1500, nowEpochMs, budapestZone)
        assertEquals(direct1500, paged1500)
        assertEquals(1500, direct1500.totalWorkoutCount)
        assertEquals(3000, direct1500.totalSetCount)
        assertEquals(18_750_000.0, direct1500.totalDistanceMeters, 1e-6)
        assertEquals(4_173_750.0, direct1500.totalDurationSeconds, 1e-6)
        assertEquals(337_500.0, direct1500.timeOnlyDurationSeconds, 1e-6)
        assertEquals(204.6, direct1500.overallPaceSecondsPerKm ?: 0.0, 1e-6)

        val runRec1500 = direct1500.records.first { it.exerciseId == "running" && it.roundedDistanceMeters == 5000 }
        assertEquals(1500, runRec1500.count)
        assertEquals("w-1450", runRec1500.best.workoutId)
        assertEquals("w-1400", runRec1500.second!!.workoutId)

        val bikeRec1500 = direct1500.records.first { it.exerciseId == "cycling" && it.roundedDistanceMeters == 10000 }
        assertEquals(1125, bikeRec1500.count)
        assertEquals("w-1470", bikeRec1500.best.workoutId)
        assertEquals("s-1470-2", bikeRec1500.best.setId)
        assertEquals("w-1410", bikeRec1500.second!!.workoutId)
        assertEquals("s-1410-2", bikeRec1500.second!!.setId)
    }

    private fun generateDeterministicWorkoutRows(workoutCount: Int, nowEpochMs: Long): List<CardioSetRow> {
        val out = ArrayList<CardioSetRow>(workoutCount * 2)
        var rowId = 1L
        for (i in 0 until workoutCount) {
            val startedMs = nowEpochMs - (i.toLong() + 1L) * 21_600_000L
            val workoutId = "w-$i"
            out.add(
                CardioSetRow(
                    workoutId = workoutId, setId = "s-$i-1", setRowId = rowId++, setNumber = 1,
                    startedEpochMs = startedMs, exerciseId = "running", distanceMeters = 5000.0,
                    reps = (1200 + (i % 50) * 5).toString(), distanceSource = "gps", gpsRouteKey163 = "gps-163-$i"
                )
            )
            if (i % 4 == 0) {
                out.add(
                    CardioSetRow(
                        workoutId = workoutId, setId = "s-$i-2", setRowId = rowId++, setNumber = 2,
                        startedEpochMs = startedMs, exerciseId = "cycling", distanceMeters = null,
                        reps = "900", distanceSource = "manual"
                    )
                )
            } else {
                out.add(
                    CardioSetRow(
                        workoutId = workoutId, setId = "s-$i-2", setRowId = rowId++, setNumber = 2,
                        startedEpochMs = startedMs, exerciseId = "cycling", distanceMeters = 10000.0,
                        reps = (1500 + (i % 30) * 10).toString(), distanceSource = "gps", gpsRouteKey193 = "gps-193-$i"
                    )
                )
            }
        }
        return out
    }

    private fun computeViaPagedAccumulator(rows: List<CardioSetRow>, nowEpochMs: Long, zoneId: ZoneId) =
        NativeCardioModel.Accumulator(nowEpochMs = nowEpochMs, zoneId = zoneId).also { acc ->
            for (page in rows.chunked(NativeCardioModel.DEFAULT_PAGE_SIZE)) {
                val cursor = acc.addPage(page)
                assertNotNull(cursor)
                assertEquals(page.last().workoutId, cursor!!.beforeWorkoutId)
                assertEquals(page.last().setRowId, cursor.beforeSetRowId)
            }
        }.build()
}