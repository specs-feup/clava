#!/usr/bin/env python3
"""Measure raw versus zstd protobuf production on the same source.

Both commands are production commands and receive ``{source}``, ``{output}``,
and ``{work}`` substitutions.  Each run has a new work directory and separate
GNU ``time`` record, so output size, producer time, and peak RSS are retained
without folding them into a suite wall-time claim.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import shlex
import subprocess
import time
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--raw-command", required=True)
    parser.add_argument("--compressed-command", required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--repeats", type=int, default=3)
    return parser.parse_args()


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    if not path.is_file():
        return values
    for line in path.read_text().splitlines():
        key, separator, value = line.partition("=")
        if not separator:
            continue
        values[key] = int(float(value)) if key in {"max_rss_kb", "exit_status"} else float(value)
    return values


def run_command(template: str, source: Path, output: Path, work: Path) -> dict[str, Any]:
    values = {"source": str(source.resolve()), "output": str(output.resolve()), "work": str(work.resolve())}
    rendered = [token.format(**values) for token in shlex.split(template)]
    log = work / "producer.log"
    time_path = work / "time.txt"
    wrapped = [
        "/usr/bin/time", "-f",
        "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(time_path), "--", *rendered,
    ]
    started = time.perf_counter()
    with log.open("w") as output_log:
        process = subprocess.run(wrapped, stdout=output_log, stderr=subprocess.STDOUT, check=False, text=True)
    result = {
        "return_code": process.returncode,
        "wall_s": time.perf_counter() - started,
        "gnu_time": parse_time(time_path),
        "bytes": output.stat().st_size if output.is_file() else None,
        "command": rendered,
        "log": str(log),
        "time_file": str(time_path),
    }
    return result


def main() -> int:
    args = parse_args()
    if args.repeats < 1:
        raise SystemExit("--repeats must be positive")
    if not args.source.is_file():
        raise SystemExit(f"source does not exist: {args.source}")
    args.output_root.mkdir(parents=True, exist_ok=False)
    observations: list[dict[str, Any]] = []
    for repeat in range(1, args.repeats + 1):
        work = args.output_root / f"repeat-{repeat}"
        raw_work, compressed_work = work / "raw", work / "compressed"
        raw_work.mkdir(parents=True)
        compressed_work.mkdir()
        order = ("raw", "compressed") if repeat % 2 else ("compressed", "raw")
        results: dict[str, dict[str, Any]] = {}
        for kind in order:
            if kind == "raw":
                results[kind] = run_command(args.raw_command, args.source, raw_work / "dump.pb", raw_work)
            else:
                results[kind] = run_command(args.compressed_command, args.source, compressed_work / "dump.pb.zst", compressed_work)
        raw, compressed = results["raw"], results["compressed"]
        raw_bytes, compressed_bytes = raw["bytes"], compressed["bytes"]
        observation: dict[str, Any] = {"repeat": repeat, "execution_order": list(order), "raw": raw, "compressed": compressed}
        if isinstance(raw_bytes, int) and isinstance(compressed_bytes, int) and raw_bytes > 0:
            observation["compression_ratio"] = compressed_bytes / raw_bytes
            observation["size_reduction"] = 1.0 - observation["compression_ratio"]
        observations.append(observation)
        print(json.dumps(observation, sort_keys=True), flush=True)
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "source": str(args.source.resolve()),
        "repeats": args.repeats,
        "comparison": "same source, separate fresh output roots; raw and compressed producer metrics are not wall-time components",
        "observations": observations,
        "failed": [item for item in observations if item["raw"]["return_code"] != 0 or item["compressed"]["return_code"] != 0 or item["raw"]["bytes"] is None or item["compressed"]["bytes"] is None],
    }
    (args.output_root / "compression.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 1 if summary["failed"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
