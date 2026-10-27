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

## Pontos állapot

- PR: https://github.com/fuloplevente1998/TrainPilot/pull/270
- UI alkalmazás-/tesztforrás jelölt: `9c9ccdee25de941f2f3cb407add687ddbde4f9b8`.
- A korábbi PR-head8c3336bb + friss migration történet integrálva, nincs forcepush.
- Javítva a felső főfülek selected/Role.Tab és selectableGroup szemantikája.
  Az eredeti NativePhoneNavigationTest.tab() már kivárja a Selected=true állapotot;
  tesztet nem gyengítettünk. Runtime kapu: Actions40535425696,
  exact5bbc916227e1bac0d6e0368f95e269b399ded049, még folyamatban.
  https://github.com/fuloplevente1998/TrainPilot/actions/runs/40535425696
  Ne indíts mellette duplagate-et; a lefutott eredményt és nyers evidence-t olvasd.
- A felső pill elrendezés és a main launcherikon megvan, ez NEM a teljes StudioUI.
- Valódi kiindulási képek8nézetre: docs/design/previews/p1-f0c474f1/README.md;
  még a Studio ELŐTTI alsó navigációval, világosan szintetikus fixture-adatokkal.
- Stabil migrációs appforrás: f0c474f17072aaf6f5c23506e0ad5b1c0092e2ae,
  Actions40529220611 mind7jobSUCCESS. Root raw1333JVM+117phone+183Wear PASS,
  0failure/error/skip/missing/crash;2signedupgradeSUCCESS.65PNG hash/size/PIL validált.
  Ez NEM a PR270 új felső navigációjának elfogadása.

## Kritikus hiányzó bemenet

A TELJES friss Studio export még nincs ebben a workspace-ben és a PR diffjében.
Ismert név: trainpilot-oct-27-2026-3-58-04-pm.zip;
SHA256:574f17f639b91037a6e0bdea428f350d4c5950307227511eafc32e7f64bcefcc.
A másik chat csak a4fájlos topnav/iconpróbát publikálta; ez nem a teljes forrás.
A felhasználótól az export GitHub URL-jét vagy repóbeli helyét bekértük, válasz függőben.
Ha megérkezik, őrizd meg az eredeti forrás/provenance-t és SHA-t GitHubon,
olvasható kis fájlokkal/raw linkindexszel; ne adj új többmillió tokenes repoZIP-et.
Ne találj ki egy másik dizájnt az export helyett. Az ismert régebbi MD-spec alább
használható a meglévő minimumhoz, de nem igazolja az új StudioUI pontos megjelenését.

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
