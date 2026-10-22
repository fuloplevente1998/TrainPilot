package com.repforge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.*
import com.repforge.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.currentWorkout?.title ?: "Aktív Edzés",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val totalSec = state.elapsedSeconds
                        Text(
                            text = "%02d:%02d".format(totalSec / 60, totalSec % 60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onRequestCancel() }) {
                        Icon(Icons.Default.Close, contentDescription = "Megszakítás", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { viewModel.onRequestFinish() }) {
                        Icon(Icons.Default.Check, contentDescription = "Befejezés", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        },
        bottomBar = {
            if (state.isRestActive) {
                RestTimerHud(
                    restSecondsRemaining = state.restSecondsRemaining,
                    onSkip = { viewModel.onSkipRest() },
                    onAdjust = { viewModel.onAdjustRest(it) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ownership Transition Banner (#193)
            OwnershipHandoffBanner(
                role = state.ownershipRole,
                onHandoffToWatch = { viewModel.onInitiateHandoffToWatch() },
                onReclaimToPhone = { viewModel.onReclaimOwnershipToPhone() }
            )

            // Laposabb progressziós panel (v1.3.71)
            CollapsibleProgressionPanel(
                isExpanded = state.isProgressionPanelExpanded,
                style = state.selectedStyle,
                progression = state.selectedProgression,
                advice = state.progressionAdvice,
                onToggleExpand = { viewModel.onToggleProgressionPanel() },
                onSelectStyle = { viewModel.onSelectStyle(it) },
                onSelectProgression = { viewModel.onSelectProgression(it) }
            )

            // GPS és beltéri HUD (#193/#202)
            if (state.gpsSnapshot.isTracking) {
                GpsRouteHud(
                    gpsSnapshot = state.gpsSnapshot,
                    onToggleIndoor = { viewModel.onToggleIndoorGps() },
                    onCompleteExplicitCardio = { viewModel.onCompleteExplicitCardio() }
                )
            }

            // Gyakorlatok és sorozatok listája
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.currentWorkout?.exercises?.forEach { exercise ->
                    item {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(exercise.sets) { s ->
                        SetRowItem(
                            set = s,
                            onValuesChanged = { r, w, rir ->
                                viewModel.onSetValuesChanged(s.setId, r, w, rir)
                            },
                            onComplete = { r, w, rir ->
                                viewModel.onCompleteSet(s, r, w, rir)
                            },
                            onUndo = {
                                viewModel.onUndoSet(s)
                            }
                        )
                    }
                }
            }
        }
    }

    // v1.3.71 Visszaállított megerősítő Cancel ablak
    if (state.showCancelDialog) {
        CancelWorkoutConfirmationDialog(
            onConfirm = {
                viewModel.onConfirmCancel()
                onNavigateBack()
            },
            onDismiss = { viewModel.onDismissCancelDialog() }
        )
    }

    // Befejezés dialógus
    if (state.showFinishDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissFinishDialog() },
            title = { Text("Edzés Befejezése") },
            text = { Text("Minden sorozatot menteni szeretnél a naplóba?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.onConfirmFinish()
                    onNavigateBack()
                }) {
                    Text("Mentés és Befejezés")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onDismissFinishDialog() }) {
                    Text("Mégsem")
                }
            }
        )
    }

    // PR Értesítő badge
    if (state.activePrBreakdowns.isNotEmpty()) {
        PersonalRecordDetailsDialog(
            breakdown = state.activePrBreakdowns.first(),
            onDismiss = { viewModel.onDismissPrBadge() }
        )
    }
}
