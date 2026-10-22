package com.repforge.shared

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Arrays
import kotlin.math.min

/** Private route chunks and consent history. The caller supplies a no-backup directory. */
class GpsRouteLedger193 private constructor(noBackupDirectory: File) {
    private val directory = File(noBackupDirectory, "wear_routes193")

    companion object {
        const val MAX_ROUTES: Int = 512
        const val MAX_GRANTS: Int = 128
        private const val ROUTE_BYTES: Int = 2 * 1024 * 1024
        private const val INDEX_BYTES: Int = 1024 * 1024
        private val ROUTE_KEY_REGEX = Regex("wear193:[a-f0-9]{64}")
        private val instances = HashMap<String, GpsRouteLedger193>()

        @JvmStatic
        @Synchronized
        @Throws(Exception::class)
        fun open(noBackupDirectory: File): GpsRouteLedger193 {
            val key = noBackupDirectory.canonicalPath
            return instances.getOrPut(key) { GpsRouteLedger193(File(key)) }
        }

        private fun requireLedger(value: Boolean) {
            if (!value) throw IllegalStateException("Route storage changed or budget reached")
        }

        @JvmStatic
        @Throws(Exception::class)
        fun fingerprint(packet: JSONObject): String {
            val canonical = JSONArray()
            for (field in arrayOf(
                "schema", "routeKey", "workoutId", "exerciseId", "setId",
                "sourceNode", "targetNode", "ownerEpoch193", "streamId", "sequence", "closed", "partial"
            )) {
                canonical.put(packet.get(field))
            }
            val points = JSONArray()
            val raw = packet.getJSONArray("points")
            for (i in 0 until raw.length()) {
                val row = JSONArray()
                val point = raw.getJSONObject(i)
                for (field in arrayOf("latitude", "longitude", "accuracyMeters", "elapsedMillis", "epochMillis", "segment")) {
                    row.put(point.get(field))
                }
                points.put(row)
            }
            return GpsRoutePacket193.digest(canonical.put(points).toString())
        }
    }

    private fun index(): AtomicFile = AtomicFile(File(directory, "index.json"))

    @Throws(Exception::class)
    private fun route(key: String): AtomicFile {
        return AtomicFile(File(directory, GpsRoutePacket193.digest(key) + ".json"))
    }

