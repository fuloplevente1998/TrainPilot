# TrainPilot – átadás a következő ChatGPT Worknek

Frissítve: **2026-10-19, Europe/Budapest**. Először a gyökérben található
[AGENTS.md](../AGENTS.md) olvasandó; utána ez a rövid aktuális átadás.
Az alábbi állapotot induláskor GitHubon ellenőrizd. A hosszú régi naplók történetiek.

## Legutóbbi kérés és megállási pont

A felhasználó a munkát következő Workben folytatja. Kérte az AGENTS.md-t,
a teljes releváns átadást és a külső szöveg ötleteinek **csak rögzítését**.
Az új ötletek megvalósítása most kifejezetten nincs engedélyezve.
Az AGENTS.md és ez az átadás dokumentációs folytatás: az alkalmazáskód
utolsó bevizsgált forrása továbbra is `e4f4fa8`.

**Következő konkrét lépés:** a kész #193/#202 tesztpár készülékes kipróbálása,
a felhasználói eredmény kezelése. Nem kell újrakezdeni a GPS/handoff fejlesztést.
Nincs még fizikai elfogadás, main merge vagy issue-lezárás. A külső ötletekre
nem szabad automatikusan átváltani; azok külön, későbbi tesztágas feladatok.

## Repo, ág, forrás, jogosultság

