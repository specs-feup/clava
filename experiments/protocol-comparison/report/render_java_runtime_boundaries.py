#!/usr/bin/env python3
"""Render compact Java wall/JUnit boundary and JaCoCo-control evidence."""

from __future__ import annotations

import argparse
import csv
import html
import json
import statistics
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
MATRIX_ROOT = (
    REPOSITORY_ROOT
    / "experiments/protocol-comparison/results/deadline-20260930/matrix-r2"
)
FRESH_ROOT = MATRIX_ROOT / "java-noagent-matrix-r2"
WARM_ROOT = MATRIX_ROOT / "java-jacoco-agent-control-r4"
DEFAULT_HTML = MATRIX_ROOT / "decision-noagent-r1/java-runtime-boundaries.html"
DEFAULT_CSV = MATRIX_ROOT / "decision-noagent-r1/java-runtime-boundaries.csv"

METRICS = (
    ("elapsed_s", "wall"),
    ("junit_aggregate_s", "JUnit sum"),
    ("outside_junit_s", "outside"),
)
PRIMARY_MODES = ("direct", "cold", "warm")
STAGE_LABELS = {"ccache-text": "Text", "protobuf": "PB"}
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


def normalized_stage_identity(stage: dict, old_stage: dict) -> dict:
    old = old_stage["identity"]
    return {
        "clava_revision": stage.get("clava_revision"),
        "clava_patch_sha256": stage.get("clava_patch_sha256"),
        "native_revision": stage.get("native_revision"),
        "native_binary_sha256": stage.get("native_binary_sha256"),
        "runtime_manifest_sha256": stage.get("runtime_manifest_sha256"),
        "parser_jar_sha256": stage.get("parser_jar_sha256"),
    }, {
        "clava_revision": old.get("clava_revision"),
        "clava_patch_sha256": old.get("clava_patch_sha256"),
        "native_revision": old.get("native_revision"),
        "native_binary_sha256": old.get("native_tool_sha256"),
        "runtime_manifest_sha256": old.get("runtime_manifest", {}).get("sha256"),
        "parser_jar_sha256": old.get("parser_jar_sha256"),
    }


def prior_warm_rows(matrix: dict, warm: dict, summary: dict) -> list[dict]:
    old_rows = warm.get("results", [])
    require(len(old_rows) == 16 and all(row.get("valid") for row in old_rows),
            "prior warm control must contain 16 valid ON/OFF invocations")
    require(all((row.get("passed_tests"), row.get("failed_tests"), row.get("skipped_tests"))
                == (116, 0, 0) for row in old_rows),
            "prior warm control does not preserve the 116-test result")
    require(all(row.get("test_identity_sha256") ==
                "11660f260465c336e4b34d961858247df15975ac1053daddb6ba8eb56eb03450"
                for row in old_rows), "prior warm test identity differs")
    require(all(row.get("test_task_executed") is True
                and row.get("test_worker_xmx_512m") is True
                and row.get("worker_args_stable_except_agent") is True
                and row.get("actual_test_executor_only_jacoco_agent") is True
                and row.get("report_tasks_skipped", {}).get("jacocoTestReport") is True
                and row.get("report_tasks_skipped", {}).get(
                    "jacocoTestCoverageVerification") is True
                for row in old_rows), "prior warm worker/report settings differ")
    require(warm["plan"].get("rounds") == 4,
            "prior warm control is not a four-round paired run")

    fresh_stages = matrix["plan"]["stages"]
    for stage_name in ("ccache-text", "protobuf"):
        current, old = normalized_stage_identity(
            fresh_stages[stage_name], warm["plan"]["stages"][stage_name]
        )
        require(current == old, f"prior warm {stage_name} stage identity differs")

    # Require four executions per arm and stage before using the summarized pairs.
    for stage_name in ("ccache-text", "protobuf"):
        for agent in ("on", "off"):
            count = sum(row.get("stage") == stage_name and row.get("agent") == agent
                        for row in old_rows)
            require(count == 4, f"prior warm {stage_name}/{agent} count is not four")

    stage_metrics = {
        "elapsed_s": "elapsed_s",
        "junit_aggregate_s": "junit_aggregate_s",
        "outside_junit_s": "wall_minus_junit_residual_s",
    }
    output = []
    for stage_name in ("ccache-text", "protobuf"):
        stage_summary = summary["by_stage"][stage_name]
        for metric, summary_metric in stage_metrics.items():
            value = stage_summary[summary_metric]["on_minus_off_paired_median"]
            output.append({
                "comparison": "jacoco_on_minus_off_prior_warm",
                "condition": "warm-prior",
                "stage": stage_name,
                "n_pairs": 4,
                "metric": metric,
                "label": dict(METRICS)[metric],
                "median_delta_s": round(value, 6),
                "definition": "Prior warm JaCoCo ON minus OFF; shown separately, not pooled",
            })
    return output


