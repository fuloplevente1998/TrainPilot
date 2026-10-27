# TrainPilot – Studio UI folytatási pont

## Ezt másold az új Codex beszélgetésbe

Folytasd a TrainPilot AI Studio dizájn beépítését a meglévő PR #270 ágán:
`feat/studio-ui-topnav-trial-20261027`, cél `feat/native-android-migration`.
A felhasználó Rootnak átadta a teljes beépítést; nem várunk másik fejlesztőre.
Minden érdemi csomag és állapot kerüljön GitHubra, köztes commit [skip ci].
Olvasd előbb ezt az átadót és AGENTS.md legfrissebb fejlécét. A főágat ne módosítsd.
Az elfogadott új irány FELSŐ ötfüles navigáció. A régi alsófüles dokumentumot ez
felülírja. A Studio demóállapotváltók/demószövegek nem végleges termékfunkciók;
valódi funkciógombot ettől még ne törölj. Minden nézetet valós natív állapothoz,
meglévő callbackhez és adatforráshoz köss. A terv95%-ossága nem a natív kód készültsége.

## Studio increment01 QA-javítások – 2026-10-27

40539957380 exact0fb350a4 lezártFAIL. Root JVM12750582974:1333PASS;
phone12750939854:114main112PASS/2FAIL+4GPSPASS=116/118,0missing/crash.
Mind3Wear és2signedupgradeSUCCESS. Home/profile/header-entry goldUI PASS.
65PNG manifestSHA256/PIL validált,2köztes natívHomekép:
docs/design/previews/studio-increment-0fb350a4/README.md.
457dc1e6 javítja a font2fülek naturalwidth mérését és a nestedLazy öröklésgomb
közvetlen scroll/visible assertjét +teljesdiag; b80ebc62 explicit sötét
systembarstyle és valódiUIassert (feketefelületen fehérikonok). Új runtime UNRUN.
AIStudio prompt APP BUILDERRE javítva6c9bdfa3: sajátprojektben módosít,
TELJES újZIP letöltve, nempatch/részarchívum; Rootdiffeli. Felső5fül/demokontrollmarad.
P1teljesUI/paritás továbbra nincs kész; részletek STUDIO_UI_RESUME-ben.

## Pontos állapot

- PR: https://github.com/fuloplevente1998/TrainPilot/pull/270
- UI alkalmazás-/tesztforrás jelölt: `d7e64697`.
- A korábbi PR-head8c3336bb + friss migration történet integrálva, nincs forcepush.
- Javítva a felső főfülek selected/Role.Tab és selectableGroup szemantikája.
  Az eredeti NativePhoneNavigationTest.tab() már kivárja a Selected=true állapotot;
  tesztet nem gyengítettünk. Az előző40535425696 lezárt egy összehasonlító UI-hibán;
  a lent dokumentált javítással és a teljes Studio-incrementtel kell új gate.
- A felső pill elrendezés és a main launcherikon megvan, ez NEM a teljes StudioUI.
- Valódi kiindulási képek8nézetre: docs/design/previews/p1-f0c474f1/README.md;
  még a Studio ELŐTTI alsó navigációval, világosan szintetikus fixture-adatokkal.
- Stabil migrációs appforrás: f0c474f17072aaf6f5c23506e0ad5b1c0092e2ae,
  Actions40529220611 mind7jobSUCCESS. Root raw1333JVM+117phone+183Wear PASS,
  0failure/error/skip/missing/crash;2signedupgradeSUCCESS.65PNG hash/size/PIL validált.
  Ez NEM a PR270 új felső navigációjának elfogadása.

## Bemenet megérkezett – ellenőrzött eredeti Studio ZIP

A felhasználó f685d540 alatt feltöltötte:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/f685d540/docs/ai-studio/trainpilot-oct-27-2026-3-58-04-pm.zip
3,957,338byte, CRC és SHA256 ellenőrizve:
574f17f639b91037a6e0bdea428f350d4c5950307227511eafc32e7f64bcefcc.
A16eredetiKotlinfájl változatlan referencia a docs/ai-studio/export-20261027 alatt,
README rawlinkindexszel és manifest bytehashokkal. Nem Android source-set.
A natív app nem importál com.example modellt, DemoDataProvider-t vagy ViewModel-t.
A3eredetiTTF ésSIL-OFL licencek res/font +assets/font-licenses alatt.
Nem kell új bemenetet kérni. Az alkalmazáskódba a következő increment kerül.

