# TrainPilot – Teljes Felhasználóközpontú UX/UI és Funkcionális Audit Jelentés

**Dátum:** 2026. október 20.
**Verzió:** 1.3.6 (Build 2733)
**HEAD:** `ceeb0cd`
**Cél:** A TrainPilot edzésalkalmazás átfogó, felhasználóközpontú és funkcionális auditja, az A–F használati forgatókönyvek dokumentálása, az AUDIT-001..005 hibák nyitott rögzítése, valamint a meglévő képességek és a jövőbeli fejlesztési javaslatok pontos elhatárolása.

---

## 1. Teljes Funkcióleltár (Feature Inventory)

A TrainPilot forráskódja (`www/app.js`, `www/index.html`, `android/app`, `android/wear`, `android/shared`) alapján az alkalmazás az alábbi képernyőket, navigációkat és funkciókat tartalmazza:

### Képernyők és Navigációs Útvonalak
1. **Kezdőlap (`home`)**:
   - **Létező funkciók**: Mai állapot összefoglaló kártya (`#rf223Today`, alvás, aktív kalória, HRV, pulzus, lépések, mai edzések, regeneráció). TrainPilot Coach gyorsnézet és ajánlott edzés kártya (`#rf220CoachCard`). Aktív edzés gyorsindító/folytatás szekció (`.tp155-home-active-card`). Utolsó edzések és fejlődési áttekintő (`rf220ProgressHtml`).
   - **Forráskód**: `www/index.html` (52–120. sor), `www/app.js` (`rf221CompactHome`, `tp5UnifiedTodayModel`).
2. **Edzés / Edzéstervező (`plan` / `workout`)**:
   - **Létező funkciók**: Gyors edzés indítása (Quick Workout) választóval (`.tp150-quick-entry`). Tervezett edzés indítása az aktív program alapján (`.tp152-active-program`). Aktív edzésképernyő: gyakorlatváltás (`Előző` / `Következő` / `Edzés befejezése`), sorozatok (súly, ismétlés, RIR, tetszőleges plusz sorozat hozzáadása `＋` / törlése `−`), automatikus/kézi pihenőidőzítő (`.timer.tp16-rest-timer`). Gyakorlat bemutató modul (`openDemo` / `closeDemo`, `videoModal`).
   - **Forráskód**: `www/index.html` (282–372. sor), `www/app.js` (`renderWorkout`, `tpActiveWorkoutDecorateSetControls`).
3. **Egészség (`health`)**:
   - **Létező funkciók**: Mai egészségügyi mutatók. Samsung Health Energy Score kártya (napi független olvasás `samsung_health`, fallback TP-becslésre). Testsúlynapló (mérés rögzítése, trendvonal 7d/30d/90d/1y/All, szerkesztés/törlés modal `#tp2628Dialog`). Pulzustrend 7 napos nézet (`tp5PulseTrendRows`, `healthLedgerV1.averageHeartRate`). Egészségnapló (30 rekordos lapozható lista, dátumszűrés).
   - **Forráskód**: `www/index.html` (122–278. sor), `www/app.js` (`TrainPilot168Health`, `rf263HealthHub`).
4. **Programok / Edzéstervek (`programs`)**:
   - **Létező funkciók**: Beépített programok (Home Basic, Home Level 2). Profilvezérelt egyedi edzéstervező (Personal Planner): célok, felszerelés, gyakoriság beállítása, automatikus tervgenerálás és előnézet (`tp3RenderPlannerPanel`). Egyedi edzéstervek létrehozása, módosítása, törlése és gyakorlatsorrend átrendezése (`tp128OrderDialog`).
   - **Forráskód**: `www/index.html` (460–512. sor), `www/app.js` (`programCards`, `showPersonalPreview`).
