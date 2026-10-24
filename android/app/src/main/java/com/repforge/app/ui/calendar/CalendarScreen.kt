package com.repforge.app.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun CalendarScreen(starting: Boolean, onStart: (CalendarScheduleEntity) -> Unit,
    onJournal: () -> Unit, model: CalendarViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val words = calendarWords(state.language)
    val locale = Locale.forLanguageTag(state.language)
    val dateFormat = remember(state.language) { DateTimeFormatter.ofPattern("yyyy. MMMM d.", locale) }
    var editing by remember { mutableStateOf<CalendarScheduleEntity?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CalendarScheduleEntity?>(null) }
    val busy = state.busy || starting
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

private fun calendarWords(language: String) = when (language) {
    "en" -> listOf("Calendar", "Previous", "Next", "Add workout", "Create a program day first.", "No workout on this date.", "Planned", "Skipped", "Completed", "Open journal", "Start workout", "Edit / move", "Skip", "Restore", "Delete", "Delete this planned occasion? Logged workouts remain in the journal.", "Cancel", "Date", "Time", "Minutes", "Program", "Program day", "Save", "Check the date, time and duration.", "The plan changed or could not be saved. Reopen it and check your input. Logged workouts remain available.", "OK")
    "de" -> listOf("Kalender", "Zurück", "Weiter", "Training hinzufügen", "Zuerst einen Programmtag erstellen.", "Kein Training an diesem Datum.", "Geplant", "Übersprungen", "Abgeschlossen", "Protokoll öffnen", "Training starten", "Bearbeiten / verschieben", "Überspringen", "Wiederherstellen", "Löschen", "Diesen geplanten Termin löschen? Protokollierte Trainings bleiben erhalten.", "Abbrechen", "Datum", "Uhrzeit", "Minuten", "Programm", "Programmtag", "Speichern", "Datum, Uhrzeit und Dauer prüfen.", "Der Plan wurde geändert oder konnte nicht gespeichert werden. Erneut öffnen und Eingaben prüfen. Protokollierte Trainings bleiben erhalten.", "OK")
    "ro" -> listOf("Calendar", "Înapoi", "Înainte", "Adaugă antrenament", "Creează mai întâi o zi de program.", "Niciun antrenament la această dată.", "Planificat", "Omis", "Finalizat", "Deschide jurnalul", "Începe antrenamentul", "Editează / mută", "Omite", "Restabilește", "Șterge", "Ștergi această sesiune planificată? Antrenamentele înregistrate rămân în jurnal.", "Anulează", "Data", "Ora", "Minute", "Program", "Zi de program", "Salvează", "Verifică data, ora și durata.", "Planul a fost modificat sau nu a putut fi salvat. Redeschide-l și verifică datele. Antrenamentele înregistrate rămân disponibile.", "OK")
    else -> listOf("Naptár", "Előző", "Következő", "Edzés hozzáadása", "Előbb hozz létre programnapot.", "Erre a napra nincs edzés.", "Tervezett", "Kihagyva", "Teljesített", "Napló megnyitása", "Edzés indítása", "Módosítás / áthelyezés", "Kihagyás", "Visszaállítás", "Törlés", "Törlöd ezt a tervezett alkalmat? A naplózott edzések megmaradnak.", "Mégse", "Dátum", "Időpont", "Perc", "Program", "Programnap", "Mentés", "Ellenőrizd a dátumot, időpontot és időtartamot.", "Az alkalom megváltozott vagy nem menthető. Nyisd meg újra és ellenőrizd az adatokat. A naplózott edzések megmaradtak.", "Rendben")
}
