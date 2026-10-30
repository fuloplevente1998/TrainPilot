package com.repforge.app.ui.home

import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.coach.NativeCoachTargets
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId

/** One Room snapshot: shared Coach selection, one program catalog read and indexed daily count. */
internal class NativeHomeRepository(private val db: TrainPilotDatabase) {
    data class Snapshot(val program: ProgramWithDays?, val target: HomeWorkoutTarget?,
        val exerciseCount: Int, val workoutCount: Int)

    suspend fun read(activeProgramId: String, language: String, now: Instant, zone: ZoneId): Snapshot = db.withTransaction {
        val today = now.atZone(zone).toLocalDate()
        val daysByProgram = db.programDao().getProgramDaysForSnapshot().groupBy { it.programId }
        val programs = db.programDao().getProgramRowsForSnapshot().map { program ->
            ProgramWithDays(program, daysByProgram[program.id].orEmpty())
        }
        val selected = NativeCoachTargets(db).select(activeProgramId, language, now, zone, programs)
        val program = selected?.let { target -> programs.single { it.program.id == target.programId } }
        val day = selected?.let { target -> program!!.days.single { it.dayId == target.dayId } }
        Snapshot(program, selected?.let { HomeWorkoutTarget(it.programId, it.dayId, it.schedule?.id) },
            day?.let { JSONArray(it.exercisesJson).length() } ?: 0,
            db.workoutHistoryDao().getWorkoutCountBetween(today.atStartOfDay(zone).toInstant().toEpochMilli(),
                today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()))
    }

    /** Observe real writes, including same-count edits. Never load full history to detect a change. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun invalidations() = callbackFlow<Unit> {
        val observer = object : InvalidationTracker.Observer("programs", "program_days", "calendar_entries",
            "workout_history", "history_tombstones") {
            override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
        }
        db.invalidationTracker.addObserver(observer)
        trySend(Unit)
        awaitClose { db.invalidationTracker.removeObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)
}
