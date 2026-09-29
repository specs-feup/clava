#!/usr/bin/env python3
"""Summarize individual parser-call timings from the per-parse benchmark CSV."""

from __future__ import annotations

import argparse
import csv
import html
import json
import math
from pathlib import Path
import re
import statistics
import sys
from typing import Any


SUITES = {
    "clava-js": "Clava-JS",
    "java": "Java parser",
}
PROTOCOLS = {
    "text": ("Text + ccache", "#059669"),
    "text+ccache": ("Text + ccache", "#059669"),
    "ccache-text": ("Text + ccache", "#059669"),
    "protobuf": ("Protobuf", "#7c3aed"),
    "proto": ("Protobuf", "#7c3aed"),
}
PROTOCOL_ORDER = ("text", "protobuf")
REQUIRED_COLUMNS = {
    "run_id", "pair_id", "suite", "cache_mode", "gc_policy", "protocol", "repeat",
    "source_identity", "parse_pair_key", "pair_available", "parse_elapsed_ms", "parse_timing_boundary",
    "run_valid", "event_valid", "ccache_disabled",
}


def esc(value: Any) -> str:
    return html.escape(str(value), quote=True)


def safe_path_label(value: Any) -> str:
    """Return a compact path label without disclosing a machine-local root."""
    text = str(value or "").strip()
    if not text:
        return ""
    candidate = Path(text)
    if candidate.is_absolute():
        return candidate.name
    text = re.sub(r"__clava_woven_[^/\\]+", "__clava_woven_<id>", text)
    return re.sub(r"(^|[/\\])tmp_[^/\\]+", r"\1tmp_<temp>", text)


def display_identity(row: dict[str, Any]) -> str:
    parts = []
    for value in (row.get("test_id"), safe_path_label(row.get("resource_key")),
                  row.get("parse_pass"), safe_path_label(row.get("source_path")),
                  row.get("parse_id")):
        text = str(value or "").strip()
        if text and text not in parts:
            parts.append(text)
    return " · ".join(parts) or "parse event"


def number(value: Any) -> float | None:
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if math.isfinite(parsed) else None


def flag(value: Any) -> bool | None:
    if value is None or str(value).strip() == "":
        return None
    normalized = str(value).strip().lower()
    if normalized in {"true", "1", "yes", "y"}:
        return True
    if normalized in {"false", "0", "no", "n"}:
        return False
    return None


def count(value: Any) -> int | None:
    parsed = number(value)
    if parsed is None or parsed < 0 or not parsed.is_integer():
        return None
    return int(parsed)


def canonical_suite(value: Any) -> str | None:
    normalized = str(value or "").strip().lower().replace("_", "-")
    if normalized in {"clava-js", "clavajs", "clava"}:
        return "clava-js"
    if normalized in {"java", "java-parser"}:
        return "java"
    return None


def canonical_protocol(value: Any) -> str | None:
    normalized = str(value or "").strip().lower().replace("_", "-").replace(" ", "")
    if normalized in PROTOCOLS:
        return "text" if normalized in {"text", "text+ccache", "ccache-text"} else "protobuf"
    return None


def canonical_policy(value: Any) -> str:
    return str(value or "unspecified").strip() or "unspecified"


def quantile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    if not ordered:
        raise ValueError("Cannot compute a quantile for an empty sample")
    if len(ordered) == 1:
        return ordered[0]
    position = (len(ordered) - 1) * fraction
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def distribution(values: list[float]) -> dict[str, Any]:
    if not values:
        return {"n": 0, "outliers": []}
    ordered = sorted(values)
    q1 = quantile(ordered, .25)
    median = statistics.median(ordered)
    q3 = quantile(ordered, .75)
    spread = q3 - q1
    low_fence = q1 - 1.5 * spread
    high_fence = q3 + 1.5 * spread
    inliers = [value for value in ordered if low_fence <= value <= high_fence]
    return {
        "n": len(ordered),
        "minimum": ordered[0],
        "q1": q1,
        "median": median,
        "q3": q3,
        "maximum": ordered[-1],
        "whisker_minimum": min(inliers),
        "whisker_maximum": max(inliers),
        "outliers": [value for value in ordered if value < low_fence or value > high_fence],
    }


