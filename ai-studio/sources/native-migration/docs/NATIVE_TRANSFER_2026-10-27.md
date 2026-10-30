# TrainPilot — önálló fejlesztési átadó

Állapot: 2026-10-27. Ez a dokumentum másik ChatGPT/Codex/Claude/egyéb fejlesztői környezetben, online vagy a mellékelt forráscsomaggal offline is használható. A fennmaradó munka egészét a folytató ügynök viszi; nincs másik modell eredményére váró függőség.

## 1. Azonnali eligazítás

- Repository: https://github.com/fuloplevente1998/TrainPilot
- Aktív ág: **feat/native-android-migration**.
- E dokumentum előtti ellenőrzött branch HEAD: **eb0f73bae73a7815cadfd0f4e4d00f4d04158c78**.
- Utolsó alkalmazáskód-változás: **67f2889fd252f9a413b7b7272716459d2d67ba80**. Az ezutáni, e dokumentumig tartó commitok dokumentációs pontosítások.
- Legutóbbi teljesen zöld P0 alap: **2f7cd835fad7cd3e0d5578da4a4ec65afa130983**.
- Korábbi zöld funkcionális alap: **31dbcde2ce6c69085a227b2b7ff9f12b6c78852b**.
- Main alkalmazásreferencia: **7473b30f387eda54186c1e31830304ca12bd104f**, csak olvasásra.
- Munkahely eddig: `/workspace/TrainPilot-native-migration`; más gépen a klón gyökere az irányadó.
- A natív app **nincs kész**, és a legfrissebb P1 jelölt közös kapuja **piros**. Ne mondd, hogy a mainen vagy a jelenlegi teljes ágon minden kész/zöld.
- Aktuális fókusz a felhasználó utolsó kérései szerint: **a dizájn részletes továbbtervezése a rögzített minőségi mércével**. A P1 kód kísérleti jelölt, nem vizuálisan elfogadott eredmény.

Olvasási sorrend a teljes beszélgetés helyett:
1. Ez az átadó.
2. `AGENTS.md` és `docs/NATIVE_RESUME.md` teteje.
3. `docs/design/NATIVE_DESIGN_HANDOFF.md` — a legfrissebb felhasználói döntések és a teljes funkció-/dizájnleltár.
4. `docs/design/final-ui-ux/01-visual-system.md`, `02-screen-contracts.md`, `03-source-bindings.md`, `04-implementation-and-acceptance.md`, `05-feature-preservation.md`.
5. `docs/plans/NATIVE_FINAL_DELIVERY.md` és a konkrét funkcióhoz tartozó review. A `MIGRATION_STATE.md`, régi batch és handoff alsó részei történeti bejegyzések, nem friss készültségi állítások.

GitHub követés: **draft PR #238** a migráció összesített állapotlapja. A leírása időnként elavulhat: mindig az exact SHA, run, raw XML és a frissebb felhasználói döntés alapján dolgozz.

## 2. Termék és változatlan korlátok

A meglévő WebView/Capacitor TrainPilot teljes telefonos Kotlin/Jetpack Compose migrációját fejezzük be, a natív Wear OS pár megőrzésével. Nem új, csökkentett funkcionalitású appot készítünk.

- Telefon: `:app`, óra: `:wear`, közös: `:shared` az `android` projektben.
- Mindkét eszköz csomagja: **com.repforge.app**.
- Verzió: **1.3.71-native-test / 2736**. Ne emeld önkényesen.
- A működő main release eredeti és gyorsított 1.3.71/2735 változata továbbra is referencia.
- **Main írás/merge, force push, PR/issue automatikus lezárás és éles kiadás nincs engedélyezve.** A migrációs ágon lehet kis, visszakövethető commitokban dolgozni.
- Felhasználói napló, fotó, mérés, terv, beállítás nem törölhető vagy írható felül fejlesztési rövidítésként.
- Meglévő `com.repforge.app` natív blokkokat használd. A régi `com.repforge.HealthConnectManager` prototípust ne kösd be helyettük.
- UI-átalakítás miatt engine/Room/ownership/Health/GPS/backup/finalizer nem írható át. Valódi hiányzó funkcióhoz külön, célzott bekötés és teszt kell.
- A köztes commitok `[skip ci]`-vel készültek, majd egyetlen kézi natív workflow futott. Ne fusson indokolatlanul több azonos teljes gate.
- Ne gyengíts assertiont, ne használj `@Ignore`-t, no-op callbacket, fake szolgáltatói adatot vagy puszta timeout-emelést.
- Válasz magyarul, lényegre törően. A felhasználó kevés felesleges outputot kér. Önállóan haladj az engedélyezett munkával; ne kérj minden aprósághoz megerősítést.
- Modellezési preferencia: vizuális kivitelezéshez 6.1 Medium; funkcióbekötéshez High; komoly CI/debughoz Extra High. Váltási igényt jelezd, tényleges modellváltást nem állíthatsz eszköz nélkül.

## 3. A legfrissebb, kötelező dizájndöntések

**A main teljes funkciókészlete és használhatósága a minimum. A jóváhagyott mockupok vizuális részletességét legalább el kell érni.** A bemutatott köztes natív képet a felhasználó kifejezetten elégtelennek ítélte. Egy színcsere, nagyobb betű és általános kártyák átrendezése nem kész dizájn. A zöld CI sem vizuális elfogadás.

