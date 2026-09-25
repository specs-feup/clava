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
AB_STAGES = {
    "ab-text": ("Text", "#059669"),
    "ab-protobuf": ("Protobuf", "#7c3aed"),
}
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
    return f"{value:.2f}s"


def time_axis_ticks(low: float, high: float) -> list[tuple[float, str]]:
    """Place time ticks at regular, readable values inside the plotted domain."""
    if not math.isfinite(low) or not math.isfinite(high) or low >= high:
        raise ValueError("A time axis needs a finite, increasing range")

    rough_step = (high - low) / 4
    exponent = math.floor(math.log10(rough_step))
    candidates = {
        factor * 10 ** power
        for power in range(exponent - 2, exponent + 2)
        for factor in (1, 2, 2.5, 5, 10)
    }

    def positions(step: float) -> list[float]:
        first = math.ceil(low / step - 1e-9)
        last = math.floor(high / step + 1e-9)
        return [index * step for index in range(first, last + 1)]

    step = min(
        candidates,
        key=lambda candidate: (
            0 if 3 <= len(positions(candidate)) <= 7 else 1,
            abs(len(positions(candidate)) - 5),
            abs(math.log(candidate / rough_step)),
        ),
    )
    ticks = positions(step)
    for decimals in range(10):
        rounded = [round(tick, decimals) for tick in ticks]
        if len(set(rounded)) == len(rounded) and all(
                abs(value - tick) <= step * 1e-6 for value, tick in zip(rounded, ticks)):
            return [(tick, f"{value:.{decimals}f}s") for tick, value in zip(ticks, rounded)]
    raise ValueError("Time-axis ticks are too close to label distinctly")


def load_ab_result(path: Path) -> dict[str, Any]:
    manifest = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(manifest, dict) or not isinstance(manifest.get("results"), list):
        raise ValueError(f"{path} is not a dual-format A/B results manifest")
    gate = manifest.get("fidelity_gate")
    if manifest.get("valid") is not True or not isinstance(gate, dict) or gate.get("passed") is not True:
        raise ValueError(f"{path} has not passed its full A/B and fidelity gates")
    rows = manifest["results"]
    expected = manifest.get("repeat_count")
    if type(expected) is not int or expected < 2:
        raise ValueError(f"{path} needs at least two measured repeats per format")
    for suite in SUITES:
        for stage in AB_STAGES:
            selected = [row for row in rows if isinstance(row, dict) and row.get("suite") == suite
                        and row.get("stage") == stage and row.get("measured") is True]
            repeats = {row.get("repeat") for row in selected}
            if len(selected) != expected or repeats != set(range(1, expected + 1)):
                raise ValueError(f"{path} has incomplete {suite}/{stage} measured repeats")
            if any(not is_valid_run(row, suite) or row.get("mode") != "direct" for row in selected):
                raise ValueError(f"{path} has invalid {suite}/{stage} measured runs")
            if any(row.get("compressed") is not False or row.get("ccache_disabled") is not True
                   for row in selected):
                raise ValueError(f"{path} is not a matched, uncompressed ccache-bypass A/B")
            if any(not isinstance(row.get("metrics"), dict) or any(
                    number(row["metrics"].get(field)) is None
                    for field in ("native_ms", "read_ms", "ast_ms", "dump_bytes"))
                   for row in selected):
                raise ValueError(f"{path} has incomplete {suite}/{stage} phase or size metrics")
    return manifest


def ab_values(manifest: dict[str, Any], suite: str, stage: str) -> list[tuple[float, dict[str, Any]]]:
    return sorted(
        [(value, row) for row in manifest["results"]
         if isinstance(row, dict) and row.get("suite") == suite and row.get("stage") == stage
         and row.get("measured") is True and (value := time_value(row)) is not None],
        key=lambda item: int(item[1]["repeat"]),
    )


def ab_delta(manifest: dict[str, Any], suite: str) -> tuple[float, float, float]:
    text_values = [value for value, _ in ab_values(manifest, suite, "ab-text")]
    proto_values = [value for value, _ in ab_values(manifest, suite, "ab-protobuf")]
    text_median = statistics.median(text_values)
    proto_median = statistics.median(proto_values)
    return text_median, proto_median, (proto_median / text_median - 1) * 100


def ab_candle_svg(manifest: dict[str, Any], suite: str) -> str:
    groups = {stage: ab_values(manifest, suite, stage) for stage in AB_STAGES}
    observed = [value for group in groups.values() for value, _ in group]
    low, high = min(observed), max(observed)
    padding = max((high - low) * 0.15, 0.25)
    low, high = max(0.0, low - padding), high + padding
    width, height, left, right = 1010, 230, 220, 725

    def x(value: float) -> float:
        return left + (right - left) * (value - low) / (high - low)

    bits = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(SUITES[suite]["title"])} same-revision text and Protobuf wall-time candles" class="candle-chart">',
        f'<title>{esc(SUITES[suite]["title"])} same-revision A/B wall time</title>',
        '<desc>Two modes of one native binary and Java runtime. Whiskers show minimum and maximum, boxes the interquartile range, center marks the median, and dots individual paired-repeat results.</desc>',
    ]
    for tick, label in time_axis_ticks(low, high):
        tx = x(tick)
        bits.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="24" y2="204" class="grid-line"/>')
        bits.append(f'<text x="{tx:.2f}" y="18" text-anchor="middle" class="axis-text">{esc(label)}</text>')
    for index, (stage, (label, color)) in enumerate(AB_STAGES.items()):
        group = groups[stage]
        values = [value for value, _ in group]
        y = 82 + index * 84
        q1, median, q3 = quantile(values, .25), statistics.median(values), quantile(values, .75)
        bits.append(f'<text x="14" y="{y + 5}" class="stage-text">{esc(label)}</text>')
        bits.append(f'<line x1="{x(min(values)):.2f}" x2="{x(max(values)):.2f}" y1="{y}" y2="{y}" stroke="{color}" stroke-width="2"/>')
        for endpoint in (min(values), max(values)):
            bits.append(f'<line x1="{x(endpoint):.2f}" x2="{x(endpoint):.2f}" y1="{y - 9}" y2="{y + 9}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<rect x="{x(q1):.2f}" y="{y - 15}" width="{max(3, x(q3) - x(q1)):.2f}" height="30" rx="4" fill="{color}" fill-opacity=".23" stroke="{color}" stroke-width="1.6"><title>Q1 {esc(fmt_seconds(q1))} to Q3 {esc(fmt_seconds(q3))}</title></rect>')
        bits.append(f'<line x1="{x(median):.2f}" x2="{x(median):.2f}" y1="{y - 16}" y2="{y + 16}" stroke="{color}" stroke-width="4"/>')
        for value, row in group:
            offset = (int(row["repeat"]) % 5 - 2) * 4
            bits.append(f'<circle cx="{x(value):.2f}" cy="{y + offset}" r="4" fill="{color}" stroke="var(--surface)" stroke-width="1.4"><title>Repeat {esc(row["repeat"])}: {esc(fmt_seconds(value))}</title></circle>')
        bits.append(f'<text x="755" y="{y + 5}" class="median-label">median {esc(fmt_seconds(median))} · n={len(values)}</text>')
    bits.append('</svg>')
    return "".join(bits)


