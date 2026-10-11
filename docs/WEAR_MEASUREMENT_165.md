# Telefonról indított Wear mérés – #165

A távoli Activity megnyitása nem igazolja a mérés indulását. A javítás a teljes utat kezeli:

1. A telefon először publikálja az aktív edzést, majd az edzésazonosítót és a minimum revíziót átadva megnyitja az órát.
2. A látható Wear Activity célzottan visszakéri az edzésadatot. Ha az első DataItem késik vagy a változatlan adatból nincs új esemény, a telefon közvetlen üzenetben válaszol és újra publikálja a tartós adatcsomagot. A válasz csak az aktuális, még aktív, azonos edzés lehet.
3. Az óra 15 másodpercig ismételheti a visszakérést; lejárt, másik edzéshez vagy másik revízióhoz tartozó kérés/válasz nem indíthat mérést. Offline helyi módosítást és lezárt edzést a meglévő snapshot védelem továbbra is megőriz.

   Függő helyi órás módosításnál a frissebb telefonos revízió beérkezését külön elfogadja, miközben az órán módosított sorozatok és a helyi parancssorszám megmaradnak. Eddig a teljes csomagot figyelmen kívül hagyta: a távoli mérésindítás a már beérkezett revízióra várhatott korlátlan ideig, pedig az óra → telefon parancsok működtek.
4. A szenzoros foreground service továbbra is csak látható Activity-ből indul. Az indítás alatti képernyővédelem csak igazolt active/paused állapot, végállapot, hiányzó engedély, kikapcsolt mérés vagy 15 másodperces időkorlát után oldódik fel.
5. Egy felülírható `/trainpilot/watch-health` DataItem külön küldi a mérési állapotot, valamint legfeljebb 5 másodpercenként a rendelkezésre álló mért adatokat. Nincs korlátlan élőadat-parancssor. A végső összesítés változatlanul a tartós parancssoron is megérkezik.
6. A telefon az edzés fejlécében mutatja az állapotot, a legutolsó pulzust és összes energiát. A korábbi adatot nem nevezi frissnek, ha 20 másodpercig nincs visszajelzés. Az explicit „Óra megnyitása” gomb egyszeri új átadást kérhet. Minden adat edzés- és revízióazonosítóhoz kötött; az üzenetek nem változtatják a sorozatok értékeit.
7. A már megadott órás engedélyeket újra használja; kikapcsolt mérés esetén az óra az Órás mérések oldalra lép. A szünetelő Health Services mérés nem jelenik meg aktívként.

## Fizikai ellenőrzés Galaxy Watch7-en

Mind a telefonos, mind a Wear APK frissítése szükséges. A verziószám változatlan: 1.3.3 / 2729.

- Programnap és gyorsedzés indítása a telefonról: ugyanaz az edzés jelenjen meg az órán. Engedélyezett mérésnél a telefon mutassa az induló, majd aktív állapotot.
- A pulzus/energia megérkezését ellenőrizni az óra Órás mérések oldalán és a telefonos edzés fejlécében. Hiányzó szenzoradat maradjon hiányzó.
- Órakijelző kikapcsolása után a service folytassa a rögzítést; visszakapcsoláskor új mért érték legyen.
- Kapcsolat megszakításakor a telefon csak a legutolsó visszajelzést mutassa; újracsatlakozáskor a legfrissebb csomag érkezzen meg.
- Óráról mentés/törlés továbbra is ugyanazt a telefonos edzést zárja le; végső mérési összesítés érkezzen a naplóba. Másik vagy korábban lezárt edzést késői adat ne nyisson meg.
- Kikapcsolt mérés és hiányzó szenzorengedély: egyértelmű állapot, nincs hamis „Mérés folyamatban”. A meglévő engedélyek újbóli használata ne nyisson engedélykérőt.

Az automatikus intervallumok #162 / PR #164 külön fejlesztés. A fizikai hibajelenség pontos oka a készülékes próbáig nem tekinthető igazoltnak.
