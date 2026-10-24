package com.repforge.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources

/** Explicit redo/alternative-backup entry; never clears a stale fence just to enter the app. */
@Composable
internal fun ReplacementRecoveryPanel(language: String, busy: Boolean, error: Boolean,
    onResume: () -> Unit, onJson: (Uri) -> Unit, onZip: (Uri) -> Unit) {
    val context = LocalContext.current
    val text = remember(context, language) { nativeResources(context, language) }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onJson) }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onZip) }
    Column(Modifier.fillMaxWidth().padding(16.dp).testTag("replacement_recovery"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text.getString(R.string.replacement_title), style = MaterialTheme.typography.titleLarge)
        Text(text.getString(R.string.replacement_explanation))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error) Text(text.getString(R.string.replacement_error))
        Button(onClick = onResume, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(text.getString(R.string.replacement_resume))
        }
        OutlinedButton(onClick = { jsonPicker.launch(arrayOf("application/json", "*/*")) }, enabled = !busy) {
            Text(text.getString(R.string.replacement_json))
        }
        OutlinedButton(onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) }, enabled = !busy) {
            Text(text.getString(R.string.replacement_zip))
        }
    }
}
