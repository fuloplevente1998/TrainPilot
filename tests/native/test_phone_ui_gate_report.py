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
