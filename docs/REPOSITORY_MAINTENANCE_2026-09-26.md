# TrainPilot – nyilvános repository tisztítása és 1.0.0 megőrzése

Eredeti terv: **2026-09-26**. Frissítve: **2026-09-27**, az 1.0.0 (2675) kiadása után. A 2026-09-27-i tisztítást a GitHub állapotának külön ellenőrzése igazolta: a publikus repositoryban csak a `main` ág, a `v1.0.0` tag és a TrainPilot 1.0.0 Release maradt.

## Biztonsági alap

- Nyilvános `main` commit a tisztításkor: `ba2bcaa59359021bae299d5152c9dada266f0a79`. A `v1.0.0` tag az 1.0.0 kiadási commitra (`76f79dfd18baf2e23941ec3e3a230aba4374595a`) mutat.
- A nyilvános stabil kiadás már **`v1.0.0` / `1.0.0` (2675)**. A `v1.0.0` tag a `76f79dfd18baf2e23941ec3e3a230aba4374595a` commitra mutat; a Release és az aláírt APK publikálva van. Az eredeti `v1.7.7` APK aláíró tanúsítványa egyezik az új APK-éval.
- A külön, privát `fuloplevente1998/TrainPilot-Archive` repositoryban 2026-09-26-án a teljes eredeti **56 ág**, **19 tag**, **17 release** és **62 release-asset** átvitele megtörtént; az eredeti Git-referenciák és az archivált assetek SHA-256 értékei ellenőrzöttek.
- A Google Drive-on külön teljes Git-bundle, mirror és release-biztonsági mentés található.
- Az új 1.0.0 (2675) teljes forrása és hat kiadási fájlja a privát `archive/public-v1.0.0-2675` ágon szerepel. A négy publikus Release-asset SHA-256 értéke és a privát Git blobok egyezése ellenőrzött. Az ugyanitt tárolt `TrainPilot-public-all-refs-2026-09-27.bundle` a törlés előtti 65 ágat, 20 taget és a PR-referenciákat tartalmazza; a `git bundle verify` teljes történetként elfogadta.
- A privát archívum `archive/metadata-20260926` ágán release-, issue- és PR-jegyzék, a `backup/release-files-20260926` ágon további kiadási jegyzékek vannak.
- A privát release-ek közül a `v1.7.2` és `v1.7.7` leírásának Windows-karakterkódolási hibáját kijavítottuk; a GitHubon mindkét ékezetes leírás visszaellenőrzött.
- Sem a nyilvános, sem a privát repositoryba nem kerülhet `.jks`, signing-jelszó vagy titkosítatlan kulcsmentés.

## Nyilvános repository végállapota

1. Csak a **`main` állandó ág** marad; a jövőbeli fejlesztési ágak ideiglenesen létezhetnek, majd merge után törlendők.
2. A régi publikációk takarítása után **`v1.0.0` marad látható** GitHub Release-ként és Git-tagként, a hozzá tartozó APK-val és source ZIP-pel. A legújabb 1.0.0 verziót és a `main`-t semmiképpen sem szabad törölni. A törölt régi tagek **nem** jelentenek Git-history törlést.
3. Az elavult egyszer használatos 1.7.4/1.7.6 kiadó workflow-k eltávolíthatók a publikációk archiválása után. A gyors regressziót, a teljes Release Gate-et és a Phase 7 teljesítményellenőrzést megtartjuk.
4. A README, a fejlesztési állapot, a buildleírás, a release-jegyzet és a Play ellenőrzőlista mindig a tényleges stabil alappal egyezzen.

## A 1.7.7 → 1.0.0 kiadásának szabályai

- Az 1.0.0 (2675) már a `main`-en és a publikus Release-ben van; a korábbi terv itt történeti kontextus. A készülékes, adatmegőrző frissítési próba külön ellenőrzendő.
- Aktuális `versionName`: `1.0.0`; `versionCode`: `2675`. Az 1.7.7 APK kódja 2670 volt.
- `com.repforge.app` application ID, signing tanúsítvány és régi tárolási kulcsok változatlanok maradnak a frissíthetőség és az adatok miatt.
- Minden érdemi kódtisztítást külön, mérhető lépésekben végzünk: legacy wrapper és eseménykezelő rétegek felmérése, regressziós tesztek, teljesítménykapu, APK-build, majd adatmegőrző 1.7.7 → 1.0.0 készülékes frissítési próba.
- Nem squasholjuk vagy force-pusholjuk a publikus `main` történetét.

## Még ellenőrizendő

- [x] A két archivált release-leírás UTF-8 javítása és visszaellenőrzése.
- [ ] A historyn kívüli issue-/PR-hozzászólások és review-k külön archívumának lezárása, ha szükséges.
- [x] A pontos 1.0.0 (2675) commit és a publikus Release assetjeinek privát archiválása, hash-ellenőrzése.
- [x] 17 régi publikus Release, 19 régi tag és 64 mellékág törlése. Visszaellenőrzés: egyetlen ág (`main`, `ba2bcaa59359021bae299d5152c9dada266f0a79`), egyetlen tag (`v1.0.0`) és egyetlen Release (TrainPilot 1.0.0) maradt.
- [x] Az 1.0.0 és 1.7.7 APK aláíró tanúsítványának összehasonlítása; SHA-256: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [x] Az 1.0.0 (2675) Release Gate sikeres, a publikus Release elkészült.
- [ ] Készülékes 1.7.7 → 1.0.0 frissítés és adatmegőrzés külön ellenőrzése.
