package com.repforge.shared

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

class OwnershipJournal193(context: Context, private val host: Host) {
    interface Host {
        @Throws(Exception::class) fun current(): JSONObject?
        @Throws(Exception::class) fun install(value: JSONObject)
    }
    private val file = AtomicFile(File(context.noBackupFilesDir, "ownership193.json"))
    companion object {
        private const val MAX_STORAGE_BYTES = 512_000
        private const val MAX_ENVELOPE_BYTES = 96_000
        private val ID_REGEX = Regex("[A-Za-z0-9._:-]{1,128}")
        private fun requireJournal(v: Boolean) { if (!v) throw IllegalStateException("Ownership transfer changed") }
        private fun matching(a: JSONObject?, b: JSONObject?): Boolean = a != null && b != null && a.optString("id") == b.optString("id") && a.optString("workoutId") == b.optString("workoutId") && a.optLong("epoch") == b.optLong("epoch")
        private fun matchingWorkout(r: JSONObject?, s: JSONObject?): Boolean = r != null && s != null && r.optString("workoutId") == s.optString("workoutId") && r.optLong("epoch") == WorkoutOwnership193.epoch(s)
        @Throws(Exception::class) private fun envelope(kind: String, request: JSONObject, snapshot: JSONObject?, sender: String, target: String): JSONObject {
            val value = WorkoutOwnership193.copy(request); value.put("protocol", 1).put("kind", kind).put("sender", sender).put("target", target)
            value.remove("snapshot"); if (snapshot != null) value.put("snapshot", snapshot)
            requireJournal(value.toString().toByteArray(StandardCharsets.UTF_8).size <= MAX_ENVELOPE_BYTES); return value
        }
    }

    @Synchronized @Throws(Exception::class) fun read(): JSONObject {
        val bak = File(file.baseFile.path + ".bak"); if (!file.baseFile.exists() && !bak.exists()) return JSONObject()
        if (file.baseFile.length() > MAX_STORAGE_BYTES || bak.length() > MAX_STORAGE_BYTES) throw IllegalStateException("Ownership journal exceeds storage budget")
        val b = file.readFully(); if (b.size > MAX_STORAGE_BYTES) throw IllegalStateException("Ownership journal exceeds storage budget")
        return JSONObject(String(b, StandardCharsets.UTF_8))
    }

    @Throws(Exception::class) private fun write(value: JSONObject) {
        val b = value.toString().toByteArray(StandardCharsets.UTF_8); if (b.size > MAX_STORAGE_BYTES) throw IllegalStateException("Ownership journal exceeds storage budget")
        var out: FileOutputStream? = null
        try { out = file.startWrite(); out.write(b); out.fd.sync(); file.finishWrite(out) } catch (e: Exception) { if (out != null) file.failWrite(out); throw e }
    }

    @Synchronized @Throws(Exception::class) fun request(localNode: String, peerNode: String, now: Long): JSONObject {
        val current = host.current(); requireJournal(current != null && current.optBoolean("active", true))
        var state = read(); val prev = state.optJSONObject("request")
        if (WorkoutOwnership193.pending(current)) return state
        requireJournal(localNode != peerNode)
        val req = JSONObject().put("protocol", 1).put("kind", "request").put("id", if (matchingWorkout(prev, current)) prev!!.getString("id") else UUID.randomUUID().toString()).put("workoutId", current!!.getString("workoutId")).put("epoch", WorkoutOwnership193.epoch(current)).put("sender", localNode).put("target", peerNode).put("createdAt", now)
        state = JSONObject().put("request", req).put("outgoing", req).put("stage", "requested"); write(state); return state
    }

    @Synchronized @Throws(Exception::class) fun prepare(frozen: JSONObject, localNode: String, now: Long): JSONObject {
        val state = read(); val req = state.optJSONObject("request")
        requireJournal(req != null && localNode == req.optString("target") && matchingWorkout(req, frozen) && now - req.optLong("createdAt") <= 30_000L && now >= req.optLong("createdAt") - 5_000L && frozen.optBoolean("workoutPaused"))
        val offered = WorkoutOwnership193.offer(frozen, frozen.optString("timerOwner", "phone"), localNode, req!!.getString("sender"), req.getString("id"), req.getLong("epoch"), now)
        state.put("snapshot", offered).put("stage", "offered").put("outgoing", envelope("offered", req, offered, localNode, req.getString("sender")))
        write(state); host.install(offered); return state
    }

