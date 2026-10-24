package com.repforge.app.ui.navigation

/** Accepted five-tab hierarchy; secondary destinations keep a primary parent. */
internal object NativeMainMenu {
    val routes = listOf("home", "training", "journal", "progress", "health")
    val secondaryRoutes = listOf("programs", "calendar", "coach", "quick_workout", "quick_outdoor", "active_workout", "settings")

    fun selected(route: String, settingsOrigin: String = "home"): String = when (route) {
        "active_workout", "quick_workout", "quick_outdoor", "programs" -> "training"
        "calendar", "coach" -> "home"
        "settings" -> if (settingsOrigin == "settings") "home" else selected(settingsOrigin)
        else -> route.takeIf { it in routes } ?: "home"
    }

    fun destination(route: String, hasActiveWorkout: Boolean): String {
        require(route in routes || route in secondaryRoutes)
        return if (route == "training" && hasActiveWorkout) "active_workout" else route
    }

    /** Settings is a temporary visit, while switching away from the editor is explicit leave. */
    fun pausesWorkout(current: String, target: String, settingsOrigin: String = "home"): Boolean =
        target != "settings" && target != "active_workout" &&
            (current == "active_workout" || (current == "settings" && settingsOrigin == "active_workout"))

    /** A failed finalization may need an editor retry; returning there never starts a session. */
    fun navigationAllowed(target: String, starting: Boolean, finishing: Boolean, finalizing: Boolean,
        restoreBlocked: Boolean, commandsSuspended: Boolean): Boolean =
        !starting && !finishing && !restoreBlocked &&
            (target == "active_workout" || (!finalizing && !commandsSuspended))

    fun labels(language: String) = when (language) {
        "en" -> listOf("Home", "Training", "Journal", "Progress", "Health")
        "de" -> listOf("Startseite", "Training", "Protokoll", "Fortschritt", "Gesundheit")
        "ro" -> listOf("Acasă", "Antrenament", "Jurnal", "Progres", "Sănătate")
        else -> listOf("Kezdőlap", "Edzés", "Napló", "Fejlődés", "Egészség")
    }

    fun title(route: String, language: String): String {
        if (route in routes) return labels(language)[routes.indexOf(route)]
        val names = when (language) {
            "en" -> listOf("Programs", "Calendar", "Coach", "Quick workout", "Outdoor workout", "Training", "Settings", "Back")
            "de" -> listOf("Programme", "Kalender", "Coach", "Schnelltraining", "Training im Freien", "Training", "Einstellungen", "Zurück")
            "ro" -> listOf("Programe", "Calendar", "Coach", "Antrenament rapid", "Antrenament în aer liber", "Antrenament", "Setări", "Înapoi")
            else -> listOf("Programok", "Naptár", "Coach", "Gyors edzés", "Szabadtéri edzés", "Edzés", "Beállítások", "Vissza")
        }
        return names[secondaryRoutes.indexOf(route).takeIf { it >= 0 } ?: names.lastIndex]
    }
    fun backLabel(language: String) = title("back", language)
}
