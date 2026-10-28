package com.repforge.app.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.text.NumberFormat
import java.util.Locale

/** 07:48 AnatomicalMuscleHeatmapCard presentation, backed by the canonical
 * ProgressViewModel. Preserve the original anatomy painter and actual set counts.
 * Source: Native-AI-Studio@f1d96422, not the export's standalone demo ViewModel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativeStudioMuscleCard(state: ProgressUiState,
    onRange: (NativeVolumeRange) -> Unit, onRetry: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val strings = nativeResources(LocalContext.current, state.language)
    val percent = remember(state.language) {
        NumberFormat.getPercentInstance(Locale.forLanguageTag(state.language))
    }
    var selectedMuscle by rememberSaveable(state.selectedMuscleRange) { mutableStateOf<String?>(null) }
    val periods = listOf(NativeVolumeRange.THREE_MONTHS, NativeVolumeRange.MONTH,
        NativeVolumeRange.WEEK, NativeVolumeRange.DAY)
    val labels = listOf(R.string.studio_volume_one_day, R.string.studio_volume_week,
        R.string.studio_volume_month, R.string.studio_volume_three_months)
    Surface(Modifier.fillMaxWidth().testTag("anatomical_muscle_heatmap_card"),
        shape = RoundedCornerShape(20.dp), color = colors.card,
        border = BorderStroke(1.dp, colors.line)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.getString(R.string.native_progress_muscle_load),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = colors.text)
                Surface(Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        onRange(periods[(periods.indexOf(state.selectedMuscleRange) + 1) % periods.size])
                    }.testTag("muscle_heatmap_period_pill"),
                    shape = RoundedCornerShape(12.dp), color = colors.card2,
                    border = BorderStroke(1.dp, colors.line)) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(strings.getString(labels[state.selectedMuscleRange.ordinal]),
                            style = MaterialTheme.typography.labelMedium, color = colors.muted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                state.muscleLoading -> Text(strings.getString(R.string.native_progress_loading),
                    Modifier.testTag("progress_muscle_loading"), color = colors.muted)
                state.muscleError -> {
                    Text(strings.getString(R.string.native_progress_error),
                        Modifier.testTag("progress_muscle_error"), color = colors.dangerText)
                    TextButton(onClick = onRetry) { Text(strings.getString(R.string.native_progress_retry)) }
                }
                else -> {
                    val bars: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            // Same order as the Studio card; calculation/grouping remains canonical.
                            for (key in listOf("chest", "back", "shoulders", "legs", "arms", "core")) {
                                val load = state.muscleLoads.firstOrNull { it.muscleKey == key } ?: continue
                                Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .semantics { selected = selectedMuscle == key }
                                    .clickable(role = Role.Button) {
                                        selectedMuscle = key.takeUnless { selectedMuscle == it }
                                    }.testTag("muscle_chip_$key"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(load.label, Modifier.weight(1f), color = colors.text,
                                            style = MaterialTheme.typography.bodySmall)
                                        Text(percent.format(load.loadPercentage.toDouble()), color = colors.muted,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.testTag("muscle_percent_$key"))
                                    }
                                    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(colors.card2)) {
                                        Box(Modifier.fillMaxWidth(load.loadPercentage).height(10.dp)
                                            .clip(CircleShape).background(muscleLoadColor(load.loadPercentage, colors)))
                                    }
                                }
                            }
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        // The Studio side-by-side layout expands vertically for narrow/large text.
                        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                NativeMuscleLoadVisual(state.muscleLoads, state.language, Modifier.fillMaxWidth())
                                bars()
                            }
                        } else Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NativeMuscleLoadVisual(state.muscleLoads, state.language, Modifier.weight(.48f))
                            Box(Modifier.weight(.55f)) { bars() }
                        }
                    }
                    state.muscleLoads.firstOrNull { it.muscleKey == selectedMuscle }?.let { load ->
                        Surface(Modifier.fillMaxWidth().padding(top = 10.dp).testTag("muscle_selected_detail"),
                            shape = RoundedCornerShape(14.dp), color = colors.card2,
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = .5f))) {
                            Column(Modifier.padding(10.dp)) {
                                Text(load.label, style = MaterialTheme.typography.labelLarge, color = colors.accentText)
                                Text(strings.getString(R.string.native_progress_sets_count, load.setsCount),
                                    Modifier.testTag("muscle_selected_sets"), color = colors.text,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((label, color) in listOf(R.string.native_visual_load_low to colors.loadLow,
                    R.string.native_visual_load_medium to colors.loadMedium, R.string.native_visual_load_high to colors.loadHigh)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(strings.getString(label), color = colors.muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(strings.getString(R.string.native_visual_load_note), color = colors.muted,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
