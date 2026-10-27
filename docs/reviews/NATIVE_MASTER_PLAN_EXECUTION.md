# Native master plan – végrehajtás és forrásillesztés

Bázis: 937e66099da37bfa12ac7d48cf54660199d9bc8f, feat/native-android-migration.
Felhasználói sorrend: 1 stabilitás/V01–V02 → zöld ellenőrzés → 2 V03–V05 →
3 P2 → P3 → P4 → P5, csomagonként ellenőrzés. A mellékelt eredeti terv változatlanul:
`docs/plans/NATIVE_MASTER_PLAN_AND_FIXES.md`. A forráskódblokkok bemeneti javaslatok;
a tényleges API-hoz igazítás szükséges, funkciót vagy guardot nem törölhetnek.

## Ellenőrzött eltérések a javaslat és a repository között

- NativeHomeRepository valós helye ui/home, osztály és bounded Room snapshot.
  Nem data/repository objektum; data/local/TrainPilotDatabase és WorkoutLauncher
  importok, TrainingLandingViewModel, HomeMetricCardUiModel nem a jelenlegi API-k.
- A valós HomeViewModel már NativeHomeRecoveryPresentation.select-et hív;
  nincs Home-ból NativeCoachEngine.load és nincs loadTodayMetrics/rebuildState.
  E két fájl megtartva, a korábbi bounded/epoch/restore admission megmarad.
  A feltételezett 10× gyorsulás nincs mérve és nem állítjuk.
- A jelenlegi hat Home-metrika sleep/calories/pulse/steps/workouts/recovery.
  Nem cserélhető duration/weight/streak új listára a meglévők elvesztésével.
- A valós oldalak JournalScreen.kt és ProgressScreen.kt; WorkoutHistoryScreen és
  ProgressAnalyticsScreen néven nem készül párhuzamos implementáció.
- A training mode elutasítás valódi resource-ja native_modes_error, aktuális
  appnyelvvel. A tervben native_modes_compare_stale/hu nem az itteni szerződés.
- A quick/planning entry már loading alatt is létezik. Ezt megtartjuk; a két
  hibás tesztcsalád közvetlen offscreen kattintását tényleges scroll előzi meg.
- A runner-crash pontos oka még nem bizonyított. A main-looper/Compose idle
  szinkronizálás forrásjavítási jelölt, amíg a teljes futás nem igazolja.

## 1. lépés – első javítási jelölt

- Locale calendar/coach scroll és valódi navigation_back ellenőrzés;
  Coach variation tesztben szintén valódi görgetés. Assertion nem gyengült.
- Fixture close előtt main idle; mode approval elutasítás és teardown előtt
  Compose idle. Metric layout teszt teardown előtt is Compose idle.
- Közös Hero/Section/Subtle surface és StatusBadge, meglévő palettából.
- Home: meglévő state/callbackok, óra és minden metrika/forrás/hiba megmarad;
  nincs redundáns Quick cím; quick és metric párok intrinsic/equal height,
  nagy fontnál egymás alatti layout; quick műveleteknél busy admission.
- Training: ugyanazok a start/quick/library/planning callbackok, kompakt intrinsic
  quick gombpár (nagy betűnél egymás alatt), közös felületek/48–52dp/testTag-ek.
  Nincs kitalált következő nap, új launcher vagy második VM/session motor.
- Statikus ellenőrzés: módosított Kotlin-fájlok meglévő testTag-jei és UI @Test
  metódusai megmaradtak. Ez nem Android-fordítás vagy UI PASS.

Az első javítási jelölt ellenőrzött eredménye alább található; a 2–3. lépés
a teljes kapu zöld eredménye előtt nem kezdődött. Main/verzió/adat/PR-merge változatlan.

## Következő csomagok tényleges forrástérképe (csak feltárás)

