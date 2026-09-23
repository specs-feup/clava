#!/usr/bin/env python3
"""Render standalone HTML from one or more protocol-comparison results.json files."""

from __future__ import annotations

import argparse
import html
import json
import math
from pathlib import Path
import statistics
import sys
from typing import Any


MODES = {
    "direct": "Direct (no ccache)",
    "cold": "Cold cache",
    "warm": "Warm cache",
}
MODE_ORDER = ("direct", "cold", "warm")
STAGES = {
    "before-cache": ("Before cache", "#2563eb"),
    "ccache-text": ("Text + ccache", "#059669"),
    "protobuf": ("Protobuf", "#7c3aed"),
    "flatbuffers": ("FlatBuffers eager", "#d97706"),
}
STAGE_ORDER = tuple(STAGES)
SUITES = {
    "clava-js": {
        "title": "Clava-JS",
        "tests": 164,
        "passed": 158,
        "skipped": 6,
        "description": "164 selected tests, including six host-dependent OpenMP/CUDA skips.",
    },
    "java": {
        "title": "Java parser",
        "tests": 116,
        "passed": 116,
        "skipped": 0,
        "description": "116 parser tests; resource, CUDA, cache-adapter, generated-root integration, and protocol-only tests are excluded.",
    },
}


def esc(value: Any) -> str:
    return html.escape(str(value), quote=True)


def number(value: Any) -> float | None:
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if math.isfinite(parsed) else None


def time_value(row: dict[str, Any]) -> float | None:
    for key in ("elapsed_s", "wall_time_s"):
        parsed = number(row.get(key))
        if parsed is not None and parsed >= 0:
            return parsed
    return None


def stage_title(key: str, info: dict[str, Any] | None = None) -> str:
    if key in STAGES:
        return STAGES[key][0]
    return str((info or {}).get("label") or key)


def load_inputs(paths: list[Path]) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]], list[str]]:
    manifests: list[dict[str, Any]] = []
    provenance: dict[str, dict[str, Any]] = {}
    warnings: list[str] = []
    for path in paths:
        value = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(value, dict) or not isinstance(value.get("results"), list):
            raise ValueError(f"{path} is not a results.json manifest with a results array")
        top_mode = value.get("mode")
        row_modes = {row.get("mode") for row in value["results"] if isinstance(row, dict)}
        if top_mode == "bypass" or "bypass" in row_modes:
            raise ValueError(
                f"{path} uses the legacy 'bypass' mode. Supply a new 'direct' manifest; "
                "legacy bypass results are not eligible for this comparison."
            )
        if top_mode not in (None, "all", *MODE_ORDER):
            raise ValueError(f"{path} has unsupported mode {top_mode!r}; expected direct, cold, or warm")
        manifests.append(value)
        for raw in value.get("stages", []):
            if not isinstance(raw, dict) or not raw.get("key"):
                continue
            key = str(raw["key"])
            per_stage = provenance.setdefault(key, {})
            declared_mode = top_mode if top_mode in MODE_ORDER else "all"
            if declared_mode in per_stage and per_stage[declared_mode] != raw:
                warnings.append(f"Conflicting provenance for {key} in {declared_mode} inputs.")
            per_stage[declared_mode] = raw

    # A stage revision should remain fixed across cache states. Keep each state's metadata
    # in the appendix and call out any drift instead of silently treating it as one build.
    for key, states in provenance.items():
        snapshots = [item for item in states.values() if isinstance(item, dict)]
        fields = ("clava_revision", "dumper_revision", "dumper_sha256")
        for field in fields:
            values = {item.get(field) for item in snapshots if item.get(field) is not None}
            if len(values) > 1:
                warnings.append(f"{key} has different {field} values across cache states.")
    return manifests, provenance, sorted(set(warnings))


