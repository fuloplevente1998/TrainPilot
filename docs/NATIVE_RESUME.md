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

## Legújabb sablon/helyettesítés/időkeret – 2026-10-25

**b69eb56**: final main tp128Template v2 + szerep/needs/score/area data-only policy,
aktuális Room-katalógus és profiltól függő csere, különböző ID-k, min3, perc-kapacitás.
Meglévő programok nem íródnak át. Számolt max napi becslés + hátulról 3→2 sorozat
időadaptáció base és history után, súly megmarad; estimatedMinutes/workSets friss.
Lokalizált név/csere/ok az előnézetben, tényleges becsült perc/időkeret.
8 main JS-generátor-fixture +3 unit+1 UI új kód; **batch356 unit+37 UI nem futott**.
Default katalógus reprodukálva, XML/resource/Node/diff rendben; nincs Android/CI/APK.
Következő: bizonyított beginner practiced-kivétel, korábban teljesített teljes nap
időben beférő megtartása és hordozható history128.reductions. Teljes generátor/UI-
paritás és végső közös kapu még nyitott. PR #238 követi, main/élő telepítés STOP.

## Legújabb explicit Level2-receptblokk – 2026-10-25

**5bf0654**: közös NativeTrainingPrescription main tp128Rx/Level2 felülírásokból,
személyes/Coach előnézet ugyanazt használja. Level2 intermediate és gear guard,
paused pushup/weighted core, címkék, csak tényleges súlyzós trial kér kg-ot.
History128 effektív rx.loadType alapján örökli a súlyozott testsúlyos mozgás súlyát.
14 main JS referencia-fixture generálva és reprodukálva; Kotlin nem futott.
18 új unit+1 UI-kód: **batch345 unit+36 UI még nem futott**. XML/diff/fixture check.
Mainben nincs külön automatikus Level2-trigger; kézi előnézet/elfogadás az út.
Következő valós rés: a data-only katalógus korábbi napjai helyett tp128Template v2
szerepsorrend/gyakorlatok és szerepalapú helyettesítés/min3; utána időkeret/
előzmény-adaptáció. Nem teljes generátorparitás, CI/APK/telepítés továbbra STOP.

## Legfrissebb Coach variáció-bekötés – 2026-10-25

**432727e**: meglévő Coach 3 stabil alkalmas ajánlóból read-only variáció-előnézet,
profil/felszerelés/kizárás/kezdő/readiness/pain guard. Main tp128Rx recept, trial0,
explicit mentéskori teljes újraszámolás/hash/Room tx + NativeProgramEditor clone.
Saját/builtin program is új ID, parentProgramId/sourceType coach, változott
history128 entry törlés; napló és aktív program nem változik. **Mentés új programként**,
aktiválás külön Programok oldalon, a dialog ezt jelzi (main automatikus aktiválási
viselkedésétől eltér). Level2 teljes programupgrade még nyitott.
10 unit+1 UI új kód; **batch327 unit+35 UI még nem futott**. XML/resource/diff és
recheck-before-clone statikus ellenőrzés; nincs új Android/CI/APK/screenshot.
PR #238 frissítve. Wear #239 külön hotfix nincs itt átvéve, elfogadás utáni port nyitott.

## Legutóbbi review/javítás – 2026-10-25

**7143cef**: da65f66 utáni célzott forrásreview. A nehezebb fekvőtámasz-csere
közös eligibility-szűrést kapott; személyes elfogadás Room tx-n belül teljes
kanonikus katalógus/profil/history128 újragenerálást és draft/recept/bizonyíték
összevetést végez írás előtt. UI publikus/internal típusütközések javítva.
4 új unit-kód; **batch317 unit+34 Phone UI még nem futott**. XML/resource/diff és
mentés előtti forrássorrend ellenőrizve; nem build vagy készülékteszt.
Következő változatlanul: három stabil alkalom utáni külön Coach variáció/upgrade
előnézet és jóváhagyás, main-referencia alapján. PR #238 az állapotlap.

## Legújabb natív batch – 90 napos A/B előzményöröklés

