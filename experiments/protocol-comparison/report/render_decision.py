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


def validate_uninstrumented_headlines(rows):
    """Never relabel a normal coverage-enabled Gradle run as parser performance."""
    for row in rows:
        if row.get("suite") != "java" or not row.get("selected", True) or not is_measured(row):
            continue
        argv = row.get("actual_test_executor_args")
        if (row.get("agent") != "off" or not isinstance(argv, list) or not argv
                or any(str(arg).startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))
                       for arg in argv)):
            raise ValueError("Java headline requires verified uninstrumented Test-worker arguments")


def validate_matched_syntax(rows):
    for row in rows:
        if row.get("selected", True) and is_measured(row) and row.get("fast_syntax") is not True:
            raise ValueError("Matched headline requires fast_syntax=true in every selected measurement")


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
                reference = {r["repeat"]: time_value(r) for r in cell(rows, suite, mode, "ccache-text")}
                candidate = {r["repeat"]: time_value(r) for r in cell(rows, suite, mode, stage)}
                effects = [100 * (candidate[n] / reference[n] - 1) for n in sorted(reference)]
                delta = statistics.median(effects)
                faster = sum(value < 0 for value in effects)
                slower = sum(value > 0 for value in effects)
                direction = "faster" if delta < 0 else "slower"
                count = faster if delta < 0 else slower
                consistent = count == len(effects)
                colour = direction if consistent else "muted"
                note = f"{count}/{len(effects)} {direction}" if delta else "tied"
                out.append(f'<span class="delta {colour}">{delta:+.1f}%<small style="display:block;font-size:12px;font-weight:400">{note}</small></span>')
            out.append('</div>')
        out.append('</div>')
    return ''.join(out) + '</div>'


def cards(items, class_name="cards"):
    return f'<div class="{class_name}">' + ''.join(
        f'<article><h3>{esc(item["title"])}</h3><p>{esc(item["text"])}</p></article>' for item in items) + '</div>'


def diagnostic_chart(spec):
    """Compact paired-change candles, with labels above rather than over bars."""
    groups = spec["rows"]
    limit = max(.1, max(abs(v) for row in groups for v in row["values"]) * 1.12)
    x = lambda value: 20 + 320 * (value + limit) / (2 * limit)
    height = 58 + len(groups) * 76
    out = [f'<section class="diagnostic"><h3>{esc(spec["title"])}</h3><p>{esc(spec["caption"])}</p>',
           f'<svg viewBox="0 0 360 {height}" role="img" aria-label="{esc(spec["title"])}">']
    for value in (-limit, 0, limit):
        anchor = "start" if value < 0 else "end" if value > 0 else "middle"
        out.append(f'<text x="{x(value):.1f}" y="20" text-anchor="{anchor}">{value:+.1f}s</text>')
    for i, row in enumerate(groups):
        values = row["values"]
        med = statistics.median(values)
        y = 70 + i * 76
        colour = "var(--good)" if med < 0 else "var(--bad)"
        out += [f'<text x="20" y="{y-15}">{esc(row["label"])}</text>',
                f'<text x="340" y="{y-15}" text-anchor="end">{med:+.2f}s</text>',
                f'<line class="grid" x1="20" x2="340" y1="{y}" y2="{y}"/>',
                f'<line class="divider" x1="{x(0)}" x2="{x(0)}" y1="{y-10}" y2="{y+10}"/>',
                f'<line x1="{x(min(values))}" x2="{x(max(values))}" y1="{y}" y2="{y}" stroke="{colour}" stroke-width="2"/>',
                f'<rect x="{x(quantile(values,.25))}" y="{y-7}" width="{max(2,x(quantile(values,.75))-x(quantile(values,.25)))}" height="14" fill="{colour}" fill-opacity=".2" stroke="{colour}"/>',
                f'<line x1="{x(med)}" x2="{x(med)}" y1="{y-9}" y2="{y+9}" stroke="{colour}" stroke-width="3"/>']
        for n, value in enumerate(values):
            out.append(f'<circle cx="{x(value)}" cy="{y+(n%3-1)*3}" r="3" fill="{colour}"><title>Pair {n+1}: {value:+.4f} seconds</title></circle>')
    out += ['</svg></section>']
    return ''.join(out)


