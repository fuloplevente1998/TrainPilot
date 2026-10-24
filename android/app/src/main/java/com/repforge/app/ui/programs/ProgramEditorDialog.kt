package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import com.repforge.app.NativeProgramEditor
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import java.util.Locale

private data class ProgramEditorWords(
    val titleNew: String, val titleEdit: String, val titleCopy: String, val name: String,
    val description: String, val addDay: String, val day: String, val remove: String,
    val up: String, val down: String, val exerciseSearch: String, val addExercise: String,
    val save: String, val cancel: String, val delete: String, val confirmDelete: String,
    val referencedWarning: String, val error: String, val cloneHint: String
)
private fun words(lang: String) = when (lang) {
    "en" -> ProgramEditorWords("New program", "Edit program", "Copy built-in program", "Program name",
        "Description", "Add day", "Day name", "Remove", "Up", "Down", "Search exercises",
        "Add exercise", "Save program", "Cancel", "Delete", "Confirm delete",
        "Programs and days referenced by history or calendar cannot be removed.",
        "Could not save changes. Reopen if another edit was saved.", "The built-in template stays unchanged.")
    "de" -> ProgramEditorWords("Neues Programm", "Programm bearbeiten", "Vorlage kopieren", "Programmname",
        "Beschreibung", "Tag hinzufügen", "Tagesname", "Entfernen", "Hoch", "Runter", "Übungen suchen",
        "Übung hinzufügen", "Programm speichern", "Abbrechen", "Löschen", "Löschen bestätigen",
        "Tage und Programme mit Trainings- oder Kalendereinträgen können nicht gelöscht werden.",
        "Änderung nicht gespeichert. Nach zwischenzeitlicher Änderung neu öffnen.", "Die Vorlage bleibt unverändert.")
    "ro" -> ProgramEditorWords("Program nou", "Editează program", "Copiază programul original", "Nume program",
        "Descriere", "Adaugă zi", "Nume zi", "Elimină", "Sus", "Jos", "Caută exerciții",
        "Adaugă exercițiu", "Salvează programul", "Anulează", "Șterge", "Confirmă ștergerea",
        "Zilele și programele folosite în jurnal sau calendar nu pot fi șterse.",
        "Modificările nu au fost salvate. Redeschide după o schimbare externă.", "Șablonul original rămâne neschimbat.")
    else -> ProgramEditorWords("Új program", "Program szerkesztése", "Beépített program másolása", "Program neve",
        "Leírás", "Edzésnap hozzáadása", "Edzésnap neve", "Eltávolítás",
        "Fel", "Le", "Gyakorlat keresése", "Gyakorlat hozzáadása", "Program mentése",
        "Mégse", "Törlés", "Törlés megerősítése",
        "Naplóban vagy naptárban használt program és edzésnap nem törölhető.",
        "Nem menthető. Ha másik eszközön változott, nyisd meg újra.",
        "A beépített program változatlan marad.")
}
private fun exerciseName(ex: ExerciseLibraryEntity, lang: String) = when (lang) {
    "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu
}.ifBlank { ex.nameHu }

