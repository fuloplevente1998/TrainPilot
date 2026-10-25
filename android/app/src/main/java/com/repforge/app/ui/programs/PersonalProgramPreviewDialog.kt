package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.repforge.app.NativeProfileProgramPreview
import com.repforge.app.NativeProgramEditor
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import org.json.JSONObject

/** Compact, read-only preview; only an explicit acceptance performs a write. */
@Composable
internal fun PersonalProgramPreviewDialog(
    programs: List<ProgramWithDays>,
    catalog: List<ExerciseLibraryEntity>,
    profileJson: String,
    previews: Map<String, NativeProfileProgramPreview.Preview>,
    loading: Boolean,
    language: String,
    saving: Boolean,
    saveError: String?,
    onAccept: (NativeProfileProgramPreview.Preview) -> Unit,
    onCancel: () -> Unit
) {
    val strings = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, language)
    fun exerciseName(id: String): String {
        val ex = catalog.firstOrNull { it.id == id } ?: return id
        return when (language) { "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu }.ifBlank { ex.nameHu }
    }
    fun reason(code: String): String = strings.getString(when (code) {
        "equipment" -> com.repforge.app.R.string.planner_reason_equipment
        "excluded" -> com.repforge.app.R.string.planner_reason_excluded
        "avoid-area" -> com.repforge.app.R.string.planner_reason_avoid
        "experience" -> com.repforge.app.R.string.planner_reason_experience
        "time-sets" -> com.repforge.app.R.string.planner_reason_sets
        "time" -> com.repforge.app.R.string.planner_reason_time
        else -> com.repforge.app.R.string.planner_reason_missing
    })
    val profile = remember(profileJson) { runCatching { JSONObject(profileJson) }.getOrDefault(JSONObject()) }
    val recommended = if (profile.optString("experience") == "intermediate")
        "home-level2" else "home-basic"
    var choice by remember(profileJson) { mutableStateOf(recommended) }
    val preview = previews[choice]
    val label = when (language) {
        "en" -> listOf("Personal A/B plan preview", "Basic", "Level 2", "Check your Training profile",
            "Set workout minutes and equipment first in Settings → Training profile. No exercise or weight is guessed.",
            "Work sets", "Estimated minutes / budget", "Plan adjustments", "Load: to be set with a trial set",
            "Accept and activate", "Cancel", "Could not save. Check that your profile or template has not changed.",
            "No safe workout for the selected profile.")
        "de" -> listOf("Persönlicher A/B-Plan – Vorschau", "Basis", "Stufe 2", "Trainingsprofil prüfen",
            "Trainingsdauer und Ausrüstung zuerst in Einstellungen → Trainingsprofil angeben.",
            "Arbeitssätze", "Geschätzte Minuten / Zeitbudget", "Plananpassungen", "Gewicht: durch Probesatz bestimmen",
            "Annehmen und aktivieren", "Abbrechen", "Speichern fehlgeschlagen. Prüfe Profil und Vorlage.",
            "Kein geeignetes Training für dieses Profil.")
        "ro" -> listOf("Previzualizare program A/B", "De bază", "Nivel 2", "Verifică profilul",
            "Completează minutele și echipamentul în Setări → Profil antrenament.",
            "Serii", "Minute estimate / buget", "Modificări ale planului", "Greutate: se stabilește printr-o serie de probă",
            "Acceptă și activează", "Anulează", "Salvarea a eșuat. Verifică profilul și șablonul.",
            "Niciun antrenament sigur pentru acest profil.")
        else -> listOf("Személyes A/B edzésterv előnézete", "Alap", "2. szint", "Edzésprofil ellenőrzése",
            "Előbb add meg az edzésidőt és felszerelést a Beállítások → Edzésprofil alatt. Nem találgatunk gyakorlatot vagy súlyt.",
            "Munkasorozatok", "Becsült perc / időkeret", "Tervmódosítások", "Súly: próbasorozattal beállítandó",
            "Elfogadás és aktiválás", "Mégse", "Nem menthető. Lehet, hogy közben változott a profil vagy a sablon.",
            "Nincs biztonsággal javasolható edzés ezzel a profillal.")
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(label[0]) },
        text = {
            Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState())
                .testTag("personal_preview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((id, name) in listOf("home-basic" to label[1], "home-level2" to label[2])) {
                        FilterChip(selected = choice == id, enabled = !saving,
                            onClick = { choice = id }, label = { Text(name) },
                            modifier = Modifier.testTag("personal_template_$id"))
                    }
                }
                if (loading) {
                    Text(when (language) {
                        "en" -> "Checking workout history…"; "de" -> "Trainingsverlauf wird geprüft…"
                        "ro" -> "Se verifică istoricul antrenamentelor…"
                        else -> "Edzéselőzmények ellenőrzése…"
                    }, modifier = Modifier.testTag("personal_preview_loading"))
                } else if (preview == null) {
                    Text(label[3])
                    Text(label[4], modifier = Modifier.testTag("personal_preview_blocked"))
                } else {
                    Text("${label[6]}: ${preview.estimateMinutes} / ${preview.profileSnapshot.getInt("minutes")} · ${label[5]}: ${preview.workSets}")
                    val inherited = preview.history128.optJSONObject("entries")
                    if (inherited != null && inherited.length() > 0) {
                        Text(when (language) {
                            "en" -> "History-backed loads: ${inherited.length()} exercises"
                            "de" -> "Trainingshistorie: ${inherited.length()} Übungen"
                            "ro" -> "Istoric compatibil: ${inherited.length()} exerciții"
                            else -> "Kompatibilis edzéselőzmény: ${inherited.length()} gyakorlat"
                        }, modifier = Modifier.testTag("personal_history_summary"))
                    }
                    preview.history128.optJSONObject("days")?.let { days ->
                        for (id in days.keys()) if (days.getJSONObject(id).optBoolean("retained")) {
                            val name = preview.draft.days.firstOrNull { it.id == id }?.name ?: id
                            Text(strings.getString(com.repforge.app.R.string.planner_retained_day, name),
                                modifier = Modifier.testTag("personal_retained_$id"))
                        }
                    }
                    preview.history128.optJSONObject("practiced")?.let { practiced ->
                        for (id in practiced.keys()) {
                            Text(strings.getString(com.repforge.app.R.string.planner_practiced_exercise, exerciseName(id)),
                                modifier = Modifier.testTag("personal_practiced_$id"))
                        }
                    }
                    preview.history128.optJSONArray("reductions")?.let { changes ->
                        for (i in 0 until changes.length()) {
                            val row = changes.getJSONObject(i)
                            val id = row.getString("dayId")
                            val name = preview.draft.days.firstOrNull { it.id == id }?.name ?: id
                            Text(strings.getString(com.repforge.app.R.string.planner_day_reduction, name,
                                row.getInt("fromExercises"), row.getInt("toExercises"), row.getInt("fromSets"), row.getInt("toSets"),
                                strings.getString(if (row.getString("reason") == "time") com.repforge.app.R.string.planner_day_time_reason
                                    else com.repforge.app.R.string.planner_day_equipment_reason)), modifier = Modifier.testTag("personal_day_reduction_$id"))
                        }
                    }
                    preview.draft.days.forEach { day ->
                        Text(day.name)
                        day.entries.forEach { entry ->
                            val id = NativeProgramEditor.exerciseId(entry)
                            val ex = catalog.firstOrNull { it.id == id }
                            val name = when (language) {
                                "en" -> ex?.nameEn; "de" -> ex?.nameDe
                                "ro" -> ex?.nameRo; else -> ex?.nameHu
                            }.orEmpty().ifBlank { ex?.nameHu ?: id }
                            val recipe = preview.prescriptions.getJSONObject(id)
                            Text("• $name — ${recipe.getInt("sets")} × ${recipe.getString("reps")}")
                            if (recipe.optString("variation").isNotBlank()) {
                                val text = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, language)
                                Text(com.repforge.app.ui.coach.NativeCoachAdviceText.variation(text,
                                    com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Variation(id,
                                        recipe.getString("variation"), recipe.optInt("progressionLevel"))),
                                    modifier = Modifier.testTag("personal_variation_$id"))
                            }
                            if (com.repforge.app.NativeTrainingPrescription.needsLoad(recipe))
                                Text(label[8], modifier = Modifier.testTag("personal_trial_$id"))
                            else if (recipe.optDouble("weight") > 0) {
                                Text(when (language) {
                                    "en" -> "Last completed load: ${recipe.optDouble("weight")} kg (no automatic increase)"
                                    "de" -> "Letztes Arbeitsgewicht: ${recipe.optDouble("weight")} kg (keine automatische Erhöhung)"
                                    "ro" -> "Ultima greutate: ${recipe.optDouble("weight")} kg (fără creștere automată)"
                                    else -> "Utolsó teljesített súly: ${recipe.optDouble("weight")} kg (nincs automatikus emelés)"
                                }, modifier = Modifier.testTag("personal_inherited_$id"))
                            }
                        }
                    }
                    if (preview.reductions.isNotEmpty()) {
                        Text("${label[7]}: ${preview.reductions.size}")
                        preview.reductions.forEach { removed ->
                            val name = exerciseName(removed.exerciseId)
                            val message = if (removed.reason.startsWith("substitution:")) {
                                strings.getString(com.repforge.app.R.string.planner_substitution, name,
                                    exerciseName(removed.reason.substringAfter(":"))) +
                                    if (removed.cause.isBlank()) "" else " (${reason(removed.cause)})"
                            } else "$name (${reason(removed.reason)})"
                            Text(message, modifier = Modifier.testTag("personal_change_${removed.dayId}_${removed.exerciseId}"))
                        }
                    }
                }
                if (saveError != null) Text(label[11], modifier = Modifier.testTag("personal_preview_error"))
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null && !saving && !loading,
                onClick = { preview?.let(onAccept) },
                modifier = Modifier.testTag("personal_preview_accept")) { Text(label[9]) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !saving) { Text(label[10]) }
        }
    )
}