## Studio increment01 – forrás megírva, új runtime még szükséges

Jelölt app/tesztforrás: d7e64697 (8ba6a388+34c629b8+d7e64697).
Aktív canonical gate:40539957380 exact0fb350a47b1c17a04851ce6a0f7529f510dad8d9.
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40539957380
LezártFAIL; a fenti116/118eredmény és javítások érvényesek. Új egyszeri gate kell.
A későbbi065db698 csak docs-only AIStudio-finomítási prompt, nem új appforrás.
A felhasználó promptot kért AIStudiónak; átadó elkészült, tényleges külsőfeladat
indulását még nem jelezte. Root bekötés ettől független, nem állhat meg erre várva:
docs/plans/AISTUDIO_STUDIO_ACCESSIBILITY_REFINEMENT_HANDOFF.md.
Pontosított userfolyamat: AIStudio APP BUILDER; sajátprojektben javítás +TELJES új
projektZIP export. Nem kérünk patch-et/részleges ZIP-et vagy kézi fájlcserét a usertől;
Root hasonlítja össze az archívumokat és portolja a megfelelő UI-deltát.
- Studio SpaceGrotesk/PlusJakartaSans/JetBrainsMono fontok és typography, tnum;
  valódi statikusregular fontok, hiányzó súlyokat Compose szintetizálja.
- Studio sötét közösalap, hero finom háttérgradiens, a meglévő32mentettpaletta
  és kontrasztpolicy alapján; nincs téma/változó formátumcsere vagy fontScaleclamp.
- Márkajelzéses fejléc, valós nyelv/téma/profile Settings-entry; initialSection csakUI.
- Home üdvözlés/planner/valós session-target hero/metrikák/planning/Coach-belépő.
  A profil CTA meglévő Settingsprofil, nem állítjuk, hogy új programot generál.
  A Coach-előnézet még a tényleges regenerációt és forrás/notices értékét mutatja,
  nem a teljes Coach tanács/advice kivonat. Ezek további valós bekötést igényelnek.
- Metrikacímsor tényleges Health/Coach navigáció, minden régi érték/unit/source tag
  továbbra külön elérhető; ne tedd újra merge-descendants kattintható parent alá.
- Nagyfont: a felső fülsor vízszintesen görgethető, intrinsic címkékkel;
  új320dp/font2DE próba valós textLayout overflow/selected/disabled assertionnel.
- Eredeti profile-persistence és4nyelv/téma roundtrip tesztek új header/Home entryvel
  bővítve; minden régi assertion megmarad. Várható1333JVM+118phone+183Wear.
- Az előző topnav-gate40535425696 exact5bbc9162 lezárt FAIL:116/117phone,
  0missing/crash; uiApprovalRejectsProgramRawChangeAfterShownComparison awaitReady.
  Diagnosztikai onRoot kétablakos hibája elfedte az eredeti várási okot.
  d7e64697 teljes összehasonlítást egyetlen scroll owner alá tesz, megvárja a látható
  comparison_0 sort; multi-root diagnostic javítva. Nincs timeoutemelés/assertiongyengítés.
  A többi6jobSUCCESS. Új közös gate kell az egész incrementre, ne régiPASS-t állíts.
Részletes acceptance/provenance: docs/reviews/NATIVE_STUDIO_INCREMENT_01.md.

## Újabb Home-bekötés – c167d92f (még UNRUN)

ad10ff9d: Home újterv gomb → kanonikus Programs VM personalpreview, hidratálás
után egy memóriás intenttel. Profil külön natív szerkesztő; cancel/program/profile/
session megőrzés bővített eredetiUItesztben. c167d92f: márkafelirat → Home valódi
navguarddal, eredeti5fülesUIteszt ezt is ellenőrzi. Új8locale-kulcs nincs, csak
1newplan kulcs4nyelven. Várhatóképszám66,tesztszám118 változatlan.
A futó40544853264 exact1aacc644 MÉG NEM tartalmazza ezt a2újcodecommitot;
csak a 457dc1e6+b80ebc62 javításokat teszteli. Ne nevezz újHeadet elfogadottnak.
24képernyő valós forrás-/adattérképe: docs/ai-studio/STUDIO_NATIVE_BINDING_MATRIX.md.

