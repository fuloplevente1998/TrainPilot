# Wave 1 / Chat 3 — Az öt fennmaradt telefonos UI-eset tényleges lezárása

**Saját ág javaslat:** `fix/native-phone-five-failures-wave1` a rögzített migration HEAD-ből.  
**Saját report:** `docs/reviews/NATIVE_PHONE_FIVE_FAILURES_WAVE1.md`.  
**Baseline bizonyíték:** PR #238 szerint run [40372076529](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40372076529) exact `1d2be2a5e19f8f458280c47451da313ccb196358`: 104/104 telefonos terminális, **99 PASS / 5 FAIL**, 47 nyers PNG; a `162746c7` Home-root navigációs javítás még NEM része ennek. **Ez nem a jelenlegi HEAD runja.**

## Feladat

A régi 5 konkrét hiba újrafuttatható reprodukciója és javítása a mai integrált HEAD-en; ne tételezd fel, hogy mind az öt továbbra is jelen van. Minden eltérést tesztnév + raw failure/log + valódi UI-végállapot alapján osztályozz.

Öt hibacsoport (a raw logból rendeld hozzá a konkrét assertionöket):
1. Intervallum UI #1 — `interval_rest` / mező elérhetősége és a tartós preset;
2. Intervallum UI #2 — `interval_auto_advance` / globális vs workout-scoped állapot;
3. Egyéni gyakorlat törlése — `NativePhoneNavigationTest#customExerciseCanBeCreatedEditedAndDeletedWithoutStartingWorkout`;
4. Settings és/vagy editor elhagyása — valódi back/save/discard/lifecycle és futó edzés megőrzése;
5. Öt főfül + secondary navigáció — `NativePhoneNavigationTest#fiveTabsAndSecondaryPlanningEntriesUseActualNativeScreens`, restore-backstack és képernyőütközés.

Nézd a `NativePhoneNavigationTest#intervalConfigurationIsWorkoutScopedUntilGlobalDefaultIsExplicitlyChosen` és `#settingsThemeAndLanguageChangesReturnToTheirOrigin` regressziókat is. Ha a raw log más konkrét 5 failure-nevet mutat, **a raw a mérvadó**, ne tartsd fenn az elavult osztályozást.

## Pontos írható határ (kizárólag a reprodukált ok által indokolt rész)

- `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt`, `ActiveWorkoutViewModel.kt` (csak UI-állapot/callback; interval/writer engine read-only)
- `android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt`
- `android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt`, `ExerciseLibraryViewModel.kt`, `CustomExerciseDialog.kt` (custom delete UI/callback/fixture)
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt` és saját új `NativePhoneFiveFailures*Test.kt`
- `scripts/native/phone-ui-gate.py`, `scripts/native/evidence-summary.py` és meglévő parser-unit tesztjeik kizárólag igazolt evidenciahiba esetén
- saját report és saját ági QA-only workflow.

**Szigorúan read-only:** `ui/home/**`, `ui/progress/**`, `ui/programs/TrainingProgramsScreen.kt`, `TrainingProgramsViewModel.kt`, `NativeProgramActions.kt`, `NativeProgramEditor.kt`, A07 factory/catalog, Coach/Health/Room/DAO, native core interval engine, backup/sync/Wear, közös erőforrás-katalógus. Így Chat 1/2/4/5 production fájljával nincs átírási jogütközés.

## Végrehajtás és elfogadás

1. Szerezd le a fenti run teljes host `instrumentation`/evidence artifactjait, keresd vissza mind az 5 failure pontos test ID-ját és a dokumentált log végállapotot. Nem elég a PR címkéje.
2. A jelenlegi saját HEAD-en célzott reproduce; ha egy eset már zöld, rögzítsd a reprodukciót és a javító őskommitot, ne módosítsd fölöslegesen a code-pathot.
3. Javíts productot ott, ahol rossz; teszt fixture/selector csak bizonyított rossz várakozáskor változhat. `performScrollToNode` és tényleges Compose hierarchy is vizsgálandó; nincs sleep-spam.
4. A releváns egyedi esetek + nearby regressziók API35 futtatása, aztán **minden telefonos suite a saját végső SHA-n**, a 4 izolált GPS permission tesztet is számolva; az összes deklarált/terminális szám egyezzen, skip/missing/crash = 0. Debug, releváns JVM és UI-test APK build zöld.
5. Valós `adb exec-out screencap -p` képek az 5 javított útvonalról, nyers UI log + JUnit XML + SHA manifest. Ha a teljes phone kaput másik Root futás fogja később validálni, a saját teljes phone gate ettől még **nem váltható ki** meg nem futott PASS-szal.
6. A legacy UI a G01/G10 csomaggal a migráción **még nincs egyesítve**; e saját zöld után Root közös kapu kötelező a transzplantált G01/G10 kóddal.

## Közös munkaszerződés — kötelező

- Repo: `fuloplevente1998/TrainPilot`. **Induló, rögzített migration HEAD:** `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; a munka megkezdése előtt újraolvasandó a tényleges HEAD. A vizsgált kód és bizonyíték SHA-ja végig külön szerepeljen. **Main read-only**; a migration ág is read-only ezen chat számára.
- Olvasd el: `AGENTS.md`, `docs/NATIVE_RESUME.md` eleje, PR #238, `docs/design/final-ui-ux/04-implementation-and-acceptance.md` releváns része és az itt megadott konkrét források. Régi státuszszöveg vagy régi SHA tesztje nem bizonyít mai PASS-t.
- Egyedi saját ág; draft PR célja `feat/native-android-migration`. **Nincs automatikus merge, PR-/issue-zárás, main push, migration push, force push, rebase a másik chat ágára, csomag-/verzióemelés vagy aláírási módosítás.**
- Nincs új belső subagent/másik Codex indítása. Szomszédos chat fájljainak módosítása tiltott. Fájlhatáron túli akadályt pontos útvonal/sor, javasolt patch és felelős megjelölésével add át, ne lépj át a másik tulajdonába.
- Nem kerülhet be tesztkihagyás, `@Ignore`, assertion-/selector-gyengítés, no-op callback, hamis PASS, fake provideradat vagy csak timeout-emelésből álló javítás. A korábbi működő funkciókat meg kell tartani.
- A saját ágon szükség esetén a **már regisztrált** `.github/workflows/native-android.yml` manuális dispatch és csak saját ági *QA-only* átírás megengedett. Ezt külön változásként jelöld: **nem integrálandó** a migration/main workflow-ba. Kizárólag izolált API35 emulátor/szintetikus fixture, nem éles egészség- vagy felhasználói adat.
- Átadáskor: own branch, kezdő és végső commit SHA, változtatott fájlok, célzott parancsok/osztályok, tényleges GitHub Actions run+job+artifact link, raw log/JUnit XML darabszám (pass/fail/skip/missing/crash), APK/PNG SHA256, korlátok, draft PR URL. Ami nem futott, azt **NOT_RUN**-nak jelöld. Ne állíts összevont zöldet saját scoped zöld alapján.
