package com.repforge.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Transaction
    @Query("SELECT * FROM programs ORDER BY sortOrder ASC, id ASC")
    fun observeProgramsWithDays(): Flow<List<ProgramWithDays>>

    @Transaction
    @Query("SELECT * FROM programs ORDER BY sortOrder ASC, id ASC")
    suspend fun getProgramsWithDays(): List<ProgramWithDays>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :programId LIMIT 1")
    suspend fun getProgramWithDaysById(programId: String): ProgramWithDays?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgramDays(days: List<ProgramDayEntity>)

    @Query("DELETE FROM program_days WHERE programId = :programId")
    suspend fun deleteDaysForProgram(programId: String)

    @Query("DELETE FROM programs WHERE id = :programId AND builtin = 0")
    suspend fun deleteCustomProgram(programId: String)

    @Query("DELETE FROM programs")
    suspend fun deleteAllPrograms()

    @Transaction
    suspend fun saveProgramWithDays(program: ProgramEntity, days: List<ProgramDayEntity>) {
        upsertProgram(program)
        deleteDaysForProgram(program.id)
        upsertProgramDays(days)
    }
}

@Dao
interface ExerciseLibraryDao {
    @Query("SELECT * FROM exercise_library ORDER BY custom DESC, nameHu ASC")
    fun observeAllExercises(): Flow<List<ExerciseLibraryEntity>>

    @Query("SELECT * FROM exercise_library ORDER BY custom DESC, nameHu ASC")
    suspend fun getAllExercises(): List<ExerciseLibraryEntity>

    @Query("SELECT * FROM exercise_library WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): ExerciseLibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseLibraryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseLibraryEntity)

    @Query("DELETE FROM exercise_library WHERE id = :id AND custom = 1")
    suspend fun deleteCustomExercise(id: String)

    @Query("DELETE FROM exercise_library WHERE custom = 1")
    suspend fun deleteAllCustomExercises()
}

@Dao
interface WorkoutHistoryDao {
    @Query("SELECT rawJson FROM workout_history WHERE (CASE WHEN finishedEpochMs > 0 THEN finishedEpochMs ELSE startedEpochMs END) BETWEEN :fromEpoch AND :toEpoch ORDER BY (CASE WHEN finishedEpochMs > 0 THEN finishedEpochMs ELSE startedEpochMs END) DESC, id LIMIT 3")
    suspend fun getCoachRecentHistory(fromEpoch: Long, toEpoch: Long): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM workout_history WHERE scheduleId = :scheduleId)")
    suspend fun hasScheduleHistory(scheduleId: String): Boolean

    @Query("SELECT id, started, programName, dayName, scheduleId FROM workout_history WHERE startedEpochMs >= :fromEpoch AND startedEpochMs < :toEpoch ORDER BY startedEpochMs, id")
    fun observeCalendarHistory(fromEpoch: Long, toEpoch: Long): Flow<List<CalendarHistorySummary>>

    @Query("SELECT EXISTS(SELECT 1 FROM workout_history WHERE id = :id OR syncId = :id OR started = :started)")
    suspend fun hasFinishedIdentity(id: String, started: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM history_tombstones WHERE primaryId = :id OR workoutKey = :id OR instr(idsJson, :quotedId) > 0)")
    suspend fun isTombstoned(id: String, quotedId: String): Boolean

