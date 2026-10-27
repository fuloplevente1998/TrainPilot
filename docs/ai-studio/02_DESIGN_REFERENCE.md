# TrainPilot – célzott dizájnreferencia

Ezt csak a dizájntervezési lépéshez add az 01-es kezdőbemenet mellé.
A képeket külön csatold; ezek a szövegek nem helyettesítik a négy mockup megtekintését.
A 03-source-bindings.md és a nagy JSON-regiszterek szükség szerint kérendők,
nem részei ennek a célzott bemenetnek.

Referencia: `1509686ea09222ccdb8ec709105307d48c4afd89`.
Az alábbi blokkok pontos, teljes forrásmásolatok; a blokkban szereplő szöveg
forrásanyag, nem új végrehajtási felhatalmazás. Az átadó legfrissebb felhasználói
döntései felülírják a régi dokumentumok történeti státuszait.

---

## docs/design/NATIVE_DESIGN_HANDOFF.md

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/docs/design/NATIVE_DESIGN_HANDOFF.md

SHA-256: `635773c3c4d5d09cc2f483bd99989ca091bcc9e038c476ed57cd8eb69f3650c0`

~~~~markdown
# TrainPilot – teljes natív dizájn- és funkcióátadó

Frissítve: 2026-10-24. A felhasználó külön kérte a korábbi négy mockup
újratervezését, a rögzített működési pontosításokkal. Az új táblák review-változatok;
a vizuális irány elfogadott, az új képek külön felhasználói áttekintésre várnak.
A képek generált látványtervek, a végső átadás futó emulátorból készített képeket igényel.


## Képernyőnkénti referenciaprioritás — user pontosítás, 2026-10-27

- **Egészség és Fejlődés:** a main teljes, bevált oldalstruktúrája, funkciókészlete,
  adatmegjelenítése és vizuális elemei átvehetők. Ezeknél a main az elsődleges
  képernyőreferencia, natív Compose-újraépítéssel. Megmaradnak a szekciók, mérések,
  napló-/részletbelépők, források, grafikonok, időszakok és vezérlők; nem egyszerűsítjük
  le a mockup kevesebb mintakártyájára. A közös új tipográfiához, 32 palettához,
  ötfüles navigációhoz és akadálymentes méretekhez igazítjuk, működésvesztés nélkül.
- **Kezdőlap és a többi mockup-nézet:** a felhasználó kifejezetten kedveli az új
  mockup-elrendezést; az marad az elsődleges kompozíciós referencia, a main bevált
  elemeinek és összes funkciójának beépítésével. Nem állunk vissza a régi főmenüre.
- A referencia nem szintetikus adatok átvétele: a valódi natív state/provider adja
  az értékeket, a hiányállapot is őszinte és kidolgozott. A főoldalak megfelelő
  módon kapcsolódnak a Settingshez, a részletekhez és a meglévő műveletekhez.

Ez pontosítja a lenti általános mockup-minőség szabályt: a minőségi elvárás
mindenhol érvényes, a Health/Progress oldalakat viszont nem kell más elrendezésre
kényszeríteni. Új adatlogika, képlet, engine vagy WebView-visszavezetés ebből nem következik.
A két külső UI-chat tervezett átadójában is ezt a prioritást kell használni.

## Main-elemek tudatos újrahasznosítása — felhasználói döntés, 2026-10-27

A mainben már bevált és elfogadott dizájnelemek átvehetők, majd az új natív
mockupokhoz igazítandók. Ez az elsődleges kiindulás ott, ahol használható mintánk
van; nem kell mindent újratervezni. A formákat és interakciókat Compose-ban
valósítjuk meg, a meglévő natív state/callback bekötésével.

A fő navigáció továbbra az új ötfüles elrendezés; a main bevált elemei ebbe az
új szerkezetbe kerülnek. A mockupok részletességi/minőségi elvárása és a main teljes
funkciókészlete együtt érvényes. A korábbi köztes előnézet ettől még nem elfogadott.

### Konkrét újrahasznosítási leltár

Olvasott main-forrás: `7473b30f387eda54186c1e31830304ca12bd104f`, csak olvasásra.
A CSS-hivatkozások mintaforrások: az `index.html:2` tényleges stylesheet-sorrendje,
a későbbi felülírások és a renderelt állapot együtt adják az érvényes látványt.
Egy régi CSS-részlet nyers színkódját vagy betűméretét nem másoljuk vakon.

| Bevált elem | Main forrásjelölt | Natív adaptáció |
|---|---|---|
| Matt grafit felületek, finom kontúrok, kiválasztott hangsúly | `www/global-uiux-177.css:1–55`, `.card/.hero/.stat`, `.active-program` | Közös SurfaceCard és hero/selected szerepek, 32 palettából származó kontrasztos tokenek. A kijelölés felirattal/ikonnal is látszik. |
| Erős főgomb, sötét másodlagos gomb, veszélyes művelet | `www/styles.css:26–28`, `.btn/.secondary/.danger` | Egységes Compose gombcsalád, mockup szerinti hangsúly, teljes felirat, minimum48dp és tartalommal növekvő méret. |
| Sorszámblokk, kész állapot, kompakt sorozatsor és jelvény | `www/styles.css:30–39`, `.num/.check/.badge` | Ismerős súly/ismétlés/RIR és kész/visszavonás interakció, új mockup szerinti elrendezéssel; a meglévő motor és auto-rögzítés változatlan. |
| Egészségmérési és forrás-/állapotkártyák | `www/health-dashboard-168.css:14–31`, `.tp168-metric/.tp168-status` + globális felülírás | Ikon, nagy szám, kisebb egység, dátum/forrás/frissesség. Mockup-szintű kidolgozás, kis képernyő/nagy font mellett tördelődő rács. Minden meglévő mérés elérhető marad. |
| Naplókártya és szekcionált beállítások | `www/styles.css:41`, `www/global-uiux-177.css:26–35`, `.history/.setting` | Tartalmi hierarchia, lenyitható részletek, jól elkülönített műveletek; natív paging/állapotmegőrzés és adatút megtartása. |
| Fejlődés időszakválasztója és grafikonpanelek | `www/progress-177.css:21–46`, `.tp177-volume-panel/.tp177-periods` | Bevált időszakválasztás az új grafikon- és metrikakártyákban; ugyanazok az adatok és számítások. |
| Térkép és egyértelmű bezárás | `www/gps-route-map-163.css:4–7`, `.tp163-route-dialog/.tp163-close` | Mockup szerinti nagy térképfelület és piros X; csak a térkép záródik. Az online réteg bekötése külön G06 funkciófeladat. |
| Olvasható, karakterhelyes tipográfia | `www/typography-137.css:1–8`, helyi Inter betűkészlet | A bevált tipográfiai karaktert követjük; natívan használható fontasset és licenc ellenőrzése után közös FontFamily, nem képernyőnként eltérő betűk. |

Minden következő képernyőterv röviden rögzítse: **mit veszünk át a mainből,
hogyan illesztjük a mockuphoz, mi változik a használatban, és mivel ellenőrizzük**.
A komponenskönyvtár, a két chat átadója és a vizuális review ugyanazt a leltárt
használja; az újrahasznosítás az egységességet és a részletességet szolgálja.

## Kötelező minőségi mérce — felhasználói pontosítás, 2026-10-27

**A main működése, funkciókészlete és használhatósága a minimum. A négy elfogadott
mockup vizuális kidolgozottságát legalább el kell érni, lehetőleg túl kell lépni.**
A korábban megmutatott natív alapállapot/köztes előnézet ezt még nem teljesíti;
a felhasználó nem fogadta el végleges vagy kielégítő vizuális eredményként.
Most a rögzített tervek további pontosítása az aktív feladat; új UI-kódot ebben a
pontosító körben nem írunk. A meglévő P1 kód kísérleti jelölt, nem jóváhagyott irány.

A meglévő 24 nézet részletes leírása és a final-ui-ux specifikáció érvényes marad.
Nem új látványtervet vagy eltérő irányt találunk ki, és nem csökkentjük a korábban
rögzített feladatot általános Material-kártyák, betűméret- vagy színcsere szintjére.
A P0 komponensrendszer és a zöld CI szükséges alap; önmagában egyik sem vizuális elfogadás.

### Képernyőnkénti elfogadási minimum

- A mockup szerinti információhierarchia, kidolgozott hero-/akció-/mérési kártyák,
  ikonkezelés, állapotjelzések, tagolás és kiegyensúlyozott képernyőkitöltés.
- A main összes releváns művelete és adata megtalálható és használható; kevesebb
  navigációs oldal megengedett, kevesebb funkció vagy elvesző szerkesztés nem.
- Az aktuális edzés/indítás és a gyakori műveletek elsőbbséget kapnak; a sűrű adatok
  olvashatók, a ritkák rendezett részletekben elérhetők. Nincs nagy üres tér vagy
  egymást ismétlő cím/gomb csak azért, mert egyszerűbb volt így bekötni.
- A grafit/arany referencia minősége mind a 32 palettával és HU/EN/DE/RO felirattal
  megmarad; 320dp, nagy betű, üres/töltő/hiba/engedélyhiányos állapot is tervezett.
- A mockup mintaszámai, Watch-modellje, képei nem válhatnak kitalált élő adattá.
  Valódi hiány esetén is kidolgozott, őszinte állapot kell; a funkcióhiány külön tétel.
- Azonos valódi állapotról készült, módosítatlan emulátorkép összevetése a mockuppal
  és a main megfelelő használati útjával. A tesztzöld és egy létező PNG nem elég:
  a látvány és a használhatóság is meg kell feleljen a fenti minőségi mércének.

### A mostani két jelölt még hiányzó vizuális kidolgozása

| Nézet | Következő tervezési pontosítás |
|---|---|
| Kezdőlap | Teljes aktív/mai program hero komponenshierarchia, erős főművelet, vizuálisan megkülönböztetett gyorsindítás és óraállapot, egységes ikonozott mérési kártyák és forrásjelzések; minden meglévő mérés megtartva. Nem pusztán a jelenlegi dobozok átrendezése. |
| Edzés | A mockup gyorsindító kártyái, gyakorlatválasztás és kültéri mód külön karaktere, aktuális program napjainak átlátható kártyái, kompakt Programok/Naptár/Coach belépők. A három egymás alatti általános gomb önmagában nem kész dizájn. |
| További főoldalak/aktív módok | A már rögzített 02-screen-specifications/24 nézet szerint kidolgozva, azonos közös komponensekkel; nem új egymástól eltérő képernyőstílusok. |

A két külső chatnek szánt átadó egyelőre tervezet. Kiosztás előtt e minőségi
mércét és a képernyőspecifikációt be kell építeni a feladataikba. A közös rendszer
és a végső vizuális review egy gazdánál marad; a párhuzamosítás nem minőségi engedmény.

## Aktuális termékirány – felhasználói pontosítás, 2026-10-26

A WebView-app a minőségi minimum; a natív app egyértelmű használhatósági és
vizuális előrelépés legyen. A jóváhagyott grafit/arany referencia, az ötfüles
navigáció, mindegyik főoldal és aktív edzés teljes kidolgozása kötelező.
Az edzésmotorhoz/adatmegőrzéshez vizuális átalakítás címén nem nyúlunk.
Az elfogadás alapja valódi, futó appból származó emulátorkép és folyamatellenőrzés.
Külön Codex előbb ütközésmentes specifikációt készíthet; production UI csak a
mostani telefonos QA-terület feloldása után. Részletes munkaszerződés:
[Native final UI/UX Codex handoff](../plans/NATIVE_FINAL_UI_UX_CODEX_HANDOFF.md).

### Az új mockupok és a WebView-elemek kapcsolata

A felhasználó megerősítette: az új, ötfüles alsó navigációjú mockupok legyenek
az elsődleges vizuális referencia. Modern, részletgazdag, mégis áttekinthető
felület a cél. A WebView-os main bevált kártyái, adatmegjelenítései, vezérlői,
feliratai és ismerős interakciói indokoltan átvehetők, az új dizájnba illesztve.
Képernyőnként jelölni kell, mely bevált elem marad meg és hogyan illeszkedik
az új mockup felépítéséhez. A teljes funkciómegőrzés mellett a használatban már
bevált megoldások folytonossága is tervezési követelmény. Az ötfüles navigáció
és az új grafit/arany vizuális rendszer marad az alap; a további témák is megmaradnak.

## Közös felépítés

- Öt alsó fül: **Kezdőlap, Edzés, Napló, Fejlődés, Egészség**.
- Jobb felső **Beállítások fogaskerék** minden főoldalon és aktív edzésben.
  Vissza ugyanahhoz a képernyőhöz/sessionhöz; megnyitása nem pause vagy új edzés.
- Sötét grafit/arany bemutató, konzisztens natív Compose kártyák és ikonok,
  jól olvasható szöveg, kompakt elrendezés, legalább48 dp érintési célok.
- **32 megőrzött paletta:** 26 main Basic/Vivid +6 natív kompatibilitási paletta.
  Minden szín tématokenből jön; a mentett felhasználói választás érvényesül.
- HU/EN/DE/RO; nagy betűméret és kis kijelző mellett tördelődő vezérlők.
- A mockup számai illusztrációk. Darabszám, dátum, állapot és mérés valódi adatból
  jön. A beépített katalógus jelenlegi forrása106 gyakorlat és13 program; az egyedi
  tartalom miatt a tényleges listák ennél nagyobbak lehetnek.

## Funkciómegőrzés – a 24 kép nem képernyő- vagy funkciólimit

Felhasználói pontosítás: az egészségnapló, testsúly, pulzus és a main többi
meglévő funkciója a natív appban is megmarad. A mockup áttekintő főnézeteket
mutat; az ott nem ábrázolt részletek nem törölhetők a termékből. Az új
felület kevesebb önálló oldalba, közös fülekbe/panelekbe vonhatja össze a funkciókat.

| Terület | A főnézeten túl is megőrzendő |
| --- | --- |
| Egészségnapló | Korábbi napok/időszakok, metrikák részletei, mért értékek, forrás és időbélyeg; teljes main-metrikakészlet |
| Testsúly | Mentett mérések, kézi rögzítés, időszakszűrés, változás és grafikon; a szolgáltatói testsúly forrása elkülönül |
| Pulzus | Napi átlag/nyugalmi/minimum/maximum és meglévő előzmények; valódi élő Watch/BLE adat külön állapot |
| További egészségadatok | Alvás/HRV, energia, lépések, kalóriák, testösszetétel és minden egyéb, mainben elérhető adat; hiányzó engedély/mérés jelölve |
| Kapcsolatok | Samsung Health/Health Connect, BLE és Wear meglévő beállításai és állapotai |
| Egyéb main-funkciók | Program-/gyakorlatszerkesztés, napló/részletek/szűrők, fotók, backup, naptár és minden létező beállítás külön lefedettségi ellenőrzése |

Átadás előtt main-funkció → natív útvonal → adatforrás → teszt megfeleltetés kell.
A mockupon hiányzó funkció nem jelölhető késznek pusztán adatmodell vagy motor alapján.

## Teljes main-funkcióleltár és az új elérési helyük

