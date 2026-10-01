#!/usr/bin/env python3
"""Render paired Protobuf-versus-FlatBuffers runtime differences.

The Java chart uses matched OFF-worker rounds from the fresh 116-test matrix.
An optional normalized Clava-JS cohort adds a separate warm comparison; it is
never merged with the accepted original-JS observations.
"""
from __future__ import annotations

import argparse
import csv
import html
import io
import json
import math
from pathlib import Path
import statistics


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
MATRIX_ROOT = REPOSITORY_ROOT / "experiments/protocol-comparison/results/deadline-20260930/matrix-r2"
DEFAULT_JS = MATRIX_ROOT / "results.json"
DEFAULT_JAVA = MATRIX_ROOT / "java-noagent-matrix-r2/results.json"
DEFAULT_OUTPUT = MATRIX_ROOT / "decision-noagent-r1/protobuf-vs-flatbuffers-paired.html"
DEFAULT_CSV = MATRIX_ROOT / "decision-noagent-r1/protobuf-vs-flatbuffers-paired.csv"

REPEATS = (1, 2, 3, 4)
MODES = ("direct", "cold", "warm")
STAGES = ("protobuf", "flatbuffers")
JAVA_METRICS = (
    ("elapsed_s", "Wall"),
    ("junit_aggregate_s", "JUnit"),
    ("outside_junit_s", "Outside JUnit"),
)


def read_json(path: Path):
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def _rows(payload) -> list[dict]:
    if isinstance(payload, list):
        rows = payload
    elif isinstance(payload, dict):
        rows = payload.get("results")
    else:
        rows = None
    if not isinstance(rows, list) or not all(isinstance(row, dict) for row in rows):
        raise ValueError("Expected a results array or a manifest containing results")
    return rows


def _selected(rows: list[dict], suite: str, mode: str, stage: str) -> list[dict]:
    return [row for row in rows
            if row.get("suite") == suite and row.get("mode") == mode
            and row.get("stage") == stage and row.get("selected", True) is True
            and row.get("measured") is True]


def _validate_run(row: dict, suite: str, *, normalized_js: bool = False) -> None:
    if row.get("valid") is not True:
        raise ValueError(f"Invalid selected run in {suite}/{row.get('mode')}/{row.get('stage')}")
    status = row.get("return_code", row.get("exit_status", 0))
    if status != 0 or row.get("failed_tests", 0) != 0:
        raise ValueError(f"Failed selected run in {suite}/{row.get('mode')}/{row.get('stage')}")
    if row.get("workload_identity_match") is False:
        raise ValueError(f"Workload identity mismatch in {suite}/{row.get('mode')}/{row.get('stage')}")
    if normalized_js and row.get("fast_syntax") is not True:
        raise ValueError("Normalized JS inputs must explicitly mark fast_syntax=true")


def _validate_cell(rows: list[dict], suite: str, mode: str, stage: str,
                   *, normalized_js: bool = False) -> dict[int, dict]:
    cell = _selected(rows, suite, mode, stage)
    if len(cell) != 4 or {row.get("repeat") for row in cell} != set(REPEATS):
        raise ValueError(f"Expected four distinct selected rounds for {suite}/{mode}/{stage}")
    by_repeat = {}
    for row in cell:
        _validate_run(row, suite, normalized_js=normalized_js)
        by_repeat[row["repeat"]] = row
    return by_repeat


def _require_same_identity(left: dict[int, dict], right: dict[int, dict], suite: str) -> None:
    for repeat in REPEATS:
        for field in ("test_identity_sha256", "fixture_fingerprint_sha256"):
            a, b = left[repeat].get(field), right[repeat].get(field)
            if a is not None and b is not None and a != b:
                raise ValueError(f"Matched {suite} round {repeat} differs in {field}")