def display_sample(rows: list[dict[str, Any]], value_field: str,
                   limit: int) -> list[dict[str, Any]]:
    """Choose deterministic, range-spanning display points; never use for statistics."""
    ordered = sorted(rows, key=lambda row: (row[value_field], row.get("identity", ""),
                                            row.get("source_identity", "")))
    if limit <= 0:
        return []
    if len(ordered) <= limit:
        return ordered
    if limit == 1:
        return [ordered[len(ordered) // 2]]
    indexes = {round(index * (len(ordered) - 1) / (limit - 1)) for index in range(limit)}
    return [ordered[index] for index in sorted(indexes)]


def format_time_ms(value: float, scale: float | None = None) -> str:
    if scale is None:
        scale = 1000.0 if abs(value) >= 1000 else 1.0
    if scale == 1000.0:
        return f"{value / 1000:.3f} s"
    return f"{value:.2f} ms"


def format_delta_ms(value: float, scale: float | None = None) -> str:
    sign = "+" if value > 0 else ""
    if scale is None:
        scale = 1000.0 if abs(value) >= 1000 else 1.0
    if scale == 1000.0:
        return f"{sign}{value / 1000:.3f} s"
    return f"{sign}{value:.2f} ms"


def axis_unit(values: list[float]) -> tuple[float, str]:
    if max((abs(value) for value in values), default=0) >= 1000:
        return 1000.0, "s"
    return 1.0, "ms"


def logarithmic_ticks(low: float, high: float) -> list[float]:
    if low <= 0 or high <= low:
        return []
    first_power = math.floor(math.log10(low)) - 1
    last_power = math.ceil(math.log10(high)) + 1
    ticks = sorted({factor * 10 ** power
                    for power in range(first_power, last_power + 1)
                    for factor in (1, 2, 5)
                    if low <= factor * 10 ** power <= high})
    if len(ticks) < 3:
        ticks = sorted({low, *ticks, high})
    return ticks


def duration_axis(values: list[float]) -> dict[str, Any]:
    positives = [value for value in values if value > 0]
    maximum = max(values)
    scale, unit = axis_unit(values)
    use_log = bool(positives) and maximum / min(positives) >= 10
    if use_log:
        reference = min(positives)
        has_zero = min(values) == 0
        transform = ((lambda value: math.log1p(value / reference)) if has_zero
                     else (lambda value: math.log(value)))
        low = 0.0 if has_zero else reference / 1.2
        high = max(maximum * 1.2, low + 1.0)
        raw_ticks = logarithmic_ticks(max(reference / 1.2, 1e-12), high)
        if has_zero:
            raw_ticks = sorted({0.0, *raw_ticks})
        low_position, high_position = transform(low), transform(high)
        if not raw_ticks:
            raw_ticks = [low, high]
        scale_label = (f"log1p scale, reference {format_time_ms(reference, scale)}"
                       if has_zero else "logarithmic scale")
    else:
        low, high = padded_domain(values)
        low = max(0.0, low)
        transform = lambda value: value
        raw_ticks = nice_ticks(low, high)
        low_position, high_position = low, high
        scale_label = "linear scale"
    if high_position <= low_position:
        high_position = low_position + 1.0
    return {
        "low": low,
        "high": high,
        "low_position": low_position,
        "high_position": high_position,
        "transform": transform,
        "ticks": raw_ticks,
        "unit_scale": scale,
        "unit": unit,
        "label": scale_label,
    }


def nice_ticks(low: float, high: float) -> list[float]:
    span = high - low
    if not math.isfinite(span) or span <= 0:
        return [low]
    rough = span / 5
    power = math.floor(math.log10(rough))
    candidates = sorted({factor * (10 ** exponent)
                         for exponent in range(power - 2, power + 3)
                         for factor in (1, 2, 2.5, 5, 10)})

    def ticks(step: float) -> list[float]:
        first = math.ceil(low / step - 1e-10)
        last = math.floor(high / step + 1e-10)
        return [index * step for index in range(first, last + 1)]

    step = min(candidates, key=lambda candidate: (
        0 if 4 <= len(ticks(candidate)) <= 7 else 1,
        abs(len(ticks(candidate)) - 5),
        abs(math.log(candidate / rough)),
    ))
    return ticks(step)


def format_axis(value: float, scale: float, unit: str) -> str:
    scaled = value / scale
    magnitude = abs(scaled)
    decimals = 0 if magnitude >= 100 else (1 if magnitude >= 10 else 2)
    if abs(scaled - round(scaled)) < 1e-8:
        decimals = 0
    return f"{scaled:.{decimals}f} {unit}"


def read_csv(path: Path, measured_run_ids: set[str] | None = None
             ) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    valid_rows: list[dict[str, Any]] = []
    invalid_rows: list[dict[str, Any]] = []
    with path.open(newline="", encoding="utf-8-sig") as source:
        reader = csv.DictReader(source)
        columns = set(reader.fieldnames or [])
        missing = REQUIRED_COLUMNS - columns
        if missing:
            raise ValueError(f"{path} is missing required per-parse CSV columns: {', '.join(sorted(missing))}")
        has_phase = "phase" in columns
        has_measured = "measured" in columns
        if not has_phase and not has_measured and measured_run_ids is None:
            raise ValueError(
                f"{path} has no phase/measured marker. Supply --runs-csv so seed rows can be excluded by run_id."
            )

        for line_number, raw in enumerate(reader, start=2):
            suite = canonical_suite(raw.get("suite"))
            cache_mode = str(raw.get("cache_mode") or "unspecified").strip() or "unspecified"
            policy = canonical_policy(raw.get("gc_policy"))
            reason: str | None = None
            run_ok = flag(raw.get("run_valid"))
            event_ok = flag(raw.get("event_valid"))
            if run_ok is not True:
                reason = "run invalid or unverified"
            elif event_ok is not True:
                reason = str(raw.get("validity_reason") or "parse event invalid or incomplete")

            elapsed = number(raw.get("parse_elapsed_ms"))
            timing_boundary = str(raw.get("parse_timing_boundary") or "").strip()
            protocol = canonical_protocol(raw.get("protocol"))
            identity = str(raw.get("source_identity") or "").strip()
            parse_pair_key = str(raw.get("parse_pair_key") or "").strip()
            pair_available = flag(raw.get("pair_available"))
            pair_id = str(raw.get("pair_id") or "").strip()
            repeat = str(raw.get("repeat") or "").strip()
            run_id = str(raw.get("run_id") or "").strip()
            cache_enabled = flag(raw.get("cache_enabled"))
            explicit_gc_disabled = flag(raw.get("explicit_gc_disabled"))
            if reason is None and (
                    (has_phase and str(raw.get("phase") or "").strip().lower() != "measured")
                    or (has_measured and flag(raw.get("measured")) is not True)):
                reason = "seed or non-measured parse row"
            elif reason is None and measured_run_ids is not None and run_id not in measured_run_ids:
                reason = "parse row has no valid measured run in runs.csv"
            if reason is None and suite is None:
                reason = f"unsupported suite {raw.get('suite')!r}"
            if reason is None and protocol is None:
                reason = f"unsupported protocol {raw.get('protocol')!r}"
            if reason is None and (elapsed is None or elapsed < 0):
                reason = "missing or invalid parse_elapsed_ms"
            if reason is None and not timing_boundary:
                reason = "missing parse_timing_boundary"
            if reason is None and not identity:
                reason = "missing source_identity"
            expected_gc_disabled = policy == "disabled"
            if (reason is None and "explicit_gc_disabled" in columns
                    and policy in {"normal", "disabled"}
                    and explicit_gc_disabled is not expected_gc_disabled):
                reason = "effective explicit-GC setting disagrees with gc_policy"

            if reason is not None:
                invalid_rows.append({
                    "line": line_number, "suite": suite or str(raw.get("suite") or "unknown"),
                    "cache_mode": cache_mode, "gc_policy": policy,
                    "protocol": protocol or str(raw.get("protocol") or "unknown"),
                    "reason": reason,
                })
                continue

            valid_rows.append({
                "suite": suite,
                "run_id": run_id,
                "cache_mode": cache_mode,
                "gc_policy": policy,
                "protocol": protocol,
                "pair_id": pair_id,
                "repeat": repeat,
                "source_identity": identity,
                "parse_pair_key": parse_pair_key,
                "pair_available": pair_available,
                "test_id": str(raw.get("test_id") or "").strip(),
                "resource_key": str(raw.get("resource_key") or "").strip(),
                "parse_pass": str(raw.get("parse_pass") or "").strip(),
                "source_path": str(raw.get("source_path") or "").strip(),
                "parse_id": str(raw.get("parse_id") or "").strip(),
                "elapsed_ms": elapsed,
                "timing_boundary": timing_boundary,
                "line": line_number,
                "ccache_disabled": flag(raw.get("ccache_disabled")),
                "cache_enabled": cache_enabled,
                "explicit_gc_disabled": explicit_gc_disabled,
            })

    if not valid_rows:
        raise ValueError(f"{path} contains no valid per-parse measurements")

    text_rows = [row for row in valid_rows if row["protocol"] == "text"]
    if text_rows and any(row["ccache_disabled"] is not False for row in text_rows):
        raise ValueError("Text rows must explicitly record ccache_disabled=false for the Text + ccache comparison")
    boundaries = {row["timing_boundary"] for row in valid_rows}
    if len(boundaries) > 1:
        raise ValueError(f"Per-parse rows mix timer boundaries: {', '.join(sorted(boundaries))}")
    return valid_rows, invalid_rows


def group_rows(rows: list[dict[str, Any]]) -> dict[tuple[str, str], dict[str, list[dict[str, Any]]]]:
    grouped: dict[tuple[str, str], dict[str, list[dict[str, Any]]]] = {}
    for row in rows:
        dimension = (row["gc_policy"], row["cache_mode"])
        grouped.setdefault(dimension, {}).setdefault(row["suite"], []).append(row)
    return grouped


def pair_rows(rows: list[dict[str, Any]]) -> tuple[
        dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
        dict[tuple[str, str, str], int],
        dict[tuple[str, str, str], int]]:
    indexed: dict[tuple[str, str, str, str, str], dict[str, dict[str, Any]]] = {}
    unavailable: dict[tuple[str, str, str], int] = {}
    for row in rows:
        dimension = (row["gc_policy"], row["cache_mode"], row["suite"])
        if row.get("pair_available") is not True or not row.get("parse_pair_key"):
            unavailable[dimension] = unavailable.get(dimension, 0) + 1
            continue
        if not row["pair_id"]:
            unavailable[dimension] = unavailable.get(dimension, 0) + 1
            continue
        key = (row["gc_policy"], row["cache_mode"], row["suite"], row["pair_id"],
               row["repeat"] + "\0" + row["parse_pair_key"])
        by_protocol = indexed.setdefault(key, {})
        protocol = row["protocol"]
        if protocol in by_protocol:
            raise ValueError(
                "Duplicate valid parse event for pairing key "
                f"{row['gc_policy']}/{row['cache_mode']}/{row['suite']}/{row['pair_id']}/"
                f"{row['repeat']}/{row['parse_pair_key']}/{protocol}"
            )
        by_protocol[protocol] = row

    pairs: dict[tuple[str, str], dict[str, list[dict[str, Any]]]] = {}
    unmatched: dict[tuple[str, str, str], int] = {}
    for key, by_protocol in indexed.items():
        policy, cache_mode, suite, pair_id, identity_key = key
        text_row, proto_row = by_protocol.get("text"), by_protocol.get("protobuf")
        if text_row is None or proto_row is None:
            unmatched[(policy, cache_mode, suite)] = unmatched.get((policy, cache_mode, suite), 0) + 1
            continue
        text_ms = text_row["elapsed_ms"]
        proto_ms = proto_row["elapsed_ms"]
        delta = proto_ms - text_ms
        pairs.setdefault((policy, cache_mode), {}).setdefault(suite, []).append({
            "pair_id": pair_id,
            "identity_key": identity_key,
            "identity": display_identity(text_row),
            "parse_pair_key": text_row["parse_pair_key"],
            "test_id": text_row.get("test_id", ""),
            "resource_key": text_row.get("resource_key", ""),
            "parse_pass": text_row.get("parse_pass", ""),
            "source_path": text_row.get("source_path", ""),
            "parse_id": text_row.get("parse_id", ""),
            "repeat": text_row["repeat"],
            "text_ms": text_ms,
            "protobuf_ms": proto_ms,
            "delta_ms": delta,
            "relative_pct": (delta / text_ms * 100) if text_ms > 0 else None,
        })
    for dimension in pairs:
        for suite in pairs[dimension]:
            pairs[dimension][suite].sort(key=lambda pair: (pair["pair_id"], pair["repeat"], pair["identity"]))
    return pairs, unmatched, unavailable


RUN_REQUIRED_COLUMNS = {
    "run_id", "pair_group_id", "pair_id", "suite", "phase", "measured", "cache_mode",
    "gc_policy", "protocol", "repeat", "return_code", "elapsed_s", "valid",
    "ccache_disabled", "ccache_cacheable_calls", "ccache_uncacheable_calls",
    "ccache_adapter_events", "ccache_event_counter_match", "gc_policy_verified",
    "identity_multiset_match", "source_content_match", "parse_args_match",
    "source_args_match", "cache_distribution_match",
}


def read_run_csv(path: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    valid_rows: list[dict[str, Any]] = []
    invalid_rows: list[dict[str, Any]] = []
    with path.open(newline="", encoding="utf-8-sig") as source:
        reader = csv.DictReader(source)
        columns = set(reader.fieldnames or [])
        missing = RUN_REQUIRED_COLUMNS - columns
        if missing:
            raise ValueError(f"{path} is missing required runs.csv columns: {', '.join(sorted(missing))}")
        for line_number, raw in enumerate(reader, start=2):
            suite = canonical_suite(raw.get("suite"))
            protocol = canonical_protocol(raw.get("protocol"))
            policy = canonical_policy(raw.get("gc_policy"))
            cache_mode = str(raw.get("cache_mode") or "unspecified").strip() or "unspecified"
            if str(raw.get("phase") or "").strip().lower() != "measured" or flag(raw.get("measured")) is not True:
                invalid_rows.append({
                    "line": line_number, "run_id": str(raw.get("run_id") or "").strip(),
                    "suite": suite or str(raw.get("suite") or "unknown"),
                    "gc_policy": policy, "cache_mode": cache_mode,
                    "reason": "seed or non-measured command row",
                })
                continue
            elapsed = number(raw.get("elapsed_s"))
            reason: str | None = None
            if flag(raw.get("valid")) is not True:
                reason = str(raw.get("validity_reason") or "run marked invalid")
            elif number(raw.get("return_code")) != 0:
                reason = f"nonzero return code {raw.get('return_code')}"
            elif flag(raw.get("gc_policy_verified")) is not True:
                reason = "GC policy was not verified in the worker"
            elif flag(raw.get("identity_multiset_match")) is not True:
                reason = "parse identity multisets did not match"
            elif flag(raw.get("source_content_match")) is not True:
                reason = "source-content digest multisets did not match"
            elif flag(raw.get("parse_args_match")) is not True:
                reason = "parser-argument digest multisets did not match"
            elif flag(raw.get("source_args_match")) is not True:
                reason = "joint source/configuration tuples did not match"
            elif flag(raw.get("cache_distribution_match")) is not True:
                reason = "ccache eligibility distributions did not match"
            elif flag(raw.get("ccache_event_counter_match")) is not True:
                reason = "run-level ccache event counters did not match parser events"
            elif (count(raw.get("ccache_adapter_events")) is None
                  or count(raw.get("ccache_cacheable_calls")) is None
                  or count(raw.get("ccache_uncacheable_calls")) is None
                  or count(raw.get("ccache_adapter_events")) != count(raw.get("ccache_cacheable_calls"))
                  + count(raw.get("ccache_uncacheable_calls"))):
                reason = "ccache adapter event count did not equal cacheable plus uncacheable calls"
            elif suite is None:
                reason = f"unsupported suite {raw.get('suite')!r}"
            elif protocol is None:
                reason = f"unsupported protocol {raw.get('protocol')!r}"
            elif elapsed is None or elapsed < 0:
                reason = "missing or invalid elapsed_s"
            elif protocol == "text" and flag(raw.get("ccache_disabled")) is not False:
                reason = "Text command did not verify ccache enabled"

            if reason is not None:
                invalid_rows.append({
                    "line": line_number, "run_id": str(raw.get("run_id") or "").strip(),
                    "suite": suite or str(raw.get("suite") or "unknown"),
                    "gc_policy": policy, "cache_mode": cache_mode, "reason": reason,
                })
                continue
            valid_rows.append({
                "run_id": str(raw.get("run_id") or "").strip(),
                "pair_group_id": str(raw.get("pair_group_id") or "").strip(),
                "pair_id": str(raw.get("pair_id") or "").strip(),
                "suite": suite,
                "cache_mode": cache_mode,
                "gc_policy": policy,
                "protocol": protocol,
                "repeat": str(raw.get("repeat") or "").strip(),
                "elapsed_s": elapsed,
                "ccache_disabled": flag(raw.get("ccache_disabled")),
                "ccache_cacheable_calls": count(raw.get("ccache_cacheable_calls")),
                "ccache_hits": count(raw.get("ccache_hits")),
                "ccache_misses": count(raw.get("ccache_misses")),
                "ccache_uncacheable_calls": count(raw.get("ccache_uncacheable_calls")),
                "parse_event_count": count(raw.get("parse_event_count")),
                "expected_event_count": count(raw.get("expected_event_count")),
                "test_total": count(raw.get("test_total")),
                "test_passed": count(raw.get("test_passed")),
                "test_failed": count(raw.get("test_failed")),
                "test_skipped": count(raw.get("test_skipped")),
                "line": line_number,
            })
    return valid_rows, invalid_rows


def pair_run_rows(rows: list[dict[str, Any]]) -> tuple[
        dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
        dict[tuple[str, str], list[dict[str, Any]]],
        dict[tuple[str, str], int]]:
    indexed: dict[tuple[str, str, str, str, str], dict[str, dict[str, Any]]] = {}
    for row in rows:
        if not row["pair_id"]:
            continue
        key = (row["gc_policy"], row["cache_mode"], row["suite"],
               row["pair_id"], row["repeat"])
        by_protocol = indexed.setdefault(key, {})
        if row["protocol"] in by_protocol:
            raise ValueError(f"Duplicate valid run row for pairing key {key}/{row['protocol']}")
        by_protocol[row["protocol"]] = row

    pairs_by_suite: dict[tuple[str, str], dict[str, list[dict[str, Any]]]] = {}
    unmatched_by_dimension: dict[tuple[str, str], int] = {}
    groups: dict[tuple[str, str, str], dict[str, dict[str, Any]]] = {}
    for key, by_protocol in indexed.items():
        policy, cache_mode, suite, pair_id, repeat = key
        text_row, proto_row = by_protocol.get("text"), by_protocol.get("protobuf")
        if text_row is None or proto_row is None:
            dimension = (policy, cache_mode)
            unmatched_by_dimension[dimension] = unmatched_by_dimension.get(dimension, 0) + 1
            continue
        if text_row["pair_group_id"] != proto_row["pair_group_id"]:
            raise ValueError(f"Text and Protobuf commands have different pair_group_id values for {key}")
        delta = proto_row["elapsed_s"] - text_row["elapsed_s"]
        pair = {
            "pair_id": pair_id,
            "pair_group_id": text_row["pair_group_id"],
            "repeat": repeat,
            "suite": suite,
            "text_s": text_row["elapsed_s"],
            "protobuf_s": proto_row["elapsed_s"],
            "delta_s": delta,
            "relative_pct": (delta / text_row["elapsed_s"] * 100)
                            if text_row["elapsed_s"] > 0 else None,
        }
        dimension = (policy, cache_mode)
        pairs_by_suite.setdefault(dimension, {}).setdefault(suite, []).append(pair)
        group_id = pair["pair_group_id"]
        if group_id:
            group_key = (policy, cache_mode, group_id)
            per_suite = groups.setdefault(group_key, {})
            if suite in per_suite:
                raise ValueError(f"Duplicate suite pair for global wall-time group {group_key}/{suite}")
            per_suite[suite] = pair
        else:
            unmatched_by_dimension[dimension] = unmatched_by_dimension.get(dimension, 0) + 1

    global_pairs: dict[tuple[str, str], list[dict[str, Any]]] = {}
    required_suites = set(SUITES)
    for (policy, cache_mode, group_id), per_suite in groups.items():
        if set(per_suite) != required_suites:
            dimension = (policy, cache_mode)
            unmatched_by_dimension[dimension] = unmatched_by_dimension.get(dimension, 0) + 1
            continue
        repeat_values = {pair["repeat"] for pair in per_suite.values()}
        if len(repeat_values) != 1:
            raise ValueError(f"Suite command repeats differ within global wall-time group {group_id}")
        text_s = sum(pair["text_s"] for pair in per_suite.values())
        protobuf_s = sum(pair["protobuf_s"] for pair in per_suite.values())
        delta_s = protobuf_s - text_s
        global_pairs.setdefault((policy, cache_mode), []).append({
            "pair_id": group_id,
            "pair_group_id": group_id,
            "repeat": next(iter(repeat_values)),
            "suite": "global",
            "text_s": text_s,
            "protobuf_s": protobuf_s,
            "delta_s": delta_s,
            "relative_pct": (delta_s / text_s * 100) if text_s > 0 else None,
        })

    for dimension in pairs_by_suite:
        for suite in pairs_by_suite[dimension]:
            pairs_by_suite[dimension][suite].sort(
                key=lambda pair: (pair["pair_id"], pair["repeat"]))
    for dimension in global_pairs:
        global_pairs[dimension].sort(key=lambda pair: pair["pair_group_id"])
    return pairs_by_suite, global_pairs, unmatched_by_dimension


def run_wall_table(rows: list[dict[str, Any]],
                   pairs_by_suite: dict[str, list[dict[str, Any]]],
                   global_pairs: list[dict[str, Any]]) -> str:
    output_rows: list[str] = []
    for suite in SUITES:
        suite_rows = [row for row in rows if row["suite"] == suite]
        pairs = pairs_by_suite.get(suite, [])
        by_protocol = {
            protocol: [row["elapsed_s"] * 1000 for row in suite_rows if row["protocol"] == protocol]
            for protocol in PROTOCOL_ORDER
        }
        delta_ms = [pair["delta_s"] * 1000 for pair in pairs]
        pct = [pair["relative_pct"] for pair in pairs if pair["relative_pct"] is not None]
        median_pct = f"{statistics.median(pct):+.2f}%" if pct else "n/a"
        output_rows.append(
            f'<tr><th scope="row">{esc(SUITES[suite])}</th>'
            f'<td>{fmt_summary(by_protocol["text"])}</td>'
            f'<td>{fmt_summary(by_protocol["protobuf"])}</td>'
            f'<td>{fmt_summary(delta_ms, paired=True)}</td><td>{esc(median_pct)}</td></tr>'
        )
    text_ms = [pair["text_s"] * 1000 for pair in global_pairs]
    protobuf_ms = [pair["protobuf_s"] * 1000 for pair in global_pairs]
    delta_ms = [pair["delta_s"] * 1000 for pair in global_pairs]
    pct = [pair["relative_pct"] for pair in global_pairs if pair["relative_pct"] is not None]
    median_pct = f"{statistics.median(pct):+.2f}%" if pct else "n/a"
    output_rows.append(
        f'<tr class="pooled"><th scope="row">Sequential Clava-JS + Java block</th>'
        f'<td>{fmt_summary(text_ms)}</td><td>{fmt_summary(protobuf_ms)}</td>'
        f'<td>{fmt_summary(delta_ms, paired=True)}</td><td>{esc(median_pct)}</td></tr>'
    )
    return (
        '<div class="table-wrap"><table><thead><tr><th>Command scope</th>'
        '<th>Text + ccache wall time</th><th>Protobuf wall time</th>'
        '<th>Matched command delta</th><th>Median matched change</th>'
        '</tr></thead><tbody>' + "".join(output_rows) + '</tbody></table></div>'
    )


def wall_chart_rows(pairs_by_suite: dict[str, list[dict[str, Any]]],
                    global_pairs: list[dict[str, Any]]) -> list[tuple[str, list[dict[str, Any]]]]:
    groups: list[tuple[str, list[dict[str, Any]]]] = []
    for suite in SUITES:
        rows = []
        for pair in pairs_by_suite.get(suite, []):
            rows.append({
                "pair_id": pair["pair_id"], "repeat": pair["repeat"],
                "identity": f'{SUITES[suite]} command pair {pair["pair_id"]}',
                "text_ms": pair["text_s"] * 1000,
                "protobuf_ms": pair["protobuf_s"] * 1000,
                "delta_ms": pair["delta_s"] * 1000,
            })
        if rows:
            groups.append((SUITES[suite], rows))
    total_rows = [{
        "pair_id": pair["pair_group_id"], "repeat": "",
        "identity": f'Sequential suite block {pair["pair_group_id"]}',
        "text_ms": pair["text_s"] * 1000,
        "protobuf_ms": pair["protobuf_s"] * 1000,
        "delta_ms": pair["delta_s"] * 1000,
    } for pair in global_pairs]
    if total_rows:
        groups.append(("Sequential suite block", total_rows))
    return groups


def ccache_counter_table(rows: list[dict[str, Any]]) -> str:
    output_rows: list[str] = []
    counter_fields = (
        ("ccache_cacheable_calls", "Cacheable calls"),
        ("ccache_hits", "Hits"),
        ("ccache_misses", "Misses"),
        ("ccache_uncacheable_calls", "Uncacheable calls"),
    )
    for protocol in PROTOCOL_ORDER:
        selected = [row for row in rows if row["protocol"] == protocol]
        if not selected:
            continue
        states = {row["ccache_disabled"] for row in selected}
        state = "enabled" if states == {False} else (
            "disabled" if states == {True} else "not recorded" if states == {None} else "mixed"
        )
        values: list[str] = []
        for field, _label in counter_fields:
            known = [row[field] for row in selected if row[field] is not None]
            if not known:
                values.append("unavailable")
            elif len(known) == len(selected):
                values.append(str(sum(known)))
            else:
                values.append(f"{sum(known)} ({len(known)}/{len(selected)} runs)")
        output_rows.append(
            f'<tr><th scope="row">{esc(PROTOCOLS[protocol][0])}</th>'
            f'<td>{esc(state)}</td>' + "".join(f"<td>{esc(value)}</td>" for value in values) + "</tr>"
        )
    if not output_rows:
        return '<p class="empty">No valid measured command counters.</p>'
    headers = "".join(f"<th>{label}</th>" for _, label in counter_fields)
    return (
        '<div class="table-wrap"><table><thead><tr><th>Protocol</th><th>ccache setting</th>'
        + headers + '</tr></thead><tbody>' + "".join(output_rows) + '</tbody></table></div>'
    )


def padded_domain(values: list[float], include_zero: bool = False) -> tuple[float, float]:
    minimum, maximum = min(values), max(values)
    span = maximum - minimum
    padding = max(span * .13, abs(maximum) * .035, 1.0)
    low, high = minimum - padding, maximum + padding
    if include_zero:
        low, high = min(low, 0.0), max(high, 0.0)
    return low, high


def svg_distribution_chart(title: str, by_protocol: dict[str, list[dict[str, Any]]],
                          max_points_per_protocol: int = 250,
                          show_points: bool = True) -> str:
    entries = [(protocol, row) for protocol in PROTOCOL_ORDER for row in by_protocol.get(protocol, [])]
    values = [row["elapsed_ms"] for _, row in entries]
    if not values:
        return '<p class="empty">No valid parse measurements for this group.</p>'
    axis = duration_axis(values)
    scale, unit = axis["unit_scale"], axis["unit"]
    fmt_time = lambda value: format_time_ms(value, scale)
    width, height, left, right = 1160, 264, 235, 890
    x = lambda value: left + (right - left) * (
        axis["transform"](value) - axis["low_position"]
    ) / (axis["high_position"] - axis["low_position"])
    svg = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)} individual parse latency distributions">',
        f'<title>{esc(title)} individual parse latency distributions</title>',
        '<desc>Candles and whiskers summarize every valid measured parse event using the 1.5 interquartile-range rule. '
        + (f'Dots are a deterministic display sample, at most {max_points_per_protocol} per protocol; '
           'they do not determine the candle statistics.' if show_points
           else 'Dots are omitted in this pooled summary; candles use every parse event.') + '</desc>',
    ]
    for tick in axis["ticks"]:
        tx = x(tick)
        svg.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="35" y2="222" class="grid-line"/>')
        svg.append(f'<text x="{tx:.2f}" y="27" text-anchor="middle" class="axis-text">{esc(format_axis(tick, scale, unit))}</text>')
    for index, protocol in enumerate(PROTOCOL_ORDER):
        rows = by_protocol.get(protocol, [])
        if not rows:
            continue
        values_for_protocol = [row["elapsed_ms"] for row in rows]
        stats = distribution(values_for_protocol)
        y = 93 + index * 94
        label, color = PROTOCOLS[protocol]
        svg.append(f'<text x="14" y="{y + 5}" class="condition-text">{esc(label)}</text>')
        svg.append(f'<line x1="{x(stats["whisker_minimum"]):.2f}" x2="{x(stats["whisker_maximum"]):.2f}" y1="{y}" y2="{y}" stroke="{color}" stroke-width="2"/>')
        for end in (stats["whisker_minimum"], stats["whisker_maximum"]):
            svg.append(f'<line x1="{x(end):.2f}" x2="{x(end):.2f}" y1="{y - 10}" y2="{y + 10}" stroke="{color}" stroke-width="2"/>')
        box_width = max(3.0, x(stats["q3"]) - x(stats["q1"]))
        svg.append(f'<rect x="{x(stats["q1"]):.2f}" y="{y - 16}" width="{box_width:.2f}" height="32" rx="3" fill="{color}" fill-opacity=".22" stroke="{color}" stroke-width="1.6"><title>Q1 {esc(fmt_time(stats["q1"]))}, median {esc(fmt_time(stats["median"]))}, Q3 {esc(fmt_time(stats["q3"]))}</title></rect>')
        svg.append(f'<line x1="{x(stats["median"]):.2f}" x2="{x(stats["median"]):.2f}" y1="{y - 18}" y2="{y + 18}" stroke="{color}" stroke-width="4"/>')
        plotted_rows = (display_sample(rows, "elapsed_ms", max_points_per_protocol)
                        if show_points else [])
        for point_index, row in enumerate(plotted_rows):
            value = row["elapsed_ms"]
            jitter = ((point_index * 37 + len(row.get("source_identity", ""))) % 11 - 5) * 3.3
            klass = ("point outlier" if value < stats["whisker_minimum"]
                     or value > stats["whisker_maximum"] else "point")
            svg.append(f'<circle cx="{x(value):.2f}" cy="{y + jitter:.2f}" r="3.5" class="{klass}" fill="{color}"/>')
        svg.append(f'<text x="916" y="{y - 3}" class="sample-text">n={stats["n"]}</text>')
        point_label = f'dots {len(plotted_rows)}/{stats["n"]}' if show_points else 'dots omitted'
        svg.append(f'<text x="916" y="{y + 16}" class="detail-text">median {esc(fmt_time(stats["median"]))}, IQR {esc(fmt_time(stats["q3"] - stats["q1"]))}, outliers {len(stats["outliers"])}; {point_label}</text>')
    svg.append(f'<text x="{(left + right) / 2:.1f}" y="251" text-anchor="middle" class="axis-title">Parser invocation latency ({unit}, {esc(axis["label"])})</text>')
    svg.append('</svg>')
    return "".join(svg)


