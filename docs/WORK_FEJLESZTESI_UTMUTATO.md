# TrainPilot — fejlesztés folytatása Workban

Frissítve: **2026. október 13.** Ez az egy fájl új Work-beszélgetésbe feltölthető vagy bemásolható. A Samsung energiapontszám kutatása és a javasolt Coach-integráció is itt szerepel.

## 1. Honnan dolgozunk?

- Repository: **https://github.com/fuloplevente1998/TrainPilot**.
- Kiinduló ág: mindig a GitHub **aktuális mainje**, külön feladatágon folytatva. A dokumentum készítése előtti main: `f88d9c10682d0a80bedc44e7a29ebdd929ab2873`; ez azonosítás, nem utasítás egy később elavuló commithoz való visszatérésre.
- A #179 egyesítette a telefonos/órás automata módot és a Wear-kezdőlapot. A #181 mentésjavítása is mainen van. A #180 mentést a felhasználó elfogadta, a hibajegy lezárva.
- Verzió mindkét appban: **1.3.4 /2730**. Több teszt-APK használja ezt a verziót; a pontos forráscommit és SHA-256 azonosítja a csomagot.
- Az eredeti `v1.3.4` és `wear-v1.3.4` release binárisai korábbi kiadott forrást őriznek. Folytatáshoz a main és az alábbi tesztpár az alap.
- Egy régi chat `/workspace` könyvtára és letöltési linkje nem garantáltan érhető el az új környezetben. A GitHubon tárolt forrás, dokumentáció és buildazonosító a tartós alap.

Elsőként olvasandó:

1. [CHATGPT_FOLYTATAS.md](CHATGPT_FOLYTATAS.md): döntések, aktuális állapot és ismert korlátok.
2. [DEVELOPMENT_STATUS.md](../DEVELOPMENT_STATUS.md): megvalósított és következő feladatok.
3. [WEAR_OS_FEJLESZTESI_TERV.md](wear/WEAR_OS_FEJLESZTESI_TERV.md): a telefonos/órás automata és Wear UI közös, továbbvezetendő terve.
4. [BUILD.md](BUILD.md), [SAMSUNG_HEALTH.md](SAMSUNG_HEALTH.md), az érintett issue-k és a környezetben található `AGENTS.md` utasítások.

## 2. Folytatható sima Workban?

**Igen, a megfelelő eszközökkel.** A Work felület neve önmagában nem bizonyítja, hogy az adott beszélgetéshez kódfuttató környezet is tartozik.

| Feladat | Szükséges hozzáférés |
| --- | --- |
| Terv, kutatás, dokumentáció, issue/PR kezelése | GitHub-kapcsolat; kutatáshoz webes hozzáférés |
| Kódszerkesztés és helyi teszt | Repó-klón, fájlszerkesztés és terminál; Node/npm, szükség szerint JDK és Android SDK |
| Aláírt, telepíthető APK | A repó meglévő GitHub Actions buildjei és a jogosult CI signing Secrets |
| Valódi óra/telefon, rezgés, Samsung-adat ellenőrzése | A felhasználó fizikai készülékes próbája |

Ha csak GitHub-eszközök vannak, a dokumentáció és PR előkészíthető, és a meglévő Actionsből kérhető build megfelelő jogosultsággal. Helyi tesztet ekkor nem szabad lefutottnak állítani. APK-hoz a meglévő CI használható akkor is, ha nincs lokális Android SDK vagy aláírókulcs.

## 3. Munkamenet

1. Ellenőrizd a felhasználó legutóbbi kérését, az aktuális main/issue/PR állapotot és az esetleges helyi módosításokat. A régi dokumentum nem írja felül az új felhasználói döntést.
2. Új munkaterületen klónozd a repót, vagy frissítsd a meglévőt. Más ember módosítását ne töröld és ne keverd automatikusan a saját feladatodba.
3. A friss mainből készíts `feat/...`, `fix/...` vagy dokumentációhoz `docs/...` ágat. A régi #164/#178 munkafákból ne készüljön külön eltérő folytatás.
4. Nézd át az érintett adatút és a meglevő tesztek működését. A `www/app.js` szakaszcímeit `rg`-vel keresd; a legutolsó függvényfelülírás számít, nem feltétlenül az első találat.
5. Készíts célzott változtatást. A meglévő napló, piszkozat, téma, szinkron és mérés viselkedését őrizd meg.
6. Futtasd a változáshoz szükséges ellenőrzéseket, majd push és review-zható PR. A PR írja le az új működést, a bizonyítékot és a fennmaradó korlátot.
7. Az aktuális felhasználói felhatalmazás szerint emeld mainre és kezeld a kiadást. A már megadott engedélyt ne kérd újra; egy korábbi konkrét kiadás elfogadását ne állítsd egy későbbi, még nem tesztelt funkció elfogadásának.
8. A tesztbuild mellett add át a forráscommitot, SHA-256-ot, verziót és CI-linket. A fizikai próba visszajelzését rögzítsd; a közös tervet és folytatási leírást aktualizáld.

