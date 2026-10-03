#!/usr/bin/env python3
"""Run source round-trip and cross-TU checks through the built Clava parser."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = ROOT.parents[1]
FIXTURES = ROOT / "fixtures"
VALIDATION_FIXTURES = Path(__file__).resolve().parent / "fixtures"
PROBE_SOURCE = Path(__file__).resolve().parent / "java" / "ValidationProbe.java"
RESULTS_ROOT = ROOT / "results" / "validation"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--runtime-root",
        type=Path,
        default=CLAVA_ROOT / "ClavaWeaver" / "build" / "install" / "ClavaWeaver",
        help="Built ClavaWeaver distribution with the production parser JAR.",
    )
    parser.add_argument("--output-root", type=Path)
    parser.add_argument(
        "--case", choices=("all", "roundtrip", "cross-tu"), default="all"
    )
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def host_state() -> dict[str, Any]:
    state: dict[str, Any] = {}
    try:
        state["loadavg"] = Path("/proc/loadavg").read_text().strip()
    except OSError:
        pass
    try:
        state["meminfo"] = {
            key: int(value.split()[0])
            for line in Path("/proc/meminfo").read_text().splitlines()
            for key, _, value in [line.partition(":")]
            if key in {"MemTotal", "MemAvailable", "SwapTotal", "SwapFree"}
        }
    except OSError:
        pass
    return state


def git_state(repo: Path) -> dict[str, Any]:
    head = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"],
        text=True,
        capture_output=True,
        check=False,
    )
    status = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True,
        capture_output=True,
        check=False,
    )
    return {
        "head": head.stdout.strip() if head.returncode == 0 else "unknown",
        "dirty": status.returncode != 0 or bool(status.stdout.strip()),
        "status": status.stdout.splitlines(),
    }


def compile_probe(runtime: Path, classes: Path) -> str:
    lib = runtime / "lib"
    parser_jar = lib / "ClangAstParser.jar"
    if not parser_jar.is_file():
        raise SystemExit(f"missing ClangAstParser.jar in runtime: {runtime}")
    classes.mkdir(parents=True)
    classpath = f"{classes}:{lib}/*"
    command = ["javac", "-cp", f"{lib}/*", "-d", str(classes), str(PROBE_SOURCE)]
    subprocess.run(command, cwd=CLAVA_ROOT, check=True)
    return classpath


def run_case(
    name: str,
    command: list[str],
    work: Path,
    env: dict[str, str],
) -> dict[str, Any]:
    log = work / f"{name}.log"
    started = subprocess.run(
        command,
        cwd=CLAVA_ROOT,
        env=env,
        text=True,
        capture_output=True,
        check=False,
    )
    output = started.stdout + started.stderr
    log.write_text(output)
    rows = []
    for line in output.splitlines():
        match = re.match(r"(?:CLAVA_VALIDATION) (\{.*\})$", line)
        if match:
            try:
                rows.append(json.loads(match.group(1)))
            except json.JSONDecodeError:
                pass
    return {
        "case": name,
        "return_code": started.returncode,
        "command": command,
        "log": str(log),
        "results": rows,
        "passed": started.returncode == 0 and len(rows) == 1,
    }


def main() -> int:
    args = parse_args()
    runtime = args.runtime_root.resolve()
    if not runtime.is_dir():
        raise SystemExit(f"ClavaWeaver runtime does not exist: {runtime}")
    output = args.output_root.resolve() if args.output_root else RESULTS_ROOT / dt.datetime.now(
        dt.timezone.utc
    ).strftime("correctness-%Y%m%dT%H%M%SZ")
    output.mkdir(parents=True, exist_ok=False)
    temp_root = output / "tmp"
    temp_root.mkdir()
    classes = output / "classes"
    classpath = compile_probe(runtime, classes)

    environment = os.environ.copy()
    java_options = environment.get("JAVA_TOOL_OPTIONS", "")
    if re.search(r"(?<!\S)-Dclava\.astWire=\S+", java_options) or any(
        key in environment for key in ("AST_WIRE_FLAT", "AST_WIRE_DENSE_TEXT")
    ):
        raise SystemExit("remove obsolete AST wire-selection overrides before running eager checks")
    environment["CCACHE_DISABLE"] = "true"
    environment["XDG_CACHE_HOME"] = str(output / "xdg-cache")
    environment["TMPDIR"] = str(temp_root)
    environment["TMP"] = str(temp_root)
    environment["TEMP"] = str(temp_root)

    cases: list[tuple[str, Path, str]] = []
    if args.case in {"all", "roundtrip"}:
        cases.extend([
            ("roundtrip-c", FIXTURES / "fidelity.c", "c11"),
            ("roundtrip-cpp", FIXTURES / "fidelity.cpp", "c++17"),
        ])
    if args.case in {"all", "cross-tu"}:
        cases.append(("cross-tu", VALIDATION_FIXTURES / "cross-tu", "c++17"))

    outcomes = []
    for name, fixture, standard in cases:
        work = output / name
        work.mkdir()
        if name == "cross-tu":
            mode = "cross-tu"
            source = fixture
            arguments = [mode, str(source), str(work)]
        else:
            mode = "roundtrip"
            arguments = [mode, str(fixture), str(work), standard]
        command = [
            "java", "-Xms128m", "-Xmx4g",
            f"-Djava.io.tmpdir={temp_root}",
            "-cp", classpath, "ValidationProbe", *arguments,
        ]
        outcomes.append(run_case(name, command, work, environment))

    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "runtime_root": str(runtime),
        "runtime_parser_jar_sha256": sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        "clava_git": git_state(CLAVA_ROOT),
        "host_state": host_state(),
        "java_tool_options": environment.get("JAVA_TOOL_OPTIONS", ""),
        "cache_policy": "CCACHE_DISABLE=true; AST_DUMP_CACHE=false in ValidationProbe",
        "cases": outcomes,
        "passed": all(case["passed"] for case in outcomes),
    }
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({"output": str(output), "passed": summary["passed"], "cases": outcomes}, indent=2))
    return 0 if summary["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
