package com.repforge.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic sizing inputs only; real Home data/provider flows remain in the host suite. */
@RunWith(AndroidJUnit4::class)
class NativeHomeMetricLayoutUiTest {
    @get:Rule val compose = createComposeRule()
    @After fun settleComposition() = compose.waitForIdle()

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

    @Test fun acceptedStudioBentoHasFourRealMetricCardsNotSixDemoTiles() {
        compose.setContent {
            TrainPilotTheme(TrainPilotUserSettings()) {
                Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    com.repforge.app.ui.home.StudioHomeVitals(
                        TodayMetricsUiModel(sleepText = "7 h", pulseText = "—",
                            stepsText = "412", recoveryText = "—"),
                        "hu", onHealth = {}, onCoach = {}, enabled = true
                    )
                }
            }
        }
        for (key in listOf("sleep", "pulse", "steps", "recovery")) {
            compose.onNodeWithTag("home_metric_$key").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("home_metric_pulse_value").assertTextEquals("—")
        compose.onNodeWithTag("home_metric_steps_value").assertTextEquals("412")
        compose.onNodeWithTag("home_metric_recovery_source").performScrollTo()
            .assertIsDisplayed()
        // Secondary calorie/workout counts belong in the lower Studio goals row,
        // not in the accepted 2×2 bento layout.
        compose.onAllNodesWithTag("home_metric_calories").assertCountEquals(0)
        compose.onAllNodesWithTag("home_metric_workouts").assertCountEquals(0)
    }

    @Test fun acceptedStudioGreetingKeepsCompactTwoDpGapAtOwnerWidth() {
        compose.setContent {
            TrainPilotTheme(TrainPilotUserSettings()) {
                Column(Modifier.width(384.dp)) {
                    com.repforge.app.ui.home.StudioHomeGreeting(
                        title = "Üdv újra!", subtitle = "Mai edzésed és állapotod áttekintése."
                    )
                }
            }
        }
        val greeting = compose.onNodeWithTag("home_studio_greeting").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val intro = compose.onNodeWithTag("home_studio_intro").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        val gapDp = (intro.top - greeting.bottom) / density
        assertTrue("Greeting and supporting text must not overlap", gapDp >= -0.01f)
        assertTrue("Source 18:49 Studio greeting should use a 2dp gap: $gapDp dp",
            gapDp in 1.0f..3.5f)
        compose.onNodeWithTag("home_studio_greeting_group").assertIsDisplayed()
    }

    @Test fun narrowPhoneUsesTwoReadableColumnsWithAllSixMetricsReachable() {
        show(1f)
        val first = compose.onNodeWithTag("home_metric_sleep").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("home_metric_calories").fetchSemanticsNode().boundsInRoot
        assertTrue("Adjacent metric cards must not overlap", first.right < second.left)
        assertTrue("Intrinsic metric row must align card heights", kotlin.math.abs(first.height - second.height) <= 1f)
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
    @Test fun enlargedGermanHealthTileKeepsItsValueUnitAndFullProviderText() {
        val source = "com.example.provider.with.long.provenance.and.measurement.time"
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        com.repforge.app.ui.health.HealthMetricTile(
                            com.repforge.app.ui.health.HealthMetricItemUiModel("distanceMeters", "Gemessene Entfernung",
                                "12345", "m", source, true), Modifier.fillMaxWidth())
                    }
                }
            }
        }
        compose.onNodeWithText("12345").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("m").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(source).performScrollTo().assertIsDisplayed().assertTextEquals(source)
    }

    @Test fun studioTabsWithLargeGermanTextKeepEveryLabelAndSelectionAccessible() = largeGermanTabs(false)

    @Test fun actualStudioShellKeepsLargeGermanTabsAndSelectionAccessible() = largeGermanTabs(true)

    private fun largeGermanTabs(studioShell: Boolean) {
        compose.setContent {
            val density = LocalDensity.current.density
            var route by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("home") }
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        if (studioShell) com.repforge.app.ui.navigation.NativeStudioShellHeader(
                            route, "home", "de", "graphite_gold", route != "health",
                            onHome = { route = "home" }, onSelect = { route = it },
                            onSelectLanguage = {}, onTheme = {}, onSettings = {})
                        else com.repforge.app.ui.navigation.NativeTopNavigation(route, "home", "de",
                            enabled = route != "health", onSelect = { route = it })
                    }
                }
            }
        }
        if (studioShell) {
            val brandLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("header_brand_text", useUnmergedTree = true).assertIsDisplayed()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(brandLayouts) }
            assertTrue("Brand must remain complete at 320dp/font2: " + brandLayouts.joinToString { "size=${it.size}, paragraph=${it.multiParagraph.width}, constraints=${it.layoutInput.constraints}" }, brandLayouts.isNotEmpty() &&
                brandLayouts.none { it.hasVisualOverflow })
        }
        for ((route, label) in listOf("home" to "Startseite", "training" to "Training",
            "journal" to "Protokoll", "progress" to "Fortschritt", "health" to "Gesundheit")) {
            compose.onNodeWithTag("navigation_$route").performScrollTo().assertIsDisplayed().performClick()
                .assertIsSelected()
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("A tab must expose its actual text layout", layouts.isNotEmpty())
            assertTrue("Full German tab label '$label' must remain visible at font2.0: " +
                layouts.joinToString { "size=${it.size},widthOverflow=${it.didOverflowWidth},heightOverflow=${it.didOverflowHeight},paragraphWidth=${it.multiParagraph.width},constraints=${it.layoutInput.constraints}" },
                layouts.none { it.hasVisualOverflow })
        }
        for (route in listOf("home", "training", "journal", "progress", "health")) {
            compose.onNodeWithTag("navigation_$route").assertIsNotEnabled()
        }
    }

}
