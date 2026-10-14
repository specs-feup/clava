#!/usr/bin/env python3
"""Split two full Clava-JS diagnostic runs by outer parse outcome.

This is descriptive accounting for the frozen four-run diagnostic artifact.
It does not normalize compiler arguments or attribute a cause to the result.
"""
from __future__ import annotations

import argparse
import csv
from decimal import Decimal, ROUND_HALF_UP
import hashlib
import html
import io
import json
import math
from pathlib import Path
import statistics
from typing import Any


RUN_ORDER = ((1, "ccache-text"), (1, "protobuf"), (2, "protobuf"), (2, "ccache-text"))
EXPECTED_OUTCOMES = {"app": 170, "null": 130}
EXPECTED_MULTISOURCE_CALLS = 48
EXPECTED_ORDER_DIFFERENCES = (30, 27)
EXPECTED_TESTS = {"total_tests": 164, "passed_tests": 158,
                  "failed_tests": 0, "skipped_tests": 6}
RUNNER_MANIFEST = "execution-manifest.json"
AGGREGATE_LOG = "outer-parse.jsonl"
CSV_NAME = "paired-components.csv"
HTML_NAME = "paired-components.html"
HEX_SHA256_LENGTH = 64

CSV_FIELDS = (
    "pair", "text_stage", "protobuf_stage", "suite",
    "text_tests", "passed_tests", "failed_tests", "skipped_tests",
    "text_outer_parse_calls", "protobuf_outer_parse_calls",
    "matched_call_positions", "matched_input_content_basename_sets",
    "matched_outcomes",
    "app_calls", "app_text_s", "app_protobuf_s", "app_delta_pb_minus_text_s",
    "null_calls", "null_text_s", "null_protobuf_s", "null_delta_pb_minus_text_s",
    "text_suite_elapsed_s", "protobuf_suite_elapsed_s", "suite_delta_pb_minus_text_s",
    "text_remainder_s", "protobuf_remainder_s", "remainder_delta_pb_minus_text_s",
    "multisource_calls", "multisource_order_differences",
    "source_content_sha256_differences", "raw_options_sha256_differences",
    "text_native_binary_sha256", "protobuf_native_binary_sha256",
    "text_parser_jar_sha256", "protobuf_parser_jar_sha256",
    "source_results_sha256", "source_execution_manifest_sha256",
    "source_outer_parse_sha256",
)


class AnalysisError(ValueError):
    """The diagnostic artifact does not meet this analysis contract."""


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def _is_sha256(value: Any) -> bool:
    return (isinstance(value, str) and len(value) == HEX_SHA256_LENGTH
            and all(character in "0123456789abcdef" for character in value.lower()))


