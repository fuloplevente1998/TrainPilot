package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
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

private val Black = Color(0xFF0E1015)
private val Card = Color(0xFF1A1E25)
private val Secondary = Color(0xFF232933)
private val White = Color(0xFFF6F8FB)
private val Muted = Color(0xFF9AA5B6)
private val Gold: Color
    @Composable get() = LocalWearHomeTheme.current.accentColor
private val Danger = Color(0xFF3B232A)
private val DangerText = Color(0xFFEFBCC3)
private val LocalAutomaticToggle = compositionLocalOf<(() -> Unit)?> { null }
private val LocalAutomaticCommand = compositionLocalOf<((String) -> Unit)?> { null }
internal val LocalSaveSync = compositionLocalOf<WearSaveSyncState?> { null }
private val LocalOpenSaveSync = compositionLocalOf<(() -> Unit)?> { null }
private val LocalUndo = compositionLocalOf<((String)->Unit)?> { null }
private val LocalTouchLock189 = compositionLocalOf<(() -> Unit)?> { null }
private val LocalEditorSave = compositionLocalOf<((String,Double,String)->Boolean)?> { null }

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
    onToggleGps: () -> Unit,
    phoneConnected: State<Boolean?>? = null,
    batteryLevel: State<Int?>? = null,
    onToggleAutomatic: (() -> Unit)? = null,
    onAutomaticCommand: ((String) -> Unit)? = null,
    onEditorSave: ((String,Double,String)->Boolean)? = null,
    onWorkoutFeedback: ((WatchWorkoutFeedback, String?) -> Unit)? = null,
    saveSync: State<WearSaveSyncState?>? = null,
    onRetrySync: ((String) -> Unit)? = null,
    onUndo: ((String) -> Unit)? = null,
    touchClock189: ()->Long = { android.os.SystemClock.uptimeMillis() },
) {
    var route by rememberSaveable { mutableStateOf("home") }
    val context = LocalContext.current
    var answeredWorkout by rememberSaveable { mutableStateOf("") }
    val localFeedback = closure.value?.takeIf { it.needsFeedback && it.status != "error" }?.let {
        WatchWorkoutFeedback.parse(JSONObject().put("workoutId", it.workoutId).put("started", it.started))
    }
    val pendingFeedback = (home.value?.pendingFeedback ?: localFeedback)?.takeUnless {
        it.workoutId == answeredWorkout || WearWorkoutFeedback.answered(context, it.workoutId)
    }
    LaunchedEffect(route) { if (route == "journal" || route == "metrics") onRefreshJournal() }
    LaunchedEffect(requestedRoute.value) {
        requestedRoute.value?.let {
            route = it
            onRouteConsumed()
        }
    }
    LaunchedEffect(pendingFeedback?.workoutId, workout.value?.workoutId, requestedRoute.value) {
        if (pendingFeedback != null && workout.value == null && onWorkoutFeedback != null) route = "feedback"
        else if (route == "feedback") route = "home"
    }
    LaunchedEffect(workout.value?.automation?.enabled, route) {
        if (workout.value?.automation?.enabled == true && route == "timer") route = "workout"
    }
    var editorField by rememberSaveable { mutableStateOf("reps") }
    var miniJournalId by rememberSaveable { mutableStateOf("") }
    var miniJournalBackRoute by rememberSaveable { mutableStateOf("journal") }
    var journalBackRoute by rememberSaveable { mutableStateOf("menu") }
    var syncBackRoute by rememberSaveable { mutableStateOf("home") }
    var metricsBackRoute by rememberSaveable { mutableStateOf("menu") }
    var settingsBackRoute by rememberSaveable { mutableStateOf("home") }
    var returnWorkoutPage by rememberSaveable { mutableStateOf(0) }
    val touchIdentity=WearTouchLock189.identity(workout.value)
    var touchLocked by remember(touchIdentity) { mutableStateOf(WearTouchLock189.locked(context,workout.value)) }
    val touchCopy=WearTouchCopy189.language(java.util.Locale.getDefault().language)
    LaunchedEffect(touchIdentity) { WearTouchLock189.retain(context,workout.value) }
    fun lockTouches() {
        val active=workout.value ?: return
        if(WearTouchLock189.set(context,active,true)) { returnWorkoutPage=0;route="workout";touchLocked=true }
    }
    BackHandler(enabled=touchLocked) { /* System Home/Power remain available; Back cannot open a hidden app action. */ }
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
    BackHandler(enabled = route != "home" && !touchLocked) {
        route =
            when (route) {
                "editor",
                "timer",
                "finish" -> "workout"
                "feedback" -> {
                    answeredWorkout = pendingFeedback?.workoutId.orEmpty()
                    pendingFeedback?.let { onWorkoutFeedback?.invoke(it, null) }
                    "home"
                }
                "save-sync" -> syncBackRoute
                "journal-detail" -> miniJournalBackRoute
                "journal" -> journalBackRoute
                "metrics" -> metricsBackRoute
                "settings" -> settingsBackRoute
                "discard" -> "home"
                else -> "home"
            }
    }
    CompositionLocalProvider(
        LocalWearHomeTheme provides (home.value?.theme ?: WearHomeTheme.DEFAULT),
        LocalAutomaticToggle provides onToggleAutomatic,
        LocalAutomaticCommand provides onAutomaticCommand,
        LocalEditorSave provides onEditorSave,
        LocalUndo provides onUndo,
        LocalTouchLock189 provides if(workout.value!=null)({lockTouches()})else null,
        LocalSaveSync provides saveSync?.value,
        LocalOpenSaveSync provides { syncBackRoute=route;route="save-sync" }
    ) {
        MaterialTheme {
            Box(Modifier.fillMaxSize().background(Black)) {
                val active = workout.value
                when {
                    touchLocked && active != null -> WearTouchLockedScreen189(active,restRemaining.value,health.value,touchCopy,onUnlock={
                        if(WearTouchLock189.set(context,active,false)){touchLocked=false;returnWorkoutPage=0;route="workout"}
                    },nowMillis=touchClock189)
                    route == "feedback" && pendingFeedback != null ->
                        WorkoutFeedbackScreen { effort ->
                            onWorkoutFeedback?.invoke(pendingFeedback, effort)
                            answeredWorkout = pendingFeedback.workoutId
                            route = "home"
                        }
                    route == "settings" ->
                        SettingsScreen(
                            measuring.value,
                            gpsEnabled.value,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            onMetrics = {
                                metricsBackRoute = "settings"
                                route = "metrics"
                            },
                            onOpenPhone = onOpenPhone,
                            backLabel = if (settingsBackRoute == "home") "‹ Kezdőlap" else "‹ Főmenü",
                            onBack = { route = settingsBackRoute },
                        )
                    route == "metrics" ->
                        HealthScreen(
                            health.value,
                            measuring.value,
                            gpsEnabled.value,
                            active != null,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            home.value,
                            onRefreshJournal,
                            onOpenJournal = { openJournal("metrics") },
                            onSelectMeasurement = { openMeasurement(it, "metrics") },
                            onBack = { route = metricsBackRoute },
                        )
                    route == "save-sync" -> SaveSyncScreen(saveSync?.value,
                        onRetry = { saveSync?.value?.let { onRetrySync?.invoke(it.workoutId) } },
                        onBack = { route=syncBackRoute })
                    route == "menu" ->
                        MainMenuScreen(active != null, active?.quickWorkout == true, onOpenPhone) {
                            if (it == "journal") openJournal("menu")
                            else {
                                if (it == "metrics") metricsBackRoute = "menu"
                                if (it == "settings") settingsBackRoute = "menu"
                                route = it
                            }
                        }
                    route == "journal" ->
                        MiniJournalScreen(
                            home.value,
                            onRefresh = onRefreshJournal,
                            onOpenPhone = onOpenPhone,
                            backLabel =
                                when (journalBackRoute) {
                                    "menu" -> "‹ Főmenü"
                                    "home" -> "‹ Kezdőlap"
                                    else -> "‹ Mérések"
                                },
                            onBack = { route = journalBackRoute },
                        ) { selected ->
                            miniJournalId = selected.workoutId
                            miniJournalBackRoute = "journal"
                            route = "journal-detail"
                        }
                    route == "journal-detail" ->
                        MiniJournalDetailScreen(
                            home.value?.recentWorkouts?.firstOrNull {
                                it.workoutId == miniJournalId
                            },
                            backLabel =
                                if (miniJournalBackRoute == "journal") "‹ Napló" else "‹ Mérések",
                            onBack = { route = miniJournalBackRoute },
                        )
                    route == "quick" ->
                        QuickWorkoutPicker(
                            home.value,
                            active,
                            active == null &&
                                home.value?.hasDraft == false &&
                                closure.value?.status !in listOf("pending", "error"),
                            onBack = { route = "menu" },
                        ) { exercise ->
                            if (active?.quickWorkout == true) onAddQuick(exercise)
                            else if (active == null) onStartQuick(exercise)
                            if (workout.value?.quickWorkout == true) route = "workout"
                        }
                    route == "calendar" ->
                        CalendarScreen(
                            home.value,
                            active == null &&
                                home.value?.hasDraft == false &&
                                closure.value?.status !in listOf("pending", "error"),
                            onBack = { route = "menu" },
                        ) { entry ->
                            entry.day?.let {
                                onStart(it, entry.scheduleId)
                                if (workout.value != null) route = "workout"
                            }
                        }
                    route == "summary" && closure.value != null ->
                        SummaryScreen(closure.value!!, health.value, onRetry) {
                            onDismissClosure()
                            route = "home"
                        }
                    route == "discard" && active != null ->
                        ConfirmationScreen(true, active.completedSets) {
                            if (it) {
                                onDiscard()
                                route = "summary"
                            } else route = "home"
                        }
                    route == "finish" && active != null ->
                        ConfirmationScreen(false, active.completedSets) {
                            if (it) {
                                onFinish()
                                route = if (onWorkoutFeedback != null) "feedback" else "summary"
                            } else route = "workout"
                        }
                    route == "editor" && active != null ->
                        ValueEditor(active, editorField, onChange) { route = "workout" }
                    route == "timer" && active != null ->
                        TimedSetToolsScreen(
                            active,
                            onChange,
                            onComplete,
                            onEdit = {
                                editorField = it
                                route = "editor"
                            },
                            onBack = { route = "workout" },
                        )
                    route == "days" && home.value != null ->
                        ProgramDays(home.value!!, onBack = { route = "home" }) { day ->
                            onStart(day, "")
                            if (workout.value != null) route = "workout"
                        }
                    route in listOf("workout","editor","timer") && active?.workoutPaused==true ->
                        RoundList("Edzés szünetel") {
                            centerItem { Text(active.exercise?.name.orEmpty(),color=White,fontSize=14.sp,
                                textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth()) }
                            centerItem { Text("A mérés és az időzítő szünetel.",color=Muted,fontSize=11.sp,textAlign=TextAlign.Center) }
                            centerItem { Pill("Folytatás telefonon",tone=Gold) { onOpenPhone() } }
                            centerItem { Pill("Kezdőlap",tone=Secondary) { route="home" } }
                        }
                    route == "workout" && active != null ->
                        WorkoutPager(
                            active,
                            restRemaining.value,
                            health.value,
                            measuring.value,
                            gpsEnabled.value,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            onChange,
                            onComplete,
                            onPrevious,
                            onNext,
                            onSelectExercise,
                            onSkipRest,
                            home = home.value,
                            onRefreshJournal = onRefreshJournal,
                            onOpenJournal = { openJournal("workout") },
                            onSelectMeasurement = { openMeasurement(it, "workout") },
                            initialPage = returnWorkoutPage,
                            onReturnPageConsumed = { returnWorkoutPage = 0 },
                            onEdit = {
                                editorField = it
                                route = "editor"
                            },
                            onTimerTools = { route = "timer" },
                            onFinish = { route = "finish" },
                            onHome = { route = "home" },
                        )
                    else ->
                        WearHomeDashboard(
                            home.value,
                            active,
                            closure.value,
                            health.value,
                            measuring.value,
                            phoneConnected?.value,
                            batteryLevel?.value,
                            onStart = { day, scheduleId ->
                                onStart(day, scheduleId)
                                if (workout.value != null) route = "workout"
                            },
                            onResume = {
                                if(active?.workoutPaused==true)onOpenPhone()else onResumeWorkout()
                                route = "workout"
                            },
                            onDays = { route = "days" },
                            onResult = { route = "summary" },
                            onJournal = { openJournal("home") },
                            onDiscard = { route = "discard" },
                            onSettings = {
                                settingsBackRoute = "home"
                                route = "settings"
                            },
                            onMenu = { route = "menu" },
                            onOpenPhone = onOpenPhone,
                        )
                }
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

/** Short pages keep their controls fixed; only content that actually overflows can scroll. */
@Composable
internal fun FixedRoundPanel(
    title: String,
    arc: Boolean = false,
    count: Int = 2,
    gap: Float = 7f,
    headerColor: Color = Gold,
    sweep: Float = WEAR_HEADER_SWEEP,
    onTitleClick: (() -> Unit)? = null,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-round-list")) {
        val diameter = minOf(maxWidth, maxHeight).value
        val layout = wearFixedPageLayout(maxWidth.value, maxHeight.value, arc, count, gap)
        val compact = layout.bodyHeight < 72f * LocalDensity.current.fontScale
        CurvedHeader(title, headerColor, sweep)
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = layout.bodyTop.dp)
                .width((diameter - 24f).dp)
                .height(layout.bodyHeight.dp)
                .testTag("wear-fixed-body"),
            contentAlignment = Alignment.Center,
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = layout.bodyHeight.dp)
                    .verticalScroll(scroll, enabled = scroll.maxValue > 0),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                content(compact)
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = layout.footerTop.dp)
                .width(layout.footerWidth.dp)
                .height(layout.footerHeight.dp)
                .testTag("wear-fixed-actions"),
            contentAlignment = Alignment.TopCenter,
        ) {
            footer()
        }
        if (onTitleClick != null)
            Box(
                Modifier.align(Alignment.TopCenter)
                    .width((diameter * .7f).dp)
                    .height(32.dp)
                    .clickable(role = Role.Button, onClick = onTitleClick)
                    .semantics { contentDescription = "Megnyitás telefonon" }
            )
    }
}

