package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.ProgramTypeEmblemIcon
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.programEquipmentCategoryLabel
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
fun WorkoutTabScreen(
    state: TrainPilotUiState,
    onOpenSettings: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenOutdoorWorkout: () -> Unit,
    onOpenProgramsList: () -> Unit,
    onOpenProgramEditor: (String, Int) -> Unit,
    onStartWorkoutDay: (Int) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenProgressionModal: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Gyors edzés Card (Mockup 1/4 #2)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenQuickWorkout() }
                .testTag("workout_tab_quick_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gyors edzés",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Indíts egy rövid edzést most",
                        style = MaterialTheme.typography.bodyMedium,
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

        // 2. Two-column cards: Gyakorlat választása (with posture mini-graphic) & Szabadtéri edzés (with mini GPS map & live metrics preview)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenExerciseLibrary() }
                    .testTag("workout_tab_exercise_picker")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExercisePostureMiniIllustration(
                            exerciseName = "Goblet guggolás",
                            size = 42.dp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "106 gyak.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Gyakorlat\nválasztása",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pozíció & izom\ngrafikonokkal",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenOutdoorWorkout() }
                    .testTag("workout_tab_outdoor_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini outdoor GPS map preview box
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14261C))
                                .border(1.dp, palette.success.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                // River stripe
                                drawLine(
                                    color = Color(0xFF1E3A5F),
                                    start = Offset(0f, h * 0.2f),
                                    end = Offset(w * 0.35f, h),
                                    strokeWidth = 8.dp.toPx()
                                )
                                // GPS loop
                                drawLine(
                                    color = Color(0xFFF59E0B),
                                    start = Offset(w * 0.28f, h * 0.78f),
                                    end = Offset(w * 0.55f, h * 0.24f),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color(0xFFF59E0B),
                                    start = Offset(w * 0.55f, h * 0.24f),
                                    end = Offset(w * 0.82f, h * 0.65f),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = Color(0xFF22C55E),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(w * 0.28f, h * 0.78f)
                                )
                                drawCircle(
                                    color = Color(0xFFF59E0B),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(w * 0.82f, h * 0.65f)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.success.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "GPS élő",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Szabadtéri\nedzés",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mini térkép, táv,\ntempó & GPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }
        }

        // 3. Aktuális program Header + Összes megtekintése >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktuális program",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenProgramsList() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Összes megtekintése",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Side-by-side A nap / B nap cards (Mockup 1/4 #2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            activeProgram.days.take(2).forEachIndexed { index, day ->
                val isPrimaryDay = index == 0
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isPrimaryDay) 1.5.dp else 1.dp,
                            color = if (isPrimaryDay) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onOpenProgramEditor(activeProgram.id, index) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.dayCode,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isPrimaryDay) palette.primary else palette.textSecondary
                            )
                            ProgramTypeEmblemIcon(
                                programTitle = "${activeProgram.nameHu} ${day.titleHu}",
                                isHighlighted = isPrimaryDay,
                                size = 34.dp
                            )
                        }
                        Text(
                            text = day.localizedTitle(state.language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.surfaceElevated
                        ) {
                            Text(
                                text = programEquipmentCategoryLabel(activeProgram.nameHu),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "📋 ${day.exercises.size} gyakorlat • ⏱ kb. ${day.durationMinutes} perc",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = { onStartWorkoutDay(index) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPrimaryDay) palette.primary else palette.surfaceElevated,
                                contentColor = if (isPrimaryDay) palette.onPrimary else palette.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Indítás",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Edzés beállításai (from main Edzés screen)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                .clickable { onOpenProgressionModal() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙ Edzés beállításai • ${activeProgram.localizedName(state.language)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${state.trainingStyleHu} • ${state.progressionRuleHu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.primary
                )
            }
        }

        // 4. Bottom 3 shortcut tiles: Programok, Naptár, Coach (Mockup 1/4 #2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutSquareCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                title = "Programok",
                subtitle = "Edzéstervek",
                onClick = onOpenProgramsList
            )
            ShortcutSquareCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                title = "Naptár",
                subtitle = "Tervezés",
                onClick = onOpenCalendar
            )
            ShortcutSquareCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Psychology,
                title = "Coach",
                subtitle = "AI segítség",
                onClick = onOpenCoach
            )
        }
    }
}