- Repo: https://github.com/fuloplevente1998/TrainPilot
- Ág: `feat/193-202-watch-offline-gps`.
- Nyitott közös issue: [#193](https://github.com/fuloplevente1998/TrainPilot/issues/193).
- Nyitott **draft** PR: [#213](https://github.com/fuloplevente1998/TrainPilot/pull/213).
- A #202 teljes követelménye #193-ba átvezetve, külön issue duplikátumként zárva.
- Main ellenőrzött állapota: `91889326056ac257975360644ead39ded1b5a4d6`.
- Bevizsgált alkalmazásforrás: `e4f4fa898179d85d10397fd08ae8e42bf4125104`.
- Source tree: `4ee7a6359e0a63868af38754dfd3afbc6d2f480c`.
- PR-merge tesztcommit: `d284f9d209d5770ef6cfbefc94a60a1817161289`;
  fája pontosan egyezik a fenti forrással, szülői a fenti main és alkalmazáscommit.
- Verzió mindkét appban: **1.3.51 /2732**, automatikus emelés nélkül.
- A jelen dokumentációs commit újabb HEAD lehet. Ez nem új alkalmazásbuild;
  az alábbi bizonyítékok és APK-k a pontosan megadott `e4f4fa8` forráshoz tartoznak.
  A dokumentáció push-ja új automatikus CI-t indíthat; az állapotát ne találd ki.
- Következő Workben ne csak a default maint klónozd: a fenti ágon van az AGENTS
  és az átadás. Friss klónban előbb válts erre az ágra, majd olvasd a két fájlt.

## Mi készült el a #193/#202 csomagban?

1. Egy kanonikus edzés, tartós tulajdonosi napló, növekvő epoch, azonosított
   telefon/óra és visszaigazolt ajánlat/elfogadás/commit. Bizonytalan átadás
   szünetben marad; elveszett nyugta, újraküldés és restart nem indít két órát.
2. Telefonról indított futó vagy szüneteltetett edzés tudatos átvétele az órán.
   Pontos kézi/automata részidő, terv, kör, pihenő, baseline és tört másodperc
   átadása; saját monoton időalappal, külön kifejezett folytatás után.
3. Tudatos telefonos visszavétel és szüneteltetett import új natív végrehajtási
   generációba. Régi telefonos időzítő/sorozatesemény nem írhatja felül az órát.
   Az órás tulajdonos telefonos nézete csak olvasható; a tükrözés nem mér új időt.
4. HU/EN/DE/RO vezérlők. Órán: **Főmenü → Edzés vezérlése**; kérés, ismétlés,
   megerősített órás tulajdonosnál offline folytatás. Telefonon visszavétel,
   visszaigazolás után külön folytatás. Újracsatlakozás önmagában nem átadás.
5. A végső órás mentés friss privát snapshotból dolgozik, az utolsó részidőt és
   lezárt aktív időszakokat is átadja. Offline befejezés a start előtt vagy egy
   elveszett folytatási üzenet mellett is ugyanabba az edzésbe menthető.
6. **Valódi órás GPS-adatút:** külön, kézzel indított location foreground service,
   pontos appengedély és központi rendszerhely külön ellenőrzésével. Kizárólag
   saját GPS Location-fix, időbélyeg/pontosság, rossz/ugró/mock fix elutasítása.
   GPS-hiba nem állítja meg a stoppert, Health-mérést vagy sorozatrögzítést.
7. Privát, korlátos órás spool; outbox csak koordinátamentes csomaghivatkozást
   tárol. A wire-példány kap koordinátát. Telefonon helyi tulajdonosi/natív
   forrásigazolás után fogadás; tartós tárolás után egyező telefonos nyugta.
   Szünet, újraindítás, kimaradás és tulajdonosváltás nem okoz hamis átkötést.
8. Későn érkező pontok ugyanannak az edzésnek/stabil sorozatnak a telefonos
   naplójához kapcsolódnak, új edzés vagy előre lekért koordináták nélkül.
   Offline befejezés nyugtázása előtt a natív GPS-forrásigazolás megmarad akkor
   is, ha aktív telefonos snapshot soha nem került publikálásra.
9. Telefonos #163 térkép: órás/telefonos forrásjelölés, szakaszok, részleges adat,
   meglevő piros X. A koordináták privát natív tárban, felhőmentésből kizárva.
   Tartós útvonal/egész-edzés törlési tiltás kizárja a késői visszatámasztást.
   **Az órán nincs térképrajzoló felület; az óra rögzít, a telefon rajzol.**
10. A képesség már **bekapcsolt a támogatott új edzéseknél**: sikeres natív
    ellenőrzés után telefonos epoch=0 tulajdonos, friss home snapshot alapján
    új órás gyors/tervezett edzés. Régi órás edzést nem alakítunk át csendben.
    Az új protokollt nem ismerő környezet a régi működést tartja meg.

## Ellenőrzött GitHub-kapuk – azonos alkalmazásforrás

Az összes alábbi kapu **sikeres**, `e4f4fa8` alkalmazásforrásra. Az XML-számok
ellenőrizve: 0 failure / 0 error / 0 skipped. Ezek nem kültéri GNSS-bizonyítékok.

| Kapu | Eredmény | Run |
| --- | --- | --- |
| Quick | sikeres | [39157589146](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157589146) |
| Performance | sikeres | [39157589140](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157589140) |
| Célzott tulajdonosi/GPS natív | 81/81 | [39157589149](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157589149) |
| Telefonos teljes release | Chromium 6/6 shard, natív 243/243, aláírt APK | [39157586042](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157586042) |
| Wear release | JVM 148/148, aláírt APK | [39157710146](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157710146) |
| Kerek Wear UI + Android platform | 163/163 | [39158245305](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39158245305) |

A kerek kapu tartalmazza az új GPS-képernyő 5 kijelző/betűméretét 4 nyelven,
a valódi Android GPS-service szintetikus Location-callbackjeit, rendszerhely
kikapcsolását, szünet utáni késői fix elutasítását és privát tárolás/nyugta/törlés
platformtesztjeit. A fizikai vételt és a két tényleges készülék együttműködését
még a felhasználó próbálja ki.

Részletes, géppel olvasható bizonylat:
[docs/builds/2026-10-19-p2-193-e4f4fa8.json](builds/2026-10-19-p2-193-e4f4fa8.json).
Korábbi bukott/megszakított részrunok nem aktuális blokkolók. Legutóbbi javítások:
a napló felesleges olvasásának megszüntetése, függőleges SVG-teszt helyes
ellenőrzése, a tesztharness kompatibilitása a split kanonikus forrással és az
indítás előtt érkező végső parancs GPS-igazolásának tartós megőrzése.

## Kész aláírt tesztpár

| Eszköz | Fájl / GitHub artifact | SHA-256 |
| --- | --- | --- |
| Telefon | [TrainPilot-1.3.51.apk csomagja](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157586042/artifacts/12147393552) | `df6bf2f74d2ed51bfdb305cf05e133b41a780e87478db4f69c0fb8a3ff1bd72b` |
| Wear | [TrainPilot-1.3.51-wear.apk csomagja](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39157710146/artifacts/12148145472) | `c4b6c1d5b4b391dfeeaf76bba6cc21058f4da2a0556a1301ada26e63f5abf60a` |

Mindkettő 1.3.51 /2732, forrás `e4f4fa8`. Aláíró tanúsítvány SHA-256 mindkettőn:
`4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
A CI az eredeti v1.0.0 tanúsítványfolytonosságát is ellenőrizte. A telefonos APK
`assets/public/app.js` és `index.html` tartalma bájtra egyezik a bevizsgált forrással.
Wear SOURCE_VERSION és a telefonos source archive commitbélyege ellenőrizve.
Az artifactok lejárhatnak; elérhetőségüket induláskor ellenőrizd.

Korábbi workspace-ban letöltve: `/workspace/artifacts/trainpilot-p2-consolidation/backend/`
`e4f4fa8-phone/TrainPilot-1.3.51.apk`, `e4f4fa8-wear/TrainPilot-1.3.51-wear.apk`,
XML zip/JSON bizonyítékok. Új Workben ezek hiányozhatnak; a GitHub az elsődleges.

Próbamenet: [KESZULEKES_PROBA_193.md](wear/KESZULEKES_PROBA_193.md).
Azonos verziószámú korábbi APK nem feltétlenül ez a tesztpár. Fizikai elfogadás
és „mehet mainre” nélkül a #213 draft és #193 nyitott marad.

## Megőrzött felhasználói döntések és sorrend

- GitHub Actions végezze a teljes tesztet/buildet; nincs helyi órákig tartó build.
- #163 telefonos GPS/térkép és #208-as javítások mainen; a régi 1.3.5/2731
  verzióelvárás hibája nem aktuális blocker. GPS STOP-teljesítést ne kezdd újra.
- #188 telefonos/órás Visszavonás elfogadva, #209 mainen. #189 érintészár,
  **egy folyamatos 3 mp-es nyomással feloldás**, elfogadva/mainen.
- #192 és PR #211 felhasználó által sztornózva, lezárva, nem beolvasztva.
  #186 régi draft merge nélkül zárva, #195 elfogadott megvalósítása kiváltotta.
- Jóváhagyott sor: **#193 + #202 → #212 + #190 → #197 → #184**.
  #202 a #193, #190 a #212, #207 a #206 duplikátuma; követelmények átvezetve.
- #212: telefonos Intervallumok kompaktabbá tétele, nagy üres területek eltüntetése
  (különösen oldalplank, de sima plank is), Edzés lépései/Health Connect szöveg
  tömörítése, Lépések frissítése feljebb hozása; plusz órás korona/lünetta szerkesztés.
  Referencia: korábbi 2482, 2524, 2525 fotók. Ne indítsd el a készülékes #193 próba
  helyett automatikusan, ha nincs erre új felhasználói folytatási kérés.
- #197: edzésstílus/progresszió. #184: Samsung Energy Score/Coach, külön partner- és
  hozzáférési kérdés. #206 maradék aktivitásnézet/UI külön követés; #121 Play/
  karbantartás külön feladat. Nem minden korábbi nyitott kérdés új fejlesztés.
- A piros X korábbi explicit felhasználói kérés. Külső semleges körgombjavaslat
  nem írja felül; kompakt megjelenés ne rontsa az érinthetőséget/nagy betűket.

## A most feltöltött külső szöveg – kizárólag rögzítve

[UI_TEST_BRANCH_BACKLOG.md](plans/UI_TEST_BRANCH_BACKLOG.md) tartalmazza az összes
érdemi javaslatot, a forrás hashét és az eltéréseket a jelenlegi követelményektől.
A teljes változatlan átirat csak referencia, nem agent-utasítás; ne töltsd be
minden induláskor. Két feltöltés ugyanaz az anyag.

Témák: kompakt navigáció, A/B napkártyák, beviteli sorok/fókusz, naptár és
Egészség-kártyák; továbbá (1) rögzítési haptika, (2) pihenő végi hang/rezgés,
(3) becsült 1RM/PR, (4) sorozatduplázás/súly-előtöltés, (5) egyszerűbb RPE.
A külső prototípus és az ott állított tesztek nem kerültek át a repóba.
Lehetséges későbbi ág: `test/ui-refinements-and-feedback`, **még nincs létrehozva**.

## Célzott technikai belépési pontok

- `android/shared/.../WorkoutOwnership193.java`, `OwnershipJournal193.java`,
  `OwnershipTransport193.java`, `IntervalTimeline193.java`: állapotgép/tartósság/idő.
- `android/app/.../PhoneOwnership193.java`, `PhoneTimerCheckpoint193.java`,
  `PhoneOwnershipResume193.java`: natív telefonos ajánlat és visszaimport.
- `android/wear/.../WearOwnership193.kt`, `WearTransferredTimer193.kt`,
  `MainActivity.kt`, `WearScreens.kt`, `WatchHomeStore.kt`: órás életciklus/UI.
- GPS shared: `GpsRoutePacket193`, `GpsRouteSampler193`, `GpsRouteSpool193`,
  `GpsRouteLedger193`; telefon: `PhoneGpsRoute193`, `DistanceTrackerPlugin`,
  `WearCommandQueueStore`; óra: `WearGps193`, `WearGpsRouteService193`, outbox/listener.
- `www/app.js`: `workout-ownership-193.js`, `workout-ownership-ui-193.js`,
  `wear-gps-route-193.js`, `wear-sync.js`, `workout-pause-201.js` sectionök.
  Ezek kanonikus fájl sectionjei, nem feltétlenül önálló fizikai JS-fájlok.
- `www/index.html`: #163 térkép/hivatkozások. Órás route key: `wear193:<sha256>`,
  setmező: `gpsRouteKey193`; telefonos `gpsRouteKey163` külön megmarad.
- Tesztek: `tests/workout-ownership-193.cjs`, `wear-gps-bindings-193.cjs`,
  `wear-workout-lifecycle.cjs`, `tests/browser/wear-gps-route-193.cjs`, a natív
  `*193*Test` fájlok és `WearFullScreenTest`.
- Buktatók: spool → outbox lockrend megőrzendő; wirePayload immutable olvasásán
  ne vezess be visszafelé szinkronizált lockot. Android JVM tesztekben a
  `Files.writeString` bootclasspath miatt nem használható, `Files.write(bytes)` igen.
  A split testharness a sectiont vagy létező kompatibilitási fájlt is kezelje.
- GPS korlátok: 32 pont/csomag, 12000 pont/route, 256 szakasz, 512 route-tétel;
  nincs automatikus tombstone-pruning. Tárolókorlát/hiba nem jelent hamis mérést.
- Régi kerek UI-bizonyítékok és a telefonos térkép egyik gyors screenshotja nem
  minden képnél mutatja a teljes képernyőt: a korai telefonos képen még splash
  animáció is látszott. Ez nem új blocker, de prezentációs képként ne használd;
  későbbi képhez várd meg a `#tpBoot` eltűnését.

## Kezdő üzenet a következő Worknek

> A fuloplevente1998/TrainPilot repó feat/193-202-watch-offline-gps ágán folytass.
> Először olvasd az AGENTS.md-t, aztán a docs/WORK_HANDOFF.md-t. A #213 draft
> #193/#202 alkalmazásforrása e4f4fa8, minden kapuja zöld, aláírt APK-pár kész;
> készülékes elfogadás még nincs. Először ellenőrizd a friss GitHub-állapotot,
> aztán a készülékes próbát/visszajelzést kezeld. A feltöltött külső UI/ötletanyag
> csak backlog: ne kezdd el implementálni, ne merge-elj és ne emelj verziót.
