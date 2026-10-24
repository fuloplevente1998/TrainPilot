package com.repforge.app

import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONObject
import java.time.Instant

/** One plan per persisted calendar row, including old cancellation records. No tail/window cutoff. */
internal object NativeCalendarSyncPlan {
    data class Operation(val id: String, val delete: Boolean, val payload: JSONObject?)
    fun operations(entries: List<CalendarScheduleEntity>): Sequence<Operation> = entries.asSequence().map { entry ->
        val id = entry.googleEventId.takeIf { Regex("rf[0-9a-f]{64}").matches(it) }
            ?: LegacySyncIdentity18.calendarEventId(entry.id, entry.date, entry.programId, entry.dayId)
        if (entry.status == "skipped") Operation(id, true, null)
        else {
            val start = entry.startIso.ifBlank { "${entry.date}T18:00:00Z" }
            val end = entry.endIso.ifBlank {
                runCatching { Instant.parse(start).plusSeconds(3600).toString() }.getOrDefault("${entry.date}T19:00:00Z")
            }
            Operation(id, false, JSONObject().put("id", id)
                .put("summary", "TrainPilot: ${entry.programName} – ${entry.dayName}".trim(' ', '–'))
                .put("description", "Státusz: ${entry.status}")
                .put("start", JSONObject().put("dateTime", start)).put("end", JSONObject().put("dateTime", end)))
        }
    }
}
