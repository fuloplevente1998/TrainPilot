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

## Aktuális stabil alap: 1.7.7

- Legutóbbi publikált kiadás: **[v1.7.7](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.7.7)** (2026-09-26).
- Android `versionCode`: **2670**; `versionName`: **1.7.7**.
- Elfogadott kiadási alap: `main`, commit `61b7d2008740ffd56d352736f10aa93ab8c2e08c`.
- Android application ID: `com.repforge.app`.
- Kanonikus webalkalmazás-forrás: `www/app.js`.
- A #60, #61, #62 és #63 feladatok a 1.7.7-es alapban lezárva.

Az alkalmazás átnevezése **TrainPilot 1.0.0**-ra külön fejlesztési/ellenőrzési körben történik. A következő APK-nál a `versionName` lehet `1.0.0`, de az Android `versionCode` értékét legalább **2671**-re kell növelni. Ez a dokumentációs frissítés még nem módosítja a telepíthető alkalmazás verzióját.

A `com.repforge.app`, egyes `repforge:*` helyi adattárolási kulcsok és régi backup/szinkron azonosítók szándékosan megmaradnak a korábbi telepítésekkel és felhasználói adatokkal való kompatibilitás miatt. Az eredeti release-aláírásnak a frissítésnél is változatlannak kell maradnia.

## Git-történet és privát archívum

A publikus repository története a **TrainPilot 1.6.0** clean base állapottól indul. A korábbi privát/legacy előzmények nem részei ennek a Git-történetnek. A publikus `main` teljes meglévő commitelőzménye megmarad.

A 2026-09-26-i takarítás előtt a külön, **privát `TrainPilot-Archive`** repositoryba átmásoltuk és összehasonlítottuk a forrás Git-ágait és tageit, valamint a 17 régebbi GitHub Release-t és azok 62 fájlját. Egy további Google Drive-mentés is létezik. A privát archívum nem a nyilvános letöltési hely. További információ: [repository-karbantartási terv](docs/REPOSITORY_MAINTENANCE_2026-09-26.md).

A régi tagek és mellékágak publikusból való eltávolítása nem jelent Git-history-újraírást: a jelenlegi `main` előzményeit nem squasholjuk vagy force-pusholjuk.

## Fejlesztési és kiadási modell

- `main`: stabil, ellenőrzött állapot;
- rövid életű `feat/*`, `fix/*`, `maintenance/*` ágak: célzott módosítások;
- normál commit/push: gyors regressziós ellenőrzések;
- teljes Release Gate: Node + Chromium regresszió, aláírt Android APK, package/source és checksum ellenőrzés;
- alkalmazásfunkció módosítása esetén **telefonos jóváhagyás után** történhet merge a `main` ágra.

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
