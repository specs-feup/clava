#!/usr/bin/env python3
"""Collect comparable Clava-JS and Java parser distributions at four revisions.

Each stage gets one uncharted resource warm-up followed by six measured runs.
Warm mode reuses one owned ccache root, cold mode clears only that root before
each run, and bypass mode sets CCACHE_DISABLE. The Clava-JS workload skips the
four host-dependent OpenMP/CUDA cases. The Java workload is the established
116-test parser-only selection.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import getpass
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
from typing import Any, Iterable
import xml.etree.ElementTree as ET
import zipfile


SCRIPT_ROOT = Path(__file__).resolve().parent
DEFAULT_OUTPUT_ROOT = SCRIPT_ROOT / "results"
TIME_FORMAT = "elapsed_s=%e\nuser_s=%U\nsys_s=%S\nmax_rss_kb=%M\nexit_status=%x"
JS_TEST_FILTER = (
    r"^(?!(?:CxxTest OmpThreadsExplore|CudaTest Cuda|CudaTest CudaMatrixMul|"
    r"CudaTest CudaQuery)$).*$"
)


STAGES = (
    {
        "key": "before-cache",
        "label": "Before cache",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/clava-optimizations"),
        "clava_revision": "70c30bdb8cce66dba53c0e2963c3ec31cbc223bc",
        "dumper": None,
        "dumper_revision": "published v18.1.8_4",
        "wire": "text",
        "cache": False,
    },
    {
        "key": "ccache-text",
        "label": "After ccache",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/Exploration"),
        "clava_revision": "603997af2fb1a7f9cdd1b418b15772f892494aca",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache/build/tool"),
        "dumper_revision": "bc498f5cedb88239062eef21a9669bc9bddb0ff7",
        "wire": "text",
        "cache": True,
    },
    {
        "key": "protobuf",
        "label": "Protobuf branch",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf"),
        "clava_revision": "c31df4c057a92d0c0cc83bb19b39ead010a32b8a",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper/build/tool"),
        "dumper_revision": "ab9d0238bd9c27eb9f156fd19967d16025137a37",
        "wire": "protobuf",
        "cache": True,
    },
    {
        "key": "flatbuffers",
        "label": "FlatBuffers branch",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers"),
        "clava_revision": "e94405f2b3dccba98577e5de797ee00b6d4f44c0",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper/build/tool"),
        "dumper_revision": "e69a77f2838581bcfdbe7d025f389d3ed9e85b0b",
        "wire": "flat-eager",
        "cache": True,
    },
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--repeat-count", type=int, default=6)
    parser.add_argument("--suite", choices=("all", "clava-js", "java"), default="all")
    parser.add_argument("--mode", choices=("bypass", "cold", "warm"), default="warm")
    parser.add_argument(
        "--stages",
        default=",".join(stage["key"] for stage in STAGES),
        help="comma-separated stage keys",
    )
    return parser.parse_args()


def cache_namespace(stage: dict[str, Any]) -> str:
    return "clang-dumper-protobuf-ccache-v1" if stage["key"] == "protobuf" else "clang-dumper-ccache"


def configure_cache_mode(
    stage: dict[str, Any], mode: str, environment: dict[str, str], cache_dir: Path
) -> None:
    environment.pop("CCACHE_DISABLE", None)
    if not stage["cache"]:
        return
    if mode == "bypass":
        environment["CCACHE_DISABLE"] = "true"
    elif mode == "cold" and cache_dir.exists():
        shutil.rmtree(cache_dir)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def git_value(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args], text=True, capture_output=True, check=False
    )
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def git_status(repo: Path) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True,
        capture_output=True,
        check=False,
    )
    return result.stdout.splitlines()


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    if not path.is_file():
        return values
    for line in path.read_text().splitlines():
        key, separator, raw = line.partition("=")
        if not separator:
            continue
        values[key] = int(float(raw)) if key in {"max_rss_kb", "exit_status"} else float(raw)
    return values


def iter_assertions(value: Any) -> Iterable[dict[str, Any]]:
    if isinstance(value, dict):
        assertions = value.get("assertionResults")
        if isinstance(assertions, list):
            for assertion in assertions:
                if isinstance(assertion, dict):
                    yield assertion
        for key in ("testResults", "files", "children"):
            yield from iter_assertions(value.get(key, []))
    elif isinstance(value, list):
        for item in value:
            yield from iter_assertions(item)


def js_counts(report: dict[str, Any]) -> dict[str, Any]:
    assertions = list(iter_assertions(report))
    statuses = [str(item.get("status", "unknown")).lower() for item in assertions]
    failures = [
        str(item.get("fullName") or item.get("name") or item.get("title") or "")
        for item, status in zip(assertions, statuses)
        if status not in {"passed", "pass", "skipped", "pending", "todo"}
    ]
    return {
        "total_tests": len(assertions),
        "passed_tests": sum(status in {"passed", "pass"} for status in statuses),
        "failed_tests": len(failures),
        "skipped_tests": sum(status in {"skipped", "pending", "todo"} for status in statuses),
        "failure_names": failures,
    }


def java_counts(result_root: Path) -> dict[str, Any]:
    tests = failures = skipped = 0
    duration = 0.0
    failure_names: list[str] = []
    for path in sorted(result_root.glob("*.xml")):
        suite = ET.parse(path).getroot()
        tests += int(suite.attrib.get("tests", 0))
        failures += int(suite.attrib.get("failures", 0)) + int(suite.attrib.get("errors", 0))
        skipped += int(suite.attrib.get("skipped", 0))
        duration += float(suite.attrib.get("time", 0.0))
        for case in suite.findall("testcase"):
            if case.find("failure") is not None or case.find("error") is not None:
                failure_names.append(f"{case.attrib.get('classname', '')}#{case.attrib.get('name', '')}")
    return {
        "total_tests": tests,
        "passed_tests": tests - failures - skipped,
        "failed_tests": failures,
        "skipped_tests": skipped,
        "junit_aggregate_s": duration,
        "failure_names": failure_names,
    }


def runtime_manifest(root: Path) -> dict[str, Any]:
    jars = {
        str(path.relative_to(root)): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }
    canonical = json.dumps(jars, sort_keys=True, separators=(",", ":")).encode()
    return {"jar_count": len(jars), "sha256": hashlib.sha256(canonical).hexdigest()}


def stage_runtime(source: Path, destination: Path, dumper: str | None) -> None:
    shutil.copytree(source, destination)
    if dumper is None:
        return
    parser_jar = destination / "lib" / "ClangAstParser.jar"
    temporary = parser_jar.with_suffix(".patched.jar")
    replacement = (str(Path(dumper).resolve().parent) + "\n").encode()
    found = False
    with zipfile.ZipFile(parser_jar) as original, zipfile.ZipFile(
        temporary, "w", compression=zipfile.ZIP_DEFLATED
    ) as patched:
        for entry in original.infolist():
            data = original.read(entry)
            if entry.filename == "clang-dumper-release.tag":
                data = replacement
                found = True
            patched.writestr(entry, data)
    if not found:
        temporary.unlink(missing_ok=True)
        raise RuntimeError(f"runtime has no clang-dumper-release.tag: {parser_jar}")
    temporary.replace(parser_jar)


def write_vitest_config(run_dir: Path, clava: Path, runtime: Path) -> Path:
    config = run_dir / "vitest.comparison.config.ts"
    helper = clava.parent / "node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts"
    config.write_text(
        f'import {{ createWeaverVitestConfig }} from "{helper.as_uri()}";\n'
        f'import {{ weaverConfig }} from "{(clava / "Clava-JS/code/WeaverConfiguration.ts").as_uri()}";\n'
        f'export default {{ ...createWeaverVitestConfig({{ ...weaverConfig, jarPath: {json.dumps(str(runtime))} }}), '
        f'root: {json.dumps(str(clava / "Clava-JS"))} }};\n'
    )
    return config


def validate_stages() -> list[dict[str, Any]]:
    metadata = []
    for raw_stage in STAGES:
        stage = dict(raw_stage)
        clava = stage["root"] / "clava"
        observed = git_value(clava, "rev-parse", "HEAD")
        if observed != stage["clava_revision"]:
            raise SystemExit(
                f"{stage['key']} Clava revision is {observed}, expected {stage['clava_revision']}"
            )
        runtime = clava / "Clava-JS" / "java-binaries"
        if not runtime.is_dir():
            raise SystemExit(f"missing Java distribution for {stage['key']}: {runtime}")
        dumper = stage["dumper"]
        if dumper is not None and (not dumper.is_file() or not os.access(dumper, os.X_OK)):
            raise SystemExit(f"missing executable dumper for {stage['key']}: {dumper}")
        stage["clava_status"] = git_status(clava)
        stage["runtime_manifest"] = runtime_manifest(runtime)
        stage["dumper_sha256"] = sha256_file(dumper) if dumper is not None else None
        stage["root"] = str(stage["root"])
        stage["dumper"] = str(dumper) if dumper is not None else None
        metadata.append(stage)
    return metadata


def run_clava_js(stage: dict[str, Any], output_root: Path, ordinal: int, measured: bool,
                 repeat: int | None, mode: str) -> dict[str, Any]:
    root = Path(stage["root"])
    clava = root / "clava"
    run_dir = output_root / "runs" / "clava-js" / f"{ordinal:02d}-{stage['key']}"
    run_dir.mkdir(parents=True)
    temp_root = output_root / "temp" / "clava-js" / stage["key"]
    cache_root = output_root / "cache" / "clava-js" / stage["key"]
    temp_root.mkdir(parents=True, exist_ok=True)
    cache_root.mkdir(parents=True, exist_ok=True)
    report_path = run_dir / "vitest.json"
    log_path = run_dir / "run.log"
    time_path = run_dir / "time.txt"
    runtime = run_dir / "java-binaries"
    stage_runtime(clava / "Clava-JS" / "java-binaries", runtime, stage["dumper"])
    vitest_config = write_vitest_config(run_dir, clava, runtime)
    command = [
        "/usr/bin/time", "-f", TIME_FORMAT, "-o", str(time_path), "--",
        "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
        "--config", str(vitest_config), "--reporter=json", "--outputFile", str(report_path),
        "-t", JS_TEST_FILTER,
    ]
    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root),
        "TMP": str(temp_root),
        "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(cache_root),
        "JAVA_TOOL_OPTIONS": environment.get("JAVA_TOOL_OPTIONS", "")
        + f" -Djava.io.tmpdir={temp_root} -Dclava.astWire={stage['wire']}",
    })
    ccache_dir = cache_root / "@specs-feup" / "clava" / cache_namespace(stage)
    configure_cache_mode(stage, mode, environment, ccache_dir)
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(
            command, cwd=clava / "Clava-JS", env=environment,
            stdout=log, stderr=subprocess.STDOUT, check=False,
        )
    driver_elapsed = time.perf_counter() - started
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    counts = js_counts(report)
    result = {
        "suite": "clava-js",
        "mode": mode,
        "stage": stage["key"],
        "label": stage["label"],
        "measured": measured,
        "repeat": repeat,
        "return_code": process.returncode,
        "driver_elapsed_s": driver_elapsed,
        **parse_time(time_path),
        **counts,
        "valid": counts["total_tests"] == 164 and counts["passed_tests"] == 158
        and counts["failed_tests"] == 0 and counts["skipped_tests"] == 6,
        "command": command,
        "run_dir": str(run_dir),
    }
    (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def run_java(stage: dict[str, Any], output_root: Path, ordinal: int, measured: bool,
             repeat: int | None, mode: str) -> dict[str, Any]:
    root = Path(stage["root"])
    clava = root / "clava"
    run_dir = output_root / "runs" / "java" / f"{ordinal:02d}-{stage['key']}"
    run_dir.mkdir(parents=True)
    temp_root = output_root / "temp" / "java" / stage["key"]
    cache_root = output_root / "cache" / "java" / stage["key"]
    temp_root.mkdir(parents=True, exist_ok=True)
    cache_root.mkdir(parents=True, exist_ok=True)
    log_path = run_dir / "run.log"
    time_path = run_dir / "time.txt"
    command = [
        "/usr/bin/time", "-f", TIME_FORMAT, "-o", str(time_path), "--",
        "gradle", "--no-daemon", "--offline", "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
        "-p", "ClangAstParser", "test",
    ]
    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root),
        "TMP": str(temp_root),
        "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(cache_root),
        "JAVA_TOOL_OPTIONS": environment.get("JAVA_TOOL_OPTIONS", "")
        + f" -Djava.io.tmpdir={temp_root} -Dclava.astWire={stage['wire']}",
    })
    ccache_dir = (
        temp_root / f"clang_ast_exe_{getpass.getuser()}" / cache_namespace(stage)
    )
    configure_cache_mode(stage, mode, environment, ccache_dir)
    test_results = clava / "ClangAstParser" / "build" / "test-results" / "test"
    if test_results.exists():
        shutil.rmtree(test_results)
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(
            command, cwd=clava, env=environment,
            stdout=log, stderr=subprocess.STDOUT, check=False,
        )
    driver_elapsed = time.perf_counter() - started
    counts = java_counts(test_results)
    result = {
        "suite": "java",
        "mode": mode,
        "stage": stage["key"],
        "label": stage["label"],
        "measured": measured,
        "repeat": repeat,
        "return_code": process.returncode,
        "driver_elapsed_s": driver_elapsed,
        **parse_time(time_path),
        **counts,
        "valid": counts["total_tests"] == 116 and counts["passed_tests"] == 116
        and counts["failed_tests"] == 0 and counts["skipped_tests"] == 0,
        "command": command,
        "run_dir": str(run_dir),
    }
    (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def write_csv(results: list[dict[str, Any]], path: Path) -> None:
    fields = [
        "suite", "mode", "stage", "label", "measured", "repeat", "valid", "elapsed_s", "user_s", "sys_s",
        "max_rss_kb", "total_tests", "passed_tests", "failed_tests", "skipped_tests", "return_code",
    ]
    with path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(results)


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    output_root = (args.output_root or DEFAULT_OUTPUT_ROOT / f"comparison-{stamp}").resolve()
    if output_root.exists():
        raise SystemExit(f"refusing to reuse output root: {output_root}")
    output_root.mkdir(parents=True)
    stages_by_key = {stage["key"]: stage for stage in validate_stages()}
    selected_keys = [key.strip() for key in args.stages.split(",") if key.strip()]
    unknown_keys = [key for key in selected_keys if key not in stages_by_key]
    if unknown_keys:
        raise SystemExit(f"unknown stage keys: {', '.join(unknown_keys)}")
    stages = [stages_by_key[key] for key in selected_keys]
    if not stages:
        raise SystemExit("--stages must select at least one stage")
    plan = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "repeat_count": args.repeat_count,
        "suite": args.suite,
        "mode": args.mode,
        "clava_js_filter": JS_TEST_FILTER,
        "java_init_script": str(SCRIPT_ROOT / "java-suite.init.gradle"),
        "stages": stages,
    }
    (output_root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
    results: list[dict[str, Any]] = []
    suites = ("clava-js", "java") if args.suite == "all" else (args.suite,)
    runners = {"clava-js": run_clava_js, "java": run_java}
    for suite in suites:
        ordinal = 0
        for stage in stages:
            ordinal += 1
            result = runners[suite](stage, output_root, ordinal, False, None, args.mode)
            results.append(result)
            print(json.dumps(result, sort_keys=True), flush=True)
        for repeat in range(1, args.repeat_count + 1):
            rotation = (repeat - 1) % len(stages)
            order = stages[rotation:] + stages[:rotation]
            for stage in order:
                ordinal += 1
                result = runners[suite](stage, output_root, ordinal, True, repeat, args.mode)
                results.append(result)
                print(json.dumps(result, sort_keys=True), flush=True)
        write_csv(results, output_root / "observations.csv")
        (output_root / "results.json").write_text(json.dumps({**plan, "results": results}, indent=2) + "\n")
    invalid = [result for result in results if not result["valid"]]
    return 1 if invalid else 0


if __name__ == "__main__":
    raise SystemExit(main())
