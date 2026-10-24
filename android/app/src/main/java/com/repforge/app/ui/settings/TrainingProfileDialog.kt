package com.repforge.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import org.json.JSONObject

/**
 * Edits only the scalar profile fields. Equipment, exclusion and focus arrays
 * remain inside the original #128 JSON, untouched by this compact form.
 */
@Composable
internal fun TrainingProfileDialog(
    profileJson: String, language: String, saving: Boolean, failed: Boolean,
    onSave: (Map<String, String>) -> Unit, onDismiss: () -> Unit
) {
    val source = remember(profileJson) { runCatching { JSONObject(profileJson) }.getOrDefault(JSONObject()) }
    val fields = listOf("age", "height", "weight", "minutes",
        "goal", "experience", "activity", "location", "cadence", "split", "focus",
        "gear", "excluded", "avoidAreas")
    val names = when (language) {
        "en" -> listOf("Age", "Height (cm)", "Weight (kg)", "Minutes per workout", "Goal",
            "Experience", "Activity", "Location", "Schedule", "Split", "Focus",
            "Equipment keys (comma-separated)", "Excluded exercise IDs", "Avoided muscle groups")
        "de" -> listOf("Alter", "Größe (cm)", "Gewicht (kg)", "Minuten je Training", "Ziel",
            "Erfahrung", "Aktivität", "Ort", "Rhythmus", "Aufteilung", "Schwerpunkt",
            "Ausrüstung (durch Komma getrennt)", "Ausgeschlossene Übungen", "Zu vermeidende Muskelgruppen")
        "ro" -> listOf("Vârstă", "Înălțime (cm)", "Greutate (kg)", "Minute pe antrenament", "Obiectiv",
            "Experiență", "Activitate", "Loc", "Ritm", "Împărțire", "Accent",
            "Echipament (separat prin virgulă)", "Exerciții excluse", "Grupe musculare de evitat")
        else -> listOf("Életkor", "Magasság (cm)", "Testsúly (kg)", "Edzésidő (perc)", "Cél",
            "Tapasztalat", "Aktivitás", "Helyszín", "Edzésritmus", "Felosztás", "Fókusz",
            "Felszerelés (pl. dumbbells, barbell)", "Kizárt gyakorlat-azonosítók", "Kerülendő izomcsoportok")
    }
    val title = when (language) {
        "en" -> "Training profile"; "de" -> "Trainingsprofil"
        "ro" -> "Profil de antrenament"; else -> "Edzésprofil"
    }
    val emptyHint = when (language) {
        "en" -> "Empty fields are not guessed. Equipment and exclusions change only when you edit them."
        "de" -> "Leere Felder werden nicht geschätzt. Ausrüstung und Ausschlüsse ändern sich nur durch deine Eingabe."
        "ro" -> "Câmpurile goale nu sunt deduse. Echipamentul și excluderile se schimbă doar prin editare."
        else -> "Az üres mezőket nem találgatjuk. A felszerelés és kizárások csak külön módosításkor változnak."
    }
    var values by remember(profileJson) {
        mutableStateOf(fields.associateWith { k ->
            if (source.isNull(k)) "" else source.optJSONArray(k)?.let { rows ->
                (0 until rows.length()).joinToString(", ") { rows.optString(it) }
            } ?: source.opt(k)?.toString().orEmpty()
        })
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emptyHint)
                fields.forEachIndexed { i, key ->
                    OutlinedTextField(value = values[key].orEmpty(),
                        onValueChange = { values = values + (key to it) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_field_$key"),
                        label = { Text(names[i]) }, singleLine = true)
                }
                if (failed) Text(when (language) {
                    "en" -> "Cannot save: invalid field or newer profile. Reopen and retry."
                    "de" -> "Speichern fehlgeschlagen: ungültiges Feld oder neueres Profil."
                    "ro" -> "Salvare eșuată: câmp invalid sau profil modificat."
                    else -> "Nem menthető: hibás mező vagy közben módosult profil. Nyisd meg újra."
                }, modifier = Modifier.testTag("profile_write_error"))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(values) }, enabled = !saving,
                modifier = Modifier.testTag("profile_save")) {
                Text(when (language) {
                    "en" -> "Save"; "de" -> "Speichern"; "ro" -> "Salvează"; else -> "Mentés"
                })
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(when (language) {
                    "en" -> "Cancel"; "de" -> "Abbrechen"; "ro" -> "Anulează"; else -> "Mégse"
                })
            }
        }
    )
}
