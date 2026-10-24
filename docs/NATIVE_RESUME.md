# Natív folytatási pont – rövid kontextus

Frissítve: 2026-10-24. Kontextustömörítés/új chat után ezt és az AGENTS.md-t,
majd a WORK_HANDOFF.md legfelső aktuális bekezdéseit olvasd. Ne töltsd újra az
összes történeti naplót vagy a Gemini-exportot; csak az érintett blokkokat keresd.

## Hatókör és munkamód

- Repo fuloplevente1998/TrainPilot; munkafa `/workspace/TrainPilot-native-migration`;
  kizárólag `feat/native-android-migration`.
- Teljes elfogadott feladat: `docs/plans/NATIVE_FINAL_DELIVERY.md` és batch terv.
  Teljes appfunkció/bekötés + 24 nézet + témák + HU/EN/DE/RO + tesztek + végső
  valódi emulátoros képek. Nem csak témázás; nincs új generált mockup.
- Előbb teljes kód/tesztkód; köztes commit `[skip ci]`. Utána közös GitHub CI,
  nem helyi Gradle/emulátor. Nincs main merge, verzióemelés vagy élő adatos telepítés.
- Verzió `1.3.71-native-test` /2736, com.repforge.app mindkét eszközön.
- High: kód/bekötés; dizájn előtt jelezni Mediumhoz; CI-hiba/komoly debug előtt
  jelezni Extra Highhoz. A felhasználó kevés, lényegi kommunikációt kér.
- Nem indítható subagent külön kérés nélkül. Meglévő Kotlin/kanonikus adatút az alap;
  prototípus/fake Health-forrás tilos. Nyolcgombos menü helyett öt alsó fül elfogadva,
  jobb felső Settings; abból visszatérés nem pause, valódi editor-elhagyás igen.

## Legutóbbi konkrét munka

- Legfrissebb alkalmazásforrás **edc8f52**: Egészségnapló hónap/nap/csatorna
  választás, max31 napos meglévő HealthJournalStore oldal, kiválasztott hiányzó nap
  nem helyettesül régebbi adattal. SDK-frissítés a kiválasztott napra; Coach továbbra
  mai napot kér. NativeHealthMetrics mind26 provider-kulcsot megjeleníti, négy nyelvű
  címkékkel, véges szám/nincs-adat elkülönítéssel. 7 új unit+1 történeti testsúly UI
  tesztkód megírva, nem futott. Kódolás/High jelzés elhangzott.
  Nyers mérések kurzorlapozása, pontos eredet/mérési időbélyeg, alvásrészletek és
  további egészség/testsúly szerkesztési paritás még hátra van; nem teljes készség.

- Friss felhasználói dizájnszabály: teljes main-funkciókör megőrzendő kevesebb,
  összevont oldallal. NATIVE_DESIGN_HANDOFF.md A–E funkcióleltár/új elhelyezés
  táblázatokkal bővült. Kedvencek is main-funkció (v144), pótlandó a mockupból.
  Ez követelménylista; minden funkció tényleges natív útvonala/adatforrása/tesztje
  külön igazolandó. Nincs új kód/CI ebben a dokumentációs körben.

- Korábbi katalógusforrás **033386e**: pontos mainből kinyert106 gyakorlat,
  13 beépített program, teljes név/leírás HU/EN/DE/RO. Adat-only Java resource,
  natív appban nincs JS/WebView futtatás. Hiányzó elemek pótlása, IGNORE+Room tx+
  metadata admission, meglévő egyedi/korábbi natív sorok és programnapok megőrzése.
  Import hiányzó builtin mérési típus/fordítás pótlása; 7 új unit+1 UI próbakód,
  még nem futtatva. Referenciakinyerés reprodukálva, nem Android-eredmény.