**Elsőbbségi termékdöntés:** a jelenlegi main összes meglévő, felhasználó számára
elérhető funkcióját és adatát megőrizzük. Az elrendezés, felirat és navigáció
megváltozhat; több régi oldal egy új oldal fülébe, lenyitható részébe vagy
szerkesztőpaneljébe kerülhet. Kevesebb önálló oldal a cél, teljes funkcionalitással.
A 24 telefonkép nézetminta, nem24 kötelező külön navigációs útvonal.

Ellenőrzött referencia: main `8a36502` (alkalmazásbaseline `7473b30`, elfogadott
1.3.71 /2735). Funkcióforrás: a rögzített `7473b30:www/app.js` végleges
felülírásaival, a kapcsolódó Android/Wear működéssel. A main dokumentációs
HEAD-je nem jelent új alkalmazásverziót. A táblázat **megőrzési követelmény és
elhelyezési terv**, nem készre jelentett natív funkciólista.

### Öt fő hely, összevont részletekkel

| Fő hely | Egy helyen elérhető funkciócsoportok |
| --- | --- |
| **Kezdőlap** | Mai/ajánlott edzés, aktív vagy félbehagyott edzés folytatása, gyors indítás, óraállapot, egészség-összefoglaló |
| **Edzés** | Indítás, programok és programszerkesztő, gyakorlatkönyvtár/kedvencek/saját gyakorlatok, aktív edzés és mérési módok, naptár, Coach |
| **Napló** | Kereshető/szűrhető edzéselőzmény; egy edzés adatainak, forrásainak, fotóinak, útvonalának és visszajelzésének részletei |
| **Fejlődés** | Teljesítmény-, mennyiség-, izomcsoport- és PR-nézet; testsúlytrend gyorseléréssel az Egészség testsúlynaplójára |
| **Egészség** | Áttekintés, teljes egészségnapló, metrikarészletek, testsúlynapló, adatforrások/eszközök. Ezek lehetnek belső fülek és panelek |
| **Globális fogaskerék** | Profil, megjelenés, nyelv, időzítő/haptika, edzésmódok, kapcsolatok, mentés/szinkron, engedélyek/adatvédelem, alkalmazásadatok |

Naptár/Coach/Programok gyorselérés a Kezdőlapon és az Edzésen marad. Ugyanazt
a kanonikus funkciót nyitják; a több belépési pont nem jelent külön adatmotort.

### A. Indítás, profil, programok, gyakorlatkönyvtár

| ID | Megőrzendő main-funkció | Natív elhelyezés |
| --- | --- | --- |
| A01 | Első indítás, profil/onboarding és annak kihagyása/folytatása | Kezdőlap elsőindítási panel + Beállítások → Profil |
| A02 | Edzéscél, tapasztalat, felszerelés, helyszín, időkeret és meglévő egyéni korlátozások | Közös profil-/tervezőpanel; a main által már használt adatokkal |
| A03 | Mai/ajánlott programnap és következő edzés | Kezdőlap; Edzés → Aktuális program; Coach |
| A04 | Gyors edzés, gyakorlatválasztás és kültéri aktivitások | Edzés tetején, Kezdőlap gyorsindításból is |
| A05 | Aktív/paused/native/importált draft azonos edzésként folytatása | Kezdőlap Folytatás-kártya és aktív edzésjelzősáv |
| A06 | Minden beépített és saját program, aktiválás, napok megtekintése és indítása | Edzés → Programok, közös lista/részlet/szerkesztő |
| A07 | Program létrehozása/másolása/szerkesztése/törlése és a mainben létező visszaállítási műveletek | Programlista műveletmenü és programszerkesztő; egyedi adatok védelmével |
| A08 | Programnapok, gyakorlatválasztás/csere/kizárás és sorrend, sets/reps/súly/pihenő/prescription | Egy programszerkesztő napfülekkel és lenyitható gyakorlatkártyákkal |
| A09 | Adaptív/személyes tervgenerálás, előnézet, felszereléshez és korlátozáshoz illesztés, tudatos elfogadás | Programok → Saját terv, profilhoz kötött szerkesztő/előnézet |
| A10 | A jóváhagyott korábbi programelőzmények használata és változatkompatibilitás | Tervelőnézet/Coach magyarázat; háttérben meglévő history128-szabályok |
| A11 | Teljes gyakorlatkatalógus, keresés és meglévő szűrők/kategóriák | Edzés → Gyakorlatok, közös kereshető lista |
| A12 | **Gyakorlatkedvencek**, hozzáadás/eltávolítás és megőrzés backupban | Könyvtár Kedvencek szűrő és részletoldali csillag/szív. A mockupból kimaradt, de a mainben létezik |
| A13 | Saját gyakorlat létrehozása/szerkesztése és meglévő törlési lehetősége | Ugyanazon könyvtár +/műveletmenü, közös szerkesztő |
| A14 | Ismétléses/időalapú/kétoldali/idő+táv gyakorlat, súlyértelmezés és felszerelés | Gyakorlatszerkesztő és részletek; kg/kéz, összsúly, egy súlyzó és testsúly helyesen |
| A15 | Gyakorlatleírás, célizmok, meglévő bemutatóvideó/forrás és annak ellenőrzöttsége | Könyvtár részletpanel; megbízható videó és keresőhivatkozás elkülönítve |

Forráscsoportok: `library131`, `onboarding132`, `v144`, `adaptive-planner`,
`profile-training-128`, `custom-exercise-distance-106`, a teljes i18n és a végső
program-/katalógusfelülírások. A katalogizált106/13 nem korlátozza a saját tartalmat.

### B. Aktív edzés, időzítés, GPS és lezárás

| ID | Megőrzendő main-funkció | Natív elhelyezés |
| --- | --- | --- |
| B01 | Programnap/gyors/kültéri indítás és a megfelelő korábbi súly/ismétlés előtöltése | Közös indítási út; kézi módosítás és draft nem írható felül |
| B02 | Sorozat hozzáadás/törlés, értékszerkesztés, rögzítés és teljesítettség | Aktív gyakorlat kompakt sorai |
| B03 | Gyakorlat hozzáadása, előző/következő/közvetlen váltás | Edzésfejléc és gyakorlatválasztó |
| B04 | Visszavonás, megfelelő edzés/sorozat célzása | Aktív edzés Visszavonás gomb; telefonos és órás eredetnél is |
| B05 | Alsó sorozatstopper, indítás/stop/reset, egyoldali és kétoldali mérés | Gyakorlat típusához igazított ugyanazon mérőpanel, felső duplikált stopper nélkül |
| B06 | Pozitív explicit időmérés-stop automatikus rögzítése, két pozitív oldal feltétele | Ugyanazon natív rögzítési út; pause/reset/nulla nem teljesítés |
| B07 | Pihenő, pause/resume, átugrás, ±15 mp, gyakorlatváltás után is megmaradó idő | Aktív edzés közös pihenőkártyája |
| B08 | Intervallum munka/pihenő/kör, hang/rezgés, edzés- és globális preset, auto/nem-auto léptetés | Összecsukott opcionális intervallumpanel; meglévő egyoldali automatika |
| B09 | Automata edzésvezérlés, natív háttéridőzítő, zárolt képernyő alatti állapot/értesítés | Aktív edzés és közös natív szolgáltatás; nem új UI-időzítő |
| B10 | Edzésszünet, folytatás, tartós draft és kilépés | Aktív edzés; Beállítások megnyitása önmagában nem pause |
| B11 | GPS szándék/választás aktivitásonként, idő/táv/sebesség/tempó, pontszám/pontosság, útvonal | Kültéri mérőpanel és térképmodal |
| B12 | Kézi idő/táv, explicit Mérés befejezése és pozitív GPS-mérés automatikus készre jelölése | Kültéri panel; külön sorozat-OK nem szükséges sikeres explicit stop után |
| B13 | Privát útvonal, vegyes telefon/óra szakaszok és edzés utáni térkép | Aktív mérésből és naplórészletből ugyanazon térkép |
| B14 | Érvényes élő Watch/BLE pulzus, hiányzó/elavult forrás megkülönböztetése | Aktív mérőpanel; forrás nélkül — bpm |
| B15 | Tényleges RIR rögzítése, gyakorlat- és sorozatszintű adatok megőrzése | Sorozatsor/lenyitott részlet; előírt RIR nem mérés |
| B16 | Edzés-/gyakorlat-visszajelzés, hat effort-állapot, fájdalom, megjegyzések | Kompakt edzés-/gyakorlatpanel és közös befejezési összegzés |
| B17 | Mentés, szüneteltetve kilépés, külön elvetés; hibakezelés/újrapróbálás | Közös lezárási felület. Napló/naptár/nyugta csak megfelelő tartós lezárás után |
| B18 | Meglévő érintésvédelem és egy folyamatos3 mp-es feloldás, haptikus sorozatjelzés | Aktív edzés/óra meglévő védelme és haptikabeállítás |

Forráscsoportok: `training-107`, `intervals-162`, `automatic-workout-162`,
`phone-execution-162`, `workout-pause-201`, `workout-undo-188`,
`training-actual-rir-197`, `distance-activity-flow-220`, `set-haptic-227`.

### C. Napló, fotók, fejlődés és Coach

| ID | Megőrzendő main-funkció | Natív elhelyezés |
| --- | --- | --- |
| C01 | Teljes edzésnapló, keresés, hónap/dátum/időszak és meglévő szűrők | Napló listafejléc és lenyitható szűrő; lapozott megjelenítés |
| C02 | Naplózott edzés gyakorlat-/sorozat-/idő-/távadatai és meglévő szerkesztés/törlés | Egy edzés részletnézete és műveletmenü |
| C03 | Edzés-visszajelzés utólagos megadása/javítása, mainben engedett módosítások | Naplórészlet visszajelzéspanel |
| C04 | Edzés egészségadatai és forrásai, külön közvetlen TrainPilot óra és HC/Samsung adatok | Naplórészlet Forrás fül, egyező időablakkal, duplázott kalóriaösszegzés nélkül |
| C05 | Fotó készítése/galériás hozzáadás, lista/nézet/törlés és privát tárolás | Aktív edzés + naplórészlet Fotók panel; haladásfotó gyorselérés |
| C06 | Mentett útvonal megtekintése, pont-/szakasz-/távadatok | Naplórészlet Útvonal, térkép piros X-szel |
| C07 | PR-jelvények és rekordrészletek | Napló kártya/részlet és Fejlődés |
| C08 | Gyakorlatonkénti előzmény, 1RM, volumen/ismétlés/teljesítmény és időszakos grafikonok | Fejlődés belső fülei, közös gyakorlat- és időszakválasztó |
| C09 | Izomcsoportterhelés/eloszlás, meglévő összesített statisztikák | Fejlődés Mennyiség/Izomcsoport panel |
| C10 | Testsúlytrend, korábbi testtömeg és haladásfotók kapcsolata | Fejlődés Testsúly/Foto gyorselérés, Egészség közös adatával |
| C11 | Coach készenlét, tényezők, adatfrissesség és Samsung/saját/korlátozott pontszám | Coach panel és Kezdőlap/Egészség összefoglaló; meglévő számításból |
| C12 | Gyakorlatonkénti tanács, előzmény és terhelési/progressziós indokok | Coach lenyitható gyakorlatkártyái és Fejlődés részletelérés |
| C13 | Következő programnap kiválasztása, ajánlott edzés indítása/naptár megnyitása | Coach javasolt edzés kártya |
| C14 | Mainben meglévő edzésstílus/progressziós módok, előnézet, megerősítés, visszaállítás | Program-/globális edzésbeállítás közös panel; előnézet és alkalmazott állapot külön |

Forráscsoportok: `v215`, `v220/v221`, `v233/v235`, `progression-2`,
`progress-model/ui-177`, `workout-photos`, `wear-journal-sources-160`,
`workout-records-226`, a main végső `training-modes/progression-modes-197`
és megerősítési/Coach/RIR szekciói. Sztornózott #192/#211 munkát nem élesztünk újra.

### D. Egészségnapló, testsúly és adatforrások

| ID | Megőrzendő main-funkció | Natív elhelyezés |
| --- | --- | --- |
| D01 | Egészség áttekintése és teljes egészségnapló: mai és korábbi napok | Egészség → Áttekintés / Napló belső fül |
| D02 | Dátumtól/dátumig választás, lapozás és adatforrás-szűrés | Egészségnapló közös szűrősáv |
| D03 | Napi összesítések és nyers/egyedi mérések, részletek/időbélyeg/forrás | Metrikakártya lenyitása vagy közös részletpanel |
| D04 | A main teljes metrikakészlete és részletes alvásadatok/szakaszok | Metrikacsoportok; az aktuális szolgáltató/engedély szerinti adatokkal |
| D05 | Pulzus: átlag/nyugalmi/min/max, korábbi mérések; élő mérés külön kezelve | Pulzus részletpanel és Eszközök élő kártya |
| D06 | Testsúly: bejegyzés dátummal, hozzáadás/szerkesztés/törlés, előzmény/grafikon/időszak | Egészség → Testsúly; Fejlődésből ugyanide vezető gyorselérés |
| D07 | Testzsír, BMI, SpO₂, VO₂max, vérnyomás/vércukor és a többi mainben elérhető metrika | Egészség méréscsoportok és részletek; nem csak a mockup hat csempéje |
| D08 | Lépés, napi táv, aktív/összes energia, napi aktivitás és edzésekhez kapcsolás | Egészség Aktivitás és naplórészletek |
| D09 | Samsung Health / Health Connect szolgáltatóválasztás, engedély, foreground frissítés | Egészség → Adatforrások; globális Beállításokból is |
| D10 | Samsung Energy Score hozzáférés, hitelesség, nap/idő és saját becsléstől elválasztás | Regeneráció/Energia részletpanel |
| D11 | Napi összesítés forrása: automatikus, HC-prioritás vagy választott eredet | Egészség → Adatforrások lenyitható beállítás |
| D12 | Elsődleges lépésforrás, órás lépések naplózása, automatikus/engedélyezett háttérfrissítés | Ugyanazon adatforrás-/háttérbeállítási panel |
| D13 | Régi egészségadatok beolvasása/migrációja, megőrzött előzmények | Adatút + indokolt helyreállítási státusz; meglévő történet nem vész el |
| D14 | Kiegészítő adatok és hozzáférési/hiányzóadat-állapotok | Metrikák/adatforrások panel; hiány nem helyettesíthető mintaszámmal |

A metrikákat kulcsonként is össze kell vetni a `health-journal-120`,
`samsung-health-provider`, `health-details`, `cardio-daily`, `v240` és
`samsung-energy-access-184` működésével és a natív adapterekkel. Az edc8f52 forrásban a26 provider-kulcs megjelenítése és a havi/nap/csatorna
választás már bekötve, de még nem futtatott ellenőrzés. A rekord- és alvásrészletek,
eredetszűrés és teljes szerkesztési paritás külön szükséges. A darabszám
önmagában továbbra nem igazolja a teljes egészségnaplót.

### E. Naptár, eszközök, mentések és általános beállítások

