#!/usr/bin/env python3
"""Produce a local-only paired analysis of a validated App-build matrix.

All comparisons pair the same suite and repeat. Per-group summaries are
descriptive decompositions over the four repeats, not independent samples.
This tool intentionally performs no significance tests and is not a report
renderer; its raw group IDs must remain in local diagnostic output.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import statistics
import sys
from typing import Any


REPORT_DIR = Path(__file__).resolve().parents[1] / "report"
sys.path.insert(0, str(REPORT_DIR))
import render_app_build  # noqa: E402


def _percent_change(reference: float, candidate: float) -> float | None:
    if reference <= 0:
        return None
    return 100.0 * (candidate / reference - 1.0)


def _total_summary(totals: dict[tuple[str, str, str], list[float]]) -> list[dict[str, Any]]:
    result = []
    for (suite, mode, stage), values in sorted(totals.items()):
        result.append({
            "suite": suite,
            "mode": mode,
            "stage": stage,
            "repeat_totals_ms": values,
            "repeat_count": len(values),
            "median_total_ms": statistics.median(values),
        })
    return result


def _contrast(
    data: dict[str, Any],
    totals: dict[tuple[str, str, str], list[float]],
    suite: str,
    reference_mode: str,
    reference_stage: str,
    candidate_mode: str,
    candidate_stage: str,
    comparison: str,
) -> dict[str, Any]:
    """Compare candidate minus reference by paired round and workload group."""
    reference_totals = totals[(suite, reference_mode, reference_stage)]
    candidate_totals = totals[(suite, candidate_mode, candidate_stage)]
    per_repeat = []
    for repeat, (reference, candidate) in enumerate(zip(reference_totals, candidate_totals), start=1):
        per_repeat.append({
            "repeat": repeat,
            "reference_total_ms": reference,
            "candidate_total_ms": candidate,
            "delta_ms_candidate_minus_reference": candidate - reference,
            "change_pct_candidate_vs_reference": _percent_change(reference, candidate),
        })

    group_differences = []
    ids = sorted(data["ids_by_suite"][suite])
    for group_id in ids:
        reference_values = [
            data["values_ms"][(suite, reference_stage, reference_mode, repeat, group_id)]
            for repeat in range(1, render_app_build.REPEAT_COUNT + 1)
        ]
        candidate_values = [
            data["values_ms"][(suite, candidate_stage, candidate_mode, repeat, group_id)]
            for repeat in range(1, render_app_build.REPEAT_COUNT + 1)
        ]
        deltas = [candidate - reference for reference, candidate in zip(reference_values, candidate_values)]
        percentages = [
            _percent_change(reference, candidate)
            for reference, candidate in zip(reference_values, candidate_values)
        ]
        group_differences.append({
            "group_id": group_id,
            "source_count": data["groups"][(suite, group_id)],
            "median_delta_ms_candidate_minus_reference": statistics.median(deltas),
            "median_change_pct_candidate_vs_reference": render_app_build.median_or_none(percentages),
        })

    group_differences.sort(
        key=lambda item: (
            -abs(item["median_delta_ms_candidate_minus_reference"]),
            item["group_id"],
        )
    )
    absolute_group_medians = sum(
        abs(item["median_delta_ms_candidate_minus_reference"])
        for item in group_differences
    )
    for item in group_differences:
        item["share_of_absolute_group_median_deltas_pct"] = (
            100.0 * abs(item["median_delta_ms_candidate_minus_reference"])
            / absolute_group_medians
            if absolute_group_medians > 0 else None
        )

    positive = sum(item["median_delta_ms_candidate_minus_reference"] > 0 for item in group_differences)
    negative = sum(item["median_delta_ms_candidate_minus_reference"] < 0 for item in group_differences)
    zero = len(group_differences) - positive - negative
    round_deltas = [item["delta_ms_candidate_minus_reference"] for item in per_repeat]
    round_percentages = [item["change_pct_candidate_vs_reference"] for item in per_repeat]
    return {
        "comparison": comparison,
        "suite": suite,
        "reference": {"mode": reference_mode, "stage": reference_stage},
        "candidate": {"mode": candidate_mode, "stage": candidate_stage},
        "per_repeat": per_repeat,
        "summary": {
            "repeat_count": len(per_repeat),
            "median_delta_ms_candidate_minus_reference": statistics.median(round_deltas),
            "median_change_pct_candidate_vs_reference": render_app_build.median_or_none(round_percentages),
            "positive_rounds": sum(value > 0 for value in round_deltas),
            "negative_rounds": sum(value < 0 for value in round_deltas),
            "zero_rounds": sum(value == 0 for value in round_deltas),
        },
        "group_direction_counts_by_median_delta": {
            "positive": positive,
            "negative": negative,
            "zero": zero,
            "group_count": len(group_differences),
        },
        "groups_ranked_by_absolute_median_delta": group_differences,
    }


def analyze(payload: Any) -> dict[str, Any]:
    data = render_app_build.validate_input(payload)
    totals = render_app_build.aggregate(data)
    contrasts = []
    for suite in data["suites"]:
        for mode in render_app_build.MODES:
            for candidate_stage in ("protobuf", "flatbuffers"):
                contrasts.append(_contrast(
                    data, totals, suite, mode, "ccache-text", mode, candidate_stage,
                    f"{candidate_stage}-minus-text ({mode})",
                ))

        for stage in ("ccache-text", "protobuf", "flatbuffers"):
            contrasts.append(_contrast(
                data, totals, suite, "direct", stage, "warm", stage,
                f"warm-minus-direct ({stage})",
            ))

        contrasts.append(_contrast(
            data, totals, suite, "direct", "ccache-text", "direct", "before-cache",
            "before-cache-minus-text (direct)",
        ))
        for mode in render_app_build.MODES:
            contrasts.append(_contrast(
                data, totals, suite, mode, "protobuf", mode, "flatbuffers",
                f"flatbuffers-minus-protobuf ({mode})",
            ))

    return {
        "analysis_kind": "descriptive paired-round App-build timing analysis",
        "repeat_count": render_app_build.REPEAT_COUNT,
        "sample_unit": "whole-suite repeat totals; App groups are paired workloads, not independent repeats",
        "inference": "No significance tests, p-values, or confidence intervals are computed.",
        "privacy_note": "Group IDs are included only in this local diagnostic output; do not publish this JSON.",
        "suite_mode_stage_totals": _total_summary(totals),
        "contrasts": contrasts,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True,
                        help="validated app-build-matrix.json from the measurement run")
    args = parser.parse_args(argv)
    payload = json.loads(args.input.read_text(encoding="utf-8"))
    print(json.dumps(analyze(payload), indent=2, allow_nan=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
