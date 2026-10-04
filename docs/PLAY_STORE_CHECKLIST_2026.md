# TrainPilot — Google Play publication checklist (2026-10)

Current stable app: **1.2.6 / 2709**  
Package: **`com.repforge.app`**  
Repository: **private / proprietary**  
Publisher brand: **FulTech Studios**

This document tracks the work required to move from GitHub-distributed APKs to Google Play. Repository/build readiness and Play Console submission are kept separate so a checked item always means the step was actually completed.

## 1. Android and build readiness

- [x] `applicationId` is stable: `com.repforge.app`.
- [x] `compileSdk` / `targetSdk` are API 36.
- [x] Release signing material is excluded from Git and supplied through GitHub Secrets.
- [x] The normal signed APK release gate verifies package, version, source packaging and signer continuity.
- [x] A separate guarded Play AAB workflow exists: `.github/workflows/build-play-bundle.yml`.
- [x] The Play build requires a public HTTPS privacy URL, support email and developer identity before Gradle can build.
- [x] The Play workflow uses a separate upload-key secret set (`PLAY_UPLOAD_*`) rather than silently reusing the GitHub APK key.
- [ ] Decide and document the final **Play App Signing** strategy in Play Console.
- [ ] Create/confirm the production Play upload key, back it up securely offline, and record its certificate fingerprints outside the repository.
- [ ] Run the Play AAB workflow with the final Play variables/secrets and verify the produced AAB and SHA-256 artifact.
- [ ] Upload the first accepted AAB to an internal/closed testing track before production.

## 2. Release/version discipline

- [x] Stable releases use SemVer `X.Y.Z`.
- [x] Phone-test candidates use prerelease versions such as `1.2.7-rc.1`, not a new stable patch for every test build.
- [x] Android `versionCode` remains separately monotonic.
- [x] CI verifies npm / SOURCE_VERSION / Android version consistency and tag matching.
- [x] Historical releases remain untouched for provenance.
- [ ] The first Play candidate must use a versionCode greater than every previously distributed installable build.

See [RELEASE_POLICY.md](RELEASE_POLICY.md).

## 3. Privacy and user-data requirements

Google Play requires every published app to provide a privacy policy and Data safety declaration. Health-related apps have additional disclosure requirements.

- [x] In-app privacy entry point exists.
- [x] Play builds require a configured privacy-policy URL.
- [x] Local backup/restore and deletion behavior is documented in the app/repository.
- [x] Health data is permission-gated and treated as sensitive data.
- [ ] Publish the privacy policy at an **active, public, non-geofenced HTTPS URL**. Do not use a PDF-only privacy policy.
- [ ] Ensure the public privacy policy names **TrainPilot** and **FulTech Studios**, provides a privacy/support contact, and describes data access, use, sharing, retention and deletion.
- [ ] Complete the Play Console **Data safety** form from the actual production data flows, including third-party SDK behavior.
- [ ] Re-check whether optional Google Drive/Calendar transfers change any Data safety answers.
- [ ] Verify account/data deletion obligations against the final production account model.

Official references:
- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play privacy/user-data policy: https://support.google.com/googleplay/android-developer/answer/18258653

## 4. Health Connect / Health apps policy

TrainPilot falls under **Health and fitness → Activity and Fitness** because it records workouts and reads fitness/health signals.

Current manifest permissions include:
`READ_STEPS`, `READ_HEART_RATE`, `READ_RESTING_HEART_RATE`, `READ_HEART_RATE_VARIABILITY`,
`READ_ACTIVE_CALORIES_BURNED`, `READ_TOTAL_CALORIES_BURNED`, `READ_EXERCISE`,
`READ_SLEEP`, `READ_WEIGHT`, `READ_BODY_FAT`, `READ_OXYGEN_SATURATION`,
`READ_VO2_MAX`, `READ_DISTANCE`, `READ_SPEED`, `READ_BLOOD_PRESSURE`,
`READ_BLOOD_GLUCOSE`, `READ_RESPIRATORY_RATE`, `READ_HEALTH_DATA_IN_BACKGROUND`,
and `WRITE_EXERCISE`.

