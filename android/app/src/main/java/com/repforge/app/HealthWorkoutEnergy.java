package com.repforge.app;

import java.util.*;

/** Raw energy tied to external exercise sessions, never the daily/BMR aggregate.
 * Samsung exports exercise calories as TotalCaloriesBurnedRecord:
 * https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect
 */
final class HealthWorkoutEnergy {
    static final String SAMSUNG = "com.sec.android.app.shealth";
    static final long EDGE_TOLERANCE_MS = 180_000;

    static final class Session {
        final String source;
        final long start, end;
        Session(String source, long start, long end) {
            this.source = source; this.start = start; this.end = end;
        }
    }
    static final class EnergyRow {
        final String id, source;
        final long start, end, modified;
        final double kcal;
        final boolean total;
        EnergyRow(String id, String source, long start, long end, double kcal, boolean total, long modified) {
            this.id = id; this.source = source; this.start = start; this.end = end;
            this.kcal = kcal; this.total = total; this.modified = modified;
        }
    }
    static final class Result {
        Double calories;
        long coverageMs;
        boolean prorated;
        final Set<String> sources = new TreeSet<>();
        final Set<String> types = new TreeSet<>();
        final Set<String> recordIds = new TreeSet<>();
    }

    static Result match(List<Session> sessions, List<EnergyRow> rows, long start, long end, String ownPackage) {
        Result result = new Result();
        if (end <= start) return result;
        List<EnergyRow> candidates = new ArrayList<>();
        Map<String, EnergyRow> unique = new HashMap<>();
        for (EnergyRow row : rows) {
            if (row.source == null || row.source.isEmpty() || row.source.equals(ownPackage)
                    || row.end <= row.start || row.end <= start || row.start >= end
                    || !Double.isFinite(row.kcal) || row.kcal < 0) continue;
            String key = row.source + "|" + row.total + "|" + row.id;
            EnergyRow previous = unique.get(key);
            if (previous == null || previous.modified < row.modified) unique.put(key, row);
        }
        // Reject long daily records even if they overlap an exercise. Energy must
        // belong to the same origin and fit inside one external session.
        for (EnergyRow row : unique.values()) {
            for (Session session : sessions) {
                long overlap = Math.max(0, Math.min(row.end, session.end) - Math.max(row.start, session.start));
                if (row.source.equals(session.source) && session.end > session.start
                        && row.start >= session.start - EDGE_TOLERANCE_MS
                        && row.end <= session.end + EDGE_TOLERANCE_MS
                        && overlap >= (row.end - row.start) * 0.9) {
                    candidates.add(row); break;
                }
            }
        }
        // Partition time to avoid adding duplicates, overlapping summaries, or
        // the same workout mirrored by multiple apps. Total wins over active
        // for a source; finer records win over that source's broad summary.
        TreeSet<Long> boundaries = new TreeSet<>();
        boundaries.add(start); boundaries.add(end);
        for (EnergyRow row : candidates) {
            boundaries.add(Math.max(start, row.start)); boundaries.add(Math.min(end, row.end));
        }
        for (Session session : sessions) if (session.start < end && session.end > start) {
            boundaries.add(Math.max(start, session.start)); boundaries.add(Math.min(end, session.end));
        }
        List<Long> edges = new ArrayList<>(boundaries);
        Map<EnergyRow, Long> used = new HashMap<>();
        Comparator<EnergyRow> priority = Comparator
                .comparing((EnergyRow row) -> !row.source.equals(SAMSUNG))
                .thenComparing(row -> row.source)
                .thenComparing(row -> !row.total)
                .thenComparingLong(row -> row.end - row.start)
                .thenComparing(Comparator.comparingLong((EnergyRow row) -> row.modified).reversed())
                .thenComparing(row -> row.id);
        double calories = 0;
        for (int i = 1; i < edges.size(); i++) {
            long a = edges.get(i - 1), b = edges.get(i);
            EnergyRow best = null;
            for (EnergyRow row : candidates) {
                if (row.start > a || row.end < b) continue;
                boolean inSession = false;
                for (Session session : sessions) if (row.source.equals(session.source)
                        && session.start <= a && session.end >= b) { inSession = true; break; }
                if (inSession && (best == null || priority.compare(row, best) < 0)) best = row;
            }
            if (best == null) continue;
            calories += best.kcal * ((double) (b - a) / (best.end - best.start));
            result.coverageMs += b - a;
            result.sources.add(best.source);
            result.types.add(best.total ? "total" : "active");
            result.recordIds.add(best.source + "|" + best.id);
            used.put(best, used.getOrDefault(best, 0L) + b - a);
        }
        if (!used.isEmpty()) result.calories = calories;
        for (Map.Entry<EnergyRow, Long> use : used.entrySet())
            if (use.getValue() < use.getKey().end - use.getKey().start) result.prorated = true;
        return result;
    }
    private HealthWorkoutEnergy() {}
}
