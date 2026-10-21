"""Recover bounded-size diagnostic records from Gradle's per-test logcat."""
import json
from pathlib import Path

root = Path('android/app/build/outputs/androidTest-results')
report = None
launches = {}
for path in root.rglob('logcat-*-coldNavigationProfilesRealTouchesAcrossRelaunches.txt'):
    for line in path.read_text().splitlines():
        if 'I System.out: COLD_NAVIGATION_' not in line:
            continue
        kind, payload = line.split('I System.out: COLD_NAVIGATION_', 1)[1].split(' ', 1)
        data = json.loads(payload)
        if kind == 'META':
            report = data
        elif kind == 'LAUNCH':
            launches[data['launch']] = {**data, 'rows': [], 'longTasks': []}
        elif kind in ('ROW', 'TASK'):
            launches[data['launch']]['rows' if kind == 'ROW' else 'longTasks'].append(data['row' if kind == 'ROW' else 'task'])
assert report and len(launches) == 2, 'Missing complete cold navigation report'
for launch in launches.values():
    assert len(launch['rows']) == 16 and len(launch['catalog']) == 4, 'Incomplete diagnostic records'
report['launches'] = [launches[key] for key in sorted(launches)]
output = root / 'cold-navigation' / 'cold-navigation.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2) + '\n')
print('Collected cold navigation: 2 launches, 32 route samples, 8 catalog samples')
