# PR #270 — retrospective UI audit against ACCEPTED 18:49 Studio export

**Locked reference (user instruction):** `docs/ai-studio/trainpilot-oct-27-2026-6-49-09-pm.zip`, SHA-256 `b699d72bc2b673cefbd3dabbdb8164f681c35762e47d19adf42c12e6b22eabf1`. **Do not compare against the newer 20:14 hybrid APK/project appearance.**

## Audit outcome of UI modifications ALREADY on PR #270

**Important:** "partly" means the old native screen was restyled or given a partial Studio component; it does **not** imply the approved 18:49 Studio component tree was ported.

| Accepted 18:49 Studio reference | Current native implementation | Status | Required corrective action |
| --- | --- | --- | --- |
| `ui/components/TrainPilotCommonComponents.kt` → `TrainPilotGlobalTopHeaderAndNav`, `TopNavPillItem` | `ui/navigation/NativeMainHeader.kt`, `NativeTopNavigation` | PARTIAL | The top-five-tabs placement, pill colors, font sizing, borders, 48dp, and small-screen scroll now follow Studio; header control composition (flag/dropdown, exact theme selector with palette number, responsive spacing) still differs. Remove only the prototype's DEMO badge |
| `ui/theme/TrainPilotPalettes.kt`, `Type.kt` | `NativeThemeTokens.kt`, `TrainPilotTheme.kt`, `NativeStudioTypography.kt`, settings | PARTIAL | `graphite_gold` default tokens now match (background `#0E1015`, card `#171A21`, elevated `#1F242D`, gold `#E5B842`, teal `#4CA6A8`). The exact **Studio 32-palette** registry, names, per-palette background/text/border colors and control styling are NOT yet mapped into the native app. Keep existing saved legacy themes and data intact |
| `ui/screens/HomeScreen.kt` → `HomeScreen`, `HomeVitalCard`, `HomeQuickShortcutTile` | `ui/home/HomeScreen.kt` | PARTIAL | Native real-state cards exist; port exact Studio layout/ordering/structure, hero states, compact vital cards, Coach/Watch actions rather than redesigning from generic native cards |
| `ui/screens/WorkoutAndPlanningScreens.kt` → `WorkoutTabScreen`, `ShortcutSquareCard`, `ProgramsListScreen`, `ProgramEditorScreen`, `CalendarPlannerScreen`, `ExerciseLibraryScreen` | `ui/programs/TrainingLandingScreen.kt`, native program/planner/exercise destinations | PARTIAL | Real training navigation kept, but source component hierarchy and all editing/details dimensions are not 1:1; port exact Studio presentation + keep native edit/load/session contracts |
| `ui/screens/LogAndProgressScreens.kt` → `LogTabScreen`, `LogWorkoutCard`, `LogWorkoutDetailScreen`, `LogRouteMapScreen` | `ui/journal/JournalScreen.kt`, route dialog and editor | PARTIAL | Saved real paging/GPS/photo/edit/delete functionality preserved; original Studio log cards and route controls not fully ported |
| `ui/screens/LogAndProgressScreens.kt` → `ProgressTabScreen`, `MainProgressVolumeAndMuscleSection`, `ExpandableStatSummaryTile`, `MainStatisticsRecordsAndCardioSection` | `ui/progress/ProgressScreen.kt` + real Room-derived data | PARTIAL | Actual 1/7/30-day/3mo volume range and real 1RM/strength exists; source stat-card layout, expand behavior and advice/details still not identical. **Preserve main's actual full-body anatomy masks and muscle load** in the same approved Studio card layout |
| `ui/screens/HealthCoachAndSettingsScreens.kt` → `HealthTabScreen`, `MainTodayStatusTile`, `PulseInnerBox`, `ExpandableHealthSectionCard` | `ui/health/HealthDashboardScreen.kt` | PARTIAL | 6 genuine reading cards + selected-day filter already exist, but visual structure/pulse and expanded sections still differ; missing readings must remain missing |
| `ui/screens/HealthCoachAndSettingsScreens.kt` → `CoachDetailScreen`, `SettingsMainScreen`, `WatchAndSyncScreen`, `BackupRestoreScreen`, `PermissionsScreen`, `PrivatePhotosScreen` | Native Coach/Settings/Wear/Backup/Permissions screens | NOT YET VERIFIED FOR 1:1 | Audit and visually port each original Studio screen without removing any actual functionality or permissions |
| `ui/screens/ActiveMeasurementScreens.kt`, `ui/components/AnatomicalBodyAndMapComponents.kt` | Native active workout, workout route and wearable state | NOT YET VERIFIED FOR 1:1 | Port Studio visual states for active measurement/workout; preserve real GPS segmented paths, live workout state, Wear handoff, app lifecycle and original icon |

