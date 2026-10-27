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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.TrendingUp
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.components.TrainPilotHeroSurface
import com.repforge.app.ui.components.TrainPilotMetricSurface
import com.repforge.app.data.db.ProgressPeriodTotals
import com.repforge.app.ui.components.TrainPilotSectionHeader
import androidx.compose.runtime.remember
import com.repforge.app.ui.theme.LocalTrainPilotColors
import kotlin.math.max
import java.text.NumberFormat
import java.util.Locale
import java.time.format.DateTimeFormatter

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
            Text(strings.getString(R.string.native_progress_error), color = colors.dangerText, modifier = Modifier.testTag("progress_error"))
            androidx.compose.material3.TextButton(onClick = viewModel::loadProgressData) {
                Text(strings.getString(R.string.native_progress_retry))
            }
        }

        // Original Studio 2x2 time-range selector and tappable volume bars,
        // driven entirely by persisted, completed native workout history.
        item(key = "progress_volume_chart") {
            ProgressVolumeChart(
                state, onRange = viewModel::selectVolumeRange,
                onRetry = viewModel::retryVolume
            )
        }

        // Studio's four-metric summary comes from bounded, tombstone-safe native Room aggregates.
        state.periodTotals?.let { totals ->
            item(key = "progress_period_overview") {
                ProgressPeriodOverview(totals, state.language)
            }
        }

        // 1. #177 Izom-anatómia terheléstérkép
        item {
            TrainPilotSectionSurface(Modifier.fillMaxWidth()) {
                Column {
                    TrainPilotSectionHeader(strings.getString(R.string.native_progress_muscle_load), icon = Icons.Default.AccessibilityNew)
                    Text(strings.getString(R.string.native_progress_month_sets), color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))

                    MuscleLoadPanel(state.muscleLoads, state.language)

                }
            }
        }

        // 2. 1RM Fejlődési diagram
        item {
            TrainPilotHeroSurface(Modifier.fillMaxWidth()) {
                Column {
                    TrainPilotSectionHeader(strings.getString(R.string.native_progress_1rm_chart), icon = Icons.Default.TrendingUp)
                    Text(strings.getString(R.string.native_progress_1rm_description), color = colors.muted, fontSize = 11.sp)
                    if (!state.strengthLoading && !state.strengthError && state.exercise1RmTrend.isNotEmpty()) {
                        ProgressStrengthSnapshot(state.exercise1RmTrend, state.language)
                    }
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

/** Studio's total-volume panel with four authentic native history ranges.
 * This is visual-only: no separate DEMO state or simulated chart measurements.
 */
@Composable
private fun ProgressVolumeChart(
    state: ProgressUiState,
    onRange: (NativeVolumeRange) -> Unit,
    onRetry: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) {
        nativeResources(context, state.language)
    }
    val locale = remember(state.language) { Locale.forLanguageTag(state.language) }
    val number = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
    }
    val date = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val selectedBar = remember(state.selectedVolumeRange) { mutableIntStateOf(-1) }
    val snapshot = state.volumeSnapshot?.takeIf {
        it.range == state.selectedVolumeRange
    }
    val narrow = LocalConfiguration.current.screenWidthDp < 480 ||
        LocalDensity.current.fontScale > 1.15f
    val rangeLabels = listOf(
        R.string.studio_volume_one_day, R.string.studio_volume_week,
        R.string.studio_volume_month, R.string.studio_volume_three_months
    )
    val ranges = NativeVolumeRange.values().toList()
    val control: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (pair in ranges.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (range in pair) {
                        FilterChip(
                            selected = range == state.selectedVolumeRange,
                            onClick = { onRange(range) },
                            label = { Text(strings.getString(rangeLabels[range.ordinal])) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("progress_volume_range_${range.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
    val chart: @Composable () -> Unit = {
        when {
            state.volumeLoading -> Text(strings.getString(R.string.studio_volume_loading),
                color = colors.muted, modifier = Modifier.testTag("progress_volume_loading"))
            state.volumeError -> androidx.compose.material3.TextButton(
                onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("progress_volume_retry")
            ) { Text(strings.getString(R.string.studio_volume_retry)) }
            snapshot == null -> Text(strings.getString(R.string.studio_volume_empty),
                color = colors.muted)
            else -> {
                val maxValue = snapshot.buckets.maxOfOrNull { it.volumeKg }?.coerceAtLeast(1.0) ?: 1.0
                LazyRow(modifier = Modifier.fillMaxWidth().testTag("progress_volume_bars"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(snapshot.buckets, key = { index, _ -> index }) { index, bucket ->
                        Column(
                            modifier = Modifier.width(48.dp).heightIn(min = 150.dp)
                                .semantics {
                                    contentDescription = "${bucket.startDate.format(date)}: " +
                                        "${number.format(bucket.volumeKg)} kg"
                                    selected = selectedBar.intValue == index
                                }
                                .clickable(role = Role.Button) { selectedBar.intValue = index }
                                .testTag("progress_volume_bar_$index"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(Modifier.fillMaxWidth().height(105.dp),
                                contentAlignment = Alignment.BottomCenter) {
                                val filled = (bucket.volumeKg / maxValue).coerceIn(0.0, 1.0)
                                Box(Modifier.width(25.dp)
                                    .height((if (bucket.volumeKg > 0) 5.0 + filled * 95.0 else 3.0).toFloat().dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (selectedBar.intValue == index)
                                        colors.accent2 else colors.accent))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(bucket.startDate.format(date), color = colors.muted,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                maxLines = 1)
                        }
                    }
                }
                if (snapshot.workoutCount == 0) {
                    Text(strings.getString(R.string.studio_volume_empty),
                        color = colors.muted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("progress_volume_empty"))
                }
                if (selectedBar.intValue in snapshot.buckets.indices) {
                    val bucket = snapshot.buckets[selectedBar.intValue]
                    Text(
                        "${bucket.startDate.format(date)} – ${bucket.endExclusive.minusDays(1).format(date)} · " +
                            "${number.format(bucket.volumeKg)} kg · ${bucket.workoutCount} " +
                            strings.getString(R.string.studio_volume_workouts),
                        color = colors.text,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("progress_volume_selected_detail")
                    )
                }
            }
        }
    }
    TrainPilotHeroSurface(Modifier.fillMaxWidth().testTag("progress_total_volume_card")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(strings.getString(R.string.studio_volume_total),
                    color = colors.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                Text(
                    if (snapshot == null) "—" else "${number.format(snapshot.totalVolumeKg)} kg",
                    color = colors.text,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("progress_volume_total")
                )
                if (snapshot != null) Text(
                    "${snapshot.startDate.format(date)} – ${snapshot.endDate.format(date)}",
                    color = colors.muted,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
            Icon(Icons.Default.TrendingUp, contentDescription = null,
                modifier = Modifier.size(24.dp), tint = colors.accentText)
        }
        Spacer(Modifier.height(10.dp))
        if (narrow) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            control()
            chart()
        } else Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(0.55f)) { control() }
            Box(Modifier.weight(1.45f)) { chart() }
        }
    }
}

/** A read-only Studio overview of completed workouts from the last 30 local dates.
 * The DB aggregate is the source of truth; no DEMO counters are introduced here.
 */
@Composable
private fun ProgressPeriodOverview(totals: ProgressPeriodTotals, language: String) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val number = remember(language) {
        NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
    val entries = listOf(
        R.string.studio_progress_period_workouts to number.format(totals.workoutCount),
        R.string.studio_progress_period_volume to "${number.format(totals.totalVolumeKg)} kg",
        R.string.studio_progress_period_sets to number.format(totals.completedSets),
        R.string.studio_progress_period_pr_workouts to number.format(totals.prWorkouts)
    )
    val stack = LocalConfiguration.current.screenWidthDp < 320 ||
        LocalDensity.current.fontScale > 1.3f
    TrainPilotHeroSurface(Modifier.fillMaxWidth().testTag("progress_period_overview")) {
        TrainPilotSectionHeader(strings.getString(R.string.studio_progress_period_title),
            icon = Icons.Default.TrendingUp)
        for (row in entries.chunked(if (stack) 1 else 2)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, value) in row) {
                    TrainPilotMetricSurface(Modifier.weight(1f).fillMaxHeight()) {
                        Text(strings.getString(label),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = colors.muted)
                        Text(value, style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                            fontFeatureSettings = "tnum"), color = colors.text)
                    }
                }
                repeat(if (stack) 1 - row.size else 2 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** The source trend is an ascending time-series for the selected exercise and date range.
 * Missing/invalid calculations never become values in the Studio summary cards.
 */
internal fun strengthSnapshot(
    estimates: List<Exercise1RmPointUiModel>
): Pair<Exercise1RmPointUiModel, Exercise1RmPointUiModel>? {
    val valid = estimates.filter { it.e1rmKg.isFinite() && it.e1rmKg > 0.0 }
    val latest = valid.lastOrNull() ?: return null
    val best = valid.maxByOrNull { it.e1rmKg } ?: return null
    return latest to best
}

/** Only the selected exercise/range's persisted, finite 1RM estimates are displayed.
 * Do not conflate the maximum estimate with a confirmed personal record.
 */
@Composable
private fun ProgressStrengthSnapshot(
    estimates: List<Exercise1RmPointUiModel>, language: String
) {
    val (latest, best) = strengthSnapshot(estimates) ?: return
    val narrow = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.3f
    val text = nativeResources(LocalContext.current, language)
    val number = NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    val latestCard: @Composable (Modifier) -> Unit = { modifier ->
        TrainPilotMetricSurface(modifier.testTag("progress_latest_1rm")) {
            Text(text.getString(R.string.studio_progress_latest_1rm),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = LocalTrainPilotColors.current.muted)
            Text("${number.format(latest.e1rmKg)} kg",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum"),
                color = LocalTrainPilotColors.current.text)
            Text(latest.date,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = LocalTrainPilotColors.current.muted)
        }
    }
    val bestCard: @Composable (Modifier) -> Unit = { modifier ->
        TrainPilotMetricSurface(modifier.testTag("progress_best_1rm")) {
            Text(text.getString(R.string.studio_progress_best_1rm),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = LocalTrainPilotColors.current.muted)
            Text("${number.format(best.e1rmKg)} kg",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum"),
                color = LocalTrainPilotColors.current.accentText)
            Text(best.date,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = LocalTrainPilotColors.current.muted)
        }
    }
    Spacer(Modifier.height(12.dp))
    if (narrow) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            latestCard(Modifier.fillMaxWidth())
            bestCard(Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            latestCard(Modifier.weight(1f).fillMaxHeight())
            bestCard(Modifier.weight(1f).fillMaxHeight())
        }
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
            if (i == 0) path.moveTo(o.x, o.y) else {
                val previous = offsets[i - 1]
                val middle = (previous.x + o.x) / 2f
                // Both controls stay within this pair's range: no invented extrema.
                path.cubicTo(middle, previous.y, middle, o.y, o.x, o.y)
            }
        }

        for (line in 0..4) {
            val y = 10.dp.toPx() + (size.height - 20.dp.toPx()) * line / 4f
            drawLine(colors.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleLoadPanel(loads: List<MuscleLoadUiModel>, language: String) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val percent = remember(language) { java.text.NumberFormat.getPercentInstance(java.util.Locale.forLanguageTag(language)) }
    val bars: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (load in loads) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(load.label, Modifier.weight(1f), color = colors.text, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Text(strings.getString(R.string.native_progress_sets_count, load.setsCount),
                        color = colors.muted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(colors.card2)) {
                        Box(Modifier.fillMaxWidth(load.loadPercentage).height(8.dp).clip(CircleShape)
                            .background(muscleLoadColor(load.loadPercentage, colors)))
                    }
                    Text(if (load.setsCount == 0) "—" else percent.format(load.loadPercentage.toDouble()),
                        color = colors.muted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"))
                }
            }
        }
    }
    // The original main front/back anatomy is the central visual, not a tiny thumbnail.
    // Preserve the real tinted muscle-mask renderer and set percentages; the Studio
    // outline/illustrative DEMO body is deliberately NOT substituted here.
    // On phones the full body image gets the available width, with load bars below;
    // on wider tablets both can appear side-by-side without squeezing the anatomy.
    val fullBody = LocalConfiguration.current.screenWidthDp < 600 ||
        LocalDensity.current.fontScale > 1.3f
    if (fullBody) Column(Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NativeMuscleLoadVisual(loads, language, Modifier.fillMaxWidth())
        bars()
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NativeMuscleLoadVisual(loads, language, Modifier.weight(1.05f))
        Box(Modifier.weight(0.95f)) { bars() }
    }
    Spacer(Modifier.height(12.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((label, color) in listOf(R.string.native_visual_load_low to colors.loadLow,
            R.string.native_visual_load_medium to colors.loadMedium, R.string.native_visual_load_high to colors.loadHigh)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Text(strings.getString(label), color = colors.muted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(strings.getString(R.string.native_visual_load_note), color = colors.muted,
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
}
