# PR #270 — visual-first AI Studio UI integration (canonical design contract)

**Product decision, 2026-10-27:** the target is **the exported AI Studio Compose design running with every real TrainPilot function**, not the old native screen hierarchy merely recolored. This PR remains draft against `feat/native-android-migration`; `main` must not be merged/overwritten in this work.

## Canonical visual reference — USER LOCKED (2026-10-27)

**Use ONLY the earlier, user-approved 18:49 AI Studio visual export to judge or port the UI:**
[`docs/ai-studio/trainpilot-oct-27-2026-6-49-09-pm.zip`](trainpilot-oct-27-2026-6-49-09-pm.zip).
Original filename `trainpilot-oct-27-2026-6-49-09-pm.zip` (4,011,698 bytes),
SHA-256 `b699d72bc2b673cefbd3dabbdb8164f681c35762e47d19adf42c12e6b22eabf1`.
See [original source index](STUDIO_EXPORT_2026-10-27_1849.md).

**Explicit user correction:** **DO NOT** use the newer 20:14 (`trainpilot-oct-27-2026-8-14-38-pm.zip`) Studio/compiled hybrid as a visual design source or parity benchmark. It launches the older native `com.repforge.app.MainActivity` and its rendered appearance is *not* the accepted design. Its existence does not supersede the 18:49 export.

A production APK must continue to launch the **native** `com.repforge.app.MainActivity`, but the *actual native screen appearance* must reproduce the accepted 18:49 export with real native data, not use the 20:14 APK's appearance.

The approved source has static demo controls and data: omit those controls from production, not the styling/layout of real corresponding states.

## Visual source-of-truth → actual native logic

| Surface | Copy visual specification / composition from Studio | Native state + callbacks to preserve |
| --- | --- | --- |
| Root shell and top tabs | `com/example/ui/components/TrainPilotCommonComponents.kt`: `TrainPilotGlobalTopHeaderAndNav`, `TopNavPillItem`, header spacing, actual selected 5-tab style, theme picker; **keep the original native gold dumbbell in place of Studio mountain** | `NativeStudioShellHeader`, `TrainPilotBrandLogo`, `NativeMainHeader`, `NativeTopNavigation`, `NativeMainMenu`; real navigation guards; TOP tabs are intentional |
| Theme + typography | `com/example/ui/theme/TrainPilotPalettes.kt`, `Type.kt`, `Theme.kt`; reference Graphite & Gold `#0E1015/#171A21/#1F242D/#E5B842/#4CA6A8/#F3F4F6/#B6BFCF/#2B313D`; Space Grotesk / Plus Jakarta / JetBrains Mono; 32 original Studio palettes | `TrainPilotTheme`, `NativeThemeTokens`, stored user settings and prior native theme keys must not be destroyed |
| Home | `com/example/ui/screens/HomeScreen.kt`: greeting, planner, hero states, action grid, health mini-cards, Coach/Watch cards | `ui/home/HomeScreen.kt`, `HomeViewModel`, real active workout, imported drafts, real program/day and navigation |
| Training/program/planning/active workout | `WorkoutAndPlanningScreens.kt`, `ActiveMeasurementScreens.kt` | Native Training / program repository, active session, real set logging, interval ownership, GPS, Wear handoff, finalization |
| Journal, details | `LogAndProgressScreens.kt` | `ui/journal/JournalScreen.kt`, paging/search/month/detail/editor/delete/tombstones, photo/privacy and route callbacks |
| Progress | `LogAndProgressScreens.kt`: visually exact period selector, bar panel, expandable stat grid, front/back anatomy card, advice card | Native `ProgressScreen`, actual volume/PR/strength queries and `NativeMuscleLoadVisual` based on **main original anatomy masks**, no fixed percentages or fictional records |
| Health + Coach | `HealthCoachAndSettingsScreens.kt` | Native Health dashboard / BLE / Connect / Samsung / permissions / Coach state; unknown or unavailable readings stay missing |
| Map | `AnatomicalBodyAndMapComponents.kt` and route controls in Studio screens | Native `WorkoutRouteMapDialog` and saved **segmented** GPS points. Do not reconnect missing GPS segments or show the Studio fixed Margitsziget demo trail |
| Settings / localization | Studio settings and 32 themes / 4 languages | Native settings/backup/theme/language, prior migration data, Android packaging and main launcher icon |

## Explicit user acceptance rule: pixel-faithful Studio presentation (2026-10-27)

The user has expressly confirmed that **every Studio UI surface, element dimension, card, padding, color, and arrangement had already been finalized to their satisfaction**. Therefore:

