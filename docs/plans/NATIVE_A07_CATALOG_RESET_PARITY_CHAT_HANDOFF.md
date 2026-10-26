# Wave 1 / Chat 4 — A07: teljes main catalog és legacy-plan reset szemantikai paritás

**Saját ág javaslat:** `fix/native-a07-catalog-reset-parity` a rögzített migration HEAD-ből.  
**Saját report:** `docs/reviews/NATIVE_A07_CATALOG_RESET_PARITY.md`.

## Pontos probléma

PR #238 szerint a jelenleg zöld szűk A/B day-reset teszt **nem bizonyítja** az A07 teljes main catalog / legacy-plan reset paritását. A natív `NativeProgramActions.kt` jelenlegi kommentje szerint a funkció szándékosan csak `home-basic` A/B napjaira terjed ki és a main globális exercise catalog resetjét **nem** teszi elérhetővé. Ez ténybeli forráseltérés, de önmagában még nem bizonyít, hogy ugyanazt a globális destruktív műveletet ugyanabból a natív képernyőből kell indítani.

## Végrehajtandó main ↔ native audit

1. Main exact referencia: `7473b30f387eda54186c1e31830304ca12bd104f`. Nézd az *élő végső handler/callsite* kódot a `www/index.html`/`www/app.js` és a tényleges rf153/reset/settings/catalog funkciók körül; ne keverd a felülírt vagy dead-code definíciókat az aktív UI-hoz. Készíts műveletenként főkönyvet: indító UI, guard, preview/confirm, mi törlődik/visszaáll, mi marad, cache, beállítások, idempotencia.
2. Native: `NativeProgramActions.preview/restore`, `NativeProgramEditor.save`, `TrainingProgramsViewModel.previewRestoreDay/confirmRestoreDay`, `TrainingProgramsScreen` CTA és `data/db/NativeBuiltinCatalog`; örökölt programok, custom exercises, user módosított programok és újrafelvett importált plan események.
3. Állapotonként döntés: **paritást kötelezően pótló bug / tudatosan eltérő, elfogadható UX / hiányzó explicit public contract / új feature**. Egy jövőbeli veszélyes „reset everything” gomb nem következik pusztán a mockupból. A fő minőségmérce a main valóban elérhető végfelhasználói művelete.
4. Ha az audit konkrét kötelező hiányt igazol, a fájlhatáron belül implementálj helyes, explicit preview+confirm+CAS/continuation/idempotencia szemantikával, megőrizve a felhasználói egyedi adatokat a main tényleges szabályai szerint. Audit nélkül ne legyen automatikus globális katalógus-törlés. Ha DAO/schema/core akadálya van, írás helyett pontos Root patch-javaslat.
5. Esetmátrix: 13 beépített program/106 gyakorlat katalógus, A/B nem módosított és módosított nap, factory vs custom, örökölt/migrált program, favorite, user custom exercise, cancel, stale preview, kétszeri submit, restore közbeni adatcsere, rollback/hiba, aktivált program változatlansága, locale és adatbiztonság. Számot a jelenlegi forrás/kánon alapján ellenőrizd.

## Fájlhatár — Chat 3-tól szétválasztva

**Írható:** 
- `android/app/src/main/java/com/repforge/app/NativeProgramActions.kt`
- `android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt`, `TrainingProgramsViewModel.kt`
- Új `NativeA07CatalogResetPolicy.kt` kizárólag e két helyhez kapcsolt önálló tiszta helperként
- `android/app/src/test/java/com/repforge/app/NativeProgramActionsTest.kt` és új `NativeA07CatalogResetParityTest.kt`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgramActionsUiTest.kt`
- csak új, saját névterű `native_a07_reset.xml` HU/EN/DE/RO resource, saját report, opcionális saját QA-only workflow.

**Read-only:** `NativeProgramEditor.kt`, `NativeBuiltinCatalog.kt`, program/gyakorlat Room/DAO/schema, `NativeDataReplacement.kt`, `ui/programs/ExerciseLibrary*`/`CustomExerciseDialog.kt` (Chat 3), `NativePhoneNavigationTest.kt` (Chat 3), Home/Progress/Workout/Nav/Settings, backup/migrator, Wear, global strings.

## Tesztelési szerződés

- Előbb **source-audit evidence** exact path:line és konkrét call chain, majd megvalósítás csak bizonyított hiányra.
- JVM: `NativeProgramActionsTest`, `NativeProgramEditorTest`, `NativeBuiltinCatalogTest` + új A07 parity/migration/rollback tesztek. API35: `NativeProgramActionsUiTest` összes eredeti esete + új cancel/confirm/stale/migrált-katalógus esetek ugyanazon app/test APK-n.
- Raw JUnit/XML, test totals, valós UI preview/confirm/error screenshot, SHA256 manifest; saját zöld scoped kapu. A teljes user-content restore/migration és signed-upgrade acceptance Root-owned.
- Ha nincs a mai forráson bizonyítható implementálandó eltérés, **NE készíts felesleges kódváltoztatást**: adj kész, bizonyítékos A07 lezáró auditot és futtatott regressziót; a ki nem futtatott kapu nem zöld.

## Közös munkaszerződés — kötelező

- Repo: `fuloplevente1998/TrainPilot`. **Induló, rögzített migration HEAD:** `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; a munka megkezdése előtt újraolvasandó a tényleges HEAD. A vizsgált kód és bizonyíték SHA-ja végig külön szerepeljen. **Main read-only**; a migration ág is read-only ezen chat számára.
- Olvasd el: `AGENTS.md`, `docs/NATIVE_RESUME.md` eleje, PR #238, `docs/design/final-ui-ux/04-implementation-and-acceptance.md` releváns része és az itt megadott konkrét források. Régi státuszszöveg vagy régi SHA tesztje nem bizonyít mai PASS-t.
- Egyedi saját ág; draft PR célja `feat/native-android-migration`. **Nincs automatikus merge, PR-/issue-zárás, main push, migration push, force push, rebase a másik chat ágára, csomag-/verzióemelés vagy aláírási módosítás.**
- Nincs új belső subagent/másik Codex indítása. Szomszédos chat fájljainak módosítása tiltott. Fájlhatáron túli akadályt pontos útvonal/sor, javasolt patch és felelős megjelölésével add át, ne lépj át a másik tulajdonába.
- Nem kerülhet be tesztkihagyás, `@Ignore`, assertion-/selector-gyengítés, no-op callback, hamis PASS, fake provideradat vagy csak timeout-emelésből álló javítás. A korábbi működő funkciókat meg kell tartani.
- A saját ágon szükség esetén a **már regisztrált** `.github/workflows/native-android.yml` manuális dispatch és csak saját ági *QA-only* átírás megengedett. Ezt külön változásként jelöld: **nem integrálandó** a migration/main workflow-ba. Kizárólag izolált API35 emulátor/szintetikus fixture, nem éles egészség- vagy felhasználói adat.
- Átadáskor: own branch, kezdő és végső commit SHA, változtatott fájlok, célzott parancsok/osztályok, tényleges GitHub Actions run+job+artifact link, raw log/JUnit XML darabszám (pass/fail/skip/missing/crash), APK/PNG SHA256, korlátok, draft PR URL. Ami nem futott, azt **NOT_RUN**-nak jelöld. Ne állíts összevont zöldet saját scoped zöld alapján.