def fmt(value: float) -> str:
    return f"{value:+.4f}".rstrip("0").rstrip(".")


def primary_svg(rows: list[dict]) -> str:
    values = {(row["condition"], row["metric"]): row["median_delta_s"] for row in rows}
    x_zero = 199
    scale = 142
    plot_min = x_zero - 0.6 * scale
    plot_max = x_zero + 0.6 * scale
    height = 240
    out = [f'<svg class="chart" viewBox="0 0 360 {height}" role="img" aria-label="Paired median differences, Protobuf minus Text, in seconds">']
    out.append('<text class="axis" x="112" y="13">−0.6 s</text><text class="axis" x="199" y="13" text-anchor="middle">0</text><text class="axis" x="285" y="13" text-anchor="end">+0.6 s</text>')
    out.append(f'<line class="tick" x1="{plot_min:.1f}" y1="20" x2="{plot_min:.1f}" y2="232"/><line class="zero" x1="{x_zero}" y1="20" x2="{x_zero}" y2="232"/><line class="tick" x1="{plot_max:.1f}" y1="20" x2="{plot_max:.1f}" y2="232"/>')
    for index, mode in enumerate(PRIMARY_MODES):
        top = 38 + index * 66
        out.append(f'<text class="group" x="5" y="{top}">{html.escape(mode)}</text>')
        for metric_index, (metric, label) in enumerate(METRICS):
            y = top + 16 + metric_index * 14
            value = values[(mode, metric)]
            end = x_zero + value * scale
            x = min(x_zero, end)
            width = abs(end - x_zero)
            direction = "positive" if value >= 0 else "negative"
            out.append(f'<text class="rowlabel" x="5" y="{y + 3}">{html.escape(label)}</text>')
            out.append(f'<rect class="{direction}" x="{x:.2f}" y="{y - 5}" width="{width:.2f}" height="8" rx="2"/>')
            out.append(f'<text class="value" x="294" y="{y + 3}">{fmt(value)}</text>')
    out.append('</svg>')
    return "".join(out)


def causal_svg(rows: list[dict]) -> str:
    values = {(row["condition"], row["stage"], row["metric"]): row["median_delta_s"]
              for row in rows}
    groups = (
        ("direct", "ccache-text", "direct · Text"),
        ("direct", "protobuf", "direct · PB"),
        ("cold", "ccache-text", "cold · Text"),
        ("cold", "protobuf", "cold · PB"),
        ("warm-prior", "ccache-text", "warm prior · Text"),
        ("warm-prior", "protobuf", "warm prior · PB"),
    )
    x_zero = 191
    scale = 42.0
    max_delta = 1.8
    height = 356
    out = [f'<svg class="chart" viewBox="0 0 360 {height}" role="img" aria-label="JaCoCo agent ON minus OFF paired median timing differences in seconds">']
    out.append('<text class="axis" x="115" y="13">−1.8 s</text><text class="axis" x="191" y="13" text-anchor="middle">0</text><text class="axis" x="267" y="13" text-anchor="end">+1.8 s</text>')
    out.append(f'<line class="tick" x1="{x_zero - max_delta * scale:.1f}" y1="20" x2="{x_zero - max_delta * scale:.1f}" y2="350"/><line class="zero" x1="{x_zero}" y1="20" x2="{x_zero}" y2="350"/><line class="tick" x1="{x_zero + max_delta * scale:.1f}" y1="20" x2="{x_zero + max_delta * scale:.1f}" y2="350"/>')
    for group_index, (condition, stage, title) in enumerate(groups):
        top = 31 + group_index * 54
        out.append(f'<text class="group" x="5" y="{top}">{html.escape(title)}</text>')
        for metric_index, (metric, label) in enumerate(METRICS):
            y = top + 13 + metric_index * 12
            value = values[(condition, stage, metric)]
            end = x_zero + value * scale
            x = min(x_zero, end)
            width = abs(end - x_zero)
            direction = "positive" if value >= 0 else "negative"
            out.append(f'<text class="rowlabel" x="5" y="{y + 3}">{html.escape(label)}</text>')
            out.append(f'<rect class="{direction}" x="{x:.2f}" y="{y - 4}" width="{width:.2f}" height="7" rx="2"/>')
            out.append(f'<text class="value" x="272" y="{y + 3}">{fmt(value)}</text>')
    out.append('</svg>')
    return "".join(out)


