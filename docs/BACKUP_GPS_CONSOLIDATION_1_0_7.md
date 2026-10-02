# TrainPilot 1.0.7 / 2691 candidate

Date: 2026-10-03, Europe/Budapest. Branch: `fix/backup-gps-consolidation`.
Baseline: validated main `ef09c5ee44b61902a7bb265f8ef2b3d2ea8de88e`, public 1.0.6 / 2690. The downloaded public APK matches this main's `www/app.js`; its SHA-256 matches the published checksums. This candidate requires physical-phone acceptance before main merge.

## Changes

1. **Backup import:** v3 accepted untrusted IDs that were later interpolated into inline JavaScript actions. The restore now rejects unsafe identifiers, prototype-related IDs and malformed nested rows before migration, confirmation, database changes or native photo installation. Validation covers exercise/program/day references, history/set/photo identifiers, schedules, favorites and plan references. Existing safe Unicode identifiers and legacy ID-less workout/weight records remain supported. Inline UI handlers outside this import boundary are a later cleanup task.
2. **GPS ownership:** tracking previously used the set's array index. Deleting the owner moved that index onto another set. New trips persist a stable `setId`; existing 1.0.6 trips receive one when resolved. Deleting the tracked set first stops the native tracker, and a failed stop preserves the set. Deleting an earlier set retains ownership and totals. Permission/start operations block deletion; confirmation/native-stop continuations recheck the session and target object.
3. **Backup builder:** 68 historical `makeBackup` definitions become one. It preserves programs, planner, schedules, weights, workouts, distance measurements, photo references, favorites, language, theme and recovery data. Health consent is read at export time; projection clones the payload before redaction, and Drive continues to omit the daily Health ledger/wellness snapshot. Historical compatibility section boundaries remain.
4. **Evidence:** the performance workflow now compares against validated 1.0.6 main using the same current benchmark on one runner. Metrics record version, source commit and app checksum instead of a fixed historical commit label.

The public app ID and signing configuration are unchanged. Version code 2691 can upgrade 2690. There is no new user-facing feature in this first cleanup step.

## Verification

- `npm test`: 116 passed. Existing Drive recovery/merge/tombstone, Journal, Coach, Health, native export and distance regressions remain in the suite.
- `node tests/backup-consolidation.cjs`: complete dataset preservation; distance/set IDs/photos; read-only Health projection; opt-in export; Drive ledger omission; invalid import leaves storage unchanged; two distinct same-time ID-less v1 workouts and weights survive restore.
- Exact before/after snapshots of `makeBackup()` and `syncData()` match every field except app version/export timestamp with Health consent both off and on, using the validated main runtime and this runtime with the same synthetic data.
- `node tests/browser/backup-gps-consolidation.cjs`: actual import and rendered delete controls; unsafe IDs/null rows write nothing; owner removal stops GPS; earlier removal preserves totals; legacy trip migration; failed native stop; pending permission and stale-confirmation races. Four widths (320/360/393/412 px), two themes and navigation re-entry.
- `npm run test:ui`: full Chromium suite, including the new behavioral regression. Workflow logs are the final candidate evidence; browser native bridges are mocked and do not replace physical-phone checks.
- `npm audit`: zero vulnerabilities across runtime and development dependencies. The previously discussed brace-expansion advisory is absent from this main's current dependency lock.
- Performance: `node tests/browser/phase7-performance-19.cjs` before/after with 240 workouts × 8 exercises × 4 sets; run `node tests/browser/phase7-performance-budget.cjs <log>`. CI archives `phase7-before.log` and `phase7-metrics.log`; budgets are unchanged. Local timing differences are runner measurements, not phone speed claims.
- Release Gate: full Node/Chromium, Android release unit tests, signed APK build, package/version checks, packaged source comparison, source commit stamp, checksums and certificate continuity against public v1.0.0. Branch builds upload artifacts and do not publish a release.

## Phone acceptance checklist

1. Install the signed 1.0.7 / 2691 APK over the existing app. Verify historical workouts, weights, programs, settings and photo references remain available.
2. Export and restore a test backup with Health inclusion disabled, then enabled. Verify distance/time, programs, favorites, language/theme and photo references. Use a disposable dataset for restore testing.
3. Start GPS on a distance workout with at least two sets. Remove the measured set: native tracking should stop, and the remaining set should have no inherited kilometres/time.
4. Start GPS on the second set after completing the first, then remove the first. Tracking should continue on the original second set with its totals intact.
5. Check GPS permission denial, background/resume, stop/start, draft resume and normal workout finish. Check Quick Workout, Journal editing and manual distance entry.
6. Check navigation/Coach/Journal responsiveness with the existing dataset. Confirm the accepted UI still behaves as expected.

Do not merge or publish this candidate until the user explicitly accepts the signed APK. No device test is claimed by automated validation.
