# TrainPilot Wear OS – architecture and implementation plan

Status: initial implementation branch (`feat/wear-os`)

## Goal

Add a native Wear OS companion experience to TrainPilot without rewriting the existing Android application.

The current phone application remains the source product:

- Capacitor 7 / Android WebView UI;
- the existing Java Capacitor plugins and Android services remain in place;
- the existing JavaScript workout model, backup, calendar, Health Connect and phone UX remain unchanged unless a small Wear bridge needs an explicit integration point.

Wear OS is implemented as a **separate native Android application module** inside the same Gradle project.

## Why the phone app does not need a Kotlin rewrite

TrainPilot already has a useful separation between the web workout UI and Android-native capabilities. Rewriting the entire application in Kotlin would be a large migration with little benefit for the first Wear release and would create unnecessary regression risk.

Instead:

```text
Existing phone app
Capacitor / WebView / JavaScript
        |
        | thin native bridge
        v
ActiveWorkoutStore + WearSync
        |
        | Wear OS Data Layer
        v
Native Wear OS app
Kotlin + Compose for Wear OS Material 3
```

Only the Wear client is Kotlin/Compose.

Longer term, individual domain models can be extracted into a shared native module if standalone-watch requirements justify it. That is an optimization, not a prerequisite.

## Module layout

```text
android/
├── app/                     existing phone application
└── wear/                    native Wear OS companion
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/repforge/app/wear/
        │   └── MainActivity.kt
        └── res/
```

Later phone-side additions are expected to be narrow and additive:

```text
android/app/src/main/java/com/repforge/app/
├── WearSyncPlugin.java
├── ActiveWorkoutStore.java
└── WearDataListenerService.java
```

and a small web integration layer such as:

```text
www/wear-sync.js
```

## Data ownership and sync

The first Wear release is a **companion app**, not a completely standalone replacement for the phone app.

The current active workout still originates from the TrainPilot phone experience. To avoid depending on a live WebView while the phone is locked, the active workout should be mirrored into a native persisted snapshot.

### Proposed flow

1. JavaScript starts or changes an active workout.
2. Existing `persistDraft()` behavior continues unchanged.
3. The Wear bridge writes a normalized active-workout snapshot to `ActiveWorkoutStore`.
4. The phone sends the snapshot through the Wear OS Data Layer.
5. Wear renders the workout from its local copy.
6. A watch action (for example "set complete") creates a small command/event.
7. The phone native layer stores that event immediately.
8. When the WebView is active, the event is applied to `state.session` and the normal TrainPilot draft is persisted.
9. On resume/reload the JS side reconciles pending Wear events before rendering the workout.

This prevents the Wear app from requiring the WebView process to stay alive.

## Initial sync contract

The watch does not need the complete TrainPilot backup or database. The first payload should contain only what is needed during the current workout.

Example:

```json
{
  "schema": 1,
  "workoutId": "A",
  "programId": "home-basic",
  "programName": "Otthoni A/B - Alap",
  "started": "2026-10-07T17:20:00Z",
  "currentExercise": 0,
  "restSeconds": 90,
  "exercises": [
    {
      "id": "goblet-squat",
      "name": "Goblet squat",
      "sets": [
        {
          "set": 1,
          "reps": "12",
          "weight": 10,
          "done": false
        }
      ]
    }
  ]
}
```

The exact schema will be versioned before real phone/watch synchronization is enabled.

## MVP user experience

The first useful Wear build should focus on the active workout:

1. show the active workout;
2. show the current exercise;
3. show current set / total sets;
4. show repetitions and load;
5. mark a set complete;
6. adjust repetitions;
7. adjust load;
8. start/show the rest timer;
9. haptic feedback when rest ends;
10. previous / next exercise;
11. finish workout;
12. optional live heart rate from Wear OS Health Services.