## Acceptance gate from now on

1. The **18:49 Studio ZIP** is the one and only visual golden reference, not the later compiled/merged 20:14 project. Extract source from the GitHub ZIP itself if resuming on another machine.
2. Audit and correct **all previously modified UI** along with not-yet-touched screens. A successful build never implies pixel parity.
3. Each screen is complete only when (a) the actual source composable structure, dimensions, content positions, palette and typography match the approved 18:49 export, (b) original production functions, guards, backup, Room, GPS, Health, Coach, Wear and signing work, (c) exact-current-commit emulator screenshots demonstrate visual parity at the same viewport/language/theme/state, (d) same-commit native Android, Wear and signed-upgrade CI gates pass.
4. No DEMO state switcher, fake values or fixed GPS route in the release; the approved visual states still display on genuine events.
5. Keep PR #270 draft, `main` unchanged. Do not present older emulator screenshots or the 20:14 compiled UI as proof of visual parity.

## Home visual re-port implementation on PR #270 (new, NOT yet screenshot-approved)

- `ui/home/HomeScreen.kt` now substantially replaces the old native Home presentation tree with the **18:49 Studio Home source** structure: genuine workout 56dp warm-accent hero icon, program/day details and 50dp full-width CTA; Studio full-width 44dp-icon Quick Workout row; separate Watch status row; genuine 2×2 sleep/pulse/steps/recovery bento; 3-column compact programs/calendar/Coach shortcuts; actual activity/goal and Coach recovery lower cards. It preserves real callbacks and no hardcoded readings, fake watch battery, synthetic Coach score or DEMO mode controls.
- `TrainPilotNavigation.kt` routes the Watch overview action to actual native Settings rather than incorrectly to the profile; the original GPS/outdoor workout remains on the existing Training route. All native workout/session/draft/loading/onboarding callbacks remain.
- The historical 6-item `TodayMetricsGrid` and its accessibility test remain available; `NativeHomeMetricLayoutUiTest` now also verifies the Studio 2×2 Home bento and unavailable readings as missing.
- `NativeThemeTokensTest` repaired after switching new-install default to graphite/gold: legacy basic/vivid test cases now explicitly select their own theme keys; stored palette behavior stays unchanged.
- **Current UI source commit**: `5826baa6c3f399df9ac936c98d6104d078684176`. [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40574470177) succeeded on that exact SHA; [Full Android/Wear/phone UI/signed-upgrade gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40574479567) was queued at the last check, NOT PASS. No current-head emulator screenshot verified; therefore this Home port is **visual WIP, not approved**.
- **Remaining Home discrepancies to inspect against original 18:49 Studio**: full 32 Studio palettes and exact per-palette surfaces, Home personal planner typography/text/spacing, device/watch settings destination, three shortcut subtitles on compact screens, Studio goals/provider error footers and state-specific screen snapshots. The main anatomy remains the separately required native renderer for Progress.

## Current changes following this audit

- `docs/ai-studio/PR270_VISUAL_FIRST_INTEGRATION.md` locks the **18:49** reference, explicitly discards the 20:14 appearance.
- `NativeMainHeader.kt` restores the original 18:49 top header's **12dp horizontal / 10dp top / 8dp bottom** margins and 48dp min row, brand tracking -0.2sp; top-tab label padding now matches **2dp horizontal / 7dp vertical** on normal width.
- Pending: the remaining 1:1 screen-tree ports and matching current-HEAD screenshot evidence. These are NOT claimed complete by this document.