def _finite_number(value: Any, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise AnalysisError(f"{label} must be numeric")
    result = float(value)
    if not math.isfinite(result) or result < 0:
        raise AnalysisError(f"{label} must be finite and non-negative")
    return result


def _read_json(path: Path) -> tuple[Any, bytes]:
    try:
        content = path.read_bytes()
        return json.loads(content), content
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read {path}: {error}") from error


def _read_jsonl(path: Path) -> tuple[list[dict[str, Any]], bytes]:
    try:
        content = path.read_bytes()
    except OSError as error:
        raise AnalysisError(f"cannot read outer-parse log: {error}") from error

    rows: list[dict[str, Any]] = []
    for line_number, line in enumerate(content.decode("utf-8").splitlines(), 1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as error:
            raise AnalysisError(f"{path.name}:{line_number}: malformed JSON: {error.msg}") from error
        if not isinstance(row, dict):
            raise AnalysisError(f"{path.name}:{line_number}: record must be an object")
        rows.append(row)
    return rows, content


def _input_signature(row: dict[str, Any], context: str) -> tuple[tuple[str, str], ...]:
    basenames = row.get("input_basenames")
    content_hashes = row.get("input_content_sha256")
    count = row.get("input_count")
    if (not isinstance(basenames, list) or not isinstance(content_hashes, list)
            or type(count) is not int or count < 0
            or len(basenames) != count or len(content_hashes) != count):
        raise AnalysisError(f"{context}: input count, basenames, and content hashes disagree")

    pairs = []
    for basename, digest in zip(basenames, content_hashes):
        if (not isinstance(basename, str) or not basename.strip()
                or basename.strip().replace("\\", "/").rsplit("/", 1)[-1] != basename.strip()
                or not _is_sha256(digest)):
            raise AnalysisError(f"{context}: invalid input basename/content fingerprint")
        pairs.append((basename.strip(), digest.lower()))
    return tuple(sorted(pairs))


def _validate_record(row: dict[str, Any], ordinal: int, stage: str, repeat: int,
                     context: str) -> None:
    expected = {
        "record_type": "full_parse",
        "content_hash_phase": "after_parse",
        "suite": "clava-js",
        "mode": "direct",
        "stage": stage,
        "repeat": repeat,
        "call_ordinal": ordinal,
    }
    for field, value in expected.items():
        if row.get(field) != value:
            raise AnalysisError(f"{context}: unexpected {field} at call {ordinal}")
    if row.get("outcome") not in EXPECTED_OUTCOMES:
        raise AnalysisError(f"{context}: unexpected parse outcome at call {ordinal}")
    _input_signature(row, context)
    for field in ("identity_sha256", "options_sha256", "source_content_sha256"):
        if not _is_sha256(row.get(field)):
            raise AnalysisError(f"{context}: missing or invalid {field} at call {ordinal}")
    _finite_number(row.get("elapsed_ms"), f"{context} call {ordinal} elapsed_ms")


def compare_call_logs(text_rows: list[dict[str, Any]], protobuf_rows: list[dict[str, Any]]) -> dict[str, int]:
    """Match corresponding calls by ordinal, outcome, and order-insensitive input identity."""
    if len(text_rows) != len(protobuf_rows):
        raise AnalysisError("Text and Protobuf logs have different outer-parse record counts")

    matched_sets = 0
    matched_outcomes = 0
    multisource_calls = 0
    order_differences = 0
    source_content_hash_differences = 0
    option_hash_differences = 0

    for ordinal, (text, protobuf) in enumerate(zip(text_rows, protobuf_rows), 1):
        if text.get("call_ordinal") != protobuf.get("call_ordinal"):
            raise AnalysisError(f"outer-parse call ordinal differs at position {ordinal}")
        text_signature = _input_signature(text, f"Text call {ordinal}")
        protobuf_signature = _input_signature(protobuf, f"Protobuf call {ordinal}")
        if text_signature != protobuf_signature:
            raise AnalysisError(f"input content/basename set differs at call {ordinal}")
        matched_sets += 1
        if text.get("outcome") != protobuf.get("outcome"):
            raise AnalysisError(f"parse outcome differs at call {ordinal}")
        matched_outcomes += 1

        if text["input_count"] > 1:
            multisource_calls += 1
            text_order = tuple(zip(text["input_basenames"], text["input_content_sha256"]))
            protobuf_order = tuple(zip(protobuf["input_basenames"], protobuf["input_content_sha256"]))
            if text_order != protobuf_order:
                order_differences += 1
        if text.get("source_content_sha256") != protobuf.get("source_content_sha256"):
            source_content_hash_differences += 1
        if text.get("options_sha256") != protobuf.get("options_sha256"):
            option_hash_differences += 1

    return {
        "matched_call_positions": len(text_rows),
        "matched_input_content_basename_sets": matched_sets,
        "matched_outcomes": matched_outcomes,
        "multisource_calls": multisource_calls,
        "multisource_order_differences": order_differences,
        "source_content_sha256_differences": source_content_hash_differences,
        "raw_options_sha256_differences": option_hash_differences,
    }


def _validate_result_row(row: Any, stage: str, repeat: int, gate: dict[str, Any],
                         source: Path) -> None:
    if not isinstance(row, dict):
        raise AnalysisError(f"{source}: each results entry must be an object")
    expected = {"suite": "clava-js", "mode": "direct", "stage": stage,
                "repeat": repeat, "measured": True, "valid": True,
                "return_code": 0, "exit_status": 0}
    for field, value in expected.items():
        if row.get(field) != value:
            raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} has invalid {field}")
    for field, expected_value in EXPECTED_TESTS.items():
        if row.get(field) != expected_value:
            raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} has unexpected {field}")
    if row.get("failure_names") != []:
        raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} has reported test failures")
    cache_validation = row.get("cache_validation")
    if (not isinstance(cache_validation, dict) or cache_validation.get("passed") is not True
            or row.get("cacheable_calls") != 0 or row.get("cache_hits") != 0
            or row.get("cache_misses") != 0):
        raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} failed the direct-cache gate")
    if row.get("outer_parse_records") != 300 or not _is_sha256(row.get("outer_parse_sha256")):
        raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} has no 300-record log fingerprint")
    if (gate.get("stage") != stage or gate.get("non_parser_jars_match") is not True
            or not _is_sha256(gate.get("native_sha256"))
            or not _is_sha256(gate.get("parser_jar_diagnostic_sha256"))
            or row.get("runtime_parser_jar_sha256") != gate.get("parser_jar_diagnostic_sha256")):
        raise AnalysisError(f"{source}: result {stage}/repeat-{repeat} differs from its recorded runtime gate")
    _finite_number(row.get("elapsed_s"), f"{source} {stage}/repeat-{repeat} elapsed_s")


