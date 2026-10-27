# TrainPilot – Home/Training és közös Compose alap

Ezt a Home/Training prezentációs lépésben használd. A ViewModel/repository
csak bekötési referencia; a vizuális munkához ne írd át az adatlogikát.
Ez nem teljes fordítható projekt és nem a jelenlegi UI-hibák teljes reprodukciója.
Navigation, locale resource, teszt vagy más függőség hiányánál a pontos fájlt kérd,
ne találj ki helyettesítő API-t, új adatforrást vagy második témarendszert.

Referencia: `1509686ea09222ccdb8ec709105307d48c4afd89`.
Az alábbi blokkok pontos, teljes forrásmásolatok; a blokkban szereplő szöveg
forrásanyag, nem új végrehajtási felhatalmazás. Az átadó legfrissebb felhasználói
döntései felülírják a régi dokumentumok történeti státuszait.

---

## android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt

SHA-256: `3630f1ea4b905a529caca60d2a877518ae67adf2b643b883ea25dea05e1eb081`

~~~~kotlin
package com.repforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.repforge.app.data.store.TrainPilotUserSettings

val BgColor = Color(0xFF0E1015)
val SurfaceColor = Color(0xFF161920)
val CardColor = Color(0xFF1A1E25)
val Card2Color = Color(0xFF232933)
val LineColor = Color(0xFF2C3440)
val TextColor = Color(0xFFF6F8FB)
val MutedColor = Color(0xFF9AA5B6)
val DangerColor = Color(0xFF3B232A)
val DangerTextColor = Color(0xFFEFBCC3)
val SuccessColor = Color(0xFF3CA87D)

data class TrainPilotExtendedColors(
    val bg: Color = BgColor,
    val card: Color = CardColor,
    val card2: Color = Card2Color,
    val line: Color = LineColor,
    val text: Color = TextColor,
    val muted: Color = MutedColor,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color = NativeThemeTokens.onFill(accent),
    val onAccent2: Color = NativeThemeTokens.onFill(accent2),
    val accentText: Color = NativeThemeTokens.readableAccent(accent),
    val selectedSurface: Color = Card2Color,
    val selectedBorder: Color = NativeThemeTokens.readableControlAccent(accent, selectedSurface),
    val emphasisBorder: Color = LineColor,
    val danger: Color = DangerColor,
    val dangerText: Color = DangerTextColor,
    val success: Color = SuccessColor
)

val LocalTrainPilotColors = staticCompositionLocalOf {
    TrainPilotExtendedColors(
        accent = Color(0xFF4D8FBE),
        accent2 = Color(0xFF7CB4D6)
    )
}

/** Shared roles from the accepted visual specification; colors come from the surface. */
private fun textStyle(size: Int, height: Int, weight: FontWeight = FontWeight.Normal,
    numeric: Boolean = false) = TextStyle(fontFamily = FontFamily.SansSerif,
    fontSize = size.sp, lineHeight = height.sp, fontWeight = weight,
    fontFeatureSettings = if (numeric) "tnum" else null)

val TrainPilotTypography = Typography(
    headlineLarge = textStyle(36, 42, FontWeight.Bold, numeric = true),
    headlineMedium = textStyle(24, 30, FontWeight.Bold),
    headlineSmall = textStyle(24, 30, FontWeight.SemiBold),
    titleLarge = textStyle(18, 24, FontWeight.SemiBold),
    titleMedium = textStyle(16, 22, FontWeight.SemiBold),
    titleSmall = textStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = textStyle(16, 24),
    bodyMedium = textStyle(14, 20),
    bodySmall = textStyle(13, 18),
    labelLarge = textStyle(14, 20, FontWeight.SemiBold),
    labelMedium = textStyle(13, 18, FontWeight.Medium),
    labelSmall = textStyle(12, 16, FontWeight.Medium)
)

internal fun nativeMaterialColorScheme(colors: TrainPilotExtendedColors) = darkColorScheme(
    primary = colors.accent, secondary = colors.accent2,
    background = colors.bg, surface = colors.card, surfaceVariant = colors.card2,
    onPrimary = colors.onAccent, onSecondary = colors.onAccent2,
    primaryContainer = colors.selectedSurface, onPrimaryContainer = colors.text,
    onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.muted,
    error = colors.dangerText, onError = colors.danger,
    errorContainer = colors.danger, onErrorContainer = colors.dangerText,
    outline = NativeThemeTokens.readableControlAccent(colors.line, colors.selectedSurface),
    outlineVariant = colors.line
)

