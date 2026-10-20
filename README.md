# TrainPilot

Workout planning and logging for Android and Wear OS, with optional health data, recovery guidance, GPS routes and Google sync.

[Magyar leírás](README.hu.md) · [Release catalog](docs/releases/INDEX.md) · [Current handoff](docs/WORK_HANDOFF.md)

## Downloads and source status

| State, checked 20 October 2026 | Version | Source |
| --- | --- | --- |
| Most recently published GitHub APKs: [phone](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.6) and [Wear OS](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.6) | 1.3.6 /2733 | `98be662`, published 19 October. [Release receipt](docs/builds/2026-10-19-release-136.json). |
| Accepted main after #222: Samsung Energy Score and RIR/Coach | 1.3.6 /2733 | Application source `c14e707`. [Acceptance receipt](docs/builds/2026-10-20-222-accepted.json). |

**The published 19 October APKs do not contain the later #222 changes.** The accepted [phone test APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379) is a separate Actions artifact and may expire. Wear is unchanged. No version bump or new public release accompanied the #222 merge.

Install the phone APK on the phone and the Wear APK on the watch. Preserve the existing application data when updating. APKs, source archives, checksums and build metadata belong to their specific release; check the [catalog](docs/releases/INDEX.md) before choosing an older build. Google Play publication is still being prepared in [#214](https://github.com/fuloplevente1998/TrainPilot/issues/214).

## Features in accepted main

- Personal programs, quick workouts, an exercise library, sets, repetitions, weights, timers, training history and progress.
- Normal or HIT-inspired training styles; double, linear or RIR/Coach progression with preview, approval and undo. Actual RIR can be logged per exercise in RIR/Coach mode.
- Recovery guidance with dated data and explicit sources. A valid current-day Samsung Energy Score takes precedence; unavailable data falls back to TrainPilot's estimate. Pain and workload limits remain active.
- Health Connect or direct Samsung Health import, a persistent local health journal and separately identified TrainPilot Wear workout measurements.
- Phone/watch workout control, automatic work/rest phases, acknowledged handoff, offline watch workouts, Mini diary, Tile and complication.
- Optional GPS distance and private workout routes; indoor intervals work without GPS.
- Local JSON/ZIP backup, private workout photos, optional Google Drive `appDataFolder` sync and Google Calendar integration.
- Multiple interface languages, themes and system text-size support.

Availability depends on the device, provider, permissions and data actually shared. Successful Samsung reading on the accepted device does not establish production partner access for every installation. See the [Play preparation checklist](docs/PLAY_STORE_CHECKLIST_2026.md).

## Interface examples

| Calendar and planning | Coach with larger system text |
| --- | --- |
| ![Calendar with workout planning](docs/screenshots/calendar-planner.png) | ![Coach with larger text](docs/screenshots/coach-large-text.png) |

These are synthetic Chromium fixtures from the accepted source, not personal health records or native device-test evidence. [Screenshot provenance](docs/screenshots/README.md).

## Documentation

- [Changes by version family: 1.0, 1.1, 1.2, 1.3](RELEASE_NOTES.md)
- [Release catalog and immutable build history](docs/releases/INDEX.md)
- [Version and release policy](docs/VERSION_POLICY.md)
- [Build and signing](docs/BUILD.md), [Wear OS](docs/WEAR_OS.md)
- [UI validation](docs/UI_TESTS.md), [performance policy](docs/PERFORMANCE_REGRESSION_POLICY.md)
- [Play preparation](docs/PLAY_STORE_CHECKLIST_2026.md), [privacy draft](docs/PRIVACY_POLICY_DRAFT.md)
- [Current handoff](docs/WORK_HANDOFF.md), [maintenance scope](docs/MAINTENANCE_214.md)

## Development

```bash
npm ci
node scripts/check-version-policy.cjs
node --test scripts/check-version-policy.test.cjs scripts/release-metadata.test.cjs
npm test
npm run test:ui
```

Full Chromium, native release and signed APK validation runs in GitHub Actions. Phone and Wear have separate release gates. Application changes need source-matched evidence and user acceptance; work starts on a development branch and Draft PR. See [AGENTS.md](AGENTS.md).

Google integrations require separate OAuth configuration. Signing keys, credentials and personal backups must stay outside the repository. `npm run sync` updates Capacitor's Android project and should be reviewed before committing generated files.

## License

TrainPilot is proprietary. The package is marked `private` and `UNLICENSED`; public source visibility does not grant an open-source license. Distribution/commercial terms remain a separate decision in #214. Third-party components retain their own licenses, including the [Lucide license](www/LUCIDE-LICENSE.txt).
