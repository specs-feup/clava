#!/usr/bin/env python3
"""Validate grouped parser A/B observations and render report-ready SVGs.

The unit of analysis is one scheduled CodeParser call group.  Rows are paired
by the stable suite/input_id identity; repeated rounds are kept as repeated
measurements of that group, never as additional groups.
"""
from __future__ import annotations

import base64
import csv
import html
import io
import json
import math
from pathlib import Path
import re
import statistics
from typing import Any, Iterable


SUITES = ("clava-js", "java")
PROTOCOLS = ("text", "protobuf")
CACHE_MODES = ("direct", "warm")
REPEATS = tuple(range(1, 7))
EXPECTED_GROUP_COUNTS = {"clava-js": 300, "java": 216}
GROUP_ID_PATTERNS = {
    "clava-js": re.compile(r"^(?:js|clava-js)-group-[0-9]{4}$"),
    "java": re.compile(r"^java-group-[0-9]{4}$"),
}

SUITE_LABELS = {"clava-js": "Clava-JS", "java": "Java"}
PROTOCOL_LABELS = {"text": "Text", "protobuf": "Proto"}
CACHE_LABELS = {"direct": "Direct", "warm": "Warm"}
SUITE_COLORS = {"clava-js": "#2368a2", "java": "#b45a2a"}
PROTOCOL_COLORS = {"text": "#4d647a", "protobuf": "#147d64"}


class AnalysisError(ValueError):
    """The schedules or observations do not form the complete valid matrix."""


def _number(value: Any, label: str, *, positive: bool = False) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise AnalysisError(f"{label} must be numeric")
    number = float(value)
    if not math.isfinite(number) or number < 0 or (positive and number == 0):
        qualifier = "finite and positive" if positive else "finite and non-negative"
        raise AnalysisError(f"{label} must be {qualifier}")
    return number


def _cell(row: dict[str, Any], source: str) -> tuple[str, str, str, int]:
    suite = row.get("suite")
    protocol = row.get("protocol")
    cache_mode = row.get("cache_mode")
    repeat = row.get("repeat")
    if suite not in SUITES or protocol not in PROTOCOLS or cache_mode not in CACHE_MODES:
        raise AnalysisError(f"{source}: unexpected suite/protocol/cache cell")
    if type(repeat) is not int or repeat not in REPEATS:
        raise AnalysisError(f"{source}: repeat must be an integer from 1 through 6")
    if row.get("phase") != "measure":
        raise AnalysisError(f"{source}: expected phase=measure")
    return suite, protocol, cache_mode, repeat


def _safe_basename(value: Any) -> str | None:
    if not isinstance(value, str) or not value.strip():
        return None
    basename = value.strip().replace("\\", "/").rsplit("/", 1)[-1]
    if basename in ("", ".", ".."):
        return None
    if basename[0] in "=+-@\t\r":
        basename = "'" + basename
    return basename


def _source_attribution(row: dict[str, Any]) -> dict[str, Any]:
    labels: list[str] = []
    for field in ("source_paths", "source_files"):
        values = row.get(field)
        if isinstance(values, list):
            for value in values:
                label = _safe_basename(value)
                if label is not None and label not in labels:
                    labels.append(label)
    source_label = row.get("source_label")
    if isinstance(source_label, str):
        for component in source_label.split(","):
            label = _safe_basename(component)
            if label is not None and label not in labels:
                labels.append(label)
    source_count = row.get("source_count")
    if type(source_count) is not int or source_count < 0:
        source_files = row.get("source_files")
        source_paths = row.get("source_paths")
        if type(source_files) is int and source_files >= 0:
            source_count = source_files
        elif isinstance(source_files, list):
            source_count = len(source_files)
        elif isinstance(source_paths, list):
            source_count = len(source_paths)
        else:
            source_count = len(labels) if labels else None
    hashes = {}
    for field in ("source_sha256", "args_sha256", "options_sha256"):
        value = row.get(field)
        if value is not None and (not isinstance(value, str)
                                  or not re.fullmatch(r"[0-9a-fA-F]{64}", value)):
            raise AnalysisError(f"schedule has an invalid {field}")
        hashes[field] = value.lower() if isinstance(value, str) else ""
    source_file_hashes: list[str] = []
    source_files = row.get("source_files")
    if isinstance(source_files, list):
        for record in source_files:
            if not isinstance(record, dict) or record.get("sha256") is None:
                continue
            digest = record["sha256"]
            if not isinstance(digest, str) or not re.fullmatch(r"[0-9a-fA-F]{64}", digest):
                raise AnalysisError("schedule has an invalid source file SHA-256")
            source_file_hashes.append(digest.lower())
    return {
        "source_labels": "; ".join(labels),
        "source_file_sha256s": ";".join(source_file_hashes),
        "source_count": source_count,
        **hashes,
    }


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    try:
        with path.open(encoding="utf-8") as source:
            for line_number, line in enumerate(source, start=1):
                if not line.strip():
                    continue
                try:
                    row = json.loads(line)
                except json.JSONDecodeError as error:
                    raise AnalysisError(
                        f"{path.name}:{line_number}: malformed JSON: {error.msg}"
                    ) from error
                if not isinstance(row, dict):
                    raise AnalysisError(f"{path.name}:{line_number}: row must be an object")
                rows.append(row)
    except OSError as error:
        raise AnalysisError(f"cannot read {path.name}: {error}") from error
    return rows


def load_grouped_analysis(
    run_root: Path,
    *,
    expected_group_counts: dict[str, int] | None = None,
) -> dict[str, Any]:
    """Load the 48 scheduled cells and outputs from the grouped runner."""
    run_root = Path(run_root)
    results_root = next(
        (run_root / name for name in ("observations", "measure")
         if (run_root / name).is_dir()),
        None,
    )
    if results_root is None:
        raise AnalysisError("run root needs an observations/ or measure/ directory")
    schedules: list[dict[str, Any]] = []
    observations: list[dict[str, Any]] = []
    for suite in SUITES:
        for protocol in PROTOCOLS:
            for cache_mode in CACHE_MODES:
                for repeat in REPEATS:
                    name = f"measure-{suite}-{protocol}-{cache_mode}-r{repeat:02d}.jsonl"
                    schedule_path = run_root / "schedules" / name
                    result_path = results_root / name
                    schedules.extend(_read_jsonl(schedule_path))
                    observations.extend(_read_jsonl(result_path))
    return analyze_grouped_runs(
        schedules, observations, expected_group_counts=expected_group_counts
    )


