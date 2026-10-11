# TrainPilot 1.3 – alkalmazásellenőrzés

2026. október 11. A telefonos látható verzió és telepítési kód változatlan: **1.3.3 /2729**. A javítások a #160 tesztágon vannak; a Wear natív kódja és protokollja változatlan.

## Megtalált hibák és javítások

| Hiba | Bizonyíték | Javítás |
| --- | --- | --- |
| A gyors edzés választója egy pillanat után bezárul | Felhasználói képernyőfelvétel és késleltetett natív Health Journal inicializálással reprodukált Chromium-hiba | A normál újrarajzolás nem zárja be a választót. Explicit bezárás, navigáció vagy edzésindítás továbbra is bezárja. |
| Háttéradatok érkezése elveszítheti a beírt sorozatszámot, súlyt, gyakorlat- vagy programnevet | A közös panel frissítése új HTML-re cserélte a meglévő mezőket | Háttérfrissítéskor a panel meglévő vezérlői és értékei megmaradnak; a mögötte lévő adatoldal frissülhet. A profil, a teljes oldalas gyorsválasztó és a futó edzés védett. |
| Lemondott navigáció közben bezáródhat a választó | A panel bezárása az edzéselhagyás megerősítése előtt történt | Aktív edzésnél csak a jóváhagyott navigáció zárja be. Elutasításkor a panel, keresés és edzés megmarad; jóváhagyáskor a piszkozat továbbra is mentődik. |
| Hiányzó natív integrációnál JavaScript-kivétel | A natív Health Journal-only auditban a Google-híd hiánya `new Proxy(undefined)` hibát okozott | Az integrációs wrapper megőrzi a hiányzó híd értékét; a meglévő elérhetőségi ellenőrzések tovább működnek. A rendelkezésre álló híd nyelvi és hívási paraméterei változatlanok. |
| A frissítés és kézi szinkron újra engedélykérést nyitott | Mindkét aktív Health Connect és Samsung Health szinkronág feltétel nélkül kérte újra az olvasási hozzáférést | Meglévő, akár részleges hozzáféréssel közvetlenül olvas. Új olvasási engedélyt csak hozzáférés nélküli kézi kapcsolódás kér; a szinkron nem kér további exportengedélyt. A natív hidak a teljesen megadott olvasási és meglévő háttérengedélyt is újra felhasználják. Az explicit Engedélyek gomb továbbra is kezelhetővé teszi a hozzáférést. |
| Az egészségnapló külön panelben nyílt | Felhasználói kérés az Egészség oldalon lenyitható naplóra | Önálló lenyíló kártya ugyanazon az oldalon, dátumszűréssel és 30 rekordos lapozással. Háttérfrissítéskor megmarad a szűrő, lap és nyitott részek. Nyitva az oldal görgethető. |
| Az egészségnapló soráról hiányzott az ikon; a Fejlődés oldal Coach-fejléce lecsúszott a képernyőről | Felhasználói képernyőképek, feltöltött 19 edzéses Chromium-napló | A napló közös stílusú szíves naplóikont kapott. A négy statisztikakártya alacsonyabb, az izomterhelés térközei kisebbek. A Coach-fejléc magyarul 360×873, 393×823 és 412×847 px méretben görgetés nélkül látható és megnyomható; rövidebb képernyőn továbbra is görgethető. |
| Keskeny Egészség-nézetben levágott lenyitható sorok és elérhetetlen oldalláb | A teljes kiadási kapu 320×740 px német nézetben 18,78 px túllógást jelzett; vizuális ellenőrzéskor a lenyitható kártyák 22 px magasra zsugorodtak, miközben a feliratuk ennél magasabb maradt | A kártyák megtartják természetes magasságukat. A zárt Egészség-nézet csak akkor rögzített, ha az összes tartalom elfér; rövid/keskeny kijelzőn vagy nagy betűknél az oldal görgethető. |

## Ellenőrzési kör

A képernyőképek vizuális átnézése két további hibát mutatott: az Egészség oldal műveletgombjai magyar szavakat betűn belül törtek két sorba, a Google-fiók leírásában pedig régi RepForge márkanév maradt. A gombok most a feliratuk tényleges szélessége szerint, egész gombként rendeződnek új sorba. A nagy betűs mód egyoszlopos marad. A fiókleírás TrainPilotot nevez meg. A mobilos audit szavankénti szövegtörést, gombon belüli túlcsordulást, a lenyitható sorok teljes magasságát és az Egészség oldal alsó szélének tényleges elérhetőségét is ellenőrzi. Ha a természetes magasságú tartalom nem fér el egy képernyőn, valódi görgetéssel és az oldalláb képernyőre hozásával ellenőrzi az elérhetőséget; ahol minden elfér, a nézet rögzített marad.

- A tényleges Edzés → Gyors edzés indítása gomb, késleltetett natív inicializálás, automatikus egészségadat-frissítés és annak ismételt hívása.
- Beállított sorozatszám, súly, nyitott gyakorlat és keresés megőrzése; explicit bezárás és edzésindítás.
- Gyakorlatkönyvtár, saját gyakorlat, saját program, célok, Naptár, Beállítások, Coach panelje, a lenyíló egészségnapló, valamint a személyes edzéstervező.
- Kezdőlap, Edzés, Egészség, Programok, Napló és a három navigációs panel: 320×740, 393×873 és 412×915 px; magyar/angol/német/román; Basic/Vivid témák. Összesen 192 méret/nyelv/téma/nézet kombináció. Navigáció, görgetészár feloldása, vízszintes oldaltúlcsordulás és JavaScript-hibák ellenőrzése. Az Egészség-oldalon a két fő művelet közös sorban marad; a napló külön lenyíló kártyát kapott. A természetes magasságú tartalom a szükséges helyen görgethető.
- Health Connect: részleges meglévő olvasási hozzáférés, ismételt és teljes kézi szinkron, hiányzó írási engedély, engedély nélküli háttérfrissítés, első kapcsolódás. Samsung Health: kezdeti elutasítás, sikeres engedélyadás, ismételt és teljes szinkron újabb kérés nélkül. Az Egészség Szinkronizálás és a napló Frissítés gombja a meglévő jogosultságot használja; a háttérkapcsoló újra bekapcsolása sem kér már megadott engedélyt.
- Lenyíló egészségnapló: 16 méret/nyelv kombináció, ugyanazon az oldalon; szűrés, lapozás, rekordok/alrészek, háttérfrissítés közbeni DOM- és szűrőmegőrzés.
- Teljes meglévő Node- és Chromium-regressziók, köztük az órás átadás, edzésnapló, kalóriaforrások, Drive-ütközéskezelés, visszaállítás, naptár, GPS, oldalspecifikus időmérés és szerkesztési folyamatok.

A kiadási ellenőrzések és az aláírt teszttelepítő eredménye a #160 PR-on követhető. A böngészős audit képernyőképei a telefonos kiadási kapu UI-artifactjában találhatók.

## Készülékes ellenőrzés

A teszt a natív adatérkezést vezérelt helyettesítővel játssza el. A valódi órás szenzor, Samsung Health/Health Connect jogosultság, GPS, hálózati kapcsolat és Google-fiók készülékes viselkedését ez nem igazolja. A korábban visszaigazolt Drive-szinkron és programos órás háttérmérés megmarad; a gyorsválasztó ismételt megnyitását és a gyors edzés órás mérését az új telefonos APK-val is ki kell próbálni.

Ez a vizsgálat a felsorolt hibákat és útvonalakat ellenőrzi, nem állítja, hogy az alkalmazásban nincs más hiba.
