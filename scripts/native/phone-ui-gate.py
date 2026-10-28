#!/usr/bin/env python3
"""Run real native instrumentation and capture unchanged adb screencap PNGs at UI checkpoints."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import struct
import time
import xml.etree.ElementTree as ET


def adb(*args, check=True, timeout=15):
    return subprocess.run(["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          timeout=timeout, check=check).stdout


def configure_display(size="", density="", font_scale=""):
    """Configure the emulator before installing/launching the app; never resize PNGs.

    An explicit size needs density too: 1080 physical px at emulator-default 160dpi
    would test a 1080dp tablet layout, not the owner's phone.
    """
    if bool(size) != bool(density):
        raise ValueError("Display size and density must be supplied together")
    if size and not re.fullmatch(r"[1-9]\d{2,3}x[1-9]\d{2,3}", size):
        raise ValueError("Display size must be portrait WIDTHxHEIGHT in pixels")
    if density and (not density.isdigit() or not 120 <= int(density) <= 1000):
        raise ValueError("Display density must be a DPI value between 120 and 1000")
    if font_scale and (not re.fullmatch(r"\d+(?:\.\d+)?", font_scale) or
                       not .5 <= float(font_scale) <= 3):
        raise ValueError("Font scale must be a finite Android scale between 0.5 and 3")
    if adb("shell", "getprop", "ro.kernel.qemu").decode().strip() != "1":
        raise RuntimeError("Native UI setup is restricted to the disposable emulator")
    # This unrelated launcher raised ANRs over the app in both owner-profile and
    # default compact CI. Isolate it independently of optional display overrides.
    # App ANRs, focus checks and instrumentation failures remain fail-closed.
    launcher_state = adb("shell", "pm", "disable-user", "--user", "0",
                         "com.google.android.apps.nexuslauncher", check=False).decode().strip()
    if size:
        adb("shell", "wm", "size", size)
        adb("shell", "wm", "density", density)
    if font_scale:
        adb("shell", "settings", "put", "system", "font_scale", font_scale)
    actual_size = adb("shell", "wm", "size").decode().strip()
    actual_density = adb("shell", "wm", "density").decode().strip()
    actual_font = adb("shell", "settings", "get", "system", "font_scale").decode().strip()
    if size:
        sizes = re.findall(r"\d+x\d+", actual_size)
        densities = re.findall(r"\d+", actual_density)
        if not sizes or sizes[-1] != size or not densities or int(densities[-1]) != int(density):
            raise RuntimeError(f"Emulator did not apply {size}@{density}dpi: {actual_size!r}; {actual_density!r}")
    if font_scale and float(actual_font) != float(font_scale):
        raise RuntimeError("Emulator did not apply the requested font scale")
    return {"requestedSizePx": size or None, "requestedDensityDpi": int(density) if density else None,
            "wmSize": actual_size, "wmDensity": actual_density, "fontScale": actual_font,
            "ciLauncherIsolation": launcher_state}


def screenshot_dimensions(png, expected_size=""):
    if not png.startswith(b"\x89PNG\r\n\x1a\n") or len(png) < 24 or png[12:16] != b"IHDR":
        raise ValueError("Screenshot has no valid PNG dimensions")
    width, height = struct.unpack(">II", png[16:24])
    if not width or not height or expected_size and f"{width}x{height}" != expected_size:
        raise ValueError("Screenshot dimensions differ from the requested emulator profile")
    return width, height


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
    parser.add_argument("--display-size", default="")
    parser.add_argument("--display-density", default="")
    parser.add_argument("--font-scale", default="")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    (output / "screenshots").mkdir(exist_ok=True)
    manifest = {"display": {"requestedSizePx": args.display_size or None,
                            "requestedDensityDpi": args.display_density or None,
                            "requestedFontScale": args.font_scale or None}, "commit": args.commit, "appSha256": digest(args.app), "testSha256": digest(args.tests),
                "imageOrigin": "unmodified-adb-exec-out-screencap-p", "data": "isolated-emulator-test-fixtures",
                "device": adb("shell", "getprop", "ro.product.model").decode().strip(),
                "result": "running", "api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(), "screenshots": []}
    process = None
    try:
        manifest["display"] = configure_display(args.display_size, args.display_density, args.font_scale)
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
                            width, height = screenshot_dimensions(png, args.display_size)
                            file = output / "screenshots" / (name + ".png")
                            file.write_bytes(png)
                            manifest["screenshots"].append({**request, "file": "screenshots/" + file.name,
                                                            "sha256": digest(file), "bytes": len(png), "focus": focus,
                                                            "widthPx": width, "heightPx": height})
                            seen.add(name)
                            adb("shell", "printf '%s' '" + name + "' > /data/local/tmp/trainpilot-shot-ack")
                    time.sleep(.15)
            raw = (output / filename).read_text(errors="replace")
            valid = report(raw, output / ("instrumentation-derived.xml" if not mode else filename + ".xml"))
            if not mode and not valid:
                # The full 94-case suite can take long enough to rotate the short
                # global logcat tail. Preserve focused diagnostics without modifying
                # instrumentation or converting failed outcomes to success.
                (output / "native-mode-diagnostics.txt").write_bytes(
                    adb("logcat", "-d", "-v", "brief", "-s", "NativeModeApproval:*", "NativeLibraryEdit:*",
                        "NativeNavigation:*", "NativeIntervalPanel:*", check=False, timeout=30))
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
