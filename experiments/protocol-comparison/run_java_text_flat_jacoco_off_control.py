#!/usr/bin/env python3
"""Compare the warm Text and FlatBuffers Java suites with the JaCoCo agent off."""

from __future__ import annotations

import argparse
import datetime as dt
import getpass
import json
import os
from pathlib import Path
import shutil
import statistics
import subprocess
import sys
import time

sys.dont_write_bytecode = True

import run_comparison as base
import run_java_jacoco_agent_control as control


SCRIPT_ROOT = Path(__file__).resolve().parent
MATRIX = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/results.json"
DEFAULT_OUTPUT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-text-flat-jacoco-off-r1"
STAGES = ("ccache-text", "flatbuffers")
ORDER = (
    ("ccache-text", 1), ("flatbuffers", 1),
    ("flatbuffers", 2), ("ccache-text", 2),
    ("ccache-text", 3), ("flatbuffers", 3),
    ("flatbuffers", 4), ("ccache-text", 4),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=MATRIX)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--resume-existing", action="store_true",
                        help="resume the preserved Text/Flat control after a pre-test configuration failure")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def load_inputs(matrix_path: Path) -> tuple[dict, dict[str, dict], dict[str, Path]]:
    matrix = json.loads(matrix_path.read_text())
    if matrix.get("identity_preflight", {}).get("passed") is not True:
        raise RuntimeError("primary matrix identity preflight did not pass")
    if matrix.get("plan", {}).get("expected_tests", {}).get("java") != control.EXPECTED_JAVA_PLAN:
        raise RuntimeError("primary matrix does not declare the exact 116-pass Java suite")
    reference = matrix["identity_preflight"]["reference_test_ids"]["java"]
    if reference.get("count") != 116:
        raise RuntimeError("primary Java test identity does not contain exactly 116 cases")
    stages = {key: matrix["plan"]["stages"][key] for key in STAGES}
    source_caches: dict[str, Path] = {}
    for key in STAGES:
        for stage in (row for row in matrix["results"] if row.get("suite") == "java"
                      and row.get("mode") == "warm" and row.get("measured") is True
                      and row.get("stage") == key):
            if (not stage.get("valid") or any(stage.get(name) != value
                                              for name, value in control.EXPECTED_JAVA.items())):
                raise RuntimeError(f"invalid primary warm Java row in {key}: {stage.get('run_dir')}")
            observed = {
                "cacheable_calls": int(stage.get("cacheable_calls", 0)),
                "hits": int(stage.get("cache_hits", 0)),
                "misses": int(stage.get("cache_misses", 0)),
                "uncacheable_calls": int(stage.get("cache_validation", {}).get("uncacheable_calls", 0)),
            }
            if observed != control.EXPECTED_PRIMARY_CACHE:
                raise RuntimeError(f"primary warm cache outcome changed in {key}: {observed}")
        measured = [row for row in matrix["results"] if row.get("suite") == "java"
                    and row.get("mode") == "warm" and row.get("measured") is True
                    and row.get("stage") == key]
        if len(measured) != 4:
            raise RuntimeError(f"expected four primary warm rows for {key}, found {len(measured)}")
        paths = {Path(row["cache_validation"]["cache_dir"]).resolve() for row in measured}
        if len(paths) != 1:
            raise RuntimeError(f"primary warm runs do not share one cache directory in {key}")
        source_caches[key] = next(iter(paths))
        control.verify_stage(stages[key])
    return matrix, stages, source_caches


