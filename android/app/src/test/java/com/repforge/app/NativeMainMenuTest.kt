package com.repforge.app

import com.repforge.app.ui.navigation.NativeMainMenu
import org.junit.Assert.*
import org.junit.Test

class NativeMainMenuTest {
    @Test fun mainKeepsItsAcceptedEightEntriesAndOrder() {
        assertEquals(listOf("home","training","health","programs","journal","calendar","coach","settings"),NativeMainMenu.routes)
        assertEquals(listOf("Kezdőlap","Edzés","Egészség","Programok","Napló","Naptár","Coach","Beállítások"),NativeMainMenu.labels("hu"))
    }
    @Test fun activeTrainingReturnsToItsCanonicalSessionInsteadOfStartingAnotherOne() {
        assertEquals("active_workout",NativeMainMenu.destination("training",true))
        assertEquals("training",NativeMainMenu.destination("training",false))
        assertEquals("programs",NativeMainMenu.destination("programs",true))
    }
    @Test fun secondaryViewsHighlightTheirExistingPrimaryEntry() {
        assertEquals("training",NativeMainMenu.selected("active_workout"))
        assertEquals("training",NativeMainMenu.selected("quick_outdoor"))
        assertEquals("coach",NativeMainMenu.selected("progress"))
        assertEquals("calendar",NativeMainMenu.selected("calendar"))
    }
    @Test fun everySupportedLanguageHasEightNamedEntriesAndUnknownRoutesAreRejected() {
        for (language in listOf("hu","en","de","ro")) {
            assertEquals(8,NativeMainMenu.labels(language).size)
            assertTrue(NativeMainMenu.labels(language).all { it.isNotBlank() })
        }
        assertThrows(IllegalArgumentException::class.java) { NativeMainMenu.destination("unknown",false) }
    }
}
