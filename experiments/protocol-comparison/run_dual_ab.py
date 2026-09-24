#!/usr/bin/env python3
"""Run a same-revision text/Protobuf A/B with a fidelity-gated preflight.

The preflight builds and stages one Java runtime, compares normalized C and
C++ AST snapshots, and runs a small Java and Clava-JS smoke test in each mode.
Timing runs require that passing preflight manifest and reuse its staged runtime.
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
import shlex
import shutil
import statistics
import subprocess
import sys
import time
from typing import Any

sys.dont_write_bytecode = True

import run_comparison as base


SCRIPT_ROOT = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_ROOT.parents[1]
DEFAULT_NATIVE_TOOL = CLAVA_ROOT.parent / "clang-dumper" / "build" / "tool"
DEFAULT_JS_WORKSPACE = Path(
    "/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clava/Clava-JS"
)
ANALYSIS_ROOT = SCRIPT_ROOT / "analysis"
FIDELITY_TEST = "pt.up.fe.specs.clang.dumper.AstWireFidelitySnapshotTest"
JAVA_SMOKE_TEST = "pt.up.fe.specs.clang.dumper.ClangAstDumperTest"
STAGES = (
    {"key": "ab-text", "label": "Text", "wire": "text"},
    {"key": "ab-protobuf", "label": "Protobuf", "wire": "protobuf"},
)
EXPECTED_COMPRESSED = False
EXPECTED_SUITE_COUNTS = {
    "clava-js": {"total_tests": 164, "passed_tests": 158, "failed_tests": 0, "skipped_tests": 6},
    "java": {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0},
}
EXPECTED_METRIC_EVENTS = {"clava-js": 191, "java": 247}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clava-root", type=Path, default=CLAVA_ROOT)
    parser.add_argument("--native-tool", type=Path, default=DEFAULT_NATIVE_TOOL)
    parser.add_argument("--js-workspace", type=Path, default=DEFAULT_JS_WORKSPACE)
    parser.add_argument("--fixture-c", type=Path,
                        default=CLAVA_ROOT / "ClangAstParser/test-resources/c/struct.c")
    parser.add_argument("--fixture-cxx", type=Path,
                        default=CLAVA_ROOT / "ClangAstParser/test-resources/cxx/classes.cpp")
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--repeat-count", type=int, default=6)
    parser.add_argument("--suite", choices=("all", "clava-js", "java"), default="all")
    parser.add_argument(
        "--dry-run", action="store_true",
        help="write the plan and commands without building or running tests",
    )
    parser.add_argument(
        "--preflight-only", action="store_true",
        help="build the runtime and run fidelity plus smoke checks, without timings",
    )
    parser.add_argument(
        "--preflight-result", type=Path,
        help="passing gate.json directory required before timing runs",
    )
    return parser.parse_args()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def git_output(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args], text=True, capture_output=True, check=False
    )
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def git_dirty_fingerprint(repo: Path) -> dict[str, Any]:
    status = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True, capture_output=True, check=False,
    )
    diff = subprocess.run(
        ["git", "-C", str(repo), "diff", "--binary", "HEAD"],
        capture_output=True, check=False,
    )
    return {
        "status": status.stdout.splitlines(),
        "status_error": status.returncode,
        "diff_sha256": sha256_bytes(diff.stdout),
        "diff_error": diff.returncode,
    }


def directory_hash(root: Path, excluded_names: set[str]) -> dict[str, Any]:
    manifest: dict[str, str] = {}
    for path in sorted(root.rglob("*")):
        relative = path.relative_to(root)
        if any(part in excluded_names for part in relative.parts):
            continue
        if path.is_symlink():
            manifest[relative.as_posix()] = "symlink:" + os.readlink(path)
        elif path.is_file():
            manifest[relative.as_posix()] = base.sha256_file(path)
    canonical = json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode()
    return {
        "file_count": len(manifest),
        "sha256": sha256_bytes(canonical),
    }


def configure_gradle_dependency_roots(js_workspace: Path) -> dict[str, Path]:
    project_root = js_workspace.parents[1]
    defaults = {
        "SPECS_JAVA_LIBS_HOME": project_root / "specs-java-libs",
        "LARA_FRAMEWORK_HOME": project_root / "lara-framework",
    }
    roots: dict[str, Path] = {}
    for variable, default in defaults.items():
        root = Path(os.environ.get(variable, default)).resolve()
        if not root.is_dir():
            raise SystemExit(f"Gradle composite dependency path is missing ({variable}): {root}")
        os.environ[variable] = str(root)
        roots[variable] = root
    return roots


def source_metadata(clava_root: Path, native_tool: Path, js_workspace: Path) -> dict[str, Any]:
    native_root = native_tool.parent.parent
    js_repo = Path(git_output(js_workspace, "rev-parse", "--show-toplevel"))
    js_revision = git_output(js_repo, "rev-parse", "HEAD") if js_repo.is_dir() else "unknown"
    try:
        js_relative = js_workspace.resolve().relative_to(js_repo.resolve()).as_posix()
    except ValueError:
        js_relative = "unknown"
    js_tree = git_output(js_repo, "rev-parse", f"HEAD:{js_relative}") if js_relative != "unknown" else "unknown"
    js_status = subprocess.run(
        ["git", "-C", str(js_repo), "status", "--porcelain=v1", "--untracked-files=all", "--", js_relative],
        text=True, capture_output=True, check=False,
    ) if js_relative != "unknown" else None
    dependency_repos = {
        variable: {
            "root": value,
            "revision": git_output(Path(value), "rev-parse", "HEAD"),
            "dirty": git_dirty_fingerprint(Path(value)),
        }
        for variable, value in (
            ("SPECS_JAVA_LIBS_HOME", os.environ.get("SPECS_JAVA_LIBS_HOME")),
            ("LARA_FRAMEWORK_HOME", os.environ.get("LARA_FRAMEWORK_HOME")),
        ) if value
    }
    return {
        "clava": {
            "root": str(clava_root.resolve()),
            "revision": git_output(clava_root, "rev-parse", "HEAD"),
            "branch": git_output(clava_root, "branch", "--show-current"),
            "dirty": git_dirty_fingerprint(clava_root),
        },
        "native": {
            "repo": str(native_root.resolve()),
            "revision": git_output(native_root, "rev-parse", "HEAD"),
            "branch": git_output(native_root, "branch", "--show-current"),
            "dirty": git_dirty_fingerprint(native_root),
            "tool": str(native_tool.resolve()),
            "tool_sha256": base.sha256_file(native_tool) if native_tool.is_file() else None,
        },
        "clava_js": {
            "workspace": str(js_workspace.resolve()),
            "repo": str(js_repo.resolve()) if js_repo.is_dir() else str(js_repo),
            "revision": js_revision,
            "tree": js_tree,
            "status": js_status.stdout.splitlines() if js_status else [],
            "tree_manifest": directory_hash(js_workspace, {".git", "node_modules", "java-binaries"}),
        },
        "java_build_dependencies": dependency_repos,
    }


def validate_inputs(clava_root: Path, native_tool: Path, js_workspace: Path,
                    dependency_roots: dict[str, Path], require_native: bool) -> None:
    if git_output(clava_root, "rev-parse", "--show-toplevel") != str(clava_root.resolve()):
        raise SystemExit(f"--clava-root must be the scratch Clava Git root: {clava_root}")
    if not (clava_root / "ClangAstParser" / "build.gradle").is_file():
        raise SystemExit(f"missing scratch ClangAstParser project: {clava_root}")
    if require_native and (not native_tool.is_file() or not os.access(native_tool, os.X_OK)):
        raise SystemExit(f"missing executable native tool: {native_tool}")
    if not js_workspace.is_dir() or not (js_workspace / "package.json").is_file():
        raise SystemExit(f"missing Clava-JS workspace: {js_workspace}")
    for path in (
        js_workspace / "code" / "WeaverConfiguration.ts",
        js_workspace / "api" / "LegacyIntegrationTests - C.test.ts",
        js_workspace / "api" / "LegacyIntegrationTests - CXX.test.ts",
    ):
        if not path.is_file():
            raise SystemExit(f"missing Clava-JS source or smoke test: {path}")
    helper = js_workspace.parents[1] / "node_modules" / "@specs-feup" / "lara" / "vitest" / "weaverVitestConfig.ts"
    if not helper.is_file():
        raise SystemExit(f"missing Clava-JS Vitest helper: {helper}")
    required_build_files = {
        "SPECS_JAVA_LIBS_HOME": "jOptions/settings.gradle",
        "LARA_FRAMEWORK_HOME": "LangSpec2/settings.gradle",
    }
    for variable, root in dependency_roots.items():
        if not (root / required_build_files[variable]).is_file():
            raise SystemExit(f"Gradle composite dependency is incomplete ({variable}): {root}")
    if base.git_value(native_tool.parent.parent, "rev-parse", "--show-toplevel") == "unknown":
        raise SystemExit(f"native tool parent is not a Git checkout: {native_tool.parent.parent}")


def timestamped_root(parent: Path, label: str) -> Path:
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    return (parent / f"{label}-{stamp}").resolve()


def output_root_for(args: argparse.Namespace, label: str) -> Path:
    root = args.output_root or SCRIPT_ROOT / "results" / timestamped_root(Path("."), label).name
    root = root.resolve()
    if root.exists():
        raise SystemExit(f"refusing to reuse output root: {root}")
    root.mkdir(parents=True)
    return root


def plan_data(clava_root: Path, native_tool: Path, js_workspace: Path, repeat_count: int,
              suite: str, output_root: Path, fixture_c: Path,
              fixture_cxx: Path) -> dict[str, Any]:
    state = source_metadata(clava_root, native_tool, js_workspace)
    native_root = native_tool.parent.parent
    runtime_source = clava_root / "Clava-JS" / "java-binaries"
    return {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "experiment": "same-revision dual-format direct A/B",
        "output_root": str(output_root),
        "repeat_count": repeat_count,
        "suite": suite,
        "mode": "direct",
        "direct_mode": {
            "ccache_disabled": True,
            "compression_expected": EXPECTED_COMPRESSED,
            "validation": "CCACHE_DISABLE=true, ccache PATH probe, zero ccache calls, and per-parse Java metrics",
            "note": "Direct bypasses ccache; metric output records whether AST dumps were compressed.",
        },
        "selectors": {
            "java_property": "-Dclava.astAbWire=text|protobuf",
            "metrics_property": "-Dclava.astWireMetrics=true",
            "native_format_selection": "injected by the scratch Java parser",
        },
        "rotation": {
            "warmup_order": [stage["key"] for stage in STAGES],
            "measured_order": "rotate ab-text/ab-protobuf on each repeat",
            "repeats": repeat_count,
        },
        "workloads": {
            "java_init_script": str(SCRIPT_ROOT / "java-suite.init.gradle"),
            "clava_js_filter": base.JS_TEST_FILTER,
            "clava_js_workspace": str(js_workspace.resolve()),
            "java_smoke_test": JAVA_SMOKE_TEST,
            "clava_js_smoke_test_names": ["CTest Loop", "CxxTest Statement"],
            "expected_metric_events_reference": EXPECTED_METRIC_EVENTS,
        },
        "fidelity_gate": {
            "test_class": FIDELITY_TEST,
            "fixtures": {
                "c": str(fixture_c.resolve()),
                "cxx": str(fixture_cxx.resolve()),
            },
            "comparator": str((ANALYSIS_ROOT / "ab_fidelity.py").resolve()),
        },
        "runtime": {
            "source": str(runtime_source.resolve()),
            "staged_path": str((output_root / "runtime" / "java-binaries").resolve()),
            "native_root_for_schema": str(native_root.resolve()),
            "same_staged_runtime_for_both_modes": True,
        },
        "sources": state,
        "stages": [
            {**stage, "native_tool": str(native_tool.resolve()), "runtime_source": str(runtime_source.resolve())}
            for stage in STAGES
        ],
        "commands": command_plan(clava_root, native_tool, js_workspace, output_root),
    }


def command_plan(clava_root: Path, native_tool: Path, js_workspace: Path, output_root: Path) -> dict[str, Any]:
    common = ["gradle", "--no-daemon", "--offline", f"-PclangDumperRoot={native_tool.parent.parent}"]
    return {
        "build_runtime": [*common, "-p", str(clava_root / "ClavaWeaver"), "syncClavaJsJavaBinaries"],
        "fidelity_test": [*common, "-p", str(clava_root / "ClangAstParser"), "--rerun-tasks", "test", "--tests", FIDELITY_TEST],
        "java_smoke": [*common, "-p", str(clava_root / "ClangAstParser"), "--rerun-tasks", "test", "--tests", JAVA_SMOKE_TEST],
        "clava_js_workspace": str(js_workspace.resolve()),
        "benchmark_suites": ["clava-js", "java"],
        "ccache_probe": "PATH wrapper records invocation; CCACHE_DISABLE=true must leave it untouched",
        "native_format_switch": "set inside scratch Clava Java code from clava.astAbWire",
        "java_worker_options": ["-Dclava.astAbWire=<wire>", "-Dclava.astWireMetrics=true"],
    }


def save_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, indent=2) + "\n")


def java_environment(stage: dict[str, Any], temp_root: Path, native_tool: Path,
                    run_dir: Path) -> tuple[dict[str, str], Path]:
    temp_root.mkdir(parents=True, exist_ok=True)
    ccache_dir = temp_root / "ccache"
    ccache_dir.mkdir(parents=True, exist_ok=True)
    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(temp_root), "CCACHE_DIR": str(ccache_dir),
        "CCACHE_DISABLE": "true", "CLANG_DUMPER_TOOL": str(native_tool.resolve()),
    })
    existing = shlex.split(environment.get("JAVA_TOOL_OPTIONS", ""))
    retained = [
        item for item in existing
        if not item.startswith(("-Djava.io.tmpdir=", "-Dclava.astAbWire=", "-Dclava.astWireMetrics="))
    ]
    retained.extend((
        f"-Djava.io.tmpdir={temp_root}",
        f"-Dclava.astAbWire={stage['wire']}",
        "-Dclava.astWireMetrics=true",
    ))
    environment["JAVA_TOOL_OPTIONS"] = " ".join(retained)
    marker = base.install_direct_ccache_probe(run_dir, environment)
    return environment, marker


def java_metrics(log_path: Path) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    for line in log_path.read_text(errors="replace").splitlines():
        match = re.search(r"(?:PROTOBUF_METRIC|CLAVA_AST_METRIC)\s+(\{.*\})", line)
        if match:
            try:
                value = json.loads(match.group(1))
            except json.JSONDecodeError:
                continue
            if isinstance(value, dict):
                events.append(value)
    return events


def metric_aggregate(events: list[dict[str, Any]]) -> dict[str, Any]:
    sums: dict[str, float | int] = {}
    fields = (
        "native_ms", "read_ms", "decode_ms", "record_ms", "reference_ms", "ast_ms",
        "cache_restore_ms", "dump_bytes", "encoded_bytes", "frames", "records", "nodes", "files",
    )
    for event in events:
        for field in fields:
            value = event.get(field)
            if isinstance(value, (int, float)):
                sums[field] = sums.get(field, 0) + value
    return {
        **sums,
        "output_bytes": sums.get("encoded_bytes", sums.get("dump_bytes", 0)),
        "compressed_values": sorted({event.get("compressed") for event in events}, key=str),
        "cached_values": sorted({event.get("cached") for event in events}, key=str),
        "ccache_disabled_values": sorted({event.get("ccache_disabled") for event in events}, key=str),
        "formats": sorted({str(event.get("format", "unknown")) for event in events}),
    }


def metric_validation(events: list[dict[str, Any]], stage: dict[str, Any], suite: str,
                      marker: Path, ccache_dir: Path) -> dict[str, Any]:
    stats = base.read_ccache_stats(ccache_dir) or {}
    observed_formats = sorted({str(event.get("format", "unknown")) for event in events})
    compressed_values = sorted({event.get("compressed") for event in events}, key=str)
    disabled_values = sorted({event.get("ccache_disabled") for event in events}, key=str)
    cached_values = sorted({event.get("cached") for event in events}, key=str)
    expected_events = EXPECTED_METRIC_EVENTS.get(suite)
    event_count_reference_match = expected_events is None or len(events) == expected_events
    valid = (
        len(events) > 0
        and observed_formats == [stage["wire"]]
        and compressed_values == [EXPECTED_COMPRESSED]
        and disabled_values == [True]
        and cached_values == [False]
        and not marker.exists()
        and int(stats.get("cacheable_calls", 0)) == 0
        and int(stats.get("hits", 0)) == 0
        and int(stats.get("misses", 0)) == 0
        and "stats_error" not in stats
    )
    return {
        "passed": valid,
        "event_count": len(events),
        "expected_event_count_reference": expected_events,
        "event_count_matches_reference": event_count_reference_match,
        "format_values": observed_formats,
        "compressed_values": compressed_values,
        "ccache_disabled_values": disabled_values,
        "cached_values": cached_values,
        "ccache_probe_invoked": marker.exists(),
        "ccache_stats": stats,
        "reason": "Java worker metrics and direct ccache bypass validated" if valid else "format, transport, or ccache validation failed",
    }


def stage_one_runtime(clava_root: Path, native_tool: Path, output_root: Path) -> tuple[Path, dict[str, Any]]:
    build_dir = output_root / "runtime-build"
    build_dir.mkdir()
    command = [
        "gradle", "--no-daemon", "--offline", f"-PclangDumperRoot={native_tool.parent.parent}",
        "-p", str(clava_root / "ClavaWeaver"), "syncClavaJsJavaBinaries",
    ]
    log_path = build_dir / "run.log"
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=clava_root, stdout=log, stderr=subprocess.STDOUT, check=False)
    if process.returncode != 0:
        raise RuntimeError(f"scratch runtime build failed; see {log_path}")
    source = clava_root / "Clava-JS" / "java-binaries"
    destination = output_root / "runtime" / "java-binaries"
    destination.parent.mkdir(parents=True)
    base.stage_runtime(source, destination, str(native_tool))
    manifest = base.runtime_manifest(destination)
    parser_jar = destination / "lib" / "ClangAstParser.jar"
    if not parser_jar.is_file():
        raise RuntimeError(f"staged runtime has no parser JAR: {parser_jar}")
    manifest["root"] = str(destination.resolve())
    manifest["parser_jar_sha256"] = base.sha256_file(parser_jar)
    manifest["native_tool_sha256"] = base.sha256_file(native_tool)
    manifest["release_tag"] = str(native_tool.resolve().parent) + "\n"
    return destination, manifest


def gradle_test_command(clava_root: Path, native_tool: Path, test_class: str,
                        extra_args: list[str] | None = None) -> list[str]:
    return [
        "gradle", "--no-daemon", "--offline", f"-PclangDumperRoot={native_tool.parent.parent}",
        "-p", str(clava_root / "ClangAstParser"), "--rerun-tasks", "test", "--tests", test_class, *(extra_args or []),
    ]


def run_java_test(stage: dict[str, Any], clava_root: Path, native_tool: Path,
                  output_root: Path, label: str, test_class: str,
                  extra_env: dict[str, str] | None = None) -> dict[str, Any]:
    run_dir = output_root / label / stage["key"]
    run_dir.mkdir(parents=True)
    temp_root = output_root / "temp" / label / stage["key"]
    env, marker = java_environment(stage, temp_root, native_tool, run_dir)
    if extra_env:
        env.update(extra_env)
    result_dir = clava_root / "ClangAstParser" / "build" / "test-results" / "test"
    if result_dir.exists():
        shutil.rmtree(result_dir)
    log_path = run_dir / "run.log"
    time_path = run_dir / "time.txt"
    command = [
        "/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(time_path), "--",
        *gradle_test_command(clava_root, native_tool, test_class),
    ]
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=clava_root, env=env, stdout=log, stderr=subprocess.STDOUT, check=False)
    wall = time.perf_counter() - started
    counts = base.java_counts(result_dir)
    events = java_metrics(log_path)
    metric_result = metric_validation(events, stage, "java", marker, temp_root / "ccache")
    result = {
        "suite": "java", "stage": stage["key"], "wire": stage["wire"], "label": label,
        "return_code": process.returncode, "driver_elapsed_s": wall,
        "ccache_disabled": True, "compressed": EXPECTED_COMPRESSED,
        "metric_validation": metric_result, "metric_event_count": len(events),
        "metrics": metric_aggregate(events), "runtime_parser_jar_sha256": None,
        **base.parse_time(time_path), **counts,
        "valid": process.returncode == 0 and metric_result["passed"] and counts["failed_tests"] == 0,
        "command": command, "run_dir": str(run_dir),
    }
    save_json(run_dir / "summary.json", result)
    return result


def write_js_config(config_path: Path, js_workspace: Path, runtime: Path) -> None:
    helper = js_workspace.parents[1] / "node_modules" / "@specs-feup" / "lara" / "vitest" / "weaverVitestConfig.ts"
    if not helper.is_file():
        raise RuntimeError(f"missing Vitest Weaver configuration helper: {helper}")
    config_path.write_text(
        f'import {{ createWeaverVitestConfig }} from "{helper.as_uri()}";\n'
        f'import {{ weaverConfig }} from "{(js_workspace / "code/WeaverConfiguration.ts").as_uri()}";\n'
        f'export default {{ ...createWeaverVitestConfig({{ ...weaverConfig, jarPath: {json.dumps(str(runtime))} }}), '
        f'root: {json.dumps(str(js_workspace.resolve()))} }};\n'
    )


def run_js_test(stage: dict[str, Any], js_workspace: Path, native_tool: Path,
                runtime: Path, output_root: Path, label: str,
                test_filter: str) -> dict[str, Any]:
    run_dir = output_root / label / stage["key"]
    run_dir.mkdir(parents=True)
    temp_root = output_root / "temp" / label / stage["key"]
    temp_root.mkdir(parents=True)
    config_path = run_dir / "vitest.config.ts"
    write_js_config(config_path, js_workspace, runtime)
    report_path = run_dir / "vitest.json"
    log_path = run_dir / "run.log"
    time_path = run_dir / "time.txt"
    command = [
        "/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(time_path), "--",
        "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
        "--config", str(config_path), "--reporter=json", "--outputFile", str(report_path),
        "-t", test_filter,
    ]
    environment, marker = java_environment(stage, temp_root, native_tool, run_dir)
    environment.update({
        "CLAVA_SUITE_JAR_PATH": str(runtime),
        "XDG_CACHE_HOME": str(temp_root),
    })
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=js_workspace, env=environment, stdout=log, stderr=subprocess.STDOUT, check=False)
    wall = time.perf_counter() - started
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    counts = base.js_counts(report)
    events = java_metrics(log_path)
    metric_result = metric_validation(events, stage, "clava-js", marker, temp_root / "ccache")
    result = {
        "suite": "clava-js", "stage": stage["key"], "wire": stage["wire"], "label": label,
        "return_code": process.returncode, "driver_elapsed_s": wall,
        "ccache_disabled": True, "compressed": EXPECTED_COMPRESSED,
        "metric_validation": metric_result, "metric_event_count": len(events),
        "metrics": metric_aggregate(events),
        "runtime_parser_jar_sha256": base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        **base.parse_time(time_path), **counts,
        "valid": process.returncode == 0 and metric_result["passed"]
        and counts["failed_tests"] == 0 and counts["passed_tests"] > 0,
        "command": command, "run_dir": str(run_dir),
    }
    save_json(run_dir / "summary.json", result)
    return result


def run_fidelity_gate(clava_root: Path, native_tool: Path, js_workspace: Path,
                      fixture_c: Path, fixture_cxx: Path,
                      output_root: Path) -> dict[str, Any]:
    gate_root = output_root / "fidelity"
    gate_root.mkdir()
    snapshot_root = gate_root / "snapshots"
    snapshot_root.mkdir()
    fixture_c = fixture_c.resolve()
    fixture_cxx = fixture_cxx.resolve()
    test_rows = []
    for stage in STAGES:
        mode_snapshot_dir = snapshot_root / stage["wire"]
        mode_snapshot_dir.mkdir()
        env_extra = {
            "CLAVA_AST_AB_WIRE": stage["wire"],
            "CLAVA_AST_AB_FIXTURE_C": str(fixture_c),
            "CLAVA_AST_AB_FIXTURE_CXX": str(fixture_cxx),
            "CLAVA_AST_AB_SNAPSHOT_DIR": str(snapshot_root),
        }
        test_rows.append(run_java_test(
            stage, clava_root, native_tool, gate_root, "snapshot-test", FIDELITY_TEST, env_extra,
        ))
    comparator = ANALYSIS_ROOT / "ab_fidelity.py"
    if not comparator.is_file():
        raise RuntimeError(f"AST fidelity comparator is missing: {comparator}")
    compare_path = gate_root / "comparison.json"
    compare_log = gate_root / "comparator.log"
    compare_command = [
        sys.executable, str(comparator), "--snapshots-dir", str(snapshot_root), "--output", str(compare_path),
    ]
    with compare_log.open("w") as log:
        compare_process = subprocess.run(compare_command, cwd=clava_root, stdout=log, stderr=subprocess.STDOUT, check=False)
    comparison = json.loads(compare_path.read_text()) if compare_path.is_file() else {}
    comparison_passed = compare_process.returncode == 0 and comparison.get("passed") is True
    snapshot_count_mismatches = mark_gate_pair_count_mismatches(test_rows)
    fidelity_passed = all(row["valid"] for row in test_rows) and comparison_passed and not snapshot_count_mismatches
    if not fidelity_passed:
        runtime = output_root / "runtime" / "java-binaries"
        result = {
            "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
            "passed": False,
            "fidelity_passed": False,
            "comparison": comparison,
            "comparison_return_code": compare_process.returncode,
            "comparison_command": compare_command,
            "snapshot_test_rows": test_rows,
            "metric_pair_count_mismatches": snapshot_count_mismatches,
            "smoke_rows": [],
            "runtime_manifest": {**base.runtime_manifest(runtime), "root": str(runtime.resolve())},
            "runtime_parser_jar_sha256": base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
            "native_tool_sha256": base.sha256_file(native_tool),
            "reason": "AST graph fidelity failed; suite smoke tests and timings were skipped",
        }
        save_json(gate_root / "gate.json", result)
        return result
    smoke_rows = []
    for stage in STAGES:
        smoke_rows.append(run_java_test(
            stage, clava_root, native_tool, gate_root, "java-smoke", JAVA_SMOKE_TEST,
        ))
    runtime = output_root / "runtime" / "java-binaries"
    js_workspace = js_workspace.resolve()
    for stage in STAGES:
        smoke_rows.append(run_js_test(
            stage, js_workspace, native_tool, runtime, gate_root,
            "clava-js-smoke", r"^(?:CTest Loop|CxxTest Statement)$",
        ))
    smoke_passed = all(row["valid"] for row in smoke_rows)
    metric_pair_mismatches = mark_gate_pair_count_mismatches(test_rows + smoke_rows)
    if metric_pair_mismatches:
        smoke_passed = False
    result = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "passed": fidelity_passed and smoke_passed,
        "fidelity_passed": fidelity_passed,
        "comparison": comparison,
        "comparison_return_code": compare_process.returncode,
        "comparison_command": compare_command,
        "snapshot_test_rows": test_rows,
        "smoke_rows": smoke_rows,
        "metric_pair_count_mismatches": metric_pair_mismatches,
        "runtime_manifest": {
            **base.runtime_manifest(runtime),
            "root": str(runtime.resolve()),
        },
        "runtime_parser_jar_sha256": base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        "native_tool_sha256": base.sha256_file(native_tool),
        "reason": "AST graphs and both smoke suites passed" if fidelity_passed and smoke_passed else "fidelity or suite smoke check failed",
    }
    save_json(gate_root / "gate.json", result)
    return result


def mark_gate_pair_count_mismatches(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        groups.setdefault(row["label"], []).append(row)
    mismatches = []
    expected_stages = {stage["key"] for stage in STAGES}
    for label, group in groups.items():
        stage_rows = {row["stage"]: row for row in group}
        matched = set(stage_rows) == expected_stages and len(
            {row["metric_event_count"] for row in group}
        ) == 1
        if not matched:
            mismatches.append({
                "gate_phase": label,
                "event_counts": {row["stage"]: row["metric_event_count"] for row in group},
            })
            for row in group:
                row["valid"] = False
                row["metric_validation"]["matching_pair_event_count"] = False
        else:
            for row in group:
                row["metric_validation"]["matching_pair_event_count"] = True
    return mismatches


def validate_preflight(preflight_root: Path, current_sources: dict[str, Any], native_tool: Path) -> tuple[Path, dict[str, Any]]:
    gate_path = preflight_root / "fidelity" / "gate.json"
    if not gate_path.is_file():
        raise SystemExit(f"preflight gate manifest does not exist: {gate_path}")
    gate = json.loads(gate_path.read_text())
    if gate.get("passed") is not True:
        raise SystemExit(f"preflight gate did not pass: {gate_path}")
    preflight_plan_path = preflight_root / "plan.json"
    preflight_plan = json.loads(preflight_plan_path.read_text())
    sources = preflight_plan["sources"]
    for key in ("clava", "native", "clava_js"):
        if current_sources.get(key) != sources.get(key):
            raise SystemExit(f"source identity changed since preflight for {key}; rerun --preflight-only")
    if base.sha256_file(native_tool) != gate.get("native_tool_sha256"):
        raise SystemExit("native executable changed since preflight; rerun --preflight-only")
    runtime = Path(gate.get("runtime_manifest", {}).get("root", ""))
    if not runtime.is_dir():
        raise SystemExit(f"preflight Java runtime is missing: {runtime}")
    parser_jar_hash = base.sha256_file(runtime / "lib" / "ClangAstParser.jar")
    if parser_jar_hash != gate.get("runtime_parser_jar_sha256"):
        raise SystemExit("staged parser JAR changed since preflight; rerun --preflight-only")
    return runtime, gate


def run_timing(stage: dict[str, Any], suite: str, clava_root: Path, native_tool: Path,
               js_workspace: Path, runtime: Path, output_root: Path,
               ordinal: int, measured: bool, repeat: int | None) -> dict[str, Any]:
    label = f"{'measured' if measured else 'warmup'}-{repeat if repeat is not None else 'seed'}"
    if suite == "java":
        run_dir = output_root / "timing" / suite / f"{ordinal:02d}-{stage['key']}"
        run_dir.mkdir(parents=True)
        temp_root = output_root / "temp" / suite / f"{ordinal:02d}-{stage['key']}"
        env, marker = java_environment(stage, temp_root, native_tool, run_dir)
        result_dir = clava_root / "ClangAstParser" / "build" / "test-results" / "test"
        if result_dir.exists():
            shutil.rmtree(result_dir)
        time_path = run_dir / "time.txt"
        log_path = run_dir / "run.log"
        command = [
            "/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(time_path), "--",
            "gradle", "--no-daemon", "--offline", f"-PclangDumperRoot={native_tool.parent.parent}",
            "-p", str(clava_root / "ClangAstParser"), "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
            "test",
        ]
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=clava_root, env=env, stdout=log, stderr=subprocess.STDOUT, check=False)
        elapsed = time.perf_counter() - started
        counts = base.java_counts(result_dir)
        events = java_metrics(log_path)
        validation = metric_validation(events, stage, "java", marker, temp_root / "ccache")
        expected_counts = EXPECTED_SUITE_COUNTS["java"]
        counts_match = all(counts.get(key) == value for key, value in expected_counts.items())
        valid = process.returncode == 0 and validation["passed"] and counts_match
        result = {
            "suite": suite, "stage": stage["key"], "wire": stage["wire"], "mode": "direct",
            "measured": measured, "repeat": repeat, "return_code": process.returncode,
            "driver_elapsed_s": elapsed, "ccache_disabled": True, "compressed": EXPECTED_COMPRESSED,
            "metric_validation": validation, "metric_event_count": len(events), "metrics": metric_aggregate(events),
            "runtime_parser_jar_sha256": base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
            **base.parse_time(time_path), **counts, "valid": valid,
            "command": command, "run_dir": str(run_dir),
        }
    else:
        run_dir = output_root / "timing" / suite / f"{ordinal:02d}-{stage['key']}"
        run_dir.mkdir(parents=True)
        temp_root = output_root / "temp" / suite / f"{ordinal:02d}-{stage['key']}"
        temp_root.mkdir(parents=True)
        config_path = run_dir / "vitest.config.ts"
        write_js_config(config_path, js_workspace, runtime)
        report_path, log_path, time_path = run_dir / "vitest.json", run_dir / "run.log", run_dir / "time.txt"
        command = [
            "/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(time_path), "--",
            "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
            "--config", str(config_path), "--reporter=json", "--outputFile", str(report_path),
            "-t", base.JS_TEST_FILTER,
        ]
        env, marker = java_environment(stage, temp_root, native_tool, run_dir)
        env.update({"CLAVA_SUITE_JAR_PATH": str(runtime), "XDG_CACHE_HOME": str(temp_root)})
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=js_workspace, env=env, stdout=log, stderr=subprocess.STDOUT, check=False)
        elapsed = time.perf_counter() - started
        report = json.loads(report_path.read_text()) if report_path.is_file() else {}
        counts = base.js_counts(report)
        events = java_metrics(log_path)
        validation = metric_validation(events, stage, "clava-js", marker, temp_root / "ccache")
        expected_counts = EXPECTED_SUITE_COUNTS["clava-js"]
        counts_match = all(counts.get(key) == value for key, value in expected_counts.items())
        valid = process.returncode == 0 and validation["passed"] and counts_match
        result = {
            "suite": suite, "stage": stage["key"], "wire": stage["wire"], "mode": "direct",
            "measured": measured, "repeat": repeat, "return_code": process.returncode,
            "driver_elapsed_s": elapsed, "ccache_disabled": True, "compressed": EXPECTED_COMPRESSED,
            "metric_validation": validation, "metric_event_count": len(events), "metrics": metric_aggregate(events),
            "runtime_parser_jar_sha256": base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
            **base.parse_time(time_path), **counts, "valid": valid,
            "command": command, "run_dir": str(run_dir),
        }
    result.update({"phase": label, "stage_label": stage["label"]})
    save_json(Path(result["run_dir"]) / "summary.json", result)
    return result


def mark_pair_count_mismatches(results: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_pair: dict[tuple[str, bool, int | None], dict[str, dict[str, Any]]] = {}
    for row in results:
        key = (row["suite"], row["measured"], row["repeat"])
        by_pair.setdefault(key, {})[row["stage"]] = row
    mismatches = []
    for key, stages in by_pair.items():
        if set(stages) != {stage["key"] for stage in STAGES}:
            mismatches.append({"pair": key, "reason": "missing stage"})
            for row in stages.values():
                row["valid"] = False
                row["metric_validation"]["matching_pair_event_count"] = False
            continue
        counts = {stage: row["metric_event_count"] for stage, row in stages.items()}
        matched = len(set(counts.values())) == 1
        if not matched:
            mismatches.append({"pair": key, "event_counts": counts})
        for row in stages.values():
            row["metric_validation"]["matching_pair_event_count"] = matched
            if not matched:
                row["valid"] = False
    return mismatches


def summarize(results: list[dict[str, Any]], mismatches: list[dict[str, Any]]) -> dict[str, Any]:
    suites: dict[str, Any] = {}
    for suite in ("clava-js", "java"):
        rows = [row for row in results if row["suite"] == suite and row["measured"]]
        by_stage = {
            stage["key"]: sorted(
                [row for row in rows if row["stage"] == stage["key"]],
                key=lambda row: row["repeat"],
            )
            for stage in STAGES
        }
        elapsed_medians = {
            key: statistics.median(float(row.get("elapsed_s", row["driver_elapsed_s"])) for row in stage_rows)
            if stage_rows else None
            for key, stage_rows in by_stage.items()
        }
        suites[suite] = {
            "measured_runs": {key: len(stage_rows) for key, stage_rows in by_stage.items()},
            "median_elapsed_s": elapsed_medians,
            "metric_event_counts": {key: [row["metric_event_count"] for row in stage_rows] for key, stage_rows in by_stage.items()},
            "median_phase_totals": {
                key: {
                    field: statistics.median(float(row["metrics"].get(field, 0)) for row in stage_rows)
                    if stage_rows else None
                    for field in ("native_ms", "read_ms", "decode_ms", "record_ms", "reference_ms", "ast_ms", "output_bytes")
                }
                for key, stage_rows in by_stage.items()
            },
        }
    return {"suites": suites, "metric_count_mismatches": mismatches}


def write_results(root: Path, plan: dict[str, Any], results: list[dict[str, Any]],
                  mismatches: list[dict[str, Any]], gate: dict[str, Any]) -> None:
    summary = summarize(results, mismatches)
    combined = {
        **plan,
        "fidelity_gate": gate,
        "results": results,
        "summary": summary,
        "valid": all(row["valid"] for row in results) and not mismatches,
    }
    save_json(root / "results.json", combined)
    fields = [
        "suite", "stage", "wire", "measured", "repeat", "valid", "elapsed_s", "driver_elapsed_s",
        "user_s", "sys_s", "max_rss_kb", "metric_event_count", "total_tests", "passed_tests",
        "failed_tests", "skipped_tests", "return_code", "ccache_disabled", "compressed",
    ]
    with (root / "observations.csv").open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(results)


def main() -> int:
    args = parse_args()
    if args.repeat_count < 1:
        raise SystemExit("--repeat-count must be positive")
    if args.preflight_only and args.preflight_result:
        raise SystemExit("use --preflight-only or --preflight-result, not both")
    if not args.dry_run and not args.preflight_only and args.preflight_result is None:
        raise SystemExit("timings require --preflight-result pointing to a passing preflight directory")
    clava_root = args.clava_root.resolve()
    native_tool = args.native_tool.resolve()
    js_workspace = args.js_workspace.resolve()
    dependency_roots = configure_gradle_dependency_roots(js_workspace)
    fixture_c = args.fixture_c.resolve()
    fixture_cxx = args.fixture_cxx.resolve()
    validate_inputs(clava_root, native_tool, js_workspace, dependency_roots,
                    require_native=not args.dry_run)
    if not args.dry_run:
        for fixture in (fixture_c, fixture_cxx):
            if not fixture.is_file():
                raise SystemExit(f"fidelity fixture does not exist: {fixture}")
    label = "dual-ab-preflight" if args.preflight_only else "dual-ab"
    output_root = output_root_for(args, label)
    plan = plan_data(clava_root, native_tool, js_workspace, args.repeat_count, args.suite,
                     output_root, fixture_c, fixture_cxx)
    (output_root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
    if args.dry_run:
        print(json.dumps({
            "dry_run": True,
            "native_available": native_tool.is_file() and os.access(native_tool, os.X_OK),
            "runtime_source_available": (clava_root / "Clava-JS" / "java-binaries").is_dir(),
            "fidelity_comparator_available": (ANALYSIS_ROOT / "ab_fidelity.py").is_file(),
            "plan": str(output_root / "plan.json"),
        }, indent=2))
        return 0
    if not native_tool.is_file() or not os.access(native_tool, os.X_OK):
        raise SystemExit(f"native tool is not ready: {native_tool}")
    if args.preflight_only:
        runtime, runtime_manifest = stage_one_runtime(clava_root, native_tool, output_root)
        plan["runtime_manifest"] = runtime_manifest
        plan["runtime_parser_jar_sha256"] = runtime_manifest["parser_jar_sha256"]
        plan["sources"] = source_metadata(clava_root, native_tool, js_workspace)
        save_json(output_root / "plan.json", plan)
        gate = run_fidelity_gate(clava_root, native_tool, js_workspace,
                                 fixture_c, fixture_cxx, output_root)
        gate["staged_runtime"] = str(runtime)
        save_json(output_root / "fidelity" / "gate.json", gate)
        print(json.dumps({"preflight_passed": gate["passed"], "gate": str(output_root / "fidelity/gate.json")}, flush=True))
        return 0 if gate["passed"] else 1

    current_sources = source_metadata(clava_root, native_tool, js_workspace)
    runtime, gate = validate_preflight(args.preflight_result.resolve(), current_sources, native_tool)
    plan["preflight_root"] = str(args.preflight_result.resolve())
    plan["preflight_gate"] = gate
    plan["runtime_manifest"] = base.runtime_manifest(runtime)
    plan["runtime_parser_jar_sha256"] = base.sha256_file(runtime / "lib" / "ClangAstParser.jar")
    plan["sources"] = current_sources
    save_json(output_root / "plan.json", plan)
    results: list[dict[str, Any]] = []
    suites = ("clava-js", "java") if args.suite == "all" else (args.suite,)
    for suite in suites:
        ordinal = 0
        for stage in STAGES:
            ordinal += 1
            row = run_timing(stage, suite, clava_root, native_tool, js_workspace, runtime,
                             output_root, ordinal, False, None)
            results.append(row)
            print(json.dumps(row, sort_keys=True), flush=True)
        for repeat in range(1, args.repeat_count + 1):
            rotation = (repeat - 1) % len(STAGES)
            order = STAGES[rotation:] + STAGES[:rotation]
            for stage in order:
                ordinal += 1
                row = run_timing(stage, suite, clava_root, native_tool, js_workspace, runtime,
                                 output_root, ordinal, True, repeat)
                results.append(row)
                print(json.dumps(row, sort_keys=True), flush=True)
            mismatches = mark_pair_count_mismatches([row for row in results if row["suite"] == suite])
            write_results(output_root, plan, results, mismatches, gate)
    mismatches = mark_pair_count_mismatches(results)
    if source_metadata(clava_root, native_tool, js_workspace) != current_sources:
        for row in results:
            row["valid"] = False
        mismatches.append({"reason": "source or native tool identity changed during timing"})
    write_results(output_root, plan, results, mismatches, gate)
    return 0 if all(row["valid"] for row in results) and not mismatches else 1


if __name__ == "__main__":
    raise SystemExit(main())