def delta_chart_needs_central_scale(by_group: list[tuple[str, list[dict[str, Any]]]],
                                    threshold: float = 10.0) -> bool:
    values = [pair["delta_ms"] for _, rows in by_group for pair in rows]
    if len(values) < 2:
        return False
    core_low = min(distribution([pair["delta_ms"] for pair in rows])["whisker_minimum"]
                   for _, rows in by_group if rows)
    core_high = max(distribution([pair["delta_ms"] for pair in rows])["whisker_maximum"]
                    for _, rows in by_group if rows)
    full_span = max(values) - min(values)
    core_span = max(core_high - core_low, max(abs(core_low), abs(core_high)) * .01, 1.0)
    return full_span / core_span >= threshold


def svg_delta_chart(title: str, by_group: list[tuple[str, list[dict[str, Any]]]],
                    metric_label: str = "source invocation",
                    central_scale: bool = False,
                    max_points_per_group: int = 250,
                    show_points: bool = True,
                    point_groups: set[str] | None = None) -> str:
    available = [(name, rows) for name, rows in by_group if rows]
    if not available:
        return '<p class="empty">No complete matched parse pairs for this GC policy.</p>'
    values = [pair["delta_ms"] for _, rows in available for pair in rows]
    stats_by_group = {
        name: distribution([pair["delta_ms"] for pair in rows])
        for name, rows in available
    }
    if central_scale:
        core_low = min(stats["whisker_minimum"] for stats in stats_by_group.values())
        core_high = max(stats["whisker_maximum"] for stats in stats_by_group.values())
        low, high = padded_domain([core_low, core_high], include_zero=True)
    else:
        low, high = padded_domain(values, include_zero=True)
    scale, unit = axis_unit(values)
    fmt_delta = lambda value: format_delta_ms(value, scale)
    width, height = 1160, 104 + 82 * len(available)
    left, right = 235, 890
    x = lambda value: left + (right - left) * (value - low) / (high - low)
    svg = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)} paired {esc(metric_label)} differences">',
        f'<title>{esc(title)} paired {esc(metric_label)} differences</title>',
        f'<desc>The paired delta is Protobuf minus Text plus ccache. Positive values mean Protobuf took longer. '
        'Candle and whisker statistics use every matched invocation. '
        + (f'Dots are a deterministic display sample, at most {max_points_per_group} per group; '
           'they do not determine the candle statistics.' if show_points
           else 'Dots are omitted in this candle summary; all matched invocations determine the statistics.')
        + (" This central-scale view counts observations outside its linear axis; the full-range chart preserves the complete range." if central_scale else " The axis spans the full observed range.")
        + "</desc>",
    ]
    for tick in nice_ticks(low, high):
        tx = x(tick)
        klass = "zero-line" if abs(tick) < 1e-9 else "grid-line"
        svg.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="34" y2="{height - 32}" class="{klass}"/>')
        svg.append(f'<text x="{tx:.2f}" y="26" text-anchor="middle" class="axis-text">{esc(format_axis(tick, scale, unit))}</text>')
    for index, (name, rows) in enumerate(available):
        values_for_group = [pair["delta_ms"] for pair in rows]
        stats = stats_by_group[name]
        y = 75 + index * 82
        svg.append(f'<text x="14" y="{y + 5}" class="condition-text">{esc(name)}</text>')
        svg.append(f'<line x1="{x(stats["whisker_minimum"]):.2f}" x2="{x(stats["whisker_maximum"]):.2f}" y1="{y}" y2="{y}" class="delta-whisker"/>')
        for end in (stats["whisker_minimum"], stats["whisker_maximum"]):
            svg.append(f'<line x1="{x(end):.2f}" x2="{x(end):.2f}" y1="{y - 9}" y2="{y + 9}" class="delta-whisker"/>')
        box_width = max(3.0, x(stats["q3"]) - x(stats["q1"]))
        svg.append(f'<rect x="{x(stats["q1"]):.2f}" y="{y - 15}" width="{box_width:.2f}" height="30" rx="3" class="delta-box"><title>Q1 {esc(fmt_delta(stats["q1"]))}, median {esc(fmt_delta(stats["median"]))}, Q3 {esc(fmt_delta(stats["q3"]))}</title></rect>')
        svg.append(f'<line x1="{x(stats["median"]):.2f}" x2="{x(stats["median"]):.2f}" y1="{y - 17}" y2="{y + 17}" class="delta-median"/>')
        off_scale = 0
        draw_points = show_points and (point_groups is None or name in point_groups)
        plotted_rows = (display_sample(rows, "delta_ms", max_points_per_group)
                        if draw_points else [])
        if central_scale:
            off_scale = sum(not low <= pair["delta_ms"] <= high for pair in rows)
        for point_index, pair in enumerate(plotted_rows):
            delta = pair["delta_ms"]
            if central_scale and not low <= delta <= high:
                continue
            jitter = ((point_index * 37 + len(pair["identity"])) % 11 - 5) * 3.0
            klass = ("point outlier" if delta < stats["whisker_minimum"]
                     or delta > stats["whisker_maximum"] else "point")
            svg.append(f'<circle cx="{x(delta):.2f}" cy="{y + jitter:.2f}" r="3.5" class="{klass}" fill="#334155"/>')
        if central_scale:
            svg.append(f'<text x="916" y="{y - 3}" class="sample-text">n={stats["n"]}</text>')
            svg.append(f'<text x="916" y="{y + 16}" class="detail-text">median {esc(fmt_delta(stats["median"]))}, IQR {esc(fmt_delta(stats["q3"] - stats["q1"]))}; dots omitted; off-scale {off_scale}</text>')
        else:
            svg.append(f'<text x="916" y="{y - 3}" class="sample-text">n={stats["n"]}</text>')
            point_label = f'dots {len(plotted_rows)}/{stats["n"]}' if draw_points else 'dots omitted'
            svg.append(f'<text x="916" y="{y + 16}" class="detail-text">median {esc(fmt_delta(stats["median"]))}, IQR {esc(fmt_delta(stats["q3"] - stats["q1"]))}; {point_label}</text>')
    axis_label = "central linear scale" if central_scale else "full linear range"
    svg.append(f'<text x="{(left + right) / 2:.1f}" y="{height - 8}" text-anchor="middle" class="axis-title">Protobuf minus Text + ccache per {esc(metric_label)} ({unit}, {axis_label})</text>')
    svg.append('</svg>')
    return "".join(svg)


