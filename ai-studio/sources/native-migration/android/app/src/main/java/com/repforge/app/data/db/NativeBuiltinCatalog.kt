package com.repforge.app.data.db

import org.json.JSONArray
import org.json.JSONObject

/** Data-only snapshot of the accepted main. No WebView/JavaScript engine runs in the native app. */
internal object NativeBuiltinCatalog {
    private val catalog: JSONObject by lazy {
        val input = requireNotNull(NativeBuiltinCatalog::class.java.getResourceAsStream("/native-catalog-main-7473.json")) {
            "Missing bundled native catalog"
        }
        val json = JSONObject(input.bufferedReader().use { it.readText() })
        check(json.getJSONObject("source").getString("appBlob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
        json
    }
    fun programMetadata(program: ProgramEntity, field: String, language: String): String {
        val fallback = if (field == "location") program.location else program.level
        if (!program.builtin) return fallback
        val rows = catalog.getJSONArray("programs")
        for (index in 0 until rows.length()) {
            val entry = rows.getJSONObject(index)
            val raw = entry.getJSONObject("raw")
            if (raw.optString("id") == program.id && raw.optString(field) == fallback)
                return entry.getJSONObject(if (field == "location") "locations" else "levels").optString(language, fallback)
        }
        return fallback
    }
    val exercises: List<ExerciseLibraryEntity> by lazy {
        val rows = catalog.getJSONArray("exercises")
        (0 until rows.length()).map { index ->
            val entry = rows.getJSONObject(index)
            val raw = JSONObject(entry.getJSONObject("raw").toString())
            val names = entry.getJSONObject("names"); val notes = entry.getJSONObject("notes")
            val id = raw.getString("id"); val demo = entry.optJSONObject("demo")
            val muscles = entry.getJSONArray("muscles")
            raw.put("measurementType", entry.getString("measurementType"))
            for (language in listOf("hu", "en", "de", "ro")) raw.put(language, names.getString(language))
            raw.put("notes", notes.getString("hu")).put("notesEn", notes.getString("en"))
                .put("notesDe", notes.getString("de")).put("notesRo", notes.getString("ro"))
                .put("muscles", muscles).put("custom", false)
            if (demo != null) raw.put("demo", demo)
            val video = demo?.optString("videoId")?.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
            val youtube = demo?.optString("provider") == "youtube" && video != null
            ExerciseLibraryEntity(id, names.getString("hu"), names.getString("en"), names.getString("de"), names.getString("ro"),
                raw.getString("loadType"), raw.getString("repUnit"), entry.getString("measurementType"),
                raw.optInt("sets", 3), raw.optString("reps"), raw.optDouble("weight", 0.0),
                raw.optInt("restSeconds", raw.optInt("rest", 90)), muscles.optString(0, "full"), muscles.toString(),
                notes.getString("hu"), notes.getString("en"), notes.getString("de"), notes.getString("ro"),
                if (youtube) "https://www.youtube.com/watch?v=$video" else null, if (youtube) "youtube" else null,
                false, raw.optBoolean("setRir229"), raw.toString())
        }.also { rows -> check(rows.size == 106 && rows.map { it.id }.distinct().size == rows.size) }
    }
    val programs: Pair<List<ProgramEntity>, List<ProgramDayEntity>> by lazy {
        val rows = catalog.getJSONArray("programs")
        val days = ArrayList<ProgramDayEntity>()
        val programs = (0 until rows.length()).map { index ->
            val entry = rows.getJSONObject(index); val raw = JSONObject(entry.getJSONObject("raw").toString())
            val names = entry.getJSONObject("names"); val id = raw.getString("id")
            for (language in listOf("hu", "en", "de", "ro")) raw.put(language, names.getString(language))
            val sourceDays = entry.getJSONArray("days")
            val rawDays = JSONArray()
            for (dayIndex in 0 until sourceDays.length()) {
                val sourceDay = sourceDays.getJSONObject(dayIndex)
                val day = JSONObject(sourceDay.getJSONObject("raw").toString()); val labels = sourceDay.getJSONObject("names")
                for (language in listOf("hu", "en", "de", "ro")) day.put(language, labels.getString(language))
                val dayId = day.getString("id"); rawDays.put(day)
                days.add(ProgramDayEntity("$id:$dayId", id, dayId, labels.getString("hu"), labels.getString("en"),
                    labels.getString("de"), labels.getString("ro"), dayIndex, day.getJSONArray("exercises").toString()))
            }
            raw.put("days", rawDays)
            ProgramEntity(id, names.getString("hu"), names.getString("en"), names.getString("de"), names.getString("ro"),
                raw.optString("location"), raw.optString("level"), raw.optString("description"), true, raw.optBoolean("generated"),
                raw.optString("sourceType", "template"), raw.optString("sourceTemplateId", id), raw.optInt("templateVersion", 1),
                raw.optString("accentHex", "#f2bd45"), index, 0L, raw.toString())
        }
        check(programs.size == 13)
        val ids = exercises.map { it.id }.toSet()
        for (day in days) {
            val rows = JSONArray(day.exercisesJson)
            for (index in 0 until rows.length()) check((rows.optJSONObject(index)?.getString("id") ?: rows.getString(index)) in ids)
        }
        programs to days
    }
}
