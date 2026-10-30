package com.repforge

import kotlin.math.*

enum class PrCategory226 {
    MAX_WEIGHT,
    MAX_REPS,
    MAX_VOLUME,
    ESTIMATED_1RM
}

data class PrEntry226(
    val exerciseName: String,
    val category: PrCategory226,
    val value: Double,
    val reps: Int,
    val weightKg: Double,
    val achievedAt: Long,
    val formulaUsed: String
)

data class FormulaEstimate226(
    val formulaName: String,
    val estimated1RmKg: Double
)

data class PersonalRecordBreakdown226(
    val exerciseName: String,
    val category: PrCategory226,
    val newValue: Double,
    val oldValue: Double?,
    val deltaKg: Double,
    val deltaPercent: Double,
    val reps: Int,
    val weightKg: Double,
    val formulaEstimates: List<FormulaEstimate226>,
    val localizedCategoryName: Map<String, String>,
    val localizedDescription: Map<String, String>
)

/**
 * v1.3.71: PR részletek megnyitásának és bezárásának állapotmodellje.
 */
data class PrDetailViewState226(
    val isOpen: Boolean = false,
    val activeBreakdown: PersonalRecordBreakdown226? = null
)

/**
 * PR #226 szerinti egyismétléses maximum (1RM) és rekordkalkulátor.
 */
object PersonalRecordCalculator226 {

    fun calculateEpley(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    fun calculateBrzycki(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps >= 37) return weightKg * 36.0
        if (reps == 1) return weightKg
        return weightKg * (36.0 / (37.0 - reps))
    }

    fun calculateLombardi(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (reps.toDouble().pow(0.10))
    }

    fun calculateMayhew(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return (100.0 * weightKg) / (52.2 + 41.9 * exp(-0.055 * reps))
    }

    fun calculateWathan(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return (100.0 * weightKg) / (48.8 + 53.8 * exp(-0.075 * reps))
    }

    fun getFormulaEstimates(weightKg: Double, reps: Int): List<FormulaEstimate226> {
        return listOf(
            FormulaEstimate226("Epley", calculateEpley(weightKg, reps)),
            FormulaEstimate226("Brzycki", calculateBrzycki(weightKg, reps)),
            FormulaEstimate226("Lombardi", calculateLombardi(weightKg, reps)),
            FormulaEstimate226("Mayhew", calculateMayhew(weightKg, reps)),
            FormulaEstimate226("Wathan", calculateWathan(weightKg, reps))
        )
    }