def analyze_pair(text_result: dict[str, Any], protobuf_result: dict[str, Any],
                 text_rows: list[dict[str, Any]], protobuf_rows: list[dict[str, Any]],
                 match: dict[str, int], pair_number: int) -> dict[str, Any]:
    sums: dict[str, dict[str, float]] = {}
    counts: dict[str, int] = {}
    for outcome in EXPECTED_OUTCOMES:
        text_group = [row for row in text_rows if row["outcome"] == outcome]
        protobuf_group = [row for row in protobuf_rows if row["outcome"] == outcome]
        if len(text_group) != EXPECTED_OUTCOMES[outcome] or len(protobuf_group) != EXPECTED_OUTCOMES[outcome]:
            raise AnalysisError(f"pair {pair_number}: unexpected {outcome} call count")
        counts[outcome] = len(text_group)
        sums[outcome] = {
            "text_s": math.fsum(row["elapsed_ms"] for row in text_group) / 1000.0,
            "protobuf_s": math.fsum(row["elapsed_ms"] for row in protobuf_group) / 1000.0,
        }
        sums[outcome]["delta_s"] = sums[outcome]["protobuf_s"] - sums[outcome]["text_s"]

    suite_delta = float(Decimal(str(protobuf_result["elapsed_s"]))
                        - Decimal(str(text_result["elapsed_s"])))
    text_outer_total = math.fsum(row["elapsed_ms"] for row in text_rows) / 1000.0
    protobuf_outer_total = math.fsum(row["elapsed_ms"] for row in protobuf_rows) / 1000.0
    text_remainder = text_result["elapsed_s"] - text_outer_total
    protobuf_remainder = protobuf_result["elapsed_s"] - protobuf_outer_total
    remainder_delta = protobuf_remainder - text_remainder
    component_delta = sums["app"]["delta_s"] + sums["null"]["delta_s"] + remainder_delta
    if not math.isclose(component_delta, suite_delta, rel_tol=0.0, abs_tol=1e-9):
        raise AnalysisError(f"pair {pair_number}: computed components do not add to suite elapsed delta")

    return {
        "pair": pair_number,
        "text_stage": text_result["stage"],
        "protobuf_stage": protobuf_result["stage"],
        "suite": "clava-js",
        "text_tests": text_result["total_tests"],
        "passed_tests": text_result["passed_tests"],
        "failed_tests": text_result["failed_tests"],
        "skipped_tests": text_result["skipped_tests"],
        "text_outer_parse_calls": len(text_rows),
        "protobuf_outer_parse_calls": len(protobuf_rows),
        **match,
        "app_calls": counts["app"],
        "app_text_s": sums["app"]["text_s"],
        "app_protobuf_s": sums["app"]["protobuf_s"],
        "app_delta_pb_minus_text_s": sums["app"]["delta_s"],
        "null_calls": counts["null"],
        "null_text_s": sums["null"]["text_s"],
        "null_protobuf_s": sums["null"]["protobuf_s"],
        "null_delta_pb_minus_text_s": sums["null"]["delta_s"],
        "text_suite_elapsed_s": text_result["elapsed_s"],
        "protobuf_suite_elapsed_s": protobuf_result["elapsed_s"],
        "suite_delta_pb_minus_text_s": suite_delta,
        "text_remainder_s": text_remainder,
        "protobuf_remainder_s": protobuf_remainder,
        "remainder_delta_pb_minus_text_s": remainder_delta,
        "text_native_binary_sha256": text_result["native_binary_sha256"],
        "protobuf_native_binary_sha256": protobuf_result["native_binary_sha256"],
        "text_parser_jar_sha256": text_result["runtime_parser_jar_sha256"],
        "protobuf_parser_jar_sha256": protobuf_result["runtime_parser_jar_sha256"],
    }


def _csv_value(value: Any) -> Any:
    if isinstance(value, float):
        return f"{value:.6f}"
    return value