def ab_paired_svg(manifest: dict[str, Any]) -> str:
    pairs: dict[str, list[tuple[int, float]]] = {}
    for suite in SUITES:
        text_by_repeat = {int(row["repeat"]): value for value, row in ab_values(manifest, suite, "ab-text")}
        pairs[suite] = [(int(row["repeat"]), (value / text_by_repeat[int(row["repeat"])] - 1) * 100)
                        for value, row in ab_values(manifest, suite, "ab-protobuf")]
    bound = max(5.0, max(abs(delta) for group in pairs.values() for _, delta in group) * 1.15)
    width, height, left, right = 1010, 240, 230, 790

    def x(value: float) -> float:
        return left + (right - left) * (value + bound) / (2 * bound)

    bits = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="Paired Protobuf versus text wall-time percentage difference for both suites" class="candle-chart">',
        '<title>Paired wall-time difference for each repeat</title>',
        '<desc>Each dot is Protobuf wall time minus text wall time in the same rotation pair, divided by text wall time. Left is faster for Protobuf; right is slower. The thick line marks the median paired difference.</desc>',
    ]
    for tick in (-bound, -bound / 2, 0, bound / 2, bound):
        tx = x(tick)
        bits.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="23" y2="215" class="grid-line"/>')
        bits.append(f'<text x="{tx:.2f}" y="17" text-anchor="middle" class="axis-text">{tick:+.1f}%</text>')
    for index, suite in enumerate(SUITES):
        group = pairs[suite]
        y = 82 + index * 82
        median = statistics.median(delta for _, delta in group)
        color = "#059669" if median < 0 else "#7c3aed"
        bits.append(f'<text x="12" y="{y + 5}" class="stage-text">{esc(SUITES[suite]["title"])}</text>')
        for repeat, delta in group:
            offset = (repeat % 5 - 2) * 7
            bits.append(f'<circle cx="{x(delta):.2f}" cy="{y + offset}" r="5" fill="{color}" stroke="var(--surface)" stroke-width="1.5"><title>Pair {repeat}: {delta:+.2f}%</title></circle>')
        bits.append(f'<line x1="{x(median):.2f}" x2="{x(median):.2f}" y1="{y - 26}" y2="{y + 26}" stroke="{color}" stroke-width="4"><title>Median paired change {median:+.2f}%</title></line>')
        bits.append(f'<text x="810" y="{y + 5}" class="median-label">median pair {median:+.1f}%</text>')
    bits.append('</svg>')
    return "".join(bits)


def ab_phase_html(manifest: dict[str, Any], suite: str) -> str:
    fields = (("native_ms", "Run Clang dumper"), ("read_ms", "Read AST file"),
              ("ast_ms", "Build Clava AST"), ("dump_bytes", "AST file size"))
    cards = []
    for field, title in fields:
        medians = {}
        for stage in AB_STAGES:
            values = [number(row.get("metrics", {}).get(field)) for _, row in ab_values(manifest, suite, stage)]
            if any(value is None for value in values):
                raise ValueError(f"A/B phase {field} is missing for {suite}/{stage}")
            medians[stage] = statistics.median(value for value in values if value is not None)
        maximum = max(medians.values(), default=1) or 1
        bars = []
        for stage, (label, color) in AB_STAGES.items():
            value = medians[stage]
            display = (f"{value / (1024 * 1024):.1f} MiB" if field == "dump_bytes" else
                       f"{value / 1000:.2f}s" if value >= 1000 else f"{value:.0f}ms")
            bars.append(f'<div class="phase-row"><span>{esc(label)}</span><div class="phase-track"><i style="width:{value / maximum * 100:.1f}%;background:{color}"></i></div><strong>{esc(display)}</strong></div>')
        cards.append(f'<div class="phase-card"><h4>{esc(title)}</h4>{"".join(bars)}</div>')
    return "".join(cards)


def load_gc_result(path: Path, ab_manifest: dict[str, Any]) -> dict[str, Any]:
    profile = json.loads(path.read_text(encoding="utf-8"))
    if profile.get("kind") != "same-revision-java-gc-diagnostic":
        raise ValueError(f"{path}: unrecognized Java GC diagnostic")
    for name, source in (("clava", "clava_revision"), ("native", "native_revision")):
        if profile.get(source) != ab_manifest.get("sources", {}).get(name, {}).get("revision"):
            raise ValueError(f"{path}: {name} revision differs from same-revision A/B")
    rows = profile.get("runs", [])
    if len(rows) != 4 or {(row.get("condition"), row.get("wire")) for row in rows} != {
            (condition, wire) for condition in ("normal", "explicit_gc_disabled")
            for wire in ("text", "protobuf")}:
        raise ValueError(f"{path}: expected one profile of each format in each JVM condition")
    for row in rows:
        for field in ("wall_s", "gc_pause_s", "system_gc_count", "system_gc_pause_s"):
            if number(row.get(field)) is None or number(row[field]) < 0:
                raise ValueError(f"{path}: invalid {field} in Java GC diagnostic")
    by_condition = {(row["condition"], row["wire"]): row for row in rows}
    normal_counts = {by_condition["normal", wire]["system_gc_count"] for wire in ("text", "protobuf")}
    if len(normal_counts) != 1 or next(iter(normal_counts)) <= 0 or any(
            by_condition["explicit_gc_disabled", wire]["system_gc_count"] != 0
            for wire in ("text", "protobuf")):
        raise ValueError(f"{path}: explicit-GC control does not match the report claim")
    memory = profile.get("original_ab_java_median_used_heap_mib_after_logging", {})
    if any(number(memory.get(wire)) is None for wire in ("text", "protobuf")):
        raise ValueError(f"{path}: missing Java used-heap medians")
    return profile


