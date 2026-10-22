package com.repforge.app.data.db

import android.content.Context
import androidx.room.*
import org.json.JSONArray
import org.json.JSONObject

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
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        suspend fun ensureBuiltinCatalogSeeded(db: TrainPilotDatabase) {
            val existingExercises = db.exerciseLibraryDao().getAllExercises()
            if (existingExercises.none { !it.custom }) {
                db.exerciseLibraryDao().upsertExercises(defaultExercises())
            }
            val existingPrograms = db.programDao().getProgramsWithDays()
            if (existingPrograms.none { it.program.builtin }) {
                val (programs, days) = defaultPrograms()
                db.programDao().upsertPrograms(programs)
                db.programDao().upsertProgramDays(days)
            }
        }

        fun defaultExercises(): List<ExerciseLibraryEntity> {
            data class Seed(
                val id: String, val hu: String, val en: String, val de: String, val ro: String,
                val loadType: String, val repUnit: String, val measurementType: String,
                val sets: Int, val reps: String, val weight: Double, val rest: Int,
                val primary: String, val muscles: List<String>, val video: String? = null
            )
            val seeds = listOf(
                Seed("goblet-squat", "Goblet guggolás", "Goblet Squat", "Goblet Kniebeuge", "Genuflexiuni Goblet", "dumbbell", "ism.", "reps", 3, "10", 12.0, 90, "legs", listOf("legs", "core")),
                Seed("db-floor-press", "Fekvenyomás talajon kézisúlyzóval", "Dumbbell Floor Press", "Kurzhantel-Bodenpressen", "Împins de pe podea cu gantere", "dumbbell", "ism.", "reps", 3, "10", 10.0, 90, "chest", listOf("chest", "arms", "shoulders")),
                Seed("one-arm-db-row", "Egykezes döntött törzsű evezés", "One-Arm Dumbbell Row", "Einarmiges Kurzhantelrudern", "Ramat cu gantera pe o mână", "dumbbell", "ism.", "reps", 3, "10", 12.0, 90, "back", listOf("back", "arms")),
                Seed("db-romanian-deadlift", "Román felhúzás kézisúlyzóval", "Dumbbell Romanian Deadlift", "Rumänisches Kreuzheben mit Kurzhanteln", "Îndreptări românești cu gantere", "dumbbell", "ism.", "reps", 3, "10", 12.0, 90, "legs", listOf("legs", "back")),
                Seed("db-shoulder-press", "Vállból nyomás kézisúlyzóval", "Dumbbell Shoulder Press", "Kurzhantel-Schulterdrücken", "Împins deasupra capului cu gantere", "dumbbell", "ism.", "reps", 3, "10", 8.0, 90, "shoulders", listOf("shoulders", "arms")),
                Seed("plank", "Plank tartás", "Plank Hold", "Unterarmstütz (Plank)", "Plank", "bodyweight", "mp", "time", 3, "45", 0.0, 60, "core", listOf("core")),
                Seed("side-plank", "Oldalsó plank", "Side Plank", "Seitstütz", "Plank lateral", "bodyweight", "mp/oldal", "time", 3, "30", 0.0, 60, "core", listOf("core")),
                Seed("push-up", "Fekvőtámasz", "Push-Up", "Liegestütz", "Flotări", "bodyweight", "ism.", "reps", 3, "12", 0.0, 75, "chest", listOf("chest", "arms", "shoulders", "core")),
                Seed("split-squat", "Bolgár / kitöréses guggolás", "Split Squat", "Ausfallschritt-Kniebeuge", "Fandări statice", "dumbbell", "ism.", "reps", 3, "10", 8.0, 90, "legs", listOf("legs", "core")),
                Seed("db-bicep-curl", "Bicepsz karhajlítás kézisúlyzóval", "Dumbbell Bicep Curl", "Kurzhantel-Bizepscurls", "Flexii biceps cu gantere", "dumbbell", "ism.", "reps", 3, "12", 8.0, 60, "arms", listOf("arms")),
                Seed("db-tricep-extension", "Tricepsz nyújtás fej felett", "Overhead Tricep Extension", "Trizepsstrecken über Kopf", "Extensii triceps deasupra capului", "dumbbell", "ism.", "reps", 3, "12", 8.0, 60, "arms", listOf("arms")),
                Seed("glute-bridge", "Csípőemelés talajon", "Glute Bridge", "Beckenheben", "Podul fesier", "bodyweight", "ism.", "reps", 3, "15", 0.0, 60, "legs", listOf("legs", "core")),
                Seed("running", "Futás (szabadtéri)", "Outdoor Running", "Laufen im Freien", "Alergare în aer liber", "bodyweight", "km", "distance", 1, "5", 0.0, 60, "cardio", listOf("legs", "core")),
                Seed("walking", "Gyaloglás / Séta", "Walking", "Gehen / Spazieren", "Mers pe jos", "bodyweight", "km", "distance", 1, "3", 0.0, 60, "cardio", listOf("legs")),
                Seed("cycling", "Kerékpározás", "Cycling", "Radfahren", "Ciclism", "bodyweight", "km", "distance", 1, "10", 0.0, 60, "cardio", listOf("legs")),
                Seed("hiking", "Túrázás", "Hiking", "Wandern", "Drumeție", "bodyweight", "km", "distance", 1, "5", 0.0, 60, "cardio", listOf("legs", "core"))
            )
            return seeds.map { s ->
                val raw = JSONObject().put("id", s.id).put("hu", s.hu).put("en", s.en).put("de", s.de).put("ro", s.ro).put("loadType", s.loadType).put("repUnit", s.repUnit).put("measurementType", s.measurementType).put("sets", s.sets).put("reps", s.reps).put("weight", s.weight).put("restSeconds", s.rest).put("primaryMuscle", s.primary).put("muscles", JSONArray(s.muscles)).put("custom", false)
                ExerciseLibraryEntity(s.id, s.hu, s.en, s.de, s.ro, s.loadType, s.repUnit, s.measurementType, s.sets, s.reps, s.weight, s.rest, s.primary, JSONArray(s.muscles).toString(), "", "", "", "", s.video, null, false, false, raw.toString())
            }
        }

        fun defaultPrograms(): Pair<List<ProgramEntity>, List<ProgramDayEntity>> {
            val dayA = JSONArray().put("goblet-squat").put("db-floor-press").put("one-arm-db-row").put("plank")
            val dayB = JSONArray().put("db-romanian-deadlift").put("db-shoulder-press").put("split-squat").put("side-plank")
            val raw = JSONObject().put("id", "home-basic").put("name", "Otthoni Alap Program").put("builtin", true).put("templateVersion", 2).put("days", JSONArray().put(JSONObject().put("id", "A").put("name", "Alap A").put("exercises", dayA)).put(JSONObject().put("id", "B").put("name", "Alap B").put("exercises", dayB)))
            val prog = ProgramEntity("home-basic", "Otthoni Alap Program", "Home Basic Program", "Heim-Basisprogramm", "Program de bază acasă", "Otthon", "Kezdő / Középhaladó", "Teljes testes otthoni edzésterv A/B napokkal.", true, false, "template", "home-basic", 2, "#4d8fbe", 0, System.currentTimeMillis(), raw.toString())
            val days = listOf(
                ProgramDayEntity("home-basic:A", "home-basic", "A", "Alap A", "Basic A", "Basis A", "Bază A", 0, dayA.toString()),
                ProgramDayEntity("home-basic:B", "home-basic", "B", "Alap B", "Basic B", "Basis B", "Bază B", 1, dayB.toString())
            )
            return listOf(prog) to days
        }
    }
}