@Composable
private fun ShortcutSquareCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
fun ProgramsListScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleCustomTab: (Boolean) -> Unit,
    onActivateProgram: (String) -> Unit,
    onOpenProgramEditor: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SubScreenTopBar(
            title = "Programok",
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            rightExtraContent = {
                IconButton(onClick = { onOpenProgramEditor(activeProgram.id) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Új program",
                        tint = palette.textPrimary
                    )
                }
            }
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Segmented Pill: Saját programok | Közösségi (Mockup 1/4 #3)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val tabs = listOf(true to "Saját programok", false to "Közösségi / Sablon")
                    tabs.forEach { (isCustom, label) ->
                        val selected = state.programsTabCustomOnly == isCustom
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) palette.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleCustomTab(isCustom) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) palette.onPrimary else palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Aktuális program",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )

            // Active Program Highlight Card (Mockup 1/4 #3 with custom ProgramTypeEmblemIcon)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary, RoundedCornerShape(18.dp))
                    .clickable { onOpenProgramEditor(activeProgram.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProgramTypeEmblemIcon(
                        programTitle = activeProgram.nameHu,
                        isHighlighted = true,
                        size = 54.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeProgram.localizedName(state.language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = activeProgram.localizedSubtitle(state.language),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = programEquipmentCategoryLabel(activeProgram.nameHu),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeProgram.days.size} edzésnap (A/B)",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.primary
                            ) {
                                Text(
                                    text = "Aktív",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }

            Text(
                text = "További programok",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )

            state.programs.filter { it.id != activeProgram.id }.forEach { program ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenProgramEditor(program.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgramTypeEmblemIcon(
                            programTitle = program.nameHu,
                            isHighlighted = false,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = program.localizedName(state.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = program.localizedSubtitle(state.language),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = programEquipmentCategoryLabel(program.nameHu),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onActivateProgram(program.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            )
                        ) {
                            Text("Aktiválás", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // + Saját program létrehozása button (Mockup 1/4 #3)
            OutlinedButton(
                onClick = { onOpenProgramEditor(activeProgram.id) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saját program létrehozása / szerkesztése",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primary
                )
            }

            OutlinedButton(
                onClick = onOpenExerciseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Gyakorlatok / Izomcsoportok (106 gyakorlat)",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
            }
        }
    }
}

@Composable
fun ProgramEditorScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDayIndex: (Int) -> Unit,
    onToggleExpandExercise: (String) -> Unit,
    onSaveExercisePrescription: (String, Int, Int, Float, Int) -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val program = state.programs.firstOrNull { it.id == state.editingProgramId } ?: state.programs.first()
    val dayIndex = state.editingDayIndex.coerceIn(0, (program.days.size - 1).coerceAtLeast(0))
    val currentDay = program.days.getOrNull(dayIndex)
    var programName by remember(program.id) { mutableStateOf(program.nameHu) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 4/4 #6 "Program szerkesztése"
        SubScreenTopBar(
            title = "Program szerkesztése",
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Program neve",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            OutlinedTextField(
                value = programName,
                onValueChange = { programName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Text(
                text = "Edzésnapok",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )

            // Segmented A nap | B nap selector (Mockup 4/4 #6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                program.days.forEachIndexed { idx, d ->
                    val selected = idx == dayIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) palette.primary else palette.cardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDayIndex(idx) }
                    ) {
                        Text(
                            text = d.dayCode,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) palette.onPrimary else palette.textPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            currentDay?.exercises?.forEachIndexed { index, ex ->
                val isExpanded = state.expandedEditorExerciseId == ex.id
                var sets by remember(ex.id, ex.defaultSets) { mutableIntStateOf(ex.defaultSets) }
                var reps by remember(ex.id, ex.defaultReps) { mutableIntStateOf(ex.defaultReps) }
                var weight by remember(ex.id, ex.defaultWeightKg) { mutableFloatStateOf(ex.defaultWeightKg) }
                var restSec by remember(ex.id, ex.defaultRestSec) { mutableIntStateOf(ex.defaultRestSec) }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isExpanded) 1.5.dp else 1.dp,
                            color = if (isExpanded) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleExpandExercise(ex.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ExercisePostureMiniIllustration(
                                exerciseName = ex.nameHu,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}.  ${ex.localizedName(state.language)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                val weightLabel = if (ex.defaultWeightKg > 0f) "${ex.defaultWeightKg.toInt()} kg" else "Testsúly"
                                Text(
                                    text = "${ex.defaultSets} × ${ex.defaultReps}  •  $weightLabel  •  ${ex.defaultRestSec} mp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Részletek",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                modifier = Modifier
                                    .clickable { onOpenExerciseDetail(ex.id) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Sorozat",
                                        valueText = "$sets",
                                        onMinus = { sets = (sets - 1).coerceAtLeast(1) },
                                        onPlus = { sets = (sets + 1).coerceAtMost(10) }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Ismétlés",
                                        valueText = "$reps",
                                        onMinus = { reps = (reps - 1).coerceAtLeast(1) },
                                        onPlus = { reps = (reps + 1).coerceAtMost(50) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Súly / kéz (kg)",
                                        valueText = "${weight.toInt()}",
                                        onMinus = { weight = (weight - 2f).coerceAtLeast(0f) },
                                        onPlus = { weight += 2f }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "Pihenő (mp)",
                                        valueText = "$restSec",
                                        onMinus = { restSec = (restSec - 15).coerceAtLeast(15) },
                                        onPlus = { restSec = (restSec + 15).coerceAtMost(300) }
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Eszköz: ${ex.equipmentHu}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleExpandExercise(ex.id) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Mégse", color = palette.textPrimary)
                                    }
                                    Button(
                                        onClick = {
                                            onSaveExercisePrescription(ex.id, sets, reps, weight, restSec)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary
                                        )
                                    ) {
                                        Text("Mentés", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // + Gyakorlat hozzáadása
            OutlinedButton(
                onClick = onOpenExerciseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gyakorlat hozzáadása", color = palette.textPrimary)
            }

            // Program törlése (DEMO)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.danger.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.danger.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable { onBack() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = palette.danger
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Program törlése (DEMO)",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.danger
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorStepperBox(
    modifier: Modifier = Modifier,
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Csökkentés",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Növelés",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarPlannerScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onOpenReplanModal: (Boolean) -> Unit,
    onApplyReplan: (String, String) -> Unit,
    onOpenWorkoutDetail: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val dayEvents = state.calendarEvents.filter { it.dayOfMonth == state.selectedCalendarDay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #4 "Naptár" + main "Edzések újratervezése"
        SubScreenTopBar(
            title = "Naptár",
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            rightExtraContent = {
                IconButton(onClick = { onOpenReplanModal(true) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Újratervezés",
                        tint = palette.textPrimary
                    )
                }
            }
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Calendar Card (2026. október)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                        Text(
                            text = "2026. október",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }

                    // Weekday headers H K Sz Cs P Szo V
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("H", "K", "Sz", "Cs", "P", "Szo", "V").forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 5 weeks grid for October
                    val weeks = listOf(
                        listOf(-28, -29, -30, 1, 2, 3, 4),
                        listOf(5, 6, 7, 8, 9, 10, 11),
                        listOf(12, 13, 14, 15, 16, 17, 18),
                        listOf(19, 20, 21, 22, 23, 24, 25),
                        listOf(26, 27, 28, 29, 30, 31, -2)
                    )
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayNum ->
                                val isCurrentMonth = dayNum > 0
                                val actualDay = kotlin.math.abs(dayNum)
                                val isSelected = isCurrentMonth && actualDay == state.selectedCalendarDay
                                val dayEvs = if (isCurrentMonth) {
                                    state.calendarEvents.filter { it.dayOfMonth == actualDay }
                                } else emptyList()

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .clickable(enabled = isCurrentMonth) {
                                            onSelectDay(actualDay)
                                        }
                                        .background(if (isSelected) palette.primary else Color.Transparent)
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$actualDay",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = when {
                                            isSelected -> palette.onPrimary
                                            isCurrentMonth -> palette.textPrimary
                                            else -> palette.textSecondary.copy(alpha = 0.4f)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayEvs.take(2).forEach { ev ->
                                            val dotColor = if (ev.status == CalendarEventStatus.COMPLETED) {
                                                palette.success
                                            } else {
                                                if (isSelected) palette.onPrimary else palette.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "2026. október ${state.selectedCalendarDay}.",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            if (dayEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "Erre a napra nincs tervezett edzés (Pihenőnap). Koppints az Újratervezés gombra új edzés ütemezéséhez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                dayEvents.forEach { ev ->
                    val isDone = ev.status == CalendarEventStatus.COMPLETED
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .clickable { onOpenWorkoutDetail() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDone) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.success,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = ev.timeLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.titleHu,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${ev.exerciseCount} gyakorlat • kb. ${ev.durationMinutes} perc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDone) palette.success.copy(alpha = 0.18f) else palette.primary.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = if (isDone) "Elvégezve" else "Tervezett",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDone) palette.success else palette.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
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

            // Main app Planning & Replanning card ("Újratervezés" + "Edzések megtervezése")
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Otthoni A/B – Alap (személyre szabott)",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onOpenReplanModal(true) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            )
                        ) {
                            Text("Újratervezés", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Tervezési ritmus",
                            value = state.planningCadenceHu
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Első edzésnap",
                            value = state.planningFirstDayHu
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Kezdőnap",
                            value = state.planningStartDate
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Időpont",
                            value = state.planningTime
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Időtartam (perc)",
                            value = "${state.planningDurationMin}"
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Hetek száma",
                            value = "${state.planningWeeksCount}"
                        )
                    }

                    Button(
                        onClick = { onApplyReplan("2026. 10. 28.", "Alap B") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Edzések megtervezése", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (state.showReplanModal) {
        AlertDialog(
            onDismissRequest = { onOpenReplanModal(false) },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text("Edzések újratervezése", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Bármelyik edzésnappal újratervezhetsz. Programváltáskor a hátralévő tervet az új program váltja fel. A naplózott és aktív edzések megmaradnak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    PlannerFieldTile(label = "Program", value = "Otthoni A/B – Alap (személyre szabott)")
                    PlannerFieldTile(label = "Első edzésnap", value = "Automatikus (Alap B)")
                    PlannerFieldTile(label = "Új kezdőnap", value = "2026. 10. 28.")
                    Text(
                        text = "• Alap B: 2026. 10. 26. → 2026. 10. 28.\n• Alap A: 2026. 10. 28. → 2026. 10. 30.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onApplyReplan("2026. 10. 28.", "Alap B") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text("Terv frissítése", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onOpenReplanModal(false) }) {
                    Text("Mégse", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun PlannerFieldTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    val palette = LocalTrainPilotPalette.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun ExerciseLibraryScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onSelectExercise: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val filtered = state.exercises.filter { ex ->
        val matchesCategory = state.selectedExerciseCategory == ExerciseCategory.ALL || ex.category == state.selectedExerciseCategory
        val matchesSearch = state.exerciseSearchQuery.isBlank() ||
            ex.nameHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.equipmentHu.contains(state.exerciseSearchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #5 "Gyakorlatok"
        SubScreenTopBar(
            title = "Gyakorlatok",
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.exerciseSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Gyakorlat keresése...", color = palette.textSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Horizontal category chips: Összes | Láb | Mell | Hát | Váll | Kar | Core
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseCategory.entries.forEach { cat ->
                    val selected = state.selectedExerciseCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) palette.primary else palette.cardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Text(
                            text = cat.localized(state.language),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) palette.onPrimary else palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Text(
                text = "106 beépített gyakorlat (${filtered.size} megjelenítve)",
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )

            filtered.forEach { ex ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        .clickable { onSelectExercise(ex.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExercisePostureMiniIllustration(
                            exerciseName = ex.nameHu,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ex.localizedName(state.language),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.border(
                                        1.dp,
                                        palette.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                ) {
                                    Text(
                                        text = ex.category.localized(state.language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ex.equipmentHu,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language))
                        Spacer(modifier = Modifier.width(6.dp))
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
}

@Composable
fun ExerciseDetailScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddExerciseToWorkout: (ExerciseItem) -> Unit,
    onStartDemoWithExercise: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val openVideoModal = LocalOpenExerciseVideo.current
    val exercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId } ?: state.exercises.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #6 "Gyakorlat részletei"
        SubScreenTopBar(
            title = "Gyakorlat részletei",
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.localizedName(state.language),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallExerciseVideoButton(exerciseName = exercise.localizedName(state.language))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primary.copy(alpha = 0.18f),
                        modifier = Modifier.border(1.dp, palette.primary, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = exercise.category.localized(state.language),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Anatomical / Exercise Figure Canvas Card (Mockup 1/4 #6)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx1 = size.width * 0.33f
                        val cx2 = size.width * 0.68f
                        val cy = size.height * 0.5f
                        // Left standing figure silhouette with highlighted target muscles
                        drawCircle(color = Color(0xFF94A3B8), radius = 16f, center = Offset(cx1, cy - 55f))
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(cx1, cy - 35f),
                            end = Offset(cx1, cy + 15f),
                            strokeWidth = 18f,
                            cap = StrokeCap.Round
                        )
                        // Highlighted primary muscle quads in warm orange/gold
                        drawLine(
                            color = Color(0xFFF97316),
                            start = Offset(cx1 - 8f, cy + 15f),
                            end = Offset(cx1 - 16f, cy + 62f),
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFF97316),
                            start = Offset(cx1 + 8f, cy + 15f),
                            end = Offset(cx1 + 16f, cy + 62f),
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )

                        // Right bottom-of-squat figure silhouette
                        drawCircle(color = Color(0xFF94A3B8), radius = 16f, center = Offset(cx2, cy - 25f))
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(cx2, cy - 5f),
                            end = Offset(cx2 - 6f, cy + 32f),
                            strokeWidth = 18f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFF97316),
                            start = Offset(cx2 - 6f, cy + 32f),
                            end = Offset(cx2 - 26f, cy + 28f),
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFF97316),
                            start = Offset(cx2 - 26f, cy + 28f),
                            end = Offset(cx2 - 18f, cy + 65f),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Metadata table: Eszköz, Fő izmok, Másodlagos izmok
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseMetaRow(label = "Eszköz", value = exercise.equipmentHu)
                    ExerciseMetaRow(label = "Fő izmok", value = exercise.primaryMusclesHu)
                    ExerciseMetaRow(label = "Másodlagos izmok", value = exercise.secondaryMusclesHu)
                }
            }

            // Dual Front & Back Anatomical Muscle Map highlighting the target muscle of this exercise
            val mappedMuscleId = when (exercise.category) {
                ExerciseCategory.LEGS -> "legs"
                ExerciseCategory.CHEST -> "chest"
                ExerciseCategory.BACK -> "back"
                ExerciseCategory.SHOULDERS -> "shoulders"
                ExerciseCategory.ARMS -> "arms"
                ExerciseCategory.CORE -> "core"
                ExerciseCategory.ALL -> "legs"
            }
            AnatomicalMuscleHeatmapCard(
                title = "Megdolgoztatott izmok (${exercise.localizedName(state.language)})",
                subtitle = "Fő izmok: ${exercise.primaryMusclesHu} • Másodlagos: ${exercise.secondaryMusclesHu}",
                highlightedMuscleId = mappedMuscleId
            )

            Text(
                text = "Végrehajtás",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            val steps = exercise.stepsHu.ifEmpty {
                listOf(
                    "Vedd fel a stabil kiinduló testhelyzetet.",
                    "Hajtsd végre a mozdulatot kontrollált tempóban.",
                    "Tartsd meg a feszülést, majd térj vissza a kiinduló helyzetbe."
                )
            }
            steps.forEachIndexed { idx, stepText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.surfaceElevated,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.primary
                            )
                        }
                    }
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { openVideoModal(exercise.localizedName(state.language)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("YouTube Videó", color = palette.textPrimary)
                }
                Button(
                    onClick = { onAddExerciseToWorkout(exercise) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hozzáadás az edzéshez", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetaRow(label: String, value: String) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary,
            modifier = Modifier.weight(0.58f)
        )
    }
}
