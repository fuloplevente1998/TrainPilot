# TrainPilot – aktuális átadás

Frissítve: **2026-10-20**. Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## Folyamatban – #214 repository-karbantartás

- Felhasználó 2026-10-20: GitHub-verziók, leírások és karbantartás engedélyezve.
- Ág: `maintenance/214-repository-hygiene`, alap main `a3a4ddce2250c32ef5b55e9ed8733b6a3aa85bda`.
- Angol + magyar README, 37 release dátum/platform szerinti jegyzéke, friss családos változáslista és Play-checklist, proprietary csomagjelölés.
- Új verzióegyezési ellenőrzés és célzott tesztek; csak leírásfejlécet módosító release-karbantartás, védett eredeti szöveg/tag/melléklet-metaadatokkal.
- Helyi célzott teszt: 8/8; verzió 1.3.6 /2733, eredeti kiadási feljegyzések és alkalmazás-/natív forrás változatlan.
- CI és a GitHub-leírások tényleges frissítésének eredményét a karbantartási Draft PR követi. Nincs új APK, verzióemelés vagy Play-publikálás; main merge még nem történt ehhez a karbantartáshoz.
- #214 nyitva marad a [rögzített külső Play/Samsung/aláírás/licenc feladatokkal](MAINTENANCE_214.md).
- A felhasználó #184/#197 main engedélyét 2026-10-20-án újra megerősítette; GitHub szerint mindkettő már completed, #222 merged `f413a3a`.

## Elfogadott alkalmazás – #222 mainen, funkcionális kör lezárva

- Felhasználói elfogadás/lezárás 2026-10-20 17:12 (Europe/Budapest):
  „Zarhato ez is, igy akkor kb minden mar nem ?”
  Ez a legutolsó Samsung/Regeneráció-javítás elfogadása és a korábban feltételesen
  jóváhagyott RIR utáni #222 integráció lezárási pontja; nem kitalált részletes mérési napló.
- **#222 merged mainre: `f413a3a475cf31a4e596429fe1c8dcf3e1e135df`. #184 és #197 completed.**
  Main merge fa `791db4a8608a27bdcbfc31490a8cbf427b3fd307`, byteazonos a PR-head fájával.
  A korábbi dokumentációs eltérésen túl a bevizsgált alkalmazásforrás változatlan:
  `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`.
- Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A végső dokumentációs HEAD Quick
  **39441673066** és Performance **39441673033** is sikeres.
  A merge után automatikusan induló main futások külön futások; eredményüket nem találjuk ki.
- Verzió **1.3.6 /2733**, Wear változatlan. Nem történt verzióemelés vagy új release.
  A már telepített, elfogadott [telefonos APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379)
  alkalmazásforrása azonos a mainre került kóddal.
  [CI/APK bizonylat](builds/2026-10-20-222-recovery-tiles.json);
  [lezárási bizonylat](builds/2026-10-20-222-accepted.json).
- **Egyetlen nyitott issue: #214**, karbantartás és Google Play-előkészítés.
  A Samsung nyilvános/production partner-hozzáférés ellenőrzése is ott marad;
  saját készülékes siker nem partnerregisztrációs igazolás. Nyitott PR nincs.
- A jelenlegi funkcionális fejlesztési sor lezárult. A dizájnmódosítások lehetnek
  a következő külön feladat; még nem kezdődtek el. Rögzített ötletek:
  [UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md), külön későbbi tesztágra.

## Történeti #222 – Samsung-pontszám olvasható, Regeneráció-csempe egységesítése

- Felhasználó 16:24-es képe **sikeres készülékes Samsung-olvasást igazol** az e97fe1e APK-n.
  Ez nem teljes PR/main elfogadás és nem külön egyezésigazolás a Samsung Health kijelzésével.
- Kérés: a Regeneráció csempe is ugyanazt a mai külső pontszámot mutassa.
  A csempe eddig friss saját alvás/HRV rekordhoz kötötten, Coach-korláttal számolt.
  Most a hiteles mai Samsung-pontszámot mutatja Home/Egészség oldalon akkor is,
  ha külön alvás/HRV-rekord nincs. Külső adat hiányában a korábbi saját becslés marad.
