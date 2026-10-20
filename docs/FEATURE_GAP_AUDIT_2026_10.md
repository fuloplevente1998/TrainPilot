# TrainPilot – újfunkció- és átfedésaudit

Dátum: **2026-10-20**. Vizsgált main: `f309137c461c6cb3b39497ab46a0e764a7e7b077`. Verzió: **1.3.6 /2733**.

Kérés: „Keszitsunk egy új auditot, melyek azok az uj funkciok, amelyek meg nincsenek bent? PR jelveny? Stb”. A vizsgálat a [korábban rögzített öt ötletet](plans/UI_TEST_BRANCH_BACKLOG.md) bontja valóban hiányzó képességekre és meglévő funkciók finomításaira. Külön jelzi a RIR adatainak jelenlegi részletességét. A PR-jelvényben a PR **személyes rekordot / egyéni csúcsot** jelent.

## 1. Eredmény röviden

**Négy konkrét új bővítés jelölt:** edzéshez kötött PR-jelvény; sikeresen rögzített kézi sorozat rövid haptikus visszajelzése; kifejezett sorozatmásolás/ismétlés-előtöltés; külön RIR minden sorozathoz.

**Már létezik:** PR-felismerés, PR-darabszám és korábbi → új rekordlista; becsült 1RM-kijelzés; pihenő/intervallum rezgés és hang támogatása; új sorozat súlyának előtöltése; öt fokozatú edzésértékelés külön fájdalomválasszal; gyakorlatonkénti tényleges RIR.

A dupla rezgésminta és az egyszerűsített háromválaszos értékelő ezek meglévő funkcióknak a finomítása lehet. Nem szabad a pihenőjelzést, az 1RM-et vagy az értékelést teljesen hiányzóként újrafejleszteni.

## 2. Mit igazol ez az audit?

A teljes kanonikus `www/app.js`, a `www/index.html`, a releváns natív visszajelzés-/időzítőkód, Wear képernyők és meglévő tesztek forrását néztem át. A nagy app.js fájl a Git blob API-ból lett kiolvasva; blobazonosítója `fa65c26e9e4701367f6842e923ea298569e49677`.

- **Megvan a forrásban:** létező adatmodell, hívás és felületi megjelenítés található. Ez önmagában nem új készülékes működésigazolás.
- **Részben megvan:** az alap képesség létezik, a konkrét javasolt változat külön bővítés.
- **Új javaslat / nincs bekötve:** az átnézett adatútban és felületben nem található meg a meghatározott végponttól végpontig működés. Nem pusztán egy elnevezésre végzett keresés eredménye.
- **A:** meglévő funkció javítása/egyszerűsítése. **B:** új felhasználói képesség, külön jövőbeli fejlesztési javaslat.

Új fizikai telefonos/órás próbát, manuális Chromium-futtatást vagy új APK-t ez az audit nem állít. A tesztfájlokra mutató hivatkozások meglévő ellenőrzéseket azonosítanak, nem újonnan lefuttatott teszteket. A #224 auditdokumentum önmagában nem bizonyít alkalmazásjavítást.

## 3. Mi van már bent, és mi hiányzik pontosan?