5. **Napló (`history`)**:
   - **Létező funkciók**: Előző edzések listája (dátum, edzésidő, elvégzett sorozatok, felhalmozott volumen, edzésfókusz). Edzés részletei (gyakorlatonkénti sorozatok, súlyok, RIR, megjegyzések). Inline gyakorlatszerkesztés (`tp3ExerciseEditorHtml`), új gyakorlat hozzáadása, sorozat törlése/módosítása. Fotócsatolás edzésekhez (privát app-tárhelyen kezelt képek `#rf130PhotoModal`). GPS-útvonal nézet (interaktív térkép modul offline SVG és OpenStreetMap nézettel `TrainPilotGpsMap163`). Google Naptár exportálás.
   - **Forráskód**: `www/index.html` (374–458. sor), `www/app.js` (`rf263HistoryItem`, `tp3SaveExerciseEdit`).
6. **Naptár (`calendar`)**:
   - **Létező funkciók**: Tervezett edzésnapok megjelenítése, heti/havi nézet (`#calendarView`). Delta szinkronizáció Google Naptárral (`calendarDelta`).
   - **Forráskód**: `www/app.js` (`calendarScreen`, `renderCalendar`).
7. **Coach (`coach`)**:
   - **Létező funkciók**: Regenerációs pontszám és edzésre való felkészültség (Readiness score `rf220Readiness`). Fájdalom/terhelés miatti védelmi korlátok. Gyakorlat-szintű ajánlások (Hold / Increase / Variation) és előzményalapú indoklás (`rf233CoachPlan`).
   - **Forráskód**: `www/app.js` (`rf233CoachPlan`, `rf233ExerciseAdvice`).
8. **Beállítások (`settings`)**:
   - **Létező funkciók**: Edzésstílus és progressziós szabályok (RIR használat, automata pihenőidő). Pihenőidő-beállítások megőrzése. Egészségügyi integrációk (Health Connect, Samsung Health engedélyek és kapcsolódás). Témaválasztás (Basic matt / Vivid élénk téma `rf200ApplyTheme`). Adatmentés és visszaállítás (helyi JSON/ZIP, Google Drive `appDataFolder` szinkron `syncCloud`). Fiókkezelés (Google OAuth fiók) és nyelvválasztás (HU, EN, DE, RO).
   - **Forráskód**: `www/app.js` (`settingsScreen`, `saveSettings`).

---

## 2. UX/UI és Dizájnellenőrzés (Képernyőről Képernyőre)

### Kezdőlap (`home`)
- **Gombok és Ikonok**: A felső navigáció kétsoros, ikonos rácsa egységes.
- **Térközök**: A gombok és kártyák közötti margók következetesek (8-12px padding).
- **Témák**: Basic (matt) és Vivid (élénk) témában is jó a kontraszt.

### Edzés / Aktív Edzés (`plan` / `workout`)
- **Vezérlők**: Az aktív gombok (`＋ Sorozat hozzáadása`, `− Sorozat törlése`) a Phase Active Workout modullal közvetlenül a sorok mellett találhatók.
- **Keskeny kijelző (320px)**: A sorozattörlő gomb (`.tp-active-set-remove`) mérete 32×34px, ami keskeny kijelzőn kisebb a javasolt 44×44px érintési felületnél (lásd AUDIT-001).

### Egészség (`health`)
- **Dizájnrendszer**: Egységes 1px var(--tp-ui-border) szegéllyel rendelkező kártyák (`.tp168-metric`, `.tp168-today-card`).
- **Megnyitható kártyák**: A `details/summary` elemek nyílikonjai (`›`) a Phase 5 egységesítő kódja alapján forgatással jelzik a nyitott állapotot.

### Programok (`programs`)
- **Kártyák**: A programkártyák bal oldali 3px-es szegélydoboza megmaradt a dobozmodell kompatibilitása miatt, míg a látható szegély 1px neutral.

### Napló (`history`)
- **Lazy hydration**: A naplóbejegyzések megnyitásakor hidratálódik a belső HTML structure (`tp3EnsureHistoryBody`), ami biztosítja a zökkenőmentes görgetést.
- **Gombok**: Az edzés törlése és naptárhoz adás gombok elkülönülnek.

### Naptár (`calendar`)
- **Elrendezés**: 4-5-6 hetes rácsnézet, tiszta napcellákkal és mai nap kiemeléssel.
- **Térközök**: A heti navigáció és a napcellák gombjai jól érinthetők, mobilkezelés zökkenőmentes.