- Közös egész számú kijelzés: a képen látott SDK-tizedes érték 55/100-ként jelenik meg.
  A nyers SDK-pontszám és a Coach fájdalom/terhelés védelmei változatlanok.
  Kártya/csempe/Coach forrássor azonos formázást használ. Natív és Wear kód érintetlen.
- Az új teszt elkapta a nyitott panel mögött elmaradó csempefrissítést; javítva csak
  a read-only pontszám közvetlen DOM-frissítésével, panel/draft/scroll megőrzése mellett.
- **Végső alkalmazásforrás:** `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`;
  PR-merge fája is `8332fd6ffa21d0281af7bc5f702f928ecdcef6e7`.
  Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**. Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A Samsung-teszt egész számú kártya/Health/Home
  csempe-egyezést igazol frissítéskor, nyitott panel és külön alvásrekord hiánya mellett.
- **Telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379),
  1.3.6 /2733, SHA-256 `3798a8e679f9a5a16b013be774f918e62eca24e0070b36084a7d2e1349cf5e28`.
  app.js/index.html byteazonos, forrásjelölés/SHA256SUMS/aláírás és signer-folytonosság helyes.
  [Bizonylat](builds/2026-10-20-222-recovery-tiles.json). Wear APK változatlan.
- **Következő:** új APK-n csempe/kártya egyezésének készülékes visszajelzése.
  #222 Draft, main/verzió változatlan, nincs merge/kiadás/issue-lezárás.

## Korábbi #222 – 1001-es napi lekérdezés javítása

- Felhasználó 16:07-kor az új APK képernyőképét küldte: **SAMSUNG_HEALTH_1001**.
  Samsung dokumentáció: ERR_INVALID_INPUT (érvénytelen bemenet); nem igazolt partnerhiba.
- Konkrét kódhiba: a napi startDate és endDate azonos dátum volt, mindkettő inclusive.
  A valódi Samsung SDK ezt azonos éjféli kezdő/végű LocalTimeFilterré alakítja.
  Javítás: pozitív [mai éjfél, következő éjfél) helyi dátumintervallum; a holnapi pont
  továbbra is kizárt. Nincs másnapi vagy saját becslés Samsung-pontként felcímkézve.
- Natív teszt a tényleges buildelt SDK-kérés LocalTimeFilterét vizsgálja; 1001-et okozó
  nulla hosszúságot minden energiaolvasó teszt elutasít. Új regresszió 23/24/25 órás
  budapesti napokra és a holnapi éjféli pont kizárására. Korábban a mock ezt nem nézte.
- **Alkalmazásforrás:** `e97fe1e31a824640ea78c1cf39b45c1c572456db`;
  Quick/Performance PR-merge fa is `d5f7d46679560ee44fcf7615d66c393b9ad25a0c`.
  Quick **39436835835: 154/154**, Performance **39436835798: SUCCESS**,
  Phone Gate **39436826354: SUCCESS**. Chromium **6/6, 105/105**, letöltött JSON-ok
  forrása/duplikációmentessége ellenőrizve. Natív XML **249/249, 0 failure/error/skipped**;
  valódi SDK-szűrő/DST regresszió és a régi nulla hosszúságot igazoló teszt sikeres.
- **Új telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39436826354/artifacts/12271587790),
  1.3.6 /2733, SHA-256 `52eecad1878da78d20d773cb35193ed27482c6464cb14215de75e8c499b98fc8`.
  app.js/index.html és a forrásarchívum natív kódja byteazonos, forrásjelölés és SHA256SUMS
  helyes; apksigner és eredeti signer-folytonosság sikeres.
  [Bizonylat](builds/2026-10-20-222-samsung1001.json).
