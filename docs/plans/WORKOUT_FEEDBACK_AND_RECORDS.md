# Négy jóváhagyott workout feature – #226–#229

Jóváhagyás: 2026-10-20, „Ezt a 4 uj kepesseget epitsuk be :) Rogzitsuk fejlesztesnek, vagy featnek.”

Alap main: `f309137c461c6cb3b39497ab46a0e764a7e7b077`. Ág: `feat/workout-records-feedback-rir`. Verzió: **1.3.6 /2733**.

Az [újfunkció-audit #225](https://github.com/fuloplevente1998/TrainPilot/pull/225) alapján a négy B kategóriás képesség implementációja most engedélyezett. A dupla pihenőrezgésminta és a háromválaszos értékelés nem része ennek a csomagnak.

| Sorrend | Issue | Scope | Állapot |
| --- | --- | --- | --- |
| 1 | [#226](https://github.com/fuloplevente1998/TrainPilot/issues/226) | Mentett eredmény és Napló PR-jelvénye a meglévő rekordmodellre építve | Kód és célzott modell/UI teszt elkészült; CI még követendő |
| 2 | [#227](https://github.com/fuloplevente1998/TrainPilot/issues/227) | Kapcsolható rövid haptika sikeres kézi sorozat-OK után | Kód és nyugta/owner/deduplikáció regresszió elkészült; CI követendő |
| 3 | [#228](https://github.com/fuloplevente1998/TrainPilot/issues/228) | Minden erősítő gyakorlat automatikus Napló-alapú súly-/ismétlés-előtöltése, külön gomb nélkül | Pontosított követelmény implementálva; új modell/UI/Wear regressziók, új CI szükséges |
| 4 | [#229](https://github.com/fuloplevente1998/TrainPilot/issues/229) | Sorozatonkénti tényleges RIR, kompatibilis napló/Coach/protokoll | Telefonos bevitel, konzervatív Coach, napló és natív/Wear értékmegőrzés kódja elkészült; integrált kapuk következnek |

### Készülékes hibajelzés és javítás – #228 gyors edzés
A felhasználó az aláírt `fd72074` APK-n igazolta: tervezett edzésben működik a Napló-előtöltés, gyors edzésnél viszont az ismétlések üresek. Gyökérok: a `tp151BuildQuick` és `tp152StartQuickInline` a `tp150QuickExercise` helyes Napló-adatait új `reps:''` sorozatokkal cserélte le. A megjelenő „Célismétlés” külön tervérték; nem azonos az új edzés előtöltött tényleges sorozatismétlésével.

Javítás: közös helper megőrzi a Naplóból átvett sorozatonkénti súlyt, ismétlést, friss azonosítót és a teljesítetlen állapotot. A cél szerkesztése nem töröl ismétlést; szándékos súlymódosítás átírhatja a súlyokat, de nem az ismétlést; a sorozatszám változásakor az új pozíciókhoz is a Napló keresése érvényes. Mindkét gyorsedzés-beállítási út és a tényleges indítás bekerül a regression suite-ba. Main/release csak új készülékes elfogadás után.

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

## #228 végleges pontosítás – minden erősítő gyakorlat automatikusan a Naplóból (2026-10-20)
A korábbi csak A/B-azonos programnapból töltő algoritmust a felhasználó kiterjesztette: minden súlyzós/erősítő gyakorlat következő indításakor a Napló (`history()`) legutóbbi érvényes, teljesített **azonos gyakorlat** sorozat-súlyait és ismétlésszámait kell előtölteni. A gyakorlat azonosítója a kulcs, **nem a programnap**. A másik programban, B napon vagy gyors edzésként mentett korábbi érték is forrás. Nincs másolásgomb, jóváhagyáskérés vagy külön kapcsoló.

- Minden sorozatpozícióhoz időrendben a legutóbbi teljesített, számszerűen érvényes bejegyzést választjuk ki a befejezett, nem törölt Napló-edzésekből. Eltérő terhelési mód/ismétlési egység, idő-/táv-/GPS-gyakorlat, duplikált gyakorlat-előfordulás és hibás adat nem használható. A legfrissebb részleges sorozat esetén korábbi érvényes mentés használható.
- A jelenlegi terv sorozatszáma és a nem talált pozíciók alapértéke marad. Minden új sorozat új ID-val, done:false állapotban indul; actualRir, idő-/GPS-/szinkron-/teljesítési metaadat nem öröklődik.
- A Naplót nem írjuk át. A felhasználó által kézzel szerkesztett vagy újranyitott draft értékeit nem módosítjuk; a Coach terhelésnövelése nem írhatja felül csendben az előtöltést.
- Azonos telefonos logika szolgálja a tervezett programnapot, a gyors edzés indítását és gyakorlat-hozzáadását, valamint az órára küldött programnapi és **gyors edzéses** sablonokat. A Wear OS ezeket új, teljesítetlen sorozatokként indítja; régi sablon továbbra is kompatibilis.
- Új forrásazonos modell-, böngészős és Wear-indítási regressziók, majd telefon/Wear APK-pár és készülékes próba szükséges. Nincs main merge vagy release a felhasználói jóváhagyásig.

### Automatizálási UI-elv / Health
A már engedélyezett, ismétlődő adatfrissítések automatikusan történjenek, állapotjelzéssel. Kézi művelet az első kapcsolódáshoz/engedélyhez, felhasználói döntéshez és hibajavításhoz szükséges. A jelenlegi Health-kód már tartalmaz appnyitási/előtéri frissítést, deduplikált olvasást és külön háttérengedélyt; ezt a #228 módosítás nem helyettesíti új szinkronfolyamattal. Teljes Health-UI egyszerűsítést vagy új háttérszinkront ez a commit nem állít elkészültnek.

A korábban átadott `bd5c0dc` APK a régi másolásgombot tartalmazza; az új követelményhez új Actions-build és készülékes teszt kell. A felhasználó main-jóváhagyása továbbra is a készülékes validálástól függ.
