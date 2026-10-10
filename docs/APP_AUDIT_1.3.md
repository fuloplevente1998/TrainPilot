# TrainPilot 1.3 – alkalmazásellenőrzés

2026. október 10. A telefonos látható verzió és telepítési kód változatlan: **1.3.3 /2729**. A javítások a #160 tesztágon vannak; a Wear natív kódja és protokollja változatlan.

## Megtalált hibák és javítások

| Hiba | Bizonyíték | Javítás |
| --- | --- | --- |
| A gyors edzés választója egy pillanat után bezárul | Felhasználói képernyőfelvétel és késleltetett natív Health Journal inicializálással reprodukált Chromium-hiba | A normál újrarajzolás nem zárja be a választót. Explicit bezárás, navigáció vagy edzésindítás továbbra is bezárja. |
| Háttéradatok érkezése elveszítheti a beírt sorozatszámot, súlyt, gyakorlat- vagy programnevet | A közös panel frissítése új HTML-re cserélte a meglévő mezőket | Háttérfrissítéskor a panel meglévő vezérlői és értékei megmaradnak; a mögötte lévő adatoldal frissülhet. A profil, a teljes oldalas gyorsválasztó és a futó edzés védett. |
| Lemondott navigáció közben bezáródhat a választó | A panel bezárása az edzéselhagyás megerősítése előtt történt | Aktív edzésnél csak a jóváhagyott navigáció zárja be. Elutasításkor a panel, keresés és edzés megmarad; jóváhagyáskor a piszkozat továbbra is mentődik. |
| Hiányzó natív integrációnál JavaScript-kivétel | A natív Health Journal-only auditban a Google-híd hiánya `new Proxy(undefined)` hibát okozott | Az integrációs wrapper megőrzi a hiányzó híd értékét; a meglévő elérhetőségi ellenőrzések tovább működnek. A rendelkezésre álló híd nyelvi és hívási paraméterei változatlanok. |
| Keskeny Egészség-nézetben levágott lenyitható sorok és elérhetetlen oldalláb | A teljes kiadási kapu 320×740 px német nézetben 18,78 px túllógást jelzett; vizuális ellenőrzéskor a lenyitható kártyák 22 px magasra zsugorodtak, miközben a feliratuk ennél magasabb maradt | A kártyák megtartják természetes magasságukat. A zárt Egészség-nézet csak akkor rögzített, ha az összes tartalom elfér; rövid/keskeny kijelzőn vagy nagy betűknél az oldal görgethető. |

## Ellenőrzési kör

A képernyőképek vizuális átnézése két további hibát mutatott: az Egészség oldal műveletgombjai magyar szavakat betűn belül törtek két sorba, a Google-fiók leírásában pedig régi RepForge márkanév maradt. A gombok most a feliratuk tényleges szélessége szerint, egész gombként rendeződnek új sorba. A nagy betűs mód egyoszlopos marad. A fiókleírás TrainPilotot nevez meg. A mobilos audit szavankénti szövegtörést, gombon belüli túlcsordulást, a lenyitható sorok teljes magasságát és az Egészség oldal alsó szélének tényleges elérhetőségét is ellenőrzi. Ha a természetes magasságú tartalom nem fér el egy képernyőn, valódi görgetéssel és az oldalláb képernyőre hozásával ellenőrzi az elérhetőséget; ahol minden elfér, a nézet rögzített marad.

- A tényleges Edzés → Gyors edzés indítása gomb, késleltetett natív inicializálás, automatikus egészségadat-frissítés és annak ismételt hívása.
- Beállított sorozatszám, súly, nyitott gyakorlat és keresés megőrzése; explicit bezárás és edzésindítás.
- Gyakorlatkönyvtár, saját gyakorlat, saját program, célok, Naptár, Beállítások, Coach és egészségnapló panelje, valamint a személyes edzéstervező.
- Kezdőlap, Edzés, Egészség, Programok, Napló és a három navigációs panel: 320×740, 393×873 és 412×915 px; magyar/angol/német/román; Basic/Vivid témák. Összesen 192 méret/nyelv/téma/nézet kombináció. Navigáció, görgetészár feloldása, vízszintes oldaltúlcsordulás és JavaScript-hibák ellenőrzése. A 360 px-nél keskenyebb Egészség-oldalon a két fő művelet közös sorban, az egészségnapló külön, teljes szélességű sorban marad; így a hosszabb német feliratok sem kényszerítenek harmadik sort.
- Teljes meglévő Node- és Chromium-regressziók, köztük az órás átadás, edzésnapló, kalóriaforrások, Drive-ütközéskezelés, visszaállítás, naptár, GPS, oldalspecifikus időmérés és szerkesztési folyamatok.

A kiadási ellenőrzések és az aláírt teszttelepítő eredménye a #160 PR-on követhető. A böngészős audit képernyőképei a telefonos kiadási kapu UI-artifactjában találhatók.

## Készülékes ellenőrzés

A teszt a natív adatérkezést vezérelt helyettesítővel játssza el. A valódi órás szenzor, Samsung Health/Health Connect jogosultság, GPS, hálózati kapcsolat és Google-fiók készülékes viselkedését ez nem igazolja. A korábban visszaigazolt Drive-szinkron és programos órás háttérmérés megmarad; a gyorsválasztó ismételt megnyitását és a gyors edzés órás mérését az új telefonos APK-val is ki kell próbálni.

Ez a vizsgálat a felsorolt hibákat és útvonalakat ellenőrzi, nem állítja, hogy az alkalmazásban nincs más hiba.
