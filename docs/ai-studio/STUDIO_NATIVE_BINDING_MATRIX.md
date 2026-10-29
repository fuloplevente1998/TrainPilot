# Studio → natív bekötési térkép,24nézet

A prototípus5screenfájlban24főképernyőt tartalmaz; ez NEM24új natív motor.
Ellenőrzött eredeti export:574f17f6…bcefcc, reference-kód8ba6a388.
A natív útvonalakat c167d92f forrásán ellenőriztük. A státusz nem runtimePASS.
A közös fontok/felületek minden natív nézetre hatnak, ez nem teljes képernyőport.

| Studio composable | Eredeti fájl | Meglévő natív hely | Bekötési feltétel / státusz |
|---|---|---|---|
| `HomeScreen` | `HomeScreen.kt` | `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt` | Studio kompozíció részben kész; profil/planner/brand új bekötés külön UNRUN. |
| `WorkoutTabScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt` | Valós quick/library/outdoor/day callbackek megvannak; teljes kompozíció hátra. |
| `ProgramsListScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt` | Valós programok/aktiválás/planner; Studio programkártyák hátra. |
| `ProgramEditorScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt` | Kanonikus editor/actionToken megmarad; csak a megjelenítés portolandó. |
| `CalendarPlannerScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt` | Valós planner és ütemezés megvan; fix demódátum nem vehető át. |
| `ExerciseLibraryScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt` | Meglévő szűrők/catalog/custom editor/canonical video megőrzendő. |
| `ExerciseDetailScreen` | `WorkoutAndPlanningScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt` | Részlet a meglévő library UI-ban; nem új párhuzamos katalógus. |
| `ActiveStrengthWorkoutScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Meglévő session VM/commands/setID; Studio állapotgomb nem új motor. |
| `ActiveTimedWorkoutScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Egy kanonikus időzítő; pihenő pause/skip/±15 útvonal marad. |
| `ActiveBilateralWorkoutScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Valós bal/jobb kulcs és óra; nem a demo számláló. |
| `ActiveOutdoorGpsScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Valós GPS/permission/segment/owner; fix távolság és térkép nem. |
| `IntervalSettingsScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Meglévő intervalconfig/action/guard a ViewModel-ben; új timer nem. |
| `WorkoutSummaryScreen` | `ActiveMeasurementScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt` | Kanonikus finish/save/pause/discard, egy finalizer; nem demo listamentés. |
| `LogTabScreen` | `LogAndProgressScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt` | Meglévő keresés/keyset/paging/detail; gazdag Studio sorok még hátra. |
| `LogWorkoutDetailScreen` | `LogAndProgressScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt` | Meglévő JournalDetailDialog és editor/canonical history; nem új history modell. |
| `LogRouteMapScreen` | `LogAndProgressScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt` | Valódi mentett szegmens/távolság; online alaptérkép külön valós provider hook. |
| `ProgressTabScreen` | `LogAndProgressScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt` | Main anatómia/maszk/zoom/1RM/photo/cardio/weight megvan; volume/PR részletek hátra. |
| `HealthTabScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt` | 26valósmetrika+history/provider/sync/records megvan; fő6/pulse/weight accordions hátra. |
| `CoachDetailScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt` | Valós NativeCoachReadiness/advice/target; Samsung/TrainPilot/limited külön marad. |
| `SettingsMainScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt` | Működő nyelv/32paletta/profile/timer/cloud/backup; Studio elrendezés hátra. |
| `WatchAndSyncScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt` | Valós watch state a Home VM-ben + WearSyncRepository/PhoneOwnership; új read-only UI kell. |
| `BackupRestoreScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt` | Valós SAF/BackupRestoreManager/ReplacementRecoveryPanel; nincs kitalált archívumlista. |
| `PermissionsScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt` | Meglévő HC/Samsung/runtime/OS permission callbackek; installed nem granted. |
| `PrivatePhotosScreen` | `HealthCoachAndSettingsScreens.kt` | `android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt` | Valós privát photoID/file/generation/admission; demo placeholder nem valódi fotó. |

## Adat- és állapothatárok

A MeasurementModeSwitcherRow és HomeDemoMode felületnézegető vezérlő: a Studio
prototípusban maradhat; a natív session mérési módját csak a meglévő kanonikus
belépő/admission módosítja. Nem kerül új DemoDataProvider/ViewModel az appba.
A felső ötfül kifejezett userdöntés, nem mozgatható vissza a régi alsó mintára.

Health: az elérhető26provider-metrika, sleepSessions és HealthJournalStore.page
adat az igazság. Pulzustrend7naptári nap, hiányzó nap nem0. Source/partial/stale
nem rejthető el. A kiválasztott hiányzó nap nem helyettesíthető korábbi nap értékével.
A testsúly provider-érték és a külön portable BodyWeight journal nem keverhető
össze forrásjelzés nélkül. A legacyBLE új diagnosztikai oldal nem fő termékirány;
főintegráció WearOS/HC/Samsung. Régi adat/protokoll önkényesen nem törölhető.

Progress: a WorkoutHistoryEntity már tartalmaz totalVolumeKg/durationSeconds/
completedSetsCount/prRecordsJson226 mezőket. A teljes main overviewhoz ezekből
read-only, tombstone-tisztelő, időszakos/oldalazott SQL-projekció készíthető;
ne tölts teljes GPS/photo/set gráfot és ne írj új személyesrekord-motort.
Natív izomeloszlás a meglévőquery eredménye, nem kitalált százalék/fáradtság.
Main anatómiai assetek a NativeMuscleLoadVisual komponensben már megvannak.

Home személyes terv: a valódi TrainingProgramsViewModel.openPersonalPlanner és
PersonalProgramPreviewDialog használható. A profilgomb a Settings VM writerét
használja. Előnézet/cancel nem írhat programot, profilt vagy sessiont; mentés
csak a már meglévő explicit elfogadás után. Root új UI-intent memóriában él,
process-restart nem játszhatja újra. A valódi Coach-advice kivonat még külön port.

## Pontos forráslinkek

- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt
- https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/c167d92f/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt

Eredeti összesStudio rawforrás: docs/ai-studio/export-20261027/README.md.
