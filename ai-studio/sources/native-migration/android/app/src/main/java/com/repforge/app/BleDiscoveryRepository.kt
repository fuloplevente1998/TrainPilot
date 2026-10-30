package com.repforge.app

import android.Manifest
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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.max

data class BleDiscoveredDeviceUiModel(
    val id: String,
    val number: Int,
    val name: String,
    val displayAddress: String,
    val rssi: Int,
    val remembered: Boolean,
    val advertisedServices: List<String>,
    val manufacturerIds: List<Int>
)

data class BleGattCharacteristicUiModel(
    val uuid: String,
    val properties: Int,
    val descriptors: List<String>
)

data class BleGattServiceUiModel(
    val uuid: String,
    val type: Int,
    val characteristics: List<BleGattCharacteristicUiModel>
)

data class RdfitProbeUiModel(
    val status: String = "idle",
    val batteryReceived: Boolean = false,
    val stepsReceived: Boolean = false,
    val notifications: Int = 0,
    val requests: Int = 0,
    val rejectedFrames: Int = 0,
    val batteryPercent: Int? = null,
    val stepsCount: Long? = null
)

data class BleDiscoveryUiState(
    val state: String = "idle", // "idle", "scanning", "connecting", "discovering", "connected", "probing", "subscribing", "monitoring", "disconnected"
    val code: String = "",
    val gattStatus: Int = 0,
    val deviceName: String = "",
    val rememberedWatchPresent: Boolean = false,
    val rememberedWatchName: String = "",
    val scanRemainingSeconds: Int = 0,
    val devices: List<BleDiscoveredDeviceUiModel> = emptyList(),
    val services: List<BleGattServiceUiModel> = emptyList(),
    val probeSupported: Boolean = false,
    val rdfitProbe: RdfitProbeUiModel = RdfitProbeUiModel(),
    val heartSupported: Boolean = false,
    val liveHeartRateBpm: Int? = null,
    val liveHeartRateMeasuredAt: String = "",
    val permissionGranted: Boolean = false,
    val bluetoothSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false
)

