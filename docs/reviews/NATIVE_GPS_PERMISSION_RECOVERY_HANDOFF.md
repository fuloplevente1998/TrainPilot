# PR #256 – GPS recovery QA-folytatás (2026-10-26)

## Pontos változási alap és határ

- Folytatott saját ág: `feat/native-gps-permission-recovery`; PR: https://github.com/fuloplevente1998/TrainPilot/pull/256 (draft, nyitott).
- QA előtti exact HEAD: `a3ee639cfbbce5251c89ff6d9f8a6148860ff0ce`. Az eredeti pinned kiindulás `e28ab41887a976ee380f63f465dbdcb51f494329` megmarad.
- Utolsó **kódszintű** QA commit: `6546afeae981246190b631ef504cadd51edf3e3c`. A végső, riportot is tartalmazó HEAD-et a PR saját ágának refje adja meg.
- Main és `feat/native-android-migration` írás/merge/rebase/force push nélkül. Nincs verzió, dependency, schema, Wear, service, route, Settings/NavHost vagy canonical CI módosítás.
- A QA után új diff: `ActiveWorkoutScreen.kt`, `NativeGpsPermissionRecovery.kt`, mind a négy saját `native_gps_recovery.xml`, `NativeGpsPermissionRecoveryDecisionTest.kt`, új `.github/workflows/native-gps-recovery-qa.yml`, és ez a saját review.

## Független QA és konkrét javítás

1. **Valódi bug javítva:** az ActivityResult launcher függőben lévő GPS-célja eddig `remember` állapotban elveszhetett forgatás után. Most `rememberSaveable`-ben kizárólag a stabil eredeti workout/generation/exercise/set identitásának memóriabeli JSON reprezentációja marad fenn; ez nem kerül tartós Store-ba vagy GPS indítási queue-ba. A callback újra ellenőrzi az eredeti célt.
2. **Race javítva:** `gpsPermissionRequestInFlight` egyetlen platform launchert enged. Mégse után a korábbi pending identity törlődik, **de az in-flight lease csak a régi callback érkezésekor oldódik fel**, így a késői callback nem használhat új kérés célt. Forgatás nem indít ismételt rendszerengedély-kérést.
3. A cél ellenőrzése előtt most külön `NativeDataReplacement.blocked`, `PhoneWorkoutExecution.restoreBlocked/commandsSuspended`, `PhoneOwnership193.phoneAllowed` ellenőrzés van. Ezt követi változatlanul a meglévő writer tényleges admission/CAS védelme. Stale/pause/finish/watch owner esetben sem a UI sem a callback nem hívja a GPS writert.
4. Hiányzó/tematizált Activity esetén `ContextWrapper` unwrap, nincs összeomló permission launch; sikertelen Activity/Settings indításnál lokalizált hibaüzenet. Négy nyelven immár **14 azonos** saját szövegkulcs.
5. `ON_RESUME` továbbra kizárólag a pontos helyengedély + központi GPS szolgáltatás aktuális állapotát frissíti; sem ott, sem Back/Mégse esetén nem indul új mérés. A sikeres explicit újrapróbálás az eredeti `toggleGpsTracking → NativeWorkoutEditing.startGps → NativeGpsMeasurement/PhoneWorkoutExecution → DistanceTrackingService` változatlan adatutat használja.

## Megírt tesztforrás

- **11 JVM/Robolectric teszt deklarálva:** `NativeGpsPermissionRecoveryDecisionTest`; a meglévő missing/stale eset kiegészítve az új saved target encode/decode invariánssal.
- **4 Android Compose UI teszt deklarálva:** `NativeGpsPermissionRecoveryUiTest`; izolált emulátoros permission/system service toggle és `@After` cleanup.
- Ezek továbbra is **tesztforrások**. Nincs tényleges JVM/Compose PASS, nem állítunk saját készülékes GPS/OEM elfogadást.

## TÉNYLEGESEN elvégzett ellenőrzések

