#!/usr/bin/env python3
"""Measure completed-file transport and actual producer/consumer overlap.

Commands are shell-free templates with ``{input}``, ``{output}``, ``{source}``,
and ``{work}`` substitutions.  ``--producer`` and ``--consumer`` measure the
normal completed-file path sequentially.  Supplying ``--stream-producer`` and
``--stream-consumer`` additionally measures two processes connected by a FIFO,
which is the only result labeled overlap.  The probe is independent from the
Clava suite and is intended for a large existing fixture such as NAS LU.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import time
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--producer", required=True)
    parser.add_argument("--consumer", required=True)
    parser.add_argument("--stream-producer")
    parser.add_argument("--stream-consumer")
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--repeats", type=int, default=3)
    return parser.parse_args()


def command(template: str, values: dict[str, str]) -> list[str]:
    return [token.format(**values) for token in shlex.split(template)]


def run_one(producer: str, consumer: str, source: Path, work: Path, stream: bool) -> dict[str, Any]:
    work.mkdir(parents=True, exist_ok=True)
    output = work / ("stream.fifo" if stream else "record.bin")
    values = {"source": str(source), "input": str(output), "output": str(output), "work": str(work)}
    producer_command, consumer_command = command(producer, values), command(consumer, values)
    if stream:
        os.mkfifo(output)
    producer_log, consumer_log = work / "producer.log", work / "consumer.log"
    start = time.perf_counter()
    if stream:
        with consumer_log.open("w") as consumer_out, producer_log.open("w") as producer_out:
            consumer_process = subprocess.Popen(consumer_command, stdout=consumer_out, stderr=subprocess.STDOUT, text=True)
            producer_process = subprocess.Popen(producer_command, stdout=producer_out, stderr=subprocess.STDOUT, text=True)
            producer_code = producer_process.wait(); consumer_code = consumer_process.wait()
    else:
        with producer_log.open("w") as producer_out:
            producer_process = subprocess.run(producer_command, stdout=producer_out, stderr=subprocess.STDOUT, check=False, text=True)
        producer_code = producer_process.returncode
        with consumer_log.open("w") as consumer_out:
            consumer_process = subprocess.run(consumer_command, stdout=consumer_out, stderr=subprocess.STDOUT, check=False, text=True)
        consumer_code = consumer_process.returncode
    elapsed = time.perf_counter() - start
    return {"stream": stream, "producer_return_code": producer_code, "consumer_return_code": consumer_code, "elapsed_s": elapsed, "output_bytes": output.stat().st_size if output.exists() and not stream else None, "producer_log": str(producer_log), "consumer_log": str(consumer_log), "transport": "fifo-overlap" if stream else "completed-file"}


def main() -> int:
    args = parse_args()
    if args.repeats < 1: raise SystemExit("--repeats must be positive")
    if not args.source.is_file(): raise SystemExit(f"source does not exist: {args.source}")
    if bool(args.stream_producer) != bool(args.stream_consumer): raise SystemExit("provide both stream commands or neither")
    args.output_root.mkdir(parents=True, exist_ok=False)
    outcomes: list[dict[str, Any]] = []
    for repeat in range(1, args.repeats + 1):
        work = args.output_root / f"repeat-{repeat}"; work.mkdir()
        result = run_one(args.producer, args.consumer, args.source, work / "file", False); result.update({"repeat": repeat}); outcomes.append(result)
        if args.stream_producer:
            streamed = run_one(args.stream_producer, args.stream_consumer, args.source, work / "stream", True); streamed.update({"repeat": repeat}); outcomes.append(streamed)
    summary = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "source": str(args.source.resolve()), "repeats": args.repeats, "completed_file_transport": True, "overlap_measurement": bool(args.stream_producer), "observations": outcomes, "failed": [item for item in outcomes if item["producer_return_code"] != 0 or item["consumer_return_code"] != 0]}
    (args.output_root / "incremental.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 1 if summary["failed"] else 0


if __name__ == "__main__": raise SystemExit(main())
