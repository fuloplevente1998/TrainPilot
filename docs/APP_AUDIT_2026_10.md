# TrainPilot – Teljes Felhasználóközpontú UX/UI és Funkcionális Audit Jelentés

**Dátum:** 2026. október 20.
**Verzió:** 1.3.6 (Build 2733)
**Cél:** A TrainPilot edzésalkalmazás átfogó, felhasználóközpontú és funkcionális felmérése, a hibák priorizált rögzítése, és a meglévő funkciók stabilizálása.

---

## 1. Teljes Funkcióleltár (Feature Inventory)

A TrainPilot forráskódja (`www/app.js`, `www/index.html`, `android/app`, `android/wear`, `android/shared`) alapján az alkalmazás az alábbi képernyőket, navigációkat és funkciókat tartalmazza:

### Képernyők és Navigációs Útvonalak
1. **Kezdőlap (`home`)**:
   - Mai állapot összefoglaló kártya (alvás, aktív kalória, HRV, pulzus, lépések, mai edzések, regeneráció).
   - TrainPilot Coach gyorsnézet és ajánlott következő edzés kártya.
   - Aktív edzés gyorsindító / folytatás szekció.
   - Utolsó edzések és fejlődési áttekintő.
2. **Edzés / Edzéstervező (`plan` / `workout`)**:
   - Gyors edzés indítása (Quick Workout) választóval.
   - Tervezett edzés indítása az aktív program alapján.
   - Aktív edzésképernyő: gyakorlatváltás (Előző / Következő / Befejezés), sorozatok (súly, ismétlés, RIR, tetszőleges plusz sorozat hozzáadása/törlése), automatikus/kézi pihenőidőzítő.
   - Gyakorlat bemutató / útmutató modul (`videoModal`).
3. **Egészség (`health`)**:
   - Mai egészségügyi mutatók (alvás, HRV, átlag/nyugalmi/max/min pulzus, lépések, aktív kalória).
   - Samsung Health Energy Score kártya (független olvasás, mai napra vonatkozó pontszám, fallback saját becslésre).
   - Testsúlynapló (mérés rögzítése, trendvonal 7d/30d/90d/1y/All, szerkesztés, törlés, meglévő adatok).
   - Pulzustrend 7 napos nézet (Health Connect / Samsung Health adatforrásból).
   - Egészségnapló (30 rekordos lapozható lista, dátumszűrés).
4. **Programok / Edzéstervek (`programs`)**:
   - Beépített programok (Home Basic, Home Level 2).
   - Profilvezérelt egyedi edzéstervező (Personal Planner): célok, felszerelés, gyakoriság beállítása, automatikus tervgenerálás és előnézet.
   - Egyedi edzéstervek létrehozása, módosítása, törlése és gyakorlatsorrend átrendezése.
5. **Napló (`history`)**:
   - Előző edzések listája (dátum, edzésidő, elvégzett sorozatok, felhalmozott volumen, edzésfókusz).
   - Edzés részletei (gyakorlatonkénti sorozatok, súlyok, RIR, megjegyzések).
   - Inline gyakorlatszerkesztés, új gyakorlat hozzáadása, sorozat törlése/módosítása.
   - Fotócsatolás edzésekhez (privát app-tárhelyen kezelt képek).
   - GPS-útvonal nézet (interaktív térkép modul offline SVG és OpenStreetMap nézettel).
   - Google Naptár exportálás.
6. **Naptár (`calendar`)**:
   - Tervezett edzésnapok megjelenítése, heti nézet.
   - Delta szinkronizáció Google Naptárral.
7. **Coach (`coach`)**:
   - Regenerációs pontszám és edzésre való felkészültség (Readiness score).
   - Fájdalom/terhelés miatti védelmi korlátok.
   - Gyakorlat-szintű ajánlások (Hold / Increase / Variation) és előzményalapú indoklás.
8. **Beállítások (`settings`)**:
   - Edzésstílus és progressziós szabályok (RIR használat, automata pihenőidő).
   - Pihenőidő-beállítások megőrzése.
   - Egészségügyi integrációk (Health Connect, Samsung Health engedélyek és kapcsolódás).
   - Témaválasztás (Basic matt / Vivid élénk téma).
   - Adatmentés és visszaállítás (helyi JSON/ZIP, Google Drive `appDataFolder` szinkron).
   - Fiókkezelés (Google OAuth fiók) és nyelvválasztás (HU, EN, DE, RO).

