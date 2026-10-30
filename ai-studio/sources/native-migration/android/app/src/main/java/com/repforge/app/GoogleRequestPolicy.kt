package com.repforge.app

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

internal object GoogleRequestPolicy {
    private val EVENT_ID_REGEX = Regex("rf[0-9a-f]{64}")

    @JvmStatic
    fun networkMessage(error: Throwable?, language: String?): String? {
        var network = false
        var cause = error
        while (cause != null) {
            if (cause is UnknownHostException ||
                cause is SocketTimeoutException ||
                cause is SocketException
            ) {
                network = true
                break
            }
            if (cause.cause === cause) break
            cause = cause.cause
        }
        if (!network) return null
        val messages = arrayOf(
            "A Google nem érhető el hálózati hiba miatt. Ellenőrizd a kapcsolatot, majd próbáld újra. A helyi adatok és a ZIP-mentés továbbra is elérhetők.",
            "Google cannot be reached because of a network error. Check your connection and retry. Local data and ZIP backup remain available.",
            "Google ist wegen eines Netzwerkfehlers nicht erreichbar. Verbindung prüfen und erneut versuchen. Lokale Daten und ZIP-Sicherung bleiben verfügbar.",
            "Google nu poate fi accesat din cauza unei erori de rețea. Verifică conexiunea și reîncearcă. Datele locale și copia ZIP rămân disponibile."
        )
        val index = when (language) {
            "hu" -> 0
            "de" -> 2
            "ro" -> 3
            else -> 1
        }
        return messages[index]
    }

    @JvmStatic
    fun canRetry(method: String, url: String, eventId: String?): Boolean {
        return method == "GET" ||
            method == "PUT" ||
            method == "DELETE" ||
            (
                method == "POST" &&
                    url.startsWith("https://www.googleapis.com/calendar/v3/calendars/") &&
                    url.contains("/events?") &&
                    eventId != null &&
                    EVENT_ID_REGEX.matches(eventId)
                )
    }

    @JvmStatic
    fun transientStatus(code: Int): Boolean {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504
    }

    @JvmStatic
    fun transientStatus(code: Int, reason: String?): Boolean {
        return transientStatus(code) ||
            (code == 403 && ("rateLimitExceeded" == reason || "userRateLimitExceeded" == reason))
    }

    @JvmStatic
    fun delay(attempt: Int, retryAfter: String?, now: Long): Long {
        var delay = 1000L * (1L shl attempt)
        if (retryAfter != null) {
            delay = try {
                max(delay, retryAfter.toLong() * 1000L)
            } catch (_: NumberFormatException) {
                try {
                    val parsed = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(retryAfter)
                    if (parsed != null) max(delay, parsed.time - now) else delay
                } catch (_: Exception) {
                    delay
                }
            }
        }
        // A long server cooldown ends this attempt; do not retry before Retry-After.
        return if (delay > 15_000L) -1L else delay
    }
}
