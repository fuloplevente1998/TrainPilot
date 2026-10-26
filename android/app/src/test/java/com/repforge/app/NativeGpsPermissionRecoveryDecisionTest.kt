package com.repforge.app

import android.content.Context
import android.provider.Settings
import com.repforge.app.ui.workout.NativeGpsPermissionBlocker
import com.repforge.app.ui.workout.NativeGpsPermissionRecovery
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Read-only decision matrix: passing these cases does not imply phone/emulator GPS success. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeGpsPermissionRecoveryDecisionTest {
    private val recovery = NativeGpsPermissionRecovery
    private val target = NativeWorkoutTarget("workout-A", 17, "running", "set-A")

    private fun frame() = JSONObject("""{
      "active":true,"workoutId":"workout-A","generation":17,
      "timerOwner":"phone","nativeEditorProjection":1,"currentExercise":0,
      "exercises":[{"id":"running","measurementType":"distance","repUnit":"km",
        "sets":[{"setId":"set-A","done":false},{"setId":"set-B","done":false}]}]
    }""".trimIndent())

    @Test fun firstDenialIsRequestableWithoutAssumingPermanentDenial() {
        assertEquals(NativeGpsPermissionBlocker.DENIED,
            recovery.decide(false, false, false, false, false))
        assertEquals(NativeGpsPermissionBlocker.DENIED,
            recovery.decide(false, false, true, true, true))
    }

    @Test fun approximateGrantIsNotPreciseEvenWithGpsEnabled() {
        assertEquals(NativeGpsPermissionBlocker.APPROXIMATE,
            recovery.decide(false, true, true, true, false))
        assertEquals(NativeGpsPermissionBlocker.APPROXIMATE,
            recovery.decide(false, true, false, true, true))
    }

    @Test fun repeatedDeniedWithNoRationaleRequiresAppSettings() {
        assertEquals(NativeGpsPermissionBlocker.PERMANENTLY_DENIED,
            recovery.decide(false, false, true, true, false))
    }

    @Test fun finePermissionCannotOverridePhoneLocationSwitch() {
        assertEquals(NativeGpsPermissionBlocker.LOCATION_OFF,
            recovery.decide(true, true, false, true, false))
    }

    @Test fun settingsReturnWithoutGpsMustStayBlockedAndNoAutostartDecision() {
        val before = recovery.decide(true, true, false, true, false)
        assertEquals(NativeGpsPermissionBlocker.LOCATION_OFF, before)
        assertEquals(NativeGpsPermissionBlocker.READY,
            recovery.decide(true, true, true, true, false))
        // READY only reports the platform state. No service operation exists in this policy.
    }

    @Test fun missingAndReplacedWorkoutOrGenerationRejectTheOldTarget() {
        assertFalse(recovery.targetIsCurrent(null, target, false))
        assertFalse(recovery.targetIsCurrent(frame().put("workoutId", "replacement"), target, false))
        assertFalse(recovery.targetIsCurrent(frame().put("generation", 18), target, false))
    }

    @Test fun replacedDeletedOrDuplicateSetCannotBeRetargeted() {
        val missing = frame()
        missing.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").remove(0)
        assertFalse(recovery.targetIsCurrent(missing, target, false))
        val duplicate = frame()
        duplicate.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            .getJSONObject(1).put("setId", "set-A")
        assertFalse(recovery.targetIsCurrent(duplicate, target, false))
        assertFalse(recovery.targetIsCurrent(frame(), target.copy(exerciseId = "walking"), false))
    }

    @Test fun noRetryWhilePausedFinishingTransferredOrNotActive() {
        assertFalse(recovery.targetIsCurrent(frame().put("workoutPaused", true), target, false))
        assertFalse(recovery.targetIsCurrent(frame(), target, true))
        assertFalse(recovery.targetIsCurrent(frame().put("finalizationPending", true), target, false))
        assertFalse(recovery.targetIsCurrent(frame().put("timerOwner", "watch"), target, false))
        assertFalse(recovery.targetIsCurrent(frame().put("active", false), target, false))
    }

    @Test fun wrongExerciseAndCompletedSetCannotBeRestarted() {
        assertFalse(recovery.targetIsCurrent(frame().put("currentExercise", 1), target, false))
        val done = frame()
        done.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            .getJSONObject(0).put("done", true)
        assertFalse(recovery.targetIsCurrent(done, target, false))
    }

    @Test fun originalSetCanBeExplicitlyRetriedWhilePhoneStillOwnsIt() {
        assertTrue(recovery.targetIsCurrent(frame(), target, false))
    }

    @Test fun correctSettingsDestinationForTwoIndependentSwitches() {
        val context: Context = RuntimeEnvironment.getApplication()
        val location = recovery.settingsIntent(context, NativeGpsPermissionBlocker.LOCATION_OFF)!!
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, location.action)
        for (reason in listOf(NativeGpsPermissionBlocker.APPROXIMATE,
            NativeGpsPermissionBlocker.PERMANENTLY_DENIED)) {
            val intent = recovery.settingsIntent(context, reason)!!
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
            assertEquals("package:${context.packageName}", intent.data.toString())
        }
        assertNull(recovery.settingsIntent(context, NativeGpsPermissionBlocker.DENIED))
        assertNull(recovery.settingsIntent(context, NativeGpsPermissionBlocker.READY))
    }
}
