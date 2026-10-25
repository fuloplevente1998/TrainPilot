package com.repforge.app.ui.health

import android.Manifest
import android.app.job.JobScheduler
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.repforge.app.BleWatchPreference
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthBackgroundService
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement

enum class BackgroundBlock {
    READY, RESTORE, UNSUPPORTED, READ_PERMISSION, BACKGROUND_PERMISSION,
    WATCH_NOT_READY, BLUETOOTH_PERMISSION, NOTIFICATION_PERMISSION, BLUETOOTH_OFF
}

/** A saved opt-in is not proof that Android has granted access or started a background job. */
internal object NativeHealthBackgroundPolicy {
    fun health(supported: Boolean, read: Boolean, background: Boolean, restoreReady: Boolean): BackgroundBlock =
        when {
            !restoreReady -> BackgroundBlock.RESTORE
            !supported -> BackgroundBlock.UNSUPPORTED
            !read -> BackgroundBlock.READ_PERMISSION
            !background -> BackgroundBlock.BACKGROUND_PERMISSION
            else -> BackgroundBlock.READY
        }

    fun ble(watchReady: Boolean, connect: Boolean, notifications: Boolean,
            bluetoothOn: Boolean, restoreReady: Boolean): BackgroundBlock =
        when {
            !restoreReady -> BackgroundBlock.RESTORE
            !watchReady -> BackgroundBlock.WATCH_NOT_READY
            !connect -> BackgroundBlock.BLUETOOTH_PERMISSION
            !notifications -> BackgroundBlock.NOTIFICATION_PERMISSION
            !bluetoothOn -> BackgroundBlock.BLUETOOTH_OFF
            else -> BackgroundBlock.READY
        }

    data class Snapshot(
        val health: BackgroundBlock,
        val ble: BackgroundBlock,
        val healthScheduled: Boolean,
        val bleRunning: Boolean
    )

    fun snapshot(context: Context, store: HealthJournalStore): Snapshot {
        val restoreReady = !NativeDataReplacement.blocked(context) && store.pendingRestore().isEmpty()
        val supported = HealthBackgroundJob.supported(context)
        val read = HealthConnectRepository.anyReadAllowed(context)
        val granted = HealthBackgroundJob.permitted(context)
        val watch = BleWatchPreference(context)
        val readyWatch = watch.address().isNotBlank() && watch.service() == HealthBackgroundService.SERVICE.toString()
        val connect = HealthBackgroundService.permission(context)
        val notifications = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val bluetoothOn = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
        } catch (_: SecurityException) { false }
        val scheduled = try {
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler)
                ?.getPendingJob(HealthBackgroundJob.ID) != null
        } catch (_: Exception) { false }
        return Snapshot(
            health(supported, read, granted, restoreReady),
            ble(readyWatch, connect, notifications, bluetoothOn, restoreReady),
            scheduled,
            HealthBackgroundService.snapshot(context).optBoolean("running")
        )
    }
}
