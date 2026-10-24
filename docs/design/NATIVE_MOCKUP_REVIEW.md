# Natív UI – döntés és látványterv-áttekintés (2026-10-24)

<!--
THESIS: A következő valódi edzéslépés és a folytatható session legyen a felület
azonnali fókusza; a generikus, adatot nem hordozó dashboard nem.
OWN-WORLD: Grafit háttér és rétegzett sötét kártyák, meleg arany az elsődleges
műveletekhez/kijelöléshez, visszafogott szegélyek, Material 3 és kompakt 12–16dp
sarkok; a felhasználó korábban mentett palettája továbbra is érvényes.
STORY: A sportoló látja a valódi tervet vagy aktív edzést, majd biztosan folytat,
indít, mér vagy naplóz; hiányzó szenzor-/egészségadat nem kap kitalált értéket.
FIRST VIEWPORT: Ötfüles alsó navigáció, közös felső sáv fogaskerékkel; a kezdőlapon
valós következő terv és elsődleges indítás/folytatás, utána mai adatok és gyors
indítás. Terv nélkül tanító üres állapot jelenik meg.
FORM: A felhasználó négy, 24 nézetes natív tervlapja (1/4–4/4) a kötelező
vizuális irány; új, alternatív mockup nem készül.
-->

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
az új hierarchiában. `4f60bc0`: az ötfüles navigáció és a globális fogaskerék
első bekötése elkészült. Programok/Naptár/Coach tördelődő gyorsgomb a Kezdőlapon
és az Edzés oldalon; a jelzősáv helyet foglal a bottom scaffoldban. Aktív edzésnél
ugyanaz az editor/backstack marad a Beállítások mögött. A gombfeliratok négy
nyelven szerepelnek. **Még nem fordított/nem futtatott kód**, a teljes mockup
vizuális megvalósítása és a valós navigációs/UI regresszió ellenőrzése hátra van.
A natív technológia önmagában nem igazol stabilitást vagy gyorsulást.

## Elkészült vizuális áttekintés

Négy képtábla, összesen 24 fő nézet, sötét/grafit/arany stílusban. A képek
az öt alsó füles alternatívát mutatják, AI-generált szemléltető látványtervek.
Nem alkalmazásképernyőfotók, nem UI-teszteredmények, nem funkcióígéretek.
Szövegek, értékek, ikonok és példák nem tekinthetők kész termékspecifikációnak.

### Frissített tervlapok – aktuális UI-referencia

A felhasználó a `feat/native-android-migration` ágon lévő
[`NATIVE_DESIGN_HANDOFF.md`](NATIVE_DESIGN_HANDOFF.md) dokumentumra és az ottani
négy újratervezett táblára irányította a figyelmet. Ezek a `36462aa` upstream
dokumentációs commitból változatlanul kerültek át; a commit alkalmazáskódot nem
tartalmazott. A frissített táblák a jelenlegi Compose-elrendezési referenciák;
az illusztratív szöveg, mintaadat és funkcióábrázolás nem írja felül a rögzített
működési szabályokat.

| Tervlap | Fájl |
| --- | --- |
| 1/4 – Kezdőlap és tervezés | [`mockups/01-main-pages-and-planning.png`](mockups/01-main-pages-and-planning.png) |
| 2/4 – Edzés és mérés | [`mockups/02-workouts-and-measurement.png`](mockups/02-workouts-and-measurement.png) |
| 3/4 – Napló, térkép és egészség | [`mockups/03-journal-maps-and-health.png`](mockups/03-journal-maps-and-health.png) |
| 4/4 – Beállítások és szerkesztés | [`mockups/04-editing-and-settings.png`](mockups/04-editing-and-settings.png) |

Az eredetileg megadott táblák `native-ui-board-1.png`–`-4.png` néven megmaradnak;
az új, sorszámozott táblák helyettesítik őket a további UI-igazítás vizuális
forrásaként. A képek manifestje a `mockups/MANIFEST.json` fájlban ellenőrizhető.

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

A felhasználó által megadott, javított képek a repositoryban is megőrzésre
kerültek, változtatás nélkül:

