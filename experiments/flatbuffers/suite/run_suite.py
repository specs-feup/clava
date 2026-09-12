#!/usr/bin/env python3
"""Run the real Clava-JS Vitest suite with isolated cache and runtime state.

The runner records one JSON result from Vitest, GNU time resource metrics,
ccache statistics, environment/revision metadata, and flattened per-test
durations. It never writes outside this experiment's ignored ``results`` tree
except for the Vitest process itself, which runs against the checked-out suite.

Use one invocation per cache state. ``cold`` creates a new cache and measures
the first run. ``warm`` reuses an existing cache root supplied with
``--cache-root``; this lets a previous cold run populate it without hiding the
population run inside the measured observation. ``bypass`` sets CCACHE_DISABLE.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
from typing import Any, Iterable


EXPERIMENT_ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = EXPERIMENT_ROOT.parents[1]
CLAVA_JS_ROOT = CLAVA_ROOT / "Clava-JS"
RESULTS_ROOT = EXPERIMENT_ROOT / "suite" / "results"
DEFAULT_DUMPER = Path(
    "/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ast-flatbuffers/build/tool"
)
DEFAULT_DUMPER_REPO = DEFAULT_DUMPER.parents[1]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("bypass", "cold", "warm"), required=True)
    parser.add_argument(
        "--cache-root",
        type=Path,
        help="XDG cache root. Cold creates it; warm must point at a populated root.",
    )
    parser.add_argument("--format", choices=("text", "flat-eager", "flat-lazy"), default="text")
    parser.add_argument("--dumper", type=Path, default=DEFAULT_DUMPER)
    parser.add_argument("--dumper-repo", type=Path, default=DEFAULT_DUMPER_REPO)
    parser.add_argument("--output-root", type=Path, default=RESULTS_ROOT)
    parser.add_argument(
        "--runtime-root",
        type=Path,
        help="Optional staged Java runtime directory. Defaults to a new ignored directory.",
    )
    parser.add_argument(
        "--vitest-arg",
        action="append",
        default=[],
        help="Additional argument passed to `vitest run`; repeat for multiple arguments.",
    )
    parser.add_argument(
        "--no-stage-runtime",
        action="store_true",
        help="Use the packaged runtime as-is. This is only useful after its release tag is updated.",
    )
    return parser.parse_args()


def require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(f"{label} does not exist or is not a file: {path}")


def run_checked(command: list[str], *, cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, check=True, text=True, capture_output=True)


def stage_java_runtime(source: Path, destination: Path, dumper: Path) -> Path:
    """Copy the existing distribution and patch only the staged tag resource."""

    if destination.exists():
        raise SystemExit(f"Refusing to overwrite runtime staging directory: {destination}")
    shutil.copytree(source, destination)
    parser_jar = destination / "lib" / "ClangAstParser.jar"
    require_file(parser_jar, "staged ClangAstParser.jar")

    import zipfile

    temporary_jar = parser_jar.with_suffix(".patched.jar")
    # ClangAstWebResource's LocalBuild tag names the build directory; Java
    # appends the platform executable name (``tool`` on this host).
    tag = (str(dumper.resolve().parent) + "\n").encode()
    with zipfile.ZipFile(parser_jar) as original, zipfile.ZipFile(
        temporary_jar, "w", compression=zipfile.ZIP_DEFLATED
    ) as patched:
        for entry in original.infolist():
            data = tag if entry.filename == "clang-dumper-release.tag" else original.read(entry)
            patched.writestr(entry, data)
    temporary_jar.replace(parser_jar)
    return destination


def cache_dir(xdg_root: Path) -> Path:
    return xdg_root / "@specs-feup" / "clava" / "clang-dumper-ccache"


def ccache(command: str, cache: Path) -> str:
    completed = subprocess.run(
        ["ccache", "-d", str(cache), command],
        cwd=CLAVA_JS_ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    output = completed.stdout + completed.stderr
    if completed.returncode != 0:
        raise RuntimeError(f"ccache {command} failed ({completed.returncode}):\n{output}")
    return output


def parse_ccache_stats(text: str) -> dict[str, int | float | str]:
    values: dict[str, int | float | str] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        match = re.match(r"([^:]+):\s+([0-9][0-9,]*(?:\.[0-9]+)?)(?:\s+.*)?$", line)
        if not match:
            continue
        key = re.sub(r"[^a-z0-9]+", "_", match.group(1).strip().lower()).strip("_")
        raw_value = match.group(2).replace(",", "")
        values[key] = float(raw_value) if "." in raw_value else int(raw_value)
    return values


def parse_time_metrics(path: Path) -> dict[str, float | int]:
    metrics: dict[str, float | int] = {}
    for line in path.read_text().splitlines():
        key, _, value = line.partition("=")
        if not _:
            continue
        metrics[key] = float(value) if key not in {"max_rss_kb", "exit_status"} else int(float(value))
    return metrics


def iter_assertions(value: Any, suite_file: str = "") -> Iterable[tuple[str, dict[str, Any]]]:
    """Yield (suite file, assertion) for Vitest/Jest JSON variants."""

    if isinstance(value, dict):
        current_file = str(value.get("name") or value.get("filepath") or suite_file)
        assertions = value.get("assertionResults")
        if isinstance(assertions, list):
            for assertion in assertions:
                if isinstance(assertion, dict):
                    yield current_file, assertion
        for key in ("testResults", "files", "children"):
            children = value.get(key)
            if isinstance(children, list):
                for child in children:
                    yield from iter_assertions(child, current_file)
    elif isinstance(value, list):
        for child in value:
            yield from iter_assertions(child, suite_file)


def flatten_tests(report: dict[str, Any], csv_path: Path, report_path: Path, log_path: Path) -> dict[str, int]:
    rows: list[dict[str, str | int | float]] = []
    for ordinal, (suite_file, assertion) in enumerate(iter_assertions(report), start=1):
        status = str(assertion.get("status", "unknown"))
        duration = assertion.get("duration")
        duration_ms = "" if duration is None else float(duration)
        title = str(assertion.get("fullName") or assertion.get("name") or assertion.get("title") or "")
        rows.append(
            {
                "test_ordinal": ordinal,
                "suite_file": suite_file,
                "test_name": title,
                "status": status,
                "duration_ms": duration_ms,
                "raw_json_path": str(report_path),
                "raw_log_path": str(log_path),
            }
        )
    with csv_path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=[
            "test_ordinal", "suite_file", "test_name", "status", "duration_ms",
            "raw_json_path", "raw_log_path",
        ])
        writer.writeheader()
        writer.writerows(rows)

    counts = {"total_tests": len(rows), "passed_tests": 0, "failed_tests": 0, "pending_tests": 0}
    for row in rows:
        status = str(row["status"]).lower()
        if status in {"passed", "pass"}:
            counts["passed_tests"] += 1
        elif status in {"skipped", "pending", "todo"}:
            counts["pending_tests"] += 1
        else:
            counts["failed_tests"] += 1
    reported_files = report.get("testResults")
    counts["total_files"] = len(reported_files) if isinstance(reported_files, list) else len({str(row["suite_file"]) for row in rows})
    return counts


def git_revision(repo: Path) -> str:
    completed = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"], text=True, capture_output=True, check=False
    )
    return completed.stdout.strip() if completed.returncode == 0 else "unknown"


def git_status(repo: Path) -> dict[str, Any]:
    """Capture the exact checkout state used by a run, including dirtiness."""

    completed = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True,
        capture_output=True,
        check=False,
    )
    lines = completed.stdout.splitlines()
    return {
        "revision": git_revision(repo),
        "dirty": bool(lines) or completed.returncode != 0,
        "porcelain": lines,
        "status_return_code": completed.returncode,
    }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def filesystem_metadata(path: Path) -> dict[str, Any]:
    """Record the mounted filesystem containing a run-owned temporary path."""

    completed = subprocess.run(
        ["df", "-P", str(path)], text=True, capture_output=True, check=False
    )
    lines = completed.stdout.splitlines()
    if completed.returncode != 0 or len(lines) < 2:
        return {"path": str(path), "df_return_code": completed.returncode, "df_output": lines}
    fields = lines[-1].split()
    if len(fields) < 6:
        return {"path": str(path), "df_return_code": completed.returncode, "df_output": lines}
    return {
        "device": fields[0],
        "blocks_kb": int(fields[1]),
        "used_kb": int(fields[2]),
        "available_kb": int(fields[3]),
        "use_percent": fields[4],
        "mount": " ".join(fields[5:]),
    }


def prepare_temp_environment(path: Path) -> None:
    """Make Python staging and all descendants use the run-owned temp path."""

    path.mkdir(parents=True, exist_ok=True)
    value = str(path.resolve())
    os.environ.update({"TMPDIR": value, "TMP": value, "TEMP": value})
    tempfile.tempdir = value


def runtime_jar_manifest(root: Path) -> dict[str, Any]:
    """Hash packaged runtime jars using a canonical, order-independent manifest."""

    entries = {
        str(path.relative_to(root)): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }
    canonical = json.dumps(entries, sort_keys=True, separators=(",", ":")).encode()
    return {
        "root": str(root.resolve()),
        "jar_count": len(entries),
        "files": entries,
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
    }


def write_svg(summary_root: Path, svg_path: Path) -> None:
    """Create a dependency-free grouped-bar report from this runner's summaries."""

    summaries = [json.loads(path.read_text()) for path in sorted(summary_root.glob("*/summary.json"))]
    if not summaries:
        return
    width, row_height, left, chart_width = 1000, 110, 220, 700
    height = 90 + row_height * len(summaries)
    max_wall = max(float(item.get("wall_s", 0)) for item in summaries) or 1.0
    max_rss = max(float(item.get("max_rss_kb", 0)) for item in summaries) or 1.0
    colors = {"bypass": "#b35c44", "cold": "#2f6f9f", "warm": "#4f8a5b"}
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             '<style>text{font-family:system-ui,sans-serif;fill:#202124}.small{font-size:12px}.title{font-size:20px;font-weight:600}</style>',
             '<rect width="100%" height="100%" fill="white"/>', '<text x="20" y="32" class="title">Clava-JS suite observations</text>',
             '<text x="20" y="54" class="small">Wall time (blue scale) and peak RSS (green scale)</text>']
    for index, item in enumerate(summaries):
        y = 80 + index * row_height
        mode = str(item.get("mode", "unknown"))
        color = colors.get(mode, "#777")
        wall = float(item.get("wall_s", 0))
        rss = float(item.get("max_rss_kb", 0))
        wall_width = chart_width * wall / max_wall
        rss_width = chart_width * rss / max_rss
        parts.extend([
            f'<text x="20" y="{y + 18}">{mode}</text>',
            f'<rect x="{left}" y="{y + 4}" width="{wall_width:.2f}" height="20" fill="{color}"/>',
            f'<text x="{left + wall_width + 8:.2f}" y="{y + 19}" class="small">{wall:.2f}s</text>',
            f'<rect x="{left}" y="{y + 38}" width="{rss_width:.2f}" height="20" fill="#74a47a"/>',
            f'<text x="{left + rss_width + 8:.2f}" y="{y + 53}" class="small">{rss / 1024:.0f} MiB RSS</text>',
            f'<text x="20" y="{y + 53}" class="small">{item.get("total_tests", "?")} tests, {item.get("passed_tests", "?")} passed</text>',
        ])
    parts.append("</svg>")
    svg_path.write_text("\n".join(parts))


