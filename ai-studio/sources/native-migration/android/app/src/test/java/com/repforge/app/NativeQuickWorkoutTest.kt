package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeQuickWorkoutTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }

    @Before fun open() {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        runBlocking {
            settings.resetAllSettingsForEraseLocal()
            db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
        }
    }

    @After fun close() {
        db.close()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }

    private suspend fun start(id: String = "running", gps: Boolean = false) = NativeWorkoutSession.startQuickWorkout(context, id, gps, db)
    private fun frame() = PhoneWorkoutExecution.store(context).snapshot()!!
    private fun exercise(frame: JSONObject = frame()) = frame.getJSONArray("exercises").getJSONObject(0)
    private fun set(frame: JSONObject = frame()) = exercise(frame).getJSONArray("sets").getJSONObject(0)
    private suspend fun rejected(block: suspend () -> Unit) {
        var failure: Exception? = null
        try { block() } catch (error: Exception) { failure = error }
        assertNotNull("The invalid start must fail", failure)
    }

    @Test fun editedProgramTargetsReachTheActualNativeSessionAndStayIncompleteUntilRecorded() = runTest {
        val source = db.exerciseLibraryDao().getExerciseById("goblet-squat")!!
        var draft = NativeProgramEditor.addDay(NativeProgramEditor.Draft("Rx plan", "", emptyList()), "A")
        draft = draft.copy(days = draft.days.map { NativeProgramEditor.addExercise(it, source.id) })
        val id = NativeProgramEditor.save(context, db, null, draft, "hu")
        val opened = db.programDao().getProgramWithDaysById(id)!!
        val patch = NativeProgramPrescription.edit(NativeProgramPrescription.Fields("4", "6–8", "22.5", "75"), source.loadType)
        val edited = NativeProgramEditor.from(opened, "hu").copy(prescriptionEdits = mapOf(source.id to
            NativeProgramPrescription.Edit(patch.toString(), NativeProgramPrescription.source(source))))
        NativeProgramEditor.save(context, db, opened, edited, "hu")
        val session = NativeWorkoutSession.startProgramWorkout(context, id, opened.days.single().dayId, database = db).snapshot
        val exercise = session.getJSONArray("exercises").getJSONObject(0)
        assertEquals("6–8", exercise.getString("targetReps"))
        assertEquals(75, exercise.getInt("restSeconds162"))
        assertEquals(4, exercise.getJSONArray("sets").length())
        assertEquals(22.5, exercise.getJSONObject("prescription").getDouble("weight"), 0.0)
        for (index in 0 until exercise.getJSONArray("sets").length()) {
            val set = exercise.getJSONArray("sets").getJSONObject(index)
            assertEquals(22.5, set.getDouble("weight"), 0.0)
            assertFalse(set.optBoolean("done"))
        }
        assertEquals(0, db.workoutHistoryDao().getWorkoutCount())
    }

    @Test fun quickStartNeedsNoProgramAndDurablySharesItsIdsWithTheEditor() = runTest {
        assertTrue(db.programDao().getProgramsWithDays().isEmpty())
        val result = start()
        val saved = result.snapshot
        assertTrue(saved.getBoolean("quickWorkout")); assertEquals("quick", saved.getString("type"))
        assertTrue(saved.isNull("programId")); assertTrue(saved.isNull("dayId"))
        assertEquals(saved.getString("workoutId"), frame().getString("workoutId"))
        assertEquals(saved.getLong("generation"), frame().getLong("generation"))
        assertEquals(set(saved).getString("setId"), set().getString("setId"))
        assertEquals("", set().getString("reps")); assertFalse(set().getBoolean("done"))
        assertFalse(set().has("distanceMeters")); assertNull(result.gpsTarget)
    }

    @Test fun optedInOutdoorStartReturnsOneEphemeralTargetAndRemembersTheActivity() = runTest {
        val result = start(gps = true)
        assertEquals(NativeOutdoorStart.target(frame(), "running"), result.gpsTarget)
        assertEquals(true, settings.current().gpsStartChoices216["running"])
        // Creating the workout never fabricates a binding, measured distance or coordinates.
        assertFalse(frame().has(NativeGpsMeasurement.FIELD)); assertFalse(set().has("distanceMeters"))
        assertFalse(frame().toString().contains("latitude"))
    }

    @Test fun repeatedStartResumesTheSameWorkoutWithoutGpsOrConsentChange() = runTest {
        val first = start(gps = true)
        val again = start("walking", false)
        assertEquals(first.snapshot.getString("workoutId"), again.snapshot.getString("workoutId"))
        assertNull(again.gpsTarget)
        assertEquals(mapOf("running" to true), settings.current().gpsStartChoices216)
    }

    @Test fun explicitOptOutIsRememberedWithoutChangingAnotherActivity() = runTest {
        settings.setGpsStartChoice("running", true); settings.setGpsStartChoice("cycling", true)
        assertNull(start(gps = false).gpsTarget)
        assertEquals(mapOf("running" to false, "cycling" to true), settings.current().gpsStartChoices216)
    }

    @Test fun preferenceIsPerActivityAndDoesNotOptInOtherOutdoorExercises() = runTest {
        settings.setGpsStartChoice("running", true)
        assertNull(start("walking").gpsTarget)
        assertEquals(mapOf("running" to true, "walking" to false), settings.current().gpsStartChoices216)
    }

    @Test fun strengthQuickStartHasBlankActualValuesAndCannotOptIntoGps() = runTest {
        val strength = db.exerciseLibraryDao().getAllExercises().first { it.measurementType == "reps" }
        rejected { start(strength.id, true) }
        assertNull(PhoneWorkoutExecution.store(context).snapshot())
        val result = start(strength.id)
        assertEquals(strength.id, exercise().getString("id")); assertEquals("", set().getString("reps"))
        assertFalse(set().getBoolean("done")); assertFalse(set().has("actualRir")); assertNull(result.gpsTarget)
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun importedDraftIsUnchangedAndBlocksNewQuickWorkout() = runTest {
        val original = """{"session":{"id":"old","exercises":[{"id":"running"}]},"savedAt":"2026-10-22T10:00:00Z"}"""
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("draft", original, 7L))
        rejected { start(gps = true) }
        assertEquals(original, db.keyValueDocumentDao().getDocument("draft")!!.json)
        assertNull(PhoneWorkoutExecution.store(context).snapshot()); assertNull(ActiveWorkoutStore.current(context))
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun missingExerciseCannotCreateAnEmptyWorkoutOrSaveConsent() = runTest {
        rejected { start("missing", true) }
        assertNull(PhoneWorkoutExecution.store(context).snapshot()); assertNull(ActiveWorkoutStore.current(context))
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun restoreFenceLeavesStartAndPreferenceUntouched() = runTest {
        PhoneWorkoutExecution.store(context).setRestoreBlocked(true)
        rejected { start(gps = true) }
        assertNull(PhoneWorkoutExecution.store(context).snapshot()); assertNull(ActiveWorkoutStore.current(context))
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun executionStorageFailureDoesNotPublishAndRetryCreatesOneWorkout() = runTest {
        val store = PhoneWorkoutExecution.store(context)
        store.writableDatabase.execSQL("CREATE TRIGGER start_disk_full BEFORE INSERT ON execution BEGIN SELECT RAISE(ABORT,'synthetic disk failure'); END")
        rejected { start(gps = true) }
        assertNull(store.snapshot()); assertNull(ActiveWorkoutStore.current(context))
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
        store.writableDatabase.execSQL("DROP TRIGGER start_disk_full")
        val retry = start(gps = true)
        assertNotNull(retry.gpsTarget)
        assertEquals(retry.snapshot.getString("workoutId"), start().snapshot.getString("workoutId"))
    }

    @Test fun watchOwnedMirrorWinsWithoutLocalExecutionOrGpsIntent() = runTest {
        val mirror = NativeWorkoutSession.quickCandidate(JSONObject().put("id", "running"), TrainPilotUserSettings())
            .put("timerOwner", "watch").put("generation", 10)
        ActiveWorkoutStore.saveOwned(context, mirror)
        val result = start(gps = true)
        assertEquals(mirror.getString("workoutId"), result.snapshot.getString("workoutId")); assertNull(result.gpsTarget)
        assertNull(PhoneWorkoutExecution.store(context).snapshot()); assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun recoveredNativeSessionNeverReplaysAnOldGpsStartIntent() = runTest {
        val ex = JSONObject().put("id", "running").put("measurementType", "distance").put("repUnit", "km")
            .put("sets", NativeWorkoutStartValues.blankSets(1, 0.0))
        val source = NativeWorkoutSession.quickCandidate(ex, TrainPilotUserSettings())
            .put("generation", 42).put("executionRevision", 0).put("nativeEditorProjection", 1)
        PhoneWorkoutExecution.store(context).prepare(source, 0)
        val result = start(gps = true)
        assertEquals(source.getString("workoutId"), result.snapshot.getString("workoutId")); assertNull(result.gpsTarget)
        assertTrue(settings.current().gpsStartChoices216.isEmpty())
    }

    @Test fun outdoorAllowlistRejectsIndoorAndCustomDistanceActivities() {
        for (id in listOf("running", "walking", "jogging", "cycling", "hiking", "inline-skating"))
            assertTrue(NativeOutdoorStart.eligible(id, "distance", "km"))
        for (id in listOf("treadmill", "rower", "custom-running", "plank"))
            assertFalse(NativeOutdoorStart.eligible(id, "distance", "km"))
        assertFalse(NativeOutdoorStart.eligible("running", "time", "mp"))
    }

    @Test fun launchTargetRejectsPauseOwnerFinalizationAndChangedSelection() = runTest {
        val saved = start().snapshot
        for (change in listOf<Pair<String, Any>>("workoutPaused" to true, "timerOwner" to "watch", "finalizationPending" to true)) {
            val altered = JSONObject(saved.toString()).put(change.first, change.second)
            assertThrows(IllegalStateException::class.java) { NativeOutdoorStart.target(altered, "running") }
        }
        assertThrows(IllegalStateException::class.java) { NativeOutdoorStart.target(saved, "walking") }
        val changed = JSONObject(saved.toString()); set(changed).put("done", true)
        assertThrows(IllegalStateException::class.java) { NativeOutdoorStart.target(changed, "running") }
    }

    @Test fun quickCandidateCopiesExerciseAndUsesUniqueSetAndWorkoutIds() {
        val ex = JSONObject().put("id", "running").put("sets", NativeWorkoutStartValues.blankSets(2, 0.0))
        val original = ex.toString()
        val a = NativeWorkoutSession.quickCandidate(ex, TrainPilotUserSettings(language = "de"))
        val b = NativeWorkoutSession.quickCandidate(ex, TrainPilotUserSettings())
        exercise(a).put("name", "changed")
        assertEquals(original, ex.toString()); assertNotEquals(a.getString("workoutId"), b.getString("workoutId"))
        assertEquals("Schnelltraining", a.getString("dayName"))
        val sets = ex.getJSONArray("sets")
        assertNotEquals(sets.getJSONObject(0).getString("setId"), sets.getJSONObject(1).getString("setId"))
    }
}
