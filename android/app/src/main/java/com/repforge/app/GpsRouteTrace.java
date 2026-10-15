package com.repforge.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** #163: bounded, in-memory polyline segments. No coordinates persist until explicit route storage. */
final class GpsRouteTrace {
    static final int MAX_POINTS = 12000;
    static final int MAX_SEGMENTS = 256;
    private static final long MAX_GAP_MS = 30000L;
    private static final double MAX_ACCURACY_METERS = 35d;
    private static final double MAX_SPEED_MPS = 20d;

    static final class Point {
        final double latitude, longitude, accuracyMeters;
        final long elapsedMillis, epochMillis;
        Point(double latitude, double longitude, double accuracyMeters, long elapsedMillis, long epochMillis) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracyMeters = accuracyMeters;
            this.elapsedMillis = elapsedMillis;
            this.epochMillis = epochMillis;
        }
    }

    static final class Segment {
        final int ordinal;
        final List<Point> points;
        Segment(int ordinal, List<Point> points) {
            this.ordinal = ordinal;
            this.points = Collections.unmodifiableList(new ArrayList<>(points));
        }
    }

    private final ArrayList<ArrayList<Point>> segments = new ArrayList<>();
    private Point anchor;
    private int pointCount;
    private boolean truncated;

    /** Explicit pause, stop or lost owner: never draw a straight line across the break. */
    void breakSegment() { anchor = null; }

    /**
     * Accept only plausible, moving fixes. Elapsed time is monotonic; epoch time is for the log.
     * The existing DistanceAccumulator remains authoritative for measured distance.
     */
    boolean record(double latitude, double longitude, double accuracyMeters, long elapsedMillis,
                   long epochMillis, double speedMps) {
        if (truncated || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180
                || !Double.isFinite(accuracyMeters) || accuracyMeters < 0
                || accuracyMeters > MAX_ACCURACY_METERS || elapsedMillis < 0 || epochMillis <= 0
                || Double.isFinite(speedMps) && (speedMps < 0 || speedMps > MAX_SPEED_MPS)) {
            return false;
        }
        if (anchor != null && elapsedMillis <= anchor.elapsedMillis) return false;
        if (anchor != null && elapsedMillis - anchor.elapsedMillis > MAX_GAP_MS) breakSegment();
        if (anchor != null) {
            double meters = DistanceAccumulator.distance(anchor.latitude, anchor.longitude, latitude, longitude);
            double seconds = (elapsedMillis - anchor.elapsedMillis) / 1000d;
            if (meters / seconds > MAX_SPEED_MPS) {
                breakSegment(); // Discard jump and require a new, independently anchored section.
                return false;
            }
            double minimum = Math.max(3d, Math.min(15d, (anchor.accuracyMeters + accuracyMeters) * .25d));
            if (meters < minimum || Double.isFinite(speedMps) && speedMps < .5d) return false;
        }
        if (pointCount >= MAX_POINTS || anchor == null && segments.size() >= MAX_SEGMENTS) {
            truncated = true;
            return false;
        }
        if (anchor == null) segments.add(new ArrayList<>());
        Point point = new Point(latitude, longitude, accuracyMeters, elapsedMillis, epochMillis);
        segments.get(segments.size() - 1).add(point);
        anchor = point;
        pointCount++;
        return true;
    }

    int pointCount() { return pointCount; }
    int segmentCount() { return segments.size(); }
    Point lastPoint() { return anchor; }
    boolean isTruncated() { return truncated; }

    /** Immutable defensive copies; later GPS callbacks cannot mutate a saved snapshot. */
    List<Segment> snapshot() {
        ArrayList<Segment> copy = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) copy.add(new Segment(i + 1, segments.get(i)));
        return Collections.unmodifiableList(copy);
    }
}
