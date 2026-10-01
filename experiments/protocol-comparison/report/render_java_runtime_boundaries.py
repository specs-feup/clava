#!/usr/bin/env python3
"""Render compact Java wall/JUnit boundary and JaCoCo-control evidence."""

from __future__ import annotations

import argparse
import csv
import html
import json
import statistics
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
MATRIX_ROOT = (
    REPOSITORY_ROOT
    / "experiments/protocol-comparison/results/deadline-20260930/matrix-r2"
)
FRESH_ROOT = MATRIX_ROOT / "java-noagent-matrix-r2"
DEFAULT_HTML = MATRIX_ROOT / "decision-noagent-r1/java-runtime-boundaries.html"
DEFAULT_CSV = MATRIX_ROOT / "decision-noagent-r1/java-runtime-boundaries.csv"

METRICS = (
    ("elapsed_s", "wall"),
    ("junit_aggregate_s", "JUnit sum"),
    ("outside_junit_s", "outside"),
)
PRIMARY_MODES = ("direct", "cold", "warm")
COLORS = {"positive": "#bd4a21", "negative": "#246f9b"}


def load_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def rounded_median(values: list[float]) -> float:
    return round(statistics.median(values), 6)


def primary_rows(matrix: dict, paired: dict) -> list[dict]:
    require(matrix.get("plan", {}).get("suites") == ["java"]
            and matrix["plan"].get("repeat_count") == 4,
            "expected the Java-only four-round fresh matrix")
    rows = matrix["results"]
    measured = [row for row in rows if row.get("selected") and row.get("measured")]
    require(len(measured) == 40, "expected 40 selected fresh OFF measurements")
    require(all(row.get("valid") and row.get("agent") == "off" for row in measured),
            "fresh matrix contains an invalid or agent-on primary row")
    require(all((row.get("passed_tests"), row.get("failed_tests"), row.get("skipped_tests"))
                == (116, 0, 0) for row in measured),
            "fresh matrix does not preserve the 116-test result")
    require(all(row.get("no_explicit_gc_flags") is True for row in measured),
            "fresh matrix has an explicit-GC worker argument")
    require(all(not any("javaagent" in arg for arg in row.get("actual_test_executor_args", []))
                for row in measured),
            "fresh OFF worker argv contains a javaagent")
    require(all(row.get("report_tasks_skipped", {}).get("jacocoTestReport") is True
                and row.get("report_tasks_skipped", {}).get(
                    "jacocoTestCoverageVerification") is True for row in measured),
            "fresh matrix did not skip both JaCoCo report tasks")
    require(paired.get("rows") == 40 and paired.get("n_per_stage_mode") == 4,
            "paired summary does not describe the 40-row, four-round matrix")

    comparisons = paired["paired_gaps_vs_text_by_mode"]
    output = []
    for mode in PRIMARY_MODES:
        for field, label in METRICS:
            summary_field = "gradle_non_test_elapsed_s" if field == "outside_junit_s" else field
            stat_field = f"median_{summary_field}_gap"
            value = comparisons[mode]["protobuf"][stat_field]
            output.append({
                "comparison": "protobuf_minus_text",
                "condition": mode,
                "stage": "protobuf-minus-text",
                "n_pairs": 4,
                "metric": field,
                "label": label,
                "median_delta_s": round(value, 6),
                "definition": "Protobuf minus Text; positive means Protobuf took longer",
            })
    return output


