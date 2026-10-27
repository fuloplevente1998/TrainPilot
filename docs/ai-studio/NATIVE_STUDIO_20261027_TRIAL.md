# TrainPilot AI Studio 2026-10-27 — native trial (not release-ready)

## Provenance
- Source supplied by user: `trainpilot-oct-27-2026-3-58-04-pm.zip`
- SHA-256: `574f17f639b91037a6e0bdea428f350d4c5950307227511eafc32e7f64bcefcc`.
- Design prototype: `com.aistudio.trainpilot.tpux01`, Kotlin + Jetpack Compose, single module and DEMO state.
- Source of functional truth: `feat/native-android-migration` (`com.repforge.app`), with main read-only.

## This first, intentionally limited change
- Position the five primary **functional native** tabs in a Studio-style top pill bar below the existing header.
- Preserve native canonical routes, navigation guards, test tags `navigation_*`, back handling, and active-workout banner.
- Select the existing **main TrainPilot icon** in the native Android manifest; the matching launcher resources already exist.
- Do not replace native screen composables, ViewModels or real data with Studio DEMO screens.

## Design clarification from user (2026-10-27)
- The five-tab **TOP navigation** is a deliberate, final design choice. Do not move it back to the bottom, even if older design handoffs describe bottom tabs.
- AI Studio export is explicitly an **interactive DEMO/prototype**. Text such as `DEMO MINTAADAT`, buttons used to switch Home between `PAUSED_WORKOUT`, `PLANNED_NEXT`, and `COACH_LIGHT_DAY`, fake workout metrics or illustrated GPS scenery are testing controls, NOT permanent features.
- In the *Studio design preview*, retain these switches because they let the user review alternate card states without completing workouts. In the *integrated native APK*, state must come from the existing ViewModels/repositories, with these demo-only controls and labels absent. Do **not** remove valid functional actions merely because their prototype callback temporarily manipulates demo state.
- Preserve the visual appearance and interaction patterns of each reviewed state, but render it when the real underlying app condition occurs. For screenshot-only prototyping, label made-up data clearly as a demo.
- Distinguish temporary demo/test-only controls from authentic functional controls using the native app's source and user acceptance; don't delete the latter.

## Important visual integration next
1. Bring Studio surface hierarchy, icons, typography and selected 32-palette color tokens to the existing native components. Preserve 4 locale variants, accessibility, original `testTag` semantics, and navigation. Keep top navigation (user choice).
2. Progress: keep `NativeMuscleLoadVisual` and the actual main anatomy mask resources with `NativeProgressQuery.muscle()` from native Room workout history. **Do not** use the Studio fixed `MuscleLoadItem` values.
3. GPS map: replace the Studio hard-coded Margitsziget scenery / fixed kilometer splits with saved segmented GPS route data, while visually adopting mockup 03's map controls, route legend, card and close button. Never join route gaps. A real base map needs separate provider support and real-device test; fake canvas is not a substitute.
4. Home, training, journal, coach, health, progress, settings, modals: integrate component by component into native data/callback contracts without replacing engines, Room, Health, GPS, backup, finalization, or Wear sync.
5. Keep app package `com.repforge.app`, release signature continuity, data migration/backup guarantees, and target version governed by the native branch.

## Acceptance before any user APK
- Android build + unit tests + native phone UI test, Wear build/test and signed upgrade validation.
- Real-device top tab / back / active-workout and 320dp with large text, all 4 languages.
- Health / Progress detail parity and actual anatomy image updating from recorded workouts.
- Route shows its original segmented GPS path; no fake location, no false live data.
- On-device original signing key upgrade protection; never install a different-signature APK over user data.
- Actual emulator PNG review against Studio reference + main. 
- No merge to main or release until reviewed.

## Status
**Only the first top-navigation and app-icon trial is implemented in this branch.**
The complete Studio redesign, real map tiles, the remaining screen changes, and the required functional/system acceptance tests are still pending. An APK from this intermediate commit is not the final Studio UI.