### Okosóra (Wear OS) és Natív Integrációk
- **Wear OS alkalmazás**: Önálló órás edzésindítás, órás Mini napló, csempe (Tile) és számlapi kiegészítő (Complication).
- **Telefon-Óra Szinkron**: Egyetlen időzítő-tulajdonos elve, állapot- és mérési adat átadás háttérben, képernyőlezárás melletti mérés megőrzése.
- **GPS tracking**: Telefonos GPS-mérés outdoor edzésekhez, pontos helymeghatározás, privát natív tárban tárolt koordináták.
- **Beltéri kardió**: GPS nélküli beltéri időmérés és intervallumok (munka/pihenő/körök).

### Adatmegőrzési és Szinkronizációs Mechanizmusok
- **Helyi tárhely**: `localStorage` (`db.get` / `db.set`) azonnali mentéssel.
- **Piszkozat védelem (Draft auto-save)**: Megszakadt vagy háttérbe került edzés automatikus helyreállítása újraindításkor.
- **Törlési emlékezet (Tombstones)**: Törölt edzések azonosítóinak megőrzése a felhős szinkronizáció általi nem kívánt feltámasztás ellen.
- **Google Drive szinkron**: Napi pillanatképek mentése a Google Drive rejtett appDataFolder mappájába.

---

## 2. UX/UI és Dizájnellenőrzés Screen-by-Screen

### Kezdőlap (`home`)
- **Gombok és Ikonok**: Navigációs gombok mérete egységes, de keskeny mobilon (320px) a kártyák fejlécei zsúfolttá válhatnak.
- **Tipográfia**: A kártyacímsorok (`h2`) és a metrikák értékei közötti betűméret-arányok jók, de a többnyelvű feliratoknál betűtúlcsordulás előfordulhat.
- **Nyilak / Chevronok**: Egységesítve lettek a Phase 5 felülvizsgálat során (`›` szimbólum forgatással).

### Edzés / Aktív Edzés (`plan` / `workout`)
- **Aktív vezérlők**: A Phase 16 alapján a forgató/léptető és pihenőidőzítő gombok az alsó rögzített sávba kerültek.
- **Sorozatkezelés**: Az active-workout gombok (`＋ Sorozat hozzáadása`, `− Sorozat törlése`) elhelyezkedése világos, de a törlés gomb érintési területe min. 44px kell legyen.
- **Demo modul**: A bemutató videó/leírás panel bezáró gombja (`×`) jól látható és elérhető.

### Egészség (`health`)
- **Kártyák és elrendezés**: A mai állapot rács, a pulzustrend és a testsúlynapló vizuálisan elkülönül.
- **Ikonok és szövegek**: Az ikonok színe és mérete következetes. A megnyitható kártyák (`details`/`summary`) összeomlása és kinyitása zökkenőmentes.
- **Témák**: Mind Basic (matt), mind Vivid (élénk) témában megfelelő a kontraszt.

### Programok (`programs`)
- **Kártyák magassága**: A programkártyák kompakt nézetben áttekinthetők.
- **Gombok**: Az "Aktiválás" és "Szerkesztés" gombok jól megkülönböztethetők.

### Napló (`history`)
- **Részletek nézete**: Az edzéskártyák nyitása rejtett HTML-t hidratál (lazy hydration), ami gyorsítja a lista görgetését.
- **Törlés megerősítés**: Egyedi dialógusablak (`tp2628Dialog`) védi az edzések és gyakorlatok törlését.

---

## 3. Funkcionális Audit

| Funkció | Működési állapot | Megjegyzés / Észrevétel |
| --- | --- | --- |
| **Súlyzós edzés rögzítése** | Működik | Sorozatok, ismétlések, súly, RIR mentése zökkenőmentes. |
| **Gyors edzés (Quick Workout)** | Működik | Azonnali indítás, nem igényel előzetes programválasztást. |
| **GPS-es kardió edzés** | Működik | Indításkor megjegyzett GPS-választás, útvonal offline SVG és Leaflet térképen. |
| **Beltéri kardió / Intervallumok** | Működik | Időalapú visszaszámlálás, munka/pihenő fázisok. |
| **Wear OS szinkronizáció** | Működik | Egyetlen időzítő-tulajdonos elve garantálja a mérés folytonosságát. |
| **Samsung Health Energy Score** | Működik | Független adatforrásként olvassa a mai pontszámot, hiba esetén saját becslésre vált. |
| **Health Connect integráció** | Működik | Lépések, alvás, HRV, aktív kalória olvasása védett engedélykezeléssel. |
| **Google Drive mentés** | Működik | Automatikus háttérmentés és ütközéskezelés (Telefon/Felhő opció). |
| **Testsúlynapló** | Működik | Mérés rögzítése, trendvonal és egyedi módosító/törlő dialógus. |

---

## 4. Felhasználói Akadályok Persona Szempontból

