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

        // The three Studio hero states come from the real session/target, never a demo switch.
        TrainPilotHeroSurface(Modifier.fillMaxWidth().testTag("home_today_hero")) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainPilotIconBadge(Icons.Default.FitnessCenter)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.getString(if (hasActiveSession) R.string.native_workout_component_resume else R.string.native_home_next_workout),
                        color = colors.accentText, style = MaterialTheme.typography.labelMedium)
                    Text(if (hasActiveSession) activeSession?.optString("programName").orEmpty()
                        .ifBlank { strings.getString(R.string.native_home_quick_workout) }
                    else state.recommendedProgram?.program?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(R.string.home_state_no_program),
                        Modifier.testTag("home_program_name"), color = colors.muted,
                        style = MaterialTheme.typography.bodySmall)
                    Text(if (hasActiveSession) activeSession?.optString("dayName").orEmpty()
                        .ifBlank { strings.getString(R.string.native_workout_component_resume) }
                    else state.recommendedProgram?.days?.firstOrNull { it.dayId == target?.dayId }?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(if (state.loading) R.string.home_state_loading else R.string.home_state_no_target),
                        Modifier.testTag("home_day_name"), color = colors.text,
                        style = MaterialTheme.typography.titleLarge)
                    if (!hasActiveSession && target != null) Text(
                        strings.getString(R.string.native_home_planned_exercises, state.nextDayExercisesCount),
                        color = colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = { if (hasActiveSession) onQuickWorkout(false)
                    else target?.let { onStartWorkout(it.programId, it.dayId, it.scheduleId) } },
                enabled = !busy && (hasActiveSession || (!state.loading && target != null)),
                modifier = Modifier.fillMaxWidth().heightIn(min = TrainPilotDimensions.PrimaryAction)
                    .testTag("home_start_workout"), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(strings.getString(if (hasActiveSession) R.string.native_workout_component_resume else R.string.native_home_start_workout),
                    style = MaterialTheme.typography.labelLarge)
            }
        }

        // Navigation stays available during local data loading; no duplicate section title.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f
            val gym: @Composable (Modifier) -> Unit = { modifier ->
                QuickActionButton(strings.getString(R.string.native_home_gym),
                    strings.getString(R.string.native_home_select_exercise), Icons.Default.FitnessCenter,
                    modifier.testTag("home_quick_workout"), enabled = !busy, onClick = { onQuickWorkout(false) })
            }
            val outdoor: @Composable (Modifier) -> Unit = { modifier ->
                QuickActionButton(strings.getString(R.string.native_home_outdoor_gps),
                    strings.getString(R.string.native_home_run_walk), Icons.Default.DirectionsRun,
                    modifier.testTag("home_outdoor_workout"), enabled = !busy, onClick = { onQuickWorkout(true) })
            }
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                gym(Modifier.fillMaxWidth()); outdoor(Modifier.fillMaxWidth())
            } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                gym(Modifier.weight(1f).fillMaxHeight()); outdoor(Modifier.weight(1f).fillMaxHeight())
            }
        }

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
        TodayMetricsGrid(metrics = state.todayMetrics, language = language,
            onMetricClick = { key -> if (!busy) { if (key == "recovery") onCoach() else onHealth() } })


        NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach)
        TrainPilotSurfaceCard(Modifier.fillMaxWidth().testTag("home_coach_preview"),
            onClick = onCoach, enabled = !busy) {
            TrainPilotSectionHeader(NativeMainMenu.title("coach", language))
            Text(strings.getString(R.string.native_home_recovery), style = MaterialTheme.typography.bodySmall,
                color = colors.muted)
            Text(state.todayMetrics.recoveryText, style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = TrainPilotNumericFont))
            val sourceLabel = when (state.todayMetrics.recovery.source) {
                NativeHomeRecoveryPresentation.Source.SAMSUNG -> R.string.native_home_recovery_samsung
                NativeHomeRecoveryPresentation.Source.TRAINPILOT_ESTIMATE -> R.string.native_home_recovery_estimate
                NativeHomeRecoveryPresentation.Source.NONE -> R.string.native_home_recovery_no_data
            }
            Text(strings.getString(sourceLabel), color = colors.muted, style = MaterialTheme.typography.bodySmall)
            val notices = listOfNotNull(
                strings.getString(R.string.native_home_recovery_partial).takeIf { state.todayMetrics.recovery.partial },
                strings.getString(R.string.native_home_recovery_stale).takeIf { state.todayMetrics.recovery.stale },
                strings.getString(R.string.native_home_recovery_read_error).takeIf { state.todayMetrics.recovery.refreshFailed }
            ).joinToString(" · ")
            if (notices.isNotEmpty()) Text(notices, color = colors.muted, style = MaterialTheme.typography.bodySmall)
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
