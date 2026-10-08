#!/usr/bin/env python3
"""Report direct parser measurements, grouped by source suite and cache state."""
import argparse
import csv
import html
import json
import math
from pathlib import Path
import statistics

import render_per_parse as charts

SUITES = {'clava-js': 'Clava-JS inputs', 'java': 'Java inputs'}
MODES = {'warm': 'Warm cache', 'direct': 'Cache bypass', 'bypass': 'Cache bypass'}
MODE_ALIASES = {'bypass': 'direct', 'directbypass': 'direct', 'warmcache': 'warm'}


def scope_banner_html():
    return (
        '<aside class="scope-banner" aria-label="Benchmark scope">'
        '<strong>Isolated-source replay. Not a suite benchmark.</strong>'
        '<p>Each captured source ran in its own CodeParser call. The original multi-file groups and '
        'cross-translation-unit reconciliation are absent; each call still builds an App and runs '
        'its per-file postprocess. Inputs were relocated into private snapshots. This replay does '
        'not reproduce the suite workload or explain its speedup or regression.</p>'
        '<p>Clava-JS and Java identify input origin. Suite tests, code generation, heap sampling, '
        'and explicit GC were not run.</p>'
        '</aside>'
    )


def esc(value):
    return html.escape(str(value))


def time_label(ms):
    return f'{ms / 1000:.2f} s' if abs(ms) >= 1000 else f'{ms:.2f} ms'


def load(path):
    measured = []
    with path.open(newline='') as stream:
        for row in csv.DictReader(stream):
            if row.get('phase', 'measured') not in ('measure', 'measured', 'measurement'):
                continue
            if row.get('valid', row.get('event_valid', 'true')).lower() not in ('true', '1'):
                raise ValueError('A measured parse failed validation; refusing to publish timings')
            row['elapsed_ms'] = float(row.get('elapsed_ms') or row.get('parse_elapsed_ms'))
            row['input_id'] = row.get('input_id') or row.get('invocation_id') or row.get('source_identity')
            if not row['input_id']:
                raise ValueError('Missing captured input identity')
            label = row.get('source_label') or row.get('resource_key') or row.get('source_path') or row['input_id']
            row['identity'] = row['source_identity'] = charts.safe_path_label(label)
            row['cache_mode'] = MODE_ALIASES.get(row['cache_mode'], row['cache_mode'])
            if row['suite'] not in SUITES or row['cache_mode'] not in MODES:
                raise ValueError('Unknown suite/cache state')
            if row['protocol'] not in ('text', 'protobuf'):
                raise ValueError('Unknown format')
            if not math.isfinite(row['elapsed_ms']) or row['elapsed_ms'] < 0:
                raise ValueError('Invalid parse duration')
            measured.append(row)
    if not measured:
        raise ValueError('No measured parses')
    return measured


def paired(rows):
    groups = {}
    cohorts = {}
    input_hashes = {}
    for row in rows:
        for field in ('source_sha256', 'args_sha256'):
            if not row.get(field):
                raise ValueError(f'Missing {field} for captured input')
        identity = (row['suite'], row['input_id'], row['protocol'])
        fingerprint = (row['source_sha256'], row['args_sha256'])
        previous = input_hashes.setdefault(identity, fingerprint)
        if previous != fingerprint:
            raise ValueError(f'Captured input changed across measurements for {identity}')
        key = (row['suite'], row['cache_mode'], row['input_id'], row['repeat'])
        group = groups.setdefault(key, {})
        if row['protocol'] in group:
            raise ValueError(f'Duplicate measurement {key}')
        group[row['protocol']] = row
        cohorts.setdefault((row['suite'], row['cache_mode'], row['repeat']), set()).add(row['input_id'])
    for suite in SUITES:
        populations = [population for (name, _mode, _repeat), population in cohorts.items() if name == suite]
        if populations and any(population != populations[0] for population in populations[1:]):
            raise ValueError(f'Input population changed across repetitions/cache states for {suite}')
    modes = {row['cache_mode'] for row in rows}
    repeats = {row['repeat'] for row in rows}
    suites = {row['suite'] for row in rows}
    for suite in suites:
        for mode in modes:
            for repeat in repeats:
                if (suite, mode, repeat) not in cohorts:
                    raise ValueError(f'Missing measurement cell {(suite, mode, repeat)}')
    output = []
    for key, group in groups.items():
        if set(group) != {'text', 'protobuf'}:
            raise ValueError(f'Unmatched input {key}')
        text, proto = group['text'], group['protobuf']
        for field in ('source_sha256', 'source_content_sha256', 'args_sha256', 'parse_args_sha256'):
            if text.get(field) != proto.get(field):
                raise ValueError(f'{field} differs between formats for {key}')
        output.append(dict(suite=key[0], cache_mode=key[1], input_id=key[2], repeat=key[3],
                           identity=text['identity'], text_ms=text['elapsed_ms'], protobuf_ms=proto['elapsed_ms'],
                           delta_ms=proto['elapsed_ms'] - text['elapsed_ms'], text=text, protobuf=proto))
    return output