def render_html(primary: list[dict], causal: list[dict]) -> str:
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Java runtime boundaries</title>
<style>
:root {{ color-scheme: light; font: 14px/1.45 system-ui, sans-serif; color: #202a34; background: #f2f5f7; }}
body {{ margin: 0 auto; padding: 20px 14px 32px; max-width: 540px; }}
main {{ background: #fff; border: 1px solid #dce3e8; border-radius: 12px; padding: 18px; }}
h1 {{ font-size: 1.25rem; margin: 0 0 6px; }}
h2 {{ font-size: 1rem; margin: 20px 0 4px; }}
p {{ margin: 6px 0; color: #4e5b67; }}
.chart {{ display: block; width: min(100%, 360px); height: auto; margin: 5px 0 0; overflow: visible; }}
.axis {{ font: 10px system-ui, sans-serif; fill: #56636e; }}
.group {{ font: 600 11px system-ui, sans-serif; fill: #28343e; }}
.rowlabel {{ font: 10px system-ui, sans-serif; fill: #52606b; }}
.value {{ font: 10px ui-monospace, monospace; fill: #28343e; }}
.tick {{ stroke: #d9e0e5; stroke-width: 1; }}
.zero {{ stroke: #596772; stroke-width: 1.25; }}
.positive {{ fill: {COLORS['positive']}; }}
.negative {{ fill: {COLORS['negative']}; }}
.note {{ font-size: .82rem; }}
.legend {{ display: flex; gap: 12px; font-size: .78rem; color: #52606b; margin-top: 4px; }}
.swatch {{ display: inline-block; width: 9px; height: 9px; border-radius: 2px; margin-right: 4px; }}
.positive-swatch {{ background: {COLORS['positive']}; }}
.negative-swatch {{ background: {COLORS['negative']}; }}
@media (max-width: 390px) {{ main {{ padding: 13px 10px; }} body {{ padding: 10px 8px; }} }}
</style>
</head>
<body>
<main>
<h1>Java runtime boundaries</h1>
<p>Each run covers 116 test bodies, including setup, codegen, and assertions. This is not parse-only time.</p>
<h2>Protobuf − Text · JaCoCo off</h2>
<p>Paired medians across four fresh rounds; positive means Protobuf took longer.</p>
{primary_svg(primary)}
<div class="legend"><span><i class="swatch positive-swatch"></i>longer</span><span><i class="swatch negative-swatch"></i>shorter</span></div>
<h2>JaCoCo on − off</h2>
<p>Four paired rounds per direct/cold cell. Warm is the prior four-pair control, shown separately.</p>
{causal_svg(causal)}
<p class="note">Outside-JUnit residual is wall minus summed JUnit duration. Gradle exposed no task-level split, so the residual is unassigned, not parser or coverage time. Reports were skipped; no explicit GC was requested.</p>
</main>
</body>
</html>
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
    parser.add_argument("--warm-control", type=Path, default=WARM_ROOT / "results.json")
    parser.add_argument("--warm-summary", type=Path, default=WARM_ROOT / "summary.json")
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV)
    args = parser.parse_args()

    matrix = load_json(args.matrix)
    paired = load_json(args.paired_summary)
    diagnostics = load_json(args.diagnostics)
    warm = load_json(args.warm_control)
    warm_summary = load_json(args.warm_summary)

    primary = primary_rows(matrix, paired)
    fresh_agent = diagnostic_rows(matrix, diagnostics)
    prior_agent = prior_warm_rows(matrix, warm, warm_summary)
    causal = fresh_agent + prior_agent
    output_rows = primary + causal

    args.html.parent.mkdir(parents=True, exist_ok=True)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    args.html.write_text(render_html(primary, causal), encoding="utf-8")
    write_csv(args.csv, output_rows)
    print(json.dumps({
        "html": str(args.html),
        "csv": str(args.csv),
        "primary_points": len(primary),
        "fresh_agent_points": len(fresh_agent),
        "prior_warm_points": len(prior_agent),
        "csv_rows": len(output_rows),
        "validated": True,
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