@Composable
private fun ActionPanel(
    title: String,
    count: Int = 2,
    gap: Float = 7f,
    actions: @Composable (WearActionLayout) -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearActionLayout(maxWidth.value, maxHeight.value, count, gap)
        FixedRoundPanel(
            title,
            count = count,
            gap = gap,
            footer = { actions(layout) },
            content = content,
        )
    }
}

@Composable
private fun Label(text: String, color: Color = Muted, size: Int = 11, lines: Int = 1) {
    Text(
        text,
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 2).sp,
        textAlign = TextAlign.Center,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Title(text: String, size: Int = 19, lines: Int = 2) {
    Text(text, color = White, fontSize = size.sp, lineHeight = (size + 2).sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Pill(text: String, modifier: Modifier = Modifier, tone: Color = Gold,
                 enabled: Boolean = true, height: Int = 48, size: Int = 14,
                 lines: Int = 2, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.35f) / fontScale
    val shape = if (lines == Int.MAX_VALUE) RoundedCornerShape(18.dp) else CircleShape
    Box(modifier.fillMaxWidth().heightIn(min = height.dp)
        .background(if (enabled) tone else Secondary, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = text }.padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (!enabled) Muted else if (tone == Gold) Black else if (tone == Danger) DangerText else White,
            fontSize = (size * captionScale).sp, lineHeight = ((size + 2) * captionScale).sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RoundList(title: String, content: LazyListScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-round-list")) {
        val state = rememberLazyListState()
        // No half-screen spacers or initial center offsets: opening starts at the first row.
        LazyColumn(
            Modifier.fillMaxSize().padding(top = 32.dp).hierarchicalFocusGroup(active = true),
            state = state,
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        CurvedHeader(title)
    }
}

/** Long pages scroll individual rows, without moving the fixed title. */
private fun LazyListScope.centerItem(content: @Composable ColumnScope.() -> Unit) {
    item {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** Status lives in the existing menus, leaving workout controls and timers intact. */
private fun LazyListScope.saveSyncItem() {
    centerItem {
        val status=LocalSaveSync.current
        val open=LocalOpenSaveSync.current
        if(status!=null && open!=null) {
            Text(wearSaveLabel(status.phase),color=if(status.phase==WearSavePhase.ERROR)DangerText else Gold,
                fontSize=11.sp,textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth().heightIn(min=44.dp).clickable(role=Role.Button,onClick=open)
                    .padding(horizontal=12.dp,vertical=8.dp).testTag("wear-save-status")
                    .semantics { contentDescription=wearSaveTitle()+": "+wearSaveLabel(status.phase) })
        }
    }
}

@Composable
internal fun SaveSyncScreen(state: WearSaveSyncState?,onRetry: ()->Unit,onBack: ()->Unit) {
    RoundList(wearSaveTitle()) {
        centerItem { Text(state?.let { wearSaveLabel(it.phase) } ?: wearSaveEmpty(),color=Gold,
            fontSize=14.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp)) }
        centerItem { Text(wearSaveDetail(state),color=Muted,fontSize=11.sp,textAlign=TextAlign.Center,
            modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp).testTag("wear-save-detail")) }
        if(state?.retryable==true)centerItem { Pill(wearSaveRetry(),tone=Secondary,onClick=onRetry) }
        centerItem { Pill(wearSaveBack(),tone=Secondary,onClick=onBack) }
    }
}

@Composable
internal fun MainMenuScreen(
    hasWorkout: Boolean,
    quickWorkout: Boolean,
    onOpenPhone: () -> Unit,
    onRoute: (String) -> Unit,
) {
    val status=LocalSaveSync.current
    RoundList("Főmenü") {
        if(status!=null)saveSyncItem()
        for ((label, destination) in
            listOf(
                "Kezdőlap" to "home",
                "Mini napló" to "journal",
                "Naptár" to "calendar",
                "Programnapok" to "days",
                "Órás mérések" to "metrics",
                "Beállítások" to "settings",
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

@Composable
private fun SettingsScreen(
    enabled: Boolean,
    gps: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    onMetrics: () -> Unit,
    onOpenPhone: () -> Unit,
    backLabel: String,
    onBack: () -> Unit,
) {
    RoundList("Beállítások") {
        centerItem { JournalText(if (enabled) "Edzésmérés bekapcsolva" else "Edzésmérés kikapcsolva", Gold, 12) }
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
            JournalText("A GPS a következő mozgásos edzés mérésétől használható.", size = 11)
        }
        centerItem { Pill("Órás mérések", tone = Card, onClick = onMetrics) }
        centerItem { JournalText("A színek a telefonon választott témát követik.", size = 11) }
        centerItem { Pill("Telefon megnyitása", tone = Secondary, onClick = onOpenPhone) }
        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
    }
}

/** Journal paragraphs keep the entire text and leave room for the curved screen edge. */
@Composable
private fun JournalText(
    text: String,
    color: Color = Muted,
    size: Int = 11,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(.8f),
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 2).sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        softWrap = true,
    )
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
        centerItem { JournalText("Mentett telefonos edzések · offline másolat", size = 10) }

        if (items.isEmpty()) {
            val message =
                when {
                    home == null -> "Várakozás a telefonos napló szinkronizálására."
                    !home.journalAvailable ->
                        "A telefon legutóbbi adatcsomagja még nem tartalmaz Mini naplót. Frissítsd és nyisd meg a telefonos TrainPilotot."
                    else -> "A telefonos naplóban még nincs átadható, befejezett edzés."
                }
            centerItem {
                JournalText(message, size = 12)
            }
        }
        if (home?.phoneVersion?.isNotBlank() == true) {
            centerItem { JournalText("Telefon: ${home.phoneVersion}", size = 10) }
        }
        items.forEach { item ->
            centerItem {
                Pill(
                    "${miniJournalDate(item.started)}\n${item.dayName}",
                    modifier = Modifier.fillMaxWidth(.84f).testTag("wear-journal-entry"),
                    tone = Card,
                    size = 12,
                    height = 47,
                    lines = Int.MAX_VALUE,
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

private fun LazyListScope.MiniJournalMetrics(
    title: String,
    metrics: MiniJournalMetric,
    watch: Boolean,
) {
    centerItem { JournalText(title, Gold, 12) }
    val avg = miniNumber(metrics.averageHeartRate, 1)
    val max = miniNumber(metrics.maxHeartRate)
    if (avg != null || max != null) {
        centerItem {
            JournalText(
                "Pulzus: ${avg ?: "—"} / ${max ?: "—"} bpm",
                color = White,
                size = 12,
            )
        }
    }
    if (watch) {
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem {
                JournalText(
                    "Összes mért energia: $value kcal",
                    color = White,
                    size = 12,
                )
            }
        }
        miniNumber(metrics.activeDurationSeconds)?.let { value ->
            centerItem { JournalText("Órás mérési idő: $value s", size = 11) }
        }
    } else {
        miniNumber(metrics.workoutCalories)?.let { value ->
            centerItem {
                JournalText(
                    "Edzéskalória: $value kcal",
                    color = White,
                    size = 12,
                )
            }
        }
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem { JournalText("Ablak összenergiája: $value kcal", size = 11) }
        }
        miniNumber(metrics.activeCalories)?.let { value ->
            centerItem { JournalText("Aktív energia: $value kcal", size = 11) }
        }
        miniNumber(metrics.exerciseMinutes)?.let { value ->
            centerItem { JournalText("Egészségapp edzésideje: $value perc", size = 11) }
        }
    }
    miniNumber(metrics.steps)?.let { centerItem { JournalText("Lépések: $it", size = 11) } }
    if (metrics.partial) centerItem { JournalText("Részleges órás mérés", Gold, 10) }
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
                JournalText(
                    "Ez az edzés már nincs a szinkronizált listában.",
                    size = 12,
                )
            }
        } else {
            centerItem { JournalText(item.dayName, White, 16, FontWeight.SemiBold) }
            centerItem { JournalText(miniJournalDate(item.started), size = 11) }
            centerItem {
                JournalText(
                    "${item.durationSeconds / 60} perc · ${item.exercises.size} gyakorlat",
                    size = 11,
                )
            }

            if (item.wear != null) {
                MiniJournalMetrics("TrainPilot óra · Health Services", item.wear, true)
            } else centerItem { JournalText("Nem érkezett órás mérési összesítés", size = 11) }
            if (item.healthConnect != null) {
                MiniJournalMetrics(
                    if (item.healthConnect.source == "samsung_health") "Samsung Health"
                    else "Health Connect",
                    item.healthConnect,
                    false,
                )

                centerItem { JournalText("A két mérés nem adódik össze", size = 10) }
            }

            centerItem { JournalText("Elvégzett gyakorlatok", Gold, 12) }
            if (item.exercises.isEmpty()) {
                centerItem { JournalText("Nincs rögzített sorozat", size = 11) }
            }
            item.exercises.forEach { exercise ->
                centerItem {
                    JournalText(
                        exercise.name,
                        color = White,
                        size = 13,
                        weight = FontWeight.SemiBold,
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
                        JournalText(
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
        // Put the actual reason beside the failure, before the empty metric rows.
        data?.optString("message")?.takeIf { it.isNotBlank() }?.let { message ->
            centerItem {
                Text(message, color = Gold, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp).testTag("wear-health-message"))
            }
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

            centerItem { Pill(wearRecordingRetryLabel(data), tone = Gold, onClick = onEnable) }
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
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier.fillMaxSize()
                    .hierarchicalFocusGroup(active = pager.currentPage == page)
                    .then(
                        if (pager.currentPage == page) Modifier
                        else Modifier.clearAndSetSemantics {}
                    )
            ) {
                if (page == 0) {
                    Box(Modifier.fillMaxSize()) {
                        if (restRemaining > 0) RestRing(workout, restRemaining)
                        val onMenu = {
                            scope.launch { pager.animateScrollToPage(1) }
                            Unit
                        }
                        if(workout.automation?.enabled==true && WearAutomaticEngine.timed(workout.exercise))
                            AutomaticTimedSetScreen(workout,restRemaining,health,onPrevious,onNext,onSkipRest,onFinish,onMenu)
                        else if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
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
            Modifier.fillMaxSize().padding(top = 6.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                Modifier.testTag("wear-page-indicator"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier.width(if (pager.currentPage == index) 12.dp else 4.dp)
                            .height(2.dp)
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
    val color = Gold
    val remainingFraction=wearRestProgress(workout,seconds)
    Canvas(Modifier.fillMaxSize().padding(8.dp).testTag("wear-rest-ring")
        .semantics { progressBarRangeInfo=ProgressBarRangeInfo(remainingFraction,0f..1f) }) {
        drawArc(
            color,
            -90f,
            360f * remainingFraction,
            false,
            style = Stroke(2.dp.toPx()),
        )
    }
}

@Composable
private fun WorkoutPanel(
    workout: WearWorkout,
    onMenu: () -> Unit,
    actions: @Composable (WearArcLayout) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearArcLayout(maxWidth.value, maxHeight.value)
        FixedRoundPanel(
            workout.exercise?.name.orEmpty(),
            arc = true,
            sweep = 140f,
            headerColor = White,
            footer = {
                Box(Modifier.width(layout.width.dp).height(layout.height.dp)) { actions(layout) }
            },
        ) {
            content()
        }
        Box(
            Modifier.align(Alignment.TopCenter)
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
private fun SetHeading(workout: WearWorkout, restRemaining: Int, detail: String? = null, compact: Boolean = false) {
    val completed = workout.exercise?.sets?.count { it.done } ?: 0
    val count = workout.exercise?.sets?.size ?: 0
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60000L - now % 60000L)
        }
    }
    val clock = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val automatic=workout.automation
    val command=LocalAutomaticCommand.current
    val toggle=LocalAutomaticToggle.current
    val touchLock=LocalTouchLock189.current
    val heading = when {
        automatic?.awaitingPhone==true -> "Telefonra vár"
        automatic?.enabled==true && automatic.interrupted -> "Megszakadt · folytatható"
        automatic?.enabled==true && automatic.paused -> "Szünet · ${formatSeconds((automatic.remaining(android.os.SystemClock.elapsedRealtime())/1000).toInt())}"
        automatic?.enabled==true && automatic.phase=="rest" -> "Pihenő ${formatSeconds(restRemaining)}"
        restRemaining > 0 -> "Pihenő ${formatSeconds(restRemaining)} · $completed/$count"
        workout.exerciseComplete -> "✓ $completed/$count sorozat rögzítve"
        else -> detail ?: "${workout.currentExercise + 1}/${workout.exercises.size} · Sorozat ${workout.currentSetIndex + 1}/$count"
    }
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(if(compact)1f else 1.35f) / fontScale
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Text(heading, modifier = Modifier.weight(1f, fill = false).then(if(automatic?.enabled==true&&automatic.counting&&command!=null)
            Modifier.clickable(role=Role.Button){command(if(automatic.paused)"resume" else "pause")}else Modifier), color = Gold,
            fontSize = (8 * captionScale).sp, lineHeight = (10 * captionScale).sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(" · ", color = Muted, fontSize = (7 * captionScale).sp,
            lineHeight = (10 * captionScale).sp, maxLines = 1)
        Text(clock, modifier = Modifier.testTag("wear-workout-clock")
            .semantics { contentDescription = "Pontos idő: $clock" },
            color = Muted, fontSize = (7 * captionScale).sp,
            lineHeight = (10 * captionScale).sp, maxLines = 1)
        if(toggle!=null) Text(if(automatic?.enabled==true)" A" else " M",color=if(automatic?.enabled==true)Gold else Muted,
            fontSize=(7*captionScale).sp,modifier=Modifier.size(if(compact)16.dp else 20.dp).clickable(role=Role.Button,onClick=toggle)
                .testTag("wear-automatic-toggle").semantics { contentDescription=if(automatic?.enabled==true)"Automata mód kikapcsolása" else "Automata mód bekapcsolása" })
        if(touchLock!=null)Box(Modifier.size(24.dp).clickable(role=Role.Button,onClick=touchLock).testTag("wear-touch-lock")
            .semantics { contentDescription=WearTouchCopy189.language(java.util.Locale.getDefault().language).lock },contentAlignment=Alignment.Center) {
            WearLockIcon189(Muted,Modifier.size(14.dp))
        }
    }
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
    val automatic=workout.automation
    val command=LocalAutomaticCommand.current
    val undo=LocalUndo.current
    var quickUndo by remember(workout.lastRecord188,workout.exercises) { mutableStateOf(WearUndo188.quick(workout,System.currentTimeMillis())) }
    LaunchedEffect(workout.lastRecord188,workout.exercises) {
        val age=System.currentTimeMillis()-(WearUndo188.record(workout)?.optLong("recordedAt") ?: 0L)
        if(quickUndo){delay((WearUndo188.QUICK_MILLIS-age).coerceAtLeast(0));quickUndo=false}
    }
    CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
        Box {
            RoundControl("‹", enabled = workout.currentExercise > 0, caption = "Előző", description = "Előző gyakorlat", onClick = onPrevious)
        }
        Box(Modifier.offset(x = layout.sideOffset.dp, y = (layout.centerTop).dp)) {
            when {
                quickUndo&&undo!=null&&WearUndo188.available(workout) -> RoundControl("↶",Gold,caption="Visszavonás",description="Utolsó sorozat visszavonása",onClick={WearUndo188.record(workout)?.optString("recordId")?.let(undo)})
                WearUndo188.blocked(workout) -> RoundControl("…",enabled=false,caption="Egyeztetés",description="Visszavonás egyeztetése",onClick={})
                automatic?.awaitingPhone==true -> RoundControl("…",enabled=false,caption="Telefon",description="Várakozás a telefonra",onClick={})
                automatic?.enabled==true&&automatic.paused&&command!=null -> RoundControl("▶",caption="Folytatás",description="Automata folytatása",onClick={command("resume")})
                restRemaining > 0 -> RoundControl("»", caption = "Kihagyás", description = "Pihenő kihagyása", onClick = onSkipRest)
                workout.exerciseComplete -> RoundControl("✓", enabled = false, caption = "Rögzítve", onClick = {})
                else -> RoundControl("✓", Gold, enabled = recordingEnabled, caption = recordLabel,
                    description = if (recordLabel == "Mentés") "Idő rögzítése" else "Sorozat rögzítése", onClick = onRecord)
            }
        }
        Box(Modifier.offset(x = (2 * layout.sideOffset).dp)) {
            if (workout.currentExercise == workout.exercises.lastIndex)
                RoundControl("✓", if (workout.exerciseComplete) Gold else Secondary,
                    enabled = workout.completedSets > 0 && !WearUndo188.blocked(workout), caption = "Befejezés", description = "Edzés befejezése", onClick = onFinish)
            else RoundControl("›", if (workout.exerciseComplete) Gold else Secondary,
                caption = "Következő", description = "Következő gyakorlat", onClick = onNext)
        }
    }
}

@Composable
private fun AutomaticTimedSetScreen(workout: WearWorkout,restRemaining: Int,health: JSONObject?,
    onPrevious: ()->Unit,onNext: ()->Unit,onSkipRest: ()->Unit,onFinish: ()->Unit,onMenu: ()->Unit) {
    val a=workout.automation ?: return
    val command=LocalAutomaticCommand.current
    val side=WearBilateral192.isSide(workout.exercise)
    val copy=WearSideCopy192.language(java.util.Locale.getDefault().language)
    val cfg=WearBilateral192.config(workout.exercise,a)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bodyHeight=wearFixedPageLayout(maxWidth.value,maxHeight.value,true).bodyHeight
        val tiny=bodyHeight<55f
        val fontScale=LocalDensity.current.fontScale
        val scale=fontScale.coerceAtMost(if(tiny)1f else 1.25f)/fontScale
        val rows=wearLiveWorkoutRows(health,workout.workoutId,System.currentTimeMillis())
        WorkoutPanel(workout,onMenu,actions={layout->WorkoutControls(workout,restRemaining,false,
            onRecord={},onPrevious=onPrevious,onNext=onNext,onSkipRest=onSkipRest,onFinish=onFinish,layout=layout)}) {
            // On the smallest circle, measurements share the heading instead of pushing
            // the actionable countdown below the fixed body. Full metrics remain on page 3.
            SetHeading(workout,restRemaining,detail=if(tiny)rows.firstOrNull() else null,compact=tiny)
            if(!tiny)rows.take(if(bodyHeight>=96f&&fontScale<1.5f)2 else 1).forEach {
                Text(it,color=Gold,fontSize=(9*scale).sp,lineHeight=(11*scale).sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            val waiting=a.phase=="ready"&&!a.interrupted
            val remaining=if(waiting&&side)cfg.work(cfg.first) else if(waiting)a.workSeconds else ((a.remaining(android.os.SystemClock.elapsedRealtime())+999)/1000).toInt()
            val currentSide=a.side192.ifBlank { cfg.first }
            val label=if(side)if(a.phase=="rest")if(a.restKind192=="side")copy.swap else copy.rest else copy.side(currentSide)+" · "+copy.work
                else if(a.phase=="rest")"Pihenő" else if(a.phase=="complete")"Rögzítve" else "Munka"
            val another=workout.exercises.withIndex().any { (e,ex)->ex.sets.withIndex().any { (s,set)->!set.done&&!WearBilateral192.skipped(set)&&(e>a.targetExercise||e==a.targetExercise&&s>a.targetSet) } }
            val nextSide=if(a.phase=="rest"||waiting)currentSide else if(currentSide==cfg.first)cfg.second() else if(a.round<a.rounds||another)cfg.first else ""
            val nextLabel=if(nextSide.isBlank())WearTouchCopy189.language(java.util.Locale.getDefault().language).saved else copy.side(nextSide)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("${if(a.paused||waiting)"▶" else "Ⅱ"} ${formatSeconds(remaining)}",color=White,
                    fontSize=((if(tiny)18 else 24)*scale).sp,lineHeight=((if(tiny)20 else 28)*scale).sp,
                    fontWeight=FontWeight.SemiBold,maxLines=1,modifier=Modifier.weight(1f).testTag("wear-automatic-clock")
                        .clickable(enabled=command!=null&&a.phase!="complete",role=Role.Button){command?.invoke(if(waiting)"start" else if(a.paused)"resume" else "pause")}
                        .semantics { contentDescription=if(waiting)"Automata munkaszakasz indítása" else if(a.paused)"Automata folytatása" else "Automata szüneteltetése" })
                Text(if(side)label+"\n→ "+nextLabel+if(a.rounds>1)" · ${if(waiting)1 else a.round}/${a.rounds}" else "" else label+if(a.rounds>1)" · ${if(waiting)1 else a.round}/${a.rounds}. kör" else "",color=Muted,
                    fontSize=((if(tiny)8 else 9)*scale).sp,lineHeight=((if(tiny)9 else 10)*scale).sp,maxLines=2,
                    textAlign=TextAlign.Center,modifier=Modifier.weight(1f).testTag("wear-side-phase192"))
            }
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
        run {
            SetHeading(workout, restRemaining)
            LiveWorkoutHealth(workout, health)
        }

        run {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (workout.exercise?.loadType != "bodyweight")
                    ValueTile(
                        set?.weight?.ifBlank { "0" } ?: "0",
                        "kg",
                        Modifier.weight(1f),
                        enabled = set?.done == false && (workout.automation?.counting!=true||workout.automation.paused),
                    ) {
                        onEdit("weight")
                    }
                ValueTile(
                    set?.reps?.ifBlank { "0" } ?: "0",
                    "ismétlés",
                    Modifier.weight(1f),
                    enabled = set?.done == false && (workout.automation?.counting!=true||workout.automation.paused),
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
internal fun ValueEditor(
    workout: WearWorkout,
    field: String,
    onChange: (String, Double) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    val expected=rememberSaveable(workout.workoutId,field){WearAutomaticEngine.targetId(workout,workout.currentExercise,workout.currentSetIndex)}
    val editable=expected==WearAutomaticEngine.targetId(workout,workout.currentExercise,workout.currentSetIndex)&&!set.done&&
        (workout.automation?.counting!=true||workout.automation.paused)
    val save=LocalEditorSave.current
    val original =
        remember(workout.workoutId, expected, field) {
            fieldValue(set, field)
        }
    var edited by
        rememberSaveable(workout.workoutId, expected, field) {
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
    ActionPanel(
        title,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl("×", caption = "Mégse", description = "Mégse", onClick = onBack)
                    RoundControl("✓", Gold, enabled=editable,caption = "Mentés", description = "✓ Mentés") {
                        if(save!=null&&!save(field,edited,expected))return@RoundControl
                        if (
                            field.endsWith("Seconds") ||
                                (field == "reps" &&
                                    normalizeUnit(workout.exercise?.repUnit.orEmpty())
                                        .startsWith("mp"))
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
                        if(save==null)onChange(field, edited - fieldValue(workout.currentSet ?: set, field))
                        onBack()
                    }
                }
            }
        },
    ) { compact ->
        val fontScale = LocalDensity.current.fontScale
        val series = if(editable)"Sorozat ${workout.currentSetIndex + 1}/${workout.exercise?.sets?.size ?: 0}"else "A sorozat közben változott"
        if (compact) Text(series, color = Muted, fontSize = (8f / fontScale).sp,
            lineHeight = (10f / fontScale).sp, textAlign = TextAlign.Center)
        else Label(series, size = 9)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundControl("−", enabled=editable,description = "$title csökkentése") {
                edited = (edited - step).coerceAtLeast(0.0)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val measurer = rememberTextMeasurer()
                    val valueSize = if (compact) 26f / fontScale else 26f
                    val valueLine = if (compact) 28f / fontScale else 28f
                    val style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = valueSize.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    val naturalWidth = measurer.measure(numberText(edited), style).size.width
                    val available = with(LocalDensity.current) { (maxWidth - 4.dp).toPx() }
                    val fit = (available / naturalWidth.coerceAtLeast(1)).coerceAtMost(1f)
                    Text(numberText(edited), style = style, color = White,
                        fontSize = (valueSize * fit).sp, lineHeight = (valueLine * fit).sp,
                        maxLines = 1, modifier = Modifier.testTag("wear-edited-value"))
                }
                val unit = if (field == "weight") "kg" else if (step == 5.0) "mp" else "ism."
                if (compact) Text(unit, color = Muted, fontSize = (9f / fontScale).sp,
                    lineHeight = (11f / fontScale).sp, textAlign = TextAlign.Center)
                else Label(unit, size = 9)
            }
            RoundControl("+", enabled=editable,description = "$title növelése") { edited += step }
        }
        if (!compact) Label("Lépésköz: ${numberText(step)}", size = 9)
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
        run {
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
        run {
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
                    "${if (set.done) "✓" else if (timer.startedAt > 0 || restRemaining > 0) "Ⅱ" else "▶"} ${formatSeconds(displayed)}",
                    color = White,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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

/** Keep side-setting touch targets away from the clipped edge of a round display. */
@Composable
private fun BilateralAdjustButton192(text: String,description: String,tag: String,enabled: Boolean=true,onClick: ()->Unit) {
    val scale=LocalDensity.current.fontScale
    Box(Modifier.size(44.dp).background(if(enabled)Secondary else Card,CircleShape)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).testTag(tag)
        .semantics { contentDescription=description },contentAlignment=Alignment.Center) {
        Text(text,color=if(enabled)White else Muted,fontSize=((if(text.length==1)22f else 11f)/scale).sp,lineHeight=((if(text.length==1)26f else 14f)/scale).sp,maxLines=1)
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
    val automaticToggle=LocalAutomaticToggle.current
    val automaticCommand=LocalAutomaticCommand.current
    val status=LocalSaveSync.current
    val undo=LocalUndo.current
    val touchLock=LocalTouchLock189.current
    RoundList("Edzésmenü") {
        touchLock?.let { lock -> centerItem { Pill(WearTouchCopy189.language(java.util.Locale.getDefault().language).lock,tone=Secondary,onClick=lock) } }
        if(undo!=null&&WearUndo188.available(workout))centerItem {
            Pill("Utolsó sorozat visszavonása",tone=Gold) { WearUndo188.record(workout)?.optString("recordId")?.let(undo) }
        }
        if(WearUndo188.marker(workout)?.optString("status")=="conflict")centerItem { Label("Eltérő módosítás. Ellenőrizd a telefonon.") }
        if(status!=null)saveSyncItem()
        centerItem { Pill("‹ Kezdőlap", tone = Secondary, onClick = onHome) }
        if(automaticToggle!=null) centerItem {
            Text("Automata mód: ${if(workout.automation?.enabled==true)"Be" else "Ki"}",color=Gold,fontSize=11.sp,
                modifier=Modifier.fillMaxWidth().heightIn(min=30.dp).clickable(role=Role.Button,onClick=automaticToggle)
                    .padding(6.dp).semantics { contentDescription="Automata mód kapcsoló" },textAlign=TextAlign.Center)
        }
        if(workout.automation?.enabled==true&&automaticCommand!=null) {
            val a=workout.automation
            val side=WearBilateral192.isSide(workout.exercise)
            val copy=WearSideCopy192.language(java.util.Locale.getDefault().language)
            val c=WearBilateral192.config(workout.exercise,a)
            val fields=if(side)listOf(Triple(copy.left,c.leftSeconds,"left"),Triple(copy.right,c.rightSeconds,"right"),Triple(copy.swap,c.sideRestSeconds,"sideRest"),Triple(copy.rest,c.setRestSeconds,"setRest"),Triple(copy.rounds,a.rounds,"rounds"))
                else listOf(Triple("Munka",a.workSeconds,"work"),Triple("Pihenő",a.restSeconds,"rest"),Triple("Kör",a.rounds,"rounds"))
            if(side)centerItem { Pill(copy.order+": "+copy.side(c.first),tone=Secondary,enabled=a.owner=="watch"&&!a.counting) { automaticCommand("sideFirst192") } }
            for((name,seconds,field) in fields)centerItem {
                val editable=if(side)a.owner=="watch"&&(!a.counting||a.paused&&field!="rounds") else !a.counting
                val suffix=if(side&&field!="rounds")"192" else ""
                if(side) {
                    Text("$name $seconds"+(if(field=="rounds")"" else " s"),color=Muted,
                        fontSize=(10*LocalDensity.current.fontScale.coerceAtMost(1.2f)/LocalDensity.current.fontScale).sp,
                        lineHeight=(12*LocalDensity.current.fontScale.coerceAtMost(1.2f)/LocalDensity.current.fontScale).sp,modifier=Modifier.fillMaxWidth(.8f).testTag("wear-side192-$field-value"),textAlign=TextAlign.Center,maxLines=2)
                    Row(Modifier.width(112.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                        BilateralAdjustButton192("−","$name −","wear-side192-$field-minus",editable){automaticCommand(field+"Minus"+suffix)}
                        BilateralAdjustButton192("+","$name +","wear-side192-$field-plus",editable){automaticCommand(field+"Plus"+suffix)}
                    }
                } else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("−",color=White,modifier=Modifier.size(30.dp).clickable(enabled=editable,role=Role.Button){automaticCommand(field+"Minus"+suffix)}.testTag("wear-side192-$field-minus").semantics { contentDescription="$name csökkentése" })
                    Text("$name $seconds"+(if(field=="rounds")"" else " s"),color=Muted,fontSize=(10*LocalDensity.current.fontScale.coerceAtMost(1.2f)/LocalDensity.current.fontScale).sp,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,maxLines=2)
                    Text("+",color=White,modifier=Modifier.size(30.dp).clickable(enabled=editable,role=Role.Button){automaticCommand(field+"Plus"+suffix)}.testTag("wear-side192-$field-plus").semantics { contentDescription="$name növelése" })
                }
            }
            if(side&&a.owner=="phone")centerItem { Label(when(java.util.Locale.getDefault().language){"en"->"Configure on the phone";"de"->"Am Telefon einstellen";"ro"->"Configurează pe telefon";else->"Beállítás a telefonon"}) }
            if(side&&a.phase=="rest")centerItem {
                Row(Modifier.width(112.dp),horizontalArrangement=Arrangement.SpaceEvenly){
                    BilateralAdjustButton192("−5 s","Pihenő rövidítése 5 másodperccel","wear-side192-active-rest-minus"){automaticCommand("restMinus192")}
                    BilateralAdjustButton192("+5 s","Pihenő növelése 5 másodperccel","wear-side192-active-rest-plus"){automaticCommand("restPlus192")}
                }
            }
            if(side&&a.phase=="work")centerItem { Pill(when(java.util.Locale.getDefault().language){"en"->"Skip this side";"de"->"Seite überspringen";"ro"->"Sari peste parte";else->"Oldal kihagyása"},tone=Secondary){automaticCommand("skipSide192")} }
            centerItem { Text("Rezgés: ${if(a.vibration)"Be" else "Ki"}",color=Muted,fontSize=10.sp,
                modifier=Modifier.fillMaxWidth().heightIn(min=30.dp).clickable(role=Role.Button){automaticCommand("vibration")}.padding(6.dp)) }
        }
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
        if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp")&&!(workout.automation?.enabled==true&&WearAutomaticEngine.timed(workout.exercise))) {
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
                enabled = workout.completedSets > 0 && !WearUndo188.blocked(workout),
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
        run {
            Title(
                if (discard) "Törlöd az edzést?" else "Befejezed az edzést?",
                if (compact) 14 else 17,
            )
        }

        run {
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
internal fun WorkoutFeedbackScreen(onAnswer: (String?) -> Unit) {
    RoundList("Milyen volt az edzés?") {
        WearWorkoutFeedback.choices.forEachIndexed { index, (value, label) ->
            centerItem { Pill("${index + 1} · $label", tone = Card) { onAnswer(value) } }
        }
        centerItem { Pill("Fájdalom / kellemetlenség", tone = Danger) { onAnswer("pain") } }
        centerItem { Pill("Most kihagyom", tone = Secondary) { onAnswer(null) } }
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
        if (result.status == "error") run { Title("Még nem menthető", if (compact) 14 else 19) }
        if (result.status == "error") {
            run {
                Text(result.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else if (result.action == "finishWorkout") {
            run {
                Text(
                    formatSeconds(result.durationSeconds),
                    color = White,
                    fontSize = 33.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            run {
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
            run {
                Label(
                    if (saved) "Mentve a telefonos naplóba"
                    else "Helyben megőrizve · mentés várakozik",
                    lines = 3,
                )
            }
        } else
            run {
                Label(
                    if (saved) "A félbehagyott edzés törölve"
                    else "A telefonos törlés még várakozik",
                    lines = 3,
                )
            }
        if (result.action == "finishWorkout") {
            val metrics = wearMeasuredTotals(health, result.workoutId)
            if (metrics.isNotEmpty()) {
                run { Label("ÓRÁN MÉRT ADATOK", Gold, size = 10) }
                for (pair in metrics.chunked(2)) {

                    run {
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
                run {
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
                    run {
                        Text(
                            "A mérés lezárása folyamatban; az utolsó adatok még frissülhetnek.",
                            color = Gold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (health.optBoolean("partial"))
                    run {
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
