package com.repforge.app

import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
import android.health.connect.datatypes.BloodGlucoseRecord
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BodyFatRecord
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import android.health.connect.datatypes.InstantRecord
import android.health.connect.datatypes.IntervalRecord
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.RestingHeartRateRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SpeedRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.TotalCaloriesBurnedRecord
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.WeightRecord
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min

/** Raw, source-attributed HC records. Daily HC aggregates are stored separately. */
@RequiresApi(34)
internal object HealthConnectJournal {

    @JvmStatic
    @Throws(Exception::class)
    fun serialize(records: List<Record>, ownPackage: String): JSONArray {
        val out = JSONArray()
        for (r in records) {
            if (r.metadata.dataOrigin.packageName != ownPackage) {
                out.put(serialize(r))
            }
        }
        return out
    }

    @Throws(Exception::class)
    private fun zone(out: JSONObject, key: String, offset: ZoneOffset?) {
        if (offset != null) out.put(key, offset.totalSeconds)
    }

    private fun asleep(type: Int): Boolean {
        return type == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING ||
            type == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT ||
            type == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP ||
            type == SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM
    }

    @JvmStatic
    @Throws(Exception::class)
    fun serialize(r: Record): JSONObject {
        val start: Long
        val end: Long
        val d = JSONObject()
        val out = JSONObject()
            .put("channel", HealthJournalStore.HC)
            .put("source", r.metadata.dataOrigin.packageName)
            .put("type", r.javaClass.simpleName)
            .put("recordId", r.metadata.id)
            .put("lastModifiedMs", r.metadata.lastModifiedTime.toEpochMilli())

        when (r) {
            is IntervalRecord -> {
                start = r.startTime.toEpochMilli()
                end = r.endTime.toEpochMilli()
                zone(out, "startZoneOffsetSeconds", r.startZoneOffset)
                zone(out, "endZoneOffsetSeconds", r.endZoneOffset)
            }
            is InstantRecord -> {
                start = r.time.toEpochMilli()
                end = start
                zone(out, "zoneOffsetSeconds", r.zoneOffset)
            }
            else -> throw IllegalArgumentException("Unsupported HC record")
        }

        out.put("startMs", start)
            .put("endMs", end)
            .put("day", HealthJournalStore.day(if (r is SleepSessionRecord) end else start))

        when (r) {
            is StepsRecord -> {
                d.put("value", r.count).put("unit", "steps")
            }
            is ActiveCaloriesBurnedRecord -> {
                d.put("value", r.energy.inCalories / 1000.0)
                    .put("unit", "kcal")
                    .put("energyType", "active")
            }
            is TotalCaloriesBurnedRecord -> {
                d.put("value", r.energy.inCalories / 1000.0)
                    .put("unit", "kcal")
                    .put("energyType", "total")
            }
            is DistanceRecord -> {
                d.put("value", r.distance.inMeters).put("unit", "m")
            }
            is RestingHeartRateRecord -> {
                d.put("value", r.beatsPerMinute).put("unit", "bpm")
            }
            is HeartRateVariabilityRmssdRecord -> {
                d.put("value", r.heartRateVariabilityMillis).put("unit", "ms")
            }
            is WeightRecord -> {
                d.put("value", r.weight.inGrams / 1000.0).put("unit", "kg")
            }
            is BodyFatRecord -> {
                d.put("value", r.percentage.value).put("unit", "%")
            }
            is OxygenSaturationRecord -> {
                d.put("value", r.percentage.value).put("unit", "%")
            }
            is Vo2MaxRecord -> {
                d.put("value", r.vo2MillilitersPerMinuteKilogram)
                    .put("unit", "ml/kg/min")
                    .put("method", r.measurementMethod)
            }
            is BloodPressureRecord -> {
                d.put("systolic", r.systolic.inMillimetersOfMercury)
                    .put("diastolic", r.diastolic.inMillimetersOfMercury)
                    .put("unit", "mmHg")
            }
            is BloodGlucoseRecord -> {
                d.put("value", r.level.inMillimolesPerLiter).put("unit", "mmol/L")
            }
            is RespiratoryRateRecord -> {
                d.put("value", r.rate).put("unit", "/min")
            }
            is HeartRateRecord -> {
                val samples = JSONArray()
                for (s in r.samples) {
                    samples.put(
                        JSONObject()
                            .put("timeMs", s.time.toEpochMilli())
                            .put("value", s.beatsPerMinute)
                    )
                }
                d.put("samples", samples).put("unit", "bpm")
            }
            is SpeedRecord -> {
                val samples = JSONArray()
                for (s in r.samples) {
                    samples.put(
                        JSONObject()
                            .put("timeMs", s.time.toEpochMilli())
                            .put("value", s.speed.inMetersPerSecond)
                    )
                }
                d.put("samples", samples).put("unit", "m/s")
            }
            is SleepSessionRecord -> {
                val stages = JSONArray()
                val intervals = ArrayList<LongArray>()
                for (s in r.stages) {
                    val a = max(start, s.startTime.toEpochMilli())
                    val b = min(end, s.endTime.toEpochMilli())
                    stages.put(
                        JSONObject()
                            .put("startMs", a)
                            .put("endMs", b)
                            .put("type", s.type)
                            .put("isAsleep", asleep(s.type))
                    )
                    if (asleep(s.type) && b > a) {
                        intervals.add(longArrayOf(a, b))
                    }
                }
                intervals.sortBy { it[0] }
                var total = 0L
                var a = -1L
                var b = -1L
                for (span in intervals) {
                    if (a < 0L) {
                        a = span[0]
                        b = span[1]
                    } else if (span[0] <= b) {
                        b = max(b, span[1])
                    } else {
                        total += b - a
                        a = span[0]
                        b = span[1]
                    }
                }
                if (a >= 0L) total += b - a
                // A stage-less session is explicitly a session duration, not measured sleep.
                d.put("value", (if (r.stages.isEmpty()) end - start else total) / 60_000.0)
                    .put("unit", "min")
                    .put("durationKind", if (r.stages.isEmpty()) "session" else "asleep_stages")
                    .put("stages", stages)
            }
            is ExerciseSessionRecord -> {
                d.put("value", Duration.between(r.startTime, r.endTime).toMillis() / 60_000.0)
                    .put("unit", "min")
                    .put("exerciseType", r.exerciseType)
            }
            else -> throw IllegalArgumentException("Unsupported HC record type")
        }
        return out.put("data", d)
    }
}
