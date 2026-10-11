# Automatikus intervallumléptetés (#162)

Az edzés időalapú gyakorlatán az **Intervallumidőzítő** lenyitható kártyájában adható meg a munka, pihenő és körszám; a hang és rezgés külön kapcsolható. A mentett beállítás tartalmazza az automatikus léptetés kapcsolóját is.

- Alapállapotban az intervallum egy sorozatot mér, és nem jelöli késznek.
- Automatikus módban a jelenlegi és következő időalapú sorozatokra ugyanez az intervallum érvényes. A sorozatok között a megadott pihenő is szerepel, az utolsó után nincs külön pihenő. A sorozat mért ideje a munka és pihenő együttese.
- Ismétlésszámos vagy oldalanként mért gyakorlatnál a program megáll. Nem ír ismétlést vagy súlyt, nem ugrik át egy erősítő gyakorlatot, és nem menti el automatikusan az edzést.
- Korábban beírt idő/távolság megmarad, az új mérés hozzáadódik.
- Szünet / folytatás a kártya óráján. A Leállítás megtartja az addigi mérést; új intervallum ezután indítható.

## Telefon és GPS

A felhasználó által, megnyitott alkalmazásban indított `IntervalTimerService` véges, monotón idővonalat futtat. A kijelző lezárásától független hang/rezgés és leállítható értesítés tartozik hozzá. A `specialUse` foreground service és a futás alatti, időkorlátos partial wake lock kifejezetten erre szolgál; szünetben és befejezéskor elengedi a wake lockot. Nincs bootból újraindítás vagy pontos-riasztás engedélykérés. A Play Console foreground-service deklarációjához a manifestben szerepel a konkrét fitneszidőzítő felhasználás.

A GPS külön elindítható az intervallum előtt vagy közben. Automatikus, egymást követő távolsági sorozatoknál a meglévő helymeghatározási szolgáltatás marad életben, csak az adat tulajdonosa vált. Minden szakasz idő/távolság adata a stabil sorozatazonosítóhoz kerül. Nincs új háttérből indított location service. Szünetben a GPS sem mér, folytatáskor új koordináta-horgonyt vesz fel, hogy a szünet alatti elmozdulást ne számolja be.

Piszkozatból a szolgáltatás ugyanazon azonosítóihoz csatlakozik vissza. Ha az Android megszakította a folyamatot, szünetelt/megszakadt állapotot mutat; nem állítja késznek a kimaradt időt. Az értesítés Leállítás gombja az adott időzítő azonosítójára érvényes. A GPS-szolgáltatás is csak a saját elvárt mérési kulcsával állítható le vagy váltható át.

Böngészőben a manuális időzítő kompatibilis marad; automatikus módban az oldalt nyitva kell tartani. Újratöltés után a böngészős program megszakad, nem folytatódik észrevétlenül.

## Ellenőrzés

- Node: fázishatárok, nulla pihenő, sorozatok közti pihenő, késői callback, 24 órás / 100 sorozatos korlát, duplikált gyakorlat mellett stabil tulajdonos.
- JVM: ugyanazon idővonal határesetei; GPS-horgony visszaállítása szünet után.
- Böngésző: 32 telefonméret/nyelv/téma kombináció; mentett kapcsoló; automatikus sorozat/gyakorlatváltás; GPS-szakaszok, szünet/folytatás; piszkozat; dupla indítás és későn visszatérő bridge-válasz; kézi végső mentés. A natív bridge válaszait itt tesztadapter adja.
- Teljes meglévő release gate és Android/JVM build a PR-ágon.

Fizikai telefonos elfogadási ellenőrzés még szükséges: hang + rezgés lezárt kijelzővel; GPS és intervallum együtt, szünet/folytatás; értesítésből leállítás; app bezárása és visszaállítás. A CI nem bizonyít OEM akkumulátorkezelést vagy tényleges GPS-pontosságot.

A verzió továbbra is 1.3.3 / 2729. A térképes útvonalnapló külön #163 feladat, ez a változtatás nem tárol útvonalkoordinátákat.
