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
import android.content.res.Resources
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val names = listOf(
        resources.getString(R.string.native_profile_00),
        resources.getString(R.string.native_profile_01),
        resources.getString(R.string.native_profile_02),
        resources.getString(R.string.native_profile_03),
        resources.getString(R.string.native_profile_04),
        resources.getString(R.string.native_profile_05),
        resources.getString(R.string.native_profile_06),
        resources.getString(R.string.native_profile_07),
        resources.getString(R.string.native_profile_08),
        resources.getString(R.string.native_profile_09),
        resources.getString(R.string.native_profile_10),
        resources.getString(R.string.native_profile_11),
        resources.getString(R.string.native_profile_12),
        resources.getString(R.string.native_profile_13)
    )
    val title = resources.getString(R.string.native_profile_14)
    val emptyHint = resources.getString(R.string.native_profile_15)
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
                if (failed) Text(resources.getString(R.string.native_profile_16), modifier = Modifier.testTag("profile_write_error"))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(values) }, enabled = !saving,
                modifier = Modifier.testTag("profile_save")) {
                Text(resources.getString(R.string.native_profile_17))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(resources.getString(R.string.native_profile_18))
            }
        }
    )
}
