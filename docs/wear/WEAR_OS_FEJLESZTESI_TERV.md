## Aktuális folytatás — 2026-10-17, összevont P2-csomagok

A felhasználó jóváhagyta a közös csomagokat és a folytatást. **Következő: #193 + #202 önálló órás edzés**, közös issue #193, ág `feat/193-202-watch-offline-gps`, main alap `9188932`. #202 duplikátumként lezárva, teljes követelményei #193-ban megőrizve. Utána **#212 + #190 UI-csomag → #197 edzésmódok → #184 Samsung Coach**; #190 duplikátumként #212-be átvezetve. #207 duplikátumként #206-ba átvezetve; a #208-cal már mainre került GPS STOP-teljesítés nem új fejlesztés, a maradék aktivitásnézet/UI ellenőrzése #206-ban marad.

#188 Visszavonás és #189 három másodperces érintészár elfogadva és mainen. **#192 és #211 a felhasználó kérésére sztornózott, beolvasztás nélkül lezárva.** A #192 APK-k történetiek, nincs készülékes elfogadás vagy merge. A régi #186 draft beolvasztás nélkül lezárva: a készülékesen elfogadott #195 már kiváltotta. #121 karbantartás/Play-publikálás külön marad. A közös #193 fejlesztés a #213 draft PR-ban elindult; közös tulajdonosi állapotgép, tartós átadási napló és natív/Data Layer adapterek elkészültek. 2026-10-18: a friss JS-piszkozat tartós átadási egyeztetése, natív időzítő-checkpoint és közös idővonallal működő órás folytató bekerült. A régi telefonos végrehajtás nem egyeztethet vissza az átvett edzésbe; a visszaadás új telefonos végrehajtásig szünetben marad. A telefonos visszaadási import, kifejezett folytatás, csak olvasható telefonos tükör és képességhez kötött HU/EN/DE/RO vezérlők is bekötve. Az órás végső állapot lezárásakor a friss natív forrás és tényleges aktív idő kerül a telefonra; elveszett korábbi folytatási üzenet nem okoz adatvesztést. Az órás GPS adatmag és #163-formátumú natív olvasás/törlés elkészült: korlátos privát csomagok, eszköz/epoch szerinti helyi engedély, deduplikálás, szakaszhatárok és tartós törlési védelem. A `bdc4f6c` célzott natív kapuja 53/53 sikeres, ebből 11 GPS-eset; XML ellenőrizve. A normál edzések képességhirdetése még kikapcsolt; az órás privát ponttároló és a koordinátamentes outbox-hivatkozások natív küldési/fogadási útja bekötés alatt. A Location-szolgáltatás, GPS-felület és teljes életciklus/törlés integrációja, végső aktiválás és készülékes elfogadás hátravan. Az aktuális új kapuk Actionsben futnak; korábbi commit eredménye nem a teljes funkció igazolása. Verzió változatlan **1.3.51 /2732**. Teljes teszt/build Actionsön; a #193 új funkció main/kiadásához később külön készülékes elfogadás és jóváhagyás szükséges. [Közös részletes terv](P2_193_ONALLO_ORAS_EDZES.md). Az alábbi korábbi állapotok történetiek.

# Telefonos és Wear OS automata mód, Wear UI-fejlesztési terv

## P2 #188 lezárva → #189 egyszerű érintészár — 2026. október 17.

A `0694413` telefonos + Wear tesztpárt a felhasználó elfogadta (**„Zárhatjuk, jók vagyunk :) Folytathatod”**). #209 mainre került `a06485c`, #188 lezárva; 143/143 regresszió, 6/6 telefonos UI-futás, natív/Wear kapuk, 119/119 kerek UI/háttér/szinkron sikeres. A régebbi következő szakaszok történeti állapotok.

A dokumentált következő feladat **#189**, `feat/189-wear-touch-lock` ágon, 1.3.51 /2732 verzióemelés nélkül. Az érintészár kizárólag helyi felületi védelem: külön tartós edzésazonosítóhoz kötött jelző, a kanonikus edzés/időnapló/outbox változtatása nélkül. Zár alatt a normál pager, szerkesztők és mentési/törlési megerősítők helyett olvasható edzésállapot és egyetlen feloldó vezérlő van. A háttér és telefonos vezérlés változatlanul halad. Felhasználói pontosítás: feloldás egyetlen folyamatos 3 mp-es nyomva tartással a lakaton, automatikusan a tartás közben; külön koppintás és elengedési lépés nincs. Mozgatás/több ujj/háttér/rövid nyomás megszakítja. A felengedés nem csúszhat át edzésműveletre. Képernyőébresztés és újraindítás után megmarad, más/lezárt edzésre nem kerül át. Új készülékes elfogadásig draft PR, teljes teszt és aláírt build Actionsön. Utána #192 → #193.