def flatten_results(manifests: list[dict[str, Any]]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for manifest in manifests:
        for raw in manifest.get("results", []):
            if not isinstance(raw, dict):
                continue
            row = dict(raw)
            row.setdefault("mode", manifest.get("mode"))
            rows.append(row)
    return rows


def is_measured(row: dict[str, Any]) -> bool:
    if row.get("measured") is False:
        return False
    return row.get("measured") is True or row.get("repeat") is not None


def is_valid_run(row: dict[str, Any], suite: str) -> bool:
    spec = SUITES.get(suite)
    if spec is None or row.get("valid") is False:
        return False
    if row.get("return_code", row.get("exit_status")) != 0:
        return False
    expected = {
        "total_tests": spec["tests"],
        "passed_tests": spec["passed"],
        "failed_tests": 0,
        "skipped_tests": spec["skipped"],
    }
    if any(number(row.get(key)) != value for key, value in expected.items()):
        return False
    return time_value(row) is not None


def quantile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    position = (len(ordered) - 1) * fraction
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def fmt_seconds(value: float) -> str:
    if value < 10:
        return f"{value:.2f}s"
    if value < 100:
        return f"{value:.1f}s"
    return f"{value:.0f}s"


def chart_svg(
    suite: str,
    mode: str,
    rows: list[dict[str, Any]],
    provenance: dict[str, dict[str, Any]],
) -> str:
    points: dict[str, list[tuple[float, dict[str, Any]]]] = {key: [] for key in STAGE_ORDER}
    excluded: dict[str, int] = {key: 0 for key in STAGE_ORDER}
    for row in rows:
        if row.get("suite") != suite or row.get("stage") not in points or not is_measured(row):
            continue
        key = str(row["stage"])
        # The pre-cache checkout has no cache-state variants. Reuse its exact Direct
        # distribution as a neutral reference in the Cold and Warm panels.
        source_mode = "direct" if key == "before-cache" and mode in {"cold", "warm"} else mode
        if row.get("mode") != source_mode:
            continue
        if not is_valid_run(row, suite):
            excluded[key] += 1
            continue
        value = time_value(row)
        if value is not None:
            points[key].append((value, row))

    all_values = [value for group in points.values() for value, _ in group]
    if not all_values:
        return (
            '<div class="empty-chart">No eligible measured runs were supplied for this suite and cache state. '
            'Warm-up rows and runs that failed the suite validity checks are excluded.</div>'
        )

    width = 1020
    left = 250
    right = 748
    top = 45
    lane = 68
    height = top + lane * len(STAGE_ORDER) + 28
    max_value = max(all_values) * 1.08
    if max_value <= 0:
        max_value = 1.0

    def x(value: float) -> float:
        return left + (right - left) * value / max_value

    bits = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(SUITES[suite]["title"])} wall time distribution in {esc(MODES[mode])}" class="candle-chart">',
        f'<title>{esc(SUITES[suite]["title"])} wall time, {esc(MODES[mode])}</title>',
        '<desc>Each row is a tested repository state. Whiskers show the minimum and maximum, the box shows the first and third quartiles, the thick mark is the median, and dots are individual valid runs. Lower time is better.</desc>',
    ]
    tick_count = 4
    for index in range(tick_count + 1):
        tick = max_value * index / tick_count
        tx = x(tick)
        bits.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="22" y2="{height - 22}" class="grid-line"/>')
        bits.append(f'<text x="{tx:.2f}" y="17" text-anchor="middle" class="axis-text">{esc(fmt_seconds(tick))}</text>')

    for index, key in enumerate(STAGE_ORDER):
        y = top + index * lane + lane / 2
        is_reference = key == "before-cache" and mode in {"cold", "warm"}
        label = "Pre-cache reference, no cache state" if is_reference else stage_title(key, provenance.get(key, {}).get("all"))
        color = "#64748b" if is_reference else STAGES[key][1]
        bits.append(f'<text x="12" y="{y - 3:.2f}" class="stage-text">{esc(label)}</text>')
        values = [value for value, _ in points[key]]
        if not values:
            if key == "before-cache" and mode in {"cold", "warm"}:
                note = "No Direct pre-cache reference supplied"
            else:
                note = f"No valid runs; {excluded[key]} excluded" if excluded[key] else "No measured runs"
            bits.append(f'<text x="{left}" y="{y + 5:.2f}" class="empty-lane">{esc(note)}</text>')
            continue

        low = min(values)
        high = max(values)
        q1 = quantile(values, 0.25)
        median = statistics.median(values)
        q3 = quantile(values, 0.75)
        xlow, xhigh, xq1, xq3, xmed = (x(v) for v in (low, high, q1, q3, median))
        box_width = max(3, xq3 - xq1)
        bits.append(f'<line x1="{xlow:.2f}" x2="{xhigh:.2f}" y1="{y:.2f}" y2="{y:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<line x1="{xlow:.2f}" x2="{xlow:.2f}" y1="{y - 8:.2f}" y2="{y + 8:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<line x1="{xhigh:.2f}" x2="{xhigh:.2f}" y1="{y - 8:.2f}" y2="{y + 8:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<rect x="{xq1:.2f}" y="{y - 14:.2f}" width="{box_width:.2f}" height="28" rx="4" fill="{color}" fill-opacity=".22" stroke="{color}" stroke-width="1.6"/>')
        bits.append(f'<line x1="{xmed:.2f}" x2="{xmed:.2f}" y1="{y - 15:.2f}" y2="{y + 15:.2f}" stroke="{color}" stroke-width="4"/>')
        for run_index, (value, row) in enumerate(points[key]):
            jitter = ((run_index % 5) - 2) * 4
            repeat = row.get("repeat")
            bits.append(
                f'<circle cx="{x(value):.2f}" cy="{y + jitter:.2f}" r="4" fill="{color}" stroke="var(--surface)" stroke-width="1.4">'
                f'<title>{esc(label)} · repeat {esc(repeat if repeat is not None else run_index + 1)}: {esc(fmt_seconds(value))}, valid run'
                f'{", from Direct mode" if is_reference else ""}</title></circle>'
            )
        count = len(values)
        detail = f"median {fmt_seconds(median)} · n={count}"
        if excluded[key]:
            detail += f" · {excluded[key]} excluded"
        bits.append(f'<text x="770" y="{y + 5:.2f}" class="median-label">{esc(detail)}</text>')
    bits.append('</svg>')
    return "".join(bits)


