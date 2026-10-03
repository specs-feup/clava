#!/usr/bin/env python3
"""Run the source-level ClangAstParser fixed 116-test workload."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = ROOT.parents[1]
RESULTS_ROOT = ROOT / "results" / "validation"
INIT_SCRIPT = Path(__file__).with_name("java-suite.init.gradle")
EXPECTED = {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0}


def git_state() -> dict[str, object]:
    revision = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=CLAVA_ROOT,
        capture_output=True, text=True, check=False,
    )
    status = subprocess.run(
        ["git", "status", "--porcelain=v1", "--untracked-files=all"], cwd=CLAVA_ROOT,
        capture_output=True, text=True, check=False,
    )
    return {
        "revision": revision.stdout.strip() if revision.returncode == 0 else "unknown",
        "dirty": status.returncode != 0 or bool(status.stdout.strip()),
        "porcelain": status.stdout.splitlines(),
    }


def host_state() -> dict[str, object]:
    state: dict[str, object] = {}
    try:
        state["loadavg"] = Path("/proc/loadavg").read_text().strip()
    except OSError:
        pass
    try:
        state["meminfo_kib"] = {
            key: int(value.split()[0])
            for line in Path("/proc/meminfo").read_text().splitlines()
            for key, _, value in [line.partition(":")]
            if key in {"MemTotal", "MemAvailable", "SwapTotal", "SwapFree"}
        }
    except OSError:
        pass
    return state


def parse_junit(result_dir: Path) -> dict[str, int]:
    counts = {"total_tests": 0, "passed_tests": 0, "failed_tests": 0, "skipped_tests": 0}
    for path in sorted(result_dir.glob("TEST-*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as error:
            raise RuntimeError(f"malformed JUnit report {path}: {error}") from error
        for testcase in root.iter("testcase"):
            counts["total_tests"] += 1
            if testcase.find("failure") is not None or testcase.find("error") is not None:
                counts["failed_tests"] += 1
            elif testcase.find("skipped") is not None:
                counts["skipped_tests"] += 1
            else:
                counts["passed_tests"] += 1
    return counts


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    if not path.is_file():
        return values
    for line in path.read_text().splitlines():
        key, separator, value = line.partition("=")
        if not separator:
            continue
        values[key] = int(value) if key in {"max_rss_kb", "exit_status"} else float(value)
    return values


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--gradle", default="gradle")
    parser.add_argument("--offline", action="store_true", help="Require all Gradle dependencies to be cached.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output = args.output_root.resolve() if args.output_root else RESULTS_ROOT / dt.datetime.now(
        dt.timezone.utc
    ).strftime("java-suite-%Y%m%dT%H%M%SZ")
    output.mkdir(parents=True, exist_ok=False)
    test_results = CLAVA_ROOT / "ClangAstParser" / "build" / "test-results" / "test"
    if test_results.exists():
        shutil.rmtree(test_results)

    temp_root = output / "tmp"
    xdg_root = output / "xdg-cache"
    temp_root.mkdir()
    xdg_root.mkdir()
    command = [
        "/usr/bin/time", "-f", "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(output / "time.txt"), "--", args.gradle, "--no-daemon",
        "--init-script", str(INIT_SCRIPT), "-p", "ClangAstParser", "test",
    ]
    if args.offline:
        command.insert(command.index("--no-daemon") + 1, "--offline")
    environment = os.environ.copy()
    java_options = environment.get("JAVA_TOOL_OPTIONS", "")
    if re.search(r"(?<!\S)-Dclava\.astWire=\S+", java_options) or any(
        key in environment for key in ("AST_WIRE_FLAT", "AST_WIRE_DENSE_TEXT")
    ):
        raise SystemExit("remove obsolete AST wire-selection overrides before running the parser workload")
    environment["CCACHE_DISABLE"] = "true"
    environment["XDG_CACHE_HOME"] = str(xdg_root)
    environment.update({"TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root)})
    java_options = java_options.strip()
    environment["JAVA_TOOL_OPTIONS"] = (java_options + " " if java_options else "") + f"-Djava.io.tmpdir={temp_root}"
    (output / "command.json").write_text(json.dumps(command, indent=2) + "\n")
    before = host_state()
    started = time.perf_counter()
    with (output / "gradle.log").open("w") as log:
        process = subprocess.run(command, cwd=CLAVA_ROOT, env=environment, stdout=log,
                                 stderr=subprocess.STDOUT, check=False)
    wall_s = time.perf_counter() - started
    counts = parse_junit(test_results) if test_results.is_dir() else {
        "total_tests": 0, "passed_tests": 0, "failed_tests": 0, "skipped_tests": 0,
    }
    valid_counts = counts == EXPECTED
    release_tag_path = CLAVA_ROOT / "ClangAstParser" / "clang-dumper-release.tag"
    release_tag = release_tag_path.read_text().strip() if release_tag_path.is_file() else None
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "workload": "historical matched parser-only ClangAstParser workload",
        "expected_counts": EXPECTED,
        "counts": counts,
        "counts_valid": valid_counts,
        "clava_git": git_state(),
        "release_tag": release_tag,
        "release_tag_file_sha256": hashlib.sha256(release_tag_path.read_bytes()).hexdigest()
            if release_tag_path.is_file() else None,
        "host_state_before": before,
        "host_state_after": host_state(),
        "timing_boundary": "GNU time and wall timer cover the Gradle test invocation only",
        "wall_s": wall_s,
        "time_metrics": parse_time(output / "time.txt"),
        "environment": {key: environment.get(key) for key in (
            "CCACHE_DISABLE", "XDG_CACHE_HOME", "TMPDIR", "JAVA_TOOL_OPTIONS"
        )},
        "command": command,
        "return_code": process.returncode,
        "passed": process.returncode == 0 and valid_counts,
    }
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({"output": str(output), **summary}, indent=2))
    return 0 if summary["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