def analyze_grouped_runs(
    schedule_rows: Iterable[dict[str, Any]],
    observation_rows: Iterable[dict[str, Any]],
    *,
    expected_group_counts: dict[str, int] | None = None,
) -> dict[str, Any]:
    """Validate and summarize a six-round Text/Proto direct/warm group matrix.

    ``schedule_rows`` must contain all 48 schedule files flattened into one
    iterable.  Invalid, unselected, untimed, and non-measure observation rows
    are left out.  The remaining selected valid observations must still cover
    every planned suite/input_id/protocol/cache/repeat cell exactly once.
    """
    expected_group_counts = expected_group_counts or EXPECTED_GROUP_COUNTS
    if set(expected_group_counts) != set(SUITES) or any(
        type(count) is not int or count < 1 for count in expected_group_counts.values()
    ):
        raise AnalysisError("expected_group_counts must provide a positive count for both suites")

    schedule_cells: dict[tuple[str, str, str, int], dict[str, dict[str, Any]]] = {}
    expected_ids_by_suite: dict[str, set[str]] = {}
    stable_metadata: dict[tuple[str, str], tuple[Any, ...]] = {}
    attribution_by_group: dict[tuple[str, str], dict[str, Any]] = {}
    event_ids_by_suite: dict[str, set[str]] = {suite: set() for suite in SUITES}
    group_ids_by_suite: dict[str, set[str]] = {suite: set() for suite in SUITES}
    schedule_row_count = 0
    for index, row in enumerate(schedule_rows, start=1):
        if not isinstance(row, dict):
            raise AnalysisError(f"schedule row {index} must be an object")
        suite, protocol, cache_mode, repeat = _cell(row, f"schedule row {index}")
        if row.get("compression_policy") != "raw_control":
            raise AnalysisError(f"schedule row {index}: expected compression_policy=raw_control")
        input_id = row.get("input_id")
        if not isinstance(input_id, str) or not GROUP_ID_PATTERNS[suite].fullmatch(input_id):
            raise AnalysisError(f"schedule row {index}: invalid stable input_id")
        event_id, group_id = row.get("event_id"), row.get("group_id")
        if not isinstance(event_id, str) or not event_id.strip():
            raise AnalysisError(f"schedule row {index}: missing event_id")
        if not isinstance(group_id, str) or not group_id.strip():
            raise AnalysisError(f"schedule row {index}: missing group_id")

        cell_key = (suite, protocol, cache_mode, repeat)
        scheduled = schedule_cells.setdefault(cell_key, {})
        if input_id in scheduled:
            raise AnalysisError(f"duplicate scheduled input_id {input_id} in {cell_key}")
        scheduled[input_id] = row
        expected_ids_by_suite.setdefault(suite, set()).add(input_id)

        # These values identify the exact source/configuration event. Hashes
        # and safe basenames are exported; source paths stay internal.
        attribution = _source_attribution(row)
        identity = (event_id, group_id, attribution["source_labels"],
                    attribution["source_file_sha256s"],
                    attribution["source_count"], attribution["source_sha256"],
                    attribution["args_sha256"], attribution["options_sha256"])
        identity_key = (suite, input_id)
        previous = stable_metadata.get(identity_key)
        if previous is None:
            if event_id in event_ids_by_suite[suite] or group_id in group_ids_by_suite[suite]:
                raise AnalysisError(f"event/group identity is not unique for {suite}")
            event_ids_by_suite[suite].add(event_id)
            group_ids_by_suite[suite].add(group_id)
            stable_metadata[identity_key] = identity
            attribution_by_group[identity_key] = attribution
        elif previous != identity:
            raise AnalysisError(f"scheduled identity changed for {input_id}")
        schedule_row_count += 1

    expected_cells = {
        (suite, protocol, cache_mode, repeat)
        for suite in SUITES for protocol in PROTOCOLS
        for cache_mode in CACHE_MODES for repeat in REPEATS
    }
    actual_cells = set(schedule_cells)
    if actual_cells != expected_cells:
        raise AnalysisError(
            f"schedule matrix incomplete; missing={sorted(expected_cells - actual_cells)}, "
            f"unexpected={sorted(actual_cells - expected_cells)}"
        )
    for suite in SUITES:
        actual_ids = expected_ids_by_suite.get(suite, set())
        required_count = expected_group_counts[suite]
        if len(actual_ids) != required_count:
            raise AnalysisError(
                f"schedule has {len(actual_ids)} stable IDs for {suite}; expected {required_count}"
            )
        ordinals = sorted(int(input_id.rsplit("-", 1)[1]) for input_id in actual_ids)
        if ordinals != list(range(1, required_count + 1)):
            raise AnalysisError(f"schedule does not contain the complete one-based ID range for {suite}")
        for cell_key in sorted(key for key in expected_cells if key[0] == suite):
            cell_ids = set(schedule_cells[cell_key])
            if cell_ids != actual_ids:
                missing = sorted(actual_ids - cell_ids)
                unexpected = sorted(cell_ids - actual_ids)
                raise AnalysisError(
                    f"schedule ID set differs for {cell_key}; "
                    f"missing={missing[:4]}, unexpected={unexpected[:4]}"
                )

    expected_by_cell: dict[tuple[str, str, str, int], set[str]] = {
        cell: set(rows) for cell, rows in schedule_cells.items()
    }
    valid: dict[tuple[str, str, str, int, str], dict[str, Any]] = {}
    excluded = {"non_measure": 0, "unselected": 0, "invalid": 0, "untimed": 0}
    for index, row in enumerate(observation_rows, start=1):
        if not isinstance(row, dict):
            raise AnalysisError(f"observation row {index} must be an object")
        if row.get("phase") != "measure":
            excluded["non_measure"] += 1
            continue
        if row.get("selected") is False:
            excluded["unselected"] += 1
            continue
        if row.get("valid") is not True:
            excluded["invalid"] += 1
            continue
        if "elapsed_ms" not in row or row.get("elapsed_ms") is None:
            excluded["untimed"] += 1
            continue
        suite, protocol, cache_mode, repeat = _cell(row, f"observation row {index}")
        input_id = row.get("input_id")
        cell = (suite, protocol, cache_mode, repeat)
        if not isinstance(input_id, str) or input_id not in expected_by_cell.get(cell, set()):
            raise AnalysisError(f"observation row {index}: unexpected stable input_id")
        if input_id not in expected_ids_by_suite[suite]:
            raise AnalysisError(f"observation row {index}: input_id is not in the schedule")
        schedule = schedule_cells[cell][input_id]
        app_returned_null = row.get("app_null") is True or row.get("app_returned_null") is True
        parser_config = schedule.get("parser_config")
        syntax_only = isinstance(parser_config, dict) and parser_config.get("syntax_only") is True
        if app_returned_null and not syntax_only:
            excluded["invalid"] += 1
            continue
        if row.get("event_id") != schedule.get("event_id"):
            raise AnalysisError(f"observation row {index}: event_id differs from schedule")
        for field in ("source_sha256", "args_sha256", "options_sha256"):
            if field in schedule:
                scheduled_digest = schedule[field]
                observed_digest = row.get(field)
                if (not isinstance(scheduled_digest, str)
                        or not re.fullmatch(r"[0-9a-fA-F]{64}", scheduled_digest)
                        or not isinstance(observed_digest, str)
                        or observed_digest.lower() != scheduled_digest.lower()):
                    raise AnalysisError(f"observation row {index}: {field} differs from schedule")
        # StandaloneParseRunner exports event_id/input_id but not group_id.
        # The schedules independently prove group_id stability; event_id is
        # unique per group and is the join key for these result rows.
        if row.get("group_id") is not None and row.get("group_id") != schedule.get("group_id"):
            raise AnalysisError(f"observation row {index}: group_id differs from schedule")
        elapsed_ms = _number(row.get("elapsed_ms"), f"observation row {index}.elapsed_ms", positive=True)
        key = (suite, cache_mode, protocol, repeat, input_id)
        if key in valid:
            raise AnalysisError(f"duplicate selected valid observation for {key}")
        valid[key] = {
            "suite": suite,
            "input_id": input_id,
            "cache_mode": cache_mode,
            "protocol": protocol,
            "repeat": repeat,
            "elapsed_ms": elapsed_ms,
        }

    expected_observation_keys = {
        (suite, cache_mode, protocol, repeat, input_id)
        for suite, protocol, cache_mode, repeat in expected_cells
        for input_id in expected_by_cell[(suite, protocol, cache_mode, repeat)]
    }
    actual_observation_keys = set(valid)
    if actual_observation_keys != expected_observation_keys:
        missing = sorted(expected_observation_keys - actual_observation_keys)
        unexpected = sorted(actual_observation_keys - expected_observation_keys)
        raise AnalysisError(
            "selected valid timed observations do not cover the complete matrix; "
            f"missing={missing[:6]}, unexpected={unexpected[:6]}, excluded={excluded}"
        )

    group_ids = {suite: sorted(expected_ids_by_suite[suite]) for suite in SUITES}
    group_rounds: list[dict[str, Any]] = []
    group_summaries: list[dict[str, Any]] = []
    for suite in SUITES:
        for cache_mode in CACHE_MODES:
            for input_id in group_ids[suite]:
                text_samples = [
                    valid[(suite, cache_mode, "text", repeat, input_id)]["elapsed_ms"]
                    for repeat in REPEATS
                ]
                proto_samples = [
                    valid[(suite, cache_mode, "protobuf", repeat, input_id)]["elapsed_ms"]
                    for repeat in REPEATS
                ]
                paired_pct: list[float] = []
                for repeat, text_ms, proto_ms in zip(REPEATS, text_samples, proto_samples):
                    delta_ms = proto_ms - text_ms
                    relative_pct = 100 * (proto_ms / text_ms - 1)
                    paired_pct.append(relative_pct)
                    group_rounds.append({
                        "suite": suite,
                        "input_id": input_id,
                        **attribution_by_group[(suite, input_id)],
                        "cache_mode": cache_mode,
                        "repeat": repeat,
                        "text_ms": text_ms,
                        "protobuf_ms": proto_ms,
                        "delta_ms": delta_ms,
                        "relative_pct": relative_pct,
                    })
                text_median = statistics.median(text_samples)
                proto_median = statistics.median(proto_samples)
                group_summaries.append({
                    "suite": suite,
                    "input_id": input_id,
                    **attribution_by_group[(suite, input_id)],
                    "cache_mode": cache_mode,
                    "n_rounds": len(REPEATS),
                    "text_median_ms": text_median,
                    "protobuf_median_ms": proto_median,
                    "proto_text_ratio": proto_median / text_median,
                    "median_paired_relative_pct": statistics.median(paired_pct),
                })

    round_totals: list[dict[str, Any]] = []
    for cache_mode in CACHE_MODES:
        for repeat in REPEATS:
            suite_totals: dict[str, dict[str, float]] = {}
            for suite in SUITES:
                suite_totals[suite] = {}
                for protocol in PROTOCOLS:
                    suite_totals[suite][protocol] = sum(
                        valid[(suite, cache_mode, protocol, repeat, input_id)]["elapsed_ms"]
                        for input_id in group_ids[suite]
                    )
                    round_totals.append({
                        "scope": suite,
                        "suite": suite,
                        "cache_mode": cache_mode,
                        "repeat": repeat,
                        "protocol": protocol,
                        "group_count": len(group_ids[suite]),
                        "sum_elapsed_ms": suite_totals[suite][protocol],
                    })
            for protocol in PROTOCOLS:
                round_totals.append({
                    "scope": "global",
                    "suite": "global",
                    "cache_mode": cache_mode,
                    "repeat": repeat,
                    "protocol": protocol,
                    "group_count": sum(len(group_ids[suite]) for suite in SUITES),
                    "sum_elapsed_ms": sum(suite_totals[suite][protocol] for suite in SUITES),
                })

    totals_by_key = {
        (row["scope"], row["cache_mode"], row["repeat"], row["protocol"]): row
        for row in round_totals
    }
    paired_round_totals: list[dict[str, Any]] = []
    for scope in (*SUITES, "global"):
        for cache_mode in CACHE_MODES:
            for repeat in REPEATS:
                text_sum = totals_by_key[(scope, cache_mode, repeat, "text")]["sum_elapsed_ms"]
                proto_sum = totals_by_key[(scope, cache_mode, repeat, "protobuf")]["sum_elapsed_ms"]
                paired_round_totals.append({
                    "scope": scope,
                    "cache_mode": cache_mode,
                    "repeat": repeat,
                    "group_count": (sum(len(group_ids[suite]) for suite in SUITES)
                                    if scope == "global" else len(group_ids[scope])),
                    "text_sum_ms": text_sum,
                    "protobuf_sum_ms": proto_sum,
                    "delta_ms": proto_sum - text_sum,
                    "relative_pct": 100 * (proto_sum / text_sum - 1),
                })

    return {
        "schema_version": 1,
        "suites": list(SUITES),
        "protocols": list(PROTOCOLS),
        "cache_modes": list(CACHE_MODES),
        "repeats": list(REPEATS),
        "group_counts": {suite: len(group_ids[suite]) for suite in SUITES},
        "excluded_rows": excluded,
        "schedule_row_count": schedule_row_count,
        "observation_row_count": len(valid),
        "group_rounds": group_rounds,
        "group_summaries": group_summaries,
        "round_totals": round_totals,
        "paired_round_totals": paired_round_totals,
    }


