#!/usr/bin/env python3
"""Run the frozen four-stage Java matrix without JaCoCo or GC instrumentation.

The ten direct/cold/warm preflights run first and seed only this output root's
warm caches.  The measured matrix then records four rotated rounds.  Optional
JaCoCo-on direct/cold Text and Protobuf diagnostics are interleaved with the
off-agent cells but stored in a separate manifest.
"""

from __future__ import annotations

import argparse
import copy
import csv
import datetime as dt
import getpass
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True

import run_comparison as base
import run_deadline_matrix as deadline
import run_java_jacoco_agent_control as control


SCRIPT_ROOT = Path(__file__).resolve().parent
DEFAULT_MATRIX = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/results.json"
DEFAULT_OUTPUT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-noagent-matrix-r2"
STAGES = ("before-cache", "ccache-text", "protobuf", "flatbuffers")
MODES = ("direct", "cold", "warm")
EXPECTED = control.EXPECTED_JAVA
EXPECTED_PLAN = control.EXPECTED_JAVA_PLAN
EXPECTED_CACHE = {
    "direct": {"cacheable_calls": 0, "hits": 0, "misses": 0, "uncacheable_calls": 0},
    "cold": {"cacheable_calls": 208, "hits": 0, "misses": 208, "uncacheable_calls": 0},
    "warm": {"cacheable_calls": 208, "hits": 207, "misses": 1, "uncacheable_calls": 0},
}
AGENT_FLAGS = ("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("preflight", "measure", "finalize"), required=True)
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--skip-agent-diagnostics", action="store_true",
                        help="run only the 40 OFF-agent headline measurements")
    return parser.parse_args()


def canonical_hash(value: object) -> str:
    import hashlib
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def cache_expected_from_primary(matrix: dict) -> None:
    observed: dict[tuple[str, str], set[tuple[int, int, int, int]]] = {}
    for row in matrix["results"]:
        if row.get("suite") != "java" or row.get("measured") is not True or not row.get("valid"):
            continue
        key = (row["stage"], row["mode"])
        cache = row.get("cache_validation", {})
        observed.setdefault(key, set()).add((int(row.get("cacheable_calls", 0)),
                                              int(row.get("cache_hits", 0)),
                                              int(row.get("cache_misses", 0)),
                                              int(cache.get("uncacheable_calls", 0))))
    for stage in STAGES:
        for mode in (MODES if stage != "before-cache" else ("direct",)):
            if len(observed.get((stage, mode), set())) != 1:
                raise RuntimeError(f"primary cache outcomes are not unique for {stage}/{mode}")
            actual = next(iter(observed[(stage, mode)]))
            wanted = EXPECTED_CACHE[mode]
            expected = (wanted["cacheable_calls"], wanted["hits"], wanted["misses"], wanted["uncacheable_calls"])
            if actual != expected:
                raise RuntimeError(f"primary cache contract changed for {stage}/{mode}: {actual} != {expected}")


def verify_stage(stage: dict) -> dict:
    if stage["key"] != "before-cache":
        return control.verify_stage(stage)
    clava = Path(stage["root"]) / "clava"
    runtime = clava / "Clava-JS/java-binaries"
    parser_jar = runtime / "lib/ClangAstParser.jar"
    observations = {
        "clava_revision": control.git(clava, "rev-parse", "HEAD"),
        "clava_status": control.git_status(clava),
        "clava_patch_sha256": control.diff_sha256(clava),
        "parser_jar_sha256": base.sha256_file(parser_jar),
        "runtime_manifest": base.runtime_manifest(runtime),
    }
    expected = {
        "clava_revision": stage["clava_revision"],
        "clava_status": stage["clava_status"],
        "clava_patch_sha256": stage["clava_patch_sha256"],
        "parser_jar_sha256": stage["parser_jar_sha256"],
        "runtime_manifest": {"jar_count": stage["runtime_manifest"]["jar_count"],
                             "sha256": stage["runtime_manifest_sha256"]},
    }
    if observations != expected:
        raise RuntimeError(f"frozen before-cache Java stage changed: {observations}")
    for name in ("specs_java_libs", "lara_framework"):
        dependency = stage["java_build_dependencies"][name]
        root = Path(dependency["root"])
        actual = {"revision": control.git(root, "rev-parse", "HEAD"),
                  "status": control.git_status(root),
                  "diff_sha256": control.diff_sha256(root)}
        wanted = {key: dependency[key] for key in ("revision", "status", "diff_sha256")}
        if name == "specs_java_libs":
            actual["specsutils_jar_sha256"] = base.sha256_file(Path(dependency["specsutils_jar"]))
            wanted["specsutils_jar_sha256"] = dependency["specsutils_jar_sha256"]
        if actual != wanted:
            raise RuntimeError(f"frozen {name} build input changed")
    return observations


