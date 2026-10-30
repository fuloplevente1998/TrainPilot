package com.repforge.app

import android.content.Context
import androidx.room.Room
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class NativeProgramGpsStartTest {
    private lateinit var context: Context
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val day by lazy { TrainPilotDatabase.defaultPrograms().second.first().copy(compositeId = "gps-program:outdoor",
        programId = "gps-program", dayId = "outdoor", exercisesJson = "[\"running\"]") }
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
            db.programDao().upsertProgram(TrainPilotDatabase.defaultPrograms().first.first().copy(id = "gps-program", rawJson = "{}"))
            db.programDao().upsertProgramDays(listOf(day))
        }
    }
    @After fun close() {
        db.close(); (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    private suspend fun start(record: Boolean = true) = NativeWorkoutSession.startProgramWorkout(context, "gps-program", "outdoor",
        gpsActivityId = "running", recordRoute = record, database = db)
    @Test fun readingRememberedChoiceCreatesNeitherSessionNorGps() = runTest {
        settings.setGpsStartChoice("running", true)
        val option = NativeProgramGpsChoice.read(context, "gps-program", "outdoor", db)!!
        assertEquals("running", option.activityId); assertTrue(option.remembered)
        assertNull(PhoneWorkoutExecution.store(context).snapshot()); assertNull(ActiveWorkoutStore.current(context))
    }
    @Test fun explicitProgramStartReturnsOneMemoryOnlyGpsIntentAndNoFakeCoordinates() = runTest {
        val launch = start()
        assertEquals(NativeOutdoorStart.target(launch.snapshot, "running"), launch.gpsTarget)
        assertEquals(true, settings.current().gpsStartChoices216["running"])
        assertFalse(launch.snapshot.has(NativeGpsMeasurement.FIELD))
        assertFalse(launch.snapshot.toString().contains("latitude"))
        assertEquals("gps-program", launch.snapshot.getString("programId"))
    }
    @Test fun repeatedStartReturnsCanonicalSessionWithoutReplayingGpsOrChangingConsent() = runTest {
        val first = start()
        val again = start(record = false)
        assertEquals(first.snapshot.getString("workoutId"), again.snapshot.getString("workoutId"))
        assertNull(again.gpsTarget)
        assertEquals(true, settings.current().gpsStartChoices216["running"])
    }
    @Test fun optOutIsRememberedAndDoesNotEnableAnotherActivity() = runTest {
        settings.setGpsStartChoice("running", true); settings.setGpsStartChoice("walking", true)
        assertNull(start(false).gpsTarget)
        assertEquals(mapOf("running" to false, "walking" to true), settings.current().gpsStartChoices216)
    }
    @Test fun changedFirstExerciseRejectsAnOldGpsChoiceBeforePublishingSession() = runTest {
        assertNotNull(NativeProgramGpsChoice.read(context, "gps-program", "outdoor", db))
        db.programDao().upsertProgramDays(listOf(day.copy(exercisesJson = "[\"walking\"]")))
        var rejected = false
        try { start() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertNull(PhoneWorkoutExecution.store(context).snapshot())
        assertNull(ActiveWorkoutStore.current(context))
    }
    @Test fun rememberedPreferenceIsNotAnAutomaticRecoveryOrLegacyStartCommand() = runTest {
        settings.setGpsStartChoice("running", true)
        val launch = NativeWorkoutSession.startProgramWorkout(context, "gps-program", "outdoor", database = db)
        assertNull(launch.gpsTarget)
        assertEquals(true, settings.current().gpsStartChoices216["running"])
    }
}
