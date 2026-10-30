package com.repforge.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Wrapping entry points keep all former primary functions reachable with five tabs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativePlanningShortcuts(language: String, enabled: Boolean,
    onPrograms: () -> Unit, onCalendar: () -> Unit, onCoach: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((route, callback) in listOf("programs" to onPrograms, "calendar" to onCalendar, "coach" to onCoach)) {
            val icon = when (route) {
                "programs" -> Icons.Default.ViewAgenda
                "calendar" -> Icons.Default.DateRange
                else -> Icons.Default.AutoAwesome
            }
            AssistChip(onClick = callback, enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp).testTag("shortcut_$route"),
                label = { Text(NativeMainMenu.title(route, language)) },
                leadingIcon = { Icon(icon, contentDescription = null) })
        }
    }
}
