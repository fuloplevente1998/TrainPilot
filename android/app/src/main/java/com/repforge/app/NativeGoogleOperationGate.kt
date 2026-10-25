package com.repforge.app

import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.io.IOException

/** One process-wide admission point for Google I/O and account changes, across repository instances.
 * Reject rather than queue: a queued deletion must not run after an unrelated sync/account action.
 * This is independent of the short local data-replacement lease; never hold that lease over HTTP.
 */
internal class NativeGoogleOperationGate {
    private val mutex = Mutex()

    suspend fun <T> run(operation: suspend () -> T): T {
        if (!mutex.tryLock()) throw Busy()
        try { return operation() } finally { mutex.unlock() }
    }

    fun <T> runNow(operation: () -> T): T {
        if (!mutex.tryLock()) throw Busy()
        try { return operation() } finally { mutex.unlock() }
    }

    class Busy : IllegalStateException("Már folyamatban van egy Google-művelet. Várd meg a végét.")
}

internal object NativeGoogleAccountPolicy {
    fun requireConnected(sub: String, email: String) {
        check(sub.isNotBlank() && email.isNotBlank()) { "Előbb kapcsolódj a Google-fiókodhoz." }
    }

    /** Called after userinfo verification, before accessing Drive/Calendar or persisting the profile. */
    fun requireMatchingProfile(sub: String, email: String, profile: JSONObject, connecting: Boolean) {
        if (!connecting) requireConnected(sub, email)
        val verifiedSub = profile.opt("sub") as? String
        val verifiedEmail = profile.opt("email") as? String
        if (verifiedSub.isNullOrBlank() || verifiedEmail.isNullOrBlank()) throw IOException("Hiányos Google-profil.")
        // Google's stable subject is the identity. An email rename must not change the cloud owner.
        if (sub.isNotBlank() && sub != verifiedSub) throw IOException("Eltérő Google-fiók. Előbb jelentkezz ki.")
    }
}
