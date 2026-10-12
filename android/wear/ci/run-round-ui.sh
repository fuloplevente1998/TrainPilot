#!/usr/bin/env bash
set -euo pipefail
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
result=0
(cd android && ./gradlew --no-daemon :wear:connectedDebugAndroidTest) || result=$?
mkdir -p wear-ui-evidence
# The emulator runner stops the device after the script: collect evidence while it is alive.
adb pull /sdcard/Android/data/com.repforge.app/files/wear-ui wear-ui-evidence/ || true
adb logcat -d > wear-ui-evidence/logcat.txt || true
exit "$result"