- Forrásváltozás csak telefonos Samsung-olvasó és natív teszt; felület/Wear/verzió azonos.
  **Következő:** új APK telepítése, Regeneráció → Samsung-pontszám frissítése;
  a mai Samsung-érték egyezésének készülékes visszajelzése. Készülékes siker még nem
  igazolt; #222 Draft, main `1393982d`, nincs merge/verzióemelés/issue-lezárás.

## Korábbi #222 – Regeneráció alá rendezés

- Felhasználó az új APK-ról képet küldött: Samsung Healthen telefonon/órán van mai
  pontszám, TrainPilot az általános „olvasás nem sikerült” állapotot mutatja.
  **Ez sikertelen készülékes teszt**, a korábbi zöld CI nem igazolja a Samsung-hozzáférést.
- Kérés szerint a Samsung-kártya a Regeneráció megnyitható részébe került;
  külön felső szintű kártya nincs. Frissítés után a panel nyitva marad.
- Az önálló olvasás állapotellenőrzése is kizárólag ENERGY_SCORE engedélyre kérdez;
  más adattípus nem akadályozhatja. Natív SDK-hibakód megőrizve aszinkron és belső
  platform-kivételen át; részleges olvasásnál is strukturált mezőhiba jut a felületre.
- A felület külön jelzi a 2000 engedélyhiányt, 2003 app-hozzáférési korlátot,
  3001 frissítési igényt és 3000/3002/3003 Samsung-beállítási akadályt. Nyers
  kivétel/adat nem kerül ki; diagnózishoz hibakód látszik. Hiánynál marad a TP-becslés.
- Samsung hivatalos API-feltétel: csomagnév + release aláírás + engedélyezett
  adattípusok partnerregisztrációja kell normál felhasználói hozzáféréshez.
  https://developer.samsung.com/health/data/process.html
  https://developer.samsung.com/health/data/api-reference/-shd/com.samsung.android.sdk.health.data.error/-error-code/index.html
  A konkrét készülékhibát kód nélkül **nem** minősítjük biztos partnerhibának.
- **Bevizsgált alkalmazásforrás:** `ae82d481b31d8d01394ce41d08ef003c8847d0c1`.
  PR-merge fája is `4056ab3898c1386ea61969c330c06287b612380f`, azonos alkalmazásforrás.
  Quick **39423620810: 154/154**, Performance **39423620964: SUCCESS**.
  Phone Gate **39423616111: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **248/248, 0 failure/error/skipped**. Új engedélyhatár- és kivételláncteszt sikeres;
  Samsung Chromium: Regeneráción belüli kártya, nyitva maradó panel és hibakód rendben.
- **Új telefonos teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39423616111/artifacts/12264688934),
  1.3.6 /2733, SHA-256 `49426605ba22046bdba3051004528e70814c0b62de9714c372c488ef67ff752d`.
  APK app.js/index.html byteazonos, forrásjelölés és SHA256SUMS helyes, signer
  az eredeti v1.0.0-val egyezik. Wear változatlan. [Bizonylat](builds/2026-10-20-222-recovery.json).
- **Következő készülékes próba:** Egészség → Regeneráció → Samsung energiapontszám,
  egyszeri frissítés/engedélyezés; ha hiba marad, a konkrét hibakód szükséges.
  Ez a build az adatolvasás és hibaazonosítás javítása, nem igazolt készülékes megoldás.
  #222 Draft, main/verzió változatlan, issue-k nyitva.

## Korábbi #222 – teljes CI sikeres, teszt-APK kész

- Integráció: `feat/184-197-coach-integration`, **#222 nyitott / Draft**.
  **Bevizsgált alkalmazásforrás:** `a8bec13630136d89128f259bd065f6c2f57b2cea`.
  Quick/Performance PR-merge fája byteazonos: `889b18da87d94f3dc38cbb37b9629ecab2927e71`.
  A későbbi dokumentációs commit nem új alkalmazásbuild.
- **Quick 39391181566: 154/154; Performance 39391181636: SUCCESS.**
  **Phone Release Gate 39391176400: SUCCESS; Chromium 6/6, 105/105**,
  mind a 6 letöltött shard-JSON pontos forrásra és duplikáció nélkül ellenőrizve.
  Natív XML: **246/246, 0 failure/error/skipped**, benne a Samsung SDK olvasó 8 esete.