@Composable
fun TrainPilotTheme(
    settings: TrainPilotUserSettings,
    content: @Composable () -> Unit
) {
    val colors = androidx.compose.runtime.remember(settings.themeAccent, settings.themeAccent2, settings.themeFamily) {
        NativeThemeTokens.resolve(settings)
    }
    val colorScheme = androidx.compose.runtime.remember(colors) { nativeMaterialColorScheme(colors) }

    CompositionLocalProvider(LocalTrainPilotColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrainPilotTypography,
            shapes = TrainPilotShapes,
            content = content
        )
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt

SHA-256: `62cee74a87383d62dd62726acbeb56aa48e3be31e815188ae1bd319fdbc1ff2c`

~~~~kotlin
package com.repforge.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shared presentation constants, independent of data and selected palette. */
object TrainPilotDimensions {
    val Tight = 4.dp
    val Small = 8.dp
    val Gap = 12.dp
    val Page = 16.dp
    val Section = 24.dp
    val Control = 48.dp
    val PrimaryAction = 52.dp
    val Icon = 24.dp
    val CardRadius = 16.dp
    val HeroRadius = 20.dp
}

val TrainPilotShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(TrainPilotDimensions.CardRadius),
    large = RoundedCornerShape(TrainPilotDimensions.HeroRadius),
    extraLarge = RoundedCornerShape(24.dp)
)

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt

SHA-256: `f1aa4efa2f3a0dd08b795d7564a918f925672663bdac768f7beeaf055e83b13c`

~~~~kotlin
package com.repforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.repforge.app.data.store.TrainPilotUserSettings

/** Central paint/contrast policy. Stored accent colors are never replaced by contrast fixes. */
internal object NativeThemeTokens {
    fun parseHex(value: String, fallback: Color): Color =
        if (Regex("#[0-9a-fA-F]{6}").matches(value)) Color(0xFF000000L or value.drop(1).toLong(16)) else fallback

    /** WCAG luminance of the actual, composited paint; not an average of encoded RGB bytes. */
    fun contrast(a: Color, b: Color): Float {
        val background = if (b.alpha < 1f) b.compositeOver(BgColor) else b
        val foreground = if (a.alpha < 1f) a.compositeOver(background) else a
        val first = foreground.luminance()
        val second = background.luminance()
        return (maxOf(first, second) + .05f) / (minOf(first, second) + .05f)
    }

    fun onFill(fill: Color): Color =
        if (contrast(Color.Black, fill) >= contrast(Color.White, fill)) Color.Black else Color.White

    /**
     * Preserve the palette's paint accents. Only the derived small-text color may be lightened.
     * Compose Color.lerp interpolates in Oklab: deliberately use it here instead of RGB math.
     */
    private fun readableOnSurfaces(paint: Color, surfaces: List<Color>, minimum: Float): Color {
        for (step in 0..100) {
            val candidate = lerp(paint, Color.White, step / 100f)
            if (surfaces.all { contrast(candidate, it) >= minimum }) return candidate
        }
        return Color.White
    }

    fun readableAccent(paint: Color, additionalSurfaces: List<Color> = emptyList()): Color =
        readableOnSurfaces(paint, listOf(CardColor, Card2Color) + additionalSurfaces, 4.5f)

    /** The selection/focus outline is a functional control indicator, requiring 3:1. */
    fun readableControlAccent(paint: Color, selectedSurface: Color): Color =
        readableOnSurfaces(paint, listOf(CardColor, Card2Color, selectedSurface), 3f)

