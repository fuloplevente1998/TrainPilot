package com.repforge.app.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.PrivacyPolicy
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.theme.LocalTrainPilotColors

@Composable
private fun StudioSettingsActionCard(
    title: String,
    subtitle: String,
    tag: String,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .testTag(tag)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    color = colors.muted,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
            badge?.let {
                Text(
                    it,
                    color = colors.accentText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(colors.accent.copy(alpha = .15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("›", color = colors.accentText, fontSize = 22.sp)
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    allowDataReplacement: Boolean = true,
    viewModel: SettingsViewModel = viewModel(),
    initialSection: String = "general",
    onConnectedDevices: () -> Unit = {},
    onPrivatePhotos: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.settings.language) { nativeResources(context, state.settings.language) }
    val activity = context as? Activity
    val maintenancePreview by viewModel.maintenancePreview.collectAsStateWithLifecycle()
    NativeGoogleMaintenanceConfirmation(maintenancePreview, state.settings.language, state.maintenanceBusy,
        viewModel::cancelGoogleMaintenance, { activity?.let(viewModel::confirmGoogleMaintenance) })
    val googleConsent by viewModel.googleConsent.collectAsStateWithLifecycle()
    NativeGoogleConsentLauncher(googleConsent, viewModel::takeGoogleConsent,
        viewModel::completeGoogleConsent, viewModel::failGoogleConsent)
    var profileOpen by remember { mutableStateOf(false) }
    var openedProfileJson by remember { mutableStateOf("{}") }
    var openedTimerJson by remember { mutableStateOf<String?>(null) }
    var themeExpanded by rememberSaveable(initialSection) {
        mutableStateOf(initialSection == "theme")
    }
    var backupExpanded by rememberSaveable { mutableStateOf(false) }
    var privacyExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.timerSavedSequence) {
        if (state.timerSavedSequence > 0) openedTimerJson = null
    }
    LaunchedEffect(state.profileSavedSequence) {
        if (state.profileSavedSequence > 0) profileOpen = false
    }

    openedTimerJson?.let { config ->
        NativeTimerDefaultsDialog(config, state.settings.language, state.timerSaving, state.timerWriteFailed,
            onDismiss = { openedTimerJson = null }, onSave = viewModel::saveTimerDefaults)
    }

    // SAF Launcherek (Export/Import)
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.onSafJsonExportCompleted(it) }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onSafJsonImportCompleted(it) }
    }
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.onSafZipExportCompleted(it) }
    }
    val importZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onSafZipImportCompleted(it) }
    }

    if (state.needsRecovery) {
        ReplacementRecoveryPanel(state.settings.language, state.replacementBusy, state.recoveryError,
            viewModel::resumeReplacement, viewModel::onSafJsonImportCompleted, viewModel::onSafZipImportCompleted)
        return
    }

    // The proposal/dialog belongs to the screen, not a disposable lazy-list item.
    val globalModeState = remember(state.settings.trainingModes197Json, state.settings.activeProgramId) {
        NativeTrainingModePickerState(NativeTrainingModes.globalChoice(state.settings.trainingModes197Json))
    }

    // Presentation-only entry points reuse the original settings controls/writers.
    val initialIndex = when (initialSection) {
        "theme" -> 2
        "language" -> 3
        "profile" -> 7
        else -> 0
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex =
            if (initialIndex > 0 && !allowDataReplacement) initialIndex + 1 else initialIndex
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("settings_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        if (!allowDataReplacement) item {
            Text(strings.getString(R.string.native_settings_workout_running), color = colors.muted)
        }

        item {
            val gs = state.googleSyncState
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_google_summary"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (gs.connectedEmail.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings.getString(R.string.studio_settings_google_sync),
                                color = colors.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (gs.connectedEmail.isNotEmpty()) gs.connectedEmail
                                else strings.getString(R.string.native_settings_google_help),
                                color = colors.muted,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                    Button(
                        onClick = {
                            activity?.let {
                                if (gs.connectedEmail.isNotEmpty()) viewModel.syncDriveCloud(it)
                                else viewModel.connectGoogle(it)
                            }
                        },
                        enabled = !gs.isBusy && !state.replacementBusy,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gs.connectedEmail.isNotEmpty()) colors.card2 else colors.accent,
                            contentColor = if (gs.connectedEmail.isNotEmpty()) colors.text else colors.onAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            strings.getString(
                                if (gs.connectedEmail.isNotEmpty()) R.string.native_settings_drive_sync
                                else R.string.native_settings_google_login
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            val paletteIndex = TrainPilotSettingsStore.AVAILABLE_THEMES
                .indexOfFirst { it.key == state.settings.themeKey }
                .takeIf { it >= 0 }?.plus(1)
            Card(
                modifier = Modifier.fillMaxWidth()
                    .testTag("settings_theme_summary")
                    .clickable(enabled = !state.replacementBusy) {
                        themeExpanded = !themeExpanded
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.getString(R.string.studio_settings_themes),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            state.settings.let {
                                TrainPilotSettingsStore.AVAILABLE_THEMES
                                    .firstOrNull { theme -> theme.key == it.themeKey }
                                    ?.displayName(it.language)
                            } ?: state.settings.themeKey,
                            color = colors.muted,
                            fontSize = 11.sp
                        )
                    }
                    paletteIndex?.let {
                        Text(
                            "#$it",
                            color = colors.accentText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(colors.accent.copy(alpha = .15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Text(if (themeExpanded) "⌄" else "›", color = colors.accentText, fontSize = 22.sp)
                }
            }
        }
        if (themeExpanded) {
            item { NativeThemePicker(state.settings, viewModel::setTheme) }
        }
        item { NativeLanguagePicker(state.settings.language, !state.replacementBusy, viewModel::setLanguage) }

        item {
            Text(
                strings.getString(R.string.studio_settings_training_notifications),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .testTag("settings_timer_open")
                    .clickable(enabled = !state.replacementBusy && !state.timerSaving) {
                        openedTimerJson = viewModel.beginTimerDefaults()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.getString(R.string.native_timer_defaults_title),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            strings.getString(R.string.studio_settings_timer_subtitle),
                            color = colors.muted,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        strings.getString(
                            R.string.studio_settings_rest_badge,
                            state.settings.restSeconds
                        ),
                        color = colors.accentText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(colors.accent.copy(alpha = .15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = colors.accentText, fontSize = 22.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.getString(R.string.native_timer_defaults_sound),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            strings.getString(R.string.studio_settings_sound_subtitle),
                            color = colors.muted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.settings.soundEnabled,
                        enabled = !state.replacementBusy,
                        onCheckedChange = viewModel::setSoundEnabled,
                        modifier = Modifier.testTag("settings_sound_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accent
                        )
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .testTag("training_profile_open")
                    .clickable(enabled = !state.profileSaving && !state.replacementBusy) {
                        openedProfileJson = state.settings.profileJson
                        viewModel.clearProfileError()
                        profileOpen = true
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.getString(R.string.studio_settings_profile_progression),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            strings.getString(R.string.native_settings_profile_info),
                            color = colors.muted,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                    Text(
                        strings.getString(R.string.studio_settings_edit_badge),
                        color = colors.accentText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(colors.accent.copy(alpha = .15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("›", color = colors.accentText, fontSize = 22.sp)
                }
            }
        }

        // Keep the complete native progression-mode editor reachable below the compact Studio row.
        item {
            NativeTrainingModePicker(
                language = state.settings.language,
                raw = state.settings.trainingModes197Json,
                activeProgramId = state.settings.activeProgramId,
                scope = "global",
                enabled = allowDataReplacement && !state.modeSaving && !state.replacementBusy,
                onApprove = viewModel::approveTrainingMode,
                onUndo = { viewModel.undoTrainingMode("global", null) },
                holder = globalModeState, showDialog = false
            )
        }

        item {
            Text(
                strings.getString(R.string.studio_settings_devices_data),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            StudioSettingsActionCard(
                title = strings.getString(R.string.studio_settings_connected_devices),
                subtitle = strings.getString(R.string.studio_settings_connected_devices_subtitle),
                tag = "settings_connected_devices",
                badge = strings.getString(R.string.studio_settings_manage_badge),
                onClick = onConnectedDevices
            )
        }
        item {
            StudioSettingsActionCard(
                title = strings.getString(R.string.studio_settings_backup_restore),
                subtitle = strings.getString(R.string.studio_settings_backup_subtitle),
                tag = "settings_backup_summary",
                badge = "JSON",
                enabled = !state.replacementBusy,
                onClick = { backupExpanded = !backupExpanded }
            )
        }
        item {
            StudioSettingsActionCard(
                title = strings.getString(R.string.studio_settings_privacy_permissions),
                subtitle = strings.getString(R.string.studio_settings_privacy_subtitle),
                tag = "settings_privacy_summary",
                badge = strings.getString(R.string.studio_settings_manage_badge),
                onClick = { privacyExpanded = !privacyExpanded }
            )
        }
        item {
            StudioSettingsActionCard(
                title = strings.getString(R.string.studio_settings_private_photos),
                subtitle = strings.getString(R.string.studio_settings_private_photos_subtitle),
                tag = "settings_private_photos",
                onClick = onPrivatePhotos
            )
        }

        // Detailed native controls stay available behind the compact Studio rows.
        if (backupExpanded) {
        // 2. Google Cloud Szinkron (Drive appDataFolder + Managed Naptár)
        item {
            val gs = state.googleSyncState
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.getString(R.string.native_settings_cloud_title), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))

                    if (gs.connectedEmail.isNotEmpty()) {
                        Text(strings.getString(R.string.native_settings_connected, gs.connectedEmail), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (gs.lastDriveSyncIso.isNotEmpty()) {
                            Text(strings.getString(R.string.native_settings_last_drive, gs.lastDriveSyncIso.take(19).replace('T', ' ')), color = colors.muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.recoverEarlierBackupsChecked,
                                onCheckedChange = { viewModel.setRecoverEarlierBackups(it) },
                                colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                            )
                            Text(strings.getString(R.string.native_settings_older_backups), color = colors.muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { activity?.let { viewModel.syncDriveCloud(it) } },
                            enabled = !gs.isBusy && !state.replacementBusy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text(strings.getString(R.string.native_settings_drive_sync), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { activity?.let { viewModel.syncCalendarCloud(it) } },
                            enabled = !gs.isBusy && !state.replacementBusy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                            ) {
                                Text(strings.getString(R.string.native_settings_calendar_sync), color = colors.text, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        NativeGoogleMaintenanceControls(state.settings.language, state.maintenanceBusy || gs.isBusy || state.replacementBusy,
                            viewModel::openGoogleMaintenance)
                        TextButton(onClick = { viewModel.disconnectGoogle() }, enabled = !gs.isBusy && !state.replacementBusy) {
                            Text(strings.getString(R.string.native_settings_disconnect), color = colors.dangerText, fontSize = 11.sp)
                        }
                    } else {
                        Text(strings.getString(R.string.native_settings_google_help), color = colors.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { activity?.let { viewModel.connectGoogle(it) } },
                            enabled = !gs.isBusy && !state.replacementBusy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text(strings.getString(R.string.native_settings_google_login), color = colors.onAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    NativeGoogleMaintenanceFeedback(state.settings.language, state.maintenanceOutcome, state.maintenanceCompleted)
                    if (gs.isBusy) {
                        Spacer(Modifier.height(6.dp))
                        Text(gs.stageMessage, color = colors.accent2, fontSize = 11.sp)
                    }
                }
            }
        }

        // 3. Helyi Mentés és Visszaállítás (SAF JSON és ZIP Fotókkal)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.getString(R.string.native_settings_local_backup), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(strings.getString(R.string.native_settings_backup_help), color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportJsonLauncher.launch("trainpilot-backup.json") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.getString(R.string.native_settings_json_export), fontSize = 10.sp)
                        }
                        OutlinedButton(enabled = allowDataReplacement, onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.getString(R.string.native_settings_json_import), fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportZipLauncher.launch("TrainPilot.zip") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Text(strings.getString(R.string.native_settings_zip_export), color = colors.text, fontSize = 10.sp)
                        }
                        Button(
                            enabled = allowDataReplacement,
                            onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Text(strings.getString(R.string.native_settings_zip_import), color = colors.text, fontSize = 10.sp)
                        }
                    }
                }
            }
        }


        }

        if (privacyExpanded) {
        // 4. Haptika (#227) és Adatvédelmi opciók
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.getString(R.string.native_settings_privacy_section), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // #227 Haptikus sorozat visszajelzés
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.getString(R.string.native_settings_haptic), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(strings.getString(R.string.native_settings_haptic_info), color = colors.muted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.settings.hapticSetRecorded227,
                            onCheckedChange = { viewModel.setHapticSetRecorded227(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card2)
                        )
                    }

                    // Egészségügyi mentési hozzájárulás
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.getString(R.string.native_settings_health_backup), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(strings.getString(R.string.native_settings_health_backup_info), color = colors.muted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.settings.healthBackupConsent,
                            onCheckedChange = { viewModel.setHealthBackupConsent(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card2)
                        )
                    }
                }
            }
        }

        // 5. Adatvédelmi tájékoztató & Minden helyi adat törlése (Veszélyes zóna)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.openPrivacyDialog() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.getString(R.string.native_settings_privacy_open), color = colors.text, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = allowDataReplacement,
                        onClick = { viewModel.openEraseLocalDialog() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = colors.dangerText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.getString(R.string.native_settings_erase_open), color = colors.dangerText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    NativeTrainingModePicker(
        language = state.settings.language,
        raw = state.settings.trainingModes197Json,
        activeProgramId = state.settings.activeProgramId,
        scope = "global",
        enabled = allowDataReplacement && !state.modeSaving && !state.replacementBusy,
        onApprove = viewModel::approveTrainingMode,
        onUndo = { viewModel.undoTrainingMode("global", null) },
        holder = globalModeState, showCard = false
    )

    if (profileOpen) TrainingProfileDialog(
        profileJson = openedProfileJson, language = state.settings.language,
        saving = state.profileSaving, failed = state.profileWriteFailed,
        onSave = { fields -> viewModel.saveProfile(fields, openedProfileJson) },
        onDismiss = {
            if (!state.profileSaving) {
                profileOpen = false
                viewModel.clearProfileError()
            }
        }
    )

    // Minden helyi adat törlése dialógus
    if (state.isEraseLocalDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeEraseLocalDialog() },
            title = { Text(strings.getString(R.string.native_settings_erase_title), color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text(strings.getString(R.string.native_settings_erase_message), color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmEraseLocalData() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text(strings.getString(R.string.native_settings_erase_confirm), color = colors.dangerText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeEraseLocalDialog() }) {
                    Text(strings.getString(R.string.native_settings_cancel), color = colors.text)
                }
            },
            containerColor = colors.card
        )
    }

    // Adatkezelési tájékoztató dialógus (Privacy Policy)
    if (state.isPrivacyDialogOpen) {
        val scroll = rememberScrollState()
        AlertDialog(
            onDismissRequest = { viewModel.closePrivacyDialog() },
            title = { Text(strings.getString(R.string.native_settings_privacy_title), color = colors.text, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp).verticalScroll(scroll)) {
                    Text(PrivacyPolicy.text(state.settings.language), color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.closePrivacyDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(strings.getString(R.string.native_settings_understood), color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card
        )
    }

    // Státusz Toast / Üzenet megerősítés
    state.statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearStatusMessage() },
            title = { Text(strings.getString(R.string.native_settings_notice_title), color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearStatusMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(strings.getString(R.string.native_settings_ok), color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card
        )
    }
}
