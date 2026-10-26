package com.repforge.app.ui.settings

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.BackupRestoreManager
import com.repforge.app.GoogleSyncAction
import com.repforge.app.NativeGoogleConsentBridge
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.GoogleSyncRepository
import com.repforge.app.GoogleSyncUiState
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeTrainingProfile
import org.json.JSONObject
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.NativeTrainingModeChange
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
    val statusMessage: String? = null,
    val profileSaving: Boolean = false,
    val profileWriteFailed: Boolean = false,
    val profileSavedSequence: Int = 0,
    val modeSaving: Boolean = false,
    val maintenanceBusy: Boolean = false,
    val maintenanceOutcome: String = "",
    val maintenanceCompleted: Int = 0,
    val timerSaving: Boolean = false,
    val timerWriteFailed: Boolean = false,
    val timerSavedSequence: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val settingsStore = TrainPilotSettingsStore(context)
    val backupManager = BackupRestoreManager(context)
    val googleRepo = GoogleSyncRepository(context)

    private val googleConsentBridge = NativeGoogleConsentBridge()
    internal val googleConsent = googleConsentBridge.prompt
    private var googleRequestRunning = false
    private val _maintenancePreview = MutableStateFlow<NativeGoogleMaintenance.Request?>(null)
    internal val maintenancePreview = _maintenancePreview.asStateFlow()

    internal fun openGoogleMaintenance(kind: NativeGoogleMaintenance.Kind) {
        if (googleRequestRunning || _uiState.value.replacementBusy || _uiState.value.maintenanceBusy) return
        _uiState.value = _uiState.value.copy(maintenanceBusy = true, maintenanceOutcome = "")
        viewModelScope.launch {
            try { _maintenancePreview.value = googleRepo.previewMaintenance(kind) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(maintenanceOutcome = "failed") }
            finally { _uiState.value = _uiState.value.copy(maintenanceBusy = false) }
        }
    }
    internal fun cancelGoogleMaintenance() { _maintenancePreview.value = null }
    internal fun confirmGoogleMaintenance(activity: Activity) {
        val request = _maintenancePreview.value ?: return
        if (googleRequestRunning || _uiState.value.replacementBusy || _uiState.value.maintenanceBusy) return
        _maintenancePreview.value = null // One confirmation cannot be replayed by a repeated tap.
        googleRequestRunning = true
        _uiState.value = _uiState.value.copy(maintenanceBusy = true, maintenanceOutcome = "")
        viewModelScope.launch {
            try {
                val action = when (request.kind) {
                    NativeGoogleMaintenance.Kind.REVOKE -> GoogleSyncAction.CONNECT
                    NativeGoogleMaintenance.Kind.DRIVE -> GoogleSyncAction.DRIVE
                    NativeGoogleMaintenance.Kind.CALENDAR -> GoogleSyncAction.CALENDAR
                }
                val result = googleRepo.authorizeAndRun(activity, action, googleConsentBridge::resolve, maintenance = request)
                val report = result.getOrNull()?.maintenance
                _uiState.value = _uiState.value.copy(maintenanceCompleted = report?.completed ?: 0,
                    maintenanceOutcome = when {
                        report?.complete == true -> "done"
                        report != null -> "partial"
                        else -> "failed"
                    })
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(maintenanceOutcome = "failed") }
            finally { googleRequestRunning = false; _uiState.value = _uiState.value.copy(maintenanceBusy = false) }
        }
    }
    private var timerEdit: Pair<String, NativeDataReplacement.Continuation>? = null

    internal fun takeGoogleConsent(id: String) = googleConsentBridge.takeForLaunch(id)
    internal fun completeGoogleConsent(id: String, data: Intent?) { googleConsentBridge.complete(id, data) }
    internal fun failGoogleConsent(id: String) {
        googleConsentBridge.fail(id, java.io.IOException(nativeResources(context, _uiState.value.settings.language)
            .getString(R.string.native_google_launch_failed)))
    }

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

    fun saveProfile(fields: Map<String, String>, openedJson: String) {
        if (_uiState.value.profileSaving || _uiState.value.replacementBusy) return
        _uiState.value = _uiState.value.copy(profileSaving = true, profileWriteFailed = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        check(settingsStore.current().profileJson == openedJson) {
                            "Training profile changed while editing"
                        }
                        val revised = NativeTrainingProfile.edit(JSONObject(openedJson), fields)
                        settingsStore.saveTrainingProfile(revised)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    profileSavedSequence = _uiState.value.profileSavedSequence + 1)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(profileWriteFailed = true)
            } finally { _uiState.value = _uiState.value.copy(profileSaving = false) }
        }
    }

    fun clearProfileError() {
        _uiState.value = _uiState.value.copy(profileWriteFailed = false)
    }

    internal fun approveTrainingMode(preview: NativeTrainingModeChange.Preview) =
        writeModeChange(R.string.native_modes_saved) { NativeTrainingModeChange.approve(settingsStore, preview) }

    fun undoTrainingMode(scope: String, programId: String?) =
        writeModeChange(R.string.native_modes_reverted) { NativeTrainingModeChange.undo(settingsStore, scope, programId) }

    private fun writeModeChange(successLabel: Int, operation: suspend () -> Boolean) {
        if (_uiState.value.modeSaving || _uiState.value.replacementBusy) return
        _uiState.value = _uiState.value.copy(modeSaving = true)
        viewModelScope.launch {
            try {
                val succeeded = withContext(Dispatchers.IO) { operation() }
                _uiState.value = _uiState.value.copy(statusMessage =
                    nativeResources(context, _uiState.value.settings.language).getString(
                        if (succeeded) successLabel else R.string.native_modes_error))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage =
                    nativeResources(context, _uiState.value.settings.language).getString(R.string.native_modes_error))
            } finally { _uiState.value = _uiState.value.copy(modeSaving = false) }
        }
    }

    fun setTheme(theme: ThemePaletteOption) = writeSetting { settingsStore.setTheme(theme) }
    fun setLanguage(lang: String) = writeSetting { settingsStore.setLanguage(lang) }
    fun setTimerDefaults(workSec: Int, restSec: Int, rounds: Int, sound: Boolean, vibration: Boolean) =
        writeSetting { settingsStore.setTimerDefaults(workSec, restSec, rounds, sound, vibration) }
    internal fun beginTimerDefaults(): String? {
        if (_uiState.value.replacementBusy || _uiState.value.timerSaving) return null
        return try {
            val config = _uiState.value.settings.timerConfiguration().toString()
            timerEdit = config to NativeDataReplacement.continuationForCurrentData(context)
            _uiState.value = _uiState.value.copy(timerWriteFailed = false)
            config
        } catch (_: Exception) { null }
    }

    internal fun saveTimerDefaults(config: JSONObject) {
        val opened = timerEdit ?: return
        if (_uiState.value.replacementBusy || _uiState.value.timerSaving) return
        _uiState.value = _uiState.value.copy(timerSaving = true, timerWriteFailed = false)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { settingsStore.updateTimerDefaults(opened.first, config, opened.second) }
                timerEdit = null
                _uiState.value = _uiState.value.copy(timerSavedSequence = _uiState.value.timerSavedSequence + 1)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.value = _uiState.value.copy(timerWriteFailed = true) }
            finally { _uiState.value = _uiState.value.copy(timerSaving = false) }
        }
    }

    fun setHapticSetRecorded227(enabled: Boolean) = writeSetting { settingsStore.setHapticSetRecorded227(enabled) }
    fun setHealthBackupConsent(consented: Boolean) = writeSetting { settingsStore.setHealthBackupConsent(consented) }
    fun setAutoWriteHc(enabled: Boolean) = writeSetting { settingsStore.setAutoWriteWorkoutToHealthConnect(enabled) }

    fun setRecoverEarlierBackups(checked: Boolean) {
        _uiState.value = _uiState.value.copy(recoverEarlierBackupsChecked = checked)
    }

    fun connectGoogle(activity: Activity) = runGoogleAction(activity, GoogleSyncAction.CONNECT)
    fun syncDriveCloud(activity: Activity) = runGoogleAction(activity, GoogleSyncAction.DRIVE)
    fun syncCalendarCloud(activity: Activity) = runGoogleAction(activity, GoogleSyncAction.CALENDAR)

    private fun runGoogleAction(activity: Activity, action: GoogleSyncAction) {
        if (_uiState.value.replacementBusy || googleRequestRunning || _uiState.value.maintenanceBusy) return
        googleRequestRunning = true
        val recoverEarlier = _uiState.value.recoverEarlierBackupsChecked
        viewModelScope.launch {
            try {
                val result = googleRepo.authorizeAndRun(activity, action, googleConsentBridge::resolve, recoverEarlier)
                val strings = nativeResources(context, _uiState.value.settings.language)
                val message = result.fold(onSuccess = { completion ->
                    when (completion.action) {
                        GoogleSyncAction.CONNECT -> strings.getString(R.string.native_google_connected)
                        GoogleSyncAction.DRIVE -> strings.getString(R.string.native_google_drive_done)
                        GoogleSyncAction.CALENDAR -> strings.getString(R.string.native_google_calendar_done, completion.count ?: 0)
                    }
                }, onFailure = { it.message ?: strings.getString(R.string.native_google_failed) })
                _uiState.value = _uiState.value.copy(statusMessage = message)
            } finally { googleRequestRunning = false }
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