- **16/16** célzott GitHub-forráson futtatott, ellenőrző kóddal végzett *statikus* check PASS: négy lokál kulcsazonosság (14/nyelv), mind a 14 felhasznált resource ID létezik, saved target/egy launcher/cancel lease/owner guard/ON_RESUME/missing activity logika, 11 és 4 tesztdeklaráció, manuális-only workflow, pontos szűrők és artifact/log/XML konfiguráció.
- GitHub compare az átadott `a3ee639c` és a kódszintű `6546afea` között: kizárólag a kijelölt GPS-helyreállítási területek + az új QA workflow, nincs base/rebase módosítás.
- Android SDK/Gradle/isolated emulator a jelenlegi futtatókörnyezetben **nem áll rendelkezésre** (`android-sdk-absent`; a GitHub host DNS nem elérhető). A csatlakoztatott GitHub-műveletek a repo fájl/PR írást engedik, **Actions workflow dispatch műveletet nem kínálnak**.
- Az új workflow `workflow_dispatch`-only: `.github/workflows/native-gps-recovery-qa.yml`; terv szerint Java 21/SDK 36 debug + androidTest APK compile, **csak** `NativeGpsPermissionRecoveryDecisionTest` 11 JVM eset + API 35 emulátoron **csak** `NativeGpsPermissionRecoveryUiTest` négy eset; exact checkout, Gradle XML, teljes instrumentation log, logcat, QA JUnit XML, JSON summary, commit és SHA256 artifact. Sem canonical teljes CI, sem Wear/R8/upgrade nem része.
- **Valós GitHub blocker:** a `workflow_dispatch` csak akkor indítható, ha a workflow-fájl jelen van a **default branch-en** (GitHub Docs: https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch). Az új workflow **nincs** a mainen (404 read-only ellenőrzés); a main/migration módosítása tiltott, ezért ezen scope-ból nem indítható. A canonical native Android workflow teljes kapuja nem megengedett alternatíva.
- **Tényleges JVM: 0/11 futott; Compose UI: 0/4 futott; APK fordítás: NEM FUTOTT; GitHub Actions run link: NINCS; XML/logcat/artifact: NINCS.** Az előbbi forrásvizsgálatot tilos runtime zöldként értelmezni.

## Elmaradó elfogadás

Célzott APK fordítás és teljes 11 JVM + 4 API35 UI *valódi* végrehajtása/hibakeresése csak jogosult Android CI futtatóval vagy a default branchen regisztrált, scope-szűrhető runnerrel lehetséges. Ehhez Root döntés kell, de nincs előírva main módosítás vagy PR merge. Valódi GPS-fix és OEM háttérműködés külön fizikai telefonos elfogadás marad. Élő felhasználói adatra semmi nem települt.

---

## Korábbi alapátadás (történeti állapot, a fenti QA-rész a mérvadó)

# Native GPS permission recovery – külön ági átadás

Dátum: 2026-10-26

## Forrás / munkaszerződés

- Repository: `fuloplevente1998/TrainPilot`
- Exact induló commit: `e28ab41887a976ee380f63f465dbdcb51f494329`
- Saját ág: `feat/native-gps-permission-recovery`
- Draft PR cél: `feat/native-android-migration`; nincs merge, rebase, force push vagy root CI-dispatch.
- Megtartott csomag: `com.repforge.app`; verzió `1.3.71-native-test`, code `2736`.
- Az új kód kizárólag a szerződésben engedélyezett GPS-képernyő, saját helper, saját nyelvi XML és saját tesztfájlok területén van.

## Kód és adatutak

1. A `DistanceMeasurementPanel` explicit GPS gombja és az egyszer elfogyasztott `launchGps` továbbra is az eredeti `NativeWorkoutTarget(workoutId,generation,exerciseId,setId)` azonosítót viszi a `requestGps` függvényhez.
2. A `NativeGpsPermissionRecovery.read` *külön* vizsgálja a FINE/COARSE Android grantot, a `shouldShowRequestPermissionRationale` eredményét és a telefon GPS providerét. Lehetséges eredmény: READY / DENIED / APPROXIMATE / PERMANENTLY_DENIED / LOCATION_OFF. A hozzávetőleges hely soha nem jogosít fel GPS-indításra.
3. Előzetes DENIED esetben a **meglévő** `RequestMultiplePermissions` Activity Result launcher fut. A felhasználó Mégse/Back művelete és a sikertelen callback nem indít GPS-t. A képernyő kontextusosan jelzi az okot, alkalmazásengedély vagy rendszerhely-beállításra nyitó natív Android gombbal, illetve explicit újrapróbálással.
4. `ON_RESUME` kizárólag a tényleges platformállapotot olvassa újra; nem hívja a GPS startot. A felhasználónak újra meg kell nyomnia a saját GPS újrapróbálás gombját.
5. Minden első indításnál, launcher callbacknél és újrapróbálásnál `targetIsCurrent` ellenőrzi a tartós `ActiveWorkoutStore` aktuális edzését, generációját, gyakorlatát és sorozatát; megköveteli a még aktív, telefon által birtokolt, nem szünetelő, nem finalizálódó és nem kész sorozatot. Az elavult cél csak hibaüzenetet kap.
6. **Writer változatlan:** engedélyezett explicit indítás → `ActiveWorkoutViewModel.toggleGpsTracking(target)` → `NativeWorkoutEditing.startGps` → meglévő `NativeGpsMeasurement.prepare` / `commitEdit` / `PhoneWorkoutExecution` → `DistanceTrackingService`. Sorozatlezárás továbbra a meglévő `finishGps` úton történik, nem a permission visszaúton.
7. Sem útvonal/GPS-pontszűrés, sem service, Wear, háttérengedély, navigation, Settings, Store vagy finalizer nem módosult. A képernyő eredeti stopper/interval/ownership folyamatába nincs új párhuzamos vezérlő. Lokalizáció külön `native_gps_recovery.xml` HU/EN/DE/RO.

