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

## Codex: Google-karbantartási műveletek bekötése

Ág: feat/native-google-maintenance-ui

Bizonyított hiány: GoogleSyncRepository.revokeAccess /deleteAllDriveAppData /
deleteManagedCalendars létezik, Settings UI/VM caller nincs. Main7473b30
app.js L10759/L11177 → tp105DataPanel L15019–15021 → tp105ManageData L14998–15017.
Nem új szinkronmotort kérünk: a meglévő account/consent/operation/epoch adatútra
kösd rá a három külön, egyértelműen megerősített műveletet.

Írható: GoogleSyncRepository.kt (csak maintenance/authorization routing),
ui/settings/SettingsScreen.kt, SettingsViewModel.kt, új ui/settings/NativeGoogleMaintenance*.kt;
NativeGoogleConsentBridge.kt és NativeGoogleOperationGate.kt csak szükséges,
bizonyított adapterkiegészítés; meglévő szerződés megőrzendő.
Új res/values{,-en,-de,-ro}/native_google_maintenance.xml;
új NativeGoogleMaintenance*Test.kt JVM/nativePhoneUiTest;
docs/reviews/NATIVE_GOOGLE_MAINTENANCE_HANDOFF.md.

Nem írható: más backup/merge/photo/journal/Health/GPS/planner/finalizer/nav,
SettingsStore, shared, régi közös teszt, Gradle/workflow/script. A meglévő JSON/ZIP
és időzítő UI megmarad. Másik munkás a GPS kontextusos engedély-visszaútján dolgozik.

Elfogadás:
1. Kapcsolt fióknál külön revoke, saját Drive app-data törlés és kizárólag TrainPilot
   által kezelt Google-naptár törlés. Pontos érintett fiók/hatókör és következmény,
   explicit megerősítés/Mégse. A helyi napló egyiknél sem törölhető mellékhatásként.
2. Existing authorizeAndRun / GoogleSyncAction bővítés vagy meglévő adapter: token
   nem kerül UI-ba/logba/backupba, nincs második account- vagy consent-motor.
   Kerüld a nested operation-gate holtpontot; SDK/network metadata mutexen kívül.
3. Preview/consent után account+dataset újraellenőrzés; aktív/finalizáló/átadó edzés,
   restore és már futó sync mellett biztonságos admission, nincs early success.
4. Részben sikeres távoli törlésnél pontos partial/error, nem rollback ígéret; újra-
   próbálás csak explicit. Lokális cloudBase/heads/sent cache és autosync olyan
   állapotba kerül, hogy ne támassza fel azonnal a törölt felhőadatot. A main tényleges
   disable/clear kezelését kövesd, meglévő helyi adatot ne veszíts.
5. JVM tesztkód deny/cancel/account switch/epoch/gate/partial/retry sorrendre;
   valódi Compose UI tesztkód nyitás/cancel/megerősítés/failure semantikára.
   Fake transport csak izolált tesztben; valódi OAuth/Google-fiók külön készülékteszt.
