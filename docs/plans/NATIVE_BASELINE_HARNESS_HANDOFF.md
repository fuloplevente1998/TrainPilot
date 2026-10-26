# Új chat A – három közös JVM-regresszió tesztkörnyezetének rendezése

Repo: https://github.com/fuloplevente1998/TrainPilot
Rögzített induló commit: `4eae08c8b917893ac5da37346201872828783814` a feat/native-android-migration ágról.
A bázis tartalmazza #246 kardió és #244 javított Health kódját. #245 külön munkásnál.

Először AGENTS.md, docs/NATIVE_RESUME.md, docs/WORK_HANDOFF.md teteje, majd
csak a konkrét alábbi források. A későbbi főági státusz nem a bázisod tesztbizonyítéka.
Ez a felhasználó által kért KÜLÖN CHAT-feladat, saját ág kivétel az AGENTS fő
munkásának migration-only szabálya alól; nem belső subagent-felhatalmazás.
Main/migration írás, merge, force push, issue/PR-close, release tiltott.
App com.repforge.app /1.3.71-native-test /2736 változatlan. Nincs schema/toolchain/
függőség/workflow változtatás, új implementáció a meglévő motor helyett vagy élő install.
Kis [skip ci] commitok a saját ágra, draft PR cél feat/native-android-migration.
Helyben nincs Gradle/APK/emulátor/teljes teszt. Forrásvizsgálat, statikus ellenőrzés
és indokolt kis referenciafuttatás megengedett; közös natív CI az integrátornál,
ne indíts külön teljes kaput. Megírt teszt nem futtatott teszt; pontosan külön jelöld.
Ne küldj GitHub-kommentet. PR-leírás/új saját átadód frissíthető. Magyarul röviden.
Javasolt High kódoláshoz; nem látványterv- vagy témarendszer-feladat.

Átadás: exact SHA, fájllista, bizonyított ok és minimális fix, megőrzött invariánsok,
megírt és valóban futtatott ellenőrzés külön, nyitott pontok. Más feladat fájlját
ne módosítsd: szükséges plusz termékfixet külön javaslatként adj át, ne rejtsd tesztbe.

Saját ág: `test/native-baseline-harness-fixes`.

A korábbi CI40238833748 (Phone984/6fail) három esetét vizsgáld. A már #244-ben
javított CoachVariation/GoogleConsent hibához ne nyúlj. Planner másik chatnél.
Olvasd: docs/reviews/NATIVE_PARALLEL_PR245_PR246_REVIEW.md közös hibák táblázata,
valamint docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md.

Kizárólag írható három meglévő fájl az android/app/src/test/java/com/repforge/app alatt:
- NativeMetadataGateTest.kt
- NativeWorkoutDiscardTest.kt
- NativeHomeRepositoryTest.kt
Új saját, nem UI teszthelper a test source setben megengedett. Termékkód, közös
NativePhoneNavigationTest, meglévő más fixture, XML, planner és Health nem írható.

1. MetadataGate nestedImporterAndSettingsCallbacksShareTheSameAdmittedWriterWithoutDeadlock:
virtuális5s timeout a real Dispatchers.IO-val. A reentrant admission értelmét őrizd:
az outer és nested write valóban lefut és a számláló2; független writer továbbra
blokkolt, cancellation felszabadít. Valós IO/Main határ és determinisztikus befejezés;
fix sleep vagy puszta timeout-növelés helyett helyes scheduler/óra-koordináció,
valós időben is korlátozott hibázás, dispatcher reset minden ágon.

2. WorkoutDiscard explicitDiscardAbandonsPendingRecordingOnlyAfterDurableTombstoneAndNativeClose:
prepare(input,0) revision1-et ad, a fixture completeSet command még revision0.
A record eredményét ellenőrizni kell, a tényleges prepared frame/revision alapján
hozz létre valódi pending eventet, és csak utána discard. Maradjon pending=1,
Room tombstone, natív close, nincs history, retryable failure, reopen és retry.
Tilos eltávolítani pending assertet vagy a finalizert megkerülni.
Olvasd a PhoneWorkoutEngine/ExecutionStore/TrainPilotWorkoutFinalization tényleges útját.

3. HomeRepository snapshotsReadTheProgramCatalogOnceAndNeverLoadAllHistoryDetails:
query callback2 SELECT-et jelzett1 helyett. Vizsgáld meg a DAO/Room callbacket és
a NativeHomeRepository/NativeCoachTargets útját. Készíts hasznos query-trace
hibaüzenetet a0/200/1500 fixture-hez, izoláld a setup/background queryket. Ha valódi
dupla terméklekérdezés, azt bizonyítékkal jelentsd a fő munkásnak, ne növeld vakon
2-re az elvárást. A bounded history/no-photo/no-set hidratálás ellenőrzése marad.

A feladat nem három piros pipa kozmetikázása: téves fixture/scheduler javítását
bizonyítsd, valódi termékhibát ne fedj el. A jelen integrált bázison nem futott új
suite; a korábbi bukás nem automatikusan friss reprodukció. Ha futás nélkül
valami bizonytalan, külön jelöld; végső runtime gate az integrátornál marad.
