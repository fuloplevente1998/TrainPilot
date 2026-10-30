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
internal fun NativeLanguagePicker(
    language: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    val options = listOf(
        Triple("hu", "🇭🇺", "HU"),
        Triple("en", "🇬🇧", "EN"),
        Triple("de", "🇩🇪", "DE"),
        Triple("ro", "🇷🇴", "RO")
    )
    Card(
        Modifier.fillMaxWidth().testTag("settings_language_picker"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                strings.getString(R.string.native_language_title),
                color = colors.text,
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((code, flag, short) in options) {
                    val selected = language == code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) colors.accent else colors.card2,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.accent else colors.line
                        )
                    ) {
                        Column(
                            Modifier
                                .heightIn(min = 48.dp)
                                .testTag("settings_language_$code")
                                .selectable(
                                    selected = selected,
                                    enabled = enabled,
                                    role = Role.RadioButton,
                                    onClick = { if (!selected) onSelect(code) }
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "$flag $short",
                                color = if (selected) colors.onAccent else colors.text,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
