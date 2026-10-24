package com.repforge.app.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.R
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.util.Date

@Composable
fun HomeScreen(
    hasActiveWorkout: Boolean,
    activeWorkoutTitle: String,
    isWorkoutPaused: Boolean,
    activeWorkoutOwner: String,
    onResumeActiveWorkout: () -> Unit,
    onStartRecommendedWorkout: (String, String, String?) -> Unit,
    onOpenTraining: () -> Unit,
    onPrograms: () -> Unit,
    onCalendar: () -> Unit,
    onCoach: () -> Unit,
    onResumeDraft: () -> Unit,
    onQuickWorkout: () -> Unit,
    language: String,
    busy: Boolean = false,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(language) { viewModel.loadHomeData(language) }
    HomeScreenContent(
        state = state,
        hasActiveWorkout = hasActiveWorkout,
        activeWorkoutTitle = activeWorkoutTitle,
        isWorkoutPaused = isWorkoutPaused,
        activeWorkoutOwner = activeWorkoutOwner,
        onResumeActiveWorkout = onResumeActiveWorkout,
        onStartRecommendedWorkout = onStartRecommendedWorkout,
        onOpenTraining = onOpenTraining,
        onPrograms = onPrograms,
        onCalendar = onCalendar,
        onCoach = onCoach,
        onResumeDraft = onResumeDraft,
        onQuickWorkout = onQuickWorkout,
        onRetryMetrics = { viewModel.loadHomeData(language) },
        language = language,
        busy = busy
    )
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    hasActiveWorkout: Boolean,
    activeWorkoutTitle: String,
    isWorkoutPaused: Boolean,
    activeWorkoutOwner: String,
    onResumeActiveWorkout: () -> Unit,
    onStartRecommendedWorkout: (String, String, String?) -> Unit,
    onOpenTraining: () -> Unit,
    onPrograms: () -> Unit,
    onCalendar: () -> Unit,
    onCoach: () -> Unit,
    onResumeDraft: () -> Unit,
    onQuickWorkout: () -> Unit,
    onRetryMetrics: () -> Unit,
    language: String,
    busy: Boolean
) {
    val context = LocalContext.current
    val resources = remember(context, language) { nativeResources(context, language) }
    val colors = LocalTrainPilotColors.current
    val scrollState = rememberScrollState()
    val hasRecovery = hasActiveWorkout || state.hasImportedDraft
    val quickWorkoutEnabled = !busy && !hasRecovery

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hasActiveWorkout) {
            ActiveWorkoutCard(
                title = activeWorkoutTitle.ifBlank { resources.getString(R.string.home_active_workout) },
                isPaused = isWorkoutPaused,
                isWatchOwned = activeWorkoutOwner == "watch",
                enabled = !busy,
                resources = resources,
                onClick = onResumeActiveWorkout
            )
        } else if (state.hasImportedDraft) {
            DraftResumeCard(
                enabled = !busy,
                resources = resources,
                onClick = onResumeDraft
            )
        }

        if (!hasRecovery) {
            RecommendedWorkoutSection(
                recommendation = state.recommendedWorkout,
                loading = state.isLoading,
                error = state.recommendationError,
                enabled = !busy,
                resources = resources,
                onStart = { recommendation ->
                    onStartRecommendedWorkout(
                        recommendation.programId,
                        recommendation.dayId,
                        recommendation.scheduleId
                    )
                },
                onOpenTraining = onOpenTraining,
                onRetry = onRetryMetrics
            )
        }

        QuickWorkoutCard(
            enabled = quickWorkoutEnabled,
            blocked = hasRecovery || busy,
            resources = resources,
            onClick = onQuickWorkout
        )

        NativePlanningShortcuts(
            language = language,
            enabled = !busy,
            onPrograms = onPrograms,
            onCalendar = onCalendar,
            onCoach = onCoach
        )

        WatchStatusCard(
            updatedAt = state.watchUpdatedAt,
            resources = resources
        )

        Text(
            text = resources.getString(R.string.home_today_status),
            color = colors.text,
            style = MaterialTheme.typography.titleLarge
        )
        if (state.todayMetrics.isPartial) {
            Text(
                text = resources.getString(R.string.home_metrics_partial),
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        when {
            state.isLoading && state.todayMetrics == TodayMetricsUiModel() ->
                HomeMetricsSkeleton(resources.getString(R.string.home_loading))
            state.todayMetrics.isError -> MetricsError(
                resources = resources,
                onRetry = onRetryMetrics
            )
            else -> TodayMetricsGrid(state.todayMetrics, resources)
        }
    }
}

