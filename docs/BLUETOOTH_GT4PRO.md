# GT4Pro+ / RDFit Bluetooth investigation — 1.2.0 / 2703

The user accepted release 1.2.0 /2703 for main on 2026-10-04, including the earlier foreground BLE diagnostic/reconnection work. Battery/current zero-step replies were physically confirmed; positive values and history remain separate protocol checks in #113. The current native journal stores opted-in remembered-watch step snapshots and permitted HC records.

## Phone procedure

1. Install the signed 2703 APK over the current app. Keep existing data; no uninstall or watch reset is needed.
2. Open **Settings → Bluetooth watch trial** (Hungarian: **Beállítások → Bluetooth-óra próba**).
3. Enable phone Bluetooth, keep the watch nearby and allow Nearby devices. Android 11 or earlier also requires location permission/services for BLE scanning; the trial does not request GPS fixes.
4. Tap **Scan for watches**. For unnamed devices, compare the locally displayed **MAC address** with RDFit device information, then select the matching GT4Pro+. Each result also has a stable per-scan number, signal strength and a protocol hint when an advertised service is recognized. Signal strength or a protocol hint alone is not device identity. Scan stops after 60 seconds; connection/service discovery times out after 20 seconds.
5. If the watch is missing or connection fails, temporarily disconnect its RDFit data connection and retry. The separate Bluetooth call connection is not proof that a BLE health-data channel is available. Do not unpair/reset the watch as a first troubleshooting step.
6. After scanning ends (or tapping **Stop scan**), **Save diagnostics** also works without connecting and includes up to 120 retained scan results. A successful connection adds the actual GATT services. Select a local destination and attach the resulting `TrainPilot-BLE-*.json` to the investigation. This shows the actual watch services needed to choose the next protocol-specific implementation.
7. If a standard heart-rate service is present, **Start heart-rate reader** enables its standard notification/indication descriptor. A watch with only vendor-specific services still provides a useful diagnostic report. Start the watch's own heart-rate measurement if it does not send samples automatically.

Leaving the trial, putting the app in the background or restarting stops scanning and closes the connection. Returning can reconnect to an explicitly remembered watch. Pulse disappears after ten seconds without a valid sample. Diagnostics remain exportable during the current app session after disconnect.

## 2702 saved-watch phone procedure (#110 phase 1)

