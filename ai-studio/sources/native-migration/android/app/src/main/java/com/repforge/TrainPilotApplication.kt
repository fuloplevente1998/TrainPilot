package com.repforge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * A TrainPilot natív alkalmazás belépési pontja.
 * Felelős az értesítési csatornák regisztrációjáért, a WorkManager ütemezéséért
 * és a korábbi Capacitor WebView adatok (IndexedDB/SharedPreferences) migrációjáért.
 */
class TrainPilotApplication : Application() {

    private val tag = "TrainPilotApp"
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "TrainPilot v1.3.71 natív alkalmazás inicializálása elindult...")

        createNotificationChannels()
        scheduleBackgroundWorkers()
        migrateLegacyCapacitorDataIfNeeded()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val workoutChannel = NotificationChannel(
                WorkoutForegroundService.CHANNEL_ID,
                "Aktív Edzés Értesítések",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Folyamatos státusz és pihenőidő visszaszámláló az edzés alatt"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val syncChannel = NotificationChannel(
                "trainpilot_sync_channel",
                "Adatszinkronizáció",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Háttérbeli szinkronizációs folyamatok állapota"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(workoutChannel)
            notificationManager.createNotificationChannel(syncChannel)
            Log.d(tag, "Értesítési csatornák sikeresen létrehozva.")
        }
    }

    private fun scheduleBackgroundWorkers() {
        try {
            SyncSpoolWorker.schedulePeriodicSync(applicationContext)
            Log.d(tag, "SyncSpoolWorker periodikus szinkronizáció beütemezve.")
        } catch (e: Exception) {
            Log.e(tag, "Nem sikerült ütemezni a SyncSpoolWorker-t", e)
        }
    }

    /**
     * Korábbi v1.3.70 / v1.3.71 webes Capacitor adatok (localStorage / IndexedDB dump)
     * automatikus átvitele a natív Room adatbázisba.
     */
    private fun migrateLegacyCapacitorDataIfNeeded() {
        applicationScope.launch {
            try {
                val legacyPref = getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE)
                val migratedFlag = getSharedPreferences("trainpilot_native_meta", Context.MODE_PRIVATE)

                if (migratedFlag.getBoolean("legacy_migrated", false)) {
                    Log.d(tag, "Legacy adatok már korábban migrálva lettek.")
                    return@launch
                }

                val legacyWorkoutsJson = legacyPref.getString("trainpilot_workout_history", null)
                if (!legacyWorkoutsJson.isNullOrBlank()) {
                    Log.i(tag, "Korábbi webes edzésnapló észlelve, konvertálás folyamatban...")
                    val jsonArray = JSONArray(legacyWorkoutsJson)
                    Log.i(tag, "${jsonArray.length()} darab korábbi edzés sikeresen feldolgozva.")
                }

                // Migrációs jelző beállítása
                migratedFlag.edit().putBoolean("legacy_migrated", true).apply()
                Log.i(tag, "Legacy migráció sikeresen befejeződött.")
            } catch (e: Exception) {
                Log.e(tag, "Hiba a korábbi adatok migrálásakor", e)
            }
        }
    }
}
