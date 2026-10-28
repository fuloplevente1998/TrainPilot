# TrainPilot – WebView mockup-redesign fejlesztési mesterterv

**Rögzítés:** 2026-10-28 · **Állapot:** ELFOGADOTT IRÁNY / CSAK TERV, NINCS IMPLEMENTÁCIÓ

## 0. Döntés és biztonsági határ

A TrainPilot `main` ágának **már működő Capacitor + Chromium WebView** telefonos alkalmazásából indulunk. **Külön ág:** `feat/webview-mockup-redesign`, rögzített induló HEAD: `8a36502da5832a42f1cb91708ecfed5cf4f9ed62` (alkalmazáskód-baseline `7473b30f387eda54186c1e31830304ca12bd104f`, kiadási baseline **1.3.71 / 2735**, csomag `com.repforge.app`).

Első körben **csak a telefonos webes megjelenést és az ahhoz szükséges, meglévő funkciókat hívó UI-navigációt** alakítjuk át. Nem építünk új natív motort; nem cseréljük le a jelenlegi JavaScript edzéslogikát, natív Android plugineket, Wear OS motort, tárolást, adat- és szinkronprotokollokat. A meglévő kiadás és a felhasználói adatok érintetlenek maradnak. A teljes natív `feat/native-android-migration` és a draft `PR #270` külön kísérletek, nem kerülnek automatikusan összevonásra ezzel az ággal. A hibrid „WebView UI + új natív motor” lehetőséget **később, önálló technikai döntésként** vizsgáljuk.

**A 4 eredeti mockup (24 tervezett nézet) az elsődleges kompozíció**, a `main` készüléken elfogadott felhasználói működése a kötelező minimum. A Drive-on lévő 18 AI Studio-képből **csak az előnyös, kompatibilis részleteket** veszünk át, a kiválasztási jegyzék szerint: [WEBVIEW_REFERENCE_SELECTION_2026-10-28.md](../design/WEBVIEW_REFERENCE_SELECTION_2026-10-28.md).

A mostani commit kizárólag dokumentáció: nincs HTML/CSS/JS/Kotlin-módosítás, nincs tesztelt új dizájn, APK, main merge vagy kiadás.

## 1. Kanonikus referenciák (forráskötött)