1. Scan and connect to the actual GT4Pro+ using the locally shown RDFit address. Results appear immediately; selecting a row stops the remaining scan. The visible one-minute countdown and Stop scan control remain available.
2. Before running the read trial, press **Remember watch** (**Óra megjegyzése**). This requires a successful connection with a supported RDFit or standard Heart Rate channel. The native private preference stores only this watch address, name and verified service type. It is not part of diagnostics, JSON/ZIP or Drive, and is removed by **Forget watch** / **Erase all local data**.
3. Close and reopen the trial, then restart the app and open it again. It should connect to the saved address without another broad scan. Saved-address reconnection checks the required GATT channel before accepting the connection; no vendor data queries start automatically.
4. Transient reconnect failure allows at most three attempts per open/manual retry, with waits between attempts. Disconnect, Forget watch, scanning, navigation and backgrounding stop queued retries. Missing permissions, Bluetooth off and a changed service profile stop automatic retry. Enable Bluetooth/grant permissions and retry explicitly.
5. Check Cancel/Disconnect while connecting, Forget watch during connection, scan stop and timeout, and resumed app behavior. If the watch rotates its address, or the saved address no longer identifies it, scan/select/remember again; the app does not guess from name/RSSI alone.
6. The first phase remains **foreground only**. Closing/backgrounding ends the native connection; reopening the visible trial can reconnect. The RDFit read trial still closes on completion. Native record storage and Health Connect migration are included in 2703. Background health sync and verified watch history continue in [#113](https://github.com/fuloplevente1998/TrainPilot/issues/113), described in [HEALTH_JOURNAL_ROADMAP.md](HEALTH_JOURNAL_ROADMAP.md).

The scan retains at most 120 rows. A selected watch, GT4-like name, vendor service or 0x0201 advertising candidate can replace a lower-priority neighbour even with a weaker signal. Ordinary new devices replace an equally ranked weaker row only with a 4 dBm margin, to reduce churn. Numbers stay stable for retained rows; evictions invalidate the removed selection. Names, RSSI and advertising hints are not identity proof or proof of historical-health compatibility. The 2700 report placed the watch at #21, so the old scan duration/cap was not proven to cause the earlier missed searches.

## 2703 health journal

Health → Health journal has date/source filters and a separate watch-step logging opt-in. Remember the watch in the Bluetooth trial first. Daily cumulative snapshots replace the previous same-device/day value; valid zero is retained, overlapping HC/BLE steps are never added, and a preferred step source can be chosen. The date is the phone observation time. Health Connect refresh on app open is a separate opt-in; no closed-app BLE service or HC background permission is implemented. Full native records can enter a health-consented manual backup, not a Drive snapshot. Bluetooth addresses remain excluded. See [release usage](releases/v1.2.0.md).

## Physical GT4Pro+ result and next read trial

The supplied **1.1.2 / 2700** phone report proves `connected`, GATT status **0**, name **GT4Pro+**, RSSI **−51 dBm** and seven GATT services. It advertises `0x0201`, not the default data-channel UUID, so a missing advertising protocol hint does not mean incompatibility. It exposes both the default RDFit MCU channel `6e40ab01/02/03` and the JieLi `ae00/01/02` channel. This identifies available interfaces, not a unique chipset. Standard Heart Rate Service `0x180D` is absent, so the standard pulse button correctly stays disabled.

**1.1.3 / 2701** adds an explicit **RDFit data trial** after a supported connection. It enables notifications on `6e40ab03`, sends only the two traced read queries below to `6e40ab02`, and closes the connection on completion or after 20 seconds. Battery and today's step count appear only in the current trial view. They are cleared after background/navigation/restart and never added to Health Connect, Coach, history, backups or Drive. Diagnostics retain only bounded query/reply counts and success flags, not actual values or packets. Install, scan/select GT4Pro+, press **RDFit data trial**, compare with the watch, then save diagnostics even if the trial times out. No health-history import is included yet.

The checked-in fixture `tests/fixtures/gt4pro-gatt.json` contains only the supplied service/property table; neighbouring devices and their metadata are excluded. The phone report is evidence of GATT discovery, **not** evidence of proprietary replies.

### Traced transport facts

Static inspection of RDFit 4.1.4 / 436 shows that `BleMcuHelper.send` always routes these MCU queries through the default `6e40ab02` writer, separately from its JieLi/OTA handler. Frames have header `ED`, flags, reserved `00`, an 8-bit payload CRC, a two-byte big-endian payload length, then the command payload. Request flags are `40`. CRC starts at `FF`, processes each payload byte least-significant bit first, and uses reflected polynomial `B8`. This is established by `RDMcuConstants.a` and the transport builder; no encryption or reset is added by this query builder.

| Read operation | Source call | Complete request |
|---|---|---|
| Battery | `getBattery → send(04, 0B)` | `ED 40 00 33 00 02 04 0B` |
| Today's steps | `getRealStep → send(0A, 0B)` | `ED 40 00 E7 00 02 0A 0B` |

`RDMcuAnalysisUtils` dispatches `04/0B` to `NordicBatteryBean`: payload byte 2 is battery percent, byte 3 is state. It dispatches `0A/0B` to `NordicStepBean`: the following three unsigned, big-endian 32-bit fields are steps, calorie tenths and distance. This trial displays only steps; it does not infer a distance unit or import the watch's date. The original decoder accepts only CRC-valid, complete battery/step payload shapes, bounded values and supported flags, buffers notification fragments, and ignores other command types. It has no arbitrary-send API. Unit response vectors are **synthetic derivations**, clearly distinct from the physical service fixture.

History uses separate `0A/01` selectors (steps/sleep/heart/sport: 1/2/3/4), chunk requests and next-sync actions in `RDMcuAnalysisUtils`. Its device-buffer side effects, dates, duplicates and actual firmware responses require verification before enabling import. The user has confirmed battery and a valid zero current-step value from these two queries. Positive steps and all history/energy/distance semantics still need physical checks.

## What the candidate does

- Retains up to 120 nearby BLE devices, with user-selected connection, bounded scan/connection/subscription timeouts and cleanup of cancelled/late callbacks.
- Shows a MAC address only in the current local scan UI for exact comparison with RDFit; it is not logged or exported. Explicit Remember watch stores only the selected address in private native preferences for reconnection. Protocol hints are candidates inferred from advertised services, not verified GT4Pro+ identity.
- Reads GATT service/characteristic identifiers and properties. Discovery sends no vendor commands. The separately started RDFit trial sends only battery/current-step read queries; no firmware updates, pairing resets or setting changes are exposed.
- Optionally reads **Bluetooth SIG Heart Rate Service 0x180D / Measurement 0x2A37**. The pulse reader writes the standard 0x2902 notification/indication enable values on that verified service after the user starts it. The separate RDFit trial enables only its verified notification descriptor before its two read queries. The parser handles unsigned 8/16-bit values, contact flags and optional energy/RR fields; malformed, zero and no-contact readings are rejected.
- Displays live pulse only in the trial. It does not alter Health Connect, historical health records, training readiness, workout history, backups or Drive data.
- Exports a locally verified JSON with service UUIDs, capabilities, manufacturer identifiers/payload lengths, scan numbers/signal strengths and connection status. Bluetooth addresses, opaque scan IDs, raw advertisement/characteristic bytes and actual pulse readings are omitted. No report is sent automatically to a server.

Sleep, steps and historical heart rate are **not implemented** by service discovery. The unified native journal is included in 2703; proprietary historical health import remains a #113 follow-up.

## RDFit APK static investigation

Inspected package `com.rd.tengfei.bdnotification`, versionName **4.1.4**, versionCode **436**. The official manufacturer download page directs international users to Google Play and did not provide a direct APK. The analysis APK was obtained from APKPure for static inspection only; it was not executed, installed or redistributed. Its APK v2 signature verifies, but this alone does not establish equality with the Play-delivered build or the user's installed version.

- APK SHA-256: `cc33b28c131410ba00a04aa794b8cc591251f8b4d43606c0f61ddf50b64b6b1b`.
- Signer certificate SHA-256: `6612b93ef05ea5ad004dd47abc5f31679d4c0d989a40cd01c579afdb2687f3b8`.
- The device-search path in `com.rd.rdbluetooth.search.RDScan` parses advertising service UUIDs with `BleUtils`, then classifies MediaTek, Realtek and JieLi device families. It does not identify every watch from its marketing name alone.
- `RDRealTekBase` defines the default service `6e40ab01-b5a3-f393-e0a9-e50e24dcca9e`, write characteristic `6e40ab02-b5a3-f393-e0a9-e50e24dcca9e`, and notification characteristic `6e40ab03-b5a3-f393-e0a9-e50e24dcca9e`. Its matching UUID list can be overridden, so the defaults are not proof of GT4Pro+ compatibility.
- `RDJieLiBase` uses default service `0000ae00-0000-1000-8000-00805f9b34fb`, write `ae01` and notify `ae02` with the same Bluetooth base suffix. `MtkBase` recognizes advertising markers `0x2222` and `0x4444`. These are separate paths in the same app.
- In `RDNordicSendUtils`, history helpers request steps, sleep, pulse and sport using data selectors **1, 2, 3, 4**, respectively. They route through `watchSyncHistory`, which passes command fields **0x0A / 0x01** plus the selector to the app's transport builder. These fields are **not a complete Bluetooth packet**: framing, fragmentation, acknowledgement, authentication and exact firmware behavior still need verification before use on the watch.
- Sleep/history response models are present, including separate dates, sleep stages, heart-rate parts and sport parts. Their presence in an APK that supports several families does not prove that this exact watch exposes them or that history retrieval leaves the watch buffer unchanged.

The repository contains original diagnostic code and these interoperability facts, not the RDFit APK, decompiled classes or third-party SDKs. Continue by comparing the physical service list with these paths, tracing the matching transport/handshake, and verifying recorded request/response fixtures before implementing history imports with stable IDs and source labels.

Sources: [manufacturer download page](https://abroad.rundefit.com/app.html), [RDFit Google Play listing](https://play.google.com/store/apps/details?id=com.rd.tengfei.bdnotification), [analysis download endpoint](https://d.apkpure.com/b/APK/com.rd.tengfei.bdnotification?version=latest). Research working files stay outside the repository.

## Release checks

Required: full Node and Chromium regressions, Android/JVM tests, signed APK verification, exact source/bundled asset comparison and unchanged performance budgets. Physical GT4Pro+ connection and service discovery were verified by the supplied report. Battery and a valid zero current-step reply were confirmed by the user on 2701; positive values, new reconnection behavior and history import need further phone trials. Merge requires phone approval under [the performance policy](PERFORMANCE_REGRESSION_POLICY.md).
