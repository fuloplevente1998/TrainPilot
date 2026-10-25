package com.repforge.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.repforge.app.R
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun WeightJournalScreen(viewModel: WeightJournalViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = viewModel.entries.collectAsLazyPagingItems()
    val resources = nativeResources(LocalContext.current, state.language)
    val colors = LocalTrainPilotColors.current
    var adding by rememberSaveable { mutableStateOf(false) }
    // Keep the exact version the user opened, so a concurrent update cannot be silently overwritten.
    var editing by remember { mutableStateOf<BodyWeightEntryEntity?>(null) }
    var deleting by remember { mutableStateOf<BodyWeightEntryEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().testTag("weight_journal_list").padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            WeightJournalOverview(state, viewModel::setRange, {
                viewModel.clearWriteError(); adding = true
            }, R.string.weight_add, viewModel::retryReads)
        }
        if (entries.loadState.refresh is LoadState.Loading || entries.loadState.prepend is LoadState.Loading)
            item { Text(resources.getString(R.string.weight_loading), color = colors.muted) }
        if (!adding && editing == null && deleting == null && state.writeFailed) item { WeightWriteError(state) }
        if (entries.loadState.refresh is LoadState.NotLoading && entries.itemCount == 0) item {
            Text(resources.getString(R.string.weight_empty), color = colors.muted)
        }
        items(count = entries.itemCount, key = entries.itemKey { it.id }) { index ->
            val entry = entries[index] ?: return@items
            Card(Modifier.fillMaxWidth().testTag("weight_entry_${entry.id}"),
                colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${weightNumber(entry.kg, state.language)} kg", color = colors.text, fontWeight = FontWeight.Bold)
                    Text(entry.dayKey, color = colors.muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(enabled = !state.writing, modifier = Modifier.testTag("weight_edit_${entry.id}"),
                            onClick = { viewModel.clearWriteError(); editing = entry }) {
                            Text(resources.getString(R.string.weight_edit))
                        }
                        TextButton(enabled = !state.writing, modifier = Modifier.testTag("weight_delete_${entry.id}"),
                            onClick = { viewModel.clearWriteError(); deleting = entry }) {
                            Text(resources.getString(R.string.weight_delete), color = colors.danger)
                        }
                    }
                }
            }
        }
        if (entries.loadState.append is LoadState.Loading)
            item { Text(resources.getString(R.string.weight_loading), color = colors.muted) }
        if (entries.loadState.refresh is LoadState.Error || entries.loadState.append is LoadState.Error ||
            entries.loadState.prepend is LoadState.Error) item {
            Column {
                Text(resources.getString(R.string.weight_read_failed), color = colors.danger)
                TextButton(onClick = { entries.retry() }, modifier = Modifier.testTag("weight_page_retry")) {
                    Text(resources.getString(R.string.weight_retry))
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.height(80.dp)) }
    }
    if (adding || editing != null) {
        val entry = editing
        WeightEntryDialog(state, entry,
            onDismiss = { adding = false; editing = null; viewModel.clearWriteError() },
            onSave = { kg, date ->
                val saved = { adding = false; editing = null }
                if (entry == null) viewModel.add(kg, date, saved) else viewModel.edit(entry, kg, saved)
            })
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { if (!state.writing) deleting = null },
            title = { Text(resources.getString(R.string.weight_delete)) },
            text = {
                Column {
                    Text(resources.getString(R.string.weight_delete_question,
                        entry.dayKey, weightNumber(entry.kg, state.language)))
                    WeightWriteError(state)
                }
            },
            confirmButton = {
                TextButton(enabled = !state.writing && !state.staleEntry, modifier = Modifier.testTag("weight_confirm_delete"),
                    onClick = { viewModel.delete(entry) { deleting = null } }) {
                    Text(resources.getString(R.string.weight_delete), color = colors.danger)
                }
            },
            dismissButton = { TextButton(enabled = !state.writing, onClick = { deleting = null; viewModel.clearWriteError() }) {
                Text(resources.getString(R.string.weight_cancel))
            } }, containerColor = colors.card)
    }
}

