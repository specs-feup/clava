#!/usr/bin/env python3
"""Render the compact decision report from validated measurements and an analysis.

The analysis JSON is an explicit, reviewed conclusion, not an automatic ranking.
Measurements retain the comparison runner's results.json schema.
"""
from __future__ import annotations

import argparse
import base64
import csv
import io
import json
from pathlib import Path
import statistics
import sys

from render_report import (
    MODE_ORDER, STAGE_ORDER, STAGES, SUITES, esc, flatten_results,
    is_measured, is_valid_run, load_inputs, quantile, time_value,
)

LABELS = {"before-cache": "Before cache", "ccache-text": "Text", "protobuf": "Protobuf", "flatbuffers": "FlatBuffers"}
MODES = {"direct": "Direct · ccache off", "cold": "Cold · empty cache", "warm": "Warm · seeded cache"}


def cell(rows, suite, mode, stage):
    return [r for r in rows if r.get("suite") == suite and r.get("mode") == mode
            and r.get("stage") == stage and r.get("selected", True)
            and is_measured(r) and is_valid_run(r, suite)]


def values(rows, suite, mode, stage):
    return [time_value(r) for r in cell(rows, suite, mode, stage)]


def validate_matrix(rows, repeat_count=6):
    for suite in SUITES:
        for mode in MODE_ORDER:
            for stage in STAGE_ORDER:
                if mode != "direct" and stage == "before-cache":
                    continue
                selected = cell(rows, suite, mode, stage)
                if len(selected) != repeat_count or {r.get("repeat") for r in selected} != set(range(1, repeat_count + 1)):
                    raise ValueError(f"Expected {repeat_count} distinct valid repeats for {suite}/{mode}/{stage}")


def candle(suite, rows):
    groups = [(mode, stage, values(rows, suite, mode, stage)) for mode in MODE_ORDER
              for stage in STAGE_ORDER if mode == "direct" or stage != "before-cache"]
    all_values = [v for _, _, vs in groups for v in vs]
    spread = max(all_values) - min(all_values)
    margin = max(.25, .04 * spread)
    low, high = max(0, min(all_values) - margin), max(all_values) + margin
    x = lambda v: 112 + (v-low) * 180 / (high-low)
    height = 525
    out = [f'<svg viewBox="0 0 360 {height}" role="img" aria-label="{esc(SUITES[suite]["title"])} runtime distributions">',
           '<desc>Lower is faster. Dots are individual runs. Box is middle half. Line is median. Whiskers show all runs. All cache sections share one non-zero time axis.</desc>']
    for v in (low, (low+high)/2, high):
        out += [f'<line class="grid" x1="{x(v):.2f}" x2="{x(v):.2f}" y1="28" y2="480"/>',
                f'<text class="tick" x="{x(v):.2f}" y="18" text-anchor="middle">{v:.1f}s</text>']
    y = 43
    for mode in MODE_ORDER:
        if mode != "direct":
            out.append(f'<line class="divider" x1="0" x2="360" y1="{y-10}" y2="{y-10}"/>')
        out.append(f'<text class="mode" x="0" y="{y+6}">{esc(MODES[mode])}</text>')
        y += 32
        for _, stage, vs in (g for g in groups if g[0] == mode):
            q1, med, q3 = quantile(vs, .25), statistics.median(vs), quantile(vs, .75)
            colour = STAGES[stage][1]
            out += [f'<text class="label" x="0" y="{y+4}">{esc(LABELS[stage])}</text>',
                    f'<line x1="{x(min(vs)):.2f}" x2="{x(max(vs)):.2f}" y1="{y}" y2="{y}" stroke="{colour}" stroke-width="2"/>',
                    f'<rect x="{x(q1):.2f}" y="{y-9}" width="{max(2,x(q3)-x(q1)):.2f}" height="18" fill="{colour}" fill-opacity=".22" stroke="{colour}"/>',
                    f'<line x1="{x(med):.2f}" x2="{x(med):.2f}" y1="{y-10}" y2="{y+10}" stroke="{colour}" stroke-width="3"/>',
                    f'<text class="value" x="359" y="{y+4}" text-anchor="end">{med:.2f}s</text>']
            for i, v in enumerate(vs):
                out.append(f'<circle cx="{x(v):.2f}" cy="{y+(i%3-1)*4}" r="2.8" fill="{colour}"><title>Run {i+1}: {v:.3f}s</title></circle>')
            y += 30
        y += 20
    out += ['<text class="foot" x="0" y="514">Non-zero axis · seconds · lower is faster</text>', '</svg>']
    return ''.join(out)


