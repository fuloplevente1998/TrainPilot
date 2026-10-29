# AI Studio 21:02 export — natív integráció audit, 2026-10-28

## Forrás és hatókör

- Bemenet: `docs/ai-studio/trainpilot-oct-27-2026-9-02-00-pm.zip`, a felhasználó csatolmányával byteazonos.
- SHA256: `db5ce026e2559366a7d3366332ab64affef7448839a7727f0dbfce447d7d65d9`.
- 8 846 684 byte, 479 ZIP-bejegyzés; CRC ellenőrzés PASS.
- Összevetés: PR270 `dc6ffc18b9d8ad6aad596fcfa3e8b864dc952cbe`.
- A közös modul útvonalát normalizálva 176 natív Kotlin/Java fájlból 160 byteazonos, 16 eltér a jelenlegi GitHub-verziótól. Ez nem bizonyítja, hogy a ZIP összes eltérése új fejlesztés: közben a natív ág is fejlődött.
- Statikus, szekvenciális review; nincs build, emulátorfuttatás, merge vagy appforrásmódosítás. Az export saját PASS állításai nem független tesztbizonyítékok.

## Ellenőrzött elemek

A natív adatbázis, store-ok, edzésmotor és shared szerződések forrásai valóban bekerültek az exportba. A launcher a natív MainActivity. A `MainActivity.kt:68–79` a DataStore-ból olvasott nyelvet, témakulcsot, pihenőidőt, hangot és rezgést továbbítja a Studio ViewModel `syncNativeSettings` függvényének (`:755–778`). Ez tényleges, egyirányú settings-beolvasás.

A Studio saját képernyői és felső navigációja maradnak a megjelenített felület. Ez összhangban van a felhasználó felső navigációs döntésével. A demóvezérlők a Studio prototípusban megengedettek; a következő hiányok az éles bekötést érintik.

## Kockázatok / hiányzó bizonyíték

### 1. P0 — migrációs olvasási hibát üres adatkészletté alakít

ZIP `app/src/main/java/com/repforge/app/data/migration/LegacyLocalStorageReader.kt:23–30,37,66–67`: a WebView inicializálási/olvasási hibát és a timeoutot `catch(Throwable)` után `emptyMap()` váltja fel; az időkorlát 60-ról 8 másodpercre csökkent. A catch a coroutine cancellationt is elnyeli. A változatlan `LegacyWebViewMigrator.kt:181–213` a sikeresen visszakapott üres map után eljuthat a `markMigrationComplete()` hívásig. Így egy valódi régi tár olvasási hibája hiányzó adatként kezelhető és kész migrációként rögzülhet. Ez adatkihagyás kockázata; nem bizonyított tényleges adatvesztés vagy törlés.

ZIP `MainActivity.kt:89–104` csak a replacement recovery állapotát választja szét. A natív `else if (migrationReady)` ág, betöltés, migrációs hiba és újrapróbálás felülete eltűnt, miközben a háttérben `startMigration()` tovább fut. A splash készjelzése nem igazol sikeres migrációt.

**Teendő:** a kanonikus migrációs olvasót és readiness/error/retry képernyőágat megőrizni; ezeket a prototípus kedvéért nem módosítani.

### 2. P1 — a megjelenített edzés/Health/Wear még demó, a natív motor nincs bekötve

ZIP `MainActivity.kt:103` a `com.example.TrainPilotAppRoot` felületét indítja, nem a natív Scaffoldot. `com/example/ui/viewmodel/TrainPilotViewModel.kt:72,84,94,129,142` továbbra is DemoDataProvider-adatokat használ.

- `finishAndSaveWorkout():519–541` csak MutableStateFlow-listába tesz fix dátumú, időtartamú és volumenű naplóbejegyzést; nem hívja a kanonikus edzéslezárást és Room-mentést.
- `saveNewBodyWeight():653–660` csak memóriát módosít.
- `triggerHealthSyncNow():673–679` 120-szal növeli a lépésszámot; nem valódi provider-szinkron.
- `toggleWatchControlMode():683–691` boolean/szöveg csere, nem tartós ownership/ack/handoff.

**Teendő:** az elfogadott Studio képernyőszerkezetet a natív state-ekhez és callbackekhez kötni; a demóállapotgépet nem éles motorként átvenni. Az eredeti testTag-ek és session/ownership guardok maradjanak elérhetők.

### 3. P1 — settings-beolvasás van, visszamentés nincs; futási bizonyíték hiányzik

ZIP `TrainPilotViewModel.kt:225–248,694–707`: a téma-, nyelv-, hang- és pihenőidő-változtatás csak `_uiState.update`. Nincs a natív settings store-ba visszaírás; újraindítás után a korábbi natív érték töltődik vissza. Egy későbbi settings-emisszió a lokális demóválasztást is felülírhatja.

A ZIP `app/build.gradle.kts:17` applicationId-je `com.aistudio.trainpilot.tpux01`, nem a kiadott app identitása. Ez külön Studio sandboxként érthető, de nem igazol a valódi TrainPilot-adatokat használó frissítést. Az exportban csak három generikus Example teszt található; a kanonikus natív regressziós suite nincs csomagolva. Az export jelentése a connected tesztet NOT_RUN-ként jelöli; független fordítási log, APK és exact-source emulátoros bizonyíték nincs ebben a ZIP-ben. A nagyfont/4nyelv/32paletta teljes megfelelőség nincs igazolva; a Studio Theme továbbra is fontScale-clampet alkalmaz (`Theme.kt:67`).

## Következő mikro-lépés

A 21:02 ZIP-et nem szabad a natív projekt teljes helyettesítőjeként ráhúzni. A jelenleg elfogadott 18:49 vizuális kánon marad érvényes, amíg a felhasználó ezt nem módosítja. A 21:02 export vizuális deltái külön áttekinthetők. A meglévő PR270 natív state/callback portját kell folytatni, a Studio komponenshierarchiáját megtartva. A settings írás a meglévő store-on, az edzésmentés a meglévő motoron, Health/Wear a valódi szolgáltatásain keresztül történjen. Utána egy exact-source közös Actions kapu és valódi képernyőképes összehasonlítás szükséges.
