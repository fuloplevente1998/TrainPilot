package com.repforge.app.ui.journal

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.repforge.app.R
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.data.db.JournalWorkoutSummary
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.db.WorkoutPhotoEntity
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.components.TrainPilotSurfaceCard
import com.repforge.app.ui.components.TrainPilotIconBadge
import com.repforge.app.ui.components.TrainPilotStatusBadge
import com.repforge.app.ui.theme.LocalTrainPilotColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun JournalScreen(navController: NavController, language: String = "hu", viewModel: JournalViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val workouts = viewModel.workouts.collectAsLazyPagingItems()
    val context = LocalContext.current
    val text = remember(context, language) { journalResources(context, language) }
    val colors = LocalTrainPilotColors.current
    val locale = remember(language) { Locale.forLanguageTag(language) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.query, state.month, state.day) { listState.scrollToItem(0) }
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickerMonthKey by rememberSaveable {
        mutableStateOf(
            (state.day?.let { YearMonth.from(it) } ?: state.month ?: YearMonth.now()).toString()
        )
    }
    LaunchedEffect(monthPickerOpen) {
        if (monthPickerOpen) {
            pickerMonthKey =
                (state.day?.let { YearMonth.from(it) } ?: state.month ?: YearMonth.now()).toString()
        }
    }
    if (monthPickerOpen) {
        val pickerMonth = runCatching { YearMonth.parse(pickerMonthKey) }.getOrElse { YearMonth.now() }
        val firstDay = pickerMonth.atDay(1)
        val leading = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val totalCells = ((leading + pickerMonth.lengthOfMonth() + 6) / 7) * 7
        val weekdayLabels = remember(locale) {
            DayOfWeek.entries.map { day ->
                day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
            }
        }
        AlertDialog(
            onDismissRequest = { monthPickerOpen = false },
            containerColor = colors.card,
            title = {
                Text(
                    text.getString(R.string.studio_journal_choose_date),
                    color = colors.text,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val recent = (0..3).map { YearMonth.now().minusMonths((3 - it).toLong()) }
                        items(recent, key = { it.toString() }) { month ->
                            FilterChip(
                                selected = pickerMonth == month,
                                onClick = { pickerMonthKey = month.toString() },
                                label = {
                                    Text(
                                        month.month.getDisplayName(
                                            TextStyle.SHORT_STANDALONE,
                                            locale
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("journal_picker_view_${month}")
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { pickerMonthKey = pickerMonth.minusMonths(1).toString() },
                            modifier = Modifier.testTag("journal_picker_previous_month")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = text.getString(R.string.journal_previous_month),
                                tint = colors.accentText
                            )
                        }
                        Text(
                            pickerMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                            color = colors.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { pickerMonthKey = pickerMonth.plusMonths(1).toString() },
                            modifier = Modifier.testTag("journal_picker_next_month")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = text.getString(R.string.journal_next_month),
                                tint = colors.accentText
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        weekdayLabels.forEach { label ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(totalCells / 7) { week ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(7) { column ->
                                    val cell = week * 7 + column
                                    val dayNumber = cell - leading + 1
                                    if (dayNumber !in 1..pickerMonth.lengthOfMonth()) {
                                        Spacer(Modifier.weight(1f).height(42.dp))
                                    } else {
                                        val date = pickerMonth.atDay(dayNumber)
                                        val selected = state.day == date
                                        val today = date == LocalDate.now()
                                        Surface(
                                            onClick = {
                                                viewModel.setDay(date)
                                                monthPickerOpen = false
                                            },
                                            modifier = Modifier.weight(1f).height(42.dp)
                                                .testTag("journal_picker_day_${date}"),
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                selected -> colors.accent
                                                today -> colors.accent.copy(alpha = .12f)
                                                else -> colors.card2
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    selected -> colors.accent
                                                    today -> colors.accent.copy(alpha = .45f)
                                                    else -> colors.line
                                                }
                                            )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    dayNumber.toString(),
                                                    color = if (selected) colors.onAccent else colors.text,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (selected || today)
                                                        FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.setMonth(pickerMonth)
                            monthPickerOpen = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("journal_picker_${pickerMonth}")
                    ) {
                        Text(
                            text.getString(R.string.studio_journal_whole_month),
                            color = colors.accentText
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.setMonth(null)
                            monthPickerOpen = false
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("journal_picker_all")
                    ) {
                        Text(
                            text.getString(R.string.journal_all_months),
                            color = if (state.month == null && state.day == null)
                                colors.accentText else colors.text
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { monthPickerOpen = false }) {
                    Text(text.getString(R.string.journal_close), color = colors.accentText)
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.bg)
        .padding(horizontal = 16.dp).padding(top = 8.dp)) {
        // Exact approved Studio composition: independent 16dp search input,
        // 40dp circular month arrows and a central 14dp outlined picker.
        OutlinedTextField(
            state.query, viewModel::setSearch,
            modifier = Modifier.fillMaxWidth().testTag("journal_search"),
            placeholder = { Text(text.getString(R.string.journal_search), color = colors.muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null,
                tint = colors.muted) },
            shape = RoundedCornerShape(16.dp), singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().testTag("journal_filters"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = {
                viewModel.setMonth((state.month ?: YearMonth.now()).minusMonths(1))
            }, modifier = Modifier.size(40.dp).testTag("journal_previous_month"),
                shape = CircleShape, color = colors.card,
                border = BorderStroke(1.dp, colors.line)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        text.getString(R.string.journal_previous_month), tint = colors.accentText)
                }
            }
            Surface(onClick = { monthPickerOpen = true },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("journal_month"),
                shape = RoundedCornerShape(14.dp), color = colors.card,
                border = BorderStroke(1.dp, colors.accent.copy(alpha = .5f))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null,
                        tint = colors.accentText, modifier = Modifier.size(18.dp))
                    Text(
                        state.day?.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                        ) ?: state.month?.format(
                            DateTimeFormatter.ofPattern("LLLL yyyy", locale)
                        ) ?: text.getString(R.string.journal_all_months),
                        color = colors.text, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        maxLines = 1)
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = colors.accentText)
                }
            }
            Surface(onClick = {
                viewModel.setMonth((state.month ?: YearMonth.now()).plusMonths(1))
            }, modifier = Modifier.size(40.dp).testTag("journal_next_month"),
                shape = CircleShape, color = colors.card,
                border = BorderStroke(1.dp, colors.line)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        text.getString(R.string.journal_next_month), tint = colors.accentText)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("journal_list"),
            contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (workouts.loadState.refresh is LoadState.Loading) item { JournalLoading(text) }
            if (workouts.loadState.refresh is LoadState.Error) item { JournalRetry(text) { workouts.retry() } }
            if (workouts.itemCount == 0 && workouts.loadState.refresh is LoadState.NotLoading) item {
                Text(text.getString(if (state.query.isBlank() && state.month == null) R.string.journal_empty else R.string.journal_no_matches),
                    color = colors.muted, modifier = Modifier.padding(vertical = 24.dp).testTag("journal_empty"))
            }
            items(workouts.itemCount, key = workouts.itemKey { it.id }) { index ->
                workouts[index]?.let { row -> JournalSummaryCard(row, text, locale) { viewModel.openDetails(row.id) } }
            }
            if (workouts.loadState.append is LoadState.Loading) item { JournalLoading(text) }
            if (workouts.loadState.append is LoadState.Error) item { JournalRetry(text) { workouts.retry() } }
        }
    }
    if (state.selectedWorkoutId != null) JournalDetailDialog(state, text, language, viewModel)
    state.activeRoute?.let { WorkoutRouteMapDialog(it, viewModel::closeRouteMap, language) }
    editor?.let { JournalEditorDialog(it, language, viewModel::editDraft, viewModel::saveEditor, viewModel::cancelEditor) }
}

/** Faithful 18:49 Studio 20dp-rounded horizontal workout card.
 * The summary is the canonical paged native DB projection. Missing items such
 * as total-volume or route thumbnail are not replaced by fake demo graphics.
 */
@Composable
private fun JournalSummaryCard(row: JournalWorkoutSummary, text: Resources, locale: Locale, onOpen: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val stamp = remember(row.startedEpochMs, locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(row.startedEpochMs))
    }
    Surface(onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("journal_workout_${row.id}"),
        shape = RoundedCornerShape(20.dp), color = colors.card,
        border = BorderStroke(1.dp, colors.line)) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(48.dp),
                shape = CircleShape, color = colors.accent.copy(alpha = .16f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null,
                        modifier = Modifier.size(24.dp), tint = colors.accentText)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stamp, color = colors.muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("journal_date_${row.id}"))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.dayName, modifier = Modifier.weight(1f, fill = false),
                        color = colors.text, style = MaterialTheme.typography.titleMedium,
                        maxLines = 2)
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = colors.success, modifier = Modifier.size(16.dp))
                }
                Text(row.programName, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(
                    text.getString(R.string.journal_minutes, row.durationSeconds / 60) +
                        " · " + text.getString(R.string.journal_sets,
                        row.completedSetsCount, row.totalSetsCount),
                    color = colors.text, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("journal_summary_${row.id}"))
            }
            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (row.hasPr226) {
                    Surface(modifier = Modifier.testTag("journal_pr_${row.id}")
                        .semantics {
                            contentDescription = text.getString(R.string.journal_pr)
                        }, shape = RoundedCornerShape(8.dp), color = colors.accent) {
                        Text("PR", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = colors.onAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = text.getString(R.string.journal_details),
                    tint = colors.accentText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun JournalDetailDialog(state: JournalUiState, text: Resources, language: String, model: JournalViewModel) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    var tab by rememberSaveable(state.selectedWorkoutId) { mutableIntStateOf(0) }
    var photo by remember { mutableStateOf<WorkoutPhotoEntity?>(null) }
    Dialog(onDismissRequest = model::closeDetails, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnBackPress = !state.deleting, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.bg) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text.getString(R.string.journal_details), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = model::closeDetails, enabled = !state.deleting,
                        modifier = Modifier.size(48.dp).testTag("journal_details_close").background(colors.danger, RoundedCornerShape(12.dp))) {
                        Icon(Icons.Default.Close, text.getString(R.string.journal_close), tint = colors.dangerText)
                    }
                }
                when {
                    state.detailLoading -> JournalLoading(text)
                    state.detailError -> JournalRetry(text) { model.openDetails(state.selectedWorkoutId!!) }
                    state.detail == null -> Text(text.getString(R.string.journal_missing))
                    else -> {
                        val detail = state.detail
                        val workout = detail.workout
                        Text(workout.dayName, style = MaterialTheme.typography.headlineSmall)
                        val tabs = listOf(R.string.journal_exercises, R.string.journal_statistics, R.string.journal_source)
                        TabRow(tab) { tabs.forEachIndexed { index, label ->
                            Tab(tab == index, onClick = { tab = index }, modifier = Modifier.testTag("journal_tab_$index"),
                                text = { Text(text.getString(label)) })
                        } }
                        LazyColumn(Modifier.weight(1f).testTag("journal_details_list"), verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)) {
                            if (tab == 0) items(detail.exercises.sortedBy { it.exercise.orderIndex }, key = { it.exercise.rowId }) { exercise ->
                                Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(if (language == "hu") exercise.exercise.nameHu else exercise.exercise.nameEn.ifBlank { exercise.exercise.nameHu }, fontWeight = FontWeight.Bold)
                                        for (set in exercise.sets.sortedBy { it.setNumber }) {
                                            val unit = if (exercise.exercise.measurementType == "time") text.getString(R.string.journal_seconds)
                                                else exercise.exercise.repUnit
                                            val reps = if (set.leftSeconds != null && set.rightSeconds != null)
                                                "${set.leftSeconds}/${set.rightSeconds} ${text.getString(R.string.journal_seconds)}" else "${set.reps} $unit"
                                            Text("${if (set.done) "✓" else "—"} ${set.setNumber}. $reps" +
                                                (set.weight?.let { " · $it kg" } ?: "") + (set.actualRir?.let { " · RIR $it" } ?: "") +
                                                (set.distanceMeters?.let { " · $it m" } ?: ""), color = if (set.done) colors.text else colors.muted)
                                        }
                                    }
                                }
                            }
                            if (tab == 1) item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text.getString(R.string.journal_minutes, workout.durationSeconds / 60))
                                    Text(text.getString(R.string.journal_sets, workout.completedSetsCount, workout.totalSetsCount))
                                    Text("${text.getString(R.string.journal_volume)}: ${workout.totalVolumeKg} kg")
                                    val effort = when (workout.effortFeedback) {
                                        "easy" -> R.string.journal_effort_easy; "light" -> R.string.journal_effort_light
                                        "good" -> R.string.journal_effort_good; "challenging" -> R.string.journal_effort_challenging
                                        "hard" -> R.string.journal_effort_hard; "pain" -> R.string.journal_effort_pain; else -> R.string.journal_no_data
                                    }
                                    Text("${text.getString(R.string.journal_feedback)}: ${text.getString(effort)}")
                                    OutlinedButton(onClick = model::openEditor, enabled = !state.deleting,
                                        modifier = Modifier.fillMaxWidth().testTag("journal_edit_feedback")) {
                                        Text(text.getString(R.string.journal_feedback_edit))
                                    }
                                    for (record in model.parsePrRecords(workout.prRecordsJson226)) {
                                        Text("${text.getString(R.string.journal_pr)} · ${record.exerciseName}: ${record.value}")
                                        Text(text.getString(R.string.journal_previous_record, record.previous), color = colors.muted)
                                    }
                                }
                            }
                            if (tab == 2) item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text.getString(if (workout.originDevice == "watch") R.string.journal_watch else R.string.journal_phone))
                                    JournalHealthPanel(detail, text, language, state.healthRefreshing, state.healthError, !state.deleting) {
                                        model.refreshWorkoutHealth(context as? android.app.Activity)
                                    }
                                }
                            }
                            val keys = detail.exercises.flatMap { it.sets }.flatMap { listOfNotNull(it.gpsRouteKey193, it.gpsRouteKey163) }
                                .filter { it.isNotBlank() }.distinct()
                            if (keys.isNotEmpty()) item {
                                OutlinedButton(onClick = { model.openRouteMap(keys) }, enabled = !state.routeLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("journal_open_route")) { Text(text.getString(R.string.journal_route)) }
                                if (state.routeLoading) JournalLoading(text)
                                if (state.routeError) Text(text.getString(R.string.journal_route_error), color = colors.muted)
                            }
                            val photos = detail.photos.filter { it.deletedAt.isNullOrBlank() }
                            if (photos.isNotEmpty()) item {
                                Text(text.getString(R.string.journal_photos), style = MaterialTheme.typography.titleMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(photos, key = { it.id }) { entry ->
                                        Column(Modifier.width(150.dp).clickable { photo = entry }) {
                                            PrivateJournalPhoto(entry.id, 400, text, Modifier.size(150.dp))
                                            Text(when (entry.label) { "before" -> text.getString(R.string.journal_photo_before)
                                                "after" -> text.getString(R.string.journal_photo_after); else -> entry.label })
                                        }
                                    }
                                }
                            }
                            item {
                                OutlinedButton(onClick = model::openEditor, enabled = !state.deleting,
                                    modifier = Modifier.fillMaxWidth().testTag("journal_edit")) { Text(text.getString(R.string.journal_edit_title)) }
                                OutlinedButton(onClick = { runCatching { context.startActivity(model.createCalendarIntent(detail)) } }, modifier = Modifier.fillMaxWidth()) {
                                    Text(text.getString(R.string.journal_calendar))
                                }
                                TextButton(onClick = model::requestDeleteWorkout, enabled = !state.deleting,
                                    modifier = Modifier.fillMaxWidth().testTag("journal_delete")) {
                                    Text(text.getString(R.string.journal_delete), color = colors.dangerText)
                                }
                            }
                        }
                    }
                }
                if (state.operationFailed) TextButton(onClick = model::clearOperationError) { Text(text.getString(R.string.journal_operation_error)) }
            }
        }
    }
    if (state.workoutToDelete != null) AlertDialog(onDismissRequest = model::cancelDeleteWorkout,
        title = { Text(text.getString(R.string.journal_delete_title)) },
        text = { Text(text.getString(R.string.journal_delete_message, state.workoutToDelete.workout.dayName)) },
        confirmButton = { TextButton(onClick = model::confirmDeleteWorkout, enabled = !state.deleting,
            modifier = Modifier.testTag("journal_delete_confirm")) { Text(text.getString(R.string.journal_delete)) } },
        dismissButton = { TextButton(onClick = model::cancelDeleteWorkout, enabled = !state.deleting) { Text(text.getString(R.string.journal_cancel)) } })
    photo?.let { entry ->
        Dialog(onDismissRequest = { photo = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = colors.bg) {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    IconButton(onClick = { photo = null }, modifier = Modifier.align(Alignment.End).size(48.dp)) {
                        Icon(Icons.Default.Close, text.getString(R.string.journal_close), tint = colors.dangerText)
                    }
                    PrivateJournalPhoto(entry.id, 1600, text, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun PrivateJournalPhoto(id: String, edge: Int, text: Resources, modifier: Modifier) {
    val context = LocalContext.current
    val preview by produceState<Result<ImageBitmap?>?>(initialValue = null, key1 = id, key2 = edge) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = WorkoutPhotoRepository(context).photoFile(id)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > edge * 2) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bitmap = preview?.getOrNull()
        if (bitmap != null) Image(bitmap, text.getString(R.string.journal_photos), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else if (preview == null) JournalLoading(text) else Text(text.getString(R.string.journal_photo_error))
    }
}

@Composable
private fun JournalLoading(text: Resources) {
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("journal_loading"))
    Text(text.getString(R.string.journal_loading), style = MaterialTheme.typography.labelSmall)
}
@Composable
private fun JournalRetry(text: Resources, onRetry: () -> Unit) {
    Text(text.getString(R.string.journal_error))
    TextButton(onClick = onRetry) { Text(text.getString(R.string.journal_retry)) }
}
