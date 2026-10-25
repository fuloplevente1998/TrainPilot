# Natív folytatási pont – rövid kontextus

Frissítve: 2026-10-25. AGENTS.md + ez + WORK_HANDOFF.md teteje elég induláskor.
Történeti részletek az átadóban/Gitben, ne olvasd vissza a teljes chatet/exportot.

## Hatókör és munkamód

- Munkafa `/workspace/TrainPilot-native-migration`, kizárólag feat/native-android-migration
  a fő munkához. Main merge/verzióemelés/élő adatos install STOP.
- App com.repforge.app, 1.3.71-native-test /2736 mindkét eszközön. Main app7473b30,
  docs8a36502; elfogadott gyorsított 1.3.71/2735 a v1.3.71-perf-20261022 és
  wear-v1.3.71-perf-20261022 pár. Ezen kívül semmilyen release nem igazolt.
- Teljes feladat: plans/NATIVE_FINAL_DELIVERY.md /NATIVE_COMPLETION_BATCH.md;
  minden funkció/adatút +24 főnézet + teljes témakatalógus + HU/EN/DE/RO.
  design/NATIVE_DESIGN_HANDOFF.md; öt alsó fül, globális Settings, aktív edzésből
  Settings roundtrip nem pause; új mockup helyett végül valódi emulátorképek.
- Előbb kód/tesztkód, kis [skip ci] commitok/push, utána közös GitHub-kapu.
  Helyben nincs Gradle/emulátor/teljes suite. Meglévő Kotlin/adatút, nem prototípus.
  High kód; Medium vizuális dizájn előtt jelezni; Extra High komoly végső debug előtt.
  Kevés lényegi magyar output, nincs belső subagent külön kérés nélkül. PR238 követőlap.

## Legújabb alkalmazásforrás

- **1a9b6d3 /6856abe**: Journal részletekből valódi izolált edit/save/cancel:
  idő, gyakorlat/szett add/remove, weight/reps/time/bilateral/distance/done/RIR.
  Csak explicit Save ír; exact-ID detail CAS minden relationnel, dataset epoch,
  tombstone és ugyanazon aktív/finalizáló edzés guard. Main-looper snapshotcheck;
  meglévő bounded PR evaluator +LegacyWebViewMigrator egy Room tx-ben, nincs
  finish/active UI replay. Shared single-row exporter megőrzi raw extensionöket
  exercise/set/photo/feedback szinten is. ID/sync/key/routes/photos/Health marad;
  időváltozásnál eredeti Health-window jelölés, nem átcímkézett mérés. Változatlan
  időknél aktív duration és a szubmásodpercek is maradnak. Új katalógusgyakorlat
  unit-CAS, újsetUUID; hibás legacy dátum még UI-megnyitás előtt error.20 resource
  négy nyelven. +12 JVM/+1 valódi UI-case kód NEM FUTOTT; csak static/SQL/index.

- **b161126 /97f18fd**: #242 P1 program Rx editor bekötve: sets/reps/idő/weight/
  rest/trial program-szinten, explicit delta a meglévő NativeProgramEditor.Draft-
  ban. Csak parent Save ír, global prescriptions és meglévő inline explicit
  mezők együtt frissülnek, opaque/RIR/variation/string ID megmarad. Builtin clone/
  program CAS/active guard marad, katalógus unitváltozáskor stale save tiltott.
  Form HU/EN/DE/RO17 új string, valódi settings-rest fallback. A közös actual
  NativeWorkoutSession/start/prefill policy változatlan; grouped entries/parsed
  Rx reuse mentéskor. +11 JVM-kód és meglévő app UI-eset invalid/cancel/save/
  reopen/outerCancel bővítés NEM FUTOTT; nincs új UI-esetszám.

- **e4920f4**: Home mai edzésszám helyi nap kezdete <= startedEpochMs < következő
  nap kezdete scalar Room queryből, nem teljes naplóból. Zone/DST23/25h és
  tombstone-kizárás; Health-read hiba sem nullázza a már ismert edzésszámot,
  cancellation továbbmegy. +3 JVM-kód NEM FUTOTT; actual SQL boundary/DST/
  tombstone/index check PASS, nem Kotlin/Room. Célnap/start/invalidation hátra.

