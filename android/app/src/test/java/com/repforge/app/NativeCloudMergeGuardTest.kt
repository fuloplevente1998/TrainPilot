package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeCloudMergeGuardTest {
    @Test fun keyOrderDoesNotCreateAFalseConflict() {
        NativeCloudMergeGuard.requireUnchanged(JSONObject("""{"history":[],"settings":{"language":"hu","themeKey":"classicBlue"}}"""),
            JSONObject("""{"settings":{"themeKey":"classicBlue","language":"hu"},"history":[]}"""))
    }
    @Test fun sameCountWorkoutEditAndReplacementAreConflicts() {
        val before = JSONObject("""{"history":[{"id":"one","weight":10}]}""")
        for (after in listOf("""{"history":[{"id":"one","weight":12}]}""", """{"history":[{"id":"other","weight":10}]}"""))
            assertThrows(IllegalStateException::class.java) { NativeCloudMergeGuard.requireUnchanged(before, JSONObject(after)) }
    }
    @Test fun changedPreferencesOrDraftCannotBeOverwrittenByAnOlderNetworkMerge() {
        val before = JSONObject("""{"history":[],"settings":{"language":"hu"}}""")
        val original = before.toString()
        for (after in listOf("""{"history":[],"settings":{"language":"ro"}}""", """{"history":[],"settings":{"language":"hu"},"draft":{"id":"new"}}"""))
            assertThrows(IllegalStateException::class.java) { NativeCloudMergeGuard.requireUnchanged(before, JSONObject(after)) }
        assertEquals(original, before.toString())
    }
}
