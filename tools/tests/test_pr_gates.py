"""Synthetic regression cases for the trusted-main mechanical PR gate helper."""

import copy
import importlib.util
import json
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch


SCRIPT = Path(__file__).resolve().parents[1] / "pr-gates.py"
SPEC = importlib.util.spec_from_file_location("hris_pr_gates", SCRIPT)
GATES = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATES)


def pull():
    repo = {"full_name": "owner/hris", "id": 123, "owner": {"login": "owner"}, "fork": False}
    return {
        "state": "open", "draft": False, "user": {"login": "owner"},
        "base": {"ref": "main", "sha": "a" * 40, "repo": copy.deepcopy(repo)},
        "head": {"ref": "task", "sha": "b" * 40, "repo": copy.deepcopy(repo)},
    }


def jobs(names=GATES.EXPECTED_JOBS, overrides=None):
    overrides = overrides or {}
    return [
        {"name": name, "status": "completed", "conclusion": overrides.get(name, "success")}
        for name in names
    ]


def mocked_evaluation(first=None, second=None, ci_status="PASS"):
    first = copy.deepcopy(first if first is not None else pull())
    second = copy.deepcopy(second if second is not None else first)
    responses = [first, second]

    def fake_run(*args):
        if args[:3] == ("gh", "repo", "view"):
            return json.dumps({"nameWithOwner": "owner/hris"})
        if args[:3] == ("git", "diff", "--name-only"):
            return ""
        raise AssertionError(f"unexpected command: {args!r}")

    with patch.object(GATES, "run", side_effect=fake_run), \
         patch.object(GATES, "gh_json", side_effect=lambda _: responses.pop(0)), \
         patch.object(GATES, "ensure_commit"), \
         patch.object(GATES, "ci_for", return_value=(ci_status, 42, {})), \
         patch.object(GATES.subprocess, "run", return_value=SimpleNamespace(returncode=0)):
        return GATES.evaluate(99)


class RequiredJobTests(unittest.TestCase):
    def test_three_required_jobs_pass(self):
        self.assertEqual(GATES.EXPECTED_JOBS,
                         ("ci / policy", "ci / build-linux", "ci / build-windows"))
        self.assertEqual(GATES.evaluate_jobs(jobs(), "completed", "success")[0], "PASS")

    def test_missing_duplicate_and_non_success_required_jobs(self):
        for omitted in GATES.EXPECTED_JOBS:
            with self.subTest(omitted=omitted):
                actual = [name for name in GATES.EXPECTED_JOBS if name != omitted]
                self.assertEqual(GATES.evaluate_jobs(jobs(actual), "completed", "success")[0], "ERROR")
        self.assertEqual(GATES.evaluate_jobs(jobs() + [jobs()[0]], "completed", "success")[0], "ERROR")
        for name in GATES.EXPECTED_JOBS:
            for conclusion in ("failure", "cancelled", "skipped", "timed_out", None):
                with self.subTest(name=name, conclusion=conclusion):
                    self.assertEqual(
                        GATES.evaluate_jobs(jobs(overrides={name: conclusion}), "completed", "success")[0],
                        "FAIL",
                    )

    def test_pending_is_not_failure_or_pass(self):
        pending = jobs()
        pending[0]["status"] = "in_progress"
        self.assertEqual(GATES.evaluate_jobs(pending, "in_progress", None)[0], "PENDING")
        self.assertEqual(GATES.evaluate_jobs(jobs(), "queued", None)[0], "PENDING")

    def test_run_failure_cannot_pass_green_jobs(self):
        self.assertEqual(GATES.evaluate_jobs(jobs(), "completed", "failure")[0], "FAIL")

    def test_ci_for_uses_exact_sha_branch_and_event(self):
        exact = {"id": 55, "head_sha": "b" * 40, "head_branch": "task",
                 "event": "pull_request", "status": "completed", "conclusion": "success"}
        run_payload = {"workflow_runs": [
            {**exact, "id": 99, "head_branch": "other"},
            {**exact, "id": 98, "head_sha": "c" * 40},
            {**exact, "id": 97, "event": "push"}, exact,
        ]}

        def api(endpoint):
            if "/runs?" in endpoint:
                return run_payload
            self.assertIn("/runs/55/jobs", endpoint)
            return {"jobs": jobs()}

        with patch.object(GATES, "gh_json", side_effect=api):
            status, run_id, states = GATES.ci_for("owner/hris", "task", "b" * 40)
        self.assertEqual((status, run_id), ("PASS", 55))
        self.assertEqual(set(states), set(GATES.EXPECTED_JOBS))


