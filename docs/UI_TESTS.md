# Chromium regression execution

The release gate runs the complete browser suite in six independent GitHub Actions jobs. Each job fetches the full Git history required by the historical parity regression, and has its own Chromium, filesystem and test data; tests within a job still run sequentially. The APK job depends on **all six** passing. A failed test fails its shard, while the remaining tests continue to provide useful evidence. There are no automatic retries or reduced viewport/language/theme matrices.

The accepted main baseline ran 56 browser scripts sequentially in **10 min 9 sec** ([run 37121932726](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37121932726)). The initial six-shard plan estimates roughly two minutes per shard; measure actual Actions times before claiming the target is met. Runner setup, queue time and APK compilation are separate from browser-test duration.

## Current verification baseline — 2026-10-13

The #179 integration contains 90 browser test scripts in the canonical manifest. All six shards passed on the latest tested phone source `1650906` ([run 38352741468](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352741468)); phone source is unchanged in the paired Wear commit `e3db5e2`. Quick regression reports 137 application checks plus the separate runner checks.

Wear uses actual Compose instrumentation on a round API 30 emulator: 51 tests passed at 160/192/225 dp and normal/1.5×/2× font sizes, including the real automatic foreground service with the Activity stopped ([run 38352812492](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38352812492)). This does not replace physical vibration, locked-device, pairing or OEM battery-management acceptance.

## Commands

```sh
npm test                                 # runner safety checks + existing Node regressions
npm run test:ui                           # every browser test, sequentially
npm run test:ui -- --shard 2/6             # the second CI shard
npm run test:ui -- --shard 1/6 --list      # inspect the complete six-shard plan
```

`tests/browser/suite.json` is the canonical list: the original full-suite entries and order are preserved. Add each new full-suite test there, with a positive `estimatedSeconds` weight. The planner assigns the longest tests first to the lightest shard, then restores original order within each shard. Weights affect scheduling only; they never filter tests or relax assertions. The runner rejects duplicate entries, missing files and invalid shard selections. Its safety tests prove exact one-time coverage across shards and actual CLI failure propagation.

Every shard uploads screenshots and a `ui-results/shard-N-of-6.json` report containing the source commit, assigned tests, exit codes and measured durations. Use these reports to update estimates if future additions make a shard slower. Artifact names include the shard number to prevent collisions. `ui-results/` is generated evidence and is excluded from Git.

The separate Phase 7 performance workflow remains unchanged and uses an isolated runner. The benchmark in the full suite also runs alone within its shard; no browser tests compete for CPU in the same job. Application code, versions, signing, package/source checks and stable release assets are unaffected by this CI-only change.
