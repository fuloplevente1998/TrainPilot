# Natív folytatási pont – rövid kontextus

Frissítve: 2026-10-24. Kontextustömörítés/új chat után ezt és az AGENTS.md-t,
majd a WORK_HANDOFF.md legfelső aktuális bekezdéseit olvasd. Ne töltsd újra az
összes történeti naplót vagy a Gemini-exportot; csak az érintett blokkokat keresd.

## Hatókör és munkamód

- Repo fuloplevente1998/TrainPilot; aktív munkaág `feat/native-android-finalization`.
  A `feat/native-android-migration` egyirányú, csak olvasható forráság; a `main`
  szintén érintetlen referencia.
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
- Az alábbi korábbi technikai checkpointok a migrációs ágon rögzített munkát írják le;
  az aktuális ág/CI/UI állapothoz a `docs/WORK_HANDOFF.md` és
  `docs/NATIVE_FINALIZATION_STATUS.md` a mérvadó.

## Legutóbbi konkrét munka

- A legutóbbi frissítéskor az egyirányú migration upstream `1e4783b` volt. A
  `edc8f52` Health-blokkja és a `1e4783b` BLE-csatorna-konstans javítása ellenőrzött
  cherry-pickként átkerült; a finalization saját Wear-/tesztjavításai megmaradtak.
- Finalization Home batch: `dee1199`. A kanonikus `NativeCoachTargets` szerinti
  ajánlott programnap indítható, az aktív/importált draft elsőbbséget élvez, és a
  Programok/Naptár/Coach gyorselérések megmaradtak. Home UI-próbakód hozzáadva.
- Health batch: `b7a82ef`, Egészségnapló hónap/nap/csatorna választással, kiválasztott
  hiányzó nap adatát nem helyettesíti korábbi érték, és a napi adatfrissítés a
  kiválasztott napra irányul. Mind a 26 provider metric kulcs megjeleníthető,
  lokalizált HU/EN/DE/RO címkékkel. BLE csatorna a kanonikus HealthJournalStore
  azonosítót használja. Kód/próbák még nem futottak Androidon.
- A branch jelenlegi appkódja és új Home/Health tesztjei még nincsenek CI-vel
  igazolva. Az utolsó ismert CI a `40119749853` (`8ead84a`) fordítási hiba;
  a `BackupRestoreManager`-beli `appContext` javítás már a Home batch-ben van.
- Teljes A01–E20 leltár (81 megőrzendő funkció) és a négy ellenőrzött tervlap:
  `docs/design/NATIVE_DESIGN_HANDOFF.md` és `docs/design/mockups/`. A leltár
  követelmény, nem teljesítési bizonyíték; pontos útvonal/adatforrás/teszt
  megfeleltetés, signed upgrade és fizikai készülékpróba még nyitott.

- A korábbi `40119749853` továbbra is az utolsó ismert CI-futás: a Kotlin fordítás egy `context`
  hivatkozás miatt elbukott. A munkafában az `appContext`-javítás megvan, de a
  CI/egységteszt/aláírt upgrade/API-APK futás nincs elindítva az aktuális forráson.
- `docs/design/NATIVE_DESIGN_HANDOFF.md` most a friss teljes funkcióleltárat és
  négy SHA-val ellenőrzött tervlapot tartalmazza. Ezek kifejezetten nem funkció-
  vagy tesztbizonyítékok; teljes ID-szintű lefedettség még nincs felmérve.

- Friss intervallumblokk: meglévő motor nem-autoAdvance módja csak egy aktuális
  sorozatot mér, pozitív munkaidőt megőriz, nem készít/nem léptet; auto mód megmarad.
  Változatlan futó terv mellett edzéskonfig és explicit globális preset; main
  intervalPreset110 migráció/export és legacy draft interval110 megőrzése.
  24h/typed validáció; négy nyelvű opcionális panel, 9 unit + 1 UI-próbakód új.

- Friss munkablokk: Phone discard a közös finalizer/mutex és metadata admission
  alatt; tartós finalizationPending+discardPending, pause/stop, Room tombstone+draft
  retirement tranzakció, ellenőrzött natív discard, csak utána closure/navigáció.
  Hiba után explicit discard retry; finish nem mentheti az elvetésre váró edzést.
  Bodyweight admission/hibakezelés; Settings/programváltás gate-elutasítás kezelve.
  8 új unit-hibapróba + 1 valódi Compose discard-fault/retry tesztkód; nem futottak.

- Korábbi alkalmazásforrás-checkpoint **0f51320**: a replacement mutex író-admissionnel
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
  származtatottnak jelölt XML/manifest. Jelenleg4 instrumentációs próbakód.
- **122 új unit + 6 instrumentációs tesztkód megírva, még nem futtatva.**
  Nincs új zöld kapu/APK/screenshot. Utolsó teljes zöld forrás **dd4cc27**:
  unit Phone393/Wear167, Wear UI183, signed upgrade2. Bizonyíték: WORK_HANDOFF.md.
- Main utóbbi 8a36502d változása csak AGENTS thinking-dokumentáció; main app baseline
  továbbra elfogadott 1.3.71-perf-20261022 (5d27aa9 / app715dc5d), nem régi buildre portolunk.

## Következő feladatok – nem lezárt részek

1. A még szabadon futó egyéb írók admission és UI hibakezelése;
   cloud photo staging/transaction. A bekötött kapu nem minden szabad DAO és külön
   Health SQLite írás globális zárja. A megírt ZIP/Health tesztek még nem futottak.
2. Intervallum/preset új kód futtatott igazolása még nincs; Coach per-gyakorlat
   javaslat/progresszió és provider-frissítés; katalógus/nevek/lokalizáció.
3. Mockup teljes vizuális kidolgozás (előtte Medium jelzés): többek közt mini-route
   listathumbnail, részletes Health forrásadatok, program/gyakorlat szerkesztés/paritás.
4. Kritikus Phone UI suite bővítése GPS/kétoldali/finish/discard/undo/retry/backup/
   0-200-1500 mintára és minden screenshotállapotra; végső azonos forrású CI és APK-pár.
5. Valódi Phone/Watch7/Health és kétkészülékes offline próba külön; emulátor nem igazolja.

A dizájn hat kötelező motorfeltétele a `docs/design/NATIVE_MOCKUP_REVIEW.md`-ben.
Minden dátum/eredmény forráshoz kötött: a kód megléte nem sikeres futtatás.