def _quantile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lo, hi = math.floor(position), math.ceil(position)
    if lo == hi:
        return ordered[lo]
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (position - lo)


def _summary(values: list[float]) -> dict[str, float]:
    return {
        "min": min(values),
        "q1": _quantile(values, 0.25),
        "median": statistics.median(values),
        "q3": _quantile(values, 0.75),
        "max": max(values),
    }


def _fmt_ms(value: float) -> str:
    if value >= 1000:
        return f"{value / 1000:.2f} s"
    if value >= 10:
        return f"{value:.0f} ms"
    if value >= 1:
        return f"{value:.2f} ms"
    return f"{value:.3f} ms"


def _fmt_main_axis_ms(value: float) -> str:
    if value >= 100:
        label = f"{value:,.0f}"
    elif value >= 10:
        label = f"{value:.1f}".rstrip("0").rstrip(".")
    elif value >= 1:
        label = f"{value:.2f}".rstrip("0").rstrip(".")
    else:
        label = f"{value:.2g}"
        if label == "0":
            label = f"{value:.12f}".rstrip("0").rstrip(".")
    return f"{label}ms"


def _ratio_axis_label(value: float) -> str:
    return f"{value:g}×"


def _svg_candle(summary: dict[str, Any], suite: str) -> str:
    rows = [row for row in summary["group_summaries"] if row["suite"] == suite]
    values = {
        (cache_mode, protocol): [
            row[f"{protocol}_median_ms"] for row in rows if row["cache_mode"] == cache_mode
        ]
        for cache_mode in CACHE_MODES for protocol in PROTOCOLS
    }
    all_values = [value for sample in values.values() for value in sample]
    log_low, log_high = math.log10(min(all_values)), math.log10(max(all_values))
    if log_high - log_low < 0.5:
        middle = (log_high + log_low) / 2
        log_low, log_high = middle - 0.25, middle + 0.25
    padding = (log_high - log_low) * 0.04
    log_low -= padding
    log_high += padding
    left, right = 94.0, 318.0
    x = lambda value: left + (math.log10(value) - log_low) * (right - left) / (log_high - log_low)
    positions = (("direct", "text", 70), ("direct", "protobuf", 99),
                 ("warm", "text", 171), ("warm", "protobuf", 200))
    output = [
        '<svg class="ga-main-svg" viewBox="0 0 360 270" role="img" '
        f'aria-label="{html.escape(SUITE_LABELS[suite])} per-group runtime spread, direct and warm, '
        'shared logarithmic millisecond axis">',
        '<desc>Each candle summarizes per-group medians across six matched rounds. '
        'Boxes show the middle half; whiskers show the full group range. Lower is faster.</desc>',
    ]
    ticks = [10.0 ** (log_low + (log_high - log_low) * fraction / 2) for fraction in range(3)]
    for value in ticks:
        px = x(value)
        label = _fmt_main_axis_ms(value)
        output.extend([
            f'<line class="ga-grid-line" x1="{px:.2f}" x2="{px:.2f}" y1="31" y2="225"/>',
            f'<text class="ga-main-tick" x="{px:.2f}" y="23" text-anchor="middle">{html.escape(label)}</text>',
        ])
    output.extend([
        '<text class="ga-group-title" x="3" y="49">Direct</text>',
        '<line class="ga-divider" x1="2" x2="355" y1="133" y2="133"/>',
        '<text class="ga-group-title" x="3" y="151">Warm</text>',
    ])
    for cache_mode, protocol, y in positions:
        sample = values[(cache_mode, protocol)]
        stats = _summary(sample)
        color = PROTOCOL_COLORS[protocol]
        label = PROTOCOL_LABELS[protocol]
        output.extend([
            f'<text class="ga-main-label" x="5" y="{y + 5}">{label}</text>',
            f'<line class="ga-whisker" stroke="{color}" x1="{x(stats["min"]):.2f}" x2="{x(stats["max"]):.2f}" y1="{y}" y2="{y}"/>',
            f'<line class="ga-cap" stroke="{color}" x1="{x(stats["min"]):.2f}" x2="{x(stats["min"]):.2f}" y1="{y - 7}" y2="{y + 7}"/>',
            f'<line class="ga-cap" stroke="{color}" x1="{x(stats["max"]):.2f}" x2="{x(stats["max"]):.2f}" y1="{y - 7}" y2="{y + 7}"/>',
            f'<rect class="ga-box" stroke="{color}" x="{x(stats["q1"]):.2f}" y="{y - 10}" width="{max(1.5, x(stats["q3"]) - x(stats["q1"])):.2f}" height="20"/>',
            f'<line class="ga-median" stroke="{color}" x1="{x(stats["median"]):.2f}" x2="{x(stats["median"]):.2f}" y1="{y - 11}" y2="{y + 11}"/>',
            f'<title>{CACHE_LABELS[cache_mode]} {label}: median {html.escape(_fmt_ms(stats["median"]))}; {len(sample)} groups</title>',
        ])
    output.append('<text class="ga-main-axis-title" x="212" y="254" text-anchor="middle">Runtime (ms; log)</text>')
    output.append('</svg>')
    return "".join(output)


