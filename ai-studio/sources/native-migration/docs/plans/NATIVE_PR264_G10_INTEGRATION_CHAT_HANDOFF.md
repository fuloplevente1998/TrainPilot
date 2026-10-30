# Wave 1 / Chat 2 — PR #264 G10 Progress integrációs review és saját, zöldre vihető előkészítő ág

**Kiinduló PR:** https://github.com/fuloplevente1998/TrainPilot/pull/264  
**Forrás PR head (ellenőrzött 2026-10-26):** `0ad6698641b2717973febf42c1b70740b4b0ee7d`; még draft, nincs merge-elve.  
**Saját ág javaslat:** `review/native-g10-progress-integration-ready` a rögzített migration HEAD-ből.  
**Saját report:** `docs/reviews/NATIVE_G10_PROGRESS_INTEGRATION_REVIEW.md`.

## Feladat

Végezd el a PR #264 pontos integrációs review-ját, majd saját ágon a jelenlegi migration HEAD-re transzplantáld a **csak G10-re** vonatkozó kódot/tesztet/resource-ot; szükség esetén javítsd a rebase nélkül feltárt különbségeket. A meglévő G10 implementációt ne írd újra és ne próbáld általános P1 Progress redesignná bővíteni.

A PR bizonyítéka: run [40389898998](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40389898998) az előzőleg tesztelt pontos forráson; 12/12 JVM és 6/6 API35 UI, 5 hash-ellenőrzött módosítatlan PNG. A régi Node/npm workflow-fail nem G10 product failure; a #265 routing audit másik feladat. **A mostani integrációs SHA új scoped kaput igényel.**

## Pontos engedélyezett határ

- `android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt`, `ProgressScreen.kt`, `NativeProgressQuery.kt`, `NativeProgressPhotoGallery.kt`, `NativeStrengthRange.kt`
- `android/app/src/main/res/values{,-en,-de,-ro}/native_progress_photo_range.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgressPhotoRangeUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeProgressPhotoRangeTest.kt`, `NativeProgressQueryTest.kt`; `NativeCardioProgressUiTest` csak olvasás/tesztfuttatás
- A saját `docs/reviews/NATIVE_G10_PROGRESS_INTEGRATION_REVIEW.md` és opcionális saját **QA-only** workflow.

**Read-only:** `ui/home/**`, `ui/workout/**`, `ui/navigation/**`, `ui/programs/**`, közös `NativePhoneNavigationTest`, Room/DAO/schema, private-photo writer/`WorkoutPhotoRepository`, Health/Weight, backup/Drive/sync, canonical workflow.

## Kötelező review és kapu

1. PR #264 fájlonkénti review, migration HEAD-delta és conflict-ellenőrzés. A branch `.github/workflows/native-android.yml` scoped változása **tiltott integrációs fájl**.
2. Strength 30/90 naptári nap / 1 év / minden időszak az **erőedzés adatait** szűrje, ne a kardió/testsúly szűrőt címkézze át. Helyi dátumhatár + DST, jövőbeli history elutasítás, per-exercise 256-os keyset bounded olvasás, kanonikus 1RM-képlet változatlan.
3. Fotók csak élő workout-fotó metadata által hitelesített privát ID-kből: orphan/tombstone/missing/corrupt állapot, bounded thumbnail decode, részlet modal bezárás; nincs directory-scan, export, írás vagy szintetikus „előtte/utána” besorolás.
4. Room invalidation és gyors rangecsere/gyakorlatcsere request revision ne engedjen stale eredményt a képernyőre. Nem léphet fel extra all-history hydrate.
5. Célzott JVM: `NativeProgressPhotoRangeTest`, `NativeProgressQueryTest`. API35 UI: `NativeProgressPhotoRangeUiTest`, `NativeCardioProgressUiTest`. Valódi képek: range, üres és feltöltött galéria, nagyított részlet, több téma/locale. Dokumentáld az app/test APK SHA256 és PNG hash kontinuitást.
6. Ugyanazon saját végső forrásról zöld debug/JVM/API35 + raw log/JUnit/artifact; hibákat a saját fájlhatáron belül javítsd, majd ténylegesen futtasd újra.

**Root utómunkája:** a két review-ág G01/G10 integrációjának ellenőrzése, QA-only workflow-k kizárása, majd egyetlen közös migration HEAD kapu. Nincs fizikai Samsung/Google/Watch elfogadás ezen chaten.

## Közös munkaszerződés — kötelező

- Repo: `fuloplevente1998/TrainPilot`. **Induló, rögzített migration HEAD:** `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; a munka megkezdése előtt újraolvasandó a tényleges HEAD. A vizsgált kód és bizonyíték SHA-ja végig külön szerepeljen. **Main read-only**; a migration ág is read-only ezen chat számára.
- Olvasd el: `AGENTS.md`, `docs/NATIVE_RESUME.md` eleje, PR #238, `docs/design/final-ui-ux/04-implementation-and-acceptance.md` releváns része és az itt megadott konkrét források. Régi státuszszöveg vagy régi SHA tesztje nem bizonyít mai PASS-t.
- Egyedi saját ág; draft PR célja `feat/native-android-migration`. **Nincs automatikus merge, PR-/issue-zárás, main push, migration push, force push, rebase a másik chat ágára, csomag-/verzióemelés vagy aláírási módosítás.**
- Nincs új belső subagent/másik Codex indítása. Szomszédos chat fájljainak módosítása tiltott. Fájlhatáron túli akadályt pontos útvonal/sor, javasolt patch és felelős megjelölésével add át, ne lépj át a másik tulajdonába.
- Nem kerülhet be tesztkihagyás, `@Ignore`, assertion-/selector-gyengítés, no-op callback, hamis PASS, fake provideradat vagy csak timeout-emelésből álló javítás. A korábbi működő funkciókat meg kell tartani.
- A saját ágon szükség esetén a **már regisztrált** `.github/workflows/native-android.yml` manuális dispatch és csak saját ági *QA-only* átírás megengedett. Ezt külön változásként jelöld: **nem integrálandó** a migration/main workflow-ba. Kizárólag izolált API35 emulátor/szintetikus fixture, nem éles egészség- vagy felhasználói adat.
- Átadáskor: own branch, kezdő és végső commit SHA, változtatott fájlok, célzott parancsok/osztályok, tényleges GitHub Actions run+job+artifact link, raw log/JUnit XML darabszám (pass/fail/skip/missing/crash), APK/PNG SHA256, korlátok, draft PR URL. Ami nem futott, azt **NOT_RUN**-nak jelöld. Ne állíts összevont zöldet saját scoped zöld alapján.