def median_for(rows: list[dict[str, Any]], suite: str, mode: str, stage: str) -> float | None:
    values = [
        time_value(row)
        for row in rows
        if row.get("suite") == suite and row.get("mode") == mode and row.get("stage") == stage
        and is_measured(row) and is_valid_run(row, suite)
    ]
    clean = [value for value in values if value is not None]
    return statistics.median(clean) if clean else None


def pct_change(value: float, reference: float) -> str:
    delta = (value / reference - 1) * 100
    if abs(delta) < 0.05:
        return "about the same median"
    direction = "faster" if delta < 0 else "slower"
    return f"{abs(delta):.1f}% {direction} by median"


def takeaway_cards(rows: list[dict[str, Any]]) -> str:
    cards: list[str] = []
    for mode in MODE_ORDER:
        if not any(row.get("mode") == mode and is_measured(row) for row in rows):
            cards.append(
                f'<article class="takeaway-card"><p class="eyebrow">{esc(MODES[mode])}</p>'
                '<p>No manifest supplied for this cache state.</p></article>'
            )
            continue
        statements: list[str] = []
        for suite, spec in SUITES.items():
            medians = {
                key: median_for(rows, suite, mode, key)
                for key in STAGE_ORDER
            }
            available = [(key, value) for key, value in medians.items() if value is not None]
            if not available:
                statements.append(f"{spec['title']}: no valid measured runs.")
                continue
            fastest_key, fastest_value = min(available, key=lambda item: item[1])
            pb = medians.get("protobuf")
            fb = medians.get("flatbuffers")
            branch_note = ""
            if pb is not None and fb is not None:
                if fb < pb:
                    branch_note = f" FlatBuffers was {pct_change(fb, pb)} than Protobuf."
                elif pb < fb:
                    branch_note = f" Protobuf was {pct_change(pb, fb)} than FlatBuffers."
                else:
                    branch_note = " Protobuf and FlatBuffers had the same median."
            statements.append(
                f"{spec['title']}: {stage_title(fastest_key)} had the lowest observed median "
                f"({fmt_seconds(fastest_value)}).{branch_note}"
            )
        cards.append(
            f'<article class="takeaway-card"><p class="eyebrow">{esc(MODES[mode])}</p>'
            f'<p>{esc(" ".join(statements))}</p></article>'
        )
    return "".join(cards)


def cache_count_value(value: Any) -> float | None:
    if isinstance(value, (int, float, str)):
        return number(value)
    if isinstance(value, dict):
        for key in ("actual", "count", "value"):
            result = number(value.get(key))
            if result is not None:
                return result
    return None