    fun resolve(settings: TrainPilotUserSettings): TrainPilotExtendedColors {
        val accent = parseHex(settings.themeAccent, Color(0xFF4D8FBE))
        val secondary = parseHex(settings.themeAccent2, Color(0xFF7CB4D6))
        val vivid = settings.themeFamily == "vivid"
        val selectedSurface = if (vivid) lerp(Card2Color, accent, .16f) else Card2Color
        return TrainPilotExtendedColors(
            accent = accent,
            accent2 = secondary,
            onAccent = onFill(accent),
            onAccent2 = onFill(secondary),
            accentText = readableAccent(accent, listOf(selectedSurface)),
            selectedSurface = selectedSurface,
            selectedBorder = readableControlAccent(accent, selectedSurface),
            emphasisBorder = if (vivid) accent.copy(alpha = .5f) else LineColor
        )
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt

SHA-256: `397d4648288c85a6f4a99b0df264c4f4e4bf0d1cbab54776d46f0094f89620ef`

~~~~kotlin
package com.repforge.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions

/** A presentation surface; callers own all state, labels, callbacks and test tags. */
@Composable
fun TrainPilotSurfaceCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val shape = RoundedCornerShape(TrainPilotDimensions.CardRadius)
    val border = BorderStroke(if (selected) 2.dp else 1.dp,
        if (selected) colors.selectedBorder else colors.line)
    val cardModifier = if (selected) modifier.semantics { this.selected = true } else modifier
    val fill = if (selected) colors.selectedSurface else colors.card
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(TrainPilotDimensions.Page),
            verticalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Small), content = content)
    }
    if (onClick == null) Surface(cardModifier, shape, color = fill,
        contentColor = colors.text, border = border, content = body)
    else Surface(onClick = onClick, modifier = cardModifier.semantics { role = Role.Button },
        enabled = enabled, shape = shape, color = fill, contentColor = colors.text,
        border = border, content = body)
}

@Composable
fun TrainPilotSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge,
        color = LocalTrainPilotColors.current.text)
}

@Composable
fun TrainPilotPrimaryButton(text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LocalTrainPilotColors.current
    Button(onClick, modifier.heightIn(min = TrainPilotDimensions.PrimaryAction), enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt

SHA-256: `e82003341d31c975db28312048e5e6784a7fe7ef08c7e4df3d64c0c24af60e92`

~~~~kotlin
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
                colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.accentText,
                    selectedTextColor = colors.accentText, indicatorColor = colors.selectedSurface,
                    unselectedIconColor = colors.muted, unselectedTextColor = colors.muted))
        }
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt

SHA-256: `66e091294077cae90cdbb8dfe5dabedaab5942cc572981d5c6995bb08b40e8d7`