@Composable
internal fun ProgramEditorDialog(
    opened: ProgramWithDays?, language: String, exercises: List<ExerciseLibraryEntity>,
    saving: Boolean, error: String?, onSave: (NativeProgramEditor.Draft) -> Unit,
    onDelete: () -> Unit, onCancel: () -> Unit
) {
    val labels = remember(language) { words(language) }
    var draft by remember(opened?.program?.id, language) {
        mutableStateOf(NativeProgramEditor.from(opened, language).let { initial ->
            if (initial.days.isEmpty()) NativeProgramEditor.addDay(initial, "1") else initial
        })
    }
    var focusedDay by remember(opened?.program?.id) { mutableStateOf(draft.days.firstOrNull()?.id) }
    var search by remember { mutableStateOf("") }
    var deletionPending by remember { mutableStateOf(false) }
    val active = draft.days.firstOrNull { it.id == focusedDay }
    fun editDay(day: NativeProgramEditor.DayDraft) {
        draft = draft.copy(days = draft.days.map { if (it.id == day.id) day else it })
    }
    val choices = remember(exercises, search, language) {
        val q = search.trim().lowercase(Locale.ROOT)
        exercises.filter { q.isEmpty() || listOf(it.nameHu, it.nameEn, it.nameDe, it.nameRo)
            .any { name -> name.lowercase(Locale.ROOT).contains(q) } }.take(80)
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(if (opened == null) labels.titleNew else if (opened.program.builtin)
            labels.titleCopy else labels.titleEdit) },
        text = {
            Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (opened?.program?.builtin == true) Text(labels.cloneHint)
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("program_editor_name"),
                    label = { Text(labels.name) })
                OutlinedTextField(draft.description, { draft = draft.copy(description = it) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    label = { Text(labels.description) })
                draft.days.forEachIndexed { index, day ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { focusedDay = day.id },
                            modifier = Modifier.weight(1f).testTag("program_editor_day_${day.id}")) {
                            Text("${index + 1}. ${day.name}")
                        }
                        TextButton(onClick = {
                            val copy = draft.days.toMutableList()
                            if (index > 0) {
                                copy.add(index - 1, copy.removeAt(index))
                                draft = draft.copy(days = copy)
                            }
                        }, enabled = index > 0 && !saving) { Text(labels.up) }
                        TextButton(onClick = {
                            val copy = draft.days.toMutableList()
                            if (index < copy.lastIndex) {
                                copy.add(index + 1, copy.removeAt(index))
                                draft = draft.copy(days = copy)
                            }
                        }, enabled = index < draft.days.lastIndex && !saving) { Text(labels.down) }
                    }
                }
                OutlinedButton(onClick = {
                    draft = NativeProgramEditor.addDay(draft, (draft.days.size + 1).toString())
                    focusedDay = draft.days.last().id
                }, enabled = draft.days.size < 31 && !saving,
                    modifier = Modifier.fillMaxWidth().testTag("program_editor_add_day")) { Text(labels.addDay) }
                if (active != null) {
                    OutlinedTextField(active.name, { editDay(active.copy(name = it)) },
                        modifier = Modifier.fillMaxWidth().testTag("program_editor_day_name"), singleLine = true,
                        label = { Text(labels.day) })
                    if (draft.days.size > 1) TextButton(onClick = {
                        draft = draft.copy(days = draft.days.filterNot { it.id == active.id })
                        focusedDay = draft.days.firstOrNull()?.id
                    }, enabled = !saving, modifier = Modifier.testTag("program_editor_remove_day")) { Text(labels.remove) }
                    active.entries.forEachIndexed { ix, entry ->
                        val id = runCatching { NativeProgramEditor.exerciseId(entry) }.getOrDefault("?")
                        val name = exercises.firstOrNull { it.id == id }?.let { exerciseName(it, language) } ?: id
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${ix + 1}. $name", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                editDay(active.copy(entries = NativeProgramEditor.move(active.entries, ix, -1)))
                            }, enabled = ix > 0 && !saving) { Text("↑") }
                            TextButton(onClick = {
                                editDay(active.copy(entries = NativeProgramEditor.move(active.entries, ix, 1)))
                            }, enabled = ix < active.entries.lastIndex && !saving) { Text("↓") }
                            TextButton(onClick = {
                                editDay(active.copy(entries = active.entries.filterIndexed { j, _ -> j != ix }))
                            }, enabled = !saving, modifier = Modifier.testTag("program_editor_remove_exercise_$ix")) {
                                Text("×")
                            }
                        }
                    }
                    OutlinedTextField(search, { search = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("program_editor_search"),
                        label = { Text(labels.exerciseSearch) })
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 148.dp).testTag("program_editor_choices")) {
                        items(choices, key = { it.id }) { exercise ->
                            TextButton(onClick = {
                                editDay(NativeProgramEditor.addExercise(active, exercise.id))
                            }, enabled = active.entries.size < 60 && !saving,
                                modifier = Modifier.fillMaxWidth().testTag("program_editor_add_exercise_${exercise.id}")) {
                                Text("+ ${exerciseName(exercise, language)}")
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(labels.error, modifier = Modifier.testTag("program_editor_error"))
                    Text(labels.referencedWarning)
                }
                if (opened != null && !opened.program.builtin && deletionPending) {
                    Text(labels.referencedWarning)
                    Button(onClick = onDelete, enabled = !saving,
                        modifier = Modifier.testTag("program_editor_confirm_delete")) { Text(labels.confirmDelete) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(draft) }, enabled = !saving,
                modifier = Modifier.testTag("program_editor_save")) { Text(labels.save) }
        },
        dismissButton = {
            Row {
                if (opened != null && !opened.program.builtin)
                    TextButton(onClick = { deletionPending = true }, enabled = !saving,
                        modifier = Modifier.testTag("program_editor_delete")) { Text(labels.delete) }
                TextButton(onClick = onCancel, enabled = !saving) { Text(labels.cancel) }
            }
        }
    )
}
