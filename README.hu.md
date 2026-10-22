# TrainPilot

Edzéstervezés és naplózás Androidon és Wear OS-en, opcionális egészségadatokkal, regenerációs ajánlással, GPS-útvonallal és Google-szinkronnal.

[English](README.md) · [Kiadásjegyzék](docs/releases/INDEX.md) · [Aktuális munkaátadás](docs/WORK_HANDOFF.md)

## Letöltés és aktuális forrás

| Legfrissebb stabil kiadás | Verzió | Forrás |
| --- | --- | --- |
| [Telefon](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.71-perf-20261022) és [Wear OS](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.71-perf-20261022) | **1.3.71 /2735**, 2026-10-22-i gyorsított build | Készüléken elfogadott #235/#237, main `5d27aa9c5ebf68d1c206c88a74dea89e660a4937`. Külön címke őrzi meg a korábbi 1.3.71-es csomagokat. |
| Korábbi közzétett verzió | 1.3.7 /2734 | [Történeti kiadási jegyzet](docs/releases/v1.3.7.md). |

A frissített 1.3.71 gyorsítja az indulást, korlátozza a Napló kezdeti kártyaszámát, együtt váltja a navigációs gomb színét és az oldalt, és kisebb, optimalizált telefonos APK-t tartalmaz. A lassulás javítását a felhasználó készüléken elfogadta. A korábbi PR-, Mégse-, haptika-, automatikus előtöltés-, RIR- és Samsung/Coach funkciók megmaradnak. [Kiadási jegyzet](docs/releases/v1.3.71-perf-20261022.md).

