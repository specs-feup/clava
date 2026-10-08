#!/usr/bin/env python3
"""Render the validated in-suite App-build timing matrix as an HTML fragment.

Input is JSON with ``schema_version``, ``timing_boundary``, ``repeat_count``,
``forced_gc``, ``startup_included``, ``groups`` and ``rows``. A group has
``suite``, ``group_id`` and ``source_count``. A row has ``suite``, ``stage``,
``mode``, ``repeat``, ``group_id``, ``elapsed_ms``, ``valid`` and
``app_returned_null``. Group identifiers are used only for validation and are
never included in generated output.

The chart sums each repeat's completed-App call durations. It does not treat
individual Apps as independent suite runs and does not add this time to command
wall time.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import html
import io
import json
import math
from pathlib import Path
import statistics
from typing import Any


TIMING_BOUNDARY = "ParallelCodeParser.parse (3-arg) entry to completed App"
REPEAT_COUNT = 4
MODES = ("direct", "cold", "warm")
STAGES = ("before-cache", "ccache-text", "protobuf", "flatbuffers")
SUITE_ORDER = ("clava-js", "java")
SUITE_LABELS = {"clava-js": "Clava-JS", "java": "Java parser"}
STAGE_LABELS = {
    "before-cache": "Before cache",
    "ccache-text": "Text",
    "protobuf": "Protobuf",
    "flatbuffers": "FlatBuffers",
}
STAGE_COLOURS = {
    "before-cache": "#2563eb",
    "ccache-text": "#059669",
    "protobuf": "#7c3aed",
    "flatbuffers": "#d97706",
}
MODE_LABELS = {
    "direct": "Direct · ccache off",
    "cold": "Cold · empty cache",
    "warm": "Warm · seeded cache",
}


def _fail(message: str) -> None:
    raise ValueError(message)


def validate_input(payload: Any) -> dict[str, Any]:
    """Validate a complete, paired 2-suite App timing matrix."""
    if not isinstance(payload, dict) or type(payload.get("schema_version")) is not int or payload["schema_version"] != 1:
        _fail("expected App-build schema_version 1")
    if payload.get("timing_boundary") != TIMING_BOUNDARY:
        _fail("timing_boundary does not match the reviewed ParallelCodeParser.parse boundary")
    if type(payload.get("repeat_count")) is not int or payload["repeat_count"] != REPEAT_COUNT:
        _fail("expected exactly four repeats")
    if payload.get("forced_gc") is not False:
        _fail("forced_gc must be false for App-build timings")
    if payload.get("startup_included") is not False:
        _fail("startup_included must be false for App-build timings")

    raw_groups = payload.get("groups")
    raw_rows = payload.get("rows")
    if not isinstance(raw_groups, list) or not raw_groups:
        _fail("groups must be a non-empty array")
    if not isinstance(raw_rows, list) or not raw_rows:
        _fail("rows must be a non-empty array")

    groups: dict[tuple[str, str], int] = {}
    suites: set[str] = set()
    for group in raw_groups:
        if not isinstance(group, dict):
            _fail("each group must be an object")
        suite = group.get("suite")
        group_id = group.get("group_id")
        source_count = group.get("source_count")
        if not isinstance(suite, str) or not suite.strip():
            _fail("group suite must be a non-empty string")
        if not isinstance(group_id, str) or not group_id:
            _fail("group_id must be a non-empty string")
        if type(source_count) is not int or source_count < 0:
            _fail("source_count must be a non-negative integer")
        key = (suite, group_id)
        if key in groups:
            _fail("duplicate group metadata")
        groups[key] = source_count
        suites.add(suite)

    if len(suites) != 2:
        _fail("expected exactly two suites")
    ordered_suites = tuple(suite for suite in SUITE_ORDER if suite in suites)
    if len(ordered_suites) != 2:
        _fail("unsupported suite set; expected Clava-JS and Java")
    ids_by_suite = {
        suite: {group_id for group_suite, group_id in groups if group_suite == suite}
        for suite in ordered_suites
    }

    index: dict[tuple[str, str, str, int, str], float] = {}
    groups_by_cell: dict[tuple[str, str, str, int], set[str]] = {}
    expected_modes = {
        (mode, stage)
        for mode in MODES
        for stage in STAGES
        if mode == "direct" or stage != "before-cache"
    }
    for row in raw_rows:
        if not isinstance(row, dict):
            _fail("each timing row must be an object")
        suite = row.get("suite")
        stage = row.get("stage")
        mode = row.get("mode")
        repeat = row.get("repeat")
        group_id = row.get("group_id")
        if (not isinstance(suite, str) or suite not in suites
                or not isinstance(stage, str) or stage not in STAGES
                or not isinstance(mode, str) or mode not in MODES):
            _fail("timing row has an unknown suite, stage, or mode")
        if (mode, stage) not in expected_modes:
            _fail("before-cache rows are valid only in direct mode")
        if type(repeat) is not int or repeat not in range(1, REPEAT_COUNT + 1):
            _fail("repeat must be an integer from 1 through 4")
        if not isinstance(group_id, str) or group_id not in ids_by_suite[suite]:
            _fail("timing row does not match declared group metadata")
        if row.get("valid") is not True:
            _fail("every App timing row must be valid")
        if row.get("app_returned_null") is not False:
            _fail("every timed parse must return a non-null App")
        elapsed = row.get("elapsed_ms")
        if (isinstance(elapsed, bool) or not isinstance(elapsed, (int, float))
                or not math.isfinite(elapsed) or elapsed < 0):
            _fail("elapsed_ms must be a finite non-negative number")
        key = (suite, stage, mode, repeat, group_id)
        if key in index:
            _fail("duplicate suite/stage/mode/repeat/group timing row")
        index[key] = float(elapsed)
        groups_by_cell.setdefault((suite, stage, mode, repeat), set()).add(group_id)

    expected_count = sum(
        len(ids_by_suite[suite]) * len(expected_modes) * REPEAT_COUNT
        for suite in ordered_suites
    )
    if len(index) != expected_count:
        _fail("timing matrix has missing rows")
    for suite in ordered_suites:
        expected_groups = ids_by_suite[suite]
        for mode, stage in expected_modes:
            for repeat in range(1, REPEAT_COUNT + 1):
                cell_groups = groups_by_cell.get((suite, stage, mode, repeat), set())
                if cell_groups != expected_groups:
                    _fail("each timing cell must contain the same declared group set")

    return {
        "suites": ordered_suites,
        "groups": groups,
        "ids_by_suite": ids_by_suite,
        "values_ms": index,
        "payload": payload,
    }


def aggregate(data: dict[str, Any]) -> dict[tuple[str, str, str], list[float]]:
    """Return summed App-call milliseconds for each suite/mode/stage repeat."""
    sums: dict[tuple[str, str, str], list[float]] = {}
    for suite in data["suites"]:
        group_ids = sorted(data["ids_by_suite"][suite])
        for mode in MODES:
            for stage in STAGES:
                if mode != "direct" and stage == "before-cache":
                    continue
                sums[(suite, mode, stage)] = [
                    sum(
                        data["values_ms"][(suite, stage, mode, repeat, group_id)]
                        for group_id in group_ids
                    )
                    for repeat in range(1, REPEAT_COUNT + 1)
                ]
    return sums


def paired_percentages(
    totals: dict[tuple[str, str, str], list[float]], suite: str, mode: str, stage: str
) -> list[float | None]:
    """Paired per-repeat percent change versus Text; None means zero baseline."""
    reference = totals[(suite, mode, "ccache-text")]
    candidate = totals[(suite, mode, stage)]
    return [
        100.0 * (value / baseline - 1.0) if baseline > 0 else None
        for value, baseline in zip(candidate, reference)
    ]


def median_or_none(values: list[float | None]) -> float | None:
    if any(value is None for value in values):
        return None
    valid = [value for value in values if value is not None]
    return statistics.median(valid) if valid else None


def _quantile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def _suite_label(suite: str) -> str:
    return SUITE_LABELS[suite]


def _chart(data: dict[str, Any], totals: dict[tuple[str, str, str], list[float]], suite: str) -> str:
    cells = [
        (mode, stage, totals[(suite, mode, stage)])
        for mode in MODES
        for stage in STAGES
        if mode == "direct" or stage != "before-cache"
    ]
    all_values = [value for _, _, values in cells for value in values]
    min_value = min(all_values, default=0.0)
    max_value = max(all_values, default=0.0)
    spread = max_value - min_value
    margin = max(250.0, 0.04 * spread)
    low = max(0.0, min_value - margin)
    high = max_value + margin
    if high <= low:
        high = low + 250.0
    x = lambda value: 98.0 + 180.0 * (value - low) / (high - low)
    tick_step_seconds = (high - low) / 2000.0
    precision = (0 if tick_step_seconds >= 10 else 1 if tick_step_seconds >= 1
                 else 2 if tick_step_seconds >= 0.1 else 3)
    height = 490
    axis_note = "Non-zero axis" if low > 0 else "Zero-based axis"
    out = [
        f'<svg class="app-chart" viewBox="0 0 360 {height}" role="img" aria-label="{html.escape(_suite_label(suite), quote=True)} summed App-build timing distributions">',
        '<desc>Each dot is one of four repeats, summed across all completed App calls in the original suite. All modes and stages share one seconds axis. This is not per-file replay or suite command wall time.</desc>',
    ]
    for tick in (low, (low + high) / 2.0, high):
        out.extend([
            f'<line class="grid" x1="{x(tick):.2f}" x2="{x(tick):.2f}" y1="28" y2="454"/>',
            f'<text class="tick" x="{x(tick):.2f}" y="18" text-anchor="middle">{tick / 1000:.{precision}f}s</text>',
        ])
    y = 40
    for mode in MODES:
        if mode != "direct":
            out.append(f'<line class="divider" x1="0" x2="360" y1="{y - 10}" y2="{y - 10}"/>')
        out.append(f'<text class="mode" x="0" y="{y + 5}">{html.escape(MODE_LABELS[mode])}</text>')
        y += 27
        stages = STAGES if mode == "direct" else STAGES[1:]
        for stage in stages:
            values = totals[(suite, mode, stage)]
            q1, med, q3 = _quantile(values, 0.25), statistics.median(values), _quantile(values, 0.75)
            colour = STAGE_COLOURS[stage]
            out.extend([
                f'<text class="label" x="0" y="{y + 4}">{html.escape(STAGE_LABELS[stage])}</text>',
                f'<line x1="{x(min(values)):.2f}" x2="{x(max(values)):.2f}" y1="{y}" y2="{y}" stroke="{colour}" stroke-width="2"/>',
                f'<rect x="{x(q1):.2f}" y="{y - 8}" width="{max(2.0, x(q3) - x(q1)):.2f}" height="16" fill="{colour}" fill-opacity=".22" stroke="{colour}"/>',
                f'<line x1="{x(med):.2f}" x2="{x(med):.2f}" y1="{y - 9}" y2="{y + 9}" stroke="{colour}" stroke-width="3"/>',
                f'<text class="value" x="359" y="{y + 4}" text-anchor="end">{med / 1000:.2f}s</text>',
            ])
            for repeat, value in enumerate(values, start=1):
                dot_y = y + ((repeat - 1) % 3 - 1) * 3
                out.append(
                    f'<circle cx="{x(value):.2f}" cy="{dot_y}" r="2.8" fill="{colour}"><title>Repeat {repeat}: {value / 1000:.3f}s summed across {len(data["ids_by_suite"][suite])} App calls</title></circle>'
                )
            y += 28
        y += 20
    out.extend([
        f'<text class="foot" x="0" y="464">{axis_note} · seconds · same scale across Direct, Cold, and Warm</text>',
        f'<text class="foot" x="0" y="480">Four repeats · summed App calls per repeat · lower is faster</text>',
        '</svg>',
    ])
    return "".join(out)


def _comparison_table(
    totals: dict[tuple[str, str, str], list[float]], suite: str
) -> str:
    out = [
        f'<div class="app-comparison"><h3>{html.escape(_suite_label(suite))}: paired format change vs Text</h3>',
        '<table><thead><tr><th scope="col">Format</th><th scope="col">Direct</th><th scope="col">Cold</th><th scope="col">Warm</th></tr></thead><tbody>',
    ]
    for stage in ("protobuf", "flatbuffers"):
        out.append(f'<tr><th scope="row">{STAGE_LABELS[stage]}</th>')
        for mode in MODES:
            effects = paired_percentages(totals, suite, mode, stage)
            delta = median_or_none(effects)
            if delta is None:
                label = "n/a"
                note = "incomplete zero baseline"
            else:
                faster = sum(value < 0 for value in effects if value is not None)
                label = f"{delta:+.1f}%"
                note = f"{faster}/{REPEAT_COUNT} faster"
            out.append(f'<td>{label}<small>{note}</small></td>')
        out.append('</tr>')
    out.append('</tbody></table></div>')
    return "".join(out)


def render_fragment(payload: Any) -> str:
    data = validate_input(payload)
    totals = aggregate(data)
    charts = []
    for suite in data["suites"]:
        charts.append(
            f'<figure><figcaption>{html.escape(_suite_label(suite))} · {len(data["ids_by_suite"][suite])} original suite App calls</figcaption>{_chart(data, totals, suite)}</figure>'
        )
    comparisons = "".join(_comparison_table(totals, suite) for suite in data["suites"])
    return f'''<section class="app-build-timing" aria-label="In-suite completed App-build timings">
<style>
.app-build-timing {{ color:var(--ink,#202a34); font:15px/1.45 system-ui,sans-serif; }}
.app-build-timing h2 {{ margin:0 0 6px; font-size:21px; }}
.app-build-timing h3 {{ margin:14px 0 5px; font-size:16px; }}
.app-build-timing .intro,.app-build-timing .note {{ margin:6px 0; color:var(--muted,#56636e); }}
.app-build-timing .panels {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }}
.app-build-timing figure {{ min-width:0; margin:8px 0; }}
.app-build-timing figcaption {{ margin:0 0 4px; font-weight:650; }}
.app-build-timing svg.app-chart {{ display:block; width:100%; max-width:520px; height:auto; margin:6px 0 0; border:0; padding:0; }}
.app-build-timing .grid {{ stroke:var(--line,#d9e0e5); stroke-width:1; }}
.app-build-timing .divider {{ stroke:var(--muted,#87939d); stroke-width:1.2; stroke-dasharray:4 3; }}
.app-build-timing .app-chart .tick {{ fill:var(--muted,#56636e); font:13px system-ui,sans-serif; }}
.app-build-timing .app-chart .label,.app-build-timing .app-chart .mode {{ fill:var(--muted,#56636e); font:15px system-ui,sans-serif; }}
.app-build-timing .mode {{ font-weight:650; }}
.app-build-timing .app-chart .value {{ fill:var(--ink,#28343e); font:14px ui-monospace,monospace; }}
.app-build-timing .app-chart .foot {{ fill:var(--muted,#56636e); font:11px system-ui,sans-serif; }}
.app-build-timing .app-comparison {{ display:block; margin-top:12px; }}
.app-build-timing .app-comparison table {{ display:table; width:100%; table-layout:fixed; border-collapse:collapse; font-size:13px; border:0; padding:0; }}
.app-build-timing th,.app-build-timing td {{ padding:5px 8px; border-bottom:1px solid var(--line,#d9e0e5); text-align:right; }}
.app-build-timing th:first-child {{ width:29%; text-align:left; }}
.app-build-timing td small {{ display:block; color:var(--muted,#56636e); font-size:11px; }}
@media (max-width:700px) {{ .app-build-timing .panels {{ grid-template-columns:minmax(0,1fr); gap:6px; }} .app-build-timing svg.app-chart {{ max-width:420px; }} .app-build-timing .app-comparison table {{ font-size:12px; }} .app-build-timing th,.app-build-timing td {{ padding:4px 3px; }} }}
</style>
<h2>Build the Clava App</h2>
<p class="intro">Only the work from the original C/C++ inputs to a ready-to-use App is timed, including cross-file linking. The App is Clava’s parsed program model. Each dot is one round’s total across all App-building calls in that suite.</p>
<p class="intro">No Gradle/JVM startup, assertions, syntax-only checks, code display or forced GC.</p>
<div class="panels">{''.join(charts)}</div>
<p class="note">Format changes below compare matching rounds against Text. Negative means faster.</p>
{comparisons}
<p class="note">Compare options within each suite. The two suites use different scales.</p>
<p class="note">Line median; box middle half; whiskers range; dots four rounds.</p>
<details class="app-method"><summary>Timing and calculation details</summary>
<p>The timer starts at entry to the 3-argument <code>ParallelCodeParser.parse</code> method and stops after the final App tree transformations, before <code>SHOW_EXEC_INFO</code>, heap logging, or code/AST display. Parser-instance construction and a new <code>ClavaContext</code> created before method entry are excluded. TextParser/text passes and cross-file linking are inside the interval. Natural GC during the call is included; forced GC is disabled.</p>
<p>These are timings from the original suites, not replay or per-file timing. The interval excludes heap logging, code display, assertions, code generation, syntax-only validation, and Gradle/JVM startup.</p>
<p>The four rounds describe the spread of observed totals. They are not confidence intervals.</p>
<p>For each round, the chart sums elapsed milliseconds across all App calls, including group-empty Apps. Paired change is calculated per round as 100 × (format total / same-mode Text total − 1), then summarized by the median of four paired percentages. A zero Text total makes that percentage undefined.</p>
</details>
</section>
'''


def csv_text(payload: Any) -> str:
    data = validate_input(payload)
    totals = aggregate(data)
    fields = (
        "row_type", "suite", "stage", "mode", "repeat", "group_sha256",
        "app_call_count", "source_count", "source_count_total", "elapsed_ms",
        "paired_change_vs_text_pct", "faster_repeats",
    )
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields)
    writer.writeheader()
    for suite in data["suites"]:
        group_count = len(data["ids_by_suite"][suite])
        for mode in MODES:
            stages = STAGES if mode == "direct" else STAGES[1:]
            for stage in stages:
                for repeat in range(1, REPEAT_COUNT + 1):
                    for group_id in sorted(data["ids_by_suite"][suite]):
                        digest = hashlib.sha256(
                            f"{suite}\0{group_id}".encode("utf-8")
                        ).hexdigest()
                        elapsed = data["values_ms"][(suite, stage, mode, repeat, group_id)]
                        writer.writerow({
                            "row_type": "app_call",
                            "suite": _suite_label(suite),
                            "stage": STAGE_LABELS[stage],
                            "mode": mode,
                            "repeat": repeat,
                            "group_sha256": digest,
                            "app_call_count": 1,
                            "source_count": data["groups"][(suite, group_id)],
                            "source_count_total": "",
                            "elapsed_ms": f"{elapsed:.6f}",
                            "paired_change_vs_text_pct": "",
                            "faster_repeats": "",
                        })
        source_total = sum(
            count for (group_suite, _), count in data["groups"].items()
            if group_suite == suite
        )
        for mode in MODES:
            stages = STAGES if mode == "direct" else STAGES[1:]
            for stage in stages:
                values = totals[(suite, mode, stage)]
                effects = (
                    None if stage in ("before-cache", "ccache-text")
                    else paired_percentages(totals, suite, mode, stage)
                )
                for repeat, elapsed in enumerate(values, start=1):
                    effect = None if effects is None else effects[repeat - 1]
                    writer.writerow({
                        "row_type": "repeat_total",
                        "suite": _suite_label(suite),
                        "stage": STAGE_LABELS[stage],
                        "mode": mode,
                        "repeat": repeat,
                        "group_sha256": "",
                        "app_call_count": group_count,
                        "source_count": "",
                        "source_count_total": source_total,
                        "elapsed_ms": f"{elapsed:.6f}",
                        "paired_change_vs_text_pct": "" if effect is None else f"{effect:.6f}",
                        "faster_repeats": "",
                    })
                if effects is not None:
                    valid_effects = effects if all(effect is not None for effect in effects) else []
                    writer.writerow({
                        "row_type": "paired_median",
                        "suite": _suite_label(suite),
                        "stage": STAGE_LABELS[stage],
                        "mode": mode,
                        "repeat": "",
                        "group_sha256": "",
                        "app_call_count": group_count,
                        "source_count": "",
                        "source_count_total": source_total,
                        "elapsed_ms": "",
                        "paired_change_vs_text_pct": "" if not valid_effects else f"{statistics.median(valid_effects):.6f}",
                        "faster_repeats": f"{sum(effect < 0 for effect in valid_effects)}/{len(valid_effects)}" if valid_effects else "",
                    })
    return stream.getvalue()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--csv", type=Path, required=True)
    args = parser.parse_args(argv)
    payload = json.loads(args.input.read_text(encoding="utf-8"))
    fragment = render_fragment(payload)
    exported = csv_text(payload)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(fragment, encoding="utf-8")
    args.csv.write_text(exported, encoding="utf-8", newline="")
    print(json.dumps({"output": args.output.name, "csv": args.csv.name, "validated": True}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
