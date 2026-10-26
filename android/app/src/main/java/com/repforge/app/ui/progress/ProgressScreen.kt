package com.repforge.app.ui.progress

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.time.ZoneId
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.repforge.app.ui.theme.LocalTrainPilotColors
import kotlin.math.max

@Composable
fun ProgressScreen(
    onWeightJournal: () -> Unit,
    viewModel: ProgressViewModel = viewModel(),
    weightViewModel: WeightJournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val weights by weightViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // A Room write is not required for a new day/week or an OS time-zone
    // change. ON_RESUME also catches returning from a backgrounded app or
    // another navigation destination without losing the selected activity.
    DisposableEffect(context, lifecycleOwner, viewModel) {
        val applicationContext = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receivingContext: Context, intent: Intent) {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    intent.action in setOf(Intent.ACTION_DATE_CHANGED,
                        Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
                    viewModel.onCardioSystemClockChanged()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(applicationContext, receiver, filter,
            ContextCompat.RECEIVER_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onCardioScreenResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            applicationContext.unregisterReceiver(receiver)
        }
    }
    // Cheap date/zone check only; Room reloads only when the local date/zone
    // has actually changed. The short fallback handles missed OS broadcasts.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(NativeCardioCalendarRefresh.delayToCheck(
                    System.currentTimeMillis(), ZoneId.systemDefault()))
                viewModel.onCardioCalendarTick()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("progress_list"),
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        if (state.loading) item {
            Text(strings.getString(R.string.native_progress_loading), color = colors.muted)
        }
        if (state.loadError) item {
            Text(strings.getString(R.string.native_progress_error), color = colors.danger, modifier = Modifier.testTag("progress_error"))
            androidx.compose.material3.TextButton(onClick = viewModel::loadProgressData) {
                Text(strings.getString(R.string.native_progress_retry))
            }
        }

        // 1. #177 Izom-anatómia terheléstérkép
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.getString(R.string.native_progress_muscle_load), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(strings.getString(R.string.native_progress_month_sets), color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))

                    state.muscleLoads.forEach { load ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(load.label, color = colors.text, fontSize = 13.sp, modifier = Modifier.width(90.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.card2)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(load.loadPercentage)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(strings.getString(R.string.native_progress_sets_count, load.setsCount), color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                        }
                    }
                }
            }
        }

        // 2. 1RM Fejlődési diagram
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.getString(R.string.native_progress_1rm_chart), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(strings.getString(R.string.native_progress_1rm_description), color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))

                    // Independent strength date window. Cardio and weight keep their own filters.
                    LazyRow(
                        modifier = Modifier.testTag("progress_strength_ranges"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(NativeStrengthRange.values().toList(), key = { it.name }) { range ->
                            val title = when (range) {
                                NativeStrengthRange.DAYS_30 -> R.string.native_progress_strength_30
                                NativeStrengthRange.DAYS_90 -> R.string.native_progress_strength_90
                                NativeStrengthRange.YEAR -> R.string.native_progress_strength_year
                                NativeStrengthRange.ALL -> R.string.native_progress_strength_all
                            }
                            FilterChip(
                                selected = state.selectedStrengthRange == range,
                                onClick = { viewModel.selectStrengthRange(range) },
                                label = { Text(strings.getString(title)) },
                                modifier = Modifier.heightIn(min = 48.dp)
                                    .testTag("progress_strength_${range.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Gyakorlat választó chip sor
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.testTag("progress_exercise_choices")) {
                        items(state.exercises, key = { it.id }) { ex ->
                            val isSelected = ex.id == state.selectedExerciseId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accent else colors.card2)
                                    .clickable { viewModel.selectExercise(ex.id) }
                                    .testTag("progress_exercise_${ex.id}")
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(when (state.language) {
                                    "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo
                                    else -> ex.nameHu
                                }.ifBlank { ex.nameHu },
                                    color = if (isSelected) colors.onAccent else colors.text,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // 1RM Canvas grafikon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card2)
                            .padding(12.dp)
                    ) {
                        when {
                            state.strengthLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.getString(R.string.native_progress_strength_loading),
                                    color = colors.muted, modifier = Modifier.testTag("progress_strength_loading"))
                            }
                            state.strengthError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.TextButton(
                                    onClick = viewModel::retryStrength,
                                    modifier = Modifier.testTag("progress_strength_retry")
                                ) {
                                    Text(strings.getString(R.string.native_progress_retry), color = colors.danger)
                                }
                            }
                            state.exercise1RmTrend.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.getString(
                                    if (state.selectedStrengthRange == NativeStrengthRange.ALL)
                                        R.string.native_progress_no_history
                                    else R.string.native_progress_strength_empty
                                ), color = colors.muted, fontSize = 12.sp,
                                    modifier = Modifier.testTag("progress_strength_empty"))
                            }
                            else -> TrendCanvas(points = state.exercise1RmTrend.map { it.e1rmKg })
                        }
                    }
                }
            }
        }

        item(key = "progress_photos") {
            NativeProgressPhotoGallery(state, viewModel::openPhoto, viewModel::closePhoto)
        }

        item(key = "progress_cardio") {
            NativeCardioProgressPanel(
                state = state,
                onSelectActivity = viewModel::selectCardioActivity,
                onRetry = viewModel::retryCardio
            )
        }

        item {
            WeightJournalOverview(weights, weightViewModel::setRange, onWeightJournal,
                com.repforge.app.R.string.weight_open, weightViewModel::retryReads)
        }
        if (weights.writeFailed) item {
            Text(com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, weights.language)
                .getString(com.repforge.app.R.string.metadata_write_failed), color = colors.danger)
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

}

@Composable
fun TrendCanvas(points: List<Double>) {
    val colors = LocalTrainPilotColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val span = max(0.1, maxVal - minVal)

        val stepX = size.width / max(1, points.size - 1)
        val path = Path()
        val offsets = points.mapIndexed { i, v ->
            val x = i * stepX
            val y = (size.height - ((v - minVal) / span * (size.height - 20.dp.toPx())) - 10.dp.toPx()).toFloat()
            Offset(x, y)
        }

        offsets.forEachIndexed { i, o ->
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }

        drawPath(
            path = path,
            color = colors.accentText,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        offsets.forEach { o ->
            drawCircle(color = colors.accent2, radius = 3.dp.toPx(), center = o)
        }
    }
}
