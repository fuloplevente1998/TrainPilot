# Natív UI – döntés és látványterv-áttekintés (2026-10-24)

## Felhasználói döntés

A felhasználó a mockup megtekintése után **az öt alsó füles változatot
választotta**. Ez felülírja a korábbi nyolcgombos main-menü választását.
Vizuális irány: a bemutatott sötét/grafit/arany dizájn, a meglévő funkciók
megőrzésével. Az öt fül: **Kezdőlap, Edzés, Napló, Fejlődés, Egészség**.

**Beállítások:** jobb felső fogaskerék, minden főoldalon és az aktív edzés
felületén elérhető, akadálymentes címkével és legalább 48dp érintési területtel.
A fogaskerék a meglévő Beállítások oldalra vezet, amelyből vissza ugyanarra
az oldalra/edzésre jutunk. Megnyitása önmagában nem félbehagyás vagy pause,
nem indít új sessiont; a mérés tovább fut. Lezárás és folyamatban lévő védett
művelet közben a meglévő zárolásokat meg kell tartani. Beágyazott dialógusra
nem szükséges új fogaskereket zsúfolni.

A Programok, Naptár, Coach és minden korábbi funkció elérését meg kell tartani
az új hierarchiában; ezek pontos belépési pontjait implementáláskor és UI-próbán
ellenőrizni kell. A döntés rögzítve, **a jelenlegi forrás még a nyolcgombos
első szerkezetet tartalmazza**. Ez a dokumentációs módosítás nem UI-implementáció.
A natív technológia önmagában nem igazol stabilitást vagy gyorsulást.

## Elkészült vizuális áttekintés

Négy képtábla, összesen 24 fő nézet, sötét/grafit/arany stílusban. A képek
az öt alsó füles alternatívát mutatják, AI-generált szemléltető látványtervek.
Nem alkalmazásképernyőfotók, nem UI-teszteredmények, nem funkcióígéretek.
Szövegek, értékek, ikonok és példák nem tekinthetők kész termékspecifikációnak.

1. Főoldalak és tervezés: Kezdőlap, Edzés, Programok, Naptár, Gyakorlatok,
   Gyakorlat részletei.
2. Edzés és mérés: súlyzós edzés, egyoldali időmérés, kétoldali időmérés,
   kültéri GPS-mérés, intervallumbeállítás, befejezés/visszajelzés/mentés.
3. Napló/térkép/egészség: naplólista, edzésrészletek, piros X-es útvonaltérkép,
   fejlődés, egészség, Coach.
4. Szerkesztés/beállítások: Beállítások, Óra és szinkron, mentés/visszaállítás,
   engedélyek, edzésfotók, programszerkesztés.

Az 1/4 lapon Galaxy Watch7-re javított óracímke; a 2/4 lapon undo/mérési állapot,
csak egyoldali automatikus intervallum és mentésre váró állapot pontosítva;
a 4/4 lapon nem létező e-mail/verzió mintafelirata eltávolítva. A javított képek
az érvényes review-példányok. A ritka hibaállapotok, összes dialógus és minden
nyelvi változat nem szerepel ezen a 24 nézeten.

A képek a beszélgetésben letölthetők; nem kerültek binárisként a repositoryba.
Másik modellnek történő átadáskor a képeket külön is csatolni kell, ha a vizuális
terv alapján kell dolgoznia. Funkcionális referencia továbbra az elfogadott main;
a választott vizuális/navigációs irány az öt alsó füles mockup.