class MetadataTests(unittest.TestCase):
    def test_valid_snapshot(self):
        original = pull()
        snapshot = GATES.metadata_snapshot(original)
        self.assertEqual(snapshot["base_sha"], "a" * 40)
        self.assertEqual(snapshot["head_sha"], "b" * 40)
        self.assertIs(snapshot["draft"], False)

    def test_required_metadata_rejects_missing_and_wrong_types(self):
        paths = (
            ("draft",), ("base", "repo", "id"), ("head", "repo", "id"),
            ("base", "sha"), ("head", "sha"), ("user", "login"),
            ("head", "repo", "fork"),
        )
        for path in paths:
            for malformed in (None, "missing"):
                with self.subTest(path=path, malformed=malformed):
                    changed = pull()
                    target = changed
                    for part in path[:-1]:
                        target = target[part]
                    if malformed is None:
                        target.pop(path[-1])
                    else:
                        target[path[-1]] = "" if path[-1] != "id" else "123"
                    with self.assertRaises(GATES.CommandError):
                        GATES.metadata_snapshot(changed)

    def test_boolean_ids_are_not_valid_integer_identity(self):
        changed = pull()
        changed["head"]["repo"]["id"] = True
        with self.assertRaises(GATES.CommandError):
            GATES.metadata_snapshot(changed)

    def test_ineligible_origin_author_target_state_and_draft(self):
        cases = []
        for modify in (
            lambda x: x["head"]["repo"].update(id=124, full_name="fork/hris", fork=True),
            lambda x: x["head"]["repo"].update(fork=True),
            lambda x: x["user"].update(login="dependabot[bot]"),
            lambda x: x["user"].update(login="unapproved"),
            lambda x: x["base"].update(ref="release"),
            lambda x: x.update(state="closed"),
            lambda x: x.update(draft=True),
        ):
            changed = pull()
            modify(changed)
            cases.append(changed)
        for changed in cases:
            with self.subTest(changed=changed):
                self.assertTrue(mocked_evaluation(changed)["errors"])

    def test_changes_during_evaluation_invalidate_gates(self):
        paths_and_values = (
            (("head", "sha"), "c" * 40),
            (("base", "sha"), "c" * 40),
            (("base", "ref"), "release"),
            (("head", "ref"), "renamed"),
            (("draft",), True),
            (("state",), "closed"),
            (("user", "login"), "different"),
            (("head", "repo", "id"), 456),
        )
        for path, value in paths_and_values:
            with self.subTest(path=path):
                changed = pull()
                target = changed
                for part in path[:-1]:
                    target = target[part]
                target[path[-1]] = value
                result = mocked_evaluation(pull(), changed)
                self.assertTrue(any("metadata changed" in error for error in result["errors"]))

    def test_invalid_final_metadata_fails_closed(self):
        changed = pull()
        changed.pop("draft")
        result = mocked_evaluation(pull(), changed)
        self.assertTrue(any("metadata" in error for error in result["errors"]))

    def test_green_valid_and_pending_are_distinct(self):
        valid = mocked_evaluation()
        self.assertEqual((valid["errors"], valid["ci_status"]), ([], "PASS"))
        pending = mocked_evaluation(ci_status="PENDING")
        self.assertEqual((pending["errors"], pending["ci_status"]), ([], "PENDING"))


if __name__ == "__main__":
    unittest.main()
