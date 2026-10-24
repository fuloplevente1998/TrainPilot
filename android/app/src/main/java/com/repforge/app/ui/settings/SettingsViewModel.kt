package com.repforge.app.ui.settings

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.BackupRestoreManager
import com.repforge.app.GoogleAuthOutcome
import com.repforge.app.GoogleSyncRepository
import com.repforge.app.GoogleSyncUiState
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: TrainPilotUserSettings = TrainPilotUserSettings(),
    val googleSyncState: GoogleSyncUiState = GoogleSyncUiState(),
    val isEraseLocalDialogOpen: Boolean = false,
    val isPrivacyDialogOpen: Boolean = false,
    val recoverEarlierBackupsChecked: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsStore = TrainPilotSettingsStore(context)
    val backupManager = BackupRestoreManager(context)
    val googleRepo = GoogleSyncRepository(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeGoogleSyncState()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsStore.settingsFlow.collect { s ->
                _uiState.value = _uiState.value.copy(settings = s)
            }
        }
    }

    private fun observeGoogleSyncState() {
        viewModelScope.launch {
            googleRepo.uiState.collect { gs ->
                _uiState.value = _uiState.value.copy(googleSyncState = gs)
            }
        }
    }

    fun setTheme(theme: ThemePaletteOption) {
        viewModelScope.launch {
            settingsStore.setTheme(theme)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsStore.setLanguage(lang)
        }
    }

    fun setTimerDefaults(workSec: Int, restSec: Int, rounds: Int, sound: Boolean, vibration: Boolean) {
        viewModelScope.launch {
            settingsStore.setTimerDefaults(workSec, restSec, rounds, sound, vibration)
        }
    }

    fun setHapticSetRecorded227(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setHapticSetRecorded227(enabled)
        }
    }

    fun setHealthBackupConsent(consented: Boolean) {
        viewModelScope.launch {
            settingsStore.setHealthBackupConsent(consented)
        }
    }

    fun setAutoWriteHc(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoWriteWorkoutToHealthConnect(enabled)
        }
    }

    fun setRecoverEarlierBackups(checked: Boolean) {
        _uiState.value = _uiState.value.copy(recoverEarlierBackupsChecked = checked)
    }

    fun syncDriveCloud(activity: Activity) {
        viewModelScope.launch {
            when (val auth = googleRepo.authorizeOperation(activity, "drive")) {
                is GoogleAuthOutcome.Authorized -> {
                    val r = googleRepo.syncDriveCloud(auth.accessToken, recoverEarlier = _uiState.value.recoverEarlierBackupsChecked)
                    _uiState.value = _uiState.value.copy(
                        statusMessage = if (r.isSuccess) "Drive szinkron sikeres!" else "Hiba: ${r.exceptionOrNull()?.message}"
                    )
                }
                is GoogleAuthOutcome.NeedsResolution -> {
                    try { activity.startIntentSenderForResult(auth.pendingIntent.intentSender, 9001, null, 0, 0, 0) } catch (_: Exception) {}
                }
                is GoogleAuthOutcome.Failed -> {
                    _uiState.value = _uiState.value.copy(statusMessage = auth.message)
                }
            }
        }
    }

    fun syncCalendarCloud(activity: Activity) {
        viewModelScope.launch {
            when (val auth = googleRepo.authorizeOperation(activity, "calendar")) {
                is GoogleAuthOutcome.Authorized -> {
                    val r = googleRepo.syncCalendarCloud(auth.accessToken)
                    _uiState.value = _uiState.value.copy(
                        statusMessage = if (r.isSuccess) "Naptár szinkron sikeres (${r.getOrNull()} esemény)!" else "Hiba: ${r.exceptionOrNull()?.message}"
                    )
                }
                is GoogleAuthOutcome.NeedsResolution -> {
                    try { activity.startIntentSenderForResult(auth.pendingIntent.intentSender, 9002, null, 0, 0, 0) } catch (_: Exception) {}
                }
                is GoogleAuthOutcome.Failed -> {
                    _uiState.value = _uiState.value.copy(statusMessage = auth.message)
                }
            }
        }
    }

    fun disconnectGoogle() {
        val r = googleRepo.disconnect()
        _uiState.value = _uiState.value.copy(
            statusMessage = if (r.isSuccess) "Google-fiók lekapcsolva." else r.exceptionOrNull()?.message
        )
    }

    fun onSafJsonExportCompleted(uri: Uri) {
        viewModelScope.launch {
            val r = backupManager.exportJsonBackupToUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "JSON mentés sikeres (${r.getOrNull()?.second} bájt)!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun onSafJsonImportCompleted(uri: Uri) {
        if (!canReplaceData()) return
        viewModelScope.launch {
            val r = backupManager.importJsonBackupFromUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "JSON visszaállítás sikeres!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun onSafZipExportCompleted(uri: Uri) {
        viewModelScope.launch {
            val r = backupManager.exportZipArchiveToUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "ZIP archívum mentve (${(r.getOrNull()?.second ?: 0L) / 1024} KB)!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun onSafZipImportCompleted(uri: Uri) {
        if (!canReplaceData()) return
        viewModelScope.launch {
            val r = backupManager.importZipArchiveFromUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "ZIP archívum és fotók sikeresen visszaállítva!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun openEraseLocalDialog() {
        if (canReplaceData()) _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = true)
    }
    fun closeEraseLocalDialog() { _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = false) }
    fun confirmEraseLocalData() {
        if (!canReplaceData()) return
        viewModelScope.launch {
            val r = backupManager.eraseAllLocalData()
            _uiState.value = _uiState.value.copy(
                isEraseLocalDialogOpen = false,
                statusMessage = if (r.isSuccess) "Minden helyi adat törölve." else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    // Recheck SAF callbacks as well: a watch may start a workout while the picker is open.
    // This UI guard complements (does not replace) restore's transactional validation.
    private fun canReplaceData(): Boolean {
        val active = ActiveWorkoutStore.current(context)?.optBoolean("active") == true ||
            PhoneWorkoutExecution.store(context).snapshot()?.optBoolean("active") == true
        if (!active) return true
        _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = false,
            statusMessage = when (_uiState.value.settings.language) {
                "en" -> "Finish or discard the active workout before restoring or deleting data."
                "de" -> "Beende oder verwirf das aktive Training vor dem Wiederherstellen oder Löschen von Daten."
                "ro" -> "Încheie sau abandonează antrenamentul activ înainte de restaurarea ori ștergerea datelor."
                else -> "Az adatok visszaállítása vagy törlése előtt fejezd be vagy vesd el az aktív edzést."
            })
        return false
    }

    fun openPrivacyDialog() { _uiState.value = _uiState.value.copy(isPrivacyDialogOpen = true) }
    fun closePrivacyDialog() { _uiState.value = _uiState.value.copy(isPrivacyDialogOpen = false) }
    fun clearStatusMessage() { _uiState.value = _uiState.value.copy(statusMessage = null) }
}
