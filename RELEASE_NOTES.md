# TrainPilot — összevont változáslista

A köztes javító- és tesztverziók tartalmát **1.0, 1.1, 1.2 és 1.3** alatt foglaljuk össze. A felsorolás a funkciók fejlődését mutatja; az újabb verziók a korábbi funkciókat is tartalmazzák.

A GitHub release-ek, tagek, APK-k, aláírások és tényleges buildszámok változatlanok. A telefon és a Wear kiadása továbbra is külön azonosítható. Az egyedi kiadási feljegyzések a [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) archívumban maradnak.

## 1.0

Az alapalkalmazás és a köztes javítások összevont tartalma:

- Helyben működő edzéstervezés, programok, gyors edzés, gyakorlatkönyvtár és saját gyakorlatok.
- Edzésnapló sorozatokkal, ismétlésekkel, terheléssel és idővel; közös, lenyitáskor betöltődő Edzés-/Programok-szerkesztő, egyértelmű Mentés/Mégse műveletekkel.
- Fejlődés, volumentrend, személyes rekordok, izomábra, Coach és a Napló Edzésnapló/Fejlődés/Statisztikák fülei.
- Privát edzésfotók, képgaléria és a naplóhoz kötött fotókezelés.
- Google Drive- és Google Naptár-integráció; teljes JSON/ZIP-mentés, fotófájlok és ellenőrzőösszegek, megszakítás utáni visszaállítási védelem.
- A programok, beállítások, kedvencek, téma és nyelv szinkronja; helyi és távoli törlések megőrzése, megkülönböztethető naplóbejegyzések védelme.
- Idő- és távolságalapú gyakorlatok, futás/séta/kerékpár és további kardiómozgások; kézi távolság, opcionális GPS és GPS nélküli beltéri időmérő.
- Kardióstatisztika, távolsággal súlyozott tempó és azonos mozgásra/távra értelmezett rekordok. Lépésből vagy hiányzó távolságból nem készül becsült kilométer.
- Health Connect-edzésadatok, aktív időszakokhoz kötött lépések, későbbi adatfrissítés és külön egészségmentési hozzájárulás.
- Egységes keretek, témaválasztó, közös nyilak, mobilos elrendezés és nagyobb rendszerbetűméretnél elérhető vezérlők.

## 1.1

Célok, tervezés és beltéri edzés:

- Heti edzésszám- és havi kilométercél; kezdőlapi haladás és külön beállítható célok.
- Kihagyott edzések előnézetes újratervezése, A/B sorrenddel és a megkezdett/teljesített edzések védelmével.
- GPS nélküli munka/pihenő intervallumok, menthető beállítások, hang/rezgés, szünet és újraindítás utáni helyreállítás.
- Coach-indoklás a rendelkezésre álló, dátumozott alvás-, HRV-, terhelés- és fájdalomadatokkal.
- Egységes üres állapotok, következő lépés és mentési visszajelzés.
- Kísérleti Bluetooth/RDFit diagnosztika, eszközazonosítás és korlátozott akkumulátor-/aktuális lépéslekérdezés. Ez nem teljes történeti egészségimport; a kísérleti belépő később kikerült az aktív felületből.

## 1.2

Egészségnapló, személyre szabás, telefonos felület és Wear OS alapok:

- Natív SQLite-egészségnapló, megőrzött rekordazonosítóval, eredettel és módosítási idővel; lapozott részletek, napi összesítések, ismételt import és törlésvédelem.
- Health Connect és Samsung Health közvetlen adatforrás kezelése, egyértelmű forrásjelöléssel. A szolgáltatók és az átfedő eredetek értékei nem adódnak össze; a hiányzó adat hiányzó marad.
- Engedélyezhető megnyitáskori és támogatott rendszeren háttérbeli Health Connect-frissítés; tranzakciós visszaállítás és nagy egészségmentések kezelése.
- Profilvezérelt Otthoni A/B Alap és 2. szint programok, kihagyható/folytatható onboarding, előnézet és külön elfogadás. Meglévő programot és naplót a profilváltozás nem ír át automatikusan.
- Naplóalapú terhelési ajánlás, fájdalom/readiness védelem, külön elfogadott testsúlyos variációváltás és menthető gyakorlatsorrend.
- Naptári napablak, áthelyezés és újratervezés; megőrzött azonosítók, előnézet, védett napok és sikertelen mentéskor visszaállított korábbi terv.
- Kompakt edzés utáni visszajelzés, közös mentési jelzés, egységes témák és helyi betűk, gyorsabb Kezdőlap/Coach/nézetváltás, javított Egészség-oldali frissítés.
- Saját indítóanimáció és az óráról történő telefonos hidegindítás javítása. A korábban jelzett indulási és navigációs lassulás célzott javítását 2026-10-22-én a felhasználó készüléken elfogadta; a részletek az [1.3.71 gyorsított kiadásában](docs/releases/v1.3.71-perf-20261022.md).
- Natív Wear OS edzésvezérlés, körkijelzőhöz igazított kezdőlap, főmenü, heti mini naptár, telefonos megnyitás, Tile és számlapi kiegészítő.
- Engedélyezett Health Services-edzésmérés, háttérben futó előtérszolgáltatás, támogatott pulzus-/energia-/lépés-/távolságadatok, opcionális GPS és végső mérési összegzés.
- Stabil órás edzésazonosítók, offline parancsok és visszaigazolások, ismételt befejezés kezelése, törölt naplóbejegyzések visszaéledésének megakadályozása.
- Órás nullos szolgáltatásválaszok, újraindulás utáni mérési állapot és részleges végső adatok kezelése.