def cache_counters(row: dict[str, Any]) -> dict[str, float]:
    counts = {"direct_hits": 0.0, "preprocessed_hits": 0.0, "hits": 0.0, "misses": 0.0, "hit_rate": 0.0}
    seen: set[tuple[str, str]] = set()

    def collect(obj: Any, in_cache: bool = False, parent: str = "") -> None:
        if isinstance(obj, dict):
            for raw_key, value in obj.items():
                key = "".join(character for character in str(raw_key).lower() if character.isalnum())
                cache_context = in_cache or "cache" in key or "ccache" in key or "counter" in key or "stats" in key
                metric: str | None = None
                if "hitrate" in key:
                    metric = "hit_rate"
                elif "miss" in key and cache_context:
                    metric = "misses"
                elif "hit" in key and cache_context:
                    if "preprocessed" in key:
                        metric = "preprocessed_hits"
                    elif "direct" in key:
                        metric = "direct_hits"
                    else:
                        metric = "hits"
                if metric is not None:
                    amount = cache_count_value(value)
                    if amount is not None:
                        token = (str(id(obj)), key)
                        if token not in seen:
                            counts[metric] += amount
                            seen.add(token)
                        continue
                collect(value, cache_context, key)
        elif isinstance(obj, list):
            for item in obj:
                collect(item, in_cache, parent)

    # The runner records the same counters both in `cache_validation` and flattened
    # on each result row. Prefer the nested object so totals are not counted twice.
    source = row.get("cache_validation") if isinstance(row.get("cache_validation"), dict) else row
    collect(source, in_cache=source is not row)
    if source is not row and any(key in source for key in ("hits", "misses", "cacheable_calls")):
        return {
            "direct_hits": counts["direct_hits"],
            "preprocessed_hits": counts["preprocessed_hits"],
            "hits": counts["hits"],
            "misses": counts["misses"],
        }
    return {key: value for key, value in counts.items() if value != 0}


def cache_summary(rows: list[dict[str, Any]], provenance: dict[str, dict[str, Any]]) -> str:
    cards: list[str] = []
    cache_stages = [key for key in STAGE_ORDER if any(
        bool(meta.get("cache")) for meta in provenance.get(key, {}).values() if isinstance(meta, dict)
    )]
    if not cache_stages:
        cache_stages = ["ccache-text", "protobuf", "flatbuffers"]

    for mode in MODE_ORDER:
        parts: list[str] = []
        found_counters = False
        for stage in cache_stages:
            measured = [
                row for row in rows if row.get("mode") == mode and row.get("stage") == stage
                and row.get("suite") in SUITES and is_measured(row) and is_valid_run(row, row.get("suite", ""))
            ]
            if not measured:
                continue
            counters = [cache_counters(row) for row in measured]
            available = [item for item in counters if item]
            if not available:
                continue
            found_counters = True
            hit_values = [item.get("direct_hits", 0) + item.get("preprocessed_hits", 0) + item.get("hits", 0) for item in available]
            miss_values = [item.get("misses", 0) for item in available if "misses" in item]
            hit_runs = sum(value > 0 for value in hit_values)
            total_hits = sum(hit_values)
            validations = [row.get("cache_validation") for row in measured if isinstance(row.get("cache_validation"), dict)]
            checks_passed = sum(item.get("passed") is True for item in validations)
            text = f"{stage_title(stage)}: cache check passed {checks_passed}/{len(validations)} runs; hits in {hit_runs}/{len(available)} measured runs ({total_hits:.0f} recorded)"
            if miss_values:
                text += f", {sum(miss_values):.0f} misses"
            parts.append(text)
        if mode == "direct":
            heading = "Direct mode should produce no cache hits."
        elif mode == "cold":
            heading = "Cold runs should start with an empty stage-owned cache."
        else:
            heading = "Warm runs should show hits after the first population."
        detail = " ".join(parts) if found_counters else "The input manifests contain no readable per-run cache counters."
        cards.append(
            f'<article class="cache-card"><p class="eyebrow">{esc(MODES[mode])}</p>'
            f'<p>{esc(heading)} {esc(detail)}</p></article>'
        )
    return "".join(cards)


def branch_svg() -> str:
    return '''<svg viewBox="0 0 850 230" role="img" aria-label="Before-cache text leads to the cache-integrated text state, from which protobuf and eager FlatBuffers are sibling branches" class="branch-chart">
      <title>Protocol comparison branch topology</title>
      <desc>Before-cache text leads to cache-integrated text. Protobuf and eager FlatBuffers branch independently from the same cache-integrated head.</desc>
      <g fill="none" stroke="currentColor" stroke-width="2"><path d="M230 115H310"/><path d="M535 115H595V55H650"/><path d="M595 115V175H650"/><path d="M642 47L650 55L642 63"/><path d="M642 167L650 175L642 183"/></g>
      <g class="branch-boxes"><rect x="10" y="76" width="220" height="78" rx="12"/><rect x="310" y="76" width="225" height="78" rx="12"/><rect x="650" y="16" width="190" height="78" rx="12"/><rect x="650" y="136" width="190" height="78" rx="12"/></g>
      <g class="branch-labels" text-anchor="middle"><text x="120" y="108">Before cache</text><text x="120" y="132" class="sub-label">text transport</text><text x="422" y="108">Text + ccache</text><text x="422" y="132" class="sub-label">shared protocol parent</text><text x="745" y="48">Protobuf</text><text x="745" y="70" class="sub-label">binary branch</text><text x="745" y="168">FlatBuffers eager</text><text x="745" y="190" class="sub-label">binary branch</text></g>
    </svg>'''