Képernyőnként eltérő elsődleges referencia:
- **Egészség és Fejlődés:** a main teljes bevált felépítése, tartalma és megjelenítési elemei átvehetők Compose-ba; ezeknél nem kötelező a mockup eltérő felosztását erőltetni. A részletes egészség-/testsúlynapló, korábbi mérések, alvás, pulzus, források, dátumok, szűrők, grafikonok és részletbelépők maradnak.
- **Kezdőlap és többi nézet:** a felhasználónak kifejezetten tetszik a mockup-elrendezés. Ez legyen a kompozíció alapja, a main elfogadott elemeinek és funkcióinak beépítésével.
- Nem építjük előbb újra a teljes régi main-elrendezést, majd másodszor át a mockupokra. A közös bevált elemeket vesszük át, és rögtön a véglegesnek szánt képernyőbe illesztjük.
- Öt alsó fül marad: **Kezdőlap / Edzés / Napló / Fejlődés / Egészség**. A Settings fogaskerék főoldalról és aktív edzésből elérhető. Naptár/Coach/Programok meglévő belépői megmaradnak.
- Grafit/arany az elsődleges látványreferencia, **mind a 32 paletta** és HU/EN/DE/RO megmarad. A mentett témát nem állítjuk át aranyra.
- Kis kijelző, 320dp, nagy betű és hosszú DE/RO címkék; 48dp érintési célok, helyes kontraszt, teljes műveleti feliratok.
- A mintaadat és illusztráció nem lesz kitalált élő mérés. Hiányzó pulzus/akku/energia/pontszám esetén őszinte, kidolgozott hiányállapot kell.
- Valódi, változatlan emulátoros PNG-k kellenek. Új generált mockup nem helyettesíti a futó UI ellenőrzését.

A négy kötelező képtábla a repositoryban:
- `docs/design/mockups/01-main-pages-and-planning.png`
- `docs/design/mockups/02-workouts-and-measurement.png`
- `docs/design/mockups/03-journal-maps-and-health.png`
- `docs/design/mockups/04-editing-and-settings.png`

A 24 mintanézet nem funkció- vagy oldalszámkorlát. Részletoldal/dialógus lehet több.

## 4. Mi készült el kódban és ellenőrzésben?

### Funkcionális migráció

A natív motor és számos bekötés már működő, közös zöld alapot kapott. Ne kezdd újra a migrációt és ne feltételezd, hogy csak le nem forduló importhalmaz van:
- Programnapi, quick/outdoor indítás, explicit/megjegyzett GPS-szándék; natív és importált draft.
- Aktív editor, súly/ismétlés/RIR, egyetlen alsó stopper, kétoldali és GPS/kézi mért sorozatok, undo, gyakorlatváltás, mentés/discard/hiba-retry.
- Pihenő Skip/±15mp és már **külön rest-only pause/resume** a meglévő service-en. Ez nem workout-wide pause.
- Natív Naptár és Coach, program-/gyakorlatkönyvtár és szerkesztők, profil/onboarding, progression/training mode utak.
- Bounded/lapozott Napló, teljes keresési/szűrési adatút; Health/sleep/weight rekordok és dátum-/forráskezelés.
- Health foreground/full/incremental refresh; ownership/handoff/offline/reconnect infrastruktúra.
- BackupRestoreManager/ArchiveIO/LegacyWebViewMigrator, JSON/ZIP/fotó/adatcsere-admission és helyreállítási utak.
- Privát Progress-fotógaléria és strength időszakválasztó; hiteles Home recovery-forrás/freshness.
- HU/EN/DE/RO és a teljes palettakatalógus, globális Settings.

Ezek közös automatizált kapuval ellenőrzött alapterületek, **nem a main minden funkciójának vagy minden fizikai integrációnak végső igazolása**. A 6. pont konkrét hiányai továbbra is feladatok.

### P0 vizuális alap — zöld

Commit **2f7cd835**, workflow **40461365542**, mind a 7 job SUCCESS.
- Root letöltött XML: **1333 JVM / 0 failure/error/skip**.
- Telefon: **112/112 UI PASS /0skip**.
- 3 Wear UI-rész és 2 signed upgrade sikeres.
- JVM artifact **12715017976**, telefonos evidence **12715019281**.

Közös források:
- `android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt`
- `.../ui/theme/TrainPilotVisualSystem.kt`
- `.../ui/theme/NativeThemeTokens.kt`
- `.../ui/components/TrainPilotSurfaces.kt`
- `.../ui/navigation/NativeMainHeader.kt`

Közös tipográfiai szerepek, térköz/méret/sarokskála, palette-aware Material error/outline, `TrainPilotSurfaceCard`, `TrainPilotSectionHeader`, `TrainPilotPrimaryButton`; a betűszínek a felületből jönnek. A P0 jó technikai alap, nem a végleges komponens-/dizájnkönyvtár elkészülte.

### P1 Home/Training — kísérleti, nem elfogadott és jelenleg piros

