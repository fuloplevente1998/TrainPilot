# PR #244 Health auto-refresh – javítási delta a 3e2a81bd review után

2026-10-25. Kiinduló review: `docs/reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md` a `3e2a81bd` commitban.
Felülvizsgált branch-HEAD: `8c00b46dbee2a818fa5acf2de44899748a2817dd`.
**Saját fejlesztési ág:** `feat/native-health-auto-refresh`. Csak ez íródott; Draft PR #244 célja `feat/native-android-migration`.

## P1 – tényleges főszálhoz kötött admission

- `NativeHealthForegroundAdmission.safeOnMain(context)`: canonical `PhoneWorkoutExecution.store(context)` + `commandsSuspended`, `restoreBlocked`, `snapshot` **kizárólag Main**; a korábbi IO-s „Execution requires main looper” hamis elutasítás megszűnt.
- `safeOffMain(context)`: Health Journal pending restore, replacement, ZIP/Drive backup, ActiveWorkoutStore, PhoneOwnership – IO-ból és rövid journal-commit során újraellenőrizhető.
- `HealthDashboardViewModel.launchRefresh`: először Main `safeOnMain`, majd IO prepare/metadata ticket és `safeOffMain`; a korábban minden alkalommal `false`-szá forduló `safe(context)` hívás megszűnt.
- `NativeHealthRefresh.readDay`: Main-preflight a valós SDK-olvasás előtt; IO callbackben csak IO-safe guard.
- **Valódi adapter publication callback:** `NativeHealthForegroundAdmission.publish(context, requireIdentity, writer)`, amit mindkét meglévő HC/Samsung napi SDK adapter a `NativeHealthRefresh.commitRead` hookon át használ. A rövid Main.immediate execution check a metadata mutex **előtt** fut, a második IO-safe check + generation/screen/backup ticket pedig a védett lokális journal writer **belül**. Nincs SDK IPC vagy új worker a lockban, nincs Main→IO→Main kör a lease alatt.
- Nem lazult a canonical `PhoneWorkoutExecution.store` looper-guard, és nincsen hamis provider-mérés.

## P2 – ZIP előkészítés és backup-zár

- `BackupRestoreManager.importZipArchiveFromUri`: az `UUID`- és `transactionDir(token)`-előkészítés a `try/finally` alatt, `stagingDir: File?` biztonságos cleanup-fallbackkel.
- Valódi `archive_transactions` könyvtár helyén fájl miatti `mkdir` IOException: `Result.failure`, `NativeHealthBackupGate.available()==true`; utána egy valódi JSON backup-export újra végigfut.
- Cancellation továbbra sem válik látszólagos sikeres/soft fail adattá. Nem lett módosítva backup-archívum formátum, séma vagy adatmotor.

## Célzott regressziók

- `NativeHealthAutoRefreshBackupTest`: ZIP parent mkdir hibainjektálás és sikeres következő export.
- `NativeHealthAutoRefreshUiTest`: quiet IO→Main admission; direkt IO-looper violation elutasítása; ugyanazon HC/Samsung védett sync publication hook futása; backup/restore/active/paused/finalization/stale ticket elutasítás, nincs journal write.
- A többtesztes Android-emulátor fixture valódi SharedPreferences és in-memory `ActiveWorkoutStore.snapshotFlow` helyreállítást is ellenőriz; a régi kapcsolóteszt nem maradhat aktív edzésbe navigálva.

## Tényleges futási bizonyíték (nem helyettesíti a fizikai provider SDK-t)

- [Health CI #40242390795](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40242390795), head `a58c65f5fc8e1003ed4183f386143532224485c4`: **30/30 JVM PASS**, 0 error/fail/skipped; [eredeti XML #12617590629](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40242390795/artifacts/12617590629). **Android UI 3/4 PASS**, a korábbi navigációs teszt a mesterséges in-memory aktívedzés-állapot után timeoutolt; [eredeti XML #12617741590](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40242390795/artifacts/12617741590). A három új P1 production dispatcher/publish UI-teszt zöld.
- A UI test-fixture isolation javítva a `7cb2c11332ab39f2d78831e9f79f5e0c5f533fd7` commitban. [Health CI #40243229800](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40243229800) **folyamatban**; az aktuális HEAD eredményét külön kell még hitelesíteni.

## Határok és integráció

Nincs `main`/`feat/native-android-migration` írás, merge, issue-close, APK upgrade vagy fizikai Samsung/HC-siker állítás.
A régi teljes migrációs CI közös 6+ hiba, a #246 integrátori összevezetése, és a WebView rf250Sync 30 napos/full/incremental sync paritás külön feladat. A célzott Health UI/JVM gate nem helyettesíti a canonical release gate-et.
