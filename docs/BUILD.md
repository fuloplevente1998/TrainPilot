# Build és ellenőrzés

**Legfrissebb P0 telefonos részeredmény és hibakereső teszt-APK:** `0bcc1ea33a08cc9ba372f744e431b7b17f0fd337`, változatlan **1.3.4 /2730**. A `/kar` és `/láb` egységek natív rögzítése javítva; az előkészítés/élesítés hibája `E162-...` kódot kap. A minden edzésnél jelzett közös készülékes háttérhiba még nincs igazoltan megoldva. A teljes telefonos gate 91 Chromium-csomaggal, natív release tesztekkel sikeres; az órás `859a6a1` APK tovább használható. Aktuális letöltés/hash/CI/próba: [P0 átadási lap](P0_KESZULEKES_ELFOGADAS_162_157.md).

A kanonikus webalkalmazás-forrás a `www/app.js`. Az Android csomag azonosítója: `com.repforge.app`.

Az aktuális main fejlesztési bázisa **1.3.4 / 2730**, a #179-ben egyesített telefonos/Wear megvalósítással. Az eredeti 1.3.4 release és a legutóbbi automata tesztpár külön forrásazonosítóval rendelkezik; aktuális részletek: [DEVELOPMENT_STATUS.md](../DEVELOPMENT_STATUS.md).

**Korábbi közös P0 branch tesztpár:** #195, `fix/p0-162-157-safe-wear-sync`, közös APK-forrás **`859a6a15d1d7d02ebbfaba4385e857eb9e824014`**, változatlan 1.3.4 /2730 verzió. A teljes telefonos és Wear release gate, 140 Node-csomag, 90 Chromium-csomag és teljesítményellenőrzés sikeres; a változatlan Wear-forrás 51 kerek emulátoros próbája sikeres. A két telepítő eredeti aláírással, branch artifactként készült. A main és a kiadott assetek változatlanok; készülékes elfogadás nincs. Pontos letöltés, hash, CI-forrás és próbamenet: [P0 átadási lap](P0_KESZULEKES_ELFOGADAS_162_157.md).

A legutóbbi ellenőrzött kódon 138 telefonos regresszió, külön futtatótesztek, 90 Chromium-tesztcsomag hat CI-jobban, natív telefon/Wear release-tesztek és 51 Wear-emulátoros próba sikeres. A pontos tesztlista a `tests/browser/suite.json`, az aktuális futások a GitHub Actionsön találhatók. A fizikai rezgés és OEM háttérkezelés elfogadása külön készülékes próba.

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

A mainen új verzióhoz új tag és fájlok készülnek. Már meglévő verzió main buildje csak a kiadási megjegyzéseket frissíti; a korábban publikált bináris fájlokat megtartja. Új publikált kiadáshoz új verzió és növelt Android `versionCode` szükséges. Köztes teszt-APK ugyanazon verzióval is készülhet; ilyenkor a forráscommit és SHA-256 azonosítja, és a meglévő release fájljait nem cseréljük le.

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

Történeti adat: az 1.2.8 /2711 main/release-jóváhagyása a [#139](https://github.com/fuloplevente1998/TrainPilot/pull/139) r5 telefonos elfogadásán alapul (2026-10-07); kiadási megjegyzések: [v1.2.8](releases/v1.2.8.md). A kiadott taghez tartozó pontos APK/forrás párost és eredeti aláírást a main gate után külön is ellenőrizzük.
