#!/usr/bin/env bash
set -euo pipefail
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
result=0
test_args=(--no-daemon :wear:connectedDebugAndroidTest)
if [[ -n "${TRAINPILOT_WEAR_UI_FILTER:-}" ]]; then
  [[ "$TRAINPILOT_WEAR_UI_FILTER" =~ ^[A-Za-z0-9_.#]+$ ]] || { echo "Invalid AndroidJUnit UI filter" >&2; exit 2; }
  test_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$TRAINPILOT_WEAR_UI_FILTER")
fi
(cd android && ./gradlew "${test_args[@]}") || result=$?
mkdir -p wear-ui-evidence
# The emulator runner stops the device after the script: collect evidence while it is alive.
adb pull /sdcard/Pictures/TrainPilot-Wear-layout wear-ui-evidence/ || true
adb logcat -d > wear-ui-evidence/logcat.txt || true
exit "$result"
