package com.repforge.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class BleDiscoveredDevice(
    val id: String, // MAC Cím
    val name: String,
    val displayAddress: String,
    val rssi: Int,
    val remembered: Boolean
)

data class BleDiscoveryUiState(
    val state: String = "idle", // "idle" | "scanning" | "connected" | "probing" | "monitoring" | "disconnected"
    val code: String = "IDLE",
    val scanRemainingSeconds: Int = 0,
    val deviceName: String = "",
    val liveHeartRateBpm: Int? = null,
    val heartSupported: Boolean = false,
    val probeSupported: Boolean = false,
    val rememberedWatchPresent: Boolean = false,
    val rdfitProbe: RdfitProbeResult = RdfitProbeResult(),
    val devices: List<BleDiscoveredDevice> = emptyList()
)

/**
 * #111 Bluetooth Low Energy eszközkereső és GATT kapcsolatkezelő.
 */
class BleDiscoveryRepository(context: Context) {
    private val tag = "BleDiscoveryRepo"
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences("trainpilot_ble", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(BleDiscoveryUiState())
    val uiState: StateFlow<BleDiscoveryUiState> = _uiState.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var scanCountdownJob: Job? = null
    private val discoveredDevicesMap = LinkedHashMap<String, BleDiscoveredDevice>()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val rememberedAddr = prefs().getString("remembered_address", "").orEmpty()
        val rememberedName = prefs().getString("remembered_name", "").orEmpty()
        val hasRemembered = rememberedAddr.isNotEmpty()

        _uiState.value = _uiState.value.copy(
            rememberedWatchPresent = hasRemembered,
            deviceName = if (hasRemembered && _uiState.value.deviceName.isEmpty()) rememberedName else _uiState.value.deviceName
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _uiState.value = _uiState.value.copy(state = "idle", code = "BLUETOOTH_OFF")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _uiState.value = _uiState.value.copy(state = "idle", code = "SCANNER_UNAVAILABLE")
            return
        }

        discoveredDevicesMap.clear()
        _uiState.value = _uiState.value.copy(
            state = "scanning",
            code = "SCANNING",
            scanRemainingSeconds = 60,
            devices = emptyList()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(BleWatchAndProtocols.HEART_RATE_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(BleWatchAndProtocols.RDFIT_SERVICE_UUID)).build()
        )

        try {
            scanner.startScan(null, settings, leScanCallback)
            startScanCountdown()
        } catch (e: Exception) {
            Log.e(tag, "Hiba a BLE keresés indításakor", e)
            _uiState.value = _uiState.value.copy(state = "idle", code = "SCAN_ERROR")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCountdownJob?.cancel()
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        if (_uiState.value.state == "scanning") {
            _uiState.value = _uiState.value.copy(state = "idle", code = "IDLE", scanRemainingSeconds = 0)
        }
    }

    private fun startScanCountdown() {
        scanCountdownJob?.cancel()
        scanCountdownJob = scope.launch {
            for (sec in 60 downTo 1) {
                _uiState.value = _uiState.value.copy(scanRemainingSeconds = sec)
                delay(1000L)
            }
            stopScan()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val name = result.scanRecord?.deviceName ?: device.name ?: ""
            val address = device.address ?: return
            val rememberedAddr = prefs().getString("remembered_address", "")

            val dev = BleDiscoveredDevice(
                id = address,
                name = name,
                displayAddress = address,
                rssi = result.rssi,
                remembered = address.equals(rememberedAddr, ignoreCase = true)
            )

            discoveredDevicesMap[address] = dev
            _uiState.value = _uiState.value.copy(devices = discoveredDevicesMap.values.toList())
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String) {
        stopScan()
        val adapter = bluetoothAdapter ?: return
        val device = adapter.getRemoteDevice(deviceAddress) ?: return

        _uiState.value = _uiState.value.copy(state = "connecting", code = "CONNECTING", deviceName = device.name ?: deviceAddress)

        activeGatt?.close()
        activeGatt = device.connectGatt(appContext, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        activeGatt?.disconnect()
        activeGatt?.close()
        activeGatt = null
        _uiState.value = _uiState.value.copy(
            state = "disconnected",
            code = "DISCONNECTED",
            liveHeartRateBpm = null,
            heartSupported = false,
            probeSupported = false
        )
    }

    @SuppressLint("MissingPermission")
    fun startHeartRate() {
        val gatt = activeGatt ?: return
        val hrService = gatt.getService(BleWatchAndProtocols.HEART_RATE_SERVICE_UUID) ?: return
        val hrChar = hrService.getCharacteristic(BleWatchAndProtocols.HEART_RATE_MEASUREMENT_UUID) ?: return

        gatt.setCharacteristicNotification(hrChar, true)
        val descriptor = hrChar.getDescriptor(BleWatchAndProtocols.CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            _uiState.value = _uiState.value.copy(state = "monitoring", code = "MONITORING_HR")
        }
    }

    @SuppressLint("MissingPermission")
    fun readRdfitData() {
        val gatt = activeGatt ?: return
        val rdfitService = gatt.getService(BleWatchAndProtocols.RDFIT_SERVICE_UUID) ?: return
        val writeChar = rdfitService.getCharacteristic(BleWatchAndProtocols.RDFIT_WRITE_CHAR_UUID) ?: return

        _uiState.value = _uiState.value.copy(state = "probing", code = "PROBING_RDFIT")
        writeChar.value = BleWatchAndProtocols.buildRdfitQueryPacket()
        gatt.writeCharacteristic(writeChar)
    }

    fun rememberWatch() {
        val currentAddr = activeGatt?.device?.address ?: return
        val currentName = _uiState.value.deviceName
        prefs().edit()
            .putString("remembered_address", currentAddr)
            .putString("remembered_name", currentName)
            .apply()
        refreshStatus()
    }

    fun forgetWatch() {
        prefs().edit()
            .remove("remembered_address")
            .remove("remembered_name")
            .apply()
        refreshStatus()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                scope.launch {
                    _uiState.value = _uiState.value.copy(state = "connected", code = "CONNECTED")
                    gatt?.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                scope.launch {
                    disconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val hasHr = gatt.getService(BleWatchAndProtocols.HEART_RATE_SERVICE_UUID) != null
                val hasRdfit = gatt.getService(BleWatchAndProtocols.RDFIT_SERVICE_UUID) != null

                scope.launch {
                    _uiState.value = _uiState.value.copy(
                        heartSupported = hasHr,
                        probeSupported = hasRdfit
                    )
                    if (hasHr) startHeartRate()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic ?: return
            if (characteristic.uuid == BleWatchAndProtocols.HEART_RATE_MEASUREMENT_UUID) {
                val bpm = BleWatchAndProtocols.parseHeartRate(characteristic.value)
                if (bpm != null && bpm > 0) {
                    scope.launch {
                        _uiState.value = _uiState.value.copy(liveHeartRateBpm = bpm)
                    }
                }
            } else if (characteristic.uuid == BleWatchAndProtocols.RDFIT_NOTIFY_CHAR_UUID) {
                val probe = BleWatchAndProtocols.parseRdfitResponse(characteristic.value)
                scope.launch {
                    _uiState.value = _uiState.value.copy(rdfitProbe = probe, state = "connected")
                }
            }
        }
    }
}