| Tervlap | Fájl |
| --- | --- |
| 1/4 – főoldalak és tervezés | [`mockups/native-ui-board-1.png`](mockups/native-ui-board-1.png) |
| 2/4 – edzés és mérés | [`mockups/native-ui-board-2.png`](mockups/native-ui-board-2.png) |
| 3/4 – napló, térkép, egészség | [`mockups/native-ui-board-3.png`](mockups/native-ui-board-3.png) |
| 4/4 – szerkesztés és beállítások | [`mockups/native-ui-board-4.png`](mockups/native-ui-board-4.png) |

Funkcionális referencia továbbra az elfogadott main; a választott natív
vizuális/navigációs irány ez a négy képtábla és az öt alsó fül.

A teljes main-funkciókör és az összevont natív elérési helyek megőrzési
követelményeit a [teljes dizájn- és funkcióátadó](NATIVE_DESIGN_HANDOFF.md)
A01–E20 leltára rögzíti. Ez nem megvalósítási vagy tesztbizonyíték: a kód,
adatforrás és ellenőrző teszt állapotát külön kell vezetni a
`docs/NATIVE_FINALIZATION_STATUS.md` fájlban.


## Kötelező Compose-bekötési feltételek – felhasználói pontosítás (2026-10-24)

Az alábbi hat pont explicit felhasználói követelmény. A mockup alapállapotot
szemléltet; nem írhatja felül a motor működését. **Most csak rögzítés történt,
sem új implementáció, sem tesztfuttatás.** A korábbi zöld motor-kapuk és a
jelenlegi, még nem futtatott UI-batch igazolása külön kezelendő.

### 1. Kezdőlap: félbehagyott vagy aktív edzés folytatása

Referencia: 1/4 • 1. Kezdőlap; `dd4cc27` draft-átvételi kapu.

- Importált draft esetén dinamikus „Félbehagyott edzés folytatása” kártya/gomb.
- Aktív vagy szüneteltetett natív session esetén aktív edzés folytatása legyen
  elérhető; ugyanaz a workoutId/generation és a rögzített sorozatok maradjanak.
- Az aktív session elsőbbséget élvez; egy régi importált draft vagy az alapállapot
  „Edzés indítása” gombja nem írhatja felül és nem hozhat létre második edzést.
- Importált draft átvétele a meglévő natív recovery útján, szünetben történik;
  a kártya megjelenése/alkalmazásindítás nem indít automatikusan GPS-t vagy stoppert.

Ellenőrzendő UI-állapotok: nincs draft/aktív session; csak importált draft;
futó natív; szüneteltetett natív; újranyitás és ismételt folytatás.

### 2. Pihenőkártya: Átugrás és ±15 mp

Referencia: 2/4 • 1. Súlyzós edzés; `e164ef3` motor, `NativeTimerTarget`.
A kapcsolódó teljes zöld kapu forrása `922b6b7`, részletek az átadóban.

- A szüneteltetés mellett valódi Átugrás/Kihagyás, −15 mp és +15 mp vezérlő
  legyen a kártyán vagy annak jól elérhető lenyitásában.
- Mindegyik a meglévő natív rest-parancsot hívja, aktuális workoutId/generation/
  timer-key célzással. Külön Compose-visszaszámláló motor nem készül.
- A tulajdonos-, állapot- és stale-target védelem megmarad; órás tükörből nem
  indítunk telefonos időzítőmutációt.

Ellenőrzendő: valódi szolgáltatás ideje változik, nem csak a kijelzés;
régi célpont elutasítása; órás tulajdonos; leállt/szüneteltetett pihenő.

### 3. Gyakorlatváltás aktív edzésben

Referencia: 2/4 • 1–3. képernyő; meglévő editor Előző/Következő műveletei.

- Előző/Következő nyíl, fejlécválasztó vagy egyértelmű lapozás tegye elérhetővé
  az összes gyakorlatot, egyoldali és kétoldali időmérésnél is.
- Az első/utolsó gyakorlat határa és az aktuális kiválasztás egyértelmű legyen.
- A motor kiválasztási műveletét használjuk; a váltás önmagában nem sorozat-OK,
  befejezés, új edzés vagy új mérés. A már rögzített értékek megmaradnak.
- A már elfogadott szabály szerint a pihenő gyakorlatváltáskor tovább számol és
  látható marad; ne tűnjön el a kiválasztott gyakorlattal együtt.

Ellenőrzendő: váltás futó pihenő alatt, korábbi mérés/sorozat megtartása,
első/utolsó gyakorlat és órás tulajdonos csak olvasható felülete.

### 4. „Mentés vázlatként” = szüneteltetés és kilépés

