# PR270 compact emulator launcher isolation

Candidate: `d1839cfa1f0fb277efb5a57318fac4039e44933c`, PR #270 remains draft.
Application and Android-test source are unchanged from `350d7f54`.

## Verified failure and source

[Run40724759390](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40724759390)
on PR head `350d7f54d6f77449c1aabec91691de83128ede7d` failed its phone job.
Actual checkout `54ede727e917d2b22194b164beddc0e097c22101` has the identical
tree `4740bd963d0eda3d4d4b77e1d5337df15c1262c8`.

Raw phone artifact12832569853 has119 declared main cases,44 terminal
(43 pass/1 fail),75 missing, zero screenshots and no isolated GPS execution.
The sole completed failure was `RootViewWithoutFocusException`; the fatal
host focus window was `Application Not Responding: com.google.android.apps.nexuslauncher`.
This is not a successful phone test run, and missing cases are not passes.
Re-execution is required before attributing other possible app defects.

The manifest records320×640/160dpi/fontScale1.0 and no launcher isolation.
The existing runner disabled the unrelated NexusLauncher only when an explicit
display size was supplied, so automatic compact PR runs lacked that isolation.

Build/signature validation and both signed upgrades succeeded. JVM raw XML:
168 files,1345 passing, zero failure/error/skip/duplicate. Wear raw instrumentation:
183 passing, zero failure/error/skip/missing/crash, **but all three jobs were CANCELLED**.
Raw success does not turn those job states or this full run into SUCCESS.

## Targeted repair

- Apply the same existing NexusLauncher isolation to default compact disposable emulators.
- Require the emulator qemu guard before setup mutation; physical devices are refused.
- Keep display defaults, the real app's focus/ANR checks and instrumentation failure gates.
- Include phone-runner/parser sources and their host tests in the existing native workflow triggers.
- Run the two fast host suites before Gradle/APK work in the canonical workflow.

No Android UI, provider, database, migration, workout engine, version, applicationId,
key or signing-policy change. No local Android build/emulator/APK execution.
Local verification:14 runner tests +34 evidence-parser tests pass;
workflow YAML/trigger/fail-fast ordering and `git diff --check` pass.

## Current validation

[Quick40736984836](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40736984836)
SUCCESS (native source routing, legacy npm not applicable).
[Canonical40736984933](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40736984933)
is IN PROGRESS at this checkpoint. Inspect its exact checkout, raw results and
display manifest before declaring runtime success or owner-profile screenshots.

[Machine-readable baseline and APK hashes/links](../builds/2026-10-28-pr270-launcher-repair.json).
The previous signed pair is diagnostic, not a fully accepted final pair.

## Current design scope

[Current-source audit](PR270_UI_AUDIT_350_2026-10-28.md) supersedes the old list
of structural gaps: most now exist in source. Remaining P2 items are recovery
source/qualification, direct weight-entry intent and pulse-curve source coherence.
Complete visual parity and physical-device acceptance remain pending.
