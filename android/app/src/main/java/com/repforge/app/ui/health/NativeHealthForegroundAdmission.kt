package com.repforge.app.ui.health

import android.content.Context
import android.os.Looper
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthBackupGate
import com.repforge.app.NativeDataReplacement
import com.repforge.app.PhoneOwnership193
import com.repforge.app.PhoneWorkoutExecution
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Split the foreground admission by the real thread ownership of its dependencies.
 *
 * PhoneWorkoutExecution.store() is canonically main-looper-only. Calling it from an
 * SDK/IO publisher catches "Execution requires main looper" and rejects EVERY read.
 * The other conditions are checked on IO, including again under the short commit lease.
 */
internal object NativeHealthForegroundAdmission {
    private val ownershipTransitions = setOf(
        "requested", "offered", "incoming", "accepted", "committed", "installed"
    )

    internal fun eligible(
        replacementReady: Boolean,
        executionReady: Boolean,
        ownershipStage: String,
        frames: List<JSONObject?>
    ): Boolean =
        replacementReady && executionReady && ownershipStage !in ownershipTransitions &&
            frames.filterNotNull().none { frame ->
                frame.optBoolean("active") || frame.optBoolean("workoutPaused") ||
                    frame.optBoolean("finalizationPending") || frame.optBoolean("discardPending")
            }

    /** No metadata lock, provider IPC or coroutine hop in this main-only section. */
    fun safeOnMain(context: Context): Boolean {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Foreground admission requires main looper" }
        return try {
            val execution = PhoneWorkoutExecution.store(context)
            eligible(
                replacementReady = true,
                executionReady = !execution.commandsSuspended() && !execution.restoreBlocked(),
                ownershipStage = "",
                frames = listOf(execution.snapshot())
            )
        } catch (_: Exception) {
            false // Fail closed for unreadable execution state.
        }
    }

    /** Room/restore/backup/ownership checks are legal on an SDK reader's IO thread. */
    fun safeOffMain(context: Context): Boolean {
        return try {
        if (!NativeHealthBackupGate.available() || NativeDataReplacement.blocked(context)) return false
        if (HealthJournalStore.get(context).pendingRestore().isNotEmpty()) return false
        val ownership = PhoneOwnership193.journal(context).read()
        eligible(
            replacementReady = true,
            executionReady = true,
            ownershipStage = ownership.optString("stage"),
            frames = listOf(ActiveWorkoutStore.current(context), ownership.optJSONObject("snapshot"))
        )
    } catch (_: Exception) {
        false
        }
    }

    /** Outside ALL metadata locks; only the main-looper execution probe changes dispatcher. */
    suspend fun requireMainAdmission(context: Context) {
        val safe = withContext(Dispatchers.Main.immediate) { safeOnMain(context) }
        check(safe) { "Workout execution or restore blocks foreground Health refresh" }
    }

    /**
     * Actual HC/Samsung synchronous provider publication callback.
     *
     * The IO caller briefly hops to Main *before* acquiring the metadata mutex.
     * Once under the mutex, only the IO-safe ownership/restore checks are repeated,
     * and the writer is a local journal write (never SDK IPC).
     */
    fun publish(context: Context, requireIdentity: () -> Unit, writer: () -> Unit) {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Provider journal publication must run off the main thread"
        }
        runBlocking {
            requireMainAdmission(context)
            NativeDataReplacement.mutate(context) {
                NativeHealthBackupGate.publish {
                    check(safeOffMain(context)) {
                        "Workout ownership, restore or backup blocks Health publication"
                    }
                    requireIdentity()
                    writer()
                }
            }
        }
    }
}