Commitok **6dd51799**, **67f2889f**:
- Home: fő CTA minimum52dp, gyorsindítás előrébb, óraállapot, hat megőrzött mérési kártya; két oszlop, nagy fontnál/keskeny nézetnél egy oszlop; nagyobb értékek/forrás-/hiányjelzések.
- Aktív session esetén Home hero „Folytatás” a meglévő kanonikus editor callbackjével. Nem indít új sessiont.
- Training: Quick előre, közös kártyák/gombok, aktuális program napjai; üres footer csökkentve.
- Két új `NativeHomeMetricLayoutUiTest`; a meglévő Settings/stopwatch teszt Home-Folytatás/azonosworkoutId/screenshot ellenőrzéssel bővült.
- A minőségi felülvizsgálat szerint ez még nem éri el a main/mockup kívánt kidolgozottságát. Nem szabad automatikusan „P1 kész”-nek jelölni vagy változatlan formában továbbmásolni minden oldalra.

## 5. Legújabb CI: konkrét feladat, nem régi zöld állapot

**40464028059**, exact **67f2889fd252f9a413b7b7272716459d2d67ba80**: lezárt **FAIL**, kizárólag a telefonos UI-job hibás.
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40464028059

- Build/JVM/release/signer: SUCCESS. Letöltött XML **1333/0failure/error/skip**; artifact **12715983295**.
- Mindkét signed upgrade és mindhárom Wear UI-job: SUCCESS.
- Telefonos artifact **12716292699**, manifest result `failed`, **56 PNG**.
- Fősor: **110 deklarált, 98 terminális**, 3 hibás; **12 nem jelentett eset**, majd `Process crashed`.
- Külön izolált GPS: **4/4 PASS**.
- Együtt **114 elvárt /102 terminális =99 PASS,3 FAIL,12 MISSING**, plusz runner crash. Nem szabad 102/102 sikernek vagy teljes lefutásnak jelölni.

A három jelentett hiba:
1. `NativeLocalizationLanguageUiTest.trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation`: `navigation_back` hiányzik kattintáskor. Vizsgáld a valódi képernyőt, a görgetés után látható célpontot és a navigation receiptet.
2. `NativePhoneNavigationTest.coachVariationPreviewCancelsWithoutWritingAndSavesOnlyAnExplicitCopy`: timeout, a nyers stackben kb.1276.sor. Vizsgáld a Home/Coach belépőt és a preview tényleges megnyílását.
3. `NativeTrainingModeApprovalUiTest.uiApprovalRejectsProgramRawChangeAfterShownComparison`: Compose `ArrayIndexOutOfBoundsException`, `SlotWriter.moveSlotGapTo/removeSlots/removeGroup`, composition dispose; majd process crash. Vizsgáld a kompozíció/Activity/teszt lifecycle-t, az új layout-teszt hostját is. A gyökérok még nincs bizonyítva.

A Home planning gombjai a P1-ben lejjebb kerültek. Emiatt a görgetés nélküli `performClick()` elavulhatott. Ez konkrét vizsgálandó hipotézis, nem igazolt hibaok vagy engedély az assertionök gyengítésére.

A hibás APK/evidence manifest hashjai:
- app APK SHA256: `9ebe3099a631561d26dca79f503c3fc1a0f4a957fc4bec4d3c892c3c37261cee`
- test APK SHA256: `35edd3aa1a04cd2a0681e4e7d9ab32c25945ea8b76e5473ea2a3b99c7db56fa7`

A stabil visszatekintési pont 2f7cd835. Ne töröld automatikusan a P1 munkát: diffelj, különítsd el az értékes prezentációs részeket, javítsd a regressziót és a vizuális tervet. Ha revert/worktree szükséges, legyen visszakövethető és ne vesszen el a jelölt.

## 6. A még hiányzó funkcionális/bekötési feladatok

A részletes, Root által korrigált main→native audit: `docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md`. Későbbi állapot (pl.G04) elsőbbséget élvez az audit régi snapshotjához képest.

