package com.repforge.app;

/**
 * #163: delegates each GPS fix to the existing distance algorithm and an independent,
 * bounded route trace. It does not persist or expose coordinates.
 */
final class GpsRouteCapture {
    private final DistanceAccumulator distance = new DistanceAccumulator();
    private final GpsRouteTrace route = new GpsRouteTrace();

    boolean addFix(double latitude, double longitude, double accuracyMeters,
                   long elapsedMillis, long epochMillis, double speedMps) {
        // Keep the existing distance acceptance algorithm authoritative and unchanged.
        if (!distance.add(latitude, longitude, accuracyMeters, elapsedMillis, speedMps)) return false;
        route.record(latitude, longitude, accuracyMeters, elapsedMillis, epochMillis, speedMps);
        return true;
    }

    void breakSegment() {
        distance.resetAnchor();
        route.breakSegment();
    }

    double totalMeters() { return distance.totalMeters(); }
    int routePointCount() { return route.pointCount(); }
    int routeSegmentCount() { return route.segmentCount(); }
    boolean routeTruncated() { return route.isTruncated(); }

    /** In-memory only; never serialize the points into generic GPS status or WebView events. */
    GpsRouteTrace route() { return route; }
}
