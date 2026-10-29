# PR270 Studio visual recheck — 2026-10-29

Reviewed app source: `151ced0ac367b5b1d5cccc6294d42317a71fa280`.
CI-only repair: `79ccd5c39575c8654dc4fc56655627e801a46def`; Android source unchanged.
No new product features, duplicate native editors or demo data added.

## Evidence actually inspected

- Canonical 40761838896, merge a59be2bcfe063673cf9c578ffe15b85edd4b9dba:
  original phone artifact 12849821663,71 PNGs, 320×640px / 160 dpi / fontScale 1.0.
- Independently parsed raw phone instrumentation/XML: 123 PASS, 0 failure/error/skip,
  0 missing; all 71 PNG bytes/CRC/hash verified and app/test APK hashes matched
  against independently downloaded artifacts 12850085290/12849876502.
- Opened original native Home/metrics/Health/Progress/settings/library images.
- Downloaded and visually opened Drive reference 01/02/04/07/10/12/16.
  All 7 original byte hashes match STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.json;
  JPEG decoding verified. Original user pictures are not republished.
- The Studio browser frame and emulator controls are excluded from the comparison.
  Different data, palette and scroll positions are explicitly accounted for.

## Remaining findings

1. **P2 compact brand label truncation, verified in PNG and source.** At 320 dp,
   Home's brand reads `Train...`. NativeStudioShellHeader.kt reserves 48 dp controls,
   an unweighted right control row and a weighted logo/title row; the title uses
   ellipsis. Keep the original gold logo, usable 48 dp touch targets and full
   navigation semantics when adapting this compact header. 384 dp owner-profile
   behavior is not established by this screenshot.
2. **P2 Health date formatting differs, verified in source and PNG.**
   HealthSelectedDayOverview passes raw state.selectedDay to the localized status
   resource, yielding 2026-10-28. The Studio reference uses a localized date
   (e.g. 2026. okt. 27.). Reuse a locale-aware native formatter for presentation;
   retain ISO dates as query/storage keys. No change is made in the CI repair.
3. **P2 visual evidence lacks matched overview captures.** Current p1_progress
   intentionally scrolls to the anatomical card before capture
   (NativePhoneNavigationTest.kt). It cannot prove the top volume/summary layout.
   The settings PNG intentionally shows the expanded palette, not the initial
   collapsed screen; do not report it as the default settings hierarchy. Need
   top-of-page Progress and initial Settings screenshots in Graphite/Gold on
   1080×2392 / 450 dpi / fontScale 0.90 before claiming full parity.

## Confirmed existing implementations: do not duplicate

- Pulse trend uses query.channel (ebe9e739), matching selected-channel detail.
- Recovery source/estimate/partial/stale/read-error presentation is implemented
  (32e95607), including health_recovery_source.
- Health's new measurement routes to weight_journal_add and the same guarded
  WeightJournalScreen(initiallyAdding=true), not a new writer (151ced0a).
- Progress has native volume, four metrics and muscle card; the scrolled anatomy
  PNG does not mean the top panels are absent.
- Health uses 3 columns at width >= 350 dp / fontScale <= 1.2; the 320 dp 2-column image is
  intentional accessibility adaptation, not evidence of a 384 dp 3-column defect.
- Settings has collapsed theme summary and real cloud summary in current source.
  The expanded theme-selector PNG is not proof that these are missing.

## Acceptance boundary

This is a bounded visual/source recheck, not full 18-screen pixel parity or
physical-device acceptance. Existing real Room/Health/Wear/GPS/workout paths,
applicationId, version 2736 and signing configuration are unchanged.
New canonical40815986152 is now7/7 SUCCESS: raw1346 JVM,123 phone,183 Wear
PASS,71 verified PNGs and both signed upgrades SUCCESS. This establishes
this run of the Wear cold-boot repair; full visual/device acceptance remains open.
