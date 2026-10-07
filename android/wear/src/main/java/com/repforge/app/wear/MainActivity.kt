package com.repforge.app.wear

import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private val TpBg = Color(0xFF0E1015)
private val TpCard = Color(0xFF1A1E25)
private val TpCard2 = Color(0xFF232933)
private val TpText = Color(0xFFF6F8FB)
private val TpMuted = Color(0xFF9AA5B6)
private val TpAccent = Color(0xFFF2BD45)

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val workout = mutableStateOf<WearWorkout?>(null)
    private val home = mutableStateOf<WatchHomeSnapshot?>(null)
    private val restRemaining = mutableIntStateOf(0)
    private var restTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workout.value = WorkoutSnapshotStore.load(this)
        home.value = WatchHomeStore.load(this)
        restoreRestFromSnapshot(workout.value)
        setContent {
            TrainPilotWearApp(
                workout = workout,
                home = home,
                restRemaining = restRemaining,
                onStart = ::startWorkoutFromWatch,
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
        restoreRestFromSnapshot(workout.value)
        Wearable.getDataClient(this).addListener(this)
        refreshFromDataLayer()
    }

    override fun onPause() {
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
                    val parsed = WorkoutSnapshotStore.save(this, raw)
                    runOnUiThread {
                        workout.value = parsed
                        restoreRestFromSnapshot(parsed)
                    }
                }
                WearDataListenerService.WATCH_HOME_PATH -> {
                    val parsed = WatchHomeStore.save(this, raw)
                    runOnUiThread { home.value = parsed }
                }
            }
        }
    }

    private fun refreshFromDataLayer() {
        Wearable.getDataClient(this).getDataItems()
            .addOnSuccessListener { items ->
                try {
                    var remoteWorkoutSeen = false
                    var newestWorkout: WearWorkout? = null
                    var newestRevision = Long.MIN_VALUE
                    var newestHome: WatchHomeSnapshot? = null
                    for (item in items) {
                        if (item == null || item.uri == null) continue
                        try {
                            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot") ?: continue
                            when (item.uri.path) {
                                WearDataListenerService.ACTIVE_WORKOUT_PATH -> {
                                    remoteWorkoutSeen = true
                                    val parsed = WorkoutSnapshotStore.parse(raw)
                                    val revision = parsed?.revision ?: Long.MIN_VALUE
                                    if (revision >= newestRevision) {
                                        newestRevision = revision
                                        newestWorkout = parsed
                                    }
                                    if (parsed != null) WorkoutSnapshotStore.save(this, raw)
                                }
                                WearDataListenerService.WATCH_HOME_PATH -> {
                                    newestHome = WatchHomeStore.save(this, raw) ?: newestHome
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore malformed stale DataItems and keep scanning.
                        }
                    }
                    runOnUiThread {
                        if (newestRevision != Long.MIN_VALUE) {
                            workout.value = newestWorkout
                            restoreRestFromSnapshot(newestWorkout)
                        } else if (remoteWorkoutSeen && workout.value == null) {
                            workout.value = null
                        }
                        if (newestHome != null) home.value = newestHome
                    }
                } finally {
                    items.release()
                }
            }
    }

    private fun startWorkoutFromWatch(day: WatchHomeDay, scheduleId: String) {
        val source = home.value ?: return
        if (source.hasDraft || day.exercises.isEmpty()) return
        val started = Instant.now().toString()
        val localWorkout = source.createWorkout(day, scheduleId, started)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, localWorkout)
        sendStartCommand(day, scheduleId, started)
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
        putCommand(commandId, command)
    }

    private fun restoreRestFromSnapshot(value: WearWorkout?) {
        val endAt = value?.restEndAt ?: 0L
        if (endAt > System.currentTimeMillis()) startRest(max(1, ((endAt - System.currentTimeMillis() + 999L) / 1000L).toInt()), true)
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
                if (vibrateAtEnd) getSystemService(Vibrator::class.java)?.vibrate(
                    VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }.start()
    }

    private fun skipRest() {
        restTimer?.cancel()
        restRemaining.intValue = 0
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
        val current = workout.value ?: return
        val exerciseIndex = current.currentExercise
        val exercise = current.exercise ?: return
        val setIndex = current.currentSetIndex
        val set = exercise.sets.getOrNull(setIndex) ?: return
        if (set.done) return
        val perSide = normalizeUnit(exercise.repUnit) == "mp/oldal"
        val valid = if (perSide) set.leftSeconds > 0 && set.rightSeconds > 0 else (set.reps.toIntOrNull() ?: 0) > 0
        if (!valid) return
        sendCommand(
            action = "completeSet",
            exerciseIndex = exerciseIndex,
            setIndex = setIndex,
            reps = set.reps.ifBlank { "0" },
            weight = set.weight,
            leftSeconds = set.leftSeconds,
            rightSeconds = set.rightSeconds
        )
        replaceSet(current, exerciseIndex, setIndex, set.copy(done = true))
        startRest(current.restSeconds, true)
    }

    private fun navigateExercise(delta: Int) {
        val current = workout.value ?: return
        val next = (current.currentExercise + delta).coerceIn(0, current.exercises.lastIndex)
        if (next == current.currentExercise) return
        workout.value = current.copy(currentExercise = next)
        sendCommand(if (delta > 0) "nextExercise" else "prevExercise")
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
        putCommand(commandId, command)
    }

    private fun putCommand(commandId: String, command: JSONObject) {
        val map = PutDataMapRequest.create("${WearDataListenerService.COMMAND_PATH_PREFIX}#commandId")
        map.dataMap.putString("command", command.toString())
        map.dataMap.putLong("createdAt", System.currentTimeMillis())
        Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent())
    }

    private fun trimNumber(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}

