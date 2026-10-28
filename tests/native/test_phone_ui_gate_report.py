import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET


SCRIPT = Path(__file__).resolve().parents[2] / 'scripts/native/phone-ui-gate.py'
spec = importlib.util.spec_from_file_location('phone_ui_gate', SCRIPT)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


def case(name, declared=2, code=0, stack=''):
    return (f'INSTRUMENTATION_STATUS: class=app.RealUiTest\n'
            f'INSTRUMENTATION_STATUS: test={name}\n'
            f'INSTRUMENTATION_STATUS: numtests={declared}\n'
            + (f'INSTRUMENTATION_STATUS: stack={stack}\n' if stack else '')
            + f'INSTRUMENTATION_STATUS_CODE: {code}\n')


class PhoneUiReportTest(unittest.TestCase):
    def report(self, raw):
        with tempfile.TemporaryDirectory() as folder:
            out = Path(folder) / 'report.xml'
            success = gate.report(raw, out)
            return success, ET.parse(out).getroot()

    def test_complete_successful_suite_is_accepted(self):
        success, xml = self.report(case('one') + case('two') + 'OK (2 tests)\n')
        self.assertTrue(success)
        self.assertEqual('2', xml.get('tests'))

    def test_truncation_after_successful_case_is_never_success(self):
        success, xml = self.report(case('one'))
        self.assertFalse(success)
        self.assertEqual('1', xml.get('errors'))

    def test_early_ok_and_duplicate_terminals_do_not_hide_missing_tests(self):
        self.assertFalse(self.report(case('one') + 'OK (1 test)\n')[0])
        self.assertFalse(self.report(case('one') + case('one') + 'OK (2 tests)\n')[0])

    def test_failure_retains_multiline_trace_and_prevents_acceptance(self):
        trace = 'AssertionError: wrong value\n\tat app.RealUiTest.failure(Test.kt:42)\nCaused by: actual cause'
        success, xml = self.report(case('one') + case('two', code=-2, stack=trace) + 'FAILURES!!!\n')
        self.assertFalse(success)
        self.assertEqual(trace, xml.find('testcase/failure').text)

    def test_process_crash_after_passes_is_an_error(self):
        success, xml = self.report(case('one') + case('two') +
                                   'INSTRUMENTATION_RESULT: shortMsg=Process crashed.\n')
        self.assertFalse(success)
        self.assertEqual('1', xml.get('errors'))


if __name__ == '__main__':
    unittest.main()


class GpsPermissionSetupTest(unittest.TestCase):
    def test_host_revokes_and_clears_denial_before_grants(self):
        from unittest.mock import patch
        with patch.object(gate, 'adb') as adb:
            gate.prepare_gps_permissions('approximate')
        calls = [c.args for c in adb.call_args_list]
        self.assertEqual(('shell', 'am', 'force-stop', 'com.repforge.app'), calls[0])
        self.assertEqual(2, sum('revoke' in c for c in calls))
        self.assertEqual(2, sum('clear-permission-flags' in c for c in calls))
        self.assertEqual([('shell', 'pm', 'grant', 'com.repforge.app',
                           'android.permission.ACCESS_COARSE_LOCATION')],
                         [c for c in calls if 'grant' in c])

    def test_denied_setup_never_grants_location(self):
        from unittest.mock import patch
        with patch.object(gate, 'adb') as adb:
            gate.prepare_gps_permissions('denied')
        self.assertFalse(any('grant' in c.args for c in adb.call_args_list))
        self.assertEqual(4, len(gate.GPS_CASES))
        self.assertEqual(4, len({name for name, _ in gate.GPS_CASES}))


class EmulatorDisplayProfileTest(unittest.TestCase):
    @staticmethod
    def response(*args, **kwargs):
        if args == ('shell', 'getprop', 'ro.kernel.qemu'):
            return b'1\n'
        if args == ('shell', 'wm', 'size'):
            return b'Physical size: 320x640\nOverride size: 1080x2392\n'
        if args == ('shell', 'wm', 'density'):
            return b'Physical density: 160\nOverride density: 440\n'
        if args == ('shell', 'settings', 'get', 'system', 'font_scale'):
            return b'1.0\n'
        return b''

    def test_requested_phone_geometry_and_density_are_applied_and_recorded(self):
        from unittest.mock import patch
        with patch.object(gate, 'adb', side_effect=self.response) as calls:
            d = gate.configure_display('1080x2392', '440', '1.0')
        calls.assert_any_call('shell', 'wm', 'size', '1080x2392')
        self.assertEqual('1080x2392', d['requestedSizePx'])
        self.assertEqual(440, d['requestedDensityDpi'])
        self.assertIn('Override density: 440', d['wmDensity'])

    def test_pixels_without_density_cannot_accidentally_test_a_tablet_layout(self):
        from unittest.mock import patch
        with patch.object(gate, 'adb') as calls:
            with self.assertRaises(ValueError):
                gate.configure_display('1080x2392')
        calls.assert_not_called()

    def test_profile_readback_mismatch_is_not_accepted(self):
        from unittest.mock import patch
        def response(*args, **kwargs):
            return b'Physical size: 320x640\n' if args == ('shell', 'wm', 'size') else self.response(*args, **kwargs)
        with patch.object(gate, 'adb', side_effect=response), self.assertRaises(RuntimeError):
            gate.configure_display('1080x2392', '440', '1.0')

    def test_screenshots_are_measured_not_rescaled_and_wrong_size_fails(self):
        import struct
        png = b'\x89PNG\r\n\x1a\n' + struct.pack('>I', 13) + b'IHDR' + struct.pack('>II', 1080, 2392)
        self.assertEqual((1080, 2392), gate.screenshot_dimensions(png, '1080x2392'))
        with self.assertRaises(ValueError):
            gate.screenshot_dimensions(png, '320x640')
