# TrainPilot – teljes natív dizájn- és funkcióátadó

Frissítve: 2026-10-24. A felhasználó külön kérte a korábbi négy mockup
újratervezését, a rögzített működési pontosításokkal. Az új táblák review-változatok;
a vizuális irány elfogadott, az új képek külön felhasználói áttekintésre várnak.
A képek generált látványtervek, a végső átadás futó emulátorból készített képeket igényel.

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
