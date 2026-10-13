# Telefonos és Wear OS automata mód, Wear UI-fejlesztési terv

Frissítve: 2026. október 13. Állapot: aktuális main megvalósítás + következő lépések; a készülékes elfogadás külön jelölt.

Ez az egy fájl fogja össze a **telefonos és órás automata mód közös tervét**, valamint a Wear OS UI- és funkcióötleteket. Az automata mód mindkét alkalmazás fejlesztési feladata. A következő döntéseket és pontosításokat is itt vezetjük tovább.

A közös main-integráció [#179](https://github.com/fuloplevente1998/TrainPilot/pull/179); a felhasználó 2026. október 13-án kérte a main aktualizálását. Új beszélgetéshez: [CHATGPT_FOLYTATAS.md](../CHATGPT_FOLYTATAS.md).

Kapcsolódások: [kezdőlap #170](https://github.com/fuloplevente1998/TrainPilot/issues/170), [Wear UI előzmény #178](https://github.com/fuloplevente1998/TrainPilot/pull/178), [automata mód #162](https://github.com/fuloplevente1998/TrainPilot/issues/162), [telefonos automata előzmény #164](https://github.com/fuloplevente1998/TrainPilot/pull/164), [későbbi térkép #163](https://github.com/fuloplevente1998/TrainPilot/issues/163).

## 1. Kiinduló állapot

- A #178 Wear-kezdőlapja és a #164 telefonos automata módja a #179 közös main-integrációjának része; a két régi ág történeti előzmény, a további munka a friss mainből indul.
- Kész a telefonos ikonokat/témát követő kezdőlap, külön Beállítások, közvetlen edzéstörlés megerősítéssel, felfelé húzható menü, Mini napló tördelése és gyakorlatoldali kis `HH:mm` idő.
- Kész az első telefonos és órás automata tesztváltozat, a közös tulajdonosi modell és a natív háttéridőzítők. A telefonon indított edzés teljes háttérrögzítése és a fizikai elfogadás még feladat.
- Az eredeti 1.3.4 release APK-kat a main frissítése nem cseréli le; a legutóbbi tesztpár pontos forrásazonosítóval szerepel lent.
- A térkép a #163 folytatás az automata működés lezárása után. Órás útvonalszakaszok külön további fejlesztés.

### Első közös tesztváltozat — 2026. október 13.

A #179 main-integráció a legutóbbi telefonos `3e66ab0` és órás `e41f09f` buildre épül. A legutóbbi átadott automata tesztpár: telefon `1650906`, Wear `e3db5e2`; a telefonos forrás a két commit között változatlan. Megmarad a telefonos zoomjavítás, az alsó visszaszámláló, a témaszinkron és az órás kezdőlap; az új funkciók tesztváltozatban kerülnek melléjük.

- Órán indított tervezett/gyors edzés: az óra az időzítő tulajdonosa. Az órás alkalmazás háttérbe tétele és a telefonkapcsolat elvesztése sem állítja le a natív időzítést; Health Services-engedély nem kell hozzá.
- Ismétléses sorozatot a felhasználó rögzít. A beállított pihenő után a következő sorozat/gyakorlat aktív, az ismétlést nem találja ki az app. Pontosan `mp` egységű sorozatnál a munkaszakaszok és körök automatikusan futnak. Oldalankénti mérés továbbra is kézi, utána automatikus pihenővel.
- Kis A/M kapcsoló a gyakorlatoldal órája mellett; munka/pihenő/kör és rezgés az edzésmenüben. A munkaszakasz első indítása kézi. Szünet/folytatás és pihenőkihagyás elérhető, a végső edzésmentés továbbra is külön megerősített művelet.
- Az órás jelzés: 80 ms a 3–2–1 másodpercnél, majd 1 másodperces egybefüggő rezgés. Szünet/leállítás megszakítja; ismételt állapotfrissítés nem ismétli meg.
- Óra által vezérelt edzésnél a telefon csak tükrözi a számlálót, és az órának küldi a rögzítést/szünetet/folytatást/kihagyást. Kézi gyakorlatváltás leállítja az automatikát. Stabil sorozatazonosító, szakaszszám és eseményazonosító védi a régi/dupla parancsoktól.
- Telefonon indított edzésnél a telefon marad a tulajdonos. A natív telefonos óra közvetlenül frissíti az órás kijelzést, az órás szünet/folytatás/kihagyás a telefon natív szolgáltatását vezérli. Nem indul második órás időzítő vagy kettős rezgés.
- Megszakadt szolgáltatás/újraindítás után az utolsó mentett hátralévő időből szünetelt állapot áll vissza; nincs utólag kitalált sorozat. Nyitott szerkesztőből nem menthető véletlenül a közben aktívvá vált másik sorozatba.

**Telepítés:** mindkét új APK szükséges. Az első használat előtt meg kell nyitni a telefonos appot és frissíteni az órás adatokat; az új közös protokoll ezután helyben is elérhető. Verzió: 1.3.4 / 2730; a tesztpárt a forráscommit különbözteti meg.

**Még hátravan:** telefonon indított ismétléses sorozat órás rögzítése és egy új telefonos menetrend óráról indítása a telefonos webfelület háttérben alvásakor még a telefon visszatéréséig várhat. Az óra ilyenkor „Telefonra vár” állapotot jelez. Emiatt a három használati irány teljes készülékes elfogadása még nincs kész. Az órás automata időmérés most a tényleges munka másodperceit összegzi, a meglévő telefonos intervallum a korábbi munka+pihenő értéket tartja; az egységes naplóbontás külön folytatás, régi adatok átírása nélkül. Órás GPS-szakaszok, visszavonás, érintészár, forgatható szerkesztés és új rezgésminták továbbra is tervben vannak. A fizikai rezgés és OEM háttérkezelés készülékes próbát igényel.

Ellenőrzés sikeres: 137 telefonos regresszió, 90 Chromium-tesztcsomag, 16 új Kotlin állapot-/tükrözési teszt és 51 emulátoros Wear-próba, beleértve a valódi foreground-service működést háttérbe tett Activityvel és kis kerek felületet nagy betűvel. Mindkét aláírt APK forrása és tanúsítványa ellenőrzött. A futások a [fejlesztési állapotban](../../DEVELOPMENT_STATUS.md) szerepelnek; a fizikai jelzések és párosított próbák külön hátravannak.

## 2. Wear OS-javaslatok és sorrend

A felhasználó ezeket felvette a tervbe; a lent javasolt részletszabályok még tervezési alapok.

| Sorrend | Fejlesztés | Tervezett működés | Elfogadás fő szempontja |
| --- | --- | --- | --- |
| 1 | Automata mód és pihenőjelzések | Munka/pihenő/sorozat/gyakorlat léptetése a 3. fejezet szerint; 3–2–1 előjelzés és külön indulási rezgés | Az óra lezárva is helyesen vezet végig, pontosan egyszer vált |
| 2 | Utolsó rögzítés visszavonása | Rögzítés után rövid ideig közvetlen Visszavonás; később az edzésmenüből is visszaérhető | A téves sorozat és a hozzá tartozó automatikus léptetés együtt visszaállítható |
| 3 | Érintészár edzés közben | Külön bekapcsolható védelem izzadság és ruha okozta érintések ellen; jól látható zárjel, hosszú nyomásos feloldás | Egy véletlen érintés nem rögzít, töröl vagy léptet; a feloldás egyértelmű |
| 4 | Lünetta/korona a szerkesztőben | A súly és ismétlés forgatással állítható a támogatott órákon; a plusz–mínusz és Mentés/Mégse is használható | Csak a megnyitott szerkesztő értéke változik, elfordítás nem ment magától |
| 5 | Kapcsolat és mentés állapotának finomítása | A már létező kapcsolatjelzés mellett világos „Órán mentve” / „Szinkronra vár” visszajelzés, szükség esetén újrapróbálás | Kapcsolatvesztéskor is érthető, hol van az edzés; nincs elveszett vagy kettős mentés |

UI-alapelv: a gyakorlatnév, aktuális szakasz és elsődleges művelet maradjon jól olvasható. Az új jelzések ne tolják le a gombokat; a kör alakú kijelző széle és a nagyított rendszerbetű is a terv része. A színek és ikonok a telefonos TrainPilothoz igazodjanak.

## 3. Automata mód — eldöntött működés

**Felhasználói döntés, 2026. október 12.: súlyzós, ismétléses edzésnél a felhasználó rögzíti a sorozatot; utána a pihenő és a következő sorozat indítása automatikus.**

Ez a szabály a telefonos és az órás alkalmazásban is érvényes, tervezett és gyors edzésnél egyaránt.

### Három megcélzott használat

| Használat | Elvárt működés |
| --- | --- |
| Csak telefon, óra nélkül | Teljes automata mód: beállítás, rögzítés, pihenő, sorozat-/gyakorlatváltás, jelzések, szünet, folytatás és mentés a telefonon |
| Csak óra, telefonkapcsolat nélkül | A helyben elérhető edzéssel önálló órás automata működés; helyi megőrzés és későbbi szinkron |
| Telefon és óra együtt | Ugyanaz az edzésállapot mindkét felületen; rögzítés/szünet/folytatás bármelyikről, egyetlen irányított idővonallal |

### Telefonos automata mód — önálló fejlesztési rész

- Edzésindítás előtt bekapcsolható Automata mód, az edzés és a gyakorlat típusához illő beállításokkal. A meglévő munka-/pihenőidő és körszám használható, a választás elmenthető.
- Ismétléses edzésnél a telefonos Rögzítés indítja a pihenőt, a lejáró pihenő pedig a következő sorozatot vagy gyakorlatot. Az ismétlés és a súly tényleges adat marad.
- Időzített gyakorlatnál és futóintervallumnál a munka/pihenő automatikusan vált, a sorozat vagy kör állapota a megfelelő edzéshez kerül.
- A natív Android telefonos appban lezárt kijelző és háttérben levő app mellett is időzítsen, a beállított telefonos hang-/rezgésjelzéssel. Látható edzésértesítésből lehessen szüneteltetni, folytatni és leállítani.
- Óra nélkül is teljes értékűen használható legyen. A telefonos GPS-mérés együtt futhasson az intervallummal; helyengedély-hiány ne állítsa le az időzítőt.
- A telefonos piszkozat-visszaállítás, edzésváltás, kézi beavatkozás és végső mentés ugyanazokat az állapotvédelmeket kövesse, mint az órás működés.
- Böngészős használatnál a korábbi nyitott oldalas korlát érvényes; a lezárt kijelzős háttérműködés vállalása a natív Android alkalmazásra vonatkozik.

### Ismétléses / súlyzós edzés

1. A felhasználó elindítja az edzést, majd elvégzi a sorozatot.
2. Ellenőrzi vagy javítja a tényleges súlyt és ismétlést, és megnyomja a Rögzítés gombot.
3. Elindul a beállított pihenő. A kijelzőn a hátralévő idő és a következő sorozat/gyakorlat neve látszik.
4. A pihenő végén rezgés jelez, és újabb Start-koppintás nélkül a következő sorozat munkaszakasza lesz aktív.
5. Ha a gyakorlat összes sorozata elkészült, a következő gyakorlatra vált. A sorozat vége ott is a felhasználó tényleges rögzítése.
6. Az utolsó gyakorlat utolsó sorozata után az edzés összegzése következik; az edzés végső mentése tudatos művelet.

Az automatikus indulás készenléti/munkaszakaszt jelent. A pihenő lejárta nem rögzít magától ismétlést, súlyt vagy teljesített sorozatot. Az óra mozgásából történő automatikus ismétlésszámlálás külön, későbbi kutatási feladat lenne.

### Időre végzett gyakorlat

- Beállítható munkaidő, pihenő és ismétlődés/sorozatszám. Az aktív munkaidő lejáratakor a mért időt az adott sorozathoz rögzíti, majd pihenőre vált.
- A pihenő végén automatikusan indul a következő munkaszakasz. A gyakorlat utolsó sorozata után a következő gyakorlat következik.
- Ismétléses gyakorlatra érkezve az ismétléses szabály lép életbe: a sorozat végét a felhasználó rögzíti.
- Oldalankénti időzésnél a bal/jobb oldal sorrendjét és az esetleges oldalcserés pihenőt külön meg kell adni. Ez a korábbi #164-ben megállási pont volt; nem tekinthető kész támogatásnak.
- Nulla pihenőnél közvetlen átmenet legyen; az utolsó munkaszakasz után ne induljon felesleges záró pihenő.

### Futás / időalapú intervallum

Példa: **30 mp futás → 60 mp pihenő/lassú szakasz → következő kör**, nyolc körön át, újabb Start-koppintások nélkül.

- A munka, pihenő és körszám előre állítható. A bemelegítés és levezetés választható későbbi bővítés.
- A kijelzőn az aktuális szakasz, hátralévő idő, körszám és a következő szakasz röviden jelenik meg.
- GPS-szel együtt is használható; a helyadatok a megfelelő edzéshez és szakaszhoz kapcsolódnak.
- A korábbi #164 a pihenő távolságát nem adja a munkaszakasz távjához. A lassú regeneráló futás külön mérése és összegzése még tervezési döntés; ezt nem változtatjuk meg észrevétlenül.
- GPS-jelkimaradás vagy engedélyhiány ne állítsa le az önálló időzítést. A hiányzó táv és a hibás helyjel legyen látható.

## 4. Állapotok és kézi beavatkozás

```mermaid
stateDiagram-v2
    [*] --> Keszen
    Keszen --> Munka: Indítás
    Munka --> Piheno: Sorozat rögzítve vagy munkaidő lejárt
    Piheno --> Munka: Pihenő lejárt / Pihenő kihagyása
    Munka --> Szunet: Szünet
    Piheno --> Szunet: Szünet
    Szunet --> Munka: Folytatás munkából
    Szunet --> Piheno: Folytatás pihenőből
    Munka --> Osszegzes: Utolsó sorozat elkészült
    Munka --> Osszegzes: Befejezés megerősítve
    Piheno --> Osszegzes: Befejezés megerősítve
    Szunet --> Osszegzes: Befejezés megerősítve
    Osszegzes --> [*]: Mentés
```

- **Pihenő:** a terv része, magától lejár. **Szünet:** felhasználói megállítás; a hátralévő idő és az aktív sorozat megőrződik.
- A Szünet/Folytatás minden munkaszakaszban és pihenőben elérhető. A felhasználó a pihenőhöz időt adhat vagy kihagyhatja azt.
- Kézi gyakorlatléptetésnél a kihagyott sorozat legyen kihagyott; ne váljon teljesítetté.
- Automata mód kikapcsolásakor az aktuális adatok megmaradnak, a következő automatikus váltás leáll.
- Leállítás, végső mentés és törlés külön műveletek; a meglévő törlési megerősítés megmarad.
- Visszavonásnál az idővonal először szünetel, majd az utolsó rögzítés előtti sorozathoz tér vissza. Újabb sorozat módosítása után már szerkesztési folyamat kell. A visszavonás ne törölje az órán ténylegesen mért egészségmintákat.

## 5. Telefonos és órás kijelzés, jelzések

### Telefonos felület

A folyamatban levő edzésben legyen egyértelmű Automata mód állapot, aktuális gyakorlat/sorozat vagy kör és következő gyakorlat. **Felhasználói pontosítás: az aktív visszaszámláló kézi és automata módban is alul, az Előző és Következő gomb között marad; az Automata mód kapcsolója kis, visszafogott vezérlő, kisebb az Előző/Következő gomboknál.** A tényleges ismétlés/súly rögzítése, Szünet/Folytatás és pihenő módosítása közvetlenül elérhető. A befejezés és mentés külön művelet. A háttérben frissülő idő vagy szinkron ne csukja össze a megnyitott szerkesztőt vagy edzésrészletet. A színek a választott telefonos témát kövessék.

### Órás felület

Javasolt kijelzés: felül gyakorlatnév, alatta kis sorozat/kör adat és pontos idő; középen a fontos állapot vagy hátralévő munka/pihenő; mellette/alatta pulzus és kcal. Pihenőben a következő gyakorlat is legyen előre látható. Az elsődleges gomb a pillanatnyi állapot szerint Rögzítés, Szünet vagy Folytatás.

Javasolt rezgések, még készülékes próbára:

| Esemény | Javasolt jel |
| --- | --- |
| Visszaszámlálás utolsó 3 másodperce | Másodpercenként egy rövid, 80 ms-os impulzus |
| Szakasz lejár, új munka/sorozat indul | Egy 1 másodperces, egybefüggő rezgés |
| Következő gyakorlat | Két elkülönülő impulzus |
| Edzéssor vége | A fentiektől eltérő, rövid záró jel |

A hang és rezgés külön kapcsolható legyen. Telefonos önálló használatnál a telefon, órás önálló használatnál az óra jelez. Közös használatban egy átmenet csak egy jelzéscsomagot indítson; a telefon és óra együtt ne adjon véletlenül kettős jelzést. A tervezett prioritás az órás rezgés, elérhetetlenség esetén a telefonos beállítás szerinti visszajelzés. Felhasználó által elfogadott első minta: 3–2–1 rövid előjelzés, lejáratkor 1 másodperces, egybefüggő rezgés. A #179 telefonos és órás natív időzítője ezt az egységes mintát futtatja; az eseményenként eltérő órás minták integrációja még hátravan. Szünet és leállítás megszakítja a jelzést, ismételt háttérfrissítés nem ismétli meg. Az előjelzés külön ki-/bekapcsolhatósága későbbi finomítás.

## 6. Telefon–óra együttműködés és megszakítás

- Egy edzés idővonalának egyszerre egy irányítója legyen. A telefonon indított edzés kezdetben telefonos, az órán indított edzés órás irányítással fusson; a másik készülék vezérlő és megjelenítő.
- Parancsonként stabil edzés-/gyakorlat-/sorozatazonosító és eseményazonosító kell. Dupla koppintás, késve érkező adat vagy újraküldés nem rögzíthet még egy sorozatot.
- Kapcsolatvesztéskor az irányító eszköz folytathatja az időzítést. A másik jelezze az offline állapotot; ne induljon el két független automatikus léptetés. Irányítóváltás csak egyeztetett, helyreállítható átadással történhet.
- A szakaszhatárt eltelt idő alapján kell számolni, nem UI-frissítésekből. A lezárt kijelző ne állítsa le az időzítést vagy a rezgést.
- Ha a futtató szolgáltatás megszakad vagy az eszköz újraindul, helyreállítható szünetelt állapot legyen. A kimaradt időből ne keletkezzen utólag több késznek jelölt gyakorlat.
- A GPS és Health Services életciklusa az edzéshez kötődjön; képernyőváltás ne indítson új mérést. Szünet és folytatás a meglévő mérési szabályokkal összhangban működjön.
- A napló külön őrizze meg a munka, tervezett pihenő és felhasználói szünet időtartamát. A #164 régi munka+pihenő összesítésének kompatibilitását a megvalósítás előtt pontosítani kell; régi naplóadat nem írható át találgatással.

## 7. Megvalósítási lépések és elfogadás

1. **Készülékes tesztpár-ellenőrzés:** telefon `1650906` + Wear `e3db5e2`, mindkét APK frissítése és első adatszinkron. Külön telefonos, önálló órás és párosított indítás; lezárt kijelző, tényleges rezgés és újracsatlakozás. A #179 már egyesíti a Wear UI-t és a telefonos/órás első automata módot.
2. **Telefonos tulajdon háttérrögzítése:** az órás ismétlésrögzítés és egy új telefonos menetrend indítása az alvó webfelület visszatérésére várhat. Ezt natív, stabil sorozatazonosítóhoz kötött feldolgozással kell folytatni, a telefonos felület és a háttérszolgáltatás kettős léptetése nélkül. Ütközés, duplikált/késői parancs, folyamatújraindítás és draft-védelem a regresszió része.
3. **Munka/pihenő/szünet és GPS:** az órás munkaidő és a régi telefonos munka+pihenő összesítés egységes, forráshű naplóbontása; a régi adatok változatlanok. Oldalankénti automata menetrend és órás GPS-szakaszok külön bővítések, nem kész támogatás.
4. **Kényelmi Wear-funkciók:** visszavonás, érintészár, lünetta/korona-szerkesztés és mentési/szinkronállapot finomítása, külön ellenőrizhető változtatásokban. A #170 kezdőlap első revampja kész; további UI-hibák készülékes visszajelzés alapján.
5. **Térkép #163:** az automata működés elfogadása után, elsőként a telefon GPS-ére építve. Útvonalpontok tárolása, megszakított szakaszok, offline nézet, mentés/visszaállítás/törlés és forráshű adatok szükségesek; órás útvonalrögzítés további hatókör.


Elfogadási próbák:

- A súlyzós és időzített/futó próbák mindegyike külön telefonon, óra nélkül; külön órán, kapcsolat nélkül; és párosított telefon–óra használatban is teljesül.
- Telefonos automata beállítások és vezérlők kis kijelzőn, Basic/Vivid témákban és a támogatott nyelveken; Android értesítési vezérlés és háttéridőzítés fizikai telefonon.
- Súlyzós 3 sorozat: minden sorozatot a felhasználó rögzít, pihenő végén automatikus indulás, végül helyes gyakorlatváltás.
- Időzített 30/60-as futás 8 körön át, nulla pihenővel is; az utolsó kör után helyes befejezés.
- Szünet munka és pihenő közben; folytatáskor ugyanaz a gyakorlat és helyes hátralévő idő.
- Pihenő hozzáadása/kihagyása, kézi léptetés, visszavonás; nincs kettős rögzítés.
- Lezárt telefon és óra, kijelző felébresztése, háttérből visszatérés, szolgáltatás megszakítása.
- Kapcsolatvesztés és újracsatlakozás mindkét indítási iránynál; az elmentett sorozatok és mérések egyeznek.
- GPS és időzítő együtt, jelkimaradás és engedélyhiány; helyes munka/pihenő hozzárendelés.
- Rezgések fizikai órán: érthetők, elkülönülnek, nincsenek duplázva.
- Kis kerek kijelző és nagyított betű; minden fontos gomb elérhető, a feliratok olvashatók.

## 8. Következő tervezési döntések

- Legyen-e külön felkészülési idő a következő gyakorlat előtt, és mi legyen az alapértéke?
- A rezgések közül melyek legyenek alapból bekapcsolva; kérünk-e hangot is?
- Futásnál a regeneráló szakasz külön munkatípus legyen-e, saját távval és tempóval?
- Melyik oldalcserés menetrend legyen támogatott az első automata változatban?
- Az érintészár kézi kapcsoló legyen-e első körben, vagy automatikusan is bekapcsolható induláskor?

Ezek nyitott tervezési kérdések. A súlyzós sorozat kézi rögzítése és a pihenő utáni automatikus indulás már eldöntött követelmény.
