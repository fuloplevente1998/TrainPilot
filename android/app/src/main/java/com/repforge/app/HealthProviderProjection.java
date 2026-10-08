package com.repforge.app;

import org.json.JSONObject;

/** Select values by provider, never sum two copies of the same health data. */
final class HealthProviderProjection {
    static final String SAMSUNG = "samsung_health";
    static final String[] METRICS = {"steps", "activeCalories", "totalCalories", "distanceMeters",
        "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples", "restingHeartRate",
        "weightKg", "bodyFatPercent", "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg",
        "totalBodyWaterLiters", "basalMetabolicRateKcal", "bodyMassIndex", "oxygenSaturationPercent",
        "hrvRmssdMs", "vo2Max", "bloodPressureSystolic", "bloodPressureDiastolic",
        "bloodGlucoseMmolL", "respiratoryRate", "energyScore", "sleepScore"};

    static JSONObject day(JSONObject hc, JSONObject samsung, JSONObject legacy, String preferred) throws Exception {
        JSONObject fallback = hc != null ? hc : legacy;
        boolean direct = SAMSUNG.equals(preferred) && samsung != null;
        JSONObject base = direct ? samsung : fallback;
        JSONObject out = base == null ? new JSONObject() : new JSONObject(base.toString());
        String channel = direct ? SAMSUNG : hc != null ? HealthJournalStore.HC : HealthJournalStore.LEGACY;
        JSONObject sources = new JSONObject();
        for (String key : METRICS) {
            if (!out.isNull(key)) sources.put(key, channel);
            else if (direct && fallback != null && !fallback.isNull(key)) {
                out.put(key, fallback.get(key));
                sources.put(key, hc != null ? HealthJournalStore.HC : HealthJournalStore.LEGACY);
                // Keep measurement timestamps and underlying origin together with a fallback value.
                String prefix = key.equals("weightKg") ? "weight" : key.equals("bodyFatPercent") ? "bodyFat"
                    : key.equals("oxygenSaturationPercent") ? "oxygenSaturation" : key.equals("hrvRmssdMs") ? "hrv" : key;
                for (String suffix : new String[]{"Time", "Source"})
                    if (fallback.has(prefix + suffix)) out.put(prefix + suffix, fallback.get(prefix + suffix));
            }
        }
        if (direct && (out.optJSONArray("sleepSessions") == null || out.getJSONArray("sleepSessions").length() == 0)
                && fallback != null && fallback.optJSONArray("sleepSessions") != null) {
            out.put("sleepSessions", fallback.getJSONArray("sleepSessions"));
            sources.put("sleepSessions", hc != null ? HealthJournalStore.HC : HealthJournalStore.LEGACY);
        } else if (out.optJSONArray("sleepSessions") != null && out.getJSONArray("sleepSessions").length() > 0)
            sources.put("sleepSessions", channel);
        out.put("channel", channel);
        out.put("metricProviders", sources);
        if (!out.isNull("steps")) out.put("stepsSource", sources.optString("steps", channel));
        return out;
    }
    private HealthProviderProjection() {}
}
