package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tests the real Room attach/projection path, not SDK permission/service success. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeWorkoutHealthTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var mapper: LegacyWebViewMigrator
    private lateinit var health: NativeWorkoutHealth
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        mapper = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
        health = NativeWorkoutHealth(context, db)
    }
    @After fun cleanup() {
        ActiveWorkoutStore.clear(context)
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        db.close(); Dispatchers.resetMain()
    }
    private suspend fun current() = db.workoutHistoryDao().getWorkoutWithDetailsById("health-workout")!!
    private suspend fun seed(): WorkoutHistoryWithDetails {
        mapper.saveWorkoutJsonToRoom(JSONObject().put("id", "health-workout").put("syncId", "health-sync").put("key", "health-key")
            .put("started", "2026-10-20T10:00:00Z").put("finished", "2026-10-20T10:30:00Z")
            .put("wear", JSONObject().put("totalCalories", 500).put("averageHeartRate", 117))
            .put("future", JSONObject().put("keep", true)).put("nativeHealthWindowStale", true)
            .put("nativeOriginalHealthWindow", JSONObject().put("started", "2026-10-20T09:00:00Z").put("finished", "2026-10-20T09:30:00Z"))
            .put("exercises", JSONArray().put(JSONObject().put("id", "running").put("repUnit", "km")
                .put("measurementType", "distance").put("sets", JSONArray().put(JSONObject().put("setId", "measured-set")
                    .put("reps", "60").put("distanceMeters", 100).put("done", true).put("gpsRouteKey193", "route-keep")
                    .put("futureSet", true))))))
        return current()
    }
    private fun response(calories: Int = 243) = JSONObject().put("start", "2026-10-20T10:00:00Z")
        .put("end", "2026-10-20T10:30:00Z").put("activeCalories", calories).put("steps", 0)
        .put("sources", JSONArray().put("com.sec.android.app.shealth")).put("warnings", JSONArray())
    private suspend fun reject(operation: suspend () -> Unit) {
        var failure: Exception? = null
        try { operation() } catch (error: Exception) { failure = error }
        assertNotNull("Expected stale/deleted data to be rejected", failure)
    }

    @Test fun attachesToTheExactWindowWhilePreservingLaterSetEditsWearAndUnknownMetadata() = runTest(dispatcher) {
        val before = seed()
        val request = health.prepare(before.workout)
        val edited = JSONObject(before.workout.rawJson)
        edited.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps", "75")
        mapper.saveWorkoutJsonToRoom(edited)
        health.attach(request, response())
        val after = current()
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertEquals("health-sync", after.workout.syncId); assertEquals("health-key", after.workout.workoutKey)
        assertEquals("75", after.exercises.single().sets.single().reps)
        assertEquals("route-keep", after.exercises.single().sets.single().gpsRouteKey193)
        assertEquals(before.workout.wearMetricsJson, after.workout.wearMetricsJson)
        val raw = JSONObject(after.workout.rawJson)
        assertTrue(raw.getJSONObject("future").getBoolean("keep"))
        assertTrue(raw.getBoolean("nativeHealthWindowStale")) // Original Wear window remains distinct.
        val hc = JSONObject(after.workout.health240Json!!)
        assertEquals(243, hc.getInt("activeCalories")); assertEquals(0, hc.getInt("steps"))
        assertTrue(NativeWorkoutHealth.matchesWindow(hc, after.workout)); assertTrue(hc.getString("syncedAt").isNotBlank())
        assertEquals(500, JSONObject(after.workout.wearMetricsJson!!).getInt("totalCalories"))
    }

    @Test fun providerMismatchAndChangedWorkoutTimesCannotAttachOldMeasurements() = runTest(dispatcher) {
        val before = seed(); val request = health.prepare(before.workout)
        reject { health.attach(request, response().put("end", "2026-10-20T11:00:00Z")) }
        assertNull(current().workout.health240Json)
        mapper.saveWorkoutJsonToRoom(JSONObject(before.workout.rawJson).put("finished", "2026-10-20T10:40:00Z"))
        reject { health.attach(request, response()) }
        assertNull(current().workout.health240Json)
        assertEquals("2026-10-20T10:40:00Z", current().workout.finished)
    }

    @Test fun aSecondRefreshCannotBeOverwrittenByAnEarlierResponse() = runTest(dispatcher) {
        val before = seed()
        val older = health.prepare(before.workout); val newer = health.prepare(before.workout)
        health.attach(newer, response(500))
        val saved = current()
        reject { health.attach(older, response(243)) }
        assertEquals(saved, current())
    }

    @Test fun tombstonedOrDeletedWorkoutsAreNeverRecreatedByAHealthResponse() = runTest(dispatcher) {
        val before = seed(); val request = health.prepare(before.workout)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(before.workout.id, before.workout.workoutKey,
            JSONArray().put(before.workout.id).toString(), 1L))
        reject { health.prepare(before.workout) }
        reject { health.attach(request, response()) }
        db.workoutHistoryDao().deleteWorkoutByIdOrKey(before.workout.id)
        reject { health.attach(request, response()) }
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun restoringEvenIdenticalDataInvalidatesTheOutstandingRead() = runTest(dispatcher) {
        val before = seed(); val request = health.prepare(before.workout)
        NativeDataReplacement.exclusively(context) { /* Same row can be restored into a new dataset epoch. */ }
        reject { health.attach(request, response()) }
        assertEquals(before, current())
    }

    @Test fun sourceProjectionKeepsZerosAndMissingDistinctAndNeverMixesWearAndHcEnergy() = runTest(dispatcher) {
        val row = seed().workout
        val wear = JSONObject().put("totalCalories", 500).put("activeCalories", 999)
            .put("averageHeartRate", 0).put("heartRateSamples", 0).put("steps", 0).put("distanceMeters", -1)
        val hc = response().put("averageHeartRate", JSONObject.NULL).put("maxHeartRate", "NaN")
        val wearValues = NativeWorkoutHealth.metrics(wear, true)
        val hcValues = NativeWorkoutHealth.metrics(hc, false)
        assertEquals(500.0, wearValues.getValue("totalCalories"), 0.0)
        assertFalse(wearValues.containsKey("activeCalories")); assertFalse(wearValues.containsKey("averageHeartRate"))
        assertEquals(0.0, wearValues.getValue("heartRateSamples"), 0.0); assertEquals(0.0, hcValues.getValue("steps"), 0.0)
        assertFalse(wearValues.containsKey("distanceMeters")); assertFalse(hcValues.containsKey("maxHeartRate"))
        assertEquals(243.0, hcValues.getValue("activeCalories"), 0.0); assertFalse(hcValues.containsKey("totalCalories"))
        assertFalse(NativeWorkoutHealth.matchesWindow(hc, row))
        assertTrue(NativeWorkoutHealth.matchesWindow(hc.put("windowStart", "2026-10-20T12:00:00+02:00")
            .put("windowEnd", "2026-10-20T12:30:00+02:00"), row))
    }

    @Test fun actualPortableExportRetainsTheRefreshedWindowAndBothSeparateSources() = runTest(dispatcher) {
        val before = seed(); health.attach(health.prepare(before.workout), response())
        val exported = mapper.exportBackupJson(false).getJSONArray("history").getJSONObject(0)
        assertEquals(243, exported.getJSONObject("health240").getInt("activeCalories"))
        assertEquals(500, exported.getJSONObject("wear").getInt("totalCalories"))
        assertTrue(exported.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("futureSet"))
        mapper.saveWorkoutJsonToRoom(exported)
        assertTrue(NativeWorkoutHealth.matchesWindow(JSONObject(current().workout.health240Json!!), current().workout))
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
    }
}
