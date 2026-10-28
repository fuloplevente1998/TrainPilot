package com.repforge.app

import com.repforge.app.ui.navigation.NativeMainMenu
import org.junit.Assert.*
import org.junit.Test

class NativeMainMenuTest {
    @Test fun topNavigationScrollsForSmallScreensOrScaledTextWithoutLosingSelectedTab() {
        assertTrue(NativeMainMenu.topTabsScrollable(320f, 1.0f))
        assertTrue(NativeMainMenu.topTabsScrollable(359f, 1.0f))
        assertTrue(NativeMainMenu.topTabsScrollable(360f, 1.16f))
        assertTrue(NativeMainMenu.topTabsScrollable(480f, 2.0f))
        assertFalse(NativeMainMenu.topTabsScrollable(360f, 1.15f))
        assertFalse(NativeMainMenu.topTabsScrollable(480f, 1.0f))
        assertEquals(0, NativeMainMenu.topTabScrollTarget(260, 0))
        assertEquals(65, NativeMainMenu.topTabScrollTarget(260, 1))
        assertEquals(130, NativeMainMenu.topTabScrollTarget(260, 2))
        assertEquals(260, NativeMainMenu.topTabScrollTarget(260, 4))
        assertEquals(260, NativeMainMenu.topTabScrollTarget(260, 50))
        assertEquals(0, NativeMainMenu.topTabScrollTarget(260, -1))
        assertEquals(0, NativeMainMenu.topTabScrollTarget(0, 4))
        assertEquals(Int.MAX_VALUE, NativeMainMenu.topTabScrollTarget(Int.MAX_VALUE, 4))
    }
    @Test fun acceptedHierarchyHasFiveTabsWithSettingsOutsideTheTabBar() {
        assertEquals(listOf("home", "training", "journal", "progress", "health"), NativeMainMenu.routes)
        assertEquals(listOf("Kezdőlap", "Edzés", "Napló", "Fejlődés", "Egészség"), NativeMainMenu.labels("hu"))
        assertFalse("settings" in NativeMainMenu.routes)
        assertTrue("settings" in NativeMainMenu.secondaryRoutes)
    }
    @Test fun activeTrainingReturnsToItsCanonicalSessionInsteadOfStartingAnotherOne() {
        assertEquals("active_workout", NativeMainMenu.destination("training", true))
        assertEquals("training", NativeMainMenu.destination("training", false))
        assertEquals("programs", NativeMainMenu.destination("programs", true))
        assertEquals("exercise_library", NativeMainMenu.destination("exercise_library", true))
        assertEquals("settings", NativeMainMenu.destination("settings", true))
    }
    @Test fun secondaryViewsKeepTheirPrimaryParent() {
        for (route in listOf("active_workout", "quick_workout", "quick_outdoor", "programs", "exercise_library"))
            assertEquals("training", NativeMainMenu.selected(route))
        for (route in listOf("calendar", "coach")) assertEquals("home", NativeMainMenu.selected(route))
        assertEquals("progress", NativeMainMenu.selected("progress"))
        assertEquals("health", NativeMainMenu.selected("weight_journal"))
    }
    @Test fun directWeightAddKeepsHealthParentAndNativeJournalTitle() {
        assertTrue("weight_journal_add" in NativeMainMenu.secondaryRoutes)
        assertEquals("health", NativeMainMenu.selected("weight_journal_add"))
        assertEquals("weight_journal_add", NativeMainMenu.destination("weight_journal_add", false))
        for (lang in listOf("hu", "en", "de", "ro"))
            assertEquals(NativeMainMenu.title("weight_journal", lang),
                NativeMainMenu.title("weight_journal_add", lang))
    }

    @Test fun settingsKeepsTheOriginTabIncludingActiveWorkoutAndNestedViews() {
        for (origin in NativeMainMenu.routes + listOf("active_workout", "calendar", "coach", "programs", "exercise_library"))
            assertEquals(NativeMainMenu.selected(origin), NativeMainMenu.selected("settings", origin))
        assertEquals("home", NativeMainMenu.selected("settings", "settings"))
    }
    @Test fun openingSettingsNeverPausesWhileActualEditorLeaveDoes() {
        assertFalse(NativeMainMenu.pausesWorkout("active_workout", "settings"))
        assertFalse(NativeMainMenu.pausesWorkout("settings", "active_workout", "active_workout"))
        for (target in listOf("home", "journal", "health", "progress", "calendar", "coach", "programs")) {
            assertTrue(NativeMainMenu.pausesWorkout("active_workout", target))
            assertTrue(NativeMainMenu.pausesWorkout("settings", target, "active_workout"))
        }
    }
    @Test fun settingsOpenedFromAnotherTabDoesNotManufactureAnEditorLeave() {
        for (origin in NativeMainMenu.routes + listOf("programs", "coach", "calendar"))
            assertFalse(NativeMainMenu.pausesWorkout("settings", "journal", origin))
    }
    @Test fun localizedToolbarAndAllDestinationsAreReachableWithoutEmptyLabels() {
        for (language in listOf("hu", "en", "de", "ro")) {
            assertEquals(5, NativeMainMenu.labels(language).size)
            for (route in NativeMainMenu.routes + NativeMainMenu.secondaryRoutes) {
                assertTrue(NativeMainMenu.title(route, language).isNotBlank())
                assertTrue(NativeMainMenu.selected(route) in NativeMainMenu.routes)
                assertTrue(NativeMainMenu.destination(route, false).isNotBlank())
            }
            assertTrue(NativeMainMenu.backLabel(language).isNotBlank())
        }
        assertEquals("Beállítások", NativeMainMenu.title("settings", "hu"))
        assertEquals("Settings", NativeMainMenu.title("settings", "en"))
        assertEquals("Gyakorlatkönyvtár", NativeMainMenu.title("exercise_library", "hu"))
        assertEquals("Vissza", NativeMainMenu.backLabel("hu"))
    }
    @Test fun lateFinalizationCannotTrapSettingsInsteadOfAllowingEditorRetry() {
        assertTrue(NativeMainMenu.navigationAllowed("active_workout", false, false, true, false, true))
        for (target in NativeMainMenu.routes + listOf("settings", "calendar", "coach", "programs"))
            assertFalse(NativeMainMenu.navigationAllowed(target, false, false, true, false, true))
    }
    @Test fun runningSaveRestoreAndStartStayProtectedEvenForAnEditorReturn() {
        assertFalse(NativeMainMenu.navigationAllowed("active_workout", true, false, false, false, false))
        assertFalse(NativeMainMenu.navigationAllowed("active_workout", false, true, true, false, true))
        assertFalse(NativeMainMenu.navigationAllowed("active_workout", false, false, false, true, false))
        assertTrue(NativeMainMenu.navigationAllowed("settings", false, false, false, false, false))
        assertFalse(NativeMainMenu.navigationAllowed("settings", false, false, false, false, true))
    }
    @Test fun unknownDestinationsCannotEnterTheBackStack() {
        assertThrows(IllegalArgumentException::class.java) { NativeMainMenu.destination("unknown", false) }
    }
}
