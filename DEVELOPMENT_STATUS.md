# TrainPilot — fejlesztési állapot


## #162 — natív telefonos háttérrögzítés, 2026. október 13.

Külön feladatág: `fix/162-phone-background-recording`, a friss `e02dcf5` mainből. Az új tesztkód a telefonon bekapcsolt automata módot a látható Activityből előkészített foreground szolgáltatáshoz köti. Órás, stabil sorozatazonosítós rögzítés natívan menti a tényleges ismétlést/súlyt, majd a következő pihenőt vagy beltéri időalapú menetrendet indítja. Az óráról indított új beltéri időmenetrendnek nem kell telefonos HTML-űrlap. A telefon felébresztéskor a meglévő natív menetrendet veszi át.

A rögzítés és a visszajátszást védő bizonylat egyetlen tartós tranzakció. A foreground parancsfoglalás megakadályozza, hogy ugyanazt a kérést a háttérfigyelő és a webes felület is feldolgozza. Későbbi javítás, idegen edzés/piszkozat, már teljesített sorozat, megszakított vagy eltérő fázis nem írható felül. A Wear az ugyanazon menetrendből késve érkező régebbi fázist elutasítja. A szolgáltatás pihenő után is készenlétben marad, mérési wake lock nélkül; kikapcsolás, edzéslezárás, értesítési Stop, feladateltávolítás vagy legfeljebb 24 óra után leáll.

Hatókör: a telefonos automata módnak az edzésből már előkészítettnek kell lennie. Folyamatleállítás/újraindítás után nincs automatikus szolgáltatásindítás vagy kimaradt sorozatok kitalálása. Harminc másodpercnél régebbi órás kérés a foreground feldolgozásra vár. GPS-indítás és oldalankénti időmenetrend továbbra is külön bővítés; az új natív menetrend ezeket nem indítja a háttérből. A munka/pihenő/kézi szünet naplóbontása és a #163 térkép hátravan.

Ellenőrzés: 138 meglévő Node-regresszió és az automata/Wear böngészős próbák helyben sikeresek; új `phone-background-162.cjs` ellenőrzi az állapotátvételt, a kettős időzítő kizárását, az ismételt kérést és a másik piszkozat védelmét. A natív `PhoneAutomaticBackgroundTest` a CI gate része. Helyi Android SDK/JDK nincs, ezért a natív és aláírt APK-eredményt a GitHub Actionsből kell igazolni. Fizikai telefon–óra, rezgés, lezárt kijelző és OEM háttérkezelés még nincs elfogadva. Verzió változatlan: 1.3.4 /2730.

Frissítve: **2026. október 13.** A felhasználói verzióleírások négy családot használnak: **1.0, 1.1, 1.2, 1.3**. A buildazonosítók, APK-k, tagek és release-ek megmaradnak.


## Mentési javítás — #180

A #179 mainre került (`c623af7`), a #164 és #178 is egyesítve. A [#180](https://github.com/fuloplevente1998/TrainPilot/issues/180) új felhasználói visszajelzése: aktív edzés nélkül a JSON-export „Érvénytelen vagy túl nagy mentés”, a ZIP „hiányzó fotó” hibát ad; a fotók a telefonon megnyithatók.

A javítás kompakt, veszteségmentes JSON-t exportál, és a 20 MiB korlátot UTF-8 bájtban ellenőrzi. A ZIP az élő fotóhivatkozásokat előzetesen ellenőrzi, a hiányzó/üres fájlt a meglévő Google-fiókból próbálja visszatölteni, majd újra ellenőrzi. A hiányzó, üres és 6 MiB-nál nagyobb eredeti képfájlt külön, négy nyelven és az edzés azonosításával jelzi; a nagy kép előnézete megnyitható lehet. A képek és egészségadatok nem maradhatnak ki csendben. A 20 MiB JSON / 6 MiB kép / 500 MiB archívum korlát és a korábbi visszaállítási formátum megmarad.