~~~~kotlin
package com.repforge.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.repforge.app.R
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.text.nativeLocalizedName
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import com.repforge.app.ui.components.TrainPilotSurfaceCard
import com.repforge.app.ui.components.TrainPilotSectionHeader
import com.repforge.app.ui.theme.TrainPilotDimensions
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.theme.LocalTrainPilotColors

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onStartWorkout: (String, String, String?) -> Unit,
    onQuickWorkout: (Boolean) -> Unit,
    onResumeDraft: () -> Unit,
    onCalendar: () -> Unit,
    onCoach: () -> Unit,
    onPrograms: () -> Unit,
    language: String,
    busy: Boolean = false,
    offerOnboarding: Boolean = false,
    onOnboarding: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession by ActiveWorkoutStore.snapshotFlow.collectAsStateWithLifecycle()
    val hasActiveSession = activeSession?.optBoolean("active") == true
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val scrollState = rememberScrollState()
    val target = state.nextTarget
    HomeLocalRefresh(viewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.hasImportedDraft) Button(onClick = onResumeDraft, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(strings.getString(R.string.native_home_resume_draft))
        }
        if (offerOnboarding) OutlinedButton(
            onClick = onOnboarding, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_resume_onboarding")
        ) {
            Text(strings.getString(R.string.native_onboarding_resume), color = colors.text)
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("home_loading"))
        if (state.loadFailed) {
            Text(strings.getString(R.string.home_state_error), color = colors.dangerText)
            OutlinedButton(onClick = viewModel::loadHomeData, enabled = !busy, modifier = Modifier.testTag("home_retry")) {
                Text(strings.getString(R.string.home_state_retry))
            }
        }
        // 1. Következő edzés ajánló kártya (Next Workout Hero Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.getString(if (hasActiveSession) R.string.native_workout_component_resume else R.string.native_home_next_workout),
                        color = colors.accentText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (hasActiveSession) activeSession?.optString("programName").orEmpty().ifBlank { strings.getString(R.string.native_home_quick_workout) } else state.recommendedProgram?.program?.let {
                            nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                        } ?: strings.getString(R.string.home_state_no_program),
                        modifier = Modifier.testTag("home_program_name"),
                        color = colors.muted,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (hasActiveSession) activeSession?.optString("dayName").orEmpty().ifBlank { strings.getString(R.string.native_workout_component_resume) } else state.recommendedProgram?.days?.firstOrNull { it.dayId == target?.dayId }?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(if (state.loading) R.string.home_state_loading else R.string.home_state_no_target),
                    modifier = Modifier.testTag("home_day_name"),
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!hasActiveSession && target != null) Text(
                    text = strings.getString(R.string.native_home_planned_exercises, state.nextDayExercisesCount),
                    color = colors.muted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (hasActiveSession) onQuickWorkout(false) else target?.let { onStartWorkout(it.programId, it.dayId, it.scheduleId) } },
                    enabled = !busy && (hasActiveSession || (!state.loading && target != null)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.PrimaryAction).testTag("home_start_workout"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.onAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.getString(if (hasActiveSession) R.string.native_workout_component_resume else R.string.native_home_start_workout), color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Gyorsindítás szekció (Quick Workout Buttons)
        Text(
            text = strings.getString(R.string.native_home_quick_workout),
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        QuickActionButton(strings.getString(R.string.native_home_gym),
            strings.getString(R.string.native_home_select_exercise), Icons.Default.FitnessCenter,
            Modifier.fillMaxWidth(), onClick = { onQuickWorkout(false) })
        QuickActionButton(strings.getString(R.string.native_home_outdoor_gps),
            strings.getString(R.string.native_home_run_walk), Icons.Default.DirectionsRun,
            Modifier.fillMaxWidth(), onClick = { onQuickWorkout(true) })

        // 4. Wear OS kapcsolat és diagnosztika státusz sáv
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card2)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Watch, contentDescription = null, tint = colors.accent2)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(strings.getString(R.string.native_home_wear_watch), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(state.watchState.ifBlank { strings.getString(R.string.home_state_watch_unknown) }, color = colors.muted, fontSize = 13.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.watchConnected == true) colors.success else colors.muted)
                )
            }
        }
        // 2. Phase 5 #44: Mai állapot rács (Today Status 6-Metric Grid)
        Text(
            text = strings.getString(R.string.native_home_today_status),
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        TodayMetricsGrid(metrics = state.todayMetrics, language = language)


        NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach)

    }
}

/** Re-read local state on return and local-date changes. This does not request provider/network sync. */
@Composable
private fun HomeLocalRefresh(model: HomeViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(context, owner, model) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) { model.loadHomeData() }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_DATE_CHANGED)
            addAction(android.content.Intent.ACTION_TIME_CHANGED)
            addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.loadHomeData() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
}

@Composable
fun TodayMetricsGrid(metrics: TodayMetricsUiModel, language: String = "hu") {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val source = when (metrics.recovery.source) {
        NativeHomeRecoveryPresentation.Source.SAMSUNG -> strings.getString(R.string.native_home_recovery_samsung)
        NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE -> strings.getString(R.string.native_home_recovery_estimate)
        NativeHomeRecoveryPresentation.Source.NONE -> strings.getString(R.string.native_home_recovery_no_data)
    }
    val formattedTime = metrics.recovery.timestamp?.let { stamp ->
        val localTime = runCatching { Instant.parse(stamp).atZone(ZoneId.systemDefault()).toLocalTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(stamp).toLocalTime() }.getOrNull()
        localTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val calculatedTime = metrics.recovery.calculatedAt
        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
        ?.format(DateTimeFormatter.ofPattern("HH:mm"))
    val recoveryDetail = when {
        formattedTime != null -> "$source · $formattedTime"
        calculatedTime != null -> "$source · ${strings.getString(R.string.native_home_recovery_calculated)} $calculatedTime"
        else -> source
    }
    val recoveryNotices = listOfNotNull(
        strings.getString(R.string.native_home_recovery_partial).takeIf { metrics.recovery.partial },
        strings.getString(R.string.native_home_recovery_stale).takeIf { metrics.recovery.stale },
        strings.getString(R.string.native_home_recovery_read_error).takeIf { metrics.recovery.refreshFailed }
    ).joinToString(" · ")
    val items = listOf(
        HomeMetric("sleep", strings.getString(R.string.native_home_sleep), metrics.sleepText),
        HomeMetric("calories", strings.getString(R.string.native_home_active_calories), metrics.caloriesText),
        HomeMetric("pulse", strings.getString(R.string.native_home_pulse), metrics.pulseText),
        HomeMetric("steps", strings.getString(R.string.native_home_steps), metrics.stepsText, strings.getString(R.string.native_home_unit_steps)),
        HomeMetric("workouts", strings.getString(R.string.native_home_today_workout), metrics.workoutCountText, strings.getString(R.string.native_home_unit_workouts)),
        HomeMetric("recovery", strings.getString(R.string.native_home_recovery), metrics.recoveryText,
            detail = recoveryDetail, notice = recoveryNotices)
    )

    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 280.dp || fontScale > 1.3f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Gap)) {
            for (row in items.chunked(columns)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Gap)) {
                    for ((key, label, value, unit, detail, notice) in row) {
                        TrainPilotSurfaceCard(Modifier.weight(1f).testTag("home_metric_$key")) {
                            Text(label, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                            Text(value, color = colors.text, style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.testTag("home_metric_${key}_value"))
                            if (unit != null) Text(unit, color = colors.muted, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("home_metric_${key}_unit"))
                            if (detail != null) Text(detail, color = colors.muted, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("home_metric_${key}_source"))
                            if (!notice.isNullOrEmpty()) Text(notice, color = colors.muted, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("home_metric_${key}_status"))
                        }
                    }
                }
            }
        }
    }
}

