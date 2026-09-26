# TrainPilot – nyilvános repository tisztítása és 1.0.0 előkészítése

Állapotdátum: **2026-09-26**. Ez a dokumentum a dokumentációs átállás tervét rögzíti; **önmagában nem igazolja**, hogy a régi nyilvános ágak, tagek és release-ek törlése megtörtént.

## Biztonsági alap

- Nyilvános `main` kiindulási commit: `61b7d2008740ffd56d352736f10aa93ab8c2e08c`.
- Publikált 1.7.7 / 2670 release és aláírt APK: `v1.7.7`.
- A külön, privát `fuloplevente1998/TrainPilot-Archive` repositoryban 2026-09-26-án a teljes eredeti **56 ág**, **19 tag**, **17 release** és **62 release-asset** átvitele megtörtént; az eredeti Git-referenciák és az archivált assetek SHA-256 értékei ellenőrzöttek.
- A Google Drive-on külön teljes Git-bundle, mirror és release-biztonsági mentés található.
- A privát archívum `archive/metadata-20260926` ágán release-, issue- és PR-jegyzék, a `backup/release-files-20260926` ágon további kiadási jegyzékek vannak.
- A privát release-ek közül a `v1.7.2` és `v1.7.7` leírásának Windows-karakterkódolási hibáját kijavítottuk; a GitHubon mindkét ékezetes leírás visszaellenőrzött.
- Sem a nyilvános, sem a privát repositoryba nem kerülhet `.jks`, signing-jelszó vagy titkosítatlan kulcsmentés.

## Nyilvános repository végállapota

1. Csak a **`main` állandó ág** marad; a jövőbeli fejlesztési ágak ideiglenesen létezhetnek, majd merge után törlendők.
2. A jelenlegi történeti publikációból csak **`v1.7.7`** marad látható GitHub Release-ként és `v1.7.7` Git-tagként. A törölt régi tagek **nem** jelentenek Git-history törlést.
3. Az elavult egyszer használatos 1.7.4/1.7.6 kiadó workflow-k eltávolíthatók a publikációk archiválása után. A gyors regressziót, a teljes Release Gate-et és a Phase 7 teljesítményellenőrzést megtartjuk.
4. A README, a fejlesztési állapot, a buildleírás, a release-jegyzet és a Play ellenőrzőlista mindig a tényleges stabil alappal egyezzen.

## A 1.7.7 → 1.0.0 kiadási szabályai

- Az első 1.0.0 fejlesztési ág a `main`-ről indul; külön validált és telefonon elfogadott APK kerül kiadásra.
- Új `versionName`: `1.0.0`. Következő `versionCode`: legalább `2671`, mert a telepített 1.7.7-es APK kódja 2670.
- `com.repforge.app` application ID, signing tanúsítvány és régi tárolási kulcsok változatlanok maradnak a frissíthetőség és az adatok miatt.
- Minden érdemi kódtisztítást külön, mérhető lépésekben végzünk: legacy wrapper és eseménykezelő rétegek felmérése, regressziós tesztek, teljesítménykapu, APK-build, majd adatmegőrző 1.7.7 → 1.0.0 készülékes frissítési próba.
- Nem squasholjuk vagy force-pusholjuk a publikus `main` történetét.

## Még ellenőrizendő

- [x] A két archivált release-leírás UTF-8 javítása és visszaellenőrzése.
- [ ] A historyn kívüli issue-/PR-hozzászólások és review-k külön archívumának lezárása, ha szükséges.
- [ ] A régi nyilvános release-ek, tagek és mellékágak törlése CLI-vel, a `main` és `v1.7.7` megtartásával.
- [ ] A helyreállított keystore aláíró tanúsítványának összehasonlítása az eredeti APK-éval.
- [ ] Az első 1.0.0 release candidate teljes teszt- és telefonos jóváhagyása.
