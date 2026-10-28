#!/usr/bin/env python3
"""Fail-closed source-level guard against accidentally installing the AI Studio demo
over the canonical native TrainPilot app and its migration safety gates.

This is a cheap PR tripwire, not a substitute for runtime migration, signed upgrade,
data integrity or real Studio UI parity tests.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
app = ROOT / "android/app"
errors = []


def fail(message):
    errors.append(message)


def must_contain(text, pattern, name):
    if not re.search(pattern, text, re.DOTALL):
        fail(f"{name}: required native guard missing")


def main():
    gradle = (app / "build.gradle.kts").read_text(encoding="utf-8")
    must_contain(gradle, r'applicationId\s*=\s*"com\.repforge"', "Production applicationId")
    if "com.aistudio.trainpilot.tpux01" in gradle:
        fail("Studio sandbox applicationId is not an install/update-compatible production identity")

    manifest = ET.parse(app / "src/main/AndroidManifest.xml").getroot()
    ns = "{http://schemas.android.com/apk/res/android}"
    launchers = []
    for activity in manifest.iter("activity"):
        for intent in activity.findall("./intent-filter"):
            actions = {e.get(ns + "name") for e in intent.findall("action")}
            categories = {e.get(ns + "name") for e in intent.findall("category")}
            if "android.intent.action.MAIN" in actions and "android.intent.category.LAUNCHER" in categories:
                launchers.append(activity.get(ns + "name"))
    if launchers != [".MainActivity"]:
        fail(f"Production launcher unexpectedly changed: {launchers}")

    reader = (app / "src/main/java/com/repforge/app/data/migration/"
              "LegacyLocalStorageReader.kt").read_text(encoding="utf-8")
    must_contain(reader, r"withTimeout\(60_000\)", "Legacy read 60-second guard")
    must_contain(reader, r"suspendCancellableCoroutine", "Cancellable legacy WebView read")
    must_contain(reader, r"finally\s*\{[\s\S]*?view\.destroy\(\)", "Legacy WebView cleanup")
    if re.search(r"catch\s*\(\s*[^)]*Throwable", reader):
        fail("Legacy localStorage reader MUST propagate failures and coroutine cancellation")
    if "emptyMap()" in reader:
        fail("Legacy localStorage reader MUST NOT convert errors into an empty dataset")

    migrator = (app / "src/main/java/com/repforge/app/data/migration/"
                "LegacyWebViewMigrator.kt").read_text(encoding="utf-8")
    must_contain(migrator, r"markMigrationComplete\(\)", "Migration completion marker")
    must_contain(migrator, r"extractAllLegacyKeys\(\)", "Migration source extraction")

    activity = (app / "src/main/java/com/repforge/app/MainActivity.kt").read_text(encoding="utf-8")
    must_contain(activity,
                 r"if\s*\(recoveryNeeded\)[\s\S]*?else\s+if\s*\(migrationReady\)",
                 "Migration-ready startup gate")
    must_contain(activity, r"migrationFailure", "Migration failure visibility")
    must_contain(activity, r"onClick\s*=\s*\{\s*startMigration\(\)\s*\}",
                 "Explicit user retry after migration failure")
    must_contain(activity, r"catch\s*\(cancelled:\s*CancellationException\)\s*\{\s*throw cancelled",
                 "Startup coroutine cancellation")
    # Studio can own the visuals *inside* the migrationReady branch in the future,
    # but never as an unconditional else after recovery.
    ready = re.search(r"else\s+if\s*\(migrationReady\)\s*\{", activity)
    if ready and "TrainPilotAppScaffold(" not in activity[ready.end():]:
        if "TrainPilotAppRoot(" not in activity[ready.end():]:
            fail("App shell must be rendered only after migrationReady")

    if errors:
        for msg in errors:
            print("FAIL", msg, file=sys.stderr)
        return 1
    print("PASS: native TrainPilot app identity, 60-second propagating migration reader,"
          " recovery gate, migration retry and cancellation semantics retained.")
    print("NOTE: source tripwire only; complete signed-upgrade and migration tests still required.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
