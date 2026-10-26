# A01 – natív első indítási onboarding átadás (PR250)

Dátum: 2026-10-26  
Repository: `fuloplevente1998/TrainPilot`  
Saját ág: `feat/native-first-launch-onboarding`  
**Exact alap:** `7043fc7d5fac91383f583332fd8fc0e9397f4a1c`  
**App/teszt kód HEAD a riport készítésekor:** `76f72ee1ba0ce7d90fd31e7c7870e7e945b53028`  
A dokumentum hozzáadása újabb `[skip ci]` commit; a teljes végső HEAD a draft PR-ban olvasható.  
PR cél: `feat/native-android-migration`. **Nincs merge, rebase, force push, CI-indítás, Gradle, APK vagy emulátorfuttatás.**

## Referencia és döntés

- Main baseline `7473b30f387eda54186c1e31830304ca12bd104f`: `www/index.html` 2028–2032 a `needsWelcome()` végső boot-dekorátor hívását az adatmigráció/restore utánra halasztja. Csak új/pending, profil nélküli telepítés kap welcome-et; meglévő és skipped állapot nem.
- A `docs/audit-input/main-7473b30/app-part-079.txt` és `app-part-080.txt` tartalmazza a `tp128Welcome`, `tp128BeginOnboarding`, `tp128SkipOnboarding` definíciókat. A natív implementáció a mentési helyzeteket külön `welcome` / `in_progress` / `skipped` / `complete` állapotokra bontja.
- A Settings jelenlegi, közösen használt `TrainingProfileDialog` és `NativeTrainingProfile.edit/verified` változatlan; a cél/gear/excluded/avoidAreas/jövőbeli mezők ugyanazon az adatúton maradnak.

## Követelmény → UI → writer → tartós eredmény

| Követelmény | Natív UI/guard | Meglévő writer + új állapotkapu | Tartós eredmény |
|---|---|---|---|
| Friss welcome + explicit döntés | `NativeOnboardingScreen` welcome; Kezdés és Most kihagyom, 48dp | `transitionOnboarding(expected, ...)` DataStore CAS + `NativeDataReplacement` ticket | `onboarding128=in_progress` vagy `skipped` |
| Skip utáni folytatás | Home `home_resume_onboarding`, welcome újranyitható | `skipped → in_progress`, utána a kanonikus profil-UI | A korábbi skip és későbbi állapot process/Activity restart után is megmarad |
| Profil + validálás | Meglévő `TrainingProfileDialog`; `profile_field_*`, `profile_save` | `NativeTrainingProfile.edit`, egyetlen DataStore edit a profil+complete számára; expected JSON CAS | `trainingProfile128` és `onboarding128=complete` együtt |
| Friss/legacy/restore döntés | AppScaffold csak a migráció flag betöltése után enged Home-ot | `LegacyWebViewMigrator` valódi legacy evidencia; `initializeOnboardingForMigration` a sikeres import után | Friss üres adat → welcome; létező/importált profil vagy legacy adat → nem új felhasználó |
| Backup/erase/interruption | Visszaállítás alatt a writer nem enged be; onboarding alatt nem indul edzés | `NativeDataReplacement.Continuation.requireCurrent`; restore nélküli import nem dobja el a draftot; full restore hiányzó mezőn complete, erase után welcome | Stale editor nem írja felül az új adatot, kész profile/draft nem indul új installként |

## Diff (csak megengedett fájlok)

- `android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt`
- `android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt`
- `android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt`
- `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/onboarding/NativeOnboardingPolicy.kt` (új)
- `android/app/src/main/java/com/repforge/app/ui/onboarding/NativeOnboardingScreen.kt` (új)
- `android/app/src/main/res/values/native_onboarding.xml` (új)
- `android/app/src/main/res/values-en/native_onboarding.xml` (új)
- `android/app/src/main/res/values-de/native_onboarding.xml` (új)
- `android/app/src/main/res/values-ro/native_onboarding.xml` (új)
- `android/app/src/test/java/com/repforge/app/NativeOnboardingStateTest.kt` (új)
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeOnboardingUiTest.kt` (új)
- `docs/reviews/NATIVE_ONBOARDING_HANDOFF.md` (ez)

A `SettingsScreen`, `SettingsViewModel`, `NativeTrainingProfile`, program/calendar, Journal/Health/sync/finalizer, CI, Gradle, main és migration branch érintetlen. A `com.repforge.app` és `1.3.71-native-test /2736` nem változott.

## Új tesztforrások – **nem futtatott PASS**

JVM, `NativeOnboardingStateTest` (7):
1. `distinguishesFreshLegacyAndExistingNativeProfiles`
2. `ignoresSyntheticEmptyLegacyRawButRespectsRealOrUnknownData`
3. `skipAndResumeSurviveStoreRecreationThenSaveTogether`
4. `invalidProfileDoesNotCompleteOrWritePartialData`
5. `staleProfileCannotOverwriteNewerSettingsAndNoProgramIsStarted`
6. `restoredLegacyOrImportedDraftDoesNotLookLikeFreshInstall`
7. `transitionsRejectImplicitCompleteAndDuplicatedSave`

nativePhoneUiTest, `NativeOnboardingUiTest` (2):
1. `welcomeSkipColdRestartThenContinueProfile`
2. `invalidProfileCannotCompleteThenValidProfileSaves`

## Tényleges ellenőrzés

- GitHub compare exact alap→saját ág: csak kijelölt fájllista; nem történt tiltott diff.
- Célzott Kotlin statikus lexical bracket/import-ismétlés ellenőrzés a 8 módosított/új Kotlin fájlon: **0 jelzett probléma**; ez nem Kotlin fordító.
- Négy dedikált lokalizációs XML: **4×10 egyforma string-kulcs**, minden kódbeli `R.string.native_onboarding_*` hivatkozás feloldható a fájlokban. Nincs közös `strings.xml` változtatás.
- **UNRUN:** Gradle, JVM tesztvégrehajtás, natív UI/emulátor, process-kill, legacy/restore device-fixture, R8, signed upgrade, közös CI. A két Android teszt készülékes futtatása az integrátor feladata.

## Nyitott korlát / integrátori ellenőrzési lista

- Valódi tiszta telepítés + valódi régi WebView-adat + importált draft + teljes backup/restore + erase és elakadt restore eseteit a közös kapun külön validálni kell; egy Activity újraindítás nem valódi OS process-kill.
- A `legacyStorageRaw` üres JSON-ja szintetikus, nem valódi korábbi adat. Ismeretlen vagy sérült tároló-nyomot szándékosan konzervatívan meglévőnek tekintünk, hogy ne fedjünk el adatot egy téves welcome-del.
- Az `onboarding128=complete` beállítása nem indít automatikus programgenerálást; a #128 program-preview/coaching út a meglévő natív program-/profil-műveleteknél marad. Ha teljes main preview-paritás szükséges, az külön saját fájlfoglalás/review.
- Minden későbbi integráció előtt exact diff review, majd **csak a fő integrátor** által indított közös CI. Ez a draft nem elfogadott runtime PASS, és nem kér automatikus merge-et.
