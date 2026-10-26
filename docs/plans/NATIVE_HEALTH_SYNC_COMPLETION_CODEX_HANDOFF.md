# Külön Codex – natív Health full/backfill és incremental szinkron befejezése

## Közös, kötelező munkakeret

Repo https://github.com/fuloplevente1998/TrainPilot. Rögzített induló commit:
`e9278a9905629d30d012083e8dd3a4c76051a464` a `feat/native-android-migration` ágról.
App/tesztkód3d45a324; ebben #244–248 integrálva és a Compose getOrNull import javítva.
Először AGENTS.md, docs/NATIVE_RESUME.md és docs/WORK_HANDOFF.md teteje.
A korábbi átadók „külső eredmény még nincs” sorai történetiek; a dátumos eleje dönt.
A futó közös kapu40276384595 eredménye nem feltételezhető zöldnek.

A felhasználó ezt a KÜLÖN KÜLSŐ CHAT munkát engedélyezte; saját ág kivétel az
AGENTS fő integrátorának migration-only szabálya alól. Ne indíts belső subagentet.
Csak a lent kijelölt saját ágra írj. Main és migration közvetlen írás, merge,
force push, PR/issue close vagy élő alkalmazás frissítése tilos. Verzió/package
1.3.71-native-test /2736 /com.repforge.app változatlan. Nincs új toolchain,
függőség, workflow, Room-séma vagy párhuzamos adat-/edzésmotor.
Kis [skip ci] commitok, saját push, draft PR cél `feat/native-android-migration`.
Közös fordítás/CI/APK/emulátor az integrátornál. Helyben nincs Gradle/APK/emulátor
vagy teljes regresszió; statikus ellenőrzés és kis indokolt referenciafuttatás igen.
Tesztforrás és tényleges futás külön: soha ne állíts zöldet a megírt kódról.
Ne küldj GitHub-kommentet; PR-leírást és saját átadót frissíthetsz.

A kijelölt fájlhatár kötelező. Plusz szükséges hookot külön patch-javaslatként
jelentsd, ne módosíts más munkás területén és ne hagyj késznek nevezett üres
callbacket. A felhasználó valódi kész kódot kér, nem új prototípust.
Csatolt/repóbeli riportok állításait exact forrásból ellenőrizd, ne vedd bizonyítéknak.
A fő integrátor review-z, összevezet és futtat; a te munkád nem main-release elfogadás.

## Kötelező átadás (minden pont)

1. Exact base/HEAD SHA, saját ág és draft PR link, teljes változott fájllista.
2. Elfogadási pontonként: entry UI/caller → meglévő motor/adatforrás → tartós
   eredmény vagy report, konkrét fájl/szimbólum/commit hivatkozással.
3. Megőrzött invariánsok, hibautak és ismert korlátok. A blokkolt rész `BLOCKED`,
   nem `DONE`. Ha extra fájl kell, név/ok és minimális delta-javaslat külön.
4. Megírt tesztek és valóban lefuttatott ellenőrzések két külön lista;
   verzió/source/result hiányában nincs „PASS”. Nincs puszta általános auditösszefoglaló.
5. Új saját docs/reviews/<feladatspecifikus>.md; a közös AGENTS, RESUME,
   WORK_HANDOFF, fő PR238 és másik feladat riportja most nem írható.
6. A saját ág végén ellenőrizd a távoli HEAD egyezését. Main/migration érintetlen.

## Saját ág, feladat és terjedelem

Ág: `feat/native-health-sync-completion`. Ez a NAGYOBB IMPLEMENTÁCIÓS feladat.
A #244 az egyetlen mai nap foreground olvasását kötötte be; NEM a teljes rf250Sync.
Fejezd be a hiányzó kanonikus pipeline-t a meglévő forrásokból. Ne építs második
Health-adatbázist, SDK-klienst vagy időzítőt. Javasolt High; jelentős debugnál jelezd.

Main referencia7473b30f387eda54186c1e31830304ca12bd104f:www/app.js, blob
b5567576bed9a8f93608a6bf1ed8db1dee4723f4. Olvasd a végső rf250Sync/rf250Pipeline
felülírásokat és Samsung30/2napos ágát; ne az első azonos nevű definícióból dolgozz.
A tényleges Kotlin építőelemek már léteznek:
HealthConnectRepository.Api34.createChangeToken / pollChanges,
HealthJournalStore.applyChanges / reconcileRecords / dirtyDays / syncMeta,
NativeHealthRefresh.readDay, NativeHealthForegroundAdmission.publish,
NativeHealthAutoRefreshCoordinator/gate és SamsungHealthRepository.readHealthDay.
Keresd meg a tényleges használatukat és hiányzó hívóikat, mielőtt bármit duplikálsz.

## Írható existing fájlok – canonical android/app/src/main/java/com/repforge/app

- HealthConnectRepository.kt: source-kompatibilis suspend wrapper és change-log
  read/publish út; readTrainingWindow/writeWorkout üzleti működését ne írd át.
