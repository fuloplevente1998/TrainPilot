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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import com.example.localization.LocalizedStrings
import com.example.model.LoggedWorkoutEntry
import com.example.model.SubScreen
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.MargitszigetDetailedMapBox
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.MockupWatchGraphicBadge
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 1. NAPLÓ (Mockup 3/4 #1 + main Edzésnapló)
 */
@Composable
fun LogTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSearchChange: (String) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    onOpenRouteMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val availableMonths = listOf(
        "Összes hónap (2026)",
        "2026. augusztus",
        "2026. szeptember",
        "2026. október",
        "2026. november"
    )
    var selectedMonthIndex by remember { mutableStateOf(3) } // 2026. október
    var selectedDateFilter by remember { mutableStateOf<String?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val currentMonthLabel = selectedDateFilter ?: availableMonths[selectedMonthIndex]

    val filteredLogs = uiState.loggedWorkouts.filter { entry ->
        val matchesSearch = uiState.logSearchQuery.isBlank() ||
            entry.titleHu.contains(uiState.logSearchQuery, ignoreCase = true) ||
            entry.summaryExercisesHu.contains(uiState.logSearchQuery, ignoreCase = true)
        val matchesDate = selectedDateFilter == null || entry.dateLabelHu.contains(selectedDateFilter!!, ignoreCase = true)
        matchesSearch && matchesDate
    }

    if (showDatePickerDialog) {
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "Dátum- és hónapválasztó (Napló)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Válassz hónapot:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    availableMonths.forEachIndexed { idx, monthName ->
                        val isSel = selectedDateFilter == null && selectedMonthIndex == idx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSel) palette.primary else palette.cardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedMonthIndex = idx
                                    selectedDateFilter = null
                                    showDatePickerDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 $monthName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = palette.textPrimary
                                )
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vagy ugorj konkrét edzésnapra:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val specificDates = listOf(
                        "okt. 13." to "2026. okt. 13. • Kültéri futás (Margitsziget)",
                        "okt. 11." to "2026. okt. 11. • Teljes testes edzés A",
                        "okt. 09." to "2026. okt. 09. • Teljes testes edzés B",
                        "okt. 06." to "2026. okt. 06. • Felsőtest & Core"
                    )
                    specificDates.forEach { (shortKey, fullDesc) ->
                        val isDateSel = selectedDateFilter == shortKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDateSel) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isDateSel) palette.primary else palette.cardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedDateFilter = shortKey
                                    showDatePickerDialog = false
                                }
                        ) {
                            Text(
                                text = fullDesc,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        selectedDateFilter = null
                        selectedMonthIndex = 3
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Alaphelyzet (Október)", color = palette.primary)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePickerDialog = false }) {
                    Text("Bezárás", color = palette.textPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("log_tab_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = uiState.logSearchQuery,
                onValueChange = onSearchChange,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                placeholder = { Text(strings.logSearchHint, color = palette.textMuted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_search_input")
            )
        }

        // Interactive Month & Date selector row (< | 📅 2026. október v | >)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            selectedDateFilter = null
                            selectedMonthIndex = (selectedMonthIndex - 1 + availableMonths.size) % availableMonths.size
                        }
                        .testTag("log_prev_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Előző hónap",
                            tint = palette.primary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDatePickerDialog = true }
                        .testTag("log_month_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentMonthLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Dátumválasztó megnyitása",
                            tint = palette.primary
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            selectedDateFilter = null
                            selectedMonthIndex = (selectedMonthIndex + 1) % availableMonths.size
                        }
                        .testTag("log_next_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Következő hónap",
                            tint = palette.primary
                        )
                    }
                }
            }
        }

        // Quick shortcut banner to Edzés részletei & Óra és szinkron
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentMonthLabel • ${filteredLogs.size} rögzített edzés (DEMO)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val firstStrength = filteredLogs.firstOrNull { !it.isOutdoorRun } ?: uiState.loggedWorkouts.first()
                            onOpenWorkoutDetail(firstStrength.id)
                        }
                        .testTag("log_open_latest_detail_shortcut")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edzés részletei",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        items(filteredLogs, key = { it.id }) { logEntry ->
            LogWorkoutCard(
                entry = logEntry,
                onClick = {
                    if (logEntry.isOutdoorRun) {
                        onOpenRouteMap()
                    } else {
                        onOpenWorkoutDetail(logEntry.id)
                    }
                },
                onOpenDetailAlways = { onOpenWorkoutDetail(logEntry.id) }
            )
        }
    }
}

