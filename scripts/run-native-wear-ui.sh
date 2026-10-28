#!/usr/bin/env bash
set -euo pipefail
mkdir -p wear-ui/results
finish_wear_ui() {
  local test_status=$?
  trap - EXIT
  set +e
  # Capture evidence before disposing the AVD; a dead adb must not block upload.
  timeout --kill-after=2s 10s adb logcat -d > wear-ui/results/logcat.txt 2>/dev/null
  timeout --kill-after=2s 10s adb shell dumpsys window > wear-ui/results/windows.txt 2>/dev/null
  timeout --kill-after=2s 20s adb pull /sdcard/Pictures/TrainPilot-Wear-layout wear-ui/results/ >/dev/null 2>&1
  python3 scripts/native/stop-ci-emulator.py --test-exit-code "$test_status" \
    --report wear-ui/results/emulator-cleanup.json
  exit "$test_status"
}
trap finish_wear_ui EXIT
# The prebuilt-APK lane starts tests immediately after boot. The Wear setup
# wizard otherwise launches EmulatorStatusActivity late and steals window focus.
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell pm disable-user --user 0 com.google.android.wearable.setupwizard
adb shell am force-stop com.google.android.wearable.setupwizard
# The emulator boots as a plugged-in watch. SystemUI's delayed charging overlay
# also steals focus from the first rotary editor, even after disabling setup.
# This is an isolated test device; no product focus/lifecycle guard is bypassed.
# Protected system components cannot be disabled by shell on this image. Model
# an unplugged watch instead, so SystemUI does not open its charging experience.
adb shell dumpsys battery unplug
adb shell settings put global stay_on_while_plugged_in 7
adb shell svc power stayon true
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb install -r wear-ui/app/wear-debug.apk
adb install -r -t wear-ui/test/wear-debug-androidTest.apk
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
# Disabling the wizard can leave its already queued windows/remote animations
# draining during the first tests. Bring the real home window to the front and
# require stable focus before instrumentation, rather than racing those tasks.
# This changes only this disposable emulator, never the product's focus guards.
adb shell settings put system screen_off_timeout 1800000
adb shell settings put secure screensaver_enabled 0
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
stable_focus=0
home_ready=false
for check in $(seq 1 30); do
  current_focus="$(adb shell dumpsys window | tr -d '\r' | sed -n '/mCurrentFocus=/p')"
  if [[ "$current_focus" == *com.google.android.wearable.sysui*SysUiActivity* ]]; then
    stable_focus=$((stable_focus + 1))
    if ((stable_focus >= 3)); then home_ready=true; break; fi
  else
    stable_focus=0
  fi
  sleep 1
done
adb shell dumpsys window > wear-ui/results/windows-before.txt
if [[ "$home_ready" != true ]]; then
  echo 'Wear emulator home never acquired stable focus before instrumentation.' >&2
  exit 1
fi
adb shell dumpsys battery > wear-ui/results/battery-before.txt
adb shell am instrument -w -r -e numShards 3 -e shardIndex "${WEAR_SHARD:?}" com.repforge.app.test/androidx.test.runner.AndroidJUnitRunner | tee wear-ui/results/instrumentation.txt
grep -E 'OK \([1-9][0-9]* tests?\)' wear-ui/results/instrumentation.txt
