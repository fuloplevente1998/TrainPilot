package com.repforge.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.NativeOutdoorStart
import com.repforge.app.NativeExerciseFavorites
import com.repforge.app.NativeExerciseDemo
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.theme.LocalTrainPilotColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Selection never starts a session or changes consent; only the explicit confirmation does. */
@Composable
internal fun QuickWorkoutScreen(outdoorOnly: Boolean, starting: Boolean,
    onStart: (String, Boolean) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val scope = rememberCoroutineScope()
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var favoriteSaving by remember { mutableStateOf(false) }
    var favoriteError by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf<Triple<List<ExerciseLibraryEntity>, TrainPilotUserSettings, List<String>>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<ExerciseLibraryEntity?>(null) }
    var demoOpenFailed by remember(selected?.id) { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            loaded = withContext(Dispatchers.IO) {
                val db = TrainPilotDatabase.getInstance(context)
                val exercises = db.exerciseLibraryDao().getAllExercises()
                val settings = TrainPilotSettingsStore(context).current()
                Triple(exercises, settings, NativeExerciseFavorites.load(db))
            }
            favorites = loaded?.third.orEmpty().toSet()
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = nativeResources(context, loaded?.second?.language ?: "hu").getString(R.string.native_quick_library_error) }
    }
    val settings = loaded?.second ?: TrainPilotUserSettings()
    val resources = remember(context, settings.language) { nativeResources(context, settings.language) }
    val text = quickWords(resources)
    fun name(ex: ExerciseLibraryEntity) = when (settings.language) {
        "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu
    }.ifBlank { ex.nameHu }
    var recordRoute by remember(selected?.id) { mutableStateOf(settings.gpsStartChoices216[selected?.id] == true) }
    BackHandler(enabled = !starting) { if (selected != null) selected = null else onCancel() }
    val list = remember(loaded?.first, query, outdoorOnly, settings.language, favorites, favoritesOnly) {
        val needle = searchKey(query)
        loaded?.first.orEmpty().filter { ex ->
            (!favoritesOnly || ex.id in favorites) &&
                (!outdoorOnly || NativeOutdoorStart.eligible(ex.id, ex.measurementType, ex.repUnit)) &&
                needle in searchKey(listOf(ex.nameHu, ex.nameEn, ex.nameDe, ex.nameRo).joinToString(" "))
        }.sortedWith(compareBy<ExerciseLibraryEntity> { it.id !in favorites }.thenBy { name(it) })
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (outdoorOnly) text[1] else text[0], style = MaterialTheme.typography.titleLarge, color = colors.text)
        Text(text[2], color = colors.muted)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(text[3]) },
            modifier = Modifier.fillMaxWidth().testTag("exercise_search"), enabled = !starting, singleLine = true)
        val favText = quickFavoriteWords(resources)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly },
                enabled = !starting, label = { Text(favText[0]) }, modifier = Modifier.testTag("exercise_favorites_filter"))
            Text("${favorites.size} ★", color = colors.muted)
        }
        if (favoriteError) Text(favText[3], color = colors.danger,
            modifier = Modifier.testTag("exercise_favorite_error"))
        when {
            error != null -> Text(error!!, color = colors.text)
            loaded == null -> CircularProgressIndicator()
            list.isEmpty() -> Text(text[4], color = colors.text)
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { ex ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { selected = ex }, enabled = !starting,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("exercise_select_${ex.id}")) { Text(name(ex)) }
                        TextButton(onClick = {
                            if (favoriteSaving || starting) return@TextButton
                            favoriteSaving = true
                            favoriteError = false
                            scope.launch {
                                try {
                                    val next = withContext(Dispatchers.IO) {
                                        NativeExerciseFavorites.toggle(context, TrainPilotDatabase.getInstance(context), ex.id)
                                    }
                                    favorites = next.toSet()
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (_: Exception) { favoriteError = true }
                                finally { favoriteSaving = false }
                            }
                        }, enabled = !starting && !favoriteSaving,
                            modifier = Modifier.testTag("exercise_favorite_${ex.id}").semantics {
                                contentDescription = if (ex.id in favorites) favText[2] else favText[1]
                            }) {
                            Text(if (ex.id in favorites) "★" else "☆",
                                color = if (ex.id in favorites) colors.accentText else colors.muted)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onCancel, enabled = !starting, modifier = Modifier.heightIn(min = 48.dp)) { Text(text[5]) }
    }
    selected?.let { ex ->
        val demo = remember(ex) { NativeExerciseDemo.resolve(ex) }
        val demoWords = quickDemoWords(resources)
        AlertDialog(onDismissRequest = { if (!starting) selected = null }, title = { Text(name(ex)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text[6])
                if (demo != null) {
                    TextButton(onClick = {
                        demoOpenFailed = false
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(demo.url))
                                .addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) { demoOpenFailed = true }
                    }, enabled = !starting, modifier = Modifier.testTag("quick_exercise_video")) {
                        Text(if (demo.kind == NativeExerciseDemo.Kind.VIDEO) demoWords[0] else demoWords[1])
                    }
                    if (demo.kind == NativeExerciseDemo.Kind.UNVERIFIED_SEARCH) Text(demoWords[2])
                    if (demoOpenFailed) Text(demoWords[3], color = colors.danger)
                }
                if (NativeOutdoorStart.eligible(ex.id, ex.measurementType, ex.repUnit)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !starting) { recordRoute = !recordRoute },
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordRoute, onCheckedChange = { recordRoute = it }, enabled = !starting)
                        Text(text[7])
                    }
                    Text(text[8])
                    Text(text[9])
                    Text(text[10])
                }
            }
        }, confirmButton = {
            TextButton(onClick = { onStart(ex.id, recordRoute) }, enabled = !starting) {
                Text(if (starting) text[12] else text[11])
            }
        }, dismissButton = { TextButton(onClick = { selected = null }, enabled = !starting) { Text(text[5]) } })
    }
}

private fun searchKey(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT).trim()

private fun quickWords(res: android.content.res.Resources): List<String> = listOf(
    res.getString(R.string.native_quick_label_00),
    res.getString(R.string.native_quick_label_01),
    res.getString(R.string.native_quick_label_02),
    res.getString(R.string.native_quick_label_03),
    res.getString(R.string.native_quick_label_04),
    res.getString(R.string.native_quick_label_05),
    res.getString(R.string.native_quick_label_06),
    res.getString(R.string.native_quick_label_07),
    res.getString(R.string.native_quick_label_08),
    res.getString(R.string.native_quick_label_09),
    res.getString(R.string.native_quick_label_10),
    res.getString(R.string.native_quick_label_11),
    res.getString(R.string.native_quick_label_12)
)

private fun quickFavoriteWords(res: android.content.res.Resources): List<String> = listOf(
    res.getString(R.string.native_quick_favorite_00),
    res.getString(R.string.native_quick_favorite_01),
    res.getString(R.string.native_quick_favorite_02),
    res.getString(R.string.native_quick_favorite_03)
)

private fun quickDemoWords(res: android.content.res.Resources): List<String> = listOf(
    res.getString(R.string.native_quick_demo_00),
    res.getString(R.string.native_quick_demo_01),
    res.getString(R.string.native_quick_demo_02),
    res.getString(R.string.native_quick_demo_03)
)