@Composable
private fun LogWorkoutCard(
    entry: LoggedWorkoutEntry,
    onClick: () -> Unit,
    onOpenDetailAlways: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenDetailAlways() }
            .testTag("log_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.isOutdoorRun) palette.success.copy(alpha = 0.16f)
                        else palette.primary.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isOutdoorRun) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (entry.isOutdoorRun) palette.success else palette.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.dateLabelHu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.titleHu,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (entry.isOutdoorRun) {
                        "${entry.distanceKm} km • ${entry.workTimeLabel} • ${entry.paceLabel}"
                    } else {
                        "${entry.durationMinutes} perc • ${entry.totalSets} sorozat • ${entry.totalVolumeKg} kg"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
                Text(
                    text = entry.summaryExercisesHu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (entry.hasPr) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.primary
                    ) {
                        Text(
                            text = "PR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (entry.isOutdoorRun) {
                    // Mini route thumbnail (clickable to open route map directly)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF121C26))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        val lineCol = palette.success
                        Canvas(modifier = Modifier.size(30.dp)) {
                            val path = Path().apply {
                                moveTo(4f, size.height - 4f)
                                lineTo(size.width * 0.45f, size.height * 0.35f)
                                lineTo(size.width * 0.72f, size.height * 0.65f)
                                lineTo(size.width - 4f, 4f)
                            }
                            drawPath(
                                path = path,
                                color = lineCol,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenDetailAlways() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Részletek",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. EDZÉS RÉSZLETEI (Mockup 3/4 #2)
 */
@Composable
fun LogWorkoutDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDetailTab: (Int) -> Unit,
    onToggleExpandedExercise: (Int) -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenRouteMap: () -> Unit,
    onOpenWatchSync: () -> Unit,
    onOpenHealthTab: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val workout = uiState.loggedWorkouts.find { it.id == uiState.selectedLogWorkoutId }
        ?: uiState.loggedWorkouts.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("log_workout_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SubScreenTopBar(
                title = strings.workoutDetailTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        // Summary Header Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = workout.titleHu,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.success,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "${workout.dateLabelHu} • 17:03–17:51",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = palette.cardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${workout.durationMinutes} perc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("edzésidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.workTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("munkaidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.restTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("pihenőidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        // Sub-tabs: Gyakorlatok (5) | Statisztikák | Forrás
        item {
            val tabTitles = listOf(
                "Gyakorlatok (${workout.exerciseDetails.size.coerceAtLeast(5)})",
                "Statisztikák",
                "Forrás"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabTitles.forEachIndexed { idx, label ->
                    val selected = uiState.logDetailTabIndex == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDetailTab(idx) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) palette.onPrimary else palette.textSecondary,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        // Tab-specific content for Gyakorlatok (0) | Statisztikák (1) | Forrás (2)
        if (uiState.logDetailTabIndex == 0) {
            val details = workout.exerciseDetails
            items(details.size) { index ->
                val exDetail = details[index]
                val expanded = uiState.expandedLogExerciseIndex == index
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleExpandedExercise(index) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExercisePostureMiniIllustration(
                                exerciseNameOrId = exDetail.nameHu,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exDetail.nameHu,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = exDetail.summaryPrescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SmallExerciseVideoButton(exerciseName = exDetail.nameHu)
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.textSecondary
                                )
                            }
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = palette.cardBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            exDetail.sets.forEach { setRow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${setRow.setNumber}. sorozat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "${setRow.weightKg.toInt()} kg  ×  ${setRow.reps}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.logDetailTabIndex == 1) {
            item {
                AnatomicalMuscleHeatmapCard(
                    title = "Edzés izomterhelése (${workout.titleHu})",
                    subtitle = "Elöl- és hátulnézeti anatómiai terhelés ezen az edzésen"
                )
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Adatforrás és szinkron napló", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("• Rögzítő eszköz: Galaxy Watch7 (TrainPilot Wear OS v2.4)", color = palette.textPrimary)
                        Text("• Másodlagos híd: Health Connect + Samsung Health", color = palette.textSecondary)
                        Text("• Átlagpulzus: 128 bpm • Max pulzus: 162 bpm • Aktív kalória: 312 kcal", color = palette.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Fotók (2) & Útvonal megtekintése buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenPhotos,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("log_detail_photos_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fotók (2)", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onOpenRouteMap,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("log_detail_route_button")
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Útvonal megtekintése", color = palette.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        // Egészségügyi forrás: TrainPilot óra & Health Connect (Mockup 3/4 #2 custom icons)
        item {
            Text(
                text = "Egészségügyi forrás",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenWatchSync() }
                        .testTag("log_detail_open_watch_sync")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupWatchGraphicBadge(modifier = Modifier.size(34.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TrainPilot óra",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Galaxy Watch7",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenHealthTab() }
                        .testTag("log_detail_open_health_connect")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupHealthConnectBadge(modifier = Modifier.size(34.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Health Connect",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Szinkronizálva",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. EDZÉSÚTVONAL (Mockup 3/4 #3 - Margitsziget 1,62 km)
 */
@Composable
fun LogRouteMapScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp)
            .testTag("log_route_map_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.routeMapTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Surface(
                shape = CircleShape,
                color = palette.danger,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .testTag("close_route_map_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        // High-precision Margitsziget + Duna + Vizafogó + Újlipótváros + Margit híd Map Canvas
        MargitszigetDetailedMapBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            interactiveControls = true
        )

        // Bottom info card + Km Splits matching Mockup 3/4 #3
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "1,62 km",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.success.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "7:48 /km átlag",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = "Privát útvonal • 31 GPS-pont • Margitsziget futókör",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "2026. október 13. • 07:12–07:24 (DEMO)",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                HorizontalDivider(color = palette.cardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1. km részidő: 7:42 /km (142 bpm)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("0,62 km: 7:56 /km (151 bpm)", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 4. FEJLŐDÉS (Kompakt `main` Fejlődés & Statisztikák + Mockup 3/4 #4 1RM Erőnlét)
 * Minden fő kártya ráfér a képernyőre, és koppintásra lenyílik / becsukódik!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSelectMainSection: (Int) -> Unit,
    onSelectExercise: (String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onSelectStatsSubTab: (Int) -> Unit,
    onToggleCardioFilterStepsMode: (Boolean) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val sections = listOf("📈 Fejlődés", "📊 Statisztikák", "🏋️ Erőnlét (1RM)")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("progress_tab_screen"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Top Sub-Tab Bar (like in `main`: Fejlődés | Statisztikák | Erőnlét 1RM)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sections.forEachIndexed { idx, label ->
                        val selected = uiState.progressMainSection == idx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) palette.primary.copy(alpha = 0.22f) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) palette.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectMainSection(idx) }
                                .testTag("progress_section_tab_$idx")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) palette.primary else palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 0 (DEFAULT): Exact Compact `main` Fejlődés Dashboard (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`)
        if (uiState.progressMainSection == 0) {
            item {
                MainProgressVolumeAndMuscleSection(
                    uiState = uiState,
                    onSelectRange = onSelectRange,
                    onSelectVolumeBar = onSelectVolumeBar,
                    onSelectExpandedStatsCard = onSelectExpandedStatsCard,
                    onOpenWorkoutDetail = onOpenWorkoutDetail
                )
            }
        }

        // SECTION 1: Full `main` Statisztikák (Személyes rekordok & Kardióstatisztika)
        if (uiState.progressMainSection == 1) {
            item {
                MainStatisticsRecordsAndCardioSection(
                    uiState = uiState,
                    onSelectStatsSubTab = onSelectStatsSubTab,
                    onToggleCardioFilterStepsMode = onToggleCardioFilterStepsMode
                )
            }
        }

        // SECTION 2: Mockup 3/4 #4 Erőnlét (1RM becslés + gyakorlatválasztó + PR)
        if (uiState.progressMainSection == 2) {
            item {
                val exerciseOptions = listOf("Fekvenyomás", "Goblet guggolás", "Döntött törzsű evezés")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gyakorlat (1RM becslés)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            SmallExerciseVideoButton(exerciseName = uiState.selectedProgressExercise)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            exerciseOptions.forEach { exName ->
                                val active = uiState.selectedProgressExercise == exName
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) palette.primary else palette.surfaceElevated,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectExercise(exName) }
                                ) {
                                    Text(
                                        text = exName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) palette.onPrimary else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1RM becslés (kg) Line Chart Card
            item {
                val est1Rm = when (uiState.selectedProgressExercise) {
                    "Goblet guggolás" -> "24 kg"
                    "Döntött törzsű evezés" -> "68 kg"
                    else -> "82 kg"
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1RM becslés (kg) ⓘ", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = est1Rm,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "+ 12 kg (3 hó)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.success
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val lineGold = palette.primary
                        val gridCol = palette.cardBorder
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            for (i in 0..3) {
                                val y = h * i / 3f
                                drawLine(gridCol, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                            }
                            val pts = listOf(0.75f, 0.68f, 0.60f, 0.52f, 0.45f, 0.34f, 0.26f, 0.18f)
                            val path = Path()
                            pts.forEachIndexed { idx, normY ->
                                val x = w * (idx / (pts.size - 1f))
                                val y = h * normY
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = lineGold,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            pts.forEachIndexed { idx, normY ->
                                val x = w * (idx / (pts.size - 1f))
                                val y = h * normY
                                drawCircle(color = lineGold, radius = 4.dp.toPx(), center = Offset(x, y))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("júl.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            Text("aug.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            Text("szept.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            Text("okt.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Személyes rekord", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text("🏆 100 kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                            Text("2026. szept. 14.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Legjobb sorozat", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            Text("80 kg × 8", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("2026. okt. 11.", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Exact Compact `main` Fejlődés Layout (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * 1. Összes volumen (20 800 kg) with side-by-side 2x2 period selector (1 nap, 7 nap, 30 nap, 3 hó) + bar chart, expandable on tap!
 * 2. 2x2 Compact Expandable Metric Cards (PR rekordok 45, Edzések száma 25, Átlagos volumen 904,3 kg, Átl. edzésidő 45 min)
 * 3. Compact Side-by-Side Izomcsoport-terhelés (Front & Back bodies on left + 6 muscle bars on right)
 * 4. Compact Expandable Fejlődési javaslat card at the bottom
 */
@Composable
private fun MainProgressVolumeAndMuscleSection(
    uiState: TrainPilotUiState,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var expandedVolumeDetails by remember { mutableStateOf(false) }
    var expandedAdviceDetails by remember { mutableStateOf(false) }

    val volumeBars = listOf(
        "5" to 200,
        "12" to 300,
        "19" to 150,
        "26" to 250,
        "2" to 650,
        "9" to 2100,
        "16" to 3850,
        "23" to 2050,
        "30" to 3250,
        "7" to 4400,
        "14" to 2750,
        "21" to 2600
    )

    val totalVolumeText = when (uiState.selectedProgressRangeIndex) {
        0 -> "1 430 kg"
        1 -> "3 230 kg"
        2 -> "9 760 kg"
        else -> "20 800 kg"
    }

    val dateSubtitle = when (uiState.selectedProgressRangeIndex) {
        0 -> "2026. okt. 26. – 2026. okt. 27."
        1 -> "2026. okt. 20. – 2026. okt. 27."
        2 -> "2026. szept. 27. – 2026. okt. 27."
        else -> "2026. júl. 29. – 2026. okt. 27."
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. ÖSSZES VOLUMEN COMPACT SIDE-BY-SIDE CARD (Gold border, 2x2 period selector + bar chart)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("progress_total_volume_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row (clickable to expand/collapse volume breakdown)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expandedVolumeDetails = !expandedVolumeDetails },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Összes volumen",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = totalVolumeText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = dateSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (expandedVolumeDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "az előző időszakhoz képest",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Side-by-Side Row: Left = 2x2 Period Buttons (1 nap, 7 nap, 30 nap, 3 hó), Right = Y-axis + Bar Chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: 2x2 Grid of Period Buttons
                    val rangeLabels = listOf("1 nap", "7 nap", "30 nap", "3 hó")
                    Column(
                        modifier = Modifier.weight(0.45f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 1).forEach { rIdx ->
                                val isSel = uiState.selectedProgressRangeIndex == rIdx
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (isSel) palette.primary else palette.surfaceElevated,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) palette.primary else palette.cardBorder,
                                            shape = RoundedCornerShape(11.dp)
                                        )
                                        .clip(RoundedCornerShape(11.dp))
                                        .clickable { onSelectRange(rIdx) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = rangeLabels[rIdx],
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) palette.onPrimary else palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2, 3).forEach { rIdx ->
                                val isSel = uiState.selectedProgressRangeIndex == rIdx
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (isSel) palette.primary else palette.surfaceElevated,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) palette.primary else palette.cardBorder,
                                            shape = RoundedCornerShape(11.dp)
                                        )
                                        .clip(RoundedCornerShape(11.dp))
                                        .clickable { onSelectRange(rIdx) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = rangeLabels[rIdx],
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) palette.onPrimary else palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right: Y-Axis Labels (8000, 6000, 4000, 2000, 0) + 12 Gold Rounded Bars
                    Row(
                        modifier = Modifier
                            .weight(0.55f)
                            .height(86.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .height(72.dp)
                                .padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            listOf("8000", "6000", "4000", "2000", "0").forEach { yLbl ->
                                Text(
                                    text = yLbl,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(82.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            volumeBars.forEachIndexed { idx, (dayLbl, kg) ->
                                val isBarSelected = uiState.selectedVolumeBarIndex == idx
                                val ratio = (kg / 5000f).coerceIn(0.06f, 1f)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectVolumeBar(idx)
                                            expandedVolumeDetails = true
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((62 * ratio).dp)
                                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                            .background(
                                                if (isBarSelected) palette.primary else palette.primary.copy(alpha = 0.82f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = dayLbl,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                        color = if (isBarSelected) palette.primary else palette.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Volume Breakdown when user taps the header or any bar
                AnimatedVisibility(visible = expandedVolumeDetails) {
                    val selBar = volumeBars.getOrElse(uiState.selectedVolumeBarIndex) { volumeBars.last() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = palette.cardBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kiválasztott hét (${selBar.first}.): ${selBar.second} kg összvolumen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = "Bezárás ▲",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                modifier = Modifier.clickable { expandedVolumeDetails = false }
                            )
                        }
                        uiState.loggedWorkouts.take(2).forEach { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.surfaceElevated)
                                    .clickable { onOpenWorkoutDetail(w.id) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${w.dateLabelHu} • ${w.titleHu}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Részletek >",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 2x2 COMPACT EXPANDABLE METRIC CARDS (PR rekordok 45, Edzések száma 25, Átlagos volumen 904,3 kg, Átl. edzésidő 45 min)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandableStatSummaryTile(
                icon = Icons.Default.EmojiEvents,
                title = "PR rekordok",
                value = "45",
                dashSuffix = "—",
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 0,
                onClick = { onSelectExpandedStatsCard(0) },
                modifier = Modifier.weight(1f)
            )
            ExpandableStatSummaryTile(
                icon = Icons.Default.CalendarMonth,
                title = "Edzések száma",
                value = "25",
                dashSuffix = "—",
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 1,
                onClick = { onSelectExpandedStatsCard(1) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandableStatSummaryTile(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Átlagos volumen",
                value = "904,3 kg",
                dashSuffix = "—",
                subtitle = "súlyzós edzésenként",
                selected = uiState.expandedStatsCardIndex == 2,
                onClick = { onSelectExpandedStatsCard(2) },
                modifier = Modifier.weight(1f)
            )
            ExpandableStatSummaryTile(
                icon = Icons.Default.Schedule,
                title = "Átl. edzésidő",
                value = "45 min",
                dashSuffix = null,
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 3,
                onClick = { onSelectExpandedStatsCard(3) },
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable Dropdown Card when any of the 4 tiles is tapped (collapsed by default so everything fits on screen!)
        AnimatedVisibility(visible = uiState.expandedStatsCardIndex in 0..3) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (uiState.expandedStatsCardIndex) {
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PR rekordok (45)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Bezárás ▲", style = MaterialTheme.typography.labelSmall, color = palette.primary, modifier = Modifier.clickable { onSelectExpandedStatsCard(0) })
                            }
                            uiState.prRecords.take(4).forEach { pr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surfaceElevated)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pr.exerciseNameHu, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                        Text(pr.typeLabelHu, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    }
                                    SmallExerciseVideoButton(exerciseName = pr.exerciseNameHu, compactIconOnly = true)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${pr.previousLabel} → ${pr.newLabel}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                }
                            }
                        }
                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Edzések száma (25)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Bezárás ▲", style = MaterialTheme.typography.labelSmall, color = palette.primary, modifier = Modifier.clickable { onSelectExpandedStatsCard(1) })
                            }
                            uiState.loggedWorkouts.take(4).forEach { w ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surfaceElevated)
                                        .clickable { onOpenWorkoutDetail(w.id) }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${w.dateLabelHu} • ${w.titleHu}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text("${w.durationMinutes} min >", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        2 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Átlagos volumen • 904,3 kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Bezárás ▲", style = MaterialTheme.typography.labelSmall, color = palette.primary, modifier = Modifier.clickable { onSelectExpandedStatsCard(2) })
                            }
                            val volRows = listOf("2026. okt. 26." to "852 kg", "2026. okt. 24." to "1 430 kg", "2026. okt. 19." to "1 397 kg", "2026. okt. 14." to "980 kg")
                            volRows.forEach { (d, v) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surfaceElevated)
                                        .clickable { onOpenWorkoutDetail("log_4") }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(d, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                    Text("$v >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Átlagos edzésidő • 45 min", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Bezárás ▲", style = MaterialTheme.typography.labelSmall, color = palette.primary, modifier = Modifier.clickable { onSelectExpandedStatsCard(3) })
                            }
                            val durRows = listOf("2026. okt. 26." to "51 min", "2026. okt. 24." to "35 min", "2026. okt. 24. (2)" to "29 min", "2026. okt. 19." to "70 min")
                            durRows.forEach { (d, dur) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.surfaceElevated)
                                        .clickable { onOpenWorkoutDetail("log_4") }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(d, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                    Text("$dur >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. COMPACT SIDE-BY-SIDE IZOMCSOPORT-TERHELÉS CARD (Front & Back bodies on left + 6 muscle bars on right)
        AnatomicalMuscleHeatmapCard(
            title = "Izomcsoport-terhelés",
            subtitle = "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek."
        )

        // 4. COMPACT EXPANDABLE "Fejlődési javaslat" CARD AT BOTTOM (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (expandedAdviceDetails) palette.primary else palette.cardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable { expandedAdviceDetails = !expandedAdviceDetails }
                .testTag("progress_advice_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fejlődési javaslat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "A legtöbb sorozatterhelést ez az izomcsoport kapta: Kar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = if (expandedAdviceDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }

                AnimatedVisibility(visible = expandedAdviceDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = palette.cardBorder)
                        Text(
                            text = "• Egyensúly javaslat: A Mell (45%) és Váll (59%) sorozatszámát érdemes +2 munkasorozattal növelni a következő héten.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "• Dupla progresszió: A Goblet guggolás (16 kg × 12) elérte a felső ismétléshatárt, emelj 18 kg-ra!",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableStatSummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    dashSuffix: String?,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceCard,
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    if (dashSuffix != null) {
                        Text(
                            text = dashSuffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (selected) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MainStatisticsRecordsAndCardioSection(
    uiState: TrainPilotUiState,
    onSelectStatsSubTab: (Int) -> Unit,
    onToggleCardioFilterStepsMode: (Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val subTabs = listOf("Személyes rekordok", "Kardióstatisztika")
            subTabs.forEachIndexed { idx, label ->
                val selected = uiState.statsSubTab == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) palette.primary.copy(alpha = 0.22f) else palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, if (selected) palette.primary else palette.cardBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectStatsSubTab(idx) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) palette.primary else palette.textPrimary
                        )
                    }
                }
            }
        }

        if (uiState.statsSubTab == 0) {
            uiState.prRecords.forEach { pr ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(pr.exerciseNameHu, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text(pr.typeLabelHu, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Korábbi", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Text(pr.previousLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.primary.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, palette.primary, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Új rekord", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    Text(pr.newLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Kardióstatisztika (from main screenshots)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clickable { onToggleCardioFilterStepsMode(!uiState.cardioFilterStepsMode) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.cardioFilterStepsMode) "Napi aktivitás / lépések" else "Minden mozgás",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text("Váltás >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatBox(
                    label = if (uiState.cardioFilterStepsMode) "Mai lépések" else "Heti kilométer",
                    value = if (uiState.cardioFilterStepsMode) "${uiState.dailySteps}" else "0 km",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    label = if (uiState.cardioFilterStepsMode) "7 napos átlag" else "Előző hét",
                    value = if (uiState.cardioFilterStepsMode) "6 377" else "1,623 km",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatBox(
                    label = if (uiState.cardioFilterStepsMode) "Mai aktív kcal" else "Átlagtempó",
                    value = if (uiState.cardioFilterStepsMode) "164 kcal" else "18:03 /km",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    label = if (uiState.cardioFilterStepsMode) "Célteljesítés (7 nap)" else "Rögzített idő",
                    value = if (uiState.cardioFilterStepsMode) "0/7" else "29:17",
                    modifier = Modifier.weight(1f)
                )
            }

            // Weekly Cardio Distance List from `main` screenshot (`Screenshot_2026-10-27-11-25-45-367_com.repforge.app.jpg`)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val cardioWeeks = listOf(
                        "2026. szept. 7." to (0f to "0 km"),
                        "2026. szept. 14." to (0f to "0 km"),
                        "2026. szept. 21." to (0f to "0 km"),
                        "2026. szept. 28." to (0f to "0 km"),
                        "2026. okt. 5." to (0f to "0 km"),
                        "2026. okt. 12." to (0f to "0 km"),
                        "2026. okt. 19." to (1f to "1,623 km"),
                        "2026. okt. 26." to (0f to "0 km")
                    )
                    cardioWeeks.forEach { (weekLabel, pair) ->
                        val (progressVal, kmLabel) = pair
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = weekLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary,
                                modifier = Modifier.width(96.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.surfaceElevated)
                            ) {
                                if (progressVal > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressVal)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.primary)
                                    )
                                }
                            }
                            Text(
                                text = kmLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (progressVal > 0f) palette.primary else palette.textSecondary,
                                modifier = Modifier.width(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
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
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}
