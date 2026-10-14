#!/usr/bin/env python3
"""Summarize phase and test-file timings from the 2026-09-23 comparisons."""

from __future__ import annotations

import argparse
import json
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any


COMPARISON_MODES = ("direct", "cold", "warm")
EXPECTED_REPEATS = 6
EXPECTED_JAVA_METRIC_LINES = 216
SUITES = ("java", "clava-js")
JAVA_METRICS = {
    "code_to_ast": re.compile(
        r"^\[ClavaMetrics\] Code to AST:\s*([0-9]+(?:\.[0-9]+)?)\s*(ms|s)\s*$"
    ),
    "ast_processing": re.compile(
        r"^\[ClavaMetrics\] AST Processing:\s*([0-9]+(?:\.[0-9]+)?)\s*(ms|s)\s*$"
    ),
}
STRUCTURED_METRIC_LINE = re.compile(r"^(CLAVA_AST_METRIC|PROTOBUF_METRIC)\s+(\{[^\n]+\})\s*$")
VITEST_FILES = {
    "c": "LegacyIntegrationTests - C.test.ts",
    "cxx": "LegacyIntegrationTests - CXX.test.ts",
}


class AnalysisError(Exception):
    """Raised when an input is incomplete or inconsistent with the experiment."""


def seconds(value: float, unit: str) -> float:
    return value / 1000.0 if unit == "ms" else value


def rounded(value: float) -> float:
    return round(value, 6)


def median(rows: list[dict[str, Any]], field: str) -> float:
    return rounded(statistics.median(float(row[field]) for row in rows))


def default_manifests(comparison_dir: Path) -> list[Path]:
    return [
        comparison_dir / "results" / f"comparison-20260923-{mode}" / "results.json"
        for mode in COMPARISON_MODES
    ]


def resolve_run_dir(manifest_path: Path, row: dict[str, Any]) -> Path:
    recorded = Path(str(row.get("run_dir", "")))
    candidates: list[Path] = []
    if recorded.is_absolute():
        candidates.append(recorded)
    else:
        candidates.append(manifest_path.parent / recorded)
    candidates.append(
        manifest_path.parent / "runs" / str(row.get("suite", "")) / recorded.name
    )
    for candidate in candidates:
        if candidate.is_dir():
            return candidate
    tried = ", ".join(str(path) for path in candidates)
    raise AnalysisError(f"run directory for {row.get('stage')} repeat {row.get('repeat')} not found; tried {tried}")


def parse_java_log(path: Path) -> dict[str, Any]:
    totals = {name: 0.0 for name in JAVA_METRICS}
    counts = {name: 0 for name in JAVA_METRICS}
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as error:
        raise AnalysisError(f"cannot read Java run log {path}: {error}") from error

    for line in lines:
        for name, pattern in JAVA_METRICS.items():
            match = pattern.match(line.strip())
            if match:
                totals[name] += seconds(float(match.group(1)), match.group(2))
                counts[name] += 1

    for name, count in counts.items():
        if count != EXPECTED_JAVA_METRIC_LINES:
            raise AnalysisError(
                f"{path}: expected {EXPECTED_JAVA_METRIC_LINES} {name} metric lines, found {count}"
            )
    return {
        "code_to_ast_count": counts["code_to_ast"],
        "code_to_ast_s": rounded(totals["code_to_ast"]),
        "ast_processing_count": counts["ast_processing"],
        "ast_processing_s": rounded(totals["ast_processing"]),
    }


def parse_structured_phase_metrics(path: Path) -> dict[str, Any]:
    by_marker: dict[str, list[dict[str, Any]]] = defaultdict(list)
    try:
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as error:
        raise AnalysisError(f"cannot read run log {path}: {error}") from error

    for line_number, line in enumerate(lines, start=1):
        match = STRUCTURED_METRIC_LINE.match(line.strip())
        if not match:
            continue
        try:
            metric = json.loads(match.group(2))
        except json.JSONDecodeError as error:
            raise AnalysisError(f"{path}:{line_number}: malformed structured metric JSON: {error}") from error
        if not isinstance(metric, dict):
            raise AnalysisError(f"{path}:{line_number}: structured metric must be a JSON object")
        by_marker[match.group(1)].append(metric)

    parsed: dict[str, Any] = {}
    for marker, metrics in by_marker.items():
        phase_values: dict[str, list[float]] = defaultdict(list)
        for metric in metrics:
            for field, value in metric.items():
                if not field.endswith("_ms") or not isinstance(value, (int, float)) or isinstance(value, bool):
                    continue
                phase_name = str(metric.get("phase")) if field == "duration_ms" and metric.get("phase") else field[:-3]
                phase_values[phase_name].append(float(value))

        parsed[marker] = {
            "metric_event_count": len(metrics),
            "phase_event_counts": {phase: len(values) for phase, values in sorted(phase_values.items())},
            "phase_sums_ms": {
                phase: rounded(sum(values)) for phase, values in sorted(phase_values.items())
            },
            "phase_event_medians_ms": {
                phase: rounded(statistics.median(values))
                for phase, values in sorted(phase_values.items())
            },
        }
    return parsed


