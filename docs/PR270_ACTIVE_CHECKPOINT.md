# PR270 active checkpoint

Updated 2026-10-29. PR270 remains DRAFT, base `feat/native-android-migration`.
Root works alone. No merge/release/version/signing/data changes.

## Current Wear startup repair

Verified incoming HEAD `151ced0ac367b5b1d5cccc6294d42317a71fa280`.
Canonical run [40761838896](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40761838896)
used merge `a59be2bcfe063673cf9c578ffe15b85edd4b9dba`: build/unit/signing,
phone, both signed upgrades and Wear1/2 SUCCESS; Wear0 FAILED before instrumentation.
Quick40761838890 SUCCESS. These are GitHub job outcomes; no new raw XML count claimed.

Wear0 log: `default_boot` snapshot restored at20:31:13 UTC, then empty
`sys.boot_completed` through20:41:28, boot timeout. No Wear0 result directory existed,
so upload also failed. Snapshot corruption is NOT proven; this repair removes
snapshot restoration as a dependency instead of weakening test assertions.

Wear now creates a fresh AVD with `-no-snapshot`, like the established phone lane.
The old shared Wear AVD/adb cache and snapshot-creation invocation are removed.
All3 shards, fail-fast:false, instrumentation/strict nonempty success check,
scoped cleanup, SDK adb shutdown guard and always-upload remain. Step budget20min
allows cold boot plus tests within the existing25min job. Prelaunch startup.json
and bounded failure diagnostics preserve evidence even when the test script never runs.

Local verification:63 Python host tests PASS; shell syntax, workflow YAML,
cold-boot/matrix/strict-upload invariants and git diff --check PASS.
Android runtime still awaits the new canonical full run; do not call the fix proven yet.

## UI status

The older350d7f54 audit is stale for its three P2 findings: ebe9e739 scopes the pulse
trend to query.channel;32e95607 displays recovery source/qualifications;
151ced0a opens the existing weight editor with an explicit add intent.
Do not duplicate these native features. Full Studio visual parity is not established.
Use the18 referenced Drive images and new same-profile native screenshots;
the previous run used compact320x640, so it cannot prove owner-profile parity.

## Historical repair context


Updated 2026-10-28. PR remains DRAFT on `feat/studio-ui-topnav-trial-20261027`.
Root works alone; separate agents remain stopped. No merge/release authorized.

## Current repair

Initial source a560ee34 /run40748357263 was superseded by the focused crashpad
cleanup follow-up after the full completed Wear log became available. Inspect
the latest branch-source run, not the superseded candidate.

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
The full log shows the second adb call returned (endgroup within40ms), but the
action remained open until its15minute deadline. Runner cleanup then found an
orphan SDK crashpad_handler (PID2997), alongside adb. Scoped cleanup now also
captures this known SDK helper while it is an emulator descendant, before adb
shutdown can reparent it, and terminates it with the existing bounded escalation.
An unrelated crashpad/other AVD remains excluded. A CI-only
SDK adb guard is installed AFTER SDK setup via the action's pre-launch hook.
It keeps the original executable as adb.trainpilot-real on this disposable runner,
forwards all regular/physical-device commands unchanged, and bounds only the exact
`-s emulator-N emu kill` call to10seconds plus2seconds kill grace. Nonzero timeout
status is preserved; test errors are never converted to success. The adb guard is defense in depth, not the demonstrated residual-hang cause.
Both Wear action
invocations and the phone action use it; always-upload and job/step limits remain.
Five guard host tests include a genuinely hung subprocess and nonzero timeout,
normal argv/exit preservation, exact matching, CI refusal and idempotent setup.
Together14runner +34parser +8scoped-cleanup +5guard tests PASS (61total). YAML, py_compile,
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
