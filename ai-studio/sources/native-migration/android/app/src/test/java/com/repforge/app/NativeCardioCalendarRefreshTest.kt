package com.repforge.app

import com.repforge.app.ui.progress.CardioSetRow
import com.repforge.app.ui.progress.NativeCardioCalendarRefresh
import com.repforge.app.ui.progress.NativeCardioModel
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

/** Dedicated date/week/zone/lifecycle regressions; does not alter the 26 cardio tests. */
class NativeCardioCalendarRefreshTest {
    private val budapest = ZoneId.of("Europe/Budapest")
    private val utc = ZoneId.of("UTC")
    private fun epoch(time: String) = Instant.parse(time).toEpochMilli()
    private fun stamp(time: String, zone: ZoneId = budapest) =
        NativeCardioCalendarRefresh.stamp(epoch(time), zone)

    @Test fun crossingLocalMidnightOnSameWeekTriggersRefresh() {
        val sunday = stamp("2026-10-20T21:30:00Z") // Tuesday 23:30 Budapest
        val wednesday = stamp("2026-10-20T22:30:00Z") // Wednesday 00:30
        assertNotEquals(sunday, wednesday)
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK, sunday, wednesday))
        assertFalse(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK, wednesday, wednesday))
    }

    @Test fun mondayRolloverMovesCardioToPreviousWeekWithoutRoomChanges() {
        val finished = epoch("2026-10-25T20:00:00Z")
        val rows = listOf(CardioSetRow(
            workoutId = "sun-run", setId = "s1", exerciseId = "running",
            startedEpochMs = finished - 300_000L, finishedEpochMs = finished,
            measurementType = "distance", done = true, distanceMeters = 1000.0, reps = "300"
        ))
        val sunday = NativeCardioModel.compute(rows,
            epoch("2026-10-25T22:30:00Z"), budapest)
        val monday = NativeCardioModel.compute(rows,
            epoch("2026-10-25T23:30:00Z"), budapest)
        assertEquals(1, sunday.currentWeek.setCount)
        assertEquals(0, monday.currentWeek.setCount)
        assertEquals(1, monday.previousWeek.setCount)
        assertEquals("2026-10-26", monday.currentWeek.weekStartIso)
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK,
            stamp("2026-10-25T22:30:00Z"), stamp("2026-10-25T23:30:00Z")))
    }

    @Test fun sameLocalDateDifferentTimeZoneTriggersRebucket() {
        val instant = "2026-10-25T12:00:00Z"
        val old = stamp(instant, utc)
        val changed = stamp(instant, budapest)
        assertEquals(old.localDate, changed.localDate)
        assertNotEquals(old.zone, changed.zone)
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK, old, changed))
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertNotEquals(stamp("2026-10-25T22:30:00Z", utc).localDate,
            stamp("2026-10-25T22:30:00Z", tokyo).localDate)
    }

    @Test fun returnAndManualClockChangeRefreshWithoutDateOrZoneChange() {
        val same = stamp("2026-10-25T20:00:00Z")
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.SCREEN_RESUME, same, same))
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.SYSTEM_CLOCK_CHANGE, same, same))
        assertTrue(NativeCardioCalendarRefresh.shouldRefresh(
            NativeCardioCalendarRefresh.Trigger.CALENDAR_TICK, null, same))
    }

    @Test fun nextMidnightDelayHandlesDstTransitionAndStaysBounded() {
        // DST ends on Oct 25 in Hungary; the local date is 25 hours long.
        val dstDateStart = epoch("2026-10-24T22:00:00Z")
        val nextDateStart = epoch("2026-10-25T23:00:00Z")
        assertEquals(25L * 60 * 60 * 1000, nextDateStart - dstDateStart)
        assertEquals(500L, NativeCardioCalendarRefresh.delayToCheck(
            epoch("2026-10-25T22:59:59.500Z"), budapest))
        assertEquals(60_000L, NativeCardioCalendarRefresh.delayToCheck(
            epoch("2026-10-25T03:00:00Z"), budapest))
    }
}
