package com.repforge.app;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Bounded, allow-listed ZIP extraction shared by the plugin and JVM regressions. */
final class ArchiveIO {
    static long copy(InputStream in, OutputStream out, long limit) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
            total += count;
            if (total > limit) throw new IOException("Backup exceeds size limit.");
            out.write(buffer, 0, count);
        }
        return total;
    }

    static Set<String> extract(InputStream input, File directory, int jsonLimit,
            int photoLimit, long totalLimit, int entryLimit) throws IOException {
        Set<String> names = new HashSet<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!names.add(name) || names.size() > entryLimit || entry.isDirectory()
                        || !name.matches("backup\\.json|manifest\\.json|photos/[a-f0-9-]{36}\\.jpg")) {
                    throw new IOException("Invalid or duplicate ZIP entry.");
                }
                File file = new File(directory, name);
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create archive directory.");
                long bytes;
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bytes = copy(zip, out, name.startsWith("photos/") ? photoLimit : jsonLimit);
                    out.getFD().sync();
                }
                total += bytes;
                if (bytes == 0 || total > totalLimit) throw new IOException("Empty or oversized archive.");
                zip.closeEntry();
            }
        }
        return names;
    }

    static String hash(InputStream in) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest()) hex.append(String.format(Locale.US, "%02x", value & 255));
        return hex.toString();
    }

    static void verify(File directory, Set<String> names, Set<String> expected,
            Map<String, String> checksums) throws Exception {
        if (!names.equals(expected) || checksums.size() != names.size() - 1) {
            throw new IOException("Incomplete photo archive.");
        }
        for (String name : names) {
            if (name.equals("manifest.json")) continue;
            try (InputStream in = new FileInputStream(new File(directory, name))) {
                if (!hash(in).equals(checksums.get(name))) throw new IOException("Archive checksum mismatch.");
            }
        }
    }
}
