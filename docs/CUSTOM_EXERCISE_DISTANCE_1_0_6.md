# TrainPilot 1.0.6 / 2690 – own exercises and distance

Candidate based on published v1.0.5/main `f4a556ab158c0770dfebb42a0580b0bf3089cc2a`. This document describes implementation and acceptance checks; it does not assert physical-phone approval.

## User flow

Programs → Create own exercise opens the existing shared panel. The underlying route and scroll remain mounted; red X and Android Back close it, cancel without saving, and restore focus. Equipment checkboxes use the same theme-aware selected accent and checkmark as the other current forms. HU/EN/DE/RO text and 320–412px layouts are covered by UI regression.

Choose repetitions, time, or time and distance. Running, jogging, skating and similar activities can use the distance type regardless of their name. During the workout enter kilometres and seconds manually, or explicitly start GPS and then stop/record it. The Journal's existing inline exercise editor supports changing distance and time later.

## Data and GPS

- `measurementType`: `reps`, `time`, `distance`. Old exercises without it remain compatible.
- Distance exercises use bodyweight, `repUnit: mp`; set `reps` stores duration in seconds for existing timed-exercise compatibility. Kilometres never enter repetition totals or lifted volume.
- Set `distanceMeters` is a finite nonnegative number (maximum 2,000,000m); `distanceSource` is `manual` or `gps`. JSON/ZIP/Drive preserve these existing exercise/set object extensions. Comma decimal input is supported; invalid values do not overwrite saved values.
- Native Android `DistanceTracker` starts a location foreground service only after a user action and precise location permission. No Google Maps API, key or route backend is used. No background-location permission or startup receiver is requested.
- The service keeps only aggregate time/distance, a tracking key and signal quality in app preferences. Coordinates are transient filter inputs and never enter the journal, preferences, logs or export. The normal all-local-data erase also stops GPS and clears its aggregate preferences.
- The app or notification stops measurement. Locking the screen uses the foreground-service path; removing the task or process interruption stops measurement and leaves the last persisted totals recoverable. It does not silently restart GPS. With notification permission denied, Android may hide the drawer notification; stop remains available in the app/system active-app control.
- A session/exercise/set key prevents attaching totals to a different workout. Polling applies an absolute aggregate plus the pre-start totals, avoiding double counting. Notification stop and WebView reload recover totals into the same draft. GPS failure releases the UI and leaves manual totals intact.
- Reject invalid fixes, accuracy worse than 35m, stale fixes, implausible jumps above 20m/s and stationary jitter. A gap over 30s reanchors without counting a guessed straight line. This is an estimate: missing signal can undercount; corners and device accuracy affect the result. Manual correction is supported.

## Built-in activities and own program panels

The existing 100 exercise entries remain intact. Six distance activities (running, jogging, inline skating, cycling, walking, hiking) are available directly in Quick Workout and in custom-program exercise selection. All six have Hungarian, English, German and Romanian names, equipment and notes. Demonstration links are explicitly unverified searches. Saved old libraries gain the new entries through the normal merge, without rewriting user data.

Distance activity IDs retain measurement semantics through legacy session builders. Their Coach guidance describes time and distance without automatic weight or repetition progression. They are excluded from generated strength programs. Own distance exercises use conditioning defaults when selected.

Both custom builders have a full rounded frame and a separate sticky header containing the red X. New program name/day-count entry and the day editor use the shared overlay; cancelling creation writes nothing. Existing day-name, selection, order and removal operations continue to save immediately. Closing preserves the underlying Programs page.

## Validation and remaining phone checks

Node regression covers decimal parsing, invalid exports, concurrent starts, permission denial, stop/resume, draft recovery, manual correction and aggregate export. Chromium covers real panel lifecycle, current controls, narrow layouts, workout recording and Journal editing. JVM tests cover the native GPS distance filter. Release Gate builds/signs the APK and verifies source bytes and signer continuity. Performance CI compares against the same published v1.0.5 baseline.

Before phone approval, install 2690 over 2687 without uninstalling and check:

1. Programs panels: full frame, header-contained red X even after scrolling, themed controls, Android Back, no page jump. Create/edit a program and an own distance exercise. Find all six built-in activities directly in Quick Workout.
2. Enter e.g. `5,25`km / `1800`s, record it, edit in Journal, and round-trip a ZIP containing photos. Confirm existing workouts/Google links remain intact.
3. Deny precise location or disable GPS: manual recording still works. Grant permission only when starting GPS; verify notification allow/deny paths and rapid repeated taps.
4. Walk/run a known outdoor segment, lock the screen for several minutes, reopen, stop in the app and via the notification, then complete the workout. Compare approximate distance and elapsed time.
5. Interrupt the app/WebView and resume the same draft. Confirm the last saved totals are recoverable, no unwanted GPS restart, and no totals copied into a new workout. Test a signal gap and manual correction.
6. Repeat existing automatic Drive/Calendar plus offline ZIP backup checks. Verify all-local-data erase stops GPS and removes its aggregate state.

## Play publication follow-up

The new optional location use and `FOREGROUND_SERVICE_LOCATION` declaration must be reflected in the Play Console's foreground-service use case, privacy policy and Data safety answers before a Play release. Existing OAuth/signing configuration remains separate. Official platform requirements: https://developer.android.com/develop/background-work/services/fgs/service-types#location . Device approval and Console configuration are not replaced by a successful GitHub build.

## Indoor walking and workout steps (2690)

Health Connect `READ_STEPS` now supplies steps for the workout's exact active windows, excluding pauses and merging overlapping intervals. A lightweight aggregate-only endpoint refreshes the distance-workout card at most once per minute automatically; a manual refresh is also available. It uses Health Connect's source deduplication, replaces absolute totals, distinguishes zero from unavailable data, preserves the previous reading on failure, and ignores responses for a replaced session. No imported steps are written back and no step-to-kilometre estimate is made.

The completed workout's Health panel shows and persists steps; the existing Health-data export checkbox protects them inside `health240`. Phone/watch data can be delayed, so refresh the Journal panel after the source sync finishes. Existing installations may not already have the newly used `READ_STEPS` permission, so the workout card detects that state and offers a dedicated step-permission request without asking for unrelated Health permissions. Indoors, duration-only recording is allowed; unknown distance is shown as `— km`. GPS weak-signal text describes indoor limitations. The earlier “23 seconds” observation referred only to repeated step-data tests and is not treated as a GPS elapsed-time defect.

Additional phone checks: enable/deny Health Connect step reading, walk indoors, refresh after phone/watch sync, complete a duration-only walking workout, reopen its Journal Health panel, and verify a Health-inclusive/exclusive ZIP. Test an interrupted/resumed workout to exclude pause steps. GPS distance and elapsed-time behavior still need an outdoor phone test.

Platform references: https://developer.android.com/health-and-fitness/health-connect/features/steps and https://developer.android.com/health-and-fitness/health-connect/aggregate-data .
