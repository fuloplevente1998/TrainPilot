package com.repforge.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Private, append-only GPS route files indexed by the existing per-set tracking key. */
final class GpsRouteStore {
    static final long MAX_BYTES = 2L * 1024 * 1024;
    static final int MAX_ROUTE_FILES = 512;
    private static final String SUFFIX = ".route";
    private final File directory;

    GpsRouteStore(File privateDirectory) { directory = new File(privateDirectory, "gps_routes"); }

    static boolean validKey(String key) {
        return key != null && key.matches("[A-Za-z0-9:._|+\\-]{1,200}");
    }

    private static String digest(String value) throws IOException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : hash) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (NoSuchAlgorithmException e) { throw new IOException("SHA-256 unavailable", e); }
    }

    private File fileFor(String key) throws IOException {
        if (!validKey(key)) throw new IOException("Invalid route owner key");
        return new File(directory, digest(key) + SUFFIX);
    }

    private void prepare(File file) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Route directory unavailable");
        if (!file.exists()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(SUFFIX));
            if (files == null || files.length >= MAX_ROUTE_FILES) throw new IOException("Route file budget reached");
        }
    }

    static final class Route {
        final List<GpsRouteTrace.Segment> segments;
        final int pointCount;
        final boolean partial;
        Route(List<GpsRouteTrace.Segment> segments, int pointCount, boolean partial) {
            this.segments = Collections.unmodifiableList(segments);
            this.pointCount = pointCount;
            this.partial = partial;
        }
        int lastOrdinal() { return segments.isEmpty() ? 0 : segments.get(segments.size() - 1).ordinal; }
    }

    synchronized Route read(String key) throws IOException {
        File file = fileFor(key);
        if (!file.exists()) return new Route(new ArrayList<>(), 0, false);
        if (file.length() > MAX_BYTES) throw new IOException("Route exceeds file size limit");
        ArrayList<GpsRouteTrace.Segment> segments = new ArrayList<>();
        ArrayList<GpsRouteTrace.Point> section = null;
        int ordinal = 0, count = 0;
        boolean partial = false;
        try (BufferedReader input = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                String[] columns = line.split("\t", -1);
                if (columns.length != 6) { partial = true; break; }
                try {
                    int nextOrdinal = Integer.parseInt(columns[0]);
                    double lat = Double.parseDouble(columns[1]), lon = Double.parseDouble(columns[2]);
                    double accuracy = Double.parseDouble(columns[3]);
                    long elapsed = Long.parseLong(columns[4]), epoch = Long.parseLong(columns[5]);
                    if (nextOrdinal <= 0 || nextOrdinal > GpsRouteTrace.MAX_SEGMENTS
                            || nextOrdinal < ordinal || nextOrdinal > ordinal + 1
                            || !Double.isFinite(lat) || !Double.isFinite(lon)
                            || Math.abs(lat) > 90 || Math.abs(lon) > 180
                            || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 35
                            || elapsed < 0 || epoch <= 0 || ++count > GpsRouteTrace.MAX_POINTS) {
                        partial = true; break;
                    }
                    if (nextOrdinal != ordinal) {
                        section = new ArrayList<>();
                        segments.add(new GpsRouteTrace.Segment(nextOrdinal, section));
                        ordinal = nextOrdinal;
                    }
                    // Segment holds an immutable snapshot; replace after each append.
                    section.add(new GpsRouteTrace.Point(lat, lon, accuracy, elapsed, epoch));
                    segments.set(segments.size() - 1, new GpsRouteTrace.Segment(ordinal, section));
                } catch (NumberFormatException e) { partial = true; break; }
            }
        }
        return new Route(segments, Math.min(count, GpsRouteTrace.MAX_POINTS), partial);
    }

    /** Returns the last stored segment ordinal for an existing owner, or zero for a new owner. */
    synchronized int lastOrdinal(String key) throws IOException { return read(key).lastOrdinal(); }

    /** A failed write never changes the prior completed log lines. No WebView or network output. */
    synchronized void append(String key, int ordinal, GpsRouteTrace.Point point) throws IOException {
        if (point == null || ordinal <= 0 || ordinal > GpsRouteTrace.MAX_SEGMENTS) {
            throw new IOException("Invalid route point/section");
        }
        File file = fileFor(key);
        prepare(file);
        String row = String.format(Locale.ROOT, "%d\t%.7f\t%.7f\t%.1f\t%d\t%d\n",
                ordinal, point.latitude, point.longitude, point.accuracyMeters,
                point.elapsedMillis, point.epochMillis);
        byte[] bytes = row.getBytes(StandardCharsets.UTF_8);
        if (file.length() + bytes.length > MAX_BYTES) throw new IOException("Route size budget reached");
        try (FileOutputStream output = new FileOutputStream(file, true)) {
            output.write(bytes);
            output.flush();
            output.getFD().sync();
        }
    }

    /** Called only after an explicit user-directed removal; never automatically prunes old tracks. */
    synchronized boolean delete(String key) throws IOException {
        File file = fileFor(key);
        return !file.exists() || file.delete();
    }
}
