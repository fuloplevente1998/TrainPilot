package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearWorkoutFeedbackTest {
    @Test fun fiveChoicesKeepTheCanonicalPhoneKeys() {
        assertEquals(listOf("easy", "light", "good", "challenging", "hard"), WearWorkoutFeedback.choices.map { it.first })
        WearWorkoutFeedback.choices.forEach { (key, _) ->
            assertEquals(key, WearWorkoutFeedback.answer(key).getString("effort"))
        }
        assertEquals("pain", WearWorkoutFeedback.answer("pain").getString("effort"))
        assertTrue(WearWorkoutFeedback.answer(null).getBoolean("skipped"))
        assertTrue(runCatching { WearWorkoutFeedback.answer("invalid") }.isFailure)
    }
    @Test fun pendingQuestionKeepsTheFinishedWorkoutIdentityAndRejectsInvalidData() {
        val value = JSONObject().put("workoutId", "watch-id").put("started", "2026-10-14T10:00:00Z")
        assertEquals(WatchWorkoutFeedback("watch-id", "2026-10-14T10:00:00Z"), WatchWorkoutFeedback.parse(value))
        assertNull(WatchWorkoutFeedback.parse(null))
        assertNull(WatchWorkoutFeedback.parse(JSONObject(value.toString()).put("workoutId", "")))
        assertNull(WatchWorkoutFeedback.parse(JSONObject(value.toString()).put("started", "invalid")))
        val home = WatchHomeStore.parse(JSONObject().put("pendingFeedback", value).toString())!!
        assertEquals("watch-id", home.pendingFeedback?.workoutId)
        assertNull(WatchHomeStore.parse("{}")!!.pendingFeedback)
    }
}
