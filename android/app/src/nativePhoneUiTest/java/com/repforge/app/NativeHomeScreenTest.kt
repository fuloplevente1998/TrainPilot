package com.repforge.app

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.home.HomeWorkoutRecommendation
import com.repforge.app.ui.home.HomeScreenContent
import com.repforge.app.ui.home.HomeUiState
import com.repforge.app.ui.theme.TrainPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeHomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickWorkoutAndRealHealthTilesAreShownWithoutInventedPlanData() {
        var openedTraining = false
        compose.setContent {
            TrainPilotTheme(TrainPilotUserSettings()) {
                HomeScreenContent(
                    state = HomeUiState(isLoading = false),
                    hasActiveWorkout = false,
                    activeWorkoutTitle = "",
                    isWorkoutPaused = false,
                    activeWorkoutOwner = "phone",
                    onResumeActiveWorkout = {},
                    onStartRecommendedWorkout = { _, _, _ -> },
                    onOpenTraining = { openedTraining = true },
                    onPrograms = {},
                    onCalendar = {},
                    onCoach = {},
                    onResumeDraft = {},
                    onQuickWorkout = {},
                    onRetryMetrics = {},
                    language = "hu",
                    busy = false
                )
            }
        }

        compose.onNodeWithTag("home_quick_workout").assertIsDisplayed()
        compose.onNodeWithTag("home_watch_status").assertIsDisplayed()
        compose.onNodeWithTag("home_metric_sleep").assertIsDisplayed()
        compose.onNodeWithTag("home_metric_pulse").assertIsDisplayed()
        compose.onNodeWithTag("home_metric_steps").assertIsDisplayed()
        compose.onNodeWithTag("home_metric_energy").assertIsDisplayed()
        compose.onNodeWithTag("home_active_workout").assertDoesNotExist()
        compose.onNodeWithTag("home_imported_draft").assertDoesNotExist()
        compose.onNodeWithTag("home_open_training").performClick()
        assertTrue(openedTraining)
    }

    @Test fun activeWorkoutWinsOverImportedDraftAndCannotStartAnotherWorkout() {
        var resumed = false
        var quickStart = false
        compose.setContent {
            TrainPilotTheme(TrainPilotUserSettings()) {
                HomeScreenContent(
                    state = HomeUiState(isLoading = false, hasImportedDraft = true),
                    hasActiveWorkout = true,
                    activeWorkoutTitle = "Teljes test A",
                    isWorkoutPaused = true,
                    activeWorkoutOwner = "watch",
                    onResumeActiveWorkout = { resumed = true },
                    onStartRecommendedWorkout = { _, _, _ -> },
                    onOpenTraining = {},
                    onPrograms = {},
                    onCalendar = {},
                    onCoach = {},
                    onResumeDraft = {},
                    onQuickWorkout = { quickStart = true },
                    onRetryMetrics = {},
                    language = "hu",
                    busy = false
                )
            }
        }

        compose.onNodeWithTag("home_active_workout").assertIsDisplayed()
        compose.onNodeWithTag("home_active_workout_action").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home_imported_draft").assertDoesNotExist()
        compose.onNodeWithTag("home_resume_draft").assertDoesNotExist()
        compose.onNodeWithTag("home_quick_workout").assertIsDisplayed().assertIsNotEnabled()
        assertTrue(resumed)
        assertFalse(quickStart)
    }

    @Test fun recommendedWorkoutStartsTheSelectedProgramDayAndSchedule() {
        var started: Triple<String, String, String?>? = null
        compose.setContent {
            TrainPilotTheme(TrainPilotUserSettings()) {
                HomeScreenContent(
                    state = HomeUiState(
                        isLoading = false,
                        recommendedWorkout = HomeWorkoutRecommendation(
                            programId = "program-a",
                            dayId = "day-b",
                            scheduleId = "schedule-1",
                            programTitle = "Alap program",
                            dayTitle = "B nap"
                        )
                    ),
                    hasActiveWorkout = false,
                    activeWorkoutTitle = "",
                    isWorkoutPaused = false,
                    activeWorkoutOwner = "phone",
                    onResumeActiveWorkout = {},
                    onStartRecommendedWorkout = { programId, dayId, scheduleId ->
                        started = Triple(programId, dayId, scheduleId)
                    },
                    onOpenTraining = {},
                    onPrograms = {},
                    onCalendar = {},
                    onCoach = {},
                    onResumeDraft = {},
                    onQuickWorkout = {},
                    onRetryMetrics = {},
                    language = "hu",
                    busy = false
                )
            }
        }

        compose.onNodeWithTag("home_recommended_workout").assertIsDisplayed()
        compose.onNodeWithTag("home_recommended_action").assertIsDisplayed().performClick()
        assertEquals(Triple("program-a", "day-b", "schedule-1"), started)
    }
}
