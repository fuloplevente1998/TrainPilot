package com.repforge.app;

// Native SDK interoperability adapter: reader preserved from the verified 1.3.71
// SamsungHealthPlugin at 60fe647a. No Capacitor dependency or WebView interface.
import com.samsung.android.sdk.health.data.HealthDataService;
import com.samsung.android.sdk.health.data.HealthDataStore;
import com.samsung.android.sdk.health.data.data.*;
import com.samsung.android.sdk.health.data.data.entries.*;
import com.samsung.android.sdk.health.data.permission.AccessType;
import com.samsung.android.sdk.health.data.permission.Permission;
import com.samsung.android.sdk.health.data.request.*;
import com.samsung.android.sdk.health.data.response.*;
import com.samsung.android.sdk.health.data.error.HealthDataException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import android.content.Context;

final class SamsungSdkReader {
    static final String SOURCE = "com.sec.android.app.shealth";
    private static LinkedHashMap<String, DataType> types() {
        LinkedHashMap<String, DataType> types = new LinkedHashMap<>();
        types.put("READ_STEPS", DataTypes.STEPS);
        types.put("READ_ACTIVE_CALORIES_BURNED", DataTypes.ACTIVITY_SUMMARY);
        types.put("READ_TOTAL_CALORIES_BURNED", DataTypes.ACTIVITY_SUMMARY);
        types.put("READ_DISTANCE", DataTypes.ACTIVITY_SUMMARY);
        types.put("READ_EXERCISE", DataTypes.EXERCISE);
        types.put("READ_HEART_RATE", DataTypes.HEART_RATE);
        types.put("READ_SLEEP", DataTypes.SLEEP);
        types.put("READ_WEIGHT", DataTypes.BODY_COMPOSITION);
        types.put("READ_BODY_FAT", DataTypes.BODY_COMPOSITION);
        types.put("READ_OXYGEN_SATURATION", DataTypes.BLOOD_OXYGEN);
        types.put("READ_ENERGY_SCORE", DataTypes.ENERGY_SCORE);
        return types;
    }

    private static Set<Permission> requested() {
        Set<Permission> result = new LinkedHashSet<>();
        for (DataType type : types().values()) result.add(Permission.of(type, AccessType.READ));
        return result;
    }

    private static JSONObject status(Set<Permission> granted) throws org.json.JSONException {
        JSONObject permissions = new JSONObject();
        for (Map.Entry<String, DataType> entry : types().entrySet())
            permissions.put(entry.getKey(), granted.contains(Permission.of(entry.getValue(), AccessType.READ)));
        permissions.put("READ_HEART_RATE_VARIABILITY", false);
        permissions.put("WRITE_EXERCISE", false);
        JSONObject out = new JSONObject();
        out.put("provider", HealthJournalStore.SAMSUNG);
        out.put("permissions", permissions);
        out.put("granted", !granted.isEmpty());
        out.put("hrvSupported", false);
        out.put("backgroundSupported", false);
        return out;
    }

    static Set<Permission> energyPermission() {
        return Collections.singleton(Permission.of(DataTypes.ENERGY_SCORE, AccessType.READ));
    }

