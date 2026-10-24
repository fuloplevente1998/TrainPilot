package com.repforge.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/** Exclusive local restore/erase fence, shared with native execution and background publications. */
internal object NativeDataReplacement {
    private const val PREFS = "trainpilot_data_replacement"
    private val mutex = Mutex()
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
        try {
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
                        setBlocked(app, true)
                        execution.setRestoreBlocked(true)
                        acquired = true
                    }
                }
            }
            return operation()
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
            } finally { mutex.unlock() }
        }
    }
}
