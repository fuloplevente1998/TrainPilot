package com.repforge.app.ui.navigation

/** Main's accepted primary order. Native secondary screens stay behind their original entry. */
internal object NativeMainMenu {
    val routes = listOf("home", "training", "health", "programs", "journal", "calendar", "coach", "settings")
    fun selected(route: String): String = when(route) {
        "active_workout", "quick_workout", "quick_outdoor" -> "training"
        "progress" -> "coach"
        else -> route
    }
    fun destination(route: String, hasActiveWorkout: Boolean): String {
        require(route in routes)
        return if (route == "training" && hasActiveWorkout) "active_workout" else route
    }
    fun labels(language: String) = when(language) {
        "en" -> listOf("Home", "Training", "Health", "Programs", "Journal", "Calendar", "Coach", "Settings")
        "de" -> listOf("Startseite", "Training", "Gesundheit", "Programme", "Protokoll", "Kalender", "Coach", "Einstellungen")
        "ro" -> listOf("Acasă", "Antrenament", "Sănătate", "Programe", "Jurnal", "Calendar", "Coach", "Setări")
        else -> listOf("Kezdőlap", "Edzés", "Egészség", "Programok", "Napló", "Naptár", "Coach", "Beállítások")
    }
}
