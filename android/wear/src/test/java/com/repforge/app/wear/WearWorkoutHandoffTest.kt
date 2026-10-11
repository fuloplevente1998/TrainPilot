package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearWorkoutHandoffTest {
    @Test fun directReplyMustBelongToTheCurrentUnexpiredLaunch() {
        val start=WearMeasurementStart("current",42)
        val data=JSONObject().put("active",true).put("workoutId","current").put("revision",42)
        assertTrue(wearHandoffReplyMatches(start,15000,1000,data))
        assertTrue(wearHandoffReplyMatches(start,15000,1000,JSONObject(data.toString()).put("revision",43)))
        assertFalse(wearHandoffReplyMatches(start,15000,15001,data))
        assertFalse(wearHandoffReplyMatches(null,15000,1000,data))
        assertFalse(wearHandoffReplyMatches(start,15000,1000,JSONObject(data.toString()).put("workoutId","previous")))
        assertFalse(wearHandoffReplyMatches(start,15000,1000,JSONObject(data.toString()).put("revision",41)))
        assertFalse(wearHandoffReplyMatches(start,15000,1000,JSONObject(data.toString()).put("active",false)))
    }
}
