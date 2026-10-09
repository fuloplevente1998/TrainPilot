package com.repforge.app.wear

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

internal data class WearCalendarPoint(val x: Float, val y: Float)
internal data class WearCalendarLayout(
    val buttonSize: Float,
    val monthTop: Float,
    val weekTop: Float,
    val navigationSize: Float,
    val menuTop: Float,
    val days: List<WearCalendarPoint>
)

internal fun wearCalendarLayout(width: Float, height: Float): WearCalendarLayout {
    val diameter = min(width, height)
    val inset = (height - diameter) / 2f
    val button = min(44f, diameter * .205f)
    val stride = button + min(4f, diameter * .02f)
    val days = (0..6).map { index ->
        val x = if (index < 4) (index - 1.5f) * stride else (index - 5f) * stride
        WearCalendarPoint(x, inset + diameter * if (index < 4) .47f else .73f)
    }
    return WearCalendarLayout(button, inset + diameter * .08f, inset + diameter * .235f,
        min(24f, diameter * .125f), inset + diameter * .87f, days)
}

internal fun wearCalendarMonday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal fun wearCalendarDate(entry: WatchCalendarEntry): LocalDate? =
    runCatching { LocalDate.parse(entry.date) }.getOrNull()

internal fun wearCalendarWeeks(entries: List<WatchCalendarEntry>, today: LocalDate): List<LocalDate> =
    (entries.mapNotNull(::wearCalendarDate).map(::wearCalendarMonday) + wearCalendarMonday(today)).distinct().sorted()

internal fun wearCalendarCanStart(entry: WatchCalendarEntry, today: LocalDate, canStart: Boolean): Boolean =
    canStart && entry.status == "planned" && entry.day != null &&
        wearCalendarDate(entry)?.let { !it.isBefore(today) } == true
