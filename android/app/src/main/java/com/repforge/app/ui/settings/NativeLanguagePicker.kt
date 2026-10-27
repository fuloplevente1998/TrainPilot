package com.repforge.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Autonyms stay recognizable even after an accidental language change; selection uses DataStore. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativeLanguagePicker(language: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    Card(Modifier.fillMaxWidth().testTag("settings_language_picker"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.getString(R.string.native_language_title), color = colors.text,
                style = MaterialTheme.typography.titleLarge)
            Text(strings.getString(R.string.native_language_hint), color = colors.muted,
                style = MaterialTheme.typography.bodyMedium)
            FlowRow(Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((code, label) in listOf("hu" to "Magyar", "en" to "English", "de" to "Deutsch", "ro" to "Română")) {
                    val selected = language == code
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = if (selected) colors.selectedSurface else colors.card,
                        border = BorderStroke(if (selected) 2.dp else 1.dp,
                            if (selected) colors.selectedBorder else colors.line)) {
                        Column(Modifier.heightIn(min = 48.dp).testTag("settings_language_$code")
                            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton,
                                onClick = { if (!selected) onSelect(code) })
                            .padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(label, color = if (selected) colors.accentText else colors.text,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
