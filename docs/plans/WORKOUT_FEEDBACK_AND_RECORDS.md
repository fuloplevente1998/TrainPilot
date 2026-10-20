# Négy jóváhagyott workout feature – #226–#229

Jóváhagyás: 2026-10-20, „Ezt a 4 uj kepesseget epitsuk be :) Rogzitsuk fejlesztesnek, vagy featnek.”

Alap main: `f309137c461c6cb3b39497ab46a0e764a7e7b077`. Ág: `feat/workout-records-feedback-rir`. Verzió: **1.3.6 /2733**.

Az [újfunkció-audit #225](https://github.com/fuloplevente1998/TrainPilot/pull/225) alapján a négy B kategóriás képesség implementációja most engedélyezett. A dupla pihenőrezgésminta és a háromválaszos értékelés nem része ennek a csomagnak.

| Sorrend | Issue | Scope | Állapot |
| --- | --- | --- | --- |
| 1 | [#226](https://github.com/fuloplevente1998/TrainPilot/issues/226) | Mentett eredmény és Napló PR-jelvénye a meglévő rekordmodellre építve | Kód és célzott modell/UI teszt elkészült; CI még követendő |
| 2 | [#227](https://github.com/fuloplevente1998/TrainPilot/issues/227) | Kapcsolható rövid haptika sikeres kézi sorozat-OK után | Kód és nyugta/owner/deduplikáció regresszió elkészült; CI követendő |
| 3 | [#228](https://github.com/fuloplevente1998/TrainPilot/issues/228) | Azonos programnap automatikus súly-/ismétlés-előtöltése, külön gomb nélkül | Pontosított követelmény implementálva; új modell/UI/Wear regressziók, új CI szükséges |
| 4 | [#229](https://github.com/fuloplevente1998/TrainPilot/issues/229) | Sorozatonkénti tényleges RIR, kompatibilis napló/Coach/protokoll | Telefonos bevitel, konzervatív Coach, napló és natív/Wear értékmegőrzés kódja elkészült; integrált kapuk következnek |

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

## RIR adatmodell (#229)

A `sets[].actualRir` opcionális egész 0–10. A Coach a kész sorozatok megadott értékeinek minimumát használja; az üres érték nem nulla. Az `exercise.setRir229` jelzi a sorozatos bevitel tudatos használatát, ezért minden új megfigyelés törlése nem kapcsolja vissza csendben a régi gyakorlat-RIR-t. Az eredeti `exercise.actualRir` változatlanul olvasható és külön régi értékként látható. A cél-RIR nem kerül a megfigyelésekbe.

A telefonos és Wear snapshot külön nullable sorozat-RIR-t, régi gyakorlat-RIR-t és a jelzőt visz át. A meglévő nyolcmezős Undo guard változatlan protokoll; a visszavont teljesítés RIR-je törlődik JS-ben, natív telefonon és órán. Másolás soha nem viszi át. A natív előkészítés csak érvényes egész 0–10 vagy hiányzó RIR-t fogad.

A haptika alapból bekapcsolt, az Edzés beállításai panelen mentett kapcsolóval. Sikeres, tartós kézi nyugta egyszer jelez 40 ms-ot; automatikus módban a meglévő időzítőjelzések maradnak. A #228 kézi másolásgombját a felhasználó pontosítása alapján az alábbi automatikus előtöltés váltja ki.

## #228 pontosítás – automatikus folytatás ugyanazon programnapból (2026-10-20)

A felhasználó a külön másolásgomb helyett azt kéri, hogy például a négy nappal későbbi A nap a legutóbbi A nap tényleges súly-/ismétlésértékeivel induljon. Nincs külön gomb, felugró kérdés vagy bekapcsolandó opció.

- Egyező programazonosító + napazonosító; a legutóbbi mentett, befejezett alkalom időrendben, nem a tömb sorrendjéből. Másik nap/program, draft, törölt és jövőbeli bejegyzés kizárt.
- Csak az azonos gyakorlat azonos sorszámú, teljesített, érvényes erősítő sorozatának súlya és ismétlése kerül át. Eltérő terhelési/ismétlési egység, hiányzó vagy nem teljesített sor esetén az aktuális terv alapértékei maradnak. A jelenlegi terv sorozatszáma az irányadó.
- Minden új sorozat done:false, új azonosító; nincs örökölt tényleges RIR, idő, GPS vagy teljesítési metaadat. Előtöltés csak új edzés létrehozásakor; a draft újranyitását és kézi módosításait nem írja felül.
- Telefonos indítás és órára küldött programnapsablon közös kiválasztási logikát használ. Az óra a sablon ismétléseit is beolvassa, de saját új azonosítót generál; régi sablon ismétlés nélkül továbbra is működik.
- A korábbi adat nem automatikus progresszió. Előtöltött értéket a legacy emelés nem ír felül. Az explicit Coach-jóváhagyás érintetlen előtöltés mellett is elérhető, kézi szerkesztés és teljesítés után továbbra is védett.
- A sorozatmásolás gombja és CSS-e eltávolítva. Célzott modell-, integrált böngészős és Wear JSON/indítási tesztek frissítve.

### Automatizálási UI-elv / Health
A már engedélyezett, ismétlődő adatfrissítések automatikusan történjenek, állapotjelzéssel. Kézi művelet az első kapcsolódáshoz/engedélyhez, felhasználói döntéshez és hibajavításhoz szükséges. A jelenlegi Health-kód már tartalmaz appnyitási/előtéri frissítést, deduplikált olvasást és külön háttérengedélyt; ezt a #228 módosítás nem helyettesíti új szinkronfolyamattal. Teljes Health-UI egyszerűsítést vagy új háttérszinkront ez a commit nem állít elkészültnek.

A korábban átadott `bd5c0dc` APK a régi másolásgombot tartalmazza; az új követelményhez új Actions-build és készülékes teszt kell. A felhasználó main-jóváhagyása továbbra is a készülékes validálástól függ.
