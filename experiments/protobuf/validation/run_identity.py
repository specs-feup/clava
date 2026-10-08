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
import re
import shutil
import shlex
import subprocess
from typing import Any

from run_fidelity import DEFAULT_FIXTURES, fixtures


WORKSPACE_ROOT = Path(__file__).resolve().parents[4]
IDENTITY_RE = re.compile(r"^PROTOBUF_IDENTITY\s+(\{.*\})$", re.MULTILINE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-command", required=True)
    parser.add_argument("--protobuf-command", required=True)
    parser.add_argument("--fixture", action="append")
    parser.add_argument("--only", action="append", metavar="NAME",
                        help="Run only these fixture names after applying --fixture overrides.")
    parser.add_argument("--shared-generated", action="store_true",
                        help="Reuse absolute generated paths across implementations for graph comparison.")
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


def parse_identity(log: Path) -> dict[str, Any] | None:
    """Read the helper's machine-readable graph digest from its log."""
    match = IDENTITY_RE.search(log.read_text(errors="replace"))
    if match is None:
        return None
    try:
        payload = json.loads(match.group(1))
    except json.JSONDecodeError:
        return None
    return payload if isinstance(payload, dict) else None


def main() -> int:
    args = parse_args()
    args.output_root.mkdir(parents=True, exist_ok=False)
    outcomes: list[dict[str, Any]] = []
    selected_fixtures = fixtures(args.fixture)
    if args.only:
        unknown = sorted(set(args.only) - set(selected_fixtures))
        if unknown:
            raise SystemExit(f"unknown fixture(s): {', '.join(unknown)}")
        selected_fixtures = {name: selected_fixtures[name] for name in args.only}
    for name, (source, standard) in selected_fixtures.items():
        if not source.is_file():
            outcomes.append({"fixture": name, "excluded": True, "reason": "fixture missing"}); continue
        for implementation, template in (("baseline", args.baseline_command), ("protobuf", args.protobuf_command)):
            work = args.output_root / name / implementation; work.mkdir(parents=True)
            if args.shared_generated:
                generated = args.output_root / name / "shared-generated"
                first = generated / "first"
                second = generated / "second"
            else:
                first = work / "first"
                second = work / "second"
            for directory in (first, second):
                if directory.exists():
                    shutil.rmtree(directory)
                directory.mkdir(parents=True)
            values = {"source": str(source), "std": standard, "fixture": name, "work": str(work), "first": str(first), "second": str(second)}
            code = execute(template, values, work / "pipeline.log")
            first_hash, first_count = tree_hash(first); second_hash, second_count = tree_hash(second)
            if args.shared_generated:
                for source_dir, artifact_dir in ((first, work / "first"), (second, work / "second")):
                    if artifact_dir.exists():
                        shutil.rmtree(artifact_dir)
                    shutil.copytree(source_dir, artifact_dir)
            payload = parse_identity(work / "pipeline.log")
            outcome = {"fixture": name, "implementation": implementation, "return_code": code, "first_sha256": first_hash, "second_sha256": second_hash, "first_files": first_count, "second_files": second_count, "identity_equal": code == 0 and first_hash == second_hash, "log": str(work / "pipeline.log")}
            if payload is not None:
                outcome.update({key: payload.get(key) for key in ("first_graph_sha256", "second_graph_sha256")})
            outcomes.append(outcome); print(json.dumps(outcome, sort_keys=True), flush=True)
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for outcome in outcomes:
        if "implementation" in outcome: by_fixture.setdefault(outcome["fixture"], []).append(outcome)
    mismatches = [name for name, values in by_fixture.items() if len(values) != 2 or any(not item["identity_equal"] for item in values) or values[0]["first_sha256"] != values[1]["first_sha256"] or values[0]["second_sha256"] != values[1]["second_sha256"]]
    graph_mismatches: list[str] = []
    graph_reparse_mismatches: list[str] = []
    for name, values in by_fixture.items():
        by_implementation = {item["implementation"]: item for item in values}
        baseline = by_implementation.get("baseline")
        protobuf = by_implementation.get("protobuf")
        if baseline is None or protobuf is None:
            continue
        graph_fields = ("first_graph_sha256", "second_graph_sha256")
        if all(field in baseline and field in protobuf for field in graph_fields):
            for field in graph_fields:
                baseline[field.removesuffix("_graph_sha256") + "_graph_equal"] = baseline[field] == protobuf[field]
            if baseline["first_graph_sha256"] != protobuf["first_graph_sha256"]:
                graph_mismatches.append(name)
            if baseline["second_graph_sha256"] != protobuf["second_graph_sha256"]:
                graph_reparse_mismatches.append(name)
    summary = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "normalization": "none; generated trees compared by exact relative paths and bytes; graph digests normalize only node IDs/pointer identity to traversal ordinals", "fixtures": outcomes, "failed": mismatches, "graph_mismatches": graph_mismatches, "graph_reparse_mismatches": graph_reparse_mismatches}
    (args.output_root / "identity.json").write_text(json.dumps(summary, indent=2) + "\n")
    return 1 if mismatches else 0


if __name__ == "__main__":
    raise SystemExit(main())
