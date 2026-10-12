package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearHomeDashboardStateTest {
    private val now = 1791805500000L
    private val exercise =
        WatchHomeExercise(
            "squat",
            "Goblet guggolás",
            "weight",
            "ism.",
            "strength",
            "8",
            listOf(WatchHomeSet(1, "5.5")),
        )
    private val day = WatchHomeDay("a", "Alap A", "p", "Otthoni A/B", listOf(exercise))
    private val home =
        WatchHomeSnapshot(
            "p",
            "Otthoni A/B",
            60,
            false,
            WatchHomeRecommendation("s", "", day),
            listOf(day),
        )
    private val workout =
        home.createWorkout(day, "s", java.time.Instant.ofEpochMilli(now - 65000L).toString())

    private fun state(
        work: WearWorkout? = null,
        source: WatchHomeSnapshot? = home,
        result: WearClosure? = null,
        health: JSONObject? = null,
    ) = wearHomeDashboardState(source, work, result, health, true, now)

    private fun metrics(id: String) =
        JSONObject()
            .put("workoutId", id)
            .put("source", "wear_health_services")
            .put("state", "active")
            .put("updatedAt", now)
            .put("heartRate", 97)
            .put("totalCalories", 7)
            .put("steps", 65)

    @Test
    fun idleNeverBorrowsPreviousWorkoutsAndOnlyOffersRealPreparedDays() {
        val value = state(health = metrics(workout.workoutId))
        assertTrue(value.metrics.isEmpty())
        assertEquals(HomeAction.START, value.action)
        assertEquals("Alap A", value.title)
        assertEquals(1, value.total)
        assertEquals(HomeAction.PHONE, state(source = null).action)
        assertEquals(
            HomeAction.PHONE,
            state(source = home.copy(recommended = null, days = emptyList())).action,
        )
    }

    @Test
    fun idleClockShowsSecondsAndChangesEverySecondWithoutChangingSnapshotTime() {
        val first = wearHomeDashboardState(home, null, null, null, true, now)
        val next = wearHomeDashboardState(home, null, null, null, true, now + 1000L)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault())
        assertEquals(formatter.format(java.time.Instant.ofEpochMilli(now)), first.clock)
        assertEquals(formatter.format(java.time.Instant.ofEpochMilli(now + 1000L)), next.clock)
        assertNotEquals(first.clock, next.clock)
        assertEquals(first.source, next.source)
    }

    @Test
    fun currentMetricsAreSessionMatchedAndStaleDataIsExplicit() {
        assertTrue(state(workout, health = metrics("previous")).metrics.isEmpty())
        val data = metrics(workout.workoutId)
        val value = state(workout, health = data)
        assertEquals(listOf("97 bpm · 7 kcal", "65 lépés"), value.metrics)
        assertEquals("01:05", value.clock)
        assertEquals(HomeAction.RESUME, value.action)
        data.put("updatedAt", now - 30000L)
        assertEquals("Legutóbbi mérés", state(workout, health = data).measurement)
    }

    @Test
    fun pendingSaveAndPhoneDraftCannotStartAnotherWorkout() {
        val pending = WearClosure("c", workout.workoutId, "finishWorkout", "pending", "", 65, 1, 1)
        assertEquals(HomeAction.RESULT, state(result = pending).action)
        assertEquals(HomeAction.RESULT, state(result = pending.copy(status = "error")).action)
        assertEquals(HomeAction.PHONE, state(source = home.copy(hasDraft = true)).action)
        assertEquals(HomeAction.RESUME, state(workout, result = pending).action)
    }

    @Test
    fun restAndProgressUseActualWorkoutFields() {
        val active =
            workout.copy(
                restEndAt = now + 20000L,
                exercises =
                    listOf(
                        workout.exercises
                            .first()
                            .copy(
                                sets =
                                    listOf(
                                        WearSet(1, "8", "5.5", true, 0, 0, 0.0),
                                        WearSet(2, "", "5.5", false, 0, 0, 0.0),
                                    )
                            )
                    ),
            )
        val value = state(active)
        assertEquals("PIHENŐ", value.phase)
        assertEquals("00:20", value.clock)
        assertEquals(1, value.completed)
        assertEquals(2, value.total)
        assertEquals("Sorozat 2/2", value.detail)
    }

    @Test
    fun optionalPhonePaletteIsBoundedAndOldSnapshotsUseFallback() {
        assertEquals(WearHomeTheme.DEFAULT, WearHomeTheme.parse(null))
        val theme =
            JSONObject()
                .put("key", "green")
                .put("accent", "#62d08a")
                .put("accent2", "#9cf5ba")
                .put("family", "vivid")
        assertEquals("green", WearHomeTheme.parse(theme).key)
        assertEquals("vivid", WearHomeTheme.parse(theme).family)
        theme.put("accent", "transparent")
        assertEquals(WearHomeTheme.DEFAULT, WearHomeTheme.parse(theme))
    }

    @Test
    fun framesAndButtonSurfacesMatchThePhoneSrgbColorMix() {
        val theme = WearHomeTheme.DEFAULT
        val frame = theme.frame
        assertEquals(66f / 255f, frame.red, 1f / 255f)
        assertEquals(112f / 255f, frame.green, 1f / 255f)
        assertEquals(147f / 255f, frame.blue, 1f / 255f)
        val button = theme.button
        assertEquals(42f / 255f, button.red, 1f / 255f)
        assertEquals(57f / 255f, button.green, 1f / 255f)
        assertEquals(73f / 255f, button.blue, 1f / 255f)
    }
}