| ID | Megőrzendő main-funkció | Natív elhelyezés |
| --- | --- | --- |
| E01 | Havi/napi naptár, tervezett/teljesült/törölt események és tényleges teljesítési kapcsolat | Közös Naptár nézet |
| E02 | Tervezés meglévő napi/váltott/heti módokkal, napok/idő/időtartam/ismétlődés és programciklus | Naptár tervezőpanel, globális alapbeállításokkal |
| E03 | Esemény létrehozás/módosítás/lemondás, programnap indítása és teljesítésjelölés | Naptár napi eseménypanel |
| E04 | Google-kapcsolat és a mainben meglévő naptárszinkron/export | Naptár szinkronpanel + Beállítások → Kapcsolatok |
| E05 | Wear óra státusz, megnyitás/mérés/vezérlés és kétirányú edzésszinkron | Óra és szinkron; aktív edzés és Kezdőlap állapota |
| E06 | Phone↔Watch ownership átadás/visszavétel, offline folytatás, reconnect és függő művelet | Ugyanazon Óra és szinkron panel, aktív edzésből elérve |
| E07 | Óráról indított gyors/tervezett edzés, GPS-útvonal, mérések és mini-napló megtartása | Meglévő Wear működés + telefonos napló és forrásrészletek |
| E08 | Korona/lünetta, órás menürend, pihenő továbbélése, feloldás és meglévő órás vezérlők | Wear megőrzendő; a telefon új dizájnja nem szűkíti az óra funkcióit |
| E09 | BLE keresés, protokoll-/eszközállapot, csatlakozás/bontás, megjegyzés/elfelejtés, reconnect | Egészség → Eszközök / Beállítások → Kapcsolt eszközök, ugyanazon adatút |
| E10 | Meglévő BLE élő pulzus, RDFit/egyéb támogatott mérési és diagnosztikai próbák, háttérkapcsolat | Eszköz részletpanel, támogatás és eredmény egyértelmű jelzésével |
| E11 | Helyi JSON mentés és import, teljes ZIP/fotóarchívum, ellenőrzés/előnézet | Beállítások → Mentés és visszaállítás |
| E12 | Drive app-data mentés/szinkron, korábbi mentések és mainbeli retention/helyreállítási lehetőségek | Ugyanazon mentéspanel, felhőkapcsolat és explicit választás |
| E13 | Szelektív egészségadat-mentés, fotóreferenciák, minden hordozható beállítás/kedvenc/program/törlésjelölő | Meglévő backup-adatút; formátum és beleegyezés szerint |
| E14 | Sérült/részleges import kezelése, félbeszakadt művelet folytatása, validáció és adatvédelem | Visszaállítási előnézet/helyreállító panel; nincs hamis siker |
| E15 | Helyi adatok törlése és a mainben létező helyreállítási/karbantartási műveletek | Adatkezelés megerősítéssel; nem automatikus reset |
| E16 | Teljes témaválaszték és HU/EN/DE/RO, a mainben elérhető nyelvválasztási működés | Globális Megjelenés/Nyelv panel; mentett választás migrálva |
| E17 | Hang/rezgés/pihenő/preset/haptika és meglévő edzésmód-alapbeállítások | Globális Edzésbeállítások, edzésenkénti felülírással |
| E18 | Pontos helyengedély, központi helyszolgáltatás, értesítési/Health/Bluetooth hozzáférés és háttérállapotok | Közös Engedélyek, a megfelelő funkcióból közvetlen eléréssel |
| E19 | Adatvédelem, eredeti verzióinformációk és rendszerbeállításokhoz vezető meglévő elérések | Beállítások → Adatvédelem / Alkalmazás / Kapcsolatok |
| E20 | Meglévő értesítések, háttérfolyamatok és rendszerintegrációk | Kontextushoz kötött állapotok + globális engedély/időzítő/eszközpanel |

Forráscsoportok: `cloud`, `v224/v228c/v229`, `publication-105`,
`ble-discovery-111`, `wear-sync`, `workout-ownership-193`, `wear-gps-route-193`,
a main Android/Wear bridge-ek, valamint a témák/nyelvek végső felülírásai.

### Lefedettségi és egyszerűsítési kapu

Minden fenti ID-hoz rögzítendő a **tényleges natív útvonal/belépési pont,
adatforrás és ellenőrző teszt**, valamint a kész/hiányzó/nem bevizsgált állapot.
A közös kártyába, fülbe vagy szerkesztőbe összevont funkció is külön ellenőrzendő.
A végleges main-audit során talált további elérhető művelet a leltárt bővíti;
az nem hagyható el azért, mert az eredeti mockupon nem szerepelt.

A cél az átláthatóság: közös kereső/szűrő, egy egészségnapló, egy testsúlytörténet,
egy programszerkesztő és egy mentési felület. Ezeket több helyről lehet megnyitni,
de az adatok, szerkesztések és visszalépési állapotok közösek. A mockup hiányzó
Kedvencek/egészségnapló/testsúly/részletvezérlői a megvalósítás során pótlandók.

## Négy tábla, 24 képernyő

| Tábla | A hat képernyő |
| --- | --- |
| [1/4 – Kezdőlap és tervezés](mockups/01-main-pages-and-planning.png) | Kezdőlap; Edzés; Programok; Naptár; Gyakorlatok; Gyakorlat részletei |
| [2/4 – Edzés és mérés](mockups/02-workouts-and-measurement.png) | Súlyzós edzés; Időalapú edzés; Kétoldali mérés; Kültéri mérés; Intervallumok; Edzés befejezése |
| [3/4 – Napló, térkép és egészség](mockups/03-journal-maps-and-health.png) | Napló; Edzés részletei; Edzésútvonal; Fejlődés; Egészség; Coach |
| [4/4 – Beállítások és szerkesztés](mockups/04-editing-and-settings.png) | Beállítások; Óra és szinkron; Mentés és visszaállítás; Engedélyek; Privát edzésfotók; Program szerkesztése |

### 1/4 – Kezdőlap és tervezés

Dinamikus Folytatás-kártya aktív/importált/szüneteltetett edzésnél, ugyanazzal az
azonosítóval. Gyors edzés elöl. Galaxy Watch7 státusz és négy egészségcsempe.
Edzésválasztó programnapokkal, Programok/Naptár/Coach gyorseléréssel. Programlista,
havi/napi naptár, kereshető kategóriás könyvtár és valódi gyakorlatleírás/bemutató.

### 2/4 – Edzés és mérés

Kg/Ism./RIR sorok, visszavonás, gyakorlatváltó nyilak. Pihenőn szünet,
−15 mp/+15 mp és Kihagyás. Gyakorlatváltáskor a pihenő megmarad.
Egyetlen alsó sorozatstopper, összecsukott opcionális intervallum.
Explicit pozitív stop készre jelöl; kétoldali mérés mindkét pozitív oldal után kész.
Globális/edzésenkénti intervallumpreset és explicit automatikus léptetés.
GPS-választás látható; élő pulzus forrás nélkül **— bpm**. Manuális adatok lenyithatók.
Befejezés: mentésre váró összegzés, hat visszajelzés, megjegyzés, Mentés,
Szüneteltetés és kilépés, külön megerősített Elvetés. Sikertelen mentés/elvetés
után helyben újrapróbálható; siker előtt nincs lezárási navigáció/nyugta.

### 3/4 – Napló, térkép és egészség

Lapozott napló, teljes adathalmazra működő keresés, PR és létező útvonal előnézete.
Részletek Gyakorlatok/Statisztikák/Forrás fülekkel, fotó- és útvonaleléréssel.
Teljes képernyős privát térképen **piros X**; csak a térképet zárja.
A térképmodalnak nem kell alsó navigáció/fogaskerék. Offline saját útvonal és
háttértérkép-elérhetőség külön állapot; nem ígérünk nem létező offline csempéket.
Fejlődés: 1RM/volumen/PR. Egészség: valódi források, időbélyeg, hiányzóadat-állapot.
Coach: a NativeCoachReadiness eredménye; Samsung energia, saját becslés és korlátozott
Coach-pontszám külön. A mintabeli87/61/54 nem összegzés. A gyakorlatonkénti tanács
nem írja át a programot vagy az aktív edzést.

### 4/4 – Beállítások és szerkesztés

Teljes témakatalógus és négy nyelv. Időzítő, eszközök, profil, adatvédelem.
Ownership/handoff és offline folytatás állapota, tudatos átadás. JSON/ZIP/Drive
mentés, előnézet és félbeszakadt helyreállítás explicit folytatása. Egészségadat
mentése külön választás. Pontos GPS-engedély és központi helyszolgáltatás külön.
Privát fotók, kamera/galéria/törlés; a felhő/ZIP-leírás a ténylegesen engedélyezett
mentési beállítást tükrözze. Programszerkesztő napokkal, sorrenddel,
sorozat/ismétlés/súly/pihenő értékekkel és tudatos mentés/elvetés művelettel.

## A képeken látható mintafeliratokat felülíró pontosítások

- Beállítások és más másodlagos oldalak az eredeti navigációs kontextust őrzik;
  a képtáblák eltérő kiemelései nem jelentenek új navigációs szabályt.
- A Programok képen megjelenő „Közösségi” mintaelemhez nem készült elfogadott
  közösségi funkcióspecifikáció; ezt a megvalósításba nem kell átvenni.
- Helyes restore-szöveg: **„Aktív edzés közben nem cserélhetők az adatok.”**
  Nem az edzés ELŐTT tiltott; előbb befejezés/elvetés szükséges, ha aktív session van.
- Átadás: az aktuális, megfelelő tulajdonú edzés tudatos átadása. A „következő
  edzésnél” vagy „edzés indítása” képfelirat nem írja felül az ownership-protokollt.
- A verzió a tényleges buildből jön. A táblák személyes köszöntései, kalóriái,
  fotói és számadatai nem alapértelmezett felhasználói adatok.

A működés részletes elsőbbségi szabályai: [NATIVE_MOCKUP_REVIEW.md](NATIVE_MOCKUP_REVIEW.md).
Teljes megvalósítási feladat: [NATIVE_FINAL_DELIVERY.md](../plans/NATIVE_FINAL_DELIVERY.md).
Aktuális kód/tesztállapot: [WORK_HANDOFF.md](../WORK_HANDOFF.md).

### D06 implementációs nyom – 2026-10-24

`e3e79d3`: Egészség és Fejlődés közös `weight_journal` útvonal, Room body_weights,
dátumos hozzáadás, változatlan mérési dátummal szerkesztés, megerősített exact-ID
törlés, grafikon/időszak/min-max/kezdő/aktuális/változás. WeightJournalScreen / VM /
NativeWeightJournalRepository; 13 új unit +2 telefonos UI-próbakód, még nem futott.
Ez funkcionális bekötés, nem a végleges vizuális dizájn vagy teljes D06-elfogadás.
A providerből érkező történeti testsúly ugyanabban az Egészség menüben külön
adatforrásként marad; kézi mérés szerkesztése nem írja át a provider mérését.

D06 adatbetöltés pontosítása (`29ed6a4`, 2026-10-25): a teljes szerkeszthető lista
Room-lapozott. A kezdő/aktuális/min-max/darabszám a teljes naplóé; az időszakválasztó
csak a grafikont szűri. Grafikon legfeljebb180 időablak-átlag, kifejezett átlag és
mintaszám felirattal; ez nem mérésvágás vagy nyers adatok felülírása. Ismeretlen
dátumú import sor megmarad, de nem lesz kitalált pont. Külön retry; HU/EN/DE/RO.
13 új JVM+2 UI-kód még nem futott; célzott SQL-próba nem Android-eredmény.

### D03–D05 részletpanel implementációs nyom – 2026-10-24

`606b8c0`: kiválasztott napi/csatornás egyedi mérések és külön alvás részletpanel,
25 rekordos oldalak/eredetszűrés, saját forrás és mérési/módosítási idő; HC/Samsung
fázisok és pulzus/sebességminták 20-as részletoldalakban. Dátum/összesítés nem
módosul bezáráskor; aggregátumból nem gyártunk hiányzó mérést. Források nem adódnak
össze. Visszaállítás utáni kurzor explicit reloadot kér. 16 unit+3 UI új próbakód,
még nem futott; végleges dizájn/CI/készülékes Health-elfogadás nincs igazolva.

~~~~

---

## docs/design/final-ui-ux/01-visual-system.md

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/docs/design/final-ui-ux/01-visual-system.md

SHA-256: `9dd75e3a034c73b227e3b9392185b9e8ffc514df570f6a91b4d2310cca8bdd9c`

~~~~markdown
# Vizuális rendszer és megvalósítható komponensspecifikáció

