package com.repforge.app.data.migration

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.WorkoutHistorySetEntity
import com.repforge.app.data.db.WorkoutPhotoEntity
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.LegacySyncIdentity18
import com.repforge.shared.SetEditorRules212
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.AtomicFile
import androidx.room.withTransaction
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * One-time migrator from TrainPilot v1.3.71 hybrid storage
 * (CapacitorStorage SharedPreferences + Chromium WebView LevelDB localStorage)
 * into Room + DataStore, plus canonical JSON backup/restore mapper.
 */
class LegacyWebViewMigrator(
    private val context: Context,
    private val database: TrainPilotDatabase,
    private val settingsStore: TrainPilotSettingsStore
) {
    companion object {
        internal fun decodeSnapshot(values: Map<String, String>): JSONObject {
            val root = JSONObject()
            val arrays = setOf("history", "weights", "scheduled", "exercises", "programs", "phase3HistoryTombstones")
            for ((key, rawValue) in values) {
                val value = try { org.json.JSONTokener(rawValue).nextValue() }
                catch (failure: Exception) { throw IllegalStateException("Invalid legacy value for $key", failure) }
                check(key !in arrays || value is JSONArray) { "Invalid legacy array: $key" }
                check(key != "settings" || value is JSONObject) { "Invalid legacy settings" }
                root.put(key, value)
            }
            root.optJSONArray("history")?.let { history ->
                for (i in 0 until history.length()) {
                    val row = history.getJSONObject(i)
                    check(row.optString("started").isNotBlank()) { "Legacy workout has no start identity" }
                }
            }
            return root
        }

        val KNOWN_DB_KEYS = setOf(
            "history",
            "weights",
            "scheduled",
            "settings",
            "exercises",
            "plan",
            "programs",
            "activeProgramId",
            "plannerSettings",
            "draft",
            "gpsTrip106",
            "wearClosedWorkouts",
            "phase3HistoryTombstones",
            "cloudDevice",
            "cloudStatus",
            "cloudRetention105"
        )
    }

    /**
     * Runs once on startup. Extracts any existing v1.3.71 WebView localStorage or
     * Capacitor SharedPreferences keys, imports them into Room + DataStore, seeds
     * builtin exercises/programs, and sets migration_v1_3_71_complete = true.
     */
    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        val currentSettings = settingsStore.current()
        if (currentSettings.migrationV1371Complete) {
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(database)
            return@withContext
        }

        val extractedKeys = extractAllLegacyKeys()
        // Preserve every captured key, including fields not yet projected into Room.
        // Never include this private archive in cloud/Android backup (GPS/privacy).
        val archiveDir = File(context.noBackupFilesDir, "native-migration-v1371")
        check(archiveDir.isDirectory || archiveDir.mkdirs()) { "Cannot create migration recovery archive" }
        val archive = AtomicFile(File(archiveDir, "legacy-localstorage.json"))
        val snapshot = JSONObject().apply {
            put("origin", "https://localhost")
            put("values", JSONObject(extractedKeys as Map<*, *>))
        }.toString().toByteArray(Charsets.UTF_8)
        val stream = archive.startWrite()
        try { stream.write(snapshot); archive.finishWrite(stream) }
        catch (failure: Exception) { archive.failWrite(stream); throw failure }
        if (extractedKeys.isNotEmpty()) {
            val compositeRoot = decodeSnapshot(extractedKeys)
            database.withTransaction { importBackupJson(compositeRoot, replaceAll = false) }
        }

        TrainPilotDatabase.ensureBuiltinCatalogSeeded(database)
        settingsStore.ensureCloudDeviceId()
        settingsStore.markMigrationComplete()
    }

    /**
     * Reads keys from both Capacitor SharedPreferences files and Chromium WebView's
     * app_webview/Default/Local Storage/leveldb directory (.ldb and .log files).
     */
    private suspend fun extractAllLegacyKeys(): Map<String, String> {
        val result = LinkedHashMap<String, String>()

        // 1. Check Capacitor & RepForge SharedPreferences files
        val prefNames = listOf("CapacitorStorage", "com.getcapacitor.plugin.preferences", "repforge", "trainpilot")
        for (prefName in prefNames) {
            try {
                val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for ((rawKey, value) in prefs.all) {
                    val cleanKey = rawKey.removePrefix("_cap_").removePrefix("repforge:").removePrefix("trainpilot:")
                    if ((cleanKey in KNOWN_DB_KEYS || cleanKey.startsWith("cloud")) && value is String && value.isNotBlank()) {
                        result[cleanKey] = value
                    }
                }
            } catch (failure: Exception) {
                throw IllegalStateException("Cannot read legacy preferences", failure)
            }
        }

        // The original UI used https://localhost. Reading LevelDB bytes cannot
        // distinguish deleted/obsolete/compressed records and is not a migration.
        // Opening Chromium also works for a clean install (an empty namespace).
        val webValues = LegacyLocalStorageReader(context).read()
        result["legacyStorageRaw"] = JSONObject(webValues as Map<*, *>).toString()
        for ((key, value) in webValues.filterKeys { it.startsWith("trainpilot:") }) result[key.removePrefix("trainpilot:")] = value
        // repforge: is the canonical v1.3.71 namespace; preserve alternate keys in the archive.
        for ((key, value) in webValues.filterKeys { it.startsWith("repforge:") }) result[key.removePrefix("repforge:")] = value
        return result
    }

    /**
     * Imports a full TrainPilot backup/sync JSON payload (`backup.json` or `RepForgeSync.data`)
     * into the Room database and DataStore.
     */
    suspend fun importBackupJson(rootInput: JSONObject, replaceAll: Boolean) = withContext(Dispatchers.IO) {
        val data = if (rootInput.optString("app") == "RepForgeSync" && rootInput.optJSONObject("data") != null) {
            rootInput.getJSONObject("data")
        } else {
            rootInput
        }

        data.optString("language").takeIf { it in listOf("hu", "en", "de", "ro") }?.let { settingsStore.setLanguage(it) }
        // Preserve all original documents, even fields not yet projected by native screens.
        // The immutable file archive holds the whole source; avoid duplicating full
        // normalized history/catalog collections again in the key-value table.
        val projected = setOf("history", "exercises", "programs", "weights", "scheduled", "legacyStorageRaw")
        for (key in data.keys()) if (key !in projected) database.keyValueDocumentDao().putDocument(
            KeyValueDocumentEntity(key = "legacy:$key", json = JSONObject().put("value", data.get(key)).toString(), updatedAt = System.currentTimeMillis())
        )
        // 1. Settings & Theme
        val settingsObj = data.optJSONObject("settings")
        if (settingsObj != null) {
            val lang = settingsObj.optString("language", settingsObj.optString("lang", ""))
            if (lang in listOf("hu", "en", "de", "ro")) settingsStore.setLanguage(lang)
            if (settingsObj.has("restSeconds")) {
                settingsStore.setTimerDefaults(
                    workSeconds = settingsObj.optInt("workSeconds", 30),
                    restSeconds = settingsObj.optInt("restSeconds", 90),
                    rounds = settingsObj.optInt("rounds", 1),
                    sound = settingsObj.optBoolean("sound", true),
                    vibration = settingsObj.optBoolean("vibration", true)
                )
            }
            if (settingsObj.has("hapticSetRecorded227")) {
                settingsStore.setHapticSetRecorded227(settingsObj.optBoolean("hapticSetRecorded227", true))
            }
            if (settingsObj.has("healthBackupConsent")) {
                settingsStore.setHealthBackupConsent(settingsObj.optBoolean("healthBackupConsent", false))
            }
            val themeKey = settingsObj.optString("themeKey", data.optString("themeKey", ""))
            val matchedTheme = TrainPilotSettingsStore.AVAILABLE_THEMES.firstOrNull { it.key == themeKey }
            if (matchedTheme != null) settingsStore.setTheme(matchedTheme)
        }
        val activeProg = data.optString("activeProgramId", "")
        if (activeProg.isNotBlank()) settingsStore.setActiveProgramId(activeProg)

        // 2. Tombstones (phase3HistoryTombstones)
        val tombstonesArr = data.optJSONArray("phase3HistoryTombstones")
        if (tombstonesArr != null) {
            if (replaceAll) database.workoutHistoryDao().deleteAllTombstones()
            val list = ArrayList<HistoryTombstoneEntity>()
            for (i in 0 until tombstonesArr.length()) {
                val t = tombstonesArr.optJSONObject(i) ?: continue
                val ids = t.optJSONArray("ids") ?: JSONArray()
                val key = t.optString("key", "")
                val primary = if (ids.length() > 0) ids.optString(0) else key
                if (primary.isNotBlank()) {
                    list.add(
                        HistoryTombstoneEntity(
                            primaryId = primary,
                            workoutKey = key,
                            idsJson = ids.toString(),
                            deletedAt = t.optLong("deletedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            database.workoutHistoryDao().upsertTombstones(list)
        }

        // 3. Exercises Catalog
        val exercisesArr = data.optJSONArray("exercises")
        if (exercisesArr != null) {
            if (replaceAll) database.exerciseLibraryDao().deleteAllCustomExercises()
            val entities = ArrayList<ExerciseLibraryEntity>()
            for (i in 0 until exercisesArr.length()) {
                val ex = exercisesArr.optJSONObject(i) ?: continue
                val id = ex.optString("id").trim()
                if (id.isEmpty()) continue
                val muscles = ex.optJSONArray("muscles") ?: JSONArray()
                entities.add(
                    ExerciseLibraryEntity(
                        id = id,
                        nameHu = ex.optString("hu", ex.optString("name", id)),
                        nameEn = ex.optString("en", ex.optString("name", id)),
                        nameDe = ex.optString("de", ex.optString("en", id)),
                        nameRo = ex.optString("ro", ex.optString("en", id)),
                        loadType = ex.optString("loadType", "weight"),
                        repUnit = ex.optString("repUnit", "ism."),
                        measurementType = ex.optString("measurementType", "reps"),
                        defaultSets = ex.optInt("sets", 3).coerceIn(1, 20),
                        defaultReps = ex.optString("reps", "10"),
                        defaultWeight = ex.optDouble("weight", 0.0).let { if (it.isFinite() && it >= 0.0) it else 0.0 },
                        defaultRestSeconds = ex.optInt("restSeconds", 90).coerceIn(0, 3600),
                        primaryMuscle = ex.optString("primaryMuscle", if (muscles.length() > 0) muscles.optString(0, "full") else "full"),
                        muscleGroupsJson = muscles.toString(),
                        notesHu = ex.optString("notes", ""),
                        notesEn = ex.optString("notesEn", ex.optString("notes", "")),
                        notesDe = ex.optString("notesDe", ""),
                        notesRo = ex.optString("notesRo", ""),
                        videoUrl = ex.optString("videoUrl").takeIf { it.isNotBlank() },
                        videoProvider = ex.optString("provider").takeIf { it.isNotBlank() },
                        custom = ex.optBoolean("custom", true),
                        setRir229 = ex.optBoolean("setRir229", false),
                        rawJson = ex.toString()
                    )
                )
            }
            database.exerciseLibraryDao().upsertExercises(entities)
        }

        // 4. Programs & Days
        val programsArr = data.optJSONArray("programs")
        if (programsArr != null) {
            if (replaceAll) database.programDao().deleteAllPrograms()
            val progEntities = ArrayList<ProgramEntity>()
            val dayEntities = ArrayList<ProgramDayEntity>()
            for (i in 0 until programsArr.length()) {
                val p = programsArr.optJSONObject(i) ?: continue
                val pid = p.optString("id").trim()
                if (pid.isEmpty()) continue
                val name = p.optString("name", pid)
                progEntities.add(
                    ProgramEntity(
                        id = pid,
                        nameHu = p.optString("hu", name),
                        nameEn = p.optString("en", name),
                        nameDe = p.optString("de", name),
                        nameRo = p.optString("ro", name),
                        location = p.optString("location", "Otthon"),
                        level = p.optString("level", "Kezdő"),
                        description = p.optString("description", ""),
                        builtin = p.optBoolean("builtin", false),
                        generated = p.optBoolean("generated", false),
                        sourceType = p.optString("sourceType", "own"),
                        sourceTemplateId = p.optString("sourceTemplateId").takeIf { it.isNotBlank() },
                        templateVersion = p.optInt("templateVersion", 1),
                        accentHex = p.optString("accent", "#4d8fbe"),
                        sortOrder = i,
                        updatedAt = p.optLong("updatedAt", System.currentTimeMillis()),
                        rawJson = p.toString()
                    )
                )
                val daysArr = p.optJSONArray("days")
                if (daysArr != null) {
                    for (dIdx in 0 until daysArr.length()) {
                        val d = daysArr.optJSONObject(dIdx) ?: continue
                        val did = d.optString("id", (dIdx + 1).toString())
                        val dName = d.optString("name", did)
                        dayEntities.add(
                            ProgramDayEntity(
                                compositeId = "$pid:$did",
                                programId = pid,
                                dayId = did,
                                nameHu = d.optString("hu", dName),
                                nameEn = d.optString("en", dName),
                                nameDe = d.optString("de", dName),
                                nameRo = d.optString("ro", dName),
                                sortOrder = dIdx,
                                exercisesJson = (d.optJSONArray("exercises") ?: JSONArray()).toString()
                            )
                        )
                    }
                }
            }
            database.programDao().upsertPrograms(progEntities)
            database.programDao().upsertProgramDays(dayEntities)
        }

        // 5. Workout History
        val historyArr = data.optJSONArray("history")
        if (historyArr != null) {
            if (replaceAll) database.workoutHistoryDao().deleteAllWorkouts()
            for (i in 0 until historyArr.length()) {
                val h = historyArr.optJSONObject(i) ?: continue
                saveWorkoutJsonToRoom(h)
            }
        }

        // 6. Body Weights
        val weightsArr = data.optJSONArray("weights")
        if (weightsArr != null) {
            if (replaceAll) database.bodyWeightDao().deleteAllWeights()
            val weightEntities = ArrayList<BodyWeightEntryEntity>()
            for (i in 0 until weightsArr.length()) {
                val w = weightsArr.optJSONObject(i) ?: continue
                val kg = w.optDouble("kg", Double.NaN)
                val date = w.optString("date").trim()
                if (!kg.isFinite() || kg <= 0.0 || date.isEmpty()) continue
                val id = w.optString("id").takeIf { it.isNotBlank() } ?: LegacySyncIdentity18.legacyWeightId(w)
                val dayKey = if (date.length >= 10) date.substring(0, 10) else date
                val copyJson = JSONObject(w.toString()).put("id", id)
                weightEntities.add(
                    BodyWeightEntryEntity(
                        id = id,
                        date = date,
                        dayKey = dayKey,
                        kg = (kg * 10.0).roundToInt() / 10.0,
                        updatedAt = w.optLong("updatedAt", System.currentTimeMillis()),
                        rawJson = copyJson.toString()
                    )
                )
            }
            database.bodyWeightDao().upsertWeights(weightEntities)
        }

        // 7. Scheduled Calendar Entries
        val scheduledArr = data.optJSONArray("scheduled")
        if (scheduledArr != null) {
            if (replaceAll) database.calendarScheduleDao().deleteAllEntries()
            val schedEntities = ArrayList<CalendarScheduleEntity>()
            for (i in 0 until scheduledArr.length()) {
                val s = scheduledArr.optJSONObject(i) ?: continue
                val startIso = s.optString("start", s.optString("plannedStart", "")).trim()
                val date = s.optString("date", if (startIso.length >= 10) startIso.substring(0, 10) else "").trim()
                if (date.isEmpty()) continue
                val id = s.optString("id", s.optString("scheduleId", "")).ifBlank {
                    "legacy-schedule-" + UUID.nameUUIDFromBytes(s.toString().toByteArray(Charsets.UTF_8))
                }
                val programId = s.optString("programId", "")
                val dayId = s.optString("dayId", s.optString("workout", ""))
                val googleId = s.optString("googleEventId").takeIf { it.matches(Regex("rf[0-9a-f]{64}")) }
                    ?: LegacySyncIdentity18.calendarEventId(id, date, programId, dayId)
                schedEntities.add(
                    CalendarScheduleEntity(
                        id = id,
                        googleEventId = googleId,
                        date = date,
                        startIso = startIso,
                        endIso = s.optString("end", ""),
                        programId = programId,
                        programName = s.optString("programName", ""),
                        dayId = dayId,
                        dayName = s.optString("dayName", dayId),
                        status = s.optString("status", "planned"),
                        updatedAt = s.optLong("updatedAt", System.currentTimeMillis()),
                        rawJson = s.toString()
                    )
                )
            }
            database.calendarScheduleDao().upsertEntries(schedEntities)
        }

        // 8. Auxiliary JSON Documents (draft, plannerSettings, plan, wearClosedWorkouts, cloud*)
        for (docKey in listOf("draft", "plannerSettings", "plan", "gpsTrip106", "wearClosedWorkouts", "cloudStatus", "cloudRetention105")) {
            if (data.has(docKey) && !data.isNull(docKey)) {
                database.keyValueDocumentDao().putDocument(
                    KeyValueDocumentEntity(
                        key = docKey,
                        json = data.get(docKey).toString(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Parses a canonical workout history JSONObject and persists it with all exercises,
     * sets, RIRs (#229), GPS route keys (#163/#193), and photo references into Room.
     */
    suspend fun saveWorkoutJsonToRoom(h: JSONObject) {
        val started = h.optString("started", "").trim()
        if (started.isEmpty()) return
        val finished = h.optString("finished", started).trim()
        val id = h.optString("id").takeIf { it.isNotBlank() } ?: LegacySyncIdentity18.legacyHistoryId(h)
        val identities = setOf(id, h.optString("syncId"), h.optString("workoutId"), LegacySyncIdentity18.legacyHistoryId(h)) - ""
        for (tombstone in database.workoutHistoryDao().getTombstones()) {
            val ids = JSONArray(tombstone.idsJson)
            if ((0 until ids.length()).any { ids.optString(it) in identities } ||
                (tombstone.workoutKey.isNotBlank() && tombstone.workoutKey == h.optString("key"))) return
        }
        val syncId = h.optString("syncId", h.optString("workoutId", id)).ifBlank { id }
        val workoutKey = h.optString("key", syncId).ifBlank { started }
        val startedMs = runCatching { Instant.parse(started).toEpochMilli() }.getOrDefault(0L)
        val finishedMs = runCatching { Instant.parse(finished).toEpochMilli() }.getOrDefault(startedMs)
        val durationSec = h.optInt(
            "durationSeconds",
            max(0L, (finishedMs - startedMs) / 1000L).toInt()
        )

        val exercisesWithSets = ArrayList<Pair<WorkoutHistoryExerciseEntity, List<WorkoutHistorySetEntity>>>()
        val srcExercises = h.optJSONArray("exercises") ?: JSONArray()
        var totalVolume = 0.0
        var completedSets = 0
        var totalSets = 0
        var totalReps = 0

        for (eIdx in 0 until srcExercises.length()) {
            val exObj = srcExercises.optJSONObject(eIdx) ?: continue
            val exId = exObj.optString("id", "exercise-$eIdx")
            val repUnit = exObj.optString("repUnit", "ism.")
            val loadType = exObj.optString("loadType", "weight")
            val measurementType = exObj.optString("measurementType", "reps")
            val isBilateral = SetEditorRules212.normalizeUnit(repUnit) == "mp/oldal" || exId == "side-plank"
            val isTimed = isBilateral || SetEditorRules212.normalizeUnit(repUnit).startsWith("mp")

            val exerciseEntity = WorkoutHistoryExerciseEntity(
                workoutHistoryId = id,
                exerciseId = exId,
                nameHu = exObj.optString("hu", exObj.optString("name", exId)),
                nameEn = exObj.optString("en", exObj.optString("name", exId)),
                loadType = loadType,
                repUnit = repUnit,
                measurementType = measurementType,
                orderIndex = eIdx,
                actualRir = SetEditorRules212.parseActualRir229(exObj),
                setRir229 = exObj.optBoolean("setRir229", false)
            )

            val setsList = ArrayList<WorkoutHistorySetEntity>()
            val srcSets = exObj.optJSONArray("sets") ?: JSONArray()
            for (sIdx in 0 until srcSets.length()) {
                val sObj = srcSets.optJSONObject(sIdx) ?: continue
                val setNum = sObj.optInt("set", sObj.optInt("number", sIdx + 1))
                val setId = sObj.optString("setId").ifBlank { "$id:$exId:$setNum" }
                val done = sObj.optBoolean("done", true)
                val weightVal = sObj.optString("weight", "").replace(',', '.').toDoubleOrNull()
                val repsStr = sObj.opt("reps")?.toString().orEmpty()
                val repsNum = repsStr.replace(',', '.').toDoubleOrNull()?.roundToInt() ?: 0
                val leftSec = sObj.optInt("leftSeconds", 0).takeIf { it > 0 }
                    ?: if (isBilateral && repsNum > 0) repsNum else null
                val rightSec = sObj.optInt("rightSeconds", 0).takeIf { it > 0 }
                    ?: if (isBilateral && repsNum > 0) repsNum else null
                val distMeters = if (sObj.has("distanceMeters") && !sObj.isNull("distanceMeters")) {
                    sObj.optDouble("distanceMeters").takeIf { it.isFinite() && it >= 0.0 }
                } else null

                totalSets++
                if (done) {
                    completedSets++
                    if (!isTimed && repsNum > 0) {
                        totalReps += repsNum
                        if (weightVal != null && weightVal > 0.0) {
                            totalVolume += weightVal * repsNum
                        }
                    }
                }

                setsList.add(
                    WorkoutHistorySetEntity(
                        setId = setId,
                        workoutHistoryId = id,
                        historyExerciseRowId = 0L,
                        exerciseId = exId,
                        setNumber = setNum,
                        weight = weightVal,
                        reps = repsStr,
                        leftSeconds = leftSec,
                        rightSeconds = rightSec,
                        distanceMeters = distMeters,
                        done = done,
                        actualRir = SetEditorRules212.parseActualRir229(sObj),
                        gpsRouteKey163 = sObj.optString("gpsRouteKey163").takeIf { it.isNotBlank() },
                        gpsRouteKey193 = sObj.optString("gpsRouteKey193").takeIf { it.isNotBlank() }
                    )
                )
            }
            exercisesWithSets.add(exerciseEntity to setsList)
        }

        val photosList = ArrayList<WorkoutPhotoEntity>()
        val srcPhotos = h.optJSONArray("photos")
        if (srcPhotos != null) {
            for (pIdx in 0 until srcPhotos.length()) {
                val pObj = srcPhotos.optJSONObject(pIdx) ?: continue
                val pid = pObj.optString("id")
                if (!pid.matches(Regex("[a-f0-9-]{36}"))) continue
                val deletedAt = if (pObj.has("deletedAt") && !pObj.isNull("deletedAt") &&
                    pObj.opt("deletedAt") != false && pObj.optString("deletedAt") != "0" &&
                    pObj.optString("deletedAt").isNotBlank()
                ) pObj.optString("deletedAt") else null
                photosList.add(
                    WorkoutPhotoEntity(
                        id = pid,
                        workoutHistoryId = id,
                        label = pObj.optString("label", ""),
                        createdAt = pObj.optString("createdAt", started),
                        updatedAt = pObj.optLong("updatedAt", System.currentTimeMillis()),
                        width = pObj.optInt("width", 0),
                        height = pObj.optInt("height", 0),
                        bytes = pObj.optLong("bytes", 0L),
                        driveFileId = pObj.optString("driveFileId").takeIf { it.isNotBlank() },
                        deletedAt = deletedAt
                    )
                )
            }
        }

        val feedbackObj = h.optJSONObject("feedback")
        val effort = feedbackObj?.optString("effort")?.takeIf { it.isNotBlank() }
            ?: h.optString("effortFeedback").takeIf { it.isNotBlank() }
        val skipped = feedbackObj?.optBoolean("skipped", false) ?: false
        val feedbackUpdatedAt = feedbackObj?.optLong("updatedAt", 0L) ?: 0L

        val canonicalRaw = JSONObject(h.toString()).put("id", id)
        val workoutEntity = WorkoutHistoryEntity(
            id = id,
            workoutKey = workoutKey,
            syncId = syncId,
            started = started,
            finished = finished,
            startedEpochMs = startedMs,
            finishedEpochMs = finishedMs,
            durationSeconds = durationSec,
            programId = h.optString("programId", ""),
            programName = h.optString("programName", ""),
            dayId = h.optString("dayId", h.optString("workout", "")),
            dayName = h.optString("dayName", h.optString("workout", "Edzés")),
            scheduleId = h.optString("scheduleId", ""),
            quickWorkout = h.optBoolean("quickWorkout", false),
            originDevice = h.optString("originDevice", if (h.optString("timerOwner") == "watch") "watch" else "phone"),
            ownershipProtocol193 = h.optInt("ownershipProtocol193", 0),
            ownerEpoch193 = h.optLong("ownerEpoch193", 0L),
            effortFeedback = effort,
            feedbackSkipped = skipped,
            feedbackUpdatedAt = feedbackUpdatedAt,
            totalVolumeKg = totalVolume,
            completedSetsCount = completedSets,
            totalSetsCount = totalSets,
            totalRepsCount = totalReps,
            hasPr226 = h.optJSONArray("prRecords226")?.let { it.length() > 0 } ?: false,
            prRecordsJson226 = (h.optJSONArray("prRecords226") ?: JSONArray()).toString(),
            wearMetricsJson = (h.optJSONObject("wear") ?: h.optJSONObject("healthSummary"))?.toString(),
            health240Json = (h.optJSONObject("health240") ?: h.optJSONObject("healthConnect"))?.toString(),
            timeLogs162Json = h.optJSONObject("timeLogs162")?.toString(),
            activeIntervalsJson = h.optJSONArray("activeIntervals")?.toString(),
            editedAt = h.optString("editedAt").takeIf { it.isNotBlank() },
            rawJson = canonicalRaw.toString()
        )

        database.workoutHistoryDao().saveFullWorkout(workoutEntity, exercisesWithSets, photosList)
    }

    /**
     * Exports the current Room + DataStore state into the exact canonical `backup.json` /
     * `RepForgeSync` data JSONObject format expected by `BackupArchivePlugin` and `GoogleSyncPlugin`.
     */
    suspend fun exportBackupJson(includeHealthJournal: Boolean, healthSnapshot: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val settings = settingsStore.current()
            val out = JSONObject()

            // History
            val historyArray = JSONArray()
            for (item in database.workoutHistoryDao().getAllWorkoutsWithDetails()) {
                val raw = try {
                    JSONObject(item.workout.rawJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                raw.put("id", item.workout.id)
                raw.put("started", item.workout.started)
                raw.put("finished", item.workout.finished)
                raw.put("programId", item.workout.programId)
                raw.put("programName", item.workout.programName)
                raw.put("dayId", item.workout.dayId)
                raw.put("workout", item.workout.dayId)
                raw.put("dayName", item.workout.dayName)
                if (item.workout.effortFeedback != null || item.workout.feedbackSkipped) {
                    raw.put(
                        "feedback",
                        JSONObject()
                            .apply {
                                if (item.workout.effortFeedback != null) put("effort", item.workout.effortFeedback)
                                if (item.workout.feedbackSkipped) put("skipped", true)
                                put("updatedAt", item.workout.feedbackUpdatedAt)
                            }
                    )
                }
                val exArr = JSONArray()
                for (exWithSets in item.exercises.sortedBy { it.exercise.orderIndex }) {
                    val ex = exWithSets.exercise
                    val exJson = JSONObject()
                        .put("id", ex.exerciseId)
                        .put("name", ex.nameHu)
                        .put("hu", ex.nameHu)
                        .put("en", ex.nameEn)
                        .put("loadType", ex.loadType)
                        .put("repUnit", ex.repUnit)
                        .put("measurementType", ex.measurementType)
                        .put("setRir229", ex.setRir229)
                    if (ex.actualRir != null) exJson.put("actualRir", ex.actualRir)
                    val sArr = JSONArray()
                    for (s in exWithSets.sets.sortedBy { it.setNumber }) {
                        val sJson = JSONObject()
                            .put("set", s.setNumber)
                            .put("setId", s.setId)
                            .put("reps", s.reps)
                            .put("weight", s.weight ?: 0.0)
                            .put("done", s.done)
                        if (s.leftSeconds != null && s.leftSeconds > 0) sJson.put("leftSeconds", s.leftSeconds)
                        if (s.rightSeconds != null && s.rightSeconds > 0) sJson.put("rightSeconds", s.rightSeconds)
                        if (s.distanceMeters != null) sJson.put("distanceMeters", s.distanceMeters)
                        if (s.actualRir != null) sJson.put("actualRir", s.actualRir)
                        if (s.gpsRouteKey163 != null) sJson.put("gpsRouteKey163", s.gpsRouteKey163)
                        if (s.gpsRouteKey193 != null) sJson.put("gpsRouteKey193", s.gpsRouteKey193)
                        sArr.put(sJson)
                    }
                    exJson.put("sets", sArr)
                    exArr.put(exJson)
                }
                raw.put("exercises", exArr)
                if (item.photos.isNotEmpty()) {
                    val pArr = JSONArray()
                    for (p in item.photos) {
                        val pJson = JSONObject()
                            .put("id", p.id)
                            .put("label", p.label)
                            .put("createdAt", p.createdAt)
                            .put("updatedAt", p.updatedAt)
                            .put("width", p.width)
                            .put("height", p.height)
                            .put("bytes", p.bytes)
                        if (p.driveFileId != null) pJson.put("driveFileId", p.driveFileId)
                        if (p.deletedAt != null) pJson.put("deletedAt", p.deletedAt)
                        pArr.put(pJson)
                    }
                    raw.put("photos", pArr)
                }
                historyArray.put(raw)
            }
            out.put("history", historyArray)

            // Weights
            val weightsArray = JSONArray()
            for (w in database.bodyWeightDao().getAllWeights()) {
                val wJson = try {
                    JSONObject(w.rawJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                wJson.put("id", w.id).put("date", w.date).put("kg", w.kg).put("updatedAt", w.updatedAt)
                weightsArray.put(wJson)
            }
            out.put("weights", weightsArray)

            // Scheduled
            val scheduledArray = JSONArray()
            for (s in database.calendarScheduleDao().getAllEntries()) {
                val sJson = try {
                    JSONObject(s.rawJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                sJson.put("id", s.id)
                    .put("scheduleId", s.id)
                    .put("googleEventId", s.googleEventId)
                    .put("date", s.date)
                    .put("start", s.startIso)
                    .put("plannedStart", s.startIso)
                    .put("end", s.endIso)
                    .put("programId", s.programId)
                    .put("programName", s.programName)
                    .put("dayId", s.dayId)
                    .put("dayName", s.dayName)
                    .put("status", s.status)
                    .put("updatedAt", s.updatedAt)
                scheduledArray.put(sJson)
            }
            out.put("scheduled", scheduledArray)

            // Programs
            val programsArray = JSONArray()
            for (pWithDays in database.programDao().getProgramsWithDays()) {
                val p = pWithDays.program
                val pJson = try {
                    JSONObject(p.rawJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                pJson.put("id", p.id).put("name", p.nameHu).put("builtin", p.builtin)
                val dArr = JSONArray()
                for (d in pWithDays.days.sortedBy { it.sortOrder }) {
                    dArr.put(
                        JSONObject()
                            .put("id", d.dayId)
                            .put("name", d.nameHu)
                            .put("exercises", JSONArray(d.exercisesJson))
                    )
                }
                pJson.put("days", dArr)
                programsArray.put(pJson)
            }
            out.put("programs", programsArray)

            // Exercises
            val exercisesArray = JSONArray()
            for (ex in database.exerciseLibraryDao().getAllExercises()) {
                exercisesArray.put(JSONObject(ex.rawJson))
            }
            out.put("exercises", exercisesArray)

            // Tombstones
            val tombstonesArray = JSONArray()
            for (t in database.workoutHistoryDao().getTombstones()) {
                tombstonesArray.put(
                    JSONObject()
                        .put("ids", JSONArray(t.idsJson))
                        .put("key", t.workoutKey)
                        .put("deletedAt", t.deletedAt)
                )
            }
            out.put("phase3HistoryTombstones", tombstonesArray)

            // Settings
            out.put("activeProgramId", settings.activeProgramId)
            out.put("themeAccent", settings.themeAccent)
            out.put(
                "settings",
                JSONObject()
                    .put("language", settings.language)
                    .put("themeKey", settings.themeKey)
                    .put("restSeconds", settings.restSeconds)
                    .put("workSeconds", settings.workSeconds)
                    .put("rounds", settings.rounds)
                    .put("sound", settings.soundEnabled)
                    .put("vibration", settings.vibrationEnabled)
                    .put("hapticSetRecorded227", settings.hapticSetRecorded227)
                    .put("healthBackupConsent", settings.healthBackupConsent)
                    .put("onboarding128", settings.onboarding128)
            )

            if (includeHealthJournal && healthSnapshot != null) {
                out.put("nativeHealthJournal", healthSnapshot)
            }

            out
        }
}
