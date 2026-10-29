package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.MockupWatchGraphicBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 5. EGÉSZSÉG (Mockup 3/4 #5 + teljes régi main Egészség elrendezés)
 */
data class WeightMeasurementEntry(
    val id: String,
    val weightLabel: String,
    val weightValue: Float,
    val dateLabel: String
)

/**
 * 5. EGÉSZSÉG (A csatolt 3 main Egészség képernyőkép pontos, lenyitható felépítése:
 * Mai állapot + Pulzustrend + Testsúlynapló + További Health-adatok + Egészségnapló + Health Connect)
 */
@Composable
fun HealthTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onToggleHealthSection: (String) -> Unit,
    onSelectWeightRange: (Int) -> Unit,
    onToggleAddWeightDialog: (Boolean) -> Unit,
    onSaveNewWeight: (Float) -> Unit,
    onTogglePrimaryHealthSource: () -> Unit,
    onTriggerHealthSync: () -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    onOpenPalettePicker: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current

    var recentMeasurements by remember {
        mutableStateOf(
            listOf(
                WeightMeasurementEntry("w1", "64,5 kg", 64.5f, "2026. 10. 10."),
                WeightMeasurementEntry("w2", "66,5 kg", 66.5f, "2026. 09. 15."),
                WeightMeasurementEntry("w3", "66,7 kg", 66.7f, "2026. 09. 13.")
            )
        )
    }
    var editingMeasurement by remember { mutableStateOf<WeightMeasurementEntry?>(null) }

    if (uiState.showAddWeightDialog || editingMeasurement != null) {
        val initialVal = editingMeasurement?.weightValue?.toString() ?: uiState.currentBodyWeightKg.toString()
        var weightText by remember(editingMeasurement, uiState.showAddWeightDialog) { mutableStateOf(initialVal) }
        var dateText by remember(editingMeasurement, uiState.showAddWeightDialog) {
            mutableStateOf(editingMeasurement?.dateLabel ?: "2026. 10. 27.")
        }
        AlertDialog(
            onDismissRequest = {
                editingMeasurement = null
                onToggleAddWeightDialog(false)
            },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = if (editingMeasurement != null) "Mérés szerkesztése (DEMO)" else "Új testsúlymérés (DEMO)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Testsúly (kg) és dátum:")
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        singleLine = true,
                        label = { Text("Testsúly (kg)") }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        singleLine = true,
                        label = { Text("Dátum") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = weightText.replace(',', '.').toFloatOrNull() ?: uiState.currentBodyWeightKg
                        val formattedKg = if (parsed % 1f == 0f) "${parsed.toInt()} kg" else "${"%.1f".format(parsed).replace('.', ',')} kg"
                        if (editingMeasurement != null) {
                            val targetId = editingMeasurement!!.id
                            recentMeasurements = recentMeasurements.map {
                                if (it.id == targetId) it.copy(weightLabel = formattedKg, weightValue = parsed, dateLabel = dateText) else it
                            }
                            editingMeasurement = null
                        } else {
                            recentMeasurements = listOf(
                                WeightMeasurementEntry(
                                    id = "w_${recentMeasurements.size + 1}",
                                    weightLabel = formattedKg,
                                    weightValue = parsed,
                                    dateLabel = dateText
                                )
                            ) + recentMeasurements
                        }
                        onSaveNewWeight(parsed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(strings.saveAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        editingMeasurement = null
                        onToggleAddWeightDialog(false)
                    }
                ) {
                    Text(strings.cancelAction, color = palette.textPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("health_tab_screen"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. OUTER GOLD-BORDERED CONTAINER: "Mai állapot 2026. okt. 27." (Screenshot 1, 2, 3)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header row: Heart icon box + "Mai állapot 2026. okt. 27." + "Az egészséged rövid áttekintése"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.primary.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Mai állapot",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "2026. okt. 27.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Az egészséged rövid áttekintése",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    // Row 1 of 3 tiles: Alvás (6,2 h) | Aktív kalória (292 kcal) | Pulzus (65 bpm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainTodayStatusTile(
                            icon = Icons.Default.Bedtime,
                            title = "Alvás",
                            value = "6,2 h",
                            onClick = { onToggleHealthSection("journal") },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Aktív kalória",
                            value = "292 kcal",
                            onClick = { onToggleHealthSection("extra") },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.Favorite,
                            title = "Pulzus",
                            value = "65 bpm",
                            onClick = { onToggleHealthSection("pulse") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2 of 3 tiles: Lépések (6734) | Mai edzés (0) | Regeneráció (84/100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainTodayStatusTile(
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            title = "Lépések",
                            value = "6734",
                            onClick = { onToggleHealthSection("journal") },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.FitnessCenter,
                            title = "Mai edzés",
                            value = "0",
                            onClick = { onNavigateToSubScreen(SubScreen.LOG_WORKOUT_DETAIL) },
                            modifier = Modifier.weight(1f)
                        )
                        MainTodayStatusTile(
                            icon = Icons.Default.Eco,
                            title = "Regeneráció",
                            value = "84/100",
                            onClick = { onNavigateToSubScreen(SubScreen.COACH_DETAIL) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Expandable Inner Card 1: Pulzustrend (Screenshot 1 collapsed & Screenshot 2 expanded)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.background.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (uiState.expandedHealthPulseTrend) palette.primary.copy(alpha = 0.55f) else palette.cardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleHealthSection("pulse") }
                                    .testTag("health_pulse_trend_header"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.primary.copy(alpha = 0.16f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MonitorHeart,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pulzustrend",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                    Text(
                                        text = "Elmúlt 7 naptári nap • napi átlagpulzus",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = palette.textSecondary,
                                        maxLines = 2
                                    )
                                }
                                // Mini gold 7-point sparkline curve
                                val sparkColor = palette.primary
                                Canvas(
                                    modifier = Modifier
                                        .width(86.dp)
                                        .height(34.dp)
                                ) {
                                    val values = listOf(66f, 60f, 72f, 69f, 63f, 61f, 65f)
                                    val minV = 58f
                                    val maxV = 74f
                                    val pts = values.mapIndexed { idx, v ->
                                        val x = (idx.toFloat() / (values.size - 1)) * (size.width - 10.dp.toPx()) + 5.dp.toPx()
                                        val norm = (v - minV) / (maxV - minV)
                                        val y = size.height - 5.dp.toPx() - norm * (size.height - 10.dp.toPx())
                                        Offset(x, y)
                                    }
                                    val path = Path()
                                    pts.forEachIndexed { i, pt ->
                                        if (i == 0) {
                                            path.moveTo(pt.x, pt.y)
                                        } else {
                                            val prev = pts[i - 1]
                                            val cx = (prev.x + pt.x) / 2f
                                            path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                        }
                                    }
                                    drawPath(path, color = sparkColor, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
                                    pts.forEach { pt ->
                                        drawCircle(color = sparkColor, radius = 2.6.dp.toPx(), center = pt)
                                    }
                                }
                                Icon(
                                    imageVector = if (uiState.expandedHealthPulseTrend) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.primary
                                )
                            }

                            if (uiState.expandedHealthPulseTrend) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Sub-card 1: Mai pulzus (2x2 grid + Samsung Health · 382 pulzusminta)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Mai pulzus",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = "Átlagpulzus",
                                                value = "65 bpm",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "Nyugalmi pulzus",
                                                value = "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = "Max. pulzus",
                                                value = "98 bpm",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "Min. pulzus",
                                                value = "50 bpm",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = "Samsung Health · 382 pulzusminta",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Sub-card 2: 7 napos trend (7/7 + smooth curve + Sze 66 .. K 65)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "7 napos trend",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = "7/7",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                        }
                                        Text(
                                            text = "Elmúlt 7 naptári nap • napi átlagpulzus",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        val chartColor = palette.primary
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(115.dp)
                                                .padding(vertical = 6.dp)
                                        ) {
                                            val values = listOf(66f, 60f, 72f, 69f, 63f, 61f, 65f)
                                            val minV = 58f
                                            val maxV = 74f
                                            val pts = values.mapIndexed { idx, v ->
                                                val x = (idx.toFloat() / (values.size - 1)) * (size.width - 24.dp.toPx()) + 12.dp.toPx()
                                                val norm = (v - minV) / (maxV - minV)
                                                val y = size.height - 10.dp.toPx() - norm * (size.height - 20.dp.toPx())
                                                Offset(x, y)
                                            }
                                            val path = Path()
                                            pts.forEachIndexed { i, pt ->
                                                if (i == 0) {
                                                    path.moveTo(pt.x, pt.y)
                                                } else {
                                                    val prev = pts[i - 1]
                                                    val cx = (prev.x + pt.x) / 2f
                                                    path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = chartColor,
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                            )
                                            pts.forEach { pt ->
                                                drawCircle(color = chartColor, radius = 4.2.dp.toPx(), center = pt)
                                            }
                                        }

                                        val dayItems = listOf(
                                            "Sze" to "66",
                                            "Cs" to "60",
                                            "P" to "72",
                                            "Szo" to "69",
                                            "V" to "63",
                                            "H" to "61",
                                            "K" to "65"
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            dayItems.forEach { (dayLabel, bpmLabel) ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = palette.background.copy(alpha = 0.5f),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = dayLabel,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = palette.textSecondary
                                                        )
                                                        Text(
                                                            text = bpmLabel,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Adatforrás: Samsung Health",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Bottom link inside Pulzustrend: Regenerációs előzmények >
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Regenerációs előzmények",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = palette.primary
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Inner Card 2: Testsúlynapló (Screenshot 1 collapsed & Screenshot 3 expanded)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.background.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (uiState.expandedHealthWeightLog) palette.primary.copy(alpha = 0.55f) else palette.cardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleHealthSection("weight") }
                                    .testTag("health_weight_log_header"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonitorWeight,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Testsúlynapló",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                    Text(
                                        text = "${uiState.currentBodyWeightKg.toInt()} kg · Mérés, változás és trend e...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "•  •  •",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Icon(
                                    imageVector = if (uiState.expandedHealthWeightLog) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.primary
                                )
                            }

                            if (uiState.expandedHealthWeightLog) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Sub-card: Test és fittség (Testzsír, SpO2, VO2max)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Test és fittség",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                            Text(
                                                text = "Testzsír, SpO₂, VO₂max",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }

                                        // Full-width Testsúly inner box
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = palette.background.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = "Testsúly",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
                                                )
                                                Text(
                                                    text = "${uiState.currentBodyWeightKg.toInt()} kg",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = palette.textPrimary
                                                )
                                                Text(
                                                    text = "Samsung Health · 2026. okt. 21.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }

                                        // 3-column row: Testzsír (18,3 %) | SpO2 (97,0 %) | VO2max (38,6)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = "Testzsír",
                                                value = "18,3 %",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "SpO₂",
                                                value = "97,0 %",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = "VO₂max",
                                                value = "38,6",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2-column row: Változás (-1 kg) | Minimum / Maximum (63 / 66,7 kg)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PulseInnerBox(
                                        label = "Változás",
                                        value = "-1 kg",
                                        modifier = Modifier.weight(1f)
                                    )
                                    PulseInnerBox(
                                        label = "Minimum / Maximum",
                                        value = "63 / 66,7 kg",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Full-width "+ Új mérés >" card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.surfaceCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable { onToggleAddWeightDialog(true) }
                                        .testTag("health_add_weight_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "+ Új mérés",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = palette.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Period filter chips: 7d | 30d | 3m | 1y | All
                                val wRanges = listOf("7d", "30d", "3m", "1y", "All")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    wRanges.forEachIndexed { idx, r ->
                                        val sel = uiState.selectedWeightRangeIndex == idx
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (sel) palette.primary.copy(alpha = 0.25f) else palette.surfaceCard,
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = if (sel) palette.primary else palette.cardBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSelectWeightRange(idx) }
                                        ) {
                                            Text(
                                                text = r,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sel) palette.primary else palette.textPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                // Chart / empty state area matching Screenshot 3
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.selectedWeightRangeIndex <= 1) {
                                        Text(
                                            text = "Nincs elég friss adat",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary
                                        )
                                    } else {
                                        val lineColor = palette.primary
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            val pts = listOf(66.7f, 66.5f, 64.5f, 65.0f)
                                            val minW = 63f
                                            val maxW = 67.5f
                                            val offsets = pts.mapIndexed { idx, w ->
                                                val x = (idx.toFloat() / (pts.size - 1)) * size.width
                                                val y = size.height - ((w - minW) / (maxW - minW)) * size.height
                                                Offset(x, y)
                                            }
                                            val path = Path()
                                            offsets.forEachIndexed { i, pt ->
                                                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                                            }
                                            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                                            offsets.forEach { pt ->
                                                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Legutóbbi mérések",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recentMeasurements.forEach { entry ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = palette.surfaceCard,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = entry.weightLabel,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = entry.dateLabel,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    // Szerkesztés button (gold-bordered pill)
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = palette.primary.copy(alpha = 0.12f),
                                                        modifier = Modifier
                                                            .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable { editingMeasurement = entry }
                                                    ) {
                                                        Text(
                                                            text = "Szerkesztés",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.textPrimary,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                    // Törlés button (red/rose-bordered pill)
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                                        modifier = Modifier
                                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                recentMeasurements = recentMeasurements.filterNot { it.id == entry.id }
                                                            }
                                                    ) {
                                                        Text(
                                                            text = "Törlés",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.textPrimary,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Expandable Section: További Health-adatok (Screenshot 1)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Restore,
                title = "További Health-adatok",
                subtitle = "Vérnyomás, vércukor, légzésszám, távolság",
                expanded = uiState.expandedHealthExtraData,
                onToggle = { onToggleHealthSection("extra") }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Vérnyomás", "118/76 Hgmm", Modifier.weight(1f))
                    SummarySmallPill("Vércukor", "5,1 mmol/L", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Légzésszám", "14,2 /perc", Modifier.weight(1f))
                    SummarySmallPill("Távolság (ma)", "5,24 km", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummarySmallPill("Véroxigén (SpO₂)", "97,0 %", Modifier.weight(1f))
                    SummarySmallPill("Testhőmérséklet", "36,5 °C", Modifier.weight(1f))
                }
            }
        }

        // 3. Expandable Section: Egészségnapló (Screenshot 1)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Favorite,
                title = "Egészségnapló",
                subtitle = null,
                expanded = uiState.expandedHealthJournal,
                onToggle = { onToggleHealthSection("journal") }
            ) {
                val journalDays = listOf(
                    "2026. okt. 27." to "Alvás: 6,2 h • Pulzus: 65 bpm • Lépések: 6 734 • 292 kcal",
                    "2026. okt. 26." to "Alvás: 9,4 h • Pulzus: 61 bpm • Lépések: 6 036 • 235 kcal",
                    "2026. okt. 25." to "Alvás: 4,2 h • Pulzus: 63 bpm • Lépések: 9 919 • 392 kcal"
                )
                journalDays.forEach { (d, summary) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(d, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(summary, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        // 4. Expandable Section: Health Connect (Screenshot 1 + custom Health Connect badge)
        item {
            ExpandableHealthSectionCard(
                icon = Icons.Default.Link,
                title = "Health Connect",
                subtitle = "Adatforrások, engedélyek és szinkron",
                expanded = uiState.expandedHealthConnect,
                onToggle = { onToggleHealthSection("connect") },
                customLeadingBadge = {
                    MockupHealthConnectBadge(size = 44.dp)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onTogglePrimaryHealthSource() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Samsung Health", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("● Közvetlen (1/2)", style = MaterialTheme.typography.labelSmall, color = palette.success)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (!uiState.primaryHealthSourceSamsung) 1.5.dp else 1.dp,
                                color = if (!uiState.primaryHealthSourceSamsung) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onTogglePrimaryHealthSource() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Health Connect", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("● 10/17 engedély", style = MaterialTheme.typography.labelSmall, color = palette.success)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Olvasott adatok: Lépések, Pulzus, Alvás, Aktív kalória, Testsúly, Testzsír, SpO₂, VO₂max, Edzések.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // 4b. Direct Óra és szinkron (Galaxy Watch7) card on Egészség tab so it's 1-tap reachable
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC) }
                    .testTag("health_open_watch_sync_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupWatchGraphicBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Óra és szinkron (${uiState.watchName})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "● Csatlakoztatva • ${uiState.watchBatteryPercent}% • Edzés átadása órának",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
            }
        }

        // 5. Bottom Action Row: Szinkronizálás & Engedélyek + Sync Status Footer (Screenshot 1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.primary.copy(alpha = 0.42f),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTriggerHealthSync() }
                            .testTag("health_sync_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Szinkronizálás",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) }
                            .testTag("health_open_permissions_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Engedélyek",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Utolsó sikeres szinkron: 2026. 10. 27. 14:59",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "Samsung Health · közvetlen: 1/2",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun MainTodayStatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PulseInnerBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.background.copy(alpha = 0.45f),
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpandableHealthSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    customLeadingBadge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (expanded) palette.primary.copy(alpha = 0.6f) else palette.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customLeadingBadge != null) {
                    customLeadingBadge()
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = palette.cardBorder)
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SummarySmallPill(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}

/**
 * 6. COACH (Mockup 3/4 #6 + main TrainPilot Coach)
 */
@Composable
fun CoachDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSuggestedWorkout: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("coach_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "TrainPilot Coach",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Mai felkészültség (54 / 100 circular gauge + Samsung energia 87/100 + TrainPilot becslés 61/100)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mai felkészültség ⓘ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val score = uiState.coachReadinessScore
                            val ringCol = palette.primary
                            val trackCol = palette.surfaceElevated
                            Canvas(modifier = Modifier.size(98.dp)) {
                                drawArc(
                                    color = trackCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringCol,
                                    startAngle = 135f,
                                    sweepAngle = 270f * (score / 100f),
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${uiState.coachReadinessScore}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "/100",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "A legutóbbi terhelés korlátozza a javaslatot. Kezdj 1 sorozattal kevesebbel vagy hagyd 2–3 RIR tartalékon a sorozatokat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Samsung energia", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text("87 / 100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Jó állapotban", style = MaterialTheme.typography.labelSmall, color = palette.success)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("TrainPilot becslés", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text("61 / 100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Mérsékelt", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                            }
                        }
                    }
                }
            }
        }

        // Kulcsjelek Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kulcsjelek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("Részletek", style = MaterialTheme.typography.labelMedium, color = palette.secondary)
                    }
                    CoachSignalRow("Alvás", uiState.sleepDurationLabel, palette.success)
                    CoachSignalRow("HRV", "42 ms", palette.success)
                    CoachSignalRow("Legutóbbi terhelés", "Magas (1 397 kg)", palette.danger)
                }
            }
        }

        // Javasolt edzés Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Javasolt edzés (MAI JAVASLAT)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = palette.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ma • Teljes testes edzés B", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("Közepes intenzitás • ~ 45 perc", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }

                    CoachExerciseAdviceRow("Guggolás", "Normál terhelés", palette.success) {
                        onOpenExerciseDetail("goblet_squat")
                    }
                    CoachExerciseAdviceRow("Fekvenyomás", "Kissé csökkentett terhelés", palette.primary) {
                        onOpenExerciseDetail("bench_press")
                    }
                    CoachExerciseAdviceRow("Evezés gumiszalaggal", "Könnyebb kezdés", palette.primary) {
                        onOpenExerciseDetail("band_row")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onStartSuggestedWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("coach_start_workout_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edzés indítása", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachSignalRow(label: String, value: String, dotColor: Color) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun CoachExerciseAdviceRow(
    name: String,
    advice: String,
    dotColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(advice, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
            }
        }
    }
}

/**
 * 4/4 #1 BEÁLLÍTÁSOK (Mockup 4/4 #1 + main Beállítások)
 */
@Composable
fun SettingsMainScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onCycleRestTimer: () -> Unit,
    onToggleSoundAlert: (Boolean) -> Unit,
    onToggleProgressionModal: (Boolean) -> Unit,
    onSaveProgression: (String, String, Boolean) -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val currentPaletteObj = TrainPilotPalettes.getById(uiState.selectedPaletteId)

    if (uiState.showProgressionModal) {
        var selectedStyle by remember { mutableStateOf(uiState.trainingStyleHu) }
        var selectedRule by remember { mutableStateOf(uiState.progressionRuleHu) }
        var beginnerGentle by remember { mutableStateOf(uiState.beginnerGentleProfileActive) }
        AlertDialog(
            onDismissRequest = { onToggleProgressionModal(false) },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Edzésprofil és progresszió", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edzésstílus:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    listOf("Normál hipertrófia", "Erő (5×5)", "Állóképesség / Köredzés").forEach { st ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedStyle == st) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStyle = st }
                        ) {
                            Text(
                                text = st,
                                color = if (selectedStyle == st) palette.onPrimary else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Text("Progressziós elv:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    listOf("Dupla progresszió", "Lineáris súlyemelés", "RIR alapú autoreguláció").forEach { rl ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedRule == rl) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRule = rl }
                        ) {
                            Text(
                                text = rl,
                                color = if (selectedRule == rl) palette.onPrimary else palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kezdő kímélő profil", color = palette.textPrimary)
                        Switch(checked = beginnerGentle, onCheckedChange = { beginnerGentle = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSaveProgression(selectedStyle, selectedRule, beginnerGentle) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(strings.saveAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onToggleProgressionModal(false) }) {
                    Text(strings.cancelAction, color = palette.textPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("settings_main_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.settingsTitle,
                onBack = onBack,
                onOpenSettings = {}
            )
        }

        // Profile & Google Sync Card (from Mockup 4/4 #1 + main)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fülöp Levente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("paszia1998@gmail.com • Google-szinkron aktív", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onShowBanner("DEMO • Google Drive és Naptár szinkronizálva") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Szinkron most", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        // Megjelenés és nyelv (32 Palettes + 4 Languages)
        item {
            Text(strings.appearanceAndLang, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.themes32Title,
                subtitle = "${currentPaletteObj.localizedName(uiState.language)} • Basic / Vivid (32)",
                badgeText = "#${uiState.selectedPaletteId}",
                onClick = onOpenPalettePicker,
                testTag = "settings_open_palettes_row"
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(strings.languageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = uiState.language == lang
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectLanguage(lang) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "${lang.flag} ${lang.code}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) palette.onPrimary else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edzés és értesítések
        item {
            Text("Edzés és értesítések", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = "Edzés időzítő",
                subtitle = "Alapértelmezett pihenőidő (koppints a váltáshoz)",
                badgeText = "${uiState.defaultRestSec} mp",
                onClick = onCycleRestTimer
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hangjelzés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("Pihenőidő lejártakor", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    Switch(checked = uiState.soundAlertEnabled, onCheckedChange = onToggleSoundAlert)
                }
            }
        }

        item {
            SettingsActionRowCard(
                title = "Edzésprofil és progresszió",
                subtitle = "${uiState.trainingStyleHu} • ${uiState.progressionRuleHu}",
                badgeText = "Szerk.",
                onClick = { onToggleProgressionModal(true) }
            )
        }

        // Eszközök és adatok (Óra, Mentés, Engedélyek, Privát fotók)
        item {
            Text("Eszközök és adatok", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }

        item {
            SettingsActionRowCard(
                title = strings.watchAndSyncTitle,
                subtitle = "${uiState.watchName} • Csatlakoztatva (${uiState.watchBatteryPercent}%)",
                badgeText = "Aktív",
                onClick = { onNavigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                testTag = "settings_open_watch_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.backupRestoreTitle,
                subtitle = "Helyi JSON / ZIP és Google Drive biztonsági mentés",
                badgeText = uiState.selectedBackupFormat,
                onClick = { onNavigateToSubScreen(SubScreen.BACKUP_RESTORE) },
                testTag = "settings_open_backup_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.permissionsTitle,
                subtitle = "GPS, Értesítések, Health Connect, Samsung Health",
                badgeText = "Kezelés",
                onClick = { onNavigateToSubScreen(SubScreen.PERMISSIONS_SCREEN) },
                testTag = "settings_open_permissions_row"
            )
        }

        item {
            SettingsActionRowCard(
                title = strings.privatePhotosTitle,
                subtitle = "Csak ezen az eszközön tárolva • Nem kerül felhőbe",
                badgeText = "${uiState.privatePhotoCount} fotó",
                onClick = { onNavigateToSubScreen(SubScreen.PRIVATE_PHOTOS) },
                testTag = "settings_open_photos_row"
            )
        }
    }
}

@Composable
private fun SettingsActionRowCard(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.primary.copy(alpha = 0.18f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
        }
    }
}

/**
 * 4/4 #2 ÓRA ÉS SZINKRON (Mockup 4/4 #2 teljes, részletes felépítése)
 */
@Composable
fun WatchAndSyncScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleControlMode: () -> Unit,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("watch_and_sync_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.watchAndSyncTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Section 1: Órakapcsolat (Mockup 4/4 #2)
        item {
            Text(
                text = "Órakapcsolat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MockupWatchGraphicBadge(size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.watchName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "● Csatlakoztatva",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.success
                                )
                                Text(
                                    text = "Akkumulátor: ${uiState.watchBatteryPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TrainPilot v2.4  •  Utolsó szinkron: ${uiState.lastWatchSyncLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Aktív edzés vezérlése (Mockup 4/4 #2 side-by-side mode cards + gold button + bullets)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aktív edzés vezérlése",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )

                    // Side-by-side cards: Telefon vezérli vs Óra vezérli
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val phoneSelected = uiState.phoneControlsWorkout
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (phoneSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (phoneSelected) 1.5.dp else 1.dp,
                                    color = if (phoneSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!phoneSelected) onToggleControlMode()
                                }
                                .testTag("watch_mode_phone_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📱 Telefon vezérli",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (phoneSelected) palette.primary else palette.textPrimary
                                    )
                                    if (phoneSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Óra megjeleníti",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val watchSelected = !uiState.phoneControlsWorkout
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (watchSelected) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (watchSelected) 1.5.dp else 1.dp,
                                    color = if (watchSelected) palette.primary else palette.cardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!watchSelected) onToggleControlMode()
                                }
                                .testTag("watch_mode_watch_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⌚ Óra vezérli",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (watchSelected) palette.primary else palette.textPrimary
                                    )
                                    if (watchSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Önálló edzés",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onToggleControlMode,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("watch_handover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.phoneControlsWorkout) "Edzés átadása az órának" else "Vezérlés visszavétele telefonra",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "• Pulzus, sorozatok és pihenőidő valós időben szinkronizálva",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "• Offline folytatás: kapcsolat megszakadásakor az óra önállóan folytatja az edzést",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // Section 3: Egészségügyi szinkron (Mockup 4/4 #2: Health Connect & Samsung Health cards)
        item {
            Text(
                text = "Egészségügyi szinkron",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onShowBanner("DEMO • Health Connect szinkron: minden engedély aktív") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MockupHealthConnectBadge(size = 46.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "● Aktív • Olvasás és írás engedélyezve (10/17)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = "Pulzus, alvás, lépések, elégetett kalória, edzések",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onShowBanner("DEMO • Samsung Health közvetlen híd szinkronizálva") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF14B8A6).copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF14B8A6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Samsung Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "● Szinkronizálva • Közvetlen kapcsolat (1/2)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.success
                        )
                        Text(
                            text = "Utolsó frissítés: ma 14:59 • Energia pontszám & testsúly",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 4/4 #3 MENTÉS ÉS VISSZAÁLLÍTÁS (Mockup 4/4 #3)
 */
@Composable
fun BackupRestoreScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFormat: (String) -> Unit,
    onToggleIncludeHealth: (Boolean) -> Unit,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("backup_restore_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.backupRestoreTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("JSON", "ZIP", "DRIVE").forEach { fmt ->
                    val sel = uiState.selectedBackupFormat == fmt
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (sel) palette.primary else palette.cardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelectFormat(fmt) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
                            Text(
                                text = fmt,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) palette.onPrimary else palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mentés tartalma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text("✓ Edzésnapló és PR rekordok (25 edzés)", color = palette.success)
                    Text("✓ Saját programok és gyakorlatok", color = palette.success)
                    Text("✓ Beállítások és 32 színpaletta profil", color = palette.success)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Egészségügyi előzmények (Health)", color = palette.textPrimary)
                        Switch(checked = uiState.includeHealthInBackup, onCheckedChange = onToggleIncludeHealth)
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onShowBanner("DEMO • Mentés elkészült (${uiState.selectedBackupFormat})") },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mentés indítása (${uiState.selectedBackupFormat})", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onShowBanner("DEMO • Visszaállítás fájlból (előnézet)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Visszaállítás fájlból", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 4/4 #4 ENGEDÉLYEK (Mockup 4/4 #4)
 */
@Composable
fun PermissionsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePermission: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.permissionsTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            PermissionToggleCard("GPS és helyadatok", "Kültéri futás és útvonal rögzítése", uiState.permissionGpsEnabled) {
                onTogglePermission("gps")
            }
        }
        item {
            PermissionToggleCard("Központi helymeghatározás", "Pontos távolságmérés a háttérben", uiState.permissionCentralLocEnabled) {
                onTogglePermission("central")
            }
        }
        item {
            PermissionToggleCard("Értesítések", "Pihenőidő és aktív edzés állapotsor", uiState.permissionNotificationsEnabled) {
                onTogglePermission("notif")
            }
        }
        item {
            PermissionToggleCard("Health Connect (10/17)", "Pulzus, alvás, lépések és testsúly szinkron", uiState.permissionHealthConnectEnabled) {
                onTogglePermission("hc")
            }
        }
        item {
            PermissionToggleCard("Samsung Health közvetlen híd", "Energia pontszám és Galaxy Watch7 adatok", uiState.permissionSamsungHealthEnabled) {
                onTogglePermission("samsung")
            }
        }
    }
}

@Composable
private fun PermissionToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

/**
 * 4/4 #5 PRIVÁT EDZÉSFOTÓK (Mockup 4/4 #5)
 */
@Composable
fun PrivatePhotosScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("private_photos_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.privatePhotosTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Csak ezen az eszközön tárolva • A privát edzésfotók nem kerülnek felhőbe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivatePhotoPlaceholderCard("2026. okt. 11. • 17:48", "Teljes testes edzés A #1", Modifier.weight(1f))
                PrivatePhotoPlaceholderCard("2026. okt. 11. • 17:50", "Teljes testes edzés A #2", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onShowBanner("DEMO • Kamera megnyitása privát fotóhoz") },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onShowBanner("DEMO • Galéria megnyitása") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galéria", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PrivatePhotoPlaceholderCard(
    dateLabel: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(caption, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
    }
}
