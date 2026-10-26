package com.repforge.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

public class ArchiveIOTest {
    private byte[] zip(String... names) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("abc".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }
    private Set<String> extract(byte[] bytes, File directory, int limit, int entries) throws Exception {
        return ArchiveIO.extract(new ByteArrayInputStream(bytes), directory, limit, limit, limit, entries);
    }
    @Test public void validArchiveAndChecksum() throws Exception {
        File directory = Files.createTempDirectory("archive-test").toFile();
        Set<String> names = extract(zip("backup.json", "manifest.json"), directory, 100, 10);
        String sha = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        ArchiveIO.verify(directory, names, new HashSet<>(Arrays.asList("backup.json", "manifest.json")), Collections.singletonMap("backup.json", sha));
        try { ArchiveIO.verify(directory, names, names, Collections.singletonMap("backup.json", "wrong")); fail("Modified payload accepted"); } catch (IOException expected) { }
    }
    @Test public void rejectsZipSlipAndUnexpectedFiles() throws Exception {
        for (String name : Arrays.asList("../escaped", "photos/../../escaped", "/backup.json", "photos/not-a-photo.jpg", "script.js")) {
            File directory = Files.createTempDirectory("archive-test").toFile();
            try { extract(zip(name), directory, 100, 10); fail("Unsafe entry accepted: " + name); } catch (IOException expected) { }
            assertEquals(0, directory.list().length);
        }
    }
    @Test public void boundsExpandedBytesAndEntryCount() throws Exception {
        File directory = Files.createTempDirectory("archive-test").toFile();
        try { extract(zip("backup.json", "manifest.json"), directory, 5, 10); fail("Expanded byte limit ignored"); } catch (IOException expected) { }
        try { extract(zip("backup.json", "manifest.json"), directory, 100, 1); fail("Entry limit ignored"); } catch (IOException expected) { }
    }
    @Test public void rejectsTruncatedCentralDirectoryEvenWithCompleteLocalEntries() throws Exception {
        byte[] complete = zip("backup.json", "manifest.json");
        // A streaming ZIP parser can read both entries and incorrectly accept this
        // prefix despite the missing central directory and end-of-central-directory record.
        int eocd = -1;
        for (int i = complete.length - 22; i >= 0; i--) {
            if ((complete[i] & 255) == 0x50 && (complete[i + 1] & 255) == 0x4b
                    && (complete[i + 2] & 255) == 0x05 && (complete[i + 3] & 255) == 0x06) {
                eocd = i;
                break;
            }
        }
        assertTrue("Expected ZIP end record", eocd > 0);
        File directory = Files.createTempDirectory("archive-truncated").toFile();
        try {
            extract(Arrays.copyOf(complete, eocd), directory, 100, 10);
            fail("Truncated archive falsely accepted");
        } catch (IOException expected) { }
    }

    @Test public void rejectsCentralDirectoryOnlyArchiveWithCorruptedOffset() throws Exception {
        byte[] complete = zip("backup.json", "manifest.json");
        byte[] corrupt = complete.clone();
        // Flip the local-header offset of the first central-directory entry.
        int central = -1;
        for (int i = 0; i < corrupt.length - 46; i++) {
            if ((corrupt[i] & 255) == 0x50 && (corrupt[i + 1] & 255) == 0x4b
                    && (corrupt[i + 2] & 255) == 0x01 && (corrupt[i + 3] & 255) == 0x02) {
                central = i;
                break;
            }
        }
        assertTrue(central > 0);
        corrupt[central + 42] = (byte) 0xff;
        corrupt[central + 43] = (byte) 0xff;
        corrupt[central + 44] = (byte) 0xff;
        corrupt[central + 45] = (byte) 0x7f;
        File directory = Files.createTempDirectory("archive-offset").toFile();
        try {
            extract(corrupt, directory, 100, 10);
            fail("Invalid ZIP entry offset accepted");
        } catch (IOException expected) { }
    }

    @Test public void rejectsMissingReferencedPhoto() throws Exception {
        File directory = Files.createTempDirectory("archive-test").toFile();
        Set<String> names = extract(zip("backup.json", "manifest.json"), directory, 100, 10);
        Set<String> expected = new HashSet<>(names);
        expected.add("photos/00000000-0000-0000-0000-000000000001.jpg");
        try { ArchiveIO.verify(directory, names, expected, Collections.singletonMap("backup.json", "whatever")); fail("Missing photo accepted"); } catch (IOException rejected) { }
    }
}
