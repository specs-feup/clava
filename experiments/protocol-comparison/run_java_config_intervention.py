#!/usr/bin/env python3
"""Bounded config-only intervention for the protobuf Gradle integration."""

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
import statistics
import subprocess
import sys
import time

sys.dont_write_bytecode = True

import run_java_runtime_timeline as runtime_timeline


SCRIPT_ROOT = Path(__file__).resolve().parent
MATRIX_PATH = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/results.json"
WORKER_EVIDENCE_PATH = (
    SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-noagent-matrix-r2/results.json"
)
OUTPUT_ROOT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-config-intervention-r1"
GENERATED_SOURCE_DIRS = (
    "generated/source/proto/main/java",
    "generated/source/proto-schema/main",
    "generated/source/proto-bindings/main",
    "generated/source/proto-descriptor-hash/main",
)
SCHEDULE = (
    (1, ("original", "control")),
    (2, ("control", "original")),
    (3, ("original", "control")),
    (4, ("control", "original")),
)
ARMS = ("original", "control")
TASK_STATUS = re.compile(r"^(:\S+)\s+(\S+)\s*$")
WORKER_MARKERS = (
    "Gradle Test Executor",
    "DEADLINE_TEST_WORKER",
    "Starting process 'Gradle Test Executor",
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def remove_exactly_once(content: str, old: str, description: str) -> str:
    count = content.count(old)
    require(count == 1, f"expected one {description} anchor, found {count}")
    return content.replace(old, "", 1)


def section(content: str, start: str, end: str) -> str:
    first = content.index(start)
    last = content.index(end, first + len(start))
    return content[first:last]


def section_to_end(content: str, start: str) -> str:
    return content[content.index(start):]


def disable_protobuf_build_integration(original: str) -> tuple[str, list[str]]:
    """Remove plugin/generation wiring while retaining runtime/source inputs."""
    control = remove_exactly_once(
        original,
        "    id 'com.google.protobuf' version '0.9.4'\n",
        "protobuf plugin declaration",
    )

    generation_start = "tasks.register('generateProtoSchemaHash') {"
    generation_end = "\njava {\n"
    start = control.find(generation_start)
    end = control.find(generation_end, start)
    require(start >= 0 and end > start, "protobuf generation block boundaries changed")
    control = control[:start] + "// Config-only control: protobuf plugin/generation wiring omitted.\n" + control[end + 1:]

    control = remove_exactly_once(
        control,
        "        proto {\n            srcDir wireSchemaDir\n        }\n",
        "protobuf source-set extension registration",
    )
    control = remove_exactly_once(
        control,
        "            srcDir 'src'\n",
        "main Java source root",
    )
    control = control.replace(
        "        java {\n",
        "        java {\n            srcDir 'src'\n"
        "            // Preserve the exact protoc Java inputs without plugin wiring.\n"
        "            srcDir new File(buildDir, 'generated/source/proto/main/java')\n",
        1,
    )
    compile_dependencies = (
        "tasks.named('compileJava') {\n"
        "    dependsOn tasks.named('generateProtoSchemaHash')\n"
        "    dependsOn tasks.named('generateProtoJavaBindings')\n"
        "    dependsOn tasks.named('generateProtoDescriptorHash')\n"
        "}\n\n"
    )
    control = remove_exactly_once(control, compile_dependencies, "compileJava protobuf dependencies")

    require("id 'com.google.protobuf'" not in control,
            "control still applies the protobuf Gradle plugin")
    require("protobuf-java:${protobufVersion}" in control,
            "control changed the frozen protobuf runtime dependency")
    require("generateProto" not in control,
            "control still references generated protobuf tasks")
    require("srcDir 'test'" in control and "srcDir 'test-resources'" in control,
            "control changed frozen Java test inputs")
    require("srcDir 'resources'" in control and "srcDir 'src'" in control,
            "control changed Java production/resource inputs")
    require("generated/source/proto/main/java" in control,
            "control lacks neutral generated Java source registration")

    invariants = [
        "protobuf plugin declaration removed",
        "protobuf plugin and generation task block removed",
        "protobuf-only proto source-set DSL removed",
        "protoc Java output explicitly retained as a Java source root",
        "protobuf-java dependency block retained byte-for-byte",
        "Java Test/task/JaCoCo configuration retained byte-for-byte",
        "hand-written Java/test/resource source roots retained",
    ]
    require(section(original, "dependencies {", "// Project sources") ==
            section(control, "dependencies {", "// Project sources"),
            "dependency declarations changed in the control")
    require(section_to_end(original, "// Test coverage configuration") ==
            section_to_end(control, "// Test coverage configuration"),
            "test and JaCoCo configuration changed in the control")
    return control, invariants


def generated_source_manifest(project: Path) -> dict[str, dict[str, str]]:
    manifest: dict[str, dict[str, str]] = {}
    for relative in GENERATED_SOURCE_DIRS:
        root = project / "build" / relative
        require(root.is_dir(), f"frozen generated Java input is missing: {root}")
        files = {
            path.relative_to(root).as_posix(): sha256_file(path)
            for path in sorted(root.rglob("*.java"))
            if path.is_file()
        }
        require(bool(files), f"frozen generated Java input is empty: {root}")
        manifest[relative] = files
    return manifest


def tree_hashes(stage_project: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for relative in ("src", "test", "resources", "test-resources"):
        root = stage_project / relative
        require(root.is_dir(), f"frozen source/test directory is missing: {root}")
        entries = {
            path.relative_to(root).as_posix(): sha256_file(path)
            for path in sorted(root.rglob("*")) if path.is_file()
        }
        result[relative] = sha256_bytes(json.dumps(entries, sort_keys=True).encode())
    return result


def materialize_isolated_project(output_root: Path, stage_project: Path,
                                 original_build: str, control_build: str) -> Path:
    isolated_parent = output_root / "isolated-project"
    isolated_project = isolated_parent / "ClangAstParser"
    isolated_parent.mkdir(parents=True)
    isolated_project.mkdir()

    for item in sorted(stage_project.iterdir()):
        if item.name in {".gradle", ".git", "build", "build.gradle", "settings.gradle"}:
            continue
        target = isolated_project / item.name
        target.symlink_to(item.resolve(), target_is_directory=item.is_dir())

    (isolated_project / "settings.gradle").write_text(
        (stage_project / "settings.gradle").read_text(encoding="utf-8"), encoding="utf-8"
    )
    (isolated_project / "build.gradle").write_text(original_build, encoding="utf-8")

    clava_ast = stage_project.parent / "ClavaAst"
    (isolated_parent / "ClavaAst").symlink_to(clava_ast.resolve(), target_is_directory=True)

    for relative in GENERATED_SOURCE_DIRS:
        source = stage_project / "build" / relative
        target = isolated_project / "build" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(source, target)

    (output_root / "arms").mkdir()
    (output_root / "arms" / "original-build.gradle").write_text(
        original_build, encoding="utf-8"
    )
    (output_root / "arms" / "control-build.gradle").write_text(
        control_build, encoding="utf-8"
    )
    return isolated_project


def frozen_inputs(matrix_path: Path, worker_evidence_path: Path
                  ) -> tuple[dict, dict, dict, dict]:
    matrix, stages, identity, _ = runtime_timeline.load_and_verify(
        matrix_path, worker_evidence_path
    )
    stage = stages["protobuf"]
    return matrix, stage, identity, stages


def prepare(output_root: Path, matrix_path: Path,
            worker_evidence_path: Path) -> dict:
    require(not output_root.exists(), f"refusing to reuse intervention root: {output_root}")
    matrix, stage, identity, _ = frozen_inputs(matrix_path, worker_evidence_path)
    stage_project = Path(stage["root"]) / "clava" / "ClangAstParser"
    original_build = (stage_project / "build.gradle").read_text(encoding="utf-8")
    control_build, invariants = disable_protobuf_build_integration(original_build)
    generated = generated_source_manifest(stage_project)
    source_trees = tree_hashes(stage_project)
    isolated_project = materialize_isolated_project(
        output_root, stage_project, original_build, control_build
    )

    plan = {
        "schema_version": 1,
        "experiment": "Java protobuf Gradle configuration intervention",
        "diagnostic_only_not_headline": True,
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "status": "prepared_not_run",
        "source_matrix": str(matrix_path.resolve()),
        "source_matrix_sha256": sha256_file(matrix_path),
        "source_worker_evidence": str(worker_evidence_path.resolve()),
        "source_worker_evidence_sha256": sha256_file(worker_evidence_path),
        "frozen_stage": {
            "key": stage["key"],
            "root": stage["root"],
            "clava_revision": stage["clava_revision"],
            "native_binary_sha256": stage["native_binary_sha256"],
            "runtime_manifest_sha256": stage["runtime_manifest_sha256"],
            "parser_jar_sha256": stage["parser_jar_sha256"],
            "expected_test_count": identity["expected_test_count"],
            "expected_test_identity_sha256": identity["expected_test_identity_sha256"],
        },
        "isolated_project": str(isolated_project.resolve()),
        "fixed_root_for_both_arms": True,
        "settings_file_sha256": sha256_file(stage_project / "settings.gradle"),
        "protobuf_build_file_sha256": sha256_file(stage_project / "build.gradle"),
        "control_build_file_sha256": sha256_bytes(control_build.encode()),
        "control_invariants": invariants,
        "source_tree_hashes": source_trees,
        "generated_source_tree_manifest": generated,
        "generated_source_tree_sha256": {
            name: sha256_bytes(json.dumps(files, sort_keys=True).encode())
            for name, files in generated.items()
        },
        "classpath_and_test_inputs": {
            "protobuf_runtime_dependency_retained": True,
            "dependency_block_identical": True,
            "test_source_dirs_identical": True,
            "test_resource_dirs_identical": True,
            "source_files_symlinked_to_frozen_stage": True,
            "generated_java_sources_copied_from_frozen_stage": True,
            "test_worker_spawned": False,
            "test_actions_run": False,
        },
        "intervention": {
            "treatment": "protobuf plugin plus schema/binding generation configuration disabled in a copied build.gradle",
            "control": "same isolated project/build-file path with original protobuf build.gradle restored",
            "preserved": [
                "protobuf-java dependency declaration and dependency resolution context",
                "hand-written source, test, and resource roots and exact frozen file contents",
                "existing protoc-generated Java files and Java source roots",
                "Java Test, JaCoCo, report-skip, and fixed-test filter configuration",
                "same settings.gradle, included ClavaAst build, Gradle user home, JVM settings, environment, and init scripts",
            ],
            "disabled": [
                "com.google.protobuf Gradle plugin 0.9.4 application",
                "protobuf plugin DSL and generated task graph",
                "custom schema-hash/protoc/bindings/hash task registrations and compileJava dependencies",
                "plugin-provided proto source-set DSL; protoc Java output root remains explicitly registered",
            ],
        },
        "command_contract": {
            "task": "test --dry-run",
            "gradle_flags": ["--no-daemon", "--offline", "--dry-run", "--info"],
            "init_scripts": [
                str((SCRIPT_ROOT / "java-suite.init.gradle").resolve()),
                str((SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle").resolve()),
                str((SCRIPT_ROOT / "java-runtime-timeline.init.gradle").resolve()),
            ],
            "primary_metric": "projects_loaded to projects_evaluated monotonic elapsed seconds",
            "secondary_metrics": ["task graph creation", "task count and paths", "whole dry-run process wall"],
            "worker_agent": "off",
            "explicit_gc_override": "none",
            "compilation_or_test_actions": "forbidden; Gradle --dry-run plus log/event gates",
        },
        "preflight_invocations": ["original", "control"],
        "measured_pair_schedule": [
            {"pair": pair, "order": list(order)} for pair, order in SCHEDULE
        ],
        "host_lock": None,
        "results": [],
        "paired_summary": None,
    }
    write_json(output_root / "plan.json", plan)
    return plan


def verify_isolated_inputs(plan: dict, output_root: Path,
                           stage: dict, matrix: dict) -> Path:
    project = Path(plan["isolated_project"])
    stage_project = Path(stage["root"]) / "clava" / "ClangAstParser"
    require(project.is_dir(), f"isolated project disappeared: {project}")
    require(sha256_file(stage_project / "build.gradle") ==
            plan["protobuf_build_file_sha256"],
            "frozen protobuf stage build.gradle changed")
    require(sha256_file(stage_project / "settings.gradle") ==
            plan["settings_file_sha256"], "frozen protobuf settings.gradle changed")
    require(tree_hashes(stage_project) == plan["source_tree_hashes"],
            "frozen protobuf source/test/resource inputs changed")
    require((project.parent / "ClavaAst").resolve() ==
            (stage_project.parent / "ClavaAst").resolve(),
            "isolated included ClavaAst build no longer resolves to the frozen stage")
    require(str(project) == plan["isolated_project"],
            "the isolated project root changed between arms")
    require(matrix["identity_preflight"]["reference_test_ids"]["java"]["sha256"] ==
            plan["frozen_stage"]["expected_test_identity_sha256"],
            "frozen Java test identity changed")
    return project


def build_command(project: Path, stage: dict, run_dir: Path) -> list[str]:
    gradle = shutil.which("gradle")
    require(gradle is not None, "gradle was not found on PATH")
    project_cache = run_dir.parents[2] / "shared-project-cache"
    command = [
        gradle,
        "--no-daemon",
        "--offline",
        "--dry-run",
        "--info",
        "--project-cache-dir",
        str(project_cache),
    ]
    for init_script in (
        SCRIPT_ROOT / "java-suite.init.gradle",
        SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle",
        SCRIPT_ROOT / "java-runtime-timeline.init.gradle",
    ):
        command.extend(["--init-script", str(init_script.resolve())])
    command.extend([
        f"-PclangDumperRoot={stage['native_root']}",
        "-p", str(project), "test",
    ])
    return command


def command_environment(stage: dict, run_dir: Path,
                        output_root: Path, inherited: list[str]) -> dict[str, str]:
    temp_root = output_root / "runstate" / "tmp"
    xdg_root = output_root / "runstate" / "xdg-cache"
    diagnostic_root = run_dir / "gradle-output"
    for path in (temp_root, xdg_root, diagnostic_root):
        path.mkdir(parents=True, exist_ok=True)
    properties = [f"-Djava.io.tmpdir={temp_root}", "-Dclava.astWire=protobuf"]
    options = inherited + properties
    require(sum(option.startswith("-Djava.io.tmpdir=") for option in options) == 1
            and sum(option.startswith("-Dclava.astWire=") for option in options) == 1,
            "intervention JVM options repeat or omit an owned system property")
    environment = os.environ.copy()
    environment.update({
        "TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(xdg_root),
        "JAVA_TOOL_OPTIONS": runtime_timeline.shlex.join(options),
        "DEADLINE_JACOCO_AGENT": "off",
        "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic_root),
        "DEADLINE_JAVA_TIMELINE_PATH": str(run_dir / "gradle-events.jsonl"),
        "SPECS_JAVA_LIBS_HOME": str(runtime_timeline.control.SPECS_JAVA_LIBS_ROOT.resolve()),
        "LARA_FRAMEWORK_HOME": str(runtime_timeline.control.LARA_FRAMEWORK_ROOT.resolve()),
    })
    return environment


def root_event(events: list[dict], event_name: str) -> dict:
    matches = [event for event in events
               if event.get("source") == "gradle"
               and event.get("event") == event_name
               and event.get("root_name") == "ClangAstParser"]
    require(len(matches) == 1,
            f"expected one root {event_name} event, found {len(matches)}")
    return matches[0]


def validate_dry_run(log_path: Path, events_path: Path) -> dict:
    log = log_path.read_text(encoding="utf-8", errors="replace")
    for marker in WORKER_MARKERS:
        require(marker not in log, f"dry-run unexpectedly spawned a test worker ({marker})")
    require("BUILD SUCCESSFUL" in log, "Gradle dry-run did not report BUILD SUCCESSFUL")
    task_statuses = [match.group(2) for line in log.splitlines()
                     if (match := TASK_STATUS.match(line))]
    require(bool(task_statuses), "dry-run log contained no Gradle task status lines")
    require(all(status == "SKIPPED" for status in task_statuses),
            f"dry-run executed or inspected a task action: {task_statuses}")

    events = [json.loads(line) for line in events_path.read_text(encoding="utf-8").splitlines()
              if line.strip()]
    loaded = root_event(events, "projects_loaded")
    evaluated = root_event(events, "projects_evaluated")
    graph = root_event(events, "task_graph_ready")
    finished = root_event(events, "build_finished")
    require(loaded["monotonic_ns"] < evaluated["monotonic_ns"] < graph["monotonic_ns"],
            "Gradle project/task-graph lifecycle timestamps are not ordered")
    require(finished.get("failed") is False, "Gradle build-finished event reports failure")
    require(":test" in graph.get("task_paths", []),
            "dry-run task graph did not include the Java test task")
    after_events = [event for event in events
                    if event.get("event") == "task_after_execute"
                    and event.get("root_name") == "ClangAstParser"]
    return {
        "project_configuration_s": (
            evaluated["monotonic_ns"] - loaded["monotonic_ns"]
        ) / 1_000_000_000,
        "task_graph_creation_s": (
            graph["monotonic_ns"] - evaluated["monotonic_ns"]
        ) / 1_000_000_000,
        "task_count": graph.get("task_count"),
        "task_paths": graph.get("task_paths", []),
        "task_after_execute_count": len(after_events),
        "all_tasks_dry_run_skipped": True,
        "task_action_event_gate": "no Test worker markers; every Gradle task log line says SKIPPED",
        "test_worker_spawned": False,
        "test_actions_run": False,
    }


def run_once(plan: dict, matrix: dict, stage: dict, output_root: Path,
             project: Path, arm: str, label: str, inherited: list[str]) -> dict:
    run_dir = output_root / "runs" / label / arm
    run_dir.mkdir(parents=True, exist_ok=False)
    build_path = project / "build.gradle"
    arm_build = output_root / "arms" / f"{arm}-build.gradle"
    expected_digest = sha256_file(arm_build)
    build_path.write_bytes(arm_build.read_bytes())
    require(sha256_file(build_path) == expected_digest,
            f"could not switch isolated build.gradle to arm {arm}")

    environment = command_environment(stage, run_dir, output_root, inherited)
    command = build_command(project, stage, run_dir)
    (run_dir / "command.json").write_text(json.dumps({
        "command": command,
        "cwd": str(project.parent),
        "arm": arm,
        "isolated_project": str(project),
        "build_file_sha256": expected_digest,
    }, indent=2) + "\n", encoding="utf-8")
    started = time.perf_counter()
    with (run_dir / "gradle.log").open("w", encoding="utf-8") as log:
        process = subprocess.run(command, cwd=project.parent, env=environment,
                                 stdout=log, stderr=subprocess.STDOUT, check=False)
    wall_elapsed = time.perf_counter() - started
    require(process.returncode == 0,
            f"Gradle {arm} config-only command failed with {process.returncode}; see {run_dir}")
    phases = validate_dry_run(run_dir / "gradle.log", run_dir / "gradle-events.jsonl")
    require(sha256_file(build_path) == expected_digest,
            f"isolated {arm} build.gradle changed during Gradle configuration")
    row = {
        "label": label,
        "arm": arm,
        "valid": True,
        "return_code": process.returncode,
        "whole_command_wall_s": wall_elapsed,
        "phases": phases,
        "isolated_project": str(project),
        "build_file_sha256": expected_digest,
        "run_dir": str(run_dir),
    }
    write_json(run_dir / "summary.json", row)
    return row


def paired_summary(rows: list[dict]) -> dict:
    pairs = []
    for pair_number, _ in SCHEDULE:
        label = f"pair-{pair_number:02d}"
        arm_rows = [row for row in rows if row["label"] == label]
        by_arm = {row["arm"]: row for row in arm_rows}
        require(set(by_arm) == set(ARMS), f"paired results missing an arm in {label}")
        original = by_arm["original"]["phases"]
        control = by_arm["control"]["phases"]
        pairs.append({
            "pair": pair_number,
            "order": [row["arm"] for row in arm_rows],
            "control_minus_original_project_configuration_s": (
                control["project_configuration_s"] - original["project_configuration_s"]
            ),
            "original_minus_control_project_configuration_s": (
                original["project_configuration_s"] - control["project_configuration_s"]
            ),
            "control_minus_original_task_graph_s": (
                control["task_graph_creation_s"] - original["task_graph_creation_s"]
            ),
            "control_minus_original_whole_command_wall_s": (
                by_arm["control"]["whole_command_wall_s"]
                - by_arm["original"]["whole_command_wall_s"]
            ),
            "original_task_count": original["task_count"],
            "control_task_count": control["task_count"],
        })
    config_deltas = [row["control_minus_original_project_configuration_s"] for row in pairs]
    graph_deltas = [row["control_minus_original_task_graph_s"] for row in pairs]
    wall_deltas = [row["control_minus_original_whole_command_wall_s"] for row in pairs]
    measured_rows = [row for row in rows if row.get("measured")]
    original_walls = [row["whole_command_wall_s"] for row in measured_rows
                      if row["arm"] == "original"]
    control_walls = [row["whole_command_wall_s"] for row in measured_rows
                     if row["arm"] == "control"]
    return {
        "pairs": pairs,
        "median_control_minus_original_project_configuration_s": statistics.median(config_deltas),
        "median_original_minus_control_project_configuration_s": -statistics.median(config_deltas),
        "median_control_minus_original_task_graph_s": statistics.median(graph_deltas),
        "median_control_minus_original_whole_command_wall_s": statistics.median(wall_deltas),
        "median_original_whole_command_wall_s": statistics.median(original_walls),
        "median_control_whole_command_wall_s": statistics.median(control_walls),
        "n_pairs": len(pairs),
        "direction_consistent": all(value < 0 for value in config_deltas)
                              or all(value > 0 for value in config_deltas),
        "interpretation_boundary": (
            "This estimates the contribution of protobuf plugin plus generation-task configuration "
            "to Gradle project configuration in a no-action dry run. It does not measure parser, "
            "native, compile, or Test-worker cost, and cannot attribute effects among the plugin "
            "and custom generation wiring separately."
        ),
    }


def results_csv_rows(preflight: list[dict], measured: list[dict]) -> list[dict]:
    rows = []
    for item in [*preflight, *measured]:
        rows.append({
            "label": item["label"],
            "arm": item["arm"],
            "measured": item.get("measured", False),
            "valid": item["valid"],
            "return_code": item.get("return_code"),
            "whole_command_wall_s": item.get("whole_command_wall_s"),
            "project_configuration_s": item["phases"]["project_configuration_s"],
            "task_graph_creation_s": item["phases"]["task_graph_creation_s"],
            "task_count": item["phases"]["task_count"],
            "task_after_execute_count": item["phases"]["task_after_execute_count"],
            "all_tasks_dry_run_skipped": item["phases"]["all_tasks_dry_run_skipped"],
            "test_worker_spawned": item["phases"]["test_worker_spawned"],
            "test_actions_run": item["phases"]["test_actions_run"],
            "build_file_sha256": item["build_file_sha256"],
            "run_dir": item["run_dir"],
        })
    return rows


def write_results_csv(path: Path, rows: list[dict]) -> None:
    fields = (
        "label", "arm", "measured", "valid", "return_code", "whole_command_wall_s",
        "project_configuration_s", "task_graph_creation_s", "task_count",
        "task_after_execute_count", "all_tasks_dry_run_skipped", "test_worker_spawned",
        "test_actions_run", "build_file_sha256", "run_dir",
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def export_completed(output_root: Path) -> dict:
    plan_path = output_root / "plan.json"
    require(plan_path.is_file(), f"completed plan missing: {plan_path}")
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    require(plan.get("status") == "complete", "cannot export an incomplete intervention")
    preflight = plan.get("preflight_results", [])
    measured = plan.get("results", [])
    require(len(preflight) == 2 and len(measured) == 8,
            "completed intervention row counts do not match 2 preflight + 8 measured")
    stage_project = Path(plan["frozen_stage"]["root"]) / "clava" / "ClangAstParser"
    isolated_project = Path(plan["isolated_project"])
    require(sha256_file(stage_project / "build.gradle") ==
            plan["protobuf_build_file_sha256"],
            "post-run frozen protobuf build.gradle hash changed")
    require(sha256_file(stage_project / "settings.gradle") ==
            plan["settings_file_sha256"]
            and sha256_file(isolated_project / "settings.gradle") ==
            plan["settings_file_sha256"],
            "post-run frozen or isolated settings.gradle hash changed")
    require(tree_hashes(stage_project) == plan["source_tree_hashes"],
            "post-run frozen source/test/resource hashes changed")
    require(generated_source_manifest(stage_project) ==
            plan["generated_source_tree_manifest"]
            and generated_source_manifest(isolated_project) ==
            plan["generated_source_tree_manifest"],
            "post-run frozen/copied generated Java source hashes changed")
    require(sha256_file(isolated_project / "build.gradle") ==
            plan["protobuf_build_file_sha256"]
            and plan.get("isolated_build_file_restored_to_original") is True,
            "isolated build.gradle was not restored after measurements")
    require(all(row.get("valid") is True
                and row["phases"].get("all_tasks_dry_run_skipped") is True
                and row["phases"].get("test_worker_spawned") is False
                and row["phases"].get("test_actions_run") is False
                for row in [*preflight, *measured]),
            "post-run no-worker/no-task-action evidence gate failed")
    summary = paired_summary([*preflight, *measured])
    plan["paired_summary"] = summary
    plan["results_csv"] = str((output_root / "results.csv").resolve())
    plan["postrun_integrity"] = {
        "frozen_source_test_resource_tree_hashes_match_preparation": True,
        "frozen_generated_java_hashes_match_preparation": True,
        "isolated_generated_java_hashes_match_frozen_stage": True,
        "frozen_build_and_settings_hashes_match_preparation": True,
        "isolated_build_file_restored_to_frozen_original": True,
        "all_rows_no_worker_no_task_action_gates_passed": True,
    }
    write_json(plan_path, plan)
    write_results_csv(output_root / "results.csv", results_csv_rows(preflight, measured))
    results = {
        "schema_version": 1,
        "diagnostic_only_not_headline": True,
        "experiment": plan["experiment"],
        "plan_sha256": sha256_file(plan_path),
        "results_csv": str((output_root / "results.csv").resolve()),
        "results_csv_sha256": sha256_file(output_root / "results.csv"),
        "results": measured,
        "preflight_results": preflight,
        "paired_summary": summary,
        "postrun_integrity": plan["postrun_integrity"],
    }
    write_json(output_root / "results.json", results)
    return results


def recover_collector_rejected_preflight(output_root: Path, plan: dict) -> dict:
    """Adopt a successful dry-run rejected only by the previous log parser."""
    expected_failure = (
        "RuntimeError: dry-run log contained no Gradle task status lines"
    )
    require(plan.get("failure") == expected_failure,
            "refusing to resume a failed intervention unless only the known collector check failed")
    run_dir = output_root / "runs" / "preflight" / "original"
    command_path = run_dir / "command.json"
    require(command_path.is_file(), "collector-recovery original preflight command is missing")
    command = json.loads(command_path.read_text(encoding="utf-8"))
    require(command.get("arm") == "original"
            and command.get("build_file_sha256") == plan["protobuf_build_file_sha256"],
            "collector-recovery preflight is not the frozen original arm")
    require(not (output_root / "runs" / "preflight" / "control").exists(),
            "unexpected control preflight exists; refusing ambiguous recovery")
    phases = validate_dry_run(run_dir / "gradle.log", run_dir / "gradle-events.jsonl")
    return {
        "label": "preflight",
        "arm": "original",
        "measured": False,
        "valid": True,
        "return_code": 0,
        "return_code_observed_before_collector_failure": True,
        "whole_command_wall_s": None,
        "whole_command_wall_note": "not persisted before collector failure; phase timing is recovered from raw Gradle events",
        "phases": phases,
        "isolated_project": plan["isolated_project"],
        "build_file_sha256": plan["protobuf_build_file_sha256"],
        "run_dir": str(run_dir),
        "recovered_existing_raw_run": True,
    }


def measure(output_root: Path, matrix_path: Path, worker_evidence_path: Path,
            host_lock_note: str) -> dict:
    plan_path = output_root / "plan.json"
    require(plan_path.is_file(), f"prepared plan missing: {plan_path}")
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    recover_original_preflight = plan.get("status") == "failed"
    require(plan.get("status") == "prepared_not_run" or recover_original_preflight,
            f"intervention plan is not runnable: {plan.get('status')}")
    require(host_lock_note.strip(), "a parent host-lock note is required to run Gradle")
    require(sha256_file(matrix_path) == plan["source_matrix_sha256"],
            "frozen Java matrix manifest changed after preparation")
    require(sha256_file(worker_evidence_path) == plan["source_worker_evidence_sha256"],
            "frozen Java worker evidence changed after preparation")

    matrix, stage, _, _ = frozen_inputs(matrix_path, worker_evidence_path)
    project = verify_isolated_inputs(plan, output_root, stage, matrix)
    inherited, inherited_audit = runtime_timeline.inherited_java_options()
    plan["host_lock"] = {
        "confirmed": True,
        "note": host_lock_note,
        "recorded_at": dt.datetime.now(dt.timezone.utc).isoformat(),
    }
    plan["inherited_jvm_environment_audit"] = inherited_audit
    plan["status"] = "running"
    rows: list[dict] = []
    if recover_original_preflight:
        recovered = recover_collector_rejected_preflight(output_root, plan)
        rows.append(recovered)
        plan["preflight_results"] = rows.copy()
        plan["collector_recovery"] = {
            "adopted_raw_original_preflight": True,
            "reason": "Gradle 9.6.1 logs dry-run tasks as ':task SKIPPED' and emits no TaskExecutionListener completion callbacks",
            "reran_preflight": False,
        }
        plan.pop("failure", None)
    write_json(plan_path, plan)

    original_path = output_root / "arms" / "original-build.gradle"
    try:
        for arm in (("control",) if recover_original_preflight else ("original", "control")):
            row = run_once(plan, matrix, stage, output_root, project, arm,
                           "preflight", inherited)
            row["measured"] = False
            rows.append(row)
            plan["preflight_results"] = rows.copy()
            write_json(plan_path, plan)

        for pair_number, order in SCHEDULE:
            label = f"pair-{pair_number:02d}"
            for arm in order:
                row = run_once(plan, matrix, stage, output_root, project, arm,
                               label, inherited)
                row["measured"] = True
                rows.append(row)
                plan["results"] = [item for item in rows if item.get("measured")]
                write_json(plan_path, plan)

        plan["paired_summary"] = paired_summary(rows)
        plan["status"] = "complete"
        plan["completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
    except Exception as error:
        plan["status"] = "failed"
        plan["failure"] = f"{type(error).__name__}: {error}"
        plan["results"] = [item for item in rows if item.get("measured")]
        plan["completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
        raise
    finally:
        build_path = project / "build.gradle"
        build_path.write_bytes(original_path.read_bytes())
        plan["isolated_build_file_restored_to_original"] = (
            sha256_file(build_path) == plan["protobuf_build_file_sha256"]
        )
        write_json(plan_path, plan)

    return export_completed(output_root)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="action", required=True)
    prepare_parser = commands.add_parser("prepare", help="freeze inputs and materialize the isolated project")
    prepare_parser.add_argument("--output-root", type=Path, default=OUTPUT_ROOT)
    prepare_parser.add_argument("--matrix", type=Path, default=MATRIX_PATH)
    prepare_parser.add_argument("--worker-evidence", type=Path, default=WORKER_EVIDENCE_PATH)
    measure_parser = commands.add_parser("measure", help="run guarded config-only Gradle pairs")
    measure_parser.add_argument("--output-root", type=Path, default=OUTPUT_ROOT)
    measure_parser.add_argument("--matrix", type=Path, default=MATRIX_PATH)
    measure_parser.add_argument("--worker-evidence", type=Path, default=WORKER_EVIDENCE_PATH)
    measure_parser.add_argument("--host-lock-confirmed", action="store_true")
    measure_parser.add_argument("--host-lock-note", default="")
    export_parser = commands.add_parser("export", help="derive CSV and paired wall summaries from completed raw rows")
    export_parser.add_argument("--output-root", type=Path, default=OUTPUT_ROOT)
    args = parser.parse_args()

    if args.action == "prepare":
        plan = prepare(args.output_root.resolve(), args.matrix.resolve(),
                       args.worker_evidence.resolve())
        print(json.dumps({
            "status": plan["status"],
            "output_root": str(args.output_root.resolve()),
            "isolated_project": plan["isolated_project"],
            "control_build_file_sha256": plan["control_build_file_sha256"],
        }, indent=2))
        return

    if args.action == "export":
        results = export_completed(args.output_root.resolve())
        print(json.dumps(results["paired_summary"], indent=2))
        return
    if not args.host_lock_confirmed:
        raise SystemExit("refusing Gradle runs until the parent explicitly confirms host release")
    results = measure(args.output_root.resolve(), args.matrix.resolve(),
                      args.worker_evidence.resolve(), args.host_lock_note)
    print(json.dumps(results["paired_summary"], indent=2))


if __name__ == "__main__":
    main()
