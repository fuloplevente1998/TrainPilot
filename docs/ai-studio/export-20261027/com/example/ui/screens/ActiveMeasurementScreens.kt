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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.MargitszigetDetailedMapBox
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementModeSwitcherRow(
    currentSubScreen: SubScreen,
    onSelectMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val modes = listOf(
        SubScreen.ACTIVE_STRENGTH to "1. Súlyzós",
        SubScreen.ACTIVE_TIMED to "2. Időalapú",
        SubScreen.ACTIVE_BILATERAL to "3. Kétoldali",
        SubScreen.ACTIVE_OUTDOOR_GPS to "4. Kültéri GPS",
        SubScreen.INTERVAL_SETTINGS to "5. Intervallum",
        SubScreen.WORKOUT_SUMMARY to "6. Összegzés"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, label) ->
            val selected = currentSubScreen == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.primary else palette.surfaceElevated,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("measurement_mode_${mode.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) palette.onPrimary else palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * 1. SÚLYZÓS EDZÉS (Mockup 2/4 #1)
 */
@Composable
fun ActiveStrengthWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onToggleSetCompleted: (Int) -> Unit,
    onAdjustSetValues: (Int, Float, Int, Int) -> Unit,
    onUndoLastSet: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onToggleRestRunning: () -> Unit,
    onSkipRest: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val restMin = uiState.restTimerRemainingSec / 60
    val restSec = uiState.restTimerRemainingSec % 60
    val formattedRest = String.format("%02d:%02d", restMin, restSec)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_strength_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Teljes test A",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "24:18 • 2 / 8 gyakorlat • DEMO",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        item {
            MeasurementModeSwitcherRow(
                currentSubScreen = SubScreen.ACTIVE_STRENGTH,
                onSelectMode = onSelectMeasurementMode
            )
        }

        // Exercise Header Card (Goblet guggolás)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onOpenExerciseDetail("goblet_squat") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExercisePostureMiniIllustration(
                        exerciseName = "Goblet guggolás",
                        size = 56.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Goblet guggolás",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Cél: 3 × 10 • PR: 16 kg × 12",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Súlyzós mérés",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    SmallExerciseVideoButton(exerciseName = "Goblet guggolás")
                }
            }
        }

        // Set table header + Undo button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                    Text("Kg", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                    Text("Ism.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                    Text("RIR", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUndoLastSet() }
                        .testTag("undo_last_set_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Visszavonás",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Set rows
        items(uiState.activeSets, key = { it.setNumber }) { setItem ->
            val isActiveSet = !setItem.completed && uiState.activeSets.firstOrNull { !it.completed }?.setNumber == setItem.setNumber
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isActiveSet) palette.surfaceElevated else palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isActiveSet) 1.5.dp else 1.dp,
                        color = if (isActiveSet) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${setItem.setNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveSet) palette.primary else palette.textSecondary,
                            modifier = Modifier.width(28.dp)
                        )

                        // Weight pill with +/- tap
                        SetValuePill(
                            valueText = setItem.weightKg.toInt().toString(),
                            highlighted = isActiveSet,
                            onClick = { onAdjustSetValues(setItem.setNumber, 2f, 0, 0) }
                        )

                        // Reps pill
                        SetValuePill(
                            valueText = setItem.reps.toString(),
                            highlighted = isActiveSet,
                            onClick = { onAdjustSetValues(setItem.setNumber, 0f, 1, 0) }
                        )

                        // RIR pill
                        SetValuePill(
                            valueText = setItem.rir.toString(),
                            highlighted = isActiveSet,
                            onClick = { onAdjustSetValues(setItem.setNumber, 0f, 0, 1) }
                        )

                        // Check / Complete button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        setItem.completed -> palette.success.copy(alpha = 0.22f)
                                        isActiveSet -> palette.primary
                                        else -> palette.surfaceElevated
                                    }
                                )
                                .clickable { onToggleSetCompleted(setItem.setNumber) }
                                .testTag("toggle_set_${setItem.setNumber}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete set",
                                tint = when {
                                    setItem.completed -> palette.success
                                    isActiveSet -> palette.onPrimary
                                    else -> palette.textSecondary
                                }
                            )
                        }
                    }

                    if (isActiveSet) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SmallAdjustChip("-2 kg") { onAdjustSetValues(setItem.setNumber, -2f, 0, 0) }
                            SmallAdjustChip("+2 kg") { onAdjustSetValues(setItem.setNumber, 2f, 0, 0) }
                            SmallAdjustChip("-1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, -1, 0) }
                            SmallAdjustChip("+1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, 1, 0) }
                        }
                    }
                }
            }
        }

        // Rest Timer Card (Pihenőidő 00:52)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pihenőidő",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedRest,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Automatikus indítás • Rezgés a végén",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }

                        // Circular Rest Ring
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { onToggleRestRunning() },
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = (uiState.restTimerRemainingSec.toFloat() / uiState.defaultRestSec.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ringColor = palette.primary
                            val trackColor = palette.surfaceElevated
                            Canvas(modifier = Modifier.size(60.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustRestTimer(-15) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onAdjustRestTimer(15) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onSkipRest,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Kihagyás", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Prev / Next / Finish Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSelectMeasurementMode(SubScreen.ACTIVE_TIMED) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Időalapú >", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("finish_workout_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edzés befejezése", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SetValuePill(
    valueText: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.background.copy(alpha = 0.65f),
        modifier = Modifier
            .width(64.dp)
            .border(
                width = 1.dp,
                color = if (highlighted) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SmallAdjustChip(
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.background,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 2. IDŐALAPÚ EDZÉS (Mockup 2/4 #2 - Plank)
 */
@Composable
fun ActiveTimedWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var currentSec by remember { mutableIntStateOf(uiState.timedCurrentSec) }
    var isRunning by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_timed_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Teljes test A • 3 / 8 gyakorlat",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            MeasurementModeSwitcherRow(
                currentSubScreen = SubScreen.ACTIVE_TIMED,
                onSelectMode = onSelectMeasurementMode
            )
        }
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
                        ExercisePostureMiniIllustration(
                            exerciseName = "Plank",
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plank",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Cél: 3 × 45 mp • 2. sorozat",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.secondary.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Időalapú mérés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        SmallExerciseVideoButton(exerciseName = "Plank")
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = (currentSec / 45f).coerceIn(0f, 1f)
                        val primaryColor = palette.primary
                        val trackColor = palette.surfaceElevated
                        Canvas(modifier = Modifier.size(190.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("00:%02d", currentSec),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Hátra: ${45 - currentSec} mp",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "Cél: 00:45",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isRunning = !isRunning },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRunning) "Leállítás" else "Indítás", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { currentSec = 0 },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = palette.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nullázás", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        // Completed sets list
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1. sorozat", color = palette.textSecondary)
                        Text("00:45 ✓", color = palette.success, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2. sorozat (Folyamatban)", color = palette.primary, fontWeight = FontWeight.Bold)
                        Text(String.format("00:%02d", currentSec), color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3. sorozat", color = palette.textMuted)
                        Text("00:45", color = palette.textMuted)
                    }
                }
            }
        }
    }
}

/**
 * 3. KÉTOLDALI MÉRÉS (Mockup 2/4 #3 - Oldalsó plank)
 */
@Composable
fun ActiveBilateralWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var activeRightSide by remember { mutableStateOf(true) }
    var leftSec by remember { mutableIntStateOf(uiState.bilateralLeftSec) }
    var rightSec by remember { mutableIntStateOf(uiState.bilateralRightSec) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_bilateral_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Oldalsó plank • Kétoldali mérés",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            MeasurementModeSwitcherRow(
                currentSubScreen = SubScreen.ACTIVE_BILATERAL,
                onSelectMode = onSelectMeasurementMode
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExercisePostureMiniIllustration(
                        exerciseName = "Oldalsó plank",
                        size = 46.dp
                    )
                    Text(
                        text = "Gyakorlat: Oldalsó plank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                SmallExerciseVideoButton(exerciseName = "Oldalsó plank")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bal oldal
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (!activeRightSide) 1.5.dp else 1.dp,
                            color = if (!activeRightSide) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { activeRightSide = false }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Bal oldal", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("00:%02d", leftSec),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text("Kész ✓", style = MaterialTheme.typography.labelSmall, color = palette.success)
                    }
                }
                // Jobb oldal
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (activeRightSide) 1.5.dp else 1.dp,
                            color = if (activeRightSide) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { activeRightSide = true }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Jobb oldal", style = MaterialTheme.typography.labelMedium, color = palette.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("00:%02d", rightSec),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text("Aktív • Cél 00:25", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (activeRightSide) "Jobb oldal mérése" else "Bal oldal mérése",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("00:%02d", if (activeRightSide) rightSec else leftSec),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { activeRightSide = !activeRightSide },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Oldal váltása", color = palette.textPrimary)
                        }
                        Button(
                            onClick = {
                                if (activeRightSide) rightSec = 25 else leftSec = 25
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            )
                        ) {
                            Text("Oldal kész (00:25)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. KÜLTÉRI MÉRÉS (Mockup 2/4 #4 - Futás GPS)
 */
@Composable
fun ActiveOutdoorGpsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var gpsActive by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var lapCount by remember { mutableIntStateOf(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_outdoor_gps_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mockup 2/4 #4 Header: Pause icon + "Futás" + green "● GPS aktív • Jó jel" + Settings icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Szünet / Vissza",
                                tint = palette.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Futás • Kültéri mérés",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (gpsActive) palette.success else palette.primary)
                            )
                            Text(
                                text = if (gpsActive) "GPS aktív • Jó jel • DEMO" else "GPS rögzítés szünetel",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (gpsActive) palette.success else palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = palette.textSecondary
                    )
                }
            }
        }

        item {
            MeasurementModeSwitcherRow(
                currentSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
                onSelectMode = onSelectMeasurementMode
            )
        }

        // 1. GPS Route Mini Map Card (Mockup 2/4 #4 "GPS útvonal" + "GPS rögzítés" switch + Margitsziget mini map)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.success.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = palette.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "GPS útvonal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Margitsziget futókör • Élő mini térkép",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GPS rögzítés",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Switch(
                                checked = gpsActive,
                                onCheckedChange = { gpsActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.onPrimary,
                                    checkedTrackColor = palette.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    MargitszigetDetailedMapBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp),
                        interactiveControls = true
                    )
                }
            }
        }

        // 2. Hero Elapsed Timer + 3-column Live Metrics Card (Mockup 2/4 #4: 09:42 Eltelt idő | 1,62 km | 6:00 /km | 118 kcal)
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPaused) "SZÜNETELTETVE" else "ELTELT IDŐ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) palette.primary else palette.textSecondary
                    )
                    Text(
                        text = uiState.outdoorElapsedLabel,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutdoorMetricTile(
                            label = "Távolság",
                            value = "1,62 km",
                            subValue = "Cél: 5,0 km",
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutdoorMetricTile(
                            label = "Átlagtempó",
                            value = "${uiState.outdoorPaceLabel} /km",
                            subValue = "Aktuális: 5:52",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                        OutdoorMetricTile(
                            label = "Kalória & Pulzus",
                            value = "118 kcal",
                            subValue = "♥ 142 bpm",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Km-részidők (Splits) preview row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. km részidő: 5:58 /km  •  2. km (rész): 6:03 /km",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "$lapCount kör",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Control buttons: Szünet | Részidő (Kör) | Befejezés
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isPaused = !isPaused },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPaused) "Folytatás" else "Szünet",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { lapCount += 1 },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "+ Kör ($lapCount)",
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Befejezés", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutdoorMetricTile(
    label: String,
    value: String,
    subValue: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.background.copy(alpha = 0.55f),
        modifier = modifier.border(
            width = 1.dp,
            color = if (highlighted) palette.primary.copy(alpha = 0.6f) else palette.cardBorder,
            shape = RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) palette.primary else palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * 5. INTERVALLUMOK BEÁLLÍTÁSA (Mockup 2/4 #5)
 */
@Composable
fun IntervalSettingsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onSaveAndStartIntervals: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var workSec by remember { mutableIntStateOf(uiState.intervalWorkSec) }
    var restSec by remember { mutableIntStateOf(uiState.intervalRestSec) }
    var rounds by remember { mutableIntStateOf(uiState.intervalRounds) }
    var sound by remember { mutableStateOf(uiState.soundAlertEnabled) }
    var vibration by remember { mutableStateOf(uiState.vibrationEnabled) }
    var autoStep by remember { mutableStateOf(uiState.autoStepIntervals) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("interval_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubScreenTopBar(
                title = "Intervallumok beállítása",
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }
        item {
            MeasurementModeSwitcherRow(
                currentSubScreen = SubScreen.INTERVAL_SETTINGS,
                onSelectMode = onSelectMeasurementMode
            )
        }
        item {
            IntervalStepperCard(
                label = "Munkaidő",
                valueText = "$workSec mp",
                onMinus = { workSec = (workSec - 5).coerceAtLeast(10) },
                onPlus = { workSec = (workSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Pihenőidő",
                valueText = "$restSec mp",
                onMinus = { restSec = (restSec - 5).coerceAtLeast(10) },
                onPlus = { restSec = (restSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Ismétlések (körök)",
                valueText = "$rounds kör",
                onMinus = { rounds = (rounds - 1).coerceAtLeast(1) },
                onPlus = { rounds = (rounds + 1).coerceAtMost(20) }
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hangjelzés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rezgés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = vibration, onCheckedChange = { vibration = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automatikus léptetés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = autoStep, onCheckedChange = { autoStep = it })
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    onSaveAndStartIntervals(workSec, restSec, rounds, sound, vibration, autoStep)
                    onSelectMeasurementMode(SubScreen.ACTIVE_TIMED)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Intervallumok indítása", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IntervalStepperCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
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
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(valueText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = palette.textPrimary)
                }
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = palette.onPrimary)
                }
            }
        }
    }
}

/**
 * 6. EDZÉS BEFEJEZÉSE / ÖSSZEGZÉSE (Mockup 2/4 #6)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutSummaryScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onSelectMood: (Int) -> Unit,
    onUpdateNote: (String) -> Unit,
    onSaveWorkout: () -> Unit,
    onPauseAndExit: () -> Unit,
    onDiscardWorkout: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val moods = listOf(
        "😄 Túl könnyű",
        "🙂 Könnyű",
        "💪 Pont jó",
        "😤 Nehéz",
        "🥵 Túl nehéz",
        "⚠️ Fájdalom"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("workout_summary_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Edzés összegzése",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "Teljes test A • 8 / 8 gyakorlat",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }

        // 3 stat boxes: 38:24, 420 ism., 312 kcal
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile("Időtartam", "38:24", Modifier.weight(1f))
                SummaryStatTile("Volumen", "420 ism.", Modifier.weight(1f))
                SummaryStatTile("Kalória", "312 kcal", Modifier.weight(1f))
            }
        }

        // Mood selector ("Milyen volt az edzés?")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Milyen volt az edzés?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moods.forEachIndexed { idx, moodLabel ->
                            val selected = uiState.workoutSummaryMoodIndex == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectMood(idx) }
                                    .testTag("mood_chip_$idx")
                            ) {
                                Text(
                                    text = moodLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) palette.onPrimary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = uiState.workoutSummaryNote,
                        onValueChange = onUpdateNote,
                        placeholder = { Text("Megjegyzés hozzáadása az edzéshez...", color = palette.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Action buttons: Mentés, Szüneteltetés és kilépés, Edzés elvetése
        item {
            Button(
                onClick = onSaveWorkout,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_workout_summary_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mentés", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPauseAndExit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Szüneteltetés és kilépés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDiscardWorkout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edzés elvetése", color = palette.danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceCard,
        modifier = modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}
