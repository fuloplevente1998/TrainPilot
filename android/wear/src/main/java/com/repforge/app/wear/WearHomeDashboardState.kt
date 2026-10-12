package com.repforge.app.wear

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

internal enum class HomeAction {
    START,
    RESUME,
    DAYS,
    RESULT,
    PHONE,
}

internal data class WearHomeDashboardState(
    val phase: String,
    val clock: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val action: HomeAction,
    val actionLabel: String,
    val measurement: String,
    val metrics: List<String>,
    val completed: Int,
    val total: Int,
    val source: String,
    val active: Boolean,
)

internal fun wearHomeDashboardState(
    home: WatchHomeSnapshot?,
    workout: WearWorkout?,
    closure: WearClosure?,
    health: JSONObject?,
    enabled: Boolean,
    now: Long,
): WearHomeDashboardState {
    val clock = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val homeClock = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    val source =
        if (home?.publishedAt?.let { it > 0L && it <= now + 5000 } == true)
            "Adat ${clock.format(Instant.ofEpochMilli(home.publishedAt))}"
        else "Helyi adat"
    val measurementId =
        workout?.workoutId ?: closure?.takeIf { it.status in listOf("pending", "error") }?.workoutId
    val currentHealth =
        health?.takeIf {
            measurementId != null &&
                measurementId.isNotBlank() &&
                it.optString("workoutId") == measurementId &&
                it.optString("source") == "wear_health_services"
        }
    val measurement =
        when (currentHealth?.optString("state")) {
            "active" ->
                if (now - currentHealth.optLong("updatedAt") > 20000L) "Legutóbbi mérés"
                else "Mérés folyamatban"
            "paused" -> "Mérés szünetel"
            "starting" -> "Mérés indul"
            "ending" -> "Mérés lezárása"
            "error" -> "Mérési hiba"
            "ended" -> "Mérés lezárva"
            else ->
                if (!enabled) "Mérés kikapcsolva"
                else if (workout != null) "Mérésre vár" else "Mérés bekapcsolva"
        }
    fun value(
        phase: String,
        title: String,
        subtitle: String,
        detail: String,
        action: HomeAction,
        label: String,
        total: Int = 0,
    ) =
        WearHomeDashboardState(
            phase,
            homeClock.format(Instant.ofEpochMilli(now)),
            title,
            subtitle,
            detail,
            action,
            label,
            measurement,
            emptyList(),
            0,
            total,
            source,
            false,
        )
    if (workout != null) {
        val rest = ((workout.restEndAt - now + 999L) / 1000L).coerceAtLeast(0L).toInt()
        val sets = workout.exercise?.sets.orEmpty()
        val completed = sets.count { it.done }
        val metrics =
            wearLiveWorkoutRows(currentHealth, workout.workoutId, now)
                .filter { it.contains("bpm") || it.contains("kcal") || it.startsWith("Edzés: ") }
                .map { it.removePrefix("Edzés: ") }
        return WearHomeDashboardState(
            if (rest > 0) "PIHENŐ" else "EDZÉS",
            formatSeconds(if (rest > 0) rest else workout.elapsedSeconds(now)).padStart(5, '0'),
            workout.exercise?.name ?: workout.programName,
            workout.dayName,
            if (completed == sets.size && sets.isNotEmpty()) "$completed/${sets.size} sorozat kész"
            else "Sorozat ${workout.currentSetIndex + 1}/${sets.size}",
            HomeAction.RESUME,
            "Folytatás",
            measurement,
            metrics,
            completed,
            sets.size,
            source,
            true,
        )
    }
    if (closure?.status in listOf("pending", "error"))
        return value(
            if (closure?.status == "error") "MENTÉSI HIBA" else "MENTÉS VÁR",
            "Órán megőrizve",
            "Az edzés a telefonra vár",
            source,
            HomeAction.RESULT,
            "Állapot",
        )
    if (home == null)
        return value(
            "SZINKRONRA VÁR",
            "TrainPilot",
            "Nyisd meg a telefonon",
            "A programadat még nem érkezett meg",
            HomeAction.PHONE,
            "Telefon",
        )
    if (home.hasDraft)
        return value(
            "FÉLBEHAGYOTT",
            "Edzés a telefonon",
            "Nyisd meg a folytatáshoz",
            source,
            HomeAction.PHONE,
            "Telefon",
        )
    home.recommended
        ?.takeIf { it.day.exercises.isNotEmpty() }
        ?.let { rec ->
            val time =
                try {
                    if (rec.plannedStart.isBlank()) ""
                    else clock.format(Instant.parse(rec.plannedStart))
                } catch (_: Exception) {
                    ""
                }
            return value(
                "KÖVETKEZŐ EDZÉS",
                rec.day.name,
                rec.day.programName,
                "${rec.day.exercises.size} gyakorlat${if (time.isBlank()) "" else " · $time"}",
                HomeAction.START,
                "Indítás",
                rec.day.exercises.size,
            )
        }
    return if (home.days.any { it.exercises.isNotEmpty() })
        value(
            "EDZÉSRE KÉSZ",
            home.activeProgramName.ifBlank { "Programnapok" },
            "Válassz edzésnapot",
            source,
            HomeAction.DAYS,
            "Napok",
        )
    else
        value(
            "NINCS PROGRAM",
            "Válassz programot",
            "A telefonos alkalmazásban",
            source,
            HomeAction.PHONE,
            "Telefon",
        )
}
