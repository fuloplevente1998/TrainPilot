# TrainPilot WebView – referenciaválasztás: 4 eredeti mockup + 18 Drive-kép

**Dátum:** 2026-10-28 · **Állapot:** előzetes vizuális döntési jegyzék; implementáció NINCS.

Ez a [WebView mockup-redesign mesterterv](../plans/WEBVIEW_MOCKUP_REDESIGN_2026-10-28.md) vizuális melléklete. A bejegyzések alapja a korábban megnyitott/auditált [18 Drive-kép indexe](https://github.com/fuloplevente1998/TrainPilot/blob/88828ba84e15f5f5fbf4fea9384e8e1bad936167/docs/design/STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.md) és [Drive audit](https://github.com/fuloplevente1998/TrainPilot/blob/88828ba84e15f5f5fbf4fea9384e8e1bad936167/docs/reviews/STUDIO_DRIVE_01_18_AUDIT_2026-10-28.md); **ez nem új, WebView-ben igazolt screenshot-összehasonlítás**. A Drive JPEG-ek privátok, nem publikáljuk őket. A [4 eredeti mockup](https://github.com/fuloplevente1998/TrainPilot/tree/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups) adja a képernyőkompozíciót és 24 nézetet; a main a tartalmi/funkcionális minimum.

## 1. Választási hierarchia

1. **Működés: main** – minden eredeti adat, mérés, tárolás, interakció, útvonal, Wear/Health/GPS/Google-szabály.
2. **Képernyőkompozíció: 4 eredeti tábla** – grafit/arany vizuális világ, **öt ALUL elhelyezett főfül**, főoldalak és részletnézetek hierarchiája.
3. **Vizuális finomítás: 18 Drive-kép** – kompakt fejléc, kártyaarányok, ikonok, szekciók, lenyitható panelek, rendezettebb műveletek, jobb tartalomsűrűség ott, ahol a main-működéssel és a kiválasztott mockuppal összefér.
4. Ha ütköznek: az eredeti funkcióvédelmet és az öt alsó főfüles döntést nem írjuk felül hallgatólagosan. Új navigációs vagy adatlogikai döntés külön rögzítendő.

## 2. Drive 01–18: javasolt vizuális átvétel, nem automatikus import

| Drive | Eredeti megjelenítés | Amit érdemes átvenni a WebView-ba | Korlát / vizsgálat |
|---|---|---|---|
| **01** | Home felső része | Kompakt tervező/mai edzés/gyorsindítás/óra kártyák hierarchiája, valós folytatás kiemelése | Mockup 01 elsődleges, nincs hardcoded edzés |
| **02** | Home alsó része | 2×2 állapotkártya, Programok/Naptár/Coach gyorsműveletek, ajánlat vizuális rendje | A main összes fontos mérésére és aktív státuszára kattintható valódi út |
| **03** | Edzés gyorsindító bottom sheet | Jól elkülönített indítási módok és kompakt alulról nyíló választó | Csak ha az eredeti quick-start callback/védelmek megtarthatók; nem második motor |
| **04** | Gyakorlatkönyvtár | Keresősáv, izomcsoport-chipek, képes gyakorlatsorok, ismertető/videóhierarchia | Valós katalógus, kedvencek, offline működés; nem licenc nélküli kép |
| **05** | Kültéri GPS | Táv/idő/pulzus kártyaritmus, valós térkép, vezérlők elkülönítése | Csak tényleges route és friss mért adat; GPS-stop szemantikája változatlan |
| **06** | Napló hónap/nap választó | Jól olvasható, fókuszált dátumválasztó stílus | Fennmaradnak a main szűrők és az eredeti dátum/naplóindex |
| **07** | Fejlődés | Volumenes főpanel, belső lapok, négy mutató, anatómiai elem elhelyezése | Mutatók csak valóban kiszámolt, helyesen címkézett main-adatokból |
| **08** | Statisztika/PR/kardió | Letisztult statisztika-alfülek és őszinte üres állapot | Valódi PR és kardió részleteket nem rejtünk el, ha vannak |
| **09** | Erő/1RM | Gyakorlatválasztó, görbe, rekord/best-set kártyák | Becsült 1RM-et nem nevezzük hiteles PR-nak |
| **10** | Egészség áttekintő | Sűrű 3×2 napi mérésrács, pulzus/testsúly sorok | A mockup hat száma nem teljes main-metrikakészlet; helyes forrás/nap |
| **11** | Egészség alsó rész | Wear/Health kapcsolatok, szinkron/engedélyek hierarchiája | Kapcsolat és jogosultság csak hiteles visszajelzésből |
| **12** | Testsúly kibontva | Inline trend, változás, új mérés és időszakválasztó | Valódi mentés, testsúly és fitness-metrikák elérhetősége |
| **13** | További egészségadatok | Kompakt 2×3 metrikacsoport első szintje | Az ezen túli teljes main-metrikakészlet továbbra elérhető |
| **14** | Egészségnapló | Dátumhoz kötött részletsorok és lenyitás | Meglévő main napló és provider-szűrők, dátum/forrás-idő |
| **15** | Kapcsolt eszközök | Különálló Wear/Health Connect/Samsung Health kezelési kártyák | Nincs hamis csatlakozás/akkuszázalék; BLE-eszközök adatai nem törölhetők |
| **16** | Beállítások felső rész | Profil, téma, nyelv, stopper, hang és progresszió rendezett szekciói | Main beállítások tartós megőrzése, HU/EN/DE/RO |
| **17** | Beállítások alsó rész | Eszközök, mentés, engedély, privát fotó szekciók | Nem gyártunk nem létező backup-listát vagy siker-visszajelzést |
| **18** | Fejléc nyelvválasztó | Kompakt, világos nyelvválasztó, valós opciókkal | Csak a main tartós nyelvi beállítását használja; mobil fejlécben el kell férnie |

**Státusz:** a fenti elemek **átvételi jelöltek**, nem elfogadott, kódba beépített vagy pixelpontos egyezések. P0-ban mindegyikre meg kell nevezni a konkrét `main` DOM/CSS/JS route-ot és visszatesztet. A Drive 18 kép **nem** kötelező minden megjelenített demóelem felhasználása.

## 3. Ütközések és elutasított automatikus átvételek

| Téma | Eltérés | Döntés |
|---|---|---|
| Navigáció | 4 mockup: 5 alsó fül; 18 Drive: 5 felső fül | **Alul marad az 5 főfül**. A felső Studio-füleket nem duplikáljuk; fejléc és finomítás átvehető |
| Egészség/Fejlődés | Drive kevés, szép kártya vs. main részletes mérések | A teljes main-tartalom megmarad; a kártyák csak új összefoglaló/szekció-nézetek |
| Szín | Referencia arany témára készült | A mentett main-paletták mind maradnak; a grafit/arany csak egy bemutató |
| Studio-demó | 0 bpm, 0 kg, 87 pont, Watch-modell, „csatlakoztatva”, példa-GPS | **Nem** éles adat vagy „siker”; igazi érték vagy „nincs adat”, illetve valódi jogosultság/kapcsolati státusz |
| Fejlécek és menük | Studio felső márkafejléc, mockup alsó nav, main jelenlegi menü | Egyetlen konzisztens alkalmazás-keret; nincs kétszeres fix navigáció/overlap |
| „Mentés vázlatként” | Képen egyszerű akció | Csak valós félbehagyás és folytatható session, lezárástól és elvetéstől elkülönítve |
| Gyorsindítás | Studio alsó választó vs. main tényleges oldalak | A tálca csak prezentációs belépő, a meglévő main végrehajtási utakhoz |
| Anatómia, brand | Studio eltérő ikon/ábra lehet | Eredeti TrainPilot arany súlyzó és eredeti izomtérkép megmarad |
| Működés | Studio kódban demo/fake write/ack előfordulhat | Nincs Studio ViewModel/állapotmotor import. A Drive-képek vizuális források |

## 4. Mockup-tábla → Drive képek → main oldalcsoport

| Eredeti tábla / rész | Bevonható Drive-képek | Main tartalom- és eseménygazda |
|---|---|---|
| 01 – Kezdőlap, Edzés, Programok, Naptár, könyvtár | 01–04, 18 | Home, aktuális edzés/draft, quick-start, programnapi akciók, Naptár/Coach, gyakorlattár |
| 02 – súlyzós/idő/GPS/intervallum/lezárás | 03, 05 | Aktív edzés, sorozat, rest/interval, GPS, undo, finish/discard |
| 03 – Napló, térkép, Fejlődés, Egészség, Coach | 06–15 | Journal, Progress/PR, anatómia, összes Health metrika, részletes Coach |
| 04 – Beállítások, Wear/sync, backup, engedély, fotó, editor | 15–18 | Settings, Android pluginok, fájl/Drive, képkezelés és program-szerkesztés |

## 5. P0-val létrehozandó tényleges UI-paritási leltár

Minden sor állapota alapból **FELTÁRÁSRA VÁR**, amíg a `main` valódi végleges DOM-ját, JavaScript callbackjét és CSS cascade-jét nem vizsgáljuk:

| Azonosító | Komponens/nézet | Kötelező bizonyítás |
|---|---|---|
| W01 | Öt alsó főfül | aktuális main route-hivatkozás, back/Settings/session/fix footer működése |
| W02 | Kompakt fejléc | valódi súlyzóasset, nyelv/téma mentés, fogaskerék, safe-area |
| W03 | Aktív Home hero | main session/draft/ajánlott programnap, valódi CTA, újranyitás |
| W04 | Edzés belépő és quick-start | nem törlődő kereső/program/naptár/Coach, indítás- és state-védelem |
| W05 | Sorozat, stopper, GPS | megszokott main motor, undo, explicit stop, Wear ownership |
| W06 | Journal, Progress | történeti lista, PR, idősor, relatív izomterhelés, fotó és térkép |
| W07 | Health, Coach | minden main adat/forrás/frissesség/engedély és napló-részlet |
| W08 | Settings, backup, sync | minden main mentett beállítás és valós Android/Google-funkció |
| W09 | 4 nyelv és témák | main tényleges palettaszáma, teljes lokalizáció, kapcsoló és tárolás |
| W10 | Teljesítmény és hozzáférhetőség | 320/360/393/412 CSS px, nagy betű, 0/200/1500 edzés, DOM/perf, éles érintés |

A leltárt fejlesztés közben bővíteni kell `main forrásút/funkció` + `új UI elem` + `teszt` + `képi eltérés` + `státusz` mezőkkel. Nem készre pipálható pusztán a képen szereplő elem.

## 6. Első megvalósítás vizuális vázlata

- **Fejléc:** eredeti arany súlyzó, márkanév, kompakt valódi beállításműveletek.
- **Home görgethető tartalom:** valódi folytatás/mai javaslat (erős hero), gyorsindítás, óraállapot, összefoglaló metrikák, Programok/Naptár/Coach; minden kártya a main-hez kötve.
- **Alsó 5 fül:** Kezdőlap – Edzés – Napló – Fejlődés – Egészség; aktív route megbízható kijelzése, system safe-area, nem takarja el az aktív stoppert/panelt.
- **Kompakt, de nem hiányos:** a megmaradó részletek lenyitással, belső választóval vagy külön jelenlegi route-on érhetők el.
- **Kontraszt és valós állapot:** nincs csak színnel jelzett siker/edzésállapot; a mintaképhez hasonlítás mindig valódi screenshotra és működő route-ra vonatkozik.

**Külön elvégzendő feladat:** a referenciafotók tényleges vizuális újraellenőrzése képpárokon az első P1 webes prototípus után. Most az auditált forrásból tervet készítettünk, **nem** jelentünk kész vizuális megvalósítást.
