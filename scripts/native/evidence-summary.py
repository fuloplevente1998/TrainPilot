#!/usr/bin/env python3
"""Offline, fail-closed summary of extracted TrainPilot native Actions evidence."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zlib

SHA = re.compile(r"[0-9a-f]{40}\Z")
HASH = re.compile(r"[0-9a-f]{64}\Z")
WEAR = re.compile(r"TrainPilot-Native-Wear-UI-Results-([0-9a-f]{40})-(\d+)\Z")
JVM = re.compile(r"TrainPilot-Native-Tests-([0-9a-f]{40})\Z")
CODE = re.compile(r"^INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)\s*$")
SUMMARY = re.compile(r"\bOK\s*\((\d+)\s+tests?\)")
SAFE_CASE = re.compile(r"[A-Za-z0-9_.$\[\]-]+\Z")
STATUS_RANK = {"PASS": 0, "UNKNOWN": 1, "INCOMPLETE": 2, "FAILED": 3}


def status(*values):
    return max(values, key=lambda s: STATUS_RANK[s]) if values else "UNKNOWN"


def issue(issues, kind, detail):
    issues.append({"kind": kind, "detail": detail})


def relative(path, roots):
    """A bounded, non-sensitive evidence locator, never an absolute user path."""
    for index, root in enumerate(roots, 1):
        try:
            parts = path.resolve().relative_to(root.resolve()).parts
        except ValueError:
            continue
        if any(not re.fullmatch(r"[A-Za-z0-9_.-]{1,100}", part) for part in parts):
            return f"input-{index}/[redacted-path]"
        return (f"input-{index}/" + "/".join(parts))[:240]
    return "[external-file]"


def safe_case(value):
    return value[:160] if SAFE_CASE.fullmatch(value) and len(value) <= 160 else "[redacted-case]"


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def png_ok(path):
    """Bounded structural PNG/scanline validation; no image renderer or new deps."""
    try:
        if path.stat().st_size > 32 * 1024 * 1024:
            return False
        data = path.read_bytes()
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            return False
        pos, seen, payload, header, idat_ended, palette = 8, [], bytearray(), None, False, False
        while pos + 12 <= len(data):
            count = struct.unpack_from(">I", data, pos)[0]
            if count > 32 * 1024 * 1024 or pos + 12 + count > len(data):
                return False
            kind = data[pos + 4:pos + 8]
            chunk = data[pos + 8:pos + 8 + count]
            crc = struct.unpack_from(">I", data, pos + 8 + count)[0]
            if not re.fullmatch(rb"[A-Za-z]{4}", kind) or zlib.crc32(kind + chunk) & 0xffffffff != crc:
                return False
            if not seen and (kind != b"IHDR" or count != 13):
                return False
            if kind == b"IHDR":
                if seen or count != 13:
                    return False
                width, height, depth, colour, compression, filtering, interlace = struct.unpack(">IIBBBBB", chunk)
                legal = {0: (1, 2, 4, 8, 16), 2: (8, 16), 3: (1, 2, 4, 8),
                         4: (8, 16), 6: (8, 16)}
                if (not width or not height or depth not in legal.get(colour, ()) or
                        compression or filtering or interlace or width * height > 16_000_000):
                    return False  # unsupported interlaced images are conservatively rejected
                channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
                stride = (width * channels * depth + 7) // 8
                expected = height * (stride + 1)
                if expected > 64 * 1024 * 1024:
                    return False
                header = (height, stride, expected, colour)
            elif kind == b"PLTE":
                if palette or b"IDAT" in seen or count == 0 or count > 768 or count % 3:
                    return False
                palette = True
            elif kind == b"IDAT":
                if idat_ended or (header and header[3] == 3 and not palette):
                    return False
                payload.extend(chunk)
                if len(payload) > 32 * 1024 * 1024:
                    return False
            elif kind == b"IEND":
                return (count == 0 and pos + 12 == len(data) and b"IDAT" in seen and
                        bool(payload) and _inflate_ok(payload, *header[:3]))
            elif kind[0] & 0x20 == 0:  # unknown critical chunk
                return False
            if kind != b"IDAT" and b"IDAT" in seen:
                idat_ended = True
            seen.append(kind)
            pos += 12 + count
        return False
    except (OSError, ValueError, struct.error, zlib.error):
        return False


def _inflate_ok(payload, rows, stride, expected):
    decoder = zlib.decompressobj()
    expanded = bytearray()
    for offset in range(0, len(payload), 65536):
        part = payload[offset:offset + 65536]
        while part:
            data = decoder.decompress(part, expected - len(expanded) + 1)
            expanded.extend(data)
            if len(expanded) > expected:
                return False
            part = decoder.unconsumed_tail
    if not decoder.eof or decoder.unused_data or len(expanded) != expected:
        return False
    return all(expanded[index * (stride + 1)] <= 4 for index in range(rows))


def instrument(path, roots):
    raw = path.read_text(encoding="utf-8")  # malformed UTF-8 must not yield PASS
    declared, cases, started, failures, problems = set(), {}, set(), [], []
    current, final_codes = {}, []
    for line in raw.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: "):
            key, sep, value = line.removeprefix("INSTRUMENTATION_STATUS: ").partition("=")
            if sep:
                current[key] = value
                if key == "numtests":
                    if value.isdigit() and int(value) > 0:
                        declared.add(int(value))
                    else:
                        issue(problems, "invalid-declaration", "Invalid numtests")
        elif CODE.match(line):
            code = int(CODE.match(line).group(1))
            cls, method = current.get("class", ""), current.get("test", "")
            if not cls or not method:
                issue(problems, "missing-identity", "Instrumentation event without class/method")
            else:
                identity = (cls, method)
                if code == 1:
                    if identity in started or identity in cases:
                        issue(problems, "duplicate-start", "Repeated instrumentation case")
                    started.add(identity)
                else:
                    if identity in cases:
                        issue(problems, "duplicate-terminal", "Repeated terminal case")
                    cases[identity] = code
                    if code != 0:
                        kind = "failure" if code == -2 else "skip" if code in (-3, -4) else "error"
                        # Exception/stack message is deliberately not echoed (may contain secrets).
                        failures.append({"case": safe_case(cls) + "#" + safe_case(method),
                                         "kind": kind, "excerpt": "instrumentation-status-" + str(code),
                                         "raw": relative(path, roots)})
            current = {}
        elif line.startswith("INSTRUMENTATION_CODE: "):
            try:
                final_codes.append(int(line.partition(":")[2].strip()))
            except ValueError:
                issue(problems, "invalid-final-code", "Invalid runner final code")
    declared_count = next(iter(declared)) if len(declared) == 1 else None
    if len(declared) != 1:
        issue(problems, "declaration-conflict", "Missing or inconsistent numtests")
    total = len(cases)
    successes = sum(v == 0 for v in cases.values())
    failed = sum(v == -2 for v in cases.values())
    skipped = sum(v in (-3, -4) for v in cases.values())
    errors = total - successes - failed - skipped
    missing = max(0, declared_count - total) if declared_count is not None else None
    crash = bool(re.search(r"INSTRUMENTATION_RESULT:\s*shortMsg=|INSTRUMENTATION_FAILED|Process crashed|FATAL EXCEPTION", raw, re.I))
    failed_summary = bool(re.search(r"(?m)^FAILURES!!!\s*$|^Tests run:\s*\d+,\s*Failures:\s*[1-9]\d*", raw))
    # The runner's summary must be in the final INSTRUMENTATION_RESULT block,
    # not a forged 'OK' appearing inside a testcase stack or arbitrary record.
    summaries = re.findall(r"(?m)^INSTRUMENTATION_RESULT: stream=(?:[^\n]*\n)(?:[\s\S]*?)^OK \((\d+) tests?\)\s*$", raw)
    inline = re.findall(r"(?m)^INSTRUMENTATION_RESULT: stream=OK \((\d+) tests?\)\s*$", raw)
    summaries.extend(inline)
    stream_headers = len(re.findall(r"(?m)^INSTRUMENTATION_RESULT: stream=", raw))
    consistent = (not problems and stream_headers == 1 and not (started - cases.keys()) and
                  started == set(cases))
    complete = (declared_count is not None and total == declared_count and total > 0 and
                consistent and final_codes == [-1] and len(summaries) == 1 and
                int(summaries[0]) == total and not crash and not failed_summary and
                not failed and not errors and not skipped)
    if crash:
        issue(problems, "process-crash", "Instrumentation process crashed")
    if failed_summary:
        issue(problems, "runner-failures", "Runner reports a failed execution")
    if declared_count is not None and total > declared_count:
        issue(problems, "over-count", "More unique terminal cases than declared")
    if not complete:
        issue(problems, "incomplete-run", "Runner lacks consistent complete terminal summary")
    contradiction = {"duplicate-terminal", "duplicate-start", "missing-identity", "invalid-declaration",
                     "declaration-conflict", "invalid-final-code", "over-count"}
    result_status = ("FAILED" if failed or errors or crash or failed_summary or
                     any(x["kind"] in contradiction for x in problems) or any(c != -1 for c in final_codes)
                     or len(final_codes) > 1 or stream_headers > 1
                     else "INCOMPLETE" if not complete or skipped else "PASS")
    return {"status": result_status, "declared": declared_count, "executed": total,
            "passed": successes, "failed": failed, "errors": errors, "skipped": skipped,
            "missing": missing, "crashed": crash, "complete": complete,
            "case_ids": set(cases), "failure_excerpts": failures[:25], "issues": problems,
            "raw": relative(path, roots)}


def jvm_summary(directory, roots):
    xmls = sorted(p for p in directory.rglob("TEST-*.xml") if p.is_file() and "test-results" in p.parts)
    totals = {"app": [0, 0, 0, 0], "wear": [0, 0, 0, 0], "shared": [0, 0, 0, 0]}
    problems, seen = [], set()
    for path in xmls:
        module = next((m for m in totals if m in path.relative_to(directory).parts), None)
        if not module:
            issue(problems, "unknown-module", "Unrecognized JVM module")
            continue
        try:
            if path.stat().st_size > 32 * 1024 * 1024:
                raise ValueError("Oversized JVM XML")
            root = ET.parse(path).getroot()
            if root.tag not in ("testsuite", "testsuites"):
                raise ValueError("Unexpected JVM XML root")
            suites = [root] if root.tag == "testsuite" else list(root)
            if not suites or any(suite.tag != "testsuite" for suite in suites):
                raise ValueError("Unexpected XML suites collection")
            file_counts = [0, 0, 0, 0]
            for suite in suites:
                if suite.findall("testsuite"):
                    raise ValueError("Nested JVM suites are ambiguous")
                cases = suite.findall("testcase")
                counts = [len(cases), 0, 0, 0]
                for case in cases:
                    key = (module, case.get("classname"), case.get("name"))
                    if not all(key[1:]) or key in seen:
                        raise ValueError("Duplicate or anonymous JVM testcase")
                    seen.add(key)
                    outcomes = [child.tag for child in case if child.tag in ("failure", "error", "skipped")]
                    if len(outcomes) > 1:
                        raise ValueError("Multiple outcomes for one JVM testcase")
                    if outcomes:
                        counts[{"failure": 1, "error": 2, "skipped": 3}[outcomes[0]]] += 1
                attr = [int(suite.attrib[k]) for k in ("tests", "failures", "errors", "skipped")]
                if counts != attr or min(attr) < 0 or sum(attr[1:]) > attr[0]:
                    raise ValueError("JVM suite totals contradict testcases")
                file_counts = [a + b for a, b in zip(file_counts, counts)]
            if root.tag == "testsuites" and any(k in root.attrib for k in ("tests", "failures", "errors", "skipped")):
                outer = [int(root.attrib[k]) for k in ("tests", "failures", "errors", "skipped")]
                if outer != file_counts:
                    raise ValueError("JVM root totals contradict child suites")
            totals[module] = [a + b for a, b in zip(totals[module], file_counts)]
        except (ET.ParseError, ValueError, KeyError, UnicodeError, OSError):
            issue(problems, "malformed-xml", "Invalid JVM XML at " + relative(path, roots))
    n = sum(v[0] for v in totals.values())
    failures = sum(v[1] + v[2] for v in totals.values())
    skipped = sum(v[3] for v in totals.values())
    return {"status": "FAILED" if problems or failures else "INCOMPLETE" if skipped or not n else "PASS",
            "modules": dict(zip(totals, (dict(zip(("tests", "failures", "errors", "skips"), v)) for v in totals.values()))),
            "tests": n, "failures": failures, "skips": skipped,
            "xml_files": len(xmls), "issues": problems}


def find_sources(roots):
    phone, jvm, wear, meta = [], [], [], []
    for root in roots:
        dirs = [root] + [p for p in root.rglob("*") if p.is_dir() and not p.is_symlink()]
        for folder in dirs:
            # Recognize partially downloaded artifacts instead of silently ignoring them.
            if folder.name == "TrainPilot-Native-Phone-UI-Evidence" or (folder / "manifest.json").is_file():
                phone.append(folder)
            if folder.name == "TrainPilot-Native-Update-Verification" or (folder / "native-apk-sha256.txt").is_file():
                meta.append(folder)
            if JVM.fullmatch(folder.name):
                jvm.append(folder)
            if WEAR.fullmatch(folder.name):
                wear.append(folder)
    return phone, jvm, wear, meta


def analyse_phone(folder, args, roots):
    problems = []
    try:
        manifest = json.loads((folder / "manifest.json").read_text(encoding="utf-8"))
        if not isinstance(manifest, dict):
            raise ValueError("manifest is not an object")
    except (OSError, json.JSONDecodeError, ValueError):
        return {"status": "FAILED", "issues": [{"kind": "invalid-manifest", "detail": "Cannot parse phone manifest"}]}
    try:
        run = instrument(folder / "instrumentation.txt", roots)
    except (OSError, UnicodeError):
        return {"status": "FAILED", "issues": [{"kind": "invalid-instrumentation", "detail": "Missing or invalid phone log"}]}
    sha = manifest.get("commit")
    if sha != args.expected_commit:
        issue(problems, "mixed-source", "Phone manifest commit does not match expected commit")
    for key, filename in (("appSha256", args.phone_apk), ("testSha256", args.phone_test_apk)):
        value = manifest.get(key)
        if not isinstance(value, str) or not HASH.fullmatch(value):
            issue(problems, "bad-apk-hash", "Missing/invalid " + key)
        elif filename is not None:
            if not filename.is_file() or sha256(filename) != value:
                issue(problems, "apk-hash-mismatch", "APK bytes do not match " + key)
        else:
            issue(problems, "unverified-apk", "No local APK bytes provided for " + key)
    screenshots = manifest.get("screenshots")
    if not isinstance(screenshots, list):
        issue(problems, "invalid-screenshots", "Screenshot list is missing")
        screenshots = []
    shot_names = set()
    for entry in screenshots:
        if not isinstance(entry, dict) or not isinstance(entry.get("file"), str):
            issue(problems, "invalid-screenshot-entry", "Malformed screenshot entry")
            continue
        rel = Path(entry["file"])
        target = (folder / rel).resolve()
        if rel.is_absolute() or not target.is_relative_to(folder.resolve()) or target.suffix.lower() != ".png":
            issue(problems, "unsafe-screenshot-path", "Screenshot path escapes evidence root")
            continue
        if (entry.get("name") != rel.stem or not re.fullmatch(r"[a-z0-9_]{1,120}", rel.stem) or
                len(rel.parts) != 2 or rel.parts[0] != "screenshots"):
            issue(problems, "invalid-screenshot-entry", "Screenshot name/path mismatch")
        if entry["file"] in shot_names:
            issue(problems, "duplicate-screenshot", "Repeated screenshot manifest entry")
        shot_names.add(entry["file"])
        if not target.is_file() or not png_ok(target):
            issue(problems, "bad-png", "Missing or invalid screenshot PNG")
        elif not isinstance(entry.get("sha256"), str) or not HASH.fullmatch(entry["sha256"]) or sha256(target) != entry["sha256"]:
            issue(problems, "png-hash-mismatch", "Screenshot hash mismatch")
        elif entry.get("bytes") != target.stat().st_size:
            issue(problems, "png-size-mismatch", "Screenshot size mismatch")
    for needed in args.require_screenshot:
        if not re.fullmatch(r"[a-z0-9_]{1,120}", needed) or "screenshots/" + needed + ".png" not in shot_names:
            issue(problems, "missing-screenshot", "Required screenshot not present")
    if not screenshots:
        issue(problems, "missing-screenshot", "Phone evidence has zero screenshots")
    if manifest.get("result") != "success" or manifest.get("imageOrigin") != "unmodified-adb-exec-out-screencap-p":
        issue(problems, "manifest-result", "Phone manifest does not assert successful raw capture")
    hard = {"mixed-source", "bad-apk-hash", "apk-hash-mismatch", "invalid-screenshot-entry", "unsafe-screenshot-path",
            "duplicate-screenshot", "bad-png", "png-hash-mismatch", "png-size-mismatch", "invalid-screenshots"}
    merged = status(run["status"], "FAILED" if any(x["kind"] in hard for x in problems) else
                    "INCOMPLETE" if problems else "PASS")
    return {**{k: v for k, v in run.items() if k != "case_ids"}, "status": merged,
            "commit": sha, "screenshots": len(screenshots), "issues": run["issues"] + problems}


def analyse_wear(folders, args, roots):
    problems, shards, all_cases = [], [], set()
    indices = set()
    for folder in folders:
        sha, index = WEAR.fullmatch(folder.name).groups()
        index = int(index)
        if sha != args.expected_commit:
            issue(problems, "mixed-source", "Wear artifact SHA does not match expected commit")
        if index in indices or index >= args.wear_shards:
            issue(problems, "duplicate-or-invalid-shard", "Duplicate or out-of-range Wear shard")
        indices.add(index)
        try:
            result = instrument(folder / "instrumentation.txt", roots)
        except (OSError, UnicodeError):
            result = {"status": "FAILED", "declared": None, "executed": 0, "passed": 0,
                      "failed": 0, "errors": 0, "skipped": 0, "missing": None,
                      "case_ids": set(), "issues": [{"kind": "invalid-instrumentation", "detail": "Missing or invalid Wear log"}],
                      "raw": relative(folder / "instrumentation.txt", roots)}
        repeated = all_cases & result["case_ids"]
        if repeated:
            issue(problems, "duplicate-case", "Case appears in more than one Wear shard")
        all_cases.update(result["case_ids"])
        pictures = list(folder.rglob("*.png"))
        for picture in pictures:
            if not png_ok(picture):
                issue(problems, "bad-png", "Invalid Wear PNG evidence")
        shards.append({"index": index, "commit": sha, "screenshots": len(pictures),
                       **{k: v for k, v in result.items() if k != "case_ids"}})
    shards.sort(key=lambda x: x["index"])
    if indices != set(range(args.wear_shards)):
        issue(problems, "missing-shard", "Wear shard set is not complete")
    passes = sum(x["passed"] for x in shards)
    executed = sum(x["executed"] for x in shards)
    declared = sum(x["declared"] or 0 for x in shards)
    if args.wear_total is not None and (executed != args.wear_total or len(all_cases) != args.wear_total):
        issue(problems, "global-count", "Wear global count differs from required total")
    hard = {"mixed-source", "duplicate-or-invalid-shard", "duplicate-case", "bad-png", "global-count"}
    grade = status(*(x["status"] for x in shards), "FAILED" if any(x["kind"] in hard for x in problems)
                   else "INCOMPLETE" if problems else "PASS")
    return {"status": grade, "expected_shards": args.wear_shards, "shards": shards,
            "declared": declared, "executed": executed, "passed": passes,
            "unique_cases": len(all_cases), "issues": problems}


def analyse_apk(folder, args, roots):
    problems, records = [], {}
    try:
        text = (folder / "native-apk-sha256.txt").read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        return {"status": "FAILED", "records": 0, "verified": 0,
                "issues": [{"kind": "invalid-apk-metadata", "detail": "Missing or invalid APK checksum file"}]}
    for line in text.splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})\s+(.+\.apk)", line)
        if not match or match.group(2) in records:
            issue(problems, "bad-hash-record", "Malformed or duplicate APK checksum record")
            continue
        records[match.group(2)] = match.group(1)
    for logical, path in args.apk_file.items():
        if logical not in records or not path.is_file() or sha256(path) != records[logical]:
            issue(problems, "apk-hash-mismatch", "APK verification hash mismatch")
    if not records:
        issue(problems, "missing-hashes", "No APK checksum records")
    if not args.apk_file or set(records) - set(args.apk_file):
        issue(problems, "unverified-apk", "Not every APK checksum record has independently checked bytes")
    return {"status": "FAILED" if any(i["kind"] in ("bad-hash-record", "apk-hash-mismatch") for i in problems)
            else "INCOMPLETE" if problems else "PASS", "records": len(records), "verified": len(args.apk_file),
            "issues": problems, "raw": relative(folder / "native-apk-sha256.txt", roots)}


def summarize(args):
    roots = [p.resolve() for p in args.roots]
    for path in roots:
        if not path.is_dir():
            raise ValueError("Input is not an extracted artifact directory")
    phone_dirs, jvm_dirs, wear_dirs, apk_dirs = find_sources(roots)
    gates = {}
    if phone_dirs:
        gates["phone"] = analyse_phone(phone_dirs[0], args, roots) if len(phone_dirs) == 1 else {
            "status": "FAILED", "issues": [{"kind": "duplicate-phone", "detail": "Multiple phone evidence manifests"}]}
    if jvm_dirs:
        gates["jvm"] = jvm_summary(jvm_dirs[0], roots) if len(jvm_dirs) == 1 else {
            "status": "FAILED", "issues": [{"kind": "duplicate-jvm", "detail": "Multiple JVM artifacts"}]}
        if len(jvm_dirs) == 1 and JVM.fullmatch(jvm_dirs[0].name).group(1) != args.expected_commit:
            gates["jvm"]["status"] = "FAILED"
            issue(gates["jvm"]["issues"], "mixed-source", "JVM artifact SHA does not match expected commit")
    if wear_dirs:
        gates["wear"] = analyse_wear(wear_dirs, args, roots)
    if apk_dirs:
        gates["apk"] = analyse_apk(apk_dirs[0], args, roots) if len(apk_dirs) == 1 else {
            "status": "FAILED", "issues": [{"kind": "duplicate-apk-meta", "detail": "Multiple checksum artifacts"}]}
    required = set(args.require_gate)
    if args.require_screenshot or args.phone_apk or args.phone_test_apk:
        required.add("phone")
    if args.wear_total is not None:
        required.add("wear")
    if args.apk_file:
        required.add("apk")
    for gate in required:
        if gate not in gates:
            gates[gate] = {"status": "INCOMPLETE", "issues": [{"kind": "missing-gate", "detail": "Required evidence absent"}]}
    overall = status(*(value["status"] for value in gates.values()))
    if not gates:
        overall = "UNKNOWN"
    return {"schema_version": 1, "expected_commit": args.expected_commit,
            "status": overall, "complete": overall == "PASS",
            "gates": gates,
            "limitations": ["No network or runner execution; result covers only supplied extracted artifacts.",
                            "Wear/JVM commit attribution comes from artifact directory names, not in-file cryptographic proof.",
                            "Missing independent APK bytes leave hash attestations incomplete.",
                            "Screenshots are integrity-checked PNG bytes; origin cannot be proven cryptographically."]}


def markdown(report):
    lines = ["# Native CI evidence summary", "", "Expected commit: `" + report["expected_commit"] + "`",
             "Overall: **" + report["status"] + "**", "", "| Gate | Status | Counts |", "| --- | --- | --- |"]
    for name, value in sorted(report["gates"].items()):
        if name == "jvm":
            counts = ", ".join(f"{m} {v['tests']} / {v['failures']} fail / {v['errors']} error / {v['skips']} skip"
                               for m, v in value.get("modules", {}).items())
        elif name == "wear" and "shards" in value:
            counts = f"{value['passed']}/{value['executed']} pass; {value['unique_cases']} unique; {len(value['shards'])}/{value['expected_shards']} shards"
        elif name == "phone" and "executed" in value:
            counts = f"{value['passed']} pass, {value['failed']} fail, {value['errors']} error, {value['skipped']} skip; {value['executed']}/{value['declared']} terminal, {value['missing']} missing; {value['screenshots']} shots"
        else:
            counts = f"{value.get('verified', 0)}/{value.get('records', 0)} APK hashes verified" if name == "apk" else "No evidence"
        lines.append(f"| {name} | {value['status']} | {counts} |")
    lines += ["", "## Evidence issues"]
    for name, value in sorted(report["gates"].items()):
        for item in value.get("issues", [])[:12]:
            lines.append(f"- {name}: {item['kind']} — {item['detail']}")
        for item in value.get("failure_excerpts", [])[:6]:
            lines.append(f"- {name}: {item['kind']} {item['case']}: {item['excerpt']} (raw: `{item['raw']}`)")
    lines += ["", "No runtime test has been run by this summary tool; statuses describe input artifacts only.", ""]
    return "\n".join(lines)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="+", type=Path, help="Extracted, unmodified artifact directories")
    parser.add_argument("--expected-commit", required=True, help="Full 40-character SHA")
    parser.add_argument("--require-gate", action="append", default=[], choices=("phone", "wear", "jvm", "apk"))
    parser.add_argument("--wear-shards", type=int, default=3)
    parser.add_argument("--wear-total", type=int, default=None, help="Required unique/executed Wear count, e.g. 183")
    parser.add_argument("--require-screenshot", action="append", default=[], help="Phone screenshot checkpoint, repeatable")
    parser.add_argument("--phone-apk", type=Path, help="Independent app-debug.apk path to compare to phone manifest")
    parser.add_argument("--phone-test-apk", type=Path, help="Independent androidTest APK path to compare")
    parser.add_argument("--apk-file", action="append", default=[], metavar="ARTIFACT_PATH=LOCAL_APK")
    parser.add_argument("--json-out", type=Path, required=True)
    parser.add_argument("--markdown-out", type=Path, required=True)
    args = parser.parse_args(argv)
    if not SHA.fullmatch(args.expected_commit) or args.wear_shards < 1 or args.wear_total is not None and args.wear_total < 1:
        parser.error("Expected full lowercase SHA and positive Wear constraints")
    mappings = {}
    for entry in args.apk_file:
        key, sep, value = entry.partition("=")
        if not sep or not key.endswith(".apk") or key in mappings or not value:
            parser.error("--apk-file needs unique artifact-relative.apk=local.apk")
        mappings[key] = Path(value)
    args.apk_file = mappings
    if args.json_out.resolve() == args.markdown_out.resolve():
        parser.error("JSON and Markdown destinations must differ")
    if any(not re.fullmatch(r"[a-z0-9_]{1,120}", name) for name in args.require_screenshot):
        parser.error("Invalid required screenshot checkpoint")
    try:
        report = summarize(args)
        for destination in (args.json_out, args.markdown_out):
            if any(destination.resolve().is_relative_to(p.resolve()) for p in args.roots):
                raise ValueError("Output may not modify any input artifact directory")
        for target, contents in ((args.json_out, json.dumps(report, ensure_ascii=False, indent=2) + "\n"),
                                 (args.markdown_out, markdown(report))):
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(contents, encoding="utf-8")
    except (OSError, ValueError, UnicodeError):
        parser.error("Cannot process evidence or write reports; check inputs and permissions")
    print(f"{report['status']}: " + ", ".join(f"{name}={v['status']}" for name, v in report["gates"].items()))
    return 0 if report["status"] == "PASS" else 1 if report["status"] == "FAILED" else 2


if __name__ == "__main__":
    sys.exit(main())
