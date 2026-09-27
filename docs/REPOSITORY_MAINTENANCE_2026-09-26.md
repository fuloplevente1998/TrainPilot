# TrainPilot – nyilvános repository tisztítása és 1.0.0 megőrzése

Eredeti terv: **2026-09-26**. Frissítve: **2026-09-27**, az 1.0.0 (2675) kiadása után. Ez a dokumentum **önmagában nem igazolja**, hogy a régi nyilvános ágak, tagek és release-ek törlése megtörtént.

## Biztonsági alap

- Nyilvános `main` kiindulási commit: `61b7d2008740ffd56d352736f10aa93ab8c2e08c`.
- A nyilvános stabil kiadás már **`v1.0.0` / `1.0.0` (2675)**. A `main` és a `v1.0.0` tag a `76f79dfd18baf2e23941ec3e3a230aba4374595a` commitra mutatott a kiadáskor; a Release és az aláírt APK publikálva van. Az eredeti `v1.7.7` APK aláíró tanúsítványa egyezik az új APK-éval.
- A külön, privát `fuloplevente1998/TrainPilot-Archive` repositoryban 2026-09-26-án a teljes eredeti **56 ág**, **19 tag**, **17 release** és **62 release-asset** átvitele megtörtént; az eredeti Git-referenciák és az archivált assetek SHA-256 értékei ellenőrzöttek.
- A Google Drive-on külön teljes Git-bundle, mirror és release-biztonsági mentés található.
- Az új 1.0.0 (2675) commitjának és Release-assetjeinek privát archiválása még ellenőrzésre vár; a korábbi archív mentés **nem** tartalmazhatta ezt az utóbb elkészült verziót. A nyilvános törléseket csak a pontos új commit és az assetek privát másolatának ellenőrzése után szabad elvégezni.
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
- [ ] A pontos 1.0.0 (2675) commit és a publikus Release assetjeinek privát archiválása, hash-ellenőrzése.
- [ ] A régi nyilvános release-ek, tagek és mellékágak törlése a `main`, `v1.0.0` tag és `v1.0.0` Release megtartásával.
- [x] Az 1.0.0 és 1.7.7 APK aláíró tanúsítványának összehasonlítása; SHA-256: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [x] Az 1.0.0 (2675) Release Gate sikeres, a publikus Release elkészült.
- [ ] Készülékes 1.7.7 → 1.0.0 frissítés és adatmegőrzés külön ellenőrzése.
