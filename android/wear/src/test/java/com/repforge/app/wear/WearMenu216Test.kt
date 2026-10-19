package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearMenu216Test {
    @Test fun activeWorkoutAndQuickActionsComeBeforeSettingsWithoutParallelStarts() {
        assertEquals("quick",WearMenu216.entries(false,false).first().first)
        val active=WearMenu216.entries(true,false)
        assertEquals("resume",active.first().first);assertFalse(active.any {it.first=="quick"})
        assertEquals(listOf("resume","add"),WearMenu216.entries(true,true).take(2).map {it.first})
        assertTrue(WearMenu216.entries(false,false).indexOfFirst {it.first=="planned"}<WearMenu216.entries(false,false).indexOfFirst {it.first=="settings"})
    }
    @Test fun indoorAndUnknownDistanceTypesNeverInferAutomaticLocationConsent() {
        for(id in listOf("running","walking","jogging","cycling","inline-skating","hiking"))assertTrue(WearMenu216.eligible(id,true))
        for(id in listOf("treadmill","indoor-cycling","plank","squat","unknown"))assertFalse(WearMenu216.eligible(id,true))
        assertFalse(WearMenu216.eligible("running",false))
    }
    @Test fun everyNewMenuAndConsentLabelExistsInAllSupportedLanguages() {
        for(lang in listOf("hu","en","de","ro"))for(key in listOf("menu","resume","quick","add","planned","journal","metrics","settings","home","calendar","days","phone","discard","start","cancel","gps","on","off","remember","aggregate","next","back","consent"))assertTrue(WearMenu216.text(key,lang).isNotBlank())
    }
}
