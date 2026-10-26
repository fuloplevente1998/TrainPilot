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
    for root in roots:
        try:
            p = path.resolve().relative_to(root.resolve())
            return (root.name + "/" + p.as_posix())[:240]
        except ValueError:
            pass
    return path.name[:120]


def safe_case(value):
    return value[:160] if SAFE_CASE.fullmatch(value) and len(value) <= 160 else "[redacted-case]"


def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def png_ok(path):
    """Validate PNG chunk order/CRC, zlib stream and terminal IEND (no Pillow)."""
    try:
        if path.stat().st_size > 32 * 1024 * 1024:
            return False
        data = path.read_bytes()
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            return False
        pos, chunks, payload = 8, [], bytearray()
        while pos + 12 <= len(data):
            count = struct.unpack_from(">I", data, pos)[0]
            if count > 32 * 1024 * 1024 or pos + 12 + count > len(data):
                return False
            kind = data[pos + 4:pos + 8]
            chunk = data[pos + 8:pos + 8 + count]
            crc = struct.unpack_from(">I", data, pos + 8 + count)[0]
            if zlib.crc32(kind + chunk) & 0xffffffff != crc:
                return False
            if not chunks and (kind != b"IHDR" or count != 13):
                return False
            if kind == b"IHDR":
                if chunks or not all(struct.unpack_from(">II", chunk)[:2]):
                    return False
            if kind == b"IDAT":
                payload.extend(chunk)
            if kind == b"IEND":
                return count == 0 and pos + 12 == len(data) and bool(payload) and _inflate_ok(payload)
            chunks.append(kind)
            pos += 12 + count
        return False
    except (OSError, ValueError, struct.error, zlib.error):
        return False


def _inflate_ok(payload):
    decompressor = zlib.decompressobj()
    total = 0
    for offset in range(0, len(payload), 65536):
        part = payload[offset:offset + 65536]
        while part:
            expanded = decompressor.decompress(part, 8 * 1024 * 1024 - total + 1)
            total += len(expanded)
            if total > 8 * 1024 * 1024:
                return False
            part = decompressor.unconsumed_tail
    return decompressor.eof and not decompressor.unused_data and total > 0


def instrument(path, roots):
    raw = path.read_text(encoding="utf-8", errors="replace")
    declared, cases, started, failures, problems = set(), {}, set(), [], []
    current, final_code = {}, None
    for line in raw.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: "):
            key, sep, value = line[24:].partition("=")
            if sep:
                current[key] = value
                if key == "numtests":
                    if value.isdigit():
                        declared.add(int(value))
                    else:
                        issue(problems, "invalid-declaration", "Non-numeric numtests")
        elif CODE.match(line):
            code = int(CODE.match(line).group(1))
            cls, method = current.get("class", ""), current.get("test", "")
            if cls and method:
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
                        reason = "failure" if code == -2 else "skip" if code in (-3, -4) else "error"
                        exception = re.search(r"([A-Za-z][A-Za-z0-9_.]*(?:Exception|Error))\b", current.get("stack", ""))
                        failures.append({"case": safe_case(cls) + "#" + safe_case(method),
                                         "kind": reason, "excerpt": exception.group(1) if exception else "runner-status-" + str(code),
                                         "raw": relative(path, roots)})
            elif code != 1:
                issue(problems, "missing-identity", "Terminal without class/method")
            current = {}
        elif line.startswith("INSTRUMENTATION_CODE: "):
            try:
                final_code = int(line.partition(":")[2].strip())
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
    ok = [int(x) for x in SUMMARY.findall(raw)]
    complete = (declared_count is not None and total == declared_count and
                not (started - cases.keys()) and final_code == -1 and
                len(ok) == 1 and ok[0] == total and not crash)
    if missing == 0 and declared_count is not None and len(started) < total:
        issue(problems, "missing-start", "Terminal case lacks start event")
        complete = False
    if missing is not None and missing < 0:
        complete = False
    if crash:
        issue(problems, "process-crash", "Instrumentation process crashed")
    if not complete:
        issue(problems, "incomplete-run", "Runner lacks consistent complete terminal summary")
    bad = failed + errors > 0 or crash or final_code not in (-1, None)
    result_status = "FAILED" if bad or any(x["kind"] in ("duplicate-terminal", "duplicate-start") for x in problems) else "INCOMPLETE" if not complete else "PASS"
    if not cases:
        result_status = "FAILED" if crash else "INCOMPLETE"
    if declared_count is not None and total > declared_count:
        result_status = "FAILED"
        issue(problems, "over-count", "More executed cases than declared")
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
            root = ET.parse(path).getroot()
            if root.tag not in ("testsuite", "testsuites"):
                raise ValueError("unexpected XML root")
            suites = [root] if root.tag == "testsuite" else list(root.findall("testsuite"))
            for suite in suites:
                cases = list(suite.iter("testcase"))
                counts = [len(cases), 0, 0, 0]
                for case in cases:
                    key = (module, case.get("classname"), case.get("name"))
                    if not all(key[1:]) or key in seen:
                        raise ValueError("duplicate or anonymous JVM testcase")
                    seen.add(key)
                    counts[1] += bool(case.findall("failure"))
                    counts[2] += bool(case.findall("error"))
                    counts[3] += bool(case.findall("skipped"))
                attr = [int(suite.attrib[k]) for k in ("tests", "failures", "errors", "skipped")]
                if counts != attr or min(attr) < 0 or sum(attr[1:]) > attr[0]:
                    raise ValueError("JVM suite totals contradict testcases")
                totals[module] = [a + b for a, b in zip(totals[module], counts)]
        except (ET.ParseError, ValueError, KeyError) as exc:
            issue(problems, "malformed-xml", "Invalid JVM XML at " + relative(path, roots))
    n = sum(v[0] for v in totals.values())
    failures = sum(v[1] + v[2] for v in totals.values())
    skipped = sum(v[3] for v in totals.values())
    return {"status": "FAILED" if problems or failures or skipped else "PASS" if n else "INCOMPLETE",
            "modules": dict(zip(totals, (dict(zip(("tests", "failures", "errors", "skips"), v)) for v in totals.values()))),
            "tests": n, "failures": failures, "skips": skipped,
            "xml_files": len(xmls), "issues": problems}


