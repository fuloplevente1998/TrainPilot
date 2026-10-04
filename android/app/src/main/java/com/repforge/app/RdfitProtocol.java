package com.repforge.app;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Original bounded decoder for the observed RDFit MCU transport; no history/settings commands. */
public final class RdfitProtocol {
    public static final int BATTERY = 4, STEPS = 10;
    private byte[] pending = new byte[0];
    public int rejectedFrames = 0;

    public static byte[] request(int command) {
        if (command != BATTERY && command != STEPS) throw new IllegalArgumentException("Unsupported query");
        byte[] payload = {(byte) command, 11};
        return new byte[]{(byte) 0xed, 0x40, 0, (byte) crc(payload), 0, 2, payload[0], payload[1]};
    }
    private static int crc(byte[] bytes) {
        int value = 255;
        for (byte b : bytes) {
            value ^= b & 255;
            for (int i = 0; i < 8; i++) value = (value & 1) != 0 ? (value >> 1) ^ 0xb8 : value >> 1;
        }
        return value;
    }
    public List<Reading> accept(byte[] notification) {
        List<Reading> readings = new ArrayList<>();
        if (notification == null || notification.length == 0) return readings;
        if (notification.length > 1024 || pending.length + notification.length > 2048) {
            pending = new byte[0]; rejectedFrames++; return readings;
        }
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        joined.write(pending, 0, pending.length); joined.write(notification, 0, notification.length);
        byte[] bytes = joined.toByteArray(); int offset = 0;
        while (bytes.length - offset >= 6) {
            if ((bytes[offset] & 255) != 0xed) { offset++; rejectedFrames++; continue; }
            int length = (bytes[offset + 4] & 255) * 256 + (bytes[offset + 5] & 255);
            if (length < 2 || length > 256) { offset++; rejectedFrames++; continue; }
            if (bytes.length - offset < length + 6) break;
            int flags = bytes[offset + 1] & 255;
            byte[] payload = Arrays.copyOfRange(bytes, offset + 6, offset + 6 + length);
            if ((flags != 0x40 && flags != 0x60) || bytes[offset + 2] != 0 || crc(payload) != (bytes[offset + 3] & 255)) rejectedFrames++;
            else {
                Reading reading = parse(payload);
                if (reading != null) readings.add(reading);
            }
            offset += length + 6;
        }
        pending = Arrays.copyOfRange(bytes, offset, bytes.length);
        return readings;
    }
    private static long unsigned32(byte[] bytes, int offset) {
        return ((long)(bytes[offset] & 255) << 24) | ((long)(bytes[offset + 1] & 255) << 16)
            | ((long)(bytes[offset + 2] & 255) << 8) | (bytes[offset + 3] & 255);
    }
    private static Reading parse(byte[] payload) {
        if (payload[1] != 11) return null;
        int command = payload[0] & 255;
        if (command == BATTERY && payload.length == 4 && (payload[2] & 255) <= 100) {
            return new Reading(command, payload[2] & 255, null, null, null);
        }
        if (command == STEPS && payload.length == 14) {
            long steps = unsigned32(payload, 2), calorieTenths = unsigned32(payload, 6), distance = unsigned32(payload, 10);
            if (steps <= 1000000 && calorieTenths <= 1000000 && distance <= 1000000)
                return new Reading(command, null, steps, calorieTenths / 10.0, distance);
        }
        return null;
    }
    public static final class Reading {
        public final int command;
        public final Integer battery;
        public final Long steps, distance;
        public final Double calories;
        private Reading(int command, Integer battery, Long steps, Double calories, Long distance) {
            this.command = command; this.battery = battery; this.steps = steps; this.calories = calories; this.distance = distance;
        }
    }
}
