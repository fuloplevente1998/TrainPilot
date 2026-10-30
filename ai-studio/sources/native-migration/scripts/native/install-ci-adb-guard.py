#!/usr/bin/env python3
"""Bound the upstream action's otherwise unbounded emulator shutdown command.

Install only on a disposable Actions runner, after SDK installation (the action's
pre-emulator-launch hook). All non-shutdown adb commands run unchanged.
"""
import os
from pathlib import Path


WRAPPER = '''#!/usr/bin/env bash
# TrainPilot bounded CI adb shutdown
set -euo pipefail
real="$(dirname -- "$0")/adb.trainpilot-real"
if [[ "$#" -eq 4 && "$1" == -s && "$2" =~ ^emulator-[0-9]+$ && "$3" == emu && "$4" == kill ]]; then
  exec timeout --kill-after=2s 10s "$real" "$@"
fi
exec "$real" "$@"
'''


def install(env=os.environ):
    sdk = env.get('ANDROID_HOME', '')
    if env.get('GITHUB_ACTIONS') != 'true' or not sdk or not Path(sdk).is_absolute():
        raise RuntimeError('Requires a disposable Actions runner and absolute SDK path')
    adb = Path(sdk) / 'platform-tools' / 'adb'
    original = adb.with_name('adb.trainpilot-real')
    if original.exists():
        if adb.read_text() != WRAPPER:
            raise RuntimeError('Unexpected adb guard or SDK replacement; refusing overwrite')
        return
    if not adb.is_file() or adb.is_symlink() or not os.access(adb, os.X_OK):
        raise RuntimeError('Requires the installed SDK adb executable')
    adb.rename(original)
    try:
        adb.write_text(WRAPPER)
        adb.chmod(0o755)
    except BaseException:
        adb.unlink(missing_ok=True)
        original.rename(adb)
        raise


if __name__ == '__main__':
    install()
    print('CI adb guard installed: emulator shutdown deadline 10s + 2s kill grace', flush=True)
