package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes

import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.model.SubScreen

@Composable
fun TrainPilotMountainLogo(
    color: Color,
    modifier: Modifier = Modifier.size(30.dp, 20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = h * 0.22f

        val path = Path().apply {
            moveTo(w * 0.06f, h * 0.86f)
            lineTo(w * 0.34f, h * 0.20f)
            lineTo(w * 0.54f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.26f)
            lineTo(w * 0.94f, h * 0.86f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Global Top Header + 5-Button Top Navigation Bar:
 * - Properly padded below the Android status bar (WindowInsets.statusBars + extra top margin)
 * - Thin top bar: TrainPilot mountain logo + app name + Language selector icon + Theme selector icon + Settings icon
 * - Single row of the 5 primary navigation buttons at the top: Kezdőlap, Edzés, Napló, Fejlődés, Egészség
 */
@Composable
fun TrainPilotGlobalTopHeaderAndNav(
    currentTab: MainTab,
    currentSubScreen: SubScreen,
    currentLanguage: AppLanguage,
    selectedPaletteId: Int,
    strings: LocalizedStrings,
    onSelectTab: (MainTab) -> Unit,
    onNavigateToSubScreen: (SubScreen, MainTab?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenPalettePicker: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        color = palette.background,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = palette.cardBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Vékony sáv: App logó + TrainPilot név + Nyelvválasztó + Témaválasztó + Beállítások ikon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(MainTab.HOME) }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                        .testTag("top_bar_brand_home")
                ) {
                    TrainPilotMountainLogo(
                        color = palette.primary,
                        modifier = Modifier.size(26.dp, 18.dp)
                    )
                    Text(
                        text = "TrainPilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = palette.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.primary.copy(alpha = 0.16f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = palette.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(6.dp)
                        )
                    ) {
                        Text(
                            text = "DEMO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = palette.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                // Right icons: Language Selector + 32-Theme Selector + Settings Gear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Language selector button + dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showLangMenu = true }
                                .testTag("top_bar_language_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Nyelvválasztó",
                                    tint = palette.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${currentLanguage.flag} ${currentLanguage.code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            modifier = Modifier.background(palette.surface)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val isCurrent = lang == currentLanguage
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = lang.flag)
                                            Text(
                                                text = "${lang.label} (${lang.code})",
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) palette.primary else palette.textPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        showLangMenu = false
                                        onSelectLanguage(lang)
                                    }
                                )
                            }
                        }
                    }

                    // 32 Theme Selector Icon Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .border(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenPalettePicker() }
                            .testTag("top_bar_theme_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "32 Színpaletta",
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "#$selectedPaletteId",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                        }
                    }

                    // Settings Icon Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentSubScreen == SubScreen.SETTINGS_MAIN) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenSettings() }
                            .testTag("top_bar_settings_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = if (currentSubScreen == SubScreen.SETTINGS_MAIN) palette.onPrimary else palette.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Felső 5 fő navigációs gomb: Kezdőlap | Edzés | Napló | Fejlődés | Egészség
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TopNavPillItem(
                    label = strings.navHome,
                    icon = Icons.Default.Home,
                    selected = currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.HOME) },
                    modifier = Modifier.weight(1f),
                    testTag = "top_nav_home"
                )
                TopNavPillItem(
                    label = strings.navWorkout,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    selected = currentTab == MainTab.WORKOUT && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.WORKOUT) },
                    modifier = Modifier.weight(1f),
                    testTag = "top_nav_workout"
                )
                TopNavPillItem(
                    label = strings.navLog,
                    icon = Icons.Default.Description,
                    selected = currentTab == MainTab.LOG && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.LOG) },
                    modifier = Modifier.weight(1f),
                    testTag = "top_nav_log"
                )
                TopNavPillItem(
                    label = strings.navProgress,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    selected = currentTab == MainTab.PROGRESS && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.PROGRESS) },
                    modifier = Modifier.weight(1f),
                    testTag = "top_nav_progress"
                )
                TopNavPillItem(
                    label = strings.navHealth,
                    icon = if (currentTab == MainTab.HEALTH && currentSubScreen == SubScreen.NONE) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    selected = currentTab == MainTab.HEALTH && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.HEALTH) },
                    modifier = Modifier.weight(1f),
                    testTag = "top_nav_health"
                )
            }
        }
    }
}