def measurements_download(rows):
    """Publish measurement fields without commands or local filesystem paths."""
    fields = ("suite", "stage", "mode", "repeat", "elapsed_s",
              "junit_aggregate_s", "total_tests", "passed_tests",
              "failed_tests", "skipped_tests", "cacheable_calls",
              "cache_hits", "cache_misses", "agent", "no_explicit_gc_flags", "fast_syntax",
              "clava_revision", "native_revision", "native_binary_sha256",
              "clava_patch_sha256", "native_build_provenance_sha256",
              "runtime_manifest_sha256")
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
    return f'<a download="clava-suite-measurements.csv" href="data:text/csv;charset=utf-8;base64,{payload}">All {count} accepted suite timings (CSV)</a>'


def csv_viewer():
    """CSV export remains usable inside DraftLink's download-blocking sandbox."""
    return '''<details id="csv-viewer" hidden><summary id="csv-name">CSV data</summary>
<p>Copy the selected text into a file with the displayed .csv name. This avoids DraftLink's embedded-download restriction.</p>
<button type="button" id="csv-select">Select all CSV text</button>
<textarea id="csv-text" readonly spellcheck="false" aria-label="CSV contents" style="display:block;width:100%;height:240px;margin-top:12px;font:13px monospace;color:var(--ink);background:var(--panel);border:1px solid var(--line)"></textarea>
</details><script>
document.addEventListener('click', function(event) {
  const link = event.target.closest('a[href^="data:text/csv"]');
  if (!link) return;
  event.preventDefault();
  const viewer = document.getElementById('csv-viewer');
  const text = document.getElementById('csv-text');
  const payload = link.getAttribute('href').split('base64,')[1];
  text.value = new TextDecoder().decode(Uint8Array.from(atob(payload), c => c.charCodeAt(0)));
  document.getElementById('csv-name').textContent = link.getAttribute('download') || 'CSV data';
  viewer.hidden = false;
  viewer.open = true;
  viewer.scrollIntoView({block:'center'});
  text.focus();
  text.select();
});
document.getElementById('csv-select').addEventListener('click', function() {
  const text = document.getElementById('csv-text');
  text.focus();
  text.select();
});
</script>'''


def csv_link(path):
    if path.suffix != ".csv":
        raise ValueError("Evidence exports must be CSV files")
    content = path.read_bytes()
    if b"/home/" in content or b"/tmp/" in content:
        raise ValueError("Evidence CSV contains private local paths")
    payload = base64.b64encode(content).decode()
    return f'<p><a download="{esc(path.name)}" href="data:text/csv;charset=utf-8;base64,{payload}">{esc(path.name)}</a></p>'


def render(manifests, provenance, analysis):
    rows = flatten_results(manifests)
    repeat_counts = {m.get("plan", {}).get("repeat_count", 6) for m in manifests}
    if len(repeat_counts) != 1:
        raise ValueError("Repeat count differs across manifests")
    repeat_count = repeat_counts.pop()
    validate_matrix(rows, repeat_count)
    uninstrumented = analysis.get("headline_policy") == "uninstrumented"
    if uninstrumented:
        validate_uninstrumented_headlines(rows)
    if analysis.get("validation_policy") == "syntax-only":
        validate_matched_syntax(rows)
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
    java_label = "116 pass · coverage agent disabled" if uninstrumented else "116 pass · coverage agent enabled"
    charts = ''.join(f'<article class="chart"><h3>{esc(SUITES[s]["title"])}</h3><p>{"158 pass · 6 skip · no coverage agent" if s=="clava-js" else java_label} · {repeat_count} runs per candle</p>{candle(s,rows)}</article>' for s in SUITES)
    evidence = ''.join(f'<li><a href="{esc(e["url"])}">{esc(e["title"])}</a></li>' for e in analysis["evidence"])
    revisions = ''.join(
        f'<li>{esc(STAGES[k][0])}: Clava <code>{esc(next(iter(v.values())).get("clava_revision", "not recorded"))}</code>; '
        f'native <code>{esc(next(iter(v.values())).get("native_revision", "not recorded"))}</code>; '
        f'producer SHA-256 <code>{esc(next(iter(v.values())).get("native_binary_sha256", "not recorded"))}</code></li>'
        for k, v in provenance.items())
    extra_visuals = '<div class="diagnostics">' + ''.join(diagnostic_chart(spec) for spec in analysis.get("diagnostic_charts", [])) + '</div>' + analysis.get("reviewed_visuals_html", "")
    detail_visuals = analysis.get("detail_visuals_html", "")
    cohort_note = f'<p class="muted">{esc(analysis["cohort_note"])}</p>' if analysis.get("cohort_note") else ""
    suite_visuals = f'''{analysis.get("decision_visuals_html", "")}
<h2>Whole-command change versus Text</h2><p>Median same-round change versus Text in the same cache state. Negative is faster. Counts show how often that direction repeated. These compare implemented branches, not the format alone.</p>{comparison(rows)}
<h2>Both suites, every cache state</h2><p class="muted">Whole test-command wall time; builds excluded. Line = median. Box = middle half. Whiskers = full range. Dots = individual runs. Each suite uses one scale across all three sections.</p><div class="charts">{charts}</div>'''
    app_visuals = analysis.get("app_build_visuals_html", "")
    reason_cards = cards(analysis["reasons"])
    lead_reason_cards, following_reason_cards = reason_cards, ""
    if analysis.get("primary_timing") == "app-build":
        if not app_visuals:
            raise ValueError("App-build headline requires its validated evidence fragment")
        performance_visuals = app_visuals + '<details><summary>Whole-suite command timings and binary controls</summary>' + suite_visuals + '</details>'
        lead_reason_cards, following_reason_cards = "", reason_cards
    else:
        performance_visuals = suite_visuals.replace("Whole-command change versus Text", "What changes the runtime?", 1) + app_visuals
    return f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Clava protocol and cache decision</title><style>
