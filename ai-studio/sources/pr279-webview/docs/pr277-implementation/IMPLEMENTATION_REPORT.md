# PR277 UI/UX megvalósítás — ellenőrzési bizonylat

- Kiinduló `main`: `8a36502da5832a42f1cb91708ecfed5cf4f9ed62`, GitHub API és `git ls-remote` egyezett.
- Ág: `feat/pr277-uiux-implementation-20261029`; [Draft PR #279](https://github.com/fuloplevente1998/TrainPilot/pull/279).
- Specifikáció és a ténylegesen megnyitott nyolc kép: [PR277](https://github.com/fuloplevente1998/TrainPilot/pull/277), [részletes terv](https://github.com/fuloplevente1998/TrainPilot/blob/feat/uiux-page-content-20261029/docs/plans/uiux-2026-10-29/IMPLEMENTATION_PLAN.md).
- A PR271 és PR278 alkalmazáskódját nem vettük át. Az eredeti 2×4 főmenü, router és natív motorok megmaradtak.
- Verzió: **1.3.71 / 2735**, telefon/Wear metaadatok változatlanok. Nincs merge vagy nyilvános kiadás.

## Beépített változások

| Terület | Szerkezeti változás | Adat és megőrzött funkció |
|---|---|---|
| Programok — 08 | Ikon, név, tömör metaadatsor, külön aktiválás; kétoszlopos másodlagos eszköztár; megnyitható sablonfrissítés | Meglévő programazonosító, lazy részletek, aktiválás, létrehozás és szerkesztés. Típusikon csak ismert célból, különben általános/otthoni ikon. |
| Indítás — 02 | A csukott programnap három gyakorlatot és munkasorozatszámot mutat | Valódi prescription; a korábbi időtartam egyértelműen „Legutóbbi idő”, nem kitalált becslés. Videó, sorrend és szerkesztés megmaradt. |
| Beállítások — 01 | Hat megnyitható funkciócsoport, nyitottság megőrzésével | Profil, téma, nyelv, globális pihenő/mód, eredeti programbeállítás átjáró, Wear-szinkron, JSON/ZIP/Drive, adatvédelem/reset. A program szerkesztője nem duplikálódik a háttér és a panel között. |
| Napló/PR — 07 | Valódi összegzés csukva is, elkülönített jobb oldali vezérlőoszlop, kis PR*, Összes/PR/GPS szűrés; sticky belső fülsor | Eredeti rekordindex; eredeti lapozás és dátumszűrés; lazy részletek, szerkesztés, törlés, fotó, Health és GPS-útvonal. A PR felirat apró, a célterület 44 px. |
| Egészség — 03 | A meglévő szinkron/engedély és igazolt utolsó frissítés előre került | Nincs kitalált „Kapcsolódva”, akku vagy új adatforrásgomb. Meglévő Health Connect/Samsung kiválasztó és hibaállapotok. |
| Aktív edzés — 04 | Közös oszlopfejléc; előző érték, súly, ismétlés, kész sorozat RIR-je egy sorhoz rendelve; következő gyakorlat; kompakt Coach-részlet | Eredeti input-/RIR-mentés, draft, Undo, timer-owner, automatikus súlykitöltés és videó. 320 px-en az előző érték saját alsó sorba törik. Az eltelt idő a kanonikus aktív időablakokból, renderkor frissül; új óramotor nincs. |
| Kültér/térkép — 05/06 | Rövid útvonalelőnézet; a meglévő élő térkép teljes képernyőre nyílik, alul összegzéssel | Csak a privát natív route API koordinátái; a preview legfeljebb 5 másodpercenként kér új adatot a meglévő paint hívásból. Nincs új GPS-/időzítőpoll. Térképbezárás nem állítja le a mérést. |
| Átmenetek | A meglévő navigációs modul egy belső tartalomfelületet halványít 160 ms alatt | Reduced-motion támogatás; a fő navigáció és a Settings panel azonnali marad; nincs új render/go wrapper. |

## Képernyőképek és vizuális próba

A `tests/browser/pr277-visual.cjs` az ellenőrzött main `www` könyvtárát külön ideiglenes mappába bontja ki. Ugyanazokkal a **szintetikus CI-tesztadatokkal** futtatja az előtte/utána változatot. A tesztadatok nem felhasználói edzések, és nem kerültek az alkalmazás adatbázisába alapértelmezett tartalomként.

A képek a Phone Gate `TrainPilot-ui-<run>-shard-1` artifactjában:

- `ui-evidence/277-before-*` és `ui-evidence/277-after-*`;
- 320/360/393 px, Basic kék / Neon sárga, valamint nagyobb betűs DE és RO;
- csukott/nyitott Programok, programnapok, Settings, Napló, sticky görgetés, Egészség, aktív edzés, kültér és teljes képernyős térkép;
- `ui-results/pr277-visual.json`: baseline/source commit, konfiguráció, fájllista és hibák.

A végső `65eea4473224614d74895b3ff8f841365a6c0ca0` forráson **200 kép, 0 JS-/vízszintes túlcsordulási hiba**. A végső utána-képeket kontaktlapokon, az aktív edzést külön eredeti méretben, mind a nyolc referencia–előtte–utána összehasonlítást külön megnyitottuk. A kontaktlapok áttekintése nem azonos minden 200 eredeti kép egyenkénti nagyításával, és nem fizikai érintéses telefonpróba.

A [comparisons](comparisons) mappában nyolc JPG őrzi a PR277 referenciát, a tiszta main és a végső megvalósítás 393 px-es, azonos tesztadatú képét. A teljes 200 PNG és a gépi eredmény [CI artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058363/artifacts/12909102290). A képek látható szerkezeti különbséget mutatnak; a Health változása visszafogottabb: a valóban működő műveletek kerültek előre.

360/393 px-en a tesztképernyőn megkezdett programkártyák száma 3→4; 320 px-en 3→3. Nagy betűs német 320 px-en 2→3; román 393 px-en 3→4. Ez adott fixture és viewport eredménye, nem minden programnévre érvényes ígéret. A sticky belső fülsor görgetés után is látható. A 1500 edzéses PR-szűrés 3 history-olvasást végzett; a szűrés a lapozás előtt történik.

## Ellenőrzések és korlátok

A kód elkészült / automatizált teszt sikeres / vizuálisan ellenőrzött / telefonon elfogadott állapotokat külön kell kezelni. Készülékes elfogadás **nincs**.

A tesztekben az új Settings-csoportokat valódi kattintás nyitja meg. A korábbi működési elvárások megmaradtak. Kifejezetten felülírt régi vizuális szerződés: a PR többé nem egyenlő szélességű harmadik metrikagomb; az összegzés csukva is látható; a nyelv a Megjelenés csoportban található. A zárt Napló-sor kompakt magasságának korlátját megtartottuk.

Nyitott termékkorlátok:

1. A main nem kínál támogatott telefonos „programnap indítása az órán” API-t. A telefon indítása változatlan; a Wear önálló programnapválasztása és meglévő szinkronja marad. Nincs dekoratív vagy második indítási implementáció.
2. Új időbecslő modellt nem vezettünk be. Ismert korábbi időtartam megjelenik, egyébként nincs percérték.
3. A main meglévő Basic/Neon témái sötét felületűek; külön világos módot ez a munka nem vezetett be. A témaszínek megmaradtak. Valódi világos mód teljesülését nem állítjuk.
4. Telefon/Wear/GPS/Health készülékes elfogadás a felhasználó próbája után rögzíthető. A böngészős mock és az emulátor ezt nem helyettesíti.

## Végső ellenőrzött állapot

**Alkalmazás-/APK-forrás:** `65eea4473224614d74895b3ff8f841365a6c0ca0`. Fa: `07f783c3c31544825f729a1e32c7b55e9619c17b`. A későbbi dokumentációs commit nem változtatja az alkalmazást.

| Ellenőrzés | Eredmény | GitHub Actions |
|---|---|---|
| Quick Validation | 163 PASS / 0 FAIL | [40900070656](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900070656) |
| Performance | sikeres meglévő budget/strukturális kapuk | [40900070503](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900070503) |
| Teljes Chromium UI | 112/112 modul; 6/6 shard | [40900058363](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058363) |
| Telefon natív release | 251 teszt, 0 hiba/kihagyás; build és v2 aláírásfolytonosság sikeres | ugyanaz a Phone Release Gate |
| Telefon natív WebView | 3 teszt, 0 hiba/kihagyás | [40900058931](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058931) |
| Wear natív release | 167 teszt, 0 hiba/kihagyás; build és v2 ellenőrzés sikeres | [40900058671](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058671) |
| Kerek Wear UI | 183 külön teszt, 0 hiba/kihagyás; coverage kapu sikeres | [40900058641](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058641) |
| Vizuális fixture | 200 kép; 320/360/393, több szín, nagyobb betű, HU/DE/RO; 0 script/overflow hiba | Phone shard 1 |

A Performance a GitHub `7b0c7115` PR-merge checkoutján futott; annak fája byteazonos az ellenőrzött alkalmazásforrás fájával. Nem új fizikai gyorsulási mérés. A letöltött UI JSON-ok és natív XML-ek újra összegezve: [ci-evidence.json](ci-evidence.json).

A meglévő regressziócsomagok lefedik többek között a program szerkesztését/aktiválását/videóját, Settings/Drive/restore/nyelv működését, Napló dátum/lapozás/fotó/PR funkcióját, Health forrásokat, aktív edzés draft/Undo/RIR/automatikus súly/timer/Wear ownership folyamatát és a GPS explicit stop szabályait. Az új vizuális fixture külön ellenőrzi a PR vezérlőoszlopot, sticky füleket, szűrőket és a térkép bezárása után tovább futó rögzítést.

### Teszt-APK, nem nyilvános release

- [Telefon APK + pontos forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058363/artifacts/12908009635): 3 301 225 byte; SHA256 `e387885ca457e73f207695d4af2833640bc6625fed37d2acf53abfebe70f906c`.
- [Wear APK + metaadatok](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40900058671/artifacts/12910045097): 4 226 963 byte; SHA256 `374ccf48d7bf9ac50fb3b774109df3cb9af0e1c53203bd478c5f933fc00f6e59`.
- Mindkettő `com.repforge.app`, 1.3.71 /2735, source stamp `65eea447…`; az eredeti v2 tanúsítvány SHA256 `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. A telefon csomagolt www fájljai a helyi végső forrással egyeznek.

**Kód:** hét területen beépítve, a fenti termékkorlátokkal. **Automatikus ellenőrzés:** sikeres. **Vizuális ellenőrzés:** böngészős képek megtekintve, eltérések dokumentálva. **Telefonon elfogadott:** nem; a felhasználó próbája szükséges. Draft marad, nincs automatikus merge, verzióemelés vagy publikálás.
