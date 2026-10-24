# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Feltételezett elsődleges felhasználók: edzéstervet követő vagy saját edzést
összeállító sportolók, akik súlyzós, időalapú és szabadtéri edzést rögzítenek
telefonon, esetenként Wear OS órával. A pontos célcsoport nincs külön
kutatással igazolva.

## Product Purpose

A TrainPilot natív telefonos és Wear OS alkalmazása segít edzést tervezni,
elindítani, végrehajtani, folytatni, naplózni és áttekinteni. Sikeres használat
esetén az edzés és a rögzített adatok eszközváltás, megszakítás vagy alkalmazás-
frissítés után is megmaradnak.

## Positioning

A termék a strukturált erőedzést, idő-/GPS-mérést, edzésnaplót és telefon–óra
folyamatot egyetlen tartós edzésállapotban kapcsolja össze. Ez a pozicionálás a
repositoryban rögzített működési követelményekből következik; külön piaci
összehasonlítás nem történt.

## Operating Context

A telefon az edzésterv, szerkesztés, rögzítés és naplózás elsődleges felülete.
A Wear OS óra kiegészítő megjelenítő és jóváhagyott edzésvezérlő; tulajdonosváltás
visszaigazolást igényel. Az alkalmazás edzés közben, mozgásban és időnként
hálózat nélkül is használható. A Beállítások megnyitása nem szakíthatja meg
önmagában az aktív mérést.

## Capabilities and Constraints

- A funkciók közé tartozik az edzéstervek, gyakorlatok, sorozatok, RIR, aktív és
  gyors edzések, beltéri időmérés, intervallumok, GPS-útvonalak, edzésnapló,
  naptár, fejlődési adatok, Health Connect/Samsung Health, mentés és
  visszaállítás, valamint Wear OS kapcsolat.
- A phone–Wear kapcsolat egy kanonikus edzést, stabil workout/set azonosítókat
  és egy időzítő-tulajdonost használ. Bizonytalan átadás szünetben marad.
- Csak tényleges, friss egészségügyi/szenzoradat jeleníthető meg élő adatként;
  hiányzó pulzus értéke „— bpm”. Mintaérték és AI-látványterv nem termékadat.
- Az `applicationId` `com.repforge.app`; a meglévő kiadási aláírás, verziófolytonosság
  és a felhasználói adatok megőrzése kötelező. A régi main felület WebView-ként
  nem állítható vissza; a main a működési referencia.
- Támogatott nyelvek: magyar, angol, német és román. A fizikai telefon-/óra-
  viselkedés külön ellenőrzendő; CI nem helyettesíti a készülékes elfogadást.

## Brand Commitments

- A terméknév TrainPilot és az alkalmazásazonosító változatlan.
- A natív app külön vizuális rendszerű a main alkalmazástól; a felhasználó által
  megadott négy natív képtábla a vizuális referencia.
- A rögzített fő navigáció: Kezdőlap, Edzés, Napló, Fejlődés, Egészség öt alsó
  célponttal; Beállítások jobb felső fogaskerékről érhető el.
- A részletes natív vizuális döntéseket és a hat kötelező Compose-működési
  feltételt a `docs/design/NATIVE_MOCKUP_REVIEW.md` tartalmazza.

## Evidence on Hand

A felhasználó 2026-10-24-én négy, egyenként hat natív képernyőt bemutató
képtáblát csatolt (1/4–4/4); változatlan példányaik a
`docs/design/mockups/native-ui-board-1.png`–`native-ui-board-4.png` fájlok.
Ezek AI-generált látványtervek: elrendezés- és stílusreferenciák, nem működő
képernyőfotók, teszteredmények vagy valódi felhasználói/egészségügyi adatok.
A `docs/screenshots` képei a régi main appot mutatják, és nem a natív app
dizájnreferenciái.

## Product Principles

- Az edzés és felhasználói adatok tartósak; hiba nem mutatható sikeres mentésnek.
- A valós működést nem helyettesíti látványterv, szimulált adat vagy pusztán
  létező forráskód.
- A telefon, óra, mentés és visszaállítás ugyanazt az edzés- és adatállapotot
  őrzi meg.
- A fő műveletek edzés közben egyértelműek és könnyen elérhetők.

## Accessibility & Inclusion

Az Android natív felület Material komponenseket, rendszer-visszalépést,
legalább 48 dp-es érintési célokat és skálázható `sp` tipográfiát használjon.
A négy támogatott nyelv, a nagyobb betűméret és a kompakt telefonképernyő is
használható maradjon.
