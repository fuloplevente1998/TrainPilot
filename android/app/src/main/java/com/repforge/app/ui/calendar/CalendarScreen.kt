package com.repforge.app.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun CalendarScreen(starting: Boolean, onStart: (CalendarScheduleEntity) -> Unit,
    onJournal: () -> Unit, model: CalendarViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val words = calendarWords(strings)
    val locale = Locale.forLanguageTag(state.language)
    val dateFormat = remember(state.language) { DateTimeFormatter.ofPattern("yyyy. MMMM d.", locale) }
    var editing by remember { mutableStateOf<CalendarScheduleEntity?>(null) }
    var plannerOpen by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CalendarScheduleEntity?>(null) }
    val busy = state.busy || starting
    // A planner can only start after persisted settings and a usable program are loaded.
    val plannerReady = state.plannerLoaded && state.programs.any { it.days.isNotEmpty() }
    val dayList = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(state.month) { dayList.scrollToItem((state.day.dayOfMonth - 3).coerceAtLeast(0)) }
    val plans = state.plans.filter { it.date == state.day.toString() }
    val history = state.history.filter { model.historyDay(it) == state.day }
    val completedIds = state.history.map { it.scheduleId }.filter { it.isNotBlank() }.toSet()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(words[0], style = MaterialTheme.typography.headlineSmall, color = colors.text)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { model.changeMonth(-1) }, enabled = !busy) { Text(words[1]) }
                    Text(state.month.format(DateTimeFormatter.ofPattern("yyyy. MMMM", locale)), color = colors.text,
                        modifier = Modifier.padding(top = 12.dp))
                    TextButton(onClick = { model.changeMonth(1) }, enabled = !busy) { Text(words[2]) }
                }
                LazyRow(state = dayList, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((1..state.month.lengthOfMonth()).toList(), key = { it }) { number ->
                        val date = state.month.atDay(number)
                        if (date == state.day) Button(onClick = { model.selectDay(date) }, enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text(number.toString()) }
                        else OutlinedButton(onClick = { model.selectDay(date) }, enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text(number.toString()) }
                    }
                }
                Text(state.day.format(dateFormat), color = colors.text)
                Button(onClick = { editing = null; editorOpen = true }, enabled = !busy && state.programs.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(words[3]) }
                OutlinedButton(onClick = { plannerOpen = true }, enabled = !busy && plannerReady,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("calendar_planner_open")) {
                    Text(strings.getString(R.string.calendar_planner_title))
                }
                if (state.programs.isEmpty()) Text(words[4], color = colors.muted)
                if (!state.loaded) CircularProgressIndicator()
                if (state.loaded && plans.isEmpty() && history.isEmpty()) Text(words[5], color = colors.muted)
            }
        }
        items(plans, key = { "plan|${it.id}" }) { row ->
            val completed = row.status == "completed" || row.id in completedIds
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${row.programName} · ${row.dayName}", color = colors.text)
                    val time = runCatching { Instant.parse(row.startIso).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm", locale)) }.getOrDefault("—")
                    Text("$time · ${if (completed) words[8] else if (row.status == "skipped") words[7] else words[6]}", color = colors.muted)
                    if (completed) TextButton(onClick = onJournal) { Text(words[9]) }
                    else {
                        if (row.status == "planned") Button(onClick = { onStart(row) }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(words[10]) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editing = row; editorOpen = true }, enabled = !busy) { Text(words[11]) }
                            TextButton(onClick = { model.status(row, if (row.status == "skipped") "planned" else "skipped") }, enabled = !busy) {
                                Text(if (row.status == "skipped") words[13] else words[12])
                            }
                            TextButton(onClick = { deleting = row }, enabled = !busy) { Text(words[14]) }
                        }
                    }
                }
            }
        }
        items(history.filter { it.scheduleId !in plans.map { p -> p.id }.toSet() }, key = { "history|${it.id}" }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("${row.programName} · ${row.dayName}", color = colors.text)
                    Text(words[8], color = colors.muted)
                    TextButton(onClick = onJournal) { Text(words[9]) }
                }
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
    if (editorOpen) CalendarEditor(state, editing, busy, words, onDismiss = { editorOpen = false }, onSave = {
        model.save(it) { editorOpen = false }
    })
    if (plannerOpen) CalendarPlannerDialog(state, busy, strings, onCancel = {
        model.cancelPlanner(); plannerOpen = false
    }, onPreview = model::previewPlanner, onApply = { model.applyPlanner { plannerOpen = false } })
    deleting?.let { row ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text(words[14]) },
            text = { Text(words[15]) }, confirmButton = {
                TextButton(onClick = { model.status(row, "skipped", delete = true); deleting = null }, enabled = !busy) { Text(words[14]) }
            }, dismissButton = { TextButton(onClick = { deleting = null }, enabled = !busy) { Text(words[16]) } })
    }
    if (state.error) AlertDialog(onDismissRequest = model::clearError, title = { Text(words[0]) },
        text = { Text(words[24]) }, confirmButton = { TextButton(onClick = model::clearError) { Text(words[25]) } })
}