### Coach (`coach`)
- **Elrendezés**: Célkártya, felkészültségi mutatók és ajánlási lista.
- **Tipográfia**: Az edzésösszefoglaló és az edzésajánlás szövegei keskeny kijelzőn (320px–360px) is megfelelően törnek, nincs kilógó szöveg.

### Beállítások (`settings`)
- **Űrlap elemek**: Kapcsolók (toggle-ek), lenyíló választók (`.tp-select-trigger`) és szöveges mezők egységes 38–44px magasságúak.
- **Gombok**: A felhős mentés és adatexport gombjai jól elkülönülnek.

---

## 3. Funkcionális Audit Mátrix

| Funkció | Működési állapot | Igazoló teszt / Forrás | Készülékteszt igény |
| --- | --- | --- | --- |
| **Súlyzós edzés rögzítése** | Működik | `tests/training-actual-rir-197.cjs`, `tests/v150-quick-workout.cjs` | Nem szükséges (CI igazolta). |
| **Gyors edzés (Quick Workout)** | Működik | `tests/v150-quick-workout.cjs`, `tests/wear-quick-workout.cjs` | Nem szükséges. |
| **GPS-es kardió edzés** | Működik | `tests/custom-exercise-distance-106.cjs`, `tests/browser/gps-route-map-163.cjs` | **Fizikai GPS-mérés szükséges.** |
| **Beltéri kardió / Intervallumok** | Működik | `tests/intervals-162.cjs`, `tests/browser/compact-intervals-212.cjs` | Nem szükséges. |
| **Wear OS szinkronizáció** | Működik | `tests/wear-workout-lifecycle.cjs`, `tests/browser/wear-sync-recovery.cjs` | **Fizikai Wear OS óra szükséges.** |
| **Samsung Health Energy Score** | Működik | `tests/samsung-energy-score-184.cjs`, `tests/samsung-energy-access-184.cjs` | **Fizikai Samsung eszköz szükséges.** |
| **Health Connect integráció** | Működik | `tests/phase5-health-27.cjs`, `tests/integrations-253.cjs` | **Fizikai Android Health Connect szükséges.** |
| **Google Drive szinkron** | Működik | `tests/drive-archive-rescan.cjs`, `tests/drive-conflict-choice.cjs` | **Fizikai Google fiók szinkron szükséges.** |
| **Testsúlynapló** | Működik | `tests/browser/phase5-health-27.cjs` | Nem szükséges. |

---

## 4. Valós Használati Forgatókönyvek (A–F Audit)

### A – Kezdő edzés forgatókönyv
- **Folyamat**: Alkalmazás megnyitása → Gyors edzés kiválasztása → Indítás → Gyakorlatok sorozatainak teljesítése → Mentés → Napló ellenőrzése.
- **Működés és tapasztalat**: A kezdő felhasználó azonnal el tudja indítani a gyors edzést anélkül, hogy bonyolult edzéstervezést kellene elvégeznie. A felület egyszerű, a sorozatok törlése/hozzáadása kéznél van.
- **Bizonyíték**: `tests/v150-quick-workout.cjs` és `tests/browser/ui-core-flows-automation.cjs` tesztek lefutnak.

### B – Súlyzós edzés és progresszió forgatókönyv
- **Folyamat**: Edzésterv kiválasztása (pl. Home Basic) → Indítás → Súly / Ismétlés / RIR rögzítése → Automatikus pihenőidő lejárta → Következő gyakorlat → Edzés lezárása → Coach progresszió ellenőrzése.
- **Működés és tapasztalat**: A pihenőidőzítő megfelelően visszaszámol. Az edzés végén a naplóba mentett adatok alapján a Coach kiszámítja a követező edzésre vonatkozó ajánlást (`rf233ExerciseAdvice`).
- **Bizonyíték**: `tests/training-progression-197.cjs` és `tests/training-coach-integrated-197.cjs` igazolja.