- **A 4 képtábla:** [01 főoldalak/tervezés](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups/01-main-pages-and-planning.png), [02 edzések/mérés](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups/02-workouts-and-measurement.png), [03 napló/térkép/egészség](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups/03-journal-maps-and-health.png), [04 szerkesztés/beállítások](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups/04-editing-and-settings.png). A [manifest](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/mockups/MANIFEST.json) SHA256 és 1024×1536 méret szerint rögzíti az eredeti kompozit táblákat. Nem egyetlen 1024 px széles telefonképernyők.
- **Eredeti vizuális döntések és 24 nézet:** [mockup review](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/NATIVE_MOCKUP_REVIEW.md), [design handoff](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/NATIVE_DESIGN_HANDOFF.md) és [képernyőszerződések](https://github.com/fuloplevente1998/TrainPilot/blob/96c283d9c851b20e6cbfcc9f9324fb989fc6f5b3/docs/design/final-ui-ux/02-screen-contracts.md). Ezek **Compose-hoz írt technikai instrukcióit nem másoljuk** a WebView-be; a vizuális és működési döntések használhatók.
- **18 AI Studio-kép:** [eredeti privát Drive-mappa](https://drive.google.com/drive/folders/1G-s2Llme03Wah_7J_TDrS_xs5go9Ut9V), [auditált 01–18-as index](https://github.com/fuloplevente1998/TrainPilot/blob/88828ba84e15f5f5fbf4fea9384e8e1bad936167/docs/design/STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.md), [Drive vizuális audit](https://github.com/fuloplevente1998/TrainPilot/blob/88828ba84e15f5f5fbf4fea9384e8e1bad936167/docs/reviews/STUDIO_DRIVE_01_18_AUDIT_2026-10-28.md). Az eredeti privát képeket nem másoljuk nyilvános repóba.
- **Valós, működő webes forrás:** `www/index.html`, `www/app.js`, `www/styles.css`, `www/global-uiux-177.css`, `www/progress-177.css`, `www/health-dashboard-168.css`, további ténylegesen használt CSS; Android pluginok `android/app`, óra `android/wear`, megosztott rész `android/shared`. A CSS tényleges betöltési sorrendjét, komponens-state-et és a végső cascade-et ellenőrizzük, nem régi, felülírt részleteket másolunk vakon.
- **Alap-bizonyíték:** [main aktuális átadó](../WORK_HANDOFF.md), [UI tesztek](../UI_TESTS.md), [teljesítményvédelmi szabályzat](../PERFORMANCE_REGRESSION_POLICY.md), [AGENTS.md](../../AGENTS.md).

## 2. Vizuális döntések – az eltérő két dizájn összehangolása

1. **Navigáció:** az eredeti 4 mockup **öt alsó főfüle** a kiválasztott alap: **Kezdőlap · Edzés · Napló · Fejlődés · Egészség**. A 18 Drive-képen látható **öt felső fül NEM kerül automatikusan át**, mert ellentmond az elsődleges mockupnak. Az alsó sor a mobil safe-area felett marad, valódi route-állapottal. A jelenlegi webes főnavigáció helyére/átalakításával kerül, nem második párhuzamos nav-rendszerként. Programok, Naptár, Coach és minden korábbi funkció továbbra elérhető gyorsbelépőn/részleten keresztül.
2. **Fejléc:** kompakt, valódi eredeti arany súlyzó-logo; TrainPilot név; elérhető beállítások. A Studio jobb oldali nyelv-/paletta-vezérlése vizuálisan újrahasznosítható **csak a meglévő, tartós beállítást állító művelettel**, valódi készülékméret és helyigény alapján. A fogaskerék az eredeti nézetre/aktív edzésre biztonságosan visszavezet.
3. **Stílus:** matt grafit/arany *referenciatéma*, finom határok, kiegyensúlyozott kártyák, kompakt de olvasható tipográfia, igényes ikonok, karakteres CTA-k. A **main összes meglévő mentett témája/színe** változatlanul működik; nem írunk rájuk arany hexet. A natív terv 32 palettáját **nem tekintjük automatikusan meglévő WebView-készletnek**: a main tényleges katalógusát ellenőrizni kell; hiányzó paletta bővítése külön, opcionális funkció.
4. **Tartalom:** az Egészség és Fejlődés **teljes main-funkciógazdagsága** (mérési forrás, időszak, grafikon, lenyitás, rekord, testsúly stb.) megmarad. A 4 mockup fő vizuális hierarchiáját követjük; a Studio jobb sűrűségét/részletező kártyáit kiválasztva átvesszük. Minden mérés, időbélyeg, kapcsolati státusz valós adatot vagy őszinte hiány/hiba-állapotot jelez.
5. **Állapotgombok:** valamennyi valódi callbacket átvezetünk. Nincs díszgomb, szimulált siker, hardcoded személynév, állapot vagy szám; a képek példái nem backend-specifikációk. A mockup `Mentés vázlatként` feliratát csak a tényleges létező szüneteltetés/folytatás szemantikájával jelenítjük meg.

## 3. A négy tábla és a 24 nézet megőrzése

A 24 kép **nem 24 kötelező külön route**. Ha a main ezt panelben, modálban vagy lenyitásban valósítja meg, a működő út megtartható. Egyetlen meglévő funkció sem tűnhet el csak azért, mert a mockupon nincs.

| Tábla | Nézetek (6–6) | WebView megközelítés |
|---|---|---|
| **01** | Kezdőlap; Edzés belépő; Programok; Naptár; Gyakorlatkönyvtár; Gyakorlat-részletek | Kezdőlap és Edzés mockup-kompozíció; meglévő program/day/coach/gyakorlat-route és dialógus |
| **02** | Súlyzós edzés; egyoldali időmérés; kétoldali időmérés; kültéri GPS; intervallumok; edzés befejezése | A már működő edzés/óra/idő/GPS/undo/mentési szemantikát reprezentáló új vizuális réteg |
| **03** | Naplólista; edzésrészlet; útvonaltérkép; Fejlődés; Egészség; Coach | Meglévő részletes napló, eredeti anatómia, a teljes progress/health és az eredeti piros X megtartása |
| **04** | Beállítások; óra/szinkron; mentés/visszaállítás; engedélyek; edzésfotók; programszerkesztés | A main meglévő vezérlőit és valós írási/engedélyezési útjait szekciókba szervezzük |

## 4. Funkció- és adatbiztonság: a dizájn nem változtathat szabályokat

- **Edzés:** stabil session és workout/set azonosítók, aktív/félbehagyott edzés folytatása, helyes előtöltés, sorozatmentés, visszavonás, RIR/Coach, programnap, lezárás és elvetés. Nincs új globális `render`/`go`/`finishWorkout` felülíró wrapper, párhuzamos edzésmotor vagy második timer.
- **Idő/GPS:** háttér-/foreground mérések, pihenő, ±15 mp, explicit stop, kétoldali mérések, route privacy és megőrzés. Navigáció vagy Settings megnyitása ne jelöljön teljesítést, ne indítson új mérést és ne veszítse el az aktív sessiont.
- **Wear OS:** eredeti párosítás, Data Layer ownership, offline edzés, visszaigazolt átadás, read-only telefonos tükör; a UI nem hazudhat „csatlakoztatva”/„átadva” állapotot visszaigazolás nélkül.
- **Egészség:** Health Connect, Samsung Health, aktuális/forrásjelölt pulzus, alvás, lépések, HRV, energia és egyéb main-metrikák; forrás, frissesség és engedély hiányakor megfelelő üres/hiba. A Coach/Samsung/TrainPilot pontszámokat nem olvasztjuk össze.
- **Tár:** helyi adatok, napló, fotók, testsúly, beállítások, mentés/visszaállítás, Drive/Calendar szinkron, tombstone és duplikációvédelem. Ne váltsunk tárolási formátumot vizuális okból; az APK rátelepítése előtt explicit upgrade és adatmegőrzési próba szükséges.
- **Megjelenés:** HU/EN/DE/RO; minden main mentett téma, eredeti launcher súlyzó és anatómia, fókusz/akadálymentesség, szövegnövelés, legalább kb. 48 CSS px érintési cél mobil méretezésben a tényleges viewport/DPR figyelembevételével.

Ha a mockupon olyan művelet van, amelynek a mainben **nincs hiteles funkcionális megfelelője**, azt a lefedettségi listába `GAP / külön döntés` státusszal kell tenni. Nem szabad dekorációval késznek nyilvánítani. Előzetesen ilyen lehet bizonyos inline eszköz- vagy quick-start részlet, ezért az első implementáció előtt szükséges a tényleges main callbackleltár.

## 5. Fejlesztési szakaszok és mérhető kapuk

| Szakasz | Terjedelem | Belépési feltétel / átadás |
|---|---|---|
| **P0 – rögzítés és feltárás** | jelen két dokumentum; 4 tábla és 18 kép forrásazonosítása; `main` UI route/funkció, CSS-cascade, téma- és perf-baseline leltár; érintett callbackek | Most csak a tervek és a még nem vizsgált integrációk dokumentálva. Nincs UI-kód |
| **P1 – közös keret és Kezdőlap** | grafit/arany referencia, **öt alsó főfül**, fejléc, valódi Settings visszaút, Home hero/mai edzés/óra/gyorsindítás/állapotkártyák; empty/loading/error | Valós route-váltás, aktivált edzés folytatás, téma/nyelv, 320–412 px, navigáció gyorsasága és nyers Chromium-képek |
| **P2 – Edzés és alap használat** | edzésbelépő, program/gyakorlat/naptár/Coach linkek, quick start; aktív edzés sorozat- és pihenőfelület | Sorozat, timer/GPS, undo, pause/resume, lezárás, Wear ownership megőrzés; valós interakció |
| **P3 – Napló / Fejlődés / Egészség** | 03. mockup és kiválasztott Drive-kártyák; gazdag main metrikák, grafikonok, időszakok, részletek | 0/200/1500 edzés, disclosure és gyorsaság, helyes mért adat/forrás, napló és fotófunkció |
| **P4 – többi 24-es nézet** | 04. szerkesztés/Beállítások és a 02–03 finom részletei; négy nyelv, témák, reszponzivitás | Mentés, visszaállítás, fájl és engedélyút, privát kép, párosítás, félbeszakadt folyamat és akadálymentesség |
| **P5 – teljes elfogadás** | szükséges javítások, lefedettségi mátrix, azonos forrású képek és CI, **külön teszt-APK**, fizikai telefonos kipróbálás | Aláírás/csomag/verzió/adatmegőrzés, telefon+Wear ellenőrzés; csak külön felhasználói jóváhagyással main merge/release |

A fázisok **nem határidőígéretek**. Kisebb csomagokkal haladunk, és minden P1–P5 működési/kinézeti eredményt tényleges renderből és tesztből jelentünk.

## 6. Teszt- és vizuális elfogadási szerződés

1. A módosított JS/CSS/HTML-re célzott automatizált teszt, `npm test`, teljes `npm run test:ui` / **hat Chromium-shard**, és az alkalmazható Android/WebView, Wear és release-kapuk GitHub Actionsön. A szerződés és a meglevő tesztmennyiség nem gyengíthető kizárólag a dizájn miatt.
2. Az érintett útvonalakra azonos fixture-bemenettel **előtte/utána** teljesítménymérés: indítás, navigáció, Journal, Health, Settings, 0/200/1500 workout és nagy adatállomány. A [perf-szabályzat](../PERFORMANCE_REGRESSION_POLICY.md) eredeti limitjeit nem írjuk át mérés nélkül; ne legyen DOM-robbanás, újraszámolás vagy teljes oldal újrarender a stílus miatt.
3. Valós WebView/Chromium screenshotok, nem AI-újrarenderelt referenciák. Telefon ellenőrzési profil: **1080×2392 / 450 logikai dpi / 384 dp / fontScale 0.90** mint cél, valamint 320/360/393/412 CSS px, nagy betű, tájolás/keyboard/safe-area. A webes CSS px és a natív dp nem automatikusan azonos; tényleges viewportmérést végzünk.
4. Minden szakaszban kontraszt, teljes szöveg, HU/EN/DE/RO, fókusz, nagy betű, fizikai érintés, scrollpozíció, aktív session, mentés/visszaolvasás, nincs díszgomb vagy félrevezető mért adat.
5. Fizikai készülékre telepítés előtt azonos aláírókulcs, `applicationId`, verziópolicy, upgrade-biztonság, régi adat megőrzése; új teszt-APK **nem telepíthető automatikusan** a felhasználó saját adataira.

## 7. Munkarend és kész definíció

- UI-kód csak **P0 forrásfeltárás után**; az új dizájn nem feltételez készre jelentett hiányzó funkciókat.
- Egy jól követhető CSS/komponensirány, meglévő `index.html` betöltési sorrendjének tiszteletben tartásával. Nincs új párhuzamos navigációs motor, SPA/framework-csere, indokolatlan új globális hooklánc.
- Minden képernyőnél: (1) mockup-rész, (2) hasznos Studio-rész, (3) main adat+callback, (4) eltérések/GAP, (5) mérés/screenshot, (6) tényleges elfogadás.
- „Kész” csak **valódi megjelenés + eredeti main-funkcióegyezés + regresszió + használható készülékes felület** együtt. Egy zöld teszt vagy egy szép kép önmagában nem elfogadás.
- Branch és draft PR a `main` irányába; a PR #270 változatlanul külön marad. **Main merge, kiadás, verzióemelés, natív motor portolása: külön engedély.**

## 8. Első kódolási csomag előtt pontosan ezt ellenőrizzük

1. Valódi main navigációs DOM, útvonalak és `go`/render-mechanizmus; melyik CSS a legutolsó érvényes, mi a footer és timer pozicionálási szerződése.
2. A négy mockup és a 18 kép közös UI-mintái, hiányzó részletek és ellentétes szándékok képenként; a mellékelt referenciajegyzékből választási mátrix.
3. A `main` aktuális tényleges témakatalógusa, rendelkezésre álló arany súlyzó és izomtérkép asset; `main` Health/Progress és aktív-edzés állapotok/callbackek.
4. Jelenleg futó és elfogadott telefonos/Wear baseline ugyanazon forráshoz kötve; a mainből nyitott design ág szerkezeti különbségei.
5. P1-et **csak** úgy kezdjük, hogy az aktuális main funkcióinak elérése igazolható, és az öt főfül miatt egyetlen edzésművelet vagy diagnosztikai/engedélyoldal sem válik elérhetetlenné.
