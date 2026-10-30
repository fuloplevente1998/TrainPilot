package com.repforge

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import kotlin.math.*

/**
 * Szerepkör az edzésvezérlésben (#193 Ownership Transition).
 */
enum class PhoneOwnershipRole {
    PRIMARY,        // A telefon az elsődleges mérő és vezérlő (Primary Controller)
    MIRROR,         // Az óra vezérel, a telefon csak tükrözött kijelző (Replicated Display)
    TRANSITIONING   // Átadás/kézfogás folyamatban a telefon és az óra között
}

/**
 * GPS útvonal és pillanatnyi mérési állapot (#193/#202).
 */
data class PhoneGpsSnapshot193(
    val isTracking: Boolean = false,
    val isIndoor: Boolean = false,
    val totalDistanceMeters: Double = 0.0,
    val currentSpeedMps: Float = 0.0f,
    val currentPaceSecondsPerKm: Double = 0.0,
    val recordedPointsCount: Int = 0,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastAltitude: Double? = null,
    val lastTimestamp: Long = 0L
)

/**
 * Explicit kardió/intervallum mérés lezárási eredmény (#193/#202).
 */
data class CardioIntervalResult193(
    val workoutId: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val averagePaceSecondsPerKm: Double,
    val isIndoor: Boolean,
    val completedAt: Long
)

/**
 * A telefonos Ownership Transition és a GPS/Indoor követés koordinátora.
 */
