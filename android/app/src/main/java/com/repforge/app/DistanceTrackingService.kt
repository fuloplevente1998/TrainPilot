package com.repforge.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

/** User-started location service; route points stay in bounded RAM and private .route files. */
class DistanceTrackingService : Service(), LocationListener {
    companion object {
        const val PREFS: String = "trainpilot-distance"
        const val START: String = "distance.START"
        const val STOP: String = "distance.STOP"
        private const val CHANNEL: String = "trainpilot_distance"
        private const val NOTIFICATION: Int = 106

        @Volatile
        private var instance: DistanceTrackingService? = null

        private val _gpsStateFlow = MutableStateFlow(JSONObject())
        val gpsStateFlow: StateFlow<JSONObject> = _gpsStateFlow.asStateFlow()

        @JvmStatic
        fun text(lang: String?, hu: String, en: String, de: String, ro: String): String {
            return when (lang) {
                "hu" -> hu
                "de" -> de
                "ro" -> ro
                else -> en
            }
        }

        @JvmStatic
        fun snapshot(context: Context): JSONObject {
            val service = instance
            if (service != null && service.active) {
                return service.current()
            }
            return try {
                val out = JSONObject(context.getSharedPreferences(PREFS, MODE_PRIVATE).getString("state", "{}") ?: "{}")
                out.put("active", false)
                _gpsStateFlow.value = out
                out
            } catch (_: Exception) {
                JSONObject()
            }
        }

        @JvmStatic
        fun stop(context: Context): JSONObject {
            val service = instance
            if (service != null) {
                service.stopMeasurement()
                service.stopSelf()
            }
            return snapshot(context)
        }

        @JvmStatic
        fun stopOwned(context: Context, expected: String): JSONObject? {
            val s = instance
            if (s != null && s.active && expected.isNotEmpty() && expected == s.key) {
                return stop(context)
            }
            val previous = snapshot(context)
            return if (expected.isNotEmpty() && expected == previous.optString("key")) previous else null
        }

        @JvmStatic
        fun pauseOwned(context: Context, expected: String, pause: Boolean) {
            val s = instance
            if (s == null || !s.active || expected.isEmpty() || expected != s.key || s.paused == pause) return
            if (pause) {
                s.accumulated += max(0L, SystemClock.elapsedRealtime() - s.began)
            } else {
                s.began = SystemClock.elapsedRealtime()
            }
            s.paused = pause
            s.capture.breakSegment()
            s.save()
            s.notifyStatus()
        }

        /** Keep the existing location FGS; never start a new location service from background. */
        @JvmStatic
        fun rollover(context: Context, expected: String, next: String, name: String): JSONObject? {
            val s = instance
            if (s == null || !s.active || expected != s.key) {
                return stopOwned(context, expected)
            }
            val previous = s.current()
            s.key = next
            s.name = name
            s.capture = GpsRouteCapture()
            s.prepareRouteOwner(next)
            s.accumulated = 0L
            s.began = SystemClock.elapsedRealtime()
            s.lastFix = 0L
            s.accuracy = 0f
            s.save()
            s.notifyStatus()
            return previous
        }

        @JvmStatic
        fun erase(context: Context) {
            stop(context)
            val service = instance
            if (service != null) {
                service.key = ""
                service.error = ""
                service.capture = GpsRouteCapture()
                service.routeSavedPoints = 0
                service.routePriorPointCount = 0
                service.routeSegmentOffset = 0
                service.routeSaveError = false
            }
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply()
            _gpsStateFlow.value = JSONObject()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var capture = GpsRouteCapture()
    private lateinit var routeStore: GpsRouteStore
    private var routeSegmentOffset: Int = 0
    private var routeSavedPoints: Int = 0
    private var routePriorPointCount: Int = 0
    private var routeSaveError: Boolean = false
    private var locations: LocationManager? = null

    @Volatile
    private var active: Boolean = false
    private var paused: Boolean = false
    private var accumulated: Long = 0L
    private var key: String = ""
    private var name: String = ""
    private var language: String = "en"
    private var error: String = ""
    private var began: Long = 0L
    private var lastFix: Long = 0L
    private var accuracy: Float = 0f

    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            save()
            notifyStatus()
            handler.postDelayed(this, 5000L)
        }
    }