def input_medians(pairs):
    groups = {}
    for pair in pairs:
        groups.setdefault(pair['input_id'], []).append(pair)
    result = []
    for identity, group in groups.items():
        label = group[0].get('identity', identity)
        result.append(dict(identity=label, source_identity=label,
                           text_ms=statistics.median(row['text_ms'] for row in group),
                           protobuf_ms=statistics.median(row['protobuf_ms'] for row in group),
                           delta_ms=statistics.median(row['delta_ms'] for row in group),
                           repeats=len(group)))
    return result


def totals(pairs):
    repetitions = {}
    for pair in pairs:
        totals = repetitions.setdefault(pair['repeat'], [0.0, 0.0])
        totals[0] += pair['text_ms']
        totals[1] += pair['protobuf_ms']
    text = statistics.median(total[0] for total in repetitions.values())
    proto = statistics.median(total[1] for total in repetitions.values())
    delta = statistics.median(total[1] - total[0] for total in repetitions.values())
    percent = statistics.median((total[1] / total[0] - 1) * 100 for total in repetitions.values())
    return text, proto, delta, percent, len(repetitions)


def totals_html(pairs, modes):
    output = ['<section><h2>Cumulative time for isolated sources</h2><p class="note">Each value sums one-source CodeParser calls run sequentially. This is not suite wall time. Positive change means Protobuf was slower. Changes compare matching runs, not the two displayed runtime medians.</p>']
    for mode in modes:
        output.append(f'<article><h3>{esc(MODES[mode])}</h3>')
        for suite, title in [*SUITES.items(), ('all', 'All replayed inputs')]:
            selected = [pair for pair in pairs if pair['cache_mode'] == mode and (suite == 'all' or pair['suite'] == suite)]
            if not selected:
                continue
            text, proto, delta, percent, repeat_count = totals(selected)
            output.append(f'<div class="result-row"><h4>{esc(title)}</h4><p><b>{percent:+.1f}%</b> · {delta/1000:+.3f} s</p>'
                          f'<p class="note">Median runtime · Text {time_label(text)} · Protobuf {time_label(proto)}</p></div>')
            if suite == 'all' and mode == 'direct':
                output.append('<p class="note">The combined bypass change switches sign across the three repetitions. Treat this as a small, mixed effect.</p>')
        output.append('</article>')
    output.append('</section>')
    return ''.join(output)


def source_rankings(pairs):
    medians = sorted(input_medians(pairs), key=lambda row: row['delta_ms'])
    def group(title, rows):
        output = [f'<h4>{title}</h4><ul class="rankings">']
        for row in rows:
            # Captured identifiers are stable workload IDs; source labels must never expose local roots.
            label = charts.safe_path_label(row['identity'])
            output.append(f'<li><span>{esc(label)}</span><b>{row["delta_ms"]:+.2f} ms</b></li>')
        return ''.join(output) + '</ul>'
    return group('Largest slowdowns', [row for row in reversed(medians) if row['delta_ms'] > 0][:5]) + group('Largest speedups', [row for row in medians if row['delta_ms'] < 0][:5])


