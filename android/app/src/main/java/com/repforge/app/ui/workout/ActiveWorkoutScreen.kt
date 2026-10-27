package com.repforge.app.ui.workout
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.NativeTimerTarget
import com.repforge.app.ui.journal.WorkoutRouteMapDialog
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.repforge.app.ActiveWorkoutStore

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    language: String = "hu",
    launchGps: NativeWorkoutTarget? = null,
    onLaunchGpsConsumed: () -> Unit = {},
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The workout exercise is a LazyColumn item: its expanded timer controls must
    // belong to the screen so scrolling never discards the user's open panel.
    val intervalExpansion = remember(state.workoutId, state.generation) { mutableStateMapOf<String, Boolean>() }
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val activity = NativeGpsPermissionRecovery.findActivity(context)
    val strings = remember(context, language) { nativeResources(context, language) }
    val photoRepo = remember { WorkoutPhotoRepository(context) }
    // Only a user action may start a GPS measurement. A Settings return updates the prompt, never starts it.
    var gpsRecovery by remember { mutableStateOf<NativeGpsRecoveryPrompt?>(null) }
    var requestedGpsPermission by rememberSaveable { mutableStateOf(false) }
    val renderedWorkoutId = state.workoutId
    val renderedGeneration = state.generation
    BackHandler(enabled = gpsRecovery == null) {
        if (!state.isFinishing) viewModel.leaveWorkout(renderedWorkoutId, renderedGeneration) { navController.popBackStack() }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onEditorVisible() }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.onEditorVisible()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    // Bundle-saveable identity survives configuration changes without persisting a GPS command.
    var pendingGpsPermissionJson by rememberSaveable { mutableStateOf("") }
    var gpsPermissionRequestInFlight by rememberSaveable { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val target = NativeGpsPermissionRecovery.decodePending(pendingGpsPermissionJson)
        pendingGpsPermissionJson = ""
        gpsPermissionRequestInFlight = false
        if (target != null) {
            if (!NativeGpsPermissionRecovery.mayStart(context, ActiveWorkoutStore.current(context), target, state.isFinishing)) {
                gpsRecovery = null
                viewModel.showStatus(strings.getString(R.string.native_gps_recovery_stale))
            } else {
                val blocker = NativeGpsPermissionRecovery.read(context, activity, requestedGpsPermission)
                if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    blocker == NativeGpsPermissionBlocker.READY) {
                    gpsRecovery = null
                    viewModel.toggleGpsTracking(target)
                } else {
                    // Approximate is not precise; a denied or disabled service never writes a GPS start.
                    gpsRecovery = NativeGpsRecoveryPrompt(target, blocker)
                }
            }
        }
    }
    fun requestGps(target: NativeWorkoutTarget, measurementId: String?) {
        if (measurementId != null) { viewModel.toggleGpsTracking(target, measurementId); return }
        if (!NativeGpsPermissionRecovery.mayStart(context, ActiveWorkoutStore.current(context), target, state.isFinishing)) {
            gpsRecovery = null
            viewModel.showStatus(strings.getString(R.string.native_gps_recovery_stale))
            return
        }
        if (gpsPermissionRequestInFlight) return // Only one Android launcher/callback for this screen.
        val blocker = NativeGpsPermissionRecovery.read(context, activity, requestedGpsPermission)
        if (blocker == NativeGpsPermissionBlocker.READY) {
            gpsRecovery = null
            viewModel.toggleGpsTracking(target)
        } else {
            // Do not open the app AlertDialog *over* the Android permission sheet.
            // The launcher result will show the recovery explanation only if the
            // user actually dismisses/denies system permission. No GPS start here.
            gpsRecovery = if (blocker == NativeGpsPermissionBlocker.DENIED && activity != null)
                null else NativeGpsRecoveryPrompt(target, blocker)
            if (blocker == NativeGpsPermissionBlocker.DENIED && activity != null) {
                pendingGpsPermissionJson = NativeGpsPermissionRecovery.encodePending(target)
                gpsPermissionRequestInFlight = true
                requestedGpsPermission = true
                try {
                    locationPermissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } catch (_: RuntimeException) {
                    pendingGpsPermissionJson = ""
                    gpsPermissionRequestInFlight = false
                    viewModel.showStatus(strings.getString(R.string.native_gps_recovery_request_unavailable))
                }
            } else if (blocker == NativeGpsPermissionBlocker.DENIED) {
                viewModel.showStatus(strings.getString(R.string.native_gps_recovery_request_unavailable))
            }
        }
    }

    // ON_RESUME reflects both independent Android switches without replaying an old GPS request.
    val latestRecovery by rememberUpdatedState(gpsRecovery)
    val latestRequestedBefore by rememberUpdatedState(requestedGpsPermission)
    DisposableEffect(lifecycleOwner, context, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                latestRecovery?.let { prompt ->
                    gpsRecovery = prompt.copy(blocker =
                        NativeGpsPermissionRecovery.read(context, activity, latestRequestedBefore))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Consume before waiting/permission: cancellation and recreation never replay an old start.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val target = launchGps
        onLaunchGpsConsumed()
        if (target != null && viewModel.awaitLaunchGps(target)) requestGps(target, null)
    }

    gpsRecovery?.let { recovery ->
        NativeGpsPermissionRecoveryDialog(
            blocker = recovery.blocker,
            language = language,
            onRetry = { requestGps(recovery.target, null) },
            onSettings = {
                NativeGpsPermissionRecovery.settingsIntent(context, recovery.blocker)?.let { intent ->
                    try {
                        if (activity == null) throw IllegalStateException("no activity")
                        activity.startActivity(intent)
                    } catch (_: Exception) {
                        viewModel.showStatus(strings.getString(R.string.native_gps_recovery_settings_unavailable))
                    }
                }
            },
            onDismiss = {
                // Cancellation invalidates any saved launcher callback; it cannot start GPS later.
                pendingGpsPermissionJson = ""
                // Do not release the launcher lease until the old result arrives;
                // otherwise a late callback could consume a newer pending target.
                gpsRecovery = null
            }
        )
    }

    // Both sections participate in measurement. An overlaid dock let scroll-to
    // expose controls behind its buttons, so a visible interval toggle could
    // receive a different action (or no click) on small displays.
    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 1. Fejléc: Cím, Időtartam, Szenzor státuszok és Ownership gomb
            ActiveWorkoutHeader(
                state = state,
                language = language,
                onTogglePause = { viewModel.toggleWorkoutPause(renderedWorkoutId, renderedGeneration) },
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
                        Icon(Icons.Default.Undo, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.getString(R.string.native_workout_set_recorded, state.undoExerciseName, state.undoSetNumber),
                            color = colors.text,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { viewModel.undoLastSet() }) {
                        Text(strings.getString(R.string.native_workout_undo), color = colors.accentText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            val selectedIndex = state.currentExerciseIndex.coerceIn(0, (state.exercises.size - 1).coerceAtLeast(0))
            val navigationTargets = state.exercises.map { ex -> ex.sets.firstOrNull()?.let {
                NativeWorkoutTarget(state.workoutId, state.generation, ex.exerciseId, it.setId)
            } }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { navigationTargets.getOrNull(selectedIndex - 1)?.let(viewModel::selectExercise) },
                    enabled = selectedIndex > 0 && state.timerOwner == "phone" && !state.isFinishing) { Text(strings.getString(R.string.native_workout_previous), color = colors.accent) }
                Text("${if (state.exercises.isEmpty()) 0 else selectedIndex + 1} / ${state.exercises.size}", color = colors.muted, modifier = Modifier.padding(top = 14.dp))
                TextButton(onClick = { navigationTargets.getOrNull(selectedIndex + 1)?.let(viewModel::selectExercise) },
                    enabled = selectedIndex < state.exercises.lastIndex && state.timerOwner == "phone" && !state.isFinishing) { Text(strings.getString(R.string.native_workout_next), color = colors.accent) }
            }

            // 3. Gyakorlatok és Szettek listája
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("active_workout_exercises")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                itemsIndexed(state.exercises.filterIndexed { index, _ -> index == selectedIndex }, key = { index, ex -> "${state.workoutId}:${state.generation}:${ex.sets.firstOrNull()?.setId ?: "empty:$index"}" }) { _, ex ->
                    // Capture identity now. A retained callback must never read a later workout from the State delegate.
                    val renderedTargets = ex.sets.map { NativeWorkoutTarget(state.workoutId, state.generation, ex.exerciseId, it.setId) }
                    fun target(index: Int) = renderedTargets.getOrNull(index)
                    ExerciseCardItem(
                        exercise = ex,
                        language = language,
                        onEditSet = { sIdx, field, v -> target(sIdx)?.let { viewModel.editSet(it, field, v) } },
                        onToggleSetDone = { sIdx -> target(sIdx)?.let { viewModel.toggleSetCompleted(it) } },
                        onSetRir = { sIdx, rir -> target(sIdx)?.let { viewModel.setSetRir(it, rir) } },
                        onToggleRirMode = { en -> target(0)?.let { viewModel.toggleSetRirMode(it, en) } },
                        onAddSet = { target(0)?.let { viewModel.addSetToExercise(it) } },
                        onRemoveSet = { sIdx -> target(sIdx)?.let { viewModel.removeSetFromExercise(it) } },
                        stopwatch = {
                            val nextIndex = ex.sets.indexOfFirst { !it.isCompleted }
                            val intervalActive = state.timerWorkoutId == state.workoutId && state.timerGeneration == state.generation &&
                                !state.timerRestOnly && state.timerKey.isNotEmpty() && !state.timerStopped && state.timerPhase != "complete"
                            val isDistance = ex.measurementType == "distance" || ex.repUnit.trim().lowercase() in setOf("m", "km")
                            if (isDistance) {
                                DistanceMeasurementPanel(state, ex,
                                     language = language,
                                    onToggle = { measurementId -> if (nextIndex >= 0) target(nextIndex)?.let { requestGps(it, measurementId) } },
                                    onOpenRoute = viewModel::openRouteMap)
                            } else if (ex.isTimed && nextIndex >= 0 && !intervalActive) {
                                LowerSetStopwatch(state, ex.sets[nextIndex], ex.isBilateral,
                                     language = language,
                                    onToggle = { side -> target(nextIndex)?.let { viewModel.toggleSetStopwatch(it, side) } },
                                    onReset = { target(nextIndex)?.let { viewModel.resetSetStopwatch(it) } })
                            }
                            target(if (nextIndex >= 0) nextIndex else 0)?.let { renderedTarget ->
                                val renderedInterval = NativeTimerTarget(renderedTarget.workoutId, renderedTarget.generation, state.timerKey)
                                val intervalIdentity = "${state.workoutId}:${state.generation}:${ex.exerciseId}"
                                IntervalOptions(state, ex, intervalActive, language,
                                    expanded = intervalExpansion[intervalIdentity] == true,
                                    onToggleExpanded = {
                                        intervalExpansion[intervalIdentity] = !(intervalExpansion[intervalIdentity] ?: false)
                                        if (com.repforge.app.BuildConfig.DEBUG) android.util.Log.d("NativeIntervalPanel",
                                            "expanded=${intervalExpansion[intervalIdentity]}")
                                    },
                                    onSave = { work, rest, rounds, sound, vibration, advance, global ->
                                        viewModel.configureIntervals(renderedTarget, work, rest, rounds, sound, vibration, advance, global) },
                                    onStart = { viewModel.startIntervals(renderedTarget) },
                                    onStop = { viewModel.stopIntervals(renderedInterval) },
                                    onSkip = { viewModel.skipRestTimer(renderedInterval) })
                            }
                        }
                    )
                    if (state.explicitTrainingMode && !ex.isTimed && ex.measurementType !in setOf("distance", "time", "gps") &&
                        ex.loadType != "bodyweight") {
                        Card(modifier = Modifier.fillMaxWidth().testTag("training_mode_coach_panel"),
                            colors = CardDefaults.cardColors(containerColor = colors.card)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.getString(R.string.native_modes_title),
                                    color = colors.text, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { viewModel.previewTrainingModeWeight(ex.exerciseId) },
                                    modifier = Modifier.fillMaxWidth().testTag("training_mode_coach_preview"),
                                    enabled = !state.modeProposalLoading && !state.isFinishing &&
                                        !state.isPaused && state.timerOwner == "phone") {
                                    Text(strings.getString(R.string.native_modes_coach_open))
                                }
                                if (state.modeProposalForExercise == ex.exerciseId) {
                                    Text(strings.getString(R.string.native_modes_coach_preview),
                                        color = colors.muted)
                                    Text(state.modeReason, color = colors.text)
                                    Text(strings.getString(R.string.native_modes_coach_history,
                                        state.modeHistoryCount), color = colors.muted)
                                    Text(strings.getString(R.string.native_modes_coach_sets,
                                        state.modeWorkingSets), color = colors.muted)
                                    if (state.modeSuggestedWeight != null && state.modeBaselineWeight != null) {
                                        Text(strings.getString(R.string.native_modes_coach_weight,
                                            state.modeBaselineWeight.toString(),
                                            state.modeSuggestedWeight.toString()), color = colors.text)
                                        Button(onClick = viewModel::reviewTrainingModeWeight,
                                            modifier = Modifier.fillMaxWidth().testTag("training_mode_coach_review"),
                                            enabled = !state.modeProposalLoading && !state.isPaused &&
                                                state.timerOwner == "phone") {
                                            Text(strings.getString(R.string.native_modes_coach_review))
                                        }
                                    } else {
                                        Text(strings.getString(R.string.native_modes_coach_no_action),
                                            color = colors.muted)
                                    }
                                }
                            }
                        }
                    }
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
                        Text(strings.getString(R.string.native_workout_add_plus), color = colors.text, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Csatolt fotók sora
                if (state.attachedPhotos.isNotEmpty()) {
                    item {
                        Text(strings.getString(R.string.native_workout_attached_photos), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                        Icon(Icons.Default.Close, contentDescription = strings.getString(R.string.native_workout_delete), tint = colors.dangerText, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }

        // 4. Alsó vezérlősáv és Élő Pihenőidő sáv
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_workout_controls")
                .background(colors.card.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Ha pihenőidő fut, élő pihenősáv jelenik meg gombokkal
            if (state.timerRestOnly && state.timerPhase == "rest" && state.timerRemainingSec > 0 && state.timerWorkoutId == state.workoutId &&
                state.timerGeneration == state.generation && state.timerOwner == "phone") {
                val renderedTimer = NativeTimerTarget(state.workoutId, state.generation, state.timerKey)
                RestTimerFloatingBar(
                    remainingSec = state.timerRemainingSec,
                    paused = !state.timerRunning,
                    canToggle = !state.isPaused && !state.isFinishing,
                    onSetPaused = { paused -> viewModel.setRestPaused(renderedTimer, paused) },
                    onAdjust = { delta -> viewModel.adjustRestTimer(renderedTimer, delta) },
                    onSkip = { viewModel.skipRestTimer(renderedTimer) },
                    canAdjust = state.timerRestOnly && state.timerRunning && !state.isPaused,
                    language = language
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
                    Icon(Icons.Default.CameraAlt, contentDescription = strings.getString(R.string.native_workout_camera), tint = colors.text)
                }
                // Galéria gomb
                IconButton(
                    onClick = {
                        galleryLauncher.launch(photoRepo.createPickImageIntent())
                    },
                    modifier = Modifier.size(48.dp).background(colors.card2, CircleShape)
                ) {
                    Icon(Icons.Default.Image, contentDescription = strings.getString(R.string.native_workout_gallery), tint = colors.text)
                }
                // Befejezés gomb
                Button(
                    onClick = { viewModel.openFinishModal() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.onAccent)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.getString(R.string.native_workout_finish), color = colors.onAccent, fontWeight = FontWeight.Bold)
                }
                // Elvetés gomb
                Button(
                    onClick = { viewModel.openDiscardModal() },
                    modifier = Modifier.height(48.dp).testTag("workout_discard"),
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
                Text(strings.getString(R.string.native_workout_add_exercise), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                Text(when (language) { "en" -> libEx.nameEn; "de" -> libEx.nameDe; "ro" -> libEx.nameRo; else -> libEx.nameHu }, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
            language = language,
            onDismiss = { viewModel.closeFinishModal() },
            onConfirm = { effort ->
                viewModel.closeFinishModal()
                viewModel.finishWorkout(effort) { navController.popBackStack() }
            }
        )
    }

    // Elvetés megerősítő dialógus
    if (state.showDiscardModal) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDiscardModal() },
            title = { Text(strings.getString(R.string.native_workout_discard_title), color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text(strings.getString(R.string.native_workout_discard_message), color = colors.muted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeDiscardModal()
                        viewModel.discardWorkout { navController.popBackStack() }
                    },
                    modifier = Modifier.testTag("workout_confirm_discard"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text(strings.getString(R.string.native_workout_delete), color = colors.dangerText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDiscardModal() }) {
                    Text(strings.getString(R.string.native_workout_cancel), color = colors.text)
                }
            },
            containerColor = colors.card
        )
    }

    if (state.modeReviewOpen) {
        AlertDialog(
            onDismissRequest = viewModel::cancelTrainingModeReview,
            title = { Text(strings.getString(R.string.native_modes_coach_confirm), color = colors.text) },
            text = { Text(strings.getString(R.string.native_modes_no_rewrite), color = colors.muted) },
            confirmButton = {
                Button(onClick = viewModel::approveTrainingModeWeight,
                    enabled = !state.modeProposalLoading,
                    modifier = Modifier.testTag("training_mode_coach_approve")) {
                    Text(strings.getString(R.string.native_modes_coach_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelTrainingModeReview,
                    modifier = Modifier.testTag("training_mode_coach_cancel")) {
                    Text(strings.getString(R.string.native_modes_coach_cancel))
                }
            }, containerColor = colors.card
        )
    }

    state.activeRoute?.let { route -> WorkoutRouteMapDialog(route, onDismiss = viewModel::closeRouteMap) }

    if (state.isFinishing) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { }, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.accent)
        }
    }
    state.statusMessage?.let { message ->
        AlertDialog(onDismissRequest = { viewModel.clearStatusMessage() },
            title = { Text(strings.getString(R.string.native_workout_status)) }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { viewModel.clearStatusMessage() }, modifier = Modifier.testTag("workout_status_dismiss")) { Text(strings.getString(R.string.native_workout_ok)) } })
    }
}

@Composable
fun ActiveWorkoutHeader(
    state: ActiveWorkoutUiState,
    onTogglePause: () -> Unit,
    onOwnershipAction: () -> Unit,
    language: String = "hu"
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
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
                        text = if (state.isPaused) strings.getString(R.string.native_workout_component_paused) else if (state.timerOwner == "watch") strings.getString(R.string.native_workout_component_owner_watch) else strings.getString(R.string.native_workout_component_owner_phone),
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
                        contentDescription = strings.getString(if (state.isPaused) R.string.native_workout_component_resume else R.string.native_workout_component_pause),
                        tint = colors.accent
                    )
                }
            }

            Text(strings.getString(R.string.native_workout_component_measured_work, measuredTime(state.measuredWorkSeconds), measuredTime(state.measuredRestSeconds)),
                color = colors.muted, fontSize = 11.sp)
            if ((state.measuredPauseSeconds ?: 0L) > 0) Text(strings.getString(R.string.native_workout_component_measured_pause, measuredTime(state.measuredPauseSeconds)), color = colors.muted, fontSize = 11.sp)
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
                            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(14.dp))
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
                            text = strings.getString(if (state.timerOwner == "watch") R.string.native_workout_component_takeback else R.string.native_workout_component_start_watch),
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
    language: String = "hu",
    onEditSet: (Int, String, String) -> Unit,
    onToggleSetDone: (Int) -> Unit,
    onSetRir: (Int, Int?) -> Unit,
    onToggleRirMode: (Boolean) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    stopwatch: @Composable () -> Unit = {}
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val isDistance = exercise.measurementType == "distance" || exercise.repUnit.trim().lowercase() in setOf("m", "km")
    var manualDistance by remember(exercise.sets.firstOrNull()?.setId) { mutableStateOf(false) }
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
                Text(strings.getString(R.string.native_workout_component_sets_done, exercise.sets.count { it.isCompleted }, exercise.sets.size), color = colors.accentText, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))

            stopwatch()

            if (isDistance) TextButton(onClick = { manualDistance = !manualDistance }) {
                Text(strings.getString(R.string.native_workout_component_manual), color = colors.muted)
            }
            if (!isDistance || manualDistance) {
            // Szett fejléc
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("#", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                Text(strings.getString(if (isDistance) R.string.native_workout_component_distance else R.string.native_workout_component_weight), color = colors.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(strings.getString(if (exercise.isBilateral) R.string.native_workout_component_bilateral else if (exercise.isTimed || isDistance) R.string.native_workout_component_time else R.string.native_workout_component_reps), color = colors.muted, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                if (exercise.setRirEnabled) {
                    Text("RIR", color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                }
                Text(strings.getString(R.string.native_workout_component_done), color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(6.dp))

            // Szettek sorai
            exercise.sets.forEachIndexed { sIdx, set ->
                key(set.setId) {
                SetRowItem(
                    set = set,
                    language = language,
                    isBilateral = exercise.isBilateral,
                    isDistance = isDistance,
                    onDistanceChange = { onEditSet(sIdx, "distanceKm", it) },
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
            }

            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddSet) {
                    Text(strings.getString(R.string.native_workout_component_add_set), color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { onToggleRirMode(!exercise.setRirEnabled) }) {
                    Text(strings.getString(if (exercise.setRirEnabled) R.string.native_workout_component_hide_rir else R.string.native_workout_component_show_rir), color = colors.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DistanceMeasurementPanel(state: ActiveWorkoutUiState, exercise: ActiveExerciseUiModel, onToggle: (String?) -> Unit, onOpenRoute: (String) -> Unit, language: String = "hu") {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val set = exercise.sets.firstOrNull { !it.isCompleted } ?: exercise.sets.lastOrNull() ?: return
    val bound = state.gpsBoundSetId == set.setId
    val live = bound && state.gpsBindingState in setOf("prepared", "running", "explicitStopped")
    val meters = if (live) state.gpsBaseMeters + state.gpsDistanceMeters else set.distanceMeters
    val seconds = if (live) state.gpsBaseSeconds + state.gpsElapsedSeconds else set.reps.toLongOrNull() ?: 0L
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text("${"%.2f".format(meters / 1000.0)} km · ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
            color = colors.accentText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(strings.getString(if (set.isCompleted) R.string.native_workout_component_gps_saved else if (live && !state.gpsHasFix) R.string.native_workout_component_gps_wait else if (live) R.string.native_workout_component_gps_running else R.string.native_workout_component_gps_ready),
            color = colors.muted, fontSize = 12.sp)
        Button(onClick = { onToggle(if (live) state.gpsMeasurementId else null) }, enabled = !set.isCompleted && state.timerOwner == "phone" && !state.isPaused && !state.isFinishing,
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
            Text(strings.getString(if (live && state.gpsBindingState == "explicitStopped") R.string.native_workout_component_gps_retry else if (live) R.string.native_workout_component_gps_finish else R.string.native_workout_component_gps_start),
                color = colors.onAccent)
        }
        if (bound && state.gpsError.isNotEmpty()) Text(state.gpsError, color = colors.dangerText, fontSize = 12.sp)
        val route = exercise.sets.lastOrNull { it.gpsRouteKey.isNotBlank() }?.gpsRouteKey
        if (route != null) TextButton(onClick = { onOpenRoute(route) }) { Text(strings.getString(R.string.native_workout_component_map), color = colors.accent) }
    }
}

private fun measuredTime(seconds: Long?): String = seconds?.let {
    "${it / 60}:${(it % 60).toString().padStart(2, '0')}"
} ?: "—"

@Composable
private fun IntervalOptions(
    state: ActiveWorkoutUiState,
    exercise: ActiveExerciseUiModel,
    active: Boolean,
    language: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSave: (String, String, String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val text = remember(context, language) { com.repforge.app.ui.text.nativeResources(context, language) }
    val identity = "${state.workoutId}:${state.generation}:${exercise.sets.firstOrNull()?.setId}"
    var work by remember(identity, state.intervalWork) { mutableStateOf(state.intervalWork.toString()) }
    var rest by remember(identity, state.intervalRest) { mutableStateOf(state.intervalRest.toString()) }
    var rounds by remember(identity, state.intervalRounds) { mutableStateOf(state.intervalRounds.toString()) }
    var sound by remember(identity, state.intervalSound) { mutableStateOf(state.intervalSound) }
    var vibration by remember(identity, state.intervalVibration) { mutableStateOf(state.intervalVibration) }
    var advance by remember(identity, state.intervalAutoAdvance) { mutableStateOf(state.intervalAutoAdvance) }
    var global by remember(identity) { mutableStateOf(false) }
    val singleTimed = exercise.isTimed && !exercise.isBilateral
    val editable = state.timerOwner == "phone" && !state.isFinishing && !state.intervalSaving
    val saved = work == state.intervalWork.toString() && rest == state.intervalRest.toString() && rounds == state.intervalRounds.toString() &&
        sound == state.intervalSound && vibration == state.intervalVibration && advance == state.intervalAutoAdvance
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        TextButton(onClick = onToggleExpanded, modifier = Modifier.fillMaxWidth().testTag("interval_options")) {
            Text(text.getString(com.repforge.app.R.string.interval_title) +
                (if (active) " · ${measuredTime(state.timerRemainingSec.toLong())}" else "") + (if (expanded) " ▴" else " ▾"), color = colors.accentText)
        }
        if (expanded) {
            Text(text.getString(com.repforge.app.R.string.interval_scope), color = colors.muted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (singleTimed) OutlinedTextField(value = work, onValueChange = { work = it }, label = { Text(text.getString(com.repforge.app.R.string.interval_work)) },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = editable)
                OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text(text.getString(com.repforge.app.R.string.interval_rest)) },
                    modifier = Modifier.weight(1f).testTag("interval_rest"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = editable)
                if (singleTimed) OutlinedTextField(value = rounds, onValueChange = { rounds = it }, label = { Text(text.getString(com.repforge.app.R.string.interval_rounds)) },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = editable)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = sound, onCheckedChange = { sound = it }, enabled = editable)
                Text(text.getString(com.repforge.app.R.string.interval_sound), color = colors.text, modifier = Modifier.weight(1f))
                Checkbox(checked = vibration, onCheckedChange = { vibration = it }, enabled = editable)
                Text(text.getString(com.repforge.app.R.string.interval_vibration), color = colors.text, modifier = Modifier.weight(1f))
            }
            if (singleTimed) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = advance, onCheckedChange = { advance = it }, enabled = editable,
                    modifier = Modifier.testTag("interval_auto_advance"))
                Text(text.getString(com.repforge.app.R.string.interval_auto_advance), color = colors.text)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = global, onCheckedChange = { global = it }, enabled = editable,
                    modifier = Modifier.testTag("interval_global"))
                Text(text.getString(com.repforge.app.R.string.interval_global), color = colors.text)
            }
            TextButton(onClick = { onSave(work, rest, rounds, sound, vibration, advance, global) },
                enabled = editable && (!saved || global), modifier = Modifier.testTag("interval_save")) {
                Text(text.getString(com.repforge.app.R.string.interval_save), color = colors.accentText)
            }
            if (active) {
                Text(text.getString(com.repforge.app.R.string.interval_round_status, state.timerRound, state.timerRounds), color = colors.muted, fontSize = 12.sp)
                if (state.timerPhase == "rest") TextButton(onClick = onSkip, enabled = editable && state.timerRunning && !state.isPaused) {
                    Text(text.getString(com.repforge.app.R.string.interval_skip_rest), color = colors.accentText)
                }
                Button(onClick = onStop, enabled = editable, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(text.getString(com.repforge.app.R.string.interval_stop))
                }
            } else if (singleTimed && exercise.sets.any { !it.isCompleted }) {
                Text(text.getString(if (advance) com.repforge.app.R.string.interval_auto_note else com.repforge.app.R.string.interval_manual_note),
                    color = colors.muted, fontSize = 12.sp)
                Button(onClick = onStart, enabled = editable && saved && !state.isPaused && !state.stopwatchRunning && !state.isGpsActive,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("interval_start")) {
                    Text(text.getString(com.repforge.app.R.string.interval_start))
                }
            }
        }
    }
}

/** The original lower set stopwatch; there is no second upper interval clock. */
@Composable
private fun LowerSetStopwatch(
    state: ActiveWorkoutUiState,
    set: ActiveSetUiModel,
    bilateral: Boolean,
    onToggle: (String) -> Unit,
    onReset: () -> Unit,
    language: String = "hu"
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val bound = state.stopwatchSetId == set.setId
    val enabled = state.timerOwner == "phone" && !state.isPaused && !state.isFinishing && !state.isTransferPending
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(strings.getString(R.string.native_workout_component_stopwatch_set, set.setNumber), color = colors.text, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (if (bilateral) listOf("left", "right") else listOf("single")).forEach { side ->
                val matching = bound && state.stopwatchSide == side
                val running = matching && state.stopwatchRunning
                val seconds = if (matching) state.stopwatchSeconds else when (side) {
                    "left" -> set.leftSeconds.toLong(); "right" -> set.rightSeconds.toLong(); else -> 0L
                }
                Column(Modifier.weight(1f)) {
                    if (bilateral) Text(strings.getString(if (side == "left") R.string.native_workout_component_left else R.string.native_workout_component_right), color = colors.muted, fontSize = 12.sp)
                    Text("${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}",
                        color = colors.accentText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { onToggle(side) }, enabled = enabled && (!state.stopwatchRunning || running),
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                        Text(strings.getString(if (running) R.string.native_workout_component_stop else R.string.native_workout_component_start), color = colors.onAccent)
                    }
                }
            }
        }
        if (bound) TextButton(onClick = onReset, enabled = enabled) { Text(strings.getString(R.string.native_workout_component_reset), color = colors.muted) }
        Text(strings.getString(if (bilateral) R.string.native_workout_component_bilateral_note else R.string.native_workout_component_stopwatch_note), color = colors.muted, fontSize = 12.sp)
    }
}

@Composable
fun SetRowItem(
    set: ActiveSetUiModel,
    language: String = "hu",
    isBilateral: Boolean,
    isDistance: Boolean = false,
    onDistanceChange: (String) -> Unit = {},
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
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${set.setNumber}", color = colors.text, fontSize = 13.sp, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)

        // Súly beviteli mező
        OutlinedTextField(
            value = if (isDistance) (set.distanceMeters / 1000.0).takeIf { it > 0.0 }?.toString().orEmpty() else set.weight,
            onValueChange = if (isDistance) onDistanceChange else onWeightChange,
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
                    placeholder = { Text(strings.getString(R.string.native_workout_left_initial), fontSize = 10.sp, color = colors.muted) },
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
                    placeholder = { Text(strings.getString(R.string.native_workout_right_initial), fontSize = 10.sp, color = colors.muted) },
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
                checkmarkColor = colors.onAccent
            )
        )
    }
}

@Composable
fun RestTimerFloatingBar(
    remainingSec: Int,
    paused: Boolean,
    onSetPaused: (Boolean) -> Unit,
    canToggle: Boolean,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    canAdjust: Boolean = true,
    language: String = "hu"
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.card2)
        .border(1.dp, colors.emphasisBorder, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("⏱ ${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                color = colors.accentText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(strings.getString(R.string.native_workout_component_rest), color = colors.muted,
                fontSize = 11.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            IconButton(onClick = { onSetPaused(!paused) }, enabled = canToggle,
                modifier = Modifier.size(48.dp).testTag("workout_rest_pause")) {
                Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = strings.getString(if (paused) R.string.native_workout_component_resume else R.string.native_workout_component_pause),
                    tint = colors.text)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onAdjust(-15) }, enabled = canAdjust) { Text("-15s", color = colors.text, fontSize = 11.sp) }
            TextButton(onClick = { onAdjust(15) }, enabled = canAdjust) { Text("+15s", color = colors.text, fontSize = 11.sp) }
            Button(onClick = onSkip, enabled = canAdjust, modifier = Modifier.testTag("workout_rest_skip"),
                shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.accent)) {
                Text(strings.getString(R.string.native_workout_component_skip), color = colors.onAccent,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FinishWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    language: String = "hu"
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val choices = listOf(
        "easy" to strings.getString(R.string.native_workout_feedback_easy),
        "light" to strings.getString(R.string.native_workout_feedback_light),
        "good" to strings.getString(R.string.native_workout_feedback_good),
        "challenging" to strings.getString(R.string.native_workout_feedback_challenging),
        "hard" to strings.getString(R.string.native_workout_feedback_hard)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.getString(R.string.native_workout_feedback_title), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.getString(R.string.native_workout_feedback_hint), color = colors.muted, fontSize = 12.sp)
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
                    Text(strings.getString(R.string.native_workout_feedback_pain), color = colors.dangerText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text(strings.getString(R.string.native_workout_feedback_skip), color = colors.accentText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.getString(R.string.native_workout_cancel), color = colors.muted)
            }
        },
        containerColor = colors.card
    )
}
