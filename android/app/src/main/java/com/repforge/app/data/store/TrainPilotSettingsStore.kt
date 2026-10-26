package com.repforge.app.data.store

import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeTrainingProfile
import com.repforge.app.ui.onboarding.NativeOnboardingPolicy
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private val Context.trainPilotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "trainpilot_native_settings"
)

data class ThemePaletteOption(
    val key: String,
    val nameHu: String,
    val nameEn: String,
    val accent: String,
    val accent2: String,
    val family: String, // "basic" | "vivid"
    val nameDe: String = nameEn,
    val nameRo: String = nameEn
) {
    fun displayName(language: String): String = when (language) {
        "en" -> nameEn; "de" -> nameDe; "ro" -> nameRo; else -> nameHu
    }
    fun toJson(): JSONObject = JSONObject()
        .put("key", key)
        .put("accent", accent)
        .put("accent2", accent2)
        .put("family", family)
}

data class TrainPilotUserSettings(
    val language: String = "hu",
    val activeProgramId: String = "home-basic",
    val themeKey: String = "classicBlue",
    val themeAccent: String = "#4d8fbe",
    val themeAccent2: String = "#7cb4d6",
    val themeFamily: String = "basic",
    val restSeconds: Int = 90,
    val workSeconds: Int = 30,
    val rounds: Int = 1,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val intervalAutoAdvance: Boolean = true,
    val progressionSteps: Map<String, Double> = emptyMap(),
    val hapticSetRecorded227: Boolean = true,
    val healthBackupConsent: Boolean = false,
    val autoWriteWorkoutToHealthConnect: Boolean = false,
    val onboarding128: String = "complete",
    val profileJson: String = "{}",
    val weightRange: String = "30",
    val cloudDeviceId: String = "",
    val pendingFeedbackWorkoutId: String = "",
    val pendingFeedbackStarted: String = "",
    val migrationV1371Complete: Boolean = false,
    val gpsStartChoices216: Map<String, Boolean> = emptyMap(),
    /** Unmodified settings.trainingModes197 JSON, including unknown portable extensions. */
    val trainingModes197Json: String? = null
) {
    fun timerConfiguration(): JSONObject = com.repforge.app.NativeIntervalPreset.normalized(JSONObject()
        .put("work", workSeconds).put("rest", restSeconds).put("rounds", rounds)
        .put("sound", soundEnabled).put("vibration", vibrationEnabled).put("autoAdvance", intervalAutoAdvance))

    fun themeJson(): JSONObject = JSONObject()
        .put("key", themeKey)
        .put("accent", themeAccent)
        .put("accent2", themeAccent2)
        .put("family", themeFamily)
}

class TrainPilotSettingsStore(private val context: Context) {
    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_ACTIVE_PROGRAM_ID = stringPreferencesKey("activeProgramId")
        val KEY_THEME_KEY = stringPreferencesKey("themeKey")
        val KEY_THEME_ACCENT = stringPreferencesKey("themeAccent")
        val KEY_THEME_ACCENT2 = stringPreferencesKey("themeAccent2")
        val KEY_THEME_FAMILY = stringPreferencesKey("themeFamily")
        val KEY_REST_SECONDS = intPreferencesKey("restSeconds")
        val KEY_WORK_SECONDS = intPreferencesKey("workSeconds")
        val KEY_ROUNDS = intPreferencesKey("rounds")
        val KEY_INTERVAL_AUTO_ADVANCE = booleanPreferencesKey("intervalAutoAdvance")
        val KEY_PROGRESSION_STEPS = stringPreferencesKey("progressionSteps")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
        val KEY_HAPTIC_SET_RECORDED_227 = booleanPreferencesKey("hapticSetRecorded227")
        val KEY_HEALTH_BACKUP_CONSENT = booleanPreferencesKey("healthBackupConsent")
        val KEY_AUTO_WRITE_HC = booleanPreferencesKey("autoWriteWorkoutToHealthConnect")
        val KEY_ONBOARDING_128 = stringPreferencesKey("onboarding128")
        val KEY_TRAINING_PROFILE = stringPreferencesKey("trainingProfile128")
        val KEY_WEIGHT_RANGE = stringPreferencesKey("weightRange")
        val KEY_CLOUD_DEVICE_ID = stringPreferencesKey("cloudDevice")
        val KEY_PENDING_FEEDBACK_ID = stringPreferencesKey("pendingFeedbackWorkoutId")
        val KEY_PENDING_FEEDBACK_STARTED = stringPreferencesKey("pendingFeedbackStarted")
        val KEY_MIGRATION_V1371_COMPLETE = booleanPreferencesKey("migration_v1_3_71_complete")
        val KEY_GPS_START_CHOICES_216 = stringPreferencesKey("gpsStartChoices216")
        val KEY_TRAINING_MODES_197 = stringPreferencesKey("trainingModes197")

