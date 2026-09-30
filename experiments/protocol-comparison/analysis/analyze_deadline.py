#!/usr/bin/env python3
"""Validate and summarize the fresh protocol/cache deadline matrix.

This tool intentionally computes distributions and paired differences only. It
does not rank implementations or make a technology recommendation.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import statistics
from typing import Any


SUITES = ("clava-js", "java")
MODES = ("direct", "cold", "warm")
STAGES = ("before-cache", "ccache-text", "protobuf", "flatbuffers")
STAGES_BY_MODE = {
    "direct": STAGES,
    "cold": ("ccache-text", "protobuf", "flatbuffers"),
    "warm": ("ccache-text", "protobuf", "flatbuffers"),
}
REPEATS = tuple(range(1, 7))
EXPECTED_TESTS = {
    "clava-js": {"total": 164, "passed": 158, "failed": 0, "skipped": 6},
    "java": {"total": 116, "passed": 116, "failed": 0, "skipped": 0},
}
EXPECTED_CACHE_STATES = {mode: list(stages) for mode, stages in STAGES_BY_MODE.items()}
BUILD_PROVENANCE_FIELDS = (
    "clava_revision",
    "clava_patch_sha256",
    "native_revision",
    "native_binary_sha256",
    "parser_jar_sha256",
    "runtime_manifest_sha256",
    "specsutils_revision",
    "specsutils_jar_sha256",
)


class AnalysisError(ValueError):
    """Input does not meet the complete, valid-matrix contract."""


def expected_cells() -> set[tuple[str, str, str]]:
    return {
        (suite, mode, stage)
        for suite in SUITES
        for mode, stages in STAGES_BY_MODE.items()
        for stage in stages
    }


def finite_number(value: Any, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise AnalysisError(f"{label} must be a number")
    number = float(value)
    if not math.isfinite(number) or number < 0:
        raise AnalysisError(f"{label} must be finite and non-negative")
    return number


def q(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lo, hi = math.floor(position), math.ceil(position)
    if lo == hi:
        return ordered[lo]
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (position - lo)


def stats(values: list[float]) -> dict[str, float]:
    q1, median, q3 = q(values, .25), statistics.median(values), q(values, .75)
    return {
        "median_s": median,
        "min_s": min(values),
        "max_s": max(values),
        "q1_s": q1,
        "q3_s": q3,
        "iqr_s": q3 - q1,
    }


def cell_id(suite: str, mode: str, stage: str, repeat: int | None) -> str:
    attempt = "warmup" if repeat is None else f"repeat-{repeat:02d}"
    return f"{suite}/{mode}/{stage}/{attempt}"


def validate_plan(plan: Any, source: Path) -> tuple[set[tuple[str, str, str]], dict[str, dict[str, Any]]]:
    if not isinstance(plan, dict):
        raise AnalysisError(f"{source}: plan must be an object")
    if (type(plan.get("repeat_count")) is not int or plan.get("repeat_count") != 6
            or type(plan.get("warmup_count")) is not int or plan.get("warmup_count") != 1):
        raise AnalysisError(f"{source}: expected one warm-up and six measured repeats")
    planned_suites = plan.get("suites")
    if (not isinstance(planned_suites, list) or not planned_suites
            or not all(isinstance(suite, str) for suite in planned_suites)
            or len(planned_suites) != len(set(planned_suites))
            or not set(planned_suites) <= set(SUITES)):
        raise AnalysisError(f"{source}: plan.suites must select one or both known suites")
    actual_tests = plan.get("expected_tests")
    tests_match = isinstance(actual_tests, dict) and set(actual_tests) == set(EXPECTED_TESTS)
    if tests_match:
        tests_match = all(
            isinstance(actual_tests[suite], dict)
            and set(actual_tests[suite]) == set(EXPECTED_TESTS[suite])
            and all(type(actual_tests[suite][key]) is int
                    and actual_tests[suite][key] == expected
                    for key, expected in EXPECTED_TESTS[suite].items())
            for suite in SUITES
        )
    if not tests_match:
        raise AnalysisError(f"{source}: suite test-count contract does not match the planned workload")
    cache_states = plan.get("cache_states")
    if not isinstance(cache_states, dict) or not cache_states:
        raise AnalysisError(f"{source}: plan.cache_states must describe selected cache modes")
    for mode, stages_for_mode in cache_states.items():
        if mode not in STAGES_BY_MODE or stages_for_mode != list(STAGES_BY_MODE[mode]):
            raise AnalysisError(f"{source}: unexpected cache-state/stage selection for {mode}")

    cells = plan.get("cells")
    if not isinstance(cells, list):
        raise AnalysisError(f"{source}: plan.cells must be an array")
    planned: list[tuple[str, str, str]] = []
    for item in cells:
        if not isinstance(item, dict):
            raise AnalysisError(f"{source}: every planned cell must be an object")
        key = (item.get("suite"), item.get("mode"), item.get("stage"))
        if (not all(isinstance(part, str) for part in key)
                or type(item.get("warmup_count")) is not int or item.get("warmup_count") != 1
                or type(item.get("repeat_count")) is not int or item.get("repeat_count") != 6):
            raise AnalysisError(f"{source}: {key} has the wrong warm-up/repeat count")
        planned.append(key)
    planned_set = set(planned)
    if len(planned) != len(planned_set) or not planned_set or not planned_set <= expected_cells():
        raise AnalysisError(f"{source}: plan.cells is duplicated or contains an unexpected suite/cell")
    if {suite for suite, _, _ in planned_set} != set(planned_suites):
        raise AnalysisError(f"{source}: plan.suites does not match plan.cells")
    if {mode for _, mode, _ in planned_set} != set(cache_states):
        raise AnalysisError(f"{source}: plan.cache_states does not match plan.cells")

    stages = plan.get("stages")
    used_stages = {stage for _, _, stage in planned_set}
    if not isinstance(stages, dict) or set(stages) != used_stages:
        raise AnalysisError(f"{source}: plan.stages must describe exactly the selected stages")
    for stage, metadata in stages.items():
        if not isinstance(metadata, dict):
            raise AnalysisError(f"{source}: plan stage {stage} provenance must be an object")
        missing = [field for field in BUILD_PROVENANCE_FIELDS if field not in metadata]
        if missing:
            raise AnalysisError(f"{source}: plan stage {stage} is missing provenance {missing}")
        for field in BUILD_PROVENANCE_FIELDS:
            value = metadata[field]
            if not isinstance(value, str) or not value:
                raise AnalysisError(f"{source}: plan stage {stage} has empty {field}")
            if field.endswith("_sha256") and not re.fullmatch(r"[0-9a-f]{64}", value):
                raise AnalysisError(f"{source}: plan stage {stage} has invalid SHA-256 in {field}")
    return planned_set, stages


def validate_identity(identity: Any, plan: dict[str, Any], planned: set[tuple[str, str, str]], source: Path) -> tuple[dict[str, str], dict[str, dict[str, Any]]]:
    if not isinstance(identity, dict) or identity.get("passed") is not True:
        raise AnalysisError(f"{source}: identity_preflight did not pass")
    if identity.get("mismatches") != []:
        raise AnalysisError(f"{source}: identity_preflight contains mismatches")
    references = identity.get("reference_test_ids")
    selected_suites = set(plan.get("suites", []))
    if not isinstance(references, dict) or set(references) != selected_suites:
        raise AnalysisError(f"{source}: identity_preflight lacks one reference-test identity per selected suite")
    baseline: dict[str, str] = {}
    for suite, reference in references.items():
        if not isinstance(reference, dict):
            raise AnalysisError(f"{source}: {suite} reference test identity must be an object")
        digest = reference.get("sha256")
        if not isinstance(reference.get("source_cell"), str) or not reference["source_cell"]:
            raise AnalysisError(f"{source}: {suite} reference test identity lacks source_cell")
        if not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise AnalysisError(f"{source}: {suite} baseline identity hash is missing")
        if (type(reference.get("count")) is not int
                or reference.get("count") != EXPECTED_TESTS[suite]["total"]):
            raise AnalysisError(f"{source}: {suite} reference test identity count does not match workload")
        baseline[suite] = digest
    warmups = identity.get("warmup_test_identities")
    if not isinstance(warmups, dict) or set(warmups) != selected_suites:
        raise AnalysisError(f"{source}: identity_preflight lacks warm-up identities for selected suites")
    for suite, mode, stage in planned:
        run_id = cell_id(suite, mode, stage, None)
        details = warmups[suite].get(run_id) if isinstance(warmups[suite], dict) else None
        if (not isinstance(details, dict) or details.get("sha256") != baseline[suite]
                or type(details.get("count")) is not int
                or details.get("count") != EXPECTED_TESTS[suite]["total"]):
            raise AnalysisError(f"{source}: warm-up identity does not match baseline for {run_id}")

    fixture_map = plan.get("fixture_fingerprints")
    if not isinstance(fixture_map, dict):
        raise AnalysisError(f"{source}: plan.fixture_fingerprints must be an object")
    fixture_categories = {"java_parser_resources", "js_imported_resources", "clava_js_sources"}
    if set(fixture_map) != fixture_categories:
        raise AnalysisError(f"{source}: fixture fingerprints lack required categories")
    for category, per_stage in fixture_map.items():
        if not isinstance(per_stage, dict) or set(per_stage) != set(STAGES):
            raise AnalysisError(f"{source}: fixture category {category} must contain all four stage maps")
        for stage, files in per_stage.items():
            if not isinstance(files, dict):
                raise AnalysisError(f"{source}: fixture fingerprints for {category}/{stage} must be a file-hash map")
            for relative_path, digest in files.items():
                if (not isinstance(relative_path, str) or not relative_path
                        or not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest)):
                    raise AnalysisError(f"{source}: invalid fixture fingerprint for {category}/{stage}")
    return baseline, warmups


def expected_fixture_digest(plan: dict[str, Any], stage: str) -> str:
    fingerprints = plan["fixture_fingerprints"]
    payload = {category: fingerprints[category][stage] for category in fingerprints}
    encoded = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def test_identity_digest(identities: list[Any]) -> str:
    encoded = json.dumps(identities, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _attempt_key(row: Any, planned: set[tuple[str, str, str]], source: Path):
    if not isinstance(row, dict):
        raise AnalysisError(f"{source}: result rows must be objects")
    suite, mode, stage = row.get("suite"), row.get("mode"), row.get("stage")
    if not all(isinstance(part, str) for part in (suite, mode, stage)):
        raise AnalysisError(f"{source}: result row has invalid suite/mode/stage identifiers")
    cell = (suite, mode, stage)
    if cell not in planned:
        raise AnalysisError(f"{source}: unexpected suite/mode/stage row {cell}")
    measured, repeat = row.get("measured"), row.get("repeat")
    if measured is True:
        if type(repeat) is not int or repeat not in REPEATS:
            raise AnalysisError(f"{source}: measured {cell} row has invalid repeat {repeat!r}")
        slot = repeat
    elif measured is False and repeat in (None, 0):
        slot = 0
    else:
        raise AnalysisError(f"{source}: invalid measured/repeat values for {cell}")
    if row.get("cell_id") != cell_id(suite, mode, stage, None if slot == 0 else slot):
        raise AnalysisError(f"{source}: inconsistent cell_id for {cell} repeat {repeat!r}")
    attempt = row.get("attempt")
    if type(attempt) is not int or attempt < 1:
        raise AnalysisError(f"{source}: invalid attempt number for {cell} repeat {repeat!r}")
    if type(row.get("selected")) is not bool:
        raise AnalysisError(f"{source}: missing boolean selected flag for {cell} attempt {attempt}")
    return (*cell, slot), attempt


def _audit_attempt(row: dict[str, Any], key: tuple[str, str, str, int], attempt: int, source: Path) -> dict[str, Any]:
    next_attempt = row.get("superseded_by_attempt")
    if row.get("selected") is not False or row.get("valid") is not False:
        raise AnalysisError(f"{source}: unselected attempt {attempt} is not an invalid superseded attempt")
    if type(next_attempt) is not int or next_attempt != attempt + 1:
        raise AnalysisError(f"{source}: attempt {attempt} lacks a link to the next repair attempt")
    suite, mode, stage, repeat = key
    return {
        "suite": suite,
        "mode": mode,
        "stage": stage,
        "cell_id": row["cell_id"],
        "measured": row["measured"],
        "repeat": None if repeat == 0 else repeat,
        "attempt": attempt,
        "selected": False,
        "valid": False,
        "return_code": row.get("return_code"),
        "failure_names": row.get("failure_names", []),
        "superseded_by_attempt": next_attempt,
        "run_dir_relative": row.get("run_dir_relative"),
    }


def _validate_row(row: Any, plan: dict[str, Any], planned: set[tuple[str, str, str]], baseline: dict[str, str], source: Path) -> tuple[str, str, str, int | None]:
    if not isinstance(row, dict):
        raise AnalysisError(f"{source}: result rows must be objects")
    suite, mode, stage = row.get("suite"), row.get("mode"), row.get("stage")
    if not all(isinstance(part, str) for part in (suite, mode, stage)):
        raise AnalysisError(f"{source}: result row has invalid suite/mode/stage identifiers")
    key = (suite, mode, stage)
    if key not in planned:
        raise AnalysisError(f"{source}: unexpected suite/mode/stage row {key}")
    measured = row.get("measured")
    repeat = row.get("repeat")
    if measured is True:
        if type(repeat) is not int or repeat not in REPEATS:
            raise AnalysisError(f"{source}: measured {key} row has invalid repeat {repeat!r}")
        expected_id = cell_id(suite, mode, stage, repeat)
    elif measured is False:
        if repeat not in (None, 0):
            raise AnalysisError(f"{source}: warm-up {key} row must not have a measured repeat")
        repeat = None
        expected_id = cell_id(suite, mode, stage, None)
    else:
        raise AnalysisError(f"{source}: {key} row must declare measured as true or false")
    if row.get("cell_id") != expected_id:
        raise AnalysisError(f"{source}: inconsistent cell_id {row.get('cell_id')!r}; expected {expected_id!r}")
    if type(row.get("attempt")) is not int or row.get("attempt") < 1 or row.get("selected") is not True:
        raise AnalysisError(f"{source}: invalid selected attempt for {expected_id}")
    if row.get("superseded_by_attempt") is not None:
        raise AnalysisError(f"{source}: selected attempt is marked superseded for {expected_id}")

    counts = EXPECTED_TESTS[suite]
    count_fields = {
        "total_tests": "total", "passed_tests": "passed",
        "failed_tests": "failed", "skipped_tests": "skipped",
    }
    if row.get("valid") is not True or type(row.get("return_code")) is not int or row.get("return_code") != 0:
        raise AnalysisError(f"{source}: failed/invalid run {expected_id}")
    if any(type(row.get(field)) is not int or row.get(field) != counts[name]
           for field, name in count_fields.items()):
        raise AnalysisError(f"{source}: test-count mismatch in {expected_id}")
    if row.get("compile_tasks_clean") is not True:
        raise AnalysisError(f"{source}: compilation was not confirmed clean in {expected_id}")
    if row.get("failure_names") not in (None, []):
        raise AnalysisError(f"{source}: failure_names is non-empty in {expected_id}")
    if finite_number(row.get("elapsed_s"), f"{expected_id}.elapsed_s") <= 0:
        raise AnalysisError(f"{source}: elapsed_s must be positive in {expected_id}")

    validation = row.get("cache_validation")
    if not isinstance(validation, dict) or validation.get("passed") is not True:
        raise AnalysisError(f"{source}: cache validation failed or is absent in {expected_id}")
    counters = {
        name: row.get(name)
        for name in ("cacheable_calls", "cache_hits", "cache_misses", "uncacheable_calls")
    }
    if any(type(value) is not int or value < 0 for value in counters.values()):
        raise AnalysisError(f"{source}: invalid cache counters in {expected_id}")
    expected_applicable = stage != "before-cache"
    if validation.get("applicable") is not expected_applicable:
        raise AnalysisError(f"{source}: cache applicability is inconsistent in {expected_id}")
    validation_counters = {
        "cacheable_calls": validation.get("cacheable_calls"),
        "cache_hits": validation.get("hits"),
        "cache_misses": validation.get("misses"),
        "uncacheable_calls": validation.get("uncacheable_calls"),
    }
    if counters != validation_counters:
        raise AnalysisError(f"{source}: cache counters disagree with validation record in {expected_id}")
    if mode == "direct" and any(counters.values()):
        raise AnalysisError(f"{source}: Direct row has ccache activity in {expected_id}")
    if mode == "cold" and counters["cache_misses"] == 0:
        raise AnalysisError(f"{source}: cold row has no cache misses in {expected_id}")
    if mode == "warm" and measured and counters["cache_hits"] == 0:
        raise AnalysisError(f"{source}: warm row has no cache hits in {expected_id}")
    if mode == "warm" and not measured and counters["cache_misses"] == 0:
        raise AnalysisError(f"{source}: warm-up did not seed cache in {expected_id}")

    stage_metadata = plan["stages"][stage]
    for field in BUILD_PROVENANCE_FIELDS:
        if field not in row or row[field] != stage_metadata[field]:
            raise AnalysisError(f"{source}: {field} differs from fixed stage provenance in {expected_id}")
    if not isinstance(row.get("compression"), dict):
        raise AnalysisError(f"{source}: compression record is missing in {expected_id}")
    identities = row.get("test_identity")
    if not isinstance(identities, list) or len(identities) != counts["total"]:
        raise AnalysisError(f"{source}: test identity list has wrong length in {expected_id}")
    identity_hash = test_identity_digest(identities)
    if (row.get("test_identity_sha256") != identity_hash
            or row.get("test_identity_sha256") != baseline[suite]
            or row.get("workload_identity_match") is not True):
        raise AnalysisError(f"{source}: test identity differs from preflight in {expected_id}")
    if row.get("fixture_fingerprint_sha256") != expected_fixture_digest(plan, stage):
        raise AnalysisError(f"{source}: fixture fingerprint differs from the plan in {expected_id}")
    compression_policy = plan.get("compression_policy")
    if (not isinstance(compression_policy, dict)
            or not isinstance(compression_policy.get(stage), dict)
            or compression_policy[stage].get(mode) != row.get("compression")):
        raise AnalysisError(f"{source}: compression record differs from planned policy in {expected_id}")
    return suite, mode, stage, repeat


def validate_manifest(manifest: Any, source: Path) -> tuple[dict[str, Any], set[tuple[str, str, str]], dict[str, str], list[dict[str, Any]], list[dict[str, Any]]]:
    if not isinstance(manifest, dict) or manifest.get("schema_version") != 1:
        raise AnalysisError(f"{source}: expected results.json schema_version 1")
    plan = manifest.get("plan")
    planned, _ = validate_plan(plan, source)
    baseline, _ = validate_identity(manifest.get("identity_preflight"), plan, planned, source)
    rows = manifest.get("results")
    if not isinstance(rows, list):
        raise AnalysisError(f"{source}: results must be an array")

    attempts_by_cell: dict[tuple[str, str, str, int], dict[int, dict[str, Any]]] = {}
    for row in rows:
        key, attempt = _attempt_key(row, planned, source)
        attempts = attempts_by_cell.setdefault(key, {})
        if attempt in attempts:
            raise AnalysisError(f"{source}: duplicate attempt {attempt} for logical cell {key}")
        attempts[attempt] = row

    selected_rows, audit_rows = [], []
    for key, attempts in attempts_by_cell.items():
        attempt_numbers = sorted(attempts)
        if attempt_numbers != list(range(1, attempt_numbers[-1] + 1)):
            raise AnalysisError(f"{source}: attempt history for {key} is not contiguous from attempt 1")
        slot = key[-1]
        if slot == 0 and attempt_numbers != [1]:
            raise AnalysisError(f"{source}: warm-up {key} may not be retried")
        selected_attempts = [number for number, row in attempts.items() if row["selected"] is True]
        if len(selected_attempts) != 1:
            raise AnalysisError(f"{source}: logical cell {key} needs exactly one selected valid attempt")
        chosen = selected_attempts[0]
        if chosen != attempt_numbers[-1]:
            raise AnalysisError(f"{source}: selected attempt for {key} is not the terminal attempt")
        for number in attempt_numbers[:-1]:
            audit_rows.append(_audit_attempt(attempts[number], key, number, source))
        selected = attempts[chosen]
        _validate_row(selected, plan, planned, baseline, source)
        selected_rows.append(selected)
    return plan, planned, baseline, selected_rows, audit_rows


def pair_summary(pairs: list[dict[str, Any]], delta_key: str) -> dict[str, Any]:
    deltas = [pair[delta_key] for pair in pairs]
    return {
        "median_delta_s": statistics.median(deltas),
        "positive_count": sum(delta > 0 for delta in deltas),
        "negative_count": sum(delta < 0 for delta in deltas),
        "zero_count": sum(delta == 0 for delta in deltas),
        "pairs": pairs,
    }


def analyze_cohort(sources: list[Path], manifests: list[Any]) -> dict[str, Any]:
    if not sources or len(sources) != len(manifests):
        raise AnalysisError("at least one matching source manifest is required")
    plans, planned_cells, baselines = [], [], []
    baseline_by_suite: dict[str, str] = {}
    stage_by_key: dict[str, dict[str, Any]] = {}
    provenance_by_cell: dict[tuple[str, str], dict[str, Any]] = {}
    grouped: dict[tuple[str, str, str], dict[int, dict[str, Any]]] = {}
    attempt_audits: list[dict[str, Any]] = []
    for source, manifest in zip(sources, manifests):
        plan, planned, baseline, rows, audit = validate_manifest(manifest, source)
        # The unsuccessful retries remain in the audit output; only the one
        # selected valid attempt returned as `rows` enters any timing statistic.
        attempt_audits.extend(audit)
        plans.append(plan)
        planned_cells.append(planned)
        for suite, digest in baseline.items():
            if suite in baseline_by_suite and baseline_by_suite[suite] != digest:
                raise AnalysisError(f"{source}: baseline test identity differs across input manifests for {suite}")
            baseline_by_suite[suite] = digest
        for stage, metadata in plan["stages"].items():
            if stage in stage_by_key and stage_by_key[stage] != metadata:
                raise AnalysisError(f"{source}: stage provenance differs across input manifests for {stage}")
            stage_by_key[stage] = metadata
        for row in rows:
            cell = (row["suite"], row["mode"], row["stage"])
            repeat = row["repeat"] if row["measured"] else 0
            if repeat in grouped.setdefault(cell, {}):
                raise AnalysisError(f"duplicate row across inputs for {cell} repeat {repeat}")
            grouped[cell][repeat] = row
            cell_provenance_key = (row["mode"], row["stage"])
            cell_signature = {
                field: row[field] for field in (*BUILD_PROVENANCE_FIELDS, "compression")
            }
            previous = provenance_by_cell.setdefault(cell_provenance_key, cell_signature)
            if previous != cell_signature:
                raise AnalysisError(f"{source}: provenance/compression changed within {row['mode']}/{row['stage']}")

    union_cells = set().union(*planned_cells)
    if union_cells != expected_cells():
        missing = sorted(expected_cells() - union_cells)
        unexpected = sorted(union_cells - expected_cells())
        raise AnalysisError(f"input manifests do not cover full matrix; missing={missing}, unexpected={unexpected}")
    # Shared plan fields must match. The selected cells, modes, and stage maps
    # may differ when the inputs are deliberately split into Direct/Cold/Warm.
    common_keys = ("schema_version", "repeat_count", "warmup_count",
                   "expected_tests", "fixture_fingerprints",
                   "timing_boundary", "experiment")
    common = {key: plans[0].get(key) for key in common_keys}
    for source, plan in zip(sources[1:], plans[1:]):
        if any(plan.get(key) != common[key] for key in common_keys):
            raise AnalysisError(f"{source}: plan provenance/workload differs from other input manifests")
    planned_suites_union = set().union(*(set(plan.get("suites", [])) for plan in plans))
    if planned_suites_union != set(SUITES):
        raise AnalysisError(f"input manifests do not cover both expected suites: {sorted(planned_suites_union)}")
    for suite in SUITES:
        if suite not in baseline_by_suite:
            raise AnalysisError(f"input manifests lack identity baseline for {suite}")
    if set(grouped) != expected_cells():
        raise AnalysisError("result rows do not cover every expected matrix cell")
    for key in sorted(expected_cells()):
        attempts = grouped[key]
        measured_ids = {repeat for repeat in attempts if repeat != 0}
        if measured_ids != set(REPEATS):
            raise AnalysisError(f"incomplete {key}: expected measured repeats 1-6, found {sorted(measured_ids)}")
        if len(attempts) > 7:
            raise AnalysisError(f"too many warm-up/measurement attempts for {key}")

    plan = plans[0]
    plan = dict(plan)
    plan["stages"] = stage_by_key
    plan["fixture_fingerprints"] = common["fixture_fingerprints"]
    runs: dict[tuple[str, str, str], dict[int, float]] = {}
    summaries = []
    for suite in SUITES:
        for mode in MODES:
            for stage in STAGES_BY_MODE[mode]:
                key = (suite, mode, stage)
                values = {
                    repeat: finite_number(grouped[key][repeat]["elapsed_s"], "elapsed_s")
                    for repeat in REPEATS
                }
                runs[key] = values
                summaries.append({
                    "suite": suite, "mode": mode, "stage": stage,
                    "n": len(values), **stats(list(values.values())),
                })

    comparisons = []
    for suite in SUITES:
        for mode in MODES:
            reference = runs[(suite, mode, "ccache-text")]
            reference_median = statistics.median(reference.values())
            for stage in STAGES_BY_MODE[mode]:
                if stage == "ccache-text":
                    continue
                candidate = runs[(suite, mode, stage)]
                delta_pairs = [
                    {
                        "repeat": repeat,
                        "reference_s": reference[repeat],
                        "candidate_s": candidate[repeat],
                        "delta_s_candidate_minus_reference": candidate[repeat] - reference[repeat],
                        "relative_effect_pct": 100 * (candidate[repeat] / reference[repeat] - 1),
                    }
                    for repeat in REPEATS
                ]
                comparisons.append({
                    "kind": "vs_text_same_mode",
                    "suite": suite, "mode": mode, "stage": stage,
                    "delta_definition": "candidate elapsed_s minus Text elapsed_s; positive means candidate took longer",
                    "median_relative_effect_pct": 100 * (statistics.median(candidate.values()) / reference_median - 1),
                    "median_paired_relative_effect_pct": statistics.median(
                        pair["relative_effect_pct"] for pair in delta_pairs
                    ),
                    **pair_summary(delta_pairs, "delta_s_candidate_minus_reference"),
                })

    for suite in SUITES:
        for stage in STAGES_BY_MODE["cold"]:
            direct = runs[(suite, "direct", stage)]
            for mode in ("cold", "warm"):
                cached = runs[(suite, mode, stage)]
                pairs = [
                    {
                        "repeat": repeat,
                        "direct_s": direct[repeat],
                        "cached_s": cached[repeat],
                        "benefit_s_direct_minus_cached": direct[repeat] - cached[repeat],
                        "relative_benefit_pct": 100 * (direct[repeat] / cached[repeat] - 1),
                    }
                    for repeat in REPEATS
                ]
                comparisons.append({
                    "kind": "cache_benefit_vs_same_stage_direct",
                    "suite": suite, "mode": mode, "stage": stage,
                    "delta_definition": "Direct elapsed_s minus cached elapsed_s; positive means time saved under cache",
                    "median_relative_benefit_pct": 100 * (statistics.median(direct.values()) / statistics.median(cached.values()) - 1),
                    "median_paired_relative_benefit_pct": statistics.median(
                        pair["relative_benefit_pct"] for pair in pairs
                    ),
                    **pair_summary(pairs, "benefit_s_direct_minus_cached"),
                })

    combined = []
    for mode in MODES:
        for stage in STAGES_BY_MODE[mode]:
            pairs = [
                {
                    "repeat": repeat,
                    "clava_js_s": runs[("clava-js", mode, stage)][repeat],
                    "java_s": runs[("java", mode, stage)][repeat],
                    "combined_s": runs[("clava-js", mode, stage)][repeat] + runs[("java", mode, stage)][repeat],
                }
                for repeat in REPEATS
            ]
            values = [pair["combined_s"] for pair in pairs]
            combined.append({
                "mode": mode, "stage": stage, "n": len(values), **stats(values),
                "label": "combined two-suite time; descriptive sum only, not production weighting",
                "pairs": pairs,
            })

    return {
        "cohort": ", ".join(source.stem for source in sources),
        "source_manifests": [source.name for source in sources],
        "experiment": plan.get("experiment"),
        "repeat_count": 6,
        "timing_field": "elapsed_s (runner subprocess wall time)",
        "summaries": summaries,
        "comparisons": comparisons,
        "combined_two_suite_time": combined,
        "attempt_audit": attempt_audits,
        "note": "Descriptive statistics only; no automatic winner or technology recommendation.",
    }


def csv_rows(cohorts: list[dict[str, Any]]) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    for cohort in cohorts:
        name = cohort["cohort"]
        for summary in cohort["summaries"]:
            output.append({"record_type": "cell_summary", "cohort": name, **summary})
        for comparison in cohort["comparisons"]:
            common = {key: comparison[key] for key in ("kind", "suite", "mode", "stage") if key in comparison}
            output.append({
                "record_type": "comparison_summary", "cohort": name, **common,
                "median_relative_effect_pct": comparison.get("median_relative_effect_pct"),
                "median_paired_relative_effect_pct": comparison.get("median_paired_relative_effect_pct"),
                "median_relative_benefit_pct": comparison.get("median_relative_benefit_pct"),
                "median_paired_relative_benefit_pct": comparison.get("median_paired_relative_benefit_pct"),
                "median_delta_s": comparison["median_delta_s"],
                "delta_definition": comparison["delta_definition"],
                "positive_count": comparison["positive_count"],
                "negative_count": comparison["negative_count"],
                "zero_count": comparison["zero_count"],
            })
            for pair in comparison["pairs"]:
                output.append({"record_type": "paired_delta", "cohort": name, **common, **pair})
        for combined in cohort["combined_two_suite_time"]:
            output.append({
                "record_type": "combined_two_suite_summary", "cohort": name,
                **{key: combined[key] for key in ("mode", "stage", "n", "median_s", "min_s", "max_s", "q1_s", "q3_s", "iqr_s", "label")},
            })
            for pair in combined["pairs"]:
                output.append({"record_type": "combined_two_suite_pair", "cohort": name, "mode": combined["mode"], "stage": combined["stage"], **pair})
        for attempt in cohort["attempt_audit"]:
            output.append({"record_type": "attempt_audit", "cohort": name, **attempt})
    return output


def load_manifest(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read {path}: {error}") from error


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, action="append", required=True,
                        help="complete manifest or disjoint mode/suite shard for one matrix; duplicates are rejected")
    parser.add_argument("--output", type=Path, required=True,
                        help="JSON output path; CSV is written beside it with a .csv suffix")
    args = parser.parse_args(argv)
    if len({path.resolve() for path in args.input}) != len(args.input):
        raise AnalysisError("the same input manifest was supplied more than once")
    csv_path = args.output.with_suffix(".csv")
    input_paths = {path.resolve() for path in args.input}
    if args.output.resolve() in input_paths or csv_path.resolve() in input_paths:
        raise AnalysisError("output paths must not overwrite an input manifest")
    cohorts = [analyze_cohort(args.input, [load_manifest(path) for path in args.input])]
    payload = {
        "schema_version": 1,
        "analysis": "fresh protocol/cache deadline matrix",
        "cohorts": cohorts,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    fields = [
        "record_type", "cohort", "suite", "mode", "stage", "kind", "n",
        "median_s", "min_s", "max_s", "q1_s", "q3_s", "iqr_s",
        "median_relative_effect_pct", "median_paired_relative_effect_pct",
        "median_relative_benefit_pct", "median_paired_relative_benefit_pct", "median_delta_s",
        "positive_count", "negative_count", "zero_count", "repeat", "delta_definition",
        "attempt", "selected", "valid", "return_code", "failure_names", "superseded_by_attempt",
        "run_dir_relative",
        "reference_s", "candidate_s", "delta_s_candidate_minus_reference", "relative_effect_pct",
        "direct_s", "cached_s", "benefit_s_direct_minus_cached", "relative_benefit_pct",
        "clava_js_s", "java_s", "combined_s", "label",
    ]
    with csv_path.open("w", encoding="utf-8", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(csv_rows(cohorts))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
