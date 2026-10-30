package com.repforge.app.data.migration

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Portable backups keep route references and measurements, but never private coordinate samples. */
internal object NativeBackupPrivacy {
    fun sanitize(root: JSONObject): JSONObject = copy(root) as JSONObject
    private fun copy(value: Any): Any = when (value) {
        is JSONObject -> JSONObject().also { target ->
            for (key in value.keys()) {
                val item = value.get(key)
                val normalized = key.lowercase(Locale.ROOT)
                if (normalized in setOf("lat", "lon", "lng", "latitude", "longitude") && item is Number) continue
                if (normalized == "coordinates" && item is JSONArray) continue
                target.put(key, copy(item))
            }
        }
        is JSONArray -> JSONArray().also { target -> for (i in 0 until value.length()) target.put(copy(value.get(i))) }
        else -> value
    }
}
