package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseType

class WearRecordingStateTest {
    @Test fun grantedWhileInUseLocationNeedsLocationServiceEvenWhenGpsIsOff() {
        val access=WearRecordingAccess(gpsRequested=false,providerRequiresLocation=false,fineLocationGranted=true)
        assertTrue(access.foregroundLocation)
        assertFalse(access.gpsEnabled)
        assertFalse(access.locationBlocked)
    }

    @Test fun knownProviderLocationRequirementRequestsPermissionWithoutEnablingGps() {
        val denied=WearRecordingAccess(false,true,false)
        assertTrue(denied.needsFineLocation)
        assertTrue(denied.locationBlocked)
        assertFalse(denied.foregroundLocation)
        assertFalse(denied.gpsEnabled)
        val granted=denied.copy(fineLocationGranted=true)
        assertFalse(granted.locationBlocked)
        assertTrue(granted.foregroundLocation)
        assertFalse(granted.gpsEnabled)
        assertTrue(WearRecordingAccess(true,false,true).gpsEnabled)
        val independent=WearRecordingAccess(false,false,false)
        assertFalse(independent.needsFineLocation)
        assertFalse(independent.locationBlocked)
        assertFalse(independent.foregroundLocation)
    }

    @Test fun retryPreservesRealTotalsAndGpsIntentWhileClearingTheResolvedPermissionFailure() {
        val previous=JSONObject().put("state","error").put("locationPermissionRequired",true)
            .put("message",WEAR_LOCATION_PERMISSION_MESSAGE).put("stopRequested",true)
            .put("totalCalories",4.5).put("steps",12).put("recordingGps",false)
            .put("recordingExerciseType","running")
        val retry=wearRetryRecording(previous)!!
        assertEquals("starting",retry.getString("state"))
        assertFalse(retry.has("message"));assertFalse(retry.has("locationPermissionRequired"));assertFalse(retry.has("stopRequested"))
        assertEquals(4.5,retry.getDouble("totalCalories"),.001)
        assertEquals(12,retry.getInt("steps"));assertFalse(retry.getBoolean("recordingGps"))
        assertEquals("running",retry.getString("recordingExerciseType"))
        assertEquals("error",previous.getString("state"))
        assertNull(wearRetryRecording(JSONObject().put("state","active")))
        assertEquals("Pontos helyengedély szükséges",wearRecordingStatus(previous,true,true))
    }

    private val sensorMetrics: Set<DataType<*,*>> = setOf(DataType.HEART_RATE_BPM,DataType.CALORIES_TOTAL,
        DataType.STEPS_TOTAL,DataType.DISTANCE_TOTAL,DataType.SPEED)

    @Test fun deniedLocationKeepsIndependentSensorsAndAvoidsOutdoorProviderPermissionFailure() {
        val types=setOf(ExerciseType.RUNNING,ExerciseType.WORKOUT,ExerciseType.STRENGTH_TRAINING)
        for(type in listOf(ExerciseType.RUNNING,ExerciseType.WALKING,ExerciseType.BIKING)) {
            assertEquals(ExerciseType.WORKOUT,wearRecordingType(type,types,false))
        }
        assertEquals(setOf(DataType.HEART_RATE_BPM,DataType.CALORIES_TOTAL,DataType.STEPS_TOTAL),
            wearRecordingMetrics(sensorMetrics,true,true,false))
        assertEquals(ExerciseType.STRENGTH_TRAINING,wearRecordingType(ExerciseType.RUNNING,
            setOf(ExerciseType.RUNNING,ExerciseType.STRENGTH_TRAINING),false))
        assertNull(wearRecordingType(ExerciseType.RUNNING,setOf(ExerciseType.RUNNING),false))
        assertEquals(ExerciseType.STRENGTH_TRAINING,wearRecordingType(ExerciseType.STRENGTH_TRAINING,types,false))
    }

    @Test fun preciseLocationAllowsSupportedMovementMetricsAndOtherDeniedSensorsStayAbsent() {
        assertEquals(ExerciseType.RUNNING,wearRecordingType(ExerciseType.RUNNING,
            setOf(ExerciseType.RUNNING,ExerciseType.WORKOUT),true))
        assertEquals(sensorMetrics,wearRecordingMetrics(sensorMetrics,true,true,true))
        assertEquals(setOf(DataType.HEART_RATE_BPM),wearRecordingMetrics(sensorMetrics,true,false,true))
        assertEquals(sensorMetrics-DataType.HEART_RATE_BPM,wearRecordingMetrics(sensorMetrics,false,true,true))
        assertTrue(wearRecordingMetrics(sensorMetrics,false,false,true).isEmpty())
        assertEquals(setOf(DataType.STEPS_TOTAL),wearRecordingMetrics(setOf(DataType.STEPS_TOTAL),true,true,true))
    }

    @Test fun onlyTheReportedMissingLocationPermissionErrorIsEligibleForRecovery() {
        assertTrue(WearHealthFutures.isMissingLocationError("Mérési hiba: Missing permissions: [android.permission.ACCESS_FINE_LOCATION]"))
        assertFalse(WearHealthFutures.isMissingLocationError("Missing permissions: [android.permission.BODY_SENSORS]"))
        assertFalse(WearHealthFutures.isMissingLocationError("A mérési kapcsolat megszakadt"))
    }

    private fun workout(exercise: WearExercise) = WearWorkout(1,"p","Program","A","","2026-10-11T09:00:00Z",
        "current",0,60,0,listOf(exercise))