| Tétel | Megmaradt munka és korlát |
|---|---|
| G03 Könyvtár→Quick | A standalone könyvtári részlet Quick indítás/hozzáadás és konfiguráció hiányzó hídja. Aktív editorban már van működő picker. A main végső lánca inline Quick, nem a régi felülírt függvény. Natívan explicit start, draft/active/finishing/Wear/restore/generation guardok kellenek. Meglévő Quick képernyőt és session API-t használd; ne írj második sessionmotort. |
| G04 Rest-only pause | **Kész és a 31dbcde2 zöld futásban ellenőrzött.** Ne valósítsd meg újra. Pause csak rest clock, desired-state idempotencia, stale/owner guard, paused idő nem credit, workoutPaused nem változik. |
| G05 GPS accuracy | A natív állapot tartalmaz pontosságot, az élő pulzus már a fejlécben megjelenik. A megfelelő élő mérési panel pontosság-/no-fix/stale jelzésének rendezése; 0 nem kitalált hiteles fix. |
| G06 Online map+zoom | A main `www/index.html:2118–2132,2297–2317,2335–2400` már tud explicit beleegyezéses online tile/zoom nézetet, offline alapnézettel. Natívan jelenleg privát Canvas útvonal van. A hiányzó opcionális online réteget valódi provider/attribution/consent/no-network-before-consent/failure fallback mellett kell bekötni. Recenter és offline tile letöltés külön igény, nem automatikus scope. |
| G07 Befejezés/Save/note | A motor mentése/draftmaradása létezik. A mockup szerinti effort-kiválasztás utáni explicit Mentés és valódi megjegyzés/pain-részlet tartós bekötése még feladat. A modal ne jelentsen sikert a writer nyugtája előtt; retry/draft/pause/discard megmarad. Ne legyen új finalizer. |
| G12 Aktív privát fotók | Az aktív fotósorban blank Box és24dp delete maradt. Valódi bounded privát thumbnail, missing/corrupt fallback és48dp delete kell. Napló/Progress privát képek már léteznek: azok dekódolását/adatait használd, ne tárolj bitmapot a Bundle-be vagy részleteket tömegesen a listára. |
| A07 Globális defaults-reset | A szűk A/B restore kész. A main globális reset eltávolíthat egyedi gyakorlatokat, miközben programok/kedvencek rájuk hivatkoznak. Ezt nem másoljuk vakon. Külön, explicit adatmegőrzési és preview/confirm/CAS/transaction szabály kell Settings/data oldalon. Lásd `NATIVE_A07_CATALOG_RESET_PARITY.md`. |
| G02 Watch modell/akku | Valós metadataadapter nélkül nincs kitalált Watch7/92%. A meglévő kapcsolatállapot megőrzendő; bővítés csak hiteles forrásból. |
| G08/G09 | Elfogadott eltérések: modalba nem kell erőltetett Settings gear; kevesebb önálló route elfogadható, ha minden funkció elérhető. Ne csinálj felesleges új oldalakat. |
| G10 | Progress photo/range már integrált és ellenőrzött; ne implementáld újra. |
| G11 Backup-lista | Több mentésből álló dátum/méret/preview lista külön új feature. A main utolsó export metaadata nem bizonyít ilyen listát. Ne találj ki elérhető mentéseket. |

További elfogadási elvek:
- Home dinamikus active/importált draft folytatás; meglévő edzés identitása megmarad.
- Pihenő Skip/±15 és pause, gyakorlatváltás, undo; egyszeri alsó stopper és pozitív mérés auto-rögzítése megmarad.
- Kétoldali mérés csak mindkét pozitív oldal után kész.
- „Vázlat” = tartós szünet/kilépés, mellette discard; visszalépés/Settings külön szerződések.
- Samsung mai hiteles energia és TrainPilot becslés/Coach ajánlás külön jelentésű; nincs összegzés vagy új pontszámképlet.
- Nincs élő pulzusforrás → hiányjelzés, nem példa-bpm.
- Térkép piros X csak a térképet zárja.

## 7. A vizuális befejezés tényleges hátralévő részei

A 24 nézet teljes szerződését a design dokumentumokból vedd át. A következő csomagok csak kivitelezési sorrendek, nem funkciótörlési engedélyek:

- **P0:** közös hero/action/metric/status/form/empty/error komponensek teljesítése, egyetlen témarendszer, kontraszt, méretek, ikonok, tipográfia. A jelenlegi három alapkomponens önmagában kevés a mockup részletességéhez. A main bevált Inter fontját és assetlicencét ellenőrizd, ha natívan is átveszed.
- **P1:** öt főoldal. Home/Training jelöltet a valódi tervig kidolgozni; Napló mockup-szerkezet/main funkciók; Egészség/Fejlődés teljes main-referencia. Nem egyszerű gombhalmaz vagy mindenhol ugyanaz az általános kártya.
- **P2:** súlyzós, egyoldali idő, kétoldali idő, GPS mérés és lezárás. UI state/funkcióhook különválasztás, jól látható főművelet, kompakt/olvasható pihenő, minden mérési állapot.
- **P3:** Programok, Könyvtár/részlet, Naptár, szerkesztők. Mentés/mégse/restore/generation, scroll/selection és locale megőrzés.
- **P4:** Naplórészlet/források/fotók/térkép, egészségnapló és testsúlyrészlet, Coach. Meglévő paging/bounded decode/forrásidő és üzleti szabályok változatlanok.
- **P5:** Settings/témák/nyelvek/profil/időzítő/eszközök/szinkron/backup/recovery/engedély/adatvédelem. Veszélyes műveletek scope-ja marad; nincs dummy backup-lista vagy no-op gomb.
- **P6:** teljes vizuális és működési végkapu, több nyelv/paletta/kis képernyő/nagy font, raw screenshot összevetés, végül valós készülékes próbák.

Minden képernyőnél írd le: mit veszünk át a mainből, hogyan illeszkedik a mockuphoz, mi változik a használatban, és mivel ellenőrizzük. Ne készíts új generált látványtervet a megvalósítás helyett.

## 8. Fontos forrásfájlok

Közös állapot és edzés:
- `NativeWorkoutSession.kt`, `NativeWorkoutEditing.kt`, `NativeWorkoutTarget.kt`, `NativeTimerTarget.kt`.
- `PhoneWorkoutEngine.kt` / `PhoneWorkoutExecutionStore.kt`, `ActiveWorkoutStore.kt`, `NativeWorkoutEventMerge.kt`.
- `IntervalTimerService.kt`, `NativeSetStopwatch.kt`, `NativeGpsMeasurement.kt`, `DistanceTrackingService.kt`.
- `ui/workout/ActiveWorkoutScreen.kt`, `ActiveWorkoutViewModel.kt`.
- `ui/navigation/TrainPilotNavigation.kt`, `NativeMainMenu.kt`, `NativeMainHeader.kt`.

