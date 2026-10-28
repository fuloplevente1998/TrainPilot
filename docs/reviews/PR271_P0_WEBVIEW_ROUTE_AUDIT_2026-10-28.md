# PR #271 – P0 feltárás és első WebView implementáció, 2026-10-28

**Ág:** `feat/webview-mockup-redesign` · **Kiindulás:** `main@8a36502` · **Appverzió:** 1.3.71 /2735 változatlan.

## Ténylegesen elolvasott források

- `www/index.html`: stylesheet-include sorrend, meglévő `render` dekorátorok, `tp5UnifiedTodayGridHtml` / `rf223StatusHtml`, `#rf223Today` hat valódi kártyája, Home/Health belépők.
- `www/styles.css`, `www/global-uiux-177.css`, `www/health-dashboard-168.css`, `www/progress-177.css`, `www/typography-137.css`, `www/page-frame-109.css`: a valódi komponensek és utólagos CSS-specifikusság; ebből következik a külön, **utolsónak betöltött és scoped** `www/webview-mockup-p1.css` használata.
- `tests/browser/nav-active-latency-176.cjs`, `tests/browser/issue67-navigation-checkbox.cjs`, `tests/browser/overview-layout-107.cjs`, `tests/browser/home-draft-compact.cjs`, `tests/browser/global-uiux-177.cjs`: navigáció/Chrome, Journal belső panelek, Home fix magasság/draft, legalább 320/360/393/412px és a basic/vivid témák működési szerződése.
- A több mint 2 MB-os `www/app.js` teljes tartalma a GitHub `fetch_file` csatlakozón keresztül jelenleg **nem volt kiolvasható** (a hívás üres tartalmat adott). Ezért a teljes kanonikus router, callback- és adatútleltár **még nem teljes**. P1a tudatosan nem módosítja a routert, motorstate-et vagy valamelyik írási útvonalat. Később célzott helyi/CI forrásvizsgálat kell, mielőtt az 5 fülre váltunk.

## Meglévő menü – rögzített megfigyelések

A `nav-active-latency-176.cjs` ellenőrzi a meglévő `.top.tp154-nav-grid` **8 eredeti gombját**, a kiválasztás azonnali visszajelzését és a Calendar, Coach, Settings panelnyitást. A tesztben a `home`, `plan`, `programs`, `history` route-ok és a paneles három művelet bizonyítottan használatban vannak; `health` útvonalat a Home/Health regressziók használják. A `history` alatt a `.tp155-journal-tabs` **három alnézet**: Edzésnapló, Fejlődés, Statisztikák. A Fejlődés kiválasztása ma nem egy új fő `go('progress')` útvonal, hanem Journal-alnézet.

**Következmény az új 5 alsó fülhöz:** Kezdőlap → `home`, Edzés → a működő `plan`/indító út, Napló → `history` naplónézet, Fejlődés → `history` Fejlődés alnézet, Egészség → `health`. A Programok/Calendar/Coach/Settings belépői nem tűnhetnek el, a profil/szerkesztő/órás részletek elérése végig kötelező. Az Android Back és a modalok/fix Home/akciósor alsó insets megőrzése külön P1b teszt.

**A menü átépítése még nincs implementálva.** A jelenlegi `top` elrendezésre több teszt explicit geometriát és nyolc gombot vár; puszta `display:none` vagy a meglévő gombok klónozása nem biztonságos, mert a kiválasztott állapot/Back/egy kattintásra egyszeri navigáció megbontható.

## Első megvalósított, visszafordítható UI-kód

| Elem | Változás | Amit NEM változtat |
|---|---|---|
| `www/webview-mockup-p1.css` | Új vizuális kártyarend a valódi Home-on: kiemelt hero él, 6 metrikkártya, forrásadatú Coach-panel; mentett téma accentjei | Nincs új kártya, mért érték, JS, route, timer vagy adatírás |
| `www/index.html` | A scoped stíluslap egyszeri betöltése a jelenlegi CSS-sor végén | A HTML törzse és meglévő scriptjei változatlanok |
| `tests/browser/p271-webview-home-mockup.cjs` | Két telefonviewport × két téma; változatlan geometriák, 6 állapotkártya, 8 eredeti gomb, Home→Health→Journal→Home, screenshot | Nem lazít meglévő tesztet |
| `tests/browser/suite.json` | A P1a próba bekerült a teljes 6-shard regresszióba | Semelyik korábbi teszt nem kerül ki |

A P1a **szándékosan csak vizuális első részcsomag**, nem a teljes Home 01 mockup, és nem az ötfüles navigáció. A teljes, 18 képpel egyező dizájn nem igazolt.

## Megmaradt P0 ellenőrzés és következő implementáció

1. A nagy `app.js` tényleges render/router függvényeinek célzott forrásfeltárása CI-ben vagy ellenőrzött fejlesztői munkakönyvtárban; pontos belépési pont és a top-szinkronizált selection-state.
2. Öt alsó fül: **egy** kanonikus route-állapot, Journal/Progress szétválasztott megjelenítési szándék, a többi régi belépő érvényes célhelyre rendezése; nincs új alapmotor.
3. Panelok és aktív edzés: z-index, safe-area, fixed timer, onboarding, 320px, nagyfont, négy nyelv, minden témacsalád; teljes életciklus regresszió.
4. A komponensek és a 18 Studio-elem `main` hívás/hiteles állapot szerinti lefedettsége; hiányzó tételek nyilvántartása.
5. Új UI-commit után ugyanazon forrásra Quick, Performance, hat Chromium shard, Android/WebView és felhasználói készülékes elfogadás. Egy most futó CI nem jelent automatikus PASS-t. Nincs main/release merge.

## Biztonsági megkötés

A felhasználó által napi használatban lévő, mentett adatos APK-t nem telepítjük felül automatikusan. A natív PR #270 kódját nem vonjuk be; az aláírás, verzió, applicationId, edzésmotor, Wear, Health, GPS és szinkron változatlan. A referenciák privát Drive-képei nem kerülnek ki a GitHubra.