- V03: `ui/journal/JournalScreen.kt`.
- V04: `ui/health/HealthDashboardScreen.kt`, `ui/health/HealthRecordBrowser.kt`.
- V05: `ui/progress/ProgressScreen.kt`, `ui/progress/NativeCardioProgressPanel.kt`, `ui/progress/NativeProgressPhotoGallery.kt`.
- V06–V11: `ui/workout/ActiveWorkoutScreen.kt`, `ui/workout/ActiveWorkoutViewModel.kt`.
- V12–V15: `ui/programs/TrainingProgramsScreen.kt`, `ui/programs/ExerciseLibraryScreen.kt`, `ui/calendar/CalendarScreen.kt`, `ui/programs/ProgramEditorDialog.kt`, `ui/programs/CustomExerciseDialog.kt`.
- V16–V19: `ui/journal/JournalScreen.kt`, `ui/journal/JournalHealthPanel.kt`, `ui/journal/WorkoutRouteMapDialog.kt`, `ui/health/HealthRecordBrowser.kt`, `ui/progress/WeightJournalScreen.kt`, `ui/coach/CoachScreen.kt`.
- V20–V24: `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`.

A rövid utak az android/app/src/main/java/com/repforge/app alatt értendők.
A tervben külön megnevezett WatchSyncSettings/BackupRestore/Permissions/PrivatePhotos
Screen fájlok helyett ezek a jelenlegi Settings meglévő paneljei és callbackjai;
csak vizuális okból nem készül új párhuzamos adatút.
A12 kedvenc-szűrés és csillag művelet a valós ExerciseLibraryScreen/ViewModel-ben
már létezik (`exercise_library_favorite_*`), nem új feature-ként készítendő el.
A07/G03/G05/G06/G07/G12 továbbra célzott feladatok a megfelelő csomagban.
Ez a leltár nem a 2–3. lépés megvalósításának vagy futtatásának igazolása.

## 1. lépés futtatott részeredmény

Exact alkalmazásforrás: 39da06cca96d93586fc1ef00ab8bac2ded0925fb.
Kanonikus workflow: https://github.com/fuloplevente1998/TrainPilot/actions/runs/40481815367
A debug/JVM/R8-release/signer buildjob sikeres. Root letöltötte a
12723443426 artifactot: 166 JUnit XML, **1333 teszt, 0 failure/error/skipped**.
A telefon/Wear/upgrade emulátoros jobok még folyamatban; ez nem teljes kapu PASS.

## 1. lépés lezárt közös kapuja – 2026-10-27

40481815367 exact 39da06cca96d93586fc1ef00ab8bac2ded0925fb: **7/7 job SUCCESS**.
Root JVM XML: **1333/1333, 0 failure/error/skip** (artifact 12723443426).
Root phone XML: **110 main + 4 izolált GPS = 114/114, 0 failure/error/skip**
(artifact 12723743019). Mindhárom korábbi regresszió név szerint PASS a raw XML-ben.
Nincs hiányzó teszt és runner-crash; manifest success, exact SHA, **57 nyers PNG**
bájtméret/SHA-256/PIL PNG-verify ellenőrizve. A három Wear shard és két signed
upgrade job SUCCESS, build/release/signer SUCCESS. Nem készülékes elfogadás.
Ez a futás igazolja a javítási jelöltet, nem bizonyítja a bemeneti dokumentum
minden feltételezett gyökérokát vagy a teljes app végleges vizuális készségét.

## Friss AI Studio-s vizuális mesterterv

Verbatim bemenet: docs/design/NATIVE_VISUAL_MASTER_PLAN_2026-10-27.md.
A main valós képei és a négy mockup együtt a minőségi referencia.
A Health/Progress teljes tartalma, lenyitható részletei és grafikonjai megőrzendők.
A bemeneti kódblokk API-eltérései továbbra fennállnak; az eltérő fájlcserék nem
kerülnek a zöld baseline helyére. A fájl tetején lévő 67f2889f piros státusz történeti.
A sárga CTA sötét felirata az existing onAccent/WCAG tokenből jön, nem új
luminance-threshold vagy minden palettára beégetett arany/fekete színpárból.
A példaszámok és mockup-óramodell nem élő szolgáltatói adatok.

Következő csomag: 2. lépés / P1, V03 JournalScreen, V04 HealthDashboardScreen,
V05 ProgressScreen. A main lenyitható Health/Progress panelekhez hiányzó
read-model/interaction kötések külön követendők; puszta Card-csere nem teljesítés.
P2–P5 még nem kezdődött; csomaghatáron közös ellenőrzés szükséges.
