# Natív Health full/incremental szinkron – kötelező átadás

2026-10-26. Draft [PR #251](https://github.com/fuloplevente1998/TrainPilot/pull/251),
saját ág `feat/native-health-sync-completion`, cél `feat/native-android-migration`.
Feladat: a `5224ada0` dokumentumverzió teljes
[NATIVE_HEALTH_SYNC_COMPLETION_CODEX_HANDOFF.md](https://github.com/fuloplevente1998/TrainPilot/blob/5224ada0/docs/plans/NATIVE_HEALTH_SYNC_COMPLETION_CODEX_HANDOFF.md).

## 1. Exact forrás és hatókör

- Rögzített implementációs base: `e9278a9905629d30d012083e8dd3a4c76051a464`. Az ág ebből indult;
  a linkelt dokumentum commitja nem az implementációs base.
- Kód/teszt HEAD: **`01bf93da07c781ebe626b408c9b8956e37b0ccba`**. Ez a riport ezután készült docs-only deltában.
  A végső teljes átadási HEAD-et a PR-leírás és a végső átadási üzenet rögzíti;
  a riport saját commitjának hashét nem lehet önmagába beleírni.
- Részcommitok: `cc1d1286b7b677f983bf8e24a16cfe709e75560c` motor/adapter/store;
  `9ba6f64c49829dd5417c615ff33a8f83172cf3d6` VM/UI/locale;
  `d52da853038803859ed1c1f053866b45eaa60348` tesztforrás;
  `01bf93da07c781ebe626b408c9b8956e37b0ccba` permission RESUME + history-invalidation regresszió.
- Nem változott a natív `com.repforge.app`, `1.3.71-native-test` / `2736` Phone/Wear.
  A pinned natív ágban nincs `package.json`; `SOURCE_VERSION.json` továbbra is
  a meglévő `1.3.71` / `2735` webes referenciát tartalmazza. Egyik metaadat sem módosult.
- Nincs workflow/függőség/toolchain/séma/paging/backup-formátum-változás,
  külön SDK-kliens, adatbázis vagy időzítő. Nincs belső subagent.
- A közös AGENTS/NATIVE_RESUME/WORK_HANDOFF, PR238 és másik feladat riportja változatlan.

Teljes változott fájllista (15):

- `android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt`
- `android/app/src/main/java/com/repforge/app/HealthJournalStore.kt`
- `android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt`
- `android/app/src/main/java/com/repforge/app/NativeHealthSync.kt`
- `android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt`
- `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt`
- `android/app/src/main/res/values-de/native_health_sync.xml`
- `android/app/src/main/res/values-en/native_health_sync.xml`
- `android/app/src/main/res/values-ro/native_health_sync.xml`
- `android/app/src/main/res/values/native_health_sync.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeHealthSyncUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeHealthSyncJournalTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeHealthSyncTest.kt`
- `docs/reviews/NATIVE_HEALTH_SYNC_COMPLETION.md`

## 2. Token és adat életciklusa

A valódi UI belépő [VM.syncHealth](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L114) / [VM.launchRefresh](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L332) → [NativeHealthRefresh.sync](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt#L55) → [NativeHealthSync.run](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthSync.kt#L54).
A journal és az SDK-adapterek maradnak az egyetlen adatforrás.

HC első/full olvasás előtt készül egy jelölt SDK-token. A 30 helyi nap soros
olvasása után a token változásoldalai is feldolgozódnak, és a közben érintett
napok újraolvasódnak. Normál HC-frissítés a korábban mentett token valódi
változásoldalait alkalmazza, majd a dirty napokat és a legutóbbi két napot olvassa.
Törlésnél a raw tombstone azonnal tartós; main mintájára teljes 30 napos
újraolvasás is történik, mert ismeretlen/legacy törlési ID-hez nem biztos,
hogy helyi nap rendelhető. A kiválasztott régi nap kézi olvasása megmarad: [NativeHealthRefresh.readDay](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt#L47).

Tokenváltás kizárólag a sikeresen alkalmazott összes oldal és nem részleges
napi eredmények után, [store.nativeSyncCheckpoint](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt#L683) tranzakciójában történik. Hiba/cancel
után az előző token marad; a már alkalmazott oldal idempotensen újrajátszható.
A dirty jelzések addig tartósak, amíg az adott teljes napi összesítés sikeres.
A privát meta-kulcs epochhoz és revízióhoz kötött, scope-ja provider + időzóna +
engedélyezett típuskör + protokollverzió. Restore/erase/provider-váltás után a
régi epoch tokenjét az új kérés nem használja, és régi kérés nem mentheti vissza.

A token nem kerül a projection/page/JSON export explicit whitelistjeibe;
JSON/ZIP/Drive a meglévő `exportSnapshot` kimenetét használja. A rendszerbackup
is tiltott (`allowBackup=false`, adatbázis-kizárások). Nincs tokenlog vagy
UI-tokenmező; a checkpoint/page `toString()` is kitakarja a privát állapotot.

## 3. Elfogadási pontok – caller → motor/adatforrás → tartós eredmény

Minden „IMPLEMENTED” forrásszintű állapot. A fordítás/runtime **UNRUN**;
nem állítunk készülékes elfogadást vagy zöld CI-t.

| Pont | Konkrét út és eredmény | Állapot |
|---|---|---|
| 1. HC első/explicit full | [HealthDashboardScreen](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) teljes 30 nap gomb → [VM.syncHealth](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L114) → [NativeHealthSync.run](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthSync.kt#L54) `recentDays/window` → meglévő HC napi aggregátor és `reconcileRecords/healthDay`; 30 helyi nap, DST 23/25h, ma az olvasáskori now-ig. Soros, tényleges nap-/oldalszámláló, cancel/retry. | IMPLEMENTED; runtime UNRUN |
| 2. HC incremental | Ugyanaz a normál/auto belépő → `pollChangePageSuspend` → [Api34.pollChangePage](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt#L1048) valódi ChangeLogsRequest, hasMore/nextToken, upsert/delete → [store.applyChanges](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt#L358), tartós dirty napok újraolvasása. Lejárt/érvénytelen token vagy scope-változás teljes fallback. Törléskor is biztonságos full-reconcile. | IMPLEMENTED; SDK UNRUN |
| 3. Durable token/ticket | [NativeHealthSync.run](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthSync.kt#L54) jelölt tokenje → [store.nativeSyncCheckpoint](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt#L683), generáció + régi revision CAS, egy SQLite-tranzakció a sikeres időbélyegekkel. Részleges nap/hiba/cancel nem léptet tokent. Token nincs exportban. | IMPLEMENTED; SQLite tesztforrás megírva, UNRUN |
| 4. Régi közvetlen poll-write | [Api34.pollChanges](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt#L1079) → ugyanaz a [Api34.pollChangePage](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt#L1048) → `Access.publishRead` → a caller `readCommit` publication fence-e → [store.applyChanges](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt#L358). Nincs közvetlen, megkerülő applyChanges az SDK-olvasás után. | IMPLEMENTED; dispatcher runtime UNRUN |
| 5. Samsung | Normál/explicit full ugyanazon runneren → [Samsung.readHealthDayScoped](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt#L164) → meglévő SamsungSdkReader napi summary/history és `samsungDay`. Első/full 30, később 2 nap. Nincs Samsung change-log/token vagy incremental-sikerállítás. Energia üzleti számítás változatlan. | IMPLEMENTED; fizikai SDK UNRUN |
| 6. Közös single-flight | [VM.launchRefresh](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L332) továbbra is a processzszintű NativeHealthAutoRefreshCoordinator/gate kapuját használja; opt-in + 10 perc, manual bypass. Auto nem kér engedélyt. A kézi HC permission eredménye RESUMED állapotban folytatódik; Samsung explicit consent után a hosszú olvasás már nem kap lifecycle-kivételt. History query/source/filter nem cserélődik sync kimenetére. | IMPLEMENTED; Compose/VM futás UNRUN |
| 7. Régi válaszok | `NativeHealthRefresh.execute` lifecycle epoch, coroutine-cancel, screen guard, nap/zóna/provider/grant/generation/NativeDataReplacement continuation → meglévő Main probe + IO-safe admission + rövid backup/local publish guard. HC régi daily-válasz [store.nativePublishAtChangeRevision](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt#L341) atomikus helyi ellenőrzésén bukik el, ha közben change-page alkalmazódott. SDK/permission IPC a helyi publication lockokon kívül; Main-only execution probe a metadata lease előtt. | IMPLEMENTED; versenytesztek UNRUN |
| 8. Valós állapot | Admitted pipeline-start → `nativeSyncAttempt`; részleges beérkező mezők a meglévő HC/Samsung merge útján láthatók. Teljes siker → `nativeSyncCheckpoint` lastSyncAt; csak full-siker emeli lastFullSyncAt-et. Korábbi jó időbélyeget hiba/granthiány nem nulláz. Négy locale, részleges/hiba/cancel/retry UI. | IMPLEMENTED; UNRUN |
| 9. Bekötés | [HealthDashboardScreen](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) `onSync/onFullSync/onCancelSync/onRetrySync` → [VM.syncHealth](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L114)/[VM.launchRefresh](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L332) → [NativeHealthRefresh.sync](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt#L55) → valódi provider-wrapper; történeti refresh továbbra is [NativeHealthRefresh.readDay](https://github.com/fuloplevente1998/TrainPilot/blob/01bf93da07c781ebe626b408c9b8956e37b0ccba/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt#L47). A produktív callbackek bekötöttek. | IMPLEMENTED; UI/runtime UNRUN |
| 10. Tesztforrás | NativeHealthSyncTest 18 JVM + NativeHealthSyncJournalTest 11 SQLite/Robolectric + NativeHealthSyncUiTest 1 Compose; alább külön részletezve. Meglévő #244 tesztek érintetlenek. | MEGÍRVA, NEM FUTTATVA |

## 4. Megőrzött invariánsok, hibautak és korlátok

- Egyetlen meglévő journal/SDK-adapter. Nincs history/route/photo teljes hydrate,
  workout-export vagy `readTrainingWindow/writeWorkout` üzleti átírás.
  A két meglévő wrapper és két Api34 üzleti függvény byteazonos a pinned base-szel
  (külön forrás-összehasonlítás lefutott).
- #244 P1 Main/IO felosztás és rövid local publication guard megmarad;
  #244 P2 backup cleanup fájlját nem érintettük. A sync requestek a valódi
  Main execution probe-ot hívják, a provider callback az IO publication utat.
- SDK/permission IPC nincs az új metadata/journal publication blokkokban.
  A VM új prepare/load permission/capability probe-jai is a lease előtt futnak.
- Törlés-oldal és a hozzá tartozó dirty mark ugyanazon meglévő SQLite-tranzakcióban
  tartós. Az upsert az eredeti timestamp/tombstone szabályt őrzi. A napi válasz
  és lokális record publication a change-revision összehasonlítással együtt
  ugyanazon journal-monitor alatt történik: nincs check/write rés a törléssel szemben.
- HC hiányzó jogosultság miatti warning és Samsung denied/failed field részleges
  állapot. Korábbi jó érték megmarad; token/full-success nem nyugtázódik.
  Permission/provider/restore/lifecycle/nap/zónaváltozás elutasítja az elavult flightot.
- SDK Future-k a meglévő adapterben blokkolók, legfeljebb 30 s várakozással.
  Cancel azonnal érvényteleníti a publicationt, de az SDK-hívást fizikailag nem
  szakítja félbe; a közös flight csak az unwinding után enged új requestet.
- Bounded foreground budget: 2000 rekord/page, legfeljebb 500 változásoldal,
  legfeljebb 530 érintett/dirty nap és 530 nap hosszú változás-intervallum.
  Túllépés hiba, nincs token-nyugta vagy csendes truncation. Nagy change-lognál
  az explicit full új tokent és 30 napos snapshotot készít. 530-nál több tartós
  dirty napnál külön batchelt feldolgozás lehet szükséges; ez ismert ritka korlát,
  nem támogatott teljes history-import. Nem állítunk sikeres syncet ilyen adatra.
- A Main probe/permission probe a rövid metadata lease előtt fut; a meglévő
  backup/local guard és epoch-ellenőrzések a tényleges írásnál ismét futnak.
  A provider jogosultsági állapota nem tehető a rendszerrel közös atomi SQLite-
  tranzakcióvá; az adapterek a kérések között és publication előtt újraellenőrzik.
- Samsung SDK nem nyújt ehhez valódi change-logot. Ez nem megkerült blocker:
  a kért 30/2 napos summary/history út működését kötöttük be, nem nevezünk napi
  olvasást incrementalnak.

**BLOCKED – futási/elfogadási bizonyíték:** a közös Kotlin/Gradle/JVM/Compose/
APK és fizikai HC/Samsung bizonyíték nem készült el ebben a munkában, a feladat
szerint a fő integrátornál marad. Ez nem DONE/PASS. Konkrét új, határon túli
hook szükségessége jelenleg nem igazolt; külön patch-javaslat nincs.
Ha a foreground budget fölötti dirty-adathalmaz termékkövetelmény, minimális
következő delta a már engedélyezett `HealthJournalStore` meta/dirty API-jában
kurzoros dirty-batch + runner batch-drain, nem második adatmotor vagy sémamódosítás.

## 5. Megírt tesztek – tényleges futás NÉLKÜL

Forrás: `01bf93da07c781ebe626b408c9b8956e37b0ccba`, app `1.3.71-native-test` / `2736`.

- `NativeHealthSyncTest`: 18 JVM-eset. 30 nap + mindkét DST + now;
  snapshot előtti token/catchup; több change-page és dirty-dedupe;
  snapshot közbeni törlés; expired token; grant/type/zone/provider scope-reset;
  missing token/explicit full; köztes oldalhiba/idempotens retry;
  partial grant/failed day; cancel; old scene/restore/workout guard;
  checkpoint előtti permission change; Samsung 30→2 változáskövetés nélkül;
  progress csak befejezett munkából; history-query váltás invalidálása;
  produktív auto/manual gate; ismeretlen törlés full fallback; nonadvancing page.
- `NativeHealthSyncJournalTest`: 11 SQLite/Robolectric-eset. Privát token
  reopen/export/projection/page; revision CAS; attempt/partial megőrzi a jó
  időbélyegeket; incremental nem emeli a full-időt; pending/completed restore;
  provider/erase epoch; replay/delete/tombstone; teljes régi interval dirty;
  rollback oldalhiba; pre-delete napi válasz; korábbi Access.publishRead guard.
- `NativeHealthSyncUiTest`: 1 Compose komponens-eset. Valódi normal/full/
  cancel/retry kontrollok, progress-számok, partial error, history/source state.
  Ez komponens-bekötési teszt; a produktív VM/SDK/permission teljes end-to-end
  készülékes futását nem helyettesíti, és nem fake provider-siker.
- A meglévő #244 admission/publication/backup/lifecycle és refresh window
  tesztosztályokat nem módosítottuk és nem futtattuk újra.

## 6. Valóban lefuttatott ellenőrzések

Exact app/tesztforrás: `01bf93da07c781ebe626b408c9b8956e37b0ccba`. Eredmények:

- `git diff e9278a9905629d30d012083e8dd3a4c76051a464 --check`: statikus PASS.
- Saját ellenőrző: 10 Kotlin-fájl string/comment-aware lexikai delimiter check,
  4 locale × 11 XML-kulcs és format placeholder egyezés, fájlhatár-ellenőrzés:
  statikus PASS. **Nem Kotlin parser/compiler, nem típuselemzés.**
- Export-whitelist forrásszintű ellenőrzése: privát meta/token és
  hc_change_revision nincs az exportblokkban: statikus PASS, nem futó SQLite-teszt.
- A két training-window és két write függvény törzsének pinned-base
  összehasonlítása: byteazonos. A közös gate/admission és backup cleanup nincs módosítva.
- Valódi pinned main referencia: `7473b30f387eda54186c1e31830304ca12bd104f`,
  `www/app.js` blob `b5567576bed9a8f93608a6bf1ed8db1dee4723f4`, hash ellenőrizve.
  Node alatt az exact `rf240DayKey/rf240DayBounds/rf240RecentDays` futott
  Europe/Budapest zónával: 30 külön helyi nap, 2026-03-29 23h,
  2026-10-25 25h, mai végpont nem jövőbeli: **referencia PASS**.
  Ez JavaScript-referencia, **nem a Kotlin-implementáció futási bizonyítéka**.
- A végső rf250Pipeline definíciók, journal wrapper, Samsung 30/2 napos felülírás
  és későbbi wrapper célzott exact-source olvasása megtörtént.
- Nem futott: Gradle, Kotlin-fordító, JVM/Robolectric, Compose/emulátor,
  teljes regresszió, APK, fizikai permission/provider próba. Nincs új toolchain.
- A korábban futó közös Actions40276384595 API-lekérdezéskor COMPLETED/CANCELLED,
  head3d45a324c5d31c058353a0e4316e02c9a9133520; nem zöld, nem erre az új kódra
  vonatkozik. Új workflow-indítás nem történt.

## 7. Integrátori következő lépés

Exact saját HEAD/delta review és a változott fájlhatár ellenőrzése után
integráció a fő munkásnál. A canonical native workflow-val ugyanazon integrált
forráson fordítás, a 29 új JVM + 1 új Compose és meglévő #244 tesztek futtatása;
XML-ek valós fail/error/skip ellenőrzése. Ezután HC/Samsung valódi első/full,
permission-szűkítés/bővítés, token-expiry, deletion/offline, scene-cancel,
backup/restore és Phone/Wear készülékes elfogadás. A kötelező készülékes kapu
nélkül nincs main/release elfogadás vagy PR/issue lezárás.

Az ág push-olt; a végső docs-only push után a lokális és távoli HEAD egyezését
ismét ellenőrizzük, ennek exact eredménye a PR-leírásba és a végső üzenetbe kerül.
Csak a saját branchre írtunk, main/migration push, force-push, merge, komment,
PR238 update vagy issue/PR close nem történt.
