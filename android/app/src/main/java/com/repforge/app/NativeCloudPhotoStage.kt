package com.repforge.app

import android.content.Context
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Network-phase files only. Live photo changes belong to BackupRestoreManager's durable redo. */
internal class NativeCloudPhotoStage(private val context: Context, data: JSONObject) : Closeable {
    internal data class Reference(val id: String, val rows: List<JSONObject>, val deleted: Boolean, val driveId: String)
    companion object {
        private val ID = Regex("[a-f0-9-]{36}")
        private val DRIVE_ID = Regex("[A-Za-z0-9_-]+")
        /** Supported workout-photo roots: journal plus the resumable imported draft, not library assets. */
        internal fun photoRows(data: JSONObject): List<JSONObject> {
            val rows = ArrayList<JSONObject>()
            fun append(workout: JSONObject) {
                val photos = workout.optJSONArray("photos") ?: return
                for (index in 0 until photos.length()) rows.add(photos.getJSONObject(index))
            }
            val history = data.getJSONArray("history")
            for (index in 0 until history.length()) append(history.getJSONObject(index))
            data.optJSONObject("draft")?.let { draft ->
                append(draft)
                draft.optJSONObject("session")?.let(::append)
            }
            return rows
        }

        internal fun references(data: JSONObject): List<Reference> {
            val byId = linkedMapOf<String, MutableList<JSONObject>>()
            for (row in photoRows(data)) {
                val id = row.optString("id")
                if (!ID.matches(id)) {
                    if (BackupRestoreManager.photoDeleted(row)) continue
                    throw IOException("Invalid photo reference.")
                }
                byId.getOrPut(id) { mutableListOf() }.add(row)
            }
            val refs = byId.map { (id, rows) ->
                val live = rows.filterNot { BackupRestoreManager.photoDeleted(it) }
                val relevant = live.ifEmpty { rows }
                val remoteIds = relevant.map { if (it.isNull("driveFileId")) "" else it.optString("driveFileId") }
                    .filter { it.isNotEmpty() }.distinct()
                if (remoteIds.size > 1 || remoteIds.any { !DRIVE_ID.matches(it) }) throw IOException("Conflicting photo identity.")
                Reference(id, relevant, live.isEmpty(), remoteIds.singleOrNull().orEmpty())
            }
            if (refs.count { !it.deleted } > 5000) throw IOException("Too many photos.")
            return refs
        }

        /** Verify Google's binary checksum before publishing even a staged download. */
        internal fun installDownload(id: String, metadata: JSONObject, bytes: ByteArray, destination: File) {
            val expected = metadata.optString("md5Checksum")
            val actual = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
            if (!ID.matches(id) || metadata.optString("name") != "trainpilot-photo-$id.jpg" ||
                metadata.optString("mimeType") != "image/jpeg" || metadata.optLong("size", -1) != bytes.size.toLong() ||
                bytes.size !in 1..BackupRestoreManager.PHOTO_LIMIT || !Regex("[a-fA-F0-9]{32}").matches(expected) ||
                !expected.equals(actual, ignoreCase = true)) throw IOException("The downloaded photo failed verification.")
            val temp = File(destination.parentFile, "$id.tmp")
            try {
                FileOutputStream(temp).use { it.write(bytes); it.fd.sync() }
                if (!temp.renameTo(destination)) throw IOException("Cannot stage photo.")
            } finally { temp.delete() }
        }
    }

    private val refs = references(data)
    private val root = File(context.noBackupFilesDir, "cloud_photo_staging/${UUID.randomUUID()}")
    internal val photos = File(root, "photos")
    internal val deletedPhotoIds = refs.filter { it.deleted }.map { it.id }.toSet()
    private val localVersions = linkedMapOf<String, String?>()
    private val stagedVersions = linkedMapOf<String, String>()
    private var prepared = false
    private var installed = false

    init { if (!photos.mkdirs()) throw IOException("Cannot create cloud photo staging directory.") }

    private fun local(id: String) = File(context.filesDir, "workout_photos/$id.jpg")
    private fun fingerprint(file: File): String? {
        if (!file.exists()) return null
        if (!file.isFile || file.length() > BackupRestoreManager.PHOTO_LIMIT) throw IOException("Invalid local photo.")
        return FileInputStream(file).use { ArchiveIO.hash(it) }
    }

    fun prepare(upload: (String, File) -> String, download: (String, String, File) -> Unit) {
        check(!prepared)
        var total = 0L
        for (ref in refs) {
            val source = local(ref.id)
            localVersions[ref.id] = fingerprint(source)
            if (ref.deleted) continue
            val target = File(photos, "${ref.id}.jpg")
            val driveId = if (source.isFile) {
                FileInputStream(source).use { input -> FileOutputStream(target).use { out ->
                    ArchiveIO.copy(input, out, BackupRestoreManager.PHOTO_LIMIT.toLong()); out.fd.sync()
                } }
                if (target.length() == 0L) throw IOException("Empty photo.")
                check(fingerprint(target) == localVersions[ref.id]) { "Local photo changed while staging." }
                // Verify/recover stale Drive IDs against the immutable local copy as well.
                upload(ref.id, target).also { if (!DRIVE_ID.matches(it)) throw IOException("Invalid uploaded photo identity.") }
            } else {
                if (ref.driveId.isEmpty()) throw IOException("A referenced photo is unavailable.")
                download(ref.id, ref.driveId, target)
                ref.driveId
            }
            if (!target.isFile || target.length() !in 1L..BackupRestoreManager.PHOTO_LIMIT.toLong())
                throw IOException("Missing staged photo.")
            total += target.length()
            if (total > BackupRestoreManager.ARCHIVE_LIMIT) throw IOException("Photo staging exceeds archive limit.")
            val stagedHash = requireNotNull(fingerprint(target))
            check(localVersions[ref.id] == null || stagedHash == localVersions[ref.id]) { "Staged upload bytes changed." }
            stagedVersions[ref.id] = stagedHash
            for (row in ref.rows) row.put("driveFileId", driveId)
        }
        prepared = true
    }

    /** Call under the replacement lease, after the canonical metadata comparison. */
    fun requireUnchanged() {
        check(prepared)
        for ((id, expected) in localVersions) check(fingerprint(local(id)) == expected) { "Local photo changed during cloud sync." }
        for ((id, expected) in stagedVersions) check(fingerprint(File(photos, "$id.jpg")) == expected) { "Staged photo changed." }
    }

    internal fun verifiedCopyHash(id: String, file: File): String {
        check(prepared)
        val expected = requireNotNull(stagedVersions[id]) { "Unexpected cloud photo." }
        check(fingerprint(file) == expected) { "Cloud photo copy changed." }
        return expected
    }

    internal fun markInstalled() { check(prepared); installed = true }

    /** Tombstone and Drive ID stay durable in the snapshot until a later retry succeeds. */
    suspend fun finishRemoteDeletes(delete: suspend (String, String) -> Unit) {
        check(prepared && installed) { "Local cloud photo commit is required before remote deletion." }
        for (ref in refs) if (ref.deleted && ref.driveId.isNotEmpty()) delete(ref.id, ref.driveId)
    }

    override fun close() { root.deleteRecursively() }
}