def gc_diagnostic_html(profile: dict[str, Any]) -> str:
    rows = {(row["condition"], row["wire"]): row for row in profile["runs"]}

    def bars(field: str, title: str, maximum: int, ticks: tuple[int, ...]) -> str:
        tracks = []
        for condition, prefix in (("normal", "Normal"), ("explicit_gc_disabled", "GC off")):
            for wire, color in (("text", AB_STAGES["ab-text"][1]),
                                ("protobuf", AB_STAGES["ab-protobuf"][1])):
                value = rows[condition, wire][field]
                tracks.append(
                    f'<div class="gc-row"><span>{esc(prefix)} · {esc(wire.title())}</span>'
                    f'<div class="gc-track"><i style="width:{value / maximum * 100:.2f}%;background:{color}"></i></div>'
                    f'<strong>{value:.2f}s</strong></div>')
        labels = "".join(f'<span>{tick}s</span>' for tick in ticks)
        return (f'<div class="gc-card"><h4>{esc(title)}</h4>{"".join(tracks)}'
                f'<div class="gc-axis"><span></span><div>{labels}</div><span></span></div></div>')

    normal_text, normal_proto = rows["normal", "text"], rows["normal", "protobuf"]
    off_text, off_proto = rows["explicit_gc_disabled", "text"], rows["explicit_gc_disabled", "protobuf"]
    used_heap = profile["original_ab_java_median_used_heap_mib_after_logging"]
    wall_gap = normal_proto["wall_s"] - normal_text["wall_s"]
    off_gap = off_proto["wall_s"] - off_text["wall_s"]
    forced_pause_gap = normal_proto["system_gc_pause_s"] - normal_text["system_gc_pause_s"]
    return f'''<section class="gc-section" aria-labelledby="gc-title">
      <p class="eyebrow">Why Java slowed down</p><h3 id="gc-title">Forced garbage collection dominates the Java gap</h3>
      <p>The Java parser suite logs used heap after each parse. That logging calls <code>System.gc()</code> twice. In the profiled pair, both formats triggered {normal_text['system_gc_count']} full collections. Protobuf spent {forced_pause_gap:.2f}s more paused for those collections, close to its {wall_gap:.2f}s longer whole-suite run.</p>
      <div class="gc-grid">{bars('wall_s', 'Whole Java test command', 50, (0, 25, 50))}{bars('gc_pause_s', 'Test-worker GC pauses', 15, (0, 5, 10, 15))}</div>
      <p class="takeaway">When the test worker ignored explicit GC requests, Protobuf was only {off_gap:.2f}s slower ({off_text['wall_s']:.2f}s vs {off_proto['wall_s']:.2f}s), and GC pauses fell below 0.4s in both modes. This identifies repeated full GC in the Java test path as the main source of its measured gap. It does not show a 4-second Protobuf reader penalty.</p>
      <p class="small">These are one exploratory, JFR-profiled pair per JVM condition, not new six-run medians. The original six-pair comparison above remains the measured result. Protobuf reported about {used_heap['protobuf']} MiB of used Java heap after logging versus {used_heap['text']} MiB for Text in the original runs; the profile does not isolate which live objects made each collection slower. Turning off explicit GC is a diagnostic control, not a claim that the Java test configuration should silently change.</p>
    </section>'''


def ab_section(manifest: dict[str, Any], gc_profile: dict[str, Any] | None = None) -> str:
    candles = []
    phase_cards = []
    for suite in SUITES:
        candles.append(f'<figure class="chart-card"><figcaption><h3>{esc(SUITES[suite]["title"])}</h3></figcaption>{ab_candle_svg(manifest, suite)}</figure>')
        phase_cards.append(f'<div class="phase-suite"><h3>{esc(SUITES[suite]["title"])}</h3><div class="phase-grid">{ab_phase_html(manifest, suite)}</div></div>')
    sources = manifest.get("sources", {})
    clava_revision = sources.get("clava", {}).get("revision", "not recorded")
    native_revision = sources.get("native", {}).get("revision", "not recorded")
    native_hash = sources.get("native", {}).get("tool_sha256", "not recorded")
    jar_hash = manifest.get("runtime_parser_jar_sha256", "not recorded")
    repeats = int(manifest["repeat_count"])
    js_text, js_proto, _ = ab_delta(manifest, "clava-js")
    java_text, java_proto, java_change = ab_delta(manifest, "java")
    java_read = {
        stage: statistics.median(row["metrics"]["read_ms"]
                                 for _, row in ab_values(manifest, "java", stage))
        for stage in AB_STAGES
    }
    java_read_change = (java_read["ab-protobuf"] - java_read["ab-text"]) / 1000
    java_read_direction = "lower" if java_read_change < 0 else "higher"
    java_pairs_slower = sum(
        proto > text for (proto, _), (text, _) in zip(
            ab_values(manifest, "java", "ab-protobuf"),
            ab_values(manifest, "java", "ab-text"))
    )
    gc_html = gc_diagnostic_html(gc_profile) if gc_profile is not None else ""
    phase_notice = ("The phase bars do not include Java's repeated full-GC pauses; the diagnostic below measures them separately."
                    if gc_profile is not None else "These counters do not yet tell us where Java spent the extra time.")
    return f'''<section aria-labelledby="ab-title" class="ab-section">
    <p class="eyebrow">Isolating the file format</p><h2 id="ab-title">Same code, two ways to send the AST to Clava</h2>
    <p>The earlier charts compare different code branches. Those branches changed more than the AST file format. Here we used one build of the Clang AST dumper and Clava, then switched only how the intermediate AST file was written and read:</p>
    <div class="ab-paths"><div><strong>Text mode</strong><span>The dumper writes a text AST file → Clava reads text</span></div><div><strong>Protobuf mode</strong><span>The dumper writes a Protobuf AST file → Clava reads Protobuf</span></div></div>
    <p class="small">For each mode, we ran the complete Clava-JS and Java test suites {repeats} times, alternating which mode went first. We turned off the AST cache, so every test run made new dumps. The candles measure the time for the entire test command, including test setup and work beyond AST parsing.</p>
    <p class="takeaway">Clava-JS took {fmt_seconds(js_text)} with Text and {fmt_seconds(js_proto)} with Protobuf, effectively a tie. Java took {fmt_seconds(java_text)} with Text and {fmt_seconds(java_proto)} with Protobuf. Protobuf was {java_change:.1f}% slower on Java and slower in {java_pairs_slower} of {repeats} runs. Lower is faster.</p>
    <div class="chart-grid">{"".join(candles)}</div>
    <figure class="chart-card ab-paired"><figcaption><h3>Did Protobuf win each run?</h3><p>Left of zero: faster · right of zero: slower</p></figcaption>{ab_paired_svg(manifest)}</figure>
    <p class="small">Each candle shows {repeats} full-suite runs. The dots in the last chart compare the Text and Protobuf runs with the same repeat number. Candle whiskers show the fastest and slowest runs; the box covers the middle half.</p>
    <h3 class="phase-title">Dumper time, reader time, and file size</h3>
    <p class="small">The bars below count work recorded while each suite ran. Several source files can be processed at once, so the time bars do not add up to the whole-suite times above.</p>
    {"".join(phase_cards)}
    <p class="notice">Smaller Protobuf files did not speed up Clava-JS on the same code. Java was slower overall, even though its measured AST file-reading work was {abs(java_read_change):.2f}s {java_read_direction} with Protobuf. {esc(phase_notice)}</p>
    <p class="small">Each bar is the median of work summed across parser calls, or the median total size of the uncompressed AST files. Each Text/Protobuf bar pair has its own scale. Protobuf decoding and node creation are already counted inside its reader time.</p>
    {gc_html}
    <details class="details-card"><summary>Exactly how we checked this</summary>
      <p>{repeats} measured repeats per format and suite, plus uncharted warm-ups. Both formats used the same source revisions, native executable, and Java runtime. We disabled ccache and compression. A C and a C++ example produced matching normalized ASTs in both formats before timing, and both formats passed suite smoke tests.</p>
      <p>Graph normalization excludes wire-local IDs, process context, edit-origin references, object identity, and the DataStore dispatch label. Concrete node classes, ordered children, source ranges, references, and populated semantic fields remain compared. The full measured suite tests passed in both modes.</p>
      <p>The Java suite's two-input PairHash fixture uses one parser worker in both modes; all other tests retain their configured concurrency.</p>
      {'<p>The Java GC diagnostic used the same Clava and native revisions. Java Flight Recorder captured one full-suite Text and one Protobuf run with normal JVM behavior, then one of each with <code>-XX:+DisableExplicitGC</code> on the test worker. <code>ParallelCodeParser</code> calls <code>SpecsSystem.getUsedMemory(true)</code> when reporting execution information; that method calls <code>System.gc()</code> twice. The diagnostic keeps JFR enabled in both JVM conditions.</p>' if gc_profile is not None else ''}
      {'<p>In a separate, unpaired Java diagnostic run, 108 of 116 individual test durations were higher with Protobuf; the largest single increase was 0.34s. This suggests many small contributions rather than one runaway test, but one extra run cannot establish their cause and is excluded from the six-pair estimate.</p>' if gc_profile is None else ''}
      <p class="small">Clava commit: <code>{esc(clava_revision)}</code><br>Native commit: <code>{esc(native_revision)}</code><br>Native binary SHA-256: <code>{esc(native_hash)}</code><br>Staged parser JAR SHA-256: <code>{esc(jar_hash)}</code></p>
      <p>{repeats} repeats on one workstation show workload-specific behavior, not universal format performance. The earlier branch comparison also includes changes beyond the transport path.</p>
    </details>
  </section>'''


