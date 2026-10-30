package com.repforge.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Arrays
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Both the diagnostic trial and background service share one GATT ownership gate. */
internal object BleConnectionLease {
    private var owner: Any? = null

    @JvmStatic
    @Synchronized
    fun acquire(candidate: Any): Boolean {
        if (owner != null && owner !== candidate) return false
        owner = candidate
        return true
    }

    @JvmStatic
    @Synchronized
    fun release(candidate: Any) {
        if (owner === candidate) owner = null
    }
}

internal object BleReconnectPolicy {
    @JvmStatic
    fun delay(failure: Int): Long {
        return min(300_000L, 15_000L shl max(0, min(failure, 5)))
    }
}

/** Bounded discovery: retain the selected watch even in a crowded scan. */
internal object BleScanPolicy {
    const val DURATION_MS: Int = 60_000
    const val CAPACITY: Int = 120

    class Candidate(
        @JvmField val id: String,
        @JvmField val priority: Int,
        @JvmField val rssi: Int
    )

    // null means there is room; "" means ignore; otherwise replace that row.
    @JvmStatic
    fun replacement(retained: Collection<Candidate>, incoming: Candidate): String? {
        if (retained.size < CAPACITY) return null
        var weakest: Candidate? = null
        for (item in retained) {
            if (weakest == null ||
                item.priority < weakest.priority ||
                (item.priority == weakest.priority && item.rssi < weakest.rssi)
            ) {
                weakest = item
            }
        }
        if (weakest == null || incoming.priority < weakest.priority) return ""
        if (incoming.priority == weakest.priority && incoming.rssi < weakest.rssi + 4) return ""
        return weakest.id
    }
}

/** One explicitly chosen watch. Private app storage; never part of diagnostics/backups. */
internal class BleWatchPreference(context: Context) {
    companion object {
        private const val FILE: String = "ble_selected_watch"
        private val MAC_REGEX = Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")
    }

    private val appContext: Context = context.applicationContext
    private val preferences: SharedPreferences = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun address(): String {
        val value = preferences.getString("address", "") ?: ""
        return if (MAC_REGEX.matches(value)) value else ""
    }

    fun name(): String = preferences.getString("name", "") ?: ""

    fun service(): String = preferences.getString("service", "") ?: ""

    fun journalId(): String {
        var id = preferences.getString("journal_id", "") ?: ""
        if (id.isEmpty() && address().isNotEmpty()) {
            id = UUID.randomUUID().toString()
            if (!preferences.edit().putString("journal_id", id).commit()) {
                throw IllegalStateException("Cannot save watch identity")
            }
        }
        return id
    }

    fun save(address: String, name: String, service: String): Boolean {
        val journalId = if (address.equals(address(), ignoreCase = true) && journalId().isNotEmpty()) {
            journalId()
        } else {
            UUID.randomUUID().toString()
        }
        return preferences.edit()
            .putString("journal_id", journalId)
            .putString("address", address)
            .putString("name", name)
            .putString("service", service)
            .commit()
    }

    fun clear(): Boolean {
        try {
            HealthJournalStore.get(appContext).setPreferences(JSONObject().put("bleBackground", false))
        } catch (_: Exception) {
        }
        HealthBackgroundService.stop(appContext)
        return preferences.edit().clear().commit()
    }
}

/** Bluetooth SIG Heart Rate Measurement (0x2A37), never vendor-specific payloads. */
internal object HeartRateMeasurement {
    @JvmStatic
    fun parse(value: ByteArray?): Int? {
        if (value == null || value.size < 2) return null
        val flags = value[0].toInt() and 255
        if ((flags and 0xe0) != 0) return null
        var offset = if ((flags and 1) == 0) 2 else 3
        if (value.size < offset) return null
        var bpm = value[1].toInt() and 255
        if ((flags and 1) != 0) {
            bpm = bpm or ((value[2].toInt() and 255) shl 8)
        }
        if (bpm == 0 || ((flags and 4) != 0 && (flags and 2) == 0)) return null
        if ((flags and 8) != 0) offset += 2
        if (value.size < offset) return null
        if ((flags and 16) != 0) {
            if (value.size <= offset || (value.size - offset) % 2 != 0) return null
        } else if (value.size != offset) {
            return null
        }
        return bpm
    }
}

