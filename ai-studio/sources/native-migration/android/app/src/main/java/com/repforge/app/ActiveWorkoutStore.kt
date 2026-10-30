package com.repforge.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

internal object ActiveWorkoutStore {
    private const val PREFS = "trainpilot_wear_sync"
    private const val SNAPSHOT = "snapshot"
    private const val REVISION = "revision"

    private val _snapshotFlow = MutableStateFlow<JSONObject?>(null)
    val snapshotFlow: StateFlow<JSONObject?> = _snapshotFlow.asStateFlow()

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun save(context: Context, input: JSONObject): JSONObject {
        // All ownership-aware writes take journal -> snapshot locks, including clear().
        synchronized(PhoneOwnership193.journal(context)) {
            if (!PhoneOwnership193.publicationAllowed(context, input)) {
                throw IllegalStateException("Reconcile workout ownership before publishing")
            }
            return saveOwned(context, input)
        }
    }

    @JvmStatic
    @Synchronized
    @Throws(Exception::class)
    fun saveOwned(context: Context, input: JSONObject): JSONObject {
        return saveSnapshot(context, input, finalizing = false)
    }

    /** Only the finalizer may advance a draft after the durable save fence is installed. */
    @Synchronized
    internal fun saveFinalizing(context: Context, input: JSONObject): JSONObject {
        check(input.optBoolean("finalizationPending")) { "Missing workout finalization fence" }
        return saveSnapshot(context, input, finalizing = true)
    }

    private fun saveSnapshot(context: Context, input: JSONObject, finalizing: Boolean): JSONObject {
        check(!NativeDataReplacement.blocked(context)) { "Adat-visszaállítás vagy törlés van folyamatban." }
        val previous = prefs(context).getString(SNAPSHOT, null)?.let { JSONObject(it) }
        if (previous?.optBoolean("active") == true && previous.optBoolean("finalizationPending")) {
            check(finalizing && previous.optString("workoutId") == input.optString("workoutId") && input.optBoolean("finalizationPending")) {
                "Workout finalization is in progress"
            }
        }
        val revision = prefs(context).getLong(REVISION, 0L) + 1L
        val snapshot = JSONObject(input.toString())
        PhoneGpsRoute193.captureOrigin(context, snapshot)
        snapshot.put("schema", snapshot.optInt("schema", 1))
        snapshot.put("revision", revision)
        snapshot.put("active", true)
        snapshot.put("syncedAt", System.currentTimeMillis())
        NativeCloudPhotoDeletion.requireUnreservedSnapshot(snapshot)
        val saved = prefs(context).edit()
            .putLong(REVISION, revision)
            .putString(SNAPSHOT, snapshot.toString())
            .commit()
        if (!saved) {
            throw IllegalStateException("Workout snapshot could not be saved")
        }
        if (snapshot.optLong("ownerEpoch193") == 0L && !snapshot.has("handoff193")) {
            PhoneGpsRoute193.authorizeOwned(context, snapshot)
        }
        _snapshotFlow.value = JSONObject(snapshot.toString())
        return snapshot
    }

    @JvmStatic
    @Throws(Exception::class)
    fun clear(context: Context): JSONObject {
        synchronized(PhoneOwnership193.journal(context)) {
            return clearOwned(context)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    private fun clearOwned(context: Context): JSONObject {
        val previous = current(context)
        if (previous != null) {
            PhoneOwnership193.journal(context).close(previous.optString("workoutId"))
        }
        val revision = prefs(context).getLong(REVISION, 0L) + 1L
        val snapshot = JSONObject()
            .put("schema", 1)
            .put("revision", revision)
            .put("active", false)
            .put("workoutId", previous?.optString("workoutId").orEmpty())
            .put("ownerEpoch193", previous?.optLong("ownerEpoch193") ?: 0L)
            .put("syncedAt", System.currentTimeMillis())
        val saved = prefs(context).edit()
            .putLong(REVISION, revision)
            .putString(SNAPSHOT, snapshot.toString())
            .commit()
        if (!saved) {
            throw IllegalStateException("Workout closure could not be saved")
        }
        _snapshotFlow.value = JSONObject(snapshot.toString())
        return snapshot
    }

    @JvmStatic
    @Synchronized
    fun current(context: Context): JSONObject? {
        val raw = prefs(context).getString(SNAPSHOT, "")
        if (raw.isNullOrEmpty()) return null
        return try {
            JSONObject(raw).also { _snapshotFlow.value = it }
        } catch (_: Exception) {
            null
        }
    }
}
