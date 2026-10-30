from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

repls = {
    'tests/startup-behavior.cjs': [
        ("assert.equal(run('makeBackup().appVersion'),'1.4.4');", "assert.equal(run('makeBackup().appVersion'),'1.4.6');"),
    ],
    'tests/trainpilot-142.cjs': [
        ("assert.equal(pkg.version,'1.4.5');assert.equal(src.version,'1.4.5');assert.equal(src.versionCode,2629);", "assert.equal(pkg.version,'1.4.6');assert.equal(src.version,'1.4.6');assert.equal(src.versionCode,2630);"),
        ("assert.match(gradle,/versionCode\\s+2629/);assert.match(gradle,/versionName\\s+\"1\\.4\\.5\"/);", "assert.match(gradle,/versionCode\\s+2630/);assert.match(gradle,/versionName\\s+\"1\\.4\\.6\"/);"),
    ],
    'tests/trainpilot-143.cjs': [
        ("assert.equal(pkg.version,'1.4.5');assert.equal(src.version,'1.4.5');assert.equal(src.versionCode,2629);", "assert.equal(pkg.version,'1.4.6');assert.equal(src.version,'1.4.6');assert.equal(src.versionCode,2630);"),
        ("assert.match(gradle,/versionCode\\s+2629/);assert.match(gradle,/versionName\\s+\"1\\.4\\.5\"/);assert.ok(sw.includes('trainpilot-v145'));", "assert.match(gradle,/versionCode\\s+2630/);assert.match(gradle,/versionName\\s+\"1\\.4\\.6\"/);assert.ok(sw.includes('trainpilot-v146'));"),
    ],
    'tests/v142.cjs': [
        ("assert.equal(pkg.version,'1.4.5','canonical product version');", "assert.equal(pkg.version,'1.4.6','canonical product version');"),
        ("assert(vn&&vn[1]==='1.4.5','canonical Android versionName');", "assert(vn&&vn[1]==='1.4.6','canonical Android versionName');"),
    ],
    'tests/v245.cjs': [
        ("assert.equal(run('makeBackup().appVersion'),'1.4.4');", "assert.equal(run('makeBackup().appVersion'),'1.4.6');"),
    ],
    'tests/v250.cjs': [
        ("assert.equal(run('makeBackup().appVersion'),'1.4.4');", "assert.equal(run('makeBackup().appVersion'),'1.4.6');"),
    ],
    'tests/single-source.cjs': [
        ("assert.equal(run('makeBackup().appVersion'),'1.4.5');", "assert.equal(run('makeBackup().appVersion'),'1.4.6');"),
    ],
    'tests/runtime-120.cjs': [
        ("assert.equal(pkg.version,'1.4.5');assert.equal(meta.version,'1.4.5');assert.equal(meta.versionCode,2629);", "assert.equal(pkg.version,'1.4.6');assert.equal(meta.version,'1.4.6');assert.equal(meta.versionCode,2630);"),
        ("assert.match(gradle,/versionCode\\s+2629/);assert.match(gradle,/versionName\\s+\"1\\.4\\.5\"/);assert.match(sw,/trainpilot-v145/);", "assert.match(gradle,/versionCode\\s+2630/);assert.match(gradle,/versionName\\s+\"1\\.4\\.6\"/);assert.match(sw,/trainpilot-v146/);"),
        ("assert.equal(run('makeBackup().appVersion'),'1.4.5');", "assert.equal(run('makeBackup().appVersion'),'1.4.6');"),
    ],
    'tests/v130-photos.cjs': [
        ("assert.ok(gradle.includes('versionCode 2629')&&gradle.includes('versionName \"1.4.5\"'));", "assert.ok(gradle.includes('versionCode 2630')&&gradle.includes('versionName \"1.4.6\"'));"),
    ],
    'tests/trainpilot-144.cjs': [
        ("assert.equal(src.versionCode,2629);assert.match(gradle,/versionCode\\s+2629/);", "assert.equal(src.versionCode,2630);assert.match(gradle,/versionCode\\s+2630/);"),
        ("console.log('PASS TrainPilot 2629 local gallery / MediaStore camera / queue isolation guards');", "console.log('PASS TrainPilot 2630 local gallery / MediaStore camera / queue isolation guards');"),
    ],
}

for rel, pairs in repls.items():
    p = ROOT / rel
    s = p.read_text(encoding='utf-8')
    for old, new in pairs:
        if old not in s:
            raise SystemExit(f'2630 test expectation marker missing in {rel}: {old}')
        s = s.replace(old, new, 1)
    p.write_text(s, encoding='utf-8')

print('Updated 2630 release metadata expectations in compatibility tests')