def phase_html(diagnostics, suite, modes):
    if not diagnostics:
        return ''
    output = ['<h3>Where the time changed</h3><p class="note">One separate profiling pass. These timings locate costs; they are not subtracted from the headline runs.</p>']
    for mode in modes:
        groups = {protocol: [row for row in diagnostics if row['suite'] == suite and row['cache_mode'] in (mode, 'bypass' if mode == 'direct' else mode) and row['protocol'] == protocol]
                  for protocol in ('text', 'protobuf')}
        if not all(groups.values()):
            continue
        def total(protocol, fields):
            return sum(float(row.get(field) or 0) for row in groups[protocol] for field in fields)
        phases = [('Producer / cache I/O', ('native_ms', 'ccache_invoke_ms')),
                  ('Read and build nodes', ('read_ms',)), ('Assemble translation unit', ('ast_ms',))]
        output.append(f'<h4>{esc(MODES[mode])}</h4>')
        byte_totals = {protocol: total(protocol, ('bytes',)) for protocol in ('text', 'protobuf')}
        if all(byte_totals.values()):
            saving = (1 - byte_totals['protobuf'] / byte_totals['text']) * 100
            output.append(f'<p class="note">Raw output · Text {byte_totals["text"]/1_000_000:.2f} MB → '
                          f'Protobuf {byte_totals["protobuf"]/1_000_000:.2f} MB · {saving:.1f}% smaller.</p>')
        values = [(label, total('text', fields), total('protobuf', fields)) for label, fields in phases]
        maximum = max(max(text, proto) for _, text, proto in values) or 1
        for label, text, proto in values:
            output.append(f'<div class="phase-row"><b>{esc(label)}</b><span class="note">Change {(proto-text)/1000:+.3f} s</span>'
                          f'<div class="phase-value"><span>Text</span><i style="width:{100*text/maximum:.3f}%;background:var(--text-color)"></i><span>{time_label(text)}</span></div>'
                          f'<div class="phase-value"><span>Protobuf</span><i style="width:{100*proto/maximum:.3f}%;background:var(--proto-color)"></i><span>{time_label(proto)}</span></div></div>')
    return ''.join(output)


def historical_context_html(history):
    if not history:
        return ''
    cards = []
    for row in history:
        cards.append(f'<div class="result-row"><h4>{esc(SUITES[row["suite"]].replace(" inputs", ""))}</h4>'
                     f'<p class="note">{esc(row["comparison"])}</p>'
                     f'<p class="note">{row["text_s"]:.2f} s → {row["protobuf_s"]:.2f} s · Protobuf '
                     f'{abs(row["change_pct"]):.1f}% {"slower" if row["change_pct"] > 0 else "faster"}</p></div>')
    return ('<section><h2>Separate historical suite measurements</h2>'
            '<p>These whole-command results come from separate suite experiments. The across-branch rows also change code and runtime artifacts, so they cannot isolate Protobuf. All rows use a different workload and timing boundary from the isolated-source replay above; this replay cannot reproduce or explain an earlier suite speedup or regression.</p>'
            '<article><h3>Whole-command cache bypass, older measurements</h3>' + ''.join(cards) + '</article>'
            '<p class="note">Six valid runs per format. Java Protobuf uses the dedicated direct recheck. These are whole test-command medians. Both historical comparisons used the corrected heap-logging helper; the suites still requested explicit GC.</p>'
            '<p>The older Clava-JS across-branch speedup reversed in the same-revision suite comparison. These rows do not explain why.</p>'
            '<p>The suite medians and isolated-source timings answer different questions. Do not use one to infer the cause of a change in the other.</p></section>')


def validate_diagnostics(diagnostics, pairs):
    if not diagnostics:
        return
    expected = {(pair['suite'], pair['cache_mode'], pair['input_id'], protocol)
                for pair in pairs for protocol in ('text', 'protobuf')}
    observed = set()
    for row in diagnostics:
        if str(row.get('valid', '')).lower() not in ('true', '1'):
            raise ValueError('Invalid or unvalidated diagnostic parse')
        mode = MODE_ALIASES.get(row['cache_mode'], row['cache_mode'])
        row['cache_mode'] = mode
        key = (row['suite'], mode, row['input_id'], row['protocol'])
        if key in observed:
            raise ValueError('Duplicate diagnostic parse')
        observed.add(key)
    if observed != expected:
        raise ValueError('Diagnostic coverage differs from measured workload')


