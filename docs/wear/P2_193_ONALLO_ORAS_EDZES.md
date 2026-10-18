# P2 – Önálló órás edzés (#193 + #202)

## Jóváhagyott munkarend

2026-10-17: a felhasználó elfogadta az összevonást és a folytatást. A #193 a
közös issue; #202 teljes követelményei átvezetve, a külön jegy duplikátumként
lezárva. A #192 és #211 sztornózott, kódjuk nem része a mainnek vagy ennek a
fejlesztésnek. Alap: main `91889326056ac257975360644ead39ded1b5a4d6`.
Verzió: változatlan **1.3.51 /2732**.

Sorrend: **#193 órás önállóság → #212 UI-csomag → #197 edzésmódok → #184
Samsung Coach**. A #212 átveszi #190 korona/lünetta követelményeit; #190
duplikátumként lezárva. #206 a #207 követelményeit is követi; #207 lezárva.
A #208-cal már mainre került GPS STOP-teljesítést nem implementáljuk újra.
A #206 nyitott marad a külön aktivitásnézet és maradék UI-pontok egyeztetésére.
A régi #186 draft beolvasztás nélkül lezárva: a #195 mainen levő, elfogadott
megvalósítása kiváltotta. A #121 karbantartás/Play-publikálás külön marad.

## Közös működés

1. Az órán indított edzés meglévő önálló működése megmarad. Telefonon indított
   aktív vagy szüneteltetett edzéshez tudatos **Edzés átvétele** művelet tartozik.
2. Egy kanonikus workoutId és növekvő ownerEpoch; a Data Layer üzenetnek az
   átadási azonosítót, a két eszközt és az alap-epochot is egyeztetnie kell.
3. Az átadó először tartósan befagyasztja saját vezérlését, időzítőjét és
   GPS-forrását; utána ad ajánlatot. Az átvevő az ellenőrzött pillanatképet
   tartósan elmenti, és csak végleges tulajdonosi visszaigazolás után válik
   tulajdonossá. A folytatás kifejezett felhasználói művelet.
4. Elveszett üzenet/nyugta, újraindítás és újraküldés esetén nincs új edzés,
   dupla időzítő vagy néma visszavétel. Bizonytalan átadás szünetben marad,
   ugyanazzal az átadási azonosítóval újrapróbálható. Telefonkapcsolat nélkül
   csak már véglegesen átvett edzés folytatható.
5. A visszaadás külön, visszaigazolt átadás; telefonos újracsatlakozás önmagában
   nem változtat tulajdonost. A korábbi explicit telefonos szünet megmarad.
6. Az órás GPS külön opt-in, a helyengedélytől és a központi helyszolgáltatástól
   függ. A koordináták saját Location-fixekből származnak, nem a Health Services
   összesített távolságából. GPS-hiba nem állítja le a stoppert/edzésrögzítést.
7. Csak az egyeztetett forrás rögzít. Szünet, forrásváltás, jelkimaradás,
   újraindítás és elutasított ugró fix új útvonalszakaszt jelent; nincs hamis
   átkötés. A pontok időt és pontosságot is tartalmaznak.
8. Méretkorlátos privát órás tároló, azonosított és visszaigazolt útvonalcsomagok.
   A telefon a meglévő #163 natív route-tárba importál; a napló, offline térkép,
   mentés/visszaállítás és törlés ugyanazt a rendszert használja.

## Megvalósítás és ellenőrzés

