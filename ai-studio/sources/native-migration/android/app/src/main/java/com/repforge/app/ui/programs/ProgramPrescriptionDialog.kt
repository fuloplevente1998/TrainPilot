package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.repforge.app.NativeProgramPrescription
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject

/** Edits only the parent's isolated program draft; the outer program Save remains the commit point. */
@Composable
internal fun ProgramPrescriptionDialog(name: String, initial: JSONObject, language: String,
    saving: Boolean, onApply: (JSONObject) -> Unit, onCancel: () -> Unit) {
    val strings = nativeResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    var fields by remember(initial.toString()) { mutableStateOf(NativeProgramPrescription.fields(initial)) }
    var invalid by remember { mutableStateOf(false) }
    val loadType = initial.getString("loadType")
    val targetLabel = when {
        initial.optString("repUnit").contains("/oldal") -> R.string.program_rx_target_side
        initial.optString("measurementType") in setOf("time", "distance") ||
            initial.optString("repUnit") in setOf("mp", "sec", "s") -> R.string.program_rx_target_time
        else -> R.string.program_rx_target_reps
    }
    AlertDialog(onDismissRequest = { if (!saving) onCancel() },
        title = { Text(strings.getString(R.string.program_rx_title, name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.getString(R.string.program_rx_scope), color = colors.muted)
                OutlinedTextField(fields.sets, { fields = fields.copy(sets = it); invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("program_rx_sets"), singleLine = true, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(strings.getString(R.string.program_rx_sets)) })
                OutlinedTextField(fields.reps, { fields = fields.copy(reps = it); invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("program_rx_reps"), singleLine = true, enabled = !saving,
                    label = { Text(strings.getString(targetLabel)) })
                if (loadType != "bodyweight") {
                    val weightLabel = when (loadType) {
                        "per_hand" -> R.string.program_rx_weight_hand
                        "total" -> R.string.program_rx_weight_total
                        "single_dumbbell" -> R.string.program_rx_weight_single
                        "assisted" -> R.string.program_rx_weight_assisted
                        else -> R.string.program_rx_weight
                    }
                    OutlinedTextField(fields.weight, { fields = fields.copy(weight = it); invalid = false },
                        modifier = Modifier.fillMaxWidth().testTag("program_rx_weight"), singleLine = true, enabled = !saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = { Text(strings.getString(weightLabel)) })
                    if (NativeProgramPrescription.allowsTrial(loadType)) Text(strings.getString(R.string.program_rx_trial), color = colors.muted)
                }
                OutlinedTextField(fields.rest, { fields = fields.copy(rest = it); invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("program_rx_rest"), singleLine = true, enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(strings.getString(R.string.program_rx_rest)) })
                if (invalid) Text(strings.getString(R.string.program_rx_invalid), color = colors.dangerText,
                    modifier = Modifier.testTag("program_rx_error"))
            }
        },
        confirmButton = {
            Button(onClick = {
                val patch = runCatching { NativeProgramPrescription.edit(fields, loadType) }.getOrNull()
                if (patch == null) invalid = true else onApply(patch)
            }, enabled = !saving, modifier = Modifier.testTag("program_rx_apply")) {
                Text(strings.getString(R.string.program_rx_apply))
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !saving,
            modifier = Modifier.testTag("program_rx_cancel")) { Text(strings.getString(R.string.program_rx_cancel)) } },
        containerColor = colors.card)
}