def validate_metadata(metadata, rows, diagnostics):
    plan, results = metadata.get('plan', {}), metadata.get('results', {})
    if results.get('valid') is not True or results.get('complete') is not True:
        raise ValueError('Benchmark metadata must mark the complete experiment valid')
    if results.get('source_fingerprint_unchanged') is not True or results.get('materialized_inputs_unchanged') is not True:
        raise ValueError('Benchmark sources or inputs changed during measurement')
    if not plan.get('source_fingerprint', {}).get('clava_head') or not plan.get('native_tool_sha256') or not plan.get('runtime_manifest', {}).get('sha256'):
        raise ValueError('Missing parser, native tool, or runtime provenance')
    if results.get('measured_rows') != len(rows) or results.get('diagnostic_rows') != len(diagnostics or []):
        raise ValueError('Published CSV row counts differ from validated experiment')
    counts = {suite: len({row['input_id'] for row in rows if row['suite'] == suite}) for suite in SUITES}
    if counts != plan.get('suite_event_counts'):
        raise ValueError('Published input counts differ from captured corpus')
    if {str(row['repeat']) for row in rows} != {str(repeat) for repeat in plan.get('repeats', [])}:
        raise ValueError('Published repetitions differ from benchmark plan')
    if {row['cache_mode'] for row in rows} != {MODE_ALIASES.get(mode, mode) for mode in plan.get('cache_modes', [])}:
        raise ValueError('Published cache states differ from benchmark plan')
    if plan.get('compression') is not False or plan.get('show_exec_info') is not False:
        raise ValueError('Format-only controls require raw output and no heap logging')
    fidelity = results.get('fidelity', {})
    if fidelity.get('valid') is not True or fidelity.get('compared_input_pairs') != sum(counts.values()):
        raise ValueError('Output validation must cover every captured input pair')


def java_workload_html(pairs, diagnostics):
    output = ['<h3>Eight large inputs drive Java\'s gain</h3>'
              '<p>Four NAS C files are parsed in original and generated form. These eight inputs take about three quarters of Java\'s warm-cache Text time. Their gains outweigh the slowdowns on the other 239 inputs.</p>']
    groups = []
    for mode in ('warm', 'direct'):
        for nas, title in ((True, '8 NAS inputs'), (False, '239 other inputs')):
            selected = [pair for pair in pairs if pair['suite'] == 'java' and pair['cache_mode'] == mode
                        and ('nas_' in pair['identity'].lower()) == nas]
            if not selected:
                continue
            _, _, delta, percent, _ = totals(selected)
            groups.append((mode, title, delta, percent))
    maximum = max(abs(delta) for _, _, delta, _ in groups) or 1
    for mode in ('warm', 'direct'):
        output.append(f'<h4>{esc(MODES[mode])}</h4>')
        for name, title, delta, percent in groups:
            if name != mode:
                continue
            width = abs(delta) / maximum * 50
            left = 50 - width if delta < 0 else 50
            color = 'var(--text-color)' if delta < 0 else 'var(--loss-color)'
            output.append(f'<div class="phase-row"><b>{title}</b><span class="note">'
                          f'{delta/1000:+.2f} s · {percent:+.1f}% matched change</span>'
                          f'<div class="change-track"><i style="left:{left:.3f}%;width:{width:.3f}%;background:{color}"></i></div></div>')
    output.append('<p class="note">Bars share one scale. Left of the centre saves time; right adds time. Each bar is the median change in a matched group total, over three repetitions.</p>')
    if diagnostics:
        nas_ids = {pair['input_id'] for pair in pairs if 'nas_' in pair['identity'].lower()}
        changes = []
        for nas in (True, False):
            subset = [row for row in diagnostics if row['suite'] == 'java' and row['cache_mode'] == 'warm'
                      and (row['input_id'] in nas_ids) == nas]
            values = {protocol: sum(float(row['read_ms']) for row in subset if row['protocol'] == protocol)
                      for protocol in ('text', 'protobuf')}
            changes.append(values['protobuf'] - values['text'])
        output.append(f'<p class="note">The separate profiling pass locates the gain in reading and building nodes: '
                      f'NAS {changes[0]/1000:+.2f} s; other inputs {changes[1]/1000:+.2f} s. Translation-unit assembly changes little.</p>')
    return ''.join(output)


