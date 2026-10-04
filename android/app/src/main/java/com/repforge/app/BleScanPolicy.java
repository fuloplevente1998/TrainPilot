package com.repforge.app;

import java.util.Collection;

/** Bounded discovery: retain the selected watch even in a crowded scan. */
final class BleScanPolicy {
    static final int DURATION_MS = 60000;
    static final int CAPACITY = 120;

    static final class Candidate {
        final String id;
        final int priority, rssi;
        Candidate(String id, int priority, int rssi) {
            this.id = id; this.priority = priority; this.rssi = rssi;
        }
    }

    // null means there is room; "" means ignore; otherwise replace that row.
    static String replacement(Collection<Candidate> retained, Candidate incoming) {
        if (retained.size() < CAPACITY) return null;
        Candidate weakest = null;
        for (Candidate item : retained) {
            if (weakest == null || item.priority < weakest.priority
                || item.priority == weakest.priority && item.rssi < weakest.rssi) weakest = item;
        }
        if (weakest == null || incoming.priority < weakest.priority) return "";
        if (incoming.priority == weakest.priority && incoming.rssi < weakest.rssi + 4) return "";
        return weakest.id;
    }
}
