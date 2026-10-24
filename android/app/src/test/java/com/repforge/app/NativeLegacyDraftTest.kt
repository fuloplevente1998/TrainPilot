package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeLegacyDraftTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private fun draft() = JSONObject("""{"savedAt":"2026-10-23T10:10:00Z","current":0,"restEndAt":9999999999999,
        "session":{"started":"2026-10-23T10:00:00Z","syncId":"legacy","timerOwner":"phone",
        "activeIntervals":[{"start":"2026-10-23T10:00:00Z","end":null}],
        "exercises":[{"id":"running","sets":[{"done":true,"reps":"61","distanceMeters":69,"gpsRouteKey163":"private-key","actualRir":2}]}]}}""")
    private fun prepare(d: JSONObject) = NativeLegacyDraft.prepare(d.toString(), setOf("running"), now)
    private fun row(s: JSONObject) = s.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun reject(d: JSONObject) { assertThrows(Exception::class.java) { prepare(d) } }

    @Test fun preflightLeavesTheOriginalByteIdenticalAndPreservesActualMeasurements() {
        val d = draft(); val original = d.toString(); val next = prepare(d)
        assertEquals(original, d.toString()); assertEquals("61", row(next).getString("reps"))
        assertEquals(69, row(next).getInt("distanceMeters")); assertTrue(row(next).getBoolean("done"))
        assertEquals(2, row(next).getInt("actualRir")); assertEquals("private-key", row(next).getString("gpsRouteKey163"))
    }
    @Test fun repeatedPreflightKeepsWorkoutAndSynthesizedSetIdentityStable() {
        val a = prepare(draft()); val b = prepare(draft())
        assertEquals("legacy", a.getString("workoutId")); assertEquals(row(a).getString("setId"), row(b).getString("setId"))
    }
    @Test fun existingSetIdentityIsPreserved() {
        val d = draft(); row(d.getJSONObject("session")).put("setId", "original-set")
        assertEquals("original-set", row(prepare(d)).getString("setId"))
    }
    @Test fun missingLegacyIdentityUsesAcceptedStartedIdentity() {
        val d = draft(); d.getJSONObject("session").remove("syncId")
        assertEquals("2026-10-23T10:00:00Z", prepare(d).getString("workoutId"))
    }
    @Test fun downtimeIsNotCountedAndNoOldClockBecomesLive() {
        val d = draft(); val s = d.getJSONObject("session")
        s.put(NativeSetStopwatch.FIELD, JSONObject().put("running", true))
        s.put(NativeGpsMeasurement.FIELD, JSONObject().put("state", "running"))
        val next = prepare(d)
        assertTrue(next.getBoolean("workoutPaused")); assertEquals(0, next.getInt("restEndAt"))
        assertEquals("2026-10-23T10:10:00Z", next.getJSONArray("activeIntervals").getJSONObject(0).getString("end"))
        assertFalse(next.has(NativeSetStopwatch.FIELD)); assertFalse(next.has(NativeGpsMeasurement.FIELD))
    }
    @Test fun watchOwnerOrOwnershipEpochRequiresReconciliation() {
        for (pair in listOf("timerOwner" to "watch", "ownerEpoch193" to 1)) {
            val d = draft(); d.getJSONObject("session").put(pair.first, pair.second); reject(d)
        }
    }
    @Test fun closedDeletedCancelledOrFinalizingDraftIsRejected() {
        for (key in listOf("deleted", "cancelled", "finalizationPending", "finished")) {
            val d = draft(); d.getJSONObject("session").put(key, if (key == "finished") now.toString() else true); reject(d)
        }
    }
    @Test fun conflictingIdsAndDuplicateSetIdsAreRejected() {
        val d = draft(); d.getJSONObject("session").put("workoutId", "other"); reject(d)
        val duplicate = draft(); val s = duplicate.getJSONObject("session"); row(s).put("setId", "same")
        s.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").put(JSONObject(row(s).toString())); reject(duplicate)
    }
    @Test fun missingLibraryEntryCannotSilentlyDropARecordedExercise() {
        assertThrows(Exception::class.java) { NativeLegacyDraft.prepare(draft().toString(), emptySet(), now) }
    }
    @Test fun futureOrReversedDatesAreRejected() {
        for (date in listOf("2027-01-01T00:00:00Z", "2026-10-22T00:00:00Z")) {
            val d = draft().put("savedAt", date); reject(d)
        }
    }
    @Test fun invalidActiveIntervalIsRejectedInsteadOfInventingElapsedTime() {
        val d = draft(); d.getJSONObject("session").getJSONArray("activeIntervals").getJSONObject(0)
            .put("start", "2026-10-24T10:00:00Z"); reject(d)
    }
    @Test fun selectionIsClampedWithoutDiscardingTheRecordedRow() {
        assertEquals(0, prepare(draft().put("current", 100)).getInt("currentExercise"))
        assertEquals(0, prepare(draft().put("current", -3)).getInt("currentExercise"))
    }
}