        // Effective main 7473b30 palette: final Basic overrides + original Vivid keys.
        val AVAILABLE_THEMES = listOf(
            ThemePaletteOption("classicBlue", "Kék", "Blue", "#4d8fbe", "#7cb4d6", "basic", "Blau", "Albastru"),
            ThemePaletteOption("classicGreen", "Zöld", "Green", "#4d946a", "#7ab18d", "basic", "Grün", "Verde"),
            ThemePaletteOption("classicRed", "Piros", "Red", "#b95f63", "#d58a8d", "basic", "Rot", "Roșu"),
            ThemePaletteOption("classicOrange", "Narancs", "Orange", "#c07d45", "#d9a06e", "basic", "Orange", "Portocaliu"),
            ThemePaletteOption("classicPurple", "Lila", "Purple", "#8567b1", "#a58cc9", "basic", "Violett", "Mov"),
            ThemePaletteOption("graphite", "Grafit", "Graphite", "#687989", "#96a4b1", "basic", "Graphit", "Grafit"),
            ThemePaletteOption("classicTeal", "Türkiz", "Teal", "#3f918a", "#75b6b1", "basic", "Türkis", "Turcoaz"),
            ThemePaletteOption("classicRose", "Rózsaszín", "Rose", "#b45f82", "#d38aa5", "basic", "Rosé", "Roz"),
            ThemePaletteOption("classicAmber", "Arany", "Amber", "#ae843c", "#cca45f", "basic", "Bernstein", "Chihlimbar"),
            ThemePaletteOption("classicCyan", "Cián", "Cyan", "#4c8fa5", "#77b0c0", "basic", "Cyan", "Cian"),
            ThemePaletteOption("yellow", "Sárga", "Yellow", "#f2bd45", "#ffd975", "vivid", "Gelb", "Galben"),
            ThemePaletteOption("blue", "Kék", "Blue", "#5ca8ff", "#8bc4ff", "vivid", "Blau", "Albastru"),
            ThemePaletteOption("green", "Zöld", "Green", "#62d08a", "#92e6ad", "vivid", "Grün", "Verde"),
            ThemePaletteOption("purple", "Lila", "Purple", "#a98cff", "#c6b5ff", "vivid", "Violett", "Mov"),
            ThemePaletteOption("orange", "Narancs", "Orange", "#ff9b54", "#ffbd8d", "vivid", "Orange", "Portocaliu"),
            ThemePaletteOption("cyan", "Cián", "Cyan", "#48c6ef", "#83dcf6", "vivid", "Cyan", "Cian"),
            ThemePaletteOption("teal", "Türkiz", "Teal", "#45c6b2", "#7de0d1", "vivid", "Türkis", "Turcoaz"),
            ThemePaletteOption("pink", "Rózsaszín", "Pink", "#ff78b9", "#ffa6d0", "vivid", "Rosa", "Roz"),
            ThemePaletteOption("indigo", "Indigó", "Indigo", "#7f96ff", "#a8b8ff", "vivid", "Indigo", "Indigo"),
            ThemePaletteOption("lime", "Lime", "Lime", "#a8d75f", "#c7ea8b", "vivid", "Limette", "Lime"),
            ThemePaletteOption("red", "Piros", "Red", "#ff6b6b", "#ff9b9b", "vivid", "Rot", "Roșu"),
            ThemePaletteOption("magenta", "Magenta", "Magenta", "#e66cff", "#f1a0ff", "vivid", "Magenta", "Magenta"),
            ThemePaletteOption("lavender", "Levendula", "Lavender", "#9a86ff", "#c0b4ff", "vivid", "Lavendel", "Lavandă"),
            ThemePaletteOption("sky", "Égkék", "Sky", "#66c7ff", "#9adfff", "vivid", "Himmelblau", "Azuriu"),
            ThemePaletteOption("mint", "Menta", "Mint", "#63d9b1", "#96ead0", "vivid", "Minze", "Mentă"),
            ThemePaletteOption("coral", "Korall", "Coral", "#ff7f72", "#ffaaa0", "vivid", "Koralle", "Coral"),
            // Preserve the six extra keys already saved by native test versions.
            ThemePaletteOption("amberGold", "Arany (natív)", "Amber Gold (native)", "#f2bd45", "#ffd97d", "basic", "Gold (nativ)", "Auriu (nativ)"),
            ThemePaletteOption("emerald", "Smaragd (natív)", "Emerald (native)", "#3ca87d", "#7fd4ba", "basic", "Smaragd (nativ)", "Smarald (nativ)"),
            ThemePaletteOption("slateCoral", "Korall (natív)", "Coral (native)", "#d46a6a", "#f09c9c", "basic", "Koralle (nativ)", "Coral (nativ)"),
            ThemePaletteOption("vividCyan", "Élénk cián (natív)", "Vivid cyan (native)", "#26b6d4", "#6ee7ff", "vivid", "Leuchtendes Cyan (nativ)", "Cian intens (nativ)"),
            ThemePaletteOption("vividGold", "Élénk arany (natív)", "Vivid gold (native)", "#f3c545", "#fff1a0", "vivid", "Leuchtendes Gold (nativ)", "Auriu intens (nativ)"),
            ThemePaletteOption("vividEmerald", "Élénk smaragd (natív)", "Vivid emerald (native)", "#2dd48f", "#7bf1b8", "vivid", "Leuchtender Smaragd (nativ)", "Smarald intens (nativ)")
        )

