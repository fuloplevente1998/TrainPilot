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

internal object WearLiveHealthStore {
    const val PATH: String = WearDataLayerPaths.WATCH_HEALTH_PATH
    private const val PREFS = "trainpilot_wear_live_health"
    private val _liveHealthFlow = MutableStateFlow<JSONObject?>(null)
    val liveHealthFlow: StateFlow<JSONObject?> = _liveHealthFlow.asStateFlow()

    @JvmStatic @Synchronized fun save(context: Context, raw: String?) {
        if (raw == null || raw.length > 32768) return
        try {
            val next = JSONObject(raw); if (!valid(next)) return
            val old = current(context)
            if (old != null && (if (old.optString("watchId") == next.optString("watchId")) old.optLong("revision") >= next.optLong("revision") else old.optLong("updatedAt") > next.optLong("updatedAt"))) return
            context.getSharedPreferences(PREFS, 0).edit().putString("health", raw).apply()
            _liveHealthFlow.value = next
        } catch (_: Exception) {}
    }

    @JvmStatic fun valid(v: JSONObject): Boolean = v.optInt("schema") == 1 && v.optLong("revision") > 0L && v.optString("workoutId").isNotEmpty() && v.optString("workoutId").length <= 512 && v.optString("watchId").isNotEmpty() && v.optString("watchId").length <= 80 && v.optLong("updatedAt") > 0L

    @JvmStatic @Synchronized fun current(context: Context): JSONObject? = try {
        val raw = context.getSharedPreferences(PREFS, 0).getString("health", "")
        if (raw.isNullOrEmpty()) null else JSONObject(raw).also { _liveHealthFlow.value = it }
    } catch (_: Exception) { null }
}

internal object WearWorkoutHandoff {
    const val REQUEST: String = WearDataLayerPaths.WORKOUT_REQUEST_PATH
    const val REPLY: String = WearDataLayerPaths.WORKOUT_REPLY_PATH

    @JvmStatic fun response(current: JSONObject?, request: JSONObject?, now: Long): JSONObject? {
        if (current == null || request == null || !current.optBoolean("active")) return null
        val id = request.optString("workoutId"); val revision = request.optLong("minimumRevision"); val sentAt = request.optLong("sentAt")
        if (id.isEmpty() || id.length > 512 || id != current.optString("workoutId") || revision < 1L || current.optLong("revision") < revision || sentAt <= 0L || now - sentAt > 15_000L || sentAt - now > 15_000L) return null
        return current
    }
}