- **Telefon teszt-APK: 1.3.6 /2733**, [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39391176400/artifacts/12249603397).
  SHA-256: `0352cbfc68b6f46dc402f82a267878bc1f0aaec2035e3514e782606c2bf7fb80`.
  A letöltött APK app.js/index.html byteazonos a bevizsgált forrással; source stamp helyes,
  apksigner és eredeti v1.0.0 signer-folytonosság rendben. Wear forrás/APK változatlan.
  [Ellenőrzési bizonylat](builds/2026-10-20-222-rir-samsung.json).
- **#197:** opcionális valódi 0–10 RIR, közvetlen számmezőn; azonnali draftmentés
  újrarajzolás nélkül; hibás szerkesztés nem hagy korábbi mérést. Reload, edzéslezárás,
  a tényleges lazy Napló és a következő konzervatív Coach-ajánlás E2E sikeres.
  A cél-RIR nem mérés, hiányzó cél nem implicit 0; nincs automatikus súlyátírás.
- **#184 felhasználói hiba:** Samsung Healthben van pontszám, TrainPilotban nem látszik.
  Javítás: külön napi ENERGY_SCORE olvasás, külön engedély, a lépés-/kalóriaforrástól
  független automatikus frissítés; mai hiteles Samsung-pont elsődleges, hiány/hiba/
  engedélymegvonás esetén saját becslés. Nincs kettős pontozás, fájdalomkorlát megmarad.
  Az Egészség oldalon adat nélkül is látszik a pontszámkártya és az ok/frissítés.
  Az önálló olvasás nem írja felül a teljes Samsung-napi rekordot. Éjfél kizáró végpont javítva.
- **Felhasználói felhatalmazás:** „ha a RIR javítás bekerül, mehet mainre” feltételes
  jóváhagyás, majd kifejezett #222 feltöltési engedély. Ez nem sikeres készülékes
  Samsung-tesztbeszámoló. A közben jelentett pontszámhiba új APK-n még visszaellenőrzendő.
- **Következő:** APK telepítés; Egészség → Samsung energiapontszám, szükség esetén
  egyszeri engedélyezés. Egyezzen a Samsung Health mai értékével; hiánynál fallback.
  Ellenőrizendő a pihenőidők megőrzése APK-frissítés/Drive-visszaállítás után és releváns
  telefon/Wear használat. A készülékes eredményt ne találd ki.
  Main továbbra is `1393982d`, nincs merge, verzióemelés, release vagy Issue-lezárás.
  Dizájnmódosítások a funkcionális lezárás után, külön későbbi feladatként jönnek.

## #216 lezárva, 1.3.6 mainen

Felhasználói engedély: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez lezárási/main/kiadási jóváhagyás; részletes fizikai tesztbeszámolót nem találunk ki.

- **#218 merged `2059770c0eadf9c9e66929635865861667404d41`; #216 completed.**
  Órás menü: aktív edzés/Gyors edzés elöl; Naptár/Programnapok közös csoportban.
  Új kültéri gyors/tervezett edzés előtt látható, aktivitásonként megjegyzett GPS-választás,
  futó edzésről közvetlen állapot/kezelés. Reconnect/render nem indít rögzítést.
  Kézi gyakorlatváltás megőrzi a pihenő eredeti határidejét és automata fázis/cél/tulajdonosát.
  Telefonos működés és GPS-adatút megmaradt. HU/EN/DE/RO.
- **#219 merged `98be66221430202715aafa9a71583bbbd22d4fd3`; 1.3.6 /2733.**
  Mindkét app metaadatai és appfelirat, SW cache, verzióelvárások és kiadási jegyzetek frissítve.
  Main teljes fája byteazonos a bevizsgált `76fa539` PR-head fájával:
  `d0f5487aa3338369c879126b7d2ca15cbbd4e3b3`.