def comparison(rows):
    out = ['<div class="comparison">']
    for suite in SUITES:
        out.append(f'<div><h3>{esc(SUITES[suite]["title"])}</h3><div class="matrix-row matrix-head"><span></span><span>Direct</span><span>Cold</span><span>Warm</span></div>')
        for stage in ("protobuf", "flatbuffers"):
            out.append(f'<div class="matrix-row"><span>{esc(LABELS[stage])}</span>')
            for mode in MODE_ORDER:
                reference = statistics.median(values(rows, suite, mode, "ccache-text"))
                delta = 100*(statistics.median(values(rows, suite, mode, stage))/reference-1)
                out.append(f'<span class="delta {"faster" if delta<0 else "slower"}">{delta:+.1f}%</span>')
            out.append('</div>')
        out.append('</div>')
    return ''.join(out) + '</div>'


def cards(items, class_name="cards"):
    return f'<div class="{class_name}">' + ''.join(
        f'<article><h3>{esc(item["title"])}</h3><p>{esc(item["text"])}</p></article>' for item in items) + '</div>'


def measurements_download(rows):
    """Publish measurement fields without commands or local filesystem paths."""
    fields = ("suite", "stage", "mode", "repeat", "elapsed_s",
              "junit_aggregate_s", "total_tests", "passed_tests",
              "failed_tests", "skipped_tests", "cacheable_calls",
              "cache_hits", "cache_misses")
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields)
    writer.writeheader()
    count = 0
    for suite in SUITES:
        for mode in MODE_ORDER:
            for stage in STAGE_ORDER:
                for row in sorted(cell(rows, suite, mode, stage), key=lambda r: r["repeat"]):
                    writer.writerow({field: row.get(field, "") for field in fields})
                    count += 1
    payload = base64.b64encode(stream.getvalue().encode()).decode()
    return f'<a download="clava-suite-measurements.csv" href="data:text/csv;charset=utf-8;base64,{payload}">Download all {count} accepted suite timings (CSV)</a>'


def render(manifests, provenance, analysis):
    rows = flatten_results(manifests)
    repeat_counts = {m.get("plan", {}).get("repeat_count", 6) for m in manifests}
    if len(repeat_counts) != 1:
        raise ValueError("Repeat count differs across manifests")
    repeat_count = repeat_counts.pop()
    validate_matrix(rows, repeat_count)
    # The deadline runner keeps fixed artifact identities under plan.stages.
    # Historical runners instead used a top-level stage array.
    provenance = dict(provenance)
    for manifest in manifests:
        for key, metadata in manifest.get("plan", {}).get("stages", {}).items():
            provenance.setdefault(key, {})["all"] = metadata
    required = ("recommendation", "reasons", "discrepancy", "tradeoffs", "method", "limitations", "evidence")
    if any(k not in analysis for k in required):
        raise ValueError("Analysis is incomplete; refuse to publish a measurement-only decision report")
    if len(analysis["discrepancy"]) < 2:
        raise ValueError("The new results need an explicit consistency check and evidence-backed explanation")
    charts = ''.join(f'<article class="chart"><h3>{esc(SUITES[s]["title"])}</h3><p>{"158 pass · 6 skip" if s=="clava-js" else "116 pass · no skips"} · {repeat_count} runs per candle</p>{candle(s,rows)}</article>' for s in SUITES)
    evidence = ''.join(f'<li><a href="{esc(e["url"])}">{esc(e["title"])}</a></li>' for e in analysis["evidence"])
    revisions = ''.join(f'<li>{esc(STAGES[k][0])}: <code>{esc(next(iter(v.values())).get("clava_revision", "not recorded"))}</code></li>' for k,v in provenance.items())
    extra_visuals = analysis.get("reviewed_visuals_html", "")
    return f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Clava protocol and cache decision</title><style>
