#!/usr/bin/env python3
"""Conservative, opt-in cleanup. Default is read-only; never delete release assets/tags."""
from __future__ import annotations
import argparse
from collections import defaultdict
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen

REPO = "fuloplevente1998/TrainPilot"
CONFIRM = "DELETE_OBSOLETE_TRAINPILOT_DATA"
RELEASE_GATES = {"Phone Release Gate + TrainPilot APK", "Wear Release Gate"}
KEEP_PREFIXES = ("archive/", "backup/")
KEEP_BRANCHES = {"main"}  # open PR heads are protected dynamically until merge


def parse_date(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")) if value else None


def older(value, cutoff):
    dt = parse_date(value)
    return dt is not None and dt < cutoff


class GitHub:
    def __init__(self, repo=REPO, token=None):
        self.repo = repo
        self.token = token or os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
        if not self.token:
            raise RuntimeError("Missing GH_TOKEN")

    def call(self, path, method="GET"):
        request = Request("https://api.github.com/" + path,
                          headers={"Authorization": "Bearer " + self.token,
                                   "Accept": "application/vnd.github+json",
                                   "X-GitHub-Api-Version": "2022-11-28"},
                          method=method)
        try:
            with urlopen(request, timeout=50) as response:
                contents = response.read()
                return json.loads(contents) if contents else None
        except HTTPError as error:
            raise RuntimeError(f"GitHub {method} {path}: HTTP {error.code}: "
                               f"{error.read().decode('utf-8', errors='replace')[:300]}") from error

    def paged(self, endpoint, key=None):
        out = []
        for page in range(1, 150):
            join = "&" if "?" in endpoint else "?"
            data = self.call(f"repos/{self.repo}/{endpoint}{join}per_page=100&page={page}")
            rows = data[key] if key else data
            out.extend(rows)
            if len(rows) < 100:
                return out
        raise RuntimeError("Pagination safety cap reached; nothing deleted")


def choose_runs(runs, cutoff):
    pinned = set()
    per_workflow = defaultdict(int)
    for run in sorted(runs, key=lambda r: r.get("created_at") or "", reverse=True):
        flow = run.get("workflow_id") or run.get("name")
        if per_workflow[flow] < 5:
            pinned.add(run["id"])
        per_workflow[flow] += 1
    candidates = []
    for run in runs:
        if (run.get("status") != "completed" or not older(run.get("created_at"), cutoff)
                or run["id"] in pinned):
            continue
        if run.get("head_branch") == "main" and run.get("conclusion") == "success" \
                and run.get("name") in RELEASE_GATES:
            continue
        if run.get("event") in ("release", "deployment"):
            continue
        candidates.append({"id": run["id"], "name": run.get("name"),
                           "branch": run.get("head_branch"), "date": run.get("created_at"),
                           "conclusion": run.get("conclusion")})
    return sorted(candidates, key=lambda r: r["date"])


def choose_caches(caches, cutoff):
    return sorted(({"id": cache["id"], "key": cache.get("key"),
                    "last_accessed_at": cache.get("last_accessed_at")}
                   for cache in caches if older(cache.get("last_accessed_at")
                                               or cache.get("created_at"), cutoff)),
                  key=lambda r: r["last_accessed_at"] or "")


def choose_branches(branches, open_pulls, closed_pulls, cutoff, compare):
    active = {p["head"]["ref"] for p in open_pulls
              if p["head"]["repo"] and p["head"]["repo"]["full_name"] == REPO}
    merged = defaultdict(list)
    for pr in closed_pulls:
        if (pr.get("merged_at") and pr.get("head", {}).get("repo")
                and pr["head"]["repo"]["full_name"] == REPO):
            merged[pr["head"]["ref"]].append(pr["merged_at"])
    candidates = []
    review = []
    for branch in branches:
        name = branch["name"]
        if name in KEEP_BRANCHES or name in active or branch.get("protected") \
                or name.startswith(KEEP_PREFIXES):
            continue
        dates = merged.get(name, [])
        if not dates:
            review.append({"branch": name, "reason": "No verified merged PR: manual review"})
            continue
        if any(not older(date, cutoff) for date in dates):
            continue
        # Do not delete branches with any unmerged commits, even if an old PR
        # was merged from a previous incarnation of this branch.
        head = branch["commit"]["sha"]
        result = compare(head)
        if result.get("status") in ("ahead", "identical") and result.get("behind_by", 0) == 0:
            candidates.append({"name": name, "sha": head, "last_merged_at": max(dates)})
        else:
            review.append({"branch": name, "reason": "Diverged from main; preserve"})
    return candidates, review


def plan(api, now, run_days, cache_days, branch_days):
    # Complete inventory before ANY deletion; a partial plan must fail closed.
    branches = api.paged("branches")
    open_pulls = api.paged("pulls?state=open")
    closed_pulls = api.paged("pulls?state=closed")
    runs = api.paged("actions/runs", "workflow_runs")
    caches = api.paged("actions/caches", "actions_caches")
    main = api.call(f"repos/{api.repo}/git/ref/heads/main")["object"]["sha"]
    cutoff = lambda days: now - timedelta(days=days)

    def compare(sha):
        path = f"repos/{api.repo}/compare/{sha}...{main}"
        return api.call(path)

    merged, review = choose_branches(branches, open_pulls, closed_pulls,
                                     cutoff(branch_days), compare)
    return {
        "generated_at": now.isoformat(), "repository": api.repo,
        "main_sha": main,
        "retention": {"runs_days": run_days, "caches_days": cache_days,
                      "merged_branches_days": branch_days},
        "inventory": {"branches": len(branches), "open_prs": len(open_pulls),
                      "workflow_runs": len(runs), "action_caches": len(caches)},
        "candidates": {"runs": choose_runs(runs, cutoff(run_days)),
                       "caches": choose_caches(caches, cutoff(cache_days)),
                       "merged_branches": merged},
        "manual_review_branches": review,
        "protected": "main, open PRs, protected refs, archive/backup, unmerged or diverged refs, "
                     "latest five runs per workflow, successful main release gates, tags/assets"
    }


def execute(api, report, max_deletes):
    done = {"runs": [], "caches": [], "merged_branches": []}
    pending = [(key, row) for key in ("runs", "caches", "merged_branches")
               for row in report["candidates"][key]]
    # A bounded cleanup prevents unexpectedly deleting thousands of records at once.
    for key, row in pending[:max_deletes]:
        if key == "runs":
            path = f"repos/{api.repo}/actions/runs/{row['id']}"
        elif key == "caches":
            path = f"repos/{api.repo}/actions/caches/{row['id']}"
        else:
            path = f"repos/{api.repo}/git/refs/heads/{quote(row['name'], safe='/')}"
            # Recheck the branch tip immediately before removing it.
            fresh = api.call(f"repos/{api.repo}/branches/{quote(row['name'], safe='')}")
            if fresh.get("protected") or fresh["commit"]["sha"] != row["sha"]:
                raise RuntimeError(f"Branch changed since audit: {row['name']}")
        api.call(path, method="DELETE")
        done[key].append(row["id"] if key != "merged_branches" else row["name"])
    report["applied"] = done
    report["remaining_after_budget"] = max(0, len(pending) - max_deletes)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--confirmation", default="")
    parser.add_argument("--runs-days", type=int, default=14)
    parser.add_argument("--caches-days", type=int, default=7)
    parser.add_argument("--branches-days", type=int, default=7)
    parser.add_argument("--max-deletes", type=int, default=300)
    parser.add_argument("--output", default="ui-results/repository-cleanup-report.json")
    args = parser.parse_args(argv)
    if args.runs_days < 7 or args.caches_days < 3 or args.branches_days < 7:
        parser.error("Minimum retention: runs >=7d, caches >=3d, branches >=7d")
    if not 1 <= args.max_deletes <= 500:
        parser.error("1 <= max-deletes <= 500")
    if args.apply and (args.confirmation != CONFIRM or
                       os.getenv("GITHUB_EVENT_NAME") != "workflow_dispatch" or
                       os.getenv("GITHUB_REF_NAME") != "main" or
                       os.getenv("GITHUB_REPOSITORY") != REPO):
        parser.error("Deletion needs exact confirmation, manual workflow_dispatch on main")
    api = GitHub()
    result = plan(api, datetime.now(timezone.utc), args.runs_days,
                  args.caches_days, args.branches_days)
    if args.apply:
        execute(api, result, args.max_deletes)
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print("MODE:", "APPLY" if args.apply else "READ-ONLY AUDIT")
    print("INVENTORY:", result["inventory"])
    print("DELETE CANDIDATES:", {key: len(value) for key, value in result["candidates"].items()})
    print("Unmerged/differing branches for manual review:", len(result["manual_review_branches"]))


if __name__ == "__main__":
    main()
