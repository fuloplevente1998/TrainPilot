# TrainPilot – Natív Android-migráció forrás-visszaállítási állapota

## Aktuális ellenőrzés – 2026-10-23

A visszaállítás utáni javítások az `feat/native-android-migration` ágon készülnek.
A `c89709f` forráson a telefon és Wear debug APK is lefordult; teljes unit,
aláírt release és adatmigrációs siker még nincs igazolva. Aktuális alkalmazásforrás
`eb1d21f`; [Actions 39917203006](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39917203006)
az állapot rögzítésekor fut. Részletes aktuális belépési pont:
[docs/WORK_HANDOFF.md](docs/WORK_HANDOFF.md).

A következő táblázat a **történeti forrás-visszaállítás** állapota, nem az aktuális
fordítás/teszt összesítése. A készülékes funkcióegyezés, kétirányú szinkron és élő
adatok migrációja külön igazolást igényel; main merge nem történt.

## Eredeti forrás-visszaállítás összegzése
A teljes AI Studio beszélgetés-exportból a Slice 1–8 fájlba címzett natív forrásblokkjai vissza lettek állítva. Ez a forrás-visszaállítás önmagában nem igazolja a fordíthatóságot vagy a készülékes működést.

- Az export 115 üzenetét feldolgozó kinyerő 70 teljes, egyértelműen fájlhoz rendelt forrásfájlt talált és állított vissza; az eredet és a SHA-256 a [NATIVE_IMPORT_MANIFEST.md](NATIVE_IMPORT_MANIFEST.md) fájlban szerepel.
- Két fájlút nélküli Kotlin-blokk elkülönítve vár kézi attribúcióra: [chunk-004.json](migration-source/review/chunk-004.json) és [chunk-016.json](migration-source/review/chunk-016.json); ezek tartalma nem lett találgatással beépítve.
- Az importált képernyők bekötötték a Slice 6 navigáció korábbi helyőrző útvonalait.
- Statikus ellenőrzés: nincs duplikált teljesen minősített Kotlin/Java osztálydeklaráció, hibás Kotlin package/path egyezés vagy fel nem oldott explicit projektimport.
- **A visszaállításkor még nem futott le a Gradle/Kotlin-fordítás:** a JDK 21-es próbát az Android SDK hiánya állította meg. A fordítás, tesztek és készülékes próba továbbra is nyitott.

## Szeletek Állapota (Migration Slices)

| Szelet | Leírás | Forrásállapot | Visszaállított komponensek |
| :--- | :--- | :---: | :--- |
| **Slice 1** | `:shared` Közös motorok és szerződések | Visszaállítva; build nyitott | `IntervalTimeline193`, GPS route osztályok, `WorkoutOwnership193`, ownership journal/transport, `WorkoutSharedContracts` |
| **Slice 2/A–B** | `:app` Room adatbázis, tárolók és migráció | Visszaállítva; build nyitott | Room entitások/DAO/adatbázis, DataStore, `LegacyWebViewMigrator`, workout/GPS/health stores |
| **Slice 3** | `:app` Wear-szinkron és parancsmotor | Visszaállítva; build nyitott | Telefonos workout-engine/ownership/GPS, PR-számítás, Wear sync és command-listener források |
| **Slice 4/A–B** | `:app` Előtérszolgáltatások és szenzorok | Visszaállítva; build nyitott | BLE, timer, distance tracking, health service/Connect és Samsung Health források |
| **Slice 5/A–B** | `:app` Fájl, fotó, adatvédelem és felhőszinkron | Visszaállítva; build nyitott | Archive/backup, fotó, privacy/startup és Google sync források |
| **Slice 6** | `:app` Compose alap, navigáció és főképernyők | Visszaállítva; build nyitott | Theme, activity, navigation, home/program screens és viewmodel-ek |
| **Slice 7** | `:app` Aktív edzés képernyő | Visszaállítva; build nyitott | Aktív edzés képernyő és viewmodel |
| **Slice 8/A** | `:app` Napló és fejlődés | Visszaállítva; build nyitott | Journal, útvonaltérkép és progress képernyők/viewmodel-ek |
| **Slice 8/B** | `:app` Egészségnapló és beállítások | Visszaállítva; build nyitott | Health dashboard és settings képernyők/viewmodel-ek |
| **Utólagos integráció** | Android manifest, FileProvider és Wear Gradle | Visszaállítva; build nyitott | A manifest, `file_paths.xml` és Wear modul konfiguráció |
