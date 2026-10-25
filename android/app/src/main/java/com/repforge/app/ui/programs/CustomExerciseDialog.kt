package com.repforge.app.ui.programs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext
import android.content.res.Resources
import com.repforge.app.NativeCustomExerciseEditor
import com.repforge.app.data.db.ExerciseLibraryEntity

private fun formText(resources: Resources): List<String> = listOf(
    resources.getString(R.string.native_custom_exercise_00),
    resources.getString(R.string.native_custom_exercise_01),
    resources.getString(R.string.native_custom_exercise_02),
    resources.getString(R.string.native_custom_exercise_03),
    resources.getString(R.string.native_custom_exercise_04),
    resources.getString(R.string.native_custom_exercise_05),
    resources.getString(R.string.native_custom_exercise_06),
    resources.getString(R.string.native_custom_exercise_07),
    resources.getString(R.string.native_custom_exercise_08),
    resources.getString(R.string.native_custom_exercise_09),
    resources.getString(R.string.native_custom_exercise_10),
    resources.getString(R.string.native_custom_exercise_11),
    resources.getString(R.string.native_custom_exercise_12),
    resources.getString(R.string.native_custom_exercise_13),
    resources.getString(R.string.native_custom_exercise_14),
    resources.getString(R.string.native_custom_exercise_15),
    resources.getString(R.string.native_custom_exercise_16),
    resources.getString(R.string.native_custom_exercise_17),
    resources.getString(R.string.native_custom_exercise_18),
    resources.getString(R.string.native_custom_exercise_19),
    resources.getString(R.string.native_custom_exercise_20)
)

@Composable
internal fun CustomExerciseDialog(
    exercise: ExerciseLibraryEntity?, language: String, saving: Boolean, error: String?,
    onSave: (NativeCustomExerciseEditor.Draft) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit
) {
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val words = formText(resources)
    var draft by remember(exercise?.id, language) {
        mutableStateOf(NativeCustomExerciseEditor.draft(exercise, language))
    }
    var setsText by remember(exercise?.id) { mutableStateOf(draft.sets.toString()) }
    var weightText by remember(exercise?.id) { mutableStateOf(draft.weight.toString()) }
    var restText by remember(exercise?.id) { mutableStateOf(draft.restSeconds.toString()) }
    var deleteConfirm by remember(exercise?.id) { mutableStateOf(false) }
    val muscles = listOf("chest", "back", "shoulders", "arms", "core", "legs", "cardio", "full")
    val measurements = listOf("reps", "time", "distance", "steps", "strength")
    val loads = listOf("bodyweight", "weight", "dumbbell", "barbell", "single_dumbbell",
        "total", "machine", "cable")
    val muscleLabels = listOf(
        resources.getString(R.string.native_custom_chip_muscle_chest),
        resources.getString(R.string.native_custom_chip_muscle_back),
        resources.getString(R.string.native_custom_chip_muscle_shoulders),
        resources.getString(R.string.native_custom_chip_muscle_arms),
        resources.getString(R.string.native_custom_chip_muscle_core),
        resources.getString(R.string.native_custom_chip_muscle_legs),
        resources.getString(R.string.native_custom_chip_muscle_cardio),
        resources.getString(R.string.native_custom_chip_muscle_full)
    )
    val measureLabels = listOf(
        resources.getString(R.string.native_custom_chip_measure_reps),
        resources.getString(R.string.native_custom_chip_measure_time),
        resources.getString(R.string.native_custom_chip_measure_distance),
        resources.getString(R.string.native_custom_chip_measure_steps),
        resources.getString(R.string.native_custom_chip_measure_strength)
    )
    val loadLabels = listOf(
        resources.getString(R.string.native_custom_chip_load_bodyweight),
        resources.getString(R.string.native_custom_chip_load_weight),
        resources.getString(R.string.native_custom_chip_load_dumbbell),
        resources.getString(R.string.native_custom_chip_load_barbell),
        resources.getString(R.string.native_custom_chip_load_single_dumbbell),
        resources.getString(R.string.native_custom_chip_load_total),
        resources.getString(R.string.native_custom_chip_load_machine),
        resources.getString(R.string.native_custom_chip_load_cable)
    )
    AlertDialog(onDismissRequest = { if (!saving) onCancel() },
        title = { Text(if (exercise == null) words[0] else words[1]) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) },
                    label = { Text(words[2]) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_exercise_name"))
                OutlinedTextField(draft.note, { draft = draft.copy(note = it) },
                    label = { Text(words[3]) }, minLines = 2, maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("custom_exercise_note"))
                Text(words[4])
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (key in muscles) FilterChip(selected = draft.muscle == key,
                        onClick = { draft = draft.copy(muscle = key) },
                        label = { Text(muscleLabels[muscles.indexOf(key)]) }, modifier = Modifier.testTag("custom_exercise_muscle_$key"))
                }
                Text(words[5])
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (key in measurements) FilterChip(selected = draft.measurement == key,
                        onClick = { draft = draft.copy(measurement = key) },
                        label = { Text(measureLabels[measurements.indexOf(key)]) }, modifier = Modifier.testTag("custom_exercise_measure_$key"))
                }
                Text(words[6])
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (key in loads) FilterChip(selected = draft.load == key,
                        onClick = { draft = draft.copy(load = key) },
                        label = { Text(loadLabels[loads.indexOf(key)]) }, modifier = Modifier.testTag("custom_exercise_load_$key"))
                }
                OutlinedTextField(draft.repUnit, { draft = draft.copy(repUnit = it) },
                    label = { Text(words[7]) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(setsText, { setsText = it }, label = { Text(words[8]) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.reps, { draft = draft.copy(reps = it) },
                    label = { Text(words[9]) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weightText, { weightText = it }, label = { Text(words[10]) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(restText, { restText = it }, label = { Text(words[11]) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.videoUrl, { draft = draft.copy(videoUrl = it) },
                    label = { Text(words[12]) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_exercise_video"))
                if (error != null) {
                    val message = when {
                        error.contains("changed", ignoreCase = true) -> words[19]
                        error.contains("history", ignoreCase = true) || error.contains("program", ignoreCase = true) ||
                            error.contains("active workout", ignoreCase = true) -> words[17]
                        error.contains("Adatcsere", ignoreCase = true) || error.contains("lecserődtek", ignoreCase = true) -> words[20]
                        else -> words[18]
                    }
                    Text(message, modifier = Modifier.testTag("custom_exercise_error"))
                }
                if (exercise != null && deleteConfirm) {
                    Text(words[16]); Text(words[17])
                    TextButton(onClick = onDelete, enabled = !saving,
                        modifier = Modifier.testTag("custom_exercise_confirm_delete")) { Text(words[15]) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(draft.copy(sets = setsText.toIntOrNull() ?: -1,
                    weight = weightText.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                    restSeconds = restText.toIntOrNull() ?: -1))
            }, enabled = !saving, modifier = Modifier.testTag("custom_exercise_save")) { Text(words[13]) }
        },
        dismissButton = {
            Row {
                if (exercise != null) TextButton(onClick = { deleteConfirm = true }, enabled = !saving,
                    modifier = Modifier.testTag("custom_exercise_delete")) { Text(words[15]) }
                TextButton(onClick = onCancel, enabled = !saving) { Text(words[14]) }
            }
        })
}
