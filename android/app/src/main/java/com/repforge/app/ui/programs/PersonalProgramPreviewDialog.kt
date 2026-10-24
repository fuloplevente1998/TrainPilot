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
    val profile = remember(profileJson) { runCatching { JSONObject(profileJson) }.getOrDefault(JSONObject()) }
    val recommended = if (profile.optString("experience") in setOf("intermediate", "advanced"))
        "home-level2" else "home-basic"
    var choice by remember(profileJson) { mutableStateOf(recommended) }
    val preview = previews[choice]
    val label = when (language) {
        "en" -> listOf("Personal A/B plan preview", "Basic", "Level 2", "Check your Training profile",
            "Set workout minutes and equipment first in Settings → Training profile. No exercise or weight is guessed.",
            "Work sets", "Minutes available", "Removed exercises", "Load: to be set with a trial set",
            "Accept and activate", "Cancel", "Could not save. Check that your profile or template has not changed.",
            "No safe workout for the selected profile.")
        "de" -> listOf("Persönlicher A/B-Plan – Vorschau", "Basis", "Stufe 2", "Trainingsprofil prüfen",
            "Trainingsdauer und Ausrüstung zuerst in Einstellungen → Trainingsprofil angeben.",
            "Arbeitssätze", "Minuten verfügbar", "Entfernte Übungen", "Gewicht: durch Probesatz bestimmen",
            "Annehmen und aktivieren", "Abbrechen", "Speichern fehlgeschlagen. Prüfe Profil und Vorlage.",
            "Kein geeignetes Training für dieses Profil.")
        "ro" -> listOf("Previzualizare program A/B", "De bază", "Nivel 2", "Verifică profilul",
            "Completează minutele și echipamentul în Setări → Profil antrenament.",
            "Serii", "Minute disponibile", "Exerciții eliminate", "Greutate: se stabilește printr-o serie de probă",
            "Acceptă și activează", "Anulează", "Salvarea a eșuat. Verifică profilul și șablonul.",
            "Niciun antrenament sigur pentru acest profil.")
        else -> listOf("Személyes A/B edzésterv előnézete", "Alap", "2. szint", "Edzésprofil ellenőrzése",
            "Előbb add meg az edzésidőt és felszerelést a Beállítások → Edzésprofil alatt. Nem találgatunk gyakorlatot vagy súlyt.",
            "Munkasorozatok", "Rendelkezésre álló perc", "Kihagyott gyakorlatok", "Súly: próbasorozattal beállítandó",
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
                    Text("${label[6]}: ${preview.estimateMinutes} · ${label[5]}: ${preview.workSets}")
                    val inherited = preview.history128.optJSONObject("entries")
                    if (inherited != null && inherited.length() > 0) {
                        Text(when (language) {
                            "en" -> "History-backed loads: ${inherited.length()} exercises"
                            "de" -> "Trainingshistorie: ${inherited.length()} Übungen"
                            "ro" -> "Istoric compatibil: ${inherited.length()} exerciții"
                            else -> "Kompatibilis edzéselőzmény: ${inherited.length()} gyakorlat"
                        }, modifier = Modifier.testTag("personal_history_summary"))
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
                            if (recipe.optBoolean("trial"))
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
                        preview.reductions.take(12).forEach { removed ->
                            Text("${removed.exerciseId} (${removed.reason})")
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
