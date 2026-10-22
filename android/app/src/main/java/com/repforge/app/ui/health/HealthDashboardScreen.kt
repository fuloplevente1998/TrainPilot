package com.repforge.app.ui.health
import android.app.Activity
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.repforge.app.HealthConnectRepository
import androidx.compose.material3.TextButton

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.ui.theme.LocalTrainPilotColors

@Composable
fun HealthDashboardScreen(
    navController: NavController,
    viewModel: HealthDashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val activity = context as? Activity
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refresh(activity)
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        state.statusMessage?.let { Text(it, color = colors.muted, modifier = Modifier.padding(12.dp)) }
        // Két fül: Egészségnapló vs #111 BLE Eszközkereső
        TabRow(
            selectedTabIndex = if (state.activeTab == "journal") 0 else 1,
            containerColor = colors.card,
            contentColor = colors.text,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(it[if (state.activeTab == "journal") 0 else 1]),
                    color = colors.accent
                )
            }
        ) {
            Tab(
                selected = state.activeTab == "journal",
                onClick = { viewModel.setTab("journal") },
                text = { Text("Egészségnapló (26 metrika)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = state.activeTab == "ble",
                onClick = { viewModel.setTab("ble") },
                text = { Text("BLE Okosóra (#111)", fontWeight = FontWeight.Bold) }
            )
        }

        if (state.activeTab == "journal") {
            HealthJournalTabContent(
                state = state,
                onSelectProvider = { viewModel.setProvider(it, activity) },
                onToggleHcBackground = { viewModel.toggleHcBackground(it) },
                onRefresh = {
                    val permissions = HealthConnectRepository.READ_PERMS.map { "android.permission.health.$it" }.toTypedArray()
                    if (state.provider == "health_connect" && Build.VERSION.SDK_INT >= 34 &&
                        permissions.none { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                        permissionLauncher.launch(permissions)
                    } else viewModel.refresh(activity)
                }
            )
        } else {
            BleDiscoveryTabContent(
                state = state,
                bleRepo = viewModel.bleRepo,
                onToggleBleBackground = { viewModel.toggleBleBackground(it) }
            )
        }
    }
}

@Composable
fun HealthJournalTabContent(
    state: HealthDashboardUiState,
    onSelectProvider: (String) -> Unit,
    onToggleHcBackground: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Szolgáltató választó kártya (Health Connect vs Samsung Health)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Egészségügyi forrás", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Frissítés", tint = colors.accent)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProviderChoiceChip(
                            label = "Health Connect",
                            isSelected = state.provider == "health_connect",
                            onClick = { onSelectProvider("health_connect") },
                            modifier = Modifier.weight(1f)
                        )
                        ProviderChoiceChip(
                            label = "Samsung Health",
                            isSelected = state.provider == "samsung_health",
                            onClick = { onSelectProvider("samsung_health") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A kiválasztott szolgáltató adatai képezik a napi aggregáció alapját (duplikáció és összeadás nélkül).",
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Alvásnapló összefoglaló
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Alvásfázisok & Szakaszok", color = colors.accent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(state.sleepSessionsSummaryText, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 26 egészségügyi metrika listája
        items(state.metrics, key = { it.key }) { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.label, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(m.sourceText, color = colors.muted, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(m.valueText, color = if (m.isAvailable) colors.accent else colors.muted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (m.unit.isNotBlank() && m.isAvailable) {
                            Spacer(Modifier.width(4.dp))
                            Text(m.unit, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }

        // Háttérszinkron kapcsoló kártya
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Connect háttérszinkron", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Periodikus 15 perces háttérfrissítés Android 14+ rendszeren", color = colors.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.hcBackground,
                        onCheckedChange = onToggleHcBackground,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun BleDiscoveryTabContent(
    state: HealthDashboardUiState,
    bleRepo: com.repforge.app.BleDiscoveryRepository,
    onToggleBleBackground: (Boolean) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val ble = state.bleState

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // #111 Keresés vezérlő panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bluetooth LE Keresés", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Állapot: ${ble.state} (${ble.code})", color = colors.muted, fontSize = 11.sp)
                        }
                        if (ble.state == "scanning") {
                            Button(
                                onClick = { bleRepo.stopScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                            ) {
                                Text("Leállítás", color = colors.dangerText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { bleRepo.startScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color(0xFF0E1015))
                                Spacer(Modifier.width(4.dp))
                                Text("Keresés (60 mp)", color = Color(0xFF0E1015), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (ble.state == "scanning") {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ble.scanRemainingSeconds / 60f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = colors.accent,
                            trackColor = colors.card2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${ble.scanRemainingSeconds} mp van hátra", color = colors.muted, fontSize = 10.sp)
                    }
                }
            }
        }

        // Élő BPM & RDFit Teszt panel (ha csatlakoztatva van)
        if (ble.state in listOf("connected", "probing", "monitoring")) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ble.deviceName.ifBlank { "Csatlakoztatott óra" }, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { bleRepo.disconnect() }) {
                                Text("Bontás", color = colors.dangerText, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 0x2A37 Élő Pulzus
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Élő pulzus (0x2A37)", color = colors.muted, fontSize = 10.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (ble.liveHeartRateBpm != null) "${ble.liveHeartRateBpm} bpm" else "—", color = Color(0xFFEF5350), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    if (ble.heartSupported && ble.state != "monitoring") {
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(onClick = { bleRepo.startHeartRate() }) {
                                            Text("Figyelés", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            // RDFit Lépés & Akkumulátor
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("RDFit mérés (0xED)", color = colors.muted, fontSize = 10.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (ble.rdfitProbe.stepsCount != null) "${ble.rdfitProbe.stepsCount} lépés" else if (ble.rdfitProbe.batteryPercent != null) "${ble.rdfitProbe.batteryPercent}%" else "—", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (ble.probeSupported && ble.state != "probing") {
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(onClick = { bleRepo.readRdfitData() }) {
                                            Text("Lekérdezés", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { bleRepo.rememberWatch() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Óra megjegyzése", color = Color(0xFF0E1015), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (ble.rememberedWatchPresent) {
                                Button(
                                    onClick = { bleRepo.forgetWatch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                                ) {
                                    Text("Elfelejtés", color = colors.dangerText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Háttérkapcsolat (5 percenkénti lépéslekérés)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Háttérbeli lépésszinkron (BLE)", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("A kiválasztott RDFit óra lépésszámának 5 percenkénti frissítése", color = colors.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.bleBackground,
                        onCheckedChange = onToggleBleBackground,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card)
                    )
                }
            }
        }

        // Talált eszközök listája (#111 .tp111-ble)
        item {
            Text("Felfedezett eszközök (${ble.devices.size})", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        items(ble.devices, key = { it.id }) { dev ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { bleRepo.connect(dev.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (dev.remembered) colors.accent else colors.line)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dev.name.ifBlank { "Névtelen eszköz" }, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${dev.displayAddress} • RSSI: ${dev.rssi} dBm", color = colors.muted, fontSize = 11.sp)
                        if (dev.remembered) {
                            Text("★ Megjegyzett óra", color = colors.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { bleRepo.connect(dev.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                    ) {
                        Text("Kapcsolódás", color = colors.accent, fontSize = 11.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ProviderChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = LocalTrainPilotColors.current
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.accent else colors.card2)
            .border(1.dp, if (isSelected) colors.accent else colors.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0E1015) else colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
