package com.repforge.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.repforge.app.NativeIntervalPreset
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject

/** A draft of future defaults; never starts or reconfigures the active workout's timer. */
@Composable
internal fun NativeTimerDefaultsDialog(opened: String, language: String, saving: Boolean, writeFailed: Boolean,
    onDismiss: () -> Unit, onSave: (JSONObject) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    val source = JSONObject(opened)
    var work by rememberSaveable(opened) { mutableStateOf(source.getInt("work").toString()) }
    var rest by rememberSaveable(opened) { mutableStateOf(source.getInt("rest").toString()) }
    var rounds by rememberSaveable(opened) { mutableStateOf(source.getInt("rounds").toString()) }
    var sound by rememberSaveable(opened) { mutableStateOf(source.getBoolean("sound")) }
    var vibration by rememberSaveable(opened) { mutableStateOf(source.getBoolean("vibration")) }
    var auto by rememberSaveable(opened) { mutableStateOf(source.getBoolean("autoAdvance")) }
    val config = runCatching {
        NativeIntervalPreset.normalized(JSONObject().put("work", requireNotNull(work.toIntOrNull()))
            .put("rest", requireNotNull(rest.toIntOrNull())).put("rounds", requireNotNull(rounds.toIntOrNull()))
            .put("sound", sound).put("vibration", vibration).put("autoAdvance", auto))
    }.getOrNull()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() }, containerColor = colors.card,
        title = { Text(strings.getString(R.string.native_timer_defaults_title), color = colors.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.getString(R.string.native_timer_defaults_info), color = colors.muted)
                TimerNumber(work, { work = it }, R.string.native_timer_defaults_work, "work", !saving, language)
                TimerNumber(rest, { rest = it }, R.string.native_timer_defaults_rest, "rest", !saving, language)
                TimerNumber(rounds, { rounds = it }, R.string.native_timer_defaults_rounds, "rounds", !saving, language)
                TimerToggle(sound, { sound = it }, R.string.native_timer_defaults_sound, "sound", !saving, language)
                TimerToggle(vibration, { vibration = it }, R.string.native_timer_defaults_vibration, "vibration", !saving, language)
                TimerToggle(auto, { auto = it }, R.string.native_timer_defaults_auto, "auto", !saving, language)
                if (config == null) Text(strings.getString(R.string.native_timer_defaults_invalid), color = colors.dangerText,
                    modifier = Modifier.testTag("settings_timer_invalid"))
                if (writeFailed) Text(strings.getString(R.string.native_timer_defaults_failed), color = colors.dangerText,
                    modifier = Modifier.testTag("settings_timer_error"))
            }
        },
        confirmButton = {
            TextButton(onClick = { config?.let(onSave) }, enabled = !saving && config != null && !writeFailed,
                modifier = Modifier.testTag("settings_timer_save")) {
                Text(strings.getString(R.string.native_timer_defaults_save), color = colors.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.testTag("settings_timer_cancel")) {
                Text(strings.getString(R.string.native_settings_cancel), color = colors.text)
            }
        }
    )
}

@Composable
private fun TimerNumber(value: String, onChange: (String) -> Unit, label: Int, tag: String, enabled: Boolean, language: String) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= 6) onChange(it) },
        label = { Text(nativeResources(LocalContext.current, language).getString(label)) },
        enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("settings_timer_$tag"))
}

@Composable
private fun TimerToggle(value: Boolean, onChange: (Boolean) -> Unit, label: Int, tag: String, enabled: Boolean, language: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(nativeResources(LocalContext.current, language).getString(label), color = LocalTrainPilotColors.current.text,
            modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange, enabled = enabled,
            modifier = Modifier.testTag("settings_timer_$tag"))
    }
}
