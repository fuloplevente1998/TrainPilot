package com.repforge.app.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.NativeWorkoutHealth
import com.repforge.app.R
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The two providers are explicit separate panels, never added together or silently substituted. */
@Composable
internal fun JournalHealthPanel(detail: WorkoutHistoryWithDetails, text: android.content.res.Resources,
    language: String, refreshing: Boolean, error: Int?, enabled: Boolean, refresh: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val workout = detail.workout
    val raw = remember(workout.rawJson) { parseHealth(workout.rawJson) }
    val wear = remember(workout.wearMetricsJson) { parseHealth(workout.wearMetricsJson) }
    val health = remember(workout.health240Json) { parseHealth(workout.health240Json) }
    val current = health != null && NativeWorkoutHealth.matchesWindow(health, workout)
    val locale = remember(language) { Locale.forLanguageTag(language) }
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val date = remember(locale) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale).withZone(ZoneId.systemDefault()) }
    fun time(value: String) = runCatching { date.format(Instant.parse(value)) }.getOrDefault("—")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), modifier = Modifier.fillMaxWidth().testTag("journal_health_wear")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text.getString(R.string.journal_health_wear), style = MaterialTheme.typography.titleMedium)
                Text("Health Services", color = colors.muted)
                if (wear != null && raw?.optBoolean("nativeHealthWindowStale") == true) {
                    Text(text.getString(R.string.journal_edit_stale), color = colors.muted)
                    raw.optJSONObject("nativeOriginalHealthWindow")?.let {
                        Text("${text.getString(R.string.journal_health_window)}: ${time(it.optString("started"))} – ${time(it.optString("finished"))}", color = colors.muted)
                    }
                }
                val metrics = wear?.let { NativeWorkoutHealth.metrics(it, true) }.orEmpty()
                JournalHealthMetrics(metrics, "wear", text, number)
                if (metrics.isEmpty()) Text(text.getString(R.string.journal_no_data), color = colors.muted)
                if (wear?.optBoolean("partial") == true) Text(text.getString(R.string.journal_health_partial), color = colors.muted)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.card), modifier = Modifier.fillMaxWidth().testTag("journal_health_hc")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                Text("${text.getString(R.string.journal_health_window)}: ${time(workout.started)} – ${time(workout.finished)}")
                if (!current) Text(text.getString(if (health == null) R.string.journal_health_missing else R.string.journal_health_stale),
                    color = colors.muted, modifier = Modifier.testTag("journal_health_status"))
                else {
                    val data = health!!
                    val metrics = NativeWorkoutHealth.metrics(data, false)
                    JournalHealthMetrics(metrics, "hc", text, number)
                    if (metrics.isEmpty()) Text(text.getString(R.string.journal_health_empty), color = colors.muted)
                    if (data.optBoolean("partial") || (data.optJSONArray("warnings")?.length() ?: 0) > 0)
                        Text(text.getString(R.string.journal_health_partial), color = colors.muted)
                    if (data.optBoolean("workoutEnergyProrated")) Text(text.getString(R.string.journal_health_prorated), color = colors.muted)
                    val sources = data.optJSONArray("sources")
                    val labels = data.optJSONObject("sourceLabels")
                    val names = (0 until (sources?.length() ?: 0)).map { sources!!.optString(it) }
                        .filter { it.isNotBlank() }.map { labels?.optString(it)?.takeIf(String::isNotBlank) ?: it }.distinct()
                    if (names.isNotEmpty()) Text("${text.getString(R.string.journal_health_sources)}: ${names.joinToString(", ")}", color = colors.muted)
                    Text("${text.getString(R.string.journal_health_synced)}: ${time(data.optString("syncedAt"))}", color = colors.muted)
                }
                OutlinedButton(onClick = refresh, enabled = enabled && !refreshing,
                    modifier = Modifier.fillMaxWidth().testTag("journal_health_refresh")) {
                    Text(text.getString(if (refreshing) R.string.journal_health_refreshing else R.string.journal_health_refresh))
                }
                error?.let { Text(text.getString(it), color = colors.dangerText, modifier = Modifier.testTag("journal_health_error")) }
            }
        }
        Text(text.getString(R.string.journal_health_separate_energy), color = colors.muted)
    }
}

private fun parseHealth(raw: String?): JSONObject? = raw?.let { runCatching { JSONObject(it) }.getOrNull() }

@Composable
private fun JournalHealthMetrics(metrics: Map<String, Double>, source: String, text: android.content.res.Resources, number: NumberFormat) {
    val colors = LocalTrainPilotColors.current
    for ((key, rawValue) in metrics) {
        val label = when (key) {
            "averageHeartRate" -> R.string.health_metric_averageheartrate
            "minHeartRate" -> R.string.health_metric_minheartrate
            "maxHeartRate" -> R.string.health_metric_maxheartrate
            "heartRateSamples" -> R.string.health_metric_heartratesamples
            "steps" -> R.string.health_metric_steps
            "activeCalories" -> R.string.health_metric_activecalories
            "totalCalories" -> R.string.health_metric_totalcalories
            "workoutCalories" -> R.string.journal_health_workout_energy
            "distanceMeters" -> R.string.journal_health_distance
            "activeDurationSeconds" -> R.string.journal_health_active_duration
            "exerciseMinutes" -> R.string.journal_health_exercise_minutes
            "exerciseSessionCount" -> R.string.journal_health_exercise_count
            "averageSpeedMps" -> R.string.journal_health_average_speed
            "maxSpeedMps" -> R.string.journal_health_max_speed
            else -> continue
        }
        val unit = when (key) {
            "averageHeartRate", "minHeartRate", "maxHeartRate" -> "bpm"
            "activeCalories", "totalCalories", "workoutCalories" -> "kcal"
            "distanceMeters" -> "m"
            "activeDurationSeconds" -> "s"
            "exerciseMinutes" -> "min"
            "averageSpeedMps", "maxSpeedMps" -> "km/h"
            else -> ""
        }
        val value = if (key in setOf("averageSpeedMps", "maxSpeedMps")) rawValue * 3.6 else rawValue
        if (!value.isFinite()) continue
        Column(Modifier.fillMaxWidth().testTag("journal_${source}_$key")) {
            Text(text.getString(label), style = MaterialTheme.typography.labelMedium, color = colors.muted)
            Text("${number.format(value)} $unit".trim(), modifier = Modifier.testTag("journal_${source}_${key}_value"))
        }
    }
}