def _svg_distribution(
    summary: dict[str, Any], *, field: str, title: str, axis_label: str,
    formatter, zero_line: bool = False, logarithmic: bool = False,
) -> str:
    categories: list[tuple[str, list[float]]] = []
    for suite in SUITES:
        for cache_mode in CACHE_MODES:
            rows = [row for row in summary["group_summaries"]
                    if row["suite"] == suite and row["cache_mode"] == cache_mode]
            categories.append((f"{SUITE_LABELS[suite]} {CACHE_LABELS[cache_mode].lower()}",
                               [float(row[field]) for row in rows]))
    all_values = [value for _, values in categories for value in values]
    if logarithmic:
        if any(value <= 0 for value in all_values):
            raise AnalysisError(f"{field} contains non-positive values on a logarithmic chart")
        domain_min, domain_max = math.log10(min(all_values)), math.log10(max(all_values))
    else:
        domain_min, domain_max = min(all_values), max(all_values)
    span = domain_max - domain_min
    if span == 0:
        span = max(abs(domain_min) * 0.2, 1.0)
        domain_min -= span / 2
        domain_max += span / 2
    else:
        domain_min -= span * 0.04
        domain_max += span * 0.04
    left, right = 147.0, 425.0

    def project(value: float) -> float:
        actual = math.log10(value) if logarithmic else value
        return left + (actual - domain_min) * (right - left) / (domain_max - domain_min)

    output = [
        '<svg class="ga-detail-svg" viewBox="0 0 440 244" role="img" '
        f'aria-label="{html.escape(title)}">',
        '<desc>Each row is a distribution across stable groups, with full-range whiskers and an interquartile box.</desc>',
    ]
    baseline = 218
    tick_values: list[float]
    if logarithmic:
        min_exp, max_exp = math.ceil(domain_min), math.floor(domain_max)
        exponents = list(range(min_exp, max_exp + 1))
        if not exponents:
            tick_values = [10.0 ** ((domain_min + domain_max) / 2)]
        elif len(exponents) > 5:
            tick_values = [10.0 ** (domain_min + (domain_max - domain_min) * part / 4)
                           for part in range(5)]
        else:
            tick_values = [10.0 ** exponent for exponent in exponents]
    else:
        tick_values = [domain_min + (domain_max - domain_min) * part / 4 for part in range(5)]
    if zero_line and domain_min <= 0 <= domain_max:
        px = project(0)
        output.append(f'<line class="ga-zero" x1="{px:.2f}" x2="{px:.2f}" y1="25" y2="{baseline}"/>')
    for value in tick_values:
        px = project(value)
        label = formatter(value)
        output.extend([
            f'<line class="ga-grid-line" x1="{px:.2f}" x2="{px:.2f}" y1="27" y2="{baseline}"/>',
            f'<text class="ga-tick" x="{px:.2f}" y="232" text-anchor="middle">{html.escape(label)}</text>',
        ])
    for index, (label, values) in enumerate(categories):
        y = 42 + index * 43
        stats = _summary(values)
        color = SUITE_COLORS["clava-js" if label.startswith("Clava-JS") else "java"]
        output.extend([
            f'<text class="ga-label" x="3" y="{y + 4}">{html.escape(label)} · n={len(values)}</text>',
            f'<line class="ga-whisker" stroke="{color}" x1="{project(stats["min"]):.2f}" x2="{project(stats["max"]):.2f}" y1="{y}" y2="{y}"/>',
            f'<rect class="ga-box" stroke="{color}" x="{project(stats["q1"]):.2f}" y="{y - 9}" width="{max(1.5, project(stats["q3"]) - project(stats["q1"])):.2f}" height="18"/>',
            f'<line class="ga-median" stroke="{color}" x1="{project(stats["median"]):.2f}" x2="{project(stats["median"]):.2f}" y1="{y - 11}" y2="{y + 11}"/>',
            f'<text class="ga-value" x="438" y="{y + 4}" text-anchor="end">{html.escape(formatter(stats["median"]))}</text>',
        ])
    output.append(f'<text class="ga-axis-title" x="286" y="242" text-anchor="middle">{html.escape(axis_label)}</text>')
    output.append('</svg>')
    return "".join(output)


