# NATIVE G01 Home integrációs review

## Scope és exact források

- Repo: `fuloplevente1998/TrainPilot`
- Saját ág: `review/native-g01-home-integration-ready`
- Rögzített és induláskor újraellenőrzött migration HEAD: `a075a21956ea4bd2f1089a9bc106dbe7a598c264`
- Vizsgált PR: #263, accepted head: `cd4cedb524507367f994102faeff9e4988d3fcdd`
- #263 merge-base / eredeti handoff baseline: `30e087646298fb0191f5ffa0e7e157b14519d669`
- Saját production/test SHA: `412ccc1c1f0ae235e3a183bbe8c9f4c1e1587ba8`
- Main és `feat/native-android-migration` read-only maradt; nincs merge, force push, verzió-, aláírás-, schema- vagy csomagmódosítás.

## 3-way review és fájlhatár

A migration HEAD és a #263 eredeti baseline között a két meglévő Home production fájl nem változott:

- `HomeScreen.kt`: baseline blob = migration blob = `1c44c1cb4db8271edd888f4cbe7b4e7c9ac16117`; #263 blob = `49e0e002f1014dd17c1c315909bdd2c0facb7d25`.
- `HomeViewModel.kt`: baseline blob = migration blob = `22ca6f2919e77e248cb678bb596ab127c10c06cb`; #263 blob = `6a2b5d1645bd5a3f512187f649edc43439c2bd95`.

Ezért Home-szintű konfliktus nincs, a #263 production/test/resource fájlok pontosan transzplantálhatók. A #263 saját `.github/workflows/native-android.yml` fájlja és `docs/reviews/NATIVE_HOME_RECOVERY_PROVENANCE_HANDOFF.md` reportja **nem lett transzplantálva**. A saját ágon külön, QA-only workflow készült kizárólag a kötelező scoped kapu futtatásához; ezt Root nem integrálhatja migration/main workflow-ba.

A saját production/test commit eltérései kizárólag:
- `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt`
- `android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt`
- `android/app/src/main/res/values{,-en,-de,-ro}/native_home_recovery.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHomeRecoveryUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeHomeRecoveryPresentationTest.kt`
- saját QA-only `.github/workflows/native-android.yml`

Read-only szomszédos területhez nem történt írás.

## Forrás/provenance és viselkedési audit

- A Home lokális `HealthJournalStore.page(..., "journal", ...)` projectionből olvas; nincs új Samsung/HC SDK poll.
- A Room terhelési lekérdezés `getCoachRecentHistory(fromEpoch,toEpoch)` időablakkal és SQL `LIMIT 3` korláttal fut; a `NativeCoachReadiness` ezen felül 14 napra szűr.
- Minden journal/Room/JSON/scoring munka `Dispatchers.IO` alatt történik.
- Samsung esetben a Home a validált `readiness.samsungScore` eredeti 0–100 értékét és `samsungTime` idejét jeleníti meg; a load/pain által korlátozott Coach `score` nincs kijelezve Home energiaként.
- Samsung hiányában csak a meglévő `trainpilotScore` jelenik meg, ha van tényleges observation; adat nélkül `—`.
- `partial`, `stale` és `refreshFailed` külön állapotok; source/time/calculated-at külön UI detail.
- A globális `NativeCoachReadiness.validEnergy` jelenleg `value <= 0` miatt elutasítja a 0-t. A Home kompatibilitási út csak egy defensív JSON-másolatban 1.0-s probe-bal futtatja ugyanazt a source/time/today/36h validátort, majd kizárólag az eredeti, tényleges 0-t mutatja. Nincs journal/Coach adatírás.
- Javasolt külön Root patch: `NativeCoachReadiness.kt` validátorban `value <= 0` → `value < 0`, shared zero-boundary teszttel; ez a G01 integrációban szándékosan nincs módosítva.
- Régi load eredménye `NativeHomeRecoveryLoadGate` generation miatt nem publikálható; az előző coroutine cancel megtörténik és `CancellationException` újradobódik.
- `NativeDataReplacement.continuationForCurrentData()` + `requireCurrent()` megtartja a restore/erase admissiont.
- Date/time/timezone broadcast és lifecycle resume újratölt; a settings flow language/active-program változásra újratölt.
- 0/100 boundary, NaN/Infinity/out-of-range, invalid source, hiányzó/future/stale timestamp, day/zone váltás tesztelve.
- Coach engine, validator, HealthJournalStore, Samsung/HC adapter, Room/DAO/schema, navigation és globális resource fájlok változatlanok.

## #263 korábbi scoped evidence független ellenőrzése

A PR-szövegtől függetlenül ellenőrzött run: [40395096019](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40395096019).

- Tényleges Actions source SHA: `cd4cedb524507367f994102faeff9e4988d3fcdd` — megegyezik az accepted PR head-del.
- Két job: build/JVM SUCCESS, API35 Home UI SUCCESS.
- Build/JVM artifact #12686102957 ZIP SHA256: `a6f5f47f9c4b5c702c900873f4d2479f130beb8357be0fa04452ece5bf832c0c`.
- APK artifact #12685333137 ZIP SHA256: `25ebb511cf042248c678b821bf1fc31bfc4ff5b2ceea68bc13e927965eb97a02`.
- UI artifact #12687046313 ZIP SHA256: `217f36ecca0e846226190935346016b438e6f3ee7771ace08af9be615d16f2ac`.
- Öt valódi JVM XML: 35 tests / 0 failures / 0 errors / 0 skipped.
- UI JUnit: 4 tests / 0 failures / 0 errors / 0 skipped; meglévő Home navigation regression raw terminal PASS.
- Eredeti APK SHA256: app-debug `cf5f2529aa45f4f280ecf8fd50b06b3a0d29f05c9dc5f225397aa15dea2160c8`; androidTest `96f46cf500b440a79430d9c5b31819726bf94244d7590a7f2f120ef72c10479d`.
- 7 Home state/locale PNG + 2 host checkpoint PNG volt az artifactban.

