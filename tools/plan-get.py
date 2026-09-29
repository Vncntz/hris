#!/usr/bin/env python3
"""Retrieve one indexed frozen-planning decision without loading a whole source."""

from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "docs/planning/INDEX.md"
MASTER = "docs/planning/MASTER_SOFTWARE_PLAN.md"
FINAL = "docs/planning/FINAL_PLANNING_STATE.md"
SOURCES = (MASTER, FINAL)
HEADER = "| Decision ID | Title | Status | Source file | Start line | End line | SHA256 of range | Preferred |"
SEPARATOR = "| --- | --- | --- | --- | ---: | ---: | --- | --- |"
DECISION_HEADING = re.compile(r"^ {0,3}#{1,6}[ \t]+(D-\d{3})[ \t]+[—–-][ \t]+(.+?)\s*$")


def split_row(line: str) -> list[str]:
    if not line.startswith("|") or not line.endswith("|"):
        raise ValueError("malformed index row")
    cells = []
    current = []
    body = line[1:-1]
    position = 0
    while position < len(body):
        char = body[position]
        if char == "\\" and position + 1 < len(body) and body[position + 1] == "|":
            current.append("|")
            position += 2
        elif char == "|":
            cells.append("".join(current).strip())
            current = []
            position += 1
        else:
            current.append(char)
            position += 1
    cells.append("".join(current).strip())
    if len(cells) != 8:
        raise ValueError("index row does not have eight fields")
    return cells


def read_index() -> list[dict[str, object]]:
    try:
        lines = INDEX.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        raise ValueError(f"cannot read {INDEX.relative_to(ROOT)}: {error}") from error
    if (
        len(lines) < 9
        or "GENERATED DERIVATIVE INDEX — NOT AN AUTHORITATIVE PLANNING SOURCE." not in lines
        or lines[6] != HEADER
        or lines[7] != SEPARATOR
    ):
        raise ValueError("malformed planning index header")
    records = []
    seen = set()
    for number, line in enumerate(lines[8:], 9):
        decision_id, title, status, source, start, end, digest, preferred = split_row(line)
        if (
            not re.fullmatch(r"D-\d{3}", decision_id)
            or not title
            or not status
            or source not in SOURCES
            or not start.isdigit()
            or not end.isdigit()
            or int(start) < 1
            or int(end) < int(start)
            or not re.fullmatch(r"[0-9a-f]{64}", digest)
            or preferred not in ("yes", "no; also defined in Master Plan")
        ):
            raise ValueError(f"malformed planning index row {number}")
        key = (decision_id, source)
        if key in seen:
            raise ValueError(f"duplicate indexed definition of {decision_id} in {source}")
        seen.add(key)
        records.append(
            {
                "id": decision_id,
                "title": title,
                "source": source,
                "start": int(start),
                "end": int(end),
                "sha256": digest,
            }
        )
    if not records:
        raise ValueError("planning index has no decisions")
    return records


def select(records: list[dict[str, object]], query: str) -> dict[str, object]:
    if re.fullmatch(r"D-\d{3}", query):
        matches = [record for record in records if record["id"] == query]
        if not matches:
            raise ValueError(f"unknown Decision ID: {query}")
    else:
        matches = [record for record in records if record["title"] == query]
        if not matches:
            raise ValueError(f"unknown indexed section name: {query}")
        ids = {record["id"] for record in matches}
        if len(ids) != 1:
            raise ValueError(f"ambiguous indexed section name: {query}")
    return min(matches, key=lambda record: SOURCES.index(record["source"]))


def source_range(record: dict[str, object]) -> str:
    path = ROOT / str(record["source"])
    content = []
    try:
        with path.open(encoding="utf-8") as source:
            for number, line in enumerate(source, 1):
                if number > record["end"]:
                    break
                if number >= record["start"]:
                    content.append(line)
    except OSError as error:
        raise ValueError(f"cannot read indexed source {record['source']}: {error}") from error
    if len(content) != record["end"] - record["start"] + 1:
        raise ValueError("stale line range: source is shorter than the index")
    heading = DECISION_HEADING.fullmatch(content[0].rstrip("\r\n"))
    if heading is None or heading.group(1) != record["id"] or heading.group(2) != record["title"]:
        raise ValueError("stale line range: indexed heading no longer matches")
    result = "".join(content)
    if hashlib.sha256(result.encode("utf-8")).hexdigest() != record["sha256"]:
        raise ValueError("stale line range: indexed content has changed")
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("decision", help="Decision ID or exact indexed decision title")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    try:
        record = select(read_index(), args.decision)
        content = source_range(record)
    except (UnicodeError, ValueError) as error:
        print(f"plan-get: {error}", file=sys.stderr)
        return 1
    sys.stdout.write(f"Source: {record['source']}\nLines: {record['start']}-{record['end']}\n\n")
    sys.stdout.write(content)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
