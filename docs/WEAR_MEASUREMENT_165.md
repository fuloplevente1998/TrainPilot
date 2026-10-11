# Telefonról indított Wear mérés – #165

A távoli Activity megnyitása nem igazolja a mérés indulását. A javítás a teljes utat kezeli:

1. A telefon először publikálja az aktív edzést, majd az edzésazonosítót és a minimum revíziót átadva megnyitja az órát.
2. A látható Wear Activity célzottan visszakéri az edzésadatot. Ha az első DataItem késik vagy a változatlan adatból nincs új esemény, a telefon közvetlen üzenetben válaszol és újra publikálja a tartós adatcsomagot. A válasz csak az aktuális, még aktív, azonos edzés lehet.
3. Az óra 15 másodpercig ismételheti a visszakérést; lejárt, másik edzéshez vagy másik revízióhoz tartozó kérés/válasz nem indíthat mérést. Offline helyi módosítást és lezárt edzést a meglévő snapshot védelem továbbra is megőriz.

   Függő helyi órás módosításnál a frissebb telefonos revízió beérkezését külön elfogadja, miközben az órán módosított sorozatok és a helyi parancssorszám megmaradnak. Eddig a teljes csomagot figyelmen kívül hagyta: a távoli mérésindítás a már beérkezett revízióra várhatott korlátlan ideig, pedig az óra → telefon parancsok működtek.
4. A szenzoros foreground service továbbra is csak látható Activity-ből indul. Az indítás alatti képernyővédelem csak igazolt active/paused állapot, végállapot, hiányzó engedély, kikapcsolt mérés vagy 15 másodperces időkorlát után oldódik fel.
5. Egy felülírható `/trainpilot/watch-health` DataItem külön küldi a mérési állapotot, valamint legfeljebb 5 másodpercenként a rendelkezésre álló mért adatokat. Nincs korlátlan élőadat-parancssor. A végső összesítés változatlanul a tartós parancssoron is megérkezik.
6. A telefon az edzés fejlécében mutatja az állapotot, a legutolsó pulzust és összes energiát, valamint a rendelkezésre álló lépést, távolságot és sebességet. A lépés és távolság az egész órás edzés összesítése, nem az aktuális sorozaté. A korábbi adatot nem nevezi frissnek, ha 20 másodpercig nincs visszajelzés. Az explicit „Óra megnyitása” gomb egyszeri új átadást kérhet. Minden adat edzés- és revízióazonosítóhoz kötött; az üzenetek nem változtatják a sorozatok értékeit.
7. A már megadott órás engedélyeket újra használja; kikapcsolt mérés esetén az óra az Órás mérések oldalra lép. A szünetelő Health Services mérés nem jelenik meg aktívként.
8. Az egyszer már elfogadott telefonos indítás nem blokkolhatja egy következő szinkronizált edzés mérését a korábbi edzésazonosító miatt. Aktív edzés és hiányzó mérés esetén az órás oldal külön jelzi, hogy a mérés még nem indult, és egy „Aktuális edzés mérése” gombbal újraindítható. Az explicit indítás a friss, tartós edzésadatot olvassa, a megadott engedélyeket újra használja, hiba után a megőrzött mért értékeket megtartja.

A 2026. október 11-én küldött készülékfotón „Mérés a következő edzésnél” látszik, mért adatok nélkül. Ez engedélyezett mérési beállítást, de hiányzó aktuális mérési állapotot jelez; a fotó önmagában nem bizonyítja, hogy a készüléken melyik indítási feltétel maradt teljesítetlen.

## Fizikai ellenőrzés Galaxy Watch7-en

Mind a telefonos, mind a Wear APK frissítése szükséges. A verziószám változatlan: 1.3.3 / 2729.

- Programnap és gyorsedzés indítása a telefonról: ugyanaz az edzés jelenjen meg az órán. Engedélyezett mérésnél a telefon mutassa az induló, majd aktív állapotot.
- A pulzus/energia megérkezését ellenőrizni az óra Órás mérések oldalán és a telefonos edzés fejlécében. Hiányzó szenzoradat maradjon hiányzó.
- Órakijelző kikapcsolása után a service folytassa a rögzítést; visszakapcsoláskor új mért érték legyen.
- Kapcsolat megszakításakor a telefon csak a legutolsó visszajelzést mutassa; újracsatlakozáskor a legfrissebb csomag érkezzen meg.
- Óráról mentés/törlés továbbra is ugyanazt a telefonos edzést zárja le; végső mérési összesítés érkezzen a naplóba. Másik vagy korábban lezárt edzést késői adat ne nyisson meg.
- Kikapcsolt mérés és hiányzó szenzorengedély: egyértelmű állapot, nincs hamis „Mérés folyamatban”. A meglévő engedélyek újbóli használata ne nyisson engedélykérőt.

Az automatikus intervallumok #162 / PR #164 külön fejlesztés. A fizikai hibajelenség pontos oka a készülékes próbáig nem tekinthető igazoltnak.

