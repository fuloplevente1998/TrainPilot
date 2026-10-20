#!/usr/bin/env bash
set -euo pipefail
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
result=0
test_args=(--no-daemon :wear:connectedDebugAndroidTest)
count="${TRAINPILOT_WEAR_UI_SHARD_COUNT:-1}"
index="${TRAINPILOT_WEAR_UI_SHARD_INDEX:-0}"
if [[ ! "$count" =~ ^[1-9][0-9]*$ || ! "$index" =~ ^(0|[1-9][0-9]*)$ ]] || (( index >= count )); then
  echo "Invalid round UI shard index/count: $index/$count" >&2
  exit 2
fi
if [[ -n "${TRAINPILOT_WEAR_UI_FILTER:-}" ]]; then
  # AndroidJUnitRunner requires the full parameterized method name to filter it.
  if (( count != 1 )); then echo 'Class/method filter cannot be combined with sharding' >&2; exit 2; fi
  test_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$TRAINPILOT_WEAR_UI_FILTER")
elif (( count > 1 )); then
  # AndroidX Test Runner shards by test case, including parameterized cases.
  # Each shard uses its own emulator and writes a separate JUnit XML report.
  test_args+=("-Pandroid.testInstrumentationRunnerArguments.numShards=$count"
              "-Pandroid.testInstrumentationRunnerArguments.shardIndex=$index")
fi
echo "Round UI shard: $index/$count, filter: ${TRAINPILOT_WEAR_UI_FILTER:-<full-suite>}"
(cd android && ./gradlew "${test_args[@]}") || result=$?
# AndroidJUnit may return success when a parameterized method filter selects nothing.
# Treat an empty run as a failure; a green job must prove that tests actually ran.
python3 - <<'XML' || result=1
from pathlib import Path
from xml.etree import ElementTree
reports=list(Path('android/wear/build/outputs/androidTest-results/connected').rglob('TEST-*.xml'))
total=sum(int(ElementTree.parse(p).getroot().get('tests',0)) for p in reports)
assert total>0, 'No round-screen UI tests executed; check the AndroidJUnit filter'
print(f'Round UI executed tests: {total}',flush=True)
XML
mkdir -p wear-ui-evidence
# The emulator runner stops the device after the script: collect evidence while it is alive.
adb pull /sdcard/Pictures/TrainPilot-Wear-layout wear-ui-evidence/ || true
adb logcat -d > wear-ui-evidence/logcat.txt || true
exit "$result"
