package com.repforge.app

import com.repforge.app.data.store.NativeTrainingModes
import com.repforge.app.ui.coach.NativeCoachExerciseAdvice
import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeTrainingModeProgression
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeTrainingModeProgressionTest {
    private val readiness = NativeCoachReadiness.Result(null, null, null, null, null,
        null, null, null, null, 0, false)
    private fun mode(rule: String, style: String = "normal", explicit: Boolean = true) =
        NativeTrainingModes.Effective(programId = "p", trainingStyle = style,
            progressionMode = rule, explicitChoice = explicit,
            source = if (explicit) "program" else "legacy")
    private fun library() = JSONObject().put("id", "press").put("loadType", "total")
        .put("measurementType", "reps").put("repUnit", "ism.")
    private fun target() = NativeCoachExerciseAdvice.Target("reps", 8.0, 12.0,
        rirMin = 1.0, rirMax = 2.0, sets = 3)
    private fun occurrence(reps: Int = 12, rir: Int? = null, weight: Double = 10.0,
        effort: String = "good", complete: Boolean = true, pain: Boolean = false): NativeCoachExerciseAdvice.Occurrence {
        val sets = JSONArray().apply {
            repeat(3) { put(JSONObject().put("reps", reps).put("weight", weight)
                .put("done", complete).also { if (rir != null) it.put("actualRir", rir) }) }
        }
        val exercise = JSONObject().put("id", "press").put("sets", sets)
            .put("effort", if (pain) "pain" else effort).put("setRir229", true)
        val workout = JSONObject().put("feedback", JSONObject().put("rating", if (pain) "pain" else "good"))
        return NativeCoachExerciseAdvice.occurrence(workout, exercise, target())
    }
    private fun preview(rule: String, rows: List<NativeCoachExerciseAdvice.Occurrence>,
        style: String = "normal", r: NativeCoachReadiness.Result = readiness) =
        NativeTrainingModeProgression.compute(mode(rule, style), library(), target(), rows, r, 2.5)!!

    @Test fun legacyDelegatesToExistingCoachWithoutSecondEngine() {
        assertNull(NativeTrainingModeProgression.compute(mode("linear", explicit = false), library(), target(),
            listOf(occurrence()), readiness, 2.5))
    }
    @Test fun doubleRequiresThreeSafeTopCompletions() {
        val one = preview("double", listOf(occurrence()))
        assertEquals("hold", one.action)
        assertEquals("double-await-stability", one.reason)
        val three = preview("double", List(3) { occurrence() })
        assertEquals("increase", three.action)
        assertEquals(12.5, three.proposedWeight, 0.0)
        assertFalse(three.autoApply)
    }
    @Test fun doubleGivesOneExtraRepBeforeWeightIncrease() {
        val p = preview("double", listOf(occurrence(reps = 10)))
        assertEquals("reps", p.action)
        assertEquals(11.0, p.nextReps.first(), 0.0)
    }
    @Test fun linearRequiresTwoStableSessions() {
        assertEquals("hold", preview("linear", listOf(occurrence())).action)
        assertEquals("increase", preview("linear", List(2) { occurrence(reps = 8) }).action)
    }
    @Test fun actualRirMissingIsNotZeroAndRirZeroIsMeasured() {
        val missing = occurrence(rir = null)
        assertNull(NativeTrainingModeProgression.observedRir(missing.exercise))
        assertEquals("rir-feedback-missing", preview("rir", listOf(missing)).reason)
        val zero = occurrence(rir = 0)
        assertEquals(0.0, NativeTrainingModeProgression.observedRir(zero.exercise)!!, 0.0)
        assertEquals("rir-overexertion", preview("rir", listOf(zero)).reason)
    }
    @Test fun rirNeedsTwoActualStableHighReserves() {
        assertEquals("hold", preview("rir", listOf(occurrence(rir = 5))).action)
        assertEquals("increase", preview("rir", List(2) { occurrence(rir = 5) }).action)
    }
    @Test fun painPoorReadinessAndIncompleteBlockIncreases() {
        assertEquals("pain", preview("linear", listOf(occurrence(pain = true), occurrence())).reason)
        assertEquals("incomplete", preview("linear", listOf(occurrence(complete = false), occurrence())).reason)
        assertEquals("low-readiness", preview("linear", List(2) { occurrence() },
            r = readiness.copy(score = 40.0)).reason)
    }
    @Test fun hitOnlyChangesProposedSetsNeverChangesHistoricalData() {
        val rows = List(2) { occurrence() }
        val before = rows.first().exercise.toString()
        val result = preview("linear", rows, style = "hit")
        assertEquals(2, result.workingSets)
        assertEquals("increase", result.action)
        assertFalse(result.autoApply)
        assertEquals(before, rows.first().exercise.toString())
    }
    @Test fun distanceAndBodyweightStayConservative() {
        assertEquals("non-strength-activity", NativeTrainingModeProgression.compute(
            mode("linear"), library().put("measurementType", "distance"), target(),
            List(2) { occurrence() }, readiness, 2.5)?.reason)
        val body = NativeTrainingModeProgression.compute(mode("linear"),
            library().put("loadType", "bodyweight"), target(),
            List(2) { occurrence() }, readiness, 2.5)!!
        assertEquals("variation", body.action)
        assertEquals(0.0, body.proposedWeight, 0.0)
    }
}
