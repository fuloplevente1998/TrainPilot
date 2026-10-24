package com.repforge.app

import com.repforge.app.ui.coach.NativeCoachAdviceRepository
import com.repforge.app.ui.coach.NativeCoachExerciseAdvice
import com.repforge.app.ui.coach.NativeCoachReadiness
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Expected actions come from the actual final main functions, not this Kotlin implementation. */
@RunWith(Parameterized::class)
class NativeCoachAdviceParityTest(private val name: String, private val data: JSONObject) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val raw = requireNotNull(NativeCoachAdviceParityTest::class.java.getResourceAsStream("/native-coach-main-7473.json"))
                .bufferedReader().use { it.readText() }
            val fixture = JSONObject(raw)
            require(fixture.getJSONObject("source").getString("gitBlob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
            val cases = fixture.getJSONArray("cases")
            return (0 until cases.length()).map { cases.getJSONObject(it).let { row -> arrayOf<Any>(row.getString("name"), row) } }
        }
    }
    @Test fun finalMainRecommendationAndCoachActionRemainEquivalentWithoutChangingInputs() {
        val original = data.toString()
        val library = data.getJSONObject("library")
        val rx = data.getJSONObject("program").getJSONObject("prescriptions").optJSONObject(library.getString("id")) ?: JSONObject()
        val reference = data.getJSONObject("reference")
        val target = NativeCoachExerciseAdvice.target(library, rx)
        val expectedTarget = reference.getJSONObject("target")
        assertEquals(name, expectedTarget.getString("kind"), target.kind)
        for ((key, actual) in listOf("min" to target.min, "max" to target.max)) {
            if (expectedTarget.isNull(key)) assertNull(name, actual)
            else assertEquals(name, expectedTarget.getDouble(key), actual!!, .00001)
        }
        if (target.kind == "rir") {
            assertEquals(expectedTarget.getDouble("rirMin"), target.rirMin!!, .00001)
            assertEquals(expectedTarget.getDouble("rirMax"), target.rirMax!!, .00001)
        }
        val selector = NativeCoachAdviceRepository.Selection(data.getJSONObject("program").getString("id"), library, rx,
            data.getJSONObject("program"), java.time.Instant.parse(data.getString("now")))
        val sourceHistory = data.getJSONArray("history")
        val selected = (0 until sourceHistory.length()).map { sourceHistory.getJSONObject(it) }
            .sortedByDescending { it.optString("finished") }.mapNotNull { selector.accept(it) }.take(4)
        val expectedRows = reference.getJSONArray("rows")
        assertEquals(name, (0 until expectedRows.length()).map { expectedRows.getJSONObject(it).getJSONObject("workout").getString("id") },
            selected.map { it.first.getString("id") })
        val occurrences = selected.map { row -> NativeCoachExerciseAdvice.occurrence(row.first, row.second, target) }
        val ready = data.getJSONObject("readiness")
        val readiness = NativeCoachReadiness.Result(score = ready.optDouble("score"), trainpilotScore = null, samsungScore = null,
            samsungTime = null, samsungSource = null, sleepMinutes = null, hrvMs = null, baselineMs = null, load = null,
            trainpilotObservations = ready.optInt("parts"), pain = false)
        val settings = data.getJSONObject("settings").optJSONObject("progressionSteps") ?: JSONObject()
        val overrides = settings.keys().asSequence().associateWith { settings.getDouble(it) }
        val merged = JSONObject(library.toString()).put("loadType", rx.optString("loadType", library.getString("loadType")))
        val step = NativeCoachExerciseAdvice.step(merged, occurrences.firstOrNull()?.weight ?: 0.0, overrides,
            if (reference.isNull("observed")) null else reference.getDouble("observed"))
        assertEquals(name, reference.getDouble("step"), step, .00001)
        val actual = NativeCoachExerciseAdvice.recommend(library, rx, occurrences, target, readiness, step)
        val expected = reference.getJSONObject("recommendation")
        assertEquals(name, expected.getString("action"), actual.action)
        assertEquals(name, expected.getDouble("weight"), actual.weight, .00001)
        assertEquals(name, expected.getBoolean("autoApply"), actual.autoApply)
        assertEquals(name, expected.getInt("historyCount"), actual.historyCount)
        if (expected.isNull("variation")) assertNull(actual.variation)
        else {
            val variation = expected.getJSONObject("variation")
            val actualVariation = requireNotNull(actual.variation)
            assertEquals(variation.getString("id"), actualVariation.exerciseId)
            assertEquals(variation.getString("variation"), actualVariation.name)
            assertEquals(variation.getInt("level"), actualVariation.level)
        }
        val coach = NativeCoachExerciseAdvice.forCoach(actual, reference.optString("lastEffort"), readiness)
        assertEquals(name, reference.getString("coachAction"), coach.action)
        assertFalse("Coach display cannot auto-apply an edit", coach.autoApply)
        assertEquals("Pure advice cannot edit source documents", original, data.toString())
    }
}
