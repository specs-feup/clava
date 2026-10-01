#!/usr/bin/env python3
"""Collect a bounded, non-headline Java Gradle/test runtime timeline."""

from __future__ import annotations

import argparse
import csv
import datetime as dt
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

sys.dont_write_bytecode = True

import run_comparison as base
import run_deadline_matrix as deadline
import run_java_jacoco_agent_control as control


SCRIPT_ROOT = Path(__file__).resolve().parent
MATRIX_PATH = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/results.json"
WORKER_EVIDENCE_PATH = (
    SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-noagent-matrix-r2/results.json"
)
OUTPUT_ROOT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-runtime-timeline-r2"
EXPECTED = control.EXPECTED_JAVA
EXPECTED_WARM_CACHE = control.EXPECTED_PRIMARY_CACHE
EXPECTED_COLD_CACHE = {
    "cacheable_calls": 208,
    "hits": 0,
    "misses": 208,
    "uncacheable_calls": 0,
}
STAGES = ("ccache-text", "protobuf")
SCHEDULE = ((1, "ccache-text"), (1, "protobuf"), (2, "protobuf"), (2, "ccache-text"))
TIMELINE_INIT = SCRIPT_ROOT / "java-runtime-timeline.init.gradle"
JAVA_SYSTEM_PROPERTIES = ("-Djava.io.tmpdir=", "-Dclava.astWire=")
GC_FLAGS = ("-XX:", "-Xlog:gc", "-Xloggc", "-verbose:gc")
AGENT_FLAGS = ("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def sha256_file(path: Path) -> str:
    return base.sha256_file(path)


def utc_from_ns(epoch_ns: int | None) -> str | None:
    if epoch_ns is None:
        return None
    return dt.datetime.fromtimestamp(epoch_ns / 1_000_000_000, dt.timezone.utc).isoformat()


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def load_and_verify(matrix_path: Path, worker_evidence_path: Path
                    ) -> tuple[dict, dict[str, dict], dict, dict[str, list[dict]]]:
    matrix = control.load_matrix(matrix_path)
    worker_evidence = json.loads(worker_evidence_path.read_text(encoding="utf-8"))
    require(worker_evidence.get("identity_preflight", {}).get("passed") is True,
            "completed Java no-agent matrix identity preflight did not pass")
    require(worker_evidence.get("plan", {}).get("suites") == ["java"],
            "worker evidence is not the Java-only no-agent matrix partition")
    stages = {key: matrix["plan"]["stages"][key] for key in STAGES}
    identity = matrix["identity_preflight"]["reference_test_ids"]["java"]
    require(identity.get("count") == 116, "frozen Java identity does not contain 116 tests")

    expected_fixtures = matrix["plan"]["fixture_fingerprints"]
    observed_fixtures = deadline.fixture_maps([stages[key] for key in STAGES])
    expected_subset = {
        category: {key: expected_fixtures[category][key] for key in STAGES}
        for category in expected_fixtures
    }
    require(observed_fixtures == expected_subset,
            "Text/Protobuf source or resource fixture fingerprints changed")

    warm_rows = {
        key: [row for row in worker_evidence["results"]
              if row.get("suite") == "java" and row.get("mode") == "warm"
              and row.get("measured") is True and row.get("stage") == key]
        for key in STAGES
    }
    for key, rows in warm_rows.items():
        require(len(rows) == 4, f"expected four frozen warm rows for {key}")
        for row in rows:
            require(row.get("valid") is True and row.get("agent") == "off" and
                    {field: row.get(field) for field in EXPECTED} == EXPECTED,
                    f"frozen no-agent warm row is not 116/116 valid for {key}")
            require(row.get("test_identity_sha256") == identity["sha256"],
                    f"frozen no-agent warm test identity changed for {key}")
            cache = {
                "cacheable_calls": int(row.get("cacheable_calls", 0)),
                "hits": int(row.get("cache_hits", 0)),
                "misses": int(row.get("cache_misses", 0)),
                "uncacheable_calls": int(
                    row.get("cache_validation", {}).get("uncacheable_calls", 0)
                ),
            }
            require(cache == EXPECTED_WARM_CACHE,
                    f"frozen no-agent warm cache changed for {key}: {cache}")
            executor_args = row.get("actual_test_executor_args", [])
            require("-Xmx512m" in executor_args and
                    not any(argument.startswith(AGENT_FLAGS) for argument in executor_args) and
                    not any(argument.startswith(GC_FLAGS) for argument in executor_args),
                    f"frozen no-agent warm worker flags changed for {key}")
            require(all(row.get("report_tasks_skipped", {}).get(task) is True
                        for task in ("jacocoTestReport", "jacocoTestCoverageVerification")),
                    f"frozen no-agent warm JaCoCo report-task policy changed for {key}")
            primary_rows = [primary for primary in matrix["results"]
                            if primary.get("suite") == "java"
                            and primary.get("mode") == "warm"
                            and primary.get("measured") is True
                            and primary.get("stage") == key]
            require(bool(primary_rows) and all(
                all(row.get(field) == primary_rows[0].get(field) for field in (
                    "clava_revision", "clava_patch_sha256", "native_revision",
                    "native_binary_sha256", "parser_jar_sha256", "runtime_manifest_sha256",
                    "test_identity_sha256", "fixture_fingerprint_sha256",
                )) for row in primary_rows
            ), f"primary stage provenance is internally inconsistent for {key}")
            require(all(
                all(row.get(field) == primary_rows[0].get(field) for field in (
                    "clava_revision", "clava_patch_sha256", "native_revision",
                    "native_binary_sha256", "parser_jar_sha256", "runtime_manifest_sha256",
                    "test_identity_sha256", "fixture_fingerprint_sha256",
                )) for row in rows
            ), f"no-agent worker evidence stage does not match frozen provenance for {key}")

    cold_rows = {
        key: [row for row in worker_evidence["results"]
              if row.get("suite") == "java" and row.get("mode") == "cold"
              and row.get("measured") is True and row.get("stage") == key]
        for key in STAGES
    }
    for key, rows in cold_rows.items():
        require(len(rows) == 4, f"expected four frozen cold rows for {key}")
        for row in rows:
            cache = {
                "cacheable_calls": int(row.get("cacheable_calls", 0)),
                "hits": int(row.get("cache_hits", 0)),
                "misses": int(row.get("cache_misses", 0)),
                "uncacheable_calls": int(
                    row.get("cache_validation", {}).get("uncacheable_calls", 0)
                ),
            }
            require(row.get("valid") is True and row.get("agent") == "off"
                    and {field: row.get(field) for field in EXPECTED} == EXPECTED
                    and row.get("test_identity_sha256") == identity["sha256"]
                    and cache == EXPECTED_COLD_CACHE
                    and "-Xmx512m" in row.get("actual_test_executor_args", [])
                    and not any(arg.startswith(AGENT_FLAGS)
                                for arg in row.get("actual_test_executor_args", []))
                    and not any(arg.startswith(GC_FLAGS)
                                for arg in row.get("actual_test_executor_args", []))
                    and all(row.get("report_tasks_skipped", {}).get(task) is True
                            for task in ("jacocoTestReport", "jacocoTestCoverageVerification")),
                    f"frozen no-agent cold evidence gate failed for {key}: {row.get('cell_id')}")

    return matrix, stages, {
        "expected_test_identity_sha256": identity["sha256"],
        "expected_test_count": identity["count"],
        "fixture_fingerprints": {
            category: {
                key: control.canonical_hash(expected_subset[category][key])
                for key in STAGES
            }
            for category in expected_subset
        },
    }, cold_rows


def inherited_java_options() -> tuple[list[str], dict]:
    inherited = os.environ.get("JAVA_TOOL_OPTIONS", "")
    try:
        tokens = shlex.split(inherited)
    except ValueError as error:
        raise RuntimeError(f"cannot parse inherited JAVA_TOOL_OPTIONS: {error}") from error
    duplicates = [token for token in tokens
                  if token.startswith(JAVA_SYSTEM_PROPERTIES)]
    require(not duplicates,
            "inherited JAVA_TOOL_OPTIONS already defines a timeline-owned system property")
    audit = control.audit_inherited_jvm_options()
    require(all(not audit[name]["heap_gc_agent_flags"]
                for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS")),
            "inherited JVM environment has a heap/GC/agent override")
    return tokens, {
        name: {
            "present": audit[name]["present"],
            "argument_count": audit[name]["argument_count"],
            "heap_gc_agent_flags": audit[name]["heap_gc_agent_flags"],
        }
        for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS")
    }


def create_environment(stage: dict, run_dir: Path, cache_dir: Path,
                       inherited_options: list[str]) -> dict[str, str]:
    key = stage["key"]
    temp_root = run_dir / "tmp"
    xdg_root = run_dir / "xdg-cache"
    diagnostic_root = run_dir / "gradle-output"
    temp_root.mkdir(parents=True)
    xdg_root.mkdir(parents=True)
    diagnostic_root.mkdir(parents=True)
    properties = [f"-Djava.io.tmpdir={temp_root}", f"-Dclava.astWire={stage['wire']}"]
    options = inherited_options + properties
    require(len([arg for arg in options if arg.startswith(JAVA_SYSTEM_PROPERTIES[0])]) == 1
            and len([arg for arg in options if arg.startswith(JAVA_SYSTEM_PROPERTIES[1])]) == 1,
            "timeline JAVA_TOOL_OPTIONS would repeat a fixed system property")

    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root),
        "TMP": str(temp_root),
        "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(xdg_root),
        "JAVA_TOOL_OPTIONS": shlex.join(options),
        "CCACHE_DIR": str(cache_dir),
        "DEADLINE_JACOCO_AGENT": "off",
        "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic_root),
        "DEADLINE_JAVA_TIMELINE_PATH": str(run_dir / "gradle-events.jsonl"),
        "SPECS_JAVA_LIBS_HOME": str(control.SPECS_JAVA_LIBS_ROOT.resolve()),
        "LARA_FRAMEWORK_HOME": str(control.LARA_FRAMEWORK_ROOT.resolve()),
    })
    return environment


