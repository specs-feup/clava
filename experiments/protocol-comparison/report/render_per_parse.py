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
CHART_CONDITIONS = {
    "text-normal": ("Text + ccache · explicit GC on", "#059669"),
    "protobuf-normal": ("Protobuf · explicit GC on", "#7c3aed"),
    "text-disabled": ("Text + ccache · explicit GC off", "#059669"),
    "protobuf-disabled": ("Protobuf · explicit GC off", "#7c3aed"),
}
CHART_CONDITION_ORDER = tuple(CHART_CONDITIONS)
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


def compact_ticks(ticks: list[float], limit: int = 5) -> list[float]:
    """Keep axis labels legible on narrow charts without changing their domain."""
    if len(ticks) <= limit:
        return ticks
    indexes = {round(index * (len(ticks) - 1) / (limit - 1)) for index in range(limit)}
    selected = {ticks[index] for index in indexes}
    if 0.0 in ticks and 0.0 not in selected:
        selected.remove(ticks[min(indexes, key=lambda index: abs(ticks[index]))])
        selected.add(0.0)
    return sorted(selected)


def spaced_ticks(ticks, position, label):
    """Drop colliding mobile labels, preserving zero and endpoints first."""
    if not ticks:
        return []
    selected, intervals = [], []
    priorities = [tick for tick in ticks if tick == 0] + [ticks[0], ticks[-1]] + list(ticks)
    for tick in priorities:
        if tick in selected:
            continue
        half_width = len(label(tick)) * 5.0
        left, right = position(tick) - half_width, position(tick) + half_width
        if any(left < end + 8 and right > start - 8 for start, end in intervals):
            continue
        selected.append(tick)
        intervals.append((left, right))
    return sorted(selected)


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
                          show_points: bool = True,
                          protocol_order: tuple[str, ...] | list[str] | None = None,
                          mobile: bool = False) -> str:
    order = tuple(protocol_order or PROTOCOL_ORDER)
    entries = [(protocol, row) for protocol in order for row in by_protocol.get(protocol, [])]
    values = [row["elapsed_ms"] for _, row in entries]
    if not values:
        return '<p class="empty">No valid parse measurements for this group.</p>'
    axis = duration_axis(values)
    scale, unit = axis["unit_scale"], axis["unit"]
    fmt_time = lambda value: format_time_ms(value, scale)
    width = 380 if mobile else 1160
    height = max(250, 112 + 67 * len(order)) if mobile else max(264, 264 + 94 * (len(order) - 2))
    left, right = (48, 360) if mobile else (235, 1138)
    plot_top, plot_bottom = (34, height - 34) if mobile else (35, height - 36)
    x = lambda value: left + (right - left) * (
        axis["transform"](value) - axis["low_position"]
    ) / (axis["high_position"] - axis["low_position"])
    ticks = compact_ticks(axis["ticks"]) if mobile else axis["ticks"]
    if mobile:
        ticks = spaced_ticks(ticks, x, lambda value: format_axis(value, scale, unit))
    svg = [
        f'<svg class="chart-svg {"chart-svg-mobile" if mobile else "chart-svg-wide"}" viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)} individual parse latency distributions">',
        f'<title>{esc(title)} individual parse latency distributions</title>',
        '<desc>Candles and whiskers summarize every valid measured parse event using the 1.5 interquartile-range rule. '
        + (f'Dots are a deterministic display sample, at most {max_points_per_protocol} per protocol; '
           'they do not determine the candle statistics.' if show_points
           else 'Dots are omitted in this pooled summary; candles use every parse event.') + '</desc>',
    ]
    for tick in ticks:
        tx = x(tick)
        svg.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="{plot_top}" y2="{plot_bottom}" class="grid-line"/>')
        svg.append(f'<text x="{tx:.2f}" y="25" text-anchor="middle" class="{"mobile-axis-text" if mobile else "axis-text"}">{esc(format_axis(tick, scale, unit))}</text>')
    for index, protocol in enumerate(order):
        rows = by_protocol.get(protocol, [])
        if not rows:
            continue
        if index and '/' in protocol and protocol.split('/')[0] != order[index - 1].split('/')[0]:
            divider_y = (76 + index * 67 - 42) if mobile else (93 + index * 94 - 47)
            svg.append(f'<line x1="12" x2="{right}" y1="{divider_y}" y2="{divider_y}" class="grid-line"/>')
        values_for_protocol = [row["elapsed_ms"] for row in rows]
        stats = distribution(values_for_protocol)
        y = (76 + index * 67) if mobile else (93 + index * 94)
        label, color = CHART_CONDITIONS.get(protocol, PROTOCOLS.get(protocol, (protocol, "#475569")))
        if mobile:
            svg.append(f'<text x="12" y="{y - 24}" class="mobile-condition-text">{esc(label)}</text>')
        else:
            svg.append(f'<text x="14" y="{y + 5}" class="condition-text">{esc(label)}</text>')
        dash = ' stroke-dasharray="5 4"' if protocol.endswith("-disabled") else ""
        svg.append(f'<line x1="{x(stats["whisker_minimum"]):.2f}" x2="{x(stats["whisker_maximum"]):.2f}" y1="{y}" y2="{y}" stroke="{color}" stroke-width="2"{dash}/>')
        for end in (stats["whisker_minimum"], stats["whisker_maximum"]):
            svg.append(f'<line x1="{x(end):.2f}" x2="{x(end):.2f}" y1="{y - 10}" y2="{y + 10}" stroke="{color}" stroke-width="2"/>')
        box_width = max(3.0, x(stats["q3"]) - x(stats["q1"]))
        svg.append(f'<rect x="{x(stats["q1"]):.2f}" y="{y - 16}" width="{box_width:.2f}" height="32" rx="3" fill="{color}" fill-opacity=".22" stroke="{color}" stroke-width="1.6"><title>Q1 {esc(fmt_time(stats["q1"]))}, median {esc(fmt_time(stats["median"]))}, Q3 {esc(fmt_time(stats["q3"]))}</title></rect>')
        svg.append(f'<line x1="{x(stats["median"]):.2f}" x2="{x(stats["median"]):.2f}" y1="{y - 18}" y2="{y + 18}" stroke="{color}" stroke-width="4"/>')
        plotted_rows = (display_sample(rows, "elapsed_ms", max_points_per_protocol)
                        if show_points else [])
        for point_index, row in enumerate(plotted_rows):
            value = row["elapsed_ms"]
            jitter = ((point_index * 37 + len(row.get("source_identity", ""))) % 11 - 5) * (2.2 if mobile else 3.3)
            klass = ("point outlier" if value < stats["whisker_minimum"]
                     or value > stats["whisker_maximum"] else "point")
            svg.append(f'<circle cx="{x(value):.2f}" cy="{y + jitter:.2f}" r="{4 if mobile else 3.5}" class="{klass}" fill="{color}"/>')
    axis_y = height - 6 if mobile else height - 8
    svg.append(f'<text x="{(left + right) / 2:.1f}" y="{axis_y}" text-anchor="middle" class="{"mobile-axis-title" if mobile else "axis-title"}">Parse latency ({unit}, {esc(axis["label"])})</text>')
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
                    point_groups: set[str] | None = None,
                    mobile: bool = False) -> str:
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
    width = 380 if mobile else 1160
    height = (118 + 75 * len(available)) if mobile else (104 + 82 * len(available))
    left, right = (50, 360) if mobile else (235, 1138)
    x = lambda value: left + (right - left) * (value - low) / (high - low)
    ticks = compact_ticks(nice_ticks(low, high)) if mobile else nice_ticks(low, high)
    if mobile:
        ticks = spaced_ticks(ticks, x, lambda value: format_axis(value, scale, unit))
    plot_bottom = height - (32 if mobile else 32)
    svg = [
        f'<svg class="chart-svg {"chart-svg-mobile" if mobile else "chart-svg-wide"}" viewBox="0 0 {width} {height}" role="img" aria-label="{esc(title)} paired {esc(metric_label)} differences">',
        f'<title>{esc(title)} paired {esc(metric_label)} differences</title>',
        f'<desc>The paired delta is Protobuf minus Text. Positive values mean Protobuf took longer. '
        'Candle and whisker statistics use every matched invocation. '
        + (f'Dots are a deterministic display sample, at most {max_points_per_group} per group; '
           'they do not determine the candle statistics.' if show_points
           else 'Dots are omitted in this candle summary; all matched invocations determine the statistics.')
        + (" This central-scale view counts observations outside its linear axis; the full-range chart preserves the complete range." if central_scale else " The axis spans the full observed range.")
        + "</desc>",
    ]
    for tick in ticks:
        tx = x(tick)
        klass = "zero-line" if abs(tick) < 1e-9 else "grid-line"
        svg.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="34" y2="{plot_bottom}" class="{klass}"/>')
        svg.append(f'<text x="{tx:.2f}" y="25" text-anchor="middle" class="{"mobile-axis-text" if mobile else "axis-text"}">{esc(format_axis(tick, scale, unit))}</text>')
    for index, (name, rows) in enumerate(available):
        values_for_group = [pair["delta_ms"] for pair in rows]
        stats = stats_by_group[name]
        y = (78 + index * 75) if mobile else (75 + index * 82)
        if mobile:
            svg.append(f'<text x="12" y="{y - 25}" class="mobile-condition-text">{esc(name)}</text>')
        else:
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
            jitter = ((point_index * 37 + len(pair["identity"])) % 11 - 5) * (2.2 if mobile else 3.0)
            klass = ("point outlier" if delta < stats["whisker_minimum"]
                     or delta > stats["whisker_maximum"] else "point")
            svg.append(f'<circle cx="{x(delta):.2f}" cy="{y + jitter:.2f}" r="{4 if mobile else 3.5}" class="delta-point {klass}"/>')
    axis_label = "central linear scale" if central_scale else "full linear range"
    axis_title = f"Protobuf − Text ({unit})" if mobile else f"Protobuf − Text per {metric_label} ({unit}, {axis_label})"
    svg.append(f'<text x="{(left + right) / 2:.1f}" y="{height - 7}" text-anchor="middle" class="{"mobile-axis-title" if mobile else "axis-title"}">{esc(axis_title)}</text>')
    svg.append('</svg>')
    return "".join(svg)