Főoldalak és adatok:
- `ui/home/HomeScreen.kt`, `HomeViewModel.kt`, `NativeHomeRecoveryPresentation.kt`, `QuickWorkoutScreen.kt`.
- `ui/programs/TrainingLandingScreen.kt`, `ExerciseLibraryScreen.kt`, `TrainingProgramsScreen.kt` és editorok.
- `ui/health/HealthDashboardScreen.kt`, `HealthDashboardViewModel.kt`, `HealthRecordBrowser.kt` és NativeHealth* szabályok.
- `ui/progress/ProgressScreen.kt`, `ProgressViewModel.kt`, `NativeProgressQuery.kt`, `NativeProgressPhotoGallery.kt`, `NativeStrengthRange.kt`, `WeightJournalScreen.kt`.
- `ui/journal/JournalScreen.kt`, `JournalViewModel.kt`, `JournalEditorDialog.kt`, `JournalHealthPanel.kt`, `WorkoutRouteMapDialog.kt`.
- `ui/coach/NativeCoachReadiness.kt`, `CoachScreen.kt`; Naptár és Settings UI saját alkönyvtárban.
- `data/store/TrainPilotSettingsStore.kt` — 32 paletta, portable settings.

Adatbiztonság: `NativeDataReplacement.kt`, `NativeReplacementJournal.kt`, `data/db/TrainPilotDatabase.kt`, `data/migration/LegacyWebViewMigrator.kt`, `BackupRestoreManager.kt`, `ArchiveIO.kt`, privát fotórepo, Wear szinkron.

A rövid fájlnevek az `android/app/src/main/java/com/repforge/app/` alatt keresendők; némely adat-/backup fájl további alkönyvtárban van. `rg --files`-szal keresd a pontos helyet, ne hozz létre duplikátumot.

A `TrainPilot_Full_Conversation_Export.json` a repositoryban benne van: eredeti Kotlin-blokkok referenciaanyaga. Célzottan keress benne, ne olvasd végig minden induláskor.

## 9. Main-forrás és vizuális referenciák használata

A mainből átvehető: matt felületek, fő/másodlagos/veszélyes gombok, sorszám/kész/jelvény, health és naplókártyák, progress időszakválasztás, térképmód és bezárás, tipográfia. Konkrét mintaleltár a `NATIVE_DESIGN_HANDOFF.md` tetején.

Figyelem: a main `www/app.js` több végső felülírást tartalmaz. Az első azonos nevű függvény nem feltétlenül élő. A `www/index.html` jelentős inline DOM/UI implementációt is tartalmaz: pl. Health-dekorátor, további pulzusnézet, online térkép.

Olvasható app.js forrásindex: `docs/audit-input/main-7473b30/INDEX.md`, `manifest.json`, `app-part-001.txt`…`093.txt`. Ezek együtt a teljes main app.js byteazonos másolatai. Az index eredeti sorszámokkal mutatja a részeket. Online rögzített index:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md

A CSS-cascade a végső stylesheet-sorrenddel és inline felülírásokkal együtt értelmezendő. A main képeit/értékeit ne másold a valódi natív adat helyére.

## 10. Build és tesztelés

A ténylegesen használt projekt jelenleg Groovy: `android/app/build.gradle`, `android/wear/build.gradle`. Kotlin DSL másolatok is léteznek; ne feltételezd, hogy azok beállításai érvényesek. Aktív fájlból és CI-ből ellenőrizz.

Ellenőrzött aktuális build: **AGP8.13.0 / Gradle8.13 / Kotlin2.2.21**, JDK21 futtató és Java17 bytecode. Egy régi átadóban szereplő Kotlin2.4.21 nem a most ellenőrzött aktív verzió. Toolchain-policy és build tényleges egyezését a feladat előtt vizsgáld; ne cseréld vakon a működő verziót.

Elsődleges natív kapu: `.github/workflows/native-android.yml`.

```bash
# GitHub hozzáféréssel, a megfelelő repóban:
git status --short
git branch --show-current
git fetch origin
gh workflow run native-android.yml --repo fuloplevente1998/TrainPilot --ref feat/native-android-migration
```

Szükség esetén célzott helyi környezetben (nem állítunk helyi futtatást e kézi parancsok alapján):
```bash
cd android
./gradlew --continue --no-daemon --stacktrace :app:assembleDebug :wear:assembleDebug
./gradlew --no-daemon :app:testDebugUnitTest :wear:testDebugUnitTest
./gradlew --no-daemon -PnativePhoneUiTests :app:assembleDebugAndroidTest
```

A teljes Gradle/Android/emulátor/aláírás ellenőrzést eddig GitHubon végeztük. Helyben célzott forrás-/XML/YAML/diff és a host Python tesztjei engedélyezettek.

