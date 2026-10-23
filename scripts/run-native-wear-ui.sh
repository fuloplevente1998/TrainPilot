#!/usr/bin/env bash
set -euo pipefail
mkdir -p wear-ui/results
trap 'adb logcat -d > wear-ui/results/logcat.txt 2>/dev/null || true; adb pull /sdcard/Pictures/TrainPilot-Wear-layout wear-ui/results/ >/dev/null 2>&1 || true' EXIT
# The prebuilt-APK lane starts tests immediately after boot. The Wear setup
# wizard otherwise launches EmulatorStatusActivity late and steals window focus.
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell pm disable-user --user 0 com.google.android.wearable.setupwizard
adb shell am force-stop com.google.android.wearable.setupwizard
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r wear-ui/app/wear-debug.apk
adb install -r -t wear-ui/test/wear-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell am instrument -w -r -e numShards 3 -e shardIndex "${WEAR_SHARD:?}" com.repforge.app.test/androidx.test.runner.AndroidJUnitRunner | tee wear-ui/results/instrumentation.txt
grep -E 'OK \([1-9][0-9]* tests?\)' wear-ui/results/instrumentation.txt
