# Nyitott kötések, kivitelezési sorrend és elfogadás

> **Aktuális Root pontosítás (2026-10-26):** [UI belépési kapu](../../plans/NATIVE_UI_ENTRY_GATE.md)
> és [korrigált G02–G12 audit](../../reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md)
> az irányadó a történeti baseline mellett. G01/G10 integrált, G06 opcionális online
> map+zoom létező main funkció; nem tisztán új feature. Teljes UI-kivitelezés még nem indult.

## Pontos forráseltérések, nem dekorációval javítandó funkciók

A G01–G12 sorok az exact c116a042 olvasási baseline-ra vonatkoznak. Nem a másik QA-ág mostani állapotát írják le, és nem újabb teljes audit. A phase2 integrált baseline-on mind újraellenőrizendő. G08 kifejezetten megengedett eltérés.

### G01. Home energia-hitelesség/fallback

**Forrás:** [homeenergy](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L117) · [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36)

**Megfigyelés:** HomeViewModel a napi energyScore mezőből közvetlen recoveryText-et képez; nincs ebben az adapterben NativeCoachReadiness hívás vagy source/time kimenet.

**Következő konkrét delta/korlát:** Root adatadapter review; a dizájn nem írhat saját score-képletet. Addig semleges címke, nincs hiteles mai Samsung-badge. A01/V01 és V18 konzisztencia külön kapu.

### G02. Óra akkumulátor és modell

**Forrás:** [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36) · [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62)

**Megfigyelés:** watchBattery nullable, Home adatbetöltés nem ad hozzá értéket; a kártya Watch státuszt kap.

**Következő konkrét delta/korlát:** Nem írható be a mockup92% vagy GalaxyWatch7 mint valós alapérték. Hiányban —/ismeretlen. További adatadapter csak Rootnál.

### G03. Könyvtár részlet → aktív hozzáadás

**Forrás:** [librarydetail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L165) · [library](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L115) · [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105)

**Megfigyelés:** ExerciseLibraryScreen signature csak language/model; részletében nincs onAddToWorkout. Aktív editor saját pickerében addExerciseToWorkout létezik.

**Következő konkrét delta/korlát:** A működő aktív picker marad. Külön új könyvtári Hozzáadás CTA csak Root által adott guardolt entryvel; nem no-op callbackkel.

### G04. Pihenő külön pause

**Forrás:** [rest](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1078) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**Megfigyelés:** RestTimerFloatingBar paraméterei remainingSec/onAdjust/onSkip/canAdjust/language. Nincs rest-pause callback.

**Következő konkrét delta/korlát:** ±15 és Skip változatlanul köthető. Külön pause igényét Root a meglévő natív timer contracton oldja meg; teljes workoutPause nem nevezhető át rest-pause-nak.

### G05. GPS metrikák vizuális bekötése

**Forrás:** [distance](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L808) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**Megfigyelés:** gpsAccuracyMeters és liveHeartRateBpm mező létezik, DistanceMeasurementPanel jelenleg táv/idő/státusz/gomb szintet jelenít meg.

**Következő konkrét delta/korlát:** Phase2 prezentációs bekötés lehetséges az adott fieldből, nulla új writer/számítás. Pontszám/accuracy/forrás csak ha state rendelkezésre áll. Hiányban —.

### G06. Háttértérkép és recenter

**Forrás:** [map](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L48) · [mapcanvas](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L79) · [journal](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L55)

**Megfigyelés:** MercatorRouteCanvas Canvas/Path saját pontokat rajzol; nincs tile provider/zoom/recenter paraméter. Napló summary nem tartalmaz térkép geometryt.

**Következő konkrét delta/korlát:** Nem funkcionális UI-bugként fedjük el: a privát route-canvas kidolgozható; várostérkép, offline tile és recenter új külön funkció lenne. Nincs N+1 listahydrate vagy generált mapbitmap.

### G07. Lezárási form és note

**Forrás:** [finish](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1128) · [finishwriter](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L1005)