private data class HomeMetric(val key: String, val label: String, val value: String,
    val unit: String? = null, val detail: String? = null, val notice: String? = null)

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSurfaceCard(modifier.heightIn(min = TrainPilotDimensions.Control), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(TrainPilotDimensions.Icon))
            Spacer(Modifier.width(TrainPilotDimensions.Gap))
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt

SHA-256: `c68bd7fe95f901cc168d120ef329d7d4228f0be7b78eec5e842d9a1126cb7a0b`

~~~~kotlin
package com.repforge.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.Duration
import kotlin.math.roundToInt

data class TodayMetricsUiModel(
    val sleepText: String = "—",
    val hrvText: String = "—",
    val caloriesText: String = "—",
    val pulseText: String = "—",
    val stepsText: String = "—",
    val workoutCountText: String = "—",
    val recoveryText: String = "—",
    val recovery: NativeHomeRecoveryPresentation.Model = NativeHomeRecoveryPresentation.Model(
        NativeHomeRecoveryPresentation.Source.NONE, null, null, null, false, false),
    val isPartial: Boolean = false
)

data class HomeWorkoutTarget(val programId: String, val dayId: String, val scheduleId: String? = null)

data class HomeUiState(
    val recommendedProgram: ProgramWithDays? = null,
    val nextTarget: HomeWorkoutTarget? = null,
    val nextDayExercisesCount: Int = 0,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val todayMetrics: TodayMetricsUiModel = TodayMetricsUiModel(),
    val watchConnected: Boolean? = null,
    val watchBattery: Int? = null,
    val hasImportedDraft: Boolean = false,
    val watchState: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = TrainPilotDatabase.getInstance(context)
    private val settingsStore = TrainPilotSettingsStore(context)
    private val repository = NativeHomeRepository(database)
    private var loading: Job? = null
    private val readEpoch = NativeHomeRecoveryLoadGate()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidations().catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { loadHomeData() }
        }
        viewModelScope.launch {
            // Theme/timer edits need no program/history read. Language and active program do.
            settingsStore.settingsFlow.map { it.activeProgramId to it.language }.distinctUntilChanged()
                .catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { loadHomeData() }
        }
        viewModelScope.launch {
            database.keyValueDocumentDao().observeDocument("draft")
                .catch { error -> if (error is CancellationException) throw error else failed() }
                .collect { document -> _uiState.update { it.copy(hasImportedDraft = document != null) } }
        }
    }

    /** Cancelling an older read prevents its result from overwriting a changed plan or dataset. */
    fun loadHomeData() {
        val epoch = readEpoch.next()
        loading?.cancel()
        loading = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadFailed = false) }
            try {
                val (result, admission) = withContext(Dispatchers.IO) {
                    NativeDataReplacement.mutate(context) {
                        val settings = settingsStore.current()
                        val now = Instant.now(); val zone = ZoneId.systemDefault()
                        val snapshot = repository.read(settings.activeProgramId, settings.language, now, zone)
                        val metrics = fetchTodayMetrics(now, zone, snapshot.workoutCount)
                        val liveHealth = WearLiveHealthStore.current(context)
                        HomeUiState(recommendedProgram = snapshot.program, nextTarget = snapshot.target,
                            nextDayExercisesCount = snapshot.exerciseCount, loading = false, todayMetrics = metrics,
                            watchConnected = liveHealth != null, watchState = liveHealth?.optString("state").orEmpty()) to
                            NativeDataReplacement.continuationForCurrentData(context)
                    }
                }
                admission.requireCurrent(context)
                if (readEpoch.isCurrent(epoch)) {
                    _uiState.update { result.copy(hasImportedDraft = it.hasImportedDraft) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (readEpoch.isCurrent(epoch)) failed() }
        }
    }

    private fun failed() {
        _uiState.update { it.copy(loading = false, loadFailed = true, recommendedProgram = null, nextTarget = null,
            nextDayExercisesCount = 0, todayMetrics = it.todayMetrics.copy(isPartial = true,
                recovery = it.todayMetrics.recovery.copy(stale = true, refreshFailed = true))) }
    }

    /**
     * Bounded canonical provider projection and recent effort records; the same
     * signal/readiness pipeline as Coach, without reading all history or writing
     * the journal. All SQLite, JSON and scoring work remains off the UI thread.
     */
    private suspend fun fetchTodayMetrics(now: Instant, zone: ZoneId, workoutCount: Int):
        TodayMetricsUiModel = withContext(Dispatchers.IO) {
        try {
            val today = now.atZone(zone).toLocalDate()
            val store = HealthJournalStore.get(context)
            val page = store.page(today.minusDays(14).toString(), today.toString(), "journal", "", 0L, 0L, 1)
            val summaries = page.optJSONArray("days")
            val days = (0 until (summaries?.length() ?: 0)).mapNotNull { summaries?.optJSONObject(it) }
            val dayObj = days.firstOrNull { it.optString("day") == today.toString() } ?: JSONObject()
            val history = database.workoutHistoryDao()
                .getCoachRecentHistory(now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli())
                .map { JSONObject(it) }
            val recovery = NativeHomeRecoveryPresentation.select(days, history, now, zone)

            val steps = dayObj.optDouble("steps", Double.NaN)
            val calories = dayObj.optDouble("activeCalories", Double.NaN)
            val pulse = dayObj.optDouble("averageHeartRate", Double.NaN)
            val hrv = dayObj.optDouble("hrvRmssdMs", Double.NaN)
            val sleepMinutes = dayObj.optDouble("sleepMinutes", Double.NaN)

            TodayMetricsUiModel(
                sleepText = if (sleepMinutes.isFinite() && sleepMinutes > 0.0) "${(sleepMinutes / 60.0 * 10.0).roundToInt() / 10.0} h" else "—",
                hrvText = if (hrv.isFinite() && hrv > 0.0) "${hrv.roundToInt()} ms" else "—",
                caloriesText = if (calories.isFinite() && calories > 0.0) "${calories.roundToInt()} kcal" else "—",
                pulseText = if (pulse.isFinite() && pulse > 0.0) "${pulse.roundToInt()} bpm" else "—",
                stepsText = if (steps.isFinite() && steps >= 0.0) steps.roundToInt().toString() else "—",
                workoutCountText = workoutCount.toString(),
                recoveryText = recovery.displayValue,
                recovery = recovery,
                isPartial = recovery.partial
            )
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            TodayMetricsUiModel(workoutCountText = workoutCount.toString(),
                recovery = NativeHomeRecoveryPresentation.Model(
                    NativeHomeRecoveryPresentation.Source.NONE, null, null, null,
                    partial = true, stale = true, refreshFailed = true), isPartial = true)
        }
    }
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt

