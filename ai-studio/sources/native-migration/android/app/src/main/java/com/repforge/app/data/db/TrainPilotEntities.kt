package com.repforge.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "programs",
    indices = [Index(value = ["sortOrder"])]
)
data class ProgramEntity(
    @PrimaryKey val id: String,
    val nameHu: String,
    val nameEn: String,
    val nameDe: String,
    val nameRo: String,
    val location: String,
    val level: String,
    val description: String,
    val builtin: Boolean,
    val generated: Boolean,
    val sourceType: String,
    val sourceTemplateId: String?,
    val templateVersion: Int,
    val accentHex: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val rawJson: String
)

@Entity(
    tableName = "program_days",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["programId", "dayId"], unique = true),
        Index(value = ["programId", "sortOrder"])
    ]
)
data class ProgramDayEntity(
    @PrimaryKey val compositeId: String, // "$programId:$dayId"
    val programId: String,
    val dayId: String,
    val nameHu: String,
    val nameEn: String,
    val nameDe: String,
    val nameRo: String,
    val sortOrder: Int,
    val exercisesJson: String // JSONArray of exercise prescriptions / IDs
)

@Entity(
    tableName = "exercise_library",
    indices = [
        Index(value = ["primaryMuscle"]),
        Index(value = ["custom"])
    ]
)
data class ExerciseLibraryEntity(
    @PrimaryKey val id: String,
    val nameHu: String,
    val nameEn: String,
    val nameDe: String,
    val nameRo: String,
    val loadType: String, // "weight", "dumbbell", "barbell", "bodyweight", "total", "machine", "cable"
    val repUnit: String, // "ism.", "mp", "mp/oldal", "km", "m", "lépés"
    val measurementType: String, // "reps", "time", "distance", "steps", "strength"
    val defaultSets: Int,
    val defaultReps: String,
    val defaultWeight: Double,
    val defaultRestSeconds: Int,
    val primaryMuscle: String, // "chest", "back", "shoulders", "arms", "core", "legs", "cardio"
    val muscleGroupsJson: String, // JSONArray of muscle group keys
    val notesHu: String,
    val notesEn: String,
    val notesDe: String,
    val notesRo: String,
    val videoUrl: String?,
    val videoProvider: String?,
    val custom: Boolean,
    val setRir229: Boolean,
    val rawJson: String
)

@Entity(
    tableName = "workout_history",
    indices = [
        Index(value = ["started"]),
        Index(value = ["finished"]),
        Index(value = ["startedEpochMs"]),
        Index(value = ["scheduleId"])
    ]
)
data class WorkoutHistoryEntity(
    @PrimaryKey val id: String, // Canonical ID or tp18-h-...
    val workoutKey: String, // rf142WorkoutKey equivalent
    val syncId: String,
    val started: String, // ISO-8601
    val finished: String, // ISO-8601
    val startedEpochMs: Long,
    val finishedEpochMs: Long,
    val durationSeconds: Int,
    val programId: String,
    val programName: String,
    val dayId: String, // also mapped from legacy "workout" field
    val dayName: String,
    val scheduleId: String,
    val quickWorkout: Boolean,
    val originDevice: String, // "phone" | "watch"
    val ownershipProtocol193: Int,
    val ownerEpoch193: Long,
    val effortFeedback: String?, // "easy", "light", "good", "challenging", "hard", "pain", or null
    val feedbackSkipped: Boolean,
    val feedbackUpdatedAt: Long,
    val totalVolumeKg: Double,
    val completedSetsCount: Int,
    val totalSetsCount: Int,
    val totalRepsCount: Int,
    val hasPr226: Boolean,
    val prRecordsJson226: String, // JSONArray of #226 PR badges
    val wearMetricsJson: String?, // Health Services summary
    val health240Json: String?, // Health Connect / Samsung Health matched workout summary
    val timeLogs162Json: String?, // IntervalTimeLedger per-set workMs/restMs/pausedMs
    val activeIntervalsJson: String?, // JSONArray of {start, end}
    val editedAt: String?,
    val rawJson: String // Full original JSON object for 100% lossless round-trip backup/sync
)

@Entity(
    tableName = "workout_history_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutHistoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutHistoryId", "orderIndex"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutHistoryExerciseEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val workoutHistoryId: String,
    val exerciseId: String,
    val nameHu: String,
    val nameEn: String,
    val loadType: String,
    val repUnit: String,
    val measurementType: String,
    val orderIndex: Int,
    val actualRir: Int?,
    val setRir229: Boolean
)

