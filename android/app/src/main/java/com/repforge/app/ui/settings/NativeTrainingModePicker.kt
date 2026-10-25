package com.repforge.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Global and active-program editors share one read-only preview/explicit-approval UI. */
@Composable
internal fun NativeTrainingModePicker(
    language: String, raw: String?, activeProgramId: String,
    scope: String, programId: String? = null, enabled: Boolean = true,
    onApprove: (NativeTrainingModeChange.Preview) -> Unit,
    onUndo: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val ctx = LocalContext.current
    val strings = remember(ctx, language) { nativeResources(ctx, language) }
    val effective = remember(raw, scope, programId, activeProgramId) {
        NativeTrainingModes.resolveSavedJson(raw, if (scope == "global") activeProgramId else programId)
    }
    var style by remember(raw, scope, programId) { mutableStateOf(effective.trainingStyle) }
    var progression by remember(raw, scope, programId) { mutableStateOf(effective.progressionMode) }
    var pending by remember(raw, scope, programId) { mutableStateOf<NativeTrainingModeChange.Preview?>(null) }
    var styleExpanded by remember { mutableStateOf(false) }
    var progressionExpanded by remember { mutableStateOf(false) }
    val undoAllowed = remember(raw, scope, programId) {
        NativeTrainingModeChange.undoValue(raw, scope, programId) != null
    }
    fun styleLabel(value: String): String = strings.getString(if (value == "hit")
        R.string.native_modes_style_hit else R.string.native_modes_style_normal)
    fun progressionLabel(value: String): String = strings.getString(when (value) {
        "linear" -> R.string.native_modes_progression_linear
        "rir" -> R.string.native_modes_progression_rir
        else -> R.string.native_modes_progression_double
    })
    val tag = if (scope == "global") "training_modes_global" else "training_modes_program"

    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.getString(R.string.native_modes_title), color = colors.text, fontWeight = FontWeight.Bold)
            Text(strings.getString(R.string.native_modes_help), color = colors.muted)
            Text(strings.getString(R.string.native_modes_style), color = colors.text)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { styleExpanded = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tag}_style")) {
                    Text(styleLabel(style), color = colors.text)
                }
                DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                    for (option in listOf("normal", "hit")) {
                        DropdownMenuItem(text = { Text(styleLabel(option)) },
                            onClick = { style = option; pending = null; styleExpanded = false },
                            modifier = Modifier.testTag("${tag}_style_$option"))
                    }
                }
            }
            Text(strings.getString(R.string.native_modes_progression), color = colors.text)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { progressionExpanded = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tag}_progression")) {
                    Text(progressionLabel(progression), color = colors.text)
                }
                DropdownMenu(expanded = progressionExpanded, onDismissRequest = { progressionExpanded = false }) {
                    for (option in listOf("double", "linear", "rir")) {
                        DropdownMenuItem(text = { Text(progressionLabel(option)) },
                            onClick = { progression = option; pending = null; progressionExpanded = false },
                            modifier = Modifier.testTag("${tag}_progression_$option"))
                    }
                }
            }
            Text(
                strings.getString(if (effective.explicitChoice) R.string.native_modes_selected
                    else R.string.native_modes_legacy),
                color = colors.muted
            )
            Text(strings.getString(R.string.native_modes_notice), color = colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pending = NativeTrainingModeChange.prepare(
                            raw, activeProgramId, scope, programId, style, progression)
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tag}_preview")
                ) { Text(strings.getString(R.string.native_modes_preview)) }
                if (undoAllowed) {
                    TextButton(onClick = onUndo, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tag}_undo")) {
                        Text(strings.getString(R.string.native_modes_undo), color = colors.accentText)
                    }
                }
            }
        }
    }
    pending?.let { preview ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(strings.getString(R.string.native_modes_confirm_title), color = colors.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.getString(R.string.native_modes_before,
                        styleLabel(preview.before.trainingStyle),
                        progressionLabel(preview.before.progressionMode)), color = colors.text)
                    Text(strings.getString(R.string.native_modes_after,
                        styleLabel(preview.after.trainingStyle),
                        progressionLabel(preview.after.progressionMode)), color = colors.text)
                    Text(strings.getString(R.string.native_modes_no_rewrite), color = colors.muted)
                }
            },
            confirmButton = {
                Button(onClick = { pending = null; onApprove(preview) }, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_approve")) {
                    Text(strings.getString(R.string.native_modes_approve))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }, modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_cancel")) {
                    Text(strings.getString(R.string.native_modes_cancel))
                }
            },
            containerColor = colors.card
        )
    }
}
