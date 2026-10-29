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
    initialSection: String? = null,
    onInitialSectionConsumed: () -> Unit = {},
    viewModel: ProgressViewModel = viewModel(),
    weightViewModel: WeightJournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val weights by weightViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Presentation-only Studio section state. Native filters and query state stay in the ViewModel.
    val studioSection = remember { mutableIntStateOf(0) }
    val statisticsSection = remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    LaunchedEffect(initialSection, state.loading, state.loadError, state.periodTotals) {
        if (initialSection == "photos" && !state.loading && !state.loadError) {
            studioSection.intValue = 0
            val photoIndex = if (state.periodTotals != null) 5 else 4
            listState.scrollToItem(photoIndex)
            onInitialSectionConsumed()
        }
    }

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
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).testTag("progress_list"),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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

        item(key = "progress_studio_sections") {
            StudioProgressTabs(studioSection.intValue, state.language) { studioSection.intValue = it }
        }

        when (studioSection.intValue) {
            0 -> {
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
                ProgressPeriodOverview(totals, state.periodPrRecords, state.language)
            }
        }

        item(key = "studio_muscle_heatmap") {
            NativeStudioMuscleCard(state, viewModel::selectMuscleRange, viewModel::retryMuscle)
        }

            }
            1 -> {
                item(key = "progress_statistics_sections") {
                    StudioProgressStatisticsTabs(statisticsSection.intValue, state.language) {
                        statisticsSection.intValue = it
                    }
                }
                if (statisticsSection.intValue == 0) {
                    state.periodTotals?.takeIf { it.prWorkouts > 0 }?.let { totals ->
                        item(key = "progress_personal_records_summary") {
                            ProgressPersonalRecordsSummary(totals.prWorkouts, state.language)
                        }
                    }
                } else {
        item(key = "progress_cardio") {
            NativeCardioProgressPanel(
                state = state,
                onSelectActivity = viewModel::selectCardioActivity,
                onRetry = viewModel::retryCardio
            )
        }

                }
            }
            else -> {
                item(key = "progress_strength_selector") {
                    ProgressStrengthSelector(
                        state = state,
                        onRange = viewModel::selectStrengthRange,
                        onExercise = viewModel::selectExercise
                    )
                }

                item(key = "progress_strength_chart") {
                    ProgressStrengthChart(
                        state = state,
                        onRetry = viewModel::retryStrength
                    )
                }

                if (!state.strengthLoading && !state.strengthError &&
                    state.exercise1RmTrend.isNotEmpty()
                ) {
                    item(key = "progress_strength_summary") {
                        ProgressStrengthSnapshot(state.exercise1RmTrend, state.language)
                    }
                }
            }
        }


        // Private progress photos stay reachable independently of the Studio analytics section.
        item(key = "progress_photos") {
            NativeProgressPhotoGallery(state, viewModel::openPhoto, viewModel::closePhoto)
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
private fun ProgressPersonalRecordsSummary(count: Int, language: String) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    TrainPilotHeroSurface(
        Modifier.fillMaxWidth().testTag("progress_personal_records_summary")
    ) {
        Text(
            strings.getString(R.string.studio_progress_stats_records),
            color = colors.accentText,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            strings.getString(R.string.studio_progress_pr_workouts_count, count),
            color = colors.text,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                fontFeatureSettings = "tnum"
            )
        )
        Text(
            strings.getString(R.string.studio_progress_pr_workouts_hint),
            color = colors.muted,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
        )
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
    // Phone widths must give the 2x2 period selector the full card width.
    // At 384dp the chart beside it squeezed labels into vertical text.
    val narrow = LocalConfiguration.current.screenWidthDp < 600 ||
        LocalDensity.current.fontScale > 1.15f
    val singleColumnRanges = LocalConfiguration.current.screenWidthDp < 300 ||
        LocalDensity.current.fontScale >= 1.4f
    val rangeLabels = listOf(
        R.string.studio_volume_one_day, R.string.studio_volume_week,
        R.string.studio_volume_month, R.string.studio_volume_three_months
    )
    val ranges = NativeVolumeRange.values().toList()
    val control: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (pair in ranges.chunked(if (singleColumnRanges) 1 else 2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (range in pair) {
                        FilterChip(
                            selected = range == state.selectedVolumeRange,
                            onClick = { onRange(range) },
                            label = { Text(strings.getString(rangeLabels[range.ordinal]),
                                maxLines = 1, softWrap = false) },
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
private fun ProgressPeriodOverview(
    totals: ProgressPeriodTotals,
    prRecords: Int,
    language: String
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val number = remember(language) {
        NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
    fun label(hu: String, en: String, de: String, ro: String) = when (language) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }
    val averageVolume = if (totals.strengthWorkoutCount > 0)
        totals.totalVolumeKg / totals.strengthWorkoutCount else null
    val averageMinutes = if (totals.durationWorkoutCount > 0)
        totals.totalDurationSeconds.toDouble() / totals.durationWorkoutCount / 60.0 else null
    val durationText = averageMinutes?.let {
        val value = number.format(it)
        when (language) {
            "hu" -> "$value perc"
            else -> "$value min"
        }
    } ?: "—"
    val entries = listOf(
        label("PR rekordok", "PR records", "PR-Rekorde", "Recorduri PR") to number.format(prRecords),
        strings.getString(R.string.studio_progress_period_workouts) to number.format(totals.workoutCount),
        label(
            "Átlagos volumen / erőedzés",
            "Average volume / strength workout",
            "Ø Volumen / Krafttraining",
            "Volum mediu / antrenament de forță"
        ) to (averageVolume?.let { "${number.format(it)} kg" } ?: "—"),
        label(
            "Átlagos edzésidő",
            "Average workout duration",
            "Ø Trainingsdauer",
            "Durata medie a antrenamentului"
        ) to durationText
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
                        Text(label,
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

@Composable
private fun ProgressStrengthSelector(
    state: ProgressUiState,
    onRange: (NativeStrengthRange) -> Unit,
    onExercise: (String) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("progress_strength_selector"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                strings.getString(R.string.studio_progress_strength_exercise),
                color = colors.muted,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("progress_exercise_choices")
            ) {
                items(state.exercises, key = { it.id }) { ex ->
                    val isSelected = ex.id == state.selectedExerciseId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.accent else colors.card2)
                            .clickable(role = Role.Button) { onExercise(ex.id) }
                            .testTag("progress_exercise_${ex.id}")
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            when (state.language) {
                                "en" -> ex.nameEn
                                "de" -> ex.nameDe
                                "ro" -> ex.nameRo
                                else -> ex.nameHu
                            }.ifBlank { ex.nameHu },
                            color = if (isSelected) colors.onAccent else colors.text,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LazyRow(
                modifier = Modifier.testTag("progress_strength_ranges"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        onClick = { onRange(range) },
                        label = { Text(strings.getString(title)) },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("progress_strength_${range.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStrengthChart(
    state: ProgressUiState,
    onRetry: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val number = remember(state.language) {
        NumberFormat.getNumberInstance(Locale.forLanguageTag(state.language)).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
    val latest = if (!state.strengthLoading && !state.strengthError) {
        strengthSnapshot(state.exercise1RmTrend)?.first
    } else null
    Card(
        modifier = Modifier.fillMaxWidth().testTag("progress_strength_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                strings.getString(R.string.studio_progress_strength_estimate),
                color = colors.muted,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
            Text(
                latest?.let { "${number.format(it.e1rmKg)} kg" } ?: "—",
                color = colors.text,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier.testTag("progress_latest_1rm")
            )
            Text(
                strings.getString(R.string.native_progress_1rm_description),
                color = colors.muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card2)
                    .padding(12.dp)
            ) {
                when {
                    state.strengthLoading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.getString(R.string.native_progress_strength_loading),
                            color = colors.muted,
                            modifier = Modifier.testTag("progress_strength_loading")
                        )
                    }
                    state.strengthError -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("progress_strength_retry")
                        ) {
                            Text(strings.getString(R.string.native_progress_retry), color = colors.danger)
                        }
                    }
                    state.exercise1RmTrend.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.getString(
                                if (state.selectedStrengthRange == NativeStrengthRange.ALL)
                                    R.string.native_progress_no_history
                                else R.string.native_progress_strength_empty
                            ),
                            color = colors.muted,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("progress_strength_empty")
                        )
                    }
                    else -> TrendCanvas(points = state.exercise1RmTrend.map { it.e1rmKg })
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
    estimates: List<Exercise1RmPointUiModel>,
    language: String
) {
    val (_, bestEstimate) = strengthSnapshot(estimates) ?: return
    val heaviestSet = estimates
        .filter { it.weightKg.isFinite() && it.weightKg > 0.0 && it.reps > 0 }
        .maxWithOrNull(
            compareBy<Exercise1RmPointUiModel> { it.weightKg }
                .thenBy { it.reps }
        )
    val narrow = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.3f
    val strings = nativeResources(LocalContext.current, language)
    val number = NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    val estimateCard: @Composable (Modifier) -> Unit = { modifier ->
        TrainPilotMetricSurface(modifier.testTag("progress_best_1rm")) {
            Text(
                strings.getString(R.string.studio_progress_best_1rm),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = LocalTrainPilotColors.current.muted
            )
            Text(
                "${number.format(bestEstimate.e1rmKg)} kg",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = LocalTrainPilotColors.current.accentText
            )
            Text(
                bestEstimate.date,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = LocalTrainPilotColors.current.muted
            )
        }
    }
    val setCard: @Composable (Modifier) -> Unit = { modifier ->
        TrainPilotMetricSurface(modifier.testTag("progress_best_set")) {
            Text(
                strings.getString(R.string.studio_progress_heaviest_set),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = LocalTrainPilotColors.current.muted
            )
            Text(
                heaviestSet?.let { "${number.format(it.weightKg)} kg × ${it.reps}" } ?: "—",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = LocalTrainPilotColors.current.text
            )
            if (heaviestSet != null) {
                Text(
                    heaviestSet.date,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = LocalTrainPilotColors.current.muted
                )
            }
        }
    }
    if (narrow) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estimateCard(Modifier.fillMaxWidth())
            setCard(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            estimateCard(Modifier.weight(1f).fillMaxHeight())
            setCard(Modifier.weight(1f).fillMaxHeight())
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