SHA-256: `efc4f84b9b45c8d14afc24ad0f2b01a265c193209c683f3b2598731bcdc7d76f`

~~~~kotlin
package com.repforge.app.ui.home

import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.coach.NativeCoachTargets
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId

/** One Room snapshot: shared Coach selection, one program catalog read and indexed daily count. */
internal class NativeHomeRepository(private val db: TrainPilotDatabase) {
    data class Snapshot(val program: ProgramWithDays?, val target: HomeWorkoutTarget?,
        val exerciseCount: Int, val workoutCount: Int)

    suspend fun read(activeProgramId: String, language: String, now: Instant, zone: ZoneId): Snapshot = db.withTransaction {
        val today = now.atZone(zone).toLocalDate()
        val daysByProgram = db.programDao().getProgramDaysForSnapshot().groupBy { it.programId }
        val programs = db.programDao().getProgramRowsForSnapshot().map { program ->
            ProgramWithDays(program, daysByProgram[program.id].orEmpty())
        }
        val selected = NativeCoachTargets(db).select(activeProgramId, language, now, zone, programs)
        val program = selected?.let { target -> programs.single { it.program.id == target.programId } }
        val day = selected?.let { target -> program!!.days.single { it.dayId == target.dayId } }
        Snapshot(program, selected?.let { HomeWorkoutTarget(it.programId, it.dayId, it.schedule?.id) },
            day?.let { JSONArray(it.exercisesJson).length() } ?: 0,
            db.workoutHistoryDao().getWorkoutCountBetween(today.atStartOfDay(zone).toInstant().toEpochMilli(),
                today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()))
    }

