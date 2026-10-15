## Legfrissebb készülékes hiba — az engedély megvan, a Wear mérés mégis leáll

A felhasználó megerősítette a `0d1ecf1` Wear telepítését és az újrapróbálás utáni hibát. Új fotón `Missing permissions: ACCESS_FINE_LOCATION`, majd a rendszerbeállításban **Pontos helyadatok bekapcsolva / Az app használata közben** látszik. Nem tekintjük a mérést elfogadottnak, és nem állítjuk, hogy a felhasználó nem adott engedélyt.

Feltárt kódhiba: a `WearHealthService` a `location` foreground típust csak a GPS-kapcsoló mellett kérte, miközben Health Services a már engedélyezett helyhez kötött mozgásadatokat GPS nélkül is kérhette. Javítás: a service `location` típusa a meglévő pontos helyengedélyhez igazodik; az `ExerciseConfig.isGpsEnabled` külön, a felhasználó GPS-szándékát őrzi. A konkrét helyengedélyhiba megjegyzése után az explicit újrapróbálás tud helyengedélyt kérni; elutasításkor nincs hibás indítás vagy kitalált adat. A hiba oka a mérési oldal elejére kerül, az összesítések megmaradnak. Új JUnit és 160/192/225 dp + nagy betűs Compose-regresszió szükséges GitHubon.

Ez **Wear-javítás**; a telefonos `0d1ecf1` APK/map forrása változatlan. Új aláírt órás APK és fizikai próba szükséges; a verzió továbbra is **1.3.4 /2730**, #205 draft / #163 nyitott. Az aktuális CI/APK/hash az issue/PR tetején ellenőrizendő. Az alábbi korábbi helyengedély-magyarázatot ez pontosítja.

## Előző visszajelzés — #163 térkép és órás helyengedély

A felhasználó a `97cce85` térképét kezdetnek elfogadta, piros X bezárást kért, és az órán `Missing permissions: ACCESS_FINE_LOCATION` hibát mutatott. A #205 draft folytatása: akadálymentes piros X; azonosítható Capacitor User-Agent mellett opt-in OSM-háttér, copyright-link és HTTP-cache, hibánál megmaradó SVG. Órán helyengedély nélkül általános szenzormérés, GPS-táv/sebesség nélkül; az órás GPS külön engedélyt kér. A régi konkrét helyengedélyhibás indítás újrapróbálható a megőrzött összesítésekkel. Az új forrás friss GitHub CI-je és aláírt **telefonos + Wear tesztpárja** szükséges; helyi teljes teszt/build nincs. **1.3.4 /2730**, main/release változatlan. Aktuális eredmény és hash az #163/#205 oldalán; az alábbi korábbi provider-korlátok az új forráson már módosultak.

## Előző folytatás — 2026-10-15, #163 GPS 8/8

A #199 már mainen van; az aktív munka **#163 / #205 draft PR**, `feat/163-phone-gps-route-model`. A `3af303b` [release gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38673756408) a GPS-visszaállítási Chromium-teszten elbukott, így APK nem készült. A mostani javítás megvárja az aszinkron `restoreText` sikerét, és a zárolást megszerző belépés helyett csak a tényleges visszaállításnál takarít egyszer; elutasítás, hibás JSON és tárhelyhiba megőrzi a route-okat. A teszt valódi jóváhagyó párbeszédet és rollbacket ellenőriz. Az új commit GitHub-gate-jét ellenőrizni kell, korábbi zöld forrás nem elég.

**Felhasználói munkamód:** amit lehet, GitHubon végezzünk; teljes teszt és aláírt build Actionsön, helyben csak célzott javítás/gyors ellenőrzés. Nincs verzióemelés: **1.3.4 /2730**. Következő: friss CI és aláírt telefonos teszt-APK, majd a #163 8/8 fizikai GPS-mátrixa. Online térképcsempe-provider még nincs konfigurálva Androidon; az offline helyi route működésétől külön termékdöntés szükséges. Main/release csak az új funkció külön elfogadásával. Az alábbi #201/#199 állapotok történetiek.