internal object WearCommandQueueStore {
    private const val PREFS = "trainpilot_wear_commands"; private const val PENDING = "pending"; private const val SEEN = "seen"; private const val MAX_PENDING = 64; private const val MAX_SEEN = 256
    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun read(c: Context, k: String): JSONArray = try { JSONArray(prefs(c).getString(k, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
    private fun containsId(a: JSONArray, id: String): Boolean { for (i in 0 until a.length()) { val v = a.opt(i); if (v is JSONObject && id == v.optString("commandId")) return true; if (id == v.toString()) return true }; return false }

    @JvmStatic @Synchronized @Throws(Exception::class) fun enqueue(context: Context, input: JSONObject, uri: String?) {
        if (PhoneGpsRoute193.ACTION == input.optString("action") || input.has("gpsRoutePacket193")) return
        val id = input.optString("commandId", ""); if (id.isEmpty()) return
        val pending = read(context, PENDING); val seen = read(context, SEEN)
        if (containsId(pending, id) || containsId(seen, id)) return
        if ("watchAutomationState" == input.optString("action")) for (i in pending.length() - 1 downTo 0) {
            val old = pending.optJSONObject(i)
            if (old != null && "watchAutomationState" == old.optString("action") && input.optString("workoutId") == old.optString("workoutId")) {
                if (old.optLong("sequence") >= input.optLong("sequence")) return
                pending.remove(i)
            }
        }
        val cmd = JSONObject(input.toString()).put("receivedAt", System.currentTimeMillis())
        if (uri != null) cmd.put("_uri", uri) else cmd.remove("_uri")
        if (pending.length() >= MAX_PENDING) return
        pending.put(cmd)
        if (!prefs(context).edit().putString(PENDING, pending.toString()).commit()) throw IllegalStateException("Unable to persist watch command")
    }

    @JvmStatic @Synchronized fun pending(context: Context): JSONArray = try { JSONArray(read(context, PENDING).toString()) } catch (_: Exception) { JSONArray() }
    @JvmStatic @Synchronized fun seen(context: Context, id: String): Boolean = containsId(read(context, SEEN), id)

    @JvmStatic @Synchronized fun sourceOrigin193(context: Context, workoutId: String): String {
        val nodes = HashSet<String>(); val pending = read(context, PENDING); val seen = read(context, SEEN)
        for (i in 0 until pending.length()) { val c = pending.optJSONObject(i); if (c != null && workoutId == c.optString("workoutId")) { val o = PhoneGpsRoute193.startOrigin(c); if (o.isNotEmpty()) nodes.add(o) } }
        for (i in 0 until seen.length()) { val r = seen.optJSONObject(i); if (r != null && workoutId == r.optString("workoutId") && GpsRoutePacket193.id(r.optString("sourceNode193"))) nodes.add(r.optString("sourceNode193")) }
        return if (nodes.size == 1) nodes.iterator().next() else ""
    }

    @JvmStatic @Synchronized fun receipt(context: Context, id: String): JSONObject? {
        val seen = read(context, SEEN)
        for (i in 0 until seen.length()) { val r = seen.optJSONObject(i); if (r != null && id == r.optString("commandId")) return r }
        return null
    }

    @JvmStatic @Synchronized fun ack(context: Context, command: JSONObject, status: String): Boolean {
        val id = command.optString("commandId"); if (id.isEmpty()) return false
        if ("discardWorkout" == command.optString("action") && "discarded" == status && command.optJSONObject("finalSnapshot")?.optInt("ownershipProtocol193") == 1) try { GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(command.optString("workoutId")) } catch (_: Exception) { return false }
        if ("finishWorkout" == command.optString("action") && "saved" == status) try {
            val o = PhoneGpsRoute193.startOrigin(command); if (o.isNotEmpty() && command.optJSONObject("finalSnapshot") != null) PhoneGpsRoute193.authorize(context, JSONObject(command.getJSONObject("finalSnapshot").toString()).put("watchSourceNode193", o))
        } catch (_: Exception) { return false }
        val pending = read(context, PENDING); val next = JSONArray(); val seen = read(context, SEEN)
        for (i in 0 until pending.length()) { val r = pending.optJSONObject(i); if (r != null && id != r.optString("commandId")) next.put(r) }
        if (!containsId(seen, id)) try {
            val r = JSONObject().put("schema", 1).put("commandId", id).put("workoutId", command.optString("workoutId")).put("action", command.optString("action")).put("sequence", command.optLong("sequence")).put("status", status).put("receivedAt", System.currentTimeMillis())
            val o = PhoneGpsRoute193.startOrigin(command); if (o.isNotEmpty()) r.put("sourceNode193", o)
            if (PhoneGpsRoute193.ACTION == command.optString("action")) { r.put("route193", command.getJSONObject("route193")); if (command.optBoolean("routeDeleted193")) r.put("routeDeleted193", true) }
            seen.put(r)
        } catch (_: Exception) { return false }
        while (seen.length() > MAX_SEEN) seen.remove(0)
        val p = prefs(context); val bPend = p.getString(PENDING, "[]"); val bSeen = p.getString(SEEN, "[]")
        if (p.edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).commit()) return true
        p.edit().putString(PENDING, bPend).putString(SEEN, bSeen).commit(); return false
    }

    @JvmStatic @Synchronized fun ack(context: Context, commandId: String?): Boolean {
        if (commandId.isNullOrEmpty()) return false
        val pending = read(context, PENDING); val next = JSONArray()
        for (i in 0 until pending.length()) { val c = pending.optJSONObject(i); if (c != null && commandId != c.optString("commandId")) next.put(c) }
        val seen = read(context, SEEN); if (!containsId(seen, commandId)) seen.put(commandId)
        while (seen.length() > MAX_SEEN) seen.remove(0)
        return prefs(context).edit().putString(PENDING, next.toString()).putString(SEEN, seen.toString()).commit()
    }
}

internal object WearCommandReceipts {
    const val PREFIX: String = WearDataLayerPaths.RECEIPT_PATH_PREFIX

    @JvmStatic fun complete(context: Context, command: JSONObject, status: String, uri: Uri?): Boolean {
        if (!WearCommandQueueStore.ack(context, command, status)) return false
        replay(context, command, uri); return true
    }

    @JvmStatic fun replay(context: Context, command: JSONObject, uri: Uri?) {
        val id = command.optString("commandId"); val receipt = WearCommandQueueStore.receipt(context, id)
        if (receipt == null || receipt.optString("workoutId").isEmpty() || receipt.optLong("sequence") <= 0L) { if (uri != null) Wearable.getDataClient(context).deleteDataItems(uri); return }
        if ("undoSet" == command.optString("action")) { val a = ActiveWorkoutStore.current(context); if (a != null && a.optString("workoutId") == receipt.optString("workoutId")) try { receipt.put("undoFrame", a) } catch (_: Exception) {} }
        val map = PutDataMapRequest.create(PREFIX + id).apply { dataMap.putString("receipt", receipt.toString()) }
        Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent()).addOnSuccessListener { if (uri != null) Wearable.getDataClient(context).deleteDataItems(uri) }
    }
}
