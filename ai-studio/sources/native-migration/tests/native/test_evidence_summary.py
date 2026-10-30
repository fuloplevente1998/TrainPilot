"""Synthetic offline regressions for scripts/native/evidence-summary.py."""
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import struct
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zlib

MODULE_PATH = Path(__file__).resolve().parents[2] / "scripts/native/evidence-summary.py"
spec = importlib.util.spec_from_file_location("native_evidence_summary", MODULE_PATH)
evidence = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evidence)
SHA = "a" * 40
OTHER = "b" * 40


def checksum(data):
    return hashlib.sha256(data).hexdigest()


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)


def png():
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)) +
            chunk(b"IDAT", zlib.compress(b"\x00\x00\x00\x00")) + chunk(b"IEND", b""))


def instrumentation(declared, states, crash=False, complete=True):
    lines = []
    for index, state in enumerate(states):
        for code in (1, state):
            lines += ["INSTRUMENTATION_STATUS: class=example.Cases",
                      f"INSTRUMENTATION_STATUS: current={index + 1}",
                      f"INSTRUMENTATION_STATUS: numtests={declared}",
                      f"INSTRUMENTATION_STATUS: test=method{index}",
                      f"INSTRUMENTATION_STATUS_CODE: {code}"]
    if crash:
        lines.append("INSTRUMENTATION_RESULT: shortMsg=Process crashed.")
        lines.append("INSTRUMENTATION_CODE: 0")
    elif complete:
        lines += [f"INSTRUMENTATION_RESULT: stream=OK ({len(states)} tests)", "INSTRUMENTATION_CODE: -1"]
    return "\n".join(lines) + "\n"


class EvidenceTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.inputs = self.root / "artifacts"
        self.inputs.mkdir()
        self.out = self.root / "report.json"
        self.md = self.root / "report.md"

    def run_summary(self, *opts, sha=SHA):
        argv = [str(self.inputs), "--expected-commit", sha, "--json-out", str(self.out),
                "--markdown-out", str(self.md), *opts]
        with contextlib.redirect_stdout(io.StringIO()):
            code = evidence.main(argv)
        return code, json.loads(self.out.read_text())

    def phone(self, declared=69, states=None, crash=False, shots=True, commit=SHA):
        if states is None:
            states = [0] * declared
        folder = self.inputs / "TrainPilot-Native-Phone-UI-Evidence"
        folder.mkdir(exist_ok=True)
        (folder / "instrumentation.txt").write_text(instrumentation(declared, states, crash))
        app, test = self.root / "app-debug.apk", self.root / "app-debug-androidTest.apk"
        app.write_bytes(b"app-APK-bytes")
        test.write_bytes(b"test-APK-bytes")
        entries = []
        if shots:
            (folder / "screenshots").mkdir(exist_ok=True)
            shot = folder / "screenshots" / "home.png"
            shot.write_bytes(png())
            entries.append({"name": "home", "file": "screenshots/home.png", "sha256": checksum(shot.read_bytes()),
                            "bytes": shot.stat().st_size})
        data = {"commit": commit, "appSha256": checksum(app.read_bytes()), "testSha256": checksum(test.read_bytes()),
                "imageOrigin": "unmodified-adb-exec-out-screencap-p", "result": "failed" if crash else "success",
                "screenshots": entries}
        (folder / "manifest.json").write_text(json.dumps(data))
        return folder, app, test

    def apk_options(self, app, test):
        return ("--phone-apk", str(app), "--phone-test-apk", str(test))

    def wear(self, counts=(68, 56, 59), commit=SHA):
        for idx, n in enumerate(counts):
            folder = self.inputs / f"TrainPilot-Native-Wear-UI-Results-{commit}-{idx}"
            folder.mkdir()
            text = instrumentation(n, [0] * n).replace("test=method", f"test=shard{idx}method")
            (folder / "instrumentation.txt").write_text(text)
            (folder / "shot.png").write_bytes(png())

    def jvm(self, app=1096, wear=167, fail=0, skip=0, malformed=False):
        folder = self.inputs / f"TrainPilot-Native-Tests-{SHA}"
        for module, n in (("app", app), ("wear", wear)):
            out = folder / module / "build/test-results/testDebugUnitTest/TEST-example.Cases.xml"
            out.parent.mkdir(parents=True, exist_ok=True)
            root = ET.Element("testsuite", tests=str(n), failures=str(fail if module == "app" else 0),
                              errors="0", skipped=str(skip if module == "app" else 0))
            for i in range(n):
                case = ET.SubElement(root, "testcase", classname=f"{module}.Cases", name=f"case{i}")
                if module == "app" and i < fail:
                    ET.SubElement(case, "failure").text = "secret token example, never echo"
                elif module == "app" and i < fail + skip:
                    ET.SubElement(case, "skipped")
            out.write_bytes(ET.tostring(root))
            if malformed and module == "wear":
                out.write_text("<testsuite")

    def test_four_isolated_gps_cases_are_mandatory_in_final_qa(self):
        folder, app, test = self.phone(declared=2)
        names = ["gps-" + method + ".txt" for method in evidence.REQUIRED_GPS_METHODS]
        data = json.loads((folder / "manifest.json").read_text())
        data["additionalInstrumentation"] = names
        (folder / "manifest.json").write_text(json.dumps(data))
        opts = (*self.apk_options(app, test), "--require-isolated-gps")
        code, report = self.run_summary(*opts)
        self.assertEqual(1, code)  # all four logs missing must fail closed
        for filename, method in zip(names, evidence.REQUIRED_GPS_METHODS):
            raw = instrumentation(1, [0]).replace("example.Cases",
                "com.repforge.app.NativeGpsPermissionRecoveryUiTest").replace("method0", method)
            (folder / filename).write_text(raw)
        code, report = self.run_summary(*opts)
        self.assertEqual(0, code)
        self.assertEqual(6, report["gates"]["phone"]["passed"])
        # Green main suite and three green GPS cases must still be FAILED.
        data["additionalInstrumentation"] = names[:-1]
        (folder / "manifest.json").write_text(json.dumps(data))
        code, report = self.run_summary(*opts)
        self.assertEqual(1, code)
        self.assertEqual("FAILED", report["gates"]["phone"]["status"])
        self.assertTrue(any(i["kind"] == "invalid-instrumentation-runs"
                            for i in report["gates"]["phone"]["issues"]))

    def test_isolated_gps_log_is_required_and_counted(self):
        folder, app, test = self.phone(declared=2)
        method = "explicitRetry"
        filename = "gps-" + method + ".txt"
        data = json.loads((folder / "manifest.json").read_text())
        data["additionalInstrumentation"] = [filename]
        (folder / "manifest.json").write_text(json.dumps(data))
        code, report = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(1, code)  # absent GPS evidence cannot be silently omitted
        raw = instrumentation(1, [0]).replace("example.Cases", "com.repforge.app.NativeGpsPermissionRecoveryUiTest").replace("method0", method)
        (folder / filename).write_text(raw)
        code, report = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(0, code)
        self.assertEqual(3, report["gates"]["phone"]["passed"])
        (folder / filename).write_text(instrumentation(1, [0], crash=True))
        code, report = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(1, code)
        self.assertTrue(report["gates"]["phone"]["crashed"])

    def test_phone_crashed_69_declared_18_terminal_12_pass_6_fail(self):
        folder, app, test = self.phone(states=[0] * 12 + [-2] * 6, crash=True, shots=False)
        code, result = self.run_summary(*self.apk_options(app, test), "--require-gate", "phone")
        self.assertEqual(code, 1)
        p = result["gates"]["phone"]
        self.assertEqual((p["declared"], p["executed"], p["passed"], p["failed"], p["missing"]),
                         (69, 18, 12, 6, 51))
        self.assertTrue(p["crashed"])
        self.assertFalse(p["complete"])
        self.assertNotIn("secret", self.md.read_text())

    def test_phone_full_69_without_screenshot_is_incomplete(self):
        folder, app, test = self.phone(shots=False)
        code, result = self.run_summary(*self.apk_options(app, test), "--require-screenshot", "home")
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["status"], "INCOMPLETE")
        self.assertEqual(result["gates"]["phone"]["passed"], 69)

    def test_phone_complete_requires_matching_apk_and_png(self):
        folder, app, test = self.phone()
        code, result = self.run_summary(*self.apk_options(app, test), "--require-screenshot", "home")
        self.assertEqual(code, 0)
        self.assertEqual(result["status"], "PASS")
        shot = folder / "screenshots/home.png"
        shot.write_bytes(shot.read_bytes() + b"corrupt")
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertEqual(result["status"], "FAILED")

    def test_phone_png_hash_mismatch_and_apk_mismatch(self):
        folder, app, test = self.phone()
        raw = json.loads((folder / "manifest.json").read_text())
        raw["screenshots"][0]["sha256"] = "0" * 64
        (folder / "manifest.json").write_text(json.dumps(raw))
        app.write_bytes(b"wrong")
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        kinds = {i["kind"] for i in result["gates"]["phone"]["issues"]}
        self.assertIn("png-hash-mismatch", kinds)
        self.assertIn("apk-hash-mismatch", kinds)

    def test_phone_missing_apk_is_not_pass(self):
        self.phone()
        code, result = self.run_summary()
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["status"], "INCOMPLETE")

    def test_wear_full_three_shards_183_unique(self):
        self.wear()
        code, result = self.run_summary("--require-gate", "wear", "--wear-total", "183")
        self.assertEqual(code, 0)
        w = result["gates"]["wear"]
        self.assertEqual((w["executed"], w["passed"], w["unique_cases"]), (183, 183, 183))
        self.assertEqual([s["declared"] for s in w["shards"]], [68, 56, 59])

    def test_wear_missing_or_duplicate_shard_rejected(self):
        self.wear((68, 56))
        code, result = self.run_summary("--require-gate", "wear", "--wear-total", "183")
        self.assertNotEqual(code, 0)
        self.assertIn("missing-shard", {i["kind"] for i in result["gates"]["wear"]["issues"]})
        (self.inputs / f"TrainPilot-Native-Wear-UI-Results-{SHA}-2").mkdir()
        (self.inputs / f"TrainPilot-Native-Wear-UI-Results-{SHA}-2/instrumentation.txt").write_text(
            instrumentation(59, [0] * 59).replace("test=method", "test=shard1method"))
        code, result = self.run_summary("--require-gate", "wear")
        self.assertEqual(code, 1)
        self.assertIn("duplicate-case", {i["kind"] for i in result["gates"]["wear"]["issues"]})

    def test_duplicate_wear_shard_index_is_failed(self):
        self.wear()
        # A second unpacked artifact with the same provenance and shard number.
        duplicate_root = self.inputs / "second-download"
        duplicate = duplicate_root / f"TrainPilot-Native-Wear-UI-Results-{SHA}-1"
        duplicate.mkdir(parents=True)
        (duplicate / "instrumentation.txt").write_text(
            instrumentation(56, [0] * 56).replace("test=method", "test=anothermethod"))
        code, result = self.run_summary("--require-gate", "wear", "--wear-total", "183")
        self.assertEqual(code, 1)
        kinds = {item["kind"] for item in result["gates"]["wear"]["issues"]}
        self.assertIn("duplicate-or-invalid-shard", kinds)

    def test_apk_checksums_require_actual_matching_bytes(self):
        folder = self.inputs / "TrainPilot-Native-Update-Verification"
        folder.mkdir()
        apk = self.root / "app-release.apk"
        apk.write_bytes(b"signed-release")
        logical = "app/build/outputs/apk/release/app-release.apk"
        (folder / "native-apk-sha256.txt").write_text(checksum(apk.read_bytes()) + "  " + logical + "\n")
        code, result = self.run_summary("--require-gate", "apk")
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["apk"]["status"], "INCOMPLETE")
        code, result = self.run_summary("--require-gate", "apk", "--apk-file", logical + "=" + str(apk))
        self.assertEqual(code, 0)
        apk.write_bytes(b"tampered-release")
        code, result = self.run_summary("--require-gate", "apk", "--apk-file", logical + "=" + str(apk))
        self.assertEqual(code, 1)

    def test_wear_mixed_source_and_truncated_log(self):
        self.wear((68, 56))
        other = self.inputs / f"TrainPilot-Native-Wear-UI-Results-{OTHER}-2"
        other.mkdir()
        (other / "instrumentation.txt").write_text(instrumentation(59, [0] * 59, complete=False))
        code, result = self.run_summary("--require-gate", "wear")
        self.assertEqual(code, 1)
        self.assertEqual(result["gates"]["wear"]["shards"][2]["status"], "INCOMPLETE")
        self.assertIn("mixed-source", {i["kind"] for i in result["gates"]["wear"]["issues"]})

    def test_jvm_1096_plus_167_and_failure_skip(self):
        self.jvm()
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 0)
        self.assertEqual(result["gates"]["jvm"]["tests"], 1263)
        self.assertEqual(result["gates"]["jvm"]["modules"]["app"]["tests"], 1096)
        self.assertEqual(result["gates"]["jvm"]["modules"]["wear"]["tests"], 167)
        self.jvm(fail=1, skip=1)
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 1)
        self.assertEqual(result["gates"]["jvm"]["failures"], 1)
        self.assertEqual(result["gates"]["jvm"]["skips"], 1)

    def test_malformed_jvm_xml_and_missing_required_gate(self):
        self.jvm(malformed=True)
        code, result = self.run_summary("--require-gate", "jvm", "--require-gate", "phone")
        self.assertEqual(code, 1)
        self.assertEqual(result["gates"]["jvm"]["status"], "FAILED")
        self.assertEqual(result["gates"]["phone"]["status"], "INCOMPLETE")

    def test_mixed_phone_commit_cannot_pass(self):
        folder, app, test = self.phone(commit=OTHER)
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertEqual(result["status"], "FAILED")

    def test_output_may_not_overwrite_source(self):
        self.phone()
        self.out = self.inputs / "report.json"
        with self.assertRaises(SystemExit):
            evidence.main([str(self.inputs), "--expected-commit", SHA,
                           "--json-out", str(self.out), "--markdown-out", str(self.md)])


    def test_normal_failed_runner_75_terminal_35_pass_40_fail_is_not_success(self):
        folder, app, test = self.phone(declared=75, states=[0] * 35 + [-2] * 40, shots=False)
        code, result = self.run_summary(*self.apk_options(app, test), "--require-gate", "phone")
        self.assertEqual(code, 1)
        phone = result["gates"]["phone"]
        self.assertEqual((phone["declared"], phone["executed"], phone["passed"], phone["failed"]),
                         (75, 75, 35, 40))
        self.assertEqual(phone["status"], "FAILED")
        self.assertFalse(phone["complete"])

    def test_forged_ok_in_stack_cannot_replace_runner_summary(self):
        folder, app, test = self.phone(declared=2, states=[0, 0])
        raw = instrumentation(2, [0, 0], complete=False)
        raw += "INSTRUMENTATION_STATUS: stack=private\nOK (2 tests)\nINSTRUMENTATION_CODE: -1\n"
        (folder / "instrumentation.txt").write_text(raw)
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["status"], "INCOMPLETE")

    def test_conflicting_numtests_and_duplicate_terminal_are_failed(self):
        folder, app, test = self.phone(declared=2, states=[0, 0])
        path = folder / "instrumentation.txt"
        path.write_text(path.read_text().replace("numtests=2", "numtests=3", 1))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("declaration-conflict", {x["kind"] for x in result["gates"]["phone"]["issues"]})
        path.write_text(instrumentation(2, [0, 0]).replace(
            "INSTRUMENTATION_RESULT: stream=",
            "INSTRUMENTATION_STATUS: class=example.Cases\nINSTRUMENTATION_STATUS: test=method0\n"
            "INSTRUMENTATION_STATUS: numtests=2\nINSTRUMENTATION_STATUS_CODE: 0\n"
            "INSTRUMENTATION_RESULT: stream="))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("duplicate-terminal", {x["kind"] for x in result["gates"]["phone"]["issues"]})

    def test_skip_assumption_and_incomplete_terminal_are_not_pass(self):
        folder, app, test = self.phone(declared=2, states=[0, -3])
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["skipped"], 1)
        (folder / "instrumentation.txt").write_text(instrumentation(2, [0], complete=False))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["missing"], 1)

    def test_failed_summary_without_failed_codes_is_not_pass(self):
        folder, app, test = self.phone(declared=2, states=[0, 0])
        raw = (folder / "instrumentation.txt").read_text()
        raw = raw.replace("INSTRUMENTATION_CODE: -1",
                          "FAILURES!!!\nTests run: 2, Failures: 1\nINSTRUMENTATION_CODE: -1")
        (folder / "instrumentation.txt").write_text(raw)
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("runner-failures", {x["kind"] for x in result["gates"]["phone"]["issues"]})

    def test_jvm_testsuites_root_totals_cannot_contradict_child(self):
        self.jvm(app=2, wear=0)
        path = next((self.inputs / f"TrainPilot-Native-Tests-{SHA}" / "app").rglob("TEST-*.xml"))
        existing = ET.parse(path).getroot()
        outer = ET.Element("testsuites", tests="999", failures="0", errors="0", skipped="0")
        outer.append(existing)
        path.write_bytes(ET.tostring(outer))
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 1)
        self.assertIn("malformed-xml", {x["kind"] for x in result["gates"]["jvm"]["issues"]})

    def test_jvm_rejects_multiple_outcomes_and_invalid_utf8(self):
        self.jvm(app=2, wear=0)
        path = next((self.inputs / f"TrainPilot-Native-Tests-{SHA}" / "app").rglob("TEST-*.xml"))
        root = ET.parse(path).getroot()
        ET.SubElement(root.find("testcase"), "failure")
        root.set("failures", "1")
        ET.SubElement(root.find("testcase"), "skipped")
        root.set("skipped", "1")
        path.write_bytes(ET.tostring(root))
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 1)
        path.write_bytes(b"<?xml version='1.0'?><testsuite>\xff</testsuite>")
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 1)

    def test_jvm_parameterized_case_identities_are_distinct(self):
        self.jvm(app=2, wear=0)
        path = next((self.inputs / f"TrainPilot-Native-Tests-{SHA}" / "app").rglob("TEST-*.xml"))
        root = ET.parse(path).getroot()
        cases = root.findall("testcase")
        cases[0].set("name", "parameter[0]")
        cases[1].set("name", "parameter[1]")
        path.write_bytes(ET.tostring(root))
        code, result = self.run_summary("--require-gate", "jvm")
        self.assertEqual(code, 0)
        self.assertEqual(result["gates"]["jvm"]["tests"], 2)

    def test_png_valid_crc_but_wrong_dimensions_or_scanline_is_rejected(self):
        folder, app, test = self.phone(declared=1, states=[0])
        shot = folder / "screenshots/home.png"
        data = png()
        ihdr = struct.pack(">IIBBBBB", 2, 1, 8, 2, 0, 0, 0)
        corrupted = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + data[33:]
        shot.write_bytes(corrupted)
        manifest = json.loads((folder / "manifest.json").read_text())
        manifest["screenshots"][0].update(sha256=checksum(corrupted), bytes=len(corrupted))
        (folder / "manifest.json").write_text(json.dumps(manifest))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("bad-png", {x["kind"] for x in result["gates"]["phone"]["issues"]})

    def test_png_invalid_filters_and_noncontiguous_idat_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "shot.png"
            img = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
            target.write_bytes(img + chunk(b"IDAT", zlib.compress(b"\x05\x00\x00\x00")) + chunk(b"IEND", b""))
            self.assertFalse(evidence.png_ok(target))
            compressed = zlib.compress(b"\x00\x00\x00\x00")
            target.write_bytes(img + chunk(b"IDAT", compressed[:4]) +
                               chunk(b"tEXt", b"hi\x00x") + chunk(b"IDAT", compressed[4:]) +
                               chunk(b"IEND", b""))
            self.assertFalse(evidence.png_ok(target))
            self.assertTrue(evidence.png_ok(Path(self.phone(declared=1, states=[0])[0] / "screenshots/home.png")))

    def test_apk_metadata_partial_hash_verification_is_incomplete(self):
        folder = self.inputs / "TrainPilot-Native-Update-Verification"
        folder.mkdir()
        app, wear = self.root / "phone.apk", self.root / "wear.apk"
        app.write_bytes(b"phone")
        wear.write_bytes(b"wear")
        logical1, logical2 = "app/phone.apk", "wear/watch.apk"
        (folder / "native-apk-sha256.txt").write_text(
            checksum(app.read_bytes()) + "  " + logical1 + "\n" +
            checksum(wear.read_bytes()) + "  " + logical2 + "\n")
        code, result = self.run_summary("--require-gate", "apk", "--apk-file", logical1 + "=" + str(app))
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["apk"]["status"], "INCOMPLETE")
        code, result = self.run_summary("--apk-file", logical1 + "=" + str(app),
                                        "--apk-file", logical2 + "=" + str(wear))
        self.assertEqual(code, 0)

    def test_partial_phone_artifact_cannot_hide_behind_green_jvm(self):
        self.jvm(app=2, wear=0)
        folder = self.inputs / "TrainPilot-Native-Phone-UI-Evidence"
        folder.mkdir()
        (folder / "instrumentation.txt").write_text(instrumentation(2, [0, 0]))
        code, result = self.run_summary("--require-gate", "jvm", "--require-gate", "phone")
        self.assertEqual(code, 1)
        self.assertEqual(result["gates"]["phone"]["status"], "FAILED")

    def test_bad_utf8_log_is_failed_not_pass(self):
        folder, app, test = self.phone(declared=1, states=[0])
        (folder / "instrumentation.txt").write_bytes(b"\xff\xfe")
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("invalid-instrumentation", {x["kind"] for x in result["gates"]["phone"]["issues"]})

    def test_private_multiline_failures_never_enter_reports(self):
        folder, app, test = self.phone(declared=1, states=[-2])
        raw = (folder / "instrumentation.txt").read_text()
        raw = raw.replace("INSTRUMENTATION_STATUS_CODE: -2",
                          "INSTRUMENTATION_STATUS: stack=https://secrets.invalid/t?token=ABCD\n"
                          "record a@example.com GPS 46.123,18.234\nINSTRUMENTATION_STATUS_CODE: -2")
        (folder / "instrumentation.txt").write_text(raw)
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        all_report = self.out.read_text() + self.md.read_text()
        for secret in ("https://", "ABCD", "a@example.com", "46.123", "18.234"):
            self.assertNotIn(secret, all_report)

    def test_required_gate_inferred_from_wear_total_and_screenshot(self):
        self.jvm(app=2, wear=0)
        code, result = self.run_summary("--wear-total", "183", "--require-screenshot", "home")
        self.assertEqual(code, 2)
        self.assertEqual(result["gates"]["phone"]["status"], "INCOMPLETE")
        self.assertEqual(result["gates"]["wear"]["status"], "INCOMPLETE")

    def test_missing_wear_instrumentation_is_failed(self):
        self.wear()
        (self.inputs / f"TrainPilot-Native-Wear-UI-Results-{SHA}-1/instrumentation.txt").unlink()
        code, result = self.run_summary("--wear-total", "183")
        self.assertEqual(code, 1)
        self.assertIn("invalid-instrumentation", {x["kind"] for x in result["gates"]["wear"]["shards"][1]["issues"]})

    def test_duplicate_runner_stream_header_not_success(self):
        folder, app, test = self.phone(declared=1, states=[0])
        log = folder / "instrumentation.txt"
        log.write_text(log.read_text().replace("INSTRUMENTATION_CODE: -1",
                       "INSTRUMENTATION_RESULT: stream=OK (1 test)\nINSTRUMENTATION_CODE: -1"))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertEqual(result["gates"]["phone"]["status"], "FAILED")

    def test_malformed_manifest_and_mismatched_screenshot_name_rejected(self):
        folder, app, test = self.phone(declared=1, states=[0])
        manifest = folder / "manifest.json"
        original = json.loads(manifest.read_text())
        manifest.write_text("{bad json")
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("invalid-manifest", {x["kind"] for x in result["gates"]["phone"]["issues"]})
        original["screenshots"][0]["name"] = "wrong"
        manifest.write_text(json.dumps(original))
        code, result = self.run_summary(*self.apk_options(app, test))
        self.assertEqual(code, 1)
        self.assertIn("invalid-screenshot-entry", {x["kind"] for x in result["gates"]["phone"]["issues"]})


if __name__ == "__main__":
    unittest.main()