def clone_cache(source: Path, target: Path) -> tuple[str, str]:
    if not source.is_dir() or target.exists() or target.resolve() == source.resolve():
        raise RuntimeError(f"invalid cache clone paths: {source} -> {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    before = control.cache_manifest(source)
    shutil.copytree(source, target)
    after = control.cache_manifest(target)
    if before != after:
        raise RuntimeError(f"copied warm cache does not match primary payload: {source}")
    return control.canonical_hash(before), target.name


def summarize(rows: list[dict]) -> dict:
    metrics = ("elapsed_s", "junit_aggregate_s", "wall_minus_junit_residual_s")
    paired = {}
    stage_medians = {}
    for metric in metrics:
        deltas = []
        for round_number in range(1, 5):
            text = next(row for row in rows if row["stage"] == "ccache-text" and row["round"] == round_number)
            flat = next(row for row in rows if row["stage"] == "flatbuffers" and row["round"] == round_number)
            deltas.append(float(flat[metric]) - float(text[metric]))
        paired[metric] = {"flat_minus_text_by_round": deltas,
                          "median_flat_minus_text": statistics.median(deltas)}
    for stage in STAGES:
        stage_medians[stage] = {
            metric: statistics.median(float(row[metric]) for row in rows if row["stage"] == stage)
            for metric in metrics
        }
    return {
        "control": "JaCoCo Test-worker agent off for both formats; report tasks disabled for both",
        "n_per_stage": 4,
        "units": "seconds; positive Flat-minus-Text means FlatBuffers took longer",
        "residual_definition": "max(0, elapsed_s - JUnit aggregate); timing-boundary residual, not a Gradle task timer",
        "stage_medians": stage_medians,
        "paired_flat_minus_text": paired,
    }


def main() -> int:
    args = parse_args()
    matrix_path = args.matrix.resolve()
    output_root = args.output_root.resolve()
    if output_root.exists() and not (args.dry_run or args.resume_existing):
        raise SystemExit(f"refusing to reuse diagnostic output root: {output_root}")
    if args.resume_existing and not output_root.is_dir():
        raise SystemExit(f"--resume-existing requires an existing output root: {output_root}")
    matrix, stages, source_caches = load_inputs(matrix_path)
    reference = matrix["identity_preflight"]["reference_test_ids"]["java"]
    inherited_jvm_options = control.audit_inherited_jvm_options()
    if args.dry_run:
        print(json.dumps({"matrix": str(matrix_path), "output_root": str(output_root),
                          "rounds": 4, "command_count": len(ORDER),
                          "cache_namespaces_from_primary": {key: source_caches[key].name for key in STAGES},
                          "order": [{"ordinal": index + 1, "round": round_number, "stage": stage,
                                     "agent": "off"} for index, (stage, round_number) in enumerate(ORDER)]}, indent=2))
        return 0

    temp_roots = {key: output_root / "temp/java" / key for key in STAGES}
    cache_dirs = {}
    cache_manifests = {}
    if args.resume_existing:
        plan_path = output_root / "plan.json"
        results_path = output_root / "results.json"
        if not plan_path.is_file() or not results_path.is_file():
            raise RuntimeError("existing output root is missing its plan or partial results")
        plan = json.loads(plan_path.read_text())
        matrix_sha256 = base.sha256_file(matrix_path)
        if plan.get("primary_matrix_sha256") != matrix_sha256:
            raise RuntimeError("resume primary matrix differs from the frozen run")
        result_data = json.loads(results_path.read_text())
        if result_data.get("plan", {}).get("primary_matrix_sha256") != matrix_sha256:
            raise RuntimeError("partial results do not belong to the frozen primary matrix")
        results = result_data.get("results", [])
        if not results or len(results) >= len(ORDER):
            raise RuntimeError(f"resume requires an incomplete result prefix; found {len(results)} rows")
        for index, row in enumerate(results):
            if (row.get("ordinal") != index + 1 or row.get("stage") != ORDER[index][0]
                    or row.get("round") != ORDER[index][1] or row.get("valid") is not True):
                raise RuntimeError(f"existing results are not a valid fixed-order prefix at cell {index + 1}")
        worker_baseline: dict[str, dict] = {}
        for key in STAGES:
            stage_plan = plan.get("stages", {}).get(key, {})
            if Path(stage_plan.get("cache_source", "")).resolve() != source_caches[key]:
                raise RuntimeError(f"primary source cache path changed for {key}")
            cache_dirs[key] = Path(stage_plan.get("cache_clone", "")).resolve()
            if not cache_dirs[key].is_dir() or output_root not in cache_dirs[key].parents:
                raise RuntimeError(f"preserved cache clone is missing or outside output root: {cache_dirs[key]}")
            cache_manifests[key] = str(stage_plan.get("cache_source_manifest_sha256", ""))
            if cache_manifests[key] != control.canonical_hash(control.cache_manifest(source_caches[key])):
                raise RuntimeError(f"primary cache payload changed since original attempt for {key}")
        for row in results:
            key = row["stage"]
            worker = row["worker_configuration"]
            executor_args = row["actual_test_executor_args"]
            executor_normalized, _ = control.normalize_executor_args(executor_args)
            identity = {
                "max_heap_size": worker.get("max_heap_size"),
                "jvm_args_without_javaagent": [arg for arg in worker.get("jvm_args", [])
                                               if not arg.startswith("-javaagent:")],
                "actual_executor_args_without_javaagent": executor_normalized,
            }
            if key in worker_baseline and worker_baseline[key] != identity:
                raise RuntimeError(f"existing worker arguments are not stable for {key}")
            worker_baseline[key] = identity
    else:
        output_root.mkdir(parents=True)
        for key in STAGES:
            temp_roots[key].mkdir(parents=True)
            clone = temp_roots[key] / f"clang_ast_exe_{getpass.getuser()}" / source_caches[key].name
            manifest_sha256, _ = clone_cache(source_caches[key], clone)
            cache_dirs[key] = clone
            cache_manifests[key] = manifest_sha256
            zero = subprocess.run(["ccache", "--zero-stats"],
                                  env={**os.environ, "CCACHE_DIR": str(clone), "LC_ALL": "C"},
                                  text=True, capture_output=True, check=False)
            if zero.returncode != 0:
                raise RuntimeError(f"could not zero cloned cache stats: {zero.stderr.strip()}")

        plan = {
            "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
            "primary_matrix": str(matrix_path),
            "primary_matrix_sha256": base.sha256_file(matrix_path),
            "output_root": str(output_root),
            "stages": {key: {"identity": control.verify_stage(stages[key]),
                             "runtime_manifest_sha256": stages[key]["runtime_manifest_sha256"],
                             "native_binary_sha256": stages[key]["native_binary_sha256"],
                             "cache_source": str(source_caches[key]), "cache_clone": str(cache_dirs[key]),
                             "cache_namespace_from_primary": source_caches[key].name,
                             "cache_source_manifest_sha256": cache_manifests[key],
                             "source_metadata": stages[key]} for key in STAGES},
            "expected_java": control.EXPECTED_JAVA,
            "reference_test_ids_sha256": reference["sha256"],
            "expected_cache_per_call": control.EXPECTED_PRIMARY_CACHE,
            "inherited_jvm_environment_audit": inherited_jvm_options,
            "constant_settings": ["--info", "Gradle Test Executor -Xmx512m", "JaCoCo agent off",
                                  "JaCoCo report tasks skipped", "exact 116-test identity"],
            "order": [{"ordinal": index + 1, "round": round_number, "stage": stage,
                       "agent": "off"} for index, (stage, round_number) in enumerate(ORDER)],
        }
        (output_root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
        results: list[dict] = []
        worker_baseline: dict[str, dict] = {}
    try:
        for ordinal, (key, round_number) in enumerate(ORDER, start=1):
            if ordinal <= len(results):
                continue
            stage = stages[key]
            control.verify_stage(stage)
            run_name = f"{ordinal:02d}-r{round_number}-{key}-jacoco-off"
            run_dir = output_root / "runs/java" / run_name
            retry = 1
            while run_dir.exists():
                run_dir = output_root / "runs/java" / f"{run_name}-retry{retry}"
                retry += 1
            run_dir.mkdir(parents=True)
            diagnostic_dir = run_dir / "gradle-output"
            cache_dir = cache_dirs[key]
            stats_before = control.ccache_stats(cache_dir)
            if control.audit_inherited_jvm_options() != inherited_jvm_options:
                raise RuntimeError("inherited JVM environment changed during the control")
            java_options = os.environ.get("JAVA_TOOL_OPTIONS", "")
            java_options += f" -Djava.io.tmpdir={temp_roots[key]} -Dclava.astWire={stage['wire']}"
            environment = os.environ.copy()
            environment.update({
                "TMPDIR": str(temp_roots[key]), "TMP": str(temp_roots[key]), "TEMP": str(temp_roots[key]),
                "XDG_CACHE_HOME": str(output_root / "cache/java" / key),
                "JAVA_TOOL_OPTIONS": java_options,
                "DEADLINE_JACOCO_AGENT": "off",
                "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic_dir),
                "SPECS_JAVA_LIBS_HOME": str(control.SPECS_JAVA_LIBS_ROOT.resolve()),
                "LARA_FRAMEWORK_HOME": str(control.LARA_FRAMEWORK_ROOT.resolve()),
            })
            if key == "flatbuffers":
                environment["FLAT_NATIVE"] = str(stage["native_root"])
            command = ["/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(run_dir / "time.txt"), "--",
                       "gradle", "--no-daemon", "--offline", "--info",
                       "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
                       "--init-script", str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle")]
            if key == "protobuf":
                command.append(f"-PclangDumperRoot={stage['native_root']}")
            command.extend(["-p", "ClangAstParser", "test"])
            (run_dir / "command.json").write_text(json.dumps({"command": command,
                "cwd": str(Path(stage["root"]) / "clava"), "stage": key, "agent": "off"}, indent=2) + "\n")
            log_path = run_dir / "run.log"
            started_at = dt.datetime.now(dt.timezone.utc).isoformat()
            started = time.perf_counter()
            with log_path.open("w") as log:
                process = subprocess.run(command, cwd=Path(stage["root"]) / "clava", env=environment,
                                         stdout=log, stderr=subprocess.STDOUT, check=False)
            elapsed = time.perf_counter() - started
            finished_at = dt.datetime.now(dt.timezone.utc).isoformat()
            stats_after = control.ccache_stats(cache_dir)
            delta = {
                "cacheable_calls": stats_after["cacheable_calls"] - stats_before["cacheable_calls"],
                "hits": stats_after["hits"] - stats_before["hits"],
                "misses": stats_after["misses"] - stats_before["misses"],
                "uncacheable_calls": stats_after["uncacheable_calls"] - stats_before["uncacheable_calls"],
            }
            counts = control.junit_counts(diagnostic_dir / "junit-xml")
            ids_sha256 = control.junit_identity_sha256(diagnostic_dir / "junit-xml")
            executor_args = control.observed_test_executor_args(log_path)
            executor_normalized, response_files = control.normalize_executor_args(executor_args)
            worker_path = diagnostic_dir / "worker-configuration.json"
            worker = json.loads(worker_path.read_text()) if worker_path.is_file() else {}
            worker_args = worker.get("jvm_args", [])
            worker_non_agent = [arg for arg in worker_args if not arg.startswith("-javaagent:")]
            identity = {"max_heap_size": worker.get("max_heap_size"),
                        "jvm_args_without_javaagent": worker_non_agent,
                        "actual_executor_args_without_javaagent": executor_normalized}
            baseline = worker_baseline.setdefault(key, identity)
            stable = identity == baseline
            agent_flags = [arg for arg in worker_args
                           if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
            executor_agent_flags = [arg for arg in executor_args
                                    if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
            actual_tool_path, actual_tool_sha256 = control.observed_native_tool(stage, log_path)
            time_data = control.parse_time(run_dir / "time.txt")
            log_text = log_path.read_text(errors="replace")
            report_skips = {task: f"{task} SKIPPED" in log_text
                            for task in ("jacocoTestReport", "jacocoTestCoverageVerification")}
            compile_tasks = control.task_compilation_lines(log_path)
            test_executed = control.test_task_was_executed(log_path)
            actual_elapsed = float(time_data.get("elapsed_s", elapsed))
            row = {
                "ordinal": ordinal, "round": round_number, "stage": key, "agent": "off",
                "valid": False, "return_code": process.returncode,
                "elapsed_s": actual_elapsed, "junit_aggregate_s": counts.get("junit_aggregate_s", 0.0),
                "wall_minus_junit_residual_s": max(0.0, actual_elapsed-float(counts.get("junit_aggregate_s", 0.0))),
                "run_started_at": started_at, "run_finished_at": finished_at,
                **time_data, **counts, "cache_delta": delta,
                "cache_validation_passed": delta == control.EXPECTED_PRIMARY_CACHE,
                "test_identity_sha256": ids_sha256,
                "test_identity_match": ids_sha256 == reference["sha256"],
                "actual_native_tool": actual_tool_path, "actual_native_tool_sha256": actual_tool_sha256,
                "worker_configuration": worker, "actual_test_executor_args": executor_args,
                "actual_executor_classpath_response_files": response_files,
                "worker_args_stable_within_stage": stable,
                "only_expected_jacoco_agent": not agent_flags and not executor_agent_flags
                    and worker.get("requested_agent") == "off" and worker.get("jacoco_enabled") is False,
                "test_worker_xmx_512m": [arg for arg in executor_args if arg.startswith("-Xmx")] == ["-Xmx512m"],
                "test_task_executed": test_executed, "report_tasks_skipped": report_skips,
                "compile_tasks_not_up_to_date": compile_tasks,
                "command": command, "run_dir": str(run_dir),
            }
            row["valid"] = (
                process.returncode == 0
                and {name: row.get(name) for name in control.EXPECTED_JAVA} == control.EXPECTED_JAVA
                and row["cache_validation_passed"] and row["test_identity_match"]
                and actual_tool_sha256 == stage["native_binary_sha256"]
                and not compile_tasks and row["only_expected_jacoco_agent"]
                and row["test_worker_xmx_512m"] and stable and test_executed
                and all(report_skips.values())
            )
            (run_dir / "summary.json").write_text(json.dumps(row, indent=2) + "\n")
            results.append(row)
            (output_root / "results.json").write_text(json.dumps({"plan": plan, "results": results}, indent=2) + "\n")
            print(json.dumps({key: row[key] for key in
                ("ordinal", "round", "stage", "valid", "elapsed_s", "junit_aggregate_s",
                 "total_tests", "failed_tests", "cache_validation_passed")}), flush=True)
            if not row["valid"]:
                raise RuntimeError(f"Text/Flat Java control cell failed gates: {run_dir}")
            control.verify_stage(stage)
        summary = summarize(results)
        (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    except Exception as exc:
        (output_root / "failure.txt").write_text(f"{type(exc).__name__}: {exc}\n")
        raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
