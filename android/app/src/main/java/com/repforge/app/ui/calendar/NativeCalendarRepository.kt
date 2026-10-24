package com.repforge.app.ui.calendar

import androidx.room.withTransaction
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** Uses the imported calendar entity, legacy raw JSON and managed Google event identity. */
internal class NativeCalendarRepository(private val db: TrainPilotDatabase,
    private val activeScheduleId: () -> String? = { null }) {
    data class Edit(val id: String? = null, val expected: CalendarScheduleEntity? = null,
        val programId: String, val dayId: String, val date: LocalDate,
        val time: LocalTime, val minutes: Int)

    suspend fun save(edit: Edit, zone: ZoneId = ZoneId.systemDefault()): CalendarScheduleEntity = db.withTransaction {
        require(edit.minutes in 10..240) { "calendarDuration" }
        require(edit.date.year in 1900..2200) { "calendarDate" }
        val old = edit.id?.let { requireNotNull(db.calendarScheduleDao().getEntry(it)) { "calendarChanged" } }
        if (old != null) {
            check(old == edit.expected) { "calendarChanged" }
            checkMutable(old)
        } else require(edit.expected == null) { "calendarChanged" }
        val program = requireNotNull(db.programDao().getProgramWithDaysById(edit.programId)) { "calendarProgram" }
        val day = requireNotNull(program.days.singleOrNull { it.dayId == edit.dayId }) { "calendarDay" }
        // Reject a nonexistent DST wall time instead of silently shifting the user's plan.
        val wall = edit.date.atTime(edit.time)
        require(zone.rules.getValidOffsets(wall).isNotEmpty()) { "calendarDate" }
        val start = wall.atZone(zone).toInstant()
        val end = start.plusSeconds(edit.minutes * 60L)
        val id = old?.id ?: UUID.randomUUID().toString()
        val event = old?.googleEventId?.takeIf { it.matches(Regex("rf[0-9a-f]{64}")) }
            ?: LegacySyncIdentity18.calendarEventId(id, edit.date.toString(), edit.programId, edit.dayId)
        val stamp = maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0) + 1)
        val raw = JSONObject(old?.rawJson ?: "{}")
            .put("id", id).put("scheduleId", id).put("googleEventId", event)
            .put("date", edit.date.toString()).put("start", start.toString()).put("plannedStart", start.toString())
            .put("end", end.toString()).put("programId", program.program.id).put("programName", program.program.nameHu)
            .put("dayId", day.dayId).put("workout", day.dayId).put("dayName", day.nameHu)
            .put("status", old?.status ?: "planned").put("cancelled", false).put("updatedAt", stamp)
        CalendarScheduleEntity(id, event, edit.date.toString(), start.toString(), end.toString(),
            program.program.id, program.program.nameHu, day.dayId, day.nameHu, old?.status ?: "planned",
            stamp, raw.toString()).also { db.calendarScheduleDao().upsertEntry(it) }
    }

    suspend fun changeStatus(expected: CalendarScheduleEntity, status: String, delete: Boolean = false) = db.withTransaction {
        require(status in setOf("planned", "skipped") && (!delete || status == "skipped"))
        val current = requireNotNull(db.calendarScheduleDao().getEntry(expected.id)) { "calendarChanged" }
        check(current == expected) { "calendarChanged" }
        checkMutable(current)
        val stamp = maxOf(System.currentTimeMillis(), current.updatedAt + 1)
        val raw = JSONObject(current.rawJson).put("status", status).put("cancelled", delete).put("updatedAt", stamp)
        // Keep a cancellation record for the existing cloud merge and managed Calendar delete.
        // Physical deletion would lose the remote deletion signal and could resurrect a plan.
        db.calendarScheduleDao().upsertEntry(current.copy(status = status, updatedAt = stamp, rawJson = raw.toString()))
    }

    private suspend fun checkMutable(row: CalendarScheduleEntity) {
        check(activeScheduleId() != row.id) { "calendarActive" }
        check(row.status in setOf("planned", "skipped") && !cancelled(row) &&
            !db.workoutHistoryDao().hasScheduleHistory(row.id)) { "calendarCompleted" }
    }

    companion object {
        fun cancelled(row: CalendarScheduleEntity): Boolean =
            runCatching { JSONObject(row.rawJson).optBoolean("cancelled") }.getOrDefault(true)

        fun requireStart(row: CalendarScheduleEntity?, programId: String, dayId: String): CalendarScheduleEntity {
            val entry = requireNotNull(row) { "calendarChanged" }
            check(entry.status == "planned" && !cancelled(entry) && entry.programId == programId && entry.dayId == dayId) {
                "calendarChanged"
            }
            return entry
        }
    }
}
