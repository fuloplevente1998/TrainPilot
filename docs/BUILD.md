# Build és ellenőrzés

A kanonikus webalkalmazás-forrás a `www/app.js`. Az Android csomag azonosítója: `com.repforge.app`.

Az elfogadott aktuális verzió **1.2.0 /2703**. Kiadási regresszió: 119 Node + 4 futtatóteszt, 60 Chromium-szkript hat párhuzamos CI-jobban, 37 Android/JVM-teszt. A korábbi jelölt APK-k/tag-ek megmaradnak; az új stabil main build a `v1.2.0` release-t publikálja.

## Gyors ellenőrzés

```bash
npm ci
npm test
```

A normál commit/push CI szintaxisellenőrzést és gyors Node regressziós teszteket futtat.

## Teljes Release Gate

A teljes kiadásellenőrzés a `main`, `feat/**` és `fix/**` ágak pusholásakor, verziótag (`v*`) pusholásakor vagy kézzel (`workflow_dispatch`) fut. A Release Gate:

1. ellenőrzi a signing Secrets meglétét;
2. ideiglenesen materializálja a keystore-t;
3. ellenőrzi a verzióadatok konzisztenciáját;
4. lefuttatja a Node regressziós teszteket;
5. lefuttatja a teljes Chromium UI suite-ot;
6. lefuttatja a Capacitor Android syncet;
7. lefuttatja az Android release JVM-teszteket és elkészíti a Gradle release APK-t;
8. ellenőrzi az APK package-, verzió- és aláírásadatait;
9. összehasonlítja az APK-ba csomagolt `www/app.js` fájlt a forrással;
10. elkészíti az APK-t, source ZIP-et, SHA256SUMS és apk-badging artifactokat;
11. `main` vagy `v*` tag esetén GitHub Release-ként is publikálja ezeket.

A mainen új verzióhoz új tag és fájlok készülnek. Már meglévő verzió main buildje csak a kiadási megjegyzéseket frissíti; a korábban publikált bináris fájlokat megtartja. Alkalmazáskód-javításhoz ezért új verzió és növelt Android `versionCode` szükséges.

A workflow teljes Git-historyt kér le, mert egyes regressziós ellenőrzések publikus baseline commitot használnak. A publikus TrainPilot history 1.6.0-nál kezdődik, ezért teszt nem hivatkozhat ennél korábbi, csak legacy/private historyban létező commitra.

## Release signing

Signing kulcs vagy jelszó nem része a repositorynak. Lokális release buildhez ezek szükségesek:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

GitHub Actions alatt a keystore a `ANDROID_KEYSTORE_BASE64` Repository Secretből ideiglenesen készül el a runner temp könyvtárában. A workflow a futás végén eltávolítja.

A GitHub Actions Repository Secretek:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Lokális release build példa:

```bash
export ANDROID_KEYSTORE_PATH=/safe/path/trainpilot-release.jks
export ANDROID_STORE_PASSWORD='...'
export ANDROID_KEY_ALIAS='...'
export ANDROID_KEY_PASSWORD='...'
npm run sync
cd android
./gradlew --no-daemon clean assembleRelease
```

A keystore és a Secret értékek soha ne kerüljenek Gitbe, source ZIP-be vagy GitHub Release assetbe.

## A 1.7.7 → 1.0.0 átállás biztonsági feltételei

- A kiindulási, kiadott APK verziója `1.7.7`, `versionCode 2670`. A következő `1.0.0` APK `versionCode` értéke legalább `2671`.
- Az Android application ID `com.repforge.app`, az eredeti release signer és a meglévő helyi adattárolási azonosítók változatlanok.
- Kiadás előtt az eredeti 1.7.7 APK aláírási tanúsítványának SHA-256 ujjlenyomatát az új APK-val össze kell hasonlítani; szükség van meglévő 1.7.7 telepítés fölé történő, adatmegőrző frissítési próbára is.
- A történeti release-ek és tagek külön privát archiválása nem változtatja meg a publikus `main` commitelőzményét. Lásd [karbantartási terv](REPOSITORY_MAINTENANCE_2026-09-26.md).

The next test candidate is **1.2.2 /2705**, based on accepted main 1.2.0. New native background/source tests run with the same Android JVM gate; the signed candidate APK must retain the existing certificate and pass the complete UI/performance gate before phone testing. [Candidate notes](releases/v1.2.2.md).