def parse_vitest_file_durations(path: Path) -> dict[str, float]:
    try:
        report = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read Vitest JSON report {path}: {error}") from error

    matches: dict[str, list[float]] = {name: [] for name in VITEST_FILES}
    for test_result in report.get("testResults", []):
        name = Path(str(test_result.get("name", ""))).name
        for key, filename in VITEST_FILES.items():
            if name == filename:
                start = test_result.get("startTime")
                end = test_result.get("endTime")
                if not isinstance(start, (int, float)) or not isinstance(end, (int, float)):
                    raise AnalysisError(f"{path}: {filename} has no numeric startTime/endTime")
                if end < start:
                    raise AnalysisError(f"{path}: {filename} ends before it starts")
                matches[key].append((end - start) / 1000.0)

    for key, filename in VITEST_FILES.items():
        if len(matches[key]) != 1:
            raise AnalysisError(
                f"{path}: expected one Vitest result for {filename}, found {len(matches[key])}"
            )
    return {key: rounded(values[0]) for key, values in matches.items()}


def analyze_run(
    manifest_path: Path, manifest_mode: str, result: dict[str, Any]
) -> dict[str, Any]:
    run_dir = resolve_run_dir(manifest_path, result)
    elapsed = result.get("elapsed_s")
    if not isinstance(elapsed, (int, float)) or elapsed <= 0:
        raise AnalysisError(f"{manifest_path}: invalid elapsed_s for {run_dir}")
    if result.get("valid") is not True or result.get("exit_status") != 0:
        raise AnalysisError(f"{manifest_path}: measured run is not valid: {run_dir}")

    row: dict[str, Any] = {
        "suite": result["suite"],
        "mode": manifest_mode,
        "stage": result["stage"],
        "label": result.get("label", result["stage"]),
        "repeat": int(result["repeat"]),
        "run_dir": str(run_dir),
        "elapsed_s": rounded(float(elapsed)),
    }
    row["structured_phase_metrics"] = parse_structured_phase_metrics(run_dir / "run.log")

    if result["suite"] == "java":
        phase_times = parse_java_log(run_dir / "run.log")
        row.update(phase_times)
        row["residual_s"] = rounded(
            float(elapsed) - phase_times["code_to_ast_s"] - phase_times["ast_processing_s"]
        )
    elif result["suite"] == "clava-js":
        file_times = parse_vitest_file_durations(run_dir / "vitest.json")
        row["c_file_s"] = file_times["c"]
        row["cxx_file_s"] = file_times["cxx"]
        row["c_and_cxx_s"] = rounded(file_times["c"] + file_times["cxx"])
        row["residual_s"] = rounded(float(elapsed) - row["c_and_cxx_s"])
    else:
        raise AnalysisError(f"unexpected suite {result['suite']!r} in {manifest_path}")

    return row


def summarize_cell(rows: list[dict[str, Any]]) -> dict[str, Any]:
    first = rows[0]
    summary: dict[str, Any] = {
        "suite": first["suite"],
        "mode": first["mode"],
        "stage": first["stage"],
        "label": first["label"],
        "repeat_count": len(rows),
        "repeat_ids": [row["repeat"] for row in rows],
        "median_elapsed_s": median(rows, "elapsed_s"),
        "median_residual_s": median(rows, "residual_s"),
        "runs": rows,
    }
    if first["suite"] == "java":
        summary.update(
            {
                "java_metric_lines_per_run": {
                    "code_to_ast": EXPECTED_JAVA_METRIC_LINES,
                    "ast_processing": EXPECTED_JAVA_METRIC_LINES,
                },
                "median_code_to_ast_s": median(rows, "code_to_ast_s"),
                "median_ast_processing_s": median(rows, "ast_processing_s"),
            }
        )
    else:
        summary.update(
            {
                "median_c_file_s": median(rows, "c_file_s"),
                "median_cxx_file_s": median(rows, "cxx_file_s"),
                "median_c_and_cxx_s": median(rows, "c_and_cxx_s"),
            }
        )
    structured_summary = summarize_structured_phase_metrics(rows)
    if structured_summary:
        summary["structured_phase_metrics"] = structured_summary
    return summary


