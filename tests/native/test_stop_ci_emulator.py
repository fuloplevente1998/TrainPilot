"""Offline regressions for scoped, bounded Actions emulator disposal."""
import importlib.util
import json
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[2] / 'scripts/native/stop-ci-emulator.py'
spec = importlib.util.spec_from_file_location('stop_ci_emulator', SCRIPT)
stop = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = stop
spec.loader.exec_module(stop)


class ScopedEmulatorCleanupTest(unittest.TestCase):
    env = {'GITHUB_ACTIONS': 'true', 'EMULATOR_PORT': '5554',
           'ANDROID_SERIAL': 'emulator-5554', 'ANDROID_HOME': '/sdk'}

    def process(self, pid=10, parent=1, port='5554', exe='/sdk/emulator/qemu/linux-x86_64/qemu-system-i386', uid=1000):
        return stop.Process(pid, parent, '123', exe,
                            (exe, '-avd', 'test', '-port', port), uid)

    def test_only_sdk_instance_and_its_netsim_children_match(self):
        root = self.process()
        netsim = self.process(11, 10, exe='/sdk/emulator/netsimd')
        other_avd = self.process(20, port='5556')
        other_netsim = self.process(21, 20, exe='/sdk/emulator/netsimd')
        foreign = self.process(30, exe='/usr/bin/qemu-system-i386')
        other_user = self.process(40, uid=2000)
        non_emulator_child = self.process(12, 10, exe='/sdk/emulator/some-other-tool')
        items = [root, netsim, other_avd, other_netsim, foreign, other_user, non_emulator_child]
        self.assertEqual([10, 11], [p.pid for p in stop.scoped_processes(items, '/sdk', '5554', 1000)])

    def test_reparented_crashpad_is_scoped_only_to_one_recent_sdk_emulator(self):
        root = self.process()
        old = stop.Process(11, 1, '122', '/sdk/emulator/lib64/crashpad_handler',
                           ('/sdk/emulator/lib64/crashpad_handler',), 1000)
        orphan = stop.Process(12, 1, '124', '/sdk/emulator/lib64/crashpad_handler',
                              ('/sdk/emulator/lib64/crashpad_handler',), 1000)
        foreign = stop.Process(13, 1, '124', '/usr/bin/crashpad_handler',
                               ('/usr/bin/crashpad_handler',), 1000)
        other_user = stop.Process(14, 1, '124', '/sdk/emulator/lib64/crashpad_handler',
                                  ('/sdk/emulator/lib64/crashpad_handler',), 2000)
        current = [root, old, orphan, foreign, other_user]
        self.assertEqual([10, 12], [p.pid for p in stop.scoped_processes(current, '/sdk', '5554', 1000)])
        # The same SDK may host another AVD. In that case no parentless
        # crashpad is attributable to our exact emulator, so touch none.
        second_root = self.process(20, port='5556')
        self.assertEqual([10], [p.pid for p in stop.scoped_processes(
            current + [second_root], '/sdk', '5554', 1000)])
        self.assertEqual([], stop.scoped_processes(
            [old, orphan, foreign, other_user], '/sdk', '5554', 1000))

    def test_orphan_crashpad_is_terminated_when_emulator_exits(self):
        root = self.process()
        orphan = stop.Process(12, 1, '124', '/sdk/emulator/lib64/crashpad_handler',
                              ('/sdk/emulator/lib64/crashpad_handler',), 1000)
        with patch.object(stop.os, 'getuid', return_value=1000), \
             patch.object(stop, 'process_snapshot', return_value=[root, orphan]), \
             patch.object(stop.subprocess, 'run', return_value=type('Result', (), {'returncode': 0})()), \
             patch.object(stop, 'wait_until_stopped', side_effect=[[orphan], []]), \
             patch.object(stop, 'send_signal') as send:
            result = stop.cleanup(self.env)
        self.assertEqual('stopped', result['result'])
        self.assertEqual([10, 12], result['matchedPids'])
        self.assertEqual([12], result['termPids'])
        send.assert_called_once_with([orphan], signal.SIGTERM)

    def test_real_device_or_missing_ci_identity_cannot_mutate_processes(self):
        for change in ({'GITHUB_ACTIONS': 'false'}, {'ANDROID_SERIAL': 'physical-device'},
                       {'ANDROID_HOME': ''}, {'EMULATOR_PORT': '5554;kill'}):
            with self.subTest(change=change), patch.object(stop, 'process_snapshot') as snapshot:
                with self.assertRaises(RuntimeError):
                    stop.cleanup(dict(self.env, **change))
                snapshot.assert_not_called()

    def test_crashpad_is_scoped_before_emulator_shutdown_and_reaped_after_reparenting(self):
        root = self.process()
        crashpad = self.process(13, 10, exe='/sdk/emulator/lib64/crashpad_handler')
        foreign = self.process(14, 20, exe='/sdk/emulator/lib64/crashpad_handler')
        self.assertEqual([10, 13], [p.pid for p in stop.scoped_processes(
            [root, crashpad, foreign], '/sdk', '5554', 1000)])
        with patch.object(stop.os, 'getuid', return_value=1000), \
             patch.object(stop, 'process_snapshot', return_value=[root, crashpad, foreign]), \
             patch.object(stop.subprocess, 'run', return_value=type('Result', (), {'returncode': 0})()), \
             patch.object(stop, 'wait_until_stopped', side_effect=[[crashpad], []]), \
             patch.object(stop, 'send_signal') as send:
            result = stop.cleanup(self.env)
        self.assertEqual('stopped', result['result'])
        self.assertEqual([13], result['termPids'])
        send.assert_called_once_with([crashpad], signal.SIGTERM)

    def test_adb_timeout_uses_independent_term_and_kill_deadlines(self):
        root = self.process()
        with patch.object(stop.os, 'getuid', return_value=1000), \
             patch.object(stop, 'process_snapshot', return_value=[root]), \
             patch.object(stop.subprocess, 'run', side_effect=subprocess.TimeoutExpired('adb', 8)) as adb, \
             patch.object(stop, 'wait_until_stopped', side_effect=[[root], [root], []]) as wait, \
             patch.object(stop, 'send_signal') as send:
            result = stop.cleanup(self.env)
        self.assertTrue(result['adbTimedOut'])
        self.assertEqual('stopped', result['result'])
        self.assertEqual(8, adb.call_args.kwargs['timeout'])
        self.assertEqual([2, 3, 2], [c.args[1] for c in wait.call_args_list])
        self.assertEqual([signal.SIGTERM, signal.SIGKILL], [c.args[1] for c in send.call_args_list])

    def test_no_match_is_not_reported_as_proven_shutdown(self):
        candidate = self.process(port='5556')
        with patch.object(stop.os, 'getuid', return_value=1000), \
             patch.object(stop, 'process_snapshot', return_value=[candidate]), \
             patch.object(stop.subprocess, 'run') as adb:
            result = stop.cleanup(self.env)
        self.assertEqual('no-scoped-process', result['result'])
        self.assertEqual('5556', result['sdkCandidates'][0]['port'])
        self.assertNotIn('argv', result['sdkCandidates'][0])
        adb.assert_not_called()

    def test_reused_pid_and_zombie_are_not_signalled(self):
        original = self.process()
        reused = stop.Process(original.pid, 1, '456', original.executable, original.argv, 1000)
        zombie = stop.Process(original.pid, 1, original.start, original.executable, original.argv, 1000, 'Z')
        for current in (None, reused, zombie):
            with self.subTest(current=current), patch.object(stop, 'read_process', return_value=current), \
                 patch.object(stop.os, 'kill') as kill:
                stop.send_signal([original], signal.SIGKILL)
                kill.assert_not_called()

    def test_cleanup_error_preserves_original_test_status_and_report(self):
        with tempfile.TemporaryDirectory() as folder:
            report = Path(folder) / 'cleanup.json'
            for code in (0, 17):
                with self.subTest(code=code), patch.object(stop, 'cleanup', side_effect=RuntimeError('not scoped')):
                    self.assertEqual(code, stop.main(['--test-exit-code', str(code), '--report', str(report)]))
                self.assertEqual(code, json.loads(report.read_text())['testExitCode'])
                self.assertEqual('cleanup-error', json.loads(report.read_text())['result'])

    def test_shell_exit_trap_captures_evidence_then_preserves_failed_test(self):
        source = (SCRIPT.parents[1] / 'run-native-wear-ui.sh').read_text()
        trap_source = source[source.index('finish_wear_ui()'):source.index('# The prebuilt-APK lane')]
        with tempfile.TemporaryDirectory() as folder:
            test_root = Path(folder)
            (test_root / 'wear-ui/results').mkdir(parents=True)
            harness = '''set -euo pipefail
 timeout() { echo "capture:$*" >> order; return 0; }
 python3() { echo "cleanup:$*" >> order; return 0; }
''' + trap_source + '\nexit 17\n'
            result = subprocess.run(['bash', '-c', harness], cwd=folder, capture_output=True)
            self.assertEqual(17, result.returncode)
            order = (test_root / 'order').read_text().splitlines()
            self.assertEqual(4, len(order))
            self.assertTrue(all(line.startswith('capture:') for line in order[:3]))
            self.assertIn('--test-exit-code 17', order[3])


if __name__ == '__main__':
    unittest.main()
