package com.repforge.app

import com.repforge.app.ui.navigation.NativeMainMenu
import org.junit.Assert.*
import org.junit.Test

class NativeMainMenuTest {
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
        assertEquals("settings", NativeMainMenu.destination("settings", true))
    }
    @Test fun secondaryViewsKeepTheirPrimaryParent() {
        for (route in listOf("active_workout", "quick_workout", "quick_outdoor", "programs"))
            assertEquals("training", NativeMainMenu.selected(route))
        for (route in listOf("calendar", "coach")) assertEquals("home", NativeMainMenu.selected(route))
        assertEquals("progress", NativeMainMenu.selected("progress"))
    }
    @Test fun settingsKeepsTheOriginTabIncludingActiveWorkoutAndNestedViews() {
        for (origin in NativeMainMenu.routes + listOf("active_workout", "calendar", "coach", "programs"))
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
