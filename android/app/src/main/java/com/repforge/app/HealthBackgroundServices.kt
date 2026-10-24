package com.repforge.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ext.SdkExtensions
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** User-enabled watch connection. Independent of UI; only verified battery/steps queries. */
class HealthBackgroundService : Service() {
    companion object {
        const val STOP: String = "health.WATCH_STOP"
        const val REFRESH: String = "health.WATCH_REFRESH"
        const val CHANNEL: String = "trainpilot_health_watch"
        const val STATUS: String = "health_background_status"
        @JvmField val SERVICE: UUID = UUID.fromString("6e40ab01-b5a3-f393-e0a9-e50e24dcca9e")
        @JvmField val WRITE: UUID = UUID.fromString("6e40ab02-b5a3-f393-e0a9-e50e24dcca9e")
        @JvmField val NOTIFY: UUID = UUID.fromString("6e40ab03-b5a3-f393-e0a9-e50e24dcca9e")
        @JvmField val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val NOTIFICATION: Int = 1204

        @Volatile
        private var instance: HealthBackgroundService? = null

        @JvmStatic
        fun permission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }

        @JvmStatic
        @Throws(Exception::class)
        fun configured(context: Context): Boolean {
            val watch = BleWatchPreference(context)
            val prefs = HealthJournalStore.get(context).preferences()
            return prefs.optBoolean("bleBackground") &&
                prefs.optBoolean("logWatchSteps") &&
                watch.address().isNotEmpty() &&
                SERVICE.toString() == watch.service() &&
                permission(context) &&
                (Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        }

        @JvmStatic
        fun resume(context: Context): Boolean {
            return try {
                if (!configured(context) || HealthJournalStore.get(context).pendingRestore().isNotEmpty()) false
                else {
                    ContextCompat.startForegroundService(context, Intent(context, HealthBackgroundService::class.java))
                    true // Started/requested, not yet a verified BLE connection.
                }
            } catch (_: Exception) { false }
        }

        @JvmStatic
        fun stop(context: Context) {
            val service = instance
            if (service != null) {
                service.main.post {
                    service.active = false
                    service.close()
                    service.status("stopped", "")
                    service.stopSelf()
                }
            }
            context.stopService(Intent(context, HealthBackgroundService::class.java))
        }

        @JvmStatic
        fun yieldConnection() {
            val service = instance
            if (service != null) {
                service.close()
                service.status("waiting", "TRIAL_ACTIVE")
                service.retry(15_000L)
            }
        }

        @JvmStatic
        fun snapshot(context: Context): JSONObject {
            return try {
                val value = JSONObject(context.getSharedPreferences(STATUS, MODE_PRIVATE).getString("state", "{}") ?: "{}")
                val running = instance != null && instance!!.active
                value.put("running", running)
                if (!running) value.put("state", "stopped")
                value
            } catch (_: Exception) {
                JSONObject()
            }
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private val storage = Executors.newSingleThreadExecutor()
    private var gatt: BluetoothGatt? = null
    private var write: BluetoothGattCharacteristic? = null
    private var notify: BluetoothGattCharacteristic? = null
    private var decoder: RdfitProtocol? = null

    @Volatile
    private var active: Boolean = false
    private var writePending: Boolean = false
    private var received: Boolean = false
    private var command: Int = 0
    private var attempts: Int = 0
    private var notifications: Int = 0
    private var state: String = "stopped"
    private var reason: String = ""
    private var language: String = "hu"
    private var identity: String = ""
    private var name: String = ""
    private var steps: Long? = null
    private var battery: Int? = null
    private var started: Long = 0L
    private var epoch: Long = 0L
    private var lastReading: Long = 0L
    private var retryRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null
    private var pollRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, id: Int): Int {
        if (intent != null && STOP == intent.action) {
            try {
                HealthJournalStore.get(this).setPreferences(JSONObject().put("bleBackground", false))
            } catch (_: Exception) {
            }
            active = false
            close()
            status("stopped", "")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            if (!configured(this)) {
                active = false
                close()
                status("stopped", "DISABLED_OR_PERMISSION")
                stopSelf()
                return START_NOT_STICKY
            }
            if (intent != null) {
                language = intent.getStringExtra("language") ?: language
            }
            if (Build.VERSION.SDK_INT >= 26) {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(CHANNEL, "TrainPilot Bluetooth", NotificationManager.IMPORTANCE_LOW)
                )
            }
            startForeground(NOTIFICATION, notification())
            if (!active) {
                active = true
                attempts = 0
                connect()
            } else if (intent != null && REFRESH == intent.action && "connected" == state) {
                query()
            }
            return START_STICKY
        } catch (_: Exception) {
            active = false
            close()
            status("stopped", "SERVICE_UNAVAILABLE")
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun usable(): Boolean {
        return try {
            active && configured(this) && HealthJournalStore.get(this).pendingRestore().isEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun connect() {
        if (!usable()) {
            active = false
            close()
            status("stopped", "DISABLED_OR_PERMISSION")
            stopSelf()
            return
        }
        if (!BleConnectionLease.acquire(this)) {
            status("waiting", "TRIAL_ACTIVE")
            retry(15_000L)
            return
        }
        try {
            val manager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter? = manager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                failed("BLUETOOTH_OFF")
                return
            }
            val watch = BleWatchPreference(this)
            identity = watch.journalId()
            name = watch.name()
            status("connecting", "")
            gatt = adapter.getRemoteDevice(watch.address()).connectGatt(this, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                failed("CONNECTION_FAILED")
                return
            }
            timeoutRunnable = Runnable { failed("CONNECTION_TIMEOUT") }
            main.postDelayed(timeoutRunnable!!, 60_000L)
        } catch (_: RuntimeException) {
            failed("CONNECTION_FAILED")
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, next: Int) {
            main.post {
                if (connection !== gatt) return@post
                if (status != BluetoothGatt.GATT_SUCCESS || next == BluetoothProfile.STATE_DISCONNECTED) {
                    failed("CONNECTION_LOST")
                    return@post
                }
                if (next == BluetoothProfile.STATE_CONNECTED) {
                    try {
                        this@HealthBackgroundService.status("discovering", "")
                        if (!connection.discoverServices()) failed("DISCOVERY_FAILED")
                    } catch (_: RuntimeException) {
                        failed("DISCOVERY_FAILED")
                    }
                }
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, result: Int) {
            main.post {
                if (connection !== gatt) return@post
                try {
                    val service: BluetoothGattService? = connection.getService(SERVICE)
                    write = service?.getCharacteristic(WRITE)
                    notify = service?.getCharacteristic(NOTIFY)
                    val descriptor = notify?.getDescriptor(CCCD)
                    if (result != BluetoothGatt.GATT_SUCCESS ||
                        write == null ||
                        notify == null ||
                        descriptor == null ||
                        (write!!.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 ||
                        (notify!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
                    ) {
                        disable("WATCH_CHANGED")
                        return@post
                    }
                    if (!connection.setCharacteristicNotification(notify, true)) {
                        failed("SUBSCRIBE_FAILED")
                        return@post
                    }
                    val queued = if (Build.VERSION.SDK_INT >= 33) {
                        connection.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        connection.writeDescriptor(descriptor)
                    }
                    if (!queued) failed("SUBSCRIBE_FAILED")
                } catch (_: RuntimeException) {
                    failed("SUBSCRIBE_FAILED")
                }
            }
        }

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, result: Int) {
            main.post {
                if (connection !== gatt) return@post
                if (result != BluetoothGatt.GATT_SUCCESS) {
                    failed("SUBSCRIBE_FAILED")
                    return@post
                }
                timeoutRunnable?.let { main.removeCallbacks(it) }
                status("connected", "")
                query()
            }
        }

        override fun onCharacteristicWrite(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, result: Int) {
            main.post {
                if (connection !== gatt || characteristic !== write || command == 0) return@post
                if (result != BluetoothGatt.GATT_SUCCESS) {
                    failed("QUERY_FAILED")
                    return@post
                }
                writePending = false
                advance()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            receive(connection, characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
            receive(connection, characteristic, payload)
        }
    }

    private fun query() {
        if (!usable()) {
            disable("DISABLED_OR_PERMISSION")
            return
        }
        if (gatt == null || write == null || command != 0) return
        pollRunnable?.let { main.removeCallbacks(it) }
        decoder = RdfitProtocol()
        notifications = 0
        battery = null
        steps = null
        started = System.currentTimeMillis()
        epoch = HealthJournalStore.get(this).generation()
        status("reading", "")
        timeoutRunnable = Runnable { failed("QUERY_TIMEOUT") }
        main.postDelayed(timeoutRunnable!!, 25_000L)
        send(RdfitProtocol.BATTERY)
    }

    private fun send(next: Int) {
        command = next
        received = false
        writePending = true
        try {
            val payload = RdfitProtocol.request(next)
            val queued = if (Build.VERSION.SDK_INT >= 33) {
                gatt!!.writeCharacteristic(write!!, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                write!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                write!!.value = payload
                @Suppress("DEPRECATION")
                gatt!!.writeCharacteristic(write!!)
            }
            if (!queued) failed("QUERY_FAILED")
        } catch (_: RuntimeException) {
            failed("QUERY_FAILED")
        }
    }

    private fun receive(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic, payload: ByteArray?) {
        val bytes = payload?.clone()
        main.post {
            if (connection !== gatt || characteristic !== notify || command == 0) return@post
            if (++notifications > 64) {
                failed("QUERY_FAILED")
                return@post
            }
            for (reading in decoder!!.accept(bytes)) {
                if (reading.command != command) continue
                if (reading.battery != null) {
                    battery = reading.battery
                    received = true
                }
                if (reading.steps != null) {
                    steps = reading.steps
                    received = true
                }
            }
            advance()
        }
    }

    private fun advance() {
        if (writePending || !received) return
        if (command == RdfitProtocol.BATTERY) {
            send(RdfitProtocol.STEPS)
            return
        }
        if (command != RdfitProtocol.STEPS || steps == null) return
        attempts = 0
        command = 0
        timeoutRunnable?.let { main.removeCallbacks(it) }
        val count = steps!!
        val requestAt = started
        val observedAt = System.currentTimeMillis()
        val generation = epoch
        val source = identity
        val device = name
        storage.execute {
            try {
                if (!usable() || source != BleWatchPreference(this).journalId()) return@execute
                HealthJournalStore.get(this).watchSteps(source, device, count, requestAt, observedAt, generation)
                main.post {
                    if (!active) return@post
                    lastReading = observedAt
                    status("connected", "")
                }
            } catch (_: Exception) {
                main.post {
                    if (active) status("connected", "SAVE_RETRY")
                }
            }
        }
        status("connected", "")
        pollRunnable = Runnable { query() }
        main.postDelayed(pollRunnable!!, 5 * 60_000L)
    }

    private fun failed(why: String) {
        close()
        if (!active) return
        if (!usable()) {
            disable("DISABLED_OR_PERMISSION")
            return
        }
        status("waiting", why)
        retry(BleReconnectPolicy.delay(attempts++))
    }

    private fun retry(delay: Long) {
        retryRunnable?.let { main.removeCallbacks(it) }
        if (!active) return
        retryRunnable = Runnable { connect() }
        main.postDelayed(retryRunnable!!, delay)
    }

    private fun disable(why: String) {
        try {
            HealthJournalStore.get(this).setPreferences(JSONObject().put("bleBackground", false))
        } catch (_: Exception) {
        }
        active = false
        close()
        status("stopped", why)
        stopSelf()
    }

    private fun close() {
        retryRunnable?.let { main.removeCallbacks(it) }
        timeoutRunnable?.let { main.removeCallbacks(it) }
        pollRunnable?.let { main.removeCallbacks(it) }
        val old = gatt
        gatt = null
        write = null
        notify = null
        decoder = null
        command = 0
        writePending = false
        received = false
        if (old != null) {
            try {
                old.disconnect()
            } catch (_: RuntimeException) {
            }
            try {
                old.close()
            } catch (_: RuntimeException) {
            }
        }
        BleConnectionLease.release(this)
    }

    private fun status(next: String, code: String) {
        state = next
        reason = code
        try {
            val value = JSONObject()
                .put("state", state)
                .put("code", reason)
                .put("running", active)
                .put("updatedAt", HealthJournalStore.iso(System.currentTimeMillis()))
                .put("lastReadingAt", if (lastReading == 0L) JSONObject.NULL else HealthJournalStore.iso(lastReading))
                .put("battery", battery ?: JSONObject.NULL)
            getSharedPreferences(STATUS, MODE_PRIVATE).edit().putString("state", value.toString()).apply()
            if (active) {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
            }
        } catch (_: Exception) {
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            1204,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            1205,
            Intent(this, HealthBackgroundService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = DistanceTrackingService.text(
            language,
            "Óra háttérkapcsolata",
            "Background watch connection",
            "Uhr-Hintergrundverbindung",
            "Conexiunea ceasului în fundal"
        )
        val body = DistanceTrackingService.text(
            language,
            if ("connected" == state) "Kapcsolódva · lépések frissítése 5 percenként" else "Kapcsolódás / újracsatlakozás…",
            if ("connected" == state) "Connected · steps refreshed every 5 minutes" else "Connecting / reconnecting…",
            if ("connected" == state) "Verbunden · Schritte alle 5 Minuten" else "Verbindung / Wiederverbindung…",
            if ("connected" == state) "Conectat · pași actualizați la 5 minute" else "Conectare / reconectare…"
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, DistanceTrackingService.text(language, "Leállítás", "Stop", "Stoppen", "Oprește"), stop)
            .build()
    }

    override fun onDestroy() {
        active = false
        close()
        status("stopped", reason)
        storage.shutdownNow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (instance === this) instance = null
        super.onDestroy()
    }
}

/** Health Connect background reads have a separate permission; the BLE service grants none. */
class HealthBackgroundJob : JobService() {
    companion object {
        const val ID: Int = 1204

        @JvmStatic
        fun supported(context: Context): Boolean {
            return Build.VERSION.SDK_INT >= 34 &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13 &&
                context.getSystemService(HealthConnectManager::class.java) != null
        }

        @JvmStatic
        fun permitted(context: Context): Boolean {
            return supported(context) &&
                context.checkSelfPermission("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND") == PackageManager.PERMISSION_GRANTED
        }

        @JvmStatic
        @Throws(Exception::class)
        fun schedule(context: Context) {
            val jobs = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            if (!HealthJournalStore.get(context).preferences().optBoolean("hcBackground") || !permitted(context)) {
                jobs.cancel(ID)
                return
            }
            if (jobs.getPendingJob(ID) == null &&
                jobs.schedule(
                    JobInfo.Builder(ID, ComponentName(context, HealthBackgroundJob::class.java))
                        .setPeriodic(15 * 60 * 1000L)
                        .setPersisted(false)
                        .build()
                ) != JobScheduler.RESULT_SUCCESS
            ) {
                throw IllegalStateException("Cannot schedule Health Connect background sync")
            }
        }
    }

    private val worker = Executors.newSingleThreadExecutor()

    @Volatile
    private var cancelled = AtomicBoolean()

    override fun onStartJob(params: JobParameters): Boolean {
        val stopped = AtomicBoolean()
        cancelled = stopped
        worker.execute {
            var retry = false
            try {
                val store = HealthJournalStore.get(this)
                val epoch = store.generation()
                val access = HealthConnectRepository.Access(this, epoch) {
                    if (stopped.get() ||
                        !permitted(this) ||
                        !store.preferences().optBoolean("hcBackground") ||
                        store.pendingRestore().isNotEmpty()
                    ) {
                        throw IllegalStateException("Background sync stopped")
                    }
                }
                access.requireForeground()
                val today = LocalDate.now()
                var complete = true
                for (i in 1 downTo 0) {
                    val date = today.minusDays(i.toLong())
                    val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val end = if (i == 0) Instant.now() else date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    if (end.isAfter(start) && Build.VERSION.SDK_INT >= 34) {
                        val day = HealthConnectRepository.Api34.readHealthDay(
                            access,
                            JSONObject().put("start", start.toString()).put("end", end.toString())
                        )
                        if (day.optJSONArray("warnings")?.let { it.length() > 0 } == true) {
                            complete = false
                        }
                    }
                }
                access.requireForeground()
                if (store.generation() != epoch) throw IllegalStateException("Dataset changed")
                val meta = JSONObject().put("lastAttemptAt", Instant.now().toString())
                if (complete) meta.put("lastSyncAt", Instant.now().toString())
                store.syncMeta(meta)
            } catch (_: Exception) {
                retry = !stopped.get() && permitted(this)
            } finally {
                if (!stopped.get()) jobFinished(params, retry)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        cancelled.set(true)
        return false
    }

    override fun onDestroy() {
        cancelled.set(true)
        worker.shutdownNow()
        super.onDestroy()
    }
}
