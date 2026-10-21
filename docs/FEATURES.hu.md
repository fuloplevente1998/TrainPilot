# TrainPilot – funkcióáttekintés

Állapot: **1.3.71 /2735**, elfogadott és kiadott telefon/Wear forrás: `60fe647a98ccdcc7f102d647a6a1cc8db40bbbfa`. Ez a meglévő képességek összefoglalója; nem fejlesztési ígéret. [Képernyőképek](../README.hu.md#képernyőképek) · [Letöltések](releases/INDEX.md).

## Edzéstervezés

- Saját programok, szerkeszthető napok és gyakorlatok; beépített gyakorlatkönyvtár és saját gyakorlat.
- Profil alapján előnézhető, külön elfogadással létrehozható Otthoni A/B programok. Profilváltozás nem írja át automatikusan a meglévő naplót/programot.
- Gyors edzés programválasztás nélkül; programnapos edzés a kezdőlapról és az Edzés oldalról.
- Naptáras ütemezés, edzésszám-/távolságcélok, kihagyott alkalmak előnézetes újratervezése.

## Erősítő edzés és progresszió

- Súly, ismétlés, sorozat, pihenő és kétoldalas gyakorlatok kezelése.
- A következő erősítő gyakorlat súlyai és ismétlései a Napló legutóbbi megfelelő, teljesített alkalma alapján automatikusan töltődnek, gyors és tervezett edzésnél is. Nincs másolásgomb; a kézi módosítás és a meglévő draft védett. Teljesítettség, RIR és mérési adatok nem öröklődnek.
- Normál/HIT-inspirált stílus; dupla, lineáris és RIR/Coach progresszió, programonkénti beállításokkal és terhelési lépcsőkkel.
- Tényleges, opcionális **0–10 RIR sorozatonként**. A cél nem mérés; a hiányzó adat nem nulla. A Coach ajánlása és jóváhagyása külön terhelési döntés.
- Munka/pihenő időzítés, automata fázisok, hang-/rezgésjelzések, kapcsolható kézi sorozatrögzítési haptika és utolsó sorozatrögzítés visszavonása.

## Napló és fejlődés

- Mentett edzések, sorozatok és tényleges eredmények, dátum szerinti áttekintés.
- Mentett eredményhez kapcsolódó **Új PR** jelvény; koppintással megnyitható és visszazárható korábbi → új rekordrészletek.
- Volumen, edzésszám, idő, kardióeredmények és becsült 1RM. A becsült 1RM nem külön igazolt maximumteszt.
- Izomcsoport-terhelési ábra: relatív edzésterhelést mutat, nem közvetlenül mért izomfáradtságot.
- Privát edzésfotók és helyi egészségnapló.

## Egészség és regeneráció

- Health Connect vagy közvetlen Samsung Health import a támogatott, ténylegesen megosztott adatokból.
- Dátumozott, forrással jelölt alvás-/pulzus-/aktivitási adatok; a TrainPilot Wear mérése elkülönül a szolgáltatói adatoktól.
- Mai hiteles Samsung Energy Score esetén az az elsődleges regenerációs alap; hiányában a TrainPilot saját becslése jelenik meg.
- Coach-ajánlások a rendelkezésre álló adatokkal és fájdalom-/terhelési korlátokkal. Hiányzó mérés helyett nem keletkezik kitalált egészségadat.
- Az elérhetőség készülék-, engedély- és szolgáltatófüggő. A Samsung nyilvános/production partner-hozzáférés ellenőrzése a [#214](https://github.com/fuloplevente1998/TrainPilot/issues/214) része.

## GPS és időalapú mozgás

- Opcionális telefonos vagy órás GPS, mért idő/távolság és privát útvonal; a térkép a telefonon jelenik meg.
- Indításkor látható, megjegyzett GPS-választás; rendszer- és helyengedély szükséges.
- A telefonon kifejezetten és sikeresen befejezett GPS-mérés külön sorozat-OK nélkül rögzíti a teljesítést. Hibás vagy implicit megállás nem teljesítés.
- Beltéri időmérés és munka/pihenő intervallum GPS nélkül is működik.
- A GPS-útvonal privát natív tárban marad, nem része a felhős mentésnek.

## Wear OS

- Tervezett és gyors edzés, önálló/offline folytatás és visszaigazolt telefon–óra átadás.
- Egy edzés és egy időzítő-tulajdonos; bizonytalan átadás szünetben marad, a visszakapcsolódás nem indít párhuzamos időzítőt.
- Támogatott Health Services-edzésmérés, pulzus/energia/lépés/távolság és opcionális GPS; az elérhető szenzoroktól és engedélyektől függően.
- Mini napló, naptár/programnapok, Tile és számlapi komplikáció.
- Korona/lünetta alapú szerkesztés, érintészár és folyamatos három másodperces feloldás, mentési/szinkronállapot és visszavonás.

## Mentés, szinkron és megjelenés

- Helyi JSON/ZIP-mentés és visszaállítás; opcionális Google Drive `appDataFolder` szinkron és Google Naptár-integráció.
- Programok, beállítások és napló összehangolása; konfliktus- és törlésvédelem. A Google-fiókhoz/OAuth-hoz szükséges külön beállítások továbbra is érvényesek.
- HU/EN/DE/RO nyelvek, több téma, nagyobb rendszerbetű és konfigurálható visszajelzések.
- Telefonra és órára külön APK, azonos verzióval. A [GitHub Release](releases/INDEX.md) elérhető; a Google Play-kiadás még előkészítés alatt.

## Forrás és ellenőrzés

Kanonikus felület: [app.js](../www/app.js) és [index.html](../www/index.html); natív telefon: [android/app](../android/app); óra: [android/wear](../android/wear), [Wear dokumentáció](WEAR_OS.md). Történeti és kiadási részletek: [változáslista](../RELEASE_NOTES.md), [1.3.71 jegyzet](releases/v1.3.71.md), [kapuk és APK-hashek](builds/2026-10-21-release-1371.json). A képernyőképek a funkció bemutatását szolgálják, és nem helyettesítik a natív készülékes próbát.
