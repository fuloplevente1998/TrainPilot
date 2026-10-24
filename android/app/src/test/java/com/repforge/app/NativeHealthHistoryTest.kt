package com.repforge.app

import android.content.Context
import com.repforge.app.ui.health.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeHealthHistoryTest {
    private lateinit var context: Context
    private lateinit var store: HealthJournalStore
    @Before fun open() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("health-journal.db")
        store = HealthJournalStore(context)
    }
    @After fun close() { store.close() }
    private fun put(day: String, weight: Double) { store.healthDay(day, JSONObject().put("source", "@aggregate").put("weightKg", weight), store.generation()) }
    @Test fun allProviderMetricKeysHaveExactlyOneDisplayDefinition() {
        assertEquals(HealthProviderProjection.METRICS.toSet(), NativeHealthMetrics.definitions.map { it.key }.toSet())
        assertEquals(26, NativeHealthMetrics.definitions.size)
    }
    @Test fun missingOrMalformedValuesAreUnavailableWhileZeroRemainsARealMeasurement() {
        val rows = NativeHealthMetrics.parse(context, JSONObject().put("weightKg", "").put("averageHeartRate", "invalid").put("steps", 0), "en").associateBy { it.key }
        assertFalse(rows.getValue("weightKg").isAvailable); assertFalse(rows.getValue("averageHeartRate").isAvailable)
        assertTrue(rows.getValue("steps").isAvailable); assertEquals("0", rows.getValue("steps").valueText)
    }
    @Test fun selectedMissingDayNeverFallsBackToAnEarlierWeight() {
        put("2026-10-02", 72.3)
        val page = NativeHealthHistory(store).load(NativeHealthQuery(LocalDate.parse("2026-10-03")))
        assertEquals(listOf("2026-10-02"), page.days)
        assertFalse(page.selected.has("weightKg")); assertEquals("2026-10-03", page.selected.getString("day"))
    }
    @Test fun historicalDayReadsTheStoredValueWithoutMixingOtherMonths() {
        put("2026-09-02", 71.0); put("2026-10-02", 72.3)
        val page = NativeHealthHistory(store).load(NativeHealthQuery(LocalDate.parse("2026-09-02")))
        assertEquals(71.0, page.selected.getDouble("weightKg"), .0001)
        assertEquals(listOf("2026-09-02"), page.days)
    }
    @Test fun channelFilterDoesNotRelabelHealthConnectAsSamsung() {
        put("2026-10-02", 72.3)
        val page = NativeHealthHistory(store).load(NativeHealthQuery(LocalDate.parse("2026-10-02"), channel = "samsung_health"))
        assertTrue(page.days.isEmpty()); assertFalse(page.selected.has("weightKg"))
    }
    @Test fun allDaysOfA31DayMonthRemainSelectable() {
        for (day in 1..31) put(LocalDate.of(2026, 10, day).toString(), day + 60.0)
        val page = NativeHealthHistory(store).load(NativeHealthQuery(LocalDate.parse("2026-10-01")))
        assertEquals(31, page.days.size); assertEquals(61.0, page.selected.getDouble("weightKg"), .0001)
    }
    @Test fun monthNavigationUsesCalendarBoundariesAndRejectsFutureAndUnknownChannels() {
        val q = NativeHealthQuery(LocalDate.parse("2026-03-31"))
        assertEquals(HealthJournalStore.BLE, q.copy(channel = HealthJournalStore.BLE).channel)
        assertEquals(LocalDate.parse("2026-02-28"), q.shift(-1, LocalDate.parse("2026-03-31")).day)
        assertThrows(IllegalArgumentException::class.java) { q.shift(1, LocalDate.parse("2026-03-31")) }
        assertThrows(IllegalArgumentException::class.java) { q.copy(channel = "made-up") }
        assertThrows(IllegalArgumentException::class.java) { q.copy(month = YearMonth.of(2026, 2)) }
    }
}