def _svg_round_totals(summary: dict[str, Any], cache_mode: str, protocol: str) -> str:
    totals = {
        (row["suite"], row["repeat"]): row["sum_elapsed_ms"]
        for row in summary["round_totals"]
        if row["scope"] != "global" and row["cache_mode"] == cache_mode
        and row["protocol"] == protocol
    }
    maximum = max(sum(totals[(suite, repeat)] for suite in SUITES) for repeat in REPEATS)
    domain = max(1.0, maximum * 1.05)
    left, right = 58.0, 420.0
    x = lambda value: left + value * (right - left) / domain
    output = [
        '<svg class="ga-detail-svg" viewBox="0 0 440 222" role="img" '
        f'aria-label="{html.escape(CACHE_LABELS[cache_mode])} {html.escape(PROTOCOL_LABELS[protocol])} '
        'per-round summed parse time by suite">',
        '<desc>Six rounds. Each horizontal bar sums per-group parser-call times and is stacked by suite.</desc>',
    ]
    for tick in range(5):
        value = domain * tick / 4
        px = x(value)
        output.extend([
            f'<line class="ga-grid" x1="{px:.2f}" x2="{px:.2f}" y1="22" y2="193"/>',
            f'<text class="ga-tick" x="{px:.2f}" y="207" text-anchor="middle">{html.escape(_fmt_ms(value))}</text>',
        ])
    for index, repeat in enumerate(REPEATS):
        y = 37 + index * 27
        offset = 0.0
        output.append(f'<text class="ga-label" x="2" y="{y + 4}">Round {repeat}</text>')
        for suite in SUITES:
            value = totals[(suite, repeat)]
            output.append(
                f'<rect x="{x(offset):.2f}" y="{y - 8}" width="{max(0.6, x(offset + value) - x(offset)):.2f}" height="16" '
                f'fill="{SUITE_COLORS[suite]}"><title>{SUITE_LABELS[suite]}: {html.escape(_fmt_ms(value))}</title></rect>'
            )
            offset += value
        output.append(f'<text class="ga-value" x="438" y="{y + 4}" text-anchor="end">{html.escape(_fmt_ms(offset))}</text>')
    output.extend([
        f'<rect x="83" y="216" width="9" height="9" fill="{SUITE_COLORS["clava-js"]}"/>',
        '<text class="ga-legend" x="96" y="224">Clava-JS</text>',
        f'<rect x="174" y="216" width="9" height="9" fill="{SUITE_COLORS["java"]}"/>',
        '<text class="ga-legend" x="187" y="224">Java</text>',
    ])
    output.append('</svg>')
    return "".join(output)