@Composable
private fun RecommendedWorkoutSection(
    recommendation: HomeWorkoutRecommendation?,
    loading: Boolean,
    error: Boolean,
    enabled: Boolean,
    resources: android.content.res.Resources,
    onStart: (HomeWorkoutRecommendation) -> Unit,
    onOpenTraining: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = resources.getString(R.string.home_recommended_workout),
            color = colors.text,
            style = MaterialTheme.typography.titleLarge
        )
        when {
            error -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(resources.getString(R.string.home_recommendation_error), color = colors.text)
                    Button(
                        onClick = onRetry,
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(resources.getString(R.string.home_retry))
                    }
                }
            }
            loading && recommendation == null -> Text(
                text = resources.getString(R.string.home_loading),
                color = colors.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            recommendation == null -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.card)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(resources.getString(R.string.home_no_recommendation), color = colors.muted)
                    Button(
                        onClick = onOpenTraining,
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("home_open_training")
                    ) {
                        Text(resources.getString(R.string.home_open_training))
                    }
                }
            }
            else -> Card(
                modifier = Modifier.fillMaxWidth().testTag("home_recommended_workout"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = BorderStroke(1.dp, colors.accent)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = recommendation.dayTitle.ifBlank { recommendation.programTitle },
                        color = colors.text,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (recommendation.programTitle.isNotBlank()) {
                        Text(
                            text = recommendation.programTitle,
                            color = colors.muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = { onStart(recommendation) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("home_recommended_action"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            resources.getString(R.string.home_start_recommended_workout),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutCard(
    title: String,
    isPaused: Boolean,
    isWatchOwned: Boolean,
    enabled: Boolean,
    resources: android.content.res.Resources,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val status = when {
        isWatchOwned && isPaused -> resources.getString(R.string.home_workout_paused_watch)
        isPaused -> resources.getString(R.string.home_workout_paused)
        isWatchOwned -> resources.getString(R.string.home_watch_controls_workout)
        else -> resources.getString(R.string.home_workout_running)
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_active_workout"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.accent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = colors.text,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = status,
                color = colors.accentText,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_active_workout_action"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    text = resources.getString(
                        if (isWatchOwned) R.string.home_view_workout else R.string.home_resume_workout
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DraftResumeCard(
    enabled: Boolean,
    resources: android.content.res.Resources,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_imported_draft"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = resources.getString(R.string.home_draft_title),
                color = colors.text,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = resources.getString(R.string.home_draft_description),
                color = colors.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_resume_draft"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                )
            ) {
                Text(resources.getString(R.string.home_resume_workout), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickWorkoutCard(
    enabled: Boolean,
    blocked: Boolean,
    resources: android.content.res.Resources,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .testTag("home_quick_workout"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.line)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resources.getString(R.string.home_quick_workout),
                    color = colors.text,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = resources.getString(
                        if (blocked) R.string.home_quick_workout_blocked else R.string.home_quick_workout_description
                    ),
                    color = colors.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (enabled) colors.accentText else colors.muted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WatchStatusCard(
    updatedAt: Long?,
    resources: android.content.res.Resources
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val status = updatedAt?.let {
        resources.getString(
            R.string.home_watch_last_update,
            DateFormat.getTimeFormat(context).format(Date(it))
        )
    } ?: resources.getString(R.string.home_watch_no_data)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_watch_status"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card2)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Watch, contentDescription = null, tint = colors.accent2)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = resources.getString(R.string.home_watch_title),
                    color = colors.text,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = status,
                    color = colors.muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HomeMetricsSkeleton(label: String) {
    val colors = LocalTrainPilotColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Card(
                        modifier = Modifier.weight(1f).heightIn(min = 82.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.card)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier.width(84.dp).height(12.dp).clip(RoundedCornerShape(6.dp))
                                    .background(colors.card2)
                            )
                            Box(
                                Modifier.width(54.dp).height(18.dp).clip(RoundedCornerShape(6.dp))
                                    .background(colors.card2)
                            )
                        }
                    }
                }
            }
        }
        Text(label, color = colors.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MetricsError(
    resources: android.content.res.Resources,
    onRetry: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(resources.getString(R.string.home_metrics_error), color = colors.text)
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp).testTag("home_metrics_retry")
            ) {
                Text(resources.getString(R.string.home_retry))
            }
        }
    }
}

@Composable
internal fun TodayMetricsGrid(
    metrics: TodayMetricsUiModel,
    resources: android.content.res.Resources
) {
    val unknown = resources.getString(R.string.home_no_value)
    val sleep = metrics.sleepMinutes?.let { minutes ->
        resources.getString(R.string.home_sleep_value, minutes / 60, minutes % 60)
    } ?: unknown
    val pulse = metrics.restingHeartRate?.let { resources.getString(R.string.home_bpm_value, it) } ?: unknown
    val steps = metrics.steps?.let { resources.getString(R.string.home_steps_value, it) } ?: unknown
    val energy = metrics.samsungEnergy?.let { resources.getString(R.string.home_energy_value, it) } ?: unknown
    val source = if (
        metrics.samsungEnergy != null &&
        metrics.samsungEnergySource in setOf("samsung_health", "com.sec.android.app.shealth")
    ) resources.getString(R.string.home_samsung_health) else null
    val items = listOf(
        Triple(R.string.home_sleep, sleep, null),
        Triple(R.string.home_resting_pulse, pulse, null),
        Triple(R.string.home_steps, steps, null),
        Triple(R.string.home_samsung_energy, energy, source)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((labelId, value, subtitle) in row) {
                    MetricCard(
                        modifier = Modifier.weight(1f).testTag(
                            when (labelId) {
                                R.string.home_sleep -> "home_metric_sleep"
                                R.string.home_resting_pulse -> "home_metric_pulse"
                                R.string.home_steps -> "home_metric_steps"
                                else -> "home_metric_energy"
                            }
                        ),
                        label = resources.getString(labelId),
                        value = value,
                        subtitle = subtitle
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    subtitle: String?
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.line)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = colors.muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = colors.text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.accentText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