- A felhasználó külön kérte a négy dizájntábla újratervezését. Új24-képernyős
  review-csomag: docs/design/NATIVE_DESIGN_HANDOFF.md + mockups/ négyPNG ésmanifest.
  Fogaskerék/pihenő/preset/téma/státusz pontosítások. Generált design, nem futóapp;
  az új változat reviewra vár. Most vizuális fázis (Medium jelzés elhangzott).

- Korábbi Coach-forrás **ccc5c9a**: NativeCoachExerciseAdvice + bounded
  NativeCoachAdviceRepository és Coach-kártyák a végső main tp128/rf233 szabályokból.
  90 nap / max4 kompatibilis előzmény, 32-es keyset oldalak, elfogadott history128
  forráshivatkozás, 128-as distinct súlylapok; hiány/részleges/fájdalom/RIR/romló
  trend/stabil növekedés/testsúlyos változat és readiness korlát. Tanács nem ír
  aktív edzést/programot/draftot. ProgressionSteps hordozható DataStore + backup.
  HU/EN/DE/RO erőforrások; 32 main-referencia unit-eset +6 Room +1 portable unit,
  +1 read-only UI próbakód. Android/Kotlin tesztek nem futottak.
- Referencia: `git show 7473b30:www/app.js`, blob **b5567576bed9a8f93608a6bf1ed8db1dee4723f4**.
  A `/workspace/TrainPilot` checkout local-release-136, nem aktuális main; azt nem
  módosítottuk. `scripts/native/coach-reference.cjs` pontos blobot ellenőrizve
  32 szintetikus JS-helyzetet futtatott és reprodukálható fixture-t generált.
  Ez nem Kotlin/Android teszteredmény. A 29ac3a2 az előző alkalmazásforrás.

- Korábbi alkalmazásforrás **29ac3a2**. A természetesen lejárt nem-auto interval
  terv nyugdíjazva: kézi OK/újraindítás és régi tick kizárása működési útba kötve;
  inaktív elvetett frame régi receiptje megmarad, más workout returnjét nem blokkolja,
  ugyanaz a lezárt workout nem támasztható fel. 3 új unit-próbakód, nem futott.
- **036a2cd**: közös NativeHealthRefresh a már meglévő Samsung/HC adapterrel,
  foreground-guard/observer cleanup és metadata admission. Coach közvetlen frissítő
  gomb + kezelt hiba, új pontszám/adat nem kitalálva. NativeCoachTargets átveszi
  a korábbi VM-blokkot; SQL julianday tényleges időrend, lokális nap osztályozás,
  lapozott skip a cancelled/hiányzó nap/program felett. 10 unit + 1 UI-próbakód.
- Copilot ágellenőrzés: feat/native-android-finalization **6a21fc1**, a saját
  **036a2cd** forrásból +2 környezet/CI commit, 7 infrastruktúra/dokumentáció fájl;
  appkód nem változott benne. Main **8a36502** (csak korábbi AGENTS dokumentáció).
  Egyik idegen ágra sem írtunk, nem vettük át a Copilot környezet/build scriptjeit.

- **9ea2ddf** intervallumblokk: meglévő motor nem-autoAdvance módja csak egy aktuális
  sorozatot mér, pozitív munkaidőt megőriz, nem készít/nem léptet; auto mód megmarad.
  Változatlan futó terv mellett edzéskonfig és explicit globális preset; main
  intervalPreset110 migráció/export és legacy draft interval110 megőrzése.
  24h/typed validáció; négy nyelvű opcionális panel, 9 unit + 1 UI-próbakód új.

- **6af1d79**: Phone discard a közös finalizer/mutex és metadata admission
  alatt; tartós finalizationPending+discardPending, pause/stop, Room tombstone+draft
  retirement tranzakció, ellenőrzött natív discard, csak utána closure/navigáció.
  Hiba után explicit discard retry; finish nem mentheti az elvetésre váró edzést.
  Bodyweight admission/hibakezelés; Settings/programváltás gate-elutasítás kezelve.
  8 új unit-hibapróba + 1 valódi Compose discard-fault/retry tesztkód; nem futottak.