def _csv_link(
    filename: str, columns: list[str], rows: Iterable[dict[str, Any]], *, label: str | None = None
) -> str:
    csv_text = _csv_text(columns, rows)
    encoded = base64.b64encode(csv_text.encode("utf-8")).decode("ascii")
    return (
        f'<a download="{html.escape(filename, quote=True)}" '
        f'href="data:text/csv;charset=utf-8;base64,{encoded}">{html.escape(label or filename)}</a>'
    )


def _csv_text(columns: list[str], rows: Iterable[dict[str, Any]]) -> str:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=columns, extrasaction="ignore", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return stream.getvalue()


def _csv_exports(summary: dict[str, Any]) -> dict[str, tuple[list[str], Iterable[dict[str, Any]]]]:
    return {
        "grouped-ab-round-totals.csv": (
            ["scope", "suite", "cache_mode", "repeat", "protocol", "group_count", "sum_elapsed_ms"],
            summary["round_totals"],
        ),
        "grouped-ab-paired-round-totals.csv": (
            ["scope", "cache_mode", "repeat", "group_count", "text_sum_ms",
             "protobuf_sum_ms", "delta_ms", "relative_pct"],
            summary["paired_round_totals"],
        ),
        "grouped-ab-group-medians-and-ratios.csv": (
            ["suite", "input_id", "source_labels", "source_count", "source_sha256",
             "source_file_sha256s", "args_sha256", "options_sha256", "cache_mode", "n_rounds",
             "text_median_ms", "protobuf_median_ms", "proto_text_ratio", "median_paired_relative_pct"],
            summary["group_summaries"],
        ),
        "grouped-ab-paired-rounds.csv": (
            # Keep each timing row directly identifiable by filename. Stable
            # group keys join it to the medians CSV, which carries provenance
            # hashes once per group/cache instead of repeating them per round.
            ["suite", "input_id", "source_labels", "cache_mode", "repeat",
             "text_ms", "protobuf_ms", "delta_ms", "relative_pct"],
            summary["group_rounds"],
        ),
    }


