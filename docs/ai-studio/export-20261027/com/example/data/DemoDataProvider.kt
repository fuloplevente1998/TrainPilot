package com.example.data

import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.LoggedExerciseDetail
import com.example.model.LoggedWorkoutEntry
import com.example.model.PrRecordEntry
import com.example.model.ProgramDayModel
import com.example.model.TrainingProgramModel

object DemoDataProvider {

    val defaultExercises: List<ExerciseItem> = listOf(
        ExerciseItem(
            id = "goblet_squat",
            nameHu = "Goblet guggolás",
            nameEn = "Goblet Squat",
            nameDe = "Goblet Kniebeuge",
            nameRo = "Genuflexiuni Goblet",
            category = ExerciseCategory.LEGS,
            equipmentHu = "Kettlebell / Súlyzó",
            primaryMusclesHu = "Comb, Farizom, Core",
            secondaryMusclesHu = "Vádli, Törzs",
            defaultSets = 3,
            defaultReps = 10,
            defaultWeightKg = 16f,
            defaultRestSec = 90,
            stepsHu = listOf(
                "Fogd meg a kettlebellt a mellkasod előtt.",
                "Állj vállszélességben, a lábak enyhén kifelé.",
                "Végezz guggolást, a térdek a lábujjak irányába mozognak."
            ),
            prWeightKg = 16f,
            prReps = 12,
            coachStatusHu = "Normál terhelés",
            coachHintHu = "Kezdj könnyebben; ma ne erőltesd a progressziót."
        ),
        ExerciseItem(
            id = "bench_press",
            nameHu = "Fekvenyomás",
            nameEn = "Bench Press",
            nameDe = "Bankdrücken",
            nameRo = "Împins din culcat",
            category = ExerciseCategory.CHEST,
            equipmentHu = "Kétkezes rúd / Kézisúlyzó",
            primaryMusclesHu = "Mellizom, Elülső váll",
            secondaryMusclesHu = "Tricepsz",
            defaultSets = 4,
            defaultReps = 8,
            defaultWeightKg = 80f,
            defaultRestSec = 120,
            stepsHu = listOf(
                "Feküdj stabilan a padra, zárd a lapockákat.",
                "Engedd le a rudat kontrolláltan a mellkas alsó részéhez.",
                "Nyomd ki erőteljesen a kiinduló helyzetbe."
            ),
            prWeightKg = 100f,
            prReps = 8,
            coachStatusHu = "Kissé csökkentett terhelés",
            coachHintHu = "Kezdj könnyebben; ma ne erőltesd a progressziót."
        ),
        ExerciseItem(
            id = "push_up",
            nameHu = "Fekvőtámasz",
            nameEn = "Push-up",
            nameDe = "Liegestütze",
            nameRo = "Flotări",
            category = ExerciseCategory.CHEST,
            equipmentHu = "Saját testsúly",
            primaryMusclesHu = "Mellizom, Tricepsz",
            secondaryMusclesHu = "Elülső váll, Core",
            defaultSets = 3,
            defaultReps = 12,
            defaultWeightKg = 0f,
            defaultRestSec = 90,
            stepsHu = listOf(
                "Vedd fel a feszes fekvőtámasz pozíciót.",
                "Engedd le a mellkasod a talaj közelébe, könyök 45 fokban.",
                "Told vissza magad teljes karnyújtásig."
            ),
            prWeightKg = 0f,
            prReps = 15,
            coachStatusHu = "Normál terhelés",
            coachHintHu = "Stabil törzstartás minden ismétlésnél."
        ),
        ExerciseItem(
            id = "band_row",
            nameHu = "Evezés gumiszalaggal",
            nameEn = "Resistance Band Row",
            nameDe = "Rudern mit Widerstandsband",
            nameRo = "Ramat cu bandă elastică",
            category = ExerciseCategory.BACK,
            equipmentHu = "Gumiszalag (közepes)",
            primaryMusclesHu = "Széles hátizom, Romboideusz",
            secondaryMusclesHu = "Bicepsz, Hátsó váll",
            defaultSets = 3,
            defaultReps = 12,
            defaultWeightKg = 12f,
            defaultRestSec = 60,
            stepsHu = listOf(
                "Rögzítsd a gumiszalagot stabil ponthoz vagy talpad alá.",
                "Húzd a könyököd a törzs mellett hátra, zárd a lapockát.",
                "Lassan engedd vissza a kiinduló helyzetbe."
            ),
            prWeightKg = 14f,
            prReps = 12,
            coachStatusHu = "Könnyebb kezdés",
            coachHintHu = "Fókuszálj a lapockazárásra."
        ),
        ExerciseItem(
            id = "barbell_row",
            nameHu = "Döntött törzsű evezés – kétkezes rúddal",
            nameEn = "Barbell Bent-Over Row",
            nameDe = "Langhantelrudern vorgebeugt",
            nameRo = "Ramat din aplecat cu haltera",
            category = ExerciseCategory.BACK,
            equipmentHu = "Kétkezes rúd",
            primaryMusclesHu = "Hátizom, Csuklyás izom",
            secondaryMusclesHu = "Bicepsz, Alsó hát",
            defaultSets = 4,
            defaultReps = 10,
            defaultWeightKg = 60f,
            defaultRestSec = 90,
            stepsHu = listOf(
                "Dőlj előre 45 fokban egyenes háttal.",
                "Húzd a rudat az alhas irányába.",
                "Kontrolláltan engedd vissza."
            ),
            prWeightKg = 65f,
            prReps = 10
        ),
        ExerciseItem(
            id = "plank",
            nameHu = "Plank",
            nameEn = "Plank Hold",
            nameDe = "Unterarmstütz (Plank)",
            nameRo = "Plank izometric",
            category = ExerciseCategory.CORE,
            equipmentHu = "Saját testsúly",
            primaryMusclesHu = "Egyenes és haránt hasizom",
            secondaryMusclesHu = "Farizom, Vállöv",
            defaultSets = 3,
            defaultReps = 45,
            defaultWeightKg = 0f,
            defaultRestSec = 60,
            isTimed = true,
            targetSeconds = 45,
            stepsHu = listOf(
                "Támaszkodj az alkarodon, könyök a váll alatt.",
                "Feszítsd meg a has- és farizmokat, a test egyenes vonalban.",
                "Tartsd a pozíciót egyenletes légzéssel."
            )
        ),
        ExerciseItem(
            id = "side_plank",
            nameHu = "Oldalsó plank",
            nameEn = "Side Plank",
            nameDe = "Seitstütz",
            nameRo = "Plank lateral",
            category = ExerciseCategory.CORE,
            equipmentHu = "Saját testsúly",
            primaryMusclesHu = "Ferde hasizom, Core",
            secondaryMusclesHu = "Csípőstabilizálók",
            defaultSets = 3,
            defaultReps = 30,
            defaultWeightKg = 0f,
            defaultRestSec = 45,
            isTimed = true,
            isBilateral = true,
            targetSeconds = 30,
            stepsHu = listOf(
                "Támaszkodj az egyik alkarodon oldalfekvésben.",
                "Emeld meg a csípőd, hogy a törzs egyenes legyen.",
                "Ismételd meg mindkét oldalra."
            )
        ),
        ExerciseItem(
            id = "pull_up",
            nameHu = "Húzódzkodás",
            nameEn = "Pull-up",
            nameDe = "Klimmzüge",
            nameRo = "Tracțiuni",
            category = ExerciseCategory.BACK,
            equipmentHu = "Saját testsúly",
            primaryMusclesHu = "Széles hátizom",
            secondaryMusclesHu = "Bicepsz, Alkar",
            defaultSets = 3,
            defaultReps = 8,
            defaultWeightKg = 0f,
            defaultRestSec = 120,
            stepsHu = listOf(
                "Kapaszkodj meg vállszélességnél kicsit szélesebb fogással.",
                "Húzd fel magad, amíg az állad a rúd fölé ér.",
                "Kontrolláltan engedd le magad."
            )
        ),
        ExerciseItem(
            id = "lunges",
            nameHu = "Kitörés",
            nameEn = "Lunges",
            nameDe = "Ausfallschritte",
            nameRo = "Fandări",
            category = ExerciseCategory.LEGS,
            equipmentHu = "Saját testsúly / Súlyzó",
            primaryMusclesHu = "Combfeszítő, Farizom",
            secondaryMusclesHu = "Combhajlító, Vádli",
            defaultSets = 3,
            defaultReps = 12,
            defaultWeightKg = 12f,
            defaultRestSec = 90
        ),
        ExerciseItem(
            id = "bulgarian_split_squat",
            nameHu = "Bolgár guggolás",
            nameEn = "Bulgarian Split Squat",
            nameDe = "Bulgarische Kniebeuge",
            nameRo = "Genuflexiuni bulgărești",
            category = ExerciseCategory.LEGS,
            equipmentHu = "Kézisúlyzó",
            primaryMusclesHu = "Comb, Farizom",
            secondaryMusclesHu = "Core",
            defaultSets = 3,
            defaultReps = 12,
            defaultWeightKg = 5.5f,
            defaultRestSec = 90,
            prWeightKg = 5.5f,
            prReps = 15
        ),
        ExerciseItem(
            id = "lateral_raise",
            nameHu = "Oldalemelés",
            nameEn = "Lateral Raise",
            nameDe = "Seitheben",
            nameRo = "Ridicări laterale",
            category = ExerciseCategory.SHOULDERS,
            equipmentHu = "Kézisúlyzó",
            primaryMusclesHu = "Oldalsó vállizom",
            secondaryMusclesHu = "Csuklyás izom",
            defaultSets = 3,
            defaultReps = 15,
            defaultWeightKg = 5.5f,
            defaultRestSec = 60,
            prWeightKg = 5.5f,
            prReps = 15
        ),
        ExerciseItem(
            id = "overhead_triceps",
            nameHu = "Tricepsz fej fölött",
            nameEn = "Overhead Triceps Extension",
            nameDe = "Trizepsdrücken über Kopf",
            nameRo = "Extensii triceps deasupra capului",
            category = ExerciseCategory.ARMS,
            equipmentHu = "Kézisúlyzó",
            primaryMusclesHu = "Tricepsz hosszú fej",
            secondaryMusclesHu = "Alkar",
            defaultSets = 3,
            defaultReps = 12,
            defaultWeightKg = 5.5f,
            defaultRestSec = 60,
            prWeightKg = 5.5f,
            prReps = 12
        )
    )

