# #163 GPS – 4/8. Tartós útvonalnapló és tulajdonosi kulcs

Állapot: **fejlesztési draft, csak a `feat/163-phone-gps-route-model` ágon**, a #205 PR részeként. A verzió változatlan: **1.3.4 /2730**.

## Kötés a meglévő edzéshez

A telefonos `DistanceTracker.start({key,...})` eddig is az adott távolsági edzés/sorozat stabil mérési kulcsát használta. A GPS-útvonal az **ugyanilyen kulccsal** mentett, saját készüléken tárolt adatsor; nem készül második edzésnapló, és a súlyzós sorozatokat nem érinti. Az elérhető natív metódusok:

- `DistanceTracker.route({key})`: célzott visszaolvasás: `{key,pointCount,partial,segments:[{ordinal,points:[{latitude,longitude,accuracyMeters,elapsedMillis,epochMillis}]}]}`.
- `DistanceTracker.deleteRoute({key})`: kifejezett törlés, **aktív** mérés kulcsát nem törölheti.
- A régi `DistanceTracker.status/start/stop` válaszok koordinátamentesek; csak `routeSavedPoints` és `routeSaveError` mutatja a mentés állapotát.

**Következő UI-kapcsolat:** az 5/8. lépésben a naplónézet a megőrzött mérési `key` segítségével kérhet útvonalat és jelenítheti meg a térképen. Az aktuális munkakör nem állítja, hogy a kész edzésnaplóban már látszik térkép, illetve hogy a naplóbejegyzés törlése automatikusan meghívja a natív útvonaltörlést. Ezeket külön integrációs és adatvédelmi tesztigazolással kell befejezni.

## Natív tárolási szabályok

- Kizárólag Android alkalmazásprivát, a rendszer automatikus biztonsági mentéséből kizárt `noBackupFilesDir/gps_routes` mappában tárolunk, `SHA-256(key).route` fájlnévvel. Sem az útvonalak, sem a nyers koordináták nem mennek automatikusan felhőbe.
- Egy pont: szakaszsorszám, szélesség, hosszúság, pontosság, monoton idő, GPS wall-clock idő. Az adattároló maximálisan 12 000 pontot, 256 szakaszt, 2 MiB fájlt és 512 útvonal-fájlt enged; túlcsordulás/íráshiba esetén a GPS-távolságmérés **nem állhat le**, de a `routeSaveError` jelzés kötelező.
- Egy-egy elfogadott GNSS-pont után az adatsor bővül. Szünet, szolgáltató kiesése vagy új mérési tulajdonos után új szakasz kezdődik. Egy mérési `key` ismételt indításakor a korábbi szakaszok megmaradnak, az új mérés folytató szakaszsorszámot kap.
- Sérült/hiányos tárolt útvonal jelzése `partial`; a korábbi érvényes pontokat nem töröljük automatikusan. A normál edzés `distanceMeters` értékének egyetlen forrása továbbra is a változatlan `DistanceAccumulator`.
- **Korlát:** a route fájlok szándékosan nincsenek benne az általános felhőszinkronban és az alkalmazás backup-archívumában. Export/import, adatmegőrzési és teljes naplótörlési összekötést az #163 átadásának későbbi részében ellenőrizni kell. Ne ígérjünk helyreállítható GPS útvonalat alkalmazás újratelepítése után, amíg ezek nincsenek meg.

## Ellenőrzés

`GpsRouteStoreTest`: ugyanazon kulcs alatti több szakasz, új Store instance utáni tartósság, külön tulajdonosok, explicit törlés, invalid kulcsok/útvonalak, hibás rekord, méretkorlát. `GpsRouteCaptureTest` és `GpsRouteTraceTest`: GPS pontszűrés/szünet/távregresszió. A **teljes, aktuális commitra futó natív és telefonos release gate** eredményét meg kell várni. PR merge csak külön felhasználói jóváhagyással.

## 5/8 – naplómegjelenítés, térképmotor és törlés

A `gpsRouteKey163` csak elfogadott natív pontmentés után kerül a sorozat/draft adataihoz, és mentett workouttal a naplóba. A napló kinyitott kártyáján csak meglévő route-key esetén látható útvonalgomb; a koordináták a `DistanceTracker.route({key})` metódussal kizárólag felhasználói megnyitáskor olvashatók ki. A sötét alapú, zöld útvonalas nézet offline SVG polylinet ad, online Leaflet 1.9.4 + OSM térképhátteret próbál betölteni késleltetve. A natív route pontok privát tárolása nem változik.