| Terület | Jelenlegi forrás | A hiányzó vagy új rész | Besorolás |
| --- | --- | --- | --- |
| PR-felismerés | A Fejlődés modell összehasonlítható súly-, azonos súlyon ismétlés-, testsúlyos ismétlés-, idő- és kétoldalas rekordokat képez. | A rekordfelismerést nem kell újraírni. | Megvan |
| PR-kijelzés | PR-metrikakártya, rekordszám és teljes, dátumozott korábbi → új lista. | Az adott mentett edzés/sorozat mellett megjelenő „Új PR” jelvény nincs bekötve. | B – NF-001 |
| Becsült 1RM | Megvan a statisztikában, `w * (1 + r / 30)` becsléssel. | A jelenlegi PR-típusok nem tartalmaznak külön e1RM-rekordot; ilyet csak külön szabállyal lehet hozzáadni. | Megvan; e1RM-PR külön döntés |
| Sorozat OK visszajelzés | Kész állapot, pihenőindítás, visszavonás és mentési/szinkron adatút létezik. Van natív rezgésprimitív is. | A sikeresen rögzített kézi sorozathoz külön, rövid, egyszeri haptikus jelzés nincs bekötve a vizsgált rögzítési adatútban. | B – NF-002 |
| Pihenő/intervallum jelzés | Hang-/rezgéskapcsolók, utolsó 3 mp rövid rezgése és fázisváltási/lejárati rezgés létezik. | A korábbi ötlet szerinti választható dupla lejárati minta külön finomítás lenne. | A – EF-001 |
| Új sorozat súlya | Az új aktív sorozat az előző súlyát kapja; az ismétlés üres, `done:false`. | Kifejezett sorozatmásolás vagy az előző ismétlés kézi előtöltése nincs ebben a vezérlőben. | B – NF-003 |
| Edzésértékelés | Öt erőfeszítési fokozat, külön fájdalomválasz és kihagyás; telefon/óra adatút. | Választható háromgombos felület külön UX-egyszerűsítés; nem új értékelési képesség. | A – EF-002 |
| Tényleges RIR | Opcionális egész 0–10 érték `exercise.actualRir` mezőben, RIR/Coach módban, telefonos tulajdon mellett, teljesített sorozat után. | Minden sorozat saját RIR-értéke nincs megvalósítva. | B – NF-004 |

### Pontos forráshivatkozások