    val defaultPrograms: List<TrainingProgramModel> = listOf(
        TrainingProgramModel(
            id = "home_ab_basic",
            nameHu = "Otthoni alap A/B",
            nameEn = "Home Basic A/B",
            subtitleHu = "Kezdőbarát • Saját testsúlyos • 2 edzésnap (A/B)",
            subtitleEn = "Beginner friendly • Bodyweight/Dumbbell • 2 days (A/B)",
            tagsHu = listOf("Beépített sablon", "Otthon", "Kezdő"),
            isCustom = true,
            isActive = true,
            days = listOf(
                ProgramDayModel(
                    id = "day_a",
                    dayCode = "A nap",
                    titleHu = "Teljes test A",
                    titleEn = "Full Body A",
                    durationMinutes = 60,
                    exercises = listOf(
                        defaultExercises[0], // Goblet guggolás
                        defaultExercises[2], // Fekvőtámasz
                        defaultExercises[3], // Evezés gumiszalaggal
                        defaultExercises[5], // Plank
                        defaultExercises[10], // Oldalemelés
                        defaultExercises[11], // Tricepsz fej fölött
                        defaultExercises[8], // Kitörés
                        defaultExercises[6]  // Oldalsó plank
                    )
                ),
                ProgramDayModel(
                    id = "day_b",
                    dayCode = "B nap",
                    titleHu = "Teljes test B",
                    titleEn = "Full Body B",
                    durationMinutes = 50,
                    exercises = listOf(
                        defaultExercises[9], // Bolgár guggolás
                        defaultExercises[1], // Fekvenyomás
                        defaultExercises[4], // Döntött törzsű evezés
                        defaultExercises[7], // Húzódzkodás
                        defaultExercises[10], // Oldalemelés
                        defaultExercises[5], // Plank
                        defaultExercises[11]  // Tricepsz
                    )
                )
            )
        ),
        TrainingProgramModel(
            id = "custom_equipment_abc",
            nameHu = "Saját felszerelés",
            nameEn = "Custom Equipment Hybrid",
            subtitleHu = "Hibrid program • Alapgyakorlatok • 3 edzésnap (A/B/C)",
            subtitleEn = "Hybrid program • Compound lifts • 3 days (A/B/C)",
            tagsHu = listOf("Beépített sablon", "Otthon", "Középhaladó"),
            isCustom = true,
            isActive = false,
            days = listOf(
                ProgramDayModel(
                    id = "custom_a",
                    dayCode = "A nap",
                    titleHu = "Alap A (Nyomó + Láb)",
                    titleEn = "Base A (Push + Legs)",
                    durationMinutes = 55,
                    exercises = defaultExercises.take(5)
                ),
                ProgramDayModel(
                    id = "custom_b",
                    dayCode = "B nap",
                    titleHu = "Alap B (Húzó + Core)",
                    titleEn = "Base B (Pull + Core)",
                    durationMinutes = 50,
                    exercises = defaultExercises.drop(3).take(5)
                )
            )
        ),
        TrainingProgramModel(
            id = "strength_mass_abcd",
            nameHu = "Erő és izomtömeg",
            nameEn = "Strength & Hypertrophy",
            subtitleHu = "Haladó • Felszereléses • 4 edzésnap (A/B/C/D)",
            subtitleEn = "Advanced • Gym equipment • 4 days (A/B/C/D)",
            tagsHu = listOf("Beépített sablon", "Edzőterem", "Haladó"),
            isCustom = false,
            isActive = false,
            days = listOf(
                ProgramDayModel(
                    id = "str_a",
                    dayCode = "A nap",
                    titleHu = "Felsőtest Erő",
                    titleEn = "Upper Body Strength",
                    durationMinutes = 65,
                    exercises = listOf(defaultExercises[1], defaultExercises[4], defaultExercises[7], defaultExercises[10], defaultExercises[11])
                )
            )
        ),
        TrainingProgramModel(
            id = "functional_abc",
            nameHu = "Funkcionális edzés",
            nameEn = "Functional Training",
            subtitleHu = "Középhaladó • Teljes test • 3 edzésnap (A/B/C)",
            subtitleEn = "Intermediate • Full body • 3 days (A/B/C)",
            tagsHu = listOf("Beépített sablon", "Otthon", "Középhaladó"),
            isCustom = false,
            isActive = false,
            days = listOf(
                ProgramDayModel(
                    id = "func_a",
                    dayCode = "A nap",
                    titleHu = "Funkcionális A",
                    titleEn = "Functional A",
                    durationMinutes = 45,
                    exercises = listOf(defaultExercises[0], defaultExercises[2], defaultExercises[5], defaultExercises[6])
                )
            )
        ),
        TrainingProgramModel(
            id = "conditioning_abc",
            nameHu = "Kondíció és erőnlét",
            nameEn = "Conditioning & Stamina",
            subtitleHu = "Minden szint • Vegyes • 3 edzésnap (A/B/C)",
            subtitleEn = "All levels • Mixed • 3 days (A/B/C)",
            tagsHu = listOf("Beépített sablon", "Vegyes", "Kezdő"),
            isCustom = false,
            isActive = false,
            days = listOf(
                ProgramDayModel(
                    id = "cond_a",
                    dayCode = "A nap",
                    titleHu = "Kondíció A",
                    titleEn = "Conditioning A",
                    durationMinutes = 40,
                    exercises = listOf(defaultExercises[0], defaultExercises[2], defaultExercises[8], defaultExercises[5])
                )
            )
        )
    )