- #216 alkalmazáskód `c04851f`; utána csak tesztjavítások. A home fixture törli az aktív edzést,
  ezért a felfelé gesztus után **Gyors edzés** az első látható menügomb, nem Napló/Folytatás.
  Az eredeti azonnali láthatósági, gesztus- és geometriai feltételek megmaradtak.

## Ellenőrzött kiadási forrás

- Quick **39231417696**, Performance **39231417699**: sikeres (`76fa539`).
- Telefon **39231412495**: Chromium **6/6**, JVM **243/243** (`76fa539`).
- Wear **39231317332**: JVM **159/159** (`7b600e4`).
- Ownership **39231417723**: **81/81**; kerek UI/platform **39231317394: 183/183**.
- Minden vizsgált XML: **0 failure/error/skipped**. Külön #216 kerek UI
  **39231315228: 183/183**; új GPS/pihenő/menü tesztek mind az öt kijelző/betűméreten.
- `7b600e4..76fa539` csak AGENTS/átadás/bizonylat; alkalmazás- és tesztkód byteazonos.
- Aláírás folytonos a nyilvános v1.0.0-val; signer SHA-256:
  `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [Bizonylat, APK-hashek és források](builds/2026-10-19-release-136.json).

## Végleges, publikált 1.3.6 APK-pár

Mindkét kiadás/tag forrása **`98be66221430202715aafa9a71583bbbd22d4fd3`**, 1.3.6 /2733:
- [Telefon APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.6/TrainPilot-1.3.6.apk),
  SHA-256 `4b3b2fd90b38912a7a51157ed18c337f361f5e3f5521df69d10a64697c4cc987`.
- [Wear APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.6/TrainPilot-1.3.6-wear.apk),
  SHA-256 `ac9d16066df7f63290bca20656bf883391e41bbd1399cde397b9292176233958`.

Main telefon **39233098299**: 6/6 UI, JVM XML 243/243; Wear **39233098238**: JVM XML 159/159;
0 failure/error/skipped. Mindkét apksigner és eredeti signer-folytonosság sikeres.
A letöltött main artifact APK-hashek egyeznek a nyilvános release asset digestjeivel,
a telefonos forrásarchívum és Wear SOURCE_VERSION egyaránt `98be6622`-t igazol.
Main Quick **39233098560**, Performance **39233098199** sikeres.
Kerek UI **39233098292** további automatikus main futás; az azonos teljes forrásfa
183/183 kapuja a fenti **39231317394** futáson XML-ben igazolt. Élő státuszát GitHubon ellenőrizd.
A későbbi dokumentációs HEAD nem új alkalmazásbuild; a két APK tényleges forrása a fenti commit.

## Külön maradó backlog / lezárt előzmények

- **#206**: maradék aktivitásnézet/UI. Az explicit sikeres telefonos GPS „Mérés befejezése”
  külön sorozat-OK nélkül már elfogadott; térképbezárás/hibás/implicit stop nem teljesítés.
- **#197**: választható edzésstílusok/progresszió; **#184**: Samsung Energy Score;
  **#214**: karbantartás/Play. Az 1.3.6 ezeket nem zárja le.
- #193/#202 önálló órás GPS és telefonos naplótérkép + offline átadás/visszavétel elfogadva;
  #213 main `311f507`, #193 completed. Az órán nincs térképrajzoló: az óra rögzít, a telefon rajzol.
- #215 kompakt Intervallumok elfogadva/main `c67006b`; kétoldali UI nem engedélyez egyórás motort,
  két pozitív idő kell a kész állapothoz. #212 korona/lünetta #217 main `102eb366`, elfogadva.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen. #192/#211 sztornózott.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre engedélyezve,
  későbbi megvalósítás külön tesztágon. [Részletes #193 terv](wear/P2_193_ONALLO_ORAS_EDZES.md).
- Régi hosszú átadás: `885b6df:docs/WORK_HANDOFF.md` a Git-történetben; történeti státuszai elavultak.

## Környezet

CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
