package com.repforge.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
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
import com.repforge.app.ui.theme.TrainPilotDimensions

/** Compact shared toolbar: settings remains available without adding a sixth tab. */
@Composable
internal fun NativeMainHeader(route: String, language: String, enabled: Boolean,
    onBack: (() -> Unit)?, onSettings: () -> Unit, backEnabled: Boolean = enabled) {
    val colors = LocalTrainPilotColors.current
    Surface(color = colors.bg) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)
            .heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack, enabled = backEnabled,
                modifier = Modifier.size(TrainPilotDimensions.Control).testTag("navigation_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, NativeMainMenu.backLabel(language), tint = colors.text)
            }
            Text(if (route == "home") "TrainPilot" else NativeMainMenu.title(route, language),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                color = colors.text, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onSettings, enabled = enabled && route != "settings",
                modifier = Modifier.size(TrainPilotDimensions.Control).testTag("navigation_settings")) {
                Icon(Icons.Default.Settings, NativeMainMenu.title("settings", language),
                    tint = if (enabled && route != "settings") colors.muted else colors.muted.copy(alpha = .4f))
            }
        }
    }
}

/** Studio-inspired five-pill navigation, intentionally fixed below the header.
 * Keep the canonical navigation_* test tags and native navigation guards.
 * No demo state or action is added here.
 */
@Composable
internal fun NativeTopNavigation(route: String, settingsOrigin: String, language: String,
    enabled: Boolean, onSelect: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val selected = NativeMainMenu.selected(route, settingsOrigin)
    val labels = NativeMainMenu.labels(language)
    val destinations = NativeMainMenu.routes
    Surface(color = colors.bg, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            destinations.forEachIndexed { index, destination ->
                val active = selected == destination
                val icon = when (destination) {
                    "home" -> Icons.Default.Home
                    "training" -> Icons.Default.FitnessCenter
                    "journal" -> Icons.Default.MenuBook
                    "progress" -> Icons.Default.ShowChart
                    else -> Icons.Default.FavoriteBorder
                }
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        .clip(shape)
                        .background(if (active) colors.selectedSurface else colors.card)
                        .border(if (active) 1.5.dp else 1.dp,
                            if (active) colors.accentText else colors.line, shape)
                        .clickable(enabled = enabled) { onSelect(destination) }
                        .testTag("navigation_$destination")
                        .padding(horizontal = 1.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (active) colors.accentText else colors.muted)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        labels[index],
                        color = if (active) colors.accentText else colors.text,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