Host bizonyítékok:
- `scripts/native/phone-ui-gate.py`: valós MainActivity/próbák, screenshot-kézfogás, külön instrumentation process minden GPS-engedélyes esetnek.
- `scripts/native/evidence-summary.py`: részleges/crash/missing/duplicate eredményt is el kell utasítani. Ne jelents teljes PASS-t kizárólag sikeres adb exit code alapján.
- `scripts/run-native-signed-upgrade.sh`: régi aláírt APK→natív `adb install -r`, szintetikus adatok.
- Kerek Wear tesztek a kanonikus workflow szerinti shardokban.

Célzott Python ellenőrzés (nem Android UI PASS):
```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests/native -p test_phone_ui_gate_report.py -q
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests/native -p test_evidence_summary.py -q
```

Az éles frissítés feltételei: eredeti package, magasabb versionCode, eredeti release signer, ellenőrzött migráció és adatmegőrzés. A GitHub Secrets meglévő keystore adatait használd a workflowban; értékeiket ne írd naplóba, forrásba vagy az átadóba. A zöld emulátoros upgrade nem igazolja önmagában a felhasználó valódi naplóival végzett frissítést. Ne kérd az éles app eltávolítását.

## 11. PR-ok, integrációk és munkaosztás

- #238 a fő draft migrációs PR/állapotlap; nem végleges elfogadás.
- Sok korábbi saját ág célzottan integrálva lett, a PR-ok egy része ettől még draft/open. Ne merge-eld őket vakon újra.
- Fontos integrált csomagok: lokalizáció, auto-refresh, training módok/kardió, Google-karbantartás, GPS recovery, Calendar/Planner/mode-approval, backup/data-integrity, Home recovery, Progress gallery/range, Actions-routing.
- #263/#264 Home/Progress scoped változások beépítve; #265 routing-check; #266 feladatindex; #267 audit Root-korrekciókkal integrálva; #269 A07 teszt/audit és javításai integrálva.
- Saját QA-only workflow-kat nem vettünk át a kanonikus migrációs workflow helyére.
- A korábbi téves issue→feladat-kiosztás miatt a végső main caller és elfogadási feltételek ellenőrzése kötelező. Nem a PR címe bizonyít kész funkciót.
- Nincs jelenleg másik modellnek kiosztott, visszajelzésre váró feladat. A folytató agent minden maradékot maga visz, vagy később új, explicit felhasználói döntéssel osztja szét.
- Belső subagentet ne indíts automatikusan. Párhuzamosításnál közös theme/components egy gazdánál, külön oldalak diszjunkt fájlhatárral; végső egységesség és integrált kapu egy gazdánál.

## 12. Online és offline folytatás

### Online

Klónozd/frissítsd az aktív ágat, ellenőrizd a remote HEAD-et és a munkafát. A GitHub branch HEAD a dokumentációs commitok miatt újabb lehet az e dokumentum elején rögzített alkalmazáscommitnál. A régi zöld SHA és az új aktuális SHA bizonyítékait külön kezeld.

A hitelesítés környezetfüggő. Korábban a CLI rövid ideig401-et adott, miközben a git push a meglévő credential helperrel működött. Ne indíts önkényes login/credentialcserét; diagnosztizáld a konkrét hívást, vagy használd az elérhető GitHub connectort. Offline környezetben se állíts friss GitHub-állapotot lekérdezés nélkül.

### Offline csomag

A kísérő ZIP:
- `START_HERE.md`: ez az átadó;
- `native/`: a migrációs forrás snapshotja, képek, tervek, eredeti Kotlin-export; `.git` és buildcache nélkül;
- `main-reference/`: a main7473b30 `www/` és régi Android Java forrása referenciaolvasásra;
- `evidence/`: a legutóbbi P1 hibás és a P0 zöld futás elérhető raw bizonyítékai;
- `SNAPSHOTS.json` és `SHA256SUMS.txt`: pontos forrásrefek és fájlellenőrzés.

A csomag SDK-t, JDK-t, Gradle/Maven dependencycache-t és aláírókulcsot nem tartalmaz. Offline a forrás/terv szerkeszthető; teljes offline build csak előre telepített toolchain és dependencycache mellett lehetséges. Egyébként az új ellenőrzésekhez online CI kell.

Ez forrássnapshot, nem teljes Git-história és nem telepítő. Offline új repositoryt lehet inicializálni saját mentéshez; később az eltéréseket a valódi migration ággal kell integrálni, nem mainre vagy force push-sal felülírni. A forráscsomag nem tartalmaz release signing secreteket. A kézi, még nem commitolt UI-kiosztási vázlatok nem aktív munkák és nem részei a snapshotnak.

## 13. Következő konkrét munkasorrend

