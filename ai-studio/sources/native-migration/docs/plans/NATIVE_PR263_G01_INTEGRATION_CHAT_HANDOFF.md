# Wave 1 / Chat 1 — PR #263 G01 Home integrációs review és saját, zöldre vihető előkészítő ág

**Kiinduló PR:** https://github.com/fuloplevente1998/TrainPilot/pull/263  
**Forrás PR head (ellenőrzött 2026-10-26):** `cd4cedb524507367f994102faeff9e4988d3fcdd`; a PR még draft és nincs merge-elve.  
**Saját ág javaslat:** `review/native-g01-home-integration-ready` a rögzített migration HEAD-ből.  
**Saját report:** `docs/reviews/NATIVE_G01_HOME_INTEGRATION_REVIEW.md`.

## Feladat

Ne új G01 implementációt írj nulláról! Auditáld a PR #263 teljes diffjét az **aktuális migration HEAD** ellen, és készíts a saját ágon ténylegesen integrálható, szükség esetén javított **Home-only** production/test/resource változatot. A PR #263 korábban saját scoped bizonyítékkal zöld; ez nem a migration HEAD-re integrált elfogadás.

A #263 PR által állított elfogadott scoped evidence: run [40395096019](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40395096019), 35/35 releváns JVM, 4/4 új API35 Home UI és meglévő Home-navigációs regresszió, raw PNG/manifest. **Vesd össze a tényleges Actions forrás-SHA-val és artifacttal**, ne csak a PR szövegével.

## Pontos engedélyezett határ

- `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt`
- `android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt`
- `android/app/src/main/res/values{,-en,-de,-ro}/native_home_recovery.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHomeRecoveryUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeHomeRecoveryPresentationTest.kt`, kizárólag indokolt kiegészítés a meglévő `NativeHomeDailyCountTest`/`NativeHomeRepositoryTest` tesztekhez
- A saját `docs/reviews/NATIVE_G01_HOME_INTEGRATION_REVIEW.md` és opcionális, **nem átveendő** saját ági QA workflow.

**Read-only:** `ui/progress/**`, `ui/workout/**`, `ui/navigation/**`, `ui/programs/**`, `NativePhoneNavigationTest.kt`, Coach engine, `HealthJournalStore`, Samsung/HC adapter, Room/DAO/schema, global resources, main/migration canonical workflow. A végleges P0/P1 grafit/arany design nem feladat.

## Kötelező review és implementációs kapu

1. Ellenőrizd a migration HEAD és PR #263 valódi fájldeltáját; a branch saját scoped QA workflow-fájlját **ne másold át**. Ha a Home-kód időközben módosult, készíts soronkénti 3-way review-t és csak az ütközést javítsd.
2. Kövesd az energia forrását: mai hiteles Samsung Energy Score = **eredeti 0–100 érték** és tényleges source/idő, nem Coach terheléssel büntetett readiness. Samsung hiányában csak valós TrainPilot-becslés, adat híján `—`. `partial`, `stale`, `error` külön állapot.
3. A PR ismert korlátja: a megosztott readiness validátor a 0-t elutasítja; a PR csak Home-szintű kompatibilitási megoldást tartalmaz. Ellenőrizd, hogy nem szivárog score- vagy adatírási változtatás a Coachba; a globális validátor javítására csak külön Root patch-javaslat.
4. Figyeld a 14 napos korlátot, IO-dispatchert, nap/zóna/language/restore változást, cancellációt, régi load admission tiltását és a 0/100/NaN határokat. Nincs SDK-közvetlen új poll.
5. Célzott JVM: `NativeHomeRecoveryPresentationTest`, `NativeHomeDailyCountTest`, `NativeHomeRepositoryTest`, `NativeCoachReadinessTest`, `NativeCoachSignalsTest`. API35: `NativeHomeRecoveryUiTest` és meglévő `NativePhoneNavigationTest#homeTracksPlanSettingsProgramAndHistoryChangesAndStartsTheDisplayedSchedule`. Valós Home screenshot legalább Samsung/TrainPilot/nincs adat/locale állapotban, módosítatlan PNG-k és hash.
6. A munka addig készül, míg ugyanazon saját production/test SHA-n a releváns debug/JVM/UI **zöld**. Ha a PR #263 kódja változatlanul átvehető, a saját ágon akkor is pontos transzplantáció + tesztbizonyíték + review szükséges; a puszta „PR zöld” kevés.

**Root utómunkája:** külön review, a QA-only workflow kizárásával integráció a migrációba; közös telefonos és teljes CI csak az egyesített commiton. Fizikai Samsung SDK provenance külön elfogadás.

## Közös munkaszerződés — kötelező

- Repo: `fuloplevente1998/TrainPilot`. **Induló, rögzített migration HEAD:** `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; a munka megkezdése előtt újraolvasandó a tényleges HEAD. A vizsgált kód és bizonyíték SHA-ja végig külön szerepeljen. **Main read-only**; a migration ág is read-only ezen chat számára.
- Olvasd el: `AGENTS.md`, `docs/NATIVE_RESUME.md` eleje, PR #238, `docs/design/final-ui-ux/04-implementation-and-acceptance.md` releváns része és az itt megadott konkrét források. Régi státuszszöveg vagy régi SHA tesztje nem bizonyít mai PASS-t.
- Egyedi saját ág; draft PR célja `feat/native-android-migration`. **Nincs automatikus merge, PR-/issue-zárás, main push, migration push, force push, rebase a másik chat ágára, csomag-/verzióemelés vagy aláírási módosítás.**
- Nincs új belső subagent/másik Codex indítása. Szomszédos chat fájljainak módosítása tiltott. Fájlhatáron túli akadályt pontos útvonal/sor, javasolt patch és felelős megjelölésével add át, ne lépj át a másik tulajdonába.
- Nem kerülhet be tesztkihagyás, `@Ignore`, assertion-/selector-gyengítés, no-op callback, hamis PASS, fake provideradat vagy csak timeout-emelésből álló javítás. A korábbi működő funkciókat meg kell tartani.
- A saját ágon szükség esetén a **már regisztrált** `.github/workflows/native-android.yml` manuális dispatch és csak saját ági *QA-only* átírás megengedett. Ezt külön változásként jelöld: **nem integrálandó** a migration/main workflow-ba. Kizárólag izolált API35 emulátor/szintetikus fixture, nem éles egészség- vagy felhasználói adat.
- Átadáskor: own branch, kezdő és végső commit SHA, változtatott fájlok, célzott parancsok/osztályok, tényleges GitHub Actions run+job+artifact link, raw log/JUnit XML darabszám (pass/fail/skip/missing/crash), APK/PNG SHA256, korlátok, draft PR URL. Ami nem futott, azt **NOT_RUN**-nak jelöld. Ne állíts összevont zöldet saját scoped zöld alapján.
