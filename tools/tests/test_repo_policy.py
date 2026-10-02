"""Regression tests for deterministic repository/document policy validation."""

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "repo-policy.py"
SPEC = importlib.util.spec_from_file_location("hris_repo_policy", SCRIPT)
POLICY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(POLICY)


class RepoPolicyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "docs/tasks").mkdir(parents=True)
        (self.root / "docs/implementation/tasks").mkdir(parents=True)
        (self.root / "docs/reference").mkdir(parents=True)
        (self.root / "docs/reference/guide.md").write_text("# Guide\n\n## Safe Mode\n", encoding="utf-8")
        (self.root / "docs/tasks/TASK-0001.md").write_text(
            "# TASK-0001 — Example\n\n"
            "Parent implementation item: [IMP-001](../implementation/tasks/IMP-001.md)\n\n"
            "See the [guide](../reference/guide.md#safe-mode).\n",
            encoding="utf-8",
        )
        (self.root / "docs/implementation/tasks/IMP-001.md").write_text(
            "# IMP-001 — Example parent\n\n"
            "Child: [TASK-0001](../../tasks/TASK-0001.md)\n",
            encoding="utf-8",
        )
        (self.root / "docs/tasks/README.md").write_text(
            "# Tasks\n\n| ID | Parent |\n| --- | --- |\n"
            "| [TASK-0001](TASK-0001.md) | [IMP-001](../implementation/tasks/IMP-001.md) |\n",
            encoding="utf-8",
        )

    def tearDown(self):
        self.temp.cleanup()

    def errors(self):
        return POLICY.validate(self.root)[0]

    def test_valid_repository_policy(self):
        self.assertEqual([], self.errors())

    def test_broken_local_link_and_anchor_fail(self):
        task = self.root / "docs/tasks/TASK-0001.md"
        task.write_text(
            task.read_text(encoding="utf-8")
            + "\n[missing](../reference/missing.md) [bad](../reference/guide.md#not-there)\n",
            encoding="utf-8",
        )
        errors = self.errors()
        self.assertTrue(any("broken local link" in error for error in errors))
        self.assertTrue(any("missing anchor" in error for error in errors))

    def test_task_identity_must_match_filename(self):
        task = self.root / "docs/tasks/TASK-0001.md"
        task.write_text(task.read_text(encoding="utf-8").replace("TASK-0001 —", "TASK-0002 —", 1), encoding="utf-8")
        self.assertTrue(any("first heading must identify TASK-0001" in error for error in self.errors()))

    def test_parent_imp_requires_backlink(self):
        imp = self.root / "docs/implementation/tasks/IMP-001.md"
        imp.write_text("# IMP-001 — Example parent\n", encoding="utf-8")
        self.assertTrue(any("missing backlink" in error for error in self.errors()))

    def test_task_index_entry_is_required(self):
        index = self.root / "docs/tasks/README.md"
        index.write_text("# Tasks\n", encoding="utf-8")
        self.assertTrue(any("missing canonical index entry" in error for error in self.errors()))

    def test_duplicate_heading_anchors_follow_github_suffixes(self):
        guide = self.root / "docs/reference/guide.md"
        guide.write_text("# Guide\n\n## Repeat\n\n## Repeat\n", encoding="utf-8")
        task = self.root / "docs/tasks/TASK-0001.md"
        task.write_text(
            task.read_text(encoding="utf-8").replace("#safe-mode", "#repeat-1"),
            encoding="utf-8",
        )
        self.assertEqual([], self.errors())


if __name__ == "__main__":
    unittest.main()