Ez **tervezési specifikáció**, nem végrehajtott theme/UI patch. A jelenlegi tokenek:
[theme](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt#L28), [tokens](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt#L10), tipográfia [typography](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt#L55); palette katalógus [palettes](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt#L113).
A nyers képszínek nem válnak univerzális hex konstansokká. A bemutató-fixture
`amberGold` (basic) vagy `yellow` (vivid); a mentett `themeKey/themeAccent/themeAccent2`
választást nem állítjuk át. A további31téma egyenrangú, a grafit/arany csak referencia.

## A négy tényleges kép vizuális olvasata

- **01**: a sötét alapon a Folytatás/Indítás egyértelmű nagy hangsúly, másodlagos
  műveletek sötét felületűek. Az aktuális program kijelölt kontúrt + „Aktív” jelzést
  kap; finom vonalak és12–16egységnyi ritmus választja el a listát. A kézi fejléc,
  indokolatlan dupla oldalcím és nagy üres top-margin a natívban kerülendő.
- **02**: a gyakorlat azonosító fejléc és a mérés/sorozat a fókusz; nincs általános
  dashboard dekoráció. Nagy időszám, jól látható state és explicit Stop, alatta
  kompakt pihenő. A kétoldali panelek és a kis ±15célok nem másolhatók mechanikusan
  keskeny/font2.0 nézetre: tördelődő fallback és48dp szükséges.
- **03**: edzéslista és napi mérési kártyák erős név/szám, visszafogott metaadat.
  A térkép teljes felületű modal, piros X-szel. A három Coach-pontszám külön
  jelentésű; a csinos háromszámos panel nem indokol új átlagolást.
- **04**: beállítások szekcionált sorokból, egy kijelölt/lenyitott editorsorból
  és jól elkülönített veszélyes műveletekből állnak. A mockup restore- és
  handoff-feliratát a dokumentált működés írja felül; a bemutató fotó nem useradat.

A fájlok SHA256/dimenzió egyezését a MANIFEST-tel ténylegesen ellenőriztük;
az eredmény a source-evidence.json-ban van. Mind4kép 1024×1536 kompozit tábla,
nem1024dp széles natív telefon. Nem exportálunk belőlük UI bitmapgombokat.

## Színek és állapotok

| Szerep | Meglévő token / alkalmazás | Kötelező megkülönböztetés |
|---|---|---|
| Háttér | bg (jelenleg0E1015) | Scaffold és összefüggő lap; nincs minden komponens mögött külön keret |
| Fő kártya | card (1A1E25), card2 (232933) | card a tartalom; card2 a beviteli/másodlagos felület |
| Szöveg | text, muted | fő adat text; címke/időbélyeg muted; nem .4alpha pici szöveg |
| Fő művelet | accent kitöltés + onAccent | onFill derivált kontrasztot használ; nem fix fehér vagy fix fekete |
| Kiegészítő hangsúly | accent2 + onAccent2 | nem helyettesíti a második elsődleges CTA-t |
| Kis hangsúlyos cím | accentText | a nyers accent helyett a4.5:1-re derivált szövegtoken |
| Kijelölt sor/fül | selectedSurface + selectedBorder + text/state label | border≥3:1, ikon/szöveg jelzi a kiválasztást is |
| Dekoratív elválasztó | line / emphasisBorder | a line nem funkcionális focus vagy hibajelző, mert a3:1 nem garantált |
| Siker | success + kész ikon/szöveg | szín önmagában nem bizonyít mérést/mentést |
| Veszély / hiba | danger felület + dangerText felirat/ikon | normál destructív outline, explicit megerősítés; X a route modalon a kivételes hangsúly |
| Betöltés / részleges / offline | muted/text + ismert progress vagy indeterminate | nem kapnak jóváhagyást jelző pipát; ismeretlen százalék nem rajzolható |

A raw `accent` szövegként sötét Basic témákban nem garantáltan olvasható.
A [tokens](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt#L10) jelenlegi Oklab-lerp és alpha-composite kontrasztpolitikája marad;
nem írjuk újra RGB-közelítéssel. Tényleges renderben4.5:1 normál szöveg,
3:1 nagy szöveg és funkcionális kontúr a minimum. A kiválasztás/fókusz nem csak
opacity-váltás. Semleges/dangerText szöveg nem rendelhető véletlenül danger
háttértokenhez foregroundként (pl. most több errorlabel `colors.danger`).

## Tipográfia (tervezett szerepek, nem mind létező jelenlegi Style nevek)

| Szint | Méret / sorköz sp | Súly | Használat |
|---|---|---|---|
| Screen title | 24 /30 |600 | ha a közös topbar már tartalmazza a nevet, nem duplikáljuk még egyszer |
| Hero név | 24 /30 |700 | következő/folytatott edzés; több sor engedett |
| Szekció | 18 /24 |600 | aktuális program, mai adatok |
| Kártya címe | 16 /22 |600 | név és fő állapot |
| Body/input | 16 /24 |400 | beviteli érték és hiba; nem11sp |
| Meta / címke | 13 /18 |400–500 | forrás, dátum, egység |
| Micro | 12 /16 |500 | csak másodlagos rövid badge, nem fő művelet |
| Fő mérés | 36 /42 |700 | stopper/táv, tabular numerals, egység13sp a szám mellett/alatt |
| Másodlagos mérés | 24 /30 |600 | kétoldali vagy rácsérték |
| Button | 14 /20 |600 | teljes címke, fontscale tördelődik; label rövidítése nem információvesztés |

A mostani TrainPilotTypography30/22/18/15/13/11/10sp szintjei a baseline;
phase2-ben a meglévő Material szerepekhez igazított hunkokkal egységesítjük.
Egy glyph se legyen színben fixTextColor azért, mert a TextStyle onnan származik:
a komponens explicit témaszerepet használ. Rendszerfontot használunk, nincs új
fontdependency. Nincs all-caps hosszú DE/RO cím vagy túlzott letterSpacing.

## Méret, térköz és sarkok

-4dp alapegység; belső mikro-gap4, kapcsolódó sor8, kártya-gap12, oldal padding16,
 szekcióköz24. Kártya padding16 (sűrű adatnál12); a card-title és fő adat8.
- Sarki sugarak: fő card16, hero20, input/vezérlő12, badge8. Chip nem teljes
 pill mindenen. Funkcionális kontúr1–2dp; kiválasztott panel2dp. A nyers képen
 látszó glow/shadow nem kell a jó adat-hierarchiához.
- Vezérlő **minimum48×48dp**, fő CTA minimum52dp magas, tartalommal növekszik.
 `height(42.dp)` és24dp foto-X nem marad. Ikon24dp vizuálisan, touch48dp.
 Soros +/- komponens teljes egységfeliratot tart, nincs9dp széles cél.
-320dp szélesség és1.0font mellett az oldal belseje288dp; két kártya csak ha
 mindkettő értéke/címkéje elfér. Font>1.3 vagy keskeny constraint esetén one-column.
 A layout valódi constraints/fontScale alapján vált, nem eszközmodell-nevekből.
- Footernek és aktív bannernek mért layout hely kell. Nem elég a mostani
 sok képernyőn80/96dp Spacer, ha a footer+font ennél magasabb. Phase2-ben közös
 inset/contentPadding szerződés; a Scaffold és IME inset nem alkalmazandó kétszer.
- Alsó nav öt célja megmarad. A címke legfeljebb két sor, de2.0fontnál új
 mérés/célhossz szükséges; beszédes TalkBack címke mindig teljes.

## Tervezett, újrahasznosítható megjelenítők

Ezek **új vizuális composable-javaslatok**, nem új motor/state/writer:

| Tervezett komponens | Paraméterek forrása | Interakció |
|---|---|---|
| SectionHeader / SurfaceCard | cím, opcionális state text, colors | opcionális meglévő callback, nincs implicit load |
| ActionCard / StatusBadge | meglévő title/subtitle/status/enabled | meglévő onClick; szerep+selected semantics |
| MetricTile | valueText/unit/source/time/isAvailable | opcionális létező részletnyitás; nincs adatkonverziós képlet |
| SessionHero | kanonikus snapshot jelenlegi célja + meglévő resume callback | nem startol új sessiont |
| ExerciseHeader / SetRow | ActiveExerciseUiModel/ActiveSetUiModel, renderedTargets | ugyanezen targetre ugyanaz az editor callback |
| MeasurementCard / RestPanel | timer/gps state + NativeTimerTarget | meglévő toggle/adjust/skip, önálló tick nélkül |
| SourceAndFreshness | meglévő source/date/partial | csak megjelenít, nem dönti újra el a hitelességet |
| InlineIssue / EmptyState | meglévő loading/error/permission/empty + retry | retry csak explicit user action |
| FormSection / FieldRow | meglévő draft mező és save/error | IME/tördelés/fókusz; domain-validátor változatlan |

Képek: csak létező privát fotó, ellenőrzött demo/video vagy meglévő asset.
Nincs új stockfotó élő mérést jelképezve, nincs generált „after” appképernyő.
A ritmus és az ikonográfia a fő vezérlő és adat elkülönítését szolgálja.
Iconok a meglévő Material készletből, azonos24dp stroke/térfoglalással; a belső
entitynevek helyett lokalizált jelentés. A mockupnak nincs új asset-licencígérete.

## Űrlap, navigáció és visszajelzés

Módosított local draft → explicit mentés → meglévő writer eredmény → siker.
Mégse/Back nem ír; pending save közben nem szabad bezárni és késznek jelezni.
Egy sikertelen mentés megtartja az inputot és az értelmezhető hibát; stale szerkesztő
újraolvasást kér. Rotationt megőrző UI Saver csak prezentációs draftot őrizhet,
nem kerülhet token/consent/activity vagy privát bitmap a Bundle-be.
Selection/scroll/query visszaáll; onResume nem új mentés vagy GPS-start.
A mérés-/hibaállapot-frissítés TalkBacknek nem250ms-onkénti live region.

A fájlhatárok és a tényleges callbackek a03-binding dokumentumban vannak.

~~~~

---

## docs/design/final-ui-ux/02-screen-contracts.md

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/docs/design/final-ui-ux/02-screen-contracts.md

SHA-256: `6d4ec34a27b1b2858756dda7ab60414232b77b2dc734bf1339e9f16097b45838`

~~~~markdown
# A 24 nézet kivitelezési szerződése

Minden **tervezett komponensnév** prezentációs javaslat; nem állítjuk, hogy már létező composable. A hivatkozott kisbetűs kódaliasok exact c116a042 forrásra mutatnak. A binding-dokumentum részletezi az adat/callback utat. A globális G-AC01…08 elfogadás **mind a24nézetre kötelező**, és az alábbi nézetspecifikus próbák hozzáadódnak. Minden screenshot-/runtime-állapot jelenleg NOT_RUN.

A nézetazonosítók V01–06 az1.tábla balról jobbra, fentről lefelé; V07–12 a2., V13–18 a3., V19–24 a4.tábla sorrendjét követik. A megosztott modalt több útvonal is megnyithatja; ez nem24külön route.

## V01. Kezdőlap

**Meglévő hely:** `home`. **Megőrzési ID-k:** A01, A03, A04, A05, C11, E05.

**Meglévő forrás:** [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36) · [homeenergy](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L117) · [homequery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt#L18) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A main mai naptári elsőbbsége, Folytatás és gyorsindítás marad. A mockup arany Folytatás-kártyája kerül első helyre; a köszöntés nem lehet fix Levente, az óra neve nem fix Watch7.

**Felépítés, felülről lefelé:** Globális fejléc → aktív/paused/importált Folytatás hero (ha van) → különben következő valódi programnap → Gyors edzés → óraállapot → mai metrikák → Programok/Naptár/Coach. A folytatás mellett az ajánlott új indítás ne legyen vele versengő elsődleges CTA.

**Elsődleges / másodlagos műveletek:** Fő: meglévő banner selectDestination("training") aktívnál, onResumeDraft importáltnál, különben onStartWorkout(programId, dayId, scheduleId). Másodlagos: onQuickWorkout(false/true), onPrograms/onCalendar/onCoach; onOnboarding kihagyott profilnál.

**Scroll / billentyűzet / Back:** Egy vertikális scroll; visszatéréskor scroll és célzott nap megmarad. Nincs billentyűzet. Bottom aktív banner mért Scaffold-helyet foglal. Back nem indít új sessiont.

**Üres / töltő / hiba / permission és session:** loading: valódi folyamatjelző; loadFailed: loadHomeData retry, régi adat nem kap mai címkét. Nincs cél: programválasztó, nem inaktív demóedzés. Óraadat hiánya: ismeretlen/—. Samsung-forrás csak igazolt forrással.

**Nézetspecifikus elfogadás:** 320dp-n egy teljes Folytatás CTA és a session állapota elérhető. DE/RO 2.0 fontnál metrikák egy oszlopra váltanak. TalkBack a session állapotát a CTA előtt olvassa. Aktív+importált fixture: az aktív marad a kanonikus session.

**Forrásból bizonyított nyitott kötés:** G01, G02: Home recoveryText nyers energyScore; watchBattery nincs kitöltve. A felső aktív hero adat/callback már a scaffoldban megvan, a prezentációs slotot Root-reviewed hunkkal kell áthelyezni, nem új Home-motorral.

## V02. Edzésválasztó

**Meglévő hely:** `training`. **Megőrzési ID-k:** A03, A04, A06, B01, E01.

**Meglévő forrás:** [training](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt#L23) · [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A bevált gyors beltéri/kültéri indítás és az aktuális program naplistája marad. A mockup három gyorscsempéje és kijelölt programnapja adja a ritmust; nem találunk ki becsült edzésidőt.

**Felépítés, felülről lefelé:** Gyors edzés fő kártya → Gyakorlatok/Kültéri két kártya → aktuális program neve → programnapok sorrendben → Programok/Naptár/Coach gyorselérés. Sok napnál vertikális lista, nem levágott A/B fixrács.

**Elsődleges / másodlagos műveletek:** Fő: onStart(programId, dayId), onQuick(false/true). Másodlagos: onLibrary/onPrograms/onCalendar/onCoach. Busy/aktív új indítás a meglévő nav szabályával folytatást kap.

**Scroll / billentyűzet / Back:** Főoldali scroll mentett helyzettel; nincs IME. Programnapok kártyája egy címzett startművelet, a teljes kártya és belső gomb ne legyen két fókuszállomás ugyanarra.

**Üres / töltő / hiba / permission és session:** Nincs aktív program: onPrograms elérés. Betöltés alatt nem villan üres programként az állapot; ahol nincs loading state, ne találjunk ki külön sikerállapotot.

**Nézetspecifikus elfogadás:** Kis szélesség/nagy betű: két gyorscsempe egymás alá, minimum48dp start. Hosszú napnév teljesen tördelődik. TalkBack név + gyakorlatdarab + Indítás; nem színből derül ki a kiválasztás.

**Forrásból bizonyított nyitott kötés:** —

## V03. Programok

**Meglévő hely:** `programs`. **Megőrzési ID-k:** A06, A07, A08, A09, A10, C14.

**Meglévő forrás:** [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32)

**WebView folytonosság és kép:** A main programaktiválás, másolás, saját szerkesztés és napi visszaállítás marad. A mockup kiemelt Aktuális program kártyája és listaritmusa megmarad; a Közösségi mintafül kimarad.

**Felépítés, felülről lefelé:** Aktív program + állapotcímke → további beépített/saját listák → Saját terv/új program → kártyán belüli naplista és műveletmenü. A training-mode összehasonlító előnézet külön modal, nem csendes kapcsoló.

**Elsődleges / másodlagos műveletek:** Fő: onStartProgramDay és activateProgram. Másodlagos: saveProgram, személyes terv openPersonalPlanner/savePersonalProgram, previewRestoreDay/confirmRestoreDay, deleteProgram, approveTrainingModeCompared/undoTrainingMode. Az editor callbackek változatlan draft/CAS célra kötődnek.

**Scroll / billentyűzet / Back:** Lista scroll visszaállítás; a program szerkesztése modal, IME a címmezőhöz igazodik. Back először az aktuális modalt zárja; mód-/összehasonlítási request epoch védelme megmarad.

**Üres / töltő / hiba / permission és session:** Betöltő/mentési hiba az adott programhoz; stale compare esetén új előnézetet kér. Beépített törlés/reset korlátozását nem oldja fel új menüpont.

**Nézetspecifikus elfogadás:** Kis képernyőn az aktiválás és menü elérhető; DE/RO hosszú címe több sor. TalkBack Aktív jelző + műveletnevek. Nagy fontnál a mode összehasonlítás vertikális, nem levágott kétoszlopos tábla.

**Forrásból bizonyított nyitott kötés:** —

## V04. Naptár

**Meglévő hely:** `calendar`. **Megőrzési ID-k:** E01, E02, E03, E04.

**Meglévő forrás:** [calendar](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L28) · [calendarvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarViewModel.kt#L35) · [calendarplanner](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L196)

**WebView folytonosság és kép:** A main naptári napkapcsolat és többhetes ciklus marad. A mockup havi rácsa + napi esemény-idővonala a cél; a jelenlegi napcsík ugyanazon month/day adataiból alakítható rácscsá.

**Felépítés, felülről lefelé:** Hónapváltó → hét napnevek + 7 oszlopos valódi hónaprács → választott dátum → tervezett/teljesített eseménykártyák → többhetes tervezőpanel. A rácspont csak meglévő plan/history eseményt jelez.

**Elsődleges / másodlagos műveletek:** Fő: onStart(row), completed esetben onJournal. Másodlagos: selectDay/changeMonth, save/status, plannerOpen lokális megnyitás, previewPlanner/applyPlanner/cancelPlanner. Terv előnézet és Mégse megtartandó.

**Scroll / billentyűzet / Back:** Hónapváltó és rács fölött nincs vízszintes képernyőscroll. Nagy betűn a rács számcellája48dp; ha7×48dp nem fér a belső szélességbe, a jelenlegi vízszintes napcsík a működő fallback, plusz teljes dátum fejléccel. Editor IME/date picker; Back a preview/edit réteget zárja, nincs mentés.

**Üres / töltő / hiba / permission és session:** Újraolvasási readiness/fallback és busy a VM állapotából; nem indítható idő előtt előnézet. Üres nap külön, üres hónap külön. Cancelled sorok VM-szűrését nem kerüli meg a UI.

**Nézetspecifikus elfogadás:** DE/RO eseménynév wrap, TalkBack teljes dátum + státusz + indítás. DST23/25h nap megtartja a helyi idő szerződését. 4 hét terv cancel/save/reopen és ugyanazon scheduleId indítása kötelező.

**Forrásból bizonyított nyitott kötés:** G09: Settingsből már van Google naptár sync, itt nincs külön onSync callback; nem rajzolható működőnek egy új inline sync gomb. Root nav-hook kell, ha itt is kívánják.

## V05. Gyakorlatkönyvtár

**Meglévő hely:** `exercise_library`. **Megőrzési ID-k:** A11, A12, A13, A14.

**Meglévő forrás:** [library](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L115) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A WebView keresője, izomcsoportjai, Kedvencek és saját gyakorlatok maradnak. A mockup kereső/chip/listakártya felépítése javítja az áttekinthetőséget; a106 szám valós aktuális state.rows méretéből jön.

**Felépítés, felülről lefelé:** Kereső → Összes/Kedvencek és izomcsoport-szűrők tördelődő sor → találatszám → teljes lista → saját gyakorlat hozzáadása. Soronként név, főizom, felszerelés és kedvenc; stock anatómiai fotó csak valódi meglévő assetből.

**Elsődleges / másodlagos műveletek:** Fő: selectedId megnyitja a meglévő részletet. Másodlagos: toggleFavorite, saveCustom/deleteCustom editor. A sor szív/csillag önálló48dp cél, nem a kártyanyitás mellékhatása.

**Scroll / billentyűzet / Back:** Keresésnél IME ne takarja a találatlista első akcióját, kereső clear48dp. Back előbb saját editor, majd részlet, majd route. Query/favoritesOnly/muscle már rememberSaveable, megmarad.

**Üres / töltő / hiba / permission és session:** loading/error/reload külön; nincs találat a szűrő miatt nem üres katalógus. WriteFailed a kedvencsoron; nem animálunk sikeres mentést tartós hiba esetén.

**Nézetspecifikus elfogadás:** 320dp és hosszú DE/RO gyakorlatnevek: nincs horizontális elcsúszás; font2.0-n ikon helyett is marad szöveges név. TalkBack kedvenc be/ki állapot és listaindex. Saját elemek nem tűnnek el hardcoded106 limit miatt.

**Forrásból bizonyított nyitott kötés:** —

## V06. Gyakorlat részletei

**Meglévő hely:** `exercise_library / selectedId`. **Megőrzési ID-k:** A12, A13, A14, A15, B03.

**Meglévő forrás:** [librarydetail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L165) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A main leírás, főizmok, súlyértelmezés és videóforrások megmaradnak. A mockup nagy demonstrációs panelje csak rendelkezésre álló tartalmat használ; nem generálunk új, ellenőrzöttnek látszó bemutatót.

**Felépítés, felülről lefelé:** Cím + kedvenc → valódi leírás és felszerelés → célizom/adattípus/előírás → videó vagy külön jelölt keresőhivatkozás → saját elem szerkesztése. Ha nincs asset, leírás és típusszimbólum helyettesíti a dekorációt.

**Elsődleges / másodlagos műveletek:** Fő: NativeExerciseDemo.resolve szerinti VIDEO vagy UNVERIFIED_SEARCH open(link). Másodlagos: toggleFavorite; custom edit. Aktív edzéshez hozzáadás az editor meglévő exercise pickeréből működik.

**Scroll / billentyűzet / Back:** Vertikális részletscroll; Back a kereső listához tér, megőrzött szűréssel. Külső videóból visszatérve nincs új session vagy programmutáció. IME csak custom editorban.

**Üres / töltő / hiba / permission és session:** Videóhiány és linknyitási hiba saját szöveg, nem néma ikon. Nem hitelesített keresés világosan különbözik a demonstrációtól.

**Nézetspecifikus elfogadás:** TalkBack a videó/keresés jellegét és a kedvenc állapotát mondja. Hosszú cím/nagy font mellett a bemutató nem tolja el az első valódi műveletet. Súly/kéz vs összsúly jelentés látható.

**Forrásból bizonyított nyitott kötés:** G03: ebben a screen-signature-ben nincs onAddToWorkout; a mockup Hozzáadás gombját nem lehet új writer/callback nélkül ide bekötni. A meglévő aktív picker út marad, Root dönt az extra entryről.

## V07. Súlyzós aktív edzés

**Meglévő hely:** `active_workout / reps`. **Megőrzési ID-k:** B01, B02, B03, B04, B07, B09, B10, B15, B18.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [setcard](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L717) · [rest](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1078) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683)

**WebView folytonosság és kép:** A megszokott kg/ism/RIR sor és egyértelmű sorozat-OK, undo, ±15 és Kihagyás marad. A mockup egy kiválasztott gyakorlatra koncentrál, nem több teljes kártyára.

**Felépítés, felülről lefelé:** Session/owner állapot → előző/cím/következő → előírás → sorozatrács → visszavonás → opcionális részletek. A pihenőkártya a gyakorlatlistán kívül, a mért alsó vezérlőterületen marad. Befejezés és kamera/galéria külön egyértelmű akció.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetCompleted(renderedTarget). Másodlagos: editSet/setSetRir/toggleSetRirMode, add/removeSet, selectExercise, undoLastSet; rest adjustRestTimer/skipRestTimer. Minden cél workoutId/generation/exerciseId/setId/timerKey a renderelt snapshotból.

**Scroll / billentyűzet / Back:** IME alatt a fókuszos cella és OK elérhető; numerikus megjelenítés nem alakítja át a validátort. Kis szélesség/font2.0: egy sorozat kétsoros blokk (felső set/kg/ism, alsó RIR/OK), nincs tömörítés8sp-re. Back a meglévő leaveWorkout pause út; Settings külön, nem pause.

**Üres / töltő / hiba / permission és session:** Watch-owner csak olvasható tükröt kap; pending ownership/finalization nem oldható fel új gombbal. Timerérték a state-ből, nincs Compose tick motor.

**Nézetspecifikus elfogadás:** Valódi pihenő futása alatt gyakorlatváltáskor marad kijelzés/azonos timerKey. Régi target nem ír. DE/RO labels és TalkBack Sorozat2,16kg,10ism, RIR2, kész/nem kész. Undo és ±15/Skip48dp.

**Forrásból bizonyított nyitott kötés:** G04: a RestTimerFloatingBar csak onAdjust/onSkip paramétert kap; a mockup külön rest-pause vezérlőjét nem szabad a teljes workout pause-nak álcázni.

## V08. Egyoldali időalapú edzés

**Meglévő hely:** `active_workout / timed unilateral`. **Megőrzési ID-k:** B02, B03, B05, B06, B08, B09.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A main eredeti alsó sorozatstopperje marad, és az explicit pozitív stop auto-rögzít. A mockup nagy olvasható ideje, státuszcímkéje és csukott Intervallumok sora kerül a valódi stopper köré.

**Felépítés, felülről lefelé:** Gyakorlatváltó → sorozatlista (kész/fut/még nem) → aktuális alsó stopper → Stop/Start és külön Nullázás → csukott intervallum → közös finish/rest terület. Intervallum futásakor nincs duplikált alsó stopper.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetStopwatch(target, side). Másodlagos: resetSetStopwatch, selectExercise; IntervalOptions onSave/onStart/onStop/onSkip a meglévő célokkal.

**Scroll / billentyűzet / Back:** Stop/Start a szám közelében; Nullázás nem piros stopnak álcázott. Számok tabulárisak. Vertikális scroll, nincs billentyűzet a futó méréshez; kézi adat csak lenyitott cellában. Back pause, Settings folyamatos mérés.

**Üres / töltő / hiba / permission és session:** 0mp/reset/implicit pause nem teljesítés. Futó/nincs futó/szüneteltetett/ready/complete külön szöveg+ikon, nem csak progresscsík.

**Nézetspecifikus elfogadás:** 1–59mp és60+mp fixture, explicit pozitív stop után ugyanaz a sorozat egyszer kész. Nagy font/DE a stop és reset nem lóg össze; TalkBack nem szólal meg250ms-onként, állapotváltást jelez.

**Forrásból bizonyított nyitott kötés:** —

## V09. Kétoldali időmérés

**Meglévő hely:** `active_workout / timed bilateral`. **Megőrzési ID-k:** B03, B05, B06, B08.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838)

**WebView folytonosság és kép:** A main külön bal/jobb pozitív időfeltétele megmarad. A két mockup-oszlop ugyanazt a meglévő stopper-state-et jeleníti meg, nem két párhuzamos Compose-motort.

**Felépítés, felülről lefelé:** Gyakorlat → sorozatcél → Bal / Jobb külön panel → teljesítési feltétel → csukott pihenő/intervallum. Széles nézeten két oszlop; kis/nagyfonton egymás alatti azonos sorrendű panelek.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetStopwatch(target, "left"/"right"); Másodlagos: resetSetStopwatch a meglévő szerződéssel, gyakorlatváltó, pihenő. Automatikus intervallumléptetés kétoldalinál nem kínálható új funkcióként.

**Scroll / billentyűzet / Back:** Oldalcímke mindig az idő előtt olvasható, oldalváltás nem helycserés UI-trükk. Reset megerősítése csak ha a meglévő út kéri; nem vezetünk be második állapotot. Back/Settings mint V08.

**Üres / töltő / hiba / permission és session:** Csak bal pozitív, jobb0: nem kész. Mindkettő pozitív: meglévő auto-rögzítés. Watch mirror/read-only ugyanazt mutatja más inputengedéllyel.

**Nézetspecifikus elfogadás:** 2.0 font és hosszú oldalcím: vertikális fallback. TalkBack oldal + idő + mérési állapot + művelet. Oldal/gyakorlatváltás és visszatérés nem nulláz rögzített értéket.

**Forrásból bizonyított nyitott kötés:** —

## V10. Kültéri/GPS mérés

**Meglévő hely:** `active_workout / distance`. **Megőrzési ID-k:** B11, B12, B13, B14, E18.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [distance](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L808) · [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A bevált Mérés befejezése, kézi idő/táv és kifejezett GPS-döntés megmarad. A mockup GPS-státuszsáv és2×2 metrikarács szervezése a cél; nincs például148bpm alapérték.

**Felépítés, felülről lefelé:** Aktivitás/owner → GPS-választás/engedélyállapot → táv+idő hangsúly → fix/pontosság és valós pulzus külön → útvonal megnyitás → manuális sorozatmezők → explicit mérés-start/finish. Élő háttértérkép dekoráció nélkül.

**Elsődleges / másodlagos műveletek:** Fő: requestGps(renderedTarget, expectedMeasurementId) → meglévő permission launcher/toggleGpsTracking. Másodlagos: openRouteMap(routeKey), ugyanazon editSet manuális mezők. Permission-settings visszatérés nem hív automatikus GPS-writert.

**Scroll / billentyűzet / Back:** Mérés befejezése nem ugyanaz az Edzés befejezése. A map-close csak modal. IME-nél manuális adatok szerkeszthetők és olvasható a mért vs kézi forrás; futó mérés readonly szabályai maradnak.

**Üres / töltő / hiba / permission és session:** Pontos/hozzávetőleges/megtagadott, központi hely OFF, vár fixre, fixvesztés, hibás/partial route külön. Pulzusforrás nélkül —bpm. Engedélyhiány mellett a workout nem áll le és nincs kitalált pont.

**Nézetspecifikus elfogadás:** Kis nézeten a táv/idő és explicit stop minimum48dp. TalkBack fixállapot és forrás, élő érték nincs zajos folyamatos bejelentéssel. Pozitív explicit GPS-stop külön sorozat-OK nélkül kész; mapX/back/hiba nem.

**Forrásból bizonyított nyitott kötés:** G05: DistanceMeasurementPanel jelenleg nem rendereli liveHeartRateBpm és gpsAccuracyMeters mezőt, noha a state tartalmazza. Bemutatásuk vizuális kötés, új pulzus/pace számítás nem. G06: nincs alaptérkép-provider.

## V11. Intervallum beállítása

**Meglévő hely:** `active_workout / IntervalOptions; settings timer`. **Megőrzési ID-k:** B08, B09, E17.

**Meglévő forrás:** [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A munka/pihenő/kör, hang/rezgés és explicit automatikus léptetés a mainből megmarad. A mockup rendezett beállítássorai a csukott, opcionális aktív panel lenyitásában kapnak helyet.

**Felépítés, felülről lefelé:** Először aktuális mentett preset összefoglaló → lenyitva munka/pihenő/kör → hang/rezgés → autoAdvance (csak támogatott egyoldali mód) → csak erre/mentés alapnak → Mentés → Indítás. Globális timerdialog ugyanazon mezőhierarchia.

**Elsődleges / másodlagos műveletek:** Fő: configureIntervals / IntervalOptions onSave, majd startIntervals. Másodlagos: stopIntervals és skipRestTimer a meglévő célhoz; Settingsben beginTimerDefaults/saveTimerDefaults. A csukás nem mentési gomb.

**Scroll / billentyűzet / Back:** Számbillentyűzet, IME-padding, Next mezősorrend. Módosított és mentett preset külön; start csak az eddig is engedett feltételekkel. Back csak a panel/editor állapotot kezeli, nem állít le rejtetten szolgáltatást.

**Üres / töltő / hiba / permission és session:** Invalid vagy24h feletti konfiguráció a meglévő validátoron marad. intervalSaving/writeFailed látható, nincs hamis Saved. Kétoldali módban nincs új egyoldali automata ígéret.

**Nézetspecifikus elfogadás:** Kis/nagyfont: mezők oszlopba, plusz/mínusz48dp. TalkBack érték+egység és checkboxállapot. Csak a megadott scope változik; más aktív frame/global config nem íródik felül stale mentésből.

**Forrásból bizonyított nyitott kötés:** —

## V12. Edzés befejezése

**Meglévő hely:** `active_workout / showFinishModal`. **Megőrzési ID-k:** B16, B17, B10.

**Meglévő forrás:** [finish](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1128) · [finishwriter](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L1005) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683) · [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105)

**WebView folytonosság és kép:** A hat effort-állapot, fájdalom és külön pause/discard megmarad. A mockup „Mentésre vár” hierarchiája a cél: az effort választás önmagában ne tűnjön végleges mentésnek.

**Felépítés, felülről lefelé:** Valódi összegzés → hat értékelés mint választható mező → megjegyzés, ha lesz meglévő writable state → elsődleges Mentés → Szüneteltetés és kilépés → elkülönített Elvetés. Nincs nyerttrófea/mentve állítás a writer sikere előtt; kalória csak valós forrásból.

**Elsődleges / másodlagos műveletek:** Jelenlegi fő út: FinishWorkoutDialog.onConfirm(effort) → closeFinishModal → finishWorkout(effort,onSaved). Pause: leaveWorkout. Discard: openDiscardModal/discardWorkout. Tervben a kiválasztás vizuális draft, a valódi finish csak explicit Mentésre hívható; note callback jelenleg nincs.

**Scroll / billentyűzet / Back:** Back/Mégse bezárja az összegzést, nem ment; siker előtt nincs navigate. Nagy font/IME esetén külön görgethető form és hozzáférhető mentés. Finish/restore/owner lockot a UI nem oldja fel.

**Üres / töltő / hiba / permission és session:** Íráshiba vagy finalize/discard hiba megtartandó szerkesztési értékekkel, külön retry. Nincs „mentve” nyugta részleges natív lezárásnál.

**Nézetspecifikus elfogadás:** 2.0font, DE/RO: hat válasz legalább48dp, nem42dp magas buttonok. TalkBack kiválasztott effort külön a Mentéstől. Pause nem hoz naplósort vagy naptári teljesítést; discard külön confirmation.

**Forrásból bizonyított nyitott kötés:** G07: a jelenlegi modal azonnal ment és nincs megjegyzés vagy összegző input contract. A note/fájdalom részletes writer Root-funkciócsomag; nem új ViewModel API dizájnindokból. A phase2 nem nevezheti késznek ezt a teljes képet e hiány rendezése nélkül.

## V13. Napló

**Meglévő hely:** `journal`. **Megőrzési ID-k:** C01, C07.

**Meglévő forrás:** [journal](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L55) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)

**WebView folytonosság és kép:** A main kereső, hónap/időszak és PR-jelzés marad; a mockup lapozott, jól szkennelhető kártyáihoz illesztjük. Nem csak a képernyőre már betöltött lapban keresünk.

**Felépítés, felülről lefelé:** Kereső → hónap/időszak → szűrő összefoglaló → dátumos workoutkártyák (név, teljesítés, idő, sorozat/PR) → append loading/retry. Útvonalthumbnail csak ténylegesen betöltött meglévő adatból, nincs listaszintű route-hydrate.

**Elsődleges / másodlagos műveletek:** Fő: openDetails(row.id). Másodlagos: setSearch/setMonth, Paging retry. Az ID stabil; a kártya nem tölt részletet vagy fotófájlt csak a díszítésért.

**Scroll / billentyűzet / Back:** LazyPagingItems stabil kulcs; query/month és scroll vissza részletből. IME keresőhöz, Back előbb szerkesztő/részlet réteg, majd főoldal. Részletdialog nem hagy két aktív focus-rootot.

**Üres / töltő / hiba / permission és session:** Üres napló / nincs keresési találat / appenderror külön. PR hiány nem „0PR rekord”. Betöltő kártyákhoz nincs kamu edzésnév vagy mérés.

**Nézetspecifikus elfogadás:** Kis kijelzőn legalább két áttekinthető rekord, nagy betűn egy is elfogadható, minden művelet elérhető. DE/RO hosszú név wrap. TalkBack dátum → cím → státusz/PR → megnyitás; mini térkép dekoráció csak igazolt meglévő geometryből.

**Forrásból bizonyított nyitott kötés:** G06: listában nincs alaptérkép/route geometry betöltés; a látványterv várostérképes thumbja nem kész funkció. Ne hozzunk létre N+1 route-queryt a dizájnért.

## V14. Naplózott edzés részletei

**Meglévő hely:** `journal / selectedWorkoutId modal`. **Megőrzési ID-k:** C02, C03, C04, C05, C07.

**Meglévő forrás:** [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65) · [journaledit](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt#L33) · [sources](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalHealthPanel.kt#L23) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257)

**WebView folytonosság és kép:** A main részletes sorozatok, Statisztikák/Forrás és utólagos szerkesztés marad. A mockup három füle, időtartam-hierarchiája és külön fotó/útvonal elérése a cél.

**Felépítés, felülről lefelé:** Edzésazonosító, dátum/név → időmutatók → scrollozható fülek Gyakorlatok/Statisztikák/Forrás → teljes gyakorlat/sorozatlista → fotó/útvonal → Szerkesztés/Calendar intent → megerősített törlés.

**Elsődleges / másodlagos műveletek:** Fő: openEditor / szerkesztő save callback. Másodlagos: openRouteMap, refreshWorkoutHealth(activity), createCalendarIntent, requestDeleteWorkout/confirmDeleteWorkout; privát fotó modal. A két health source külön jelenik meg.

**Scroll / billentyűzet / Back:** Full-screen dialog jelenlegi close/back szerződése megmarad, a közös gear hozzáférhetőségét a dialogon belüli guardolt nav-hook nélkül nem ígérjük. Tab/scroll workout-ID-hez kötött. IME csak editorban, cancel nem ír.

**Üres / töltő / hiba / permission és session:** Loading/missing/detailError retry; photosMissing és health partial saját állapot. Delete vagy mentés közben close/back csak meglévő engedéllyel. Az alkalmazott feedback nem válik módosíthatóvá új chip miatt.

**Nézetspecifikus elfogadás:** 2.0font hosszú edzésnév/tabs nem fedik a listát. TalkBack sorozatonként súly/ism/idő/RIR és forrás. Ugyanaz az edzés-ID marad edit után. Fotó törlés és workout törlés külön jelentésű.

**Forrásból bizonyított nyitott kötés:** G08 megengedett modal-kivétel: a JournalDetailDialog külön Window; a NATIVE_MOCKUP_REVIEW szerint beágyazott dialógusra nem szükséges új fogaskerék. Nem kell új nav-hookot vagy hatodik fület bevezetni emiatt.

## V15. Privát útvonaltérkép

**Meglévő hely:** `WorkoutRouteMapDialog modal`. **Megőrzési ID-k:** B13, C06.

**Meglévő forrás:** [map](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L48) · [mapcanvas](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L79) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)

**WebView folytonosság és kép:** A bevált piros X és a privát útvonalszakaszok maradnak. A mockup teljes képernyős útvonal és alsó adatpanel kompozíciója a cél, nem a generált Budapest háttérképe.

**Felépítés, felülről lefelé:** Cím + piros veszélytokennel megkülönböztetett X → route point/partial állapot → nagy saját geometry canvas → start/path/finish legenda. Csak létező összesített adat lehet alsó panelen. Nincs fogaskerék vagy bottom nav a térképmodalban.

**Elsődleges / másodlagos műveletek:** Fő/másodlagos: onDismiss (X, rendszer-Back), minden aktív/journal belépő ugyanazt a canvasadatot adja. Nincs stop/finish writer a close útján.

**Scroll / billentyűzet / Back:** Window teljes méret, rendszer insetek, X48dp. Route bounding box elfér, szakaszok nincsenek összekötve hiányzó pontokon. Nincs IME; Back csak ezt a modalt zárja.

**Üres / töltő / hiba / permission és session:** Offline saját route továbbra rajzolható; hiányzó rész explicit jelzés. Alaptérkép hiánya nem routehiba és nincs új offline tile-ígéret.

**Nézetspecifikus elfogadás:** TalkBack route szakasz/pont összefoglaló és X címke. Nagy font mellett a címke/legenda nem fedheti a close-t. X után GPS/stopper/session állapota változatlan.

**Forrásból bizonyított nyitott kötés:** G06: MercatorRouteCanvas saját Path-rajz; nincs tile provider, zoom/recenter callback vagy térképcache. Ezeket a mockupból nem szabad működőként átvenni.

## V16. Fejlődés

**Meglévő hely:** `progress; weight_journal közös elérés`. **Megőrzési ID-k:** C07, C08, C09, C10.

**Meglévő forrás:** [progress](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt#L61) · [progressstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L40) · [cardio](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioProgressPanel.kt#L35) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48) · [weightvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalViewModel.kt#L40)

**WebView folytonosság és kép:** A main gyakorlatválasztó, 1RM/volumen, izomterhelés, kardió-időszakok és testsúly megmarad. A mockup belső fülei az információkat csoportosítják, nem tüntetik el az összetett régi statisztikát.

**Felépítés, felülről lefelé:** Áttekintő cím → Erőnlét/Mennyiség/Kardió/Testsúly helyi panelek → az adott meglévő queryhez tartozó választók → valódi grafikon + táblás összefoglaló → testsúlynapló gyorselérés. A meglévő state-ben nem létező időszakszűrőhöz nem rajzolunk placebo chipet.

**Elsődleges / másodlagos műveletek:** Fő: selectExercise, selectCardioActivity, onWeightJournal. Másodlagos: retryCardio/loadProgressData, WeightJournalViewModel.setRange/retryReads. Mérésaggregátumot a UI nem számol újra.

**Scroll / billentyűzet / Back:** Azonos selection/scroll/panel visszaállítás; nagy grafikonhoz nincs vízszintes oldalhúzás. IME csak testsúly editorban. Back a közös weight naplóból ugyanide.

**Üres / töltő / hiba / permission és session:** Loading és cardioError/trenderror külön a panelekben; nincs minta e1RM/history pont. A napi/zóna-váltás meglévő refresh collectora megmarad.

**Nézetspecifikus elfogadás:** TalkBack grafikonalternatíva mértérték/lista és mintaösszeg, nem puszta canvas. Kis/nagyfont: kártyák egy oszlop, hosszú DE/RO periódus felirat wrap. Same-count history edit frissítése megmarad.

**Forrásból bizonyított nyitott kötés:** G10: progressPhotos listát betölt a VM, de a ProgressScreen nem renderel fotógalériát vagy callbacket. A mockup Testsúly/Fotó gyorseléréshez Root dönt a hiányzó közös fotó-entryről; a strength időszakszűrés sem található a publikus state-ben.

## V17. Egészség és részletes napló

**Meglévő hely:** `health; HealthRecordBrowser; weight_journal`. **Megőrzési ID-k:** D01, D02, D03, D04, D05, D06, D07, D08, D09, D10, D11, D12, D13, D14, E09, E10.

**Meglévő forrás:** [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [metrics](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthMetrics.kt#L10) · [records](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt#L63) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48)

**WebView folytonosság és kép:** A main teljes naplója, forrásai, 26 metrikakulcsa és alvásrészletei megmaradnak. A mockup hat csempéje áttekintő hierarchia, nem metrikalimit. Lépés/aktív/összes energia nem összeadás.

**Felépítés, felülről lefelé:** Szolgáltató/lastSync/partial → normál/full frissítés és progress/cancel/retry → választott hónap/nap/csatorna → fontos metrikák → teljes metrikacsoportok → alvás/rekord részletek → testsúly → Eszközök és adatforrásbeállítások. Statisztika és permission külön.

**Elsődleges / másodlagos műveletek:** Fő: syncHealth, record detail megnyitás és HealthRecordBrowser range/source/paging. Másodlagos: setProvider, setDailySourcePreference, setAutoOnOpen, toggleHcBackground/toggleBleBackground, onWeightJournal; kézi Samsung consent megmarad.

**Scroll / billentyűzet / Back:** Metrikarács2oszlop alapból,1oszlop nagyfontnál. A részletbrowser scroll/query megmarad bezárás után; új sync nem állítja át a kiválasztott napot. Dátum natív picker, Back a browserből azonosnapra.

**Üres / töltő / hiba / permission és session:** Engedélyhiány/providerhiány/partial/syncerror/üresnap külön, no data=—. Jövőbeli/régi adat nem élő. HC/Samsung/BLE külön forrás és időbélyeg; a timestamp nélküli forrásbadge nem lehet „friss”.

**Nézetspecifikus elfogadás:** Kis nézet és DE/RO: minden metrikakulcs/részlet elérhető. TalkBack szám+egység+forrás+dátum; progress oldalszámból/napból jön. 30 nap/full/incremental és régi nap/source selection megmarad. Kézi testsúly update nem ír provider-recordot.

**Forrásból bizonyított nyitott kötés:** —

## V18. Coach

**Meglévő hely:** `coach`. **Megőrzési ID-k:** C11, C12, C13, C14, A09, A10.

**Meglévő forrás:** [coach](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt#L23) · [coachvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachViewModel.kt#L26) · [readiness](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt#L14)

**WebView folytonosság és kép:** A main tanácsok, terhelés/fájdalom korlátozás és külön Samsung/saját/Coach érték marad. A mockup nagy readiness körét az egyetlen NativeCoachReadiness eredmény tölti ki.

**Felépítés, felülről lefelé:** Nagy Coach score + korlátozó ok → Samsung energia és TrainPilot becslés két névvel → frissesség/tényezők → javasolt programnap CTA → gyakorlatonkénti lenyitható tanács → megerősített variációelőnézet.

**Elsődleges / másodlagos műveletek:** Fő: meglévő onStart vagy upcoming esetben onCalendar; másodlagos: refreshHealth, previewVariation/saveVariation/closeVariation, onHealth/onProgress/onPrograms. Read-only tanács nem alkalmazott terv.

**Scroll / billentyűzet / Back:** Egy scroll, számpanel csak addig nagy, amíg az első ajánlás elérhető. Nagyfonton két pontszám vertikális. Back variációt zár/route-ra lép, nem fogad el programot.

**Üres / töltő / hiba / permission és session:** Nincs score: —, nem0. Elavult/hiányzó Samsung és fallback külön. Loading/error/reload megtartja az engedélyezett tanácsokat, nem jelzi késznek a félkész új readet.

**Nézetspecifikus elfogadás:** Azonos inputnál Home/Health recovery nem veheti át a terheléssel csökkentett Coach score-t. TalkBack három külön skála és indok, nem „87+61=54”. DE/RO a körtől jobbra szűk szöveg helyett alatta jelenik meg.

**Forrásból bizonyított nyitott kötés:** —

## V19. Beállítások

**Meglévő hely:** `settings`. **Megőrzési ID-k:** A02, C14, E11, E12, E13, E14, E15, E16, E17, E19, E20.

**Meglévő forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [themepicker](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt#L31) · [lang](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeLanguagePicker.kt#L32) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)

**WebView folytonosság és kép:** A main szekciók, 32paletta és négy nyelv marad; a mockup soros beállításkártyái a hosszú Material-listát tagolják. A user mentett témáját a grafit/arany bemutató kedvéért nem reseteljük.

**Felépítés, felülről lefelé:** Megjelenés (nyelv/témák) → Profil → Edzés/időzítő/haptika/mód → Kapcsolatok/Google → Mentés-visszaállítás → Adatvédelem/karbantartás. Szekciócímek és fogaskerék kontextus megmarad. Veszélyes műveletek nem elsődleges arany főgombok.

**Elsődleges / másodlagos műveletek:** Fő: meglévő editor Save callbackek. Másodlagos: setTheme/setLanguage, beginTimerDefaults/saveTimerDefaults, saveProfile, training-mode compare, connect/sync/disconnect, három Google maintenance. Nincs új verzióadat vagy backup-lista lekérés.

**Scroll / billentyűzet / Back:** IME profile/timerben a formnak foglal helyet. Back először megnyitott panel, majd eredeti route/session; Settingsbe lépés önmagában nem pause. Dialog input local draft nem kerül újra inicializálásra háttérrecompozíciótól.

**Üres / töltő / hiba / permission és session:** Versengő restore/sync/writer busy és hibák panelspecifikusak; új felület nem kerüli meg a guardot. Google confirmation account/scope/cancel megmarad, token nem UI.

**Nézetspecifikus elfogadás:** 32 palette választható és persistált, HU/EN/DE/RO az egész felületet frissíti.48dp switchsor, hosszú német label-wrap. TalkBack szekció és be/ki/mentési állapot. Aktív időmérés Settings roundtrip során folytatódik.

**Forrásból bizonyított nyitott kötés:** G09: globális Permissions/Watch külön route nincs; a mockup ezekre mutató sorait csak meglévő kontextusos entryvel vagy Root nav-bekötéssel lehet megvalósítani. Verziósort csak valós BuildConfigból, ha Root jóváhagyja a hiányzó kötést.

## V20. Óra és szinkron

**Meglévő hely:** `active_workout / ownership dialog + Home status`. **Megőrzési ID-k:** E05, E06, E07, E08.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [ownership](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L805) · [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A main kétirányú ownership/protokoll és offline saját folytatás marad. A mockup státuszkártyáit a meglévő session-owner felület köré tervezzük; nem „következő edzésnél” ígéretet.

**Felépítés, felülről lefelé:** Valódi kapcsolat → aktuális workout tulajdonos → pending/ack/blocked állapot → engedélyezett átadás/visszavétel → offline magyarázat. Akkumulátor csak valódi adatból, kapcsolat nem következik pusztán egy régi profilból.

**Elsődleges / másodlagos műveletek:** Fő: requestOwnershipBackFromWatch vagy startWatchMeasurementRemotely(activity), a jelenlegi UI permission/owner szabályaival. Másodlagos: meglévő ownership dialog zárás/megtekintés; Home csak státusz, nem hamis mutatós órafotó.

**Scroll / billentyűzet / Back:** Őrizze az aktív session nézetét és a bottom eredetfület. Back csak a panelt zárja. Nincs IME; ownership közben a globális navigation guard megmarad.

**Üres / töltő / hiba / permission és session:** Disconnected/ack-pending/phone-owner/watch-owner és recovery blokk külön szöveg. Reconnect nem indít automatikus átadást vagy két stoppert.

**Nézetspecifikus elfogadás:** TalkBack kimondja a tulajdonost és az átadás függő állapotát, a zöld pont nem elég. Kis kijelzőn a manuális átadás és a figyelmeztetés együtt látható. Offline→online nem lesz néma ownercsere.

**Forrásból bizonyított nyitott kötés:** G09: standalone watch_sync route és settings callback nincs; az aktuális aktív editorba szervezett panel megvalósítható. Teljes külön oldalhoz Root prezentációs nav-hook szükséges; Wear-kód nem módosítható.

## V21. Mentés és visszaállítás

**Meglévő hely:** `settings / SAF + recovery`. **Megőrzési ID-k:** E11, E12, E13, E14, E15.

**Meglévő forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [recovery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt#L19) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)

**WebView folytonosság és kép:** A bevált JSON/ZIP/Drive és explicit egészségadat-beleegyezés megmarad. A mockup három formátumkártyáját a már meglévő SAF/Drive callbackekhez kötjük, kitalált mentéslista nélkül.

**Felépítés, felülről lefelé:** Formátum és tényleges tartalom → health consent → Export → Import külön blokk → interrupted recovery + Folytatás/alternatív JSON/ZIP → Google recoveryEarlier és maintenance külön veszélyzóna. Restore tiltásszöveg: aktív edzés KÖZBEN, nem ELŐTT.

**Elsődleges / másodlagos műveletek:** Fő: meglévő JSON/ZIP launcherek + onSaf*Completed; Google syncDriveCloud. Másodlagos: resumeReplacement/onJson/onZip recovery; explicit erasure sajátconfirm. SAF cancel nem hív import/export writert.

**Scroll / billentyűzet / Back:** Scrollos szekció Settingsen belül, nem új route. Rendszer fájlválasztóból Back ugyanoda és meglévő restore-fence mellett tér vissza. Nem adunk dummy fájllistát vagy kijelölt visszaállítás CTA-t hiányzó API-val.

**Üres / töltő / hiba / permission és session:** Hiba/részleges ZIP/restore pending explicit recovery. Műveleti siker csak repository eredményből. Busy alatt a felhasználó tudja, mely művelet fut; aktív session nem vész el.

**Nézetspecifikus elfogadás:** Kis/nagyfont és DE/RO: tartalommagyarázat nem sűrített apróbetű. TalkBack formátum+tartalom+scope és megerősítés. Import hiba után továbbra van retry/alternatíva, nem dekorációs zöld pipa.

**Forrásból bizonyított nyitott kötés:** G11: nincs mentések listázására/preview-lista választására state a SettingsUiState-ben. A mintabeli dátumok/méretek/sorok nem jeleníthetők meg valós funkcióként. Adatintegritási QA-ból jövő jövőbeli baseline-t Root adja.

## V22. Engedélyek

**Meglévő hely:** `kontextusos panelek: GPS/health/BLE/settings`. **Megőrzési ID-k:** E18, E09, E10, D09, B11.

**Meglévő forrás:** [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74)

**WebView folytonosság és kép:** A main kontextusos engedélykérés és rendszerlinkek maradnak. A mockup állapotsoros elrendezése átvehető a létező panelekben, nem új második permission-managerrel.

**Felépítés, felülről lefelé:** Funkcióhoz szükséges hozzáférés → rendszerhely külön sor → hiány oka → konkrét Android prompt/settings elérés → explicit visszatérés/újrapróba. Külön HC/Samsung/BLE/Wear és értesítési indokok.

**Elsődleges / másodlagos műveletek:** Fő: NativeGpsPermissionRecovery existing request/retry/settingsIntent; health permission launcherek; BLE háttér permission launcherek. Másodlagos: cancel/back és állapotfrissítés. ON_RESUME csak olvas, nem GPS-start.

**Scroll / billentyűzet / Back:** Rendszerablak visszatérési út megmarad; eredeti workout/generation/exercise/set requestid nem cserélődhet újra. Nincs IME. Back elveti a pending felhasználói szándékot, nem teljesít sorozatot.

**Üres / töltő / hiba / permission és session:** Approximate vs precise, permanently denied vs egyszer denied, location OFF, SDK unsupported és app-szintű Samsung korlát külön. Általános engedélylista nem állíthat minden platformtámogatást zöldre.

**Nézetspecifikus elfogadás:** TalkBack pontos engedély+státusz+következmény. Kis képernyőn a „rendszerhely” nem rövidül „GPS engedély”-re. Permissiongrant után sem startol háttérben GPS; explicit felhasználói retry kell.

**Forrásból bizonyított nyitott kötés:** G09: önálló permissions route és közös permission-list state nem létezik. A meglévő panelek vizuális rendezése készíthető; a kép szerinti közös oldal Root-adaptert igényel.

## V23. Privát edzésfotók

**Meglévő hely:** `active_workout attachedPhotos + journal photo modal`. **Megőrzési ID-k:** C05, C10, E13, E15.

**Meglévő forrás:** [activephotos](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L418) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257) · [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A main kamera/galéria, privát tárolás és tudatos törlés marad. A mockup két képkártyája valódi mentett fotót jelenthet, nem súlyzós stock-képet.

**Felépítés, felülről lefelé:** Privát tartalom/backup consent magyarázat → meglévő fotók (szükség szerint lapozott decode) → camera/gallery aktív edzésben → full-view meglévő fotómodal → egyértelmű adott-ID törlés a támogatott kontextusban.

**Elsődleges / másodlagos műveletek:** Fő: meglévő cameraLauncher/galleryLauncher és onPhotoCaptured. Másodlagos: ActiveWorkoutViewModel.deletePhoto(photo.id), journal photo megnyitás/bezárás. Fotó készítése nem állít meg edzést.

**Scroll / billentyűzet / Back:** Scroll/thumbs és full-view elválasztva; Back csak kép/modal. Nagy kép dekódolása privát repositoryból, nincs teljes archívum hydrate. Törlés célja látható,48dp gomb.

**Üres / töltő / hiba / permission és session:** Üres, filemissing, decodeerror, restore-fence, consent-/feltöltési állapot a valós adatok szerint. Nem ígérjük, hogy „soha nem kerül felhőbe”, ha user ZIP/Drive fotómentést engedélyez.

**Nézetspecifikus elfogadás:** TalkBack fotó címke/dátum/kontextus, törlési scope. Nagyfontnál a grid1oszlopra áll. Thumbnail maxedge és aktív mérés melletti scroll nem foghatja az UI-threadet. Képbezárás nem finish.

**Forrásból bizonyított nyitott kötés:** G12: aktív attachedPhotos sorban jelenleg üres Box +24dp delete ikon, nincs bitmapmegjelenítés vagy fotó-view callback. Journal PrivateJournalPhoto működő megjelenítő. Standalone progress photos út hiány G10; új mutáció nincs dizájncsomagban.

## V24. Program szerkesztése

**Meglévő hely:** `programs / ProgramEditorDialog`. **Megőrzési ID-k:** A07, A08, A09, A14, C14.

**Meglévő forrás:** [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A main napok/gyakorlatok sorrendje, prescriptions és saját elemek védelme marad. A mockup fókuszált napfül + egy lenyitott gyakorlat + külön Mentés/Mégse jó vizuális minta.

**Felépítés, felülről lefelé:** Név → programnapválasztó → nap neve/sorrend → gyakorlatkártyák, kijelölt részletben sets/reps/load/rest és típusspecifikus mezők → hozzáadás → Mentés/Mégse → külön veszélyes törlés/visszaállítás.

**Elsődleges / másodlagos műveletek:** Fő: onSave(draft) → saveProgram(editing,draft,language,editorToken). Másodlagos: onCancel, prescriptions meglévő editor, sorrend/move és exercise chooser, delete/restore csak ahol eredeti szerződés engedi. Nincs új két szerkesztő ugyanazon draftnak.

**Scroll / billentyűzet / Back:** IME fókuszhoz bring-into-view, min48dp sorrendkezelők. A 3×gomb és input nem fér minden sorba: kis/nagyfonton címkézett vertikális mezőpárok. Back/Mégse nem ment, megnyitott editorToken/CAS érvényben marad.

**Üres / töltő / hiba / permission és session:** Saving/invalid/stale/writefailed a draftban marad, nem defaultokat tölt újra. A template-reset/copy működés meglévő domain-policyt követ, nem adunk új globális reset ígéretet.

**Nézetspecifikus elfogadás:** DE/RO hosszú gyakorlat + kg/kéz érték együtt olvasható. TalkBack sorrendindex és explicit Fel/Le fallback drag mellett. Mégse után DB változatlan; siker után ugyanazon program és megfelelő nap marad elérhető.

**Forrásból bizonyított nyitott kötés:** —

~~~~

---

## docs/design/final-ui-ux/04-implementation-and-acceptance.md

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/docs/design/final-ui-ux/04-implementation-and-acceptance.md

SHA-256: `f49c9b15e9060d245d581719baf788b4ac0eed5cd3042d0e892b3b07a683d3ac`

~~~~markdown
# Nyitott kötések, kivitelezési sorrend és elfogadás

> **Aktuális feloldás (2026-10-27):** Root P0/P1 prezentációs megvalósítást
> elkezdhet az exact31dbcde2 közös zöld baseline-ról (Actions40458015954,
> 1332JVM+112phoneUI PASS,3WearUI+2upgradeSUCCESS). A régi docs-only/QA-aktív
> megkötések az alábbi történeti baseline-ra vonatkoznak. Másik agent indítására
> ez nem felhatalmazás. G04restpause már bekötve/tesztelve; motor/adatút readonly.

> **Aktuális Root pontosítás (2026-10-26):** [UI belépési kapu](../../plans/NATIVE_UI_ENTRY_GATE.md)
> és [korrigált G02–G12 audit](../../reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md)
> az irányadó a történeti baseline mellett. G01/G10 integrált, G06 opcionális online
> map+zoom létező main funkció; nem tisztán új feature. Teljes UI-kivitelezés még nem indult.

## Pontos forráseltérések, nem dekorációval javítandó funkciók

A G01–G12 sorok az exact c116a042 olvasási baseline-ra vonatkoznak. Nem a másik QA-ág mostani állapotát írják le, és nem újabb teljes audit. A phase2 integrált baseline-on mind újraellenőrizendő. G08 kifejezetten megengedett eltérés.

### G01. Home energia-hitelesség/fallback

**Forrás:** [homeenergy](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L117) · [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36)

**Megfigyelés:** HomeViewModel a napi energyScore mezőből közvetlen recoveryText-et képez; nincs ebben az adapterben NativeCoachReadiness hívás vagy source/time kimenet.

**Következő konkrét delta/korlát:** Root adatadapter review; a dizájn nem írhat saját score-képletet. Addig semleges címke, nincs hiteles mai Samsung-badge. A01/V01 és V18 konzisztencia külön kapu.

### G02. Óra akkumulátor és modell

**Forrás:** [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36) · [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62)

**Megfigyelés:** watchBattery nullable, Home adatbetöltés nem ad hozzá értéket; a kártya Watch státuszt kap.

**Következő konkrét delta/korlát:** Nem írható be a mockup92% vagy GalaxyWatch7 mint valós alapérték. Hiányban —/ismeretlen. További adatadapter csak Rootnál.

### G03. Könyvtár részlet → aktív hozzáadás

**Forrás:** [librarydetail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L165) · [library](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L115) · [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105)

**Megfigyelés:** ExerciseLibraryScreen signature csak language/model; részletében nincs onAddToWorkout. Aktív editor saját pickerében addExerciseToWorkout létezik.

**Következő konkrét delta/korlát:** A működő aktív picker marad. Külön új könyvtári Hozzáadás CTA csak Root által adott guardolt entryvel; nem no-op callbackkel.

### G04. Pihenő külön pause

**Forrás:** [rest](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1078) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**Megfigyelés:** RestTimerFloatingBar paraméterei remainingSec/onAdjust/onSkip/canAdjust/language. Nincs rest-pause callback.

**Következő konkrét delta/korlát:** ±15 és Skip változatlanul köthető. Külön pause igényét Root a meglévő natív timer contracton oldja meg; teljes workoutPause nem nevezhető át rest-pause-nak.

### G05. GPS metrikák vizuális bekötése

**Forrás:** [distance](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L808) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**Megfigyelés:** gpsAccuracyMeters és liveHeartRateBpm mező létezik, DistanceMeasurementPanel jelenleg táv/idő/státusz/gomb szintet jelenít meg.

**Következő konkrét delta/korlát:** Phase2 prezentációs bekötés lehetséges az adott fieldből, nulla új writer/számítás. Pontszám/accuracy/forrás csak ha state rendelkezésre áll. Hiányban —.

### G06. Háttértérkép és recenter

**Forrás:** [map](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L48) · [mapcanvas](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L79) · [journal](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L55)

**Megfigyelés:** MercatorRouteCanvas Canvas/Path saját pontokat rajzol; nincs tile provider/zoom/recenter paraméter. Napló summary nem tartalmaz térkép geometryt.

**Következő konkrét delta/korlát:** Nem funkcionális UI-bugként fedjük el: a privát route-canvas kidolgozható; várostérkép, offline tile és recenter új külön funkció lenne. Nincs N+1 listahydrate vagy generált mapbitmap.

### G07. Lezárási form és note

**Forrás:** [finish](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1128) · [finishwriter](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L1005)

**Megfigyelés:** FinishWorkoutDialog az effort választáskor onConfirm-et hív; ViewModel.finishWorkout(effort,onSaved) nem kap note-ot. Parent előbb zárja a modalt.

**Következő konkrét delta/korlát:** Vizuális effort-selection + explicit Save ugyanazon callbackkel megoldható, de valódi note/pain-részlet tartós írása nincs kitéve. Root minimális funkcióhook szükséges; sikertelen mentéshez draftmaradás tesztelendő, nem új finalizer.

### G08. Naplórészlet gear – megengedett kivétel

**Forrás:** [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**Megfigyelés:** A journal részlet külön Window. NATIVE_MOCKUP_REVIEW szerint beágyazott dialógusra nem kell új fogaskerék.

**Következő konkrét delta/korlát:** Nem blocker és nem új funkcióhiány. Marad a close/back; a kép gearjét nem erőltetjük új route-hívással a modalba.

### G09. Óra/Engedélyek és inline sync önálló oldalak

**Forrás:** [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92) · [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [calendar](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L28)

**Megfigyelés:** A NavHostban nincs watch_sync/permissions/backup/photos route; a funkciók a meglévő Settings/Health/aktív panelen elérhetők. CalendarScreen nem kap onSync-et.

**Következő konkrét delta/korlát:** Kevesebb oldal megengedett: a24nézet lehet ugyanazon panel. Új nav entryt csak Root adhat meglévő navigationAllowed/backstack contracttal. Nincs mockupból kitalált új üzleti API.

### G10. Fejlődés fotó-entry és strength időszak

**Forrás:** [progressstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L40) · [progress](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt#L61)

**Megfigyelés:** progressPhotos a VM-ben betöltött lista, ProgressScreen nem rendereli; a strength state-hez nincs időszakválasztó contract. Kardió és testsúly időszakai külön meglévő utak.

**Következő konkrét delta/korlát:** Meglévő súly/kardió queryt nem cimkézzük strength időszakszűrésnek. Fotógaléria/entry és szükséges filter Root bekötés; addig nincs placebo fül/gomb.

### G11. Mentéslista a mockupon

**Forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [recovery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt#L19)

**Megfigyelés:** SettingsUiState nem ad fájl/remote-backup listát; SAF választók és explicit recovery elérés van.

**Következő konkrét delta/korlát:** Formátumkártya a meglévő launchert hívhatja. A képen lévő fájlméret/dátum/előnézet-lista nem megvalósított API. A data-integrity QA jövőbeli eredményét nem előlegezzük meg.

### G12. Aktív fotó thumbnail és célméret

**Forrás:** [activephotos](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L418) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257)

**Megfigyelés:** AttachedPhotos sor Boxot és24dp delete IconButtont tartalmaz; ott nincs bitmap. Journal PrivateJournalPhoto valós privát decode/load/error megjelenítő.

**Következő konkrét delta/korlát:** Phase2-ben átvehető privát megjelenítő és48dp delete cél, a meglévő photo-id writerrel. Új fotómutáció/Drive-policy nem fér dizájnindokba.

## Kötelező, globális G-AC elfogadás (mind a24nézet)

Ezek **következő szakaszban végrehajtandó** próbák, most mind NOT_RUN.

| ID | Feltétel | Elvárt eredmény |
|---|---|---|
| G-AC01 | 320×640dp és360×800dp, font1.0 | Nincs vízszintes oldaloverflow; utolsó művelet felgörgethető; rendszer/bottom/banner nem takarja |
| G-AC02 | font1.5 és2.0, DE/RO hosszú címke, közepes kijelzőn is | Tördelés/one-column fallback, nincs fix42dp/levágott főgomb; minden formmező és footer elérhető |
| G-AC03 | HU/EN/DE/RO, valós program-/gyakorlatnevek | UI label locale szerinti, szám/egység/dátum helyes, useradat nincs lefordítva/módosítva |
| G-AC04 | TalkBack be, kijelölt/futó/hibás/fogyatékos állapot | Logikus cím→adat→művelet sorrend, minimum48dp, ikonok jelentése és selected/disabled állapot; nincs250ms beszédáradat |
| G-AC05 | Mind32paletta választása + kontraszt | Választás persistálódik, derived onAccent/accentText/focus outline; szöveg4.5:1, nagy szöveg/vezérlő3:1; nem csak szín jelöl állapotot |
| G-AC06 | IME és Back, modal/előnézet/Settings roundtrip/rotation | Ugyanaz a selection/draft/scroll és session; cancel nem ír, Settings nem pause; stale callback nem írhat későbbi célra |
| G-AC07 | Üres, betöltő, hiba, offline, permission-deny | Külön, valós state, egyértelmű explicit retry, nincs kitalált mérés/siker vagy felesleges unattended prompt |
| G-AC08 | Futó/paused/Watch-owner/pending transfer/finalization/restore | Mutáció és navigation guard marad; UI-változás nem indít/fejez be órát, GPS-t vagy edzést; régi target elutasított |

A nézetspecifikus bizonyíték a02-screen-contracts minden Vxx fejezetében szerepel.
A globális feltételek nem válthatók ki pusztán egy álló alapképernyővel.

## A meglévő UI-QA munkától függő startfeltétel

Az átadó szó szerint: **„Második szakasz – csak az UI-QA átadása és Root
területfeloldása után”**. A saját docs ágról sem ui/**, sem phoneUItest, sem
screenshot/evidence script, sem workflow nem írható most. A PR261 tényleges
olvasott HEAD835e533f QA-ág volt a vizsgálatkor; ez nem automatikus feloldás.
Root adja meg az új implementációs commitot és az ági frissítés módját. Addig
nincs cherry-pick/rebase/merge a QA-ágra vagy onnan, nincs saját másolt workflow.

## Kivitelezési sorrend a terület felszabadítása UTÁN

Minden csomagnál ugyanaz a minta: előtte állapot/screenshot exact fixture-rel →
csak prezentációs hunk → meglévő üzleti assertionök → új vizuális/a11y próbák →
utána változatlan screencap. A későbbi fájllista **javasolt hunkscope**, nem mostani
írási engedély. ViewModel/Room/motor fájlok csak olvashatók maradnak.

| Csomag | Javasolt képernyő-/resource fájlok | Megőrzendő határ / készültségi kapu |
|---|---|---|
| P0 baseline és közös rendszer | ui/theme/TrainPilotTheme.kt; ui/navigation/NativeMainHeader.kt; új tiszta ui komponensek és saját locale XML, ha Root kijelöli | NativeThemeTokens és palette policyt nem írjuk át vakon; nav guard és5route nem változik;32paletta+4nyelv+small/font kontraszt baseline |
| P1 öt főoldal | ui/home/HomeScreen.kt; ui/programs/TrainingLandingScreen.kt; ui/journal/JournalScreen.kt; ui/progress/ProgressScreen.kt; ui/health/HealthDashboardScreen.kt | Csak card/typography/layout/scroll/semantics. G01/G10 új adatadapter esetén Rootnak külön patchigény, nem saját VM-átírás |
| P2 négy aktív mérési mód | ui/workout/ActiveWorkoutScreen.kt és új tiszta megjelenítők | Renderelt NativeWorkoutTarget/NativeTimerTarget, update250ms köre ne indítson queryt. Stop/auto-complete/rest/ownership/Settings roundtrip megőrzés; G04/G07 domainhook Rootnál |
| P3 lista és szerkesztők | ui/programs/TrainingProgramsScreen.kt; ExerciseLibraryScreen.kt; CustomExerciseDialog.kt; ProgramEditorDialog.kt; ProgramPrescriptionDialog.kt; PersonalProgramPreviewDialog.kt; ui/calendar/CalendarScreen.kt | Cancel/save/restore epoch és request generation nem változik. G03/G09 navigációhook külön Root döntés |
| P4 részletek és mérési előzmények | ui/journal/JournalEditorDialog.kt; JournalHealthPanel.kt; WorkoutRouteMapDialog.kt; ui/health/HealthRecordBrowser.kt; ui/progress/WeightJournalScreen.kt; ui/coach/CoachScreen.kt | Nincs history/photo/route hydrate vagy új metrikaképlet. G06 új map provider kizárt. Forrás/time/alvás/paging megmarad |
| P5 Settings/kapcsolatok | ui/settings/SettingsScreen.kt; NativeThemePicker.kt; NativeLanguagePicker.kt; NativeTimerDefaultsDialog.kt; NativeTrainingModePicker/Comparison.kt; NativeGoogleMaintenanceUi.kt; ReplacementRecoveryPanel.kt; TrainingProfileDialog.kt | Veszélyes művelet scope, Google consent és restore admission változatlan; G09/G11 hiány nem lesz dummy lista |
| P6 összesített vizuális és működési kapu | Root által kijelölt meglévő phoneUI fixture és screenshot runner, új vizuális case-ek | Csak az új baseline CI-szerződésével, nem ebből a docsból kap workflow-engedélyt. Bizonyíték: raw PNG/XML/commit/artifact/hash |

NavHost fájlban legfeljebb Root-reviewed prezentációs slot hunk tervezett
(pl. Home aktív hero). A selectDestination/goBack/navigationAllowed,
openCanonicalWorkout, ownership/restore/start/finish viselkedésének átírása
nem dizájnfeladat. A teljes32katalógus miatt SettingsStore továbbra readonly.
Új callbacktípus vagy új publikus VM szerződés esetén a munka funkcióigényként
visszakerül Roothoz, a meglévő vizuális csomag ettől függetlenül folytatható.

## Reprodukálható vizuális bizonyítékterv (nem mostani futtatás)

- Root kijelöli az **integrált before commitot**, az after commitot és ugyanazon
  tesztfixture adatkészlet azonosítóját/hashét. Model seed/clock/local zone/permission/
  locale/palette/fontScale/device-resolution/SDK egyezés nélkül nincs before-after ítélet.
- Alap: mind24nézet ×4nyelv a gold bemutatópalettával =96 állapotpár. További
 4nem-arany paletta (classicBlue, graphite, vividCyan, red) az5főoldal+4aktív
 módban =36 állapotpár. Ez132 pár (=264 raw PNG), **terv, nem mért futási szám**.
 32paletta teljes kiválasztási/persist/kontraszt tesztje és a picker megfelelően
 scrollozott raw képei külön szükségesek; csak5paletta screenshotja nem bizonyít32funkciót.
- Small/large-font és kritikus state próbák a Vxx elfogadás alapján; a kép mellett
  az üzleti eredmény (sessionazonosság, OK/undo/pause/mérés-stop) külön assert.
  G-AC06/08 nem screenshotból eldönthető. A megerősítési/engedély/restore modalt
  ugyanazon fixture-ben nyitják meg; csak izolált tesztadat és tesztfiók.
- PNG kizárólag `adb exec-out screencap -p`, teljes fájl, SHA256 az eredeti byteokra.
  Nincs crop, kompozit, retus, új szövegréteg, generált after-kép. A négy mostani
  mockup csak referencia, nem before-kép. Screenshot előkészítő helper sem írható most.
- Manifest minimum: viewId/route, fixtureId+hash, sourceCommit, screenshotCommit,
  APK source/version/package/hash, activity, SDK, pixels+density, fontScale, locale,
  zone/time fixture, themeKey, state, original PNG path+SHA256, workflowRun és artifact ID.
- Valódi XML-ben tests/failures/errors/skipped és instrumentáció declared vs terminal,
  process-crash/missing esetek ellenőrzése. Egy zöld job vagy artifactcímke nem elég.
  A meglévő evidence-summary jövőbeli integrált változata a QA-csomag része; most nem másoljuk.
- Elfogadás csak azonosított állapotpáron és futó appon. Ha valamely screenshot hiányzik
  vagy sérült, az a sor NOT_RUN/INCOMPLETE, nem automatikus PASS. APK/készülékes
  elfogadás továbbra külön, nem a jelen dokumentációs csomag eredménye.

~~~~

---

## docs/design/final-ui-ux/05-feature-preservation.md

RAW: https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/1509686ea09222ccdb8ec709105307d48c4afd89/docs/design/final-ui-ux/05-feature-preservation.md

SHA-256: `e52684c8ede9f2332ccae702287dbfb4246c290731503e7968b96cb5a3dadd4b`

~~~~markdown
# Megőrzési leltár – mind a 81 A–E követelmény

A NATIVE_DESIGN_HANDOFF leltárának lefedése. Ez **nézethez/adatúthoz rendelés**, nem új teljes main-funkcióaudit és nem „81 kész natív funkció”. A nézetek adatforrását, callbackjét és meglévő tesztforrását a03-binding tartalmazza; a következő megvalósítás során minden ID funkcionális állapotát újra kell ellenőrizni az integrált baseline-on.

| Követelmény | Specifikált nézet | Forrás és teszt | Igazolási határ |
|---|---|---|---|
| A01 | V01 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A02 | V19 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A03 | V01, V02 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A04 | V01, V02 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A05 | V01 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A06 | V02, V03 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A07 | V03, V24 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A08 | V03, V24 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A09 | V03, V18, V24 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A10 | V03, V18 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A11 | V05 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A12 | V05, V06 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A13 | V05, V06 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A14 | V05, V06, V24 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| A15 | V06 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B01 | V02, V07 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B02 | V07, V08 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B03 | V06, V07, V08, V09 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B04 | V07 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B05 | V08, V09 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B06 | V08, V09 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B07 | V07 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B08 | V08, V09, V11 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B09 | V07, V08, V11 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B10 | V07, V12 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B11 | V10, V22 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B12 | V10 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B13 | V10, V15 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B14 | V10 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B15 | V07 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| B16 | V12 | [binding](03-source-bindings.md) | G07: note/finish-szerződés külön Root-eldöntés; nem kész funkció |
| B17 | V12 | [binding](03-source-bindings.md) | G07: note/finish-szerződés külön Root-eldöntés; nem kész funkció |
| B18 | V07 | [binding](03-source-bindings.md) | Wear/protokoll megőrzés; nem új telefonos lock/óra UI ígéret; runtime NOT_RUN |
| C01 | V13 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C02 | V14 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C03 | V14 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C04 | V14 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C05 | V14, V23 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C06 | V15 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C07 | V13, V14, V16 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C08 | V16 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C09 | V16 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C10 | V16, V23 | [binding](03-source-bindings.md) | G10: progress fotó-entry hiányzik; nem kész funkció |
| C11 | V01, V18 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C12 | V18 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C13 | V18 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| C14 | V03, V18, V19, V24 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D01 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D02 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D03 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D04 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D05 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D06 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D07 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D08 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D09 | V17, V22 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D10 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D11 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D12 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D13 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| D14 | V17 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E01 | V02, V04 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E02 | V04 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E03 | V04 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E04 | V04 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E05 | V01, V20 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E06 | V20 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E07 | V20 | [binding](03-source-bindings.md) | Wear/protokoll megőrzés; nem új telefonos lock/óra UI ígéret; runtime NOT_RUN |
| E08 | V20 | [binding](03-source-bindings.md) | Wear/protokoll megőrzés; nem új telefonos lock/óra UI ígéret; runtime NOT_RUN |
| E09 | V17, V22 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E10 | V17, V22 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E11 | V19, V21 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E12 | V19, V21 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E13 | V19, V21, V23 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E14 | V19, V21 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E15 | V19, V21, V23 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E16 | V19 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E17 | V11, V19 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E18 | V10, V22 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E19 | V19 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |
| E20 | V19 | [binding](03-source-bindings.md) | SPEC_SOURCE_ONLY; runtime NOT_RUN |

~~~~
