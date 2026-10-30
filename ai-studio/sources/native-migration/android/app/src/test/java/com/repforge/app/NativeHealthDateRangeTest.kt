package com.repforge.app

import android.content.Context
import com.repforge.app.ui.health.HealthRecordCursor
import com.repforge.app.ui.health.HealthRecordQuery
import com.repforge.app.ui.health.NativeHealthRecords
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Exercises the real bounded HealthJournalStore.page implementation, not a fake provider. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthDateRangeTest {
    private lateinit var store: HealthJournalStore
    private lateinit var reader: NativeHealthRecords
    private val today = LocalDate.parse("2026-10-24")
    private val from = today.minusDays(2)

    @Before fun open() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("health-journal.db")
        store = HealthJournalStore(context)
        reader = NativeHealthRecords(store)
    }

    @After fun close() { store.close() }

    private fun row(id: String, date: LocalDate = today, channel: String = HealthJournalStore.HC,
        source: String = "origin.alpha", type: String = "WeightRecord"): JSONObject {
        val time = date.atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return JSONObject().put("recordId", id).put("day", date.toString())
            .put("channel", channel).put("source", source).put("type", type)
            .put("startMs", time).put("endMs", time + 1000)
            .put("lastModifiedMs", time + 2000)
            .put("data", JSONObject().put("value", 72.3).put("unit", "kg"))
    }

    private fun put(vararg rows: JSONObject) {
        store.importRecords(JSONArray(rows.toList()), store.generation())
    }

    @Test fun legacyPositionalAndNamedOneDayConstructorsKeepExactlyOneDay() {
        put(row("before", today.minusDays(1)), row("today"), row("after", today.plusDays(1)))
        val legacy = HealthRecordQuery(today)
        assertEquals(today, legacy.fromDay)
        assertEquals(today, legacy.toDay)
        assertEquals(listOf("today"), reader.load(legacy).rows.map { it.id })
        val positional = HealthRecordQuery(today, HealthJournalStore.HC, "origin.alpha", "WeightRecord")
        assertEquals(listOf("today"), reader.load(positional).rows.map { it.id })
        assertEquals(listOf("today"), reader.load(HealthRecordQuery(today, type = "WeightRecord")).rows.map { it.id })
        assertEquals(25, NativeHealthRecords.PAGE_SIZE)
    }

    @Test fun rangeIncludesBothCalendarBoundariesButNeverDaysOutsideThem() {
        put(row("left-outside", from.minusDays(1)), row("first", from),
            row("middle", from.plusDays(1)), row("last", today), row("right-outside", today.plusDays(1)))
        val query = HealthRecordQuery(from, endDay = today)
        assertEquals(setOf("first", "middle", "last"), reader.load(query).rows.map { it.id }.toSet())
        assertEquals(listOf("middle"), reader.load(query.withRange(from.plusDays(1), from.plusDays(1))).rows.map { it.id })
    }

    @Test fun invalidAndReversedRangesAreRejectedBeforeReaderAccess() {
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(today, endDay = from) }
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(today).withRange(today, from) }
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(LocalDate.of(10000, 1, 1)) }
        assertThrows(java.time.DateTimeException::class.java) { LocalDate.parse("2026-02-30") }
        assertFalse(HealthJournalStore.validDay("2026-02-30"))
        assertFalse(HealthJournalStore.validDay("2026-10-1"))
    }

    @Test fun severalBoundedPagesHaveNoDuplicatesAndNoMissingRowsAcrossDays() {
        val records = (0 until 68).map { index ->
            row("record-$index", from.plusDays((index % 3).toLong()))
        }
        store.importRecords(JSONArray(records), store.generation())
        var query = HealthRecordQuery(from, endDay = today)
        val all = mutableListOf<String>()
        val sizes = mutableListOf<Int>()
        for (index in 0 until 4) {
            val page = reader.load(query)
            sizes += page.rows.size
            all += page.rows.map { it.id }
            if (page.next == null) break
            query = query.copy(cursor = page.next, generation = page.generation)
        }
        assertEquals(listOf(25, 25, 18), sizes)
        assertEquals(68, all.size)
        assertEquals(68, all.toSet().size)
        assertEquals((0 until 68).map { "record-$it" }.toSet(), all.toSet())
    }

    @Test fun channelSourceTypeAndDateFiltersComposeWithoutRelabelingProviders() {
        put(row("hc-start-sleep", from, type = "SleepSessionRecord"),
            row("hc-end-sleep", today, type = "SleepSessionRecord"),
            row("wrong-source", today, source = "origin.beta", type = "SleepSessionRecord"),
            row("wrong-type", today),
            row("wrong-channel", today, channel = HealthJournalStore.SAMSUNG, type = "SleepSessionRecord"),
            row("ble-hidden", today, channel = HealthJournalStore.BLE, type = "SleepSessionRecord"),
            row("outside", from.minusDays(1), type = "SleepSessionRecord"))
        val query = HealthRecordQuery(from, channel = HealthJournalStore.HC,
            source = "origin.alpha", type = "SleepSessionRecord", endDay = today)
        val page = reader.load(query)
        assertEquals(setOf("hc-start-sleep", "hc-end-sleep"), page.rows.map { it.id }.toSet())
        assertTrue(page.rows.all { it.channel == HealthJournalStore.HC && it.type == "SleepSessionRecord" })
        assertEquals(setOf("origin.alpha", "origin.beta"), page.origins.map { it.source }.toSet())
        val journal = reader.load(HealthRecordQuery(from, type = "SleepSessionRecord", endDay = today))
        assertEquals(4, journal.rows.size)
        assertTrue(journal.rows.none { it.channel == HealthJournalStore.BLE })
    }

    @Test fun changingRangeOrOtherFilterAfterPagingResetsCursorGenerationAndHistoryScope() {
        store.importRecords(JSONArray((0 until 54).map { row("old-$it", from) } +
            listOf(row("current", today), row("other-source", today, source = "origin.beta"))), store.generation())
        val firstQuery = HealthRecordQuery(from, endDay = today)
        val first = reader.load(firstQuery)
        assertEquals(25, first.rows.size)
        assertNotNull(first.next)
        val paged = firstQuery.copy(cursor = first.next, generation = first.generation)
        val second = reader.load(paged)
        assertNotEquals(first.rows.first().id, second.rows.first().id)

        val newRange = paged.withRange(today, today)
        assertNull(newRange.cursor)
        assertNull(newRange.generation)
        assertEquals(today, newRange.fromDay)
        assertEquals(today, newRange.toDay)
        assertEquals(setOf("current", "other-source"), reader.load(newRange).rows.map { it.id }.toSet())

        val filtered = paged.withFilters(source = "origin.beta", type = "WeightRecord")
        assertNull(filtered.cursor)
        assertNull(filtered.generation)
        assertEquals(from, filtered.fromDay)
        assertEquals(today, filtered.toDay)
        assertEquals(listOf("other-source"), reader.load(filtered).rows.map { it.id })
    }

    @Test fun restoreAndGenerationChangeRejectOldCursorAcrossMultiDayRange() {
        store.importRecords(JSONArray((0 until 27).map { row("old-$it", from.plusDays((it % 3).toLong())) }), store.generation())
        val query = HealthRecordQuery(from, endDay = today)
        val first = reader.load(query)
        assertNotNull(first.next)
        val continuation = query.copy(cursor = first.next, generation = first.generation)
        val token = store.prepareRestore(store.exportSnapshot())
        try {
            assertThrows(NativeHealthRecords.ReplacedJournal::class.java) { reader.load(query) }
            assertThrows(NativeHealthRecords.ReplacedJournal::class.java) { reader.load(continuation) }
        } finally { store.finishRestore(token, false) }

        store.erase()
        put(row("new", today))
        assertThrows(NativeHealthRecords.ReplacedJournal::class.java) { reader.load(continuation) }
        assertEquals(listOf("new"), reader.load(HealthRecordQuery(from, endDay = today)).rows.map { it.id })
    }
}
