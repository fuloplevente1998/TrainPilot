# TrainPilot

TrainPilot egy Androidra készült, local-first edzéstervező és edzésnapló alkalmazás. A fő edzésfunkciók helyben is használhatók; a Health Connect, Google Drive és Google Naptár integrációk opcionálisak.

## Fő funkciók

- személyre szabható edzésprogramok és gyors edzés;
- edzésnapló sorozatokkal, ismétlésekkel, terheléssel és időadatokkal;
- progresszív edzésjavaslatok a korábbi teljesítmény és visszajelzések alapján;
- TrainPilot Coach és edzés/statisztikai összesítések;
- beépített pihenőidőzítő és kétoldalas gyakorlatokhoz bal/jobb stopper;
- gyakorlatkönyvtár és saját gyakorlatok;
- opcionális edzésfotók privát app-tárhelyen;
- Health Connect integráció edzés-, pulzus-, energia-, testsúly-, alvás- és más támogatott fitneszadatokhoz;
- Google Drive `appDataFolder` alapú mentés és opcionális fotószinkron;
- Google Naptár szinkron a tervezett edzésekhez;
- többnyelvű felület és mobilra optimalizált Android/WebView UI.

A Google-funkciókhoz külön Google Cloud/OAuth konfiguráció szükséges. A Health Connect és a Google-integrációk tényleges működése eszköz-, Android-verzió-, jogosultság- és fiókbeállítás-függő, ezért kiadás előtt fizikai készülékes ellenőrzés szükséges.

## Aktuális kiadási ág

A jelenlegi kiadásra jelölt verzió:

- verzió: **1.6.2**
- Android `versionCode`: **2651**
- Android package / application ID: `com.repforge.app`
- kanonikus webalkalmazás-forrás: `www/app.js`

A `com.repforge.app`, egyes `repforge:*` helyi adattárolási kulcsok és régi backup/szinkron azonosítók szándékosan megmaradtak a korábbi telepítésekkel és felhasználói adatokkal való kompatibilitás miatt. Ezek nem a publikus terméknév részei.

## Clean source history

A publikus repository története a **TrainPilot 1.6.0** clean base állapottól indul. A korábbi privát/legacy fejlesztési history nincs átemelve ebbe a repositoryba.

A repositoryban maradt egyes regressziós tesztek régebbi funkcióverziókra hivatkozhatnak, de publikus Git-history összehasonlítást csak a clean 1.6.0 base vagy újabb commit ellen végezhetnek.

## Fejlesztési és kiadási modell

- `main`: stabil / kiadásra jelölt állapot;
- feature/test branchek: célzott módosítások és telefonos tesztre szánt release candidate-ek;
- normál commit/push: szintaxis- és gyors regressziós ellenőrzések;
- Release Gate: teljes Node + Chromium UI regresszió, Android release APK build, APK/package ellenőrzés és source ZIP;
- `v*` tag: sikeres Release Gate után GitHub Release készül az APK, source ZIP és SHA256 fájlokkal.

## Helyi build és teszt

```bash
npm ci
npm test
npm run test:ui
npm run sync
```

A részletes build- és signing-leírás: `docs/BUILD.md`.

## Release artifactok

A Release Gate az alábbi fájlokat állítja elő:

- `TrainPilot-<verzió>.apk`
- `TrainPilot-<verzió>-source.zip`
- `SHA256SUMS.txt`
- `apk-badging.txt`

A source ZIP nem tartalmaz signing kulcsot vagy GitHub Secret értékeket.

## Signing és Secrets

A signing kulcs nincs a repositoryban. A GitHub Actions az alábbi Repository Secret neveket használja:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Signing kulcsot, jelszót, OAuth credentialt, `.env` fájlt vagy más titkos adatot tilos commitolni.

## Google Play állapot

A Play Store publikálás előkészítése folyamatban van. A release/signing workflow és a szükséges Secrets konfigurálva vannak; az 1.6.1 stabil alap a `main` ágon; az 1.6.2 minor UI patch telefonos és Release Gate ellenőrzése következik. A Play App Signing, OAuth production konfiguráció, privacy/Data Safety/Health deklarációk és a fizikai készülékes integrációs tesztek külön kiadási feladatok.

Lásd: `docs/PLAY_STORE_CHECKLIST_2026.md`.
