package com.repforge.app.ui.programs
import androidx.compose.foundation.layout.size

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
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
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONArray
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun TrainingProgramsScreen(
    navController: NavController,
    language: String = "hu",
    viewModel: TrainingProgramsViewModel = viewModel(),
    onStartProgramDay: (String, String) -> Unit,
    requestPersonalPlanner: Boolean = false,
    onPersonalPlannerConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    // A memory-only user navigation intent; wait for the canonical VM's first snapshot.
    // The existing preview/cancel/explicit-accept path owns every read and write.
    LaunchedEffect(requestPersonalPlanner, state.programs.isNotEmpty(), state.writeFailed) {
        if (requestPersonalPlanner && (state.programs.isNotEmpty() || state.writeFailed)) {
            viewModel.openPersonalPlanner()
            onPersonalPlannerConsumed()
        }
    }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ProgramWithDays?>(null) }
    var editorToken by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    // A separate screen-owned map survives LazyColumn item removal. Keeping the
    // expanded card inside the virtualized item can make actions disappear on scroll.
    val expandedPrograms = remember { mutableStateMapOf<String, Boolean>() }
    // Keep the proposal/approval owner outside the virtualized LazyColumn item.
    // Scrolling must not cancel an in-flight comparison or dispose its dialog.
    val programModeState = remember(state.trainingModes197Json, state.activeProgramId) {
        com.repforge.app.ui.settings.NativeTrainingModePickerState(
            com.repforge.app.data.store.NativeTrainingModes.resolveSavedJson(
                state.trainingModes197Json, state.activeProgramId))
    }
    LaunchedEffect(state.savedProgramId, state.deletedProgramId) {
        if (state.savedProgramId != null || state.deletedProgramId != null) {
            editorOpen = false
            editing = null
            viewModel.clearEditorOutcome()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("program_list")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.restoreError != null && state.restorePreview == null) item {
            Text(state.restoreError.orEmpty(), color = colors.danger)
            OutlinedButton(onClick = viewModel::dismissRestoreDay,
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.getString(R.string.native_program_actions_cancel))
            }
        }
        if (state.writeFailed) item {
            Text(com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, state.language)
                .getString(com.repforge.app.R.string.metadata_write_failed), color = colors.danger)
        }

        // 1. #37 Személyes Edzéstervező nyitó kártya (Personal Planner Banner)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openPersonalPlanner() }
                    .testTag("personal_planner_open"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.emphasisBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.getString(R.string.native_program_planner),
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.getString(R.string.native_program_planner_help),
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                    }
                    Text(strings.getString(R.string.native_program_open), color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.getString(R.string.native_program_list),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.getString(R.string.native_program_available, state.programs.size),
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
        }

        item {
            OutlinedButton(onClick = {
                editing = null; editorToken = java.util.UUID.randomUUID().toString()
                editorOpen = true; viewModel.clearEditorOutcome()
            },
                enabled = !state.editorSaving, modifier = Modifier.fillMaxWidth().testTag("program_create")) {
                Text(strings.getString(R.string.native_program_create))
            }
        }

        // Training-mode overrides are settings-only, not writes into program recipes.
        if (state.programs.any { it.program.id == state.activeProgramId }) item {
            com.repforge.app.ui.settings.NativeTrainingModePicker(
                language = language,
                raw = state.trainingModes197Json,
                activeProgramId = state.activeProgramId,
                scope = "program",
                programId = state.activeProgramId,
                enabled = !state.modeSaving && !state.editorSaving,
                onApprove = viewModel::approveTrainingMode,
                onApproveCompared = viewModel::approveTrainingModeCompared,
                onUndo = { viewModel.undoTrainingMode(state.activeProgramId) },
                holder = programModeState, showDialog = false
            )
        }

        // 2. Programkártyák listája
        items(state.programs, key = { it.program.id }) { programWithDays ->
            val isActive = programWithDays.program.id == state.activeProgramId
            ProgramCardItem(
                programWithDays = programWithDays,
                isActive = isActive,
                expanded = expandedPrograms[programWithDays.program.id] ?: isActive,
                onToggleExpanded = {
                    val id = programWithDays.program.id
                    expandedPrograms[id] = !(expandedPrograms[id] ?: isActive)
                },
                language = language,
                onActivate = { viewModel.activateProgram(programWithDays.program.id) },
                onStartDay = { dayId -> onStartProgramDay(programWithDays.program.id, dayId) },
                onRestoreDay = { dayId -> viewModel.previewRestoreDay(programWithDays, dayId) },
                onEdit = {
                    editing = programWithDays; editorToken = java.util.UUID.randomUUID().toString()
                    editorOpen = true; viewModel.clearEditorOutcome()
                }
            )
        }
    }
    // This dialog belongs to the screen, not to a lazy item that can leave composition.
    if (state.programs.any { it.program.id == state.activeProgramId }) {
        com.repforge.app.ui.settings.NativeTrainingModePicker(
            language = language, raw = state.trainingModes197Json,
            activeProgramId = state.activeProgramId, scope = "program",
            programId = state.activeProgramId,
            enabled = !state.modeSaving && !state.editorSaving,
            onApprove = viewModel::approveTrainingMode,
            onApproveCompared = viewModel::approveTrainingModeCompared,
            onUndo = { viewModel.undoTrainingMode(state.activeProgramId) },
            holder = programModeState, showCard = false
        )
    }
    state.modeStatus?.let { message ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::clearModeStatus,
            title = { Text(strings.getString(R.string.native_modes_title), color = colors.text) },
            text = { Text(message, color = colors.muted) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::clearModeStatus) {
                    Text(strings.getString(R.string.native_modes_approve), color = colors.accentText)
                }
            },
            containerColor = colors.card
        )
    }
    if (state.isPersonalPlannerOpen) PersonalProgramPreviewDialog(
        programs = state.programs,
        catalog = state.exerciseChoices,
        profileJson = state.trainingProfileJson,
        previews = state.personalPreviews,
        loading = state.personalLoading,
        language = language,
        saving = state.editorSaving,
        saveError = state.editorError,
        onAccept = { preview -> viewModel.savePersonalProgram(preview, language) },
        onCancel = viewModel::closePersonalPlanner
    )
    state.restorePreview?.let { preview ->
        fun exerciseNames(items: List<String>): String = items.mapIndexed { index, raw ->
            val id = runCatching { com.repforge.app.NativeProgramEditor.exerciseId(raw) }.getOrDefault("?")
            val ex = state.exerciseChoices.firstOrNull { it.id == id }
            val localized = when (language) {
                "en" -> ex?.nameEn; "de" -> ex?.nameDe; "ro" -> ex?.nameRo
                else -> ex?.nameHu
            }.orEmpty().ifBlank { ex?.nameHu ?: id }
            "${index + 1}. $localized ($id)"
        }.joinToString("\n")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissRestoreDay,
            title = { Text(strings.getString(R.string.native_program_actions_title, preview.dayId),
                color = colors.text) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.getString(R.string.native_program_actions_scope), color = colors.muted)
                    Text(strings.getString(R.string.native_program_actions_before), color = colors.text)
                    Text(exerciseNames(preview.before), color = colors.muted)
                    Text(strings.getString(R.string.native_program_actions_after), color = colors.text)
                    Text(exerciseNames(preview.after), color = colors.muted,
                        modifier = Modifier.testTag("program_restore_factory_exercises"))
                    Text(strings.getString(R.string.native_program_actions_recipe,
                        preview.resetRecipeIds.sorted().joinToString(", ").ifEmpty { "—" }), color = colors.muted)
                    if (preview.otherDaysUsingRecipes.isNotEmpty())
                        Text(strings.getString(R.string.native_program_actions_shared,
                            preview.otherDaysUsingRecipes.joinToString(", ")), color = colors.danger)
                    state.restoreError?.let {
                        Text(it, color = colors.danger, modifier = Modifier.testTag("program_restore_error"))
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestoreDay, enabled = !state.editorSaving,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("program_restore_confirm")) {
                    Text(strings.getString(R.string.native_program_actions_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = viewModel::dismissRestoreDay,
                    enabled = !state.editorSaving,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("program_restore_cancel")) {
                    Text(strings.getString(R.string.native_program_actions_cancel))
                }
            },
            containerColor = colors.card
        )
    }
    if (editorOpen) ProgramEditorDialog(opened = editing, language = language,
        exercises = state.exerciseChoices, defaultRestSeconds = state.defaultRestSeconds,
        saving = state.editorSaving, error = state.editorError,
        onSave = { draft -> viewModel.saveProgram(editing, draft, language, editorToken) },
        onDelete = { editing?.let(viewModel::deleteProgram) },
        onCancel = { if (!state.editorSaving) {
            editorOpen = false; editing = null; viewModel.clearEditorOutcome()
        } })
}