def fmt_summary(values: list[float], paired: bool = False) -> str:
    if not values:
        return "n/a (n=0)"
    stats = distribution(values)
    scale, _ = axis_unit(values)
    center = format_delta_ms(stats["median"], scale) if paired else format_time_ms(stats["median"], scale)
    q1 = format_delta_ms(stats["q1"], scale) if paired else format_time_ms(stats["q1"], scale)
    q3 = format_delta_ms(stats["q3"], scale) if paired else format_time_ms(stats["q3"], scale)
    return f"median {center}<br><small>Q1 to Q3 {q1} to {q3}; n={stats['n']}</small>"


def direction_summary(pairs: list[dict[str, Any]]) -> str:
    if not pairs:
        return "n/a (n=0)"
    total = len(pairs)
    faster = sum(pair["delta_ms"] < 0 for pair in pairs)
    tied = sum(pair["delta_ms"] == 0 for pair in pairs)
    slower = sum(pair["delta_ms"] > 0 for pair in pairs)
    return (
        f'<small>faster {faster}/{total} ({faster / total:.0%}) · '
        f'tied {tied}/{total} · slower {slower}/{total} ({slower / total:.0%})</small>'
    )


def cache_state_summary(rows: list[dict[str, Any]]) -> str:
    fragments: list[str] = []
    for protocol in PROTOCOL_ORDER:
        selected = [row for row in rows if row["protocol"] == protocol]
        if not selected:
            continue
        known = [row.get("cache_enabled") for row in selected if row.get("cache_enabled") is not None]
        if not known:
            state = "unavailable"
        else:
            state = f"{sum(value is True for value in known)}/{len(selected)}"
            if len(known) != len(selected):
                state += f" known ({len(known)}/{len(selected)} events)"
        fragments.append(f'{PROTOCOLS[protocol][0]} {state}')
    if not fragments:
        return "Cache-enabled event flags: unavailable."
    return "Events with cache_enabled=true (not hit counts): " + "; ".join(fragments) + "."


