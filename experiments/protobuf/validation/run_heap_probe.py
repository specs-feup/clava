#!/usr/bin/env python3
"""Measure a production protobuf memory probe outside suite timing trials.

The supplied command owns AST construction and must print one JSON object per
phase using ``PROTOBUF_HEAP {json}`` or ``CLAVA_HEAP {json}``. A phase should
run an explicit GC before reporting ``used_bytes`` if it is intended to mean
retained heap. The wrapper records GNU time peak RSS separately.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import re
import shlex
import subprocess
import time
from typing import Any


HEAP_RE = re.compile(r"(?:PROTOBUF_HEAP|CLAVA_HEAP)\s+(\{[^\n]+\})")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--input", type=Path, help="Completed protobuf file or other probe input")
    parser.add_argument("--command", required=True, help="Probe command with {source}, {input}, {work} substitutions")
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--repeats", type=int, default=3)
    return parser.parse_args()


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    for line in path.read_text().splitlines():
        key, separator, value = line.partition("=")
        if not separator:
            continue
        values[key] = int(float(value)) if key in {"max_rss_kb", "exit_status"} else float(value)
    return values


def main() -> int:
    args = parse_args()
    if args.repeats < 1:
        raise SystemExit("--repeats must be positive")
    if not args.source.is_file():
        raise SystemExit(f"source does not exist: {args.source}")
    if args.input is not None and not args.input.is_file():
        raise SystemExit(f"probe input does not exist: {args.input}")
    args.output_root.mkdir(parents=True, exist_ok=False)
    observations: list[dict[str, Any]] = []
    for repeat in range(1, args.repeats + 1):
        work = args.output_root / f"repeat-{repeat}"
        work.mkdir()
        time_path, log_path = work / "time.txt", work / "probe.log"
        values = {
            "source": str(args.source.resolve()),
            "input": str(args.input.resolve()) if args.input else "",
            "work": str(work.resolve()),
        }
        rendered = [token.format(**values) for token in shlex.split(args.command)]
        command = [
            "/usr/bin/time", "-f",
            "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
            "-o", str(time_path), "--", *rendered,
        ]
        started = time.perf_counter()
        with log_path.open("w") as output:
            process = subprocess.run(command, stdout=output, stderr=subprocess.STDOUT, check=False, text=True)
        heap_phases: list[dict[str, Any]] = []
        for match in HEAP_RE.finditer(log_path.read_text(errors="replace")):
            try:
                value = json.loads(match.group(1))
            except json.JSONDecodeError:
                continue
            if isinstance(value, dict):
                heap_phases.append(value)
        observation = {
            "repeat": repeat,
            "return_code": process.returncode,
            "wall_s": time.perf_counter() - started,
            "gnu_time": parse_time(time_path) if time_path.is_file() else {},
            "heap_phases": heap_phases,
            "command": rendered,
            "log": str(log_path),
        }
        observations.append(observation)
        print(json.dumps(observation, sort_keys=True), flush=True)
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "source": str(args.source.resolve()),
        "input": str(args.input.resolve()) if args.input else None,
        "repeats": args.repeats,
        "retained_heap_contract": "production command emits phase used_bytes after explicit GC",
        "peak_rss_contract": "GNU time max_rss_kb",
        "observations": observations,
        "failed": [item for item in observations if item["return_code"] != 0 or not item["heap_phases"]],
    }
    (args.output_root / "memory.json").write_text(json.dumps(summary, indent=2) + "\n")
    return 1 if summary["failed"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
