package com.repforge.app

import java.io.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

internal class GpsRouteStore(privateDirectory: File) {
    private val directory = File(privateDirectory, "gps_routes")
    companion object {
        const val MAX_BYTES = 2L * 1024L * 1024L; const val MAX_ROUTE_FILES = 512; private const val SUFFIX = ".route"
        private val KEY_REGEX = Regex("[A-Za-z0-9:._|+\\-]{1,200}")
        @JvmStatic fun validKey(k: String?): Boolean = k != null && KEY_REGEX.matches(k)
        private fun digest(v: String): String {
            val h = MessageDigest.getInstance("SHA-256").digest(v.toByteArray(StandardCharsets.UTF_8)); val s = StringBuilder(64)
            for (b in h) s.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255)); return s.toString()
        }
    }
    class Route(segments: List<GpsRouteTrace.Segment>, @JvmField val pointCount: Int, @JvmField val partial: Boolean) {
        @JvmField val segments: List<GpsRouteTrace.Segment> = Collections.unmodifiableList(segments)
        fun lastOrdinal(): Int = if (segments.isEmpty()) 0 else segments.last().ordinal
    }
    private fun fileFor(k: String?): File { if (!validKey(k)) throw IOException("Invalid route key"); return File(directory, digest(k!!) + SUFFIX) }
    private fun prepare(f: File) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Route dir unavailable")
        if (!f.exists()) { val files = directory.listFiles { _, n -> n.endsWith(SUFFIX) }; if (files == null || files.size >= MAX_ROUTE_FILES) throw IOException("Route budget reached") }
    }
    @Synchronized @Throws(IOException::class) fun read(key: String?): Route {
        val f = fileFor(key); if (!f.exists()) return Route(ArrayList(), 0, false)
        if (f.length() > MAX_BYTES) throw IOException("Route size exceeded")
        val segs = ArrayList<GpsRouteTrace.Segment>(); var pts: ArrayList<GpsRouteTrace.Point>? = null
        var ord = 0; var cnt = 0; var last = -1L; var part = false
        BufferedReader(InputStreamReader(FileInputStream(f), StandardCharsets.UTF_8)).use { r ->
            while (true) {
                val line = r.readLine() ?: break; val cols = line.split("\t"); if (cols.size != 6) { part = true; break }
                try {
                    val o = cols[0].toInt(); val lat = cols[1].toDouble(); val lon = cols[2].toDouble(); val acc = cols[3].toDouble(); val el = cols[4].toLong(); val ep = cols[5].toLong()
                    if (o <= 0 || o > GpsRouteTrace.MAX_SEGMENTS || o < ord || o > ord + 1 || !lat.isFinite() || !lon.isFinite() || abs(lat) > 90 || abs(lon) > 180 || !acc.isFinite() || acc < 0 || acc > 35 || el < 0 || ep <= 0 || cnt >= GpsRouteTrace.MAX_POINTS || (o == ord && el <= last)) { part = true; break }
                    if (o != ord) { if (pts != null) segs.add(GpsRouteTrace.Segment(ord, pts!!)); pts = ArrayList(); ord = o }
                    pts!!.add(GpsRouteTrace.Point(lat, lon, acc, el, ep)); last = el; cnt++
                } catch (_: Exception) { part = true; break }
            }
        }
        if (pts != null) segs.add(GpsRouteTrace.Segment(ord, pts!!))
        return Route(segs, min(cnt, GpsRouteTrace.MAX_POINTS), part)
    }
    @Synchronized fun lastOrdinal(k: String?): Int = read(k).lastOrdinal()
    @Synchronized @Throws(IOException::class) fun append(key: String?, ordinal: Int, p: GpsRouteTrace.Point?) {
        if (p == null || ordinal <= 0 || ordinal > GpsRouteTrace.MAX_SEGMENTS || !p.latitude.isFinite() || !p.longitude.isFinite() || abs(p.latitude) > 90 || abs(p.longitude) > 180 || !p.accuracyMeters.isFinite() || p.accuracyMeters < 0 || p.accuracyMeters > 35 || p.elapsedMillis < 0 || p.epochMillis <= 0) throw IOException("Invalid fix")
        val f = fileFor(key); prepare(f)
        val row = String.format(Locale.ROOT, "%d\t%.7f\t%.7f\t%.1f\t%d\t%d\n", ordinal, p.latitude, p.longitude, p.accuracyMeters, p.elapsedMillis, p.epochMillis)
        val b = row.toByteArray(StandardCharsets.UTF_8); if (f.length() + b.size > MAX_BYTES) throw IOException("Route budget exceeded")
        FileOutputStream(f, true).use { out -> out.write(b); out.flush(); out.fd.sync() }
    }
    @Synchronized fun delete(k: String?): Boolean { val f = fileFor(k); return !f.exists() || f.delete() }
}
