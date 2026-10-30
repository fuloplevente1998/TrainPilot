package com.repforge.app

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Bounded, allow-listed ZIP extraction shared by BackupRestoreManager and JVM regressions. */
internal object ArchiveIO {
    private val ALLOWED_ENTRY_REGEX = Regex("backup\\.json|manifest\\.json|photos/[a-f0-9-]{36}\\.jpg")

    @JvmStatic
    @Throws(IOException::class)
    fun copy(input: InputStream, out: OutputStream, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            total += count
            if (total > limit) throw IOException("Backup exceeds size limit.")
            out.write(buffer, 0, count)
        }
        return total
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extract(
        input: InputStream,
        directory: File,
        jsonLimit: Int,
        photoLimit: Int,
        totalLimit: Long,
        entryLimit: Int
    ): Set<String> {
        // ZipInputStream accepts a local-file-header-only prefix when the central
        // directory / end record has been truncated. This is not a complete backup.
        // Stage the bounded compressed stream OUTSIDE the extraction directory, then
        // require ZipFile to validate its central directory before any payload is used.
        val staged = File.createTempFile("verified-archive-", ".zip", directory.parentFile ?: directory)
        try {
            FileOutputStream(staged).use { out ->
                // Compressed bytes include local/central headers and ZIP metadata
                // even when expanded payload is tiny (see 100-byte JVM fixtures).
                // Keep a bounded allowance without accepting unbounded input.
                copy(input, out, totalLimit + entryLimit.toLong() * 4096L + 65557L)
                out.fd.sync()
            }
            val names = HashSet<String>()
            var total = 0L
            ZipFile(staged).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (!names.add(name) ||
                        names.size > entryLimit ||
                        entry.isDirectory ||
                        !ALLOWED_ENTRY_REGEX.matches(name)
                    ) {
                        throw IOException("Invalid or duplicate ZIP entry.")
                    }
                    val file = File(directory, name)
                    val parent = file.parentFile
                    if (parent == null || (!parent.isDirectory && !parent.mkdirs())) {
                        throw IOException("Cannot create archive directory.")
                    }
                    val bytes: Long
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(file).use { out ->
                            bytes = copy(
                                input,
                                out,
                                (if (name.startsWith("photos/")) photoLimit else jsonLimit).toLong()
                            )
                            out.fd.sync()
                        }
                    }
                    total += bytes
                    if (bytes == 0L || total > totalLimit) {
                        throw IOException("Empty or oversized archive.")
                    }
                }
            }
            return names
        } finally {
            if (staged.exists() && !staged.delete()) staged.deleteOnExit()
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun hash(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            digest.update(buffer, 0, count)
        }
        val hex = StringBuilder()
        for (value in digest.digest()) {
            hex.append(String.format(Locale.US, "%02x", value.toInt() and 255))
        }
        return hex.toString()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun verify(
        directory: File,
        names: Set<String>,
        expected: Set<String>,
        checksums: Map<String, String>
    ) {
        if (names != expected || checksums.size != names.size - 1) {
            throw IOException("Incomplete photo archive.")
        }
        for (name in names) {
            if ("manifest.json" == name) continue
            FileInputStream(File(directory, name)).use { input ->
                if (hash(input) != checksums[name]) {
                    throw IOException("Archive checksum mismatch.")
                }
            }
        }
    }
}
