# TrainPilot – külön kiosztható natív UI-lokalizáció

Jóváhagyva: 2026-10-25. A felhasználó külön ChatGPT-beszélgetésben végezteti.
Ez végrehajtható részfeladat, nem a teljes migráció átadása. A másik beszélgetés
nem kapja meg automatikusan az itteni előzményeket, forrást vagy GitHub-hozzáférést.

## Indulás és ág

- Repository: https://github.com/fuloplevente1998/TrainPilot
- Saját ág: **feat/native-ui-localization**.
- Funkcionális kiindulás: **958907c685df54362bc6c683ee1d3ffbd222801f**.
  A később hozzáadott kiosztási dokumentáció nem új alkalmazásforrás.
- Először `AGENTS.md`, majd `docs/NATIVE_RESUME.md` első aktuális szakasza.
  A hosszú teljes beszélgetés/Gemini-export/main-forrás ehhez nem szükséges.
- Külön munkafa/checkout szükséges; ne váltsd át egy másik agent megosztott
  munkafájának ágát. Kezdéskor ellenőrizd az aktuális ágat, HEAD-et és a dirty fájlokat.
- A jelen dokumentum a felhasználó jóváhagyott kivétele a régi általános
  „kizárólag feat/native-android-migration” előíráshoz: **neked csak a lokalizációs
  ágra szabad írnod**. Main/migrációs ág, merge, force push és verzióemelés tiltott.

## Feladat

A telefonos natív Compose felület még beégetett, felhasználónak megjelenő
feliratait kösd be a meglévő **HU / EN / DE / RO** erőforrásrendszerbe.
Mind a négy nyelven teljes, természetes fordítás kell; ne jelölj késznek egy
képernyőt, ha annak fő feliratai továbbra is magyarul maradnak más nyelven.

A meglévő `ui/text/NativeLocalizedResources.kt` `nativeResources(context,
language)` segédjét használd. Az alkalmazás DataStore-ban választott nyelve a
mérvadó, nem feltétlenül az Android rendszer nyelve. Ne cseréld le az Activity
kontextusát; Health/engedélykérés/SAF működésének meg kell maradnia.

Csak megjelenített szövegek változzanak. A route-ok, testTagek, JSON- és
beállításkulcsok, enum/állapotazonosítók, program/gyakorlat-ID-k változatlanok.
A tulajdonneveket és szabványos jelöléseket (TrainPilot, Health Connect,
Samsung Health, GPS, RIR, kg, bpm stb.) ne fordítsd félre.

## Kizárólag módosítható fájlok

1. `android/app/src/main/java/com/repforge/app/ui/` alatti `*Screen.kt` és
   `*Dialog.kt` fájlok: csak feliratok/akadálymentes szövegek bekötése.
2. `android/app/src/main/java/com/repforge/app/ui/text/` segédek, ha indokolt;
   lehetőleg a meglévő megoldás használatával.
3. `android/app/src/main/res/values/strings.xml`, `values-en/strings.xml`,
   `values-de/strings.xml`, `values-ro/strings.xml`.
4. Új, külön `NativeLocalization*Test.kt` unit/Compose tesztfájlok a meglévő
   tesztforrások alatt. A közösen bővített `NativePhoneNavigationTest.kt`-t ne írd át.
5. Saját összefoglaló: `docs/reviews/NATIVE_UI_LOCALIZATION_REPORT.md`.

Más fájl módosítása helyett pontosan jegyezd fel a szükséges folytatást.
Különösen **ne módosíts ViewModelt, repositoryt, adatbázist, motort, szinkront,
backupot, natív szolgáltatást, navigációt, Wear/shared-kódot, Gradle-t vagy workflow-t**.
A képernyőfájlokban se változtass üzleti logikát, callbacket, műveletsorrendet,
layoutot, színtémát, érintési célt vagy piros X bezárást.

A backendből érkező nyers magyar hibaszöveget ne rejtsd el általános szöveggel,
és ne építs törékeny, hibaüzenet-szövegre illeszkedő fordítómotort. Ha ehhez
állapotkód/VM-módosítás kell, jelöld a riportban hatókörön kívüli maradékként.

## Ellenőrzés

- Egyező kulcskészlet az új szövegeknél mind a négy nyelven; nincs duplikált kulcs.
- Azonos számú/típusú, pozicionált formátumargumentum (`%1$s`, `%2$d` stb.).
  Őrizd meg a mértékegységet, változó értéket, sortörést, UTF-8 karaktereket.
- Android resource-szabályos XML és escape (apostrofok/idézőjelek/ampersand).
  Az XML-parser sikeressége önmagában nem Android-erőforrásfordítás.
- Minden új `R.string` hivatkozás létezik; nincs változó helyett beégetett
  pulzus/kalória/idő/súly vagy kitalált adat.
- Ellenőrizd a diffet: valóban csak a kijelölt feliratok változtak-e, az ID,
  testTag, route, állapotkezelés és callback érintetlen maradt-e.
- Célzott gyors ellenőrzés helyben megengedett; teljes Gradle/emulátor/APK a
  közös GitHub CI-kapu része, az integráció után. Ne indíts minden részcommitra
  teljes buildet és ne hozz létre új workflow-t. Köztes commit: `[skip ci]`.
- A külön új UI-tesztet a meglévő CI esetleges osztályszűrője nem feltétlenül
  futtatja: az integrációs bekötést jelezd a riportban, workflow-módosítás nélkül.

## Átadás

Kis, követhető commitokkal csak a saját ágra pusholj. GitHub-hozzáférés esetén
**draft PR célága feat/native-android-migration**, ne main. Ne merge-elj és ne
zárj issue-t. Hozzáférés nélkül patch/diff + riport is megfelelő; ne állíts
GitHub-push/CI-sikert bizonyíték nélkül.

A riport tartalmazza:

- pontos bázis/HEAD, módosított fájlok és képernyők;
- új kulcsok és nyelvi lefedettség, szándékosan nem fordított tulajdonnevek;
- fennmaradó feliratok és hatókörön kívüli szükséges módosítások pontos helye;
- ténylegesen futtatott ellenőrzések eredménye, megírt de nem futott tesztek külön;
- rövid javaslat az integrációs nyelv-/kis kijelzős/nagy betűs próbákra.

A fő migrációs agent közben cloud-fotó/backup/szinkron/törlési replay munkát végez.
Az itteni munkát kódfelülvizsgálat után integráljuk; a teljes app készültségét,
APK-telepíthetőségét vagy élő adatmegőrzését a lokalizáció önmagában nem igazolja.
