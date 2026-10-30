# PR252–254 – közös integrátori review, 2026-10-26

| PR | Exact külső HEAD | Integráció | Terület |
|---|---|---|---|
|252|936e5df16ceb138ffa60a1dfbd512411449cd4a0|f208617c|Többhetes Calendar preview/mentés|
|253|3d45d3aedae169bf9a3afd4598bfaba51a92cff7|acd6927c|Welcome/profil/skip/continue|
|254|c636682624716923f1f5e7be1e380732e57e5723|624aaf01|Builtin copy-on-edit/gyári A–B nap|

Mindhárom pinned7043fc7d-ből, fájlhatáron belül érkezett. A külső teljes delták
beemelve, a root b28672bf Coach/Home és bc18d019 Journal változás megőrizve.
Külső PR/branch/main merge/lezárás nincs. A #253 riport hat Markdown hard-break
sorvége byteazonos maradt; az Android-kód/resource diff whitespace ellenőrzése tiszta.

## Review és root-korrekció f72c9cb0

- Calendar: main planWeeks L15651–15683 és a végső L13063 caller szerinti
  daily/alternate/weekly, ciklus/ID reuse/cancelled tombstone. Preview mezőváltozás
  után már nem jóváhagyható. Atomikus plannerSettings+schedule és CAS/epoch/receipt.
  Root a védett alkalmat egyszer olvassa preview-nként, és a main-only native frame,
  projekció és ownership snapshot közös scheduleId-jét is védi; eltérő aktív ID-k
  mellett leáll. Main→IO/Room→Main rövid snapshot-hop, nincs SDK/network a lease-ben.
  Dokumentált eltérés megmarad: DST-gap elutasítás (meglévő natív editor szabály),
  bármely mentett history konzervatív védelme. Nem teljes byteazonos JS-paritás.
- Onboarding: sikeres legacy import utáni valódi empty/new döntés, unknown nemüres
  legacy kulcs megőrzése, profil+complete atomikus CAS, skip/in-progress tartósság.
  `resetAllSettingsForEraseLocal` immár welcome állapotot jelent. Az eddigi UI
  tesztek létező felhasználót modelleznek: root explicit complete fixture-t ad nekik;
  az új first-install teszt továbbra valódi welcome-t kér. Friss UI-teszt előtt a
  durable execution/Room/Health állapot is izolálva, nem csak a snapshot törölve.
- Program: meglévő editor/Room writer továbbhasználva; idempotens copy request,
  új composite day ID és gyári lineage; szűk A/B reset explicit cross-day warninggal,
  source/factory CAS, metadata/ownership/finalizer guard. Root a hat JVM tesztet
  StandardTestDispatcher/runTest-re teszi a Room→Main hívások miatt, valódi store
  cleanup mellett. Nincs main-thread guard kivétele vagy assert lazítása.

**A07 részleges:** a main legacy `plan` reset és a globális gyakorlatkatalógus
Settings-reset művelete nem azonos az új saját program A/B visszaállításával.
Tetszőleges program duplikáló gombot nem találtak/vezettek be forrás nélkül.
Ez nem zárja le automatikusan a teljes A07 követelményt.

Megírt külső tesztforrás: Calendar11 JVM+1 UI, Onboarding7 JVM+2 UI,
Program6 JVM+3 UI. Ezek a három csomag közös új forrásán még UNRUN.

## Korábbi b28672bf tényleges futás és javított UI-próbák

Actions40291546367 valós XML: telefon1096/0fail/0skip, Wear167/0fail/0skip.
R8 aláírt APK-pár, com.repforge.app /2736 /1.3.71-native-test és eredeti signer
ellenőrzés PASS. Mindkét régi release-ből signed adb install-r upgrade PASS
(v1.3.71 és v1.3.71-perf-20261022). Wear round három job68+56+59=183 PASS.

Telefonos UI deklarált69 tesztből csak18 fejeződött be:12PASS/6FAIL, majd process
crash,0képernyőkép. Ez nem18/69 teljesített kapu. Az új Journal feedback UI PASS.
Raw evidence: artifact12636867224, instrumentation/logcat/manifest. Hibák:

1. Cardio a még nem komponált theme lazy-itemet kereste. Root a szülő Settings
   listát görgeti a taghez, megőrzi a valódi picker és persisted theme assertet.
2. Health range a képernyőn kívüli tegnapi lazy sort várta. Root betöltött lista
   után odagörget, majd mindkét valódi rekordot/range-validációt ellenőrzi.
3. Health sync `assertTextContains` alapból teljes text-értéket keres; a node
   ténylegesen `Feldolgozott napok: 7 / 30`. Root explicit substring ellenőrzést ad.
4. Lokalizált interval-rest mező off-screen volt: root odagörget és címkét ellenőriz.
5. Második stopper indítása futó pihenő mellett helyesen elutasított. Root a pihenőt
   a valódi Skip UI-val lezárja, majd a második stopper identitását/haladását teszteli.
6. NavController log a naptárról indított launch után worker threadet mutatott,
   teardownkor INITIALIZED→DESTROYED crash. Root coroutine és post-IO navigáció
   explicit Android Main dispatchert kap, a canonical nyitó main-looper assertet.
   Ennek termékoldali javítása csak új UI-runból fogadható el, nem a régi JVM-ből.

Új canonical run40315675080, exact appf72c9cb0c8e336ab6cc4073d3005e7d30b3ed7fd:
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40315675080
Indításkor IN_PROGRESS. Ne indíts második teljes kaput. Main/verzió/élő adatok
változatlanok; emulátoros upgrade PASS nem általános saját adatos install-engedély.

## Követő compiler-javítás

40315675080 debug/app+phone UI-test APK fordult, de compileDebugUnitTestKotlin
NativeOnboardingStateTest három p.Destination hivatkozása nem oldható fel.
771d90a9 csak teljes NativeOnboardingPolicy.Destination típusnevet használ,
assert és appforrás változatlan. Új egyetlen40316447151 checkout771d90a9,
indításkor IN_PROGRESS; az új24JVM/6UI még nem igazolt elfogadás.

## Futással igazolt közös JVM eredmény

40316447151 cancellation log tényleges utolsó eset: NativeProfileDayHistory128Test.
dayEvidenceAndReductionSurviveCanonicalBackupRoundTrip (nem a még el nem ért
ProgramActions osztály). A #254 commitkori második Main guard régi teszteket is
érintett. b2536516 eltávolítja a programtesztek nested blokkolását; e28ab418 öt
korábbi program/profil osztályba közös StandardTestDispatcher/runTest rule-t és
felfüggeszthető, pontos kivételtípust ellenőrző assertet tesz. Produkciós védelem
megmaradt.40317244820 saját elavult futás szintén cancel; nincs PASS-ként számolva.

Új40317771457, exact e28ab418, valós XML12647857246: telefon1120/0/0, Wear167/0/0,
Calendar11/Onboarding7/Program6 mind PASS. Debug+test-APK, R8/signing/metadata kapu
SUCCESS. UI/Wear/upgrade futás még folyamatban az ellenőrzés pillanatában.