def pair_eligibility_summary(rows: list[dict[str, Any]]) -> str:
    fragments: list[str] = []
    total_unpairable = 0
    total_events = 0
    for protocol in PROTOCOL_ORDER:
        selected = [row for row in rows if row["protocol"] == protocol]
        if not selected:
            continue
        per_run: dict[str, list[dict[str, Any]]] = {}
        for row in selected:
            per_run.setdefault(row["pair_id"] or "(no pair id)", []).append(row)

        def not_pairable(row: dict[str, Any]) -> bool:
            return (row.get("pair_available") is not True
                    or not row.get("parse_pair_key")
                    or not row.get("pair_id"))

        unpairable = sum(not_pairable(row) for row in selected)
        total_unpairable += unpairable
        total_events += len(selected)
        unpairable_per_run = [sum(not_pairable(row) for row in group)
                              for group in per_run.values()]
        events_per_run = [len(group) for group in per_run.values()]
        if (unpairable_per_run and len(set(unpairable_per_run)) == 1
                and len(set(events_per_run)) == 1):
            detail = (
                f'{unpairable_per_run[0]}/{events_per_run[0]} per run × {len(per_run)} runs; '
                f'{unpairable}/{len(selected)} total'
            )
        else:
            detail = f'{unpairable}/{len(selected)} events'
        fragments.append(f'{PROTOCOLS[protocol][0]}: {detail}')

    if not fragments:
        return "Pairability counts unavailable."
    return (
        "Events not individually pairable: " + "; ".join(fragments)
        + f". {total_unpairable}/{total_events} event rows across both protocols stay in the runtime candles "
        "but are excluded from paired deltas."
    )


def summary_table(policy: str, raw_by_suite: dict[str, list[dict[str, Any]]],
                  pairs_by_suite: dict[str, list[dict[str, Any]]]) -> str:
    rows: list[str] = []
    suite_order = [suite for suite in SUITES if suite in raw_by_suite or suite in pairs_by_suite]
    for suite in suite_order:
        raw = raw_by_suite.get(suite, [])
        paired = pairs_by_suite.get(suite, [])
        by_protocol = {protocol: [row["elapsed_ms"] for row in raw if row["protocol"] == protocol]
                       for protocol in PROTOCOL_ORDER}
        deltas = [pair["delta_ms"] for pair in paired]
        percentages = [pair["relative_pct"] for pair in paired if pair["relative_pct"] is not None]
        pct = f"{statistics.median(percentages):+.2f}%" if percentages else "n/a"
        rows.append(
            f'<tr><th scope="row">{esc(SUITES[suite])}</th>'
            f'<td>{fmt_summary(by_protocol["text"])}</td>'
            f'<td>{fmt_summary(by_protocol["protobuf"])}</td>'
            f'<td>{fmt_summary(deltas, paired=True)}</td>'
            f'<td>{esc(pct)}</td>'
            f'<td>{direction_summary(paired)}</td></tr>'
        )

    all_raw = [row for rows in raw_by_suite.values() for row in rows]
    all_pairs = [pair for rows in pairs_by_suite.values() for pair in rows]
    by_protocol = {protocol: [row["elapsed_ms"] for row in all_raw if row["protocol"] == protocol]
                   for protocol in PROTOCOL_ORDER}
    deltas = [pair["delta_ms"] for pair in all_pairs]
    percentages = [pair["relative_pct"] for pair in all_pairs if pair["relative_pct"] is not None]
    pct = f"{statistics.median(percentages):+.2f}%" if percentages else "n/a"
    rows.append(
        f'<tr class="pooled"><th scope="row">Pooled per-parse sample</th>'
        f'<td>{fmt_summary(by_protocol["text"])}</td>'
        f'<td>{fmt_summary(by_protocol["protobuf"])}</td>'
        f'<td>{fmt_summary(deltas, paired=True)}</td>'
        f'<td>{esc(pct)}</td>'
        f'<td>{direction_summary(all_pairs)}</td></tr>'
    )
    return (
        '<div class="table-wrap"><table><thead><tr><th>Suite / aggregation</th>'
        '<th>Text + ccache latency</th><th>Protobuf latency</th>'
        '<th>Matched per-parse delta</th><th>Median matched change</th>'
        '<th>Paired invocations: faster / tied / slower</th>'
        '</tr></thead><tbody>' + "".join(rows) + '</tbody></table></div>'
    )


