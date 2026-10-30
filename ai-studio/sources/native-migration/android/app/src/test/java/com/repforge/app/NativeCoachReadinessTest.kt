package com.repforge.app

import com.repforge.app.ui.coach.NativeCoachReadiness
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NativeCoachReadinessTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val zone = ZoneId.of("UTC")
    private fun energy(n: Double = 87.6) = JSONObject().put("energyScore",n).put("energyScoreSource","samsung_health")
        .put("energyScoreTime","2026-10-24T02:00:00Z").put("day","2026-10-24")
    private fun recovery() = JSONObject().put("sleepMinutes",480).put("sleepEnd","2026-10-24T07:00:00Z")
        .put("hrvRmssdMs",60).put("hrvTime","2026-10-24T08:00:00Z").put("hrvBaselineMs",50)
    private fun workout(effort: String, count: Int = 1, time: String = "2026-10-23T10:00:00Z"): JSONObject =
        JSONObject().put("finished",time).put("exercises",org.json.JSONArray().apply { repeat(count) { put(JSONObject().put("effort",effort)) } })
    private fun score(rec: JSONObject = JSONObject(), daily: JSONObject = JSONObject(), rows: List<JSONObject> = emptyList()) =
        NativeCoachReadiness.calculate(rec,daily,rows,now,zone)

    @Test fun absentSignalsDoNotInventAReadinessScore() {
        val r=score();assertNull(r.score);assertNull(r.trainpilotScore);assertNull(r.samsungScore);assertEquals("unknown",r.mode)
    }
    @Test fun samsungEnergyIsNotIncreasedByGoodSleepOrHrvAndKeepsPrecision() {
        val r=score(recovery(),energy());assertEquals(87.6,r.score!!,0.0);assertEquals(92,r.trainpilotScore)
        assertEquals(87.6,r.samsungScore!!,0.0);assertEquals("samsung_health",r.samsungSource)
    }
    @Test fun painLimitsHighSamsungEnergyTo54WithoutChangingItsOriginalValue() {
        val r=score(recovery(),energy(99.0),listOf(workout("pain")))
        assertEquals(54.0,r.score!!,0.0);assertEquals(99.0,r.samsungScore!!,0.0);assertTrue(r.pain)
    }
    @Test fun originalFiveStepEffortWeightsArePreserved() {
        for ((effort,load) in mapOf("easy" to 100,"light" to 100,"good" to 100,"challenging" to 96,"hard" to 92,"pain" to 80))
            assertEquals(load,score(rows=listOf(workout(effort))).load)
    }
    @Test fun severeAndModerateLoadsOnlyReduceSamsungReadiness() {
        assertEquals(54.0,score(daily=energy(99.0),rows=listOf(workout("hard",6))).score!!,0.0)
        assertEquals(74.0,score(daily=energy(99.0),rows=listOf(workout("hard",4))).score!!,0.0)
        assertEquals(30.0,score(daily=energy(30.0),rows=listOf(workout("hard",6))).score!!,0.0)
    }
    @Test fun staleAndFutureReadingsAreIgnored() {
        val rec=recovery().put("sleepEnd","2026-10-20T07:00:00Z").put("hrvTime","2026-10-25T08:00:00Z")
        assertNull(score(rec).score)
        assertNull(score(daily=energy().put("energyScoreTime","2026-10-25T02:00:00Z")).score)
    }
    @Test fun samsungNeedsVerifiedSourceAndTimestampForToday() {
        for (key in listOf("energyScoreSource","energyScoreTime")) { val d=energy();d.remove(key);assertNull(score(daily=d).score) }
        assertNull(score(daily=energy().put("day","2026-10-23")).score)
        assertNull(score(daily=energy().put("energyScoreTime","2026-10-23T23:00:00Z")).score)
    }
    @Test fun recoveryFallbackIsIndependentWhenSamsungReadingIsInvalid() {
        assertEquals(92.0,score(recovery(),energy().put("energyScoreSource","unknown")).score!!,0.0)
    }
    @Test fun onlyTheThreeLatestWorkoutsWithin14DaysContribute() {
        val rows=listOf(workout("good",time="2026-10-24T10:00:00Z"),workout("good",time="2026-10-23T10:00:00Z"),
            workout("good",time="2026-10-22T10:00:00Z"),workout("pain",time="2026-10-21T10:00:00Z"),
            workout("pain",time="2026-10-25T10:00:00Z"),workout("pain",time="2026-10-01T10:00:00Z"))
        val r=score(rows=rows);assertFalse(r.pain);assertEquals(100,r.load)
    }
    @Test fun missingExerciseEffortUsesWorkoutFeedbackAndDoesNotMutateInput() {
        val h=workout("").put("feedback",JSONObject().put("effort","challenging"));val raw=h.toString()
        assertEquals(96,score(rows=listOf(h)).load);assertEquals(raw,h.toString())
    }
    @Test fun explicitInvalidFreshnessTimeDoesNotFallBackToToday() {
        val rec=recovery().put("sleepEnd","invalid").put("hrvTime","invalid").put("day","2026-10-24")
        assertNull(score(rec).score)
    }
}