        /** Native backup keys take precedence; legacy WebView themeAccent is a KEY, not hex. */
        fun importedTheme(data: JSONObject): ThemePaletteOption? {
            val candidates = listOf(data.optJSONObject("settings")?.optString("themeKey").orEmpty(),
                data.optString("themeKey"), data.optString("themeAccent"))
            return candidates.firstNotNullOfOrNull { key -> AVAILABLE_THEMES.firstOrNull { it.key == key } }
        }

    }

    val settingsFlow: Flow<TrainPilotUserSettings> = context.trainPilotDataStore.data.map { prefs ->
        TrainPilotUserSettings(
            language = prefs[KEY_LANGUAGE] ?: "hu",
            activeProgramId = prefs[KEY_ACTIVE_PROGRAM_ID] ?: "home-basic",
            themeKey = prefs[KEY_THEME_KEY] ?: "classicBlue",
            themeAccent = prefs[KEY_THEME_ACCENT] ?: "#4d8fbe",
            themeAccent2 = prefs[KEY_THEME_ACCENT2] ?: "#7cb4d6",
            themeFamily = prefs[KEY_THEME_FAMILY] ?: "basic",
            restSeconds = (prefs[KEY_REST_SECONDS] ?: 90).coerceIn(0, 3600),
            workSeconds = (prefs[KEY_WORK_SECONDS] ?: 30).coerceIn(1, 3600),
            rounds = (prefs[KEY_ROUNDS] ?: 1).coerceIn(1, 100),
            intervalAutoAdvance = prefs[KEY_INTERVAL_AUTO_ADVANCE] ?: true,
            progressionSteps = runCatching {
                val json = JSONObject(prefs[KEY_PROGRESSION_STEPS] ?: "{}")
                json.keys().asSequence().mapNotNull { key ->
                    val n = json.optDouble(key, Double.NaN)
                    if (n.isFinite() && n in .25..10.0) key to n else null
                }.toMap()
            }.getOrDefault(emptyMap()),
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            hapticSetRecorded227 = prefs[KEY_HAPTIC_SET_RECORDED_227] ?: true,
            healthBackupConsent = prefs[KEY_HEALTH_BACKUP_CONSENT] ?: false,
            autoWriteWorkoutToHealthConnect = prefs[KEY_AUTO_WRITE_HC] ?: false,
            onboarding128 = prefs[KEY_ONBOARDING_128] ?: "complete",
            profileJson = prefs[KEY_TRAINING_PROFILE] ?: "{}",
            weightRange = prefs[KEY_WEIGHT_RANGE] ?: "30",
            cloudDeviceId = prefs[KEY_CLOUD_DEVICE_ID] ?: "",
            pendingFeedbackWorkoutId = prefs[KEY_PENDING_FEEDBACK_ID] ?: "",
            pendingFeedbackStarted = prefs[KEY_PENDING_FEEDBACK_STARTED] ?: "",
            migrationV1371Complete = prefs[KEY_MIGRATION_V1371_COMPLETE] ?: false,
            gpsStartChoices216 = runCatching {
                val json = JSONObject(prefs[KEY_GPS_START_CHOICES_216] ?: "{}")
                json.keys().asSequence().filter { com.repforge.app.NativeOutdoorStart.eligible(it, "distance", "km") }
                    .associateWith { json.opt(it) == true }
            }.getOrDefault(emptyMap()),
            trainingModes197Json = prefs[KEY_TRAINING_MODES_197]
        )
    }

    suspend fun current(): TrainPilotUserSettings = settingsFlow.first()

    suspend fun ensureCloudDeviceId(): String = NativeDataReplacement.mutate(context) {
        val existing = current().cloudDeviceId
        if (existing.matches(Regex("[a-f0-9-]{36}"))) return@mutate existing
        val generated = UUID.randomUUID().toString().lowercase()
        context.trainPilotDataStore.edit { it[KEY_CLOUD_DEVICE_ID] = generated }
        generated
    }

    suspend fun setLanguage(language: String) {
        NativeDataReplacement.mutate(context) {
            if (language !in listOf("hu", "en", "de", "ro")) return@mutate
        context.trainPilotDataStore.edit { it[KEY_LANGUAGE] = language }
        }
    }

    suspend fun saveTrainingProfile(profile: JSONObject) {
        // Do not promote a blank/partial profile into inferred personal details.
        val checked = NativeTrainingProfile.verified(profile)
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_TRAINING_PROFILE] = checked.toString() }
        }
    }

    suspend fun setActiveProgramId(programId: String) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_ACTIVE_PROGRAM_ID] = programId }
        }
    }

    suspend fun setTheme(option: ThemePaletteOption) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_THEME_KEY] = option.key
            prefs[KEY_THEME_ACCENT] = option.accent
            prefs[KEY_THEME_ACCENT2] = option.accent2
            prefs[KEY_THEME_FAMILY] = option.family
        }
        }
    }

    suspend fun setTimerDefaults(workSeconds: Int, restSeconds: Int, rounds: Int, sound: Boolean, vibration: Boolean,
        autoAdvance: Boolean? = null) {
        com.repforge.app.NativeIntervalPreset.normalized(JSONObject().put("work", workSeconds).put("rest", restSeconds)
            .put("rounds", rounds).put("sound", sound).put("vibration", vibration))
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_WORK_SECONDS] = workSeconds.coerceIn(1, 3600)
            prefs[KEY_REST_SECONDS] = restSeconds.coerceIn(0, 3600)
            prefs[KEY_ROUNDS] = rounds.coerceIn(1, 100)
            prefs[KEY_SOUND_ENABLED] = sound
            prefs[KEY_VIBRATION_ENABLED] = vibration
            if (autoAdvance != null) prefs[KEY_INTERVAL_AUTO_ADVANCE] = autoAdvance
        }
        }
    }

    /** Explicit Settings save: stale dialogs cannot overwrite another edit or a restored dataset. */
    internal suspend fun updateTimerDefaults(expected: String, proposed: JSONObject,
        ticket: NativeDataReplacement.Continuation) {
        val config = com.repforge.app.NativeIntervalPreset.normalized(proposed)
        NativeDataReplacement.mutate(context) {
            ticket.requireCurrent(context)
            check(current().timerConfiguration().toString() == expected) { "Timer defaults changed; reopen editor" }
            setTimerDefaults(config.getInt("work"), config.getInt("rest"), config.getInt("rounds"),
                config.getBoolean("sound"), config.getBoolean("vibration"), config.getBoolean("autoAdvance"))
        }
    }

    suspend fun setHapticSetRecorded227(enabled: Boolean) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_HAPTIC_SET_RECORDED_227] = enabled }
        }
    }

    suspend fun setHealthBackupConsent(consented: Boolean) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_HEALTH_BACKUP_CONSENT] = consented }
        }
    }

    suspend fun setAutoWriteWorkoutToHealthConnect(enabled: Boolean) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_AUTO_WRITE_HC] = enabled }
        }
    }

    suspend fun setWeightRange(range: String) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_WEIGHT_RANGE] = range }
        }
    }

    suspend fun setPendingFeedback(workoutId: String?, started: String?) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_PENDING_FEEDBACK_ID] = workoutId.orEmpty()
            prefs[KEY_PENDING_FEEDBACK_STARTED] = started.orEmpty()
        }
        }
    }

    /**
     * Called only after a successful legacy extraction/import, before migration
     * is marked complete. Existing native settings and imported profiles win.
     * Do not infer a new user from the synthetic legacyStorageRaw entry.
     */
    internal suspend fun initializeOnboardingForMigration(legacyEvidence: Boolean) {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { prefs ->
                val existing = prefs[KEY_ONBOARDING_128]
                val status = NativeOnboardingPolicy.initialState(
                    existing, legacyEvidence, prefs[KEY_TRAINING_PROFILE] ?: "{}"
                )
                prefs[KEY_ONBOARDING_128] = status
            }
        }
    }

    /** Compare-and-set under the same replacement lease as restore/erase. */
    internal suspend fun transitionOnboarding(expected: String, next: String,
        ticket: NativeDataReplacement.Continuation): Boolean {
        require(NativeOnboardingPolicy.canTransition(expected, next))
        return NativeDataReplacement.mutate(context) {
            ticket.requireCurrent(context)
            var changed = false
            context.trainPilotDataStore.edit { prefs ->
                if ((prefs[KEY_ONBOARDING_128] ?: "complete") == expected) {
                    prefs[KEY_ONBOARDING_128] = next
                    changed = true
                }
            }
            changed
        }
    }

    /** One atomic profile+completion write; never starts a program or touches a draft. */
    internal suspend fun completeOnboarding(expectedProfileJson: String, fields: Map<String, String>,
        ticket: NativeDataReplacement.Continuation): Boolean {
        val checked = NativeTrainingProfile.edit(JSONObject(expectedProfileJson), fields)
        return NativeDataReplacement.mutate(context) {
            ticket.requireCurrent(context)
            var changed = false
            context.trainPilotDataStore.edit { prefs ->
                if (prefs[KEY_ONBOARDING_128] == NativeOnboardingPolicy.IN_PROGRESS &&
                    (prefs[KEY_TRAINING_PROFILE] ?: "{}") == expectedProfileJson) {
                    prefs[KEY_TRAINING_PROFILE] = checked.toString()
                    prefs[KEY_ONBOARDING_128] = NativeOnboardingPolicy.COMPLETE
                    changed = true
                }
            }
            changed
        }
    }

    suspend fun markMigrationComplete() {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { it[KEY_MIGRATION_V1371_COMPLETE] = true }
        }
    }

    suspend fun setGpsStartChoice(activityId: String, enabled: Boolean) {
        NativeDataReplacement.mutate(context) {
            require(com.repforge.app.NativeOutdoorStart.eligible(activityId, "distance", "km"))
        context.trainPilotDataStore.edit { prefs ->
            val choices = runCatching { JSONObject(prefs[KEY_GPS_START_CHOICES_216] ?: "{}") }.getOrElse { JSONObject() }
            prefs[KEY_GPS_START_CHOICES_216] = choices.put(activityId, enabled).toString()
        }
        }
    }

    /**
     * Persists only an already approved v1 mode-settings change.
     * Raw JSON compare-and-set rejects previews based on obsolete settings; it never
     * modifies a workout, program prescription, draft or journal.
     * The caller must separately enforce explicit confirmation and active-session guards.
     */
    internal suspend fun saveApprovedTrainingModes197(expectedRaw: String?, approved: JSONObject?,
        expectedActiveProgramId: String? = null, restoringUndo: Boolean = false): Boolean {
        if (restoringUndo) {
            // Restoring a previously imported malformed document is permitted solely
            // if it is precisely the undo snapshot of the currently saved version.
            // The resolver still treats it as legacy; no malformed mode can activate.
            val previous = runCatching { expectedRaw?.let(::JSONObject) }
                .getOrNull()?.optJSONObject("undo")?.opt("previous")
            val expectedPrevious = (previous as? JSONObject)?.let {
                JSONObject(it.toString()).apply { remove("undo") }
            }
            require((previous == JSONObject.NULL && approved == null) ||
                (expectedPrevious != null && approved?.toString() == expectedPrevious.toString())) {
                "Undo did not match the last saved mode"
            }
        } else {
            require(approved == null || NativeTrainingModes.validForWrite(approved)) {
                "Invalid approved training modes configuration"
            }
        }
        val replacement = approved?.toString()
        return NativeDataReplacement.mutate(context) {
            // Also protect paused sessions, ownership transfer and native execution
            // that may not be visible to the currently displayed UI.
            val noActiveWorkout = withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                // PhoneWorkoutExecution.store is explicitly main-looper only.
                // Run this short, synchronous admission while holding metadata lease,
                // then leave the DataStore edit on its normal suspending IO path.
                val activeUi = com.repforge.app.ActiveWorkoutStore.current(context)
                    ?.optBoolean("active") == true
                val execution = com.repforge.app.PhoneWorkoutExecution.store(context)
                check(!execution.restoreBlocked() && !execution.commandsSuspended()) {
                    "Native workout execution is temporarily unavailable"
                }
                !activeUi && execution.snapshot()?.optBoolean("active") != true
            }
            check(noActiveWorkout) { "Finish the active workout before changing training modes" }
            var updated = false
            context.trainPilotDataStore.edit { prefs ->
                if (prefs[KEY_TRAINING_MODES_197] == expectedRaw &&
                    (expectedActiveProgramId == null || (prefs[KEY_ACTIVE_PROGRAM_ID] ?: "home-basic") == expectedActiveProgramId)) {
                    if (replacement == null) prefs.remove(KEY_TRAINING_MODES_197)
                    else prefs[KEY_TRAINING_MODES_197] = replacement
                    updated = true
                }
            }
            updated
        }
    }

    /** One atomic portable-settings update, invoked only after the Room import commits. */
    internal suspend fun importPortableSettings(data: JSONObject, replaceTrainingModesOnMissing: Boolean = false) {
        val source = data.optJSONObject("settings") ?: JSONObject()
        val language = source.optString("language", source.optString("lang", data.optString("language")))
        val theme = importedTheme(data)
        val preset = source.optJSONObject("intervalPreset110")?.let {
            com.repforge.app.NativeIntervalPreset.normalized(it, defaultAutoAdvance = false)
        }
        context.trainPilotDataStore.edit { prefs ->
            if (language in listOf("hu", "en", "de", "ro")) prefs[KEY_LANGUAGE] = language
            if (data.optString("activeProgramId").isNotBlank()) prefs[KEY_ACTIVE_PROGRAM_ID] = data.getString("activeProgramId")
            if (theme != null) {
                prefs[KEY_THEME_KEY] = theme.key; prefs[KEY_THEME_ACCENT] = theme.accent
                prefs[KEY_THEME_ACCENT2] = theme.accent2; prefs[KEY_THEME_FAMILY] = theme.family
            }
            if (source.has("restSeconds")) {
                prefs[KEY_REST_SECONDS] = source.optInt("restSeconds", 90).coerceIn(0, 3600)
                prefs[KEY_WORK_SECONDS] = source.optInt("workSeconds", 30).coerceIn(1, 3600)
                prefs[KEY_ROUNDS] = source.optInt("rounds", 1).coerceIn(1, 100)
                prefs[KEY_SOUND_ENABLED] = source.optBoolean("sound", true)
                prefs[KEY_VIBRATION_ENABLED] = source.optBoolean("vibration", true)
            }
            if (preset != null) {
                prefs[KEY_WORK_SECONDS] = preset.getInt("work"); prefs[KEY_REST_SECONDS] = preset.getInt("rest")
                prefs[KEY_ROUNDS] = preset.getInt("rounds"); prefs[KEY_SOUND_ENABLED] = preset.getBoolean("sound")
                prefs[KEY_VIBRATION_ENABLED] = preset.getBoolean("vibration")
                prefs[KEY_INTERVAL_AUTO_ADVANCE] = preset.getBoolean("autoAdvance")
            } else if (source.opt("intervalAutoAdvance") is Boolean) {
                prefs[KEY_INTERVAL_AUTO_ADVANCE] = source.getBoolean("intervalAutoAdvance")
            }
            source.optJSONObject("progressionSteps")?.let { steps ->
                val portable = JSONObject()
                for (key in steps.keys()) {
                    val n = steps.optDouble(key, Double.NaN)
                    if (n.isFinite() && n in .25..10.0) portable.put(key, n)
                }
                prefs[KEY_PROGRESSION_STEPS] = portable.toString()
            }
            if (source.has("trainingModes197")) {
                // A valid JSON object is retained byte-for-byte at the field level
                // (unknown future keys included); the resolver handles malformed v1 safely.
                val mode = source.opt("trainingModes197")
                if (mode is JSONObject) prefs[KEY_TRAINING_MODES_197] = mode.toString()
                else prefs.remove(KEY_TRAINING_MODES_197)
            } else if (replaceTrainingModesOnMissing) {
                // Full restore of a legacy backup must not retain a newer local mode.
                prefs.remove(KEY_TRAINING_MODES_197)
            }
            if (source.has("hapticSetRecorded227")) prefs[KEY_HAPTIC_SET_RECORDED_227] = source.optBoolean("hapticSetRecorded227", true)
            if (source.has("healthBackupConsent")) prefs[KEY_HEALTH_BACKUP_CONSENT] = source.optBoolean("healthBackupConsent")
            if (source.has("autoWriteWorkoutToHealthConnect")) prefs[KEY_AUTO_WRITE_HC] = source.optBoolean("autoWriteWorkoutToHealthConnect")
            if (source.has("weightRange")) prefs[KEY_WEIGHT_RANGE] = source.optString("weightRange", "30")
            if (source.has("onboarding128")) prefs[KEY_ONBOARDING_128] =
                NativeOnboardingPolicy.normalizedImportedState(
                    source.optString("onboarding128", "complete"),
                    data.has("draft") && !data.isNull("draft"))
            else if (replaceTrainingModesOnMissing) prefs[KEY_ONBOARDING_128] = NativeOnboardingPolicy.COMPLETE
            // Main #128 profile is a separate document under portable settings,
            // not Google account identity. An explicit empty object clears it.
            if (source.has("profile")) {
                val profile = source.optJSONObject("profile")
                    ?: throw IllegalArgumentException("Invalid training profile")
                prefs[KEY_TRAINING_PROFILE] = NativeTrainingProfile.verified(profile).toString()
            }
            source.optJSONObject("gpsStartChoices216")?.let { choices ->
                val valid = JSONObject()
                for (key in choices.keys()) if (com.repforge.app.NativeOutdoorStart.eligible(key, "distance", "km") && choices.opt(key) is Boolean)
                    valid.put(key, choices.getBoolean(key))
                prefs[KEY_GPS_START_CHOICES_216] = valid.toString()
            }
            // Device identity and migration/runtime flags are local, never taken from a foreign backup.
        }
    }

    suspend fun resetAllSettingsForEraseLocal() {
        NativeDataReplacement.mutate(context) {
            context.trainPilotDataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_MIGRATION_V1371_COMPLETE] = true
            prefs[KEY_ONBOARDING_128] = NativeOnboardingPolicy.WELCOME
        }
        }
    }
}