def diagnostic_rows(matrix: dict, diagnostics: dict) -> list[dict]:
    runs = diagnostics.get("results", [])
    pairs = diagnostics.get("paired_diagnostics", [])
    require(len(runs) == 16 and len(pairs) == 16,
            "expected 16 separate JaCoCo-on direct/cold diagnostic pairs")
    require(all(run.get("valid") and run.get("agent") == "on" for run in runs),
            "JaCoCo-on diagnostics contain invalid or non-agent-on rows")
    require(all((run.get("passed_tests"), run.get("failed_tests"), run.get("skipped_tests"))
                == (116, 0, 0) for run in runs),
            "JaCoCo-on diagnostics do not preserve the 116-test result")
    require(all(run.get("no_explicit_gc_flags") is True
                and run.get("report_tasks_skipped", {}).get("jacocoTestReport") is True
                and run.get("report_tasks_skipped", {}).get(
                    "jacocoTestCoverageVerification") is True for run in runs),
            "JaCoCo-on diagnostics changed the GC or report-task controls")

    off_rows = {
        (row["stage"], row["mode"], row["repeat"]): row
        for row in matrix["results"]
        if row.get("selected") and row.get("measured")
    }
    grouped: dict[tuple[str, str], list[dict]] = {}
    for pair in pairs:
        key = (pair["stage"], pair["mode"])
        grouped.setdefault(key, []).append(pair)
        require(pair.get("on_agent_args") and any(
            "javaagent" in arg for arg in pair["on_agent_args"]
        ), "ON diagnostic is missing its observed JaCoCo agent argument")
        require(pair.get("off_agent_args") == [],
                "paired OFF row contains an unexpected agent argument")
        off = off_rows.get((pair["stage"], pair["mode"], pair["round"]))
        require(off is not None, "diagnostic OFF half has no matching primary row")
        require(abs(float(off["elapsed_s"]) - float(pair["off_elapsed_s"])) < 1e-6
                and abs(float(off["junit_aggregate_s"])
                        - float(pair["off_junit_aggregate_s"])) < 1e-6,
                "diagnostic OFF half differs from its primary OFF run")

    require(set(grouped) == {
        ("ccache-text", "direct"), ("protobuf", "direct"),
        ("ccache-text", "cold"), ("protobuf", "cold"),
    }, "unexpected JaCoCo diagnostic stage/mode set")
    require(all(len(cell) == 4 for cell in grouped.values()),
            "each JaCoCo diagnostic cell must have four paired rounds")

    output = []
    metric_fields = {
        "elapsed_s": "on_minus_off_s",
        "junit_aggregate_s": "on_minus_off_junit_s",
        "outside_junit_s": "on_minus_off_outside_junit_s",
    }
    for (stage, mode), cell in sorted(grouped.items()):
        for metric, pair_field in metric_fields.items():
            label = dict(METRICS)[metric]
            output.append({
                "comparison": "jacoco_on_minus_off",
                "condition": mode,
                "stage": stage,
                "n_pairs": 4,
                "metric": metric,
                "label": label,
                "median_delta_s": rounded_median(
                    [float(row[pair_field]) for row in cell]
                ),
                "definition": "JaCoCo ON minus OFF; positive means ON took longer",
            })
    return output


def fmt(value: float) -> str:
    rounded = Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return f"{rounded:+.2f} s"


def primary_svg(rows: list[dict]) -> str:
    values = {(row["condition"], row["metric"]): row["median_delta_s"] for row in rows}
    x_zero = 194
    scale = 120
    plot_min = x_zero - 0.6 * scale
    plot_max = x_zero + 0.6 * scale
    height = 354
    out = [f'<svg class="chart" viewBox="0 0 360 {height}" role="img" aria-label="Paired median differences, Protobuf minus Text, in seconds">']
    out.append('<text class="axis" x="122" y="19">−0.60 s</text><text class="axis" x="194" y="19" text-anchor="middle">0 s</text><text class="axis" x="266" y="19" text-anchor="end">+0.60 s</text>')
    out.append(f'<line class="tick" x1="{plot_min:.1f}" y1="28" x2="{plot_min:.1f}" y2="346"/><line class="zero" x1="{x_zero}" y1="28" x2="{x_zero}" y2="346"/><line class="tick" x1="{plot_max:.1f}" y1="28" x2="{plot_max:.1f}" y2="346"/>')
    for index, mode in enumerate(PRIMARY_MODES):
        top = 45 + index * 100
        out.append(f'<text class="group" x="5" y="{top}">{html.escape(mode)}</text>')
        for metric_index, (metric, label) in enumerate(METRICS):
            y = top + 30 + metric_index * 26
            value = values[(mode, metric)]
            end = x_zero + value * scale
            x = min(x_zero, end)
            width = abs(end - x_zero)
            direction = "positive" if value >= 0 else "negative"
            out.append(f'<text class="rowlabel" x="5" y="{y + 3}">{html.escape(label)}</text>')
            out.append(f'<rect class="{direction}" x="{x:.2f}" y="{y - 9}" width="{width:.2f}" height="15" rx="3"/>')
            out.append(f'<text class="value" x="271" y="{y + 4}">{fmt(value)}</text>')
    out.append('</svg>')
    return "".join(out)


def causal_svg(rows: list[dict]) -> str:
    values = {(row["condition"], row["stage"]): row["median_delta_s"]
              for row in rows if row["metric"] == "elapsed_s"}
    groups = (
        ("direct", "ccache-text", "direct · Text"),
        ("direct", "protobuf", "direct · PB"),
        ("cold", "ccache-text", "cold · Text"),
        ("cold", "protobuf", "cold · PB"),
    )
    x_zero = 191
    scale = 42.0
    max_delta = 1.8
    height = 166
    out = [f'<svg class="chart" viewBox="0 0 360 {height}" role="img" aria-label="JaCoCo agent ON minus OFF paired median wall-time differences in seconds">']
    out.append('<text class="axis" x="115" y="19">−1.80 s</text><text class="axis" x="191" y="19" text-anchor="middle">0 s</text><text class="axis" x="267" y="19" text-anchor="end">+1.80 s</text>')
    out.append(f'<line class="tick" x1="{x_zero - max_delta * scale:.1f}" y1="28" x2="{x_zero - max_delta * scale:.1f}" y2="158"/><line class="zero" x1="{x_zero}" y1="28" x2="{x_zero}" y2="158"/><line class="tick" x1="{x_zero + max_delta * scale:.1f}" y1="28" x2="{x_zero + max_delta * scale:.1f}" y2="158"/>')
    for group_index, (condition, stage, title) in enumerate(groups):
        y = 49 + group_index * 31
        value = values[(condition, stage)]
        end = x_zero + value * scale
        x = min(x_zero, end)
        width = abs(end - x_zero)
        direction = "positive" if value >= 0 else "negative"
        out.append(f'<text class="group" x="5" y="{y + 4}">{html.escape(title)}</text>')
        out.append(f'<rect class="{direction}" x="{x:.2f}" y="{y - 10}" width="{width:.2f}" height="15" rx="3"/>')
        out.append(f'<text class="value" x="272" y="{y + 4}">{fmt(value)}</text>')
    out.append('</svg>')
    return "".join(out)