def write_grouped_analysis_exports(
    summary: dict[str, Any], output_directory: Path, *,
    expected_group_counts: dict[str, int] | None = None,
    compact: bool = False,
) -> dict[str, Path]:
    """Write the sanitized JSON, HTML fragment, and four linked CSV datasets."""
    if summary.get("schema_version") != 1:
        raise AnalysisError("unsupported grouped analysis schema")
    output_directory = Path(output_directory)
    output_directory.mkdir(parents=True, exist_ok=True)
    summary_text = json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if any(marker in summary_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("summary includes an absolute/local path")
    paths = {
        "summary": output_directory / "grouped-ab-analysis.json",
        "html": output_directory / "grouped-ab-review-visuals.html",
    }
    paths["summary"].write_text(summary_text, encoding="utf-8")
    html_text = render_reviewed_visuals_html(
        summary, expected_group_counts=expected_group_counts, compact=compact
    )
    if any(marker in html_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("HTML fragment includes an absolute/local path")
    paths["html"].write_text(html_text, encoding="utf-8")
    for filename, (columns, rows) in _csv_exports(summary).items():
        csv_text = _csv_text(columns, rows)
        if any(marker in csv_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
            raise AnalysisError(f"{filename} includes an absolute/local path")
        path = output_directory / filename
        path.write_text(csv_text, encoding="utf-8", newline="")
        paths[filename] = path
    return paths


def render_reviewed_visuals_html(
    summary: dict[str, Any], *,
    expected_group_counts: dict[str, int] | None = None,
    compact: bool = False,
) -> str:
    """Render a self-contained, responsive HTML fragment for the decision report."""
    expected_group_counts = expected_group_counts or EXPECTED_GROUP_COUNTS
    if summary.get("schema_version") != 1:
        raise AnalysisError("unsupported grouped analysis schema")
    counts = summary["group_counts"]
    if counts != expected_group_counts:
        raise AnalysisError(f"reviewed grouped visuals require all expected IDs: {counts}")
    runtime_charts = "".join(
        '<figure class="ga-figure"><figcaption>'
        f'{html.escape(SUITE_LABELS[suite])} · {counts[suite]} groups</figcaption>'
        f'{_svg_candle(summary, suite)}</figure>'
        for suite in SUITES
    )
    total_charts = "".join(
        '<figure class="ga-figure"><figcaption>'
        f'{html.escape(CACHE_LABELS[cache_mode])} · {html.escape(PROTOCOL_LABELS[protocol])}'
        f'</figcaption>{_svg_round_totals(summary, cache_mode, protocol)}</figure>'
        for cache_mode in CACHE_MODES for protocol in PROTOCOLS
    )
    ratio_chart = _svg_distribution(
        summary, field="proto_text_ratio", title="Per-group median Proto/Text runtime ratio",
        axis_label="Proto / Text ratio · log scale · 1× means equal",
        formatter=_ratio_axis_label, logarithmic=True,
    )
    delta_chart = _svg_distribution(
        summary, field="median_paired_relative_pct", title="Per-group paired percent delta",
        axis_label="Median paired Proto minus Text · percent",
        formatter=lambda value: f"{value:+.1f}%", zero_line=True,
    )

    ratio_rows = []
    for suite in SUITES:
        for cache_mode in CACHE_MODES:
            values = [row["proto_text_ratio"] for row in summary["group_summaries"]
                      if row["suite"] == suite and row["cache_mode"] == cache_mode]
            stats = _summary(values)
            ratio_rows.append(
                f'<tr><th scope="row">{html.escape(SUITE_LABELS[suite])} · '
                f'{html.escape(CACHE_LABELS[cache_mode])}</th>'
                f'<td data-label="Groups">{len(values)}</td>'
                f'<td data-label="Median ratio">{stats["median"]:.3f}×</td>'
                f'<td data-label="Middle half">{stats["q1"]:.3f}×–{stats["q3"]:.3f}×</td>'
                f'<td data-label="Full range">{stats["min"]:.3f}×–{stats["max"]:.3f}×</td></tr>'
            )

    full_total_rows = []
    for row in summary["paired_round_totals"]:
        scope = "Global (all groups)" if row["scope"] == "global" else SUITE_LABELS[row["scope"]]
        full_total_rows.append(
            f'<tr><th scope="row">{html.escape(scope)}</th>'
            f'<td data-label="Cache">{html.escape(CACHE_LABELS[row["cache_mode"]])}</td>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Text sum">{html.escape(_fmt_ms(row["text_sum_ms"]))}</td>'
            f'<td data-label="Proto sum">{html.escape(_fmt_ms(row["protobuf_sum_ms"]))}</td>'
            f'<td data-label="Proto − Text">{html.escape(_fmt_ms(row["delta_ms"]))}</td>'
            f'<td data-label="Paired change">{row["relative_pct"]:+.2f}%</td></tr>'
        )
    median_total_rows = []
    for scope in (*SUITES, "global"):
        for cache_mode in CACHE_MODES:
            rows = [row for row in summary["paired_round_totals"]
                    if row["scope"] == scope and row["cache_mode"] == cache_mode]
            text_s = statistics.median(row["text_sum_ms"] for row in rows) / 1000
            proto_s = statistics.median(row["protobuf_sum_ms"] for row in rows) / 1000
            delta_pct = statistics.median(row["relative_pct"] for row in rows)
            label = {"clava-js": "JS", "java": "Java", "global": "All"}[scope]
            median_total_rows.append(
                f'<tr><th scope="row">{html.escape(label)} · {html.escape(CACHE_LABELS[cache_mode])}</th>'
                f'<td data-label="Text median">{text_s:.3f}s</td>'
                f'<td data-label="Proto median">{proto_s:.3f}s</td>'
                f'<td data-label="Paired change">{delta_pct:+.2f}%</td></tr>'
            )

    exports = _csv_exports(summary)
    per_round_link = _csv_link(
        "grouped-ab-round-totals.csv", *exports["grouped-ab-round-totals.csv"], label="round totals"
    )
    paired_totals_link = _csv_link(
        "grouped-ab-paired-round-totals.csv", *exports["grouped-ab-paired-round-totals.csv"],
        label="paired totals",
    )
    group_link = _csv_link(
        "grouped-ab-group-medians-and-ratios.csv", *exports["grouped-ab-group-medians-and-ratios.csv"],
        label="group medians + ratios",
    )
    paired_link = _csv_link(
        "grouped-ab-paired-rounds.csv", *exports["grouped-ab-paired-rounds.csv"], label="paired rounds"
    )
    fragment = f'''<section class="grouped-ab" aria-labelledby="grouped-ab-title">
<style>
section.grouped-ab{{max-width:1040px;margin:34px auto;color:inherit}}
section.grouped-ab *{{box-sizing:border-box}}
section.grouped-ab h2{{font-size:clamp(21px,3vw,27px);line-height:1.2;margin:26px 0 10px}}
section.grouped-ab h3{{font-size:18px;line-height:1.3;margin:22px 0 8px}}
section.grouped-ab p,section.grouped-ab li{{line-height:1.5}}
section.grouped-ab .ga-note{{color:var(--muted,#536174);font-size:14px}}
section.grouped-ab .ga-grid-layout{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}}
section.grouped-ab .ga-figure{{margin:0;min-width:0;border:1px solid var(--line,#d5dfe8);border-radius:9px;padding:10px;background:var(--panel,#f4f7fa)}}
section.grouped-ab figcaption{{font-weight:650;font-size:14px;margin:0 0 3px}}
section.grouped-ab .ga-main-svg{{display:block;width:100%;height:auto;max-width:520px;margin:0 auto}}
section.grouped-ab .ga-detail-svg{{display:block;width:100%;height:auto;max-width:720px;margin:0 auto}}
section.grouped-ab svg text{{fill:currentColor;font-family:system-ui,sans-serif}}
section.grouped-ab .ga-grid-line,section.grouped-ab .ga-grid{{stroke:var(--line,#d5dfe8);stroke-dasharray:3 4}}
section.grouped-ab .ga-zero{{stroke:#64748b;stroke-dasharray:4 3;stroke-width:1}}
section.grouped-ab .ga-divider{{stroke:var(--line,#d5dfe8);stroke-width:1.2}}
section.grouped-ab .ga-whisker,section.grouped-ab .ga-cap{{stroke-width:1.4}}
section.grouped-ab .ga-box{{fill:rgba(35,104,162,.16);stroke-width:1.4}}
section.grouped-ab .ga-median{{stroke-width:2.5}}
section.grouped-ab .ga-main-label,section.grouped-ab .ga-main-tick,section.grouped-ab .ga-group-title,section.grouped-ab .ga-main-axis-title{{font-size:15px}}
section.grouped-ab .ga-label{{font-size:11px}}
section.grouped-ab .ga-value{{font-size:10px;fill:var(--muted,#455568)}}
section.grouped-ab .ga-tick,section.grouped-ab .ga-legend,section.grouped-ab .ga-axis-title{{font-size:10px;fill:var(--muted,#536174)}}
section.grouped-ab .ga-links{{display:flex;flex-wrap:wrap;gap:8px 18px;padding-left:20px}}
section.grouped-ab .ga-table-wrap{{width:100%;min-width:0}}
section.grouped-ab table{{border-collapse:collapse;width:100%;table-layout:fixed;font-size:13px}}
section.grouped-ab th,section.grouped-ab td{{text-align:left;padding:7px 9px;border-bottom:1px solid var(--line,#d5dfe8);overflow-wrap:anywhere;word-break:normal}}
section.grouped-ab th[scope="col"]{{font-size:12px;color:var(--muted,#536174)}}
section.grouped-ab a{{overflow-wrap:anywhere}}
@media(max-width:700px){{section.grouped-ab{{margin:28px 0}}section.grouped-ab .ga-grid-layout{{grid-template-columns:1fr;gap:10px}}section.grouped-ab .ga-figure{{padding:8px 6px}}section.grouped-ab .ga-main-svg{{max-width:360px}}}}
@media(max-width:560px){{section.grouped-ab .ga-table-wrap table,section.grouped-ab .ga-table-wrap tbody,section.grouped-ab .ga-table-wrap tr,section.grouped-ab .ga-table-wrap th,section.grouped-ab .ga-table-wrap td{{display:block;width:100%}}section.grouped-ab .ga-table-wrap thead{{display:none}}section.grouped-ab .ga-table-wrap tr{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));border-bottom:1px solid var(--line,#d5dfe8);padding:6px 0}}section.grouped-ab .ga-table-wrap th[scope="row"]{{grid-column:1/-1;border:0;font-weight:700}}section.grouped-ab .ga-table-wrap td{{min-width:0;border:0;padding:5px 8px}}section.grouped-ab .ga-table-wrap td::before{{content:attr(data-label);display:block;color:var(--muted,#536174);font-size:12px;font-weight:650;margin-bottom:2px}}}}
</style>
<h2 id="grouped-ab-title">Raw same-build grouped replay</h2>
<p>Both formats use fast syntax-only validation here. This removes Text's wasted AST dumping; the replay cannot predict whole-suite rankings. Two patches enforce legacy AST equality. Raw dumps, captured parser groups and cross-file linking. JS includes 130 syntax-only groups. Candles show group-call times, not isolated-file times.</p>
<h3>Per-group runtime spread</h3>
<div class="ga-grid-layout">{runtime_charts}</div>
<h3>Median parse totals</h3>
<p class="ga-note">Six-round sums; delta is median round change. Global covers 516 groups.</p>
<div class="ga-table-wrap"><table class="ga-median-table"><thead><tr><th scope="col">Scope/cache</th><th scope="col">Text (s)</th><th scope="col">Proto (s)</th><th scope="col">Delta (%)</th></tr></thead><tbody>{''.join(median_total_rows)}</tbody></table></div>
<p>CSV: <span class="ga-links">{per_round_link}{paired_totals_link}{group_link}{paired_link}</span></p>
<details><summary>Detailed ratios, paired deltas, and per-round totals</summary>
<p class="ga-note">The format-only Proto control includes compatibility patches for ReferenceType <code>pointeeTypeAsWritten</code> and the optional empty-string <code>CXXPseudoDestructorExpr</code> qualifier. Fidelity checks require complete AST field and node/reference-graph identity. This is not the unmodified Proto branch.</p>
<p class="ga-note">Join paired-round timings to group medians/provenance by <code>suite</code> + <code>input_id</code>. The medians CSV carries source counts and SHA-256 digests without repeating them on every round.</p>
<h3>Per-group Proto/Text ratio distribution</h3><p class="ga-note">Ratios use per-group medians across six rounds. Values below 1× are lower for Proto.</p>
<div class="ga-figure">{ratio_chart}</div>
<div class="ga-table-wrap"><table><thead><tr><th scope="col">Suite · cache</th><th scope="col">Groups</th><th scope="col">Median ratio</th><th scope="col">Middle half</th><th scope="col">Full range</th></tr></thead><tbody>{''.join(ratio_rows)}</tbody></table></div>
<h3>Per-group paired percent-delta distribution</h3><p class="ga-note">For each group, this is the median of its six same-round changes: 100 × (Proto / Text − 1). Positive means Proto took longer.</p>
<div class="ga-figure">{delta_chart}</div>
<h3>Per-round summed parse time by suite</h3><p class="ga-note">Bars show six round sums, stacked by suite, for each cache/protocol condition. The global sum covers 516 groups, not command wall time.</p>
<div class="ga-grid-layout">{total_charts}</div>
<div class="ga-table-wrap"><table><thead><tr><th scope="col">Scope</th><th scope="col">Cache</th><th scope="col">Round</th><th scope="col">Text sum</th><th scope="col">Proto sum</th><th scope="col">Proto − Text</th><th scope="col">Paired change</th></tr></thead><tbody>{''.join(full_total_rows)}</tbody></table></div>
</details>
</section>'''
    if compact:
        marker = '<details><summary>Detailed ratios, paired deltas, and per-round totals</summary>'
        fragment = fragment.partition(marker)[0] + '''<p class="ga-note">Compatibility patches preserve the legacy reference-type field and absent pseudo-destructor qualifier. Full graph and generated-code fidelity passed. This is not the unmodified Protobuf branch. All per-group and per-round values remain in the CSVs.</p></section>'''
    if len(fragment.encode("utf-8")) > 4 * 1024 * 1024:
        raise AnalysisError("grouped visual fragment exceeds the 4 MiB report limit")
    return fragment


if __name__ == "__main__":
    raise SystemExit("Import this helper from the report pipeline; it does not run benchmarks.")
