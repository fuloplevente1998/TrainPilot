package com.repforge.app

import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** ViewModel-owned, process-local consent. No token/result is saved to disk.
 * Rotation keeps the same prompt; a recreated process has no pending attempt to accept a stale result.
 */
internal class NativeGoogleConsentBridge {
    data class Prompt(val id: String, val pendingIntent: PendingIntent)
    private class Attempt(val prompt: Prompt) {
        val result = CompletableDeferred<Intent?>()
        var launched = false
    }
    private val lock = Any()
    private var attempt: Attempt? = null
    private val _prompt = MutableStateFlow<Prompt?>(null)
    val prompt = _prompt.asStateFlow()

    suspend fun resolve(pendingIntent: PendingIntent): Intent? {
        val current = synchronized(lock) {
            check(attempt == null) { "Google consent already pending" }
            Attempt(Prompt(UUID.randomUUID().toString(), pendingIntent)).also {
                attempt = it; _prompt.value = it.prompt
            }
        }
        try { return current.result.await() }
        finally {
            synchronized(lock) {
                if (attempt === current) { attempt = null; _prompt.value = null }
                current.result.cancel()
            }
        }
    }

    /** Keep the prompt published until result/cancellation, so rotation can re-register its launcher. */
    fun takeForLaunch(id: String): Prompt? = synchronized(lock) {
        val current = attempt ?: return@synchronized null
        if (current.prompt.id != id || current.launched) return@synchronized null
        current.launched = true
        current.prompt
    }

    fun complete(id: String, result: Intent?): Boolean = synchronized(lock) {
        val current = attempt ?: return@synchronized false
        if (current.prompt.id != id || !current.launched) return@synchronized false
        current.result.complete(result)
    }

    fun fail(id: String, error: Exception): Boolean = synchronized(lock) {
        val current = attempt ?: return@synchronized false
        if (current.prompt.id != id || !current.launched) return@synchronized false
        current.result.completeExceptionally(error)
    }
}
