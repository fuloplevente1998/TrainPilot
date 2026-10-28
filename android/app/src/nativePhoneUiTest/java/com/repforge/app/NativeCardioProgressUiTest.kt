package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.WorkoutHistorySetEntity
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production MainActivity + Room + DataStore on an isolated debuggable emulator. */
@RunWith(AndroidJUnit4::class)
class NativeCardioProgressUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val fixture = NativeLocalizationEmulatorFixture()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun reset() = fixture.reset()
    @After fun close() = fixture.close(scenario, compose)

    private fun exists(tag: String) =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun hasText(text: String) =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun openCardioSection() {
        compose.onNodeWithTag("progress_list")
            .performScrollToNode(hasTestTag("progress_section_tab_1"))
        compose.onNodeWithTag("progress_section_tab_1").performClick()
        compose.onNodeWithTag("progress_list")
            .performScrollToNode(hasTestTag("progress_stats_subtab_1"))
        compose.onNodeWithTag("progress_stats_subtab_1").performClick()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_cardio"))
        compose.waitUntil(30_000) { exists("progress_cardio_all") }
    }

    private fun openStrengthSection() {
        compose.onNodeWithTag("progress_list")
            .performScrollToNode(hasTestTag("progress_section_tab_2"))
        compose.onNodeWithTag("progress_section_tab_2").performClick()
    }

    private fun enterProgress() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) { exists("navigation_progress") }
        compose.onNodeWithTag("navigation_progress").performClick()
        compose.waitUntil(30_000) { exists("progress_list") }
        openCardioSection()
    }

    private suspend fun insert(id: Int, activity: String, meters: Double?, reps: String) {
        val dao = fixture.database.workoutHistoryDao()
        val epoch = System.currentTimeMillis() - id * 60_000L
        val start = Instant.ofEpochMilli(epoch - 300_000L).toString()
        val end = Instant.ofEpochMilli(epoch).toString()
        val workoutId = "cardio-$id"
        dao.insertWorkout(WorkoutHistoryEntity(
            id = workoutId, workoutKey = "key-cardio-$id", syncId = "sync-cardio-$id",
            started = start, finished = end, startedEpochMs = epoch - 300_000L,
            finishedEpochMs = epoch, durationSeconds = 300,
            programId = "test", programName = "Test", dayId = "A", dayName = "A",
            scheduleId = "", quickWorkout = false, originDevice = "phone",
            ownershipProtocol193 = 0, ownerEpoch193 = 0L,
            effortFeedback = null, feedbackSkipped = false, feedbackUpdatedAt = 0L,
            totalVolumeKg = 0.0, completedSetsCount = 1, totalSetsCount = 1,
            totalRepsCount = 0, hasPr226 = false, prRecordsJson226 = "[]",
            wearMetricsJson = null, health240Json = null, timeLogs162Json = null,
            activeIntervalsJson = null, editedAt = null,
            rawJson = """{"exercises":[{"id":"$activity","sets":[{"set":1,"setId":"cardio-set-$id","distanceSource":"manual"}]}]}"""
        ))
        val rowId = dao.insertExercise(WorkoutHistoryExerciseEntity(
            workoutHistoryId = workoutId, exerciseId = activity,
            nameHu = if (activity == "running") "Futás" else "Kerékpár",
            nameEn = if (activity == "running") "Running" else "Cycling",
            loadType = "bodyweight", repUnit = "mp", measurementType = "distance",
            orderIndex = 0, actualRir = null, setRir229 = false
        ))
        dao.insertSets(listOf(WorkoutHistorySetEntity(
            setId = "cardio-set-$id", workoutHistoryId = workoutId,
            historyExerciseRowId = rowId, exerciseId = activity, setNumber = 1,
            weight = null, reps = reps, leftSeconds = null, rightSeconds = null,
            distanceMeters = meters, done = true, actualRir = null,
            gpsRouteKey163 = null, gpsRouteKey193 = null
        )))
    }

    @Test fun realProgressShowsEightWeeksAndSwitchesActivitiesWithoutLosingStrength() {
        runBlocking(Dispatchers.IO) {
            insert(1, "running", 3000.0, "900")
            insert(2, "cycling", 2000.0, "600")
        }
        enterProgress()
        compose.waitUntil(30_000) { exists("progress_cardio_total_distance") }
        compose.onNodeWithTag("progress_cardio_chart").assertExists()
        compose.onNodeWithTag("progress_cardio_week").assertExists()
        compose.onNodeWithTag("progress_cardio_previous_week").assertExists()
        compose.onNodeWithTag("progress_cardio_choices")
            .performScrollToNode(hasTestTag("progress_cardio_activity_running"))
        compose.onNodeWithTag("progress_cardio_activity_running").performClick()
        compose.waitUntil(30_000) {
            exists("progress_cardio_total_distance") && hasText("3,00")
        }
        compose.onNodeWithTag("progress_cardio_record_running_3000").assertExists()
        compose.onNodeWithTag("progress_cardio_all").performClick()
        compose.waitUntil(30_000) { hasText("5,00") }
        // Strength/1RM remains reachable through the Studio section navigation.
        openStrengthSection()
        compose.onNodeWithTag("progress_list")
            .performScrollToNode(hasTestTag("progress_exercise_choices"))
        compose.onNodeWithTag("progress_exercise_choices").assertExists()
    }

    @Test fun sameCountSetEditTriggersRealRoomInvalidationAndNewTotal() {
        runBlocking(Dispatchers.IO) { insert(1, "running", 3000.0, "900") }
        enterProgress()
        compose.waitUntil(30_000) { hasText("3,00") }
        runBlocking(Dispatchers.IO) {
            val dao = fixture.database.workoutHistoryDao()
            val row = dao.pageCardioSets(null, "", Long.MAX_VALUE, 5).single()
            val newSet = WorkoutHistorySetEntity(
                rowId = row.setRowId, setId = row.setId, workoutHistoryId = row.workoutId,
                historyExerciseRowId = fixture.database
                    .workoutHistoryDao().getWorkoutWithDetailsById(row.workoutId)!!
                    .exercises.single().exercise.rowId,
                exerciseId = row.exerciseId, setNumber = row.setNumber,
                weight = null, reps = "900", leftSeconds = null, rightSeconds = null,
                distanceMeters = 8000.0, done = true, actualRir = null,
                gpsRouteKey163 = null, gpsRouteKey193 = null
            )
            dao.insertSets(listOf(newSet))
        }
        compose.waitUntil(30_000) { hasText("8,00") }
        compose.onNodeWithTag("progress_cardio_record_running_8000").assertExists()
    }

    @Test fun emptyHistoryAndHuEnDeRoThemesRenderRealProgressPanel() {
        enterProgress()
        compose.waitUntil(30_000) { exists("progress_cardio_empty") }
        val locales = listOf(
            Triple("hu", "classicBlue", "Kardió"),
            Triple("en", "yellow", "Cardio"),
            Triple("de", "classicRose", "Kardio"),
            Triple("ro", "cyan", "Cardio")
        )
        for ((language, theme, heading) in locales) {
            compose.onNodeWithTag("navigation_settings").performClick()
            fixture.selectLanguageThroughUi(compose, language)
            compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("theme_$theme"))
            compose.onNodeWithTag("theme_$theme").performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(15_000) {
                fixture.selectedLanguage() == language &&
                    runBlocking { fixture.settings.current().themeKey == theme }
            }
            compose.onNodeWithTag("navigation_back").performClick()
            compose.waitUntil(30_000) { exists("progress_list") }
            openCardioSection()
            compose.waitUntil(30_000) { hasText(heading) && exists("progress_cardio_empty") }
            compose.onNodeWithTag("progress_cardio_all").assertExists()
        }
    }
}