def render_csv(pairs: list[dict[str, Any]], provenance: dict[str, str]) -> str:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=CSV_FIELDS, extrasaction="ignore", lineterminator="\n")
    writer.writeheader()
    for pair in pairs:
        writer.writerow({field: _csv_value(value) for field, value in {**pair, **provenance}.items()})
    return stream.getvalue()


def _seconds(value: float, digits: int = 2) -> str:
    quantum = Decimal(1).scaleb(-digits)
    rounded = Decimal(str(value)).quantize(quantum, rounding=ROUND_HALF_UP)
    return f"{rounded:+.{digits}f} s"


def _delta_row(label: str, value: float, y: int, *, calls: int | None = None) -> str:
    zero_x = 190.0
    pixels_per_second = 100.0
    endpoint = zero_x + max(-1.65, min(1.5, value)) * pixels_per_second
    color = "var(--slower-color)" if value > 0 else "var(--faster-color)"
    call_note = f" ({calls} calls)" if calls is not None else ""
    return (
        f'<g class="component-row"><text class="component-label" x="18" y="{y}">'
        f'{html.escape(label + call_note)}</text>'
        f'<text class="component-value" x="342" y="{y}" text-anchor="end">{_seconds(value)}</text>'
        f'<line class="row-track" x1="20" y1="{y + 18}" x2="340" y2="{y + 18}" />'
        f'<line class="zero-line" x1="{zero_x}" y1="{y + 8}" x2="{zero_x}" y2="{y + 29}" />'
        f'<rect x="{min(zero_x, endpoint):.2f}" y="{y + 12}" width="{max(1.5, abs(endpoint - zero_x)):.2f}" '
        f'height="12" rx="3" fill="{color}" />'
        f'<circle cx="{endpoint:.2f}" cy="{y + 18}" r="4" fill="{color}" />'
        f'</g>'
    )


def _svg(means: dict[str, float], app_calls: int, null_calls: int) -> str:
    ticks = (-1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5)
    axis = ['<line class="axis-line" x1="20" y1="48" x2="340" y2="48" />']
    for tick in ticks:
        x = 190 + tick * 100
        axis.append(f'<line class="axis-tick" x1="{x:.1f}" y1="44" x2="{x:.1f}" y2="52" />')
        label = "0" if tick == 0 else f"{tick:+.1f}"
        axis.append(f'<text class="axis-label" x="{x:.1f}" y="39" text-anchor="middle">{label}</text>')
    rows = [
        _delta_row("App parse", means["app_delta_pb_minus_text_s"], 80, calls=app_calls),
        _delta_row("Syntax validation", means["null_delta_pb_minus_text_s"], 160, calls=null_calls),
        _delta_row("Other / remainder", means["remainder_delta_pb_minus_text_s"], 240),
        _delta_row("Whole-suite elapsed", means["suite_delta_pb_minus_text_s"], 320),
    ]
    labels = [
        '<text class="direction faster-label" x="20" y="405">← Protobuf faster</text>',
        '<text class="direction slower-label" x="340" y="405" text-anchor="end">Protobuf slower →</text>',
    ]
    return (
        '<svg class="component-chart" viewBox="0 0 360 420" role="img" aria-labelledby="chart-title chart-desc">'
        '<title id="chart-title">Mean parse and suite elapsed changes</title>'
        '<desc id="chart-desc">Positive bars mean Protobuf took longer. Negative bars mean Protobuf was faster. '
        'Values are the arithmetic means of two matched diagnostic pairs.</desc>'
        + "".join(axis) + "".join(rows) + "".join(labels) + "</svg>"
    )


