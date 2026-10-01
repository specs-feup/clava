#!/usr/bin/env python3
"""Render paired Java configuration-phase evidence from frozen diagnostics.

The fragment separates two isolated-cold timeline pairs from four config-only
intervention pairs. It does not add whole-command wall times or treat these
diagnostics as additional workload measurements.
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
TIMELINE_ROOT = MATRIX_ROOT / "java-runtime-timeline-r2"
CONFIG_ROOT = MATRIX_ROOT / "java-config-intervention-r1"
DEFAULT_HTML = MATRIX_ROOT / "decision-noagent-r1/java-configuration-cause.html"
DEFAULT_CSV = MATRIX_ROOT / "decision-noagent-r1/java-configuration-cause.csv"
TIMELINE_STAGES = ("ccache-text", "protobuf")
TIMELINE_ROUNDS = (1, 2)
CONFIG_PAIRS = ("pair-01", "pair-02", "pair-03", "pair-04")


def read_json(path: Path):
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def read_csv(path: Path) -> list[dict]:
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source))


def _number(value, field: str) -> float:
    try:
        parsed = float(value)
    except (TypeError, ValueError) as error:
        raise ValueError(f"Missing or invalid numeric field: {field}") from error
    if not math.isfinite(parsed):
        raise ValueError(f"Non-finite numeric field: {field}")
    return parsed


def _positive_cost(values: list[float], label: str) -> list[float]:
    if not values or any(value < 0 for value in values):
        raise ValueError(f"{label} did not produce non-negative paired overhead values")
    return values


def timeline_cost_rows(timeline: dict, phase_rows: list[dict]) -> list[dict]:
    plan = timeline.get("plan", {})
    if (plan.get("schema_version") != 1 or plan.get("status") != "complete"
            or plan.get("diagnostic_only_not_headline") is not True
            or plan.get("actual_cache_mode") != "per-command isolated cold"
            or plan.get("expected_test_count") != 116):
        raise ValueError("Expected the completed two-round isolated-cold Java timeline diagnostic")
    policy = plan.get("worker_policy", {})
    if (policy.get("JaCoCo_test_worker_agent") != "off"
            or policy.get("explicit_gc_override") is not False
            or policy.get("JaCoCo_report_tasks") != "skipped"):
        raise ValueError("Timeline worker/report controls differ from the frozen OFF diagnostic")

    results = timeline.get("results")
    if not isinstance(results, list) or len(results) != 4:
        raise ValueError("Expected two matched cold timeline pairs")
    by_key = {}
    for row in results:
        key = (int(row.get("round", -1)), row.get("stage"))
        if key in by_key or key[0] not in TIMELINE_ROUNDS or key[1] not in TIMELINE_STAGES:
            raise ValueError("Unexpected or duplicate timeline run identity")
        if (row.get("valid") is not True or row.get("return_code") != 0
                or row.get("total_tests") != 116 or row.get("passed_tests") != 116
                or row.get("failed_tests") != 0 or row.get("skipped_tests") != 0):
            raise ValueError("Timeline run failed the 116-test diagnostic gate")
        actual_mode = row.get("actual_mode")
        if actual_mode is not None and actual_mode != "per-command isolated cold":
            raise ValueError("Timeline run is not an isolated-cold invocation")
        if row.get("report_tasks_skipped", {}).get("jacocoTestReport") is not True or row.get(
                "report_tasks_skipped", {}).get("jacocoTestCoverageVerification") is not True:
            raise ValueError("Timeline run did not skip both JaCoCo report tasks")
        _number(row.get("wall_minus_junit_residual_s"), "wall_minus_junit_residual_s")
        by_key[key] = row
    expected = {(round_id, stage) for round_id in TIMELINE_ROUNDS for stage in TIMELINE_STAGES}
    if set(by_key) != expected:
        raise ValueError("Timeline rows do not contain both stages in both rounds")

    phase_by_key = {}
    for row in phase_rows:
        if row.get("phase") != "project_configuration":
            continue
        try:
            key = (int(row["round"]), row["stage"])
        except (KeyError, ValueError) as error:
            raise ValueError("Invalid project-configuration phase identity") from error
        if key[0] in TIMELINE_ROUNDS and key[1] in TIMELINE_STAGES:
            if key in phase_by_key:
                raise ValueError("Duplicate project-configuration phase boundary")
            if row.get("task_failure"):
                raise ValueError("Project-configuration timeline phase failed")
            phase_by_key[key] = _number(row.get("duration_s"), "project_configuration duration_s")
    if set(phase_by_key) != expected:
        raise ValueError("Expected exactly four project-configuration phase bounds")

    residuals = []
    configurations = []
    for round_id in TIMELINE_ROUNDS:
        text = by_key[(round_id, "ccache-text")]
        protobuf = by_key[(round_id, "protobuf")]
        residuals.append(_number(protobuf["wall_minus_junit_residual_s"], "protobuf residual")
                         - _number(text["wall_minus_junit_residual_s"], "Text residual"))
        configurations.append(phase_by_key[(round_id, "protobuf")]
                              - phase_by_key[(round_id, "ccache-text")])

    return [
        _series("Outside JUnit · PB − Text", "Timeline · two isolated-cold pairs · n=2",
                _positive_cost(residuals, "Outside-JUnit PB−Text"), "wall_minus_junit_residual_s",
                "116/116 passed; JaCoCo worker OFF; no explicit GC; report tasks skipped"),
        _series("Project config · PB − Text", "Timeline · same isolated-cold pairs · n=2",
                _positive_cost(configurations, "Project-configuration PB−Text"), "project_configuration_s",
                "116/116 passed; JaCoCo worker OFF; no explicit GC; report tasks skipped"),
    ]


def _series(label: str, scope: str, values: list[float], phase: str,
            validation_gate: str) -> dict:
    return {"label": label, "scope": scope, "values": values,
            "median_delta_s": statistics.median(values), "phase": phase,
            "validation_gate": validation_gate}


def _config_rows(payload) -> list[dict]:
    rows = payload if isinstance(payload, list) else payload.get("results") if isinstance(payload, dict) else None
    if not isinstance(rows, list) or not all(isinstance(row, dict) for row in rows):
        raise ValueError("Config intervention results must contain a results array")
    return rows


def _pair_label(row: dict) -> str:
    label = row.get("label")
    if label in CONFIG_PAIRS:
        return label
    pair = row.get("pair")
    if isinstance(pair, int) and 1 <= pair <= 4:
        return f"pair-{pair:02d}"
    if isinstance(pair, str) and pair in CONFIG_PAIRS:
        return pair
    raise ValueError("Config intervention row is missing its pair-01..pair-04 identity")


def config_intervention_cost(plan: dict, payload, timeline_plan: dict) -> dict:
    if plan.get("schema_version") != 1 or plan.get("status") != "complete":
        raise ValueError("The Java config intervention must be complete before rendering")
    if plan.get("isolated_build_file_restored_to_original") is not True:
        raise ValueError("The isolated Gradle build file was not restored after the intervention")
    if (plan.get("source_matrix_sha256") != timeline_plan.get("primary_matrix_sha256")
            or plan.get("source_worker_evidence_sha256") != timeline_plan.get("worker_evidence_matrix_sha256")):
        raise ValueError("Config intervention and timeline diagnostics do not share the frozen Java sources")
    intervention = plan.get("intervention", {})
    disabled = intervention.get("disabled", [])
    preserved = intervention.get("preserved", [])
    required_disabled = ("com.google.protobuf Gradle plugin 0.9.4 application",
                         "custom schema-hash/protoc/bindings/hash task registrations and compileJava dependencies")
    required_preserved = ("protobuf-java dependency declaration and dependency resolution context",
                          "Java Test, JaCoCo, report-skip, and fixed-test filter configuration")
    if not all(item in disabled for item in required_disabled) or not all(
            item in preserved for item in required_preserved):
        raise ValueError("Intervention does not isolate the combined build integration while preserving runtime inputs")
    invariants = plan.get("control_invariants", [])
    if ("protobuf-java dependency block retained byte-for-byte" not in invariants
            or "Java Test/task/JaCoCo configuration retained byte-for-byte" not in invariants):
        raise ValueError("Config intervention control invariants are incomplete")

    rows = [row for row in _config_rows(payload) if row.get("measured") is True]
    if len(rows) != 8:
        raise ValueError("Expected eight measured config-only rows (four original/control pairs)")
    by_pair = {pair: {} for pair in CONFIG_PAIRS}
    for row in rows:
        label = _pair_label(row)
        arm = row.get("arm")
        if arm not in ("original", "control") or arm in by_pair[label]:
            raise ValueError("Duplicate or invalid config intervention arm")
        if row.get("valid") is not True or row.get("return_code") != 0:
            raise ValueError("A config intervention command failed validation")
        phases = row.get("phases", {})
        if phases.get("test_worker_spawned") is not False or phases.get("test_actions_run") is not False:
            raise ValueError("Config intervention unexpectedly launched or ran JUnit")
        if phases.get("all_tasks_dry_run_skipped") is not True:
            raise ValueError("Config intervention must observe the complete task graph without executing tasks")
        # Parse wall time only as an integrity check; it is intentionally not plotted or added.
        if _number(row.get("whole_command_wall_s"), "whole_command_wall_s") <= 0:
            raise ValueError("Invalid intervention command duration")
        phases_copy = dict(phases)
        phases_copy["project_configuration_s"] = _number(
            phases.get("project_configuration_s"), "phases.project_configuration_s")
        by_pair[label][arm] = phases_copy
    if any(set(arms) != {"original", "control"} for arms in by_pair.values()):
        raise ValueError("Config intervention is missing an original/control pair")

    costs = []
    for label in CONFIG_PAIRS:
        arms = by_pair[label]
        costs.append(arms["original"]["project_configuration_s"]
                     - arms["control"]["project_configuration_s"])

    summary = None
    if isinstance(payload, dict):
        summary = payload.get("paired_summary")
    if summary is None:
        summary = plan.get("paired_summary")
    if not isinstance(summary, dict) or summary.get("n_pairs") != 4:
        raise ValueError("Config intervention paired summary does not describe four matched pairs")
    summary_pairs = summary.get("pairs", [])
    if len(summary_pairs) != 4:
        raise ValueError("Config intervention paired summary is incomplete")
    for pair in summary_pairs:
        label = _pair_label(pair)
        index = CONFIG_PAIRS.index(label)
        if "original_minus_control_project_configuration_s" in pair:
            expected_cost = _number(pair["original_minus_control_project_configuration_s"], "summary original-control")
            if abs(expected_cost - costs[index]) > 1e-6:
                raise ValueError("Paired summary disagrees with original-minus-control phase timing")
        elif "control_minus_original_project_configuration_s" in pair:
            expected_cost = -_number(pair["control_minus_original_project_configuration_s"], "summary control-original")
            if abs(expected_cost - costs[index]) > 1e-6:
                raise ValueError("Paired summary disagrees with original-minus-control phase timing")

    values = _positive_cost(costs, "Original-minus-generation-disabled project configuration")
    return _series("Build integration · original − disabled",
                   "Combined plugin + generation wiring · n=4", values,
                   "project_configuration_s",
                   "valid; exit 0; tasks dry-run skipped; no worker; no Test actions; runtime inputs preserved")


def cause_data(timeline: dict, phase_rows: list[dict], config_plan: dict,
               config_results) -> dict:
    timeline_plan = timeline.get("plan", {})
    rows = timeline_cost_rows(timeline, phase_rows)
    rows.append(config_intervention_cost(config_plan, config_results, timeline_plan))
    return {"rows": rows}


def _nice_limit(values: list[float]) -> float:
    maximum = max(values, default=0.0)
    if maximum <= 0:
        return 0.1
    return math.ceil(maximum * 1.08 * 10) / 10


def cause_svg(rows: list[dict]) -> str:
    values = [value for row in rows for value in row["values"]]
    limit = _nice_limit(values)
    left, right = 20.0, 270.0
    scale = (right - left) / limit
    x = lambda value: left + value * scale
    row_top = 53
    row_step = 79
    height = row_top + row_step * len(rows) + 8
    output = [f'<svg class="cause-chart" viewBox="0 0 360 {height}" role="img" aria-label="Paired Java outside-test and Gradle configuration diagnostics in seconds">']
    for fraction, label in ((0.0, "0 s"), (0.5, f"{limit / 2:.1f} s"), (1.0, f"{limit:.1f} s")):
        position = left + fraction * (right - left)
        anchor = "start" if fraction == 0 else ("end" if fraction == 1 else "middle")
        output.append(f'<text class="axis" x="{position:.1f}" y="18" text-anchor="{anchor}">{label}</text>')
        output.append(f'<line class="{("zero" if fraction == 0 else "gridline")}" x1="{position:.1f}" x2="{position:.1f}" y1="25" y2="{height - 7}"/>')
    for index, row in enumerate(rows):
        top = row_top + row_step * index
        display_labels = ("Extra time outside tests", "Extra project configuration",
                          "Removed with generators disabled")
        display_scopes = ("Protobuf − Text · two cold pairs", "Same two paired suite commands",
                          "Same Protobuf project · four pairs")
        # Split the label and sample-size note to keep 15px text inside a 360px viewBox.
        output.append(f'<text class="label" x="20" y="{top}">{display_labels[index]}</text>')
        output.append(f'<text class="scope" x="20" y="{top + 18}">{display_scopes[index]}</text>')
        y = top + 39
        data = row["values"]
        median = row["median_delta_s"]
        output.append(f'<line class="range" x1="{x(min(data)):.2f}" x2="{x(max(data)):.2f}" y1="{y}" y2="{y}"/>')
        for pair_index, value in enumerate(data):
            cy = y + (-4.5 + pair_index * 3)
            output.append(f'<circle class="point" cx="{x(value):.2f}" cy="{cy:.1f}" r="3"><title>Pair {pair_index + 1}: {value:.4f} s</title></circle>')
        output.append(f'<line class="median" x1="{x(median):.2f}" x2="{x(median):.2f}" y1="{y - 8}" y2="{y + 8}"/>')
        output.append(f'<text class="value" x="355" y="{y + 5}" text-anchor="end">{median:.2f} s</text>')
    output.append('</svg>')
    return "".join(output)


def render_html(data: dict) -> str:
    return f'''<section class="java-configuration-cause" aria-label="Java Gradle configuration evidence">
<style>
.java-configuration-cause {{ color:var(--ink,#18212e); font:15px/1.4 system-ui,sans-serif; min-width:0; }}
.java-configuration-cause .panel {{ max-width:620px; min-width:0; padding:14px; border:1px solid var(--line,#d5dfe8); border-radius:8px; }}
.java-configuration-cause h3 {{ margin:0 0 6px; font-size:18px; }}
.java-configuration-cause p {{ margin:5px 0 10px; color:var(--muted,#536174); }}
.java-configuration-cause .cause-chart {{ display:block; width:100%; height:auto; }}
.java-configuration-cause text {{ font-family:system-ui,sans-serif; fill:var(--ink,#18212e); }}
.java-configuration-cause .axis {{ font-size:15px; fill:var(--muted,#536174); }}
.java-configuration-cause .label {{ font-size:15px; font-weight:650; }}
.java-configuration-cause .scope {{ font-size:15px; fill:var(--muted,#536174); }}
.java-configuration-cause .value {{ font:15px ui-monospace,monospace; }}
.java-configuration-cause .gridline {{ stroke:var(--line,#d5dfe8); stroke-dasharray:3 4; }}
.java-configuration-cause .zero {{ stroke:var(--muted,#536174); stroke-width:1.3; }}
.java-configuration-cause .range {{ stroke:var(--bad,#ae3535); stroke-width:2; }}
.java-configuration-cause .point {{ fill:var(--bad,#ae3535); }}
.java-configuration-cause .median {{ stroke:var(--bad,#ae3535); stroke-width:2.5; }}
.java-configuration-cause details {{ margin-top:8px; }}
.java-configuration-cause summary {{ cursor:pointer; }}
.java-configuration-cause table {{ border-collapse:collapse; width:100%; font-size:14px; }}
.java-configuration-cause th,.java-configuration-cause td {{ border-bottom:1px solid var(--line,#d5dfe8); padding:5px; text-align:left; }}
@media(max-width:380px) {{ .java-configuration-cause .panel {{ padding:10px 6px; }} }}
</style>
<article class="panel"><h3>The Java overhead is build setup</h3>
<p>Protobuf's plugin and generator setup causes most of the outside-test penalty. The final control confirms this without running any tests. Dots = pairs; tick = median; line = range.</p>
{cause_svg(data["rows"])}
<p>These are separate comparisons, not times to add together. The control identifies the main cause, not every remaining millisecond.</p>
<details><summary>Pair values and intervention scope</summary>
<p>The isolated intervention disables the protobuf Gradle plugin plus schema/binding generation wiring as one combined build-integration package. It preserves the protobuf-java dependency, generated Java inputs, and test configuration; it does not isolate a single plugin method.</p>
{_detail_table(data["rows"])}
</details></article></section>'''


def _detail_table(rows: list[dict]) -> str:
    result = ['<table><thead><tr><th>Comparison</th><th>Pairs (s)</th></tr></thead><tbody>']
    for row in rows:
        values = ", ".join(f"{value:.4f}" for value in row["values"])
        result.append(f'<tr><th>{html.escape(row["label"])}</th><td>{values}</td></tr>')
    return "".join(result) + "</tbody></table>"


def csv_text(rows: list[dict]) -> str:
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=("comparison", "scope", "phase", "pair",
                                                "phase_delta_s", "median_delta_s", "valid",
                                                "validation_gate"))
    writer.writeheader()
    for row in rows:
        for index, value in enumerate(row["values"], 1):
            writer.writerow({"comparison": row["label"], "scope": row["scope"],
                             "phase": row["phase"], "pair": index,
                             "phase_delta_s": f"{value:.6f}",
                             "median_delta_s": f"{row['median_delta_s']:.6f}",
                             "valid": "true", "validation_gate": row["validation_gate"]})
    return output.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--timeline-results", type=Path, default=TIMELINE_ROOT / "results.json")
    parser.add_argument("--phase-bounds", type=Path, default=TIMELINE_ROOT / "phase-bounds.csv")
    parser.add_argument("--config-plan", type=Path, default=CONFIG_ROOT / "plan.json")
    parser.add_argument("--config-results", type=Path, default=CONFIG_ROOT / "results.json")
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--csv", type=Path, default=DEFAULT_CSV)
    args = parser.parse_args()
    timeline = read_json(args.timeline_results)
    phase_rows = read_csv(args.phase_bounds)
    config_plan = read_json(args.config_plan)
    config_results = read_json(args.config_results)
    data = cause_data(timeline, phase_rows, config_plan, config_results)
    args.html.parent.mkdir(parents=True, exist_ok=True)
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    args.html.write_text(render_html(data), encoding="utf-8")
    args.csv.write_text(csv_text(data["rows"]), encoding="utf-8")
    print(json.dumps({"html": str(args.html), "csv": str(args.csv),
                      "pair_counts": [len(row["values"]) for row in data["rows"]],
                      "validated": True}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
