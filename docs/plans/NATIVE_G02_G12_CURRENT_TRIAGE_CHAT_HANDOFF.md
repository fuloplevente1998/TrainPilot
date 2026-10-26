# Wave 1 / Chat 5 — G02–G12 exact baseline triage, kizárólag audit/docs

**Saját ág javaslat:** `audit/native-g02-g12-head-triage` a rögzített migration HEAD-ből.  
**Egyetlen írható fájl:** `docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md`.  
**Nincs production, tests, fixture, workflow, AGENTS, NATIVE_RESUME vagy design spec írás. NINCS CI/API35 indítás ezen chaten.**

## Cél

A `docs/design/final-ui-ux/04-implementation-and-acceptance.md` G02–G12 megfigyelései korábbi `c116a042` forrásra szóltak. Minden pontot újra kell ellenőrizni a jelenlegi migration exact `a075a21956ea4bd2f1089a9bc106dbe7a598c264` forrásán, összevetve PR #238 állításaival és a már külön végzett #263 (G01) és #264 (G10) munkával. **11 sor nem 11 automatikus hiba.** Ne másold a régi táblát friss ellenőrzés nélkül.

## Kötelező triage-mezők minden sorhoz

`ID | main-látható funkció és exact callsite | migration exact forrás útvonal/sor | UI→callback→VM→engine/DAO lánc | reprodukció/teszt/létező evidence SHA | státusz | valós hiány következménye | prior | javasolt patch owner és írható fájlok | szükséges teszt`.

Státusz kizárólag **már javított / valós kötelező parity bug / elfogadott eltérés / külön új feature / bizonyíték hiányzik / integrációra váró PR**; unknown nem kap automatikus zöld státuszt. A main exact referencia `7473b30f387eda54186c1e31830304ca12bd104f`; élő felhasználói callsite-ot vizsgálj, ne csak a mockup szövegét.

## Vizsgálandó pontos tételek

- **G02** Home Watch akkumulátor/modell: valós adatforrás/nullable; mockup 92% nem adat.
- **G03** Exercise Library részlet → élő edzés hozzáadás: tényleges guard és callback, nem no-op; Active Workout és P3 tulajdonosi ütközés miatt későbbi külön implementation-chat.
- **G04** rest-specific pause vs workout-wide pause; `±15`/Skip marad.
- **G05** `gpsAccuracyMeters` és `liveHeartRateBpm` tényleges UI-olvasás és forrás/hiány.
- **G06** route-background city map/zoom/recenter: tipikusan **új scope**, nem P0–P6 blocker; a meglévő privát route-canvas ellenőrzendő.
- **G07** explicit finish save, note/pain, sikertelen mentés draft marad; valódi mentési contract.
- **G08** naplórészlet modal gear: **elfogadott eltérés** a jelenlegi spec szerint, ellenőrizd, hogy változott-e.
- **G09** Watch/Permissions/backup/photos standalone route: nem kötelező, ha funkciók már elérhetők a jelenlegi panelről.
- **G10** Progress: PR #264 saját head `0ad6698641b2717973febf42c1b70740b4b0ee7d` scoped green, de migrationba **még nem integrálva**; jelöld külön PR-várakozásként, ne duplikáld.
- **G11** backup listing és dátum/fájlméret: mockup-lista jelenlegi API nélkül nem kötelező; ne alkoss hamis preview API-t.
- **G12** aktív workout privát photo thumbnail, hiány/korrupt és 48dp delete target; különítsd el Progress galériától.

## Eredmény

- Egy bizonyíték-alapú, priorizált `P0/P1/P2/accepted/new-feature` backlog, **legfeljebb 3–4 egymástól diszjunkt végrehajtható csomaggal**, pontos írható fájlhatárral.
- Jelöld az Active Workout kombinálható G04/G05/G07/G12 csomagot és a G03 guardolt Library→workout integráció külön kockázatát. A G01/G10 saját PR zöldje nem migration acceptance.
- Ellenőrizd a migration aktuális HEAD-jét leadáskor is; delta esetén exakt új SHA és csak a releváns sorok célzott újraolvasása. Nem állítható runtime PASS, ha csak source-t olvastál.
- Docs-only `git diff --check` + Markdown/belső link sanity, report, saját [skip ci] commit, draft PR a migrációs ágra. Más PR kommentjét/állapotát ne módosítsd.

## Közös munkaszerződés — kötelező

- Repo: `fuloplevente1998/TrainPilot`. **Induló, rögzített migration HEAD:** `a075a21956ea4bd2f1089a9bc106dbe7a598c264`; a munka megkezdése előtt újraolvasandó a tényleges HEAD. A vizsgált kód és bizonyíték SHA-ja végig külön szerepeljen. **Main read-only**; a migration ág is read-only ezen chat számára.
- Olvasd el: `AGENTS.md`, `docs/NATIVE_RESUME.md` eleje, PR #238, `docs/design/final-ui-ux/04-implementation-and-acceptance.md` releváns része és az itt megadott konkrét források. Régi státuszszöveg vagy régi SHA tesztje nem bizonyít mai PASS-t.
- Egyedi saját ág; draft PR célja `feat/native-android-migration`. **Nincs automatikus merge, PR-/issue-zárás, main push, migration push, force push, rebase a másik chat ágára, csomag-/verzióemelés vagy aláírási módosítás.**
- Nincs új belső subagent/másik Codex indítása. Szomszédos chat fájljainak módosítása tiltott. Fájlhatáron túli akadályt pontos útvonal/sor, javasolt patch és felelős megjelölésével add át, ne lépj át a másik tulajdonába.
- Nem kerülhet be tesztkihagyás, `@Ignore`, assertion-/selector-gyengítés, no-op callback, hamis PASS, fake provideradat vagy csak timeout-emelésből álló javítás. A korábbi működő funkciókat meg kell tartani.
- A saját ágon szükség esetén a **már regisztrált** `.github/workflows/native-android.yml` manuális dispatch és csak saját ági *QA-only* átírás megengedett. Ezt külön változásként jelöld: **nem integrálandó** a migration/main workflow-ba. Kizárólag izolált API35 emulátor/szintetikus fixture, nem éles egészség- vagy felhasználói adat.
- Átadáskor: own branch, kezdő és végső commit SHA, változtatott fájlok, célzott parancsok/osztályok, tényleges GitHub Actions run+job+artifact link, raw log/JUnit XML darabszám (pass/fail/skip/missing/crash), APK/PNG SHA256, korlátok, draft PR URL. Ami nem futott, azt **NOT_RUN**-nak jelöld. Ne állíts összevont zöldet saját scoped zöld alapján.
