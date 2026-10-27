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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.repforge.app.ui.theme.TrainPilotNumericFont
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.components.TrainPilotPrimaryButton
import com.repforge.app.ui.navigation.NativeMainMenu
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.repforge.app.ui.components.TrainPilotIconBadge
import com.repforge.app.ui.components.TrainPilotMetricSurface
import com.repforge.app.ui.components.trainPilotMetricIcon
import com.repforge.app.ui.components.TrainPilotHeroSurface
import com.repforge.app.ui.components.TrainPilotStatusBadge
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
    onOnboarding: () -> Unit = {},
    onProfile: () -> Unit = {},
    onWatch: () -> Unit = onProfile,
    onHealth: () -> Unit = {},
    onPersonalPlan: () -> Unit = {}
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
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
        Text(strings.getString(R.string.studio_home_greeting),
            style = MaterialTheme.typography.headlineLarge, color = colors.text,
            modifier = Modifier.testTag("home_studio_greeting"))
        Text(strings.getString(if (hasActiveSession) R.string.studio_home_active_intro else R.string.studio_home_overview),
            style = MaterialTheme.typography.bodyLarge, color = colors.muted)
        TrainPilotSectionSurface(Modifier.fillMaxWidth().testTag("home_personal_planner")) {
            Text(strings.getString(R.string.studio_home_planner_title), style = MaterialTheme.typography.titleMedium)
            Text(strings.getString(R.string.studio_home_planner_info), style = MaterialTheme.typography.bodySmall,
                color = colors.muted)
            TrainPilotPrimaryButton(strings.getString(R.string.studio_home_new_plan), onPersonalPlan,
                Modifier.fillMaxWidth().testTag("home_personal_plan"), !busy)
            androidx.compose.material3.TextButton(onClick = onProfile, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.Control).testTag("home_profile_open")) {
                Text(strings.getString(R.string.native_settings_profile_title))
            }
        }

        // Accepted 18:49 Studio hero structure: 56dp warm-gold icon, real status,
        // primary action, and a details affordance, not a generic native card.
        TrainPilotHeroSurface(Modifier.fillMaxWidth()
            .testTag("home_today_hero")) {
            Row(Modifier.fillMaxWidth().testTag("home_hero_workout_card")
                .clickable(enabled = !busy, role = Role.Button) { onPrograms() },
                verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp)
                        .border(1.dp, colors.accent.copy(alpha = .3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp), color = colors.accent.copy(alpha = .14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null,
                            modifier = Modifier.size(28.dp), tint = colors.accentText)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val programName = if (hasActiveSession) {
                        activeSession?.optString("programName").orEmpty().ifBlank {
                            strings.getString(R.string.native_home_quick_workout)
                        }
                    } else state.recommendedProgram?.program?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(R.string.home_state_no_program)
                    Text(strings.getString(if (hasActiveSession)
                        R.string.native_workout_component_resume else R.string.native_home_next_workout),
                        color = colors.accentText, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                    Text(if (hasActiveSession) activeSession?.optString("dayName").orEmpty()
                        .ifBlank { strings.getString(R.string.native_workout_component_resume) }
                    else state.recommendedProgram?.days?.firstOrNull { it.dayId == target?.dayId }?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(if (state.loading)
                        R.string.home_state_loading else R.string.home_state_no_target),
                        Modifier.testTag("home_day_name"), color = colors.text,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp))
                    Text(programName, Modifier.testTag("home_program_name"),
                        color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                    if (!hasActiveSession && target != null)
                        Text(strings.getString(R.string.native_home_planned_exercises,
                            state.nextDayExercisesCount), color = colors.muted,
                            style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
            }
            Button(
                onClick = {
                    if (hasActiveSession) onQuickWorkout(false)
                    else target?.let { onStartWorkout(it.programId, it.dayId, it.scheduleId) }
                },
                enabled = !busy && (hasActiveSession || (!state.loading && target != null)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                    .testTag("home_start_workout"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent,
                    contentColor = colors.onAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp).testTag("home_continue_workout_button"))
                Spacer(Modifier.width(8.dp))
                Text(strings.getString(if (hasActiveSession)
                    R.string.native_workout_component_resume else R.string.native_home_start_workout),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Studio 18:49 quick-workout strip, separate from the 3 navigation tiles.
        // Outdoor/GPS workout remains reachable on the real Training tab.
        StudioHomeNavigationRow(
            title = strings.getString(R.string.native_home_quick_workout),
            subtitle = strings.getString(R.string.native_home_select_exercise),
            icon = Icons.Default.DirectionsRun,
            modifier = Modifier.fillMaxWidth().testTag("home_quick_workout_card"), enabled = !busy,
            onClick = { onQuickWorkout(false) }
        )

        // Studio 18:49 separate Wear card: no hard-coded model/battery/online indicator.
        StudioHomeNavigationRow(
            title = strings.getString(R.string.native_home_wear_watch),
            subtitle = state.watchState.ifBlank {
                strings.getString(R.string.home_state_watch_unknown)
            },
            icon = Icons.Default.Watch,
            modifier = Modifier.fillMaxWidth().testTag("home_watch_status_card"),
            connected = state.watchConnected,
            enabled = !busy,
            onClick = onWatch
        )

        // Exactly the source's 2 × 2 health/readiness bento composition.
        // Real unavailable native provider measurements are displayed as missing.
        StudioHomeVitals(state.todayMetrics, language, onHealth, onCoach, !busy)

        // Studio's three compact program/calendar/Coach shortcuts, not
        // the former native AssistChip row. These keep the real callbacks.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f
            val options = listOf(
                Triple(strings.getString(R.string.native_home_next_workout),
                    Icons.Default.ViewAgenda, onPrograms),
                Triple(NativeMainMenu.title("calendar", language),
                    Icons.Default.CalendarMonth, onCalendar),
                Triple(NativeMainMenu.title("coach", language),
                    Icons.Default.Psychology, onCoach)
            )
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEachIndexed { index, (title, icon, action) ->
                    StudioHomeShortcut(title, icon,
                        Modifier.fillMaxWidth().testTag(
                            listOf("home_shortcut_programs", "home_shortcut_calendar",
                                "home_shortcut_coach")[index]), !busy, action)
                }
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEachIndexed { index, (title, icon, action) ->
                    StudioHomeShortcut(title, icon,
                        Modifier.weight(1f).testTag(
                            listOf("home_shortcut_programs", "home_shortcut_calendar",
                                "home_shortcut_coach")[index]), !busy, action)
                }
            }
        }

        // Original Studio lower cards: the existing real active-calories/workout
        // count and Coach readiness are surfaced with *no invented advice*.
        StudioHomeGoalsRow(state.todayMetrics, strings.getString(R.string.native_home_today_status),
            !busy, onPersonalPlan)
        StudioHomeCoachCard(state, language, !busy, onCoach)

    }
}

@Composable
private fun StudioHomeNavigationRow(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, connected: Boolean? = null,
    enabled: Boolean, onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Surface(modifier.heightIn(min = 48.dp)
        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
        .clickable(enabled = enabled, role = Role.Button) { onClick() },
        shape = RoundedCornerShape(18.dp), color = colors.card) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(12.dp),
                color = colors.card2) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.accentText,
                        modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
            if (connected != null) Box(Modifier.size(14.dp).clip(CircleShape)
                .background(if (connected) colors.success else colors.muted))
            else Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.muted)
        }
    }
}

