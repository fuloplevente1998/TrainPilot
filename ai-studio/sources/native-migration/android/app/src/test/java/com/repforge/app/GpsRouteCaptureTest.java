package com.repforge.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class GpsRouteCaptureTest {
    private static final long EPOCH = 1700000000000L;
    private static boolean fix(GpsRouteCapture c, double lat, long elapsed) {
        return c.addFix(lat, 19d, 5d, elapsed, EPOCH + elapsed, 3d);
    }

    @Test public void acceptedFixesPreserveOldDistanceAndCollectRoute() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(fix(c, 47d, 1000));
        assertTrue(fix(c, 47.0001d, 6000));
        assertEquals(11.12d, c.totalMeters(), .2d);
        assertEquals(2, c.routePointCount());
        assertEquals(1, c.routeSegmentCount());
    }

    @Test public void badFixCannotInventRouteOrDistance() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(fix(c, 47d, 1000));
        assertFalse(fix(c, 48d, 2000));
        assertFalse(c.addFix(47.1d, 19d, 99d, 3000, EPOCH + 3000, 4d));
        assertEquals(1, c.routePointCount());
        assertEquals(0d, c.totalMeters(), 0d);
        assertTrue(fix(c, 47.0001d, 6000));
        assertEquals(11.12d, c.totalMeters(), .2d);
    }

    @Test public void explicitPausePreventsStraightLineWithoutAddingDistance() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(fix(c, 47d, 1000));
        assertTrue(fix(c, 47.0001d, 6000));
        double before = c.totalMeters();
        c.breakSegment();
        assertTrue(fix(c, 47.02d, 7000));
        assertEquals(before, c.totalMeters(), 0d);
        assertEquals(2, c.routeSegmentCount());
        assertTrue(fix(c, 47.0201d, 12000));
        assertEquals(before + 11.12d, c.totalMeters(), .2d);
    }

    @Test public void signalGapStartsNewSegmentAndKeepsOldDistanceContract() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(fix(c, 47d, 1000));
        assertTrue(fix(c, 47.0001d, 6000));
        double before = c.totalMeters();
        assertTrue(fix(c, 47.02d, 37001));
        assertEquals(before, c.totalMeters(), 0d);
        assertEquals(2, c.routeSegmentCount());
    }

    @Test public void stationaryJitterDoesNotInventRouteOrDistance() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(fix(c, 47d, 1000));
        for (int i = 1; i <= 20; i++) {
            assertTrue(c.addFix(47.00001d, 19d, 8d, 1000 + i * 1000,
                    EPOCH + 1000 + i * 1000, 0d));
        }
        assertEquals(0d, c.totalMeters(), 0d);
        assertEquals(1, c.routePointCount());
    }

    @Test public void anotherWorkoutStartsWithEmptyTraceAndDistance() {
        GpsRouteCapture previous = new GpsRouteCapture();
        assertTrue(fix(previous, 47d, 1000));
        GpsRouteCapture next = new GpsRouteCapture();
        assertEquals(0, next.routePointCount());
        assertEquals(0, next.routeSegmentCount());
        assertEquals(0d, next.totalMeters(), 0d);
        assertEquals(1, previous.routePointCount());
    }

    @Test public void routeStorageIsSeparateFromExistingDistanceTotals() {
        GpsRouteCapture c = new GpsRouteCapture();
        assertTrue(c.addFix(47d, 19d, 5d, 1000, 0, 3d)); // Invalid wall timestamp for route only.
        assertTrue(c.addFix(47.0001d, 19d, 5d, 6000, 0, 3d));
        assertEquals(11.12d, c.totalMeters(), .2d);
        assertEquals(0, c.routePointCount());
    }
}