    static String errorCode(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause())
            if (cause instanceof HealthDataException)
                return "SAMSUNG_HEALTH_" + ((HealthDataException) cause).getErrorCode();
        return "SAMSUNG_HEALTH_UNAVAILABLE";
    }
    static Reader open(Context context, Runnable foreground) throws Exception {
        foreground.run();
        HealthDataStore store = HealthDataService.getStore(context);
        Set<Permission> granted = store.getGrantedPermissionsAsync(requested()).get(30, TimeUnit.SECONDS);
        foreground.run();
        return new Reader(store, granted, foreground);
    }
    static Set<Permission> permissions() { return requested(); }
    static final class Reader {
        final HealthDataStore store;
        final Set<Permission> granted;
        final Runnable requireForeground;
        final JSONArray warnings = new JSONArray(), failed = new JSONArray(), journal = new JSONArray();
        final JSONObject errors = new JSONObject();
        Reader(HealthDataStore store, Set<Permission> granted, Runnable requireForeground) {
            this.store = store; this.granted = granted; this.requireForeground = requireForeground;
        }
        boolean allowed(DataType type) { return granted.contains(Permission.of(type, AccessType.READ)); }
        // One SDK-type mapping shared by status, reads and Kotlin sync scope checks.
        Set<String> grantedKeys() {
            Set<String> keys = new LinkedHashSet<>();
            for (Map.Entry<String, DataType> entry : types().entrySet()) {
                if (allowed(entry.getValue())) keys.add(entry.getKey());
            }
            return keys;
        }
        private interface Read { void run() throws Exception; }
        private void safe(DataType type, Read action, String... fields) throws Exception {
            requireForeground.run();
            if (!allowed(type)) return;
            try { action.run(); }
            catch (Exception error) {
                warnings.put(type.getName() + ": " + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
                for (String field : fields) { failed.put(field); errors.put(field, errorCode(error)); }
            }
            requireForeground.run();
        }
        private List<HealthDataPoint> points(java.util.function.Supplier<ReadDataRequest.DualTimeBuilder<HealthDataPoint>> factory,
                                             Instant start, Instant end) throws Exception {
            Map<String, HealthDataPoint> unique = new LinkedHashMap<>();
            String token = null;
            Set<String> tokens = new HashSet<>();
            for (int page = 0; page < 30; page++) {
                requireForeground.run();
                ReadDataRequest.DualTimeBuilder<HealthDataPoint> builder = factory.get().setPageSize(500)
                    .setInstantTimeFilter(InstantTimeFilter.of(start, end, true, false));
                if (token != null) builder.setPageToken(token);
                DataResponse<HealthDataPoint> response = store.readDataAsync(builder.build()).get(30, TimeUnit.SECONDS);
                for (HealthDataPoint point : response.getDataList()) unique.put(point.getUid(), point);
                token = response.getPageToken();
                if (token == null || token.isEmpty()) return new ArrayList<>(unique.values());
                if (!tokens.add(token)) throw new IllegalStateException("Repeated Samsung Health page token.");
            }
            throw new IllegalStateException("Too many Samsung Health record pages.");
        }
        private <T> T aggregate(AggregateOperation<T, AggregateRequest.LocalTimeBuilder<T>> operation,
                                 Instant start, Instant end) throws Exception {
            ZoneId zone = ZoneId.systemDefault();
            AggregateRequest<T> request = operation.getRequestBuilder().setLocalTimeFilter(LocalTimeFilter.of(
                LocalDateTime.ofInstant(start, zone), LocalDateTime.ofInstant(end, zone), true, false)).build();
            DataResponse<AggregatedData<T>> response = store.aggregateDataAsync(request).get(30, TimeUnit.SECONDS);
            // No grouping requested: there must be a single aggregate, never add overlapping buckets.
            if (response.getPageToken() != null && !response.getPageToken().isEmpty()
                    || response.getDataList().size() > 1) throw new IllegalStateException("Unexpected Samsung aggregate grouping.");
            return response.getDataList().isEmpty() ? null : response.getDataList().get(0).getValue();
        }
        private JSONObject base(Instant start, Instant end) throws Exception {
            JSONObject out = status(granted);
            out.put("source", SOURCE);
            out.put("sources", new JSONArray().put(SOURCE));
            out.put("sourceLabels", new JSONObject().put(SOURCE, "Samsung Health · direct"));
            out.put("start", start.toString()); out.put("end", end.toString());
            out.put("readAt", Instant.now().toString());
            out.put("warnings", warnings); out.put("failedFields", failed); out.put("errors", errors);
            return out;
        }
        private static String source(HealthDataPoint p) {
            return p.getDataSource() == null || p.getDataSource().getAppId() == null ? SOURCE : p.getDataSource().getAppId();
        }
        private static void number(JSONObject out, String key, Number value) throws Exception {
            out.put(key, value != null && Double.isFinite(value.doubleValue()) && value.doubleValue() >= 0 ? value : JSONObject.NULL);
        }
        private static Instant end(HealthDataPoint p) {
            if (p.getEndTime() != null) return p.getEndTime();
            if (p.getStartTime() != null) return p.getStartTime();
            if (p.getUpdateTime() != null) return p.getUpdateTime();
            return Instant.now();
        }
        private void record(HealthDataPoint p, String type, String suffix, JSONObject data, Instant a, Instant b) throws Exception {
            journal.put(new JSONObject().put("channel", HealthJournalStore.SAMSUNG).put("source", source(p))
                .put("type", type).put("recordId", p.getUid() + suffix)
                .put("lastModifiedMs", (p.getUpdateTime() == null ? a : p.getUpdateTime()).toEpochMilli())
                .put("startMs", a.toEpochMilli()).put("endMs", b.toEpochMilli())
                .put("day", HealthJournalStore.day((type.equals("SleepSessionRecord") ? b : a).toEpochMilli())).put("data", data));
        }
        private void activity(JSONObject out, Instant start, Instant end) throws Exception {
            safe(DataTypes.STEPS, () -> number(out, "steps", aggregate(DataType.StepsType.TOTAL, start, end)), "steps");
            safe(DataTypes.ACTIVITY_SUMMARY, () -> number(out, "activeCalories", aggregate(DataType.ActivitySummaryType.TOTAL_ACTIVE_CALORIES_BURNED, start, end)), "activeCalories");
            safe(DataTypes.ACTIVITY_SUMMARY, () -> number(out, "totalCalories", aggregate(DataType.ActivitySummaryType.TOTAL_CALORIES_BURNED, start, end)), "totalCalories");
            safe(DataTypes.ACTIVITY_SUMMARY, () -> number(out, "distanceMeters", aggregate(DataType.ActivitySummaryType.TOTAL_DISTANCE, start, end)), "distanceMeters");
            out.put("activityOrigin", SOURCE);
        }
        private void heart(JSONObject out, Instant start, Instant end) throws Exception {
            safe(DataTypes.HEART_RATE, () -> {
                // Deduplicate series by origin + sample interval, including overlapping parent records.
                Map<String, double[]> samples = new LinkedHashMap<>();
                for (HealthDataPoint p : points(DataTypes.HEART_RATE::getReadDataRequestBuilder, start, end)) {
                    List<HeartRate> series = p.getValue(DataType.HeartRateType.SERIES_DATA);
                    if (series != null && !series.isEmpty()) {
                        for (HeartRate h : series) if (h.getStartTime().isBefore(end) && !h.getEndTime().isBefore(start))
                            samples.put(source(p) + h.getStartTime() + "|" + h.getEndTime(), new double[]{h.getHeartRate(), h.getMin(), h.getMax()});
                    } else if (!p.getStartTime().isBefore(start) && p.getStartTime().isBefore(end)) {
                        Float value = p.getValue(DataType.HeartRateType.HEART_RATE);
                        if (value != null) samples.put(source(p) + p.getStartTime(), new double[]{value,
                            p.getValueOrDefault(DataType.HeartRateType.MIN_HEART_RATE, value), p.getValueOrDefault(DataType.HeartRateType.MAX_HEART_RATE, value)});
                    }
                }
                double sum = 0, min = Double.POSITIVE_INFINITY, max = 0; int count = 0;
                for (double[] h : samples.values()) if (Double.isFinite(h[0]) && h[0] > 0) {
                    sum += h[0]; count++; min = Math.min(min, h[1] > 0 && Double.isFinite(h[1]) ? h[1] : h[0]);
                    max = Math.max(max, h[2] > 0 && Double.isFinite(h[2]) ? h[2] : h[0]);
                }
                number(out, "averageHeartRate", count == 0 ? null : sum / count);
                number(out, "minHeartRate", count == 0 ? null : min); number(out, "maxHeartRate", count == 0 ? null : max);
                out.put("heartRateSamples", count);
            }, "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples");
        }
        private <T extends Number> void latest(JSONObject out, List<HealthDataPoint> points, Field<T> field,
                                               String key, String prefix, String unit, String recordType) throws Exception {
            HealthDataPoint newest = null; T value = null;
            for (HealthDataPoint p : points) {
                T candidate = p.getValue(field);
                if (candidate == null || !Double.isFinite(candidate.doubleValue()) || candidate.doubleValue() < 0) continue;
                Instant pTime = p.getStartTime() != null ? p.getStartTime() : p.getUpdateTime();
                if (pTime == null) continue; // Never fabricate a fresh sample timestamp
                Instant newestTime = newest == null ? null : (newest.getStartTime() != null ? newest.getStartTime() : newest.getUpdateTime());
                if (newest == null || pTime.isAfter(newestTime)) { newest = p; value = candidate; }
                record(p, recordType, ":" + key, new JSONObject().put("value", candidate).put("unit", unit), pTime, end(p));
            }
            number(out, key, value);
            if (newest != null) {
                Instant time = newest.getStartTime() != null ? newest.getStartTime() : newest.getUpdateTime();
                out.put(prefix + "Time", time != null ? time.toString() : Instant.now().toString());
                out.put(prefix + "Source", source(newest));
            }
        }

        private void energyScore(JSONObject out, Instant start, Instant end) throws Exception {
            out.put("energyScore", JSONObject.NULL);
            safe(DataTypes.ENERGY_SCORE, () -> {
                ZoneId zone = ZoneId.systemDefault();
                LocalDate startDate = start.atZone(zone).toLocalDate();
                // LocalDateFilter becomes a midnight-based LocalTimeFilter in
                // the SDK. Equal dates produce a zero-length query (1001).
                // Use a positive [first day, day after last) interval, including
                // on 23/25-hour DST days; never include tomorrow's midnight.
                LocalDate endDateExclusive = end.minusNanos(1).atZone(zone).toLocalDate().plusDays(1);
                Map<String, HealthDataPoint> unique = new LinkedHashMap<>();
                String token = null;
                Set<String> tokens = new HashSet<>();
                for (int page = 0; page < 30; page++) {
                    requireForeground.run();
                    ReadDataRequest.LocalDateBuilder<HealthDataPoint> builder = DataTypes.ENERGY_SCORE.getReadDataRequestBuilder()
                        .setPageSize(500)
                        .setLocalDateFilter(LocalDateFilter.of(startDate, endDateExclusive, true, false));
                    if (token != null) builder.setPageToken(token);
                    DataResponse<HealthDataPoint> response = store.readDataAsync(builder.build()).get(30, TimeUnit.SECONDS);
                    for (HealthDataPoint point : response.getDataList()) {
                        Number value = point.getValue(DataType.EnergyScoreType.ENERGY_SCORE);
                        Instant time = point.getStartTime();
                        if (value == null || !Double.isFinite(value.doubleValue()) || value.doubleValue() <= 0 || value.doubleValue() > 100 || time == null) continue;
                        LocalDate day = time.atZone(zone).toLocalDate();
                        if (day.isBefore(startDate) || !day.isBefore(endDateExclusive)) continue;
                        unique.put(point.getUid(), point);
                    }
                    token = response.getPageToken();
                    if (token == null || token.isEmpty()) break;
                    if (!tokens.add(token)) throw new IllegalStateException("Repeated Samsung Health page token.");
                }
                latest(out, new ArrayList<>(unique.values()), DataType.EnergyScoreType.ENERGY_SCORE, "energyScore", "energyScore", "", "EnergyScoreRecord");
            }, "energyScore");
        }
        JSONObject energyDay(Instant start, Instant end) throws Exception {
            JSONObject out = base(start, end);
            energyScore(out, start, end);
            return out;
        }
        JSONObject wellness(Instant start, Instant end) throws Exception {
            JSONObject out = base(start, end);
            safe(DataTypes.BODY_COMPOSITION, () -> {
                List<HealthDataPoint> rows = points(DataTypes.BODY_COMPOSITION::getReadDataRequestBuilder, start, end);
                latest(out, rows, DataType.BodyCompositionType.WEIGHT, "weightKg", "weight", "kg", "WeightRecord");
                latest(out, rows, DataType.BodyCompositionType.BODY_FAT, "bodyFatPercent", "bodyFat", "%", "BodyFatRecord");
                latest(out, rows, DataType.BodyCompositionType.BODY_FAT_MASS, "bodyFatMassKg", "bodyFatMass", "kg", "BodyFatMass");
                latest(out, rows, DataType.BodyCompositionType.SKELETAL_MUSCLE_MASS, "skeletalMuscleMassKg", "skeletalMuscleMass", "kg", "SkeletalMuscleMass");
                latest(out, rows, DataType.BodyCompositionType.FAT_FREE_MASS, "fatFreeMassKg", "fatFreeMass", "kg", "FatFreeMass");
                latest(out, rows, DataType.BodyCompositionType.TOTAL_BODY_WATER, "totalBodyWaterLiters", "totalBodyWater", "L", "TotalBodyWater");
                latest(out, rows, DataType.BodyCompositionType.BASAL_METABOLIC_RATE, "basalMetabolicRateKcal", "basalMetabolicRate", "kcal/day", "BasalMetabolicRate");
                latest(out, rows, DataType.BodyCompositionType.BODY_MASS_INDEX, "bodyMassIndex", "bodyMassIndex", "", "BodyMassIndex");
            }, "weightKg", "weightTime", "weightSource", "bodyFatPercent", "bodyFatTime", "bodyFatSource",
                "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg", "totalBodyWaterLiters", "basalMetabolicRateKcal", "bodyMassIndex");
            safe(DataTypes.BLOOD_OXYGEN, () -> latest(out, points(DataTypes.BLOOD_OXYGEN::getReadDataRequestBuilder, start, end),
                DataType.BloodOxygenType.OXYGEN_SATURATION, "oxygenSaturationPercent", "oxygenSaturation", "%", "OxygenSaturationRecord"),
                "oxygenSaturationPercent", "oxygenSaturationTime", "oxygenSaturationSource");
            return out;
        }
        private void sleep(JSONObject out, Instant start, Instant end) throws Exception {
            safe(DataTypes.SLEEP, () -> {
                JSONArray sessions = new JSONArray(); Set<String> seen = new HashSet<>();
                for (HealthDataPoint p : points(DataTypes.SLEEP::getReadDataRequestBuilder, start.minus(Duration.ofDays(2)), end)) {
                    List<SleepSession> rows = p.getValue(DataType.SleepType.SESSIONS);
                    if (rows == null) continue;
                    for (SleepSession s : rows) {
                        // Sleep belongs to its wake-up day, even when it started yesterday.
                        if (s.getEndTime().isBefore(start) || !s.getEndTime().isBefore(end)) continue;
                        String id = source(p) + "|" + s.getStartTime() + "|" + s.getEndTime(); if (!seen.add(id)) continue;
                        JSONArray stages = new JSONArray();
                        for (SleepSession.SleepStage stage : s.getStages() == null ? Collections.<SleepSession.SleepStage>emptyList() : s.getStages()) {
                            boolean asleep = stage.getStage() == DataType.SleepType.StageType.LIGHT
                                || stage.getStage() == DataType.SleepType.StageType.DEEP || stage.getStage() == DataType.SleepType.StageType.REM;
                            stages.put(new JSONObject().put("start", stage.getStartTime().toString()).put("end", stage.getEndTime().toString())
                                .put("stage", stage.getStage().name()).put("isAsleep", asleep));
                        }
                        JSONObject value = new JSONObject().put("id", "samsung:" + p.getUid() + ":" + s.getStartTime())
                            .put("provider", HealthJournalStore.SAMSUNG)
                            .put("source", source(p)).put("start", s.getStartTime().toString()).put("end", s.getEndTime().toString())
                            .put("durationMinutes", s.getDuration().toMillis() / 60000.0).put("stages", stages);
                        sessions.put(value);
                        record(p, "SleepSessionRecord", ":" + s.getStartTime(), value, s.getStartTime(), s.getEndTime());
                    }
                }
                out.put("sleepSessions", sessions);
            }, "sleepSessions");
        }
        JSONObject stepsWindow(Instant start, Instant end) throws Exception {
            JSONObject out = base(start, end);
            out.put("provider", HealthJournalStore.SAMSUNG);
            out.put("steps", JSONObject.NULL);
            safe(DataTypes.STEPS, () -> number(out, "steps", aggregate(DataType.StepsType.TOTAL, start, end)), "steps");
            return out;
        }
        JSONObject daily(Instant start, Instant end) throws Exception {
            JSONObject out = wellness(start, end); activity(out, start, end); heart(out, start, end); sleep(out, start, end); energyScore(out, start, end);
            // No HRV/RMSSD field exists in Samsung's 1.1.0 Data SDK. Never derive one from HR or Energy Score.
            out.put("hrvRmssdMs", JSONObject.NULL);
            return out;
        }
        JSONObject workout(Instant start, Instant end) throws Exception {
            JSONObject out = base(start, end); heart(out, start, end);
            out.put("workoutEnergyVersion", 1); out.put("workoutEnergyProvider", HealthJournalStore.SAMSUNG);
            out.put("workoutCalories", JSONObject.NULL);
            safe(DataTypes.EXERCISE, () -> {
                List<HealthWorkoutEnergy.Session> spans = new ArrayList<>(); List<HealthWorkoutEnergy.EnergyRow> energy = new ArrayList<>();
                JSONArray sessions = new JSONArray(); Set<String> seen = new HashSet<>();
                for (HealthDataPoint p : points(DataTypes.EXERCISE::getReadDataRequestBuilder, start.minus(Duration.ofDays(1)), end)) {
                    if (source(p).equals("com.repforge.app") || p.getClientDataId() != null && p.getClientDataId().startsWith("trainpilot:")) continue;
                    List<ExerciseSession> rows = p.getValue(DataType.ExerciseType.SESSIONS); if (rows == null) continue;
                    for (ExerciseSession s : rows) {
                        long a = s.getStartTime().toEpochMilli(), b = s.getEndTime().toEpochMilli();
                        if (a >= end.toEpochMilli() || b <= start.toEpochMilli() || b <= a) continue;
                        String id = source(p) + "|" + a + "|" + b; if (!seen.add(id)) continue;
                        spans.add(new HealthWorkoutEnergy.Session(source(p), a, b));
                        energy.add(new HealthWorkoutEnergy.EnergyRow(id, source(p), a, b, s.getCalories(), true,
                            p.getUpdateTime() == null ? a : p.getUpdateTime().toEpochMilli()));
                        JSONObject session = new JSONObject().put("id", p.getUid()).put("source", source(p)).put("ownExport", false)
                            .put("start", s.getStartTime().toString()).put("end", s.getEndTime().toString()).put("title", s.getCustomTitle());
                        sessions.put(session);
                    }
                }
                HealthWorkoutEnergy.Result result = HealthWorkoutEnergy.match(spans, energy, start.toEpochMilli(), end.toEpochMilli(), "com.repforge.app");
                out.put("workoutCalories", result.calories == null ? JSONObject.NULL : result.calories);
                out.put("workoutEnergyVersion", 1); out.put("workoutEnergyProvider", HealthJournalStore.SAMSUNG);
                out.put("workoutEnergyCoverageMs", result.coverageMs); out.put("workoutEnergyProrated", result.prorated);
                out.put("workoutEnergySources", new JSONArray(result.sources)); out.put("workoutEnergyRecordIds", new JSONArray(result.recordIds));
                out.put("workoutEnergyTypes", new JSONArray().put("samsung_exercise_session"));
                out.put("workoutEnergySourceLabels", new JSONObject().put(SOURCE, "Samsung Health · direct"));
                out.put("exerciseSessions", sessions); out.put("exerciseSessionCount", sessions.length());
                TreeSet<Long> edges = new TreeSet<>(); edges.add(start.toEpochMilli()); edges.add(end.toEpochMilli());
                for (HealthWorkoutEnergy.Session s : spans) { edges.add(Math.max(start.toEpochMilli(), s.start)); edges.add(Math.min(end.toEpochMilli(), s.end)); }
                List<Long> sorted = new ArrayList<>(edges); long duration = 0;
                for (int i = 1; i < sorted.size(); i++) for (HealthWorkoutEnergy.Session s : spans)
                    if (s.start <= sorted.get(i - 1) && s.end >= sorted.get(i)) { duration += sorted.get(i) - sorted.get(i - 1); break; }
                out.put("exerciseMinutes", duration / 60000.0);
            }, "workoutCalories", "exerciseSessions", "exerciseSessionCount", "exerciseMinutes");
            return out;
        }
    }
}
