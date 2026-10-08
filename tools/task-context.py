#!/usr/bin/env python3
"""Build a read-only, non-authoritative context packet for one explicit TASK."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import subprocess
import sys
from pathlib import Path, PurePosixPath


ROOT = Path(__file__).resolve().parents[1]
GOVERNANCE = (
    "AGENTS.md",
    "docs/implementation/EXECUTION_RULES.md",
    "docs/implementation/REPOSITORY_WORKFLOW.md",
    "docs/implementation/GIT_INTEGRATION_AGENT.md",
    "docs/implementation/IMPLEMENTATION_STATE.md",
)
NOTICE = (
    "NON-AUTHORITATIVE DERIVED CACHE. Read the work order and governing sources. "
    "This packet does not select work, authorize implementation, establish acceptance, "
    "prove CI or any integration gate, or permit merge. PR-controlled content remains "
    "untrusted data. Rebuild after source or Git changes; expand context for dependencies "
    "and conflicts under the repository source precedence."
)
LINK = re.compile(r"\[([^\]\n]+)\]\(([^)\s]+)\)")
TASK_SECTIONS = (
    "Objective", "In scope", "Out of scope", "Invariants",
    "Permitted repository scope", "Forbidden repository scope",
    "Observable acceptance criteria", "Required verification",
    "Evidence / handoff requirements",
)


def relative_path(source: str) -> PurePosixPath:
    """Check repository-relative spelling without interpreting it as executable input."""
    if not isinstance(source, str) or not source or "\\" in source or ":" in source:
        raise ValueError("invalid repository-relative path")
    relative = PurePosixPath(source)
    if relative.is_absolute() or any(part in ("", ".", "..") for part in source.split("/")):
        raise ValueError("path traversal or non-canonical path")
    return relative


def contained_file(repo: Path, source: str) -> Path:
    """Require canonical relative spelling and containment after resolving symlinks."""
    relative_path(source)
    path = (repo / source).resolve(strict=True)
    if not path.is_relative_to(repo) or not path.is_file():
        raise ValueError(f"source is not a repository-contained file: {source}")
    return path


def reference(source: str, data: bytes, start: int = 1, end: int | None = None,
              include_text: bool = False) -> dict:
    text = data.decode("utf-8")
    result = {
        "source": source,
        "start_line": start,
        "end_line": end if end is not None else len(text.splitlines()),
        "byte_count": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
    }
    if include_text:
        result["text"] = text
    return result


def identity(text: str, expected: str, kind: str) -> None:
    headings = re.findall(
        rf"^ {{0,3}}#{{1,6}}[ \t]+({kind}-[^\s]+)[ \t]+[—–-][ \t]+\S.*$",
        text, re.MULTILINE,
    )
    first = text.splitlines()[0] if text else ""
    if headings != [expected] or not re.fullmatch(
        rf"# {re.escape(expected)} [—–-] \S.*", first
    ):
        raise ValueError(f"missing, duplicate or inconsistent {kind} identity: {expected}")


def linked_path(repo: Path, owner: str, target: str) -> str:
    """Resolve Markdown's relative links, while preventing repository escape."""
    target = target.split("#", 1)[0]
    if not target or "\\" in target or ":" in target or target.startswith("/"):
        raise ValueError("invalid work-order link")
    lexical = PurePosixPath(owner).parent / target
    parts: list[str] = []
    for part in lexical.parts:
        if part == "..":
            if not parts:
                raise ValueError("work-order link escapes repository")
            parts.pop()
        elif part != ".":
            parts.append(part)
    source = "/".join(parts)
    contained_file(repo, source)
    return source