## Készülékes folytatás: élő telefonos adat, üres órás mérési oldal

A felhasználó telefonon indított és telefonon fejezett be egy tervezett edzést. Edzés közben a telefonos fejléc 93 bpm / 2 kcal órás adatot mutatott, az óra Órás mérések oldala végig „—” maradt. A mentett telefonos Napló fotóján nincs külön TrainPilot Wear összesítés; a 3 kcal, 3 lépés és 0 pulzusminta Health Connectből származik. Ez az élő átadást igazolja, az órás kijelzést és a végső naplómentést nem.

A normál telefonos mentés a helyi teljes runtime-próbában megőrzi az élő Wear adatot. Két további adatút javítva:

- Az órás Activity másodpercenként a tartós aktuális edzésből frissíti a kijelzéshez használt azonosítót. A mérési nézet a helyi mérést és a telefonra ténylegesen publikált mérési csomag helyi másolatát ugyanahhoz az azonosítóhoz ellenőrzi, és a frissebb mérési revíziót mutatja. Másik edzés vagy másik mérőóra adata nem helyettesítheti az aktuális saját mérést.
- Telefonos lezárás után az óra megtartja a legutóbbi mérés látható értékeit, „Legutóbbi edzés mérése” jelöléssel és frissítési idővel. Új edzésnél a korábbi értékek nem látszanak aktuálisként.
- A telefonra lezárás után érkező érvényes élő mérési csomag összesítése a már mentett edzéshez is kapcsolódik, függetlenül attól, hogy az aktív edzésfejléc még létezik-e. A végső tartós parancsút megmarad. Régi revízió, eltérő azonosító és hibás csomag nem írhatja felül az értékeket vagy nyithatja újra az edzést.
- A telefonos indítás az órás átadás előtt rögzíti ugyanazt a stabil `syncId`-t, amelyet az óráról indított edzés már használt; mentés és késői összesítés az eredeti edzésazonosítóhoz kapcsolódik.

Célzott regresszió: valódi telefonos indítás és mentés 93 bpm / 2 kcal élő adattal, késői végső adat beérkezése a Naplóba, következő aktív edzés sértetlensége; JVM-próba a publikált és helyi mérés kiválasztására és változatlan UI-pillanatképre. A konkrét Watch7-es kiváltó ok és az órás élő kijelzés elfogadása továbbra is készülékes ellenőrzést igényel.

A következő készülékfotón az órán indított edzés telefonos Naplója már külön TrainPilot óra forrást mutat: 85,1 bpm átlag, 93 bpm maximum és 9 kcal. Az óráról indított összesítés útja tehát működik. Ugyanezen képen a telefonos edzésidő 57 másodperc, az órás aktív mérési idő 96,4 másodperc; a Health Services munkamenet/óra-időablak összevetése külön nyitott pont, nem tekintjük a két időt azonosnak és nem igazítjuk a mért értékeket becsléssel.

## Sikeres v2 próba és az 1.3.3 kiegészítése

A felhasználó 2026. október 11-én visszaigazolta: „Mindkettő szuperül üzemel.” Ez a két, `2aec9bbc15cff1530d3e1f7ec87411242908f036` forrásból készült, aláírt v2 APK működésének készülékes elfogadása. A visszajelzés nem állítja, hogy az összes külön hibaágat végigpróbálta.

A felhasználó a meglévő 1.3.3 kiadást az új kijelzésekkel együtt kéri frissíteni:

- Telefonos élő fejléc: pulzus/kcal, elérhető edzéslépés és mért távolság, aktuális sebesség km/h-ban. A Health Connect késleltetett lépéslekérése külön forrás marad.
- Órás gyakorlatnézet: pulzus és kcal az ismétléses és időméréses sorozatok mellett; rendelkezésre álló lépés/távolság külön, teljes edzésként jelölve. Szünetelő vagy korábbi adat nem jelenik meg friss aktív mérésként; a fix sorozatvezérlők helye változatlan.
- Lépésmértékű és ismert sétagyakorlatok a walking mérési típust kérik; futás/kerékpár és súlyzós típus külön marad. A Health Services által támogatott és engedélyezett adatokat kérjük. A GPS külön, meglévő beállítás, nem vezetünk be lépéshosszból számolt távolságot.
- Telefonos napló: a meglévő lépés/távolság/idő mellett minimum pulzus, pulzusmintaszám és mért maximum sebesség is megjelenik. A minimum és maximum a ténylegesen fogadott, ellenőrzött mintákból származik.
- Hiányzó mérési mező rejtve marad; valódi nulla megjelenhet. Az élő és végső adatok továbbra is az eredeti edzéshez kötődnek, nem írják át a tervezett értékeket.

Az új kijelzések külön készülékes próbája még szükséges; az 1.3.3-ba foglalásukat és a meglévő kiadás frissítését a felhasználó kifejezetten kérte.
