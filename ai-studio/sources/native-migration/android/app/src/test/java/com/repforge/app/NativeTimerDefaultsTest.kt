package com.repforge.app

import android.content.Context
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class NativeTimerDefaultsTest {
    private lateinit var context: Context
    private lateinit var store: TrainPilotSettingsStore
    private val dispatcher = StandardTestDispatcher()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    @Before fun open() = runBlocking {
        Dispatchers.setMain(dispatcher)
        context = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        NativeReplacementJournal(context).clear()
        ActiveWorkoutStore.clear(context)
        store = TrainPilotSettingsStore(context)
        store.resetAllSettingsForEraseLocal()
    }
    @After fun close() {
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        Dispatchers.resetMain()
    }
    private fun next() = JSONObject().put("work", 45).put("rest", 0).put("rounds", 6)
        .put("sound", false).put("vibration", false).put("autoAdvance", false)
    private suspend fun rejects(block: suspend () -> Unit) {
        var error: Exception? = null
        try { block() } catch (e: Exception) { error = e }
        assertNotNull("A stale or invalid edit must be rejected", error)
    }
    @Test fun savesAllSixDefaultsWithoutTouchingAnActiveExecution() = runTest(dispatcher) {
        NativeWorkoutSession.installPrepared(context, JSONObject("""{"active":true,"timerOwner":"phone",
            "workoutId":"timer-settings","started":"2026-10-26T08:00:00Z","currentExercise":0,
            "exercises":[{"id":"plank","repUnit":"mp","sets":[{"setId":"s1","done":false}]}]}"""))
        val snapshot = ActiveWorkoutStore.current(context).toString()
        val frame = PhoneWorkoutExecution.store(context).snapshot().toString()
        val before = store.current().timerConfiguration().toString()
        store.updateTimerDefaults(before, next(), NativeDataReplacement.continuationForCurrentData(context))
        assertEquals(next().toString(), TrainPilotSettingsStore(context).current().timerConfiguration().toString())
        assertEquals(snapshot, ActiveWorkoutStore.current(context).toString())
        assertEquals(frame, PhoneWorkoutExecution.store(context).snapshot().toString())
    }
    @Test fun anotherSettingsEditCannotBeOverwrittenByAnOlderDialog() = runTest(dispatcher) {
        val before = store.current().timerConfiguration().toString()
        val ticket = NativeDataReplacement.continuationForCurrentData(context)
        store.setTimerDefaults(60, 20, 3, true, true, true)
        val fresh = store.current().timerConfiguration().toString()
        rejects { store.updateTimerDefaults(before, next(), ticket) }
        assertEquals(fresh, store.current().timerConfiguration().toString())
    }
    @Test fun replacementEpochInvalidatesEvenAnIdenticalRestoredConfiguration() = runTest(dispatcher) {
        val before = store.current().timerConfiguration().toString()
        val ticket = NativeDataReplacement.continuationForCurrentData(context)
        NativeDataReplacement.exclusively(context) { store.resetAllSettingsForEraseLocal() }
        assertEquals(before, store.current().timerConfiguration().toString())
        rejects { store.updateTimerDefaults(before, next(), ticket) }
        assertEquals(before, store.current().timerConfiguration().toString())
    }
    @Test fun invalidDurationRejectsTheWholeEditIncludingSoundAndAdvance() = runTest(dispatcher) {
        val before = store.current().timerConfiguration().toString()
        val ticket = NativeDataReplacement.continuationForCurrentData(context)
        rejects { store.updateTimerDefaults(before, next().put("work", 3600).put("rest", 3600).put("rounds", 100), ticket) }
        assertEquals(before, store.current().timerConfiguration().toString())
    }
}
