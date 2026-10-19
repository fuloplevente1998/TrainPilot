# TrainPilot – aktuális átadás

Frissítve: **2026-10-19**. Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## #216 lezárva, 1.3.6 mainen

Felhasználói engedély: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez lezárási/main/kiadási jóváhagyás; részletes fizikai tesztbeszámolót nem találunk ki.

- **#218 merged `2059770c0eadf9c9e66929635865861667404d41`; #216 completed.**
  Órás menü: aktív edzés/Gyors edzés elöl; Naptár/Programnapok közös csoportban.
  Új kültéri gyors/tervezett edzés előtt látható, aktivitásonként megjegyzett GPS-választás,
  futó edzésről közvetlen állapot/kezelés. Reconnect/render nem indít rögzítést.
  Kézi gyakorlatváltás megőrzi a pihenő eredeti határidejét és automata fázis/cél/tulajdonosát.
  Telefonos működés és GPS-adatút megmaradt. HU/EN/DE/RO.
- **#219 merged `98be66221430202715aafa9a71583bbbd22d4fd3`; 1.3.6 /2733.**
  Mindkét app metaadatai és appfelirat, SW cache, verzióelvárások és kiadási jegyzetek frissítve.
  Main teljes fája byteazonos a bevizsgált `76fa539` PR-head fájával:
  `d0f5487aa3338369c879126b7d2ca15cbbd4e3b3`.
- #216 alkalmazáskód `c04851f`; utána csak tesztjavítások. A home fixture törli az aktív edzést,
  ezért a felfelé gesztus után **Gyors edzés** az első látható menügomb, nem Napló/Folytatás.
  Az eredeti azonnali láthatósági, gesztus- és geometriai feltételek megmaradtak.

## Ellenőrzött kiadási forrás

- Quick **39231417696**, Performance **39231417699**: sikeres (`76fa539`).
- Telefon **39231412495**: Chromium **6/6**, JVM **243/243** (`76fa539`).
- Wear **39231317332**: JVM **159/159** (`7b600e4`).
- Ownership **39231417723**: **81/81**; kerek UI/platform **39231317394: 183/183**.
- Minden vizsgált XML: **0 failure/error/skipped**. Külön #216 kerek UI
  **39231315228: 183/183**; új GPS/pihenő/menü tesztek mind az öt kijelző/betűméreten.
- `7b600e4..76fa539` csak AGENTS/átadás/bizonylat; alkalmazás- és tesztkód byteazonos.
- Aláírás folytonos a nyilvános v1.0.0-val; signer SHA-256:
  `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [Bizonylat, APK-hashek és források](builds/2026-10-19-release-136.json).

## Végleges, publikált 1.3.6 APK-pár

Mindkét kiadás/tag forrása **`98be66221430202715aafa9a71583bbbd22d4fd3`**, 1.3.6 /2733:
- [Telefon APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.6/TrainPilot-1.3.6.apk),
  SHA-256 `4b3b2fd90b38912a7a51157ed18c337f361f5e3f5521df69d10a64697c4cc987`.
- [Wear APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.6/TrainPilot-1.3.6-wear.apk),
  SHA-256 `ac9d16066df7f63290bca20656bf883391e41bbd1399cde397b9292176233958`.

Main telefon **39233098299**: 6/6 UI, JVM XML 243/243; Wear **39233098238**: JVM XML 159/159;
0 failure/error/skipped. Mindkét apksigner és eredeti signer-folytonosság sikeres.
A letöltött main artifact APK-hashek egyeznek a nyilvános release asset digestjeivel,
a telefonos forrásarchívum és Wear SOURCE_VERSION egyaránt `98be6622`-t igazol.
Main Quick **39233098560**, Performance **39233098199** sikeres.
Kerek UI **39233098292** további automatikus main futás; az azonos teljes forrásfa
183/183 kapuja a fenti **39231317394** futáson XML-ben igazolt. Élő státuszát GitHubon ellenőrizd.
A későbbi dokumentációs HEAD nem új alkalmazásbuild; a két APK tényleges forrása a fenti commit.

## Külön maradó backlog / lezárt előzmények

- **#206**: maradék aktivitásnézet/UI. Az explicit sikeres telefonos GPS „Mérés befejezése”
  külön sorozat-OK nélkül már elfogadott; térképbezárás/hibás/implicit stop nem teljesítés.
- **#197**: választható edzésstílusok/progresszió; **#184**: Samsung Energy Score;
  **#214**: karbantartás/Play. Az 1.3.6 ezeket nem zárja le.
- #193/#202 önálló órás GPS és telefonos naplótérkép + offline átadás/visszavétel elfogadva;
  #213 main `311f507`, #193 completed. Az órán nincs térképrajzoló: az óra rögzít, a telefon rajzol.
- #215 kompakt Intervallumok elfogadva/main `c67006b`; kétoldali UI nem engedélyez egyórás motort,
  két pozitív idő kell a kész állapothoz. #212 korona/lünetta #217 main `102eb366`, elfogadva.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen. #192/#211 sztornózott.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre engedélyezve,
  későbbi megvalósítás külön tesztágon. [Részletes #193 terv](wear/P2_193_ONALLO_ORAS_EDZES.md).
- Régi hosszú átadás: `885b6df:docs/WORK_HANDOFF.md` a Git-történetben; történeti státuszai elavultak.

## Környezet

CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
