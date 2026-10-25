# PR242 – migrációs triage és javítási sorrend

2026-10-25. Külső audit: **9c3c26474251387986bff522eeaedb906d2877b2**,
test/native-feature-parity-audit, draft #242. Eredeti fő-/natív bázis7473b30/a5f562d;
a teljes eredeti riport byte-pontosan átemelve NATIVE_FEATURE_PARITY_AUDIT.md-be.
Egyetlen dokumentumot módosított, nincs külső appkód/teszt/build/merge.

## Elbírált megállapítások

A hét hiány a vizsgált felhasználói utak alapján fennállt; egyik sem kizárólag
elmaradt tesztfuttatás. Ez statikus megerősítés, nem eszközös reprodukció.

| Prioritás | Hiány | Aktuális állapot |
| --- | --- | --- |
| P1 | Naplózott edzés idő-/gyakorlat-/sorozatadatainak utólagos szerkesztése | **1a9b6d3/6856abe** kódban pótolva, futtatás/elfogadás hátra. |
| P1 | Programgyakorlat sorozat/ismétlés-idő/súly/pihenő/próbasúly szerkesztése | **b161126** kódban pótolva, futtatás/elfogadás hátra. |
| P2 | Kardió heti táv/tempó/idő és azonos távú rekordok | Nyitott: Progress jelenleg izom/1RM/testsúly út. |
| P2 | Edzésidőablakhoz kötött HC-részletek és felhasználói frissítés | Nyitott: Journal forrásfül JSON jelenlétét jelzi, nem részletes értéket/refresh-t. |
| P2 | Globális/programonkénti edzésstílus/progresszió megerősítéssel és undo-val | Nyitott: main trainingModes197 UI és natív effektus útja külön pótlandó. |
| P2 | Egészségnapló automatikus foreground/on-open frissítés | Nyitott: main autoOnOpen és10perces throttle nem azonos a natív háttérmóddal. |
| P2 | Nyers egészségrekordok tetszőleges többnapos from/to szűrése | Nyitott: natív record query/browser egy kiválasztott napra épül. |

Ellenőrzött main hívási utak: végső editHistoryWorkout → rf151HistoryEditor /
rf151SaveHistory; tp107SaveProgramRow → program.prescriptions[exerciseId];
tp107CardioModel; trainingModes197 mentés/undo; Health Journal preference /
autoOnOpen → rf250Sync és readPage(from,to). Natív oldalon a megfelelő UI/VM
vezérlők/callbackek/queryk átnézve. A rögzített permalinkek az eredeti riportban.
Testsúlynapló,26 metrika/nyers rekord/alvás, aktív edzés/undo és globális
intervallumpreset már létező adatútját nem minősítjük hiánynak.

## Naplószerkesztés – 1a9b6d3 /6856abe

- Részletekből explicit edit/save/cancel, ViewModel-owned izolált draft; idő,
  gyakorlat/szett add/remove, weight/reps/time/bilateral/distance/done/RIR.
- Exact-ID Room detail lookup és teljes parent/exercise/set/photo CAS a tx-ben;
  dataset ticket, törlés/tombstone, azonos active/finalization guard. Új
  katalógusgyakorlatnál loadType/repUnit/measurementType változás elutasítás.
- Egyetlen meglévő mapper és bounded PR értékelés; nincs aktív UI/finish/nyugta
  újrajátszás. ID/sync/key/schedule és retainedsetUUID/route/photo/Health megmarad.
- Canonical single-row export kiemelve a backup mapperből; megmaradó raw unknown
  exercise/set/photo/feedback mezőket sem dobja el a későbbi portable export.
- Időváltozáskor egészségmérés eredeti window-ja külön jelölve a Source fülön;
  mérést nem tolunk át új időszakra. Változatlan dátumoknál duration és a
  UI-formátumban nem látható szubmásodpercek megmaradnak. Hibás legacy dátum
  megnyitás előtt műveleti hiba; a kézi dátummező explicit timezone offsetet kér.
- 20 új resource HU/EN/DE/RO.12 JVM-metódus és1 valódi appos UI-case új kód,
  **NEM FUTOTT**. Statikus lexical/XML/R-ref/adatút és exact-ID SQLite index check;
  nem Kotlin/Room/Compose futás. Batch545 JVM+49 Phone UI vár a közös kapura.
- A megnyitott PR/audit nem lezárt. Ez functional UI bekötés, nem végleges
  vizuális polish; összes paritás/platform/CI/device elfogadás továbbra külön.

## Programcélérték-javítás – b161126

- A meglévő NativeProgramEditor.Draft csak explicit, per-exercise patch-et hordoz.
  Parent program Save az egyetlen commit; child Apply csak lokális draft.
- Main-kompatibilis sets1..10/reps1..40karakter/weight0..10000/rest30..300,
  tizedesvessző és üres súly mint trial csak az elfogadott loadType-oknál.
- Globális prescriptions és minden meglévő inline occurrence explicit mezői
  együtt frissülnek; string ID-k, variation/RIR és ismeretlen metaadat megmarad.
  Az UI jelzi, hogy ugyanazon gyakorlat minden programnapjára érvényes.
- Beépített program klónozódik, eredeti nem változik; meglévő program/day CAS és
  aktív edzés elleni guard marad. Katalógus loadType/repUnit/measurementType
  változásakor stale editor mentés elutasítva, közös Room tranzakció.
- A tényleges NativeWorkoutSession továbbra ugyanazt a global→inline rx adatot
  olvassa. A tp228 előzmény-prefill policy változatlan; a programcélok szerkesztése
  nem ír át edzéselőzményt vagy aktív sorozatot.
- 11 új JVM-metódus + meglévő valódi appos UI-eset bővítése (invalid/child cancel,
  parent save, reopen és parent cancel/no-write). **NEM FUTOTT.** Statikus XML/
  kulcs/paraméter/R-ref/import/lexical/diff és main-forrás/adatút ellenőrzés volt.
  Batch: 533 új JVM +48 Phone UI tesztkód vár a közös kapura.

97f18fd a mentésnél egyszer indexeli az exercise-entryket és újrahasználja a
prescription JSON-t, nem parse-olja a teljes programot minden sorhoz.

## Következő csomagok

1. A két P1 kódban pótolva; következő Journal-rész az edzéshez kötött HC-panel és
   explicit refresh. Közben Home target/start/invalidation a fő munkásnál.
2. Külön program/settings módválasztás +valódi progressziós policy parity.
3. Kardió bounded projekció/aggregátum és megfelelő Progress UI.
4. Health foreground opt-in/throttle +permission-aware sync és többnapos records UI/query.

A korábban javított Home napi count e4920f4, célprogramnap/start/invalidation
külön hátra. Az audit29 útvonala nem teljes release-elfogadás: a többi A–E,
Google/cross-device/Health/GPS/platform és a végső kapuk is megmaradnak.
Main merge, verzióemelés, PR-lezárás vagy élő adatos APK-frissítés nem történt.

Elkülönített promptok átadva a felhasználónak, indulás/eredmény nem igazolt:
Health date range → plans/NATIVE_HEALTH_DATE_RANGE_HANDOFF.md; Gemini cardio
számítás → plans/NATIVE_CARDIO_GEMINI_HANDOFF.md. Csak saját ág/two-file keret,
main/migráció írás tiltott a külső munkásnak; közös review/integráció itt.
