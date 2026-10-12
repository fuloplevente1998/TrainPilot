package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        if (route == "journal" || route == "metrics") onRefreshJournal()
    }
    LaunchedEffect(requestedRoute.value) {
        requestedRoute.value?.let { route=it;onRouteConsumed() }
    }
    var editorField by rememberSaveable { mutableStateOf("reps") }
    var miniJournalId by rememberSaveable { mutableStateOf("") }
    var miniJournalBackRoute by rememberSaveable { mutableStateOf("journal") }
    var journalBackRoute by rememberSaveable { mutableStateOf("menu") }
    var returnWorkoutPage by rememberSaveable { mutableStateOf(0) }
    fun openMeasurement(item: MiniJournalWorkout, origin: String) {
        miniJournalId = item.workoutId
        miniJournalBackRoute = origin
        if (origin == "workout") returnWorkoutPage = 2
        route = "journal-detail"
    }
    fun openJournal(origin: String) {
        journalBackRoute = origin
        if (origin == "workout") returnWorkoutPage = 2
        route = "journal"
    }
    BackHandler(enabled = route != "home") {
        route = when (route) {
            "editor", "timer", "finish" -> "workout"
            "journal-detail" -> miniJournalBackRoute
            "journal" -> journalBackRoute
            "discard" -> "home"
            else -> "home"
        }
    }
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Black)) {
            val active = workout.value
            when {
                route == "metrics" -> HealthScreen(health.value,measuring.value,gpsEnabled.value,active!=null,onEnableHealth,onDisableHealth,onToggleGps,
                    home.value, onRefreshJournal, onOpenJournal = { openJournal("metrics") },
                    onSelectMeasurement = { openMeasurement(it, "metrics") }, onBack = { route="menu" })
                route == "menu" -> MainMenuScreen(active != null, active?.quickWorkout == true, onOpenPhone) {
                    if (it == "journal") openJournal("menu") else route = it
                }
                route == "journal" -> MiniJournalScreen(home.value,
                    onRefresh = onRefreshJournal, onOpenPhone = onOpenPhone,
                    backLabel = if (journalBackRoute == "menu") "‹ Főmenü" else "‹ Mérések",
                    onBack = { route = journalBackRoute }) { selected ->
                    miniJournalId = selected.workoutId
                    miniJournalBackRoute = "journal"
                    route = "journal-detail"
                }
                route == "journal-detail" -> MiniJournalDetailScreen(
                    home.value?.recentWorkouts?.firstOrNull { it.workoutId == miniJournalId },
                    backLabel = if (miniJournalBackRoute == "journal") "‹ Napló" else "‹ Mérések",
                    onBack = { route = miniJournalBackRoute })
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
                    home = home.value, onRefreshJournal = onRefreshJournal,
                    onOpenJournal = { openJournal("workout") },
                    onSelectMeasurement = { openMeasurement(it, "workout") },
                    initialPage = returnWorkoutPage, onReturnPageConsumed = { returnWorkoutPage = 0 },
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
private fun CurvedHeader(text: String, color: Color = Gold, sweep: Float = WEAR_HEADER_SWEEP) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CurvedLayout(
            Modifier.align(Alignment.Center).size(minOf(maxWidth, maxHeight) - 16.dp).semantics {
                contentDescription = text
            }
        ) {
            basicCurvedText(
                text,
                modifier = CurvedModifier.sizeIn(maxSweepDegrees = sweep),
                style =
                    CurvedTextStyle(
                        color = color,
                        fontSize = (12.5f * fontScale.coerceAtMost(1.1f) / fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionPanel(
    title: String,
    count: Int = 2,
    gap: Float = 7f,
    actions: @Composable (WearActionLayout) -> Unit,
    content: ScalingLazyListScope.(Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearActionLayout(maxWidth.value, maxHeight.value, count, gap)
        RoundList(title) {
            content(false)
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    actions(layout)
                }
            }
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
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.35f) / fontScale
    Box(modifier.fillMaxWidth().heightIn(min = height.dp)
        .background(if (enabled) tone else Secondary, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = text }.padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White,
            fontSize = (size * captionScale).sp, lineHeight = ((size + 2) * captionScale).sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeScreen(
    home: WatchHomeSnapshot?,
    active: WearWorkout?,
    closure: WearClosure?,
    onStart: (WatchHomeDay, String) -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onDays: () -> Unit,
    onResult: () -> Unit,
    onMenu: () -> Unit,
    onCalendar: () -> Unit,
    onOpenPhone: () -> Unit,
) {
    var elapsed by remember { mutableIntStateOf(active?.elapsedSeconds() ?: 0) }
    LaunchedEffect(active?.workoutId) {
        while (active != null) {
            elapsed = active.elapsedSeconds()
            delay(1000)
        }
    }
    val waiting = closure?.status in listOf("pending", "error")
    val rec = home?.recommended
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val phone = wearHomePhoneLayout(maxWidth.value, maxHeight.value)
        val layout = wearArcLayout(maxWidth.value, maxHeight.value)
        RoundList(
            "Telefon ↗",
            initialCenterItemIndex = 0,
            initialCenterItemScrollOffset =
                with(LocalDensity.current) { (if (fontScale <= 1.1f) 16.dp else 0.dp).roundToPx() },
            onTitleClick = onOpenPhone,
            titleTouchWidth = phone.width,
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when {
                        active != null -> {
                            Box(Modifier.fillMaxWidth()) {
                                Title(active.exercise?.name ?: active.programName, 13)
                            }
                            Label(
                                "${active.dayName.ifBlank { active.dayId }} · ${active.currentSetIndex + 1}/${active.exercise?.sets?.size ?: 0}. sorozat",
                                size = 9,
                            )
                            Text(
                                formatSeconds(elapsed),
                                color = White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        waiting -> {
                            Title(
                                if (closure?.status == "error") "Szinkronizálás szükséges"
                                else "Várakozás a telefonra",
                                18,
                            )
                            Text(
                                "Az edzés az órán megmaradt.",
                                color = Muted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        home == null -> {
                            Title("Szinkronizálás…", 18)
                            Text(
                                "Nyisd meg a TrainPilotot a telefonon.",
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        home.hasDraft -> {
                            Title("Félbehagyott edzés", 18)
                            Text(
                                "Az edzés adatainak szinkronja a telefonra vár.",
                                color = Muted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        else -> {
                            Box(Modifier.fillMaxWidth()) {
                                Title(
                                    rec?.day?.programName ?: home.activeProgramName,
                                    15,
                                    lines = 1,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Title(rec?.day?.name ?: "Válassz edzésnapot", 26)
                            if (rec != null) Label(recommendedMeta(rec), size = 11)
                        }
                    }
                }
            }
            item {
                Box(Modifier.width(layout.width.dp).height(layout.height.dp)) {
                    CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
                        Box(Modifier.align(Alignment.TopCenter).offset(x = -layout.sideOffset.dp)) {
                            when {
                                active != null ->
                                    RoundControl(
                                        "▶",
                                        Gold,
                                        caption = "Folytatás",
                                        description = "Edzés folytatása",
                                        onClick = onResume,
                                    )
                                waiting ->
                                    RoundControl(
                                        "↻",
                                        Gold,
                                        caption = "Állapot",
                                        description = "Edzés mentésének állapota",
                                        onClick = onResult,
                                    )
                                rec != null && home?.hasDraft == false ->
                                    RoundControl(
                                        "▶",
                                        Gold,
                                        caption = "Indítás",
                                        description = "Edzés indítása",
                                    ) {
                                        onStart(rec.day, rec.scheduleId)
                                    }
                                else ->
                                    RoundControl(
                                        "A/B",
                                        Gold,
                                        enabled =
                                            home != null &&
                                                home.hasDraft == false &&
                                                home.days.isNotEmpty(),
                                        caption = "Napok",
                                        description = "Program napjai",
                                        onClick = onDays,
                                    )
                            }
                        }
                        Box(Modifier.align(Alignment.TopCenter).offset(y = layout.centerTop.dp)) {
                            if (active != null)
                                RoundControl(
                                    "✕",
                                    Danger,
                                    caption = "Törlés",
                                    description = "Megkezdett edzés törlése",
                                    onClick = onDiscard,
                                )
                            else
                                RoundControl(
                                    "▦",
                                    caption = "Naptár",
                                    description = "Edzésnaptár",
                                    onClick = onCalendar,
                                )
                        }
                        Box(Modifier.align(Alignment.TopCenter).offset(x = layout.sideOffset.dp)) {
                            RoundControl(
                                "···",
                                caption = "Menü",
                                description = "Főmenü",
                                onClick = onMenu,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundList(
    title: String,
    initialCenterItemIndex: Int = 1,
    initialCenterItemScrollOffset: Int = 0,
    onTitleClick: (() -> Unit)? = null,
    titleTouchWidth: Float = 100f,
    sweep: Float = WEAR_HEADER_SWEEP,
    headerColor: Color = Gold,
    content: ScalingLazyListScope.() -> Unit,
) {
    val state =
        rememberScalingLazyListState(
            initialCenterItemIndex = initialCenterItemIndex,
            initialCenterItemScrollOffset = initialCenterItemScrollOffset,
        )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = wearRoundViewport(maxWidth.value, maxHeight.value)
        ScalingLazyColumn(
            Modifier.fillMaxSize().hierarchicalFocusGroup(active = true).testTag("wear-round-list"),
            state = state,
            contentPadding =
                PaddingValues(
                    horizontal = ((maxWidth.value - viewport.width) / 2f).dp,
                    vertical = (viewport.height / 2f).dp,
                ),
            autoCentering = null,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
        CurvedHeader(title, headerColor, sweep = sweep)
        if (onTitleClick != null)
            Box(
                Modifier.align(Alignment.TopCenter)
                    .width(titleTouchWidth.dp)
                    .height(32.dp)
                    .clickable(role = Role.Button, onClick = onTitleClick)
                    .semantics { contentDescription = "Megnyitás telefonon" }
            )
    }
}

/** Each row is a separate lazy item, so even the first and last controls can reach the center. */
private fun ScalingLazyListScope.centerItem(content: @Composable ColumnScope.() -> Unit) {
    item {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
internal fun MainMenuScreen(
    hasWorkout: Boolean,
    quickWorkout: Boolean,
    onOpenPhone: () -> Unit,
    onRoute: (String) -> Unit,
) {
    RoundList("Főmenü") {
        for ((label, destination) in
            listOf(
                "Kezdőlap" to "home",
                "Mini napló" to "journal",
                "Naptár" to "calendar",
                "Programnapok" to "days",
                "Órás mérések" to "metrics",
            )) {
            centerItem { Pill(label, tone = Card) { onRoute(destination) } }
        }
        if (!hasWorkout || quickWorkout) {
            centerItem {
                Pill(if (quickWorkout) "+ Gyakorlat" else "Gyors edzés", tone = Gold) {
                    onRoute("quick")
                }
            }
        }
        if (hasWorkout) {
            centerItem { Pill("Edzés folytatása") { onRoute("workout") } }
            centerItem { Pill("Edzés törlése", tone = Danger) { onRoute("discard") } }
        }
        centerItem { Pill("Megnyitás telefonon", tone = Card, onClick = onOpenPhone) }
    }
}

private fun miniJournalDate(value: String): String =
    try {
        DateTimeFormatter.ofPattern("MM. dd. HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(value))
    } catch (_: Exception) {
        "—"
    }

private fun miniNumber(value: Double?, decimals: Int = 0): String? {
    if (value == null || !value.isFinite()) return null
    return if (decimals == 0) kotlin.math.round(value).toInt().toString()
    else String.format(java.util.Locale.forLanguageTag("hu-HU"), "%.${decimals}f", value)
}

@Composable
private fun MiniJournalScreen(
    home: WatchHomeSnapshot?,
    onRefresh: () -> Unit,
    onOpenPhone: () -> Unit,
    backLabel: String,
    onBack: () -> Unit,
    onSelect: (MiniJournalWorkout) -> Unit,
) {
    val items = home?.recentWorkouts.orEmpty()
    RoundList("Mini napló") {
        centerItem { Label("Mentett telefonos edzések · offline másolat", size = 10) }

        if (items.isEmpty()) {
            val message =
                when {
                    home == null -> "Várakozás a telefonos napló szinkronizálására."
                    !home.journalAvailable ->
                        "A telefon legutóbbi adatcsomagja még nem tartalmaz Mini naplót. Frissítsd és nyisd meg a telefonos TrainPilotot."
                    else -> "A telefonos naplóban még nincs átadható, befejezett edzés."
                }
            centerItem {
                Text(message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        if (home?.phoneVersion?.isNotBlank() == true) {
            centerItem { Label("Telefon: ${home.phoneVersion}", size = 10) }
        }
        items.forEach { item ->
            centerItem {
                Pill(
                    "${miniJournalDate(item.started)} • ${item.dayName}",
                    tone = Card,
                    size = 12,
                    height = 47,
                ) {
                    onSelect(item)
                }
            }
        }

        centerItem { Pill("Frissítés", tone = Secondary, onClick = onRefresh) }

        centerItem { Pill("Telefon megnyitása", tone = Secondary, onClick = onOpenPhone) }

        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
    }
}

private fun ScalingLazyListScope.MiniJournalMetrics(
    title: String,
    metrics: MiniJournalMetric,
    watch: Boolean,
) {
    centerItem { Label(title, Gold, 12) }
    val avg = miniNumber(metrics.averageHeartRate, 1)
    val max = miniNumber(metrics.maxHeartRate)
    if (avg != null || max != null) {
        centerItem {
            Text(
                "Pulzus: ${avg ?: "—"} / ${max ?: "—"} bpm",
                color = White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
    if (watch) {
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem {
                Text(
                    "Összes mért energia: $value kcal",
                    color = White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        miniNumber(metrics.activeDurationSeconds)?.let { value ->
            centerItem { Label("Órás mérési idő: $value s", size = 11) }
        }
    } else {
        miniNumber(metrics.workoutCalories)?.let { value ->
            centerItem {
                Text(
                    "Edzéskalória: $value kcal",
                    color = White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem { Label("Ablak összenergiája: $value kcal", size = 11) }
        }
        miniNumber(metrics.activeCalories)?.let { value ->
            centerItem { Label("Aktív energia: $value kcal", size = 11) }
        }
        miniNumber(metrics.exerciseMinutes)?.let { value ->
            centerItem { Label("Egészségapp edzésideje: $value perc", size = 11) }
        }
    }
    miniNumber(metrics.steps)?.let { centerItem { Label("Lépések: $it", size = 11) } }
    if (metrics.partial) centerItem { Label("Részleges órás mérés", Gold, 10) }
}

@Composable
private fun MiniJournalDetailScreen(
    item: MiniJournalWorkout?,
    backLabel: String,
    onBack: () -> Unit,
) {
    RoundList("Edzésnapló") {
        if (item == null) {
            centerItem {
                Text(
                    "Ez az edzés már nincs a szinkronizált listában.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            centerItem { Title(item.dayName, 16) }
            centerItem { Label(miniJournalDate(item.started), size = 11) }
            centerItem {
                Label(
                    "${item.durationSeconds / 60} perc · ${item.exercises.size} gyakorlat",
                    size = 11,
                )
            }

            if (item.wear != null) {
                MiniJournalMetrics("TrainPilot óra · Health Services", item.wear, true)
            } else centerItem { Label("Nem érkezett órás mérési összesítés", size = 11) }
            if (item.healthConnect != null) {
                MiniJournalMetrics(
                    if (item.healthConnect.source == "samsung_health") "Samsung Health"
                    else "Health Connect",
                    item.healthConnect,
                    false,
                )

                centerItem { Label("A két mérés nem adódik össze", size = 10) }
            }

            centerItem { Label("Elvégzett gyakorlatok", Gold, 12) }
            if (item.exercises.isEmpty()) {
                centerItem { Label("Nincs rögzített sorozat", size = 11) }
            }
            item.exercises.forEach { exercise ->
                centerItem {
                    Text(
                        exercise.name,
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                exercise.sets.forEach { set ->
                    val details = buildList {
                        if (set.reps.isNotBlank()) add("${set.reps} ism.")
                        if (set.weight != null && set.weight > 0.0)
                            add("${miniNumber(set.weight, 1)} kg")
                        if (
                            set.leftSeconds != null &&
                                set.rightSeconds != null &&
                                set.leftSeconds > 0.0 &&
                                set.rightSeconds > 0.0
                        )
                            add(
                                "${miniNumber(set.leftSeconds)} / ${miniNumber(set.rightSeconds)} s"
                            )
                        if (set.distanceMeters != null && set.distanceMeters > 0.0)
                            add("${miniNumber(set.distanceMeters)} m")
                    }
                    centerItem {
                        Label(
                            "${set.number}. sorozat • ${details.joinToString(" · ").ifBlank { "Kész" }}",
                            size = 11,
                        )
                    }
                }
            }
        }

        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun QuickWorkoutPicker(
    home: WatchHomeSnapshot?,
    active: WearWorkout?,
    canStart: Boolean,
    onBack: () -> Unit,
    onSelect: (WatchHomeExercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val adding = active?.quickWorkout == true
    val catalog = home?.quickExercises.orEmpty()
    val matches =
        remember(catalog, query, active?.exercises) {
            val needle = query.trim()
            catalog.filter { item ->
                (needle.isBlank() ||
                    item.name.contains(needle, ignoreCase = true) ||
                    item.id.contains(needle, ignoreCase = true)) &&
                    (!adding || active?.exercises?.none { it.id == item.id } == true)
            }
        }
    val pageCount = ((matches.size + 11) / 12).coerceAtLeast(1)
    val selectedPage = page.coerceIn(0, pageCount - 1)
    RoundList(if (adding) "+ Gyakorlat" else "Gyors edzés") {
        if (active != null && !adding) {
            centerItem {
                Text(
                    "Előbb zárd le a jelenlegi edzést.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (!adding && !canStart) {
            centerItem {
                Text(
                    "A telefonos edzésvázlat vagy a függő szinkron lezárása után indítható.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (catalog.isEmpty()) {
            centerItem {
                Text(
                    "A gyakorlatok szinkronizálásához nyisd meg a TrainPilotot a telefonon.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            centerItem {
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(60)
                        page = 0
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp),
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Secondary, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                    decorationBox = { content ->
                        Box {
                            if (query.isEmpty())
                                Text("Gyakorlat keresése…", color = Muted, fontSize = 13.sp)
                            content()
                        }
                    },
                )
            }

            if (matches.isEmpty())
                centerItem {
                    Text(
                        "Nincs további választható gyakorlat.",
                        color = Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            matches.drop(selectedPage * 12).take(12).forEach { exercise ->
                centerItem {
                    Pill(exercise.name, tone = Card, height = 48, size = 12) { onSelect(exercise) }
                }
            }
            if (pageCount > 1) {
                centerItem { Label("${selectedPage + 1} / $pageCount") }
                if (selectedPage > 0)
                    centerItem { Pill("‹ Előző", tone = Secondary) { page = selectedPage - 1 } }
                if (selectedPage + 1 < pageCount)
                    centerItem {
                        Pill("További gyakorlatok ›", tone = Secondary) { page = selectedPage + 1 }
                    }
            }
        }

        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun CalendarScreen(
    home: WatchHomeSnapshot?,
    canStart: Boolean,
    onBack: () -> Unit,
    onStart: (WatchCalendarEntry) -> Unit,
) {
    val today = LocalDate.now()
    val entries = home?.calendar.orEmpty().filter { wearCalendarDate(it) != null }
    val weeks = wearCalendarWeeks(entries, today)
    var weekKey by rememberSaveable { mutableStateOf(wearCalendarMonday(today).toString()) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val week = runCatching { LocalDate.parse(weekKey) }.getOrDefault(wearCalendarMonday(today))
    BackHandler(enabled = selectedKey != null) { selectedKey = null }
    selectedKey?.let { key ->
        val selected = LocalDate.parse(key)
        CalendarDayScreen(
            selected,
            entries.filter { wearCalendarDate(it) == selected },
            today,
            canStart,
            onBack = { selectedKey = null },
            onStart = onStart,
        )
        return
    }
    if (home == null || entries.isEmpty()) {
        RoundList("Naptár") {
            centerItem {
                Text(
                    "A naptár frissítéséhez nyisd meg a TrainPilotot a telefonon.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
            centerItem { Pill("‹ Főmenü", tone = Secondary, onClick = onBack) }
        }
        return
    }
    // Native paging consumes a horizontal drag before it can select a day.
    // Keep only cached weeks, as requested; no unknown future/history pages.
    key(weeks) {
        val pager =
            rememberPagerState(
                initialPage =
                    weeks.indexOf(week).takeIf { it >= 0 }
                        ?: weeks.indices.minByOrNull {
                            kotlin.math.abs(
                                java.time.temporal.ChronoUnit.DAYS.between(weeks[it], today)
                            )
                        }
                        ?: 0,
                pageCount = { weeks.size },
            )
        LaunchedEffect(pager.settledPage) { weekKey = weeks[pager.settledPage].toString() }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { weeks[it].toString() },
        ) { page ->
            CalendarWeekPage(weeks[page], entries, today, page + 1, weeks.size, onBack) {
                selectedKey = it.toString()
            }
        }
    }
}

@Composable
private fun CalendarWeekPage(
    week: LocalDate,
    entries: List<WatchCalendarEntry>,
    today: LocalDate,
    pageNumber: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.25f)
    fun textSize(size: Float) = (size * captionScale / fontScale).sp
    val locale = java.util.Locale.forLanguageTag("hu")
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-calendar-week")) {
        val layout = wearCalendarLayout(maxWidth.value, maxHeight.value)
        val caption = wearCalendarCaption(week, today)
        Text(
            caption.month,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = layout.monthTop.dp)
                    .width((minOf(maxWidth, maxHeight) * .76f)),
            color = White,
            fontSize = textSize(if (minOf(maxWidth, maxHeight).value < 180f) 16f else 20f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            caption.dates,
            color = Muted,
            fontSize = textSize(10f),
            textAlign = TextAlign.Center,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = layout.weekTop.dp)
                    .width(minOf(maxWidth, maxHeight) * .76f)
                    .semantics {
                        contentDescription =
                            "${caption.dates}. Átvett hét: $pageNumber / $pageCount. Hétváltás balra vagy jobbra csúsztatással."
                    },
            maxLines = 1,
        )
        val dayScale = minOf(1f, layout.buttonSize / 40f)
        val labels = listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
        layout.days.forEachIndexed { dayIndex, point ->
            val date = week.plusDays(dayIndex.toLong())
            val records = entries.filter { wearCalendarDate(it) == date }
            val current = date == today
            val color =
                if (current) Black
                else if (records.isEmpty()) Muted else if (dayIndex == 6) DangerText else White
            Column(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = point.x.dp, y = (point.y - layout.buttonSize / 2f).dp)
                    .size(layout.buttonSize.dp)
                    .background(
                        if (current) Gold else if (records.isEmpty()) Card else Secondary,
                        CircleShape,
                    )
                    .clickable(role = Role.Button) { onSelect(date) }
                    .semantics {
                        contentDescription =
                            date.format(DateTimeFormatter.ofPattern("MMMM d., EEEE", locale)) +
                                if (current) ", ma" else ""
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    labels[dayIndex],
                    color = color,
                    fontSize = textSize(9f * dayScale),
                    lineHeight = textSize(10f * dayScale),
                    maxLines = 1,
                )
                Text(
                    "${date.dayOfMonth}",
                    color = color,
                    fontSize = textSize(17f * dayScale),
                    lineHeight = textSize(19f * dayScale),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            val status = records.map { it.status }
            val marker =
                when {
                    "planned" in status -> Gold
                    "completed" in status -> Color(0xFF7FD4BA)
                    "skipped" in status -> Muted
                    else -> null
                }
            if (marker != null)
                Box(
                    Modifier.align(Alignment.TopCenter)
                        .offset(x = point.x.dp, y = (point.y + layout.buttonSize / 2f + 2f).dp)
                        .size(4.dp)
                        .background(marker, CircleShape)
                )
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = layout.menuTop.dp)
                .width(56.dp)
                .height(20.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Vissza a főmenübe" },
            contentAlignment = Alignment.Center,
        ) {
            Text("Menü", color = Muted, fontSize = textSize(9f), maxLines = 1)
        }
    }
}

@Composable
private fun CalendarDayScreen(
    date: LocalDate,
    entries: List<WatchCalendarEntry>,
    today: LocalDate,
    canStart: Boolean,
    onBack: () -> Unit,
    onStart: (WatchCalendarEntry) -> Unit,
) {
    RoundList(
        date.format(
            DateTimeFormatter.ofPattern("MMM d., EEEE", java.util.Locale.forLanguageTag("hu"))
        )
    ) {
        if (entries.isEmpty()) {
            centerItem {
                Text(
                    "Ehhez a naphoz nincs adat az órán. A telefonos naptárban találod meg.",
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        entries.forEach { entry ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Card, RoundedCornerShape(18.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    entry.day?.let { day ->
                        Label(day.programName)
                        Title(day.name, 16)
                        Label("${day.exercises.size} gyakorlat", size = 10)
                    }
                    Label(
                        when (entry.status) {
                            "completed" -> "✓ Teljesítve"
                            "skipped" -> "Kihagyva"
                            "rest" -> "Pihenőnap"
                            else -> "Tervezett"
                        },
                        size = 11,
                    )
                    val time =
                        runCatching {
                                Instant.parse(entry.plannedStart)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                            }
                            .getOrNull()
                    if (time != null) Label(time, size = 10)
                    if (wearCalendarCanStart(entry, today, canStart)) {
                        Spacer(Modifier.height(6.dp))
                        Pill("▶ Edzés indítása") { onStart(entry) }
                    } else if (
                        !canStart &&
                            entry.status == "planned" &&
                            entry.day != null &&
                            !date.isBefore(today)
                    ) {
                        Text(
                            "Az aktív edzés vagy a függő szinkron lezárása után indítható.",
                            color = Muted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        centerItem { Pill("‹ Heti naptár", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun HealthScreen(
    data: JSONObject?,
    enabled: Boolean,
    gps: Boolean,
    hasWorkout: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    home: WatchHomeSnapshot?,
    onRefresh: () -> Unit,
    onOpenJournal: () -> Unit,
    onSelectMeasurement: (MiniJournalWorkout) -> Unit,
    onBack: () -> Unit,
) {
    fun value(key: String, unit: String, divisor: Double = 1.0): String {
        val number = data?.takeIf { it.has(key) && !it.isNull(key) }?.optDouble(key)
        return if (number == null || !number.isFinite()) "—"
        else "${trimWearNumber(number/divisor)} $unit"
    }
    RoundList("Órás mérések") {
        if (!hasWorkout && data != null) centerItem { Label("Legutóbbi edzés mérése", size = 10) }
        centerItem {
            Text(
                wearRecordingStatus(data, enabled, hasWorkout),
                color = Gold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
        data
            ?.optLong("updatedAt")
            ?.takeIf { it > 0L }
            ?.let {
                centerItem {
                    Label(
                        "Frissítve: ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                        size = 10,
                    )
                }
            }
        if (wearRecordingNeedsRetry(data, enabled, hasWorkout)) {

            centerItem { Pill("Aktuális edzés mérése", tone = Gold, onClick = onEnable) }
        }

        val fresh =
            data?.optString("state") == "active" &&
                System.currentTimeMillis() -
                    (data.optLong("lastSampleAt", data.optLong("updatedAt"))) < 15000
        centerItem { Label(if (fresh) "Élő pulzus" else "Pulzus · utolsó mérés", size = 10) }
        centerItem { Title(value("heartRate", "bpm"), 28) }
        for ((title, metric, unit) in
            listOf(
                Triple("Átlagpulzus", "averageHeartRate", "bpm"),
                Triple("Max. pulzus", "maxHeartRate", "bpm"),
                Triple("Összes energia", "totalCalories", "kcal"),
                Triple("Lépések", "steps", "lépés"),
                Triple("Aktív mérési idő", "activeDurationSeconds", "s"),
                Triple("Távolság", "distanceMeters", "m"),
                Triple("Sebesség", "speedMps", "m/s"),
            )) {
            centerItem { Label(title, size = 10) }
            centerItem { Title(value(metric, unit), 17) }
        }
        val speed = data?.optDouble("speedMps", 0.0) ?: 0.0
        if (speed > 0 && speed.isFinite()) {
            centerItem { Label("Aktuális tempó") }
            centerItem { Title("${formatSeconds((1000/speed).toInt())} /km", 17) }
        }
        centerItem {
            Text(
                "A hiányzó adatot az óra még nem adta át. Az összes kcal az alapanyagcserét is tartalmazza.",
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (data?.optBoolean("partial") == true)
            centerItem {
                Text(
                    "Részleges mérés: a rögzítés edzés közben indult újra.",
                    color = Gold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        data
            ?.optString("message")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                centerItem {
                    Text(it, color = Gold, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }

        centerItem {
            Pill(
                if (enabled) "Mérés kikapcsolása" else "Mérések engedélyezése",
                tone = if (enabled) Secondary else Gold,
                onClick = if (enabled) onDisable else onEnable,
            )
        }
        centerItem {
            Pill(if (gps) "GPS kikapcsolása" else "GPS engedélyezése", tone = Card, onClick = onGps)
        }
        centerItem {
            Text(
                "GPS csak mozgásos edzésnél használható, a következő mérés indításától. Az óra az engedélyezett edzésmérést a háttérben is folytatja.",
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }

        centerItem { Label("Korábbi órás mérések", Gold, 13) }
        centerItem { Label("Mentett edzések · TrainPilot óra", size = 10) }
        val previous = WatchMiniJournal.measurementHistory(home?.recentWorkouts.orEmpty()).take(3)
        if (previous.isEmpty()) {
            centerItem {
                Text(
                    if (home?.journalAvailable == true)
                        "A szinkronizált edzések között még nincs órás mérési összesítés."
                    else "Az előzményekhez nyisd meg a telefonos TrainPilotot, majd frissíts.",
                    color = Muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        previous.forEach { item ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Card, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { onSelectMeasurement(item) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        item.dayName,
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Label(miniJournalDate(item.started), size = 10)
                    val duration =
                        if (item.durationSeconds < 60) "${item.durationSeconds} s"
                        else "${item.durationSeconds / 60} perc"
                    Label("$duration · ${item.exercises.size} gyakorlat", size = 10)
                    item.exercises.take(2).forEach { Label(it.name, size = 10) }
                    val metrics = item.wear!!
                    miniNumber(metrics.totalCalories)?.let {
                        Label("Összes mért energia: $it kcal", Gold, 11)
                    }
                    miniNumber(metrics.averageHeartRate, 1)?.let {
                        Label("Átlagpulzus: $it bpm", size = 11)
                    }
                    if (metrics.partial) Label("Részleges mérés", Gold, 10)
                    Label("Edzés részletei ›", size = 10)
                }
            }
        }
        centerItem { Pill("Mini napló megnyitása", tone = Card, onClick = onOpenJournal) }
        centerItem { Pill("Előzmények frissítése", tone = Secondary, onClick = onRefresh) }
        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun ProgramDays(
    home: WatchHomeSnapshot,
    onBack: () -> Unit,
    onSelect: (WatchHomeDay) -> Unit,
) {
    RoundList("Program napjai") {
        centerItem {
            Text(
                home.activeProgramName,
                color = Gold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }

        home.days.forEach { day ->
            centerItem {
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(Card, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button) { onSelect(day) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).background(Gold.copy(alpha = .16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            day.name.substringAfterLast(" ").take(2),
                            color = Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(day.name, color = White, fontSize = 14.sp, maxLines = 2)
                        Text("${day.exercises.size} gyakorlat", color = Muted, fontSize = 11.sp)
                    }
                    Text("›", color = Gold)
                }
            }
        }
        centerItem { Pill("‹ Kezdőlap", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun WorkoutPager(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    measuring: Boolean,
    gps: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onEdit: (String) -> Unit,
    onTimerTools: () -> Unit,
    home: WatchHomeSnapshot?,
    onRefreshJournal: () -> Unit,
    onOpenJournal: () -> Unit,
    onSelectMeasurement: (MiniJournalWorkout) -> Unit,
    initialPage: Int,
    onReturnPageConsumed: () -> Unit,
    onFinish: () -> Unit,
    onHome: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    LaunchedEffect(Unit) { onReturnPageConsumed() }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage == 2) onRefreshJournal() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .hierarchicalFocusGroup(active = pager.currentPage == page)
                    .then(
                        if (pager.currentPage == page) Modifier
                        else Modifier.clearAndSetSemantics {}
                    )
            ) { page ->
                if (page == 0) {
                    Box(Modifier.fillMaxSize()) {
                        if (restRemaining > 0) RestRing(workout, restRemaining)
                        val onMenu = {
                            scope.launch { pager.animateScrollToPage(1) }
                            Unit
                        }
                        if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
                            TimedSetScreen(
                                workout,
                                restRemaining,
                                health,
                                onChange,
                                onComplete,
                                onPrevious,
                                onNext,
                                onSkipRest,
                                onFinish,
                                onMenu,
                            )
                        else
                            CurrentSetScreen(
                                workout,
                                restRemaining,
                                health,
                                onEdit,
                                onComplete,
                                onPrevious,
                                onNext,
                                onSkipRest,
                                onFinish,
                                onMenu,
                            )
                    }
                } else if (page == 2)
                    HealthScreen(
                        health,
                        measuring,
                        gps,
                        true,
                        onEnable,
                        onDisable,
                        onGps,
                        home,
                        onRefreshJournal,
                        onOpenJournal,
                        onSelectMeasurement,
                    ) {
                        scope.launch { pager.animateScrollToPage(0) }
                    }
                else
                    WorkoutMenu(
                        workout,
                        {
                            onPrevious()
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        {
                            onNext()
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        { index ->
                            onSelect(index)
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        onEdit,
                        onTimerTools,
                        restRemaining,
                        onFinish,
                        onHome,
                    )
            }
        }
        Box(
            Modifier.fillMaxSize().padding(bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.width(if (pager.currentPage == index) 12.dp else 4.dp)
                            .height(4.dp)
                            .background(
                                if (pager.currentPage == index) Gold else Muted.copy(alpha = .45f),
                                CircleShape,
                            )
                    )
                }
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
private fun WorkoutPanel(
    workout: WearWorkout,
    onMenu: () -> Unit,
    actions: @Composable (WearArcLayout) -> Unit,
    content: ScalingLazyListScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearArcLayout(maxWidth.value, maxHeight.value)
        RoundList(
            workout.exercise?.name.orEmpty(),
            initialCenterItemIndex = if (LocalDensity.current.fontScale > 1.25f) 0 else 1,
            sweep = 140f,
            headerColor = White,
        ) {
            content()
            item {
                Box(Modifier.width(layout.width.dp).height(layout.height.dp)) { actions(layout) }
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = ((maxHeight - minOf(maxWidth, maxHeight)) / 2))
                .width(minOf(maxWidth, maxHeight) * .7f)
                .height(32.dp)
                .clickable(role = Role.Button, onClick = onMenu)
                .semantics {
                    contentDescription = "${workout.exercise?.name.orEmpty()} · Edzésmenü"
                }
        )
    }
}

@Composable
private fun SetHeading(workout: WearWorkout, restRemaining: Int, detail: String? = null) {
    val completed = workout.exercise?.sets?.count { it.done } ?: 0
    val count = workout.exercise?.sets?.size ?: 0
    Label(when {
        restRemaining > 0 -> "Pihenő ${formatSeconds(restRemaining)} · $completed/$count"
        workout.exerciseComplete -> "✓ $completed/$count sorozat rögzítve"
        else -> detail ?: "${workout.currentExercise + 1}/${workout.exercises.size} · Sorozat ${workout.currentSetIndex + 1}/$count"
    }, Gold, size = 8)
}

@Composable
private fun ValueTile(value: String, label: String, modifier: Modifier, enabled: Boolean = true,
                      onClick: () -> Unit) {
    Column(modifier.heightIn(min = 32.dp).background(Card, RoundedCornerShape(10.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = White, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Label(if (label == "ismétlés") "ism." else label, size = 8)
    }
}

@Composable
private fun WorkoutControls(workout: WearWorkout, restRemaining: Int, recordingEnabled: Boolean,
                            recordLabel: String = "Rögzítés", onRecord: () -> Unit,
                            onPrevious: () -> Unit, onNext: () -> Unit, onSkipRest: () -> Unit, onFinish: () -> Unit,
                            layout: WearArcLayout) {
    CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
        Box {
            RoundControl("‹", enabled = workout.currentExercise > 0, caption = "Előző", description = "Előző gyakorlat", onClick = onPrevious)
        }
        Box(Modifier.offset(x = layout.sideOffset.dp, y = (layout.centerTop).dp)) {
            when {
                restRemaining > 0 -> RoundControl("»", caption = "Kihagyás", description = "Pihenő kihagyása", onClick = onSkipRest)
                workout.exerciseComplete -> RoundControl("✓", enabled = false, caption = "Rögzítve", onClick = {})
                else -> RoundControl("✓", Gold, enabled = recordingEnabled, caption = recordLabel,
                    description = if (recordLabel == "Mentés") "Idő rögzítése" else "Sorozat rögzítése", onClick = onRecord)
            }
        }
        Box(Modifier.offset(x = (2 * layout.sideOffset).dp)) {
            if (workout.currentExercise == workout.exercises.lastIndex)
                RoundControl("✓", if (workout.exerciseComplete) Gold else Secondary,
                    enabled = workout.completedSets > 0, caption = "Befejezés", description = "Edzés befejezése", onClick = onFinish)
            else RoundControl("›", if (workout.exerciseComplete) Gold else Secondary,
                caption = "Következő", description = "Következő gyakorlat", onClick = onNext)
        }
    }
}

@Composable
private fun LiveWorkoutHealth(workout: WearWorkout, health: JSONObject?) {
    wearLiveWorkoutRows(health, workout.workoutId, System.currentTimeMillis()).forEach {
        Text(it, color = Gold, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CurrentSetScreen(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    onEdit: (String) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
) {
    val set = workout.currentSet
    WorkoutPanel(
        workout,
        onMenu,
        actions = { layout ->
            WorkoutControls(
                workout,
                restRemaining,
                recordingEnabled = set != null && !set.done && (set.reps.toIntOrNull() ?: 0) > 0,
                onRecord = onComplete,
                onPrevious = onPrevious,
                onNext = onNext,
                onSkipRest = onSkipRest,
                onFinish = onFinish,
                layout = layout,
            )
        },
    ) {
        centerItem {
            SetHeading(workout, restRemaining)
            LiveWorkoutHealth(workout, health)
        }

        centerItem {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (workout.exercise?.loadType != "bodyweight")
                    ValueTile(
                        set?.weight?.ifBlank { "0" } ?: "0",
                        "kg",
                        Modifier.weight(1f),
                        enabled = set?.done == false,
                    ) {
                        onEdit("weight")
                    }
                ValueTile(
                    set?.reps?.ifBlank { "0" } ?: "0",
                    "ismétlés",
                    Modifier.weight(1f),
                    enabled = set?.done == false,
                ) {
                    onEdit("reps")
                }
            }
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
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val actionSize = LocalActionSize.current
    val captionStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val captionSize = remember(caption, captionScale, density, textMeasurer, actionSize, captionStyle) {
        val requested = 8.5f * captionScale / fontScale
        val measured = caption?.let { textMeasurer.measure(it, captionStyle.copy(fontSize = requested.sp)).size.width } ?: 0
        val available = with(density) { (actionSize - 4f).dp.toPx() }
        requested * if (measured > available) available / measured else 1f
    }
    Column(Modifier.size(LocalActionSize.current.dp).background(if (enabled) tone else Card, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White
        Text(text, color = color, fontSize = (glyph / fontScale).sp,
            lineHeight = ((glyph + 2) / fontScale).sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (caption != null) Text(caption, modifier = Modifier.widthIn(max = (actionSize - 4f).dp), style = captionStyle,
            color = color, fontSize = captionSize.sp, lineHeight = (captionSize * 1.18f).sp,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueEditor(
    workout: WearWorkout,
    field: String,
    onChange: (String, Double) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val original =
        remember(workout.workoutId, workout.currentExercise, set.number, field) {
            fieldValue(set, field)
        }
    var edited by
        rememberSaveable(workout.workoutId, workout.currentExercise, set.number, field) {
            mutableDoubleStateOf(original)
        }
    val step =
        if (field == "weight") .5
        else if (
            field.endsWith("Seconds") ||
                (field == "reps" &&
                    normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
        )
            5.0
        else 1.0
    val title =
        when (field) {
            "weight" -> "Súly"
            "leftSeconds" -> "Bal oldal"
            "rightSeconds" -> "Jobb oldal"
            else -> if (step == 5.0) "Idő" else "Ismétlés"
        }
    RoundList(title) {
        centerItem {
            Label(
                "Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}",
                size = 9,
            )
        }
        centerItem {
            Text(
                numberText(edited),
                color = White,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        centerItem {
            Label(if (field == "weight") "kg" else if (step == 5.0) "mp" else "ismétlés", size = 9)
        }
        centerItem {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundControl("−", description = "$title csökkentése") {
                    edited = (edited - step).coerceAtLeast(0.0)
                }
                RoundControl("+", description = "$title növelése") { edited += step }
            }
        }
        centerItem { Label("Lépésköz: ${numberText(step)}", size = 9) }

        centerItem {
            Pill("✓ Mentés") {
                if (
                    field.endsWith("Seconds") ||
                        (field == "reps" &&
                            normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
                ) {
                    // A manually entered duration must not be overwritten by
                    // the old paused stopwatch when the set is recorded.
                    WearStopwatchStore.save(
                        context,
                        WearStopwatch(
                            "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"
                        ),
                    )
                }
                onChange(field, edited - fieldValue(workout.currentSet ?: set, field))
                onBack()
            }
        }
        centerItem { Pill("Mégse", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun TimedSetScreen(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val prefix = "${workout.workoutId}:${workout.exercise?.id}:${set.number}"
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by
        rememberSaveable(prefix) {
            mutableStateOf(
                WearStopwatchStore.currentField(context, prefix)?.takeIf {
                    it == "leftSeconds" || it == "rightSeconds"
                }
                    ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds"
                    else "leftSeconds"
            )
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
    WorkoutPanel(
        workout,
        onMenu,
        actions = { layout ->
            WorkoutControls(
                workout,
                restRemaining,
                recordingEnabled =
                    !set.done && (timer.startedAt > 0 || elapsed > 0 || fieldValue(set, field) > 0),
                recordLabel = "Mentés",
                onRecord = {
                    val measured = timer.elapsed()
                    val seconds =
                        if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
                    onChange(field, seconds - fieldValue(set, field))
                    timer = WearStopwatchStore.save(context, WearStopwatch(key))
                    elapsed = 0L
                    if (
                        !perSide ||
                            (side == "leftSeconds" && set.rightSeconds > 0) ||
                            (side == "rightSeconds" && set.leftSeconds > 0)
                    )
                        onComplete()
                    else {
                        side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                        WearStopwatchStore.save(context, WearStopwatch("$prefix:$side"))
                    }
                },
                onPrevious = onPrevious,
                onNext = onNext,
                onSkipRest = onSkipRest,
                onFinish = onFinish,
                layout = layout,
            )
        },
    ) {
        centerItem {
            SetHeading(
                workout,
                restRemaining,
                detail =
                    if (perSide)
                        "${if (side == "leftSeconds") "Bal" else "Jobb"} oldal · ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}"
                    else null,
            )
            LiveWorkoutHealth(workout, health)
        }
        val displayed =
            if (set.done || (timer.startedAt == 0L && elapsed == 0L)) fieldValue(set, field).toInt()
            else (elapsed / 1000).toInt()
        item {
            Box(
                Modifier.fillMaxWidth()
                    .heightIn(min = 34.dp)
                    .background(Card, RoundedCornerShape(10.dp))
                    .clickable(enabled = restRemaining == 0 && !set.done, role = Role.Button) {
                        timer = WearStopwatchStore.save(context, timer.toggle())
                    }
                    .semantics {
                        contentDescription =
                            if (timer.startedAt > 0) "Stopper szüneteltetése"
                            else "Stopper indítása"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatSeconds(displayed),
                    color = White,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        val hint =
            when {
                restRemaining > 0 -> "Pihenő alatt"
                set.done -> "Rögzített idő"
                timer.startedAt > 0 -> "Ⅱ Koppints a szünethez"
                perSide ->
                    if (side == "leftSeconds") "▶ Bal oldal indítása" else "▶ Jobb oldal indítása"
                else -> "▶ Indítás · cél: ${workout.exercise?.targetReps?.ifBlank { "—" }} mp"
            }
        item { Label(hint, size = 8) }
    }
}

@Composable
private fun TimedSetToolsScreen(
    workout: WearWorkout,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val perSide = normalizeUnit(workout.exercise?.repUnit.orEmpty()) == "mp/oldal"
    var side by
        rememberSaveable(workout.workoutId, workout.currentExercise, set.number) {
            mutableStateOf(
                WearStopwatchStore.currentField(
                        context,
                        "${workout.workoutId}:${workout.exercise?.id}:${set.number}",
                    )
                    ?.takeIf { it == "leftSeconds" || it == "rightSeconds" }
                    ?: if (set.leftSeconds > 0 && set.rightSeconds == 0) "rightSeconds"
                    else "leftSeconds"
            )
        }
    val field = if (perSide) side else "reps"
    val key = "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"
    var timer by remember(key) { mutableStateOf(WearStopwatchStore.load(context, key)) }
    var elapsed by remember(key) { mutableLongStateOf(timer.elapsed()) }
    LaunchedEffect(key, timer) {
        while (true) {
            val now = timer.elapsed()
            if (now / 1000 != elapsed / 1000) elapsed = now
            delay(200)
        }
    }
    fun recordTime() {
        val measured = timer.elapsed()
        val seconds = if (measured > 0) ceil(measured / 1000.0) else fieldValue(set, field)
        onChange(field, seconds - fieldValue(workout.currentSet ?: set, field))
        timer = WearStopwatchStore.save(context, WearStopwatch(key))
        if (
            !perSide ||
                (side == "leftSeconds" && set.rightSeconds > 0) ||
                (side == "rightSeconds" && set.leftSeconds > 0)
        )
            onComplete()
        else {
            side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
            WearStopwatchStore.save(
                context,
                WearStopwatch("${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"),
            )
        }
        onBack()
    }
    RoundList(workout.exercise?.name.orEmpty()) {
        centerItem {
            Label(
                "Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}",
                Gold,
            )
        }
        if (perSide)
            centerItem {
                Label(
                    if (side == "leftSeconds") "Bal oldal · jobb: ${set.rightSeconds} mp"
                    else "Jobb oldal · bal: ${set.leftSeconds} mp"
                )
            }
        centerItem {
            Text(
                formatSeconds((elapsed / 1000).toInt()),
                color = White,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        centerItem {
            Label(
                workout.exercise?.targetReps?.let { if (it.isBlank()) "Stopper" else "Cél: $it mp" }
                    ?: "Stopper"
            )
        }
        centerItem {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoundControl(
                    if (timer.startedAt > 0) "Ⅱ" else "▶",
                    enabled = !set.done,
                    description =
                        if (timer.startedAt > 0) "Stopper szüneteltetése" else "Stopper indítása",
                ) {
                    timer = WearStopwatchStore.save(context, timer.toggle())
                }
                RoundControl(
                    if (perSide) "B/J" else "↺",
                    enabled = !set.done,
                    description = if (perSide) "Bal és jobb oldal váltása" else "Stopper nullázása",
                ) {
                    if (perSide) {
                        val measured = timer.elapsed()
                        if (measured > 0)
                            onChange(
                                field,
                                ceil(measured / 1000.0) -
                                    fieldValue(workout.currentSet ?: set, field),
                            )
                        timer = WearStopwatchStore.save(context, WearStopwatch(key))
                        side = if (side == "leftSeconds") "rightSeconds" else "leftSeconds"
                        WearStopwatchStore.save(
                            context,
                            WearStopwatch(
                                "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$side"
                            ),
                        )
                    } else timer = WearStopwatchStore.save(context, WearStopwatch(key))
                }
            }
        }

        centerItem {
            Pill(
                if (perSide) "✓ ${if (side == "leftSeconds") "Bal" else "Jobb"} idő rögzítése"
                else "✓ Idő rögzítése",
                enabled =
                    !set.done &&
                        (timer.startedAt > 0 ||
                            timer.elapsedMillis > 0 ||
                            elapsed > 0 ||
                            fieldValue(set, field) > 0),
                size = 12,
                onClick = ::recordTime,
            )
        }

        centerItem {
            Pill("± Kézi időbeállítás", tone = Secondary, enabled = !set.done, size = 11) {
                timer =
                    WearStopwatchStore.save(
                        context,
                        timer.copy(elapsedMillis = timer.elapsed(), startedAt = 0L),
                    )
                onEdit(field)
            }
        }
        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}

@Composable
private fun WorkoutMenu(
    workout: WearWorkout,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onEdit: (String) -> Unit,
    onTimerTools: () -> Unit,
    restRemaining: Int,
    onFinish: () -> Unit,
    onHome: () -> Unit,
) {
    RoundList("Edzésmenü") {
        centerItem { Pill("‹ Kezdőlap", tone = Secondary, onClick = onHome) }
        if (workout.exercise?.loadType != "bodyweight") {
            centerItem {
                Pill(
                    "Súly: ${workout.currentSet?.weight?.ifBlank { "0" } ?: "0"} kg",
                    tone = Secondary,
                    enabled = !workout.exerciseComplete,
                ) {
                    onEdit("weight")
                }
            }
        }
        if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp")) {
            centerItem {
                Pill(
                    "Stopper és oldalak",
                    tone = Secondary,
                    enabled = restRemaining == 0 && !workout.exerciseComplete,
                    onClick = onTimerTools,
                )
            }
        }
        centerItem {
            Pill(
                "Edzés befejezése",
                tone = Danger,
                enabled = workout.completedSets > 0,
                onClick = onFinish,
            )
        }
        if (workout.completedSets == 0) centerItem { Label("Előbb rögzíts egy sorozatot") }
        centerItem { Label("GYAKORLATOK", Gold) }
        workout.exercises.forEachIndexed { index, exercise ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            if (index == workout.currentExercise) Secondary else Card,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable(role = Role.Button) { onSelect(index) }
                        .padding(12.dp)
                ) {
                    Text(exercise.name, color = White, fontSize = 13.sp, maxLines = 2)
                    Text(
                        "${exercise.sets.count { it.done }}/${exercise.sets.size} sorozat",
                        color = if (index == workout.currentExercise) Gold else Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        centerItem {
            Pill(
                "‹ Előző",
                tone = Secondary,
                enabled = workout.currentExercise > 0,
                onClick = onPrevious,
            )
        }

        centerItem {
            Pill(
                "Következő ›",
                tone = Secondary,
                enabled = workout.currentExercise < workout.exercises.lastIndex,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun ConfirmationScreen(discard: Boolean, completed: Int, onAnswer: (Boolean) -> Unit) {
    ActionPanel(
        if (discard) "Félbehagyott edzés" else "Edzés befejezése",
        count = 2,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl(
                        "×",
                        caption = "Mégse",
                        description = "Vissza, az edzés megőrzése",
                    ) {
                        onAnswer(false)
                    }
                    RoundControl(
                        if (discard) "✕" else "✓",
                        if (discard) Danger else Gold,
                        caption = if (discard) "Törlés" else "Befejezés",
                        description =
                            if (discard) "Edzés törlése" else "Edzés befejezése és mentése",
                    ) {
                        onAnswer(true)
                    }
                }
            }
        },
    ) { compact ->
        centerItem {
            Title(
                if (discard) "Törlöd az edzést?" else "Befejezed az edzést?",
                if (compact) 14 else 17,
            )
        }

        centerItem {
            Text(
                if (discard) "A félbehagyott edzés elvész.\nA napló megmarad."
                else "$completed sorozat bekerül a naplóba.\nRészleges edzés is menthető.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SummaryScreen(
    result: WearClosure,
    health: JSONObject?,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val saved = result.status == "saved" || result.status == "discarded"
    ActionPanel(
        if (result.status == "error") "Szinkronizálás"
        else if (!saved) "Várakozás a telefonra"
        else if (result.action == "discardWorkout") "Edzés törölve" else "Edzés kész",
        count = if (result.status == "error") 2 else 1,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl(
                        if (saved) "✓" else "‹",
                        if (saved) Gold else Secondary,
                        caption = if (saved) "Kész" else "Kezdőlap",
                        onClick = onDone,
                    )
                    if (result.status == "error")
                        RoundControl(
                            "↻",
                            Gold,
                            caption = "Újrapróba",
                            description = "Mentés újrapróbálása",
                            onClick = onRetry,
                        )
                }
            }
        },
    ) { compact ->
        if (result.status == "error")
            centerItem { Title("Még nem menthető", if (compact) 14 else 19) }
        if (result.status == "error") {
            centerItem {
                Text(result.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else if (result.action == "finishWorkout") {
            centerItem {
                Text(
                    formatSeconds(result.durationSeconds),
                    color = White,
                    fontSize = 33.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            centerItem {
                Row(horizontalArrangement = Arrangement.spacedBy(19.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Title("${result.exerciseCount}", 22)
                        Label("gyakorlat")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Title("${result.completedSets}", 22)
                        Label("sorozat")
                    }
                }
            }
            centerItem {
                Label(
                    if (saved) "Mentve a telefonos naplóba"
                    else "Helyben megőrizve · mentés várakozik"
                )
            }
        } else
            centerItem {
                Label(
                    if (saved) "A félbehagyott edzés törölve"
                    else "A telefonos törlés még várakozik"
                )
            }
        if (result.action == "finishWorkout") {
            val metrics = wearMeasuredTotals(health, result.workoutId)
            if (metrics.isNotEmpty()) {
                centerItem { Label("ÓRÁN MÉRT ADATOK", Gold, size = 10) }
                for (pair in metrics.chunked(2)) {

                    centerItem {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (metric in pair) Column(
                                Modifier.weight(1f)
                                    .background(Card, RoundedCornerShape(12.dp))
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Label(metric.label, size = 9)
                                Title(metric.value, 14)
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                centerItem {
                    Text(
                        "Az összes kcal az alapanyagcserét is tartalmazza.",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (health?.optString("workoutId") == result.workoutId) {
                if (health.optString("state") in listOf("starting", "active", "ending")) {
                    centerItem {
                        Text(
                            "A mérés lezárása folyamatban; az utolsó adatok még frissülhetnek.",
                            color = Gold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (health.optBoolean("partial"))
                    centerItem {
                        Text(
                            "Részleges mérés",
                            color = Gold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
            }
        }
    }
}

private fun recommendedMeta(rec: WatchHomeRecommendation): String {
    val count = "${rec.day.exercises.size} gyakorlat"
    return try {
        if (rec.plannedStart.isBlank()) count
        else
            "$count · ${Instant.parse(rec.plannedStart).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
    } catch (_: Exception) {
        count
    }
}

internal fun normalizeUnit(unit: String): String = unit.replace("\\s+".toRegex(), "").lowercase()

internal fun formatSeconds(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun numberText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')

private fun fieldValue(set: WearSet, field: String): Double =
    when (field) {
        "weight" -> set.weight.toDoubleOrNull() ?: 0.0
        "leftSeconds" -> set.leftSeconds.toDouble()
        "rightSeconds" -> set.rightSeconds.toDouble()
        else -> set.reps.toDoubleOrNull() ?: 0.0
    }