## Következő csomagok

1. Zárd le a topnav javítás canonical GitHub Android/UI ellenőrzését;
   olvasható HU/EN/DE/RO,320dp/font2.0,48dp célok, selected/disabled/Role.Tab.
   A10sp/egysoros ellipsis feliratok keskeny/nagyfont viselkedése még ellenőrizendő.
2. Friss Studio-export beolvasása és komponens/nézet→valós natív API megfeleltetés;
   közös P0 theme/components adaptációja,32paletta/4nyelv megtartásával.
3. Home/Training Studio-kompozíció: aktív/paused/planned/Coach valós állapot,
   jelenlegi6metrika és forrásjelzés; program/day/schedule indítás meglévő guarddal.
4. Health teljes main áttekintés/pulse/weight/journal/provider/permission részletek;
   Progress volume/PR/trend/csoport-részletek és main anatómiai maszkok; Journal gazdag
   summary/photo/route és részletek. A már működő metrikák/tag-ek ne tűnjenek el.
5. Aktív edzés összes módja, interval/bilateral/outdoor/finish, majd supporting
   program/calendar/coach/settings/backup/permissions/privatephotos nézetek; a korábbi
   master P2→P3→P4→P5 és G03/G04/G06/G07/G12 bekötési hiányok tételesen megmaradnak.
6. Csomagonként érdemi teszt, végül azonos forrású canonical CI és valódi emulátorPNG.
   Generált/prototípus kép nem natív készülékes bizonyíték, main funkcióminőség minimum.

## Megőrzési keret

Package com.repforge.app, verzió1.3.71-native-test/2736 nem változik.
Main, Room/schema, workout/timer/finalizer, backup/replacement, ownership/sessionguard
és Wear szinkron nem cserélhető Studio demómodellre. Állapot-projekció és UI callback
az actual forrásból. Integráció Wear OS,Google Health Connect,Samsung Health;
kínai órás próba miatt nem készül külön BLE-oldal. Az örökölt BLE-tab végleges
elhelyezése nyitott, meglévő adatok/protokollok nem törölhetők önkényesen.
Nincs kitalált élő pulzus, GPS térkép vagy izomterhelési százalék. GPS szegmensrés
megmarad, demó Margitsziget nem éles térkép. Privát fotó és éles felhasználói adat védett.
Main merge/release/valódi adatok fölé telepítés külön felhasználói elfogadás után.

## Fontos raw források, pontos kódpillanatkép

- `AGENTS.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/AGENTS.md
- `docs/NATIVE_RESUME.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/NATIVE_RESUME.md
- `docs/reviews/NATIVE_AISTUDIO_API_CORRECTIONS_2026-10-27.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/reviews/NATIVE_AISTUDIO_API_CORRECTIONS_2026-10-27.md
- `docs/ai-studio/NATIVE_STUDIO_20261027_TRIAL.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/ai-studio/NATIVE_STUDIO_20261027_TRIAL.md
- `docs/design/NATIVE_VISUAL_MASTER_PLAN_2026-10-27.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/design/NATIVE_VISUAL_MASTER_PLAN_2026-10-27.md
- `docs/plans/NATIVE_MASTER_PLAN_AND_FIXES.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/plans/NATIVE_MASTER_PLAN_AND_FIXES.md
- `docs/reviews/NATIVE_P1_VISUAL_INCREMENT_01.md`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/docs/reviews/NATIVE_P1_VISUAL_INCREMENT_01.md
- `android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/navigation/NativeMainHeader.kt
- `android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt
- `android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/components/TrainPilotSurfaces.kt
- `android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt
- `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt
- `android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt
- `android/app/src/main/java/com/repforge/app/ui/progress/NativeMuscleLoadVisual.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/main/java/com/repforge/app/ui/progress/NativeMuscleLoadVisual.kt
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt`
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9c9ccdee25de941f2f3cb407add687ddbde4f9b8/android/app/src/nativePhoneUiTest/java/com/repforge/app/NativePhoneNavigationTest.kt
