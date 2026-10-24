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
    val replacementBusy: Boolean = false,
    val needsRecovery: Boolean = false,
    val recoveryError: Boolean = false,
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

    private fun writeSetting(operation: suspend () -> Unit) {
        if (_uiState.value.replacementBusy) return
        viewModelScope.launch {
            try { operation() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage =
                    com.repforge.app.ui.text.nativeResources(context, _uiState.value.settings.language)
                        .getString(com.repforge.app.R.string.metadata_write_failed))
            }
        }
    }

    fun setTheme(theme: ThemePaletteOption) = writeSetting { settingsStore.setTheme(theme) }
    fun setLanguage(lang: String) = writeSetting { settingsStore.setLanguage(lang) }
    fun setTimerDefaults(workSec: Int, restSec: Int, rounds: Int, sound: Boolean, vibration: Boolean) =
        writeSetting { settingsStore.setTimerDefaults(workSec, restSec, rounds, sound, vibration) }
    fun setHapticSetRecorded227(enabled: Boolean) = writeSetting { settingsStore.setHapticSetRecorded227(enabled) }
    fun setHealthBackupConsent(consented: Boolean) = writeSetting { settingsStore.setHealthBackupConsent(consented) }
    fun setAutoWriteHc(enabled: Boolean) = writeSetting { settingsStore.setAutoWriteWorkoutToHealthConnect(enabled) }

    fun setRecoverEarlierBackups(checked: Boolean) {
        _uiState.value = _uiState.value.copy(recoverEarlierBackupsChecked = checked)
    }

    fun syncDriveCloud(activity: Activity) {
        if (_uiState.value.replacementBusy) return
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
        if (_uiState.value.replacementBusy) return
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
        if (_uiState.value.replacementBusy) return
        viewModelScope.launch {
            val r = backupManager.exportJsonBackupToUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "JSON mentés sikeres (${r.getOrNull()?.second} bájt)!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun onSafJsonImportCompleted(uri: Uri) {
        if (!canReplaceData()) return
        runReplacement { backupManager.importJsonBackupFromUri(uri, recoverInterrupted = _uiState.value.needsRecovery) }
    }

    fun onSafZipExportCompleted(uri: Uri) {
        if (_uiState.value.replacementBusy) return
        viewModelScope.launch {
            val r = backupManager.exportZipArchiveToUri(uri)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (r.isSuccess) "ZIP archívum mentve (${(r.getOrNull()?.second ?: 0L) / 1024} KB)!" else "Hiba: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun onSafZipImportCompleted(uri: Uri) {
        if (!canReplaceData()) return
        runReplacement { backupManager.importZipArchiveFromUri(uri, recoverInterrupted = _uiState.value.needsRecovery) }
    }

    fun openEraseLocalDialog() {
        if (canReplaceData()) _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = true)
    }
    fun closeEraseLocalDialog() { _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = false) }
    fun confirmEraseLocalData() {
        if (!canReplaceData()) return
        _uiState.value = _uiState.value.copy(isEraseLocalDialogOpen = false)
        runReplacement { backupManager.eraseAllLocalData() }
    }
    fun resumeReplacement() { runReplacement { backupManager.resumeInterruptedReplacement() } }
    private fun runReplacement(operation: suspend () -> Result<Unit>) {
        if (_uiState.value.replacementBusy) return
        _uiState.value = _uiState.value.copy(replacementBusy = true, recoveryError = false)
        viewModelScope.launch {
            try {
                val result = operation()
                _uiState.value = _uiState.value.copy(needsRecovery = com.repforge.app.NativeDataReplacement.blocked(context) ||
                    com.repforge.app.NativeReplacementJournal(context).exists(), recoveryError = result.isFailure,
                    statusMessage = result.exceptionOrNull()?.message)
            } finally { _uiState.value = _uiState.value.copy(replacementBusy = false,
                needsRecovery = com.repforge.app.NativeDataReplacement.blocked(context) || com.repforge.app.NativeReplacementJournal(context).exists()) }
        }
    }

    // Recheck SAF callbacks as well: a watch may start a workout while the picker is open.
    // This UI guard complements (does not replace) restore's transactional validation.
    private fun canReplaceData(): Boolean {
        if (_uiState.value.replacementBusy) return false
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