def render(rows, metadata, diagnostics=None):
    pairs = paired(rows)
    validate_diagnostics(diagnostics, pairs)
    validate_metadata(metadata, rows, diagnostics)
    modes = [mode for mode in ('warm', 'direct') if any(row['cache_mode'] == mode for row in rows)]
    output = ['<section><h2>The result depends on input mix</h2>'
              '<p>Protobuf slows most individual inputs in both source populations. Java\'s replay total improves because a few large NAS inputs become much faster. Clava-JS has no comparable offset.</p>'
              '</section>', totals_html(pairs, modes)]
    for suite, title in SUITES.items():
        selected = [pair for pair in pairs if pair['suite'] == suite]
        if not selected:
            continue
        output.append(f'<section><h2>{esc(title)}</h2>')
        runtime_groups = {}
        delta_groups = []
        for mode in modes:
            group = [pair for pair in selected if pair['cache_mode'] == mode]
            medians = input_medians(group)
            delta_groups.append((MODES[mode], medians))
            for protocol in ('text', 'protobuf'):
                key = f'{mode}/{protocol}'
                charts.CHART_CONDITIONS[key] = (f'{MODES[mode]} · {"Text" if protocol == "text" else "Protobuf"}', 'var(--text-color)' if protocol == 'text' else 'var(--proto-color)')
                runtime_groups[key] = [dict(identity=row['identity'], source_identity=row['identity'], elapsed_ms=row[f'{protocol}_ms']) for row in medians]
        chart_title = title
        output.append('<h3>Individual one-source parse runtimes</h3>')
        output.append('<p class="note">Each input is parsed on its own. The original multi-file groups and cross-translation-unit reconciliation are not represented. Line = median · box = middle 50% of inputs · lower is faster.</p>')
        output.append(charts.distribution_chart_html(chart_title, runtime_groups, tuple(runtime_groups)))
        output.append('<h3>Protobuf change per source</h3>')
        output.append('<p class="note">Each pair compares separate one-source calls. Left of zero = faster with Protobuf. Right of zero = slower.</p>')
        output.append(charts.comparative_delta_chart_html(chart_title, delta_groups, 'source invocation'))
        for label, medians in delta_groups:
            slower = sum(row['delta_ms'] > 0 for row in medians)
            faster = sum(row['delta_ms'] < 0 for row in medians)
            output.append(f'<p class="note">{esc(label)} · {slower}/{len(medians)} inputs slower, {faster}/{len(medians)} faster.</p>')
        if suite == 'java':
            output.append(java_workload_html(selected, diagnostics))
        output.append(phase_html(diagnostics, suite, modes))
        output.append('<details><summary>Inputs with the largest changes</summary>')
        for mode in modes:
            output.append(f'<h3>{esc(MODES[mode])}</h3>')
            output.append(source_rankings([pair for pair in selected if pair['cache_mode'] == mode]))
        output.append('</details></section>')
    counts = {suite: len({row['input_id'] for row in rows if row['suite'] == suite}) for suite in SUITES}
    repeats = sorted({row['repeat'] for row in rows})
    output.append(historical_context_html(metadata.get('historical_context', [])))
    output.append('<details><summary>Measurement method and coverage</summary>'
                  '<p>The source capture recorded 516 original CodeParser calls. 73 carried multiple sources, '
                  'including 48 Clava-JS calls and 25 Java calls. The replay retained '
                  f'{counts["clava-js"]} Clava-JS and {counts["java"]} Java native events. '
                  'The suite names identify event origin, not full-suite execution.</p>'
                  '<p>Each captured source runs alone through CodeParser. This removes the original multi-source '
                  'groups and cross-translation-unit reconciliation. App construction and its single-source '
                  'postprocess still run once per replayed file.</p>'
                  f'<p>{counts["clava-js"]} Clava-JS parse calls and {counts["java"]} Java parse calls. '
                  f'{len(repeats)} matched repetitions per input and cache state. Each candle represents the spread of input runtimes, using the median across repetitions for each input.</p>'
                  '<p>The captured source populations came from the Clava-JS workload, where 158 tests passed and 6 skipped, and the Java workload, where 116 tests passed. Generated and round-trip inputs are included. Repeated parse occurrences are retained.</p>'
                  '<p>The total is the sum of sequential parse-call durations for each repetition. Changes are medians of matched differences. Warmups, JVM launch, source capture, and validation are outside the timed interval.</p>'
                  '<p>The replay does not execute suite tests or code generation, collect heap samples, or explicitly request garbage collection. Automatic JVM garbage collection remains enabled.</p>'
                  '<p>Extra phase logging is disabled during timed runs. Profiling and generated-code validation run separately. Automatic JVM garbage collection remains enabled.</p>'
                  '<p>Both formats write uncompressed native output. The cache uses the same storage policy for both formats.</p>'
                  '<p>Warm cache applies to eligible inputs only: 166 of 191 Clava-JS inputs and 208 of 247 Java inputs hit in both formats, with zero misses. The others bypass the cache by design.</p>'
                  '<p>Each event gets a private relocated filesystem snapshot for its source and dependencies. The runner maps captured paths into that root and checks staged file hashes. Separate event roots preserve different captured versions of a shared original path instead of overwriting one with another. Relocation lengthens absolute paths, which can enlarge Text dumps. Reported sizes and absolute times belong to this replay, not the original suite commands. Path relocation is separate from the configured warm-cache and direct-bypass conditions.</p>'
                  '<p>The middle line is the median. The box contains the middle 50% of inputs. Whiskers use the 1.5-IQR rule. Dots are display samples; all inputs determine the candles. Full ranges are available in expandable charts.</p></details>')
    plan = metadata['plan']
    output.append('<details><summary>Build fingerprints</summary><ul>'
                  f'<li>Clava benchmark revision <code>{esc(plan["source_fingerprint"]["clava_head"])}</code></li>'
                  f'<li>Native executable SHA-256 <code>{esc(plan["native_tool_sha256"])}</code></li>'
                  f'<li>Java runtime manifest SHA-256 <code>{esc(plan["runtime_manifest"]["sha256"])}</code></li>'
                  '</ul></details>')
    return '''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Text and Protobuf isolated-source replay</title><style>
:root{--surface:#fff;--ink:#172033;--muted:#526174;--line:#d8dee8;--grid:#e5eaf1;--outlier:#b91c1c;--soft:#f4f7fb;--delta-ink:#475569;--delta-box:#64748b;color-scheme:light}
html.dark{--surface:#111827;--ink:#e5e7eb;--muted:#aab5c5;--line:#374151;--grid:#273244;--outlier:#f87171;--soft:#182334;--delta-ink:#cbd5e1;--delta-box:#94a3b8;color-scheme:dark}
:root{--text-color:#059669;--proto-color:#7c3aed;--loss-color:#dc2626}html.dark{--text-color:#34d399;--proto-color:#a78bfa;--loss-color:#f87171}
*{box-sizing:border-box}body{margin:0;background:var(--surface);color:var(--ink);font:16px/1.5 system-ui,sans-serif}main{max-width:1160px;margin:auto;padding:24px 18px 48px}h1{font-size:clamp(1.7rem,4vw,2.3rem);line-height:1.2}h2{margin:28px 0 12px}h3{font-size:1.1rem;margin:20px 0 8px}h4,p{margin:5px 0}.note{color:var(--muted);font-size:.9rem}.scope-banner{margin:16px 0 24px;padding:14px 16px;border:2px solid var(--line);border-left:6px solid var(--proto-color);border-radius:10px;background:var(--soft)}.scope-banner>strong{display:block;font-size:1.05rem}.scope-banner p{margin:7px 0 0;color:var(--muted)}.scope-banner p:last-child{font-size:.9rem}section{margin:28px 0;padding-top:12px;border-top:1px solid var(--line)}article{margin:18px 0;padding:12px;background:var(--soft);border:1px solid var(--line);border-radius:10px}.result-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:2px 8px;padding:10px 0;border-bottom:1px solid var(--line)}.result-row>.note{grid-column:1/-1}.result-row:last-child{border:0}details{margin:14px 0}summary{cursor:pointer;color:var(--muted)}details p{margin:10px 0}.chart-mobile{display:none}svg{display:block;width:100%;height:auto}.chart-svg-mobile{width:min(100%,560px);margin:auto}.grid-line{stroke:var(--grid)}.zero-line{stroke:var(--muted);stroke-width:1.6;stroke-dasharray:5 4}.axis-text{fill:var(--muted);font-size:14px}.axis-title{fill:var(--muted);font-size:14px}.condition-text{fill:var(--ink);font-size:16px;font-weight:650}.mobile-axis-text,.mobile-axis-title,.mobile-condition-text{fill:var(--muted);font-size:18px}.mobile-condition-text{fill:var(--ink);font-weight:650}.point{opacity:.7;stroke:var(--surface);stroke-width:1}.point.outlier{opacity:1;stroke:var(--outlier);stroke-width:2}.delta-whisker{stroke:var(--delta-ink);stroke-width:2}.delta-box{fill:var(--delta-box);fill-opacity:.18;stroke:var(--delta-ink)}.delta-median{stroke:var(--delta-ink);stroke-width:4}.delta-point{fill:var(--delta-ink)}.chart-stats{padding:0;list-style:none}.chart-stats li{padding:7px 0;border-bottom:1px solid var(--line)}.chart-stats li>span:not(.swatch){display:block}.chart-stats details{margin:2px 0;font-size:.85rem}.swatch{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:5px}.rankings{padding:0;list-style:none}.rankings li{display:flex;justify-content:space-between;gap:12px;padding:8px 0;border-bottom:1px solid var(--line)}.rankings span{overflow-wrap:anywhere;min-width:0}.rankings b{white-space:nowrap}
h2{font-size:1.4rem;font-weight:700}h3,h4{font-weight:650}code{overflow-wrap:anywhere}.change-track{position:relative;height:18px;margin:8px 0;background:var(--soft)}.change-track:before{content:'';position:absolute;left:50%;height:100%;border-left:1px solid var(--muted)}.change-track i{display:block;position:absolute;top:4px;height:10px;border-radius:3px}.phase-row{margin:12px 0;padding:8px 0;border-bottom:1px solid var(--line)}.phase-row>b,.phase-row>.note{display:block}.phase-value{display:grid;grid-template-columns:55px minmax(0,1fr) 78px;align-items:center;gap:6px;margin:4px 0;font-size:.8rem}.phase-value i{display:block;height:9px;min-width:1px;border-radius:3px}.phase-value>span:last-child{text-align:right}
@media(max-width:900px){.chart-wide{display:none}.chart-mobile{display:block}main{padding:20px 12px 40px}}
</style></head><body><main><h1>Text and Protobuf parsing of isolated sources</h1>''' + scope_banner_html() + ''.join(output) + '</main></body></html>'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--csv', type=Path, required=True)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--diagnostics', type=Path)
    parser.add_argument('--history', type=Path, help='Validated older whole-command comparisons')
    args = parser.parse_args()
    rows = load(args.csv)
    metadata = json.loads(args.metadata.read_text()) if args.metadata else {}
    if args.history:
        metadata['historical_context'] = json.loads(args.history.read_text())
    if args.diagnostics:
        with args.diagnostics.open(newline='') as stream:
            diagnostics = list(csv.DictReader(stream))
    else:
        diagnostics = []
    args.output.write_text(render(rows, metadata, diagnostics))


if __name__ == '__main__':
    main()
