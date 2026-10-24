# TrainPilot – a 24 natív képernyő dizájnátadója

Frissítve: 2026-10-24. A felhasználó külön kérte a korábbi négy mockup
újratervezését, a rögzített működési pontosításokkal. Az új táblák review-változatok;
a vizuális irány elfogadott, az új képek külön felhasználói áttekintésre várnak.
A képek generált látványtervek, a végső átadás futó emulátorból készített képeket igényel.

## Közös felépítés

- Öt alsó fül: **Kezdőlap, Edzés, Napló, Fejlődés, Egészség**.
- Jobb felső **Beállítások fogaskerék** minden főoldalon és aktív edzésben.
  Vissza ugyanahhoz a képernyőhöz/sessionhöz; megnyitása nem pause vagy új edzés.
- Sötét grafit/arany bemutató, konzisztens natív Compose kártyák és ikonok,
  jól olvasható szöveg, kompakt elrendezés, legalább48 dp érintési célok.
- **32 megőrzött paletta:** 26 main Basic/Vivid +6 natív kompatibilitási paletta.
  Minden szín tématokenből jön; a mentett felhasználói választás érvényesül.
- HU/EN/DE/RO; nagy betűméret és kis kijelző mellett tördelődő vezérlők.
- A mockup számai illusztrációk. Darabszám, dátum, állapot és mérés valódi adatból
  jön. A beépített katalógus jelenlegi forrása106 gyakorlat és13 program; az egyedi
  tartalom miatt a tényleges listák ennél nagyobbak lehetnek.

## Funkciómegőrzés – a 24 kép nem képernyő- vagy funkciólimit

Felhasználói pontosítás: az egészségnapló, testsúly, pulzus és a main többi
meglévő funkciója a natív appban is megmarad. A mockup áttekintő főnézeteket
mutat; az ott nem ábrázolt részletek nem törölhetők a termékből. A végleges
képernyőlefedettség a szükséges aloldalakkal, fülekkel és dialógusokkal bővül.

| Terület | A főnézeten túl is megőrzendő |
| --- | --- |
| Egészségnapló | Korábbi napok/időszakok, metrikák részletei, mért értékek, forrás és időbélyeg; teljes main-metrikakészlet |
| Testsúly | Mentett mérések, kézi rögzítés, időszakszűrés, változás és grafikon; a szolgáltatói testsúly forrása elkülönül |
| Pulzus | Napi átlag/nyugalmi/minimum/maximum és meglévő előzmények; valódi élő Watch/BLE adat külön állapot |
| További egészségadatok | Alvás/HRV, energia, lépések, kalóriák, testösszetétel és minden egyéb, mainben elérhető adat; hiányzó engedély/mérés jelölve |
| Kapcsolatok | Samsung Health/Health Connect, BLE és Wear meglévő beállításai és állapotai |
| Egyéb main-funkciók | Program-/gyakorlatszerkesztés, napló/részletek/szűrők, fotók, backup, naptár és minden létező beállítás külön lefedettségi ellenőrzése |

Átadás előtt main-funkció → natív útvonal → adatforrás → teszt megfeleltetés kell.
A mockupon hiányzó funkció nem jelölhető késznek pusztán adatmodell vagy motor alapján.

## Négy tábla, 24 képernyő

| Tábla | A hat képernyő |
| --- | --- |
| [1/4 – Kezdőlap és tervezés](mockups/01-main-pages-and-planning.png) | Kezdőlap; Edzés; Programok; Naptár; Gyakorlatok; Gyakorlat részletei |
| [2/4 – Edzés és mérés](mockups/02-workouts-and-measurement.png) | Súlyzós edzés; Időalapú edzés; Kétoldali mérés; Kültéri mérés; Intervallumok; Edzés befejezése |
| [3/4 – Napló, térkép és egészség](mockups/03-journal-maps-and-health.png) | Napló; Edzés részletei; Edzésútvonal; Fejlődés; Egészség; Coach |
| [4/4 – Beállítások és szerkesztés](mockups/04-editing-and-settings.png) | Beállítások; Óra és szinkron; Mentés és visszaállítás; Engedélyek; Privát edzésfotók; Program szerkesztése |

### 1/4 – Kezdőlap és tervezés

