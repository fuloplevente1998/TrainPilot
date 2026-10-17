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
