#!/usr/bin/env python3
"""Run the baseline/protobuf Clava-JS matrix with retained raw cells.

There are three states (bypass, cold, warm), three repetitions, and two
implementations.  Format order rotates on each repetition.  Cold and warm use
the same owned Java temporary root; bypass gets a new root.  A non-zero Vitest
status is recorded in the cell and does not discard its diagnostics.
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
EXPERIMENT_ROOT = SCRIPT_ROOT.parents[1]
CLAVA_ROOT = EXPERIMENT_ROOT.parents[1]
RESULTS_ROOT = SCRIPT_ROOT / "results"
IMPLEMENTATIONS = ("baseline", "protobuf")
MODES = ("cold", "warm", "bypass")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-dumper", type=Path, required=True)
    parser.add_argument("--protobuf-dumper", type=Path, required=True)
    parser.add_argument("--baseline-runtime", type=Path, required=True)
    parser.add_argument("--protobuf-runtime", type=Path, required=True)
    parser.add_argument("--baseline-dumper-repo", type=Path)
    parser.add_argument("--protobuf-dumper-repo", type=Path)
    parser.add_argument("--baseline-runtime-repo", type=Path)
    parser.add_argument("--protobuf-runtime-repo", type=Path)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--repeat-count", type=int, default=3)
    parser.add_argument("--expected-failures-file", type=Path,
                        help="JSON list of known test names; failures remain recorded, not hidden.")
    parser.add_argument("--wire-property", default="clava.astWire")
    parser.add_argument("--baseline-wire-value", default="text")
    parser.add_argument("--protobuf-wire-value", default="protobuf")
    parser.add_argument("--vitest-arg", action="append", default=[])
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_expected(path: Path | None) -> list[str]:
    if path is None:
        return []
    value = json.loads(path.read_text())
    if isinstance(value, list) and all(isinstance(item, str) for item in value):
        return value
    if isinstance(value, dict) and isinstance(value.get("failed_tests"), list):
        return [str(item) for item in value["failed_tests"]]
    raise SystemExit(f"expected failures file must be a JSON list or {{failed_tests: [...]}}: {path}")


def check_input(path: Path, label: str, executable: bool = False) -> None:
    if not path.exists():
        raise SystemExit(f"{label} does not exist: {path}")
    if executable and not path.is_file():
        raise SystemExit(f"{label} is not a file: {path}")
    if executable and not path.stat().st_mode & 0o111:
        raise SystemExit(f"{label} is not executable: {path}")


def find_summary(output_root: Path, before: set[Path]) -> Path | None:
    found = set(output_root.glob("*/summary.json")) - before
    return sorted(found)[-1] if found else None


def classify(summary: dict[str, Any] | None, return_code: int, log: Path) -> dict[str, Any]:
    if summary is None:
        return {"status": "excluded", "excluded": True, "reason": "runner produced no summary", "return_code": return_code, "log": str(log)}
    unexpected = summary.get("unexpected_failures", [])
    if unexpected:
        return {"status": "unexpected-test-failure", "excluded": False, "reason": "unexpected test failures retained", "return_code": return_code, "unexpected_failures": unexpected}
    if return_code != 0:
        return {"status": "expected-test-failure", "excluded": False, "reason": "only expected failures or non-Vitest diagnostics", "return_code": return_code}
    return {"status": "pass", "excluded": False, "reason": None, "return_code": return_code}


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    inputs = {
        "baseline": (args.baseline_dumper, args.baseline_runtime, args.baseline_dumper_repo, args.baseline_runtime_repo, args.baseline_wire_value),
        "protobuf": (args.protobuf_dumper, args.protobuf_runtime, args.protobuf_dumper_repo, args.protobuf_runtime_repo, args.protobuf_wire_value),
    }
    if args.baseline_dumper.resolve() == args.protobuf_dumper.resolve():
        raise SystemExit("baseline and protobuf dumpers must be separate executable inputs")
    if args.baseline_runtime.resolve() == args.protobuf_runtime.resolve():
        raise SystemExit("baseline and protobuf runtimes must be separate build inputs")
    for implementation, (dumper, runtime, *_rest) in inputs.items():
        check_input(dumper, f"{implementation} dumper", executable=True)
        check_input(runtime, f"{implementation} runtime")
    expected_failures = read_expected(args.expected_failures_file)
    output_root = (args.output_root or RESULTS_ROOT / ("matrix-" + dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ"))).resolve()
    if output_root.exists() and any(output_root.iterdir()):
        raise SystemExit(f"refusing to reuse non-empty output root: {output_root}")
    output_root.mkdir(parents=True, exist_ok=False)
    (output_root / "driver-logs").mkdir()
    (output_root / "temps").mkdir()

    plan: list[dict[str, Any]] = []
    for repeat in range(1, args.repeat_count + 1):
        rotation = (repeat - 1) % len(IMPLEMENTATIONS)
        order = IMPLEMENTATIONS[rotation:] + IMPLEMENTATIONS[:rotation]
        for implementation in order:
            pair_temp = output_root / "temps" / f"{implementation}-repeat-{repeat}-cold-warm"
            for mode in MODES:
                temp_root = pair_temp if mode in {"cold", "warm"} else output_root / "temps" / f"{implementation}-repeat-{repeat}-bypass"
                plan.append({"index": len(plan) + 1, "implementation": implementation, "repeat": repeat, "mode": mode, "temporary_root": str(temp_root)})
    metadata = {
        "output_root": str(output_root),
        "repeat_count": args.repeat_count,
        "implementations": list(IMPLEMENTATIONS),
        "modes": list(MODES),
        "format_order_by_repeat": "baseline/protobuf then protobuf/baseline",
        "cache_states": {
            "bypass": "CCACHE_DISABLE=true with a fresh isolated temporary root",
            "cold": "empty ccache below a new temporary root",
            "warm": "same temporary root and ccache as paired cold cell",
        },
        "completed_file_transport": True,
        "true_producer_consumer_overlap": False,
        "expected_failures": expected_failures,
        "inputs": {
            implementation: {
                "dumper": str(values[0].resolve()),
                "dumper_sha256": sha256_file(values[0]),
                "runtime": str(values[1].resolve()),
                "dumper_repo": str(values[2].resolve()) if values[2] else None,
                "runtime_repo": str(values[3].resolve()) if values[3] else None,
                "wire_value": values[4],
            }
            for implementation, values in inputs.items()
        },
        "cells": plan,
    }
    (output_root / "matrix-plan.json").write_text(json.dumps(metadata, indent=2) + "\n")

    runner = SCRIPT_ROOT / "run_suite.py"
    completed: list[dict[str, Any]] = []
    for cell in plan:
        implementation = cell["implementation"]
        dumper, runtime, dumper_repo, runtime_repo, wire_value = inputs[implementation]
        label = f"{implementation}-repeat-{cell['repeat']}-{cell['mode']}"
        log_path = output_root / "driver-logs" / f"{cell['index']:02d}-{label}.log"
        command = [sys.executable, str(runner), "--implementation", implementation, "--mode", cell["mode"],
                   "--dumper", str(dumper.resolve()), "--runtime", str(runtime.resolve()),
                   "--output-root", str(output_root), "--temp-root", cell["temporary_root"],
                   "--wire-property", args.wire_property, "--wire-value", wire_value]
        if dumper_repo:
            command += ["--dumper-repo", str(dumper_repo.resolve())]
        if runtime_repo:
            command += ["--runtime-repo", str(runtime_repo.resolve())]
        for failure in expected_failures:
            command += ["--expected-failure", failure]
        for vitest_arg in args.vitest_arg:
            command += ["--vitest-arg", vitest_arg]
        before = set(output_root.glob("*/summary.json"))
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=CLAVA_ROOT, stdout=log, stderr=subprocess.STDOUT, check=False, text=True)
        driver_elapsed = time.perf_counter() - started
        summary_path = find_summary(output_root, before)
        summary = json.loads(summary_path.read_text()) if summary_path else None
        result = {**cell, "command": command, "driver_elapsed_s": driver_elapsed,
                  "summary": str(summary_path) if summary_path else None,
                  "driver_log": str(log_path), **classify(summary, process.returncode, log_path)}
        completed.append(result)
        (output_root / "matrix-progress.json").write_text(json.dumps({**metadata, "completed": completed}, indent=2) + "\n")
        print(json.dumps(result, sort_keys=True), flush=True)

    final = {**metadata, "completed": completed, "complete": True,
             "excluded_trials": [item for item in completed if item["excluded"]],
             "unexpected_trials": [item for item in completed if item["status"] == "unexpected-test-failure"]}
    (output_root / "matrix-results.json").write_text(json.dumps(final, indent=2) + "\n")
    return 1 if final["excluded_trials"] or final["unexpected_trials"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
