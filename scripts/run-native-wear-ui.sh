#!/usr/bin/env bash
set -euo pipefail
mkdir -p wear-ui/results
trap 'adb logcat -d > wear-ui/results/logcat.txt 2>/dev/null || true' EXIT
adb install -r wear-ui/app/wear-debug.apk
adb install -r -t wear-ui/test/wear-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell am instrument -w -r -e numShards 3 -e shardIndex "${WEAR_SHARD:?}" com.repforge.app.test/androidx.test.runner.AndroidJUnitRunner | tee wear-ui/results/instrumentation.txt
grep -E 'OK \([1-9][0-9]* tests?\)' wear-ui/results/instrumentation.txt
