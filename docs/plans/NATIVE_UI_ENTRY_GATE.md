# UI-fejlesztés belépési kapuja — Root, 2026-10-26

Ez az aktuális sorrend a régi c116a042 baseline-ra készült tervek mellett.
A meglévő edzésmotor, adatbázis, migráció, backup és ownership marad.
Main/csomag/verzió nem változik. A modellváltást a felhasználó végzi.

## 1. Amit most javítottunk / integráltunk

- PR267: importált, Root által korrigált forrásaudit. A végső Quick inline/GPS
  wrapper, Wear-owned rest dispatcher és index.html online map provider is szerepel.
  G03/G04/G06 tényleges, behatárolt legacy-paritás; nem 11 új funkció.
- PR269: A07 audit + migrált custom-katalógus/adatmegőrzés regressziók.
  DataStore tesztizoláció setup+cleanup; nyers screenshot ellenőrzésben a nem
  értelmezett `&& echo` helyett valódi fájlméret-vizsgálat. QA-only workflow nem
  került a migration ágra. Az éles globális katalógustörlés nem lett hozzáadva.
- A PR-ok saját forrását is ugyanazon meglévő ágon javította Root a user kérésére;
  új fejlesztési ág nem indult, a cél továbbra a migration, mainhez nem nyúltunk.

## 2. Mért közös baseline — nem becslés

Actions **40404547333**, exact **3297e82bd7f841dc7a64b60ea26981898f5272ea**:

- JVM artifact12691626551: **1327 teszt, 0 failure/error/skip**, XML-ből Root ellenőrizte.
- Telefon artifact12691576896: **111/111 terminális, 105 PASS / 6 FAIL**,
  ezen belül mind a 4 izolált GPS-permission eset PASS; **52 nyers PNG**.
- Telefon/Wear debug, eredeti signerrel release/build/signer job SUCCESS;
  két signed upgrade és három kerek Wear UI-job SUCCESS.
- Egész futás FAIL a telefonos UI miatt. A legutóbbi dokumentációs/tesztjavításokra
  ez nem új közös elfogadás, és nem jogosít valós készüléken adatbiztonsági ígéretre.

## 3. A következő kódolási csomag: hat telefonos regresszió

| Eset | Bizonyított jelenség | Következő cél |
|---|---|---|
| Home HU/EN/DE/RO (`NativeLocalizationLanguageUiTest.settingsPickerSwitchHuEnDeRoRecomposesSettingsAndHomeIndependentlyOfSystemLocale`) | A régi `home_metric_recovery_unit` már nem létezik a #263 forrásjelölt recovery kártyán | A valódi új forrás-/hiányállapot lokalizációját ellenőrizni, a többi unit assertion megtartásával; nem visszatenni félrevezető readiness címkét |
| Intervallum nyelvváltás (`NativeLocalizationWorkoutUiTest.stopwatchAndIntervalRestLabelsRecomposeWithoutStartingOrCompletingSets`) | `interval_rest` nem található a scroll containerben nyitás után | Tényleges nyitás, lazy életciklus, görgethetőség bizonyítása; timer/sorozat nem indulhat el |
| Intervallum scope (`NativePhoneNavigationTest.intervalConfigurationIsWorkoutScopedUntilGlobalDefaultIsExplicitlyChosen`) | `interval_auto_advance` nem található | Ugyanazon valódi panel javítása, workout/global külön írás tesztje változatlan |
| Egyedi gyakorlat törlés (`NativePhoneNavigationTest.customExerciseCanBeCreatedEditedAndDeletedWithoutStartingWorkout`) | A sor megmarad, dialog általános invalid-field hibát mutat | Pontos backend exception és delete út javítása; tényleges Room törlés, dependency/CAS védelem megtartásával |
| Settings/editor elhagyás (`NativePhoneNavigationTest.openingSettingsKeepsTheCanonicalWorkoutAndStopwatchRunning`) | Home tab selected-re várva timeout | A valódi leave/pause/navigation receipt javítása; Settings alatt tovább futó stopper és identitás nem gyengíthető |
| Főfülek és másodlagos utak (`NativePhoneNavigationTest.fiveTabsAndSecondaryPlanningEntriesUseActualNativeScreens`) | Health után Home selected-re várva timeout | Navigációs stack és valódi selected állapot; korábbi 162746c7 fix önmagában nem oldotta meg |

A locale-esethez Root `94114204` tesztjavítást írt: a két valódi unit assertion
megmarad, recovery-nél a négy nyelv tényleges No-data source feliratát ÉS a
hiányzó pontszámot (`—`) ellenőrzi a törölt általános readiness-unit helyett.
Ez **kódban javított / runtime újrafuttatásra vár**, nem már zöld hatodik eset.

Ezt Root viszi **High / nehéz debugnál Extra High** ajánlással. Nem indítunk
második írót e fájlokra. Ugyanarra a javított commitra közös zöld natív kapu kell.

## 4. Mikor jöhet a 6.1 Medium / UI megvalósítás?

- **P0/P1 vizuális megvalósítás:** a fenti hat regresszió rendezése, közös zöld
  baseline és rögzített előtte-képek után. Specifikáció, tématoken-/komponensleltár
  olvasása már most is lehetséges; a végleges UI kódolását most még nem indítottuk el.
- **P2 aktív edzés:** G04 külön rest pause és G07 valódi Save/note hook Root
  funkciófeladat, G05 meglévő GPS accuracy megjelenítés hiányállapottal, G12 privát
  aktív thumbnail +48dp delete új design/a11y követelmény. Ne a dizájn írja át a motort.
- **P3 könyvtár:** G03 guarded Library→Quick start/add konfigurációs entry hiányzik;
  az aktív editor működő pickere nem törlendő. Root adja a végső callback-szerződést.
- **P4 térkép:** G06 consent-gated online réteg és zoom létező main funkció.
  Külön funkcióbekötés kell, mielőtt a térkép új UI-ja teljesnek mondható. Recenter
  és offline tile letöltés ettől nem automatikus új scope.
- **A07:** a globális Settings defaults reset nyilvános/adatmegőrzési szerződése
  még nincs kész; nem helyettesíti a működő A/B restore, és nem másolható át vakon
  a main egyedi gyakorlatokat eltávolító művelete. A vizuális alapcsomagot nem
  kell egy veszélyes reset implementációjára váratni; Settings végelfogadáskor külön tétel.
- G02 valós Watch model/battery nélkül ismeretlen állapot; G08/G09 elfogadott
  routing eltérés; G10 már integrált; G11 többelemű backup-lista külön új feature.

A UI-nál **6.1 Medium megfelelő** a grafit/arany, ötfüles megjelenítéshez,
32 paletta és HU/EN/DE/RO megtartásával. Új funkcióhook/CI-debug esetén vissza
High/Extra High. Hardveres Samsung/HC/BLE/Watch és valós személyes adatokkal
készülékes elfogadás a végső kiadáshoz kell; nem előfeltétele minden P0 kártyának.

## 5. UI után a készre jelentés feltétele

Minden P0–P6 képernyő/funkció megőrzési mátrixa, ugyanazon végső forrás zöld
közös kapuja, változatlan emulátoros before/after screencapok, több téma/4 nyelv,
48dp/kontraszt/nagy betű/kis képernyő, és külön valós készülékes elfogadás.
Nincs generált mockup mint futtatási bizonyíték, nincs tesztkihagyás vagy fake adat.
