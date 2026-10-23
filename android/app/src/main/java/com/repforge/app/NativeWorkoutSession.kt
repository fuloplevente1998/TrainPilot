package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** The phone start buttons create one durable session before opening its editor. */
internal object NativeWorkoutSession {
    private val startLock = Mutex()

    suspend fun startWorkout(context: Context, requestedProgramId: String?, dayId: String): JSONObject = startLock.withLock {
        val app = context.applicationContext
        // Never replace an existing phone session or a watch-owned mirror on a repeated tap.
        withContext(Dispatchers.Main.immediate) { recoverActive(app) }?.let { return@withLock it }
        val db = TrainPilotDatabase.getInstance(app)
        val settings = TrainPilotSettingsStore(app).current()
        val candidate = withContext(Dispatchers.IO) {
            // Imported drafts are preserved until the dedicated draft continuation path exists.
            // Opening a new day must not silently overwrite one.
            check(db.keyValueDocumentDao().getDocument("draft") == null) {
                "A korábbi félbehagyott edzés megmaradt. A natív folytatása még fejlesztés alatt áll."
            }
            val programs = db.programDao().getProgramsWithDays()
            val program = if (requestedProgramId != null) programs.firstOrNull { it.program.id == requestedProgramId }
                else programs.firstOrNull { it.program.id == settings.activeProgramId } ?: programs.firstOrNull()
            requireNotNull(program) { "Az edzésprogram nem található." }
            val day = requireNotNull(program.days.firstOrNull { it.dayId == dayId }) { "Az edzésnap nem található." }
            val library = db.exerciseLibraryDao().getAllExercises().associateBy { it.id }
            val raw = JSONObject(program.program.rawJson)
            val prescriptions = raw.optJSONObject("prescriptions") ?: JSONObject()
            val exercises = JSONArray()
            val ids = JSONArray(day.exercisesJson)
            for (i in 0 until ids.length()) {
                val item = ids.get(i)
                val id = if (item is JSONObject) item.getString("id") else item as String
                val source = requireNotNull(library[id]) { "Hiányzó gyakorlat: $id" }
                val rx = JSONObject(prescriptions.optJSONObject(id)?.toString() ?: "{}")
                if (item is JSONObject) for (key in item.keys()) if (key != "id") rx.put(key, item.get(key))
                val exercise = JSONObject(source.rawJson)
                    .put("id", id).put("hu", source.nameHu).put("en", source.nameEn)
                    .put("de", source.nameDe).put("ro", source.nameRo)
                    .put("name", localized(settings.language, source.nameHu, source.nameEn, source.nameDe, source.nameRo))
                    .put("loadType", rx.optString("loadType", source.loadType))
                    .put("repUnit", rx.optString("repUnit", source.repUnit))
                    .put("measurementType", rx.optString("measurementType", source.measurementType))
                    .put("targetReps", rx.optString("reps", source.defaultReps))
                    .put("setRir229", source.setRir229)
                    .put("prescription", rx)
                    .put("restSeconds162", rx.optInt("rest", settings.restSeconds).coerceIn(0, 3600))
                val count = rx.optInt("sets", source.defaultSets).coerceIn(1, 10)
                val weight = if (exercise.optString("loadType") == "bodyweight") 0.0
                    else rx.optDouble("weight", source.defaultWeight).also { require(it.isFinite() && it >= 0) }
                val sets = NativeWorkoutStartValues.blankSets(count, weight)
                if (NativeWorkoutStartValues.strength(exercise)) {
                    var offset = 0
                    val now = System.currentTimeMillis()
                    while (true) {
                        val rows = db.workoutHistoryDao().getPrefillCandidates(id, exercise.getString("loadType"),
                            exercise.getString("repUnit"), now, 32, offset)
                        for (row in rows) NativeWorkoutStartValues.prefill(exercise, sets, JSONObject(row.rawJson), now)
                        offset += rows.size
                        if (rows.size < 32 || (0 until sets.length()).all { sets.getJSONObject(it).has("previousValues228") }) break
                    }
                }
                exercise.remove("actualRir")
                exercise.put("sets", sets)
                exercises.put(exercise)
            }
            require(exercises.length() > 0) { "Az edzésnap üres." }
            val id = UUID.randomUUID().toString()
            val now = Instant.now().toString()
            JSONObject().put("schema", 1).put("active", true).put("workoutId", id).put("id", id).put("syncId", id)
                .put("programId", program.program.id)
                .put("programName", localized(settings.language, program.program.nameHu, program.program.nameEn,
                    program.program.nameDe, program.program.nameRo))
                .put("dayId", day.dayId).put("workout", day.dayId)
                .put("dayName", localized(settings.language, day.nameHu, day.nameEn, day.nameDe, day.nameRo))
                .put("started", now).put("activeIntervals", JSONArray().put(JSONObject().put("start", now).put("end", JSONObject.NULL)))
                .put("currentExercise", 0).put("timerOwner", "phone").put("workoutPaused", false)
                .put("restEndAt", 0).put("restSeconds", settings.restSeconds).put("language", settings.language)
                .put("automaticEnabled", true).put("timerConfig", JSONObject().put("work", settings.workSeconds)
                    .put("rest", settings.restSeconds).put("rounds", settings.rounds)
                    .put("sound", settings.soundEnabled).put("vibration", settings.vibrationEnabled))
                .put("exercises", exercises).put("originDevice", "phone")
        }
        withContext(NonCancellable + Dispatchers.Main.immediate) {
            installPrepared(app, candidate)
        }
    }

