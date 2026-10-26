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
