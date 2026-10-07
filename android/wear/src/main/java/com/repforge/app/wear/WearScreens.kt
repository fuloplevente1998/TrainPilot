package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

private val Black = Color.Black
private val Card = Color(0xFF1A1E25)
private val Secondary = Color(0xFF232933)
private val White = Color(0xFFF6F8FB)
private val Muted = Color(0xFF9AA5B6)
private val Gold = Color(0xFFF2BD45)
private val Danger = Color(0xFF3B232A)
private val DangerText = Color(0xFFEFBCC3)

@Composable
internal fun TrainPilotWearApp(
    workout: State<WearWorkout?>,
    home: State<WatchHomeSnapshot?>,
    closure: State<WearClosure?>,
    restRemaining: State<Int>,
    onStart: (WatchHomeDay, String) -> Unit,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onResumeWorkout: () -> Unit,
    onRetry: () -> Unit,
    onDismissClosure: () -> Unit
) {
    var route by rememberSaveable { mutableStateOf("home") }
    var editorField by rememberSaveable { mutableStateOf("reps") }
    BackHandler(enabled = route != "home") {
        route = when (route) { "editor", "finish" -> "workout"; "discard" -> "home"; else -> "home" }
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Black)) {
            val active = workout.value
            when {
                route == "summary" && closure.value != null -> SummaryScreen(closure.value!!, onRetry) {
                    onDismissClosure(); route = "home"
                }
                route == "discard" && active != null -> ConfirmationScreen(true, active.completedSets) {
                    if (it) { onDiscard(); route = "summary" } else route = "home"
                }
                route == "finish" && active != null -> ConfirmationScreen(false, active.completedSets) {
                    if (it) { onFinish(); route = "summary" } else route = "workout"
                }
                route == "editor" && active != null -> ValueEditor(active, editorField, onChange) { route = "workout" }
                route == "days" && home.value != null -> ProgramDays(home.value!!, onBack = { route = "home" }) { day ->
                    onStart(day, ""); if (workout.value != null) route = "workout"
                }
                route == "workout" && active != null -> WorkoutPager(
                    active, restRemaining.value, onChange, onComplete, onPrevious, onNext,
                    onSelectExercise, onSkipRest,
                    onEdit = { editorField = it; route = "editor" },
                    onFinish = { route = "finish" }, onHome = { route = "home" }
                )
                else -> HomeScreen(home.value, active, closure.value,
                    onStart = { day, scheduleId -> onStart(day, scheduleId); if (workout.value != null) route = "workout" },
                    onResume = { onResumeWorkout(); route = "workout" },
                    onDiscard = { route = "discard" }, onDays = { route = "days" }, onResult = { route = "summary" })
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)
}

