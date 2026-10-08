#!/usr/bin/env python3
"""Collect, render, and privately update the Protobuf benchmark report."""

from __future__ import annotations

import argparse
import datetime as dt
import fcntl
import hashlib
import importlib.util
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid
from typing import Any, Callable

# Loading the report renderer imports source modules from the canonical checkout.
# Keep that read-only preparation step from creating untracked __pycache__ files.
sys.dont_write_bytecode = True

import support


REFRESH_DIR = Path(__file__).resolve().parent
PROTOBUF_ROOT = REFRESH_DIR.parents[1]
REPORT_SCRIPT = PROTOBUF_ROOT / "report/render_updated_benchmark.py"
FROZEN_CONTROLS = PROTOBUF_ROOT / "validation/evidence/benchmark-20261008/frozen-controls-20261007.json"
DEFAULT_OUTPUT_BASE = Path.home() / ".cache/protobuf-production-validation/benchmark-refreshes"
DEFAULT_LOCK = Path.home() / ".cache/protobuf-production-validation/protobuf-benchmark-refresh.lock"
EXPECTED_WORKLOAD = {
    "java_pinned_test_ids": 116,
    "js_tests": 164,
    "js_passed": 158,
    "js_skipped": 6,
    "java_app_calls": 216,
    "js_app_calls": 170,
    "js_syntax_only_calls": 130,
    "repeats": 4,
    "workers": 1,
    "java_max_heap_mib": 512,
    "js_file_parallelism": False,
    "js_isolation": False,
    "stages": ["protobuf"],
}
COHORT_BASELINE_PATH = REFRESH_DIR / "cohort-baseline.json"
COHORT_BASELINE = support.load_cohort_baseline(COHORT_BASELINE_PATH)
LOCAL_PATH_RE = re.compile(r"(?:/home/[^\s\"'<>]+|/tmp/[^\s\"'<>]+|file://[^\s\"'<>]+)")
PRIVATE_URL_RE = re.compile(
    r"(?:localhost(?::\d+)?|127\.0\.0\.1(?::\d+)?|draftlink\.lmsousa\.workers\.dev/d/)[^\s\"'<>]*",
    re.IGNORECASE,
)
SECRET_RE = re.compile(
    r"(?:Bearer\s+[A-Za-z0-9._~+/=-]{12,}|(?:gh[pousr]_[A-Za-z0-9_]{24,})|"
    r"(?:api[_-]?key|access[_-]?token|password)\s*[:=]\s*['\"]?[^\s'\"<>]{8,})",
    re.IGNORECASE,
)


