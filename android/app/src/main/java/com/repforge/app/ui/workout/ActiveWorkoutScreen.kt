package com.repforge.app.ui.workout

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.ui.theme.LocalTrainPilotColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val activity = context as? Activity
    val photoRepo = remember { WorkoutPhotoRepository(context) }

    // Kamera és Galéria indítók
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        viewModel.viewModelScope.launch {
            val r = photoRepo.handleCameraResult(res.resultCode, res.data)
            r.getOrNull()?.let { viewModel.onPhotoCaptured(it) }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        viewModel.viewModelScope.launch {
            val r = photoRepo.importPickedUri(res.data?.data)
            r.getOrNull()?.let { viewModel.onPhotoCaptured(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Fejléc: Cím, Időtartam, Szenzor státuszok és Ownership gomb
            ActiveWorkoutHeader(
                state = state,
                onTogglePause = { viewModel.toggleWorkoutPause() },
                onOwnershipAction = {
                    if (state.timerOwner == "watch") viewModel.requestOwnershipBackFromWatch()
                    else activity?.let { viewModel.startWatchMeasurementRemotely(it) }
                }
            )

            // 2. #188 Lebegő Undo Set sáv (csak 8 mp-ig aktív sorozat után!)
            AnimatedVisibility(
                visible = state.undoAvailable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.card2)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Undo, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${state.undoExerciseName} ${state.undoSetNumber}. szett rögzítve",
                            color = colors.text,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { viewModel.undoLastSet() }) {
                        Text("Visszavonás", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // 3. Gyakorlatok és Szettek listája
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                itemsIndexed(state.exercises, key = { _, it -> it.exerciseId }) { exIdx, ex ->
                    ExerciseCardItem(
                        exercise = ex,
                        onEditSet = { sIdx, field, v -> viewModel.editSet(exIdx, sIdx, field, v) },
                        onToggleSetDone = { sIdx -> viewModel.toggleSetCompleted(exIdx, sIdx) },
                        onSetRir = { sIdx, rir -> viewModel.setSetRir(exIdx, sIdx, rir) },
                        onToggleRirMode = { en -> viewModel.toggleSetRirMode(exIdx, en) },
                        onAddSet = { viewModel.addSetToExercise(exIdx) },
                        onRemoveSet = { sIdx -> viewModel.removeSetFromExercise(exIdx, sIdx) }
                    )
                }

                // + Gyakorlat hozzáadása gomb
                item {
                    Button(
                        onClick = { viewModel.openExercisePicker() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("+ Gyakorlat hozzáadása", color = colors.text, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Csatolt fotók sora
                if (state.attachedPhotos.isNotEmpty()) {
                    item {
                        Text("Csatolt fotók", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.attachedPhotos, key = { it.id }) { photo ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.card)
                                        .border(1.dp, colors.line, RoundedCornerShape(8.dp))
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deletePhoto(photo.id) },
                                        modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Törlés", tint = colors.dangerText, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // 4. Alsó vezérlősáv és Élő Pihenőidő sáv
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.card.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Ha pihenőidő fut, élő pihenősáv jelenik meg gombokkal
            if (state.timerPhase == "rest" && state.timerRemainingSec > 0) {
                RestTimerFloatingBar(
                    remainingSec = state.timerRemainingSec,
                    onAdjust = { delta -> viewModel.adjustRestTimer(delta) },
                    onSkip = { viewModel.skipRestTimer() }
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Kamera gomb
                IconButton(
                    onClick = {
                        runCatching { cameraLauncher.launch(photoRepo.prepareCameraCaptureIntent()) }
                    },
                    modifier = Modifier.size(48.dp).background(colors.card2, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Kamera", tint = colors.text)
                }
                // Galéria gomb
                IconButton(
                    onClick = {
                        galleryLauncher.launch(photoRepo.createPickImageIntent())
                    },
                    modifier = Modifier.size(48.dp).background(colors.card2, CircleShape)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Galéria", tint = colors.text)
                }
                // Befejezés gomb
                Button(
                    onClick = { viewModel.openFinishModal() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0E1015))
                    Spacer(Modifier.width(6.dp))
                    Text("Befejezés", color = Color(0xFF0E1015), fontWeight = FontWeight.Bold)
                }
                // Elvetés gomb
                Button(
                    onClick = { viewModel.openDiscardModal() },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("✕", color = colors.dangerText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // =========================================================================
    // Modális ablakok és BottomSheet-ek
    // =========================================================================

    // Gyakorlat választó BottomSheet
    if (state.showExercisePicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeExercisePicker() },
            sheetState = sheetState,
            containerColor = colors.card
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Gyakorlat hozzáadása", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    items(state.availableLibraryExercises) { libEx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addExerciseToWorkout(libEx) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(libEx.nameHu, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${libEx.primaryMuscle} • ${libEx.loadType}", color = colors.muted, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        }
    }

    // Befejezés & RPE visszajelzés dialógus
    if (state.showFinishModal) {
        FinishWorkoutDialog(
            onDismiss = { viewModel.closeFinishModal() },
            onConfirm = { effort ->
                viewModel.closeFinishModal()
                viewModel.finishWorkout(effort)
                navController.popBackStack()
            }
        )
    }

    // Elvetés megerősítő dialógus
    if (state.showDiscardModal) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDiscardModal() },
            title = { Text("Félbehagyott edzés", color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text("Biztosan törlöd a megkezdett edzést? Az edzés adatai elvesznek.", color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeDiscardModal()
                        viewModel.discardWorkout()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Törlés", color = colors.dangerText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDiscardModal() }) {
                    Text("Mégse", color = colors.text)
                }
            },
            containerColor = colors.card
        )
    }
}

@Composable
fun ActiveWorkoutHeader(
    state: ActiveWorkoutUiState,
    onTogglePause: () -> Unit,
    onOwnershipAction: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        text = if (state.isPaused) "Edzés szünetel" else if (state.timerOwner == "watch") "Óra vezérli (#193)" else "Telefon vezérli",
                        color = if (state.isPaused) colors.accent else colors.muted,
                        fontSize = 11.sp
                    )
                }
                // Szünet / Folytatás gomb
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.size(36.dp).background(colors.card2, CircleShape)
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (state.isPaused) "Folytatás" else "Szünet",
                        tint = colors.accent
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Szenzor státuszok (Pulzus + GPS)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.liveHeartRateBpm != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${state.liveHeartRateBpm} bpm", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.isGpsActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${(state.gpsDistanceMeters / 100.0).roundToInt() / 10.0} km", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Ownership átadás / átvétel gomb
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.card2)
                        .clickable(onClick = onOwnershipAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colors.accent2, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.timerOwner == "watch") "Visszavétel" else "Indítás órán",
                            color = colors.accent2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCardItem(
    exercise: ActiveExerciseUiModel,
    onEditSet: (Int, String, String) -> Unit,
    onToggleSetDone: (Int) -> Unit,
    onSetRir: (Int, Int?) -> Unit,
    onToggleRirMode: (Boolean) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(exercise.name, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${exercise.sets.count { it.isCompleted }}/${exercise.sets.size} kész", color = colors.accent, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))

            // Szett fejléc
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("#", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                Text("Súly (kg)", color = colors.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(if (exercise.isBilateral) "Bal / Jobb (mp)" else "Ism.", color = colors.muted, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                if (exercise.setRirEnabled) {
                    Text("RIR", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                }
                Text("Kész", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(6.dp))

            // Szettek sorai
            exercise.sets.forEachIndexed { sIdx, set ->
                SetRowItem(
                    set = set,
                    isBilateral = exercise.isBilateral,
                    showRir = exercise.setRirEnabled,
                    onWeightChange = { onEditSet(sIdx, "weight", it) },
                    onRepsChange = { onEditSet(sIdx, "reps", it) },
                    onLeftChange = { onEditSet(sIdx, "leftSeconds", it) },
                    onRightChange = { onEditSet(sIdx, "rightSeconds", it) },
                    onToggleDone = { onToggleSetDone(sIdx) },
                    onRirChange = { onSetRir(sIdx, it) },
                    onDelete = { onRemoveSet(sIdx) }
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddSet) {
                    Text("+ Szett hozzáadása", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { onToggleRirMode(!exercise.setRirEnabled) }) {
                    Text(if (exercise.setRirEnabled) "RIR elrejtése" else "#229 RIR megadása", color = colors.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SetRowItem(
    set: ActiveSetUiModel,
    isBilateral: Boolean,
    showRir: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onLeftChange: (String) -> Unit,
    onRightChange: (String) -> Unit,
    onToggleDone: () -> Unit,
    onRirChange: (Int?) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${set.setNumber}", color = colors.text, fontSize = 13.sp, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)

        // Súly beviteli mező
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            enabled = !set.isCompleted,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.card2,
                unfocusedContainerColor = colors.card2,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line
            ),
            modifier = Modifier.weight(1f).height(44.dp)
        )
        Spacer(Modifier.width(6.dp))

        // Ismétlés vagy Kétoldali idő mező
        if (isBilateral) {
            Row(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(
                    value = if (set.leftSeconds > 0) set.leftSeconds.toString() else "",
                    onValueChange = onLeftChange,
                    enabled = !set.isCompleted,
                    placeholder = { Text("B", fontSize = 10.sp, color = colors.muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.text, unfocusedTextColor = colors.text),
                    modifier = Modifier.weight(1f).height(44.dp)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = if (set.rightSeconds > 0) set.rightSeconds.toString() else "",
                    onValueChange = onRightChange,
                    enabled = !set.isCompleted,
                    placeholder = { Text("J", fontSize = 10.sp, color = colors.muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.text, unfocusedTextColor = colors.text),
                    modifier = Modifier.weight(1f).height(44.dp)
                )
            }
        } else {
            OutlinedTextField(
                value = set.reps,
                onValueChange = onRepsChange,
                enabled = !set.isCompleted,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.card2,
                    unfocusedContainerColor = colors.card2,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line
                ),
                modifier = Modifier.weight(1.2f).height(44.dp)
            )
        }

        // #229 RIR mező (ha be van kapcsolva)
        if (showRir) {
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(
                value = set.actualRir?.toString() ?: "",
                onValueChange = { onRirChange(it.toIntOrNull()?.coerceIn(0, 10)) },
                enabled = !set.isCompleted,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.accent, unfocusedTextColor = colors.text),
                modifier = Modifier.width(42.dp).height(44.dp)
            )
        }

        Spacer(Modifier.width(6.dp))
        // Kész pipa gomb
        Checkbox(
            checked = set.isCompleted,
            onCheckedChange = { onToggleDone() },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.line,
                checkmarkColor = Color(0xFF0E1015)
            )
        )
    }
}

@Composable
fun RestTimerFloatingBar(
    remainingSec: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card2)
            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⏱ ${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                color = colors.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text("Pihenő", color = colors.muted, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onAdjust(-15) }) {
                Text("-15s", color = colors.text, fontSize = 11.sp)
            }
            TextButton(onClick = { onAdjust(15) }) {
                Text("+15s", color = colors.text, fontSize = 11.sp)
            }
            Button(
                onClick = onSkip,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("Kihagyás", color = Color(0xFF0E1015), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FinishWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val choices = listOf(
        "easy" to "Túl könnyű",
        "light" to "Kicsit könnyű",
        "good" to "Pont jó",
        "challenging" to "Kicsit nehéz",
        "hard" to "Túl nehéz"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Milyen volt az edzés?", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Válassz egy nehézségi fokozatot a Coach és a napló számára:", color = colors.muted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                choices.forEach { (key, label) ->
                    Button(
                        onClick = { onConfirm(key) },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                    ) {
                        Text(label, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Button(
                    onClick = { onConfirm("pain") },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Fájdalom / Kellemetlenség", color = colors.dangerText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text("Kihagyás és mentés", color = colors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse", color = colors.muted)
            }
        },
        containerColor = colors.card
    )
}
