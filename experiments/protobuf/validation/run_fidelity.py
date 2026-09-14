#!/usr/bin/env python3
"""Run source-fidelity commands for existing Clava/clang-dumper fixtures.

Each command is a shell-free template.  Supported substitutions are
``{source}``, ``{std}``, ``{output}``, ``{work}``, and ``{fixture}``.  The
command must write the generated source (or other source-fidelity artifact) to
``{output}``; the two artifacts are compared exactly.  No fixture is copied or
rewritten by this script.
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


CLAVA_ROOT = Path(__file__).resolve().parents[3]
WORKSPACE_ROOT = CLAVA_ROOT.parent
DEFAULT_FIXTURES = {
    "nas_bt": (WORKSPACE_ROOT / "clang-dumper/test/inputs/nas_bt.c", "c11"),
    "nas_lu": (CLAVA_ROOT / "ClangAstParser/test-resources/c/bench/nas_lu.c", "c11"),
    "wrap": (CLAVA_ROOT / "ClavaWeaver/resources/clava/test/weaver/cpp/src/wrap.cpp", "c++17"),
    "templates": (WORKSPACE_ROOT / "clang-dumper/test/inputs/templates.cpp", "c++17"),
    "comment": (CLAVA_ROOT / "ClangAstParser/test-resources/cxx/comment.cpp", "c++17"),
    "pragmas": (CLAVA_ROOT / "ClangAstParser/test-resources/cxx/pragmas.cpp", "c++17"),
    "literals": (CLAVA_ROOT / "ClangAstParser/test-resources/cxx/literals.cpp", "c++17"),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-command", required=True)
    parser.add_argument("--protobuf-command", required=True)
    parser.add_argument("--fixture", action="append", metavar="NAME=PATH[,STD]",
                        help="Override/add a fixture; repeat as needed.")
    parser.add_argument("--output-root", type=Path, required=True)
    return parser.parse_args()


def fixtures(values: list[str] | None) -> dict[str, tuple[Path, str]]:
    result = dict(DEFAULT_FIXTURES)
    for value in values or []:
        name, separator, spec = value.partition("=")
        if not separator:
            raise SystemExit(f"fixture must be NAME=PATH[,STD]: {value}")
        path, separator, standard = spec.partition(",")
        result[name] = (Path(path).resolve(), standard or "c++17")
    return result


def run(command: str, values: dict[str, str], cwd: Path, log: Path) -> int:
    rendered = [token.format(**values) for token in shlex.split(command)]
    with log.open("w") as output:
        result = subprocess.run(rendered, cwd=cwd, stdout=output, stderr=subprocess.STDOUT, check=False, text=True)
    return result.returncode


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    args = parse_args()
    args.output_root.mkdir(parents=True, exist_ok=False)
    outcomes: list[dict[str, Any]] = []
    for name, (source, standard) in fixtures(args.fixture).items():
        if not source.is_file():
            outcomes.append({"fixture": name, "source": str(source), "excluded": True, "reason": "fixture missing"})
            continue
        work = args.output_root / name
        work.mkdir()
        baseline_output, protobuf_output = work / "baseline.out", work / "protobuf.out"
        values = {"source": str(source), "std": standard, "fixture": name, "work": str(work), "output": str(baseline_output)}
        baseline_code = run(args.baseline_command, values, WORKSPACE_ROOT, work / "baseline.log")
        values["output"] = str(protobuf_output)
        protobuf_code = run(args.protobuf_command, values, WORKSPACE_ROOT, work / "protobuf.log")
        equal = baseline_code == 0 and protobuf_code == 0 and baseline_output.is_file() and protobuf_output.is_file() and baseline_output.read_bytes() == protobuf_output.read_bytes()
        outcome = {"fixture": name, "source": str(source), "standard": standard, "baseline_return_code": baseline_code, "protobuf_return_code": protobuf_code, "baseline_bytes": baseline_output.stat().st_size if baseline_output.is_file() else None, "protobuf_bytes": protobuf_output.stat().st_size if protobuf_output.is_file() else None, "baseline_sha256": sha256(baseline_output) if baseline_output.is_file() else None, "protobuf_sha256": sha256(protobuf_output) if protobuf_output.is_file() else None, "equal": equal, "excluded": False, "logs": [str(work / "baseline.log"), str(work / "protobuf.log")]}
        outcomes.append(outcome)
        print(json.dumps(outcome, sort_keys=True), flush=True)
    summary = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "normalization": "none; generated source compared exactly", "fixtures": outcomes, "failed": [item["fixture"] for item in outcomes if not item.get("excluded") and not item.get("equal")]}
    (args.output_root / "fidelity.json").write_text(json.dumps(summary, indent=2) + "\n")
    return 1 if summary["failed"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
