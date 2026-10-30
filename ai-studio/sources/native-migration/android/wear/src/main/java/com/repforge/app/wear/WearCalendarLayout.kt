package com.repforge.app.wear

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.time.format.DateTimeFormatter
import kotlin.math.min

internal data class WearCalendarPoint(val x: Float, val y: Float)
internal data class WearCalendarLayout(
    val buttonSize: Float,
    val monthTop: Float,
    val weekTop: Float,
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
        inset + diameter * .87f, days)
}

internal data class WearCalendarCaption(val month: String, val dates: String)

internal fun wearCalendarCaption(week: LocalDate, today: LocalDate): WearCalendarCaption {
    val end=week.plusDays(6)
    val locale=java.util.Locale.forLanguageTag("hu")
    val month=if(week.month==end.month && week.year==end.year)week.format(DateTimeFormatter.ofPattern("MMMM",locale))
        else "${week.format(DateTimeFormatter.ofPattern("MMM",locale))}–${end.format(DateTimeFormatter.ofPattern("MMM",locale))}"
    val year=if(end.year==today.year)"" else " ${end.year}"
    return WearCalendarCaption(month+year,"${week.dayOfMonth}–${end.dayOfMonth}. · ${week.get(WeekFields.ISO.weekOfWeekBasedYear())}. hét")
}

internal fun wearCalendarMonday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal fun wearCalendarDate(entry: WatchCalendarEntry): LocalDate? =
    runCatching { LocalDate.parse(entry.date) }.getOrNull()

internal fun wearCalendarWeeks(entries: List<WatchCalendarEntry>, today: LocalDate): List<LocalDate> =
    entries.mapNotNull(::wearCalendarDate).map(::wearCalendarMonday).distinct().sorted()
        .ifEmpty { listOf(wearCalendarMonday(today)) }

internal fun wearCalendarCanStart(entry: WatchCalendarEntry, today: LocalDate, canStart: Boolean): Boolean =
    canStart && entry.status == "planned" && entry.day != null &&
        wearCalendarDate(entry)?.let { !it.isBefore(today) } == true