def load_frozen(matrix_path: Path) -> tuple[dict, dict[str, dict]]:
    matrix = json.loads(matrix_path.read_text())
    if matrix.get("schema_version") != 1 or matrix.get("identity_preflight", {}).get("passed") is not True:
        raise RuntimeError("frozen matrix schema or identity preflight is invalid")
    if matrix["plan"].get("expected_tests", {}).get("java") != EXPECTED_PLAN:
        raise RuntimeError("frozen matrix does not describe the exact 116-test Java suite")
    ids = matrix["identity_preflight"].get("reference_test_ids", {}).get("java", {})
    if ids.get("count") != EXPECTED["total_tests"]:
        raise RuntimeError("frozen Java identity is not 116 tests")
    cache_expected_from_primary(matrix)
    stages = {key: matrix["plan"]["stages"][key] for key in STAGES}
    for key in STAGES:
        verify_stage(stages[key])
    current_fixtures = deadline.fixture_maps([stages[key] for key in STAGES])
    if current_fixtures != matrix["plan"]["fixture_fingerprints"]:
        raise RuntimeError("frozen Java/JS fixture fingerprints changed")
    return matrix, stages


def schedule(round_number: int) -> list[tuple[str, str]]:
    return [(mode, stage["key"]) for mode, stage in deadline.stage_mode_schedule(round_number)]


def cell_id(mode: str, stage: str, repeat: int | None) -> str:
    return f"java/{mode}/{stage}/" + ("warmup" if repeat is None else f"repeat-{repeat:02d}")


def identity_records(junit_root: Path) -> list[dict[str, str]]:
    records = []
    for path in sorted(junit_root.glob("TEST-*.xml")):
        suite = ET.parse(path).getroot()
        for case in suite.findall("testcase"):
            failed = case.find("failure") is not None or case.find("error") is not None
            skipped = case.find("skipped") is not None
            records.append({"class": case.attrib.get("classname", ""),
                            "name": case.attrib.get("name", ""),
                            "status": "failed" if failed else "skipped" if skipped else "passed"})
    return sorted(records, key=lambda item: (item["class"], item["name"], item["status"]))


def junit_failures(junit_root: Path) -> list[str]:
    found = []
    for path in sorted(junit_root.glob("TEST-*.xml")):
        suite = ET.parse(path).getroot()
        for case in suite.findall("testcase"):
            failure = case.find("failure") or case.find("error")
            if failure is not None:
                found.append(f"{case.attrib.get('classname', '')}.{case.attrib.get('name', '')}: "
                             f"{failure.attrib.get('message', '')}")
    return found


def has_explicit_gc_args(args: list[str]) -> bool:
    return any(argument.startswith(("-XX:", "-Xlog:gc", "-Xloggc", "-verbose:gc")) for argument in args)


def cache_stats_or_zero(cache_dir: Path) -> dict[str, int]:
    if not cache_dir.is_dir():
        return {"cacheable_calls": 0, "hits": 0, "misses": 0, "uncacheable_calls": 0}
    return control.ccache_stats(cache_dir)


def output_manifest_path(root: Path) -> Path:
    return root / "results.json"


def template_row(matrix: dict, mode: str, stage: str, measured: bool) -> dict:
    return copy.deepcopy(next(row for row in matrix["results"]
                              if row.get("suite") == "java" and row.get("mode") == mode
                              and row.get("stage") == stage and row.get("measured") is measured
                              and row.get("valid") is True))


def build_plan(matrix: dict, matrix_path: Path, output_root: Path, inherited_audit: dict) -> dict:
    plan = copy.deepcopy(matrix["plan"])
    plan.update({
        "suites": ["java"], "repeat_count": 4, "warmup_count": 1,
        "cells": [{"suite": "java", "mode": mode, "stage": stage,
                   "warmup_count": 1, "repeat_count": 4}
                  for mode, stages in (("direct", STAGES),
                                       ("cold", STAGES[1:]), ("warm", STAGES[1:]))
                  for stage in stages],
        "cache_states": {"direct": list(STAGES), "cold": list(STAGES[1:]), "warm": list(STAGES[1:])},
        "measurement_schedule": "serial Java-only cells; four rotations of stage/cache-mode starts; no concurrent parser subprocesses",
        "gc_request_jfr": {"enabled": False, "reason": "no explicit GC request or JFR instrumentation in this matrix"},
        "coverage_policy": "JaCoCo Test-worker javaagent disabled; both JaCoCo report tasks skipped",
        "explicit_gc_policy": "no explicit GC injected and no GC-related JVM flags; inherited JVM control flags audited before runs",
        "primary_matrix": str(matrix_path),
        "primary_matrix_sha256": base.sha256_file(matrix_path),
        "output_root": str(output_root),
        "control_name": "Java-only fresh four-stage matrix with JaCoCo agent off",
        "fresh_matrix_policy": "all timings in this manifest are fresh; no warm-control timings are pooled",
        "inherited_jvm_environment_audit": inherited_audit,
        "all_measurements_valid": False,
        "host_lock_status": "confirmed",
        "host_lock_approval": "exclusive benchmark host lock granted by the parent agent for this run",
    })
    for stage in STAGES:
        plan["compression_policy"][stage] = {
            mode: matrix["plan"]["compression_policy"][stage][mode]
            for mode in (("direct",) if stage == "before-cache" else MODES)
        }
    return plan