`cd696e7c` `[skip ci]` kód: `NativeProfileHistory128`
Room `getCoachExerciseCandidates` 32-es keyset
oldalakon (max 20 oldal/gyakorlat), max négy
bizonyított `{programId,key}` forrással,
Coach közös `compatible` terhelés/egység/
variáció szűréssel, 90 nap/tombstone/finished.
Friss rossz/fájdalmas/hiányos adaton nincs
régi kezdősúly fallback. A teljes, igazolt
munkasúly/2-3 sorozat és felső ismétlés
öröklődhet, de nincs automatikus emelés.
`history128` az elfogadott új programba
kerül, bizonyítékhash + elfogadáskori
újraszámolás (stale preview tiltás).
Portable JSON/ZIP/Drive import már a Room
csere előtt validálja a max 4 különböző
source identitást. A Planner aszinkron
előzménytöltése UI-állapottal bekötve.

**12 új JVM unit + 3 meglévő Phone UI-teszt
aszinkron igazítás**. Összesen **313 unit
+34 Phone UI tesztkód, még nem futott**;
21 statikus ellenőrzés PASS; Kotlin/Gradle,
Room, emulátor, APK és Watch7 nincs igazolva.
Következő: Coach 3 stabil alkalom utáni
variáció / explicit upgrade preview, majd
közös Android/Wear/upgrade CI. PR #238.

## Legújabb natív batch – személyes A/B tervező előnézet

`559fd43e` `[skip ci]`: a Programok Személyes
Edzéstervező bannerje már valódi HU/EN/DE/RO
előnézetet nyit. `NativeProfileProgramPreview`
a main elfogadott `home-basic/home-level2`
sablonjára és explicit edzésprofil perc/gear/
excluded/avoidAreas mezőire épít. Felszerelés
vagy edzésidő nélkül, illetve ismeretlen
kerülendő testrész mellett nincs bizonytalan terv.
Kezdő-szűrő, 20 perces rövidítés, B-összetett
sorrend, csak pad mellett nehezebb fekvőtámasz;
2-3 sorozat, 120/75 mp pihenő, 2 RIR,
`trial:true` súlybeállítás — automatikus
súlykitalálás nincs. Az előnézet nem módosít
adatot. Elfogadáskor új kanonikus Room
programpéldány `profileSnapshot/prescriptions/
sourceTemplateId/templateVersion:2` mezőkkel,
stale-CAS + restore fence és külön aktív
programváltás; sablon érintetlen, Wear publikáció.
Profil tömbök most szerkeszthetők.

**15 új unit +3 Phone UI, összesen 301 unit
+34 Phone UI kód megírva, még nem futott.**
20 statikus útvonalellenőrzés PASS, Android-
fordítás/emulátor/valódi kép/APK nem igazolt.
Következő: #128 90 napos `history128`
előzményöröklés + Coach-jóváhagyási küszöbök,
majd teljes CI. A mostani részparitást nem szabad
teljes személyes generátornak nevezni.
PR #238 frissítendő.

## Legújabb natív batch – főágbeli edzésprofil és Progress

`cfdc6d05` `[skip ci]`. A main #128 `settings.profile`
mezőjét natív DataStore + JSON/ZIP/Drive import/export
úton megőrizzük. Edzésprofil szerkesztő HU/EN/DE/RO:
fő scalar mezők, hiányzó testsúly/magasság nem
kitalált; gear/excluded/avoidAreas/extra JSON megmarad.
Restore-fence + stale-CAS, malformed profil még Room
csere ELŐTT visszautasítva. A személyes A/B
programgenerátor algoritmusát ez **még nem** pótolja.

Progress: teljes `getAllWorkoutsWithDetails()`
megszűnt; 30 napos Room izomterhelés-aggregátum
és egy kiválasztott gyakorlat 1RM adatai
256-os keyset-lapozással, tombstone-szűréssel,
teljes 106+ gyakorlat chip, négy nyelv és load-error.
**15 unit +3 Phone UI tesztkód**, összesen **286 unit
+31 Phone UI megírva, még nem futtatva**. 17 statikus
bekötési check PASS; nincs Kotlin/Room build,
emulátor/1500-as futás vagy APK-igazolás.
PR #238 frissítve; tovább profilvezérelt generátor/
Coach/progresszió, majd összevont natív CI.

## Legújabb natív batch – program- és programnapszerkesztő

