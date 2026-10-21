#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# The emulator action starts a separate shell for each script line. Keep the
# test status and diagnostic export in one process, including on test failure.
set +e
(cd android && ./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.repforge.app.PhoneWorkoutWebViewTest)
test_status=$?
set -e
mkdir -p android/app/build/outputs/androidTest-results/cold-navigation
adb pull /sdcard/Android/data/com.repforge.app/files/cold-navigation.json android/app/build/outputs/androidTest-results/cold-navigation/ 2>/dev/null || true
exit "$test_status"