class BleDiscoveryRepository(context: Context) {
    companion object {
        private val HEART_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val RDFIT_SERVICE: UUID = UUID.fromString("6e40ab01-b5a3-f393-e0a9-e50e24dcca9e")
        private val RDFIT_WRITE: UUID = UUID.fromString("6e40ab02-b5a3-f393-e0a9-e50e24dcca9e")
        private val RDFIT_NOTIFY: UUID = UUID.fromString("6e40ab03-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val MAC_REGEX = Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val devices = LinkedHashMap<String, BluetoothDevice>()
    private val addresses = HashMap<String, String>()
    private val rows = LinkedHashMap<String, BleDiscoveredDeviceUiModel>()
    private val lastSeen = HashMap<String, Long>()

    private var scanner: BluetoothLeScanner? = null
    private var activeGatt: BluetoothGatt? = null
    private var heart: BluetoothGattCharacteristic? = null
    private var heartCccd: BluetoothGattDescriptor? = null
    private var scanCallback: ScanCallback? = null
    private var probeWrite: BluetoothGattCharacteristic? = null
    private var probeNotify: BluetoothGattCharacteristic? = null
    private var probeCccd: BluetoothGattDescriptor? = null
    private var probeDecoder: RdfitProtocol? = null

    private var probeSupported: Boolean = false
    private var probeWritePending: Boolean = false
    private var probeBatteryReceived: Boolean = false
    private var probeStepsReceived: Boolean = false
    private var probeCommand: Int = 0
    private var probeNotifications: Int = 0
    private var probeRequests: Int = 0
    private var probeRejected: Int = 0
    private var probeStatus: String = "idle"
    private var probeBattery: Int? = null
    private var probeSteps: Long? = null
    private var probeStartedAt: Long = 0L
    private var probeGeneration: Long = 0L
    private var probeJournalId: String = ""
    private var probeJournalName: String = ""
    private var isProbing: Boolean = false
    private var isSubscribing: Boolean = false

    private var services: List<BleGattServiceUiModel> = emptyList()
    private var state: String = "idle"
    private var code: String = ""
    private var deviceName: String = ""
    private var expectedService: String = ""
    private var scanSavedAddress: String = ""
    private var scanEndsAt: Long = 0L
    private var nextDeviceNumber: Int = 0
    private var gattStatus: Int = 0
    private var heartSupported: Boolean = false
    private var liveBpm: Int? = null
    private var liveBpmAt: String = ""
    private var lastPulse: Long = 0L

    private var scanTimeout: Runnable? = null
    private var scanCountdownTick: Runnable? = null
    private var connectionTimeout: Runnable? = null
    private var subscriptionTimeout: Runnable? = null
    private var probeTimeout: Runnable? = null

    private val _uiState = MutableStateFlow(BleDiscoveryUiState())
    val uiState: StateFlow<BleDiscoveryUiState> = _uiState.asStateFlow()

    init {
        emitSnapshot()
    }

    private fun savedWatch(): BleWatchPreference = BleWatchPreference(appContext)

    private fun adapter(): BluetoothAdapter? {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshStatus() {
        main.post { emitSnapshot() }
    }

    private fun emitSnapshot() {
        val btAdapter = adapter()
        val btSupported = btAdapter != null && appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val btEnabled = try {
            btAdapter != null && btAdapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
        val saved = savedWatch()
        val remainingSec = if (scanCallback == null) {
            0
        } else {
            max(0L, (scanEndsAt - SystemClock.elapsedRealtime() + 999L) / 1000L).toInt()
        }
        _uiState.value = BleDiscoveryUiState(
            state = state,
            code = code,
            gattStatus = gattStatus,
            deviceName = deviceName,
            rememberedWatchPresent = saved.address().isNotEmpty(),
            rememberedWatchName = saved.name(),
            scanRemainingSeconds = remainingSec,
            devices = rows.values.toList(),
            services = services,
            probeSupported = probeSupported,
            rdfitProbe = RdfitProbeUiModel(
                status = probeStatus,
                batteryReceived = probeBatteryReceived,
                stepsReceived = probeStepsReceived,
                notifications = probeNotifications,
                requests = probeRequests,
                rejectedFrames = probeRejected,
                batteryPercent = probeBattery,
                stepsCount = probeSteps
            ),
            heartSupported = heartSupported,
            liveHeartRateBpm = liveBpm,
            liveHeartRateMeasuredAt = liveBpmAt,
            permissionGranted = hasPermissions(),
            bluetoothSupported = btSupported,
            bluetoothEnabled = btEnabled
        )
    }

    private fun transition(next: String, reason: String) {
        state = next
        code = reason
        emitSnapshot()
    }

    fun startScan() {
        main.post {
            if (!hasPermissions()) {
                transition("idle", "PERMISSION_DENIED")
                return@post
            }
            if (activeGatt != null || scanCallback != null) {
                transition(state, "BUSY")
                return@post
            }
            val btAdapter = adapter()
            if (btAdapter == null || !appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                transition("idle", "UNSUPPORTED")
                return@post
            }
            try {
                if (!btAdapter.isEnabled) {
                    transition("idle", "BLUETOOTH_OFF")
                    return@post
                }
                if (Build.VERSION.SDK_INT < 31) {
                    val location = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (location == null ||
                        (!location.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                            !location.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    ) {
                        transition("idle", "LOCATION_OFF")
                        return@post
                    }
                }
                scanner = btAdapter.bluetoothLeScanner
                if (scanner == null) {
                    transition("idle", "BLUETOOTH_OFF")
                    return@post
                }
                devices.clear()
                addresses.clear()
                rows.clear()
                lastSeen.clear()
                resetProbe()
                nextDeviceNumber = 0
                scanSavedAddress = savedWatch().address()
                services = emptyList()
                deviceName = ""
                heartSupported = false
                liveBpm = null
                liveBpmAt = ""
                gattStatus = 0

                val callback = object : ScanCallback() {
                    override fun onScanResult(type: Int, result: ScanResult) {
                        main.post { if (scanCallback === this) found(result) }
                    }

                    override fun onBatchScanResults(results: List<ScanResult>) {
                        main.post {
                            if (scanCallback === this) {
                                for (r in results) found(r)
                            }
                        }
                    }

                    override fun onScanFailed(error: Int) {
                        main.post {
                            if (scanCallback === this) {
                                stopScanInternal()
                                transition("idle", "SCAN_FAILED")
                            }
                        }
                    }
                }
                scanCallback = callback
                scanEndsAt = SystemClock.elapsedRealtime() + BleScanPolicy.DURATION_MS
                scanner!!.startScan(
                    null,
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                    callback
                )
                transition("scanning", "")
                scanTimeout = Runnable {
                    stopScanInternal()
                    transition("idle", "SCAN_FINISHED")
                }
                main.postDelayed(scanTimeout!!, BleScanPolicy.DURATION_MS.toLong())
                scheduleCountdownTick()
            } catch (_: SecurityException) {
                stopScanInternal()
                transition("idle", "PERMISSION_DENIED")
            } catch (_: RuntimeException) {
                stopScanInternal()
                transition("idle", "SCAN_FAILED")
            }
        }
    }

    private fun scheduleCountdownTick() {
        scanCountdownTick?.let { main.removeCallbacks(it) }
        if (scanCallback == null) return
        scanCountdownTick = Runnable {
            if (scanCallback != null) {
                emitSnapshot()
                scheduleCountdownTick()
            }
        }
        main.postDelayed(scanCountdownTick!!, 1000L)
    }

    private fun found(result: ScanResult) {
        val device = result.device
        val address = try {
            device.address
        } catch (_: SecurityException) {
            return
        }
        var id = addresses[address]
        val now = SystemClock.elapsedRealtime()
        if (id != null && now - (lastSeen[id] ?: -1000L) < 1000L) return
        val record = result.scanRecord
        var name = record?.deviceName
        if (name == null) {
            try {
                name = device.name
            } catch (_: SecurityException) {
            }
        }
        val displayAddress = if (address != null && MAC_REGEX.matches(address)) address else ""
        val remembered = address != null && scanSavedAddress.isNotEmpty() && address.equals(scanSavedAddress, ignoreCase = true)
        val advertised = ArrayList<String>()
        val manufacturers = ArrayList<Int>()
        if (record != null) {
            record.serviceUuids?.forEach { advertised.add(it.toString()) }
            val mData = record.manufacturerSpecificData
            if (mData != null) {
                for (i in 0 until mData.size()) {
                    manufacturers.add(mData.keyAt(i))
                }
            }
        }

        val incomingCandidate = scanCandidate("", displayAddress, name ?: "", advertised, result.rssi)
        val number: Int
        if (id == null) {
            val retained = rows.map { (k, v) ->
                scanCandidate(k, v.displayAddress, v.name, v.advertisedServices, v.rssi)
            }
            val removed = BleScanPolicy.replacement(retained, incomingCandidate)
            if ("" == removed || nextDeviceNumber >= 65535) return
            if (removed != null) {
                devices.remove(removed)
                rows.remove(removed)
                lastSeen.remove(removed)
                addresses.values.remove(removed)
            }
            id = UUID.randomUUID().toString()
            addresses[address] = id
            devices[id] = device
            number = ++nextDeviceNumber
        } else {
            number = rows[id]?.number ?: 1
        }
        rows[id] = BleDiscoveredDeviceUiModel(
            id = id,
            number = number,
            name = name ?: "",
            displayAddress = displayAddress,
            rssi = result.rssi,
            remembered = remembered,
            advertisedServices = advertised,
            manufacturerIds = manufacturers
        )
        lastSeen[id] = now
        emitSnapshot()
    }

    private fun scanCandidate(
        id: String,
        address: String,
        name: String,
        advertisedServices: List<String>,
        rssi: Int
    ): BleScanPolicy.Candidate {
        val servicesStr = advertisedServices.joinToString(",").lowercase(Locale.ROOT)
        val priority = when {
            address.isNotEmpty() && address.equals(scanSavedAddress, ignoreCase = true) -> 4
            name.lowercase(Locale.ROOT).matches(Regex(".*gt\\s*4.*")) -> 3
            servicesStr.contains("6e40ab01") ||
                servicesStr.contains("0000ae00") ||
                servicesStr.contains("00002222") ||
                servicesStr.contains("00004444") -> 2
            servicesStr.contains("00000201") -> 1
            else -> 0
        }
        return BleScanPolicy.Candidate(id, priority, rssi)
    }

    private fun stopScanInternal() {
        val callback = scanCallback
        scanCallback = null
        scanTimeout?.let { main.removeCallbacks(it) }
        scanCountdownTick?.let { main.removeCallbacks(it) }
        if (scanner != null && callback != null) {
            try {
                scanner!!.stopScan(callback)
            } catch (_: RuntimeException) {
            }
        }
        scanner = null
        scanEndsAt = 0L
        if (activeGatt == null) BleConnectionLease.release(this)
    }

    fun stopScan() {
        main.post {
            stopScanInternal()
            if (state == "scanning") transition("idle", "SCAN_FINISHED")
            else emitSnapshot()
        }
    }

    fun connect(deviceId: String) {
        main.post {
            if (!hasPermissions()) {
                transition("disconnected", "PERMISSION_DENIED")
                return@post
            }
            if (activeGatt != null) {
                transition(state, "BUSY")
                return@post
            }
            val device = devices[deviceId]
            if (device == null) {
                transition("disconnected", "DEVICE_EXPIRED")
                return@post
            }
            startConnection(device, rows[deviceId]?.name ?: "", "")
        }
    }

    fun connectRemembered() {
        main.post {
            if (!hasPermissions()) {
                transition("disconnected", "PERMISSION_DENIED")
                return@post
            }
            if (activeGatt != null || scanCallback != null) {
                transition(state, "BUSY")
                return@post
            }
            val saved = savedWatch()
            val address = saved.address()
            val service = saved.service()
            if (address.isEmpty() || (service != RDFIT_SERVICE.toString() && service != HEART_SERVICE.toString())) {
                transition("disconnected", "NO_SAVED_WATCH")
                return@post
            }
            try {
                val btAdapter = adapter()
                if (btAdapter == null || !btAdapter.isEnabled) {
                    transition("disconnected", "BLUETOOTH_OFF")
                    return@post
                }
                startConnection(btAdapter.getRemoteDevice(address), saved.name(), service)
            } catch (_: SecurityException) {
                transition("disconnected", "PERMISSION_DENIED")
            } catch (_: RuntimeException) {
                transition("disconnected", "CONNECTION_FAILED")
            }
        }
    }

    private fun startConnection(device: BluetoothDevice, name: String, service: String) {
        stopScanInternal()
        HealthBackgroundService.yieldConnection()
        if (!BleConnectionLease.acquire(this)) {
            transition("disconnected", "BUSY")
            return
        }
        resetProbe()
        services = emptyList()
        heartSupported = false
        heart = null
        heartCccd = null
        liveBpm = null
        liveBpmAt = ""
        deviceName = name
        expectedService = service
        gattStatus = 0
        transition("connecting", "")
        try {
            activeGatt = device.connectGatt(appContext, false, callbacks, BluetoothDevice.TRANSPORT_LE)
            if (activeGatt == null) {
                failConnection("CONNECTION_FAILED")
                return
            }
            connectionTimeout = Runnable { failConnection("CONNECTION_TIMEOUT") }
            main.postDelayed(connectionTimeout!!, 20_000L)
        } catch (_: SecurityException) {
            failConnection("PERMISSION_DENIED")
        } catch (_: RuntimeException) {
            failConnection("CONNECTION_FAILED")
        }
    }

    fun rememberWatch() {
        main.post {
            if (activeGatt == null || state != "connected" || (!probeSupported && !heartSupported)) {
                transition(state, "NO_SAVED_WATCH")
                return@post
            }
            try {
                if (!savedWatch().save(
                        activeGatt!!.device.address,
                        deviceName,
                        (if (probeSupported) RDFIT_SERVICE else HEART_SERVICE).toString()
                    )
                ) {
                    transition(state, "SAVE_WATCH_FAILED")
                    return@post
                }
                emitSnapshot()
            } catch (_: SecurityException) {
                transition(state, "PERMISSION_DENIED")
            }
        }
    }

    fun forgetWatch() {
        main.post {
            if (!savedWatch().clear()) {
                transition(state, "SAVE_WATCH_FAILED")
                return@post
            }
            stopScanInternal()
            failConnection("WATCH_FORGOTTEN")
        }
    }

    fun readRdfitData() {
        main.post {
            if (!hasPermissions()) {
                transition(state, "PERMISSION_DENIED")
                return@post
            }
            if (activeGatt == null || state != "connected" || !probeSupported || isProbing) {
                transition(state, "NO_RDFIT_CHANNEL")
                return@post
            }
            val service = activeGatt!!.getService(RDFIT_SERVICE)
            probeWrite = service?.getCharacteristic(RDFIT_WRITE)
            probeNotify = service?.getCharacteristic(RDFIT_NOTIFY)
            probeCccd = probeNotify?.getDescriptor(CCCD)
            if (probeWrite == null || probeNotify == null || probeCccd == null) {
                transition(state, "NO_RDFIT_CHANNEL")
                return@post
            }
            probeStartedAt = System.currentTimeMillis()
            probeGeneration = HealthJournalStore.get(appContext).generation()
            val journalWatch = BleWatchPreference(appContext)
            probeJournalId = if (activeGatt!!.device.address.equals(journalWatch.address(), ignoreCase = true)) {
                journalWatch.journalId()
            } else {
                ""
            }
            probeJournalName = journalWatch.name()
            probeDecoder = RdfitProtocol()
            probeStatus = "running"
            probeBattery = null
            probeSteps = null
            probeNotifications = 0
            probeRequests = 0
            probeRejected = 0
            probeCommand = 0
            probeBatteryReceived = false
            probeStepsReceived = false
            probeWritePending = false
            isProbing = true
            transition("probing", "")
            probeTimeout = Runnable { finishProbe(if (probeBatteryReceived) "PROBE_PARTIAL" else "PROBE_TIMEOUT") }
            main.postDelayed(probeTimeout!!, 20_000L)
            try {
                if (!activeGatt!!.setCharacteristicNotification(probeNotify, true)) {
                    finishProbe("PROBE_FAILED")
                    return@post
                }
                val queued = if (Build.VERSION.SDK_INT >= 33) {
                    activeGatt!!.writeDescriptor(probeCccd!!, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    probeCccd!!.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    activeGatt!!.writeDescriptor(probeCccd!!)
                }
                if (!queued) finishProbe("PROBE_FAILED")
            } catch (_: RuntimeException) {
                finishProbe("PROBE_FAILED")
            }
        }
    }

    fun startHeartRate() {
        main.post {
            if (!hasPermissions()) {
                transition(state, "PERMISSION_DENIED")
                return@post
            }
            if (activeGatt == null || state != "connected" || !heartSupported) {
                transition(state, "NO_HEART_SERVICE")
                return@post
            }
            isSubscribing = true
            transition("subscribing", "")
            lastPulse = 0L
            try {
                if (!activeGatt!!.setCharacteristicNotification(heart, true)) {
                    failSubscription()
                    return@post
                }
                val enable = if ((heart!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                val queued = if (Build.VERSION.SDK_INT >= 33) {
                    activeGatt!!.writeDescriptor(heartCccd!!, enable) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    heartCccd!!.value = enable
                    @Suppress("DEPRECATION")
                    activeGatt!!.writeDescriptor(heartCccd!!)
                }
                if (!queued) {
                    failSubscription()
                    return@post
                }
                subscriptionTimeout = Runnable { failSubscription() }
                main.postDelayed(subscriptionTimeout!!, 10_000L)
            } catch (_: RuntimeException) {
                failSubscription()
            }
        }
    }

    fun disconnect() {
        main.post {
            stopScanInternal()
            failConnection("DISCONNECTED")
        }
    }

    private val callbacks = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, next: Int) {
            main.post {
                if (gatt !== activeGatt) return@post
                gattStatus = status
                if (status != BluetoothGatt.GATT_SUCCESS || next == BluetoothProfile.STATE_DISCONNECTED) {
                    failConnection(if (status == BluetoothGatt.GATT_SUCCESS) "DISCONNECTED" else "CONNECTION_FAILED")
                    return@post
                }
                if (next == BluetoothProfile.STATE_CONNECTED) {
                    transition("discovering", "")
                    try {
                        if (!gatt.discoverServices()) failConnection("DISCOVERY_FAILED")
                    } catch (_: RuntimeException) {
                        failConnection("DISCOVERY_FAILED")
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            main.post {
                if (gatt !== activeGatt) return@post
                gattStatus = status
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failConnection("DISCOVERY_FAILED")
                    return@post
                }
                try {
                    services = describe(gatt)
                    val rdfitService: BluetoothGattService? = gatt.getService(RDFIT_SERVICE)
                    val rdfitWrite = rdfitService?.getCharacteristic(RDFIT_WRITE)
                    val rdfitNotify = rdfitService?.getCharacteristic(RDFIT_NOTIFY)
                    probeSupported = rdfitWrite != null &&
                        rdfitNotify != null &&
                        rdfitNotify.getDescriptor(CCCD) != null &&
                        (rdfitWrite.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 &&
                        (rdfitNotify.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    val hrService: BluetoothGattService? = gatt.getService(HEART_SERVICE)
                    heart = hrService?.getCharacteristic(HEART_MEASUREMENT)
                    heartCccd = heart?.getDescriptor(CCCD)
                    heartSupported = heart != null &&
                        heartCccd != null &&
                        (heart!!.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                    if (expectedService.isNotEmpty() &&
                        !(if (expectedService == RDFIT_SERVICE.toString()) probeSupported else heartSupported)
                    ) {
                        services = emptyList()
                        probeSupported = false
                        heartSupported = false
                        failConnection("WATCH_CHANGED")
                        return@post
                    }
                    connectionTimeout?.let { main.removeCallbacks(it) }
                    transition("connected", "")
                } catch (_: SecurityException) {
                    failConnection("PERMISSION_DENIED")
                } catch (_: RuntimeException) {
                    failConnection("DISCOVERY_FAILED")
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            main.post {
                if (gatt !== activeGatt) return@post
                if (isProbing && descriptor === probeCccd && probeCommand == 0) {
                    gattStatus = status
                    if (status != BluetoothGatt.GATT_SUCCESS) finishProbe("PROBE_FAILED")
                    else sendProbeQuery(RdfitProtocol.BATTERY)
                    return@post
                }
                if (descriptor !== heartCccd || !isSubscribing) return@post
                subscriptionTimeout?.let { main.removeCallbacks(it) }
                isSubscribing = false
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    transition("monitoring", "")
                } else {
                    transition("connected", "SUBSCRIBE_FAILED")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            main.post {
                if (gatt !== activeGatt || characteristic !== probeWrite || !isProbing) return@post
                gattStatus = status
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    finishProbe("PROBE_FAILED")
                    return@post
                }
                probeWritePending = false
                advanceProbe()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val valBytes = characteristic.value
            receiveProbe(gatt, characteristic, valBytes)
            receivePulse(gatt, characteristic, valBytes)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            receiveProbe(gatt, characteristic, value)
            receivePulse(gatt, characteristic, value)
        }
    }

    private fun resetProbe() {
        probeSupported = false
        probeStatus = "idle"
        probeBattery = null
        probeSteps = null
        probeBatteryReceived = false
        probeStepsReceived = false
        probeNotifications = 0
        probeRequests = 0
        probeRejected = 0
        isProbing = false
    }

    private fun sendProbeQuery(command: Int) {
        if (!isProbing || activeGatt == null || probeWrite == null) return
        probeCommand = command
        probeWritePending = true
        val bytes = RdfitProtocol.request(command)
        try {
            val queued = if (Build.VERSION.SDK_INT >= 33) {
                activeGatt!!.writeCharacteristic(probeWrite!!, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                probeWrite!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                probeWrite!!.value = bytes
                @Suppress("DEPRECATION")
                activeGatt!!.writeCharacteristic(probeWrite!!)
            }
            if (!queued) {
                finishProbe("PROBE_FAILED")
                return
            }
            probeRequests++
            emitSnapshot()
        } catch (_: RuntimeException) {
            finishProbe("PROBE_FAILED")
        }
    }

    private fun receiveProbe(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, payload: ByteArray?) {
        val bytes = payload?.clone()
        main.post {
            if (gatt !== activeGatt || characteristic !== probeNotify || !isProbing) return@post
            if (++probeNotifications > 64) {
                finishProbe("PROBE_FAILED")
                return@post
            }
            for (reading in probeDecoder!!.accept(bytes)) {
                if (reading.command != probeCommand) continue
                if (reading.battery != null) {
                    probeBattery = reading.battery
                    probeBatteryReceived = true
                }
                if (reading.steps != null) {
                    probeSteps = reading.steps
                    probeStepsReceived = true
                }
            }
            probeRejected = probeDecoder!!.rejectedFrames
            emitSnapshot()
            advanceProbe()
        }
    }

    private fun advanceProbe() {
        if (!isProbing || probeWritePending) return
        if (probeCommand == RdfitProtocol.BATTERY && probeBatteryReceived) {
            sendProbeQuery(RdfitProtocol.STEPS)
        } else if (probeCommand == RdfitProtocol.STEPS && probeStepsReceived) {
            finishProbe("PROBE_DONE")
        }
    }

    private fun finishProbe(reason: String) {
        if (!isProbing) return
        isProbing = false
        probeStatus = when {
            reason == "PROBE_DONE" -> "complete"
            probeBatteryReceived -> "partial"
            else -> "failed"
        }
        val steps = probeSteps
        val validSteps = probeStepsReceived
        val deviceId = probeJournalId
        val name = probeJournalName
        val started = probeStartedAt
        val epoch = probeGeneration
        val observed = System.currentTimeMillis()
        closeGatt()
        transition("disconnected", reason)
        if (validSteps && steps != null && deviceId.isNotEmpty()) {
            ioExecutor.execute {
                try {
                    HealthJournalStore.get(appContext).watchSteps(deviceId, name, steps, started, observed, epoch)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun describe(gatt: BluetoothGatt): List<BleGattServiceUiModel> {
        return gatt.services.map { service ->
            BleGattServiceUiModel(
                uuid = service.uuid.toString(),
                type = service.type,
                characteristics = service.characteristics.map { ch ->
                    BleGattCharacteristicUiModel(
                        uuid = ch.uuid.toString(),
                        properties = ch.properties,
                        descriptors = ch.descriptors.map { it.uuid.toString() }
                    )
                }
            )
        }
    }

    private fun receivePulse(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, payload: ByteArray?) {
        val value = payload?.clone()
        main.post {
            if (gatt !== activeGatt || characteristic !== heart || state != "monitoring") return@post
            val bpm = HeartRateMeasurement.parse(value)
            val now = SystemClock.elapsedRealtime()
            if (bpm == null || now - lastPulse < 1000L) return@post
            lastPulse = now
            val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            utc.timeZone = TimeZone.getTimeZone("UTC")
            liveBpm = bpm
            liveBpmAt = utc.format(Date())
            emitSnapshot()
        }
    }

    private fun failSubscription() {
        if (!isSubscribing) return
        isSubscribing = false
        closeGatt()
        transition("disconnected", "SUBSCRIBE_FAILED")
    }

    private fun closeGatt() {
        probeTimeout?.let { main.removeCallbacks(it) }
        probeWrite = null
        probeNotify = null
        probeCccd = null
        probeDecoder = null
        probeWritePending = false
        isProbing = false
        isSubscribing = false
        connectionTimeout?.let { main.removeCallbacks(it) }
        subscriptionTimeout?.let { main.removeCallbacks(it) }
        val old = activeGatt
        activeGatt = null
        BleConnectionLease.release(this)
        heart = null
        heartCccd = null
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
    }

    private fun failConnection(reason: String) {
        if (isProbing) {
            probeStatus = "failed"
            probeBattery = null
            probeSteps = null
        }
        closeGatt()
        transition("disconnected", reason)
    }
}