def top_source_changes(pairs: list[dict[str, Any]], limit: int = 5) -> dict[str, list[dict[str, Any]]]:
    """Return top positive/negative median per-source deltas across repeats."""
    by_source: dict[str, list[dict[str, Any]]] = {}
    for pair in pairs:
        key = str(pair["parse_pair_key"])
        by_source.setdefault(key, []).append(pair)

    summaries: list[dict[str, Any]] = []
    for key, source_pairs in by_source.items():
        representative = source_pairs[0]
        summaries.append({
            "parse_pair_key": key,
            "median_delta_ms": statistics.median(pair["delta_ms"] for pair in source_pairs),
            "repeat_count": len(source_pairs),
            "test_id": representative.get("test_id", ""),
            "resource_key": representative.get("resource_key", ""),
            "parse_pass": representative.get("parse_pass", ""),
            "source_path": representative.get("source_path", ""),
            "parse_id": representative.get("parse_id", ""),
            "identity": representative.get("identity", ""),
        })

    return {
        "slower": sorted(
            (row for row in summaries if row["median_delta_ms"] > 0),
            key=lambda row: (-row["median_delta_ms"], row["parse_pair_key"],
                             row["test_id"], row["source_path"]),
        )[:limit],
        "faster": sorted(
            (row for row in summaries if row["median_delta_ms"] < 0),
            key=lambda row: (row["median_delta_ms"], row["parse_pair_key"],
                             row["test_id"], row["source_path"]),
        )[:limit],
    }


def source_change_table(pairs: list[dict[str, Any]], limit: int = 5) -> str:
    changes = top_source_changes(pairs, limit)
    if not pairs:
        return '<p class="empty">No pairable source-invocation deltas.</p>'

    def render_side(title: str, rows: list[dict[str, Any]]) -> str:
        if not rows:
            return f'<section><h4>{title}</h4><p class="empty">None in this direction.</p></section>'
        values = [row["median_delta_ms"] for row in rows]
        scale, _unit = axis_unit(values)
        body: list[str] = []
        for row in rows:
            resource = safe_path_label(row["resource_key"])
            source = safe_path_label(row["source_path"])
            primary = row["test_id"] or resource or source or row["parse_id"] or row["identity"]
            primary = primary or row["identity"] or "parse event"
            details = []
            if resource and resource != primary:
                details.append(f'resource {resource}')
            if row["parse_pass"]:
                details.append(f'pass {row["parse_pass"]}')
            if row["parse_id"]:
                details.append(f'parser {row["parse_id"]}')
            if source and source != primary:
                details.append(source)
            title_text = " · ".join([str(primary), *details])
            visible = str(primary)
            if len(visible) > 78:
                visible = visible[:75] + "…"
            detail_html = f'<br><small>{esc(" · ".join(details))}</small>' if details else ""
            body.append(
                f'<tr><th scope="row" title="{esc(title_text)}">{esc(visible)}{detail_html}</th>'
                f'<td>{format_delta_ms(row["median_delta_ms"], scale)}</td>'
                f'<td>{row["repeat_count"]}</td></tr>'
            )
        return (
            f'<section><h4>{title}</h4><div class="table-wrap"><table class="compact-table">'
            '<thead><tr><th>Test / source</th><th>Median Δ</th><th>Repeats</th></tr></thead>'
            '<tbody>' + "".join(body) + '</tbody></table></div></section>'
        )

    return (
        '<div class="top-delta-grid">'
        + render_side("Largest Protobuf slowdowns", changes["slower"])
        + render_side("Largest Protobuf speedups", changes["faster"])
        + '</div>'
    )


def invalid_summary(invalid_rows: list[dict[str, Any]], policy: str, cache_mode: str,
                    suite: str | None = None) -> str:
    selected = [row for row in invalid_rows
                if row["gc_policy"] == policy and row["cache_mode"] == cache_mode
                and (suite is None or row["suite"] == suite)]
    if not selected:
        return "No invalid or incomplete rows were excluded for this policy."
    counts: dict[tuple[str, str], int] = {}
    for row in selected:
        key = (row["suite"], row["reason"])
        counts[key] = counts.get(key, 0) + 1
    items = [f"{esc(name)}: {count} row(s) {esc(reason)}"
             for (name, reason), count in sorted(counts.items())]
    return "; ".join(items)


def invalid_run_summary(invalid_rows: list[dict[str, Any]], policy: str,
                        cache_mode: str) -> str:
    selected = [row for row in invalid_rows
                if row["gc_policy"] == policy and row["cache_mode"] == cache_mode]
    if not selected:
        return "No invalid or non-measured command rows were excluded."
    counts: dict[tuple[str, str], int] = {}
    for row in selected:
        key = (row["suite"], row["reason"])
        counts[key] = counts.get(key, 0) + 1
    items = [f"{esc(name)}: {count} row(s) {esc(reason)}"
             for (name, reason), count in sorted(counts.items())]
    return "; ".join(items)


def first_recorded_count(rows: list[dict[str, Any]], field: str,
                         fallback: Any = None) -> str:
    values = {row[field] for row in rows if row.get(field) is not None}
    if len(values) == 1:
        return str(next(iter(values)))
    if len(values) > 1:
        return "varies"
    return str(fallback) if fallback is not None else "not recorded"


def measurement_panel(run_rows: list[dict[str, Any]], plan: dict[str, Any] | None,
                      pairs_by_suite: dict[tuple[str, str], dict[str, list[dict[str, Any]]]]) -> str:
    plan = plan or {}
    planned_tests = plan.get("expected_suite_counts", {})
    planned_events = plan.get("expected_parse_events", {})
    suite_rows: list[str] = []
    for suite in SUITES:
        selected = [row for row in run_rows if row["suite"] == suite]
        fallback = planned_tests.get(suite, {})
        total = first_recorded_count(selected, "test_total", fallback.get("total_tests"))
        passed = first_recorded_count(selected, "test_passed", fallback.get("passed_tests"))
        skipped = first_recorded_count(selected, "test_skipped", fallback.get("skipped_tests"))
        expected_events = first_recorded_count(
            selected, "expected_event_count", planned_events.get(suite))
        policy_repeats = []
        for (policy, _cache_mode), by_suite in pairs_by_suite.items():
            count_pairs = len(by_suite.get(suite, []))
            if count_pairs:
                policy_repeats.append(f"{policy}: {count_pairs}")
        repeats = ", ".join(policy_repeats) if policy_repeats else "not available"
        suite_rows.append(
            f'<tr><th scope="row">{esc(SUITES[suite])}</th>'
            f'<td>{esc(total)} total; {esc(passed)} passed; {esc(skipped)} skipped</td>'
            f'<td>{esc(expected_events)} per run</td><td>{esc(repeats)}</td></tr>'
        )

    warm = sorted({row["cache_mode"] for row in run_rows})
    cache_text = ", ".join(warm) if warm else str(plan.get("cache_mode", "not recorded"))
    repeat_counts = [len(by_suite[suite]) for by_suite in pairs_by_suite.values()
                     for suite in SUITES if by_suite.get(suite)]
    repeat_count = (repeat_counts[0] if repeat_counts and len(set(repeat_counts)) == 1
                    else plan.get("repeat_count") if not repeat_counts else None)
    repeat_sentence = (f"{repeat_count} paired measured repeats per GC condition" if repeat_count
                       else "Paired measured repeats per GC condition are listed below")
    parse_boundary = str(plan.get("parse_timing_boundary") or "not recorded")
    wall_boundary = str(plan.get("full_command_timing_boundary") or
                        "full elapsed time around each suite command")
    details = provenance_details(plan)
    revision_claim = ("Same captured source revision and runtime artifacts, "
                     if plan.get("sources") else "")
    return (
        '<section class="measurement-panel"><h2>What was measured</h2>'
        f'<p>{esc(revision_claim)}comparing Text + ccache with Protobuf; '
        f'ccache mode: <b>{esc(cache_text)}</b> for both. {esc(repeat_sentence)}.</p>'
        '<div class="table-wrap"><table class="compact-table"><thead><tr>'
        '<th>Suite</th><th>Test population per command</th><th>Parse events</th>'
        '<th>Matched command repeats by GC policy</th></tr></thead><tbody>'
        + "".join(suite_rows)
        + '</tbody></table></div>'
        f'<p class="chart-note"><b>Parse timer:</b> {esc(parse_boundary)}. '
        f'<b>Command wall timer:</b> {esc(wall_boundary)}. Command wall time includes caller-side heap logging and explicit GC; '
        'parse-call latency excludes them. These are separate measures, not sums of concurrent parse timings.</p>'
        + details + '</section>'
    )


def provenance_details(plan: dict[str, Any]) -> str:
    sources = plan.get("sources", {}) if isinstance(plan, dict) else {}
    rows: list[str] = []

    def revision(label: str, entry: Any) -> None:
        if not isinstance(entry, dict):
            return
        commit = entry.get("revision")
        if not commit:
            return
        dirty = entry.get("dirty")
        if isinstance(dirty, dict):
            status = dirty.get("status", [])
            state = "recorded working-tree edits" if status else "clean"
            diff_hash = dirty.get("diff_sha256")
        else:
            status = entry.get("status", [])
            state = "clean" if not status else "recorded working-tree edits"
            diff_hash = None
        suffix = f'; worktree diff SHA-256 <code>{esc(diff_hash)}</code>' if diff_hash else ""
        rows.append(
            f'<tr><th scope="row">{esc(label)}</th><td><code>{esc(commit)}</code> '
            f'({esc(state)}){suffix}</td></tr>'
        )

    revision("Clava runtime", sources.get("clava"))
    revision("Native clang-dumper", sources.get("native"))
    revision("Clava-JS", sources.get("clava_js"))
    dependencies = sources.get("java_build_dependencies", {})
    if isinstance(dependencies, dict):
        for name, entry in dependencies.items():
            revision(name, entry)

    native = sources.get("native", {}) if isinstance(sources, dict) else {}
    native_hash = native.get("tool_sha256") if isinstance(native, dict) else None
    jar_hash = plan.get("runtime_parser_jar_sha256") if isinstance(plan, dict) else None
    if native_hash:
        rows.append(f'<tr><th scope="row">Native executable SHA-256</th><td><code>{esc(native_hash)}</code></td></tr>')
    if jar_hash:
        rows.append(f'<tr><th scope="row">Runtime parser JAR SHA-256</th><td><code>{esc(jar_hash)}</code></td></tr>')
    tree = sources.get("clava_js", {}).get("tree_manifest", {}) if isinstance(sources, dict) else {}
    if isinstance(tree, dict) and tree.get("sha256"):
        rows.append(f'<tr><th scope="row">Clava-JS tree manifest SHA-256</th><td><code>{esc(tree["sha256"])}</code></td></tr>')
    if not rows:
        return '<details class="provenance"><summary>Revision and artifact fingerprints</summary><p>Provenance metadata was not supplied.</p></details>'
    return (
        '<details class="provenance"><summary>Revision and artifact fingerprints</summary>'
        '<div class="table-wrap"><table class="compact-table"><tbody>' + "".join(rows)
        + '</tbody></table></div></details>'
    )


