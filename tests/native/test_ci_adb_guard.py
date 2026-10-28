import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[2] / 'scripts/native/install-ci-adb-guard.py'
spec = importlib.util.spec_from_file_location('adb_guard', SCRIPT)
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class AdbGuardTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.sdk = Path(self.folder.name)
        self.adb = self.sdk / 'platform-tools/adb'
        self.adb.parent.mkdir()
        self.adb.write_text('#!/bin/sh\nprintf "%s\\n" "$@"\nexit 7\n')
        self.adb.chmod(0o755)
        self.env = dict(os.environ, GITHUB_ACTIONS='true', ANDROID_HOME=str(self.sdk))

    def test_non_ci_and_relative_sdk_refused_without_touching_adb(self):
        original = self.adb.read_bytes()
        for change in ({'GITHUB_ACTIONS': 'false'}, {'ANDROID_HOME': ''}, {'ANDROID_HOME': 'relative'}):
            with self.subTest(change=change), self.assertRaises(RuntimeError):
                guard.install(dict(self.env, **change))
        self.assertEqual(original, self.adb.read_bytes())

    def test_idempotent_install_and_unmodified_forwarding_of_regular_commands(self):
        original = self.adb.read_bytes()
        guard.install(self.env)
        guard.install(self.env)
        self.assertEqual(original, self.adb.with_name('adb.trainpilot-real').read_bytes())
        args = ['-s', 'physical device', 'shell', 'echo', 'value with spaces']
        result = subprocess.run([str(self.adb), *args], capture_output=True, text=True)
        self.assertEqual(7, result.returncode)
        self.assertEqual(args, result.stdout.splitlines())

    def test_only_exact_emulator_kill_uses_timeout_and_keeps_status(self):
        guard.install(self.env)
        timeout = self.sdk / 'timeout'
        timeout.write_text('#!/bin/sh\nprintf "timeout:%s\\n" "$@"\nexit 124\n')
        timeout.chmod(0o755)
        env = dict(self.env, PATH=str(self.sdk) + os.pathsep + os.environ['PATH'])
        result = subprocess.run([str(self.adb), '-s', 'emulator-5554', 'emu', 'kill'],
                                env=env, capture_output=True, text=True)
        self.assertEqual(124, result.returncode)
        self.assertEqual(['timeout:--kill-after=2s', 'timeout:10s'], result.stdout.splitlines()[:2])
        for args in (['-s', 'physical', 'emu', 'kill'], ['-s', 'emulator-5554', 'shell', 'emu', 'kill'],
                     ['-s', 'emulator-5554x', 'emu', 'kill']):
            result = subprocess.run([str(self.adb), *args], env=env, capture_output=True, text=True)
            self.assertEqual(7, result.returncode)
            self.assertNotIn('timeout:', result.stdout)

    def test_unexpected_existing_backup_refuses_overwrite(self):
        self.adb.with_name('adb.trainpilot-real').write_text('unrelated')
        with self.assertRaises(RuntimeError):
            guard.install(self.env)
        self.assertIn('exit 7', self.adb.read_text())

    def test_real_hung_shutdown_is_terminated_with_nonzero_status(self):
        self.adb.write_text('#!/bin/sh\nexec sleep 30\n')
        guard.install(self.env)
        # Exercise the real timeout utility, shortening only the test deadline.
        timeout = self.sdk / 'timeout'
        timeout.write_text('#!/bin/sh\nshift 2\nexec /usr/bin/timeout --kill-after=.1s .1s "$@"\n')
        timeout.chmod(0o755)
        env = dict(self.env, PATH=str(self.sdk) + os.pathsep + os.environ['PATH'])
        result = subprocess.run([str(self.adb), '-s', 'emulator-5554', 'emu', 'kill'],
                                env=env, capture_output=True, timeout=3)
        self.assertEqual(124, result.returncode)


if __name__ == '__main__':
    unittest.main()