def responsive_chart(wide_svg: str, mobile_svg: str, stats_html: str) -> str:
    return (
        '<div class="responsive-chart"><div class="chart-wide">' + wide_svg + '</div>'
        '<div class="chart-mobile">' + mobile_svg + '</div>' + stats_html + '</div>'
    )


def distribution_chart_stats(by_condition: dict[str, list[dict[str, Any]]],
                             order: tuple[str, ...] | list[str],
                             max_points: int = 250,
                             show_points: bool = True) -> str:
    all_values = [row["elapsed_ms"] for key in order for row in by_condition.get(key, [])]
    scale, _unit = axis_unit(all_values)
    items: list[str] = []
    for key in order:
        rows = by_condition.get(key, [])
        if not rows:
            continue
        summary = distribution([row["elapsed_ms"] for row in rows])
        plotted = len(display_sample(rows, "elapsed_ms", max_points)) if show_points else 0
        label, color = CHART_CONDITIONS.get(key, PROTOCOLS.get(key, (key, "#475569")))
        points = f"{plotted}/{summary['n']} points" if show_points else "points omitted"
        items.append(
            f'<li><span class="swatch" style="background:{color}"></span><b>{esc(label)}</b> '
            f'<span>median {format_time_ms(summary["median"], scale)} · n={summary["n"]}</span>'
            f'<details><summary>Distribution details</summary><p>IQR '
            f'{format_time_ms(summary["q3"] - summary["q1"], scale)} · '
            f'{len(summary["outliers"])} outliers · {points}</p></details></li>'
        )
    return '<ul class="chart-stats">' + "".join(items) + '</ul>'


