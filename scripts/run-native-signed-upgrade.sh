#!/usr/bin/env bash
set -euo pipefail
mkdir -p upgrade/results
trap 'adb logcat -d > upgrade/results/logcat.txt 2>/dev/null || true' EXIT
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install upgrade/original/TrainPilot-1.3.71.apk
adb install -t upgrade/seed/migration-seed-release.apk
adb shell pm grant com.repforge.app android.permission.POST_NOTIFICATIONS
adb shell am instrument -w -r com.repforge.app.migrationseed/com.repforge.migrationseed.LegacySeedInstrumentation | tee upgrade/results/seed.txt
grep -F 'INSTRUMENTATION_RESULT: seedStatus=ok' upgrade/results/seed.txt
adb shell am force-stop com.repforge.app
# This command must succeed as an UPDATE, with the old data still present.
adb install -r upgrade/phone/app-release.apk
adb install -t upgrade/test/app-release-androidTest.apk
adb shell am instrument -w -r -e class com.repforge.app.LegacyUpgradeVerificationTest com.repforge.app.test/androidx.test.runner.AndroidJUnitRunner | tee upgrade/results/verify.txt
grep -F 'OK (1 test)' upgrade/results/verify.txt
adb shell dumpsys package com.repforge.app | grep -E 'versionCode=|versionName=' > upgrade/results/installed-version.txt
