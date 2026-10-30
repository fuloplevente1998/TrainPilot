package com.repforge.app.ui.health

import android.app.DatePickerDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A read-only detail overlay: closing it never edits measurements or changes the selected summary. */
@Composable
internal fun HealthRecordBrowser(day: String, channel: String, type: String, language: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val resources = nativeResources(context, language)
    val colors = LocalTrainPilotColors.current
    var query by remember(day, channel, type) { mutableStateOf(HealthRecordQuery(LocalDate.parse(day), channel, type = type)) }
    var previous by remember(day, channel, type) { mutableStateOf(emptyList<HealthRecordQuery>()) }
    var loaded by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, HealthRecordPage>?>(null) }
    var failure by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, Boolean>?>(null) }
    var reloadToken by remember(day, channel, type) { mutableStateOf(0) }
    var draftFrom by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
    var draftTo by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
    var activePicker by remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(context, language, day, channel, type) {
        onDispose { activePicker?.dismiss(); activePicker = null }
    }
    val validRange = !draftFrom.isAfter(draftTo) &&
        HealthJournalStore.validDay(draftFrom.toString()) && HealthJournalStore.validDay(draftTo.toString())
    val rangeChanged = draftFrom != query.fromDay || draftTo != query.toDay
    val page = loaded?.takeIf { it.first == query }?.second
    val error = failure?.takeIf { it.first == query }
    val loading = page == null && error == null

    LaunchedEffect(query, reloadToken) {
        val requested = query
        val revision = reloadToken
        try {
            val result = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) {
                    NativeHealthRecords(HealthJournalStore.get(context)).load(requested)
                }
            }
            currentCoroutineContext().ensureActive()
            // IO may finish after the user changes filters/reloads. Never commit a stale page.
            if (query == requested && reloadToken == revision) {
                loaded = requested to result
                failure = null
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            if (query == requested && reloadToken == revision)
                failure = requested to (error is NativeHealthRecords.ReplacedJournal)
        }
    }

    fun clearPage() {
        previous = emptyList()
        loaded = null
        failure = null
        reloadToken++
    }

    fun restart(source: String = query.source) {
        // Re-selecting the same query must also trigger a fresh load after a read failure.
        query = query.withFilters(source = source)
        clearPage()
    }

    fun applyRange() {
        if (!validRange || !rangeChanged) return
        query = query.withRange(draftFrom, draftTo)
        clearPage()
    }

    fun pickDate(selected: LocalDate, update: (LocalDate) -> Unit) {
        activePicker?.dismiss()
        // Keep the Activity window token/theme while using the app-selected language.
        val pickerContext = android.view.ContextThemeWrapper(context, context.theme).apply {
            applyOverrideConfiguration(resources.configuration)
        }
        val picker = DatePickerDialog(pickerContext, { _, year, month, pickedDay ->
            runCatching { LocalDate.of(year, month + 1, pickedDay) }
                .getOrNull()?.takeIf { HealthJournalStore.validDay(it.toString()) }?.let(update)
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth)
        picker.setOnDismissListener { if (activePicker === picker) activePicker = null }
        activePicker = picker
        picker.show()
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp).testTag("health_record_browser")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(resources.getString(if (type == "SleepSessionRecord") R.string.health_record_sleep else R.string.health_records_title),
                        color = colors.text, fontWeight = FontWeight.Bold)
                    Text(if (query.fromDay == query.toDay) query.fromDay.toString()
                        else "${query.fromDay} – ${query.toDay}", color = colors.muted)
                    Text(healthChannelName(channel, resources), color = colors.muted)
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag("health_records_close")) {
                    Icon(Icons.Default.Close, resources.getString(R.string.health_records_close), tint = colors.danger)
                }
            }
            // Pick valid calendar dates first; apply both bounds atomically to the paged SQL query.
            Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(resources.getString(R.string.health_range_from), color = colors.muted)
                    OutlinedButton(onClick = { pickDate(draftFrom) { draftFrom = it } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_range_from")) {
                        Text(draftFrom.toString())
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(resources.getString(R.string.health_range_to), color = colors.muted)
                    OutlinedButton(onClick = { pickDate(draftTo) { draftTo = it } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_range_to")) {
                        Text(draftTo.toString())
                    }
                }
            }
            if (!validRange) Text(resources.getString(R.string.health_range_invalid),
                color = colors.dangerText, modifier = Modifier.testTag("health_range_error"))
            TextButton(onClick = ::applyRange, enabled = validRange && rangeChanged,
                modifier = Modifier.heightIn(min = 48.dp).testTag("health_range_apply")) {
                Text(resources.getString(R.string.health_range_apply))
            }
            if (loading) Text(resources.getString(R.string.health_history_loading), color = colors.muted)
            if (error != null) {
                Text(resources.getString(if (error.second) R.string.health_records_changed else R.string.health_history_error),
                    color = colors.danger, modifier = Modifier.testTag("health_records_error"))
            }
            TextButton(onClick = { restart(); }, enabled = !loading, modifier = Modifier.testTag("health_records_reload")) {
                Text(resources.getString(R.string.health_records_reload))
            }
            page?.let { current ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = query.source.isEmpty(), onClick = { restart("") },
                        label = { Text(resources.getString(R.string.health_records_origins)) }) }
                    items(current.origins, key = { it.source }) { origin ->
                        FilterChip(selected = query.source == origin.source, onClick = { restart(origin.source) },
                            modifier = Modifier.testTag("health_origin_${origin.source}"),
                            label = { Text(if (origin.name.isNotBlank()) "${origin.name} (${origin.source})" else origin.source) })
                    }
                }
                Text(resources.getString(R.string.health_records_raw_notice), color = colors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = previous.isNotEmpty(), modifier = Modifier.testTag("health_records_previous"), onClick = {
                        val last = previous.last()
                        previous = previous.dropLast(1)
                        query = last.copy(generation = current.generation)
                    }) { Text(resources.getString(R.string.health_records_newer)) }
                    Text(resources.getString(R.string.health_records_page, previous.size + 1), color = colors.text,
                        modifier = Modifier.padding(vertical = 12.dp))
                    TextButton(enabled = current.next != null, modifier = Modifier.testTag("health_records_next"), onClick = {
                        previous = previous + query
                        query = query.copy(cursor = current.next, generation = current.generation)
                    }) { Text(resources.getString(R.string.health_records_older)) }
                }
                key(query, reloadToken) {
                    LazyColumn(Modifier.weight(1f).testTag("health_records_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (current.rows.isEmpty()) item { Text(resources.getString(R.string.health_records_empty), color = colors.muted) }
                        items(current.rows, key = { "${it.channel}:${it.type}:${it.id}" }) { row -> HealthRecordCard(row, language) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRecordCard(row: HealthRecordRow, language: String) {
    val text = nativeResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    val data = remember(row.data) { JSONObject(row.data) }
    var expanded by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(false) }
    var offset by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(0) }
    val unit = (data.opt("unit") as? String).orEmpty().let { if (it == "steps") text.getString(R.string.health_records_steps) else it }
    val scalar = NativeHealthRecords.number(data, "value")
        ?: if (row.type == "SleepSessionRecord") NativeHealthRecords.number(data, "durationMinutes") else null
    val displayUnit = if (row.type == "SleepSessionRecord" && data.has("durationMinutes")) "min" else unit
    Card(Modifier.fillMaxWidth().testTag("health_record_${row.id}"), colors = CardDefaults.cardColors(containerColor = colors.card)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text.getString(NativeHealthRecords.title(row.type)), color = colors.text, fontWeight = FontWeight.Bold)
            Text(text.getString(R.string.health_record_source, "${healthChannelName(row.channel, text)} · ${row.source}"), color = colors.muted)
            Text(text.getString(R.string.health_record_start, healthRecordTime(row.startMs, language)), color = colors.muted)
            if (row.endMs != row.startMs) Text(text.getString(R.string.health_record_end, healthRecordTime(row.endMs, language)), color = colors.muted)
            Text(text.getString(R.string.health_record_modified, healthRecordTime(row.modifiedMs, language)), color = colors.muted)
            if (row.type == "BloodPressureRecord") {
                Text("${healthRecordNumber(NativeHealthRecords.number(data, "systolic"), language)} / ${healthRecordNumber(NativeHealthRecords.number(data, "diastolic"), language)} $unit", color = colors.accentText)
            } else if (scalar != null) {
                if (row.type == "SleepSessionRecord") Text(text.getString(
                    if (data.optString("durationKind") == "asleep_stages") R.string.health_sleep_asleep else R.string.health_sleep_duration), color = colors.muted)
                Text("${healthRecordNumber(scalar, language)} $displayUnit", color = colors.accentText)
            }
            val count = data.optJSONArray("stages")?.length() ?: data.optJSONArray("samples")?.length() ?: 0
            if (count > 0) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("health_record_expand_${row.id}")) {
                    Text(text.getString(if (row.type == "SleepSessionRecord") R.string.health_records_stages else R.string.health_records_samples, count))
                }
                if (expanded) {
                    val series = remember(row.data, offset) { NativeHealthRecordSeries.page(data, offset) }
                    series.items.forEach { item ->
                        Column {
                            Text(if (row.type == "SleepSessionRecord") text.getString(NativeHealthRecordSeries.stageLabel(item.stage))
                                else "${healthRecordNumber(item.value, language)} $unit", color = colors.text)
                            val start = item.startMs?.let { healthRecordTime(it, language) } ?: "—"
                            val end = item.endMs?.let { " – ${healthRecordTime(it, language)}" }.orEmpty()
                            Text(start + end, color = colors.muted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = offset > 0, onClick = { offset = maxOf(0, offset - NativeHealthRecordSeries.PAGE_SIZE) }) {
                            Text(text.getString(R.string.health_records_newer))
                        }
                        TextButton(enabled = offset + NativeHealthRecordSeries.PAGE_SIZE < series.total,
                            onClick = { offset += NativeHealthRecordSeries.PAGE_SIZE }) { Text(text.getString(R.string.health_records_older)) }
                    }
                }
            } else if (scalar == null && row.type != "BloodPressureRecord") Text(text.getString(R.string.health_history_no_data), color = colors.muted)
        }
    }
}

private fun healthRecordNumber(value: Double?, language: String): String = value?.let {
    NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 2 }.format(it)
} ?: "—"
private fun healthRecordTime(value: Long, language: String): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(language))
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
}.getOrDefault("—")
private fun healthChannelName(channel: String, text: android.content.res.Resources): String = when (channel) {
    HealthJournalStore.HC -> "Health Connect"
    HealthJournalStore.SAMSUNG -> "Samsung Health"
    HealthJournalStore.BLE -> "BLE"
    else -> text.getString(R.string.health_history_title)
}
