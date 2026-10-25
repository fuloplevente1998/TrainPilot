# Native Health foreground auto-refresh – branch validation

## 2026-10-25 – végső review-javítási státusz (P1/P2)

**A [3e2a81bd review](https://github.com/fuloplevente1998/TrainPilot/blob/3e2a81bd/docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md) mindkét jelzett hibája javítva a saját branch-en.**

- **P1 main-only admission:** `HealthDashboardViewModel.launchRefresh` Main-lopperen vizsgálja az aktív `PhoneWorkoutExecution` állapotát, majd az IO-s `safeOffMain` ellenőrzi az ownership/backup/restore feltételeket. `NativeHealthRefresh.commitRead` a valódi HC/Samsung SDK callbackből `NativeHealthForegroundAdmission.publish`-t használ: Main.immediate check **a metadata-mutex előtt**, IO-safe és dataset/screen/permission ticket-validáció **az őrzött naplóíráson belül**. Nincs Main→IO→Main blokk a lockban.
- **P2 ZIP staging:** `transactionDir(token)` és a teljes előkészítés `NativeHealthBackupGate.begin` után biztos `try/finally` blokkban van, a cleanup null `stagingDir` esetén sem akad meg. Valódi `archive_transactions` helyére tett fájl okozta mkdir/IOException regresszió `Result.failure`-t ad és utána egy érvényes JSON export bizonyítja a zárolás megszűnését.
- **Végső code/test SHA:** `7cb2c11332ab39f2d78831e9f79f5e0c5f533fd7` (további commitok kizárólag átadó dokumentáció).
- **CI [#40243229800](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40243229800): SUCCESS + SUCCESS**, két tényleges XML-riportból: **30/30 JVM (0 fail/error/skipped)** és **4/4 Android 35 Compose UI (0 fail/error/skipped)**.
- Eredeti fájlok: [JVM XML #12617517971](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40243229800/artifacts/12617517971), [UI XML #12617558453](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40243229800/artifacts/12617558453).
- Pontos javítási fájlok a 8c00b46 review-bázishoz képest: `HealthDashboardViewModel.kt`, `NativeHealthForegroundAdmission.kt`, `NativeHealthRefresh.kt`, `BackupRestoreManager.kt`, `NativeHealthAutoRefreshBackupTest.kt`, `NativeHealthAutoRefreshUiTest.kt`.
- **Integrátori feltétel változatlan:** a teljes migrációs release gate és valódi fizikai HC/Samsung read/permission/revoke próba nincs ezen az ágon igazolva; a WebView `rf250Sync` full/incremental paritás külön. Nincs merge/main/migration írás, a #244 PR Draft.

**Részletes integrátori delta-review:** `docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_FIX_DELTA.md`.

---


## HITELLESÍTETT ZÁRÓÁLLAPOT – 2026-10-25

**Health-specifikus UI és JVM CI: sikeres, nem többé „pending”.** A következő sorok a korábbi, kronologikusan megőrzött debugnapló-bejegyzéseknél frissebbek.

- **Utoljára ellenőrzött kód/teszt HEAD:** `5d89db49f57eb0867a526f522e201b4d3d72c8f9`. Branch: `feat/native-health-auto-refresh`; Draft [PR #244](https://github.com/fuloplevente1998/TrainPilot/pull/244) → `feat/native-android-migration`.
- **GitHub Actions [#40239961158](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961158) összesen SUCCESS.** A GitHub artifactból kibontott eredeti XML-ek összesítése: **29/29 JVM PASS (25 új Health auto-refresh + 4 korábbi NativeHealthRefresh), 0 failure/0 error/0 skipped**. Osztályok: `NativeHealthAutoRefreshAdmissionTest` 5, `NativeHealthAutoRefreshPublicationTest` 5, `NativeHealthAutoRefreshBackupTest` 2, `NativeHealthAutoRefreshTest` 9, `NativeHealthRefreshWindowTest` 4, `NativeHealthRefreshTest` 4. [Eredeti JVM jelentés #12616966543](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961158/artifacts/12616966543).
- **Valódi Android 35 emulátoros Compose UI: 1/1 PASS**, 0 failure/0 error/0 skipped; `NativeHealthAutoRefreshUiTest.realAutoSwitchPersistsInExistingHealthJournalAndSurvivesDestinationReentry`; XML-ben 12.007 s, timestamp `2026-10-25T22:31:11`. [Eredeti UI jelentés #12616857420](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961158/artifacts/12616857420).
- **Utolsó UI-regresszió oka és javítása:** az előző [#40237199407](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40237199407) futás `ComposeTimeoutException` hibával leállt, mert a LazyColumn még nem állította elő az offscreen kapcsolót, a teszt viszont már az elemre várt. A `5d89db49` commitban a teszt előbb a listára vár, majd `performScrollToNode(hasTestTag("health_auto_refresh_switch"))` hívással odagörget, és csak ezután ellenőrzi a kapcsolót. A következő valós emulátoros futás zöld.
- **Független további kapuk:** a teljes natív [#40239961291](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961291) a záróellenőrzés kezdetén még dolgozott, a korábbi teljes migrációs regresszió `997 test, 7 failure`; a Quick [#40239961174](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961174) piros a hiányzó npm-lock/manifest miatt. Ezektől a *Health-specifikus* célzott kapu sikere nem lesz teljes projektszintű siker.
- **Korlát:** a valós, fizikai Samsung/Health Connect SDK-szinkron, az offline és jogosultságvisszavonási edge-case, valamint a WebView `rf250Sync` inkrementális/30 napos backfill paritás **nincs készüléken bizonyítva, illetve nincs teljesen megvalósítva**. A PR továbbra is Draft, merge/issue-close nélkül.
- **Eredeti átadó is megőrizve ezen az ágon:** `docs/plans/NATIVE_HEALTH_AUTO_REFRESH_HANDOFF.md`, a 2026-10-25-i igazolt státusszal.

---


Updated: 2026-10-25. Development branch `feat/native-health-auto-refresh`, pinned integration base `0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20`. Draft PR #244 targets `feat/native-android-migration`. Do **not** merge without native test/device acceptance.

## Actual native call path

`HealthDashboardScreen` lifecycle → `HealthDashboardViewModel` → persisted `HealthJournalStore.preferences().autoOnOpen` → 10 min / in-flight `NativeHealthAutoRefreshGate` → fail-closed workout/ownership, backup and restore admission → `NativeHealthRefresh.readDay` → existing `HealthConnectRepository` / `SamsungHealthRepository` SDK calls → short protected journal publication → re-read selected day without moving its history filter.

Manual refresh bypasses the auto interval; no automatic permission prompt. Active/paused workout, finalization, ownership transfer, backup staging or dataset replacement disallow auto. Existing archive operations are marked by `NativeHealthBackupGate` for the full SAF read/write/verification window. The durable restore epoch continues to belong to `NativeDataReplacement`. The SDK IPC never holds the restore mutex.

## Data safety notes

* Source day interval is timezone-aware and clipped to `Instant.now()` for today; future days are rejected. DST 23/25 hour days retain the correct local-midnight bounds.
* A partial Health Connect grant cannot be treated as a complete daily measurement; the first partial day now saves real fields as `partial` and preserves unavailable fields. Later partial reads preserve older values where missing; no fabricated zero.
* Samsung Health permission changes are checked again before a protected journal write. Failure/denial keeps existing daily fields via `failedFields` merge.
* A live foreground cancellation, changed provider, changed permissions, changed dataset generation, changed timezone or date invalidates the publication ticket. Restore may not resurrect stale SDK values.
* No new provider client, background worker, database schema, backup format, or version bump was introduced.

## Verification matrix

* `NativeHealthAutoRefreshTest`: 8 pure gate cases (opt-in, throttle, manual, no/partial grant, concurrency, stale identity).
* `NativeHealthAutoRefreshAdmissionTest`: 5 session/ownership/restore policy cases.
* `NativeHealthAutoRefreshPublicationTest`: 5 publication/lifecycle/prefs cases.
* `NativeHealthAutoRefreshBackupTest`: 2 backup/first partial-day cases.
* `NativeHealthRefreshWindowTest`: 4 DST/future-window cases.
* `NativeHealthAutoRefreshUiTest`: 1 real Compose navigation-and-persistence scenario.

New cases total: **24 JVM test methods + 1 UI test method**, pending actual Gradle/emulator execution. Script/static checks do not constitute JVM or provider SDK validation. A physical Health Connect/Samsung permission, revoke, foreground navigation and data comparison is still required for complete device acceptance.

## Main parity boundary

This feature provides **foreground/on-open latest-day real provider refresh**, not a reimplementation of the old WebView `rf250Sync` incremental change-log/30-day full backfill. The latter remains a separate parity task, not claimed by this branch. Older WebView entry points, canonical workout refresh and Wear ownership were not rewritten.

## CI diagnosis and integration additions

The initial existing PR validation at `70c4f584` ran (GitHub Actions `40233270552`, `40233270556`).
* Native APK workflow failed in pre-existing `WearSyncRepository.kt:106` during `:app:kaptGenerateStubsDebugKotlin`, Kotlin FIR internal analysis exception; it did **not** get as far as JVM tests. A narrow explicit `mutate<Unit>` type annotation is added on this branch to validate the workaround.
* Quick validation stopped at `actions/setup-node` because the native migration branch lacks `package.json` and npm lockfile; the native migration does not own the main web/npm tree. No unrelated web toolchain was copied into this feature branch.

The separate `NativeHealthBackupGate` now covers JSON/ZIP SAF import/export **and** native Drive backup/sync staging. `HealthJournalStore.healthDay` persists a first partial HC daily row without inventing absent metric values. `NativeHealthRefresh.dayWindow` clips today's future end and is tested across Budapest DST. A transient permission-dialog pause does not cancel explicit Samsung permission consent; STOP/dispose still invalidates stale publication.

### Native compilation follow-up

The second native PR run `40233832132` progressed past the FIR analyzer and reported two independent pre-existing Kotlin source errors: a stray brace in the Wear discard-command catch block (`WearSyncRepository.kt:247`), and a non-property constructor parameter reference in backup erase (`BackupRestoreManager.kt:590`). Both are repaired by minimal changes on the feature branch, without changing the Wear protocol, database schema, or package/version metadata. New build verification is required after these fixes.

The native compiler's subsequent detailed diagnostics located the real source-level errors: the `WearSyncRepository` lambda's closing brace belonged after `when(action)`, rather than before the discard `catch`, and the backup erase method must use `appContext`. Both have now been corrected; a brace-balance source check returned zero unmatched blocks. Prior CI failures are not green build evidence.

### Fourth native build result and UI test API repairs

Existing native gate `40234213957` compiled the phone application and Wear debug sources, then failed at `:app:compileDebugAndroidTestKotlin`. Independent source errors: unsupported explicit `assertExists`/`assertDoesNotExist` top-level imports in two UI tests and a `ComposeContentTestRule`-vs-`ComposeTestRule` helper signature that disagreed with `createEmptyComposeRule()`. These were corrected in test sources only, preserving all scenarios. Fifth gate checks those fixes.

### Fifth gate test-compilation result

The fifth native gate `40234621721` successfully compiled **both debug phone/Wear app source and the Compose Android instrumentation APK**; it then failed in `:app:compileDebugUnitTestKotlin` because existing `NativeProfileHistory128Test.kt` referenced a removed `ProgramDayEntity.id` instead of `compositeId`, and a `this.id` inside `runBlocking` resolved to its coroutine scope rather than the test fixture. Fixed the two exact test references without weakening assertions. A new gate is needed for actual test execution.

Additional consent lifecycle hardening keeps a manually requested Samsung permission flow alive across a transient external permission activity's STOP, while destination disposal always cancels it.

### Sixth native CI: actual JVM results and cross-suite fixture repair

Run `40235050303` compiled phone/Wear debug APK and Compose androidTest APK successfully, then executed `972` JVM tests with `88` failures. The actual uploaded `TrainPilot-Native-Tests-d2d4e515...` XML report confirms **24/24 new Health JVM tests passed** and **4/4 existing NativeHealthRefresh tests passed**.

Inspection of the unrelated Coach/Profile/Planner failure clusters found a **test classpath resource collision**: `src/test/resources/native-planner-main-7473.json` held parity test *cases* and masked `src/main/resources/native-planner-main-7473.json` which holds the actual 106-item native planner policy *meta*. Test resource renamed to `native-planner-cases-7473.json`, and `NativePlannerTemplateParityTest` reads the renamed file; the production policy remains unchanged. Two unrelated JUnit test functions gained explicit `Unit` return types to meet JUnit4 signatures without changing any assertion. Remaining unrelated failures, if any, require separate diagnosis.

### Health-only CI (isolated from unrelated migration failures)

`native-health-auto-refresh.yml` adds a two-part, non-release CI for this branch: focused JVM Health tests and a disposable Android 35 emulator running the real `NativeHealthAutoRefreshUiTest`. The first isolated run `40236477298` passed the focused JVM job. Its UI job compiled both APKs and booted the emulator, but failed **before instrumenting the app** because `reactivecircus/android-emulator-runner` executes each newline of `script` in a separate shell (`cd android` did not persist, hence `./gradlew: not found`). The script now executes one shell command with `cd android && ./gradlew ...`, with a follow-up run required.

Additionally, the 10-minute auto cooldown is process-shared across navigation destination recreation and keyed by actual provider/read date; switching historical day filters cannot accidentally reset cooldown. The test suite includes a focused case for changing day/provider. JVM and UI evidence must be from the latest code head before accepting the PR.