def create_command(stage: dict) -> list[str]:
    gradle = shutil.which("gradle")
    require(gradle is not None, "gradle was not found on PATH")
    command = [
        gradle,
        "--no-daemon", "--offline", "--info",
        "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
        "--init-script", str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle"),
        "--init-script", str(TIMELINE_INIT),
    ]
    if stage["key"] == "protobuf":
        command.append(f"-PclangDumperRoot={stage['native_root']}")
    command.extend(["-p", "ClangAstParser", "test"])
    return command


def record_event(path: Path, source: str, name: str, **fields: object) -> dict:
    event = {
        "source": source,
        "event": name,
        "epoch_ns": time.time_ns(),
        "monotonic_ns": time.monotonic_ns(),
        **fields,
    }
    with path.open("a", encoding="utf-8") as output:
        output.write(json.dumps(event, sort_keys=True) + "\n")
    return event


def consume_gradle_output(process: subprocess.Popen, log_path: Path,
                          parent_events_path: Path) -> list[dict]:
    observed = []
    patterns = (
        (re.compile(r"Starting process 'Gradle Test Executor \d+'"), "test_executor_launch_requested"),
        (re.compile(r"Successfully started process 'Gradle Test Executor \d+'"), "test_executor_started"),
        (re.compile(r"Gradle Test Executor \d+ started executing tests\."), "worker_started_tests"),
        (re.compile(r"Gradle Test Executor \d+ finished executing tests\."), "test_executor_finished"),
        (re.compile(r"Finished generating test html results"), "test_html_report_generated"),
        (re.compile(r"Finished generating test XML results"), "test_xml_report_generated"),
        (re.compile(r"^> Task .*:jacocoTestReport\s+SKIPPED"), "jacoco_report_task_skipped"),
        (re.compile(r"^> Task .*:jacocoTestCoverageVerification\s+SKIPPED"),
         "jacoco_coverage_verification_skipped"),
        (re.compile(r"^BUILD SUCCESSFUL"), "build_success"),
        (re.compile(r"^BUILD FAILED"), "build_failure"),
    )
    with log_path.open("w", encoding="utf-8", errors="replace") as log:
        assert process.stdout is not None
        for line in process.stdout:
            log.write(line)
            log.flush()
            for pattern, event_name in patterns:
                if pattern.search(line):
                    observed.append(record_event(
                        parent_events_path, "wrapper", event_name,
                        marker_line=line.strip()[:180],
                    ))
                    break
    return observed


