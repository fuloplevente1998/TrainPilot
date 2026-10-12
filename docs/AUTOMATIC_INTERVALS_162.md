# Automatikus intervallumléptetés (#162)

## Telefonos folytatás — 1.3.4

Az aktuális mainre frissített ágban a folyamatban levő tervezett vagy gyors edzés **Automata mód** kapcsolója engedélyezi a kézi rögzítés utáni automatikus pihenő-/gyakorlatléptetést. A választás elmenthető. Az ismétlést és súlyt továbbra is a felhasználó rögzíti; a pihenő nem ír kitalált sorozatot.

A felhasználó kérése szerint a visszaszámláló manuális és automata módban is a meglévő alsó vezérlősávban marad. Az Automata mód kis, másodlagos kapcsoló, az Előző/Következő gombokhoz képest kisebb. Az automatikus pihenő szünet-/kihagyás vezérlése is kompakt; a fő tartalom fölé nem kerül nagy automata kártya.

- Ismétléses gyakorlat: Rögzítés → a gyakorlat/program szerinti pihenő → következő befejezetlen sorozat/gyakorlat. A pihenő szüneteltethető, folytatható és kihagyható. Az utolsó sorozat után nincs felesleges pihenő vagy automatikus végső mentés.
- Súlyzós → időzített átmenet: a következő időalapú sorozat menetrendje az előző gyakorlat pihenőjével indul, majd automatikusan munkára vált. A kezdő pihenő nem növeli a sorozat mért munkaidejét.
- Az önálló ismétléses pihenő is ugyanazt a natív Android foreground időzítőt használja. Lezárt telefon mellett a szolgáltatás időzít és jelez; az előkészített következő sorozatot a felület visszatéréskor ugyanahhoz az azonosítóhoz illeszti. A készre jelölés kézi marad.
- Az Android értesítésből Szünet/Folytatás és Leállítás érhető el. A GPS tervezett pihenőben és kézi szünetben is szünetel; munkára visszatérve új horgonyt vesz fel.
- Visszapipálás, kézi léptetés, sorozattörlés, új edzés és későn visszatérő natív válasz nem rögzíthet másik edzésbe. Megszakadt szolgáltatásból nincs kitalált folytatás.
- A korábban átadott Wear-kezdőlap témaszinkronja megmarad a telefonos home snapshotban.

Ez a folytatás **telefonos megvalósítás**. Az önálló órás automata mód és az eszközök közös idővonalának integrációja következő lépés; a mostani Wear app nem kap új automata funkciót ettől a telefonos ágtól. A közös terv egy fájlban: [WEAR_OS_FEJLESZTESI_TERV.md](https://github.com/fuloplevente1998/TrainPilot/blob/feat/wear-home-revamp-170/docs/wear/WEAR_OS_FEJLESZTESI_TERV.md).

## Időzített sorozatok

Az edzés időalapú gyakorlatán az **Intervallumidőzítő** lenyitható kártyájában adható meg a munka, pihenő és körszám; a hang és rezgés külön kapcsolható. A mentett beállítás tartalmazza az automatikus léptetés kapcsolóját is.

- Alapállapotban az intervallum egy sorozatot mér, és nem jelöli késznek.
- Automatikus módban a jelenlegi és következő időalapú sorozatokra ugyanez az intervallum érvényes. A sorozatok között a megadott pihenő is szerepel, az utolsó után nincs külön pihenő. A sorozat mért ideje a munka és pihenő együttese.
- Ismétlésszámos vagy oldalanként mért gyakorlatnál az időalapú menetrend megáll. A felhasználó ténylegesen rögzíti a sorozatot; a teljes edzés Automata mód kapcsolójával az ezt követő pihenő tovább léptet. Nem ír ismétlést vagy súlyt, nem ugrik át egy erősítő gyakorlatot, és nem menti el automatikusan az edzést.
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

A verzió az elfogadott main szerinti 1.3.4 / 2730; a tesztbuild a forráscommit alapján azonosítható. A térképes útvonalnapló külön #163 feladat, ez a változtatás nem tárol útvonalkoordinátákat.