## Saját exact QA — GREEN

Run: [40404104475](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40404104475), exact source `412ccc1c1f0ae235e3a183bbe8c9f4c1e1587ba8`.

### Job 1 — G01 debug APK and targeted JVM

Job id: `121857293164`, **SUCCESS**.

Célzott osztályok:
- `NativeHomeRecoveryPresentationTest`: 10/10 PASS
- `NativeHomeDailyCountTest`: 3/3 PASS
- `NativeHomeRepositoryTest`: 5/5 PASS
- `NativeCoachReadinessTest`: 11/11 PASS
- `NativeCoachSignalsTest`: 6/6 PASS

Összesen: **35 tests, 35 PASS, 0 failure, 0 error, 0 skipped, 0 missing/crash**.

Artifactok:
- [Build/JVM #12691550860](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40404104475/artifacts/12691550860), ZIP SHA256 `cb3402ffbd2659f979d13ded91f3851c080c9a3c29789541367074afeac00fa6`.
- [APK #12691481002](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40404104475/artifacts/12691481002), ZIP SHA256 `6de2b29595134db01166a62c3e0e628c975165aba293c6908917c75e95b0202d`.
- app-debug.apk SHA256: `3d8661911417d69b09e2eb21c356644f472d73548a8bb5e3903c1fd9c70c0095`.
- app-debug-androidTest.apk SHA256: `1a13b6e531fa19cb9d6175095ff0e27f6590c37e876129d96b8e0c9dd07ca37b`.

### Job 2 — API35 G01 Home UI and navigation regression

Job id: `121860831709`, **SUCCESS**.

- `NativeHomeRecoveryUiTest`: **4/4 PASS**, 0 failure/error/skip.
- `NativePhoneNavigationTest#homeTracksPlanSettingsProgramAndHistoryChangesAndStartsTheDisplayedSchedule`: **1/1 PASS**.
- Mindkét instrumentáció: `INSTRUMENTATION_CODE: -1`, adb exit 0.
- Missing terminal case: 0; process crash: 0.
- 7 nyers Home screenshot + 2 nyers host checkpoint screenshot.

[UI evidence #12691561459](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40404104475/artifacts/12691561459), ZIP SHA256 `6e880eb565ab5d7bebff2377469682bd60c18cb354298da49f0430a26ae369da`.

Nyers Home PNG SHA256:
- Samsung: `1a53beac0a61d3f03e8a0f6bfd067ee1ae0efca7a02479711d9a2fb3047782c5`
- TrainPilot fallback: `79e6fe835a21f48fdf2943cba92538240861d5c361b3eeddce42976e65a15eb6`
- no-data: `d97dc7242f5dbbd03d395df51e02d91def7b1e2855bd337a22be41d5d68e27a2`
- HU: `5d08f1490928d05713086fd9b311e6d830b0bba0ba55cc78d324748ad4fd0c66`
- EN: `0a6ce41e28d3fae1be73e4a04b92a1e4ca84ab0abc6afcde172430ced8f93bfb`
- DE: `e4ab991965e86822ad5d593d27e233f666ed535b2dcb3dbc4ac49eab569ae83e`
- RO: `58da3253510c6666fc7f23cb3c604dcbc8361bacdae87bbbc214fe828dbcb889`

Host checkpoint PNG SHA256:
- Home scheduled target: `831b588c35a7bfeed9914d1f7d3871bba198f470be157559960974fefc695586`
- Active strength: `0284c16b21a1e0c67e18a26ec0ea22891e7b49514552d8af292f3d35aabb35c3`

A branch push által indított külön Quick TrainPilot Validation run `40404104441` is SUCCESS, de ez nem helyettesíti és nem bővíti a fenti scoped G01 elfogadást.

## Korlátok / NOT_RUN

- Fizikai Samsung SDK source-acquisition/provenance: **NOT_RUN**; az API35 teszt szintetikus journal fixture, nem készülékes Samsung SDK bizonyíték.
- Teljes integrált migration phone/Wear/full CI: **NOT_RUN ezen a saját ágon**; Root feladata az egyesített commiton.
- Végleges P0/P1 grafit/arany vizuális acceptance: **NOT_RUN / out of scope**.
- Global shared validator zero-fix: **NOT_APPLIED**, külön Root patch-javaslat.
- Saját QA-only workflow: **nem integrálandó** migration/main ágba.

## Review döntés

A #263 Home production/test/resource deltája a jelenlegi migration HEAD-del Home-szinten konfliktusmentes, a forrás/provenance/admission követelményeket megtartja, és ugyanazon saját production/test SHA-n a kötelező scoped JVM + API35 Home UI + meglévő Home navigation regresszió zöld. Root integrációra alkalmas a QA-only workflow kizárásával; közös/full és fizikai Samsung elfogadás továbbra külön kapu.
