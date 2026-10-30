package com.repforge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Natív Foreground Service az edzés háttérbeli folyamatos időzítéséhez,
 * GPS követéséhez és értesítési sávos vezérléséhez.
 */
class WorkoutForegroundService : Service() {

    private val tag = "WorkoutFgService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "trainpilot_workout_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_SKIP_REST = "ACTION_SKIP_REST"
        const val ACTION_ADD_REST_30 = "ACTION_ADD_REST_30"
        const val ACTION_FINISH = "ACTION_FINISH"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(tag, "Foreground Service parancs beérkezett: $action")

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification("Edzés folyamatban...", 0L, false, 0)
            }
            ACTION_PAUSE -> {
                Log.i(tag, "Értesítésről vezérelve: Szünet")
            }
            ACTION_RESUME -> {
                Log.i(tag, "Értesítésről vezérelve: Folytatás")
            }
            ACTION_SKIP_REST -> {
                Log.i(tag, "Értesítésről vezérelve: Pihenőidő kihagyása")
            }
            ACTION_ADD_REST_30 -> {
                Log.i(tag, "Értesítésről vezérelve: +30 mp hozzáadva")
            }
            ACTION_FINISH -> {
                Log.i(tag, "Értesítésről vezérelve: Edzés befejezése")
                stopSelf()
            }
            ACTION_CANCEL -> {
                Log.i(tag, "Értesítésről vezérelve: Megszakítás")
                stopSelf()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    fun updateProgressNotification(
        workoutTitle: String,
        elapsedSeconds: Long,
        isRestActive: Boolean,
        restSecondsRemaining: Int
    ) {
        val notification = buildNotification(workoutTitle, elapsedSeconds, isRestActive, restSecondsRemaining)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startForegroundWithNotification(
        title: String,
        elapsedSeconds: Long,
        isRestActive: Boolean,
        restSecondsRemaining: Int
    ) {
        val notification = buildNotification(title, elapsedSeconds, isRestActive, restSecondsRemaining)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        title: String,
        elapsedSeconds: Long,
        isRestActive: Boolean,
        restSecondsRemaining: Int
    ): Notification {
        val contentText = if (isRestActive) {
            val rMin = restSecondsRemaining / 60
            val rSec = restSecondsRemaining % 60
            "Pihenő: %02d:%02d (Összesen: %02d:%02d)".format(rMin, rSec, elapsedSeconds / 60, elapsedSeconds % 60)
        } else {
            "Időtartam: %02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
        }

        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isRestActive) {
            val skipIntent = PendingIntent.getService(
                this, 1,
                Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_SKIP_REST },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val add30Intent = PendingIntent.getService(
                this, 2,
                Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_ADD_REST_30 },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_next, "Kihagyás", skipIntent)
            builder.addAction(android.R.drawable.ic_input_add, "+30 mp", add30Intent)
        } else {
            val pauseIntent = PendingIntent.getService(
                this, 3,
                Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val finishIntent = PendingIntent.getService(
                this, 4,
                Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_FINISH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Szünet", pauseIntent)
            builder.addAction(android.R.drawable.checkbox_on_background, "Kész", finishIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aktív Edzés Követés",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Folyamatos visszajelzés az aktív edzésről és pihenőidőről"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrainPilot::WorkoutWakeLock").apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L) // 4 órás biztonsági limit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        Log.i(tag, "WorkoutForegroundService leállítva.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
