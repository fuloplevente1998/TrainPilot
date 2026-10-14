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
