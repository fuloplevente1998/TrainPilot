package com.repforge.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

internal object ActiveWorkoutStore {
    private const val PREFS = "trainpilot_wear_sync"; private const val SNAPSHOT = "snapshot"; private const val REVISION = "revision"
    private val _snapshotFlow = MutableStateFlow<JSONObject?>(null)
    val snapshotFlow: StateFlow<JSONObject?> = _snapshotFlow.asStateFlow()
    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @JvmStatic @Throws(Exception::class) fun save(context: Context, input: JSONObject): JSONObject {
        synchronized(PhoneOwnership193.journal(context)) {
            if (!PhoneOwnership193.publicationAllowed(context, input)) throw IllegalStateException("Reconcile workout ownership before publishing")
            return saveOwned(context, input)
        }
    }

    @JvmStatic @Synchronized @Throws(Exception::class) fun saveOwned(context: Context, input: JSONObject): JSONObject {
        val rev = prefs(context).getLong(REVISION, 0L) + 1L; val snap = JSONObject(input.toString())
        PhoneGpsRoute193.captureOrigin(context, snap)
        snap.put("schema", snap.optInt("schema", 1)).put("revision", rev).put("active", true).put("syncedAt", System.currentTimeMillis())
        val ok = prefs(context).edit().putLong(REVISION, rev).putString(SNAPSHOT, snap.toString()).commit()
        if (!ok) throw IllegalStateException("Workout snapshot could not be saved")
        if (snap.optLong("ownerEpoch193") == 0L && !snap.has("handoff193")) PhoneGpsRoute193.authorizeOwned(context, snap)
        _snapshotFlow.value = JSONObject(snap.toString())
        return snap
    }

    @JvmStatic @Throws(Exception::class) fun clear(context: Context): JSONObject {
        synchronized(PhoneOwnership193.journal(context)) { return clearOwned(context) }
    }

    @Synchronized @Throws(Exception::class) private fun clearOwned(context: Context): JSONObject {
        val prev = current(context)
        if (prev != null) PhoneOwnership193.journal(context).close(prev.optString("workoutId"))
        val rev = prefs(context).getLong(REVISION, 0L) + 1L
        val snap = JSONObject().put("schema", 1).put("revision", rev).put("active", false).put("workoutId", prev?.optString("workoutId").orEmpty()).put("ownerEpoch193", prev?.optLong("ownerEpoch193") ?: 0L).put("syncedAt", System.currentTimeMillis())
        val ok = prefs(context).edit().putLong(REVISION, rev).putString(SNAPSHOT, snap.toString()).commit()
        if (!ok) throw IllegalStateException("Workout closure could not be saved")
        _snapshotFlow.value = JSONObject(snap.toString())
        return snap
    }

    @JvmStatic @Synchronized fun current(context: Context): JSONObject? {
        val raw = prefs(context).getString(SNAPSHOT, "")
        if (raw.isNullOrEmpty()) return null
        return try { JSONObject(raw).also { _snapshotFlow.value = it } } catch (_: Exception) { null }
    }
}
