package com.repforge.app;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.SparseArray;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Foreground diagnostic connection with explicitly started, allowlisted read queries. */
@CapacitorPlugin(name = "BleDiscovery", permissions = {
    @Permission(alias = "bleScan", strings = {Manifest.permission.BLUETOOTH_SCAN}),
    @Permission(alias = "bleConnect", strings = {Manifest.permission.BLUETOOTH_CONNECT}),
    @Permission(alias = "bleLocation", strings = {Manifest.permission.ACCESS_FINE_LOCATION})
})
public class BleDiscoveryPlugin extends Plugin {
    private static final UUID HEART_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID RDFIT_SERVICE = UUID.fromString("6e40ab01-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID RDFIT_WRITE = UUID.fromString("6e40ab02-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID RDFIT_NOTIFY = UUID.fromString("6e40ab03-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private final Map<String, String> addresses = new HashMap<>();
    private final Map<String, JSObject> rows = new LinkedHashMap<>();
    private final Map<String, Long> lastSeen = new HashMap<>();
    private BluetoothLeScanner scanner;
    private BluetoothGatt activeGatt;
    private BluetoothGattCharacteristic heart;
    private BluetoothGattDescriptor heartCccd;
    private ScanCallback scanCallback;
    private BluetoothGattCharacteristic probeWrite, probeNotify;
    private BluetoothGattDescriptor probeCccd;
    private PluginCall probing;
    private RdfitProtocol probeDecoder;
    private boolean probeSupported = false, probeWritePending = false, probeBatteryReceived = false, probeStepsReceived = false;
    private int probeCommand = 0, probeNotifications = 0, probeRequests = 0, probeRejected = 0;
    private String probeStatus = "idle";
    private Integer probeBattery;
    private Long probeSteps;
    private Runnable probeTimeout;
    private PluginCall connecting, subscribing, pendingPermission;
    private JSArray services = new JSArray();
    private String state = "idle", code = "", deviceName = "";
    private String expectedService = "";
    private String scanSavedAddress = "";
    private long scanEndsAt = 0;
    private int nextDeviceNumber = 0;
    private int gattStatus = 0;
    private volatile boolean foreground = true;
    private boolean heartSupported = false;
    private long lastPulse = 0;
    private Runnable scanTimeout, connectionTimeout, subscriptionTimeout;

    private BleWatchPreference savedWatch() { return new BleWatchPreference(getContext()); }

    private BluetoothAdapter adapter() {
        BluetoothManager manager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return manager == null ? null : manager.getAdapter();
    }
    private boolean permissions() {
        if (Build.VERSION.SDK_INT >= 31) return getPermissionState("bleScan") == PermissionState.GRANTED
            && getPermissionState("bleConnect") == PermissionState.GRANTED;
        return getPermissionState("bleLocation") == PermissionState.GRANTED;
    }
    private void requirePermissions(PluginCall call) {
        pendingPermission = call;
        requestPermissionForAliases(Build.VERSION.SDK_INT >= 31 ? new String[]{"bleScan", "bleConnect"}
            : new String[]{"bleLocation"}, call, "scanPermissionResult");
    }
    @PermissionCallback private void scanPermissionResult(PluginCall call) {
        if (call == null || call != pendingPermission) return;
        pendingPermission = null;
        if (!permissions()) { call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); return; }
        main.post(() -> beginScan(call));
    }
    private JSObject snapshot() {
        JSObject out = new JSObject();
        out.put("state", state); out.put("code", code); out.put("gattStatus", gattStatus);
        out.put("deviceName", deviceName); out.put("services", services);
        JSObject saved = new JSObject();
        saved.put("present", !savedWatch().address().isEmpty()); saved.put("name", savedWatch().name());
        out.put("rememberedWatch", saved);
        out.put("scanRemainingSeconds", scanCallback == null ? 0
            : Math.max(0, (scanEndsAt - android.os.SystemClock.elapsedRealtime() + 999) / 1000));
        out.put("probeSupported", probeSupported);
        JSObject probe = new JSObject(); probe.put("status", probeStatus);
        probe.put("batteryReceived", probeBatteryReceived); probe.put("stepsReceived", probeStepsReceived);
        probe.put("notifications", probeNotifications); probe.put("requests", probeRequests); probe.put("rejectedFrames", probeRejected);
        out.put("rdfitProbe", probe);
        JSObject readings = new JSObject(); readings.put("battery", probeBattery == null ? org.json.JSONObject.NULL : probeBattery);
        readings.put("steps", probeSteps == null ? org.json.JSONObject.NULL : probeSteps); out.put("probeReadings", readings);
        out.put("heartSupported", heartSupported); out.put("permissionGranted", permissions());
        out.put("sdk", Build.VERSION.SDK_INT);
        BluetoothAdapter adapter = adapter();
        out.put("supported", adapter != null && getContext().getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        try { out.put("enabled", adapter != null && adapter.isEnabled()); }
        catch (SecurityException ignored) { out.put("enabled", false); }
        return out;
    }
    private void transition(String next, String reason) {
        state = next; code = reason;
        JSObject event = snapshot(); event.put("kind", "state");
        notifyListeners("bleEvent", event);
    }
    @PluginMethod public void getStatus(PluginCall call) { main.post(() -> call.resolve(snapshot())); }
    @PluginMethod public void startScan(PluginCall call) {
        if (!permissions()) { requirePermissions(call); return; }
        main.post(() -> beginScan(call));
    }
    private void beginScan(PluginCall call) {
        if (!foreground) { call.reject("Keep the app open", "BACKGROUND"); return; }
        if (activeGatt != null || scanCallback != null) { call.reject("Bluetooth is busy", "BUSY"); return; }
        BluetoothAdapter adapter = adapter();
        if (adapter == null || !getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            call.reject("Bluetooth LE is unavailable", "UNSUPPORTED"); return;
        }
        try {
            if (!adapter.isEnabled()) { call.reject("Enable Bluetooth in system settings", "BLUETOOTH_OFF"); return; }
            if (Build.VERSION.SDK_INT < 31) {
                LocationManager location = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                if (location == null || !location.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    call.reject("Enable location for Bluetooth scanning on this Android version", "LOCATION_OFF"); return;
                }
            }
            scanner = adapter.getBluetoothLeScanner();
            if (scanner == null) { call.reject("Bluetooth scanner unavailable", "BLUETOOTH_OFF"); return; }
            devices.clear(); addresses.clear(); rows.clear(); lastSeen.clear(); resetProbe(); nextDeviceNumber = 0;
            scanSavedAddress = savedWatch().address();
            services = new JSArray(); deviceName = ""; heartSupported = false; gattStatus = 0;
            ScanCallback callback = new ScanCallback() {
                @Override public void onScanResult(int type, ScanResult result) {
                    main.post(() -> { if (scanCallback == this) found(result); });
                }
                @Override public void onBatchScanResults(List<ScanResult> results) {
                    main.post(() -> { if (scanCallback == this) for (ScanResult result : results) found(result); });
                }
                @Override public void onScanFailed(int error) {
                    main.post(() -> { if (scanCallback == this) { stopScanInternal(); transition("idle", "SCAN_FAILED"); } });
                }
            };
            scanCallback = callback;
            scanEndsAt = android.os.SystemClock.elapsedRealtime() + BleScanPolicy.DURATION_MS;
            scanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback);
            transition("scanning", "");
            scanTimeout = () -> { stopScanInternal(); transition("idle", "SCAN_FINISHED"); };
            main.postDelayed(scanTimeout, BleScanPolicy.DURATION_MS);
            call.resolve(snapshot());
        } catch (SecurityException error) { stopScanInternal(); call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); }
        catch (RuntimeException error) { stopScanInternal(); call.reject("Bluetooth scan failed", "SCAN_FAILED"); }
    }
    private void found(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        String address;
        try { address = device.getAddress(); } catch (SecurityException ignored) { return; }
        String id = addresses.get(address);
        long now = android.os.SystemClock.elapsedRealtime();
        if (id != null && now - lastSeen.getOrDefault(id, -1000L) < 1000) return;
        ScanRecord record = result.getScanRecord();
        String name = record == null ? null : record.getDeviceName();
        if (name == null) try { name = device.getName(); } catch (SecurityException ignored) { }
        JSObject row = new JSObject(); row.put("id", id); row.put("name", name == null ? "" : name);
        row.put("rssi", result.getRssi());
        // The address is displayed locally to identify unnamed watches; diagnostics omit it.
        row.put("displayAddress", address != null && address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}") ? address : "");
        row.put("remembered", address != null && !scanSavedAddress.isEmpty() && address.equalsIgnoreCase(scanSavedAddress));
        JSArray advertised = new JSArray(), manufacturers = new JSArray();
        if (record != null) {
            List<ParcelUuid> uuids = record.getServiceUuids();
            if (uuids != null) for (ParcelUuid uuid : uuids) advertised.put(uuid.toString());
            SparseArray<byte[]> data = record.getManufacturerSpecificData();
            for (int i = 0; data != null && i < data.size(); i++) {
                JSObject company = new JSObject(); company.put("companyId", data.keyAt(i));
                company.put("length", data.valueAt(i) == null ? 0 : data.valueAt(i).length); manufacturers.put(company);
            }
        }
        row.put("advertisedServices", advertised); row.put("manufacturers", manufacturers);
        String removed = null;
        if (id == null) {
            List<BleScanPolicy.Candidate> retained = new ArrayList<>();
            for (Map.Entry<String, JSObject> item : rows.entrySet()) {
                if (item.getKey() != null) retained.add(scanCandidate(item.getKey(), item.getValue()));
            }
            removed = BleScanPolicy.replacement(retained, scanCandidate("", row));
            if ("".equals(removed) || nextDeviceNumber >= 65535) return;
            if (removed != null) {
                devices.remove(removed); rows.remove(removed); lastSeen.remove(removed);
                addresses.values().remove(removed);
            }
            id = UUID.randomUUID().toString(); addresses.put(address, id); devices.put(id, device);
            row.put("number", ++nextDeviceNumber);
        } else row.put("number", rows.get(id).getInteger("number", 1));
        row.put("id", id); rows.put(id, row); lastSeen.put(id, now);
        JSObject event = new JSObject(); event.put("kind", "device"); event.put("device", row);
        if (removed != null) event.put("removedId", removed);
        notifyListeners("bleEvent", event);
    }
    private BleScanPolicy.Candidate scanCandidate(String id, JSObject row) {
        String address = row.getString("displayAddress", ""), name = row.getString("name", "");
        String services = row.optJSONArray("advertisedServices") == null ? "" : row.optJSONArray("advertisedServices").toString().toLowerCase(Locale.ROOT);
        int priority = !address.isEmpty() && address.equalsIgnoreCase(scanSavedAddress) ? 4
            : name.toLowerCase(Locale.ROOT).matches(".*gt\\s*4.*") ? 3
            : services.contains("6e40ab01") || services.contains("0000ae00") || services.contains("00002222") || services.contains("00004444") ? 2
            : services.contains("00000201") ? 1 : 0;
        return new BleScanPolicy.Candidate(id, priority, row.getInteger("rssi", -127));
    }
    private void stopScanInternal() {
        ScanCallback callback = scanCallback; scanCallback = null;
        if (scanTimeout != null) main.removeCallbacks(scanTimeout);
        if (scanner != null && callback != null) try { scanner.stopScan(callback); } catch (RuntimeException ignored) { }
        scanner = null;
        scanEndsAt = 0;
    }
    @PluginMethod public void stopScan(PluginCall call) {
        main.post(() -> { stopScanInternal(); if (state.equals("scanning")) transition("idle", "SCAN_FINISHED"); call.resolve(snapshot()); });
    }
    @PluginMethod public void connect(PluginCall call) {
        main.post(() -> {
            if (!permissions()) { call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); return; }
            if (!foreground) { call.reject("Keep the app open", "BACKGROUND"); return; }
            if (activeGatt != null) { call.reject("Bluetooth is busy", "BUSY"); return; }
            String id = call.getString("id", ""); BluetoothDevice device = devices.get(id);
            if (device == null) { call.reject("Choose a device from a new scan", "DEVICE_EXPIRED"); return; }
            startConnection(call, device, rows.get(id).getString("name", ""), "");
        });
    }
    @PluginMethod public void rememberWatch(PluginCall call) {
        main.post(() -> {
            if (activeGatt == null || !state.equals("connected") || !probeSupported && !heartSupported) {
                call.reject("Connect a supported watch first", "NO_SAVED_WATCH"); return;
            }
            try {
                if (!savedWatch().save(activeGatt.getDevice().getAddress(), deviceName,
                    (probeSupported ? RDFIT_SERVICE : HEART_SERVICE).toString())) {
                    call.reject("Cannot save selected watch", "SAVE_WATCH_FAILED"); return;
                }
                call.resolve(snapshot());
            } catch (SecurityException error) { call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); }
        });
    }
    @PluginMethod public void forgetWatch(PluginCall call) {
        main.post(() -> {
            if (!savedWatch().clear()) { call.reject("Cannot forget selected watch", "SAVE_WATCH_FAILED"); return; }
            stopScanInternal(); failConnection("WATCH_FORGOTTEN"); call.resolve(snapshot());
        });
    }
    @PluginMethod public void connectRemembered(PluginCall call) {
        main.post(() -> {
            if (!permissions()) { call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); return; }
            if (!foreground) { call.reject("Keep the app open", "BACKGROUND"); return; }
            if (activeGatt != null || scanCallback != null) { call.reject("Bluetooth is busy", "BUSY"); return; }
            BleWatchPreference saved = savedWatch(); String address = saved.address(), service = saved.service();
            if (address.isEmpty() || !service.equals(RDFIT_SERVICE.toString()) && !service.equals(HEART_SERVICE.toString())) {
                call.reject("No verified selected watch", "NO_SAVED_WATCH"); return;
            }
            try {
                BluetoothAdapter adapter = adapter();
                if (adapter == null || !adapter.isEnabled()) { call.reject("Enable Bluetooth", "BLUETOOTH_OFF"); return; }
                startConnection(call, adapter.getRemoteDevice(address), saved.name(), service);
            } catch (SecurityException error) { call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); }
            catch (RuntimeException error) { call.reject("Cannot reconnect", "CONNECTION_FAILED"); }
        });
    }
    private void startConnection(PluginCall call, BluetoothDevice device, String name, String service) {
            stopScanInternal(); resetProbe(); services = new JSArray(); heartSupported = false; heart = null; heartCccd = null;
            deviceName = name; expectedService = service; connecting = call; gattStatus = 0;
            transition("connecting", "");
            try {
                activeGatt = device.connectGatt(getContext(), false, callbacks, BluetoothDevice.TRANSPORT_LE);
                if (activeGatt == null) { failConnection("CONNECTION_FAILED"); return; }
                connectionTimeout = () -> failConnection("CONNECTION_TIMEOUT");
                main.postDelayed(connectionTimeout, 20000);
            } catch (SecurityException error) { failConnection("PERMISSION_DENIED"); }
            catch (RuntimeException error) { failConnection("CONNECTION_FAILED"); }
    }
    private final BluetoothGattCallback callbacks = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int next) {
            main.post(() -> {
                if (gatt != activeGatt) return;
                gattStatus = status;
                if (status != BluetoothGatt.GATT_SUCCESS || next == BluetoothProfile.STATE_DISCONNECTED) {
                    failConnection(status == BluetoothGatt.GATT_SUCCESS ? "DISCONNECTED" : "CONNECTION_FAILED"); return;
                }
                if (next == BluetoothProfile.STATE_CONNECTED) {
                    transition("discovering", "");
                    try { if (!gatt.discoverServices()) failConnection("DISCOVERY_FAILED"); }
                    catch (RuntimeException error) { failConnection("DISCOVERY_FAILED"); }
                }
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            main.post(() -> {
                if (gatt != activeGatt) return;
                gattStatus = status;
                if (status != BluetoothGatt.GATT_SUCCESS) { failConnection("DISCOVERY_FAILED"); return; }
                try {
                services = describe(gatt);
                BluetoothGattService rdfitService = gatt.getService(RDFIT_SERVICE);
                BluetoothGattCharacteristic rdfitWrite = rdfitService == null ? null : rdfitService.getCharacteristic(RDFIT_WRITE);
                BluetoothGattCharacteristic rdfitNotify = rdfitService == null ? null : rdfitService.getCharacteristic(RDFIT_NOTIFY);
                probeSupported = rdfitWrite != null && rdfitNotify != null && rdfitNotify.getDescriptor(CCCD) != null
                    && (rdfitWrite.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                    && (rdfitNotify.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                BluetoothGattService service = gatt.getService(HEART_SERVICE);
                heart = service == null ? null : service.getCharacteristic(HEART_MEASUREMENT);
                heartCccd = heart == null ? null : heart.getDescriptor(CCCD);
                heartSupported = heart != null && heartCccd != null && (heart.getProperties()
                    & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0;
                if (!expectedService.isEmpty() && !(expectedService.equals(RDFIT_SERVICE.toString()) ? probeSupported : heartSupported)) {
                    services = new JSArray(); probeSupported = false; heartSupported = false;
                    failConnection("WATCH_CHANGED"); return;
                }
                if (connectionTimeout != null) main.removeCallbacks(connectionTimeout);
                transition("connected", "");
                if (connecting != null) { PluginCall pending = connecting; connecting = null; pending.resolve(snapshot()); }
                } catch (SecurityException error) { failConnection("PERMISSION_DENIED"); }
                catch (RuntimeException error) { failConnection("DISCOVERY_FAILED"); }
            });
        }
        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            main.post(() -> {
                if (gatt != activeGatt) return;
                if (probing != null && descriptor == probeCccd && probeCommand == 0) {
                    gattStatus = status;
                    if (status != BluetoothGatt.GATT_SUCCESS) finishProbe("PROBE_FAILED");
                    else sendProbeQuery(RdfitProtocol.BATTERY);
                    return;
                }
                if (descriptor != heartCccd || subscribing == null) return;
                if (subscriptionTimeout != null) main.removeCallbacks(subscriptionTimeout);
                PluginCall pending = subscribing; subscribing = null;
                if (status == BluetoothGatt.GATT_SUCCESS) { transition("monitoring", ""); pending.resolve(snapshot()); }
                else { transition("connected", "SUBSCRIBE_FAILED"); pending.reject("Pulse subscription failed", "SUBSCRIBE_FAILED"); }
            });
        }
        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            main.post(() -> {
                if (gatt != activeGatt || characteristic != probeWrite || probing == null) return;
                gattStatus = status;
                if (status != BluetoothGatt.GATT_SUCCESS) { finishProbe("PROBE_FAILED"); return; }
                probeWritePending = false; advanceProbe();
            });
        }
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            receiveProbe(gatt, characteristic, characteristic.getValue());
            receivePulse(gatt, characteristic, characteristic.getValue());
        }
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            receiveProbe(gatt, characteristic, value);
            receivePulse(gatt, characteristic, value);
        }
    };
    private void resetProbe() {
        probeSupported = false; probeStatus = "idle"; probeBattery = null; probeSteps = null;
        probeBatteryReceived = false; probeStepsReceived = false; probeNotifications = 0; probeRequests = 0; probeRejected = 0;
    }
    @PluginMethod public void readRdfitData(PluginCall call) {
        main.post(() -> {
            if (!foreground || !permissions()) { call.reject("Keep Bluetooth available", "PERMISSION_DENIED"); return; }
            if (activeGatt == null || !state.equals("connected") || !probeSupported || probing != null) {
                call.reject("No supported RDFit channel", "NO_RDFIT_CHANNEL"); return;
            }
            BluetoothGattService service = activeGatt.getService(RDFIT_SERVICE);
            probeWrite = service == null ? null : service.getCharacteristic(RDFIT_WRITE);
            probeNotify = service == null ? null : service.getCharacteristic(RDFIT_NOTIFY);
            probeCccd = probeNotify == null ? null : probeNotify.getDescriptor(CCCD);
            if (probeWrite == null || probeNotify == null || probeCccd == null) {
                call.reject("No supported RDFit channel", "NO_RDFIT_CHANNEL"); return;
            }
            probeDecoder = new RdfitProtocol(); probeStatus = "running"; probeBattery = null; probeSteps = null;
            probeNotifications = 0; probeRequests = 0; probeRejected = 0; probeCommand = 0;
            probeBatteryReceived = false; probeStepsReceived = false; probeWritePending = false; probing = call;
            transition("probing", "");
            probeTimeout = () -> finishProbe(probeBatteryReceived ? "PROBE_PARTIAL" : "PROBE_TIMEOUT");
            main.postDelayed(probeTimeout, 20000);
            try {
                if (!activeGatt.setCharacteristicNotification(probeNotify, true)) { finishProbe("PROBE_FAILED"); return; }
                boolean queued;
                if (Build.VERSION.SDK_INT >= 33) queued = activeGatt.writeDescriptor(probeCccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS;
                else { probeCccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); queued = activeGatt.writeDescriptor(probeCccd); }
                if (!queued) finishProbe("PROBE_FAILED");
            } catch (RuntimeException error) { finishProbe("PROBE_FAILED"); }
        });
    }
    private void sendProbeQuery(int command) {
        if (probing == null || activeGatt == null) return;
        probeCommand = command; probeWritePending = true;
        byte[] bytes = RdfitProtocol.request(command);
        try {
            boolean queued;
            if (Build.VERSION.SDK_INT >= 33) queued = activeGatt.writeCharacteristic(probeWrite, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS;
            else { probeWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); probeWrite.setValue(bytes); queued = activeGatt.writeCharacteristic(probeWrite); }
            if (!queued) { finishProbe("PROBE_FAILED"); return; }
            probeRequests++;
        } catch (RuntimeException error) { finishProbe("PROBE_FAILED"); }
    }
    private void receiveProbe(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] payload) {
        byte[] bytes = payload == null ? null : payload.clone();
        main.post(() -> {
            if (gatt != activeGatt || characteristic != probeNotify || probing == null) return;
            if (++probeNotifications > 64) { finishProbe("PROBE_FAILED"); return; }
            for (RdfitProtocol.Reading reading : probeDecoder.accept(bytes)) {
                if (reading.command != probeCommand) continue;
                if (reading.battery != null) { probeBattery = reading.battery; probeBatteryReceived = true; }
                if (reading.steps != null) { probeSteps = reading.steps; probeStepsReceived = true; }
            }
            probeRejected = probeDecoder.rejectedFrames; advanceProbe();
        });
    }
    private void advanceProbe() {
        if (probing == null || probeWritePending) return;
        if (probeCommand == RdfitProtocol.BATTERY && probeBatteryReceived) sendProbeQuery(RdfitProtocol.STEPS);
        else if (probeCommand == RdfitProtocol.STEPS && probeStepsReceived) finishProbe("PROBE_DONE");
    }
    private void finishProbe(String reason) {
        if (probing == null) return;
        PluginCall pending = probing; probing = null;
        probeStatus = reason.equals("PROBE_DONE") ? "complete" : probeBatteryReceived ? "partial" : "failed";
        closeGatt(); transition("disconnected", reason); pending.resolve(snapshot());
    }
    private JSArray describe(BluetoothGatt gatt) {
        JSArray result = new JSArray();
        for (BluetoothGattService service : gatt.getServices()) {
            JSObject item = new JSObject(); item.put("uuid", service.getUuid().toString()); item.put("type", service.getType());
            JSArray characteristics = new JSArray();
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                JSObject entry = new JSObject(); entry.put("uuid", characteristic.getUuid().toString()); entry.put("properties", characteristic.getProperties());
                JSArray descriptors = new JSArray();
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) descriptors.put(descriptor.getUuid().toString());
                entry.put("descriptors", descriptors); characteristics.put(entry);
            }
            item.put("characteristics", characteristics); result.put(item);
        }
        return result;
    }
    private void receivePulse(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] payload) {
        byte[] value = payload == null ? null : payload.clone();
        main.post(() -> {
            if (gatt != activeGatt || characteristic != heart || !state.equals("monitoring")) return;
            Integer bpm = HeartRateMeasurement.parse(value);
            long now = android.os.SystemClock.elapsedRealtime();
            if (bpm == null || now - lastPulse < 1000) return;
            lastPulse = now;
            JSObject event = new JSObject(); event.put("kind", "pulse"); event.put("bpm", bpm);
            SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            event.put("measuredAt", utc.format(new Date())); notifyListeners("bleEvent", event);
        });
    }
    @PluginMethod public void startHeartRate(PluginCall call) {
        main.post(() -> {
            if (!foreground || !permissions()) { call.reject("Bluetooth is unavailable", "PERMISSION_DENIED"); return; }
            if (activeGatt == null || !state.equals("connected") || !heartSupported) {
                call.reject("No standard heart-rate service is available", "NO_HEART_SERVICE"); return;
            }
            subscribing = call; transition("subscribing", ""); lastPulse = 0;
            try {
                if (!activeGatt.setCharacteristicNotification(heart, true)) { failSubscription(); return; }
                byte[] enable = (heart.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                boolean queued;
                if (Build.VERSION.SDK_INT >= 33) queued = activeGatt.writeDescriptor(heartCccd, enable) == BluetoothStatusCodes.SUCCESS;
                else { heartCccd.setValue(enable); queued = activeGatt.writeDescriptor(heartCccd); }
                if (!queued) { failSubscription(); return; }
                subscriptionTimeout = this::failSubscription; main.postDelayed(subscriptionTimeout, 10000);
            } catch (RuntimeException error) { failSubscription(); }
        });
    }
    private void failSubscription() {
        if (subscribing == null) return;
        PluginCall pending = subscribing; subscribing = null;
        // A timeout makes the GATT queue uncertain: close rather than queue another descriptor write.
        closeGatt(); transition("disconnected", "SUBSCRIBE_FAILED"); pending.reject("Pulse subscription failed", "SUBSCRIBE_FAILED");
    }
    private void closeGatt() {
        if (probeTimeout != null) main.removeCallbacks(probeTimeout);
        probeWrite = null; probeNotify = null; probeCccd = null; probeDecoder = null; probeWritePending = false;
        if (pendingPermission != null) { PluginCall pending = pendingPermission; pendingPermission = null; pending.reject("Operation cancelled", "CANCELLED"); }
        if (connectionTimeout != null) main.removeCallbacks(connectionTimeout);
        if (subscriptionTimeout != null) main.removeCallbacks(subscriptionTimeout);
        BluetoothGatt old = activeGatt; activeGatt = null; heart = null; heartCccd = null;
        if (old != null) { try { old.disconnect(); } catch (RuntimeException ignored) { } try { old.close(); } catch (RuntimeException ignored) { } }
    }
    private void failConnection(String reason) {
        closeGatt();
        probeBattery = null; probeSteps = null;
        if (probing != null) { probeStatus = "failed"; PluginCall pending = probing; probing = null; pending.reject("RDFit query ended", reason); }
        if (connecting != null) { PluginCall pending = connecting; connecting = null; pending.reject("Bluetooth connection ended", reason); }
        if (subscribing != null) { PluginCall pending = subscribing; subscribing = null; pending.reject("Pulse subscription ended", reason); }
        transition("disconnected", reason);
    }
    @PluginMethod public void disconnect(PluginCall call) {
        main.post(() -> { stopScanInternal(); failConnection("DISCONNECTED"); call.resolve(snapshot()); });
    }
    @Override protected void handleOnPause() {
        foreground = false;
        main.post(() -> { if (pendingPermission != null) return; stopScanInternal(); failConnection("BACKGROUND"); devices.clear(); addresses.clear(); rows.clear(); });
    }
    @Override protected void handleOnResume() { foreground = true; }
    @Override protected void handleOnDestroy() {
        foreground = false;
        main.post(() -> { stopScanInternal(); failConnection("DISCONNECTED"); devices.clear(); addresses.clear(); rows.clear(); });
    }
}
