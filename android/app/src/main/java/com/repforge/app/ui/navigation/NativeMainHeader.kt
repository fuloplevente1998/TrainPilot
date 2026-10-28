package com.repforge.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions

/** Compact shared toolbar: settings remains available without adding a sixth tab. */
@Composable
internal fun NativeMainHeader(route: String, language: String, enabled: Boolean,
    onBack: (() -> Unit)?, onSettings: () -> Unit, backEnabled: Boolean = enabled,
    onLanguage: () -> Unit = onSettings, onTheme: () -> Unit = onSettings,
    onHome: (() -> Unit)? = null) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    Surface(color = colors.bg) {
        Row(Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack, enabled = backEnabled,
                modifier = Modifier.size(TrainPilotDimensions.Control).testTag("navigation_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, NativeMainMenu.backLabel(language), tint = colors.text)
            }
            val mainPage = route in NativeMainMenu.routes
            if (mainPage) Row(
                Modifier.weight(1f).heightIn(min = TrainPilotDimensions.Control)
                    .clickable(enabled = enabled && onHome != null, role = Role.Button) { onHome?.invoke() }
                    .semantics { contentDescription = NativeMainMenu.title("home", language) }
                    .testTag("header_home"), verticalAlignment = Alignment.CenterVertically) {
                TrainPilotBrandLogo()
                Text("TrainPilot", Modifier.weight(1f).padding(horizontal = 8.dp),
                    color = if (enabled) colors.accentText else colors.muted,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.2).sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text(NativeMainMenu.title(route, language),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = colors.text,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (mainPage) {
                IconButton(onClick = onLanguage, enabled = enabled,
                    modifier = Modifier.size(TrainPilotDimensions.Control).testTag("header_language")
                        .semantics { contentDescription = strings.getString(R.string.native_language_title) }) {
                    Text(language.uppercase(java.util.Locale.ROOT), style = MaterialTheme.typography.labelMedium,
                        color = colors.text)
                }
                IconButton(onClick = onTheme, enabled = enabled,
                    modifier = Modifier.size(TrainPilotDimensions.Control).testTag("header_theme")) {
                    Icon(Icons.Default.Palette, strings.getString(R.string.studio_header_theme),
                        tint = if (enabled) colors.accentText else colors.muted)
                }
            }
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
    // 18:49 AI Studio export: at 320dp OR increased font scale, allow every
    // translated label to remain readable instead of ellipsizing or shrinking text.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val scrollable = NativeMainMenu.topTabsScrollable(
            availableWidthDp = maxWidth.value, fontScale = LocalDensity.current.fontScale)
        val scroll = rememberScrollState()
        androidx.compose.runtime.LaunchedEffect(selected, scrollable, scroll.maxValue) {
            if (scrollable && scroll.maxValue > 0) {
                val index = destinations.indexOf(selected).coerceAtLeast(0)
                scroll.animateScrollTo(NativeMainMenu.topTabScrollTarget(
                    scroll.maxValue, index, destinations.size))
            }
        }
        Surface(color = colors.bg, tonalElevation = 0.dp) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .then(if (scrollable) Modifier.horizontalScroll(scroll) else Modifier)
                    .selectableGroup()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                destinations.forEachIndexed { index, destination ->
                val active = selected == destination
                // Icons follow the latest Studio mockup; the native routes do not change.
                val icon = when (destination) {
                    "home" -> Icons.Default.Home
                    "training" -> Icons.Default.DirectionsRun
                    "journal" -> Icons.Default.Description
                    "progress" -> Icons.Default.TrendingUp
                    else -> if (active) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                }
                val shape = RoundedCornerShape(12.dp)
                val density = LocalDensity.current
                val tabTextStyle = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                val measured = androidx.compose.ui.text.rememberTextMeasurer().measure(
                    labels[index], style = tabTextStyle, softWrap = false, maxLines = 1)
                val tabWidth = maxOf(76.dp, with(density) { measured.size.width.toDp() } + 24.dp)
                val fill = if (active) colors.accent.copy(alpha = .22f).compositeOver(colors.card) else colors.card
                Column(
                    modifier = (if (scrollable) Modifier.widthIn(min = tabWidth)
                        else Modifier.weight(1f)).heightIn(min = 48.dp)
                        .clip(shape)
                        .background(fill)
                        .border(if (active) 1.5.dp else 1.dp,
                            if (active) colors.accent else colors.line, shape)
                        .selectable(selected = active, enabled = enabled, role = Role.Tab) { onSelect(destination) }
                        .testTag("navigation_$destination")
                        .padding(horizontal = if (scrollable) 10.dp else 2.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (active) colors.accent else colors.muted)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        labels[index], style = tabTextStyle,
                        modifier = if (scrollable) Modifier.widthIn(min = tabWidth - 20.dp) else Modifier,
                        color = if (active) colors.accent else colors.text,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, softWrap = false,
                        overflow = if (scrollable) TextOverflow.Visible else TextOverflow.Ellipsis
                    )
                }
            }
        }
        }
    }
}
