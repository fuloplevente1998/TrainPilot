# TrainPilot – javítási munkaterv (2026-10-22)

Forrás: a felhasználó által átadott `TrainPilot-javitasi-prompt.md`. Az eredeti
prioritások és elfogadási feltételek irányadóak. Nem jelent automatikus engedélyt
main-merge-re, Play-kiadásra vagy verzióemelésre.

**2026-10-22-i állapot:** #235/#237 mainen; a felhasználó az indulási és navigációs gyorsítást, valamint a gombszín/oldal együttváltását készüléken elfogadta, és új kiadást kért változatlan 1.3.71 /2735 verzióval. A lentebbi kiinduló lassulási adatok történeti mérések. A P1.1/P1.3 mennyiségi céljai és a P1.4 további cache-munkája külön nyitott marad, nem nyitott készülékes kiadási hibaként szerepel.

## Ellenőrzött kiindulópont és megőrzendő fejlesztések

- 2026-10-22 ellenőrzött aktuális ág: `perf/236-journal-dom` (#237 Draft),
  #235 `refactor/workout-finalization` ágára épül; a 2026-10-21-i újabb
  alkalmazáskód `96a31a7e20f3cce951558c1f81f3f1bc4340b50a`.
- #236 eddig: 32-es Napló-lapozás, teljes történetre szűrés, lusta részletek;
  PR-jelvényhez alias→sor index. Ezek megőrzendők, nem újraimplementálandók.
- A PR-cache `data()` ága **még** minden paint során teljes
  `repforge:history` + tombstone raw sztringet olvas/összefűz a
  `www/app.js` `workout-records-226.js` szekciójában. A csatolt P1.4
  emiatt csak **részben kész**.
- [Legutóbbi aktuális profil](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39689250994):
  4× CPU, 0/200/1500 edzés, két kör; `96a31a7` 1500-as első szintetikus
  Napló ~669 ms, Naptár ~253 ms, Coach ~222 ms, Beállítások ~225 ms.
  Ezek **egyszeri** minták, nem az új tervben előírt 5-mintás mediánok.
  Az input→paint proxy nem fizikai telefon-FPS vagy input latency.
- A korábbi képen említett 490+440 ms ne legyen feltételezett állandó
  vagy összeadható költség. Az új baseline azonos forrásból, azonos runneren
  és az előírt öt ismétlésből származzon.

## P1 – Teljesítmény: ebben a sorrendben

- [ ] **P1.1** `tp155R4RefreshPanel`, késői paneldekorációk és
  `getBoundingClientRect`/layout-hívások. 0/200/1500 fixture és 5 futás
  mediánja. A panel nélküli oldalváltás ne hívjon drága panelmunkát;
  DOM-olvasás és DOM-írás külön fázisban történjen. Cél: azonos mérésű,
  **nem átfedő** két költség együttesének legalább 50%-os csökkenése.
  Új globális felülíró réteg tilos; a meglévő modulban javíts.
- [x] **P1.2** `tp107FitViews`: valódi indulási hívásszám mérés, legfeljebb
  egy tényleges indulási layout-fit, fölösleges ismételt reflow nélkül.
- [ ] **P1.3** `calendarHtml` és panelnaptár: 0 és 200 edzésnél
  4× CPU-val <400 ms oldalnyitási medián, azonos méréssel.
- [ ] **P1.4** A kész aliasindex megtartása, de a minden painthez kötött
  megabájtos raw JSON-vizsgálat megszüntetése. A helyes érvénytelenítés
  *tényleges* history/tombstone/adatváltozáshoz (edit, delete, import,
  Drive/Wear, azonos elemszám) kapcsolódjon. Napló-lapozás, PR-gombok,
  fókusz, új és régi rekordok regresszió.
- [x] **P1.5** Telefon release R8 `minifyEnabled` +
  `shrinkResources`, Capacitor/plugin/Gson/Samsung/Wear/GMS keep-szabályok,
  `androidx.profileinstaller`, indulási baseline profil.
  Külön kockázatos natív commit; csak release unit/UI és telepíthető,
  eredeti aláírással megegyező teszt-APK után fogadható el.

## P2 – Adatbiztonság és összeomlás

- [ ] **P2.1** `db.set` QuotaExceeded/failure felhasználó felé látható,
  hatnyelvű tárolási hibajelzés és exportlehetőség;
  `finishWorkout` nem veszít session/draft adatot és nem küld mentési nyugtát.
  Kötelező hiba-injektálás `repforge:history` írásra.
- [ ] **P2.2** Sérült `db.get` JSON eredeti nyers értékének
  méretkorlátos mentése és egyszeri figyelmeztetés; hibás adatot
  ne írjon felül automatikus default. Restore és storage-telítettség teszt.
- [ ] **P2.3** Hat telefon/Wear előtérszolgáltatás korai visszatérésének
  ellenőrzése. `startForeground()` időben fusson, értesítéscsatorna és
  manifest service-type egyezzen; early-return útvonalon `stopForeground`/
  `stopSelf`, dedikált natív regresszió.
- [ ] **P2.4** Órás Kotlin `!!` megszüntetése biztonságos helyi
  `val`/nullágakkal, különösen WearHealthService, MainActivity és
  WearGpsRouteService193; viselkedés és protokoll változatlan.

## P3 – Funkció, fordítás, konfiguráció

- [ ] **P3.1** Wear szolgáltatás-értesítések lokalizációja
  HU/EN/DE/RO, és ahol az alkalmazás támogatja, SK/PL; nyelvi állapot
  követése, telefon/Wear konzisztencia.
- [ ] **P3.2** Capacitor `appendUserAgent` verzió és közös
  build-metadata ellenőrzés, a verziószám kézi emelése nélkül.
- [ ] **P3.3** `trainpilot://wear` intent filter: dokumentált döntés
  érvényesített deep-link feldolgozásról **vagy** felesleges filter kivétele.
- [ ] **P3.4** Natív GPS route ledger 512-es limit előtt figyelmeztetés,
  felhasználói megerősítéses törlési opció; koordináták privátak maradnak,
  felhőmentésből kizárva.
- [ ] **P3.5** 320/393/768 px, sötét/világos UI:
  szóközi navigációs felirat-törés és a két navigációs sor egységes affordanciája.

## P4 – Külön, hosszabb refaktor

- [ ] Többszörös globális `shell/render/settingsScreen/renderWorkout`
  felülírások összevonása, 796 inline `onclick`, elnyelt hibák,
  a ~2,2 MB `app.js` és ~214 KB `index.html` modulhatárai;
  `global-uiux-177` Health háttér CSS-teszt eltérésének kivizsgálása.
  Nem része az első P1.1 commitnak.

## Kötelező ellenőrzési/kiadási szerződés

1. Minden javítás külön áttekinthető commit, célzott regressziós teszttel;
   ne csökkentsük a régi tesztek elvárásait valós regresszió elfedésére.
2. Node 22, `npm ci`, `npm test`, célzott Chromium UI; teljes Quick,
   Performance, 6/6 UI shard és natív release + Wear kerek UI GitHub Actionsben.
3. P1 mérés: 393×873, hu-HU, 4× CPU CDP, 0/200/1500 edzés
   (6 gyakorlat × 4 sorozat); startup, tárolásolvasás és minden
   érintett oldal két-rAF késése; **minimum 5 futás medián** ugyanazon runneren.
4. A natív készülékes elfogadás külön ellenőrzés. Nyitott PR/issue
   és verzió 1.3.71/2735 mindaddig változatlan, míg explicit engedély nincs.
5. Minden átadásban: módosult fájlok, tesztek, előtte/utána metrikák,
   commit és pontos forrás, még nem igazolt pontok. Nem állítunk
   APK-készülékes sikert pusztán Chromium/CI alapján.
