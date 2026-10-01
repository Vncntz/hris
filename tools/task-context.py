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


def contained_file(repo: Path, source: str) -> Path:
    """Require canonical relative spelling and containment after resolving symlinks."""
    if not isinstance(source, str) or not source or "\\" in source or ":" in source:
        raise ValueError("invalid repository-relative path")
    relative = PurePosixPath(source)
    if relative.is_absolute() or any(part in ("", ".", "..") for part in source.split("/")):
        raise ValueError("path traversal or non-canonical path")
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
    headings = re.findall(rf"^ {{0,3}}#{{1,6}}[ \t]+({kind}-[^\s]+)(?:[ \t]+.*)?$", text, re.MULTILINE)
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
    parents = [line.lstrip(" ") for line in task_text.splitlines()
               if re.match(r"^ {0,3}Parent implementation item:", line)]
    parent = re.fullmatch(
        r"Parent implementation item: \[(IMP-[0-9]{3})\]\(([^)\s]+)\)",
        parents[0] if len(parents) == 1 else "",
    )
    if parent is None:
        raise ValueError("missing, duplicate or malformed parent IMP link")
    imp_id, target = parent.groups()
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
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    try:
        packet = build_packet(args.repo, args.task, args.decisions)
        output = json.dumps(packet, ensure_ascii=False, indent=2, sort_keys=True)
    except (OSError, UnicodeError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"task-context: {error}", file=sys.stderr)
        return 1
    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
