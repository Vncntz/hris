#!/usr/bin/env python3
"""Generate a deterministic, non-authoritative index of frozen planning decisions."""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCES = (
    Path("docs/planning/MASTER_SOFTWARE_PLAN.md"),
    Path("docs/planning/FINAL_PLANNING_STATE.md"),
)
INDEX = ROOT / "docs/planning/INDEX.md"
HEADING = re.compile(r"^ {0,3}(#{1,6})[ \t]+(.+?)\s*$")
DECISION = re.compile(r"^(D-\d{3})[ \t]+[—–-][ \t]+(.+?)\s*$")
FENCE = re.compile(r"^ {0,3}([~\x60]{3,})")
STATUS = re.compile(r"^\s*(?:\*\*)?Status(?:\*\*)?:\s*(.+?)\s*$", re.IGNORECASE)


def markdown_headings(lines: list[str]) -> list[tuple[int, int, str]]:
    """Return line number, heading depth, and text, excluding fenced code."""
    result = []
    fence_char = None
    fence_width = 0
    for number, line in enumerate(lines, 1):
        marker = FENCE.match(line)
        if marker:
            run = marker.group(1)
            if fence_char is None:
                fence_char, fence_width = run[0], len(run)
            elif run[0] == fence_char and len(run) >= fence_width:
                fence_char = None
            continue
        if fence_char is not None:
            continue
        heading = HEADING.match(line)
        if heading:
            result.append((number, len(heading.group(1)), heading.group(2)))
    return result


def entries_for(source: Path) -> list[dict[str, object]]:
    lines = (ROOT / source).read_text(encoding="utf-8").splitlines(keepends=True)
    headings = markdown_headings(lines)
    result = []
    seen = set()
    for position, (start, level, text) in enumerate(headings):
        decision = DECISION.fullmatch(text)
        if decision is None:
            continue
        decision_id, title = decision.groups()
        if decision_id in seen:
            raise ValueError(f"duplicate {decision_id} definition in {source}")
        seen.add(decision_id)
        end = len(lines)
        for next_start, next_level, _ in headings[position + 1 :]:
            if next_level <= level:
                end = next_start - 1
                break
        content = "".join(lines[start - 1 : end])
        status = "UNKNOWN"
        for line in lines[start:end]:
            match = STATUS.match(line)
            if match:
                status = match.group(1).strip()
                break
        result.append(
            {
                "id": decision_id,
                "title": title,
                "status": status,
                "source": source.as_posix(),
                "start": start,
                "end": end,
                "sha256": hashlib.sha256(content.encode("utf-8")).hexdigest(),
            }
        )
    return result


def cell(value: object) -> str:
    return str(value).replace("|", r"\|").replace("\n", " ")


def generate() -> str:
    records = [record for source in SOURCES for record in entries_for(source)]
    master_ids = {record["id"] for record in records if record["source"] == SOURCES[0].as_posix()}
    rows = [
        "# Planning decision index",
        "",
        "GENERATED DERIVATIVE INDEX — NOT AN AUTHORITATIVE PLANNING SOURCE.",
        "",
        "The frozen sources govern. MASTER_SOFTWARE_PLAN.md has lookup precedence when both sources define a Decision ID; both definitions remain listed below. Regenerate with python tools/plan-index.py and verify with python tools/plan-index.py --check.",
        "",
        "| Decision ID | Title | Status | Source file | Start line | End line | SHA256 of range | Preferred |",
        "| --- | --- | --- | --- | ---: | ---: | --- | --- |",
    ]
    for record in sorted(records, key=lambda item: (item["id"], SOURCES.index(Path(item["source"])))):
        preferred = record["source"] == SOURCES[0].as_posix() or record["id"] not in master_ids
        values = (
            record["id"],
            record["title"],
            record["status"],
            record["source"],
            record["start"],
            record["end"],
            record["sha256"],
            "yes" if preferred else "no; also defined in Master Plan",
        )
        rows.append("| " + " | ".join(cell(value) for value in values) + " |")
    return "\n".join(rows) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the checked-in index is missing or stale")
    args = parser.parse_args()
    try:
        expected = generate().encode("utf-8")
    except (OSError, UnicodeError, ValueError) as error:
        print(f"plan-index: {error}", file=sys.stderr)
        return 2
    if args.check:
        if not INDEX.exists() or INDEX.read_bytes() != expected:
            print(f"plan-index: missing or stale {INDEX.relative_to(ROOT)}", file=sys.stderr)
            return 1
        print(f"plan-index: current {INDEX.relative_to(ROOT)}")
        return 0
    with INDEX.open("wb") as output:
        output.write(expected)
    print(f"plan-index: wrote {INDEX.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