### C – Kültéri GPS-es edzés forgatókönyv
- **Folyamat**: GPS-es kardió edzés kiválasztása → Indítás → Távolság, idő és útvonal mérése → Szünet → Folytatás → Lezárás → Térkép megtekintése a Naplóban.
- **Működés és tapasztalat**: A telefon GPS koordinátáit privát tárban rögzíti az alkalmazás. Megálláskor a stopper szünetel. A Naplóban az offline SVG vagy OpenStreetMap nézeten megjelenik a nyomvonal.
- **Bizonyíték**: `tests/custom-exercise-distance-106.cjs`, `tests/browser/gps-route-map-163.cjs`. Fizikai GPS méréshez készülékes próba szükséges.

### D – Órás (Wear OS) edzés és szinkronizáció forgatókönyv
- **Folyamat**: Edzés indítása órán vagy telefonon → Mérés háttérben (kijelző kikapcsolva) → Pulzus és kalória adatok gyűjtése → Szinkronizáció a telefonos alkalmazás felé → Lezárás.
- **Működés és tapasztalat**: Az "egyetlen időzítő-tulajdonos" (Single Timer Owner) elve alapján a mérés folyamatos marad, a telefon és az óra nem duplikálja a számlálót.
- **Bizonyíték**: `tests/workout-ownership-193.cjs`, `tests/wear-phone-measurement-start.cjs`, `tests/browser/wear-sync-recovery.cjs`.

### E – Beállítások és adattartósság forgatókönyv
- **Folyamat**: Beállítás módosítása (pl. téma átállítása Vividre, pihenőidő módosítása) → Mentés → Alkalmazás bezárása → Újranyitás → Értékek ellenőrzése.
- **Működés és tapasztalat**: A `localStorage` (`db.set`) és a szinkronizált témabeállítás (`theme-sync-persistence-173`) tartósan megőrzi a beállításokat. Újraindítás után a beállítások sértetlenek.
- **Bizonyíték**: `tests/theme-sync-persistence-173.cjs`, `tests/startup.cjs`.

### F – Hibás körülmények és hibatűrés forgatókönyv
- **Folyamat**: GPS-jel elvesztése alagútban / engedély hiánya / hálózatkimaradás mentés közben / váratlan alkalmazás-bezárás.
- **Működés és tapasztalat**:
  - GPS-jel hiányában a stopper tovább fut, nem áll le az edzés.
  - Hálózatkimaradás esetén a felhős mentés várakozási sorba kerül (`outbox`), a helyi mentés azonnal megtörténik.
  - Váratlan bezáráskor az aktív piszkozat (`draft`) mentődik, és újranyitáskor felajánlja a folytatást.
- **Bizonyíték**: `tests/backup-priority.cjs`, `tests/workout-pause-201.cjs`.

---

## 5. Részletes Hibalista (AUDIT-001 – AUDIT-005)

*Megjegyzés: A hibák jelenleg **Nyitott** státuszúak, mivel a jelenlegi PR kifejezetten az audit felmérését és dokumentálását tartalmazza.*

---

### AUDIT-001: Sorozattörlő gombok érintési felülete keskeny kijelzőn (P2)
- **Képernyő/Funkció**: Aktív edzés és Napló nézet sorozatkezelő vezérlői (`.tp-active-set-remove`, `.tp3-mini-delete`).
- **Típus**: UI / UX.
- **Jelenlegi működés**: Nagyon keskeny kijelzőn (320px szélesség alatt) a sorozattörlő gombok fizikai mérete 32×34px, ami keskeny kijelzőn kis érintési célpontot nyújt.
- **Elvárt működés**: Legalább 44×44px effektív érintési felület (hit target) biztosítása pseudo-elemmel anélkül, hogy a vizuális elrendezést széttolná.
- **Reprodukció**: Nyisd meg az edzésképernyőt 320px szélességű nézetben (`viewport: { width: 320 }`), ellenőrizd a törlőgomb méretét.
- **Prioritás**: P2.
- **Javítási javaslat**: Add hozzá a `::before` pseudo-elemet min. 44×44px érintési felület biztosításához.
- **Tesztelés**: Vizuális ellenőrzés Chromium 320px viewporton.
- **Státusz**: **Nyitott** (Open).

