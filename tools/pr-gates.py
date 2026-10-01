#!/usr/bin/env python3
"""Emit deterministic JSON for mechanical PR merge gates.

Run the copy checked out from trusted, current main. A PENDING CI result is not
a failure: exit 2 means wait, exit 1 means a failed/error gate, and exit 0
means all mechanical gates passed. Independent review of task scope, local
evidence, security, and governance remains mandatory before merge.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlencode


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_JOBS = ("ci / policy", "ci / build-linux", "ci / build-windows")
FROZEN = (
    "docs/planning/MASTER_SOFTWARE_PLAN.md",
    "docs/planning/FINAL_PLANNING_STATE.md",
)
# Additional identities require an explicit tracked-governance decision on main.
AUTHORIZED_LOGINS: frozenset[str] = frozenset()


class CommandError(Exception):
    pass


def run(*arguments: str) -> str:
    process = subprocess.run(
        arguments,
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if process.returncode:
        detail = process.stderr.strip() or process.stdout.strip() or f"exit {process.returncode}"
        raise CommandError(f"{arguments[0]} {arguments[1]}: {detail}")
    return process.stdout


def gh_json(endpoint: str) -> dict:
    data = json.loads(run("gh", "api", endpoint))
    if not isinstance(data, dict):
        raise CommandError(f"gh api {endpoint}: expected JSON object")
    return data


def ensure_commit(sha: str) -> None:
    try:
        run("git", "cat-file", "-e", f"{sha}^{{commit}}")
    except CommandError:
        # Fetch exact object IDs as data; never execute code from the PR head.
        run("git", "fetch", "--no-tags", "origin", sha)
        run("git", "cat-file", "-e", f"{sha}^{{commit}}")


def evaluate_jobs(jobs: list[dict], run_status: str, run_conclusion: str | None) -> tuple[str, dict[str, str]]:
    states: dict[str, str] = {}
    for name in EXPECTED_JOBS:
        matches = [job for job in jobs if job.get("name") == name]
        if len(matches) > 1:
            return "ERROR", states
        if not matches:
            states[name] = "MISSING"
        elif matches[0].get("status") != "completed":
            states[name] = "PENDING"
        else:
            states[name] = str(matches[0].get("conclusion") or "ERROR").upper()
    if run_status != "completed" or "PENDING" in states.values():
        return "PENDING", states
    if "MISSING" in states.values():
        return "ERROR", states
    if run_conclusion == "success" and all(value == "SUCCESS" for value in states.values()):
        return "PASS", states
    return "FAIL", states


def ci_for(repo: str, branch: str, sha: str) -> tuple[str, int | None, dict[str, str]]:
    query = urlencode({"head_sha": sha, "event": "pull_request", "branch": branch, "per_page": 100})
    payload = gh_json(f"repos/{repo}/actions/workflows/ci.yml/runs?{query}")
    runs = [
        item
        for item in payload.get("workflow_runs", [])
        if item.get("head_sha") == sha
        and item.get("head_branch") == branch
        and item.get("event") == "pull_request"
    ]
    if not runs:
        return "PENDING", None, {name: "MISSING" for name in EXPECTED_JOBS}
    latest = max(runs, key=lambda item: int(item["id"]))
    run_id = int(latest["id"])
    jobs_payload = gh_json(f"repos/{repo}/actions/runs/{run_id}/jobs?per_page=100")
    jobs = jobs_payload.get("jobs")
    if not isinstance(jobs, list):
        raise CommandError("GitHub jobs response is malformed")
    status, states = evaluate_jobs(jobs, str(latest.get("status")), latest.get("conclusion"))
    return status, run_id, states


def metadata_snapshot(pull: dict) -> dict:
    """Validate and capture the mechanical facts whose changes invalidate evidence."""
    try:
        base, head = pull["base"], pull["head"]
        base_repo, head_repo = base["repo"], head["repo"]
        snapshot = {
            "state": pull["state"],
            "draft": pull["draft"],
            "base_ref": base["ref"],
            "base_sha": base["sha"],
            "head_ref": head["ref"],
            "head_sha": head["sha"],
            "base_repository_id": base_repo["id"],
            "head_repository_id": head_repo["id"],
            "base_repository": base_repo["full_name"],
            "head_repository": head_repo["full_name"],
            "base_owner": base_repo["owner"]["login"],
            "author": pull["user"]["login"],
            "head_is_fork": head_repo["fork"],
        }
    except (KeyError, TypeError) as error:
        raise CommandError(f"PR metadata is incomplete: {error}") from error
    for name, value in snapshot.items():
        if name in ("draft", "head_is_fork"):
            valid = type(value) is bool
        elif name.endswith("_id"):
            valid = type(value) is int and value > 0
        else:
            valid = isinstance(value, str) and bool(value)
        if not valid:
            raise CommandError(f"PR metadata field {name} is missing or malformed")
    return snapshot


def evaluate(number: int) -> dict:
    result = {
        "pr": number,
        "head_sha": None,
        "base_sha": None,
        "metadata_snapshot": None,
        "base_ref": None,
        "base_ok": False,
        "author_login": None,
        "author_verified": False,
        "head_repository": None,
        "same_repo": False,
        "is_cross_repository": True,
        "is_draft": None,
        "expected_ci_jobs": list(EXPECTED_JOBS),
        "ci_status": "ERROR",
        "ci_run_id": None,
        "ci_jobs": {},
        "frozen_files_changed": None,
        "diff_check": "ERROR",
        "errors": [],
    }
    errors: list[str] = result["errors"]
    try:
        checkout_repo = json.loads(run("gh", "repo", "view", "--json", "nameWithOwner"))["nameWithOwner"]
        pull = gh_json(f"repos/{checkout_repo}/pulls/{number}")
        snapshot = metadata_snapshot(pull)
    except (CommandError, KeyError, TypeError, ValueError) as error:
        errors.append(f"PR metadata unavailable: {error}")
        return result

    base = pull.get("base") or {}
    head = pull.get("head") or {}
    base_repo = base.get("repo") or {}
    head_repo = head.get("repo") or {}
    repo = base_repo.get("full_name")
    base_id = base_repo.get("id")
    head_id = head_repo.get("id")
    sha = head.get("sha")
    base_sha = base.get("sha")
    branch = head.get("ref")
    owner = (base_repo.get("owner") or {}).get("login")
    author = (pull.get("user") or {}).get("login")

    result["head_sha"] = sha
    result["base_sha"] = base_sha
    result["metadata_snapshot"] = snapshot
    result["base_ref"] = base.get("ref")
    result["base_ok"] = base.get("ref") == "main"
    result["author_login"] = author
    result["author_verified"] = bool(author and (author == owner or author in AUTHORIZED_LOGINS))
    result["head_repository"] = head_repo.get("full_name")
    result["same_repo"] = bool(repo and head_repo.get("full_name") == repo and base_id == head_id)
    result["is_cross_repository"] = not result["same_repo"]
    result["is_draft"] = pull.get("draft")

    if pull.get("state") != "open":
        errors.append("PR is not open")
    if not result["base_ok"]:
        errors.append("PR does not target main")
    if not result["same_repo"]:
        errors.append("PR head repository differs from base repository")
    if head_repo.get("fork"):
        errors.append("PR originates from an external fork")
    if author in ("dependabot[bot]", "dependabot"):
        errors.append("Dependabot PR requires human review and merge")
    if not result["author_verified"]:
        errors.append("PR GitHub author is not an authorized identity")
    if result["is_draft"]:
        errors.append("PR is Draft")
    if not all(isinstance(value, str) and value for value in (repo, sha, base_sha, branch)):
        errors.append("PR base/head metadata is incomplete")
        return result

    try:
        ensure_commit(base_sha)
        ensure_commit(sha)
        changed = run("git", "diff", "--name-only", "--no-renames", base_sha, sha, "--", *FROZEN)
        result["frozen_files_changed"] = bool(changed.strip())
        if result["frozen_files_changed"]:
            errors.append("frozen planning source changed")
        check = subprocess.run(
            ("git", "diff", "--check", base_sha, sha),
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
        result["diff_check"] = "PASS" if check.returncode == 0 else "FAIL"
        if check.returncode:
            errors.append("git diff --check failed")
    except CommandError as error:
        errors.append(f"Git diff unavailable: {error}")

    try:
        status, run_id, states = ci_for(repo, branch, sha)
        result["ci_status"] = status
        result["ci_run_id"] = run_id
        result["ci_jobs"] = states
        if status in ("FAIL", "ERROR"):
            errors.append(f"CI {status.lower()}")
    except (CommandError, KeyError, TypeError, ValueError) as error:
        errors.append(f"CI unavailable: {error}")

    try:
        current = gh_json(f"repos/{repo}/pulls/{number}")
        current_snapshot = metadata_snapshot(current)
        changed_fields = sorted(
            name for name in snapshot if current_snapshot[name] != snapshot[name]
        )
        if changed_fields:
            errors.append(
                "PR metadata changed during gate evaluation: " + ", ".join(changed_fields)
            )
    except (CommandError, ValueError) as error:
        errors.append(f"PR head recheck unavailable: {error}")
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pr_number", type=int)
    args = parser.parse_args()
    if args.pr_number < 1:
        parser.error("PR number must be positive")
    result = evaluate(args.pr_number)
    print(json.dumps(result, indent=2, sort_keys=True))
    if result["errors"] or result["ci_status"] in ("FAIL", "ERROR"):
        return 1
    if result["ci_status"] == "PENDING":
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
