package com.repforge.app;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class GpsRouteTraceTest {
    private static final long EPOCH = 1700000000000L;
    private static boolean add(GpsRouteTrace route, double lat, long elapsed) {
        return route.record(lat, 19d, 5d, elapsed, EPOCH + elapsed, 3d);
    }

    @Test public void keepsValidatedPointsAndRealTimestamps() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        assertTrue(add(route, 47.0001d, 6000));
        List<GpsRouteTrace.Segment> sections = route.snapshot();
        assertEquals(1, sections.size());
        assertEquals(1, sections.get(0).ordinal);
        assertEquals(2, sections.get(0).points.size());
        assertEquals(EPOCH + 6000, sections.get(0).points.get(1).epochMillis);
        assertEquals(5d, sections.get(0).points.get(0).accuracyMeters, 0d);
    }

    @Test public void pauseNeverDrawsAConnectionAcrossTheBreak() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        route.breakSegment();
        assertTrue(add(route, 47.02d, 2000));
        assertEquals(2, route.snapshot().size());
        assertEquals(1, route.snapshot().get(0).points.size());
        assertEquals(1, route.snapshot().get(1).points.size());
    }

    @Test public void lostGpsSignalBeginsAnotherSection() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        assertTrue(add(route, 47.0001d, 6000));
        assertTrue(add(route, 47.02d, 37001));
        assertEquals(2, route.snapshot().size());
        assertEquals(3, route.pointCount());
    }

    @Test public void rejectsInvalidLocationsTimeRewindsAndTeleportation() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        assertFalse(route.record(91d, 19d, 5d, 2000, EPOCH + 2000, 3d));
        assertFalse(route.record(Double.NaN, 19d, 5d, 2000, EPOCH + 2000, 3d));
        assertFalse(route.record(47.0001d, 19d, 50d, 2000, EPOCH + 2000, 3d));
        assertFalse(add(route, 47.0001d, 1000));
        assertFalse(add(route, 48d, 2000)); // Impossible one-second jump.
        assertTrue(add(route, 47.02d, 3000));
        assertEquals(2, route.snapshot().size());
        assertEquals(2, route.pointCount());
    }

    @Test public void rejectsStationaryJitterWithoutInventingDistance() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        for (int i = 1; i <= 20; i++) {
            assertFalse(route.record(47.00001d, 19d, 8d, 1000L + i * 1000L,
                    EPOCH + 1000L + i * 1000L, 0d));
        }
        assertEquals(1, route.pointCount());
        assertEquals(1, route.snapshot().size());
    }

    @Test public void snapshotsAreImmutableAndNotLive() {
        GpsRouteTrace route = new GpsRouteTrace();
        assertTrue(add(route, 47d, 1000));
        List<GpsRouteTrace.Segment> original = route.snapshot();
        assertTrue(add(route, 47.0001d, 6000));
        assertEquals(1, original.get(0).points.size());
        assertEquals(2, route.snapshot().get(0).points.size());
        try { original.clear(); fail("snapshot list must be immutable"); }
        catch (UnsupportedOperationException expected) { /* Expected. */ }
        try { original.get(0).points.clear(); fail("segment points must be immutable"); }
        catch (UnsupportedOperationException expected) { /* Expected. */ }
    }

    @Test public void pointBudgetMarksTheTrackAsPartial() {
        GpsRouteTrace route = new GpsRouteTrace();
        for (int i = 0; i < GpsRouteTrace.MAX_POINTS; i++) {
            assertTrue("point " + i, route.record(47d, 19d + i * .0001d, 5d,
                    1000L + i * 5000L, EPOCH + i * 5000L, 3d));
        }
        assertFalse(route.record(47d, 19d + GpsRouteTrace.MAX_POINTS * .0001d, 5d,
                1000L + GpsRouteTrace.MAX_POINTS * 5000L,
                EPOCH + GpsRouteTrace.MAX_POINTS * 5000L, 3d));
        assertTrue(route.isTruncated());
        assertEquals(GpsRouteTrace.MAX_POINTS, route.pointCount());
    }

    @Test public void segmentBudgetMarksTheTrackAsPartial() {
        GpsRouteTrace route = new GpsRouteTrace();
        for (int i = 0; i < GpsRouteTrace.MAX_SEGMENTS; i++) {
            route.breakSegment();
            assertTrue(add(route, 47d + i * .0001d, 1000L + i * 1000L));
        }
        route.breakSegment();
        assertFalse(add(route, 47.1d, 500000));
        assertTrue(route.isTruncated());
        assertEquals(GpsRouteTrace.MAX_SEGMENTS, route.snapshot().size());
    }
}
