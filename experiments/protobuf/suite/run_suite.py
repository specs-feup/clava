#!/usr/bin/env python3
"""Run one real Clava-JS suite observation for the text or protobuf build.

The runner deliberately treats the two builds as separate inputs.  It stages a
copy of the selected Java distribution and changes only the dumper tag in that
copy, so the checked-out release tag and production sources are never edited.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
from typing import Any, Iterable
import zipfile


EXPERIMENT_ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = EXPERIMENT_ROOT.parents[1]
CLAVA_JS_ROOT = CLAVA_ROOT / "Clava-JS"
RESULTS_ROOT = EXPERIMENT_ROOT / "suite" / "results"
KNOWN_METRIC_NAMES = {
    "native_ms", "read_ms", "decode_ms", "ast_ms", "tu_ms", "construction_ms",
    "dump_bytes", "cache_bytes", "nodes", "references", "deferred", "records",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--implementation", choices=("baseline", "protobuf"), required=True)
    parser.add_argument("--mode", choices=("bypass", "cold", "warm"), required=True)
    parser.add_argument("--dumper", type=Path, required=True)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--dumper-repo", type=Path)
    parser.add_argument("--runtime-repo", type=Path)
    parser.add_argument("--temp-root", "--cache-root", dest="temp_root", type=Path,
                        help="Stable Java temporary root for a cold/warm pair (cache-root is a legacy alias).")
    parser.add_argument("--output-root", type=Path, default=RESULTS_ROOT)
    parser.add_argument("--wire-property", default="clava.astWire")
    parser.add_argument("--wire-value", default=None)
    parser.add_argument("--runtime-no-stage", action="store_true")
    parser.add_argument("--expected-failure", action="append", default=[])
    parser.add_argument("--exclude-reason")
    parser.add_argument("--vitest-arg", action="append", default=[])
    return parser.parse_args()


def require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(f"{label} does not exist or is not a file: {path}")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def git_status(repo: Path | None) -> dict[str, Any] | None:
    if repo is None or not repo.exists():
        return None
    result = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True, capture_output=True, check=False,
    )
    revision = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"],
        text=True, capture_output=True, check=False,
    )
    lines = result.stdout.splitlines()
    return {
        "root": str(repo.resolve()),
        "revision": revision.stdout.strip() if revision.returncode == 0 else "unknown",
        "dirty": bool(lines) or result.returncode != 0,
        "porcelain": lines,
        "status_return_code": result.returncode,
    }


def runtime_manifest(root: Path) -> dict[str, Any]:
    files = {
        str(path.relative_to(root)): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }
    canonical = json.dumps(files, sort_keys=True, separators=(",", ":")).encode()
    return {
        "root": str(root.resolve()),
        "jar_count": len(files),
        "files": files,
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
    }


def stage_runtime(source: Path, destination: Path, dumper: Path) -> Path:
    if destination.exists():
        raise SystemExit(f"Refusing to overwrite runtime staging directory: {destination}")
    shutil.copytree(source, destination)
    parser_jar = destination / "lib" / "ClangAstParser.jar"
    require_file(parser_jar, "staged ClangAstParser.jar")
    temporary = parser_jar.with_suffix(".patched.jar")
    tag = (str(dumper.resolve().parent) + "\n").encode()
    found = False
    with zipfile.ZipFile(parser_jar) as original, zipfile.ZipFile(
        temporary, "w", compression=zipfile.ZIP_DEFLATED
    ) as patched:
        for entry in original.infolist():
            data = original.read(entry)
            if entry.filename == "clang-dumper-release.tag":
                data = tag
                found = True
            patched.writestr(entry, data)
    if not found:
        temporary.unlink(missing_ok=True)
        raise SystemExit(f"runtime has no clang-dumper-release.tag: {parser_jar}")
    temporary.replace(parser_jar)
    return destination


def run_ccache(args: list[str], cache: Path) -> str:
    result = subprocess.run(
        ["ccache", "-d", str(cache), *args],
        cwd=CLAVA_JS_ROOT, text=True, capture_output=True, check=False,
    )
    return result.stdout + result.stderr


def parse_stats(text: str) -> dict[str, int | float | str]:
    values: dict[str, int | float | str] = {}
    for raw in text.splitlines():
        match = re.match(r"([^:]+):\s+([0-9][0-9,]*(?:\.[0-9]+)?)(?:\s+.*)?$", raw.strip())
        if not match:
            continue
        key = re.sub(r"[^a-z0-9]+", "_", match.group(1).lower()).strip("_")
        value = match.group(2).replace(",", "")
        values[key] = float(value) if "." in value else int(value)
    return values


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


def file_bytes(path: Path) -> int:
    if not path.exists():
        return 0
    return sum(item.stat().st_size for item in path.rglob("*") if item.is_file())


def filesystem_metadata(path: Path) -> dict[str, Any]:
    result = subprocess.run(["df", "-P", str(path)], text=True, capture_output=True, check=False)
    lines = result.stdout.splitlines()
    if result.returncode != 0 or len(lines) < 2:
        return {"path": str(path), "df_return_code": result.returncode, "df_output": lines}
    fields = lines[-1].split()
    return {"device": fields[0], "blocks_kb": int(fields[1]), "used_kb": int(fields[2]),
            "available_kb": int(fields[3]), "use_percent": fields[4], "mount": " ".join(fields[5:])}


def flatten_tests(report: Any, csv_path: Path, report_path: Path, log_path: Path) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []

    def visit(value: Any, suite_file: str = "") -> None:
        if isinstance(value, dict):
            current = str(value.get("name") or value.get("filepath") or suite_file)
            assertions = value.get("assertionResults")
            if isinstance(assertions, list):
                for assertion in assertions:
                    if isinstance(assertion, dict):
                        rows.append({
                            "test_ordinal": len(rows) + 1,
                            "suite_file": current,
                            "test_name": str(assertion.get("fullName") or assertion.get("name") or assertion.get("title") or ""),
                            "status": str(assertion.get("status", "unknown")),
                            "duration_ms": assertion.get("duration", ""),
                            "raw_json_path": str(report_path),
                            "raw_log_path": str(log_path),
                        })
            for key in ("testResults", "files", "children"):
                children = value.get(key)
                if isinstance(children, list):
                    for child in children:
                        visit(child, current)
        elif isinstance(value, list):
            for child in value:
                visit(child, suite_file)

    visit(report)
    with csv_path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=["test_ordinal", "suite_file", "test_name", "status", "duration_ms", "raw_json_path", "raw_log_path"])
        writer.writeheader()
        writer.writerows(rows)
    counts = {"total_tests": len(rows), "passed_tests": 0, "failed_tests": 0, "pending_tests": 0}
    failures: list[str] = []
    for row in rows:
        status = row["status"].lower()
        if status in {"passed", "pass"}:
            counts["passed_tests"] += 1
        elif status in {"skipped", "pending", "todo"}:
            counts["pending_tests"] += 1
        else:
            counts["failed_tests"] += 1
            failures.append(str(row["test_name"]))
    counts["failed_test_names"] = failures
    return counts


def parse_metrics(log_path: Path) -> list[dict[str, Any]]:
    metrics: list[dict[str, Any]] = []
    pattern = re.compile(r"(?:CLAVA_AST_METRIC|PROTOBUF_METRIC)\s+(\{[^\n]+\})")
    for match in pattern.finditer(log_path.read_text(errors="replace")):
        try:
            value = json.loads(match.group(1))
        except json.JSONDecodeError:
            continue
        if isinstance(value, dict):
            metrics.append(value)
    return metrics


def aggregate_metrics(metrics: list[dict[str, Any]]) -> dict[str, int | float]:
    aggregate: dict[str, int | float] = {}
    for metric in metrics:
        for key, value in metric.items():
            if key not in KNOWN_METRIC_NAMES and not key.endswith("_ms") and not key.endswith("_bytes"):
                continue
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                aggregate[key] = aggregate.get(key, 0) + value
    return aggregate


def main() -> int:
    args = parse_args()
    require_file(args.dumper, "clang-dumper executable")
    if not os.access(args.dumper, os.X_OK):
        raise SystemExit(f"clang-dumper is not executable: {args.dumper}")
    if not args.runtime.is_dir():
        raise SystemExit(f"Java runtime directory does not exist: {args.runtime}")
    if args.exclude_reason:
        excluded = True
    else:
        excluded = False
    output_root = args.output_root.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    run_name = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ") + f"-{args.implementation}-{args.mode}"
    run_dir = output_root / run_name
    run_dir.mkdir()
    temp_root = (args.temp_root or run_dir / "tmp").resolve()
    if not str(temp_root).startswith(str(output_root) + os.sep):
        raise SystemExit(f"temporary root must be owned below output root: {temp_root}")
    if args.mode in {"cold", "bypass"} and temp_root.exists() and any(temp_root.iterdir()):
        raise SystemExit(f"{args.mode} temporary root must be new and empty: {temp_root}")
    if args.mode == "warm" and not temp_root.is_dir():
        raise SystemExit(f"warm temporary root does not exist: {temp_root}")
    temp_root.mkdir(parents=True, exist_ok=True)
    os.environ.update({"TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root)})
    tempfile.tempdir = str(temp_root)
    # SpecsIo.getTempFolder("clang_ast_exe") appends the Java user name on
    # Linux. Keep this in sync so ccache counters are captured before the JVM
    # creates the folder rather than accidentally inspecting a different cache.
    cache_folder = ("clang-dumper-protobuf-ccache-v1"
                    if args.implementation == "protobuf"
                    else "clang-dumper-ccache")
    cache = temp_root / f"clang_ast_exe_{getpass.getuser()}" / cache_folder
    if args.mode == "warm" and not cache.is_dir():
        raise SystemExit(f"warm ccache directory does not exist: {cache}")
    if args.mode == "cold":
        cache.mkdir(parents=True, exist_ok=True)
    runtime_root = run_dir / "java-binaries"
    if args.runtime_no_stage:
        runtime_root = args.runtime.resolve()
    else:
        stage_runtime(args.runtime.resolve(), runtime_root, args.dumper)
    before_bytes = file_bytes(temp_root)
    stats_before_text = run_ccache(["--show-stats"], cache) if cache.is_dir() else ""
    if cache.is_dir():
        run_ccache(["--zero-stats"], cache)
    report_path, log_path, time_path = run_dir / "vitest.json", run_dir / "vitest.log", run_dir / "time.txt"
    wire_value = args.wire_value or ("protobuf" if args.implementation == "protobuf" else "text")
    java_options = os.environ.get("JAVA_TOOL_OPTIONS", "") + f" -Djava.io.tmpdir={temp_root}"
    if args.wire_property:
        java_options += f" -D{args.wire_property}={wire_value}"
    environment = os.environ.copy()
    environment.update({"JAVA_TOOL_OPTIONS": java_options, "CLAVA_SUITE_JAR_PATH": str(runtime_root)})
    if args.mode == "bypass":
        environment["CCACHE_DISABLE"] = "true"
    else:
        environment.pop("CCACHE_DISABLE", None)
    command = [
        "/usr/bin/time", "-f", "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(time_path), "--", "npm", "exec", "--workspace", "@specs-feup/clava", "--",
        "vitest", "run", "--config", str(EXPERIMENT_ROOT / "suite" / "vitest.suite.config.ts"),
        "--reporter=json", "--outputFile", str(report_path), *args.vitest_arg,
    ]
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=CLAVA_JS_ROOT, env=environment, stdout=log, stderr=subprocess.STDOUT, check=False)
    wall_s = time.perf_counter() - started
    stats_after_text = run_ccache(["--show-stats"], cache) if cache.is_dir() else ""
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    counts = flatten_tests(report, run_dir / "per_test_timings.csv", report_path, log_path)
    failures = counts.pop("failed_test_names", [])
    expected = [name for name in failures if name in set(args.expected_failure)]
    unexpected = [name for name in failures if name not in set(args.expected_failure)]
    metrics = parse_metrics(log_path)
    (run_dir / "parse-metrics.json").write_text(json.dumps(metrics, indent=2) + "\n")
    time_metrics = parse_time(time_path)
    stats = parse_stats(stats_after_text)
    summary = {
        "implementation": args.implementation,
        "mode": args.mode,
        "wire_value": wire_value,
        "return_code": process.returncode,
        "wall_s": wall_s,
        **time_metrics,
        **counts,
        "failed_tests": failures,
        "expected_failures": expected,
        "unexpected_failures": unexpected,
        "excluded": excluded,
        "exclusion_reason": args.exclude_reason,
        "parse_count": len(metrics),
        "parse_aggregate": aggregate_metrics(metrics),
        "temporary_root": str(temp_root),
        "temporary_files_before_bytes": before_bytes,
        "temporary_files_after_bytes": file_bytes(temp_root),
        "cache_root": str(cache),
        "cache_bytes": file_bytes(cache),
        "cache_transport": "completed-file",
        "producer_consumer_overlap": False,
        "ccache_stats": stats,
        "ccache_stats_before": parse_stats(stats_before_text),
        "ccache_cacheable_calls": stats.get("cacheable_calls", 0),
        "ccache_direct_hits": stats.get("direct", 0),
        "ccache_misses": stats.get("misses", 0),
        "ccache_uncacheable_calls": stats.get("uncacheable_calls", 0),
        "dumper": str(args.dumper.resolve()),
        "dumper_sha256": sha256_file(args.dumper),
        "runtime": str(args.runtime.resolve()),
        "runtime_jar_manifest": runtime_manifest(args.runtime.resolve()),
        "clava_git": git_status(CLAVA_ROOT),
        "dumper_git": git_status(args.dumper_repo),
        "runtime_git": git_status(args.runtime_repo),
        "temporary_filesystem": filesystem_metadata(temp_root),
        "command": command,
        "environment": {key: environment[key] for key in ("TMPDIR", "TMP", "TEMP", "CLAVA_SUITE_JAR_PATH", "CCACHE_DISABLE", "JAVA_TOOL_OPTIONS") if key in environment},
        "vitest_success": report.get("success"),
        "raw_files": {"vitest": str(report_path), "log": str(log_path), "time": str(time_path), "ccache": str(run_dir / "ccache.stats")},
    }
    (run_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    (run_dir / "ccache.stats").write_text(stats_after_text)
    (run_dir / "ccache.stats.before").write_text(stats_before_text)
    print(json.dumps({"run_dir": str(run_dir), **summary}, indent=2))
    return process.returncode


if __name__ == "__main__":
    raise SystemExit(main())