- [x] Health Connect access is user-permission gated.
- [x] Health data powers user-visible Health/Journal/Coach/training features rather than advertising.
- [x] The app contains a Health permission/privacy activity.
- [x] Background Health reading is separately permission/capability gated.
- [ ] Perform a **minimum-scope audit** immediately before Play submission: every requested Health permission must still back a visible production feature.
- [ ] Remove any Health permission that is no longer needed by the production build.
- [ ] Complete the Play Console **Health apps declaration** and declare **Activity and Fitness**.
- [ ] Map each requested Health permission to the exact user-facing feature in the declaration/review notes.
- [ ] Verify that no Health Connect data is used for advertising, employment/insurance eligibility, unauthorized social sharing or another prohibited secondary use.
- [ ] Repeat physical-device Health permission/read/write/background tests on the exact Play candidate.

Official references:
- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Health apps / Health Connect policy: https://support.google.com/googleplay/android-developer/answer/16679511
- Sensitive/body-sensor permissions: https://support.google.com/googleplay/android-developer/answer/16558241

## 5. Location, Bluetooth and foreground services

- [x] BLE is optional and declared as not required hardware.
- [x] Android 12+ Bluetooth scan uses `neverForLocation`.
- [x] Location hardware is optional.
- [x] GPS tracking is user-started and uses a location foreground service.
- [x] Remembered-watch background connectivity uses a connected-device foreground service.
- [ ] Confirm Play Console foreground-service declarations for **location** and **connectedDevice** against the final release behavior.
- [ ] Re-test notification/foreground-service behavior on the Play candidate, including denied permissions and force-stop/restart cases.
- [ ] Confirm the production privacy policy accurately describes the GPS aggregate-distance behavior and that route coordinates are not retained if that remains the implementation.

## 6. Google OAuth / Drive / Calendar

- [x] Google Drive and Google Calendar integrations are optional.
- [x] Drive uses `appDataFolder` for application backup data.
- [x] OAuth configuration is not committed to the repository.
- [ ] Finalize Google Auth Platform branding for the production identity.
- [ ] Configure the public app homepage and privacy-policy URL.
- [ ] Move the OAuth consent configuration to the intended production/publishing state.
- [ ] Register the final release/Play signing certificate fingerprints where required.
- [ ] Verify requested production OAuth scopes against the final features.
- [ ] Run physical-device Google sign-in, Drive backup/restore/photo and Calendar tests using the Play candidate.

## 7. Store listing

Draft copy is in [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md).

- [x] English store-listing draft prepared.
- [x] Hungarian store-listing draft prepared.
- [ ] Confirm final app name.
- [ ] Confirm support email.
- [ ] Configure public privacy URL.
- [ ] Produce a **512×512 Play icon**.
- [ ] Produce a **1024×500 feature graphic** (JPEG or 24-bit PNG without alpha).
- [ ] Produce at least **2 Play-valid phone screenshots**; target 4–8 high-quality screenshots for the primary listing.
- [ ] Prepare localized screenshots if Hungarian and other localized listings are used.
- [ ] Complete content rating.
- [ ] Complete target-audience declaration.
- [ ] Complete ads declaration.
- [ ] Confirm app category.

Google Play listing limits currently used by this checklist:
- app name: up to 30 characters;
- short description: up to 80 characters;
- full description: up to 4000 characters;
- phone screenshots: JPEG/24-bit PNG, minimum dimension 320 px, maximum dimension 3840 px;
- feature graphic: 1024×500.

Official references:
- Store listing setup: https://support.google.com/googleplay/android-developer/answer/9859152
- Preview assets: https://support.google.com/googleplay/android-developer/answer/9866151

## 8. Physical acceptance and rollout

- [ ] Build the exact Play candidate from the accepted commit.
- [ ] Install/test via Play internal or closed testing rather than only sideloading the APK.
- [ ] Verify upgrade/data retention from an existing TrainPilot installation.
- [ ] Verify Health Connect, Coach, Journal, workout save/edit, backup/restore, Google integrations, camera/photo, BLE and GPS flows.
- [ ] Review crashes/ANRs and pre-launch report results.
- [ ] Only after acceptance, promote the same validated artifact/version to production.

## Current blockers

The repository is technically close to producing a Play AAB, but production publication is **not yet complete**. The remaining external blockers are the Play App Signing/upload-key decision, public privacy/support identity, policy declarations, store assets/listing, OAuth production configuration and an accepted Play-track device test.
