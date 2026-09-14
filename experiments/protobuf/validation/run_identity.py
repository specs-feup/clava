#!/usr/bin/env python3
"""Run parse -> generate -> reparse identity commands on existing fixtures.

The command template is supplied by the production integration test/helper and
must accept ``{source}``, ``{first}``, ``{second}``, ``{work}``, and ``{fixture}``.
It must write first-pass generated files under ``{first}`` and second-pass
generated files under ``{second}``.  This keeps the normal Clava construction
path in charge of parsing and generation while this script compares the
resulting source trees.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import shlex
import subprocess
from typing import Any

from run_fidelity import DEFAULT_FIXTURES, fixtures


WORKSPACE_ROOT = Path(__file__).resolve().parents[4]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-command", required=True)
    parser.add_argument("--protobuf-command", required=True)
    parser.add_argument("--fixture", action="append")
    parser.add_argument("--output-root", type=Path, required=True)
    return parser.parse_args()


def tree_hash(root: Path) -> tuple[str, int]:
    digest = hashlib.sha256()
    count = 0
    if not root.is_dir():
        return "", 0
    for path in sorted(item for item in root.rglob("*") if item.is_file()):
        relative = str(path.relative_to(root)).encode()
        digest.update(len(relative).to_bytes(8, "big")); digest.update(relative)
        data = path.read_bytes(); digest.update(len(data).to_bytes(8, "big")); digest.update(data); count += 1
    return digest.hexdigest(), count


def execute(template: str, values: dict[str, str], log: Path) -> int:
    command = [token.format(**values) for token in shlex.split(template)]
    with log.open("w") as output:
        result = subprocess.run(command, cwd=WORKSPACE_ROOT, stdout=output, stderr=subprocess.STDOUT, check=False, text=True)
    return result.returncode


def main() -> int:
    args = parse_args()
    args.output_root.mkdir(parents=True, exist_ok=False)
    outcomes: list[dict[str, Any]] = []
    for name, (source, standard) in fixtures(args.fixture).items():
        if not source.is_file():
            outcomes.append({"fixture": name, "excluded": True, "reason": "fixture missing"}); continue
        for implementation, template in (("baseline", args.baseline_command), ("protobuf", args.protobuf_command)):
            work = args.output_root / name / implementation; work.mkdir(parents=True)
            values = {"source": str(source), "std": standard, "fixture": name, "work": str(work), "first": str(work / "first"), "second": str(work / "second")}
            (work / "first").mkdir(); (work / "second").mkdir()
            code = execute(template, values, work / "pipeline.log")
            first_hash, first_count = tree_hash(work / "first"); second_hash, second_count = tree_hash(work / "second")
            outcome = {"fixture": name, "implementation": implementation, "return_code": code, "first_sha256": first_hash, "second_sha256": second_hash, "first_files": first_count, "second_files": second_count, "identity_equal": code == 0 and first_hash == second_hash, "log": str(work / "pipeline.log")}
            outcomes.append(outcome); print(json.dumps(outcome, sort_keys=True), flush=True)
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for outcome in outcomes:
        if "implementation" in outcome: by_fixture.setdefault(outcome["fixture"], []).append(outcome)
    mismatches = [name for name, values in by_fixture.items() if len(values) != 2 or any(not item["identity_equal"] for item in values) or values[0]["first_sha256"] != values[1]["first_sha256"] or values[0]["second_sha256"] != values[1]["second_sha256"]]
    summary = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "normalization": "none; tree paths and bytes compared exactly", "fixtures": outcomes, "failed": mismatches}
    (args.output_root / "identity.json").write_text(json.dumps(summary, indent=2) + "\n")
    return 1 if mismatches else 0


if __name__ == "__main__":
    raise SystemExit(main())