- Do **not** improvise new native card sizes, spacing, colors, layouts, visual hierarchy, radii or icon choices. Studio is the exact design source, not a loose inspiration.
- Port Studio Composable structure, padding, typography, token values, interaction style and responsive layout **directly from the approved 18:49 export**; only replace DEMO data/state/actions with native production state and callbacks.
- Preserve the user-approved top navigation exactly as placed and drawn in Studio.
- Preserve the original main anatomy mask image and actual muscle load calculation as an explicitly requested exception.
- **USER-LOCKED BRAND EXCEPTION (2026-10-28):** keep the *previous original golden dumbbell logo*, exactly as used by the installed app launcher (`@mipmap/trainpilot_launcher` → `@drawable/trainpilot_launcher_foreground`). Do NOT use the approved Studio 18:49 mountain symbol. The source-approved layout, 5 tabs, card dimensions, colors, margins and typography still stand; only the brand *artwork* is intentionally substituted. `TrainPilotBrandLogo` is the single shared vector-rendering wrapper for all native headers, not a new drawing.
- Do not infer visual completeness from a successful Kotlin build. Capture **real screenshots on the exact PR HEAD** and compare side-by-side with Studio at matching phone dimensions/theme/language/state; fix discrepancies before marking a screen accepted.
- Avoid adding options, badges, headers, explanatory text, or a new visual pattern merely to expose a native feature; fit real functions within the established Studio screens. Missing feature entry points must be handled without degrading established visual identity.
- Already committed partial restylings, including the Studio graphite-gold theme, header/hero changes and volume cards, are **WIP and unapproved until screenshot parity**. In particular, the extra graphite-gold theme entry added alongside 32 old native themes is a temporary compatibility measure, not the final 32 Studio palettes: match the original Studio palette registry exactly while preserving saved legacy keys in data.
- The full integrated app must still preserve the native app's original program, training, planning, GPS, health, Coach, journal, backup, Wear and record-handling behavior.

## Correct implementation strategy

1. Preserve `com.repforge.app` applicationId, Room entities and migrations, signed update identity, existing native engine and Wear/Health/GPS contracts. Keep `main` unchanged.
2. Port the **actual Studio composable hierarchy** screen-by-screen into native screen files, replacing presentation containers and preserving native ViewModels / callbacks / test tags. Retain Studio layout order, outer margins, card paddings, gradients, typography, color and icons. Do not attempt a piecemeal old-UI restyle as an acceptable final output.
3. Wire each source screen's demo click/test-state props to a native presentation model projected from real domain state. Studio demo-only mode-switch pills and DEMO text do not ship; the visual states themselves MUST appear under the corresponding real conditions.
4. Prefer actual `NativeMuscleLoadVisual` original detailed anatomy over Studio silhouette, and actual route data over illustrated map.
5. Before marking a screen complete, produce an unmodified **latest-head** Android emulator PNG and compare it with the supplied Studio screenshot at matched viewport, language, palette and demo-equivalent state. Keep screenshots in a GitHub Actions artifact and link the exact commit SHA. Never reuse old CI screenshots as evidence for new commits.
6. Run native Android compile and JVM tests, phone Compose UI, Wear UI, and signed-upgrade validation on the same commit. An earlier fast build is not evidence of full device behavior.

## 2026-10-27 first foundation step — implemented in this PR

- A new installation selects a **Graphite & Gold** theme with the original Studio color values, while explicit existing saved native themes keep their stored key and colors.
- The original Studio color tokens are used only for `graphite_gold` in `NativeThemeTokens`, not forced onto other historical palettes. The theme picker retains its preexisting 32 options plus a Studio reference option; the **exact 32 Studio palette mappings remain outstanding**.
- The real top tabs now use Studio selected gold tint (22% overlay), Studio 48dp min pill height, border and icons; the 5 native routes / guards / test tags survive.
- Shared hero cards have the Studio gold-to-outline gradient, and Home vertical margins/spacing are aligned with the source.
- **This is foundation work, not whole-app visual parity.** Existing complex screen composition and various older native controls are still visibly different.

## Next high-priority development, not yet complete

1. Rewrite native Home presentation tree to match the Studio source **section-for-section**, preserving genuine active/scheduled/Coach state and every existing callback. Emulator screenshot first.
2. Bring over the Studio Training/Log/Progress card composition and live detailed state with real native data. Visual compare each screen after working build.
3. Native Health, Coach, Settings and all secondary/editing screens. Verify small-screen text, permissions, missing readings, and signed upgrade.
4. Audit the whole resulting UI against the **approved 18:49 export** and the separate four mockups before removing draft status; **never** use the 20:14 compiled/hybrid build as a visual benchmark.

## Resume from another ChatGPT/Codex session

Read this document, [`docs/STUDIO_UI_RESUME.md`](../STUDIO_UI_RESUME.md), and PR #270's current **HEAD**. Use the checked-in **18:49 ZIP exclusively as the accepted visual reference**. Do NOT use the newer 20:14 ZIP as a design benchmark. Report exact commits, test run URLs and real unmodified screenshot artifacts. No unrequested merge or release.
