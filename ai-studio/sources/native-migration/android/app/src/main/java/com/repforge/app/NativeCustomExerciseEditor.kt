package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Edits only user-owned exercise definitions: historic sets and program IDs are never rewritten. */
internal object NativeCustomExerciseEditor {
    private val nameLanguages = setOf("hu", "en", "de", "ro")
    private val muscleKeys = setOf("chest", "back", "shoulders", "arms", "core", "legs", "cardio", "full")
    private val measurements = setOf("reps", "time", "distance", "steps", "strength")
    private val loads = setOf("weight", "dumbbell", "barbell", "bodyweight", "total", "machine", "cable", "single_dumbbell")
    private val youTubeUrl = Regex("https://www\\.youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}")

    data class Draft(
        val name: String, val note: String, val muscle: String, val measurement: String,
        val load: String, val repUnit: String, val sets: Int, val reps: String,
        val weight: Double, val restSeconds: Int, val videoUrl: String
    )

    fun draft(ex: ExerciseLibraryEntity?, language: String): Draft {
        val lang = language.takeIf { it in nameLanguages } ?: "hu"
        val name = when (lang) { "en" -> ex?.nameEn; "de" -> ex?.nameDe; "ro" -> ex?.nameRo; else -> ex?.nameHu }
        val note = when (lang) { "en" -> ex?.notesEn; "de" -> ex?.notesDe; "ro" -> ex?.notesRo; else -> ex?.notesHu }
        return Draft(name.orEmpty(), note.orEmpty(), ex?.primaryMuscle ?: "full",
            ex?.measurementType ?: "reps", ex?.loadType ?: "bodyweight",
            ex?.repUnit ?: "ism.", ex?.defaultSets ?: 3, ex?.defaultReps ?: "10",
            ex?.defaultWeight ?: 0.0, ex?.defaultRestSeconds ?: 90, ex?.videoUrl.orEmpty())
    }

    fun validate(draft: Draft): Draft {
        val clean = draft.copy(name = draft.name.trim(), note = draft.note.trim(),
            reps = draft.reps.trim(), repUnit = draft.repUnit.trim(), videoUrl = draft.videoUrl.trim())
        require(clean.name.isNotBlank() && clean.name.length <= 120 && clean.name.none { it.isISOControl() })
        require(clean.note.length <= 3000 && clean.note.none { it.isISOControl() && it != '\n' })
        require(clean.muscle in muscleKeys && clean.measurement in measurements && clean.load in loads)
        require(clean.repUnit.isNotEmpty() && clean.repUnit.length <= 24 && clean.repUnit.none { it.isISOControl() })
        require(clean.sets in 1..20 && clean.restSeconds in 0..3600)
        require(clean.reps.isNotBlank() && clean.reps.length <= 48 && clean.reps.none { it.isISOControl() })
        require(clean.weight.isFinite() && clean.weight in 0.0..10000.0)
        require(clean.videoUrl.isEmpty() || youTubeUrl.matches(clean.videoUrl)) {
            "Only a canonical YouTube watch URL with a video ID is accepted"
        }
        return clean
    }