    /** Observe real writes, including same-count edits. Never load full history to detect a change. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun invalidations() = callbackFlow<Unit> {
        val observer = object : InvalidationTracker.Observer("programs", "program_days", "calendar_entries",
            "workout_history", "history_tombstones") {
            override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
        }
        db.invalidationTracker.addObserver(observer)
        trySend(Unit)
        awaitClose { db.invalidationTracker.removeObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt

SHA-256: `02b69caab8bc42bec72b62eea588dd641dbaef296ad65d89418bcd4deea534b5`

~~~~kotlin
package com.repforge.app.ui.home

import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachSignals
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Only a display projection. Readiness recommendations may be capped by training
 * load or pain; the Home energy tile must never display that capped Coach score.
 * No persisted data is changed and no synthetic provider reading is produced.
 */
object NativeHomeRecoveryPresentation {
    enum class Source { SAMSUNG, TRAINPILOT_ESTIMATE, NONE }

    data class Model(
        val source: Source,
        val value: Double?,
        val timestamp: String?,
        val calculatedAt: Instant?,
        val partial: Boolean,
        val stale: Boolean,
        val refreshFailed: Boolean = false
    ) {
        val displayValue: String get() = value?.let {
            if (it.isFinite() && it >= 0.0 && it <= 100.0) "${kotlin.math.round(it).toInt()}%" else "—"
        } ?: "—"
    }

    fun select(
        days: List<JSONObject>,
        history: List<JSONObject>,
        now: Instant,
        zone: ZoneId,
        refreshFailed: Boolean = false
    ): Model {
        val today = now.atZone(zone).toLocalDate()
        val signals = NativeCoachSignals.fromDays(days, today, zone)
        val readiness = NativeCoachReadiness.calculate(signals.recovery, signals.daily, history, now, zone)
        val partial = signals.daily.optBoolean("partial", false) ||
            signals.recovery.optBoolean("partial", false)
        val older = signals.recovery.optString("day").let { stamp ->
            runCatching { LocalDate.parse(stamp) }.getOrNull()?.let { it < today } == true
        }
        val unverifiedEnergy = (days.any { row ->
            row.has("energyScore") && !row.isNull("energyScore")
        } && readiness.samsungScore == null)

        if (readiness.samsungScore != null) {
            return Model(Source.SAMSUNG, readiness.samsungScore, readiness.samsungTime,
                null, partial, false, refreshFailed)
        }

        // NativeCoachReadiness currently rejects exactly zero (<= 0). Preserve
        // its *existing* source, time, today, and 36h validators by invoking it
        // with a positive validation probe on a defensive copy. Only a genuine
        // finite zero is subsequently presented as zero; never as a Coach score.
        // Remove this compatibility shim if the shared validator accepts zero.
        for (record in listOf(signals.daily, signals.recovery)) {
            val raw = record.opt("energyScore")
            val numeric = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> null
            }
            if (numeric == null || !numeric.isFinite() || numeric != 0.0) continue
            val probe = JSONObject(record.toString()).put("energyScore", 1.0)
            val verification = NativeCoachReadiness.calculate(
                signals.recovery, signals.daily, history, now, zone,
                externalEnergy = probe, externalEnergyAvailable = true)
            if (verification.samsungScore == 1.0) {
                return Model(Source.SAMSUNG, 0.0, verification.samsungTime,
                    null, partial, false, refreshFailed)
            }
        }

