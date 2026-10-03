#!/usr/bin/env python3
"""Run the production eager Clava-JS cache matrix sequentially.

The matrix has one cold, one warm and one bypass run per repeat. A warm run
reuses its paired cold cache. Every invocation is kept as
the runner's normal raw result directory, while this script writes a manifest
that maps each matrix cell to its result. The fixed test filter excludes the
four host-dependent CUDA/OpenMP failures in every cache state.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import subprocess
import sys
import time
from typing import Any


SCRIPT_ROOT = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_ROOT.parents[3]
RESULTS_ROOT = SCRIPT_ROOT / "results"
DEFAULT_RUNTIME = CLAVA_ROOT / "ClavaWeaver" / "build" / "install" / "ClavaWeaver"
MODES = ("cold", "warm", "bypass")
EXPECTED_COUNTS = {
    "total_tests": 164,
    "passed_tests": 158,
    "failed_tests": 0,
    "pending_tests": 6,
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime-root", type=Path, default=DEFAULT_RUNTIME)
    parser.add_argument("--local-release-dir", type=Path)
    parser.add_argument(
        "--output-root",
        type=Path,
        help="Matrix output directory; defaults to a new timestamped results directory.",
    )
    parser.add_argument(
        "--repeat-count",
        type=int,
        default=3,
        help="Number of repeats per cache state (default: 3).",
    )
    parser.add_argument(
        "--vitest-arg",
        action="append",
        default=[],
        help="Additional argument passed to every vitest run; repeat for multiple arguments.",
    )
    return parser.parse_args()


def find_new_summary(output_root: Path, before: set[Path]) -> Path | None:
    candidates = set(output_root.glob("*/summary.json")) - before
    if len(candidates) != 1:
        return sorted(candidates)[-1] if candidates else None
    return candidates.pop()


def validate_cell(summary_path: Path, mode: str) -> list[str]:
    summary = json.loads(summary_path.read_text())
    errors = [
        f"{key}={summary.get(key)!r}, expected {expected}"
        for key, expected in EXPECTED_COUNTS.items()
        if summary.get(key) != expected
    ]
    if not summary.get("test_counts_valid"):
        errors.append("runner did not validate the fixed Clava-JS test population")
    if summary.get("wire_protocol") != "flatbuffers-eager":
        errors.append(f"wire protocol={summary.get('wire_protocol')!r}, expected eager FlatBuffers")
    release = summary.get("release", {})
    if not release.get("schema_sha256") or not release.get("tool_sha256"):
        errors.append("run did not record the selected schema and native executable hashes")
    stats = summary.get("ccache_stats", {})
    cacheable = int(stats.get("cacheable_calls", 0))
    hits = int(stats.get("direct", 0))
    misses = int(stats.get("misses", 0))
    if mode == "cold" and (cacheable == 0 or misses == 0):
        errors.append("cold run produced no ccache misses")
    if mode == "warm" and (cacheable == 0 or hits == 0):
        errors.append("warm run restored no ccache hits")
    if mode == "bypass" and (
        summary.get("environment", {}).get("CCACHE_DISABLE", "").lower() not in {"1", "true", "yes", "on"}
        or cacheable != 0 or hits != 0 or misses != 0
    ):
        errors.append("bypass did not disable all ccache calls")
    return errors


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    if not args.runtime_root.is_dir():
        raise SystemExit(f"ClavaWeaver runtime distribution does not exist: {args.runtime_root}")
    if args.local_release_dir is not None and not args.local_release_dir.is_dir():
        raise SystemExit(f"local release directory does not exist: {args.local_release_dir}")

    if args.output_root is None:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        output_root = RESULTS_ROOT / f"matrix-{stamp}"
    else:
        output_root = args.output_root
    output_root = output_root.resolve()
    if output_root.exists() and any(output_root.iterdir()):
        raise SystemExit(f"Refusing to reuse non-empty matrix output directory: {output_root}")
    output_root.mkdir(parents=True, exist_ok=False)
    log_root = output_root / "driver-logs"
    cache_root = output_root / "caches"
    log_root.mkdir()
    cache_root.mkdir()

    plan: list[dict[str, Any]] = []
    for repeat in range(1, args.repeat_count + 1):
        paired_cache = cache_root / f"eager-repeat-{repeat}-cold"
        for mode in MODES:
            plan.append(
                {
                    "index": len(plan) + 1,
                    "mode": mode,
                    "repeat": repeat,
                    "cache_root": str(paired_cache) if mode in {"cold", "warm"} else None,
                }
            )
    plan_metadata = {
        "output_root": str(output_root),
        "runtime_root": str(args.runtime_root.resolve()),
        "local_release_dir": str(args.local_release_dir.resolve()) if args.local_release_dir else None,
        "repeat_count": args.repeat_count,
        "wire_protocol": "flatbuffers-eager",
        "modes": list(MODES),
        "sequential": True,
        "runner": str((SCRIPT_ROOT / "run_suite.py").resolve()),
        "vitest_args": args.vitest_arg,
        "cells": plan,
    }
    (output_root / "matrix-plan.json").write_text(json.dumps(plan_metadata, indent=2) + "\n")

    results: list[dict[str, Any]] = []
    runner = SCRIPT_ROOT / "run_suite.py"
    for cell in plan:
        index = int(cell["index"])
        label = f'eager-repeat-{cell["repeat"]}-{cell["mode"]}'
        command = [
            sys.executable,
            str(runner),
            "--mode",
            str(cell["mode"]),
            "--runtime-root",
            str(args.runtime_root.resolve()),
            "--output-root",
            str(output_root),
        ]
        if args.local_release_dir is not None:
            command.extend(["--local-release-dir", str(args.local_release_dir.resolve())])
        if cell["cache_root"] is not None:
            command.extend(["--cache-root", str(cell["cache_root"])])
        for vitest_arg in args.vitest_arg:
            command.extend(["--vitest-arg", vitest_arg])
        log_path = log_root / f"{index:02d}-{label}.log"
        summaries_before = set(output_root.glob("*/summary.json"))
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(
                command,
                cwd=CLAVA_ROOT,
                stdout=log,
                stderr=subprocess.STDOUT,
                check=False,
                text=True,
            )
        elapsed = time.perf_counter() - started
        summary_path = find_new_summary(output_root, summaries_before)
        result = {
            **cell,
            "command": command,
            "driver_log": str(log_path),
            "driver_return_code": process.returncode,
            "driver_elapsed_s": elapsed,
            "summary": str(summary_path) if summary_path else None,
        }
        if summary_path is None:
            result["validation_errors"] = ["runner did not produce summary.json"]
        else:
            result["validation_errors"] = validate_cell(summary_path, str(cell["mode"]))
        results.append(result)
        (output_root / "matrix-progress.json").write_text(
            json.dumps({**plan_metadata, "completed": results}, indent=2) + "\n"
        )
        print(json.dumps(result, sort_keys=True), flush=True)
        if result["validation_errors"]:
            print(
                f"Stopping matrix after unexpected result in cell {index}: "
                + "; ".join(result["validation_errors"]),
                file=sys.stderr,
                flush=True,
            )
            return 1

    final = {**plan_metadata, "completed": results, "complete": True}
    (output_root / "matrix-results.json").write_text(json.dumps(final, indent=2) + "\n")
    return 0 if all(item["summary"] for item in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