def read_events(path: Path) -> list[dict]:
    if not path.is_file():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]


def event_time(events: list[dict], name: str, predicate=None, last: bool = False) -> int | None:
    selected = [event for event in events if event.get("event") == name
                and (predicate is None or predicate(event))]
    if not selected:
        return None
    return selected[-1 if last else 0].get("epoch_ns")


def phase(name: str, start_ns: int | None, end_ns: int | None) -> dict:
    valid = start_ns is not None and end_ns is not None and end_ns >= start_ns
    return {
        "phase": name,
        "start_epoch_ns": start_ns if valid else None,
        "end_epoch_ns": end_ns if valid else None,
        "start_utc": utc_from_ns(start_ns) if valid else None,
        "end_utc": utc_from_ns(end_ns) if valid else None,
        "duration_s": (end_ns - start_ns) / 1_000_000_000 if valid else None,
    }


def task_durations(gradle_events: list[dict]) -> list[dict]:
    starts: dict[tuple[str, str], list[int]] = {}
    durations = []
    for event in gradle_events:
        if event.get("root_name") != "ClangAstParser":
            continue
        key = (str(event.get("task")), str(event.get("project")))
        if event["event"] == "task_before_execute":
            starts.setdefault(key, []).append(int(event["epoch_ns"]))
        elif event["event"] == "task_after_execute" and starts.get(key):
            start_ns = starts[key].pop(0)
            end_ns = int(event["epoch_ns"])
            durations.append({
                "task": key[0],
                "project": key[1],
                "start_epoch_ns": start_ns,
                "end_epoch_ns": end_ns,
                "start_utc": utc_from_ns(start_ns),
                "end_utc": utc_from_ns(end_ns),
                "duration_s": (end_ns - start_ns) / 1_000_000_000,
                "skipped": event.get("skipped"),
                "skip_message": event.get("skip_message"),
                "failure": event.get("failure"),
            })
    return durations


def summarize_timeline(gradle_events: list[dict], wrapper_events: list[dict]) -> dict:
    def gts(name: str, **filters) -> int | None:
        return event_time(
            gradle_events, name,
            predicate=lambda row: row.get("root_name") == "ClangAstParser"
            and all(row.get(key) == value for key, value in filters.items()),
        )

    def wts(name: str) -> int | None:
        return event_time(wrapper_events, name)

    root_settings = next((event for event in gradle_events
                          if event.get("event") == "settings_evaluated"
                          and event.get("root_name") == "ClangAstParser"), None)
    root_init_ns = root_settings.get("init_epoch_ns") if root_settings else None
    phases = [
        phase("gradle_process_startup_and_init",
              wts("process_spawned"), root_init_ns),
        phase("settings_evaluation",
              root_init_ns, gts("settings_evaluated")),
        phase("project_configuration",
              gts("projects_loaded"), gts("projects_evaluated")),
        phase("task_graph_creation",
              gts("projects_evaluated"), gts("task_graph_ready")),
        phase("graph_ready_to_test_task",
              gts("task_graph_ready"), gts("task_before_execute", task=":test")),
        phase("test_task_setup_before_do_first",
              gts("task_before_execute", task=":test"), gts("test_task_do_first")),
        phase("test_task_configuration_to_worker_spawn",
              gts("test_task_do_first"), wts("test_executor_launch_requested")),
        phase("test_executor_launch",
              wts("test_executor_launch_requested"), wts("test_executor_started")),
        phase("test_task_do_first_to_root_suite_before",
              gts("test_task_do_first"), gts("root_suite_before")),
        phase("test_worker_lifecycle",
              wts("worker_started_tests"), wts("test_executor_finished")),
        phase("root_suite_callback_interval",
              gts("root_suite_before"), gts("root_suite_after")),
        phase("worker_finish_to_html_report_log",
              wts("test_executor_finished"), wts("test_html_report_generated")),
        phase("worker_finish_to_xml_report_log",
              wts("test_executor_finished"), wts("test_xml_report_generated")),
        phase("worker_finish_to_test_do_last",
              wts("test_executor_finished"), gts("test_task_do_last")),
        phase("test_task_action_close",
              gts("test_task_do_last"), gts("task_after_execute", task=":test")),
        phase("test_task_close_to_build_success",
              gts("task_after_execute", task=":test"), wts("build_success")),
        phase("whole_gradle_command",
              wts("process_spawned"), wts("process_exit")),
    ]
    root_after = next((event for event in gradle_events
                       if event.get("event") == "root_suite_after"
                       and event.get("root_name") == "ClangAstParser"), None)
    class_events = [event for event in gradle_events
                    if event.get("event") == "class_suite_after"
                    and event.get("root_name") == "ClangAstParser"]
    return {
        "phases": phases,
        "task_durations": task_durations(gradle_events),
        "test_results": {
            "root_suite": root_after,
            "class_suite_results": class_events,
            "class_suite_count": len(class_events),
            "root_summed_leaf_duration_ms": (
                root_after.get("summed_leaf_duration_ms") if root_after else None
            ),
            "root_leaf_test_count": root_after.get("leaf_test_count") if root_after else None,
        },
    }


def phase_csv_rows(round_number: int, stage: str, summary: dict) -> list[dict]:
    rows = []
    for item in summary["phases"]:
        rows.append({"round": round_number, "stage": stage, **item})
    for item in summary["task_durations"]:
        rows.append({
            "round": round_number,
            "stage": stage,
            "phase": f"task:{item['task']}",
            "start_epoch_ns": item["start_epoch_ns"],
            "end_epoch_ns": item["end_epoch_ns"],
            "start_utc": item["start_utc"],
            "end_utc": item["end_utc"],
            "duration_s": item["duration_s"],
            "task_project": item["project"],
            "task_skipped": item["skipped"],
            "task_skip_message": item["skip_message"],
            "task_failure": item["failure"],
        })
    return rows


