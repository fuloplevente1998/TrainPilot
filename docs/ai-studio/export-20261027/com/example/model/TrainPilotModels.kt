package com.example.model

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    HU("HU", "Magyar", "🇭🇺"),
    EN("EN", "English", "🇬🇧"),
    DE("DE", "Deutsch", "🇩🇪"),
    RO("RO", "Română", "🇷🇴")
}

enum class MainTab {
    HOME,
    WORKOUT,
    LOG,
    PROGRESS,
    HEALTH
}

/**
 * Sub-screens that can be pushed on top of the main tabs, matching V01..V24 from the mockups and main app.
 * Back navigation pops back to the previous screen or tab while preserving state.
 */
enum class SubScreen {
    NONE,
    PROGRAMS_LIST,          // 1/4 #3 Programok
    PROGRAM_EDITOR,         // 4/4 #6 Program szerkesztése
    CALENDAR_PLANNER,       // 1/4 #4 Naptár + main Újratervezés
    EXERCISE_LIBRARY,       // 1/4 #5 Gyakorlatok
    EXERCISE_DETAIL,        // 1/4 #6 Gyakorlat részletei
    ACTIVE_STRENGTH,        // 2/4 #1 Súlyzós edzés
    ACTIVE_TIMED,           // 2/4 #2 Időalapú edzés (Plank)
    ACTIVE_BILATERAL,       // 2/4 #3 Kétoldali mérés (Oldalsó plank)
    ACTIVE_OUTDOOR_GPS,     // 2/4 #4 Kültéri mérés (Futás GPS)
    INTERVAL_SETTINGS,      // 2/4 #5 Intervallumok beállítása
    WORKOUT_SUMMARY,        // 2/4 #6 Edzés befejezése / összegzése
    LOG_WORKOUT_DETAIL,     // 3/4 #2 Edzés részletei
    LOG_ROUTE_MAP,          // 3/4 #3 Edzésútvonal (Margitsziget)
    COACH_DETAIL,           // 3/4 #6 Coach + main TrainPilot Coach
    SETTINGS_MAIN,          // 4/4 #1 Beállítások + main Beállítások
    WATCH_AND_SYNC,         // 4/4 #2 Óra és szinkron (Galaxy Watch7)
    BACKUP_RESTORE,         // 4/4 #3 Mentés és visszaállítás
    PERMISSIONS_SCREEN,     // 4/4 #4 Engedélyek
    PRIVATE_PHOTOS          // 4/4 #5 Privát edzésfotók
}

enum class HomeDemoMode {
    PAUSED_WORKOUT,         // Mockup 1/4 #1: "Folytasd a megkezdett edzést! • Szüneteltetve • Teljes test A"
    PLANNED_NEXT,           // Main app Home: "Aktív program • Következő: Otthoni A/B - Alap A"
    COACH_LIGHT_DAY         // Main app Coach warning: "Könnyített nap • Mai készenlét 54/100"
}

enum class ExerciseCategory(val labelHu: String, val labelEn: String, val labelDe: String, val labelRo: String) {
    ALL("Összes", "All", "Alle", "Toate"),
    LEGS("Láb", "Legs", "Beine", "Picioare"),
    CHEST("Mell", "Chest", "Brust", "Piept"),
    BACK("Hát", "Back", "Rücken", "Spate"),
    SHOULDERS("Váll", "Shoulders", "Schultern", "Umeri"),
    ARMS("Kar", "Arms", "Arme", "Brațe"),
    CORE("Core", "Core", "Core", "Core");

    fun localized(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> labelHu
        AppLanguage.EN -> labelEn
        AppLanguage.DE -> labelDe
        AppLanguage.RO -> labelRo
    }
}

data class ExerciseSetItem(
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val rir: Int,
    val completed: Boolean
)

data class ExerciseItem(
    val id: String,
    val nameHu: String,
    val nameEn: String,
    val nameDe: String,
    val nameRo: String,
    val category: ExerciseCategory,
    val equipmentHu: String,
    val primaryMusclesHu: String,
    val secondaryMusclesHu: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Float,
    val defaultRestSec: Int,
    val isTimed: Boolean = false,
    val isBilateral: Boolean = false,
    val targetSeconds: Int = 45,
    val stepsHu: List<String> = emptyList(),
    val prWeightKg: Float = 0f,
    val prReps: Int = 0,
    val coachStatusHu: String = "Normál terhelés",
    val coachHintHu: String = "Stabil technika, 1–2 RIR tartalékkal."
) {
    fun localizedName(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> nameHu
        AppLanguage.EN -> nameEn
        AppLanguage.DE -> nameDe
        AppLanguage.RO -> nameRo
    }
}

data class ProgramDayModel(
    val id: String,
    val dayCode: String, // "A nap", "B nap"
    val titleHu: String,
    val titleEn: String,
    val durationMinutes: Int,
    val exercises: List<ExerciseItem>
) {
    fun localizedTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.HU) titleHu else titleEn
}

data class TrainingProgramModel(
    val id: String,
    val nameHu: String,
    val nameEn: String,
    val subtitleHu: String,
    val subtitleEn: String,
    val tagsHu: List<String>,
    val isCustom: Boolean,
    val isActive: Boolean,
    val days: List<ProgramDayModel>
) {
    fun localizedName(lang: AppLanguage): String =
        if (lang == AppLanguage.HU) nameHu else nameEn
    fun localizedSubtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.HU) subtitleHu else subtitleEn
}

enum class CalendarEventStatus {
    PLANNED,
    COMPLETED,
    MISSED
}

data class CalendarDayEvent(
    val id: String,
    val dayOfMonth: Int,
    val timeLabel: String,
    val titleHu: String,
    val exerciseCount: Int,
    val durationMinutes: Int,
    val status: CalendarEventStatus,
    val splitBadge: String = "A"
)

data class LoggedWorkoutEntry(
    val id: String,
    val dateLabelHu: String,
    val titleHu: String,
    val durationMinutes: Int,
    val workTimeLabel: String,
    val restTimeLabel: String,
    val totalSets: Int,
    val totalVolumeKg: Int,
    val summaryExercisesHu: String,
    val hasPr: Boolean = false,
    val isOutdoorRun: Boolean = false,
    val distanceKm: Float = 0f,
    val paceLabel: String = "",
    val photoCount: Int = 0,
    val sourceWatch: Boolean = true,
    val sourceHealthConnect: Boolean = true,
    val exerciseDetails: List<LoggedExerciseDetail> = emptyList()
)

data class LoggedExerciseDetail(
    val nameHu: String,
    val summaryPrescription: String,
    val sets: List<ExerciseSetItem>
)

data class PrRecordEntry(
    val id: String,
    val exerciseNameHu: String,
    val typeLabelHu: String,
    val dateLabel: String,
    val previousLabel: String,
    val newLabel: String
)