def find_sources(roots):
    phone, jvm, wear, meta = [], [], [], []
    for root in roots:
        dirs = [root] + [p for p in root.rglob("*") if p.is_dir() and not p.is_symlink()]
        for folder in dirs:
            if (folder / "manifest.json").is_file() and (folder / "instrumentation.txt").is_file():
                phone.append(folder)
            if (folder / "native-apk-sha256.txt").is_file():
                meta.append(folder)
            if JVM.fullmatch(folder.name):
                jvm.append(folder)
            if WEAR.fullmatch(folder.name) and (folder / "instrumentation.txt").is_file():
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
    run = instrument(folder / "instrumentation.txt", roots)
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
        if entry["file"] in shot_names:
            issue(problems, "duplicate-screenshot", "Repeated screenshot manifest entry")
        shot_names.add(entry["file"])
        if not target.is_file() or not png_ok(target):
            issue(problems, "bad-png", "Missing or invalid screenshot PNG")
        elif not isinstance(entry.get("sha256"), str) or sha256(target) != entry["sha256"]:
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
        result = instrument(folder / "instrumentation.txt", roots)
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
    text = (folder / "native-apk-sha256.txt").read_text(encoding="utf-8", errors="replace")
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
    if not args.apk_file:
        issue(problems, "unverified-apk", "APK checksum file without independently checked APK bytes")
    return {"status": "FAILED" if any(i["kind"] in ("bad-hash-record", "apk-hash-mismatch") for i in problems)
            else "INCOMPLETE" if problems else "PASS", "records": len(records), "verified": len(args.apk_file),
            "issues": problems, "raw": relative(folder / "native-apk-sha256.txt", roots)}


def summarize(args):
    roots = [p.resolve() for p in args.roots]
    for path in roots:
        if not path.is_dir():
            raise ValueError("Not an extracted artifact directory: " + str(path))
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
    for gate in args.require_gate:
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
        elif name == "wear":
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
    try:
        report = summarize(args)
        for destination in (args.json_out, args.markdown_out):
            if any(destination.resolve().is_relative_to(p.resolve()) for p in args.roots):
                raise ValueError("Output may not modify any input artifact directory")
        for target, contents in ((args.json_out, json.dumps(report, ensure_ascii=False, indent=2) + "\n"),
                                 (args.markdown_out, markdown(report))):
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(contents, encoding="utf-8")
    except (OSError, ValueError) as exc:
        parser.error(str(exc))
    print(f"{report['status']}: " + ", ".join(f"{name}={v['status']}" for name, v in report["gates"].items()))
    return 0 if report["status"] == "PASS" else 1 if report["status"] == "FAILED" else 2


if __name__ == "__main__":
    sys.exit(main())
