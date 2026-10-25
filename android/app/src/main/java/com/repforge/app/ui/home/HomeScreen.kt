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
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.theme.LocalTrainPilotColors

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onStartWorkout: (String) -> Unit,
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
                        text = state.activeProgram?.program?.nameHu ?: strings.getString(R.string.native_home_default_program),
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.nextDayName,
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.getString(R.string.native_home_planned_exercises, state.nextDayExercisesCount),
                    color = colors.muted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onStartWorkout(state.nextDayId) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(state.watchState, color = colors.muted, fontSize = 11.sp)
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

@Composable
fun TodayMetricsGrid(metrics: TodayMetricsUiModel, language: String) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val items = listOf(
        Triple(strings.getString(R.string.native_home_sleep), metrics.sleepText, "h"),
        Triple(strings.getString(R.string.native_home_active_calories), metrics.caloriesText, "kcal"),
        Triple(strings.getString(R.string.native_home_pulse), metrics.pulseText, "bpm"),
        Triple(strings.getString(R.string.native_home_steps), metrics.stepsText, "lépés"),
        Triple(strings.getString(R.string.native_home_today_workout), metrics.workoutCountText, "edzés"),
        Triple(strings.getString(R.string.native_home_recovery), metrics.recoveryText, " readiness")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in items.chunked(3)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, value, _) in row) {
                    Card(
                        modifier = Modifier.weight(1f),
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
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

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