    @Throws(Exception::class)
    private fun read(file: AtomicFile, budget: Int): JSONObject {
        val bakFile = File(file.baseFile.path + ".bak")
        if (!file.baseFile.exists() && !bakFile.exists()) return JSONObject()
        requireLedger(file.baseFile.length() <= budget && bakFile.length() <= budget)
        val bytes = file.readFully()
        requireLedger(bytes.size <= budget)
        return JSONObject(String(bytes, StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    private fun write(file: AtomicFile, value: JSONObject, budget: Int) {
        val bytes = value.toString().toByteArray(StandardCharsets.UTF_8)
        requireLedger(bytes.size <= budget)
        requireLedger(directory.isDirectory || directory.mkdirs())
        var out: FileOutputStream? = null
        try {
            out = file.startWrite()
            out.write(bytes)
            out.fd.sync()
            file.finishWrite(out)
            if (!file.baseFile.isFile || !Arrays.equals(bytes, file.readFully())) {
                throw IOException("Route commit was not durable")
            }
        } catch (error: Exception) {
            if (out != null) file.failWrite(out)
            throw error
        }
    }

    @Throws(Exception::class)
    private fun routes(index: JSONObject): JSONObject {
        if (!index.has("routes")) index.put("routes", JSONObject())
        return index.getJSONObject("routes")
    }

    @Throws(Exception::class)
    private fun workoutDeleted(index: JSONObject, id: String): Boolean {
        val deleted = index.optJSONObject("deletedWorkouts") ?: return false
        return deleted.optBoolean(GpsRoutePacket193.digest(id))
    }

    /** Native watch-start proof can precede local NodeClient availability. No remote packet calls this. */
    @Synchronized
    @Throws(Exception::class)
    fun rememberSource(proof: JSONObject): Boolean {
        val key = proof.getString("routeKey")
        requireLedger(
            key == GpsRoutePacket193.key(proof.getString("workoutId"), proof.getString("setId")) &&
                GpsRoutePacket193.id(proof.getString("sourceNode")) &&
                proof.getLong("ownerEpoch193") == 0L &&
                proof.getString("exerciseId").isNotEmpty() &&
                proof.getString("exerciseId").length <= 200
        )
        val index = read(index(), INDEX_BYTES)
        val routes = routes(index)
        var entry = routes.optJSONObject(key)
        if (workoutDeleted(index, proof.getString("workoutId"))) return false
        if (entry != null && entry.optBoolean("deleted")) return false
        if (entry == null) {
            requireLedger(routes.length() < MAX_ROUTES)
            entry = JSONObject().put("grants", JSONArray())
            routes.put(key, entry)
        }
        val grants = entry.getJSONArray("grants")
        var old = entry.optJSONObject("pendingSource")
        for (i in 0 until grants.length()) {
            if (grants.getJSONObject(i).getLong("ownerEpoch193") == 0L) {
                old = grants.getJSONObject(i)
            }
        }
        val fields = arrayOf("routeKey", "workoutId", "exerciseId", "setId", "sourceNode", "ownerEpoch193")
        if (old != null) {
            for (field in fields) {
                requireLedger(old.get(field).toString() == proof.get(field).toString())
            }
            return true
        }
        val clean = JSONObject()
        for (field in fields) clean.put(field, proof.get(field))
        entry.put("pendingSource", clean)
        write(index(), index, INDEX_BYTES)
        return true
    }

    /** Only the native ownership adapter may add grants; transport input cannot authorize itself. */
    @Synchronized
    @Throws(Exception::class)
    fun authorize(grant: JSONObject): Boolean {
        val key = grant.getString("routeKey")
        val workout = grant.getString("workoutId")
        val set = grant.getString("setId")
        requireLedger(
            key == GpsRoutePacket193.key(workout, set) &&
                GpsRoutePacket193.id(grant.getString("sourceNode")) &&
                GpsRoutePacket193.id(grant.getString("targetNode")) &&
                grant.getString("sourceNode") != grant.getString("targetNode") &&
                grant.getString("exerciseId").isNotEmpty() &&
                grant.getString("exerciseId").length <= 200 &&
                grant.getLong("ownerEpoch193") >= 0L
        )
        val index = read(index(), INDEX_BYTES)
        val routes = routes(index)
        var entry = routes.optJSONObject(key)
        if (workoutDeleted(index, workout)) return false
        if (entry != null && entry.optBoolean("deleted")) return false
        if (entry == null) {
            requireLedger(routes.length() < MAX_ROUTES)
            entry = JSONObject().put("grants", JSONArray())
            routes.put(key, entry)
        }
        val grants = entry.getJSONArray("grants")
        for (i in 0 until grants.length()) {
            val old = grants.getJSONObject(i)
            if (old.getLong("ownerEpoch193") == grant.getLong("ownerEpoch193")) {
                for (field in arrayOf("routeKey", "workoutId", "exerciseId", "setId", "sourceNode", "targetNode")) {
                    requireLedger(old.getString(field) == grant.getString(field))
                }
                return true
            }
        }
        requireLedger(grants.length() < MAX_GRANTS)
        val clean = JSONObject()
        for (field in arrayOf("routeKey", "workoutId", "exerciseId", "setId", "sourceNode", "targetNode", "ownerEpoch193")) {
            clean.put(field, grant.get(field))
        }
        grants.put(clean)
        write(index(), index, INDEX_BYTES)
        return true
    }

    /** Returns null without consent. A receipt is produced only after durable storage (or a tombstone). */
    @Synchronized
    @Throws(Exception::class)
    fun accept(input: JSONObject, origin: String, localNode: String): JSONObject? {
        val index = read(index(), INDEX_BYTES)
        val entry = routes(index).optJSONObject(input.optString("routeKey"))
        if (entry == null) {
            if (!workoutDeleted(index, input.optString("workoutId"))) return null
            val own = JSONObject()
            for (field in arrayOf("routeKey", "workoutId", "exerciseId", "setId", "sourceNode", "targetNode", "ownerEpoch193")) {
                own.put(field, input.get(field))
            }
            val packet = GpsRoutePacket193.validate(input, own, origin, localNode)
            return receipt(packet, origin, localNode, "deleted")
        }
        var grant: JSONObject? = null
        val grants = entry.optJSONArray("grants")
        if (grants != null) {
            for (i in 0 until grants.length()) {
                if (grants.getJSONObject(i).optLong("ownerEpoch193", -1L) == input.optLong("ownerEpoch193", -2L)) {
                    grant = grants.getJSONObject(i)
                }
            }
        }
        val pending = entry.optJSONObject("pendingSource")
        if (grant == null && pending != null && input.optLong("ownerEpoch193", -1L) == 0L) {
            grant = JSONObject(pending.toString()).put("targetNode", localNode)
            GpsRoutePacket193.validate(input, grant, origin, localNode)
            if (!entry.optBoolean("deleted")) {
                grants?.put(grant)
                entry.remove("pendingSource")
                write(index(), index, INDEX_BYTES)
            }
        }
        if (grant == null) return null

        val packet = GpsRoutePacket193.validate(input, grant, origin, localNode)
        val hash = fingerprint(packet)
        val id = packet.getString("streamId") + ":" + packet.getLong("sequence")
        var status = if (entry.optBoolean("deleted")) "deleted" else "stored"
        if (!entry.optBoolean("deleted")) {
            val file = route(packet.getString("routeKey"))
            val track = read(file, ROUTE_BYTES)
            if (!track.has("chunks")) track.put("chunks", JSONObject())
            val chunks = track.getJSONObject("chunks")
            val old = chunks.optJSONObject(id)
            if (old != null) {
                requireLedger(hash == old.getString("hash"))
                status = "duplicate"
            } else {
                var points = packet.getJSONArray("points").length()
                requireLedger(chunks.length() < GpsRoutePacket193.MAX_POINTS + GpsRoutePacket193.MAX_SEGMENTS)
                val streams = HashSet<String>()
                val segments = HashSet<String>()
                val packetPoints = packet.getJSONArray("points")
                for (i in 0 until packetPoints.length()) {
                    segments.add(packet.getString("streamId") + ":" + packetPoints.getJSONObject(i).getInt("segment"))
                }
                val keys = chunks.keys()
                while (keys.hasNext()) {
                    val previous = chunks.getJSONObject(keys.next()).getJSONObject("packet")
                    streams.add(previous.getString("streamId"))
                    val prevPoints = previous.getJSONArray("points")
                    points += prevPoints.length()
                    for (i in 0 until prevPoints.length()) {
                        segments.add(previous.getString("streamId") + ":" + prevPoints.getJSONObject(i).getInt("segment"))
                    }
                    if (previous.getString("streamId") == packet.getString("streamId")) {
                        requireLedger(
                            previous.getLong("ownerEpoch193") == packet.getLong("ownerEpoch193") &&
                                previous.getString("sourceNode") == origin
                        )
                        requireLedger(!previous.getBoolean("closed") || previous.getLong("sequence") > packet.getLong("sequence"))
                        requireLedger(!packet.getBoolean("closed") || packet.getLong("sequence") > previous.getLong("sequence"))
                        val earlier = if (previous.getLong("sequence") < packet.getLong("sequence")) previous else packet
                        val later = if (earlier === previous) packet else previous
                        val a = earlier.getJSONArray("points")
                        val b = later.getJSONArray("points")
                        if (a.length() > 0 && b.length() > 0) {
                            val last = a.getJSONObject(a.length() - 1)
                            val first = b.getJSONObject(0)
                            val gap = first.getLong("elapsedMillis") - last.getLong("elapsedMillis")
                            requireLedger(
                                gap > 0L &&
                                    first.getLong("epochMillis") > last.getLong("epochMillis") &&
                                    first.getInt("segment") >= last.getInt("segment")
                            )
                            if (later.getLong("sequence") == earlier.getLong("sequence") + 1L &&
                                first.getInt("segment") == last.getInt("segment")
                            ) {
                                requireLedger(
                                    gap <= 30_000L &&
                                        GpsRoutePacket193.distance(
                                            last.getDouble("latitude"),
                                            last.getDouble("longitude"),
                                            first.getDouble("latitude"),
                                            first.getDouble("longitude")
                                        ) / (gap / 1000.0) <= 20.0
                                )
                            }
                        }
                    }
                }
                streams.add(packet.getString("streamId"))
                requireLedger(
                    streams.size <= GpsRoutePacket193.MAX_SEGMENTS &&
                        segments.size <= GpsRoutePacket193.MAX_SEGMENTS &&
                        points <= GpsRoutePacket193.MAX_POINTS
                )
                chunks.put(id, JSONObject().put("hash", hash).put("packet", packet))
                write(file, track, ROUTE_BYTES)
            }
            var count = 0
            val countKeys = chunks.keys()
            while (countKeys.hasNext()) {
                count += chunks.getJSONObject(countKeys.next()).getJSONObject("packet").getJSONArray("points").length()
            }
            if (entry.optInt("pointCount", -1) != count) {
                entry.put("pointCount", count)
                write(index(), index, INDEX_BYTES)
            }
        }
        return receipt(packet, origin, localNode, status)
    }

    @Throws(Exception::class)
    private fun receipt(packet: JSONObject, origin: String, localNode: String, status: String): JSONObject {
        return JSONObject()
            .put("schema", 1)
            .put("routeKey", packet.getString("routeKey"))
            .put("streamId", packet.getString("streamId"))
            .put("sequence", packet.getLong("sequence"))
            .put("ownerEpoch193", packet.getLong("ownerEpoch193"))
            .put("sourceNode", localNode)
            .put("targetNode", origin)
            .put("hash", fingerprint(packet))
            .put("status", status)
    }

    /** Coordinate-bearing data is read only by the explicit map bridge or private route transport. */
    @Synchronized
    @Throws(Exception::class)
    fun packets(key: String): JSONArray {
        val entry = routes(read(index(), INDEX_BYTES)).optJSONObject(key)
        if (entry == null || entry.optBoolean("deleted")) return JSONArray()
        val chunks = read(route(key), ROUTE_BYTES).optJSONObject("chunks")
        val result = JSONArray()
        if (chunks != null) {
            val keys = chunks.keys()
            while (keys.hasNext()) {
                result.put(chunks.getJSONObject(keys.next()).getJSONObject("packet"))
            }
        }
        return result
    }

    /** Coordinate-free references for one canonical workout; no full-history scan or location event. */
    @Synchronized
    @Throws(Exception::class)
    fun bindings(workoutId: String): JSONArray {
        val routes = routes(read(index(), INDEX_BYTES))
        val result = JSONArray()
        val keys = routes.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = routes.getJSONObject(key)
            val grants = entry.optJSONArray("grants")
            if (entry.optBoolean("deleted") || entry.optInt("pointCount") < 1 || grants == null || grants.length() == 0) {
                continue
            }
            val grant = grants.getJSONObject(0)
            if (workoutId != grant.getString("workoutId")) continue
            result.put(
                JSONObject()
                    .put("routeKey", key)
                    .put("workoutId", workoutId)
                    .put("exerciseId", grant.getString("exerciseId"))
                    .put("setId", grant.getString("setId"))
                    .put("pointCount", entry.getInt("pointCount"))
            )
        }
        return result
    }

    /** #163 route shape, exposed only on an explicit map request. Missing chunks stay visible gaps. */
    @Synchronized
    @Throws(Exception::class)
    fun view(key: String): JSONObject {
        val raw = packets(key)
        val rows = ArrayList<JSONObject>()
        val firstAt = HashMap<String, Long>()
        for (i in 0 until raw.length()) {
            val packet = raw.getJSONObject(i)
            rows.add(packet)
            val points = packet.getJSONArray("points")
            val stream = packet.getString("streamId")
            if (points.length() > 0) {
                val epoch = points.getJSONObject(0).getLong("epochMillis")
                val existing = firstAt[stream]
                firstAt[stream] = if (existing == null) epoch else min(existing, epoch)
            }
        }
        rows.sortWith { a, b ->
            val epoch = a.optLong("ownerEpoch193").compareTo(b.optLong("ownerEpoch193"))
            if (epoch != 0) return@sortWith epoch
            val x = a.optString("streamId")
            val y = b.optString("streamId")
            val time = (firstAt[x] ?: Long.MAX_VALUE).compareTo(firstAt[y] ?: Long.MAX_VALUE)
            if (time != 0) return@sortWith time
            val stream = x.compareTo(y)
            if (stream != 0) stream else a.optLong("sequence").compareTo(b.optLong("sequence"))
        }

        val sections = JSONArray()
        var points: JSONArray? = null
        var last: JSONObject? = null
        var stream = ""
        var sequence = 0L
        var segment = 0L
        var count = 0
        var partial = false
        var closed = true
        var meters = 0.0

        for (packet in rows) {
            val nextStream = packet.getString("streamId")
            val nextSequence = packet.getLong("sequence")
            val newStream = nextStream != stream
            val missing = nextSequence != (if (newStream) 1L else sequence + 1L)
            if (newStream) {
                if (!closed) partial = true
                closed = false
                last = null
                segment = 0L
            }
            if (missing) {
                partial = true
                last = null
            }
            if (packet.getBoolean("partial")) partial = true
            val fixes = packet.getJSONArray("points")
            for (i in 0 until fixes.length()) {
                val fix = fixes.getJSONObject(i)
                val nextSegment = fix.getLong("segment")
                val prev = last
                if (prev != null) {
                    val gap = fix.getLong("elapsedMillis") - prev.getLong("elapsedMillis")
                    if (nextSegment != segment ||
                        gap <= 0L ||
                        gap > 30_000L ||
                        fix.getLong("epochMillis") <= prev.getLong("epochMillis") ||
                        GpsRoutePacket193.distance(
                            prev.getDouble("latitude"),
                            prev.getDouble("longitude"),
                            fix.getDouble("latitude"),
                            fix.getDouble("longitude")
                        ) / (gap / 1000.0) > 20.0
                    ) {
                        last = null
                    }
                }
                if (last == null) {
                    if (sections.length() >= GpsRoutePacket193.MAX_SEGMENTS) {
                        partial = true
                        break
                    }
                    points = JSONArray()
                    sections.put(
                        JSONObject()
                            .put("ordinal", sections.length() + 1)
                            .put("source", "watch_gps")
                            .put("ownerEpoch193", packet.getLong("ownerEpoch193"))
                            .put("streamId", nextStream)
                            .put("points", points)
                    )
                } else {
                    val validPrev = last!!
                    meters += GpsRoutePacket193.distance(
                        validPrev.getDouble("latitude"),
                        validPrev.getDouble("longitude"),
                        fix.getDouble("latitude"),
                        fix.getDouble("longitude")
                    )
                }
                val clean = JSONObject()
                for (field in arrayOf("latitude", "longitude", "accuracyMeters", "elapsedMillis", "epochMillis")) {
                    clean.put(field, fix.get(field))
                }
                points!!.put(clean)
                count++
                last = fix
                segment = nextSegment
            }
            stream = nextStream
            sequence = nextSequence
            if (packet.getBoolean("closed")) closed = true
        }
        return JSONObject()
            .put("key", key)
            .put("source", "watch_gps")
            .put("pointCount", count)
            .put("partial", partial || !closed)
            .put("distanceMeters", meters)
            .put("segments", sections)
    }

    /** Tombstone is durable before erasure. Late or repeated packets cannot recreate the track. */
    @Synchronized
    @Throws(Exception::class)
    fun delete(key: String?) {
        requireLedger(key != null && ROUTE_KEY_REGEX.matches(key))
        val nonNullKey = key!!
        val index = read(index(), INDEX_BYTES)
        val routes = routes(index)
        var entry = routes.optJSONObject(nonNullKey)
        if (entry == null) {
            requireLedger(routes.length() < MAX_ROUTES)
            entry = JSONObject().put("grants", JSONArray())
            routes.put(nonNullKey, entry)
        }
        entry.put("deleted", true)
        write(index(), index, INDEX_BYTES)
        val file = route(nonNullKey)
        file.delete()
        requireLedger(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists())
    }

    /** Also fences routes which have not reached the phone yet. No grant can resurrect this workout. */
    @Synchronized
    @Throws(Exception::class)
    fun deleteWorkout(workoutId: String?) {
        requireLedger(!workoutId.isNullOrEmpty() && workoutId.length <= 512)
        val nonNullWorkoutId = workoutId!!
        val index = read(index(), INDEX_BYTES)
        val routes = routes(index)
        if (!index.has("deletedWorkouts")) index.put("deletedWorkouts", JSONObject())
        val deleted = index.getJSONObject("deletedWorkouts")
        val digest = GpsRoutePacket193.digest(nonNullWorkoutId)
        requireLedger(deleted.has(digest) || deleted.length() < MAX_ROUTES)
        deleted.put(digest, true)
        val keys = ArrayList<String>()
        val names = routes.keys()
        while (names.hasNext()) {
            val key = names.next()
            val entry = routes.getJSONObject(key)
            val pending = entry.optJSONObject("pendingSource")
            val grants = entry.optJSONArray("grants")
            var matches = pending != null && nonNullWorkoutId == pending.optString("workoutId")
            if (grants != null) {
                for (i in 0 until grants.length()) {
                    if (nonNullWorkoutId == grants.getJSONObject(i).optString("workoutId")) {
                        matches = true
                    }
                }
            }
            if (matches) {
                entry.put("deleted", true)
                keys.add(key)
            }
        }
        write(index(), index, INDEX_BYTES)
        var failure: Exception? = null
        for (key in keys) {
            try {
                val file = route(key)
                file.delete()
                requireLedger(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists())
            } catch (error: Exception) {
                failure = error
            }
        }
        if (failure != null) throw failure
    }
}
