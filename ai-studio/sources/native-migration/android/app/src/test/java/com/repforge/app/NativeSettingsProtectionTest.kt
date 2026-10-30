package com.repforge.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.repforge.app.ui.settings.SettingsViewModel
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeSettingsProtectionTest {
    private lateinit var app: Application
    private lateinit var model: SettingsViewModel
    private val models = ViewModelStore()
    private val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
    private val unavailableBackup = Uri.parse("content://audit/nonexistent-backup")

    @Before fun open() {
        app = RuntimeEnvironment.getApplication()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        app.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        app.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        model = SettingsViewModel(app)
        models.put("settings", model)
    }
    @After fun close() {
        models.clear()
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
        app.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        app.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun start(owner: String = "phone", paused: Boolean = false): JSONObject {
        val candidate = JSONObject("""{"active":true,"timerOwner":"phone","workoutId":"settings-workout",
            "started":"2026-10-24T10:00:00Z","currentExercise":0,
            "exercises":[{"id":"press","repUnit":"ism.","sets":[{"setId":"one","reps":"8","done":false}]}]}""")
            .put("timerOwner", owner).put("workoutPaused", paused)
        return if (owner == "watch") ActiveWorkoutStore.saveOwned(app, candidate)
            else NativeWorkoutSession.installPrepared(app, candidate)
    }
    private fun assertBlockedWithoutChangingWorkout(operation: () -> Unit) {
        val snapshot = ActiveWorkoutStore.current(app)?.toString()
        val native = PhoneWorkoutExecution.store(app).snapshot()?.toString()
        model.clearStatusMessage()
        operation()
        assertNotNull(model.uiState.value.statusMessage)
        assertTrue(model.uiState.value.statusMessage!!.contains("aktív edzést"))
        assertFalse(model.uiState.value.isEraseLocalDialogOpen)
        assertEquals(snapshot, ActiveWorkoutStore.current(app)?.toString())
        assertEquals(native, PhoneWorkoutExecution.store(app).snapshot()?.toString())
    }
    @Test fun runningWorkoutRejectsBothRestoreFormatsBeforeReadingAnyFile() {
        start()
        assertBlockedWithoutChangingWorkout { model.onSafJsonImportCompleted(unavailableBackup) }
        assertBlockedWithoutChangingWorkout { model.onSafZipImportCompleted(unavailableBackup) }
    }
    @Test fun pausingTheWorkoutDoesNotAuthorizeDestructiveSettings() {
        start(paused = true)
        assertBlockedWithoutChangingWorkout { model.openEraseLocalDialog() }
        assertBlockedWithoutChangingWorkout { model.confirmEraseLocalData() }
    }
    @Test fun watchStartingWhileSettingsIsOpenIsCheckedAgainAtTheSafCallback() {
        // Model exists first, representing a picker opened before the remote start arrives.
        start(owner = "watch")
        assertBlockedWithoutChangingWorkout { model.onSafJsonImportCompleted(unavailableBackup) }
        assertBlockedWithoutChangingWorkout { model.onSafZipImportCompleted(unavailableBackup) }
        assertBlockedWithoutChangingWorkout { model.confirmEraseLocalData() }
    }
    @Test fun durableNativeFrameStillProtectsDataIfSnapshotPublicationIsMissing() {
        start()
        app.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().remove("snapshot").commit()
        assertBlockedWithoutChangingWorkout { model.confirmEraseLocalData() }
        assertBlockedWithoutChangingWorkout { model.onSafZipImportCompleted(unavailableBackup) }
    }
    @Test fun aWorkoutStartingAfterEraseConfirmationOpenedBlocksTheConfirmation() {
        model.openEraseLocalDialog()
        assertTrue(model.uiState.value.isEraseLocalDialogOpen)
        start()
        assertBlockedWithoutChangingWorkout { model.confirmEraseLocalData() }
    }
}
