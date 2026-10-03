# GT4Pro+ / RDFit Bluetooth investigation — 1.1.1 / 2699

This candidate adds a foreground Android BLE diagnostic client. The 1.1.0 / 2698 features remain included. Stable main remains 1.0.9 / 2697; neither candidate has physical-phone approval yet.

## Phone procedure

1. Install the signed 2699 APK over the current app. Keep existing data; no uninstall or watch reset is needed.
2. Open **Settings → Bluetooth watch trial** (Hungarian: **Beállítások → Bluetooth-óra próba**).
3. Enable phone Bluetooth, keep the watch nearby and allow Nearby devices. Android 11 or earlier also requires location permission/services for BLE scanning; the trial does not request GPS fixes.
4. Tap **Scan for watches**, then select the GT4Pro+ from the results. Scan stops after 12 seconds; connection/service discovery times out after 20 seconds.
5. If the watch is missing or connection fails, temporarily disconnect its RDFit data connection and retry. The separate Bluetooth call connection is not proof that a BLE health-data channel is available. Do not unpair/reset the watch as a first troubleshooting step.
6. Tap **Save diagnostics**, select a local destination and attach the resulting `TrainPilot-BLE-*.json` to the investigation. This shows the actual watch services needed to choose the next protocol-specific implementation.
7. If a standard heart-rate service is present, **Start heart-rate reader** enables its standard notification/indication descriptor. A watch with only vendor-specific services still provides a useful diagnostic report. Start the watch's own heart-rate measurement if it does not send samples automatically.

Leaving the trial, putting the app in the background or restarting stops scanning and closes the connection. Returning requires a new scan. Pulse disappears after ten seconds without a valid sample. Diagnostics remain exportable during the current app session after disconnect.

## What the candidate does

- Discovers up to 40 nearby BLE devices, with user-selected connection, bounded scan/connection/subscription timeouts and cleanup of cancelled/late callbacks.
- Reads GATT service/characteristic identifiers and properties. Discovery sends no vendor commands, firmware updates, pairing resets or setting changes.
- Optionally reads **Bluetooth SIG Heart Rate Service 0x180D / Measurement 0x2A37**. The only descriptor writes are the standard 0x2902 notification/indication enable values on that verified service after the user starts the reader. The parser handles unsigned 8/16-bit values, contact flags and optional energy/RR fields; malformed, zero and no-contact readings are rejected.
- Displays live pulse only in the trial. It does not alter Health Connect, historical health records, training readiness, workout history, backups or Drive data.
- Exports a locally verified JSON with service UUIDs, capabilities, manufacturer identifiers/payload lengths and connection status. Bluetooth addresses, opaque scan IDs, raw advertisement/characteristic bytes and actual pulse readings are omitted. No report is sent automatically to a server.

Sleep, steps and historical heart rate are **not implemented** by service discovery. The physical watch report is the next dependency.

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

Required: full Node and Chromium regressions, Android/JVM tests, signed APK verification, exact source/bundled asset comparison and unchanged performance budgets. Physical GT4Pro+ connection remains unverified until the phone trial. Merge requires phone approval under [the performance policy](PERFORMANCE_REGRESSION_POLICY.md).
