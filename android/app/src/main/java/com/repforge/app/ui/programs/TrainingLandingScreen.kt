package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions
import com.repforge.app.ui.components.TrainPilotSurfaceCard
import com.repforge.app.ui.components.TrainPilotSectionHeader
import com.repforge.app.ui.components.TrainPilotPrimaryButton
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
        item {
            TrainPilotSurfaceCard(Modifier.fillMaxWidth()) {
                TrainPilotSectionHeader(labels[1])
                TrainPilotPrimaryButton(labels[2], { onQuick(false) }, Modifier.fillMaxWidth(), !busy)
                OutlinedButton(onClick = onLibrary, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.Control)) { Text(labels[9]) }
                OutlinedButton(onClick = { onQuick(true) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.Control)) { Text(labels[3]) }
            }
        }
        item { NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach) }
        item {
            TrainPilotSectionHeader(labels[4])
            Text(program?.program?.let { name(it.nameHu, it.nameEn, it.nameDe, it.nameRo) } ?: labels[7], color = colors.text)
            TextButton(onClick = onPrograms, enabled = !busy) { Text(labels[5]) }
        }
        items(program?.days.orEmpty().sortedBy { it.sortOrder }, key = { it.compositeId }) { day ->
            TrainPilotSurfaceCard(Modifier.fillMaxWidth()) {
                Text(name(day.nameHu, day.nameEn, day.nameDe, day.nameRo), color = colors.text,
                    style = MaterialTheme.typography.titleMedium)
                Text("${runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)} ${labels[8]}",
                    color = colors.muted, style = MaterialTheme.typography.bodySmall)
                TrainPilotPrimaryButton(labels[6], { onStart(day.programId, day.dayId) }, Modifier.fillMaxWidth(), !busy)
            }
        }
        item { Spacer(Modifier.height(TrainPilotDimensions.Page)) }
    }
}