    val initialCalendarEvents: List<CalendarDayEvent> = listOf(
        CalendarDayEvent("ev_1", 6, "17:30", "Teljes test A", 8, 55, CalendarEventStatus.COMPLETED, "A"),
        CalendarDayEvent("ev_2", 13, "18:00", "Teljes test B", 7, 50, CalendarEventStatus.COMPLETED, "B"),
        CalendarDayEvent("ev_3", 17, "17:15", "Teljes test A", 8, 58, CalendarEventStatus.COMPLETED, "A"),
        CalendarDayEvent("ev_4", 20, "18:00", "Felsőtest B", 7, 50, CalendarEventStatus.COMPLETED, "B"),
        CalendarDayEvent("ev_5", 24, "18:00", "Teljes test A", 8, 60, CalendarEventStatus.PLANNED, "A"),
        CalendarDayEvent("ev_6", 24, "09:30", "Felsőtest B", 7, 50, CalendarEventStatus.COMPLETED, "B"),
        CalendarDayEvent("ev_7", 26, "09:08", "Otthoni A/B • Alap B", 6, 51, CalendarEventStatus.COMPLETED, "B"),
        CalendarDayEvent("ev_8", 27, "18:00", "Teljes test A", 8, 60, CalendarEventStatus.PLANNED, "A"),
        CalendarDayEvent("ev_9", 28, "10:00", "Otthoni A/B • Alap A", 7, 60, CalendarEventStatus.PLANNED, "A"),
        CalendarDayEvent("ev_10", 30, "10:00", "Otthoni A/B • Alap B", 6, 55, CalendarEventStatus.PLANNED, "B"),
        CalendarDayEvent("ev_11", 31, "18:00", "Teljes test A", 8, 60, CalendarEventStatus.PLANNED, "A")
    )

