package com.repforge.app.ui.health

import android.content.Context
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.PhoneOwnership193
import com.repforge.app.PhoneWorkoutExecution

/** Fail-closed foreground admission. Recheck at prepare AND at each journal write site. */
internal object NativeHealthForegroundAdmission {
    private val ownershipTransitions = setOf(
        "requested", "offered", "incoming", "accepted", "committed", "installed"
    )

    internal fun eligible(
        replacementReady: Boolean,
        executionReady: Boolean,
        ownershipStage: String,
        frames: List<org.json.JSONObject?>
    ): Boolean =
        replacementReady && executionReady && ownershipStage !in ownershipTransitions &&
            frames.filterNotNull().none { frame ->
                frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
                    frame.optBoolean("finalizationPending") || frame.optBoolean("discardPending")
            }

    fun safe(context: Context): Boolean {
        return try {
        if (NativeDataReplacement.blocked(context)) return false
        if (HealthJournalStore.get(context).pendingRestore().isNotEmpty()) return false
        val execution = PhoneWorkoutExecution.store(context)
        if (execution.commandsSuspended() || execution.restoreBlocked()) return false
        val ownership = PhoneOwnership193.journal(context).read()
        eligible(
            replacementReady = true,
            executionReady = true,
            ownershipStage = ownership.optString("stage"),
            frames = listOf(
                ActiveWorkoutStore.current(context), execution.snapshot(),
                ownership.optJSONObject("snapshot")
            )
        )
    } catch (_: Exception) {
        false // ambiguous ownership or restore state must never trigger unattended reading
        }
    }
}