def delta_chart_stats(by_group: list[tuple[str, list[dict[str, Any]]]],
                      central_scale: bool = False,
                      max_points: int = 250,
                      show_points: bool = True,
                      point_groups: set[str] | None = None) -> str:
    values = [pair["delta_ms"] for _, rows in by_group for pair in rows]
    scale, _unit = axis_unit(values)
    if central_scale:
        summaries = [distribution([pair["delta_ms"] for pair in rows])
                     for _, rows in by_group if rows]
        low, high = padded_domain([
            min(summary["whisker_minimum"] for summary in summaries),
            max(summary["whisker_maximum"] for summary in summaries),
        ], include_zero=True)
    else:
        low = high = 0.0
    items: list[str] = []
    for label, rows in by_group:
        if not rows:
            continue
        summary = distribution([pair["delta_ms"] for pair in rows])
        draw_points = show_points and (point_groups is None or label in point_groups)
        plotted = len(display_sample(rows, "delta_ms", max_points)) if draw_points else 0
        point_note = f"{plotted}/{summary['n']} points" if draw_points else "points omitted"
        off_scale = sum(not low <= pair["delta_ms"] <= high for pair in rows) if central_scale else 0
        off_scale_note = f" · {off_scale} off scale" if central_scale else ""
        items.append(
            f'<li><b>{esc(label)}</b> <span>median {format_delta_ms(summary["median"], scale)} · '
            f'n={summary["n"]}</span><details><summary>Distribution details</summary><p>IQR '
            f'{format_delta_ms(summary["q3"] - summary["q1"], scale)} · '
            f'{len(summary["outliers"])} outliers · {point_note}{off_scale_note}</p></details></li>'
        )
    return '<ul class="chart-stats">' + "".join(items) + '</ul>'


def distribution_chart_html(title: str, by_condition: dict[str, list[dict[str, Any]]],
                            order: tuple[str, ...] | list[str],
                            max_points: int = 250,
                            show_points: bool = True) -> str:
    return responsive_chart(
        svg_distribution_chart(title, by_condition, max_points, show_points, order),
        svg_distribution_chart(title, by_condition, max_points, show_points, order, mobile=True),
        distribution_chart_stats(by_condition, order, max_points, show_points),
    )