    private fun prepareRouteOwner(ownerKey: String) {
        routeSavedPoints = 0
        routePriorPointCount = 0
        routeSegmentOffset = 0
        routeSaveError = false
        try {
            val old = routeStore.read(ownerKey)
            routeSegmentOffset = old.lastOrdinal()
            routePriorPointCount = old.pointCount
            routeSaveError = old.partial || old.pointCount >= GpsRouteTrace.MAX_POINTS
        } catch (_: IOException) {
            routeSaveError = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        locations = getSystemService(LOCATION_SERVICE) as? LocationManager
        routeStore = GpsRouteStore(noBackupFilesDir)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (STOP == intent.action) {
            stopMeasurement()
            stopSelf()
            return START_NOT_STICKY
        }
        if (active) return START_NOT_STICKY
        val reqKey = intent.getStringExtra("key")
        val reqLang = intent.getStringExtra("language")
        if (reqKey == null || reqLang == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        key = reqKey
        name = intent.getStringExtra("name") ?: "TrainPilot"
        language = reqLang
        capture = GpsRouteCapture()
        prepareRouteOwner(key)
        paused = false
        accumulated = 0L
        lastFix = 0L
        accuracy = 0f
        error = ""
        began = SystemClock.elapsedRealtime()
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("location")
            }
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(CHANNEL, "TrainPilot GPS", NotificationManager.IMPORTANCE_LOW)
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
            active = true
            startForeground(NOTIFICATION, notification())
            locations?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 3f, this, Looper.getMainLooper())
            save()
            handler.post(tick)
        } catch (_: Exception) {
            error = text(
                language,
                "A GPS-mérés nem indult el. Ellenőrizd a pontos helyengedélyt és a GPS-t.",
                "GPS tracking could not start. Check precise location permission and GPS.",
                "GPS-Messung konnte nicht starten. Genauen Standortzugriff und GPS prüfen.",
                "Măsurarea GPS nu a pornit. Verifică permisiunea de localizare precisă și GPS."
            )
            stopMeasurement()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        if (!active || paused) return
        val stamp = location.elapsedRealtimeNanos / 1_000_000L
        val now = SystemClock.elapsedRealtime()
        if (!location.hasAccuracy() || now - stamp > 15_000L || stamp > now + 1000L) return
        val priorPoints = capture.routePointCount()
        if (capture.addFix(
                location.latitude,
                location.longitude,
                location.accuracy.toDouble(),
                stamp,
                location.time,
                if (location.hasSpeed()) location.speed.toDouble() else Double.NaN
            )
        ) {
            if (capture.routePointCount() > priorPoints && !routeSaveError) {
                if (routeSavedPoints + routePriorPointCount >= GpsRouteTrace.MAX_POINTS) {
                    routeSaveError = true
                } else {
                    try {
                        routeStore.append(key, routeSegmentOffset + capture.routeSegmentCount(), capture.route().lastPoint())
                        routeSavedPoints++
                    } catch (_: IOException) {
                        routeSaveError = true
                    }
                }
            }
            lastFix = stamp
            accuracy = location.accuracy
            save()
        }
    }

    override fun onProviderDisabled(provider: String) {
        lastFix = 0L
        capture.breakSegment()
        save()
    }

    override fun onProviderEnabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun current(): JSONObject {
        val out = JSONObject()
        try {
            out.put("key", key)
            out.put("active", active)
            out.put("paused", paused)
            out.put("distanceMeters", (capture.totalMeters() * 10.0).roundToLong() / 10.0)
            out.put(
                "elapsedSeconds",
                max(0L, (accumulated + if (paused || !active) 0L else SystemClock.elapsedRealtime() - began) / 1000L)
            )
            out.put("hasFix", lastFix > 0L && SystemClock.elapsedRealtime() - lastFix < 30_000L)
            out.put("accuracyMeters", accuracy.toDouble())
            out.put("routePointCount", capture.routePointCount())
            out.put("routeSegmentCount", capture.routeSegmentCount())
            out.put("routeTruncated", capture.routeTruncated())
            out.put("routeSavedPoints", routeSavedPoints)
            out.put("routeSaveError", routeSaveError)
            out.put("error", error)
        } catch (_: Exception) {
        }
        return out
    }

    private fun save() {
        if (key.isEmpty()) return
        val stateObj = current()
        _gpsStateFlow.value = stateObj
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("state", stateObj.toString()).apply()
    }

    private fun notification(): Notification {
        val open = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = PendingIntent.getActivity(
            this,
            0,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, DistanceTrackingService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = text(
            language,
            "GPS-távolságmérés",
            "GPS distance tracking",
            "GPS-Distanzmessung",
            "Măsurare distanță GPS"
        )
        val body = String.format(
            Locale.getDefault(),
            "%.2f km · %s",
            capture.totalMeters() / 1000.0,
            if (name.isEmpty()) "TrainPilot" else name
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, text(language, "Leállítás", "Stop", "Stoppen", "Oprește"), stop)
            .build()
    }

    private fun notifyStatus() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
    }

    private fun stopMeasurement() {
        if (active) {
            capture.breakSegment()
            if (!paused) accumulated += max(0L, SystemClock.elapsedRealtime() - began)
            active = false
            paused = false
            try {
                locations?.removeUpdates(this)
            } catch (_: Exception) {
            }
            handler.removeCallbacks(tick)
            save()
        } else if (error.isNotEmpty()) {
            save()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopMeasurement()
        stopSelf()
    }

    override fun onDestroy() {
        stopMeasurement()
        if (instance === this) instance = null
        super.onDestroy()
    }
}
