# TrainPilot – Natív Android-migráció forrás-visszaállítási állapota

## Aktuális ellenőrzés – 2026-10-23

A `51fdd94` forráson a telefon/Wear debug **és eredeti kulcsos release** lefordult,
a csomagnév/verzió/tanúsítvány ellenőrzése sikeres. Letöltött XML: telefon
265/265, Wear 167/167, 0 failure/error/skipped. Az emulátoros kapuk a teszteszközök
hibáin álltak meg; az adatmigráció és a kerek UI még nem igazolt.
A `8b01fb2` ismét 265/265 + 167/167 unit sikert adott, a régi app előkészítése és az `adb install -r` sikeres. A natív tesztrunner `Trace` hiánya miatt a szemantikus migráció még nem igazolt; Wear UI 182/183. Javítás: `23db630`, [CI 39934990786](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39934990786) a rögzítéskor fut. [Aktuális átadás](docs/WORK_HANDOFF.md),
[bizonylat és artifactok](docs/builds/2026-10-23-native-migration-checkpoint.json),
[funkcióhiányok](docs/NATIVE_PARITY_AUDIT.md).

Az összehasonlítási alap a **2026-10-22-i gyorsított 1.3.71 /2735**,
`v1.3.71-perf-20261022` (`5d27aa9`); a teszt az eredeti október 21-i APK-ról is indul.
A teljes natív kiváltás, fizikai készülékes működés és élő adatok migrációja
külön igazolást igényel. **Main merge nem történt.** Az alábbi táblázat a történeti
forrás-visszaállítás állapota, nem az aktuális build összesítése.

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