class PhoneOwnershipAndGpsCoordinator193(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val tag = "PhoneOwnershipGps193"

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Ownership állapot
    private val _ownershipRole = MutableStateFlow(PhoneOwnershipRole.PRIMARY)
    val ownershipRole: StateFlow<PhoneOwnershipRole> = _ownershipRole.asStateFlow()

    private val _ownershipSequence = MutableStateFlow(1L)
    val ownershipSequence: StateFlow<Long> = _ownershipSequence.asStateFlow()

    // GPS / Beltéri követési állapot
    private val _gpsState = MutableStateFlow(PhoneGpsSnapshot193())
    val gpsState: StateFlow<PhoneGpsSnapshot193> = _gpsState.asStateFlow()

    private var activeWorkoutId: Long? = null
    private var lastValidLocation: Location? = null
    private val speedMovingAverageWindow = ArrayDeque<Float>(5)

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            processIncomingLocation(location)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    // --- Ownership Kezelés (#193) ---

    fun requestHandoffToWatch(reason: String = "USER_INITIATED"): JSONObject {
        val nextSeq = _ownershipSequence.value + 1L
        _ownershipSequence.value = nextSeq
        _ownershipRole.value = PhoneOwnershipRole.TRANSITIONING

        val payload = JSONObject().apply {
            put("action", "HANDOFF_REQUEST")
            put("source", "PHONE")
            put("target", "WATCH")
            put("sequence", nextSeq)
            put("timestamp", System.currentTimeMillis())
            put("reason", reason)
            put("workoutId", activeWorkoutId ?: -1L)
        }
        Log.d(tag, "Ownership átadás kezdeményezve az óra felé: $payload")
        return payload
    }

    fun acknowledgeHandoffFromWatch(sequence: Long): JSONObject {
        _ownershipSequence.value = max(_ownershipSequence.value, sequence) + 1L
        _ownershipRole.value = PhoneOwnershipRole.MIRROR
        Log.d(tag, "Óra Primary státusza nyugtázva (telefon -> MIRROR)")

        return JSONObject().apply {
            put("action", "HANDOFF_ACK")
            put("source", "PHONE")
            put("target", "WATCH")
            put("sequence", _ownershipSequence.value)
            put("status", "COMMITTED")
            put("timestamp", System.currentTimeMillis())
        }
    }

    fun reclaimOwnershipToPhone(reason: String = "WATCH_DISCONNECTED_OR_MANUAL") {
        val nextSeq = _ownershipSequence.value + 1L
        _ownershipSequence.value = nextSeq
        _ownershipRole.value = PhoneOwnershipRole.PRIMARY
        Log.i(tag, "Elsődleges vezérlés visszavéve a telefonra (PRIMARY). Ok: $reason")
    }

    // --- GPS és Beltéri Mód Követés (#193/#202) ---

    @SuppressLint("MissingPermission")
    fun startTracking(workoutId: Long, isIndoor: Boolean = false) {
        activeWorkoutId = workoutId
        lastValidLocation = null
        speedMovingAverageWindow.clear()

        _gpsState.update {
            it.copy(
                isTracking = true,
                isIndoor = isIndoor,
                totalDistanceMeters = 0.0,
                currentSpeedMps = 0.0f,
                currentPaceSecondsPerKm = 0.0,
                recordedPointsCount = 0
            )
        }

        if (isIndoor) {
            Log.d(tag, "Beltéri mód aktív: GPS hardveres követés kikapcsolva (#193/#202)")
            return
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1500L,
                    1.5f,
                    locationListener
                )
                Log.d(tag, "GPS szolgáltatás elindítva az edzéshez: $workoutId")
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Nincs meg a GPS engedély!", e)
        }
    }

    fun toggleIndoorMode() {
        val newIndoor = !_gpsState.value.isIndoor
        _gpsState.update { it.copy(isIndoor = newIndoor) }
        if (newIndoor) {
            try {
                locationManager.removeUpdates(locationListener)
                Log.d(tag, "Átváltva beltéri módra: GPS leállítva")
            } catch (e: Exception) {
                Log.w(tag, "Hiba a GPS leállításakor: ${e.message}")
            }
        } else {
            activeWorkoutId?.let { startTracking(it, isIndoor = false) }
        }
    }

    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.w(tag, "Location updates már törölve: ${e.message}")
        }
        _gpsState.update { it.copy(isTracking = false) }
    }

    /**
     * Kardió/intervallum lezárása közvetlenül, felesleges dialógusok nélkül (#193/#202).
     */
    fun completeExplicitCardioInterval(durationSeconds: Long): CardioIntervalResult193 {
        val current = _gpsState.value
        val dist = current.totalDistanceMeters
        val avgPace = if (dist > 10.0 && durationSeconds > 0) {
            (durationSeconds.toDouble() / (dist / 1000.0))
        } else {
            0.0
        }

        stopTracking()

        return CardioIntervalResult193(
            workoutId = activeWorkoutId ?: 0L,
            distanceMeters = dist,
            durationSeconds = durationSeconds,
            averagePaceSecondsPerKm = avgPace,
            isIndoor = current.isIndoor,
            completedAt = System.currentTimeMillis()
        )
    }

    private fun processIncomingLocation(location: Location) {
        if (_gpsState.value.isIndoor) return
        if (location.accuracy > 25.0f) return // Jitter / pontatlan ugrások szűrése

        val prev = lastValidLocation
        var deltaMeters = 0.0

        if (prev != null) {
            deltaMeters = calculateHaversineMeters(
                prev.latitude, prev.longitude,
                location.latitude, location.longitude
            )
            val elapsedSec = (location.time - prev.time) / 1000.0
            if (elapsedSec > 0.0) {
                val impliedSpeed = deltaMeters / elapsedSec
                if (impliedSpeed > 45.0) return // Valótlan GPS ugrás eldobása (>160 km/h)
            }
        }

        lastValidLocation = location

        if (location.hasSpeed() && location.speed > 0.3f) {
            if (speedMovingAverageWindow.size >= 5) speedMovingAverageWindow.removeFirst()
            speedMovingAverageWindow.addLast(location.speed)
        }

        val avgSpeed = if (speedMovingAverageWindow.isNotEmpty()) {
            speedMovingAverageWindow.average().toFloat()
        } else {
            location.speed
        }

        val currentPace = if (avgSpeed > 0.4f) (1000.0 / avgSpeed) else 0.0

        _gpsState.update { current ->
            current.copy(
                totalDistanceMeters = current.totalDistanceMeters + deltaMeters,
                currentSpeedMps = avgSpeed,
                currentPaceSecondsPerKm = currentPace,
                recordedPointsCount = current.recordedPointsCount + 1,
                lastLatitude = location.latitude,
                lastLongitude = location.longitude,
                lastAltitude = if (location.hasAltitude()) location.altitude else null,
                lastTimestamp = location.time
            )
        }
    }

    private fun calculateHaversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }
}
