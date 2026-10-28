# PR270 – Drive 01–18 UI audit, 2026-10-28

## Scope and evidence

- Audited source: `083fe6e3a5514368933c5937ca79b6015c70f676`, before the current CI-fix agent's changes. No app source edited by this audit.
- All 18 original Drive JPEGs were fetched using the connected Google Drive plugin, opened visually, and checked against the approved JSON index: 18/18 byte sizes and SHA256 hashes match. No missing reference. Originals remain private and are not republished in this report.
- Primary reference: `docs/design/STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.{md,json}`. Only the app viewport is relevant; browser/Studio controls and demo values are excluded.
- Current native evidence inspected: run `40684735676`, merge source `fe0089b135cab8ab407b3a2248ef4f3ea7fa974c`, artifact `12814684812`, 54 original PNGs. Their profile is **320×640 /160dpi/fontScale1.0**, not the owner's **1080×2392 /450dpi/fontScale0.90 (384dp)**. Several screenshots capture a scrolled section. They establish actual rendering at 320dp, not owner-profile pixel parity or complete screen coverage.
- Runtime examples inspected: original-gold-dumbbell Home, Progress anatomy, Health overview, weight journal. The test fixture values are not owner data.

## Priority findings

### P2 – Stored Wear status and live connection need a clearer visual distinction

Reference 15 displays connection information; its demonstration status must not be copied. `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt:123` reads `WearLiveHealthStore.current()`, falls back to that saved packet, then line 227 passes `wear != null` as `wearConnected`. Lines 1171–1176 select success color/text from that Boolean. **The actual label is accurately bounded: “A Wear OS status has been received from the watch” (HU: “Kapcsolódó Wear OS állapot érkezett az óráról.”), not “Connected”.** `android/app/src/main/java/com/repforge/app/WearSyncStoresAndHandoff.kt:62` loads SharedPreferences without freshness or node-reachability validation, so this is evidence of a received packet, not a live connection. The green visual can remain after disconnect; this is a presentation ambiguity/risk, not a verified false literal connection claim or fabricated reading. Any later live-connection display should use genuine Data Layer reachability and explicit last-received age; it should preserve the existing truthful received-status wording when reachability is unknown. No production status change is proposed in the current CI-fix scope.

### P2 – Progress's four cards still represent different metrics

Reference 07 contains PR records, workout count, average volume per strength workout, and average workout duration. `android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt:445` instead renders workout count, total volume, completed sets, and PR-containing workout count, inside a labelled 30-day overview. `periodTotals` is independent of the selected volume range. This is genuine native data, but it is not the requested four-card content, and PR-containing workouts are not the count of individual records. Complete the projections and presentation with explicit period labels; never substitute invented averages or infer unavailable duration.

### P2 – Health's six overview metrics differ from the approved hierarchy

Reference 10 has sleep duration, active calories, pulse, steps, today's workout count and recovery. `android/app/src/main/java/com/repforge/app/ui/health/NativeHealthMetrics.kt:44` selects `sleepScore`, active calories, average heart rate, steps, weight and energy score. The current native Health PNG visibly shows sleep score and weight instead of sleep hours and today's workout count. The responsive 3-column grid already exists at owner width (`HealthDashboardScreen.kt:780`); the missing part is metric meaning/data projection, not the grid. Any native recovery/energy distinction must remain labelled accurately.

### P2 – Health details use navigation and record filters rather than the reference's inline panels

- Reference 12 expands the weight row within Health and includes body-fat/SpO2/VO2max plus change, extrema, new measurement, period filters and chart. `HealthDashboardScreen.kt:846` calls `onWeightJournal` from an action row. The separate `WeightJournalScreen.kt:143` overview now has real current/change/extrema/range/chart UI, but no inline Health expansion or combined body-and-fitness panel.
- Reference 14 expands a compact list of dated daily summaries. `HealthDashboardScreen.kt:323` expands the original month/day/channel selectors and sleep-detail entry. It does not render the reference's dated summary cards.
- Reference 10's pulse row contains a small 7-day curve; `HealthDashboardScreen.kt:841` opens the native `HeartRateRecord` browser and `HealthStudioActionRow` has no sparkline. Keep raw records available while adding an authentic aggregated trend.
- Reference 13 exposes six compact additional readings. `HealthDashboardScreen.kt:307` expands all 26 original metrics, including overview metrics, without the reference's initial six-item grouping. Preserve all readings, but group the primary six before further details.

