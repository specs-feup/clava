#!/usr/bin/env python3
"""Re-run the pinned baseline's clean C/C++ cases through the selected dumper."""

from __future__ import annotations

import argparse
import concurrent.futures
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
from typing import Any


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def git_revision(path: Path) -> str | None:
    result = subprocess.run(["git", "-C", str(path), "rev-parse", "HEAD"],
                            capture_output=True, text=True, check=False)
    return result.stdout.strip() if result.returncode == 0 else None


def translate_flags(flags: list[str]) -> tuple[list[str], list[str]]:
    """Match the corpus baseline's cc1-to-driver flag conversion."""
    out: list[str] = []
    dropped: list[str] = []
    paired_drop = {"-target-feature", "-target-cpu", "-target-abi"}
    discard_pair = {"-aux-triple", "-main-file-name", "-internal-isystem",
                    "-internal-externc-isystem"}
    index = 0
    while index < len(flags):
        flag = flags[index]
        if flag == "-triple" and index + 1 < len(flags):
            out += ["-target", flags[index + 1]]
            index += 2
            continue
        if flag in paired_drop:
            dropped.append(f"{flag} {flags[index + 1]}" if index + 1 < len(flags) else flag)
            index += 2 if index + 1 < len(flags) else 1
            continue
        if flag in discard_pair:
            index += 2 if index + 1 < len(flags) else 1
            continue
        out.append(flag)
        index += 1
    return out, dropped


def baseline_driver_flags(item: dict[str, Any], resource_dir: str) -> tuple[list[str], list[str]]:
    flags, dropped = translate_flags(list(item["flags"]))
    has_standard = any(flag == "-std" or flag.startswith("-std=") or flag.startswith("-cl-std=")
                       for flag in flags)
    if not has_standard:
        effective = item.get("effective_std")
        if effective and str(effective).startswith("opencl"):
            flags += ["-cl-std=CL" + str(effective)[6:]]
        elif effective and effective != "cuda":
            flags += ["-std=" + str(effective)]
    if resource_dir:
        flags += ["-resource-dir", resource_dir]
    return flags, dropped