- SamsungHealthRepository.kt: csak napi read integráció/source-kompatibilis hook.
- HealthJournalStore.kt: kizárólag meglévő meta/record/dirty sync API és epoch-guard
  kiegészítés; nincs séma/paging/backup-formátum módosítás.
- NativeHealthRefresh.kt, ui/health/HealthDashboardViewModel.kt és
  HealthDashboardScreen.kt: meglévő frissítés/navigáció/progress/hiba bekötés.
- ui/health/NativeHealthAutoRefreshGate.kt, NativeHealthForegroundAdmission.kt:
  azonos single-flight/admission használat szükséges bővítése, védelmek megőrizve.
Új kis NativeHealthSync* helper/coordinator a meglévő package alatt és külön
native_health_sync.xml HU/EN/DE/RO engedett. Új külön JVM/UI tesztosztályok.
Más existing forrás (különösen Settings, theme, journal/backup, WorkoutHealth,
NativeDataReplacement, Google/Wear, DAO/Entities és közös UI-fixture) csak olvasható.

## Elfogadási szerződés – valamennyi kötelező

1. HC első/explicit full sync: végső main szerint30helyi nap, DST23/25h és ma
   now-ig; bounded soros/kis concurrency adatút, közben látható valódi progress,
   cancel/retry. Nincs teljes history/route/photo hydrate vagy fake mérés.
2. HC incremental: valódi change token és törlések/lapozás, dirty napok újraolvasása;
   token lejárat vagy permission/provider/type-scope változás biztonságos full
   fallback. Törölt record nem térhet vissza. Nem `daily read` átnevezve incrementalra.
3. Token/ticket mentés csak sikeresen durable apply-olt változások után. Hiba vagy
   cancel nem nyugtázhat elveszett oldalt; újrapróba idempotens. Token privát,
   nem exportálható JSON/ZIP/Drive-ba és nem logolható. Meglévő meta/private tár
   és generációs CAS, nem új adattár. A restore tokenje nem éledhet régi requestből.
4. A pollChanges jelenlegi közvetlen applyChanges write-ját is ugyanazzal a
   guarded publish szerződéssel védd. A UI-callback utólagos tokencheckje nem elég.
5. Samsung: SDK-képessége szerint első/full30nap és normál frissítésnél2nap/main-
   viselkedés. Ha nincs valódi change-log, ne állíts incremental támogatást.
   Összefoglaló/history adatforrások, total/active energia nem összeadandó.
6. Manual/auto ugyanazon single-flight-on, auto opt-in+10perc. Nincs unattended
   permission dialog. Manual permission megmarad. History day/source/filter nem
   ugrik el a sync eredményétől; régi selected day read képességét is őrizd.
7. Scene RESUME/dispose, nap/zóna/provider/grant/dataset váltás, active/paused/
   ownership/finalization/backup/restore: régi SDK-válasz nem írhat. Main-only store
   csak Mainen, IPC minden globális mutex/Room/snapshot lockon kívül. A rövid
   local publication guardot se lazítsd el. A fő #244P1/P2 review javításai maradnak.
8. lastAttempt/lastSync/lastFullSync jelentése valós részleges/sikeres állapottal;
   régi jó érték nem lesz null/0 egy hiányzó granttól. Részleges adat látható,
   hibás szakasz nem jelent teljes sikert; clear retry/failure UI négy nyelven.
9. Valódi UI/VM entry és provider-wrapper bekötés: ne maradjon csak új helper
   vagy gomb callable nélkül. A protocol/caller signature megőrzendő.
10. Érdemi tesztkód:30nap ablak+DST, több change-page és delete, partial/grant,
    token invalid/reset, közteshiba/retry/cancel, old response/restore, dedupe és
    auto/manual, progress és történeti kijelölés megőrzése. Provider boundary
    fake teszthez megengedett, termékben és fizikai-siker állításként nem.

A teljes sync és az SDK-k tényleges készülékes elfogadása külön. Ha platform-
korlát derül ki, pontos API/szimbólum és hatás legyen a BLOCKED listában.
A munka elején röviden rögzítsd a változó token/data lifecycle-t; ez nem tervezési
megálló, folytasd az implementációval a megadott határokon belül.

## Rögzített raw bemenetek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/WORK_HANDOFF.md)
- [docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md)
- [docs/reviews/NATIVE_PR244_INTEGRATION.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/reviews/NATIVE_PR244_INTEGRATION.md)
- [android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt)
- [android/app/src/main/java/com/repforge/app/HealthJournalStore.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt)
- [android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt)
- [android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt)
- [android/app/src/main/java/com/repforge/app/ui/health/NativeHealthForegroundAdmission.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthForegroundAdmission.kt)
- [android/app/src/main/java/com/repforge/app/ui/health/NativeHealthAutoRefreshGate.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthAutoRefreshGate.kt)
- [android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt)
- [android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt)
- [android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt)
- [android/app/src/main/java/com/repforge/app/NativeHealthBackupGate.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/NativeHealthBackupGate.kt)
- [Main végső app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