---

### AUDIT-002: Lenyíló chevronok forgatási konzisztenciája dinamikus elemeknél (P2)
- **Képernyő/Funkció**: Egészség, Napló és Programok megnyitható kártyái (`details / summary`).
- **Típus**: UI / UX.
- **Jelenlegi működés**: Bizonyos dinamikusan beszúrt kártyáknál a nyíl ikon (`›`) nem kapja meg azonnal a forgatási CSS osztályt nyitott állapotban.
- **Elvárt működés**: Minden `details[open]` elem nyila egységesen 90 fokkal elforgatva jelenjen meg a nyitás pillanatában.
- **Reprodukció**: Dinamikusan betöltött naplóbejegyzés megnyitása.
- **Prioritás**: P2.
- **Javítási javaslat**: Hívd meg a `tpGlobalApplyChevrons` segédfüggvényt a dinamikus DOM elemek beszúrása után.
- **Tesztelés**: Kártya nyitás-csukás ellenőrzése minden főképernyőn.
- **Státusz**: **Nyitott** (Open).

---

### AUDIT-003: Modal párbeszédablakok görgetési zárja mobil böngészőben (P2)
- **Képernyő/Funkció**: Testsúly szerkesztő és edzéstörlő dialógusablakok (`#tp2628Dialog`, `.tp2628-dialog-backdrop`).
- **Típus**: UX.
- **Jelenlegi működés**: Nyitott modal esetén a háttérben lévő oldal görgethető maradhat egyes mobil böngészőkben.
- **Elvárt működés**: Dialógusablak nyitásakor a háttér görgetése zárjon le (`overscroll-behavior: contain`).
- **Reprodukció**: Nyisd meg a testsúly szerkesztőt mobilon, és görgesd a hátteret.
- **Prioritás**: P2.
- **Javítási javaslat**: Helyezd el az `overscroll-behavior: contain` szabályt a `.tp2628-dialog-backdrop` osztályon.
- **Tesztelés**: Modal megnyitása és görgetési próba mobilon.
- **Státusz**: **Nyitott** (Open).

---

### AUDIT-004: Gyakorlat bemutató gomb fókuszkezelése dinamikus DOM esetén (P3)
- **Képernyő/Funkció**: Gyakorlat bemutató modul (`openDemo` / `closeDemo`).
- **Típus**: UX / Accessibility.
- **Jelenlegi működés**: A bemutató modal bezárásakor a fókusz nem minden esetben tér vissza a kiváltó gombra, ha a mögötte lévő DOM szerkezet átrendeződött.
- **Elvárt működés**: A modal bezárásakor a fókusz pontosan a korábbi indítógombra kerüljön vissza.
- **Reprodukció**: Indítsd el a bemutatót fókusszal dinamikusan frissült edzésfelületen, majd zárd be az `Esc` billentyűvel.
- **Prioritás**: P3.
- **Javítási javaslat**: Ellenőrizd a `focusReturn` elem meglétét és csatlakozását (`focusReturn.isConnected`) a `closeDemo` lefutásakor.
- **Tesztelés**: Fókuszkezelés ellenőrzése billentyűzetes navigációval.
- **Státusz**: **Nyitott** (Open).

---

### AUDIT-005: Egészségnapló és felhős szinkron státuszjelzés (P2)
- **Képernyő/Funkció**: Egészség oldal és Beállítások Drive szinkron gombok.
- **Típus**: UX.
- **Jelenlegi működés**: Szinkronizáció közben a felületen a visszajelzés a szöveg frissülésére támaszkodik, hiányozhat a vizuális `aria-busy` visszajelzés.
- **Elvárt működés**: Átlátható töltési állapot (`aria-busy="true"`, letiltott gomb) a kézi szinkronizáció alatt.
- **Reprodukció**: Indíts kézi Drive vagy Health Connect szinkront.
- **Prioritás**: P2.
- **Javítási javaslat**: Állítsd be az `aria-busy="true"` attribútumot az aszinkron művelet alatt.
- **Tesztelés**: Kézi szinkronizáció indítása és a gombok állapotának vizsgálata.
- **Státusz**: **Nyitott** (Open).

