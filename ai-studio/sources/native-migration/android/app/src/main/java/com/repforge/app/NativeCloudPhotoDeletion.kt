package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.util.ArrayDeque
import java.util.IdentityHashMap

/** In-process photo-reference admission, not a distributed Drive lock.
 * Register under the existing metadata lease and canonical snapshot locks, release after HTTP returns.
 * No network runs under those locks. Normal settings/sets/new UUID photos remain writable.
 */
internal object NativeCloudPhotoDeletion {
    private class Permit(val id: String, val driveId: String) : Closeable {
        override fun close() {
            synchronized(NativeCloudPhotoDeletion) {
                if (pending[id] === this) pending = pending - id
            }
        }
    }
    @Volatile private var pending: Map<String, Permit> = emptyMap()
    private val PHOTO_ID = Regex("[a-f0-9-]{36}")
    private val DRIVE_ID = Regex("[A-Za-z0-9_-]+")

    /** Called with NativeDataReplacement's mutex held, before setting any restore/erase fences. */
    fun requireNoPendingDeletes() {
        check(pending.isEmpty()) { "Felhős fotótörlés van folyamatban. A visszaállítást vagy törlést utána próbáld újra." }
    }

    /** Room import/save callers hold the metadata lease; canonical callers hold their store monitor. */
    fun requireUnreservedReferences(data: JSONObject) {
        for (permit in pending.values) check(!hasLiveReference(data, permit.id, permit.driveId)) {
            "A fotó felhős törlése még folyamatban van. A hivatkozás mentését próbáld újra."
        }
    }

    /** Canonical frames store progress photos only at the root. Do not scan sets/GPS on timer ticks. */
    fun requireUnreservedSnapshot(snapshot: JSONObject) {
        for (permit in pending.values) check(!hasPhotoReference(snapshot.optJSONArray("photos"), permit.id, permit.driveId)) {
            "A fotó felhős törlése még folyamatban van. A hivatkozás mentését próbáld újra."
        }
    }

    private fun hasPhotoReference(photos: JSONArray?, id: String, driveId: String): Boolean {
        if (photos == null) return false
        for (index in 0 until photos.length()) {
            val photo = photos.optJSONObject(index) ?: continue
            if (!BackupRestoreManager.photoDeleted(photo) &&
                (photo.optString("id") == id || photo.optString("driveFileId") == driveId)) return true
        }
        return false
    }

    private fun hasLiveReference(root: JSONObject, id: String, driveId: String): Boolean {
        val stack = ArrayDeque<Any>().apply { add(root) }
        val visited = IdentityHashMap<Any, Boolean>()
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (visited.put(node, true) != null) continue
            when (node) {
                is JSONObject -> {
                    if (hasPhotoReference(node.optJSONArray("photos"), id, driveId)) return true
                    for (key in node.keys()) {
                        val child = node.opt(key)
                        if (child is JSONObject || child is JSONArray) stack.add(child)
                    }
                }
                is JSONArray -> for (index in 0 until node.length()) {
                    val child = node.opt(index)
                    if (child is JSONObject || child is JSONArray) stack.add(child)
                }
            }
        }
        return false
    }

    private fun requireUnreferenced(root: JSONObject?, id: String, driveId: String) {
        check(root == null || !hasLiveReference(root, id, driveId)) {
            "A fotóra új hivatkozás érkezett. Indítsd újra a felhőszinkronizálást; a fotó nem lett törölve."
        }
    }

    suspend fun deleteUnreferenced(context: Context, id: String, driveId: String,
        continuation: NativeDataReplacement.Continuation, delete: () -> Unit) {
        require(PHOTO_ID.matches(id) && DRIVE_ID.matches(driveId)) { "Invalid cloud photo identity" }
        val app = context.applicationContext
        var permit: Permit? = null
        try {
            currentCoroutineContext().ensureActive()
            NativeDataReplacement.mutate(app) {
                continuation.requireCurrent(app)
                val db = TrainPilotDatabase.getInstance(app)
                check(!db.workoutHistoryDao().hasLiveCloudPhotoReference(id, driveId)) {
                    "A fotó újra használatban van. Indítsd újra a felhőszinkronizálást."
                }
                val draft = db.keyValueDocumentDao().getDocument("draft")
                if (draft != null) requireUnreferenced(JSONObject(draft.json), id, driveId)
                withContext(Dispatchers.Main.immediate) {
                    // Existing lock order: ownership journal -> public snapshot -> canonical execution.
                    val journal = PhoneOwnership193.journal(app)
                    synchronized(journal) {
                        synchronized(ActiveWorkoutStore) {
                            val execution = PhoneWorkoutExecution.store(app)
                            synchronized(execution) {
                                continuation.requireCurrent(app)
                                val snapshots = listOf(ActiveWorkoutStore.current(app), execution.snapshot(),
                                    journal.read().optJSONObject("snapshot"))
                                for (snapshot in snapshots) if (snapshot?.optBoolean("active", true) == true)
                                    requireUnreferenced(snapshot, id, driveId)
                                synchronized(this@NativeCloudPhotoDeletion) {
                                    check(pending.values.none { it.id == id || it.driveId == driveId }) { "Photo deletion already pending" }
                                    Permit(id, driveId).also { pending = pending + (id to it); permit = it }
                                }
                            }
                        }
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            // Synchronous transport: a cancelled coroutine cannot release admission while its call still runs.
            delete()
            currentCoroutineContext().ensureActive()
        } finally { permit?.close() }
    }
}
