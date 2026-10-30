package com.repforge.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeartRateMeasurementTest {
    @Test public void decodesUnsignedEightAndSixteenBitMeasurements() {
        assertEquals(Integer.valueOf(72), HeartRateMeasurement.parse(new byte[]{0,72}));
        assertEquals(Integer.valueOf(200), HeartRateMeasurement.parse(new byte[]{0,(byte)200}));
        assertEquals(Integer.valueOf(300), HeartRateMeasurement.parse(new byte[]{1,44,1}));
    }
    @Test public void validatesContactAndOptionalEnergyAndRrIntervals() {
        assertNull(HeartRateMeasurement.parse(new byte[]{4,72}));
        assertEquals(Integer.valueOf(72), HeartRateMeasurement.parse(new byte[]{6,72}));
        assertEquals(Integer.valueOf(72), HeartRateMeasurement.parse(new byte[]{24,72,5,0,0,4}));
        assertNull(HeartRateMeasurement.parse(new byte[]{8,72,5}));
        assertNull(HeartRateMeasurement.parse(new byte[]{16,72}));
        assertNull(HeartRateMeasurement.parse(new byte[]{16,72,1}));
    }
    @Test public void rejectsTruncatedZeroAndUnknownLayouts() {
        assertNull(HeartRateMeasurement.parse(null));
        assertNull(HeartRateMeasurement.parse(new byte[]{0}));
        assertNull(HeartRateMeasurement.parse(new byte[]{1,72}));
        assertNull(HeartRateMeasurement.parse(new byte[]{0,0}));
        assertNull(HeartRateMeasurement.parse(new byte[]{32,72}));
        assertNull(HeartRateMeasurement.parse(new byte[]{0,72,1}));
    }
}
