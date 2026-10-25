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
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext
import android.content.res.Resources
import com.repforge.app.NativeProgramEditor
import com.repforge.app.NativeProgramPrescription
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
private fun words(resources: Resources) = ProgramEditorWords(
    resources.getString(R.string.native_program_editor_00),
    resources.getString(R.string.native_program_editor_01),
    resources.getString(R.string.native_program_editor_02),
    resources.getString(R.string.native_program_editor_03),
    resources.getString(R.string.native_program_editor_04),
    resources.getString(R.string.native_program_editor_05),
    resources.getString(R.string.native_program_editor_06),
    resources.getString(R.string.native_program_editor_07),
    resources.getString(R.string.native_program_editor_08),
    resources.getString(R.string.native_program_editor_09),
    resources.getString(R.string.native_program_editor_10),
    resources.getString(R.string.native_program_editor_11),
    resources.getString(R.string.native_program_editor_12),
    resources.getString(R.string.native_program_editor_13),
    resources.getString(R.string.native_program_editor_14),
    resources.getString(R.string.native_program_editor_15),
    resources.getString(R.string.native_program_editor_16),
    resources.getString(R.string.native_program_editor_17),
    resources.getString(R.string.native_program_editor_18)
)
private fun exerciseName(ex: ExerciseLibraryEntity, lang: String) = when (lang) {
    "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu
}.ifBlank { ex.nameHu }

private data class PrescriptionRequest(val dayId: String, val index: Int, val exerciseId: String)

@Composable
internal fun ProgramEditorDialog(
    opened: ProgramWithDays?, language: String, exercises: List<ExerciseLibraryEntity>, defaultRestSeconds: Int,
    saving: Boolean, error: String?, onSave: (NativeProgramEditor.Draft) -> Unit,
    onDelete: () -> Unit, onCancel: () -> Unit
) {
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val labels = words(resources)
    var draft by remember(opened?.program?.id, language) {
        mutableStateOf(NativeProgramEditor.from(opened, language).let { initial ->
            if (initial.days.isEmpty()) NativeProgramEditor.addDay(initial, "1") else initial
        })
    }
    var focusedDay by remember(opened?.program?.id) { mutableStateOf(draft.days.firstOrNull()?.id) }
    var search by remember { mutableStateOf("") }
    var prescriptionRequest by remember(opened?.program?.id, language) { mutableStateOf<PrescriptionRequest?>(null) }
    var deletionPending by remember { mutableStateOf(false) }
    val active = draft.days.firstOrNull { it.id == focusedDay }
    fun replaceDays(days: List<NativeProgramEditor.DayDraft>) {
        val selected = days.flatMap { it.entries.map(NativeProgramEditor::exerciseId) }.toSet()
        draft = draft.copy(days = days, prescriptionEdits = draft.prescriptionEdits.filterKeys { it in selected })
    }
    fun editDay(day: NativeProgramEditor.DayDraft) {
        replaceDays(draft.days.map { if (it.id == day.id) day else it })
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
                        replaceDays(draft.days.filterNot { it.id == active.id })
                        focusedDay = draft.days.firstOrNull()?.id
                    }, enabled = !saving, modifier = Modifier.testTag("program_editor_remove_day")) { Text(labels.remove) }
                    active.entries.forEachIndexed { ix, entry ->
                        val id = runCatching { NativeProgramEditor.exerciseId(entry) }.getOrDefault("?")
                        val name = exercises.firstOrNull { it.id == id }?.let { exerciseName(it, language) } ?: id
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            TextButton(onClick = { prescriptionRequest = PrescriptionRequest(active.id, ix, id) },
                                enabled = !saving && exercises.any { it.id == id },
                                modifier = Modifier.weight(1f).testTag("program_editor_rx_$ix")) {
                                Column {
                                    Text("${ix + 1}. $name")
                                    Text(resources.getString(R.string.program_rx_open))
                                }
                            }
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
                TextButton(onClick = onCancel, enabled = !saving, modifier = Modifier.testTag("program_editor_cancel")) { Text(labels.cancel) }
            }
        }
    )
    prescriptionRequest?.let { request ->
        val entry = draft.days.firstOrNull { it.id == request.dayId }?.entries?.getOrNull(request.index)
        val source = exercises.firstOrNull { it.id == request.exerciseId }
        if (entry != null && source != null && NativeProgramEditor.exerciseId(entry) == request.exerciseId) {
            val effective = NativeProgramPrescription.effective(opened?.program, entry, source,
                draft.prescriptionEdits, defaultRestSeconds)
            ProgramPrescriptionDialog(exerciseName(source, language), effective, language, saving,
                onApply = { patch ->
                    draft = draft.copy(prescriptionEdits = draft.prescriptionEdits + (request.exerciseId to
                        NativeProgramPrescription.Edit(patch.toString(), NativeProgramPrescription.source(source))))
                    prescriptionRequest = null
                }, onCancel = { prescriptionRequest = null })
        }
    }

}
