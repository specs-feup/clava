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


def time_command(command_line: list[str], time_path: Path) -> list[str]:
    return [
        "/usr/bin/time", "-f",
        "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(time_path), "--", *command_line,
    ]


def run_one(producer: str, consumer: str, source: Path, work: Path, stream: bool) -> dict[str, Any]:
    work.mkdir(parents=True, exist_ok=True)
    output = work / ("stream.fifo" if stream else "record.bin")
    values = {"source": str(source), "input": str(output), "output": str(output), "work": str(work)}
    producer_command, consumer_command = command(producer, values), command(consumer, values)
    if stream:
        os.mkfifo(output)
    producer_log, consumer_log = work / "producer.log", work / "consumer.log"
    producer_time, consumer_time = work / "producer.time.txt", work / "consumer.time.txt"
    start = time.perf_counter()
    if stream:
        with consumer_log.open("w") as consumer_out, producer_log.open("w") as producer_out:
            timed_consumer = time_command(consumer_command, consumer_time)
            timed_producer = time_command(producer_command, producer_time)
            consumer_process = subprocess.Popen(timed_consumer, stdout=consumer_out, stderr=subprocess.STDOUT, text=True)
            producer_process = subprocess.Popen(timed_producer, stdout=producer_out, stderr=subprocess.STDOUT, text=True)
            producer_code = producer_process.wait(); consumer_code = consumer_process.wait()
    else:
        with producer_log.open("w") as producer_out:
            producer_process = subprocess.run(
                time_command(producer_command, producer_time),
                stdout=producer_out, stderr=subprocess.STDOUT, check=False, text=True)
        producer_code = producer_process.returncode
        with consumer_log.open("w") as consumer_out:
            consumer_process = subprocess.run(
                time_command(consumer_command, consumer_time),
                stdout=consumer_out, stderr=subprocess.STDOUT, check=False, text=True)
        consumer_code = consumer_process.returncode
    elapsed = time.perf_counter() - start
    producer_metrics = parse_time(producer_time)
    consumer_metrics = parse_time(consumer_time)
    peaks = [metric["max_rss_kb"] for metric in (producer_metrics, consumer_metrics) if "max_rss_kb" in metric]
    return {
        "stream": stream,
        "producer_return_code": producer_code,
        "consumer_return_code": consumer_code,
        "elapsed_s": elapsed,
        "producer_time": producer_metrics,
        "consumer_time": consumer_metrics,
        "peak_rss_kb": max(peaks) if peaks else None,
        "peak_rss_sum_kb_upper_bound": sum(peaks) if peaks else None,
        "peak_rss_semantics": "maximum single-process GNU time RSS; sum is an upper bound for FIFO overlap, not a simultaneous sample",
        "producer_peak_rss_kb": producer_metrics.get("max_rss_kb"),
        "consumer_peak_rss_kb": consumer_metrics.get("max_rss_kb"),
        "output_bytes": output.stat().st_size if output.exists() and not stream else None,
        "producer_log": str(producer_log),
        "consumer_log": str(consumer_log),
        "producer_time_file": str(producer_time),
        "consumer_time_file": str(consumer_time),
        "transport": "fifo-overlap" if stream else "completed-file",
    }


def main() -> int:
    args = parse_args()
    if args.repeats < 1: raise SystemExit("--repeats must be positive")
    if not args.source.is_file(): raise SystemExit(f"source does not exist: {args.source}")
    if bool(args.stream_producer) != bool(args.stream_consumer): raise SystemExit("provide both stream commands or neither")
    args.output_root.mkdir(parents=True, exist_ok=False)
    outcomes: list[dict[str, Any]] = []
    for repeat in range(1, args.repeats + 1):
        work = args.output_root / f"repeat-{repeat}"; work.mkdir()
        modes = [False, True] if repeat % 2 else [True, False]
        for stream in modes:
            if stream and not args.stream_producer:
                continue
            producer = args.stream_producer if stream else args.producer
            consumer = args.stream_consumer if stream else args.consumer
            result = run_one(producer, consumer, args.source, work / ("stream" if stream else "file"), stream)
            result.update({"repeat": repeat, "execution_order": "stream-first" if repeat % 2 == 0 else "file-first"})
            outcomes.append(result)
    summary = {"created_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "source": str(args.source.resolve()), "repeats": args.repeats, "completed_file_transport": True, "overlap_measurement": bool(args.stream_producer), "observations": outcomes, "failed": [item for item in outcomes if item["producer_return_code"] != 0 or item["consumer_return_code"] != 0]}
    (args.output_root / "incremental.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 1 if summary["failed"] else 0


if __name__ == "__main__": raise SystemExit(main())