    @Test fun stepAndDistanceWorkoutsRequestMovementMetricsWithoutChangingStrengthWorkouts() {
        val strength=WearExercise("row","Evezés","weight","ism.","reps",emptyList())
        assertEquals(WearRecordingRequest("strength",false),wearRecordingRequestForWorkout(workout(strength),true))
        val step=strength.copy(id="custom-walk",name="Lépéses edzés",repUnit="lépés")
        assertEquals(WearRecordingRequest("walking",false),wearRecordingRequestForWorkout(workout(step),false))
        assertEquals(WearRecordingRequest("walking",true),wearRecordingRequestForWorkout(workout(step.copy(repUnit="steps")),true))
        val distance=strength.copy(id="custom",name="Séta",repUnit="mp",measurementType="distance")
        assertEquals("walking",wearRecordingRequestForWorkout(workout(distance),false).exerciseType)
        assertEquals("cycling",wearRecordingRequestForWorkout(workout(distance.copy(name="Kerékpár")),true).exerciseType)
        assertEquals("running",wearRecordingRequestForWorkout(workout(distance.copy(name="Futás")),false).exerciseType)
        assertEquals("walking",wearRecordingRequestForWorkout(workout(distance.copy(id="walking",name="Walking",measurementType="time")),false).exerciseType)
    }

    @Test fun workoutRowsShowMeasuredPulseEnergyAndMovementIncludingZeroButNeverBorrowTargetsOrPreviousData() {
        val data=measured("current",1,2.0).put("steps",0).put("distanceMeters",150).put("updatedAt",100000L)
        assertEquals(listOf("93 bpm · 2 kcal","Edzés: 0 lépés · 150 m"),wearLiveWorkoutRows(data,"current",100001L))
        assertTrue(wearLiveWorkoutRows(data,"next",100001L).isEmpty())
        assertTrue(wearLiveWorkoutRows(null,"current",100001L).isEmpty())
        data.put("state","paused")
        assertEquals("Mérés szünetel",wearLiveWorkoutRows(data,"current",100001L).last())
        data.put("state","active")
        assertEquals("Legutóbbi adat",wearLiveWorkoutRows(data,"current",130001L).last())
        data.remove("steps");data.remove("distanceMeters")
        assertEquals(listOf("93 bpm · 2 kcal"),wearLiveWorkoutRows(data,"current",100001L))
        data.put("steps",-1);data.put("distanceMeters","NaN")
        assertEquals(listOf("93 bpm · 2 kcal"),wearLiveWorkoutRows(data,"current",100001L))
    }

    private fun measured(id: String, revision: Long, calories: Double) = JSONObject()
        .put("schema",1).put("source","wear_health_services").put("workoutId",id)
        .put("watchId","sensor-watch").put("revision",revision).put("state","active")
        .put("heartRate",93.0).put("totalCalories",calories)

    @Test fun watchUsesTheSameMeasuredPayloadThatReachedThePhoneWhenItsStoreReadIsMissing() {
        val data=measured("current",3,2.0)
        val published=JSONObject().put("workoutId","current").put("summary",data)
        val display=wearHealthDisplaySummary("current",null,published)!!
        assertEquals(93.0,display.getDouble("heartRate"),.001)
        assertEquals(2.0,display.getDouble("totalCalories"),.001)
        data.put("totalCalories",99.0)
        assertEquals(2.0,display.getDouble("totalCalories"),.001) // Immutable UI snapshot.
    }

    @Test fun displayPreservesNewerFinalTotalsAndRejectsOtherWorkoutsOrDifferentWatches() {
        val stored=measured("current",5,4.0).put("state","ended")
        val older=JSONObject().put("workoutId","current").put("summary",measured("current",3,2.0))
        assertEquals(4.0,wearHealthDisplaySummary("current",stored,older)!!.getDouble("totalCalories"),.001)
        val newer=JSONObject().put("workoutId","current").put("summary",measured("current",6,5.0))
        assertEquals(5.0,wearHealthDisplaySummary("current",stored,newer)!!.getDouble("totalCalories"),.001)
        assertNull(wearHealthDisplaySummary("next",stored,newer))
        newer.getJSONObject("summary").put("workoutId","other")
        assertNull(wearHealthDisplaySummary("current",null,newer))
        newer.put("summary",measured("current",7,99.0).put("watchId","foreign-watch"))
        assertEquals(4.0,wearHealthDisplaySummary("current",stored,newer)!!.getDouble("totalCalories"),.001)
        assertEquals(0.0,wearHealthDisplaySummary("current",measured("current",1,0.0),null)!!.getDouble("totalCalories"),.001)
    }

    @Test fun enabledCurrentWorkoutWithoutMeasurementCanBeStartedExplicitly() {
        assertEquals("Az aktuális edzés mérése még nem indult",wearRecordingStatus(null,true,true))
        assertTrue(wearRecordingNeedsRetry(null,true,true))
        for(state in listOf("error","ended"))assertTrue(wearRecordingNeedsRetry(JSONObject().put("state",state),true,true))
        for(state in listOf("starting","active","paused","ending"))assertFalse(wearRecordingNeedsRetry(JSONObject().put("state",state),true,true))
        assertFalse(wearRecordingNeedsRetry(null,false,true))
        assertFalse(wearRecordingNeedsRetry(null,true,false))
    }
    @Test fun cachedMeasurementIsNotAnAcknowledgementAfterServiceRecovery() {
        val data=JSONObject().put("state","active").put("totalCalories",12.5)
        val lifecycle=WearRecordingLifecycle(data)
        assertEquals("starting",data.getString("state"))
        assertFalse(lifecycle.owned)
        assertEquals(12.5,data.getDouble("totalCalories"),.001)
        val closing=JSONObject().put("state","ending").put("stopRequested",true)
        assertTrue(WearRecordingLifecycle(closing).stopRequested)
        assertEquals("ending",closing.getString("state"))
    }
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