@Composable
fun ProgramCardItem(
    programWithDays: ProgramWithDays,
    isActive: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    language: String = "hu",
    onActivate: () -> Unit,
    onStartDay: (String) -> Unit,
    onRestoreDay: (String) -> Unit = {},
    onEdit: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val prog = programWithDays.program
    fun name(hu: String, en: String, de: String, ro: String) = when (language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("program_${prog.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) colors.selectedBorder else colors.line
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.card2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(prog, "location", language), color = colors.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.card2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(prog, "level", language), color = colors.muted, fontSize = 10.sp)
                    }
                }
                if (isActive) {
                    Text(strings.getString(R.string.native_program_active), color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    OutlinedButton(
                        onClick = onActivate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text)
                    ) {
                        Text(strings.getString(R.string.native_program_select), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(name(prog.nameHu, prog.nameEn, prog.nameDe, prog.nameRo), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(prog.description, color = colors.muted, fontSize = 12.sp)
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()
                .testTag("program_edit_${prog.id}")) {
                Text(strings.getString(if (prog.builtin) R.string.native_program_copy_edit else R.string.native_program_edit))
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("program_expand_${prog.id}")
                    .clickable(onClick = onToggleExpanded),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.getString(R.string.native_program_days, programWithDays.days.size),
                    color = colors.accentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.accent
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    programWithDays.days.sortedBy { it.sortOrder }.forEach { day ->
                        val exCount = runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card2)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name(day.nameHu, day.nameEn, day.nameDe, day.nameRo), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(strings.getString(R.string.native_program_exercises, exCount), color = colors.muted, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { onStartDay(day.dayId) },
                                enabled = exCount > 0,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.onAccent)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.getString(R.string.native_program_start), color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!prog.builtin && prog.sourceTemplateId == "home-basic" &&
                                day.dayId in setOf("A", "B")) {
                                OutlinedButton(onClick = { onRestoreDay(day.dayId) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                        .testTag("program_restore_${prog.id}_${day.dayId}")) {
                                    Text(strings.getString(R.string.native_program_actions_restore),
                                        color = colors.accentText, fontSize = 11.sp)
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
