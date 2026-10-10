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
- Saját indítóanimáció és az óráról történő telefonos hidegindítás javítása. Az elfogadott animáción enyhe készülékes akadás továbbra is előfordulhat.
- Natív Wear OS edzésvezérlés, körkijelzőhöz igazított kezdőlap, főmenü, heti mini naptár, telefonos megnyitás, Tile és számlapi kiegészítő.
- Engedélyezett Health Services-edzésmérés, háttérben futó előtérszolgáltatás, támogatott pulzus-/energia-/lépés-/távolságadatok, opcionális GPS és végső mérési összegzés.
- Stabil órás edzésazonosítók, offline parancsok és visszaigazolások, ismételt befejezés kezelése, törölt naplóbejegyzések visszaéledésének megakadályozása.
- Órás nullos szolgáltatásválaszok, újraindulás utáni mérési állapot és részleges végső adatok kezelése.

## 1.3

**Aktuális telefonos és Wear OS kiadáscsalád.** A [#156](https://github.com/fuloplevente1998/TrainPilot/pull/156) felhasználói jóváhagyással mainre került. [Telefonos kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.0) · [Órás kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.0).

A közös fejlesztési sorozat összevont tartalma:

- Valódi gyors edzés az órán, a telefonról kapott saját/beépített gyakorlatokkal és edzés közbeni gyakorlat-hozzáadással. A telefon és az óra ugyanazt az edzésazonosítót és mentett bejegyzést használja.
- **Mini napló:** legfeljebb nyolc mentett telefonos edzés, gyakorlat- és sorozatrészletekkel, offline cache-sel. A függő órás parancs és félkész edzés nem válik külön naplózott alkalommá.
- Telefon előtérbe kerülésekor újraküldött pillanatkép; Mini napló megnyitásakor és kézi frissítéskor új adatkérés. Régi pillanatkép nem írhatja felül az újabbat.
- Méretkorlátos, Unicode-helyes adatcsomag; aktív program nélkül is továbbítható mentett napló.
- **Órás mérések előzménye:** a három legutóbbi mentett, TrainPilot Wear-méréssel rendelkező edzés neve, dátuma, időtartama, gyakorlatinformációja és elérhető kalória/pulzus adata.
- Az előzményből a Mini napló meglévő részlete nyílik meg. A Vissza az eredeti mérési nézetre visz, az edzés mérési oldaláról indított megnyitásnál is.
- A megfelelő edzéshez tartozó TrainPilot Wear-mérés a telefon Naplójában alapértelmezett kiemelt forrás. A Health Connect/Samsung Health időszakos adatai külön maradnak, külön aktív, összes és edzéshez kötött energiajelöléssel.
- A későn érkező mérés a már mentett edzéshez kapcsolódik. Mért nulla megmarad; hiányzó érték nem lesz nulla vagy becslés; a részleges mérés jelölést kap.

### Telefonról kezdeményezett órás appindítás

Az aktuális köztes javítást is az **1.3** részeként dokumentáljuk. A [#160 PR](https://github.com/fuloplevente1998/TrainPilot/pull/160) még tesztág; a mainre kiadott APK-k ezt a javítást még nem tartalmazzák.

Új programos vagy gyors edzés telefonos indítása előbb közzéteszi az aktív edzést, majd megnyitja a csatlakoztatott órán a TrainPilotot. Az óra röviden felébreszti a kijelzőt, ellenőrzi az edzésazonosítót és a friss pillanatképet, majd az engedélyezett mérést az előtérszolgáltatásra bízza. A kijelző ezután kikapcsolhat.

- **Felhasználói visszajelzés, 2026. október 10.:** programos edzés telefonos indításakor az app megnyílik az órán, és a háttérmérést is rögzíti.
- A gyors edzés aktuális, soron belül beállítható gyakorlatválasztójának indításakor ugyanaz az egyszeri órás megnyitás indul az aktív edzés közzététele után. Új gyakorlat hozzáadása nem indít új mérést; megszakított piszkozatcsere és óráról indított edzés nem nyitja vissza az órás appot.
- A programos háttérmérés felhasználói visszaigazolása megérkezett. A gyors edzés új bekötése és a végső naplóadat teljes készülékes ellenőrzése következik.
- Megmarad a mérési kapcsoló, a szenzorengedélyek és más alkalmazás futó mérésének védelme.
- Normál frissítés, telefonos visszatérés vagy visszaállított piszkozat nem nyitogatja az órás appot. Sikertelen megnyitáskor a telefonos edzés megmarad, kézi órás megnyitással folytatható.

### Egészségadatok és nyitott ellenőrzések

A 20 guggolásos próba során a mért órás érték helyesen érkezett át. A korábbi **óra: 4 kcal / telefon: 1 aktív, 2 összes kcal** eltérés összehasonlítása továbbra is nyitott: ellenőrizni kell a forrást, a pontos időablakot, az energia jelentését és a szinkron időpontját.

TrainPilot Wear-mérés nélkül a telefon a kiválasztott Samsung Health/Health Connect szolgáltatóból olvas. Ha óra nincs a kézen, vagy a TrainPilot nem indított saját mérést, abból nem állítható elő utólag közvetlen Wear-edzésösszegzés. Más alkalmazás ugyanazon órán mért és megosztott adata külön adatút.

Folytatandó: háttérmérés, Mini napló frissítése/újracsatlakozása, körkijelzős és nagybetűs nézetek, már naplózott egészségadatok megőrzése, későn érkező értékek és telefonos edzés egészségadatai.

### Drive-szinkron és ütközésfeloldás

Az **1.3** [#160 tesztágán](https://github.com/fuloplevente1998/TrainPilot/pull/160) a normál szinkron csak az eszközönkénti legfrissebb mentést olvassa; a régi mentések olvasása külön, alapból csukott helyreállítási művelet. Normál szinkronkor nincs archív olvasás vagy automatikus archív takarítás.

A Telefon/Felhő ütközésválasztás a kiválasztott adatváltozatot menti; a logikai gombválasz nem kerül adatmező vagy naplóbejegyzés helyére. Az egyedi edzések és testsúlyadatok megmaradnak, a szigorú mentésellenőrzés változatlan. A szinkron időtartamának és a választás utáni sikeres mentésnek készülékes elfogadása még nyitott.

## Történeti részletek

A korábbi, különálló javítási feljegyzéseket a [Git-történet](https://github.com/fuloplevente1998/TrainPilot/blob/2fdcf4fde7021fb234bd85fff0a59b698d36c808/RELEASE_NOTES.md) és a meglévő release-ek őrzik. A régi belső számozás nem az aktuális nyilvános verziócsaládok folytatása. A jelenlegi állapotot a [fejlesztési áttekintés](DEVELOPMENT_STATUS.md) tartalmazza.