    fun evaluateSet(
        exerciseName: String,
        weightKg: Double,
        reps: Int,
        historicalPrs: List<PrEntry226>
    ): List<PersonalRecordBreakdown226> {
        if (weightKg <= 0.0 || reps <= 0) return emptyList()

        val results = mutableListOf<PersonalRecordBreakdown226>()
        val exercisePrs = historicalPrs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
        val estimates = getFormulaEstimates(weightKg, reps)

        // 1. Max Weight vizsgálat
        val prevMaxWeight = exercisePrs.firstOrNull { it.category == PrCategory226.MAX_WEIGHT }?.value
        if (prevMaxWeight == null || weightKg > prevMaxWeight) {
            val delta = weightKg - (prevMaxWeight ?: 0.0)
            val pct = if (prevMaxWeight != null && prevMaxWeight > 0) (delta / prevMaxWeight) * 100.0 else 100.0
            results.add(
                PersonalRecordBreakdown226(
                    exerciseName = exerciseName,
                    category = PrCategory226.MAX_WEIGHT,
                    newValue = weightKg,
                    oldValue = prevMaxWeight,
                    deltaKg = delta,
                    deltaPercent = pct,
                    reps = reps,
                    weightKg = weightKg,
                    formulaEstimates = estimates,
                    localizedCategoryName = mapOf(
                        "hu" to "Legnagyobb Súly (Max Weight)",
                        "en" to "Maximum Weight",
                        "de" to "Maximalgewicht",
                        "ro" to "Greutate Maximă"
                    ),
                    localizedDescription = mapOf(
                        "hu" to "Új abszolút súlycsúcs: %.1f kg!".format(weightKg),
                        "en" to "New absolute weight record: %.1f kg!".format(weightKg),
                        "de" to "Neuer Gewichtsrekord: %.1f kg!".format(weightKg),
                        "ro" to "Nou record de greutate: %.1f kg!".format(weightKg)
                    )
                )
            )
        }

        // 2. Becsült 1RM vizsgálat (Epley bázison)
        val calculated1Rm = calculateEpley(weightKg, reps)
        val prev1Rm = exercisePrs.firstOrNull { it.category == PrCategory226.ESTIMATED_1RM }?.value
        if (prev1Rm == null || calculated1Rm > prev1Rm) {
            val delta = calculated1Rm - (prev1Rm ?: 0.0)
            val pct = if (prev1Rm != null && prev1Rm > 0) (delta / prev1Rm) * 100.0 else 100.0
            results.add(
                PersonalRecordBreakdown226(
                    exerciseName = exerciseName,
                    category = PrCategory226.ESTIMATED_1RM,
                    newValue = calculated1Rm,
                    oldValue = prev1Rm,
                    deltaKg = delta,
                    deltaPercent = pct,
                    reps = reps,
                    weightKg = weightKg,
                    formulaEstimates = estimates,
                    localizedCategoryName = mapOf(
                        "hu" to "Becsült 1RM (Epley)",
                        "en" to "Estimated 1RM (Epley)",
                        "de" to "Geschätztes 1RM (Epley)",
                        "ro" to "1RM Estimat (Epley)"
                    ),
                    localizedDescription = mapOf(
                        "hu" to "Új becsült egyismétléses maximum: %.1f kg (%.1f kg x %d)".format(calculated1Rm, weightKg, reps),
                        "en" to "New estimated 1RM: %.1f kg (%.1f kg x %d)".format(calculated1Rm, weightKg, reps),
                        "de" to "Neues geschätztes 1RM: %.1f kg (%.1f kg x %d)".format(calculated1Rm, weightKg, reps),
                        "ro" to "Nou 1RM estimat: %.1f kg (%.1f kg x %d)".format(calculated1Rm, weightKg, reps)
                    )
                )
            )
        }

        // 3. Max Volumen vizsgálat
        val volume = weightKg * reps
        val prevVolume = exercisePrs.firstOrNull { it.category == PrCategory226.MAX_VOLUME }?.value
        if (prevVolume == null || volume > prevVolume) {
            val delta = volume - (prevVolume ?: 0.0)
            val pct = if (prevVolume != null && prevVolume > 0) (delta / prevVolume) * 100.0 else 100.0
            results.add(
                PersonalRecordBreakdown226(
                    exerciseName = exerciseName,
                    category = PrCategory226.MAX_VOLUME,
                    newValue = volume,
                    oldValue = prevVolume,
                    deltaKg = delta,
                    deltaPercent = pct,
                    reps = reps,
                    weightKg = weightKg,
                    formulaEstimates = estimates,
                    localizedCategoryName = mapOf(
                        "hu" to "Legnagyobb Sorozat Volumen",
                        "en" to "Best Set Volume",
                        "de" to "Maximales Satzvolumen",
                        "ro" to "Volum Maxim pe Serie"
                    ),
                    localizedDescription = mapOf(
                        "hu" to "Új sorozat volumen csúcs: %.1f kg (%.1f kg x %d)".format(volume, weightKg, reps),
                        "en" to "New set volume record: %.1f kg (%.1f kg x %d)".format(volume, weightKg, reps),
                        "de" to "Neuer Satzvolumenrekord: %.1f kg (%.1f kg x %d)".format(volume, weightKg, reps),
                        "ro" to "Nou record de volum: %.1f kg (%.1f kg x %d)".format(volume, weightKg, reps)
                    )
                )
            )
        }

        return results
    }
}
