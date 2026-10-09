package com.repforge.app.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.wear.remote.interactions.RemoteActivityHelper
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val workout = mutableStateOf<WearWorkout?>(null)
    private val home = mutableStateOf<WatchHomeSnapshot?>(null)
    private val closure = mutableStateOf<WearClosure?>(null)
    private val restRemaining = mutableIntStateOf(0)
    private var restTimer: CountDownTimer? = null
    private val health=mutableStateOf<JSONObject?>(null)
    private val routeRequest=mutableStateOf<String?>(null)
    private val measuring=mutableStateOf(false)
    private val gpsEnabled=mutableStateOf(false)
    private val uiHandler=Handler(Looper.getMainLooper())
    private val healthTick=object : Runnable {
        override fun run() {
            refreshHealth()
            if(WearHealthStore.active(this@MainActivity).isBlank() && workout.value?.workoutId?.let { WearHealthStore.load(this@MainActivity,it)==null }==true)startHealthIfEnabled()
            uiHandler.postDelayed(this,1000)
        }
    }
    private val permissionRequest=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val allowed=healthAllowed()
        WearHealthStore.enable(this,allowed);measuring.value=allowed
        if(WearHealthStore.gps(this) && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)WearHealthStore.gps(this,false)
        gpsEnabled.value=WearHealthStore.gps(this)
        if(allowed){workout.value?.let { WearHealthStore.retry(this,it.workoutId) };startHealthIfEnabled()} else Toast.makeText(this,"Órás méréshez engedély szükséges.",Toast.LENGTH_LONG).show()
    }
    private fun healthAllowed(): Boolean = checkSelfPermission(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS)==PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED
    private fun refreshHealth() {
        val id=workout.value?.workoutId ?: closure.value?.workoutId ?: ""
        health.value=WearHealthStore.load(this,id)?.let { WearHealthAccumulator(it).snapshot() }
        measuring.value=WearHealthStore.enabled(this);gpsEnabled.value=WearHealthStore.gps(this)
    }
    private fun requestHealth(gps: Boolean=false) {
        WearHealthStore.gps(this,gps)
        val permissions=mutableListOf(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS,Manifest.permission.ACTIVITY_RECOGNITION)
        if(Build.VERSION.SDK_INT>=33)permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if(gps)permissions.addAll(listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        permissionRequest.launch(permissions.toTypedArray())
    }
    private fun startHealthIfEnabled() {
        if(!WearHealthStore.enabled(this) || !healthAllowed())return
        val current=workout.value ?: return
        var previous=WearHealthStore.load(this,current.workoutId)
        if(previous?.optString("state")=="error" && WearHealthFutures.isLegacyNullCompletionError(previous.optString("message"))) {
            // Recover the old Void-callback bug within the same workout, keeping its measured totals.
            WearHealthStore.retry(this,current.workoutId)
            previous=WearHealthStore.load(this,current.workoutId)
        }
        if(previous?.optString("state") in listOf("ended","error"))return
        try { WearHealthService.start(this,current) }catch(error:Exception) { Toast.makeText(this,"A mérés nem indítható: ${error.message}",Toast.LENGTH_LONG).show() }
    }
    private fun openOnPhone() {
        try {
            val task=RemoteActivityHelper(this,mainExecutor).startRemoteActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("trainpilot://wear/open")).addCategory(Intent.CATEGORY_BROWSABLE),null)
            task.addListener({ try { task.get();Toast.makeText(this,"Megnyitva a telefonon",Toast.LENGTH_SHORT).show() }catch(_:Exception) { Toast.makeText(this,"A telefon nem érhető el. Ellenőrizd a kapcsolatot.",Toast.LENGTH_LONG).show() } },mainExecutor)
        }catch(_:Exception) { Toast.makeText(this,"A telefon nem érhető el.",Toast.LENGTH_LONG).show() }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent);setIntent(intent);routeRequest.value=intent.getStringExtra("destination")?.takeIf { it in listOf("home","workout","summary","menu","calendar") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workout.value = WorkoutSnapshotStore.load(this)
        home.value = WatchHomeStore.load(this)
        closure.value = WearClosureStore.load(this)
        restoreRestFromSnapshot(workout.value)
        refreshHealth()
        routeRequest.value=intent.getStringExtra("destination")?.takeIf { it in listOf("home","workout","summary","menu","calendar") }
        setContent {
            TrainPilotWearApp(
                workout = workout,
                home = home,
                restRemaining = restRemaining,
                closure = closure,
                health = health,
                measuring = measuring,
                gpsEnabled = gpsEnabled,
                requestedRoute = routeRequest,
                onRouteConsumed = { routeRequest.value=null },
                onEnableHealth = { requestHealth(WearHealthStore.gps(this)) },
                onDisableHealth = { WearHealthStore.enable(this,false);WearHealthService.stop(this);refreshHealth() },
                onToggleGps = { if(WearHealthStore.gps(this)){WearHealthStore.gps(this,false);refreshHealth()}else requestHealth(true) },
                onOpenPhone = ::openOnPhone,
                onFinish = { closeWorkout("finishWorkout") },
                onDiscard = { closeWorkout("discardWorkout") },
                onRetry = ::retryClosure,
                onDismissClosure = { if (closure.value?.status in listOf("saved", "discarded")) { WearClosureStore.dismiss(this); closure.value = null } },
                onResumeWorkout = { sendCommand("resumeWorkout") },
                onSelectExercise = ::selectExercise,
                onStart = ::startWorkoutFromWatch,
                onStartQuick = ::startQuickWorkoutFromWatch,
                onAddQuick = ::addQuickExerciseFromWatch,
                onChange = ::changeCurrentSet,
                onComplete = ::completeCurrentSet,
                onPrevious = { navigateExercise(-1) },
                onNext = { navigateExercise(1) },
                onSkipRest = ::skipRest
            )
        }
    }

    override fun onResume() {
        super.onResume()
        workout.value = WorkoutSnapshotStore.load(this)
        home.value = WatchHomeStore.load(this)
        closure.value = WearClosureStore.load(this)
        restoreRestFromSnapshot(workout.value)
        Wearable.getDataClient(this).addListener(this)
        refreshFromDataLayer()
        WearCommandOutbox.flush(this)
        uiHandler.removeCallbacks(healthTick);uiHandler.post(healthTick)
        if(workout.value==null && WearHealthStore.active(this).isNotBlank())WearHealthService.stop(this)
        startHealthIfEnabled()
    }

    override fun onPause() {
        uiHandler.removeCallbacks(healthTick)
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        restTimer?.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot")
            when (item.uri.path) {
                WearDataListenerService.ACTIVE_WORKOUT_PATH -> {
                    runOnUiThread {
                        val parsed = WorkoutSnapshotStore.save(this, raw)
                        workout.value = parsed
                        restoreRestFromSnapshot(parsed)
                        if(parsed==null)WearHealthService.stop(this) else startHealthIfEnabled()
                        WearSurfaces.refresh(this,true)
                    }
                }
                WearDataListenerService.WATCH_HOME_PATH -> {
                    val parsed = WatchHomeStore.save(this, raw)
                    runOnUiThread { home.value = parsed; closure.value = WearClosureStore.load(this) }
                }
            }
        }
    }

    private fun refreshFromDataLayer() {
        Wearable.getDataClient(this).getDataItems()
            .addOnSuccessListener { items ->
                try {
                    var newestRaw: String? = null
                    var newestRevision = Long.MIN_VALUE
                    var newestHome: WatchHomeSnapshot? = null
                    for (item in items) {
                        try {
                            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot") ?: continue
                            when (item.uri.path) {
                                WearDataListenerService.ACTIVE_WORKOUT_PATH -> {
                                    val revision = JSONObject(raw).optLong("revision", 0L)
                                    if (revision >= newestRevision) { newestRevision = revision; newestRaw = raw }
                                }
                                WearDataListenerService.WATCH_HOME_PATH -> newestHome = WatchHomeStore.save(this, raw) ?: newestHome
                            }
                        } catch (_: Exception) { /* Keep the cache when a DataItem is malformed. */ }
                    }
                    newestRaw?.let {
                        workout.value = WorkoutSnapshotStore.save(this, it)
                        restoreRestFromSnapshot(workout.value)
                    }
                    if (newestHome != null) home.value = newestHome
                    closure.value = WearClosureStore.load(this)
                } finally {
                    items.release()
                }
            }
    }

    private fun startWorkoutFromWatch(day: WatchHomeDay, scheduleId: String) {
        val source = home.value ?: return
        if (workout.value != null || source.hasDraft || closure.value?.status in listOf("pending", "error") || day.exercises.isEmpty()) return
        val started = Instant.ofEpochMilli(System.currentTimeMillis()).toString()
        val localWorkout = source.createWorkout(day, scheduleId, started)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, localWorkout)
        sendStartCommand(day, scheduleId, started)
        startHealthIfEnabled();WearSurfaces.refresh(this,true)
    }

    private fun startQuickWorkoutFromWatch(exercise: WatchHomeExercise) {
        val source = home.value ?: return
        if (workout.value != null || source.hasDraft ||
            closure.value?.status in listOf("pending", "error") ||
            source.quickExercises.none { it.id == exercise.id }) return
        val started = Instant.ofEpochMilli(System.currentTimeMillis()).toString()
        val localWorkout = source.createQuickWorkout(exercise, started)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, localWorkout)
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", UUID.randomUUID().toString())
            .put("workoutId", started)
            .put("action", "startQuickWorkout")
            .put("exerciseId", exercise.id)
            .put("started", started)
            .put("createdAt", System.currentTimeMillis())
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = sequence)) }
        startHealthIfEnabled()
        WearSurfaces.refresh(this, true)
    }

    private fun addQuickExerciseFromWatch(exercise: WatchHomeExercise) {
        val current = workout.value ?: return
        val source = home.value ?: return
        if (!current.quickWorkout || current.exercises.size >= 20 ||
            current.exercises.any { it.id == exercise.id } ||
            source.quickExercises.none { it.id == exercise.id }) return
        val updated = current.copy(exercises = current.exercises + source.toWearExercise(exercise),
            currentExercise = current.exercises.size, restEndAt = 0L)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, updated)
        sendCommand("addQuickExercise", exerciseIndex = updated.currentExercise)
        WearSurfaces.refresh(this, true)
    }

    private fun sendStartCommand(day: WatchHomeDay, scheduleId: String, started: String) {
        val commandId = UUID.randomUUID().toString()
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", commandId)
            .put("workoutId", started)
            .put("action", "startWorkout")
            .put("programId", day.programId)
            .put("programName", day.programName)
            .put("dayId", day.id)
            .put("scheduleId", scheduleId)
            .put("started", started)
            .put("createdAt", System.currentTimeMillis())
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = sequence)) }
    }

    private fun restoreRestFromSnapshot(value: WearWorkout?) {
        val endAt = value?.restEndAt ?: 0L
        if (endAt > System.currentTimeMillis()) startRest(max(1, ((endAt - System.currentTimeMillis() + 999L) / 1000L).toInt()), true)
        else { restTimer?.cancel(); restRemaining.intValue = 0 }
    }

    private fun startRest(seconds: Int, vibrateAtEnd: Boolean) {
        restTimer?.cancel()
        if (seconds <= 0) { restRemaining.intValue = 0; return }
        restRemaining.intValue = seconds
        restTimer = object : CountDownTimer(seconds * 1000L, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                restRemaining.intValue = max(0, ((millisUntilFinished + 999L) / 1000L).toInt())
            }
            override fun onFinish() {
                restRemaining.intValue = 0
                workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this@MainActivity, it.endRest()) }
                if (vibrateAtEnd) getSystemService(Vibrator::class.java)?.vibrate(
                    VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }.start()
    }

    private fun skipRest() {
        restTimer?.cancel()
        restRemaining.intValue = 0
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.endRest()) }
        sendCommand("skipRest")
    }

    private fun changeCurrentSet(field: String, delta: Double) {
        val current = workout.value ?: return
        val exerciseIndex = current.currentExercise
        val exercise = current.exercise ?: return
        val setIndex = current.currentSetIndex
        val set = exercise.sets.getOrNull(setIndex) ?: return
        val updated = when (field) {
            "weight" -> set.copy(weight = trimNumber(max(0.0, (set.weight.toDoubleOrNull() ?: 0.0) + delta)))
            "reps" -> {
                val value = max(0, (set.reps.toIntOrNull() ?: 0) + delta.roundToInt())
                set.copy(reps = if (value == 0) "" else value.toString())
            }
            "leftSeconds" -> set.copy(leftSeconds = max(0, set.leftSeconds + delta.roundToInt()))
            "rightSeconds" -> set.copy(rightSeconds = max(0, set.rightSeconds + delta.roundToInt()))
            else -> return
        }
        val normalized = if ((field == "leftSeconds" || field == "rightSeconds") && updated.leftSeconds > 0 && updated.rightSeconds > 0)
            updated.copy(reps = minOf(updated.leftSeconds, updated.rightSeconds).toString()) else updated
        replaceSet(current, exerciseIndex, setIndex, normalized)
        sendCommand(
            action = "updateSet",
            exerciseIndex = exerciseIndex,
            setIndex = setIndex,
            field = field,
            value = when (field) {
                "weight" -> normalized.weight
                "reps" -> normalized.reps.ifBlank { "0" }
                "leftSeconds" -> normalized.leftSeconds
                "rightSeconds" -> normalized.rightSeconds
                else -> 0
            }
        )
    }

    private fun completeCurrentSet() {
        if (restRemaining.intValue > 0) return
        val current = workout.value ?: return
        val exerciseIndex = current.currentExercise
        val exercise = current.exercise ?: return
        val setIndex = current.currentSetIndex
        val set = exercise.sets.getOrNull(setIndex) ?: return
        if (set.done) return
        val perSide = normalizeUnit(exercise.repUnit) == "mp/oldal"
        val valid = if (perSide) set.leftSeconds > 0 && set.rightSeconds > 0 else (set.reps.toIntOrNull() ?: 0) > 0
        if (!valid) return
        replaceSet(current, exerciseIndex, setIndex, set.copy(done = true))
        sendCommand(
            action = "completeSet",
            exerciseIndex = exerciseIndex,
            setIndex = setIndex,
            reps = set.reps.ifBlank { "0" },
            weight = set.weight,
            leftSeconds = set.leftSeconds,
            rightSeconds = set.rightSeconds
        )
        val finishedLastExercise = workout.value?.let { it.currentExercise == it.exercises.lastIndex && it.exerciseComplete } == true
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(restEndAt = if (finishedLastExercise) 0L else System.currentTimeMillis() + current.restSeconds * 1000L)) }
        WearStopwatchStore.clear(this)
        startRest(if (finishedLastExercise) 0 else current.restSeconds, true)
    }

    private fun navigateExercise(delta: Int) {
        val current = workout.value ?: return
        val next = (current.currentExercise + delta).coerceIn(0, current.exercises.lastIndex)
        selectExercise(next)
    }

    private fun replaceSet(current: WearWorkout, exerciseIndex: Int, setIndex: Int, updated: WearSet) {
        val exercise = current.exercises[exerciseIndex]
        val sets = exercise.sets.toMutableList()
        sets[setIndex] = updated
        val exercises = current.exercises.toMutableList()
        exercises[exerciseIndex] = exercise.copy(sets = sets)
        workout.value = current.copy(exercises = exercises)
        WorkoutSnapshotStore.saveWorkout(this, workout.value ?: return)
    }

    private fun sendCommand(
        action: String,
        exerciseIndex: Int? = null,
        setIndex: Int? = null,
        field: String? = null,
        value: Any? = null,
        reps: String? = null,
        weight: String? = null,
        leftSeconds: Int? = null,
        rightSeconds: Int? = null
    ) {
        val current = workout.value ?: return
        val exercise = exerciseIndex?.let { current.exercises.getOrNull(it) }
        val set = if (exercise != null && setIndex != null) exercise.sets.getOrNull(setIndex) else null
        val commandId = UUID.randomUUID().toString()
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", commandId)
            .put("baseRevision", current.revision)
            .put("watchSequence", current.localSequence)
            .put("workoutId", current.workoutId)
            .put("action", action)
            .put("createdAt", System.currentTimeMillis())
        if (exerciseIndex != null) command.put("exerciseIndex", exerciseIndex)
        if (exercise != null) command.put("exerciseId", exercise.id)
        if (setIndex != null) command.put("setIndex", setIndex)
        if (set != null) command.put("setNumber", set.number)
        if (field != null) command.put("field", field)
        if (value != null) command.put("value", value)
        if (reps != null) command.put("reps", reps)
        if (weight != null) command.put("weight", weight)
        if (leftSeconds != null) command.put("leftSeconds", leftSeconds)
        if (rightSeconds != null) command.put("rightSeconds", rightSeconds)
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = sequence)) }
    }

    private fun selectExercise(index: Int) {
        val current = workout.value ?: return
        if (current.selectExercise(index) === current) return
        WearStopwatchStore.pause(this)
        // Order the durable commands: end the old rest first, then select an
        // absolute exercise so a different phone screen cannot offset the move.
        if (restRemaining.intValue > 0 || current.restEndAt > System.currentTimeMillis()) skipRest()
        val latest = workout.value ?: return
        workout.value = WorkoutSnapshotStore.saveWorkout(this, latest.selectExercise(index))
        sendCommand("selectExercise", exerciseIndex = index)
    }

    private fun closeWorkout(action: String) {
        val current = workout.value ?: return
        if (action == "finishWorkout" && current.completedSets == 0) return
        val command = JSONObject()
            .put("schema", 1).put("commandId", UUID.randomUUID().toString())
            .put("workoutId", current.workoutId).put("baseRevision", current.revision)
            .put("watchSequence", current.localSequence)
            .put("action", action).put("confirmed", true)
            .put("finishedAt", Instant.now().toString()).put("createdAt", System.currentTimeMillis())
            .put("finalSnapshot", WorkoutSnapshotStore.encode(current))
        WearHealthStore.load(this,current.workoutId)?.let { command.put("healthSummary",WearHealthAccumulator(it).snapshot()) }
        WearHealthService.stop(this)
        closure.value = WearClosureStore.close(this, current, command)
        WearCommandOutbox.enqueue(this, command)
        WorkoutSnapshotStore.clear(this)
        WearStopwatchStore.clear(this)
        workout.value = null
        refreshHealth()
        restTimer?.cancel()
        restRemaining.intValue = 0
        WearSurfaces.refresh(this,true)
    }

    private fun retryClosure() {
        val command = WearClosureStore.retry(this) ?: return
        closure.value = WearClosureStore.load(this)
        WearCommandOutbox.enqueue(this, command)
    }

    private fun trimNumber(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
