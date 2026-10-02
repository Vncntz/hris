#!/usr/bin/env python3
"""Validate deterministic repository documentation/work-order policy.

This is intentionally mechanical. It validates local Markdown links and anchors,
TASK identity/parent/backlinks, and task-index registration. It does not select
work, interpret scope, or replace semantic review and integration gates.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
LINK = re.compile(r"(?<!!)\[([^\]\n]+)\]\(([^)\s]+)\)")
HEADING = re.compile(r"^ {0,3}(#{1,6})[ \t]+(.+?)[ \t]*#*[ \t]*$", re.MULTILINE)
TASK_FILE = re.compile(r"TASK-[0-9]{4}\.md")
TASK_ID = re.compile(r"TASK-[0-9]{4}")
IMP_ID = re.compile(r"IMP-[0-9]{3}")
TASK_IDENTITY = re.compile(r"^# (TASK-[0-9]{4}) [—–-] \S.*$")
IMP_IDENTITY = re.compile(r"^# (IMP-[0-9]{3}) [—–-] \S.*$")
PARENT = re.compile(
    r"^ {0,3}Parent implementation item: \[(IMP-[0-9]{3})\]\(([^)\s]+)\)[ \t]*$",
    re.MULTILINE,
)


def markdown_files(root: Path) -> list[Path]:
    return sorted(
        path for path in root.rglob("*.md")
        if path.is_file() and ".git" not in path.parts
    )


def relative(root: Path, path: Path) -> str:
    return path.relative_to(root).as_posix()


def heading_text(value: str) -> str:
    value = re.sub(r"<[^>]+>", "", value)
    value = re.sub(r"!\[([^\]]*)\]\([^)]*\)", r"\1", value)
    value = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", value)
    value = value.replace("`", "")
    return value


def github_slug(value: str) -> str:
    value = unquote(heading_text(value)).strip().lower()
    value = re.sub(r"[^\w\- ]", "", value, flags=re.UNICODE)
    value = re.sub(r"[ \t]+", "-", value)
    return value


def anchors(text: str) -> set[str]:
    counts: dict[str, int] = defaultdict(int)
    result: set[str] = set()
    for _, title in HEADING.findall(text):
        base = github_slug(title)
        if not base:
            continue
        count = counts[base]
        counts[base] += 1
        result.add(base if count == 0 else f"{base}-{count}")
    return result


def local_target(root: Path, owner: Path, target: str) -> tuple[Path | None, str | None]:
    parsed = urlsplit(target)
    if parsed.scheme or parsed.netloc:
        return None, None
    path_part = unquote(parsed.path)
    anchor = unquote(parsed.fragment) if parsed.fragment else None
    if not path_part:
        return owner, anchor
    if "\\" in path_part or path_part.startswith("/"):
        raise ValueError("non-canonical local path")
    lexical = PurePosixPath(relative(root, owner)).parent / PurePosixPath(path_part)
    parts: list[str] = []
    for part in lexical.parts:
        if part == "..":
            if not parts:
                raise ValueError("local link escapes repository")
            parts.pop()
        elif part not in ("", "."):
            parts.append(part)
    resolved = root.joinpath(*parts)
    try:
        resolved.resolve(strict=False).relative_to(root.resolve())
    except ValueError as error:
        raise ValueError("local link escapes repository") from error
    return resolved, anchor


def check_links(root: Path, errors: list[str]) -> int:
    count = 0
    anchor_cache: dict[Path, set[str]] = {}
    for source in markdown_files(root):
        text = source.read_text(encoding="utf-8")
        for label, target in LINK.findall(text):
            if target.startswith("<") and target.endswith(">"):
                target = target[1:-1]
            try:
                destination, anchor = local_target(root, source, target)
            except ValueError as error:
                errors.append(f"{relative(root, source)}: {label!r}: {error}")
                continue
            if destination is None:
                continue
            count += 1
            if not destination.exists():
                errors.append(
                    f"{relative(root, source)}: broken local link {target!r}"
                )
                continue
            if anchor and destination.suffix.lower() == ".md":
                available = anchor_cache.get(destination)
                if available is None:
                    available = anchors(destination.read_text(encoding="utf-8"))
                    anchor_cache[destination] = available
                if anchor.lower() not in available:
                    errors.append(
                        f"{relative(root, source)}: missing anchor #{anchor} in "
                        f"{relative(root, destination)}"
                    )
    return count


def resolve_required_link(root: Path, owner: Path, target: str) -> Path:
    destination, _ = local_target(root, owner, target)
    if destination is None:
        raise ValueError("expected repository-local work-order link")
    return destination


def check_tasks(root: Path, errors: list[str]) -> int:
    task_dir = root / "docs" / "tasks"
    imp_dir = root / "docs" / "implementation" / "tasks"
    index = task_dir / "README.md"
    if not task_dir.is_dir() or not imp_dir.is_dir() or not index.is_file():
        errors.append("required task/implementation directories or task index are missing")
        return 0

    index_text = index.read_text(encoding="utf-8")
    indexed: set[str] = set()
    for label, target in LINK.findall(index_text):
        match = TASK_ID.fullmatch(label.strip())
        if not match:
            continue
        try:
            destination = resolve_required_link(root, index, target)
        except ValueError:
            continue
        if destination == task_dir / f"{match.group(0)}.md":
            indexed.add(match.group(0))

    tasks = sorted(path for path in task_dir.iterdir() if path.is_file() and TASK_FILE.fullmatch(path.name))
    for task in tasks:
        expected = task.stem
        text = task.read_text(encoding="utf-8")
        first = text.splitlines()[0] if text else ""
        identity = TASK_IDENTITY.fullmatch(first)
        if identity is None or identity.group(1) != expected:
            errors.append(f"{relative(root, task)}: first heading must identify {expected}")

        parents = PARENT.findall(text)
        if len(parents) != 1:
            errors.append(f"{relative(root, task)}: expected exactly one canonical parent IMP link")
            continue
        imp_id, target = parents[0]
        expected_imp = imp_dir / f"{imp_id}.md"
        try:
            actual_imp = resolve_required_link(root, task, target)
        except ValueError as error:
            errors.append(f"{relative(root, task)}: parent IMP link: {error}")
            continue
        if actual_imp != expected_imp or not actual_imp.is_file():
            errors.append(f"{relative(root, task)}: parent IMP link does not resolve to {imp_id}.md")
            continue

        imp_text = actual_imp.read_text(encoding="utf-8")
        imp_first = imp_text.splitlines()[0] if imp_text else ""
        imp_identity = IMP_IDENTITY.fullmatch(imp_first)
        if imp_identity is None or imp_identity.group(1) != imp_id:
            errors.append(f"{relative(root, actual_imp)}: first heading must identify {imp_id}")

        backlinks = []
        for label, backlink in LINK.findall(imp_text):
            if not re.match(rf"^{re.escape(expected)}\b", label.strip()):
                continue
            try:
                backlinks.append(resolve_required_link(root, actual_imp, backlink))
            except ValueError:
                backlinks.append(Path("<invalid>"))
        if task not in backlinks:
            errors.append(
                f"{relative(root, actual_imp)}: missing backlink to {relative(root, task)}"
            )

        if expected not in indexed:
            errors.append(f"docs/tasks/README.md: missing canonical index entry for {expected}")
    return len(tasks)


def validate(root: Path) -> tuple[list[str], dict[str, int]]:
    root = root.resolve()
    errors: list[str] = []
    local_links = check_links(root, errors)
    tasks = check_tasks(root, errors)
    return sorted(set(errors)), {
        "markdown_files": len(markdown_files(root)),
        "local_links": local_links,
        "tasks": tasks,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=ROOT, help="repository root to validate")
    args = parser.parse_args()
    errors, counts = validate(args.repo)
    if errors:
        for error in errors:
            print(f"repo-policy: {error}", file=sys.stderr)
        print(f"repo-policy: FAIL ({len(errors)} error(s))", file=sys.stderr)
        return 1
    print(
        "repo-policy: PASS "
        f"({counts['markdown_files']} markdown files, "
        f"{counts['local_links']} local links, {counts['tasks']} task work orders)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