def load_report_module():
    spec = importlib.util.spec_from_file_location("protobuf_refresh_report", REPORT_SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load report renderer: {REPORT_SCRIPT}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


REPORT = load_report_module()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_sha256(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def runtime_command(snapshot: Path, output: Path, resource_cache: Path) -> list[str]:
    return [sys.executable, "-u", str(REFRESH_DIR / "collect_runtime.py"),
            "--snapshot", str(snapshot.resolve()), "--output", str(output.resolve()),
            "--resource-cache", str(resource_cache.resolve())]


def memory_command(snapshot: Path, output: Path, resource_cache: Path) -> list[str]:
    return [sys.executable, "-u", str(REFRESH_DIR / "collect_memory.py"),
            "--snapshot", str(snapshot.resolve()), "--output", str(output.resolve()),
            "--resource-cache", str(resource_cache.resolve())]


def _control_rows_hash(rows: list[dict[str, Any]]) -> str:
    controls = [row for row in rows if row.get("stage") != "protobuf"]
    return canonical_sha256(controls)


def verify_control_preservation(prior: Any, app_rows: list[dict[str, Any]],
                                wall_rows: list[dict[str, Any]]) -> dict[str, str]:
    before_app = [row for row in prior["app_plot_rows"] if row.get("stage") != "protobuf"]
    before_wall = [row for row in prior["wall_plot_rows"] if row.get("stage") != "protobuf"]
    after_app = [row for row in app_rows if row.get("stage") != "protobuf"]
    after_wall = [row for row in wall_rows if row.get("stage") != "protobuf"]
    if before_app != after_app or before_wall != after_wall:
        raise ValueError("frozen non-Protobuf chart rows changed during report composition")
    return {"app": _control_rows_hash(before_app), "wall": _control_rows_hash(before_wall)}


def _validate_test_counts(row: dict[str, Any], suite: str) -> None:
    expected = COHORT_BASELINE["suites"][suite]["test_counts"]
    counts = row.get("test_counts")
    if not isinstance(counts, dict):
        raise ValueError(f"{suite} observation has no test-count record")
    for key in ("total_tests", "passed_tests", "failed_tests", "skipped_tests"):
        if counts.get(key) != expected[key]:
            raise ValueError(f"{suite} {key} differs from the pinned cohort")
    if counts.get("failure_names", []) != []:
        raise ValueError(f"{suite} observation contains failing tests")


def _validate_cache_observation(row: dict[str, Any], *, measured: bool) -> None:
    mode = row.get("mode")
    cache = row.get("cache_validation")
    if not isinstance(cache, dict) or cache.get("applicable") is not True:
        raise ValueError("observation has no applicable cache validation")
    if cache.get("passed") is not True or cache.get("stats_error") is not None:
        raise ValueError(f"{mode} cache validation failed")
    hits = cache.get("hits")
    misses = cache.get("misses")
    cacheable = cache.get("cacheable_calls")
    if any(not isinstance(value, int) or isinstance(value, bool) or value < 0
           for value in (hits, misses, cacheable)):
        raise ValueError("cache validation counters are missing or invalid")
    if mode == "direct":
        if cacheable != 0 or hits != 0 or misses != 0 or not cache.get("direct_probe_marker"):
            raise ValueError("direct observation did not prove ccache was disabled")
        marker = Path(cache["direct_probe_marker"])
        run_dir = Path(row.get("run_dir", "")).resolve()
        try:
            marker.resolve().relative_to(run_dir)
        except ValueError as error:
            raise ValueError("direct-mode ccache probe escaped its recorded run directory") from error
        if marker.exists():
            raise ValueError("direct-mode ccache probe recorded a ccache invocation")
    elif mode == "cold":
        if cache.get("direct_probe_marker") is not None:
            raise ValueError("cold observation unexpectedly contains a direct-mode probe")
        if measured and (cacheable <= 0 or misses <= 0):
            raise ValueError("measured cold observation did not record cache misses")
    elif mode == "warm":
        if cache.get("direct_probe_marker") is not None:
            raise ValueError("warm observation unexpectedly contains a direct-mode probe")
        if measured and (cacheable <= 0 or hits <= 0):
            raise ValueError("measured warm observation did not restore cached work")
    else:
        raise ValueError(f"unsupported cache mode: {mode!r}")
    if hits + misses != cacheable:
        raise ValueError("cache hit/miss counters do not add up to cacheable calls")


def validate_runtime_contract(runtime: dict[str, Any]) -> None:
    if runtime.get("workload_contract") != EXPECTED_WORKLOAD:
        raise ValueError("runtime workload contract differs from the pinned Protobuf-only cohort")
    if runtime.get("cohort_baseline_sha256") != support.COHORT_BASELINE_SHA256:
        raise ValueError("runtime capture used a different frozen cohort baseline")
    if runtime.get("cohort_baseline_results_sha256") != COHORT_BASELINE["baseline_results_sha256"]:
        raise ValueError("runtime capture used a different accepted baseline results file")
    source = runtime.get("sources", {}).get("protobuf", {})
    if source.get("selected_java_test_sources") != COHORT_BASELINE["java_selected_test_sources"]:
        raise ValueError("runtime Java test source hashes differ from the frozen selected cohort")
    if source.get("fixture_manifest_sha256") != COHORT_BASELINE["java_fixture_manifest_sha256"]:
        raise ValueError("runtime Java test fixtures differ from the frozen cohort")
    if source.get("javascript_test_sources") != COHORT_BASELINE["javascript_test_sources"]:
        raise ValueError("runtime JavaScript test source hashes differ from the frozen cohort")
    clava_revision = source.get("repositories", {}).get("clava", {}).get("revision")
    expected_js_overlay = support.expected_js_workload_overlay(clava_revision)
    if source.get("javascript_workload_overlay") != expected_js_overlay:
        raise ValueError("runtime JavaScript workload overlay provenance differs from the pinned policy")
    js_defaults = runtime.get("js_vitest_defaults", {})
    for key, expected_value in (("isolate", False), ("fileParallelism", False), ("maxWorkers", 1)):
        if js_defaults.get(key) != expected_value:
            raise ValueError(f"JavaScript Vitest {key} differs from its pinned default")
    for key in ("source_sha256", "emitted_sha256"):
        if not isinstance(js_defaults.get(key), str) or not re.fullmatch(r"[0-9a-f]{64}", js_defaults[key]):
            raise ValueError(f"JavaScript Vitest {key} is missing")
    js_runtime = runtime.get("js_runtime_exports", {})
    vitest_node_url = js_runtime.get("vitest_node_url")
    if not isinstance(vitest_node_url, str) or not vitest_node_url.startswith("file:"):
        raise ValueError("Vitest BaseSequencer must be resolved to a file URL before runtime timing")
    fixtures = support.load_js_file_orders(REFRESH_DIR / "js-file-orders.json")
    suites = ("clava-js", "java")
    for field in ("preflight", "seeds"):
        expected_mode = "direct" if field == "preflight" else "warm"
        expected_preflight = {
            (phase, suite, expected_mode, 0)
            for phase in ("app", "wall")
            for suite in suites
        }
        rows = runtime.get(field)
        if not isinstance(rows, list):
            raise ValueError(f"runtime {field} evidence is missing")
        keys = set()
        for row in rows:
            key = (row.get("phase"), row.get("suite"), row.get("mode"), row.get("repeat"))
            keys.add(key)
            if row.get("stage") != "protobuf" or row.get("valid") is not True or row.get("return_code") != 0:
                raise ValueError(f"runtime {field} contains an invalid preflight row")
            if row.get("measured") is not False:
                raise ValueError(f"runtime {field} contains a timed row")
            suite = row.get("suite")
            if suite not in COHORT_BASELINE["suites"]:
                raise ValueError(f"runtime {field} contains an unknown test suite")
            expected_suite = COHORT_BASELINE["suites"][suite]
            if row.get("test_identity_sha256") != expected_suite["test_identity_sha256"]:
                raise ValueError(f"runtime {field} test identities differ from the pinned cohort")
            _validate_test_counts(row, suite)
            _validate_cache_observation(row, measured=False)
            if row.get("phase") == "app":
                _validate_app_observation(row, suite, fixtures)
            elif suite == "clava-js" and row.get("js_file_order") != fixtures["wall"]:
                raise ValueError("JavaScript wall preflight order differs from the pinned fixture")
        if keys != expected_preflight:
            raise ValueError(f"runtime {field} does not contain the exact four-suite/mode preflight cohort")

    if runtime.get("preflight_complete") is not True:
        raise ValueError("runtime capture did not finish both App and wall preflights before timing")
    if not isinstance(runtime.get("preflight_completed_utc"), str):
        raise ValueError("runtime capture is missing its completed preflight timestamp")

    expected = {
        (phase, suite, mode, repeat)
        for phase in ("app", "wall")
        for suite in suites
        for mode in ("direct", "cold", "warm")
        for repeat in range(1, 5)
    }
    observations = runtime.get("observations")
    if not isinstance(observations, list) or len(observations) != 48:
        raise ValueError("runtime capture must contain exactly 48 measured observations")
    actual = set()
    for row in observations:
        key = (row.get("phase"), row.get("suite"), row.get("mode"), row.get("repeat"))
        if key in actual:
            raise ValueError(f"runtime capture duplicates observation {key}")
        actual.add(key)
        phase, suite, mode, _repeat = key
        if (row.get("stage") != "protobuf" or row.get("valid") is not True
                or row.get("measured") is not True or row.get("return_code") != 0):
            raise ValueError(f"runtime observation failed or is not measured: {key}")
        if suite not in COHORT_BASELINE["suites"] or phase not in ("app", "wall"):
            raise ValueError(f"runtime observation has an unknown cohort identity: {key}")
        _validate_test_counts(row, suite)
        if row.get("test_identity_sha256") != COHORT_BASELINE["suites"][suite]["test_identity_sha256"]:
            raise ValueError(f"runtime observation test identities differ from the pinned cohort: {key}")
        _validate_cache_observation(row, measured=True)
        elapsed = row.get("wall_s")
        if (not isinstance(elapsed, (int, float)) or isinstance(elapsed, bool)
                or not math.isfinite(elapsed) or elapsed <= 0):
            raise ValueError(f"runtime observation has invalid wall time: {key}")
        if phase == "app":
            _validate_app_observation(row, suite, fixtures)
            app_elapsed = row.get("app_elapsed_ms")
            if (not isinstance(app_elapsed, (int, float)) or isinstance(app_elapsed, bool)
                    or not math.isfinite(app_elapsed) or app_elapsed < 0):
                raise ValueError(f"App observation has invalid elapsed time: {key}")
            args = row.get("actual_jvm_args")
            if not isinstance(args, list) or any(
                    "ExplicitGC" in item or item.startswith("-Xlog:gc") for item in args):
                raise ValueError(f"App observation contains forced GC or GC logging: {key}")
            if suite == "java" and row.get("actual_max_heap_bytes") != 512 * 1024 * 1024:
                raise ValueError(f"Java worker heap differs from 512 MiB: {key}")
            if suite == "clava-js" and any(item.startswith(("-Xmx", "-Xms")) for item in args):
                raise ValueError(f"JavaScript worker overrides its default JVM heap: {key}")
        else:
            if any(name in row for name in ("metrics", "app_elapsed_ms", "app_calls")):
                raise ValueError(f"wall observation includes App instrumentation: {key}")
            if suite == "java" and row.get("test_counts", {}).get("total_tests") != 116:
                raise ValueError(f"Java wall observation did not run the 116-test cohort: {key}")
            if suite == "clava-js":
                run_dir = Path(row.get("run_dir", ""))
                command_path = run_dir / "command.json"
                if not command_path.is_file():
                    raise ValueError(f"JavaScript wall command record is missing: {key}")
                command = load_json(command_path).get("argv", [])
                if not command or "APP_BUILD_OVERLAY_JAR" in " ".join(command):
                    raise ValueError(f"JavaScript wall command has invalid instrumentation: {key}")
                if row.get("js_file_order") != fixtures["wall"]:
                    raise ValueError(f"JavaScript wall file order differs from the frozen fixture: {key}")
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        raise ValueError(f"runtime observation matrix differs from contract; missing={missing}, extra={extra}")


def _validate_app_observation(row: dict[str, Any], suite: str,
                              fixtures: dict[str, list[str]]) -> None:
    expected = COHORT_BASELINE["suites"][suite]
    if (row.get("app_calls") != expected["app_calls"]
            or row.get("syntax_only_calls") != expected["syntax_only_calls"]
            or row.get("source_count") != expected["source_count"]):
        raise ValueError(f"{suite} App workload counts differ from the pinned baseline")
    if support.digest(row.get("context_pattern")) != expected["context_pattern_sha256"]:
        raise ValueError(f"{suite} App context/workload fingerprint differs from the pinned baseline")
    if suite == "clava-js" and row.get("js_file_order") != fixtures["app"]:
        raise ValueError("JavaScript App file order differs from the frozen fixture")


def validate_report_html(html: str) -> None:
    if not html.lstrip().lower().startswith("<!doctype html"):
        raise ValueError("rendered report is not a standalone HTML document")
    for pattern, label in ((LOCAL_PATH_RE, "local filesystem path"),
                           (PRIVATE_URL_RE, "private or local URL"),
                           (SECRET_RE, "secret-like value")):
        match = pattern.search(html)
        if match:
            raise ValueError(f"refusing to publish HTML containing a {label}")


def _expected_memory_inputs(run_dir: Path, memory: dict[str, Any],
                            runtime: dict[str, Any], manifest: dict[str, Any],
                            manifest_sha: str) -> list[tuple[str, Any]]:
    expected_workloads = {item["key"]: item for item in support.MEMORY_WORKLOADS}
    workloads = memory.get("workloads")
    if not isinstance(workloads, list) or {item.get("key") for item in workloads} != set(expected_workloads):
        raise ValueError("memory record does not contain the two pinned Protobuf workloads")
    for workload in workloads:
        expected = expected_workloads[workload["key"]]
        if (workload.get("relative_source") != expected["relative_source"]
                or workload.get("standard") != expected["standard"]
                or workload.get("source_sha256") != expected["expected_sha256"]
                or not str(workload.get("source", "")).endswith(expected["relative_source"])):
            raise ValueError(f"memory workload {workload['key']} differs from the pinned source")

    if memory.get("jvm_options") != ["-Xms256m", "-Xmx4g", "-XX:+UseG1GC", "-XX:-DisableExplicitGC"]:
        raise ValueError("memory diagnostic JVM options differ from the accepted cleanup methodology")
    if memory.get("cache_policy") != "CCACHE_DISABLE=true; AST_DUMP_CACHE=false in ValidationProbe":
        raise ValueError("memory diagnostic cache policy differs from the accepted methodology")

    observations = memory.get("observations")
    expected_runs = {(key, repeat) for key in expected_workloads for repeat in range(1, 4)}
    actual_runs = {(row.get("workload"), row.get("repeat")) for row in observations or []}
    if not isinstance(observations, list) or len(observations) != 6 or actual_runs != expected_runs:
        raise ValueError("memory record does not contain the six pinned fresh JVM runs")
    published = memory.get("published_manifest", {})
    assets = manifest.get("assets", [])
    tool_assets = [asset for asset in assets if asset.get("kind") == "tool"
                   and asset.get("platform") == "linux" and asset.get("arch") == "x64"]
    if len(tool_assets) != 1:
        raise ValueError("release manifest does not identify one Linux x64 Protobuf tool")
    required_asset_names = {
        "clang-dumper-release-manifest.json", tool_assets[0]["filename"],
        "clang-dumper-ast-wire.proto", "clang-dumper-ast-wire.pb",
    }
    expected_asset_hashes = {
        asset["filename"]: asset["sha256"] for asset in assets
        if asset.get("filename") in required_asset_names
    }
    expected_asset_hashes["clang-dumper-release-manifest.json"] = manifest_sha
    if (set(expected_asset_hashes) != required_asset_names
            or published.get("verified_assets") != expected_asset_hashes
            or published.get("protocol") != manifest.get("protocol")
            or published.get("toolchain") != manifest.get("toolchain")):
        raise ValueError("memory record release asset hashes differ from the published manifest")
    runtime_memory = memory.get("runtime", {})
    if published.get("sha256") != manifest_sha:
        raise ValueError("memory record uses a different published release manifest")
    if published.get("native_tool_commit") != runtime.get("sources", {}).get("protobuf", {}).get("repositories", {}).get("clang-dumper", {}).get("revision"):
        raise ValueError("memory record uses a different native repository revision")
    if runtime_memory.get("jars") != runtime.get("runtime_jars", {}).get("protobuf"):
        raise ValueError("memory record runtime JAR hashes differ from the runtime capture")
    if runtime_memory.get("validation_probe_source_sha256") != runtime.get("sources", {}).get("protobuf", {}).get("validation_probe_sha256"):
        raise ValueError("memory record ValidationProbe source hash differs from the runtime capture")
    checkout_value = runtime.get("sources", {}).get("protobuf", {}).get("checkout")
    if not isinstance(checkout_value, str):
        raise ValueError("runtime capture is missing the isolated Clava snapshot path")
    checkout = Path(checkout_value).resolve()
    expected_runtime = checkout / "ClavaWeaver/build/install/ClavaWeaver"
    expected_classes = checkout / "ClangAstParser/build/protobuf-validation-classes"
    if (Path(runtime_memory.get("path", "")).resolve() != expected_runtime.resolve()
            or Path(runtime_memory.get("classes", "")).resolve() != expected_classes.resolve()):
        raise ValueError("memory runtime paths do not belong to the captured isolated Clava snapshot")
    classes_tree = runtime_memory.get("classes_tree")
    if (not isinstance(classes_tree, dict) or "ValidationProbe.class" not in classes_tree
            or any(not isinstance(row, dict)
                   or not re.fullmatch(r"[0-9a-f]{64}", str(row.get("sha256", "")))
                   for row in classes_tree.values())):
        raise ValueError("memory record is missing its compiled ValidationProbe class hashes")
    expected_sources = {}
    for workload in workloads:
        source = checkout / "ClangAstParser/test-resources" / workload["relative_source"]
        if Path(workload["source"]).resolve() != source.resolve():
            raise ValueError(f"memory workload {workload['key']} does not use the isolated snapshot source")
        if not source.is_file() or sha256_file(source) != workload["source_sha256"]:
            raise ValueError(f"memory workload {workload['key']} source hash differs from its captured file")
        expected_sources[workload["key"]] = workload
    for observation in observations:
        if (observation.get("release_manifest_sha256") != manifest_sha
                or observation.get("release_asset_sha256") != expected_asset_hashes
                or not re.fullmatch(r"[0-9a-f]{64}", str(observation.get("resource_cache_tree_sha256", "")))):
            raise ValueError("memory observation release or resource-cache hashes differ from the pinned capture")
        if observation.get("return_code") != 0:
            raise ValueError("memory observation did not exit successfully")
        for name in ("wall_s", "gnu_time.user_s", "gnu_time.sys_s", "gnu_time.elapsed_s"):
            value: Any = observation
            for component in name.split("."):
                value = value.get(component) if isinstance(value, dict) else None
            if (not isinstance(value, (int, float)) or isinstance(value, bool)
                    or not math.isfinite(value) or value <= 0):
                raise ValueError(f"memory observation has invalid {name}")
        gnu_time = observation.get("gnu_time", {})
        if (gnu_time.get("exit_status") != 0
                or not isinstance(gnu_time.get("max_rss_kb"), int)
                or isinstance(gnu_time.get("max_rss_kb"), bool)
                or gnu_time["max_rss_kb"] <= 0):
            raise ValueError("memory GNU time record is incomplete or unsuccessful")
        workload = expected_sources[observation["workload"]]
        log_path = Path(observation.get("log", "")).resolve()
        work = log_path.parent
        resource_root = (work / "resources").resolve()
        runtime_path = Path(runtime_memory["path"]).resolve()
        classes_path = Path(runtime_memory["classes"]).resolve()
        java_binary = Path(runtime_memory.get("java_binary", "")).resolve()
        expected_command = [
            str(Path(observation["command"][0]).resolve()), "-v", "-o", str(work / "time.txt"), "--",
            str(java_binary), *memory["jvm_options"],
            "-Djava.io.tmpdir=" + str(work / "tmp"), "-cp",
            str(classes_path) + os.pathsep + str(runtime_path / "lib" / "*"),
            "ValidationProbe", "memory", str(Path(workload["source"]).resolve()), str(work),
            workload["standard"], "20", "true", str(resource_root),
        ]
        if (not expected_command[0].endswith("/time")
                or observation.get("command") != expected_command
                or observation.get("resource_root") != str(resource_root)):
            raise ValueError("memory observation command differs from the pinned fresh-JVM workload")
        command_record = load_json(work / "command.json")
        expected_cwd = str(checkout)
        if command_record != {"argv": expected_command, "cwd": expected_cwd}:
            raise ValueError("memory observation command sidecar differs from its captured command")
        raw_time = gnu_time.get("raw")
        expected_timed_command = "\tCommand being timed: \"" + " ".join(expected_command[5:]) + "\""
        time_path = work / "time.txt"
        if (not isinstance(raw_time, str) or not time_path.is_file()
                or time_path.read_text(errors="replace") != raw_time
                or raw_time.splitlines()[0] != expected_timed_command):
            raise ValueError("memory GNU time sidecar differs from its captured Java command")
        if abs(float(observation["wall_s"]) - float(gnu_time["elapsed_s"])) >= 0.1:
            raise ValueError("memory wall clock and GNU time elapsed values differ")
        log_path = Path(observation["log"])
        if not log_path.is_file():
            raise ValueError("memory probe log is missing")
        logged_phases = []
        for line in log_path.read_text(errors="replace").splitlines():
            if "CLAVA_HEAP " in line:
                logged_phases.append(json.loads(line.split("CLAVA_HEAP ", 1)[1]))
        phases = observation.get("heap_phases")
        if logged_phases != phases:
            raise ValueError("memory probe log rows differ from the captured heap phases")

    labels = {"nas-lu": "NAS+", "templates": "C++ templates"}
    outputs = []
    for key, label in labels.items():
        sidecar = load_json(run_dir / "memory" / f"memory-{key}.json")
        expected_payload = support.normalize_memory_workload(memory, key)
        if sidecar != expected_payload:
            raise ValueError(f"{label} memory sidecar does not match its source capture record")
        outputs.append((label, sidecar))
    return outputs


def validate_run_bundle(run_dir: Path, prior_path: Path = FROZEN_CONTROLS,
                        *, allow_collecting_status: bool = False) -> dict[str, Any]:
    run_dir = run_dir.expanduser().resolve()
    status_path = run_dir / "status.json"
    runtime_path = run_dir / "runtime/results.json"
    memory_path = run_dir / "memory/memory.json"
    release_path = run_dir / "release-manifest.json"
    for path in (status_path, runtime_path, memory_path, release_path, prior_path):
        if not path.is_file():
            raise ValueError(f"completed benchmark bundle is missing {path.name}")
    snapshot_identity_path = run_dir / "snapshot-identity.json"
    if not snapshot_identity_path.is_file():
        raise ValueError("completed benchmark bundle is missing snapshot identity")
    status = load_json(status_path)
    acceptable_states = {"complete", "validating"} if allow_collecting_status else {"complete"}
    if status.get("status") not in acceptable_states:
        raise ValueError(f"benchmark bundle is not complete (status={status.get('status')!r})")
    prior_sha = sha256_file(prior_path)
    if status.get("frozen_controls_sha256") != prior_sha:
        raise ValueError("frozen control evidence changed since collection")
    snapshot_identity = load_json(snapshot_identity_path)
    if status.get("snapshot_identity_sha256") != sha256_file(snapshot_identity_path):
        raise ValueError("snapshot identity hash differs from collection status")
    snapshot_release = snapshot_identity.get("published_release", {})
    if snapshot_release.get("tag") != status.get("release_tag"):
        raise ValueError("snapshot identity release tag differs from collection status")

    runtime = load_json(runtime_path)
    if runtime.get("status") != "complete":
        raise ValueError("runtime capture is incomplete")
    if runtime.get("driver_sha256") != sha256_file(REFRESH_DIR / "collect_runtime.py"):
        raise ValueError("runtime driver hash does not match this checked-in workflow")
    validate_runtime_contract(runtime)
    if (snapshot_identity.get("isolated_overlays", {}).get("javascript_workload")
            != runtime.get("sources", {}).get("protobuf", {}).get("javascript_workload_overlay")):
        raise ValueError("snapshot and runtime JavaScript workload overlay provenance differ")
    if runtime.get("selected_release", {}).get("tag") != status.get("release_tag"):
        raise ValueError("runtime release selector does not match collection status")
    expected_revisions = {
        "clava": snapshot_identity.get("sources", {}).get("clava", {}).get("revision"),
        "specs-java-libs": snapshot_identity.get("sources", {}).get("specs-java-libs", {}).get("revision"),
        "lara-framework": snapshot_identity.get("sources", {}).get("lara-framework", {}).get("revision"),
        "clang-dumper": snapshot_release.get("native_repository_revision"),
    }
    repositories = runtime.get("sources", {}).get("protobuf", {}).get("repositories", {})
    for name, revision in expected_revisions.items():
        if not isinstance(revision, str) or repositories.get(name, {}).get("revision") != revision:
            raise ValueError(f"runtime {name} source revision differs from the isolated snapshot")
    report_rows = REPORT.validate_updated_manifest(runtime)
    app_rows, wall_rows = REPORT.combine_rows(load_json(prior_path), report_rows)
    controls = verify_control_preservation(load_json(prior_path), app_rows, wall_rows)

    manifest_bytes = release_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    manifest_sha = hashlib.sha256(manifest_bytes).hexdigest()
    if status.get("release_manifest_sha256") != manifest_sha:
        raise ValueError("copied release manifest hash differs from collection status")
    provenance = REPORT.validate_release_provenance(runtime, manifest, manifest_sha)

    memory = load_json(memory_path)
    if memory.get("status") != "complete":
        raise ValueError("memory capture is incomplete")
    if memory.get("driver_sha256") != sha256_file(REFRESH_DIR / "collect_memory.py"):
        raise ValueError("memory driver hash does not match this checked-in workflow")
    if memory.get("selector", {}).get("tag") != status.get("release_tag"):
        raise ValueError("memory workload selected a different release tag")
    if memory.get("published_manifest", {}).get("sha256") != manifest_sha:
        raise ValueError("memory workload used a different published release manifest")
    if (memory.get("total_parse_cycles") != 120 or memory.get("jvm_repeats_per_workload") != 3
            or memory.get("cycles_per_jvm") != 20 or len(memory.get("observations", [])) != 6):
        raise ValueError("memory capture does not contain six fresh JVMs and 120 strict-cleanup cycles")
    memory_inputs = _expected_memory_inputs(run_dir, memory, runtime, manifest, manifest_sha)
    REPORT._validate_memory_inputs(memory_inputs)
    return {
        "runtime": runtime,
        "memory": memory,
        "manifest": manifest,
        "manifest_sha256": manifest_sha,
        "memory_inputs": memory_inputs,
        "controls_sha256": controls,
        "provenance": provenance,
        "prior": load_json(prior_path),
        "report_rows": report_rows,
    }


def render_run(run_dir: Path, output: Path | None = None,
               prior_path: Path = FROZEN_CONTROLS,
               *, allow_collecting_status: bool = False) -> Path:
    run_dir = run_dir.expanduser().resolve()
    bundle = validate_run_bundle(run_dir, prior_path,
                                 allow_collecting_status=allow_collecting_status)
    html = REPORT.render_report(
        bundle["prior"], bundle["runtime"], bundle["memory_inputs"],
        bundle["manifest"], bundle["manifest_sha256"],
    )
    validate_report_html(html)
    destination = (output or (run_dir / "report.html")).expanduser().resolve()
    destination.parent.mkdir(parents=True, exist_ok=True)
    rendered = html.encode("utf-8")
    try:
        with destination.open("xb") as stream:
            stream.write(rendered)
    except FileExistsError:
        if destination.read_bytes() != rendered:
            raise ValueError(f"refusing to overwrite existing report output: {destination}")
    return destination


def _write_json(path: Path, value: Any) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(path)


def _write_json_exclusive(path: Path, value: Any) -> None:
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2, sort_keys=True)
        stream.write("\n")


def _new_run_directory(requested: Path | None) -> Path:
    if requested is None:
        timestamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        requested = DEFAULT_OUTPUT_BASE / f"protobuf-{timestamp}-{uuid.uuid4().hex[:8]}"
    output = requested.expanduser().resolve()
    if output.exists():
        raise ValueError(f"refusing to reuse an existing evidence directory: {output}")
    output.mkdir(parents=True)
    return output


def _stream_command(command: list[str], log_path: Path) -> int:
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, bufsize=1)
        assert process.stdout is not None
        for line in process.stdout:
            log.write(line)
            log.flush()
            print(line, end="", flush=True)
        return process.wait()