def delta_chart_html(title: str, by_group: list[tuple[str, list[dict[str, Any]]]],
                     metric_label: str = "source invocation",
                     central_scale: bool = False,
                     max_points: int = 250,
                     show_points: bool = True,
                     point_groups: set[str] | None = None) -> str:
    return responsive_chart(
        svg_delta_chart(title, by_group, metric_label, central_scale, max_points,
                        show_points, point_groups),
        svg_delta_chart(title, by_group, metric_label, central_scale, max_points,
                        show_points, point_groups, mobile=True),
        delta_chart_stats(by_group, central_scale, max_points, show_points, point_groups),
    )


def comparative_delta_chart_html(title: str, by_group: list[tuple[str, list[dict[str, Any]]]],
                                 metric_label: str) -> str:
    if not delta_chart_needs_central_scale(by_group):
        return delta_chart_html(title, by_group, metric_label=metric_label)
    return (
        '<p class="chart-note">All pairs determine the candles; outliers appear in the full-range view.</p>'
        '<h4>Typical paired changes</h4>'
        + delta_chart_html(title + " central scale", by_group, metric_label=metric_label,
                           central_scale=True, show_points=False)
        + '<details class="full-range-details"><summary>Full range, including outliers</summary>'
        + delta_chart_html(title + " full range", by_group, metric_label=metric_label)
        + '</details>'
    )


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
    suite_cards: list[str] = []
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
                label = "GC requests blocked" if policy == "disabled" else "GC requests allowed" if policy == "normal" else policy
                policy_repeats.append(f"{label}: {count_pairs}")
        repeats = ", ".join(policy_repeats) if policy_repeats else "not available"
        suite_cards.append(
            f'<article class="suite-glance"><h3>{esc(SUITES[suite])}</h3>'
            f'<p>{esc(total)} total; {esc(passed)} passed; {esc(skipped)} skipped</p>'
            f'<p>{esc(expected_events)} parse events per run</p>'
            f'<p>{esc(repeats)} matched command runs</p></article>'
        )

    warm = sorted({row["cache_mode"] for row in run_rows})
    cache_text = ", ".join(warm) if warm else str(plan.get("cache_mode", "not recorded"))
    parse_boundary = str(plan.get("parse_timing_boundary") or "not recorded")
    wall_boundary = str(plan.get("full_command_timing_boundary") or
                        "full elapsed time around each suite command")
    details = provenance_details(plan)
    return (
        '<section class="measurement-panel"><h2>Study at a glance</h2>'
        f'<p>Text + ccache vs Protobuf · {esc(cache_text)} ccache · '
        'paired full-command runs shown by GC policy.</p>'
        '<div class="suite-glance-grid">' + "".join(suite_cards) + '</div>'
        '<details class="method-details"><summary>Timer and measurement details</summary>'
        f'<p><b>Parse timer:</b> {esc(parse_boundary)}</p>'
        f'<p><b>Command timer:</b> {esc(wall_boundary)}. It includes caller-side heap logging and explicit GC; '
        'parse-call latency excludes them. The two measures are not added together.</p>'
        '<p>Per-parse charts retain valid events even when an event is not individually pairable. '
        'Paired deltas join only rows marked pairable by GC policy, cache mode, suite, pair ID, repeat, and source key. '
        'Command-level ccache counters are not assigned to parse events.</p>'
        '</details>' + details + '</section>'
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


def command_result_finding(
        run_pairs_by_suite: dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
        dimensions: list[tuple[str, str]]) -> str:
    medians = {
        (policy, suite): statistics.median(pair["delta_s"] for pair in by_suite.get(suite, []))
        for policy, cache_mode in dimensions
        for by_suite in [run_pairs_by_suite[(policy, cache_mode)]]
        for suite in SUITES if by_suite.get(suite)
    }
    findings = []
    clava = [medians[(policy, "clava-js")] for policy, _cache in dimensions
             if (policy, "clava-js") in medians]
    if clava and all(value > 0 for value in clava):
        findings.append("Clava-JS was slower with Protobuf in both conditions.")
    elif clava and all(value < 0 for value in clava):
        findings.append("Clava-JS was faster with Protobuf in both conditions.")
    normal_java = medians.get(("normal", "java"))
    disabled_java = medians.get(("disabled", "java"))
    if normal_java is not None and disabled_java is not None and normal_java * disabled_java < 0:
        findings.append("Java switched from slower to faster when explicit GC was turned off." if normal_java > 0 else "Java switched from faster to slower when explicit GC was turned off.")
    elif normal_java is not None and disabled_java is not None:
        direction = "slower" if disabled_java > 0 else "faster" if disabled_java < 0 else "tied"
        findings.append(f"With test-harness GC requests blocked, Protobuf was {direction} on Java.")
    return " ".join(findings)


def command_results_section(run_pairs_by_suite: dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
                            global_run_pairs: dict[tuple[str, str], list[dict[str, Any]]],
                            unmatched_run_pairs: dict[tuple[str, str], int],
                            runs_grouped: dict[tuple[str, str], list[dict[str, Any]]],
                            parse_pairs: dict[tuple[str, str], dict[str, list[dict[str, Any]]]],
                            invalid_runs: list[dict[str, Any]]) -> str:
    policy_order = {"normal": 0, "disabled": 1}
    dimensions = sorted(run_pairs_by_suite,
                        key=lambda dimension: (policy_order.get(dimension[0], 2), dimension[1]))
    if not dimensions:
        return ""
    cache_label = ", ".join(sorted({cache for _, cache in dimensions}))
    output = ['<section class="headline-results"><h2>Whole-suite runtime</h2>'
              f'<p class="chart-note">{esc(cache_label.capitalize())} cache · same revision. Positive means Protobuf was slower.</p>']
    finding = command_result_finding(run_pairs_by_suite, dimensions)
    if finding:
        output.append(f'<p class="overall-finding">{esc(finding)}</p>')
        output.append('<p class="gc-definition">Explicit GC is garbage collection requested by the tests. Automatic GC stays on.</p>')
    policy_label = {"normal": "Explicit GC on", "disabled": "Explicit GC off"}
    for suite in SUITES:
        suite_groups: list[tuple[str, list[dict[str, Any]]]] = []
        for policy, cache_mode in dimensions:
            pairs = run_pairs_by_suite[(policy, cache_mode)].get(suite, [])
            if not pairs:
                continue
            label = policy_label.get(policy, policy)
            chart_rows = [{
                "pair_id": pair["pair_id"], "repeat": pair["repeat"],
                "identity": f'{SUITES[suite]} command pair {pair["pair_id"]}',
                "delta_ms": pair["delta_s"] * 1000,
            } for pair in pairs]
            suite_groups.append((label, chart_rows))
        if not suite_groups:
            continue
        output.append(f'<article class="command-suite-result"><h3>{esc(SUITES[suite])}</h3>')
        output.append(comparative_delta_chart_html(
            f'Paired {SUITES[suite]} full-command differences', suite_groups, "command pair"))
        output.append('</article>')

    global_groups: list[tuple[str, list[dict[str, Any]]]] = []
    for policy, cache_mode in dimensions:
        pairs = global_run_pairs.get((policy, cache_mode), [])
        if not pairs:
            continue
        label = policy_label.get(policy, policy)
        global_groups.append((label, [{
            "pair_id": pair["pair_group_id"], "repeat": pair["repeat"],
            "identity": f'Sequential suite block {pair["pair_group_id"]}',
            "delta_ms": pair["delta_s"] * 1000,
        } for pair in pairs]))
    if global_groups:
        output.append('<article class="command-suite-result"><h3>Sequential Clava-JS + Java block</h3>'
                      '<p class="chart-note">Only commands measured in the same sequential pair group are combined.</p>')
        output.append(comparative_delta_chart_html(
            'Paired sequential suite-block command differences', global_groups,
            "sequential block"))
        output.append('</article>')

    output.append('<details class="command-details"><summary>Command medians, quartiles, and run audit</summary>')
    for policy, cache_mode in dimensions:
        title = policy_label.get(policy, policy)
        output.append(f'<h3>{esc(title)} · {esc(cache_mode)} cache</h3>')
        output.append(run_wall_table(
            runs_grouped.get((policy, cache_mode), []),
            run_pairs_by_suite[(policy, cache_mode)],
            global_run_pairs.get((policy, cache_mode), [])))
        output.append(f'<p>{command_conclusion(run_pairs_by_suite[(policy, cache_mode)], global_run_pairs.get((policy, cache_mode), []))}</p>')
        output.append(f'<p class="chart-note">Unmatched or incomplete command groups: '
                      f'{unmatched_run_pairs.get((policy, cache_mode), 0)}. '
                      f'{esc(invalid_run_summary(invalid_runs, policy, cache_mode))}</p>')
    output.append('</details>')

    interpretation: list[str] = []
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
            contrast = (
                f'<p class="pairing-note"><b>Java GC-policy contrast:</b> the median of {len(common)} '
                f'within-repeat differences in the protocol gap is {did:+.3f} s '
                '(normal-policy Proto−Text gap minus GC-disabled Proto−Text gap). '
                'This is paired by repeat, not the subtraction of the two condition medians. '
                'It describes this warm-ccache instrumented workload and is not the prior direct-bypass result.</p>'
            )
            interpretation.append(contrast)
        normal_parse = [pair["delta_ms"] for pair in parse_pairs.get(normal_dim, {}).get("java", [])]
        disabled_parse = [pair["delta_ms"] for pair in parse_pairs.get(disabled_dim, {}).get("java", [])]
        normal_gap = [pair["delta_s"] for pair in run_pairs_by_suite[normal_dim].get("java", [])]
        disabled_gap = [pair["delta_s"] for pair in run_pairs_by_suite[disabled_dim].get("java", [])]
        if normal_parse and disabled_parse and normal_gap and disabled_gap:
            boundary = (
                '<p class="boundary-callout"><b>Java timing-boundary check:</b> the full-command gap is '
                f'{statistics.median(normal_gap):+.3f} s with explicit GC allowed and '
                f'{statistics.median(disabled_gap):+.3f} s with it disabled, while the paired per-invocation '
                f'median is {statistics.median(normal_parse):+.3f} ms and '
                f'{statistics.median(disabled_parse):+.3f} ms, respectively. The opposing directions show that the '
                'per-call median alone does not track the command-level shift. Caller-side heap logging and GC are included in '
                'command wall time but excluded from parse-call latency. It does not quantify GC pauses or claim '
                'the earlier direct-bypass result applies unchanged to this warm-ccache workload.</p>'
            )
            interpretation.append(boundary)
    if interpretation:
        output.append('<details class="interpretation-details"><summary>GC contrast and timer interpretation</summary>')
        output.extend(interpretation)
        output.append('</details>')
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
        headline_sections.append(command_results_section(
            run_pairs_by_suite, global_run_pairs, unmatched_run_pairs, runs_grouped, pairs, invalid_runs))
        headline_sections.append(measurement_panel(run_rows, plan, run_pairs_by_suite))
    sections: list[str] = ['<section class="parser-results"><h2>Per-parse results</h2>'
                            '<p class="chart-note">Candles use every valid event; dots are sampled. Positive paired deltas mean Protobuf took longer.</p>']
    policy_order = {"normal": 0, "disabled": 1}
    dimensions = sorted(grouped, key=lambda dimension: (policy_order.get(dimension[0], 2), dimension[1]))
    cache_modes = sorted({cache_mode for _policy, cache_mode in dimensions})
    for suite in SUITES:
        for cache_mode in cache_modes:
            suite_dimensions = [dimension for dimension in dimensions
                                if dimension[1] == cache_mode and suite in grouped[dimension]]
            if not suite_dimensions:
                continue
            condition_rows: dict[str, list[dict[str, Any]]] = {}
            condition_order: list[str] = []
            delta_groups: list[tuple[str, list[dict[str, Any]]]] = []
            for policy, _mode in suite_dimensions:
                label = "Explicit GC on" if policy == "normal" else "Explicit GC off" if policy == "disabled" else policy
                for protocol in PROTOCOL_ORDER:
                    key = f"{protocol}-{policy}"
                    rows = [row for row in grouped[(policy, cache_mode)][suite]
                            if row["protocol"] == protocol]
                    if rows:
                        condition_rows[key] = rows
                        condition_order.append(key)
                delta_groups.append((label, pairs.get((policy, cache_mode), {}).get(suite, [])))

            sections.append(f'<article class="parse-suite-result"><h3>{esc(SUITES[suite])} · {esc(cache_mode)} cache</h3>')
            sections.append(distribution_chart_html(
                f'{SUITES[suite]} parser-call latency by protocol and GC policy',
                condition_rows, condition_order))
            if any(rows for _, rows in delta_groups):
                sections.append('<h4>Matched per-parse differences</h4>')
                sections.append(comparative_delta_chart_html(
                    f'{SUITES[suite]} matched parser-call differences', delta_groups,
                    "source invocation"))
            for policy, _mode in suite_dimensions:
                dimension = (policy, cache_mode)
                policy_title = "Explicit GC on" if policy == "normal" else "Explicit GC off" if policy == "disabled" else policy
                suite_rows = grouped[dimension][suite]
                suite_pairs = pairs.get(dimension, {}).get(suite, [])
                missing_pairs = unmatched.get((policy, cache_mode, suite), 0)
                unavailable_pairs = unpairable.get((policy, cache_mode, suite), 0)
                sections.append(f'<details class="evidence-details"><summary>{esc(policy_title)} pairing and source details</summary>')
                sections.append(f'<p>{esc(cache_state_summary(suite_rows))} Cache flags describe adapter eligibility, not hits. '
                                f'{esc(pair_eligibility_summary(suite_rows))}</p>')
                sections.append(f'<p>Matched invocations: {len(suite_pairs)}. Missing counterparts: {missing_pairs}. '
                                f'Events unavailable for pairing: {unavailable_pairs}. '
                                f'{esc(invalid_summary(invalid_rows, policy, cache_mode, suite))}</p>')
                sections.append('<h4>Largest per-source median changes across repeats</h4>')
                sections.append(source_change_table(suite_pairs))
                sections.append('</details>')
            sections.append('</article>')

    for policy, cache_mode in dimensions:
        dimension = (policy, cache_mode)
        raw_by_suite = grouped[dimension]
        pairs_by_suite = pairs.get(dimension, {})
        policy_title = "Explicit GC on" if policy == "normal" else "Explicit GC off" if policy == "disabled" else policy
        all_rows = [row for rows in raw_by_suite.values() for row in rows]
        pooled_rows = {protocol: [row for row in all_rows if row["protocol"] == protocol]
                       for protocol in PROTOCOL_ORDER}
        pooled_pairs = [pair for rows in pairs_by_suite.values() for pair in rows]
        delta_groups = [(SUITES[suite], pairs_by_suite.get(suite, [])) for suite in SUITES
                        if pairs_by_suite.get(suite)]
        delta_groups.append(("Pooled per-parse", pooled_pairs))
        sections.append(f'<details class="pooled-details"><summary>Pooled summaries · {esc(policy_title)}</summary>')
        sections.append(summary_table(policy, raw_by_suite, pairs_by_suite))
        sections.append('<h3>Pooled per-parse distribution</h3>')
        sections.append(distribution_chart_html(
            f'Pooled per-parse distribution for {policy_title}', pooled_rows,
            PROTOCOL_ORDER, show_points=False))
        sections.append('<h3>Matched per-parse differences</h3>')
        sections.append(delta_chart_html(
            f'Pooled and suite per-parse differences for {policy_title}', delta_groups,
            metric_label="source invocation", point_groups={SUITES[suite] for suite in SUITES}))
        if run_rows:
            wall_rows = runs_grouped.get(dimension, [])
            sections.append('<details class="run-audit"><summary>Run-level ccache counters</summary>')
            sections.append(ccache_counter_table(wall_rows))
            sections.append(f'<p>Incomplete command groups: {unmatched_run_pairs.get(dimension, 0)}. '
                            f'{esc(invalid_run_summary(invalid_runs, policy, cache_mode))}</p></details>')
        sections.append('</details>')
    sections.append('</section>')

    invalid_count = len(invalid_rows) + len(invalid_runs)
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Per-parse protocol comparison</title>
<style>
:root {{ color-scheme: light; --surface:#fff; --ink:#172033; --muted:#526174; --line:#d8dee8; --grid:#e5eaf1; --outlier:#b91c1c; --soft:#f4f7fb; --delta-ink:#475569; --delta-box:#64748b; }}
html.dark {{ color-scheme: dark; --surface:#111827; --ink:#e5e7eb; --muted:#aab5c5; --line:#374151; --grid:#273244; --outlier:#f87171; --soft:#182334; --delta-ink:#cbd5e1; --delta-box:#94a3b8; }}
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
.table-wrap {{ overflow:visible; margin:20px 0 28px; border:1px solid var(--line); border-radius:10px; }}
table {{ width:100%; border-collapse:collapse; table-layout:fixed; min-width:0; }}
.compact-table {{ min-width:0; }}
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
.mobile-axis-text,.mobile-axis-title,.mobile-condition-text {{ fill:var(--muted); font-size:18px; }}
.mobile-condition-text {{ fill:var(--ink); font-weight:650; }}
.condition-text {{ fill:var(--ink); font-size:14px; font-weight:650; }}
.sample-text {{ fill:var(--ink); font-size:13px; font-weight:650; }}
.detail-text {{ fill:var(--muted); font-size:11px; }}
.point {{ opacity:.7; stroke:var(--surface); stroke-width:1; }}
.point.outlier {{ opacity:1; stroke:var(--outlier); stroke-width:2; }}
.delta-whisker {{ stroke:var(--delta-ink); stroke-width:2; }}
.delta-box {{ fill:var(--delta-box); fill-opacity:.18; stroke:var(--delta-ink); stroke-width:1.5; }}
.delta-median {{ stroke:var(--delta-ink); stroke-width:4; }}
.delta-point {{ fill:var(--delta-ink); }}
.empty {{ color:var(--muted); font-style:italic; }}
.chart-mobile {{ display:none; }}
.chart-svg-mobile {{ width:min(100%,560px); height:auto; margin:0 auto; }}
.chart-stats {{ display:grid; gap:7px; list-style:none; margin:10px 0 18px; padding:0; }}
.chart-stats li {{ display:block; padding:8px 10px; border-bottom:1px solid var(--line); }}
.chart-stats li > span:not(.swatch) {{ display:block; }}
.chart-stats .swatch {{ display:inline-block; margin-right:5px; }}
.chart-stats details summary {{ color:var(--muted); font-size:.88rem; cursor:pointer; }}
.chart-stats details p {{ margin:3px 0 0; color:var(--muted); font-size:.9rem; }}
.swatch {{ width:11px; height:11px; border-radius:50%; align-self:center; }}
.suite-glance-grid {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }}
.suite-glance {{ padding:10px 12px; border:1px solid var(--line); border-radius:10px; background:var(--surface); }}
.suite-glance h3 {{ margin:0 0 4px; }}
.suite-glance p {{ margin:2px 0; }}
.command-suite-result,.parse-suite-result {{ margin:16px 0 28px; padding:14px; border:1px solid var(--line); border-radius:12px; }}
.command-suite-result h3,.parse-suite-result h3 {{ margin:0 0 4px; }}
.parse-suite-result h4 {{ margin:20px 0 4px; }}
.headline-conclusion {{ display:flex; flex-wrap:wrap; gap:4px 16px; margin:8px 0; }}
.overall-finding {{ margin:10px 0 3px; font-size:1.05rem; font-weight:650; }}
.gc-definition {{ margin:0 0 14px; color:var(--muted); font-size:.9rem; }}
summary {{ cursor:pointer; }}
code {{ overflow-wrap:anywhere; word-break:break-word; }}
.measurement-panel,.headline-results {{ margin:20px 0 32px; padding:20px; border:1px solid var(--line); border-radius:14px; background:var(--soft); }}
.measurement-panel h2,.headline-results h2 {{ margin:0 0 8px; padding:0; border:0; }}
.headline-condition {{ margin:22px 0 32px; }}
.headline-condition h3 {{ margin-top:14px; }}
.headline-conclusion {{ font-size:1.06rem; line-height:1.75; }}
.boundary-callout {{ padding:14px 16px; border-left:4px solid #6d28d9; background:var(--soft); border-radius:4px; }}
.provenance,.run-audit,.method-details,.evidence-details,.pooled-details,.command-details,.interpretation-details {{ margin:12px 0; }}
.provenance summary,.run-audit summary {{ cursor:pointer; color:var(--muted); }}
@media (max-width:1024px) {{
  main {{ padding:22px 12px 44px; }}
  .chart-wide {{ display:none; }}
  .chart-mobile {{ display:block; }}
  .chart-mobile .chart-svg-mobile {{ width:min(100%,560px); }}
  .chart-stats li {{ grid-template-columns:auto minmax(0,1fr); }}
  .table-wrap {{ overflow:visible; margin:12px 0 18px; border:0; }}
  .table-wrap table {{ width:100%; min-width:0; table-layout:fixed; }}
  .table-wrap th,.table-wrap td {{ padding:8px 6px; overflow-wrap:anywhere; }}
  .suite-glance-grid {{ grid-template-columns:1fr; }}
  .measurement-panel,.headline-results {{ padding:14px; }}
}}
@media (max-width:600px) {{
  table.mobile-cards, .mobile-cards tbody, .mobile-cards tr, .mobile-cards th, .mobile-cards td {{ display:block; width:100%; }}
  .mobile-cards thead {{ display:none; }}
  .mobile-cards tr {{ margin:10px 0; padding:8px 10px; border:1px solid var(--line); border-radius:8px; }}
  .mobile-cards th, .mobile-cards td {{ border:0; padding:4px 0; }}
  .mobile-cards [data-label]::before {{ content:attr(data-label); display:block; color:var(--muted); font-size:.8rem; font-weight:650; }}
}}
</style>
</head>
<body><main>
<h1>Per-parse protocol comparison</h1>
{''.join(headline_sections)}
{''.join(sections)}
<details class="run-audit"><summary>CSV inclusion audit</summary><p>Excluded parse or command rows, including invalid, incomplete, and seed rows: {invalid_count}. Input: <code>{esc(path.name)}</code>. Parser timing boundary: <code>{esc(valid_rows[0]['timing_boundary'])}</code>. Command wall time includes caller-side heap logging and explicit GC; the parse timer excludes them.</p></details>
</main><script>
document.querySelectorAll('main table').forEach(table => {{
  const headers = Array.from(table.querySelectorAll('thead th'), cell => cell.textContent.trim());
  if (!headers.length) return;
  table.classList.add('mobile-cards');
  table.querySelectorAll('tbody tr').forEach(row => {{
    Array.from(row.cells).forEach((cell, index) => cell.dataset.label = headers[index] || '');
  }});
}});
</script></body></html>'''


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