@Entity(
    tableName = "workout_history_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutHistoryExerciseEntity::class,
            parentColumns = ["rowId"],
            childColumns = ["historyExerciseRowId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutHistoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["historyExerciseRowId", "setNumber"]),
        Index(value = ["workoutHistoryId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutHistorySetEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    val setId: String,
    val workoutHistoryId: String,
    val historyExerciseRowId: Long,
    val exerciseId: String,
    val setNumber: Int,
    val weight: Double?,
    val reps: String,
    val leftSeconds: Int?,
    val rightSeconds: Int?,
    val distanceMeters: Double?,
    val done: Boolean,
    val actualRir: Int?,
    val gpsRouteKey163: String?,
    val gpsRouteKey193: String?
)

@Entity(
    tableName = "workout_photos",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutHistoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutHistoryId"]),
        Index(value = ["id"], unique = true)
    ]
)
data class WorkoutPhotoEntity(
    @PrimaryKey val id: String, // UUID [a-f0-9-]{36}
    val workoutHistoryId: String,
    val label: String,
    val createdAt: String,
    val updatedAt: Long,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val driveFileId: String?,
    val deletedAt: String?
)

@Entity(
    tableName = "body_weights",
    indices = [
        Index(value = ["date"]),
        Index(value = ["dayKey"])
    ]
)
data class BodyWeightEntryEntity(
    @PrimaryKey val id: String, // Canonical ID or tp18-w-...
    val date: String, // ISO-8601 or YYYY-MM-DD
    val dayKey: String, // YYYY-MM-DD
    val kg: Double,
    val updatedAt: Long,
    val rawJson: String
)

/** Scalar full-history statistics; never hydrated from the currently loaded UI page. */
data class WeightJournalSummary(val measurementCount: Long, val firstKg: Double?, val currentKg: Double?,
    val minimumKg: Double?, val maximumKg: Double?)

data class WeightChartBounds(val firstDay: Double?, val lastDay: Double?, val measurementCount: Long)

data class WeightDeletionCandidate(val id: String, val date: String, val kg: Double)

/** A time bucket is explicitly displayed as an average, not an invented individual measurement. */
data class WeightChartPoint(val bucket: Int, val averageKg: Double, val minimumKg: Double,
    val maximumKg: Double, val measurementCount: Long)

@Entity(
    tableName = "calendar_entries",
    indices = [
        Index(value = ["date"]),
        Index(value = ["startIso"]),
        Index(value = ["status"])
    ]
)
data class CalendarScheduleEntity(
    @PrimaryKey val id: String, // scheduleId
    val googleEventId: String, // rf[0-9a-f]{64}
    val date: String, // YYYY-MM-DD
    val startIso: String,
    val endIso: String,
    val programId: String,
    val programName: String,
    val dayId: String,
    val dayName: String,
    val status: String, // "planned", "completed", "skipped", "rest"
    val updatedAt: Long,
    val rawJson: String
)

@Entity(
    tableName = "history_tombstones",
    indices = [Index(value = ["workoutKey"])]
)
data class HistoryTombstoneEntity(
    @PrimaryKey val primaryId: String,
    val workoutKey: String,
    val idsJson: String,
    val deletedAt: Long
)

@Entity(tableName = "kv_documents")
data class KeyValueDocumentEntity(
    @PrimaryKey @ColumnInfo(name = "doc_key") val key: String,
    @ColumnInfo(name = "doc_json") val json: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

data class WorkoutHistoryExerciseWithSets(
    @Embedded val exercise: WorkoutHistoryExerciseEntity,
    @Relation(
        parentColumn = "rowId",
        entityColumn = "historyExerciseRowId"
    )
    val sets: List<WorkoutHistorySetEntity>
)

data class WorkoutHistoryWithDetails(
    @Embedded val workout: WorkoutHistoryEntity,
    @Relation(
        entity = WorkoutHistoryExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutHistoryId"
    )
    val exercises: List<WorkoutHistoryExerciseWithSets>,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutHistoryId"
    )
    val photos: List<WorkoutPhotoEntity>
)

data class ProgramWithDays(
    @Embedded val program: ProgramEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "programId"
    )
    val days: List<ProgramDayEntity>
)

/** Bounded calendar projection; never loads set/photo/route detail to show a date. */
data class CalendarHistorySummary(val id: String, val started: String, val programName: String,
    val dayName: String, val scheduleId: String)

/** Bounded journal list row; detail payloads are fetched only when requested by an editor/card. */
data class JournalWorkoutSummary(val id: String, val startedEpochMs: Long, val programName: String,
    val dayName: String, val durationSeconds: Int, val completedSetsCount: Int,
    val totalSetsCount: Int, val hasPr226: Boolean)

/** Transient search projection: small text only, no raw history/sets/photos/coordinates. */
data class JournalSearchRow(@Embedded val summary: JournalWorkoutSummary, val searchText: String)

/** Lightweight Progress aggregate; no historical raw JSON/photos or set relations. */
data class ProgressMuscleSummary(val muscleKey: String, val setsCount: Int)

/** Completed-set PR input only; no history JSON, photos, routes or Room relations. */
data class PersonalRecordSet226(val setRowId: Long, val exerciseId: String, val repUnit: String,
    val weight: Double?, val reps: String, val leftSeconds: Int?, val rightSeconds: Int?,
    val distanceMeters: Double?)

/** Keyset-paged completed strength sets for a single exercise, independent of journal size. */
data class ProgressSetSummary(val workoutId: String, val started: String,
    val startedEpochMs: Long, val setRowId: Long, val weightKg: Double, val reps: String)


/**
 * Scalar projection for the cardio feed: no history relations, photos, GPS coordinates,
 * or duplicated parent raw JSON. The adapter resolves optional set source metadata separately.
 * The DAO's keyset is (finishedEpochMs, workoutId, setRowId) in descending order.
 */
data class CardioSetProjection(
    val workoutId: String,
    val startedEpochMs: Long,
    val finishedEpochMs: Long,
    val startedIso: String,
    val finishedIso: String,
    val setId: String,
    val setRowId: Long,
    val setNumber: Int,
    val exerciseOrderIndex: Int,
    val exerciseId: String,
    val exerciseNameHu: String,
    val exerciseNameEn: String,
    val exerciseNameDe: String,
    val exerciseNameRo: String,
    val measurementType: String,
    val libraryMeasurementType: String?,
    val isKnownLibraryExercise: Boolean,
    val repUnit: String,
    val done: Boolean,
    val distanceMeters: Double?,
    val reps: String,
    val leftSeconds: Int?,
    val rightSeconds: Int?,
    val gpsRouteKey163: String?,
    val gpsRouteKey193: String?,
    val exerciseHasDistance: Boolean,
    val exerciseHasPositiveDistance: Boolean
)