def collect(snapshot: Path, resource_cache: Path, output: Path | None = None,
            prior_path: Path = FROZEN_CONTROLS, lock_path: Path = DEFAULT_LOCK,
            command_runner: Callable[[list[str], Path], int] = _stream_command) -> Path:
    lock_path = lock_path.expanduser().resolve()
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open("a") as lock:
        try:
            fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise RuntimeError(f"another Protobuf benchmark collection is active (lock {lock_path})") from error
        snapshot_identity_path = snapshot.expanduser().resolve() / "snapshot-identity.json"
        if not snapshot_identity_path.is_file():
            raise ValueError("benchmark snapshot is missing snapshot-identity.json")
        snapshot_identity = load_json(snapshot_identity_path)
        release_tag = snapshot_identity.get("published_release", {}).get("tag")
        if not isinstance(release_tag, str) or not release_tag:
            raise ValueError("snapshot identity does not name a published release")
        run_dir = _new_run_directory(output)
        runtime_out = run_dir / "runtime"
        memory_out = run_dir / "memory"
        status = {
            "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
            "status": "collecting",
            "snapshot_identity_sha256": sha256_file(snapshot / "snapshot-identity.json"),
            "frozen_controls_sha256": sha256_file(prior_path),
            "runtime_driver_sha256": sha256_file(REFRESH_DIR / "collect_runtime.py"),
            "memory_driver_sha256": sha256_file(REFRESH_DIR / "collect_memory.py"),
            "runtime_log": "runtime.stdout.log",
            "memory_log": "memory.stdout.log",
            "memory_origin": "fresh",
            "release_tag": release_tag,
        }
        _write_json(run_dir / "status.json", status)
        shutil.copy2(snapshot_identity_path, run_dir / "snapshot-identity.json")
        try:
            runtime_out.parent.mkdir(parents=True, exist_ok=True)
            runtime_code = command_runner(runtime_command(snapshot, runtime_out, resource_cache),
                                          run_dir / "runtime.stdout.log")
            if runtime_code != 0:
                raise RuntimeError(f"runtime collection failed with exit code {runtime_code}")
            if not (runtime_out / "results.json").is_file():
                raise RuntimeError("runtime collector exited successfully without results.json")
            manifest_source = snapshot.resolve() / "clang-dumper/build/clang-dumper-release-manifest.json"
            if not manifest_source.is_file():
                raise RuntimeError("snapshot is missing the published release manifest")
            shutil.copy2(manifest_source, run_dir / "release-manifest.json")
            status["release_manifest_sha256"] = sha256_file(run_dir / "release-manifest.json")

            memory_code = command_runner(memory_command(snapshot, memory_out, resource_cache),
                                         run_dir / "memory.stdout.log")
            if memory_code != 0:
                raise RuntimeError(f"memory collection failed with exit code {memory_code}")
            status["status"] = "validating"
            _write_json(run_dir / "status.json", status)
            render_run(run_dir, prior_path=prior_path, allow_collecting_status=True)
            status["status"] = "complete"
            status["completed_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
            status["report_sha256"] = sha256_file(run_dir / "report.html")
            status["controls_sha256"] = validate_run_bundle(
                run_dir, prior_path, allow_collecting_status=True)["controls_sha256"]
            _write_json(run_dir / "status.json", status)
            return run_dir
        except Exception as error:
            status["status"] = "failed"
            status["error"] = str(error)
            status["failed_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
            _write_json(run_dir / "status.json", status)
            raise


def publish_private(run_dir: Path, draft_id: str, prior_path: Path = FROZEN_CONTROLS,
                    *, private: bool, dry_run: bool = False,
                    runner: Callable[..., Any] = subprocess.run) -> dict[str, Any]:
    if not private:
        raise ValueError("private publication requires the explicit --private flag")
    run_dir = run_dir.expanduser().resolve()
    html_path = render_run(run_dir, prior_path=prior_path)
    html_bytes = html_path.read_bytes()
    if dry_run:
        return {"published": False, "html": str(html_path), "sha256": hashlib.sha256(html_bytes).hexdigest()}
    executable = shutil.which("draftlink")
    if executable is None:
        raise RuntimeError("draftlink CLI is not installed")
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    attempt_dir = run_dir / "publications" / f"attempt-{stamp}-{uuid.uuid4().hex[:8]}"
    attempt_dir.mkdir(parents=True, exist_ok=False)
    report_sha256 = hashlib.sha256(html_bytes).hexdigest()
    update = runner([executable, "update", draft_id, "--file", str(html_path), "--private",
                     "--title", "Clava Protobuf benchmark refresh", "--project", "clava"],
                    capture_output=True, check=False)
    (attempt_dir / "update.stdout.log").write_bytes(update.stdout or b"")
    (attempt_dir / "update.stderr.log").write_bytes(update.stderr or b"")
    if update.returncode != 0:
        _write_json_exclusive(attempt_dir / "publication.json", {
            "status": "update_failed", "html_sha256": report_sha256,
            "update_returncode": update.returncode,
        })
        raise RuntimeError(f"DraftLink update failed with exit code {update.returncode}")
    readback = runner([executable, "read", draft_id], capture_output=True, check=False)
    (attempt_dir / "readback.stdout.log").write_bytes(readback.stdout or b"")
    (attempt_dir / "readback.stderr.log").write_bytes(readback.stderr or b"")
    if readback.returncode != 0:
        _write_json_exclusive(attempt_dir / "publication.json", {
            "status": "readback_failed", "html_sha256": report_sha256,
            "update_returncode": update.returncode, "readback_returncode": readback.returncode,
        })
        raise RuntimeError(f"DraftLink readback failed with exit code {readback.returncode}")
    if readback.stdout != html_bytes:
        (attempt_dir / "readback-mismatch.html").write_bytes(readback.stdout or b"")
        _write_json_exclusive(attempt_dir / "publication.json", {
            "status": "readback_mismatch", "html_sha256": report_sha256,
            "readback_sha256": hashlib.sha256(readback.stdout or b"").hexdigest(),
            "update_returncode": update.returncode, "readback_returncode": readback.returncode,
        })
        raise RuntimeError("DraftLink readback HTML differs byte-for-byte from the rendered report")
    _write_json_exclusive(attempt_dir / "publication.json", {
        "status": "published_and_verified", "html_sha256": report_sha256,
        "readback_sha256": hashlib.sha256(readback.stdout).hexdigest(),
        "readback_bytes": len(readback.stdout),
        "update_returncode": update.returncode, "readback_returncode": readback.returncode,
    })
    return {"published": True, "attempt_dir": str(attempt_dir), "sha256": report_sha256}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    prepare = subparsers.add_parser("prepare", help="create an isolated build snapshot")
    prepare.add_argument("--workspace-root", type=Path, required=True)
    prepare.add_argument("--release-assets-root", type=Path, required=True)
    prepare.add_argument("--output-root", type=Path)
    prepare.add_argument("--release-tag", default="v18.1.8_6-rc8")

    collect_parser = subparsers.add_parser("collect", help="collect runtime and memory observations")
    collect_parser.add_argument("--snapshot", type=Path, required=True)
    collect_parser.add_argument("--resource-cache", type=Path, required=True)
    collect_parser.add_argument("--output-root", type=Path)
    collect_parser.add_argument("--prior-evidence", type=Path, default=FROZEN_CONTROLS)
    collect_parser.add_argument("--lock-file", type=Path, default=DEFAULT_LOCK)

    render_parser = subparsers.add_parser("render", help="render a completed saved capture")
    render_parser.add_argument("--run-dir", type=Path, required=True)
    render_parser.add_argument("--prior-evidence", type=Path, default=FROZEN_CONTROLS)
    render_parser.add_argument("--output", type=Path)

    publish_parser = subparsers.add_parser("publish", help="update and verify the private DraftLink")
    publish_parser.add_argument("--run-dir", type=Path, required=True)
    publish_parser.add_argument("--draft-id", required=True)
    publish_parser.add_argument("--private", action="store_true", required=True,
                                 help="required; the report remains private")
    publish_parser.add_argument("--dry-run", action="store_true",
                                help="render and validate without calling DraftLink")
    publish_parser.add_argument("--prior-evidence", type=Path, default=FROZEN_CONTROLS)

    args = parser.parse_args(argv)
    try:
        if args.command == "prepare":
            from prepare_snapshot import prepare_snapshot
            if args.output_root is None:
                stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
                args.output_root = DEFAULT_OUTPUT_BASE.parent / "snapshots" / f"protobuf-{stamp}-{uuid.uuid4().hex[:8]}"
            result = prepare_snapshot(args.workspace_root, args.output_root,
                                      args.release_assets_root, args.release_tag)
            print(json.dumps({"snapshot": result["snapshot"], "release_tag": result["published_release"]["tag"]}, indent=2))
        elif args.command == "collect":
            run_dir = collect(args.snapshot, args.resource_cache, args.output_root,
                              args.prior_evidence, args.lock_file)
            print(json.dumps({"status": "complete", "run_dir": str(run_dir),
                              "report_sha256": sha256_file(run_dir / "report.html")}, indent=2))
        elif args.command == "render":
            output = render_run(args.run_dir, args.output, args.prior_evidence)
            print(json.dumps({"status": "complete", "html": str(output),
                              "sha256": sha256_file(output)}, indent=2))
        else:
            result = publish_private(args.run_dir, args.draft_id, args.prior_evidence,
                                     private=args.private, dry_run=args.dry_run)
            print(json.dumps(result, indent=2))
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError, json.JSONDecodeError) as error:
        print(f"refresh_benchmark.py: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
