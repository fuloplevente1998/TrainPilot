# P0 #162 → #157 — közös tesztpár és készülékes elfogadás

Frissítve: **2026. október 13.** Közös draft PR: [#195](https://github.com/fuloplevente1998/TrainPilot/pull/195). Forráság: `fix/p0-162-157-safe-wear-sync`. Verzió mindkét appban **1.3.4 /2730**. A main és a nyilvános kiadások nem változnak; #162 és #157 a tényleges telefonos/órás eredményig nyitva marad.

## Aktuális TIME_ORDER javító telefonos APK — `8746b14`

A `1b8fb43` készülékes próbájának 2422-es képén **`E162-RECONCILE-TIME_ORDER`** jelent meg. Az élő natív időzítő gyakrabban szolgáltat friss mérést, mint az egyszer másodpercenként mentett natív checkpoint. Az összevezetés ezért egy frissebb élő mérés után régebbi mentett adatot is kaphatott; a korábbi kód ezt tiltott időcsökkenésként elutasította.

A javítás azonos kulcsú és azonos forrású időnaplónál a teljes egészében régebbi számlálóvektort mellőzi, és az egész frissebb mérést megtartja. Nem csökkenti az időket, nem ad hozzá újra már beszámított időt, és nem állít össze külön mezőkből mesterséges maximumokat. A nem összehasonlítható, egymásnak ellentmondó adatok továbbra is hibát jelentenek; forrásváltás azonos kulcson külön `TIME_OWNER` hiba. Az új sorozat/esemény és a mentési kurzor régebbi idő-checkpoint mellett is tartósan feldolgozható és nyugtázható.

- Forrás: **`8746b14a606045579df790d831cd068d47e86b21`**, **1.3.4 /2730**. [Telefonos artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442346873/artifacts/11837876879). Átadott fájl: `TrainPilot-1.3.4-P0-8746b14-Telefon.apk`.
- SHA-256: `13a16eee207d4846fc17bb9fb8bde5c7e3fd19ca02c2171a7ccde198b29b3208`; méret **5 811 649 byte**.
- [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442346873): SUCCESS; mind a 91 Chromium-csomag, natív release tesztek, aláírt APK. Az első próbában a böngészős haptikus fixture egy rövid jelzést kihagyott; ugyanaz a csoport változatlan kóddal és változatlan elvárással újrafuttatva sikeres. A tesztet nem hagytuk ki és nem gyengítettük.
- [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442356204) és [Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442356313): SUCCESS.
- [Kibővített Android–WebView próba](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442346888): SUCCESS. A tényleges natív API-ból mért frissebb élő ledger és régebbi mentett ledger közti eltérés megfigyelése kötelező; utána az összevezetés, élesített vezérlő és futó natív pihenő hiba nélkül ellenőrzött. [Teszt/logcat artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38442346888/artifacts/11837300761).
- A célzott időnapló-regresszió a régi forráson az `Automatic time moved backwards` hibát reprodukálta, a javításon sikeres. Külön vizsgáltuk a frissebb mért idő megőrzését, a natív sorozat és kurzor feldolgozását, az atomikus mentést, valamint a továbbra is elutasított ellentmondó és eltérő forrású naplókat.
- Letöltés után a csomag/verzió, hash, valódi aláírás és eredeti aláíró ellenőrizve; a csomagolt JS/CSS és a source ZIP érintett natív forrásai egyeznek az ellenőrzött commit fájljaival.

**Újrapróba:** ezt az APK-t a meglévő telefonos app fölé kell telepíteni; az órán maradhat a `859a6a1` csomag. Telefonról indított automata fekve nyomásnál rögzíts egy sorozatot, és hagyd futni a pihenőt néhány másodpercig a telefonos oldalon is, hogy a sűrűbb élő időfrissítés megtörténjen. Ha nincs üzenet, zárd le a telefont, és folytasd az órán. Rövid tervezett edzésen is ismétlendő. A 2422-es készülékes eset elfogadása még hátravan; #162/#157 nyitott, #195 draft, main és release változatlan.

## Korábbi rögzítési zár javítása — `1b8fb43`

A `0bcc1ea` készülékes próbájának 2420-as képén a hiba **`E162-RECONCILE-NATIVE`**: az összevezetésnél, az élesítés előtt vagy a rögzítés utáni összevezetéskor történt elakadás. A teljes Android–WebView próba egy valódi rögzítési versenyt reprodukált: a natív sorozat és nyugtázás elkészült, de a hívó telefonos művelet hibás `false` eredménnyel tért vissza, mert a párhuzamos frissítés átvette az összevezetést.

A javítás ugyanazzal a zárral védi az egész telefonos rögzítést: előkészítés → natív parancs → tartós telefonos mentés → nyugtázás. A polling és az órás parancssor nem léphet a telefonos művelet közepébe. A `/kar` és `/láb` natív útvonala megmarad, és az oldalankénti kézi munkaidő rögzítése is támogatott. Az összevezetés ismert adat-/idő-/szerkesztési hibái külön `E162` kódot kapnak.

**Igazolás és korlát:** ugyanaz a teljes Android 35 emulátoros teszt [a javítás előtt elbukott](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38437293282) (`2c33196`), [a javításon sikeres](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438431181) (`1b8fb43`). Valódi csomagolt WebView, Capacitor, SQLite és foreground szolgáltatás futott: rögzített sorozat, élesített vezérlő, futó 120 mp-es natív pihenő, sikeres visszatérés, hibajelzés nélkül. A teszt nem párosított fizikai óra, kijelzőzár vagy tényleges Health Services mérés; az emulátor a rögzítési versenyt, nem a fizikai hibakód teljes eredetét igazolta. A 2420-as készülékes esetet újra kell próbálni.

- Forrás: **`1b8fb43cb4cf22ab2fa708ee5dca5838037eedae`**, **1.3.4 /2730**. [Telefonos artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438430983/artifacts/11834252261). Átadott fájl: `TrainPilot-1.3.4-P0-1b8fb43-Telefon.apk`.
- SHA-256: `0a5b4037b051fb6c55ced3568f0a362eff8cc7ff9f023cea227b994042506754`; méret **5 811 549 byte**.
- [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438430983): SUCCESS; 91 Chromium-csomag, teljes natív release tesztek, aláírt APK.
- [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438438200) és [Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438438072): SUCCESS.
- [Teljes telefonos Android–WebView próba](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438431181): SUCCESS, egy célzott emulátoros teszt. [Teszt/logcat artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38438431181/artifacts/11834585558).
- Letöltés után a csomag/verzió, hash és valódi aláírás ellenőrizve; az eredeti aláíró megmarad. A csomagolt JS/CSS egyezik a forrással; a forrás ZIP natív rögzítő/tároló/szolgáltatás fájljai is egyeznek.

**Újrapróba:** telepítsd a telefonos app fölé ezt az APK-t; a `859a6a1` órás csomag maradhat. Telefonon indíts sima fekve nyomást, kapcsold be az automata módot, majd rögzíts egy sorozatot. Ha nincs figyelmeztetés, a pihenő alatt zárd le a telefont, és az órán rögzítsd a következő sorozatot. Ezután rövid tervezett edzésen is ellenőrizd. Új hiba esetén a pontos `E162-...` kódot rögzítsük. A fizikai eredményig #162/#157 nyitott, #195 draft; main, verzió és nyilvános release változatlan.

## Korábbi részeredmény és hibakereső APK — `0bcc1ea`

**Ez még nem a közös háttérhiba igazolt megoldása.** A `c613a75` készülékes próbájában (#2417–2418) telefonról indított automata edzésnél, a sorozat telefonos rögzítésekor háttérvezérlési figyelmeztetés jelent meg. A felhasználó utóbb megerősítette: **minden edzésnél előjött**, nem csak gyorsedzésnél vagy oldalankénti ismétlésnél. A korábbi általános figyelmeztetés eltakarta a hibát kiváltó lépést; a pontos készülékes ok továbbra sem igazolt.

- A `/kar` és `/láb` ismétléses gyakorlatok telefonos és natív rögzítése közös natív útvonalra került. Az oldalankénti szám nem duplázódik; pihenő, lezárt/alvó felület melletti vezérlés és ismételt parancs védelme külön tesztelt.
- Az előkészítés/összevezetés/élesítés hibája `E162-...` kódot kap. A figyelmeztetés többé nem állítja, hogy a funkció még nincs elkészítve. A hibakód ismert hibatípust és műveletet tartalmaz, edzésazonosítót, mérési adatot vagy nyers hibaüzenetet nem. Sikeres újrapróbálás törli az aktuális hibajelzést.
- Forrás: **`0bcc1ea33a08cc9ba372f744e431b7b17f0fd337`**, **1.3.4 /2730**. [Telefonos artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38434295383/artifacts/11833450023), az átadott név `TrainPilot-1.3.4-P0-0bcc1ea-Telefon.apk`.
- SHA-256: `40b51fdc5de73d80f9b1d4de20075a4f26d72503c230dddbcf10ae7ae4d1f806`; méret **5 811 389 byte**.
- [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38434295383): SUCCESS; 91 Chromium-csomag, natív release tesztek, aláírt APK. [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38434300455) és [Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38434300435): SUCCESS.
- Letöltés után a hash, csomag/verzió, eredeti aláíró és valódi aláírás ellenőrizve; a csomagolt JS/CSS megegyezik a forrással, a natív rögzítés forrása is egyezik. A célzott helyi Robolectric integrációs harness 43 tesztet futtatott sikeresen; ez nem fizikai készülékes elfogadás.

**Következő próba:** a meglévő telefonos app fölé telepítsd ezt az APK-t, az órán maradhat a `859a6a1` Wear APK. Telefonon indíts sima fekve nyomást, kapcsold be az automata módot, majd rögzíts egy sorozatot. Ha figyelmeztet, az új `E162-...` kódot kell rögzíteni; addig a közös háttérhiba nincs megoldottnak tekintve. Ha nincs figyelmeztetés, jöhet a pihenő és a következő órás rögzítés lezárt telefon mellett, majd ugyanez tervezett edzésnél. Main/release változatlan; #195 draft, #162/#157 nyitott.

## Korábbi telefonos megjelenítési APK — `c613a75`

A `859a6a1` készülékes próbájában (#2412) a szinkron nullás távolságmezője súlyzós gyakorlatokra is bekapcsolta a GPS/beltéri/lépés paneleket, az alsó automata állapotszöveg levágódott. A `0f49583` javítás ezeket megszünteti, a korábban hibásan besorolt aktív piszkozatot is helyreállítja; a súly, ismétlés és kész állapot megmarad. Futásnál, görkorcsolyánál és valódi távolsági gyakorlatnál a mérési panelek továbbra is elérhetők. A #2415 visszajelzés alapján a `c613a75` megszünteti a kényszerített második sort: az „Az óra vezérli” és a rövid munka/pihenő jelzés az „Automata mód” mellett látható, hosszabb szöveg a teljes alsó panelszélességet használva szükség szerint tördelhető. A visszaszámláló az alsó két gomb között marad.

- Telefonos APK-forrás: **`c613a7507514f7fda2641bbf7a8ba5b0303b7598`**, verzió **1.3.4 /2730**.
- [Telefonos artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38427359830/artifacts/11829562279): ZIP, benne `TrainPilot-1.3.4.apk`. Az átadott név: `TrainPilot-1.3.4-P0-c613a75-Telefon.apk`.
- APK SHA-256: `8a94e3578e2b794d364a1983c7220bd267347a8b5797bfe3f2b31b92b7cff9d7`; méret 5 810 793 byte.
- [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38427359830): SUCCESS, **91 Chromium-csomag**, release JVM-tesztek és aláírt APK.
- [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38427371230) és [Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38427371216): SUCCESS.
- Letöltés után a csomag, verzió, valódi aláírás és eredeti aláíró ellenőrizve; az APK csomagolt `app.js` és `features-110.css` fájlja byte-ra megegyezik a javított forrással. A lenti aláíró tanúsítvány megmaradt.

A célzott regresszió a korábbi forráson három téves mérési panelt reprodukált, a javításon sikeres. Vizsgáltuk a tényleges Wear nullás mezőjét, a hibás meglévő súlyzós/időalapú piszkozatot, változatlan sorozatértékeket, négy nyelvet, 320/393 px szélességet és a megmaradó futás/görkorcsolya/egyéni/legacy távolságkezelést. Az új megjelenítési állítások mindkét szélességen igazolják, hogy az „Az óra vezérli” a módválasztóval egy sorban marad. A javító APK készülékes elfogadása még hátravan; main és release változatlan, #195 draft, #162 és #157 nyitott.

**Ez a korábbi megjelenítési csomag; az aktuális telefonos APK-t a fenti szakasz azonosítja.** Az órán az alább rögzített `859a6a1` Wear APK használható: az órás natív kód ebben a javításban nem változott. Először súlyzós edzésnél kapcsold be az automata módot, ellenőrizd az eltűnt téves paneleket és a teljes alsó szöveget; utána futás/görkorcsolya gyakorlatnál a megmaradó mérési lehetőségeket. A további P0 próbák a lenti táblázat szerint folytatandók.

## Korábbi közös tesztpár — `859a6a1` (az órás APK tovább használható)

**A 8–9. kör elkészült, készülékes visszajelzésre vár.** A két aláírt APK közös forráscommitja **`859a6a15d1d7d02ebbfaba4385e857eb9e824014`**. A későbbi, kizárólag `docs/` alatti dokumentációs commitok nem módosítják az APK-k kódját. A régebbi, azonos verziószámú APK-k nem azonosítják ezt a P0 forrást.

| Csomag | GitHub artifact (ZIP, benne az APK) | APK méret |
| --- | --- | --- |
| Telefon | [TrainPilot — 38406920445](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406920445/artifacts/11819576082), `TrainPilot-1.3.4.apk` | 5 810 453 byte |
| Wear | [TrainPilot Wear — 38406919063](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406919063/artifacts/11818419041), `TrainPilot-1.3.4-wear.apk` | 4 058 635 byte |

Az átadott, átnevezett telefonos fájl: `TrainPilot-1.3.4-P0-859a6a1-Telefon.apk`; az órás: `TrainPilot-1.3.4-P0-859a6a1-Wear.apk`. Az átnevezés nem módosítja a byte-okat. SHA-256:

```text
d310eb1181ebd11f649480e754873a30f8b7fc8b80d4eef66ca979b051e4bdb0  TrainPilot-1.3.4-P0-859a6a1-Telefon.apk
8bf79eb62a94252f26cd6ac92cac1cf7e02e20a5a063b63cdff151fb6406cdb8  TrainPilot-1.3.4-P0-859a6a1-Wear.apk
```

Mindkét csomag **`com.repforge.app`**, verzió **1.3.4 /2730**, nem debug APK. Az aláírásokat a gate-ek és letöltés után az Android `apksig` ellenőrizte. Mindkét aláíró tanúsítvány SHA-256-ja megegyezik az eredeti v1.0.0-éval: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. A letöltött APK-k hash-e megegyezik a CI manifestjével; a telefon csomagolt `assets/public/app.js` fájlja byte-ra egyezik a forrással, az új natív végrehajtó/tároló/időnapló osztályai a DEX-ben szerepelnek. A telefonos source ZIP és a Wear forrásmanifest ugyanezt a `859a6a1` commitot azonosítja.

Az Actions artifactok 14 napig őrződnek; GitHub-bejelentkezés szükséges lehet a letöltéshez. Lejáratuk után a fenti commitból a meglévő branch build-workflowkkal újra készíthető tesztpár; ez nem jogosít mainre emelésre vagy release-asset cserére.

## Ellenőrzési bizonyíték

| Ellenőrzés | Eredmény / forrás |
| --- | --- |
| [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406930081) | SUCCESS; 140 Node-csomag, 0 hiba; `859a6a1` |
| [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406920445) | SUCCESS; mind a 90 Chromium-csomag, release JVM-tesztek, aláírt APK, verzió/csomag/aláíró és csomagolt forrás; `859a6a1` |
| [Wear Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406919063) | SUCCESS; release JVM-tesztek, aláírt és méretellenőrzött APK, eredeti aláíró; `859a6a1` |
| [Phase 7 Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38406929858) | SUCCESS; teljesítmény- és szerkezeti regressziókorlátok; `859a6a1` |
| [Wear round display UI](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38404763797) | SUCCESS; 51 kerek emulátoros próba; `a742efb` |

A kerek emulátorteszt után csak telefonos JS, telefonos teszt és dokumentáció változott. Az `android/wear` Git tree mind `a742efb`, mind `859a6a1` esetén **`8bf882cb361096dac741f6734c1cba5a01e754c9`**: a készülékes Wear APK natív forrása és tesztjei az emulátoron igazoltakkal azonosak. Az első, `a742efb` telefonos gate a régi fájlválasztó-fixture időzítése miatt megszakadt; a javított fixture és preflight-zár a végső teljes telefonos gate-ben sikeres. Nem váltottuk ki vagy hagytuk ki a teljes gate-et.

A 6–7. kör teljes natív debug-validációja ugyanennek a natív kódnak 145 telefonos és 97 Wear unit tesztjét igazolta. Ez nem további 242 külön teszt a mostani release-tesztekhez hozzáadva, és egyik CI-futás sem fizikai Health Services szenzormérés.

## Telepítés

A friss APK-kat a meglévő telepítés fölé kell telepíteni; az aktuális telefonos javító csomagot a fenti szakasz azonosítja. Az alkalmazás eltávolítása nem része a próbának. Az órán az Órás mérések legyen bekapcsolva, a korábban kért szenzorengedélyek megadva; másik alkalmazás ne mérjen párhuzamos edzést. A két eszköz legyen kapcsolódva. Sikertelen indításkor a pontos órás/telefonos állapotot is rögzítsük; az app megnyílása önmagában nem bizonyít szenzormérést.

## Rövid próbamenet

| Próba | Teendő | Elfogadás |
| --- | --- | --- |
| #157 gyorsindítás | A lezárt órát hagyd érintetlenül; telefonon indíts új gyors, súlyzós edzést. | Az órás TrainPilot egyszer megnyílik, ugyanaz a gyakorlat/edzés jelenik meg, majd tényleges friss HR/kcal érkezik. A puszta „Mérés indul” nem siker. |
| #162 lezárt telefon | Automata módban két kézi, ismétléses sorozat; az elsőt az órán rögzítsd, miközben a telefon már lezárt. | Rögzítés és pihenő a telefon felélesztése nélkül működik. A pihenő lejár, a következő sorozat készen áll; ismétlést továbbra is te rögzítesz. |
| Hosszú pihenő / rezgés | Válassz 120 mp pihenőt. Figyeld a teljes kört és az utolsó másodperceket. | A kör a teljes 120 mp alatt fogy. A végén három rövid előjelzés és egy 1 mp-es rezgés; ugyanarra a váltásra nincs kettős jelzés. |
| Időalapú automata | Rövid időalapú gyakorlat több sorozattal, utána ismétléses gyakorlat; a telefon kijelzője lezárt. | Munka/pihenő/sorozatléptetés működik, az időalapú kész sorozat nem akadályozza a következő kézi rögzítést. Felnyitáskor nincs eltérő vagy ismételt eredmény. |
| Szünet / kihagyás | Pihenőnél szüneteltesd, várj néhány másodpercet, folytasd; később hagyd ki a maradék pihenőt. | A kézi szünet külön idő; a kihagyott pihenő nem lesz elvégzett pihenő vagy munka. |
| Szerkesztés | Pihenő közben javíts egy előző kész sorozatot. Később egy kézi sorozat végzése közben javíts korábbi adatot. | A pihenő nem számolódik munkának; az aktuális kézi sorozat eltelt ideje nem veszik el. A súly/ismétlés javítása megmarad. |
| Kapcsolatvesztés | Rövid telefon–óra megszakadás, egy órás rögzítés, majd visszakapcsolódás és telefonos megnyitás. | Nincs elveszett vagy kétszer rögzített sorozat; a függő szinkron látható és helyreáll. A telefonos timer kapcsolat nélkül is a saját vezérlőjén fut. |
| Mentés / napló | Telefonon fejezd be és mentsd el az edzést. | Egyetlen naplóbejegyzés, stabil kész sorozatok, helyes mérési forrás. A TrainPilot Wear adatok és a Health Connect adatok elkülönülnek. |
| További irányok | Rövid tervezett edzés, külön önálló órás és önálló telefonos próba. | A meglévő indítási és automata működés megmarad. Telefon nélkül nem követelünk telefonos szinkront. |

A háttérben ténylegesen elvégzett munka-, pihenő- és szünetidő a session külön `automaticTime162` naplójába kerül. Ezt a belső adatot a UI jelenleg nem feltétlenül mutatja külön bontásban; bizonytalan összegnél konkrét edzésazonosítóval/exporttal ellenőrizzük, ne a legacy időalapú `reps` mezőt adjuk hozzá újra. GPS és oldalankénti automata bővítés nem része ennek a körnek.

## Eredményrögzítés

Készülékes eredmény még nincs. A visszajelzés tartalmazza: melyik fenti próba, indítás eszköze, telefon/óra lezárása, mi történt, látható HR/kcal, valamint hogy a naplóban egyetlen edzés és helyes sorozatok maradtak-e. Hibánál a pontos képernyőállapot vagy hibaüzenet és a hozzávetőleges idő segít.

Sikeres CI vagy telepíthető APK nem zárja automatikusan az Issue-kat. Mainre emelés és publikált release a felhasználó későbbi elfogadása szerint történhet.