def write_phase_csv(path: Path, rows: list[dict]) -> None:
    fields = (
        "round", "stage", "phase", "start_epoch_ns", "end_epoch_ns",
        "start_utc", "end_utc", "duration_s", "task_project", "task_skipped",
        "task_skip_message", "task_failure",
    )
    with path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field) for field in fields})


def verify_phase_source_events(gradle_events: list[dict], wrapper_events: list[dict]) -> None:
    root_events = [event for event in gradle_events
                   if event.get("root_name") == "ClangAstParser"]
    gradle_names = {event.get("event") for event in root_events}
    wrapper_names = {event.get("event") for event in wrapper_events}
    require({"settings_evaluated", "projects_evaluated",
             "task_graph_ready", "root_suite_before", "root_suite_after",
             "test_task_do_first", "test_task_do_last"}.issubset(gradle_names),
            f"required Gradle lifecycle markers missing: {sorted(gradle_names)}")
    require(any(event.get("event") == "init_script_loaded" for event in gradle_events)
            and any(event.get("event") == "settings_evaluated"
                    and event.get("root_name") == "ClangAstParser"
                    and event.get("init_elapsed_ns", 0) > 0 for event in gradle_events),
            "Gradle init/start boundary is missing or unassociated with the test build root")
    require({"process_spawned", "process_exit", "test_executor_launch_requested",
             "test_executor_started", "worker_started_tests", "test_executor_finished",
             "test_html_report_generated", "test_xml_report_generated",
             "jacoco_report_task_skipped", "jacoco_coverage_verification_skipped",
             "build_success"}.issubset(wrapper_names),
            f"required parent-observed markers missing: {sorted(wrapper_names)}")


def run_preflight(stage: dict, output_root: Path, inherited_options: list[str]) -> dict:
    run_dir = output_root / "preflight" / stage["key"]
    run_dir.mkdir(parents=True)
    env = create_environment(stage, run_dir, run_dir / "unused-cache", inherited_options)
    command = create_command(stage)
    command.insert(1, "--dry-run")
    start_ns = time.monotonic_ns()
    process = subprocess.run(command, cwd=Path(stage["root"]) / "clava", env=env,
                             capture_output=True, text=True, errors="replace", check=False)
    end_ns = time.monotonic_ns()
    log_path = run_dir / "preflight.log"
    log_path.write_text(process.stdout + process.stderr, encoding="utf-8")
    require(process.returncode == 0,
            f"dry-run init-script preflight failed for {stage['key']}: {process.returncode}")
    log = process.stdout + process.stderr
    require("BUILD SUCCESSFUL" in log, f"dry-run did not complete successfully for {stage['key']}")
    require(not control.task_compilation_lines(log_path),
            f"dry-run attempted a source compilation task for {stage['key']}")
    return {
        "stage": stage["key"],
        "status": "passed",
        "exit_status": process.returncode,
        "elapsed_s": (end_ns - start_ns) / 1_000_000_000,
        "timeline_init_sha256": sha256_file(TIMELINE_INIT),
        "command": command,
        "dry_run_no_tests_executed": True,
        "compile_tasks_not_executed": True,
    }


