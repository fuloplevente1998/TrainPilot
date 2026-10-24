package com.repforge.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.NativeProgramGpsChoice

@Composable
internal fun ProgramGpsStartDialog(option: NativeProgramGpsChoice.Option, language: String,
    onCancel: () -> Unit, onStart: (Boolean) -> Unit) {
    val labels = when (language) {
        "en" -> listOf("Start workout", "Record GPS route", "Your choice is remembered for this activity. Location permission and device location are required.", "Start", "Cancel")
        "de" -> listOf("Training starten", "GPS-Route aufzeichnen", "Die Auswahl wird für diese Aktivität gespeichert. Standortberechtigung und Gerätestandort sind erforderlich.", "Starten", "Abbrechen")
        "ro" -> listOf("Începe antrenamentul", "Înregistrează traseul GPS", "Alegerea este memorată pentru această activitate. Sunt necesare permisiunea și locația dispozitivului.", "Începe", "Anulează")
        else -> listOf("Edzés indítása", "GPS-útvonal rögzítése", "A választást megjegyezzük ehhez az aktivitáshoz. Pontos helyengedély és bekapcsolt rendszerhely szükséges.", "Indítás", "Mégse")
    }
    var record by remember(option) { mutableStateOf(option.remembered) }
    AlertDialog(onDismissRequest = onCancel, title = { Text(labels[0]) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(option.name)
            Row(Modifier.fillMaxWidth()) {
                Text(labels[1], Modifier.weight(1f))
                Switch(record, onCheckedChange = { record = it }, modifier = Modifier.testTag("program_start_gps"))
            }
            Text(labels[2])
        }
    }, confirmButton = { TextButton(onClick = { onStart(record) }, modifier = Modifier.testTag("program_start_confirm")) { Text(labels[3]) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(labels[4]) } })
}
