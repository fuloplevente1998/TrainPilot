package com.repforge.app

import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.NativeBuiltinCatalog
import org.json.JSONObject

/**
 * The accepted main catalog contains 100 YouTube IDs and six deliberately
 * unverified YouTube searches. Never turn a search result into a claimed video.
 * Links are opened only after an explicit user tap.
 */
internal object NativeExerciseDemo {
    enum class Kind { VIDEO, UNVERIFIED_SEARCH }
    data class Link(val kind: Kind, val url: String, val credit: String, val sourceVerified: Boolean)

    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val search = Regex("https://www\\.youtube\\.com/results\\?search_query=[A-Za-z0-9%+_.-]{1,200}")

    private fun fromRaw(json: String): Link? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val demo = obj.optJSONObject("demo")
        if (demo != null) {
            val id = demo.optString("videoId")
            if (demo.optString("provider") == "youtube" && videoId.matches(id)) {
                return Link(Kind.VIDEO, "https://www.youtube.com/watch?v=$id",
                    demo.optString("credit"), demo.optBoolean("sourceVerified"))
            }
            val source = demo.optString("source")
            if (demo.optString("sourceKind") == "unverified-search" && search.matches(source)) {
                return Link(Kind.UNVERIFIED_SEARCH, source, demo.optString("credit"), false)
            }
        }
        // User-created exercises can also carry a canonical YouTube video link.
        val url = obj.optString("videoUrl")
        val id = Regex("https://www\\.youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})")
            .matchEntire(url)?.groupValues?.get(1)
        return if (id != null) Link(Kind.VIDEO, "https://www.youtube.com/watch?v=$id",
            obj.optString("videoProvider"), false) else null
    }

    fun resolve(exercise: ExerciseLibraryEntity): Link? {
        fromRaw(exercise.rawJson)?.let { return it }
        if (exercise.custom) return null
        // Old imported builtins can predate the catalog's demo metadata.
        val original = NativeBuiltinCatalog.exercises.firstOrNull { it.id == exercise.id } ?: return null
        return fromRaw(original.rawJson)
    }
}
