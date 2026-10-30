package com.repforge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.*
import com.repforge.ui.components.PersonalRecordDetailsDialog
import com.repforge.ui.theme.PilotWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edzésnapló & Rekordok", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Egyéni Rekordok (Kattints a részletekért)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(state.personalRecords) { pr ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onOpenPrDetails(pr) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PilotWarning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(pr.exerciseName, fontWeight = FontWeight.Bold)
                            Text("${pr.category.name}: %.1f kg (%d ism)".format(pr.value, pr.reps), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Befejezett Edzések (${state.totalWorkoutsCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(state.completedWorkouts) { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(workout.title, fontWeight = FontWeight.Bold)
                        Text("Időtartam: %d perc".format(workout.durationSeconds / 60), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // v1.3.71: Megjavított PR részletek dialógus tiszta bezárással
    if (state.prDetailViewState.isOpen && state.prDetailViewState.activeBreakdown != null) {
        PersonalRecordDetailsDialog(
            breakdown = state.prDetailViewState.activeBreakdown!!,
            onDismiss = { viewModel.onClosePrDetails() }
        )
    }
}
