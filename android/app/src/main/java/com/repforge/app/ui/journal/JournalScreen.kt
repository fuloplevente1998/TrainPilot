package com.repforge.app.ui.journal

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(
    navController: NavController,
    viewModel: JournalViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        if (state.workouts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Még nincs rögzített edzésed", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("A befejezett edzések itt jelennek meg részletes statisztikákkal és PR-rekordokkal.", color = colors.muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.workouts, key = { it.workout.id }) { item ->
                    val isExpanded = state.expandedWorkoutIds.contains(item.workout.id)
                    val isPrExpanded = state.expandedPrWorkoutIds.contains(item.workout.id)
                    JournalWorkoutCard(
                        workoutWithDetails = item,
                        isExpanded = isExpanded,
                        isPrExpanded = isPrExpanded,
                        prRecords = viewModel.parsePrRecords(item.workout.prRecordsJson226),
                        onToggleExpand = { viewModel.toggleWorkoutExpanded(item.workout.id) },
                        onTogglePr = { viewModel.togglePrExpanded(item.workout.id) },
                        onOpenRoute = { key -> viewModel.openRouteMap(item, key) },
                        onAddToCalendar = {
                            val intent = viewModel.createCalendarIntent(item)
                            runCatching { context.startActivity(intent) }
                        },
                        onDelete = { viewModel.requestDeleteWorkout(item) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Törlés dialógus
        state.workoutToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteWorkout() },
                title = { Text("Edzés törlése", color = colors.text, fontWeight = FontWeight.Bold) },
                text = { Text("Biztosan törlöd ezt az edzést a naplóból?\n${target.workout.dayName} (${target.workout.started.take(10)})", color = colors.muted) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteWorkout() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Text("Törlés", color = colors.dangerText, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteWorkout() }) {
                        Text("Mégse", color = colors.text)
                    }
                },
                containerColor = colors.card
            )
        }

        // #163 GPS Útvonal Kirajzoló Modális Ablak
        state.activeRoute?.let { routeModel ->
            WorkoutRouteMapDialog(
                route = routeModel,
                onDismiss = { viewModel.closeRouteMap() }
            )
        }
    }
}

@Composable
fun JournalWorkoutCard(
    workoutWithDetails: WorkoutHistoryWithDetails,
    isExpanded: Boolean,
    isPrExpanded: Boolean,
    prRecords: List<PrRecordUiModel>,
    onToggleExpand: () -> Unit,
    onTogglePr: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val w = workoutWithDetails.workout
    val dateFormatted = runCatching {
        DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(w.started))
    }.getOrDefault(w.started)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fejléc: Cím, dátum és lenyitó ikon
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(w.dayName, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(dateFormatted, color = colors.muted, fontSize = 11.sp)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.accent
                )
            }
            Spacer(Modifier.height(10.dp))

            // Metrika jelvények sora (#234 / workout-feedback-features.css alapján 2 vagy 3 oszlopos!)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JournalBadge(text = "⏱ ${w.durationSeconds / 60} perc", modifier = Modifier.weight(1f))
                JournalBadge(text = "✓ ${w.completedSetsCount}/${w.totalSetsCount} szett", modifier = Modifier.weight(1f))
                if (prRecords.isNotEmpty()) {
                    // Interaktív PR Chip (#226 .tp226-pr-badge)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.accent.copy(alpha = 0.12f))
                            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                            .clickable(onClick = onTogglePr)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = colors.accent2, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PR (${prRecords.size}) ${if (isPrExpanded) "⌃" else "⌄"}", color = colors.accent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // #226 PR Részletek Panel (.tp226-pr-details)
            AnimatedVisibility(visible = isPrExpanded && prRecords.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.card2)
                        .border(1.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Elért személyes csúcsok (PR):", color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    prRecords.forEach { pr ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.card)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(pr.exerciseName, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Új csúcs: ${pr.value} (előző: ${pr.previous})", color = colors.accent2, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Részletes edzésadatok (ha a kártya ki van bontva)
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Gyakorlatok és elvégzett szettek
                    workoutWithDetails.exercises.sortedBy { it.exercise.orderIndex }.forEach { exWithSets ->
                        Text(exWithSets.exercise.nameHu, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
                        exWithSets.sets.filter { it.done }.sortedBy { it.setNumber }.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${s.setNumber}. szett", color = colors.muted, fontSize = 12.sp)
                                val repsStr = if (s.leftSeconds != null && s.rightSeconds != null) "${s.leftSeconds}/${s.rightSeconds} mp" else "${s.reps} ism."
                                val weightStr = if (s.weight != null && s.weight > 0.0) "${s.weight} kg • " else ""
                                val rirStr = if (s.actualRir != null) " • RIR ${s.actualRir}" else ""
                                Text("$weightStr$repsStr$rirStr", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // GPS útvonal megtekintése gomb (ha van GPS kulcs)
                    val routeKey = workoutWithDetails.exercises.flatMap { it.sets }.firstOrNull { it.gpsRouteKey163 != null || it.gpsRouteKey193 != null }?.let { it.gpsRouteKey193 ?: it.gpsRouteKey163 }
                    if (routeKey != null) {
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { onOpenRoute(routeKey) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("↗ GPS-útvonal megtekintése", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Alsó műveleti gombok (Naptár + Törlés)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddToCalendar,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.text, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Naptárhoz adás", color = colors.text, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = colors.dangerText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Törlés", color = colors.dangerText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalBadge(text: String, modifier: Modifier) {
    val colors = LocalTrainPilotColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.card2)
            .border(1.dp, colors.line, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
