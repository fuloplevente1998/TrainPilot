package com.repforge.app.ui.health
import android.app.Activity
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.FavoriteBorder
import com.repforge.app.ui.components.TrainPilotHeroSurface
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.components.TrainPilotSubtleSurface
import com.repforge.app.ui.components.TrainPilotMetricSurface
import com.repforge.app.ui.components.TrainPilotSectionHeader
import com.repforge.app.ui.components.trainPilotMetricIcon
import androidx.core.content.ContextCompat
import com.repforge.app.HealthConnectRepository
import androidx.compose.material3.TextButton

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.withResumed
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.R
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.WearLiveHealthStore
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.progress.WeightJournalOverview
import com.repforge.app.ui.progress.WeightJournalUiState
import com.repforge.app.ui.progress.WeightJournalViewModel
import androidx.compose.runtime.remember
import com.repforge.app.ui.theme.LocalTrainPilotColors


private fun backgroundReasonResource(reason: BackgroundBlock): Int = when (reason) {
    BackgroundBlock.READY -> com.repforge.app.R.string.health_background_ready
    BackgroundBlock.RESTORE -> com.repforge.app.R.string.health_background_restore
    BackgroundBlock.UNSUPPORTED -> com.repforge.app.R.string.health_background_unsupported
    BackgroundBlock.READ_PERMISSION -> com.repforge.app.R.string.health_background_read_permission
    BackgroundBlock.BACKGROUND_PERMISSION -> com.repforge.app.R.string.health_background_permission
    BackgroundBlock.WATCH_NOT_READY -> com.repforge.app.R.string.health_background_watch
    BackgroundBlock.BLUETOOTH_PERMISSION -> com.repforge.app.R.string.health_background_bluetooth_permission
    BackgroundBlock.NOTIFICATION_PERMISSION -> com.repforge.app.R.string.health_background_notification_permission
    BackgroundBlock.BLUETOOTH_OFF -> com.repforge.app.R.string.health_background_bluetooth_off
}

