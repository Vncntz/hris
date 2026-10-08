"""Synthetic syntax tests for prospective v2 contracts, never integration gates."""

import copy
import importlib.util
import json
from pathlib import Path
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "task-context.py"
SPEC = importlib.util.spec_from_file_location("hris_task_schema", SCRIPT)
CONTEXT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CONTEXT)
METADATA = {
    "schema": 2, "task": "TASK-0020", "imp": "IMP-088",
    "title": "Synthetic context packet", "owner": "CODEX",
    "baseline_main_sha": "0123456789abcdef0123456789abcdef01234567",
    "dependencies": "none",
}
PARENT = "Parent implementation item: [IMP-088](../implementation/tasks/IMP-088.md)"
# Independent Phase-3 specification fixture; do not derive expectations from the parser.
SEMANTIC_SECTIONS = (
    "Objective", "In scope", "Out of scope", "Invariants",
    "Permitted repository scope", "Forbidden repository scope",
    "Observable acceptance criteria", "Required verification",
    "Evidence / handoff requirements",
)


def contract_text(metadata=None):
    metadata = copy.deepcopy(METADATA if metadata is None else metadata)
    return ("# TASK-0020 - Synthetic context packet\n\n```task-schema-v2\n"
            + json.dumps(metadata, indent=2) + "\n```\n\n" + PARENT + "\n\n"
            + "\n\n".join("## " + section + "\n\nSynthetic required content."
                             for section in SEMANTIC_SECTIONS) + "\n")


