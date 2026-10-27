package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.navigation.NativePlanningShortcuts
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
@Composable
internal fun TrainingLandingScreen(language: String, activeProgramId: String, busy: Boolean,
    onStart: (String, String) -> Unit, onQuick: (Boolean) -> Unit, onPrograms: () -> Unit,
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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Studio's distinct quick-workout card, with the canonical native start command.
        item {
            TrainPilotHeroSurface(Modifier.fillMaxWidth().testTag("training_quick_section")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TrainPilotIconBadge(Icons.Default.FitnessCenter)
                    Column(Modifier.weight(1f)) {
                        Text(labels[1], style = MaterialTheme.typography.labelMedium,
                            color = colors.accentText, fontWeight = FontWeight.Bold)
                        Text(labels[2], style = MaterialTheme.typography.titleLarge, color = colors.text)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
                }
                TrainPilotPrimaryButton(labels[2], { onQuick(false) },
                    Modifier.fillMaxWidth().testTag("training_quick_workout"), !busy)
            }
        }
        // Two separate Studio action cards; never draw a fake GPS track/live badge.
        item {
            val stacked = LocalDensity.current.fontScale > 1.3f ||
                LocalConfiguration.current.screenWidthDp < 360
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainPilotActionSurface(labels[9], Icons.Default.MenuBook, onLibrary,
                    Modifier.fillMaxWidth().testTag("training_open_exercises"), !busy)
                TrainPilotActionSurface(labels[3], Icons.Default.DirectionsRun, { onQuick(true) },
                    Modifier.fillMaxWidth().testTag("training_outdoor_workout"), !busy)
            } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainPilotActionSurface(labels[9], Icons.Default.MenuBook, onLibrary,
                    Modifier.weight(1f).fillMaxHeight().testTag("training_open_exercises"), !busy)
                TrainPilotActionSurface(labels[3], Icons.Default.DirectionsRun, { onQuick(true) },
                    Modifier.weight(1f).fillMaxHeight().testTag("training_outdoor_workout"), !busy)
            }
        }
        item { NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach) }
        item {
            TrainPilotSectionSurface(Modifier.fillMaxWidth().testTag("training_current_program")) {
                TrainPilotSectionHeader(labels[4], icon = Icons.Default.FitnessCenter)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(program?.program?.let {
                        name(it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: labels[7],
                        modifier = Modifier.weight(1f),
                        color = colors.text, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onPrograms, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("training_open_programs")) {
                        Text(labels[5], color = colors.accentText,
                            style = MaterialTheme.typography.labelMedium)
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
        item { Spacer(Modifier.height(TrainPilotDimensions.Page)) }
    }
}
