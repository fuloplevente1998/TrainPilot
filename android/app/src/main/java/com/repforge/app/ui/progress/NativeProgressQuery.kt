package com.repforge.app.ui.progress

import com.repforge.app.data.db.ProgressMuscleSummary
import com.repforge.app.data.db.WorkoutHistoryDao
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.max

/** Progress-only projections: one exercise at a time; no full journal graphs, photos or GPS. */
internal object NativeProgressQuery {
    private const val PAGE = 256
    private val groups = listOf("chest", "back", "shoulders", "arms", "core", "legs")

    fun muscle(rows: List<ProgressMuscleSummary>, language: String): List<MuscleLoadUiModel> {
        val counts = groups.associateWith { 0 }.toMutableMap()
        for (row in rows) {
            val key = if (row.muscleKey == "cardio") "legs" else row.muscleKey
            if (key in counts && row.setsCount > 0)
                counts[key] = (counts.getValue(key).toLong() + row.setsCount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
        val labels = when (language) {
            "en" -> listOf("Chest", "Back", "Shoulders", "Arms", "Core", "Legs")
            "de" -> listOf("Brust", "Rücken", "Schultern", "Arme", "Rumpf", "Beine")
            "ro" -> listOf("Piept", "Spate", "Umeri", "Brațe", "Trunchi", "Picioare")
            else -> listOf("Mellizom", "Hátizom", "Vállizom", "Karizmok", "Törzs / Has", "Lábizmok")
        }
        val highest = max(1, counts.values.maxOrNull() ?: 0)
        return groups.mapIndexed { index, key ->
            val count = counts.getValue(key)
            MuscleLoadUiModel(key, labels[index], count, (count.toFloat() / highest).coerceIn(0f, 1f))
        }
    }

    suspend fun trend(dao: WorkoutHistoryDao, exerciseId: String): List<Exercise1RmPointUiModel> {
        if (exerciseId.isBlank()) return emptyList()
        var beforeEpoch: Long? = null
        var beforeWorkout = ""
        var beforeSet = Long.MAX_VALUE
        val best = LinkedHashMap<String, Exercise1RmPointUiModel>()
        do {
            currentCoroutineContext().ensureActive()
            val page = dao.pageProgressExerciseSets(exerciseId, beforeEpoch, beforeWorkout, beforeSet, PAGE)
            if (page.isEmpty()) break
            for (row in page) {
                val reps = row.reps.toIntOrNull() ?: continue
                if (reps <= 0 || !row.weightKg.isFinite() || row.weightKg <= 0.0) continue
                val result = row.weightKg * (1.0 + reps / 30.0)
                if (!result.isFinite() || result <= 0.0) continue
                val old = best[row.workoutId]
                if (old == null || result > old.e1rmKg) {
                    best[row.workoutId] = Exercise1RmPointUiModel(
                        row.started.take(10), row.weightKg, reps, result)
                }
            }
            val last = page.last()
            beforeEpoch = last.startedEpochMs
            beforeWorkout = last.workoutId
            beforeSet = last.setRowId
        } while (page.size == PAGE)
        return best.values.reversed()
    }
}
