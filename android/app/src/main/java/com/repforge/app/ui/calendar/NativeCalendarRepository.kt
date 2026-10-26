package com.repforge.app.ui.calendar

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.CalendarHistorySummary
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
    private val activeScheduleId: suspend () -> String? = { null }) {
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

    data class Preview(val id: String, val settings: NativeCalendarPlanner.Settings,
        val program: ProgramWithDays, val document: KeyValueDocumentEntity?,
        val rows: List<CalendarScheduleEntity>, val history: List<CalendarHistorySummary>,
        val protected: Set<String>, val plan: NativeCalendarPlanner.Plan, val zone: ZoneId)

    private suspend fun history(settings: NativeCalendarPlanner.Settings, zone: ZoneId) =
        db.workoutHistoryDao().observeCalendarHistory(settings.start.atStartOfDay(zone).toInstant().toEpochMilli(),
            settings.start.plusWeeks(settings.weeks.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()).first()

    suspend fun preview(settings: NativeCalendarPlanner.Settings, zone: ZoneId = ZoneId.systemDefault()): Preview = db.withTransaction {
        require(settings.weeks in 1..12)
        val program = requireNotNull(db.programDao().getProgramWithDaysById(settings.programId)) { "calendarProgram" }
        val rows = db.calendarScheduleDao().getEntriesInRange(settings.start.toString(),
            settings.start.plusWeeks(settings.weeks.toLong()).minusDays(1).toString())
        val history = history(settings, zone)
        val activeId = activeScheduleId()
        val protected = rows.filter { it.id == activeId || db.workoutHistoryDao().hasScheduleHistory(it.id) }.map { it.id }.toSet()
        val plan = NativeCalendarPlanner.plan(settings, program.days.sortedBy { it.sortOrder }.map { it.dayId }, rows,
            protected, history.mapNotNull { runCatching { java.time.Instant.parse(it.started).atZone(zone).toLocalDate() }.getOrNull() }.toSet(), zone)
        Preview(UUID.randomUUID().toString(), settings, program, db.keyValueDocumentDao().getDocument("plannerSettings"),
            rows, history, protected, plan, zone)
    }

    /** Caller holds the existing metadata lease and validates its replacement continuation. */
    suspend fun applyPreview(expected: Preview): List<CalendarScheduleEntity> = db.withTransaction {
        check(ZoneId.systemDefault() == expected.zone) { "calendarChanged" }
        val document = db.keyValueDocumentDao().getDocument("plannerSettings")
        if (document != null && JSONObject(document.json).optString("nativePlannerReceipt") == expected.id)
            return@withTransaction expected.plan.slots.mapNotNull { slot ->
                db.calendarScheduleDao().getEntry(slot.before?.id ?: JSONObject(document.json)
                    .getJSONObject("nativePlannerIds").getString(slot.date.toString()))
            }
        val fresh = preview(expected.settings, expected.zone)
        check(fresh.program == expected.program && fresh.document == expected.document &&
            fresh.rows == expected.rows && fresh.history == expected.history && fresh.protected == expected.protected) { "calendarChanged" }
        check(fresh.plan.slots.isNotEmpty()) { "calendarChanged" }
        val settings = expected.settings
        val stamp = maxOf(System.currentTimeMillis(), (document?.updatedAt ?: 0) + 1,
            (expected.rows.maxOfOrNull { it.updatedAt } ?: 0) + 1)
        val ids = JSONObject()
        val saved = expected.plan.slots.map { slot ->
            val old = slot.before
            val id = old?.id ?: UUID.randomUUID().toString()
            ids.put(slot.date.toString(), id)
            val event = old?.googleEventId?.takeIf { it.matches(Regex("rf[0-9a-f]{64}")) }
                ?: LegacySyncIdentity18.calendarEventId(id, slot.date.toString(), settings.programId, slot.dayId)
            val day = expected.program.days.single { it.dayId == slot.dayId }
            val raw = JSONObject(old?.rawJson ?: "{}")
                .put("id", id).put("scheduleId", id).put("googleEventId", event).put("date", slot.date.toString())
                .put("start", slot.start.toString()).put("plannedStart", slot.start.toString()).put("end", slot.end.toString())
                .put("programId", settings.programId).put("programName", expected.program.program.nameHu)
                .put("dayId", slot.dayId).put("workout", slot.dayId).put("dayName", day.nameHu)
                .put("status", "planned").put("cancelled", false).put("updatedAt", stamp)
            CalendarScheduleEntity(id, event, slot.date.toString(), slot.start.toString(), slot.end.toString(),
                settings.programId, expected.program.program.nameHu, slot.dayId, day.nameHu, "planned", stamp, raw.toString())
        }
        val removed = expected.plan.removed.map { row -> row.copy(status = "cancelled", updatedAt = stamp,
            rawJson = JSONObject(row.rawJson).put("status", "cancelled").put("cancelled", true).put("updatedAt", stamp).toString()) }
        db.calendarScheduleDao().upsertEntries(saved + removed)
        val raw = JSONObject(document?.json ?: "{}").put("mode", settings.mode).put("time", settings.time.toString())
            .put("minutes", settings.minutes).put("weekdays", org.json.JSONArray(settings.weekdays.sorted()))
            .put("nativeProgramId", settings.programId).put("nativeStart", settings.start.toString())
            .put("nativeWeeks", settings.weeks).put("nativeFirstIndex", settings.firstIndex)
            .put("nativePlannerReceipt", expected.id).put("nativePlannerIds", ids)
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("plannerSettings", raw.toString(), stamp))
        saved
    }

    suspend fun plannerSettings(): NativeCalendarPlanner.Settings? {
        val value = db.keyValueDocumentDao().getDocument("plannerSettings") ?: return null
        return runCatching {
            val raw = JSONObject(value.json)
            val days = raw.optJSONArray("weekdays") ?: org.json.JSONArray()
            NativeCalendarPlanner.Settings(raw.getString("nativeProgramId"), LocalDate.parse(raw.getString("nativeStart")),
                LocalTime.parse(raw.getString("time")), raw.getInt("minutes"), raw.getInt("nativeWeeks"),
                raw.getInt("nativeFirstIndex"), raw.getString("mode"), (0 until days.length()).map { days.getInt(it) }.toSet())
        }.getOrNull()
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