- **7d62ae3**: NativeCloudPhotoDeletion processz-szintű per-photo foglalás.
  Rövid metadata+ownership→ActiveStore→canonical store lock alatt friss Room/
  draft/native/mirror live-ref ellenőrzés és reservation, HTTP minden ilyen
  lockon kívül. Futó fizikai hívás alatt restore/erase és azonos ref visszaírás
  elutasítva; ordinary settings/új UUID-s edzés engedett. Native frame root
  photos-only gyorsellenőrzés, nem set/GPS fanézés tickenként. Külső finally a
  registration utáni cancellation esetén is takarít; server-timeout/cross-device
  bizonytalanság nem lezárt. Shared photoRows history+draft/session: a ZIP/stage/
  redo a draft képeit is viszi, aktív draft-ref legyőzi a history tombstone-t.
  Room deletedAt normalizálás közös helperrel (0.0 is élő). +14 JVM-kód NEM FUTOTT.

- **87b593b**: Google consent/network folytatás dataset-epoch ticketje. Backup/
  restore/erase után régi consent nem menthet profilt vagy calendar cache-t;
  rövid mutate lease-ben ticket+prefs commit, userinfo/Google HTTP kívül marad.
  Calendar újabb op előtt és kész nyugta előtt újraellenőriz. +2 nem futott JVM.
- **547c2f8**: valódi Google-bejelentkezés (nem disconnected sync); egyetlen
  authorizeAndRun tartja a Google-operation gate-et auth+consent+profil+adatművelet
  alatt. Korábbi connect/Drive/calendar mag privát kiemeléssel közös, nincs új
  merge/sync rendszer. NativeGoogleConsentBridge a VM-ben, token nem perzisztált;
  NativeGoogleConsentLauncher per-attempt Compose key, ActivityResult, rotation
  nem relaunch, cancel/late/duplicate/process-recreated eredmény elutasítás.
  Régi GoogleAuthOutcome /request9001/9002 út törölve. HU/EN/DE/RO 9 új resource.
  +9 consent/cancel/admission JVM-kód nem futott. Valódi OAuth/account még tesztelendő.
- **3b548ac**: #24082883ab lokalizáció squash integrációja, 20 fájl/4 JVM-kód;
  **6cf13be**: #241ab25d84 négy új fájl/6 UI-eset squash integrálva és javítva.
  Mind a hat a valódi HU/EN/DE/RO pickert kattintja, explicit Settings LazyColumn
  scrollToNode, nem direkt DataStore-írás. Home program/nap a tárolt megfelelő
  fordítást használja magyar fallbackkel; saját név nem gépi fordítás. Látható
  steps/workouts/recovery egységcaption/tag; már nem eldobott Triple metaadat.
  Teszt tényleges Home név/nap/egység UI-t ellenőriz. NativePhoneNavigationTest
  meglévő profil/nyelv görgetése is helyes LazyColumn-adatút. Még nem futott.
- **a63a1f1**: tartós NativeLanguagePicker a Settings tetején, settings_language_
  hu|en|de|ro, autonymok/radio/tématokenek/48dp és replacement-védelem. Megőrizve.
- **1839dd0 /7e8f1e5**: processz-szintű Google-gate; canonical CAS és ZIP-redo
  local acceptance utáni snapshot-publish, verified publish utáni cloudBase,
  dataset continuation guard a későbbi stale base/status/új remote delete ellen.
- **567117a /f81a1a4**: exact-ID súlytombstone meglévő kv_documents és JSON/ZIP/
  Drive marker-unió; más UUID engedett, explicit restore visszahozhat. Cloud-photo
  privát staging, MD5/SHA/CAS, existing ZIP receipt/originals/redo+törlés, tartós
  deletedAt/DriveID retryhoz. Többi korábbi funkció részletei a handoffban.

## Bizonyíték és külső ágak

