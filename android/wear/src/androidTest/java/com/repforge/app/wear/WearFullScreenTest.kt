package com.repforge.app.wear

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Actual Compose layout, touch scrolling and screenshots on a round Wear OS emulator. */
@RunWith(Parameterized::class)
class WearFullScreenTest(private val diameter: Float, private val fontScale: Float) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}dp-font{1}")
        fun sizes() =
            listOf(
                arrayOf(160f, 1f),
                arrayOf(192f, 1f),
                arrayOf(225f, 1f),
                arrayOf(160f, 1.5f),
                arrayOf(225f, 2f),
            )
    }

    @get:Rule val compose = createComposeRule()

    @Composable
    private fun Watch(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val pixels = with(LocalDensity.current) { maxWidth.toPx() }
            CompositionLocalProvider(LocalDensity provides Density(pixels / diameter, fontScale)) {
                Box(Modifier.fillMaxSize().clip(CircleShape)) { content() }
            }
        }
    }

    private fun assertFullViewport(tag: String = "wear-round-list") {
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val list = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(
            "Full height, including the space above/below the former strip",
            root.height,
            list.height,
            1f,
        )
        assertEquals("Full viewport width", root.width, list.width, 1f)
    }

    private fun lazyList() =
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
                hasAnyAncestor(hasTestTag("wear-round-list"))
        )

    private fun scrollToEnd(last: String? = null) {
        if (
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
                    hasAnyAncestor(hasTestTag("wear-round-list")))
                .fetchSemanticsNodes()
                .isEmpty()
        )
            return
        if (last != null) lazyList().performScrollToNode(hasContentDescription(last))
        repeat(5) {
            compose.onNodeWithTag("wear-round-list").performTouchInput {
                swipeUp(durationMillis = 300)
            }
        }
        compose.waitForIdle()
    }

    private fun scrollToStart() {
        repeat(5) {
            compose.onNodeWithTag("wear-round-list").performTouchInput {
                swipeDown(durationMillis = 300)
            }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        // MediaStore photos survive Gradle's automatic test-app uninstall; private app files do
        // not.
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values =
            ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "$name-${diameter.toInt()}dp-font$fontScale.png",
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/TrainPilot-Wear-layout",
                )
            }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use {
            assertTrue(
                compose
                    .onRoot()
                    .captureToImage()
                    .asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            )
        }
    }

    @Test
    fun menuUsesFullViewportAndFirstLastButtonsAreReachableAndClickable() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            Watch { MainMenuScreen(false, false, { clicks.add("phone") }, { clicks.add(it) }) }
        }
        compose.waitForIdle()
        assertFullViewport()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val middle =
            compose.onNodeWithContentDescription("Kezdőlap").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Centered menu button uses at least 80% of the diameter",
            middle.width >= root.width * .8f,
        )
        val first =
            compose.onNodeWithContentDescription("Kezdőlap").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The first button must start below the curved title",
            first.top >= root.height * 32f / diameter,
        )
        screenshot("menu-initial")
        scrollToStart()
        compose.onNodeWithContentDescription("Kezdőlap").assertIsDisplayed().performClick()
        scrollToEnd()
        compose
            .onNodeWithContentDescription("Megnyitás telefonon")
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf("home", "phone"), clicks)
        screenshot("menu-last-control")
    }

    @Test
    fun everyListRouteHasFullViewportAndItsLastActionIsReachable() {
        val selected = mutableStateOf("home")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        val routes =
            listOf(
                "home" to "Főmenü",
                "menu" to "Megnyitás telefonon",
                "journal" to "‹ Főmenü",
                "journal-detail" to "‹ Napló",
                "quick" to "‹ Vissza",
                "metrics" to "‹ Vissza",
                "days" to "‹ Kezdőlap",
                "editor" to "Mégse",
                "timer" to "‹ Vissza",
                "discard" to "Edzés törlése",
                "finish" to "Edzés befejezése és mentése",
                "summary" to "Kész",
                "workout" to "Edzés befejezése",
                "timed-workout" to "Edzés befejezése",
            )
        for ((route, last) in routes) {
            compose.runOnIdle { selected.value = route }
            compose.waitForIdle()
            assertFullViewport()
            screenshot(route)
            if (route == "workout" || route == "timed-workout") {
                compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
                val caption =
                    compose
                        .onNodeWithText(
                            if (route == "workout") "Rögzítés" else "Mentés",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNode()
                        .boundsInRoot
                val dots =
                    compose.onNodeWithTag("wear-page-indicator").fetchSemanticsNode().boundsInRoot
                if (caption.height > 0f)
                    assertFalse(
                        "Pager dots must not paint over the action caption",
                        caption.overlaps(dots),
                    )
            }
            if (route == "home")
                compose.onNodeWithContentDescription("Megnyitás telefonon").assertIsDisplayed()
            scrollToEnd(last)
            compose.onNodeWithContentDescription(last).assertIsDisplayed()
            screenshot("$route-last-control")
        }
        compose.runOnIdle { selected.value = "calendar" }
        compose.waitForIdle()
        assertFullViewport("wear-calendar-week")
        screenshot("calendar-week")
        val today =
            LocalDate.now()
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "MMMM d., EEEE",
                        java.util.Locale.forLanguageTag("hu"),
                    )
                ) + ", ma"
        compose.onNodeWithContentDescription(today).performClick()
        assertFullViewport()
        scrollToEnd()
        compose.onNodeWithContentDescription("‹ Heti naptár").assertIsDisplayed()
        screenshot("calendar-day")
    }

    @Test
    fun journalDetailsAndAllWorkoutPagerPagesRemainScrollable() {
        val selected = mutableStateOf("journal")
        val liveHealth = mutableStateOf<JSONObject?>(null)
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value, liveHealth) } } }
        lazyList()
            .performScrollToNode(hasContentDescription("Hosszú nevű próbaedzés", substring = true))
        compose
            .onNodeWithContentDescription("Hosszú nevű próbaedzés", substring = true)
            .performClick()
        assertFullViewport()
        scrollToEnd()
        compose.onNodeWithContentDescription("‹ Napló").assertIsDisplayed()
        screenshot("journal-detail-with-metrics")
        compose.runOnIdle { selected.value = "workout" }
        compose.waitForIdle()
        assertFullViewport()
        val actionsBefore =
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            liveHealth.value =
                JSONObject()
                    .put("source", "wear_health_services")
                    .put("workoutId", "layout-test")
                    .put("state", "active")
                    .put("updatedAt", System.currentTimeMillis())
                    .put("heartRate", 93)
                    .put("totalCalories", 7.0)
        }
        compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
        assertEquals(
            "New measurements must not displace the controls",
            actionsBefore,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeDown() }
        assertEquals(
            "Dragging content must not displace the controls",
            actionsBefore,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        screenshot("workout-strength")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertFullViewport()
        screenshot("workout-menu")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertFullViewport()
        scrollToEnd("‹ Vissza")
        compose.onNodeWithContentDescription("‹ Vissza").assertIsDisplayed()
        screenshot("workout-measurements")
    }

    @Test
    fun homeStaysCenteredAndBothSideSwipesOpenTheMenu() {
        val selected = mutableStateOf("home")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        val actions = compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val program = compose.onNodeWithText("Erősítés").fetchSemanticsNode().boundsInRoot
        assertEquals("The home program title must be centered", root.center.x, program.center.x, 1f)
        compose.onNodeWithContentDescription("Főmenü").assertIsDisplayed()
        if (fontScale == 1f && diameter >= 192f) {
            val text =
                compose.onNodeWithText("A nap — teljes test").fetchSemanticsNode().boundsInRoot
            repeat(2) {
                compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeUp() }
                compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeDown() }
            }
            assertEquals(
                "Short home content must not move after vertical swipes",
                text,
                compose.onNodeWithText("A nap — teljes test").fetchSemanticsNode().boundsInRoot,
            )
        }
        assertEquals(
            actions,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        screenshot("home-fixed")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Mini napló").assertIsDisplayed()
        compose.runOnIdle { selected.value = "quick" }
        compose.runOnIdle { selected.value = "home" }
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeRight() }
        compose.onNodeWithContentDescription("Mini napló").assertIsDisplayed()
    }

    @Test
    fun editorsKeepMinusAndPlusBesideTheValueAndSaveCancelVisible() {
        val field = mutableStateOf("weight")
        val revision = mutableStateOf(0)
        val changes = mutableListOf<Pair<String, Double>>()
        var backs = 0
        val set = WearSet(1, "5", "5.5", false, 0, 0, 0.0)
        val workout =
            WearWorkout(
                1,
                "p",
                "Erősítés",
                "a",
                "",
                Instant.now().toString(),
                "editor-layout",
                0,
                60,
                0,
                listOf(
                    WearExercise(
                        "press",
                        "Földön fekve nyomás",
                        "weight",
                        "ism.",
                        "strength",
                        listOf(set),
                    )
                ),
                "A nap",
            )
        compose.setContent {
            Watch {
                key(field.value, revision.value) {
                    ValueEditor(
                        workout,
                        field.value,
                        { name, delta -> changes.add(name to delta) },
                        { backs++ },
                    )
                }
            }
        }
        for ((name, title, step) in
            listOf(Triple("weight", "Súly", .5), Triple("reps", "Ismétlés", 1.0))) {
            compose.runOnIdle { field.value = name }
            compose.onNodeWithContentDescription("✓ Mentés").assertIsDisplayed()
            compose.onNodeWithContentDescription("Mégse").assertIsDisplayed()
            val plus = compose.onNodeWithContentDescription("$title növelése")
            val minus = compose.onNodeWithContentDescription("$title csökkentése")
            if (fontScale == 1f) {
                val value =
                    compose.onNodeWithTag("wear-edited-value").fetchSemanticsNode().boundsInRoot
                val left = minus.fetchSemanticsNode().boundsInRoot
                val right = plus.fetchSemanticsNode().boundsInRoot
                assertTrue(left.right <= value.left && value.right <= right.left)
                assertTrue(kotlin.math.abs(left.center.y - right.center.y) < 1f)
                assertTrue(value.center.y in left.top..left.bottom)
            }
            plus.performClick()
            minus.performClick()
            plus.performClick()
            assertTrue("Editing alone must not save", changes.none { it.first == name })
            screenshot("editor-$name-fixed")
            compose.onNodeWithContentDescription("✓ Mentés").performClick()
            assertEquals(name to step, changes.last())
            val savedCount = changes.size
            compose.runOnIdle { revision.value++ }
            plus.performClick()
            compose.onNodeWithContentDescription("Mégse").performClick()
            assertEquals("Cancel must not save changes", savedCount, changes.size)
        }
        assertEquals(4, backs)
    }

    @Composable
    private fun Fixture(route: String, liveHealth: State<JSONObject?>? = null) {
        val set = WearSet(1, "5", "5.5", false, 30, 0, 0.0)
        val exercise =
            WearExercise(
                "press",
                "Földön fekve nyomás hosszú gyakorlatnévvel",
                "weight",
                if (route == "timed-workout") "mp/oldal" else "ism.",
                "strength",
                listOf(set),
            )
        val workout =
            mutableStateOf<WearWorkout?>(
                WearWorkout(
                    1,
                    "p",
                    "Erősítés",
                    "a",
                    "",
                    Instant.now().minusSeconds(60).toString(),
                    "layout-test",
                    0,
                    60,
                    0,
                    listOf(exercise),
                    "A nap",
                )
            )
        val quickExercise =
            WatchHomeExercise(
                "press",
                exercise.name,
                "weight",
                "ism.",
                "strength",
                "5",
                listOf(WatchHomeSet(1, "5.5")),
            )
        val day = WatchHomeDay("a", "A nap — teljes test", "p", "Erősítés", listOf(quickExercise))
        val journal =
            MiniJournalWorkout(
                "journal-layout",
                "2026-10-12T06:00:00Z",
                "2026-10-12T06:10:00Z",
                "Hosszú nevű próbaedzés",
                false,
                600,
                listOf(
                    MiniJournalExercise(
                        exercise.name,
                        listOf(MiniJournalSet(1, "5", 5.5, null, null, null)),
                    )
                ),
                MiniJournalMetric(
                    "wear_health_services",
                    averageHeartRate = 93.0,
                    maxHeartRate = 110.0,
                    totalCalories = 12.0,
                    steps = 65.0,
                    activeDurationSeconds = 600.0,
                ),
                null,
            )
        val home =
            mutableStateOf<WatchHomeSnapshot?>(
                WatchHomeSnapshot(
                    "p",
                    "Erősítés",
                    60,
                    false,
                    WatchHomeRecommendation("", "", day),
                    listOf(day),
                    listOf(WatchCalendarEntry(LocalDate.now().toString(), "", "", "planned", day)),
                    listOf(quickExercise),
                    listOf(journal),
                    "1.3.4",
                    true,
                )
            )
        val closure =
            mutableStateOf<WearClosure?>(
                WearClosure("c", "layout-test", "finishWorkout", "saved", "", 60, 1, 1)
            )
        val request = mutableStateOf<String?>(if (route == "timed-workout") "workout" else route)
        val health =
            JSONObject()
                .put("workoutId", "layout-test")
                .put("source", "wear_health_services")
                .put("state", "active")
                .put("updatedAt", System.currentTimeMillis())
                .put("heartRate", 93)
                .put("totalCalories", 7.0)
                .put("averageHeartRate", 90.0)
                .put("maxHeartRate", 110.0)
                .put("steps", 65)
                .put("activeDurationSeconds", 60.0)
        if (route == "quick" || route == "home") workout.value = null
        TrainPilotWearApp(
            workout = workout,
            home = home,
            closure = closure,
            restRemaining = mutableStateOf(0),
            onStart = { _, _ -> },
            onStartQuick = {},
            onAddQuick = {},
            onChange = { _, _ -> },
            onComplete = {},
            onPrevious = {},
            onNext = {},
            onSelectExercise = {},
            onSkipRest = {},
            onFinish = {},
            onDiscard = {},
            onResumeWorkout = {},
            onRetry = {},
            onDismissClosure = {},
            onOpenPhone = {},
            onRefreshJournal = {},
            health = liveHealth ?: mutableStateOf(health),
            measuring = mutableStateOf(true),
            gpsEnabled = mutableStateOf(false),
            requestedRoute = request,
            onRouteConsumed = { request.value = null },
            onEnableHealth = {},
            onDisableHealth = {},
            onToggleGps = {},
        )
    }
}
