package com.repforge.shared

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.UUID

/** Watch-side coordinates stay in no-backup files; the existing command outbox holds pointers only. */
class GpsRouteSpool193 private constructor(noBackupDirectory: File) {
    fun interface Queue {
        @Throws(Exception::class)
        fun enqueue(pointer: JSONObject): Boolean
    }

    private val directory = File(noBackupDirectory, "wear_gps_spool193")

    companion object {
        private val instances = HashMap<String, GpsRouteSpool193>()
        private const val MAX_PACKETS = 512
        private const val USAGE_BYTES = 1024 * 1024
        private val PACKET_ID_REGEX = Regex("[a-f0-9]{64}")
        private val ROUTE_KEY_REGEX = Regex("wear193:[a-f0-9]{64}")

        @JvmStatic
        @Synchronized
        @Throws(Exception::class)
        fun open(directory: File): GpsRouteSpool193 {
            val path = directory.canonicalPath
            return instances.getOrPut(path) { GpsRouteSpool193(File(path)) }
        }

        private fun requireStorage(value: Boolean) {
            if (!value) throw IllegalStateException("Route recording storage unavailable or full")
        }

        @Throws(Exception::class)
        private fun commandId(packet: JSONObject): String {
            return GpsRoutePacket193.digest(
                JSONArray()
                    .put(packet.getString("routeKey"))
                    .put(packet.getString("streamId"))
                    .put(packet.getLong("sequence"))
                    .toString()
            )
        }

        @Throws(Exception::class)
        private fun packet(active: JSONObject, closed: Boolean, partial: Boolean): JSONObject {
            val packet = JSONObject(active.getJSONObject("meta").toString())
            packet.put("schema", 1)
                .put("streamId", active.getString("streamId"))
                .put("sequence", active.getLong("sequence"))
                .put("closed", closed)
                .put("partial", partial)
                .put("points", JSONArray(active.getJSONArray("points").toString()))
            return GpsRoutePacket193.validate(
                packet,
                active.getJSONObject("meta"),
                packet.getString("sourceNode"),
                packet.getString("targetNode")
            )
        }

        @JvmStatic
        @Throws(Exception::class)
        fun pointer(packet: JSONObject): JSONObject {
            val points = packet.getJSONArray("points")
            val createdAt = if (points.length() > 0) {
                points.getJSONObject(0).getLong("epochMillis")
            } else {
                System.currentTimeMillis()
            }
            return JSONObject()
                .put("schema", 1)
                .put("action", "gpsRouteChunk193")
                .put("commandId", commandId(packet))
                .put("workoutId", packet.getString("workoutId"))
                .put("ownerEpoch193", packet.getLong("ownerEpoch193"))
                .put("createdAt", createdAt)
                .put(
                    "route193",
                    JSONObject()
                        .put("routeKey", packet.getString("routeKey"))
                        .put("streamId", packet.getString("streamId"))
                        .put("sequence", packet.getLong("sequence"))
                        .put("sourceNode", packet.getString("sourceNode"))
                        .put("targetNode", packet.getString("targetNode"))
                        .put("hash", GpsRouteLedger193.fingerprint(packet))
                )
        }
    }

    private fun activeFile(): AtomicFile = AtomicFile(File(directory, "active.json"))
    private fun usageFile(): AtomicFile = AtomicFile(File(directory, "usage.json"))

    private fun packetFile(id: String?): AtomicFile {
        requireStorage(id != null && PACKET_ID_REGEX.matches(id))
        return AtomicFile(File(directory, "$id.packet"))
    }

    @Throws(Exception::class)
    private fun revokedFile(key: String): AtomicFile {
        requireStorage(ROUTE_KEY_REGEX.matches(key))
        return AtomicFile(File(directory, GpsRoutePacket193.digest(key) + ".revoked"))
    }

    @Throws(Exception::class)
    fun revoked(key: String): Boolean {
        val file = revokedFile(key)
        return file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
    }

