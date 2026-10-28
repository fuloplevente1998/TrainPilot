package com.repforge.app.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.R
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/**
 * Source-structure port of the user-approved 18:49 Studio
 * TrainPilotGlobalTopHeaderAndNav + TopNavPillItem.
 * Explicit brand exception: the original GOLD DUMBBELL launcher symbol stays.
 *
 * Only presentation is ported: native DataStore owns language/palette, native
 * NavHost owns navigation, and native execution fences own click permissions.
 * No Studio demo ViewModel, secondary route, fabricated value or new launcher.
 */
@Composable
internal fun NativeStudioShellHeader(
    route: String,
    settingsOrigin: String,
    language: String,
    themeKey: String,
    enabled: Boolean,
    onHome: () -> Unit,
    onSelect: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    val selected = NativeMainMenu.selected(route, settingsOrigin)
    val labels = NativeMainMenu.labels(language)
    var languageExpanded by remember { mutableStateOf(false) }
    val flags = mapOf("hu" to "🇭🇺", "en" to "🇬🇧", "de" to "🇩🇪", "ro" to "🇷🇴")
    // UI ID is a current native palette entry, not a fabricated Studio state value.
    val selectedPalette = TrainPilotSettingsStore.AVAILABLE_THEMES
        .indexOfFirst { it.key == themeKey }
        .takeIf { it >= 0 }?.plus(1)

    Surface(color = colors.bg,
        modifier = Modifier.fillMaxWidth()
            .border(.5.dp, colors.line.copy(alpha = .5f))) {
        Column(Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 18:49 source: 48dp logo/title + language/choice/settings controls.
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClick = onHome)
                    .testTag("header_home")
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Brand exception explicitly approved by the owner:
                    // exact original gold dumbbell used by the launcher icon.
                    TrainPilotBrandLogo()
                    Text("TrainPilot", style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = (-.2).sp), fontWeight = FontWeight.Bold,
                        color = colors.accentText, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Surface(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = enabled) { languageExpanded = true }
                            .testTag("header_language"),
                            shape = RoundedCornerShape(10.dp), color = colors.card2) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language,
                                    contentDescription = strings.getString(R.string.native_language_title),
                                    tint = colors.accentText, modifier = Modifier.size(16.dp))
                                Text("${flags[language] ?: "🌐"} ${language.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.text, fontWeight = FontWeight.Bold,
                                    maxLines = 1)
                            }
                        }
                        DropdownMenu(expanded = languageExpanded && enabled,
                            onDismissRequest = { languageExpanded = false },
                            modifier = Modifier.background(colors.card)) {
                            listOf(
                                Triple("hu", "🇭🇺", "Magyar"),
                                Triple("en", "🇬🇧", "English"),
                                Triple("de", "🇩🇪", "Deutsch"),
                                Triple("ro", "🇷🇴", "Română")
                            ).forEach { (code, flag, title) ->
                                DropdownMenuItem(
                                    modifier = Modifier.heightIn(min = 48.dp)
                                        .testTag("header_language_${code}"),
                                    text = { Text("$flag $title (${code.uppercase()})",
                                        color = if (code == language) colors.accentText else colors.text,
                                        fontWeight = if (code == language)
                                            FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        languageExpanded = false
                                        if (code != language) onSelectLanguage(code)
                                    }
                                )
                            }
                        }
                    }
                    Surface(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .border(1.dp, colors.accent.copy(alpha = .55f), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled, onClick = onTheme)
                        .testTag("header_theme"),
                        shape = RoundedCornerShape(10.dp), color = colors.card2) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette,
                                contentDescription = strings.getString(R.string.studio_header_theme),
                                tint = colors.accentText, modifier = Modifier.size(16.dp))
                            Text(selectedPalette?.let { "#$it" } ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                color = colors.accentText)
                        }
                    }
                    Surface(Modifier.size(48.dp)
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled, onClick = onSettings)
                        .testTag("navigation_settings"),
                        shape = RoundedCornerShape(10.dp), color = colors.card2) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Settings,
                                contentDescription = NativeMainMenu.title("settings", language),
                                tint = colors.text, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            // 18:49 source five-tab row, with the real native navigation fences.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val scrollable = NativeMainMenu.topTabsScrollable(
                    availableWidthDp = maxWidth.value,
                    fontScale = LocalDensity.current.fontScale)
                val scroll = rememberScrollState()
                LaunchedEffect(selected, scrollable, scroll.maxValue) {
                    if (scrollable && scroll.maxValue > 0) {
                        val index = NativeMainMenu.routes.indexOf(selected).coerceAtLeast(0)
                        scroll.animateScrollTo(NativeMainMenu.topTabScrollTarget(
                            scroll.maxValue, index, NativeMainMenu.routes.size))
                    }
                }
                Row(Modifier.fillMaxWidth()
                    .then(if (scrollable) Modifier.horizontalScroll(scroll) else Modifier)
                    .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NativeMainMenu.routes.forEachIndexed { index, destination ->
                        val active = destination == selected
                        val icon: ImageVector = when (destination) {
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
                        Surface(
                            modifier = (if (scrollable)
                                Modifier.widthIn(min = tabWidth)
                            else Modifier.weight(1f))
                                .heightIn(min = 48.dp)
                                .border(if (active) 1.5.dp else 1.dp,
                                    if (active) colors.accent else colors.line, shape)
                                .clip(shape)
                                .selectable(selected = active, enabled = enabled, role = Role.Tab) {
                                    onSelect(destination)
                                }
                                .testTag("navigation_$destination"),
                            shape = shape,
                            color = if (active) colors.accent.copy(alpha = .22f) else colors.card
                        ) {
                            Column(Modifier.padding(horizontal = if (scrollable) 10.dp else 2.dp,
                                    vertical = 7.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = null,
                                    tint = if (active) colors.accent else colors.muted,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(3.dp))
                                Text(labels[index], style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                                    color = if (active) colors.accentText else colors.text,
                                    maxLines = 1, softWrap = false,
                                    overflow = if (scrollable)
                                        TextOverflow.Visible else TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
