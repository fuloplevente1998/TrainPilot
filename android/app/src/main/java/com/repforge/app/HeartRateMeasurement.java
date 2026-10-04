package com.repforge.app;

/** Bluetooth SIG Heart Rate Measurement (0x2A37), never vendor-specific payloads. */
final class HeartRateMeasurement {
    static Integer parse(byte[] value) {
        if (value == null || value.length < 2) return null;
        int flags = value[0] & 255;
        if ((flags & 0xe0) != 0) return null;
        int offset = (flags & 1) == 0 ? 2 : 3;
        if (value.length < offset) return null;
        int bpm = value[1] & 255;
        if ((flags & 1) != 0) bpm |= (value[2] & 255) << 8;
        if (bpm == 0 || ((flags & 4) != 0 && (flags & 2) == 0)) return null;
        if ((flags & 8) != 0) offset += 2;
        if (value.length < offset) return null;
        if ((flags & 16) != 0) {
            if (value.length <= offset || (value.length - offset) % 2 != 0) return null;
        } else if (value.length != offset) return null;
        return bpm;
    }
}
