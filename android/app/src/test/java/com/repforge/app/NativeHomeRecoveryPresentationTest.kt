package com.repforge.app

import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachSignals
import com.repforge.app.ui.home.NativeHomeRecoveryLoadGate
import com.repforge.app.ui.home.NativeHomeRecoveryPresentation
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NativeHomeRecoveryPresentationTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val zone = ZoneId.of("UTC")

    private fun energy(value: Any, date: String = "2026-10-24", time: String = "2026-10-24T06:00:00Z",
                       provider: String = "samsung_health") = JSONObject()
        .put("day", date).put("energyScore", value).put("energyScoreTime", time)
        .put("energyScoreSource", provider)
    private fun result(vararg days: JSONObject, history: List<JSONObject> = emptyList(),
                       current: Instant = now, place: ZoneId = zone) =
        NativeHomeRecoveryPresentation.select(days.toList(), history, current, place)
    private fun effort(value: String = "pain", count: Int = 1) = JSONObject()
        .put("finished", "2026-10-24T10:00:00Z")
        .put("exercises", JSONArray().apply { repeat(count) { put(JSONObject().put("effort", value)) } })
    private fun sleepDay() = JSONObject().put("day", "2026-10-24").put("sleepSessions", JSONArray().put(
        JSONObject().put("source", "com.sec.android.app.shealth")
            .put("start", "2026-10-23T22:00:00Z").put("end", "2026-10-24T06:00:00Z")
    ))

    @Test fun todayVerifiedSamsungIsNotConfusedWithCoachPainLimitedRecommendation() {
        val row = energy(99.0)
        val history = listOf(effort("pain"))
        val home = result(row, history = history)
        val signals = NativeCoachSignals.fromDays(listOf(row), now.atZone(zone).toLocalDate(), zone)
        val coach = NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
        assertEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, home.source)
        assertEquals(99.0, home.value!!, 0.0)
        assertEquals("99%", home.displayValue)
        assertEquals("2026-10-24T06:00:00Z", home.timestamp)
        assertEquals(54.0, coach.score!!, 0.0)
    }

    @Test fun zeroAndHundredAreValidSamsungScaleBoundaries() {
        for (value in listOf(0.0, 100.0)) {
            val actual = result(energy(value))
            assertEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, actual.source)
            assertEquals(value, actual.value!!, 0.0)
            assertEquals("${value.toInt()}%", actual.displayValue)
        }
    }

    @Test fun invalidNumbersAndProvenanceNeverBecomeVerifiedSamsung() {
        for (value in listOf(-1.0, 100.01, "NaN", "Infinity", "bogus", JSONObject.NULL)) {
            val actual = result(energy(value))
            assertNotEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, actual.source)
            assertNull(actual.value)
        }
        for (row in listOf(
            energy(72.0, provider = "unknown"),
            energy(72.0).removeForTest("energyScoreTime"),
            energy(72.0, time = "2026-10-25T08:00:00Z"),
            energy(72.0, time = "2026-10-23T10:00:00Z"),
            energy(72.0, date = "2026-10-23", time = "2026-10-23T22:00:00Z")
        )) {
            assertEquals(NativeHomeRecoveryPresentation.Source.NONE, result(row).source)
        }
    }

    private fun JSONObject.removeForTest(key: String): JSONObject = apply { remove(key) }

    @Test fun staleYesterdayEnergyCannotBorrowThe36HourSleepWindow() {
        val yesterday = energy(91.0, date = "2026-10-23", time = "2026-10-23T20:00:00Z")
        val r = result(yesterday)
        assertEquals(NativeHomeRecoveryPresentation.Source.NONE, r.source)
        assertTrue(r.stale)
    }

    @Test fun sleepAndEffortEstimateIsExplicitlyTrainPilotNotSamsung() {
        val actual = result(sleepDay(), history = listOf(effort("challenging")))
        assertEquals(NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE, actual.source)
        assertNotNull(actual.value)
        assertNull(actual.timestamp)
        assertEquals(now, actual.calculatedAt)
        assertFalse(actual.refreshFailed)
    }

    @Test fun hrvWithValidBaselineCanDriveTrainPilotEstimate() {
        val rows = listOf(
            JSONObject().put("day","2026-10-24").put("hrvRmssdMs",65).put("hrvTime","2026-10-24T06:00:00Z").put("hrvSource","hc"),
            JSONObject().put("day","2026-10-23").put("hrvRmssdMs",50).put("hrvSource","hc"),
            JSONObject().put("day","2026-10-22").put("hrvRmssdMs",45).put("hrvSource","hc"),
            JSONObject().put("day","2026-10-21").put("hrvRmssdMs",55).put("hrvSource","hc")
        )
        val actual = NativeHomeRecoveryPresentation.select(rows, emptyList(), now, zone)
        assertEquals(NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE, actual.source)
        assertNotNull(actual.value)
        assertTrue(actual.stale) // Older observations form the real baseline, never imply fresh data.
    }

    @Test fun noSignalsCannotShowDefaultSeventyOrZero() {
        val actual = result(JSONObject().put("day","2026-10-24"))
        assertEquals(NativeHomeRecoveryPresentation.Source.NONE, actual.source)
        assertNull(actual.value)
        assertEquals("—", actual.displayValue)
    }

    @Test fun partialAndReadFailureAreExplicitAndDoNotHideVerifiedSource() {
        val partial = energy(82.0).put("partial", true)
        val actual = NativeHomeRecoveryPresentation.select(listOf(partial), emptyList(), now, zone, true)
        assertEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, actual.source)
        assertTrue(actual.partial)
        assertTrue(actual.refreshFailed)
    }

    @Test fun dayAndZoneChangeCanNeverRebrandOldEnergyAsToday() {
        val instant = Instant.parse("2026-10-24T23:30:00Z")
        val budapest = ZoneId.of("Europe/Budapest")
        val tokyo = ZoneId.of("Asia/Tokyo")
        val row = energy(91.0, date = "2026-10-25", time = "2026-10-24T23:00:00Z")
        assertEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, result(row, current = instant, place = budapest).source)
        assertEquals(NativeHomeRecoveryPresentation.Source.SAMSUNG, result(row, current = instant, place = tokyo).source)
        assertEquals(NativeHomeRecoveryPresentation.Source.NONE, result(row, current = instant, place = zone).source)
    }

    @Test fun outdatedCallbackTicketIsRejectedAfterRepeatedLoads() {
        val gate = NativeHomeRecoveryLoadGate()
        val prior = gate.next()
        assertTrue(gate.isCurrent(prior))
        val latest = gate.next()
        assertFalse(gate.isCurrent(prior))
        assertTrue(gate.isCurrent(latest))
        assertFalse(gate.isCurrent(gate.next() - 1L))
    }
}
