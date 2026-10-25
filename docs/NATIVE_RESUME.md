# Natív folytatási pont – rövid kontextus

Frissítve: 2026-10-25. Új chat/kompaktálás után AGENTS.md + ez + a
WORK_HANDOFF.md teteje elég; a teljes előtörténet az átadóban marad.

## Hatókör és munkamód

- Munkafa `/workspace/TrainPilot-native-migration`, fő munka kizárólag
  `feat/native-android-migration`; main merge/verzióemelés/élő adatos install STOP.
- App `com.repforge.app`, `1.3.71-native-test` /2736 mindkét eszközön.
  Main appbaseline7473b30, main docs8a36502; készüléken elfogadott gyorsított
  1.3.71/2735 a v1.3.71-perf-20261022 és wear-v1.3.71-perf-20261022 páros.
- Teljes feladat: plans/NATIVE_FINAL_DELIVERY.md és NATIVE_COMPLETION_BATCH.md;
  minden funkció/bekötés +24 főnézet + teljes témakatalógus + HU/EN/DE/RO.
  Dizájn: design/NATIVE_DESIGN_HANDOFF.md; öt alsó fül, globális Settings,
  aktív edzésből Settings roundtrip nem pause. Nincs új generált mockup.
- Előbb kód/tesztkód, kis [skip ci] commitok/push, utána közös GitHub-kapu.
  Helyben nincs Gradle/emulátor/teljes suite. Futtatott és megírt külön szerepel.
- High kód; Medium vizuális dizájn előtt jelezni; Extra High komoly végső CI-debug
  előtt. Kevés, lényegi magyar output; meglévő Kotlin blokkok/adatút, nem prototípus.
  Nincs belső subagent külön kérés nélkül. PR238 állapotlapot is frissítsd.

## Mostani appforrás és bizonyíték

- **a63a1f1**: látható, tartós HU/EN/DE/RO választó a Settings tetején,
  autonym/neves rádiószemantika és settings_language_hu|en|de|ro tag.
  Theme-től független, valódi SettingsViewModel.setLanguage/DataStore adatút;
  replacement alatt tiltott. Négy külön native_language.xml (nem a #240 XML-je).
  Meglévő settingsThemeAndLanguageChangesReturnToTheirOrigin UI-próba most
  választóra kattint, nem direkt DataStore-t ír. Megírva, nem futott.
- **1839dd0**: egy processz-szintű atomikus Google-műveleti kapu minden repo-
  példányon át; auth/connect/revoke/Drive/calendar/delete/disconnect közös.
  Nem queue-zik késői törlés/fiókmódosítás. Cancellation továbbmegy, busy finally
  takarítódik, prefs commit hibája látható. Nem kapcsolt/hiányos fiók tokenje még
  userinfo/data HTTP előtt tiltott; meglévő stabil sub mismatch-védelem megmarad.
  Pending OAuth-result teljes lifecycle/UI bekötése még hátra (lásd következő).
- **7e8f1e5**: meglévő NativeCloudMergeGuard most canonical guard + ZIP-redo
  local acceptance UTÁN hívja a snapshot-publishert, hálózat lease-en kívül.
  cloudBase csak verified publisher után íródik; hibánál az elfogadott local data
  és régi base megmarad. Replacement-epoch folytatási ticket: későbbi restore/
  erase után nincs stale base/status írás vagy új remote-delete indítás.
  Sima köztes szerkesztés megmarad, base a publikált korábbi állapotot jelenti.
- **567117a**: exact-ID súlytörlés marker+delete Room tx; meglévő kv_documents,
  opcionális weightTombstones18 JSON/ZIP/Drive; legacy/idless/stale-ID replay tiltás,
  más UUID azonos dátum/érték mellett engedett, explicit restore visszaállíthat.
- **f81a1a4**: privát cloud-photo staging, MD5/SHA/CAS, existing ZIP receipt/
  originals/redo install+törlés; nincs pre-guard live fájlírás. DeletedAt/DriveID
  megmarad retryhoz, getAllTombstones. Korábbi részletes commitok a handoffban:
  bounded súlynapló/aggregátumok, scoped streaming PR, teljes tervadaptáció,
  Health/alvás/részletek, könyvtár/programszerkesztő, main témák stb.
- **Batch490 új JVM +42 Phone UI tesztkód NEM FUTOTT.** Most +17 Google gate/
  account/admission és +9 cloud/fence recovery kód. Csak lexical delimiter,
  forrássorrend, négy XML/kulcsütközés és diff-ellenőrzés volt; nem Kotlin/Room/Google.
- Migrációs ág utolsó teljes zöldje továbbra **dd4cc27**, Actions40072751592:
  Phone393/Wear167/WearUI183/signed upgrades2. Friss CI/APK/screenshot nincs.
  Másik ág (native-android-finalization) futása nem a mi batchünk bizonyítéka.

## Párhuzamos külső munkák – nem integráltak

- #240 `feat/native-ui-localization`, bázis958907c, friss HEAD **82883ab**.
  Statikus újraellenőrzés: négy651/377 native-kulcsú XML egyezik, BLE fallback és
  lépésegység javítva; Home programcím továbbra nameHu. TodayMetricsGrid eldobja
  a Triple egységmezőt, így új egységkulcsai nem látható UI. Négy külön JVM-kód
  nem futott. reviews/NATIVE_UI_LOCALIZATION_PR240_REVIEW.md.
- #241 `test/native-localization-ui`, bázis82883ab, HEAD **ab25d84**, cél #240 ága.
  Négy új fájl, hat Compose-eset, NEM FUTOTT. A user átadta, most PR-ban rögzítve.
  Nyelvet közvetlen DataStore-ból vált, nem UI-ból; main migrationben a választó
  immár megvan, integráció után erre kell átállítani. Részletes review még nyitott.
  phone-ui-gate.py -e package com.repforge.app: az új osztályok külön class-filter
  nélkül is kiválasztódnak; korábbi feltételes class-filter aggály itt nem áll.
- Két külső ágra nem írunk, nincs merge; a 4+6 külön teszt nincs a490+42-ben.
  A migrationben hozzáadott picker/Settings sort ne veszítsük el integrációkor.

## Következő konkrét lépések

1. #240 maradék Home program/nap lokalizáció és látható egységek, #241 teljes
   review + integráció és a hat forgatókönyv tényleges UI-nyelvváltásra átállítása.
2. Google UI auth/connect és pending resolution/result még nincs bekötve:
   a bejelentkezési gomb jelenleg syncDriveCloud-ot hív, ami disconnected állapotban
   helyesen tilt. Request code9001/9002 eredményének nincs appos kezelője. A mostani
   kapu nem tartja zárolva a felhasználói consent teljes életciklusát; stale OAuth
   callback/disconnect védelmet a tényleges UI-eredményút bekötésekor kell lezárni.
3. Remote photo delete és közben induló explicit restore/új ref versenye továbbra
   nyitott: a ticket ellenőrzése nem fogja visszavonni a már folyamatban levő HTTP-t.
   Nincs elosztott atomikusság; orphan photo/snapshot bizonytalan network/read-back
   után, cross-device/older-peer és valódi Google integrált próba még szükséges.
4. Teljes A–E/UI/Health funkcióleltár a parity audit/tervek szerint; ne a mockup
   darabszáma vagy a fájlok megléte legyen a készültség mércéje. Utána közös végső
   GitHub JVM/Compose/Wear/R8/signer/upgrade kapu, valódi képek és készülékes próba.
