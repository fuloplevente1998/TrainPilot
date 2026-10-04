package com.repforge.app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.SparseArray;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.util.*;

/** Owns transient BLE scan results and their privacy-sensitive local identifiers. */
final class BleScanCatalog {
    static final class Accepted {
        final JSObject row;
        final String removedId;
        Accepted(JSObject row, String removedId) { this.row = row; this.removedId = removedId; }
    }

    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private final Map<String, String> addresses = new HashMap<>();
    private final Map<String, JSObject> rows = new LinkedHashMap<>();
    private final Map<String, Long> lastSeen = new HashMap<>();
    private String rememberedAddress = "";
    private int nextDeviceNumber = 0;

    void reset(String savedAddress) {
        clear();
        rememberedAddress = savedAddress == null ? "" : savedAddress;
        nextDeviceNumber = 0;
    }

    void clear() {
        devices.clear();
        addresses.clear();
        rows.clear();
        lastSeen.clear();
    }

    BluetoothDevice device(String id) { return devices.get(id); }

    String name(String id) {
        JSObject row = rows.get(id);
        return row == null ? "" : row.getString("name", "");
    }

    Accepted accept(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        String address;
        try { address = device.getAddress(); } catch (SecurityException ignored) { return null; }
        String id = addresses.get(address);
        long now = android.os.SystemClock.elapsedRealtime();
        if (id != null && now - lastSeen.getOrDefault(id, -1000L) < 1000) return null;

        ScanRecord record = result.getScanRecord();
        String name = record == null ? null : record.getDeviceName();
        if (name == null) try { name = device.getName(); } catch (SecurityException ignored) { }

        JSObject row = new JSObject();
        row.put("id", id);
        row.put("name", name == null ? "" : name);
        row.put("rssi", result.getRssi());
        // Address stays in this transient local scan catalog only; diagnostic exports omit it.
        row.put("displayAddress", address != null && address.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}") ? address : "");
        row.put("remembered", address != null && !rememberedAddress.isEmpty() && address.equalsIgnoreCase(rememberedAddress));

        JSArray advertised = new JSArray(), manufacturers = new JSArray();
        if (record != null) {
            List<ParcelUuid> uuids = record.getServiceUuids();
            if (uuids != null) for (ParcelUuid uuid : uuids) advertised.put(uuid.toString());
            SparseArray<byte[]> data = record.getManufacturerSpecificData();
            for (int i = 0; data != null && i < data.size(); i++) {
                JSObject company = new JSObject();
                company.put("companyId", data.keyAt(i));
                company.put("length", data.valueAt(i) == null ? 0 : data.valueAt(i).length);
                manufacturers.put(company);
            }
        }
        row.put("advertisedServices", advertised);
        row.put("manufacturers", manufacturers);

        String removed = null;
        if (id == null) {
            List<BleScanPolicy.Candidate> retained = new ArrayList<>();
            for (Map.Entry<String, JSObject> item : rows.entrySet()) {
                if (item.getKey() != null) retained.add(candidate(item.getKey(), item.getValue()));
            }
            removed = BleScanPolicy.replacement(retained, candidate("", row));
            if ("".equals(removed) || nextDeviceNumber >= 65535) return null;
            if (removed != null) {
                devices.remove(removed);
                rows.remove(removed);
                lastSeen.remove(removed);
                addresses.values().remove(removed);
            }
            id = UUID.randomUUID().toString();
            addresses.put(address, id);
            devices.put(id, device);
            row.put("number", ++nextDeviceNumber);
        } else {
            JSObject previous = rows.get(id);
            row.put("number", previous == null ? 1 : previous.getInteger("number", 1));
        }

        row.put("id", id);
        rows.put(id, row);
        lastSeen.put(id, now);
        return new Accepted(row, removed);
    }

    private BleScanPolicy.Candidate candidate(String id, JSObject row) {
        String address = row.getString("displayAddress", ""), name = row.getString("name", "");
        String services = row.optJSONArray("advertisedServices") == null ? ""
            : row.optJSONArray("advertisedServices").toString().toLowerCase(Locale.ROOT);
        int priority = !address.isEmpty() && address.equalsIgnoreCase(rememberedAddress) ? 4
            : name.toLowerCase(Locale.ROOT).matches(".*gt\\s*4.*") ? 3
            : services.contains("6e40ab01") || services.contains("0000ae00") || services.contains("00002222") || services.contains("00004444") ? 2
            : services.contains("00000201") ? 1 : 0;
        return new BleScanPolicy.Candidate(id, priority, row.getInteger("rssi", -127));
    }
}
