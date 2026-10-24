package com.repforge.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Compact shared toolbar: settings remains available without adding a sixth tab. */
@Composable
internal fun NativeMainHeader(route: String, language: String, enabled: Boolean,
    onBack: (() -> Unit)?, onSettings: () -> Unit, backEnabled: Boolean = enabled) {
    val colors = LocalTrainPilotColors.current
    Surface(color = colors.bg) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)
            .heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack, enabled = backEnabled,
                modifier = Modifier.size(48.dp).testTag("navigation_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, NativeMainMenu.backLabel(language), tint = colors.text)
            }
            Text(if (route == "home") "TrainPilot" else NativeMainMenu.title(route, language),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                color = colors.text, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onSettings, enabled = enabled && route != "settings",
                modifier = Modifier.size(48.dp).testTag("navigation_settings")) {
                Icon(Icons.Default.Settings, NativeMainMenu.title("settings", language),
                    tint = if (enabled && route != "settings") colors.muted else colors.muted.copy(alpha = .4f))
            }
        }
    }
}

@Composable
internal fun NativeBottomNavigation(route: String, settingsOrigin: String, language: String,
    enabled: Boolean, onSelect: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val selected = NativeMainMenu.selected(route, settingsOrigin)
    val labels = NativeMainMenu.labels(language)
    NavigationBar(containerColor = colors.card, tonalElevation = 0.dp) {
        NativeMainMenu.routes.forEachIndexed { index, destination ->
            val icon = when (destination) {
                "home" -> Icons.Default.Home
                "training" -> Icons.Default.FitnessCenter
                "journal" -> Icons.Default.MenuBook
                "progress" -> Icons.Default.ShowChart
                else -> Icons.Default.FavoriteBorder
            }
            NavigationBarItem(selected = selected == destination, enabled = enabled,
                onClick = { onSelect(destination) }, modifier = Modifier.testTag("navigation_$destination"),
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(labels[index], maxLines = 2, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent, indicatorColor = colors.accent.copy(alpha = .15f),
                    unselectedIconColor = colors.muted, unselectedTextColor = colors.muted))
        }
    }
}
