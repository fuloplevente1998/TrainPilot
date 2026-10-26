from pathlib import Path
import re
import sys

FILES = [
    ".github/workflows/build-apk.yml",
    ".github/workflows/build-play-bundle.yml",
    ".github/workflows/ownership-native-193.yml",
    ".github/workflows/p0-native-validation.yml",
    ".github/workflows/phase7-performance.yml",
    ".github/workflows/phone-execution-ui.yml",
    ".github/workflows/release-metadata-maintenance.yml",
    ".github/workflows/validate-branch.yml",
    ".github/workflows/wear-build.yml",
    ".github/workflows/wear-layout-ui.yml",
    ".github/workflows/wear-release.yml",
]
EXPECTED_SETUP_NODE_JOBS = 14

def job_blocks(text: str):
    lines = text.splitlines()
    in_jobs = False
    current = None
    block = []
    for line in lines:
        if line == "jobs:":
            in_jobs = True
            continue
        if not in_jobs:
            continue
        match = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
        if match:
            if current is not None:
                yield current, "\n".join(block)
            current = match.group(1)
            block = [line]
        elif current is not None:
            block.append(line)
    if current is not None:
        yield current, "\n".join(block)

errors = []
checked = 0
for filename in FILES:
    path = Path(filename)
    if not path.is_file():
        errors.append(f"{filename}: missing")
        continue
    text = path.read_text(encoding="utf-8")
    if "app-target:" not in text or "classify-app-source.yml" not in text:
        errors.append(f"{filename}: missing app-target classifier job")
    for job, block in job_blocks(text):
        if "actions/setup-node@" not in block:
            continue
        checked += 1
        if "app-target" not in block:
            errors.append(f"{filename}:{job}: setup-node job does not depend on app-target")
        if "needs.app-target.outputs.legacy == 'true'" not in block:
            errors.append(f"{filename}:{job}: setup-node job lacks legacy=true guard")

if checked != EXPECTED_SETUP_NODE_JOBS:
    errors.append(f"expected {EXPECTED_SETUP_NODE_JOBS} setup-node jobs, found {checked}")

if errors:
    print("Native/legacy routing verification FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print(f"Native/legacy routing verification PASS: {len(FILES)} workflows, {checked} setup-node jobs guarded")
