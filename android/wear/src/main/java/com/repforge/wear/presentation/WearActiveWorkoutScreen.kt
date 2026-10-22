package com.repforge.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.repforge.wear.*

@Composable
fun WearActiveWorkoutScreen(workoutManager: WearWorkoutManager) {
    val state by workoutManager.state.collectAsState()
    var selectedRir by remember { mutableStateOf(2) }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Felső sáv: Pulzus (BPM) + Szerepkör jelvény (#193)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "♥", color = Color(0xFFEF4444), fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.currentBpm > 0) "${state.currentBpm}" else "--",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                CompactChip(
                    onClick = { workoutManager.toggleOwnership() },
                    label = {
                        Text(
                            text = if (state.role == WearOwnershipRole.WATCH_PRIMARY) "WATCH" else "PHONE",
                            fontSize = 10.sp
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (state.role == WearOwnershipRole.WATCH_PRIMARY) Color(0xFF10B981) else Color(0xFF3B82F6)
                    )
                )
            }

            // Középső sáv: Időzítő vagy Pihenőidő
            if (state.isRestActive) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PIHENŐ",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    val rMin = state.restSecondsRemaining / 60
                    val rSec = state.restSecondsRemaining % 60
                    Text(
                        text = "%02d:%02d".format(rMin, rSec),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Button(
                        onClick = { workoutManager.skipRest() },
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF334155))
                    ) {
                        Text("»", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val min = state.elapsedSeconds / 60
                    val sec = state.elapsedSeconds % 60
                    Text(
                        text = "%02d:%02d".format(min, sec),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Text(
                        text = state.currentExerciseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "%.1f kg × %d".format(state.targetWeightKg, state.targetReps),
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Alsó sáv: Sorozat befejezés & RIR állítás
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactChip(
                    onClick = { selectedRir = (selectedRir + 1) % 5 },
                    label = { Text("RIR $selectedRir", fontSize = 11.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1E293B))
                )

                Button(
                    onClick = { workoutManager.completeSet(selectedRir) },
                    modifier = Modifier.size(38.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                ) {
                    Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
