# Négy jóváhagyott workout feature – #226–#229

Jóváhagyás: 2026-10-20, „Ezt a 4 uj kepesseget epitsuk be :) Rogzitsuk fejlesztesnek, vagy featnek.”

Alap main: `f309137c461c6cb3b39497ab46a0e764a7e7b077`. Ág: `feat/workout-records-feedback-rir`. Verzió: **1.3.6 /2733**.

Az [újfunkció-audit #225](https://github.com/fuloplevente1998/TrainPilot/pull/225) alapján a négy B kategóriás képesség implementációja most engedélyezett. A dupla pihenőrezgésminta és a háromválaszos értékelés nem része ennek a csomagnak.

| Sorrend | Issue | Scope | Állapot |
| --- | --- | --- | --- |
| 1 | [#226](https://github.com/fuloplevente1998/TrainPilot/issues/226) | Mentett eredmény és Napló PR-jelvénye a meglévő rekordmodellre építve | Kód és célzott modell/UI teszt elkészült; CI még követendő |
| 2 | [#227](https://github.com/fuloplevente1998/TrainPilot/issues/227) | Kapcsolható rövid haptika sikeres kézi sorozat-OK után | Következő implementáció |
| 3 | [#228](https://github.com/fuloplevente1998/TrainPilot/issues/228) | Kifejezett sorozatmásolás új azonosítóval, done:false | Tervezett |
| 4 | [#229](https://github.com/fuloplevente1998/TrainPilot/issues/229) | Sorozatonkénti tényleges RIR, kompatibilis napló/Coach/protokoll | Tervezett |

## Megőrzendő működés

- Egy kanonikus edzés és időzítőtulajdonos. Órás/távoli vezérlés és bizonytalan átadás alatt nincs telefonos szerkesztés vagy duplikált jelzés.
- Új setId, kézi teljesítés. Másolás nem visz át RIR-mérést, done állapotot vagy időmérő/GPS/szinkron metaadatot.
- Nulla RIR érvényes, hiány nem nulla. Régi exercise.actualRir nem sokszorozható kitalált sorozatértékekké.
- PR első alapértékből nem keletkezik. A jelvény statikus, mentett eredményhez kötött, szerkesztés/törlés után újraszámolható; nem ír adatot és nem ünnepel újra.
- A RIR első változata telefonos bevitel; a natív/Wear adatutak megőrzését ellenőrizni kell. Önálló órás RIR-beviteli UI külön későbbi scope.
- Nincs automatikus verzióemelés, main merge vagy release. Készülékes elfogadás és a releváns forrásazonos kapuk után zárhatók az issue-k.

## Ellenőrzés

Kis feature-commitok, célzott modell/UI/native regressziók. Teljes regresszió, Chromium, natív build és aláírt APK kizárólag Actionsben. A négy képesség integrált forrásán kell végső Quick/Performance/6 Chromium/phone és az érintett Wear kapu, majd készülékes próba.

A jelenlegi környezetben nincs használható shell; a forrásmódosítás GitHub Git-adat API-n, pontos szülőcommitokkal, diff-/tree-ellenőrzéssel és CAS ref-frissítéssel történik. Helyben csak tiszta JS szintaxis és célzott modell ellenőrzés fut a rendelkezésre álló V8 futtatóban; ez nem helyettesít alkalmazás- vagy készüléktesztet.