## Korábbi folyamatban lévő javítás — 2026-10-14, #201

Munkaforrás: `feat/191-wear-save-sync-status`, draft PR #199. A #191 mentés/szinkron és #200 pihenőkihagyás javításai után a felhasználó jelentette: **kifejezett telefonos edzéselhagyás / félbehagyás** nem szünetelteti az órát. Nem képernyőlezárásról van szó. Ezt javítjuk a [#201 leírás](wear/P1_201_EDZESSZUNET.md) szerint. A GitHub build igazolt eredménye és az új párosított APK forrása a buildjegyzékbe kerül befejezés után. A korábban átadott c503887 pár még nem tartalmazza #201-et.

A natív aktív edzés képernyőlezárás mellett továbbra is működjön; csak a kifejezett elhagyás zárja az aktív időablakot és szünetelteti a visszaszámlálót / órás Health Services-t. Folytatás megőrzi az edzést és hátralévő időt. Új készülékes elfogadásig PR draft, #191/#200/#201 nyitott, main és 1.3.4 /2730 release változatlan. Meglévő #197 P2 státuszt meg kell őrizni.

# TrainPilot — folytatási leírás új ChatGPT-beszélgetéshez

## Aktuális folytatás — #191 / #200 tesztpár

Az elfogadott main alkalmazáskód `20fe5ce`; a main aktuális feje `a377717` (#198 csak a #197 P2 igényt dokumentálta). #158/#159 lezárva. Folyamatban a [#199 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/199), `feat/191-wear-save-sync-status` ágon. #191 helyi mentés / átadás / tartós telefonos nyugta és kompakt újrapróbálás kész. A felhasználó élő edzésben jelzett #200 hibája is javítva: telefonon kihagyott pihenő után az óra Folytatásban és előző kész gyakorlaton maradhatott; a befejezett pihenő után a következő sorozat/gyakorlat rögzíthető.

**Utolsó átadott telefonos és Wear APK forrása egyaránt `c503887`, 1.3.4 /2730.** Teljes 140 Node / 93 Chromium, 264 natív és 86 kerek Wear UI-teszt, aláírás/forrás/hash ellenőrzés sikeres. [Átadási lap és rövid fizikai próba](wear/P1_191_MENTES_SZINKRON.md). Main/release változatlan; #191/#200 nyitva, készülékes elfogadásra várunk. Következő fejlesztés a #163 telefonos térkép, a #191/#200 konkrét tesztpárjának visszajelzése után. A régebbi APK-k és alábbi állapotok történeti adatok; a folytatáskor ezt a felső állapotot és az aktuális GitHub headeket ellenőrizd.


## Elfogadott #158, meglévő #159 és következő P1 — 2026. október 14.

A felhasználó 2026. október 14-én a `c800b4e` telefonos tesztbuild #158 működését elfogadta: **„#158 rendben, mehetünk tovább, de elvileg a #159 is készen van már.”** A visszajelzésből nem következtetünk külön 45 perces próba elvégzésére. Verzió marad **1.3.4 /2730**, Wear `9b94a10` változatlan.

A #159 alapfunkciója már a mainben megvalósított: `b35a567` (korlátos offline Mini Napló) és `d7a6e2e` (mérési előzmények), későbbi kerek kijelzős szövegtördeléssel és a #158 stabil késői frissítésével. A legutóbbi 8 kanonikus telefonos edzés, kész sorozatok/súly/ismétlés, elérhető oldalankénti idő/táv és külön Wear/telefonos mérés olvasható; nincs órás naplószerkesztés vagy második edzéstörténet. Az időtartam és mérési forrás a részletekben látszik. A jelen kör nem építi meg újra ezt a funkciót.

Ellenőrzés: `tests/wear-journal-resync.cjs` most is sikeres (újraszinkron, cache-vesztés, sikertelen küldés/újrapróbálás, külön 4/2 kcal, UTF-8 keret); meglévő 8 `WatchMiniJournalTest` és a hosszú nevek/tördelés Wear UI-próbája. [Main Wear release](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38553667882) és [66 kerek UI-teszt](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38553668063) sikeres. A Wear teljes fája byte szerint megegyezik az elfogadott `9b94a10` forrásával. A #159-re ebben a körben nem állítunk új, külön készülékes próbát.

**Következő valódi P1 feladat: #191 → #163.** A #191 a helyi mentést, Data Layer-átadást és a telefon által ténylegesen visszaigazolt mentést külön mutatja. A meglévő lezárási visszaigazolást és parancsbizonylatokat kell felhasználni, párhuzamos mentési rendszer nélkül. A #158 elfogadása és a #159 megvalósított állapota alapján a következő kör a #191 felmérése/fejlesztése; verzióemelés vagy új release nincs automatikusan engedélyezve.

## Legfrissebb elfogadott állapot — #195

A felhasználó 2026. október 14-én a telefonos `b9dbf7b` / órás `9b94a10` tesztpárt készülékesen elfogadta: **„Minden szuper, menjen mainre, zarjuk az issuet :)”**. A #195 main-egyesítése és a kapcsolódó #162/#157 lezárása jóváhagyva. Verzió mindkét appban **1.3.4 /2730**.

