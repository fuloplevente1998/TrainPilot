#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

output_dir="android/app/build/outputs/androidTest-results/cold-startup"
mkdir -p "$output_dir"
report="$output_dir/process-cold-startup.txt"
: > "$report"

measure_startup() {
  local label="$1"
  adb shell am force-stop com.repforge.app
  adb logcat -c
  adb shell am start -W -n com.repforge.app/.MainActivity > "$output_dir/${label}-am-start.txt"
  local marker=""
  for _ in $(seq 1 60); do
    marker="$(adb logcat -d -v brief -s TrainPilotStartup:I '*:S' | tail -n 1 || true)"
    if [[ "$marker" == *"activityToJsReadyMs="* ]]; then break; fi
    sleep 0.5
  done
  if [[ "$marker" != *"activityToJsReadyMs="* ]]; then
    adb logcat -d > "$output_dir/${label}-logcat.txt"
    echo "Missing TrainPilotStartup marker for $label" >&2
    return 1
  fi
  printf '%s %s\n' "$label" "$marker" | tee -a "$report"
}

# A fresh install has no app compilation profile. Compare it with the exact
# diagnostic proposed for the first-launch complaint; this is evidence only.
measure_startup install-default
adb shell cmd package compile -m speed -f com.repforge.app | tee "$output_dir/compile-speed.txt"
measure_startup speed-compiled
