package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class Wear129Test {
    @Test fun duplicateOrInvalidPulseSamplesCannotChangeMeasuredAverage() {
        val accumulator=WearHealthAccumulator(JSONObject())
        accumulator.heartRate(100,100.0);accumulator.heartRate(100,220.0);accumulator.heartRate(90,120.0)
        accumulator.heartRate(101,Double.NaN);accumulator.heartRate(102,400.0);accumulator.heartRate(103,140.0)
        val data=accumulator.snapshot();assertEquals(2,data.getInt("heartRateSamples"));assertEquals(120.0,data.getDouble("averageHeartRate"),.001);assertEquals(140.0,data.getDouble("maxHeartRate"),.001)
        assertFalse(data.has("heartRateSum"));assertFalse(data.has("bootOrigin"))
    }
    @Test fun cumulativeTotalsPreserveZeroDoNotSumReplaysAndLeaveUnknownAbsent() {
        val accumulator=WearHealthAccumulator(JSONObject())
        accumulator.total("steps",0.0);accumulator.total("totalCalories",20.0);accumulator.total("totalCalories",20.0);accumulator.total("totalCalories",15.0)
        accumulator.total("distanceMeters",null);accumulator.total("speedMps",Double.POSITIVE_INFINITY)
        assertEquals(0.0,accumulator.snapshot().getDouble("steps"),.001);assertEquals(20.0,accumulator.snapshot().getDouble("totalCalories"),.001)
        assertFalse(accumulator.snapshot().has("distanceMeters"));assertFalse(accumulator.snapshot().has("speedMps"))
    }
    @Test fun calendarUsesPreparedProgramDaysAndIgnoresInvalidDates() {
        val day=JSONObject("""{"id":"A","name":"Alap A","programId":"p","programName":"Program","exercises":[{"id":"squat","sets":[{"set":1,"weight":5}]}]}""")
        val home=JSONObject().put("schema",1).put("days",org.json.JSONArray().put(day)).put("calendar",org.json.JSONArray()
            .put(JSONObject("""{"date":"2026-10-08","scheduleId":"s","plannedStart":"2026-10-08T18:00:00Z","programId":"p","dayId":"A","status":"planned"}"""))
            .put(JSONObject("""{"date":"invalid","status":"planned"}""")))
        val parsed=WatchHomeStore.parse(home.toString())!!;assertEquals(1,parsed.calendar.size);assertEquals("A",parsed.calendar.first().day!!.id)
        assertEquals("s",parsed.calendar.first().scheduleId)
        val legacy=WatchHomeStore.parse(JSONObject().put("days",org.json.JSONArray().put(day)).toString())!!;assertTrue(legacy.calendar.isEmpty())
    }
    @Test fun pendingSaveSurfaceDoesNotOfferASecondWorkout() {
        val pending=WearClosure("cmd","workout","finishWorkout","pending","",60,1,1)
        val glance=wearGlance(null,null,pending,0);assertEquals("summary",glance.destination);assertEquals("Szinkron",glance.short)
    }
}
