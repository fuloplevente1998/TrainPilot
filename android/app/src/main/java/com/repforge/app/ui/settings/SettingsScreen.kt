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
import androidx.compose.runtime.getValue
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
import com.repforge.app.PrivacyPolicy
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.theme.LocalTrainPilotColors

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val activity = context as? Activity

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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // 1. Téma- és Színpaletta választó (Élő színmintákkal)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Téma & Színpaletta", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TrainPilotSettingsStore.AVAILABLE_THEMES) { themeOpt ->
                            val isSel = state.settings.themeKey == themeOpt.key
                            val cAccent = Color(android.graphics.Color.parseColor(themeOpt.accent))
                            val cAccent2 = Color(android.graphics.Color.parseColor(themeOpt.accent2))
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) colors.card2 else Color.Transparent)
                                    .border(1.dp, if (isSel) colors.accent else colors.line, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setTheme(themeOpt) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row {
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(cAccent))
                                    Spacer(Modifier.width(4.dp))
                                    Box(Modifier.size(16.dp).clip(CircleShape).background(cAccent2))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(themeOpt.nameHu, color = if (isSel) colors.accent else colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

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
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Google Cloud Szinkron (#18)", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))

                    if (gs.connectedEmail.isNotEmpty()) {
                        Text("Csatlakoztatva: ${gs.connectedEmail}", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (gs.lastDriveSyncIso.isNotEmpty()) {
                            Text("Utolsó Drive mentés: ${gs.lastDriveSyncIso.take(19).replace('T', ' ')}", color = colors.muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.recoverEarlierBackupsChecked,
                                onCheckedChange = { viewModel.setRecoverEarlierBackups(it) },
                                colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                            )
                            Text("Korábbi mentések vizsgálata és karbantartása", color = colors.muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { activity?.let { viewModel.syncDriveCloud(it) } },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Drive szinkron", color = Color(0xFF0E1015), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { activity?.let { viewModel.syncCalendarCloud(it) } },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                            ) {
                                Text("Naptár szinkron", color = colors.text, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.disconnectGoogle() }) {
                            Text("Google-fiók lekapcsolása", color = colors.dangerText, fontSize = 11.sp)
                        }
                    } else {
                        Text("Kapcsolódj Google-fiókodhoz a Drive-mentések és az edzésnaptár automatikus szinkronizálásához.", color = colors.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { activity?.let { viewModel.syncDriveCloud(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text("Bejelentkezés Google-fiókkal", color = Color(0xFF0E1015), fontWeight = FontWeight.Bold)
                        }
                    }
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
                    Text("Helyi Mentés & Visszaállítás (SAF)", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Eszközön tárolt biztonsági mentések kezelése fájlválasztóval", color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportJsonLauncher.launch("trainpilot-backup.json") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("JSON Mentés", fontSize = 10.sp)
                        }
                        OutlinedButton(onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("JSON Megnyitás", fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportZipLauncher.launch("TrainPilot.zip") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Text("Teljes ZIP Mentés (Fotókkal)", color = colors.text, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Text("ZIP Visszaállítás", color = colors.text, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 4. Haptika (#227) és Adatvédelmi opciók
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Beállítások & Adatvédelem", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // #227 Haptikus sorozat visszajelzés
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sorozat rögzítési rezgés (#227)", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("40 ms diszkrét haptikus jelzés szett kipipálásakor", color = colors.muted, fontSize = 11.sp)
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
                            Text("Egészségnapló mentése", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Health Connect / pulzus adatok belefoglalása a kézi mentésbe", color = colors.muted, fontSize = 11.sp)
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adatkezelési tájékoztató megtekintése", color = colors.text, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.openEraseLocalDialog() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = colors.dangerText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Minden helyi adat törlése (Erase Local)", color = colors.dangerText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // Minden helyi adat törlése dialógus
    if (state.isEraseLocalDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeEraseLocalDialog() },
            title = { Text("Minden helyi adat törlése", color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text("Biztosan törölsz minden edzést, programot, testsúlyt, fotót és egészségügyi adatot erről a készülékről? A felhőben vagy exportált fájlokban lévő adatok megmaradnak.", color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmEraseLocalData() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Minden adat törlése", color = colors.dangerText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeEraseLocalDialog() }) {
                    Text("Mégse", color = colors.text)
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
            title = { Text("Adatkezelési tájékoztató", color = colors.text, fontWeight = FontWeight.Bold) },
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
                    Text("Értem", color = Color(0xFF0E1015), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card
        )
    }

    // Státusz Toast / Üzenet megerősítés
    state.statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearStatusMessage() },
            title = { Text("Értesítés", color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearStatusMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Rendben", color = Color(0xFF0E1015), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card
        )
    }
}
