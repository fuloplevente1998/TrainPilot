package com.repforge.app;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

/** Serializes discovered GATT capabilities for the local diagnostic UI. */
final class BleGattIntrospection {
    private BleGattIntrospection() { }

    static JSArray describe(BluetoothGatt gatt) {
        JSArray result = new JSArray();
        for (BluetoothGattService service : gatt.getServices()) {
            JSObject item = new JSObject();
            item.put("uuid", service.getUuid().toString());
            item.put("type", service.getType());
            JSArray characteristics = new JSArray();
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                JSObject entry = new JSObject();
                entry.put("uuid", characteristic.getUuid().toString());
                entry.put("properties", characteristic.getProperties());
                JSArray descriptors = new JSArray();
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    descriptors.put(descriptor.getUuid().toString());
                }
                entry.put("descriptors", descriptors);
                characteristics.put(entry);
            }
            item.put("characteristics", characteristics);
            result.put(item);
        }
        return result;
    }
}