- [PR-modell: összehasonlítás és rekordképzés](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L13689), [rekordtípusok](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L13730), [PR-metrikakártya](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L14058), [teljes korábbi → új lista](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L14248).
- [PR-modell meglévő tesztje](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/tests/progress-177-model.cjs#L38): első alapérték nem PR, csak teljesített edzések, összehasonlítható terhelés és kétoldalas idő.
- [Becsült 1RM számítása](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L2588), [jelenlegi statisztikai megjelenítése](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/index.html#L1399).
- [Kézi sorozatrögzítés és visszavonási adatút](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L19089), [meglévő általános mentési toast](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L11015), [natív fix 90 ms jelzésprimitív](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/android/app/src/main/java/com/repforge/app/AppFeedbackPlugin.java#L31). A toast és az időzítő rezgése nem jelent külön sorozat-OK haptikát.
- [Webes pihenő/fázis haptika](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L18314), [natív haptika](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/android/app/src/main/java/com/repforge/app/IntervalHapticCues.java#L4), [meglévő 3–2–1/lejárat teszt](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/android/app/src/test/java/com/repforge/app/IntervalHapticCuesTest.java#L8), [hang-/rezgésbeállítások](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L15492).
- [Sorozathozadás és előző súly előtöltése](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/index.html#L1775).
- [Öt erőfeszítési érték](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L3191), [telefon/óra értékelés mentése](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L16590), [öt fokozat + külön fájdalom UI-teszt](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/tests/browser/post-workout-feedback-122.cjs#L24).
- [RIR jogosultsági feltételek](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L19860), [gyakorlatonkénti mentés](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L19874), [egyetlen gyakorlatoldali RIR-mező](https://github.com/fuloplevente1998/TrainPilot/blob/f309137c461c6cb3b39497ab46a0e764a7e7b077/www/app.js#L19905).

## 4. JÖVŐBELI FEJLESZTÉSI JAVASLAT – valóban új képességek

Az alábbi tételek státusza **javaslat, nincs implementálva ebben a munkában**. A rangsor egyeztethető terméksorrend, nem feltárt P0/P1 hibák listája.

### NF-001 – „Új PR” jelvény az adott edzés eredménye mellett

**Már létező alap:** `TrainPilot177Model.prepare(...)` edzésenkénti `pr` listája és a PR-statisztika.

**Új rész:** diszkrét jelvény a konkrét mentett edzés összegzésében/Napló-kártyáján, megnyitva a rekord típusa, korábbi és új értéke. Az összes rekordot listázó panel nem azonos ezzel a jelvénnyel.

**Javasolt első változat:** csak sikeresen mentett, teljesített edzéshez kötött kijelzés. Edzés közbeni élő ünneplés/animáció későbbi bővítés; egyetlen aktív sorozat alapján még ne állítsunk végleges, tartós új csúcsot.

**Megőrzendő szabályok:** első alapérték nem új PR; eltérő gyakorlat, súlyértelmezés, idő és ismétlés nem mosható össze. Azonos súlyon elért ismétlésrekord különbözik a maximális súlytól. Az e1RM-rekord új típust és külön elfogadást igényel. Törlés, szerkesztés, visszavonás és késői szinkron után a kijelzés igazodjon a kanonikus naplóhoz; újratöltés ne ünnepelje újra ugyanazt.

**Igazolás:** korábbi/új/egyenlő/első eredmény; hiányos/draft sorozat; azonos terhelésű ismétlés; kétoldalas idő; szerkesztés/törlés; telefon–óra ismételt szinkron. UI: HU/EN, 320–412 px, nagy betű, jelvény szövegesen is érthető. A jelvény ne ösztönözzön fájdalomkor a Coach-védelem megkerülésére.

### NF-002 – Rövid haptika sikeres kézi sorozatrögzítéskor

**Már létező alap:** natív rezgésprimitív, pihenő/intervallum jelzések, stabil sorozatazonosítók és visszavonási bizonylat.

**Új rész:** elkülönített visszajelzés az explicit sorozat-OK sikeres, tartós elfogadásakor; opcionálisan kikapcsolható. A 30–40 ms korábbi prototípusötlet, nem mért készülékes követelmény; a mai natív `signal()` fix 90 ms.

**Határ:** nem minden gombnyomásra és nem általános „Mentve” toastokra. Hibás adat, sikertelen mentés, ismételt parancs/nyugta és kész állapot visszavonása ne adjon új sikerjelzést. Telefon/óra tulajdon esetén ne jelezzen egyszerre mindkét eszköz; az automata pihenő jelzéseivel ne fedje egymást indokolatlanul.

**Igazolás:** siker/hiba/dupla koppintás/dupla nyugta, távoli tulajdon, némítás/rezgéskikapcsolás, háttér és valódi készülék. A meglévő időzítő haptikus tesztje nem igazolja ezt az új eseménykötést.

### NF-003 – Kifejezett sorozatmásolás vagy ismétlés-előtöltés

**Már létező alap:** „Sorozat hozzáadása” már átveszi az előző súlyt. A hiányzó rész nem a súly-előtöltés újbóli elkészítése.

**Új rész:** külön felhasználói művelet egy korábbi, kiválasztott sorozat terhelésének és ismétlésének új sorba másolására. A „másolás” és az alapból minden új sorra történő automatikus ismétlés-előtöltés két külön termékdöntés.

**Határ:** új setId, `done:false`, nincs automatikus teljesítés. A másolt ismétlés még nem újonnan teljesített adat; saját RIR, időmérés, GPS-nyomvonal, időzítő-, mérési- és szinkronazonosító nem öröklődhet. Távolság/időalapú gyakorlatot ne alakítson súlyzós sorozattá. Már beírt adatot ne írjon át némán.

**Igazolás:** üres/kitöltött sor, korlátok és sorozatlétszám, kétoldalas gyakorlat, draft/reload, Undo, stabil setId és órás szinkron.

### NF-004 – Tényleges RIR külön-külön minden sorozathoz

**Már létező alap:** `exercise.actualRir` és a gyakorlat eredményére épülő RIR/Coach ajánlás.

**Új rész:** sorozatonkénti RIR-adatmodell, soronkénti bevitel és naplókijelzés, valamint meghatározott szabály a sorozatokból képzett gyakorlat-/Coach-jelre. Ez jelentős adatmodell-bővítés, nem a már meglevő mező egyszerű áthelyezése.

**Határ:** a gyakorlat korábbi RIR-jét nem szabad kitalált mérésekként minden sorozatra másolni. A nulla érvényes; a hiány nem nulla. A cél-RIR nem tényleges RIR. A meglévő telefonos tulajdon-, GPS/időalapú és visszaállítási védelmek változatlan alapkövetelmények. Wear-bevitel nem következik automatikusan ebből: külön felület- és protokolltervezés kell.

**Igazolás:** régi mentések migrációja adatvesztés nélkül; 0/hiány/hibás érték; sorozatmásolás/törlés/Undo; reload és backup; napló → következő Coach-ajánlás; érintett órás protokoll.

**Dokumentációs helyesbítés:** az angol és magyar README a sorozatonkénti RIR-t pontatlanul állította. Ebben a dokumentációs csomagban a szöveg a tényleges, gyakorlatonkénti működésre javul. Ez nem alkalmazáskód-javítás.

## 5. Meglévő képességek finomítása – nem új alapfunkció

### EF-001 – Dupla lejárati rezgés / jelzésminta

A pihenő/intervallum hang- és rezgésjelzés már megvan; a 3–2–1 rövid és lejárati/fázisváltási jelzések is. A dupla minta vagy külön választható profil új változat lenne. Ez meglévő funkció (A) bővítése, nem hiányzó időzítő.

Előbb tisztázandó: a dupla minta lecseréli-e a már elfogadott jelzést, vagy külön opció. Nem indíthat párhuzamos telefonos/órás jelzést; háttér, szünet, tulajdonosváltás és megszakítás is tesztelendő. A meglévő jelzést indok nélkül nem írjuk át.

### EF-002 – Egyszerűbb, háromválaszos nehézségfelület

Most öt erőfeszítési fokozat + külön fájdalomválasz + kihagyás létezik. A háromgombos nézet meglévő értékelés UX-egyszerűsítése (A). Külön fájdalomjelzés és a kihagyás maradjon elérhető; a régi öt érték ne olvadjon össze az adatokban.

A valódi RPE 1–10 skála külön fogalom és külön adatmodell-/Coach-döntés lenne. Sem az öt fokozat, sem három gyorsgomb, sem a RIR nem nevezhető át automatikusan RPE-vé.

## 6. Javasolt kis feladatok és sorrend

| Sorrend | Feladat | Elkülönített eredmény |
| --- | --- | --- |
| 1 | NF-001: PR-jelvény a mentett eredménynél | Meglévő PR-modellre épített, visszavonás/szinkron után is igaz kijelzés. |
| 2 | NF-002: sikeres kézi sorozat haptikája | Csak a tényleges rögzítési sikerhez kötött, kapcsolható jelzés. |
| 3 | NF-003: kifejezett sorozatmásolás | Új sorozat, előtöltött értékek, kézi teljesítés és saját azonosító. |
| 4 | NF-004: sorozatonkénti RIR | Külön adatmodell-/migrációs/protokollterv az implementáció előtt. |

EF-001 és EF-002 csak akkor kerüljenek előre, ha a felhasználó az eltérő jelzésmintát vagy egyszerűbb bevitelt kéri. Az audit nem módosítja automatikusan a már elfogadott működést.

Mindegyik megvalósítás: külön, kis fejlesztési scope, célzott regresszió, a változásnak megfelelő GitHub Actions-kapu és szükség esetén készülékes elfogadás. Nincs automatikus új funkcióimplementáció, main merge, verzióemelés vagy Play-kiadás.

## 7. Követés és aktuális állapot

- Ez forrásalapú újfunkció-audit, nem a teljes alkalmazás UX/hibaauditjának lezárása.
- A #224 korábbi jelentése változatlanul külön Draft munka; annak „Javítva” státuszai ebből nem kapnak utólag igazolást.
- Nincs új Issue nyitva a már rögzített ötletekre; ez az audit a meglévő backloghoz kapcsolódik.
- A Samsung Energy Score, a gyakorlatonkénti RIR, a progressziós módok és az Undo már meglévő funkciók. A Samsung production terjesztés a #214 feladata, nem új felhasználói pontszámfunkció.
- Jelen csomagban csak dokumentáció és a két README RIR-pontosítása készül; az alkalmazáskód és az 1.3.6 /2733 verzió változatlan.

Kapcsolódó források: [korábbi ötletek](plans/UI_TEST_BRANCH_BACKLOG.md), [aktuális átadás](WORK_HANDOFF.md), [#224](https://github.com/fuloplevente1998/TrainPilot/pull/224), [#214](https://github.com/fuloplevente1998/TrainPilot/issues/214).
