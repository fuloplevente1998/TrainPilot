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
    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC")
    fun observeAllWorkoutsWithDetails(): Flow<List<WorkoutHistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC LIMIT :limit")
    fun observeRecentWorkouts(limit: Int): Flow<List<WorkoutHistoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_history ORDER BY started DESC")
    suspend fun getAllWorkoutsWithDetails(): List<WorkoutHistoryWithDetails>

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