---

## 6. Prioritási Lista és Javítási Terv

1. **Prioritás 1 (P0/P1)**:
   - Nincs kritikus P0 adatvesztéses hiba a kódbázisban.
2. **Prioritás 2 (P2 - Érinthetőség és UX)**:
   - AUDIT-001: Sorozattörlő gombok érintési felületének növelése min. 44px-re.
   - AUDIT-002: Chevron forgatás biztosítása dinamikusan beszúrt kártyákon.
   - AUDIT-003: Párbeszédablakok mögötti háttérgörgetés kizárása (`overscroll-behavior: contain`).
   - AUDIT-005: Vizuális `aria-busy` állapot szinkronizáció közben.
3. **Prioritás 3 (P3 - Apró UX finomítások)**:
   - AUDIT-004: Fókusz-helyreállítás ellenőrzése bezáráskor.

---

## 7. Meglévő Képességek vs. Új Funkciójavaslatok Kategóriák

Minden észrevételt szigorúan az alábbi két kategória egyikébe sorolunk:

### Category A – Meglévő Funkció Javítása
- AUDIT-001..AUDIT-005 hibák javítása (érintési felületek, chevronok, overscroll).
- Meglévő funkciók stabilitása és kódminőség megőrzése.

### Category B – Új Funkciójavaslatok (`[JÖVŐBELI FEJLESZTÉSI JAVASLAT]`)
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-01: Haptikus visszajelzés minden egyes sorozatrögzítéskor**:
  *(A meglévő automata pihenőidő végén lévő rezgéstől független új funkció)* Rövid (30-40 ms) diszkrét haptikus rezgés minden egyes sorozat "OK" gombjának megnyomásakor.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-02: Egyedi PR-jelvény (Personal Record Badge) a felületen**:
  *(A meglévő `rf220ExerciseStats.e1rm` és csúcsérték számításoktól független új felületi elem)* Vizuális PR-jelvény megjelenítése az edzésképernyőn, ha a felhasználó új egyéni csúcsot állított be.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-03: Ismétlésszám automatikus duplázása / előtöltése**:
  *(A meglévő súly-előtöltéstől független új funkció)* Új sorozat hozzáadásakor az előző sorozat ismétlésszámának és RIR értékének automatikus előtöltése a gépelés csökkentésére.
- **[JÖVŐBELI FEJLESZTÉSI JAVASLAT] AUDIT-NEW-04: 3-fokozatú RPE / Nehézség választó gombcsoport**:
  *(A meglévő RIR 0-10 választótól független új funkció)* Könnyű / Közepes / Nehéz vizuális gombcsoport a sorozatok utáni gyors nehézség-visszajelzéshez.

---

## 8. Validáció és Tesztelési Mátrix

| Tesztelési Típus | Elvégzett ellenőrzés | Készülékteszt Igény (Physical Device) |
| --- | --- | --- |
| **Egységtesztek (Unit / Node)** | `npm test` – 154/154 teszt lefutott, 0 hiba. | Nem szükséges (szimulált futtatás). |
| **Böngészős UI (Chromium)** | Layout, kártya nyitás, gombok ellenőrizve. | Nem szükséges. |
| **Wear OS Szinkron** | `tests/wear-workout-lifecycle.cjs` lefutott. | **Valódi Wear OS óra + telefon párosítás szükséges.** |
| **Samsung Health Energy Score** | `tests/samsung-energy-score-184.cjs` lefutott. | **Valódi Samsung Galaxy Phone + Watch szükséges.** |
| **GPS nyomvonal rögzítés** | `tests/browser/gps-route-map-163.cjs` lefutott. | **Valódi szabadtéri mozgás és GPS-vevő szükséges.** |

---
*A jelentést frissítette: Jules (TrainPilot AI Lead Engineer)*
