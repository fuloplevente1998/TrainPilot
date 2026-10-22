package com.repforge.app

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.GpsRouteLedger193
import com.repforge.shared.GpsRoutePacket193
import com.repforge.shared.WearDataLayerPaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Keep the last watch acknowledgement without creating an unbounded command backlog. */
internal object WearLiveHealthStore {
    const val PATH: String = WearDataLayerPaths.WATCH_HEALTH_PATH
    private const val PREFS: String = "trainpilot_wear_live_health"

    private val _liveHealthFlow = MutableStateFlow<JSONObject?>(null)
    val liveHealthFlow: StateFlow<JSONObject?> = _liveHealthFlow.asStateFlow()

    @JvmStatic
    @Synchronized
    fun save(context: Context, raw: String?) {
        if (raw == null || raw.length > 32768) return
        try {
            val next = JSONObject(raw)
            if (!valid(next)) return
            val old = current(context)
            if (old != null && (
                if (old.optString("watchId") == next.optString("watchId")) {
                    old.optLong("revision") >= next.optLong("revision")
                } else {
                    old.optLong("updatedAt") > next.optLong("updatedAt")
                }
                )
            ) {
                return
            }
            context.getSharedPreferences(PREFS, 0).edit().putString("health", raw).apply()
            _liveHealthFlow.value = next
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun valid(value: JSONObject): Boolean {
        return value.optInt("schema") == 1 &&
            value.optLong("revision") > 0L &&
            value.optString("workoutId").isNotEmpty() &&
            value.optString("workoutId").length <= 512 &&
            value.optString("watchId").isNotEmpty() &&
            value.optString("watchId").length <= 80 &&
            value.optLong("updatedAt") > 0L
    }

    @JvmStatic
    @Synchronized
    fun current(context: Context): JSONObject? {
        return try {
            val raw = context.getSharedPreferences(PREFS, 0).getString("health", "")
            if (raw.isNullOrEmpty()) null else JSONObject(raw).also { _liveHealthFlow.value = it }
        } catch (_: Exception) {
            null
        }
    }
}

/** A bounded request can only refresh the workout already published by this phone. */
internal object WearWorkoutHandoff {
    const val REQUEST: String = WearDataLayerPaths.WORKOUT_REQUEST_PATH
    const val REPLY: String = WearDataLayerPaths.WORKOUT_REPLY_PATH

    @JvmStatic
    fun response(current: JSONObject?, request: JSONObject?, now: Long): JSONObject? {
        if (current == null || request == null || !current.optBoolean("active")) return null
        val id = request.optString("workoutId")
        val revision = request.optLong("minimumRevision")
        val sentAt = request.optLong("sentAt")
        if (id.isEmpty() ||
            id.length > 512 ||
            id != current.optString("workoutId") ||
            revision < 1L ||
            current.optLong("revision") < revision ||
            sentAt <= 0L ||
            now - sentAt > 15_000L ||
            sentAt - now > 15_000L
        ) {
            return null
        }
        return current
    }
}

internal object WearCommandQueueStore {
    private const val PREFS: String = "trainpilot_wear_commands"
    private const val PENDING: String = "pending"
    private const val SEEN: String = "seen"
    private const val MAX_PENDING: Int = 64
    private const val MAX_SEEN: Int = 256

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun read(context: Context, key: String): JSONArray {
        val raw = prefs(context).getString(key, "[]")
        return try {
            JSONArray(raw ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun containsId(array: JSONArray, id: String): Boolean {
        for (i in 0 until array.length()) {
            val value = array.opt(i)
            if (value is JSONObject) {
                if (id == value.optString("commandId")) return true
            } else if (id == value.toString()) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    @Synchronized
    @Throws(Exception::class)
    fun enqueue(context: Context, input: JSONObject, uri: String?) {
        if (PhoneGpsRoute193.ACTION == input.optString("action") || input.has("gpsRoutePacket193")) return
        val id = input.optString("commandId", "")
        if (id.isEmpty()) return
        val pending = read(context, PENDING)
        val seen = read(context, SEEN)
        if (containsId(pending, id) || containsId(seen, id)) return
        if ("watchAutomationState" == input.optString("action")) {
            for (i in pending.length() - 1 downTo 0) {
                val older = pending.optJSONObject(i)
                if (older != null &&
                    "watchAutomationState" == older.optString("action") &&
                    input.optString("workoutId") == older.optString("workoutId")
                ) {
                    if (older.optLong("sequence") >= input.optLong("sequence")) return
                    pending.remove(i)
                }
            }
        }
        val command = JSONObject(input.toString())
        command.put("receivedAt", System.currentTimeMillis())
        if (uri != null) command.put("_uri", uri) else command.remove("_uri")
        if (pending.length() >= MAX_PENDING) return
        pending.put(command)
        if (!prefs(context).edit().putString(PENDING, pending.toString()).commit()) {
            throw IllegalStateException("Unable to persist watch command")
        }
    }

    @JvmStatic
    @Synchronized
    fun pending(context: Context): JSONArray {
        return try {
            JSONArray(read(context, PENDING).toString())
        } catch (_: Exception) {
            JSONArray()
        }
    }

    @JvmStatic
    @Synchronized
    fun seen(context: Context, id: String): Boolean {
        return containsId(read(context, SEEN), id)
    }

    @JvmStatic
    @Synchronized
    fun sourceOrigin193(context: Context, workoutId: String): String {
        val nodes = HashSet<String>()
        val pending = read(context, PENDING)
        val seen = read(context, SEEN)
        for (i in 0 until pending.length()) {
            val command = pending.optJSONObject(i)
            if (command == null || workoutId != command.optString("workoutId")) continue
            val origin = PhoneGpsRoute193.startOrigin(command)
            if (origin.isNotEmpty()) nodes.add(origin)
        }
        for (i in 0 until seen.length()) {
            val receipt = seen.optJSONObject(i)
            if (receipt != null &&
                workoutId == receipt.optString("workoutId") &&
                GpsRoutePacket193.id(receipt.optString("sourceNode193"))
            ) {
                nodes.add(receipt.optString("sourceNode193"))
            }
        }
        return if (nodes.size == 1) nodes.iterator().next() else ""
    }

    @JvmStatic
    @Synchronized
    fun receipt(context: Context, id: String): JSONObject? {
        val seen = read(context, SEEN)
        for (i in 0 until seen.length()) {
            val row = seen.optJSONObject(i)
            if (row != null && id == row.optString("commandId")) return row
        }
        return null
    }

    @JvmStatic
    @Synchronized
    fun ack(context: Context, command: JSONObject, status: String): Boolean {
        val id = command.optString("commandId")
        if (id.isEmpty()) return false
        if ("discardWorkout" == command.optString("action") &&
            "discarded" == status &&
            command.optJSONObject("finalSnapshot") != null &&
            command.optJSONObject("finalSnapshot")!!.optInt("ownershipProtocol193") == 1
        ) {
            try {
                GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(command.optString("workoutId"))
            } catch (_: Exception) {
                return false
            }
        }
        if ("finishWorkout" == command.optString("action") && "saved" == status) {
            try {
                val origin = PhoneGpsRoute193.startOrigin(command)
                if (origin.isNotEmpty() && command.optJSONObject("finalSnapshot") != null) {
                    PhoneGpsRoute193.authorize(
                        context,
                        JSONObject(command.getJSONObject("finalSnapshot").toString())
                            .put("watchSourceNode193", origin)
                    )
                }
            } catch (_: Exception) {
                return false
            }
        }
        val pending = read(context, PENDING)
        val next = JSONArray()
        val seen = read(context, SEEN)
        for (i in 0 until pending.length()) {
            val row = pending.optJSONObject(i)
            if (row != null && id != row.optString("commandId")) {
                next.put(row)
            }
        }
        if (!containsId(seen, id)) {
            try {
                val receipt = JSONObject()
                    .put("schema", 1)
                    .put("commandId", id)
                    .put("workoutId", command.optString("workoutId"))
                    .put("action", command.optString("action"))
                    .put("sequence", command.optLong("sequence"))
                    .put("status", status)
                    .put("receivedAt", System.currentTimeMillis())
                val origin = PhoneGpsRoute193.startOrigin(command)
                if (origin.isNotEmpty()) receipt.put("sourceNode193", origin)
                if (PhoneGpsRoute193.ACTION == command.optString("action")) {
                    receipt.put("route193", command.getJSONObject("route193"))
                    if (command.optBoolean("routeDeleted193")) receipt.put("routeDeleted193", true)
                }
                seen.put(receipt)
            } catch (_: Exception) {
                return false
            }
        }
        while (seen.length() > MAX_SEEN) seen.remove(0)
        val p = prefs(context)
        val beforePending = p.getString(PENDING, "[]")
        val beforeSeen = p.getString(SEEN, "[]")
        if (p.edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).commit()) {
            return true
        }
        p.edit().putString(PENDING, beforePending).putString(SEEN, beforeSeen).commit()
        return false
    }

    @JvmStatic
    @Synchronized
    fun ack(context: Context, commandId: String?): Boolean {
        if (commandId.isNullOrEmpty()) return false
        val pending = read(context, PENDING)
        val next = JSONArray()
        for (i in 0 until pending.length()) {
            val command = pending.optJSONObject(i)
            if (command != null && commandId != command.optString("commandId")) {
                next.put(command)
            }
        }
        val seen = read(context, SEEN)
        if (!containsId(seen, commandId)) seen.put(commandId)
        while (seen.length() > MAX_SEEN) seen.remove(0)
        return prefs(context).edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).commit()
    }
}

/** A receipt is published only after the existing command/dedupe store committed. */
internal object WearCommandReceipts {
    const val PREFIX: String = WearDataLayerPaths.RECEIPT_PATH_PREFIX

    @JvmStatic
    fun complete(context: Context, command: JSONObject, status: String, uri: Uri?): Boolean {
        if (!WearCommandQueueStore.ack(context, command, status)) return false
        replay(context, command, uri)
        return true
    }

    @JvmStatic
    fun replay(context: Context, command: JSONObject, uri: Uri?) {
        val id = command.optString("commandId")
        val receipt = WearCommandQueueStore.receipt(context, id)
        if (receipt == null || receipt.optString("workoutId").isEmpty() || receipt.optLong("sequence") <= 0L) {
            if (uri != null) Wearable.getDataClient(context).deleteDataItems(uri)
            return
        }
        if ("undoSet" == command.optString("action")) {
            val active = ActiveWorkoutStore.current(context)
            if (active != null && active.optString("workoutId") == receipt.optString("workoutId")) {
                try {
                    receipt.put("undoFrame", active)
                } catch (_: Exception) {
                }
            }
        }
        val map = PutDataMapRequest.create(PREFIX + id)
        map.dataMap.putString("receipt", receipt.toString())
        Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
            .addOnSuccessListener {
                if (uri != null) Wearable.getDataClient(context).deleteDataItems(uri)
            }
    }
}
