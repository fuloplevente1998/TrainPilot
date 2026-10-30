package com.repforge.app

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Collections
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/** Private, append-only GPS route files indexed by the existing per-set tracking key. */
internal class GpsRouteStore(privateDirectory: File) {
    private val directory = File(privateDirectory, "gps_routes")

    companion object {
        const val MAX_BYTES: Long = 2L * 1024L * 1024L
        const val MAX_ROUTE_FILES: Int = 512
        private const val SUFFIX: String = ".route"
        private val KEY_REGEX = Regex("[A-Za-z0-9:._|+\\-]{1,200}")

        @JvmStatic
        fun validKey(key: String?): Boolean {
            return key != null && KEY_REGEX.matches(key)
        }

        @Throws(IOException::class)
        private fun digest(value: String): String {
            return try {
                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.toByteArray(StandardCharsets.UTF_8))
                val out = StringBuilder(64)
                for (b in hash) {
                    out.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255))
                }
                out.toString()
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("SHA-256 unavailable", e)
            }
        }
    }

    class Route(
        segments: List<GpsRouteTrace.Segment>,
        @JvmField val pointCount: Int,
        @JvmField val partial: Boolean
    ) {
        @JvmField
        val segments: List<GpsRouteTrace.Segment> = Collections.unmodifiableList(segments)

        fun lastOrdinal(): Int = if (segments.isEmpty()) 0 else segments[segments.size - 1].ordinal
    }

    @Throws(IOException::class)
    private fun fileFor(key: String?): File {
        if (!validKey(key)) throw IOException("Invalid route owner key")
        return File(directory, digest(key!!) + SUFFIX)
    }

    @Throws(IOException::class)
    private fun prepare(file: File) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Route directory unavailable")
        }
        if (!file.exists()) {
            val files = directory.listFiles { _, name -> name.endsWith(SUFFIX) }
            if (files == null || files.size >= MAX_ROUTE_FILES) {
                throw IOException("Route file budget reached")
            }
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun read(key: String?): Route {
        val file = fileFor(key)
        if (!file.exists()) return Route(ArrayList(), 0, false)
        if (file.length() > MAX_BYTES) throw IOException("Route exceeds file size limit")
        val segments = ArrayList<GpsRouteTrace.Segment>()
        var section: ArrayList<GpsRouteTrace.Point>? = null
        var ordinal = 0
        var count = 0
        var lastElapsed = -1L
        var partial = false

        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { input ->
            while (true) {
                val line = input.readLine() ?: break
                val columns = line.split("\t")
                if (columns.size != 6) {
                    partial = true
                    break
                }
                try {
                    val nextOrdinal = columns[0].toInt()
                    val lat = columns[1].toDouble()
                    val lon = columns[2].toDouble()
                    val accuracy = columns[3].toDouble()
                    val elapsed = columns[4].toLong()
                    val epoch = columns[5].toLong()
                    if (nextOrdinal <= 0 ||
                        nextOrdinal > GpsRouteTrace.MAX_SEGMENTS ||
                        nextOrdinal < ordinal ||
                        nextOrdinal > ordinal + 1 ||
                        !lat.isFinite() ||
                        !lon.isFinite() ||
                        abs(lat) > 90.0 ||
                        abs(lon) > 180.0 ||
                        !accuracy.isFinite() ||
                        accuracy < 0.0 ||
                        accuracy > 35.0 ||
                        elapsed < 0L ||
                        epoch <= 0L ||
                        count >= GpsRouteTrace.MAX_POINTS ||
                        (nextOrdinal == ordinal && elapsed <= lastElapsed)
                    ) {
                        partial = true
                        break
                    }
                    if (nextOrdinal != ordinal) {
                        if (section != null) segments.add(GpsRouteTrace.Segment(ordinal, section!!))
                        section = ArrayList()
                        ordinal = nextOrdinal
                    }
                    section!!.add(GpsRouteTrace.Point(lat, lon, accuracy, elapsed, epoch))
                    lastElapsed = elapsed
                    count++
                } catch (_: NumberFormatException) {
                    partial = true
                    break
                }
            }
        }
        if (section != null) segments.add(GpsRouteTrace.Segment(ordinal, section!!))
        return Route(segments, min(count, GpsRouteTrace.MAX_POINTS), partial)
    }

    /** Returns the last stored segment ordinal for an existing owner, or zero for a new owner. */
    @Synchronized
    @Throws(IOException::class)
    fun lastOrdinal(key: String?): Int = read(key).lastOrdinal()

    /** A failed write never changes the prior completed log lines. No WebView or network output. */
    @Synchronized
    @Throws(IOException::class)
    fun append(key: String?, ordinal: Int, point: GpsRouteTrace.Point?) {
        if (point == null ||
            ordinal <= 0 ||
            ordinal > GpsRouteTrace.MAX_SEGMENTS ||
            !point.latitude.isFinite() ||
            !point.longitude.isFinite() ||
            abs(point.latitude) > 90.0 ||
            abs(point.longitude) > 180.0 ||
            !point.accuracyMeters.isFinite() ||
            point.accuracyMeters < 0.0 ||
            point.accuracyMeters > 35.0 ||
            point.elapsedMillis < 0L ||
            point.epochMillis <= 0L
        ) {
            throw IOException("Invalid route point/section")
        }
        val file = fileFor(key)
        prepare(file)
        val row = String.format(
            Locale.ROOT,
            "%d\t%.7f\t%.7f\t%.1f\t%d\t%d\n",
            ordinal,
            point.latitude,
            point.longitude,
            point.accuracyMeters,
            point.elapsedMillis,
            point.epochMillis
        )
        val bytes = row.toByteArray(StandardCharsets.UTF_8)
        if (file.length() + bytes.size > MAX_BYTES) {
            throw IOException("Route size budget reached")
        }
        FileOutputStream(file, true).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
    }

    /** Called only after an explicit user-directed removal; never automatically prunes old tracks. */
    @Synchronized
    @Throws(IOException::class)
    fun delete(key: String?): Boolean {
        val file = fileFor(key)
        return !file.exists() || file.delete()
    }
}
