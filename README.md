# TrainPilot

Androidos edzéstervező és edzésnapló, natív Wear OS társalkalmazással. A fő edzésfunkciók helyben használhatók; az egészségadatok, a Google Drive és a Google Naptár integrációja opcionális.

**Aktuális kiadáscsalád: 1.3 — telefon és Wear OS.** A mainen lévő párosított kiadás a Mini napló frissítését, az órás mérési előzményeket és az egészségadatok pontos forrásjelölését tartalmazza. A telefonról kezdeményezett órás appindítás javítása szintén az **1.3** leírásához tartozik; ez még a [#160 tesztágon](https://github.com/fuloplevente1998/TrainPilot/pull/160) van. A felhasználó az órás app megnyílását már visszaigazolta, a háttérmérés és a teljes adatút ellenőrzése folyamatban van.

[Telefonos 1.3 kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.0) · [Wear OS 1.3 kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.0) · [Összevont változáslista](RELEASE_NOTES.md) · [Fejlesztési állapot](DEVELOPMENT_STATUS.md)

## Verziók áttekintése

A repó leírásai a köztes javításokat négy verziócsaládba vonják össze. A release-ek, tagek, APK-k és tényleges buildazonosítók megmaradnak.

| Verzió | Összevont tartalom |
| --- | --- |
| [1.0](RELEASE_NOTES.md#10) | Edzéstervezés, napló, Coach, fejlődés, fotók, mentés, kardió és közös szerkesztők. |
| [1.1](RELEASE_NOTES.md#11) | Célok, előnézetes újratervezés, beltéri intervallumok és részletes Coach-indoklás. |
| [1.2](RELEASE_NOTES.md#12) | Tartós egészségnapló, profilvezérelt programok, naptár- és felületi javítások, Wear OS alapfunkciók. |
| [1.3](RELEASE_NOTES.md#13) | Párosított gyors edzés, Mini napló, órás mérési előzmények, forráshű egészségadatok és a telefonos órás appindítás javítása. |

## Fő funkciók

- Profilhoz igazítható és saját edzésprogramok, gyors edzés, menthető gyakorlatsorrend.
- Sorozatok, ismétlések, terhelés, idő és távolság naplózása; pihenőidőzítő és kétoldalas stopper.
- Fejlődés, személyes rekordok, kardióstatisztika, izomábra és naplóalapú TrainPilot Coach.
- Gyakorlatkönyvtár, saját gyakorlatok és privát app-tárhelyen kezelt edzésfotók.
- Opcionális GPS-távolságmérés és GPS nélküli beltéri időmérő/intervallumok.
- Health Connect vagy Samsung Health közvetlen adatforrás, tartós egészségnapló és külön jelölt órás edzésmérés.
- Wear OS edzésvezérlés, gyors edzés, mini naptár, Mini napló, mérési előzmények, Tile és számlapi kiegészítő.
- Helyi JSON/ZIP-mentés, Google Drive `appDataFolder` szinkron és opcionális fotószinkron.
- Google Naptár, többnyelvű felület és matt/élénk témák.

A telefon Naplója őrzi a mentett edzéseket. Az órás Mini napló ezek korlátozott, offline is olvasható nézete. A megfelelő edzéshez tartozó TrainPilot Wear-mérés külön forrás; a Health Connect és a Samsung Health időszakos adatai nem adódnak hozzá.

TrainPilot Wear-mérés nélkül a telefon a kiválasztott egészségadat-szolgáltatóból olvas, ha ott rendelkezésre áll adat. Azonos óra mellett is eltérhet a mérési időszak, a szinkron ideje és az aktív/összes kalória jelentése. [Részletes működés](docs/WEAR_OS.md#egészségadatok-és-kijelző-kikapcsolása).

**Folyamatban lévő ellenőrzések:** kijelző kikapcsolása melletti mérés, naplózott és későn érkező egészségadatok, eltérő kalóriaértékek összehasonlítása. **Nyitott hiba:** a Google Drive-szinkron időnként ismét túl sokáig tart; kivizsgálás szükséges.

## Telepítés és dokumentáció

A [GitHub Releases](https://github.com/fuloplevente1998/TrainPilot/releases) oldalon a telefonos és órás APK-k külön találhatók. A hozzájuk tartozó forrás, ellenőrzőösszeg és csomagadat a megfelelő release mellékleteiben marad. A telefonos órás appindítás tesztjéhez a #160-hoz átadott párosított teszt-APK-k szükségesek; a jelenlegi main release-ek még nem tartalmazzák ezt a javítást.

- [Build és aláírás](docs/BUILD.md)
- [Wear OS működés és készülékes ellenőrzés](docs/WEAR_OS.md)
- [Profilvezérelt edzésrendszer](docs/profile-training-128.md)
- [Chromium UI-ellenőrzések](docs/UI_TESTS.md)
- [Teljesítménykövetelmények](docs/PERFORMANCE_REGRESSION_POLICY.md)
- [Google Play publikálási teendők](docs/PLAY_STORE_CHECKLIST_2026.md)

A Google-integrációkhoz külön Cloud/OAuth-beállítások szükségesek. Az egészségadatok elérhetősége a szolgáltatótól, jogosultságoktól és a tényleges adatmegosztástól függ.

## Fejlesztés és kiadás

A `main` az elfogadott alkalmazásállapotot tartalmazza; a célzott fejlesztések rövid életű `feat/*`, `fix/*` vagy `maintenance/*` ágakon készülnek. Alkalmazásváltozásnál a CI eredményeit és a fizikai készülékes próbát külön rögzítjük; mainre emeléshez felhasználói jóváhagyás kell.

```bash
npm ci
npm test
npm run test:ui
npm run sync
```

A telefon teljes Release Gate-je Node-, hat részben futó Chromium- és natív Android-ellenőrzést, aláírt APK-t, forrásegyezést és ellenőrzőösszegeket használ. A Wear modulnak külön natív kiadási kapuja van; közös protokollváltozásnál mindkettő szükséges.

A signing kulcs és a jelszavak nem kerülnek a repóba. A CI secretjei: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. OAuth-credentialt, signing titkot és titkosítatlan személyes mentést sem szabad commitolni.

## Korábbi előzmények

A publikus main commitelőzménye megmarad; ez a leírásrendezés nem írja át a Git-történetet. A korábbi privát/legacy anyagokat a privát TrainPilot-Archive őrzi. [Repository-karbantartási háttér](docs/REPOSITORY_MAINTENANCE_2026-09-26.md).

Az egyedi buildek [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) kiadási feljegyzései történeti és buildtechnikai források. Az aktuális áttekintést a négy családra összevont [változáslista](RELEASE_NOTES.md) adja.