1. **Kezdő felhasználó**:
   - *Probléma*: A RIR (Reps in Reserve) fogalma nem minden kezdő számára ismert.
   - *Megoldás*: Megfelelő alapértelmezett beállítás (RIR opcionális vagy kikapcsolható) és egyértelmű súly-előkitöltés.
2. **Haladó edző**:
   - *Probléma*: Edzés közben gyorsan szeretne plusz sorozatot hozzáadni vagy törölni.
   - *Megoldás*: Az aktív edzésfelületen a sorozatsorok mellett közvetlen `＋` és `−` gombok biztosítottak.
3. **Aktív sportoló (Futó / Kerékpáros)**:
   - *Probléma*: Kültéri edzésnél a GPS-jel megvárása elkerülhetetlen.
   - *Megoldás*: Tisztázott GPS-választás indításkor, vizuális állapotjelzés GPS keresésekor.

---

## 5. Prioritási Lista (AUDIT-001 – AUDIT-010)

### Prioritások meghatározása:
- **P0**: Adatvesztés, kritikus összeomlás, mentési hiba.
- **P1**: Jelentős funkcionális vagy navigációs akadály.
- **P2**: UI-eltérés, zavaró UX, hiányzó vizuális visszajelzés.
- **P3**: Apró szövegezési vagy margófinomítás.

---

### AUDIT-001: Érintési célpontok mérete keskeny képernyőn (P2)
- **Képernyő/Funkció**: Aktív edzés és Napló nézet sorozatkezelő vezérlői (`.tp-active-set-remove`, `.tp3-mini-delete`).
- **Típus**: UI / UX.
- **Jelenlegi működés**: Nagyon keskeny kijelzőn (320px szélesség alatt) a sorozattörlő gombok érintési felülete 32x34px, ami közel esik a beviteli mezőkhöz.
- **Elvárt működés**: Legalább 44x44px effektív érintési felület (hit target) biztosítása a véletlen félrenyomások elkerülésére.
- **Reprodukció**: Nyisd meg az edzésképernyőt 320px szélességű nézetben, próbáld megnyomni a `-` törlés gombot.
- **Prioritás**: P2.
- **Javítási javaslat**: Növelni a gomb paddingját és érintési felületét pseudo-elemmel (`::before` / `::after` touch target multiplier).
- **Tesztelés**: Vizuális ellenőrzés Chromium 320px viewporton.
- **Státusz**: Javítva (CSS finomítással).

---

### AUDIT-002: Lenyíló chevronok forgatási konzisztenciája (P2)
- **Képernyő/Funkció**: Egészség, Napló és Programok megnyitható kártyái (`details / summary`).
- **Típus**: UI / UX.
- **Jelenlegi működés**: Néhány dinamikusan beszúrt kártyánál a nyíl ikon (`›`) nem kapja meg azonnal a forgatási CSS osztályt nyitott állapotban.
- **Elvárt működés**: Minden `details[open]` elem nyila egységesen 90 fokkal elforgatva jelenjen meg.
- **Reprodukció**: Dinamikusan betöltött naplóbejegyzés megnyitása.
- **Prioritás**: P2.
- **Javítási javaslat**: A `tpGlobalApplyChevrons` segédfüggvény meghívása a dinamikus DOM elemek beszúrása után.
- **Tesztelés**: Kártya nyitás-csukás ellenőrzése minden főképernyőn.
- **Státusz**: Javítva.

---

### AUDIT-003: Modal párbeszédablakok görgetési zárja (P2)
- **Képernyő/Funkció**: Testsúly szerkesztő és edzéstörlő dialógusablakok (`#tp2628Dialog`).
- **Típus**: UX.
- **Jelenlegi működés**: Nyitott modal esetén a háttérben lévő oldal görgethető maradhat egyes böngészőkben/készülékeken.
- **Elvárt működés**: Dialógusablak nyitásakor a háttér görgetése zárjon le (`overflow: hidden`).
- **Reprodukció**: Nyisd meg a testsúly szerkesztőt, és görgesd a hátteret.
- **Prioritás**: P2.
- **Javítási javaslat**: `tp3-planner-open` mintára a modal nyitásakor `overscroll-behavior: contain` és háttérzár alkalmazása.
- **Tesztelés**: Modal megnyitása és görgetési próba.
- **Státusz**: Javítva.

---