### #188 telefonos indítás javítása — 2026. október 16.

Készülékes hiba: telefonról indított kézi edzésnél hiányzott az órás Visszavonás, mert a telefon automatika nélkül nem osztott stabil sorozatazonosítókat. A #209 draft publikálás előtti tartós azonosítással javul (új és régi draft), meglévő azonosítók és mérési adatok megőrzésével. Célzott telefonos indítás/szinkron/kvóta és órás telefonvezérelt kézi UI-regresszió bővült. Teljes ellenőrzés és aláírt tesztcsomag GitHub Actionsben; fizikai elfogadás továbbra is hátravan. A telefonon rögzített kézi sorozat tartós visszavonási jelöltje is átkerül az órának, a telefonos készrejelölés → órás inverz út regresszióval fedett. A pontosított igény szerint a telefon alsó edzésvezérlő sora is kap külön **Visszavonás** gombot. Telefonos kézi/natív automata és óravezérelt edzésnél ugyanaz a védett inverz működik; óravezérlésnél tulajdonosi visszaigazolásig vár. Gyors/tervezett telefonos Chromium-próba nyolc méret/nyelv kombinációban, natív tranzakciós dupla kattintás és valódi órás távoli parancs/újraküldés regresszió védi. Az új funkcióhoz azonos forrású telefonos + Wear tesztcsomag kell. Verzió/main/release változatlan.

## P2 #188 megvalósítás — 2026. október 15.

A dokumentált P2-sorrend első feladata külön `feat/188-safe-wear-undo` ágon készül, a #208 utáni `66dbc02` mainből. Verzióemelés nincs (1.3.51 / 2732).

- Egy utolsó sorozatrögzítés: 8 másodperces közvetlen Visszavonás és későbbi edzésmenü-művelet.
- Előzetes automata-szünet, célzott sorozatinverz, pihenő lezárása és visszalépés a célgyakorlatra; egészségminták és valóban mért munka/pihenő/szünet időnapló megőrzése.
- Stabil eseményazonosító és teljes sorozatérték-ellenőrzés; újabb adatot nem ír felül. Telefonos natív háttérút és órás offline outbox is érintett. Régi időzítő-checkpoint és újraküldött rögzítés nem élesztheti újra az inverzt.
- Telefonvezérlésnél nyugtáig egyeztetés; órás offline folytatáskor a későbbi parancsok a visszavonás eredményére hivatkoznak. Ütköző későbbi helyi módosítást nem törlünk: az óra szüneteltetett konfliktust mutat, amely telefonos ellenőrzést igényel.
- Célzott JS/Kotlin/natív és 160–225 dp / nagy betűs kerek UI-regressziók; teljes gate és aláírt telefon/Wear tesztbuild GitHub Actionsben, eredmény a draft PR-on. Fizikai elfogadás és main-jóváhagyás még külön lépés.

A további dokumentált sorrend: #189 → #192 → #193; az órás térkép nem része ennek a változtatásnak.

**Legújabb Wear-visszajelzés:** a `3eb33ff` videóján az újrapróbálás továbbra sem indít mérést. A felhasználó megerősítette: **a központi helyszolgáltatás ki volt kapcsolva**, miközben a TrainPilot pontos helyengedélye megvolt; bekapcsolja és újrapróbálja, siker még nincs igazolva. Külön platformellenőrzés (`LocationManager.isLocationEnabled`), külön rendszerhely-üzenet és Helyadatok bekapcsolása gomb szükséges. A gomb a rendszer helybeállítására vezet, engedélyezés után az összesítést és a külön GPS-szándékot őrző retry következik, visszautasításkor nincs Settings-hurok. GitHubon JUnit + kerek Compose + valódi platformhely-ki/be instrumentált próba és aláírt Wear APK; telefon változatlan `0d1ecf1`, verzió 1.3.4 /2730, main/release nélkül. Az alábbi korábbi foreground-javítás önmagában nem igazolta a fizikai sikert.

**Átadási bizonyíték:** `35b4108` alkalmazásforrás, Wear gate sikeres, **102/102 kerek UI/platform teszt sikeres**. [Buildbizonylat és ellenőrzött APK/hash](../builds/2026-10-15-wear-system-location.json). A rendszerhely bekapcsolása utáni fizikai szenzorsiker még nincs visszaigazolva. Az alábbi korábbi állapotok történetiek.

**Új készülékes visszaigazolás:** a `0d1ecf1` Wear a megadott pontos, „használat közben” helyengedéllyel is `ACCESS_FINE_LOCATION` hibára áll. A `WearHealthService` foreground típusát javítjuk: meglévő pontos helyengedély mellett a `location` típus GPS nélkül is megmarad, míg az ExerciseConfig GPS-kapcsolója külön marad. A korábbi konkrét engedélyhibából tanult szükségletet az explicit újrapróbálás kezeli; megtagadott engedéllyel nincs hibás mérésindítás. A hibaüzenet felül jelenik meg, retry megőrzi a valódi összesítéseket. Új Wear JUnit/Compose és aláírt APK szükséges, telefonos kód változatlan; fizikai mérés még nem elfogadott. 1.3.4 /2730, main/release nélkül.