    /** Existing localized fields and unknown legacy JSON keys survive edits. */
    internal fun update(existing: ExerciseLibraryEntity?, id: String, draft: Draft, language: String): ExerciseLibraryEntity {
        require(language in nameLanguages)
        require(existing == null || (existing.custom && existing.id == id))
        val d = validate(draft)
        val raw = if (existing != null) JSONObject(existing.rawJson) else JSONObject()
        val priorName = existing?.nameHu ?: d.name
        val names = linkedMapOf("hu" to (existing?.nameHu ?: priorName),
            "en" to (existing?.nameEn ?: priorName), "de" to (existing?.nameDe ?: priorName),
            "ro" to (existing?.nameRo ?: priorName))
        val notes = linkedMapOf("hu" to (existing?.notesHu ?: ""),
            "en" to (existing?.notesEn ?: ""), "de" to (existing?.notesDe ?: ""),
            "ro" to (existing?.notesRo ?: ""))
        names[language] = d.name
        notes[language] = d.note
        val canonicalName = names["hu"]!!.ifBlank { d.name }
        // A single primary-muscle control must not erase imported secondary muscles.
        val groups = LinkedHashSet<String>()
        groups.add(d.muscle)
        if (existing != null) {
            val previous = runCatching { JSONArray(existing.muscleGroupsJson) }.getOrNull()
            for (i in 0 until (previous?.length() ?: 0)) {
                val group = previous?.optString(i).orEmpty()
                if (group in muscleKeys) groups.add(group)
            }
        }
        val muscles = JSONArray().apply { groups.forEach { put(it) } }
        raw.put("id", id).put("custom", true)
            .put("hu", names["hu"]).put("en", names["en"]).put("de", names["de"]).put("ro", names["ro"])
            .put("name", canonicalName)
            .put("notes", notes["hu"]).put("notesEn", notes["en"])
            .put("notesDe", notes["de"]).put("notesRo", notes["ro"])
            .put("primaryMuscle", d.muscle).put("muscles", muscles)
            .put("measurementType", d.measurement).put("loadType", d.load)
            .put("repUnit", d.repUnit).put("sets", d.sets).put("reps", d.reps)
            .put("weight", d.weight).put("restSeconds", d.restSeconds)
        if (d.videoUrl.isBlank()) {
            raw.remove("videoUrl")
            raw.remove("provider")
            if (existing?.videoUrl != null) raw.remove("demo") // explicit video removal; preserve unrelated legacy demo metadata
        } else {
            raw.put("videoUrl", d.videoUrl).put("provider", "youtube")
            raw.remove("demo")
        }
        return ExerciseLibraryEntity(id, names["hu"]!!, names["en"]!!, names["de"]!!, names["ro"]!!,
            d.load, d.repUnit, d.measurement, d.sets, d.reps, d.weight, d.restSeconds,
            d.muscle, muscles.toString(),
            notes["hu"]!!, notes["en"]!!, notes["de"]!!, notes["ro"]!!,
            d.videoUrl.ifBlank { null }, if (d.videoUrl.isBlank()) null else "youtube",
            true, existing?.setRir229 ?: false, raw.toString())
    }

    suspend fun save(context: Context, db: TrainPilotDatabase, exerciseId: String?, draft: Draft, language: String,
        expectedRawJson: String? = null): String =
        NativeDataReplacement.mutate(context) {
            db.withTransaction {
                // Validate before any writes, including user-controlled multilingual fields.
                val checked = validate(draft)
                val id = exerciseId ?: "custom-${UUID.randomUUID()}"
                val existing = db.exerciseLibraryDao().getExerciseById(id)
                if (exerciseId != null) {
                    require(existing != null && existing.custom) { "Cannot change a built-in exercise" }
                    if (expectedRawJson != null) check(existing.rawJson == expectedRawJson) {
                        "Exercise has changed; reopen it before editing"
                    }
                } else check(existing == null) { "New ID already exists" }
                db.exerciseLibraryDao().upsertExercise(update(existing, id, checked, language))
                id
            }
        }

    private fun referencedInProgram(exerciseId: String, json: String): Boolean {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val candidate = array.optJSONObject(i)?.optString("id") ?: array.optString(i)
            if (candidate == exerciseId) return true
        }
        return false
    }

    suspend fun delete(context: Context, db: TrainPilotDatabase, id: String,
        expectedRawJson: String? = null) =
        NativeDataReplacement.mutate(context) {
            assertDeletionAdmission(context)
            db.withTransaction {
                val exercise = db.exerciseLibraryDao().getExerciseById(id)
                require(exercise?.custom == true) { "Only custom exercises can be deleted" }
                if (expectedRawJson != null) check(exercise.rawJson == expectedRawJson) {
                    "Exercise has changed; reopen it before deleting"
                }
                check(!db.exerciseLibraryDao().hasWorkoutHistoryReference(id)) { "Exercise is used in workout history" }
                check(db.programDao().getProgramsWithDays().none { program ->
                    program.days.any { referencedInProgram(id, it.exercisesJson) }
                }) { "Exercise is used in a program day" }
                // Recheck after suspending Room reads, before the first write.
                assertDeletionAdmission(context)
                db.exerciseLibraryDao().deleteCustomExercise(id)
                val currentFavorites = NativeExerciseFavorites.load(db).filterNot { it == id }
                db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(
                    NativeExerciseFavorites.KEY, NativeExerciseFavorites.encode(currentFavorites).toString(),
                    System.currentTimeMillis()))
                db.keyValueDocumentDao().deleteDocument("legacy:exerciseFavorites")
            }
        }
    private suspend fun assertDeletionAdmission(context: Context) = withContext(Dispatchers.Main.immediate) {
        synchronized(ActiveWorkoutStore) {
            check(ActiveWorkoutStore.current(context)?.optBoolean("active") != true &&
                PhoneWorkoutExecution.store(context).snapshot()?.optBoolean("active") != true) {
                "Finish the active workout before deleting exercises"
            }
        }
    }

}
