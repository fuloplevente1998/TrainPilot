package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.store.TrainPilotUserSettings
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

    suspend fun startWorkout(context: Context, requestedProgramId: String?, dayId: String, scheduleId: String? = null,
        database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)): JSONObject =
        startProgramWorkout(context, requestedProgramId, dayId, scheduleId, database = database).snapshot

    /** The initial GPS choice applies only to the starting activity, never to recovery or a later exercise. */
    suspend fun startProgramWorkout(context: Context, requestedProgramId: String?, dayId: String, scheduleId: String? = null,
        gpsActivityId: String? = null, recordRoute: Boolean = false,
        database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)): Launch = startLock.withLock {
        val app = context.applicationContext
        // Never replace an existing phone session or a watch-owned mirror on a repeated tap.
        withContext(Dispatchers.Main.immediate) { recoverActive(app) }?.let { return@withLock Launch(it) }
        val db = database
        val settings = TrainPilotSettingsStore(app).current()
        var expectedSchedule: com.repforge.app.data.db.CalendarScheduleEntity? = null
        val candidate = withContext(Dispatchers.IO) {
            // New sessions must not overwrite an imported draft; Home exposes its explicit continuation.
            check(db.keyValueDocumentDao().getDocument("draft") == null) {
                "A korábbi félbehagyott edzés megmaradt. Előbb folytasd vagy rendezd azt."
            }
            val programs = db.programDao().getProgramsWithDays()
            val program = if (requestedProgramId != null) programs.firstOrNull { it.program.id == requestedProgramId }
                else programs.firstOrNull { it.program.id == settings.activeProgramId } ?: programs.firstOrNull()
            requireNotNull(program) { "Az edzésprogram nem található." }
            val day = requireNotNull(program.days.firstOrNull { it.dayId == dayId }) { "Az edzésnap nem található." }
            expectedSchedule = scheduleId?.let {
                val row = com.repforge.app.ui.calendar.NativeCalendarRepository.requireStart(
                    db.calendarScheduleDao().getEntry(it), program.program.id, day.dayId)
                check(!db.workoutHistoryDao().hasScheduleHistory(it)) { "Az alkalom már teljesített." }
                row
            }
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
                val exercise = buildExercise(db, source, settings, rx)
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
                .put("automaticEnabled", true).put("timerConfig", settings.timerConfiguration())
                .put("exercises", exercises).put("originDevice", "phone")
                .also { frame -> expectedSchedule?.let { frame.put("scheduleId", it.id) } }
        }
        val first = candidate.getJSONArray("exercises").getJSONObject(0)
        if (gpsActivityId != null) require(first.optString("id") == gpsActivityId &&
            NativeOutdoorStart.eligible(gpsActivityId, first.optString("measurementType"), first.optString("repUnit"))) {
            "Az edzés kezdő gyakorlata időközben megváltozott. Indítsd újra a kiválasztást."
        }
        require(!recordRoute || gpsActivityId != null) { "A GPS indításához kifejezett aktivitásválasztás szükséges." }
        val saved = withContext(NonCancellable + Dispatchers.IO) {
            val schedule = expectedSchedule
            if (schedule == null) withContext(Dispatchers.Main.immediate) { installPrepared(app, candidate) }
            else db.withTransaction {
                check(db.calendarScheduleDao().getEntry(schedule.id) == schedule &&
                    !db.workoutHistoryDao().hasScheduleHistory(schedule.id)) { "A naptári alkalom időközben megváltozott." }
                withContext(Dispatchers.Main.immediate) { installPrepared(app, candidate) }
            }
        }
        if (saved.optString("workoutId") != candidate.optString("workoutId")) return@withLock Launch(saved)
        if (gpsActivityId != null) TrainPilotSettingsStore(app).setGpsStartChoice(gpsActivityId, recordRoute)
        Launch(saved, if (recordRoute) NativeOutdoorStart.target(saved, gpsActivityId!!) else null)
    }

    /** Only an explicit successful new start returns a GPS intent. Never persisted/replayed on recovery. */
    data class Launch(val snapshot: JSONObject, val gpsTarget: NativeWorkoutTarget? = null)

    suspend fun startQuickWorkout(
        context: Context, exerciseId: String, recordRoute: Boolean,
        database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)
    ): Launch = startLock.withLock {
        val app = context.applicationContext
        withContext(Dispatchers.Main.immediate) { recoverActive(app) }?.let { return@withLock Launch(it) }
        val settingsStore = TrainPilotSettingsStore(app)
        val settings = settingsStore.current()
        val candidate = withContext(Dispatchers.IO) {
            check(database.keyValueDocumentDao().getDocument("draft") == null) {
                "A korábbi félbehagyott edzés megmaradt. Előbb folytasd vagy rendezd azt."
            }
            val source = requireNotNull(database.exerciseLibraryDao().getAllExercises().singleOrNull { it.id == exerciseId }) {
                "A választott gyakorlat már nem található."
            }
            require(!recordRoute || NativeOutdoorStart.eligible(source.id, source.measurementType, source.repUnit)) {
                "Ehhez a gyakorlathoz nem indítható automatikus GPS-útvonal."
            }
            quickCandidate(buildExercise(database, source, settings), settings)
        }
        withContext(NonCancellable + Dispatchers.Main.immediate) {
            val saved = installPrepared(app, candidate)
            // A watch start may have won the race while the library was read.
            if (saved.getString("workoutId") != candidate.getString("workoutId")) return@withContext Launch(saved)
            val exercise = saved.getJSONArray("exercises").getJSONObject(0)
            if (NativeOutdoorStart.eligible(exercise.optString("id"), exercise.optString("measurementType"), exercise.optString("repUnit"))) {
                settingsStore.setGpsStartChoice(exerciseId, recordRoute)
            }
            Launch(saved, if (recordRoute) NativeOutdoorStart.target(saved, exerciseId) else null)
        }
    }

    internal fun quickCandidate(exercise: JSONObject, settings: TrainPilotUserSettings): JSONObject {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val title = localized(settings.language, "Gyors edzés", "Quick workout", "Schnelltraining", "Antrenament rapid")
        return JSONObject().put("schema", 1).put("active", true).put("workoutId", id).put("id", id).put("syncId", id)
            .put("type", "quick").put("quickWorkout", true).put("programId", JSONObject.NULL).put("dayId", JSONObject.NULL)
            .put("workout", "quick").put("programName", title).put("dayName", title).put("started", now)
            .put("activeIntervals", JSONArray().put(JSONObject().put("start", now).put("end", JSONObject.NULL)))
            .put("currentExercise", 0).put("timerOwner", "phone").put("originDevice", "phone").put("workoutPaused", false)
            .put("restEndAt", 0).put("restSeconds", settings.restSeconds).put("language", settings.language)
            .put("automaticEnabled", true).put("timerConfig", settings.timerConfiguration())
            .put("exercises", JSONArray().put(JSONObject(exercise.toString())))
    }

    /** Same accepted main prefill policy for program and quick starts; measured data is always blank. */
    internal suspend fun buildExercise(db: TrainPilotDatabase, source: ExerciseLibraryEntity,
        settings: TrainPilotUserSettings, rx: JSONObject = JSONObject()): JSONObject {
        val exercise = JSONObject(source.rawJson)
            .put("id", source.id).put("hu", source.nameHu).put("en", source.nameEn).put("de", source.nameDe).put("ro", source.nameRo)
            .put("name", localized(settings.language, source.nameHu, source.nameEn, source.nameDe, source.nameRo))
            .put("loadType", rx.optString("loadType", source.loadType)).put("repUnit", rx.optString("repUnit", source.repUnit))
            .put("measurementType", rx.optString("measurementType", source.measurementType))
            .put("targetReps", rx.optString("reps", source.defaultReps)).put("setRir229", source.setRir229)
            .put("prescription", JSONObject(rx.toString()))
            .put("restSeconds162", rx.optInt("rest", settings.restSeconds).coerceIn(0, 3600))
        val weight = if (exercise.optString("loadType") == "bodyweight") 0.0
            else rx.optDouble("weight", source.defaultWeight).also { require(it.isFinite() && it >= 0) }
        val sets = NativeWorkoutStartValues.blankSets(rx.optInt("sets", source.defaultSets), weight)
        if (NativeWorkoutStartValues.strength(exercise)) {
            var offset = 0
            val now = System.currentTimeMillis()
            while (true) {
                val rows = db.workoutHistoryDao().getPrefillCandidates(source.id, exercise.getString("loadType"),
                    exercise.getString("repUnit"), now, 32, offset)
                for (row in rows) NativeWorkoutStartValues.prefill(exercise, sets, JSONObject(row.rawJson), now)
                offset += rows.size
                if (rows.size < 32 || (0 until sets.length()).all { sets.getJSONObject(it).has("previousValues228") }) break
            }
        }
        exercise.remove("actualRir")
        return exercise.put("sets", sets)
    }

    suspend fun resumeImportedDraft(context: Context,
        database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)): JSONObject = startLock.withLock {
        val app = context.applicationContext
        withContext(Dispatchers.Main.immediate) { recoverActive(app) }?.let { return@withLock it }
        val settings = TrainPilotSettingsStore(app).current()
        withContext(NonCancellable + Dispatchers.IO) {
            database.withTransaction {
                val document = requireNotNull(database.keyValueDocumentDao().getDocument("draft")) { "Nincs félbehagyott edzés." }
                val library = database.exerciseLibraryDao().getAllExercises().associateBy { it.id }
                val frame = NativeLegacyDraft.prepare(document.json, library.keys, Instant.now())
                val id = frame.getString("workoutId")
                check(!database.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id)) &&
                    !database.workoutHistoryDao().hasFinishedIdentity(id, frame.getString("started"))) { "Az edzés már mentett vagy törölt." }
                val closed = database.keyValueDocumentDao().getDocument("wearClosedWorkouts")?.json.orEmpty()
                check(!closed.contains(JSONObject.quote(id))) { "Az edzés már lezárt." }
                val exercises = frame.getJSONArray("exercises")
                for (e in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(e)
                    val source = library.getValue(exercise.getString("id"))
                    val defaults = JSONObject(source.rawJson).put("name", localized(settings.language,
                        source.nameHu, source.nameEn, source.nameDe, source.nameRo))
                        .put("loadType", source.loadType).put("repUnit", source.repUnit)
                        .put("measurementType", source.measurementType).put("targetReps", source.defaultReps)
                        .put("restSeconds162", settings.restSeconds).put("setRir229", source.setRir229)
                    for (key in defaults.keys()) if (!exercise.has(key) && key != "sets") exercise.put(key, defaults.get(key))
                }
                if (!frame.has("timerConfig")) frame.put("timerConfig", frame.optJSONObject("interval110")?.let {
                    NativeIntervalPreset.normalized(it, defaultAutoAdvance = false)
                } ?: settings.timerConfiguration())
                if (!frame.has("restSeconds")) frame.put("restSeconds", settings.restSeconds)
                frame.put("language", settings.language)
                val hash = NativeLegacyDraft.hash(document.json)
                frame.put("legacyDraftSha256", hash)
                // Archive before publishing. Never remove the original during adoption: if either
                // database fails, it remains available and a durable native frame wins on retry.
                database.keyValueDocumentDao().putDocument(com.repforge.app.data.db.KeyValueDocumentEntity(
                    "legacyDraftArchive|" + hash, document.json, System.currentTimeMillis()))
                withContext(Dispatchers.Main.immediate) {
                    val old = PhoneWorkoutExecution.store(app).snapshot()
                    check(old == null || old.optString("workoutId") != id || old.optBoolean("active")) { "A natív edzés már lezárt." }
                    installPrepared(app, frame)
                }
            }
        }
    }

    internal fun installPrepared(context: Context, candidate: JSONObject): JSONObject {
        check(!NativeDataReplacement.blocked(context)) { "Adat-visszaállítás vagy törlés van folyamatban." }
        // Recheck after database reads: a watch command may have started a workout meanwhile.
        recoverActive(context)?.let { return it }
        val execution = PhoneWorkoutExecution.store(context)
        check(!execution.restoreBlocked() && !execution.commandsSuspended()) { "Az edzésvezérlés átmenetileg foglalt." }
        val generation = maxOf(System.currentTimeMillis(), (execution.snapshot()?.optLong("generation") ?: 0L) + 1L)
        val next = JSONObject(candidate.toString()).put("generation", generation).put("executionRevision", 0)
            .put("nativeEditorProjection", 1)
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
        check(!NativeDataReplacement.blocked(context)) { "Adat-visszaállítás vagy törlés van folyamatban." }
        val current = ActiveWorkoutStore.current(context)
        if (current?.optBoolean("active") == true) return current
        val execution = PhoneWorkoutExecution.store(context)
        val frame = execution.snapshot()
        if (frame?.optBoolean("active") != true) return null
        check(current == null || current.optString("workoutId") != frame.optString("workoutId")) { "Előbb egyeztesd a lezárt edzés natív állapotát." }
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