@Composable
private fun CalendarEditor(state: CalendarUiState, row: CalendarScheduleEntity?, busy: Boolean,
    words: List<String>, onDismiss: () -> Unit, onSave: (NativeCalendarRepository.Edit) -> Unit) {
    val initialStart = remember(row?.id) { runCatching { Instant.parse(row?.startIso).atZone(ZoneId.systemDefault()) }.getOrNull() }
    var date by remember(row?.id) { mutableStateOf(initialStart?.toLocalDate()?.toString() ?: state.day.toString()) }
    var time by remember(row?.id) { mutableStateOf(initialStart?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "18:00") }
    var minutes by remember(row?.id) { mutableStateOf(runCatching { Duration.between(Instant.parse(row?.startIso), Instant.parse(row?.endIso)).toMinutes().toString() }.getOrDefault("45")) }
    var programId by remember(row?.id) { mutableStateOf(row?.programId ?: state.programs.firstOrNull()?.program?.id.orEmpty()) }
    var dayId by remember(row?.id) { mutableStateOf(row?.dayId ?: state.programs.firstOrNull()?.days?.firstOrNull()?.dayId.orEmpty()) }
    var invalid by remember { mutableStateOf(false) }
    var programMenu by remember { mutableStateOf(false) }
    var dayMenu by remember { mutableStateOf(false) }
    val program = state.programs.firstOrNull { it.program.id == programId }
    val day = program?.days?.firstOrNull { it.dayId == dayId }
    fun name(hu: String, en: String, de: String, ro: String) = when (state.language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (row == null) words[3] else words[11]) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(words[17] + " (YYYY-MM-DD)") }, enabled = !busy, singleLine = true)
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text(words[18] + " (HH:mm)") }, enabled = !busy, singleLine = true)
            OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text(words[19] + " (10–240)") }, enabled = !busy, singleLine = true)
            Box {
                OutlinedButton(onClick = { programMenu = true }, enabled = !busy) { Text(program?.program?.let { name(it.nameHu, it.nameEn, it.nameDe, it.nameRo) } ?: words[20]) }
                DropdownMenu(expanded = programMenu, onDismissRequest = { programMenu = false }) {
                    state.programs.forEach { p -> DropdownMenuItem(text = { Text(name(p.program.nameHu, p.program.nameEn, p.program.nameDe, p.program.nameRo)) }, onClick = {
                        programId = p.program.id; dayId = p.days.firstOrNull()?.dayId.orEmpty(); programMenu = false
                    }) }
                }
            }
            Box {
                OutlinedButton(onClick = { dayMenu = true }, enabled = !busy && program != null) { Text(day?.let { name(it.nameHu, it.nameEn, it.nameDe, it.nameRo) } ?: words[21]) }
                DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                    program?.days.orEmpty().forEach { d -> DropdownMenuItem(text = { Text(name(d.nameHu, d.nameEn, d.nameDe, d.nameRo)) }, onClick = { dayId = d.dayId; dayMenu = false }) }
                }
            }
            if (invalid) Text(words[23])
        }
    }, confirmButton = {
        TextButton(onClick = {
            val edit = runCatching {
                val duration = minutes.toInt(); require(duration in 10..240 && day != null)
                val parsedDate = LocalDate.parse(date); require(parsedDate.year in 1900..2200)
                NativeCalendarRepository.Edit(row?.id, row, programId, dayId, parsedDate, LocalTime.parse(time), duration)
            }.getOrNull()
            if (edit == null) invalid = true else { invalid = false; onSave(edit) }
        }, enabled = !busy && day != null) { Text(words[22]) }
    }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(words[16]) } })
}

