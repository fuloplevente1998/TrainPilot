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
# Android/Gradle can uninstall the target before adb pull; recover the same
# structured records from the per-test logcat artifact in that case.
python3 scripts/collect-phone-cold-navigation.py || { if [ "$test_status" -eq 0 ]; then test_status=1; fi; }
exit "$test_status"