def execute_run(matrix: dict, stage: dict, output_root: Path, round_number: int,
                ordinal: int, inherited_options: list[str], expected_identity: str,
                cache_dir: Path) -> dict:
    key = stage["key"]
    run_dir = output_root / "runs" / f"round-{round_number:02d}" / key
    run_dir.mkdir(parents=True)
    timeline_root = run_dir / "timeline"
    timeline_root.mkdir()
    gradle_events_path = run_dir / "gradle-events.jsonl"
    wrapper_events_path = run_dir / "wrapper-events.jsonl"

    environment = create_environment(stage, run_dir, cache_dir, inherited_options)
    base.configure_cache_mode(stage, "cold", True, environment, cache_dir)
    before_cache = {field: 0 for field in EXPECTED_COLD_CACHE}
    xdg_before = control.cache_manifest(Path(environment["XDG_CACHE_HOME"]))
    command = create_command(stage)
    cwd = Path(stage["root"]) / "clava"
    (run_dir / "command.json").write_text(json.dumps({
        "command": command,
        "cwd": str(cwd),
        "stage": key,
        "round": round_number,
        "ordinal": ordinal,
        "worker_agent": "off",
        "cache_mode": "per-command isolated cold",
        "explicit_gc_override": False,
    }, indent=2) + "\n", encoding="utf-8")

    inherited_now = control.audit_inherited_jvm_options()
    require(all(not inherited_now[name]["heap_gc_agent_flags"]
                for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS")),
            "inherited JVM control flags changed immediately before process spawn")

    record_event(wrapper_events_path, "wrapper", "process_spawn_requested",
                 command_argv=command, round=round_number, stage=key)
    spawn_mono_ns = time.monotonic_ns()
    process = subprocess.Popen(command, cwd=cwd, env=environment,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, encoding="utf-8", errors="replace", bufsize=1)
    spawned = record_event(wrapper_events_path, "wrapper", "process_spawned",
                           pid=process.pid, round=round_number, stage=key)
    observed = consume_gradle_output(
        process, run_dir / "gradle.log", wrapper_events_path
    )
    return_code = process.wait()
    process_exit = record_event(wrapper_events_path, "wrapper", "process_exit",
                                return_code=return_code, round=round_number, stage=key)
    exit_mono_ns = time.monotonic_ns()

    after_cache = control.ccache_stats(cache_dir)
    cache_delta = {
        field: int(after_cache[field]) - int(before_cache[field])
        for field in EXPECTED_COLD_CACHE
    }
    junit_root = run_dir / "gradle-output" / "junit-xml"
    test_counts = control.junit_counts(junit_root)
    test_identity = control.junit_identity_sha256(junit_root)
    executor_args = control.observed_test_executor_args(run_dir / "gradle.log")
    normalized_args, response_files = control.normalize_executor_args(executor_args)
    worker_path = run_dir / "gradle-output" / "worker-configuration.json"
    worker = json.loads(worker_path.read_text()) if worker_path.is_file() else {}
    native_path, native_sha = control.observed_native_tool(stage, run_dir / "gradle.log")
    compile_violations = control.task_compilation_lines(run_dir / "gradle.log")
    log_text = (run_dir / "gradle.log").read_text(errors="replace")
    report_skips = {
        name: bool(re.search(rf"^> Task .*:{name}\s+SKIPPED$", log_text, re.MULTILINE))
        for name in ("jacocoTestReport", "jacocoTestCoverageVerification")
    }
    gradle_events = read_events(gradle_events_path)
    wrapper_events = read_events(wrapper_events_path)
    verify_phase_source_events(gradle_events, wrapper_events)
    timeline = summarize_timeline(gradle_events, wrapper_events)

    gc_flags = [arg for arg in executor_args if arg.startswith(GC_FLAGS)]
    java_agents = [arg for arg in executor_args if arg.startswith(AGENT_FLAGS)]
    heap_flags = [arg for arg in executor_args if arg.startswith(("-Xms", "-Xmx", "-Xmn"))]
    xdg_after = control.cache_manifest(Path(environment["XDG_CACHE_HOME"]))
    cache_ok = cache_delta == EXPECTED_COLD_CACHE
    counts_ok = {field: test_counts.get(field) for field in EXPECTED} == EXPECTED
    valid = all((
        return_code == 0,
        counts_ok,
        test_identity == expected_identity,
        cache_ok,
        native_sha == stage["native_binary_sha256"],
        not compile_violations,
        worker.get("requested_agent") == "off",
        worker.get("jacoco_enabled") is False,
        worker.get("javaagent_args") == [],
        not java_agents,
        heap_flags == ["-Xmx512m"],
        not gc_flags,
        report_skips["jacocoTestReport"],
        report_skips["jacocoTestCoverageVerification"],
        not xdg_before,
        timeline["test_results"]["root_leaf_test_count"] == 116,
        timeline["test_results"]["root_suite"].get("test_count") == 116,
        timeline["test_results"]["root_suite"].get("successful_test_count") == 116,
        timeline["test_results"]["root_suite"].get("failed_test_count") == 0,
        timeline["test_results"]["root_suite"].get("skipped_test_count") == 0,
        timeline["test_results"]["class_suite_count"] > 0,
        sum(event.get("test_count", 0)
            for event in timeline["test_results"]["class_suite_results"]) == 116,
        all(item["duration_s"] is not None for item in timeline["phases"]),
    ))

    process_wall_s = (exit_mono_ns - spawn_mono_ns) / 1_000_000_000
    row = {
        "ordinal": ordinal,
        "round": round_number,
        "stage": key,
        "valid": valid,
        "diagnostic_only_not_headline": True,
        "return_code": return_code,
        "process_wall_elapsed_s": process_wall_s,
        "junit_aggregate_s": test_counts.get("junit_aggregate_s"),
        "wall_minus_junit_residual_s": process_wall_s - float(test_counts.get("junit_aggregate_s", 0.0)),
        "passed_tests": test_counts.get("passed_tests"),
        "failed_tests": test_counts.get("failed_tests"),
        "skipped_tests": test_counts.get("skipped_tests"),
        "total_tests": test_counts.get("total_tests"),
        "test_identity_sha256": test_identity,
        "expected_test_identity_sha256": expected_identity,
        "cache_delta": cache_delta,
        "cache_expected": EXPECTED_COLD_CACHE,
        "cache_passed": cache_ok,
        "cache_mode": "per-command isolated cold",
        "actual_cache_dir": str(cache_dir),
        "xdg_cache_home": environment["XDG_CACHE_HOME"],
        "xdg_cache_before_files": len(xdg_before),
        "xdg_cache_before_manifest_sha256": control.canonical_hash(xdg_before),
        "xdg_cache_after_files": len(xdg_after),
        "xdg_cache_after_manifest_sha256": control.canonical_hash(xdg_after),
        "actual_native_binary_sha256": native_sha,
        "expected_native_binary_sha256": stage["native_binary_sha256"],
        "actual_test_executor_args": executor_args,
        "normalized_test_executor_args": normalized_args,
        "classpath_response_files": response_files,
        "worker_configuration": worker,
        "report_tasks_skipped": report_skips,
        "compile_task_violations": compile_violations,
        "gc_flags": gc_flags,
        "java_agent_flags": java_agents,
        "worker_heap_flags": heap_flags,
        "timeline": timeline,
        "parent_observed_markers": observed,
        "spawn_requested_epoch_ns": int(
            next(event["epoch_ns"] for event in wrapper_events
                 if event.get("event") == "process_spawn_requested")
        ),
        "process_spawned_epoch_ns": spawned["epoch_ns"],
        "process_exit_epoch_ns": process_exit["epoch_ns"],
        "spawn_requested_utc": utc_from_ns(next(
            event["epoch_ns"] for event in wrapper_events
            if event.get("event") == "process_spawn_requested"
        )),
        "process_exit_utc": utc_from_ns(process_exit["epoch_ns"]),
    }
    write_json(run_dir / "summary.json", row)
    (run_dir / "phases.csv").write_text("", encoding="utf-8")
    return row


def recover_existing_run(stage: dict, output_root: Path, round_number: int,
                         ordinal: int, expected_identity: str) -> dict:
    """Validate and adopt a completed run whose postprocessing gate alone failed."""
    key = stage["key"]
    run_dir = output_root / "runs" / f"round-{round_number:02d}" / key
    require(run_dir.is_dir(), f"cannot recover missing run directory: {run_dir}")
    gradle_log = run_dir / "gradle.log"
    gradle_events_path = run_dir / "gradle-events.jsonl"
    wrapper_events_path = run_dir / "wrapper-events.jsonl"
    gradle_events = read_events(gradle_events_path)
    wrapper_events = read_events(wrapper_events_path)
    verify_phase_source_events(gradle_events, wrapper_events)
    timeline = summarize_timeline(gradle_events, wrapper_events)

    diagnostic_root = run_dir / "gradle-output"
    junit_root = diagnostic_root / "junit-xml"
    test_counts = control.junit_counts(junit_root)
    test_identity = control.junit_identity_sha256(junit_root)
    executor_args = control.observed_test_executor_args(gradle_log)
    normalized_args, response_files = control.normalize_executor_args(executor_args)
    worker_path = diagnostic_root / "worker-configuration.json"
    worker = json.loads(worker_path.read_text(encoding="utf-8")) if worker_path.is_file() else {}
    native_path, native_sha = control.observed_native_tool(stage, gradle_log)
    compile_violations = control.task_compilation_lines(gradle_log)
    log_text = gradle_log.read_text(errors="replace")
    report_skips = {
        name: bool(re.search(rf"^> Task .*:{name}\s+SKIPPED$", log_text, re.MULTILINE))
        for name in ("jacocoTestReport", "jacocoTestCoverageVerification")
    }
    cache_dir = (run_dir / "tmp" / f"clang_ast_exe_{getpass.getuser()}"
                 / control.NAMESPACE[key])
    cache_delta = control.ccache_stats(cache_dir)
    cache_ok = cache_delta == EXPECTED_COLD_CACHE
    return_event = next((event for event in wrapper_events
                         if event.get("event") == "process_exit"), None)
    spawn_event = next((event for event in wrapper_events
                        if event.get("event") == "process_spawn_requested"), None)
    spawned_event = next((event for event in wrapper_events
                          if event.get("event") == "process_spawned"), None)
    require(return_event is not None and spawn_event is not None and spawned_event is not None,
            "recovery is missing wrapper spawn/exit bounds")
    return_code = int(return_event["return_code"])
    spawn_mono_ns = int(spawn_event["monotonic_ns"])
    exit_mono_ns = int(return_event["monotonic_ns"])
    process_wall_s = (exit_mono_ns - spawn_mono_ns) / 1_000_000_000
    xdg_root = run_dir / "xdg-cache"
    xdg_after = control.cache_manifest(xdg_root)
    gc_flags = [arg for arg in executor_args if arg.startswith(GC_FLAGS)]
    java_agents = [arg for arg in executor_args if arg.startswith(AGENT_FLAGS)]
    heap_flags = [arg for arg in executor_args if arg.startswith(("-Xms", "-Xmx", "-Xmn"))]
    valid = all((
        return_code == 0,
        {field: test_counts.get(field) for field in EXPECTED} == EXPECTED,
        test_identity == expected_identity,
        cache_ok,
        not xdg_after,
        native_sha == stage["native_binary_sha256"],
        not compile_violations,
        worker.get("requested_agent") == "off",
        worker.get("jacoco_enabled") is False,
        worker.get("javaagent_args") == [],
        not java_agents,
        heap_flags == ["-Xmx512m"],
        not gc_flags,
        report_skips["jacocoTestReport"],
        report_skips["jacocoTestCoverageVerification"],
        timeline["test_results"]["root_leaf_test_count"] == 116,
        timeline["test_results"]["root_suite"].get("test_count") == 116,
        timeline["test_results"]["root_suite"].get("successful_test_count") == 116,
        timeline["test_results"]["root_suite"].get("failed_test_count") == 0,
        timeline["test_results"]["root_suite"].get("skipped_test_count") == 0,
        timeline["test_results"]["class_suite_count"] > 0,
        sum(event.get("test_count", 0)
            for event in timeline["test_results"]["class_suite_results"]) == 116,
        all(item["duration_s"] is not None for item in timeline["phases"]),
    ))
    require(valid, "preserved Text run does not satisfy all cold diagnostic gates")

    row = {
        "ordinal": ordinal,
        "round": round_number,
        "stage": key,
        "valid": valid,
        "diagnostic_only_not_headline": True,
        "recovered_from_successful_raw_run": True,
        "planned_mode": "warm",
        "actual_mode": "per-command isolated cold",
        "return_code": return_code,
        "process_wall_elapsed_s": process_wall_s,
        "junit_aggregate_s": test_counts.get("junit_aggregate_s"),
        "wall_minus_junit_residual_s": process_wall_s - float(test_counts.get("junit_aggregate_s", 0.0)),
        "passed_tests": test_counts.get("passed_tests"),
        "failed_tests": test_counts.get("failed_tests"),
        "skipped_tests": test_counts.get("skipped_tests"),
        "total_tests": test_counts.get("total_tests"),
        "test_identity_sha256": test_identity,
        "expected_test_identity_sha256": expected_identity,
        "cache_delta": cache_delta,
        "cache_expected": EXPECTED_COLD_CACHE,
        "cache_passed": cache_ok,
        "cache_mode": "per-command isolated cold",
        "actual_cache_dir": str(cache_dir),
        "xdg_cache_home": str(xdg_root),
        "xdg_cache_before_files": 0,
        "xdg_cache_before_manifest_sha256": control.canonical_hash({}),
        "xdg_cache_after_files": len(xdg_after),
        "xdg_cache_after_manifest_sha256": control.canonical_hash(xdg_after),
        "actual_native_binary_sha256": native_sha,
        "expected_native_binary_sha256": stage["native_binary_sha256"],
        "actual_test_executor_args": executor_args,
        "normalized_test_executor_args": normalized_args,
        "classpath_response_files": response_files,
        "worker_configuration": worker,
        "report_tasks_skipped": report_skips,
        "compile_task_violations": compile_violations,
        "gc_flags": gc_flags,
        "java_agent_flags": java_agents,
        "worker_heap_flags": heap_flags,
        "timeline": timeline,
        "parent_observed_markers": wrapper_events,
        "process_spawn_requested_epoch_ns": spawn_event["epoch_ns"],
        "process_spawned_epoch_ns": spawned_event["epoch_ns"],
        "process_exit_epoch_ns": return_event["epoch_ns"],
        "process_spawned_utc": utc_from_ns(spawned_event["epoch_ns"]),
        "process_exit_utc": utc_from_ns(return_event["epoch_ns"]),
    }
    write_json(run_dir / "summary.json", row)
    (run_dir / "phases.csv").write_text("", encoding="utf-8")
    return row


def summarize_pairs(results: list[dict]) -> dict:
    by_round = {}
    for row in results:
        by_round.setdefault(row["round"], {})[row["stage"]] = row
    pairs = []
    incomplete_rounds = []
    for round_number, stages in sorted(by_round.items()):
        if set(stages) != set(STAGES):
            incomplete_rounds.append(round_number)
            continue
        text = stages["ccache-text"]
        protobuf = stages["protobuf"]
        pairs.append({
            "round": round_number,
            "order": [row["stage"] for row in results if row["round"] == round_number],
            "protobuf_minus_text_wall_s": protobuf["process_wall_elapsed_s"] - text["process_wall_elapsed_s"],
            "protobuf_minus_text_junit_s": protobuf["junit_aggregate_s"] - text["junit_aggregate_s"],
            "protobuf_minus_text_residual_s": protobuf["wall_minus_junit_residual_s"] - text["wall_minus_junit_residual_s"],
            "phases": {
                phase_name: {
                    "protobuf_minus_text_s": (
                        next((p["duration_s"] for p in protobuf["timeline"]["phases"]
                              if p["phase"] == phase_name), None)
                        - next((p["duration_s"] for p in text["timeline"]["phases"]
                                if p["phase"] == phase_name), None)
                    )
                    if next((p["duration_s"] for p in protobuf["timeline"]["phases"]
                             if p["phase"] == phase_name), None) is not None
                    and next((p["duration_s"] for p in text["timeline"]["phases"]
                              if p["phase"] == phase_name), None) is not None else None
                }
                for phase_name in [phase["phase"] for phase in text["timeline"]["phases"]]
            },
        })
    return {"pairs": pairs, "incomplete_rounds": incomplete_rounds}


def persist(root: Path, plan: dict, results: list[dict], phase_rows: list[dict]) -> None:
    write_json(root / "results.json", {
        "plan": plan,
        "results": results,
        "paired_summary": summarize_pairs(results),
    })
    write_phase_csv(root / "phase-bounds.csv", phase_rows)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=MATRIX_PATH)
    parser.add_argument("--worker-evidence", type=Path, default=WORKER_EVIDENCE_PATH)
    parser.add_argument("--output-root", type=Path, default=OUTPUT_ROOT)
    parser.add_argument("--resume-cold-first-text", action="store_true")
    args = parser.parse_args()
    matrix_path = args.matrix.resolve()
    output_root = args.output_root.resolve()
    if args.resume_cold_first_text:
        require(output_root.is_dir(), f"cold resume requires an existing output root: {output_root}")
    else:
        require(not output_root.exists(), f"refusing to reuse existing output root: {output_root}")

    worker_evidence_path = args.worker_evidence.resolve()
    matrix, stages, identity, cold_rows = load_and_verify(matrix_path, worker_evidence_path)
    inherited, inherited_audit = inherited_java_options()
    require(TIMELINE_INIT.is_file(), "timeline init script is missing")
    init_sha = sha256_file(TIMELINE_INIT)
    require("System.gc()" not in TIMELINE_INIT.read_text()
            and "Runtime.getRuntime().gc()" not in TIMELINE_INIT.read_text(),
            "timeline init script contains an explicit GC call")

    if args.resume_cold_first_text:
        plan_path = output_root / "plan.json"
        require(plan_path.is_file(), "cold resume is missing its original preflight plan")
        plan = json.loads(plan_path.read_text(encoding="utf-8"))
        require(plan.get("primary_matrix_sha256") == sha256_file(matrix_path)
                and plan.get("worker_evidence_matrix_sha256") == sha256_file(worker_evidence_path)
                and plan.get("timeline_init_sha256") == init_sha,
                "cold resume source or instrumentation provenance differs from the original preflight")
        require(plan.get("status") == "preflight_passed"
                and all(item.get("status") == "passed" for item in plan.get("preflight", [])),
                "cold resume requires the original two successful dry-run preflights")
        first_row = recover_existing_run(
            stages["ccache-text"], output_root, 1, 1,
            identity["expected_test_identity_sha256"],
        )
        results = [first_row]
        all_phase_rows = phase_csv_rows(1, "ccache-text", first_row["timeline"])
        plan["planned_cache_mode"] = "warm"
        plan["actual_cache_mode"] = "per-command isolated cold"
        plan["cache_mode_deviation"] = (
            "the first invocation used run-specific java.io.tmpdir, so the Java runtime's default DUMPER_FOLDER placed ccache under the invocation temp root instead of the separately cloned warm cache; observed 208 misses and zero hits, matching the frozen cold-cell cache outcome"
        )
        plan["expected_cache_delta_per_command"] = EXPECTED_COLD_CACHE
        plan["worker_policy"]["ccache_mode"] = "per-command isolated cold"
        plan["worker_policy"]["JaCoCo_test_worker_agent"] = "off"
        plan["worker_policy"]["Test_worker_Xmx"] = "512m"
        plan["schedule"] = [
            {"ordinal": index + 1, "round": round_number, "stage": key,
             "mode": "per-command isolated cold", "agent": "off"}
            for index, (round_number, key) in enumerate(SCHEDULE)
        ]
        plan["environment_controls"] = {
            "java_io_tmpdir": "unique per invocation under runs/<round>/<stage>/tmp; this selects the actual ccache path and yields cold cache semantics",
            "xdg_cache_home": "unique per invocation, initially empty; manifests captured per row",
            "TMPDIR_TMP_TEMP": "same per-invocation tmp root as java.io.tmpdir",
            "cache_validation": "each command must show exactly 208 cacheable calls, 0 hits, 208 misses, 0 uncacheable calls",
        }
        plan["status"] = "resuming_cold_diagnostic"
        write_json(plan_path, plan)
    else:
        # Prove Gradle can compile/configure the instrumentation for both stages
        # without running tests or compile tasks, before any measured command.
        output_root.mkdir(parents=True)
        preflight = [run_preflight(stages[key], output_root, inherited) for key in STAGES]

        cache_source_hashes = {}
        for key in STAGES:
            rows = cold_rows[key]
            source_paths = {Path(row["cache_validation"]["cache_dir"]).resolve() for row in rows}
            require(len(source_paths) == 1, f"cold cache source is not unique for {key}")
            cache_source_hashes[key] = control.canonical_hash(
                control.cache_manifest(next(iter(source_paths)))
            )

        plan = {
        "schema_version": 1,
        "diagnostic_only_not_headline": True,
        "primary_matrix_sha256": sha256_file(matrix_path),
        "worker_evidence_matrix_sha256": sha256_file(worker_evidence_path),
        "timeline_init_sha256": init_sha,
        "stages": {
            key: {
                "clava_revision": stages[key]["clava_revision"],
                "clava_patch_sha256": stages[key]["clava_patch_sha256"],
                "native_revision": stages[key]["native_revision"],
                "native_binary_sha256": stages[key]["native_binary_sha256"],
                "parser_jar_sha256": stages[key]["parser_jar_sha256"],
                "runtime_manifest_sha256": stages[key]["runtime_manifest_sha256"],
                "schema_sha256": stages[key]["native_build"]["schema_files_sha256"],
                "cold_cache_source_manifest_sha256": cache_source_hashes[key],
                "fixture_fingerprint_sha256": identity["fixture_fingerprints"],
            }
            for key in STAGES
        },
        "expected_test_count": EXPECTED["total_tests"],
        "expected_test_identity_sha256": identity["expected_test_identity_sha256"],
        "excluded_prior_diagnostic_attempts": [{
            "artifact_root": str(OUTPUT_ROOT.with_name("java-runtime-timeline-r1")),
            "reason": "the first Text attempt completed test bodies but the init listener then failed on a nonexistent TestResult.totalTime property",
            "timing_rows_included": False,
        }],
        "expected_cache_delta_per_command": EXPECTED_COLD_CACHE,
        "worker_policy": {
            "JaCoCo_test_worker_agent": "off",
            "Test_worker_Xmx": "512m",
            "explicit_gc_override": False,
            "JaCoCo_report_tasks": "skipped",
            "source_or_runtime_builds_during_measurement": False,
        },
        "inherited_jvm_environment_audit": inherited_audit,
        "java_tool_options_policy": {
            "inherited_tokens_preserved": True,
            "timeline_owned_system_properties": ["java.io.tmpdir", "clava.astWire"],
            "each_owned_property_appended_once": True,
        },
        "schedule": [
            {"ordinal": index + 1, "round": round_number, "stage": key,
             "mode": "per-command isolated cold", "agent": "off"}
            for index, (round_number, key) in enumerate(SCHEDULE)
        ],
        "phase_definitions": {
            "gradle_process_startup_and_init": "Gradle process spawn to timeline init script loaded",
            "settings_evaluation": "init script loaded to settings evaluated",
            "project_configuration": "projects loaded to projects evaluated",
            "task_graph_creation": "projects evaluated to task graph ready",
            "test_task_setup_before_do_first": "Gradle test task beforeExecute to instrumented doFirst",
            "test_task_configuration_to_worker_spawn": "instrumented doFirst to parent-observed worker process spawn line",
            "test_executor_launch": "parent-observed start-process to successfully-started line",
            "test_task_do_first_to_root_suite_before": "Gradle test-task doFirst to TestListener root-suite start callback",
            "test_worker_lifecycle": "worker started executing tests line to worker finished executing tests line",
            "root_suite_callback_interval": "Gradle TestListener root suite before/after callback wall interval",
            "worker_finish_to_test_do_last": "parent-observed worker finish line to Gradle Test task doLast",
            "task_durations": "Gradle TaskExecutionListener monotonic before/after spans",
            "clock_alignment": "same-host epoch nanoseconds for parent/Gradle cross-process bounds; monotonic clocks retained per source",
        },
        "preflight": preflight,
        "status": "preflight_passed",
        }
        results = []
        all_phase_rows = []
        write_json(output_root / "plan.json", plan)

    start_ordinal = len(results) + 1
    for ordinal, (round_number, key) in enumerate(SCHEDULE, start=1):
        if ordinal < start_ordinal:
            continue
        result = execute_run(
            matrix, stages[key], output_root, round_number, ordinal, inherited,
            identity["expected_test_identity_sha256"],
            output_root / "runs" / f"round-{round_number:02d}" / key / "tmp"
            / f"clang_ast_exe_{getpass.getuser()}" / base.cache_namespace(stages[key]),
        )
        results.append(result)
        all_phase_rows.extend(phase_csv_rows(round_number, key, result["timeline"]))
        plan["status"] = "running"
        persist(output_root, plan, results, all_phase_rows)
        print(json.dumps({
            "ordinal": ordinal,
            "round": round_number,
            "stage": key,
            "valid": result["valid"],
            "wall_s": round(result["process_wall_elapsed_s"], 3),
            "junit_s": round(float(result["junit_aggregate_s"]), 3),
            "residual_s": round(result["wall_minus_junit_residual_s"], 3),
        }), flush=True)
        if not result["valid"]:
            plan["status"] = "stopped_invalid_row"
            persist(output_root, plan, results, all_phase_rows)
            raise RuntimeError(f"timeline command failed strict runtime gates: {key}, round {round_number}")

    plan["status"] = "complete"
    plan["completed_at_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
    write_json(output_root / "plan.json", plan)
    persist(output_root, plan, results, all_phase_rows)
    print(json.dumps({
        "output_root": str(output_root),
        "rows": len(results),
        "all_valid": all(row["valid"] for row in results),
        "phase_rows": len(all_phase_rows),
        "status": plan["status"],
    }, indent=2), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
