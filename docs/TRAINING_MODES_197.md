# #197 – Edzésstílusok és progressziós módok: kiinduló audit és adatmodell-terv

Állapot: **6/7 – közös Coach-integráció megvalósítva; teljes regresszió és készülékes ellenőrzés folyamatban (2026-10-20)**. A GitHub Actions teljes ellenőrzésének eredménye ettől külön követendő. Ez a dokumentum a `feat/197-training-styles-progression` fejlesztési ághoz tartozik; nem jelent kész, bekapcsolt funkciót vagy kiadási jóváhagyást.

Kiinduló main: `1393982d48691489978a44652c61ff2b32514e8d` (#220). A meglévő #128 edzéstervezés/progresszió, a #206 GPS-tevékenységek és a Wear/napló megőrzése kötelező.

## Feltérképezett belépési pontok

| Felelősség | Aktuális implementáció |
| --- | --- |
| Globális beállítások | `settings() → db.get('settings',{rest:90})` (`www/app.js`), a mentések többnyire az aktuális settings objektumot terjesztik ki |
| Programok / aktív program | `programs()`, `programById()`, `activeProgram()`; az alap sablonprogramok tárolás nélkül is előállhatnak |
| Programrecept | `program.prescriptions[exerciseId]`: `sets`, `reps`, `rest`, `rir`, `weight`, `trial`, `variation`, `loadType` |
| Programgenerálás | `generatePersonalProgram()`, `tp128Rx()`, `tp128AdaptHistory()`, a #128-as explicit `programPreview` / elfogadási folyamat |
| Progressziós kiértékelés | `tp140ParseTarget()`, `tp140Prescription()`, `tp140Step()`, `tp140NextRepTargets()`, `rf152Recommendation()` |
| Kompatibilis előzmények | `tp128Rows()`: max. 4, programhoz és elfogadott `history128` forrásokhoz kötött; a #128 felülírja az alap `rf152Recommendation()` függvényt |
| Terhelés tényleges előkészítése | `rf152ApplySuggestions()`: a `rf152Recommendation` `autoApply=true` javaslatát a futó session sorozatsúlyára átírja és `persistDraft()` menti |
| Coach/edzés UI | `rf233ExerciseAdvice()`, `tp155CoachShortAdvice()`, `tp155InjectWorkoutCoach()`; a beállítható súlylépcsők az általános Beállítások és a programoldali `tp1511TrainingPanel()` részei |
| Meglévő védelem | #128: fájdalom és alacsony readiness blokkol; 3 stabil azonos súlyú felső-cél teljesítés alapján progresszió; hiányzó/idegen előzmény nem használható |

## Felismert kompatibilitási kockázat

**A mai, régi mód stabil 3 alkalom után tud `autoApply=true` súlyjavaslatot adni.** Ha a #197 ezt globálisan lecseréli vagy egy módcímke alapján csendben megváltoztatja, eltérne a meglévő edzések viselkedése. Az új HIT/lineáris/RIR módoknál a javaslat először *csak előnézet* legyen; a döntés elfogadásáig nem írhatja át sem a programreceptet, sem az aktív edzés sorozatait. Az új logika nem módosíthatja a mentett naplót, GPS/distance gyakorlatokat vagy a Wear tulajdonosi/szinkronadatokat.

## Javasolt, később implementálandó v1 adatmodell

**Globális választás – egyetlen, hozzáadott mező a meglévő `settings` objektumban:**

```js
trainingModes197: {
  schemaVersion: 1,
  defaults: { trainingStyle: 'normal', progressionMode: 'double' },
  explicitChoice: false,
  byProgram: {
    // [programId]: { trainingStyle: 'hit', progressionMode: 'rir',
    //                explicitChoice: true, updatedAt: ISO8601 }
  }
}
```

- `trainingStyle`: **normal | hit**. A HIT nem automatikus bukás/kényszerített ismétlés; megfelelő bemelegítéssel és konzervatív erőfeszítéssel.
- `progressionMode`: **double | linear | rir**; a későbbi volumenfókusz csak későbbi bővítés.
- Régi mentésnél hiányzó `trainingModes197` = *legacy kompatibilis működés*, sem migráció, sem gyakorlat-átírás, sem új automatikus terhelésalkalmazás. A `normal/double` címke önmagában nem kapcsolja át a korábbi algoritmust: az `explicitChoice` külön őr.
- Programonkénti felülírás külön `settings.trainingModes197.byProgram[programId]` objektumban, mert az alap sablonprogramok tárolás nélkül is előállhatnak. Így nem kell a program, a napló vagy a `history128` sémáját módosítani.
- Minden ismeretlen/hibás érték validáció után a régi működésre áll vissza; ne legyen implicit `HIT` és ne engedjen sérüléskockázatos automatikus progressziót.
- Választás előtt számított `preview`: régi és új `sets/reps/rest/weight/RIR`, indoklás, a becsült volumen és az érintett program. Jóváhagyás után atomikus mentés; Mégse/Back nem írhat semmit. Aktív edzés esetén a módváltás ne módosítsa a megkezdett sessiont.
- Meglévő `progressionSteps` (felszerelésből adódó tényleges súlylépcsők) marad; új lineáris és dupla algoritmus ugyanazt a `tp140Step()` forrást használja.
- Az erőfeszítés, fájdalom és regeneráció **minden** módban felülírhatja a javasolt emelést konzervatív irányba. A hiányzó RIR nem jelent 0 RIR-t.
- HU/EN/DE/RO: érthető elkülönített stílus/mód választó és magyarázat; ne tegyünk edzésstílust a GPS-mérés képernyőjére.

## Megvalósított 2/7 – validáló, csak olvasó feloldó

- `www/app.js` → `// @section training-modes-197.js` – `tp197TrainingModes(programId=null)`. A függvény csak a `settings().trainingModes197` beállításokat olvassa, a visszatérési érték új, leválasztott objektum, és **nem változtat meg** tárolt beállításokat, programot, naplót, draftot vagy aktív edzést.
- Visszatérő mezők: `schemaVersion`, `programId`, `trainingStyle`, `progressionMode`, `explicitChoice` és `source: 'legacy' | 'global' | 'program'`.
- Öröklés: aktív session programja, ennek hiányában aktív program; explicit programválasztás megelőzi az explicit globális választást; `explicitChoice:false` programérték visszaenged a globálisra.
- Legacy fallback: `{ trainingStyle:'normal', progressionMode:'double', explicitChoice:false, source:'legacy' }`. A verzió nélküli, hiányos, érvénytelen vagy nem jóváhagyott beállítás **nem** aktivál új algoritmust.
- Sérült programfelülírás (ismeretlen mód / hiányzó jóváhagyás / `null`) esetén legacy fallback, **nem** csendes globális mód. A `byProgram` csak saját kulcs alapján olvasható.
- `tests/training-modes-197.cjs`: részletes Node/VM-teszt a tényleges `app.js` részletből, a fő tesztcsomagban automatikusan fut (`scripts/test.cjs` a sectionöket bontja). Legacy, globális/program, session ownership, hibás adat, backup JSON oda-vissza, és változatlan settings/program/history/draft.

**Nincs** még beállítási UI, módváltási előnézet, önálló HIT/lineáris/RIR számítás, illetve automatikus terhelésmódosítás. A `main` nem változott, nincs verzióemelés.

## Következő mikro-lépés (3/7)

Konzervatív és tesztelhető **dupla, lineáris, RIR** javaslatok elkülönítése: tiszta javaslat-adat, továbbra is `autoApply:false` amíg a felhasználó nem fogadja el az előnézetet. A régi `source:'legacy'` út érintetlen marad; fájdalom, alacsony readiness, hiányzó RIR és idegen programhoz tartozó előzmény blokkolja az emelést.

**A 2/7 lépés végén még nincs:** új Coach/UI algoritmus, súlyváltoztatás, verzióemelés, APK, `main` merge.

## 3/7 – Implementált előnézeti progressziós motor

- `www/app.js` → `progression-modes-197.js`: tiszta `tp197ComputePreview(input)`, csak olvasó `tp197ExercisePreview(id,programId)` adapter.
- Legacy / explicitChoice nélküli tárolás → `handled:false`, a meglévő #128/#140 logika és az automatikus alkalmazás **változatlan**.
- Dupla: +1 szabályos ismétlés az egyik legkisebb sorozatnál, majd **3 egymást követő**, azonos súlyú, biztonságos felső-cél teljesítés után kis súlylépcső **csak javaslatként**.
- Lineáris: **2 egymást követő** azonos súlyú, teljes, alsó-célt elérő, jó erőfeszítésű alkalom után súlylépcső-javaslat.
- RIR: csak ténylegesen naplózott `actualRir` vagy exercise feedback RIR használható, **nem** a célrecept `rir` mezője. Hiány esetén nincs emelés. 2 stabil, célnál lényegesen magasabb visszajelzés javasolhat emelést.
- HIT: csak előnézeti javaslatban kevesebb munkasorozat (1 izoláció / legfeljebb 2 más), **semmilyen automatikus sorozatcsökkentés vagy bukásig edzés**.
- Fájdalom, alacsony readiness, hiányos sorozat, rossz/nehéz erőfeszítés, cél alatti ismétlés, hibás súlylépcső és nem erősítő mérés blokkol.
- A javaslat mindig `autoApply:false`, `previewOnly:true`; jelenleg **nem** kötöttük be a Coach renderbe, programmentésbe vagy az aktív edzésbe.
- Új Node teszt: `tests/training-progression-197.cjs` (tiszta döntések, HIT, RIR-adathiány, negatív esetek és adapter tulajdonosság).
- Következő 4/7: a beállításválasztó és HU/EN/DE/RO szövegek, 5/7: előnézet/jóváhagyás és biztonságos mentés. `main` és verzió változatlan.


## 4/7 – Többnyelvű előnézeti módválasztó

- Új `training-modes-ui-197.js` kanonikus szekció a `www/app.js` fájlban; a **Beállítások** oldalon globális, az **Edzés** oldalon aktív programhoz tartozó választó.
- Két külön vezérlő: `normal/hit` edzésstílus és `double/linear/rir` progressziós szabály. HU/EN/DE/RO magyarázatok, 44 px-es mobil érintési célok és összehasonlító előnézeti sorozat/súlyértékek.
- A 3/7 számítómotor `tp197ExercisePreview(id,programId,previewModes)` paramétere csak ideiglenes UI-választás **olvasására** szolgál. Saját programhoz kötött napló és readiness marad a forrás.
- Az aktuális UI-választás **csak memóriában** (`tp197UiPending`) él; nem kerül a `settings`, `programs`, `history`, `draft`, backup, Wear vagy Drive adatmodellbe. Visszaállítás lehetséges. Nincs alkalmazás vagy jóváhagyás; azt az 5/7 lépés implementálja.
- Új tesztek: `tests/training-mode-ui-197.cjs` (öröklés, többnyelvű feliratok, tilos mentés, program-scope izoláció), `tests/browser/training-modes-ui-197.cjs` (mobil és a működő alkalmazásban való ellenőrzés).
- Ebből a változatból a #184-es Samsung pontszámot sem olvassuk közvetlenül, az integráció később külön PR-ben lesz.

## 5/7 – Megerősített, visszavonható módváltás

- Kanonikus forrás: `www/app.js` → `training-mode-confirm-197.js` szekció; egyetlen app.js runtime-script, nincs külön JS-behúzás vagy indítási regresszió.
- A globális/programbeállítási előnézet továbbra is csak a `tp197UiPending` memóriában él. **Mentés** külön jóváhagyó felületet nyit; csak a **Jóváhagyom a módváltást** gomb után van egyetlen `db.set('settings',...)`. **Mégse** az adott scope előnézetét eldobja; választás-változtatás vagy közben megváltozott beállítás érvényteleníti a régi jóváhagyást.
- A `settings.trainingModes197` v1 sémában a globális `defaults` és a `byProgram[programId]` elkülönül. `explicitChoice:true` nélkül a legacy út aktív; a mentés csak a felhasználó választott scope-ját módosítja, `updatedAt` és `revision` metaadattal.
- A legutóbbi mentett változás egyetlen szinten **visszavonható**; a mentés előtti módbeállítás a `undo.previous` részben van, az előzmény nem halmozódik rekurzívan. Korábbi legacy állapot esetén a visszavonás teljesen eltávolítja az új módbeállítást.
- Aktív (szünetelő is) edzés közben módmentés vagy visszavonás **tiltva**; program scope esetén a programazonosító és aktív program egyezése kötelező. A programrecept, korábbi napló, teljesített sorozatok, draft, GPS/Wear és Samsung-adatok nem változnak.
- HU/EN/DE/RO gombok és visszajelzések; célzott Node és 103 elemű Chromium tesztsuite: `tests/training-mode-confirm-197.cjs`, `tests/browser/training-mode-confirm-197.cjs`.
- **Határ**: a módbeállítás mentése nem kapcsolja át a meglévő Coach automatikus terhelésalkalmazását; ezt a következő **6/7** lépés fogja explicit jóváhagyáshoz kötni. Nincs verzióemelés, nincs main-merge, nincs #184-es Samsung SDK-kapcsolat.

A CI-eredményt commitonként kell ellenőrizni, és csak azonos HEAD-en sikeres kapuk után zárható le a 5/7.

## 6/7 – Közös Samsung + Coach-integráció (PR #222)

- Külön ág: feat/184-197-coach-integration; Draft PR #222. Az eredeti #184 és #197 változások összevezetése megtörtént.
- Csak aznapi, hitelesített forrású Samsung Energy Score használható, időszerűség ellenőrzésével. Hiányzáskor megmarad a TrainPilot fallback, és a két becslés külön látszik.
- A fájdalom és túlterhelés konzervatív korlát. Nincs kétszeres alvás/HRV/aktivitás pontozás.
- A választott HIT / normál stílus és dupla / lineáris / RIR mód Coach-javaslatként jelenik meg. Nincs automatikus súly/sorozat/ismétlés-átírás.
- Az új ajánlott súlyhoz kétlépcsős jóváhagyás kell; megkezdett sorozat, szüneteltetett edzés, Wear-tulajdonlás és módosult állapot esetén blokkol.
- Az 5/7-es módváltó korábbi böngészős indítási hibáját (szigorú mód, undeclared handlers) javítottuk.
- Célzott Node-teszt: tests/training-coach-integrated-197.cjs; böngészős teszt: tests/browser/training-coach-integrated-197.cjs.
- A 7/7 továbbra is az utolsó commit Quick Validation + Chromium 6/6 + natív Release Gate, majd telefonos/Samsung Health/Wear készülékes APK-teszt, felhasználói jóváhagyás. Csak ezt követően merge és Issue/PR lezárás. Külső verziókód változatlan.