def provenance_table(provenance: dict[str, dict[str, Any]]) -> str:
    table_rows: list[str] = []
    for stage in STAGE_ORDER + tuple(key for key in provenance if key not in STAGES):
        states = provenance.get(stage, {})
        if not states:
            states = {"all": {}}
        for mode, meta in sorted(states.items(), key=lambda item: (MODE_ORDER.index(item[0]) if item[0] in MODE_ORDER else -1)):
            clava = str(meta.get("clava_revision") or "not recorded")
            dumper_rev = str(meta.get("dumper_revision") or "not recorded")
            dumper_hash = str(meta.get("dumper_sha256") or "not recorded")
            runtime = meta.get("runtime_manifest") if isinstance(meta.get("runtime_manifest"), dict) else {}
            runtime_hash = str(runtime.get("sha256") or "not recorded")
            jar_count = runtime.get("jar_count")
            status = meta.get("clava_status")
            dirty_count = len(status) if isinstance(status, list) else None
            dirty = "clean" if dirty_count == 0 else (f"{dirty_count} local change(s)" if dirty_count is not None else "not recorded")
            dumper_status = meta.get("dumper_status")
            if meta.get("native_root") is None:
                dumper_dirty = "not applicable"
            elif isinstance(dumper_status, list):
                dumper_dirty = "clean" if not dumper_status else f"{len(dumper_status)} local change(s)"
            else:
                dumper_dirty = "not recorded"
            state = MODES.get(mode, "all inputs")
            cells = [stage_title(stage, meta), state, clava, dumper_rev, dumper_hash, runtime_hash, dirty, dumper_dirty]
            table_rows.append("<tr>" + "".join(f'<td>{esc(cell)}</td>' for cell in cells) + "</tr>")
    header = "".join(f"<th>{esc(item)}</th>" for item in ("Tested state", "Cache state", "Clava commit", "Dumper revision", "Dumper SHA-256", "Java runtime SHA-256", "Clava worktree", "Dumper worktree"))
    return f'<div class="table-scroll"><table><thead><tr>{header}</tr></thead><tbody>{"".join(table_rows)}</tbody></table></div>'


def measurement_summary(rows: list[dict[str, Any]]) -> tuple[int, int, int]:
    measured = [row for row in rows if row.get("suite") in SUITES and is_measured(row)]
    valid = [row for row in measured if is_valid_run(row, row.get("suite", ""))]
    return len(measured), len(valid), len(measured) - len(valid)


