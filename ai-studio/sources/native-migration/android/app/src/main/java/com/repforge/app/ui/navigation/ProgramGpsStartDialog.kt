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
    val context = androidx.compose.ui.platform.LocalContext.current
    val text = remember(context, language) { com.repforge.app.ui.text.nativeResources(context, language) }
    val labels = listOf(com.repforge.app.R.string.program_start_title, com.repforge.app.R.string.program_gps_record,
        com.repforge.app.R.string.program_gps_help, com.repforge.app.R.string.program_start_action,
        com.repforge.app.R.string.program_start_cancel).map { text.getString(it) }
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