1. Olvasd el ezt és a legújabb dizájndöntéseket; tekintsd a P1 látványt jelöltnek.
2. Reprodukálhatóan vizsgáld a három legfrissebb telefonos hibát és a12hiányzó teszt/process-crash okát. Őrizd meg a megszakadt futás bizonyítékát. Válaszd külön a jogos UI-selector/scroll frissítést az üzleti regressziótól; készíts stabil új gate-et ugyanarra a javított forrásra.
3. A már rögzített terveket pontosítsd képernyőnként a minőségi mércéig. Health/Progress main-referencia, Home/egyéb mockup-kompozíció, közös bevált elemek.
4. A natív meglévő state/callback/reader/writer leltárból különítsd el a tiszta prezentációt és a 6.pont hiányzó hookjait. A tényleges bekötéseket új párhuzamos rendszer nélkül javítsd.
5. P0 komponenskönyvtár és Home/Training teljes kidolgozása; aztán a többi főoldal, aktív módok, szerkesztők, részletek és Settings. Közben azonos fixture-es valódi before/after képek; nem minden apró docscommit után teljesCI.
6. Az összes változás integrálása után egy exact-commit teljes zöld kapu: JVM, telefon, Wear, signer/package/version, két signed upgrade, PNG/XML/hash. A kapu eredményét és a vizuális reviewt külön jegyezd.
7. Külön fizikai Phone/Watch7, Samsung Health/Health Connect/BLE/GPS/handoff/offline/Google és valódi mentés/migráció próbák, megfelelő előzetes mentéssel. Csak ezt követően értelmezhető a felhasználó adataira telepíthető végleges APK-pár. Main merge/kiadás csak külön felhasználói engedéllyel.

## 14. Bemásolható indítóutasítás másik agentnek

> Folytasd a TrainPilot natív migrációját az itt megadott aktív ágon. Először olvasd el a START_HERE.md / docs/NATIVE_TRANSFER_2026-10-27.md átadót, majd az AGENTS.md és NATIVE_RESUME.md tetejét, a NATIVE_DESIGN_HANDOFF.md friss felhasználói döntéseit és a final-ui-ux specifikációt. A P0 2f7cd835 zöld, a jelenlegi 67f2889f P1 jelölt telefonos kapuja piros: 99PASS/3FAIL/12MISSING és process crash, a pontos futás/bizonyíték az átadóban. Ne minősítsd késznek a UI-t. A main funkció/használhatóság minimum; Health/Progress teljes main-megjelenítés adaptálható; Home és többi oldal mockup-elrendezésű, legalább mockup-szintű kidolgozással. Minden megmaradt tervezést, bekötést, UI-kivitelezést és ellenőrzést te viszel; nincs más modellre váró feladat. Main/verzió/éles adat nem módosítható. Használd a meglévő natív motor/blokkokat, őrizd meg a32palettát és4nyelvet, és valós emulátoros képekkel/működéssel igazold az eredményt. Haladj önállóan, rövid magyar státuszokkal, és a kész/írt/teszteletlen állapotot mindig külön jelezd.

## 15. Közvetlen RAW-linkek — minden szükséges forrás elérése

A forráslinkek a rögzített `eb0f73ba` snapshotra mutatnak; az alkalmazáskód azonos a legfrissebb tesztelt `67f2889f` jelölttel. Ezek nem automatikusan a későbbi HEAD-re mutató linkek.

**Teljes fájlindex (minden natív snapshotfájl, továbbá main WWW/Java referencia):**
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/feat/native-android-migration/docs/NATIVE_SOURCE_LINKS_2026-10-27.tsv

Az index ref/path/git-blob-SHA/raw-URL oszlopokat ad; offline a csomagban is benne van. A saját átadó/index elérési linkje ágnévvel követi a frissítést, az indexben lévő forráslinkek commitra rögzítettek.

**Az átadó saját raw elérése:**
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/feat/native-android-migration/docs/NATIVE_TRANSFER_2026-10-27.md

### Kiemelt natív források, tervek, tesztek és mockupok

