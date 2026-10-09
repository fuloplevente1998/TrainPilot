package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearRecordingStateTest {
    @Test fun processRestartPreservesCardioAndGpsInsteadOfStartingStrengthWithoutGps() {
        val data=JSONObject()
        WearRecordingRequest.restore(data,"running",true).save(data)
        val restarted=WearRecordingRequest.restore(JSONObject(data.toString()),null,null)
        assertEquals("running",restarted.exerciseType)
        assertTrue(restarted.gps)
        // Toggling preferences only affects the next recording, not a recovery.
        assertEquals(restarted,WearRecordingRequest.restore(data,"strength",false))
    }

    @Test fun recordingsFromOlderVersionsRestoreTheirMeasuredExerciseType() {
        val data=JSONObject().put("exerciseType","BIKING").put("gps",true)
        assertEquals(WearRecordingRequest("cycling",true),WearRecordingRequest.restore(data,null,null))
        assertEquals(WearRecordingRequest("cycling",true),WearRecordingRequest.restore(data,"strength",false))
        assertEquals(WearRecordingRequest("strength",false),WearRecordingRequest.restore(JSONObject(),null,null))
    }

    @Test fun stopDuringPendingStartWaitsForOwnershipAndEndsOnlyOnce() {
        val data=JSONObject().put("state","starting")
        val session=WearRecordingLifecycle(data)
        session.requestStop()
        assertFalse(session.beginEnding()) // A foreign/pending exercise must not be ended.
        assertEquals("ending",data.getString("state"))
        session.confirmOwnership()
        assertTrue(session.beginEnding())
        assertFalse(session.beginEnding())
        assertFalse(session.finished) // End command completion is not the final data flush.
        assertTrue(session.finish())
        assertFalse(session.finish()) // Callback and timeout cannot publish twice.
        assertFalse(session.beginEnding())
    }

    @Test fun interruptedStopIsRecoveredAsAStopRatherThanANewMeasurement() {
        val data=JSONObject().put("state","active")
        WearRecordingLifecycle(data).requestStop()
        val restored=WearRecordingLifecycle(JSONObject(data.toString()))
        assertTrue(restored.stopRequested)
        assertFalse(restored.beginEnding())
        restored.confirmOwnership()
        assertTrue(restored.beginEnding())
        assertTrue(WearRecordingLifecycle(JSONObject().put("state","ending")).stopRequested)
    }

    @Test fun finalFlushPreservesLastCumulativeTotalsAndPrivateRecoveryKeysStayOnWatch() {
        val data=JSONObject().put("workoutId","workout").put("state","active")
        val accumulator=WearHealthAccumulator(data)
        WearRecordingRequest("running",true).save(data)
        accumulator.segmentTotal("totalCalories",20.0)
        val session=WearRecordingLifecycle(data)
        session.confirmOwnership();session.requestStop();assertTrue(session.beginEnding())
        // Final update can arrive after endExerciseAsync completes.
        accumulator.segmentTotal("totalCalories",25.0)
        accumulator.segmentTotal("totalCalories",25.0)
        session.finish()
        val snapshot=accumulator.snapshot()
        assertEquals(25.0,snapshot.getDouble("totalCalories"),.001)
        assertEquals("ended",snapshot.getString("state"))
        for(key in listOf("recordingExerciseType","recordingGps","stopRequested"))assertFalse(snapshot.has(key))
        assertTrue(data.has("recordingGps"))
    }

    @Test fun summaryShowsMeasuredZerosAndLeavesMissingInvalidAndOtherWorkoutDataAbsent() {
        val data=JSONObject().put("workoutId","current").put("averageHeartRate",123.0)
            .put("steps",0.0).put("totalCalories",12.5).put("distanceMeters",-1.0).put("maxHeartRate","NaN")
        val totals=wearMeasuredTotals(data,"current")
        assertEquals(listOf("123 bpm","12,5 kcal","0 lépés"),totals.map { it.value })
        assertTrue(wearMeasuredTotals(data,"previous").isEmpty())
        assertTrue(wearMeasuredTotals(null,"current").isEmpty())
    }

    @Test fun recordingStateDistinguishesEnabledPreferenceFromActiveOrCompletedMeasurement() {
        assertEquals("Mérés a következő edzésnél",wearRecordingStatus(null,true))
        assertEquals("Mérés kikapcsolva",wearRecordingStatus(null,false))
        assertEquals("Mérés folyamatban · háttérben is",wearRecordingStatus(JSONObject().put("state","active"),true))
        assertEquals("Mérés lezárása…",wearRecordingStatus(JSONObject().put("state","ending"),true))
        assertEquals("Mérés befejezve",wearRecordingStatus(JSONObject().put("state","ended"),true))
    }
}
