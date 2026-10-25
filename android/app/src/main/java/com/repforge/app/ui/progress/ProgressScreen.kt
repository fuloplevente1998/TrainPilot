package com.repforge.app.ui.progress

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("progress_list"),
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
                        if (state.exercise1RmTrend.size < 2) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.getString(R.string.native_progress_no_history), color = colors.muted, fontSize = 12.sp)
                            }
                        } else {
                            TrendCanvas(points = state.exercise1RmTrend.map { it.e1rmKg })
                        }
                    }
                }
            }
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