Folytatás a #195-öt tartalmazó friss `origin/main`-ből, új feladatágon. Aktuális forrás és szabályok: [Work-útmutató](WORK_FEJLESZTESI_UTMUTATO.md); pontos APK/hash/CI: [P0 átadási lap](P0_KESZULEKES_ELFOGADAS_162_157.md). Következő prioritás #158/#159/#191, majd #163 térkép. Az alábbi régebbi tesztpár és jegyzetek történeti adatok.


Frissítve: **2026. október 13.** Ez a fájl bemásolható egy új beszélgetésbe. A funkciók részletes, továbbvezetendő terve a [WEAR_OS_FEJLESZTESI_TERV.md](wear/WEAR_OS_FEJLESZTESI_TERV.md); az aktuális állapot a [DEVELOPMENT_STATUS.md](../DEVELOPMENT_STATUS.md). A folytatáskor először a GitHub aktuális mainjét és a nyitott issue-kat kell ellenőrizni, mert később változhatnak.



## Korábbi, átadott tesztpár — main #179 + #181

A közös kód mainre egyesítve: `3411f5506fa7e9d6197d6f891f2709bcb26e8f5f`. A két APK forrása ennek része; a telefonos forrás megegyezik a mainbe egyesített telefonnal, a Wear-forrás a #181-ben változatlan. A verzió **1.3.4 /2730**, a korábbi kiadások aláírásával frissíthetők.

| Alkalmazás | Forráscommit | APK SHA-256 | Sikeres build |
| --- | --- | --- | --- |
| Telefon: automata, pihenőfelirat és mentésjavítás | `612f8cb7da7d342fbd7b4670ef2b6225045f9cc6` | `3ed875fbe9926df1b76d87c5e2f70310f5bd3d28bdbf002db972f9aa6b8a2774` | [38358320014](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38358320014) |
| Wear: revamp, automata és arányos pihenőkör | `c623af74291fdf2ba9c59e505e623ce1b0c42d65` | `76cd485698844ec07db0f3a88662404a7fa100d05e6f6ad67833544e2334e4f9` | [38357730174](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38357730174) |

Ellenőrzött: **138/138 telefonos regresszió**, **90 Chromium-tesztcsomag**, natív release gate és **51/51 Wear-emulátoros teszt** ([38357730023](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38357730023)). Az APK-aláírás, ellenőrzőösszeg, forrásazonosító, becsomagolt telefonos webes fájlok és zoomtiltás külön ellenőrizve. A [buildbizonylat](builds/2026-10-13-main.json) tartalmazza az azonosítókat. A fizikai automata/rezgés próbája még ellenőrizendő. A mentést a felhasználó 2026. október 13-án elfogadta („rendben van a mentés is”), #180 lezárva. A meglévő release APK-k/tagek változatlanok.

