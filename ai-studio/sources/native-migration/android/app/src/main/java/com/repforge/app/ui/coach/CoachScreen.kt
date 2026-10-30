package com.repforge.app.ui.coach

import android.content.res.Resources
import com.repforge.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.theme.LocalTrainPilotColors
import androidx.compose.ui.platform.testTag
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun CoachScreen(starting: Boolean, onStart: (String, String, String?) -> Unit,
    onCalendar: () -> Unit, onHealth: () -> Unit, onProgress: () -> Unit, onPrograms: () -> Unit,
    model: CoachViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val text = remember(context, state.language) { com.repforge.app.ui.text.nativeResources(context, state.language) }
    val colors = LocalTrainPilotColors.current
    state.variationPreview?.let { preview ->
        AlertDialog(onDismissRequest = model::closeVariation,
            title = { Text(text.getString(com.repforge.app.R.string.coach_variation_preview)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(NativeCoachAdviceText.variation(text, preview.next))
                Text(text.getString(com.repforge.app.R.string.coach_variation_notice))
                val recipe = remember(preview) { org.json.JSONObject(preview.recipe) }
                Text("${recipe.getInt("sets")} × ${recipe.getString("reps")}")
                if (state.variationFailed) Text(text.getString(com.repforge.app.R.string.coach_variation_failed), color = colors.danger)
            } },
            confirmButton = { TextButton(onClick = model::saveVariation, enabled = !state.variationBusy,
                modifier = Modifier.testTag("coach_variation_save")) { Text(text.getString(com.repforge.app.R.string.coach_variation_save)) } },
            dismissButton = { TextButton(onClick = model::closeVariation, enabled = !state.variationBusy) {
                Text(text.getString(com.repforge.app.R.string.weight_cancel))
            } }, containerColor = colors.card)
    }
    val words = coachWords(text)
    val number = remember(state.language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(state.language)).apply { maximumFractionDigits = 1 } }
    fun value(n: Number?) = n?.let { number.format(it) } ?: "—"
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val readiness = state.readiness
    LazyColumn(Modifier.fillMaxSize().testTag(if (state.loading) "coach_loading" else if (state.error && state.readiness == null) "coach_error" else "coach_ready").padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("TrainPilot Coach", color = colors.text, style = MaterialTheme.typography.headlineSmall) }
        item {
            if (state.loading) CircularProgressIndicator()
            if (state.error) Text(words[0], color = colors.dangerText)
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(words[1], color = colors.text)
                    Text(if (readiness?.score == null) "—" else "${value(readiness.score)}/100",
                        style = MaterialTheme.typography.headlineLarge, color = colors.accent)
                    val mode = when(readiness?.mode) { "normal" -> 2; "reduced" -> 3; "light" -> 4; else -> 5 }
                    Text(words[mode], color = colors.text)
                    if (readiness?.pain == true) Text(words[6], color = colors.dangerText)
                }
            }
        }
        val signals = listOf(words[7] to readiness?.samsungScore, words[8] to readiness?.trainpilotScore,
            words[9] to readiness?.sleepMinutes, "HRV (ms)" to readiness?.hrvMs,
            words[10] to readiness?.baselineMs, words[11] to readiness?.load)
        for (pair in signals.chunked(2)) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, n) in pair) Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = colors.card2)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(label, color = colors.muted)
                        Text(value(n), color = colors.text, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item { Text(words[12], color = colors.muted) }
        item {
            val target = state.target
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(words[13], color = colors.text)
                    Text(target?.title ?: words[14], color = colors.text)
                    if (target != null) {
                        target.schedule?.let { Text(it.date, color = colors.muted) }
                        Button(onClick = { if (target.upcoming) onCalendar() else onStart(target.programId, target.dayId, target.schedule?.id) },
                            enabled = !starting && !state.loading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (target.upcoming) words[15] else words[16])
                        }
                    }
                }
            }
        }
        itemsIndexed(state.exercises, key = { index, row -> "$index:${row.id}" }) { index, row ->
            val advice = row.advice
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), modifier = Modifier.fillMaxWidth()
                .testTag("coach_advice_$index")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.name, color = colors.text, style = MaterialTheme.typography.titleMedium)
                    Text(NativeCoachAdviceText.label(text, advice.action), color = if (advice.action == "pain") colors.dangerText else colors.accentText)
                    Text(NativeCoachAdviceText.reason(text, advice.reason), color = colors.muted)
                    row.modePreview?.let { preview ->
                        val chosenStyle = text.getString(if (preview.trainingStyle == "hit")
                            com.repforge.app.R.string.native_modes_style_hit
                            else com.repforge.app.R.string.native_modes_style_normal)
                        val method = text.getString(when (preview.mode) {
                            "linear" -> com.repforge.app.R.string.native_modes_progression_linear
                            "rir" -> com.repforge.app.R.string.native_modes_progression_rir
                            else -> com.repforge.app.R.string.native_modes_progression_double
                        })
                        Text("$chosenStyle / $method", color = colors.accentText,
                            modifier = Modifier.testTag("coach_training_mode_${row.id}"))
                        Text(text.getString(com.repforge.app.R.string.native_modes_coach_sets,
                            preview.workingSets), color = colors.muted)
                        Text(text.getString(com.repforge.app.R.string.native_modes_coach_preview),
                            color = colors.muted)
                    }
                    Text(text.getString(com.repforge.app.R.string.coach_advice_history, advice.historyCount), color = colors.muted)
                    if (advice.target.text.isNotBlank()) Text(text.getString(com.repforge.app.R.string.coach_advice_target, advice.target.text), color = colors.text)
                    if (advice.action in setOf("increase", "reduce", "start") && advice.weight > 0)
                        Text(text.getString(when (row.loadType) {
                            "per_hand" -> com.repforge.app.R.string.coach_advice_weight_per_hand
                            "total" -> com.repforge.app.R.string.coach_advice_weight_total
                            else -> com.repforge.app.R.string.coach_advice_weight
                        }, value(advice.weight)), color = colors.text)
                    if (advice.action == "reps" && advice.nextReps.isNotEmpty()) Text(text.getString(com.repforge.app.R.string.coach_advice_reps,
                        advice.nextReps.joinToString(" / ") { value(it) }), color = colors.text)
                    if (advice.action == "variation") advice.variation?.let {
                        Text(NativeCoachAdviceText.variation(text, it), color = colors.text)
                        OutlinedButton(onClick = { model.previewVariation(row.id) }, enabled = !state.variationBusy && !starting,
                            modifier = Modifier.testTag("coach_variation_${row.id}")) {
                            Text(text.getString(com.repforge.app.R.string.coach_variation_preview))
                        }
                    }
                }
            }
        }
        if (state.variationFailed && state.variationPreview == null) item {
            Text(text.getString(com.repforge.app.R.string.coach_variation_failed), color = colors.danger)
        }
        if (state.variationSaved) item {
            Text(text.getString(com.repforge.app.R.string.coach_variation_saved), color = colors.text)
            OutlinedButton(onClick = onPrograms) { Text(text.getString(com.repforge.app.R.string.coach_variation_programs)) }
        }
        if (state.exercises.isNotEmpty()) item {
            Text(text.getString(com.repforge.app.R.string.coach_advice_read_only), color = colors.muted)
        }
        item {
            if (state.refreshFailed) Text(text.getString(com.repforge.app.R.string.coach_refresh_failed), color = colors.dangerText,
                modifier = Modifier.testTag("coach_refresh_failed"))
            OutlinedButton(onClick = { model.refreshHealth(context as? android.app.Activity) },
                enabled = !state.loading && !state.refreshing,
                modifier = Modifier.fillMaxWidth().testTag("coach_refresh_health")) {
                if (state.refreshing) CircularProgressIndicator(Modifier.size(20.dp))
                else Text(text.getString(com.repforge.app.R.string.coach_refresh_health))
            }
        }
        item { OutlinedButton(onClick = model::reload, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(words[17]) } }
        item { OutlinedButton(onClick = onHealth, modifier = Modifier.fillMaxWidth()) { Text(words[18]) } }
        item { OutlinedButton(onClick = onProgress, modifier = Modifier.fillMaxWidth()) { Text(words[19]) } }
        item { Text(words[20], color = colors.muted); Spacer(Modifier.height(96.dp)) }
    }
}

private fun coachWords(resources: Resources): List<String> = listOf(
    resources.getString(R.string.native_coach_00),
    resources.getString(R.string.native_coach_01),
    resources.getString(R.string.native_coach_02),
    resources.getString(R.string.native_coach_03),
    resources.getString(R.string.native_coach_04),
    resources.getString(R.string.native_coach_05),
    resources.getString(R.string.native_coach_06),
    resources.getString(R.string.native_coach_07),
    resources.getString(R.string.native_coach_08),
    resources.getString(R.string.native_coach_09),
    resources.getString(R.string.native_coach_10),
    resources.getString(R.string.native_coach_11),
    resources.getString(R.string.native_coach_12),
    resources.getString(R.string.native_coach_13),
    resources.getString(R.string.native_coach_14),
    resources.getString(R.string.native_coach_15),
    resources.getString(R.string.native_coach_16),
    resources.getString(R.string.native_coach_17),
    resources.getString(R.string.native_coach_18),
    resources.getString(R.string.native_coach_19),
    resources.getString(R.string.native_coach_20)
)
