package com.repforge.app.ui.programs

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import android.content.res.Resources
import com.repforge.app.NativeExerciseDemo
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.text.Normalizer
import java.util.Locale

private data class LibraryWords(
    val title: String, val search: String, val favorites: String, val all: String,
    val count: String, val empty: String, val back: String, val detail: String,
    val video: String, val searchVideo: String, val unverified: String, val noVideo: String,
    val videoError: String, val loadError: String, val saveError: String,
    val addFavorite: String, val removeFavorite: String, val sets: String, val reps: String,
    val muscle: String, val demoCredit: String
)

private fun words(resources: Resources): LibraryWords = LibraryWords(
    resources.getString(R.string.native_library_00),
    resources.getString(R.string.native_library_01),
    resources.getString(R.string.native_library_02),
    resources.getString(R.string.native_library_03),
    resources.getString(R.string.native_library_04),
    resources.getString(R.string.native_library_05),
    resources.getString(R.string.native_library_06),
    resources.getString(R.string.native_library_07),
    resources.getString(R.string.native_library_08),
    resources.getString(R.string.native_library_09),
    resources.getString(R.string.native_library_10),
    resources.getString(R.string.native_library_11),
    resources.getString(R.string.native_library_12),
    resources.getString(R.string.native_library_13),
    resources.getString(R.string.native_library_14),
    resources.getString(R.string.native_library_15),
    resources.getString(R.string.native_library_16),
    resources.getString(R.string.native_library_17),
    resources.getString(R.string.native_library_18),
    resources.getString(R.string.native_library_19),
    resources.getString(R.string.native_library_20)
)

