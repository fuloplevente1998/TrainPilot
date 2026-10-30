package com.repforge.app.wear

import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.math.hypot
import kotlin.math.min
import org.junit.Assert.*
import org.junit.Test

class WearCalendarLayoutTest {
    @Test fun sevenDayControlsAndMarkersFitTheRoundDisplayWithoutOverlapping() {
        for ((width, height) in listOf(160f to 160f, 176f to 176f, 192f to 192f,
                200f to 200f, 216f to 216f, 225f to 225f, 240f to 240f,
                254f to 254f, 225f to 192f, 192f to 225f)) {
            val layout = wearCalendarLayout(width, height)
            val radius = min(width, height) / 2f
            assertEquals(7, layout.days.size)
            assertEquals(layout.days[0].y, layout.days[3].y, .001f)
            assertEquals(layout.days[4].y, layout.days[6].y, .001f)
            assertTrue(layout.days[4].y > layout.days[0].y)
            for (point in layout.days) {
                assertTrue("Calendar circle clipped at $width x $height", hypot(point.x, point.y - height / 2f) + layout.buttonSize / 2f <= radius - 5.99f)
                assertTrue("Workout marker clipped", hypot(point.x, point.y + layout.buttonSize / 2f + 4f - height / 2f) + 2f <= radius - 3.99f)
            }
            for (a in layout.days.indices) for (b in a + 1 until layout.days.size) {
                assertTrue("Day targets overlap", hypot(layout.days[a].x - layout.days[b].x,
                    layout.days[a].y - layout.days[b].y) >= layout.buttonSize + 2.99f)
            }
        }
    }

    @Test fun weeksUseMondayAndIsoYearBoundaries() {
        val friday = LocalDate.parse("2026-10-09")
        assertEquals(LocalDate.parse("2026-10-05"), wearCalendarMonday(friday))
        assertEquals(41, friday.get(WeekFields.ISO.weekOfWeekBasedYear()))
        val newYear = LocalDate.parse("2027-01-01")
        assertEquals(LocalDate.parse("2026-12-28"), wearCalendarMonday(newYear))
        assertEquals(2026, newYear.get(WeekFields.ISO.weekBasedYear()))
    }

    @Test fun navigationIncludesOnlyCachedWeeksAndIgnoresInvalidDates() {
        val today = LocalDate.parse("2026-10-09")
        val entries = listOf(entry("2026-10-06"), entry("2026-10-06"), entry("2026-10-19"), entry("invalid"))
        assertEquals(listOf(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-19")), wearCalendarWeeks(entries, today))
        assertEquals(listOf(LocalDate.parse("2026-10-05")), wearCalendarWeeks(emptyList(), today))
        assertEquals(listOf(LocalDate.parse("2026-09-28")),wearCalendarWeeks(listOf(entry("2026-09-30")),today))
    }

    @Test fun weekCaptionsFollowTheSelectedDatesAcrossMonthsAndYears() {
        val today=LocalDate.parse("2026-10-09")
        val october=wearCalendarCaption(LocalDate.parse("2026-10-05"),today)
        assertEquals("október",october.month)
        assertEquals("5–11. · 41. hét",october.dates)
        val crossing=wearCalendarCaption(LocalDate.parse("2026-10-26"),today)
        assertTrue(crossing.month.contains("okt."));assertTrue(crossing.month.contains("nov."))
        assertEquals("26–1. · 44. hét",crossing.dates)
        val newYear=wearCalendarCaption(LocalDate.parse("2026-12-28"),today)
        assertTrue(newYear.month.contains("dec."));assertTrue(newYear.month.contains("jan."));assertTrue(newYear.month.contains("2027"))
        assertEquals("28–3. · 53. hét",newYear.dates)
    }

    @Test fun startRequiresAvailablePlannedWorkoutAndNoActiveDraft() {
        val today = LocalDate.parse("2026-10-09")
        assertTrue(wearCalendarCanStart(entry("2026-10-09", "planned"), today, true))
        assertTrue(wearCalendarCanStart(entry("2026-10-10", "planned"), today, true))
        assertFalse(wearCalendarCanStart(entry("2026-10-08", "planned"), today, true))
        for (status in listOf("completed", "skipped", "rest")) assertFalse(wearCalendarCanStart(entry("2026-10-10", status), today, true))
        assertFalse(wearCalendarCanStart(entry("2026-10-10", "planned"), today, false))
        assertFalse(wearCalendarCanStart(entry("2026-10-10", "planned").copy(day = null), today, true))
        assertFalse(wearCalendarCanStart(entry("invalid", "planned"), today, true))
    }

    private fun entry(date: String, status: String = "rest") = WatchCalendarEntry(date, "scheduled", "", status,
        WatchHomeDay("A", "Alap A", "p", "Otthoni", emptyList()))
}