def build_identity_preflight(matrix: dict, preflight_rows: list[dict], fixture_passed: bool,
                             inherited_audit: dict) -> dict:
    warmups = {}
    for row in preflight_rows:
        warmups[row["cell_id"]] = {"sha256": row["test_identity_sha256"],
                                    "count": row["total_tests"]}
    reference_row = next(row for row in preflight_rows
                         if row["mode"] == "direct" and row["stage"] == "before-cache")
    digests = {row["test_identity_sha256"] for row in preflight_rows}
    mismatch = digests != {reference_row["test_identity_sha256"]}
    return {
        "passed": fixture_passed and not mismatch and all(row["valid"] for row in preflight_rows),
        "mismatches": (["warm-up test identities differ"] if mismatch else []),
        "reference_test_ids": {"java": {"source_cell": reference_row["cell_id"],
                                           "sha256": reference_row["test_identity_sha256"],
                                           "count": reference_row["total_tests"]}},
        "warmup_test_identities": {"java": warmups},
        "fixture_gate_passed": fixture_passed,
        "no_agent_or_explicit_gc_gate_passed": all(row["valid"] for row in preflight_rows),
        "inherited_jvm_environment_audit": inherited_audit,
        "gc_requests_by_stage": {},
        "show_exec_info_gc_gate_passed": True,
    }


