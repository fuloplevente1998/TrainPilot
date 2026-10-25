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

    fun safe(context: Context): Boolean = try {
        if (NativeDataReplacement.blocked(context)) return false
        if (HealthJournalStore.get(context).pendingRestore().isNotEmpty()) return false
        val execution = PhoneWorkoutExecution.store(context)
        if (execution.commandsSuspended() || execution.restoreBlocked()) return false
        val ownership = PhoneOwnership193.journal(context).read()
        if (ownership.optString("stage") in ownershipTransitions) return false
        val frames = listOfNotNull(
            ActiveWorkoutStore.current(context), execution.snapshot(),
            ownership.optJSONObject("snapshot")
        )
        frames.none { frame ->
            frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
                frame.optBoolean("finalizationPending") || frame.optBoolean("discardPending")
        }
    } catch (_: Exception) {
        false // ambiguous ownership or restore state must never trigger unattended reading
    }
}
