# TrainPilot — fejlesztési állapot

## Kötelező közös munkaprotokoll — 2026. október 13-tól

**Prioritás:** A következő fejlesztési és hibajavítási feladatok előtt ezt a munkarendet követjük. A `DEVELOPMENT_STATUS.md` a központi állapotnyilvántartás, a GitHub Issues az egyedi feladatok nyilvántartása; a kettőt szinkronban kell tartani.

1. **Mikrolépések:** Az összetett munkát előzetesen 5–10 átlátható részfeladatra bontjuk. Egy körben csak **egy** részfeladatot végzünk el.
2. **Kötelező jóváhagyási pont:** Minden részfeladat után megállunk; a következő csak a felhasználó kifejezett „Mehet tovább” / „Folytasd” jóváhagyásával kezdődik. A jóváhagyás nem terjed ki automatikusan a mainre emelésre vagy kiadásra.
3. **Rövid válaszok:** Legfeljebb 200–300 szó vagy 40–50 sor kód válaszonként, fókuszált eredménnyel és a szükséges hivatkozásokkal. A nagyobb változtatásokat kisebb, áttekinthető egységekre bontjuk.
4. **Állapotjelzés:** Minden kör végén kötelező a `[Aktuális lépés: X/Y | Állapot: Jóváhagyásra vár]` sor, a tényleges lépés és státusz alapján.
5. **GitHub-követés:** Felméréskor először a main, ez a fájl, a részletes fejlesztési terv és a meglévő Issues/PR-ek aktuális állapotát vizsgáljuk. Meglévő feladatot nem duplikálunk. Az új fejlesztés vagy hiba saját Issue-t, elfogadási feltételeket és indokolt prioritást kap.
6. **Biztonságos változtatás:** Célzott fejlesztési ág → ellenőrzés/tesztek → PR → felhasználói jóváhagyás → main. A tesztelt és a fizikai eszközön elfogadott állapot külön szerepel. Verziószámot, release-t és APK-t nem változtatunk önkényesen.
7. **Dokumentálás:** A részfeladat lezárásakor frissítjük az érintett Issue/PR állapotát és szükség szerint ezt a fájlt: mi készült el, mi maradt hátra, mi tesztelt, mi igényel készülékes visszajelzést.
8. **Fejlesztési szakaszok:** (1) állapotfelmérés; (2) feladatok/Issues összevetése; (3) prioritások; (4) Issue-rendezés; (5) egyenkénti fejlesztés; (6) tesztelés; (7) PR és jóváhagyás; (8) dokumentáció és következő kör.