A telefonos APK a telefonra, a Wear APK az órára való. Frissítéskor őrizd meg az alkalmazás adatait. A forrás, ellenőrzőösszeg és buildadat mindig az adott kiadáshoz tartozik; régi verzió választása előtt nézd meg a [jegyzéket](docs/releases/INDEX.md). A Play-megjelenés előkészítése a [#214](https://github.com/fuloplevente1998/TrainPilot/issues/214) része.

## Az alkalmazás funkciói

| Terület | Elérhető funkciók |
| --- | --- |
| Tervezés | Saját és profil alapján ajánlott programok, szerkeszthető programnapok, gyakorlatkönyvtár, gyors edzés, naptár, célok és kihagyott alkalmak újratervezése. |
| Erősítés | Sorozatok, ismétlések és súlyok; automatikus előtöltés a legutóbbi megfelelő teljesített gyakorlatból; normál/HIT stílus, dupla, lineáris és RIR/Coach progresszió. |
| Edzés közben | Munka/pihenő időzítők, automata fázisok, hang/rezgés, sorozat-visszavonás és sorozatonkénti tényleges 0–10 RIR. |
| Napló és fejlődés | Edzésnapló, megnyitható PR-jelvények, volumen- és kardióstatisztika, becsült 1RM, izomcsoport-terhelés és privát edzésfotók. |
| Egészség és regeneráció | Health Connect vagy Samsung Health import, helyi egészségnapló, forrással jelölt mérések, regeneráció/Coach és elérhető mai Samsung Energy Score. |
| Távolság és intervallum | Telefonos vagy órás GPS, privát útvonal telefonos térképpel, GPS nélküli beltéri időmérés; a sikeres mérésbefejezéshez nincs külön sorozat-OK. |
| Wear OS | Tervezett/gyors és offline edzés, telefon–óra vezérlés és visszaigazolt átadás, támogatott Health Services-mérések, Mini napló, Tile, komplikáció, korona/lünetta és érintészár. |
| Mentés és szinkron | Helyi JSON/ZIP-mentés, opcionális Google Drive appadat-szinkron és Google Naptár-integráció. |
| Testreszabás | HU/EN/DE/RO felület, témák, nagyobb rendszerbetű és állítható edzés-/pihenőjelzések. |

[Részletes funkcióáttekintés](docs/FEATURES.hu.md).

A szolgáltatói adat elérhetősége a készüléktől, engedélyektől és tényleges megosztástól függ. A saját készülékes Samsung-siker nem bizonyítja a nyilvános production partner-hozzáférést. [Play-előkészítés](docs/PLAY_STORE_CHECKLIST_2026.md).

## Képernyőképek

| Kezdőlap | Programnap |
| --- | --- |
| ![Kezdőlap](docs/screenshots/home.png) | ![Programnap](docs/screenshots/program-day.png) |

| Aktív edzés és automata pihenő | Személyes rekord részletei (román felület) |
| --- | --- |
| ![Aktív edzés és automata pihenő](docs/screenshots/active-workout.png) | ![Személyes rekord részletei (román felület)](docs/screenshots/personal-record.png) |

| Fejlődés és izomcsoport-terhelés | Egészségáttekintés |
| --- | --- |
| ![Fejlődés és izomcsoport-terhelés](docs/screenshots/progress.png) | ![Egészségáttekintés](docs/screenshots/health.png) |

| Naptár és tervezés | Coach-ajánlások |
| --- | --- |
| ![Naptár és tervezés](docs/screenshots/calendar.png) | ![Coach-ajánlások](docs/screenshots/coach.png) |

| Wear OS kezdőlap | Wear OS aktív edzés |
| --- | --- |
| ![Wear OS kezdőlap](docs/screenshots/wear-home.png) | ![Wear OS aktív edzés](docs/screenshots/wear-workout.png) |

Az 1.3.71 forrásából készült, változtatás nélkül átvett képek: a telefonos közös felület Chromiumban, a Wear natív kerek emulátoron. Szintetikus tesztadatokat és több témát/nyelvet mutatnak. Nem személyes egészségadatok és nem Play-listingre jóváhagyott készülékfotók. [Képek eredete és teljes felbontás](docs/screenshots/README.md).

## Dokumentáció és fejlesztés

- [Változások: 1.0, 1.1, 1.2, 1.3](RELEASE_NOTES.md)
- [Kiadásjegyzék](docs/releases/INDEX.md), [verziószabály](docs/VERSION_POLICY.md)
- [Build/aláírás](docs/BUILD.md), [Wear OS](docs/WEAR_OS.md)
- [UI-ellenőrzés](docs/UI_TESTS.md), [teljesítmény](docs/PERFORMANCE_REGRESSION_POLICY.md)
- [Play-előkészítés](docs/PLAY_STORE_CHECKLIST_2026.md), [adatvédelmi tervezet](docs/PRIVACY_POLICY_DRAFT.md)
- [Aktuális átadás](docs/WORK_HANDOFF.md), [#214 karbantartás](docs/MAINTENANCE_214.md)

```bash
npm ci
node scripts/check-version-policy.cjs
node --test scripts/check-version-policy.test.cjs scripts/release-metadata.test.cjs
npm test
npm run test:ui
```

Teljes Chromium, natív release és aláírt APK GitHub Actionsben készül, külön telefonos/órás kapuval. Fejlesztés külön ágon és Draft PR-on; alkalmazásváltozáshoz pontos forrású ellenőrzés és felhasználói elfogadás kell. [Munkarend](AGENTS.md).

A Google-integrációkhoz külön OAuth-beállítás kell. Aláírókulcs, credential és személyes mentés nem kerülhet a repóba. A `npm run sync` a generált Android-projektet is módosítja; ezeket commit előtt felül kell vizsgálni.

## Licenc

A TrainPilot zárt/proprietary alkalmazás; a csomag `private` és `UNLICENSED`. A publikus forrás nem ad nyílt forrású licencengedélyt. A kereskedelmi/terjesztési feltételek véglegesítése külön #214 feladat. A külső összetevők saját licencei megmaradnak, köztük a [Lucide licence](www/LUCIDE-LICENSE.txt).