def render_html(primary: list[dict], causal: list[dict]) -> str:
    return f'''<section class="java-boundaries" aria-label="Java runtime evidence">
<style>
.java-boundaries {{ color: #202a34; font: 16px/1.4 system-ui, sans-serif; }}
.java-boundaries .title {{ margin: 0 0 6px; font-size: 20px; line-height: 1.25; }}
.java-boundaries .subtitle, .java-boundaries .caption {{ margin: 5px 0; }}
.java-boundaries .chart-scroll {{ max-width: 100%; overflow-x: auto; }}
.java-boundaries .chart {{ display: block; width: 360px; height: auto; margin: 6px 0 0; }}
.java-boundaries .axis {{ font: 15px system-ui, sans-serif; fill: #56636e; }}
.java-boundaries .group, .java-boundaries .rowlabel {{ font: 15px system-ui, sans-serif; fill: #28343e; }}
.java-boundaries .group {{ font-weight: 650; }}
.java-boundaries .value {{ font: 15px ui-monospace, monospace; fill: #28343e; }}
.java-boundaries .tick {{ stroke: #d9e0e5; stroke-width: 1; }}
.java-boundaries .zero {{ stroke: #596772; stroke-width: 1.4; }}
.java-boundaries .positive {{ fill: {COLORS['positive']}; }}
.java-boundaries .negative {{ fill: {COLORS['negative']}; }}
.java-boundaries details {{ margin-top: 12px; }}
.java-boundaries summary {{ cursor: pointer; }}
.java-boundaries .note {{ margin: 6px 0; }}
.java-boundaries .csv-link {{ display: inline-block; margin-top: 8px; }}
</style>
<h2 class="title">Java protocol runtime differences</h2>
<p class="subtitle">116 test bodies include setup, codegen, and assertions; these are not parse-only timings.</p>
<p class="caption">Protobuf − Text · JaCoCo off · paired median, n=4 · positive means Protobuf slower.</p>
<div class="chart-scroll">
{primary_svg(primary)}
</div>
<p class="caption">Outside = wall − JUnit sum; unassigned, not parser or coverage time. Reports skipped; no explicit GC.</p>
<details>
<summary>JaCoCo wall effect · ON − OFF · direct and cold · four paired rounds</summary>
<p class="note">Wall times only. JUnit and outside-JUnit deltas are in the CSV. Outside-JUnit is unassigned, not parser or coverage time. Reports were skipped; no explicit GC was requested.</p>
<div class="chart-scroll">
{causal_svg(causal)}
 </div>
</details>
<a class="csv-link" href="java-runtime-boundaries.csv" data-csv-href="java-runtime-boundaries.csv">CSV data</a>
</section>
'''


def write_csv(path: Path, rows: list[dict]) -> None:
    fields = ("comparison", "condition", "stage", "n_pairs", "metric", "median_delta_s", "definition")
    with path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row[field] for field in fields})


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=FRESH_ROOT / "results.json")
    parser.add_argument("--paired-summary", type=Path, default=FRESH_ROOT / "paired-summary.json")
    parser.add_argument("--diagnostics", type=Path, default=FRESH_ROOT / "agent-diagnostics.json")
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV)
    args = parser.parse_args()

    matrix = load_json(args.matrix)
    paired = load_json(args.paired_summary)
    diagnostics = load_json(args.diagnostics)
    primary = primary_rows(matrix, paired)
    fresh_agent = diagnostic_rows(matrix, diagnostics)
    causal = fresh_agent
    output_rows = primary + fresh_agent

    args.html.parent.mkdir(parents=True, exist_ok=True)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    args.html.write_text(render_html(primary, causal), encoding="utf-8")
    write_csv(args.csv, output_rows)
    print(json.dumps({
        "html": str(args.html),
        "csv": str(args.csv),
        "primary_points": len(primary),
        "fresh_agent_points": len(fresh_agent),
        "csv_rows": len(output_rows),
        "validated": True,
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
