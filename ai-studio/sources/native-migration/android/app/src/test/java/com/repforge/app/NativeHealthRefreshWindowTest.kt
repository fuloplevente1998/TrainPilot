package com.repforge.app

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NativeHealthRefreshWindowTest {
    private val zone = ZoneId.of("Europe/Budapest")

    @Test fun springTransitionUsesTwentyThreeHourLocalDay() {
        val day = LocalDate.parse("2026-03-29")
        val (from, to) = NativeHealthRefresh.dayWindow(day, zone, Instant.parse("2026-10-25T12:00:00Z"))
        assertEquals(23L, Duration.between(from, to).toHours())
    }

    @Test fun autumnTransitionUsesTwentyFiveHourLocalDay() {
        val day = LocalDate.parse("2026-10-25")
        val (from, to) = NativeHealthRefresh.dayWindow(day, zone, Instant.parse("2026-10-26T12:00:00Z"))
        assertEquals(25L, Duration.between(from, to).toHours())
    }

    @Test fun partialTodayNeverRequestsTomorrowOrFakeFutureSamples() {
        val now = Instant.parse("2026-10-25T12:00:00Z")
        val (from, to) = NativeHealthRefresh.dayWindow(LocalDate.parse("2026-10-25"), zone, now)
        assertEquals(now, to)
        assertTrue(from.isBefore(to))
    }

    @Test fun futureDayRejectsBeforeAnyProviderRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeHealthRefresh.dayWindow(LocalDate.parse("2026-10-27"), zone, Instant.parse("2026-10-25T12:00:00Z"))
        }
    }
}
