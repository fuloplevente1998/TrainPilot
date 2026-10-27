package com.repforge.app.ui.journal

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DateRange
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    LaunchedEffect(state.query, state.month) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 16.dp)) {
        TrainPilotSectionSurface(Modifier.fillMaxWidth().testTag("journal_filters")) {
        OutlinedTextField(state.query, viewModel::setSearch, modifier = Modifier.fillMaxWidth().testTag("journal_search"),
            label = { Text(text.getString(R.string.journal_search)) }, singleLine = true)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.setMonth((state.month ?: YearMonth.now()).minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, text.getString(R.string.journal_previous_month))
            }
            TextButton(onClick = { viewModel.setMonth(if (state.month == null) YearMonth.now() else null) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("journal_month")) {
                Text(state.month?.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)) ?: text.getString(R.string.journal_all_months),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
            }
            IconButton(onClick = { viewModel.setMonth((state.month ?: YearMonth.now()).plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, text.getString(R.string.journal_next_month))
            }
        }
        }
        Spacer(Modifier.height(12.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalSummaryCard(row: JournalWorkoutSummary, text: Resources, locale: Locale, onOpen: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    TrainPilotSurfaceCard(Modifier.fillMaxWidth().testTag("journal_workout_${row.id}"), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TrainPilotIconBadge(Icons.Default.History)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.programName, color = colors.muted, style = MaterialTheme.typography.labelMedium)
                Text(row.dayName, color = colors.text, style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.accentText)
        }
        // The Studio journal uses a distinct event-date row, but the time is the real saved
        // workout timestamp (never a fixed demo date).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.DateRange, contentDescription = null,
                modifier = Modifier.size(16.dp), tint = colors.accentText)
            Text(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(row.startedEpochMs)),
                color = colors.muted, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"))
        }
        HorizontalDivider(color = colors.line)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TrainPilotStatusBadge(text.getString(R.string.journal_minutes, row.durationSeconds / 60))
            TrainPilotStatusBadge(text.getString(R.string.journal_sets, row.completedSetsCount, row.totalSetsCount))
            if (row.hasPr226) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint = colors.accentText)
                Text(text.getString(R.string.journal_pr), color = colors.accentText)
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