## Mentési javítás — #180

A #179 mainre került (`c623af7`), a #164 és #178 is egyesítve. A [#180](https://github.com/fuloplevente1998/TrainPilot/issues/180) új felhasználói visszajelzése: aktív edzés nélkül a JSON-export „Érvénytelen vagy túl nagy mentés”, a ZIP „hiányzó fotó” hibát ad; a fotók a telefonon megnyithatók.

A javítás kompakt, veszteségmentes JSON-t exportál, és a 20 MiB korlátot UTF-8 bájtban ellenőrzi. A ZIP az élő fotóhivatkozásokat előzetesen ellenőrzi, a hiányzó/üres fájlt a meglévő Google-fiókból próbálja visszatölteni, majd újra ellenőrzi. A hiányzó, üres és 6 MiB-nál nagyobb eredeti képfájlt külön, négy nyelven és az edzés azonosításával jelzi; a nagy kép előnézete megnyitható lehet. A képek és egészségadatok nem maradhatnak ki csendben. A 20 MiB JSON / 6 MiB kép / 500 MiB archívum korlát és a korábbi visszaállítási formátum megmarad.

**Készülékes mentés elfogadva 2026. október 13-án:** a felhasználó visszajelzése: „rendben van a mentés is”. A #180 mentési hibajegy lezárható; a következő feladat az automata működés készülékes elfogadása és továbbfejlesztése, majd a #163 térkép. Külön kézi visszaállítási próbáról nem érkezett visszajelzés.

A Workban használt eszközök, ágkezelés, buildfolyamat és Samsung/Coach kutatás egy fájlban: [WORK_FEJLESZTESI_UTMUTATO.md](WORK_FEJLESZTESI_UTMUTATO.md).

## Projekt és kiinduló alap

- Repository: **https://github.com/fuloplevente1998/TrainPilot**.
- Androidos edzéstervező és napló: telefonon Capacitor/WebView + Java; órán natív Kotlin/Compose Wear OS, minimum API 30. Az órás app nem Chromium-alapú.
- Verzió: **1.3.4 / versionCode 2730** mindkét alkalmazásban. Több tesztbuildnek ugyanaz a verziószáma; az APK-t a forráscommit és SHA-256 azonosítja.
- A felhasználó 2026. október 13-án kérte a main, GitHub-leírások és következő fejlesztések aktualizálását. A **#179** egyesíti a **#164** telefonos automata módját, a **#178** Wear-kezdőlapját és az órás automata integrációt. A további fejlesztés a friss **mainből** induljon; a régi két ágon ne készüljön külön, eltérő folytatás.
- A mainre emelés jóváhagyása mellett az új automata mód **fizikai készülékes elfogadása még hátravan**. Az eredeti `v1.3.4` és `wear-v1.3.4` release APK-k a korábbi kiadott állapotot őrzik; a jelen mainfrissítés nem cseréli le ezeket.
- A felhasználó magyarul kommunikál, telefonon és valódi órán tesztel. Korábbi képein a kerek kijelzőn levágott gombok, keskeny középső tartalom, hibás szerkesztő és háttérfrissítéskor összecsukódó telefonos nézetek szerepeltek; ezek javításait meg kell őrizni.

## Korábban átadott, ellenőrzött referencia-tesztpár

Mindkét APK szükséges, majd a telefonos app megnyitásával első telefon–óra adatszinkron. Ez adja át az új automata protokollt; utána az órán indított edzés helyben is időzíthet.

| Alkalmazás | Forráscommit | APK SHA-256 | Sikeres build |
| --- | --- | --- | --- |
| Telefon | `16509065474cf76ee1222985e9155b6608f479f9` | `9ea25bb103d0aac6d862824cb18caa0082b69df735f3260512bfec4945178041` | [38352741468](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352741468) |
| Wear | `e3db5e20015bce137733763c50c0bf0c521ddf13` | `3f50d3bb36f0c0b31d731da6e3322e7cc3445308442860a7a630b79ba00db818` | [38352812467](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812467) |