@Composable
private fun TopNavPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.primary.copy(alpha = 0.22f) else palette.surface,
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) palette.primary else palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 10.sp
                ),
                color = if (selected) palette.primary else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    rightExtraContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("subscreen_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Vissza",
                tint = palette.textPrimary
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("subscreen_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Beállítások",
                    tint = palette.textSecondary
                )
            }
            if (rightExtraContent != null) {
                rightExtraContent()
            }
        }
    }
}

@Composable
fun TrainPilotBottomNavigationBar(
    currentTab: MainTab,
    strings: LocalizedStrings,
    onSelectTab: (MainTab) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        color = palette.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = palette.cardBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(MainTab.HOME, strings.navHome, Icons.Default.Home),
                Triple(MainTab.WORKOUT, strings.navWorkout, Icons.AutoMirrored.Filled.DirectionsRun),
                Triple(MainTab.LOG, strings.navLog, Icons.Default.Description),
                Triple(MainTab.PROGRESS, strings.navProgress, Icons.AutoMirrored.Filled.TrendingUp),
                Triple(
                    MainTab.HEALTH,
                    strings.navHealth,
                    if (currentTab == MainTab.HEALTH) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                )
            )

            items.forEach { (tab, label, icon) ->
                val isSelected = currentTab == tab
                val tint = if (isSelected) palette.primary else palette.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 6.dp)
                        .testTag("bottom_nav_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PalettePickerBottomSheet(
    selectedPaletteId: Int,
    selectedCategory: PaletteCategory?,
    currentLanguage: AppLanguage,
    fontScaleMultiplier: Float,
    compactPhoneMode: Boolean,
    onSelectPalette: (Int) -> Unit,
    onSelectCategory: (PaletteCategory?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredPalettes = remember(selectedCategory) {
        if (selectedCategory == null) TrainPilotPalettes.allPalettes
        else TrainPilotPalettes.allPalettes.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = palette.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "32 Színpaletta & Megjelenés",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Azonnali téma-, nyelv- és betűméret-váltás (DEMO)",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bezárás",
                        tint = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Language & Accessibility Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val active = currentLanguage == lang
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier.clickable { onSelectLanguage(lang) }
                    ) {
                        Text(
                            text = "${lang.flag} ${lang.code}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) palette.onPrimary else palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                listOf(1.0f to "100% Betű", 1.15f to "115% Nagy", 1.28f to "130% XL").forEach { (scale, label) ->
                    val active = kotlin.math.abs(fontScaleMultiplier - scale) < 0.03f
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (active) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFontScale(scale) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) palette.primary else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (compactPhoneMode) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (compactPhoneMode) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleCompactMode(!compactPhoneMode) }
                ) {
                    Text(
                        text = if (compactPhoneMode) "📱 Kis telefon: BE" else "📱 Kis telefon: KI",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (compactPhoneMode) palette.primary else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter: Összes (32) | Basic (16) | Vivid (16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    null to "Összes (32)",
                    PaletteCategory.BASIC to "Basic / Alap (16)",
                    PaletteCategory.VIVID to "Vivid / Neon (16)"
                )
                cats.forEach { (cat, label) ->
                    val active = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) palette.onPrimary else palette.textPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPalettes, key = { it.id }) { item ->
                    PaletteRowCard(
                        item = item,
                        isSelected = item.id == selectedPaletteId,
                        langCode = currentLanguage.code,
                        onClick = { onSelectPalette(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRowCard(
    item: TrainPilotPalette,
    isSelected: Boolean,
    langCode: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = item.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) item.primary else item.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("palette_option_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Swatch circles showing Background, Surface, Primary, and Secondary
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.background)
                            .border(1.dp, item.cardBorder, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.primary)
                            .border(1.dp, item.background, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.tertiary)
                            .border(1.dp, item.background, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.localizedName(langCode),
                        style = MaterialTheme.typography.titleSmall,
                        color = item.textPrimary
                    )
                    Text(
                        text = if (item.category == PaletteCategory.BASIC) "Alapszín • Letisztult kontraszt" else "Vivid / Neon • Erős kiemelés",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.textSecondary
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = item.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Kiválasztva",
                        tint = item.onPrimary,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalsEditDialog(
    currentWeeklyWorkouts: Int,
    currentStepsGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var workouts by remember { mutableIntStateOf(currentWeeklyWorkouts) }
    var steps by remember { mutableIntStateOf(currentStepsGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = "Célok beállítása (DEMO)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Állítsd be a ciklus edzéscélját és a napi lépéscélt. A változtatás azonnal frissíti a Kezdőlapot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Column {
                    Text(
                        text = "Ciklus edzéscél: $workouts edzés",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = workouts.toFloat(),
                        onValueChange = { workouts = it.toInt() },
                        valueRange = 3f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary
                        )
                    )
                }
                Column {
                    Text(
                        text = "Napi lépéscél: $steps lépés",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = { steps = (it.toInt() / 500) * 500 },
                        valueRange = 4000f..20000f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.stepsAccent,
                            activeTrackColor = palette.stepsAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(workouts, steps) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text("Mentés", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Mégse", color = palette.textPrimary)
            }
        }
    )
}

@Composable
fun PersonalPlannerModalDialog(
    currentStyle: String,
    currentRule: String,
    gentleBeginner: Boolean,
    onSaveAndOpenPrograms: (String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var selectedStyle by remember { mutableStateOf(currentStyle) }
    var selectedRule by remember { mutableStateOf(currentRule) }
    var gentle by remember { mutableStateOf(gentleBeginner) }

    val styles = listOf("Normál hipertrófia", "Erő és alapgyakorlatok", "Otthoni saját testsúlyos", "HIT / Intenzív")
    val rules = listOf("Dupla progresszió", "RIR / Coach progresszió", "Lineáris súlyemelés")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = "Személyes edzéstervező • Profil és új terv",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Válaszd ki az edzésstílust és a progressziós szabályt (DEMO előnézet):",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text("Edzésstílus", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                styles.forEach { style ->
                    val active = selectedStyle == style
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary.copy(alpha = 0.2f) else palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (active) palette.primary else palette.cardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedStyle = style }
                    ) {
                        Text(
                            text = style,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Progressziós szabály", style = MaterialTheme.typography.labelLarge, color = palette.primary)
                rules.forEach { rule ->
                    val active = selectedRule == rule
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary.copy(alpha = 0.2f) else palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (active) palette.primary else palette.cardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedRule = rule }
                    ) {
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { gentle = !gentle }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kímélő kezdőprofil (2 sorozat)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (gentle) "AKTÍV" else "KI",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (gentle) palette.primary else palette.textSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndOpenPrograms(selectedStyle, selectedRule, gentle) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text("Mentés és tervek", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Mégse", color = palette.textPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWorkoutSheet(
    onStartStrengthQuick: () -> Unit,
    onStartTimedQuick: () -> Unit,
    onStartOutdoorGps: () -> Unit,
    onBrowseExercises: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Gyors edzés indítása (DEMO)",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
            Text(
                text = "Válassz edzéstípust vagy nézd meg a gyakorlatok bemutató videóját:",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            QuickOptionRow(
                icon = Icons.Default.FitnessCenter,
                title = "Súlyzós / Saját testsúlyos gyors edzés",
                subtitle = "Goblet guggolás, Fekvőtámasz, Evezés",
                demoExerciseForVideo = "Goblet guggolás",
                onClick = onStartStrengthQuick
            )
            QuickOptionRow(
                icon = Icons.Default.Timer,
                title = "Időalapú / Intervallum edzés",
                subtitle = "Plank, Oldalsó plank és automatikus intervallumok",
                demoExerciseForVideo = "Plank",
                onClick = onStartTimedQuick
            )
            QuickOptionRow(
                icon = Icons.Default.Park,
                title = "Szabadtéri GPS edzés (Futás / Túra)",
                subtitle = "Élő térképes GPS-útvonal, tempó és távolságmérés",
                onClick = onStartOutdoorGps
            )
            HorizontalDivider(color = palette.cardBorder)
            QuickOptionRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "Gyakorlat választása (106 beépített gyakorlat)",
                subtitle = "Keresés izomcsoport, eszköz és bemutató videók szerint",
                onClick = onBrowseExercises
            )
        }
    }
}

@Composable
private fun QuickOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    demoExerciseForVideo: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (demoExerciseForVideo != null) {
                ExercisePostureMiniIllustration(
                    exerciseName = demoExerciseForVideo,
                    size = 44.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            if (demoExerciseForVideo != null) {
                SmallExerciseVideoButton(exerciseName = demoExerciseForVideo)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

val LocalOpenExerciseVideo = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

/**
 * Compact reusable "▶ Videó" button placed on exercise rows in lists, workouts, quick workouts, program editor, and logs.
 * Opens the embedded YouTube exercise demonstration popup dialog.
 */
@Composable
fun SmallExerciseVideoButton(
    exerciseName: String,
    modifier: Modifier = Modifier,
    compactIconOnly: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val openVideo = LocalOpenExerciseVideo.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE53935).copy(alpha = 0.16f),
        modifier = modifier
            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable { openVideo(exerciseName) }
            .testTag("exercise_video_btn_${exerciseName.hashCode()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compactIconOnly) 6.dp else 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.15f)
                        lineTo(size.width * 0.88f, size.height * 0.50f)
                        lineTo(size.width * 0.22f, size.height * 0.85f)
                        close()
                    }
                    drawPath(path = path, color = Color.White)
                }
            }
            if (!compactIconOnly) {
                Text(
                    text = "Videó",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = palette.textPrimary
                )
            }
        }
    }
}

/**
 * Popup Modal Window displaying the embedded YouTube video for the selected exercise.
 */
@Composable
fun ExerciseYouTubeVideoModalDialog(
    exerciseTitle: String,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var isPlaying by remember { mutableStateOf(true) }
    var useEmbeddedWebView by remember { mutableStateOf(true) }

    val youtubeVideoId = remember(exerciseTitle) {
        val lower = exerciseTitle.lowercase()
        when {
            lower.contains("goblet") || lower.contains("guggol") -> "MeIiIdhvXT4"
            lower.contains("fekvőtámasz") || lower.contains("fekvotamasz") -> "IODxDxX7oi4"
            lower.contains("fekvenyom") -> "rT7DgCr-3pg"
            lower.contains("plank") -> "pSHjTRCQxIw"
            lower.contains("húzódzkod") || lower.contains("huzodzkod") -> "eGo4IYlbE5g"
            lower.contains("evez") -> "FWJR5Ve8bnQ"
            lower.contains("kitör") || lower.contains("bolgár") -> "2C-uNgKwPLE"
            lower.contains("váll") || lower.contains("vall") -> "2yjwXTZQDDI"
            lower.contains("tricepsz") || lower.contains("bicepsz") || lower.contains("kar") -> "ykJmrZ5v0Oo"
            else -> "MeIiIdhvXT4"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE53935)
                    ) {
                        Text(
                            text = "▶ YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = exerciseTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bezárás",
                        tint = palette.textSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Embedded YouTube Player Box (16:9 ratio)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B0E14),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Visual YouTube Embed Preview + WebView layer
                        if (useEmbeddedWebView) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))
                                        webChromeClient = android.webkit.WebChromeClient()
                                        webViewClient = android.webkit.WebViewClient()
                                        val html = """
                                            <!DOCTYPE html>
                                            <html>
                                            <head>
                                              <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                              <style>
                                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #0B0E14; overflow: hidden; font-family: sans-serif; color: #fff; }
                                                .wrap { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                                                iframe { width: 100%; height: 100%; border: 0; }
                                              </style>
                                            </head>
                                            <body>
                                              <div class="wrap">
                                                <iframe src="https://www.youtube.com/embed/$youtubeVideoId?autoplay=1&rel=0&modestbranding=1&playsinline=1"
                                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                                        allowfullscreen></iframe>
                                              </div>
                                            </body>
                                            </html>
                                        """.trimIndent()
                                        loadDataWithBaseURL(
                                            "https://www.youtube.com",
                                            html,
                                            "text/html",
                                            "utf-8",
                                            null
                                        )
                                    }
                                }
                            )
                        }

                        // Top overlay badge showing YouTube ID & interactive simulation toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YouTube beágyazott lejátszó • ID: $youtubeVideoId",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = if (useEmbeddedWebView) "3D nézet" else "YouTube nézet",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.clickable { useEmbeddedWebView = !useEmbeddedWebView }
                            )
                        }

                        if (!useEmbeddedWebView) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clickable { isPlaying = !isPlaying }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isPlaying) "⏸" else "▶",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$exerciseTitle – Technikai bemutató",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isPlaying) "Lejátszás folyamatban (0:14 / 0:45)" else "Szüneteltetve",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary
                                )
                            }
                        }
                    }
                }

                // Key coaching & execution cues under the video player
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Fő végrehajtási pontok:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = "• Kontrollált excentrikus szakasz (2 mp leengedés), stabil törzzsel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "• Teljes mozgástartomány (ROM) megtartása rángatás nélkül.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text("Bezárás", fontWeight = FontWeight.Bold)
            }
        }
    )
}

