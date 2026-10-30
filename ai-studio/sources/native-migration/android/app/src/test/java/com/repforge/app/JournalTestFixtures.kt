package com.repforge.app

import com.repforge.app.data.db.WorkoutHistoryEntity

internal object JournalTestFixtures {
    fun row(i: Int) = WorkoutHistoryEntity(id = "w-%04d".format(i), workoutKey = "key-$i", syncId = "sync-$i",
        started = "2026-10-24T12:00:00Z", finished = "2026-10-24T12:01:00Z", startedEpochMs = i.toLong(),
        finishedEpochMs = i + 60000L, durationSeconds = 60, programId = "program", programName = "Program",
        dayId = "day", dayName = "Nap", scheduleId = "", quickWorkout = false, originDevice = "phone",
        ownershipProtocol193 = 0, ownerEpoch193 = 0, effortFeedback = null, feedbackSkipped = false,
        feedbackUpdatedAt = 0, totalVolumeKg = 0.0, completedSetsCount = 0, totalSetsCount = 0,
        totalRepsCount = 0, hasPr226 = false, prRecordsJson226 = "[]", wearMetricsJson = null,
        health240Json = null, timeLogs162Json = null, activeIntervalsJson = null, editedAt = null,
        rawJson = "{\"large\":\"${"x".repeat(4096)}\"}")

}
