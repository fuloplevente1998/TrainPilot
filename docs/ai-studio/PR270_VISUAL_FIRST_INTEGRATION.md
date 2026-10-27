# PR #270 — visual-first AI Studio UI integration (canonical design contract)

**Product decision, 2026-10-27:** the target is **the exported AI Studio Compose design running with every real TrainPilot function**, not the old native screen hierarchy merely recolored. This PR remains draft against `feat/native-android-migration`; `main` must not be merged/overwritten in this work.

## Which export?

- Current newest ChatGPT-supplied ZIP (20:14 local time): `trainpilot-oct-27-2026-8-14-38-pm.zip`, **8.5 MB**, SHA-256 `05c5f58cb9f184a05bd6b88c1e6d7460d456b6a9c5a4e83051c8ff432ce1a082`; 479 ZIP entries. Present in the originating ChatGPT conversation; **not yet verified as a binary GitHub upload**.
- Already archived on this branch (18:49 Studio design): [`docs/ai-studio/trainpilot-oct-27-2026-6-49-09-pm.zip`](trainpilot-oct-27-2026-6-49-09-pm.zip) and [source index](STUDIO_EXPORT_2026-10-27_1849.md); 18:49 is older.
- The 20:14 ZIP is **one Android application module containing TWO Compose screen families**: `app/src/main/java/com/example/` (Studio UI DEMO) and `app/src/main/java/com/repforge/app/` (production native app). Its launch manifest opens **`com.repforge.app.MainActivity`**, which explains why directly building that ZIP opens the older native look. Simply including the Studio classes does NOT wire them into the Activity.
- **Do not switch the production manifest to `com.example.MainActivity` as a shortcut.** It has standalone demo state, a separate package and no guaranteed production identity, real workouts, signed-upgrade continuity, migration, Room, Wear or Health integration.

## Visual source-of-truth → actual native logic

| Surface | Copy visual specification / composition from Studio | Native state + callbacks to preserve |
| --- | --- | --- |
| Root shell and top tabs | `com/example/ui/components/TrainPilotCommonComponents.kt`: `TrainPilotGlobalTopHeaderAndNav`, `TopNavPillItem`, header spacing, actual selected 5-tab style, theme picker | `NativeMainHeader`, `NativeTopNavigation`, `NativeMainMenu`, real navigation guards; TOP tabs are intentional |
| Theme + typography | `com/example/ui/theme/TrainPilotPalettes.kt`, `Type.kt`, `Theme.kt`; reference Graphite & Gold `#0E1015/#171A21/#1F242D/#E5B842/#4CA6A8/#F3F4F6/#B6BFCF/#2B313D`; Space Grotesk / Plus Jakarta / JetBrains Mono; 32 original Studio palettes | `TrainPilotTheme`, `NativeThemeTokens`, stored user settings and prior native theme keys must not be destroyed |
| Home | `com/example/ui/screens/HomeScreen.kt`: greeting, planner, hero states, action grid, health mini-cards, Coach/Watch cards | `ui/home/HomeScreen.kt`, `HomeViewModel`, real active workout, imported drafts, real program/day and navigation |
| Training/program/planning/active workout | `WorkoutAndPlanningScreens.kt`, `ActiveMeasurementScreens.kt` | Native Training / program repository, active session, real set logging, interval ownership, GPS, Wear handoff, finalization |
| Journal, details | `LogAndProgressScreens.kt` | `ui/journal/JournalScreen.kt`, paging/search/month/detail/editor/delete/tombstones, photo/privacy and route callbacks |
| Progress | `LogAndProgressScreens.kt`: visually exact period selector, bar panel, expandable stat grid, front/back anatomy card, advice card | Native `ProgressScreen`, actual volume/PR/strength queries and `NativeMuscleLoadVisual` based on **main original anatomy masks**, no fixed percentages or fictional records |
| Health + Coach | `HealthCoachAndSettingsScreens.kt` | Native Health dashboard / BLE / Connect / Samsung / permissions / Coach state; unknown or unavailable readings stay missing |
| Map | `AnatomicalBodyAndMapComponents.kt` and route controls in Studio screens | Native `WorkoutRouteMapDialog` and saved **segmented** GPS points. Do not reconnect missing GPS segments or show the Studio fixed Margitsziget demo trail |
| Settings / localization | Studio settings and 32 themes / 4 languages | Native settings/backup/theme/language, prior migration data, Android packaging and main launcher icon |

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
4. Audit the whole resulting UI against the 20:14 export and the separate four mockups before removing draft status.

## Resume from another ChatGPT/Codex session

Read this document, [`docs/STUDIO_UI_RESUME.md`](../STUDIO_UI_RESUME.md), and PR #270's current **HEAD**. Use the **20:14 ZIP** if available (verify hash above), otherwise the checked-in older 18:49 design as a provisional UI reference. The next agent must not claim the 20:14 binary was GitHub-uploaded. Report exact commits, test run URLs and real unmodified screenshot artifacts. No unrequested merge or release.