**Legfrissebb készülékes visszajelzés:** #163 térkép kezdetnek jó; órán `Missing permissions: ACCESS_FINE_LOCATION`. A #205 folytatásában helyengedély nélkül az órás pulzus/kalória/lépés szenzormérés engedélyezett általános profillal folytatható, kültéri GPS-táv/sebesség nélkül. Órás GPS bekapcsolásakor külön pontos helyengedély szükséges; elutasítás kikapcsolja a GPS-t. A korábbi konkrét helyengedélyhibás indítás újrapróbálható a saját edzés összesítéseinek megtartásával. Új JUnit, GitHub Wear gate és új aláírt telefon/Wear pár szükséges. A telefonos térkép piros X-et és opt-in, azonosítható OSM-hátteret kap; részletek az aktuális státuszban és GPS-dokumentumban. Verzió 1.3.4 /2730, Wear útvonalrögzítés továbbra is külön #193.

**Aktuális P1 – 2026-10-15:** a #199 mainre került; most #163 telefonos GPS / #205 draft, **8/8 release gate és készülékes elfogadás**. A `3af303b` gate-je a GPS-visszaállítási teszten elbukott; az aszinkron visszaállítás és route-takarítás javítása után a friss GitHub CI igazolása szükséges. Teljes teszt és aláírt APK-build a felhasználó kérésére Actionsön fut. Részletek: [aktuális státusz](../../DEVELOPMENT_STATUS.md), [GPS-tárolás](../gps/P1_163_ROUTE_STORAGE.md), #163 készülékes mátrix. **1.3.4 /2730**, Wear GPS továbbra is külön #193; fizikai elfogadás és térképcsempe-provider döntése még hiányzik. Az alábbi korábbi állapot történeti.

## Aktuális állapot — 2026. október 14.

A P0 #162/#157 készülékesen elfogadva és mainen (#195), az automata háttérvezérlés és időnapló elkészült. A #158 mérési forrásjelzés a #196-tal mainre került (`20fe5ce`), a felhasználó elfogadta, az Issue lezárva. A #159 Mini Napló meglévő megvalósítását ellenőriztük és a hibajegyet megvalósítottként lezártuk; nincs második órás történet vagy újraépítés.

Következő P1: **#191 → #163**. A #191 külön `feat/191-wear-save-sync-status` ágon megvalósítás és ellenőrzés alatt: helyi mentés, Data Layer-átadás és tartós telefonos nyugta külön státusszal, eredeti parancs újrapróbálásával. [Adatút, tesztek és átadás](P1_191_MENTES_SZINKRON.md). A #200 teszt közbeni pihenőkihagyási hiba is javítva a #199-ben. A `c503887` telefonos/Wear tesztpár átadva, teljes CI sikeres; készülékes elfogadás nincs. Verzió 1.3.4 /2730. Az alábbi október 13-i tesztpár és akkori nyitott feladatok történeti állapotot őriznek; az új munkában ez a felső állapot és a GitHub aktuális Issue-k az irányadók.

Frissítve: 2026. október 13. Állapot: aktuális main megvalósítás + következő lépések; a készülékes elfogadás külön jelölt.

Ez az egy fájl fogja össze a **telefonos és órás automata mód közös tervét**, valamint a Wear OS UI- és funkcióötleteket. Az automata mód mindkét alkalmazás fejlesztési feladata. A következő döntéseket és pontosításokat is itt vezetjük tovább.