A telefonos forrás a két commit között változatlan. Alkalmazásazonosító: `com.repforge.app`. A két APK a korábbi kiadások tanúsítványával aláírt, SHA-256 ujjlenyomata: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.

Ellenőrzött: 137 telefonos regresszió, külön futtatótesztek, 90 Chromium-tesztcsomag, 16 új Kotlin állapot-/tükrözési teszt, natív kiadási kapuk és **51 Wear-emulátoros teszt** ([38352812492](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812492)). Utóbbi 160/192/225 dp kerek kijelzőt, normál/1,5×/2× betűméretet és háttérbe tett Activity mellett valódi időzítő-szünet/folytatást ellenőriz. A fizikai rezgést és OEM háttérkezelést ez nem igazolja.

A fenti buildek a most jelzett pihenőhibák javítása előtti, korábban átadott tesztpár. Az aktuális javítást tartalmazó main buildek az Actionsön azonosíthatók; későbbi beszélgetésben ellenőrizd a forrást és a sikeres eredményt. Az Actions-artefaktok megőrzése véges (jelenleg 14 nap), lejárat után az adott forrásból új aláírt build készíthető. Egy korábbi beszélgetés `/workspace` fájljaira ne építs.

## Elkészült működés és rögzített felhasználói döntések

1. **Súlyzós/ismétléses edzés:** a felhasználó rögzíti a valódi sorozatot. Utána automatikus pihenő, majd következő befejezetlen sorozat/gyakorlat. A pihenő lejárta nem írhat kitalált ismétlést vagy súlyt.
2. **Időalapú gyakorlat:** munka/pihenő/körök automatikusan követik egymást; a kezdeti munkaszakasz explicit indítású. Órán pontosan `mp` egységnél automatikus; oldalankénti mérés továbbra is kézi. Végső edzésmentés külön művelet.
3. **Rezgés:** az utolsó 3 másodpercben másodpercenként egy **80 ms-os rövid impulzus**, lejáratkor **egy 1 másodperces, egybefüggő rezgés**. Szünet/leállítás megszakítja; ismételt frissítés nem ismételheti meg a jelzést.
4. **Telefonos felület:** az aktív visszaszámláló kézi és automata módban alul, az **Előző és Következő gomb között** marad. Az automata kapcsoló kicsi/másodlagos. A natív WebView lapzoom tiltott, a rendszerbetűméret és a reszponzív elrendezés megmarad.
5. **Wear-kezdőlap:** telefonos ikonok és témaszínek/keretek, hangsúlyos `HH:mm:ss` idő, aktuális/ajánlott edzés, Napló/Beállítások/Menu/telefon-megnyitás. A Beállítások saját oldalra vezet. Az aktív edzés közvetlen Törlés gombja megerősítést kér. A menü **lentről felfelé húzva** is nyílik; rövid kezdőlapi tartalom nem görgethető feleslegesen.
6. **Wear-edzés:** nagyobb/olvasható gyakorlatnév, ahol indokolt íves fejléc; kis `HH:mm` pontos idő, kompakt A/M kapcsoló; munka/pihenő/kör és rezgés az edzésmenüben. Kis körkijelzőn is elérhető alsó vezérlők. A súly/ismétlés szerkesztőben a mínusz/plusz a szám két oldalán, a Mentés/Mégse elérhető. Hosszú Mini napló-szöveg tördelődik.
7. **Állapotvédelem:** mentés/szinkron/frissítés nem csukhatja össze a nyitott telefonos nézetet, nem veszítheti el a piszkozatot vagy a témát. Régi parancs nem módosíthat másik sorozatot/edzést; sorozatváltás közben nyitott szerkesztő nem menthet az új célba.
8. **Egy időzítőtulajdonos:** órás indításnál `watch`, telefonosnál `phone`. A másik eszköz vezérlő és kijelző; nem futhat kettős automatikus léptetés/rezgés. Kapcsolatvesztéskor a tulajdonos folytat, megszakadt szolgáltatásból mentett szünet áll vissza, kitalált utólagos sorozat nélkül.