class TaskSchemaTests(unittest.TestCase):
    def parse(self, text):
        return CONTEXT.task_contract(text, "TASK-0020", "IMP-088")

    def reject(self, text):
        with self.assertRaises(ValueError):
            self.parse(text)

    def test_valid_both_owners_and_line_endings(self):
        for owner in ("CODEX", "ANTIGRAVITY"):
            metadata = {**METADATA, "owner": owner}
            for newline in ("\n", "\r\n"):
                with self.subTest(owner=owner, newline=newline):
                    self.assertEqual(self.parse(contract_text(metadata).replace("\n", newline)), metadata)
        # JSON ordering/formatting and supported identity separators do not change meaning.
        metadata = dict(reversed(list(METADATA.items())))
        text = contract_text(metadata).replace("# TASK-0020 -", "# TASK-0020 —", 1)
        self.assertEqual(self.parse(text), METADATA)

    def test_valid_explicit_task_and_contract_prerequisites(self):
        dependencies = ["TASK-0001", {"contract": "docs/tasks/README.md",
                                    "requirement": "Approved Phase-3 contract"}]
        metadata = {**METADATA, "dependencies": dependencies}
        self.assertEqual(self.parse(contract_text(metadata)), metadata)

    def test_numbering_never_creates_or_orders_dependencies(self):
        for dependencies in ("none", ["TASK-9999", "TASK-0001"]):
            metadata = {**METADATA, "dependencies": dependencies}
            self.assertEqual(self.parse(contract_text(metadata))["dependencies"], dependencies)

    def test_legacy_has_no_new_fields_or_sections_required(self):
        text = "# TASK-0020 - Historical contract\n\n" + PARENT + "\n"
        self.assertIsNone(self.parse(text))
        self.assertIsNone(self.parse(text + '\nLegacy prose mentions schema v2 and {"schema": 2}.\n'))

    def test_every_missing_field_and_unknown_fields_fail(self):
        for field in METADATA:
            with self.subTest(field=field):
                metadata = copy.deepcopy(METADATA)
                metadata.pop(field)
                self.reject(contract_text(metadata))
        self.reject(contract_text({**METADATA, "current_pr_head": METADATA["baseline_main_sha"]}))

    def test_duplicate_json_keys_at_every_object_level_fail(self):
        text = contract_text()
        for field in METADATA:
            duplicate = json.dumps(field) + ": " + json.dumps(METADATA[field])
            with self.subTest(field=field):
                self.reject(text.replace("{", "{" + duplicate + ",", 1))
        metadata = {**METADATA, "dependencies": [{"contract": "docs/tasks/README.md", "requirement": "Approved"}]}
        self.reject(contract_text(metadata).replace('"requirement": "Approved"',
                    '"requirement": "Approved", "requirement": "Conflicting"'))

    def test_malformed_json_and_nonobject_roots_fail(self):
        text = contract_text()
        start = text.index("{", text.index("```task-schema-v2"))
        end = text.index("\n```", start)
        for payload in ("{", "[]", "null", "true", "2", '"text"', '{} {}', '{"schema": 2,}', '{/* comment */}'):
            with self.subTest(payload=payload):
                self.reject(text[:start] + payload + text[end:])

    def test_bad_versions_owners_ids_titles_and_sha_fail(self):
        cases = {
            "schema": [True, "2", 1, 3, 2.0, None],
            "task": ["TASK-20", "TASK-00200", "TASK-0021", "task-0020", None, ["TASK-0020"]],
            "imp": ["IMP-88", "IMP-0088", "IMP-089", "imp-088", None],
            "title": ["", " ", "Different title", " Synthetic context packet", "Synthetic context packet ", "Two\nlines", None],
            "owner": ["HUMAN", "CODEX REVIEW", "codex", "CODEX,ANTIGRAVITY", ["CODEX"], ["CODEX", "ANTIGRAVITY"], None, True],
            "baseline_main_sha": [None, True, "1401899", "f" * 39, "f" * 41, "f" * 64, "A" * 40, "g" * 40, " " + "f" * 40],
        }
        for field, values in cases.items():
            for value in values:
                with self.subTest(field=field, value=value):
                    self.reject(contract_text({**METADATA, field: value}))

    def test_dependency_syntax_fails_closed(self):
        cases = (
            [], None, True, {}, "", "None", "TASK-0001", "TASK-0001,TASK-0002",
            ["none"], ["TASK-1"], ["TASK-00001"], ["IMP-088"], [True], [None],
            ["TASK-0020"], ["TASK-0001", "TASK-0001"], ["TASK-0001", "none"],
            [{"contract": "docs/tasks/README.md"}],
            [{"contract": "docs/tasks/README.md", "requirement": ""}],
            [{"contract": "docs/tasks/README.md", "requirement": " ", "unknown": True}],
            [{"contract": "docs/tasks/README.md", "requirement": "Two\nlines"}],
            [{"contract": "docs/tasks/README.md", "requirement": None}],
            [{"task": "TASK-0001"}],
        )
        for dependencies in cases:
            with self.subTest(dependencies=dependencies):
                self.reject(contract_text({**METADATA, "dependencies": dependencies}))
        for path in (None, [], "", "../outside.md", "/absolute.md", "C:/absolute.md",
                     "docs\\README.md", "./docs/README.md", "docs//README.md",
                     "docs/../README.md", "docs/README.md#anchor", "docs/README.md?query"):
            with self.subTest(path=path):
                self.reject(contract_text({**METADATA, "dependencies": [{"contract": path, "requirement": "Approved"}]}))
        prerequisite = {"contract": "docs/README.md", "requirement": "Approved"}
        self.reject(contract_text({**METADATA, "dependencies": [prerequisite, prerequisite]}))

    def test_malformed_misplaced_duplicate_or_unsupported_blocks_do_not_become_legacy(self):
        text = contract_text()
        cases = (
            text.replace("task-schema-v2", "task-schema-v3"),
            text.replace("task-schema-v2", "TASK-SCHEMA-V2"),
            text.replace("task-schema-v2", "task-schema_v2"),
            text.replace("task-schema-v2", "task-schema"),
            text.replace("task-schema-v2", "task-schema-v2 extra"),
            text.replace("```task-schema-v2", "~~~task-schema-v2"),
            text.replace("```task-schema-v2", "````task-schema-v2"),
            text.replace("```task-schema-v2", "``task-schema-v2"),
            text.replace("```task-schema-v2", "`~`task-schema-v2"),
            text.replace("```task-schema-v2", " ```task-schema-v2"),
            text.replace("```task-schema-v2", "``` task-schema-v2"),
            text.replace("\n\n```task-schema-v2", "\n```task-schema-v2", 1),
            text.replace("\n\n```task-schema-v2", "\n\nProse\n\n```task-schema-v2", 1),
            text.replace("\n```\n", "\n", 1),
            text + '\n```task-schema-v2\n{}\n```\n',
            text + '\n```task-schema-v3\n{}\n```\n',
        )
        for content in cases:
            with self.subTest(content=content[:130]):
                self.reject(content)

    def test_each_required_section_missing_duplicate_wrong_level_or_empty_fails(self):
        text = contract_text()
        for section in SEMANTIC_SECTIONS:
            block = "## " + section + "\n\nSynthetic required content."
            for replacement in ("", "### " + section + "\n\nContent.", "## " + section,
                                "## " + section + "\n\n<!-- Not content -->",
                                "## " + section + "\n\n```text\n```",
                                "## " + section + "\n\n### Empty subsection"):
                with self.subTest(section=section, replacement=replacement):
                    self.reject(text.replace(block, replacement))
            self.reject(text + "\n## " + section + "\n\nDuplicate content.\n")
        self.reject(text + "\n## objective\n\nDuplicate case-insensitive heading.\n")

    def test_container_schema_markers_never_fall_back_to_legacy(self):
        text = contract_text()
        for prefix in ("> ", "- ", "1. ", "+ ", "* ", "12) ", "> - ", "- > ", "  > 1. "):
            for invalid in (text.replace('"schema": 2', '"schema":'),
                            text[:text.index("## Objective")], text):
                with self.subTest(prefix=prefix, invalid=invalid[:100]):
                    self.reject(invalid.replace("```task-schema-v2", prefix + "```task-schema-v2"))

    def test_container_duplicate_reserved_blocks_fail(self):
        for prefix in ("> ", "- ", "1. ", "> - ", "1. > "):
            for marker in ("```task-schema-v2", "~~~task-schema-v3", "``task-schema"):
                with self.subTest(prefix=prefix, marker=marker):
                    self.reject(contract_text() + "\n" + prefix + marker + "\n{}\n```\n")

    def test_container_fenced_examples_cannot_supply_semantic_sections(self):
        text = contract_text()
        start = text.index("## Objective")
        # The reported counterexample: all nine H2 headings exist only in a list fence.
        for opener, indent in (("- ", "  "), ("1. ", "   "), ("> ", "> "),
                               ("> - ", ">   "), ("- - ", "    ")):
            for fence in ("```", "~~~~"):
                example = (opener + fence + "text\n"
                           + "\n".join(indent + line for line in text[start:].splitlines())
                           + "\n" + indent + fence + "\n")
                with self.subTest(opener=opener, fence=fence):
                    self.reject(text[:start] + example)
                    # Closing the container fence must expose subsequent real sections.
                    self.assertEqual(self.parse(text[:start] + example + text[start:]), METADATA)

    def test_fenced_and_commented_headings_cannot_supply_sections(self):
        text = contract_text()
        block = "## Objective\n\nSynthetic required content."
        for fence in ("```", "~~~", "````", "   ~~~~~"):
            self.reject(text.replace(block, fence + "\n" + block + "\n" + fence))
        self.reject(text.replace(block, "<!--\n" + block + "\n-->"))
        self.reject(text.replace(block, "<!--\n" + block))
        # A pseudo closing fence with text must not expose example headings.
        self.reject(text.replace(block, "```text\n```still-code\n" + block + "\n```"))
        self.assertEqual(self.parse(text + "\n```text\n" + block + "\n```\n"), METADATA)


if __name__ == "__main__":
    unittest.main()
