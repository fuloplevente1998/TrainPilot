package com.repforge.app.ui.progress

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The cardio week buckets depend on both wall-clock date and the device time zone,
 * not just Room invalidations. Deliberately independent of UI/Android for JVM tests.
 */
internal object NativeCardioCalendarRefresh {
    data class Stamp(val localDate: LocalDate, val zone: ZoneId)
    enum class Trigger { CALENDAR_TICK, SCREEN_RESUME, SYSTEM_CLOCK_CHANGE }

    fun stamp(epochMs: Long, zone: ZoneId): Stamp =
        Stamp(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate(), zone)

    fun shouldRefresh(trigger: Trigger, requested: Stamp?, current: Stamp): Boolean =
        trigger != Trigger.CALENDAR_TICK || requested != current

    /**
     * Wake at the next local midnight, but check at most once per minute as a
     * fallback for manual clock/time-zone changes whose broadcast was missed.
     * LocalDate.atStartOfDay(zone) handles 23/25-hour DST days correctly.
     */
    fun delayToCheck(epochMs: Long, zone: ZoneId): Long {
        val nextMidnight = stamp(epochMs, zone).localDate.plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        return (nextMidnight - epochMs).coerceAtLeast(1L).coerceAtMost(60_000L)
    }
}
