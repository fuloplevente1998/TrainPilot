# #245 / #246 integráció előtti review

Dátum: 2026-10-25. Fő ág: feat/native-android-migration. A review **nem merge és
nem alkalmazáskód-javítás**. Root nem írt a külső ágakra, nem indított Gradle-t,
emulátort vagy új CI-t. A lentiek exact forrásvizsgálat és meglévő CI-bizonyítékok.

## Források és döntés

- Bázis: `0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20`.
- [#245](https://github.com/fuloplevente1998/TrainPilot/pull/245),
  `2ff2985c4376e2a7ee946c73e6523c65a983a980`: 27 appfájl, +2219/−10.
  **Javítás szükséges integráció előtt**, öt konkrét megállapítás alább.
- [#246](https://github.com/fuloplevente1998/TrainPilot/pull/246),
  `e9eb8cbd52d83ab86327f60dbf6193d41e681396`: 23 fájl, +1251/−27.
  **A kardiós JVM-rész igazolt; közös release-elfogadás nincs.** Egy időfüggő
  frissítési hiány, valamint külön baseline-javítások és hat közös hiba marad.
- Main, verzió/package/toolchain/Room-séma/workflow egyik diffben sem változott.
  A #245 CoachScreen-bővítésének külön engedélyét a külső PR-leírás rögzíti.
  #244 még nem része ennek a review-nak, lefoglalt fájljai változatlanok.

## #245 – javítandó megállapítások

### 1. P1 – publikus VM-metódus belső típust tesz ki

[SettingsViewModel.kt:129](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L129)
`class SettingsViewModel` publikus, `approveTrainingMode` szintén publikus, de
paramétere az `internal object NativeTrainingModeChange` alatt deklarált Preview.
Ez a Kotlin láthatósági szerződés szerint `EXPOSED_PARAMETER_TYPE` fordítási hiba.
Nincs erre suppress a fájlban. Minimális javítás a VM-belépés megfelelő internal
láthatósága; ne tegyük indokolatlanul publikussá a teljes mode API-t.

**Bizonyítékhatár:** forrásból azonosított fordításblokkoló; #245 fordítását nem
futtattuk, ezért nem közlünk kitalált CI-diagnosztikát.

### 2. P1 – a módmentés és visszavonás valódi UI-útja főszálhibás

[SettingsViewModel.kt:140](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L140)
és TrainingProgramsViewModel.changeTrainingMode IO-ra teszi a műveletet.
[TrainPilotSettingsStore.kt:334](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt#L334)
ugyanebben a coroutine-ban `PhoneWorkoutExecution.store(context)`-ot hív.
A meglévő PhoneWorkoutEngine.kt:451–454 csak main looperről engedi ezt, különben
`Execution requires main looper`. A NativeDataReplacement.mutate **nem vált
Mainre**, csak a metadata lease-t biztosítja. Így az approval/undo hibát jelez,
a beállítás nem mentődik. A közvetlen runBlocking store-tesztek ezt nem fedik.

Javítás: rövid kanonikus állapotellenőrzés Main.immediate-en, metadata admission
megőrzésével; a DataStore/Room munka maradjon megfelelő háttérszálon. Érdemi
IO-entry regresszió és valódi Settings/Programs UI-flow szükséges.

### 3. P1 – a külön súly-jóváhagyás is IO-ról hívja a kanonikus szerkesztőt

[ActiveWorkoutViewModel.kt:971](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L971):
az approveTrainingModeWeight IO-blokkjában közvetlen `NativeWorkoutEditing.commitEdit`.
Az már az első sorában a fenti main-only store-t kéri. Tehát érvényes ticket,
friss config/history és untouched set mellett sem alkalmazható a javaslat.
A pure Ticket/editedFrame teszt nem próbálja ezt a tényleges műveleti utat.

Javítás: az IO-s tanácsszámítás után **Mainen újra validált**, rövid canonical
commit; owner/generation/revision/config/dataset és forrásfrissesség védelme
maradjon, ne második frame-író készüljön. Tényleges VM→commit regresszió kell.

### 4. P2 – a globális választó programoverride-ot jelenít meg

[NativeTrainingModePicker.kt:51](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/ui/settings/NativeTrainingModePicker.kt#L51)
global scope esetén is az activeProgramId effektív policy-jét oldja fel.
Példa: globális Normal/Double, aktív program HIT/RIR → a globális mezők HIT/RIR-t
mutatnak. Ha a felhasználó csak a progressziót módosítja, a globális stílust is
véletlenül HIT-re mentheti. A main végső tp197UiChoice global esetben a globális
defaultot választja, a programoverride-ot csak program scope-ban.

A pinned main tényleges függvényeit Node-ban futtatva:
`main_global_picker = normal/double`, effektív aktívprogram = `hit/rir`.
Ez JS-referenciafuttatás, nem Kotlin/Compose teszt.

Javítás: külön global choice resolver, scope-helyes before/after értékek és
regresszió egyszerre eltérő globális/programbeállítással.

### 5. P2 – a pure-RIR cél viselkedése eltér a main biztonsági alapjától

[NativeTrainingModeProgression.kt:54](https://github.com/fuloplevente1998/TrainPilot/blob/2ff2985c4376e2a7ee946c73e6523c65a983a980/android/app/src/main/java/com/repforge/app/ui/coach/NativeTrainingModeProgression.kt#L54)
külön megengedi a `target.kind == rir` célt ismétléstartomány nélkül.
Az új pureRirPrescriptionWithoutRepRangeUsesActualObservationsOnly teszt két
8-ismétléses,10kg-os, tényleges RIR5 alkalomból12.5kg javaslatot vár.
Ugyanezen bemenettel a pinned main tényleges tp197ComputePreview függvénye:
`hold / non-strength-activity / weight10`; main target.kind != reps mellett tart.

Ez nem hiányzó RIR-javításként automatikusan elfogadandó változás: az átadás
main-paritást kér, nem új progressziós szabályt. A migrációban maradjon a main
konzervatív eredménye; tudatos későbbi termékdöntésként külön fejleszthető.
A teszt elvárását csak az exact main-oracle alapján szabad korrigálni.

## #246 – ellenőrzött részek és egy javítandó hiány

Forrásból ellenőrzött: 256-soros scalar Room feed; finished/workout/raw-set
keyset, kihagyott sor után is cursor-előrelépés; undone testvérset flag;
tombstone kizárás; tényleges set.reps idő, route≠source, egy szülő raw source
streaming indexe; meglévő math mag változatlan; same-count Room invalidation;
cancellation/revision guard; all-time/8hét/rekordok/time-only UI; négy nyelv,
tématokenek és meglévő strength/1RM/testsúly felület megőrizve.

### 6. P2 – nap-/hét-/időzóna-váltás nem érvényteleníti a heti statisztikát

[ProgressViewModel.kt:69](https://github.com/fuloplevente1998/TrainPilot/blob/e9eb8cbd52d83ab86327f60dbf6193d41e681396/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L69)
csak DB invalidation, aktivitásváltás vagy retry esetén számol újra.
A `now` és ZoneId kizárólag a reload kezdetén kerül a modellbe. Ha a folyamat/
nézet életben marad vasárnap→hétfő vagy időzónaváltás alatt, új naplóírás nélkül
a „jelenlegi hét” a korábbi hetet mutatja; visszatéréskor sincs ON_RESUME reload.
A Home saját lifecycle/date/time/zone frissítője nem ír Roomot, ezért ezt nem
érvényteleníti. Új naplót nem szabad szükséges feltétellé tenni a dátumos nézethez.

Javítás: lifecycle-aware helyi időfüggő frissítés/date-time-zone trigger, rendes
dispose és azonos queryk deduplikálása. Injektált óra/zóna +resume regresszió,
provider-hívás vagy automatikus egészségszinkron nélkül.

### Külső baseline-javítások külön értékelése

- BackupRestoreManager `context`→`appContext`: az osztály megőrzött valódi
  application contextjét használja, indokolt fordítási javítás.
- WearSyncRepository: hiányzó outer mutate-lezárás, cleanup catch és explicit
  Unit típus. Forrásban a tombstone továbbra kötelező, a saved receipt továbbra
  a finalizer után. Fordítás igazolt a külső futáson; Wear/device regresszió nincs.
- Két JUnit4 Unit-visszatérés; ProgramDay compositeId és receiver `this@...`;
  ComposeTestRule fixture és érvénytelen extension-importok korrekciói:
  API-kompatibilitási javítások, nem üzleti elvárásgyengítés.
- Planner tesztfixture átnevezése `native-planner-cases-main-7473.json`-ra:
  byteazonos tartalom, a production azonos nevű resource elfedését szünteti meg.
- ProgramEditorThreading teszt null helyett inactive állapotot vár: a canonical
  store az inaktív markert jogszerűen tárolja; az aktív edzés létrejöttét továbbra
  tiltja, ezért ezen a szerződésen nem tesztgyengítés. A friss XML-ben zöld.

## Tényleges CI-bizonyíték és a hat megmaradt hiba

[Actions40238833748](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40238833748)
PR-event, head e9eb8cb. Checkout/`github.sha` merge-refje62cc08fee0c9a708be1689a055949386b47dfef1,
szülei32a9dc8 és e9eb8cb. Root ellenőrizte: app/Wear/shared/workflow forrása
azonos e9eb8cb-vel, eltérés csak hat dokumentum; ezért a kódbizonyíték ehhez köthető.

Letöltött és feldolgozott valódi XML artifact12615809890:

- Phone JVM **984 /6 fail**, Wear JVM **167 /0 fail**; összesen1151/6.
- NativeCardioBindingTest **11/11**, NativeCardioModelTest **15/15** PASS.
- Phone/Wear debug build és Phone UI test APK fordítás zöld a jobon belül.
- Wear UI, Phone UI/screenshots és signed upgrade job **SKIPPED**.
- A #245 módok forrása nincs ebben a futásban; nem vonatkoztatható rá a zöld fordítás.

| Hibás osztály | XML és forrás alapján megállapítható | Következő ellenőrzés |
|---|---|---|
| NativeCoachVariationTest | `Execution requires main looper`; save újraszámoló preview-ja Room transaction threadről hívja a main-only store-t. Valódi termékút probléma, nem kardió. | Main.immediate admission a megfelelő rövid helyen; ne a globális looper-védelmet töröljük. |
| NativeGoogleConsentBridgeTest | Azonos IOException típus/üzenet, eltérő objektumidentitás az await után. A jelenlegi `assertSame` a coroutine exception recovery mellett túl szigorú lehet. | Típus/üzenet/cause és gate cleanup/retry jelentés ellenőrzése; nem kész OAuth-próba. |
| NativeHomeRepositoryTest | Query callback2 program-SELECT-et jelez1 helyett. Source read egy katalógus-DAO hívást ad át a közös selectnek. | Tényleges query trace szükséges; ne emeljük vakon2-re az elvárást és ne jelentsünk bizonyítatlan teljesítményjavulást. |
| NativeMetadataGateTest | XML kifejezetten5s **virtuális** timeoutot jelez a valódi IO dispatcherre váltásnál. | Valódi időből korlátozott/koordinált teszt; a reentrant admission maradjon ellenőrzött, nem igazolt termék-deadlock. |
| NativePlannerTemplateParityTest main-plan-7 | Az oracle elutasítást vár egy equipment/exclusion bemenetre, a natív preview elfogadja. | Pinned main és az oracle előállítása alapján eldöntendő tényleges planner-paritás; tilos vakon sikerre módosítani. |
| NativeWorkoutDiscardTest | A bukás a pendingEvents=1 assertionnél, **discard hívása előtt**. prepare(…,0) revision1-et ad, a fixture record még revision0-val próbál írni; record eredményét nem ellenőrzi, ezért nincs esemény. | A prepare visszaadott frame/revisionjéből valódi elfogadott record, status assertion, majd változatlan durable tombstone/native-close elvárások. |

Nincs új tesztfutás vagy alkalmazáskód-javítás ebben a review-ban. A #245
33 JVM+3 UI és a #246 három UI-tesztje továbbra nem futtatott elfogadás.

## Következő lépés

Előbb a fenti izolált hibák javítása, aztán exact új külső delta review és
ellenőrzött integráció a migration ágon. #244 beérkezése külön review.
Ezután egy integrált forráson a közös natív gate; main/release/élőadat-upgrade
csak a korábban rögzített adatbiztonsági és készülékes elfogadás után.
