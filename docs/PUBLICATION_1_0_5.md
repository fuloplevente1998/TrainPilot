# TrainPilot 1.0.5 publication candidate

Base: published v1.0.4 / `1d84c54dbcb46f3c6aaf7a94435dafd85586a91a`. Version code: 2685. This is a test candidate; main and v1.0.4 must remain unchanged until phone acceptance.

## Implemented changes

- Drive applies programs, active program, planner preferences, favorites, theme and language as well as workout/weight/calendar data. The #18 legacy recovery and deletion safeguards remain active.
- Health export is off by default. Local Health data survives a redacted Drive merge. An explicit consent switch enables workout health summaries and recovery history in JSON/ZIP/Drive. The full daily ledger and latest wellness summary are included only in explicit manual backups. Revoking this choice does not erase old exports/snapshots.
- Manual restore now awaits one confirmation and writes the dataset in one rollback-protected operation. It restores optional metadata and replaces even an empty recovery history. A cancelled or failed restore must leave the old dataset intact.
- Native ZIP backup includes referenced photo files, SHA-256 manifest and backup JSON. It fails if any live photo is missing. Import validates names, exact inventory, checksums, expanded sizes (20 MB JSON / 6 MB photo / 500 MB archive), and entry count before installing photos. Original photos and local data have rollback journals; an interrupted restore is recovered on the next app start. JSON remains available, with photo references only. ZIP restore clears old Drive file IDs so photos can be uploaded into the connected account.
- Added separate controls for disconnect, Google permission revocation, Drive-data erasure, managed-calendar erasure and complete local-data erasure. Sync is disabled before an erasure. Remote failures are reported; they are not displayed as successful deletion. Other devices and previously exported files require separate action.
- Calendar preparation checks the cached calendar. Missing calendars are rediscovered/recreated, and a changed identity invalidates the delta cache. A calendar changing during a batch causes a retryable manual failure rather than partial-success reporting.
- Idempotent Google requests retry 429/selected 5xx up to three times with exponential delays and Retry-After handling. Non-idempotent Drive uploads/calendar creation are not repeated blindly. Long server cooldowns end the attempt instead of retrying early.
- After a verified new snapshot, retention preserves the latest ten snapshots and oldest snapshot of this device. Other devices and legacy snapshots with missing/synthetic IDs are protected. Cleanup failure does not invalidate the verified sync; its result remains in the local diagnostic record. Protected legacy snapshots can therefore exceed this limit.
- Android automatic backup and device transfer exclude app data. Explicit file backups and Drive sync remain available.
- Bundled HU/EN/DE/RO privacy policy describes persistent storage, optional exports and deletion limits. Native Health availability messages follow the chosen app language.
- Separate, manually triggered Play AAB workflow uses separate upload-key secrets. Gradle rejects a debug certificate and missing publisher/contact/privacy configuration for Play builds. No upload to Play or new public Release is performed by this workflow.

## Signing and Google Console work that code cannot complete

The existing GitHub APK is signed by the Android Debug certificate (SHA-1 `85:DD:CE:73:93:8C:7A:39:2A:54:B6:D7:E2:A3:67:46:20:AD:61:5E`). Keep this key for upgrade-safe phone test APKs; do not overwrite the current APK signing secrets.

For Play, configure `PLAY_UPLOAD_KEYSTORE_BASE64`, `PLAY_UPLOAD_STORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`; repository variables `TRAINPILOT_PRIVACY_POLICY_URL`, `TRAINPILOT_SUPPORT_EMAIL`, `TRAINPILOT_DEVELOPER_NAME`. Use a real HTTPS policy URL, support address and publisher identity. The bundled policy is a technical draft and needs these public details before publication.

Enable Drive and Calendar APIs in the correct Google Cloud project. Create Android OAuth clients for `com.repforge.app` with each actual installed signing certificate: the legacy phone APK certificate and the **Play App Signing certificate** (the upload certificate alone is insufficient for Play-installed apps). Configure an External audience, required public branding/contact information and the required scopes, then complete verification/publication as required by Google. A Google Play developer account does not automatically configure this Cloud OAuth project.

Validate on two real accounts outside the developer/test-user list, using a Play internal-testing installation. Validate the separate Drive and Calendar consent flows, revoked permission, offline state, account switch and reconnect. Code tests use mocked Google APIs; they cannot certify these Console settings.

## Required phone checks before merge

1. Upgrade v1.0.4 in place; existing workouts, programs, drafts and photos remain available.
2. JSON export with Health off excludes health summaries; enabling the option makes them appear.
3. ZIP export → restore on another test installation, with before/after photos. Test picker cancellation, a missing photo and a corrupt ZIP.
4. Drive restore of a custom program, planner preferences, favorites and language; preserve local Health data when Health export is off.
5. Delete the managed Google calendar, then sync; all current events must return once.
6. Test disconnect vs revoke. Only use erasure controls on a disposable test account/dataset; verify other calendars and Drive files are untouched.
7. Interrupt ZIP restoration and restart; validate data and photo rollback. Validate low-storage behavior on a test device.
8. Inspect the new privacy controls in all supported languages at small screen widths.

Optional feature proposals (background rest timer, CSV/PDF exports, supersets, warm-up sets, undo, unit conversion, Coach explanations) remain separate feature work. They are not represented as fixes completed by this candidate.
