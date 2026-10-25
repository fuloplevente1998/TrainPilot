package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeJournalEditorTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var mapper: LegacyWebViewMigrator
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val reads = ConcurrentLinkedQueue<String>()
    private val photoId = "a1234567-1234-1234-1234-123456789abc"

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.clear(context)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java)
            .setQueryCallback({ sql, _ -> reads.add(sql) }, Executor { it.run() }).build()
        mapper = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
        runBlocking { db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises()) }
    }
    @After fun cleanup() {
        ActiveWorkoutStore.clear(context)
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        db.close()
        Dispatchers.resetMain()
    }

    private fun history(id: String = "edited-workout") = JSONObject().put("id", id).put("syncId", "sync-$id")
        .put("key", "key-$id").put("workoutId", id).put("started", "2026-10-20T10:00:00Z")
        .put("finished", "2026-10-20T10:10:00Z").put("durationSeconds", 480)
        .put("scheduleId", "schedule-keep").put("ownershipProtocol193", 1).put("ownerEpoch193", 7)
        .put("futureRoot", JSONObject().put("keep", true))
        .put("wear", JSONObject().put("calories", 42)).put("health240", JSONObject().put("steps", 100))
        .put("timeLogs162", JSONObject().put("workMs", 10_000))
        .put("activeIntervals", JSONArray().put(JSONObject().put("start", 1792490400000L).put("end", 1792491000000L)))
        .put("feedback", JSONObject().put("effort", "good").put("futureFeedback", "keep"))
        .put("photos", JSONArray().put(JSONObject().put("id", photoId).put("label", "before")
            .put("createdAt", "2026-10-20T10:00:00Z").put("updatedAt", 100L).put("driveFileId", "private-drive-id").put("futurePhoto", true)))
        .put("exercises", JSONArray().put(JSONObject().put("id", "goblet-squat").put("hu", "Guggolás")
            .put("loadType", "total").put("repUnit", "ism.").put("measurementType", "reps")
            .put("variation", "slow").put("futureExercise", true).put("sets", JSONArray().put(JSONObject()
                .put("setId", "stable-set").put("set", 1).put("weight", 10).put("reps", "8").put("done", true)
                .put("actualRir", 2).put("futureSet", JSONObject().put("keep", true))
                .put("gpsRouteKey193", "route-keep").put("gpsRouteKey163", "legacy-route-keep")))))

    private suspend fun seed(raw: JSONObject = history()): WorkoutHistoryWithDetails {
        mapper.saveWorkoutJsonToRoom(raw)
        return db.workoutHistoryDao().getWorkoutWithDetailsById(raw.getString("id"))!!
    }
    private fun changed(detail: WorkoutHistoryWithDetails): NativeJournalEditor.Draft {
        val draft = NativeJournalEditor.from(detail)
        val exercise = draft.exercises.single()
        return draft.copy(exercises = listOf(exercise.copy(sets = listOf(exercise.sets.single().copy(weight = "12,5", reps = "10")))))
    }
    private suspend fun reject(operation: suspend () -> Unit) {
        var failure: Exception? = null
        try { operation() } catch (error: Exception) { failure = error }
        assertNotNull("Expected the edit to be rejected", failure)
    }

    @Test fun saveKeepsIdentityTelemetryRoutesAndOpaqueObjectsAndRebuildsSummaries() = runTest(dispatcher) {
        val before = seed()
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), changed(before))
        val saved = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        assertEquals(before.workout.syncId, saved.workout.syncId); assertEquals(before.workout.workoutKey, saved.workout.workoutKey)
        assertEquals(before.workout.scheduleId, saved.workout.scheduleId); assertEquals(125.0, saved.workout.totalVolumeKg, 0.0)
        assertEquals(before.workout.wearMetricsJson, saved.workout.wearMetricsJson)
        assertEquals(before.workout.health240Json, saved.workout.health240Json)
        assertEquals(480, saved.workout.durationSeconds)
        assertEquals(before.photos, saved.photos)
        val row = saved.exercises.single().sets.single()
        assertEquals("stable-set", row.setId); assertEquals("route-keep", row.gpsRouteKey193); assertEquals(2, row.actualRir)
        val raw = JSONObject(saved.workout.rawJson)
        assertTrue(raw.getJSONObject("futureRoot").getBoolean("keep"))
        assertEquals("keep", raw.getJSONObject("feedback").getString("futureFeedback"))
        assertTrue(raw.getJSONArray("photos").getJSONObject(0).getBoolean("futurePhoto"))
        val ex = raw.getJSONArray("exercises").getJSONObject(0)
        assertEquals("slow", ex.getString("variation")); assertTrue(ex.getBoolean("futureExercise"))
        assertTrue(ex.getJSONArray("sets").getJSONObject(0).getJSONObject("futureSet").getBoolean("keep"))
        assertFalse(raw.optBoolean("nativeHealthWindowStale"))
    }

    @Test fun changedTimesKeepStableKeyAndMarkOriginalHealthWindowWithoutRelabellingTelemetry() = runTest(dispatcher) {
        val before = seed()
        val draft = changed(before).copy(started = "2026-10-20T11:00:00Z", finished = "2026-10-20T11:12:00Z")
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), draft)
        val saved = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!
        assertEquals(720, saved.workout.durationSeconds); assertEquals(before.workout.workoutKey, saved.workout.workoutKey)
        val raw = JSONObject(saved.workout.rawJson)
        assertTrue(raw.getBoolean("nativeHealthWindowStale"))
        assertEquals(before.workout.started, raw.getJSONObject("nativeOriginalHealthWindow").getString("started"))
        assertEquals(before.workout.activeIntervalsJson, saved.workout.activeIntervalsJson)
        assertEquals(before.workout.health240Json, saved.workout.health240Json)
    }

    @Test fun concurrentRawOrPhotoRelationEditCannotBeOverwritten() = runTest(dispatcher) {
        val before = seed()
        val opened = NativeJournalEditor.open(context, before)
        db.workoutHistoryDao().insertPhotos(listOf(before.photos.single().copy(label = "after")))
        reject { NativeJournalEditor.save(context, db, opened, changed(before)) }
        val latest = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!
        assertEquals("after", latest.photos.single().label); assertEquals(10.0, latest.exercises.single().sets.single().weight!!, 0.0)
        val openedAgain = NativeJournalEditor.open(context, latest)
        val remote = JSONObject(latest.workout.rawJson).put("futureRemote", true)
        mapper.saveWorkoutJsonToRoom(remote)
        reject { NativeJournalEditor.save(context, db, openedAgain, changed(latest)) }
        assertTrue(JSONObject(db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!.workout.rawJson).getBoolean("futureRemote"))
    }

    @Test fun deletedAndTombstonedRowsNeverReappearOnSave() = runTest(dispatcher) {
        val before = seed()
        val opened = NativeJournalEditor.open(context, before)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(before.workout.id, before.workout.workoutKey,
            JSONArray().put(before.workout.id).put(before.workout.syncId).toString(), System.currentTimeMillis()))
        reject { NativeJournalEditor.save(context, db, opened, changed(before)) }
        db.workoutHistoryDao().deleteWorkoutByIdOrKey(before.workout.id)
        reject { NativeJournalEditor.save(context, db, opened, changed(before)) }
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun invalidWindowsNumbersAndCompletedSetsLeaveOriginalUntouched() = runTest(dispatcher) {
        val before = seed()
        val opened = NativeJournalEditor.open(context, before)
        val draft = changed(before)
        val exercise = draft.exercises.single()
        val invalid = listOf(draft.copy(finished = draft.started), draft.copy(finished = "2999-10-20T11:00:00Z"),
            draft.copy(finished = "2026-10-22T11:00:00Z"), draft.copy(exercises = emptyList())) +
            listOf("NaN", "Infinity", "-1", "10001").map { value -> draft.copy(exercises = listOf(exercise.copy(
                sets = listOf(exercise.sets.single().copy(weight = value))))) } +
            listOf(draft.copy(exercises = listOf(exercise.copy(sets = listOf(exercise.sets.single().copy(reps = ""))))))
        for (value in invalid) reject { NativeJournalEditor.save(context, db, opened, value) }
        assertEquals(before, db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id))
    }

    @Test fun addRemoveAndRenumberUsesCatalogMetadataAndNewStableSetIds() = runTest(dispatcher) {
        val before = seed()
        val initial = NativeJournalEditor.from(before)
        val library = db.exerciseLibraryDao().getExerciseById("side-plank")!!
        val added = NativeJournalEditor.addExercise(initial, library)
        val newSide = added.exercises.last().copy(sets = listOf(NativeJournalEditor.Set(null, "0", "", true, left = "20", right = "25", rir = "0")))
        val invalidSide = added.copy(exercises = listOf(newSide.copy(sets = listOf(newSide.sets.single().copy(right = "")))))
        reject { NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), invalidSide) }
        db.exerciseLibraryDao().upsertExercise(library.copy(repUnit = "ism."))
        reject { NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), added.copy(exercises = listOf(newSide))) }
        assertEquals(before, db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id))
        db.exerciseLibraryDao().upsertExercise(library)
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), added.copy(exercises = listOf(newSide)))
        val saved = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!
        assertEquals("side-plank", saved.exercises.single().exercise.exerciseId)
        val set = saved.exercises.single().sets.single()
        assertEquals(1, set.setNumber); assertEquals("20", set.reps); assertEquals(0, set.actualRir)
        assertEquals(20, set.leftSeconds); assertEquals(25, set.rightSeconds)
        assertTrue(set.setId.isNotBlank() && set.setId != "stable-set")
        val add = NativeJournalEditor.from(saved).let { it.copy(exercises = listOf(it.exercises.single().copy(
            sets = it.exercises.single().sets + NativeJournalEditor.Set(null, "0", "", false)))) }
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, saved), add)
        val sets = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!.exercises.single().sets.sortedBy { it.setNumber }
        assertEquals(listOf(1, 2), sets.map { it.setNumber }); assertEquals(2, sets.map { it.setId }.toSet().size)
        assertEquals(before.photos, db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!.photos)
    }

    @Test fun sameIdentityActiveWorkoutIsRejectedButAnotherActiveWorkoutIsUnaffected() = runTest(dispatcher) {
        val active = NativeWorkoutSession.startQuickWorkout(context, "running", false, db).snapshot
        val before = seed()
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), changed(before))
        assertEquals(active.toString(), ActiveWorkoutStore.current(context)!!.toString())
        val same = seed(history(active.getString("workoutId")))
        reject { NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, same), changed(same)) }
        assertEquals(active.toString(), ActiveWorkoutStore.current(context)!!.toString())
    }

    @Test fun replacementInvalidatesAnEditorEvenWhenTheSameRowStillExists() = runTest(dispatcher) {
        val before = seed()
        val opened = NativeJournalEditor.open(context, before)
        NativeDataReplacement.exclusively(context) { /* A replacement epoch, even for identical restored data. */ }
        reject { NativeJournalEditor.save(context, db, opened, changed(before)) }
        assertEquals(before, db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id))
    }

    @Test fun editedOpaqueObjectsSurviveTheRealPortableExportAndImport() = runTest(dispatcher) {
        val before = seed()
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), changed(before))
        val exported = mapper.exportBackupJson(false)
        val raw = exported.getJSONArray("history").getJSONObject(0)
        assertTrue(raw.getJSONArray("exercises").getJSONObject(0).getBoolean("futureExercise"))
        assertTrue(raw.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
            .getJSONObject("futureSet").getBoolean("keep"))
        mapper.saveWorkoutJsonToRoom(raw)
        assertEquals(1, db.workoutHistoryDao().getWorkoutCount())
        val saved = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!
        assertEquals("route-keep", saved.exercises.single().sets.single().gpsRouteKey193)
        assertTrue(JSONObject(saved.workout.rawJson).getJSONArray("photos").getJSONObject(0).getBoolean("futurePhoto"))
    }

    @Test fun distanceEditRequiresPositiveSecondsAndMetersWithoutClearingGpsKeys() = runTest(dispatcher) {
        val raw = history()
        raw.getJSONArray("exercises").getJSONObject(0).put("id", "running").put("loadType", "bodyweight")
            .put("repUnit", "km").put("measurementType", "distance").getJSONArray("sets").getJSONObject(0)
            .put("distanceMeters", 100.0).put("reps", "60")
        val before = seed(raw)
        val opened = NativeJournalEditor.open(context, before)
        val draft = NativeJournalEditor.from(before)
        val ex = draft.exercises.single()
        val changed = draft.copy(exercises = listOf(ex.copy(sets = listOf(ex.sets.single().copy(distance = "125,5", reps = "75")))))
        reject { NativeJournalEditor.save(context, db, opened, changed.copy(exercises = listOf(ex.copy(
            sets = listOf(ex.sets.single().copy(distance = "0")))))) }
        NativeJournalEditor.save(context, db, opened, changed)
        val set = db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id)!!.exercises.single().sets.single()
        assertEquals(125.5, set.distanceMeters!!, 0.0); assertEquals("75", set.reps)
        assertEquals("route-keep", set.gpsRouteKey193)
    }

    @Test fun editingOneOfFifteenHundredWorkoutsDoesNotLoadTheOtherRawDetails() = runTest(dispatcher) {
        val before = seed()
        db.withTransaction {
            repeat(1500) { db.workoutHistoryDao().insertWorkout(JournalTestFixtures.row(it)) }
        }
        reads.clear()
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), changed(before))
        val roots = reads.filter { it.contains("SELECT", true) && it.contains("FROM workout_history", true) &&
            !it.contains("FROM workout_history_sets", true) && !it.contains("FROM workout_history_exercises", true) }
        assertTrue(roots.isNotEmpty())
        assertTrue(roots.all { it.contains("WHERE id =", true) || it.contains("LIMIT", true) })
        assertEquals(1501, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun anAliasCollisionCannotRedirectAnExactIdEditToAnotherWorkout() = runTest(dispatcher) {
        val before = seed(history("original").put("syncId", "collision"))
        val unrelated = seed(history("collision").put("photos", JSONArray()))
        NativeJournalEditor.save(context, db, NativeJournalEditor.open(context, before), changed(before))
        assertEquals(unrelated, db.workoutHistoryDao().getWorkoutWithDetailsById("collision"))
        assertEquals(12.5, db.workoutHistoryDao().getWorkoutWithDetailsById("original")!!.exercises.single().sets.single().weight!!, 0.0)
    }
}
