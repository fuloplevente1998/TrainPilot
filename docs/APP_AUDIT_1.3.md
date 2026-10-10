# TrainPilot 1.3 – alkalmazásellenőrzés

2026. október 10. A telefonos látható verzió és telepítési kód változatlan: **1.3.3 /2729**. A javítások a #160 tesztágon vannak; a Wear natív kódja és protokollja változatlan.

## Megtalált hibák és javítások

| Hiba | Bizonyíték | Javítás |
| --- | --- | --- |
| A gyors edzés választója egy pillanat után bezárul | Felhasználói képernyőfelvétel és késleltetett natív Health Journal inicializálással reprodukált Chromium-hiba | A normál újrarajzolás nem zárja be a választót. Explicit bezárás, navigáció vagy edzésindítás továbbra is bezárja. |
| Háttéradatok érkezése elveszítheti a beírt sorozatszámot, súlyt, gyakorlat- vagy programnevet | A közös panel frissítése új HTML-re cserélte a meglévő mezőket | Háttérfrissítéskor a panel meglévő vezérlői és értékei megmaradnak; a mögötte lévő adatoldal frissülhet. A profil, a teljes oldalas gyorsválasztó és a futó edzés védett. |
| Lemondott navigáció közben bezáródhat a választó | A panel bezárása az edzéselhagyás megerősítése előtt történt | Aktív edzésnél csak a jóváhagyott navigáció zárja be. Elutasításkor a panel, keresés és edzés megmarad; jóváhagyáskor a piszkozat továbbra is mentődik. |

## Ellenőrzési kör

- A tényleges Edzés → Gyors edzés indítása gomb, késleltetett natív inicializálás, automatikus egészségadat-frissítés és annak ismételt hívása.
- Beállított sorozatszám, súly, nyitott gyakorlat és keresés megőrzése; explicit bezárás és edzésindítás.
- Gyakorlatkönyvtár, saját gyakorlat, saját program, célok, Naptár, Beállítások, Coach és egészségnapló panelje, valamint a személyes edzéstervező.
- Kezdőlap, Edzés, Egészség, Programok, Napló és a három navigációs panel: 320, 393 és 412 px szélesség; magyar/angol; Basic/Vivid témák. Összesen 96 méret/nyelv/téma/nézet kombináció. Navigáció, görgetészár feloldása, vízszintes oldaltúlcsordulás és JavaScript-hibák ellenőrzése.
- Teljes meglévő Node- és Chromium-regressziók, köztük az órás átadás, edzésnapló, kalóriaforrások, Drive-ütközéskezelés, visszaállítás, naptár, GPS, oldalspecifikus időmérés és szerkesztési folyamatok.

A kiadási ellenőrzések és az aláírt teszttelepítő eredménye a #160 PR-on követhető. A böngészős audit képernyőképei a telefonos kiadási kapu UI-artifactjában találhatók.

## Készülékes ellenőrzés

A teszt a natív adatérkezést vezérelt helyettesítővel játssza el. A valódi órás szenzor, Samsung Health/Health Connect jogosultság, GPS, hálózati kapcsolat és Google-fiók készülékes viselkedését ez nem igazolja. A korábban visszaigazolt Drive-szinkron és programos órás háttérmérés megmarad; a gyorsválasztó ismételt megnyitását és a gyors edzés órás mérését az új telefonos APK-val is ki kell próbálni.

Ez a vizsgálat a felsorolt hibákat és útvonalakat ellenőrzi, nem állítja, hogy az alkalmazásban nincs más hiba.