def _require_stable_identity(cell: dict[int, dict], suite: str) -> None:
    for field in ("test_identity_sha256", "fixture_fingerprint_sha256"):
        recorded = [row.get(field) for row in cell.values() if row.get(field) is not None]
        if recorded and len(set(recorded)) != 1:
            raise ValueError(f"Selected {suite} rounds differ in {field}")


def _validate_java_matrix(matrix: dict) -> list[dict]:
    plan = matrix.get("plan", {})
    if (matrix.get("schema_version") != 1 or plan.get("suites") != ["java"]
            or plan.get("repeat_count") != 4):
        raise ValueError("Expected the Java-only four-round OFF matrix")
    rows = matrix.get("results")
    if not isinstance(rows, list):
        raise ValueError("Java matrix has no results array")
    measured = [row for row in rows if row.get("selected") is True and row.get("measured") is True]
    if len(measured) != 40:
        raise ValueError("Expected 40 selected Java OFF measurements")
    for row in measured:
        _validate_run(row, "java")
        if row.get("agent") != "off":
            raise ValueError("Java contrast requires JaCoCo-off measurements")
        if row.get("no_explicit_gc_flags") is not True:
            raise ValueError("Java contrast requires the no-explicit-GC worker control")
        if row.get("test_task_executed") is not True:
            raise ValueError("Java contrast requires an executed Gradle test task")
        if (row.get("total_tests"), row.get("passed_tests"), row.get("failed_tests"),
                row.get("skipped_tests")) != (116, 116, 0, 0):
            raise ValueError("Java contrast requires 116/116 passing tests in every run")
        args = row.get("actual_test_executor_args")
        if not isinstance(args, list) or any("javaagent" in str(arg) for arg in args):
            raise ValueError("Java OFF contrast lacks proof of agent-free worker arguments")
        skipped = row.get("report_tasks_skipped", {})
        if skipped.get("jacocoTestReport") is not True or skipped.get("jacocoTestCoverageVerification") is not True:
            raise ValueError("Java OFF contrast requires both JaCoCo report tasks skipped")
    for mode in MODES:
        pb = _validate_cell(measured, "java", mode, "protobuf")
        flat = _validate_cell(measured, "java", mode, "flatbuffers")
        _require_stable_identity(pb, "java")
        _require_stable_identity(flat, "java")
        _require_same_identity(pb, flat, "java")
    return measured


def _validate_js_rows(payload, *, normalized: bool = False) -> list[dict]:
    if isinstance(payload, dict) and "plan" in payload:
        plan = payload["plan"]
        if plan.get("repeat_count") != 4 or "clava-js" not in plan.get("suites", []):
            raise ValueError("Expected a four-round Clava-JS matrix")
    rows = _rows(payload)
    pb = _validate_cell(rows, "clava-js", "warm", "protobuf", normalized_js=normalized)
    flat = _validate_cell(rows, "clava-js", "warm", "flatbuffers", normalized_js=normalized)
    _require_stable_identity(pb, "clava-js")
    _require_stable_identity(flat, "clava-js")
    _require_same_identity(pb, flat, "clava-js")
    for cell in (pb, flat):
        for row in cell.values():
            if (row.get("total_tests"), row.get("passed_tests"), row.get("failed_tests"),
                    row.get("skipped_tests")) != (164, 158, 0, 6):
                raise ValueError("JS warm comparison requires the accepted 164-test outcome")
    return rows


def _pair_values(left: dict[int, dict], right: dict[int, dict], metric: str) -> list[float]:
    result = []
    for repeat in REPEATS:
        a, b = left[repeat], right[repeat]
        if metric == "outside_junit_s":
            left_value = float(a["elapsed_s"]) - float(a["junit_aggregate_s"])
            right_value = float(b["elapsed_s"]) - float(b["junit_aggregate_s"])
        else:
            left_value, right_value = float(a[metric]), float(b[metric])
        delta = right_value - left_value
        if not math.isfinite(delta):
            raise ValueError(f"Non-finite paired value for {metric}")
        result.append(delta)
    return result


