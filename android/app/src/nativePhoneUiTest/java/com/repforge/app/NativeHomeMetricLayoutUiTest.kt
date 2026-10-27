package com.repforge.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.home.TodayMetricsGrid
import com.repforge.app.ui.home.TodayMetricsUiModel
import com.repforge.app.ui.theme.TrainPilotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic sizing inputs only; real Home data/provider flows remain in the host suite. */
@RunWith(AndroidJUnit4::class)
class NativeHomeMetricLayoutUiTest {
    @get:Rule val compose = createComposeRule()

    private fun show(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        TodayMetricsGrid(TodayMetricsUiModel(sleepText = "7 h 24 min", caloriesText = "12345"), "de")
                    }
                }
            }
        }
    }

    @Test fun narrowPhoneUsesTwoReadableColumnsWithAllSixMetricsReachable() {
        show(1f)
        val first = compose.onNodeWithTag("home_metric_sleep").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("home_metric_calories").fetchSemanticsNode().boundsInRoot
        assertTrue("Adjacent metric cards must not overlap", first.right < second.left)
        for (key in listOf("sleep", "calories", "pulse", "steps", "workouts", "recovery")) {
            compose.onNodeWithTag("home_metric_${key}_value").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun enlargedGermanTextStacksCardsAndPreservesFullMeasurementText() {
        show(2f)
        val first = compose.onNodeWithTag("home_metric_sleep").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("home_metric_calories").fetchSemanticsNode().boundsInRoot
        assertTrue("Large text must stack cards vertically", second.top >= first.bottom)
        compose.onNodeWithTag("home_metric_sleep_value").performScrollTo()
            .assertIsDisplayed().assertTextEquals("7 h 24 min")
        compose.onNodeWithTag("home_metric_calories_value").performScrollTo()
            .assertIsDisplayed().assertTextEquals("12345")
        compose.onNodeWithTag("home_metric_recovery_source").performScrollTo()
            .assertIsDisplayed().assertTextEquals("Keine Daten")
    }
}
