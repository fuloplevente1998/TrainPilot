package com.repforge.app

import com.repforge.app.data.db.CalendarScheduleEntity
import org.junit.Assert.*
import org.junit.Test

class NativeCalendarSyncPlanTest {
    private fun entry(i: Int) = CalendarScheduleEntity("entry-$i", "", "2026-10-24", "2026-10-24T10:00:00Z",
        "2026-10-24T11:00:00Z", "program", "Program", "day", "Day", "planned", i.toLong(), "{}")
    @Test fun moreThanOneHundredRowsKeepOldCancellationAndEveryPlannedEvent() {
        val rows = (0..250).map { entry(it).copy(status = if (it == 0) "skipped" else "planned") }
        val operations = NativeCalendarSyncPlan.operations(rows).toList()
        assertEquals(251, operations.size)
        assertTrue(operations.first().delete)
        assertNull(operations.first().payload)
        assertEquals(251, operations.map { it.id }.toSet().size)
        assertTrue(operations.drop(1).all { !it.delete && it.payload != null })
    }
    @Test fun validRemoteIdentityIsRetainedAndRepeatedSyncHasStableFallbackIds() {
        val remote = "rf" + "a".repeat(64)
        val row = entry(1).copy(googleEventId = remote)
        assertEquals(remote, NativeCalendarSyncPlan.operations(listOf(row)).single().id)
        val fallback = entry(2).copy(googleEventId = "untrusted/path")
        val first = NativeCalendarSyncPlan.operations(listOf(fallback)).single()
        assertTrue(Regex("rf[0-9a-f]{64}").matches(first.id))
        assertEquals(first.id, NativeCalendarSyncPlan.operations(listOf(fallback)).single().id)
    }
    @Test fun legacyMissingTimesKeepTheExistingOneHourFallback() {
        val operation = NativeCalendarSyncPlan.operations(listOf(entry(0).copy(startIso = "", endIso = ""))).single()
        assertEquals("2026-10-24T18:00:00Z", operation.payload!!.getJSONObject("start").getString("dateTime"))
        assertEquals("2026-10-24T19:00:00Z", operation.payload.getJSONObject("end").getString("dateTime"))
        assertTrue(NativeCalendarSyncPlan.operations(emptyList()).none())
    }
}