        if (readiness.trainpilotScore != null && readiness.trainpilotObservations > 0) {
            return Model(Source.TRAINPILOT_ESTIMATE, readiness.trainpilotScore.toDouble(),
                null, now, partial, older || unverifiedEnergy, refreshFailed)
        }
        return Model(Source.NONE, null, null, null, partial, older || unverifiedEnergy, refreshFailed)
    }
}

/** Exact-result admission across cancel/reload/locale/date/zone and restore invalidations. */
internal class NativeHomeRecoveryLoadGate {
    private val generation = java.util.concurrent.atomic.AtomicLong()
    fun next(): Long = generation.incrementAndGet()
    fun isCurrent(ticket: Long): Boolean = ticket == generation.get()
}

~~~~

---

## android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt

SHA-256: `1951f377a6a72254361d921bcc82f4c0613c9563fca364dcf9a87ca77bb024b8`

~~~~kotlin
package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions
import com.repforge.app.ui.components.TrainPilotSurfaceCard
import com.repforge.app.ui.components.TrainPilotSectionHeader
import com.repforge.app.ui.components.TrainPilotPrimaryButton
import org.json.JSONArray
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext

/** Main's Training entry shows quick start and active program days; Programs remains separate. */
@Composable
internal fun TrainingLandingScreen(language: String, activeProgramId: String, busy: Boolean,
    onStart: (String, String) -> Unit, onQuick: (Boolean) -> Unit, onPrograms: () -> Unit,
    onCalendar: () -> Unit, onCoach: () -> Unit, onLibrary: () -> Unit,
    model: TrainingProgramsViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val labels = listOf(
        resources.getString(R.string.native_training_landing_00),
        resources.getString(R.string.native_training_landing_01),
        resources.getString(R.string.native_training_landing_02),
        resources.getString(R.string.native_training_landing_03),
        resources.getString(R.string.native_training_landing_04),
        resources.getString(R.string.native_training_landing_05),
        resources.getString(R.string.native_training_landing_06),
        resources.getString(R.string.native_training_landing_07),
        resources.getString(R.string.native_training_landing_08),
        resources.getString(R.string.native_training_landing_09)
    )
    fun name(hu: String, en: String, de: String, ro: String) = when(language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }
    val program = state.programs.firstOrNull { it.program.id == activeProgramId }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TrainPilotSurfaceCard(Modifier.fillMaxWidth()) {
                TrainPilotSectionHeader(labels[1])
                TrainPilotPrimaryButton(labels[2], { onQuick(false) }, Modifier.fillMaxWidth(), !busy)
                OutlinedButton(onClick = onLibrary, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.Control)) { Text(labels[9]) }
                OutlinedButton(onClick = { onQuick(true) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.Control)) { Text(labels[3]) }
            }
        }
        item { NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach) }
        item {
            TrainPilotSectionHeader(labels[4])
            Text(program?.program?.let { name(it.nameHu, it.nameEn, it.nameDe, it.nameRo) } ?: labels[7], color = colors.text)
            TextButton(onClick = onPrograms, enabled = !busy) { Text(labels[5]) }
        }
        items(program?.days.orEmpty().sortedBy { it.sortOrder }, key = { it.compositeId }) { day ->
            TrainPilotSurfaceCard(Modifier.fillMaxWidth()) {
                Text(name(day.nameHu, day.nameEn, day.nameDe, day.nameRo), color = colors.text,
                    style = MaterialTheme.typography.titleMedium)
                Text("${runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)} ${labels[8]}",
                    color = colors.muted, style = MaterialTheme.typography.bodySmall)
                TrainPilotPrimaryButton(labels[6], { onStart(day.programId, day.dayId) }, Modifier.fillMaxWidth(), !busy)
            }
        }
        item { Spacer(Modifier.height(TrainPilotDimensions.Page)) }
    }
}

~~~~