def render_html(pairs: list[dict[str, Any]], provenance: dict[str, str],
                runtime: dict[str, dict[str, str]]) -> str:
    if len(pairs) != 2:
        raise AnalysisError("the report requires exactly two paired diagnostic runs")
    means = {
        field: statistics.fmean(pair[field] for pair in pairs)
        for field in ("app_delta_pb_minus_text_s", "null_delta_pb_minus_text_s",
                      "remainder_delta_pb_minus_text_s", "suite_delta_pb_minus_text_s")
    }
    app_calls = pairs[0]["app_calls"]
    null_calls = pairs[0]["null_calls"]
    if any(pair["app_calls"] != app_calls or pair["null_calls"] != null_calls for pair in pairs[1:]):
        raise AnalysisError("the two pairs have different outcome call counts")

    order_text = " and ".join(f'{pair["multisource_order_differences"]}/48' for pair in pairs)
    options_text = " and ".join(f'{pair["raw_options_sha256_differences"]}/300' for pair in pairs)
    runtime_fingerprints_differ = runtime["Text"] != runtime["Protobuf"]
    runtime_note = ("Native/runtime hashes differ." if runtime_fingerprints_differ
                    else "Native/runtime hashes match.")
    summary = (
        "Four full Clava-JS runs passed 158/164 tests (6 skipped) and logged 300 outer CodeParser.parse calls. "
        "Text/Protobuf pairs matched all 300 positions by normalized content-hash/basename sets and outcome "
        f"({app_calls} App, {null_calls} null); order changed on {order_text} multi-file calls, respectively. "
        "Null-path semantics differ: Text drains default AST-dump output; Protobuf adds -syntax-check-only. "
        f"{runtime_note} Raw option hashes differ on {options_text} calls per pair, without normalization. "
        "The split is descriptive, not causal; 'Other' is an elapsed remainder, not a measured phase."
    )
    chart = _svg(means, app_calls, null_calls)
    pair_rows = []
    for pair in pairs:
        pair_rows.append(
            '<tr>'
            f'<th scope="row">Pair {pair["pair"]}</th>'
            f'<td>{pair["text_suite_elapsed_s"]:.2f} s</td>'
            f'<td>{pair["protobuf_suite_elapsed_s"]:.2f} s</td>'
            f'<td class="{ "faster" if pair["suite_delta_pb_minus_text_s"] < 0 else "slower" }">'
            f'{_seconds(pair["suite_delta_pb_minus_text_s"])}</td>'
            f'<td>{pair["multisource_order_differences"]}/48 reordered</td>'
            '</tr>'
        )
    runtime_rows = []
    for label in ("Text", "Protobuf"):
        values = runtime[label]
        runtime_rows.append(
            f'<tr><th scope="row">{label}</th>'
            f'<td><code>{html.escape(values["native_sha256"])}</code></td>'
            f'<td><code>{html.escape(values["parser_jar_sha256"])}</code></td>'
            f'<td><code>{html.escape(values["runtime_manifest_sha256"])}</code></td></tr>'
        )

    return f'''<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="source-results-sha256" content="{html.escape(provenance["source_results_sha256"])}">
  <meta name="source-execution-manifest-sha256" content="{html.escape(provenance["source_execution_manifest_sha256"])}">
  <meta name="source-outer-parse-sha256" content="{html.escape(provenance["source_outer_parse_sha256"])}">
  <title>Clava-JS parse workload components</title>
  <style>
    :root {{
      color-scheme: light dark;
      --page-bg: #f3f6f8;
      --panel-bg: #ffffff;
      --text-color: #17232d;
      --muted-color: #4a5a66;
      --border-color: #d2dce3;
      --axis-color: #71818c;
      --faster-color: #11765a;
      --slower-color: #ad4b37;
      --code-bg: #edf2f5;
    }}
    @media (prefers-color-scheme: dark) {{
      :root {{
        --page-bg: #111820;
        --panel-bg: #19232c;
        --text-color: #edf3f6;
        --muted-color: #bdcbd3;
        --border-color: #394852;
        --axis-color: #91a2ad;
        --faster-color: #64c9a5;
        --slower-color: #f08c70;
        --code-bg: #25323c;
      }}
    }}
    * {{ box-sizing: border-box; }}
    body {{ margin: 0; padding: 16px; background: var(--page-bg); color: var(--text-color);
      font: 15px/1.5 system-ui, sans-serif; }}
    main {{ width: min(100%, 760px); margin: 0 auto; padding: 18px; border: 1px solid var(--border-color);
      border-radius: 12px; background: var(--panel-bg); }}
    h1 {{ margin: 0 0 8px; font-size: 1.45rem; line-height: 1.2; }}
    h2 {{ margin: 20px 0 6px; font-size: 1.05rem; }}
    p {{ margin: 8px 0 14px; }}
    .note {{ color: var(--muted-color); font-size: .92rem; }}
    figure {{ margin: 14px 0 8px; }}
    .component-chart {{ display: block; width: 100%; height: auto; overflow: visible; }}
    .component-chart text {{ font-family: system-ui, sans-serif; fill: var(--text-color); }}
    .component-label, .component-value {{ font-size: 15px; font-weight: 650; }}
    .axis-label, .direction {{ font-size: 12px; }}
    .direction {{ font-weight: 600; }}
    .faster-label, .faster {{ color: var(--faster-color); fill: var(--faster-color); }}
    .slower-label, .slower {{ color: var(--slower-color); fill: var(--slower-color); }}
    .axis-line, .axis-tick, .zero-line {{ stroke: var(--axis-color); stroke-width: 1; }}
    .row-track {{ stroke: var(--border-color); stroke-width: 2; }}
    .axis-label {{ fill: var(--muted-color) !important; }}
    .table-wrap {{ width: 100%; overflow-x: auto; }}
    table {{ width: 100%; border-collapse: collapse; font-variant-numeric: tabular-nums; }}
    th, td {{ padding: 7px 6px; border-bottom: 1px solid var(--border-color); text-align: left; }}
    code {{ padding: 2px 4px; border-radius: 4px; background: var(--code-bg); font-size: .8rem;
      overflow-wrap: anywhere; word-break: break-word; }}
    details {{ margin-top: 18px; }}
    summary {{ cursor: pointer; font-weight: 650; }}
    .hash-table th {{ width: 90px; }}
    .hash-table td {{ overflow-wrap: anywhere; word-break: break-word; }}
    @media (max-width: 420px) {{
      body {{ padding: 8px; }}
      main {{ padding: 12px; border-radius: 8px; }}
      h1 {{ font-size: 1.25rem; }}
      .component-label, .component-value {{ font-size: 13px; }}
      .axis-label, .direction {{ font-size: 11px; }}
      th, td {{ padding: 6px 4px; }}
    }}
  </style>
</head>
<body>
<main>
  <h1>Clava-JS parse workload components</h1>
  <p class="note">Arithmetic means of two ordered diagnostic pairs. Values show Protobuf minus Text. Negative means faster.</p>
  <p>{html.escape(summary)}</p>
  <figure aria-label="Paired mean component changes">
    {chart}
    <figcaption class="note">Call-span sums are grouped by returned outcome. The remainder includes work outside those spans and rounding.</figcaption>
  </figure>
  <h2>Observed whole-command pairs</h2>
  <div class="table-wrap"><table>
    <thead><tr><th>Pair</th><th>Text</th><th>Protobuf</th><th>PB − Text</th><th>Multi-source order</th></tr></thead>
    <tbody>{''.join(pair_rows)}</tbody>
  </table></div>
  <details>
    <summary>Runtime fingerprints and source checksums</summary>
    <p class="note">The native/runtime artifacts differ by stage. No compiler-option normalization was attempted.</p>
    <div class="table-wrap"><table class="hash-table">
      <thead><tr><th>Stage</th><th>Native binary SHA-256</th><th>Parser JAR SHA-256</th><th>Runtime manifest SHA-256</th></tr></thead>
      <tbody>{''.join(runtime_rows)}</tbody>
    </table></div>
    <ul>
      <li>Results JSON SHA-256: <code>{html.escape(provenance["source_results_sha256"])}</code></li>
      <li>Execution manifest SHA-256: <code>{html.escape(provenance["source_execution_manifest_sha256"])}</code></li>
      <li>Aggregate outer-parse log SHA-256: <code>{html.escape(provenance["source_outer_parse_sha256"])}</code></li>
    </ul>
  </details>
</main>
</body>
</html>
'''


