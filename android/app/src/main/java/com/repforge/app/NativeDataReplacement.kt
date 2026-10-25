package com.repforge.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Exclusive local restore/erase fence, shared with native execution and background publications. */
internal object NativeDataReplacement {
    private const val PREFS = "trainpilot_data_replacement"
    private val mutex = Mutex()
    private val replacementEpoch = AtomicLong(0)
    private class Access(val replacement: Boolean) : AbstractCoroutineContextElement(Key) {
        val active = AtomicBoolean(true)
        companion object Key : CoroutineContext.Key<Access>
    }

    /** An async continuation may record its result only if no newer restore/erase has begun.
     * The enclosing replacement increments the epoch once more on exit, before releasing admission.
     * Ordinary metadata edits are allowed: cloudBase describes the published snapshot, not current UI.
     */
    internal class Continuation internal constructor(private val expectedEpoch: Long) {
        fun requireCurrent(context: Context) {
            check(replacementEpoch.get() == expectedEpoch && !blocked(context)) {
                "Az adatok közben lecserélődtek. Indítsd újra a szinkronizálást."
            }
        }
    }

    internal suspend fun continuationAfterReplacement(): Continuation {
        val access = currentCoroutineContext()[Access]
        check(access != null && access.active.get() && access.replacement) { "Replacement lease required" }
        return Continuation(replacementEpoch.get() + 1)
    }

    /** For consent/network work started on the current dataset, without holding a local write lease. */
    internal fun continuationForCurrentData(context: Context): Continuation {
        val epoch = replacementEpoch.get()
        check(!blocked(context)) { "Adatcsere van folyamatban." }
        return Continuation(epoch).also { it.requireCurrent(context) }
    }

    /** Serialize admitted metadata writers with replacement, including callbacks already in progress. */
    internal suspend fun <T> mutate(context: Context, operation: suspend () -> T): T {
        val inherited = currentCoroutineContext()[Access]
        if (inherited?.active?.get() == true) {
            check(inherited.replacement || !blocked(context)) { "Adatcsere van folyamatban." }
            return operation()
        }
        val ticket = replacementEpoch.get()
        check(!blocked(context)) { "Adatcsere van folyamatban." }
        return mutex.withLock {
            check(ticket == replacementEpoch.get() && !blocked(context)) { "Az adatok közben lecserélődtek; a műveletet újra kell kezdeni." }
            val access = Access(false)
            try { withContext(access) { operation() } } finally { access.active.set(false) }
        }
    }

    fun blocked(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("blocked", false)
    private fun setBlocked(context: Context, value: Boolean) {
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("blocked", value).commit()) {
            "Az adatcsere védelme nem menthető."
        }
    }
    suspend fun <T> exclusively(context: Context, operation: suspend () -> T): T = run(context, false, operation)
    internal suspend fun <T> recover(context: Context, operation: suspend () -> T): T = run(context, true, operation)
    private suspend fun <T> run(context: Context, recovering: Boolean, operation: suspend () -> T): T {
        check(mutex.tryLock()) { "Már folyamatban van egy adat-visszaállítás vagy törlés." }
        val app = context.applicationContext
        var acquired = false
        var versionChanged = false
        try {
            NativeCloudPhotoDeletion.requireNoPendingDeletes()
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                // Same order as ownership publication: journal -> snapshot -> native execution.
                synchronized(PhoneOwnership193.journal(app)) {
                    synchronized(ActiveWorkoutStore) {
                        val execution = PhoneWorkoutExecution.store(app)
                        check(!execution.commandsSuspended() && (recovering || (!blocked(app) && !execution.restoreBlocked()))) {
                            "Az adatcsere védelme aktív. Megszakadt visszaállítás esetén helyreállítás szükséges."
                        }
                        val ownership = PhoneOwnership193.journal(app).read()
                        check(ActiveWorkoutStore.current(app)?.optBoolean("active") != true && execution.snapshot()?.optBoolean("active") != true &&
                            ownership.optJSONObject("snapshot")?.optBoolean("active", true) != true &&
                            ownership.optString("stage") !in setOf("requested", "offered", "incoming", "accepted", "committed", "installed")) {
                            "Az adatok cseréje előtt fejezd be vagy vesd el az aktív edzést."
                        }
                        // Persist publication fence first. A failure installing the native fence stays fail-closed.
                        replacementEpoch.incrementAndGet()
                        versionChanged = true
                        setBlocked(app, true)
                        execution.setRestoreBlocked(true)
                        acquired = true
                    }
                }
            }
            val access = Access(true)
            return try { withContext(access) { operation() } } finally { access.active.set(false) }
        } finally {
            try {
                // Incomplete redo receipt keeps both fences installed, including after process death.
                if (acquired && !NativeReplacementJournal(app).exists()) withContext(NonCancellable + Dispatchers.Main.immediate) {
                    synchronized(PhoneOwnership193.journal(app)) {
                        synchronized(ActiveWorkoutStore) {
                            PhoneWorkoutExecution.store(app).setRestoreBlocked(false)
                            setBlocked(app, false)
                        }
                    }
                }
            } finally {
                if (versionChanged) replacementEpoch.incrementAndGet()
                mutex.unlock()
            }
        }
    }
}
