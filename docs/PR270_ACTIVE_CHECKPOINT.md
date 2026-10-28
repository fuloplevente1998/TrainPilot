# PR270 active checkpoint

Updated 2026-10-28. PR remains DRAFT on `feat/studio-ui-topnav-trial-20261027`.
Root works alone; separate agents remain stopped. No merge/release authorized.

## Current repair

The next canonical run must validate both phone and Wear; no runtime success yet.
Application change: ExerciseLibraryScreen uses one scroll owner for its header and
results. Video/favorite actions sit below the title row, preserving readable and
clickable exercise titles on the compact profile. Existing catalog/video/write
callbacks are retained. Library UI tests scroll real controls into view; no test
identity or data/video assertion is removed.

GPS test wait accepts a temporarily absent Compose root after returning from
Android App Info, within the original 15-second deadline. The actual recovery
dialog and no-start assertions remain required.

Phone CI now creates a fresh AVD and cold boots without snapshots. This avoids
the observed ColorBuffer restore path; cache corruption and the cause of the
intermittently empty approval dialog are NOT proven. The unchanged approval
assertions still must pass. Runner prints a heartbeat every30seconds and reports
each instrumentation invocation, while retaining raw evidence and screenshot checks.

Wear's scoped process cleanup worked in run40743423923 (screenshot evidence:
matched PID2993, adbExitCode0, remainingPids[], result stopped, testExitCode0).
The upstream action then blocked in its second, unbounded adb emu kill. A CI-only
SDK adb guard is installed AFTER SDK setup via the action's pre-launch hook.
It keeps the original executable as adb.trainpilot-real on this disposable runner,
forwards all regular/physical-device commands unchanged, and bounds only the exact
`-s emulator-N emu kill` call to10seconds plus2seconds kill grace. Nonzero timeout
status is preserved; test errors are never converted to success. Both Wear action
invocations and the phone action use it; always-upload and job/step limits remain.
Five guard host tests include a genuinely hung subprocess and nonzero timeout,
normal argv/exit preservation, exact matching, CI refusal and idempotent setup.
Together14runner +34parser +7scoped-cleanup +5guard tests PASS. YAML, py_compile,
shell syntax and diff checks PASS. Kotlin/Android runtime await canonical Actions.

## Last complete phone evidence

[40743423923](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40743423923),
PR source7d2f4ab65077d7017fb94b8682317764fa665912,
CI merge37fbe0b9cb5cad3492dcf4f59d6ef2b1167b306e.
Phone artifact12841348491:119main=117PASS/2FAIL,4isolatedGPS=3PASS/1FAIL;
combined120/123,0error/skip. Failures: library title not displayed;
approval ABA test waits for an empty comparison dialog; GPS App Info return
queries an absent Compose root immediately. Default320x640/160dpi/font1.0.
Quick40743424067 SUCCESS; debug/signed build and both upgrade jobs SUCCESS.
Wear raw tests passed in the screenshot but jobs timed out; full gate is NOT green.

Native version1.3.71-native-test/2736, applicationId and signing key unchanged.
Fake Health/Wear/GPS data forbidden; original gold logo/anatomy and native
ownership, finalization, data-replacement/migration/backup semantics retained.
Three P2 design items and owner-profile visual/device acceptance remain open:
[UI audit](reviews/PR270_UI_AUDIT_350_2026-10-28.md).

Next: inspect the new run's exact merge/source tree, all7job outcomes, raw
123phone/183Wear/JVM results and signed APK metadata. Do not deliver the pair as
fully validated until these checks pass. Any residual phone failure needs raw
logs before further changes; no assertion weakening or blind rerun.