def report_html(
    manifests: list[dict[str, Any]],
    provenance: dict[str, dict[str, Any]],
    provenance_warnings: list[str] | None = None,
    smoke_note: str | None = None,
) -> str:
    rows = flatten_results(manifests)
    modes_present = sorted({str(row.get("mode")) for row in rows if row.get("mode") in MODE_ORDER}, key=MODE_ORDER.index)
    measured_count, valid_count, excluded_count = measurement_summary(rows)
    dates = sorted(str(manifest.get("created_at")) for manifest in manifests if manifest.get("created_at"))
    date_text = f"Input runs created {dates[0]}" if dates else "Creation time not recorded in the input manifests"
    if len(dates) > 1:
        date_text += f" through {dates[-1]}"

    mode_sections: list[str] = []
    for mode in MODE_ORDER:
        charts = []
        for suite, spec in SUITES.items():
            svg = chart_svg(suite, mode, rows, provenance)
            charts.append(
                f'<figure class="chart-card"><figcaption><h3>{esc(spec["title"])}</h3>'
                f'<p>{esc(spec["description"])}</p></figcaption>{svg}'
                '<p class="chart-key">Whiskers: min/max · box: Q1–Q3 · center mark: median · dots: individual valid runs. Lower is faster.</p></figure>'
            )
        declared_repeats = [
            int(number(value.get("repeat_count"))) for value in manifests
            if value.get("mode") == mode and number(value.get("repeat_count")) is not None
        ]
        repeats_text = (
            f"Up to {max(declared_repeats)} measured repeats per stage."
            if declared_repeats else "Measured repeats are shown per stage when available."
        )
        mode_sections.append(
            f'<section class="mode-section" id="{esc(mode)}"><div class="section-heading"><div>'
            f'<p class="eyebrow">Cache state</p><h2>{esc(MODES[mode])}</h2></div>'
            f'<p>{esc(repeats_text)}</p></div>'
            f'<div class="chart-grid">{"".join(charts)}</div></section>'
        )

    warnings = "".join(f'<li>{esc(warning)}</li>' for warning in (provenance_warnings or []))
    warning_box = (
        f'<aside class="notice warning"><strong>Revision drift found.</strong><ul>{warnings}</ul>'
        '<p>Cache-state rows with different revisions do not form a controlled cache comparison. Check the manifest details below.</p></aside>'
        if warnings else ""
    )
    smoke_box = (
        f'<aside class="notice smoke"><strong>Local smoke render only.</strong> {esc(smoke_note)} This page is not performance evidence and must not be uploaded.</aside>'
        if smoke_note else ""
    )
    missing_modes = [MODES[mode] for mode in MODE_ORDER if mode not in modes_present]
    coverage_note = (
        f'<aside class="notice warning"><strong>Missing cache states:</strong> {esc(", ".join(missing_modes))}. '
        'Their chart panels are intentionally empty until new manifests are supplied.</aside>'
        if missing_modes else ""
    )
    all_valid = f"{valid_count:,} valid measured runs"
    if excluded_count:
        all_valid += f" · {excluded_count:,} invalid or incomplete measured runs excluded"

    return f'''<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="Measured Clava AST protocol comparison across two suites and three cache states.">
  <title>Clava AST protocol comparison</title>
  <style>
    :root {{ color-scheme: light; --page:#f4f7fb; --surface:#ffffff; --ink:#172033; --muted:#526174; --line:#d8e0eb; --grid:#e5eaf1; --notice:#eff6ff; --warn:#fff7ed; --shadow:0 10px 30px rgba(25,40,70,.07); }}
    html.dark {{ color-scheme: dark; --page:#101722; --surface:#182231; --ink:#e8edf5; --muted:#a7b4c7; --line:#354357; --grid:#2b394c; --notice:#172a42; --warn:#38291b; --shadow:0 12px 34px rgba(0,0,0,.24); }}
    * {{ box-sizing:border-box; }}
    body {{ margin:0; background:var(--page); color:var(--ink); font:16px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif; }}
    main {{ width:min(1180px,calc(100% - 36px)); margin:32px auto 72px; }}
    h1,h2,h3,p {{ margin-top:0; }}
    h1 {{ max-width:850px; margin-bottom:12px; font-size:clamp(2rem,5vw,3.25rem); line-height:1.08; letter-spacing:-.035em; }}
    h2 {{ margin:0; font-size:1.65rem; letter-spacing:-.02em; }}
    h3 {{ margin:0 0 4px; font-size:1.1rem; }}
    p {{ margin-bottom:.7rem; }}
    code {{ overflow-wrap:anywhere; font: .88em ui-monospace,SFMono-Regular,Consolas,monospace; }}
    .lede {{ max-width:820px; color:var(--muted); font-size:1.12rem; }}
    .meta-line,.small {{ color:var(--muted); font-size:.88rem; }}
    .eyebrow {{ margin-bottom:4px; color:var(--muted); font-size:.75rem; font-weight:750; text-transform:uppercase; letter-spacing:.1em; }}
    .hero,.takeaway-card,.chart-card,.cache-card,.branch-card,.details-card {{ background:var(--surface); border:1px solid var(--line); border-radius:18px; box-shadow:var(--shadow); }}
    .hero {{ padding:28px clamp(18px,4vw,42px); margin:24px 0 18px; }}
    .hero-stat {{ display:flex; flex-wrap:wrap; gap:8px 22px; margin-top:16px; padding-top:14px; border-top:1px solid var(--line); color:var(--muted); font-size:.9rem; }}
    .hero-stat strong {{ color:var(--ink); }}
    .notice {{ margin:16px 0; padding:14px 18px; border-radius:12px; border:1px solid var(--line); background:var(--notice); }}
    .notice.warning {{ background:var(--warn); }} .notice p:last-child {{ margin-bottom:0; }} .notice ul {{ margin:.5rem 0; }}
    .notice.smoke {{ border:2px solid #d97706; background:var(--warn); }}
    .decision-grid {{ display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; margin:18px 0 38px; }}
    .takeaway-card {{ padding:18px; }} .takeaway-card p:last-child {{ margin-bottom:0; }}
    .section-heading {{ display:flex; align-items:end; justify-content:space-between; gap:18px; margin:42px 0 15px; }}
    .section-heading>p {{ max-width:490px; margin:0; color:var(--muted); text-align:right; }}
    .chart-grid {{ display:grid; grid-template-columns:1fr; gap:16px; }}
    .chart-card {{ min-width:0; margin:0; padding:18px 18px 12px; }}
    .chart-card figcaption {{ display:flex; align-items:baseline; justify-content:space-between; gap:16px; margin:0 4px; }}
    .chart-card figcaption p {{ color:var(--muted); font-size:.85rem; text-align:right; }}
    .candle-chart {{ display:block; width:100%; height:auto; overflow:visible; color:var(--ink); }}
    .grid-line {{ stroke:var(--grid); stroke-width:1; }} .axis-text {{ fill:var(--muted); font-size:12px; }}
    .stage-text {{ fill:var(--ink); font-size:14px; font-weight:700; }} .empty-lane {{ fill:var(--muted); font-size:13px; }}
    .median-label {{ fill:var(--ink); font-size:13px; font-weight:650; }}
    .chart-key {{ margin:8px 4px 0; color:var(--muted); font-size:.8rem; }}
    .empty-chart {{ padding:35px 16px; color:var(--muted); text-align:center; border:1px dashed var(--line); border-radius:10px; }}
    .mode-section {{ margin-bottom:44px; }}
    .branch-card {{ padding:20px; margin:12px 0 28px; }}
    .branch-chart {{ display:block; width:100%; height:auto; color:var(--muted); }}
    .branch-boxes rect {{ fill:var(--surface); stroke:var(--line); stroke-width:2; }}
    .branch-labels text {{ fill:var(--ink); font-size:17px; font-weight:700; }}
    .branch-labels .sub-label {{ fill:var(--muted); font-size:12px; font-weight:500; }}
    .cache-grid {{ display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; margin:14px 0 28px; }}
    .cache-card {{ padding:16px; }} .cache-card p:last-child {{ margin-bottom:0; }}
    details {{ margin:14px 0; }} summary {{ cursor:pointer; font-weight:700; }}
    .details-card {{ padding:18px 20px; }} .details-card summary {{ margin:-18px -20px; padding:18px 20px; }}
    .details-card[open] summary {{ margin-bottom:14px; border-bottom:1px solid var(--line); }}
    .table-scroll {{ overflow-x:auto; }} table {{ width:100%; border-collapse:collapse; font-size:.82rem; }}
    th,td {{ min-width:120px; padding:9px 10px; border-bottom:1px solid var(--line); text-align:left; vertical-align:top; }}
    th {{ color:var(--muted); font-size:.75rem; letter-spacing:.04em; text-transform:uppercase; }}
    td:nth-child(n+3) {{ font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:.76rem; overflow-wrap:anywhere; }}
    .legend {{ display:flex; flex-wrap:wrap; gap:10px 18px; color:var(--muted); font-size:.84rem; }}
    .legend span {{ display:inline-flex; align-items:center; gap:7px; }} .swatch {{ width:12px; height:12px; border-radius:3px; }}
    .footer-note {{ margin-top:28px; padding-top:16px; border-top:1px solid var(--line); color:var(--muted); font-size:.85rem; }}
    @media(max-width:800px) {{ .decision-grid,.cache-grid {{ grid-template-columns:1fr; }} .section-heading {{ display:block; }} .section-heading>p {{ text-align:left; margin-top:8px; }} }}
    @media(max-width:650px) {{ main {{ width:min(100% - 20px,1180px); margin-top:18px; }} .hero {{ padding:22px 18px; }} .chart-card {{ padding:14px 8px 10px; }} .chart-card figcaption {{ display:block; }} .chart-card figcaption p {{ text-align:left; margin-bottom:0; }} .candle-chart {{ min-width:760px; }} .chart-card {{ overflow-x:auto; }} .branch-chart {{ min-width:700px; }} .branch-card {{ overflow-x:auto; }} }}
  </style>
</head>
<body>
<main>
  <header>
    <p class="eyebrow">Clava · performance comparison</p>
    <h1>Which AST transport state is fastest?</h1>
    <p class="lede">Two real Clava workloads measured across four pinned repository states and three cache conditions. The charts show the observed run-to-run spread, with correctness-gated runs only.</p>
    <p class="meta-line">{esc(date_text)}</p>
  </header>
  {smoke_box}
  {coverage_note}
  {warning_box}
  <section class="hero" aria-labelledby="decision-title">
    <p class="eyebrow">Decision view</p>
    <h2 id="decision-title">Observed medians by cache state</h2>
    <p>Each summary names the lowest median and compares the two binary branches. These are outcomes for the tested repository states. The branch comparison does not isolate the wire format from every other code change.</p>
    <div class="hero-stat"><span><strong>{esc(all_valid)}</strong></span><span>{measured_count:,} measured runs across both suites</span><span>Lower elapsed time is better</span></div>
  </section>
  <div class="decision-grid">{takeaway_cards(rows)}</div>
  <section aria-labelledby="topology-title">
    <p class="eyebrow">What was compared</p><h2 id="topology-title">Two protocol branches from one text + cache head</h2>
    <div class="branch-card">{branch_svg()}<p class="small">The protobuf and eager FlatBuffers states are siblings, not successive protocol steps. The chart labels are the pinned repository states in the experiment plan. In Direct mode, the pre-cache state streams text from stdout into the parser. Post-cache states consume completed files after the dumper exits. Text and protobuf use zstd files; FlatBuffers uses a raw binary file. These transport differences are part of the branch outcomes.</p></div>
  </section>
  <section aria-labelledby="cache-check-title">
    <p class="eyebrow">Cache-state verification</p><h2 id="cache-check-title">Did each cache state behave as intended?</h2>
    <div class="cache-grid">{cache_summary(rows, provenance)}</div>
    <p class="small">The harness checks direct mode with a ccache wrapper probe, cold mode with fresh-cache misses, and warm mode with cache hits. The cards summarize those per-run checks and recorded counters for valid measured repeats.</p>
  </section>
  <section aria-labelledby="charts-title">
    <p class="eyebrow">Run distributions</p><h2 id="charts-title">Wall time across both workloads</h2>
    <p class="small">Each panel uses its own scale. Within a panel all four states share the same axis. Cold and Warm panels repeat the exact Direct pre-cache distribution as a reference candle. It is labeled as having no cache state, not as a Cold or Warm run. Warm-up runs do not appear in the distributions.</p>
    {''.join(mode_sections)}
  </section>
  <section aria-labelledby="legend-title">
    <p class="eyebrow">Reading the charts</p><h2 id="legend-title">One candle represents repeated runs</h2>
    <div class="branch-card"><div class="legend">{''.join(f'<span><i class="swatch" style="background:{STAGES[key][1]}"></i>{esc(STAGES[key][0])}</span>' for key in STAGE_ORDER)}</div><p class="small" style="margin-top:12px">Whiskers mark minimum and maximum. The colored box spans the first to third quartile. The bold mark is the median. Each dot is one valid measured repeat. Six repeats describe the observed spread; they do not establish statistical significance.</p></div>
  </section>
  <details class="details-card">
    <summary>Methods and workload</summary>
    <p><strong>Clava-JS.</strong> {esc(SUITES['clava-js']['description'])} A run is eligible only with 158 passes, six skips, zero failures, and a successful process exit.</p>
    <p><strong>Java parser.</strong> {esc(SUITES['java']['description'])} A run is eligible only with 116 passes, no skips or failures, and a successful process exit.</p>
    <p><strong>Cache modes.</strong> Direct disables ccache. Cold clears the stage-owned cache before each measured run. Warm reuses the stage-owned cache. One uncharted resource warm-up precedes each stage and suite.</p>
    <p><strong>Metric.</strong> Candles use elapsed wall time recorded by <code>/usr/bin/time</code>. The renderer excludes a run if the manifest has no wall-time field.</p>
    <p><strong>Limits.</strong> The suite samples cover two workloads on one host. These results describe the pinned branch outcomes; they do not establish behavior for other projects, platforms, or compiler versions. No result is presented as a format-only causal effect.</p>
  </details>
  <details class="details-card">
    <summary>Exact revisions and artifact fingerprints</summary>
    <p class="small">Absolute local paths are omitted. Full commit IDs and recorded hashes are retained so each tested state can be matched to its artifacts.</p>
    {provenance_table(provenance)}
  </details>
  <p class="footer-note">Generated from the supplied protocol-comparison JSON manifests. Invalid runs, warm-ups, and runs without a valid wall-time metric are excluded from candles and medians.</p>
</main>
</body>
</html>'''


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, action="append", required=True, help="results.json manifest; repeat once per cache state")
    parser.add_argument("--output", type=Path, required=True, help="HTML output path, or '-' for stdout")
    args = parser.parse_args()
    try:
        manifests, provenance, warnings = load_inputs(args.input)
        rendered = report_html(manifests, provenance, warnings)
    except (OSError, json.JSONDecodeError, ValueError) as error:
        print(f"render_report.py: {error}", file=sys.stderr)
        return 2
    if str(args.output) == "-":
        sys.stdout.write(rendered)
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
