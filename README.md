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

## Aktuális kiadás: TrainPilot 1.0.5 (2687)

Az Android `versionName` **1.0.5**, a `versionCode` **2687**. A kiadás megtartja a korábbi telepítések alkalmazásazonosítóját és aláírását.

- [v1.0.5 Release és kiadási megjegyzések](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.5)
- [TrainPilot-1.0.5.apk](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.5/TrainPilot-1.0.5.apk)
- [Forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.5/TrainPilot-1.0.5-source.zip)
- [SHA-256 ellenőrzőösszegek](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.5/SHA256SUMS.txt)

Az 1.0.5 fő változásai:

- A helyi ZIP-mentés automatikus Drive-/naptárszinkron és hálózati hiba közben is elindítható. A Drive helyi adatfrissítése megvárja a mentés végét; a fotók törlése csak a sikeresen átvett törlési adatok után történik.
- A ZIP tartalmazza a mentésben hivatkozott helyi naplófotókat és az ellenőrzésükhöz szükséges SHA-256 jegyzéket. A visszaállítás ellenőrzött, megszakadás esetén helyreállítható művelet.
- A Health Connect-adatok mentése külön, alapértelmezetten kikapcsolt engedélyhez kötött; a jelölőnégyzet követi az alkalmazás témáját.
- A Drive az egyéni programokat, tervezési beállításokat, kedvenceket, témát és nyelvet is átveszi. A régi naplóbejegyzések helyreállítási és törlési szabályai megmaradnak.
- Külön adatkezelési gombok, többnyelvű adatvédelmi tájékoztató, javított Google-hibaüzenetek és biztonsági függőségfrissítés kerültek be.

A GitHub APK kiadás nem jelenti a Google Play vagy a Google OAuth nyilvános beállításainak elkészültét. A Google Drive/Naptár minden fiókra kiterjedő használatához a megfelelő Cloud-projektet, tanúsítványokat és engedélyezést külön be kell állítani. Részletek: [1.0.5 megvalósítás és publikálási teendők](docs/PUBLICATION_1_0_5.md).

A `com.repforge.app`, egyes `repforge:*` helyi adattárolási kulcsok és régi backup/szinkron azonosítók szándékosan megmaradnak a korábbi telepítésekkel és felhasználói adatokkal való kompatibilitás miatt. Az eredeti release-aláírásnak a frissítésnél is változatlannak kell maradnia.

## Következő tesztváltozat: 1.0.6 (2690)

A saját gyakorlat és program létrehozása, illetve a program szerkesztése teljes keretű lebegő panelben nyílik meg, a fejlécen belül maradó piros X-szel és témához igazodó vezérlőkkel. A korábbi 100 gyakorlat mellé hat távolságos mozgás került: futás, kocogás, görkorcsolyázás, kerékpározás, séta és túrázás. Ezek a Gyors edzésben közvetlenül kereshetők, és saját programba is felvehetők. Az új mérési típusok: ismétlés, idő, illetve idő és távolság. A kilométer kézzel is megadható és szerkeszthető a naplóban; Androidon külön indított GPS-mérés is készül, csak összesített idő- és távadattal, útvonaltárolás nélkül.

Ez fejlesztési tesztváltozat, nem publikált Release. A GPS valós kültéri pontossága, képernyőzár, engedélyek és frissítés készülékes ellenőrzésre várnak. [Megvalósítás és tesztlépések](docs/CUSTOM_EXERCISE_DISTANCE_1_0_6.md).

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
