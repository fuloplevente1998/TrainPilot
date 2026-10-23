# TrainPilot – Natív Android-migráció forrás-visszaállítási állapota

## Legfrissebb editor/motor ellenőrzés – 2026-10-23

**`9492755` / `39957648783` teljes SUCCESS**: natív frame-alapú editor,
tartós projekció/kurzor az ack előtt, natív record/undo és tulajdonosi epoch,
foreground controller csak a látható/resumed editorból. Telefon **292/292**,
Wear **167/167** unit; a korábbi 451 unit-eset mind megmaradt, +8 új próba.
Kerek UI **183/183**, aláírt eredeti/gyorsított upgrade **2/2**, nincs hiba/kihagyás.
[Bizonylat és APK-k](docs/builds/2026-10-23-native-editor-integration.json).
A teljes natív funkcióegyezés és az élő alkalmazás kiváltása továbbra sem kész.

## Legutóbbi funkcionális lépés – 2026-10-23

`cab522a` termékkód / **`1a0cc94` / `39953886817` teljes SUCCESS**:
tartós programnapi telefonos indítás, naplóalapú súly/ismétlés-előtöltés,
közös telefon/óra lezárás, natív eseményegyeztetés, napló+naptár tranzakció és
újrapróbálási/mentési/törlési védelem. Letöltött unit XML telefon **284/284**,
Wear **167/167**, nyers kerek UI **183/183**, eredeti és gyorsított release-ről
aláírt upgrade **2/2**, nincs hiba/kihagyás. Az emulátor töltési rendszerfelületének
fókuszhibája elkülönített tesztkörnyezeti beállítással megoldva; a termék Wear
fókuszkezelése és a teszt elvárásai változatlanok.
A teljes natív app és a saját adatokkal történő készülékes frissítés továbbra sem kész.
Részletek és következő lépés: [átadás](docs/WORK_HANDOFF.md).

## Korábbi teljes technikai kapu – 2026-10-23

**`134adcd` / [CI 39937602813](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813): teljes SUCCESS.**
Telefon/Wear debug és eredeti kulcsos R8 release lefordult, csomagnév/verzió/tanúsítvány
ellenőrizve. Letöltött XML: telefon **265/265**, Wear **167/167**. Kerek Wear UI
**183/183** egyedi eset, hiba/kihagyás nélkül. A tényleges eredeti és október 22-i
gyorsított 1.3.71 APK-ról az aláírt `adb install -r` frissítés és minden szintetikus
adatmegőrzési ellenőrzés sikeres (**2/2**).

[Átadás és következő munka](docs/WORK_HANDOFF.md),
[bizonylat/APK-k/hashek](docs/builds/2026-10-23-native-migration-checkpoint.json),
[funkcióhiányok](docs/NATIVE_PARITY_AUDIT.md).

**A teljes natív funkcióegyezés és élő készülékes kiváltás nincs kész.** Telefonos
gyors/outdoor indítás, draft folytatás, stopper/GPS teljesítés, Naptár/Coach és további paritás még hátra van;
a fizikai telefon/Watch7 és kétirányú szinkron külön kapu. Main merge nem történt.
Összehasonlítási alap a **2026-10-22-i gyorsított 1.3.71 /2735** (`5d27aa9`).
Az alábbi táblázat történeti forrás-visszaállítás, nem az aktuális tesztösszesítés.

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
