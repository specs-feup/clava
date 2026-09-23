#!/usr/bin/env python3
"""Collect comparable Clava-JS and Java parser distributions at current heads.

Each selected stage gets one unmeasured warm-up followed by measured runs.
Warm mode reuses one owned ccache root, cold mode clears only that root before
each run, and direct mode sets CCACHE_DISABLE and checks that ccache was not
invoked. The Clava-JS workload skips the four host-dependent OpenMP/CUDA cases.
The Java workload is the established 116-test parser-only selection.
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
import re
import shlex
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
        "dumper": None,
        "native_root": None,
        "dumper_revision": "published v18.1.8_4",
        "wire": "text",
        "cache": False,
    },
    {
        "key": "ccache-text",
        "label": "After ccache",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/Exploration"),
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache"),
        "wire": "text",
        "cache": True,
    },
    {
        "key": "protobuf",
        "label": "Protobuf branch",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf"),
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper"),
        "wire": "protobuf",
        "cache": True,
    },
    {
        "key": "flatbuffers",
        "label": "FlatBuffers branch",
        "root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers"),
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper"),
        "wire": "flat-eager",
        "cache": True,
    },
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--repeat-count", type=int, default=6)
    parser.add_argument("--suite", choices=("all", "clava-js", "java"), default="all")
    parser.add_argument("--mode", choices=("direct", "cold", "warm"), default="warm")
    parser.add_argument(
        "--stages",
        default=",".join(stage["key"] for stage in STAGES),
        help="comma-separated stage keys",
    )
    return parser.parse_args()


def cache_namespace(stage: dict[str, Any]) -> str:
    return "clang-dumper-protobuf-ccache-v1" if stage["key"] == "protobuf" else "clang-dumper-ccache"


def configure_cache_mode(
    stage: dict[str, Any], mode: str, measured: bool, environment: dict[str, str], cache_dir: Path
) -> None:
    environment.pop("CCACHE_DISABLE", None)
    if not stage["cache"]:
        return
    if mode == "direct":
        if cache_dir.exists():
            shutil.rmtree(cache_dir)
        environment["CCACHE_DISABLE"] = "true"
    elif mode == "cold" and cache_dir.exists():
        shutil.rmtree(cache_dir)
    elif mode == "warm" and not measured and cache_dir.exists():
        # The unmeasured invocation seeds the exact stage-owned cache used by
        # all subsequent measured invocations.
        shutil.rmtree(cache_dir)
    elif mode == "warm" and measured:
        if not cache_dir.is_dir():
            raise RuntimeError(f"warm mode has no seeded ccache directory: {cache_dir}")
        zero_ccache_stats(cache_dir)


def zero_ccache_stats(cache_dir: Path) -> None:
    result = subprocess.run(
        ["ccache", "--zero-stats"],
        env={**os.environ, "CCACHE_DIR": str(cache_dir), "LC_ALL": "C"},
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"could not reset ccache statistics for {cache_dir}: {result.stderr.strip()}")


def read_ccache_stats(cache_dir: Path) -> dict[str, int] | None:
    if not cache_dir.is_dir():
        return None
    result = subprocess.run(
        ["ccache", "--show-stats", "--verbose"],
        env={**os.environ, "CCACHE_DIR": str(cache_dir), "LC_ALL": "C"},
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        return {"stats_error": result.returncode}
    output = result.stdout + result.stderr

    def number(label: str) -> int:
        match = re.search(rf"^\s*{label}:\s*([\d,]+)", output, re.MULTILINE)
        return int(match.group(1).replace(",", "")) if match else 0

    calls = re.search(r"^\s*Cacheable calls:\s*([\d,]+)", output, re.MULTILINE)
    return {
        "cacheable_calls": int(calls.group(1).replace(",", "")) if calls else 0,
        "hits": number("Hits"),
        "misses": number("Misses"),
        "uncacheable_calls": number("Uncacheable calls"),
    }


def cache_validation(
    stage: dict[str, Any], mode: str, measured: bool, cache_dir: Path,
    direct_probe_marker: Path | None,
) -> dict[str, Any]:
    if not stage["cache"]:
        return {
            "applicable": False,
            "passed": mode == "direct",
            "reason": "stage predates AST dump ccache; direct reference only",
            "cache_dir": None,
            "cacheable_calls": 0,
            "hits": 0,
            "misses": 0,
            "uncacheable_calls": 0,
        }

    stats = read_ccache_stats(cache_dir) or {}
    calls = int(stats.get("cacheable_calls", 0))
    hits = int(stats.get("hits", 0))
    misses = int(stats.get("misses", 0))
    uncacheable = int(stats.get("uncacheable_calls", 0))
    if mode == "direct":
        invoked = direct_probe_marker is not None and direct_probe_marker.exists()
        passed = not invoked and calls == 0 and "stats_error" not in stats
        reason = "CCACHE_DISABLE set; ccache command wrapper observed no invocation" if passed else "ccache was invoked in direct mode"
    elif mode == "cold":
        passed = calls > 0 and misses > 0 and "stats_error" not in stats
        reason = "fresh cache recorded misses; intra-run hits are allowed" if passed else "expected cold cache misses"
    elif not measured:
        passed = calls > 0 and misses > 0 and "stats_error" not in stats
        reason = "warm-up populated ccache" if passed else "warm-up did not populate ccache"
    else:
        passed = calls > 0 and hits > 0 and "stats_error" not in stats
        reason = "measured run restored at least one cached dump" if passed else "warm run recorded no cache hits"
    return {
        "applicable": True,
        "passed": passed,
        "reason": reason,
        "cache_dir": str(cache_dir),
        "cacheable_calls": calls,
        "hits": hits,
        "misses": misses,
        "uncacheable_calls": uncacheable,
        "stats_error": stats.get("stats_error"),
        "direct_probe_marker": str(direct_probe_marker) if direct_probe_marker is not None else None,
    }


def install_direct_ccache_probe(run_dir: Path, environment: dict[str, str]) -> Path:
    real_ccache = shutil.which("ccache")
    if real_ccache is None:
        raise RuntimeError("ccache is required to validate the direct execution path")
    probe_bin = run_dir / "ccache-probe-bin"
    probe_bin.mkdir()
    marker = run_dir / "ccache-invocations.txt"
    wrapper = probe_bin / "ccache"
    wrapper.write_text(
        "#!/bin/sh\n"
        f"printf '%s\\n' invoked >> {shlex.quote(str(marker))}\n"
        f"exec {shlex.quote(real_ccache)} \"$@\"\n"
    )
    wrapper.chmod(0o755)
    environment["PATH"] = str(probe_bin) + os.pathsep + environment.get("PATH", "")
    return marker


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


def validate_stages(selected_keys: set[str]) -> list[dict[str, Any]]:
    metadata = []
    for raw_stage in STAGES:
        if raw_stage["key"] not in selected_keys:
            continue
        stage = dict(raw_stage)
        clava = stage["root"] / "clava"
        observed = git_value(clava, "rev-parse", "HEAD")
        if observed == "unknown":
            raise SystemExit(f"could not read Clava HEAD for {stage['key']}: {clava}")
        stage["clava_revision"] = observed
        stage["clava_branch"] = git_value(clava, "branch", "--show-current")
        runtime = clava / "Clava-JS" / "java-binaries"
        if not runtime.is_dir():
            raise SystemExit(f"missing Java distribution for {stage['key']}: {runtime}")
        dumper = stage["dumper"]
        if dumper is not None and (not dumper.is_file() or not os.access(dumper, os.X_OK)):
            raise SystemExit(f"missing executable dumper for {stage['key']}: {dumper}")
        native_root = stage["native_root"]
        if native_root is not None:
            native_revision = git_value(native_root, "rev-parse", "HEAD")
            if native_revision == "unknown":
                raise SystemExit(f"could not read native dumper HEAD for {stage['key']}: {native_root}")
            stage["dumper_revision"] = native_revision
            stage["dumper_branch"] = git_value(native_root, "branch", "--show-current")
            stage["dumper_status"] = git_status(native_root)
        stage["clava_status"] = git_status(clava)
        stage["runtime_manifest"] = runtime_manifest(runtime)
        stage["dumper_sha256"] = sha256_file(dumper) if dumper is not None else None
        stage["root"] = str(stage["root"])
        stage["dumper"] = str(dumper) if dumper is not None else None
        stage["native_root"] = str(native_root) if native_root is not None else None
        metadata.append(stage)
    return metadata


def assert_heads_unchanged(stages: list[dict[str, Any]]) -> None:
    for stage in stages:
        clava = Path(stage["root"]) / "clava"
        observed = git_value(clava, "rev-parse", "HEAD")
        if observed != stage["clava_revision"]:
            raise RuntimeError(f"Clava HEAD changed during benchmark for {stage['key']}: {observed}")
        if stage["native_root"] is not None:
            observed = git_value(Path(stage["native_root"]), "rev-parse", "HEAD")
            if observed != stage["dumper_revision"]:
                raise RuntimeError(f"dumper HEAD changed during benchmark for {stage['key']}: {observed}")


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
    direct_probe_marker = (
        install_direct_ccache_probe(run_dir, environment)
        if mode == "direct" and stage["cache"] else None
    )
    configure_cache_mode(stage, mode, measured, environment, ccache_dir)
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(
            command, cwd=clava / "Clava-JS", env=environment,
            stdout=log, stderr=subprocess.STDOUT, check=False,
        )
    driver_elapsed = time.perf_counter() - started
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    counts = js_counts(report)
    cache_result = cache_validation(stage, mode, measured, ccache_dir, direct_probe_marker)
    result = {
        "suite": "clava-js",
        "mode": mode,
        "stage": stage["key"],
        "label": stage["label"],
        "measured": measured,
        "repeat": repeat,
        "return_code": process.returncode,
        "driver_elapsed_s": driver_elapsed,
        "cache_validation": cache_result,
        "cacheable_calls": cache_result["cacheable_calls"],
        "cache_hits": cache_result["hits"],
        "cache_misses": cache_result["misses"],
        "runtime_parser_jar_sha256": sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        **parse_time(time_path),
        **counts,
        "valid": process.returncode == 0 and cache_result["passed"]
        and counts["total_tests"] == 164 and counts["passed_tests"] == 158
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
    if stage["key"] == "flatbuffers":
        environment["FLAT_NATIVE"] = stage["native_root"]
    ccache_dir = (
        temp_root / f"clang_ast_exe_{getpass.getuser()}" / cache_namespace(stage)
    )
    direct_probe_marker = (
        install_direct_ccache_probe(run_dir, environment)
        if mode == "direct" and stage["cache"] else None
    )
    configure_cache_mode(stage, mode, measured, environment, ccache_dir)
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
    cache_result = cache_validation(stage, mode, measured, ccache_dir, direct_probe_marker)
    result = {
        "suite": "java",
        "mode": mode,
        "stage": stage["key"],
        "label": stage["label"],
        "measured": measured,
        "repeat": repeat,
        "return_code": process.returncode,
        "driver_elapsed_s": driver_elapsed,
        "cache_validation": cache_result,
        "cacheable_calls": cache_result["cacheable_calls"],
        "cache_hits": cache_result["hits"],
        "cache_misses": cache_result["misses"],
        **parse_time(time_path),
        **counts,
        "valid": process.returncode == 0 and cache_result["passed"]
        and counts["total_tests"] == 116 and counts["passed_tests"] == 116
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
        "cacheable_calls", "cache_hits", "cache_misses",
    ]
    with path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(results)


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    selected_keys = [key.strip() for key in args.stages.split(",") if key.strip()]
    if len(selected_keys) != len(set(selected_keys)):
        raise SystemExit("--stages contains duplicate keys")
    known_keys = {stage["key"] for stage in STAGES}
    unknown_keys = [key for key in selected_keys if key not in known_keys]
    if unknown_keys:
        raise SystemExit(f"unknown stage keys: {', '.join(unknown_keys)}")
    excluded_stages = []
    if args.mode != "direct" and "before-cache" in selected_keys:
        selected_keys.remove("before-cache")
        excluded_stages.append({
            "key": "before-cache",
            "reason": "this stage predates AST dump ccache and has no cold/warm cache state",
        })
    if not selected_keys:
        raise SystemExit("no stages support this mode; before-cache is direct-only")
    stages_by_key = {stage["key"]: stage for stage in validate_stages(set(selected_keys))}
    stages = [stages_by_key[key] for key in selected_keys]
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    output_root = (args.output_root or DEFAULT_OUTPUT_ROOT / f"comparison-{stamp}-{args.mode}").resolve()
    if output_root.exists():
        raise SystemExit(f"refusing to reuse output root: {output_root}")
    output_root.mkdir(parents=True)
    plan = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "repeat_count": args.repeat_count,
        "suite": args.suite,
        "mode": args.mode,
        "excluded_stages": excluded_stages,
        "cache_mode_validation": {
            "direct": "CCACHE_DISABLE=true plus a ccache wrapper probe and zero cache calls",
            "cold": "cache root removed before each run; misses > 0 required; intra-run hits allowed",
            "warm": "unmeasured run seeds cache; each measured run must have at least one hit",
        },
        "clava_js_filter": JS_TEST_FILTER,
        "java_init_script": str(SCRIPT_ROOT / "java-suite.init.gradle"),
        "flatbuffers_java_native_root": next(
            (stage["native_root"] for stage in stages if stage["key"] == "flatbuffers"), None
        ),
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
    assert_heads_unchanged(stages)
    invalid = [result for result in results if not result["valid"]]
    return 1 if invalid else 0


if __name__ == "__main__":
    raise SystemExit(main())