## A most jelzett pihenőhibák javítása — #179

- A telefonos kompakt időzítő közös dekorátora elrejtette a fázisfeliratot. Az automata időzítő saját, kompakt rácselrendezésben tartja láthatóan a **Munka/Pihenő** feliratot, az alsó Előző/Következő gomb között. Órás tulajdonnál a telefonos státusz is az aktuális fázist mutatja.
- Az órás pihenőkör az edzés alap pihenőértékéből számolt, amely eltérhetett az aktuális automata szakaszétól. Most a **hátralévő / teljes szakaszidő** arányát használja: 120 mp pihenőnél 119 mp = 119/120 kör, 60 mp = fél kör. Szünetben ugyanott marad.
- A telefon a kezdeti gyakorlatközi pihenő valódi hosszát is átadja, amely eltérhet a következő intervallum pihenőidejétől. A kijelzésekhez ugyanaz az aktuális szakaszidő kerül.
- Az ellenőrzés kiegészült ténylegesen látható telefonos fázisfelirattal 16 aktív méret/nyelv/téma kombinációban, 4 új órás arányteszttel, 2 új natív telefonos szakaszidő-teszttel és a 120 mp-es pihenő emulátoros megjelenítésével. A sikeres CI-futások a #179-en és a main Actionsön követhetők.

## Ismert nyitott korlátok — ezt kell először folytatni

- **Telefonon indított edzésnél** az órás ismétlésrögzítés és egy új telefonos menetrend óráról indítása még a telefonos webfelület felébresztésére várhat. Az óra „Telefonra vár” állapotot mutat. A natív telefonos számláló tükrözése és szünet/folytatás/kihagyás már működik.
- Órán indított automata edzés natív, önálló háttéridőzítése elkészült. A fizikai telefon–óra kapcsolat, lezárt kijelző, rezgés és akkumulátorkezelés próbája még szükséges.
- Órás automata időmérés: tényleges munka másodpercei. A meglévő telefonos intervallum: korábbi munka+pihenő összesítés. A **munka/pihenő/kézi szünet naplóbontását egységesíteni kell**, régi naplóadat találgatással történő átírása nélkül.
- Oldalankénti automata időmenetrend és órás GPS-szakaszkezelés további bővítés. Az aktuális GPS-híd összesített időt/távolságot ad át, **útvonalpontokat még nem tárol**.

## Következő fejlesztések sorrendje

1. **#162:** a legutóbbi tesztpár fizikai próbája mindhárom használattal (telefon, önálló óra, párosított); utána a fenti telefonos háttérrögzítés/új idővonal natív feldolgozása, stabil azonosítókkal, újraküldés-/újraindítás-/draft-védelemmel. Az issue addig nyitott.
2. Munka/pihenő/szünet naplózásának egységesítése és GPS/intervallum határok; nincs kettős mérés vagy másik sorozatba kerülő adat.
3. **#163 térkép:** az automata működés elfogadása után, elsőként telefonos GPS-útvonalrögzítés és naplóbeli térkép. Pontatlanság/jelkimaradás/szünet ne rajzoljon kitalált összekötő szakaszt; offline nézet, mentés/visszaállítás/törlés és forráshű adatok szükségesek. A régi vagy kézi távolságból nem készül kitalált útvonal. Órás útvonal későbbi hatókör.
4. **Wear kényelmi terv:** utolsó rögzítés visszavonása az automata átmenettel együtt; érintészár; lünetta/korona a szerkesztőben; világos „Órán mentve” / „Szinkronra vár” visszajelzés. A #170 első kezdőlap-revampja már megvalósított, finomítás készülékes visszajelzés alapján. E bővítések nem előfeltételei a telefonos térképnek.


Részletek és további döntések **egy kanonikus fájlban**: [docs/wear/WEAR_OS_FEJLESZTESI_TERV.md](wear/WEAR_OS_FEJLESZTESI_TERV.md). Ezt frissítsd a következő megvalósításkor.