    @Throws(Exception::class)
    private fun read(file: AtomicFile, budget: Int): JSONObject {
        val bakFile = File(file.baseFile.path + ".bak")
        if (!file.baseFile.exists() && !bakFile.exists()) return JSONObject()
        requireStorage(file.baseFile.length() <= budget && bakFile.length() <= budget)
        val bytes = file.readFully()
        requireStorage(bytes.size <= budget)
        return JSONObject(String(bytes, StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    private fun write(file: AtomicFile, value: JSONObject, budget: Int) {
        val bytes = value.toString().toByteArray(StandardCharsets.UTF_8)
        requireStorage(bytes.size <= budget)
        requireStorage(directory.isDirectory || directory.mkdirs())
        var out: FileOutputStream? = null
        try {
            out = file.startWrite()
            out.write(bytes)
            out.fd.sync()
            file.finishWrite(out)
            if (!file.baseFile.isFile || !Arrays.equals(bytes, file.readFully())) {
                throw IOException("Route write was not durable")
            }
        } catch (error: Exception) {
            if (out != null) file.failWrite(out)
            throw error
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun active(): JSONObject {
        return read(activeFile(), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
    }

    @Throws(Exception::class)
    private fun usage(): JSONObject {
        return read(usageFile(), USAGE_BYTES)
    }

    /** Begin only on an explicit permitted foreground action; a previous stream must first be sealed. */
    @Synchronized
    @Throws(Exception::class)
    fun begin(metadata: JSONObject, queue: Queue) {
        if (active().has("meta")) close(true, queue)
        val usage = usage()
        val key = metadata.getString("routeKey")
        val route = usage.optJSONObject(key)
        requireStorage(!revoked(key) && (route == null || !route.optBoolean("deleted")))
        if (route == null) requireStorage(usage.length() < GpsRouteLedger193.MAX_ROUTES)
        var points = 0
        var segments = 0
        val streams = route?.optJSONObject("streams") ?: JSONObject()
        requireStorage(streams.length() < GpsRoutePacket193.MAX_SEGMENTS)
        val keys = streams.keys()
        while (keys.hasNext()) {
            val prior = streams.getJSONObject(keys.next())
            points += prior.getInt("points")
            segments += prior.getInt("segments")
        }
        requireStorage(points < GpsRoutePacket193.MAX_POINTS && segments < GpsRoutePacket193.MAX_SEGMENTS)
        val next = JSONObject()
            .put("meta", JSONObject(metadata.toString()))
            .put("streamId", UUID.randomUUID().toString())
            .put("sequence", 1)
            .put("points", JSONArray())
            .put("accepted", 0)
            .put("segments", 0)
            .put("basePoints", points)
            .put("baseSegments", segments)
        packet(next, closed = true, partial = false)
        write(activeFile(), next, GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
    }

    /** Called by the real-fix sampler; the point is durable before its measured counters advance. */
    @Synchronized
    @Throws(Exception::class)
    fun append(point: JSONObject, queue: Queue) {
        var current = active()
        requireStorage(current.has("meta") && !current.optBoolean("closing"))
        if (current.getJSONArray("points").length() >= GpsRoutePacket193.CHUNK_POINTS) {
            flushOpen(queue)
            current = active()
        }
        requireStorage(
            current.getInt("basePoints") + current.getInt("accepted") < GpsRoutePacket193.MAX_POINTS &&
                current.getInt("baseSegments") + point.getInt("segment") <= GpsRoutePacket193.MAX_SEGMENTS
        )
        current.getJSONArray("points").put(JSONObject(point.toString()))
        current.put("accepted", current.getInt("accepted") + 1)
            .put("segments", point.getInt("segment"))
        packet(current, closed = false, partial = false)
        write(activeFile(), current, GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
        if (current.getJSONArray("points").length() == GpsRoutePacket193.CHUNK_POINTS) {
            flushOpen(queue)
        }
    }

    @Throws(Exception::class)
    private fun enqueue(packet: JSONObject, queue: Queue) {
        val id = commandId(packet)
        val file = packetFile(id)
        val previous = read(file, GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
        if (previous.has("packet")) {
            requireStorage(
                GpsRouteLedger193.fingerprint(previous.getJSONObject("packet")) ==
                    GpsRouteLedger193.fingerprint(packet)
            )
        } else {
            val files = directory.listFiles { _, name ->
                name.endsWith(".packet") || name.endsWith(".packet.bak")
            }
            requireStorage(files != null && files.size < MAX_PACKETS)
            write(
                file,
                JSONObject().put("packet", packet).put("pointer", pointer(packet)),
                GpsRoutePacket193.MAX_PACKET_BYTES + 2048
            )
        }
        requireStorage(
            queue.enqueue(
                read(file, GpsRoutePacket193.MAX_PACKET_BYTES + 2048).getJSONObject("pointer")
            )
        )
    }

    @Throws(Exception::class)
    private fun flushOpen(queue: Queue) {
        val current = active()
        requireStorage(
            !current.optBoolean("closing") &&
                current.getJSONArray("points").length() == GpsRoutePacket193.CHUNK_POINTS
        )
        enqueue(packet(current, closed = false, partial = false), queue)
        current.put("points", JSONArray()).put("sequence", current.getLong("sequence") + 1L)
        write(activeFile(), current, GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
    }

    /** Closing intent, packet, outbox pointer, usage counters, then active clear: every crash is retryable. */
    @Synchronized
    @Throws(Exception::class)
    fun close(partial: Boolean, queue: Queue) {
        var current = active()
        if (!current.has("meta")) return
        if (current.getInt("accepted") == 0) {
            write(activeFile(), JSONObject(), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
            return
        }
        if (current.getJSONArray("points").length() == GpsRoutePacket193.CHUNK_POINTS) {
            flushOpen(queue)
            current = active()
        }
        if (!current.optBoolean("closing")) {
            current.put("closing", true).put("partial", partial)
            write(activeFile(), current, GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
        }
        enqueue(packet(current, closed = true, partial = current.getBoolean("partial")), queue)
        val usage = usage()
        val key = current.getJSONObject("meta").getString("routeKey")
        var route = usage.optJSONObject(key)
        if (route == null) {
            requireStorage(usage.length() < GpsRouteLedger193.MAX_ROUTES)
            route = JSONObject().put("streams", JSONObject())
            usage.put(key, route)
        }
        requireStorage(!route.optBoolean("deleted"))
        route.getJSONObject("streams").put(
            current.getString("streamId"),
            JSONObject()
                .put("points", current.getInt("accepted"))
                .put("segments", current.getInt("segments"))
        )
        write(usageFile(), usage, USAGE_BYTES)
        write(activeFile(), JSONObject(), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
    }

    @Synchronized
    @Throws(Exception::class)
    fun payload(commandId: String?): JSONObject? {
        return read(packetFile(commandId), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
            .optJSONObject("packet")
    }

    /** Immutable file, read after commit; no spool lock under the command-outbox lock. */
    @Throws(Exception::class)
    fun wirePayload(commandId: String?): JSONObject? {
        val packet = read(packetFile(commandId), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
            .optJSONObject("packet")
        return if (packet != null && !revoked(packet.getString("routeKey"))) packet else null
    }

    @Synchronized
    @Throws(Exception::class)
    fun ready(pointer: JSONObject): Boolean {
        val packet = payload(pointer.optString("commandId")) ?: return false
        return GpsRouteLedger193.fingerprint(packet) ==
            pointer.getJSONObject("route193").getString("hash")
    }

    @Throws(Exception::class)
    fun recover(queue: Queue) {
        recover(queue, true)
    }

    @Synchronized
    @Throws(Exception::class)
    fun recover(queue: Queue, sealActive: Boolean) {
        val files = directory.listFiles { _, name ->
            name.endsWith(".packet") || name.endsWith(".packet.bak")
        }
        if (files != null) {
            for (file in files) {
                val id = file.name.substring(0, 64)
                val stored = read(packetFile(id), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
                if (revoked(stored.getJSONObject("packet").getString("routeKey"))) {
                    acknowledged(id)
                    continue
                }
                requireStorage(queue.enqueue(stored.getJSONObject("pointer")))
            }
        }
        val active = active()
        if (active.has("meta") && revoked(active.getJSONObject("meta").getString("routeKey"))) {
            write(activeFile(), JSONObject(), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
        }
        if (sealActive) {
            close(true, queue)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun acknowledged(id: String?) {
        val file = packetFile(id)
        file.delete()
        requireStorage(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists())
    }

    /** Phone deletion is durable before local erasure and also fences concurrent outbox sends. */
    @Synchronized
    @Throws(Exception::class)
    fun revoke(key: String) {
        write(revokedFile(key), JSONObject().put("deleted", true), 128)
        val current = active()
        if (current.has("meta") && key == current.getJSONObject("meta").getString("routeKey")) {
            write(activeFile(), JSONObject(), GpsRoutePacket193.MAX_PACKET_BYTES + 2048)
        }
        val files = directory.listFiles { _, name ->
            name.endsWith(".packet") || name.endsWith(".packet.bak")
        }
        if (files != null) {
            for (file in files) {
                val id = file.name.substring(0, 64)
                val packet = payload(id)
                if (packet != null && key == packet.getString("routeKey")) {
                    acknowledged(id)
                }
            }
        }
        val usage = usage()
        var entry = usage.optJSONObject(key)
        if (entry == null) {
            requireStorage(usage.length() < GpsRouteLedger193.MAX_ROUTES)
            entry = JSONObject().put("streams", JSONObject())
            usage.put(key, entry)
        }
        entry.put("deleted", true)
        write(usageFile(), usage, USAGE_BYTES)
    }
}