def command_conclusion(by_suite: dict[str, list[dict[str, Any]]],
                       global_pairs: list[dict[str, Any]]) -> str:
    snippets = []
    for suite in SUITES:
        pairs = by_suite.get(suite, [])
        if not pairs:
            continue
        median_delta = statistics.median(pair["delta_s"] for pair in pairs)
        slower = sum(pair["delta_s"] > 0 for pair in pairs)
        faster = sum(pair["delta_s"] < 0 for pair in pairs)
        direction = f"{slower}/{len(pairs)} slower" if median_delta >= 0 else f"{faster}/{len(pairs)} faster"
        snippets.append(f'<b>{esc(SUITES[suite])}:</b> Protobuf {median_delta:+.3f} s '
                        f'({direction}).')
    if global_pairs:
        median_global = statistics.median(pair["delta_s"] for pair in global_pairs)
        snippets.append(f'<b>Sequential Clava-JS + Java commands:</b> {median_global:+.3f} s '
                        '(paired sum; suite directions can differ).')
    return " ".join(snippets)


def command_results_section(run_pairs_by_suite: dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
                            global_run_pairs: dict[tuple[str, str], list[dict[str, Any]]],
                            runs_grouped: dict[tuple[str, str], list[dict[str, Any]]],
                            parse_pairs: dict[tuple[str, str], dict[str, list[dict[str, Any]]]]) -> str:
    policy_order = {"normal": 0, "disabled": 1}
    dimensions = sorted(run_pairs_by_suite,
                        key=lambda dimension: (policy_order.get(dimension[0], 2), dimension[1]))
    if not dimensions:
        return ""
    output = ['<section class="headline-results"><h2>Full suite-command result</h2>'
              '<p class="chart-note">Each point is a matched full-command difference: Protobuf minus Text + ccache. '
              'These wall times include caller-side heap logging and explicit GC; they are not sums of parse-event times. '
              'The combined Clava-JS + Java row adds only commands measured within the same sequential pair group.</p>']
    for policy, cache_mode in dimensions:
        by_suite = run_pairs_by_suite[(policy, cache_mode)]
        global_pairs = global_run_pairs.get((policy, cache_mode), [])
        rows = runs_grouped.get((policy, cache_mode), [])
        title = "Explicit GC disabled" if policy == "disabled" else "Explicit GC allowed" if policy == "normal" else policy
        output.append(f'<section class="headline-condition"><h3>{esc(title)} · {esc(cache_mode)} cache</h3>')
        output.append(f'<p class="headline-conclusion">{command_conclusion(by_suite, global_pairs)}</p>')
        output.append(run_wall_table(rows, by_suite, global_pairs))
        output.append(svg_delta_chart(
            f'Paired full-command wall-time difference — {title}',
            wall_chart_rows(by_suite, global_pairs),
            metric_label="command pair/block",
        ))
        output.append('</section>')

    normal_dim = next((dimension for dimension in dimensions if dimension[0] == "normal"), None)
    disabled_dim = next((dimension for dimension in dimensions if dimension[0] == "disabled"), None)
    if normal_dim and disabled_dim:
        normal_java = {pair["repeat"]: pair["delta_s"]
                       for pair in run_pairs_by_suite[normal_dim].get("java", [])}
        disabled_java = {pair["repeat"]: pair["delta_s"]
                         for pair in run_pairs_by_suite[disabled_dim].get("java", [])}
        common = sorted(set(normal_java) & set(disabled_java))
        if common:
            did = statistics.median(normal_java[repeat] - disabled_java[repeat] for repeat in common)
            output.append(
                f'<p class="pairing-note"><b>Java GC-policy contrast:</b> the median of {len(common)} '
                f'within-repeat differences in the protocol gap is {did:+.3f} s '
                '(normal-policy Proto−Text gap minus GC-disabled Proto−Text gap). '
                'This is paired by repeat, not the subtraction of the two condition medians. '
                'It describes this warm-ccache instrumented workload and is not the prior direct-bypass result.</p>'
            )
        normal_parse = [pair["delta_ms"] for pair in parse_pairs.get(normal_dim, {}).get("java", [])]
        disabled_parse = [pair["delta_ms"] for pair in parse_pairs.get(disabled_dim, {}).get("java", [])]
        normal_gap = [pair["delta_s"] for pair in run_pairs_by_suite[normal_dim].get("java", [])]
        disabled_gap = [pair["delta_s"] for pair in run_pairs_by_suite[disabled_dim].get("java", [])]
        if normal_parse and disabled_parse and normal_gap and disabled_gap:
            output.append(
                '<p class="boundary-callout"><b>Java timing-boundary check:</b> the full-command gap is '
                f'{statistics.median(normal_gap):+.3f} s with explicit GC allowed and '
                f'{statistics.median(disabled_gap):+.3f} s with it disabled, while the paired per-invocation '
                f'median is {statistics.median(normal_parse):+.3f} ms and '
                f'{statistics.median(disabled_parse):+.3f} ms, respectively. The opposing directions show that the '
                'per-call median alone does not track the command-level shift. Caller-side heap logging and GC are included in '
                'command wall time but excluded from parse-call latency. It does not quantify GC pauses or claim '
                'the earlier direct-bypass result applies unchanged to this warm-ccache workload.</p>'
            )
    output.append('</section>')
    return "".join(output)