    @Query("""
        SELECT DISTINCT w.* FROM workout_history w
        INNER JOIN workout_history_exercises e ON e.workoutHistoryId = w.id
        WHERE e.exerciseId = :exerciseId AND e.loadType = :loadType AND e.repUnit = :repUnit
          AND w.finishedEpochMs > w.startedEpochMs AND w.finishedEpochMs <= :now
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
              WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY w.finishedEpochMs DESC, w.startedEpochMs DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPrefillCandidates(exerciseId: String, loadType: String, repUnit: String,
        now: Long, limit: Int, offset: Int): List<WorkoutHistoryEntity>

    @Query("""
        SELECT DISTINCT w.* FROM workout_history w
        INNER JOIN workout_history_exercises e ON e.workoutHistoryId = w.id
        WHERE e.exerciseId = :exerciseId AND w.finishedEpochMs >= :fromEpoch AND w.finishedEpochMs <= :toEpoch
          AND (:beforeEpoch IS NULL OR w.finishedEpochMs < :beforeEpoch OR
            (w.finishedEpochMs = :beforeEpoch AND w.id < :beforeId))
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY w.finishedEpochMs DESC, w.id DESC LIMIT :limit
    """)
    suspend fun getCoachExerciseCandidates(exerciseId: String, fromEpoch: Long, toEpoch: Long,
        beforeEpoch: Long?, beforeId: String, limit: Int): List<WorkoutHistoryEntity>

    @Query("""
        SELECT DISTINCT ROUND(s.weight, 2) FROM workout_history_sets s
        INNER JOIN workout_history_exercises e ON e.rowId = s.historyExerciseRowId
        INNER JOIN workout_history w ON w.id = s.workoutHistoryId
        WHERE s.exerciseId = :exerciseId AND e.loadType = :loadType AND s.done = 1 AND s.weight > 0
          AND ROUND(s.weight, 2) > :after
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY ROUND(s.weight, 2) LIMIT :limit
    """)
    suspend fun getCoachObservedWeights(exerciseId: String, loadType: String, after: Double, limit: Int): List<Double>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC")
    fun observeAllWorkoutsWithDetails(): Flow<List<WorkoutHistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC LIMIT :limit")
    fun observeRecentWorkouts(limit: Int): Flow<List<WorkoutHistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC")
    suspend fun getAllWorkoutsWithDetails(): List<WorkoutHistoryWithDetails>

    /** Parent LIMIT applies before Room loads relations: never load the full phone journal for Wear. */
    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY startedEpochMs DESC, id DESC LIMIT 8")
    suspend fun getWearMiniJournal(): List<WorkoutHistoryWithDetails>

    /** List-only projection: no raw JSON, GPS coordinates, photos or set relations. */
    @Query("SELECT id, startedEpochMs, programName, dayName, durationSeconds, completedSetsCount, totalSetsCount, hasPr226 FROM workout_history ORDER BY startedEpochMs DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getJournalSummaryPage(limit: Int, offset: Int): List<JournalWorkoutSummary>

    @Query("""
        SELECT w.id, w.startedEpochMs, w.programName, w.dayName, w.durationSeconds,
            w.completedSetsCount, w.totalSetsCount, w.hasPr226,
            w.programName || ' ' || w.dayName || ' ' || COALESCE((
                SELECT group_concat(e.nameHu || ' ' || e.nameEn || ' ' ||
                    COALESCE(l.nameDe, '') || ' ' || COALESCE(l.nameRo, ''), ' ')
                FROM workout_history_exercises e LEFT JOIN exercise_library l ON l.id = e.exerciseId
                WHERE e.workoutHistoryId = w.id), '') AS searchText
        FROM workout_history w
        WHERE w.startedEpochMs >= :fromEpoch AND w.startedEpochMs < :toEpoch
            AND (:beforeEpoch IS NULL OR w.startedEpochMs < :beforeEpoch
                OR (w.startedEpochMs = :beforeEpoch AND w.id < :beforeId))
        ORDER BY w.startedEpochMs DESC, w.id DESC LIMIT :limit
    """)
    suspend fun scanJournal(fromEpoch: Long, toEpoch: Long, beforeEpoch: Long?, beforeId: String,
        limit: Int): List<JournalSearchRow>

    @Query("""
        SELECT id, startedEpochMs, programName, dayName, durationSeconds, completedSetsCount, totalSetsCount, hasPr226
        FROM workout_history WHERE startedEpochMs >= :fromEpoch AND startedEpochMs < :toEpoch
            AND (:beforeEpoch IS NULL OR startedEpochMs < :beforeEpoch OR (startedEpochMs = :beforeEpoch AND id < :beforeId))
        ORDER BY startedEpochMs DESC, id DESC LIMIT :limit
    """)
    suspend fun scanJournalSummaries(fromEpoch: Long, toEpoch: Long, beforeEpoch: Long?, beforeId: String,
        limit: Int): List<JournalWorkoutSummary>

    @Transaction
    @Query("SELECT * FROM workout_history WHERE id = :id")
    fun observeWorkoutDetails(id: String): Flow<WorkoutHistoryWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_history WHERE id = :id OR workoutKey = :id OR syncId = :id LIMIT 1")
    suspend fun getWorkoutWithDetails(id: String): WorkoutHistoryWithDetails?

    @Query("SELECT COUNT(*) FROM workout_history")
    suspend fun getWorkoutCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutHistoryExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutHistorySetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<WorkoutPhotoEntity>)

    @Query("DELETE FROM workout_history_exercises WHERE workoutHistoryId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: String)

    @Query("DELETE FROM workout_history_sets WHERE workoutHistoryId = :workoutId")
    suspend fun deleteSetsForWorkout(workoutId: String)

    @Query("DELETE FROM workout_photos WHERE workoutHistoryId = :workoutId")
    suspend fun deletePhotosForWorkout(workoutId: String)

    @Query("DELETE FROM workout_history WHERE id = :workoutId OR workoutKey = :workoutId")
    suspend fun deleteWorkoutByIdOrKey(workoutId: String)

    @Query("DELETE FROM workout_history")
    suspend fun deleteAllWorkouts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstone(tombstone: HistoryTombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstones(tombstones: List<HistoryTombstoneEntity>)

    @Query("SELECT * FROM history_tombstones ORDER BY deletedAt DESC LIMIT 2000")
    suspend fun getTombstones(): List<HistoryTombstoneEntity>

    @Query("SELECT * FROM history_tombstones ORDER BY deletedAt DESC")
    suspend fun getAllTombstones(): List<HistoryTombstoneEntity>

    @Query("DELETE FROM history_tombstones")
    suspend fun deleteAllTombstones()

    @Transaction
    suspend fun saveFullWorkout(
        workout: WorkoutHistoryEntity,
        exercisesWithSets: List<Pair<WorkoutHistoryExerciseEntity, List<WorkoutHistorySetEntity>>>,
        photos: List<WorkoutPhotoEntity>
    ) {
        insertWorkout(workout)
        deleteSetsForWorkout(workout.id)
        deleteExercisesForWorkout(workout.id)
        deletePhotosForWorkout(workout.id)
        for ((exercise, sets) in exercisesWithSets) {
            val rowId = insertExercise(exercise.copy(workoutHistoryId = workout.id))
            val linkedSets = sets.map { it.copy(workoutHistoryId = workout.id, historyExerciseRowId = rowId) }
            insertSets(linkedSets)
        }
        if (photos.isNotEmpty()) {
            insertPhotos(photos.map { it.copy(workoutHistoryId = workout.id) })
        }
    }
}

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weights ORDER BY date ASC")
    fun observeAllWeights(): Flow<List<BodyWeightEntryEntity>>

    @Query("SELECT * FROM body_weights ORDER BY date ASC")
    suspend fun getAllWeights(): List<BodyWeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(entry: BodyWeightEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeights(entries: List<BodyWeightEntryEntity>)

    @Query("DELETE FROM body_weights WHERE date = :date OR dayKey = :date OR id = :date")
    suspend fun deleteByDateOrId(date: String)

    @Query("DELETE FROM body_weights")
    suspend fun deleteAllWeights()
}

@Dao
interface CalendarScheduleDao {
    @Query("SELECT * FROM calendar_entries WHERE id = :scheduleId LIMIT 1")
    suspend fun getEntry(scheduleId: String): CalendarScheduleEntity?

    @Query("SELECT * FROM calendar_entries WHERE date >= :fromDate AND date <= :toDate ORDER BY startIso, id")
    fun observeEntriesInRange(fromDate: String, toDate: String): Flow<List<CalendarScheduleEntity>>

    @Query("SELECT * FROM calendar_entries WHERE julianday(startIso) >= julianday(:fromIso) AND status = 'planned' AND NOT EXISTS(SELECT 1 FROM workout_history h WHERE h.scheduleId = calendar_entries.id) ORDER BY julianday(startIso), id LIMIT :limit OFFSET :offset")
    suspend fun getCoachUpcoming(fromIso: String, limit: Int, offset: Int): List<CalendarScheduleEntity>

    @Query("SELECT * FROM calendar_entries WHERE programId = :programId AND julianday(startIso) IS NOT NULL ORDER BY julianday(startIso) DESC, id LIMIT :limit OFFSET :offset")
    suspend fun getCoachCyclePlans(programId: String, limit: Int, offset: Int): List<CalendarScheduleEntity>

    @Query("SELECT * FROM calendar_entries ORDER BY startIso ASC")
    fun observeAllEntries(): Flow<List<CalendarScheduleEntity>>

    @Query("SELECT * FROM calendar_entries ORDER BY startIso ASC")
    suspend fun getAllEntries(): List<CalendarScheduleEntity>

    @Query("SELECT * FROM calendar_entries WHERE date >= :fromDate AND date <= :toDate ORDER BY startIso ASC")
    suspend fun getEntriesInRange(fromDate: String, toDate: String): List<CalendarScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: CalendarScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<CalendarScheduleEntity>)

    @Query("DELETE FROM calendar_entries WHERE id = :scheduleId")
    suspend fun deleteEntry(scheduleId: String)

    @Query("DELETE FROM calendar_entries")
    suspend fun deleteAllEntries()
}

@Dao
interface KeyValueDocumentDao {
    @Query("SELECT * FROM kv_documents WHERE doc_key = :key LIMIT 1")
    fun observeDocument(key: String): Flow<KeyValueDocumentEntity?>

    @Query("SELECT * FROM kv_documents WHERE doc_key = :key LIMIT 1")
    suspend fun getDocument(key: String): KeyValueDocumentEntity?

    @Query("SELECT * FROM kv_documents")
    suspend fun getAllDocuments(): List<KeyValueDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDocument(doc: KeyValueDocumentEntity)

    @Query("DELETE FROM kv_documents WHERE doc_key = :key")
    suspend fun deleteDocument(key: String)

    @Query("DELETE FROM kv_documents")
    suspend fun deleteAllDocuments()
}
