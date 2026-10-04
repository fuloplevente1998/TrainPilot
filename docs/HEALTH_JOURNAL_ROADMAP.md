# #110 — unified Health Connect / BLE journal

Authoritative requirements: [issue #110](https://github.com/fuloplevente1998/TrainPilot/issues/110), updated 2026-10-04T00:57:16Z. This roadmap distinguishes implemented candidates from planned work; it does not claim completion of the whole issue.

## Completed predecessor: phase 1, 1.1.4 /2702

60-second live discovery with stop/countdown, 120 bounded retained devices and selected-watch priority. Explicit remembered-watch selection is stored privately in native preferences. The trial can reconnect to that exact address on open/resume, verifies its supported service and caps automatic attempts at three. Forget, disconnect, scan, navigation/background and local erase stop or remove the selection as appropriate. Rotating addresses require manual reselection. No automatic proprietary query, background service or new health import is included. [Phone procedure](BLUETOOTH_GT4PRO.md).

## Current candidate: native journal, 1.2.0 /2703

Health Connect raw records from every permitted origin now persist in one transactional native SQLite store with original type/ID, last-modified time and origin package. Changes/deletions are applied idempotently, with deletion tombstones and conservative reconciliation of recent complete queries. Legacy daily/recovery data migrates once; WebView receives only a 30-day projection and 30-record pages. Native generation guards block stale writes across restore/erase. Sleep is grouped by completion day, preserves source/stages and follows the existing source selection rule.

The new Health journal has date/source filters, bounded pages, source-preserving daily cards and freshness/partial markers. Mixed-source HC priority aggregates remain explicitly separate from per-origin records. Remembered-watch current steps can be logged with a separate opt-in; repeated snapshots replace, zero is valid, the watch uses a private random local identity and **phone observation time**, not an unverified historical timestamp. HC and BLE steps never add together; the preferred daily step source is selectable. Existing stable workout IDs, exact active intervals and pause-aware HC summaries remain unchanged; a daily BLE total does not manufacture a workout value.

HC refresh on app open and current-watch logging are independent opt-ins, disabled by default. Foreground HC imports require existing read permissions, and native reads stop on backgrounding. There is no HC background permission or shared BLE foreground service in this candidate. Whole native data is included only in health-consented manual JSON/ZIP, excluded from Drive snapshots, and covered by staged transactional restore/recovery and all-source local erase. Full export is bounded to 18 MB. Source/device addresses and raw notifications remain excluded from diagnostics and cloud snapshots.

Home receives a compact Goals button that opens the complete existing editor. [Phone acceptance procedure and limitations](releases/v1.2.0.md). Keep #110 open: positive watch values/history, service/background capability and physical acceptance remain outstanding.

## Remaining acceptance and follow-up phases

1. Validate positive steps, calories and distance, including units, cumulative semantics and timestamps. Trace historical sleep/pulse framing, chunks, acknowledgements and potential watch-buffer clearing before import. Add only physically supported fields.
2. **Implemented in the 2703 candidate; verify on the phone:** a **single native transactional store** for Health Connect and BLE. Persist original record type/ID, lastModifiedTime and dataOrigin for Health Connect, and verified device/time/record identity for BLE. Upsert by source identity, apply Health Connect deletions, and preserve zero/missing distinctions. Migrate existing healthLedgerV1 once, idempotently; the WebView becomes a bounded projection, not the primary store. Default Health Connect imports read all permitted origins without DataOrigin restriction. Aggregate-only values must not pretend to have a per-source record identity.
3. **Implemented in the 2703 candidate; verify on the phone:** a paginated Health journal with date/source filters, source-preserving records, selected-source summaries, freshness and the existing visual widths/borders. Sleep belongs to its completion day. Repeated cumulative totals replace previous snapshots instead of being summed. Overlapping Health Connect and BLE totals are not added together.
4. Connect records to stable workout IDs and exact active intervals, excluding pauses and merging overlaps once. Recompute affected summaries after delayed imports/time edits. Daily/hourly totals cannot manufacture precise workout calories/steps. Sleep stays daily recovery context. Existing Health Connect workout summaries and deletions remain valid.
5. Implement the shared native BLE controller and opt-in connectedDevice foreground service, supported permissions/notification and Companion Device Manager lifecycle. Use bounded reconnect backoff and serialized GATT operations; no continuous high-power scanning. Test RDFit contention and interrupted-history recovery. Health Connect background access is a **separate**, capability/READ_HEALTH_DATA_IN_BACKGROUND-gated path; without it use foreground/app-open sync. BLE service must not bypass Health Connect permission. Force-stop requires reopening, and OEM/boot constraints need physical checks.
6. **Implemented for the 2703 journal; physical recovery acceptance remains:** erase, health-consented backup/restore and cloud rules to **both** record channels, including migrations and deletion history. Keep device addresses/raw notifications out of public diagnostics. Verify signed upgrade/source equality and battery/UI performance per phase.

## Required next-phase acceptance

- Without a connected watch, permitted phone/Samsung Health/other Health Connect records import and survive restart.
- BLE and Health Connect records for the same day remain independently visible; selected-source daily/workout summaries do not double count.
- Repeated import, modifications/deletions, offline catch-up, midnight/time-zone changes, cross-midnight sleep, partial responses and interrupted native writes preserve correct records.
- Pause-aware workout values require adequate timestamped input; missing/insufficient data is explicit.
- Local erase and health consent cover the native store and all backup/cloud paths.
- Full existing Node/Chromium/Android/source/signature and unchanged performance gates, then physical-phone verification of the delivered phase.

The published 2701 APK/tag and stable main are unchanged by this roadmap. Phase 1 is a separate candidate based on the existing BLE work that already contains the latest approved main; future integration starts from the latest approved main plus the verified dependencies. The stacked candidates are not automatically merged by this issue.
