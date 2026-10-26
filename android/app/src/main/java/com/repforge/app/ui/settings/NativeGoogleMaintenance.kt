package com.repforge.app.ui.settings

import android.content.Context
import com.repforge.app.NativeDataReplacement
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import kotlinx.coroutines.CancellationException

/** No credentials or remote SDK client. Authorization remains in GoogleSyncRepository. */
internal object NativeGoogleMaintenance {
    enum class Kind(val key: String) { REVOKE("revoke"), DRIVE("drive-delete"), CALENDAR("calendar-delete") }
    class Request(val kind: Kind, val sub: String, val email: String,
        val dataset: NativeDataReplacement.Continuation, val accountEpoch: Long = 0) {
        override fun toString() = "Google maintenance confirmation"
        fun requireAccount(sub: String, email: String, accountEpoch: Long = this.accountEpoch) {
            check(this.accountEpoch == accountEpoch && this.sub == sub && this.email == email && sub.isNotBlank() && email.isNotBlank()) { "Google account changed; confirm again" }
        }
    }
    data class Report(val completed: Int, val complete: Boolean, val uncertain: Boolean = false)

    suspend fun requireSafe(context: Context) {
        NativeHealthForegroundAdmission.requireMainAdmission(context)
        check(NativeHealthForegroundAdmission.safeOffMain(context)) { "Workout, ownership, backup or restore blocks maintenance" }
    }

    /** Stop on the first failure; an HTTP timeout can have succeeded remotely. Never promise rollback. */
    suspend fun deleteSequentially(ids: List<String>, guard: suspend () -> Unit,
        remove: suspend (String) -> Unit): Report {
        var done = 0
        for (id in ids.distinct()) {
            try { guard() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return Report(done, false) }
            try { remove(id); done++ }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { return Report(done, false, true) }
        }
        try { guard() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return Report(done, false) }
        return Report(done, true)
    }
}
