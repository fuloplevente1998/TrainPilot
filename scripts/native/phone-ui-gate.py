#!/usr/bin/env python3
"""Run real native instrumentation and capture unchanged adb screencap PNGs at UI checkpoints."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args, check=True, timeout=15):
    return subprocess.run(["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          timeout=timeout, check=check).stdout


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def report(raw, destination):
    cases, current, declared = [], {}, 0
    stack_continues = False
    for line in raw.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: "):
            key, separator, value = line.removeprefix("INSTRUMENTATION_STATUS: ").partition("=")
            stack_continues = key == "stack"
            if separator:
                current[key] = value
                if key == "numtests" and value.isdigit():
                    declared = max(declared, int(value))
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: "):
            code = int(line.rsplit(":", 1)[1])
            if code != 1 and current.get("test"):
                cases.append((current.copy(), code))
            current = {}
            stack_continues = False
        elif line.startswith("INSTRUMENTATION_"):
            stack_continues = False
        elif stack_continues:
            current["stack"] += "\n" + line
    failed = "FAILURES!!!" in raw or "INSTRUMENTATION_FAILED" in raw or "shortMsg=" in raw
    identities = [(case.get("class"), case.get("test")) for case, _ in cases]
    complete = declared > 0 and len(cases) == declared and len(set(identities)) == declared
    final = re.search(r"OK \((\d+) tests?\)", raw)
    complete = complete and final is not None and int(final.group(1)) == declared
    if not cases or (failed or not complete) and all(code == 0 for _, code in cases):
        cases.append(({"class": "runner", "test": "instrumentation_execution", "stack": raw[-8000:]}, -1))
    root = ET.Element("testsuite", name="native-phone-adb-derived", tests=str(len(cases)),
                      failures=str(sum(code == -2 for _, code in cases)),
                      errors=str(sum(code not in (0, -2, -3, -4) for _, code in cases)),
                      skipped=str(sum(code in (-3, -4) for _, code in cases)))
    props = ET.SubElement(root, "properties")
    ET.SubElement(props, "property", name="reportOrigin", value="derived-from-raw-adb-instrumentation-not-runner-xml")
    for case, code in cases:
        element = ET.SubElement(root, "testcase", classname=case["class"], name=case["test"])
        if code != 0:
            tag = "failure" if code == -2 else "skipped" if code in (-3, -4) else "error"
            ET.SubElement(element, tag).text = case.get("stack", "Instrumentation status " + str(code))
    ET.ElementTree(root).write(destination, encoding="utf-8", xml_declaration=True)
    return complete and not failed and all(code == 0 for _, code in cases)


GPS_CLASS = "com.repforge.app.NativeGpsPermissionRecoveryUiTest"
GPS_CASES = (
    ("approximateGrantOpensAppPermissionRouteAndBackDoesNotStartMeasurement", "approximate"),
    ("systemLocationOffHasOwnSettingsAndRequiresExplicitRetryAfterReturn", "precise"),
    ("runtimePermissionDismissedByBackNeverRecordsDistance", "denied"),
    ("staleOriginalWorkoutIsRefusedAfterSettingsOrPermissionBlock", "precise"),
)


def prepare_gps_permissions(mode):
    # Revocation kills the target UID: do this on the host BEFORE starting instrumentation.
    adb("shell", "am", "force-stop", "com.repforge.app")
    for suffix in ("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"):
        permission = "android.permission." + suffix
        adb("shell", "pm", "revoke", "com.repforge.app", permission)
        adb("shell", "pm", "clear-permission-flags", "com.repforge.app", permission,
            "user-set", "user-fixed")
    if mode in ("approximate", "precise"):
        adb("shell", "pm", "grant", "com.repforge.app", "android.permission.ACCESS_COARSE_LOCATION")
    if mode == "precise":
        adb("shell", "pm", "grant", "com.repforge.app", "android.permission.ACCESS_FINE_LOCATION")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--app", type=Path, required=True)
    parser.add_argument("--tests", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--timeout", type=int, default=1200)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    (output / "screenshots").mkdir(exist_ok=True)
    manifest = {"commit": args.commit, "appSha256": digest(args.app), "testSha256": digest(args.tests),
                "imageOrigin": "unmodified-adb-exec-out-screencap-p", "data": "isolated-emulator-test-fixtures",
                "device": adb("shell", "getprop", "ro.product.model").decode().strip(),
                "result": "running", "api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(), "screenshots": []}
    process = None
    try:
        adb("install", "-r", "-t", str(args.app))
        adb("install", "-r", "-t", str(args.tests))
        for permission in ["POST_NOTIFICATIONS", "ACCESS_COARSE_LOCATION", "ACCESS_FINE_LOCATION"]:
            adb("shell", "pm", "grant", "com.repforge.app", "android.permission." + permission, check=False)
        adb("shell", "settings", "put", "global", "window_animation_scale", "0")
        adb("shell", "settings", "put", "global", "transition_animation_scale", "0")
        adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
        adb("shell", "rm", "-f", "/data/local/tmp/trainpilot-shot-request", "/data/local/tmp/trainpilot-shot-ack")
        seen = set()
        invocations = [("instrumentation.txt", ["-e", "package", "com.repforge.app", "-e", "notClass", GPS_CLASS], None)] + [
            ("gps-" + method + ".txt", ["-e", "class", GPS_CLASS + "#" + method], mode)
            for method, mode in GPS_CASES
        ]
        manifest["additionalInstrumentation"] = [name for name, _, mode in invocations if mode]
        results = []
        for filename, selector, mode in invocations:
            if mode:
                prepare_gps_permissions(mode)
            with (output / filename).open("wb") as log:
                process = subprocess.Popen(["adb", "shell", "am", "instrument", "-w", "-r", *selector,
                                            "com.repforge.app.test/androidx.test.runner.AndroidJUnitRunner"],
                                           stdout=log, stderr=subprocess.STDOUT)
                deadline = time.monotonic() + args.timeout
                while process.poll() is None:
                    if time.monotonic() > deadline:
                        raise TimeoutError("Native phone instrumentation exceeded deadline")
                    raw = adb("shell", "cat", "/data/local/tmp/trainpilot-shot-request", check=False).decode().strip()
                    if raw.startswith("{"):
                        try:
                            request = json.loads(raw)
                        except json.JSONDecodeError:
                            time.sleep(.1)
                            continue
                        name = request["name"]
                        if not re.fullmatch(r"[a-z0-9_]{1,120}", name):
                            raise ValueError("Invalid screenshot checkpoint name")
                        if name not in seen:
                            # Capture only the running application's device pixels. No overlays/crops/compositing.
                            focus = next((line.strip() for line in adb("shell", "dumpsys", "window").decode(errors="replace").splitlines()
                                          if "mCurrentFocus=" in line), "")
                            if "com.repforge.app/" not in focus:
                                raise RuntimeError("Screenshot checkpoint has no foreground native app: " + focus)
                            png = adb("exec-out", "screencap", "-p", timeout=8)
                            if not png.startswith(b"\x89PNG\r\n\x1a\n") or len(png) < 100:
                                raise ValueError("Screenshot was not a complete PNG")
                            file = output / "screenshots" / (name + ".png")
                            file.write_bytes(png)
                            manifest["screenshots"].append({**request, "file": "screenshots/" + file.name,
                                                            "sha256": digest(file), "bytes": len(png), "focus": focus})
                            seen.add(name)
                            adb("shell", "printf '%s' '" + name + "' > /data/local/tmp/trainpilot-shot-ack")
                    time.sleep(.15)
            raw = (output / filename).read_text(errors="replace")
            valid = report(raw, output / ("instrumentation-derived.xml" if not mode else filename + ".xml"))
            results.append(process.returncode == 0 and valid)
        if not all(results) or not seen:
            raise RuntimeError("Native phone UI failed or produced no screenshots; inspect raw artifacts")
        manifest["result"] = "success"
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        if manifest["result"] != "success":
            manifest["result"] = "failed"
        (output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
        for name, command in [("logcat.txt", ["logcat", "-d", "-t", "3000"]),
                              ("activity.txt", ["shell", "dumpsys", "activity", "activities"])]:
            try:
                (output / name).write_bytes(adb(*command, check=False))
            except (subprocess.SubprocessError, OSError):
                pass


if __name__ == "__main__":
    main()