def paired_data(js_original, java_matrix: dict, normalized_js=None) -> dict:
    """Validate cohorts and return round-paired deltas (candidate minus baseline)."""
    original_rows = _validate_js_rows(js_original)
    java_rows = _validate_java_matrix(java_matrix)
    java = []
    for mode in MODES:
        pb = _validate_cell(java_rows, "java", mode, "protobuf")
        flat = _validate_cell(java_rows, "java", mode, "flatbuffers")
        for metric, label in JAVA_METRICS:
            values = _pair_values(pb, flat, metric)
            java.append({
                "suite": "java", "cohort": "original", "mode": mode,
                "metric": metric, "label": label, "n_pairs": len(values),
                "values": values, "median_delta_s": statistics.median(values),
                "definition": "FlatBuffers minus Protobuf; negative means FlatBuffers was faster",
            })

    js = {"original": _js_warm_comparison(original_rows, "original")}
    if normalized_js is not None:
        normalized_rows = _validate_js_rows(normalized_js, normalized=True)
        js["normalized"] = _js_warm_comparison(normalized_rows, "syntax-normalized")
    return {"java": java, "js": js}


def _js_warm_comparison(rows: list[dict], cohort: str) -> dict:
    pb = _validate_cell(rows, "clava-js", "warm", "protobuf", normalized_js=(cohort != "original"))
    flat = _validate_cell(rows, "clava-js", "warm", "flatbuffers", normalized_js=(cohort != "original"))
    values = _pair_values(pb, flat, "elapsed_s")
    return {
        "suite": "clava-js", "cohort": cohort, "mode": "warm", "metric": "elapsed_s",
        "label": "Warm command wall", "n_pairs": len(values), "values": values,
        "median_delta_s": statistics.median(values),
        "definition": "FlatBuffers minus Protobuf; negative means FlatBuffers was faster",
    }


def _scale_limit(values: list[float]) -> float:
    return max(0.5, math.ceil(max((abs(value) for value in values), default=0.0) * 2.2) / 2)


def java_svg(rows: list[dict]) -> str:
    values = [value for row in rows for value in row["values"]]
    limit = _scale_limit(values)
    left, right = 108.0, 268.0
    zero = (left + right) / 2
    scale = (right - left) / (2 * limit)
    x = lambda value: zero + value * scale
    height = 324
    output = [f'<svg class="paired-chart" viewBox="0 0 360 {height}" role="img" aria-label="Java OFF paired differences, FlatBuffers minus Protobuf, in seconds">']
    for value in (-limit, 0.0, limit):
        position = x(value)
        label = "0 s" if value == 0 else f'{value:+.1f} s'
        anchor = "start" if value < 0 else ("end" if value > 0 else "middle")
        output.append(f'<text class="axis" x="{position:.1f}" y="18" text-anchor="{anchor}">{html.escape(label)}</text>')
        output.append(f'<line class="{("zero" if value == 0 else "gridline")}" x1="{position:.1f}" x2="{position:.1f}" y1="25" y2="315"/>')

    for mode in MODES:
        top = 43 + MODES.index(mode) * 92
        output.append(f'<text class="mode" x="4" y="{top}">{html.escape(mode.title())}</text>')
        for row in (item for item in rows if item["mode"] == mode):
            metric_index = JAVA_METRICS.index((row["metric"], row["label"]))
            y = top + 24 + metric_index * 21
            data = row["values"]
            median = row["median_delta_s"]
            colour = "negative" if median < 0 else ("positive" if median > 0 else "neutral")
            output.append(f'<text class="metric" x="4" y="{y + 4}">{html.escape(row["label"])}</text>')
            output.append(f'<line class="range {colour}" x1="{x(min(data)):.2f}" x2="{x(max(data)):.2f}" y1="{y}" y2="{y}"/>')
            for pair_index, value in enumerate(data):
                cy = y + (-4.5 + pair_index * 3)
                output.append(f'<circle class="point {colour}" cx="{x(value):.2f}" cy="{cy:.1f}" r="2.8"><title>Round {pair_index + 1}: {value:+.4f} s</title></circle>')
            output.append(f'<line class="median {colour}" x1="{x(median):.2f}" x2="{x(median):.2f}" y1="{y - 8}" y2="{y + 8}"/>')
            output.append(f'<text class="value" x="357" y="{y + 4}" text-anchor="end">{median:+.2f} s</text>')
    output.append('</svg>')
    return "".join(output)