:root{{--bg:#fff;--ink:#18212e;--muted:#536174;--panel:#f4f7fa;--line:#d5dfe8;--good:#08744c;--bad:#ae3535}}html.dark{{--bg:#111923;--ink:#edf2f7;--muted:#b2c0ce;--panel:#1c2836;--line:#3c4b5e;--good:#77dcb1;--bad:#ffaaa2}}*{{box-sizing:border-box}}body{{margin:0;background:var(--bg);color:var(--ink);font:16px/1.5 system-ui,sans-serif}}main{{max-width:1100px;margin:auto;padding:24px}}h1{{font-size:clamp(27px,4vw,38px);line-height:1.15;margin:0 0 14px}}h2{{font-size:24px;margin:34px 0 12px}}h3{{font-size:18px;margin:0 0 8px}}p{{margin:8px 0 14px}}.muted,.chart>p{{color:var(--muted);font-size:14px}}.verdict{{padding:22px;background:var(--panel);border-left:5px solid var(--good);border-radius:8px}}.verdict p{{font-size:20px;margin:0}}.cards,.charts,.comparison{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}}.cards article,.chart,.comparison>div{{border:1px solid var(--line);border-radius:10px;padding:18px;min-width:0}}.cards article p{{margin:0}}.charts svg{{display:block;width:100%;height:auto;max-width:480px;margin:auto}}svg text{{fill:var(--ink);font-family:system-ui,sans-serif}}.grid{{stroke:var(--line);stroke-dasharray:3 4}}.divider{{stroke:var(--line)}}.tick,.label,.value{{font-size:12px}}.mode{{font-size:13px;font-weight:650}}.foot{{font-size:11px;fill:var(--muted)}}.matrix-row{{display:grid;grid-template-columns:1.4fr repeat(3,1fr);gap:8px;padding:10px 0;border-bottom:1px solid var(--line);font-size:14px;align-items:center}}.matrix-row span:not(:first-child){{text-align:center}}.matrix-head{{color:var(--muted);font-size:12px}}.delta{{font-weight:700}}.faster{{color:var(--good)}}.slower{{color:var(--bad)}}details{{border-top:1px solid var(--line);padding:14px 0;margin-top:18px}}summary{{cursor:pointer;font-weight:650}}a{{color:var(--good);overflow-wrap:anywhere}}code{{overflow-wrap:anywhere}}li{{margin:8px 0}}.explain{{display:grid;gap:12px}}.explain article{{padding:16px 20px;border-left:4px solid var(--line);background:var(--panel)}}.explain article p{{margin:0}}@media(max-width:700px){{main{{padding:18px 12px}}.cards,.charts,.comparison{{grid-template-columns:1fr;gap:14px}}.chart{{padding:14px 10px}}h2{{font-size:22px}}.verdict{{padding:16px}}.verdict p{{font-size:18px}}}}
/* DraftLink injects Tailwind's reset after this stylesheet. Scoped rules must win. */
main .tick,main .label,main .value{{font-size:15px}}main .mode{{font-size:15px}}main .foot{{font-size:13px}}
main h1{{font-size:clamp(27px,4vw,38px);line-height:1.15;font-weight:750;margin:0 0 14px}}main h2{{font-size:24px;font-weight:700;margin:34px 0 12px}}main h3{{font-size:18px;font-weight:650;margin:0 0 8px}}main p{{margin:8px 0 14px}}main li{{margin:8px 0}}main ul{{list-style:disc;padding-left:22px}}main summary{{font-weight:650}}main .cards{{margin-top:20px}}@media(max-width:700px){{main h2{{font-size:22px}}}}
</style></head><body><main><p class="muted">Decision brief · 30 September 2026 · five-minute read</p><h1>Clava protocol and cache decision</h1><div class="verdict"><p>{esc(analysis["recommendation"])}</p></div>{cards(analysis["reasons"])}
<h2>What changes the runtime?</h2><p>Percent change versus Text in the same cache state. Negative is faster. These compare the implemented branches; the controlled format experiment below checks the cause.</p>{comparison(rows)}
<h2>Both suites, every cache state</h2><p class="muted">Whole test-command wall time; builds excluded. Line = median. Box = middle half. Whiskers = full range. Dots = individual runs. Each suite uses one scale across all three sections.</p><div class="charts">{charts}</div>
<h2>Why do the results look this way?</h2>{cards(analysis["discrepancy"], "explain")}{extra_visuals}
<h2>What would we maintain?</h2>{cards(analysis["tradeoffs"])}
<details><summary>Measurement method and limits</summary><p>{esc(analysis["method"])}</p><p>{esc(analysis["limitations"])}</p><ul>{revisions}</ul></details>
<details><summary>Data, run audit, and sources</summary><p>{measurements_download(rows)}</p><ul>{evidence}</ul></details></main></body></html>'''


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--input", type=Path, action="append", required=True)
    p.add_argument("--analysis", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    manifests, provenance, warnings = load_inputs(args.input)
    if warnings:
        raise ValueError("Refuse inconsistent provenance: " + "; ".join(warnings))
    if not all(m.get("schema_version") == 1 and "plan" in m for m in manifests):
        raise ValueError("The decision report requires the fresh deadline matrix schema")
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "analysis"))
    from analyze_deadline import analyze_cohort
    analyze_cohort(args.input, manifests)
    result = render(manifests, provenance, json.loads(args.analysis.read_text()))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(result, encoding="utf-8")


if __name__ == "__main__":
    main()