`87f9ee10` `[skip ci]`: meglévő Room `programs` és
`program_days` táblákhoz natív UI/szerkesztő bekötve:
saját program CRUD, beépített program csak klónozható,
napok és gyakorlatok felvétel/törlés/sorrend,
katalógus alapú választó, HU/EN/DE/RO.
Stale snapshot-CAS, aktív session és naptár/napló
hivatkozásvédelem; üres nap nem indítható.
A kanonikus JSON/ZIP/Drive mentésben a napok extra
és fordítási mezői megmaradnak, explicit importlistáról
törölt napok nem állnak vissza.
**12 unit +3 Phone Compose UI-tesztkód: batch 271
unit+28 UI még nem futtatva.** 15 statikus check PASS;
nincs új Kotlin/Gradle/emulátor/APK igazolás.
Következő: profil/progresszió maradék, majd összevont CI.
PR #238 frissítendő.

## Legújabb natív batch – saját gyakorlat CRUD

`a46a8bcd` `[skip ci]` appforrás: saját gyakorlat létrehozás,
négy nyelvű szerkesztő és megerősített törlés a natív
Gyakorlatkönyvtárban, a meglévő Room és kanonikus rawJson
adatútján. 106 builtin nem módosítható. Egyedi stabil
custom-UUID, validált típusok/sorozat/súly/YouTube watch,
minden örökölt többi adatmező és másodlagos izomcsoport
megőrzése; stale rawJson-CAS. Törléskor aktív session,
napló- és programnapi hivatkozás tilt; kedvenc csak
sikeres törléssel együtt távolítható el.
**11 új unit +2 Phone UI kód, összesen 259 unit+25 Phone UI
megírva, még nem futtatva.** Statikus szerkezeti check
rendben, Gradle/emulátor/APK nem igazolt. Következő:
programnap-szerkesztés, profil/progresszió, majd közös kapu.
PR #238 frissítve.

## Legújabb natív batch – 106 gyakorlat és video

`d5222a32` `[skip ci]`: a mainből pontosan átvett
`native-catalog-main-7473.json` alapján 106 gyakorlat,
**100 konkret YouTube videoId +6 nem ellenőrzött keresés**.
Edzés → Gyakorlatkönyvtár új natív route (Room, négy nyelv,
keresés/izomcsoport, kedvencek, leírás/sorozat/részletek,
video-eredet); a gyorsedzés választóban is videó/keresés
gomb. Csak whitelistelt YouTube link, felhasználói tap után
külső ACTION_VIEW, nincs WebView vagy nem létező videó;
régi beépített import videófallback, saját felülírás nem örököl
ismeretlen linket. 7 unit +3 Phone UI-próbakód, navigációs
teszt bővítve. **Batch 248 unit +23 UI kód, egy sem futott**;
katalógus/statisztikus útvonal ellenőrzött, Android/CI/APK
nincs. Következő saját gyakorlat CRUD, programnap-szerkesztő,
profil, teljes kapu. PR #238 frissítve.

## Legújabb natív batch – v144 kedvencek

`4159a76` `[skip ci]`: a main `exerciseFavorites` tömbje validáltan
Room kv_documents-ben, migrációs key import, canonical
JSON/ZIP/Drive roundtrip, restore/erase kizáró adatkötés,
korábbi alias olvasás és replaceAll-tisztítás, Drive konfliktus-
választás. A gyorsedzés gyakorlatválasztója kedvenc szűrőt/
csillagozást kapott négy nyelvű akadálymentes felirattal; a
csillagozás nem indít sessiont. **10 JVM unit +1 Phone UI kód:
241 unit+20 UI megírva, még nem futott.** Teljes katalógus/
részletoldal/egyedi gyakorlat és program/profil szerkesztése még
nyitott. Statikus referenciák igazolva, Android CI/APK nincs.
PR #238 naprakészen tartandó.

## Legújabb natív kódbatch — HC/BLE háttérfeltételek