    val initialLoggedWorkouts: List<LoggedWorkoutEntry> = listOf(
        LoggedWorkoutEntry(
            id = "log_1",
            dateLabelHu = "okt. 26., H • 09:08",
            titleHu = "Otthoni A/B – Alap (személyre szabott) • Alap B",
            durationMinutes = 51,
            workTimeLabel = "36:20",
            restTimeLabel = "14:40",
            totalSets = 12,
            totalVolumeKg = 1172,
            summaryExercisesHu = "Bolgár guggolás, Tricepsz fej fölött, Döntött törzsű evezés +3 gyakorlat",
            hasPr = true,
            photoCount = 0,
            exerciseDetails = listOf(
                LoggedExerciseDetail(
                    nameHu = "1. Guggolás",
                    summaryPrescription = "4 × 8 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 60f, 8, 2, true),
                        ExerciseSetItem(2, 60f, 8, 2, true),
                        ExerciseSetItem(3, 65f, 8, 1, true),
                        ExerciseSetItem(4, 65f, 8, 1, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "2. Fekvenyomás",
                    summaryPrescription = "4 × 8 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 75f, 8, 2, true),
                        ExerciseSetItem(2, 80f, 8, 1, true),
                        ExerciseSetItem(3, 80f, 8, 1, true),
                        ExerciseSetItem(4, 80f, 8, 0, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "3. Evezés rúddal",
                    summaryPrescription = "4 × 10 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 55f, 10, 2, true),
                        ExerciseSetItem(2, 60f, 10, 1, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "4. Vállból nyomás",
                    summaryPrescription = "3 × 10 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 40f, 10, 2, true),
                        ExerciseSetItem(2, 42.5f, 10, 1, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "5. Húzódzkodás",
                    summaryPrescription = "3 × max",
                    sets = listOf(
                        ExerciseSetItem(1, 0f, 10, 1, true),
                        ExerciseSetItem(2, 0f, 9, 1, true)
                    )
                )
            )
        ),
        LoggedWorkoutEntry(
            id = "log_2",
            dateLabelHu = "okt. 15., K • 18:24",
            titleHu = "Felsőtest erő",
            durationMinutes = 56,
            workTimeLabel = "39:10",
            restTimeLabel = "16:50",
            totalSets = 18,
            totalVolumeKg = 1397,
            summaryExercisesHu = "Fekvenyomás, evezés, vállból nyomás +3 gyakorlat",
            hasPr = true,
            photoCount = 1,
            exerciseDetails = listOf(
                LoggedExerciseDetail(
                    nameHu = "1. Fekvenyomás",
                    summaryPrescription = "4 × 8 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 80f, 8, 1, true),
                        ExerciseSetItem(2, 80f, 8, 1, true),
                        ExerciseSetItem(3, 82.5f, 8, 1, true),
                        ExerciseSetItem(4, 82.5f, 8, 0, true)
                    )
                )
            )
        ),
        LoggedWorkoutEntry(
            id = "log_3",
            dateLabelHu = "okt. 13., V • 07:12",
            titleHu = "Futás – Reggeli kör",
            durationMinutes = 13,
            workTimeLabel = "12:36",
            restTimeLabel = "00:00",
            totalSets = 1,
            totalVolumeKg = 0,
            summaryExercisesHu = "Margitsziget • Privát GPS-útvonal • 31 GPS-pont",
            isOutdoorRun = true,
            distanceKm = 1.62f,
            paceLabel = "7:48 /km"
        ),
        LoggedWorkoutEntry(
            id = "log_4",
            dateLabelHu = "okt. 11., P • 17:03",
            titleHu = "Teljes testes edzés A",
            durationMinutes = 48,
            workTimeLabel = "36:20",
            restTimeLabel = "11:40",
            totalSets = 16,
            totalVolumeKg = 1430,
            summaryExercisesHu = "Guggolás, fekvenyomás, húzódzkodás +2 gyakorlat",
            photoCount = 2,
            exerciseDetails = listOf(
                LoggedExerciseDetail(
                    nameHu = "1. Guggolás",
                    summaryPrescription = "4 × 8 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 60f, 8, 2, true),
                        ExerciseSetItem(2, 60f, 8, 2, true),
                        ExerciseSetItem(3, 65f, 8, 1, true),
                        ExerciseSetItem(4, 65f, 8, 1, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "2. Fekvenyomás",
                    summaryPrescription = "4 × 8 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 75f, 8, 2, true),
                        ExerciseSetItem(2, 80f, 8, 1, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "3. Evezés rúddal",
                    summaryPrescription = "4 × 10 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 55f, 10, 2, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "4. Vállból nyomás",
                    summaryPrescription = "3 × 10 ism.",
                    sets = listOf(
                        ExerciseSetItem(1, 40f, 10, 2, true)
                    )
                ),
                LoggedExerciseDetail(
                    nameHu = "5. Húzódzkodás",
                    summaryPrescription = "3 × max",
                    sets = listOf(
                        ExerciseSetItem(1, 0f, 10, 1, true)
                    )
                )
            )
        ),
        LoggedWorkoutEntry(
            id = "log_5",
            dateLabelHu = "okt. 8., K • 06:50",
            titleHu = "Futás – Park körök",
            durationMinutes = 26,
            workTimeLabel = "26:18",
            restTimeLabel = "00:00",
            totalSets = 1,
            totalVolumeKg = 0,
            summaryExercisesHu = "Városi park • GPS útvonal",
            isOutdoorRun = true,
            distanceKm = 3.21f,
            paceLabel = "8:11 /km"
        )
    )

    val prRecords: List<PrRecordEntry> = listOf(
        PrRecordEntry("pr1", "Bolgár guggolás", "Ismétlés • 2026. okt. 26.", "2026. okt. 26.", "10 ism.", "15 ism."),
        PrRecordEntry("pr2", "Tricepsz fej fölött", "Súly • 2026. okt. 26.", "2026. okt. 26.", "5 kg", "5,5 kg"),
        PrRecordEntry("pr3", "Goblet guggolás", "Súly • 2026. okt. 24.", "2026. okt. 24.", "7,5 kg", "11 kg"),
        PrRecordEntry("pr4", "Földön fekve nyomás", "Súly • 2026. okt. 24.", "2026. okt. 24.", "5,5 kg", "11 kg"),
        PrRecordEntry("pr5", "Fekvenyomás", "1RM rekordsúly • 2026. szept. 14.", "2026. szept. 14.", "95 kg", "100 kg"),
        PrRecordEntry("pr6", "Döntött törzsű evezés – kétkezes rúddal", "Legnagyobb súly • 2026. okt. 19.", "2026. okt. 19.", "10 kg", "12,5 kg"),
        PrRecordEntry("pr7", "Fekvőtámasz", "Legjobb ismétlés • 2026. okt. 14.", "2026. okt. 14.", "10 ism.", "12 ism.")
    )
}