- Korábbi metadata-forrás **0f51320**: a replacement mutex író-admissionnel
  védi a Wear parancsot, közös finisht, import/export/egyedi history save-et,
  DataStore set/resetet, naptári VM-módosítást és naplótörlést. Context-lease a
  beágyazott saját callbackhez, epoch a lecserélt állapothoz tartozó várakozóhoz.
  Admission előbb, utána Room/finalizer lock; reentrancia csak érvényes lease-szel.
  ZIP touched-journal és eredetifotó-replay javítva. 5 gate + 2 ZIP próbakód.
- Alkalmazásforrás **369a4d0**: Drive merge után canonical tartalmi újraellenőrzés,
  helyi replacement lease és meglévő staged redo import. Hálózati szakasz alatt
  nem tartja az edzésindítás-zárat. Módosult helyi tartalomnál nincs stale import.
  A felhőbe írandó merge koordinátaszűrést kap. Új3 unit-próbakód.
- Előzmény **851391c**: Room import egy tranzakció; utána DataStore egy edit;
  hordozható beállítások/draft és teljes tombstone-backup; Health pending restore
  rendezése és fázis-injektálás. A 7296f0b privát hash-kötött forward-redo receipt
  startup/Settings recoveryval, nem teljes rollback. Hibánál félkész receipt mögött
  az app blokkolt; explicit folytatás vagy másik valid backup, nincs néma unlock.
- Korábbi batch: 32-es Paging napló + globális Unicode keresés + explicit részlet,
  privát fotó/több route UI, naptár sync 100-as limit nélkül, programnapi első kültéri
  aktivitás GPS-választása; teljes main palettakatalógus + kontraszt/picker.
- Phone UI kapu alapja **1d38edf**: külön native AndroidTest source set,
  meglévő native-android.yml phone-ui job, raw adb screencap checkpoint/ack és
  származtatottnak jelölt XML/manifest. Az akkori4 óta8 instrumentációs próbakódra bővült.
- **188 új unit + 10 instrumentációs tesztkód megírva, még nem futtatva.**
  Nincs új zöld kapu/APK/screenshot. Utolsó teljes zöld forrás **dd4cc27**:
  unit Phone393/Wear167, Wear UI183, signed upgrade2. Bizonyíték: WORK_HANDOFF.md.
- Main utóbbi 8a36502d változása csak AGENTS thinking-dokumentáció; main app baseline
  továbbra elfogadott 1.3.71-perf-20261022 (5d27aa9 / app715dc5d), nem régi buildre portolunk.

## Következő feladatok – nem lezárt részek

1. A még szabadon futó egyéb írók admission és UI hibakezelése;
   cloud photo staging/transaction. A bekötött kapu nem minden szabad DAO és külön
   Health SQLite írás globális zárja. A megírt ZIP/Health tesztek még nem futottak.
2. Intervallum/preset/Coach új kód futtatott igazolása még nincs; teljes katalógus,
   nevek és lokalizáció, program/testváltozat-előnézet további paritása. A közös provider-frissítés új
   forrása/életciklustesztje még futtatandó; valós SDK/készülék nincs igazolva.
3. Mockup teljes vizuális kidolgozás (előtte Medium jelzés): többek közt mini-route
   listathumbnail, részletes Health forrásadatok, program/gyakorlat szerkesztés/paritás.
4. Kritikus Phone UI suite bővítése GPS/kétoldali/finish/discard/undo/retry/backup/
   0-200-1500 mintára és minden screenshotállapotra; végső azonos forrású CI és APK-pár.
5. Valódi Phone/Watch7/Health és kétkészülékes offline próba külön; emulátor nem igazolja.

A dizájn hat kötelező motorfeltétele a `docs/design/NATIVE_MOCKUP_REVIEW.md`-ben.
Minden dátum/eredmény forráshoz kötött: a kód megléte nem sikeres futtatás.
