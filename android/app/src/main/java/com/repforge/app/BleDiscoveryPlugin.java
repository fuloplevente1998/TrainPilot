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
import java.time.Instant;
import java.util.*;

/** Foreground diagnostic connection. Only standard heart-rate CCCD writes are allowed. */
@CapacitorPlugin(name = "BleDiscovery", permissions = {
    @Permission(alias = "bleScan", strings = {Manifest.permission.BLUETOOTH_SCAN}),
    @Permission(alias = "bleConnect", strings = {Manifest.permission.BLUETOOTH_CONNECT}),
    @Permission(alias = "bleLocation", strings = {Manifest.permission.ACCESS_FINE_LOCATION})
})
public class BleDiscoveryPlugin extends Plugin {
    private static final UUID HEART_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
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
    private PluginCall connecting, subscribing, pendingPermission;
    private JSArray services = new JSArray();
    private String state = "idle", code = "", deviceName = "";
    private int gattStatus = 0;
    private volatile boolean foreground = true;
    private boolean heartSupported = false;
    private long lastPulse = 0;
    private Runnable scanTimeout, connectionTimeout, subscriptionTimeout;

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
            devices.clear(); addresses.clear(); rows.clear(); lastSeen.clear();
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
            scanner.startScan(null, new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback);
            transition("scanning", "");
            scanTimeout = () -> { stopScanInternal(); transition("idle", "SCAN_FINISHED"); };
            main.postDelayed(scanTimeout, 12000);
            call.resolve(snapshot());
        } catch (SecurityException error) { stopScanInternal(); call.reject("Bluetooth permission denied", "PERMISSION_DENIED"); }
        catch (RuntimeException error) { stopScanInternal(); call.reject("Bluetooth scan failed", "SCAN_FAILED"); }
    }
    private void found(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        String address;
        try { address = device.getAddress(); } catch (SecurityException ignored) { return; }
        String id = addresses.get(address);
        if (id == null) {
            if (devices.size() >= 40) return;
            id = UUID.randomUUID().toString(); addresses.put(address, id); devices.put(id, device);
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastSeen.getOrDefault(id, -1000L) < 1000) return;
        lastSeen.put(id, now);
        ScanRecord record = result.getScanRecord();
        String name = record == null ? null : record.getDeviceName();
        if (name == null) try { name = device.getName(); } catch (SecurityException ignored) { }
        JSObject row = new JSObject(); row.put("id", id); row.put("name", name == null ? "" : name);
        row.put("rssi", result.getRssi());
        JSArray advertised = new JSArray(), manufacturers = new JSArray();
        if (record != null) {
            List<ParcelUuid> uuids = record.getServiceUuids();
            if (uuids != null) for (ParcelUuid uuid : uuids) advertised.put(uuid.toString());
            SparseArray<byte[]> data = record.getManufacturerSpecificData();
            for (int i = 0; i < data.size(); i++) {
                JSObject company = new JSObject(); company.put("companyId", data.keyAt(i));
                company.put("length", data.valueAt(i).length); manufacturers.put(company);
            }
        }
        row.put("advertisedServices", advertised); row.put("manufacturers", manufacturers); rows.put(id, row);
        JSObject event = new JSObject(); event.put("kind", "device"); event.put("device", row);
        notifyListeners("bleEvent", event);
    }
    private void stopScanInternal() {
        ScanCallback callback = scanCallback; scanCallback = null;
        if (scanTimeout != null) main.removeCallbacks(scanTimeout);
        if (scanner != null && callback != null) try { scanner.stopScan(callback); } catch (RuntimeException ignored) { }
        scanner = null;
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
            stopScanInternal(); services = new JSArray(); heartSupported = false; heart = null; heartCccd = null;
            deviceName = rows.get(id).getString("name", ""); connecting = call; gattStatus = 0;
            transition("connecting", "");
            try {
                activeGatt = device.connectGatt(getContext(), false, callbacks, BluetoothDevice.TRANSPORT_LE);
                if (activeGatt == null) { failConnection("CONNECTION_FAILED"); return; }
                connectionTimeout = () -> failConnection("CONNECTION_TIMEOUT");
                main.postDelayed(connectionTimeout, 20000);
            } catch (SecurityException error) { failConnection("PERMISSION_DENIED"); }
            catch (RuntimeException error) { failConnection("CONNECTION_FAILED"); }
        });
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
                services = describe(gatt);
                BluetoothGattService service = gatt.getService(HEART_SERVICE);
                heart = service == null ? null : service.getCharacteristic(HEART_MEASUREMENT);
                heartCccd = heart == null ? null : heart.getDescriptor(CCCD);
                heartSupported = heart != null && heartCccd != null && (heart.getProperties()
                    & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0;
                if (connectionTimeout != null) main.removeCallbacks(connectionTimeout);
                transition("connected", "");
                if (connecting != null) { PluginCall pending = connecting; connecting = null; pending.resolve(snapshot()); }
            });
        }
        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            main.post(() -> {
                if (gatt != activeGatt || descriptor != heartCccd || subscribing == null) return;
                if (subscriptionTimeout != null) main.removeCallbacks(subscriptionTimeout);
                PluginCall pending = subscribing; subscribing = null;
                if (status == BluetoothGatt.GATT_SUCCESS) { transition("monitoring", ""); pending.resolve(snapshot()); }
                else { transition("connected", "SUBSCRIBE_FAILED"); pending.reject("Pulse subscription failed", "SUBSCRIBE_FAILED"); }
            });
        }
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            receivePulse(gatt, characteristic, characteristic.getValue());
        }
        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            receivePulse(gatt, characteristic, value);
        }
    };
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
            event.put("measuredAt", Instant.now().toString()); notifyListeners("bleEvent", event);
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
        if (pendingPermission != null) { PluginCall pending = pendingPermission; pendingPermission = null; pending.reject("Operation cancelled", "CANCELLED"); }
        if (connectionTimeout != null) main.removeCallbacks(connectionTimeout);
        if (subscriptionTimeout != null) main.removeCallbacks(subscriptionTimeout);
        BluetoothGatt old = activeGatt; activeGatt = null; heart = null; heartCccd = null;
        if (old != null) { try { old.disconnect(); } catch (RuntimeException ignored) { } try { old.close(); } catch (RuntimeException ignored) { } }
    }
    private void failConnection(String reason) {
        closeGatt();
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
