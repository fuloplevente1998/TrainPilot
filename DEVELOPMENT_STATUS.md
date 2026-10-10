# TrainPilot — fejlesztési állapot

Frissítve: **2026. október 10.** A felhasználói verzióleírások négy családot használnak: **1.0, 1.1, 1.2, 1.3**. A buildazonosítók, APK-k, tagek és release-ek megmaradnak.

## Aktuális main és tesztág

| Terület | Állapot |
| --- | --- |
| Telefon + Wear OS | Az **1.3** párosított kiadás a mainen, a felhasználó által jóváhagyott [#156](https://github.com/fuloplevente1998/TrainPilot/pull/156) után. |
| Mini napló és mérési előzmények | Mainen: frissítés, legfeljebb nyolc mentett edzés, három legutóbbi mért edzés, részletek és visszanavigálás. |
| Egészségadatok | A 20 guggolásos próba helyes átadást igazolt. A források, késői adatok és a teljes naplóadat megőrzésének készülékes tesztje folytatódik. |
| Telefonról indított órás app | Az **1.3** javítási sorozat része a [#160 tesztágon](https://github.com/fuloplevente1998/TrainPilot/pull/160), még nincs mainen. Programos edzés megnyílása és háttérmérése felhasználó által visszaigazolva. A gyors edzés aktuális választójának indítófüggvényében a hiányzó órás megnyitás bekötve; készülékes próba következik. |
| Kiadási ellenőrzések | A #160 párosított telefonos/órás CI- és aláírt APK-ellenőrzése sikeres. Ez nem helyettesíti a készülékes elfogadást. |
| Drive-szinkron | Az **1.3** #160 tesztágán csak a legfrissebb eszközmentések olvasása, külön korábbi-mentés helyreállítás és javított Telefon/Felhő választás. A felhasználó a működő szinkront visszaigazolta; #161 lezárva. |

Az alkalmazás megnyílása vagy a mérési értesítés önmagában nem igazolja, hogy friss szenzoradat keletkezett. A kijelző kikapcsolása után is ellenőrizni kell a mérés folytatását, majd ugyanannak az edzésnek a mentett telefonos és órás részleteit.

## Összevont verziók

| Verzió | Fejlesztési tartalom |
| --- | --- |
| [1.0](RELEASE_NOTES.md#10) | Alap edzéstervező/napló, Coach, fejlődés, fotók, mentés, kardió és közös szerkesztők. |
| [1.1](RELEASE_NOTES.md#11) | Célok, előnézetes újratervezés, beltéri intervallumok, Coach-indoklás; külön kezelt korábbi Bluetooth-kísérletek. |
| [1.2](RELEASE_NOTES.md#12) | Egészségnapló, profilvezérelt programok, naptár/felület/indítás javításai és Wear OS alapok. |
| [1.3](RELEASE_NOTES.md#13) | Párosított gyors edzés, Mini napló, mért edzések előzménye, egészségforrások és a telefonos órás appindítás tesztelt javítása. |

A részletes felhasználói változáslista a [RELEASE_NOTES.md](RELEASE_NOTES.md). A régi, kiadásonkénti [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) jegyzetek történeti/buildtechnikai archívumok; a bennük rögzített akkori jelöltstátusz nem írja felül ezt az aktuális állapotot.

## Nyitott ellenőrzések

- Telefonon indított programos és gyors edzés: érintetlen óra, kezdetben sötét kijelző, új mérés, valódi pulzus/kalória, további mérés kikapcsolt kijelzővel.
- Befejezés a telefonon és az órán, pontosan egy mentett edzés, azonos órás mérési összegzés a telefon Naplójában és a Mini naplóban.
- Kikapcsolt mérések, hiányzó engedély, megszakadt kapcsolat, újracsatlakozás és offline parancsok.
- Órás előzmény → részlet → vissza, frissítés, nagybetűs körkijelző és offline napló.
- Korábbi 4 órás kcal és 1 aktív / 2 összes telefonos kcal: egyező edzés, szolgáltató, időablak és adatfrissesség vizsgálata. A forrásokat nem szabad összeadni.
- Telefonos edzés közvetlen Wear-mérés nélkül: csak a kiválasztott szolgáltató ténylegesen elérhető adata jelenhet meg.
- Már naplózott egészségadatok megőrzése és késői értékek hozzákapcsolása a megfelelő edzéshez.
- Lassú Drive-szinkron: az elakadás szakasza, hálózati kérések, fotók és változatlan adatok ismételt feldolgozásának kivizsgálása.

## Megőrzendő adatkezelési szabályok

- A mentett napló, saját programok, fotók és meglévő egészségadatok nem veszhetnek el frissítéskor.
- Azonos napi, eltérő edzések megmaradnak. Valódi másolat összevonásakor az időablakot és a teljes edzést kell összevetni; dátum alapján nem szabad törölni.
- Stabil azonosító, órás visszaigazolás és törlésjelző akadályozza meg az ismételt mentést és a törölt bejegyzés visszaéledését.
- A Drive-egyeztetés megőrzi a helyi/távoli módosításokat és törléseket; a sikertelen helyreállítás visszaállítja az előző állapotot.
- Késői Health- vagy Wear-adat ugyanahhoz az edzésazonosítóhoz kapcsolódik, új bejegyzést nem hoz létre.
- A TrainPilot Wear, Health Connect és Samsung Health értékei forrásonként külön maradnak. Mért nulla megmarad, hiányzó adat nem becsülhető.
- A teljes natív egészségnapló kézi mentése külön hozzájárulásos; nem kerül automatikusan Drive-pillanatképbe. A fotók privát app-tárhelyen maradnak.
- GPS csak összesített időt/távolságot ad át a naplónak; útvonal-koordinátát nem tesz a mentésbe.

## Megőrzendő felületi működés

A Napló kompakt, összecsukott edzésösszefoglalót és lenyitáskor betöltődő gyakorlatrészleteket használ. A szerkesztő sorozathozzáadást/-törlést és egyértelmű Mentés/Mégse műveleteket ad. A fotófolyamat kamera/galéria/mégse lehetősége és privát tárolása megmarad.

A pulzusnézet valódi hét naptári napot kezel; a hiányzó és nulla érték jelentése külön marad. A Coach súlyos, testsúlyos, időalapú és kétoldalas gyakorlatokhoz is a teljes releváns naplót használja; mesterséges sorlimit nem vághatja le a trendet. A mentési visszajelzés közös, a lenyitásnyilak egységesek, a tényleges indításikonok külön funkciót jelölnek. Az alap témák mattak, az élénk témák glow/neon színei statikusak.

## Alkalmazásváltozások kiadási folyamata

1. Az aktuális, mainen elfogadott állapotból induló célzott ág és világos hibaleírás/elfogadási feltétel.
2. Az érintett működés regressziója; telefonos változásnál a teljes Node-, Chromium- és natív kiadási ellenőrzés, változatlan teljesítménykövetelményekkel.
3. Aláírt APK, forráscommit, csomagadat, verzió és aláíró tanúsítvány egyezésének ellenőrzése. Közös protokollnál a telefon és a Wear együtt ellenőrzendő.
4. A megfelelő párosított teszt-APK-k átadása, a CI-eredmények és a fizikai készülékes tapasztalat külön rögzítése.
5. Felhasználói jóváhagyás után mainre emelés, a main kiadási ellenőrzése és a hozzá tartozó release. Kifejezett publikálási jóváhagyás nem jelent automatikusan minden készülékes teszt teljesítését.

A leírások karbantartása nem módosítja az APK verzióját és nem emel automatikusan alkalmazáskódot a mainre.

## Fejlesztői hivatkozások

- [Build és signing](docs/BUILD.md)
- [UI-tesztek](docs/UI_TESTS.md)
- [Teljesítményregressziós szabályok](docs/PERFORMANCE_REGRESSION_POLICY.md)
- [Wear OS architektúra és ellenőrzés](docs/WEAR_OS.md)
- [Profilvezérelt edzésrendszer](docs/profile-training-128.md)
- [Korábbi fázisterv, történeti referenciaként](docs/PHASE_5_6_7_ROADMAP.md)
- [A korábbi részletes állapotfeljegyzés](https://github.com/fuloplevente1998/TrainPilot/blob/2fdcf4fde7021fb234bd85fff0a59b698d36c808/DEVELOPMENT_STATUS.md)