**Munkaprotokoll állapota:** 2026. október 13-án jóváhagyva, a mainre egyesítve (#187). A feladatfelmérés és az új Issue-k létrehozása megtörtént; az alábbi sorrend az elfogadott kiinduló terv.

## Jóváhagyott GitHub-prioritások — 2026. október 13.

**12 nyitott fejlesztési Issue.** A P0–P3 besorolás a GitHub Issue-címkéken is szerepel. A sorrend a tervezett munkavégzés prioritását jelzi, **nem** azt, hogy a feladat már elkészült vagy készülékesen elfogadott.

| Prioritás | Issue | Fejlesztés / következő teendő |
| --- | --- | --- |
| **P0 – kritikus** | [#162](https://github.com/fuloplevente1998/TrainPilot/issues/162) | Automata edzés készülékes elfogadása, telefonos natív háttérrögzítés, egységes munka/pihenő/szünet naplózás |
| **P0 – kritikus** | [#157](https://github.com/fuloplevente1998/TrainPilot/issues/157) | Gyors edzésből órás appindítás és tényleges háttérmérés készülékes visszaellenőrzése |
| **P1 – magas** | [#158](https://github.com/fuloplevente1998/TrainPilot/issues/158) | TrainPilot Wear és Health Connect mért adatainak forráshű megjelenítése |
| **P1 – magas** | [#159](https://github.com/fuloplevente1998/TrainPilot/issues/159) | Wear Mini Napló bővítése, hitelesített edzés- és mérési adatok |
| **P1 – magas** | [#191](https://github.com/fuloplevente1998/TrainPilot/issues/191) | Órás helyi mentés, függő szinkron, visszaigazolás és hibakezelés egyértelmű állapotai |
| **P1 – magas** | [#163](https://github.com/fuloplevente1998/TrainPilot/issues/163) | Telefonos GPS-útvonalpontok rögzítése és naplótérkép |
| **P2 – közepes** | [#188](https://github.com/fuloplevente1998/TrainPilot/issues/188) | Utolsó sorozatrögzítés biztonságos visszavonása |
| **P2 – közepes** | [#189](https://github.com/fuloplevente1998/TrainPilot/issues/189) | Wear érintészár edzés közben |
| **P2 – közepes** | [#192](https://github.com/fuloplevente1998/TrainPilot/issues/192) | Oldalankénti (bal/jobb) automata időzítés és oldalcserés pihenő |
| **P2 – közepes** | [#193](https://github.com/fuloplevente1998/TrainPilot/issues/193) | Önálló órás GPS-útvonalszakaszok, tárolás és szinkron |
| **P3 – későbbi** | [#190](https://github.com/fuloplevente1998/TrainPilot/issues/190) | Korona/lünetta alapú súly- és ismétlésszerkesztés |
| **P3 – későbbi** | [#184](https://github.com/fuloplevente1998/TrainPilot/issues/184) | Samsung Health energiapontszám és TrainPilot Coach közös ajánlásának tervezése |

**Kötelező fejlesztési függőségek és indítási feltételek:**
1. Először a **P0** automata rendszer és gyorsindítás készülékes hibáit vizsgáljuk, különösen a három használati irányt (csak telefon, önálló óra, párosított működés), a kijelzőzárat és a szinkront. A #162-ben külön rögzítjük a fizikai elfogadás eredményét.
2. A **#163 telefonos térképet** csak a #162 alapműködésének stabilizálása után kezdjük. Az órás **#193 GPS-bővítés** a #163 mentési/megjelenítési modelljére épít, nem helyettesíti azt.
3. A #191 mentési állapot és a #159 Mini Napló a tényleges szinkron-visszaigazolásra és hiteles mérési forrásokra épül. A #192 az automata idővonal és a munka/pihenő naplóbontás rendezése után következik.
4. A P2/P3 feladatok külön Issue-n, külön ágon és külön PR-ben haladnak. Egy feladat után megállunk a felhasználó jóváhagyásáig; verzióemelés és mainre emelés külön engedélyhez kötött.

**Nem tervezett:** [#120 – HRV-adathozzáférés](https://github.com/fuloplevente1998/TrainPilot/issues/120) felhasználói kérésre `closed / not_planned` állapotú. A szükséges adatok megbízható hozzáférése jelenleg nem igazolt; nem része a 12 nyitott fejlesztésnek.

Frissítve: **2026. október 13.** A felhasználói verzióleírások négy családot használnak: **1.0, 1.1, 1.2, 1.3**. A buildazonosítók, APK-k, tagek és release-ek megmaradnak.


## P0 fejlesztési kör: #162 → #157 (2026. október 13., tesztelésre vár)

### Helyi folytatás — 5/10: natív parancsbekötés és űrlapfüggetlen indítás

A helyi `local/p0-162-native` ág a #195 `e528d1e` forrásából és a 4/10-es `3fff44c` alapból folytatódik. A GitHub sorrendjének alapja továbbra is a #194 `0b37ced` commitja: **#162 → #157**.

- A `WearCommandListenerService` a 2-es protokollú ismétlésrögzítést és új telefonos idővonal indítását már a natív végrehajtónak adja át. Stabil gyakorlat-/sorozat-ID, generáció és külön végrehajtási revízió azonosítja a célt. A régi órás protokoll és a nem támogatott mérések a korábbi útvonalon maradnak.
- A rögzítés, a bizonylat és az előkészített következő idővonal egy SQLite-tranzakcióban mentődik. A terv állapota `pending`, `started` vagy `interrupted`; az ismételt kézbesítés nem indít második órát és nem nullázza az eltelt időt. A gyakorlat saját pihenője felülírja az általános beállítást.
- A telefonon látható edzés előre indítja az automata vezérlőt a meglévő foreground szolgáltatásban. Háttérből új foreground szolgáltatás indítására nem támaszkodunk. Az értesítésből leállítható, az edzés lezárásakor megszűnik; az előkészített vezérlés felső időkorlátja 24 óra. Megszakadás után nincs automatikus utólagos munka vagy rezgéssorozat.
- A JavaScript előbb összevezeti a natív eredményt, egy mentésben tárolja a piszkozatot és az eseménykurzort, majd nyugtáz és publikál. A nyitott szerkesztőt és élő sorozatreferenciákat megtartja. Újabb, ütköző telefonos értéket nem ír felül; sikertelen mentésnél nincs nyugta. Restore és telefonos befejezés körül natív írási védelem működik.
- A `startTimed` a mentett intervallumbeállítást használja; nem keres HTML-űrlapot. Az óra csak a támogatott, telefon által vezérelt műveletekhez adja az új protokollmezőket.

**Helyi ellenőrzés:** 34 Android 33 Robolectric/JUnit teszt a rögzítési tárolóra, tervekre és valódi natív szolgáltatáslogikára; 12 Wear JVM modell-/protokollteszt; 139 Node-regressziós csomag, köztük a teljes telefonos runtime új összevezetési és űrlapfüggetlen indítási próbái. A Java integráció API 35 osztályokkal és a valódi Capacitor/AndroidX/Play Services API-kkal fordult. A helyi izolált szolgáltatásharness generált erőforráskötéseket és az érintetlen GPS-szolgáltatás helyettesítőjét használja: ez **nem Gradle APK-build, fizikai Data Layer vagy készülékes elfogadás**.

**A 6/10-es lépésben folytatandó:** munka/pihenő/szünet teljes natív időnaplója, időalapú befejezések tartós beolvasztása, explicit telefonos javítás/visszavonás és azok ütközéskezelése. A már rögzített értékeket a jelenlegi előkészítési API nem engedi egyszerű pillanatkép-felülírással módosítani. Amíg az időalapú eredmény nincs összevezetve, a natív vezérlő nem engedi egy új menetrenddel felülírni. A GPS-tulajdonos előkészítése továbbra is külön feladat; a távolsági edzés meglévő útvonala marad használatban.

**GitHub / kiadás:** mindez egyelőre helyi, ellenőrizhető fejlesztési részeredmény. A #195 továbbra is draft, a remote fej `e528d1e`; a main, a kiadások és az **1.3.4 /2730** verzió nem változik. Nincs kódpush, közös Release Gate vagy új APK. A #162 és #157 nem zárható a mostani teszteredményből.

### Előző helyi lépés — 4/10: natív rögzítési alap (`3fff44c`)

A #195 `e528d1e` forrásából külön helyi munkafában készült a `PhoneWorkoutExecutionStore` és `PhoneWorkoutRecording`. A külön SQLite-adatbázis egy tranzakcióban menti a sorozatot és az idempotens parancsbizonylatot; generáció/revízió és stabil sorozat-ID védi a célt. A JavaScript által később beolvasandó események külön nyugtázhatók, a parancsbizonylat akkor is megmarad. A már rögzített sorozat értékeit egy elavult pillanatkép nem módosíthatja. A restore-zárolás, lezárt edzés és hiányzó protokollmezők védelme is része az alapnak.

**Hatókör:** kézi, ismétléses sorozat natív adatrögzítési alapja. A 4/10-es lépés végén a szolgáltatás/órás parancs/WebView még nem volt bekötve; a pihenő- és idővonalindítás, időalapú/távolsági sorozat és egységes időnapló későbbi lépés. Ez még nem oldja meg a #162 készülékes háttérműködését, és nem zárja a #157-et.

**Ellenőrzés:** 18 sikeres helyi Robolectric/JUnit teszt Android 33 környezetben, az új forrásokat Java 17 célra fordítva. Valós SQLite-tranzakció visszagörgetése injektált írási hibánál, újranyitás előtti/utáni parancsismétlés, párhuzamos duplikáció, részleges eseménynyugtázás, stale pillanatkép, értékfelülírás, restore-zár, generációváltás és valódi nulla vizsgálva. A helyi futtató nem a teljes Android Release Gate; APK nem készült.

A kód és ez az állapotfeljegyzés egyelőre helyi commit; a #195 remote fejét, main-t és verziót ez a lépés nem módosítja. A kódpush a már rögzített közös gate-szakaszban történik.


A közös `fix/p0-162-157-safe-wear-sync` munkában a következő **részjavítások** készültek; ezek önmagukban nem jelentik a két Issue végleges teljesítését:

- **#162, natív adatbiztonság:** a telefon órás parancssora nem dobhatja el észrevétlenül a legrégebbi, még nem nyugtázott sorozatrögzítést, amikor eléri a 64 elemnyi keretet. A Data Layer tétel nyitva marad, és a kapacitás felszabadulása után újra felolvasható. A nyugtázás és duplikációvédelmi jelző tartós mentést használ; sikertelen tartós nyugtázásnál a Data Layer tétel nem törlődik. Robolectric regressziók készültek a telítődésre, újrafelvételre és az automata állapotpillanatkép cseréjére.
- **#157, gyors edzés:** a meglévő telefonos inline gyorsindításban ellenőrizendő az előbb snapshot, utána egyszeri óranyitás sorrend. A célzott Node-regresszió kiegészült sikertelen küldés/gyors helyreállás és visszautasított óranyitás tesztjével; nincs új óranyitás puszta polling miatt.

**Még nyitott:** #162-ben a fenti helyi natív integráció teljes körű ellenőrzése, az időalapú eredmények és a munka/pihenő/szünet egységes natív naplója; #157-ben a tényleges gyorsedzés-indítás és friss mérés ellenőrzése fizikai órán. A #162 és #157 Issue egyelőre **nyitva marad**.

**Készülékes elfogadás (mindkét feladathoz):** új telefonos gyors edzés → óra app egyszer nyílik → friss HR/kcal indul → telefon és óra lezárva mérés folytatódik → sorozatrögzítés, pihenő, kapcsolatvesztés/visszatérés → egyetlen naplóbejegyzés és külön eredetű mért adatok. Ismételjük meg tervezett edzéssel, önálló órás és önálló telefonos indítással; rögzítsük a tényleges eredményeket az Issue-kban.

**Kiadási szabály:** a felhasználó kérésére a közös release gate eredményét és az új teszt-APK-kat **csak a #162 és #157 végrehajtása után** szabad átadni; verziószámot előzetes jóváhagyás nélkül nem változtatunk. A CI-eredmény és a fizikai készülékes elfogadás külön igazolandó.

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
