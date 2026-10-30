# Elkülönített külső munka – 2026-10-26

Repository: https://github.com/fuloplevente1998/TrainPilot
Exact induló commit: `e28ab41887a976ee380f63f465dbdcb51f494329` (appkód f72c9cb0).
Először AGENTS.md, docs/NATIVE_RESUME.md és docs/WORK_HANDOFF.md aktuális teteje,
majd docs/reviews/NATIVE_PR250_INTEGRATION.md és az alább kijelölt források.
Az induló közös kapu40317771457 még nem elfogadott teljes eredmény.

## Munkaszerződés

- Saját új ág ebből az exact base-ből, egy Draft PR célja feat/native-android-migration.
  Main/migráció írás, merge/rebase/force-push/PR vagy issue zárás tilos.
- Kis visszakövethető [skip ci] commitok. Kód és megírt teszt nem runtime PASS.
  Root a közös teljes CI és integráció gazdája; meglévő futást ne cancel/re-run-olj.
- Nincs belső subagent, új dependency, séma, verzió/csomag módosítás.
  com.repforge.app /1.3.71-native-test /2736 megmarad.
- A felhasználó külön saját ági tesztelést is szeretne: célzott teszt/teszt-APK
  előkészíthető, ágnév+exact SHA+teszteredmény+artifact/hash egyértelmű átadásával.
  APK/build/emulátor GitHubon; teljes natív workflow-k párhuzamos indítását Root
  koordinálja, ne indíts minden részcommit után teljes suite-ot. Új workflow-t ne készíts.
- Élő saját adatok fölé install vagy valódi Google-adatok törlése/visszavonása nem
  része ennek a feladatnak. Csak izolált tesztadat és meglévő guardok; semmit nem
  gyengítünk azért, hogy egy teszt zöld legyen. Készülékes elfogadás külön státusz.
- Új UI felirat a lent megadott saját HU/EN/DE/RO XML-ben, nem közös strings.xml.
  Meglévő tématokenek/48dp célok/Back és aktív edzés megőrzése kötelező.
- Csak az engedélyezett fájlterületen írj. AGENTS/RESUME/WORK_HANDOFF és más
  munkás riportja helyett saját review-handoff; új saját tesztosztályok.
- Záró átadás: exact HEAD, diff lista, UI→caller→writer tartós eredmény, megírt és
  ténylegesen futott tesztek külön, ismert korlátok, draft PR link. GitHub-komment nincs.

## Plus chat 1: telefonos GPS-engedély és helyszolgáltatás visszaútja

Ág: feat/native-gps-permission-recovery

E18: a GPS kontextusos helyengedélykérése létezik. Csak a hiányzó helyreállítási
lépéseket pótold: precise vs approximate/denied/permanently denied, központi
location services OFF, visszatérés után valós állapot és explicit újrapróbálás.
A korábbi hiba tanulsága: az appengedély és az óra/telefon központi helykapcsolója
külön állapot. Ez a feladat kizárólag TELEFON; Wear/GPS-motor nem újraírható.

Írható: ui/workout/ActiveWorkoutScreen.kt GPS permission/request része;
új ui/workout/NativeGpsPermission*.kt helper/dialog;
új res/values{,-en,-de,-ro}/native_gps_recovery.xml;
új NativeGpsPermissionRecovery*Test.kt JVM/nativePhoneUiTest;
docs/reviews/NATIVE_GPS_PERMISSION_RECOVERY_HANDOFF.md.
ActiveWorkoutViewModel.kt csak minimális recovery UI state/callback, ha szükséges.
Olvasd, de ne módosítsd: NativeGpsMeasurement, NativeWorkoutTarget,
DistanceTrackingService, NativeOutdoorStart, NativeWorkoutEditing.

Nem írható: Settings (másik munkásé), NavHost, Health/BLE, Wear/shared, GPS provider/
route tárolás/engine/finalizer, Store, régi közös teszt, Gradle/workflow/scripts.

Elfogadás:
1. Explicit GPS-indításnál az akadály konkrétan látszik; alkalmazásengedély és
   központi helyszolgáltatás helyes rendszerbeállítására vezető működő gomb.
   Nem egy elrejtett általános hub, nem puszta angol SDK-kivétel.
2. Runtime kérés a meglévő Android launcherrel; approximate grant nem precise.
   Denied-forever esetén app details; system location OFF esetén location settings.
   Back/deny mellett nincs GPS- vagy sorozatindítás, nincs kitalált távolság.
3. ON_RESUME valós platformállapotot olvas; beállításból visszatérés önmagában
   nem automatikus GPS-indítás. Explicit retry a pontos eredeti workout/generation/
   exercise/set célhoz, még aktuális phone-owner állapot mellett; elavult cél elutasítva.
4. Aktív stopper/intervallum, átadás, befejezés, szünet és privacy szabály megmarad.
   Ne módosítsd a GPS-pontszűrést/távot, és ne engedélyezz háttérhelyet indokolatlanul.
5. Döntési JVM-mátrix és valódi UI tesztforrás revoke/approximate/system OFF/
   Settings-return/Back/stale target; emulátoros fixture-only platformtoggle, cleanup.
   Valódi GPS-fix és OEM háttérműködés külön készülékteszt, nem fake PASS.
