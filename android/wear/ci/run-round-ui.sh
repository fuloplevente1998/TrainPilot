#!/usr/bin/env bash
set -euo pipefail
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
result=0
test_args=(--no-daemon :wear:connectedDebugAndroidTest)
if [[ -n "${TRAINPILOT_WEAR_UI_FILTER:-}" ]]; then
  test_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$TRAINPILOT_WEAR_UI_FILTER")
fi
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
