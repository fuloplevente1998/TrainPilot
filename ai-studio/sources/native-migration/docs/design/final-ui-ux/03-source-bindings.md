# Meglévő Compose → tervezett komponens → adat/callback megfeleltetés

Exact olvasási baseline: `c116a042229f61b86babbbeb23c5d2c360486e20`. **Forrásbekötés azonosítva, nem runtime elfogadás.** A táblázat nagybetűs komponensnevei javaslatok. A callback-sorok csoportosítanak; nem egy új, mindent elvégző API nevei.

## V01 – Kezdőlap

- Meglévő Compose/route: `home`; [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36) · [homeenergy](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L117) · [homequery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt#L18) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)
- Tervezett megjelenítők: **SessionHero / NextWorkoutCard / MetricTile**.
- Meglévő bemenet: HomeUiState: nextTarget, loading, loadFailed, hasImportedDraft, todayMetrics, watchState; aktív snapshot a scaffoldban.
- Megőrzendő callbackek: onStartWorkout/onResumeDraft/onQuickWorkout; scaffold selectDestination; loadHomeData.
- Kanonikus adat/writer út: NativeHomeRepository + HealthJournalStore + WearLiveHealthStore; activeSnapshot → kanonikus start/recovery.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V02 – Edzésválasztó

- Meglévő Compose/route: `training`; [training](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt#L23) · [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)
- Tervezett megjelenítők: **QuickActionCard / ProgramDayCard**.
- Meglévő bemenet: TrainingProgramsUiState.programs + activeProgramId/busy paraméter.
- Megőrzendő callbackek: onQuick/onStart/onPrograms/onLibrary/onCalendar/onCoach.
- Kanonikus adat/writer út: ProgramWithDays → NativeWorkoutSession.startProgramWorkout; nem új programnap-számítás.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V03 – Programok

- Meglévő Compose/route: `programs`; [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32)
- Tervezett megjelenítők: **ProgramCard / ProgramActionMenu / ModePreview**.
- Meglévő bemenet: TrainingProgramsUiState.programs/activeProgramId + editor/mode preview.
- Megőrzendő callbackek: activateProgram/saveProgram/deleteProgram/previewRestoreDay/confirmRestoreDay; approveTrainingModeCompared.
- Kanonikus adat/writer út: meglévő NativeProgramEditor és NativeTrainingModeChange; UI csak rendez.
- Meglévő regresszióforrás: [NativeProgramActionsUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgramActionsUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V04 – Naptár

- Meglévő Compose/route: `calendar`; [calendar](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L28) · [calendarvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarViewModel.kt#L35) · [calendarplanner](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L196)
- Tervezett megjelenítők: **MonthGrid / DayTimeline / PlannerForm**.
- Meglévő bemenet: CalendarUiState.month/day/plans/history/plannerLoaded/plannerPreview/programs; plannerOpen lokális.
- Megőrzendő callbackek: selectDay/changeMonth/save/status/previewPlanner/applyPlanner/cancelPlanner; onStart/onJournal.
- Kanonikus adat/writer út: NativeCalendarRepository és NativeCalendarPlanner, meglévő metadata lease/CAS/epoch.
- Meglévő regresszióforrás: [NativeCalendarPlannerUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeCalendarPlannerUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V05 – Gyakorlatkönyvtár

- Meglévő Compose/route: `exercise_library`; [library](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L115) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)
- Tervezett megjelenítők: **LibrarySearch / FilterChips / ExerciseRow**.
- Meglévő bemenet: ExerciseLibraryState.rows/favorites/loading/saving; query/favoritesOnly/muscle/selectedId lokális.
- Megőrzendő callbackek: toggleFavorite; saveCustom/deleteCustom; selectedId kiválasztás.
- Kanonikus adat/writer út: NativeExerciseFavorites/NativeCustomExerciseEditor; meglévő exercise_library route.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V06 – Gyakorlat részletei

- Meglévő Compose/route: `exercise_library / selectedId`; [librarydetail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L165) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)
- Tervezett megjelenítők: **ExerciseDetail / DemoSource / FavoriteAction**.
- Meglévő bemenet: selected ExerciseLibraryEntity + NativeExerciseDemo.resolve; openingError lokális.
- Megőrzendő callbackek: open(link), toggleFavorite; editorOpen saját elemnél.
- Kanonikus adat/writer út: ellenőrzött VIDEO vs UNVERIFIED_SEARCH; új onAddToWorkout nincs.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V07 – Súlyzós aktív edzés

- Meglévő Compose/route: `active_workout / reps`; [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [setcard](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L717) · [rest](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1078) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683)
- Tervezett megjelenítők: **ExerciseHeader / SetRow / RestPanel**.
- Meglévő bemenet: ActiveWorkoutUiState.exercises/currentExerciseIndex/timer*/undo*/timerOwner/isFinishing.
- Megőrzendő callbackek: editSet/toggleSetCompleted/setSetRir/addSetToExercise/removeSetFromExercise/selectExercise/undoLastSet/adjustRestTimer/skipRestTimer.
- Kanonikus adat/writer út: NativeWorkoutTarget és NativeTimerTarget → meglévő PhoneWorkoutExecution/NativeWorkoutEditing.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V08 – Egyoldali időalapú edzés

- Meglévő Compose/route: `active_workout / timed unilateral`; [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)
- Tervezett megjelenítők: **MeasurementCard / TimedSetRow**.
- Meglévő bemenet: ActiveWorkoutUiState.stopwatchSetId/stopwatchSide/stopwatchSeconds/stopwatchRunning; set.leftSeconds.
- Megőrzendő callbackek: toggleSetStopwatch/resetSetStopwatch; configureIntervals/startIntervals/stopIntervals.
- Kanonikus adat/writer út: LowerSetStopwatch; meglévő natív stopwatch auto-complete policy.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V09 – Kétoldali időmérés

- Meglévő Compose/route: `active_workout / timed bilateral`; [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838)
- Tervezett megjelenítők: **BilateralMeasurementPanels**.
- Meglévő bemenet: ActiveSetUiModel.leftSeconds/rightSeconds/isCompleted + egy kanonikus stopwatchSide.
- Megőrzendő callbackek: toggleSetStopwatch(target,side)/resetSetStopwatch; selectExercise.
- Kanonikus adat/writer út: ugyanaz a natív mérő, nem két új timer; mindkét pozitív oldal szabálya marad.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V10 – Kültéri/GPS mérés

- Meglévő Compose/route: `active_workout / distance`; [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [distance](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L808) · [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)
- Tervezett megjelenítők: **GpsStatus / DistanceMetricGrid / PermissionIssue**.
- Meglévő bemenet: isGpsActive/gpsBindingState/gpsHasFix/gpsError/gpsAccuracyMeters/liveHeartRateBpm és rendered target.
- Megőrzendő callbackek: requestGps → toggleGpsTracking(target,id); openRouteMap; existing editSet.
- Kanonikus adat/writer út: NativeGpsPermissionRecovery → NativeWorkoutEditing/DistanceTrackingService; új GPS-writer nincs.
- Meglévő regresszióforrás: [NativeGpsPermissionRecoveryUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeGpsPermissionRecoveryUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V11 – Intervallum beállítása

- Meglévő Compose/route: `active_workout / IntervalOptions; settings timer`; [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)
- Tervezett megjelenítők: **IntervalForm / PresetScope**.
- Meglévő bemenet: intervalWork/Rest/Rounds/Sound/Vibration/AutoAdvance/Saving; lokális work/rest/rounds/global.
- Megőrzendő callbackek: configureIntervals/startIntervals/stopIntervals/skipRestTimer; globális beginTimerDefaults/saveTimerDefaults.
- Kanonikus adat/writer út: IntervalOptions és NativeTimerDefaultsDialog → eredeti preset validátor és szolgáltatás.
- Meglévő regresszióforrás: [NativeTimerDefaultsUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTimerDefaultsUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V12 – Edzés befejezése

- Meglévő Compose/route: `active_workout / showFinishModal`; [finish](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1128) · [finishwriter](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L1005) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683) · [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105)
- Tervezett megjelenítők: **FinishSummary / EffortChoices / DestructiveActions**.
- Meglévő bemenet: showFinishModal/isFinishing + meglévő measuredWorkSeconds/measuredRestSeconds; effort jelenleg callbackérték.
- Megőrzendő callbackek: openFinishModal/closeFinishModal/finishWorkout(effort,onSaved); leaveWorkout/discardWorkout.
- Kanonikus adat/writer út: TrainPilotWorkoutFinalization meglévő entry; note nem paraméter, Root-funkciókérdés.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V13 – Napló

- Meglévő Compose/route: `journal`; [journal](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L55) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)
- Tervezett megjelenítők: **JournalSearch / SummaryCard / PagingIssue**.
- Meglévő bemenet: JournalUiState.search/month és workouts.collectAsLazyPagingItems.
- Megőrzendő callbackek: setSearch/setMonth/openDetails; Paging retry.
- Kanonikus adat/writer út: NativeJournalRepository / Room summary, nincs route/photo részlet-hydrate listához.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V14 – Naplózott edzés részletei

- Meglévő Compose/route: `journal / selectedWorkoutId modal`; [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65) · [journaledit](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt#L33) · [sources](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalHealthPanel.kt#L23) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257)
- Tervezett megjelenítők: **WorkoutDetailTabs / SourcePanel / Editor**.
- Meglévő bemenet: JournalUiState.detail/detailLoading/detailError/selectedWorkoutId/deleting + editor StateFlow.
- Megőrzendő callbackek: openEditor/saveEditor/closeDetails/refreshWorkoutHealth/openRouteMap/requestDeleteWorkout/confirmDeleteWorkout.
- Kanonikus adat/writer út: NativeJournalEditor, NativeWorkoutHealth, JournalHealthPanel; források elkülönülnek.
- Meglévő regresszióforrás: [NativeJournalFeedbackUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeJournalFeedbackUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V15 – Privát útvonaltérkép

- Meglévő Compose/route: `WorkoutRouteMapDialog modal`; [map](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L48) · [mapcanvas](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L79) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)
- Tervezett megjelenítők: **PrivateRouteSurface / CloseAction / Legend**.
- Meglévő bemenet: RouteCoordinatesUiModel.segments/pointCount/missingRouteParts.
- Megőrzendő callbackek: onDismiss; JournalViewModel.closeRouteMap vagy ActiveWorkoutViewModel.closeRouteMap.
- Kanonikus adat/writer út: WorkoutRouteCoordinates → MercatorRouteCanvas; csak geometry, nem tile provider.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V16 – Fejlődés

- Meglévő Compose/route: `progress; weight_journal közös elérés`; [progress](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt#L61) · [progressstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L40) · [cardio](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioProgressPanel.kt#L35) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48) · [weightvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalViewModel.kt#L40)
- Tervezett megjelenítők: **ProgressPanels / TrendFigure / WeightOverview**.
- Meglévő bemenet: ProgressUiState.exercise1RmTrend/muscleLoads/cardio/cardioActivities + WeightJournalUiState.
- Megőrzendő callbackek: selectExercise/selectCardioActivity/retryCardio/loadProgressData; weight setRange/retryReads/onWeightJournal.
- Kanonikus adat/writer út: NativeProgressQuery/NativeCardioProgressAdapter; WeightJournalOverview ugyanaz a testsúly-adatút.
- Meglévő regresszióforrás: [NativeCardioProgressUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeCardioProgressUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V17 – Egészség és részletes napló

- Meglévő Compose/route: `health; HealthRecordBrowser; weight_journal`; [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [metrics](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthMetrics.kt#L10) · [records](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt#L63) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48)
- Tervezett megjelenítők: **ProviderStatus / MetricGroups / RecordBrowser**.
- Meglévő bemenet: HealthDashboardUiState.metrics/selectedDay/historyChannel/historyPartial/sync*/lastSyncAt/bleState.
- Megőrzendő callbackek: syncHealth/cancelHealthSync/refresh/setProvider/setDailySourcePreference/setAutoOnOpen; record browser applyRange/restart.
- Kanonikus adat/writer út: HealthJournalStore + NativeHealthRefresh/Sync + NativeHealthRecords; provider és BLE motor érintetlen.
- Meglévő regresszióforrás: [NativeHealthSyncUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHealthSyncUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V18 – Coach

- Meglévő Compose/route: `coach`; [coach](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt#L23) · [coachvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachViewModel.kt#L26) · [readiness](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt#L14)
- Tervezett megjelenítők: **ReadinessGauge / ScoreSources / AdviceCard**.
- Meglévő bemenet: CoachUiState.readiness/target/exercises/loading/error/variationPreview.
- Megőrzendő callbackek: reload/refreshHealth/previewVariation/saveVariation/closeVariation; onStart/onCalendar/onHealth/onProgress/onPrograms.
- Kanonikus adat/writer út: NativeCoachReadiness/NativeCoachSignals és advice repository; nincs UI-képlet.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V19 – Beállítások

- Meglévő Compose/route: `settings`; [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [themepicker](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt#L31) · [lang](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeLanguagePicker.kt#L32) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)
- Tervezett megjelenítők: **SettingsSection / FormRow / ThemePicker**.
- Meglévő bemenet: SettingsUiState.settings/replacementBusy/statusMessage/profile*/timer*; Google consent külön flow.
- Megőrzendő callbackek: setTheme/setLanguage/saveProfile/beginTimerDefaults/saveTimerDefaults; Google/SAF és maintenance existing callbackek.
- Kanonikus adat/writer út: TrainPilotSettingsStore/BackupRestoreManager/GoogleSyncRepository; ugyanaz a globális settings route.
- Meglévő regresszióforrás: [NativeLocalizationLanguageUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationLanguageUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V20 – Óra és szinkron

- Meglévő Compose/route: `active_workout / ownership dialog + Home status`; [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [ownership](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L805) · [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)
- Tervezett megjelenítők: **OwnershipStatus / TransferActions**.
- Meglévő bemenet: ActiveWorkoutUiState.timerOwner/ownerEpoch/isTransferPending/canTakeBack/canHandOver; Home watchState.
- Megőrzendő callbackek: requestOwnershipBackFromWatch/startWatchMeasurementRemotely; meglévő ownership dialog.
- Kanonikus adat/writer út: PhoneOwnership193/WearSyncRepository; saját watch_sync route jelenleg nincs.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V21 – Mentés és visszaállítás

- Meglévő Compose/route: `settings / SAF + recovery`; [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [recovery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt#L19) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)
- Tervezett megjelenítők: **BackupFormat / ConsentRow / RecoveryIssue**.
- Meglévő bemenet: SettingsUiState.settings.healthBackupConsent/replacementBusy/needsRecovery/recoveryError/googleSyncState.
- Megőrzendő callbackek: onSafJsonExportCompleted/onSafJsonImportCompleted/onSafZipExportCompleted/onSafZipImportCompleted/resumeReplacement; syncDriveCloud.
- Kanonikus adat/writer út: BackupRestoreManager és GoogleSyncRepository; nem új archívumlista vagy restore-motor.
- Meglévő regresszióforrás: [NativeGoogleMaintenanceUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeGoogleMaintenanceUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V22 – Engedélyek

- Meglévő Compose/route: `kontextusos panelek: GPS/health/BLE/settings`; [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74)
- Tervezett megjelenítők: **PermissionStatus / BlockerAction**.
- Meglévő bemenet: NativeGpsPermissionRecovery state; Health hcSupported/hcGranted/backgroundBlock; BLE permission állapot.
- Megőrzendő callbackek: existing permissionLauncher/settingsIntent/retry/cancel; health background permission launcherek.
- Kanonikus adat/writer út: platform státusz + meglévő kontextusos admission; nincs közös permissions route.
- Meglévő regresszióforrás: [NativeGpsPermissionRecoveryUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeGpsPermissionRecoveryUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V23 – Privát edzésfotók

- Meglévő Compose/route: `active_workout attachedPhotos + journal photo modal`; [activephotos](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L418) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257) · [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)
- Tervezett megjelenítők: **PrivatePhotoThumb / PhotoViewer / PhotoActions**.
- Meglévő bemenet: ActiveWorkoutUiState.attachedPhotos + journal detail.photos és lokális photo kiválasztás.
- Megőrzendő callbackek: camera/gallery launcher → onPhotoCaptured; deletePhoto(id); privát modal onDismiss.
- Kanonikus adat/writer út: WorkoutPhotoRepository és PrivateJournalPhoto; progressPhotos megjelenítési entry hiányzik.
- Meglévő regresszióforrás: [NativePhoneNavigationTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.

## V24 – Program szerkesztése

- Meglévő Compose/route: `programs / ProgramEditorDialog`; [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)
- Tervezett megjelenítők: **ProgramForm / DayPicker / PrescriptionRow**.
- Meglévő bemenet: NativeProgramEditor.Draft + openedProgram/editorToken; editor outcome a VM-ben.
- Megőrzendő callbackek: onSave → saveProgram; onCancel; meglévő onChange és prescription editor callbackek.
- Kanonikus adat/writer út: NativeProgramEditor és NativeProgramDayRestore; a CAS/metadata/owner teszt nem gyengülhet.
- Meglévő regresszióforrás: [NativeProgramActionsUiTest.kt](../../../android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgramActionsUiTest.kt). Ez nem jelenti, hogy az adott teszt minden vizuális kritériumot már ellenőriz. Új screenshot/TalkBack/nagyfont eseteket csak a felszabadított második szakaszban lehet hozzáadni.