- `.github/workflows/native-android.yml`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/.github/workflows/native-android.yml
- `AGENTS.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/AGENTS.md
- `MIGRATION_STATE.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/MIGRATION_STATE.md
- `TrainPilot_Full_Conversation_Export.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/TrainPilot_Full_Conversation_Export.json
- `android/app/build.gradle`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/build.gradle
- `android/app/src/main/java/com/repforge/app/ActiveWorkoutStore.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ActiveWorkoutStore.kt
- `android/app/src/main/java/com/repforge/app/ArchiveIO.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ArchiveIO.kt
- `android/app/src/main/java/com/repforge/app/BackupRestoreManager.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/BackupRestoreManager.kt
- `android/app/src/main/java/com/repforge/app/DistanceTrackingService.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/DistanceTrackingService.kt
- `android/app/src/main/java/com/repforge/app/IntervalTimerService.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/IntervalTimerService.kt
- `android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt
- `android/app/src/main/java/com/repforge/app/NativeGpsMeasurement.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeGpsMeasurement.kt
- `android/app/src/main/java/com/repforge/app/NativeReplacementJournal.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeReplacementJournal.kt
- `android/app/src/main/java/com/repforge/app/NativeSetStopwatch.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeSetStopwatch.kt
- `android/app/src/main/java/com/repforge/app/NativeTimerTarget.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeTimerTarget.kt
- `android/app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt
- `android/app/src/main/java/com/repforge/app/NativeWorkoutSession.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeWorkoutSession.kt
- `android/app/src/main/java/com/repforge/app/NativeWorkoutTarget.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/NativeWorkoutTarget.kt
- `android/app/src/main/java/com/repforge/app/PhoneWorkoutExecutionStore.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/PhoneWorkoutExecutionStore.kt
- `android/app/src/main/java/com/repforge/app/data/db/TrainPilotDatabase.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/data/db/TrainPilotDatabase.kt
- `android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt
- `android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt
- `android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt
- `android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt
- `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt
- `android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt
- `android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRecoveryPresentation.kt
- `android/app/src/main/java/com/repforge/app/ui/home/QuickWorkoutScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/home/QuickWorkoutScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/JournalHealthPanel.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/journal/JournalHealthPanel.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt
- `android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt
- `android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainMenu.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainMenu.kt
- `android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt
- `android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressQuery.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressQuery.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/NativeStrengthRange.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/NativeStrengthRange.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt
- `android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt
- `android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotVisualSystem.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHomeMetricLayoutUiTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHomeMetricLayoutUiTest.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationLanguageUiTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationLanguageUiTest.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationWorkoutUiTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationWorkoutUiTest.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModeApprovalUiTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModeApprovalUiTest.kt
- `android/app/src/test/java/com/repforge/app/NativeCustomExerciseEditorTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/test/java/com/repforge/app/NativeCustomExerciseEditorTest.kt
- `android/app/src/test/java/com/repforge/app/NativeRestControlTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/test/java/com/repforge/app/NativeRestControlTest.kt
- `android/app/src/test/java/com/repforge/app/NativeThemeTokensTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/app/src/test/java/com/repforge/app/NativeThemeTokensTest.kt
- `android/build.gradle`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/build.gradle
- `android/gradle/wrapper/gradle-wrapper.properties`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/gradle/wrapper/gradle-wrapper.properties
- `android/variables.gradle`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/variables.gradle
- `android/wear/build.gradle`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/android/wear/build.gradle
- `docs/NATIVE_PARITY_AUDIT.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/NATIVE_PARITY_AUDIT.md
- `docs/NATIVE_RESUME.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/NATIVE_RESUME.md
- `docs/WORK_HANDOFF.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/WORK_HANDOFF.md
- `docs/audit-input/main-7473b30/INDEX.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/audit-input/main-7473b30/INDEX.md
- `docs/audit-input/main-7473b30/manifest.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/audit-input/main-7473b30/manifest.json
- `docs/design/NATIVE_DESIGN_HANDOFF.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/NATIVE_DESIGN_HANDOFF.md
- `docs/design/final-ui-ux/01-visual-system.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/01-visual-system.md
- `docs/design/final-ui-ux/02-screen-contracts.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/02-screen-contracts.md
- `docs/design/final-ui-ux/03-source-bindings.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/03-source-bindings.md
- `docs/design/final-ui-ux/04-implementation-and-acceptance.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/04-implementation-and-acceptance.md
- `docs/design/final-ui-ux/05-feature-preservation.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/05-feature-preservation.md
- `docs/design/final-ui-ux/README.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/README.md
- `docs/design/final-ui-ux/acceptance-register.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/acceptance-register.json
- `docs/design/final-ui-ux/palette-inventory.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/palette-inventory.json
- `docs/design/final-ui-ux/screen-contracts.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/screen-contracts.json
- `docs/design/final-ui-ux/source-bindings.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/source-bindings.json
- `docs/design/final-ui-ux/source-evidence.json`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/final-ui-ux/source-evidence.json
- `docs/design/mockups/01-main-pages-and-planning.png`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/mockups/01-main-pages-and-planning.png
- `docs/design/mockups/02-workouts-and-measurement.png`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/mockups/02-workouts-and-measurement.png
- `docs/design/mockups/03-journal-maps-and-health.png`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/mockups/03-journal-maps-and-health.png
- `docs/design/mockups/04-editing-and-settings.png`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/design/mockups/04-editing-and-settings.png
- `docs/plans/NATIVE_COMPLETION_BATCH.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/plans/NATIVE_COMPLETION_BATCH.md
- `docs/plans/NATIVE_FINAL_DELIVERY.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/plans/NATIVE_FINAL_DELIVERY.md
- `docs/reviews/NATIVE_A07_CATALOG_RESET_PARITY.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/reviews/NATIVE_A07_CATALOG_RESET_PARITY.md
- `docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md
- `scripts/native/evidence-summary.py`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/scripts/native/evidence-summary.py
- `scripts/native/phone-ui-gate.py`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/scripts/native/phone-ui-gate.py
- `scripts/run-native-signed-upgrade.sh`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/eb0f73bae73a7815cadfd0f4e4d00f4d04158c78/scripts/run-native-signed-upgrade.sh

### Main megjelenítés és működés — csak olvasásra

- `www/app.js`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js
- `www/index.html`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/index.html
- `www/styles.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/styles.css
- `www/global-uiux-177.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/global-uiux-177.css
- `www/health-dashboard-168.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/health-dashboard-168.css
- `www/health-journal-120.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/health-journal-120.css
- `www/progress-177.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/progress-177.css
- `www/training-107.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/training-107.css
- `www/typography-137.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/typography-137.css
- `www/gps-route-map-163.css`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/gps-route-map-163.css

A nagy app.js helyett használható a 93részes index és manifest a 9.pont linkjén. Az összes részt a teljes fájlindex is felsorolja, mert a natív snapshotban megtalálhatók. Bináris PNG/font/JSON ugyanúgy raw linken letölthető. Ha a fogadó modell nem tud URL-t olvasni, a ZIP megfelelő helyi fájlját add át, és jeleztesd a ténylegesen hiányzó forrásokat.