def report_html(path: Path, valid_rows: list[dict[str, Any]],
                invalid_rows: list[dict[str, Any]],
                run_rows: list[dict[str, Any]] | None = None,
                invalid_runs: list[dict[str, Any]] | None = None,
                plan: dict[str, Any] | None = None) -> str:
    grouped = group_rows(valid_rows)
    pairs, unmatched, unpairable = pair_rows(valid_rows)
    run_rows = run_rows or []
    invalid_runs = invalid_runs or []
    run_pairs_by_suite, global_run_pairs, unmatched_run_pairs = pair_run_rows(run_rows)
    runs_grouped: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in run_rows:
        runs_grouped.setdefault((row["gc_policy"], row["cache_mode"]), []).append(row)
    headline_sections: list[str] = []
    if run_rows:
        headline_sections.append(measurement_panel(run_rows, plan, run_pairs_by_suite))
        headline_sections.append(command_results_section(
            run_pairs_by_suite, global_run_pairs, runs_grouped, pairs))
    sections: list[str] = []
    for policy, cache_mode in sorted(grouped):
        dimension = (policy, cache_mode)
        raw_by_suite = grouped[dimension]
        pairs_by_suite = pairs.get(dimension, {})
        policy_title = f"GC policy: {policy}, cache mode: {cache_mode}"
        sections.append(f'<section class="policy"><h2>{esc(policy_title)}</h2>')
        sections.append('<p class="policy-note">This panel contains only rows with this GC policy and cache mode. The pooled row combines individual parse samples from the listed suites. It is not total suite runtime or a global speedup.</p>')
        sections.append(summary_table(policy, raw_by_suite, pairs_by_suite))
        for suite in SUITES:
            if suite not in raw_by_suite:
                continue
            suite_title = f'{SUITES[suite]}, {policy}'
            suite_rows = raw_by_suite[suite]
            sections.append(f'<section class="chart-section"><h3>{esc(SUITES[suite])} individual parses</h3>')
            sections.append(
                f'<p class="chart-note">{esc(cache_state_summary(suite_rows))} '
                'The flag describes whether the AST-cache adapter was active for an event, not a cache hit. '
                'Text + ccache and Protobuf use the same time axis. Candle statistics use every valid event, including '
                'ineligible parses; dots are a deterministic sample of up to 250 per protocol. '
                'Outlined sample dots lie outside the 1.5-IQR whiskers; exact per-event measurements remain in the CSV.</p>'
            )
            sections.append(
                f'<p class="pairing-note"><b>Pairability:</b> '
                f'{esc(pair_eligibility_summary(suite_rows))}</p>'
            )
            sections.append(svg_distribution_chart(suite_title, {
                protocol: [row for row in suite_rows if row["protocol"] == protocol]
                for protocol in PROTOCOL_ORDER
            }))
            missing_pairs = unmatched.get((policy, cache_mode, suite), 0)
            unavailable_pairs = unpairable.get((policy, cache_mode, suite), 0)
            suite_pairs = pairs_by_suite.get(suite, [])
            sections.append(
                f'<p class="chart-note">Matched invocations: {len(suite_pairs)}. '
                f'Pairable keys missing a counterpart: {missing_pairs}. '
                f'Parse events excluded from pairing (<code>pair_available=false</code> or missing key): '
                f'{unavailable_pairs}. {esc(invalid_summary(invalid_rows, policy, cache_mode, suite))}</p>'
            )
            sections.append('<h3>Largest per-source changes across repeats</h3>')
            sections.append('<p class="chart-note">For each stable source/config key, deltas are first summarized by their median across repeats. Positive means Protobuf took longer; the table lists up to five largest changes in each direction.</p>')
            sections.append(source_change_table(suite_pairs))
            sections.append('</section>')

        all_rows = [row for rows in raw_by_suite.values() for row in rows]
        sections.append('<section class="chart-section"><h3>Pooled per-parse distribution</h3>')
        sections.append('<p class="chart-note">All valid Clava-JS and Java parse invocations under this GC policy share one time axis. Each invocation counts once. This pooled candle-only view computes all statistics from the complete data; individual dots are omitted to avoid repeating points already shown in the suite charts.</p>')
        sections.append(svg_distribution_chart(f'Pooled per-parse distribution for {policy}', {
            protocol: [row for row in all_rows if row["protocol"] == protocol]
            for protocol in PROTOCOL_ORDER
        }, show_points=False))
        pooled_pairs = [pair for rows in pairs_by_suite.values() for pair in rows]
        delta_groups = [(SUITES[suite], pairs_by_suite.get(suite, [])) for suite in SUITES
                        if pairs_by_suite.get(suite)]
        delta_groups.append(("Pooled per-parse", pooled_pairs))
        sections.append('<section class="chart-section"><h3>Matched per-parse differences</h3>')
        sections.append('<p class="chart-note">Positive means Protobuf took longer. Candles and axis limits use all matched invocations. The suite rows show a deterministic sample of up to 250 dots each; the pooled row is candle-only to avoid duplicating those dots, and is not a whole-suite speed difference.</p>')
        sections.append(svg_delta_chart(f'Paired per-parse difference for {policy}', delta_groups,
                                        metric_label="source invocation",
                                        point_groups={SUITES[suite] for suite in SUITES}))
        if delta_chart_needs_central_scale(delta_groups):
            sections.append('<h3>Central-scale view of paired parse differences</h3>')
            sections.append('<p class="chart-note">This candle-only linear view spans the shared non-outlier whisker range across suites. Every candle and count uses all paired rows; observations beyond this view are counted beside each group and the full-range chart above shows the complete range with sampled dots.</p>')
            sections.append(svg_delta_chart(
                f'Central-scale paired per-parse difference for {policy}',
                delta_groups,
                metric_label="source invocation",
                central_scale=True,
                show_points=False,
            ))
        sections.append(f'<p class="chart-note">{esc(invalid_summary(invalid_rows, policy, cache_mode))}</p></section>')

        if run_rows:
            wall_rows = runs_grouped.get(dimension, [])
            wall_unmatched = unmatched_run_pairs.get(dimension, 0)
            sections.append('<details class="run-audit"><summary>Run-level ccache audit</summary>')
            sections.append('<p class="chart-note">ccache totals below come from command-run counters. They describe run-level cache activity and are not assigned to individual parse points.</p>')
            sections.append(ccache_counter_table(wall_rows))
            sections.append(f'<p class="chart-note">Unmatched or incomplete command groups excluded from paired summaries: {wall_unmatched}. {esc(invalid_run_summary(invalid_runs, policy, cache_mode))}</p></details>')
        sections.append('</section>')

    invalid_count = len(invalid_rows) + len(invalid_runs)
    timing_boundary = valid_rows[0]["timing_boundary"]
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Per-parse protocol comparison</title>
<style>
:root {{ color-scheme: light; --surface:#fff; --ink:#172033; --muted:#526174; --line:#d8dee8; --grid:#e5eaf1; --outlier:#b91c1c; --soft:#f4f7fb; }}
html.dark {{ color-scheme: dark; --surface:#111827; --ink:#e5e7eb; --muted:#aab5c5; --line:#374151; --grid:#273244; --outlier:#f87171; --soft:#182334; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; background:var(--surface); color:var(--ink); font:16px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif; }}
main {{ max-width:1320px; margin:0 auto; padding:32px 24px 64px; }}
h1 {{ margin:0 0 8px; font-size:clamp(1.7rem,3vw,2.4rem); }}
h2 {{ margin:38px 0 8px; padding-top:24px; border-top:1px solid var(--line); font-size:1.55rem; }}
h3 {{ margin:28px 0 4px; font-size:1.2rem; }}
p {{ margin:8px 0 14px; }}
.lede,.policy-note,.chart-note {{ color:var(--muted); }}
.lede {{ max-width:1000px; }}
.policy {{ margin-top:36px; }}
.chart-section {{ margin:24px 0 34px; }}
.table-wrap {{ overflow-x:auto; margin:20px 0 28px; border:1px solid var(--line); border-radius:10px; }}
table {{ width:100%; border-collapse:collapse; min-width:920px; }}
.compact-table {{ min-width:500px; }}
.top-delta-grid {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }}
.top-delta-grid h4 {{ margin:8px 0; }}
.pairing-note {{ padding:12px 14px; border-left:4px solid #b45309; background:var(--soft); border-radius:4px; }}
th,td {{ padding:11px 13px; border-bottom:1px solid var(--line); text-align:left; vertical-align:top; }}
thead th {{ background:var(--soft); font-size:.88rem; }}
tbody tr:last-child th,tbody tr:last-child td {{ border-bottom:0; }}
.pooled {{ background:var(--soft); }}
td small {{ color:var(--muted); }}
svg {{ display:block; width:100%; height:auto; overflow:visible; }}
.grid-line {{ stroke:var(--grid); stroke-width:1; }}
.zero-line {{ stroke:var(--muted); stroke-width:1.6; stroke-dasharray:5 4; }}
.axis-text {{ fill:var(--muted); font-size:12px; }}
.axis-title {{ fill:var(--muted); font-size:13px; }}
.condition-text {{ fill:var(--ink); font-size:14px; font-weight:650; }}
.sample-text {{ fill:var(--ink); font-size:13px; font-weight:650; }}
.detail-text {{ fill:var(--muted); font-size:11px; }}
.point {{ opacity:.7; stroke:var(--surface); stroke-width:1; }}
.point.outlier {{ opacity:1; stroke:var(--outlier); stroke-width:2; }}
.delta-whisker {{ stroke:#475569; stroke-width:2; }}
.delta-box {{ fill:#64748b; fill-opacity:.18; stroke:#475569; stroke-width:1.5; }}
.delta-median {{ stroke:#334155; stroke-width:4; }}
.empty {{ color:var(--muted); font-style:italic; }}
.measurement-panel,.headline-results {{ margin:20px 0 32px; padding:20px; border:1px solid var(--line); border-radius:14px; background:var(--soft); }}
.measurement-panel h2,.headline-results h2 {{ margin:0 0 8px; padding:0; border:0; }}
.headline-condition {{ margin:22px 0 32px; }}
.headline-condition h3 {{ margin-top:14px; }}
.headline-conclusion {{ font-size:1.06rem; line-height:1.75; }}
.boundary-callout {{ padding:14px 16px; border-left:4px solid #6d28d9; background:var(--soft); border-radius:4px; }}
.provenance,.run-audit {{ margin:16px 0 0; }}
.provenance summary,.run-audit summary {{ cursor:pointer; color:var(--muted); }}
@media (max-width:720px) {{ main {{ padding:22px 12px 44px; }} .chart-section {{ overflow-x:auto; }} .chart-section svg {{ min-width:820px; }} .top-delta-grid {{ grid-template-columns:1fr; }} }}
</style>
</head>
<body><main>
<h1>Per-parse protocol comparison</h1>
{''.join(headline_sections)}
<p class="lede">Input: <code>{esc(path.name)}</code>. The CSV records the timer boundary as <code>{esc(timing_boundary)}</code>. These values are parser-call latency, not full test-command wall time or explicit-GC pause measurements. The CSV retains every valid invocation; charts label when dots are sampled or omitted.</p>
<p class="lede">All candle statistics, whiskers, medians, IQRs, outlier counts, and axis ranges use the full valid dataset. Where displayed, dots are deterministic, range-spanning samples and do not determine the candles. Pooled runtime and central-scale delta views are candle-only.</p>
<p class="lede">Per-parse delta is Protobuf minus Text + ccache. Positive values mean Protobuf took longer. Pairing uses GC policy, cache mode, suite, pair ID, repeat, and source identity. Pooled rows give every parse event equal weight and do not estimate suite-level or global wall-time speedup. Excluded parse or command rows, including invalid, incomplete, and seed rows: {invalid_count}.</p>
<p class="lede">Raw runtime charts retain all valid parse events, including events where <code>cache_enabled</code> is false. That field describes event-level cache-adapter state, not a per-file hit; ccache hits and misses are shown only as command-level counters when <code>runs.csv</code> is supplied. The effective explicit-GC setting is checked against each row's GC policy when recorded.</p>
{''.join(sections)}
</main></body></html>'''


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-csv", required=True, type=Path,
                        help="per-parse CSV emitted by the benchmark harness")
    parser.add_argument("--runs-csv", type=Path,
                        help="valid measured run records; required if parse rows lack a phase/measured marker, and adds full-suite wall-time summaries")
    parser.add_argument("--plan-json", type=Path,
                        help="optional run plan with workload counts, timer boundaries, and source/artifact fingerprints")
    parser.add_argument("--output", required=True, type=Path,
                        help="standalone HTML report path")
    args = parser.parse_args(argv)
    try:
        run_rows, invalid_runs = read_run_csv(args.runs_csv) if args.runs_csv else ([], [])
        plan = None
        if args.plan_json:
            with args.plan_json.open(encoding="utf-8") as source:
                plan = json.load(source)
        measured_run_ids = {row["run_id"] for row in run_rows if row["run_id"]}
        valid_rows, invalid_rows = read_csv(
            args.input_csv,
            measured_run_ids if args.runs_csv else None,
        )
        html_text = report_html(args.input_csv, valid_rows, invalid_rows, run_rows, invalid_runs, plan)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(html_text, encoding="utf-8")
    except (OSError, ValueError, csv.Error, json.JSONDecodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    print(
        f"Wrote {args.output} from {len(valid_rows)} valid parse rows and "
        f"{len(run_rows)} valid command rows; excluded {len(invalid_rows) + len(invalid_runs)} invalid rows."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
