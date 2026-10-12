# Kerek kijelző és stabil rövid oldalak — #176

A készülékes fotókon a korábbi kiadás középső sávra szűkített listái, majd az első teljes képernyős tesztbuild hibái is láthatók: a kezdőlap feleslegesen görgethetővé vált, a félképernyős térközök nagy üres részt hagytak, és a gombokat le lehetett görgetni az óráról. A súly/ismétlés szerkesztőben a vezérlők külön sorokra kerültek. Az emulátor korábbi elérhetőségi próbái ezeket a használhatósági hibákat nem ellenőrizték.

## Elrendezés

A kezdőlap, aktuális sorozat, stopper, szerkesztő, megerősítés és mentési összesítő ismét rögzített fejlécet és külön rögzített gombokat használ. A kör geometriája a gombok tényleges helyét határozza meg; nem szűkíti egy belső téglalapra az egész oldalt. A rövid szöveg középen marad. Csak a valóban túl hosszú tartalom görgethető a fejléc és a gombok között, például nagy betűméretnél vagy részletes mérési összesítésnél. A mérési adatok érkezése nem mozdítja el az alsó vezérlőket.

A súly/ismétlés/idő szerkesztőben a − és + a szám mellett van. A Mentés és Mégse állandóan látható külön alsó gombok. A kézi időmódosítás továbbra is törli a régi szüneteltetett stopper állapotát, mentés csak a Mentés gombbal történik.

A kezdőlap mindkét irányú vízszintes húzása a főmenüt nyitja. A Menü és a felső, koppintható Telefon cím is működik. A teljes kezdőlap-revamp külön #170.

A hosszabb listák szabványos Compose `LazyColumn`-t használnak 8 dp oldalsó margóval, félképernyős térközök és automatikus középre igazítás nélkül. Megnyitáskor az első sor következik a cím alatt. A címet külön hely tartja távol a soroktól; nincs rájuk rajzolt fekete háttérív. A heti naptár saját teljes képernyős elrendezése változatlan. Az edzéslapozó inaktív lapjai nem kapnak forgókorona-fókuszt, és nem duplázzák az akadálymentes vezérlőket. A lapjelző a felső peremen marad.

A várakozó mentés/törlés állapotmondata több sorba törik, nem egysoros ellipszissel jelenik meg.

## Ellenőrzés

- Natív kerek Wear OS emulátor: 160, 192 és 225 dp, normál, 150% és 200% betűméret.
- A főmenü első/utolsó gombja megnyomható, minden hosszú lista és a naptár végigjárható.
- A rövid kezdőlap szövege függőleges húzások után nem mozdul el; balra és jobbra húzva is megnyílik a menü.
- A szerkesztő mindkét mezőnél ellenőrzi a −/szám/+ sorrendet, a Mentés/Mégse láthatóságát, az érték változtatását, a mentett különbséget és a mentés nélküli visszalépést.
- Új pulzus/kcal adat érkezése és függőleges húzás sem mozdítja el az edzés alsó gombjait.
- A képernyőképek és teszteredmények CI-artifactokban szerepelnek. A futásazonosító és az aláírt APK lenyomata a PR-be kerül.

A tesztbuild továbbra is 1.3.4 /2730, külön fájlnévvel; a korábbi telefonos 1.3.4-gyel kompatibilis. A készülékes elfogadás még szükséges. A nyilvános 1.3.4 kiadás változatlan marad addig.
