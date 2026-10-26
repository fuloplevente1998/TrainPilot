# PR #245 – válasz a párhuzamos review 1–5. pontjára

Forrás: [NATIVE_PARALLEL_PR245_PR246_REVIEW.md, 5084f81](https://github.com/fuloplevente1998/TrainPilot/blob/5084f81/docs/reviews/NATIVE_PARALLEL_PR245_PR246_REVIEW.md).
Saját ág: `feat/native-training-modes`, draft PR: [#245](https://github.com/fuloplevente1998/TrainPilot/pull/245).
Célág: `feat/native-android-migration`. A review-pontok megoldása ezen a külön
ágon történt, másik feature branch, main, workflow, verzió és release nélkül.

## 1. P1 – Kotlin publikus/internal szerződés
- A publikus `SettingsViewModel` `approveTrainingMode` belépése `internal` lett,
  az `internal NativeTrainingModeChange.Preview` változatlanul nem publikus.

## 2. P1 – Settings / Program módmentés és undo főszál
- A kanonikus `PhoneWorkoutExecution.store(context)` és az aktív/paused
  workout ellenőrzése `Dispatchers.Main.immediate` alatt fut,
  **az eredeti `NativeDataReplacement.mutate` metadata lease-en belül**.
- A DataStore `edit` továbbra szuszpendálható, nem került Main-only sínre.
- Mind a Settings-, mind a Programs-VM meglévő IO-belépési útja ugyanazt a
  közös store-védelmet használja.
- Célzott Android emulátoros tesztforrás: IO-ból approve/undo, illetve
  aktív kanonikus workout mellett módmentés elutasítása.

## 3. P1 – Coach súlyjóváhagyás main-only kanonikus commit
- A VM a friss Coach history/readiness/catalog/step fingerprintet IO-n,
  metadata lease-en belül számolja újra.
- A `NativeTrainingModeProposal.commitApprovedOnMain` a kanonikus
  `NativeWorkoutEditing.commitEdit` hívást kizárólag Main.immediate-en,
  az érvényes beállítás és **az aktuális élő frame** ticket-CAS vizsgálata után
  indítja, további felfüggesztés nélkül.
- Nincs új workout writer; az eredeti owner/generation/revision/untouched
  set/GPS/timer/finish és háttéradat-frissesség védelem megmarad.
- Az új Android-emulátoros teszt valódi `PhoneWorkoutExecution`-t és
  kanonikus commitot használ IO-belépésről; ellenőrzi a tartós revisiont,
  a megváltozott súlyt, a stale-ticket és config elutasítását.
  A teljes Coach-előzményből VM által előállított ticket végigfuttatása
  továbbra külön integrációs elfogadási kapu.

## 4. P2 – globális picker hibás programoverride-ja
- `NativeTrainingModes.globalChoice` csak a globális `defaults` mezőkből
  állítja elő a választót, a program saját override-jától függetlenül.
  Nem explicit global esetén a mezők láthatók, de a legacy nem aktiválódik.
- A picker és a `NativeTrainingModeChange.prepare` global scope-ban ugyanazt
  használja. A program scope megőrzi a kanonikus effektív programfeloldást.
- Új JVM tesztek és valós Compose Settings-VM regresszió fedik:
  global normal/double + aktív program hit/rir; csak a global progresszió
  módosul, az aktív program override érintetlen.

## 5. P2 – main-paritás pure-RIR célra
- A `NativeTrainingModeProgression.compute` csak `target.kind == "reps"`
  gyakorlatnál értékel progressziót. Pure `kind=="rir"` cél,
  ismétléstartomány nélkül: **hold / non-strength-activity / változatlan súly**,
  a pinned main tényleges eredményével megegyezően.
- A korábbi hibás, 12.5 kg-os emelést elváró teszt most a main konzervatív
  döntését rögzíti. A valóban mért RIR-értékek a `reps` célú RIR-szabálynál
  továbbra külön kezeltek (missing != 0).

## Tesztek és átadás
- További **2 új JVM teszt** (global choice és a scope-helyes before/after)
  és **3 új Android-instrumentációs tesztmetódus** (közös kanonikus
  IO→Main/store út, valódi editor commit és teljes Settings UI).
- A pure-RIR meglévő regressziója az exact main-oracle elvárásra javítva.
- Ezek **tesztforrások**, nem állítunk lefutott zöld fordítást/CI-t.
  A review javítása során teljes CI-t, APK buildet nem indítottunk.
- Az új HEAD-et és a célág felé fennmaradó acceptance-gate-et a
  [Draft PR #245](https://github.com/fuloplevente1998/TrainPilot/pull/245) rögzíti.
