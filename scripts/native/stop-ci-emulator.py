#!/usr/bin/env python3
"""Bounded disposal of this Actions emulator, after its test evidence is captured."""
import argparse
from dataclasses import dataclass
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time


@dataclass(frozen=True)
class Process:
    pid: int
    ppid: int
    start: str
    executable: str
    argv: tuple
    uid: int
    state: str = "S"


def read_process(pid, proc_root=Path('/proc')):
    try:
        folder = proc_root / str(pid)
        tail = (folder / 'stat').read_text().rsplit(')', 1)[1].split()
        return Process(int(pid), int(tail[1]), tail[19], os.readlink(folder / 'exe'),
                       tuple(x.decode(errors='replace') for x in
                             (folder / 'cmdline').read_bytes().split(b'\0') if x),
                       folder.stat().st_uid, tail[0])
    except (OSError, ValueError, IndexError):
        return None


def process_snapshot():
    return [p for folder in Path('/proc').iterdir() if folder.name.isdigit()
            if (p := read_process(folder.name)) is not None]


def option(argv, name):
    try:
        return argv[argv.index(name) + 1]
    except (ValueError, IndexError):
        return None


def scoped_processes(processes, sdk, port, uid):
    """Match SDK emulator roots by explicit AVD+port and their known SDK helpers."""
    sdk_emulator = Path(sdk).resolve() / 'emulator'

    def trusted(p):
        return p.uid == uid and Path(p.executable).is_relative_to(sdk_emulator)

    roots = [p for p in processes if trusted(p)
             and (Path(p.executable).name == 'emulator' or Path(p.executable).name.startswith('qemu-system-'))
             and option(p.argv, '-port') == str(port) and option(p.argv, '-avd')]
    chosen = {p.pid: p for p in roots}
    while True:
        added = [p for p in processes if trusted(p) and p.ppid in chosen and p.pid not in chosen
                 and Path(p.executable).name in ('netsimd', 'netsim', 'netsim-daemon', 'crashpad_handler')]
        if not added:
            break
        chosen.update((p.pid, p) for p in added)

    # Crashpad is sometimes already reparented to PID 1 before our test trap
    # snapshots /proc. Its inherited emulator stdio can keep the upstream
    # runner action open even after the emulator exits. Scope this fallback to
    # a single running SDK AVD, same owner/executable tree, and a helper born
    # no earlier than that exact emulator. Never adopt older orphan helpers
    # or any when another SDK AVD is present.
    all_roots = [p for p in processes if trusted(p)
                 and (Path(p.executable).name == 'emulator' or
                      Path(p.executable).name.startswith('qemu-system-'))
                 and option(p.argv, '-avd')]
    if len(roots) == len(all_roots) == 1:
        born = int(roots[0].start)
        for p in processes:
            if (trusted(p) and p.pid not in chosen and p.ppid == 1
                    and p.state != 'Z'
                    and Path(p.executable).name == 'crashpad_handler'
                    and int(p.start) >= born):
                chosen[p.pid] = p
    return list(chosen.values())


def remaining(processes):
    return [p for p in processes if (now := read_process(p.pid)) is not None
            and now.start == p.start and now.executable == p.executable and now.state != 'Z']


def wait_until_stopped(processes, seconds):
    deadline = time.monotonic() + seconds
    live = remaining(processes)
    while live and time.monotonic() < deadline:
        time.sleep(min(.1, max(0, deadline - time.monotonic())))
        live = remaining(live)
    return live


def send_signal(processes, sig):
    for p in remaining(processes):
        try:
            os.kill(p.pid, sig)
        except ProcessLookupError:
            pass


def cleanup(env=os.environ):
    port = env.get('EMULATOR_PORT', '')
    serial = env.get('ANDROID_SERIAL', '')
    sdk = env.get('ANDROID_HOME', '')
    if (env.get('GITHUB_ACTIONS') != 'true' or not port.isdigit()
            or serial != f'emulator-{port}' or not sdk or not Path(sdk).is_absolute()):
        raise RuntimeError('Refusing cleanup outside this Actions emulator/SDK identity')
    snapshot = process_snapshot()
    processes = scoped_processes(snapshot, sdk, port, os.getuid())
    result = {'serial': serial, 'matchedPids': [p.pid for p in processes]}
    if not processes:
        # A missing match is not proof of shutdown. Record only safe identifiers,
        # so a changed upstream command form can be diagnosed without full argv.
        result['result'] = 'no-scoped-process'
        result['sdkCandidates'] = [
            {'pid': p.pid, 'ppid': p.ppid, 'executable': Path(p.executable).name,
             'port': option(p.argv, '-port'), 'ports': option(p.argv, '-ports'),
             'avd': option(p.argv, '-avd')}
            for p in snapshot if p.uid == os.getuid()
            and Path(p.executable).is_relative_to(Path(sdk).resolve() / 'emulator')
        ]
        return result
    try:
        command = subprocess.run([str(Path(sdk) / 'platform-tools' / 'adb'), '-s', serial, 'emu', 'kill'],
                                 capture_output=True, timeout=8, check=False)
        result['adbExitCode'] = command.returncode
    except subprocess.TimeoutExpired:
        result['adbTimedOut'] = True
    live = wait_until_stopped(processes, 2)
    if live:
        result['termPids'] = [p.pid for p in live]
        send_signal(live, signal.SIGTERM)
        live = wait_until_stopped(live, 3)
    if live:
        result['killPids'] = [p.pid for p in live]
        send_signal(live, signal.SIGKILL)
        live = wait_until_stopped(live, 2)
    result['remainingPids'] = [p.pid for p in live]
    result['result'] = 'stopped' if not live else 'cleanup-incomplete'
    return result


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--test-exit-code', type=int, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        result = cleanup()
    except Exception as error:
        result = {'result': 'cleanup-error', 'errorType': type(error).__name__}
    result['testExitCode'] = args.test_exit_code
    try:
        args.report.write_text(json.dumps(result, indent=2) + '\n')
    except OSError as error:
        print(f'Emulator cleanup report could not be written: {type(error).__name__}', file=sys.stderr)
    print('CI emulator cleanup: ' + json.dumps(result), flush=True)
    # Teardown can neither turn a failed test green nor replace its exact exit status.
    return args.test_exit_code


if __name__ == '__main__':
    sys.exit(main())
