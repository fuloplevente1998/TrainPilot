#!/usr/bin/env python3
"""No network: exercise repository-cleanup policy against deterministic fixtures."""
import importlib.util
from datetime import datetime, timedelta, timezone
from pathlib import Path
import unittest
from unittest.mock import patch

module = importlib.util.spec_from_file_location(
    "repository_cleanup", Path(__file__).resolve().parents[1] / "scripts/repository_cleanup.py")
cleanup = importlib.util.module_from_spec(module)
module.loader.exec_module(cleanup)
NOW = datetime(2026, 10, 20, tzinfo=timezone.utc)
OLD = (NOW - timedelta(days=30)).isoformat()
NEW = (NOW - timedelta(days=1)).isoformat()


class CleanupSafetyTests(unittest.TestCase):
    def test_runs_preserve_recent_latest_and_main_release_evidence(self):
        runs = [
            {"id": i, "workflow_id": 1, "name": "Quick", "status": "completed",
             "created_at": (NOW-timedelta(days=i+5)).isoformat(),
             "head_branch": "old", "conclusion": "failure"}
            for i in range(1, 9)
        ]
        runs += [
            {"id": 99, "workflow_id": 2, "name": "Wear Release Gate",
             "status": "completed", "created_at": OLD,
             "head_branch": "main", "conclusion": "success"},
            {"id": 100, "workflow_id": 3, "name": "UI",
             "status": "in_progress", "created_at": OLD, "head_branch": "main"}
        ]
        selected = cleanup.choose_runs(runs, NOW - timedelta(days=7))
        self.assertEqual([x["id"] for x in selected], [8, 7, 6])
        self.assertNotIn(99, [x["id"] for x in selected])
        self.assertNotIn(100, [x["id"] for x in selected])

    def test_caches_only_expired_last_access(self):
        data = [
            {"id": 1, "key": "old", "created_at": OLD, "last_accessed_at": OLD},
            {"id": 2, "key": "reused", "created_at": OLD, "last_accessed_at": NEW},
        ]
        self.assertEqual([r["id"] for r in cleanup.choose_caches(
            data, NOW-timedelta(days=7))], [1])

    def test_branches_keep_active_unmerged_protected_and_diverged(self):
        def br(name, sha="merged", protected=False):
            return {"name": name, "commit": {"sha": sha}, "protected": protected}
        def pull(name, merged=None):
            return {"head": {"ref": name, "repo": {"full_name": cleanup.REPO}},
                    "merged_at": merged}
        branches = [br("main"), br("perf/wear-round-ui-shards"), br("feat/active"),
                    br("fix/old-merged"), br("fix/recent"),
                    br("fix/diverged", "diverged"), br("feat/unmerged"),
                    br("fix/protected", protected=True), br("archive/old")]
        open_prs = [pull("feat/active"), pull("perf/wear-round-ui-shards")]
        closed_prs = [pull("fix/old-merged", OLD), pull("fix/recent", NEW),
                      pull("fix/diverged", OLD), pull("fix/protected", OLD),
                      pull("archive/old", OLD)]
        seen = []
        def compare(sha):
            seen.append(sha)
            return {"status": "diverged" if sha == "diverged" else "ahead",
                    "behind_by": 1 if sha == "diverged" else 0}
        found, review = cleanup.choose_branches(
            branches, open_prs, closed_prs, NOW-timedelta(days=7), compare)
        self.assertEqual([x["name"] for x in found], ["fix/old-merged"])
        self.assertEqual(seen, ["merged", "diverged"])
        self.assertTrue(any(x["branch"] == "feat/unmerged" for x in review))

    def test_merged_wear_branch_only_becomes_candidate_after_seven_days(self):
        name = "perf/wear-round-ui-shards"
        branches = [{"name": name, "commit": {"sha": "merged"}, "protected": False}]
        def pr(merge_date=None):
            return {"head": {"ref": name, "repo": {"full_name": cleanup.REPO}},
                    "merged_at": merge_date}
        def compare(sha):
            self.assertEqual(sha, "merged")
            return {"status": "ahead", "behind_by": 0}
        cutoff = NOW - timedelta(days=7)
        recent, _ = cleanup.choose_branches(branches, [], [pr(NEW)], cutoff, compare)
        self.assertEqual(recent, [])
        active, _ = cleanup.choose_branches(branches, [pr()], [pr(OLD)], cutoff, compare)
        self.assertEqual(active, [])
        expired, _ = cleanup.choose_branches(branches, [], [pr(OLD)], cutoff, compare)
        self.assertEqual([row["name"] for row in expired], [name])

    def test_apply_rejects_missing_confirmation_before_github_access(self):
        for argv in [["--apply"], ["--apply", "--confirmation", cleanup.CONFIRM]]:
            with self.assertRaises(SystemExit):
                with patch.dict("os.environ", {"GITHUB_EVENT_NAME": "push",
                                                "GITHUB_REF_NAME": "main",
                                                "GITHUB_REPOSITORY": cleanup.REPO}):
                    cleanup.main(argv)
        with self.assertRaises(SystemExit):
            cleanup.main(["--runs-days", "0"])

    def test_apply_respects_operation_budget_and_rechecks_branches(self):
        class API:
            repo = cleanup.REPO
            def __init__(self):
                self.calls = []
            def call(self, path, method="GET"):
                self.calls.append((method, path))
                if method == "GET":
                    return {"commit": {"sha": "correct"}, "protected": False}
        report = {"candidates": {"runs": [{"id": 1}], "caches": [{"id": 2}],
                                 "merged_branches": [{"name": "fix/merged", "sha": "correct"}]}}
        api = API()
        cleanup.execute(api, report, 2)
        self.assertEqual([r[0] for r in api.calls], ["DELETE", "DELETE"])
        self.assertEqual(report["remaining_after_budget"], 1)
        api = API()
        cleanup.execute(api, report, 3)
        self.assertEqual(api.calls[-1], ("DELETE",
                         f"repos/{cleanup.REPO}/git/refs/heads/fix/merged"))


if __name__ == "__main__":
    unittest.main()