@Composable
fun HealthDashboardScreen(
    navController: NavController,
    onWeightJournal: () -> Unit,
    onWeightAdd: () -> Unit = onWeightJournal,
    onActiveWorkout: () -> Unit = {},
    initialSection: String? = null,
    onInitialSectionConsumed: () -> Unit = {},
    viewModel: HealthDashboardViewModel = viewModel(),
    weightViewModel: WeightJournalViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weightState by weightViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialSection) {
        if (initialSection == "devices") {
            viewModel.setTab("ble")
            onInitialSectionConsumed()
        }
    }
    val activeWorkout by ActiveWorkoutStore.snapshotFlow.collectAsStateWithLifecycle()
    val persistedWear = remember(context) { WearLiveHealthStore.current(context) }
    val liveWear by WearLiveHealthStore.liveHealthFlow.collectAsStateWithLifecycle()
    val wear = liveWear ?: persistedWear
    val colors = LocalTrainPilotColors.current
    val activity = context as? Activity
    val screenLifecycle = LocalLifecycleOwner.current
    DisposableEffect(screenLifecycle, activity, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.healthScreenVisible(activity)
                // Samsung's explicit permission dialog can pause the activity briefly.
                // Foreground SDK publication is independently guarded by RESUMED; do not
                // cancel the in-flight permission request until STOP or destination disposal.
                Lifecycle.Event.ON_STOP -> viewModel.healthScreenHidden(activity)
                else -> Unit
            }
        }
        screenLifecycle.lifecycle.addObserver(observer)
        if (screenLifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            viewModel.healthScreenVisible(activity)
        onDispose {
            screenLifecycle.lifecycle.removeObserver(observer)
            viewModel.healthScreenHidden(activity, force = true)
        }
    }
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    var detailType by rememberSaveable(state.selectedDay, state.historyChannel) { mutableStateOf<String?>(null) }
    LaunchedEffect(detailType) {
        viewModel.healthRecordDetailChanged(detailType != null)
    }
    var pendingFullSync by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val permissionScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val full = pendingFullSync
        pendingFullSync = null
        permissionScope.launch {
            screenLifecycle.lifecycle.withResumed {
                viewModel.healthScreenVisible(activity)
                if (full == null) viewModel.refresh(activity) else viewModel.syncHealth(activity, full)
            }
        }
    }
    fun startSync(full: Boolean) {
        val permissions = HealthConnectRepository.ALL_RUNTIME_PERMISSIONS
        if (state.provider == "health_connect" && Build.VERSION.SDK_INT >= 34 &&
            permissions.none { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            pendingFullSync = full
            permissionLauncher.launch(permissions)
        } else viewModel.syncHealth(activity, full)
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleHcBackground(true) else viewModel.refreshBackgroundStatus()
    }
    val bleBackgroundPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.isNotEmpty() && granted.values.all { it }) viewModel.toggleBleBackground(true)
        else viewModel.refreshBackgroundStatus()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        state.statusMessage?.let { Text(it, color = colors.muted, modifier = Modifier.padding(12.dp)) }
        if (state.backgroundWriteFailed) Text(
            com.repforge.app.ui.text.nativeResources(context, state.language)
                .getString(com.repforge.app.R.string.health_background_error),
            color = colors.dangerText, modifier = Modifier.testTag("health_background_error").padding(12.dp)
        )
        if (state.activeTab == "journal") {
            HealthJournalTabContent(
                state = state,
                weightState = weightState,
                onWeightJournal = onWeightJournal,
                onWeightAdd = onWeightAdd,
                onWeightRange = weightViewModel::setRange,
                onWeightRetry = weightViewModel::retryReads,
                onOpenBle = { viewModel.setTab("ble") },
                onSelectProvider = { viewModel.setProvider(it, activity) },
                onAutoOnOpen = viewModel::setAutoOnOpen,
                onToggleHcBackground = { enable ->
                    if (enable && state.hcBackgroundBlock == BackgroundBlock.BACKGROUND_PERMISSION)
                        backgroundPermissionLauncher.launch(HealthConnectRepository.BACKGROUND_READ_PERMISSION)
                    else viewModel.toggleHcBackground(enable)
                },
                onPreviousMonth = { viewModel.shiftHistoryMonth(-1) },
                onNextMonth = { viewModel.shiftHistoryMonth(1) },
                onDay = viewModel::selectHistoryDay,
                onChannel = viewModel::selectHistoryChannel,
                onToday = viewModel::historyToday,
                onOpenRecords = { detailType = it },
                onDailySource = viewModel::setDailySourcePreference,
                onSync = { startSync(false) },
                onFullSync = { startSync(true) },
                onCancelSync = viewModel::cancelHealthSync,
                onRetrySync = { startSync(state.syncRetryFull) },
                onRefresh = {
                    val permissions = HealthConnectRepository.READ_PERMS.map { "android.permission.health.$it" }.toTypedArray()
                    if (state.provider == "health_connect" && Build.VERSION.SDK_INT >= 34 &&
                        permissions.none { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                        permissionLauncher.launch(permissions)
                    } else viewModel.refresh(activity)
                }
            )
        } else {
            HealthConnectedDevicesHeader(
                language = state.language,
                onBack = { viewModel.setTab("journal") }
            )
            BleDiscoveryTabContent(
                state = state,
                bleRepo = viewModel.bleRepo,
                wearStateKnown = wear != null,
                wearStateUpdatedAt = wear?.optLong("updatedAt", 0L) ?: 0L,
                activeWorkout = activeWorkout?.optBoolean("active", false) == true,
                activeWorkoutTitle = activeWorkout?.optString("dayName").orEmpty()
                    .ifBlank { activeWorkout?.optString("programName").orEmpty() },
                timerOwner = activeWorkout?.optString("timerOwner", "phone").orEmpty(),
                onActiveWorkout = onActiveWorkout,
                onToggleBleBackground = { enable ->
                    if (enable && state.bleBackgroundBlock in setOf(BackgroundBlock.BLUETOOTH_PERMISSION, BackgroundBlock.NOTIFICATION_PERMISSION)) {
                        val permissions = buildList {
                            if (Build.VERSION.SDK_INT >= 31 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                                add(Manifest.permission.BLUETOOTH_CONNECT)
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                                add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (permissions.isNotEmpty()) bleBackgroundPermissionLauncher.launch(permissions.toTypedArray())
                        else viewModel.refreshBackgroundStatus()
                    } else viewModel.toggleBleBackground(enable)
                }
            )
        }
    }
    detailType?.let { type ->
        HealthRecordBrowser(state.selectedDay, state.historyChannel, type, state.language,
            onClose = { detailType = null })
    }
}

@Composable
fun HealthJournalTabContent(
    state: HealthDashboardUiState,
    weightState: WeightJournalUiState = WeightJournalUiState(),
    onWeightJournal: () -> Unit,
    onWeightAdd: () -> Unit = onWeightJournal,
    onWeightRange: (String) -> Unit = {},
    onWeightRetry: () -> Unit = {},
    onSelectProvider: (String) -> Unit,
    onToggleHcBackground: (Boolean) -> Unit,
    onAutoOnOpen: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDay: (String) -> Unit,
    onChannel: (String) -> Unit,
    onToday: () -> Unit,
    onOpenRecords: (String) -> Unit,
    onDailySource: (String) -> Unit,
    onOpenBle: () -> Unit = {},
    onSync: () -> Unit = {},
    onFullSync: () -> Unit = {},
    onCancelSync: () -> Unit = {},
    onRetrySync: () -> Unit = {}
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val metricColumns =
        if (LocalDensity.current.fontScale > 1.3f || LocalConfiguration.current.screenWidthDp < 312) 1 else 2
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    var allMetricsExpanded by rememberSaveable { mutableStateOf(false) }
    var journalExpanded by rememberSaveable { mutableStateOf(false) }
    var connectExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("health_journal_list").padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "health_selected_day_overview") {
            HealthSelectedDayOverview(
                state = state,
                weightState = weightState,
                onWeightJournal = onWeightJournal,
                onWeightAdd = onWeightAdd,
                onWeightRange = onWeightRange,
                onWeightRetry = onWeightRetry,
                onPulseTrend = { onOpenRecords("HeartRateRecord") }
            )
        }

        item(key = "health_more_section") {
            HealthStudioExpandableSection(
                title = strings.getString(R.string.studio_health_more),
                subtitle = strings.getString(R.string.studio_health_more_subtitle),
                iconKey = "pulse",
                tag = "health_more_section",
                expanded = moreExpanded,
                onToggle = { moreExpanded = !moreExpanded }
            ) {
                // Drive 13's supported compact readings first. Body temperature is
                // intentionally absent until a real provider/ingest field exists.
                for (row in state.additionalMetrics.chunked(metricColumns)) {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (metric in row) {
                            HealthMetricTile(metric, Modifier.weight(1f).fillMaxHeight())
                        }
                        if (row.size < metricColumns) Spacer(Modifier.weight(1f))
                    }
                }

                TextButton(
                    onClick = { allMetricsExpanded = !allMetricsExpanded },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("health_all_metrics_toggle")
                ) {
                    Text(
                        when (state.language) {
                            "en" -> if (allMetricsExpanded) "Show fewer readings" else "All readings"
                            "de" -> if (allMetricsExpanded) "Weniger Messwerte" else "Alle Messwerte"
                            "ro" -> if (allMetricsExpanded) "Mai puține măsurători" else "Toate măsurătorile"
                            else -> if (allMetricsExpanded) "Kevesebb mérés" else "Összes mérés"
                        },
                        color = colors.accentText
                    )
                }

                if (allMetricsExpanded) {
                    // Every original native metric and source/test tag stays reachable.
                    for (row in state.metrics.chunked(metricColumns)) {
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (metric in row) {
                                HealthMetricTile(metric, Modifier.weight(1f).fillMaxHeight())
                            }
                            if (row.size < metricColumns) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item(key = "health_history_section") {
            HealthStudioExpandableSection(
                title = strings.getString(R.string.health_history_title),
                subtitle = strings.getString(R.string.studio_health_journal_subtitle),
                iconKey = "pulse",
                tag = "health_history_section",
                expanded = journalExpanded,
                onToggle = { journalExpanded = !journalExpanded }
            ) {
                val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                state.recentDaySummaries.forEach { summary ->
                    TrainPilotSubtleSurface(
                        Modifier.fillMaxWidth().testTag("health_day_summary_${summary.day}")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                summary.dateText,
                                color = colors.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                summary.summaryText,
                                color = colors.muted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                            if (summary.partial) {
                                Text(
                                    text.getString(R.string.health_daily_source_partial),
                                    color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                TrainPilotHeroSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().testTag("health_history")) {
                        TrainPilotSectionHeader(
                            strings.getString(R.string.health_history_title),
                            icon = Icons.Default.FavoriteBorder
                        )
                        Text(state.historyMonth, color = colors.text, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onPreviousMonth,
                                modifier = Modifier.testTag("health_previous_month")
                            ) {
                                Text(text.getString(com.repforge.app.R.string.health_history_previous))
                            }
                            TextButton(
                                onClick = onNextMonth,
                                enabled = state.historyMonth < java.time.YearMonth.now().toString()
                            ) {
                                Text(text.getString(com.repforge.app.R.string.health_history_next))
                            }
                        }
                        TextButton(onClick = onToday) {
                            Text(text.getString(com.repforge.app.R.string.health_history_today))
                        }
                        Text(
                            text.getString(com.repforge.app.R.string.health_history_day, state.selectedDay),
                            color = colors.text,
                            modifier = Modifier.testTag("health_selected_day")
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("health_day_picker")
                        ) {
                            items(
                                (state.availableDays + state.selectedDay).distinct().sortedDescending(),
                                key = { it }
                            ) { day ->
                                androidx.compose.material3.FilterChip(
                                    selected = day == state.selectedDay,
                                    onClick = { onDay(day) },
                                    label = { Text(day) },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("health_day_$day")
                                )
                            }
                        }
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                listOf(
                                    "journal", "health_connect", "samsung_health", "legacy",
                                    com.repforge.app.HealthJournalStore.BLE
                                )
                            ) { channel ->
                                val label = when (channel) {
                                    "journal" -> text.getString(com.repforge.app.R.string.health_history_auto)
                                    "health_connect" -> "Health Connect"
                                    "samsung_health" -> "Samsung Health"
                                    com.repforge.app.HealthJournalStore.BLE -> "BLE"
                                    else -> text.getString(com.repforge.app.R.string.health_history_legacy)
                                }
                                androidx.compose.material3.FilterChip(
                                    selected = state.historyChannel == channel,
                                    onClick = { onChannel(channel) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        if (state.historyLoading) {
                            Text(
                                text.getString(com.repforge.app.R.string.health_history_loading),
                                color = colors.muted
                            )
                        }
                        if (state.historyError) {
                            Text(
                                text.getString(com.repforge.app.R.string.health_history_error),
                                color = colors.dangerText
                            )
                        }
                        TextButton(
                            onClick = { onOpenRecords("") },
                            enabled = !state.historyLoading,
                            modifier = Modifier.testTag("health_open_records")
                        ) {
                            Text(text.getString(com.repforge.app.R.string.health_records_title))
                        }
                    }
                }

                TrainPilotSubtleSurface(Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                                .getString(com.repforge.app.R.string.health_record_sleep),
                            color = colors.accent2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.sleepSessionsSummaryText,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { onOpenRecords("SleepSessionRecord") },
                            enabled = !state.historyLoading,
                            modifier = Modifier.testTag("health_open_sleep")
                        ) {
                            Text(
                                com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                                    .getString(com.repforge.app.R.string.health_sleep_details)
                            )
                        }
                    }
                }
            }
        }

        item(key = "health_connect_section") {
            HealthStudioExpandableSection(
                title = "Health Connect",
                subtitle = strings.getString(R.string.studio_health_connect_subtitle),
                iconKey = "recovery",
                tag = "health_connect_section",
                expanded = connectExpanded,
                onToggle = { connectExpanded = !connectExpanded }
            ) {
                TrainPilotSectionSurface(Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.getString(R.string.native_health_source),
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onRefresh,
                                enabled = !state.healthRefreshing,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = strings.getString(R.string.native_health_refresh),
                                    tint = colors.accent
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val narrowProviders = LocalConfiguration.current.screenWidthDp < 360 ||
                            LocalDensity.current.fontScale > 1.15f
                        if (narrowProviders) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProviderChoiceChip(
                                    "Health Connect",
                                    state.provider == "health_connect",
                                    { onSelectProvider("health_connect") },
                                    Modifier.fillMaxWidth().testTag("health_provider_hc")
                                )
                                ProviderChoiceChip(
                                    "Samsung Health",
                                    state.provider == "samsung_health",
                                    { onSelectProvider("samsung_health") },
                                    Modifier.fillMaxWidth().testTag("health_provider_samsung")
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProviderChoiceChip(
                                    "Health Connect",
                                    state.provider == "health_connect",
                                    { onSelectProvider("health_connect") },
                                    Modifier.weight(1f).testTag("health_provider_hc")
                                )
                                ProviderChoiceChip(
                                    "Samsung Health",
                                    state.provider == "samsung_health",
                                    { onSelectProvider("samsung_health") },
                                    Modifier.weight(1f).testTag("health_provider_samsung")
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            strings.getString(R.string.native_health_provider_help),
                            color = colors.muted,
                            fontSize = 11.sp
                        )
                        if (state.provider == com.repforge.app.HealthJournalStore.HC) {
                            val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                            Text(
                                text.getString(com.repforge.app.R.string.health_daily_source_title),
                                color = colors.text
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.testTag("health_daily_sources_row"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    listOf("auto", "priority") + state.dailySourceOptions,
                                    key = { it }
                                ) { choice ->
                                    androidx.compose.material3.FilterChip(
                                        selected = state.dailySourcePreference == choice,
                                        enabled = !state.sourceSaving && !state.historyLoading,
                                        onClick = { onDailySource(choice) },
                                        modifier = Modifier.testTag("health_daily_$choice"),
                                        label = {
                                            Text(
                                                when (choice) {
                                                    "auto" -> text.getString(com.repforge.app.R.string.health_daily_source_auto)
                                                    "priority" -> text.getString(com.repforge.app.R.string.health_daily_source_priority)
                                                    else -> choice
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                            if (state.dailySourcePreference !in
                                (listOf("auto", "priority") + state.dailySourceOptions)
                            ) {
                                Text(state.dailySourcePreference, color = colors.muted)
                            }
                            Text(
                                text.getString(com.repforge.app.R.string.health_daily_source_hint),
                                color = colors.muted
                            )
                            if (state.sourceSaveFailed) {
                                Text(
                                    text.getString(com.repforge.app.R.string.metadata_write_failed),
                                    color = colors.danger
                                )
                            }
                        }
                        if (state.historyPartial) {
                            Text(
                                com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                                    .getString(com.repforge.app.R.string.health_daily_source_partial),
                                color = colors.muted,
                                modifier = Modifier.testTag("health_daily_partial")
                            )
                        }
                    }
                }

                TrainPilotSubtleSurface(
                    Modifier.fillMaxWidth().testTag("health_auto_refresh_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.getString(R.string.native_health_auto_refresh_title),
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                strings.getString(R.string.native_health_auto_refresh_help),
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                            if (state.autoOnOpenSaveFailed) {
                                Text(
                                    strings.getString(R.string.native_health_auto_refresh_save_error),
                                    color = colors.dangerText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("health_auto_refresh_save_error")
                                )
                            }
                        }
                        Switch(
                            checked = state.autoOnOpen,
                            enabled = !state.autoOnOpenSaving && !state.historyLoading,
                            onCheckedChange = onAutoOnOpen,
                            modifier = Modifier
                                .testTag("health_auto_refresh_switch")
                                .semantics {
                                    contentDescription =
                                        strings.getString(R.string.native_health_auto_refresh_title)
                                },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.accent,
                                checkedTrackColor = colors.card
                            )
                        )
                    }
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                        Text(
                            strings.getString(R.string.native_health_sync_last, state.lastSyncAt),
                            color = colors.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.testTag("health_sync_last")
                        )
                        if (state.healthRefreshing) {
                            Text(
                                strings.getString(R.string.native_health_auto_refresh_running),
                                color = colors.muted,
                                modifier = Modifier.testTag("health_auto_refresh_running")
                            )
                            val progressText = when (state.syncStage) {
                                "CHANGES" ->
                                    strings.getString(R.string.native_health_sync_changes, state.syncPages)
                                "DAYS", "COMPLETE" ->
                                    strings.getString(
                                        R.string.native_health_sync_days,
                                        state.syncCompleted,
                                        state.syncTotal
                                    )
                                else -> strings.getString(R.string.native_health_sync_preparing)
                            }
                            Text(
                                progressText,
                                color = colors.muted,
                                modifier = Modifier.testTag("health_sync_progress")
                            )
                            TextButton(
                                onClick = onCancelSync,
                                modifier = Modifier.testTag("health_sync_cancel")
                            ) {
                                Text(strings.getString(R.string.native_health_sync_cancel))
                            }
                        } else {
                            TextButton(
                                onClick = onSync,
                                modifier = Modifier.testTag("health_sync_now")
                            ) {
                                Text(strings.getString(R.string.native_health_sync_now))
                            }
                            TextButton(
                                onClick = onFullSync,
                                modifier = Modifier.testTag("health_sync_full")
                            ) {
                                Text(strings.getString(R.string.native_health_sync_full))
                            }
                        }
                        if (state.syncFailed || state.syncPartial || state.syncCancelled) {
                            Text(
                                strings.getString(
                                    when {
                                        state.syncCancelled -> R.string.native_health_sync_cancelled
                                        state.syncPartial -> R.string.native_health_sync_partial
                                        else -> R.string.native_health_sync_failed
                                    }
                                ),
                                color = colors.dangerText,
                                modifier = Modifier.testTag("health_sync_failure")
                            )
                            if (!state.healthRefreshing) {
                                TextButton(
                                    onClick = onRetrySync,
                                    modifier = Modifier.testTag("health_sync_retry")
                                ) {
                                    Text(strings.getString(R.string.native_health_sync_retry))
                                }
                            }
                        }
                    }
                }

                TrainPilotSubtleSurface(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.getString(R.string.native_health_background_title),
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                strings.getString(R.string.native_health_background_help),
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                            val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                            Text(
                                text.getString(backgroundReasonResource(state.hcBackgroundBlock)),
                                color = if (state.hcBackgroundBlock == BackgroundBlock.READY)
                                    colors.muted else colors.dangerText,
                                fontSize = 11.sp,
                                modifier = Modifier.testTag("health_hc_background_status")
                            )
                            Text(
                                text.getString(
                                    if (state.hcBackgroundScheduled && state.hcBackground)
                                        com.repforge.app.R.string.health_background_scheduled
                                    else
                                        com.repforge.app.R.string.health_background_not_scheduled
                                ),
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = state.hcBackground,
                            enabled = !state.backgroundSaving,
                            modifier = Modifier.testTag("health_hc_background_switch"),
                            onCheckedChange = onToggleHcBackground,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.accent,
                                checkedTrackColor = colors.card
                            )
                        )
                    }
                }
            }
        }

        item(key = "health_watch_sync") {
            HealthStudioActionRow(
                title = strings.getString(R.string.studio_health_watch_sync),
                subtitle = strings.getString(R.string.studio_health_watch_sync_subtitle),
                iconKey = "workouts",
                tag = "health_open_watch_sync_card",
                onClick = onOpenBle
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

/** Compact Studio header while the original full metric grid / provider controls remain below. */
@Composable
internal fun HealthSelectedDayOverview(
    state: HealthDashboardUiState,
    weightState: WeightJournalUiState,
    onWeightJournal: () -> Unit,
    onWeightAdd: () -> Unit,
    onWeightRange: (String) -> Unit,
    onWeightRetry: () -> Unit,
    onPulseTrend: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val ctx = LocalContext.current
    val strings = remember(ctx, state.language) { nativeResources(ctx, state.language) }
    val metrics = state.overviewMetrics
    var weightExpanded by rememberSaveable { mutableStateOf(false) }
    val bodyFitnessMetrics = state.metrics.filter {
        it.key in setOf("bodyFatPercent", "oxygenSaturationPercent", "vo2Max")
    }
    val fontScale = LocalDensity.current.fontScale
    val width = LocalConfiguration.current.screenWidthDp
    val columns = when {
        width < 312 || fontScale > 1.3f -> 1
        width < 350 || fontScale > 1.2f -> 2
        else -> 3
    }
    TrainPilotHeroSurface(Modifier.fillMaxWidth().testTag("health_selected_day_overview")) {
        val selectedIsToday = state.selectedDay == java.time.LocalDate.now().toString()
        TrainPilotSectionHeader(
            strings.getString(
                if (selectedIsToday) R.string.studio_health_today_status
                else R.string.studio_health_day_status,
                runCatching {
                    java.time.LocalDate.parse(state.selectedDay).format(
                        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                            .withLocale(strings.configuration.locales[0]))
                }.getOrDefault(state.selectedDay)
            ),
            icon = Icons.Default.FavoriteBorder
        )
        Text(
            strings.getString(R.string.studio_health_overview_subtitle),
            color = colors.muted,
            style = MaterialTheme.typography.bodySmall
        )
        for (chunk in metrics.chunked(columns)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (metric in chunk) {
                    TrainPilotMetricSurface(Modifier.weight(1f).fillMaxHeight()
                        .testTag("health_snapshot_${metric.key}")) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(trainPilotMetricIcon(metric.key), contentDescription = null,
                                modifier = Modifier.size(18.dp), tint = colors.accentText)
                            Text(metric.label, color = colors.muted,
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        }
                        Text(buildString {
                            append(metric.valueText)
                            if (metric.unit.isNotBlank() && metric.isAvailable) {
                                append(' ')
                                append(metric.unit)
                            }
                        }, color = if (metric.isAvailable) colors.text else colors.muted,
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"))
                        if (metric.key == "recovery") {
                            Text(metric.sourceText, color = colors.muted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag("health_recovery_source"))
                        }
                    }
                }
                repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (state.historyPartial) {
            Text(strings.getString(R.string.health_daily_source_partial),
                color = colors.muted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("health_overview_partial"))
        }
        HealthPulseTrendRow(
            title = strings.getString(R.string.health_record_heart_rate),
            subtitle = when (state.language) {
                "en" -> "Last 7 calendar days • daily average heart rate"
                "de" -> "Letzte 7 Kalendertage • täglicher Durchschnittspuls"
                "ro" -> "Ultimele 7 zile calendaristice • puls mediu zilnic"
                else -> "Elmúlt 7 naptári nap • napi átlagpulzus"
            },
            points = state.pulseTrend,
            onClick = onPulseTrend
        )
        HealthStudioActionRow(
            title = strings.getString(R.string.weight_open),
            subtitle = buildString {
                append(
                    weightState.currentKg?.let {
                        java.text.NumberFormat.getNumberInstance(
                            java.util.Locale.forLanguageTag(state.language)
                        ).apply { maximumFractionDigits = 1 }.format(it) + " kg"
                    } ?: "—"
                )
                append(" · ")
                append(
                    when (state.language) {
                        "en" -> if (weightExpanded) "Hide details" else "Measurement, change and trend"
                        "de" -> if (weightExpanded) "Details ausblenden" else "Messung, Änderung und Trend"
                        "ro" -> if (weightExpanded) "Ascunde detaliile" else "Măsurare, schimbare și tendință"
                        else -> if (weightExpanded) "Részletek elrejtése" else "Mérés, változás és trend"
                    }
                )
            },
            iconKey = "weightKg",
            tag = "health_weight_journal",
            onClick = { weightExpanded = !weightExpanded }
        )
        if (weightExpanded) {
            Column(
                Modifier.fillMaxWidth().testTag("health_weight_inline_panel"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bodyFitnessMetrics.isNotEmpty()) {
                    Text(
                        when (state.language) {
                            "en" -> "Body & fitness"
                            "de" -> "Körper & Fitness"
                            "ro" -> "Corp și fitness"
                            else -> "Test és fittség"
                        },
                        color = colors.accentText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (state.language) {
                            "en" -> "Body fat, SpO₂ and VO₂max from the selected Health day"
                            "de" -> "Körperfett, SpO₂ und VO₂max des gewählten Gesundheitstags"
                            "ro" -> "Grăsime corporală, SpO₂ și VO₂max din ziua selectată"
                            else -> "Testzsír, SpO₂ és VO₂max a kiválasztott egészségnapból"
                        },
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    val fitnessColumns =
                        if (LocalConfiguration.current.screenWidthDp < 350 ||
                            LocalDensity.current.fontScale > 1.2f) 1 else 3
                    for (chunk in bodyFitnessMetrics.chunked(fitnessColumns)) {
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { metric ->
                                HealthMetricTile(
                                    metric,
                                    Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            repeat(fitnessColumns - chunk.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                WeightJournalOverview(
                    state = weightState,
                    onRange = onWeightRange,
                    onAction = onWeightAdd,
                    actionLabel = R.string.weight_add,
                    onRetry = onWeightRetry
                )
            }
        }
    }
}

@Composable
private fun HealthPulseTrendRow(
    title: String,
    subtitle: String,
    points: List<HealthPulseTrendPointUiModel>,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSubtleSurface(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .testTag("health_pulse_trend")
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(trainPilotMetricIcon("pulse"), contentDescription = null,
                modifier = Modifier.size(20.dp), tint = colors.accentText)
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            HealthPulseSparkline(points)
            Text("›", color = colors.accentText, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HealthPulseSparkline(points: List<HealthPulseTrendPointUiModel>) {
    val colors = LocalTrainPilotColors.current
    val available = points.mapIndexedNotNull { index, point ->
        point.bpm?.takeIf { it.isFinite() && it > 0.0 }?.let { index to it }
    }
    if (available.size < 2) {
        Text("—", color = colors.muted, modifier = Modifier.testTag("health_pulse_sparkline"))
        return
    }
    val min = available.minOf { it.second }
    val max = available.maxOf { it.second }
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
    androidx.compose.foundation.Canvas(
        Modifier.width(96.dp).height(36.dp).testTag("health_pulse_sparkline")
    ) {
        val denominator = (points.size - 1).coerceAtLeast(1)
        fun x(index: Int) = size.width * index / denominator.toFloat()
        fun y(value: Double) = size.height -
            ((value - min) / range).toFloat() * size.height
        available.zipWithNext().forEach { (a, b) ->
            if (b.first == a.first + 1) {
                drawLine(
                    color = colors.accent,
                    start = androidx.compose.ui.geometry.Offset(x(a.first), y(a.second)),
                    end = androidx.compose.ui.geometry.Offset(x(b.first), y(b.second)),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        available.forEach { (index, value) ->
            drawCircle(
                color = colors.accent,
                radius = 2.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x(index), y(value))
            )
        }
    }
}

@Composable
private fun HealthStudioActionRow(
    title: String, subtitle: String, iconKey: String, tag: String, onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSubtleSurface(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(trainPilotMetricIcon(iconKey), contentDescription = null,
                modifier = Modifier.size(20.dp), tint = colors.accentText)
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = colors.muted, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
            Text("›", color = colors.accentText, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun HealthMetricTile(m: HealthMetricItemUiModel, modifier: Modifier = Modifier) {
    val colors = LocalTrainPilotColors.current
    TrainPilotMetricSurface(modifier.testTag("health_metric_${m.key}")) {
        Icon(trainPilotMetricIcon(m.key), null, Modifier.size(20.dp), tint = colors.accentText)
        Text(m.label, color = colors.muted, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(m.valueText, Modifier.weight(1f, fill = false),
                color = if (m.isAvailable) colors.text else colors.muted,
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"))
            if (m.unit.isNotBlank() && m.isAvailable)
                Text(m.unit, color = colors.muted, style = MaterialTheme.typography.labelSmall)
        }
        Text(m.sourceText, color = colors.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HealthStudioExpandableSection(
    title: String,
    subtitle: String,
    iconKey: String,
    tag: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = colors.card,
        modifier = Modifier.fillMaxWidth()
            .border(
                1.dp,
                if (expanded) colors.accent.copy(alpha = .6f) else colors.line,
                shape
            )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(tag)
                    .clickable(
                        role = androidx.compose.ui.semantics.Role.Button,
                        onClick = onToggle
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.accent.copy(alpha = .14f),
                    modifier = Modifier.size(44.dp)
                        .border(
                            1.dp,
                            colors.accent.copy(alpha = .35f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            trainPilotMetricIcon(iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.accentText
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = colors.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            color = colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.accentText
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.line)
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HealthConnectedDevicesHeader(
    language: String,
    onBack: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    TrainPilotSubtleSurface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("health_ble_subscreen_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.testTag("health_ble_back")
            ) {
                Text("‹", style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    strings.getString(R.string.studio_health_connected_devices),
                    color = colors.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    strings.getString(R.string.studio_health_connected_devices_subtitle),
                    color = colors.muted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun HealthOwnerStatusPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalTrainPilotColors.current
    Surface(
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.accent.copy(alpha = .18f) else colors.card2,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) colors.accent else colors.line
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text,
                color = if (selected) colors.accentText else colors.text,
                style = MaterialTheme.typography.labelLarge
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthSyncSummaryCard(
    title: String,
    subtitle: String,
    selectedLabel: String,
    active: Boolean,
    selected: Boolean,
    tag: String
) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSubtleSurface(Modifier.fillMaxWidth().testTag(tag)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (active) colors.accent.copy(alpha = .12f) else colors.card
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (active) colors.accentText else colors.muted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    color = if (active) colors.success else colors.muted,
                    style = MaterialTheme.typography.bodySmall
                )
                if (selected) {
                    Text(
                        selectedLabel,
                        color = colors.accentText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text("›", color = colors.muted, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun BleDiscoveryTabContent(
    state: HealthDashboardUiState,
    bleRepo: com.repforge.app.BleDiscoveryRepository,
    wearStateKnown: Boolean = false,
    wearStateUpdatedAt: Long = 0L,
    activeWorkout: Boolean = false,
    activeWorkoutTitle: String = "",
    timerOwner: String = "phone",
    onActiveWorkout: () -> Unit = {},
    onToggleBleBackground: (Boolean) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val ble = state.bleState
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    var wearClockMs by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(wearStateUpdatedAt, activeWorkout) {
        while (true) {
            wearClockMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(5_000L)
        }
    }
    val wearFresh = wearStateKnown && wearStateUpdatedAt > 0L &&
        kotlin.math.abs(wearClockMs - wearStateUpdatedAt) <= 15_000L
    val wearLive = activeWorkout && wearFresh

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("health_ble_list").padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text(
                strings.getString(R.string.studio_health_watch_connection),
                color = colors.text,
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            TrainPilotHeroSurface(
                Modifier.fillMaxWidth().testTag("health_connected_wear_card")
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        Modifier.size(52.dp),
                        shape = CircleShape,
                        color = colors.card2,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, if (wearLive) colors.accent else colors.line
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (wearLive) colors.accentText else colors.muted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.getString(R.string.native_home_wear_watch),
                            color = colors.text,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            strings.getString(
                                when {
                                    wearLive -> R.string.studio_health_wear_connected
                                    wearStateKnown -> R.string.studio_health_wear_last_seen
                                    else -> R.string.studio_health_wear_not_connected
                                }
                            ),
                            color = if (wearLive) colors.success else colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("health_connected_wear_status")
                        )
                    }
                }
            }
        }

        item {
            TrainPilotSectionSurface(
                Modifier.fillMaxWidth().testTag("health_connected_active_workout")
            ) {
                TrainPilotSectionHeader(
                    strings.getString(R.string.studio_health_active_workout_control),
                    icon = Icons.Default.Sync
                )
                if (activeWorkout) {
                    if (activeWorkoutTitle.isNotBlank()) {
                        Text(
                            activeWorkoutTitle,
                            color = colors.text,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthOwnerStatusPill(
                            strings.getString(R.string.studio_health_phone_controls),
                            selected = timerOwner != "watch",
                            Modifier.weight(1f)
                        )
                        HealthOwnerStatusPill(
                            strings.getString(R.string.studio_health_watch_controls),
                            selected = timerOwner == "watch",
                            Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = onActiveWorkout,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("health_connected_open_workout"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.getString(R.string.studio_health_open_active_workout))
                    }
                } else {
                    Text(
                        strings.getString(R.string.studio_health_no_active_workout),
                        color = colors.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                strings.getString(R.string.studio_health_sync_sources),
                color = colors.text,
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            HealthSyncSummaryCard(
                title = "Health Connect",
                subtitle = when {
                    !state.hcSupported -> strings.getString(R.string.studio_health_source_unsupported)
                    state.hcGranted -> strings.getString(R.string.studio_health_source_ready)
                    else -> strings.getString(R.string.studio_health_source_permission_needed)
                },
                selectedLabel = strings.getString(R.string.studio_health_selected_source),
                active = state.hcSupported && state.hcGranted,
                selected = state.provider == "health_connect",
                tag = "health_connected_hc"
            )
        }

        item {
            HealthSyncSummaryCard(
                title = "Samsung Health",
                subtitle = when {
                    !state.samsungInstalled -> strings.getString(R.string.studio_health_source_not_installed)
                    state.samsungAvailable -> strings.getString(R.string.studio_health_source_available)
                    else -> strings.getString(R.string.studio_health_source_unavailable)
                },
                selectedLabel = strings.getString(R.string.studio_health_selected_source),
                active = state.samsungAvailable,
                selected = state.provider == "samsung_health",
                tag = "health_connected_samsung"
            )
        }

        item {
            Text(
                strings.getString(R.string.studio_health_ble_tools),
                color = colors.text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp).testTag("health_connected_ble_tools")
            )
            Text(
                strings.getString(R.string.studio_health_ble_tools_subtitle),
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Existing RDFit/BLE tools stay fully native and functional below the product summary.
        // #111 Keresés vezérlő panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.getString(R.string.native_health_ble_search), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(strings.getString(R.string.native_health_ble_state, ble.state, ble.code), color = colors.muted, fontSize = 11.sp)
                        }
                        if (ble.state == "scanning") {
                            Button(
                                onClick = { bleRepo.stopScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                            ) {
                                Text(strings.getString(R.string.native_health_ble_stop), color = colors.dangerText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { bleRepo.startScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = colors.onAccent)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.getString(R.string.native_health_ble_scan), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (ble.state == "scanning") {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ble.scanRemainingSeconds / 60f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = colors.accentText,
                            trackColor = colors.card2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(strings.getString(R.string.native_health_ble_remaining, ble.scanRemainingSeconds), color = colors.muted, fontSize = 10.sp)
                    }
                }
            }
        }

        // Élő BPM & RDFit Teszt panel (ha csatlakoztatva van)
        if (ble.state in listOf("connected", "probing", "monitoring")) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.card2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ble.deviceName.ifBlank { strings.getString(R.string.native_health_ble_connected_watch) }, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { bleRepo.disconnect() }) {
                                Text(strings.getString(R.string.native_health_ble_disconnect), color = colors.dangerText, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 0x2A37 Élő Pulzus
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(strings.getString(R.string.native_health_ble_heart_rate), color = colors.muted, fontSize = 10.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (ble.liveHeartRateBpm != null) "${ble.liveHeartRateBpm} bpm" else "—", color = Color(0xFFEF5350), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    if (ble.heartSupported && ble.state != "monitoring") {
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(onClick = { bleRepo.startHeartRate() }) {
                                            Text(strings.getString(R.string.native_health_ble_listen), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            // RDFit Lépés & Akkumulátor
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = colors.card)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(strings.getString(R.string.native_health_ble_rd_fit), color = colors.muted, fontSize = 10.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(if (ble.rdfitProbe.stepsCount != null) strings.getString(R.string.native_health_ble_steps_count, ble.rdfitProbe.stepsCount) else if (ble.rdfitProbe.batteryPercent != null) "${ble.rdfitProbe.batteryPercent}%" else "—", color = colors.accentText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (ble.probeSupported && ble.state != "probing") {
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(onClick = { bleRepo.readRdfitData() }) {
                                            Text(strings.getString(R.string.native_health_ble_request), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { bleRepo.rememberWatch() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text(strings.getString(R.string.native_health_ble_remember), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (ble.rememberedWatchPresent) {
                                Button(
                                    onClick = { bleRepo.forgetWatch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                                ) {
                                    Text(strings.getString(R.string.native_health_ble_forget), color = colors.dangerText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Háttérkapcsolat (5 percenkénti lépéslekérés)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.getString(R.string.native_health_ble_background), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(strings.getString(R.string.native_health_ble_background_help), color = colors.muted, fontSize = 11.sp)
                        val text = com.repforge.app.ui.text.nativeResources(LocalContext.current, state.language)
                        Text(text.getString(backgroundReasonResource(state.bleBackgroundBlock)),
                            color = if (state.bleBackgroundBlock == BackgroundBlock.READY) colors.muted else colors.dangerText,
                            fontSize = 11.sp, modifier = Modifier.testTag("health_ble_background_status"))
                        Text(text.getString(if (state.bleBackgroundRunning && state.bleBackground) com.repforge.app.R.string.health_background_connected
                            else com.repforge.app.R.string.health_background_not_connected), color = colors.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.bleBackground,
                        enabled = !state.backgroundSaving,
                        modifier = Modifier.testTag("health_ble_background_switch"),
                        onCheckedChange = onToggleBleBackground,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.card)
                    )
                }
            }
        }

        // Talált eszközök listája (#111 .tp111-ble)
        item {
            Text(strings.getString(R.string.native_health_ble_found, ble.devices.size), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        items(ble.devices, key = { it.id }) { dev ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { bleRepo.connect(dev.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (dev.remembered) colors.accent else colors.line)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dev.name.ifBlank { strings.getString(R.string.native_health_ble_unnamed_device) }, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(strings.getString(R.string.native_health_ble_rssi, dev.displayAddress, dev.rssi), color = colors.muted, fontSize = 11.sp)
                        if (dev.remembered) {
                            Text(strings.getString(R.string.native_health_ble_saved), color = colors.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { bleRepo.connect(dev.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.card2)
                    ) {
                        Text(strings.getString(R.string.native_health_ble_connect), color = colors.accentText, fontSize = 11.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun ProviderChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = LocalTrainPilotColors.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.accent else colors.card2)
            .border(1.dp, if (isSelected) colors.accent else colors.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.onAccent else colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