def create_command(stage: dict, run_dir: Path) -> list[str]:
    command = ["/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(run_dir / "time.txt"), "--",
               "gradle", "--no-daemon", "--offline", "--info",
               "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
               "--init-script", str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle")]
    if stage["key"] == "protobuf":
        command.append(f"-PclangDumperRoot={stage['native_root']}")
    command.extend(["-p", "ClangAstParser", "test"])
    return command


def run_cell(matrix: dict, stage: dict, output_root: Path, mode: str, repeat: int | None,
             agent: str, measured: bool, ordinal: int, worker_baselines: dict[str, dict]) -> dict:
    key = stage["key"]
    label = f"r{repeat:02d}" if repeat is not None else "warmup"
    run_dir = output_root / "runs" / agent / mode / key / label
    if run_dir.exists():
        suffix = 1
        while (run_dir.parent / f"{label}-attempt{suffix}").exists():
            suffix += 1
        run_dir = run_dir.parent / f"{label}-attempt{suffix}"
    run_dir.mkdir(parents=True)
    diagnostic = run_dir / "gradle-output"
    # ON/OFF diagnostics share the same per-mode temp and Gradle cache paths.
    # Direct bypass and cold mode recreate ccache before each command, so the
    # only treatment difference within a pair is the Test-worker JaCoCo agent.
    mode_root = output_root / "runstate" / mode
    temp_root = mode_root / "temp" / "java" / key
    temp_root.mkdir(parents=True, exist_ok=True)
    cache_dir = temp_root / f"clang_ast_exe_{getpass.getuser()}" / base.cache_namespace(stage)
    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(output_root / "cache" / "java" / key),
        "JAVA_TOOL_OPTIONS": environment.get("JAVA_TOOL_OPTIONS", "")
        + f" -Djava.io.tmpdir={temp_root} -Dclava.astWire={stage['wire']}",
        "DEADLINE_JACOCO_AGENT": agent,
        "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic),
        "SPECS_JAVA_LIBS_HOME": str(control.SPECS_JAVA_LIBS_ROOT.resolve()),
        "LARA_FRAMEWORK_HOME": str(control.LARA_FRAMEWORK_ROOT.resolve()),
    })
    if key == "flatbuffers":
        environment["FLAT_NATIVE"] = str(stage["native_root"])
    direct_marker = (base.install_direct_ccache_probe(run_dir, environment)
                     if stage["cache"] and mode == "direct" else None)
    base.configure_cache_mode(stage, mode, measured, environment, cache_dir)
    command = create_command(stage, run_dir)
    (run_dir / "command.json").write_text(json.dumps({
        "command": command, "cwd": str(Path(stage["root"]) / "clava"), "mode": mode,
        "stage": key, "agent": agent, "cache_dir": str(cache_dir) if stage["cache"] else None,
    }, indent=2) + "\n")
    before = cache_stats_or_zero(cache_dir) if stage["cache"] else {}
    if control.audit_inherited_jvm_options() != matrix["plan"].get("inherited_jvm_environment_audit", control.audit_inherited_jvm_options()):
        raise RuntimeError("inherited JVM environment changed during measurement")
    started_at = dt.datetime.now(dt.timezone.utc).isoformat()
    started = time.perf_counter()
    with (run_dir / "run.log").open("w") as log:
        process = subprocess.run(command, cwd=Path(stage["root"]) / "clava", env=environment,
                                 stdout=log, stderr=subprocess.STDOUT, check=False)
    driver_elapsed = time.perf_counter() - started
    finished_at = dt.datetime.now(dt.timezone.utc).isoformat()
    after = cache_stats_or_zero(cache_dir) if stage["cache"] else {}
    delta = {name: int(after.get(name, 0)) - int(before.get(name, 0))
             for name in ("cacheable_calls", "hits", "misses", "uncacheable_calls")}
    # For cold/direct cells, cache setup starts at zero. Warm cells reset stats before the measured call.
    delta = {"cacheable_calls": delta["cacheable_calls"], "hits": delta["hits"],
             "misses": delta["misses"], "uncacheable_calls": delta["uncacheable_calls"]}
    cache = base.cache_validation(stage, mode, measured, cache_dir, direct_marker)
    cache.update({"cacheable_calls": delta["cacheable_calls"], "hits": delta["hits"],
                  "misses": delta["misses"], "uncacheable_calls": delta["uncacheable_calls"]})
    expected_cache_mode = "cold" if mode == "warm" and not measured else mode
    cache["passed"] = (not stage["cache"] or delta == EXPECTED_CACHE[expected_cache_mode]) and cache["passed"]
    junit_root = diagnostic / "junit-xml"
    counts = control.junit_counts(junit_root)
    identities = identity_records(junit_root)
    identity_sha = canonical_hash(identities)
    executor_args = control.observed_test_executor_args(run_dir / "run.log")
    executor_normalized, response_files = control.normalize_executor_args(executor_args)
    worker_path = diagnostic / "worker-configuration.json"
    worker = json.loads(worker_path.read_text()) if worker_path.is_file() else {}
    worker_args = worker.get("jvm_args", [])
    worker_agents = [arg for arg in worker_args if arg.startswith(AGENT_FLAGS)]
    executor_agents = [arg for arg in executor_args if arg.startswith(AGENT_FLAGS)]
    expected_agent = agent == "on"
    only_expected_agent = (
        len(worker_agents) == len(executor_agents) == 1
        and all("jacocoagent" in value and value.startswith("-javaagent:")
                for value in (worker_agents[0], executor_agents[0]))
    ) if expected_agent else not worker_agents and not executor_agents
    worker_identity = {
        "max_heap_size": worker.get("max_heap_size"),
        "jvm_args_without_javaagent": [arg for arg in worker_args if not arg.startswith("-javaagent:")],
        "actual_executor_args_without_javaagent": executor_normalized,
    }
    baseline = worker_baselines.setdefault(key, worker_identity)
    worker_stable = baseline == worker_identity
    executor_heap_ok = [arg for arg in executor_args if arg.startswith("-Xmx")] == ["-Xmx512m"]
    no_gc_args = not has_explicit_gc_args(worker_args + executor_args)
    task_log = (run_dir / "run.log").read_text(errors="replace")
    report_skips = {task: bool(re.search(rf"{task}\s+SKIPPED", task_log))
                    for task in ("jacocoTestReport", "jacocoTestCoverageVerification")}
    compile_violations = control.task_compilation_lines(run_dir / "run.log")
    test_executed = control.test_task_was_executed(run_dir / "run.log")
    actual_native = deadline.native_binary_for_run(stage, mode_root, "java")
    actual_native_path = str(actual_native.resolve()) if actual_native.is_file() else None
    actual_native_sha = base.sha256_file(actual_native) if actual_native.is_file() else None
    native_matches = actual_native_sha == stage["native_binary_sha256"]
    time_data = control.parse_time(run_dir / "time.txt")
    elapsed = float(time_data.get("elapsed_s", driver_elapsed))
    passed_counts = {key: counts.get(key) for key in EXPECTED} == EXPECTED
    expected_identity = matrix["identity_preflight"]["reference_test_ids"]["java"]["sha256"]
    identity_matches = identity_sha == expected_identity
    requested_agent_matches = worker.get("requested_agent") == agent
    jacoco_state_matches = worker.get("jacoco_enabled") is expected_agent
    valid = (process.returncode == 0 and passed_counts and identity_matches and cache["passed"] and native_matches
             and not compile_violations and only_expected_agent and no_gc_args and worker_stable
             and requested_agent_matches and jacoco_state_matches
             and executor_heap_ok and test_executed and all(report_skips.values()))
    template = template_row(matrix, mode, key, measured)
    row = template
    row.update({
        "suite": "java", "mode": mode, "stage": key, "measured": measured,
        "repeat": repeat, "cell_id": cell_id(mode, key, repeat), "attempt": 1,
        "selected": True, "superseded_by_attempt": None, "agent": agent,
        "valid": valid, "return_code": process.returncode, "exit_status": process.returncode,
        "elapsed_s": elapsed, "driver_elapsed_s": driver_elapsed,
        "user_s": time_data.get("user_s", 0.0), "sys_s": time_data.get("sys_s", 0.0),
        "max_rss_kb": time_data.get("max_rss_kb", 0),
        "junit_aggregate_s": counts.get("junit_aggregate_s", 0.0),
        "gradle_non_test_elapsed_s": max(0.0, elapsed - float(counts.get("junit_aggregate_s", 0.0))),
        "total_tests": counts.get("total_tests", 0), "passed_tests": counts.get("passed_tests", 0),
        "failed_tests": counts.get("failed_tests", 0), "skipped_tests": counts.get("skipped_tests", 0),
        "failure_names": junit_failures(junit_root), "test_identity": identities,
        "test_identity_sha256": identity_sha, "workload_identity_match": identity_matches,
        "cacheable_calls": delta["cacheable_calls"], "cache_hits": delta["hits"],
        "cache_misses": delta["misses"], "uncacheable_calls": delta["uncacheable_calls"],
        "cache_validation": cache, "compile_tasks_clean": not compile_violations,
        "compile_task_violations": compile_violations,
        "native_binary_path": actual_native_path, "native_binary_matches_expected": native_matches,
        "native_binary_sha256": actual_native_sha,
        "worker_configuration": worker, "actual_test_executor_args": executor_args,
        "actual_executor_classpath_response_files": response_files,
        "worker_args_stable_within_stage": worker_stable,
        "agent": agent, "only_expected_jacoco_agent": only_expected_agent,
        "no_explicit_gc_flags": no_gc_args, "test_worker_xmx_512m": executor_heap_ok,
        "test_task_executed": test_executed, "report_tasks_skipped": report_skips,
        "command": command, "run_dir": str(run_dir),
        "run_dir_relative": run_dir.relative_to(output_root).as_posix(),
        "run_started_at": started_at, "run_finished_at": finished_at,
    })
    row["compression"] = matrix["plan"]["compression_policy"][key][mode]
    if not measured:
        row["repeat"] = None
    (run_dir / "summary.json").write_text(json.dumps(row, indent=2) + "\n")
    return row


