package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.home.QuickWorkoutFilter
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions
import com.repforge.app.ui.components.TrainPilotActionSurface
import com.repforge.app.ui.components.TrainPilotHeroSurface
import com.repforge.app.ui.components.TrainPilotStatusBadge
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.components.TrainPilotSurfaceCard
import com.repforge.app.ui.components.TrainPilotSectionHeader
import com.repforge.app.ui.components.TrainPilotPrimaryButton
import com.repforge.app.ui.components.TrainPilotIconBadge
import org.json.JSONArray
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext

/** Main's Training entry shows quick start and active program days; Programs remains separate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainingLandingScreen(language: String, activeProgramId: String, busy: Boolean,
    onStart: (String, String) -> Unit, onQuick: (QuickWorkoutFilter, Boolean) -> Unit, onPrograms: () -> Unit,
    onCalendar: () -> Unit, onCoach: () -> Unit, onLibrary: () -> Unit,
    model: TrainingProgramsViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val labels = listOf(
        resources.getString(R.string.native_training_landing_00),
        resources.getString(R.string.native_training_landing_01),
        resources.getString(R.string.native_training_landing_02),
        resources.getString(R.string.native_training_landing_03),
        resources.getString(R.string.native_training_landing_04),
        resources.getString(R.string.native_training_landing_05),
        resources.getString(R.string.native_training_landing_06),
        resources.getString(R.string.native_training_landing_07),
        resources.getString(R.string.native_training_landing_08),
        resources.getString(R.string.native_training_landing_09)
    )
    fun name(hu: String, en: String, de: String, ro: String) = when(language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }
    val program = state.programs.firstOrNull { it.program.id == activeProgramId }
    var quickSheetOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize()
            .testTag("training_list")
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("training_quick_section"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = !busy, role = Role.Button) { quickSheetOpen = true }
                        .testTag("training_quick_workout")
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = null,
                        tint = colors.accentText, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(labels[1], style = MaterialTheme.typography.titleMedium, color = colors.text)
                        Text(resources.getString(R.string.studio_training_quick_subtitle),
                            style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                }
            }
        }

        item {
            val stacked = LocalDensity.current.fontScale > 1.3f ||
                LocalConfiguration.current.screenWidthDp < 360
            val exerciseCount = resources.getString(
                R.string.studio_training_exercise_count, state.exerciseChoices.size
            )
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudioTrainingActionTile(
                        resources.getString(R.string.studio_training_exercise_picker),
                        resources.getString(R.string.studio_training_exercise_subtitle),
                        exerciseCount, Icons.Default.MenuBook,
                        Modifier.fillMaxWidth().testTag("training_open_exercises"),
                        !busy, onLibrary
                    )
                    StudioTrainingActionTile(
                        resources.getString(R.string.studio_training_outdoor),
                        resources.getString(R.string.studio_training_outdoor_subtitle),
                        "GPS", Icons.Default.DirectionsRun,
                        Modifier.fillMaxWidth().testTag("training_outdoor_workout"),
                        !busy
                    ) { onQuick(QuickWorkoutFilter.ALL, true) }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioTrainingActionTile(
                        resources.getString(R.string.studio_training_exercise_picker),
                        resources.getString(R.string.studio_training_exercise_subtitle),
                        exerciseCount, Icons.Default.MenuBook,
                        Modifier.weight(1f).fillMaxHeight().testTag("training_open_exercises"),
                        !busy, onLibrary
                    )
                    StudioTrainingActionTile(
                        resources.getString(R.string.studio_training_outdoor),
                        resources.getString(R.string.studio_training_outdoor_subtitle),
                        "GPS", Icons.Default.DirectionsRun,
                        Modifier.weight(1f).fillMaxHeight().testTag("training_outdoor_workout"),
                        !busy
                    ) { onQuick(QuickWorkoutFilter.ALL, true) }
                }
            }
        }

        item {
            TrainPilotSectionSurface(Modifier.fillMaxWidth().testTag("training_current_program")) {
                TrainPilotSectionHeader(
                    resources.getString(R.string.studio_training_current_program),
                    icon = Icons.Default.FitnessCenter
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(program?.program?.let {
                        name(it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: labels[7], modifier = Modifier.weight(1f),
                        color = colors.text, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onPrograms, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("training_open_programs")) {
                        Text(resources.getString(R.string.studio_training_all),
                            color = colors.accentText, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        items(program?.days.orEmpty().sortedBy { it.sortOrder }, key = { it.compositeId }) { day ->
            TrainPilotSectionSurface(Modifier.fillMaxWidth().testTag("training_day_${day.dayId}")) {
                val dayName = name(day.nameHu, day.nameEn, day.nameDe, day.nameRo)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrainPilotStatusBadge(dayName.take(1))
                    Column(Modifier.weight(1f)) {
                        Text(dayName, color = colors.text, style = MaterialTheme.typography.titleMedium)
                        Text("${runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)} ${labels[8]}",
                            color = colors.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                }
                TrainPilotPrimaryButton(labels[6], { onStart(day.programId, day.dayId) },
                    Modifier.fillMaxWidth().testTag("training_start_day_${day.dayId}"), !busy)
            }
        }

        item { NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach) }
        item { Spacer(Modifier.height(TrainPilotDimensions.Page)) }
    }

    if (quickSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { quickSheetOpen = false },
            containerColor = colors.card,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            LazyColumn(
                Modifier.fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .testTag("training_quick_sheet")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            labels[1],
                            color = colors.text,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            resources.getString(R.string.studio_training_quick_subtitle),
                            color = colors.muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    StudioQuickChoiceRow(
                        name("Súlyzós", "Strength", "Krafttraining", "Forță"),
                        name(
                            "Erő- és ismétlésalapú gyakorlatok",
                            "Strength and repetition-based exercises",
                            "Kraft- und wiederholungsbasierte Übungen",
                            "Exerciții de forță și repetări"
                        ),
                        Icons.Default.FitnessCenter,
                        "training_quick_strength"
                    ) {
                        quickSheetOpen = false
                        onQuick(QuickWorkoutFilter.STRENGTH, false)
                    }
                }
                item {
                    StudioQuickChoiceRow(
                        name("Időalapú / intervallum", "Timed / interval", "Zeit / Intervall", "Cronometrat / interval"),
                        name(
                            "Időre mért gyakorlatok és intervallumok",
                            "Time-based exercises and intervals",
                            "Zeitbasierte Übungen und Intervalle",
                            "Exerciții cronometrate și intervale"
                        ),
                        Icons.Default.DirectionsRun,
                        "training_quick_timed"
                    ) {
                        quickSheetOpen = false
                        onQuick(QuickWorkoutFilter.TIMED, false)
                    }
                }
                item {
                    StudioQuickChoiceRow(
                        resources.getString(R.string.studio_training_outdoor),
                        resources.getString(R.string.studio_training_outdoor_subtitle),
                        Icons.Default.DirectionsRun,
                        "training_quick_outdoor_choice"
                    ) {
                        quickSheetOpen = false
                        onQuick(QuickWorkoutFilter.ALL, true)
                    }
                }
                item {
                    StudioQuickChoiceRow(
                        resources.getString(R.string.studio_training_exercise_picker),
                        resources.getString(R.string.studio_training_exercise_subtitle),
                        Icons.Default.MenuBook,
                        "training_quick_library_choice"
                    ) {
                        quickSheetOpen = false
                        onLibrary()
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun StudioQuickChoiceRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card2),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.accent.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.accentText,
                        modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
        }
    }
}

@Composable
private fun StudioTrainingActionTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), shape = RoundedCornerShape(11.dp), color = colors.card2) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = colors.accentText,
                            modifier = Modifier.size(24.dp))
                    }
                }
                Text(badge, style = MaterialTheme.typography.labelSmall,
                    color = colors.accentText, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(colors.accent.copy(alpha = .14f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = colors.text, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(subtitle, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall, color = colors.muted, maxLines = 2)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
            }
        }
    }
}

