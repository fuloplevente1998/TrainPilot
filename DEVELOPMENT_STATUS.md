# TrainPilot — fejlesztési állapot

## #163 GPS – 7/8 natív és naplós regressziók (2026. október 15.)

**Fejlesztési hely:** [#205 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/205); nincs main-egyesítés vagy verzióemelés (**1.3.4 /2730**). Az előző, `e16287b` commiton **mind a hat Chromium-shard sikeres** volt (így a korábbi #171 naplóolvasási többletjavítás igazolt), de a natív build-folyamatot újabb commitok következtében a legfrissebb forráson ismét ellenőrizni kell.

- **Útvonalnapló-tesztbővítés:** a `GpsRouteStore.read()` pontosan annyi pontot jelent, amennyi valóban validált és hozzáadott; sérült, de feldolgozható GPS-sor nem számít megtartott pontnak. Az ismétlődő monoton időbélyeget azonos szakaszon visszautasítja; `append()` közvetlen használata sem enged NaN, hibás koordinátát, 35 m feletti pontatlanságot. 3 célzott új JUnit negatív teszt.
- **Visszaállítás és helyadat-megőrzés:** a `restoreText` naplóimport csak **megerősített és teljesen sikeres** állapotban indíthatja a korábbi, importált naplóban már nem szereplő `gpsRouteKey163`-ok fájltörlését. Visszavonás/hibás import esetén GPS nem törölhető; az új napló által megtartott útvonalak megmaradnak. Célzott Playwright eset hozzáadva.
- **Adatvédelmi határ:** a route pontok csak az Android `noBackupFilesDir` lokális mappájában vannak; a hagyományos export/import nem tartalmazza a koordinátákat. Másik készülékre importálva az adott route gomb üres térképet jelezhet, nem generálhat kitalált útvonalat. Külső térképcsempék továbbra is csak explicit opt-in esetén kérhetők.
- **Kiadási feltétel:** a `#205` **legfrissebb commitján** mind a hat Chromium UI shard, Quick, Performance, `:app:testReleaseUnitTest` és `:app:assembleRelease` legyen sikeres. Friss SHA nélküli korábbi zöld futás **nem** release gate. A nyílt/törölt napló, GPS-jelkimaradás, restart, szünet és offline/online váltás valódi készülékes tesztje még a 8/8 elfogadási kör része; új kiadás nincs.

## #163 GPS – 6/8 offline térkép, adatvédelem, teljesítményregresszió (2026. október 15.)

**Fejlesztési ág:** `feat/163-phone-gps-route-model`, [draft #205 PR](https://github.com/fuloplevente1998/TrainPilot/pull/205). A korábbi 5/8 térképes napló tesztje (`gps-route-map-163.cjs`) a `ccab9e5` commit Chromium shard-1 körében **sikeres** volt, ám a teljes telefonos release gate megbukott: a `disclosure-performance-171.cjs` naplókártya-bontásnál három előzményolvasást talált a megengedett kettő helyett. A 6/8 lépésben eltávolítottuk a felesleges `mountRow/mountAll` DOM-figyelőket és render-wrapper-t; a gomb közvetlenül a meglévő lusta `tp3HistoryBodyHtml` generátorból épül, így nem adhat többlet naplóolvasást. **Az új commit teljes kapuját frissen meg kell várni.**

- **Offline-first:** a saját Android-privát GPS-útvonalpontok térképcsempe és bármilyen hálózati kérés nélkül, Mercator-vetületű, külön szakaszos, sötét SVG térképen jelennek meg. A felhasználó által bezárt edzés után nincs háttérben térkép-előtöltés; a táv számítás továbbra is a natív `DistanceAccumulator` hiteles adata, nem a polyline-vetület.
- **Online opt-in:** önmagában az online kapcsolat vagy az `Útvonal megtekintése` gomb **nem** tölti le Leaflet/OSM tartalmat. A felhasználónak külön információt és **`Online térkép betöltése`** gombot mutatunk, ami előre figyelmeztet: a megtekintett földrajzi terület és az IP-cím külső térképszolgáltatóhoz kerülhet. Online térkép betöltés csak a második kattintásra.
- **Android WebView korlát:** az OSM nyilvános `tile.openstreetmap.org` végpontja **alapértelmezetten nem aktív natív alkalmazásban**, amíg nincs jogszerű, azonosítható kliens és konfigurált, HTTPS-es tile-szolgáltató. A `TrainPilotGps163TileProvider` explicit `enabled,url,attribution,maxZoom` konfigurációjával később biztonságosan integrálható. Webes böngészőben a nyilvános OSM standard tile kizárólag explicit online gombbal használható, látható © OpenStreetMap attributionnel, szükséges normál böngésző cache/referer mellett.
- **OSM szabályzat:** `https://operations.osmfoundation.org/policies/tiles/`: attribution, alkalmazásazonosítás, referer (webes), korrekt cache, offline/bulk/prefetch tilalom. Az OSM adatforrás ingyenes, a közösségi csempeszerver nem korlátlan app-backend. Később cserélhető/licencelt tile provider vagy jogszerű saját térképcsomag szükséges a teljes offline háttértérképhez.
- **Regresszió:** `tests/browser/gps-route-map-163.cjs` ellenőrzi az offline, Android tile-disabled, beállítás nélküli, valamint az opt-in feltételeit. A meglévő `disclosure-performance-171.cjs` újra zöld kell legyen. Nincs main merge, APK-késznek nyilvánítás vagy verzióemelés; **1.3.4 /2730**.

## #163 GPS – 5/8 térképes napló és szelektív törlés (2026. október 14.)

A [#205 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/205) a korábbi 2–4/8 natív GPS-szakaszmodell és privát mentés után **kísérleti edzésnapló-térképet** kapott. A valódi GPS útvonallal rendelkező sorozatnál `gpsRouteKey163` történeti kulcs kerül a draftba/naplóba; csak ilyen naplóbejegyzésnél jelenik meg `GPS-útvonal megtekintése` gomb. Kattintásra az eddig is ismert `DistanceTracker.route({key})` célozza a készülékprivát helyadatokat; a napló böngészése koordinátaolvasás és tile letöltés nélkül működik.

- **Térkép:** a felhasználó által választott sötét vizuális téma, élénkzöld, szakaszhatárok között **nem összekötött** vonal, rajt/cél és kompakt táv/pontosság. Online Leaflet 1.9.4 + OSM csempe szükség szerinti betöltéssel; offline a GPS-útvonal SVG-vetülete térképcsempe nélkül is elérhető. Publikus OSM csempe offline/bulk letöltése nincs.
- **Naplótörlés adatvédelme:** kizárólag **megerősített** naplótörlés után a kapcsolódó `gpsRouteKey163`-ok `DistanceTracker.deleteRoute` hívással törlődnek. Megszakított megerősítés nem töröl. A törlés sikertelensége külön felhasználói figyelmeztetés, nem csendes adatmegőrzés.
- **TrainPilot single-source startup:** az első plusz külső JS betöltés megbukott a szigorú kompatibilitási kapun; a nézetet a `www/index.html` végén lévő **záró inline rétegbe** illesztettük. A JS-manifeszt továbbra is kizárólag `app.js`; a célzott `tests/browser/gps-route-map-163.cjs` a meglévő hat-shard Playwright suite része. A lusta naplómegnyitásra a render- és megnyitási hookok mellett közvetlen UI forrás-bekötés is készült.
- **Állapot:** natív és böngészős regressziók az aktuális commiton újrafuttatandók. A már megtalált lusta napló gombhiány és single-source hiba javításait ne tekintsük zöldnek **CI-igazolás nélkül**. Felhő/backup útvonalexport még nincs, újratelepítés utáni GPS-restaurálás nem ígért. Verzió **1.3.4 /2730**, nincs main merge vagy kiadás; csak külön jóváhagyással.

## #163 GPS – 4/8 privát, kulcshoz kötött útvonaltárolás (2026. október 14.)

A [#205 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/205) legújabb részfeladata a `GpsRouteStore` per-mérési-kulcs tartós útvonalnapló, a telefonos `DistanceTrackingService` háttérmentése, a `DistanceTracker.route({key})` célzott koordinátaolvasás és a `DistanceTracker.deleteRoute({key})` explicit törlés. A sorozatot/edzést az eddig használt GPS mérési `key` köti a fájlhoz; a térképes naplómegjelenítés és a napló-törléshez kötött automatikus adattisztítás a következő UI-integrációban történhet.

- **Adatvédelem:** `noBackupFilesDir/gps_routes/SHA256(key).route`, helyi készüléktár, nincs automatikus felhőszinkron vagy általános backup GPS-koordinátával. Célzott kiolvasáskor szakaszokban kapja csak meg a WebView az útvonalat; normál GPS státuszban koordináta nincs.
- **Védelmek:** 12 000 pont, 256 szakasz, max. 2 MiB/fájl, 512 fájl, sérülés és mentéshiba jelzés, az aktív mérési kulcs nem törölhető. A régi `DistanceAccumulator` hitelesített távolsága továbbra is változatlan; hiba esetén az edzés megy tovább.
- **Regresszió:** tároló újraolvasás, másik workout izoláció, törlés, helytelen azonosító és megszakadt rekord JUnit-tesztek. Kötelező a legújabb commit telefonos release gate és natív teszteredményeinek ellenőrzése, **nem előre igazolt**.
- **Részletek:** [GPS route storage terv](docs/gps/P1_163_ROUTE_STORAGE.md), [#163 issue](https://github.com/fuloplevente1998/TrainPilot/issues/163). **Nincs main-merge, release vagy verzióemelés. Marad 1.3.4 /2730.**

## #163 GPS – 3/8 natív pontgyűjtés és szakaszkezelés (2026. október 14.)

**Fejlesztési ág:** `feat/163-phone-gps-route-model`, [draft PR #205](https://github.com/fuloplevente1998/TrainPilot/pull/205). A 2/8. lépésben elkészült `GpsRouteTrace` pont-/szakaszlimites, szűrt, memóriabeli útvonalmodell és 8 teszt. A jelen **3/8. lépésben** a `GpsRouteCapture` a meglévő `DistanceAccumulator` változatlan elfogadási/távolságmérési logikája mellé kapcsolja a trace-t, és a `DistanceTrackingService` valós GPS-fixeivel táplálja.

- Kifejezett telefonos szünet, GPS provider kiesése, leállítás és új mérésszakasz esetén route-határ; átadás/új workout key esetén új route. A korábbi `pauseOwned`, `stopOwned` és `rollover` tulajdonosi ellenőrzések megmaradnak.
- A GPS plugin állapotába **csak pont-/szakaszszám és részleges (limitált) jelző kerül**, a koordináták nem. Nincs térkép, WebView-s helyadatátadás vagy tartós koordinátamentés; ez a következő, **4/8** lépés feladata.
- `GpsRouteCaptureTest` új regressziók ellenőrzik, hogy a távolság változatlan, szünet/jelkiesés nem köt össze koordinátákat, hibás mérés nem növeli a távot, álló zajból nem lesz útvonal. A natív release gate **eredményét ellenőrizni kell**, és csak sikeres teszt után tekinthető zöldnek.
- **Térképmotor:** a felhasználó Leaflet + OpenStreetMap és sötét, zöld útvonalas vizuális irányt jóváhagyta. A csempeprovider/licenc és offline stratégia későbbi #163 részlépés. Verzió változatlan: **1.3.4 /2730**; nem kerül mainre automatikusan.

## 2026-10-14 — #199 main, #191/#200 lezárva, P1 folytatás: #163

**Main integráció:** [#199](https://github.com/fuloplevente1998/TrainPilot/pull/199) egyesítve (`442b366`); a #191 órás mentés/szinkron és a #200 kihagyott pihenő felhasználói kérésre lezárva. A #201 explicit telefonos kilépéskor közös mérési/óra-szüneteltetés készülékes részpróbája sikeres; #201 nyitva marad a teljes regressziós/készülékes ellenőrzésig. Az órás önálló edzésátvétel a külön [#202](https://github.com/fuloplevente1998/TrainPilot/issues/202) P2 terv része. A korábbi P0 #162/#157 már #195-tel mainre került, újraépíteni nem kell.

**Release gate:** a #199 branch `7302d54` forrásán a telefonos [release gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38612855231) sikeres; a Wear `2ce7288` forrásán [release gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38587157455), újabb `9d17e7d` tesztkódján [kerek UI](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38598720507) sikeres. A köztes újabb commitok csak teszt/CI fájlokat változtattak, alkalmazáskódot nem. **A main aktuális commitjára a friss telefonos és Wear gate-et külön is le kell futtatni/igazolni**; korábbi eredmény nem azonos a main gate elfogadásával. A főági verzió továbbra is **1.3.4 /2730**, új felhasználói kiadás nincs önkényesen bejelentve.

**Következő P1:** [#163 – telefonos GPS-útvonalpontok mentése és térképes edzésnapló](https://github.com/fuloplevente1998/TrainPilot/issues/163). A meglévő `DistanceTrackingService` eddig csak idő/táv és GPS-státusz összegzést ment; útvonalpont és térkép később, külön feat ág/PR, valós pontokkal, pontos szakaszhatárokkal és biztonságos törléssel. Az önálló órás GPS #193 továbbra is P2, nincs duplikált helyadatfolyam.

## Új P2 Wear fejlesztés — #202: órás edzésátvétel és offline folytatás (2026. október 14.)

**Felhasználó által jóváhagyott irány, még nem implementáció:** [#202 – Telefonról indított edzés átvétele az órán](https://github.com/fuloplevente1998/TrainPilot/issues/202) (**P2 / enhancement**). A telefonon elindított **aktív vagy szándékosan szüneteltetett** edzés az órán tudatos „Edzés átvétele” művelettel átadható; az óra ezután telefonkapcsolat nélkül is kezeli az edzést, majd újracsatlakozáskor ugyanarra a kanonikus naplóra szinkronizál.

- **Átadás:** a meglévő telefon/óra tulajdonosi modellt kell továbbfejleszteni. Azonos `workoutId`, monotón tulajdonosi revízió, tartós kézfogás/nyugta és biztonságos szünet/fallback akadályozza a kettős futtatást, az elavult üzenet miatti újraindítást és a dupla edzésnaplót. Offline csak hitelesen már átvett órás tulajdonnal folytatható.
- **Órán teljes kezelés:** folytatás/szünet, aktuális gyakorlat, sorozat, ismétlés, súly, pihenő és automata időzítő; saját Health Services mérések; helyi adatmegőrzés és újracsatlakozás utáni egyszeri szinkron. A telefonos visszavétel külön igazolt átadás, nem automatikus tulajdonoscserélődés.
- **Biztonsági határ:** a #201 szerinti kifejezett telefonos edzéselhagyás továbbra is mindkét készüléken megállítja az aktív számolást; órán csak tudatos átadás és folytatás indulhat. A szünet nem növelheti az aktív időt vagy a szünetben elszámolt kalóriát; háttérbe helyezés önmagában nem szünet.
- **Kapcsolatok / újrafelhasználás:** [#199](https://github.com/fuloplevente1998/TrainPilot/pull/199) draft PR, [#201](https://github.com/fuloplevente1998/TrainPilot/issues/201) közös szünet, [#191](https://github.com/fuloplevente1998/TrainPilot/issues/191) mentési nyugta, #200, #155, #159, #188, #190 és #193. Nincs párhuzamos edzésmotor vagy második napló.
- **Sorrend:** a #199/#201 aktuális készülékes ellenőrzése és stabilizálása elsődleges; a #202 külön P2 backlog-fejlesztés, nem írja felül a meglévő P1/P2 prioritásokat. Az október 13-i prioritási táblázat történeti pillanatkép, az új igény annál későbbi.
- **Állapot:** kizárólag fejlesztési terv/Issue rögzítve. Nincs új alkalmazáskód, teszt-APK, verzióemelés vagy kiadás; **1.3.4 /2730 változatlan**. Megvalósításnál külön ág → tesztek → PR → felhasználói készülékes elfogadás → külön main/kiadási jóváhagyás.

## Új P2 fejlesztési igény — #197 (2026. október 14.)

**Jóváhagyott igény, tervezés alatt:** [#197 – Választható edzésstílusok és progressziós módok](https://github.com/fuloplevente1998/TrainPilot/issues/197) (**P2 / enhancement**). Az október 14-i négy edzésnapló-képernyőkép alapján a felhasználó külön, kézzel választható progressziós megközelítéseket kért (HIT és egyéb módszerek); nem automatikus programátállítást vagy jelen körös kódmódosítást.

- **Edzésstílus** és **progressziós szabály** külön beállítás: normál hipertrófia / biztonságos, alacsony volumenű HIT-inspirált mód; illetve dupla progresszió / lineáris terhelésnövelés / RIR- és trendalapú Coach. Később haladó volumenorientált mód is bővíthető.
- A Coach az elérhető súlylépcsőt, 3–4 releváns edzés trendjét, RIR-t, fáradtságot, saját testsúlyos nehezítési utat és a programcélokat figyelembe véve **előnézettel és felhasználói jóváhagyással** ajánljon változást. A HIT mód nem jelent automatikus bukásig edzést vagy kényszerített ismétlést.
- Az új mód nem írhatja felül a meglévő programokat/naplókat; a váltás visszavonható, a mobilos és Wear-adatokkal kompatibilis. A sorozat-, ismétlés- és kg-volumenmutatókat a célhoz illően, egymástól elkülönítve kell értelmezni.
- **Kapcsolódás:** [#128](https://github.com/fuloplevente1998/TrainPilot/issues/128) korábbi adaptív A/B és Coach-alapelvei újrafelhasználandók; a #197 azoktól eltérő, felhasználó által választható módokat ad.
- **Prioritás / sorrend:** új P2 backlog, nem előzi a már kijelölt **#191 → #163** P1 feladatokat. Az alábbi „2026. október 13-i” prioritási táblázat történeti pillanatkép; az új #197 annak dátum utáni kiegészítése.
- **Állapot:** Issue rögzítve; implementáció, teszt, APK vagy release még nincs; meglévő **1.3.4 /2730** verzió változatlan. Fejlesztés csak külön ág → tesztek → PR → kifejezett jóváhagyás szerint indulhat.

## Elfogadott #158, meglévő #159 és következő P1 — 2026. október 14.

A felhasználó 2026. október 14-én a `c800b4e` telefonos tesztbuild #158 működését elfogadta: **„#158 rendben, mehetünk tovább, de elvileg a #159 is készen van már.”** A visszajelzésből nem következtetünk külön 45 perces próba elvégzésére. Verzió marad **1.3.4 /2730**, Wear `9b94a10` változatlan.

A #159 alapfunkciója már a mainben megvalósított: `b35a567` (korlátos offline Mini Napló) és `d7a6e2e` (mérési előzmények), későbbi kerek kijelzős szövegtördeléssel és a #158 stabil késői frissítésével. A legutóbbi 8 kanonikus telefonos edzés, kész sorozatok/súly/ismétlés, elérhető oldalankénti idő/táv és külön Wear/telefonos mérés olvasható; nincs órás naplószerkesztés vagy második edzéstörténet. Az időtartam és mérési forrás a részletekben látszik. A jelen kör nem építi meg újra ezt a funkciót.

Ellenőrzés: `tests/wear-journal-resync.cjs` most is sikeres (újraszinkron, cache-vesztés, sikertelen küldés/újrapróbálás, külön 4/2 kcal, UTF-8 keret); meglévő 8 `WatchMiniJournalTest` és a hosszú nevek/tördelés Wear UI-próbája. [Main Wear release](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38553667882) és [66 kerek UI-teszt](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38553668063) sikeres. A Wear teljes fája byte szerint megegyezik az elfogadott `9b94a10` forrásával. A #159-re ebben a körben nem állítunk új, külön készülékes próbát.

**Következő valódi P1 feladat: #191 → #163.** A #191 a helyi mentést, Data Layer-átadást és a telefon által ténylegesen visszaigazolt mentést külön mutatja. A meglévő lezárási visszaigazolást és parancsbizonylatokat kell felhasználni, párhuzamos mentési rendszer nélkül. A #158 elfogadása és a #159 megvalósított állapota alapján a következő kör a #191 felmérése/fejlesztése; verzióemelés vagy új release nincs automatikusan engedélyezve.

## Előző P1 fejlesztési kör — #158 (a készülékes elfogadás előtt)

A #195 mainre került (`cac8937`), a készülékesen elfogadott #162/#157 lezárva. A jóváhagyott folytatás első feladata a **#158**, külön `fix/158-health-source-provenance` ágon, a friss mainből. Utána #159 → #191 → #163 következik.

A meglévő naplós forrásválasztó pontosítva: saját, azonosított órás mérés az alapértelmezés; a Health Connect olvasási csatornáját és a tényleges adatot közlő alkalmazást külön jelöljük. Samsung név csak a mérési eredményben közölt eredet/provider alapján szerepel. A natív telefonos lekérés megtartja az eredeti forrásmetaadatokat.

A nyitott mérési blokk, forrásválasztás, Frissítés és összecsukás stabil edzésazonosítót használ. A késői Wear revízió ugyanazt az edzést és a Mini Napló-publikációt frissíti. Az idegen edzés/időablak cache-e nem jelenik meg; sikertelen frissítés mellett a korábbi érvényes adat és a hiba együtt látszik. Valódi nulla és hiányzó adat elkülönül, részleges mérés jelölve.

Új teljes böngészős regresszió: külön 390/49 kcal, 45:36 szintetikus időablak, késői 400 kcal, listaátrendezés, forrásválasztás és nyitott napló megőrzése. A régi mainen a hamis Samsung-felirat reprodukálva. A korábbi kalória/időablak és nyitott napló tesztek megmaradnak; a forrásszövegek négy nyelven ellenőrizve. A CI és teszt-APK pontos bizonyítékai az [átadási lapon](docs/P1_158_MERESI_FORRASOK.md) követhetők.

**Verzió: 1.3.4 /2730.** A #158 rövid készülékes mérési és késői szinkronellenőrzésre vár. A 45 perc az eredeti hibajelzés időtartama, nem technikai minimum: a hosszabb összehasonlítás a következő rendes edzésben utóellenőrzésként végezhető, és nem blokkolja a további fejlesztés előkészítését. Ez a kör nem módosítja az órás alkalmazást; a készülékesen elfogadott Wear `9b94a10` tovább használható. A mainre emeléshez az új telefonos build készülékes elfogadása szükséges.

## Készülékesen elfogadott P0 kör — #195

A felhasználó 2026. október 14-én a telefonos `b9dbf7b` / órás `9b94a10` tesztpárt készülékesen elfogadta: **„Minden szuper, menjen mainre, zarjuk az issuet :)”**. A #195 main-egyesítése és a kapcsolódó #162/#157 lezárása jóváhagyva. Verzió mindkét appban **1.3.4 /2730**.

Elkészült a telefonos natív háttérrögzítés, az egységes munka/pihenő/szünet időnapló, az órás indítás és mérés szinkronja, a kész sorozat javítása/visszavonása és a félbehagyott edzés végleges törlése. Telefonon és órán is öt fokozatú edzésértékelés után közvetlen kezdőlap; tartós, ismétlés ellen védett válasz, meglévő gyakorlatértékelések elsőbbségével.

Ellenőrzött: 140 Node-csomag, 92 Chromium-csomag, teljes natív release gate-ek, 66 kerek Wear-próba és a valódi Android–WebView háttérvezérlési teszt. Mindkét APK eredeti aláírással és ellenőrzött forrás/hash/verzió adatokkal készült. [Buildek és elfogadás](docs/P0_KESZULEKES_ELFOGADAS_162_157.md).

**Folytatás:** a #195 egyesítése után friss `origin/main`-ből készül új feladatág. A tesztág és a korábbi munkafák történeti források. Következő jóváhagyott prioritás: #158, #159, #191, majd #163 térkép; a részletes terv és a Samsung Health energiapontszám/Coach kutatás a [Work-útmutatóban](docs/WORK_FEJLESZTESI_UTMUTATO.md) marad. Az alábbi korábbi naplók az akkori állapotot őrzik.

## Kötelező közös munkaprotokoll — 2026. október 13-tól

**Prioritás:** A következő fejlesztési és hibajavítási feladatok előtt ezt a munkarendet követjük. A `DEVELOPMENT_STATUS.md` a központi állapotnyilvántartás, a GitHub Issues az egyedi feladatok nyilvántartása; a kettőt szinkronban kell tartani.

1. **Átlátható körök:** Az összetett munkát előzetesen 5–10 átlátható részfeladatra bontjuk. A felhasználó későbbi kérésére az összefüggő részfeladatok közös, nagyobb fejlesztési körben végezhetők; a P0 körben a 6–7., majd a 8–9. lépés együtt halad.
2. **Kötelező jóváhagyási pont:** Minden közösen kijelölt kör után megállunk; a következő csak a felhasználó kifejezett „Mehet tovább” / „Folytasd” jóváhagyásával kezdődik. A jóváhagyás nem terjed ki automatikusan a mainre emelésre vagy kiadásra.
3. **Rövid válaszok:** Legfeljebb 200–300 szó válaszonként, fókuszált eredménnyel és a szükséges hivatkozásokkal. A kódszerkesztést nem bontjuk mesterséges 40–50 soros darabokra; az összefüggő változtatások és ellenőrzéseik egy körben készülnek el.
4. **Állapotjelzés:** Minden kör végén kötelező a `[Aktuális lépés: X/Y | Állapot: Jóváhagyásra vár]` sor, a tényleges lépés és státusz alapján.
5. **GitHub-követés:** Felméréskor először a main, ez a fájl, a részletes fejlesztési terv és a meglévő Issues/PR-ek aktuális állapotát vizsgáljuk. Meglévő feladatot nem duplikálunk. Az új fejlesztés vagy hiba saját Issue-t, elfogadási feltételeket és indokolt prioritást kap.
6. **Biztonságos változtatás:** Célzott fejlesztési ág → ellenőrzés/tesztek → PR → felhasználói jóváhagyás → main. A tesztelt és a fizikai eszközön elfogadott állapot külön szerepel. Verziószámot, release-t és APK-t nem változtatunk önkényesen.
7. **Dokumentálás:** A részfeladat lezárásakor frissítjük az érintett Issue/PR állapotát és szükség szerint ezt a fájlt: mi készült el, mi maradt hátra, mi tesztelt, mi igényel készülékes visszajelzést.
8. **Fejlesztési szakaszok:** (1) állapotfelmérés; (2) feladatok/Issues összevetése; (3) prioritások; (4) Issue-rendezés; (5) egyenkénti fejlesztés; (6) tesztelés; (7) PR és jóváhagyás; (8) dokumentáció és következő kör.

**Munkaprotokoll állapota:** 2026. október 13-án jóváhagyva, a mainre egyesítve (#187). A feladatfelmérés és az új Issue-k létrehozása megtörtént; az alábbi sorrend az elfogadott kiinduló terv.

## Jóváhagyott GitHub-prioritások — 2026. október 13.

**2026. október 13-i prioritási pillanatkép: akkor 12 nyitott fejlesztési Issue.** A #162/#157 P0 kör azóta elkészült és készülékesen elfogadott. A P0–P3 besorolás a GitHub Issue-címkéken is szerepel. A sorrend a tervezett munkavégzés prioritását jelzi, **nem** azt, hogy a feladat már elkészült vagy készülékesen elfogadott.

| Prioritás | Issue | Fejlesztés / következő teendő |
| --- | --- | --- |
| **P0 – kritikus** | [#162](https://github.com/fuloplevente1998/TrainPilot/issues/162) | **Elkészült, készülékesen elfogadva — #195** |
| **P0 – kritikus** | [#157](https://github.com/fuloplevente1998/TrainPilot/issues/157) | **Elkészült, készülékesen elfogadva — #195** |
| **P1 – magas** | [#158](https://github.com/fuloplevente1998/TrainPilot/issues/158) | **Készülékesen elfogadva — #196** |
| **P1 – magas** | [#159](https://github.com/fuloplevente1998/TrainPilot/issues/159) | **Meglévő megvalósítás ellenőrizve; nincs újrafejlesztés** |
| **P1 – magas** | [#191](https://github.com/fuloplevente1998/TrainPilot/issues/191) | Órás helyi mentés, függő szinkron, visszaigazolás és hibakezelés egyértelmű állapotai |
| **P1 – magas** | [#163](https://github.com/fuloplevente1998/TrainPilot/issues/163) | Telefonos GPS-útvonalpontok rögzítése és naplótérkép |
| **P2 – közepes** | [#188](https://github.com/fuloplevente1998/TrainPilot/issues/188) | Utolsó sorozatrögzítés biztonságos visszavonása |
| **P2 – közepes** | [#189](https://github.com/fuloplevente1998/TrainPilot/issues/189) | Wear érintészár edzés közben |
| **P2 – közepes** | [#192](https://github.com/fuloplevente1998/TrainPilot/issues/192) | Oldalankénti (bal/jobb) automata időzítés és oldalcserés pihenő |
| **P2 – közepes** | [#193](https://github.com/fuloplevente1998/TrainPilot/issues/193) | Önálló órás GPS-útvonalszakaszok, tárolás és szinkron |
| **P3 – későbbi** | [#190](https://github.com/fuloplevente1998/TrainPilot/issues/190) | Korona/lünetta alapú súly- és ismétlésszerkesztés |
| **P3 – későbbi** | [#184](https://github.com/fuloplevente1998/TrainPilot/issues/184) | Samsung Health energiapontszám és TrainPilot Coach közös ajánlásának tervezése |

**Kötelező fejlesztési függőségek és indítási feltételek:**
1. Először a **P0** automata rendszer és gyorsindítás készülékes hibáit vizsgáljuk, különösen a három használati irányt (csak telefon, önálló óra, párosított működés), a kijelzőzárat és a szinkront. A #162-ben külön rögzítjük a fizikai elfogadás eredményét.
2. A **#163 telefonos térképet** csak a #162 alapműködésének stabilizálása után kezdjük. Az órás **#193 GPS-bővítés** a #163 mentési/megjelenítési modelljére épít, nem helyettesíti azt.
3. A #191 mentési állapot és a #159 Mini Napló a tényleges szinkron-visszaigazolásra és hiteles mérési forrásokra épül. A #192 az automata idővonal és a munka/pihenő naplóbontás rendezése után következik.
4. A P2/P3 feladatok külön Issue-n, külön ágon és külön PR-ben haladnak. Egy feladat után megállunk a felhasználó jóváhagyásáig; verzióemelés és mainre emelés külön engedélyhez kötött.

**Nem tervezett:** [#120 – HRV-adathozzáférés](https://github.com/fuloplevente1998/TrainPilot/issues/120) felhasználói kérésre `closed / not_planned` állapotú. A szükséges adatok megbízható hozzáférése jelenleg nem igazolt; nem része a 12 nyitott fejlesztésnek.

Frissítve: **2026. október 13.** A felhasználói verzióleírások négy családot használnak: **1.0, 1.1, 1.2, 1.3**. A buildazonosítók, APK-k, tagek és release-ek megmaradnak.


## P0 fejlesztési kör: #162 → #157 (2026. október 13., készülékes elfogadásra vár)

### Készülékes visszajelzés — #162 megjelenítési javítás

A `c613a75` telefonos próbájában (#2417–2418) a felhasználó telefonról indított gyorsedzés, automata mód és telefonos sorozatrögzítés mellett háttérvezérlési hibát jelentett egykezes evezésnél és sima fekve nyomásnál is. A #162 nem elfogadott. A `/kar` és `/láb` ismétléses egységek JS/native útvonalhiánya javítandó; ez **önmagában nem magyarázza** a sima ismétléses gyakorlat hibáját. Az új tesztbuild a natív előkészítés/összevezetés/élesítés hibáját külön, személyes adatot nem tartalmazó `E162-...` kóddal jelzi. A teljes készülékes ok csak ezzel az eredménnyel igazolható. A pihenő alatt alvó felület, változatlan oldalankénti ismétlés és duplikált parancs elleni védelem célzott tesztje megmarad; pontos APK/CI a [P0 átadási lapon](docs/P0_KESZULEKES_ELFOGADAS_162_157.md).

A `859a6a1` telefonos tesztbuild felhasználói próbájában (#2412 kép) az automata módnál súlyzós gyakorlatokra is megjelent a beltéri/GPS/lépésmérési blokk, az alsó állapotszöveg levágódott. Ez **nem készülékes elfogadás**, a #162 továbbra is nyitott.

Az ok reprodukálva: a WearSet az opcionális távolságot nullával küldi, a telefon korábbi besorolása pedig a mező puszta jelenlétét távolsági gyakorlatnak vette. A javítás elkülöníti az üres/nullás szinkronmezőt a tényleges távolsági típustól, helyreállítja a korábban tévesen jelölt aktív piszkozatot, és nem kapcsol üres GPS-szolgáltatásállapotot időalapú erőgyakorlathoz. A súly/ismétlés/kész jelölés és a valódi futás/görkorcsolya/GPS funkció megmarad. A #2415 képes visszajelzés alapján az automata állapot az alsó panel teljes szélességét használja: rövid szöveg az „Automata mód” mellett, hosszabb szöveg szükség szerint tördelve, kényszerített második sor nélkül. A visszaszámláló az alsó két gomb között marad.

Az új böngészős regresszió a régi forráson három téves mérési panelt reprodukált, a javításon sikeres. Négy nyelv, 320/393 px, meglévő hibás piszkozat, szinkronból jövő nullás mező, valódi távolsági aktivitás és a szokásos automata időzítés ellenőrizve. A teljes telefonos gate a #195 ágon készíti a javító APK-t; a végső commit/hash/letöltés a [P0 átadási lapban](docs/P0_KESZULEKES_ELFOGADAS_162_157.md) frissül. Órás natív változás nincs; a korábbi ellenőrzött órás APK tovább használható.

### Folytatás — 8–9/10: #157 közös ellenőrzése és tesztbuild

A felhasználó jóváhagyta a közös gate- és tesztbuild-kört. A #157 jelenlegi telefonos gyorsindítása előbb publikálja az azonosított edzést, majd egyszer nyitja meg az órát. A forrásellenőrzés szerint az órás indítás megvárja az edzésazonosítót és a kért revíziót; a szenzorindítás látható Activityből, engedélyezett foreground szolgáltatásban történik. Egy korábbi edzés cache-e nem jogosít fel új mérés indítására.

Az aktuális gyorsindítás runtime-regressziója a #162 új natív vezérlésével együtt is ellenőrizve: előkészítés → vezérlés élesítése → egyező generáció/revízió publikálása → egyszeri óranyitás. Sikertelen natív előkészítésnél nincs élesítés, publikálás vagy óranyitás; sikeres közeli újrapróbálás után az eredeti függő átadás egyszer végrehajtható. A meglévő lemondási, piszkozat-, offline-, órás eredet- és óranyitási hibatesztek megmaradnak.

Az első közös telefonos gate a JSON-visszatöltési fixture-ben várakozva időkorlátba futott: a teszt a már aszinkron natív restore-védelem miatt még meg nem nyílt új fájlválasztó helyett az előző callbacket oldotta fel. A fixture most megvárja az aktuális választót; a teljes visszatöltési próba célzott Chromium-futása sikeres. A kapcsolódó natív előkészítés is az órás parancszár teljes hatálya alá került, ezért restore nem indulhat két aszinkron preflight lépés közé. Erre külön runtime-regresszió készült; egyik ellenőrzést sem távolítottuk el a teljes gate-ből.

A közös forrás a #195 **`fix/p0-162-157-safe-wear-sync`** ágára kerül. Ezen fut a telefonos Release Gate, Wear Release Gate, Wear kerek kijelzős UI és a PR teljesítményellenőrzése. A signing a meglévő GitHub Secretsből történik; verzió **1.3.4 /2730**. Az APK-k a branch workflow artifactjai, nem publikált release-ek. A pontos tesztpár, commit, SHA-256, gate-eredmény és készülékes próbamenet a [P0 készülékes átadási lapban](docs/P0_KESZULEKES_ELFOGADAS_162_157.md) szerepel, amely a build után frissül. A #195 draft; main és kiadás változatlan, #162 és #157 nyitott.

### Folytatás — 6–7/10: egységes időnapló és #162 regresszió

A felhasználó kérésére a 6. és 7. lépés közös fejlesztési kör. Az új forrás GitHub-tesztága **`work/p0-162-native`**; a #195 eredeti `fix/` ágát és a main-t még nem módosítjuk. Ezen az ágon a Quick Validation és az új **P0 native validation (no APK)** fut. A második ellenőrzés a valódi Android-projekt telefonos és Wear unit tesztjeit fordítja/futtatja; nem készít és nem publikál APK-t, nem indítja a közös release gate-et.

A `IntervalTimeLedger` külön menti a tényleges munka-, pihenő- és kézi szünetidőt. A kihagyott pihenő csak a menetrend pozícióját lépteti, nem kap mért időt. A natív checkpoint a teljes időnaplót, az aktuális órát és az időalapú kész sorozatot egy SQLite-tranzakcióban menti. Csak a kész sorozatok kapnak importálandó eseményt; az egy másodperces checkpointok nem növelik korlátlanul az eseménysort. Így egy alvó WebView mellett befejezett időalapú gyakorlat után is fogadható a következő ismétléses rögzítés.

A telefonos kézi rögzítés a támogatott ismétléses, automata edzésben ugyanarra a natív parancsútra kerül, mint az órás rögzítés. A kézi munkafázis a foreground vezérlés/előző pihenő végétől a tényleges rögzítésig számolható; új folyamatban az előző horgony nem használható ismeretlen kimaradt időre. A telefonos explicit javítás/visszavonás külön, revízióval és előző értékekkel ellenőrzött API-t használ; a szokásos pillanatkép továbbra sem írhatja felül a mért kész sorozatot.

Az önálló Wear automata ugyanazt a `schema:1`, `key`, `owner`, `workMs`, `restMs`, `pausedMs`, `positionMs` időadat-szerződést exportálja. A telefon kulcsonként kumulatív hozzájárulásként menti a `session.automaticTime162` mezőbe; a duplikált snapshot nem adja hozzá kétszer. Böngészős időzítésnél a tartalék megoldás ugyanezt a formát használja. A korábbi időalapú `reps` mező konvencióját nem használjuk újra összegzéshez: a külön időnapló az egységes adatforrás.

Pihenő közbeni telefonos javítás vagy ismételt vezérlés-előkészítés nem indít kézi munkafázist; a pihenő nem számolódhat munkaidőként is. Egy korábbi kész sorozat javítása megőrzi az éppen végzett kézi sorozat időhorgonyát. A telefonos rögzítési útvonal a modern Capacitor plugin-regiszterrel is működik, a sikertelen bridge-hívás pedig nem állítja készre a sorozatot.

**Végső GitHub-ellenőrzés — `1fff2ecd465369b7f7dd9ecf795a2820f00805de`:** a [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38402839749) **140 sikeres / 0 hibás** Node-tesztcsomaggal zárult. Az [APK nélküli natív Gradle-validáció](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38402839765) a teljes telefonos modulban **145**, a Wear modulban **97** unit tesztet futtatott: **0 hiba, 0 kihagyás**. A tényleges telefonos Java- és Wear Kotlin-forrás is fordult. Helyben az új P0 alap 41 natív Robolectric tesztje és 32 Wear modelltesztje sikeres volt; ezek a részhalmazok nem adódnak hozzá a teljes CI-tesztszámokhoz. A kizárólag Markdown-fájlokat módosító későbbi push nem ismétli meg a natív Gradle-futást.

**Készülékes elfogadás nem történt.** A GPS-tulajdonos külön előkészítése marad a korábban kijelölt hatókörben, a nem támogatott mérések meglévő útvonala megmarad. A #162 és #157 nyitott, verzió **1.3.4 /2730**. Következő, külön jóváhagyandó közös kör: #157, majd a megállapodott release gate és tesztbuild. Az alábbi 4–5/10 bejegyzések a korábbi lépések akkori helyi állapotát őrzik; a mostani forrás már a fenti GitHub-tesztágon található.

### Helyi folytatás — 5/10: natív parancsbekötés és űrlapfüggetlen indítás

A helyi `local/p0-162-native` ág a #195 `e528d1e` forrásából és a 4/10-es `3fff44c` alapból folytatódik. A GitHub sorrendjének alapja továbbra is a #194 `0b37ced` commitja: **#162 → #157**.

- A `WearCommandListenerService` a 2-es protokollú ismétlésrögzítést és új telefonos idővonal indítását már a natív végrehajtónak adja át. Stabil gyakorlat-/sorozat-ID, generáció és külön végrehajtási revízió azonosítja a célt. A régi órás protokoll és a nem támogatott mérések a korábbi útvonalon maradnak.
- A rögzítés, a bizonylat és az előkészített következő idővonal egy SQLite-tranzakcióban mentődik. A terv állapota `pending`, `started` vagy `interrupted`; az ismételt kézbesítés nem indít második órát és nem nullázza az eltelt időt. A gyakorlat saját pihenője felülírja az általános beállítást.
- A telefonon látható edzés előre indítja az automata vezérlőt a meglévő foreground szolgáltatásban. Háttérből új foreground szolgáltatás indítására nem támaszkodunk. Az értesítésből leállítható, az edzés lezárásakor megszűnik; az előkészített vezérlés felső időkorlátja 24 óra. Megszakadás után nincs automatikus utólagos munka vagy rezgéssorozat.
- A JavaScript előbb összevezeti a natív eredményt, egy mentésben tárolja a piszkozatot és az eseménykurzort, majd nyugtáz és publikál. A nyitott szerkesztőt és élő sorozatreferenciákat megtartja. Újabb, ütköző telefonos értéket nem ír felül; sikertelen mentésnél nincs nyugta. Restore és telefonos befejezés körül natív írási védelem működik.
- A `startTimed` a mentett intervallumbeállítást használja; nem keres HTML-űrlapot. Az óra csak a támogatott, telefon által vezérelt műveletekhez adja az új protokollmezőket.

**Helyi ellenőrzés:** 34 Android 33 Robolectric/JUnit teszt a rögzítési tárolóra, tervekre és valódi natív szolgáltatáslogikára; 12 Wear JVM modell-/protokollteszt; 139 Node-regressziós csomag, köztük a teljes telefonos runtime új összevezetési és űrlapfüggetlen indítási próbái. A Java integráció API 35 osztályokkal és a valódi Capacitor/AndroidX/Play Services API-kkal fordult. A helyi izolált szolgáltatásharness generált erőforráskötéseket és az érintetlen GPS-szolgáltatás helyettesítőjét használja: ez **nem Gradle APK-build, fizikai Data Layer vagy készülékes elfogadás**.

**A 6/10-es lépésben folytatandó:** munka/pihenő/szünet teljes natív időnaplója, időalapú befejezések tartós beolvasztása, explicit telefonos javítás/visszavonás és azok ütközéskezelése. A már rögzített értékeket a jelenlegi előkészítési API nem engedi egyszerű pillanatkép-felülírással módosítani. Amíg az időalapú eredmény nincs összevezetve, a natív vezérlő nem engedi egy új menetrenddel felülírni. A GPS-tulajdonos előkészítése továbbra is külön feladat; a távolsági edzés meglévő útvonala marad használatban.

**GitHub / kiadás:** mindez egyelőre helyi, ellenőrizhető fejlesztési részeredmény. A #195 továbbra is draft, a remote fej `e528d1e`; a main, a kiadások és az **1.3.4 /2730** verzió nem változik. Nincs kódpush, közös Release Gate vagy új APK. A #162 és #157 nem zárható a mostani teszteredményből.

### Előző helyi lépés — 4/10: natív rögzítési alap (`3fff44c`)

A #195 `e528d1e` forrásából külön helyi munkafában készült a `PhoneWorkoutExecutionStore` és `PhoneWorkoutRecording`. A külön SQLite-adatbázis egy tranzakcióban menti a sorozatot és az idempotens parancsbizonylatot; generáció/revízió és stabil sorozat-ID védi a célt. A JavaScript által később beolvasandó események külön nyugtázhatók, a parancsbizonylat akkor is megmarad. A már rögzített sorozat értékeit egy elavult pillanatkép nem módosíthatja. A restore-zárolás, lezárt edzés és hiányzó protokollmezők védelme is része az alapnak.

**Hatókör:** kézi, ismétléses sorozat natív adatrögzítési alapja. A 4/10-es lépés végén a szolgáltatás/órás parancs/WebView még nem volt bekötve; a pihenő- és idővonalindítás, időalapú/távolsági sorozat és egységes időnapló későbbi lépés. Ez még nem oldja meg a #162 készülékes háttérműködését, és nem zárja a #157-et.

**Ellenőrzés:** 18 sikeres helyi Robolectric/JUnit teszt Android 33 környezetben, az új forrásokat Java 17 célra fordítva. Valós SQLite-tranzakció visszagörgetése injektált írási hibánál, újranyitás előtti/utáni parancsismétlés, párhuzamos duplikáció, részleges eseménynyugtázás, stale pillanatkép, értékfelülírás, restore-zár, generációváltás és valódi nulla vizsgálva. A helyi futtató nem a teljes Android Release Gate; APK nem készült.

A kód és ez az állapotfeljegyzés egyelőre helyi commit; a #195 remote fejét, main-t és verziót ez a lépés nem módosítja. A kódpush a már rögzített közös gate-szakaszban történik.


A közös `fix/p0-162-157-safe-wear-sync` munkában a következő **részjavítások** készültek; ezek önmagukban nem jelentik a két Issue végleges teljesítését:

- **#162, natív adatbiztonság:** a telefon órás parancssora nem dobhatja el észrevétlenül a legrégebbi, még nem nyugtázott sorozatrögzítést, amikor eléri a 64 elemnyi keretet. A Data Layer tétel nyitva marad, és a kapacitás felszabadulása után újra felolvasható. A nyugtázás és duplikációvédelmi jelző tartós mentést használ; sikertelen tartós nyugtázásnál a Data Layer tétel nem törlődik. Robolectric regressziók készültek a telítődésre, újrafelvételre és az automata állapotpillanatkép cseréjére.
- **#157, gyors edzés:** a meglévő telefonos inline gyorsindításban ellenőrizendő az előbb snapshot, utána egyszeri óranyitás sorrend. A célzott Node-regresszió kiegészült sikertelen küldés/gyors helyreállás és visszautasított óranyitás tesztjével; nincs új óranyitás puszta polling miatt.

**Még nyitott:** #162-ben a fenti helyi natív integráció teljes körű ellenőrzése, az időalapú eredmények és a munka/pihenő/szünet egységes natív naplója; #157-ben a tényleges gyorsedzés-indítás és friss mérés ellenőrzése fizikai órán. A #162 és #157 Issue egyelőre **nyitva marad**.

**Készülékes elfogadás (mindkét feladathoz):** új telefonos gyors edzés → óra app egyszer nyílik → friss HR/kcal indul → telefon és óra lezárva mérés folytatódik → sorozatrögzítés, pihenő, kapcsolatvesztés/visszatérés → egyetlen naplóbejegyzés és külön eredetű mért adatok. Ismételjük meg tervezett edzéssel, önálló órás és önálló telefonos indítással; rögzítsük a tényleges eredményeket az Issue-kban.

**Kiadási szabály:** a felhasználó kérésére a közös release gate eredményét és az új teszt-APK-kat **csak a #162 és #157 végrehajtása után** szabad átadni; verziószámot előzetes jóváhagyás nélkül nem változtatunk. A CI-eredmény és a fizikai készülékes elfogadás külön igazolandó.

## Mentési javítás — #180

A #179 mainre került (`c623af7`), a #164 és #178 is egyesítve. A [#180](https://github.com/fuloplevente1998/TrainPilot/issues/180) új felhasználói visszajelzése: aktív edzés nélkül a JSON-export „Érvénytelen vagy túl nagy mentés”, a ZIP „hiányzó fotó” hibát ad; a fotók a telefonon megnyithatók.

A javítás kompakt, veszteségmentes JSON-t exportál, és a 20 MiB korlátot UTF-8 bájtban ellenőrzi. A ZIP az élő fotóhivatkozásokat előzetesen ellenőrzi, a hiányzó/üres fájlt a meglévő Google-fiókból próbálja visszatölteni, majd újra ellenőrzi. A hiányzó, üres és 6 MiB-nál nagyobb eredeti képfájlt külön, négy nyelven és az edzés azonosításával jelzi; a nagy kép előnézete megnyitható lehet. A képek és egészségadatok nem maradhatnak ki csendben. A 20 MiB JSON / 6 MiB kép / 500 MiB archívum korlát és a korábbi visszaállítási formátum megmarad.

**Készülékes mentés elfogadva 2026. október 13-án:** a felhasználó visszajelzése: „rendben van a mentés is”. A #180 mentési hibajegy lezárható; a következő feladat az automata működés készülékes elfogadása és továbbfejlesztése, majd a #163 térkép. Külön kézi visszaállítási próbáról nem érkezett visszajelzés.

## Aktuális main és következő fejlesztés

A felhasználó 2026. október 13-án kérte az aktuális main, GitHub-leírások és következő fejlesztések aktualizálását. A [#179](https://github.com/fuloplevente1998/TrainPilot/pull/179) egyesíti a #164 telefonos automata módját és a #178 Wear-kezdőlapját, valamint az új órás automata integrációt. Ez a további fejlesztés alapja; a készülékes automata elfogadás külön feladat.

| Terület | Állapot |
| --- | --- |
| Verzió | Main és legutóbbi tesztpár: **1.3.4 / 2730**; a verzió önmagában nem azonosítja a buildet. |
| Korábbi átadott telefonos referencia | `16509065474cf76ee1222985e9155b6608f479f9`: automata mód, 3–2–1 jelzés + 1 s rezgés, alsó számláló, natív zoomtiltás és órás távoli vezérlés. A telefonos forrás változatlan az órás `e3db5e2` commitban. |
| Korábbi átadott Wear referencia | `e3db5e20015bce137733763c50c0bf0c521ddf13`: megújult kezdőlap, kompakt pontos idő, natív automata szolgáltatás és kis körkijelzőre illesztett időzítő. |
| Kiinduló elfogadott tesztbuild-pár | Telefon `3e66ab0`, Wear `e41f09f`; ezekre épül a közös #179. |
| Automata mód #162 | Első közös megvalósítás a main fejlesztési alapjában. Órán indított edzésnél önálló natív órás időzítés; telefonon indítottnál telefonos tulajdon. |
| Wear-kezdőlap #170 | Első revamp megvalósítva; telefonos ikonok/téma, külön Beállítások, közvetlen törlés, felfelé húzható menü, Mini napló tördelése. Készülékes finomítás következhet. |
| Mérés és napló | Élő HR/kcal és elérhető kardióadat, Mini napló, elkülönített források és késői összesítés megmarad. |
| Automatikus ellenőrzés | 138/138 telefonos regresszió (a #180 mentésjavítással) és külön futtatótesztek; 90 Chromium-tesztcsomag; 20 célzott Kotlin állapot-/tükrözési/pihenőkör teszt; 51 Wear-emulátoros teszt; natív release gate-ek, teljesítménykapu és ellenőrzött aláírás. |
| Következő feladat | Készülékes automata próba, majd telefonos háttérrögzítés/új idővonal indítása az óráról és egységes munka/pihenő naplóbontás. |
| Utána: térkép #163 | Telefonos GPS-útvonal rögzítése és naplóbeli térképe; a koordináták rögzítése még nincs implementálva. |
| Kényelmi Wear-funkciók | Visszavonás, érintészár, forgatható szerkesztés, egyértelműbb mentési/szinkronállapot a közös tervben. |

A kanonikus következő lépések: [WEAR_OS_FEJLESZTESI_TERV.md](docs/wear/WEAR_OS_FEJLESZTESI_TERV.md). Új beszélgetéshez: [CHATGPT_FOLYTATAS.md](docs/CHATGPT_FOLYTATAS.md).

### Ellenőrzött tesztpár és kiadások

- [Telefonos tesztbuild / teljes gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352741468), APK SHA-256: `9ea25bb103d0aac6d862824cb18caa0082b69df735f3260512bfec4945178041`.
- [Wear tesztbuild / natív gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812467), APK SHA-256: `3f50d3bb36f0c0b31d731da6e3322e7cc3445308442860a7a630b79ba00db818`.
- [Wear 51 emulátoros teszt](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812492): 160/192/225 dp körkijelző, nagyított rendszerbetű és valódi időzítő háttérbe tett Activity mellett.
- Mindkét APK a korábbi kiadások tanúsítványával aláírt; a csomagazonosító `com.repforge.app`. Mindkettő telepítése és az első telefonos/órás adatszinkron szükséges az új közös protokollhoz.

Az eredeti [telefonos 1.3.4](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.4) és [Wear 1.3.4](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.4) publikált APK-ja a korábbi kiadott állapot. A main frissítése és az új tesztpár készülékes elfogadása külön történik; a jelen frissítés nem cseréli le a kiadott APK-kat és nem emeli a verziószámot.

### Aktuális pihenőjavítások — #179

A telefonon a kompakt időzítő Munka/Pihenő felirata megmarad a váltáskor. Az órás pihenőkör a tényleges automata szakasz teljes idejéből számol, nem az edzés alapértelmezett pihenőértékéből; 120 mp-nél az első másodperctől 1/120 részt fogyaszt. A telefonos kezdeti gyakorlatközi pihenő hossza is helyesen kerül át az órára. A fenti referencia-APK-k e javítások előtti átadott változatok; az aktuális main csomagok és CI az [Actionsön](https://github.com/fuloplevente1998/TrainPilot/actions?query=branch%3Amain) azonosíthatók.

### Ismert nyitott korlátok

Telefonon indított edzésnél az órás ismétlésrögzítés és egy új telefonos menetrend óráról indítása még várhat az alvó telefonos webfelület visszatérésére. Az óra ilyenkor „Telefonra vár” állapotot mutat. A három használati irány teljes készülékes elfogadása ezért még hátravan.

Az órás időzített sorozat a tényleges munka másodperceit összegzi; a meglévő telefonos intervallum a korábbi munka+pihenő értéket használja. Az egységes naplóbontás külön folytatás; régi adatot nem szabad találgatással átírni. Oldalankénti időmérés kézi marad, utána automatikus pihenővel. Órás GPS-szakaszkezelés és útvonaltárolás további feladat.

## Összevont verziók

| Verzió | Fejlesztési tartalom |
| --- | --- |
| [1.0](RELEASE_NOTES.md#10) | Alap edzéstervező/napló, Coach, fejlődés, fotók, mentés, kardió és közös szerkesztők. |
| [1.1](RELEASE_NOTES.md#11) | Célok, előnézetes újratervezés, beltéri intervallumok, Coach-indoklás; külön kezelt korábbi Bluetooth-kísérletek. |
| [1.2](RELEASE_NOTES.md#12) | Egészségnapló, profilvezérelt programok, naptár/felület/indítás javításai és Wear OS alapok. |
| [1.3](RELEASE_NOTES.md#13) | Párosított gyors edzés, Mini napló, mért edzések előzménye, egészségforrások és a telefonos órás appindítás tesztelt javítása. |

A részletes felhasználói változáslista a [RELEASE_NOTES.md](RELEASE_NOTES.md). A régi, kiadásonkénti [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) jegyzetek történeti/buildtechnikai archívumok; a bennük rögzített akkori jelöltstátusz nem írja felül ezt az aktuális állapotot.

## Következő készülékes ellenőrzés

A tesztpár telepítése és első szinkron után külön telefonos, külön órás és párosított indítással próbálandó:

- Súlyzós kézi rögzítés → pihenő → következő sorozat/gyakorlat; az app nem találhat ki ismétlést.
- Időalapú munka/pihenő/körök, nulla pihenő, utolsó sorozat és kézi végső mentés.
- 3–2–1: három 80 ms-os előjelzés; lejáratkor egy 1 másodperces, egybefüggő rezgés.
- Lezárt kijelző, háttérbe tett app, szünet/folytatás/kihagyás, értesítési vezérlés, kapcsolatvesztés és visszatérés.
- Telefonos GPS/intervallum együtt; újraindítás és ugyanahhoz a sorozathoz történő helyreállítás.
- Kis kerek kijelző, nagy betű, szerkesztő, hosszú név és a végső naplóadatok egyezése.

A mainre emelés felhasználói engedélye nem helyettesíti e próbák eredményét; a tényleges tapasztalatot az issue-ban és a közös tervben kell rögzíteni.

## Megőrzendő adatkezelési szabályok

- A mentett napló, saját programok, fotók és meglévő egészségadatok nem veszhetnek el frissítéskor.
- Azonos napi, eltérő edzések megmaradnak. Valódi másolat összevonásakor az időablakot és a teljes edzést kell összevetni; dátum alapján nem szabad törölni.
- Stabil azonosító, órás visszaigazolás és törlésjelző akadályozza meg az ismételt mentést és a törölt bejegyzés visszaéledését.
- A Drive-egyeztetés megőrzi a helyi/távoli módosításokat és törléseket; a sikertelen helyreállítás visszaállítja az előző állapotot.
- Késői Health- vagy Wear-adat ugyanahhoz az edzésazonosítóhoz kapcsolódik, új bejegyzést nem hoz létre.
- A TrainPilot Wear, Health Connect és Samsung Health értékei forrásonként külön maradnak. Mért nulla megmarad, hiányzó adat nem becsülhető.
- A teljes natív egészségnapló kézi mentése külön hozzájárulásos; nem kerül automatikusan Drive-pillanatképbe. A fotók privát app-tárhelyen maradnak.
- GPS csak összesített időt/távolságot ad át a naplónak; útvonal-koordinátát nem tesz a mentésbe.

## Megőrzendő felületi működés

A Napló kompakt, összecsukott edzésösszefoglalót és lenyitáskor betöltődő gyakorlatrészleteket használ. A szerkesztő sorozathozzáadást/-törlést és egyértelmű Mentés/Mégse műveleteket ad. A fotófolyamat kamera/galéria/mégse lehetősége és privát tárolása megmarad.

A pulzusnézet valódi hét naptári napot kezel; a hiányzó és nulla érték jelentése külön marad. A Coach súlyos, testsúlyos, időalapú és kétoldalas gyakorlatokhoz is a teljes releváns naplót használja; mesterséges sorlimit nem vághatja le a trendet. A mentési visszajelzés közös, a lenyitásnyilak egységesek, a tényleges indításikonok külön funkciót jelölnek. Az alap témák mattak, az élénk témák glow/neon színei statikusak.

## Alkalmazásváltozások kiadási folyamata

1. Az aktuális, mainen elfogadott állapotból induló célzott ág és világos hibaleírás/elfogadási feltétel.
2. Az érintett működés regressziója; telefonos változásnál a teljes Node-, Chromium- és natív kiadási ellenőrzés, változatlan teljesítménykövetelményekkel.
3. Aláírt APK, forráscommit, csomagadat, verzió és aláíró tanúsítvány egyezésének ellenőrzése. Közös protokollnál a telefon és a Wear együtt ellenőrzendő.
4. A megfelelő párosított teszt-APK-k átadása, a CI-eredmények és a fizikai készülékes tapasztalat külön rögzítése.
5. Felhasználói jóváhagyás után mainre emelés, a main kiadási ellenőrzése és a hozzá tartozó release. Kifejezett publikálási jóváhagyás nem jelent automatikusan minden készülékes teszt teljesítését.

A leírások karbantartása nem módosítja az APK verzióját és nem emel automatikusan alkalmazáskódot a mainre.

## Fejlesztői hivatkozások

- [Build és signing](docs/BUILD.md)
- [UI-tesztek](docs/UI_TESTS.md)
- [Teljesítményregressziós szabályok](docs/PERFORMANCE_REGRESSION_POLICY.md)
- [Wear OS architektúra és ellenőrzés](docs/WEAR_OS.md)
- [Profilvezérelt edzésrendszer](docs/profile-training-128.md)
- [Korábbi fázisterv, történeti referenciaként](docs/PHASE_5_6_7_ROADMAP.md)
- [A korábbi részletes állapotfeljegyzés](https://github.com/fuloplevente1998/TrainPilot/blob/2fdcf4fde7021fb234bd85fff0a59b698d36c808/DEVELOPMENT_STATUS.md)
