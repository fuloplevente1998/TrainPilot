# TrainPilot Wear OS – architecture and implementation plan

Status: 1.2.9 integration on `fix/wear-sync-replay-navigation-2713` (PR #144).

## 1.2.9 release scope — accepted 2026-10-08

The phone's /2715 backup, sync and navigation fixes were accepted on-device. The user requested the **full planned Wear package** before promoting the combined phone/watch release to main as **1.2.9 /2716**.

- Responsive Home with permanent Start/Resume, Calendar and Menu controls, using the same round-screen action layout as the workout.
- General watch menu: Home, 14-day mini calendar, program days, workout, watch measurements and Open on phone.
- Phone-prepared calendar cache; planned/completed/skipped/rest states; starting available planned workouts through the existing canonical phone workflow.
- Opt-in Health Services workout recording in a foreground service; runtime capability and permission checks; supported HR, measured average/max HR, total kcal, steps, active duration, distance/speed and optional movement-workout GPS. Missing data remains absent. No ECG, blood pressure or invented HRV.
- Durable, revisioned health summary attached to the same workout ID; replay never creates a history row or adds calories twice. Late final metrics update the existing workout. Total kcal explicitly includes basal metabolism and remains separate from Samsung Health/Health Connect.
- TrainPilot Tile and short/long text watch-face complication with state-aware tap actions.
- Explicit Open on phone action via a narrow TrainPilot deep link, with connection failure feedback.

Native/UI/phone regressions and signed release gates must pass before the main promotion. Actual Watch7 clipping, health sensor availability, granted/denied permissions, background recording and Tile/complication behavior still need paired-device verification; CI is not a physical sensor test.

Standalone program editing/account/backup is a later evaluation, outside this accepted release scope.


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

and a small Wear bridge section inside the canonical `www/app.js` runtime. TrainPilot deliberately keeps one external application script, so Wear sync follows that existing single-source rule instead of adding a second runtime JS file.

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

The branch now contains the first Data Layer implementation: the phone publishes a versioned active-workout snapshot through a Capacitor `WearSync` plugin, while the Wear module persists and renders the latest snapshot. The initial phase deferred Health Services and watch → phone commands; both are now implemented in the 1.2.9 integration described above.

## Implementation phases

### Phase 0 — project skeleton

- [x] create `feat/wear-os`;
- [x] document the architecture;
- [x] add `:wear` Gradle module;
- [x] add a minimal native Compose Wear application;
- [x] verify CI Wear debug/release builds.

### Phase 1 — phone/watch workout sync

- [x] define versioned active-workout snapshot schema (schema 1);
- [x] implement phone `ActiveWorkoutStore`;
- [x] implement phone Data Layer plugin;
- [x] implement Wear Data Layer receiver + local snapshot cache;
- [x] complete phone/watch lifecycle mirroring;
- [x] add revision/event conflict protection;
- [x] regression tests around existing `persistDraft()` semantics.

### Phase 2 — usable workout controller

- [x] current exercise screen;
- [x] set completion;
- [x] reps/load editing;
- [x] previous/next exercise;
- [x] rest timer + vibration;
- [x] workout completion;
- [x] reconnection/offline queue.

### Phase 3 — sensors and glanceable surfaces

- [x] Wear Health Services live heart rate;
- [x] optional workout exercise session integration;
- [x] Tile;
- [x] complication.

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

## Current branch implementation

As of the first Phase 1 integration on `feat/wear-os`:

- `:wear` is a real Wear OS application module;
- the phone remains Capacitor/WebView + Java and has not been migrated to Kotlin;
- `WearSyncPlugin` publishes `/trainpilot/active-workout` DataItems;
- `ActiveWorkoutStore` persists the latest phone-side snapshot and revision;
- the canonical `www/app.js` creates a normalized snapshot while an active workout exists;
- the Wear app receives and caches that snapshot and shows the active program, current exercise and current unfinished set;
- no active workout produces a dedicated idle screen on the watch;
- the fast TrainPilot regression suite passes;
- the dedicated Wear integration workflow successfully builds both `:app:assembleDebug` and `:wear:assembleDebug` and uploads both APKs.

The next implementation milestone is watch → phone commands (set complete, repetitions/load changes, exercise navigation), followed by rest timer/haptics.

## Interactive Wear controls

Implemented on `feat/wear-os` after the initial display-only prototype:

- Watch → phone commands use durable Data Layer DataItems under `/trainpilot/workout-command/<commandId>`.
- Phone native service persists commands before the WebView handles them; processed command IDs are deduplicated.
- The watch can adjust weight, reps and timed values, mark the current set complete, move to the previous/next exercise and skip rest.
- Completing a set starts the rest countdown locally on the watch and vibrates when the countdown ends.
- The phone applies commands to the canonical `state.session`, calls the existing workout handlers where appropriate, persists the draft and republishes the authoritative snapshot.
- "Open on phone" via Wear remote activity/deep link remains a later milestone.

## Next milestone — Galaxy Watch7 health recording

The following original milestone is implemented in 1.2.9: **watch-local health recording on Galaxy Watch7 via Wear OS Health Services**. Sensor and permission behavior still require physical paired-device verification.

Scope:

- request the required health/activity/location permissions directly on the watch;
- query Health Services capabilities at runtime instead of assuming every metric is supported;
- start an `ExerciseClient`-based exercise session while a TrainPilot workout is active;
- record supported metrics directly on the watch, initially prioritizing:
  - live heart rate;
  - average/max heart rate for the workout;
  - steps;
  - active duration;
  - calories where supported;
  - distance/speed/pace/GPS for relevant movement-based workouts;
- keep collection working independently of the phone screen state;
- persist a local watch-side summary so temporary phone disconnects do not lose the workout metrics;
- sync the captured health summary back to the phone through the TrainPilot Data Layer contract;
- merge the health summary into the same TrainPilot workout/journal entry rather than creating a separate workout record;
- keep Samsung-specific metrics such as ECG/blood-pressure/vendor-only sensors out of the first Health Services milestone unless a separate supported Samsung integration is later added.

The watch UI should expose the most useful live values during training, for example:

```text
❤️ 118 bpm · 🔥 34 kcal · ⏱ 12:43
```

The milestone, Tiles/complications and the "Open on phone" action are integrated together in 1.2.9.


## Product direction — full watch experience

The Wear app should evolve from a phone companion/control surface into a focused TrainPilot watch experience. It must stay intentionally smaller than the phone UI and reuse the same program/calendar/workout model instead of creating a parallel product.

Planned watch navigation:

1. **Home** — TrainPilot branding, current date, active workout resume card when present, otherwise the next planned/next cycle workout with a large start action.
2. **Workout** — current exercise, set/reps/load/time editing, set completion, rest timer, previous/next exercise and an explicit Finish action.
3. **Mini calendar** — compact 7–14 day workout view with today, planned and completed states; detailed planning/editing remains on the phone.
4. **Summary** — workout duration, completed exercises/sets and later Health Services metrics.
5. **Programs** — active program days only, with a short preview and Start; full program editing stays on the phone.

### Wear visual system

Wear should use the existing TrainPilot brand rather than GitHub UI colors.

Canonical TrainPilot colors already used by the phone app:

- background: `#0e1015`
- card/surface: `#1a1e25`
- secondary surface: `#232933`
- text: `#f6f8fb`
- muted text: `#9aa5b6`
- primary accent: `#f2bd45`
- secondary accent: `#ffd975`

The launcher artwork reinforces the same warm gold gradient (`#FFF1A0 → #F3C545 → #DEA12A`). Wear therefore uses an OLED-dark background with TrainPilot gold as the primary action/highlight color.

### Delivery order

1. [x] branded Wear Home + start workout from watch;
2. [x] explicit Finish + post-workout summary;
3. [ ] mini calendar;
4. [x] Galaxy Watch7 Health Services recording and live metrics;
5. [x] Tile/complication;
6. [ ] explicit "Open on phone" deep-link action.

Watch-started workouts should start immediately from a locally cached, phone-prepared program snapshot and sync back through the Data Layer. The watch must not require the phone WebView to be visibly open at the moment Start is pressed.


## Wear workout UI and lifecycle (PR #143)

The watch uses the approved round-screen TrainPilot design: black OLED background,
gold primary actions, large values, circular editors, and a horizontally paged
workout/menu. Home offers a recommended day, separate program-day selection,
and resume/discard actions for the existing workout. The workout page shows a
rest countdown automatically and a persistent stopwatch for timed/per-side sets.
The last exercise offers Finish; partial workouts can also finish from the menu.

Finish/discard require confirmation on the watch. The phone retains its canonical
`state.session` / draft / history save pipeline, schedule completion and Health
queue. The watch sends `finishWorkout` or `discardWorkout` with the workout ID,
confirmation and final set snapshot. Delivery is ordered and retried; saved IDs
prevent duplicate history entries and late commands from resurrecting a workout.
A final snapshot can recover an offline watch workout if its start command arrives
late, but a different phone workout is never replaced or discarded.

A durable watch outbox retries failed Data Layer writes. The Home snapshot carries
`workoutResult` as the phone's receipt. The watch labels offline closure as pending,
keeps the final data locally, and only displays phone save success after a receipt.
Phone drafts are included in active snapshots so they can resume on the watch.
Snapshot sequences protect local changes from stale phone snapshots.

Testing: phone lifecycle unit scenarios, a browser test through the actual
canonical finish wrappers, and Wear unit tests for set selection, JSON round trip,
side times, completion and stopwatch pause/resume. Actual Watch7 layout, gestures,
haptics and background timing still require the paired device test.

The 1.2.9 integration adds the mini calendar, Watch7 Health Services (live heart rate and workout
metrics), Tiles/complications and Open on phone. No simulated health
values are displayed in this implementation.
# Wear UX r2: pihenő a gyakorlat képernyőjén

A felhasználó valós Watch7 képei alapján a korábbi sorozatképernyő túl magas volt, az alsó gombok levágódtak. A pihenő Canvas és tartalom külön lapozó-gyökere pedig eltakarta a szöveges visszaszámlálót.

Az elfogadott új felület közvetlen Előző/Rögzítés/Következő kerek vezérlőket használ. A sorozat rögzítése után a gyakorlat oldalán jelenik meg a vékony, fogyó pihenőív és a kis számláló; lejáratkor csak ezek tűnnek el. A Kihagyás kizárólag a pihenőt zárja le. Nincs automatikus sorozatrögzítés vagy gyakorlatváltás. Kész gyakorlatnál ugyanaz az oldal marad, az utolsónál külön megerősített Befejezés érhető el.

Érvényes gyakorlatváltás közben a pihenő lezárása és az abszolút gyakorlatválasztás ebben a sorrendben kerül a tartós Wear parancssorba. Tiltott vagy azonos célú váltás semmit nem módosít. A telefon meglévő skipRest/selectExercise protokollját használjuk.

A hosszú címek két sorba férnek; a főképernyők magassága csökkent, a Mentés/Mégse és a kezdőlapi törlés beljebb került. A középre helyezett tartalom nagyobb rendszer-betűméretnél görgethető. A plank stoppert koppintás indítja/szünetelteti, az időrögzítés külön művelet. Nullázás, oldalváltás és kézi időbeállítás az Edzésmenü / Stopper és oldalak alatt marad.

Új natív regressziók ellenőrzik, hogy a pihenő lezárása nem változtat sorozatot/gyakorlatot, a kézi navigáció megőrzi az összes mérést, és a határgombok nem szakítják meg a pihenőt. A böngészős előnézet ellenőrzése nem helyettesíti a kész APK valós órás tesztjét. A telefonos kód és a startup animáció változatlan.