:root{{--bg:#fff;--ink:#18212e;--muted:#536174;--panel:#f4f7fa;--line:#d5dfe8;--good:#08744c;--bad:#ae3535}}html.dark{{--bg:#111923;--ink:#edf2f7;--muted:#b2c0ce;--panel:#1c2836;--line:#3c4b5e;--good:#77dcb1;--bad:#ffaaa2}}*{{box-sizing:border-box}}body{{margin:0;background:var(--bg);color:var(--ink);font:16px/1.5 system-ui,sans-serif}}main{{max-width:1100px;margin:auto;padding:24px}}h1{{font-size:clamp(27px,4vw,38px);line-height:1.15;margin:0 0 14px}}h2{{font-size:24px;margin:34px 0 12px}}h3{{font-size:18px;margin:0 0 8px}}p{{margin:8px 0 14px}}.muted,.chart>p{{color:var(--muted);font-size:14px}}.verdict{{padding:22px;background:var(--panel);border-left:5px solid var(--good);border-radius:8px}}.verdict p{{font-size:20px;margin:0}}.cards,.charts,.comparison{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}}.cards article,.chart,.comparison>div{{border:1px solid var(--line);border-radius:10px;padding:18px;min-width:0}}.cards article p{{margin:0}}.charts svg{{display:block;width:100%;height:auto;max-width:480px;margin:auto}}svg text{{fill:var(--ink);font-family:system-ui,sans-serif}}.grid{{stroke:var(--line);stroke-dasharray:3 4}}.divider{{stroke:var(--line)}}.tick,.label,.value{{font-size:12px}}.mode{{font-size:13px;font-weight:650}}.foot{{font-size:11px;fill:var(--muted)}}.matrix-row{{display:grid;grid-template-columns:1.4fr repeat(3,1fr);gap:8px;padding:10px 0;border-bottom:1px solid var(--line);font-size:14px;align-items:center}}.matrix-row span:not(:first-child){{text-align:center}}.matrix-head{{color:var(--muted);font-size:12px}}.delta{{font-weight:700}}.faster{{color:var(--good)}}.slower{{color:var(--bad)}}details{{border-top:1px solid var(--line);padding:14px 0;margin-top:18px}}summary{{cursor:pointer;font-weight:650}}a{{color:var(--good);overflow-wrap:anywhere}}code{{overflow-wrap:anywhere}}li{{margin:8px 0}}.explain{{display:grid;gap:12px}}.explain article{{padding:16px 20px;border-left:4px solid var(--line);background:var(--panel)}}.explain article p{{margin:0}}@media(max-width:700px){{main{{padding:18px 12px}}.cards,.charts,.comparison{{grid-template-columns:1fr;gap:14px}}.chart{{padding:14px 10px}}h2{{font-size:22px}}.verdict{{padding:16px}}.verdict p{{font-size:18px}}}}
/* DraftLink injects Tailwind's reset after this stylesheet. Scoped rules must win. */
main .tick,main .label,main .value{{font-size:15px}}main .mode{{font-size:15px}}main .foot{{font-size:13px}}
main .cards>article:last-child:nth-child(odd){{grid-column:1/-1}}
main .java-format-control{{color:var(--ink)}}main .java-format-control svg text{{fill:var(--ink)}}main .java-format-control .median{{fill:var(--ink);stroke:var(--bg)}}main .java-format-control .whisker{{stroke:var(--ink)}}
main h1{{font-size:clamp(27px,4vw,38px);line-height:1.15;font-weight:750;margin:0 0 14px}}main h2{{font-size:24px;font-weight:700;margin:34px 0 12px}}main h3{{font-size:18px;font-weight:650;margin:0 0 8px}}main p{{margin:8px 0 14px}}main li{{margin:8px 0}}main ul{{list-style:disc;padding-left:22px}}main summary{{font-weight:650}}main .cards{{margin-top:20px}}@media(max-width:700px){{main h2{{font-size:22px}}}}
.diagnostics{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px;margin:22px 0}}.diagnostic{{min-width:0;padding:16px;border:1px solid var(--line);border-radius:10px}}.diagnostic svg{{display:block;width:100%;max-width:480px;height:auto;margin:auto}}.diagnostic svg text{{font-size:15px}}.diagnostic p{{font-size:14px;color:var(--muted)}}@media(max-width:700px){{.diagnostics{{grid-template-columns:1fr}}}}
</style></head><body><main><p class="muted">Decision brief · {esc(analysis.get("measurement_date", "30 September to 1 October 2026"))} · five-minute read</p><h1>Clava protocol and cache decision</h1><div class="verdict"><p>{esc(analysis["recommendation"])}</p></div>{cohort_note}{lead_reason_cards}
{performance_visuals}{following_reason_cards}
<h2>Why do the results look this way?</h2>{cards(analysis["discrepancy"], "explain")}{extra_visuals}
<h2>What would we maintain?</h2>{cards(analysis["tradeoffs"])}
{detail_visuals}
<details><summary>Measurement method and limits</summary><p>{esc(analysis["method"])}</p><p>{esc(analysis["limitations"])}</p><ul>{revisions}</ul></details>
<details><summary>Data, run audit, and sources</summary><p>CSV links open selectable data in this page.</p><p>{measurements_download(rows)}</p>{analysis.get("extra_csv_html", "")}<ul>{evidence}</ul></details>{csv_viewer()}</main></body></html>'''


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--input", type=Path, action="append", required=True)
    p.add_argument("--analysis", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--decision-fragment", type=Path, action="append", default=[],
                   help="Reviewed head-to-head decision evidence, shown immediately below the verdict")
    p.add_argument("--visual-fragment", type=Path, action="append", default=[],
                   help="Reviewed, self-contained HTML evidence fragment; repeat in display order")
    p.add_argument("--app-build-fragment", type=Path, action="append", default=[],
                   help="Validated App-building evidence, shown beside whole-suite timings")
    p.add_argument("--detail-fragment", type=Path, action="append", default=[],
                   help="Reviewed supplementary evidence, collapsed to keep the decision brief short")
    p.add_argument("--csv-file", type=Path, action="append", default=[],
                   help="Sanitized evidence CSV, embedded for the selectable data viewer")
    args = p.parse_args()
    manifests, provenance, warnings = load_inputs(args.input)
    if warnings:
        raise ValueError("Refuse inconsistent provenance: " + "; ".join(warnings))
    if not all(m.get("schema_version") == 1 and "plan" in m for m in manifests):
        raise ValueError("The decision report requires the fresh deadline matrix schema")
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "analysis"))
    from analyze_deadline import analyze_cohort
    analyze_cohort(args.input, manifests)
    analysis = json.loads(args.analysis.read_text())
    analysis["decision_visuals_html"] = analysis.get("decision_visuals_html", "") + ''.join(
        fragment.read_text(encoding="utf-8") for fragment in args.decision_fragment)
    analysis["extra_csv_html"] = ''.join(csv_link(path) for path in args.csv_file)
    analysis["reviewed_visuals_html"] = analysis.get("reviewed_visuals_html", "") + ''.join(
        fragment.read_text(encoding="utf-8") for fragment in args.visual_fragment)
    analysis["app_build_visuals_html"] = analysis.get("app_build_visuals_html", "") + ''.join(
        fragment.read_text(encoding="utf-8") for fragment in args.app_build_fragment)
    if args.detail_fragment:
        analysis["detail_visuals_html"] = '<details><summary>Per-group replay, distributions and CSVs</summary>' + ''.join(
            fragment.read_text(encoding="utf-8") for fragment in args.detail_fragment) + '</details>'
    result = render(manifests, provenance, analysis)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(result, encoding="utf-8")


if __name__ == "__main__":
    main()