@Composable
private fun TrainPilotWearApp(
    workout: State<WearWorkout?>,
    home: State<WatchHomeSnapshot?>,
    restRemaining: State<Int>,
    onStart: (WatchHomeDay, String) -> Unit,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit
) {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(TpBg).padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            if (workout.value == null) {
                HomeScreen(home.value, onStart)
            } else {
                WorkoutScreen(workout.value, restRemaining.value, onChange, onComplete, onPrevious, onNext, onSkipRest)
            }
        }
    }
}

@Composable
private fun HomeScreen(home: WatchHomeSnapshot?, onStart: (WatchHomeDay, String) -> Unit) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d., EEE", Locale("hu", "HU")))
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TrainPilot", color = TpAccent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(date, color = TpMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))

        if (home == null) {
            SurfaceCard {
                Text("Szinkronizálás…", color = TpText, fontWeight = FontWeight.SemiBold)
                Text("Nyisd meg egyszer a TrainPilotot a telefonon, hogy az óra megkapja az aktív programot.", color = TpMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            return@Column
        }

        Text(home.activeProgramName, color = TpText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))

        if (home.hasDraft) {
            SurfaceCard {
                Text("Félbehagyott edzés", color = TpAccent, fontWeight = FontWeight.Bold)
                Text("A telefonon mentett edzés van. Ezt még a telefonon folytasd; órás folytatás későbbi lépés.", color = TpMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        } else {
            val rec = home.recommended
            if (rec != null) {
                SurfaceCard {
                    Text(if (rec.scheduleId.isBlank()) "Következő edzés" else "Tervezett edzés", color = TpAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(rec.day.name, color = TpText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(recommendedMeta(rec), color = TpMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(5.dp))
                    PrimaryButton("Edzés indítása") { onStart(rec.day, rec.scheduleId) }
                }
            }
        }

        if (!home.hasDraft && home.days.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Program napjai", color = TpMuted, style = MaterialTheme.typography.labelMedium)
            home.days.forEach { day ->
                Spacer(Modifier.height(5.dp))
                DayCard(day) { onStart(day, "") }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SurfaceCard(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(TpCard, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(TpAccent, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TpBg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayCard(day: WatchHomeDay, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(TpCard2, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day.name, color = TpText, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text("${day.exercises.size} gyakorlat • indítás", color = TpMuted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun recommendedMeta(rec: WatchHomeRecommendation): String {
    val count = "${rec.day.exercises.size} gyakorlat"
    if (rec.plannedStart.isBlank()) return count
    return try {
        val time = Instant.parse(rec.plannedStart).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        "$count • $time"
    } catch (_: Exception) { count }
}

@Composable
private fun WorkoutScreen(
    workout: WearWorkout?,
    restRemaining: Int,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit
) {
    if (workout == null) return
    val exercise = workout.exercise
    val set = workout.currentSet
    val unit = normalizeUnit(exercise?.repUnit.orEmpty())
    val perSide = unit == "mp/oldal"
    val timed = unit.startsWith("mp")
    val bodyweight = exercise?.loadType == "bodyweight"
    val completeEnabled = set != null && !set.done && if (perSide) {
        set.leftSeconds > 0 && set.rightSeconds > 0
    } else (set.reps.toIntOrNull() ?: 0) > 0

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(workout.programName.ifBlank { "TrainPilot" }, color = TpAccent, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        Text(exercise?.name.orEmpty(), color = TpText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(
            if (exercise == null || exercise.sets.isEmpty()) "Nincs sorozat" else "Sorozat ${workout.currentSetIndex + 1} / ${exercise.sets.size}",
            color = TpMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        if (set != null) {
            if (!bodyweight) NumericControl("Súly", formatWeight(set.weight), "−", "+", { onChange("weight", -0.5) }, { onChange("weight", 0.5) })
            if (perSide) {
                NumericControl("Bal oldal", "${set.leftSeconds} mp", "−5", "+5", { onChange("leftSeconds", -5.0) }, { onChange("leftSeconds", 5.0) })
                NumericControl("Jobb oldal", "${set.rightSeconds} mp", "−5", "+5", { onChange("rightSeconds", -5.0) }, { onChange("rightSeconds", 5.0) })
            } else {
                val step = if (timed) 5.0 else 1.0
                val label = if (timed) "Idő" else "Ismétlés"
                val value = if (timed) "${set.reps.ifBlank { "0" }} mp" else set.reps.ifBlank { "0" }
                NumericControl(label, value, if (timed) "−5" else "−", if (timed) "+5" else "+", { onChange("reps", -step) }, { onChange("reps", step) })
            }
            ActionText(if (set.done) "✓ Rögzítve" else "✓ Rögzítés", completeEnabled, onComplete)
        }
        if (restRemaining > 0) {
            Spacer(Modifier.height(5.dp))
            Text("Pihenő ${formatSeconds(restRemaining)}", color = TpAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ActionText("Pihenő kihagyása", true, onSkipRest)
        }
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionText("← Előző", workout.currentExercise > 0, onPrevious)
            ActionText("Következő →", workout.currentExercise < workout.exercises.lastIndex, onNext)
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun NumericControl(label: String, value: String, minus: String, plus: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Spacer(Modifier.height(5.dp))
    Text(label, color = TpMuted, style = MaterialTheme.typography.labelSmall)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        ActionText(minus, true, onMinus)
        Text(value, color = TpText, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ActionText(plus, true, onPlus)
    }
}

@Composable
private fun ActionText(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (enabled) TpText else TpMuted,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

private fun normalizeUnit(unit: String): String = unit.replace("\\s+".toRegex(), "").lowercase()

private fun formatWeight(raw: String): String {
    val value = raw.toDoubleOrNull() ?: 0.0
    return if (value % 1.0 == 0.0) "${value.toInt()} kg" else "$value kg"
}

private fun formatSeconds(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