## 1.3

**1.3.71 /2735 — #234:** visszazárható PR-részletek, laposabb edzésstílus/progresszió fejléc, működő Mégse és egységes haptika négyzet/pipa. Összehangolt telefon/Wear verzió, külön aláírt release gate-ek; [kiadási jegyzet](docs/releases/v1.3.71.md).

**Legutóbb közzétett APK: telefon + Wear 1.3.6 /2733, 2026. október 19.**
[Telefon](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.6) · [Wear OS](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.6) · [Dátum/platform szerinti kiadásjegyzék](docs/releases/INDEX.md).

A main ezen túl már tartalmazza a **#222-ben elfogadott #184 Samsung- és #197 RIR/Coach-javításokat**, változatlan 1.3.6 /2733 metaadatokkal. Ezek nem kerültek bele visszamenőleg a fenti APK-kba. Az [elfogadott telefonos teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379) külön Actions artifact; a Wear változatlan. [Forrás és elfogadás](docs/builds/2026-10-20-222-accepted.json).

### Az október 19-i kiadásig elkészült

- Közös telefon–óra gyors edzés, Mini napló, mérési előzmények, megőrzött azonosítók és későn érkező mérési adatok kapcsolása.
- Telefonos indításkor az órás app egyszeri megnyitása; védett piszkozatok és nyitott szerkesztők.
- Automata munka/pihenő/léptetés, rezgésjelzések, egyetlen időzítőtulajdonos és tartós szünet.
- Önálló órás edzés, visszaigazolt átadás, offline folytatás, órás GPS-útvonal és telefonos térkép.
- Sikeres, kifejezett telefonos GPS-befejezés elfogadja a mért időt/távolságot külön sorozat-OK nélkül.
- Megjegyzett GPS-választás, órás korona/lünetta szerkesztés, átrendezett menü, pontos pihenőhatáridő.
- Visszavonás, folyamatos 3 másodperces feloldás, nyelvi, szenzor- és kompakt felületi javítások.
- Kompakt, ellenőrzött JSON/ZIP-mentés, fotóhiba esetén hiányos mentés helyett jelzett hiba, legfrissebb Drive-mentések és külön helyreállítás.

### Később elfogadott main – #222, október 20.

- Normál/HIT-inspirált stílus; dupla, lineáris vagy RIR/Coach progresszió, globális és programonkénti beállítással.
- Előnézet, jóváhagyás és visszavonás; nincs automatikus súlyátírás.
- Tényleges 0–10 RIR bevitele, draft, Napló és későbbi ajánlás tartós adatútja.
- Mai, hiteles Samsung energiapontszám automatikus elsőbbsége; hiánynál a TrainPilot saját becslése marad.
- Samsung-kártya a Regeneráció alatt; egységes egész számú Home/Egészség kijelzés, nyitott panel melletti frissítéssel.
- Fájdalom/terhelés miatti Coach-korlátok megmaradnak; alvás/HRV/aktivitás nem számít kétszer.

A funkcionális sor lezárult. **#214** marad a repository-karbantartás és Play-előkészítés követésére; a Samsung production partneri hozzáférése külön ellenőrzés. A dizájnötletek [külön backlogban](docs/plans/UI_TEST_BRANCH_BACKLOG.md) maradnak.

## Történeti részletek

Az egyedi [telefonos](docs/releases/README.md) és [órás](android/wear/releases/README.md) feljegyzések az adott build akkori állapotát őrzik. A régi 1.3.51 számozást nem írjuk át. A teljes [kiadásjegyzék](docs/releases/INDEX.md) dátum, platform és tag alapján különbözteti meg őket.

Aktuális munkaátadás: [docs/WORK_HANDOFF.md](docs/WORK_HANDOFF.md). A DEVELOPMENT_STATUS.md és a régebbi tervek történeti források.