The watch UI should remain intentionally smaller than the phone UI. Program editing, detailed journal/history, Coach, backups, photos, Google account flows and complex planning stay on the phone for the initial release.

## Wear Health Services

For live metrics during an active workout, the Wear application should use Wear OS Health Services where supported. This can provide watch-local exercise data such as heart rate without routing every live reading through the phone.

TrainPilot's existing phone Health Connect flow remains responsible for its current phone-side health/journal behavior. Wear Health Services is an additional real-time exercise source, not a replacement for the existing implementation.

## Tiles and complications

These are Phase 2 features after reliable workout synchronization:

- **Tile:** active exercise, set progress, rest timer and a button to open TrainPilot Wear.
- **Complication:** compact active-workout/rest status.
- Later, an optional quick-workout launcher can be exposed from a Tile.

## Offline behavior

The watch should cache the latest active-workout snapshot locally. If the phone temporarily disconnects:

- current workout information remains visible;
- watch interactions are queued locally;
- queued events are delivered when the phone becomes reachable again;
- conflict handling uses monotonically increasing revision/event identifiers rather than blindly replacing the entire workout.

## Standalone future

The initial manifest declares the watch app as non-standalone because the phone application owns program selection and the canonical workout workflow.

A future standalone mode can be considered after:

- the workout domain model has a stable versioned native/shared representation;
- the watch can safely create and complete workouts without phone-side JS;
- reconciliation and conflict behavior are proven;
- account/backup implications are designed.

Standalone support must not force an early rewrite of the phone UI.

## Technical choices

Initial Wear module:

- Kotlin;
- Jetpack Compose;
- Compose for Wear OS Material 3;
- min SDK 30 for Wear OS 3+ target;
- existing TrainPilot `compileSdk` / `targetSdk` values are reused;
- same application ID (`com.repforge.app`) for phone/Wear packaging compatibility;
- separate namespace (`com.repforge.app.wear`) for source organization.

The first module deliberately contains no Data Layer or Health Services logic yet. The goal of the first commit is to prove that the repository can contain a clean native Wear target without destabilizing the phone application.

## Implementation phases

### Phase 0 — project skeleton

- [x] create `feat/wear-os`;
- [x] document the architecture;
- [x] add `:wear` Gradle module;
- [x] add a minimal native Compose Wear application;
- [ ] verify CI/local Wear debug build.

### Phase 1 — phone/watch workout sync

- [ ] define versioned `ActiveWorkoutSnapshot` schema;
- [ ] implement phone `ActiveWorkoutStore`;
- [ ] implement phone Data Layer service/plugin;
- [ ] implement Wear Data Layer repository;
- [ ] mirror start/resume/update/finish lifecycle;
- [ ] add revision/event conflict protection;
- [ ] regression tests around existing `persistDraft()` semantics.

### Phase 2 — usable workout controller

- [ ] current exercise screen;
- [ ] set completion;
- [ ] reps/load editing;
- [ ] previous/next exercise;
- [ ] rest timer + vibration;
- [ ] workout completion;
- [ ] reconnection/offline queue.

### Phase 3 — sensors and glanceable surfaces

- [ ] Wear Health Services live heart rate;
- [ ] optional workout exercise session integration;
- [ ] Tile;
- [ ] complication.

### Phase 4 — standalone evaluation

- [ ] determine which domain logic should be shared;
- [ ] local watch workout persistence;
- [ ] phone/watch merge rules;
- [ ] decide whether standalone mode should be enabled.

## Regression policy

The Wear work must not silently change existing phone behavior.

Until a phone-side Wear bridge is introduced:

- `android/app` behavior is expected to be identical;
- existing Node/Chromium regression suites remain authoritative for the phone UI;
- Wear changes are isolated to `android/wear` plus the minimal root Gradle configuration needed to include it.

When synchronization work starts, tests should prove that the same set/reps/load changes result in the same persisted TrainPilot draft regardless of whether the action originated on phone or watch.