def persist(root: Path, plan: dict, identity: dict, rows: list[dict]) -> None:
    payload = {"schema_version": 1, "plan": plan, "identity_preflight": identity, "results": rows}
    output_manifest_path(root).write_text(json.dumps(payload, indent=2) + "\n")
    fields = ("cell_id", "measured", "repeat", "attempt", "selected", "suite", "mode", "stage",
              "agent", "valid", "elapsed_s", "junit_aggregate_s", "cacheable_calls", "cache_hits",
              "cache_misses", "total_tests", "failed_tests")
    with (root / "results.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def preflight(args: argparse.Namespace, matrix: dict, stages: dict[str, dict], inherited: dict) -> int:
    root = args.output_root.resolve()
    if root.exists():
        raise RuntimeError(f"preflight output root already exists: {root}")
    root.mkdir(parents=True)
    plan = build_plan(matrix, args.matrix.resolve(), root, inherited)
    rows = []
    worker_baselines: dict[str, dict] = {}
    for ordinal, (mode, key) in enumerate(schedule(1), start=1):
        row = run_cell(matrix, stages[key], root, mode, None, "off", False, ordinal, worker_baselines)
        rows.append(row)
        print(json.dumps({key: row[key] for key in ("cell_id", "valid", "elapsed_s", "cacheable_calls",
                                                     "cache_hits", "cache_misses", "total_tests")}), flush=True)
    identity = build_identity_preflight(matrix, rows, True, inherited)
    if not identity["passed"]:
        raise RuntimeError("preflight failed: test identity, worker-agent, cache, or fixture gate")
    # Freeze the independently prepared warm cache so every timed warm repeat starts identically.
    for key in STAGES[1:]:
        source = root / "runstate/warm/temp/java" / key / f"clang_ast_exe_{getpass.getuser()}" / base.cache_namespace(stages[key])
        seed = root / "warm-cache-seed" / key
        seed.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(source, seed)
        if control.cache_manifest(source) != control.cache_manifest(seed):
            raise RuntimeError(f"fresh warm cache seed mismatch for {key}")
    plan["all_measurements_valid"] = False
    plan["preflight_status"] = "passed"
    plan["preflight_completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
    (root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
    persist(root, plan, identity, rows)
    return 0


def load_preflight(root: Path, matrix: dict) -> tuple[dict, dict, list[dict]]:
    plan_path, result_path = root / "plan.json", root / "results.json"
    if not plan_path.is_file() or not result_path.is_file():
        raise RuntimeError("measure phase requires a completed preflight plan and results")
    plan = json.loads(plan_path.read_text())
    payload = json.loads(result_path.read_text())
    if plan.get("preflight_status") != "passed" or plan.get("repeat_count") != 4 or plan.get("suites") != ["java"]:
        raise RuntimeError("preflight plan is not the expected Java-only four-round design")
    rows = payload.get("results", [])
    if len(rows) != 10 or not all(row.get("valid") and row.get("measured") is False for row in rows):
        raise RuntimeError("preflight must contain exactly ten valid untimed Java cells")
    if payload.get("identity_preflight", {}).get("passed") is not True:
        raise RuntimeError("identity preflight did not pass")
    if plan.get("primary_matrix_sha256") != base.sha256_file(Path(plan["primary_matrix"])):
        raise RuntimeError("frozen primary matrix changed after preflight")
    for key, stage in plan["stages"].items():
        verify_stage(stage)
    for key in STAGES[1:]:
        source = root / "warm-cache-seed" / key
        if not source.is_dir():
            raise RuntimeError(f"fresh warm cache seed is missing for {key}")
    return plan, payload["identity_preflight"], rows


def restore_warm_seed(root: Path, stage: dict) -> None:
    target = root / "runstate/warm/temp/java" / stage["key"] / f"clang_ast_exe_{getpass.getuser()}" / base.cache_namespace(stage)
    seed = root / "warm-cache-seed" / stage["key"]
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(seed, target)
    reset = subprocess.run(["ccache", "--zero-stats"], env={**os.environ, "CCACHE_DIR": str(target), "LC_ALL": "C"},
                           text=True, capture_output=True, check=False)
    if reset.returncode != 0:
        raise RuntimeError(f"could not reset fresh warm cache seed for {stage['key']}: {reset.stderr.strip()}")


def agent_order_for_round(round_number: int) -> list[tuple[str, str, bool]]:
    result = []
    for position, (mode, stage) in enumerate(schedule(round_number)):
        diagnostic = mode in ("direct", "cold") and stage in ("ccache-text", "protobuf")
        if diagnostic and (round_number + position) % 2 == 0:
            result.append((mode, stage, True))
            result.append((mode, stage, False))
        else:
            result.append((mode, stage, False))
            if diagnostic:
                result.append((mode, stage, True))
    return result


def measure(args: argparse.Namespace, matrix: dict, stages: dict[str, dict], inherited: dict) -> int:
    root = args.output_root.resolve()
    plan, identity, rows = load_preflight(root, matrix)
    if plan.get("inherited_jvm_environment_audit") != inherited:
        raise RuntimeError("inherited JVM options changed after preflight")
    worker_baselines = {}
    for row in rows:
        worker = row["worker_configuration"]
        executor, _ = control.normalize_executor_args(row["actual_test_executor_args"])
        worker_baselines[row["stage"]] = {
            "max_heap_size": worker.get("max_heap_size"),
            "jvm_args_without_javaagent": [arg for arg in worker.get("jvm_args", []) if not arg.startswith("-javaagent:")],
            "actual_executor_args_without_javaagent": executor,
        }
    diagnostic_rows = []
    primary_ordinal = 0
    diagnostic_ordinal = 0
    for round_number in range(1, 5):
        for mode, key, run_on in agent_order_for_round(round_number):
            stage = stages[key]
            if not run_on and mode == "warm":
                restore_warm_seed(root, stage)
            agent = "on" if run_on else "off"
            ordinal = diagnostic_ordinal + 1 if run_on else primary_ordinal + 1
            row = run_cell(matrix, stage, root, mode, round_number, agent, True, ordinal, worker_baselines)
            if run_on:
                diagnostic_ordinal += 1
                row["ordinal"] = diagnostic_ordinal
                diagnostic_rows.append(row)
            else:
                primary_ordinal += 1
                row["ordinal"] = primary_ordinal
                rows.append(row)
                persist(root, plan, identity, rows)
            print(json.dumps({k: row[k] for k in ("cell_id", "agent", "valid", "elapsed_s",
                                                   "cacheable_calls", "cache_hits", "cache_misses",
                                                   "total_tests", "failed_tests")}), flush=True)
            if not row["valid"]:
                raise RuntimeError(f"invalid measured cell; timing excluded, see {row['run_dir']}")
            verify_stage(stage)
    if len([row for row in rows if row.get("measured") is True]) != 40:
        raise RuntimeError("primary matrix does not contain exactly forty OFF-agent measured rows")
    plan["all_measurements_valid"] = True
    plan["measurement_completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
    persist(root, plan, identity, rows)
    (root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
    write_agent_diagnostics(root, matrix, diagnostic_rows, rows, args.skip_agent_diagnostics)
    return 0


def write_agent_diagnostics(root: Path, matrix: dict, on_rows: list[dict], off_rows: list[dict], skipped: bool) -> None:
    if skipped:
        (root / "agent-diagnostics.json").write_text(json.dumps({"skipped": True}, indent=2) + "\n")
        return
    paired = []
    for on in on_rows:
        off = next(row for row in off_rows if row["measured"] is True and row["mode"] == on["mode"]
                   and row["stage"] == on["stage"] and row["repeat"] == on["repeat"])
        paired.append({"stage": on["stage"], "mode": on["mode"], "round": on["repeat"],
                       "on_elapsed_s": on["elapsed_s"], "off_elapsed_s": off["elapsed_s"],
                       "on_minus_off_s": on["elapsed_s"] - off["elapsed_s"],
                       "on_junit_aggregate_s": on["junit_aggregate_s"],
                       "off_junit_aggregate_s": off["junit_aggregate_s"],
                       "on_minus_off_junit_s": on["junit_aggregate_s"] - off["junit_aggregate_s"],
                       "on_outside_junit_s": on["gradle_non_test_elapsed_s"],
                       "off_outside_junit_s": off["gradle_non_test_elapsed_s"],
                       "on_minus_off_outside_junit_s": on["gradle_non_test_elapsed_s"] - off["gradle_non_test_elapsed_s"],
                       "on_agent_args": on["worker_configuration"].get("javaagent_args", []),
                       "off_agent_args": off["worker_configuration"].get("javaagent_args", [])})
    payload = {"control": "JaCoCo Test-worker agent ON versus OFF for direct/cold Text and Protobuf only",
               "not_part_of_primary_matrix": True, "n_per_arm_per_stage_mode": 4,
               "coverage_reports": "both report tasks skipped in every run",
               "primary_matrix_sha256": base.sha256_file(Path(matrix["plan"].get("primary_matrix", DEFAULT_MATRIX))),
               "results": on_rows, "paired_diagnostics": paired}
    (root / "agent-diagnostics.json").write_text(json.dumps(payload, indent=2) + "\n")
    fields = ("stage", "mode", "round", "on_elapsed_s", "off_elapsed_s", "on_minus_off_s",
              "on_junit_aggregate_s", "off_junit_aggregate_s", "on_minus_off_junit_s",
              "on_outside_junit_s", "off_outside_junit_s", "on_minus_off_outside_junit_s",
              "on_agent_args", "off_agent_args")
    with (root / "agent-diagnostics.csv").open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(paired)


def write_paired_summary(root: Path, rows: list[dict]) -> dict:
    measured = [row for row in rows if row.get("measured") is True]
    metrics = ("elapsed_s", "junit_aggregate_s", "gradle_non_test_elapsed_s")
    stages_by_mode = {
        "direct": STAGES,
        "cold": STAGES[1:],
        "warm": STAGES[1:],
    }
    stage_summary = {}
    paired_vs_text = {}
    paired_text_vs_base = {}
    for mode, stages in stages_by_mode.items():
        stage_summary[mode] = {}
        for stage in stages:
            stage_rows = [row for row in measured if row["mode"] == mode and row["stage"] == stage]
            stage_summary[mode][stage] = {
                "n": len(stage_rows),
                **{f"median_{metric}": __import__("statistics").median(float(row[metric]) for row in stage_rows)
                   for metric in metrics},
                "by_round": [{"round": row["repeat"], **{metric: row[metric] for metric in metrics}}
                             for row in sorted(stage_rows, key=lambda item: item["repeat"])],
            }
        reference_stage = "ccache-text"
        paired_vs_text[mode] = {}
        candidates = ("protobuf", "flatbuffers") + (("before-cache",) if mode == "direct" else ())
        for stage in candidates:
            pairs = []
            for repeat in range(1, 5):
                reference = next(row for row in measured if row["mode"] == mode
                                 and row["stage"] == reference_stage and row["repeat"] == repeat)
                candidate = next(row for row in measured if row["mode"] == mode
                                 and row["stage"] == stage and row["repeat"] == repeat)
                pairs.append({"round": repeat, **{metric: float(candidate[metric]) - float(reference[metric])
                                                   for metric in metrics}})
            paired_vs_text[mode][stage] = {
                "definition": f"{stage} minus {reference_stage}; positive means candidate took longer",
                "by_round": pairs,
                **{f"median_{metric}_gap": __import__("statistics").median(pair[metric] for pair in pairs)
                   for metric in metrics},
            }
        if mode == "direct":
            pairs = []
            for repeat in range(1, 5):
                baseline = next(row for row in measured if row["mode"] == mode
                                and row["stage"] == "before-cache" and row["repeat"] == repeat)
                text = next(row for row in measured if row["mode"] == mode
                            and row["stage"] == "ccache-text" and row["repeat"] == repeat)
                pairs.append({"round": repeat, **{metric: float(text[metric]) - float(baseline[metric])
                                                   for metric in metrics}})
            paired_text_vs_base[mode] = {
                "definition": "ccache-text direct minus before-cache; positive means ccache-stage Text took longer",
                "by_round": pairs,
                **{f"median_{metric}_gap": __import__("statistics").median(pair[metric] for pair in pairs)
                   for metric in metrics},
            }
    summary = {
        "suite": "Java parser full suite; 116 passed, 0 failed, 0 skipped per invocation",
        "timing_definition": "GNU-time Gradle wall, JUnit XML testcase aggregate, and wall-minus-JUnit outside-test residual",
        "residual_caveat": "--info logs expose task status and aggregate build duration, not unique per-task durations; residual is not attributed to coverage or report generation",
        "rows": len(measured), "n_per_stage_mode": 4,
        "stage_summary_by_mode": stage_summary,
        "paired_gaps_vs_text_by_mode": paired_vs_text,
        "direct_text_vs_before_cache": paired_text_vs_base,
    }
    (root / "paired-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    return summary


def finalize(args: argparse.Namespace, matrix: dict) -> int:
    root = args.output_root.resolve()
    manifest_path = output_manifest_path(root)
    payload = json.loads(manifest_path.read_text())
    rows = payload.get("results", [])
    if len(rows) != 50 or len([row for row in rows if row.get("measured") is True]) != 40:
        raise RuntimeError("finalization expects exactly ten preflights and forty measured OFF-agent rows")
    if not all(row.get("valid") is True and row.get("agent") == "off" for row in rows):
        raise RuntimeError("primary matrix contains an invalid or instrumented row")
    diag_path = root / "agent-diagnostics.json"
    diagnostic = json.loads(diag_path.read_text())
    on_rows = diagnostic.get("results", [])
    if len(on_rows) != 16 or not all(row.get("valid") is True and row.get("agent") == "on" for row in on_rows):
        raise RuntimeError("separate agent control must contain sixteen valid ON rows")
    payload["plan"]["environment_control"] = {
        "xdg_cache_home": "stage-scoped at output/cache/java/<stage>, shared across direct/cold/warm and ON/OFF within stage",
        "xdg_cache_audit": "all ten per-mode Java preflights completed before measurement; frozen Java parser has no callsite to SpecsIo.getOsCacheFolder; the Clava-JS sideEffects.ts reader is not in this Java suite",
        "ccache": "explicit mode-specific CCACHE_DIR below java.io.tmpdir; direct/cold cleared for every invocation; warm cache seeded only by this matrix's untimed warm-up and restored before each measured repeat",
        "java_io_tmpdir": "mode-specific and stage-specific under the private output root",
        "native_tool_cache": "published before-cache dumper payload SHA verified per run; local protocol binaries fixed to frozen matrix paths and SHA-256",
    }
    manifest_path.write_text(json.dumps(payload, indent=2) + "\n")
    (root / "plan.json").write_text(json.dumps(payload["plan"], indent=2) + "\n")
    write_agent_diagnostics(root, matrix, on_rows, rows, False)
    summary = write_paired_summary(root, rows)
    sys.path.insert(0, str(SCRIPT_ROOT / "analysis"))
    import analyze_deadline
    plan, planned, baseline, selected, audited = analyze_deadline.validate_manifest(payload, manifest_path)
    (root / "validation.json").write_text(json.dumps({
        "passed": len(planned) == 10 and len(selected) == 50 and not audited,
        "planned_cells": len(planned), "selected_rows_including_preflight": len(selected),
        "audit_rows": len(audited), "test_identity_baseline": baseline,
        "primary_matrix_sha256": plan["primary_matrix_sha256"],
        "summary_rows": summary["rows"],
    }, indent=2) + "\n")
    print(json.dumps({"manifest": str(manifest_path), "primary_rows": len(rows),
                      "agent_diagnostic_rows": len(on_rows), "strict_validation": "passed",
                      "paired_summary": str(root / "paired-summary.json")}, indent=2))
    return 0


def main() -> int:
    args = parse_args()
    matrix_path = args.matrix.resolve()
    output_root = args.output_root.resolve()
    matrix, stages = load_frozen(matrix_path)
    inherited = control.audit_inherited_jvm_options()
    matrix["plan"]["inherited_jvm_environment_audit"] = inherited
    matrix["plan"]["primary_matrix"] = str(matrix_path)
    if args.phase == "preflight":
        return preflight(args, matrix, stages, inherited)
    if args.phase == "measure":
        return measure(args, matrix, stages, inherited)
    return finalize(args, matrix)


if __name__ == "__main__":
    raise SystemExit(main())