def load_analysis(results_path: Path) -> tuple[list[dict[str, Any]], dict[str, str],
                                                dict[str, dict[str, str]], dict[Path, str]]:
    results_path = results_path.resolve()
    execution_path = results_path.with_name(RUNNER_MANIFEST)
    aggregate_path = results_path.with_name(AGGREGATE_LOG)
    raw_results, results_bytes = _read_json(results_path)
    raw_execution, execution_bytes = _read_json(execution_path)
    if not isinstance(raw_results, list) or not isinstance(raw_execution, dict):
        raise AnalysisError("results.json must be a four-row array and execution manifest an object")
    if raw_execution.get("schema_version") != 1:
        raise AnalysisError("execution manifest must use schema_version 1")
    if len(raw_results) != len(RUN_ORDER):
        raise AnalysisError(f"expected {len(RUN_ORDER)} results rows, found {len(raw_results)}")
    expected_order = [{"repeat": repeat, "stage": stage} for repeat, stage in RUN_ORDER]
    if raw_execution.get("order") != expected_order:
        raise AnalysisError("execution manifest order does not match the frozen two-pair design")
    if raw_execution.get("results_sha256") != sha256_bytes(results_bytes):
        raise AnalysisError("execution manifest results checksum does not match results.json")

    gates = raw_execution.get("runtime_gates")
    if not isinstance(gates, list) or len(gates) != len(RUN_ORDER):
        raise AnalysisError("execution manifest must retain four runtime gate records")
    compiled_families = raw_execution.get("compiled_class_family_sha256")
    if not isinstance(compiled_families, dict):
        raise AnalysisError("execution manifest lacks compiled instrumentation class-family hashes")

    run_logs: dict[tuple[int, str], tuple[list[dict[str, Any]], bytes, Path]] = {}
    log_paths: list[Path] = []
    per_run_bytes: list[bytes] = []
    stage_native: dict[str, str] = {}
    stage_runtime: dict[str, dict[str, str]] = {}
    stage_overlay_families: dict[str, dict[str, str]] = {}

    for index, ((repeat, stage), row, gate) in enumerate(zip(RUN_ORDER, raw_results, gates)):
        if not isinstance(row, dict) or not isinstance(gate, dict):
            raise AnalysisError("results and runtime gate entries must be objects")
        _validate_result_row(row, stage, repeat, gate, results_path)
        native_sha = gate["native_sha256"]
        if stage in stage_native and stage_native[stage] != native_sha:
            raise AnalysisError(f"native artifact changed between {stage} repeats")
        stage_native[stage] = native_sha
        runtime_entry = {
            "native_sha256": native_sha,
            "parser_jar_sha256": gate["parser_jar_diagnostic_sha256"],
            "runtime_manifest_sha256": gate.get("source_runtime_manifest_sha256", ""),
        }
        if not _is_sha256(runtime_entry["runtime_manifest_sha256"]):
            raise AnalysisError(f"{stage} runtime gate lacks a runtime manifest checksum")
        if stage in stage_runtime and stage_runtime[stage] != runtime_entry:
            raise AnalysisError(f"runtime provenance changed between {stage} repeats")
        stage_runtime[stage] = runtime_entry
        overlay_entries = gate.get("overlay_class_entries")
        if not isinstance(overlay_entries, list) or not overlay_entries:
            raise AnalysisError(f"{stage} runtime gate lacks compiled overlay class entries")
        overlay_family: dict[str, str] = {}
        for entry in overlay_entries:
            if (not isinstance(entry, dict) or not isinstance(entry.get("entry"), str)
                    or not entry["entry"] or not _is_sha256(entry.get("sha256"))):
                raise AnalysisError(f"{stage} runtime gate has invalid overlay class provenance")
            overlay_family[entry["entry"]] = entry["sha256"]
        if overlay_family != compiled_families.get(stage):
            raise AnalysisError(f"{stage} runtime overlay differs from compiled class-family manifest")
        if stage in stage_overlay_families and stage_overlay_families[stage] != overlay_family:
            raise AnalysisError(f"instrumentation overlay changed between {stage} repeats")
        stage_overlay_families[stage] = overlay_family

        raw_run_path = row.get("outer_parse_path")
        if not isinstance(raw_run_path, str) or not raw_run_path:
            raise AnalysisError(f"{results_path}: result {stage}/repeat-{repeat} lacks an outer-parse path")
        run_path = Path(raw_run_path).resolve()
        rows, content = _read_jsonl(run_path)
        if sha256_bytes(content) != row["outer_parse_sha256"]:
            raise AnalysisError(f"{results_path}: outer-parse checksum differs for {stage}/repeat-{repeat}")
        if len(rows) != row["outer_parse_records"]:
            raise AnalysisError(f"{results_path}: outer-parse row count differs for {stage}/repeat-{repeat}")
        if len(rows) != 300:
            raise AnalysisError(f"{results_path}: expected 300 outer-parse records for {stage}/repeat-{repeat}")
        for ordinal, record in enumerate(rows, 1):
            _validate_record(record, ordinal, stage, repeat, str(run_path))
        outcomes = {outcome: sum(record["outcome"] == outcome for record in rows)
                    for outcome in EXPECTED_OUTCOMES}
        if outcomes != EXPECTED_OUTCOMES:
            raise AnalysisError(f"{results_path}: unexpected outcome counts for {stage}/repeat-{repeat}: {outcomes}")
        run_key = (repeat, stage)
        run_logs[run_key] = (rows, content, run_path)
        log_paths.append(run_path)
        per_run_bytes.append(content)

    if stage_overlay_families["ccache-text"] != stage_overlay_families["protobuf"]:
        raise AnalysisError("Text and Protobuf runs used different instrumentation overlay class families")

    aggregate_rows, aggregate_bytes = _read_jsonl(aggregate_path)
    aggregate_sha = sha256_bytes(aggregate_bytes)
    if raw_execution.get("outer_parse_sha256") != aggregate_sha:
        raise AnalysisError("execution manifest aggregate checksum does not match outer-parse.jsonl")
    if aggregate_bytes != b"".join(per_run_bytes):
        raise AnalysisError("aggregate outer-parse log is not the ordered concatenation of the four run logs")
    if len(aggregate_rows) != sum(row["outer_parse_records"] for row in raw_results):
        raise AnalysisError("aggregate outer-parse row count differs from the four measured logs")

    rows_by_key = {(row["repeat"], row["stage"]): row for row in raw_results}
    pairs = []
    for pair_number in (1, 2):
        text_repeat, proto_repeat = pair_number, pair_number
        text_key, proto_key = (text_repeat, "ccache-text"), (proto_repeat, "protobuf")
        text_rows = run_logs[text_key][0]
        protobuf_rows = run_logs[proto_key][0]
        match = compare_call_logs(text_rows, protobuf_rows)
        if match["matched_call_positions"] != 300:
            raise AnalysisError(f"pair {pair_number}: incomplete call-position matching")
        if match["multisource_calls"] != EXPECTED_MULTISOURCE_CALLS:
            raise AnalysisError(f"pair {pair_number}: multi-file call count differs from the frozen diagnostic")
        expected_order_differences = EXPECTED_ORDER_DIFFERENCES[pair_number - 1]
        if match["multisource_order_differences"] != expected_order_differences:
            raise AnalysisError(f"pair {pair_number}: multi-file input order differs from the frozen diagnostic")
        text_result = dict(rows_by_key[text_key])
        protobuf_result = dict(rows_by_key[proto_key])
        text_result["native_binary_sha256"] = stage_runtime["ccache-text"]["native_sha256"]
        protobuf_result["native_binary_sha256"] = stage_runtime["protobuf"]["native_sha256"]
        pairs.append(analyze_pair(text_result, protobuf_result, text_rows, protobuf_rows, match, pair_number))

    provenance = {
        "source_results_sha256": sha256_bytes(results_bytes),
        "source_execution_manifest_sha256": sha256_bytes(execution_bytes),
        "source_outer_parse_sha256": aggregate_sha,
    }
    source_checksums = {
        results_path: sha256_bytes(results_bytes),
        execution_path: sha256_bytes(execution_bytes),
        aggregate_path: aggregate_sha,
    }
    for run_path, run_bytes in zip(log_paths, per_run_bytes):
        source_checksums[run_path] = sha256_bytes(run_bytes)
    if any(not _is_sha256(value) for value in provenance.values()):
        raise AnalysisError("invalid source checksum while preparing provenance")
    for pair in pairs:
        pair.update(provenance)
    return pairs, provenance, stage_runtime, source_checksums


