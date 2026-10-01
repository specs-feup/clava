#!/usr/bin/env python3
"""Validate and export paired Java full-suite JaCoCo-agent control results."""
from __future__ import annotations

import argparse
import base64
import csv
import hashlib
import html
import json
import math
from pathlib import Path
import statistics
from typing import Any


ROUNDS = (1, 2, 3, 4)
FORMATS = ("text", "protobuf")
AGENTS = ("off", "on")
EXPECTED_TEST_ID = "11660f260465c336e4b34d961858247df15975ac1053daddb6ba8eb56eb03450"
EXPECTED_CACHE = {
    "cacheable_calls": 208,
    "hits": 207,
    "misses": 1,
    "uncacheable_calls": 0,
}


class AnalysisError(ValueError):
    """Raised when the measured agent-control matrix fails strict validation."""


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as content:
        for chunk in iter(lambda: content.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read {path.name}: {error}") from error
    if not isinstance(value, dict):
        raise AnalysisError(f"{path.name}: expected a JSON object")
    return value


def _run_directory(root: Path, row: dict[str, Any]) -> Path:
    stage = row.get("stage")
    if stage not in ("ccache-text", "protobuf"):
        raise AnalysisError("unexpected protocol stage")
    label = "text" if stage == "ccache-text" else "protobuf"
    return root / "runs" / "java" / (
        f"{row['ordinal']:02d}-r{row['round']}-{stage}-jacoco-{row['agent']}"
    )


def _expected_cache(row: dict[str, Any], plan: dict[str, Any]) -> dict[str, int]:
    stage_name = row["stage"]
    expected = plan.get("expected_per_command_cache_delta", {}).get(stage_name)
    if not isinstance(expected, dict):
        raise AnalysisError(f"missing expected cache counters for {stage_name}")
    return expected


def _normalized_worker_args(row: dict[str, Any]) -> tuple[str, ...]:
    executor_args = row.get("actual_test_executor_args")
    response_files = row.get("actual_executor_classpath_response_files")
    if not isinstance(executor_args, list) or not all(isinstance(arg, str) for arg in executor_args):
        raise AnalysisError("actual Test-worker JVM argv is missing")
    if not isinstance(response_files, list) or len(response_files) != 1:
        raise AnalysisError("actual Test-worker classpath response-file provenance is missing")
    response_path = str(response_files[0].get("path", ""))
    response_sha = response_files[0].get("sha256")
    result = []
    for argument in executor_args:
        if argument.startswith("-javaagent:"):
            continue
        if argument.startswith("-Dorg.gradle.internal.worker.tmpdir="):
            result.append("-Dorg.gradle.internal.worker.tmpdir=<per-run>")
        elif argument == f"@{response_path}":
            result.append(f"@response-file-sha256:{response_sha}")
        else:
            result.append(argument)
    expected_agents = 1 if row.get("agent") == "on" else 0
    actual_agents = sum(argument.startswith("-javaagent:") for argument in executor_args)
    if actual_agents != expected_agents:
        raise AnalysisError("actual Test-worker JVM argv contains an unexpected JaCoCo agent count")
    return tuple(result)


def _validate_revalidation(root: Path, current_rows: list[dict[str, Any]]) -> dict[str, Any]:
    sidecar_path = root / "revalidation.json"
    snapshot_path = root / "results-before-revalidation.json"
    sidecar = _json(sidecar_path)
    snapshot_sha = _sha256(snapshot_path)
    if (sidecar.get("immutable_original_results_snapshot") != str(snapshot_path)
            or sidecar.get("original_results_sha256") != snapshot_sha
            or sidecar.get("immutable_snapshot_sha256") != snapshot_sha
            or sidecar.get("revalidated_prefix_length") != 3):
        raise AnalysisError("pre-revalidation snapshot hash/length does not match its sidecar")
    snapshot = _json(snapshot_path)
    original_rows = snapshot.get("results")
    sidecar_rows = sidecar.get("rows")
    if not isinstance(original_rows, list) or len(original_rows) != 3 or not isinstance(sidecar_rows, list) or len(sidecar_rows) != 3:
        raise AnalysisError("expected the immutable snapshot and sidecar to preserve the three-row prefix")
    for index, (original, proof) in enumerate(zip(original_rows, sidecar_rows)):
        current = current_rows[index]
        if (proof.get("ordinal") != current.get("ordinal")
                or proof.get("stage") != current.get("stage")
                or proof.get("agent") != current.get("agent")
                or proof.get("original_guard_valid") is not original.get("valid")
                or proof.get("original_worker_args_stable_except_agent") is not original.get("worker_args_stable_except_agent")
                or proof.get("revalidated_valid") is not current.get("valid")
                or proof.get("revalidated_worker_args_stable_except_agent")
                is not current.get("worker_args_stable_except_agent")
                or proof.get("revalidated_worker_args_stable_except_agent") is not True):
            raise AnalysisError(f"revalidation sidecar row {index + 1} does not reconcile to original/current results")
        if proof.get("normalization") != (
            "replace Gradle @response-file path with SHA-256 of response-file contents; "
            "remove only the per-run JaCoCo agent token"
        ):
            raise AnalysisError(f"revalidation sidecar row {index + 1} has an unexpected normalization")
        classpath_files = proof.get("executor_classpath_response_files")
        if not isinstance(classpath_files, list) or len(classpath_files) != 1:
            raise AnalysisError(f"revalidation sidecar row {index + 1} lacks one classpath response file")
        response = classpath_files[0]
        response_path = Path(response.get("path", ""))
        if not response_path.is_file() or response.get("sha256") != _sha256(response_path):
            raise AnalysisError(f"revalidation sidecar row {index + 1} response-file content hash mismatch")
        current_response = current.get("actual_executor_classpath_response_files")
        if (not isinstance(current_response, list) or len(current_response) != 1
                or current_response[0].get("sha256") != response["sha256"]):
            raise AnalysisError(f"revalidation sidecar row {index + 1} disagrees with current result response-file hash")
        if original.get("valid") is False:
            # Preserve the original guard failure as evidence; this check does not
            # replace the immutable row with its revalidated status.
            if proof.get("original_guard_valid") is not False:
                raise AnalysisError("the original invalid flag was not retained for the failed prefix row")
    # The known original failure is exactly the third prefix row; it remains
    # represented in the snapshot/sidecar while the current row is revalidated.
    if not (
        original_rows[2].get("valid") is False
        and original_rows[2].get("worker_args_stable_except_agent") is False
        and current_rows[2].get("valid") is True
        and sidecar_rows[2].get("original_guard_valid") is False
        and sidecar_rows[2].get("revalidated_valid") is True
    ):
        raise AnalysisError("the original invalid third-row guard result was not preserved/revalidated")
    return {
        "prefix_rows": 3,
        "immutable_snapshot_sha256": snapshot_sha,
        "original_valid_flags": [row.get("valid") for row in original_rows],
        "current_valid_flags": [row.get("valid") for row in current_rows[:3]],
        "original_worker_args_stable_flags": [
            row.get("worker_args_stable_except_agent") for row in original_rows
        ],
        "revalidated_worker_args_stable_flags": [
            row.get("worker_args_stable_except_agent") for row in current_rows[:3]
        ],
        "revalidated_classpath_responsefile_hashes": [
            row["executor_classpath_response_files"][0]["sha256"] for row in sidecar_rows
        ],
        "protobuf_off_on_responsefile_hash_matches": (
            sidecar_rows[1]["executor_classpath_response_files"][0]["sha256"]
            == sidecar_rows[2]["executor_classpath_response_files"][0]["sha256"]
        ),
    }


def _analyze_flat_control(root: Path) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    root = Path(root)
    plan = _json(root / "plan.json")
    result_file = _json(root / "results.json")
    rows = result_file.get("results")
    if not isinstance(rows, list) or len(rows) != 8:
        raise AnalysisError("Flat/eager agent-off control must contain exactly eight measurements")
    if plan.get("expected_java") != {
        "total_tests": 116, "passed_tests": 116,
        "failed_tests": 0, "skipped_tests": 0,
    } or plan.get("reference_test_ids_sha256") != EXPECTED_TEST_ID:
        raise AnalysisError("Flat/eager plan does not match the 116-test Java suite")
    primary_path = Path(plan.get("primary_matrix", ""))
    if not primary_path.is_file() or plan.get("primary_matrix_sha256") != _sha256(primary_path):
        raise AnalysisError("Flat/eager control primary-matrix provenance hash does not match")
    stages = plan.get("stages")
    if not isinstance(stages, dict):
        raise AnalysisError("Flat/eager plan is missing stage provenance")
    expected_stage_provenance = {
        "ccache-text": ("text", "6fcf38494f87afd0de729a97f9a0cff9584dda8452580c08bc84eb97ad64fa50"),
        "flatbuffers": ("flat-eager", "1e8b3e7efc6c0392241ef73da4b66d73928a42541a71b8c47cb184cfec3a3b16"),
    }
    for stage, (wire, native_sha) in expected_stage_provenance.items():
        detail = stages.get(stage)
        metadata = detail.get("source_metadata") if isinstance(detail, dict) else None
        expected_cache_enabled = True
        if (not isinstance(metadata, dict) or metadata.get("wire") != wire
                or metadata.get("cache") is not expected_cache_enabled
                or detail.get("native_binary_sha256") != native_sha):
            raise AnalysisError(f"Flat/eager plan has unexpected {stage} source/native provenance")

    cells: dict[tuple[int, str], dict[str, Any]] = {}
    response_hashes: dict[str, set[str]] = {"ccache-text": set(), "flatbuffers": set()}
    flat_plan_native_by_stage = {
        stage: stages[stage]["native_binary_sha256"] for stage in expected_stage_provenance
    }
    for row in rows:
        if not isinstance(row, dict):
            raise AnalysisError("Flat/eager results contain a non-object row")
        round_number = row.get("round")
        stage = row.get("stage")
        if round_number not in ROUNDS or stage not in expected_stage_provenance or row.get("agent") != "off":
            raise AnalysisError("Flat/eager result has an unexpected round/stage/agent cell")
        key = (round_number, stage)
        if key in cells:
            raise AnalysisError(f"duplicate Flat/eager control cell {key}")
        run_dir = Path(row.get("run_dir", ""))
        try:
            run_dir.resolve().relative_to(root.resolve())
        except (OSError, ValueError) as error:
            raise AnalysisError("Flat/eager run directory is not contained by its result root") from error
        summary_path = run_dir / "summary.json"
        command_path = run_dir / "command.json"
        raw = _json(summary_path)
        command = _json(command_path)
        if row != raw:
            raise AnalysisError(f"Flat/eager result row differs from {summary_path.name}")
        if (command.get("stage") != stage or command.get("agent") != "off"
                or row.get("return_code") != 0 or row.get("exit_status") != 0
                or row.get("valid") is not True or row.get("test_task_executed") is not True
                or row.get("actual_native_tool_sha256") != flat_plan_native_by_stage[stage]):
            raise AnalysisError(f"{summary_path.name}: run validity/command attribution failed")
        if (row.get("total_tests") != 116 or row.get("passed_tests") != 116
                or row.get("failed_tests") != 0 or row.get("skipped_tests") != 0
                or row.get("test_identity_sha256") != EXPECTED_TEST_ID
                or row.get("test_identity_match") is not True):
            raise AnalysisError(f"{summary_path.name}: Java test identity/result differs from 116 expected tests")
        if (row.get("test_worker_xmx_512m") is not True
                or row.get("only_expected_jacoco_agent") is not True
                or row.get("worker_args_stable_within_stage") is not True):
            raise AnalysisError(f"{summary_path.name}: Test-worker heap/agent/stability proof failed")
        worker = row.get("worker_configuration")
        executor_args = row.get("actual_test_executor_args")
        if (not isinstance(worker, dict) or worker.get("requested_agent") != "off"
                or worker.get("jacoco_enabled") is not False or worker.get("javaagent_args") != []
                or not isinstance(executor_args, list)
                or any(not isinstance(arg, str) or arg.startswith("-javaagent:") for arg in executor_args)
                or [arg for arg in executor_args if arg.startswith("-Xmx")] != ["-Xmx512m"]):
            raise AnalysisError(f"{summary_path.name}: actual Test-worker argv is not agent-off at -Xmx512m")
        if (row.get("report_tasks_skipped") != {
                "jacocoTestReport": True, "jacocoTestCoverageVerification": True,
            } or row.get("compile_tasks_not_up_to_date") != []):
            raise AnalysisError(f"{summary_path.name}: reports/compilation boundary does not match the control")
        if (row.get("cache_validation_passed") is not True or row.get("cache_delta") != EXPECTED_CACHE):
            raise AnalysisError(f"{summary_path.name}: expected warm ccache counters are not proven")
        if (not isinstance(row.get("elapsed_s"), (int, float))
                or not math.isfinite(row["elapsed_s"]) or row["elapsed_s"] <= 0):
            raise AnalysisError(f"{summary_path.name}: elapsed_s is invalid")
        response_files = row.get("actual_executor_classpath_response_files")
        if not isinstance(response_files, list) or len(response_files) != 1:
            raise AnalysisError(f"{summary_path.name}: classpath response-file proof is missing")
        response = response_files[0]
        response_path = Path(response.get("path", ""))
        if not response_path.is_file() or response.get("sha256") != _sha256(response_path):
            raise AnalysisError(f"{summary_path.name}: classpath response-file content hash mismatch")
        response_hashes[stage].add(response["sha256"])
        cells[key] = row

    expected_cells = {(round_number, stage) for round_number in ROUNDS
                      for stage in expected_stage_provenance}
    if set(cells) != expected_cells:
        raise AnalysisError("Flat/eager control does not cover all eight round/stage cells")
    if any(len(response_hashes[stage]) != 1 for stage in expected_stage_provenance):
        raise AnalysisError("Flat/eager worker classpath response-file hash is not stable within stage")
    for stage in expected_stage_provenance:
        baseline = _normalized_worker_args(cells[(1, stage)])
        for round_number in ROUNDS[1:]:
            if _normalized_worker_args(cells[(round_number, stage)]) != baseline:
                raise AnalysisError(f"Flat/eager {stage} worker args differ within the four rounds")

    paired = []
    for round_number in ROUNDS:
        text_s = float(cells[(round_number, "ccache-text")]["elapsed_s"])
        flat_s = float(cells[(round_number, "flatbuffers")]["elapsed_s"])
        paired.append({
            "round": round_number,
            "text_s": text_s,
            "flat_eager_s": flat_s,
            "flat_minus_text_s": flat_s - text_s,
            "flat_minus_text_pct": 100.0 * (flat_s / text_s - 1.0),
        })
    deltas = [row["flat_minus_text_pct"] for row in paired]
    detail = {
        "cells": len(cells),
        "rounds": len(ROUNDS),
        "tests_per_cell": 116,
        "same_test_identity_sha256": EXPECTED_TEST_ID,
        "agent_off_in_both_stages": True,
        "same_actual_worker_heap": "-Xmx512m",
        "same_warm_ccache_proof": EXPECTED_CACHE,
        "primary_matrix_sha256": plan["primary_matrix_sha256"],
        "all_compile_tasks_unchanged": True,
        "report_tasks_skipped_both_stages": True,
        "responsefile_sha256_by_stage": {
            stage: next(iter(response_hashes[stage])) for stage in expected_stage_provenance
        },
        "flat_stage_is_separate_from_jacoco_control_run": True,
        "flat_faster_in_all_four_pairs": all(row["flat_minus_text_s"] < 0 for row in paired),
        "flat_minus_text_median_pct": statistics.median(deltas),
        "flat_minus_text_range_pct": [min(deltas), max(deltas)],
    }
    return paired, detail


def analyze(root: Path, flat_control_root: Path | None = None) -> dict[str, Any]:
    root = Path(root)
    plan = _json(root / "plan.json")
    result_file = _json(root / "results.json")
    rows = result_file.get("results")
    if not isinstance(rows, list) or len(rows) != 16:
        raise AnalysisError("results.json must contain exactly 16 measurements")
    if (plan.get("rounds") != 4 or plan.get("max_cells") != 16
            or plan.get("expected_tests") != {
                "total_tests": 116, "passed_tests": 116,
                "failed_tests": 0, "skipped_tests": 0,
            }
            or plan.get("control") != "JaCoCo Test-worker javaagent enabled vs disabled; reporting tasks disabled in both arms"):
        raise AnalysisError("plan does not describe the four-round 116-test agent control")
    primary_sha = plan.get("primary_matrix_sha256")
    primary_path = Path(plan.get("primary_matrix", ""))
    if not primary_path.is_file() or primary_sha != _sha256(primary_path):
        raise AnalysisError("primary matrix provenance hash does not match")

    expected_cache_by_stage = {
        "ccache-text": EXPECTED_CACHE,
        "protobuf": EXPECTED_CACHE,
    }
    if plan.get("expected_per_command_cache_delta") != expected_cache_by_stage:
        raise AnalysisError("plan cache expectations do not match 208 calls / 207 hits / 1 miss")

    cell_index: dict[tuple[int, str, str], dict[str, Any]] = {}
    response_hashes: dict[str, set[str]] = {"text": set(), "protobuf": set()}
    test_id_hashes = set()
    jacoco_agent_configurations: set[tuple[str, ...]] = set()
    all_on_off_same_cells = True
    for row in rows:
        if not isinstance(row, dict):
            raise AnalysisError("results.json contains a non-object row")
        round_number = row.get("round")
        stage = row.get("stage")
        agent = row.get("agent")
        protocol = "text" if stage == "ccache-text" else "protobuf" if stage == "protobuf" else None
        if round_number not in ROUNDS or protocol is None or agent not in AGENTS:
            raise AnalysisError("results.json has an unexpected round/protocol/agent cell")
        key = (round_number, protocol, agent)
        if key in cell_index:
            raise AnalysisError(f"duplicate cell {key}")
        run_dir = _run_directory(root, row)
        summary_path = run_dir / "summary.json"
        command_path = run_dir / "command.json"
        raw = _json(summary_path)
        command = _json(command_path)
        expected_cache = _expected_cache(row, plan)
        revalidation_fields = {
            "original_guard_valid", "original_worker_args_stable_except_agent",
            "actual_executor_classpath_response_files",
        }
        comparable_row = {key: value for key, value in row.items() if key not in revalidation_fields}
        comparable_raw = {key: value for key, value in raw.items() if key not in revalidation_fields}
        differences = {
            key for key in set(comparable_row) | set(comparable_raw)
            if comparable_row.get(key) != comparable_raw.get(key)
        }
        # The aggregate result was enriched with measured classpath-response-file
        # hashes after the per-run summaries had already been captured. Compare
        # the hashes where both records contain them; otherwise take the enriched
        # root record as the provenance source and verify its bytes below.
        if "actual_executor_classpath_response_files" in raw:
            if row.get("actual_executor_classpath_response_files") != raw.get("actual_executor_classpath_response_files"):
                differences.add("actual_executor_classpath_response_files")
        # Row 3 was explicitly revalidated after its immutable snapshot recorded
        # the original guard failure. That exception is checked against the
        # snapshot and sidecar below; it must not erase the original invalid flag.
        if row.get("ordinal") == 3:
            differences -= {"valid", "worker_args_stable_except_agent"}
        if differences:
            raise AnalysisError(f"result row differs from its per-run summary: {summary_path.name}")
        if (command.get("stage") != stage or command.get("agent") != agent
                or row.get("run_dir") != str(run_dir)):
            raise AnalysisError(f"{summary_path.name}: command attribution or run directory mismatch")
        required_equal = {
            "ordinal": row.get("ordinal"),
            "round": round_number,
            "stage": stage,
            "agent": agent,
            "return_code": 0,
            "valid": True,
            "exit_status": 0,
            "total_tests": 116,
            "passed_tests": 116,
            "failed_tests": 0,
            "skipped_tests": 0,
            "test_identity_sha256": EXPECTED_TEST_ID,
            "test_identity_match": True,
            "test_task_executed": True,
            "test_worker_xmx_512m": True,
            "only_expected_jacoco_agent": True,
            "actual_test_executor_only_jacoco_agent": True,
            "worker_args_stable_except_agent": True,
            "cache_validation_passed": True,
            "expected_cache_delta": expected_cache,
            "observed_cache_delta": expected_cache,
            "report_tasks_skipped": {
                "jacocoTestReport": True,
                "jacocoTestCoverageVerification": True,
            },
        }
        for field, expected in required_equal.items():
            if row.get(field) != expected:
                raise AnalysisError(f"{summary_path.name}: {field} does not equal {expected!r}")
        expected_cache_row = {
            "cacheable_calls_delta": expected_cache["cacheable_calls"],
            "cache_hits_delta": expected_cache["hits"],
            "cache_misses_delta": expected_cache["misses"],
            "uncacheable_calls_delta": expected_cache["uncacheable_calls"],
        }
        for field, expected in expected_cache_row.items():
            if row.get(field) != expected:
                raise AnalysisError(f"{summary_path.name}: {field} differs from the proven cache expectation")
        worker = row.get("worker_configuration")
        if (not isinstance(worker, dict) or worker.get("requested_agent") != agent
                or worker.get("jacoco_enabled") is not (agent == "on")
                or worker.get("max_heap_size") is not None):
            raise AnalysisError(f"{summary_path.name}: worker agent/heap settings do not match the cell")
        executor_args = row.get("actual_test_executor_args")
        agent_args = worker.get("javaagent_args")
        if not isinstance(executor_args, list) or not isinstance(agent_args, list):
            raise AnalysisError(f"{summary_path.name}: actual Test-worker argv proof is missing")
        heap_flags = [arg for arg in executor_args if isinstance(arg, str) and arg.startswith("-Xmx")]
        actual_agents = [arg for arg in executor_args if isinstance(arg, str) and arg.startswith("-javaagent:")]
        expected_agent_count = 1 if agent == "on" else 0
        if (heap_flags != ["-Xmx512m"] or len(actual_agents) != expected_agent_count
                or len(agent_args) != expected_agent_count):
            raise AnalysisError(f"{summary_path.name}: actual worker argv does not isolate the JaCoCo toggle at -Xmx512m")
        if agent == "off" and actual_agents:
            raise AnalysisError(f"{summary_path.name}: JaCoCo-off worker includes a javaagent")
        if agent == "on" and (len(actual_agents) != 1 or "jacocoagent.jar=" not in actual_agents[0]):
            raise AnalysisError(f"{summary_path.name}: JaCoCo-on worker lacks the expected agent")
        if agent == "on":
            agent_options_text = actual_agents[0].split("jacocoagent.jar=", 1)[1]
            options = tuple(agent_options_text.split(","))
            option_keys = tuple(sorted(item.split("=", 1)[0] for item in options))
            expected_option_keys = (
                "append", "destfile", "dumponexit", "inclnolocationclasses", "jmx", "output",
            )
            if option_keys != expected_option_keys or any(
                    "include" in item.split("=", 1)[0].lower() or "exclude" in item.split("=", 1)[0].lower()
                    for item in options):
                raise AnalysisError(f"{summary_path.name}: JaCoCo selection filters/options differ from the observed default-filter control")
            jacoco_agent_configurations.add(tuple(sorted(
                item for item in options if not item.startswith("destfile=")
            )))
        if row.get("compile_tasks_not_up_to_date") != []:
            raise AnalysisError(f"{summary_path.name}: compilation work was not unchanged")
        stage_plan = plan.get("stages", {}).get(stage, {})
        planned_native_sha = stage_plan.get("native_binary_sha256") or stage_plan.get("identity", {}).get("native_tool_sha256")
        if row.get("actual_native_tool_sha256") != planned_native_sha:
            raise AnalysisError(f"{summary_path.name}: observed native tool hash differs from the plan")
        if not isinstance(row.get("elapsed_s"), (int, float)) or not math.isfinite(row["elapsed_s"]) or row["elapsed_s"] <= 0:
            raise AnalysisError(f"{summary_path.name}: elapsed_s is invalid")
        response_files = row.get("actual_executor_classpath_response_files") or raw.get("actual_executor_classpath_response_files")
        if not isinstance(response_files, list) or len(response_files) != 1:
            raise AnalysisError(f"{summary_path.name}: expected one Gradle worker classpath response-file hash")
        response = response_files[0]
        response_path = Path(response.get("path", ""))
        if not response_path.is_file() or response.get("sha256") != _sha256(response_path):
            raise AnalysisError(f"{summary_path.name}: Gradle worker classpath response-file hash mismatch")
        jacoco_exec = run_dir / "gradle-output" / "jacoco.exec"
        if (agent == "on") != jacoco_exec.is_file():
            raise AnalysisError(f"{summary_path.name}: JaCoCo execution artifact does not match the agent state")
        response_hashes[protocol].add(response["sha256"])
        test_id_hashes.add(row["test_identity_sha256"])
        # Preserve root-level revalidation and provenance enrichment while
        # filling fields that appear only in the per-run summary.
        cell_index[key] = {**raw, **row}

    expected_cells = {
        (round_number, protocol, agent)
        for round_number in ROUNDS for protocol in FORMATS for agent in AGENTS
    }
    if set(cell_index) != expected_cells:
        raise AnalysisError("results.json does not cover all 16 paired round/protocol/agent cells")
    if test_id_hashes != {EXPECTED_TEST_ID}:
        raise AnalysisError("test identity differs across Java format/agent cells")
    if len(response_hashes["text"]) != 1 or len(response_hashes["protobuf"]) != 1:
        raise AnalysisError("worker response-file content hash is not stable by protocol")
    if len(jacoco_agent_configurations) != 1:
        raise AnalysisError("JaCoCo agent options differ across ON cells beyond destination file")
    for round_number in ROUNDS:
        for protocol in FORMATS:
            off = cell_index[(round_number, protocol, "off")]
            on = cell_index[(round_number, protocol, "on")]
            if _normalized_worker_args(off) != _normalized_worker_args(on):
                raise AnalysisError(f"r{round_number}/{protocol}: worker argv differs beyond the JaCoCo agent token")

    revalidation = _validate_revalidation(root, rows)
    rounds = []
    for round_number in ROUNDS:
        off_text = float(cell_index[(round_number, "text", "off")]["elapsed_s"])
        off_proto = float(cell_index[(round_number, "protobuf", "off")]["elapsed_s"])
        on_text = float(cell_index[(round_number, "text", "on")]["elapsed_s"])
        on_proto = float(cell_index[(round_number, "protobuf", "on")]["elapsed_s"])
        off_gap_s = off_proto - off_text
        on_gap_s = on_proto - on_text
        off_gap_pct = 100.0 * (off_proto / off_text - 1.0)
        on_gap_pct = 100.0 * (on_proto / on_text - 1.0)
        rounds.append({
            "round": round_number,
            "agent_off_text_s": off_text,
            "agent_off_protobuf_s": off_proto,
            "agent_on_text_s": on_text,
            "agent_on_protobuf_s": on_proto,
            "agent_off_format_gap_proto_minus_text_s": off_gap_s,
            "agent_on_format_gap_proto_minus_text_s": on_gap_s,
            "agent_off_format_gap_pct": off_gap_pct,
            "agent_on_format_gap_pct": on_gap_pct,
            "agent_difference_in_differences_s": on_gap_s - off_gap_s,
            "agent_difference_in_differences_percentage_points": on_gap_pct - off_gap_pct,
        })
    gap_off = [row["agent_off_format_gap_pct"] for row in rounds]
    gap_on = [row["agent_on_format_gap_pct"] for row in rounds]
    did_s = [row["agent_difference_in_differences_s"] for row in rounds]
    did_pct = [row["agent_difference_in_differences_percentage_points"] for row in rounds]
    flat_rounds: list[dict[str, Any]] = []
    flat_validation: dict[str, Any] | None = None
    if flat_control_root is not None:
        flat_rounds, flat_validation = _analyze_flat_control(flat_control_root)
        if flat_validation["primary_matrix_sha256"] != primary_sha:
            raise AnalysisError("Flat/eager and JaCoCo arms do not share the same primary-matrix identity")
    return {
        "schema_version": 1,
        "evidence_label": "validated four-round Java full-suite JaCoCo agent control",
        "timing_boundary": "per-run external wall time for the full Gradle Java test task",
        "format_gap_definition": "100 * (Protobuf elapsed_s / Text elapsed_s - 1); negative means Protobuf faster",
        "control": "JaCoCo Test-worker agent on versus off, with reporting tasks disabled in both arms",
        "caption": (
            "Both arms use the same 116-test Java suite and test identity, actual Test-worker -Xmx512m, "
            "and warm ccache (208 cacheable calls: 207 hits, 1 miss). The JaCoCo Test-worker agent is "
            "the toggled variable; JaCoCo report tasks are skipped in both arms."
        ),
        "validation": {
            "cells": len(cell_index),
            "rounds": len(ROUNDS),
            "tests_per_cell": 116,
            "passed_tests_per_cell": 116,
            "same_test_identity_sha256": EXPECTED_TEST_ID,
            "same_actual_worker_heap": "-Xmx512m",
            "same_warm_ccache_proof": EXPECTED_CACHE,
            "all_cells_valid": True,
            "all_worker_args_stable_except_agent": True,
            "all_test_classpaths_same_per_protocol": True,
            "test_worker_responsefile_sha256_by_protocol": {
                protocol: next(iter(response_hashes[protocol])) for protocol in FORMATS
            },
            "all_report_tasks_skipped_both_arms": True,
            "all_compile_tasks_unchanged": True,
            "jacoco_agent_options": [
                "append", "destfile", "dumponexit", "inclnolocationclasses", "jmx", "output",
            ],
            "jacoco_agent_options_common_except_destfile": list(next(iter(jacoco_agent_configurations))),
            "jacoco_agent_include_exclude_filters": "none explicitly specified",
            "revalidation": revalidation,
        },
        "rounds": rounds,
        "flat_control_rounds": flat_rounds,
        "paired_medians": {
            "agent_off_format_gap_pct": statistics.median(gap_off),
            "agent_off_format_gap_range_pct": [min(gap_off), max(gap_off)],
            "agent_on_format_gap_pct": statistics.median(gap_on),
            "agent_on_format_gap_range_pct": [min(gap_on), max(gap_on)],
            "agent_difference_in_differences_s": statistics.median(did_s),
            "agent_difference_in_differences_range_s": [min(did_s), max(did_s)],
            "agent_difference_in_differences_percentage_points": statistics.median(did_pct),
            "agent_difference_in_differences_range_percentage_points": [min(did_pct), max(did_pct)],
            **({
                "flat_agent_off_vs_text_gap_pct": flat_validation["flat_minus_text_median_pct"],
                "flat_agent_off_vs_text_gap_range_pct": flat_validation["flat_minus_text_range_pct"],
            } if flat_validation is not None else {}),
        },
        "flat_control_validation": flat_validation,
    }


def _chart(summary: dict[str, Any]) -> str:
    rows = summary["rounds"]
    values = {
        "Proto · JaCoCo OFF": [row["agent_off_format_gap_pct"] for row in rows],
        "Proto · JaCoCo ON": [row["agent_on_format_gap_pct"] for row in rows],
    }
    flat_rows = summary.get("flat_control_rounds", [])
    if flat_rows:
        values["Flat/eager · OFF"] = [row["flat_minus_text_pct"] for row in flat_rows]
    min_value = min(min(row) for row in values.values())
    max_value = max(max(row) for row in values.values())
    span = max_value - min_value
    padding = max(0.8, span * .10)
    extent = math.ceil(max(abs(min_value), abs(max_value)) + padding)
    low, high = -float(extent), float(extent)
    ticks = (low, low / 2, 0.0, high / 2, high)
    x0, x1 = 142.0, 326.0
    x = lambda value: x0 + (value - low) * (x1 - x0) / (high - low)
    row_y = {label: 66.0 + index * 64.0 for index, label in enumerate(values)}
    colors = ("#2369a1", "#d07816", "#198264", "#8656a1")
    output = [
        '<svg xmlns="http://www.w3.org/2000/svg" role="img" viewBox="0 0 360 334" '
        'aria-labelledby="jacoco-gap-title jacoco-gap-desc" style="display:block;width:100%;height:auto;max-width:360px">',
        '<title id="jacoco-gap-title">Java paired format/control gaps</title>',
        '<desc id="jacoco-gap-desc">Three four-round paired comparisons: Protobuf versus Text with JaCoCo off and on, and Flat eager versus Text with JaCoCo off in a separate control run. Negative means the candidate is faster. Whiskers show the round range and diamonds the median.</desc>',
        '<style>text{font:15px system-ui,sans-serif;fill:#192535}.axis{stroke:#8391a0}.grid{stroke:#d9e0e7;stroke-dasharray:3 4}.zero{stroke:#586575;stroke-width:1.5}.whisker{stroke:#374151;stroke-width:2}.median{fill:#111827;stroke:white;stroke-width:1.2}</style>',
        '<text x="4" y="20">Java full-suite paired format/control gaps</text>',
    ]
    for tick in ticks:
        xpos = x(tick)
        grid_class = "zero" if tick == 0 else "grid"
        output.append(f'<line class="{grid_class}" x1="{xpos:.2f}" x2="{xpos:.2f}" y1="34" y2="224"/>')
        output.append(f'<text x="{xpos:.2f}" y="243" text-anchor="middle">{tick:+.1f}%</text>')
    for label, values_here in values.items():
        y = row_y[label]
        median = statistics.median(values_here)
        low_x, high_x, median_x = x(min(values_here)), x(max(values_here)), x(median)
        output.extend([
            f'<text x="4" y="{y - 5:.0f}">{html.escape(label)}</text>',
            f'<text x="4" y="{y + 17:.0f}">median {median:+.2f}%</text>',
            f'<line class="whisker" x1="{low_x:.2f}" x2="{high_x:.2f}" y1="{y:.0f}" y2="{y:.0f}"/>',
            f'<line class="whisker" x1="{low_x:.2f}" x2="{low_x:.2f}" y1="{y - 6:.0f}" y2="{y + 6:.0f}"/>',
            f'<line class="whisker" x1="{high_x:.2f}" x2="{high_x:.2f}" y1="{y - 6:.0f}" y2="{y + 6:.0f}"/>',
        ])
        for round_index, value in enumerate(values_here):
            jitter = (-9, -3, 3, 9)[round_index]
            output.append(
                f'<circle cx="{x(value):.2f}" cy="{y + jitter:.0f}" r="5" fill="{colors[round_index]}">'
                f'<title>Round {round_index + 1}: {value:+.3f}%</title></circle>'
            )
        output.append(
            f'<path class="median" d="M {median_x:.2f} {y - 7:.0f} l 7 7 l -7 7 l -7 -7 Z">'
            f'<title>Median format gap: {median:+.3f}%</title></path>'
        )
    output.extend([
        f'<line class="axis" x1="{x0:.2f}" x2="{x1:.2f}" y1="224" y2="224"/>',
        '<text x="234" y="269" text-anchor="middle">Candidate − Text paired gap</text>',
    ])
    for round_index, color in enumerate(colors, 1):
        xpos = 47 + (round_index - 1) * 72
        output.extend([
            f'<circle cx="{xpos}" cy="297" r="5" fill="{color}"/>',
            f'<text x="{xpos + 9}" y="302">R{round_index}</text>',
        ])
    output.append('<text x="180" y="329" text-anchor="middle">Negative = candidate faster</text>')
    output.append('</svg>')
    return "".join(output)


def write_outputs(summary: dict[str, Any], output_dir: Path) -> dict[str, Path]:
    if (summary.get("evidence_label") != "validated four-round Java full-suite JaCoCo agent control"
            or len(summary.get("flat_control_rounds", [])) != 4
            or not isinstance(summary.get("flat_control_validation"), dict)):
        raise AnalysisError("refusing to export unvalidated timings")
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / "java-jacoco-agent-control-analysis.json"
    csv_path = output_dir / "java-format-control-paired-gaps.csv"
    svg_path = output_dir / "java-format-control-paired-gaps.svg"
    html_path = output_dir / "java-format-control-reviewed-fragment.html"
    json_text = json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if any(marker in json_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("summary contains a local path")
    json_path.write_text(json_text, encoding="utf-8")
    fields = (
        "round",
        "proto_jacoco_off_text_s", "proto_jacoco_off_s",
        "proto_jacoco_off_minus_text_s", "proto_jacoco_off_minus_text_pct",
        "proto_jacoco_on_text_s", "proto_jacoco_on_s",
        "proto_jacoco_on_minus_text_s", "proto_jacoco_on_minus_text_pct",
        "flat_eager_agent_off_text_s", "flat_eager_agent_off_s",
        "flat_eager_agent_off_minus_text_s", "flat_eager_agent_off_minus_text_pct",
    )
    with csv_path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        flat_by_round = {row["round"]: row for row in summary["flat_control_rounds"]}
        for row in summary["rounds"]:
            flat = flat_by_round[row["round"]]
            values = {
                "round": row["round"],
                "proto_jacoco_off_text_s": row["agent_off_text_s"],
                "proto_jacoco_off_s": row["agent_off_protobuf_s"],
                "proto_jacoco_off_minus_text_s": row["agent_off_format_gap_proto_minus_text_s"],
                "proto_jacoco_off_minus_text_pct": row["agent_off_format_gap_pct"],
                "proto_jacoco_on_text_s": row["agent_on_text_s"],
                "proto_jacoco_on_s": row["agent_on_protobuf_s"],
                "proto_jacoco_on_minus_text_s": row["agent_on_format_gap_proto_minus_text_s"],
                "proto_jacoco_on_minus_text_pct": row["agent_on_format_gap_pct"],
                "flat_eager_agent_off_text_s": flat["text_s"],
                "flat_eager_agent_off_s": flat["flat_eager_s"],
                "flat_eager_agent_off_minus_text_s": flat["flat_minus_text_s"],
                "flat_eager_agent_off_minus_text_pct": flat["flat_minus_text_pct"],
            }
            writer.writerow({key: value if key == "round" else f"{value:.9f}" for key, value in values.items()})
    svg_text = _chart(summary)
    if any(marker in svg_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("SVG contains a local path")
    svg_path.write_text(svg_text, encoding="utf-8")
    csv_b64 = base64.b64encode(csv_path.read_bytes()).decode("ascii")
    medians = summary["paired_medians"]
    flat_median = medians["flat_agent_off_vs_text_gap_pct"]
    flat_range = medians["flat_agent_off_vs_text_gap_range_pct"]
    flat_note = (
        f"Flat/eager, agent off: median {flat_median:+.2f}% "
        f"(rounds {flat_range[0]:+.2f}% to {flat_range[1]:+.2f}%)."
    )
    proto_off_range = medians["agent_off_format_gap_range_pct"]
    proto_on_range = medians["agent_on_format_gap_range_pct"]
    fragment = "".join([
        '<section class="java-format-control" aria-labelledby="java-format-control-title">',
        '<style>.java-format-control{max-width:48rem;margin:1.25rem auto;font:16px/1.45 system-ui,sans-serif;color:#192535}',
        '.java-format-control figure{max-width:360px;margin:1rem auto}',
        '.java-format-control .control-summary p{margin:.35rem 0}',
        '.java-format-control a{overflow-wrap:anywhere}',
        '@media(max-width:420px){.java-format-control{font-size:15px;padding:0 .25rem}}</style>',
        '<h3 id="java-format-control-title">Java full-suite format controls</h3>',
        '<p>Four within-round pairs per row on a shared percentage axis. JaCoCo rows compare Protobuf with Text, agent off/on; the separate Flat/eager row compares Flat with Text, agent off. All arms passed the same 116-test identity at worker -Xmx512m with warm ccache (208 calls, 207 hits, 1 miss); reports were skipped. These are distinct controls. Negative means the candidate took less wall time than Text.</p>',
        '<figure>', svg_text,
        '<figcaption>Candidate minus Text; whiskers show the four-round range and diamonds the median.</figcaption>',
        '</figure>',
        '<div class="control-summary">',
        f'<p><strong>Protobuf, JaCoCo off:</strong> median {medians["agent_off_format_gap_pct"]:+.2f}% '
        f'(rounds {proto_off_range[0]:+.2f}% to {proto_off_range[1]:+.2f}%).</p>',
        f'<p><strong>Protobuf, JaCoCo on:</strong> median {medians["agent_on_format_gap_pct"]:+.2f}% '
        f'(rounds {proto_on_range[0]:+.2f}% to {proto_on_range[1]:+.2f}%).</p>',
        f'<p><strong>{html.escape(flat_note)}</strong></p>',
        '</div>',
        f'<p><a download="java-format-control-paired-gaps.csv" href="data:text/csv;charset=utf-8;base64,{csv_b64}">Download the sanitized paired-round CSV</a></p>',
        '</section>',
    ])
    if any(marker in fragment for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("HTML fragment contains a local path")
    html_path.write_text(fragment, encoding="utf-8")
    return {"summary": json_path, "csv": csv_path, "svg": svg_path, "html": html_path}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-root", type=Path, required=True)
    parser.add_argument("--flat-control-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args(argv)
    outputs = write_outputs(analyze(args.run_root, args.flat_control_root), args.output_dir)
    print(json.dumps({name: str(path) for name, path in outputs.items()}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