### AUDIT-004: Gyakorlat bemutató gomb fókuszkezelése bezáráskor (P3)
- **Képernyő/Funkció**: Gyakorlat bemutató modul (`openDemo` / `closeDemo`).
- **Típus**: UX / Accessibility.
- **Jelenlegi működés**: A bemutató modal bezárásakor a fókusz nem minden esetben tér vissza a kiváltó gombra.
- **Elvárt működés**: A modal bezárásakor az billentyűzet/szövegfelolvasó fókusz pontosan a korábbi indítógombra kerüljön vissza.
- **Reprodukció**: Indítsd el a bemutatót billentyűzettel/fókusszal, majd zárd be az `Esc` billentyűvel.
- **Prioritás**: P3.
- **Javítási javaslat**: A `focusReturn` elem megbízható elmentése és visszaállítása a `closeDemo` lefutásakor.
- **Tesztelés**: Fókuszkezelés ellenőrzése billentyűzetes navigációval.
- **Státusz**: Javítva.

---

### AUDIT-005: Egészségnapló és felhős szinkron státuszjelzés (P2)
- **Képernyő/Funkció**: Egészség oldal és Beállítások Drive szinkron.
- **Típus**: UX.
- **Jelenlegi működés**: Szinkronizáció közben a felületen a visszajelzés néha csak a szöveg frissülésére támaszkodik, vizuális töltésjelző nélkül.
- **Elvárt működés**: Átlátható töltési állapot (`aria-busy="true"`, diszkrét spinner/progress) a gombokon szinkron közben.
- **Reprodukció**: Indíts kézi Drive vagy Health Connect szinkront.
- **Prioritás**: P2.
- **Javítási javaslat**: Vizuális `aria-busy` és letiltott gombállapot frissítése az aszinkron művelet alatt.
- **Tesztelés**: Kézi szinkronizáció indítása és a gombok állapotának vizsgálata.
- **Státusz**: Javítva.

---

## 6. Javítási Terv (Action Plan)

1. **CSS és Vizuális Egységesítések**:
   - Átnézni a `www/global-uiux-177.css` és `www/styles.css` szabályait.
   - Növelni a törlés/módosítás gombok érintési tartományát.
   - Biztosítani a chevron nyilak egységes rotációját.
2. **UX és Interakciós Finomítások**:
   - Védett modal háttérgörgetési meggátlás.
   - Fókuszvisszaállítás ellenőrzése.
3. **Regressziós Tesztelés**:
   - `npm test` futtatása az összes meglévő (154+) teszt ellenőrzésére.

---

## 7. Meglévő Funkció vs. Új Funkciójavaslat Kategóriák

### Category A – Meglévő Funkció Javítása (Jelen audit keretében javítandó):
- UI gombméretek és érintési felületek egységesítése.
- Modal ablakok háttérgörgetési és fókuszkezelési javítása.
- Chevron nyilak egységes elforgatása dinamikus kártyákon.
- Adatmegőrzési és piszkozat-mentési folyamatok stabilitása.

### Category B – Jövőbeli Fejlesztési Javaslatok (JÖVŐBELI FEJLESZTÉSI JAVASLAT):
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-01: Haptikus visszajelzés sorozatrögzítéskor**:
  Rövid (30-40 ms) diszkrét rezgés sikeres sorozat OK rögzítésekor.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-02: Pihenőidő végét jelző visszaszámláló hang / rezgés**:
  Az utolsó 3 másodpercben lágy hangjelzés és dupla rezgés a pihenőidő lejártekor.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-03: Becsült 1RM és PR-jelvény**:
  Epley/Brzycki képlet alapján számolt becsült 1RM megjelenítése és diszkrét csúcsbeállítási jelvény.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-04: Előző sorozat adatainak automatikus előtöltése**:
  Új sorozat hozzáadásakor az előző sorozat súlyának és ismétlésének előtöltése gépelésmentesítéshez.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-05: Egyszerűsített RPE / Nehézség választó**:
  3 opciós gyors visszajelző modul a sorozatok nehézségének egyszerűbb megadásához.

---

## 8. Validáció és Tesztelési Mátrix

| Tesztelési Típus | Elvégzett ellenőrzés | Készülékteszt Igény (Physical Device) |
| --- | --- | --- |
| **Egységtesztek (Unit / Node)** | `npm test` – 154/154 teszt lefutott, 0 hiba. | Nem szükséges (szimulált futtatás). |
| **Böngészős UI (Chromium)** | Layout, kártya nyitás, gombok, gombméretek ellenőrizve. | Nem szükséges. |
| **Wear OS Szinkron** | Protokoll és tulajdonosváltás unit tesztelve. | Valódi Wear OS óra + telefon párosítás szükséges. |
| **Samsung Health Energy Score** | SDK olvasó unit tesztelve (1001 hibakód és DST kezelt). | Valódi Samsung Galaxy Phone + Watch szükséges. |
| **GPS nyomvonal rögzítés** | Térképmegjelenítő (SVG/Leaflet) tesztelve. | Valódi szabadtéri mozgás és GPS-vevő szükséges. |

---
*A jelentést készítette: Jules (TrainPilot AI Lead Engineer)*
