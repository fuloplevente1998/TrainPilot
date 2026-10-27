package com.repforge.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.data.store.NativeTrainingModeChange
import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.ui.coach.NativeCoachAdviceText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/**
 * Owned by the non-virtualized Settings/Programs screen. Mutable dialog state
 * survives LazyColumn item disposal and cannot approve an unrelated proposal.
 */
internal class NativeTrainingModePickerState(initial: NativeTrainingModes.Effective) {
    val styleState = mutableStateOf(initial.trainingStyle)
    val progressionState = mutableStateOf(initial.progressionMode)
    val pendingState = mutableStateOf<NativeTrainingModeChange.Preview?>(null)
    val comparisonState = mutableStateOf<NativeTrainingModeComparison.Snapshot?>(null)
    val comparisonFailedState = mutableStateOf(false)
    val comparisonForState = mutableStateOf<NativeTrainingModeChange.Preview?>(null)
}

/** Global and active-program editors share one read-only preview/explicit-approval UI. */
@Composable
internal fun NativeTrainingModePicker(
    language: String, raw: String?, activeProgramId: String,
    scope: String, programId: String? = null, enabled: Boolean = true,
    onApprove: (NativeTrainingModeChange.Preview) -> Unit,
    onUndo: () -> Unit,
    holder: NativeTrainingModePickerState? = null,
    showCard: Boolean = true,
    showDialog: Boolean = true,
    onApproveCompared: ((NativeTrainingModeChange.Preview,
        NativeTrainingModeComparison.Snapshot) -> Unit)? = null
) {
    val colors = LocalTrainPilotColors.current
    val ctx = LocalContext.current
    val strings = remember(ctx, language) { nativeResources(ctx, language) }
    val effective = remember(raw, scope, programId, activeProgramId) {
        if (scope == "global") NativeTrainingModes.globalChoice(raw)
        else NativeTrainingModes.resolveSavedJson(raw, programId)
    }
    val owner = holder ?: remember(raw, scope, programId, activeProgramId) {
        NativeTrainingModePickerState(effective)
    }
    var style by owner.styleState
    var progression by owner.progressionState
    var pending by owner.pendingState
    var comparison by owner.comparisonState
    var comparisonFailed by owner.comparisonFailedState
    var comparisonFor by owner.comparisonForState
    if (showDialog) LaunchedEffect(pending, raw, activeProgramId, scope, programId) {
        val proposal = pending
        comparison = null
        comparisonFor = null
        comparisonFailed = false
        if (proposal != null) {
            try {
                comparison = withContext(Dispatchers.IO) {
                    NativeTrainingModeComparison.read(ctx, proposal)
                }
                comparisonFor = proposal
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                android.util.Log.w("NativeModeApproval", "Read-only comparison failed", failure)
                comparisonFailed = true
            }
        }
    }
    var styleExpanded by remember { mutableStateOf(false) }
    var progressionExpanded by remember { mutableStateOf(false) }
    val undoAllowed = remember(raw, scope, programId) {
        NativeTrainingModeChange.undoValue(raw, scope, programId) != null
    }
    fun styleLabel(value: String): String = strings.getString(if (value == "hit")
        R.string.native_modes_style_hit else R.string.native_modes_style_normal)
    fun progressionLabel(value: String): String = strings.getString(when (value) {
        "linear" -> R.string.native_modes_progression_linear
        "rir" -> R.string.native_modes_progression_rir
        else -> R.string.native_modes_progression_double
    })
    val tag = if (scope == "global") "training_modes_global" else "training_modes_program"

    if (showCard) Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.getString(R.string.native_modes_title), color = colors.text, fontWeight = FontWeight.Bold)
            Text(strings.getString(R.string.native_modes_help), color = colors.muted)
            Text(strings.getString(R.string.native_modes_style), color = colors.text)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { styleExpanded = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tag}_style")) {
                    Text(styleLabel(style), color = colors.text)
                }
                DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                    for (option in listOf("normal", "hit")) {
                        DropdownMenuItem(text = { Text(styleLabel(option)) },
                            onClick = { style = option; pending = null; styleExpanded = false },
                            modifier = Modifier.testTag("${tag}_style_$option"))
                    }
                }
            }
            Text(strings.getString(R.string.native_modes_progression), color = colors.text)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { progressionExpanded = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tag}_progression")) {
                    Text(progressionLabel(progression), color = colors.text)
                }
                DropdownMenu(expanded = progressionExpanded, onDismissRequest = { progressionExpanded = false }) {
                    for (option in listOf("double", "linear", "rir")) {
                        DropdownMenuItem(text = { Text(progressionLabel(option)) },
                            onClick = { progression = option; pending = null; progressionExpanded = false },
                            modifier = Modifier.testTag("${tag}_progression_$option"))
                    }
                }
            }
            if (scope == "program" && programId != null) {
                TextButton(
                    onClick = {
                        pending = NativeTrainingModeChange.prepareInherit(raw, activeProgramId, programId)
                    }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tag}_inherit")
                ) { Text(strings.getString(R.string.native_modes_inherit), color = colors.accentText) }
            }
            Text(
                strings.getString(if (effective.explicitChoice) R.string.native_modes_selected
                    else R.string.native_modes_legacy),
                color = colors.muted
            )
            Text(strings.getString(R.string.native_modes_notice), color = colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        pending = NativeTrainingModeChange.prepare(
                            raw, activeProgramId, scope, programId, style, progression)
                    },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tag}_preview")
                ) { Text(strings.getString(R.string.native_modes_preview)) }
                if (undoAllowed) {
                    TextButton(onClick = onUndo, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tag}_undo")) {
                        Text(strings.getString(R.string.native_modes_undo), color = colors.accentText)
                    }
                }
            }
        }
    }
    if (showDialog) pending?.let { preview ->
        // The comparison must belong to the exact dialog proposal; stale results
        // and partial/empty previews never authorize the settings CAS write.
        val ready = !comparisonFailed && comparisonFor == preview &&
            comparison?.rows?.isNotEmpty() == true &&
            preview.expectedRaw == raw && preview.expectedActiveProgramId == activeProgramId &&
            (scope != "program" || preview.programId == programId)
        AlertDialog(
            modifier = Modifier.testTag("${tag}_dialog"),
            onDismissRequest = { pending = null },
            title = { Text(strings.getString(R.string.native_modes_confirm_title), color = colors.text) },
            text = {
                // One scroll owner for the full comparison. Long names/large text
                // must not consume the viewport and collapse the exercise rows.
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.getString(R.string.native_modes_before,
                        styleLabel(preview.before.trainingStyle),
                        progressionLabel(preview.before.progressionMode)), color = colors.text)
                    Text(strings.getString(R.string.native_modes_after,
                        styleLabel(preview.after.trainingStyle),
                        progressionLabel(preview.after.progressionMode)), color = colors.text)
                    Text(strings.getString(R.string.native_modes_no_rewrite), color = colors.muted)
                    Text(strings.getString(R.string.native_modes_comparison_title),
                        color = colors.text, fontWeight = FontWeight.Bold)
                    when {
                        comparisonFailed -> Text(strings.getString(R.string.native_modes_comparison_error),
                            color = colors.dangerText,
                            modifier = Modifier.testTag("${tag}_comparison_error"))
                        comparison == null -> Text(strings.getString(R.string.native_modes_comparison_loading),
                            color = colors.muted,
                            modifier = Modifier.testTag("${tag}_comparison_loading"))
                        else -> {
                            val available = comparison!!
                            if (available.rows.isEmpty()) {
                                Text(strings.getString(R.string.native_modes_comparison_empty),
                                    color = colors.muted)
                            } else {
                                Text(strings.getString(R.string.native_modes_comparison_program, available.programName),
                                    color = colors.text)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    for ((index, row) in available.rows.withIndex()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                                .testTag("${tag}_comparison_$index"),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(strings.getString(R.string.native_modes_comparison_day, row.day),
                                                color = colors.muted)
                                            Text(row.exercise, color = colors.text,
                                                fontWeight = FontWeight.Bold)
                                            fun fmt(number: Number?) = number?.toString() ?: "—"
                                            Text(strings.getString(R.string.native_modes_comparison_sets,
                                                fmt(row.beforeSets), fmt(row.afterSets)), color = colors.text)
                                            Text(strings.getString(R.string.native_modes_comparison_reps,
                                                row.beforeReps, row.afterReps), color = colors.text)
                                            Text(strings.getString(R.string.native_modes_comparison_weight,
                                                fmt(row.beforeWeight), fmt(row.afterWeight)), color = colors.text)
                                            row.restSeconds?.let { rest ->
                                                Text(strings.getString(R.string.native_modes_comparison_rest, rest),
                                                    color = colors.muted)
                                            }
                                            Text(strings.getString(R.string.native_modes_comparison_rir, row.rir),
                                                color = colors.muted)
                                            Text(strings.getString(R.string.native_modes_comparison_reason,
                                                NativeCoachAdviceText.reason(strings, row.reason)),
                                                color = colors.muted)
                                        }
                                    }
                                }
                            }
                            Text(strings.getString(R.string.native_modes_comparison_notice),
                                color = colors.muted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enabled && ready) {
                        val completed = comparison!!
                        pending = null // consume before dispatch: no double approval
                        if (onApproveCompared != null) onApproveCompared(preview, completed)
                        else onApprove(preview)
                    }
                },
                    enabled = enabled && ready,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_approve")) {
                    Text(strings.getString(R.string.native_modes_approve))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }, modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_cancel")) {
                    Text(strings.getString(R.string.native_modes_cancel))
                }
            },
            containerColor = colors.card
        )
    }
}