def main() -> int:
    args = parse_args()
    require_file(args.dumper, "clang-dumper executable")
    if not os.access(args.dumper, os.X_OK):
        raise SystemExit(f"clang-dumper is not executable: {args.dumper}")
    if args.mode == "warm" and args.cache_root is None:
        raise SystemExit("--mode warm requires --cache-root from a previous cold run")

    output_root = args.output_root.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    run_name = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ") + f"-{args.mode}"
    run_dir = output_root / run_name
    run_dir.mkdir()
    temp_root = run_dir / "tmp"
    prepare_temp_environment(temp_root)
    temp_metadata = filesystem_metadata(temp_root)

    if args.cache_root is None:
        xdg_root = Path(tempfile.mkdtemp(prefix="xdg-", dir=output_root))
    else:
        xdg_root = args.cache_root.resolve()
        if args.mode in {"bypass", "cold"} and xdg_root.exists():
            raise SystemExit(
                f"{args.mode} mode requires a new cache root, already exists: {xdg_root}"
            )
        xdg_root.mkdir(parents=True, exist_ok=True)
    cache = cache_dir(xdg_root)
    cache.parent.mkdir(parents=True, exist_ok=True)
    if args.mode == "cold":
        cache.mkdir(parents=True, exist_ok=True)
    elif args.mode == "warm" and not cache.is_dir():
        raise SystemExit(f"Warm cache directory does not exist: {cache}")

    runtime_root = args.runtime_root.resolve() if args.runtime_root else run_dir / "java-binaries"
    if args.no_stage_runtime:
        runtime_root = CLAVA_JS_ROOT / "java-binaries"
    else:
        stage_java_runtime(CLAVA_JS_ROOT / "java-binaries", runtime_root, args.dumper)
    runtime_manifest = runtime_jar_manifest(CLAVA_JS_ROOT / "java-binaries")
    clava_git = git_status(CLAVA_ROOT)
    dumper_git = git_status(args.dumper_repo)

    stats_before = ccache("--show-stats", cache) if cache.is_dir() else ""
    ccache("--zero-stats", cache)
    report_path = run_dir / "vitest.json"
    log_path = run_dir / "vitest.log"
    time_path = run_dir / "time.txt"
    command = [
        "/usr/bin/time", "-f", "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(time_path), "--", "npm", "exec", "--workspace", "@specs-feup/clava", "--",
        "vitest", "run", "--config", str(EXPERIMENT_ROOT / "suite" / "vitest.suite.config.ts"),
        "--reporter=json", "--outputFile", str(report_path), *args.vitest_arg,
    ]
    environment = os.environ.copy()
    environment["JAVA_TOOL_OPTIONS"] = environment.get("JAVA_TOOL_OPTIONS", "") + " -Djava.io.tmpdir=" + str(temp_root) + " -Dclava.astWire=" + args.format + " -Dclava.astWireMetrics=true"
    environment["XDG_CACHE_HOME"] = str(xdg_root)
    environment["CLAVA_SUITE_JAR_PATH"] = str(runtime_root)
    environment.pop("AST_WIRE_FLAT", None)
    environment.pop("AST_WIRE_DENSE_TEXT", None)
    if args.mode == "bypass":
        environment["CCACHE_DISABLE"] = "true"
    else:
        environment.pop("CCACHE_DISABLE", None)
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=CLAVA_JS_ROOT, env=environment, stdout=log, stderr=subprocess.STDOUT)
    wall_s = time.perf_counter() - started

    stats_after = ccache("--show-stats", cache)
    stats = parse_ccache_stats(stats_after)
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    test_counts = flatten_tests(report, run_dir / "per_test_timings.csv", report_path, log_path)
    time_metrics = parse_time_metrics(time_path) if time_path.is_file() else {}
    parse_metrics = []
    for match in re.finditer(r'CLAVA_AST_METRIC (\{[^\n]+\})', log_path.read_text(errors="replace")):
        try:
            parse_metrics.append(json.loads(match.group(1)))
        except json.JSONDecodeError:
            pass
    (run_dir / "parse-metrics.json").write_text(json.dumps(parse_metrics, indent=2) + "\n")
    summary = {
        "mode": args.mode,
        "format": args.format,
        "parse_count": len(parse_metrics),
        "parse_aggregate": {key: sum(m.get(key,0) for m in parse_metrics) for key in
            ["native_ms","read_ms","tu_ms","dump_bytes","nodes","deferred","materialized_at_tu"]},
        "return_code": process.returncode,
        "wall_s": wall_s,
        **time_metrics,
        **test_counts,
        "cache_root": str(xdg_root),
        "ccache_dir": str(cache),
        "ccache_stats": stats,
        "ccache_stats_before": parse_ccache_stats(stats_before),
        "dumper": str(args.dumper.resolve()),
        "dumper_sha256": hashlib.sha256(args.dumper.read_bytes()).hexdigest(),
        "clava_revision": git_revision(CLAVA_ROOT),
        "clang_dumper_revision": git_revision(args.dumper_repo),
        "dumper_repo": str(args.dumper_repo.resolve()),
        "runtime_jar_manifest": runtime_manifest,
        "temp_root": str(temp_root),
        "temp_filesystem": temp_metadata,
        "git": {"clava": clava_git, "clang_dumper": dumper_git},
        "vitest_success": report.get("success"),
        "vitest_total_test_suites": report.get("numTotalTestSuites"),
        "vitest_passed_test_suites": report.get("numPassedTestSuites"),
        "vitest_failed_test_suites": report.get("numFailedTestSuites"),
        "ccache_cacheable_calls": stats.get("cacheable_calls", 0),
        "ccache_direct_hits": stats.get("direct", 0),
        "ccache_misses": stats.get("misses", 0),
        "ccache_uncacheable_calls": stats.get("uncacheable_calls", 0),
        "command": command,
        "environment": {
            key: environment[key]
            for key in ("TMPDIR", "TMP", "TEMP", "XDG_CACHE_HOME", "CLAVA_SUITE_JAR_PATH", "CCACHE_DISABLE", "JAVA_TOOL_OPTIONS")
            if key in environment
        },
    }
    (run_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    (run_dir / "ccache.stats").write_text(stats_after)
    (run_dir / "ccache.stats.before").write_text(stats_before)
    write_svg(output_root, output_root / "suite-observations.svg")
    print(json.dumps({"run_dir": str(run_dir), **summary}, indent=2))
    return process.returncode


if __name__ == "__main__":
    raise SystemExit(main())
