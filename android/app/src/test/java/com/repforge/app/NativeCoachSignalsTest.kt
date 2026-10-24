package com.repforge.app

import com.repforge.app.ui.coach.NativeCoachSignals
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NativeCoachSignalsTest {
    private val today = LocalDate.of(2026,10,24)
    private val zone = ZoneId.of("UTC")
    private fun day(date: String = today.toString()) = JSONObject().put("day",date)
    private fun session(start: String, end: String, source: String = "hc") = JSONObject()
        .put("start",start).put("end",end).put("source",source)
    private fun signals(vararg rows: JSONObject) = NativeCoachSignals.fromDays(rows.toList(),today,zone)

    @Test fun missingSleepDoesNotInventMinutesFromAConstantOrSessionCount() {
        val snapshot=signals(day())
        assertTrue(snapshot.recovery.isNull("sleepMinutes"));assertFalse(snapshot.recovery.has("hrvBaselineMs"))
    }
    @Test fun overlappingSleepSessionsAreMergedAndInputIsNotModified() {
        val d=day().put("sleepSessions",JSONArray()
            .put(session("2026-10-23T22:00:00Z","2026-10-24T02:00:00Z"))
            .put(session("2026-10-24T00:00:00Z","2026-10-24T06:00:00Z")))
        val raw=d.toString();val r=signals(d).recovery
        assertEquals(480.0,r.getDouble("sleepMinutes"),0.0);assertEquals(raw,d.toString())
    }
    @Test fun sleepStagesWinOverWholeSessionDuration() {
        val s=session("2026-10-23T22:00:00Z","2026-10-24T06:00:00Z").put("stages",JSONArray()
            .put(session("2026-10-23T23:00:00Z","2026-10-24T03:00:00Z").put("isAsleep",true))
            .put(session("2026-10-24T03:00:00Z","2026-10-24T06:00:00Z").put("isAsleep",false)))
        assertEquals(240.0,signals(day().put("sleepSessions",JSONArray().put(s))).recovery.getDouble("sleepMinutes"),0.0)
    }
    @Test fun samsungStagesWithoutAsleepSpansCannotFallBackToWholeNight() {
        val s=session("2026-10-23T22:00:00Z","2026-10-24T06:00:00Z","com.sec.android.app.shealth")
            .put("provider","samsung_health").put("stages",JSONArray().put(
                session("2026-10-23T22:00:00Z","2026-10-24T06:00:00Z").put("isAsleep",false)))
        assertTrue(signals(day().put("sleepSessions",JSONArray().put(s))).recovery.isNull("sleepMinutes"))
    }
    @Test fun competingProvidersAreNotAddedTogetherAndSleepBelongsToWakeupDay() {
        val a=session("2026-10-23T22:00:00Z","2026-10-24T06:00:00Z","other")
        val b=session("2026-10-23T23:00:00Z","2026-10-24T06:00:00Z","com.sec.android.app.shealth")
        val rows=signals(day("2026-10-23").put("sleepSessions",JSONArray().put(a).put(b)),day())
        assertEquals("2026-10-24",rows.recovery.getString("day"))
        assertEquals(420.0,rows.recovery.getDouble("sleepMinutes"),0.0)
        assertEquals("com.sec.android.app.shealth",rows.recovery.getString("sleepSource"))
    }
    @Test fun hrvBaselineNeedsThreePriorSameSourceDaysWithinSevenDays() {
        val current=day().put("hrvRmssdMs",60).put("hrvSource","source-a")
        val a=day("2026-10-23").put("hrvRmssdMs",40).put("hrvSource","source-a")
        val b=day("2026-10-22").put("hrvRmssdMs",50).put("hrvSource","source-a")
        val c=day("2026-10-21").put("hrvRmssdMs",70).put("hrvSource","source-a")
        assertTrue(signals(current,a,b).recovery.isNull("hrvBaselineMs"))
        assertEquals(50.0,signals(current,a,b,c).recovery.getDouble("hrvBaselineMs"),0.0)
        c.put("hrvSource","source-b");assertTrue(signals(current,a,b,c).recovery.isNull("hrvBaselineMs"))
    }
}
