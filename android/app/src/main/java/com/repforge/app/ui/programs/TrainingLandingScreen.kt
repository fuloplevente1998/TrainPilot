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
import org.json.JSONArray

/** Main's Training entry shows quick start and active program days; Programs remains separate. */
@Composable
internal fun TrainingLandingScreen(language: String, activeProgramId: String, busy: Boolean,
    onStart: (String, String) -> Unit, onQuick: (Boolean) -> Unit, onPrograms: () -> Unit,
    onCalendar: () -> Unit, onCoach: () -> Unit,
    model: TrainingProgramsViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val labels = when(language) {
        "en" -> listOf("Training", "Quick workout", "Choose exercise", "Outdoor workout", "Active program", "Programs", "Start workout", "No active program.", "exercises")
        "de" -> listOf("Training", "Schnelltraining", "Übung wählen", "Training im Freien", "Aktives Programm", "Programme", "Training starten", "Kein aktives Programm.", "Übungen")
        "ro" -> listOf("Antrenament", "Antrenament rapid", "Alege exercițiul", "Antrenament în aer liber", "Program activ", "Programe", "Începe antrenamentul", "Niciun program activ.", "exerciții")
        else -> listOf("Edzés", "Gyors edzés", "Gyakorlat választása", "Szabadtéri edzés", "Aktív program", "Programok", "Edzés indítása", "Nincs aktív program.", "gyakorlat")
    }
    fun name(hu: String, en: String, de: String, ro: String) = when(language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }
    val program = state.programs.firstOrNull { it.program.id == activeProgramId }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(labels[1], color = colors.text)
                    Button(onClick = { onQuick(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(labels[2]) }
                    OutlinedButton(onClick = { onQuick(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(labels[3]) }
                }
            }
        }
        item {
            Text(labels[4], color = colors.muted)
            Text(program?.program?.let { name(it.nameHu, it.nameEn, it.nameDe, it.nameRo) } ?: labels[7], color = colors.text)
            TextButton(onClick = onPrograms, enabled = !busy) { Text(labels[5]) }
        }
        items(program?.days.orEmpty().sortedBy { it.sortOrder }, key = { it.compositeId }) { day ->
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name(day.nameHu, day.nameEn, day.nameDe, day.nameRo), color = colors.text)
                    Text("${runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)} ${labels[8]}", color = colors.muted)
                    Button(onClick = { onStart(day.programId, day.dayId) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(labels[6]) }
                }
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}