def js_svg(comparisons: dict[str, dict]) -> str:
    cohorts = list(comparisons.items())
    values = [value for _, row in cohorts for value in row["values"]]
    limit = _scale_limit(values)
    left, right = 116.0, 278.0
    zero = (left + right) / 2
    scale = (right - left) / (2 * limit)
    x = lambda value: zero + value * scale
    height = 70 + len(cohorts) * 54
    output = [f'<svg class="paired-chart" viewBox="0 0 360 {height}" role="img" aria-label="Warm Clava-JS paired differences, FlatBuffers minus Protobuf, in seconds">']
    for value in (-limit, 0.0, limit):
        position = x(value)
        label = "0 s" if value == 0 else f'{value:+.1f} s'
        anchor = "start" if value < 0 else ("end" if value > 0 else "middle")
        output.append(f'<text class="axis" x="{position:.1f}" y="18" text-anchor="{anchor}">{html.escape(label)}</text>')
        output.append(f'<line class="{("zero" if value == 0 else "gridline")}" x1="{position:.1f}" x2="{position:.1f}" y1="25" y2="{height - 8}"/>')
    for index, (cohort, row) in enumerate(cohorts):
        y = 48 + index * 54
        values = row["values"]
        median = row["median_delta_s"]
        colour = "negative" if median < 0 else ("positive" if median > 0 else "neutral")
        label = "Original" if cohort == "original" else "Syntax-normalized"
        output.append(f'<text class="metric" x="4" y="{y + 4}">{label}</text>')
        output.append(f'<line class="range {colour}" x1="{x(min(values)):.2f}" x2="{x(max(values)):.2f}" y1="{y}" y2="{y}"/>')
        for pair_index, value in enumerate(values):
            cy = y + (-5 + pair_index * 3.3)
            output.append(f'<circle class="point {colour}" cx="{x(value):.2f}" cy="{cy:.1f}" r="3"><title>{html.escape(label)} round {pair_index + 1}: {value:+.4f} s</title></circle>')
        output.append(f'<line class="median {colour}" x1="{x(median):.2f}" x2="{x(median):.2f}" y1="{y - 10}" y2="{y + 10}"/>')
        output.append(f'<text class="value" x="357" y="{y + 4}" text-anchor="end">{median:+.2f} s</text>')
    output.append('</svg>')
    return "".join(output)


