package com.repforge.app

import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Pure eligibility contracts; provider SDK admission still needs device validation. */
class NativeHealthAutoRefreshAdmissionTest {
    private fun eligible(stage: String = "", frames: List<JSONObject?> = emptyList(),
        replacementReady: Boolean = true, executionReady: Boolean = true) =
        NativeHealthForegroundAdmission.eligible(replacementReady, executionReady, stage, frames)

    @Test fun quietForegroundCanReadWhenNoWorkoutOrRestoreIsActive() {
        assertTrue(eligible())
        assertTrue(eligible(frames = listOf(JSONObject().put("active", false))))
    }

    @Test fun activeOrPausedSessionOnEitherPhoneOrWatchBlocksAuto() {
        assertFalse(eligible(frames = listOf(JSONObject().put("active", true))))
        assertFalse(eligible(frames = listOf(JSONObject().put("workoutPaused", true))))
        assertFalse(eligible(frames = listOf(null, JSONObject().put("active", true).put("timerOwner", "watch"))))
    }

    @Test fun finalizationAndDiscardPublicationAreUnsafeEvenIfSnapshotIsNotActive() {
        assertFalse(eligible(frames = listOf(JSONObject().put("finalizationPending", true))))
        assertFalse(eligible(frames = listOf(JSONObject().put("discardPending", true))))
    }

    @Test fun allOwnershipTransitionStagesRejectUnattendedRead() {
        for (stage in listOf("requested", "offered", "incoming", "accepted", "committed", "installed")) {
            assertFalse("stage=$stage", eligible(stage = stage))
        }
    }

    @Test fun blockedReplacementOrExecutionRejectWithoutSdkCall() {
        assertFalse(eligible(replacementReady = false))
        assertFalse(eligible(executionReady = false))
    }
}