Indítás tiszta munkaterületen, példa:

```bash
git clone https://github.com/fuloplevente1998/TrainPilot.git
cd TrainPilot
git fetch origin
git switch -c feat/feladat-neve origin/main
npm ci
```

Terminál nélkül ne állítsd, hogy e parancsok lefutottak. A GitHub-kapcsolat írási és workflow-jogosultságát az elérhető eszközökkel ellenőrizd; titkot ne másolj chatbe vagy Gitbe.

## 4. Források és ellenőrzés

| Terület | Kanonikus hely |
| --- | --- |
| Telefonos UI | `www/app.js`, `www/features-110.css`, `www/index.html`, `www/sw.js` |
| Telefonos natív kód | `android/app/src/main/java/com/repforge/app/` — Java/Capacitor |
| Wear app | `android/wear/src/main/java/com/repforge/app/wear/` — natív Kotlin/Compose |
| Telefonos/órás automata | JS `automatic-workout-162.js`, `intervals-162.js`, `wear-automatic-162.js` szakaszok; natív `IntervalTimerService`, `WearAutomaticService`, állapottárolók és parancssorok |
| Samsung és egészségnapló | `SamsungHealthPlugin.java`, `HealthJournalPlugin.java`, `HealthJournalStore.java`, `docs/SAMSUNG_HEALTH.md` |
| Coach pontozás | A `www/app.js` aktuális `rf220Readiness`, `rf220EffortLoad`, `rf245Fresh` és regenerációs projekciói |

A telefon Capacitor/WebView alapú, az órás app natív. Az `android/app/src/main/assets/public` szinkronizált csomagtartalom; a webes javítás a `www` forrásban történik.

```bash
npm test
npm run test:ui
npm run sync
cd android
./gradlew --no-daemon :app:testReleaseUnitTest
./gradlew --no-daemon :wear:testReleaseUnitTest
```

A natív parancsokhoz megfelelő JDK és Android SDK kell. A CI Node 22 / Java 21 környezetet használ; a pontos Android/Gradle verziókat a repó konfigurációja határozza meg. A Samsung SDK saját minimumai nem jelentik a teljes telefonos app minimumának emelését.

- Telefonos teljes gate és aláírt APK: `.github/workflows/build-apk.yml`.
- Wear aláírt APK és natív gate: `.github/workflows/wear-release.yml`.
- Kerek órás kijelző és érintéses próbák: `.github/workflows/wear-layout-ui.yml`.
- Gyors regresszió és teljesítmény: `validate-branch.yml`, `phase7-performance.yml`.
- Közös protokollváltozásnál mindkét eszköz ellenőrzése szükséges. Pusztán dokumentációs módosítás miatt nem kell új alkalmazásteszteket írni.
- Az aláírt APK signing-titkait a CI kezeli. A release tanúsítvány folytonosságát és a becsomagolt forrást ellenőrizd; tesztbuildhez ne cseréld le az aláírókulcsot.

## 5. Jelenlegi tesztpár és megőrzendő működés

| App | Forrás | Sikeres Actions-futás |
| --- | --- | --- |
| Telefon | `612f8cb7da7d342fbd7b4670ef2b6225045f9cc6` | [38358320014](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38358320014) |
| Wear | `c623af74291fdf2ba9c59e505e623ce1b0c42d65` | [38357730174](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38357730174) |

SHA-256, tanúsítvány és teljes bizonylat: [2026-10-13-main.json](builds/2026-10-13-main.json). Ellenőrzött: 138 regresszió, 90 Chromium-tesztcsomag, natív gate-ek, 51 Wear-emulátoros teszt. Actions-artefaktok jelenleg 14 napig élnek; lejárat után az azonosított forrásból új build készíthető.

Megőrzendő felhasználói döntések:

- Súlyzós sorozatot a felhasználó rögzít. Utána automata pihenő és léptetés; nincs kitalált ismétlés/súly. Időalapú sorozatok munka/pihenő/kör szerint működnek, végső mentés külön művelet.
- Telefonon az aktív számláló alul, az Előző/Következő gomb között marad, a kis automata kapcsolóval. A Munka/Pihenő felirat látható.
- Rezgés: utolsó 3 másodpercben 80 ms-os jel másodpercenként, lejáratkor 1 s egybefüggően. Nincs kettős jelzés vagy időzítő.
- Az órás pihenőkör a hátralévő/teljes szakaszidő arányát mutatja: 120 mp-nél az első másodperctől fogy.
- Wear-kezdőlap: telefonos ikonok/téma, másodperces óra, külön Beállítások, közvetlen törlés megerősítéssel és felfelé húzható menü. Edzés közben kis pontos idő; olvasható címek és elérhető szerkesztőgombok kis kerek kijelzőn is.
- Háttérfrissítés/szinkron nem csukja össze a nyitott nézetet, nem veszíti el a piszkozatot/témát. Telefonos zoomtiltás mellett a rendszerbetűméret megmarad.
- Hiányzó egészségadat nem nulla, a források kalóriája nem adódik össze. A Health-mentés hozzájárulása, fotóhivatkozások, stabil azonosítók és törlésjelzők megmaradnak.