Dinamikus Folytatás-kártya aktív/importált/szüneteltetett edzésnél, ugyanazzal az
azonosítóval. Gyors edzés elöl. Galaxy Watch7 státusz és négy egészségcsempe.
Edzésválasztó programnapokkal, Programok/Naptár/Coach gyorseléréssel. Programlista,
havi/napi naptár, kereshető kategóriás könyvtár és valódi gyakorlatleírás/bemutató.

### 2/4 – Edzés és mérés

Kg/Ism./RIR sorok, visszavonás, gyakorlatváltó nyilak. Pihenőn szünet,
−15 mp/+15 mp és Kihagyás. Gyakorlatváltáskor a pihenő megmarad.
Egyetlen alsó sorozatstopper, összecsukott opcionális intervallum.
Explicit pozitív stop készre jelöl; kétoldali mérés mindkét pozitív oldal után kész.
Globális/edzésenkénti intervallumpreset és explicit automatikus léptetés.
GPS-választás látható; élő pulzus forrás nélkül **— bpm**. Manuális adatok lenyithatók.
Befejezés: mentésre váró összegzés, hat visszajelzés, megjegyzés, Mentés,
Szüneteltetés és kilépés, külön megerősített Elvetés. Sikertelen mentés/elvetés
után helyben újrapróbálható; siker előtt nincs lezárási navigáció/nyugta.

### 3/4 – Napló, térkép és egészség

Lapozott napló, teljes adathalmazra működő keresés, PR és létező útvonal előnézete.
Részletek Gyakorlatok/Statisztikák/Forrás fülekkel, fotó- és útvonaleléréssel.
Teljes képernyős privát térképen **piros X**; csak a térképet zárja.
A térképmodalnak nem kell alsó navigáció/fogaskerék. Offline saját útvonal és
háttértérkép-elérhetőség külön állapot; nem ígérünk nem létező offline csempéket.
Fejlődés: 1RM/volumen/PR. Egészség: valódi források, időbélyeg, hiányzóadat-állapot.
Coach: a NativeCoachReadiness eredménye; Samsung energia, saját becslés és korlátozott
Coach-pontszám külön. A mintabeli87/61/54 nem összegzés. A gyakorlatonkénti tanács
nem írja át a programot vagy az aktív edzést.

### 4/4 – Beállítások és szerkesztés

Teljes témakatalógus és négy nyelv. Időzítő, eszközök, profil, adatvédelem.
Ownership/handoff és offline folytatás állapota, tudatos átadás. JSON/ZIP/Drive
mentés, előnézet és félbeszakadt helyreállítás explicit folytatása. Egészségadat
mentése külön választás. Pontos GPS-engedély és központi helyszolgáltatás külön.
Privát fotók, kamera/galéria/törlés; a felhő/ZIP-leírás a ténylegesen engedélyezett
mentési beállítást tükrözze. Programszerkesztő napokkal, sorrenddel,
sorozat/ismétlés/súly/pihenő értékekkel és tudatos mentés/elvetés művelettel.

## A képeken látható mintafeliratokat felülíró pontosítások

- Beállítások és más másodlagos oldalak az eredeti navigációs kontextust őrzik;
  a képtáblák eltérő kiemelései nem jelentenek új navigációs szabályt.
- A Programok képen megjelenő „Közösségi” mintaelemhez nem készült elfogadott
  közösségi funkcióspecifikáció; ezt a megvalósításba nem kell átvenni.
- Helyes restore-szöveg: **„Aktív edzés közben nem cserélhetők az adatok.”**
  Nem az edzés ELŐTT tiltott; előbb befejezés/elvetés szükséges, ha aktív session van.
- Átadás: az aktuális, megfelelő tulajdonú edzés tudatos átadása. A „következő
  edzésnél” vagy „edzés indítása” képfelirat nem írja felül az ownership-protokollt.
- A verzió a tényleges buildből jön. A táblák személyes köszöntései, kalóriái,
  fotói és számadatai nem alapértelmezett felhasználói adatok.

A működés részletes elsőbbségi szabályai: [NATIVE_MOCKUP_REVIEW.md](NATIVE_MOCKUP_REVIEW.md).
Teljes megvalósítási feladat: [NATIVE_FINAL_DELIVERY.md](../plans/NATIVE_FINAL_DELIVERY.md).
Aktuális kód/tesztállapot: [WORK_HANDOFF.md](../WORK_HANDOFF.md).