A naplóban a megerősített törlés után a hozzá tartozó GPS-kulcsok `DistanceTracker.deleteRoute({key})` hívással törlődnek, a meghiúsult törlések külön figyelmeztetést kapnak. A felhasználó által visszavont törlés nem töröl route-ot. A különálló célzott Playwright teszt vizsgálja az offline kirajzolást, a szakaszhatárokat, az azonosítók biztonságát, a törlést és a mobil nézetet. A szigorú egy-JS bootstrap szabály miatt a megjelenítő JS az index végére került, nem új script src hivatkozással.

**Nyitott:** hivatalos csempeszolgáltatói/jogosultsági ellenőrzés, offline cache stratégia, napló összes export/import/restore szélhelyzetének tesztelése, valós telefonos GPS-séta, friss natív és Chromium gate végleges eredménye. A Google/szinkron backup jelenleg nem menti a route fájlokat, telepítés/törlés után nem garantálható a visszaállítás. Verziót nem emelünk.
## 6/8 – külső térképcsempék, helyadatvédelmi kapu és teljesítmény

Az alap GPS-útvonal magánjellegű, a készülék privát tárhelyén van; az offline sötét SVG kirajzolás nem indít külső HTTP-kérést. Még internetkapcsolatnál is ezt mutatjuk alapból. Az online háttértérkép egy *külön, informált felhasználói kattintás*, amely előtt jelezzük a külső csempeszolgáltató számára látható területet és IP-címet. Az online kérés csak ezek után indulhat. A közösségi OSM standard tile-szervere Android WebView-ban nincs automatikusan bekapcsolva; külön tile provider beállítás szükséges (jogszerű azonosítás, attribution, cache, referer/felhasználói tájékoztatás). Az online Leaflet könyvtár a második kattintás előtt nem töltődik le.

Megfelelőségi forrás: https://operations.osmfoundation.org/policies/tiles/ — tilos az offline/bulk előtöltés, szükséges az alkalmazásazonosítás, és el kell helyezni a forrásmegjelölést. A TrainPilot alkalmazás számára tartósan használható térkép-szolgáltató és esetleges jogszerű offline csempecsomag a következő részlépésben, külön elfogadással választható ki.

**Teljesítmény-bukás lezárási feltétel:** a `ccab9e5` kódon a többlet naplóolvasás miatt az `#171` disclosure teszt bukott; a fölösleges route UI render/DOM-adatolvasó hook eltávolítva. A legutóbbi kód CI-je még folyamatban; nem tekinthető zöldnek addig, amíg a natív és mind a hat Chromium UI shard sikeresen le nem futott. Verzió változatlan.

## 7/8 – biztonságos visszaolvasás, mentés-visszaállítás és regressziók

A natív `GpsRouteStore` sérült sorát a visszaolvasás **nem** adja hozzá az érvényes pontszámhoz; a hibás koordináta, pontosság és azonos szakaszon visszafelé ugró monoton időbélyeg kizárt. A sérült tail `partial` jelzést ad; nem törli automatikusan az előző érvényes szakaszokat. Külön teszt ellenőrzi az érvényesen formázott, de érvénytelen latitúdót és a visszajátszott időbélyeget, valamint a közvetlen írás megtagadását.

Napló import/export: a route-fájlok szándékosan **nem kerülnek bele** a hagyományos backupba; a `gpsRouteKey163` hivatkozás önmagában nem GPS-nyomvonal. A TrainPiloton belüli `restoreText` helyreállítás után a **korábbi helyben megmaradt útvonalakat** csak a megerősített, sikeres naplócsere után és csak akkor töröljük, ha az új történet már nem hivatkozik rájuk. Elutasított visszaállításkor vagy kivétel esetén nem törlünk; ha a natív törlés sikertelen, ezt jelezzük a felhasználónak. A megfelelő Playwright regresszió a célzott `gps-route-map-163.cjs` teszt része. A fájlok app-újratelepítéskor és adatok teljes törlésekor elveszhetnek; nem ígérhető Google Drive/Google Sync GPS-koordináta-backup.

**Kötelező további elfogadás:** a teljes, aktuális commiton futó telefonos `release gate`, JUnit natív tesztek, összes Chromium shard és valós készülékes GPS-séta. A kód a PR-ben draft, nem került a mainre, **1.3.4 /2730** változatlan.
