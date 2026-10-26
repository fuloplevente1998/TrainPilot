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

    // Flat reads for the Home snapshot: Room's @Relation cursor mapping can replay
    // its parent SELECT. Two flat lists avoid that second catalog read entirely.
    @Query("SELECT * FROM programs ORDER BY sortOrder ASC, id ASC")
    suspend fun getProgramRowsForSnapshot(): List<ProgramEntity>

    @Query("SELECT * FROM program_days ORDER BY programId ASC, sortOrder ASC, dayId ASC")
    suspend fun getProgramDaysForSnapshot(): List<ProgramDayEntity>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :programId LIMIT 1")
    suspend fun getProgramWithDaysById(programId: String): ProgramWithDays?

    @Query("SELECT id FROM programs")
    suspend fun getProgramIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM workout_history WHERE programId = :programId)")
    suspend fun hasWorkoutHistory(programId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_entries WHERE programId = :programId)")
    suspend fun hasScheduledEntries(programId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM workout_history WHERE programId = :programId AND dayId = :dayId)")
    suspend fun hasDayHistory(programId: String, dayId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_entries WHERE programId = :programId AND dayId = :dayId)")
    suspend fun hasDaySchedule(programId: String, dayId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingProgram(program: ProgramEntity): Long

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

    @Query("SELECT id FROM exercise_library")
    suspend fun getExerciseIds(): List<String>

    /** Guard custom exercise removal without loading all old workout details. */
    @Query("SELECT EXISTS(SELECT 1 FROM workout_history_exercises WHERE exerciseId = :id)")
    suspend fun hasWorkoutHistoryReference(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingExercises(exercises: List<ExerciseLibraryEntity>): List<Long>

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
        SELECT w.* FROM workout_history w
        WHERE w.programId = :programId AND w.dayId = :dayId
          AND w.finishedEpochMs >= :fromEpoch AND w.finishedEpochMs <= :toEpoch
          AND (:beforeEpoch IS NULL OR w.finishedEpochMs < :beforeEpoch OR
            (w.finishedEpochMs = :beforeEpoch AND w.id < :beforeId))
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY w.finishedEpochMs DESC, w.id DESC LIMIT :limit
    """)
    suspend fun getProfileDayCandidates(programId: String, dayId: String, fromEpoch: Long, toEpoch: Long,
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

    @Query("""
        SELECT s.rowId AS setRowId, e.exerciseId, e.repUnit, s.weight, s.reps,
          s.leftSeconds, s.rightSeconds, s.distanceMeters
        FROM workout_history_sets s
        INNER JOIN workout_history_exercises e ON e.rowId = s.historyExerciseRowId
        INNER JOIN workout_history w ON w.id = e.workoutHistoryId
        WHERE e.exerciseId IN (:exerciseIds) AND s.done = 1 AND s.rowId > :afterRowId
          AND w.id != :excludedId AND w.started != :excludedStarted
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY s.rowId ASC LIMIT :limit
    """)
    suspend fun scanPersonalRecordSets(exerciseIds: List<String>, excludedId: String,
        excludedStarted: String, afterRowId: Long, limit: Int): List<PersonalRecordSet226>

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
    @Query("SELECT * FROM workout_history WHERE id = :id")
    suspend fun getWorkoutWithDetailsById(id: String): WorkoutHistoryWithDetails?

    @Transaction
    @Query("SELECT * FROM workout_history WHERE id = :id OR workoutKey = :id OR syncId = :id LIMIT 1")
    suspend fun getWorkoutWithDetails(id: String): WorkoutHistoryWithDetails?

    /** Historical strength chart reads a single exercise in bounded pages; it never loads photos, routes or other sets. */
    @Query("""
        SELECT w.id AS workoutId, w.started AS started, w.startedEpochMs AS startedEpochMs,
            s.rowId AS setRowId, s.weight AS weightKg, s.reps AS reps
        FROM workout_history_sets s
        JOIN workout_history w ON w.id = s.workoutHistoryId
        WHERE s.exerciseId = :exerciseId AND s.done = 1 AND s.weight > 0
          AND w.finishedEpochMs > w.startedEpochMs
          AND (:beforeEpoch IS NULL OR w.startedEpochMs < :beforeEpoch
            OR (w.startedEpochMs = :beforeEpoch AND w.id < :beforeWorkoutId)
            OR (w.startedEpochMs = :beforeEpoch AND w.id = :beforeWorkoutId AND s.rowId < :beforeSetRowId))
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        ORDER BY w.startedEpochMs DESC, w.id DESC, s.rowId DESC
        LIMIT :limit
    """)
    suspend fun pageProgressExerciseSets(exerciseId: String, beforeEpoch: Long?, beforeWorkoutId: String,
        beforeSetRowId: Long, limit: Int): List<ProgressSetSummary>

    /** Completed sets for the last 30 days, grouped in SQLite rather than hydrating whole workouts. */
    @Query("""
        SELECT COALESCE(l.primaryMuscle, 'full') AS muscleKey, COUNT(*) AS setsCount
        FROM workout_history_sets s
        JOIN workout_history w ON w.id = s.workoutHistoryId
        LEFT JOIN exercise_library l ON l.id = s.exerciseId
        WHERE s.done = 1 AND w.startedEpochMs >= :fromEpoch AND w.startedEpochMs <= :toEpoch
          AND w.finishedEpochMs > w.startedEpochMs
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey) OR t.workoutKey = w.workoutKey)
        GROUP BY COALESCE(l.primaryMuscle, 'full')
    """)
    suspend fun getProgressMuscleSummary(fromEpoch: Long, toEpoch: Long): List<ProgressMuscleSummary>


    /**
     * All completed cardio candidates, not merely the latest N workout summaries.
     * The physical cursor uses FINISHED time, identity and raw set row ID (not the
     * strength chart's started-time cursor). No relation/raw-JSON hydration.
     *
     * Sibling flags deliberately include undone sets in this same exercise instance,
     * even when those siblings live outside the page or are not themselves emitted.
     * The adapter must advance using the last *raw* row even if its model rejects it.
     */
    @Query("""
        SELECT w.id AS workoutId,
            w.startedEpochMs, w.finishedEpochMs,
            w.started AS startedIso, w.finished AS finishedIso,
            s.setId, s.rowId AS setRowId, s.setNumber,
            e.orderIndex AS exerciseOrderIndex, e.exerciseId,
            CASE WHEN TRIM(e.nameHu) <> '' AND e.nameHu <> e.exerciseId
                THEN e.nameHu ELSE COALESCE(NULLIF(l.nameHu, ''), e.nameHu) END AS exerciseNameHu,
            CASE WHEN TRIM(e.nameEn) <> '' AND e.nameEn <> e.exerciseId
                THEN e.nameEn ELSE COALESCE(NULLIF(l.nameEn, ''), e.nameEn) END AS exerciseNameEn,
            COALESCE(l.nameDe, '') AS exerciseNameDe,
            COALESCE(l.nameRo, '') AS exerciseNameRo,
            e.measurementType, l.measurementType AS libraryMeasurementType,
            (l.id IS NOT NULL) AS isKnownLibraryExercise,
            e.repUnit, s.done, s.distanceMeters, s.reps,
            s.leftSeconds, s.rightSeconds, s.gpsRouteKey163, s.gpsRouteKey193,
            EXISTS (
                SELECT 1 FROM workout_history_sets sibling
                WHERE sibling.historyExerciseRowId = e.rowId
                    AND sibling.distanceMeters >= 0
                    AND sibling.distanceMeters < 1.0e308
            ) AS exerciseHasDistance,
            EXISTS (
                SELECT 1 FROM workout_history_sets sibling
                WHERE sibling.historyExerciseRowId = e.rowId
                    AND sibling.distanceMeters > 0
                    AND sibling.distanceMeters < 1.0e308
            ) AS exerciseHasPositiveDistance
        FROM workout_history_sets s
        JOIN workout_history_exercises e ON e.rowId = s.historyExerciseRowId
        JOIN workout_history w ON w.id = e.workoutHistoryId
        LEFT JOIN exercise_library l ON l.id = e.exerciseId
        WHERE s.done = 1
            AND (:beforeFinished IS NULL OR w.finishedEpochMs < :beforeFinished
                OR (w.finishedEpochMs = :beforeFinished AND w.id < :beforeWorkoutId)
                OR (w.finishedEpochMs = :beforeFinished
                    AND w.id = :beforeWorkoutId AND s.rowId < :beforeSetRowId))
            AND NOT EXISTS (SELECT 1 FROM history_tombstones t
                WHERE (t.primaryId <> '' AND t.primaryId IN (w.id, w.syncId, w.workoutKey))
                    OR (t.workoutKey <> '' AND t.workoutKey = w.workoutKey))
        ORDER BY w.finishedEpochMs DESC, w.id DESC, s.rowId DESC
        LIMIT :limit
    """)
    suspend fun pageCardioSets(
        beforeFinished: Long?, beforeWorkoutId: String, beforeSetRowId: Long, limit: Int
    ): List<CardioSetProjection>

    /** Optional legacy per-set source lives only in parent raw JSON; read once per parent. */
    @Query("SELECT rawJson FROM workout_history WHERE id = :workoutId LIMIT 1")
    suspend fun getCardioWorkoutRawJson(workoutId: String): String?

    @Query("SELECT COUNT(*) FROM workout_history")
    suspend fun getWorkoutCount(): Int

    @Query("""SELECT COUNT(*) FROM workout_history w
        WHERE w.startedEpochMs >= :fromInclusive AND w.startedEpochMs < :toExclusive
          AND NOT EXISTS (SELECT 1 FROM history_tombstones t
            WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey)
               OR (t.workoutKey <> '' AND t.workoutKey = w.workoutKey))""")
    suspend fun getWorkoutCountBetween(fromInclusive: Long, toExclusive: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutHistoryExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutHistorySetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<WorkoutPhotoEntity>)

    @Query("""SELECT EXISTS(SELECT 1 FROM workout_photos
        WHERE (id = :photoId OR driveFileId = :driveId)
        AND (deletedAt IS NULL OR TRIM(deletedAt) IN ('', '0', '0.0', 'false')))""")
    suspend fun hasLiveCloudPhotoReference(photoId: String, driveId: String): Boolean

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
    @Query("SELECT id, date, kg FROM body_weights WHERE (:afterId IS NULL OR id > :afterId) ORDER BY id ASC LIMIT :limit")
    suspend fun scanWeightDeletionCandidates(afterId: String?, limit: Int): List<WeightDeletionCandidate>

    @Query("SELECT * FROM body_weights ORDER BY date DESC, id DESC")
    fun weightJournalPages(): androidx.paging.PagingSource<Int, BodyWeightEntryEntity>

    @Query("""
        SELECT COUNT(*) AS measurementCount, MIN(kg) AS minimumKg, MAX(kg) AS maximumKg,
          (SELECT kg FROM body_weights ORDER BY date ASC, id ASC LIMIT 1) AS firstKg,
          (SELECT kg FROM body_weights ORDER BY date DESC, id DESC LIMIT 1) AS currentKg
        FROM body_weights
    """)
    suspend fun getWeightJournalSummary(): WeightJournalSummary

    @Query("""
        SELECT MIN(julianday(dayKey)) AS firstDay, MAX(julianday(dayKey)) AS lastDay,
          COUNT(*) AS measurementCount FROM body_weights
        WHERE dayKey >= :fromDay AND julianday(dayKey) IS NOT NULL
    """)
    suspend fun getWeightChartBounds(fromDay: String): WeightChartBounds

    @Query("""
        SELECT CAST((julianday(dayKey) - :firstDay) / :daysPerBucket AS INTEGER) AS bucket,
          AVG(kg) AS averageKg, MIN(kg) AS minimumKg, MAX(kg) AS maximumKg,
          COUNT(*) AS measurementCount FROM body_weights
        WHERE dayKey >= :fromDay AND julianday(dayKey) IS NOT NULL
        GROUP BY bucket ORDER BY bucket ASC LIMIT 180
    """)
    suspend fun getWeightChart(fromDay: String, firstDay: Double, daysPerBucket: Int): List<WeightChartPoint>

    @Query("SELECT * FROM body_weights ORDER BY date ASC")
    suspend fun getAllWeights(): List<BodyWeightEntryEntity>

    @Query("SELECT * FROM body_weights WHERE id = :id LIMIT 1")
    suspend fun getWeightById(id: String): BodyWeightEntryEntity?

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteWeightById(id: String): Int

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
    @Query("SELECT * FROM kv_documents WHERE doc_key GLOB 'native:weightTombstone18:*' ORDER BY doc_key")
    suspend fun getWeightTombstoneDocuments(): List<KeyValueDocumentEntity>

    @Query("SELECT COUNT(*) FROM kv_documents WHERE doc_key GLOB 'native:weightTombstone18:*'")
    suspend fun getWeightTombstoneCount(): Int

    @Query("DELETE FROM kv_documents WHERE doc_key GLOB 'native:weightTombstone18:*'")
    suspend fun deleteWeightTombstoneDocuments()

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
