package com.repforge.app.ui.progress

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Calendar-day windows, inclusive of today, with exact zone-aware midnight (DST safe). */
enum class NativeStrengthRange {
    DAYS_30, DAYS_90, YEAR, ALL;

    fun startEpochMs(nowEpochMs: Long, zone: ZoneId): Long? {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val first = when (this) {
            DAYS_30 -> today.minusDays(29)
            DAYS_90 -> today.minusDays(89)
            YEAR -> today.minusYears(1).plusDays(1)
            ALL -> return null
        }
        return first.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun contains(epochMs: Long, nowEpochMs: Long, zone: ZoneId): Boolean {
        // Exclude future bad timestamps, including later today. Never use a fixed
        // 24-hour subtraction; DST days do not always have 24 hours.
        return epochMs <= nowEpochMs && epochMs >= (startEpochMs(nowEpochMs, zone) ?: Long.MIN_VALUE)
    }
}