Következő funkciók: #162 telefonos háttérvezérlés és időnapló-egységesítés; utána #163 GPS-útvonaltérkép. A fizikai automata/rezgés elfogadás még nyitott. Az energiapontszám új, külön tervezési feladat: [#184](https://github.com/fuloplevente1998/TrainPilot/issues/184).

## 6. Samsung Health energiapontszám — kutatási eredmény

**Állapot: kutatás és javaslat, az energiapontszám beolvasása és az új Coach-logika még nincs implementálva.**

A Samsung hivatalos leírása az alábbi tényezőket nevezi meg: átlagos alvásidő, az alvásidő következetessége, a lefekvés/ébredés következetessége, az alvás időzítése, előző napi aktivitás, alvás alatti pulzus és HRV. Ez napi általános állapotértékelés. A megvizsgált nyilvános dokumentáció nem közöl olyan teljes képletet és súlyokat, amellyel a Samsung 83/100 pontosan újraszámolható lenne. A tényezőlista egy 2024-es hivatalos ismertetésből származik; az aktuális Samsung Health felület további tényezőket is mutathat. [Samsung tényezőlista](https://news.samsung.com/us/samsung-galaxy-ai-coming-new-galaxy-watch-more-motivational-health/).

A tényleges pontszámot viszont a **Samsung Health Data SDK `DataTypes.ENERGY_SCORE`** olvasási típusa kínálja, külön olvasási engedéllyel. A rekord napi időpontot, időzónaeltolást, azonosítót és forrást is tartalmaz. Az API a pontszámot adja; részletes faktoronkénti pontozás exportját az ellenőrzött típus nem dokumentálja. [EnergyScoreType API](https://developer.samsung.com/health/data/api-reference/-shd/com.samsung.android.sdk.health.data.request/-data-type/-energy-score-type/index.html).

A repóban már szerepel a Samsung Health Data SDK **1.1.0**, a `SamsungHealthPlugin.java` használja. A beépített AAR `DataTypes` és `DataType.EnergyScoreType` osztályaiban az `ENERGY_SCORE` mező jelenlétét ellenőriztük. A plugin jelenlegi engedélylistájába és adatolvasásába ez még nincs bekötve. A [Health Connect típustáblája](https://developer.android.com/health-and-fitness/health-connect/data-types) nem kínál Samsung EnergyScore rekordot; ehhez a meglévő közvetlen Samsung-adatút bővítése a konkrét lehetőség.

Valós elérhetőség: külön engedély, az aznapi Samsung-adat megléte és támogatott készülék szükséges. Az SDK Samsung Health 6.30.2+, Android 10+ és Java 17+ környezetet ír elő, emulátoron nem tesztelhető a szolgáltatás. [SDK feltételek](https://developer.samsung.com/health/data/guide/introduction.html). Nyilvános SDK-integrációhoz Samsung partneri jóváhagyás és a csomagnév/aláírás regisztrációja kell. A repó nem igazol megszerzett partneri jóváhagyást. Fejlesztői teszt-hozzáférés nem egyenlő a felhasználóknál működő éles hozzáféréssel. [Appellenőrzés](https://developer.samsung.com/health/data/guide/app-verification.html).

## 7. Miért más a Coach pontszáma?

A jelenlegi kód saját, szabályalapú becslés, 70-es indulóértékkel. A legutolsó aktív pontozás a `www/app.js` `v245.js` szakaszában van; a későbbi `tp1511ReadinessBase` burkoló a bizalmi szöveget lokalizálja, a számítást nem cseréli le.

| Jel | Jelenlegi hatás |
| --- | --- |
| Friss alvás | ≥7,5 h: +12; ≥7 h: +8; ≥6 h: +1; ennél rövidebb: −14 |
| HRV saját alaphoz képest | ≥+15%: +10; ≤−20%: −14; ≤−10%: −6; egyébként +3; csak pozitív HRV és alap esetén |
| Naplózott nehézség | A 14 napon belüli legutóbbi három edzés visszajelzéseiből terhelési jel; hozzájárulás `(terhelésiJel − 70) × 0,35` |
| Friss fájdalomjelzés | További −12 és az érintett gyakorlat progressziójának korlátozása |
| Nincs használható friss jel | Nincs pontszám; nem állítunk elő látszólag mért 70/100-at |

Alvás/HRV maximum 36 órás, jövőbeli adat kizárt. A HRV-alap az előző hét azonos forrású napi értékeinek mediánja, legalább három mintával. A használható pontszám 25–100 közé korlátozott; ≥75 jó, ≥55 közepes, alatta visszafogott nap. A közvetlen Samsung SDK jelenlegi útja nem ad HRV-t a Coach számára; meglévő Health Connect HRV lehet címkézett kiegészítés. Az energiapontszámból nem vezetünk vissza kitalált HRV-t.

Ezért az eltérő szám önmagában nem szinkronhiba: eltérő bemenetek, frissesség, megosztott adatok és szabályok. A felhasználó konkrét 83-as Samsung és aktuális Coach értékét az azonos napi, ténylegesen hozzáférhető adatokkal kell összevetni; a Coach értékét nem ismerjük ebből a visszajelzésből.

## 8. Hogyan kapcsoljuk össze? — javasolt terv

**Közös edzésajánlás:** Samsung napi regenerációs kiindulás + TrainPilot program/gyakorlat/progresszió és felhasználói visszajelzés. A Samsung pontszáma változatlan marad; a Coach magyarázza az edzésre vonatkozó döntést.

Példa, tervezett megjelenítés:

> Samsung energiapontszám: 83/100 · mai adat
>
> Coach edzésjavaslat: normál edzés; az érintett, fájdalommal jelzett gyakorlatnál nincs terhelésemelés.
>
> Indokok: Samsung napi állapot + TrainPilot edzéselőzmény és visszajelzés.

Ez felületi példa, nem az adott felhasználó állapotának értékelése. A konkrét döntési határok és módosítók még tervezendők és tesztelendők.

Megvalósítási sorrend:

1. Opcionális `ENERGY_SCORE` olvasási engedély, aznapi időzónahelyes lekérés a meglévő Samsung pluginban, foreground/journal-generáció védelemmel.
2. Érték + nap + forrás + UID + időzóna + olvasási idő tárolása a meglévő egészségnaplóban; hiány/0 külön, mentési hozzájárulások változatlanok.
3. Első tesztbuildben összehasonlító megjelenítés a jelenlegi Coach-becslés mellett, változatlan edzésajánlással. Valódi telefonon egyezés az azonos napi Samsung Health értékkel.
4. Közös ajánlás tervezése. A Samsung pontszám mellett ugyanaz az alvás, HRV vagy előző napi aktivitás nem kap még egyszer teljes súlyt. A Coach gyakorlatspecifikus progresszió/fájdalom jelét ettől elkülönítve értelmezze.
5. Engedélyhiány, lejárt/más napi adat vagy Samsung nélküli telefon esetén a jelenlegi, egyértelműen TrainPilot-becslésnek jelölt működés maradjon. A napi pontszám nem élő pulzus/kcal, a frissítés ideje látható.
6. Fizikai egyezés, éjfél/időzóna, hiányzó adat, valódi nulla, ismételt szinkron, forrásütközés, backup/restore és fájdalomkorlát regressziói; éles Samsung-hozzáférés külön igazolása.

Egyszerű 50/50 pontszámátlagot nem javaslunk: a két érték részben ugyanazokat a jeleket foglalja össze. A Samsung magas pontszáma nem írhatja felül a fájdalom miatti terheléskorlátozást. A megvalósítás előkészítése [#184](https://github.com/fuloplevente1998/TrainPilot/issues/184); a #162 automata és #163 térkép külön feladat marad.

## 9. Indító szöveg új Work-beszélgetéshez

> Folytassuk a TrainPilot fejlesztését: https://github.com/fuloplevente1998/TrainPilot. Olvasd a docs/WORK_FEJLESZTESI_UTMUTATO.md, docs/CHATGPT_FOLYTATAS.md, DEVELOPMENT_STATUS.md és docs/wear/WEAR_OS_FEJLESZTESI_TERV.md fájlokat. A friss mainből, külön feladatágon dolgozz. Jelenlegi tesztalap: telefon 612f8cb, Wear c623af7, 1.3.4 /2730; #180 mentés elfogadva. Ellenőrizd az aktuális issue/PR állapotot és az elérhető GitHub/terminál/CI eszközöket. A #162 automata és #163 térkép mellett #184 a Samsung energiapontszám és Coach közös ajánlásának terve. Az energiapontszám API kutatva, de még nincs implementálva. A teszt- és készülékes eredményeket külön dokumentáld, az új döntéseket vezesd át a közös tervbe. A konkrét következő feladatot a legutóbbi üzenetem adja meg.
