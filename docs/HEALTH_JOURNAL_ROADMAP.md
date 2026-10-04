# #110 — unified Health Connect / BLE journal

Authoritative requirements: [issue #110](https://github.com/fuloplevente1998/TrainPilot/issues/110), updated 2026-10-04T00:57:16Z. The user accepted release 1.2.0 /2703 and authorized main integration on 2026-10-04. The shipped phase is closed under #110; all unfinished requirements continue in [#113](https://github.com/fuloplevente1998/TrainPilot/issues/113). General acceptance does not establish untested proprietary watch values or background behavior.

## Completed predecessor: phase 1, 1.1.4 /2702

60-second live discovery with stop/countdown, 120 bounded retained devices and selected-watch priority. Explicit remembered-watch selection is stored privately in native preferences. The trial can reconnect to that exact address on open/resume, verifies its supported service and caps automatic attempts at three. Forget, disconnect, scan, navigation/background and local erase stop or remove the selection as appropriate. Rotating addresses require manual reselection. No automatic proprietary query, background service or new health import is included. [Phone procedure](BLUETOOTH_GT4PRO.md).

## Accepted release: native journal, 1.2.0 /2703

Health Connect raw records from every permitted origin now persist in one transactional native SQLite store with original type/ID, last-modified time and origin package. Changes/deletions are applied idempotently, with deletion tombstones and conservative reconciliation of recent complete queries. Legacy daily/recovery data migrates once; WebView receives only a 30-day projection and 30-record pages. Native generation guards block stale writes across restore/erase. Sleep is grouped by completion day, preserves source/stages and follows the existing source selection rule.

The new Health journal has date/source filters, bounded pages, source-preserving daily cards and freshness/partial markers. Mixed-source HC priority aggregates remain explicitly separate from per-origin records. Remembered-watch current steps can be logged with a separate opt-in; repeated snapshots replace, zero is valid, the watch uses a private random local identity and **phone observation time**, not an unverified historical timestamp. HC and BLE steps never add together; the preferred daily step source is selectable. Existing stable workout IDs, exact active intervals and pause-aware HC summaries remain unchanged; a daily BLE total does not manufacture a workout value.

HC refresh on app open and current-watch logging are independent opt-ins, disabled by default. Foreground HC imports require existing read permissions, and native reads stop on backgrounding. There is no HC background permission or shared BLE foreground service in this release. Whole native data is included only in health-consented manual JSON/ZIP, excluded from Drive snapshots, and covered by staged transactional restore/recovery and all-source local erase. Full export is bounded to 18 MB. Source/device addresses and raw notifications remain excluded from diagnostics and cloud snapshots.

Home receives a compact Goals button that opens the complete existing editor. [Usage and limitations](releases/v1.2.0.md). Follow-up #113 preserves positive watch values/history, service/background capability, HRV investigation and source-summary UX. In 2703 the per-origin filter shows raw measurements; mixed HC daily aggregates are shown only in All sources / Health Connect. Samsung Health is an origin through HC, not a separate integration.

## Remaining acceptance and follow-up phases

1. Validate positive steps, calories and distance, including units, cumulative semantics and timestamps. Trace historical sleep/pulse framing, chunks, acknowledgements and potential watch-buffer clearing before import. Add only physically supported fields.
2. **Implemented in the accepted 2703 release:** a **single native transactional store** for Health Connect and BLE. Persist original record type/ID, lastModifiedTime and dataOrigin for Health Connect, and verified device/time/record identity for BLE. Upsert by source identity, apply Health Connect deletions, and preserve zero/missing distinctions. Migrate existing healthLedgerV1 once, idempotently; the WebView becomes a bounded projection, not the primary store. Default Health Connect imports read all permitted origins without DataOrigin restriction. Aggregate-only values must not pretend to have a per-source record identity.
3. **Implemented in the accepted 2703 release:** a paginated Health journal with date/source filters, source-preserving records, selected-source summaries, freshness and the existing visual widths/borders. Sleep belongs to its completion day. Repeated cumulative totals replace previous snapshots instead of being summed. Overlapping Health Connect and BLE totals are not added together.
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

Release 1.2.0 /2703 integrates the 2698–2703 stack through #112 into main. Historical candidate APKs/tags stay unchanged. New work in #113 starts from the accepted main and requires a new versionCode, regression gates and phone acceptance.

## Current candidate — 1.2.1 /2704 (#113)

The requested current fix adds per-origin daily HC summaries and explicit activity-source selection (automatic Samsung when step records exist, otherwise HC priority), with raw records in a separate disclosure. Default raw imports still preserve all permitted origins. A separate native connectedDevice BLE service retains the remembered watch connection, uses the observed battery/current-step queries and writes verified step snapshots into the same journal. Diagnostic/service GATT ownership is exclusive. HC background work is separate, capability/permission-gated and scheduled by Android; without permission, foreground sync remains available. Operational opt-ins/MAC are excluded from backups. Force-stop/reboot require reopening; watch-history/HRV and phone confirmation remain open. [Candidate usage and limits](releases/v1.2.1.md).
