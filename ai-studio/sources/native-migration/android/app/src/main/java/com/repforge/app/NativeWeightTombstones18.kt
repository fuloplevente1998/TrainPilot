package com.repforge.app

import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Durable manual-weight deletions in existing Room documents; no second weight store/schema. */
internal object NativeWeightTombstones18 {
    const val KEY = "weightTombstones18"
    private const val PREFIX = "native:weightTombstone18:"
    private const val MAX = 50000
    private val LEGACY = Regex("tp18-w-[a-f0-9]{24}")
    data class Marker(val id: String, val legacyIds: Set<String>, val deletedAt: Long)

    private fun json(marker: Marker) = JSONObject().put("version", 1).put("id", marker.id)
        .put("legacyIds", JSONArray(marker.legacyIds.sorted())).put("deletedAt", marker.deletedAt)

    private fun marker(row: JSONObject): Marker {
        if (row.has("version")) require((row.opt("version") as? Number)?.toDouble() == 1.0)
        val id = row.opt("id") as? String ?: throw IllegalArgumentException("Missing weight deletion identity")
        require(id.isNotBlank() && id.length <= 1000)
        val time = row.opt("deletedAt") as? Number ?: throw IllegalArgumentException("Invalid weight deletion time")
        val value = time.toDouble()
        require(value.isFinite() && value >= 1 && value < Long.MAX_VALUE.toDouble() && value == time.toLong().toDouble())
        val array = if (row.has("legacyIds")) row.getJSONArray("legacyIds") else JSONArray()
        require(array.length() <= 128)
        val aliases = (0 until array.length()).map { index ->
            (array.opt(index) as? String)?.also { require(LEGACY.matches(it)) }
                ?: throw IllegalArgumentException("Invalid legacy weight deletion identity")
        }.toSet()
        return Marker(id, aliases, time.toLong())
    }

    fun decode(value: Any?): List<Marker> {
        val rows = value as? JSONArray ?: throw IllegalArgumentException("Expected weight deletions array")
        require(rows.length() <= MAX)
        return union((0 until rows.length()).map { marker(rows.getJSONObject(it)) })
    }

    fun union(vararg groups: List<Marker>): List<Marker> {
        val result = sortedMapOf<String, Marker>()
        for (group in groups) for (row in group) {
            val old = result[row.id]
            val merged = if (old == null) row else Marker(row.id, old.legacyIds + row.legacyIds, maxOf(old.deletedAt, row.deletedAt))
            require(merged.legacyIds.size <= 128 && result.size + (if (old == null) 1 else 0) <= MAX)
            result[row.id] = merged
        }
        return result.values.toList()
    }

    fun encode(rows: List<Marker>): JSONArray = JSONArray(union(rows).map { json(it) })

    suspend fun load(db: TrainPilotDatabase): List<Marker> = db.keyValueDocumentDao().getWeightTombstoneDocuments().map {
        marker(JSONObject(it.json)).also { row -> check(it.key == PREFIX + row.id) { "Weight deletion document identity changed" } }
    }.also { require(it.size <= MAX) }

    /** Called inside the caller's exact-row compare-and-delete Room transaction. */
    suspend fun record(db: TrainPilotDatabase, expected: BodyWeightEntryEntity, now: Long) {
        val identity = JSONObject().put("date", expected.date).put("kg", expected.kg)
        val created = Marker(expected.id, setOf(LegacySyncIdentity18.legacyWeightId(identity)), now.coerceAtLeast(1))
        val old = db.keyValueDocumentDao().getDocument(PREFIX + expected.id)?.let { marker(JSONObject(it.json)) }
        if (old != null) check(old.id == expected.id)
        else require(db.keyValueDocumentDao().getWeightTombstoneCount() < MAX) { "Too many weight deletions" }
        val merged = union(listOfNotNull(old, created)).single()
        val checked = marker(json(merged))
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(PREFIX + checked.id, json(checked).toString(), checked.deletedAt))
    }

    class Index(rows: List<Marker>) {
        private val ids = rows.map { it.id }.toSet()
        private val aliases = rows.flatMap { it.legacyIds }.toSet()
        fun deleted(row: JSONObject): Boolean {
            val id = row.optString("id")
            if (id in ids || id in aliases) return true
            // A genuinely new UUID measurement may repeat the same date/value intentionally.
            if (id.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("weights", id)) return false
            val date = row.optString("date").trim(); val kg = row.optDouble("kg", Double.NaN)
            if (date.isEmpty() || !kg.isFinite() || kg <= 0) return false
            val normalized = JSONObject().put("date", date).put("kg", (kg * 10).roundToInt() / 10.0)
            return LegacySyncIdentity18.legacyWeightId(normalized) in aliases
        }
    }

    /** Explicit replaceAll restores the selected snapshot; ordinary/cloud imports merge markers. */
    suspend fun install(db: TrainPilotDatabase, incoming: List<Marker>?, replaceAll: Boolean,
        replacingWeights: Boolean): Index {
        val combined = union(if (replaceAll) emptyList() else load(db), incoming.orEmpty())
        if (replaceAll) db.keyValueDocumentDao().deleteWeightTombstoneDocuments()
        for (row in combined) db.keyValueDocumentDao().putDocument(
            KeyValueDocumentEntity(PREFIX + row.id, json(row).toString(), row.deletedAt))
        val index = Index(combined)
        if (!replacingWeights && combined.isNotEmpty()) {
            var after: String? = null
            while (true) {
                val page = db.bodyWeightDao().scanWeightDeletionCandidates(after, 256)
                for (row in page) if (index.deleted(JSONObject().put("id", row.id).put("date", row.date).put("kg", row.kg)))
                    db.bodyWeightDao().deleteWeightById(row.id)
                if (page.size < 256) break
                after = page.last().id
            }
        }
        return index
    }
}