- **Batch545 új JVM +49 Phone UI tesztkód NEM FUTOTT** (533+12 /48+1).
  Helyben csak lexical delimiter, diff, XML/kulcs/format/import/R-ref és pontos
  forrássorrend/kiemelési diff ellenőrzés volt. Nem Kotlin/Room/Compose/Google.
- Migrációs ág utolsó teljes zöldje továbbra dd4cc27, Actions40072751592:
  Phone393/Wear167/WearUI183/signed upgrades2. Friss CI/APK/screenshot nincs.
- Külső #240 feat/native-ui-localization82883ab és #241 test/native-localization-ui
  ab25d84 kódja már bent, 4+6 most része a545+49-nek. Ágaikat nem írtuk, nem
  GitHub-merge történt; draftok nyitva maradnak az integrált próba/elfogadásig.
  Új külső HEAD-et az innen vett delta alapján nézz, ne integráld újra a régi diffet.
  Review: reviews/NATIVE_LOCALIZATION_INTEGRATION.md. Korábbi „nem integrált”/
  „DataStore-t vált”/„nincs nyelvgomb” riport az előző SHA-k történeti állapota.
- phone-ui-gate.py -e package com.repforge.app az új osztályokat is kiválasztja;
  nincs szükség új class-filter/workflow-ra. Más ág CI-ja nem a mi bizonyítékunk.

- Külső funkcióaudit #242/9c3c264 visszaérkezett: docs/reviews/
  NATIVE_FEATURE_PARITY_AUDIT.md exact másolat, rövid aktuális állapot a
  NATIVE_FEATURE_PARITY_TRIAGE.md-ben. Bázis a5f562d/main7473b30 +Home delta
  ellenőrizve. Hét igazolt source gap, ebből Rx és Journal edit kódban pótolva, öt P2 nyitott.
  Report nem runtime-teszt és nem minden feature teljes leltára; ne kezeld release-
  elfogadásnak. Külső ágra nem írtunk, draft #242 nyitva, nincs GitHub-merge.

## Következő konkrét lépések

1. A két audit-P1 (Rx/Journal edit) kódban, még nem tesztelfogadás. Fő munkás
   következő területe Home NativeCoachTargets/start/invalidation, majd workout
   HC-panel/refresh, modes/progression és foreground auto. Külön chat/Gemini
   promptok: plans/NATIVE_HEALTH_DATE_RANGE_HANDOFF.md és
   plans/NATIVE_CARDIO_GEMINI_HANDOFF.md. Indulás/eredmény nem igazolt; a két
   Health-böngészőfájlt és az új NativeCardioModel(+Test) neveket ne írd közben.
   Raw bázis9a18ca6 mindkettőnek, külső eredmény mindig exact diffből review.
2. Lokális in-flight photo delete / új ref / restore admission kódban, de szerver-
   timeout utáni bizonytalan befejezés, processdeath és cross-device/older-peer
   továbbra nyitott. A foglalás nem tartós Drive-lock/elosztott atomikusság.
3. Teljes A–E/UI/Health/main funkcióleltár és maradék dinamikus VM-/hibafeliratok;
   15 lokalizált képernyő nem minden funkció. A mai Home edzésszám e4920f4-ben javítva; az ajánlott nap még első nap. Következő kód: meglévő
   ui/coach/NativeCoachTargets újrahasználata Home-ban, programId/dayId/scheduleId
   végig vitele a nav-startnak (most csak dayId), helyi nap/DST epoch count és
   tényleges adat-invalidation. Ezt még nem implementáltuk.
4. Google-fiókos engedély/részleges grant/revoke/offline/rotation/processdeath,
   backup közbeni cancel, calendar cache és cél-scope valódi szolgáltatással még
   nem igazolt. Unit consent-intent nem OAuth- vagy UI-szolgáltatás-siker.
5. Batch végén közös GitHub JVM/Compose/Wear/R8/signer/upgrade kapu ugyanazon
   appforráson, majd valódi képek és fizikai telefon/Watch7. Ne jelöljünk késznek
   funkciót pusztán fájl/tesztkód miatt. Élő adatok fölé APK még nem adható.
