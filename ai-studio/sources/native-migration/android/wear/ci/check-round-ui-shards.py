#!/usr/bin/env python3
"""Fail closed unless every round-emulator shard contributes distinct passing JUnit cases."""
import os
import sys
from pathlib import Path
from xml.etree import ElementTree as ET


def verify(root: Path, shard_count: int, min_total: int) -> int:
    if shard_count < 1 or min_total < 1:
        raise ValueError("Expected positive shard count and test minimum")
    if not root.is_dir():
        raise ValueError(f"Artifact directory missing: {root}")
    folders = sorted(p for p in root.iterdir() if p.is_dir())
    expected = {f"shard-{i}" for i in range(shard_count)}
    mapped = {}
    for folder in folders:
        suffix = folder.name.rsplit("-shard-", 1)
        if len(suffix) != 2 or not suffix[1].isdigit():
            raise ValueError(f"Unexpected shard artifact: {folder.name}")
        key = f"shard-{int(suffix[1])}"
        if key in mapped:
            raise ValueError(f"Duplicate artifact for {key}")
        mapped[key] = folder
    if set(mapped) != expected:
        raise ValueError(f"Missing/unexpected shards: expected {expected}, got {set(mapped)}")

    identities = set()
    total = 0
    for index in range(shard_count):
        shard = mapped[f"shard-{index}"]
        # Gradle connectedDebugAndroidTest stores TEST-*.xml in outputs,
        # not the HTML reports tree and not the screenshot evidence directory.
        reports = sorted((shard / "android/wear/build/outputs/androidTest-results/connected").rglob("TEST-*.xml"))
        if not reports:
            raise ValueError(f"Shard {index} has no JUnit XML")
        shard_total = 0
        for report in reports:
            suite = ET.parse(report).getroot()
            cases = list(suite.iter("testcase"))
            declared = int(suite.get("tests", len(cases)))
            if declared != len(cases):
                raise ValueError(f"{report}: declared {declared} tests but has {len(cases)} test cases")
            for case in cases:
                if any(case.find(status) is not None for status in ("failure", "error", "skipped")):
                    raise ValueError(f"Non-passing case: {case.get('classname')}::{case.get('name')}")
                identity = (case.get("classname"), case.get("name"))
                if not all(identity):
                    raise ValueError(f"Missing test identity in {report}")
                if identity in identities:
                    raise ValueError(f"Duplicated test across shards: {identity}")
                identities.add(identity)
            shard_total += declared
        if shard_total == 0:
            raise ValueError(f"Shard {index} selected zero tests")
        print(f"Shard {index}/{shard_count}: {shard_total} unique passing tests", flush=True)
        total += shard_total
    if total < min_total:
        raise ValueError(f"Insufficient coverage: {total} tests, expected at least {min_total}")
    print(f"ROUND UI PASS: {total} unique tests across {shard_count} emulator(s)", flush=True)
    return total


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: check-round-ui-shards.py <download-artifact-root>")
    try:
        verify(Path(sys.argv[1]), int(os.environ["TRAINPILOT_WEAR_UI_SHARD_COUNT"]),
               int(os.environ["TRAINPILOT_WEAR_UI_MIN_TOTAL"]))
    except (ValueError, OSError, ET.ParseError) as error:
        sys.exit(f"ROUND UI FAIL: {error}")
