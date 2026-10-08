"""Synthetic regression cases for read-only, non-authoritative task packets."""

import copy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


TOOLS = Path(__file__).resolve().parents[1]
SCRIPT = TOOLS / "task-context.py"


def load_tool(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


CONTEXT = load_tool("hris_task_context", SCRIPT)
INDEXER = load_tool("hris_context_test_indexer", TOOLS / "plan-index.py")
TASK = "docs/tasks/TASK-0020.md"
IMP = "docs/implementation/tasks/IMP-088.md"
MANIFEST = "docs/tasks/TASK-0020.context.json"
MASTER = "docs/planning/MASTER_SOFTWARE_PLAN.md"
FINAL = "docs/planning/FINAL_PLANNING_STATE.md"
INDEX = "docs/planning/INDEX.md"
GOVERNANCE = (
    "AGENTS.md",
    "docs/implementation/EXECUTION_RULES.md",
    "docs/implementation/REPOSITORY_WORKFLOW.md",
    "docs/implementation/GIT_INTEGRATION_AGENT.md",
    "docs/implementation/IMPLEMENTATION_STATE.md",
)
TASK_TEXT = (
    "# TASK-0020 - Synthetic context packet\n\n"
    "Parent implementation item: [IMP-088](../implementation/tasks/IMP-088.md)\n\n"
    "Read D-140 and the [focused guide](../guide.md).\n"
)
IMP_TEXT = (
    "# IMP-088 - Synthetic tooling work\n\n"
    "Current work order: [TASK-0020](../../tasks/TASK-0020.md).\n"
)
MANIFEST_DATA = {
    "version": 1,
    "task": "TASK-0020",
    "imp": "IMP-088",
    "decisions": ["D-140"],
    "references": ["docs/guide.md"],
}


class TaskContextTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="hris-context-test-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "repo"
        self.root.mkdir()
        self.write(TASK, TASK_TEXT)
        self.write(IMP, IMP_TEXT)
        self.write("docs/guide.md", "# Synthetic guide\n\nOnly a routing reference.\n")
        for source in GOVERNANCE:
            self.write(source, "# Synthetic governance\n\nSource: " + source + "\n")
        self.write(MASTER, (
            "# Synthetic master\n\n"
            "### D-140 - Indexed documentation\n\n"
            "Status: DECIDED\n\n"
            "Retrieve only the requested decision.\n\n"
            "### D-141 - Explicit extra decision\n\n"
            "Status: DECIDED\n\n"
            "This decision is requested explicitly.\n"
        ))
        self.write(FINAL, (
            "# Synthetic final state\n\n"
            "### D-140 - Indexed documentation\n\n"
            "Status: DECIDED\n\n"
            "Master takes precedence over this duplicate.\n"
        ))
        self.regenerate_index()
        self.write_manifest()
        self.git("init", "--quiet", "--initial-branch=context-fixture")
        self.git("config", "core.autocrlf", "false")
        self.git("config", "user.name", "Synthetic Context Test")
        self.git("config", "user.email", "synthetic@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        self.git("config", "core.hooksPath", str(self.root / "absent-hooks"))
        self.git("add", ".")
        self.git("commit", "--quiet", "-m", "Synthetic context fixture")

    def write(self, source, content):
        path = self.root / source
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content.encode("utf-8") if isinstance(content, str) else content)

    def write_manifest(self, data=None):
        self.write(MANIFEST, json.dumps(MANIFEST_DATA if data is None else data) + "\n")

    def regenerate_index(self):
        with patch.object(INDEXER, "ROOT", self.root):
            self.write(INDEX, INDEXER.generate())

    def git(self, *arguments):
        result = subprocess.run(
            ["git", "-C", str(self.root), *arguments],
            check=True, capture_output=True, text=True, encoding="utf-8",
        )
        return result.stdout.strip()

    def packet(self, decisions=None):
        return CONTEXT.build_packet(self.root, "TASK-0020", decisions or [])

    def cli(self, *arguments, repo=None):
        return subprocess.run(
            [sys.executable, "-B", str(SCRIPT), *arguments,
             "--repo", str(self.root if repo is None else repo)],
            cwd=self.root, capture_output=True, text=True, encoding="utf-8",
        )

    def assert_cli_failure(self, *arguments):
        result = self.cli(*(arguments or ("TASK-0020",)))
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertEqual(result.stdout, "", "failure must not emit a partial packet")
        self.assertTrue(result.stderr.strip())

    def assert_reference(self, reference, *, text=False, decision=False):
        expected = {"source", "start_line", "end_line", "byte_count", "sha256"}
        if text:
            expected.add("text")
        if decision:
            expected.update(("id", "title", "indexed_sha256"))
        self.assertEqual(set(reference), expected)
        raw = (self.root / reference["source"]).read_bytes()
        self.assertGreaterEqual(reference["start_line"], 1)
        self.assertGreaterEqual(reference["end_line"], reference["start_line"])
        excerpt = b"".join(raw.splitlines(keepends=True)[
            reference["start_line"] - 1:reference["end_line"]
        ])
        self.assertEqual(reference["byte_count"], len(excerpt))
        self.assertEqual(reference["sha256"], hashlib.sha256(excerpt).hexdigest())
        if decision:
            normalized = excerpt.decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")
            self.assertEqual(reference["indexed_sha256"],
                             hashlib.sha256(normalized.encode("utf-8")).hexdigest())

    def directory_link(self, link, target):
        try:
            link.symlink_to(target, target_is_directory=True)
        except OSError:
            if os.name != "nt":
                raise
            # Windows junctions exercise the same resolved-path boundary without
            # requiring the developer-mode privilege needed by symbolic links.
            subprocess.run(
                ["cmd", "/c", "mklink", "/J", str(link), str(target)],
                check=True, capture_output=True,
            )

    def test_manifest_packet_contains_only_selected_excerpts_and_hashed_references(self):
        packet = self.packet()
        self.assertEqual(set(packet), {
            "version", "notice", "git", "task", "imp", "governance", "routing",
            "references", "planning_index", "decisions",
        })
        self.assertEqual(packet["version"], 1)
        self.assertIn("non-authoritative", packet["notice"].lower())
        self.assertIn("authorize", packet["notice"].lower())
        self.assertEqual(packet["task"]["source"], TASK)
        self.assertEqual(packet["task"]["text"], TASK_TEXT)
        self.assertEqual(packet["imp"]["source"], IMP)
        self.assertEqual(packet["imp"]["text"], IMP_TEXT)
        self.assert_reference(packet["task"], text=True)
        self.assert_reference(packet["imp"], text=True)
        self.assertEqual({ref["source"] for ref in packet["governance"]}, set(GOVERNANCE))
        for reference in packet["governance"] + packet["references"]:
            self.assert_reference(reference)
        self.assertEqual([ref["source"] for ref in packet["references"]], ["docs/guide.md"])
        self.assertEqual(packet["routing"]["source"], MANIFEST)
        self.assert_reference(packet["routing"])
        self.assertEqual(packet["planning_index"]["source"], INDEX)
        self.assert_reference(packet["planning_index"])
        self.assertEqual([entry["id"] for entry in packet["decisions"]], ["D-140"])
        decision = packet["decisions"][0]
        self.assert_reference(decision, text=True, decision=True)
        self.assertEqual(decision["source"], MASTER)
        self.assertNotIn("D-141", decision["text"])
        self.assertNotIn("Synthetic master", decision["text"])
        self.assertNotIn("Only a routing reference", json.dumps(packet))

    def test_packet_is_deterministic_and_read_only(self):
        before = {path.relative_to(self.root): path.read_bytes()
                  for path in self.root.rglob("*") if path.is_file() and ".git" not in path.parts}
        first = self.packet(["D-141"])
        self.assertEqual(first, self.packet(["D-141"]))
        after = {path.relative_to(self.root): path.read_bytes()
                 for path in self.root.rglob("*") if path.is_file() and ".git" not in path.parts}
        self.assertEqual(before, after)
        self.assertEqual(self.git("status", "--porcelain"), "")

    def test_git_identity_and_dirty_worktree_are_reported(self):
        packet = self.packet()
        self.assertEqual(packet["git"]["head"], self.git("rev-parse", "HEAD"))
        self.assertEqual(packet["git"]["branch"], "context-fixture")
        self.assertIs(packet["git"]["dirty"], False)
        self.write("untracked.txt", "Synthetic untracked change\n")
        self.assertIs(self.packet()["git"]["dirty"], True)
        (self.root / "untracked.txt").unlink()
        self.write("docs/guide.md", "# Changed synthetic guide\n")
        self.assertIs(self.packet()["git"]["dirty"], True)

    def test_cli_outputs_one_json_object_and_no_diagnostics_on_success(self):
        result = self.cli("TASK-0020", "D-141")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        self.assertEqual(json.loads(result.stdout), self.packet(["D-141"]))

    def test_without_manifest_accepts_explicit_decisions(self):
        (self.root / MANIFEST).unlink()
        packet = self.packet(["D-141"])
        self.assertIsNone(packet["routing"])
        self.assertEqual(packet["references"], [])
        self.assertEqual([entry["id"] for entry in packet["decisions"]], ["D-141"])

    def test_no_decisions_needs_no_planning_index_or_frozen_source(self):
        for source in (MANIFEST, INDEX, MASTER, FINAL):
            (self.root / source).unlink()
        packet = self.packet()
        self.assertEqual(packet["decisions"], [])
        self.assertIsNone(packet["planning_index"])

    def test_requested_decisions_and_references_are_deduplicated(self):
        manifest = copy.deepcopy(MANIFEST_DATA)
        manifest["decisions"] *= 2
        manifest["references"] *= 2
        self.write_manifest(manifest)
        packet = self.packet(["D-140", "D-141", "D-141"])
        self.assertEqual([entry["id"] for entry in packet["decisions"]], ["D-140", "D-141"])
        self.assertEqual([ref["source"] for ref in packet["references"]], ["docs/guide.md"])

    def test_raw_crlf_hashes_are_distinct_from_indexed_normalized_hashes(self):
        for source in (TASK, IMP, MASTER, "docs/guide.md", GOVERNANCE[0]):
            self.write(source, (self.root / source).read_bytes().replace(b"\n", b"\r\n"))
        packet = self.packet()
        for reference in (packet["task"], packet["imp"]):
            self.assert_reference(reference, text=True)
        for reference in packet["governance"] + packet["references"]:
            self.assert_reference(reference)
        decision = packet["decisions"][0]
        self.assert_reference(decision, text=True, decision=True)
        self.assertNotEqual(decision["sha256"], decision["indexed_sha256"])

    def test_exact_task_and_decision_ids_are_required(self):
        for task in ("TASK-20", "TASK-0020.md", "task-0020", "../TASK-0020", "TASK-0020 "):
            with self.subTest(task=task):
                with self.assertRaises((OSError, ValueError)):
                    CONTEXT.build_packet(self.root, task, [])
                self.assert_cli_failure(task)
        for decision in ("D-14", "D-0140", "d-140", "Indexed documentation", "D-140 "):
            with self.subTest(decision=decision):
                with self.assertRaises((OSError, ValueError)):
                    self.packet([decision])

    def test_missing_task_imp_or_required_governance_fails(self):
        for source in (TASK, IMP, *GOVERNANCE):
            with self.subTest(source=source):
                original = (self.root / source).read_bytes()
                (self.root / source).unlink()
                try:
                    with self.assertRaises((OSError, ValueError)):
                        self.packet()
                finally:
                    self.write(source, original)

    def test_empty_wrong_and_duplicate_identity_headings_fail(self):
        cases = (
            (TASK, ""), (TASK, TASK_TEXT.replace("# TASK-0020", "# TASK-0021", 1)),
            (TASK, TASK_TEXT.replace("# TASK-0020", "# TASK-00200", 1)),
            (TASK, TASK_TEXT.replace("# TASK-0020 -", "# TASK-0020", 1)),
            (TASK, TASK_TEXT + "\n# TASK-0020 - Duplicate identity\n"),
            (TASK, TASK_TEXT + "\n# TASK-0021 - Conflicting identity\n"),
            (TASK, TASK_TEXT + "\n ## TASK-0021 - Indented conflicting identity\n"),
            (TASK, TASK_TEXT + "\n  ## TASK-0020 – Indented duplicate identity\n"),
            (IMP, ""), (IMP, IMP_TEXT.replace("# IMP-088", "# IMP-089", 1)),
            (IMP, IMP_TEXT.replace("# IMP-088 -", "# IMP-088", 1)),
            (IMP, IMP_TEXT + "\n# IMP-088 - Duplicate identity\n"),
            (IMP, IMP_TEXT + "\n# IMP-089 - Conflicting identity\n"),
            (IMP, IMP_TEXT + "\n   ## IMP-088 - Indented duplicate identity\n"),
            (IMP, IMP_TEXT + "\n ### IMP-089 — Indented conflicting identity\n"),
        )
        for source, content in cases:
            with self.subTest(source=source, content=content):
                self.write(source, content)
                try:
                    with self.assertRaises((OSError, ValueError)):
                        self.packet()
                finally:
                    self.write(source, TASK_TEXT if source == TASK else IMP_TEXT)

    def test_same_id_report_heading_is_not_an_identity(self):
        self.write(IMP, IMP_TEXT.replace("# IMP-088 -", "# IMP-088 —", 1)
                   + "\n# IMP-088 Implementation Result\n")
        self.assertEqual(self.packet()["imp"]["source"], IMP)

    def test_malformed_first_line_identity_fails_without_partial_cli_output(self):
        self.write(IMP, IMP_TEXT.replace("# IMP-088 -", "# IMP-088", 1)
                   + "\n# IMP-088 — Later canonical identity\n")
        self.assert_cli_failure()

    def test_parent_link_missing_duplicate_or_mismatched_identity_fails(self):
        parent = "Parent implementation item: [IMP-088](../implementation/tasks/IMP-088.md)"
        cases = (
            TASK_TEXT.replace(parent, ""),
            TASK_TEXT + "\n" + parent + "\n",
            TASK_TEXT + "\n " + parent + "\n",
            TASK_TEXT.replace("[IMP-088]", "[IMP-089]"),
            TASK_TEXT.replace("tasks/IMP-088.md", "tasks/IMP-089.md"),
            TASK_TEXT.replace("../implementation/tasks/IMP-088.md", "../../outside/IMP-088.md"),
        )
        for content in cases:
            with self.subTest(content=content):
                self.write(TASK, content)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()

    def test_missing_and_mismatched_imp_backlinks_fail(self):
        for content in (
            "# IMP-088 - Synthetic tooling work\n",
            IMP_TEXT.replace("../../tasks/TASK-0020.md", "../../tasks/TASK-0021.md"),
            IMP_TEXT.replace("[TASK-0020]", "[TASK-0021]"),
        ):
            with self.subTest(content=content):
                self.write(IMP, content)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()

    def test_repeated_valid_backlink_is_not_duplicate_identity(self):
        self.write(IMP, IMP_TEXT + "\nSee [TASK-0020 - Current](../../tasks/TASK-0020.md) again.\n")
        self.assertEqual(self.packet()["imp"]["source"], IMP)

    def test_manifest_malformed_json_duplicate_keys_and_schema_fail(self):
        malformed = (
            "{", "[]", "null", '{"version":1,"version":1}',
            json.dumps(MANIFEST_DATA)[:-1] + ',"task":"TASK-0020"}',
        )
        for content in malformed:
            with self.subTest(content=content):
                self.write(MANIFEST, content)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()
        cases = []
        for field in MANIFEST_DATA:
            missing = copy.deepcopy(MANIFEST_DATA)
            missing.pop(field)
            cases.append(missing)
        for field, value in (
            ("version", 2), ("version", True), ("task", "TASK-0021"), ("imp", "IMP-089"),
            ("decisions", "D-140"), ("decisions", [1]), ("references", "docs/guide.md"),
            ("references", [None]), ("unknown", "silently expanded scope"),
        ):
            changed = copy.deepcopy(MANIFEST_DATA)
            changed[field] = value
            cases.append(changed)
        for manifest in cases:
            with self.subTest(manifest=manifest):
                self.write_manifest(manifest)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()

    def test_manifest_cannot_add_unmentioned_decisions_or_unlinked_references(self):
        self.write("docs/unlinked.md", "# Synthetic unrelated document\n")
        for field, values in (("decisions", ["D-141"]), ("references", ["docs/unlinked.md"])):
            with self.subTest(field=field):
                manifest = copy.deepcopy(MANIFEST_DATA)
                manifest[field] = values
                self.write_manifest(manifest)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()

    def test_reference_paths_must_be_canonical_repository_files(self):
        for source in (
            "../outside.md", "/outside.md", "C:/outside.md", "docs\\guide.md",
            "docs/../docs/guide.md", "./docs/guide.md", "docs/guide.md:stream",
            "docs/guide.md#section", "docs/missing.md", "docs", "",
        ):
            with self.subTest(source=source):
                manifest = copy.deepcopy(MANIFEST_DATA)
                manifest["references"] = [source]
                self.write_manifest(manifest)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()

    def test_missing_linked_reference_fails(self):
        (self.root / "docs/guide.md").unlink()
        with self.assertRaises((OSError, ValueError)):
            self.packet()

    def test_reference_directory_symlink_escape_fails(self):
        outside = Path(self.temporary.name) / "outside"
        outside.mkdir()
        (outside / "note.md").write_text("Synthetic outside data\n", encoding="utf-8")
        self.directory_link(self.root / "docs/escape", outside)
        self.write(TASK, TASK_TEXT + "\n[Outside](../escape/note.md)\n")
        manifest = copy.deepcopy(MANIFEST_DATA)
        manifest["references"] = ["docs/escape/note.md"]
        self.write_manifest(manifest)
        with self.assertRaises((OSError, ValueError)):
            self.packet()
        self.assert_cli_failure()

    def test_planning_directory_symlink_escape_fails(self):
        outside = Path(self.temporary.name) / "outside-planning"
        (self.root / "docs/planning").rename(outside)
        self.directory_link(self.root / "docs/planning", outside)
        with self.assertRaises((OSError, ValueError)):
            self.packet()

    def test_unknown_decision_fails_without_partial_cli_output(self):
        with self.assertRaises((OSError, ValueError)):
            self.packet(["D-999"])
        self.assert_cli_failure("TASK-0020", "D-140", "D-999")

    def test_stale_decision_content_heading_and_range_fail(self):
        original = (self.root / MASTER).read_text(encoding="utf-8")
        cases = (
            original.replace("Retrieve only", "Changed content: retrieve only"),
            original.replace("D-140 - Indexed documentation", "D-142 - Indexed documentation"),
            original.replace("D-140 - Indexed documentation", "D-140 - Changed title"),
            "# Source truncated\n",
        )
        for content in cases:
            with self.subTest(content=content):
                self.write(MASTER, content)
                with self.assertRaises((OSError, ValueError)):
                    self.packet()
                self.assert_cli_failure()

    def test_missing_index_source_and_malformed_index_fail(self):
        for source in (INDEX, MASTER):
            with self.subTest(source=source):
                original = (self.root / source).read_bytes()
                (self.root / source).unlink()
                try:
                    with self.assertRaises((OSError, ValueError)):
                        self.packet()
                    self.assert_cli_failure()
                finally:
                    self.write(source, original)
        self.write(INDEX, "# Invalid index\n")
        with self.assertRaises((OSError, ValueError)):
            self.packet()

    def test_stale_index_digest_fails(self):
        content = (self.root / INDEX).read_text(encoding="utf-8")
        rows = content.splitlines()
        fields = rows[8].split("|")
        fields[7] = " " + "0" * 64 + " "
        rows[8] = "|".join(fields)
        self.write(INDEX, "\n".join(rows) + "\n")
        with self.assertRaises((OSError, ValueError)):
            self.packet()

    def test_invalid_utf8_fails_without_partial_output(self):
        self.write(TASK, b"# TASK-0020 - Invalid encoding\n\xff")
        self.assert_cli_failure()

    def test_target_repository_cannot_supply_executable_planning_tool(self):
        marker = self.root / "target-code-executed"
        hostile = (
            "from pathlib import Path\n"
            f"Path({str(marker)!r}).write_text('executed', encoding='utf-8')\n"
            "raise RuntimeError('target repository code must not execute')\n"
        )
        self.write("tools/plan-get.py", hostile)
        self.assertEqual(self.packet()["decisions"][0]["id"], "D-140")
        result = self.cli("TASK-0020")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(marker.exists())

    def test_git_status_cannot_execute_repository_clean_filter(self):
        marker = self.root / "filter-executed"
        script = self.root / "hostile-filter.py"
        self.write("hostile-filter.py", (
            "from pathlib import Path\n"
            "import sys\n"
            f"Path({str(marker)!r}).write_text('executed', encoding='utf-8')\n"
            "sys.stdout.buffer.write(sys.stdin.buffer.read())\n"
        ))
        self.write(".gitattributes", "docs/guide.md filter=context_test\n")
        self.git("add", ".gitattributes")
        self.git("commit", "--quiet", "-m", "Synthetic filter attribute")
        self.git("config", "filter.context_test.clean", f'"{sys.executable}" "{script}"')
        guide = self.root / "docs/guide.md"
        original = guide.read_bytes()
        self.write("docs/guide.md", original.replace(b"Only", b"Also"))
        self.assertEqual(len(guide.read_bytes()), len(original))
        # Force a changed stat even on filesystems with coarse timestamp precision.
        stamp = guide.stat().st_mtime + 2
        os.utime(guide, (stamp, stamp))
        packet = self.packet()
        self.assertIs(packet["git"]["dirty"], True)
        self.assertFalse(marker.exists())
        result = self.cli("TASK-0020")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(marker.exists())

    def test_v2_packet_preserves_shape_hashes_determinism_and_read_only_behavior(self):
        from test_task_schema import contract_text
        legacy = self.packet()
        self.write(TASK, contract_text() + '\nRead D-140 and the [focused guide](../guide.md).\n')
        before = self.git("status", "--porcelain")
        packet = self.packet()
        self.assertEqual(set(packet), set(legacy))
        self.assertEqual(packet, self.packet())
        self.assertEqual(packet["version"], 1)
        self.assert_reference(packet["task"], text=True)
        self.assert_reference(packet["imp"], text=True)
        self.assertEqual(self.git("status", "--porcelain"), before)

    def test_validate_only_cli_reports_v2_and_legacy_without_changing_packets(self):
        from test_task_schema import contract_text, METADATA
        before = self.packet()
        result = self.cli("TASK-0020", "--validate-only")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout), {"task": "TASK-0020", "imp": "IMP-088",
                                                    "schema": "legacy", "metadata": None})
        self.assertEqual(self.packet(), before)
        self.write(TASK, contract_text() + '\nRead D-140 and the [focused guide](../guide.md).\n')
        result = self.cli("TASK-0020", "--validate-only")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        self.assertEqual(json.loads(result.stdout), {"task": "TASK-0020", "imp": "IMP-088",
                                                    "schema": 2, "metadata": METADATA})
        self.assertEqual(self.cli("TASK-0020", "--validate-only").stdout, result.stdout)

    def test_malformed_v2_fails_cli_without_partial_json_in_both_modes(self):
        from test_task_schema import contract_text, METADATA
        for text in (contract_text({**METADATA, "owner": "HUMAN"}),
                     contract_text({**METADATA, "imp": "IMP-089"}),
                     contract_text().replace('"schema": 2', '"schema": 2, "schema": 2'),
                     contract_text().replace("## Invariants", "## Missing section")):
            with self.subTest(text=text[:130]):
                self.write(TASK, text)
                self.assert_cli_failure("TASK-0020")
                self.assert_cli_failure("TASK-0020", "--validate-only")

    def test_container_schema_and_list_example_fail_cli_in_both_modes(self):
        from test_task_schema import contract_text
        text = contract_text()
        start = text.index("## Objective")
        cases = []
        for prefix in ("> ", "- ", "1. "):
            cases.extend((
                text.replace('"schema": 2', '"schema":').replace(
                    "```task-schema-v2", prefix + "```task-schema-v2"),
                text[:start].replace("```task-schema-v2", prefix + "```task-schema-v2"),
                text + "\n" + prefix + "```task-schema-v2\n{}\n```\n",
            ))
        cases.append(text[:start] + "- ```text\n"
                     + "\n".join("  " + line for line in text[start:].splitlines())
                     + "\n  ```\n")
        for number, content in enumerate(cases):
            with self.subTest(case=number):
                self.write(TASK, content + '\nRead D-140 and the [focused guide](../guide.md).\n')
                self.assert_cli_failure("TASK-0020", "--validate-only")
                self.assert_cli_failure("TASK-0020")

    def test_v2_still_requires_existing_parent_and_consistent_backlink(self):
        from test_task_schema import contract_text
        self.write(TASK, contract_text())
        self.write(IMP, "# IMP-088 - Synthetic parent without backlink\n")
        self.assert_cli_failure("TASK-0020", "--validate-only")
        (self.root / IMP).unlink()
        self.assert_cli_failure("TASK-0020", "--validate-only")

    def test_target_repository_cannot_supply_executable_task_validator(self):
        from test_task_schema import contract_text
        self.write(TASK, contract_text() + '\nRead D-140 and the [focused guide](../guide.md).\n')
        marker = self.root / "executed-validator.txt"
        self.write("tools/task-context.py", "raise RuntimeError('untrusted validator ran')\n")
        self.write("tools/task-schema.py", "from pathlib import Path\nPath('executed-validator.txt').touch()\n")
        result = self.cli("TASK-0020", "--validate-only")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(marker.exists())

    def test_non_repository_fails_without_partial_output(self):
        outside = Path(self.temporary.name) / "not-a-repository"
        outside.mkdir()
        result = self.cli("TASK-0020", repo=outside)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(result.stdout, "")


if __name__ == "__main__":
    unittest.main()