### P2 – Secondary screens still use the earlier header; several entry points land on a broader page

References 04, 05, 15, 16 and 17 retain the Studio brand/language/theme/settings header above the five tabs. `android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt:321` selects `NativeStudioShellHeader` only for the five main routes; line 343 uses `NativeMainHeader` plus `NativeTopNavigation` for secondary/editor routes. The current weight PNG confirms that different header. Preserve Back and lifecycle/finalization fences when porting the presentation.

`TrainPilotNavigation.kt:490` maps Settings → Connected devices to the general Health destination, rather than opening its connected-devices section; line 491 similarly maps Private photos to the default Progress section. Home's watch row maps to general Settings at line 389. These routes make the reference-specific destination take additional navigation. Add explicit presentation destination/section intent without changing the native engines or bypassing guards.

### P2 – Quick start is a full route, not the requested bottom choice panel

Reference 03 opens a bottom sheet with strength, timed/interval, outdoor and exercise-library choices while Training remains behind it. `TrainingLandingScreen.kt:83` invokes `onQuick(false)`, and `TrainPilotNavigation.kt:423` navigates to `quick_workout`, whose `QuickWorkoutScreen.kt:36` is a separate screen. The native execution paths can stay canonical while the entry chooser becomes the reference bottom sheet. The prototype's workout-mode demo chips are not required product functionality.

## Already implemented; do not repeat the stale a27cc2fe gap list

- Fixed five-tab row at 384dp/default font, horizontal fallback for narrower/larger text: `NativeMainMenu.kt:11`, `NativeStudioShellHeader.kt:210`. The 320dp native screenshot's scrolling is expected and does not prove owner-width failure.
- Progress's three inner tabs and statistics sub-tabs: `ProgressScreen.kt:161`, `StudioProgressTabs.kt`. Volume precedes the summary and genuine native anatomy card. Statistics' empty-content state follows reference 08 when no PR data exists; private photos/weight remain reachable below and should not be removed merely because the screenshot is empty.
- Health responsive 3×2 overview, expandable More/Journal/Health Connect sections, connected-device product summary above legacy BLE tools: `HealthDashboardScreen.kt:282`, `:454`, `:1110`. The old standalone top-level BLE tab is gone; do not report it as still present.
- Native Journal month/day calendar: `JournalScreen.kt:96`. Weight journal change/extrema/new measurement/ranges: `WeightJournalScreen.kt:143`. Strength best-estimate/heaviest-set cards: `ProgressScreen.kt:664`. Best estimated 1RM is deliberately labelled as an estimate; do not rename it a verified personal record to match demo wording.
- Original gold dumbbell and native anatomical artwork remain. Native Health/Wear/GPS absence and error states must remain truthful; the references' 0 bpm/0kg/connected/87-point demo values are not expected results.

## Next visual acceptance evidence

After the functional gate is green, capture the same final app source at the owner profile on Graphite/Gold: Home top/bottom, Training chooser, library, GPS with genuine empty/error or fixture route state, Journal open calendar, all three Progress tabs, Health overview and each expanded panel, devices, Settings top/bottom and language menu. Compare matched app viewports with matched scroll positions. Full current-head owner-profile coverage and physical-device acceptance remain unverified; this source audit is not a visual-parity PASS.

## Later owner-profile evidence at handoff

Run40690747562 / source4fda309b produced70 original1080×2392 PNGs with actual
450dpi/fontScale0.9 readback. Their manifest SHA256/bytes/PNG structure/dimensions
were verified. The phone gate still failed two test-helper cases (121/123 pass),
so this is not a full functional or visual-parity acceptance. Latest candidate17e7aeb8
repairs those helpers without changing the application layout; its new gate40695107665
is in progress at handoff. No fresh all-screen visual-parity claim is made.