## Diff

- `android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt`
- `android/app/src/main/res/values/native_gps_recovery.xml`
- `android/app/src/main/res/values-en/native_gps_recovery.xml`
- `android/app/src/main/res/values-de/native_gps_recovery.xml`
- `android/app/src/main/res/values-ro/native_gps_recovery.xml`
- `android/app/src/test/java/com/repforge/app/NativeGpsPermissionRecoveryDecisionTest.kt`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeGpsPermissionRecoveryUiTest.kt`
- `docs/reviews/NATIVE_GPS_PERMISSION_RECOVERY_HANDOFF.md`

## Megírt tesztek – NEM runtime PASS

- `NativeGpsPermissionRecoveryDecisionTest`: **11 JVM/Robolectric eset** (DENIED első/második; approximate; permanently denied; GPS OFF; settings return; nincs/másik workout és generation; törölt/dupla set; pause/owner/finalizer; kész vagy másik gyakorlat; érvényes eredeti cél; kétféle natív Settings Intent).
- `NativeGpsPermissionRecoveryUiTest`: **4 instrumentált Compose-eset** meglévő `NativeLocalizationEmulatorFixture`-rel: approximate→app details→Back/Mégse; GPS OFF→helybeállítás→ON_RESUME→explicit retry; rendszerengedély revoke/kérés→Back; elavult edzés→retry elutasítva. A fixture csak debug emulátort enged; a platform permission / location kapcsolókat `@After` visszaállítja; valódi fiók, élő adat és GPS-fix nem fixture.

Javasolt **későbbi** célzott futtatás a GitHub Android build/emulátor környezetében (a közös workflow-gazda koordinálja):

```sh
cd android
./gradlew :app:testDebugUnitTest --tests 'com.repforge.app.NativeGpsPermissionRecoveryDecisionTest'
./gradlew :app:connectedDebugAndroidTest -PnativePhoneUiTests \
  -Pandroid.testInstrumentationRunnerArguments.class=com.repforge.app.NativeGpsPermissionRecoveryUiTest
```

## Ténylegesen futtatott ellenőrzések – NEM keverendők az előzővel

- GitHub compare: az induló `e28ab418` merge-base pontosan az előírt commit; az új ág erre épül, a diff csak engedélyezett fájlokat tartalmaz.
- **14/14 forrásellenőrzés PASS**: négy saját XML erőforráskulcs-lista egyezik (13 kulcs/nyelv); mind a 13 `R.string`-hivatkozás létezik; explicit precise+target guard; visszatérés csak UI-frissítés; teszteset-darabszámok; négy Kotlin-forrás egyszerű lexikai zárójelegyensúlya. Ezek gyors, szövegalapú ellenőrzések, **nem** Kotlin compiler, Android resource linker vagy Compose runtime tesztek.
- **JVM-tesztek futtatva: 0 / 11; UI-tesztek futtatva: 0 / 4.** Nincs saját Android build/APK, instrumentációs XML, artifact vagy SHA256. Nem állítható natív compile PASS, UI PASS vagy készülékes elfogadás. Új GitHub Actions-t, teljes CI-t és emulátort nem indítottam.

## Nyitott kockázatok / Root átadás

- Kotlin/Compose fordítás és az instrumentált UI gomb-/rendszerút tényleges futtatása még szükséges a közös Android környezetben. Különösen az OEM-permission rationale, Android-Settings visszatérési navigáció és emulator `cmd location` támogatását valós futás igazolja.
- Valódi műholdas fix, előtérszolgáltatás OEM viselkedés, háttér/akkukezelés és készüléken történő adatmegőrzés külön acceptance, jelenleg **nem igazolt**.
- A permission-recovery nem szinkronizációs/Store író; sikeres GPS-indítás csak a már létező writer adatútját használja. A hibás/denied/Back ág nem készít mérést vagy sorozatrekordot.

**Státusz:** elkülönített code+tesztforrás review-ra átadható; runtime ellenőrzés és root integráció hátra. Draft PR leírás tartalmazza a jelen állapotot; PR nem merge-elve.
