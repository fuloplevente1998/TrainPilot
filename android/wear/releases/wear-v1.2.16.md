# TrainPilot Wear OS 1.2.16 /2723 — Quick Workout (candidate)

A real **Gyors edzés** is available in the watch's main menu. Choose exercises from the cached phone exercise library, including personal exercises; search by name or browse paged results. After starting, log repetitions/sets/load with the existing workout controls, and choose **+ Gyakorlat** to add another exercise while active.

- Quick Workout is a real quick-mode session, not a fake program or extra Journal item. A pending offline workout keeps its identity and commands until phone sync.
- With explicit watch measurement consent, the existing Health Services exercise recorder measures pulse, total kcal and other supported metrics against the same workout ID.
- Phone and watch use the existing canonical set controls, rest timer, finish/discard, durable Data Layer outbox, workout summary and late health update pipeline.
- Current paired phone candidate: **1.2.14 /2721**. This feature requires the updated phone protocol; it is not a Wear-only release.
- The app ID and signing certificate remain unchanged. VersionName **1.2.16**, versionCode **2723**.

## Galaxy Watch7 test checklist
1. Open the updated phone app once so the watch receives the exercise catalog. Confirm `Gyors edzés` → `Fekvőtámasz`.
2. Enable watch measurements. Start from the watch; check the measurement is active and the workout can continue with the screen off.
3. Record at least one set with repetitions (e.g. 15 pushups). Add another exercise on the watch and log a set.
4. Finish; check that **one** quick workout appears in the phone Journal with the same sets and pulse/kcal totals. Verify no double calories or duplicate workout history.
5. Test temporary disconnection/reconnect and repeat finish/reopen. Test a phone draft already in progress; the watch must not replace it.
6. Also start an existing phone Quick Workout; watch measurement controls and summary must still work.
7. Check picker search, scrolling and layout at enlarged fonts; preserve all previous calendar, Tile, complication and workout routes.

This is an **unaccepted physical-watch candidate**. Keep the PR draft until paired-device checks and both phone/Wear release gates pass.

Related: [#155](https://github.com/fuloplevente1998/TrainPilot/issues/155).

## Mini Napló (paired phone 1.2.14 and Wear 1.2.16 candidate; #159)

From the Wear **Főmenü → Mini napló**, browse the latest phone-saved workouts, then drill down to exercise names, completed sets/repetitions/loads and the separate TrainPilot Wear vs Health Connect metrics. The snapshot is cached on the watch for offline reading.

- It displays only **saved canonical phone sessions**, never half-finished watch sessions or pending outbox commands.
- Eight distinct workouts maximum; a bounded payload never copies images, full daily Health data or recovery records.
- Wear total calories correspond to an actual `wear_health_services` metric for the same workout ID. Health Connect window calories remain a distinct data source, not an added workout total; missing readings remain missing and measured zero remains zero.
- Read-only in this first iteration; no deleting or editing history on the watch. The main app and the Wear recording/release gates remain independent.

**Physical verification pending:** phone/Watch7 pairing, a 15-pushup quick session and a later 45-minute training session, accurate source labels and late Wear summary updates, 8-workout list, offline reopening, 320-pixel round screen and large fonts.

See [#158](https://github.com/fuloplevente1998/TrainPilot/issues/158) and [#159](https://github.com/fuloplevente1998/TrainPilot/issues/159).
