package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Main v144 exerciseFavorites array, stored in existing Room documents (no new schema). */
internal object NativeExerciseFavorites {
    const val KEY = "exerciseFavorites"
    private const val LEGACY_KEY = "legacy:exerciseFavorites"
    private const val MAX_FAVORITES = 5000
    private val forbidden = setOf("__proto__", "constructor", "prototype")

    fun decode(value: Any?): List<String> {
        val rows = value as? JSONArray ?: throw IllegalArgumentException("Expected exerciseFavorites array")
        require(rows.length() <= MAX_FAVORITES) { "Too many favorites" }
        val ids = LinkedHashSet<String>()
        for (i in 0 until rows.length()) {
            val id = rows.opt(i) as? String ?: throw IllegalArgumentException("Non-string favorite")
            require(validId(id)) { "Invalid exercise favorite" }
            ids.add(id)
        }
        return ids.toList()
    }

    private fun validId(id: String): Boolean = id.isNotBlank() && id.length <= 200 &&
        id !in forbidden && id.all { ch ->
            !ch.isISOControl() && ch !in "\\<>\u0022'\u0060&;{}[]()="
        }

    fun encode(ids: Collection<String>): JSONArray {
        require(ids.size <= MAX_FAVORITES) { "Too many favorites" }
        val out = JSONArray()
        ids.distinct().forEach { id ->
            require(validId(id)) { "Invalid exercise favorite" }
            out.put(id)
        }
        return out
    }

    suspend fun load(db: TrainPilotDatabase): List<String> {
        val primary = db.keyValueDocumentDao().getDocument(KEY)
        if (primary != null) return decode(JSONTokener(primary.json).nextValue())
        val old = db.keyValueDocumentDao().getDocument(LEGACY_KEY) ?: return emptyList()
        return decode(JSONObject(old.json).getJSONArray("value"))
    }

    /** Only an existing library entry can be toggled; retain unknown imported IDs until explicitly edited. */
    suspend fun toggle(context: Context, db: TrainPilotDatabase, id: String): List<String> =
        NativeDataReplacement.mutate(context) {
            require(validId(id)) { "Invalid exercise ID" }
            db.withTransaction {
                require(db.exerciseLibraryDao().getExerciseById(id) != null) { "Exercise no longer exists" }
                val current = load(db).toMutableList()
                if (!current.remove(id)) current.add(id)
                val payload = encode(current).toString()
                db.keyValueDocumentDao().putDocument(
                    KeyValueDocumentEntity(KEY, payload, System.currentTimeMillis())
                )
                current.toList()
            }
        }
}
