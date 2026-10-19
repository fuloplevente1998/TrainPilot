# #197 – Edzésstílusok és progressziós módok: kiinduló audit és adatmodell-terv

Állapot: **2/7 – feloldó és célzott Node-regresszió implementálva (2026-10-19)**. A GitHub Actions teljes ellenőrzésének eredménye ettől külön követendő. Ez a dokumentum a `feat/197-training-styles-progression` fejlesztési ághoz tartozik; nem jelent kész, bekapcsolt funkciót vagy kiadási jóváhagyást.

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
