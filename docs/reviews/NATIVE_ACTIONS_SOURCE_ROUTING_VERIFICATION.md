# Native Actions source-routing verification

## Scope

This branch exists only to validate the native-versus-legacy GitHub Actions routing now present on `feat/native-android-migration`.

Baseline: `a075a21956ea4bd2f1089a9bc106dbe7a598c264`.

The previous failures were setup failures, not application-test failures:

- Quick TrainPilot Validation run `40389899406`
- Phone Release Gate + TrainPilot APK run `40389895449`

Both stopped in `actions/setup-node` because the native Kotlin/Compose source intentionally has no root `package-lock.json`.

## Expected result

For this native-only source:

- source classification succeeds as native;
- legacy npm/Chromium/Capacitor jobs are skipped rather than entering `actions/setup-node`;
- native Android gates remain the applicable validation path;
- no application source, version, signing material, or G10 implementation is changed here.

Final run IDs and conclusions will be recorded after the GitHub Actions verification.
