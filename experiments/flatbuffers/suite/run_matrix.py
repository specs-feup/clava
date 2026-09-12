#!/usr/bin/env python3
"""Run the complete format/cache experiment matrix sequentially.

The matrix has one cold, one warm and one bypass run for each format and
repeat. A warm run reuses its paired cold cache. Every invocation is kept as
the runner's normal raw result directory, while this script writes a manifest
that maps each matrix cell to its result and preserves expected suite failures
without treating them as driver failures.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
from typing import Any


SCRIPT_ROOT = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_ROOT.parents[3]
RESULTS_ROOT = SCRIPT_ROOT / "results"
DEFAULT_DUMPER = RESULTS_ROOT / "dumper-snapshot" / "tool"
DEFAULT_DUMPER_REPO = Path(
    "/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ast-flatbuffers"
)
FORMATS = ("text", "flat-eager", "flat-lazy")
MODES = ("cold", "warm", "bypass")
EXPECTED_STATUSES = {"passed", "pass", "skipped", "pending", "todo"}
EXPECTED_COUNTS = {
    "total_tests": 164,
    "passed_tests": 158,
    "failed_tests": 4,
    "pending_tests": 2,
}
KNOWN_FAILURES = {
    "CxxTest OmpThreadsExplore",
    "CudaTest Cuda",
    "CudaTest CudaMatrixMul",
    "CudaTest CudaQuery",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dumper", type=Path, default=DEFAULT_DUMPER)
    parser.add_argument("--dumper-repo", type=Path, default=DEFAULT_DUMPER_REPO)
    parser.add_argument(
        "--output-root",
        type=Path,
        help="Matrix output directory; defaults to a new timestamped results directory.",
    )
    parser.add_argument(
        "--repeat-count",
        type=int,
        default=3,
        help="Number of repeats per format and cache state (default: 3).",
    )
    parser.add_argument(
        "--vitest-arg",
        action="append",
        default=[],
        help="Additional argument passed to every vitest run; repeat for multiple arguments.",
    )
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def failed_tests(summary_path: Path) -> list[str]:
    timings_path = summary_path.parent / "per_test_timings.csv"
    if not timings_path.is_file():
        return []
    import csv

    with timings_path.open(newline="") as source:
        return [
            row.get("test_name", "")
            for row in csv.DictReader(source)
            if row.get("status", "").lower() not in EXPECTED_STATUSES
        ]


def find_new_summary(output_root: Path, before: set[Path]) -> Path | None:
    candidates = set(output_root.glob("*/summary.json")) - before
    if len(candidates) != 1:
        return sorted(candidates)[-1] if candidates else None
    return candidates.pop()


def validate_cell(summary_path: Path) -> list[str]:
    summary = json.loads(summary_path.read_text())
    errors = [
        f"{key}={summary.get(key)!r}, expected {expected}"
        for key, expected in EXPECTED_COUNTS.items()
        if summary.get(key) != expected
    ]
    observed_failures = set(failed_tests(summary_path))
    if observed_failures != KNOWN_FAILURES:
        errors.append(
            "failed tests="
            + repr(sorted(observed_failures))
            + ", expected="
            + repr(sorted(KNOWN_FAILURES))
        )
    return errors


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    if not args.dumper.is_file():
        raise SystemExit(f"clang-dumper executable does not exist: {args.dumper}")
    if not args.dumper.stat().st_mode & 0o111:
        raise SystemExit(f"clang-dumper is not executable: {args.dumper}")
    if not args.dumper_repo.is_dir():
        raise SystemExit(f"clang-dumper repository does not exist: {args.dumper_repo}")

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
        rotation = (repeat - 1) % len(FORMATS)
        format_order = FORMATS[rotation:] + FORMATS[:rotation]
        for format_name in format_order:
            paired_cache = cache_root / f"{format_name}-repeat-{repeat}-cold"
            for mode in MODES:
                plan.append(
                    {
                        "index": len(plan) + 1,
                        "format": format_name,
                        "mode": mode,
                        "repeat": repeat,
                        "cache_root": str(paired_cache) if mode in {"cold", "warm"} else None,
                    }
                )
    plan_metadata = {
        "output_root": str(output_root),
        "dumper": str(args.dumper.resolve()),
        "dumper_sha256": sha256_file(args.dumper),
        "dumper_repo": str(args.dumper_repo.resolve()),
        "repeat_count": args.repeat_count,
        "formats": list(FORMATS),
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
        label = f'{cell["format"]}-repeat-{cell["repeat"]}-{cell["mode"]}'
        command = [
            sys.executable,
            str(runner),
            "--mode",
            str(cell["mode"]),
            "--format",
            str(cell["format"]),
            "--dumper",
            str(args.dumper.resolve()),
            "--dumper-repo",
            str(args.dumper_repo.resolve()),
            "--output-root",
            str(output_root),
        ]
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
            "failed_tests": failed_tests(summary_path) if summary_path else [],
        }
        if summary_path is None:
            result["validation_errors"] = ["runner did not produce summary.json"]
        else:
            result["validation_errors"] = validate_cell(summary_path)
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
