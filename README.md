# TrainPilot

**RDFit adatpróba — 1.1.3 / 2701:** a GT4Pro+ telefonos diagnosztikája igazolta a kapcsolódást. Külön gombbal próbálható az akkumulátor és a mai lépésszám kiolvasása az RDFit adatcsatornáján. Az értékek csak a próbanézetben jelennek meg; a tényleges válaszok telefonos ellenőrzése még hátravan. [Lépések és protokoll](docs/BLUETOOTH_GT4PRO.md).

**Bluetooth-próba javítása — 1.1.2 / 2700:** a névtelen eszközök mellett helyileg látható a MAC-cím az RDFit adataival való összehasonlításhoz. Sorszám és óvatos protokolljelölés segíti a választást; a találati lista kapcsolódás nélkül is menthető. [Telefonos lépések](docs/BLUETOOTH_GT4PRO.md). A teljes Chromium-tesztsor a gyorsított, hatrészes CI-ban fut.

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

## Aktuális kiadás: TrainPilot 1.0.9 (2697)

Az Android `versionName` **1.0.9**, a `versionCode` **2697**. A javítókiadás megtartja a korábbi telepítések alkalmazásazonosítóját és aláírását; az 1.0.8 / 2696-ra és a korábbi verziókra is rátelepíthető. A főoldalak a Coach oldal halvány keretét és egységes szélességét használják; a Napló fülsávja váltáskor a helyén marad, a Fejlődés részletei közös nyilakat kaptak ([#101](https://github.com/fuloplevente1998/TrainPilot/issues/101)). A témaválasztó mindkét oszlopa fölött működik a görgetés, így az alsó színek is kiválaszthatók ([#99](https://github.com/fuloplevente1998/TrainPilot/issues/99)).

- [v1.0.9 Release és kiadási megjegyzések](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.9)
- [TrainPilot-1.0.9.apk](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.9/TrainPilot-1.0.9.apk)
- [Forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.9/TrainPilot-1.0.9-source.zip)
- [SHA-256 ellenőrzőösszegek](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.9/SHA256SUMS.txt)
- [Android csomag- és verzióadatok](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.0.9/apk-badging.txt)

Az 1.0.7 fő változásai:

- Az Edzés és a Programok oldal közös, lenyitáskor betöltődő szerkesztőt használ. A **Mentés** gomb egyszerre menti és azonnal megjeleníti az új értékeket.
- A Napló **Edzésnapló**, **Fejlődés** és **Statisztikák** füleket kapott. A Statisztikákon belül külön érhetők el a Személyes rekordok és a Kardióstatisztika.
- A kardió heti kilométert, előző heti és nyolchetes összesítést, távolsággal súlyozott tempót és azonos mozgás/azonos távolság futórekordjait mutatja.
- Új, **GPS nélküli beltéri időmérő**: indítás, szünet, folytatás és nullázás, kézi kilométerrel és újraindítás után is megőrzött edzésállapottal.
- A Kezdőlap, a Naptár és a csukott Egészség oldal telefonon fix. A személyes tervező csak megkezdett edzésnél kompakt; az Egészség lenyitáskor megtartja a szélességét és görgethetővé válik.
- A Coach kártya egységes keretet és témaszínt kapott. A kompakt Fejlődés összehasonlítása jobb felül jelenik meg; a témaválasztó a nyelvválasztó nyilát és animációját használja.
- Biztonságosabb backup-visszaállítás, stabil GPS-sorozatazonosítók és az ismétlődő backup-kód összevonása.

A 2695-ös teszt-APK átadása után a felhasználó 2026. október 3-án jóváhagyta a main merge-et és a GitHub kiadást. A kiadási folyamat a mainen is lefuttatja a 117 kódregressziót, az 56 böngészős tesztszkriptet és az Android/aláírás/forrás ellenőrzéseket. A Google Play és a Google OAuth publikálási feladatai külön maradnak.

Az 1.0.9 dizájnfinomításainak részletei: [kiadási megjegyzések](docs/releases/v1.0.9.md). A korábbi [1.0.8 témaválasztó-javítás](docs/releases/v1.0.8.md) megmaradt.

Részletes megvalósítás: [programszerkesztés és kardió](docs/TRAINING_CARDIO_1_0_7.md), [backup/GPS összevonás](docs/BACKUP_GPS_CONSOLIDATION_1_0_7.md).

## Bluetooth tesztverzió: TrainPilot 1.1.1 (2699)

A **Beállítások → Bluetooth-óra próba** felület a GT4Pro+ / RDFit adatkapcsolatának vizsgálatához készült. Órakeresést, szolgáltatás-felderítést és helyi diagnosztikaexportot ad; szabványos pulzusszolgáltatás esetén az élő pulzus is kipróbálható. A korábbi alvás-, lépés- és pulzusadatok importja még további protokollvizsgálatot igényel. A 2698 fejlesztései benne maradnak. [Telefonos próba és RDFit megállapítások](docs/BLUETOOTH_GT4PRO.md).

## Következő tesztverzió: TrainPilot 1.1.0 (2698)

Az [#103](https://github.com/fuloplevente1998/TrainPilot/issues/103) fejlesztései külön tesztágon készülnek: heti edzés- és havi kilométercél, kihagyott edzések előnézetes újratervezése, beltéri intervallumok, részletes Coach-indoklás és egységes üres/mentési állapotok. A célok a meglévő kezdőlapi „Mai állapot” kártyából és a Beállításokból érhetők el. Nagy Android-betűméretnél a fix áttekintések görgethetővé válnak, hogy minden vezérlő elérhető maradjon.

A stabil kiadás továbbra is **1.0.9 / 2697**. A 2698-as teszt-APK telefonos elfogadása és main merge-je még hátravan. Részletek és kipróbálás: [1.1.0 fejlesztések](docs/FEATURES_1_1_0.md), [tesztkiadási megjegyzések](docs/releases/v1.1.0.md).

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

A CI-ban hat külön Chromium-rész fut, teljes lefedettséggel. [Futtatás és mért idő](docs/UI_TESTS.md).

Részletek: [build és signing](docs/BUILD.md), [fejlesztési állapot](DEVELOPMENT_STATUS.md), [1.0.0 átállási terv](docs/REPOSITORY_MAINTENANCE_2026-09-26.md).

## Signing és titkok

A signing kulcs és a jelszavak nincsenek a repositoryban. A GitHub Actions a következő Repository Secret neveket használja:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Signing kulcsot, jelszót, OAuth credentialt, `.env` fájlt vagy titkosítatlan helyreállítási mentést tilos commitolni. A privát archívum **sem** tartalmazhat signing titkokat.

A Google Play publikáláshoz hátralévő lépéseket külön [ellenőrzőlista](docs/PLAY_STORE_CHECKLIST_2026.md) tartalmazza.

Az alkalmazás az edzés aktív szakaszaihoz Health Connect-lépésszámot is lekér és naplóz. Beltéren idővel, GPS-táv nélkül is rögzíthető a mozgás; a lépésekből nem becsül kilométert. A forrás megosztási késése miatt a napló Health-blokkja később frissíthető.