def summarize_structured_phase_metrics(rows: list[dict[str, Any]]) -> dict[str, Any]:
    markers = sorted(
        {
            marker
            for row in rows
            for marker in row.get("structured_phase_metrics", {})
        }
    )
    summary: dict[str, Any] = {}
    for marker in markers:
        run_metrics = [
            row["structured_phase_metrics"][marker]
            for row in rows
            if marker in row.get("structured_phase_metrics", {})
        ]
        phase_names = sorted(
            {
                phase
                for run_metric in run_metrics
                for phase in run_metric["phase_sums_ms"]
            }
        )
        summary[marker] = {
            "run_count_with_metrics": len(run_metrics),
            "median_metric_event_count_per_run": rounded(
                statistics.median(metric["metric_event_count"] for metric in run_metrics)
            ),
            "median_phase_sums_ms_per_run": {
                phase: rounded(
                    statistics.median(
                        metric["phase_sums_ms"][phase]
                        for metric in run_metrics
                        if phase in metric["phase_sums_ms"]
                    )
                )
                for phase in phase_names
            },
            "median_phase_event_ms_per_run": {
                phase: rounded(
                    statistics.median(
                        metric["phase_event_medians_ms"][phase]
                        for metric in run_metrics
                        if phase in metric["phase_event_medians_ms"]
                    )
                )
                for phase in phase_names
            },
        }
    return summary