    @Synchronized @Throws(Exception::class) fun receive(message: JSONObject, origin: String, localNode: String, now: Long): JSONObject {
        requireJournal(message.optInt("protocol") == 1 && origin == message.optString("sender") && localNode == message.optString("target") && origin != localNode && ID_REGEX.matches(message.optString("id")))
        var state = read(); val req = state.optJSONObject("request"); val current = host.current()
        requireJournal(current != null && current.optBoolean("active", true) && current.optString("workoutId") == message.optString("workoutId"))
        val kind = message.optString("kind")
        if ("request" == kind) {
            requireJournal(matchingWorkout(message, current) && now - message.optLong("createdAt") <= 30_000L && now >= message.optLong("createdAt") - 5_000L)
            if (WorkoutOwnership193.pending(current)) { requireJournal(matching(req, message)); return state }
            requireJournal(origin != localNode); state = JSONObject().put("request", message).put("stage", "incoming"); write(state); return state
        }
        requireJournal(matching(req, message))
        val remote = message.optJSONObject("snapshot"); val next: JSONObject
        when (kind) {
            "offered" -> {
                requireJournal(localNode == req!!.getString("sender") && origin == req.getString("target") && now - req.getLong("createdAt") <= 30_000L)
                if (WorkoutOwnership193.epoch(current) > req.getLong("epoch")) return state
                next = WorkoutOwnership193.accept(current, remote, localNode, origin)
                state.put("stage", "accepted").put("snapshot", next).put("outgoing", envelope("accepted", req, next, localNode, origin))
            }
            "accepted" -> {
                requireJournal(localNode == req!!.getString("target") && origin == req.getString("sender"))
                next = WorkoutOwnership193.commit(state.getJSONObject("snapshot"), remote, localNode, origin)
                state.put("stage", "committed").put("snapshot", next).put("outgoing", envelope("committed", req, next, localNode, origin))
            }
            "committed" -> {
                requireJournal(localNode == req!!.getString("sender") && origin == req.getString("target"))
                next = WorkoutOwnership193.finish(current, remote, localNode, origin)
                state.put("stage", "installed").put("snapshot", next).put("outgoing", envelope("installed", req, null, localNode, origin))
            }
            "installed" -> {
                requireJournal("committed" == state.optString("stage") && localNode == req!!.getString("target") && origin == req.getString("sender"))
                state.remove("outgoing"); state.put("stage", "settled"); write(state); return state
            }
            else -> throw IllegalArgumentException("Unsupported ownership message")
        }
        write(state)
        if ("accepted" != kind || WorkoutOwnership193.epoch(current) < WorkoutOwnership193.epoch(next) || WorkoutOwnership193.pending(current)) host.install(next)
        return state
    }

    @Synchronized @Throws(Exception::class) fun installedReceiptPublished(id: String) {
        val state = read(); val out = state.optJSONObject("outgoing")
        if (out != null && "installed" == out.optString("kind") && id == out.optString("id")) { state.remove("outgoing"); write(state) }
    }

    @Synchronized @Throws(Exception::class) fun recover() {
        val saved = read().optJSONObject("snapshot"); val current = host.current()
        if (saved == null || !saved.optBoolean("active", true)) return
        if (current == null) { host.install(saved); return }
        if (saved.optString("workoutId") != current.optString("workoutId")) return
        if (WorkoutOwnership193.epoch(current) > WorkoutOwnership193.epoch(saved)) return
        val sh = WorkoutOwnership193.transfer(saved); val ch = WorkoutOwnership193.transfer(current)
        if (WorkoutOwnership193.epoch(current) < WorkoutOwnership193.epoch(saved) || (sh != null && (ch == null || sh.optString("id") != ch.optString("id") || ("accepted" == sh.optString("state") && "offered" == ch.optString("state"))))) host.install(saved)
    }

    @Synchronized @Throws(Exception::class) fun close(workoutId: String) {
        val state = read(); val saved = state.optJSONObject("snapshot"); val req = state.optJSONObject("request")
        if (saved != null && workoutId == saved.optString("workoutId")) saved.put("active", false)
        if ((saved != null && workoutId == saved.optString("workoutId")) || (req != null && workoutId == req.optString("workoutId"))) { state.remove("outgoing"); state.remove("request"); state.put("stage", "closed"); write(state) }
    }
}