**Megfigyelés:** FinishWorkoutDialog az effort választáskor onConfirm-et hív; ViewModel.finishWorkout(effort,onSaved) nem kap note-ot. Parent előbb zárja a modalt.

**Következő konkrét delta/korlát:** Vizuális effort-selection + explicit Save ugyanazon callbackkel megoldható, de valódi note/pain-részlet tartós írása nincs kitéve. Root minimális funkcióhook szükséges; sikertelen mentéshez draftmaradás tesztelendő, nem új finalizer.

### G08. Naplórészlet gear – megengedett kivétel

**Forrás:** [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**Megfigyelés:** A journal részlet külön Window. NATIVE_MOCKUP_REVIEW szerint beágyazott dialógusra nem kell új fogaskerék.

**Következő konkrét delta/korlát:** Nem blocker és nem új funkcióhiány. Marad a close/back; a kép gearjét nem erőltetjük új route-hívással a modalba.

### G09. Óra/Engedélyek és inline sync önálló oldalak

**Forrás:** [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92) · [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [calendar](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L28)

**Megfigyelés:** A NavHostban nincs watch_sync/permissions/backup/photos route; a funkciók a meglévő Settings/Health/aktív panelen elérhetők. CalendarScreen nem kap onSync-et.

**Következő konkrét delta/korlát:** Kevesebb oldal megengedett: a24nézet lehet ugyanazon panel. Új nav entryt csak Root adhat meglévő navigationAllowed/backstack contracttal. Nincs mockupból kitalált új üzleti API.

### G10. Fejlődés fotó-entry és strength időszak

**Forrás:** [progressstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L40) · [progress](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt#L61)

**Megfigyelés:** progressPhotos a VM-ben betöltött lista, ProgressScreen nem rendereli; a strength state-hez nincs időszakválasztó contract. Kardió és testsúly időszakai külön meglévő utak.

**Következő konkrét delta/korlát:** Meglévő súly/kardió queryt nem cimkézzük strength időszakszűrésnek. Fotógaléria/entry és szükséges filter Root bekötés; addig nincs placebo fül/gomb.

### G11. Mentéslista a mockupon

**Forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [recovery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt#L19)

**Megfigyelés:** SettingsUiState nem ad fájl/remote-backup listát; SAF választók és explicit recovery elérés van.

**Következő konkrét delta/korlát:** Formátumkártya a meglévő launchert hívhatja. A képen lévő fájlméret/dátum/előnézet-lista nem megvalósított API. A data-integrity QA jövőbeli eredményét nem előlegezzük meg.

### G12. Aktív fotó thumbnail és célméret

**Forrás:** [activephotos](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L418) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257)

**Megfigyelés:** AttachedPhotos sor Boxot és24dp delete IconButtont tartalmaz; ott nincs bitmap. Journal PrivateJournalPhoto valós privát decode/load/error megjelenítő.

**Következő konkrét delta/korlát:** Phase2-ben átvehető privát megjelenítő és48dp delete cél, a meglévő photo-id writerrel. Új fotómutáció/Drive-policy nem fér dizájnindokba.

## Kötelező, globális G-AC elfogadás (mind a24nézet)

Ezek **következő szakaszban végrehajtandó** próbák, most mind NOT_RUN.

| ID | Feltétel | Elvárt eredmény |
|---|---|---|
| G-AC01 | 320×640dp és360×800dp, font1.0 | Nincs vízszintes oldaloverflow; utolsó művelet felgörgethető; rendszer/bottom/banner nem takarja |
| G-AC02 | font1.5 és2.0, DE/RO hosszú címke, közepes kijelzőn is | Tördelés/one-column fallback, nincs fix42dp/levágott főgomb; minden formmező és footer elérhető |
| G-AC03 | HU/EN/DE/RO, valós program-/gyakorlatnevek | UI label locale szerinti, szám/egység/dátum helyes, useradat nincs lefordítva/módosítva |
| G-AC04 | TalkBack be, kijelölt/futó/hibás/fogyatékos állapot | Logikus cím→adat→művelet sorrend, minimum48dp, ikonok jelentése és selected/disabled állapot; nincs250ms beszédáradat |
| G-AC05 | Mind32paletta választása + kontraszt | Választás persistálódik, derived onAccent/accentText/focus outline; szöveg4.5:1, nagy szöveg/vezérlő3:1; nem csak szín jelöl állapotot |
| G-AC06 | IME és Back, modal/előnézet/Settings roundtrip/rotation | Ugyanaz a selection/draft/scroll és session; cancel nem ír, Settings nem pause; stale callback nem írhat későbbi célra |
| G-AC07 | Üres, betöltő, hiba, offline, permission-deny | Külön, valós state, egyértelmű explicit retry, nincs kitalált mérés/siker vagy felesleges unattended prompt |
| G-AC08 | Futó/paused/Watch-owner/pending transfer/finalization/restore | Mutáció és navigation guard marad; UI-változás nem indít/fejez be órát, GPS-t vagy edzést; régi target elutasított |

A nézetspecifikus bizonyíték a02-screen-contracts minden Vxx fejezetében szerepel.
A globális feltételek nem válthatók ki pusztán egy álló alapképernyővel.

## A meglévő UI-QA munkától függő startfeltétel

Az átadó szó szerint: **„Második szakasz – csak az UI-QA átadása és Root
területfeloldása után”**. A saját docs ágról sem ui/**, sem phoneUItest, sem
screenshot/evidence script, sem workflow nem írható most. A PR261 tényleges
olvasott HEAD835e533f QA-ág volt a vizsgálatkor; ez nem automatikus feloldás.
Root adja meg az új implementációs commitot és az ági frissítés módját. Addig
nincs cherry-pick/rebase/merge a QA-ágra vagy onnan, nincs saját másolt workflow.

## Kivitelezési sorrend a terület felszabadítása UTÁN

Minden csomagnál ugyanaz a minta: előtte állapot/screenshot exact fixture-rel →
csak prezentációs hunk → meglévő üzleti assertionök → új vizuális/a11y próbák →
utána változatlan screencap. A későbbi fájllista **javasolt hunkscope**, nem mostani
írási engedély. ViewModel/Room/motor fájlok csak olvashatók maradnak.

| Csomag | Javasolt képernyő-/resource fájlok | Megőrzendő határ / készültségi kapu |
|---|---|---|
| P0 baseline és közös rendszer | ui/theme/TrainPilotTheme.kt; ui/navigation/NativeMainHeader.kt; új tiszta ui komponensek és saját locale XML, ha Root kijelöli | NativeThemeTokens és palette policyt nem írjuk át vakon; nav guard és5route nem változik;32paletta+4nyelv+small/font kontraszt baseline |
| P1 öt főoldal | ui/home/HomeScreen.kt; ui/programs/TrainingLandingScreen.kt; ui/journal/JournalScreen.kt; ui/progress/ProgressScreen.kt; ui/health/HealthDashboardScreen.kt | Csak card/typography/layout/scroll/semantics. G01/G10 új adatadapter esetén Rootnak külön patchigény, nem saját VM-átírás |
| P2 négy aktív mérési mód | ui/workout/ActiveWorkoutScreen.kt és új tiszta megjelenítők | Renderelt NativeWorkoutTarget/NativeTimerTarget, update250ms köre ne indítson queryt. Stop/auto-complete/rest/ownership/Settings roundtrip megőrzés; G04/G07 domainhook Rootnál |
| P3 lista és szerkesztők | ui/programs/TrainingProgramsScreen.kt; ExerciseLibraryScreen.kt; CustomExerciseDialog.kt; ProgramEditorDialog.kt; ProgramPrescriptionDialog.kt; PersonalProgramPreviewDialog.kt; ui/calendar/CalendarScreen.kt | Cancel/save/restore epoch és request generation nem változik. G03/G09 navigációhook külön Root döntés |
| P4 részletek és mérési előzmények | ui/journal/JournalEditorDialog.kt; JournalHealthPanel.kt; WorkoutRouteMapDialog.kt; ui/health/HealthRecordBrowser.kt; ui/progress/WeightJournalScreen.kt; ui/coach/CoachScreen.kt | Nincs history/photo/route hydrate vagy új metrikaképlet. G06 új map provider kizárt. Forrás/time/alvás/paging megmarad |
| P5 Settings/kapcsolatok | ui/settings/SettingsScreen.kt; NativeThemePicker.kt; NativeLanguagePicker.kt; NativeTimerDefaultsDialog.kt; NativeTrainingModePicker/Comparison.kt; NativeGoogleMaintenanceUi.kt; ReplacementRecoveryPanel.kt; TrainingProfileDialog.kt | Veszélyes művelet scope, Google consent és restore admission változatlan; G09/G11 hiány nem lesz dummy lista |
| P6 összesített vizuális és működési kapu | Root által kijelölt meglévő phoneUI fixture és screenshot runner, új vizuális case-ek | Csak az új baseline CI-szerződésével, nem ebből a docsból kap workflow-engedélyt. Bizonyíték: raw PNG/XML/commit/artifact/hash |

NavHost fájlban legfeljebb Root-reviewed prezentációs slot hunk tervezett
(pl. Home aktív hero). A selectDestination/goBack/navigationAllowed,
openCanonicalWorkout, ownership/restore/start/finish viselkedésének átírása
nem dizájnfeladat. A teljes32katalógus miatt SettingsStore továbbra readonly.
Új callbacktípus vagy új publikus VM szerződés esetén a munka funkcióigényként
visszakerül Roothoz, a meglévő vizuális csomag ettől függetlenül folytatható.

## Reprodukálható vizuális bizonyítékterv (nem mostani futtatás)

- Root kijelöli az **integrált before commitot**, az after commitot és ugyanazon
  tesztfixture adatkészlet azonosítóját/hashét. Model seed/clock/local zone/permission/
  locale/palette/fontScale/device-resolution/SDK egyezés nélkül nincs before-after ítélet.
- Alap: mind24nézet ×4nyelv a gold bemutatópalettával =96 állapotpár. További
 4nem-arany paletta (classicBlue, graphite, vividCyan, red) az5főoldal+4aktív
 módban =36 állapotpár. Ez132 pár (=264 raw PNG), **terv, nem mért futási szám**.
 32paletta teljes kiválasztási/persist/kontraszt tesztje és a picker megfelelően
 scrollozott raw képei külön szükségesek; csak5paletta screenshotja nem bizonyít32funkciót.
- Small/large-font és kritikus state próbák a Vxx elfogadás alapján; a kép mellett
  az üzleti eredmény (sessionazonosság, OK/undo/pause/mérés-stop) külön assert.
  G-AC06/08 nem screenshotból eldönthető. A megerősítési/engedély/restore modalt
  ugyanazon fixture-ben nyitják meg; csak izolált tesztadat és tesztfiók.
- PNG kizárólag `adb exec-out screencap -p`, teljes fájl, SHA256 az eredeti byteokra.
  Nincs crop, kompozit, retus, új szövegréteg, generált after-kép. A négy mostani
  mockup csak referencia, nem before-kép. Screenshot előkészítő helper sem írható most.
- Manifest minimum: viewId/route, fixtureId+hash, sourceCommit, screenshotCommit,
  APK source/version/package/hash, activity, SDK, pixels+density, fontScale, locale,
  zone/time fixture, themeKey, state, original PNG path+SHA256, workflowRun és artifact ID.
- Valódi XML-ben tests/failures/errors/skipped és instrumentáció declared vs terminal,
  process-crash/missing esetek ellenőrzése. Egy zöld job vagy artifactcímke nem elég.
  A meglévő evidence-summary jövőbeli integrált változata a QA-csomag része; most nem másoljuk.
- Elfogadás csak azonosított állapotpáron és futó appon. Ha valamely screenshot hiányzik
  vagy sérült, az a sor NOT_RUN/INCOMPLETE, nem automatikus PASS. APK/készülékes
  elfogadás továbbra külön, nem a jelen dokumentációs csomag eredménye.