A közös main-integráció [#179](https://github.com/fuloplevente1998/TrainPilot/pull/179); a felhasználó 2026. október 13-án kérte a main aktualizálását. Új beszélgetéshez: [CHATGPT_FOLYTATAS.md](../CHATGPT_FOLYTATAS.md).

Kapcsolódások: [kezdőlap #170](https://github.com/fuloplevente1998/TrainPilot/issues/170), [Wear UI előzmény #178](https://github.com/fuloplevente1998/TrainPilot/pull/178), [automata mód #162](https://github.com/fuloplevente1998/TrainPilot/issues/162), [telefonos automata előzmény #164](https://github.com/fuloplevente1998/TrainPilot/pull/164), [későbbi térkép #163](https://github.com/fuloplevente1998/TrainPilot/issues/163).

## 1. Kiinduló állapot

- A #178 Wear-kezdőlapja és a #164 telefonos automata módja a #179 közös main-integrációjának része; a két régi ág történeti előzmény, a további munka a friss mainből indul.
- Kész a telefonos ikonokat/témát követő kezdőlap, külön Beállítások, közvetlen edzéstörlés megerősítéssel, felfelé húzható menü, Mini napló tördelése és gyakorlatoldali kis `HH:mm` idő.
- Kész az első telefonos és órás automata tesztváltozat, a közös tulajdonosi modell és a natív háttéridőzítők. A telefonon indított edzés teljes háttérrögzítése és a fizikai elfogadás még feladat.
- Az eredeti 1.3.4 release APK-kat a main frissítése nem cseréli le; a legutóbbi tesztpár pontos forrásazonosítóval szerepel lent.
- A térkép a #163 folytatás az automata működés lezárása után. Órás útvonalszakaszok külön további fejlesztés.

### Első közös tesztváltozat — 2026. október 13.

A #179 main-integráció a legutóbbi telefonos `3e66ab0` és órás `e41f09f` buildre épül. A legutóbbi átadott automata tesztpár: telefon `1650906`, Wear `e3db5e2`; a telefonos forrás a két commit között változatlan. Megmarad a telefonos zoomjavítás, az alsó visszaszámláló, a témaszinkron és az órás kezdőlap; az új funkciók tesztváltozatban kerülnek melléjük.

- Órán indított tervezett/gyors edzés: az óra az időzítő tulajdonosa. Az órás alkalmazás háttérbe tétele és a telefonkapcsolat elvesztése sem állítja le a natív időzítést; Health Services-engedély nem kell hozzá.
- Ismétléses sorozatot a felhasználó rögzít. A beállított pihenő után a következő sorozat/gyakorlat aktív, az ismétlést nem találja ki az app. Pontosan `mp` egységű sorozatnál a munkaszakaszok és körök automatikusan futnak. Oldalankénti mérés továbbra is kézi, utána automatikus pihenővel.
- Kis A/M kapcsoló a gyakorlatoldal órája mellett; munka/pihenő/kör és rezgés az edzésmenüben. A munkaszakasz első indítása kézi. Szünet/folytatás és pihenőkihagyás elérhető, a végső edzésmentés továbbra is külön megerősített művelet.
- Az órás jelzés: 80 ms a 3–2–1 másodpercnél, majd 1 másodperces egybefüggő rezgés. Szünet/leállítás megszakítja; ismételt állapotfrissítés nem ismétli meg.
- Óra által vezérelt edzésnél a telefon csak tükrözi a számlálót, és az órának küldi a rögzítést/szünetet/folytatást/kihagyást. Kézi gyakorlatváltás leállítja az automatikát. Stabil sorozatazonosító, szakaszszám és eseményazonosító védi a régi/dupla parancsoktól.
- Telefonon indított edzésnél a telefon marad a tulajdonos. A natív telefonos óra közvetlenül frissíti az órás kijelzést, az órás szünet/folytatás/kihagyás a telefon natív szolgáltatását vezérli. Nem indul második órás időzítő vagy kettős rezgés.
- Megszakadt szolgáltatás/újraindítás után az utolsó mentett hátralévő időből szünetelt állapot áll vissza; nincs utólag kitalált sorozat. Nyitott szerkesztőből nem menthető véletlenül a közben aktívvá vált másik sorozatba.

**Telepítés:** mindkét új APK szükséges. Az első használat előtt meg kell nyitni a telefonos appot és frissíteni az órás adatokat; az új közös protokoll ezután helyben is elérhető. Verzió: 1.3.4 / 2730; a tesztpárt a forráscommit különbözteti meg.

**Még hátravan:** telefonon indított ismétléses sorozat órás rögzítése és egy új telefonos menetrend óráról indítása a telefonos webfelület háttérben alvásakor még a telefon visszatéréséig várhat. Az óra ilyenkor „Telefonra vár” állapotot jelez. Emiatt a három használati irány teljes készülékes elfogadása még nincs kész. Az órás automata időmérés most a tényleges munka másodperceit összegzi, a meglévő telefonos intervallum a korábbi munka+pihenő értéket tartja; az egységes naplóbontás külön folytatás, régi adatok átírása nélkül. Órás GPS-szakaszok, visszavonás, érintészár, forgatható szerkesztés és új rezgésminták továbbra is tervben vannak. A fizikai rezgés és OEM háttérkezelés készülékes próbát igényel.

Ellenőrzés sikeres: 137 telefonos regresszió, 90 Chromium-tesztcsomag, 16 új Kotlin állapot-/tükrözési teszt és 51 emulátoros Wear-próba, beleértve a valódi foreground-service működést háttérbe tett Activityvel és kis kerek felületet nagy betűvel. Mindkét aláírt APK forrása és tanúsítványa ellenőrzött. A futások a [fejlesztési állapotban](../../DEVELOPMENT_STATUS.md) szerepelnek; a fizikai jelzések és párosított próbák külön hátravannak.

### Készülékes visszajelzés: pihenőfelirat és kör — 2026. október 13.

A telefonon munka után a pihenőfelirat eltűnt a kompakt időzítő dekorátorában; az automata saját felirata most látható marad. Az órás kör egy percnél hosszabb automata pihenőnél sokáig teljes maradt, mert az edzés alap pihenőértékét használta. Javítás: a hátralévő időt az aktuális szakasz teljes idejével osztja, így 2 percnél másodpercenként 1/120 kör fogy. A telefonos kezdeti pihenő és az intervallumon belüli pihenő külön hossza is megmarad az adatátadásban. A #179 tartalmazza e javításokat és célzott regressziójukat; a korábban átadott 1650906/e3db5e2 referencia előttük készült.

## 2. Wear OS-javaslatok és sorrend

A felhasználó ezeket felvette a tervbe; a lent javasolt részletszabályok még tervezési alapok.

| Sorrend | Fejlesztés | Tervezett működés | Elfogadás fő szempontja |
| --- | --- | --- | --- |
| 1 | Automata mód és pihenőjelzések | Munka/pihenő/sorozat/gyakorlat léptetése a 3. fejezet szerint; 3–2–1 előjelzés és külön indulási rezgés | Az óra lezárva is helyesen vezet végig, pontosan egyszer vált |
| 2 | Utolsó rögzítés visszavonása | Rögzítés után rövid ideig közvetlen Visszavonás; később az edzésmenüből is visszaérhető | A téves sorozat és a hozzá tartozó automatikus léptetés együtt visszaállítható |
| 3 | Érintészár edzés közben | Külön bekapcsolható védelem izzadság és ruha okozta érintések ellen; jól látható zárjel, hosszú nyomásos feloldás | Egy véletlen érintés nem rögzít, töröl vagy léptet; a feloldás egyértelmű |
| 4 | Lünetta/korona a szerkesztőben | A súly és ismétlés forgatással állítható a támogatott órákon; a plusz–mínusz és Mentés/Mégse is használható | Csak a megnyitott szerkesztő értéke változik, elfordítás nem ment magától |
| 5 | Kapcsolat és mentés állapotának finomítása | A már létező kapcsolatjelzés mellett világos „Órán mentve” / „Szinkronra vár” visszajelzés, szükség esetén újrapróbálás | Kapcsolatvesztéskor is érthető, hol van az edzés; nincs elveszett vagy kettős mentés |

UI-alapelv: a gyakorlatnév, aktuális szakasz és elsődleges művelet maradjon jól olvasható. Az új jelzések ne tolják le a gombokat; a kör alakú kijelző széle és a nagyított rendszerbetű is a terv része. A színek és ikonok a telefonos TrainPilothoz igazodjanak.

## 3. Automata mód — eldöntött működés

**Felhasználói döntés, 2026. október 12.: súlyzós, ismétléses edzésnél a felhasználó rögzíti a sorozatot; utána a pihenő és a következő sorozat indítása automatikus.**

Ez a szabály a telefonos és az órás alkalmazásban is érvényes, tervezett és gyors edzésnél egyaránt.

### Három megcélzott használat

| Használat | Elvárt működés |
| --- | --- |
| Csak telefon, óra nélkül | Teljes automata mód: beállítás, rögzítés, pihenő, sorozat-/gyakorlatváltás, jelzések, szünet, folytatás és mentés a telefonon |
| Csak óra, telefonkapcsolat nélkül | A helyben elérhető edzéssel önálló órás automata működés; helyi megőrzés és későbbi szinkron |
| Telefon és óra együtt | Ugyanaz az edzésállapot mindkét felületen; rögzítés/szünet/folytatás bármelyikről, egyetlen irányított idővonallal |

### Telefonos automata mód — önálló fejlesztési rész

- Edzésindítás előtt bekapcsolható Automata mód, az edzés és a gyakorlat típusához illő beállításokkal. A meglévő munka-/pihenőidő és körszám használható, a választás elmenthető.
- Ismétléses edzésnél a telefonos Rögzítés indítja a pihenőt, a lejáró pihenő pedig a következő sorozatot vagy gyakorlatot. Az ismétlés és a súly tényleges adat marad.
- Időzített gyakorlatnál és futóintervallumnál a munka/pihenő automatikusan vált, a sorozat vagy kör állapota a megfelelő edzéshez kerül.
- A natív Android telefonos appban lezárt kijelző és háttérben levő app mellett is időzítsen, a beállított telefonos hang-/rezgésjelzéssel. Látható edzésértesítésből lehessen szüneteltetni, folytatni és leállítani.
- Óra nélkül is teljes értékűen használható legyen. A telefonos GPS-mérés együtt futhasson az intervallummal; helyengedély-hiány ne állítsa le az időzítőt.
- A telefonos piszkozat-visszaállítás, edzésváltás, kézi beavatkozás és végső mentés ugyanazokat az állapotvédelmeket kövesse, mint az órás működés.
- Böngészős használatnál a korábbi nyitott oldalas korlát érvényes; a lezárt kijelzős háttérműködés vállalása a natív Android alkalmazásra vonatkozik.

### Ismétléses / súlyzós edzés

1. A felhasználó elindítja az edzést, majd elvégzi a sorozatot.
2. Ellenőrzi vagy javítja a tényleges súlyt és ismétlést, és megnyomja a Rögzítés gombot.
3. Elindul a beállított pihenő. A kijelzőn a hátralévő idő és a következő sorozat/gyakorlat neve látszik.
4. A pihenő végén rezgés jelez, és újabb Start-koppintás nélkül a következő sorozat munkaszakasza lesz aktív.
5. Ha a gyakorlat összes sorozata elkészült, a következő gyakorlatra vált. A sorozat vége ott is a felhasználó tényleges rögzítése.
6. Az utolsó gyakorlat utolsó sorozata után az edzés összegzése következik; az edzés végső mentése tudatos művelet.

Az automatikus indulás készenléti/munkaszakaszt jelent. A pihenő lejárta nem rögzít magától ismétlést, súlyt vagy teljesített sorozatot. Az óra mozgásából történő automatikus ismétlésszámlálás külön, későbbi kutatási feladat lenne.

### Időre végzett gyakorlat

- Beállítható munkaidő, pihenő és ismétlődés/sorozatszám. Az aktív munkaidő lejáratakor a mért időt az adott sorozathoz rögzíti, majd pihenőre vált.
- A pihenő végén automatikusan indul a következő munkaszakasz. A gyakorlat utolsó sorozata után a következő gyakorlat következik.
- Ismétléses gyakorlatra érkezve az ismétléses szabály lép életbe: a sorozat végét a felhasználó rögzíti.
- Oldalankénti időzésnél a bal/jobb oldal sorrendjét és az esetleges oldalcserés pihenőt külön meg kell adni. Ez a korábbi #164-ben megállási pont volt; nem tekinthető kész támogatásnak.
- Nulla pihenőnél közvetlen átmenet legyen; az utolsó munkaszakasz után ne induljon felesleges záró pihenő.

### Futás / időalapú intervallum

Példa: **30 mp futás → 60 mp pihenő/lassú szakasz → következő kör**, nyolc körön át, újabb Start-koppintások nélkül.

- A munka, pihenő és körszám előre állítható. A bemelegítés és levezetés választható későbbi bővítés.
- A kijelzőn az aktuális szakasz, hátralévő idő, körszám és a következő szakasz röviden jelenik meg.
- GPS-szel együtt is használható; a helyadatok a megfelelő edzéshez és szakaszhoz kapcsolódnak.
- A korábbi #164 a pihenő távolságát nem adja a munkaszakasz távjához. A lassú regeneráló futás külön mérése és összegzése még tervezési döntés; ezt nem változtatjuk meg észrevétlenül.
- GPS-jelkimaradás vagy engedélyhiány ne állítsa le az önálló időzítést. A hiányzó táv és a hibás helyjel legyen látható.

## 4. Állapotok és kézi beavatkozás

```mermaid
stateDiagram-v2
    [*] --> Keszen
    Keszen --> Munka: Indítás
    Munka --> Piheno: Sorozat rögzítve vagy munkaidő lejárt
    Piheno --> Munka: Pihenő lejárt / Pihenő kihagyása
    Munka --> Szunet: Szünet
    Piheno --> Szunet: Szünet
    Szunet --> Munka: Folytatás munkából
    Szunet --> Piheno: Folytatás pihenőből
    Munka --> Osszegzes: Utolsó sorozat elkészült
    Munka --> Osszegzes: Befejezés megerősítve
    Piheno --> Osszegzes: Befejezés megerősítve
    Szunet --> Osszegzes: Befejezés megerősítve
    Osszegzes --> [*]: Mentés
```

- **Pihenő:** a terv része, magától lejár. **Szünet:** felhasználói megállítás; a hátralévő idő és az aktív sorozat megőrződik.
- A Szünet/Folytatás minden munkaszakaszban és pihenőben elérhető. A felhasználó a pihenőhöz időt adhat vagy kihagyhatja azt.
- Kézi gyakorlatléptetésnél a kihagyott sorozat legyen kihagyott; ne váljon teljesítetté.
- Automata mód kikapcsolásakor az aktuális adatok megmaradnak, a következő automatikus váltás leáll.
- Leállítás, végső mentés és törlés külön műveletek; a meglévő törlési megerősítés megmarad.
- Visszavonásnál az idővonal először szünetel, majd az utolsó rögzítés előtti sorozathoz tér vissza. Újabb sorozat módosítása után már szerkesztési folyamat kell. A visszavonás ne törölje az órán ténylegesen mért egészségmintákat.

## 5. Telefonos és órás kijelzés, jelzések

### Telefonos felület

A folyamatban levő edzésben legyen egyértelmű Automata mód állapot, aktuális gyakorlat/sorozat vagy kör és következő gyakorlat. **Felhasználói pontosítás: az aktív visszaszámláló kézi és automata módban is alul, az Előző és Következő gomb között marad; az Automata mód kapcsolója kis, visszafogott vezérlő, kisebb az Előző/Következő gomboknál.** A tényleges ismétlés/súly rögzítése, Szünet/Folytatás és pihenő módosítása közvetlenül elérhető. A befejezés és mentés külön művelet. A háttérben frissülő idő vagy szinkron ne csukja össze a megnyitott szerkesztőt vagy edzésrészletet. A színek a választott telefonos témát kövessék.

### Órás felület

Javasolt kijelzés: felül gyakorlatnév, alatta kis sorozat/kör adat és pontos idő; középen a fontos állapot vagy hátralévő munka/pihenő; mellette/alatta pulzus és kcal. Pihenőben a következő gyakorlat is legyen előre látható. Az elsődleges gomb a pillanatnyi állapot szerint Rögzítés, Szünet vagy Folytatás.

Javasolt rezgések, még készülékes próbára:

| Esemény | Javasolt jel |
| --- | --- |
| Visszaszámlálás utolsó 3 másodperce | Másodpercenként egy rövid, 80 ms-os impulzus |
| Szakasz lejár, új munka/sorozat indul | Egy 1 másodperces, egybefüggő rezgés |
| Következő gyakorlat | Két elkülönülő impulzus |
| Edzéssor vége | A fentiektől eltérő, rövid záró jel |

A hang és rezgés külön kapcsolható legyen. Telefonos önálló használatnál a telefon, órás önálló használatnál az óra jelez. Közös használatban egy átmenet csak egy jelzéscsomagot indítson; a telefon és óra együtt ne adjon véletlenül kettős jelzést. A tervezett prioritás az órás rezgés, elérhetetlenség esetén a telefonos beállítás szerinti visszajelzés. Felhasználó által elfogadott első minta: 3–2–1 rövid előjelzés, lejáratkor 1 másodperces, egybefüggő rezgés. A #179 telefonos és órás natív időzítője ezt az egységes mintát futtatja; az eseményenként eltérő órás minták integrációja még hátravan. Szünet és leállítás megszakítja a jelzést, ismételt háttérfrissítés nem ismétli meg. Az előjelzés külön ki-/bekapcsolhatósága későbbi finomítás.

## 6. Telefon–óra együttműködés és megszakítás

- Egy edzés idővonalának egyszerre egy irányítója legyen. A telefonon indított edzés kezdetben telefonos, az órán indított edzés órás irányítással fusson; a másik készülék vezérlő és megjelenítő.
- Parancsonként stabil edzés-/gyakorlat-/sorozatazonosító és eseményazonosító kell. Dupla koppintás, késve érkező adat vagy újraküldés nem rögzíthet még egy sorozatot.
- Kapcsolatvesztéskor az irányító eszköz folytathatja az időzítést. A másik jelezze az offline állapotot; ne induljon el két független automatikus léptetés. Irányítóváltás csak egyeztetett, helyreállítható átadással történhet.
- A szakaszhatárt eltelt idő alapján kell számolni, nem UI-frissítésekből. A lezárt kijelző ne állítsa le az időzítést vagy a rezgést.
- Ha a futtató szolgáltatás megszakad vagy az eszköz újraindul, helyreállítható szünetelt állapot legyen. A kimaradt időből ne keletkezzen utólag több késznek jelölt gyakorlat.
- A GPS és Health Services életciklusa az edzéshez kötődjön; képernyőváltás ne indítson új mérést. Szünet és folytatás a meglévő mérési szabályokkal összhangban működjön.
- A napló külön őrizze meg a munka, tervezett pihenő és felhasználói szünet időtartamát. A #164 régi munka+pihenő összesítésének kompatibilitását a megvalósítás előtt pontosítani kell; régi naplóadat nem írható át találgatással.

## 7. Megvalósítási lépések és elfogadás

0. **Mentés #180 — elfogadva:** a JSON-export és a teljes ZIP fotó-előellenőrzés javítása mainen. A felhasználó 2026. október 13-án visszaigazolta: „rendben van a mentés is”. A hibajegy lezárva; ez már nem blokkolja a következő fejlesztéseket. Külön kézi visszaállítási próbáról nincs visszajelzés.
1. **Készülékes tesztpár-ellenőrzés:** a pihenőjavításokat is tartalmazó aktuális main telefon/Wear pár (korábbi referencia: `1650906` + `e3db5e2`), mindkét APK frissítése és első adatszinkron. Külön telefonos, önálló órás és párosított indítás; lezárt kijelző, tényleges rezgés és újracsatlakozás. A #179 már egyesíti a Wear UI-t és a telefonos/órás első automata módot.
2. **Telefonos tulajdon háttérrögzítése:** az órás ismétlésrögzítés és egy új telefonos menetrend indítása az alvó webfelület visszatérésére várhat. Ezt natív, stabil sorozatazonosítóhoz kötött feldolgozással kell folytatni, a telefonos felület és a háttérszolgáltatás kettős léptetése nélkül. Ütközés, duplikált/késői parancs, folyamatújraindítás és draft-védelem a regresszió része.
3. **Munka/pihenő/szünet és GPS:** az órás munkaidő és a régi telefonos munka+pihenő összesítés egységes, forráshű naplóbontása; a régi adatok változatlanok. Oldalankénti automata menetrend és órás GPS-szakaszok külön bővítések, nem kész támogatás.
4. **Térkép #163:** az automata működés elfogadása után, elsőként a telefon GPS-ére építve. Útvonalpontok tárolása, megszakított szakaszok, offline nézet, mentés/visszaállítás/törlés és forráshű adatok szükségesek; órás útvonalrögzítés további hatókör.
5. **Kényelmi Wear-funkciók:** visszavonás, érintészár, lünetta/korona-szerkesztés és mentési/szinkronállapot finomítása, külön ellenőrizhető változtatásokban. A #170 kezdőlap első revampja kész; további UI-hibák készülékes visszajelzés alapján. Ezek tervezett bővítések; nem feltételei a #163 telefonos térkép elkezdésének.


Elfogadási próbák:

- A súlyzós és időzített/futó próbák mindegyike külön telefonon, óra nélkül; külön órán, kapcsolat nélkül; és párosított telefon–óra használatban is teljesül.
- Telefonos automata beállítások és vezérlők kis kijelzőn, Basic/Vivid témákban és a támogatott nyelveken; Android értesítési vezérlés és háttéridőzítés fizikai telefonon.
- Súlyzós 3 sorozat: minden sorozatot a felhasználó rögzít, pihenő végén automatikus indulás, végül helyes gyakorlatváltás.
- Időzített 30/60-as futás 8 körön át, nulla pihenővel is; az utolsó kör után helyes befejezés.
- Szünet munka és pihenő közben; folytatáskor ugyanaz a gyakorlat és helyes hátralévő idő.
- Pihenő hozzáadása/kihagyása, kézi léptetés, visszavonás; nincs kettős rögzítés.
- Lezárt telefon és óra, kijelző felébresztése, háttérből visszatérés, szolgáltatás megszakítása.
- Kapcsolatvesztés és újracsatlakozás mindkét indítási iránynál; az elmentett sorozatok és mérések egyeznek.
- GPS és időzítő együtt, jelkimaradás és engedélyhiány; helyes munka/pihenő hozzárendelés.
- Rezgések fizikai órán: érthetők, elkülönülnek, nincsenek duplázva.
- Kis kerek kijelző és nagyított betű; minden fontos gomb elérhető, a feliratok olvashatók.

## 8. Következő tervezési döntések

- Legyen-e külön felkészülési idő a következő gyakorlat előtt, és mi legyen az alapértéke?
- A rezgések közül melyek legyenek alapból bekapcsolva; kérünk-e hangot is?
- Futásnál a regeneráló szakasz külön munkatípus legyen-e, saját távval és tempóval?
- Melyik oldalcserés menetrend legyen támogatott az első automata változatban?
- Az érintészár kézi kapcsoló legyen-e első körben, vagy automatikusan is bekapcsolható induláskor?

Ezek nyitott tervezési kérdések. A súlyzós sorozat kézi rögzítése és a pihenő utáni automatikus indulás már eldöntött követelmény.

## 9. Samsung energiapontszám és Coach — külön telefonos tervezési feladat

A [#184](https://github.com/fuloplevente1998/TrainPilot/issues/184) a Samsung tényleges napi energiapontszámának olvasását és a Coach közös edzésajánlását tervezi. A repó Samsung SDK 1.1.0 csomagja támogatja az `ENERGY_SCORE` típust, de az olvasás még nincs bekötve. Javaslat: változatlan Samsung-pontszám mint regenerációs kiindulás, ehhez TrainPilot gyakorlatspecifikus progresszió/terhelés/fájdalom; az alvás/HRV/aktivitás ne számítson kétszer. Előbb forráshű összehasonlító teszt, majd a közös ajánlás szabályai. Részletes kutatás, SDK-hozzáférési feltételek és megvalósítási terv: [WORK_FEJLESZTESI_UTMUTATO.md](../WORK_FEJLESZTESI_UTMUTATO.md). A #162 és #163 sorrendje e kutatástól nem változik; a konkrét következő fejlesztést a felhasználó aktuális kérése határozza meg.