`f90e7ef` `[skip ci]`: valós háttér-HC (olvasás/külön háttérengedély/
U extension + JobScheduler) és RDFit BLE (mentett óra/Connect/notification/
Bluetooth/FGS) feltételek, replacement/restore-fence, explicit permission
request a kapcsoló után. A UI nem téveszti össze a mentett opt-int a futó
szolgáltatással. Sikertelen mentés/start esetén előző beállítás vissza +
látható hiba; négy nyelv 14 új string.
10 új JVM unit+2 Phone Compose UI-próbakód. **Batch 231 unit+19 UI
megírva, még nem futtatva.** Statikus forrás/resource-ellenőrzés megtörtént;
nincs új CI/APK/fizikai igazolás. Következő: profil/program/kedvenc paritás,
további A–E és közös tesztkapu. PR #238 követendő.

## Legfrissebb folytatás — HC egyedi eredet

`031f0a60` `[skip ci]`: ténylegesen eltárolt HC-origin
választása az Auto/Priority mellett, max100 rendezett/deduplikált,
érvénytelen és saját eredetek nélkül, korábbi eltűnt választás megőrzésével.
Nincs új SDK vagy automata újraolvasás; napi összesítés a kézi Frissítésig
partial. 2 új unit +1 natív Phone UI-próbakód; **összesen 221 unit +17 UI
kód, még nem futott**. Nincs friss APK/CI/készülékes bizonyíték.
Következő: egészség háttér-/engedélyállapotok, aztán A–E funkcióleltár
további profil/program/gyakorlat/kedvenc részei. PR #238 frissítendő.

## Legutóbbi konkrét munka

- Legfrissebb appforrás **b55df92**: napi HC aktivitás Auto/Priority UI a meglévő
  hcDailySource-policyval; metadata admission/mentési hiba, tárolt régi értékek
  partial figyelmeztetése explicit provider-frissítésig. Egyedi origin-választó
  még nyitott, korábbi érték megőrizve. HU/EN/DE/RO, 2 unit+1 UI új kód;
  **batch219 új unit+16 UI nem futott**. XML/resource/diff rendben. PR #238 követi.

- Aktuális appforrás **606b8c0**: Egészség egyedi mérés/alvás részletpanel a meglévő
  HealthJournalStore-ból, 25 rekord/SQL típus+eredet/nap/csatorna, idő+ID kurzor,
  max200 eredet, napi összesítés nélkül; régi Java/Kotlin page API megőrizve.
  Saját source/channel/start/end/modified; HC ms+numerikus és Samsung ISO+név
  alvásfázisok, 20-as minta/szakasz oldalak. Hiány nem helyettesül aggregátumból;
  fázis alapú vs. rögzített idő külön. Generation/pendingRestore+metadata gate,
  adatcsere után régi sor nélküli hiba és explicit újratöltés; HU/EN/DE/RO.
  16 unit+3 UI új kód; **batch217 új unit+15 UI nem futott**. XML/diff és célzott
  SQLite szűrés/63 azonos idejű kurzor ellenőrzés, nem Android/Kotlin próba.
  Teljes ág követhető draft állapotlapja **PR #238**, részcommitok után frissítendő:
  https://github.com/fuloplevente1998/TrainPilot/pull/238
  Következő: teljes A–E leltár további program/profil/kedvenc/szerkesztő,
  Health forráspreferencia/engedély, UI/lokalizáció/performance/backup nyitott pontjai.

- Aktuális alkalmazásforrás **e3e79d3**: közös `weight_journal` Egészség/Fejlődés
  felől, meglévő Room weights, dátumos add/edit/megerősített exact-ID delete.
  Metadata→Room tx→eredetisor-egyezés, rawJson ismeretlen mezők megőrzése;
  törölt/stale mérés nem éled újra a szerkesztőből. Sikerig nyitott panel/retry.
  Testsúly külön VM, írás nem tölti újra az összes edzést. Meglévő weightRange
  DataStore, 365 nap is; HU/EN/DE/RO. 13 unit+2 UI új kód, **batch201+12 nem futott**.
  XML/21 kulcs/hivatkozás, SQLite exact-ID és diff ellenőrzés, nem Android-teszt.
  Teljes Fejlődés/súlytörténet bounded betöltése és felhős súlytörlés replay még nyitott.
  Következő: Health nyers oldalak/eredet/mérési idő/alvás, majd további leltár/közös CI.

- Legfrissebb egészségblokk **edc8f52** + BLE-csatorna konstansjavítás: Egészségnapló hónap/nap/csatorna
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
- **219 új unit + 16 instrumentációs tesztkód megírva, még nem futtatva.**
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
