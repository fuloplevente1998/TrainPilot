package com.repforge.app;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** Synthetic vectors derived from the traced transport, not physical watch response captures. */
public class RdfitProtocolTest {
    private static byte[] hex(String text) {
        String[] pairs = text.split(" "); byte[] result = new byte[pairs.length];
        for (int i = 0; i < pairs.length; i++) result[i] = (byte) Integer.parseInt(pairs[i], 16);
        return result;
    }
    private static final byte[] BATTERY = hex("ed 40 00 23 00 04 04 0b 4d 00");
    private static final byte[] STEPS = hex("ed 40 00 af 00 0e 0a 0b 00 00 04 d2 00 00 04 d2 00 00 00 7b");
    @Test public void requestAllowlistUsesIndependentGoldenFrames() {
        assertArrayEquals(hex("ed 40 00 33 00 02 04 0b"), RdfitProtocol.request(RdfitProtocol.BATTERY));
        assertArrayEquals(hex("ed 40 00 e7 00 02 0a 0b"), RdfitProtocol.request(RdfitProtocol.STEPS));
        try { RdfitProtocol.request(2); fail("settings commands must be unavailable"); } catch (IllegalArgumentException expected) { }
        try { RdfitProtocol.request(1); fail("history commands must be unavailable"); } catch (IllegalArgumentException expected) { }
    }
    @Test public void readsBatteryAcrossEveryNotificationSplit() {
        for (int split = 1; split < BATTERY.length; split++) {
            RdfitProtocol decoder = new RdfitProtocol();
            assertTrue(decoder.accept(Arrays.copyOfRange(BATTERY, 0, split)).isEmpty());
            List<RdfitProtocol.Reading> result = decoder.accept(Arrays.copyOfRange(BATTERY, split, BATTERY.length));
            assertEquals(1, result.size()); assertEquals(Integer.valueOf(77), result.get(0).battery);
            assertNull(result.get(0).steps);
        }
    }
    @Test public void decodesUnsignedStepsAndCoalescedFrames() {
        byte[] joined = new byte[BATTERY.length + STEPS.length];
        System.arraycopy(BATTERY, 0, joined, 0, BATTERY.length); System.arraycopy(STEPS, 0, joined, BATTERY.length, STEPS.length);
        List<RdfitProtocol.Reading> result = new RdfitProtocol().accept(joined);
        assertEquals(2, result.size()); assertEquals(Long.valueOf(1234), result.get(1).steps);
        assertEquals(Double.valueOf(123.4), result.get(1).calories); assertEquals(Long.valueOf(123), result.get(1).distance);
    }
    @Test public void corruptOrMultipartFramesCannotProduceReadings() {
        byte[] bad = BATTERY.clone(); bad[3] ^= 1; RdfitProtocol decoder = new RdfitProtocol();
        assertTrue(decoder.accept(bad).isEmpty()); assertTrue(decoder.rejectedFrames > 0);
        bad = BATTERY.clone(); bad[1] = (byte) 0xc0; assertTrue(decoder.accept(bad).isEmpty());
        bad = BATTERY.clone(); bad[2] = 1; assertTrue(decoder.accept(bad).isEmpty());
        assertEquals(1, decoder.accept(BATTERY).size());
    }
    @Test public void acknowledgementWithoutReadingIsIgnored() {
        RdfitProtocol decoder = new RdfitProtocol();
        assertTrue(decoder.accept(hex("ed 60 00 33 00 02 04 0b")).isEmpty());
        byte[] response = BATTERY.clone(); response[1] = 0x60;
        assertEquals(Integer.valueOf(77), decoder.accept(response).get(0).battery);
    }
    @Test public void oversizedAndInvalidLayoutsStayBoundedAndRecover() {
        RdfitProtocol decoder = new RdfitProtocol();
        assertTrue(decoder.accept(new byte[4096]).isEmpty()); assertTrue(decoder.rejectedFrames > 0);
        assertTrue(decoder.accept(hex("ed 40 00 00 ff ff")).isEmpty());
        assertEquals(1, decoder.accept(BATTERY).size());
        assertTrue(new RdfitProtocol().accept(hex("ed 40 00 33 00 02 04 0b")).isEmpty());
        assertTrue(new RdfitProtocol().accept(null).isEmpty());
    }
    @Test public void rejectsOutOfRangeValuesEvenWithValidCrc() {
        assertTrue(new RdfitProtocol().accept(hex("ed 40 00 ba 00 04 04 0b 65 00")).isEmpty());
        assertTrue(new RdfitProtocol().accept(hex("ed 40 00 10 00 0e 0a 0b ff ff ff ff 00 00 00 00 00 00 00 00")).isEmpty());
    }

}
