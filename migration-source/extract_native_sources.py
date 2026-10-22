#!/usr/bin/env python3
"""Extract file-addressed native source blocks from an AI Studio JSON export."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_EXPORT = ROOT / "TrainPilot_Full_Conversation_Export.json"
MANIFEST_PATH = ROOT / "NATIVE_IMPORT_MANIFEST.md"
EXTRACTION_REPORT_PATH = ROOT / "migration-source" / "extraction-report.json"
REVIEW_DIR = ROOT / "migration-source" / "review"
SUPPORTED_SUFFIXES = {
    ".kt", ".java", ".xml", ".gradle", ".kts", ".properties", ".json",
    ".pro", ".txt", ".md", ".toml", ".yaml", ".yml",
}
LANGUAGES_BY_SUFFIX = {
    ".kt": {"kotlin", "kt"},
    ".java": {"java"},
    ".xml": {"xml"},
    ".gradle": {"groovy", "gradle"},
    ".kts": {"kotlin", "kts"},
    ".properties": {"properties", "ini"},
    ".json": {"json"},
    ".pro": {"proguard"},
    ".txt": {"text", "txt"},
    ".md": {"markdown", "md"},
    ".toml": {"toml"},
    ".yaml": {"yaml"},
    ".yml": {"yaml"},
}
SOURCE_ROLES = {"model", "assistant"}
FILENAME_RE = re.compile(
    r"(?<![\w./-])([A-Za-z0-9_.+-]+\.(?:kt|java|xml|gradle|kts|properties|json|pro|txt|md|toml|yaml|yml))"
)
FULL_PATH_RE = re.compile(
    r"(?<![\w./-])((?:android|gradle)/[A-Za-z0-9_.+@/-]+|"
    r"(?:settings\.gradle(?:\.kts)?|build\.gradle(?:\.kts)?|"
    r"gradle\.properties|variables\.gradle))"
)
SLICE_RE = re.compile(r"(?i)\bslice\s*(\d+)(?:\s*/\s*([ab]))?\b")
FENCE_RE = re.compile(r"^\s*```([^`\r\n]*)[ \t]*(?:\r?\n)?$")
JAVA_DUPLICATES = (
    "IntervalTimeline193.java",
    "GpsRoutePacket193.java",
    "GpsRouteSampler193.java",
    "GpsRouteSpool193.java",
    "GpsRouteLedger193.java",
    "WorkoutOwnership193.java",
    "OwnershipJournal193.java",
    "OwnershipTransport193.java",
)


@dataclass(frozen=True)
class CodeBlock:
    language: str
    content: str
    before: str
    chunk_index: int
    timestamp: str
    closed: bool


@dataclass(frozen=True)
class Candidate:
    path: str
    content: str
    language: str
    slice_label: str
    chunk_index: int
    timestamp: str
    followup: str
    complete: bool
    review_reason: str | None = None

    @property
    def sha256(self) -> str:
        return hashlib.sha256(self.content.encode("utf-8")).hexdigest()


def unwrap_message(value: str) -> str:
    """Unwrap JSON-encoded assistant payloads while retaining the full text."""
    text = value
    for _ in range(5):
        try:
            decoded = json.loads(text)
        except (json.JSONDecodeError, TypeError):
            break
        if isinstance(decoded, str):
            text = decoded
            continue
        if isinstance(decoded, dict):
            nested = next(
                (
                    decoded[key]
                    for key in ("response", "text", "content")
                    if isinstance(decoded.get(key), str)
                ),
                None,
            )
            if nested is not None:
                text = nested
                continue
        break
    return text


def markdown_code_blocks(text: str, chunk_index: int, timestamp: str) -> list[CodeBlock]:
    lines = text.splitlines(keepends=True)
    blocks: list[CodeBlock] = []
    previous_end = 0
    opening_index: int | None = None
    opening_language = ""
    for index, line in enumerate(lines):
        stripped = line.strip()
        if opening_index is None:
            match = FENCE_RE.match(line)
            if match:
                opening_index = index
                opening_language = match.group(1).strip().lower()
            continue
        if stripped.startswith("```") and stripped.strip("`").strip() == "":
            before = "".join(lines[:opening_index])
            blocks.append(
                CodeBlock(
                    language=opening_language,
                    content="".join(lines[opening_index + 1 : index]),
                    before=before[previous_end:],
                    chunk_index=chunk_index,
                    timestamp=timestamp,
                    closed=True,
                )
            )
            previous_end = sum(len(item) for item in lines[: index + 1])
            opening_index = None
            opening_language = ""
    if opening_index is not None:
        before = "".join(lines[:opening_index])
        blocks.append(
            CodeBlock(
                language=opening_language,
                content="".join(lines[opening_index + 1 :]),
                before=before[previous_end:],
                chunk_index=chunk_index,
                timestamp=timestamp,
                closed=False,
            )
        )
    return blocks


def supported_path(path: str) -> bool:
    normalized = path.replace("\\", "/").strip().strip("`'\".,:;")
    candidate = PurePosixPath(normalized)
    if candidate.is_absolute() or ".." in candidate.parts:
        return False
    if normalized.startswith("android/"):
        return candidate.suffix.lower() in SUPPORTED_SUFFIXES
    if normalized.startswith("gradle/"):
        return candidate.suffix.lower() in SUPPORTED_SUFFIXES
    return (
        "/" not in normalized
        and candidate.name
        in {
            "settings.gradle", "settings.gradle.kts", "build.gradle",
            "build.gradle.kts", "gradle.properties", "variables.gradle",
        }
    )


def heading_file_tokens(segment: str) -> tuple[list[str], list[str]]:
    """Return file paths in the section and filenames in its latest heading."""
    headings = [
        line
        for line in segment.splitlines()
        if re.match(r"^\s*#{1,6}\s", line)
    ]
    heading = headings[-1] if headings else ""
    paths = [
        match.group(1).rstrip("`.,:;")
        for match in FULL_PATH_RE.finditer(segment)
    ]
    names = [match.group(1).rstrip("`.,:;") for match in FILENAME_RE.finditer(heading)]
    paths = [path for path in paths if "..." not in path and supported_path(path)]
    return paths, names


def resolve_path(
    block: CodeBlock, inherited_directory: str | None
) -> tuple[str | None, str | None, str | None]:
    paths, names = heading_file_tokens(block.before)
    directory = inherited_directory
    if paths:
        explicit = paths[-1]
        parent = str(PurePosixPath(explicit).parent)
        directory = "" if parent == "." else parent
        if names and PurePosixPath(explicit).name != names[-1]:
            filename = names[-1]
            resolved = f"{directory}/{filename}" if directory else filename
            if supported_path(resolved):
                return resolved, directory, None
        return explicit, directory, None
    if names:
        filename = names[-1]
        if directory is None:
            return None, None, "basename-only heading has no unambiguous parent directory"
        resolved = f"{directory}/{filename}" if directory else filename
        if supported_path(resolved):
            return resolved, directory, None
    return None, directory, "no supported project filepath in the code block heading"


def slice_label(text: str) -> str:
    headings = [line for line in text.splitlines() if re.match(r"^\s*#{1,6}\s", line)]
    heading_hits: list[str] = []
    for heading in headings:
        for match in SLICE_RE.finditer(heading):
            suffix = f"/{match.group(2).upper()}" if match.group(2) else ""
            heading_hits.append(f"Slice {match.group(1)}{suffix}")
    candidates = heading_hits or [
        f"Slice {match.group(1)}{('/' + match.group(2).upper()) if match.group(2) else ''}"
        for match in SLICE_RE.finditer(text)
    ]
    if not candidates:
        if "migration" in text.lower() or "natív" in text.lower():
            return "Post-slice integration"
        return "Unassigned"
    counts = Counter(candidates)
    best_count = max(counts.values())
    return next(item for item in candidates if counts[item] == best_count)


def is_truncated(content: str, closed: bool) -> str | None:
    if not closed:
        return "unclosed code fence"
    if not content.strip():
        return "empty code block"
    markers = (
        "[TRUNCATED]", "[truncated]", "/* ... truncated", "// ... truncated",
        "csonkolt kód", "tartalom csonkolva", "code omitted",
    )
    for marker in markers:
        if marker.lower() in content.lower():
            return f"explicit truncation marker: {marker}"
    if "```" in content:
        return "nested code fence prevents reliable file-boundary detection"
    return None


def source_language_matches(path: str, language: str) -> bool:
    expected = LANGUAGES_BY_SUFFIX.get(PurePosixPath(path).suffix.lower(), set())
    return language in expected


def followup_status(chunks: list[dict[str, Any]], source_index: int) -> str:
    following_user = next(
        (item for item in chunks[source_index + 1 :] if item.get("role") == "user"),
        None,
    )
    if following_user is None:
        return "no later user turn in export"
    text = unwrap_message(str(following_user.get("text", ""))).strip()
    compact = re.sub(r"\s+", " ", text)
    lower = compact.lower()
    if any(word in lower for word in ("kiváló", "jóváhagy", "elfogad", "mehet", "rendben")):
        return "user acceptance/continuation in following turn"
    if any(word in lower for word in ("folytasd", "folytassuk", "indithatod", "indíthatod", "tovább", "jöhet")):
        return "user continuation in following turn"
    return "following turn does not explicitly approve this source"


def collect_candidates(export_path: Path) -> tuple[list[Candidate], list[dict[str, Any]], int]:
    with export_path.open("r", encoding="utf-8") as handle:
        export = json.load(handle)
    chunks = export.get("conversation", {}).get("chunkedPrompt", {}).get("chunks")
    if not isinstance(chunks, list):
        raise ValueError("Export does not contain conversation.chunkedPrompt.chunks")

    candidates: list[Candidate] = []
    review_blocks: list[dict[str, Any]] = []
    for chunk_index, chunk in enumerate(chunks):
        if chunk.get("role") not in SOURCE_ROLES:
            continue
        text = unwrap_message(str(chunk.get("text", "")))
        blocks = markdown_code_blocks(
            text, chunk_index, str(chunk.get("createTime", "unknown"))
        )
        inherited_directory: str | None = None
        assigned_slice = slice_label(text)
        for block in blocks:
            path, inherited_directory, path_reason = resolve_path(block, inherited_directory)
            if path is None:
                if block.language in {"kotlin", "kt", "java", "xml", "groovy", "gradle"}:
                    review_blocks.append(
                        {
                            "chunk_index": chunk_index,
                            "timestamp": block.timestamp,
                            "language": block.language,
                            "reason": path_reason,
                            "preview": block.content[:180],
                            "content": block.content,
                            "closed": block.closed,
                        }
                    )
                continue
            if not source_language_matches(path, block.language):
                if block.language in {"kotlin", "kt", "java", "xml", "groovy", "gradle"}:
                    review_blocks.append(
                        {
                            "chunk_index": chunk_index,
                            "timestamp": block.timestamp,
                            "language": block.language,
                            "reason": f"fence language does not match {path}",
                            "preview": block.content[:180],
                            "content": block.content,
                            "closed": block.closed,
                        }
                    )
                continue
            reason = is_truncated(block.content, block.closed)
            candidates.append(
                Candidate(
                    path=path,
                    content=block.content,
                    language=block.language,
                    slice_label=assigned_slice,
                    chunk_index=chunk_index,
                    timestamp=block.timestamp,
                    followup=followup_status(chunks, chunk_index),
                    complete=reason is None,
                    review_reason=reason,
                )
            )
    return candidates, review_blocks, len(chunks)


def choose_latest(candidates: list[Candidate]) -> dict[str, Candidate]:
    grouped: dict[str, list[Candidate]] = defaultdict(list)
    for candidate in candidates:
        grouped[candidate.path].append(candidate)
    selected: dict[str, Candidate] = {}
    for path, versions in grouped.items():
        complete = [candidate for candidate in versions if candidate.complete]
        if complete:
            selected[path] = max(complete, key=lambda candidate: candidate.chunk_index)
    return selected


def render_manifest(
    export_path: Path,
    all_candidates: list[Candidate],
    selected: dict[str, Candidate],
    review_blocks: list[dict[str, Any]],
    imported_paths: set[str],
    cleanups: list[dict[str, str]],
) -> str:
    versions_by_path: dict[str, list[Candidate]] = defaultdict(list)
    for candidate in all_candidates:
        versions_by_path[candidate.path].append(candidate)
    lines = [
        "# Native Android import manifest",
        "",
        f"Conversation export: `{export_path.relative_to(ROOT)}`",
        f"Extracted code-file candidates: {len(all_candidates)}; selected files: {len(selected)}; "
        f"unassigned code blocks for review: {len(review_blocks)}.",
        "",
        "| Source file | Slice | Conversation source | Import status | Versions | SHA-256 |",
        "|---|---|---|---|---:|---|",
    ]
    for path in sorted(selected):
        candidate = selected[path]
        status = "Imported" if path in imported_paths else "Extracted; pending import"
        cleanup = next((item for item in cleanups if item["path"] == path), None)
        if cleanup:
            if cleanup["action"].startswith("Removed"):
                status = (
                    "Imported with verified duplicate removal"
                    if path in imported_paths and cleanup["status"] in {"removed", "already removed"}
                    else "Pending import with verified duplicate removal"
                )
            else:
                status = (
                    "Imported with verified screen wiring"
                    if path in imported_paths and cleanup["status"] in {"applied", "already applied"}
                    else "Pending import with verified screen wiring"
                )
        versions = versions_by_path[path]
        lines.append(
            f"| `{path}` | {candidate.slice_label} | chunk {candidate.chunk_index} "
            f"({candidate.timestamp}) | {status} | {len(versions)} | `{candidate.sha256}` |"
        )
    lines.extend(
        [
            "",
            "## Selection and validation notes",
            "",
            "- For repeated paths, the last completely fenced candidate in conversation order was selected.",
            "- The conversation turn following each source message is recorded in the extraction report; "
            "it is evidence of continuation, not a claim of formal approval.",
            "- Existing source was replaced only from a file-addressed, complete code block in the export.",
            "- Unassigned/ambiguous blocks and files without an unambiguous path are kept out of the Android tree.",
            "",
            "## Verified collision cleanup",
            "",
        ]
    )
    if cleanups:
        for item in cleanups:
            lines.append(
                f"- `{item['path']}`: {item['action']} ({item['status']}); "
                f"authoritative source: `{item['replacement']}`."
            )
    else:
        lines.append("- None.")
    lines.extend(
        [
            "",
            "## Blocks requiring manual attribution",
            "",
        ]
    )
    if review_blocks:
        for item in review_blocks:
            preview = re.sub(r"\s+", " ", item["preview"]).replace("|", "\\|")
            review_file = f"migration-source/review/chunk-{item['chunk_index']:03}.json"
            lines.append(
                f"- chunk {item['chunk_index']} ({item['timestamp']}), "
                f"`{item['language'] or 'unlabelled'}`: {item['reason']}; "
                f"full block isolated at [`{review_file}`](./{review_file}); "
                f"preview: `{preview}`"
            )
    else:
        lines.append("- None found among Kotlin/Java/XML/Gradle code fences.")
    lines.append("")
    return "\n".join(lines)


def write_review_blocks(review_blocks: list[dict[str, Any]]) -> None:
    REVIEW_DIR.mkdir(parents=True, exist_ok=True)
    for item in review_blocks:
        path = REVIEW_DIR / f"chunk-{item['chunk_index']:03}.json"
        record = {
            "chunk_index": item["chunk_index"],
            "timestamp": item["timestamp"],
            "language": item["language"],
            "reason": item["reason"],
            "closed": item["closed"],
            "content": item["content"],
        }
        path.write_text(
            json.dumps(record, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )


def wire_imported_screens(apply: bool) -> dict[str, str]:
    navigation_path = ROOT / "android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt"
    if not navigation_path.is_file():
        return {}
    text = navigation_path.read_text(encoding="utf-8")
    imports = (
        "import com.repforge.app.ui.health.HealthDashboardScreen",
        "import com.repforge.app.ui.journal.JournalScreen",
        "import com.repforge.app.ui.progress.ProgressScreen",
        "import com.repforge.app.ui.settings.SettingsScreen",
        "import com.repforge.app.ui.workout.ActiveWorkoutScreen",
    )
    replacements = (
        (
            '''composable(Screen.Journal.route) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Edzésnapló (Slice 8)", color = colors.muted)
                    }
                }''',
            '''composable(Screen.Journal.route) {
                    JournalScreen(navController = navController)
                }''',
        ),
        (
            '''composable(Screen.Progress.route) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Fejlődés & Anatómia (Slice 8)", color = colors.muted)
                    }
                }''',
            '''composable(Screen.Progress.route) {
                    ProgressScreen(navController = navController)
                }''',
        ),
        (
            '''composable(Screen.Health.route) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Egészségközpont & BLE (Slice 8)", color = colors.muted)
                    }
                }''',
            '''composable(Screen.Health.route) {
                    HealthDashboardScreen(navController = navController)
                }''',
        ),
        (
            '''composable("active_workout") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aktív Edzés Képernyő (Slice 7)", color = colors.accent)
                    }
                }''',
            '''composable("active_workout") {
                    ActiveWorkoutScreen(navController = navController)
                }''',
        ),
        (
            '''composable("settings") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Beállítások & Szinkron (Slice 8)", color = colors.muted)
                    }
                }''',
            '''composable("settings") {
                    SettingsScreen(navController = navController)
                }''',
        ),
    )
    changed = False
    for old, new in replacements:
        if old in text:
            text = text.replace(old, new, 1)
            changed = True
        elif new not in text:
            raise ValueError("Navigation source does not match known Slice 6 or wired screen structure")
    for import_line in imports:
        if import_line not in text:
            anchor = "import com.repforge.app.ui.home.HomeScreen\n"
            if anchor not in text:
                raise ValueError("Cannot safely insert imported screen navigation dependencies")
            text = text.replace(anchor, anchor + import_line + "\n", 1)
            changed = True
    status = "planned"
    if apply and changed:
        navigation_path.write_text(text, encoding="utf-8")
        status = "applied"
    elif not changed:
        status = "already applied"
    return {
        "path": navigation_path.relative_to(ROOT).as_posix(),
        "replacement": "Slice 7 and Slice 8 screen composables",
        "action": "Connected Slice 6 placeholder routes to the imported workout, journal, progress, health, and settings screens",
        "status": status,
    }


def apply_project_fixes(apply: bool) -> list[dict[str, str]]:
    cleanups: list[dict[str, str]] = []
    shared_root = ROOT / "android/shared/src/main/java/com/repforge/shared"

    for java_name in JAVA_DUPLICATES:
        java_path = shared_root / java_name
        kotlin_path = java_path.with_suffix(".kt")
        if not kotlin_path.is_file():
            continue
        kotlin_text = kotlin_path.read_text(encoding="utf-8")
        class_name = java_path.stem
        kotlin_package = re.search(r"^\s*package\s+([\w.]+)", kotlin_text, re.M)
        kotlin_declares = re.search(
            rf"^\s*(?:(?:open|data|sealed|abstract|internal|private)\s+)*"
            rf"(?:class|object)\s+{re.escape(class_name)}\b",
            kotlin_text,
            re.M,
        )
        if not kotlin_declares or not kotlin_package:
            raise ValueError(
                f"Refusing duplicate cleanup without Kotlin replacement: {kotlin_path.relative_to(ROOT)}"
            )
        if java_path.exists():
            java_text = java_path.read_text(encoding="utf-8")
            java_package = re.search(r"^\s*package\s+([\w.]+)", java_text, re.M)
            java_declares = re.search(
                rf"^\s*(?:public\s+)?(?:final\s+)?class\s+{re.escape(class_name)}\b",
                java_text,
                re.M,
            )
            if (
                not java_declares
                or not java_package
                or java_package.group(1) != kotlin_package.group(1)
            ):
                raise ValueError(
                    f"Refusing to remove non-colliding source file: {java_path.relative_to(ROOT)}"
                )
            status = "planned" if not apply else "removed"
        else:
            status = "already removed"
        cleanups.append(
            {
                "path": java_path.relative_to(ROOT).as_posix(),
                "replacement": kotlin_path.relative_to(ROOT).as_posix(),
                "action": "Removed duplicate Java declaration; shared Kotlin implementation retains the Java-facing API",
                "status": status,
            }
        )
        if apply and java_path.exists():
            java_path.unlink()

    contracts_path = shared_root / "WorkoutSharedContracts.kt"
    identity_path = shared_root / "LegacySyncIdentity18.kt"
    if contracts_path.is_file() and identity_path.is_file():
        contracts = contracts_path.read_text(encoding="utf-8")
        identity = identity_path.read_text(encoding="utf-8")
        start_marker = (
            "/** Shared #18 FNV-1a deterministic legacy ID generator and "
            "Google Calendar event ID generator. */"
        )
        end_marker = "/** Shared Phase 4 Exercise Statistics & Epley 1RM Calculator. */"
        start = contracts.find(start_marker)
        end = contracts.find(end_marker, start + len(start_marker)) if start >= 0 else -1
        if (
            start >= 0
            and end > start
            and re.search(r"^\s*object\s+LegacySyncIdentity18\b", identity, re.M)
        ):
            status = "planned" if not apply else "removed"
            cleanups.append(
                {
                    "path": contracts_path.relative_to(ROOT).as_posix(),
                    "replacement": identity_path.relative_to(ROOT).as_posix(),
                    "action": "Removed the earlier duplicate LegacySyncIdentity18 object",
                    "status": status,
                }
            )
            if apply:
                contracts_path.write_text(contracts[:start] + contracts[end:], encoding="utf-8")
        elif re.search(r"^\s*object\s+LegacySyncIdentity18\b", contracts, re.M):
            raise ValueError(
                "LegacySyncIdentity18 collision exists but its exact source block is not recognized"
            )
        elif re.search(r"^\s*object\s+LegacySyncIdentity18\b", identity, re.M):
            cleanups.append(
                {
                    "path": contracts_path.relative_to(ROOT).as_posix(),
                    "replacement": identity_path.relative_to(ROOT).as_posix(),
                    "action": "Removed the earlier duplicate LegacySyncIdentity18 object",
                    "status": "already removed",
                }
            )

    privacy_bundle = ROOT / "android/app/src/main/java/com/repforge/app/PrivacyPolicyAndActivity.kt"
    privacy_text = privacy_bundle.read_text(encoding="utf-8") if privacy_bundle.is_file() else ""
    for old_name, declaration in (
        ("PrivacyPolicy.kt", r"^\s*(?:(?:internal|private|public)\s+)?object\s+PrivacyPolicy\b"),
        ("HealthPrivacyActivity.kt", r"^\s*class\s+HealthPrivacyActivity\b"),
    ):
        old_path = ROOT / "android/app/src/main/java/com/repforge/app" / old_name
        if not re.search(declaration, privacy_text, re.M):
            raise ValueError(f"Imported privacy source is missing {old_name} declaration")
        if not old_path.is_file():
            cleanups.append(
                {
                    "path": old_path.relative_to(ROOT).as_posix(),
                    "replacement": privacy_bundle.relative_to(ROOT).as_posix(),
                    "action": "Removed obsolete __PACKAGE_NAME__ placeholder superseded by the imported full implementation",
                    "status": "already removed",
                }
            )
            continue
        old_text = old_path.read_text(encoding="utf-8")
        if "package __PACKAGE_NAME__" not in old_text or not re.search(declaration, old_text, re.M):
            raise ValueError(
                f"Refusing to remove unverified placeholder: {old_path.relative_to(ROOT)}"
            )
        cleanups.append(
            {
                "path": old_path.relative_to(ROOT).as_posix(),
                "replacement": privacy_bundle.relative_to(ROOT).as_posix(),
                "action": "Removed obsolete __PACKAGE_NAME__ placeholder superseded by the imported full implementation",
                "status": "planned" if not apply else "removed",
            }
        )
        if apply:
            old_path.unlink()

    navigation_fix = wire_imported_screens(apply)
    if navigation_fix:
        cleanups.append(navigation_fix)
    return cleanups


def safe_write(destination: Path, content: str, apply: bool) -> str:
    encoded = content.encode("utf-8")
    if destination.exists():
        current = destination.read_bytes()
        if current == encoded:
            return "unchanged"
        if not apply:
            return "would update"
    elif not apply:
        return "would create"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(encoded)
    return "written"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--export", type=Path, default=DEFAULT_EXPORT)
    parser.add_argument(
        "--apply",
        action="store_true",
        help="write selected source files into the repository; without this, only audit outputs are written",
    )
    args = parser.parse_args()
    export_path = args.export.resolve()
    if not export_path.is_file():
        parser.error(f"conversation export does not exist: {export_path}")

    candidates, review_blocks, message_count = collect_candidates(export_path)
    selected = choose_latest(candidates)
    written_paths: set[str] = set()
    outcomes: dict[str, str] = {}
    for relative_path, candidate in sorted(selected.items()):
        destination = (ROOT / PurePosixPath(relative_path)).resolve()
        if ROOT not in destination.parents:
            raise ValueError(f"Refusing to write outside repository: {relative_path}")
        outcome = safe_write(destination, candidate.content, args.apply)
        outcomes[relative_path] = outcome
        if outcome in {"written", "unchanged"} and args.apply:
            written_paths.add(relative_path)
    cleanups = apply_project_fixes(args.apply)

    report = {
        "export": str(export_path.relative_to(ROOT) if ROOT in export_path.parents else export_path),
        "conversation_message_count": message_count,
        "candidate_count": len(candidates),
        "selected_file_count": len(selected),
        "apply": args.apply,
        "outcomes": outcomes,
        "verified_cleanups": cleanups,
        "candidates": [
            {
                "path": candidate.path,
                "slice": candidate.slice_label,
                "chunk_index": candidate.chunk_index,
                "timestamp": candidate.timestamp,
                "language": candidate.language,
                "sha256": candidate.sha256,
                "complete": candidate.complete,
                "review_reason": candidate.review_reason,
                "followup": candidate.followup,
                "selected": selected.get(candidate.path) == candidate,
            }
            for candidate in candidates
        ],
        "unassigned_code_blocks": review_blocks,
    }
    EXTRACTION_REPORT_PATH.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    write_review_blocks(review_blocks)
    MANIFEST_PATH.write_text(
        render_manifest(
            export_path,
            candidates,
            selected,
            review_blocks,
            written_paths,
            cleanups,
        ),
        encoding="utf-8",
    )
    action = "Imported" if args.apply else "Audited"
    print(
        f"{action} {len(selected)} selected files from {message_count} messages "
        f"({len(candidates)} candidates; {len(review_blocks)} code blocks need attribution)."
    )
    for path, outcome in sorted(outcomes.items()):
        versions = sum(candidate.path == path for candidate in candidates)
        candidate = selected[path]
        suffix = f", {versions} version(s), {candidate.slice_label}, chunk {candidate.chunk_index}"
        print(f"{outcome}: {path}{suffix}")
    print(f"Manifest: {MANIFEST_PATH.relative_to(ROOT)}")
    print(f"Audit report: {EXTRACTION_REPORT_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Extraction failed: {error}", file=sys.stderr)
        raise SystemExit(1) from error