def analyze_phase_followup(path: Path) -> dict[str, Any]:
    manifest_path = path / "results.json" if path.is_dir() else path
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read phase follow-up manifest {manifest_path}: {error}") from error

    expected_repeats = manifest.get("repeat_count")
    if not isinstance(expected_repeats, int) or expected_repeats < 1:
        raise AnalysisError(f"{manifest_path}: invalid repeat_count")
    plan_path = manifest_path.parent / "plan.json"
    try:
        plan = json.loads(plan_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        plan = {}
    stage_rows = plan.get("stages") or manifest.get("stages", [])
    stage_metadata = {stage["key"]: stage for stage in stage_rows}

    grouped: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    for result in manifest.get("results", []):
        if result.get("measured") is not True:
            continue
        run_dir = resolve_run_dir(manifest_path, result)
        row = {
            "suite": result["suite"],
            "mode": manifest.get("mode"),
            "stage": result["stage"],
            "label": result.get("label", result["stage"]),
            "repeat": int(result["repeat"]),
            "run_dir": str(run_dir),
            "elapsed_s": rounded(float(result["elapsed_s"])),
            "return_code": result.get("return_code"),
            "exit_status": result.get("exit_status"),
            "valid": result.get("valid") is True and result.get("exit_status", result.get("return_code")) == 0,
            "runtime_parser_jar_sha256": result.get("runtime_parser_jar_sha256"),
            "test_counts": {
                key: result.get(key)
                for key in ("total_tests", "passed_tests", "failed_tests", "skipped_tests")
                if key in result
            },
            "failure_names": result.get("failure_names", []),
            "structured_phase_metrics": parse_structured_phase_metrics(run_dir / "run.log"),
        }
        grouped[(row["suite"], row["mode"], row["stage"])].append(row)

    expected_suite = manifest.get("suite")
    if expected_suite == "all":
        expected_suites = set(SUITES)
    elif expected_suite in SUITES:
        expected_suites = {expected_suite}
    else:
        raise AnalysisError(f"{manifest_path}: unexpected suite selector {expected_suite!r}")
    expected_pairs = {
        (suite, stage)
        for suite in expected_suites
        for stage in stage_metadata
    }
    actual_pairs = {(suite, stage) for suite, _, stage in grouped}
    if actual_pairs != expected_pairs:
        missing = sorted(expected_pairs - actual_pairs)
        unexpected = sorted(actual_pairs - expected_pairs)
        raise AnalysisError(
            f"{manifest_path}: incomplete measured suite/stage cells; missing={missing}, unexpected={unexpected}"
        )

    cells: list[dict[str, Any]] = []
    metric_count = 0
    valid_run_count = 0
    for (suite, mode, stage), rows in sorted(grouped.items()):
        rows.sort(key=lambda row: row["repeat"])
        expected_ids = list(range(1, expected_repeats + 1))
        repeat_ids = [row["repeat"] for row in rows]
        if repeat_ids != expected_ids:
            raise AnalysisError(
                f"{manifest_path}: {suite}/{mode}/{stage} expected repeats {expected_ids}, found {repeat_ids}"
            )
        metric_count += sum(
            metrics["metric_event_count"]
            for row in rows
            for metrics in row["structured_phase_metrics"].values()
        )
        valid_run_count += sum(row["valid"] for row in rows)
        cells.append(
            {
                "suite": suite,
                "mode": mode,
                "stage": stage,
                "label": rows[0]["label"],
                "repeat_count": len(rows),
                "repeat_ids": repeat_ids,
                "median_elapsed_s": median(rows, "elapsed_s"),
                "valid_run_count": sum(row["valid"] for row in rows),
                "invalid_run_count": sum(not row["valid"] for row in rows),
                "revision": {
                    key: stage_metadata.get(stage, {}).get(key)
                    for key in (
                        "clava_branch",
                        "clava_revision",
                        "dumper_branch",
                        "dumper_revision",
                        "dumper_sha256",
                    )
                }
                | {
                    "runtime_manifest_sha256": stage_metadata.get(stage, {})
                    .get("runtime_manifest", {})
                    .get("sha256")
                },
                "transport": {
                    "wire": stage_metadata.get(stage, {}).get("wire"),
                    "cache": stage_metadata.get(stage, {}).get("cache"),
                },
                "structured_phase_metrics": summarize_structured_phase_metrics(rows),
                "runs": rows,
            }
        )
    if not cells:
        raise AnalysisError(f"{manifest_path}: no measured phase follow-up runs")
    if metric_count == 0:
        raise AnalysisError(f"{manifest_path}: no CLAVA_AST_METRIC or PROTOBUF_METRIC lines found")

    created_at = manifest.get("created_at")
    return {
        "manifest": str(manifest_path.resolve()),
        "plan": str(plan_path.resolve()) if plan_path.is_file() else None,
        "created_at": created_at,
        "cohort_key": f"{str(created_at)[:10]}-{manifest.get('mode')}-{expected_suite}",
        "mode": manifest.get("mode"),
        "suite": expected_suite,
        "workload": {
            "suite_selector": expected_suite,
            "clava_js_filter": plan.get("clava_js_filter", manifest.get("clava_js_filter")),
            "java_init_script": plan.get("java_init_script", manifest.get("java_init_script")),
            "cache_mode_validation": plan.get(
                "cache_mode_validation", manifest.get("cache_mode_validation")
            ),
        },
        "repeat_count_expected": expected_repeats,
        "expected_cell_count": len(expected_pairs),
        "checked_cell_count": len(cells),
        "measured_run_count": sum(len(cell["runs"]) for cell in cells),
        "valid_run_count": valid_run_count,
        "invalid_run_count": sum(len(cell["runs"]) for cell in cells) - valid_run_count,
        "metric_event_count": metric_count,
        "cells": cells,
    }


def analyze(manifest_paths: list[Path]) -> dict[str, Any]:
    if len(manifest_paths) != len(COMPARISON_MODES):
        raise AnalysisError(f"expected exactly {len(COMPARISON_MODES)} manifests, received {len(manifest_paths)}")

    seen_modes: set[str] = set()
    all_rows: list[dict[str, Any]] = []
    manifest_metadata: list[dict[str, str]] = []
    stage_order: dict[str, int] = {}
    java_run_count = 0

    for manifest_path in manifest_paths:
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise AnalysisError(f"cannot read manifest {manifest_path}: {error}") from error
        mode = manifest.get("mode")
        if mode not in COMPARISON_MODES:
            raise AnalysisError(f"unexpected mode {mode!r} in {manifest_path}")
        if mode in seen_modes:
            raise AnalysisError(f"more than one manifest supplied for mode {mode!r}")
        seen_modes.add(mode)
        if manifest.get("repeat_count") != EXPECTED_REPEATS:
            raise AnalysisError(
                f"{manifest_path}: expected repeat_count={EXPECTED_REPEATS}, found {manifest.get('repeat_count')}"
            )
        if manifest.get("suite") != "all":
            raise AnalysisError(f"{manifest_path}: expected suite='all', found {manifest.get('suite')!r}")

        for index, stage in enumerate(manifest.get("stages", [])):
            stage_order.setdefault(stage["key"], index)
        manifest_metadata.append({"mode": mode, "path": str(manifest_path.resolve())})
        measured = [row for row in manifest.get("results", []) if row.get("measured") is True]
        if not measured:
            raise AnalysisError(f"{manifest_path}: no measured results")
        for result in measured:
            row = analyze_run(manifest_path, mode, result)
            java_run_count += row["suite"] == "java"
            all_rows.append(row)

    missing_modes = set(COMPARISON_MODES) - seen_modes
    if missing_modes:
        raise AnalysisError(f"missing manifests for modes: {', '.join(sorted(missing_modes))}")

    grouped: dict[tuple[str, str, str], list[dict[str, Any]]] = defaultdict(list)
    for row in all_rows:
        grouped[(row["mode"], row["suite"], row["stage"])].append(row)

    cells: list[dict[str, Any]] = []
    for (mode, suite, stage), rows in grouped.items():
        rows.sort(key=lambda row: row["repeat"])
        expected_ids = list(range(1, EXPECTED_REPEATS + 1))
        repeat_ids = [row["repeat"] for row in rows]
        if repeat_ids != expected_ids:
            raise AnalysisError(
                f"{mode}/{suite}/{stage}: expected repeat IDs {expected_ids}, found {repeat_ids}"
            )
        cells.append(summarize_cell(rows))

    mode_order = {mode: index for index, mode in enumerate(COMPARISON_MODES)}
    suite_order = {suite: index for index, suite in enumerate(SUITES)}
    cells.sort(
        key=lambda cell: (
            mode_order[cell["mode"]],
            suite_order[cell["suite"]],
            stage_order.get(cell["stage"], 999),
        )
    )

    return {
        "schema_version": 1,
        "experiment_date": "2026-09-23",
        "inputs": manifest_metadata,
        "measurement_definitions": {
            "java_metric_sums": "Sum of logged ClavaMetrics values within one Java run, converted to seconds.",
            "java_metrics_vs_protocol_metrics": "Code to AST and AST Processing are enclosing ClavaMetrics spans. Per-TU protocol metrics occur inside AST parsing and overlap those spans, so do not add values across these measurement levels.",
            "clava_js_file_durations": "Vitest testResults endTime minus startTime for the C and CXX integration test files.",
            "residual_s": "Per-run whole elapsed_s minus the two recorded Java metrics or the two Clava-JS file durations. This is arithmetic remainder, not a separately timed phase.",
            "whole_run_elapsed": "elapsed_s from the comparison manifest.",
            "structured_phase_metrics": "For each CLAVA_AST_METRIC or PROTOBUF_METRIC log, numeric *_ms fields are summed by phase name per run. Per-TU occupancy sums can overlap across parser jobs and with enclosing ClavaMetrics spans; they are not added to each other or treated as wall-time components.",
        },
        "validation": {
            "expected_repeats_per_cell": EXPECTED_REPEATS,
            "checked_cells": len(cells),
            "measured_run_count": len(all_rows),
            "java_measured_run_count": java_run_count,
            "expected_java_metric_lines_per_run_and_type": EXPECTED_JAVA_METRIC_LINES,
            "all_cells_have_six_repeats": True,
            "all_java_metric_counts_match": True,
        },
        "cells": cells,
    }


def main() -> int:
    comparison_dir = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        type=Path,
        action="append",
        help="comparison results.json path; provide one each for direct, cold, and warm (defaults to the stored 20260923 manifests)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=comparison_dir / "results" / "protocol-breakdown-20260923.json",
        help="JSON output path (default: results/protocol-breakdown-20260923.json)",
    )
    parser.add_argument(
        "--phase-followup",
        type=Path,
        action="append",
        help="optional completed comparison output directory or results.json with structured protocol metrics",
    )
    args = parser.parse_args()
    manifests = args.input or default_manifests(comparison_dir)

    try:
        output = analyze(manifests)
        if args.phase_followup:
            output["phase_followups"] = [analyze_phase_followup(path) for path in args.phase_followup]
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
    except AnalysisError as error:
        print(f"analysis failed: {error}", file=sys.stderr)
        return 1
    except OSError as error:
        print(f"cannot write output {args.output}: {error}", file=sys.stderr)
        return 1

    print(
        f"Wrote {args.output}; validated {output['validation']['checked_cells']} cells, "
        f"{output['validation']['measured_run_count']} measured runs, and "
        f"{output['validation']['java_measured_run_count']} Java logs."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
