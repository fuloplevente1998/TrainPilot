package com.repforge

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val selectedTheme: String = "DARK", // DARK, AMOLED, MIDNIGHT, EMERALD
    val selectedLanguage: String = "hu", // hu, en, de, ro
    val hapticFeedbackEnabled: Boolean = true, // v1.3.71: Themed haptics checkbox
    val soundAlertsEnabled: Boolean = true,
    val healthConnectEnabled: Boolean = true,
    val defaultRestSeconds: Int = 90
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang) }
    }

    /**
     * v1.3.71: Egységes tematikus haptikus jelzés kapcsoló.
     */
    fun toggleHaptics(enabled: Boolean) {
        _uiState.update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        _uiState.update { it.copy(soundAlertsEnabled = enabled) }
    }

    fun toggleHealthConnect(enabled: Boolean) {
        _uiState.update { it.copy(healthConnectEnabled = enabled) }
    }

    fun setDefaultRestSeconds(seconds: Int) {
        _uiState.update { it.copy(defaultRestSeconds = seconds) }
    }
}