def suite_domain(rows: list[dict[str, Any]], suite: str) -> tuple[float, float]:
    values = [
        value for row in rows
        if row.get("suite") == suite and row.get("stage") in STAGES
        and row.get("mode") in MODE_ORDER and is_measured(row) and is_valid_run(row, suite)
        if (value := time_value(row)) is not None
    ]
    if not values:
        return 0.01, 1.0
    # Keep all three cache-state panels on one useful scale without allowing an
    # isolated extreme run to flatten every quartile box. Values beyond the
    # padded Tukey fence remain in the data and are marked at the chart edge.
    q1 = quantile(values, 0.25)
    q3 = quantile(values, 0.75)
    iqr = q3 - q1
    low = min(values)
    raw_high = max(values)
    upper_fence = q3 + 1.5 * iqr
    # Zoom only when the largest observation is clearly separated from the
    # ordinary range; modest high runs remain inside the full padded scale.
    if raw_high > upper_fence * 1.10:
        high = max(value for value in values if value <= upper_fence)
    else:
        high = raw_high
    padding = max((high - low) * 0.12, high * 0.025, 0.25)
    return max(0.01, low - padding), high + padding


def chart_zoom_notes(rows: list[dict[str, Any]], domains: dict[str, tuple[float, float]]) -> str:
    notes = []
    for suite, (low, high) in domains.items():
        clipped = [
            row for row in rows
            if row.get("suite") == suite and row.get("stage") in STAGES
            and row.get("mode") in MODE_ORDER and is_measured(row) and is_valid_run(row, suite)
            and (value := time_value(row)) is not None and (value < low or value > high)
        ]
        if not clipped:
            continue
        labels = [
            f'{MODES[row["mode"]].split(" ")[0]} {stage_title(str(row["stage"]))} {time_value(row):.2f}s'
            for row in sorted(clipped, key=lambda item: time_value(item) or 0)
        ]
        notes.append(
            f'<p class="zoom-note">{esc(SUITES[suite]["title"])} charts share a zoomed scale from '
            f'{esc(axis_label(low))} to {esc(axis_label(high))}. Edge triangles mark clipped values: '
            f'{esc("; ".join(labels))}. Full measurements remain available in each marker tooltip.</p>'
        )
    return "".join(notes)


def axis_label(value: float) -> str:
    return f"{value:.1f}s"


