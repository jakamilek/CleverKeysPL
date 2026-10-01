#!/usr/bin/env python3
"""Validate the Polish context benchmark JSONL without adding model assumptions.

The validator deliberately treats candidate_set=null as a valid pending record.
Only a record marked verified_real_decoder may carry a populated candidate slate,
and that slate must have parallel integer scores and contain the expected lexical key
(case-insensitively). The script never generates candidates.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


REQUIRED = {
    "id",
    "context",
    "input",
    "candidate_set",
    "candidate_scores",
    "expected_candidate_key",
    "expected_surface",
    "tags",
    "candidate_set_status",
}

STATUSES = {"pending_real_decoder", "verified_real_decoder"}


def fail(path: Path, line_no: int, message: str) -> None:
    raise SystemExit(f"{path}:{line_no}: ERROR: {message}")


def load_rows(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        raise SystemExit(f"ERROR: file not found: {path}")

    rows: list[dict[str, Any]] = []
    seen_ids: set[str] = set()

    for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip():
            continue
        try:
            row = json.loads(raw)
        except json.JSONDecodeError as exc:
            fail(path, line_no, f"invalid JSON: {exc}")

        if not isinstance(row, dict):
            fail(path, line_no, "record must be a JSON object")

        missing = REQUIRED - row.keys()
        if missing:
            fail(path, line_no, f"missing fields: {sorted(missing)}")

        if set(row.keys()) != REQUIRED:
            extra = set(row.keys()) - REQUIRED
            fail(path, line_no, f"unexpected fields: {sorted(extra)}")

        row_id = row["id"]
        if not isinstance(row_id, str) or not row_id.strip():
            fail(path, line_no, "id must be a non-empty string")
        if row_id in seen_ids:
            fail(path, line_no, f"duplicate id: {row_id}")
        seen_ids.add(row_id)

        if not isinstance(row["context"], list) or not all(
            isinstance(x, str) and x.strip() for x in row["context"]
        ):
            fail(path, line_no, "context must be a list of non-empty strings")

        if not isinstance(row["input"], str) or not row["input"].strip():
            fail(path, line_no, "input must be a non-empty string")

        for field in ("expected_candidate_key", "expected_surface"):
            if not isinstance(row[field], str) or not row[field].strip():
                fail(path, line_no, f"{field} must be a non-empty string")

        if not isinstance(row["tags"], list) or not row["tags"] or not all(
            isinstance(x, str) and x.strip() for x in row["tags"]
        ):
            fail(path, line_no, "tags must be a non-empty list of strings")

        status = row["candidate_set_status"]
        if status not in STATUSES:
            fail(path, line_no, f"candidate_set_status must be one of {sorted(STATUSES)}")

        candidates = row["candidate_set"]
        scores = row["candidate_scores"]

        if status == "pending_real_decoder":
            if candidates is not None or scores is not None:
                fail(
                    path,
                    line_no,
                    "pending_real_decoder records must keep candidate_set and candidate_scores null",
                )
        else:
            if not isinstance(candidates, list) or not candidates:
                fail(path, line_no, "verified_real_decoder requires non-empty candidate_set")
            if not isinstance(scores, list) or len(scores) != len(candidates):
                fail(path, line_no, "verified_real_decoder requires parallel candidate_scores")
            if not all(isinstance(x, str) and x.strip() for x in candidates):
                fail(path, line_no, "candidate_set must contain non-empty strings")
            if not all(isinstance(x, int) and not isinstance(x, bool) for x in scores):
                fail(path, line_no, "candidate_scores must contain integers")

            expected_key = row["expected_candidate_key"].casefold()
            candidate_keys = {candidate.casefold() for candidate in candidates}
            if expected_key not in candidate_keys:
                fail(
                    path,
                    line_no,
                    "expected_candidate_key is absent from the real decoder candidate set; "
                    "this case must be classified as decoder coverage/OOV, not as a reranker error",
                )

        rows.append(row)

    if not rows:
        raise SystemExit(f"{path}: ERROR: no benchmark records")

    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "path",
        nargs="?",
        type=Path,
        default=Path("docs/eval/polish_context_benchmark_seed.jsonl"),
    )
    args = parser.parse_args()

    rows = load_rows(args.path)
    pending = sum(r["candidate_set_status"] == "pending_real_decoder" for r in rows)
    verified = len(rows) - pending
    print(
        f"OK: {len(rows)} records; "
        f"pending_real_decoder={pending}; verified_real_decoder={verified}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
