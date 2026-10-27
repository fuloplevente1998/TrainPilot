package com.repforge.app.ui.progress

import com.repforge.app.data.db.ProgressVolumeRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Exactly the four range controls of the Studio Progress mockup.
 * The 3-month option means three calendar months through the current local date,
 * not an inaccurate fixed 90*24-hour window.
 */
internal enum class NativeVolumeRange(val barCount: Int) {
    DAY(1), WEEK(7), MONTH(10), THREE_MONTHS(12)
}

internal data class NativeVolumeBucket(
    val startDate: LocalDate,
    val endExclusive: LocalDate,
    val volumeKg: Double,
    val workoutCount: Int
)

internal data class NativeVolumeSnapshot(
    val range: NativeVolumeRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalVolumeKg: Double,
    val workoutCount: Int,
    val buckets: List<NativeVolumeBucket>
)

/** Pure, deterministic local-calendar grouping; all input is the native SQL projection. */
internal object NativeVolumeProjection {
    fun dateBounds(range: NativeVolumeRange, nowEpochMs: Long, zone: ZoneId): Pair<LocalDate, LocalDate> {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val first = when (range) {
            NativeVolumeRange.DAY -> today
            NativeVolumeRange.WEEK -> today.minusDays(6)
            NativeVolumeRange.MONTH -> today.minusDays(29)
            NativeVolumeRange.THREE_MONTHS -> today.minusMonths(3).plusDays(1)
        }
        return first to today
    }

    /** Exclusive end at the current instant: completed workouts in the future are excluded. */
    fun queryBounds(range: NativeVolumeRange, nowEpochMs: Long, zone: ZoneId): Pair<Long, Long> {
        val first = dateBounds(range, nowEpochMs, zone).first
        return first.atStartOfDay(zone).toInstant().toEpochMilli() to nowEpochMs + 1
    }

    fun summarize(
        range: NativeVolumeRange, nowEpochMs: Long, zone: ZoneId,
        rows: List<ProgressVolumeRow>
    ): NativeVolumeSnapshot {
        val (first, today) = dateBounds(range, nowEpochMs, zone)
        val days = ChronoUnit.DAYS.between(first, today).toInt() + 1
        val count = range.barCount
        val weights = DoubleArray(count)
        val workouts = IntArray(count)
        val (fromMs, toExclusive) = queryBounds(range, nowEpochMs, zone)
        for (row in rows) {
            if (row.finishedEpochMs < fromMs || row.finishedEpochMs >= toExclusive) continue
            val date = Instant.ofEpochMilli(row.finishedEpochMs).atZone(zone).toLocalDate()
            val offset = ChronoUnit.DAYS.between(first, date).toInt()
            if (offset !in 0 until days) continue
            val i = (offset * count / days).coerceIn(0, count - 1)
            val weight = row.volumeKg.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            weights[i] += weight
            workouts[i] += 1
        }
        val buckets = (0 until count).map { index ->
            // Ceil-division gives the inverse of floor(dateOffset * count / days).
            val lower = (index.toLong() * days + count - 1) / count
            val upper = ((index + 1L) * days + count - 1) / count
            NativeVolumeBucket(first.plusDays(lower), first.plusDays(upper),
                weights[index], workouts[index])
        }
        return NativeVolumeSnapshot(
            range, first, today, weights.sum(), workouts.sum(), buckets
        )
    }
}