**Készülékes mentés elfogadva 2026. október 13-án:** a felhasználó visszajelzése: „rendben van a mentés is”. A #180 mentési hibajegy lezárható; a következő feladat az automata működés készülékes elfogadása és továbbfejlesztése, majd a #163 térkép. Külön kézi visszaállítási próbáról nem érkezett visszajelzés.

## Aktuális main és következő fejlesztés

A felhasználó 2026. október 13-án kérte az aktuális main, GitHub-leírások és következő fejlesztések aktualizálását. A [#179](https://github.com/fuloplevente1998/TrainPilot/pull/179) egyesíti a #164 telefonos automata módját és a #178 Wear-kezdőlapját, valamint az új órás automata integrációt. Ez a további fejlesztés alapja; a készülékes automata elfogadás külön feladat.

| Terület | Állapot |
| --- | --- |
| Verzió | Main és legutóbbi tesztpár: **1.3.4 / 2730**; a verzió önmagában nem azonosítja a buildet. |
| Korábbi átadott telefonos referencia | `16509065474cf76ee1222985e9155b6608f479f9`: automata mód, 3–2–1 jelzés + 1 s rezgés, alsó számláló, natív zoomtiltás és órás távoli vezérlés. A telefonos forrás változatlan az órás `e3db5e2` commitban. |
| Korábbi átadott Wear referencia | `e3db5e20015bce137733763c50c0bf0c521ddf13`: megújult kezdőlap, kompakt pontos idő, natív automata szolgáltatás és kis körkijelzőre illesztett időzítő. |
| Kiinduló elfogadott tesztbuild-pár | Telefon `3e66ab0`, Wear `e41f09f`; ezekre épül a közös #179. |
| Automata mód #162 | Első közös megvalósítás a main fejlesztési alapjában. Órán indított edzésnél önálló natív órás időzítés; telefonon indítottnál telefonos tulajdon. |
| Wear-kezdőlap #170 | Első revamp megvalósítva; telefonos ikonok/téma, külön Beállítások, közvetlen törlés, felfelé húzható menü, Mini napló tördelése. Készülékes finomítás következhet. |
| Mérés és napló | Élő HR/kcal és elérhető kardióadat, Mini napló, elkülönített források és késői összesítés megmarad. |
| Automatikus ellenőrzés | 138/138 telefonos regresszió (a #180 mentésjavítással) és külön futtatótesztek; 90 Chromium-tesztcsomag; 20 célzott Kotlin állapot-/tükrözési/pihenőkör teszt; 51 Wear-emulátoros teszt; natív release gate-ek, teljesítménykapu és ellenőrzött aláírás. |
| Következő feladat | Készülékes automata próba, majd telefonos háttérrögzítés/új idővonal indítása az óráról és egységes munka/pihenő naplóbontás. |
| Utána: térkép #163 | Telefonos GPS-útvonal rögzítése és naplóbeli térképe; a koordináták rögzítése még nincs implementálva. |
| Kényelmi Wear-funkciók | Visszavonás, érintészár, forgatható szerkesztés, egyértelműbb mentési/szinkronállapot a közös tervben. |

A kanonikus következő lépések: [WEAR_OS_FEJLESZTESI_TERV.md](docs/wear/WEAR_OS_FEJLESZTESI_TERV.md). Új beszélgetéshez: [CHATGPT_FOLYTATAS.md](docs/CHATGPT_FOLYTATAS.md).

### Ellenőrzött tesztpár és kiadások

- [Telefonos tesztbuild / teljes gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352741468), APK SHA-256: `9ea25bb103d0aac6d862824cb18caa0082b69df735f3260512bfec4945178041`.
- [Wear tesztbuild / natív gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812467), APK SHA-256: `3f50d3bb36f0c0b31d731da6e3322e7cc3445308442860a7a630b79ba00db818`.
- [Wear 51 emulátoros teszt](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812492): 160/192/225 dp körkijelző, nagyított rendszerbetű és valódi időzítő háttérbe tett Activity mellett.
- Mindkét APK a korábbi kiadások tanúsítványával aláírt; a csomagazonosító `com.repforge.app`. Mindkettő telepítése és az első telefonos/órás adatszinkron szükséges az új közös protokollhoz.

Az eredeti [telefonos 1.3.4](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.4) és [Wear 1.3.4](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.4) publikált APK-ja a korábbi kiadott állapot. A main frissítése és az új tesztpár készülékes elfogadása külön történik; a jelen frissítés nem cseréli le a kiadott APK-kat és nem emeli a verziószámot.

### Aktuális pihenőjavítások — #179

A telefonon a kompakt időzítő Munka/Pihenő felirata megmarad a váltáskor. Az órás pihenőkör a tényleges automata szakasz teljes idejéből számol, nem az edzés alapértelmezett pihenőértékéből; 120 mp-nél az első másodperctől 1/120 részt fogyaszt. A telefonos kezdeti gyakorlatközi pihenő hossza is helyesen kerül át az órára. A fenti referencia-APK-k e javítások előtti átadott változatok; az aktuális main csomagok és CI az [Actionsön](https://github.com/fuloplevente1998/TrainPilot/actions?query=branch%3Amain) azonosíthatók.

### Ismert nyitott korlátok

Telefonon indított edzésnél az órás ismétlésrögzítés és egy új telefonos menetrend óráról indítása még várhat az alvó telefonos webfelület visszatérésére. Az óra ilyenkor „Telefonra vár” állapotot mutat. A három használati irány teljes készülékes elfogadása ezért még hátravan.

Az órás időzített sorozat a tényleges munka másodperceit összegzi; a meglévő telefonos intervallum a korábbi munka+pihenő értéket használja. Az egységes naplóbontás külön folytatás; régi adatot nem szabad találgatással átírni. Oldalankénti időmérés kézi marad, utána automatikus pihenővel. Órás GPS-szakaszkezelés és útvonaltárolás további feladat.

## Összevont verziók

| Verzió | Fejlesztési tartalom |
| --- | --- |
| [1.0](RELEASE_NOTES.md#10) | Alap edzéstervező/napló, Coach, fejlődés, fotók, mentés, kardió és közös szerkesztők. |
| [1.1](RELEASE_NOTES.md#11) | Célok, előnézetes újratervezés, beltéri intervallumok, Coach-indoklás; külön kezelt korábbi Bluetooth-kísérletek. |
| [1.2](RELEASE_NOTES.md#12) | Egészségnapló, profilvezérelt programok, naptár/felület/indítás javításai és Wear OS alapok. |
| [1.3](RELEASE_NOTES.md#13) | Párosított gyors edzés, Mini napló, mért edzések előzménye, egészségforrások és a telefonos órás appindítás tesztelt javítása. |

A részletes felhasználói változáslista a [RELEASE_NOTES.md](RELEASE_NOTES.md). A régi, kiadásonkénti [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) jegyzetek történeti/buildtechnikai archívumok; a bennük rögzített akkori jelöltstátusz nem írja felül ezt az aktuális állapotot.

## Következő készülékes ellenőrzés

A tesztpár telepítése és első szinkron után külön telefonos, külön órás és párosított indítással próbálandó:

- Súlyzós kézi rögzítés → pihenő → következő sorozat/gyakorlat; az app nem találhat ki ismétlést.
- Időalapú munka/pihenő/körök, nulla pihenő, utolsó sorozat és kézi végső mentés.
- 3–2–1: három 80 ms-os előjelzés; lejáratkor egy 1 másodperces, egybefüggő rezgés.
- Lezárt kijelző, háttérbe tett app, szünet/folytatás/kihagyás, értesítési vezérlés, kapcsolatvesztés és visszatérés.
- Telefonos GPS/intervallum együtt; újraindítás és ugyanahhoz a sorozathoz történő helyreállítás.
- Kis kerek kijelző, nagy betű, szerkesztő, hosszú név és a végső naplóadatok egyezése.

A mainre emelés felhasználói engedélye nem helyettesíti e próbák eredményét; a tényleges tapasztalatot az issue-ban és a közös tervben kell rögzíteni.

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
