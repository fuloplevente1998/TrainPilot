package com.repforge.app.ui.health

import android.content.Context
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

internal object NativeHealthMetrics {
    data class Definition(val key: String, val label: Int, val unit: String, val decimals: Int)
    val definitions = listOf(
        Definition("steps", R.string.health_metric_steps, "", 0),
        Definition("activeCalories", R.string.health_metric_activecalories, "kcal", 0),
        Definition("totalCalories", R.string.health_metric_totalcalories, "kcal", 0),
        Definition("distanceMeters", R.string.health_metric_distancemeters, "m", 0),
        Definition("averageHeartRate", R.string.health_metric_averageheartrate, "bpm", 0),
        Definition("restingHeartRate", R.string.health_metric_restingheartrate, "bpm", 0),
        Definition("minHeartRate", R.string.health_metric_minheartrate, "bpm", 0),
        Definition("maxHeartRate", R.string.health_metric_maxheartrate, "bpm", 0),
        Definition("heartRateSamples", R.string.health_metric_heartratesamples, "", 0),
        Definition("hrvRmssdMs", R.string.health_metric_hrvrmssdms, "ms", 0),
        Definition("weightKg", R.string.health_metric_weightkg, "kg", 1),
        Definition("bodyFatPercent", R.string.health_metric_bodyfatpercent, "%", 1),
        Definition("bodyFatMassKg", R.string.health_metric_bodyfatmasskg, "kg", 1),
        Definition("skeletalMuscleMassKg", R.string.health_metric_skeletalmusclemasskg, "kg", 1),
        Definition("fatFreeMassKg", R.string.health_metric_fatfreemasskg, "kg", 1),
        Definition("totalBodyWaterLiters", R.string.health_metric_totalbodywaterliters, "L", 1),
        Definition("oxygenSaturationPercent", R.string.health_metric_oxygensaturationpercent, "%", 1),
        Definition("vo2Max", R.string.health_metric_vo2max, "", 1),
        Definition("energyScore", R.string.health_metric_energyscore, "/100", 0),
        Definition("sleepScore", R.string.health_metric_sleepscore, "/100", 0),
        Definition("bloodPressureSystolic", R.string.health_metric_bloodpressuresystolic, "mmHg", 0),
        Definition("bloodPressureDiastolic", R.string.health_metric_bloodpressurediastolic, "mmHg", 0),
        Definition("bloodGlucoseMmolL", R.string.health_metric_bloodglucosemmoll, "mmol/L", 1),
        Definition("respiratoryRate", R.string.health_metric_respiratoryrate, "/min", 1),
        Definition("basalMetabolicRateKcal", R.string.health_metric_basalmetabolicratekcal, "kcal", 0),
        Definition("bodyMassIndex", R.string.health_metric_bodymassindex, "", 1)
    )
    /** Studio's six compact overview tiles, selected only from genuine day projections.
     * This is a view projection: no fallback to past days, calculated values, or DEMO metrics.
     * Every original metric remains available in the detailed 26-reading list.
     */
    internal val overviewKeys = listOf(
        "sleepScore", "activeCalories", "averageHeartRate",
        "steps", "weightKg", "energyScore"
    )

    fun overview(metrics: List<HealthMetricItemUiModel>): List<HealthMetricItemUiModel> =
        overviewKeys.mapNotNull { key -> metrics.firstOrNull { it.key == key } }

    fun parse(context: Context, day: JSONObject, language: String): List<HealthMetricItemUiModel> {
        val text = nativeResources(context, language)
        val providers = day.optJSONObject("metricProviders")
        return definitions.map { d ->
            val raw = day.opt(d.key)
            val number = when (raw) { is Number -> raw.toDouble(); is String -> raw.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull(); else -> null }
            val available = number?.isFinite() == true
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = d.decimals }
            HealthMetricItemUiModel(d.key, text.getString(d.label), if (available) formatter.format(number) else "—", d.unit,
                if (available) providers?.optString(d.key)?.takeIf { it.isNotBlank() } ?: day.optString("channel")
                else text.getString(R.string.health_history_no_data), available)
        }
    }
}