@Composable
internal fun WeightJournalOverview(state: WeightJournalUiState, onRange: (String) -> Unit,
    onAction: () -> Unit, actionLabel: Int, onRetry: () -> Unit) {
    val resources = nativeResources(LocalContext.current, state.language)
    val colors = LocalTrainPilotColors.current
    val overview = state.overview
    val chart = overview?.chart.orEmpty()
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.card)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(resources.getString(R.string.weight_title), color = colors.text, fontWeight = FontWeight.Bold)
            Text(resources.getString(R.string.weight_current, state.currentKg?.let { "${weightNumber(it, state.language)} kg" } ?: "—"),
                color = colors.accentText)
            val start = state.startKg
            val current = state.currentKg
            if (start != null && current != null) {
                Text(resources.getString(R.string.weight_start_change, weightNumber(start, state.language),
                    weightNumber(current - start, state.language)), color = colors.muted)
            }
            val min = state.overview?.summary?.minimumKg
            val max = state.overview?.summary?.maximumKg
            if (min != null && max != null) Text(resources.getString(R.string.weight_min_max,
                weightNumber(min, state.language), weightNumber(max, state.language)), color = colors.muted,
                modifier = Modifier.testTag("weight_global_extrema"))
            state.overview?.summary?.let { summary ->
                Text(resources.getString(R.string.weight_measurements_total, summary.measurementCount),
                    color = colors.muted, modifier = Modifier.testTag("weight_measurement_count"))
            }
            Button(onClick = onAction, enabled = !state.loading && !state.writing,
                modifier = Modifier.testTag("weight_journal_action")) { Text(resources.getString(actionLabel)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("weight_range_choices")) {
                items(listOf("7", "30", "90", "365", "all")) { range ->
                    FilterChip(selected = state.range == range, enabled = !state.writing, onClick = { onRange(range) },
                        modifier = Modifier.testTag("weight_range_$range"),
                        label = { Text(if (range == "all") resources.getString(R.string.weight_all)
                            else resources.getString(R.string.weight_days, range.toInt())) })
                }
            }
            if (state.loading) Text(resources.getString(R.string.weight_loading), color = colors.muted)
            else if (state.readFailed) Column {
                Text(resources.getString(R.string.weight_read_failed), color = colors.danger)
                TextButton(onClick = onRetry, modifier = Modifier.testTag("weight_read_retry")) {
                    Text(resources.getString(R.string.weight_retry))
                }
            }
            else if (chart.size < 2) Text(resources.getString(R.string.weight_chart_empty), color = colors.muted)
            else {
                Column(Modifier.fillMaxWidth().height(110.dp).testTag("weight_chart")) { TrendCanvas(chart.map { it.averageKg }) }
                overview?.let {
                    Text(resources.getString(R.string.weight_chart_averages, it.chartMeasurementCount,
                        it.daysPerBucket), color = colors.muted, modifier = Modifier.testTag("weight_chart_coverage"))
                }
            }
        }
    }
}

@Composable
private fun WeightEntryDialog(state: WeightJournalUiState, entry: BodyWeightEntryEntity?,
    onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    val resources = nativeResources(LocalContext.current, state.language)
    val colors = LocalTrainPilotColors.current
    var weight by rememberSaveable(entry?.id) { mutableStateOf((entry?.kg ?: state.currentKg)?.toString().orEmpty()) }
    var date by rememberSaveable(entry?.id) { mutableStateOf(entry?.dayKey ?: LocalDate.now().toString()) }
    val kg = weight.replace(',', '.').toDoubleOrNull()
    val validWeight = kg != null && kg.isFinite() && kg in 30.0..300.0
    val validDate = runCatching { LocalDate.parse(date) }.isSuccess
    AlertDialog(onDismissRequest = { if (!state.writing) onDismiss() },
        title = { Text(resources.getString(if (entry == null) R.string.weight_add else R.string.weight_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, enabled = !state.writing,
                    modifier = Modifier.testTag("weight_input"), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(resources.getString(R.string.weight_kg)) }, isError = !validWeight,
                    supportingText = { if (!validWeight) Text(resources.getString(R.string.weight_invalid)) })
                if (entry == null) OutlinedTextField(value = date, onValueChange = { date = it }, enabled = !state.writing,
                    modifier = Modifier.testTag("weight_date"), singleLine = true, isError = !validDate,
                    label = { Text(resources.getString(R.string.weight_date)) })
                else Text(entry.dayKey, color = colors.muted)
                WeightWriteError(state)
            }
        },
        confirmButton = { Button(enabled = validWeight && validDate && !state.writing && !state.staleEntry,
            modifier = Modifier.testTag("weight_save"), onClick = { kg?.let { onSave(it, date) } }) {
            Text(resources.getString(R.string.weight_save))
        } },
        dismissButton = { TextButton(enabled = !state.writing, onClick = onDismiss) { Text(resources.getString(R.string.weight_cancel)) } },
        containerColor = colors.card)
}

@Composable
private fun WeightWriteError(state: WeightJournalUiState) {
    val resources = nativeResources(LocalContext.current, state.language)
    val colors = LocalTrainPilotColors.current
    if (state.staleEntry || state.writeFailed) Text(resources.getString(
        if (state.staleEntry) R.string.weight_changed else R.string.metadata_write_failed), color = colors.danger,
        modifier = Modifier.testTag("weight_write_error"))
}

private fun weightNumber(value: Double, language: String): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 }.format(value)