@Composable
private fun StudioHomeVitals(
    metrics: TodayMetricsUiModel,
    language: String,
    onHealth: () -> Unit, onCoach: () -> Unit,
    enabled: Boolean
) {
    val strings = nativeResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    val recoverySource = when (metrics.recovery.source) {
        NativeHomeRecoveryPresentation.Source.SAMSUNG ->
            strings.getString(R.string.native_home_recovery_samsung)
        NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE ->
            strings.getString(R.string.native_home_recovery_estimate)
        NativeHomeRecoveryPresentation.Source.NONE ->
            strings.getString(R.string.native_home_recovery_no_data)
    }
    val tiles = listOf(
        StudioHomeVital("sleep", strings.getString(R.string.native_home_sleep),
            metrics.sleepText, null, Icons.Default.Bedtime, onHealth),
        StudioHomeVital("pulse", strings.getString(R.string.native_home_pulse),
            metrics.pulseText, null, Icons.Default.Favorite, onHealth),
        StudioHomeVital("steps", strings.getString(R.string.native_home_steps),
            metrics.stepsText, strings.getString(R.string.native_home_unit_steps),
            Icons.Default.DirectionsRun, onHealth),
        StudioHomeVital("recovery", strings.getString(R.string.native_home_recovery),
            metrics.recoveryText, recoverySource, Icons.Default.Eco, onCoach)
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val count = if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tiles.chunked(count).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { tile ->
                        Surface(Modifier.weight(1f).fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .testTag("home_metric_${tile.key}")
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .clickable(enabled = enabled, role = Role.Button) { tile.onClick() },
                            color = colors.card, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(tile.icon, contentDescription = null,
                                        tint = colors.accentText, modifier = Modifier.size(22.dp))
                                    Text(tile.title, color = colors.muted,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(tile.value, color = colors.text,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.testTag("home_metric_${tile.key}_value"))
                                tile.detail?.let { detail ->
                                    Text(detail, color = colors.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.testTag("home_metric_${tile.key}_source"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private data class StudioHomeVital(
    val key: String, val title: String, val value: String, val detail: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit
)

@Composable
private fun StudioHomeShortcut(title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    Surface(modifier.heightIn(min = 48.dp)
        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
        .clickable(enabled = enabled, role = Role.Button) { onClick() },
        shape = RoundedCornerShape(16.dp), color = colors.card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = colors.accentText,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(title, color = colors.text, style = MaterialTheme.typography.titleSmall,
                maxLines = 2)
        }
    }
}

@Composable
private fun StudioHomeGoalsRow(metrics: TodayMetricsUiModel,
    title: String, enabled: Boolean, onOpen: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    Surface(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .testTag("home_goals_row_card")
        .border(1.dp, colors.line, RoundedCornerShape(16.dp))
        .clickable(enabled = enabled, role = Role.Button) { onOpen() },
        shape = RoundedCornerShape(16.dp), color = colors.card) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, style = MaterialTheme.typography.titleMedium)
                Text(metrics.caloriesText, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(metrics.workoutCountText, color = colors.accentText,
                style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.accentText)
        }
    }
}

@Composable
private fun StudioHomeCoachCard(state: HomeUiState,
    language: String, enabled: Boolean, onOpen: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, language)
    Surface(Modifier.fillMaxWidth().testTag("home_coach_preview")
        .border(1.dp, colors.accent.copy(alpha = .45f), RoundedCornerShape(18.dp))
        .clickable(enabled = enabled, role = Role.Button) { onOpen() },
        shape = RoundedCornerShape(18.dp), color = colors.card) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().testTag("home_coach_advice_card"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = colors.accentText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(NativeMainMenu.title("coach", language),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = colors.accentText)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.accentText)
            }
            Text(strings.getString(R.string.native_home_recovery),
                style = MaterialTheme.typography.titleLarge, color = colors.text)
            val source = when (state.todayMetrics.recovery.source) {
                NativeHomeRecoveryPresentation.Source.SAMSUNG ->
                    R.string.native_home_recovery_samsung
                NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE ->
                    R.string.native_home_recovery_estimate
                NativeHomeRecoveryPresentation.Source.NONE ->
                    R.string.native_home_recovery_no_data
            }
            Text(strings.getString(source), color = colors.muted,
                style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                    color = colors.card2, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(strings.getString(R.string.native_home_recovery),
                            color = colors.muted, style = MaterialTheme.typography.bodySmall)
                        Text(state.todayMetrics.recoveryText,
                            color = colors.text, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Surface(Modifier.weight(1f).border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                    color = colors.card2, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(strings.getString(R.string.native_home_next_workout),
                            color = colors.muted, style = MaterialTheme.typography.bodySmall)
                        Text(state.recommendedProgram?.program?.let {
                            nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                        } ?: strings.getString(R.string.home_state_no_program),
                            color = colors.text, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
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
fun TodayMetricsGrid(metrics: TodayMetricsUiModel, language: String = "hu", onMetricClick: ((String) -> Unit)? = null) {
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
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Gap)) {
                    for ((key, label, value, unit, detail, notice) in row) {
                        TrainPilotMetricSurface(Modifier.weight(1f).fillMaxHeight().testTag("home_metric_$key")) {
                            Row(Modifier.fillMaxWidth().then(if (onMetricClick != null)
                                Modifier.heightIn(min = TrainPilotDimensions.Control).clickable(role = Role.Button) { onMetricClick(key) }
                                else Modifier), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(trainPilotMetricIcon(key), null, Modifier.size(18.dp), tint = colors.accentText)
                                Text(label, color = colors.muted, modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            Text(value, color = colors.text, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp, fontFamily = TrainPilotNumericFont, fontFeatureSettings = "tnum"),
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSurfaceCard(modifier.heightIn(min = TrainPilotDimensions.Control), onClick = onClick, enabled = enabled) {
        TrainPilotIconBadge(icon)
        Text(title, color = colors.text, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
        Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodySmall)
    }
}
