package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import org.json.JSONObject

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
    onStartQuick: (WatchHomeExercise) -> Unit,
    onAddQuick: (WatchHomeExercise) -> Unit,
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
    onDismissClosure: () -> Unit,
    onOpenPhone: () -> Unit,
    onRefreshJournal: () -> Unit,
    health: State<JSONObject?>,
    measuring: State<Boolean>,
    gpsEnabled: State<Boolean>,
    requestedRoute: State<String?>,
    onRouteConsumed: () -> Unit,
    onEnableHealth: () -> Unit,
    onDisableHealth: () -> Unit,
    onToggleGps: () -> Unit
) {
    var route by rememberSaveable { mutableStateOf("home") }
    LaunchedEffect(route) {
        if (route == "journal") onRefreshJournal()
    }
    LaunchedEffect(requestedRoute.value) {
        requestedRoute.value?.let { route=it;onRouteConsumed() }
    }
    var editorField by rememberSaveable { mutableStateOf("reps") }
    var miniJournalId by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = route != "home") {
        route = when (route) {
            "editor", "timer", "finish" -> "workout"
            "journal-detail" -> "journal"
            "journal" -> "menu"
            "discard" -> "home"
            else -> "home"
        }
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Black)) {
            val active = workout.value
            when {
                route == "metrics" -> HealthScreen(health.value,measuring.value,gpsEnabled.value,onEnableHealth,onDisableHealth,onToggleGps) { route="menu" }
                route == "menu" -> MainMenuScreen(active != null, active?.quickWorkout == true, onOpenPhone) { route = it }
                route == "journal" -> MiniJournalScreen(home.value,
                    onRefresh = onRefreshJournal, onOpenPhone = onOpenPhone,
                    onBack = { route = "menu" }) { selected ->
                    miniJournalId = selected.workoutId
                    route = "journal-detail"
                }
                route == "journal-detail" -> MiniJournalDetailScreen(
                    home.value?.recentWorkouts?.firstOrNull { it.workoutId == miniJournalId },
                    onBack = { route = "journal" })
                route == "quick" -> QuickWorkoutPicker(
                    home.value, active, active == null && home.value?.hasDraft == false &&
                        closure.value?.status !in listOf("pending", "error"),
                    onBack = { route = "menu" }) { exercise ->
                    if (active?.quickWorkout == true) onAddQuick(exercise)
                    else if (active == null) onStartQuick(exercise)
                    if (workout.value?.quickWorkout == true) route = "workout"
                }
                route == "calendar" -> CalendarScreen(home.value, active == null && home.value?.hasDraft == false && closure.value?.status !in listOf("pending", "error"),
                    onBack = { route = "menu" }) { entry ->
                    entry.day?.let { onStart(it, entry.scheduleId); if (workout.value != null) route = "workout" }
                }
                route == "summary" && closure.value != null -> SummaryScreen(closure.value!!, health.value, onRetry) {
                    onDismissClosure(); route = "home"
                }
                route == "discard" && active != null -> ConfirmationScreen(true, active.completedSets) {
                    if (it) { onDiscard(); route = "summary" } else route = "home"
                }
                route == "finish" && active != null -> ConfirmationScreen(false, active.completedSets) {
                    if (it) { onFinish(); route = "summary" } else route = "workout"
                }
                route == "editor" && active != null -> ValueEditor(active, editorField, onChange) { route = "workout" }
                route == "timer" && active != null -> TimedSetToolsScreen(active, onChange, onComplete,
                    onEdit = { editorField = it; route = "editor" }, onBack = { route = "workout" })
                route == "days" && home.value != null -> ProgramDays(home.value!!, onBack = { route = "home" }) { day ->
                    onStart(day, ""); if (workout.value != null) route = "workout"
                }
                route == "workout" && active != null -> WorkoutPager(
                    active, restRemaining.value, health.value, measuring.value, gpsEnabled.value, onEnableHealth, onDisableHealth, onToggleGps, onChange, onComplete, onPrevious, onNext,
                    onSelectExercise, onSkipRest,
                    onEdit = { editorField = it; route = "editor" },
                    onTimerTools = { route = "timer" },
                    onFinish = { route = "finish" }, onHome = { route = "home" }
                )
                else -> HomeScreen(home.value, active, closure.value,
                    onStart = { day, scheduleId -> onStart(day, scheduleId); if (workout.value != null) route = "workout" },
                    onResume = { onResumeWorkout(); route = "workout" },
                    onDiscard = { route = "discard" }, onDays = { route = "days" }, onResult = { route = "summary" },
                    onMenu = { route = "menu" }, onCalendar = { route = "calendar" }, onOpenPhone = onOpenPhone)
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Keep the first/last controls away from the circular rim. Larger
        // system fonts can scroll instead of pushing controls off the display.
        Column(Modifier.align(Alignment.Center).width((maxWidth - 44.dp).coerceAtLeast(100.dp))
            .heightIn(max = (maxHeight - 64.dp).coerceAtLeast(80.dp)).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun ActionPanel(count: Int = 3, gap: Float = 7f, horizontalPadding: Int = 44,
                        normalBodyHeight: Int = 103,
                        actions: @Composable (WearActionLayout) -> Unit,
                        content: @Composable ColumnScope.(Boolean) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearActionLayout(maxWidth.value, maxHeight.value, count, gap)
        val compact = layout.bodyHeight < normalBodyHeight * fontScale
        Column(Modifier.align(Alignment.TopCenter).offset(y = layout.bodyTop.dp)
            .width((minOf(maxWidth, maxHeight) - horizontalPadding.dp).coerceAtLeast(80.dp))
            .height(layout.bodyHeight.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            content(compact)
        }
        // The footer is a sibling of the scrolling content, not its last item.
        Box(Modifier.align(Alignment.TopCenter).offset(y = layout.footerTop.dp)
            .width(layout.footerWidth.dp).height(layout.buttonSize.dp), contentAlignment = Alignment.Center) {
            actions(layout)
        }
    }
}

@Composable
private fun Label(text: String, color: Color = Muted, size: Int = 11) {
    Text(text, color = color, fontSize = size.sp, lineHeight = (size + 2).sp, textAlign = TextAlign.Center,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Title(text: String, size: Int = 19, lines: Int = 2) {
    Text(text, color = White, fontSize = size.sp, lineHeight = (size + 2).sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, tone: Color = Gold,
                 enabled: Boolean = true, height: Int = 48, size: Int = 14, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = height.dp)
        .background(if (enabled) tone else Secondary, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White,
            fontSize = size.sp, lineHeight = (size + 2).sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun HomeScreen(home: WatchHomeSnapshot?, active: WearWorkout?, closure: WearClosure?,
                       onStart: (WatchHomeDay, String) -> Unit, onResume: () -> Unit,
                       onDiscard: () -> Unit, onDays: () -> Unit, onResult: () -> Unit,
                       onMenu: () -> Unit, onCalendar: () -> Unit, onOpenPhone: () -> Unit) {
    var elapsed by remember { mutableIntStateOf(active?.elapsedSeconds() ?: 0) }
    LaunchedEffect(active?.workoutId) { while (active != null) { elapsed = active.elapsedSeconds(); delay(1000) } }
    val waiting = closure?.status in listOf("pending", "error")
    val rec = home?.recommended
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearHomeLayout(maxWidth.value, maxHeight.value)
        val compact = layout.bodyHeight < 85f * fontScale
        Box(Modifier.align(Alignment.TopCenter).offset(y = layout.phoneTop.dp)
            .width(layout.phone.width.dp)) {
            Pill("Telefon", modifier = Modifier.semantics { contentDescription = "Megnyitás telefonon" },
                tone = Secondary, height = layout.phone.height.toInt(), size = 11, onClick = onOpenPhone)
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = layout.bodyTop.dp)
            .width((minOf(maxWidth, maxHeight) - 44.dp).coerceAtLeast(80.dp))
            .height(layout.bodyHeight.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when {
                active != null -> {
                    Box(Modifier.fillMaxWidth(.9f)) { Title(active.exercise?.name ?: active.programName, if (compact) 13 else 16) }
                    Label("${active.dayName.ifBlank { active.dayId }} · ${active.currentSetIndex + 1}/${active.exercise?.sets?.size ?: 0}. sorozat", size = 9)
                    Text(formatSeconds(elapsed), color = White, fontSize = (if (compact) 24 else 30).sp, fontWeight = FontWeight.SemiBold)
                }
                waiting -> {
                    Title(if (closure?.status == "error") "Szinkronizálás szükséges" else "Várakozás a telefonra", if (compact) 14 else 18)
                    Text("Az edzés az órán megmaradt.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                home == null -> {
                    Title("Szinkronizálás…", if (compact) 14 else 18)
                    Text("Nyisd meg a TrainPilotot a telefonon.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                home.hasDraft -> {
                    Title("Félbehagyott edzés", if (compact) 14 else 18)
                    Text("Az edzés adatainak szinkronja a telefonra vár.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                else -> {
                    Box(Modifier.fillMaxWidth(.88f)) { Title(rec?.day?.programName ?: home.activeProgramName, if (compact) 13 else 15, lines = 1) }
                    Spacer(Modifier.height(2.dp))
                    Title(rec?.day?.name ?: "Válassz edzésnapot", if (compact) 23 else 30)
                    if (rec != null) Label(recommendedMeta(rec), size = if (compact) 9 else 11)
                }
            }
        }
        CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
            Box(Modifier.align(Alignment.TopCenter).offset(x = -layout.sideOffset.dp, y = layout.sideTop.dp)) {
                when {
                    active != null -> RoundControl("▶", Gold, caption = "Folytatás", description = "Edzés folytatása", onClick = onResume)
                    waiting -> RoundControl("↻", Gold, caption = "Állapot", description = "Edzés mentésének állapota", onClick = onResult)
                    rec != null && home?.hasDraft == false -> RoundControl("▶", Gold, caption = "Indítás", description = "Edzés indítása") { onStart(rec.day, rec.scheduleId) }
                    else -> RoundControl("A/B", Gold, enabled = home != null && home.hasDraft == false && home.days.isNotEmpty(), caption = "Napok", description = "Program napjai", onClick = onDays)
                }
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = layout.centerTop.dp)) {
                if (active != null) RoundControl("✕", Danger, caption = "Törlés", description = "Megkezdett edzés törlése", onClick = onDiscard)
                else RoundControl("▦", caption = "Naptár", description = "Edzésnaptár", onClick = onCalendar)
            }
            Box(Modifier.align(Alignment.TopCenter).offset(x = layout.sideOffset.dp, y = layout.sideTop.dp)) {
                RoundControl("···", caption = "Menü", description = "Főmenü", onClick = onMenu)
            }
        }
    }
}

@Composable
private fun RoundList(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = maxWidth * .1f, end = maxWidth * .1f, top = maxHeight * .17f, bottom = maxHeight * .2f),
            horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun MainMenuScreen(hasWorkout: Boolean, quickWorkout: Boolean, onOpenPhone: () -> Unit, onRoute: (String) -> Unit) {
    RoundList {
        Label("TRAINPILOT", Gold); Title("Főmenü", 22); Spacer(Modifier.height(8.dp))
        for ((label, destination) in listOf("Kezdőlap" to "home", "Mini napló" to "journal", "Naptár" to "calendar", "Programnapok" to "days", "Órás mérések" to "metrics")) {
            Pill(label, tone = Card) { onRoute(destination) }; Spacer(Modifier.height(6.dp))
        }
        if (!hasWorkout || quickWorkout) {
            Pill(if (quickWorkout) "+ Gyakorlat" else "Gyors edzés", tone = Gold) { onRoute("quick") }
            Spacer(Modifier.height(6.dp))
        }
        if (hasWorkout) { Pill("Edzés folytatása") { onRoute("workout") }; Spacer(Modifier.height(6.dp)); Pill("Edzés törlése", tone = Danger) { onRoute("discard") }; Spacer(Modifier.height(6.dp)) }
        Pill("Megnyitás telefonon", tone = Card, onClick = onOpenPhone)
    }
}



private fun miniJournalDate(value: String): String = try {
    DateTimeFormatter.ofPattern("MM. dd. HH:mm").withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
} catch (_: Exception) { "—" }

private fun miniNumber(value: Double?, decimals: Int = 0): String? {
    if (value == null || !value.isFinite()) return null
    return if (decimals == 0) kotlin.math.round(value).toInt().toString()
        else String.format(java.util.Locale.forLanguageTag("hu-HU"), "%.${decimals}f", value)
}

@Composable
private fun MiniJournalScreen(home: WatchHomeSnapshot?, onRefresh: () -> Unit,
                              onOpenPhone: () -> Unit, onBack: () -> Unit,
                              onSelect: (MiniJournalWorkout) -> Unit) {
    val items = home?.recentWorkouts.orEmpty()
    RoundList {
        Label("TRAINPILOT", Gold)
        Title("Mini napló", 21)
        Spacer(Modifier.height(8.dp))
        Label("Mentett telefonos edzések · offline másolat", size = 10)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            val message = when {
                home == null -> "Várakozás a telefonos napló szinkronizálására."
                !home.journalAvailable -> "A telefon legutóbbi adatcsomagja még nem tartalmaz Mini naplót. Frissítsd és nyisd meg a telefonos TrainPilotot."
                else -> "A telefonos naplóban még nincs átadható, befejezett edzés."
            }
            Text(message,
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        if (home?.phoneVersion?.isNotBlank() == true) {
            Label("Telefon: ${home.phoneVersion}", size = 10)
            Spacer(Modifier.height(5.dp))
        }
        items.forEach { item ->
            Pill("${miniJournalDate(item.started)} • ${item.dayName}", tone = Card, size = 12,
                height = 47) { onSelect(item) }
            Spacer(Modifier.height(7.dp))
        }
        Spacer(Modifier.height(5.dp))
        Pill("Frissítés", tone = Secondary, onClick = onRefresh)
        Spacer(Modifier.height(5.dp))
        Pill("Telefon megnyitása", tone = Secondary, onClick = onOpenPhone)
        Spacer(Modifier.height(5.dp))
        Pill("‹ Főmenü", tone = Secondary, onClick = onBack)
    }
}

@Composable
private fun MiniJournalMetrics(title: String, metrics: MiniJournalMetric, watch: Boolean) {
    Label(title, Gold, 12)
    val avg = miniNumber(metrics.averageHeartRate, 1)
    val max = miniNumber(metrics.maxHeartRate)
    if (avg != null || max != null) {
        Text("Pulzus: ${avg ?: "—"} / ${max ?: "—"} bpm", color = White,
            fontSize = 12.sp, textAlign = TextAlign.Center)
    }
    if (watch) {
        miniNumber(metrics.totalCalories)?.let { value ->
            Text("Összes mért energia: $value kcal", color = White, fontSize = 12.sp,
                textAlign = TextAlign.Center)
        }
        miniNumber(metrics.activeDurationSeconds)?.let { value ->
            Label("Órás mérési idő: $value s", size = 11)
        }
    } else {
        miniNumber(metrics.workoutCalories)?.let { value ->
            Text("Edzéskalória: $value kcal", color = White, fontSize = 12.sp,
                textAlign = TextAlign.Center)
        }
        miniNumber(metrics.totalCalories)?.let { value ->
            Label("Ablak összenergiája: $value kcal", size = 11)
        }
        miniNumber(metrics.activeCalories)?.let { value ->
            Label("Aktív energia: $value kcal", size = 11)
        }
        miniNumber(metrics.exerciseMinutes)?.let { value ->
            Label("Egészségapp edzésideje: $value perc", size = 11)
        }
    }
    miniNumber(metrics.steps)?.let { Label("Lépések: $it", size = 11) }
    if (metrics.partial) Label("Részleges órás mérés", Gold, 10)
}

@Composable
private fun MiniJournalDetailScreen(item: MiniJournalWorkout?, onBack: () -> Unit) {
    RoundList {
        Label("TRAINPILOT", Gold)
        Title("Edzésnapló", 20)
        Spacer(Modifier.height(7.dp))
        if (item == null) {
            Text("Ez az edzés már nincs a szinkronizált listában.", color = Muted,
                fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Title(item.dayName, 16)
            Label(miniJournalDate(item.started), size = 11)
            Label("${item.durationSeconds / 60} perc · ${item.exercises.size} gyakorlat", size = 11)
            Spacer(Modifier.height(10.dp))
            if (item.wear != null) {
                MiniJournalMetrics("TrainPilot óra · Health Services", item.wear, true)
                Spacer(Modifier.height(7.dp))
            } else Label("Nem érkezett órás mérési összesítés", size = 11)
            if (item.healthConnect != null) {
                MiniJournalMetrics(
                    if (item.healthConnect.source == "samsung_health") "Samsung Health"
                    else "Health Connect", item.healthConnect, false)
                Spacer(Modifier.height(5.dp))
                Label("A két mérés nem adódik össze", size = 10)
            }
            Spacer(Modifier.height(7.dp))
            Label("Elvégzett gyakorlatok", Gold, 12)
            if (item.exercises.isEmpty()) {
                Label("Nincs rögzített sorozat", size = 11)
            }
            item.exercises.forEach { exercise ->
                Spacer(Modifier.height(6.dp))
                Text(exercise.name, color = White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                exercise.sets.forEach { set ->
                    val details = buildList {
                        if (set.reps.isNotBlank()) add("${set.reps} ism.")
                        if (set.weight != null && set.weight > 0.0) add("${miniNumber(set.weight, 1)} kg")
                        if (set.leftSeconds != null && set.rightSeconds != null &&
                            set.leftSeconds > 0.0 && set.rightSeconds > 0.0)
                            add("${miniNumber(set.leftSeconds)} / ${miniNumber(set.rightSeconds)} s")
                        if (set.distanceMeters != null && set.distanceMeters > 0.0)
                            add("${miniNumber(set.distanceMeters)} m")
                    }
                    Label("${set.number}. sorozat • ${details.joinToString(" · ").ifBlank { "Kész" }}",
                        size = 11)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Pill("‹ Napló", tone = Secondary, onClick = onBack)
    }
}

@Composable
private fun QuickWorkoutPicker(home: WatchHomeSnapshot?, active: WearWorkout?, canStart: Boolean,
                               onBack: () -> Unit, onSelect: (WatchHomeExercise) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val adding = active?.quickWorkout == true
    val catalog = home?.quickExercises.orEmpty()
    val matches = remember(catalog, query, active?.exercises) {
        val needle = query.trim()
        catalog.filter { item ->
            (needle.isBlank() || item.name.contains(needle, ignoreCase = true) ||
                item.id.contains(needle, ignoreCase = true)) &&
                (!adding || active?.exercises?.none { it.id == item.id } == true)
        }
    }
    val pageCount = ((matches.size + 11) / 12).coerceAtLeast(1)
    val selectedPage = page.coerceIn(0, pageCount - 1)
    RoundList {
        Label("TRAINPILOT", Gold)
        Title(if (adding) "+ Gyakorlat" else "Gyors edzés", 20)
        Spacer(Modifier.height(8.dp))
        if (active != null && !adding) {
            Text("Előbb zárd le a jelenlegi edzést.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else if (!adding && !canStart) {
            Text("A telefonos edzésvázlat vagy a függő szinkron lezárása után indítható.",
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else if (catalog.isEmpty()) {
            Text("A gyakorlatok szinkronizálásához nyisd meg a TrainPilotot a telefonon.",
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            BasicTextField(
                value = query, onValueChange = { query = it.take(60); page = 0 },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth().background(Secondary, RoundedCornerShape(14.dp)).padding(10.dp),
                decorationBox = { content ->
                    Box {
                        if (query.isEmpty()) Text("Gyakorlat keresése…", color = Muted, fontSize = 13.sp)
                        content()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) Text("Nincs további választható gyakorlat.", color = Muted,
                fontSize = 12.sp, textAlign = TextAlign.Center)
            matches.drop(selectedPage * 12).take(12).forEach { exercise ->
                Pill(exercise.name, tone = Card, height = 48, size = 12) { onSelect(exercise) }
                Spacer(Modifier.height(5.dp))
            }
            if (pageCount > 1) {
                Label("${selectedPage + 1} / $pageCount")
                if (selectedPage > 0) Pill("‹ Előző", tone = Secondary) { page = selectedPage - 1 }
                if (selectedPage + 1 < pageCount) Pill("További gyakorlatok ›", tone = Secondary) { page = selectedPage + 1 }
            }
        }
        Spacer(Modifier.height(6.dp))
        Pill("‹ Vissza", tone = Secondary, onClick = onBack)
    }
}

@Composable
private fun CalendarScreen(home: WatchHomeSnapshot?, canStart: Boolean, onBack: () -> Unit, onStart: (WatchCalendarEntry) -> Unit) {
    val today = LocalDate.now()
    val entries = home?.calendar.orEmpty().filter { wearCalendarDate(it) != null }
    val weeks = wearCalendarWeeks(entries, today)
    var weekKey by rememberSaveable { mutableStateOf(wearCalendarMonday(today).toString()) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val week = runCatching { LocalDate.parse(weekKey) }.getOrDefault(wearCalendarMonday(today))
    BackHandler(enabled = selectedKey != null) { selectedKey = null }
    selectedKey?.let { key ->
        val selected = LocalDate.parse(key)
        CalendarDayScreen(selected, entries.filter { wearCalendarDate(it) == selected }, today, canStart,
            onBack = { selectedKey = null }, onStart = onStart)
        return
    }
    if (home == null || entries.isEmpty()) {
        RoundList {
            Title("Naptár", 22)
            Text("A naptár frissítéséhez nyisd meg a TrainPilotot a telefonon.", color = Muted,
                fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp)); Pill("‹ Főmenü", tone = Secondary, onClick = onBack)
        }
        return
    }
    // Native paging consumes a horizontal drag before it can select a day.
    // Keep only cached weeks, as requested; no unknown future/history pages.
    key(weeks) {
        val pager = rememberPagerState(initialPage = weeks.indexOf(week).takeIf { it >= 0 }
            ?: weeks.indices.minByOrNull { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(weeks[it],today)) }
            ?: 0, pageCount = { weeks.size })
        LaunchedEffect(pager.settledPage) { weekKey = weeks[pager.settledPage].toString() }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { weeks[it].toString() }) { page ->
            CalendarWeekPage(weeks[page], entries, today, page + 1, weeks.size, onBack) {
                selectedKey = it.toString()
            }
        }
    }
}

@Composable
private fun CalendarWeekPage(week: LocalDate, entries: List<WatchCalendarEntry>, today: LocalDate,
                             pageNumber: Int, pageCount: Int, onBack: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.25f)
    fun textSize(size: Float) = (size * captionScale / fontScale).sp
    val locale = java.util.Locale.forLanguageTag("hu")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearCalendarLayout(maxWidth.value, maxHeight.value)
        val caption = wearCalendarCaption(week, today)
        Text(caption.month, modifier = Modifier.align(Alignment.TopCenter).offset(y = layout.monthTop.dp)
            .width((minOf(maxWidth, maxHeight) * .76f)),
            color = White, fontSize = textSize(if (minOf(maxWidth, maxHeight).value < 180f) 16f else 20f),
            fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        Text(caption.dates, color = Muted, fontSize = textSize(10f), textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = layout.weekTop.dp)
                .width(minOf(maxWidth, maxHeight) * .76f)
                .semantics { contentDescription = "${caption.dates}. Átvett hét: $pageNumber / $pageCount. Hétváltás balra vagy jobbra csúsztatással." }, maxLines = 1)
        val dayScale = minOf(1f, layout.buttonSize / 40f)
        val labels = listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
        layout.days.forEachIndexed { dayIndex, point ->
            val date = week.plusDays(dayIndex.toLong())
            val records = entries.filter { wearCalendarDate(it) == date }
            val current = date == today
            val color = if (current) Black else if (records.isEmpty()) Muted else if (dayIndex == 6) DangerText else White
            Column(Modifier.align(Alignment.TopCenter).offset(x = point.x.dp, y = (point.y - layout.buttonSize / 2f).dp)
                .size(layout.buttonSize.dp).background(if (current) Gold else if (records.isEmpty()) Card else Secondary, CircleShape)
                .clickable(role = Role.Button) { onSelect(date) }
                .semantics { contentDescription = date.format(DateTimeFormatter.ofPattern("MMMM d., EEEE", locale)) +
                    if (current) ", ma" else "" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(labels[dayIndex], color = color, fontSize = textSize(9f * dayScale), lineHeight = textSize(10f * dayScale), maxLines = 1)
                Text("${date.dayOfMonth}", color = color, fontSize = textSize(17f * dayScale), lineHeight = textSize(19f * dayScale),
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            val status = records.map { it.status }
            val marker = when {
                "planned" in status -> Gold
                "completed" in status -> Color(0xFF7FD4BA)
                "skipped" in status -> Muted
                else -> null
            }
            if (marker != null) Box(Modifier.align(Alignment.TopCenter)
                .offset(x = point.x.dp, y = (point.y + layout.buttonSize / 2f + 2f).dp)
                .size(4.dp).background(marker, CircleShape))
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = layout.menuTop.dp)
            .width(56.dp).height(20.dp).clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = "Vissza a főmenübe" }, contentAlignment = Alignment.Center) {
            Text("Menü", color = Muted, fontSize = textSize(9f), maxLines = 1)
        }
    }
}

@Composable
private fun CalendarDayScreen(date: LocalDate, entries: List<WatchCalendarEntry>, today: LocalDate,
                              canStart: Boolean, onBack: () -> Unit, onStart: (WatchCalendarEntry) -> Unit) {
    RoundList {
        Title(date.format(DateTimeFormatter.ofPattern("MMM d., EEEE", java.util.Locale.forLanguageTag("hu"))), 18)
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text("Ehhez a naphoz nincs adat az órán. A telefonos naptárban találod meg.", color = Muted,
                fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        entries.forEach { entry ->
            Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(18.dp)).padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                entry.day?.let { day ->
                    Label(day.programName); Title(day.name, 16); Label("${day.exercises.size} gyakorlat", size = 10)
                }
                Label(when (entry.status) { "completed" -> "✓ Teljesítve"; "skipped" -> "Kihagyva"; "rest" -> "Pihenőnap"; else -> "Tervezett" }, size = 11)
                val time = runCatching { Instant.parse(entry.plannedStart).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
                if (time != null) Label(time, size = 10)
                if (wearCalendarCanStart(entry, today, canStart)) {
                    Spacer(Modifier.height(6.dp)); Pill("▶ Edzés indítása") { onStart(entry) }
                } else if (!canStart && entry.status == "planned" && entry.day != null && !date.isBefore(today)) {
                    Text("Az aktív edzés vagy a függő szinkron lezárása után indítható.", color = Muted, fontSize = 10.sp,
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Pill("‹ Heti naptár", tone = Secondary, onClick = onBack)
    }
}

@Composable
private fun HealthScreen(data: JSONObject?, enabled: Boolean, gps: Boolean,
                         onEnable: () -> Unit, onDisable: () -> Unit, onGps: () -> Unit, onBack: () -> Unit) {
    fun value(key: String, unit: String, divisor: Double=1.0): String {
        val number=data?.takeIf { it.has(key) && !it.isNull(key) }?.optDouble(key)
        return if(number==null || !number.isFinite()) "—" else "${trimWearNumber(number/divisor)} $unit"
    }
    RoundList {
        Label("TRAINPILOT",Gold);Title("Órás mérések",20);Spacer(Modifier.height(8.dp))
        Text(wearRecordingStatus(data,enabled),color=Gold,fontSize=11.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        val fresh=data?.optString("state")=="active" && System.currentTimeMillis()-(data.optLong("lastSampleAt",data.optLong("updatedAt")))<15000
        Label(if(fresh)"Élő pulzus" else "Pulzus · utolsó mérés",size=10)
        Title(value("heartRate","bpm"),28)
        for((title,metric,unit) in listOf(Triple("Átlagpulzus","averageHeartRate","bpm"),Triple("Max. pulzus","maxHeartRate","bpm"),
            Triple("Összes energia","totalCalories","kcal"),Triple("Lépések","steps","lépés"),Triple("Aktív mérési idő","activeDurationSeconds","s"),Triple("Távolság","distanceMeters","m"),Triple("Sebesség","speedMps","m/s"))) {
            Label(title,size=10);Title(value(metric,unit),17);Spacer(Modifier.height(5.dp))
        }
        val speed=data?.optDouble("speedMps",0.0) ?: 0.0
        if(speed>0 && speed.isFinite()) { Label("Aktuális tempó");Title("${formatSeconds((1000/speed).toInt())} /km",17) }
        Text("A hiányzó adatot az óra még nem adta át. Az összes kcal az alapanyagcserét is tartalmazza.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
        if(data?.optBoolean("partial")==true)Text("Részleges mérés: a rögzítés edzés közben indult újra.",color=Gold,fontSize=11.sp,textAlign=TextAlign.Center)
        data?.optString("message")?.takeIf { it.isNotBlank() }?.let { Text(it,color=Gold,fontSize=11.sp,textAlign=TextAlign.Center) }
        Spacer(Modifier.height(8.dp))
        Pill(if(enabled)"Mérés kikapcsolása" else "Mérések engedélyezése",tone=if(enabled)Secondary else Gold,onClick=if(enabled)onDisable else onEnable)
        Spacer(Modifier.height(6.dp));Pill(if(gps)"GPS kikapcsolása" else "GPS engedélyezése",tone=Card,onClick=onGps)
        Text("GPS csak mozgásos edzésnél használható, a következő mérés indításától. Az óra az engedélyezett edzésmérést a háttérben is folytatja.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(8.dp));Pill("‹ Vissza",tone=Secondary,onClick=onBack)
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
                         health: JSONObject?, measuring: Boolean, gps: Boolean,
                         onEnable: ()->Unit, onDisable: ()->Unit, onGps: ()->Unit,
                         onChange: (String, Double) -> Unit, onComplete: () -> Unit,
                         onPrevious: () -> Unit, onNext: () -> Unit, onSelect: (Int) -> Unit,
                         onSkipRest: () -> Unit, onEdit: (String) -> Unit, onTimerTools: () -> Unit,
                         onFinish: () -> Unit, onHome: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                Box(Modifier.fillMaxSize()) {
                    if (restRemaining > 0) RestRing(workout, restRemaining)
                    val onMenu = { scope.launch { pager.animateScrollToPage(1) }; Unit }
                    if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
                        TimedSetScreen(workout, restRemaining, onChange, onComplete, onPrevious, onNext,
                            onSkipRest, onFinish, onMenu)
                    else CurrentSetScreen(workout, restRemaining, onEdit, onComplete, onPrevious, onNext,
                        onSkipRest, onFinish, onMenu)
                }
            } else if(page==2)HealthScreen(health,measuring,gps,onEnable,onDisable,onGps) { scope.launch { pager.animateScrollToPage(0) } }
            else WorkoutMenu(workout,
                { onPrevious(); scope.launch { pager.animateScrollToPage(0) } },
                { onNext(); scope.launch { pager.animateScrollToPage(0) } },
                { index -> onSelect(index); scope.launch { pager.animateScrollToPage(0) } },
                onEdit, onTimerTools, restRemaining, onFinish, onHome)
        }
        Box(Modifier.fillMaxSize().padding(bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index -> Box(Modifier.width(if (pager.currentPage == index) 12.dp else 4.dp).height(4.dp)
                    .background(if (pager.currentPage == index) Gold else Muted.copy(alpha = .45f), CircleShape)) }
            }
        }
    }
}

@Composable
private fun RestRing(workout: WearWorkout, seconds: Int) {
    Canvas(Modifier.fillMaxSize().padding(8.dp)) {
        drawArc(Gold, -90f, 360f * (seconds.toFloat() / workout.restSeconds.coerceAtLeast(1)).coerceIn(0f, 1f),
            false, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun SetHeading(workout: WearWorkout, restRemaining: Int, onMenu: () -> Unit,
                       compact: Boolean = false, detail: String? = null) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onMenu)
        .semantics { contentDescription = "Edzésmenü" }, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!compact) Label("${workout.dayName.ifBlank { workout.dayId }} · ${workout.currentExercise + 1}/${workout.exercises.size} · ··")
        Text(workout.exercise?.name.orEmpty(), modifier = Modifier.heightIn(min = (if (compact) 30 else 34).dp),
            color = White, fontSize = (if (compact) 13 else 15).sp, lineHeight = (if (compact) 15 else 17).sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    val completed = workout.exercise?.sets?.count { it.done } ?: 0
    val count = workout.exercise?.sets?.size ?: 0
    Label(when {
        restRemaining > 0 -> "Pihenő ${formatSeconds(restRemaining)} · $completed/$count rögzítve"
        workout.exerciseComplete -> "✓ $completed/$count sorozat rögzítve"
        else -> detail ?: "${if (compact) "${workout.currentExercise + 1}/${workout.exercises.size} · " else ""}Sorozat ${workout.currentSetIndex + 1}/$count"
    }, Gold, size = if (compact) 9 else 10)
}

@Composable
private fun ValueTile(value: String, label: String, modifier: Modifier, enabled: Boolean = true,
                      compact: Boolean = false, onClick: () -> Unit) {
    Column(modifier.heightIn(min = (if (compact) 36 else 44).dp).background(Card, RoundedCornerShape(14.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = White, fontSize = 24.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Label(label, size = 9)
    }
}

@Composable
private fun WorkoutControls(workout: WearWorkout, restRemaining: Int, recordingEnabled: Boolean,
                            recordLabel: String = "Rögzítés", onRecord: () -> Unit,
                            onPrevious: () -> Unit, onNext: () -> Unit, onSkipRest: () -> Unit, onFinish: () -> Unit,
                            layout: WearActionLayout) {
    Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
        CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
        RoundControl("‹", enabled = workout.currentExercise > 0, caption = "Előző", description = "Előző gyakorlat", onClick = onPrevious)
        when {
            restRemaining > 0 -> RoundControl("»", caption = "Kihagyás", description = "Pihenő kihagyása", onClick = onSkipRest)
            workout.exerciseComplete -> RoundControl("✓", enabled = false, caption = "Rögzítve", onClick = {})
            else -> RoundControl("✓", Gold, enabled = recordingEnabled, caption = recordLabel,
                description = if (recordLabel == "Mentés") "Idő rögzítése" else "Sorozat rögzítése", onClick = onRecord)
        }
        if (workout.currentExercise == workout.exercises.lastIndex)
            RoundControl("✓", if (workout.exerciseComplete) Gold else Secondary,
                enabled = workout.completedSets > 0, caption = "Befejezés", description = "Edzés befejezése", onClick = onFinish)
        else RoundControl("›", if (workout.exerciseComplete) Gold else Secondary,
            caption = "Következő", description = "Következő gyakorlat", onClick = onNext)
        }
    }
}

@Composable
private fun CurrentSetScreen(workout: WearWorkout, restRemaining: Int,
                             onEdit: (String) -> Unit, onComplete: () -> Unit,
                             onPrevious: () -> Unit, onNext: () -> Unit, onSkipRest: () -> Unit,
                             onFinish: () -> Unit, onMenu: () -> Unit) {
    val set = workout.currentSet
    ActionPanel(actions = { layout ->
        WorkoutControls(workout, restRemaining, recordingEnabled = set != null && !set.done && (set.reps.toIntOrNull() ?: 0) > 0,
            onRecord = onComplete, onPrevious = onPrevious, onNext = onNext, onSkipRest = onSkipRest, onFinish = onFinish, layout = layout)
    }) { compact ->
        SetHeading(workout, restRemaining, onMenu, compact)
        Spacer(Modifier.height((if (compact) 2 else 4).dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (workout.exercise?.loadType != "bodyweight")
                ValueTile(set?.weight?.ifBlank { "0" } ?: "0", "kg", Modifier.weight(1f), enabled = set?.done == false, compact = compact) { onEdit("weight") }
            ValueTile(set?.reps?.ifBlank { "0" } ?: "0", "ismétlés", Modifier.weight(1f), enabled = set?.done == false, compact = compact) { onEdit("reps") }
        }
    }
}

private val LocalActionSize = compositionLocalOf { 48f }

@Composable
private fun RoundControl(text: String, tone: Color = Secondary, enabled: Boolean = true,
                         caption: String? = null, description: String = caption ?: text, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val glyph = if (caption == null) 24f else 22f
    val captionScale = fontScale.coerceAtMost(1.35f)
    Column(Modifier.size(LocalActionSize.current.dp).background(if (enabled) tone else Card, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White
        Text(text, color = color, fontSize = (glyph / fontScale).sp,
            lineHeight = ((glyph + 2) / fontScale).sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (caption != null) Text(caption, modifier = Modifier.widthIn(max = 44.dp),
            color = color, fontSize = (8.5f * captionScale / fontScale).sp, lineHeight = (10f * captionScale / fontScale).sp,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueEditor(workout: WearWorkout, field: String, onChange: (String, Double) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val original = remember(workout.workoutId, workout.currentExercise, set.number, field) { fieldValue(set, field) }
    var edited by rememberSaveable(workout.workoutId, workout.currentExercise, set.number, field) { mutableDoubleStateOf(original) }
    val step = if (field == "weight") .5 else if (field.endsWith("Seconds") || (field == "reps" && normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))) 5.0 else 1.0
    val title = when (field) { "weight" -> "Súly"; "leftSeconds" -> "Bal oldal"; "rightSeconds" -> "Jobb oldal"; else -> if (step == 5.0) "Idő" else "Ismétlés" }
    ActionPanel(count = 2, gap = 13f, horizontalPadding = 24, normalBodyHeight = 107,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
        Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
            RoundControl("×", Danger, caption = "Mégse", onClick = onBack)
            RoundControl("✓", Gold, caption = "Mentés") {
                if (field.endsWith("Seconds") || (field == "reps" && normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))) {
                    // A manually entered duration must not be overwritten by
                    // the old paused stopwatch when the set is recorded.
                    WearStopwatchStore.save(context, WearStopwatch("${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"))
                }
                onChange(field, edited - fieldValue(workout.currentSet ?: set, field)); onBack()
            }
        }
            }
        }) { compact ->
        if (!compact) Label("Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}")
        Title(title, 20); Spacer(Modifier.height((if (compact) 4 else 7).dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            RoundControl("−") { edited = (edited - step).coerceAtLeast(0.0) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(numberText(edited), color = White, fontSize = 35.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Label(if (field == "weight") "kg" else if (step == 5.0) "mp" else "ismétlés")
            }
            RoundControl("+") { edited += step }
        }
        Spacer(Modifier.height(4.dp)); Label("Lépésköz: ${numberText(step)}", size = 9)

    }
}

@Composable
private fun TimedSetScreen(workout: WearWorkout, restRemaining: Int,
                           onChange: (String, Double) -> Unit, onComplete: () -> Unit,
                           onPrevious: () -> Unit, onNext: () -> Unit, onSkipRest: () -> Unit,
                           onFinish: () -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val prefix = "${workout.workoutId}:${workout.exercise?.id}:${set.number}"
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by rememberSaveable(prefix) {
        mutableStateOf(WearStopwatchStore.currentField(context, prefix)
            ?.takeIf { it == "leftSeconds" || it == "rightSeconds" }
            ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds" else "leftSeconds")
    }
    val field = if (perSide) side else "reps"
    val key = "$prefix:$field"
    var timer by remember(key) { mutableStateOf(WearStopwatchStore.load(context, key)) }
    var elapsed by remember(key) { mutableLongStateOf(timer.elapsed()) }
    LaunchedEffect(key, timer) {
        while (true) {
            val now = timer.elapsed()
            if (now / 1000 != elapsed / 1000) elapsed = now
            delay(200)
        }
    }
    ActionPanel(normalBodyHeight = 112, actions = { layout ->
        WorkoutControls(workout, restRemaining,
            recordingEnabled = !set.done && (timer.startedAt > 0 || elapsed > 0 || fieldValue(set, field) > 0),
            recordLabel = "Mentés", onRecord = {
                val measured = timer.elapsed()
                val seconds = if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
                onChange(field, seconds - fieldValue(set, field))
                timer = WearStopwatchStore.save(context, WearStopwatch(key))
                elapsed = 0L
                if (!perSide || (side == "leftSeconds" && set.rightSeconds > 0) || (side == "rightSeconds" && set.leftSeconds > 0)) onComplete()
                else {
                    side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                    WearStopwatchStore.save(context, WearStopwatch("$prefix:$side"))
                }
            }, onPrevious = onPrevious, onNext = onNext, onSkipRest = onSkipRest, onFinish = onFinish, layout = layout)
    }) { compact ->
        SetHeading(workout, restRemaining, onMenu, compact,
            detail = if (perSide) "${if (side == "leftSeconds") "Bal" else "Jobb"} oldal · ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}" else null)
        val displayed = if (set.done || (timer.startedAt == 0L && elapsed == 0L)) fieldValue(set, field).toInt()
            else (elapsed / 1000).toInt()
        Box(Modifier.width(140.dp).heightIn(min = (if (compact) 36 else 40).dp).background(Card, RoundedCornerShape(14.dp))
            .clickable(enabled = restRemaining == 0 && !set.done, role = Role.Button) {
                timer = WearStopwatchStore.save(context, timer.toggle())
            }.semantics { contentDescription = if (timer.startedAt > 0) "Stopper szüneteltetése" else "Stopper indítása" },
            contentAlignment = Alignment.Center) {
            Text(formatSeconds(displayed), color = White, fontSize = (if (compact) 31 else 34).sp, lineHeight = (if (compact) 33 else 36).sp, fontWeight = FontWeight.SemiBold)
        }
        val hint = when {
            restRemaining > 0 -> "Pihenő alatt"
            set.done -> "Rögzített idő"
            timer.startedAt > 0 -> "Ⅱ Koppints a szünethez"
            perSide -> if (side == "leftSeconds") "▶ Bal oldal indítása" else "▶ Jobb oldal indítása"
            else -> "▶ Indítás · cél: ${workout.exercise?.targetReps?.ifBlank { "—" }} mp"
        }
        if (!compact) Label(hint, size = 9)

    }
}

@Composable
private fun TimedSetToolsScreen(workout: WearWorkout, onChange: (String, Double) -> Unit, onComplete: () -> Unit,
                           onEdit: (String) -> Unit, onBack: () -> Unit) {
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
            RoundControl(if (timer.startedAt > 0) "Ⅱ" else "▶", enabled = !set.done,
                description = if (timer.startedAt > 0) "Stopper szüneteltetése" else "Stopper indítása") { timer = WearStopwatchStore.save(context, timer.toggle()) }
            RoundControl(if (perSide) "B/J" else "↺", enabled = !set.done,
                description = if (perSide) "Bal és jobb oldal váltása" else "Stopper nullázása") {
                if (perSide) {
                    val measured = timer.elapsed()
                    if (measured > 0) onChange(field, ceil(measured / 1000.0) - fieldValue(workout.currentSet ?: set, field))
                    timer = WearStopwatchStore.save(context, WearStopwatch(key))
                    side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                    WearStopwatchStore.save(context, WearStopwatch("${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"))
                } else timer = WearStopwatchStore.save(context, WearStopwatch(key))
            }
            RoundControl("±", enabled = !set.done, description = "Idő kézi beállítása") { timer = WearStopwatchStore.save(context, timer.copy(elapsedMillis = timer.elapsed(), startedAt = 0L)); onEdit(field) }
        }
        Spacer(Modifier.height(6.dp))
        Pill(if (perSide) "✓ ${if (side == "leftSeconds") "Bal" else "Jobb"} idő rögzítése" else "✓ Idő rögzítése",
            enabled = !set.done && (timer.startedAt > 0 || timer.elapsedMillis > 0 || elapsed > 0 || fieldValue(set, field) > 0)) {
            val measured = timer.elapsed()
            val seconds = if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
            onChange(field, seconds - fieldValue(workout.currentSet ?: set, field))
            timer = WearStopwatchStore.save(context, WearStopwatch(key))
            if (!perSide || (side == "leftSeconds" && set.rightSeconds > 0) || (side == "rightSeconds" && set.leftSeconds > 0)) onComplete()
            else {
                side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                WearStopwatchStore.save(context, WearStopwatch("${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"))
            }
            onBack()
        }
        Spacer(Modifier.height(7.dp)); Pill("‹ Vissza az edzéshez", tone = Secondary, onClick = onBack)
    }
}

@Composable
private fun WorkoutMenu(workout: WearWorkout, onPrevious: () -> Unit, onNext: () -> Unit,
                        onSelect: (Int) -> Unit, onEdit: (String) -> Unit, onTimerTools: () -> Unit, restRemaining: Int, onFinish: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(27.dp)); Title("Edzésmenü"); Spacer(Modifier.height(9.dp))
        Pill("‹ Kezdőlap", tone = Secondary, onClick = onHome)
        if (workout.exercise?.loadType != "bodyweight") {
            Spacer(Modifier.height(7.dp)); Pill("Súly: ${workout.currentSet?.weight?.ifBlank { "0" } ?: "0"} kg", tone = Secondary, enabled = !workout.exerciseComplete) { onEdit("weight") }
        }
        if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp")) {
            Spacer(Modifier.height(7.dp)); Pill("Stopper és oldalak", tone = Secondary,
                enabled = restRemaining == 0 && !workout.exerciseComplete, onClick = onTimerTools)
        }
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
private fun SummaryScreen(result: WearClosure, health: JSONObject?, onRetry: () -> Unit, onDone: () -> Unit) {
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
        if(result.action=="finishWorkout") {
            val metrics=wearMeasuredTotals(health,result.workoutId)
            if(metrics.isNotEmpty()) {
                Spacer(Modifier.height(10.dp));Label("ÓRÁN MÉRT ADATOK",Gold,size=10)
                for(pair in metrics.chunked(2)) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        for(metric in pair)Column(Modifier.weight(1f).background(Card,RoundedCornerShape(12.dp)).padding(vertical=7.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Label(metric.label,size=9);Title(metric.value,14)
                        }
                        if(pair.size==1)Spacer(Modifier.weight(1f))
                    }
                }
                Text("Az összes kcal az alapanyagcserét is tartalmazza.",color=Muted,fontSize=10.sp,textAlign=TextAlign.Center)
            }
            if(health?.optString("workoutId")==result.workoutId) {
                if(health.optString("state") in listOf("starting","active","ending")) {
                    Text("A mérés lezárása folyamatban; az utolsó adatok még frissülhetnek.",color=Gold,fontSize=10.sp,textAlign=TextAlign.Center)
                }
                if(health.optBoolean("partial"))Text("Részleges mérés",color=Gold,fontSize=10.sp,textAlign=TextAlign.Center)
            }
        }
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