def run_case(args: tuple[str, str, str, str, str, dict[str, Any], str, str]) -> dict[str, Any]:
    source_text, relative, tool, verifier, resource_dir, item, output_root, _corpus = args
    source = Path(source_text)
    output = Path(output_root) / "buffers" / (relative + ".wire")
    output.parent.mkdir(parents=True, exist_ok=True)
    log = output.with_suffix(output.suffix + ".stderr")
    driver_flags, dropped = baseline_driver_flags(item, resource_dir)
    command = [tool, "-c", str(source), "-id=0", "-o", str(output), "--", *driver_flags]
    started = time.perf_counter()
    try:
        with log.open("wb") as diagnostics:
            process = subprocess.run(command, cwd=source.parent, stdout=diagnostics,
                                     stderr=subprocess.STDOUT, timeout=45, check=False)
        elapsed = time.perf_counter() - started
        size = output.stat().st_size if output.is_file() else 0
        verifier_command = [verifier, str(output)]
        verifier_result = None
        verifier_log = output.with_suffix(output.suffix + ".verify.log")
        if process.returncode == 0 and size > 0:
            verification = subprocess.run(verifier_command, cwd=source.parent, capture_output=True,
                                          timeout=30, check=False)
            verifier_result = verification.returncode
            verifier_log.write_bytes(verification.stdout + verification.stderr)
        bucket = "CLEAN" if process.returncode == 0 and size > 0 and verifier_result == 0 else (
            "BINARY_INVALID" if process.returncode == 0 and size > 0 else
            "EMPTY_BUFFER" if process.returncode == 0 else
            "CRASH" if process.returncode < 0 else "DUMP_FAIL"
        )
        return {
            "source": source_text,
            "relative": relative,
            "bucket": bucket,
            "return_code": process.returncode,
            "buffer_bytes": size,
            "buffer_sha256": sha256_file(output) if size else None,
            "stderr": str(log),
            "verifier_command": verifier_command,
            "verifier_return_code": verifier_result,
            "verifier_log": str(verifier_log) if verifier_result is not None else None,
            "elapsed_s": elapsed,
            "dropped_flags": dropped,
            "command": command,
        }
    except subprocess.TimeoutExpired:
        output.unlink(missing_ok=True)
        return {"source": source_text, "relative": relative, "bucket": "TIMEOUT",
                "elapsed_s": time.perf_counter() - started, "stderr": str(log),
                "command": command}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tool", type=Path, required=True)
    parser.add_argument("--verifier", type=Path, required=True,
                        help="Matching native verify_flatbuffers executable.")
    parser.add_argument("--baseline-results", type=Path, required=True,
                        help="The fixed LLVM 18.1.8 5,000-candidate results.json.")
    parser.add_argument("--corpus", type=Path, required=True,
                        help="The matching pinned clang/test checkout.")
    parser.add_argument("--clang", default="clang-18")
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--jobs", type=int, default=3)
    parser.add_argument("--limit", type=int, default=0, help="Optional leading clean-case smoke subset.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    tool = args.tool.resolve()
    verifier = args.verifier.resolve()
    baseline_path = args.baseline_results.resolve()
    corpus = args.corpus.resolve()
    for executable, label in ((tool, "dumper"), (verifier, "FlatBuffers verifier")):
        if not executable.is_file() or not os.access(executable, os.X_OK):
            raise SystemExit(f"{label} executable is missing or not executable: {executable}")
    if not baseline_path.is_file() or not corpus.is_dir():
        raise SystemExit("baseline results and matching corpus directory must exist")
    output_root = args.output_root.resolve()
    if output_root.exists() and any(output_root.iterdir()):
        raise SystemExit(f"refusing to reuse non-empty output directory: {output_root}")
    output_root.mkdir(parents=True, exist_ok=True)

    baseline = json.loads(baseline_path.read_text())
    clean = [(path, item) for path, item in baseline.items() if item.get("bucket") == "CLEAN"]
    if args.limit:
        clean = clean[:args.limit]
    if not clean:
        raise SystemExit("baseline contains no CLEAN candidates")
    for path_text, item in clean:
        path = Path(path_text).resolve()
        if corpus not in path.parents or not path.is_file():
            raise SystemExit(f"baseline source is missing or outside --corpus: {path}")
        if "flags" not in item:
            raise SystemExit(f"baseline has no extracted flags for CLEAN file: {path}")

    resource = subprocess.run([args.clang, "-print-resource-dir"], capture_output=True,
                              text=True, timeout=30, check=False)
    resource_dir = resource.stdout.strip() if resource.returncode == 0 else ""
    baseline_bytes = baseline_path.read_bytes()
    common = str(output_root)
    jobs = [
        (path_text, Path(path_text).resolve().relative_to(corpus).as_posix(),
         str(tool), str(verifier), resource_dir, item, common, str(corpus))
        for path_text, item in clean
    ]
    consumer_inputs = []
    for path_text, item in clean:
        source = Path(path_text).resolve()
        options, _dropped = baseline_driver_flags(item, resource_dir)
        consumer_inputs.append({
            "source": str(source),
            "relative": source.relative_to(corpus).as_posix(),
            "standard": item.get("std") or item.get("effective_std"),
            "options": options,
        })
    (output_root / "consumer-inputs.json").write_text(json.dumps({
        "corpus": str(corpus),
        "corpus_revision": git_revision(corpus.parent),
        "baseline_results_sha256": hashlib.sha256(baseline_bytes).hexdigest(),
        "files": consumer_inputs,
    }, indent=2) + "\n")
    started = time.perf_counter()
    results: list[dict[str, Any]] = []
    with concurrent.futures.ProcessPoolExecutor(max_workers=args.jobs) as pool:
        futures = [pool.submit(run_case, case) for case in jobs]
        for index, future in enumerate(concurrent.futures.as_completed(futures), start=1):
            results.append(future.result())
            if index % 100 == 0:
                print(f"completed {index}/{len(jobs)}", flush=True)
    results.sort(key=lambda row: row["relative"])
    counts: dict[str, int] = {}
    for row in results:
        counts[row["bucket"]] = counts.get(row["bucket"], 0) + 1
    regressions = [row for row in results if row["bucket"] != "CLEAN"]
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "baseline_candidates": len(baseline),
        "baseline_clean_cases": sum(item.get("bucket") == "CLEAN" for item in baseline.values()),
        "cases_run": len(results),
        "limit": args.limit or None,
        "bucket_counts": counts,
        "producer_regressions": len(regressions),
        "elapsed_s": time.perf_counter() - started,
        "corpus": str(corpus),
        "corpus_revision": git_revision(corpus.parent),
        "baseline_results": str(baseline_path),
        "baseline_results_sha256": hashlib.sha256(baseline_bytes).hexdigest(),
        "tool": str(tool),
        "tool_sha256": sha256_file(tool),
        "verifier": str(verifier),
        "verifier_sha256": sha256_file(verifier),
        "clang_resource_dir": resource_dir,
        "jobs": args.jobs,
        "results": str(output_root / "results.json"),
        "pass": len(results) == len(jobs) and not regressions,
        "classification": "A regression is any baseline CLEAN input not producing a non-empty stream that verify_flatbuffers accepts.",
        "timing_boundary": "Per-case wall time covers dumper process only; total includes executor startup and scheduling.",
    }
    (output_root / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps(summary, indent=2))
    return 0 if summary["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
