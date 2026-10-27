package com.repforge.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.NativeThemeTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativeThemePicker(settings: TrainPilotUserSettings, onSelect: (ThemePaletteOption) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val labels = when (settings.language) {
        "en" -> listOf("Appearance", "Basic colors", "Vivid colors")
        "de" -> listOf("Darstellung", "Grundfarben", "Leuchtende Farben")
        "ro" -> listOf("Aspect", "Culori de bază", "Culori intense")
        else -> listOf("Megjelenés", "Alapszínek", "Élénk színek")
    }
    Card(Modifier.fillMaxWidth().testTag("settings_theme_picker"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card), border = BorderStroke(1.dp, colors.line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(labels[0], color = colors.text, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((index, family) in listOf("basic", "vivid").withIndex()) {
                    Text(labels[index + 1], color = colors.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (theme in TrainPilotSettingsStore.AVAILABLE_THEMES.filter { it.family == family }) {
                            val selected = settings.themeKey == theme.key
                            var focused by remember(theme.key) { mutableStateOf(false) }
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = if (selected) colors.selectedSurface else colors.card,
                                border = BorderStroke(if (selected || focused) 2.dp else 1.dp,
                                    if (selected || focused) colors.selectedBorder else colors.line)) {
                                Column(Modifier.widthIn(min = 112.dp, max = 200.dp).heightIn(min = 64.dp)
                                    .testTag("theme_${theme.key}")
                                    .onFocusChanged { focused = it.isFocused }
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(theme) })
                                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.Start) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(NativeThemeTokens.parseHex(theme.accent, colors.accent)))
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(NativeThemeTokens.parseHex(theme.accent2, colors.accent2)))
                                    }
                                    Text(theme.displayName(settings.language), modifier = Modifier.fillMaxWidth(),
                                        color = if (selected) colors.accentText else colors.text,
                                        softWrap = true, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
