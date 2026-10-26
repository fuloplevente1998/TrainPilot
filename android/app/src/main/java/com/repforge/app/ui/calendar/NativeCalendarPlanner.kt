package com.repforge.app.ui.calendar

import com.repforge.app.data.db.CalendarScheduleEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

/** Date-only planning; persistence and identity remain the calendar repository's responsibility. */
internal object NativeCalendarPlanner {
    data class Settings(val programId: String, val start: LocalDate, val time: LocalTime,
        val minutes: Int = 45, val weeks: Int = 4, val firstIndex: Int = 0,
        val mode: String = "alternate", val weekdays: Set<Int> = setOf(1, 3, 5))
    data class Slot(val date: LocalDate, val dayId: String, val before: CalendarScheduleEntity?,
        val start: Instant, val end: Instant)
    data class Plan(val slots: List<Slot>, val removed: List<CalendarScheduleEntity>)

    fun plan(settings: Settings, days: List<String>, rows: List<CalendarScheduleEntity>,
        protectedIds: Set<String>, occupiedHistoryDates: Set<LocalDate>, zone: ZoneId): Plan {
        require(settings.weeks in 1..12 && settings.start.year in 1900..2200) { "calendarDate" }
        require(settings.minutes in 10..240 && settings.time.second == 0 && settings.time.nano == 0) { "calendarDuration" }
        require(settings.mode in setOf("daily", "alternate", "weekly")) { "calendarDate" }
        require(settings.firstIndex in days.indices && days.distinct().size == days.size) { "calendarDay" }
        require(settings.weekdays.all { it in 0..6 } && (settings.mode != "weekly" || settings.weekdays.isNotEmpty())) { "calendarWeekdays" }
        val finish = settings.start.plusWeeks(settings.weeks.toLong())
        fun date(row: CalendarScheduleEntity): LocalDate? = runCatching {
            Instant.parse(row.startIso).atZone(zone).toLocalDate()
        }.getOrNull()
        val queue = rows.filter { row ->
            !NativeCalendarRepository.cancelled(row) && row.id !in protectedIds &&
                row.status in setOf("planned", "skipped") && date(row)?.let { it >= settings.start && it < finish } == true &&
                runCatching { Instant.parse(row.endIso) > Instant.parse(row.startIso) }.getOrDefault(false)
        }.sortedWith(compareBy({ Instant.parse(it.startIso) }, { it.id }))
        val movable = queue.map { it.id }.toSet()
        val occupied = rows.filter { !NativeCalendarRepository.cancelled(it) && it.id !in movable }
            .mapNotNull(::date).toSet() + occupiedHistoryDates
        val dates = (0 until settings.weeks * 7).map { settings.start.plusDays(it.toLong()) }.filter { d ->
            d !in occupied && when (settings.mode) {
                "alternate" -> java.time.temporal.ChronoUnit.DAYS.between(settings.start, d) % 2 == 0L
                "weekly" -> d.dayOfWeek.value % 7 in settings.weekdays
                else -> true
            }
        }
        val reserved = mutableSetOf<String>()
        val owners = dates.map { d -> queue.firstOrNull { it.id !in reserved && date(it) == d }?.also { reserved.add(it.id) } }
        val spare = java.util.ArrayDeque(queue.filter { it.id !in reserved })
        val used = mutableSetOf<String>()
        val slots = dates.mapIndexed { index, d ->
            val before = owners[index] ?: if (spare.isEmpty()) null else spare.removeFirst()
            before?.let { used.add(it.id) }
            val wall = d.atTime(settings.time)
            // Match the existing native editor: nonexistent wall times require a new choice.
            require(zone.rules.getValidOffsets(wall).isNotEmpty()) { "calendarDate" }
            val start = wall.atZone(zone).toInstant()
            Slot(d, days[(settings.firstIndex + index) % days.size], before, start,
                start.plusSeconds(settings.minutes * 60L))
        }
        return Plan(slots, if (slots.isEmpty()) emptyList() else queue.filter { it.id !in used })
    }
}
