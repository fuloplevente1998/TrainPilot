package com.repforge.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

private val Context.trainPilotDataStore: DataStore<Preferences> by preferencesDataStore(name = "trainpilot_native_settings")

data class ThemePaletteOption(val key: String, val nameHu: String, val nameEn: String, val accent: String, val accent2: String, val family: String)

data class TrainPilotUserSettings(
    val language: String = "hu", val activeProgramId: String = "home-basic", val themeKey: String = "classicBlue",
    val themeAccent: String = "#4d8fbe", val themeAccent2: String = "#7cb4d6", val themeFamily: String = "basic",
    val restSeconds: Int = 90, val workSeconds: Int = 30, val rounds: Int = 1, val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true, val hapticSetRecorded227: Boolean = true, val healthBackupConsent: Boolean = false,
    val autoWriteWorkoutToHealthConnect: Boolean = false, val onboarding128: String = "complete", val weightRange: String = "30",
    val cloudDeviceId: String = "", val pendingFeedbackWorkoutId: String = "", val pendingFeedbackStarted: String = "",
    val migrationV1371Complete: Boolean = false
) {
    fun themeJson(): JSONObject = JSONObject().put("key", themeKey).put("accent", themeAccent).put("accent2", themeAccent2).put("family", themeFamily)
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
            language = prefs[KEY_LANGUAGE] ?: "hu", activeProgramId = prefs[KEY_ACTIVE_PROGRAM_ID] ?: "home-basic",
            themeKey = prefs[KEY_THEME_KEY] ?: "classicBlue", themeAccent = prefs[KEY_THEME_ACCENT] ?: "#4d8fbe",
            themeAccent2 = prefs[KEY_THEME_ACCENT2] ?: "#7cb4d6", themeFamily = prefs[KEY_THEME_FAMILY] ?: "basic",
            restSeconds = (prefs[KEY_REST_SECONDS] ?: 90).coerceIn(0, 3600), workSeconds = (prefs[KEY_WORK_SECONDS] ?: 30).coerceIn(1, 3600),
            rounds = (prefs[KEY_ROUNDS] ?: 1).coerceIn(1, 100), soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true, hapticSetRecorded227 = prefs[KEY_HAPTIC_SET_RECORDED_227] ?: true,
            healthBackupConsent = prefs[KEY_HEALTH_BACKUP_CONSENT] ?: false, autoWriteWorkoutToHealthConnect = prefs[KEY_AUTO_WRITE_HC] ?: false,
            onboarding128 = prefs[KEY_ONBOARDING_128] ?: "complete", weightRange = prefs[KEY_WEIGHT_RANGE] ?: "30",
            cloudDeviceId = prefs[KEY_CLOUD_DEVICE_ID] ?: "", pendingFeedbackWorkoutId = prefs[KEY_PENDING_FEEDBACK_ID] ?: "",
            pendingFeedbackStarted = prefs[KEY_PENDING_FEEDBACK_STARTED] ?: "", migrationV1371Complete = prefs[KEY_MIGRATION_V1371_COMPLETE] ?: false
        )
    }

    suspend fun current(): TrainPilotUserSettings = settingsFlow.first()
    suspend fun ensureCloudDeviceId(): String {
        val cur = current().cloudDeviceId
        if (cur.matches(Regex("[a-f0-9-]{36}"))) return cur
        val gen = UUID.randomUUID().toString().lowercase()
        context.trainPilotDataStore.edit { it[KEY_CLOUD_DEVICE_ID] = gen }
        return gen
    }
    suspend fun setLanguage(l: String) { if (l in listOf("hu", "en", "de", "ro")) context.trainPilotDataStore.edit { it[KEY_LANGUAGE] = l } }
    suspend fun setActiveProgramId(p: String) { context.trainPilotDataStore.edit { it[KEY_ACTIVE_PROGRAM_ID] = p } }
    suspend fun setTheme(o: ThemePaletteOption) { context.trainPilotDataStore.edit { it[KEY_THEME_KEY] = o.key; it[KEY_THEME_ACCENT] = o.accent; it[KEY_THEME_ACCENT2] = o.accent2; it[KEY_THEME_FAMILY] = o.family } }
    suspend fun setTimerDefaults(w: Int, r: Int, rnd: Int, s: Boolean, v: Boolean) { context.trainPilotDataStore.edit { it[KEY_WORK_SECONDS] = w.coerceIn(1, 3600); it[KEY_REST_SECONDS] = r.coerceIn(0, 3600); it[KEY_ROUNDS] = rnd.coerceIn(1, 100); it[KEY_SOUND_ENABLED] = s; it[KEY_VIBRATION_ENABLED] = v } }
    suspend fun setHapticSetRecorded227(e: Boolean) { context.trainPilotDataStore.edit { it[KEY_HAPTIC_SET_RECORDED_227] = e } }
    suspend fun setHealthBackupConsent(c: Boolean) { context.trainPilotDataStore.edit { it[KEY_HEALTH_BACKUP_CONSENT] = c } }
    suspend fun setAutoWriteWorkoutToHealthConnect(e: Boolean) { context.trainPilotDataStore.edit { it[KEY_AUTO_WRITE_HC] = e } }
    suspend fun setWeightRange(r: String) { context.trainPilotDataStore.edit { it[KEY_WEIGHT_RANGE] = r } }
    suspend fun setPendingFeedback(id: String?, s: String?) { context.trainPilotDataStore.edit { it[KEY_PENDING_FEEDBACK_ID] = id.orEmpty(); it[KEY_PENDING_FEEDBACK_STARTED] = s.orEmpty() } }
    suspend fun markMigrationComplete() { context.trainPilotDataStore.edit { it[KEY_MIGRATION_V1371_COMPLETE] = true } }
    suspend fun resetAllSettingsForEraseLocal() { context.trainPilotDataStore.edit { it.clear(); it[KEY_MIGRATION_V1371_COMPLETE] = true } }
}