def chart_svg(
    suite: str,
    mode: str,
    rows: list[dict[str, Any]],
    provenance: dict[str, dict[str, Any]],
    domain: tuple[float, float],
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
    domain_low, domain_high = domain

    def x(value: float) -> float:
        return left + (right - left) * (value - domain_low) / (domain_high - domain_low)

    def clipped_x(value: float) -> float:
        return min(right, max(left, x(value)))

    bits = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(SUITES[suite]["title"])} wall time distribution in {esc(MODES[mode])}" class="candle-chart">',
        f'<title>{esc(SUITES[suite]["title"])} wall time, {esc(MODES[mode])}</title>',
        '<desc>Each row is a tested repository state. Whiskers show the minimum and maximum, the box shows the first and third quartiles, the thick mark is the median, and dots are individual valid runs. Lower time is better.</desc>',
    ]
    for tick, label in time_axis_ticks(domain_low, domain_high):
        tx = x(tick)
        bits.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="22" y2="{height - 22}" class="grid-line"/>')
        bits.append(f'<text x="{tx:.2f}" y="17" text-anchor="middle" class="axis-text">{esc(label)}</text>')

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
        xlow, xhigh = clipped_x(low), clipped_x(high)
        xq1, xq3, xmed = (clipped_x(v) for v in (q1, q3, median))
        box_width = max(3, xq3 - xq1)
        bits.append(f'<line x1="{xlow:.2f}" x2="{xhigh:.2f}" y1="{y:.2f}" y2="{y:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<line x1="{xlow:.2f}" x2="{xlow:.2f}" y1="{y - 8:.2f}" y2="{y + 8:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(f'<line x1="{xhigh:.2f}" x2="{xhigh:.2f}" y1="{y - 8:.2f}" y2="{y + 8:.2f}" stroke="{color}" stroke-width="2"/>')
        bits.append(
            f'<rect x="{xq1:.2f}" y="{y - 14:.2f}" width="{box_width:.2f}" height="28" rx="4" fill="{color}" fill-opacity=".22" stroke="{color}" stroke-width="1.6">'
            f'<title>Q1 {esc(fmt_seconds(q1))} to Q3 {esc(fmt_seconds(q3))}</title></rect>'
        )
        bits.append(f'<line x1="{xmed:.2f}" x2="{xmed:.2f}" y1="{y - 15:.2f}" y2="{y + 15:.2f}" stroke="{color}" stroke-width="4"/>')
        for run_index, (value, row) in enumerate(points[key]):
            jitter = ((run_index % 5) - 2) * 4
            repeat = row.get("repeat")
            title = (
                f'{esc(label)} · repeat {esc(repeat if repeat is not None else run_index + 1)}: '
                f'{esc(fmt_seconds(value))}, valid run{", from Direct mode" if is_reference else ""}'
            )
            if value < domain_low or value > domain_high:
                edge_x = clipped_x(value)
                direction = "left" if value < domain_low else "right"
                points_to = (
                    f'M {edge_x + 7:.2f} {y + jitter - 5:.2f} L {edge_x - 1:.2f} {y + jitter:.2f} L {edge_x + 7:.2f} {y + jitter + 5:.2f} Z'
                    if direction == "left" else
                    f'M {edge_x - 7:.2f} {y + jitter - 5:.2f} L {edge_x + 1:.2f} {y + jitter:.2f} L {edge_x - 7:.2f} {y + jitter + 5:.2f} Z'
                )
                bits.append(f'<path d="{points_to}" fill="{color}" stroke="var(--surface)" stroke-width="1.4"><title>{title}; clipped at {direction} axis edge</title></path>')
            else:
                bits.append(
                    f'<circle cx="{clipped_x(value):.2f}" cy="{y + jitter:.2f}" r="4" fill="{color}" stroke="var(--surface)" stroke-width="1.4">'
                    f'<title>{title}</title></circle>'
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


def median_trend_svg(suite: str, rows: list[dict[str, Any]], domain: tuple[float, float]) -> str:
    width, height = 520, 285
    left, right, top, bottom = 62, 470, 35, 232
    domain_low, domain_high = domain
    x_positions = [left + (right - left) * index / 2 for index in range(3)]

    def y(value: float) -> float:
        bounded = min(domain_high, max(domain_low, value))
        return bottom - (bottom - top) * (bounded - domain_low) / (domain_high - domain_low)

    bits = [
        f'<svg viewBox="0 0 {width} {height}" role="img" aria-label="{esc(SUITES[suite]["title"])} median wall time across Direct, Cold, and Warm" class="trend-chart">',
        f'<title>{esc(SUITES[suite]["title"])} median trend</title>',
        '<desc>Three colored lines compare text with ccache, protobuf, and eager FlatBuffers. A dashed gray horizontal line repeats the Direct pre-cache median as a reference with no cache state.</desc>',
    ]
    for tick, label in time_axis_ticks(domain_low, domain_high):
        ty = y(tick)
        bits.append(f'<line x1="{left}" x2="{right}" y1="{ty:.2f}" y2="{ty:.2f}" class="grid-line"/>')
        bits.append(f'<text x="{left - 8}" y="{ty + 4:.2f}" text-anchor="end" class="axis-text">{esc(label)}</text>')
    for index, mode in enumerate(MODE_ORDER):
        tx = x_positions[index]
        bits.append(f'<line x1="{tx:.2f}" x2="{tx:.2f}" y1="{top}" y2="{bottom}" class="grid-line"/>')
        bits.append(f'<text x="{tx:.2f}" y="{bottom + 22}" text-anchor="middle" class="axis-text">{esc(MODES[mode].replace(" (no ccache)", ""))}</text>')

    baseline = median_for(rows, suite, "direct", "before-cache")
    if baseline is not None:
        baseline_y = y(baseline)
        bits.append(f'<line x1="{left}" x2="{right}" y1="{baseline_y:.2f}" y2="{baseline_y:.2f}" stroke="#64748b" stroke-width="2" stroke-dasharray="6 5"/>')
        bits.append(f'<text x="{right - 2}" y="{baseline_y - 5:.2f}" text-anchor="end" class="reference-label">Pre-cache Direct reference {esc(fmt_seconds(baseline))}</text>')

    for stage in ("ccache-text", "protobuf", "flatbuffers"):
        color = STAGES[stage][1]
        values = [median_for(rows, suite, mode, stage) for mode in MODE_ORDER]
        existing = [(x_positions[index], value, MODE_ORDER[index]) for index, value in enumerate(values) if value is not None]
        if len(existing) > 1:
            path = " ".join(("M" if index == 0 else "L") + f" {x_pos:.2f} {y(value):.2f}" for index, (x_pos, value, _) in enumerate(existing))
            bits.append(f'<path d="{path}" fill="none" stroke="{color}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')
        for x_pos, value, mode in existing:
            cy = y(value)
            bits.append(
                f'<circle cx="{x_pos:.2f}" cy="{cy:.2f}" r="5" fill="{color}" stroke="var(--surface)" stroke-width="2">'
                f'<title>{esc(stage_title(stage))}, {esc(MODES[mode])}: median {esc(fmt_seconds(value))}</title></circle>'
            )
    bits.append(f'<text x="14" y="{(top + bottom) / 2:.2f}" transform="rotate(-90 14 {(top + bottom) / 2:.2f})" class="axis-text">Median elapsed time</text>')
    bits.append('</svg>')
    return "".join(bits)


def pct_change(value: float, reference: float) -> str:
    delta = (value / reference - 1) * 100
    if abs(delta) < 0.05:
        return "about the same median"
    direction = "faster" if delta < 0 else "slower"
    return f"{abs(delta):.1f}% {direction} by median"


def median_deltas(rows: list[dict[str, Any]], suite: str, branch: str, reference: str) -> list[float]:
    deltas: list[float] = []
    for mode in MODE_ORDER:
        value = median_for(rows, suite, mode, branch)
        baseline = median_for(rows, suite, mode, reference)
        if value is not None and baseline not in (None, 0):
            deltas.append((value / baseline - 1) * 100)
    return deltas


def delta_range(values: list[float]) -> str:
    if not values:
        return "could not be calculated from these inputs"
    low, high = min(values), max(values)
    if high <= 0:
        smallest, largest = abs(high), abs(low)
        amount = f"{smallest:.1f}%" if math.isclose(smallest, largest, abs_tol=0.05) else f"{smallest:.1f}% to {largest:.1f}%"
        return f"{amount} lower"
    if low >= 0:
        amount = f"{low:.1f}%" if math.isclose(low, high, abs_tol=0.05) else f"{low:.1f}% to {high:.1f}%"
        return f"{amount} higher"
    return f"{abs(low):.1f}% lower to {high:.1f}% higher"


def seconds_delta_range(values: list[float]) -> str:
    if not values:
        return "could not be calculated"
    low, high = min(values), max(values)
    if low >= 0:
        return f"{fmt_seconds(low)} to {fmt_seconds(high)} slower"
    if high <= 0:
        return f"{fmt_seconds(abs(high))} to {fmt_seconds(abs(low))} faster"
    return f"{fmt_seconds(abs(low))} faster to {fmt_seconds(high)} slower"


def concise_takeaway(rows: list[dict[str, Any]]) -> str:
    js_proto = median_deltas(rows, "clava-js", "protobuf", "ccache-text")
    js_flat = median_deltas(rows, "clava-js", "flatbuffers", "ccache-text")
    java_proto = median_deltas(rows, "java", "protobuf", "ccache-text")
    java_flat = median_deltas(rows, "java", "flatbuffers", "ccache-text")
    savings = []
    cold_direct = []
    for stage in ("ccache-text", "protobuf", "flatbuffers"):
        direct = median_for(rows, "java", "direct", stage)
        cold = median_for(rows, "java", "cold", stage)
        warm = median_for(rows, "java", "warm", stage)
        if cold not in (None, 0) and warm is not None:
            savings.append((cold - warm) / cold * 100)
        if direct is not None and cold is not None:
            cold_direct.append(cold - direct)
    warm_saving = f"{statistics.median(savings):.0f}%" if savings else "not available"
    flat_java_distance = max((abs(value) for value in java_flat), default=0)
    cold_direct_change = seconds_delta_range(cold_direct)
    return (
        f"Clava-JS medians were {delta_range(js_proto)} for Protobuf and {delta_range(js_flat)} for FlatBuffers "
        f"versus the text branch. Java Protobuf was {delta_range(java_proto)}; FlatBuffers stayed within "
        f"{flat_java_distance:.1f}% of text. On Java, Cold was {cold_direct_change} than Direct, then Warm "
        f"cut median time by about {warm_saving} versus Cold. These are branch outcomes, not a format-only comparison."
    )


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
    cache_stages = {key for key in STAGE_ORDER if any(
        bool(meta.get("cache")) for meta in provenance.get(key, {}).values() if isinstance(meta, dict)
    )}
    if not cache_stages:
        cache_stages = {"ccache-text", "protobuf", "flatbuffers"}

    cards: list[str] = []
    for mode in MODE_ORDER:
        measured = [
            row for row in rows if row.get("mode") == mode and row.get("stage") in cache_stages
            and row.get("suite") in SUITES and is_measured(row) and is_valid_run(row, row.get("suite", ""))
        ]
        counters = [cache_counters(row) for row in measured]
        has_counters = bool(counters) and all(bool(item) for item in counters)
        hits = sum(item.get("direct_hits", 0) + item.get("preprocessed_hits", 0) + item.get("hits", 0) for item in counters)
        misses = sum(item.get("misses", 0) for item in counters)
        validations = [row.get("cache_validation") for row in measured if isinstance(row.get("cache_validation"), dict)]
        checks_passed = sum(item.get("passed") is True for item in validations)
        if mode == "direct":
            expected = "No ccache calls, hits, or misses."
        elif mode == "cold":
            expected = "Fresh-cache misses are required; hits within a run are allowed."
        else:
            expected = "Every measured run must restore at least one cached dump."
        if has_counters:
            detail = f"Checks passed {checks_passed}/{len(validations)} runs. {hits:,.0f} hits and {misses:,.0f} misses across all cached branches."
        else:
            detail = "Per-run cache counters are missing from the input."
        cards.append(
            f'<article class="cache-card"><p class="eyebrow">{esc(MODES[mode])}</p>'
            f'<p>{esc(expected)} {esc(detail)}</p></article>'
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


def worktree_label(status: Any) -> str:
    if not isinstance(status, list):
        return "not recorded"
    if not status:
        return "clean"
    if any(str(line).strip().endswith("clang-dumper-release.tag") for line in status):
        return "release selector modified"
    return f"{len(status)} local change(s)"


def provenance_worktree_note(provenance: dict[str, dict[str, Any]]) -> str:
    states = {key: next(iter(items.values()), {}) for key, items in provenance.items()}
    notes = []
    for key, label in (("ccache-text", "Text"), ("protobuf", "Protobuf"), ("flatbuffers", "FlatBuffers")):
        meta = states.get(key, {})
        notes.append(f"{label}: {worktree_label(meta.get('clava_status') if isinstance(meta, dict) else None)}")
    native_states = [
        worktree_label(meta.get("dumper_status"))
        for key, meta in states.items()
        if key in {"ccache-text", "protobuf", "flatbuffers"} and isinstance(meta, dict)
    ]
    if native_states and all(state == "clean" for state in native_states):
        notes.append("native dumper worktrees: clean")
    if any("release selector modified" in note for note in notes):
        notes.append("the selector points to each branch's measured executable")
    return "; ".join(notes)


def provenance_table(provenance: dict[str, dict[str, Any]], rows: list[dict[str, Any]]) -> str:
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
            jar_hashes = sorted({
                str(row.get("runtime_parser_jar_sha256")) for row in rows
                if row.get("stage") == stage and row.get("mode") == mode
                and row.get("runtime_parser_jar_sha256")
            })
            jar_hash = jar_hashes[0] if len(jar_hashes) == 1 else (f"varies across {len(jar_hashes)} hashes" if jar_hashes else "not recorded")
            dirty = worktree_label(meta.get("clava_status"))
            dumper_dirty = "not applicable" if meta.get("native_root") is None else worktree_label(meta.get("dumper_status"))
            state = MODES.get(mode, "all inputs")
            cells = [stage_title(stage, meta), state, clava, dumper_rev, dumper_hash, runtime_hash, jar_hash, dirty, dumper_dirty]
            table_rows.append("<tr>" + "".join(f'<td>{esc(cell)}</td>' for cell in cells) + "</tr>")
    header = "".join(f"<th>{esc(item)}</th>" for item in ("Tested state", "Cache state", "Clava commit", "Native dumper commit", "Dumper executable SHA-256", "Source Java runtime SHA-256", "Staged parser JAR SHA-256", "Clava worktree", "Dumper worktree"))
    return f'<div class="table-scroll"><table><thead><tr>{header}</tr></thead><tbody>{"".join(table_rows)}</tbody></table></div>'


def invocation_summary(rows: list[dict[str, Any]]) -> tuple[int, int, int, int]:
    invocations = [row for row in rows if row.get("suite") in SUITES]
    valid = [row for row in invocations if is_valid_run(row, row.get("suite", ""))]
    measured = [row for row in invocations if is_measured(row)]
    return len(invocations), len(valid), len(measured), len(invocations) - len(measured)


def report_html(
    manifests: list[dict[str, Any]],
    provenance: dict[str, dict[str, Any]],
    provenance_warnings: list[str] | None = None,
    smoke_note: str | None = None,
    ab_manifest: dict[str, Any] | None = None,
    gc_profile: dict[str, Any] | None = None,
) -> str:
    rows = flatten_results(manifests)
    modes_present = sorted({str(row.get("mode")) for row in rows if row.get("mode") in MODE_ORDER}, key=MODE_ORDER.index)
    total_runs, valid_runs, measured_count, warmup_count = invocation_summary(rows)
    invalid_count = total_runs - valid_runs
    domains = {suite: suite_domain(rows, suite) for suite in SUITES}
    dates = sorted(str(manifest.get("created_at")) for manifest in manifests if manifest.get("created_at"))
    if ab_manifest is not None and ab_manifest.get("created_at"):
        dates.append(str(ab_manifest["created_at"]))
        dates.sort()
    date_text = f"Input runs created {dates[0]}" if dates else "Creation time not recorded in the input manifests"
    if len(dates) > 1:
        date_text += f" through {dates[-1]}"

    mode_sections: list[str] = []
    for mode in MODE_ORDER:
        charts = []
        for suite, spec in SUITES.items():
            svg = chart_svg(suite, mode, rows, provenance, domains[suite])
            charts.append(
                f'<figure class="chart-card"><figcaption><h3>{esc(spec["title"])}</h3>'
                f'<p>{esc(spec["description"])}</p></figcaption>{svg}'
                '<p class="chart-key">Whiskers: min/max · box: Q1 to Q3 · center mark: median · dots: individual valid runs. Lower is faster.</p></figure>'
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
    trend_charts = "".join(
        f'<figure class="trend-card"><figcaption><h3>{esc(spec["title"])}</h3></figcaption>'
        f'{median_trend_svg(suite, rows, domains[suite])}</figure>'
        for suite, spec in SUITES.items()
    )
    validity = f"{valid_runs:,}/{total_runs:,} valid invocations · {measured_count:,} measured · {warmup_count:,} warm-ups · {invalid_count:,} invalid"
    worktree_notes = provenance_worktree_note(provenance)
    controlled_ab = ab_section(ab_manifest, gc_profile) if ab_manifest is not None else ""
    comparison_guide = (
        '<p class="small">The first charts compare different code branches and cache states. '
        'The next section runs Text and Protobuf through the same build to check what the file format itself changes.</p>'
        if ab_manifest is not None else ""
    )

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
    .mobile-hint {{ display:none; color:var(--muted); font-size:.88rem; }}
    .eyebrow {{ margin-bottom:4px; color:var(--muted); font-size:.75rem; font-weight:750; text-transform:uppercase; letter-spacing:.1em; }}
    .hero,.takeaway-card,.chart-card,.trend-card,.cache-card,.branch-card,.details-card {{ background:var(--surface); border:1px solid var(--line); border-radius:18px; box-shadow:var(--shadow); }}
    .hero {{ padding:28px clamp(18px,4vw,42px); margin:24px 0 18px; }}
    .hero-stat {{ display:flex; flex-wrap:wrap; gap:8px 22px; margin-top:16px; padding-top:14px; border-top:1px solid var(--line); color:var(--muted); font-size:.9rem; }}
    .hero-stat strong {{ color:var(--ink); }}
    .notice {{ margin:16px 0; padding:14px 18px; border-radius:12px; border:1px solid var(--line); background:var(--notice); }}
    .notice.warning {{ background:var(--warn); }} .notice p:last-child {{ margin-bottom:0; }} .notice ul {{ margin:.5rem 0; }}
    .notice.smoke {{ border:2px solid #d97706; background:var(--warn); }}
    .takeaway {{ margin:16px 0 10px; padding:16px 18px; border-left:4px solid #2563eb; border-radius:8px; background:var(--surface); font-size:1.02rem; }}
    .validity {{ margin:12px 0 0; color:var(--muted); font-size:.88rem; font-weight:650; }}
    .section-heading {{ display:flex; align-items:end; justify-content:space-between; gap:18px; margin:42px 0 15px; }}
    .section-heading>p {{ max-width:490px; margin:0; color:var(--muted); text-align:right; }}
    .chart-grid {{ display:grid; grid-template-columns:1fr; gap:16px; }}
    .trend-grid {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; margin:14px 0 34px; }}
    .trend-card {{ min-width:0; margin:0; padding:14px; overflow-x:auto; }}
    .trend-card figcaption {{ margin:0 4px; }}
    .trend-chart {{ display:block; width:100%; height:auto; color:var(--ink); }}
    .reference-label {{ fill:var(--muted); font-size:11px; font-weight:650; }}
    .chart-card {{ min-width:0; margin:0; padding:18px 18px 12px; }}
    .chart-card figcaption {{ display:flex; align-items:baseline; justify-content:space-between; gap:16px; margin:0 4px; }}
    .chart-card figcaption p {{ color:var(--muted); font-size:.85rem; text-align:right; }}
    .candle-chart {{ display:block; width:100%; height:auto; overflow:visible; color:var(--ink); }}
    .grid-line {{ stroke:var(--grid); stroke-width:1; }} .axis-text {{ fill:var(--muted); font-size:12px; }}
    .stage-text {{ fill:var(--ink); font-size:14px; font-weight:700; }} .empty-lane {{ fill:var(--muted); font-size:13px; }}
    .median-label {{ fill:var(--ink); font-size:13px; font-weight:650; }}
    .chart-key {{ margin:8px 4px 0; color:var(--muted); font-size:.8rem; }}
    .zoom-note {{ margin:12px 0; padding:10px 13px; border-left:3px solid #d97706; border-radius:6px; background:var(--warn); color:var(--ink); font-size:.88rem; }}
    .empty-chart {{ padding:35px 16px; color:var(--muted); text-align:center; border:1px dashed var(--line); border-radius:10px; }}
    .mode-section {{ margin-bottom:44px; }}
    .branch-card {{ padding:20px; margin:12px 0 28px; }}
    .branch-chart {{ display:block; width:100%; height:auto; color:var(--muted); }}
    .branch-boxes rect {{ fill:var(--surface); stroke:var(--line); stroke-width:2; }}
    .branch-labels text {{ fill:var(--ink); font-size:17px; font-weight:700; }}
    .branch-labels .sub-label {{ fill:var(--muted); font-size:12px; font-weight:500; }}
    .cache-grid {{ display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; margin:14px 0 28px; }}
    .cache-card {{ padding:16px; }} .cache-card p:last-child {{ margin-bottom:0; }}
    .ab-section {{ margin:44px 0; }} .ab-section>.chart-grid {{ margin:16px 0; }}
    .ab-paths {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; margin:14px 0; }}
    .ab-paths>div {{ display:flex; flex-wrap:wrap; gap:6px 16px; padding:14px 16px; border:1px solid var(--line); border-radius:12px; background:var(--surface); }}
    .ab-paths strong {{ color:var(--ink); }} .ab-paths span {{ color:var(--muted); }}
    .ab-paired {{ margin:16px 0; }} .phase-title {{ margin:22px 0 12px; }}
    .phase-suite {{ margin:14px 0; }} .phase-suite>h3 {{ margin:0 0 10px; }}
    .phase-grid {{ display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:12px; }}
    .phase-card {{ min-width:0; padding:16px; border:1px solid var(--line); border-radius:14px; background:var(--surface); }}
    .phase-card h4 {{ margin:0 0 12px; font-size:.95rem; }}
    .phase-row {{ display:grid; grid-template-columns:66px minmax(30px,1fr) 60px; gap:7px; align-items:center; margin:8px 0; font-size:.78rem; }}
    .phase-row strong {{ text-align:right; font-size:.78rem; }}
    .phase-track {{ height:12px; border-radius:7px; background:var(--grid); overflow:hidden; }}
    .phase-track i {{ display:block; min-width:2px; height:100%; border-radius:7px; }}
    .gc-section {{ margin:26px 0; }} .gc-section>h3 {{ font-size:1.45rem; }}
    .gc-grid {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; margin:16px 0; }}
    .gc-card {{ min-width:0; padding:16px; border:1px solid var(--line); border-radius:14px; background:var(--surface); }}
    .gc-card h4 {{ margin:0 0 14px; font-size:1rem; }}
    .gc-row,.gc-axis {{ display:grid; grid-template-columns:132px minmax(0,1fr) 48px; gap:8px; align-items:center; font-size:.79rem; }}
    .gc-row {{ margin:8px 0; }} .gc-row strong {{ text-align:right; }}
    .gc-track {{ height:14px; background:var(--grid); border-radius:7px; overflow:hidden; }}
    .gc-track i {{ display:block; height:100%; border-radius:7px; }}
    .gc-axis>div {{ display:flex; justify-content:space-between; color:var(--muted); font-size:.72rem; }}
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
    @media(max-width:1000px) {{ .phase-grid {{ grid-template-columns:repeat(2,minmax(0,1fr)); }} }}
    @media(max-width:800px) {{ .trend-grid,.cache-grid,.ab-paths,.gc-grid {{ grid-template-columns:1fr; }} .section-heading {{ display:block; }} .section-heading>p {{ text-align:left; margin-top:8px; }} }}
    @media(max-width:650px) {{ .phase-grid {{ grid-template-columns:1fr; }} }}
    @media(max-width:650px) {{ main {{ width:min(100% - 20px,1180px); margin-top:18px; }} .hero {{ padding:22px 18px; }} .mobile-hint {{ display:block; }} .chart-card {{ padding:14px 8px 10px; }} .chart-card figcaption {{ display:block; }} .chart-card figcaption p {{ text-align:left; margin-bottom:0; }} .candle-chart {{ min-width:760px; }} .chart-card,.trend-card {{ overflow-x:auto; }} .trend-chart {{ min-width:480px; }} .branch-chart {{ min-width:700px; }} .branch-card {{ overflow-x:auto; }} }}
  </style>
</head>
<body>
<main>
  <header>
    <p class="eyebrow">Clava · performance comparison</p>
    <h1>AST transport performance</h1>
    <p class="lede">An abstract syntax tree (AST) is Clang's structured view of source code. Clava uses it to analyze and transform C/C++ programs; this report compares how the AST reaches Clava.</p>
    {comparison_guide}
    <p class="meta-line">{esc(date_text)}</p>
    <p class="mobile-hint">Swipe charts sideways to see the full scale.</p>
  </header>
  {smoke_box}
  {coverage_note}
  {warning_box}
  <section aria-labelledby="trend-title">
    <p class="eyebrow">At a glance</p><h2 id="trend-title">Median wall time by suite and cache state</h2>
    <p class="takeaway">{esc(concise_takeaway(rows))}</p>
    {'<p class="notice">The matched-format Java slowdown is largely from full garbage collections forced by memory logging in that test path. With those requests ignored, Text and Protobuf nearly tied in a diagnostic pair. See the Java GC chart below.</p>' if gc_profile is not None else ''}
    <p class="validity">{esc(validity)}</p>
    <div class="trend-grid">{trend_charts}</div>
    <p class="small">Lower is faster. Colored lines show text + ccache, protobuf, and eager FlatBuffers. The dashed line repeats the Direct pre-cache median as a reference with no cache state.</p>
  </section>
  {controlled_ab}
  <section aria-labelledby="charts-title">
    <p class="eyebrow">Run distributions</p><h2 id="charts-title">Six candle charts show the spread</h2>
    <p class="small">Every cache-state panel uses the same padded, nonzero time scale within its suite. The Direct pre-cache candle is repeated in Cold and Warm as a reference, not as a measurement in those states. One dot is one valid measured repeat.</p>
    {chart_zoom_notes(rows, domains)}
    {''.join(mode_sections)}
  </section>
  <section aria-labelledby="topology-title">
    <p class="eyebrow">Branch layout</p><h2 id="topology-title">Protobuf and FlatBuffers are sibling branches</h2>
    <div class="branch-card">{branch_svg()}<p class="small">The protobuf and eager FlatBuffers checkouts branch from the same cache-integrated text state. Before-cache streams text from stdout into the parser. Direct post-cache Text and Protobuf use uncompressed files. Cold and Warm Text and Protobuf use Zstd-compressed files. FlatBuffers uses a raw binary file in each mode. The pre-cache row is context, not an isolated cache comparison, because it uses a different checkout and stdout transport.</p></div>
  </section>
  <section aria-labelledby="cache-check-title">
    <p class="eyebrow">Cache checks</p><h2 id="cache-check-title">Counters confirm the requested state</h2>
    <div class="cache-grid">{cache_summary(rows, provenance)}</div>
    <p class="small">Direct uses a ccache wrapper probe. Cold requires fresh-cache misses, while allowing hits within a run. Warm requires a cached dump hit on every measured invocation.</p>
  </section>
  <section aria-labelledby="legend-title">
    <p class="eyebrow">Reading the candles</p><h2 id="legend-title">Each candle summarizes six runs</h2>
    <div class="branch-card"><div class="legend">{''.join(f'<span><i class="swatch" style="background:{STAGES[key][1]}"></i>{esc(STAGES[key][0])}</span>' for key in STAGE_ORDER)}</div><p class="small" style="margin-top:12px">Whiskers mark minimum and maximum. The colored box spans the first to third quartile. The bold mark is the median. Each dot is one valid measured repeat. Six repeats describe the observed spread; they do not establish statistical significance.</p></div>
  </section>
  <details class="details-card">
    <summary>Methods and workload</summary>
    <p><strong>Clava-JS.</strong> {esc(SUITES['clava-js']['description'])} A run is eligible only with 158 passes, six skips, zero failures, and a successful process exit.</p>
    <p><strong>Java parser.</strong> {esc(SUITES['java']['description'])} A run is eligible only with 116 passes, no skips or failures, and a successful process exit.</p>
    <p><strong>Cache modes.</strong> Direct disables ccache. Cold clears the stage-owned cache before each measured run. Warm reuses the stage-owned cache. One uncharted resource warm-up precedes each stage and suite.</p>
    <p><strong>Metric.</strong> Candles use elapsed wall time recorded by <code>/usr/bin/time</code>. The renderer excludes a run if the manifest has no wall-time field.</p>
    <p><strong>Workstation.</strong> One Linux machine with an Intel Core i7-9700 (8 cores), OpenJDK 26, Node 26, and ccache 4.12.3 ran the suites.</p>
    <p><strong>Limits.</strong> These two workloads describe the tested branches on one workstation. They do not establish behavior on other projects, platforms, or compiler versions.</p>
  </details>
  <details class="details-card">
    <summary>Exact revisions and artifact fingerprints</summary>
    <p class="small">Absolute local paths are omitted. Full commits and hashes identify the sources and artifacts used for each cache state. {esc(worktree_notes)}.</p>
    {provenance_table(provenance, rows)}
  </details>
  <p class="footer-note">Generated from the supplied protocol-comparison JSON manifests. Invalid runs, warm-ups, and runs without a valid wall-time metric are excluded from candles and medians.</p>
</main>
</body>
</html>'''


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, action="append", required=True, help="results.json manifest; repeat once per cache state")
    parser.add_argument("--output", type=Path, required=True, help="HTML output path, or '-' for stdout")
    parser.add_argument("--ab-results", type=Path, help="passed same-revision text/Protobuf A/B results.json")
    parser.add_argument("--java-gc-profile", type=Path, help="same-revision Java JVM/GC diagnostic summary.json; requires --ab-results")
    args = parser.parse_args()
    try:
        manifests, provenance, warnings = load_inputs(args.input)
        ab_manifest = load_ab_result(args.ab_results) if args.ab_results else None
        if args.java_gc_profile and ab_manifest is None:
            raise ValueError("--java-gc-profile requires --ab-results")
        gc_profile = load_gc_result(args.java_gc_profile, ab_manifest) if args.java_gc_profile else None
        rendered = report_html(manifests, provenance, warnings, ab_manifest=ab_manifest,
                               gc_profile=gc_profile)
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
