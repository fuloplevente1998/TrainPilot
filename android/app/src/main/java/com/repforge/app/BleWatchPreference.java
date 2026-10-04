package com.repforge.app;

import android.content.Context;
import android.content.SharedPreferences;

/** One explicitly chosen watch. Private app storage; never part of diagnostics/backups. */
final class BleWatchPreference {
    private static final String FILE = "ble_selected_watch";
    private final SharedPreferences preferences;
    private final Context context;
    BleWatchPreference(Context context) { this.context=context.getApplicationContext();preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    String address() {
        String value = preferences.getString("address", "");
        return value != null && value.matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}") ? value : "";
    }
    String name() { return preferences.getString("name", ""); }
    String service() { return preferences.getString("service", ""); }
    String journalId() {
        String id = preferences.getString("journal_id", "");
        if (id.isEmpty() && !address().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
            if (!preferences.edit().putString("journal_id", id).commit()) throw new IllegalStateException("Cannot save watch identity");
        }
        return id;
    }
    boolean save(String address, String name, String service) {
        String journalId = address.equalsIgnoreCase(address()) && !journalId().isEmpty() ? journalId() : java.util.UUID.randomUUID().toString();
        return preferences.edit().putString("journal_id", journalId).putString("address", address).putString("name", name)
            .putString("service", service).commit();
    }
    boolean clear() { try{HealthJournalStore.get(context).setPreferences(new org.json.JSONObject().put("bleBackground",false));}catch(Exception ignored){}HealthBackgroundService.stop(context);return preferences.edit().clear().commit(); }
}
