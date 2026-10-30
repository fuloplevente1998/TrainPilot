package com.repforge.shared

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/** One outstanding transfer, with the exact retry envelope committed before transport. */
class OwnershipJournal193(context: Context, private val host: Host) {
    interface Host {
        @Throws(Exception::class)
        fun current(): JSONObject?

        @Throws(Exception::class)
        fun install(value: JSONObject)
    }

    private val file = AtomicFile(File(context.noBackupFilesDir, "ownership193.json"))

    companion object {
        private const val MAX_STORAGE_BYTES = 512_000
        private const val MAX_ENVELOPE_BYTES = 96_000
        private val ID_REGEX = Regex("[A-Za-z0-9._:-]{1,128}")

        private fun requireJournal(value: Boolean) {
            if (!value) throw IllegalStateException("Ownership transfer changed")
        }

        private fun matching(a: JSONObject?, b: JSONObject?): Boolean {
            return a != null &&
                b != null &&
                a.optString("id") == b.optString("id") &&
                a.optString("workoutId") == b.optString("workoutId") &&
                a.optLong("epoch") == b.optLong("epoch")
        }

        private fun matchingWorkout(request: JSONObject?, snapshot: JSONObject?): Boolean {
            return request != null &&
                snapshot != null &&
                request.optString("workoutId") == snapshot.optString("workoutId") &&
                request.optLong("epoch") == WorkoutOwnership193.epoch(snapshot)
        }

        @Throws(Exception::class)
        private fun envelope(
            kind: String,
            request: JSONObject,
            snapshot: JSONObject?,
            sender: String,
            target: String
        ): JSONObject {
            val value = WorkoutOwnership193.copy(request)
            value.put("protocol", 1).put("kind", kind).put("sender", sender).put("target", target)
            value.remove("snapshot")
            if (snapshot != null) value.put("snapshot", snapshot)
            requireJournal(value.toString().toByteArray(StandardCharsets.UTF_8).size <= MAX_ENVELOPE_BYTES)
            return value
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun read(): JSONObject {
        val bakFile = File(file.baseFile.path + ".bak")
        if (!file.baseFile.exists() && !bakFile.exists()) return JSONObject()
        if (file.baseFile.length() > MAX_STORAGE_BYTES || bakFile.length() > MAX_STORAGE_BYTES) {
            throw IllegalStateException("Ownership journal exceeds storage budget")
        }
        val bytes = file.readFully()
        if (bytes.size > MAX_STORAGE_BYTES) {
            throw IllegalStateException("Ownership journal exceeds storage budget")
        }
        return JSONObject(String(bytes, StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    private fun write(value: JSONObject) {
        val bytes = value.toString().toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_STORAGE_BYTES) {
            throw IllegalStateException("Ownership journal exceeds storage budget")
        }
        var output: FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(bytes)
            output.fd.sync()
            file.finishWrite(output)
        } catch (error: Exception) {
            if (output != null) file.failWrite(output)
            throw error
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun request(localNode: String, peerNode: String, now: Long): JSONObject {
        val current = host.current()
        requireJournal(current != null && current.optBoolean("active", true))
        val nonNullCurrent = current!!
        var state = read()
        val previous = state.optJSONObject("request")
        if (WorkoutOwnership193.pending(nonNullCurrent)) return state
        requireJournal(localNode != peerNode)
        val request = JSONObject()
            .put("protocol", 1)
            .put("kind", "request")
            .put(
                "id",
                if (matchingWorkout(previous, nonNullCurrent)) previous!!.getString("id")
                else UUID.randomUUID().toString()
            )
            .put("workoutId", nonNullCurrent.getString("workoutId"))
            .put("epoch", WorkoutOwnership193.epoch(nonNullCurrent))
            .put("sender", localNode)
            .put("target", peerNode)
            .put("createdAt", now)
        state = JSONObject()
            .put("request", request)
            .put("outgoing", request)
            .put("stage", "requested")
        write(state)
        return state
    }

    /** Called only after the source actually stopped its timers/GPS and saved all measured values. */
    @Synchronized
    @Throws(Exception::class)
    fun prepare(frozen: JSONObject, localNode: String, now: Long): JSONObject {
        val state = read()
        val request = state.optJSONObject("request")
        requireJournal(
            request != null &&
                localNode == request.optString("target") &&
                matchingWorkout(request, frozen) &&
                now - request.optLong("createdAt") <= 30_000L &&
                now >= request.optLong("createdAt") - 5_000L &&
                frozen.optBoolean("workoutPaused")
        )
        val nonNullRequest = request!!
        val offered = WorkoutOwnership193.offer(
            frozen,
            frozen.optString("timerOwner", "phone"),
            localNode,
            nonNullRequest.getString("sender"),
            nonNullRequest.getString("id"),
            nonNullRequest.getLong("epoch"),
            now
        )
        state.put("snapshot", offered)
            .put("stage", "offered")
            .put("outgoing", envelope("offered", nonNullRequest, offered, localNode, nonNullRequest.getString("sender")))
        write(state)
        host.install(offered)
        return state
    }

    /** Messages are bound to the DataItem's actual origin node, not an unauthenticated payload field. */
    @Synchronized
    @Throws(Exception::class)
    fun receive(message: JSONObject, origin: String, localNode: String, now: Long): JSONObject {
        requireJournal(
            message.optInt("protocol") == 1 &&
                origin == message.optString("sender") &&
                localNode == message.optString("target") &&
                origin != localNode &&
                ID_REGEX.matches(message.optString("id"))
        )
        var state = read()
        val request = state.optJSONObject("request")
        val current = host.current()
        requireJournal(
            current != null &&
                current.optBoolean("active", true) &&
                current.optString("workoutId") == message.optString("workoutId")
        )
        val nonNullCurrent = current!!
        val kind = message.optString("kind")
        if ("request" == kind) {
            requireJournal(
                matchingWorkout(message, nonNullCurrent) &&
                    now - message.optLong("createdAt") <= 30_000L &&
                    now >= message.optLong("createdAt") - 5_000L
            )
            if (WorkoutOwnership193.pending(nonNullCurrent)) {
                requireJournal(matching(request, message))
                return state
            }
            requireJournal(origin != localNode)
            state = JSONObject().put("request", message).put("stage", "incoming")
            write(state)
            return state
        }
        requireJournal(matching(request, message))
        val nonNullRequest = request!!
        val remote = message.optJSONObject("snapshot")
        val next: JSONObject
        when (kind) {
            "offered" -> {
                requireJournal(
                    localNode == nonNullRequest.getString("sender") &&
                        origin == nonNullRequest.getString("target") &&
                        now - nonNullRequest.getLong("createdAt") <= 30_000L
                )
                if (WorkoutOwnership193.epoch(nonNullCurrent) > nonNullRequest.getLong("epoch")) {
                    return state
                }
                next = WorkoutOwnership193.accept(nonNullCurrent, remote, localNode, origin)
                state.put("stage", "accepted")
                    .put("snapshot", next)
                    .put("outgoing", envelope("accepted", nonNullRequest, next, localNode, origin))
            }
            "accepted" -> {
                requireJournal(
                    localNode == nonNullRequest.getString("target") &&
                        origin == nonNullRequest.getString("sender")
                )
                next = WorkoutOwnership193.commit(state.getJSONObject("snapshot"), remote, localNode, origin)
                state.put("stage", "committed")
                    .put("snapshot", next)
                    .put("outgoing", envelope("committed", nonNullRequest, next, localNode, origin))
            }
            "committed" -> {
                requireJournal(
                    localNode == nonNullRequest.getString("sender") &&
                        origin == nonNullRequest.getString("target")
                )
                next = WorkoutOwnership193.finish(nonNullCurrent, remote, localNode, origin)
                state.put("stage", "installed")
                    .put("snapshot", next)
                    .put("outgoing", envelope("installed", nonNullRequest, null, localNode, origin))
            }
            "installed" -> {
                requireJournal(
                    "committed" == state.optString("stage") &&
                        localNode == nonNullRequest.getString("target") &&
                        origin == nonNullRequest.getString("sender")
                )
                state.remove("outgoing")
                state.put("stage", "settled")
                write(state)
                return state
            }
            else -> throw IllegalArgumentException("Unsupported ownership message")
        }
        write(state)
        if ("accepted" != kind ||
            WorkoutOwnership193.epoch(nonNullCurrent) < WorkoutOwnership193.epoch(next) ||
            WorkoutOwnership193.pending(nonNullCurrent)
        ) {
            host.install(next)
        }
        return state
    }

    @Synchronized
    @Throws(Exception::class)
    fun installedReceiptPublished(id: String) {
        val state = read()
        val outgoing = state.optJSONObject("outgoing")
        if (outgoing != null && "installed" == outgoing.optString("kind") && id == outgoing.optString("id")) {
            state.remove("outgoing")
            write(state)
        }
    }

    /** Recover the crash window after journal commit and before the host's snapshot write. */
    @Synchronized
    @Throws(Exception::class)
    fun recover() {
        val saved = read().optJSONObject("snapshot")
        val current = host.current()
        if (saved == null || !saved.optBoolean("active", true)) return
        if (current == null) {
            host.install(saved)
            return
        }
        if (saved.optString("workoutId") != current.optString("workoutId")) return
        if (WorkoutOwnership193.epoch(current) > WorkoutOwnership193.epoch(saved)) return
        val sh = WorkoutOwnership193.transfer(saved)
        val ch = WorkoutOwnership193.transfer(current)
        if (WorkoutOwnership193.epoch(current) < WorkoutOwnership193.epoch(saved) ||
            (sh != null && (ch == null ||
                sh.optString("id") != ch.optString("id") ||
                ("accepted" == sh.optString("state") && "offered" == ch.optString("state"))))
        ) {
            host.install(saved)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun close(workoutId: String) {
        val state = read()
        val saved = state.optJSONObject("snapshot")
        val request = state.optJSONObject("request")
        if (saved != null && workoutId == saved.optString("workoutId")) {
            saved.put("active", false)
        }
        if ((saved != null && workoutId == saved.optString("workoutId")) ||
            (request != null && workoutId == request.optString("workoutId"))
        ) {
            state.remove("outgoing")
            state.remove("request")
            state.put("stage", "closed")
            write(state)
        }
    }
}
