package com.repforge.app;

import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static org.junit.Assert.*;

public class GpsRouteStoreTest {
    private static final String KEY = "session-20261014:set-one";
    private static GpsRouteTrace.Point p(double lat, long elapsed) {
        return new GpsRouteTrace.Point(lat, 19d, 5d, elapsed, 1700000000000L + elapsed);
    }
    private static GpsRouteStore newStore() throws IOException {
        return new GpsRouteStore(Files.createTempDirectory("trainpilot-route-test").toFile());
    }

    @Test public void persistsSegmentsAfterReopeningWithoutMergingAcrossBreak() throws Exception {
        GpsRouteStore store = newStore();
        store.append(KEY, 1, p(47d, 1000));
        store.append(KEY, 1, p(47.0001d, 6000));
        store.append(KEY, 2, p(47.02d, 7000));
        assertEquals(2, store.lastOrdinal(KEY));
        GpsRouteStore.Route saved = store.read(KEY);
        assertEquals(3, saved.pointCount);
        assertEquals(2, saved.segments.size());
        assertEquals(2, saved.segments.get(0).points.size());
        assertEquals(1, saved.segments.get(1).points.size());
        assertEquals(47.02d, saved.segments.get(1).points.get(0).latitude, 0.0000001d);
        assertEquals(1700000007000L, saved.segments.get(1).points.get(0).epochMillis);
        assertFalse(saved.partial);
    }

    @Test public void persistsAfterNewStoreInstanceAndKeepsWorkoutOwnersApart() throws Exception {
        File parent = Files.createTempDirectory("trainpilot-route-owners").toFile();
        GpsRouteStore first = new GpsRouteStore(parent);
        first.append(KEY, 1, p(47d, 1000));
        first.append("other-workout:other-set", 1, p(48d, 2000));
        GpsRouteStore restarted = new GpsRouteStore(parent);
        assertEquals(1, restarted.read(KEY).pointCount);
        assertEquals(47d, restarted.read(KEY).segments.get(0).points.get(0).latitude, 0d);
        assertEquals(48d, restarted.read("other-workout:other-set").segments.get(0).points.get(0).latitude, 0d);
        assertEquals(0, restarted.read("absent-set").pointCount);
        File[] files = new File(parent, "gps_routes").listFiles();
        assertNotNull(files);
        assertEquals(2, files.length);
        for (File file : files) {
            assertFalse("Raw tracking owner must not appear in file names", file.getName().contains("session"));
            assertTrue(file.getName().endsWith(".route"));
        }
    }

    @Test public void explicitDeletionAffectsOnlyTheRequestedRoute() throws Exception {
        GpsRouteStore store = newStore();
        store.append(KEY, 1, p(47d, 1000));
        store.append("other-set", 1, p(48d, 1000));
        assertTrue(store.delete(KEY));
        assertEquals(0, store.read(KEY).pointCount);
        assertEquals(1, store.read("other-set").pointCount);
        assertTrue(store.delete(KEY)); // Idempotent.
    }

    @Test public void rejectsPathTraversalInvalidIdentifiersAndBadOrdinals() throws Exception {
        GpsRouteStore store = newStore();
        for (String invalid : new String[]{"", "../bad", "space in key", "abc/def", "ç"}) {
            assertFalse(GpsRouteStore.validKey(invalid));
            try { store.append(invalid, 1, p(47d, 1000)); fail(invalid); }
            catch (IOException expected) { /* No path traversal. */ }
        }
        try { store.append(KEY, 0, p(47d, 1000)); fail("Invalid segment index"); }
        catch (IOException expected) { /* Bounded segments. */ }
    }

    @Test public void corruptedTrailingRecordIsFlaggedWithoutErasingGoodFixes() throws Exception {
        File root = Files.createTempDirectory("trainpilot-route-tail").toFile();
        GpsRouteStore store = new GpsRouteStore(root);
        store.append(KEY, 1, p(47d, 1000));
        File file = new File(root, "gps_routes").listFiles()[0];
        try (FileOutputStream output = new FileOutputStream(file, true)) {
            output.write("unfinished".getBytes(StandardCharsets.UTF_8));
        }
        GpsRouteStore.Route route = new GpsRouteStore(root).read(KEY);
        assertEquals(1, route.pointCount);
        assertEquals(1, route.segments.size());
        assertTrue(route.partial);
    }

    @Test public void oversizedRouteIsRejectedWithoutAllocatingItsContents() throws Exception {
        File root = Files.createTempDirectory("trainpilot-route-size").toFile();
        GpsRouteStore store = new GpsRouteStore(root);
        store.append(KEY, 1, p(47d, 1000));
        File file = new File(root, "gps_routes").listFiles()[0];
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
            raf.setLength(GpsRouteStore.MAX_BYTES + 1);
        }
        try { store.read(KEY); fail("Oversized file must be rejected"); }
        catch (IOException expected) { /* Does not read unbounded locations. */ }
    }
}
