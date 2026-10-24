package com.repforge.app

import org.json.JSONObject

/**
 * Main #128 stores the complete training profile in settings.profile. Keep its
 * equipment, exclusions, lineage and future keys intact; never infer body data.
 */
internal object NativeTrainingProfile {
    private const val MAX_BYTES = 16_384
    private val numeric = mapOf("age" to 1.0..120.0, "height" to 60.0..260.0,
        "weight" to 10.0..500.0, "minutes" to 5.0..300.0)
    private val simple = setOf("goal", "experience", "activity", "location", "cadence", "split", "focus")

    fun verified(input: JSONObject): JSONObject {
        val copy = JSONObject(input.toString())
        require(copy.toString().toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Profile too large" }
        for ((key, bounds) in numeric) {
            if (!copy.has(key) || copy.isNull(key)) continue
            val value = copy.opt(key)
            require(value is Number && value.toDouble().isFinite() && value.toDouble() in bounds) {
                "Invalid profile field: $key"
            }
        }
        for (key in simple) {
            if (!copy.has(key) || copy.isNull(key)) continue
            val text = copy.opt(key) as? String ?: throw IllegalArgumentException("Invalid profile text: $key")
            require(text.length <= 120 && text.none(Char::isISOControl))
        }
        for (key in listOf("gear", "excluded", "avoidAreas")) {
            if (copy.has(key) && !copy.isNull(key)) {
                val rows = copy.optJSONArray(key) ?: throw IllegalArgumentException("Invalid profile array: $key")
                require(rows.length() <= 100)
                for (i in 0 until rows.length()) {
                    val item = rows.opt(i) as? String ?: throw IllegalArgumentException("Invalid profile item")
                    require(item.length <= 120 && item.none(Char::isISOControl))
                }
            }
        }
        return copy
    }

    /** Empty numeric input clears that field, not the rest of a saved profile. */
    fun edit(original: JSONObject, fields: Map<String, String>): JSONObject {
        val updated = JSONObject(original.toString())
        for ((key, raw) in fields) {
            require(key in numeric || key in simple || key in setOf("gear", "excluded", "avoidAreas"))
            val trimmed = raw.trim()
            if (key in setOf("gear", "excluded", "avoidAreas")) {
                val values = raw.split(',').map(String::trim).filter(String::isNotBlank)
                require(values.size <= 100 && values.all { it.length <= 120 && it.none(Char::isISOControl) })
                updated.put(key, org.json.JSONArray().apply { values.distinct().forEach { put(it) } })
            } else if (trimmed.isBlank()) updated.remove(key)
            else if (key in numeric) {
                val n = trimmed.replace(',', '.').toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid number: $key")
                require(n.isFinite() && n in numeric.getValue(key))
                if (key == "age" || key == "minutes") {
                    require(n % 1.0 == 0.0)
                    updated.put(key, n.toInt())
                } else updated.put(key, n)
            } else updated.put(key, trimmed)
        }
        return verified(updated)
    }
}
