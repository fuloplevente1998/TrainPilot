package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

/**
 * Writes only the existing programs/program_days Room tables and main-compatible raw JSON.
 * The dialog edits an isolated draft; Save alone publishes a complete atomic replacement.
 */
internal object NativeProgramEditor {
    data class DayDraft(val id: String, val name: String, val entries: List<String>)
    data class Draft(val name: String, val description: String, val days: List<DayDraft>)
    private val langs = setOf("hu", "en", "de", "ro")
    private const val MAX_DAYS = 31
    private const val MAX_EXERCISES = 60

    fun from(source: ProgramWithDays?, language: String): Draft {
        require(language in langs)
        val prog = source?.program
        val name = when (language) {
            "en" -> prog?.nameEn; "de" -> prog?.nameDe; "ro" -> prog?.nameRo; else -> prog?.nameHu
        }.orEmpty()
        val days = source?.days.orEmpty().sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId })
            .map { day ->
                val label = when (language) {
                    "en" -> day.nameEn; "de" -> day.nameDe; "ro" -> day.nameRo; else -> day.nameHu
                }.ifBlank { day.nameHu }
                val rows = JSONArray(day.exercisesJson)
                DayDraft(day.dayId, label, (0 until rows.length()).map { encodeEntry(rows.get(it)) })
            }
        return Draft(name, prog?.description.orEmpty(), days)
    }

    fun addDay(draft: Draft, name: String): Draft {
        require(draft.days.size < MAX_DAYS)
        val id = "day-${UUID.randomUUID()}"
        return draft.copy(days = draft.days + DayDraft(id, name, emptyList()))
    }

    fun addExercise(day: DayDraft, exerciseId: String): DayDraft {
        require(exerciseId.isNotBlank() && !exerciseId.any { it.isISOControl() })
        require(day.entries.size < MAX_EXERCISES)
        return day.copy(entries = day.entries + JSONObject.quote(exerciseId))
    }

    fun move(items: List<String>, index: Int, distance: Int): List<String> {
        val target = index + distance
        if (index !in items.indices || target !in items.indices) return items
        return items.toMutableList().apply { add(target, removeAt(index)) }
    }

    fun exerciseId(entry: String): String {
        val decoded = JSONTokener(entry).nextValue()
        return when (decoded) {
            is String -> decoded
            is JSONObject -> decoded.optString("id")
            else -> ""
        }.also { require(it.isNotBlank() && it.length <= 200 && !it.any(Char::isISOControl)) }
    }

    private fun encodeEntry(value: Any): String = when (value) {
        is String -> JSONObject.quote(value)
        is JSONObject -> value.toString()
        else -> throw IllegalArgumentException("Invalid program exercise")
    }

    private fun checked(draft: Draft): Draft {
        val name = draft.name.trim()
        require(name.isNotBlank() && name.length <= 120 && name.none(Char::isISOControl))
        require(draft.description.length <= 2000)
        require(draft.days.size in 1..MAX_DAYS)
        val ids = HashSet<String>()
        for (day in draft.days) {
            require(day.id.isNotBlank() && day.id.length <= 120 && ids.add(day.id))
            require(day.name.isNotBlank() && day.name.length <= 120)
            require(day.entries.size <= MAX_EXERCISES)
            day.entries.forEach { exerciseId(it) }
        }
        return draft.copy(name = name)
    }

    private fun assertNoActiveWorkout(context: Context) {
        check(ActiveWorkoutStore.current(context)?.optBoolean("active") != true &&
            PhoneWorkoutExecution.store(context).snapshot()?.optBoolean("active") != true) {
            "Finish the active workout before modifying programs"
        }
    }

    private fun assertUnchanged(opened: ProgramWithDays, latest: ProgramWithDays?) {
        check(latest != null && latest.program == opened.program &&
            latest.days.sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId }) ==
            opened.days.sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId })) {
            "Program changed; reopen editor"
        }
    }

    suspend fun save(context: Context, db: TrainPilotDatabase, opened: ProgramWithDays?,
        draft: Draft, language: String, personalMetadata: JSONObject? = null, clone: Boolean = false): String = NativeDataReplacement.mutate(context) {
        require(language in langs)
        assertNoActiveWorkout(context)
        val d = checked(draft)
        if (personalMetadata != null) {
            require(opened?.program?.builtin == true && opened.program.id in setOf("home-basic", "home-level2"))
            require(personalMetadata.optInt("templateVersion") == 2 &&
                personalMetadata.optString("sourceTemplateId") == opened.program.id)
            val expected = NativeTrainingProfile.verified(
                personalMetadata.getJSONObject("profileSnapshot"))
            val current = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().profileJson
            check(JSONObject(current).toString() == expected.toString()) { "Training profile changed" }
            val rx = personalMetadata.getJSONObject("prescriptions")
            val selected = d.days.flatMap { day -> day.entries.map(::exerciseId) }.toSet()
            require(rx.keys().asSequence().toSet() == selected) { "Prescription/exercise mismatch" }
            for (key in selected) {
                val item = rx.getJSONObject(key)
                require(item.optInt("sets") in 2..3 && item.optInt("rir") == 2 &&
                    item.optInt("rest") in 60..180 && item.optString("loadType").isNotBlank())
                val weight = item.opt("weight") as? Number
                require(weight != null && weight.toDouble().isFinite() && weight.toDouble() >= 0)
                if (item.optBoolean("trial")) require(weight.toDouble() == 0.0)
            }
            personalMetadata.optJSONObject("history128")?.let { historical ->
                val asOf = historical.optLong("asOf")
                require(asOf > 0 && asOf <= System.currentTimeMillis() &&
                    asOf > System.currentTimeMillis() - 2L * 3600_000L)
                val entries = historical.optJSONObject("entries") ?: throw IllegalArgumentException("Missing lineage")
                require(entries.keys().asSequence().all { it in selected })
                for (key in entries.keys()) {
                    val evidence = entries.getJSONObject(key)
                    require(evidence.optString("loadType") == rx.getJSONObject(key).optString("loadType") &&
                        evidence.optString("variation") == rx.getJSONObject(key).optString("variation") &&
                        evidence.optString("metric") in setOf("reps", "time"))
                    val sources = evidence.getJSONArray("sources")
                    require(sources.length() in 1..4)
                    val unique = HashSet<Pair<String, String>>()
                    for (ix in 0 until sources.length()) {
                        val item = sources.getJSONObject(ix)
                        val pid = item.getString("programId")
                        val identity = item.getString("key")
                        require(pid.isNotBlank() && identity.isNotBlank() && unique.add(pid to identity))
                    }
                }

            }
        }
        db.withTransaction {
            val original = opened?.let { db.programDao().getProgramWithDaysById(it.program.id) }
            if (opened != null) assertUnchanged(opened, original)
            if (personalMetadata != null) {
                // Rebuild from canonical catalog/profile inside the write transaction.
                // UI JSON and a historical lineage hash do not prove current eligibility.
                val base = NativeProfileProgramPreview.preview(original!!,
                    personalMetadata.getJSONObject("profileSnapshot").toString(),
                    db.exerciseLibraryDao().getAllExercises(), language)
                val historical = personalMetadata.optJSONObject("history128")
                val fresh = if (historical == null) base else NativeProfileHistory128.apply(db, base,
                    java.time.Instant.ofEpochMilli(historical.getLong("asOf")),
                    activeProgramId = com.repforge.app.data.store.TrainPilotSettingsStore(context).current().activeProgramId,
                    validationNow = java.time.Instant.now())
                require(fresh.draft == d && fresh.prescriptions.toString() ==
                    personalMetadata.getJSONObject("prescriptions").toString() &&
                    (historical == null || fresh.history128.toString() == historical.toString())) {
                    "Personal preview changed; reopen preview"
                }
            }
            // Built-in templates must be cloned, never overwritten.
            val fork = clone || opened == null || opened.program.builtin
            val id = if (fork) "custom-program-${UUID.randomUUID()}" else opened!!.program.id
            check(id !in db.programDao().getProgramIds() || !fork) { "Program ID collision" }
            val oldDays = if (fork) opened?.days.orEmpty() else original?.days.orEmpty()
            val originalRows = oldDays.associateBy { it.dayId }
            if (!fork) for (removed in oldDays.filter { it.dayId !in d.days.map(DayDraft::id) }) {
                check(!db.programDao().hasDayHistory(id, removed.dayId) &&
                    !db.programDao().hasDaySchedule(id, removed.dayId)) {
                    "Cannot delete a day referenced in history or calendar"
                }
            }
            // A newly inserted exercise must have a valid library row. Imported legacy
            // references remain untouched as long as they are unchanged in that day.
            val previousEntries = oldDays.associate { it.dayId to JSONArray(it.exercisesJson).let { arr ->
                (0 until arr.length()).map { ix -> encodeEntry(arr.get(ix)) }
            } }
            val known = db.exerciseLibraryDao().getExerciseIds().toSet()
            for (day in d.days) for (entry in day.entries) {
                val old = previousEntries[day.id].orEmpty()
                if (entry !in old) require(exerciseId(entry) in known) { "Unknown exercise" }
            }
            val originalProgram = opened?.program
            val originalJson = originalProgram?.rawJson?.let { JSONObject(it) } ?: JSONObject()
            val nameHu = if (language == "hu" || originalProgram == null) d.name else originalProgram.nameHu
            val names = mapOf("hu" to nameHu,
                "en" to (if (language == "en" || originalProgram == null) d.name else originalProgram.nameEn),
                "de" to (if (language == "de" || originalProgram == null) d.name else originalProgram.nameDe),
                "ro" to (if (language == "ro" || originalProgram == null) d.name else originalProgram.nameRo))
            val previousJsonDays = originalJson.optJSONArray("days")
            val sourceDayRaw = HashMap<String, JSONObject>()
            for (i in 0 until (previousJsonDays?.length() ?: 0)) {
                val row = previousJsonDays?.optJSONObject(i) ?: continue
                sourceDayRaw[row.optString("id")] = row
            }
            val dayEntities = d.days.mapIndexed { index, day ->
                val previous = originalRows[day.id]
                val hu = if (language == "hu" || previous == null) day.name else previous.nameHu
                val en = if (language == "en" || previous == null) day.name else previous.nameEn
                val de = if (language == "de" || previous == null) day.name else previous.nameDe
                val ro = if (language == "ro" || previous == null) day.name else previous.nameRo
                ProgramDayEntity("$id:${day.id}", id, day.id, hu, en, de, ro, index,
                    JSONArray().apply { day.entries.forEach { put(JSONTokener(it).nextValue()) } }.toString())
            }
            val jsonDays = JSONArray()
            dayEntities.forEach { day ->
                val originalDay = sourceDayRaw[day.dayId]?.let { JSONObject(it.toString()) } ?: JSONObject()
                val entries = JSONArray(day.exercisesJson)
                originalDay.put("id", day.dayId).put("name", day.nameHu)
                    .put("hu", day.nameHu).put("en", day.nameEn)
                    .put("de", day.nameDe).put("ro", day.nameRo).put("exercises", entries)
                jsonDays.put(originalDay)
            }
            val raw = JSONObject(originalJson.toString())
                .put("id", id).put("name", nameHu).put("hu", names.getValue("hu"))
                .put("en", names.getValue("en")).put("de", names.getValue("de"))
                .put("ro", names.getValue("ro")).put("description", d.description)
                .put("builtin", false).put("generated", personalMetadata != null)
                .put("sourceType", if (personalMetadata == null) "own" else "profile-template")
                .put("days", jsonDays).put("updatedAt", System.currentTimeMillis())
            if (personalMetadata != null) {
                raw.put("sourceTemplateId", opened!!.program.id).put("templateVersion", 2)
                    .put("estimatedMinutes", NativeProfileProgramPreview.estimatedMinutes(d.days, personalMetadata.getJSONObject("prescriptions")))
                    .put("profileSnapshot", JSONObject(personalMetadata.getJSONObject("profileSnapshot").toString()))
                    .put("prescriptions", JSONObject(personalMetadata.getJSONObject("prescriptions").toString()))
                personalMetadata.optJSONObject("history128")?.let { history ->
                    raw.put("history128", JSONObject(history.toString()))
                }
            }
            // Generated plan references are kept only on existing edits; new clones own their data.
            if (fork && personalMetadata == null) raw.remove("sourceTemplateId")
            val entity = ProgramEntity(id, names.getValue("hu"), names.getValue("en"),
                names.getValue("de"), names.getValue("ro"),
                originalProgram?.location ?: "Otthon", originalProgram?.level ?: "Kezdő",
                d.description, false, personalMetadata != null,
                if (personalMetadata == null) "own" else "profile-template",
                if (personalMetadata != null) opened!!.program.id else if (fork) null else originalProgram?.sourceTemplateId,
                if (personalMetadata != null) 2 else originalProgram?.templateVersion ?: 1, originalProgram?.accentHex ?: "#4d8fbe",
                if (fork) db.programDao().getProgramIds().size else originalProgram!!.sortOrder,
                System.currentTimeMillis(), raw.toString())
            db.programDao().saveProgramWithDays(entity, dayEntities)
            id
        }
    }

    suspend fun delete(context: Context, db: TrainPilotDatabase, opened: ProgramWithDays,
        settings: TrainPilotSettingsStore) = NativeDataReplacement.mutate(context) {
        assertNoActiveWorkout(context)
        db.withTransaction {
            assertUnchanged(opened, db.programDao().getProgramWithDaysById(opened.program.id))
            check(!opened.program.builtin) { "Built-in template cannot be deleted" }
            val id = opened.program.id
            check(settings.current().activeProgramId != id) { "Deactivate the program before deleting" }
            check(!db.programDao().hasWorkoutHistory(id) && !db.programDao().hasScheduledEntries(id)) {
                "Program is referenced in history or calendar"
            }
            db.programDao().deleteCustomProgram(id)
        }
    }
}
