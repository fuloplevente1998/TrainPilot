package com.repforge.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.text.nativeLocalizedName
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
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
    busy: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        NativePlanningShortcuts(language, !busy, onPrograms, onCalendar, onCoach)
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
                        text = strings.getString(R.string.native_home_next_workout),
                        color = colors.accentText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = state.recommendedProgram?.program?.let {
                            nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                        } ?: strings.getString(R.string.home_state_no_program),
                        modifier = Modifier.testTag("home_program_name"),
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.recommendedProgram?.days?.firstOrNull { it.dayId == target?.dayId }?.let {
                        nativeLocalizedName(language, it.nameHu, it.nameEn, it.nameDe, it.nameRo)
                    } ?: strings.getString(if (state.loading) R.string.home_state_loading else R.string.home_state_no_target),
                    modifier = Modifier.testTag("home_day_name"),
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (target != null) Text(
                    text = strings.getString(R.string.native_home_planned_exercises, state.nextDayExercisesCount),
                    color = colors.muted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { target?.let { onStartWorkout(it.programId, it.dayId, it.scheduleId) } },
                    enabled = !busy && !state.loading && target != null,
                    modifier = Modifier.fillMaxWidth().testTag("home_start_workout"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.onAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.getString(R.string.native_home_start_workout), color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
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

        // 3. Gyorsindítás szekció (Quick Workout Buttons)
        Text(
            text = strings.getString(R.string.native_home_quick_workout),
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionButton(
                title = strings.getString(R.string.native_home_gym),
                subtitle = strings.getString(R.string.native_home_select_exercise),
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f),
                onClick = { onQuickWorkout(false) }
            )
            QuickActionButton(
                title = strings.getString(R.string.native_home_outdoor_gps),
                subtitle = strings.getString(R.string.native_home_run_walk),
                icon = Icons.Default.DirectionsRun,
                modifier = Modifier.weight(1f),
                onClick = { onQuickWorkout(true) }
            )
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
                        Text(state.watchState.ifBlank { strings.getString(R.string.home_state_watch_unknown) }, color = colors.muted, fontSize = 11.sp)
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
    val items = listOf(
        HomeMetric("sleep", strings.getString(R.string.native_home_sleep), metrics.sleepText),
        HomeMetric("calories", strings.getString(R.string.native_home_active_calories), metrics.caloriesText),
        HomeMetric("pulse", strings.getString(R.string.native_home_pulse), metrics.pulseText),
        HomeMetric("steps", strings.getString(R.string.native_home_steps), metrics.stepsText, strings.getString(R.string.native_home_unit_steps)),
        HomeMetric("workouts", strings.getString(R.string.native_home_today_workout), metrics.workoutCountText, strings.getString(R.string.native_home_unit_workouts)),
        HomeMetric("recovery", strings.getString(R.string.native_home_recovery), metrics.recoveryText, strings.getString(R.string.native_home_unit_readiness))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(3)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, label, value, unit) in row) {
                    Card(
                        modifier = Modifier.weight(1f).testTag("home_metric_$key"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = label, color = colors.muted, fontSize = 10.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = value,
                                modifier = Modifier.testTag("home_metric_${key}_value"),
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (unit != null) Text(unit, color = colors.muted, fontSize = 10.sp,
                                modifier = Modifier.testTag("home_metric_${key}_unit"))
                        }
                    }
                }
            }
        }
    }
}

private data class HomeMetric(val key: String, val label: String, val value: String, val unit: String? = null)

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}