def unique_object(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def fence_content(line: str, *, bounded: bool = False,
                  continuation: int = 0) -> tuple[str, tuple[str, ...], int]:
    """Expose fences behind whitespace and nested Markdown quote/list prefixes.

    Reserved-marker/ambiguous-parent discovery deliberately exposes all indentation.
    Fence opening uses bounded indentation: at most three columns before each
    container or fence, and one to four columns after a list marker. It does not
    make a container's schema block or headings canonical TASK declarations.
    """
    line = line.expandtabs(4)
    indentation = len(line) - len(line.lstrip(" "))
    if bounded and indentation > continuation + 3:
        return line, (), indentation
    continuation = max(0, continuation - indentation)
    line = line.lstrip(" ")
    containers = []
    # Five or more spaces after a list marker leave indented code after its
    # one-column padding, rather than becoming unlimited fence indentation.
    spaces = r"(?: {1,4}(?! )| (?= {4}))" if bounded else r" +"
    while prefix := re.match(rf"(?:> ?|[-+*]{spaces}|[0-9]{{1,9}}[.)]{spaces})", line):
        kind = "quote" if line.startswith(">") else "list"
        containers.append(kind)
        if kind == "list":
            indentation += prefix.end()
            continuation = 0
        line = line[prefix.end():]
        extra = len(line) - len(line.lstrip(" "))
        indentation += extra
        if bounded and extra > continuation + 3:
            break
        continuation = max(0, continuation - extra)
        line = line.lstrip(" ")
    return line, tuple(containers), indentation


def schema_markers(text: str) -> list[int]:
    return [number for number, line in enumerate(text.splitlines())
            if re.match(r"^[`~]+[ \t]*task-schema", fence_content(line)[0], re.IGNORECASE)]


def parent_link(text: str, *, strict: bool = False) -> tuple[str, str]:
    """Use active declarations for v2, preserving raw-line legacy extraction."""
    if strict:
        parents = [line for _, line in active_markdown_lines(text.splitlines())
                   if fence_content(line)[0].startswith("Parent implementation item:")]
    else:
        parents = [line for line in text.splitlines()
                   if re.match(r"^ {0,3}Parent implementation item:", line)]
    parent = re.fullmatch(
        r" {0,3}Parent implementation item: \[(IMP-[0-9]{3})\]\(([^)\s]+)\)",
        parents[0] if len(parents) == 1 else "",
    )
    if parent is None:
        raise ValueError("missing, duplicate or malformed active parent IMP link" if strict
                         else "missing, duplicate or malformed parent IMP link")
    imp_id, target = parent.groups()
    if strict and target != f"../implementation/tasks/{imp_id}.md":
        raise ValueError("v2 parent IMP link must use the canonical matching path")
    return imp_id, target


def task_contract(text: str, task_id: str, imp_id: str) -> dict | None:
    """Strict opt-in v2 syntax validation; None preserves the legacy contract.

    This checks declarations, not authorization, dependency eligibility, section
    meaning, scope, GitHub evidence or independent acceptance. No TASK order is used.
    """
    lines = text.splitlines()
    markers = schema_markers(text)
    if not markers:
        return None
    if markers != [2] or lines[1] != "" or lines[2] != "```task-schema-v2":
        raise ValueError("expected one task-schema-v2 block immediately after TASK heading")
    try:
        end = lines.index("```", 3)
    except ValueError:
        raise ValueError("unclosed task-schema-v2 metadata block") from None
    metadata = json.loads("\n".join(lines[3:end]), object_pairs_hook=unique_object)
    fields = {"schema", "task", "imp", "title", "owner", "baseline_main_sha", "dependencies"}
    if not isinstance(metadata, dict) or set(metadata) != fields:
        raise ValueError("v2 metadata must have exactly schema/task/imp/title/owner/baseline_main_sha/dependencies")
    if type(metadata["schema"]) is not int or metadata["schema"] != 2:
        raise ValueError("unsupported TASK schema; expected integer 2")
    if parent_link(text, strict=True)[0] != imp_id:
        raise ValueError("v2 parent IMP identity mismatch")
    for field, expected, pattern in (("task", task_id, r"TASK-[0-9]{4}"),
                                     ("imp", imp_id, r"IMP-[0-9]{3}")):
        value = metadata[field]
        if not isinstance(value, str) or not re.fullmatch(pattern, value) or value != expected:
            raise ValueError(f"v2 {field} identity mismatch or invalid ID")
    title = metadata["title"]
    if not isinstance(title, str) or not title.strip() or title != title.strip() or "\n" in title or "\r" in title:
        raise ValueError("v2 title must be a nonempty single-line string without surrounding whitespace")
    if not re.fullmatch(rf"# {re.escape(task_id)} [—–-] {re.escape(title)}", lines[0]):
        raise ValueError("v2 title must match the TASK identity heading")
    if metadata["owner"] not in ("CODEX", "ANTIGRAVITY"):
        raise ValueError("v2 primary owner must be exactly CODEX or ANTIGRAVITY")
    baseline = metadata["baseline_main_sha"]
    if not isinstance(baseline, str) or not re.fullmatch(r"[0-9a-f]{40}", baseline):
        raise ValueError("v2 baseline_main_sha must be an exact 40-character lowercase hex SHA")
    dependencies = metadata["dependencies"]
    if dependencies != "none":
        if not isinstance(dependencies, list) or not dependencies:
            raise ValueError("v2 dependencies must be explicit 'none' or a nonempty array")
        seen = set()
        for dependency in dependencies:
            if isinstance(dependency, str) and re.fullmatch(r"TASK-[0-9]{4}", dependency):
                key = ("task", dependency)
                if dependency == task_id:
                    raise ValueError("v2 TASK cannot depend on itself")
            elif isinstance(dependency, dict) and set(dependency) == {"contract", "requirement"}:
                relative_path(dependency["contract"])
                if "#" in dependency["contract"] or "?" in dependency["contract"]:
                    raise ValueError("v2 contract path must not have a query or fragment")
                requirement = dependency["requirement"]
                if (not isinstance(requirement, str) or not requirement.strip()
                        or requirement != requirement.strip() or "\n" in requirement or "\r" in requirement):
                    raise ValueError("v2 contract prerequisite requires a nonempty single-line requirement")
                key = ("contract", dependency["contract"])
            else:
                raise ValueError("v2 dependency must be TASK-#### or a contract/requirement object")
            if key in seen:
                raise ValueError("duplicate v2 dependency declaration")
            seen.add(key)
    required_sections(text)
    return metadata


def markdown_lines(lines: list[str]):
    """Scan comments, code spans and fences together, preserving source line numbers.

    Yield comment-free content and a declaration view (None inside fences). Code
    spans remain content but are masked in the declaration view. Literal comment
    markers in either kind of code never affect comment or fence state.
    """
    fence = None
    comment = False
    span_end = None
    list_context = None

    def fence_marker(line: str) -> bool:
        nonlocal fence
        content, containers, indentation = fence_content(
            line, bounded=fence is None,
            continuation=list_context[1] if list_context is not None else 0,
        )
        marker = re.match(r"^(`{3,}|~{3,})(.*)$", content)
        if marker is None:
            return False
        run, suffix = marker.groups()
        if fence is None:
            # List items continue by indentation; quote markers continue explicitly.
            fence = (run, tuple(kind for kind in containers if kind == "quote"),
                     list_context[1] + 3 if list_context is not None else 3,
                     list_context[1] if list_context is not None else 0)
        elif (containers == fence[1] and run[0] == fence[0][0]
              and indentation <= fence[2] and len(run) >= len(fence[0])
              and not suffix.strip()):
            fence = None
        return True

    def fence_continues(line: str) -> bool:
        # A container fence ends with its container, even without a closing run.
        # Only quote prefixes/indentation are structural here; list-looking code
        # inside an existing fence remains literal.
        remaining = line.expandtabs(4)
        indentation = 0
        for _ in fence[1]:
            indentation += len(remaining) - len(remaining.lstrip(" "))
            remaining = remaining.lstrip(" ")
            if not remaining.startswith(">"):
                return False
            remaining = remaining[1:]
            if remaining.startswith(" "):
                remaining = remaining[1:]
        indentation += len(remaining) - len(remaining.lstrip(" "))
        return not remaining.strip() or indentation >= fence[3]

    def closing_span(number: int, column: int, length: int):
        # Match an exact backtick run, including multiline paragraph spans. Block
        # boundaries end inline parsing; an unmatched run is ordinary text.
        quotes = tuple(kind for kind in fence_content(lines[number])[1] if kind == "quote")
        for end_number in range(number, len(lines)):
            candidate = lines[end_number]
            if end_number > number:
                content, containers, _ = fence_content(candidate)
                if (not candidate.strip() or containers != quotes
                        or re.match(r"^(?:`{3,}|~{3,}|#{1,6}[ \t]|<!--)", content)
                        or re.fullmatch(r"(?:[-*_][ \t]*){3,}|=+[ \t]*", content)):
                    break
            for run in re.finditer(r"`+", candidate):
                if end_number == number and run.start() < column:
                    continue
                if len(run[0]) == length:
                    return end_number, run.end()
        return None

    for number, line in enumerate(lines):
        if fence is not None and not fence_continues(line):
            fence = None
        if fence is None and not comment and span_end is None:
            # Remember list content indentation across blank/continuation lines.
            # Dedented prose or a different quote container ends that allowance.
            _, containers, indentation = fence_content(line)
            quotes = tuple(kind for kind in containers if kind == "quote")
            if list_context is not None and line.strip() and (
                    quotes != list_context[0] or indentation < list_context[1]):
                list_context = None
            content, containers, indentation = fence_content(
                line, bounded=True,
                continuation=list_context[1] if list_context is not None else 0,
            )
            if "list" in containers and not content.startswith("    "):
                list_context = (quotes, indentation)
        # Block fences take precedence over inline syntax, including their info
        # strings. A fence-looking line inside a real comment is still comment.
        if not comment and span_end is None and fence_marker(line):
            yield number, line, None
            continue
        if fence is not None:
            yield number, line, None
            continue
        # Indented code is literal; its backticks/comments must not change later
        # prose state. Keep the declaration view for strict ambiguous-parent checks.
        if not comment and span_end is None and fence_content(
                line, bounded=True,
                continuation=list_context[1] if list_context is not None else 0,
        )[0].startswith("    "):
            yield number, line, line
            continue
        visible = []
        active = []
        column = 0
        # Fence contents bypass inline/comment processing entirely. Outside a
        # fence, process comments and spans in source order before finding fences.
        while column < len(line):
            if comment:
                end = line.find("-->", column)
                stop = len(line) if end < 0 else end + 3
                visible.append(" " * (stop - column))
                active.append(" " * (stop - column))
                column = stop
                comment = end < 0
            elif span_end is not None:
                stop = span_end[1] if number == span_end[0] else len(line)
                visible.append(line[column:stop])
                active.append("x" * (stop - column))
                column = stop
                if number == span_end[0]:
                    span_end = None
            elif line.startswith("<!--", column):
                comment = True
            elif line[column] == "\\" and column + 1 < len(line):
                visible.append(line[column:column + 2])
                active.append(line[column:column + 2])
                column += 2
            elif line[column] == "`":
                run = re.match(r"`+", line[column:])[0]
                span_end = closing_span(number, column + len(run), len(run))
                visible.append(run)
                active.append("x" * len(run) if span_end is not None else run)
                column += len(run)
            else:
                visible.append(line[column])
                active.append(line[column])
                column += 1
        cleaned = "".join(visible)
        declaration = "".join(active)
        if fence_marker(declaration):
            yield number, cleaned, None
            continue
        yield number, cleaned, declaration


def active_markdown_lines(lines: list[str]):
    """Use the shared scanner for v2 declarations and headings."""
    for number, _, declaration in markdown_lines(lines):
        if declaration is not None:
            yield number, declaration


def required_sections(text: str) -> None:
    """Require unique nonempty H2 sections; fenced/commented headings are examples."""
    scanned = list(markdown_lines(text.splitlines()))
    lines = [line for _, line, _ in scanned]
    headings = []
    for number, _, line in scanned:
        if line is None:
            continue
        heading = re.match(r"^ {0,3}(#{1,6})[ \t]+(.+?)[ \t]*$", line)
        if heading:
            headings.append((number, len(heading[1]), heading[2].casefold()))
    for section in TASK_SECTIONS:
        matches = [(number, level) for number, level, title in headings if title == section.casefold()]
        if len(matches) != 1 or matches[0][1] != 2:
            raise ValueError(f"v2 requires exactly one level-2 section: {section}")
        start = matches[0][0]
        end = next((number for number, level, _ in headings if number > start and level <= 2), len(lines))
        body = "\n".join(lines[start + 1:end])
        # Subheadings alone are not semantic content.
        body = re.sub(r"^ {0,3}#{1,6}[ \t]+.*$", "", body, flags=re.MULTILINE)
        body = re.sub(r"^ {0,3}(?:`{3,}|~{3,}).*$", "", body, flags=re.MULTILINE)
        if not body.strip():
            raise ValueError(f"empty required v2 section: {section}")


def routing_manifest(data: bytes, task_id: str, imp_id: str) -> dict:
    manifest = json.loads(data.decode("utf-8"), object_pairs_hook=unique_object)
    keys = {"version", "task", "imp", "decisions", "references"}
    if not isinstance(manifest, dict) or set(manifest) != keys:
        raise ValueError("routing manifest must have exactly version/task/imp/decisions/references")
    if type(manifest["version"]) is not int or manifest["version"] != 1:
        raise ValueError("unsupported routing manifest version")
    if manifest["task"] != task_id or manifest["imp"] != imp_id:
        raise ValueError("routing manifest TASK/IMP identity mismatch")
    for key in ("decisions", "references"):
        if not isinstance(manifest[key], list) or any(not isinstance(v, str) for v in manifest[key]):
            raise ValueError(f"routing {key} must be a list of strings")
    return manifest


def git_state(repo: Path) -> dict:
    options = ["git", "--no-optional-locks", "-c", "core.fsmonitor=false", "-c",
               "core.untrackedCache=false", "-C", str(repo)]

    def git(*args: str, no_matches: bool = False) -> str:
        process = subprocess.run(
            [*options, *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        )
        if process.returncode != 0 and not (no_matches and process.returncode == 1):
            process.check_returncode()
        return process.stdout.decode("utf-8").strip()

    # Even status can invoke clean/process filters on modified tracked files.
    # Enumerate names as data, then disable every driver without changing config.
    keys = git("config", "--includes", "--null", "--name-only", "--get-regexp",
               r"^filter\.", no_matches=True)
    drivers = set()
    for key in filter(None, keys.split("\0")):
        if not re.fullmatch(r"filter\.[A-Za-z0-9._/-]+\.[A-Za-z][A-Za-z0-9-]*", key):
            raise ValueError("unsupported Git filter configuration key")
        drivers.add(key.rsplit(".", 1)[0])
    for driver in sorted(drivers):
        for setting in ("clean=", "process=", "required=false"):
            options.extend(["-c", f"{driver}.{setting}"])
    if Path(git("rev-parse", "--show-toplevel")).resolve() != repo:
        raise ValueError("--repo must be a Git worktree root")
    head = git("rev-parse", "--verify", "HEAD")
    if not re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", head):
        raise ValueError("invalid Git HEAD")
    return {
        "head": head,
        "branch": git("rev-parse", "--abbrev-ref", "HEAD"),
        "dirty": bool(git("status", "--porcelain=v1", "-z", "--untracked-files=all",
                          "--ignore-submodules=all")),
    }


def planning_reader(repo: Path):
    # Only import the helper beside THIS trusted script, never --repo/tools code.
    helper = contained_file(ROOT, "tools/plan-get.py")
    spec = importlib.util.spec_from_file_location("task_context_plan_get", helper)
    module = importlib.util.module_from_spec(spec)
    bytecode_setting = sys.dont_write_bytecode
    try:
        sys.dont_write_bytecode = True
        spec.loader.exec_module(module)
    finally:
        sys.dont_write_bytecode = bytecode_setting
    module.ROOT = repo
    module.INDEX = contained_file(repo, "docs/planning/INDEX.md")
    return module


def build_packet(repo: Path, task_id: str, decisions: list[str]) -> dict:
    repo = repo.resolve(strict=True)
    if not re.fullmatch(r"TASK-[0-9]{4}", task_id):
        raise ValueError("expected one exact TASK-#### ID")
    before = git_state(repo)
    snapshots: dict[str, bytes] = {}

    def read(source: str) -> bytes:
        data = contained_file(repo, source).read_bytes()
        if source in snapshots and snapshots[source] != data:
            raise ValueError(f"source changed during packet construction: {source}")
        snapshots[source] = data
        return data

    task_source = f"docs/tasks/{task_id}.md"
    task_data = read(task_source)
    task_text = task_data.decode("utf-8")
    identity(task_text, task_id, "TASK")
    imp_id, target = parent_link(task_text, strict=bool(schema_markers(task_text)))
    task_contract(task_text, task_id, imp_id)
    imp_source = f"docs/implementation/tasks/{imp_id}.md"
    if linked_path(repo, task_source, target) != imp_source:
        raise ValueError("parent IMP link identity mismatch")
    imp_data = read(imp_source)
    imp_text = imp_data.decode("utf-8")
    identity(imp_text, imp_id, "IMP")
    backlinks = [target for label, target in LINK.findall(imp_text)
                 if re.match(rf"{re.escape(task_id)}(?:\b)", label)]
    if not backlinks or any(linked_path(repo, imp_source, link) != task_source for link in backlinks):
        raise ValueError("missing or inconsistent IMP backlink to TASK")

    manifest_source = f"docs/tasks/{task_id}.context.json"
    manifest_path = repo / manifest_source
    has_manifest = manifest_path.exists() or manifest_path.is_symlink()
    manifest_data = read(manifest_source) if has_manifest else None
    manifest = routing_manifest(manifest_data, task_id, imp_id) if has_manifest else {
        "decisions": [], "references": [],
    }
    requested = sorted(set([*manifest["decisions"], *decisions]))
    if any(not re.fullmatch(r"D-[0-9]{3}", decision) for decision in requested):
        raise ValueError("expected exact D-### IDs")
    mentioned = set(re.findall(r"\bD-[0-9]{3}\b", task_text + "\n" + imp_text))
    if not set(manifest["decisions"]).issubset(mentioned):
        raise ValueError("routing decisions must be explicit in the TASK or IMP; use CLI for additions")

    routing_references = []
    for source in sorted(set(manifest["references"])):
        data = read(source)
        linked = source in GOVERNANCE
        for owner, text in ((task_source, task_text), (imp_source, imp_text)):
            for _, link in LINK.findall(text):
                if ":" in link or link.startswith("#"):
                    continue
                # Only validate candidate links used by the routing manifest.
                try:
                    linked = linked or linked_path(repo, owner, link) == source
                except (OSError, ValueError):
                    continue
        if not linked:
            raise ValueError(f"routing reference is not linked by the TASK or IMP: {source}")
        if source not in GOVERNANCE and source not in (task_source, imp_source):
            routing_references.append(reference(source, data))

    packet = {
        "version": 1,
        "notice": NOTICE,
        "git": before,
        "task": reference(task_source, task_data, include_text=True),
        "imp": reference(imp_source, imp_data, include_text=True),
        "governance": [reference(source, read(source)) for source in GOVERNANCE],
        "routing": reference(manifest_source, manifest_data) if has_manifest else None,
        "references": routing_references,
        "planning_index": None,
        "decisions": [],
    }
    if requested:
        packet["planning_index"] = reference("docs/planning/INDEX.md", read("docs/planning/INDEX.md"))
        reader = planning_reader(repo)
        records = reader.read_index()
        for decision in requested:
            record = reader.select(records, decision)
            # Validate containment before the existing reader opens a frozen source.
            path = contained_file(repo, record["source"])
            excerpt = reader.source_range(record)
            # Hash original source bytes; plan-get separately checks its LF-normalized index hash.
            raw_lines = []
            with path.open("rb") as source:
                for number, line in enumerate(source, 1):
                    if number > record["end"]:
                        break
                    if number >= record["start"]:
                        raw_lines.append(line)
            raw = b"".join(raw_lines)
            if raw.decode("utf-8").replace("\r\n", "\n").replace("\r", "\n") != excerpt:
                raise ValueError("decision excerpt changed or has mismatched source identity")
            if record["id"] != decision:
                raise ValueError("decision excerpt identity mismatch")
            item = reference(record["source"], raw, record["start"], record["end"], True)
            item.update(id=decision, title=record["title"], indexed_sha256=record["sha256"])
            packet["decisions"].append(item)
        # Recheck the indexed excerpts so a mid-read change cannot silently mix sources.
        for item in packet["decisions"]:
            record = reader.select(records, item["id"])
            path = contained_file(repo, record["source"])
            with path.open("rb") as source:
                raw = b"".join(line for number, line in enumerate(source, 1)
                               if record["start"] <= number <= record["end"])
            if (reader.source_range(record) != item["text"].replace("\r\n", "\n").replace("\r", "\n")
                    or hashlib.sha256(raw).hexdigest() != item["sha256"] or len(raw) != item["byte_count"]):
                raise ValueError("decision source changed during packet construction")
    for source, data in snapshots.items():
        if contained_file(repo, source).read_bytes() != data:
            raise ValueError(f"source changed during packet construction: {source}")
    if has_manifest != (manifest_path.exists() or manifest_path.is_symlink()) or git_state(repo) != before:
        raise ValueError("Git or routing state changed during packet construction; rebuild")
    return packet


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("task", help="one explicit TASK-#### ID")
    parser.add_argument("decisions", nargs="*", help="additional explicit D-### IDs")
    parser.add_argument("--repo", type=Path, default=ROOT, help="data worktree root; tooling stays trusted")
    parser.add_argument("--validate-only", action="store_true",
                        help="validate the work order/context sources and emit a compact syntax result")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    try:
        packet = build_packet(args.repo, args.task, args.decisions)
        if args.validate_only:
            # Reuse the packet's validated, snapshotted sources and legacy link checks.
            imp_id = Path(packet["imp"]["source"]).stem
            metadata = task_contract(packet["task"]["text"], args.task, imp_id)
            packet = {"task": args.task, "imp": imp_id,
                      "schema": 2 if metadata is not None else "legacy", "metadata": metadata}
        output = json.dumps(packet, ensure_ascii=False, indent=2, sort_keys=True)
    except (OSError, UnicodeError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"task-context: {error}", file=sys.stderr)
        return 1
    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