def _confirm_sources_unchanged(paths: list[Path], checksums: dict[Path, str]) -> None:
    for path in paths:
        try:
            observed = sha256_bytes(path.read_bytes())
        except OSError as error:
            raise AnalysisError(f"source disappeared before report write: {path.name}") from error
        if observed != checksums[path]:
            raise AnalysisError(f"source changed during analysis: {path.name}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results", type=Path, required=True,
                        help="results.json from the frozen four-run outer-parse diagnostic")
    parser.add_argument("--output-dir", type=Path, required=True,
                        help="new report directory outside the immutable execution run")
    args = parser.parse_args(argv)

    results_path = args.results.resolve()
    output_dir = args.output_dir.resolve()
    source_root = results_path.parent
    if output_dir == source_root or source_root in output_dir.parents:
        raise AnalysisError("output directory must be outside the immutable execution run")
    csv_path = output_dir / CSV_NAME
    html_path = output_dir / HTML_NAME
    if csv_path.exists() or html_path.exists():
        raise AnalysisError(f"refusing to overwrite existing outputs in {output_dir}")

    pairs, provenance, stage_runtime, source_checksums = load_analysis(results_path)
    runtime = {"Text": stage_runtime["ccache-text"], "Protobuf": stage_runtime["protobuf"]}
    source_paths = list(source_checksums)
    csv_content = render_csv(pairs, provenance)
    html_content = render_html(pairs, provenance, runtime)
    if any(str(results_path.parent) in content for content in (csv_content, html_content)):
        raise AnalysisError("refusing to write output that contains the local source path")
    _confirm_sources_unchanged(source_paths, source_checksums)

    output_dir.mkdir(parents=True, exist_ok=True)
    csv_path.write_text(csv_content, encoding="utf-8", newline="")
    html_path.write_text(html_content, encoding="utf-8", newline="")
    print(csv_path)
    print(html_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