Referencia: 2/4 • 6. Edzés befejezése; meglévő tartós natív frame,
`leaveWorkout`/pause és külön discard/finalization műveletek.

- A „Mentés vázlatként” gomb a meglévő szüneteltetés és kilépés útját hívja,
  folytatható állapotban. A felirat lehet „Szüneteltetés és kilépés”, hogy a
  tényleges művelet egyértelmű legyen. Nem hozunk létre második draft-motort.
- Nem zárja le az edzést, nem hoz létre naplóbejegyzést, nem teljesíti a naptárt,
  nem küld „edzés mentve” lezárási nyugtát. A már tartós értékek megmaradnak.
- Legyen külön, egyértelmű Elvetés lehetőség (például hárompontos menüből), a
  meglévő discard-megerősítéssel és törlési/szinkronizációs szabályokkal.
- Tulajdonos- és finalization/restore-védelmek érvényesek; az órás tükröt nem
  szünetelteti egy telefonos gomb. A globális Beállítások megnyitása továbbra
  külön művelet, önmagában nem szüneteltetés/kilépés.

Ellenőrzendő: kilépés → újranyitás → ugyanazon session folytatása, változatlan
napló/naptár; külön elvetés után nincs feltámadás késői szinkronból.

### 5. Coach és Regeneráció: külön pontszámok, közös számítási eredmény

Referencia: 3/4 • 6. Coach; 1/4 • 1. Kezdőlap; #222;
`d134785` / `NativeCoachReadiness` az elfogadott rf245/tp184 szabályokkal.
A `d134785` és az új adapter/UI-batch tesztjei még nem futottak.

A Compose csak a számítás eredményét jelenítse meg; ne vezessen be saját
képletet, új súlyozást vagy képernyőnként eltérő alapértéket.

| Megjelenítés | Adat és jelentés |
| --- | --- |
| Coach nagy kör | `Result.score`: frissességet és terhelési/fájdalom korlátozásokat követő ajánlási pontszám |
| Samsung energia | `Result.samsungScore`, eredeti forrás/idő: hiteles mai Samsung-pontszám |
| TrainPilot becslés | `Result.trainpilotScore`: külön saját becslés, nem adódik a Samsunghoz |
| Kezdőlap/Egészség Regeneráció | Hiteles mai Samsung-adat esetén az eredeti `Result.samsungScore`; a Coach korlátozott értékét ne címkézzük Samsung-energiának |

- A forrás-hitelesség, mai dátum, 36 órás frissesség és terhelés/fájdalom
  döntéseit a meglévő kalkulátor/adatadapter érvényesíti, nem a UI.
- A 87 Samsung / 61 saját / 54 Coach hármas szemléltető példa: a különbség
  oka látható legyen (például terhelési/fájdalomkorlát); az értékeket ne rögzítsük
  konstansként. A Regeneráció ebben a hiteles Samsung-példában 87, a Coach 54.
- Samsung-adat hiányakor a meglévő fallback-policy és a saját eredmény szerinti
  forrásmegjelölés maradjon; hiányzó eredmény „—”/nincs adat, nem kitalált szám.

Ellenőrzendő: mai hiteles Samsung, régi/jövőbeli/hibás forrás, korlátozott Coach,
csak saját becslés, minden adat hiányzik; azonos bemenetekkel egyező kijelzés.

### 6. Kültéri mérés: csak valódi, friss pulzus

Referencia: 2/4 • 4. Kültéri mérés; Galaxy Watch7 Health Services vagy
BLE pulzusforrás (Slice 4/A).

- A „148 bpm” kizárólag illusztráció. Élő pulzus csak ténylegesen érkező,
  érvényes mérésből jelenhet meg, megfelelő forrás- és frissességkezeléssel.
- Nincs csatlakoztatott/adatot szolgáltató forrás, elavult vagy érvénytelen
  minta esetén „— bpm”; nincs szimulált alapérték vagy történeti mérés élőként.
- A forrás megszakadása után a korábbi érték ne maradjon korlátlanul élőként
  a kártyán; visszakapcsolódáskor az új valódi minta frissíti a kijelzést.
- Pulzus hiánya önmagában nem állítja le a GPS-t, a stoppert vagy az edzést.

Ellenőrzendő: friss Watch/BLE minta, nincs forrás, kapcsolat megszakad,
elavult/érvénytelen minta és újracsatlakozás, működő GPS pulzus nélkül.
