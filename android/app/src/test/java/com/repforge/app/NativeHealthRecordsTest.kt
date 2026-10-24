package com.repforge.app

import android.content.Context
import com.repforge.app.ui.health.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthRecordsTest {
    private lateinit var store: HealthJournalStore
    private lateinit var records: NativeHealthRecords
    private val day = LocalDate.parse("2026-10-24")
    private val time = Instant.parse("2026-10-24T10:00:00Z").toEpochMilli()
    @Before fun open() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("health-journal.db"); store = HealthJournalStore(context)
        records = NativeHealthRecords(store)
    }
    @After fun close() { store.close() }
    private fun row(id: String, type: String = "WeightRecord", source: String = "origin.a",
        channel: String = HealthJournalStore.HC, date: String = day.toString()) =
        JSONObject().put("recordId", id).put("type", type).put("source", source).put("channel", channel)
            .put("day", date).put("startMs", time).put("endMs", time + 1000).put("lastModifiedMs", time + 2000)
            .put("data", JSONObject().put("value", 72.3).put("unit", "kg"))
    private fun put(vararg rows: JSONObject) { store.importRecords(JSONArray(rows.toList()), store.generation()) }

    @Test fun equalTimestampRecordsPageWithoutDuplicatesOrSkippedIds() {
        store.importRecords(JSONArray((0 until 63).map { row("weight-$it") }), store.generation())
        var query = HealthRecordQuery(day)
        val ids = mutableListOf<String>(); val sizes = mutableListOf<Int>()
        do {
            val page = records.load(query)
            sizes.add(page.rows.size); ids.addAll(page.rows.map { it.id })
            query = query.copy(cursor = page.next, generation = page.generation)
        } while (page.next != null)
        assertEquals(listOf(25, 25, 13), sizes)
        assertEquals(63, ids.toSet().size); assertEquals(63, ids.size)
    }
    @Test fun typeFilterRunsInSqlBeforeTheLimitAndKeepsItsCursorScope() {
        put(*Array(60) { row("weight-$it") }, row("sleep", "SleepSessionRecord"))
        val page = records.load(HealthRecordQuery(day, type = "SleepSessionRecord"))
        assertEquals(listOf("sleep"), page.rows.map { it.id }); assertNull(page.next)
    }
    @Test fun originFilterDoesNotRelabelOrCombineProviders() {
        put(row("a"), row("b", source = "origin.b"), row("samsung", source = "origin.a", channel = HealthJournalStore.SAMSUNG))
        val page = records.load(HealthRecordQuery(day, channel = HealthJournalStore.HC, source = "origin.a"))
        assertEquals(listOf("a"), page.rows.map { it.id }); assertEquals(HealthJournalStore.HC, page.rows.single().channel)
        assertEquals(setOf("origin.a", "origin.b"), page.origins.map { it.source }.toSet())
    }
    @Test fun combinedJournalExcludesBleWhileExplicitBleRemainsBrowsable() {
        put(row("hc"), row("ble", channel = HealthJournalStore.BLE), row("samsung", channel = HealthJournalStore.SAMSUNG))
        assertEquals(setOf("hc", "samsung"), records.load(HealthRecordQuery(day)).rows.map { it.id }.toSet())
        assertEquals(listOf("ble"), records.load(HealthRecordQuery(day, channel = HealthJournalStore.BLE)).rows.map { it.id })
    }
    @Test fun dayScopeNeverSubstitutesAnOlderMeasurement() {
        put(row("yesterday", date = day.minusDays(1).toString()))
        assertTrue(records.load(HealthRecordQuery(day)).rows.isEmpty())
    }
    @Test fun replacementInvalidatesAnOldCursorEvenIfRowIdsAreReused() {
        store.importRecords(JSONArray((0 until 26).map { row("old-$it") }), store.generation())
        val page = records.load(HealthRecordQuery(day))
        store.erase(); put(row("new"))
        assertThrows(NativeHealthRecords.ReplacedJournal::class.java) {
            records.load(HealthRecordQuery(day, cursor = page.next, generation = page.generation))
        }
        assertEquals(listOf("new"), records.load(HealthRecordQuery(day)).rows.map { it.id })
    }
    @Test fun pendingRestoreDoesNotExposePartiallyInstalledRecords() {
        put(row("one"))
        val token = store.prepareRestore(store.exportSnapshot())
        try { assertThrows(NativeHealthRecords.ReplacedJournal::class.java) { records.load(HealthRecordQuery(day)) } }
        finally { store.finishRestore(token, false) }
    }
    @Test fun rawPagingDoesNotLoadDailySleepSummariesAndTheOldJavaApiStillDoes() {
        put(row("one")); store.healthDay(day.toString(), JSONObject().put("weightKg", 72.3), store.generation())
        val raw = store.page(day.toString(), day.toString(), "journal", "", 0, 0, 25, "", false)
        assertEquals(0, raw.getJSONArray("days").length())
        assertEquals(1, store.page(day.toString(), day.toString(), "journal", "", 0, 0, 25).getJSONArray("days").length())
    }
    @Test fun originChoicesAreBoundedToTheRequestedDayChannelAndType() {
        put(row("one", "SleepSessionRecord", "sleep-source"), row("weight", source = "weight-source"),
            row("old", "SleepSessionRecord", "old-source", date = day.minusDays(1).toString()),
            row("ble", "SleepSessionRecord", "ble-source", channel = HealthJournalStore.BLE))
        val page = records.load(HealthRecordQuery(day, channel = HealthJournalStore.HC, type = "SleepSessionRecord"))
        assertEquals(listOf("sleep-source"), page.origins.map { it.source })
    }
    @Test fun zeroIsAReadingButEmptyNullAndNonFiniteValuesAreNot() {
        assertEquals(0.0, NativeHealthRecords.number(JSONObject().put("value", 0), "value")!!, .0)
        for (value in listOf("", "NaN", "Infinity", JSONObject.NULL, true))
            assertNull(NativeHealthRecords.number(JSONObject().put("value", value), "value"))
    }
    @Test fun healthConnectStagesKeepExactTimesAndUseBoundedDetailPages() {
        val stages = JSONArray((0 until 45).map { i -> JSONObject().put("startMs", time + i * 1000)
            .put("endMs", time + (i + 1) * 1000).put("type", 5).put("isAsleep", true) })
        val data = JSONObject().put("stages", stages)
        val first = NativeHealthRecordSeries.page(data, 0)
        val last = NativeHealthRecordSeries.page(data, 40)
        assertEquals(20, first.items.size); assertEquals(45, first.total); assertEquals(5, last.items.size)
        assertEquals(time + 40000, last.items.first().startMs!!)
        assertEquals(R.string.health_sleep_deep, NativeHealthRecordSeries.stageLabel(first.items.first().stage))
    }
    @Test fun samsungIsoTimesAndNamedStagesAreNotMistakenForHealthConnectNumericStages() {
        val data = JSONObject().put("durationMinutes", 60).put("stages", JSONArray().put(JSONObject()
            .put("start", "2026-10-23T23:00:00+02:00").put("end", "2026-10-24T00:00:00+02:00").put("stage", "LIGHT")))
        val stage = NativeHealthRecordSeries.page(data, 0).items.single()
        assertEquals(Instant.parse("2026-10-23T21:00:00Z").toEpochMilli(), stage.startMs!!)
        assertEquals(3_600_000L, stage.endMs!! - stage.startMs!!)
        assertEquals(R.string.health_sleep_light, NativeHealthRecordSeries.stageLabel(stage.stage))
    }
    @Test fun malformedOrUnknownStagesDoNotBecomeAwakeOrInventedTimes() {
        val data = JSONObject().put("stages", JSONArray().put(JSONObject().put("type", 999)).put("malformed"))
        val stages = NativeHealthRecordSeries.page(data, 0).items
        assertEquals(2, stages.size)
        for (stage in stages) { assertNull(stage.startMs); assertEquals(R.string.health_sleep_unknown, NativeHealthRecordSeries.stageLabel(stage.stage)) }
    }
    @Test fun heartRateSamplesKeepZeroAndMissingValuesDistinct() {
        val data = JSONObject().put("samples", JSONArray().put(JSONObject().put("timeMs", time).put("value", 0))
            .put(JSONObject().put("timeMs", time + 1).put("value", "invalid")))
        val rows = NativeHealthRecordSeries.page(data, 0).items
        assertEquals(0.0, rows[0].value!!, .0); assertNull(rows[1].value)
        assertEquals(time, rows[0].startMs!!)
    }
    @Test fun invalidQueriesCannotEnterTheJournalReader() {
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(day, channel = "invalid") }
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(day, source = "a".repeat(256)) }
        assertThrows(IllegalArgumentException::class.java) { HealthRecordQuery(day, cursor = HealthRecordCursor(1, time)) }
        assertThrows(IllegalArgumentException::class.java) { NativeHealthRecordSeries.page(JSONObject(), -1) }
    }
    @Test fun aggregateOnlyDataDoesNotCreateFabricatedRawMeasurements() {
        store.healthDay(day.toString(), JSONObject().put("steps", 100).put("weightKg", 72.3), store.generation())
        val page = records.load(HealthRecordQuery(day))
        assertTrue(page.rows.isEmpty()); assertNull(page.next)
    }
}
