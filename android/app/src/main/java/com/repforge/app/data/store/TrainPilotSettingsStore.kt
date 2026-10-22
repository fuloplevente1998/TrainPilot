package com.repforge.app.data.store

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
    val family: String // "basic" | "vivid"
) {
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
    val hapticSetRecorded227: Boolean = true,
    val healthBackupConsent: Boolean = false,
    val autoWriteWorkoutToHealthConnect: Boolean = false,
    val onboarding128: String = "complete",
    val weightRange: String = "30",
    val cloudDeviceId: String = "",
    val pendingFeedbackWorkoutId: String = "",
    val pendingFeedbackStarted: String = "",
    val migrationV1371Complete: Boolean = false
) {
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
        val KEY_SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibrationEnabled")
        val KEY_HAPTIC_SET_RECORDED_227 = booleanPreferencesKey("hapticSetRecorded227")
        val KEY_HEALTH_BACKUP_CONSENT = booleanPreferencesKey("healthBackupConsent")
        val KEY_AUTO_WRITE_HC = booleanPreferencesKey("autoWriteWorkoutToHealthConnect")
        val KEY_ONBOARDING_128 = stringPreferencesKey("onboarding128")
        val KEY_WEIGHT_RANGE = stringPreferencesKey("weightRange")
        val KEY_CLOUD_DEVICE_ID = stringPreferencesKey("cloudDevice")
        val KEY_PENDING_FEEDBACK_ID = stringPreferencesKey("pendingFeedbackWorkoutId")
        val KEY_PENDING_FEEDBACK_STARTED = stringPreferencesKey("pendingFeedbackStarted")
        val KEY_MIGRATION_V1371_COMPLETE = booleanPreferencesKey("migration_v1_3_71_complete")

        val AVAILABLE_THEMES = listOf(
            ThemePaletteOption("classicBlue", "Klasszikus kék", "Classic Blue", "#4d8fbe", "#7cb4d6", "basic"),
            ThemePaletteOption("amberGold", "Arany", "Amber Gold", "#f2bd45", "#ffd97d", "basic"),
            ThemePaletteOption("emerald", "Smaragd", "Emerald", "#3ca87d", "#7fd4ba", "basic"),
            ThemePaletteOption("slateCoral", "Korall", "Slate Coral", "#d46a6a", "#f09c9c", "basic"),
            ThemePaletteOption("vividCyan", "Élénk cián", "Vivid Cyan", "#26b6d4", "#6ee7ff", "vivid"),
            ThemePaletteOption("vividGold", "Élénk arany", "Vivid Gold", "#f3c545", "#fff1a0", "vivid"),
            ThemePaletteOption("vividEmerald", "Élénk smaragd", "Vivid Emerald", "#2dd48f", "#7bf1b8", "vivid")
        )
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
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            hapticSetRecorded227 = prefs[KEY_HAPTIC_SET_RECORDED_227] ?: true,
            healthBackupConsent = prefs[KEY_HEALTH_BACKUP_CONSENT] ?: false,
            autoWriteWorkoutToHealthConnect = prefs[KEY_AUTO_WRITE_HC] ?: false,
            onboarding128 = prefs[KEY_ONBOARDING_128] ?: "complete",
            weightRange = prefs[KEY_WEIGHT_RANGE] ?: "30",
            cloudDeviceId = prefs[KEY_CLOUD_DEVICE_ID] ?: "",
            pendingFeedbackWorkoutId = prefs[KEY_PENDING_FEEDBACK_ID] ?: "",
            pendingFeedbackStarted = prefs[KEY_PENDING_FEEDBACK_STARTED] ?: "",
            migrationV1371Complete = prefs[KEY_MIGRATION_V1371_COMPLETE] ?: false
        )
    }

    suspend fun current(): TrainPilotUserSettings = settingsFlow.first()

    suspend fun ensureCloudDeviceId(): String {
        val existing = current().cloudDeviceId
        if (existing.matches(Regex("[a-f0-9-]{36}"))) return existing
        val generated = UUID.randomUUID().toString().lowercase()
        context.trainPilotDataStore.edit { it[KEY_CLOUD_DEVICE_ID] = generated }
        return generated
    }

    suspend fun setLanguage(language: String) {
        if (language !in listOf("hu", "en", "de", "ro")) return
        context.trainPilotDataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setActiveProgramId(programId: String) {
        context.trainPilotDataStore.edit { it[KEY_ACTIVE_PROGRAM_ID] = programId }
    }

    suspend fun setTheme(option: ThemePaletteOption) {
        context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_THEME_KEY] = option.key
            prefs[KEY_THEME_ACCENT] = option.accent
            prefs[KEY_THEME_ACCENT2] = option.accent2
            prefs[KEY_THEME_FAMILY] = option.family
        }
    }

    suspend fun setTimerDefaults(workSeconds: Int, restSeconds: Int, rounds: Int, sound: Boolean, vibration: Boolean) {
        context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_WORK_SECONDS] = workSeconds.coerceIn(1, 3600)
            prefs[KEY_REST_SECONDS] = restSeconds.coerceIn(0, 3600)
            prefs[KEY_ROUNDS] = rounds.coerceIn(1, 100)
            prefs[KEY_SOUND_ENABLED] = sound
            prefs[KEY_VIBRATION_ENABLED] = vibration
        }
    }

    suspend fun setHapticSetRecorded227(enabled: Boolean) {
        context.trainPilotDataStore.edit { it[KEY_HAPTIC_SET_RECORDED_227] = enabled }
    }

    suspend fun setHealthBackupConsent(consented: Boolean) {
        context.trainPilotDataStore.edit { it[KEY_HEALTH_BACKUP_CONSENT] = consented }
    }

    suspend fun setAutoWriteWorkoutToHealthConnect(enabled: Boolean) {
        context.trainPilotDataStore.edit { it[KEY_AUTO_WRITE_HC] = enabled }
    }

    suspend fun setWeightRange(range: String) {
        context.trainPilotDataStore.edit { it[KEY_WEIGHT_RANGE] = range }
    }

    suspend fun setPendingFeedback(workoutId: String?, started: String?) {
        context.trainPilotDataStore.edit { prefs ->
            prefs[KEY_PENDING_FEEDBACK_ID] = workoutId.orEmpty()
            prefs[KEY_PENDING_FEEDBACK_STARTED] = started.orEmpty()
        }
    }

    suspend fun markMigrationComplete() {
        context.trainPilotDataStore.edit { it[KEY_MIGRATION_V1371_COMPLETE] = true }
    }

    suspend fun resetAllSettingsForEraseLocal() {
        context.trainPilotDataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_MIGRATION_V1371_COMPLETE] = true
        }
    }
}