internal data class CalendarPlannerInitialSelection(val programId: String, val firstIndex: Int)

internal fun calendarPlannerInitialSelection(
    settings: NativeCalendarPlanner.Settings?,
    programs: List<ProgramWithDays>
): CalendarPlannerInitialSelection {
    val chosen = programs.filter { it.days.isNotEmpty() }.let { usable ->
        usable.firstOrNull { it.program.id == settings?.programId } ?: usable.firstOrNull()
    }
    val firstIndex = if (settings != null && chosen != null && settings.programId == chosen.program.id) {
        settings.firstIndex.coerceIn(0, chosen.days.lastIndex)
    } else 0
    return CalendarPlannerInitialSelection(chosen?.program?.id.orEmpty(), firstIndex)
}

@Composable
private fun CalendarPlannerDialog(state: CalendarUiState, busy: Boolean, strings: Resources,
    onCancel: () -> Unit, onPreview: (NativeCalendarPlanner.Settings) -> Unit, onApply: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val initial = state.plannerSettings
    val usablePrograms = state.programs.filter { it.days.isNotEmpty() }
    val initialSelection = calendarPlannerInitialSelection(initial, state.programs)
    var programId by remember { mutableStateOf(initialSelection.programId) }
    var date by remember { mutableStateOf((initial?.start ?: state.day).toString()) }
    var time by remember { mutableStateOf(initial?.time?.toString() ?: "18:00") }
    var weeks by remember { mutableStateOf((initial?.weeks ?: 4).toString()) }
    var minutes by remember { mutableStateOf((initial?.minutes ?: 45).toString()) }
    // A removed/shortened saved program must not leave an invalid day index.
    var first by remember { mutableStateOf(initialSelection.firstIndex) }
    var mode by remember { mutableStateOf(initial?.mode ?: "alternate") }
    var weekdays by remember { mutableStateOf(initial?.weekdays ?: setOf(1, 3, 5)) }
    var invalid by remember { mutableStateOf(false) }
    val program = state.programs.firstOrNull { it.program.id == programId }
    val days = program?.days.orEmpty().sortedBy { it.sortOrder }
    fun input(): NativeCalendarPlanner.Settings = NativeCalendarPlanner.Settings(programId, LocalDate.parse(date),
        LocalTime.parse(time), minutes.toInt(), weeks.toInt(), first, mode, weekdays)
    val preview = state.plannerPreview?.takeIf { runCatching { input() == it.settings }.getOrDefault(false) }
    AlertDialog(onDismissRequest = { if (!busy) onCancel() }, title = { Text(strings.getString(R.string.calendar_planner_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("calendar_planner_form"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                usablePrograms.forEach { p ->
                    TextButton(onClick = { programId = p.program.id; first = 0 }, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text((if (programId == p.program.id) "✓ " else "") + when (state.language) { "en" -> p.program.nameEn; "de" -> p.program.nameDe; "ro" -> p.program.nameRo; else -> p.program.nameHu }) }
                }
                listOf("alternate" to R.string.calendar_planner_alternate, "daily" to R.string.calendar_planner_daily,
                    "weekly" to R.string.calendar_planner_weekly).forEach { (value, label) ->
                    TextButton(onClick = { mode = value }, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_planner_mode_$value")) {
                        Text((if (mode == value) "✓ " else "") + strings.getString(label))
                    }
                }
                if (mode == "weekly") (1..7).forEach { n ->
                    val value = n % 7
                    Row {
                        Checkbox(checked = value in weekdays, enabled = !busy, onCheckedChange = {
                            weekdays = if (it) weekdays + value else weekdays - value
                        })
                        Text(java.time.DayOfWeek.of(n).getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag(state.language)))
                    }
                }
                @Composable fun field(value: String, label: Int, tag: String, change: (String) -> Unit) {
                    OutlinedTextField(value, change, label = { Text(strings.getString(label)) }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().testTag(tag), singleLine = true)
                }
                field(date, R.string.calendar_planner_date, "calendar_planner_date") { date = it }
                field(time, R.string.calendar_planner_time, "calendar_planner_time") { time = it }
                field(minutes, R.string.calendar_planner_minutes, "calendar_planner_minutes") { minutes = it }
                field(weeks, R.string.calendar_planner_weeks, "calendar_planner_weeks") { weeks = it }
                Text(strings.getString(R.string.calendar_planner_first), color = colors.text)
                days.forEachIndexed { index, day ->
                    TextButton(onClick = { first = index }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text((if (first == index) "✓ " else "") + when (state.language) {
                            "en" -> day.nameEn; "de" -> day.nameDe; "ro" -> day.nameRo; else -> day.nameHu
                        })
                    }
                }
                if (invalid) Text(strings.getString(R.string.calendar_planner_invalid), color = colors.dangerText)
                preview?.let { value ->
                    Text(strings.getString(R.string.calendar_planner_preview, value.plan.slots.size, value.plan.removed.size),
                        color = colors.text, modifier = Modifier.testTag("calendar_planner_preview"))
                    value.plan.slots.forEach { slot -> Text("${slot.date} · ${value.settings.time} · ${slot.dayId}", color = colors.text) }
                }
            }
        }, confirmButton = {
            if (preview != null) TextButton(onClick = onApply, enabled = !busy && preview.plan.slots.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_planner_save")) { Text(strings.getString(R.string.calendar_planner_save)) }
            else TextButton(onClick = {
                val value = runCatching { input().also {
                    require(it.weeks in 1..12 && it.minutes in 10..240 && it.firstIndex in days.indices)
                    require(it.mode != "weekly" || it.weekdays.isNotEmpty())
                } }.getOrNull()
                invalid = value == null
                if (value != null) onPreview(value)
            }, enabled = !busy && days.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_planner_generate")) {
                Text(strings.getString(R.string.calendar_planner_generate))
            }
        }, dismissButton = { TextButton(onClick = onCancel, enabled = !busy,
            modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_planner_cancel")) { Text(strings.getString(R.string.calendar_planner_cancel)) } })
}

private fun calendarWords(strings: Resources): List<String> = listOf(
    strings.getString(R.string.native_calendar_00),
    strings.getString(R.string.native_calendar_01),
    strings.getString(R.string.native_calendar_02),
    strings.getString(R.string.native_calendar_03),
    strings.getString(R.string.native_calendar_04),
    strings.getString(R.string.native_calendar_05),
    strings.getString(R.string.native_calendar_06),
    strings.getString(R.string.native_calendar_07),
    strings.getString(R.string.native_calendar_08),
    strings.getString(R.string.native_calendar_09),
    strings.getString(R.string.native_calendar_10),
    strings.getString(R.string.native_calendar_11),
    strings.getString(R.string.native_calendar_12),
    strings.getString(R.string.native_calendar_13),
    strings.getString(R.string.native_calendar_14),
    strings.getString(R.string.native_calendar_15),
    strings.getString(R.string.native_calendar_16),
    strings.getString(R.string.native_calendar_17),
    strings.getString(R.string.native_calendar_18),
    strings.getString(R.string.native_calendar_19),
    strings.getString(R.string.native_calendar_20),
    strings.getString(R.string.native_calendar_21),
    strings.getString(R.string.native_calendar_22),
    strings.getString(R.string.native_calendar_23),
    strings.getString(R.string.native_calendar_24),
    strings.getString(R.string.native_calendar_25)
)