def render_html(data: dict) -> str:
    js_rows = data["js"]
    js_caption = "Original accepted rows" + (" and a separate syntax-normalized warm control" if "normalized" in js_rows else "")
    js_content = js_svg(js_rows)
    return f'''<section class="pb-flat-head-to-head" aria-label="Paired Protobuf and FlatBuffers comparison">
<style>
.pb-flat-head-to-head {{ color:var(--ink,#18212e); font:15px/1.4 system-ui,sans-serif; min-width:0; }}
.pb-flat-head-to-head h2 {{ margin:0 0 6px; font-size:20px; line-height:1.25; }}
.pb-flat-head-to-head h3 {{ margin:14px 0 4px; font-size:16px; }}
.pb-flat-head-to-head p {{ margin:5px 0 10px; color:var(--muted,#536174); }}
.pb-flat-head-to-head .paired-chart {{ display:block; width:100%; max-width:520px; height:auto; overflow:visible; }}
.pb-flat-head-to-head text {{ font-family:system-ui,sans-serif; fill:var(--ink,#18212e); }}
.pb-flat-head-to-head .axis {{ font-size:12px; fill:var(--muted,#536174); }}
.pb-flat-head-to-head .mode {{ font-size:14px; font-weight:700; }}
.pb-flat-head-to-head .metric {{ font-size:12px; }}
.pb-flat-head-to-head .value {{ font:12px ui-monospace,monospace; }}
.pb-flat-head-to-head .gridline {{ stroke:var(--line,#d5dfe8); stroke-dasharray:3 4; }}
.pb-flat-head-to-head .zero {{ stroke:var(--muted,#536174); stroke-width:1.4; }}
.pb-flat-head-to-head .range {{ stroke-width:2; }}
.pb-flat-head-to-head .median {{ stroke-width:2.4; }}
.pb-flat-head-to-head .negative {{ stroke:var(--good,#08744c); fill:var(--good,#08744c); }}
.pb-flat-head-to-head .positive {{ stroke:var(--bad,#ae3535); fill:var(--bad,#ae3535); }}
.pb-flat-head-to-head .neutral {{ stroke:var(--muted,#536174); fill:var(--muted,#536174); }}
.pb-flat-head-to-head .legend {{ display:flex; flex-wrap:wrap; gap:8px 16px; margin:5px 0; color:var(--muted,#536174); font-size:12px; }}
.pb-flat-head-to-head .legend span {{ white-space:nowrap; }}
@media(max-width:400px) {{ .pb-flat-head-to-head .paired-chart {{ max-width:100%; }} }}
</style>
<h2>Protobuf vs FlatBuffers · paired runtime</h2>
<p>FlatBuffers − Protobuf; negative means FlatBuffers was faster. Each point is one of four matched rounds; the marker is the exact median of round deltas, not the difference of stage medians.</p>
<h3>Java OFF · 116-test suite</h3>
{java_svg(data["java"])}
<p>Wall and JUnit sums are separate; outside-JUnit is calculated per run as wall minus JUnit. It is an unassigned residual, not parser time.</p>
<div class="legend"><span>● four paired rounds</span><span>│ paired median</span><span>green: Flat faster</span><span>red: Flat slower</span></div>
<h3>Clava-JS · warm · {html.escape(js_caption)}</h3>
<p>Whole command wall time, shown separately from the Java suite; normalized observations are not combined with the original cohort.</p>
{js_content}
</section>'''


def csv_text(data: dict) -> str:
    fields = ("suite", "cohort", "mode", "metric", "repeat", "delta_s", "definition")
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    for row in data["java"]:
        for repeat, value in zip(REPEATS, row["values"]):
            writer.writerow({"suite": row["suite"], "cohort": row["cohort"], "mode": row["mode"],
                             "metric": row["metric"], "repeat": repeat, "delta_s": f"{value:.6f}",
                             "definition": row["definition"]})
    for comparison in data["js"].values():
        for repeat, value in zip(REPEATS, comparison["values"]):
            writer.writerow({"suite": comparison["suite"], "cohort": comparison["cohort"],
                             "mode": comparison["mode"], "metric": comparison["metric"],
                             "repeat": repeat, "delta_s": f"{value:.6f}",
                             "definition": comparison["definition"]})
    return output.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--js-matrix", type=Path, default=DEFAULT_JS)
    parser.add_argument("--java-matrix", type=Path, default=DEFAULT_JAVA)
    parser.add_argument("--normalized-js", type=Path,
                        help="Optional completed warm JS rows with fast_syntax=true")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV)
    args = parser.parse_args()
    data = paired_data(read_json(args.js_matrix), read_json(args.java_matrix),
                       read_json(args.normalized_js) if args.normalized_js else None)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render_html(data), encoding="utf-8")
    args.csv.write_text(csv_text(data), encoding="utf-8")
    print(json.dumps({"html": str(args.output), "csv": str(args.csv),
                      "java_points": sum(len(row["values"]) for row in data["java"]),
                      "js_cohorts": list(data["js"]), "validated": True}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
