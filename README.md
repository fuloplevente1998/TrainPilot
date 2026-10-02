# TrainPilot

TrainPilot egy Androidra készült, local-first edzéstervező és edzésnapló alkalmazás. A fő edzésfunkciók helyben is használhatók; a Health Connect, Google Drive és Google Naptár integrációk opcionálisak.

## Fő funkciók

- személyre szabható edzésprogramok és gyors edzés;
- edzésnapló sorozatokkal, ismétlésekkel, terheléssel és időadatokkal;
- fejlődési nézet, függőleges oszlopos volumentrend, naplóalapú mutatók és részletek;
- izomábra, TrainPilot Coach, statisztikai összesítések;
- beépített pihenőidőzítő és kétoldalas gyakorlatokhoz bal/jobb stopper;
- gyakorlatkönyvtár és saját gyakorlatok;
- opcionális edzésfotók privát app-tárhelyen;
- Health Connect integráció az alkalmazás által támogatott fitneszadatokhoz;
- Google Drive `appDataFolder` alapú mentés és opcionális fotószinkron;
- Google Naptár szinkron a tervezett edzésekhez;
- többnyelvű felület és mobilra optimalizált Android/WebView UI.

A Google-funkciókhoz külön Google Cloud/OAuth konfiguráció szükséges. A Health Connect és a Google-integrációk tényleges működése eszköz-, Android-verzió-, jogosultság- és fiókbeállítás-függő; kiadás előtt fizikai készülékes ellenőrzés szükséges.

## Aktuális kiadás: TrainPilot 1.0.6 (2690)

Az Android `versionName` **1.0.6**, a `versionCode` **2690**. A kiadás megtartja a korábbi telepítések alkalmazásazonosítóját és aláírását.

- [v1.0.6 Release és kiadási megjegyzések](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.6)
- [TrainPilot-1.0.6.apk](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.6/TrainPilot-1.0.6.apk)
- [Forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.6/TrainPilot-1.0.6-source.zip)
- [SHA-256 ellenőrzőösszegek](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.6/SHA256SUMS.txt)

Az 1.0.6 fő változásai:

- A saját gyakorlat és saját program létrehozása/szerkesztése egységes, keretezett overlayt használ, fejlécen belüli piros X-szel.
- A korábbi 100 gyakorlat mellé hat távolságos mozgás került: futás, kocogás, görkorcsolyázás, kerékpározás, séta és túrázás.
- Új mérési módok: ismétlés, idő, valamint idő és távolság. A kilométer kézzel megadható és a Naplóban szerkeszthető.
- Androidon opcionális GPS-mérés használható. A rendszer csak összesített időt/távolságot tart meg; útvonal-koordinátát nem ment.
- A Health Connect az edzés aktív szakaszaira lépésszámot is olvas. Hiányzó `READ_STEPS` jogosultságnál külön engedélykérés jelenik meg.
- Beltéri távolságos edzés GPS nélkül, csak idővel is rögzíthető; a lépésekből nem becsülünk GPS-kilométert.

A GitHub APK kiadás nem jelenti a Google Play vagy a Google OAuth nyilvános beállításainak elkészültét. A foreground-location, Data safety, privacy és Cloud/OAuth konfiguráció külön publikálási feladat.

Részletes megvalósítás és készülékes ellenőrzési pontok: [TrainPilot 1.0.6 — own exercises and distance](docs/CUSTOM_EXERCISE_DISTANCE_1_0_6.md).


## Git-történet és privát archívum

A publikus repository története a **TrainPilot 1.6.0** clean base állapottól indul. A korábbi privát/legacy előzmények nem részei ennek a Git-történetnek. A publikus `main` teljes meglévő commitelőzménye megmarad.

A régi nyilvános ágak, tagek és Release-ek tisztítása előtt a külön, **privát `TrainPilot-Archive`** repositoryba átmásoltuk a teljes Git-történetet és az 1.0.0 fájljait; a 17 régebbi Release és assetjeik is ott szerepelnek. A tisztítás után újabb kiadások is készültek; a korábbi publikus 1.0.x Release-ek megmaradnak. További információ: [repository-karbantartási terv](docs/REPOSITORY_MAINTENANCE_2026-09-26.md).

A régi tagek és mellékágak publikusból való eltávolítása nem jelent Git-history-újraírást: a jelenlegi `main` előzményeit nem squasholjuk vagy force-pusholjuk.

## Fejlesztési és kiadási modell

- `main`: stabil, ellenőrzött állapot;
- rövid életű `feat/*`, `fix/*`, `maintenance/*` ágak: célzott módosítások;
- normál commit/push: gyors regressziós ellenőrzések;
- teljes Release Gate: Node + Chromium regresszió, aláírt Android APK, package/source és checksum ellenőrzés;
- alkalmazásfunkció módosítása esetén felhasználói jóváhagyás után történhet merge a `main` ágra; a CI és a készülékes teszt eredményeit külön rögzítjük.

Helyi ellenőrzés:

```bash
npm ci
npm test
npm run test:ui
npm run sync
```

Részletek: [build és signing](docs/BUILD.md), [fejlesztési állapot](DEVELOPMENT_STATUS.md), [1.0.0 átállási terv](docs/REPOSITORY_MAINTENANCE_2026-09-26.md).

## Signing és titkok

A signing kulcs és a jelszavak nincsenek a repositoryban. A GitHub Actions a következő Repository Secret neveket használja:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Signing kulcsot, jelszót, OAuth credentialt, `.env` fájlt vagy titkosítatlan helyreállítási mentést tilos commitolni. A privát archívum **sem** tartalmazhat signing titkokat.

A Google Play publikáláshoz hátralévő lépéseket külön [ellenőrzőlista](docs/PLAY_STORE_CHECKLIST_2026.md) tartalmazza.

A 2690-es teszt-APK az edzés aktív szakaszaihoz Health Connect-lépésszámot is lekér és naplóz. Beltéren idővel, GPS-táv nélkül is rögzíthető a mozgás; a lépésekből nem becsül kilométert. A forrás megosztási késése miatt a napló Health-blokkja később frissíthető.