    internal fun installPrepared(context: Context, candidate: JSONObject): JSONObject {
        // Recheck after database reads: a watch command may have started a workout meanwhile.
        recoverActive(context)?.let { return it }
        val execution = PhoneWorkoutExecution.store(context)
        check(!execution.restoreBlocked() && !execution.commandsSuspended()) { "Az edzésvezérlés átmenetileg foglalt." }
        val generation = maxOf(System.currentTimeMillis(), (execution.snapshot()?.optLong("generation") ?: 0L) + 1L)
        val next = JSONObject(candidate.toString()).put("generation", generation).put("executionRevision", 0)
        // The native frame is durable first. If snapshot publication fails, recoverActive
        // restores this same frame and IDs; it never creates a second workout.
        val prepared = execution.prepare(next, 0L)
        prepared.put("phoneExecution162", JSONObject().put("generation", generation)
            .put("revision", prepared.getLong("executionRevision")).put("sequence", 0))
        val saved = ActiveWorkoutStore.save(context, prepared)
        // A disconnected watch cannot invalidate the durable local start.
        runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, saved) }
        return saved
    }

    private fun recoverActive(context: Context): JSONObject? {
        val current = ActiveWorkoutStore.current(context)
        if (current?.optBoolean("active") == true) return current
        val execution = PhoneWorkoutExecution.store(context)
        val frame = execution.snapshot()
        if (frame?.optBoolean("active") != true) return null
        check(!execution.restoreBlocked() && !execution.commandsSuspended()) { "Az edzésvezérlés átmenetileg foglalt." }
        check(frame.optString("timerOwner") == "phone") { "Előbb egyeztesd az órás edzés tulajdonosát." }
        return ActiveWorkoutStore.save(context, frame)
    }

    private fun localized(language: String, hu: String, en: String, de: String, ro: String): String =
        when (language) { "hu" -> hu; "de" -> de; "ro" -> ro; else -> en }.ifBlank { hu }
}

/** Port of accepted main tp228Strength/tp228SetValue/tp228BuildSets, not a new prescription policy. */
internal object NativeWorkoutStartValues {
    fun strength(exercise: JSONObject): Boolean = exercise.optString("measurementType").lowercase() !in setOf("distance", "time", "gps") &&
        exercise.optString("repUnit") in setOf("", "ism.", "reps", "/kar", "/láb")

    fun blankSets(count: Int, weight: Double): JSONArray = JSONArray().apply {
        repeat(count.coerceIn(1, 10)) { index -> put(JSONObject().put("set", index + 1)
            .put("setId", UUID.randomUUID().toString()).put("weight", weight).put("reps", "").put("done", false)) }
    }

    /** Candidates arrive newest finished first, page by page; each set position has its own fallback. */
    fun prefill(exercise: JSONObject, sets: JSONArray, history: JSONObject, now: Long) {
        if (!strength(exercise) || history.optBoolean("deleted") || history.has("deletedAt") && !history.isNull("deletedAt") &&
            history.optString("deletedAt").isNotEmpty() || history.optBoolean("cancelled") || history.optString("status") == "cancelled") return
        val start = runCatching { Instant.parse(history.getString("started")).toEpochMilli() }.getOrNull() ?: return
        val finish = runCatching { Instant.parse(history.getString("finished")).toEpochMilli() }.getOrNull() ?: return
        if (finish <= start || finish > now) return
        val source = history.optJSONArray("exercises") ?: return
        val matches = (0 until source.length()).mapNotNull { source.optJSONObject(it) }.filter { it.optString("id") == exercise.optString("id") }
        if (matches.size != 1) return
        val previous = matches.single()
        if (!strength(previous) || previous.optString("loadType") != exercise.optString("loadType") ||
            previous.optString("repUnit") != exercise.optString("repUnit")) return
        val oldSets = previous.optJSONArray("sets") ?: return
        for (index in 0 until sets.length()) {
            val set = sets.getJSONObject(index)
            if (set.has("previousValues228")) continue
            val old = oldSets.optJSONObject(index) ?: continue
            if (old.opt("done") != true || old.optDouble("leftSeconds", 0.0) > 0 ||
                old.optDouble("rightSeconds", 0.0) > 0 || old.optDouble("distanceMeters", 0.0) > 0) continue
            val text = old.optString("weight").replace(',', '.')
            val reps = old.optString("reps")
            val weight = text.toDoubleOrNull() ?: continue
            val repetitions = reps.toIntOrNull() ?: continue
            if (!Regex("(?:\\d+(?:\\.\\d+)?|\\.\\d+)").matches(text) || !weight.isFinite() || weight > 100000 ||
                !Regex("[0-9]+").matches(reps) || repetitions !in 1..10000) continue
            val load = if (exercise.optString("loadType") == "bodyweight") 0.0 else weight
            set.put("weight", load).put("reps", reps).put("previousValues228", JSONObject().put("weight", load).put("reps", reps))
        }
    }
}