Fejlesztés: [#213 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/213).

Első fejlesztési rész: a mindkét Android-modul által fordított közös
`WorkoutOwnership193` állapotgép és JVM-regressziói. Ez az alap még nincs
rákötve a natív tárolókra, a Data Layerre vagy a felhasználói műveletekre;
önmagában nem teszi elérhetővé az edzésátadást vagy az órás útvonalrögzítést.
A teljes integráció és készülékes elfogadás követelményei az alábbiak.

Második rész: atomi fájlba írt átadási napló és pontos újraküldési boríték,
eszközhöz kötött Data Layer fogadóút, telefonos és órás natív adapterek,
epoch-ellenőrzés a parancsoknál és pillanatképeknél. A napló írása és a
pillanatkép telepítése közti megszakadás külön helyreállítható. A régi epoch
nem indíthatja újra a telefon időzítőjét; a késői commit/accept nem törölhet
újabb órás vagy telefonra már visszaérkezett adatot. A törlés leállítja az
átadási újraküldést. Tárolási hiba nem számít nyugtázható, elavult parancsnak.

**A képességet a normál telefonos pillanatképek még nem hirdetik, és a felület
még nem kínál átvételi gombot.** A friss kanonikus telefonos piszkozat
szüneteltetése, JS-egyeztetés, időzítő-folytatás, felhasználói vezérlők és a
GPS-rész bekötése következő integrációs lépés. A második rész sem készülékes
funkciótesztre szánt kiadás; új main/kiadás nincs.

Harmadik rész (2026-10-18): a telefonos JS-piszkozat natív átadási naplóval
egyeztetése bekerült a közzététel és parancsfeldolgozás elé. Az import egy
tartós piszkozatmentéssel őrzi a sorozatazonosítókat és a helyi megjegyzéseket;
ismételt commit nem írhatja vissza a befagyasztott értékeket az újabb adatokra.
A forrás előbb egyeztet, szüneteltet, majd a szünet hatásait is egyezteti.
Szerkesztő vagy háttérben levő WebView nem indít átadást. A telefonos régi
natív végrehajtás nem egyeztethet vissza a már átadott JS-edzésbe.

A telefon natív időzítőjének teljes, ellenőrzött checkpointja megőrzi a tervet,
körszámot, kezdő pihenőt, pozíciót, baseline-t és mért munka/pihenő/szünet
számlálókat. A kihagyott pihenő nem válik mért idővé. Függő rögzítési esemény,
eltérő sorozatérték, futó/megszakadt óra és hibás számláló blokkolja az ajánlatot.
Az órás típusos tároló is megőrzi ezt a checkpointot, de annak végrehajtója és
a visszaadott telefonos végrehajtás újraindítása külön integrációs lépés.
**A képesség továbbra sincs meghirdetve, új felhasználói gomb/GPS-funkció nincs.**

Az órás checkpoint-végrehajtó ezt követően bekerült a meglévő órás időzítőbe.
A telefon és az óra egyetlen közös `IntervalTimeline193` fázisszámítást használ;
a telefon korábbi osztályneve kompatibilitási wrapper. Az átvevő saját monoton
órával, kifejezett folytatás után számol. A forrás munka/pihenő/szünet naplója
megmarad, az új tulajdonos külön naplókulcsra gyűjt. A baseline, körök, vezető
pihenő, kihagyott pihenő és tört másodperc megmarad. Boot-váltás vagy hibás
terv megszakított szünetet eredményez. A telefonos visszafolytatás és az összes
vezérlő bekötése továbbra is szükséges az elérhetőség bekapcsolása előtt.

- [ ] Tartós tulajdonosi állapotgép és stale/dupla/ACK-vesztés regressziók.
- [ ] Natív telefon–Wear átadás és visszaadás; régi alkalmazás képességvizsgálata.
- [ ] Aktív/szüneteltetett kézi és automata edzés átvitele, munka/pihenő idő megőrzése.
- [ ] Órás Location-rögzítés, szakaszhatárok, tárhelyhiba és engedélyvesztés.
- [ ] Útvonal-outbox, telefonos import, duplikációvédelem és törlési védelem.
- [ ] Telefonos és kis kerek órás UI, nagy betűméret, támogatott nyelvek.
- [ ] Egyazon commiton Quick, Performance, 6/6 Chromium, natív Phone/Wear és kerek UI.
- [ ] Actionsből aláírt, közös forrású APK-pár, hash és készülékes próbalap.
- [ ] Készülékes elfogadás, majd külön main/kiadási döntés.

Teljes teszt és APK-build GitHub Actionsön. Helyben célzott kódellenőrzés;
korábbi commit zöld futása nem igazolja a legújabb változatot.
