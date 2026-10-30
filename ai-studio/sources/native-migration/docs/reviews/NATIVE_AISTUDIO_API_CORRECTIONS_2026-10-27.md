# AI Studio – pontos API- és fájlnév-illesztés

Ellenőrzött kiinduló forrás: `05bc6ae7b0c143c812e5634efdd9a4bc535f588e` a
`feat/native-android-migration` ágon. Az app zöld forrása `39da06cc`.
A tervben szereplő megjelenést megőrizzük, a nem létező API-k helyett a valódi
bekötéseket használjuk. Ez a lista nem egy új API-réteg megírására szóló utasítás.

## Konkrét eltérések

| Tervbeli név / hívás | Tényleges megfelelő / megkötés |
|---|---|
| `com.repforge.app.data.local.TrainPilotDatabase` | `com.repforge.app.data.db.TrainPilotDatabase` |
| `data.local.entity.WorkoutHistoryEntity` | `com.repforge.app.data.db.WorkoutHistoryEntity` |
| `data.store.HealthJournalStore` | `com.repforge.app.HealthJournalStore` |
| `data.repository.NativeHomeRepository` objektum | `ui.home.NativeHomeRepository(database)` osztály; bounded program/célprogramnap/edzésszám snapshot. |
| `NativeHomeRepository.loadTodayMetrics(...)` | Nincs ilyen metódus. A repository `suspend read(activeProgramId, language, now, zone)` metódust használ, a Home-metrikák a ViewModel `fetchTodayMetrics` metódusában készülnek. |
| `HomeViewModel.rebuildState()` | `loadHomeData()`, epoch-ellenőrzéssel és adatcsere-continuation admissionnel. |
| Home-ból `NativeCoachEngine.load(...)` | Ilyen Home-hívás nincs; nincs mit eltávolítani. A dokumentum gyökérok-állítása nem a jelenlegi forrásra vonatkozik. |
| `NativeHomeRecoveryPresentation.resolve(context)` | `select(days, history, now, zone, refreshFailed = ...)`; valódi provider-/forrásellenőrzéssel. |
| `HomeRecoveryUiModel`, `HomeRecoveryTone` | `NativeHomeRecoveryPresentation.Model` és `Source`. A Source forrást jelent, nem helyettesíthető readiness-színskálával; nincs külön HomeRecoveryTone enum. |
| `HomeMetricCardUiModel` | `TodayMetricsUiModel`: sleep/calories/pulse/steps/workouts/recovery. A meglévő pulzus/kalória/lépés csempék nem veszhetnek el egy másik hatos lista miatt. |
| `TrainPilotTheme.colors`, `.spacing` | `LocalTrainPilotColors.current` és `TrainPilotDimensions`; a TrainPilotTheme Composable függvény. |
| `ui.theme.TrainPilotHeroSurface` és más surface importok | A meglévő közös komponensek a `ui.components` csomagban vannak. A kiinduló forrásban Hero/Section/Subtle/SurfaceCard/SectionHeader/StatusBadge/PrimaryButton létezik; a többi tervezett elem külön implementációt igényel, nem létező importként nem használható. |
| `TrainingLandingViewModel` | Az entry jelenleg a `TrainingProgramsViewModel.uiState`-et használja. Nem kell mellé egy párhuzamos program-/session-modell. |
| `ProgramDaySummaryUiModel`, `day.id` | `ProgramDayEntity`; `dayId` és `compositeId`. Az indításhoz a programId és dayId együtt szükséges. |
| `WorkoutLauncher.startNextProgramDay/startProgramDay` | Ilyen launcher nincs. Home: `onStartWorkout(programId, dayId, scheduleId)`; Training: `onStart(programId, dayId)`; ezek a meglévő navigáció/engine indítási útjára vezetnek. |
| `uiState.isLoading` | Home: `state.loading`. |
| `uiState.hasActiveWorkout`, `activeWorkoutTitle`, `activeCompletedSets` | Ezek nem HomeUiState mezők. Az aktív session kanonikus projekciója az `ActiveWorkoutStore.snapshotFlow`; a jelenlegi folytatás a meglévő callbacket használja. |
| `uiState.isFinishingWorkout`, helyi launching flag | A tényleges NavHost `busy`/navigationBlocked és start/finalizer/restore/owner admission védelmei megmaradnak; egy helyi flag nem helyettesíti ezeket. |
| `nextDayId`, `nextDayExerciseCount`, `activeProgramName` | Home: `nextTarget`, `nextDayExercisesCount`, `recommendedProgram`, és a valódi lokalizált programnapok. Az első napból nem találunk ki következő edzést. |
| `native_modes_compare_stale`, fix `hu` | Az érintett UI-teszt tényleges elutasítási felirata `R.string.native_modes_error`, az aktuális appnyelven. A domain visszautasítási ellenőrzéseket nem gyengítjük. |
| `WorkoutHistoryScreen.kt` | `ui/journal/JournalScreen.kt`; a részlet `JournalDetailDialog` ugyanebben a fájlban, nem külön második history-rendszer. |
| `ProgressAnalyticsScreen.kt` | `ui/progress/ProgressScreen.kt`. |
| `TrainingCalendarScreen.kt` | `ui/calendar/CalendarScreen.kt`. |
| Külön WatchSyncSettings/BackupRestore/Permissions/PrivatePhotos screen fájlok | A meglévő megvalósítás nagy része a SettingsScreen meglévő paneljeiben/callbackjeiben van; csak a tervezett névből ne következzen új adatút. |
| `native_training_landing_01` és `_02` felirat | `_01` a Gyors edzés csoportcíme (RO: Antrenament rapid, DE: Schnelltraining), `_02` a Gyakorlat választása gomb. Nem ismétlődő feliratok; a csoportcím eltávolítása locale-regressziót okozott. |
| `history_*`, `home_metric_card_*`, stb. | A tervbeli nevek nem írják felül a meglévő `journal_*`, `home_metric_*`, `exercise_library_*`, stb. tageket. A működő tagek és session-guardok megmaradnak. |

## Forráshoz igazított munkaszabály a másik chatnek

Ne feltételezd, hogy a leírásban megnevezett osztály, mező vagy resource már létezik.
A tényleges Kotlin-deklarációkat és hívóhelyeket ellenőrizd a fenti SHA-n. Ha nem
féred hozzá, nevezd meg a hiányzó fájlt; ne találj ki fordulóképesnek mondott API-t.
Új UI-építőelem készülhet a közös komponenscsomagban, de új párhuzamos workout,
recovery, repository, migráció vagy finalizer nem készül a dizájn helyett.

## Közvetlen források

- android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt
- android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt
- android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt
- android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt
- android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt
- android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt
- android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt
- android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt
- android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt
- android/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt
- android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModeApprovalUiTest.kt
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/05bc6ae7b0c143c812e5634efdd9a4bc535f588e/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModeApprovalUiTest.kt

## Integrációs pontosítás

Wear OS, Google Health Connect és Samsung Health a fő integrációk. A kínai órás
BLE-próba nem indok külön új BLE-felületre. A natív örökölt diagnosztikai fül nem
a main kötelező vizuális mintája. Mentett mérések/protokollok törlésére nincs utasítás.