@Composable
private fun Label(text: String, color: Color = Muted) {
    Text(text, color = color, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Title(text: String, size: Int = 19, lines: Int = 2) {
    Text(text, color = White, fontSize = size.sp, lineHeight = (size + 2).sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, tone: Color = Gold,
                 enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp)
        .background(if (enabled) tone else Secondary, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White,
            fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun HomeScreen(home: WatchHomeSnapshot?, active: WearWorkout?, closure: WearClosure?,
                       onStart: (WatchHomeDay, String) -> Unit, onResume: () -> Unit,
                       onDiscard: () -> Unit, onDays: () -> Unit, onResult: () -> Unit) {
    var elapsed by remember { mutableIntStateOf(active?.elapsedSeconds() ?: 0) }
    LaunchedEffect(active?.workoutId) { while (active != null) { elapsed = active.elapsedSeconds(); delay(1000) } }
    Panel {
        if (active != null) {
            Label("FOLYAMATBAN", Gold)
            Spacer(Modifier.height(7.dp))
            Title(active.exercise?.name ?: active.programName, 18, 1)
            Label("${active.dayName.ifBlank { active.dayId }} · ${active.currentSetIndex + 1}/${active.exercise?.sets?.size ?: 0}. sorozat")
            Text(formatSeconds(elapsed), color = White, fontSize = 31.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Pill("▶ Folytatás", onClick = onResume)
            Spacer(Modifier.height(6.dp))
            Pill("Félbehagyott edzés törlése", tone = Danger, onClick = onDiscard)
            return@Panel
        }
        if (closure?.status in listOf("pending", "error")) {
            Label("TRAINPILOT", Gold)
            Spacer(Modifier.height(10.dp))
            Title(if (closure?.status == "error") "Szinkronizálás szükséges" else "Várakozás a telefonra")
            Spacer(Modifier.height(8.dp))
            Text("Az edzés adatai az órán megmaradtak.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(13.dp))
            Pill("Mentés állapota", onClick = onResult)
            return@Panel
        }
        Label("TRAINPILOT", Gold)
        if (home == null) {
            Spacer(Modifier.height(12.dp)); Title("Szinkronizálás…")
            Text("Nyisd meg a TrainPilotot a telefonon, hogy az óra megkapja a programot.",
                color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            return@Panel
        }
        if (home.hasDraft) {
            Spacer(Modifier.height(12.dp)); Title("Félbehagyott edzés")
            Text("Nyisd meg a telefonos appot az edzés adatainak szinkronizálásához.",
                color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            return@Panel
        }
        val rec = home.recommended
        Spacer(Modifier.height(4.dp))
        Title(rec?.day?.programName ?: home.activeProgramName, 14)
        Spacer(Modifier.height(2.dp))
        if (rec != null) {
            Title(rec.day.name, 26)
            Label(recommendedMeta(rec))
            Spacer(Modifier.height(6.dp))
            Pill("▶ Edzés indítása") { onStart(rec.day, rec.scheduleId) }
        } else { Title("Válassz edzésnapot", 22); Spacer(Modifier.height(10.dp)) }
        Spacer(Modifier.height(4.dp))
        Pill("Program napjai ›", tone = Card, enabled = home.days.isNotEmpty(), onClick = onDays)
    }
}

@Composable
private fun ProgramDays(home: WatchHomeSnapshot, onBack: () -> Unit, onSelect: (WatchHomeDay) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(29.dp)); Label(home.activeProgramName, Gold)
        Title("Program napjai"); Spacer(Modifier.height(9.dp))
        home.days.forEach { day ->
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).background(Card, RoundedCornerShape(22.dp))
                .clickable(role = Role.Button) { onSelect(day) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Gold.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(day.name.substringAfterLast(" ").take(2), color = Gold, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(day.name, color = White, fontSize = 14.sp, maxLines = 2)
                    Text("${day.exercises.size} gyakorlat", color = Muted, fontSize = 11.sp)
                }
                Text("›", color = Gold)
            }
            Spacer(Modifier.height(7.dp))
        }
        Pill("‹ Kezdőlap", tone = Secondary, onClick = onBack); Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WorkoutPager(workout: WearWorkout, restRemaining: Int,
                         onChange: (String, Double) -> Unit, onComplete: () -> Unit,
                         onPrevious: () -> Unit, onNext: () -> Unit, onSelect: (Int) -> Unit,
                         onSkipRest: () -> Unit, onEdit: (String) -> Unit,
                         onFinish: () -> Unit, onHome: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        if (page == 0) {
            when {
                restRemaining > 0 -> RestScreen(workout, restRemaining, onSkipRest)
                workout.exerciseComplete -> ExerciseDoneScreen(workout, onNext, onFinish) { scope.launch { pager.animateScrollToPage(1) } }
                normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp") -> TimedSetScreen(workout, onChange, onComplete, onEdit) { scope.launch { pager.animateScrollToPage(1) } }
                else -> CurrentSetScreen(workout, onEdit, onComplete) { scope.launch { pager.animateScrollToPage(1) } }
            }
        } else WorkoutMenu(workout,
            { onPrevious(); scope.launch { pager.animateScrollToPage(0) } },
            { onNext(); scope.launch { pager.animateScrollToPage(0) } },
            { index -> onSelect(index); scope.launch { pager.animateScrollToPage(0) } }, onFinish, onHome)
    }
    Box(Modifier.fillMaxSize().padding(bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(2) { index -> Box(Modifier.width(if (pager.currentPage == index) 12.dp else 4.dp).height(4.dp)
                .background(if (pager.currentPage == index) Gold else Muted.copy(alpha = .45f), CircleShape)) }
        }
    }
}

@Composable
private fun SetHeading(workout: WearWorkout, onMenu: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Label("${workout.dayName.ifBlank { workout.dayId }} · ${workout.currentExercise + 1}/${workout.exercises.size}") }
        Box(Modifier.width(40.dp).height(28.dp).clickable(role = Role.Button, onClick = onMenu), contentAlignment = Alignment.Center) {
            Text("···", color = Muted, fontSize = 20.sp)
        }
    }
    Title(workout.exercise?.name.orEmpty(), 18)
    Label("Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}", Gold)
}

@Composable
private fun ValueTile(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(59.dp).background(Card, RoundedCornerShape(18.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = White, fontSize = 29.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Label(label)
    }
}

@Composable
private fun CurrentSetScreen(workout: WearWorkout, onEdit: (String) -> Unit, onComplete: () -> Unit, onMenu: () -> Unit) {
    val set = workout.currentSet
    Panel {
        SetHeading(workout, onMenu)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (workout.exercise?.loadType != "bodyweight")
                ValueTile(set?.weight?.ifBlank { "0" } ?: "0", "kg", Modifier.weight(1f)) { onEdit("weight") }
            ValueTile(set?.reps?.ifBlank { "0" } ?: "0", "ismétlés", Modifier.weight(1f)) { onEdit("reps") }
        }
        Spacer(Modifier.height(7.dp))
        Pill("✓ Sorozat kész", enabled = set != null && !set.done && (set.reps.toIntOrNull() ?: 0) > 0, onClick = onComplete)
    }
}

@Composable
private fun RoundControl(text: String, tone: Color = Secondary, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).background(tone, CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (tone == Gold) Black else if (tone == Danger) DangerText else if (enabled) White else Muted,
            fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ValueEditor(workout: WearWorkout, field: String, onChange: (String, Double) -> Unit, onBack: () -> Unit) {
    val set = workout.currentSet ?: return
    val original = remember(workout.workoutId, workout.currentExercise, set.number, field) { fieldValue(set, field) }
    var edited by rememberSaveable(workout.workoutId, workout.currentExercise, set.number, field) { mutableDoubleStateOf(original) }
    val step = if (field == "weight") .5 else if (field.endsWith("Seconds") || (field == "reps" && normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))) 5.0 else 1.0
    val title = when (field) { "weight" -> "Súly"; "leftSeconds" -> "Bal oldal"; "rightSeconds" -> "Jobb oldal"; else -> if (step == 5.0) "Idő" else "Ismétlés" }
    Panel {
        Label("Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}")
        Title(title, 22); Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            RoundControl("−") { edited = (edited - step).coerceAtLeast(0.0) }
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(numberText(edited), color = White, fontSize = 37.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Label(if (field == "weight") "kg" else if (step == 5.0) "mp" else "ismétlés")
            }
            RoundControl("+") { edited += step }
        }
        Spacer(Modifier.height(8.dp)); Label("Lépésköz: ${numberText(step)}")
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            RoundControl("×", Danger, onClick = onBack)
            RoundControl("✓", Gold) { onChange(field, edited - fieldValue(workout.currentSet ?: set, field)); onBack() }
        }
    }
}

@Composable
private fun RestScreen(workout: WearWorkout, seconds: Int, onSkip: () -> Unit) {
    Canvas(Modifier.fillMaxSize().padding(9.dp)) {
        drawArc(Secondary, -90f, 360f, false, style = Stroke(3.dp.toPx()))
        drawArc(Gold, -90f, 360f * (seconds.toFloat() / workout.restSeconds.coerceAtLeast(1)).coerceIn(0f, 1f), false, style = Stroke(3.dp.toPx()))
    }
    Panel {
        Label("PIHENŐ", Gold)
        Text(formatSeconds(seconds), color = White, fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold)
        Label(if (workout.exerciseComplete) "Gyakorlat kész" else "Következő: ${workout.currentSetIndex + 1}. sorozat")
        Title(workout.exercise?.name.orEmpty(), 14)
        Spacer(Modifier.height(14.dp)); Pill("Pihenő kihagyása", tone = Secondary, onClick = onSkip)
    }
}

@Composable
private fun TimedSetScreen(workout: WearWorkout, onChange: (String, Double) -> Unit, onComplete: () -> Unit,
                           onEdit: (String) -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by rememberSaveable(workout.workoutId, workout.currentExercise, set.number) {
        mutableStateOf(WearStopwatchStore.currentField(context, "${workout.workoutId}:${workout.exercise?.id}:${set.number}")
            ?.takeIf { it == "leftSeconds" || it == "rightSeconds" }
            ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds" else "leftSeconds")
    }
    val field = if (perSide) side else "reps"
    val key = "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"
    var timer by remember(key) { mutableStateOf(WearStopwatchStore.load(context, key)) }
    var elapsed by remember(key) { mutableLongStateOf(timer.elapsed()) }
    LaunchedEffect(key, timer) { while (true) { val now = timer.elapsed(); if (now / 1000 != elapsed / 1000) elapsed = now; delay(200) } }
    Panel {
        Label("Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}", Gold)
        Title(workout.exercise?.name.orEmpty(), 18, 1)
        if (perSide) Label(if (side == "leftSeconds") "Bal oldal · jobb: ${set.rightSeconds} mp" else "Jobb oldal · bal: ${set.leftSeconds} mp")
        Text(formatSeconds((elapsed / 1000).toInt()), color = White, fontSize = 36.sp,
            lineHeight = 39.sp, fontWeight = FontWeight.SemiBold)
        Label(workout.exercise?.targetReps?.let { if (it.isBlank()) "Stopper" else "Cél: $it mp" } ?: "Stopper")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundControl(if (timer.startedAt > 0) "Ⅱ" else "▶") { timer = WearStopwatchStore.save(context, timer.toggle()) }
            RoundControl(if (perSide) "B/J" else "↺") {
                if (perSide) {
                    val measured = timer.elapsed()
                    if (measured > 0) onChange(field, ceil(measured / 1000.0) - fieldValue(workout.currentSet ?: set, field))
                    timer = WearStopwatchStore.save(context, WearStopwatch(key))
                    side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                } else timer = WearStopwatchStore.save(context, WearStopwatch(key))
            }
            RoundControl("±") { timer = WearStopwatchStore.save(context, timer.copy(elapsedMillis = timer.elapsed(), startedAt = 0L)); onEdit(field) }
        }
        Spacer(Modifier.height(6.dp))
        Pill(if (perSide) "✓ ${if (side == "leftSeconds") "Bal" else "Jobb"} idő rögzítése" else "✓ Idő rögzítése",
            enabled = timer.startedAt > 0 || timer.elapsedMillis > 0 || elapsed > 0 || fieldValue(set, field) > 0) {
            val measured = timer.elapsed()
            val seconds = if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
            onChange(field, seconds - fieldValue(workout.currentSet ?: set, field))
            timer = WearStopwatchStore.save(context, WearStopwatch(key))
            if (!perSide || (side == "leftSeconds" && set.rightSeconds > 0) || (side == "rightSeconds" && set.leftSeconds > 0)) onComplete()
            else side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
        }
    }
}

@Composable
private fun ExerciseDoneScreen(workout: WearWorkout, onNext: () -> Unit, onFinish: () -> Unit, onMenu: () -> Unit) {
    Panel {
        Label("${workout.dayName.ifBlank { workout.dayId }} · ${workout.currentExercise + 1}/${workout.exercises.size}", Gold)
        Spacer(Modifier.height(10.dp))
        Title(if (workout.currentExercise == workout.exercises.lastIndex) "Az utolsó gyakorlat kész" else "Gyakorlat kész", 23)
        Label("${workout.completedSets} sorozat rögzítve")
        Spacer(Modifier.height(16.dp))
        Pill(if (workout.currentExercise == workout.exercises.lastIndex) "✓ Befejezés" else "Következő gyakorlat ›") {
            if (workout.currentExercise == workout.exercises.lastIndex) onFinish() else onNext()
        }
        Spacer(Modifier.height(7.dp)); Pill("Gyakorlatok ›", tone = Secondary, onClick = onMenu)
    }
}

@Composable
private fun WorkoutMenu(workout: WearWorkout, onPrevious: () -> Unit, onNext: () -> Unit,
                        onSelect: (Int) -> Unit, onFinish: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(27.dp)); Title("Edzésmenü"); Spacer(Modifier.height(9.dp))
        Pill("‹ Kezdőlap", tone = Secondary, onClick = onHome)
        Spacer(Modifier.height(7.dp)); Pill("Edzés befejezése", tone = Danger, enabled = workout.completedSets > 0, onClick = onFinish)
        if (workout.completedSets == 0) Label("Előbb rögzíts egy sorozatot")
        Spacer(Modifier.height(10.dp)); Label("GYAKORLATOK", Gold)
        workout.exercises.forEachIndexed { index, exercise ->
            Spacer(Modifier.height(5.dp))
            Column(Modifier.fillMaxWidth().background(if (index == workout.currentExercise) Secondary else Card, RoundedCornerShape(20.dp))
                .clickable(role = Role.Button) { onSelect(index) }.padding(12.dp)) {
                Text(exercise.name, color = White, fontSize = 13.sp, maxLines = 2)
                Text("${exercise.sets.count { it.done }}/${exercise.sets.size} sorozat", color = if (index == workout.currentExercise) Gold else Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.weight(1f)) { Pill("‹ Előző", tone = Secondary, enabled = workout.currentExercise > 0, onClick = onPrevious) }
            Box(Modifier.weight(1f)) { Pill("Következő ›", tone = Secondary, enabled = workout.currentExercise < workout.exercises.lastIndex, onClick = onNext) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ConfirmationScreen(discard: Boolean, completed: Int, onAnswer: (Boolean) -> Unit) {
    Panel {
        Label(if (discard) "FÉLBEHAGYOTT EDZÉS" else "EDZÉS BEFEJEZÉSE", Gold)
        Spacer(Modifier.height(5.dp)); Title(if (discard) "Törlöd ezt az edzést?" else "Befejezed az edzést?", 19)
        Spacer(Modifier.height(4.dp))
        Text(if (discard) "A félbehagyott edzés elvész.\nA napló megmarad." else "$completed sorozat bekerül a naplóba.\nRészleges edzés is menthető.",
            color = Muted, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Pill(if (discard) "Edzés törlése" else "✓ Befejezés", tone = if (discard) Danger else Gold) { onAnswer(true) }
        Spacer(Modifier.height(6.dp)); Pill("Mégse", tone = Secondary) { onAnswer(false) }
    }
}

@Composable
private fun SummaryScreen(result: WearClosure, onRetry: () -> Unit, onDone: () -> Unit) {
    Panel {
        val saved = result.status == "saved" || result.status == "discarded"
        Label(if (saved) "✓ TRAINPILOT" else if (result.status == "error") "SZINKRONIZÁLÁS" else "VÁRAKOZÁS A TELEFONRA", Gold)
        Spacer(Modifier.height(7.dp))
        Title(if (result.status == "error") "Még nem menthető" else if (result.action == "discardWorkout") "Edzés törlése" else "Edzés kész", 23)
        if (result.status == "error") {
            Text(result.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp)); Pill("Újrapróbálás", onClick = onRetry)
        } else if (result.action == "finishWorkout") {
            Text(formatSeconds(result.durationSeconds), color = White, fontSize = 33.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(19.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Title("${result.exerciseCount}", 22); Label("gyakorlat") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Title("${result.completedSets}", 22); Label("sorozat") }
            }
            Label(if (saved) "Mentve a telefonos naplóba" else "Helyben megőrizve · mentés várakozik")
        } else Label(if (saved) "A félbehagyott edzés törölve" else "A telefonos törlés még várakozik")
        Spacer(Modifier.height(10.dp)); Pill(if (saved) "Kész" else "Kezdőlap", onClick = onDone)
    }
}

private fun recommendedMeta(rec: WatchHomeRecommendation): String {
    val count = "${rec.day.exercises.size} gyakorlat"
    return try { if (rec.plannedStart.isBlank()) count else "$count · ${Instant.parse(rec.plannedStart).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}" } catch (_: Exception) { count }
}
internal fun normalizeUnit(unit: String): String = unit.replace("\\s+".toRegex(), "").lowercase()
internal fun formatSeconds(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
private fun numberText(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')
private fun fieldValue(set: WearSet, field: String): Double = when (field) {
    "weight" -> set.weight.toDoubleOrNull() ?: 0.0
    "leftSeconds" -> set.leftSeconds.toDouble()
    "rightSeconds" -> set.rightSeconds.toDouble()
    else -> set.reps.toDoubleOrNull() ?: 0.0
}
