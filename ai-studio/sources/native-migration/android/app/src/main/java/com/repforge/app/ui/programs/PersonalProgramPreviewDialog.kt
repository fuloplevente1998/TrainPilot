package com.repforge.app.ui.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.NativeProfileProgramPreview
import com.repforge.app.NativeProgramEditor
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramWithDays
import org.json.JSONObject

/** Compact, read-only preview; only an explicit acceptance performs a write. */
@Composable
internal fun PersonalProgramPreviewDialog(
    programs: List<ProgramWithDays>,
    catalog: List<ExerciseLibraryEntity>,
    profileJson: String,
    previews: Map<String, NativeProfileProgramPreview.Preview>,
    loading: Boolean,
    language: String,
    saving: Boolean,
    saveError: String?,
    onAccept: (NativeProfileProgramPreview.Preview) -> Unit,
    onCancel: () -> Unit
) {
    val strings = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, language)
    fun exerciseName(id: String): String {
        val ex = catalog.firstOrNull { it.id == id } ?: return id
        return when (language) { "en" -> ex.nameEn; "de" -> ex.nameDe; "ro" -> ex.nameRo; else -> ex.nameHu }.ifBlank { ex.nameHu }
    }
    fun reason(code: String): String = strings.getString(when (code) {
        "equipment" -> com.repforge.app.R.string.planner_reason_equipment
        "excluded" -> com.repforge.app.R.string.planner_reason_excluded
        "avoid-area" -> com.repforge.app.R.string.planner_reason_avoid
        "experience" -> com.repforge.app.R.string.planner_reason_experience
        "time-sets" -> com.repforge.app.R.string.planner_reason_sets
        "time" -> com.repforge.app.R.string.planner_reason_time
        else -> com.repforge.app.R.string.planner_reason_missing
    })
    val profile = remember(profileJson) { runCatching { JSONObject(profileJson) }.getOrDefault(JSONObject()) }
    val recommended = if (profile.optString("experience") == "intermediate")
        "home-level2" else "home-basic"
    var choice by remember(profileJson) { mutableStateOf(recommended) }
    val preview = previews[choice]
    val label = listOf(
        strings.getString(R.string.native_preview_00),
        strings.getString(R.string.native_preview_01),
        strings.getString(R.string.native_preview_02),
        strings.getString(R.string.native_preview_03),
        strings.getString(R.string.native_preview_04),
        strings.getString(R.string.native_preview_05),
        strings.getString(R.string.native_preview_06),
        strings.getString(R.string.native_preview_07),
        strings.getString(R.string.native_preview_08),
        strings.getString(R.string.native_preview_09),
        strings.getString(R.string.native_preview_10),
        strings.getString(R.string.native_preview_11),
        strings.getString(R.string.native_preview_12)
    )
    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text(label[0]) },
        text = {
            Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState())
                .testTag("personal_preview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((id, name) in listOf("home-basic" to label[1], "home-level2" to label[2])) {
                        FilterChip(selected = choice == id, enabled = !saving,
                            onClick = { choice = id }, label = { Text(name) },
                            modifier = Modifier.testTag("personal_template_$id"))
                    }
                }
                if (loading) {
                    Text(strings.getString(R.string.native_preview_13), modifier = Modifier.testTag("personal_preview_loading"))
                } else if (preview == null) {
                    Text(label[3])
                    Text(label[4], modifier = Modifier.testTag("personal_preview_blocked"))
                } else {
                    Text(strings.getString(R.string.native_preview_17, label[6], preview.estimateMinutes, preview.profileSnapshot.getInt("minutes"), label[5], preview.workSets))
                    val inherited = preview.history128.optJSONObject("entries")
                    if (inherited != null && inherited.length() > 0) {
                        Text(strings.getString(R.string.native_preview_14, inherited.length()), modifier = Modifier.testTag("personal_history_summary"))
                    }
                    preview.history128.optJSONObject("days")?.let { days ->
                        for (id in days.keys()) if (days.getJSONObject(id).optBoolean("retained")) {
                            val name = preview.draft.days.firstOrNull { it.id == id }?.name ?: id
                            Text(strings.getString(com.repforge.app.R.string.planner_retained_day, name),
                                modifier = Modifier.testTag("personal_retained_$id"))
                        }
                    }
                    preview.history128.optJSONObject("practiced")?.let { practiced ->
                        for (id in practiced.keys()) {
                            Text(strings.getString(com.repforge.app.R.string.planner_practiced_exercise, exerciseName(id)),
                                modifier = Modifier.testTag("personal_practiced_$id"))
                        }
                    }
                    preview.history128.optJSONArray("reductions")?.let { changes ->
                        for (i in 0 until changes.length()) {
                            val row = changes.getJSONObject(i)
                            val id = row.getString("dayId")
                            val name = preview.draft.days.firstOrNull { it.id == id }?.name ?: id
                            Text(strings.getString(com.repforge.app.R.string.planner_day_reduction, name,
                                row.getInt("fromExercises"), row.getInt("toExercises"), row.getInt("fromSets"), row.getInt("toSets"),
                                strings.getString(if (row.getString("reason") == "time") com.repforge.app.R.string.planner_day_time_reason
                                    else com.repforge.app.R.string.planner_day_equipment_reason)), modifier = Modifier.testTag("personal_day_reduction_$id"))
                        }
                    }
                    preview.draft.days.forEach { day ->
                        Text(day.name)
                        day.entries.forEach { entry ->
                            val id = NativeProgramEditor.exerciseId(entry)
                            val ex = catalog.firstOrNull { it.id == id }
                            val name = when (language) {
                                "en" -> ex?.nameEn; "de" -> ex?.nameDe
                                "ro" -> ex?.nameRo; else -> ex?.nameHu
                            }.orEmpty().ifBlank { ex?.nameHu ?: id }
                            val recipe = preview.prescriptions.getJSONObject(id)
                            Text("• $name — ${recipe.getInt("sets")} × ${recipe.getString("reps")}")
                            if (recipe.optString("variation").isNotBlank()) {
                                val text = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, language)
                                Text(com.repforge.app.ui.coach.NativeCoachAdviceText.variation(text,
                                    com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Variation(id,
                                        recipe.getString("variation"), recipe.optInt("progressionLevel"))),
                                    modifier = Modifier.testTag("personal_variation_$id"))
                            }
                            if (com.repforge.app.NativeTrainingPrescription.needsLoad(recipe))
                                Text(label[8], modifier = Modifier.testTag("personal_trial_$id"))
                            else if (recipe.optDouble("weight") > 0) {
                                Text(strings.getString(R.string.native_preview_15, recipe.optDouble("weight").toString()), modifier = Modifier.testTag("personal_inherited_$id"))
                            }
                        }
                    }
                    if (preview.reductions.isNotEmpty()) {
                        Text(strings.getString(R.string.native_preview_16, label[7], preview.reductions.size))
                        preview.reductions.forEach { removed ->
                            val name = exerciseName(removed.exerciseId)
                            val message = if (removed.reason.startsWith("substitution:")) {
                                strings.getString(com.repforge.app.R.string.planner_substitution, name,
                                    exerciseName(removed.reason.substringAfter(":"))) +
                                    if (removed.cause.isBlank()) "" else " (${reason(removed.cause)})"
                            } else "$name (${reason(removed.reason)})"
                            Text(message, modifier = Modifier.testTag("personal_change_${removed.dayId}_${removed.exerciseId}"))
                        }
                    }
                }
                if (saveError != null) Text(label[11], modifier = Modifier.testTag("personal_preview_error"))
            }
        },
        confirmButton = {
            TextButton(enabled = preview != null && !saving && !loading,
                onClick = { preview?.let(onAccept) },
                modifier = Modifier.testTag("personal_preview_accept")) { Text(label[9]) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !saving) { Text(label[10]) }
        }
    )
}
