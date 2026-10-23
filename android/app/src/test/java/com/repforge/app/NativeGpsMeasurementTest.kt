package com.repforge.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.journal.WorkoutRouteCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeGpsMeasurementTest {
    private lateinit var context: Context
    private lateinit var clock: ServiceController<IntervalTimerService>
    private var gps: ServiceController<DistanceTrackingService>? = null
    private lateinit var db: TrainPilotDatabase
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private fun store() = PhoneWorkoutExecution.store(context)
    private fun frame() = store().snapshot()!!
    private fun target() = NativeWorkoutTarget("gps-workout", frame().getLong("generation"), "run", "leg-a")
    private fun set(frame: JSONObject = frame()) = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun binding() = frame().getJSONObject(NativeGpsMeasurement.FIELD)
    private fun advance(seconds: Long) = ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds))
    private fun intent(binding: JSONObject = binding()) = Intent(context, DistanceTrackingService::class.java).apply {
        action = DistanceTrackingService.START
        putExtra("key", binding.getString("key")); putExtra("measurementId", binding.getString("id")); putExtra("language", "hu")
    }

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(DistanceTrackingService.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(context.getSystemService(LocationManager::class.java)).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        NativeWorkoutSession.installPrepared(context, JSONObject("""{
          "active":true,"timerOwner":"phone","workoutId":"gps-workout","id":"gps-workout","syncId":"gps-workout",
          "started":"2026-10-23T12:00:00Z","currentExercise":0,"automaticEnabled":true,
          "timerConfig":{"work":30,"rest":0,"rounds":1},"restSeconds":0,
          "exercises":[{"id":"run","name":"Futás","repUnit":"km","measurementType":"distance","loadType":"bodyweight","sets":[
          {"setId":"leg-a","set":1,"done":false,"reps":"","weight":0}]}]}"""))
        clock = Robolectric.buildService(IntervalTimerService::class.java).create()
        clock.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM; putExtra("workoutId", "gps-workout"); putExtra("generation", target().generation)
        }, 0, 1)
    }

    @After fun close() {
        gps?.destroy(); clock.destroy(); db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }

    private fun start() {
        NativeWorkoutEditing.startGps(context, target())
        gps?.destroy()
        gps = Robolectric.buildService(DistanceTrackingService::class.java).create()
        gps!!.get().onStartCommand(intent(), 0, 1)
        assertTrue(DistanceTrackingService.snapshot(context).getBoolean("active"))
    }

    private fun fix(latitude: Double) {
        gps!!.get().onLocationChanged(Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = latitude; longitude = 19.04; accuracy = 5f
            elapsedRealtimeNanos = SystemClock.elapsedRealtime() * 1_000_000L
            time = System.currentTimeMillis(); speed = 1.1f
        })
    }

    private fun walk() {
        advance(1); fix(47.5)
        advance(5); fix(47.50005)
        advance(5); fix(47.50010)
        assertTrue(DistanceTrackingService.snapshot(context).getDouble("distanceMeters") > 0.0)
    }

    private fun finish() = NativeWorkoutEditing.finishGps(context, target(), binding().getString("id"))

    @Test fun positiveExplicitFinishRecordsWithoutOkKeepsPrivateRouteAndSupportsNativeUndo() {
        start(); walk()
        val id = binding().getString("id")
        val saved = finish()
        assertTrue(set().getBoolean("done")); assertEquals("11", set().getString("reps"))
        assertTrue(set().getDouble("distanceMeters") > 0.0)
        assertEquals(binding().getString("key"), set().getString("gpsRouteKey163"))
        assertEquals("gps", set().getString("distanceSource"))
        val route = WorkoutRouteCoordinates.load(context, set().getString("gpsRouteKey163"))!!
        assertTrue(route.pointCount >= 2)
        assertFalse(frame().toString().contains("latitude")); assertFalse(frame().toString().contains("longitude"))
        assertEquals(0, store().pendingEvents("gps-workout", target().generation, 0).length())
        NativeWorkoutEditing.finishGps(context, target(), id)
        store().readableDatabase.rawQuery("SELECT COUNT(*) FROM recordings WHERE command_id=?", arrayOf("gps-finish:$id")).use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertFalse(set().getBoolean("done"))
        assertEquals(0.0, set().optDouble("distanceMeters", 0.0), 0.0)
        assertEquals(route.pointCount, GpsRouteStore(context.noBackupFilesDir).read(route.routeKey).pointCount)
    }

    @Test fun noFixAndZeroDistanceNeverCompleteOrCreateAnUndoRecord() {
        start(); advance(6); finish()
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        assertEquals(0.0, set().optDouble("distanceMeters", 0.0), 0.0)
        assertFalse(set().has("gpsRouteKey163"))
    }

    @Test fun gpsObservedWorkIsCountedOnceAndSurvivesCaptureReplayAndUndo() {
        start(); walk()
        val saved = finish()
        assertEquals(11_000L, NativeMeasuredTime.summary(frame()).workMs)
        assertFalse(store().checkpointGps(DistanceTrackingService.snapshot(context)))
        NativeWorkoutEditing.undo(context, saved.getJSONObject("lastRecord188").getString("recordId"))
        assertEquals(11_000L, NativeMeasuredTime.summary(frame()).workMs)
    }

    @Test fun implicitStopPreservesMeasuredValuesAndRouteWithoutCompletion() {
        start(); walk()
        DistanceTrackingService.stop(context)
        assertFalse(set().getBoolean("done"))
        assertThrows(IllegalStateException::class.java) { finish() }
        NativeWorkoutEditing.interruptGps(context)
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        assertTrue(set().getDouble("distanceMeters") > 0)
        assertNotNull(WorkoutRouteCoordinates.load(context, set().getString("gpsRouteKey163")))
    }

    @Test fun controllerDestructionCannotMintAnExplicitStopReceipt() {
        start(); walk(); gps!!.destroy(); gps = null
        assertThrows(IllegalStateException::class.java) { finish() }
        NativeWorkoutEditing.interruptGps(context)
        assertFalse(set().getBoolean("done")); assertTrue(set().getDouble("distanceMeters") > 0)
    }

    @Test fun nativePauseStopsGpsAndKeepsPartialValuesWithoutCompletingTheSet() {
        start(); walk()
        PhoneWorkoutExecution.setWorkoutPause(context, ActiveWorkoutStore.current(context)!!
            .put("workoutPaused", true).put("pauseRevision", 1))
        assertTrue(frame().getBoolean("workoutPaused"))
        assertFalse(DistanceTrackingService.snapshot(context).getBoolean("active"))
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
        assertTrue(set().getDouble("distanceMeters") > 0)
        assertNotNull(WorkoutRouteCoordinates.load(context, set().getString("gpsRouteKey163")))
    }

    @Test fun staleNativePauseCannotInterruptTheCurrentGpsMeasurement() {
        start(); walk()
        val current = ActiveWorkoutStore.current(context)!!
        for (invalid in listOf(
            JSONObject(current.toString()).put("generation", target().generation + 1).put("pauseRevision", 1),
            JSONObject(current.toString()).put("pauseRevision", 0),
            JSONObject(current.toString()).put("pauseRevision", 9_007_199_254_740_992L)
        )) {
            assertThrows(Exception::class.java) { PhoneWorkoutExecution.setWorkoutPause(context, invalid.put("workoutPaused", true)) }
            assertTrue(DistanceTrackingService.snapshot(context).getBoolean("active"))
            assertFalse(frame().optBoolean("workoutPaused")); assertFalse(set().getBoolean("done"))
        }
    }

    @Test fun sqlFailureAfterDurableStopSurvivesServiceLossAndRetriesOnce() {
        start(); walk()
        val id = binding().getString("id")
        store().writableDatabase.execSQL("CREATE TRIGGER gps_disk_full BEFORE INSERT ON recordings BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        assertThrows(Exception::class.java) { finish() }
        assertFalse(set().getBoolean("done")); assertFalse(DistanceTrackingService.snapshot(context).getBoolean("active"))
        assertEquals("explicitStopped", binding().getString("state"))
        gps!!.destroy(); gps = null
        store().writableDatabase.execSQL("DROP TRIGGER gps_disk_full")
        NativeWorkoutEditing.finishGps(context, target(), id)
        assertTrue(set().getBoolean("done"))
        store().readableDatabase.rawQuery("SELECT COUNT(*) FROM recordings WHERE command_id=?", arrayOf("gps-finish:$id")).use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
    }

    @Test fun continuedSetKeepsRouteSegmentsAndTotalsButRejectsOldMeasurementCaptureAndFinish() {
        start(); walk()
        val oldStatus = DistanceTrackingService.snapshot(context)
        val oldId = binding().getString("id"); val key = binding().getString("key")
        NativeWorkoutEditing.interruptGps(context)
        val firstMeters = set().getDouble("distanceMeters")
        val firstPoints = GpsRouteStore(context.noBackupFilesDir).read(key).pointCount
        start()
        assertEquals(key, binding().getString("key")); assertNotEquals(oldId, binding().getString("id"))
        assertFalse(store().checkpointGps(oldStatus))
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.finishGps(context, target(), oldId) }
        assertTrue(DistanceTrackingService.snapshot(context).getBoolean("active"))
        walk(); finish()
        assertTrue(set().getBoolean("done")); assertEquals("22", set().getString("reps"))
        assertTrue(set().getDouble("distanceMeters") > firstMeters)
        val route = GpsRouteStore(context.noBackupFilesDir).read(key)
        assertTrue(route.pointCount > firstPoints); assertEquals(2, route.segments.size)
    }

    @Test fun cancelledQueuedStartCannotReappearAfterItsFinishButton() {
        NativeWorkoutEditing.startGps(context, target())
        val queued = intent(); val id = binding().getString("id")
        NativeWorkoutEditing.finishGps(context, target(), id)
        gps = Robolectric.buildService(DistanceTrackingService::class.java).create()
        gps!!.get().onStartCommand(queued, 0, 1)
        assertFalse(DistanceTrackingService.snapshot(context).getBoolean("active"))
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
    }

    @Test fun staleWorkoutOrGenerationCannotStopTheCurrentMeasurement() {
        start(); walk(); val current = target(); val id = binding().getString("id")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.finishGps(context, current.copy(workoutId = "older"), id) }
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.finishGps(context, current.copy(generation = current.generation + 1), id) }
        assertTrue(DistanceTrackingService.snapshot(context).getBoolean("active")); assertFalse(set().getBoolean("done"))
    }

    @Test fun gpsTargetAndDurableCaptureAreProtectedFromStaleUiEdits() {
        start()
        val stale = ActiveWorkoutStore.current(context)!!
        walk()
        val expected = binding().getJSONObject("capture").getDouble("distanceMeters")
        stale.getJSONObject(NativeGpsMeasurement.FIELD).remove("capture")
        stale.put("notes", "keep my note")
        NativeWorkoutEditing.commitEdit(context, stale)
        assertEquals(expected, binding().getJSONObject("capture").getDouble("distanceMeters"), 0.0)
        val tampered = ActiveWorkoutStore.current(context)!!
        set(tampered).put("reps", "900")
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.commitEdit(context, tampered) }
        assertFalse(set().getBoolean("done"))
    }

    @Test fun invalidOrForeignCaptureCannotEnterTheBoundExecutionFrame() {
        start(); walk()
        val status = DistanceTrackingService.snapshot(context)
        for (invalid in listOf(JSONObject(status.toString()).put("distanceMeters", -1),
            JSONObject(status.toString()).put("elapsedSeconds", -1),
            JSONObject(status.toString()).put("nativeGeneration", target().generation + 1),
            JSONObject(status.toString()).put("nativeWorkoutId", "different"),
            JSONObject(status.toString()).put("measurementId", "old"))) assertFalse(store().checkpointGps(invalid))
        assertFalse(set().getBoolean("done"))
    }

    @Test fun missingPrecisePermissionDoesNotPrepareOrStartMeasurement() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.startGps(context, target()) }
        assertFalse(frame().has(NativeGpsMeasurement.FIELD))
    }

    @Test fun disabledSystemLocationDoesNotPrepareOrStartMeasurement() {
        shadowOf(context.getSystemService(LocationManager::class.java)).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.startGps(context, target()) }
        assertFalse(frame().has(NativeGpsMeasurement.FIELD))
    }

    @Test fun openingRouteWhileTrackingDoesNotStopOrCompleteMeasurement() {
        start(); walk()
        assertTrue(WorkoutRouteCoordinates.load(context, binding().getString("key"))!!.pointCount >= 2)
        assertTrue(DistanceTrackingService.snapshot(context).getBoolean("active"))
        assertFalse(set().getBoolean("done")); assertFalse(frame().has("lastRecord188"))
    }

    @Test fun completedGpsReachesRoomAndTheJournalPrivateRouteReader() = runTest(dispatcher) {
        start(); walk(); finish()
        TrainPilotWorkoutFinalization(context, db).finishWorkout(ActiveWorkoutStore.current(context)!!)
        val saved = db.workoutHistoryDao().getWorkoutWithDetails("gps-workout")!!
        val set = saved.exercises.single().sets.single()
        assertTrue(set.done); assertTrue(set.distanceMeters!! > 0); assertEquals("11", set.reps)
        assertNotNull(set.gpsRouteKey163)
        assertTrue(WorkoutRouteCoordinates.load(context, set.gpsRouteKey163!!)!!.pointCount >= 2)
        assertEquals(11_000L, JSONObject(saved.workout.rawJson).getJSONObject("automaticTime162").getLong("workMs"))
        assertNotNull(saved.workout.timeLogs162Json)
    }

    @Test fun workoutFinishPreservesActiveGpsValuesWithoutPretendingMeasurementWasExplicitlyFinished() = runTest(dispatcher) {
        start(); walk()
        TrainPilotWorkoutFinalization(context, db).finishWorkout(ActiveWorkoutStore.current(context)!!)
        val saved = db.workoutHistoryDao().getWorkoutWithDetails("gps-workout")!!
        val set = saved.exercises.single().sets.single()
        assertFalse(set.done); assertEquals(0, saved.workout.completedSetsCount)
        assertTrue(set.distanceMeters!! > 0); assertEquals("11", set.reps)
        assertTrue(WorkoutRouteCoordinates.load(context, set.gpsRouteKey163!!)!!.pointCount >= 2)
        assertFalse(DistanceTrackingService.snapshot(context).getBoolean("active"))
        assertEquals(11_000L, JSONObject(saved.workout.rawJson).getJSONObject("automaticTime162").getLong("workMs"))
    }
}