## Fontos fájlok és ellenőrzések

- Telefonos UI: `www/app.js`, automata részek: `automatic-workout-162.js`, `wear-automatic-162.js`, intervallumok: `intervals-162.js`. A szakaszcímeket `rg`-vel keresd; a fájl marad a kanonikus forrás.
- Telefon natív: `android/app/src/main/java/com/repforge/app/IntervalTimerService.java`, `WearSyncPlugin.java`, `WearCommandListenerService.java`, `WearCommandQueueStore.java`, `ActiveWorkoutStore.java`.
- Wear natív: `android/wear/src/main/java/com/repforge/app/wear/WearAutomatic.kt`, `WearAutomaticService.kt`, `WearPhoneAutomatic.kt`, `WearPhoneAutomaticProjection.kt`, `MainActivity.kt`, `WearScreens.kt`, `WorkoutSnapshotStore.kt`, `WatchHomeStore.kt`, `WearCommandOutbox.kt`.
- Automata adatút: `/trainpilot/watch-automatic`, `/trainpilot/phone-automatic`, `/trainpilot/automatic-command/<commandId>`; kezdeti kompatibilitás: `automaticProtocol=1` a home snapshotban.
- `npm ci`, `npm test`, `npm run test:ui`; a teljes böngészős lista `tests/browser/suite.json`. Natív szinkron: `npm run sync`.
- GitHub Actions: `build-apk.yml`, `wear-release.yml`, `wear-layout-ui.yml`, `validate-branch.yml`, `phase7-performance.yml`. Közös protokollváltozás mindkét natív gate-et igényli.
- Tesztek: `tests/browser/wear-automatic-162.cjs`, Wear `WearAutomaticTest`, `WearPhoneAutomaticProjectionTest`, `WearFullScreenTest`, `WearAutomaticServiceTest`; a meglévő regressziókat is meg kell tartani.
- Aláírt APK-t, source commitot, package/verziót, SHA-256-ot és tanúsítvány-egyezést ellenőrizni kell. Credential/signing titok nem kerülhet Gitbe. Pontos buildfolyamat: [BUILD.md](BUILD.md).

## Megőrzendő adatszabályok

Napló, saját program, fotó, piszkozat, téma és meglévő egészségadat nem veszhet el. TrainPilot Wear / Health Connect / Samsung Health külön források; kalóriaadataik nem adódnak össze. Mért nulla megmarad, hiányzó érték nem becsülhető. A késői mérés ugyanahhoz az edzésazonosítóhoz kapcsolódik, új naplóbejegyzést nem hoz létre. Stabil azonosító és törlésjelző védi a mentést, szinkront és visszaállítást.

## Rövid indító szöveg bemásoláshoz

> Folytassuk a TrainPilot fejlesztését: https://github.com/fuloplevente1998/TrainPilot. A friss mainből dolgozz; előbb olvasd a docs/CHATGPT_FOLYTATAS.md, DEVELOPMENT_STATUS.md és docs/wear/WEAR_OS_FEJLESZTESI_TERV.md fájlokat, valamint a #162, #163 és #170 issue aktuális állapotát. A #179 egyesíti a telefonos/órás automata módot és a Wear-kezdőlapot. Aktuális átadott tesztpár: telefon 612f8cb, Wear c623af7, mindkettő 1.3.4 /2730; #180 mentés elfogadva, fizikai automata elfogadás még hátravan. A docs/WORK_FEJLESZTESI_UTMUTATO.md rögzíti a munkamenetet és a #184 Samsung energiapontszám/Coach tervet; ez az új integráció még nincs implementálva. Először az óráról történő telefonos háttérrögzítést és a munka/pihenő naplóbontását folytassuk, utána a #163 térképet. Súlyzós sorozatot én rögzítek; utána automata pihenő/léptetés. A számláló telefonon az alsó két gomb között, az automata kapcsoló kicsi marad. Rezgés: 3–2–1 rövid + lejáratkor 1 másodperc egybefüggően. Az új döntéseket a közös tervbe is vezesd át.
