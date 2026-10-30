package com.repforge.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        ProgramEntity::class,
        ProgramDayEntity::class,
        ExerciseLibraryEntity::class,
        WorkoutHistoryEntity::class,
        WorkoutHistoryExerciseEntity::class,
        WorkoutHistorySetEntity::class,
        WorkoutPhotoEntity::class,
        BodyWeightEntryEntity::class,
        CalendarScheduleEntity::class,
        HistoryTombstoneEntity::class,
        KeyValueDocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrainPilotDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun calendarScheduleDao(): CalendarScheduleDao
    abstract fun keyValueDocumentDao(): KeyValueDocumentDao

    companion object {
        const val DATABASE_NAME = "trainpilot_native.db"

        @Volatile
        private var INSTANCE: TrainPilotDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): TrainPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrainPilotDatabase::class.java,
                    DATABASE_NAME
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun ensureBuiltinCatalogSeeded(db: TrainPilotDatabase, context: Context) {
            // Missing-only seeding preserves imported/customized rows, including old native IDs.
            com.repforge.app.NativeDataReplacement.mutate(context) {
                db.withTransaction {
                    val knownExercises = db.exerciseLibraryDao().getExerciseIds().toSet()
                    db.exerciseLibraryDao().insertMissingExercises(defaultExercises().filter { it.id !in knownExercises })
                    val knownPrograms = db.programDao().getProgramIds().toSet()
                    val (programs, days) = defaultPrograms()
                    for (program in programs.filter { it.id !in knownPrograms }) {
                        if (db.programDao().insertMissingProgram(program) != -1L)
                            db.programDao().upsertProgramDays(days.filter { it.programId == program.id })
                    }
                }
            }
        }

        fun defaultExercises(): List<ExerciseLibraryEntity> = NativeBuiltinCatalog.exercises
        fun defaultPrograms(): Pair<List<ProgramEntity>, List<ProgramDayEntity>> = NativeBuiltinCatalog.programs
    }
}