private fun localized(ex: ExerciseLibraryEntity, lang: String): String =
    when (lang) { "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu }
        .ifBlank { ex.nameHu }

private fun description(ex: ExerciseLibraryEntity, lang: String): String =
    when (lang) { "en" -> ex.notesEn; "de" -> ex.notesDe; "ro" -> ex.notesRo; else -> ex.notesHu }
        .ifBlank { ex.notesHu }

private fun key(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

private val muscles = listOf("chest", "back", "shoulders", "arms", "core", "legs", "cardio")

private fun muscleName(value: String, resources: Resources): String {
    val at = muscles.indexOf(value)
    if (at < 0) return value
    return resources.getString(intArrayOf(
        R.string.native_library_21,
        R.string.native_library_22,
        R.string.native_library_23,
        R.string.native_library_24,
        R.string.native_library_25,
        R.string.native_library_26,
        R.string.native_library_27
    )[at])
}

@Composable
internal fun ExerciseLibraryScreen(language: String, model: ExerciseLibraryViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val labels = words(strings)
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var muscle by rememberSaveable { mutableStateOf("") }
    var openingError by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseLibraryEntity?>(null) }
    androidx.compose.runtime.LaunchedEffect(state.savedId, state.deletedId) {
        if (state.savedId != null || state.deletedId != null) {
            selectedId = state.savedId
            editorOpen = false
            editingExercise = null
            model.clearEditOutcome()
        }
    }

    val selected = state.rows.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selectedId != null || editorOpen) {
        if (editorOpen && !state.saving) editorOpen = false
        else if (!editorOpen) { selectedId = null; openingError = false }
    }
    val visible = remember(state.rows, state.favorites, query, favoritesOnly, muscle, language) {
        val needle = key(query)
        state.rows.filter { ex ->
            (!favoritesOnly || ex.id in state.favorites) &&
                (muscle.isEmpty() || ex.primaryMuscle == muscle) &&
                (needle.isEmpty() || needle in key(listOf(ex.nameHu, ex.nameEn, ex.nameDe, ex.nameRo).joinToString(" ")))
        }.sortedWith(compareBy<ExerciseLibraryEntity> { it.id !in state.favorites }
            .thenBy { key(localized(it, language)) })
    }
    fun open(link: NativeExerciseDemo.Link) {
        openingError = false
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                .addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { openingError = true }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.error) {
            Text(labels.loadError, color = colors.danger, modifier = Modifier.testTag("exercise_library_error"))
            TextButton(onClick = model::reload) { Text(labels.back) }
        } else if (selected != null) {
            val demo = remember(selected) { NativeExerciseDemo.resolve(selected) }
            TextButton(onClick = { selectedId = null; openingError = false }) { Text(labels.back) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("exercise_library_detail"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(localized(selected, language), color = colors.text, fontWeight = FontWeight.Bold)
                Text("${labels.muscle}: ${muscleName(selected.primaryMuscle, strings)} • ${labels.sets}: ${selected.defaultSets} • ${labels.reps}: ${selected.defaultReps}",
                    color = colors.muted)
                Text(description(selected, language), color = colors.text)
                if (selected.custom) {
                    OutlinedButton(onClick = { editingExercise = selected; editorOpen = true; model.clearEditOutcome() },
                        modifier = Modifier.testTag("custom_exercise_edit")) {
                        Text(strings.getString(R.string.native_library_28))
                    }
                }
                TextButton(onClick = { model.toggleFavorite(selected.id) }, enabled = !state.saving,
                    modifier = Modifier.testTag("exercise_library_detail_favorite")) {
                    Text(if (selected.id in state.favorites) "★ ${labels.removeFavorite}" else "☆ ${labels.addFavorite}")
                }
                when (demo?.kind) {
                    NativeExerciseDemo.Kind.VIDEO -> {
                        Button(onClick = { open(demo) }, modifier = Modifier.fillMaxWidth().testTag("exercise_library_video")) {
                            Text(labels.video)
                        }
                        if (demo.credit.isNotBlank()) Text("${labels.demoCredit}: ${demo.credit}", color = colors.muted)
                    }
                    NativeExerciseDemo.Kind.UNVERIFIED_SEARCH -> {
                        Text(labels.unverified, color = colors.muted, modifier = Modifier.testTag("exercise_library_unverified"))
                        OutlinedButton(onClick = { open(demo) }, modifier = Modifier.fillMaxWidth().testTag("exercise_library_search_video")) {
                            Text(labels.searchVideo)
                        }
                    }
                    null -> Text(labels.noVideo, color = colors.muted)
                }
                if (openingError) Text(labels.videoError, color = colors.danger,
                    modifier = Modifier.testTag("exercise_library_video_error"))
                if (state.writeFailed) Text(labels.saveError, color = colors.danger,
                    modifier = Modifier.testTag("exercise_library_favorite_error"))
                Spacer(Modifier.height(80.dp))
            }
        } else {
            Text(labels.title, color = colors.text, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { editingExercise = null; editorOpen = true; model.clearEditOutcome() },
                modifier = Modifier.fillMaxWidth().testTag("custom_exercise_create")) {
                Text(strings.getString(R.string.native_library_29))
            }
            Text("${state.rows.size} ${labels.count}", color = colors.muted,
                modifier = Modifier.testTag("exercise_library_count"))
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text(labels.search) }, modifier = Modifier.fillMaxWidth().testTag("exercise_library_search"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text(labels.favorites) }, modifier = Modifier.testTag("exercise_library_favorites"))
                Text("${visible.size} / ${state.rows.size}", color = colors.muted)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = muscle.isEmpty(), onClick = { muscle = "" }, label = { Text(labels.all) })
                for (part in muscles) FilterChip(selected = muscle == part, onClick = { muscle = part },
                    label = { Text(muscleName(part, strings)) }, modifier = Modifier.testTag("exercise_library_muscle_$part"))
            }
            if (state.writeFailed) Text(labels.saveError, color = colors.danger,
                modifier = Modifier.testTag("exercise_library_favorite_error"))
            if (visible.isEmpty()) Text(labels.empty, color = colors.muted)
            else LazyColumn(Modifier.weight(1f).testTag("exercise_library_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { ex ->
                    Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { selectedId = ex.id; openingError = false }
                                .padding(8.dp).testTag("exercise_library_row_${ex.id}")) {
                                Text(localized(ex, language), color = colors.text)
                                Text(muscleName(ex.primaryMuscle, strings), color = colors.muted)
                            }
                            TextButton(onClick = { model.toggleFavorite(ex.id) }, enabled = !state.saving,
                                modifier = Modifier.testTag("exercise_library_favorite_${ex.id}").semantics {
                                    contentDescription = if (ex.id in state.favorites) labels.removeFavorite else labels.addFavorite
                                }) {
                                Text(if (ex.id in state.favorites) "★" else "☆", color = colors.accentText)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    if (editorOpen) {
        CustomExerciseDialog(exercise = editingExercise, language = language,
            saving = state.saving, error = state.editError,
            onSave = { draft -> model.saveCustom(editingExercise?.id, draft, language, editingExercise?.rawJson) },
            onDelete = { editingExercise?.let { model.deleteCustom(it.id, it.rawJson) } },
            onCancel = { if (!state.saving) { editorOpen = false; editingExercise = null; model.clearEditOutcome() } })
    }
}
