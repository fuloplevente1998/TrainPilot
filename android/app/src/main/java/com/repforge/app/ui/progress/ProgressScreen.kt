package com.repforge.app.ui.progress
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: ProgressViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    var showWeightDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // 1. #177 Izom-anatómia terheléstérkép
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Izom-anatómia terhelés (#177)", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Elmúlt 30 nap sorozat-eloszlása", color = colors.muted, fontSize = 11.sp)
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
                            Text("${load.setsCount} szett", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(50.dp))
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
                    Text("1RM Fejlődési görbe (Epley)", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Kalkulált 1RM fejlődés az edzések alapján", color = colors.muted, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))

                    // Gyakorlat választó chip sor
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.exercises.take(8)) { ex ->
                            val isSelected = ex.id == state.selectedExerciseId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accent else colors.card2)
                                    .clickable { viewModel.selectExercise(ex.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(ex.nameHu, color = if (isSelected) colors.onAccent else colors.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                Text("Nincs elég előzmény ehhez a gyakorlathoz", color = colors.muted, fontSize = 12.sp)
                            }
                        } else {
                            TrendCanvas(points = state.exercise1RmTrend.map { it.e1rmKg })
                        }
                    }
                }
            }
        }

        // 3. Testsúly-napló szekció (#215)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Testsúly-napló", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${state.currentWeightKg ?: "—"} kg aktuális", color = colors.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showWeightDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Új mérés", color = colors.text, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Tartomány választó (7d / 30d / 90d / 1y / all)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("7" to "7 nap", "30" to "30 nap", "90" to "3 hó", "all" to "Mind").forEach { (key, label) ->
                            val isSel = state.weightRange == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) colors.accent else colors.card2)
                                    .clickable { viewModel.setWeightRange(key) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) colors.onAccent else colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Testsúly görbe
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card2)
                            .padding(10.dp)
                    ) {
                        if (state.filteredWeights.size < 2) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nincs elég súlymérés ebben az időszakban", color = colors.muted, fontSize = 11.sp)
                            }
                        } else {
                            TrendCanvas(points = state.filteredWeights.map { it.kg })
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // Új súly bevitele dialógus
    if (showWeightDialog) {
        var weightText by remember { mutableStateOf(state.currentWeightKg?.toString() ?: "75.0") }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Új testsúly rögzítése", color = colors.text, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Testsúly (kg)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = weightText.replace(',', '.').toDoubleOrNull()
                        if (kg != null && kg in 30.0..300.0) {
                            viewModel.addWeightEntry(kg, LocalDate.now().toString())
                            showWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Mentés", color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("Mégse", color = colors.text) }
            },
            containerColor = colors.card
        )
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