/** Original bounded decoder for the observed RDFit MCU transport; no history/settings commands. */
class RdfitProtocol {
    companion object {
        const val BATTERY: Int = 4
        const val STEPS: Int = 10

        @JvmStatic
        fun request(command: Int): ByteArray {
            if (command != BATTERY && command != STEPS) {
                throw IllegalArgumentException("Unsupported query")
            }
            val payload = byteArrayOf(command.toByte(), 11)
            return byteArrayOf(
                0xed.toByte(),
                0x40,
                0,
                crc(payload).toByte(),
                0,
                2,
                payload[0],
                payload[1]
            )
        }

        private fun crc(bytes: ByteArray): Int {
            var value = 255
            for (b in bytes) {
                value = value xor (b.toInt() and 255)
                for (i in 0 until 8) {
                    value = if ((value and 1) != 0) (value shr 1) xor 0xb8 else value shr 1
                }
            }
            return value
        }

        private fun unsigned32(bytes: ByteArray, offset: Int): Long {
            return ((bytes[offset].toLong() and 255L) shl 24) or
                ((bytes[offset + 1].toLong() and 255L) shl 16) or
                ((bytes[offset + 2].toLong() and 255L) shl 8) or
                (bytes[offset + 3].toLong() and 255L)
        }

        private fun parse(payload: ByteArray): Reading? {
            if (payload[1].toInt() != 11) return null
            val command = payload[0].toInt() and 255
            if (command == BATTERY && payload.size == 4 && (payload[2].toInt() and 255) <= 100) {
                return Reading(command, payload[2].toInt() and 255, null, null, null)
            }
            if (command == STEPS && payload.size == 14) {
                val steps = unsigned32(payload, 2)
                val calorieTenths = unsigned32(payload, 6)
                val distance = unsigned32(payload, 10)
                if (steps <= 1_000_000L && calorieTenths <= 1_000_000L && distance <= 1_000_000L) {
                    return Reading(command, null, steps, calorieTenths / 10.0, distance)
                }
            }
            return null
        }
    }

    class Reading internal constructor(
        @JvmField val command: Int,
        @JvmField val battery: Int?,
        @JvmField val steps: Long?,
        @JvmField val calories: Double?,
        @JvmField val distance: Long?
    )

    private var pending = ByteArray(0)
    @JvmField
    var rejectedFrames: Int = 0

    fun accept(notification: ByteArray?): List<Reading> {
        val readings = ArrayList<Reading>()
        if (notification == null || notification.isEmpty()) return readings
        if (notification.size > 1024 || pending.size + notification.size > 2048) {
            pending = ByteArray(0)
            rejectedFrames++
            return readings
        }
        val joined = ByteArrayOutputStream()
        joined.write(pending, 0, pending.size)
        joined.write(notification, 0, notification.size)
        val bytes = joined.toByteArray()
        var offset = 0
        while (bytes.size - offset >= 6) {
            if ((bytes[offset].toInt() and 255) != 0xed) {
                offset++
                rejectedFrames++
                continue
            }
            val length = (bytes[offset + 4].toInt() and 255) * 256 + (bytes[offset + 5].toInt() and 255)
            if (length < 2 || length > 256) {
                offset++
                rejectedFrames++
                continue
            }
            if (bytes.size - offset < length + 6) break
            val flags = bytes[offset + 1].toInt() and 255
            val payload = Arrays.copyOfRange(bytes, offset + 6, offset + 6 + length)
            if ((flags != 0x40 && flags != 0x60) ||
                bytes[offset + 2].toInt() != 0 ||
                crc(payload) != (bytes[offset + 3].toInt() and 255)
            ) {
                rejectedFrames++
            } else {
                val reading = parse(payload)
                if (reading != null) readings.add(reading)
            }
            offset += length + 6
        }
        pending = Arrays.copyOfRange(bytes, offset, bytes.size)
        return readings
    }
}
