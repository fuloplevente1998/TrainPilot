package com.repforge.app.ui.coach

import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.ui.calendar.NativeCalendarRepository
import java.time.Instant
import java.time.ZoneId

/** rf233 target precedence: today's plan, upcoming plan, then the active program's next cycle day. */
internal class NativeCoachTargets(private val db: TrainPilotDatabase) {
    suspend fun select(activeProgramId: String, language: String, now: Instant, zone: ZoneId,
        programsSnapshot: List<ProgramWithDays>? = null): CoachTarget? {
        val programs = programsSnapshot ?: db.programDao().getProgramsWithDays()
        val today = now.atZone(zone).toLocalDate()
        var offset = 0
        var plan: CalendarScheduleEntity? = null
        while (plan == null) {
            val rows = db.calendarScheduleDao().getCoachUpcoming(today.atStartOfDay(zone).toInstant().toString(), 32, offset)
            plan = rows.firstOrNull { row -> !NativeCalendarRepository.cancelled(row) && programs.any { p ->
                p.program.id == row.programId && p.days.any { it.dayId == row.dayId } } }
            if (rows.size < 32) break
            offset += rows.size
        }
        fun title(hu: String, en: String, de: String, ro: String) = when (language) {
            "en" -> en; "de" -> de; "ro" -> ro; else -> hu
        }.ifBlank { hu }
        if (plan != null) {
            val row = plan
            val program = programs.first { it.program.id == row.programId }
            val day = program.days.first { it.dayId == row.dayId }
            // The local date comes from the actual instant. Imported date fields/offset strings
            // cannot reclassify a previous-day UTC timestamp that belongs to today locally.
            val localDay = Instant.parse(row.startIso).atZone(zone).toLocalDate()
            return CoachTarget(program.program.id, day.dayId, title(day.nameHu, day.nameEn, day.nameDe, day.nameRo), row, localDay != today)
        }
        val program = programs.firstOrNull { it.program.id == activeProgramId } ?: return null
        val ordered = program.days.sortedBy { it.sortOrder }
        if (ordered.isEmpty()) return null
        var last: CalendarScheduleEntity? = null
        var cycleOffset = 0
        while (last == null) {
            val rows = db.calendarScheduleDao().getCoachCyclePlans(program.program.id, 32, cycleOffset)
            last = rows.firstOrNull { row -> !NativeCalendarRepository.cancelled(row) && ordered.any { it.dayId == row.dayId } }
            if (rows.size < 32) break
            cycleOffset += rows.size
        }
        val index = last?.let { row -> ordered.indexOfFirst { it.dayId == row.dayId } } ?: -1
        val day = ordered[(index + 1).coerceAtLeast(0) % ordered.size]
        return CoachTarget(program.program.id, day.dayId, title(day.nameHu, day.nameEn, day.nameDe, day.nameRo))
    }
}
