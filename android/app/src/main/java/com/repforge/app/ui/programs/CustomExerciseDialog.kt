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
import com.repforge.app.NativeCustomExerciseEditor
import com.repforge.app.data.db.ExerciseLibraryEntity

private fun formText(lang: String) = when (lang) {
    "en" -> listOf("New exercise", "Edit exercise", "Name", "Instructions", "Muscle group",
        "Measurement", "Load type", "Unit", "Sets", "Repetitions / time",
        "Weight (kg)", "Rest (s)", "YouTube watch URL (optional)", "Save",
        "Cancel", "Delete", "Delete this custom exercise?", "References in programs or history block deletion.",
        "Required fields or YouTube URL are invalid.",
        "Exercise changed elsewhere. Reopen it before retrying.", "Data restore in progress. Try again afterward.")
    "de" -> listOf("Neue Übung", "Übung bearbeiten", "Name", "Anleitung", "Muskelgruppe",
        "Messart", "Belastung", "Einheit", "Sätze", "Wiederholungen / Zeit",
        "Gewicht (kg)", "Pause (s)", "YouTube-Video-URL (optional)", "Speichern",
        "Abbrechen", "Löschen", "Eigene Übung löschen?", "Referenzen in Plänen oder Historie verhindern das Löschen.",
        "Ungültige Pflichtfelder oder YouTube-URL.",
        "Die Übung wurde anderswo geändert. Bitte erneut öffnen.", "Datenwiederherstellung läuft. Später erneut versuchen.")
    "ro" -> listOf("Exercițiu nou", "Editează exercițiul", "Nume", "Instrucțiuni", "Grupă musculară",
        "Măsurare", "Tip încărcare", "Unitate", "Serii", "Repetări / timp",
        "Greutate (kg)", "Pauză (s)", "URL videoclip YouTube (opțional)", "Salvează",
        "Anulează", "Șterge", "Ștergi acest exercițiu personalizat?", "Referințele din programe sau istoric împiedică ștergerea.",
        "Date obligatorii sau adresă YouTube invalidă.",
        "Exercițiul a fost modificat în altă parte. Redeschide-l.", "Restaurarea datelor este în curs. Încearcă mai târziu.")
    else -> listOf("Új gyakorlat", "Gyakorlat szerkesztése", "Név", "Leírás / végrehajtás",
        "Izomcsoport", "Mérés típusa", "Terhelés típusa", "Egység", "Sorozatok",
        "Ismétlés / idő", "Súly (kg)", "Pihenő (mp)", "YouTube-videó link (opcionális)",
        "Mentés", "Mégse", "Törlés", "Törlöd a saját gyakorlatot?",
        "Programban vagy naplóban használt gyakorlat nem törölhető.",
        "Érvénytelen kötelező mező vagy YouTube-hivatkozás.",
        "A gyakorlat közben módosult. Nyisd meg újra a szerkesztéshez.",
        "Adat-visszaállítás folyamatban. Próbáld meg később.")
}

@Composable
internal fun CustomExerciseDialog(
    exercise: ExerciseLibraryEntity?, language: String, saving: Boolean, error: String?,
    onSave: (NativeCustomExerciseEditor.Draft) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit
) {
    val words = remember(language) { formText(language) }
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
                        label = { Text(key) }, modifier = Modifier.testTag("custom_exercise_muscle_$key"))
                }
                Text(words[5])
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (key in measurements) FilterChip(selected = draft.measurement == key,
                        onClick = { draft = draft.copy(measurement = key) },
                        label = { Text(key) }, modifier = Modifier.testTag("custom_exercise_measure_$key"))
                }
                Text(words[6])
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (key in loads) FilterChip(selected = draft.load == key,
                        onClick = { draft = draft.copy(load = key) },
                        label = { Text(key) }, modifier = Modifier.testTag("custom_exercise_load_$key"))
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
