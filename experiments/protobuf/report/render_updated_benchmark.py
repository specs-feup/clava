#!/usr/bin/env python3
"""Render updated Protobuf measurements with the frozen comparison controls."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import html
import importlib.util
import json
import math
from pathlib import Path
import re
import statistics
import sys
from typing import Any


SUITES = ("clava-js", "java")
MODES = ("direct", "cold", "warm")
REPEATS = (1, 2, 3, 4)
STAGES = ("before-cache", "ccache-text", "protobuf", "flatbuffers")
STAGE_LABELS = {
    "before-cache": "Before cache",
    "ccache-text": "Text + ccache",
    "protobuf": "Protobuf",
    "flatbuffers": "FlatBuffers",
}
MODE_LABELS = {"direct": "Direct", "cold": "Cold cache", "warm": "Warm cache"}
SUITE_LABELS = {"clava-js": "Clava-JS", "java": "Java parser"}
EXPECTED_OBSERVATIONS = len(SUITES) * len(MODES) * len(REPEATS) * 2


def _load_module(name: str, path: Path) -> Any:
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"could not load chart module {path.name}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


_REPORT_DIR = Path(__file__).resolve().parents[2] / "protocol-comparison" / "report"
_REFRESH_DIR = Path(__file__).resolve().parents[1] / "suite" / "refresh"
_REFRESH_SUPPORT = _load_module("_protobuf_report_refresh_support", _REFRESH_DIR / "support.py")
_APP_CHARTS = _load_module("_protobuf_report_app_charts", _REPORT_DIR / "render_app_build.py")
if str(_REPORT_DIR) not in sys.path:
    sys.path.insert(0, str(_REPORT_DIR))
_REPORT_CHARTS = _load_module("render_report", _REPORT_DIR / "render_report.py")
_WALL_CHARTS = _load_module("render_decision", _REPORT_DIR / "render_decision.py")


def _fail(message: str) -> None:
    raise ValueError(message)


def _finite_nonnegative(value: Any, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        _fail(f"{label} must be a number")
    parsed = float(value)
    if not math.isfinite(parsed) or parsed < 0:
        _fail(f"{label} must be finite and non-negative")
    return parsed


def _nonnegative_integer(value: Any, label: str) -> int:
    if type(value) is not int or value < 0:
        _fail(f"{label} must be a non-negative integer")
    return value


def _public_date(value: Any, label: str) -> str:
    if not isinstance(value, str):
        _fail(f"{label} must be a calendar date")
    try:
        parsed = dt.date.fromisoformat(value)
    except ValueError:
        _fail(f"{label} must use YYYY-MM-DD")
    if parsed.isoformat() != value:
        _fail(f"{label} must use YYYY-MM-DD")
    return value


def _utc(value: Any, label: str) -> dt.datetime:
    if not isinstance(value, str) or not value.strip():
        _fail(f"{label} must be a UTC timestamp")
    try:
        parsed = dt.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        _fail(f"{label} must be an ISO-8601 UTC timestamp")
    if parsed.tzinfo is None or parsed.utcoffset() != dt.timedelta(0):
        _fail(f"{label} must include the UTC offset")
    return parsed.astimezone(dt.timezone.utc)


def format_utc(value: dt.datetime) -> str:
    precision = "microseconds" if value.microsecond else "seconds"
    return value.isoformat(sep=" ", timespec=precision).replace("+00:00", " UTC")


def validate_updated_manifest(payload: Any) -> dict[str, Any]:
    """Validate all 48 App and wall observations from one completed session."""
    if not isinstance(payload, dict) or payload.get("status") != "complete":
        _fail("updated results must have status 'complete'")
    created = _utc(payload.get("created_utc"), "created_utc")
    completed = _utc(payload.get("completed_utc"), "completed_utc")
    if completed < created:
        _fail("completed_utc precedes created_utc")
    js_defaults = payload.get("js_vitest_defaults")
    if not isinstance(js_defaults, dict) or js_defaults.get("config_loader") != "runner":
        _fail("updated results need the pinned Vitest runner config loader")

    observations = payload.get("observations")
    if not isinstance(observations, list) or len(observations) != EXPECTED_OBSERVATIONS:
        _fail(f"expected exactly {EXPECTED_OBSERVATIONS} observations")

    app_rows: list[dict[str, Any]] = []
    wall_rows: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str, int]] = set()
    app_calls_by_suite: dict[str, set[int]] = {suite: set() for suite in SUITES}
    syntax_calls_by_suite: dict[str, set[int]] = {suite: set() for suite in SUITES}
    expected_cells = {
        (phase, suite, mode, repeat)
        for phase in ("app", "wall")
        for suite in SUITES
        for mode in MODES
        for repeat in REPEATS
    }

    for row in observations:
        if not isinstance(row, dict):
            _fail("each observation must be an object")
        phase, suite, mode, repeat = (
            row.get("phase"), row.get("suite"), row.get("mode"), row.get("repeat")
        )
        if phase not in ("app", "wall") or suite not in SUITES or mode not in MODES:
            _fail("observation has an unknown phase, suite, or cache mode")
        if row.get("stage") != "protobuf":
            _fail("updated observations must use stage 'protobuf'")
        if type(repeat) is not int or repeat not in REPEATS:
            _fail("repeat must be an integer from 1 through 4")
        key = (phase, suite, mode, repeat)
        if key in seen:
            _fail("duplicate phase/suite/mode/repeat observation")
        seen.add(key)
        if row.get("valid") is not True or row.get("measured") is not True:
            _fail("every updated observation must be valid and measured")

        normalized = {
            "suite": suite,
            "stage": "protobuf",
            "mode": mode,
            "repeat": repeat,
            "measurement_date": created.date().isoformat(),
            "reused": False,
            "source_session": "updated-protobuf",
        }
        if phase == "app":
            normalized["app_elapsed_ms"] = _finite_nonnegative(
                row.get("app_elapsed_ms"), "app_elapsed_ms"
            )
            normalized["app_calls"] = _nonnegative_integer(row.get("app_calls"), "app_calls")
            normalized["syntax_only_calls"] = _nonnegative_integer(
                row.get("syntax_only_calls"), "syntax_only_calls"
            )
            if normalized["app_calls"] == 0:
                _fail("app_calls must be positive")
            app_calls_by_suite[suite].add(normalized["app_calls"])
            syntax_calls_by_suite[suite].add(normalized["syntax_only_calls"])
            app_rows.append(normalized)
        else:
            normalized["elapsed_s"] = _finite_nonnegative(row.get("wall_s"), "wall_s")
            test_counts = row.get("test_counts")
            if not isinstance(test_counts, dict):
                _fail("wall observation needs nested test_counts")
            counts = {
                field: _nonnegative_integer(test_counts.get(field), f"test_counts.{field}")
                for field in ("total_tests", "passed_tests", "failed_tests", "skipped_tests")
            }
            if counts["total_tests"] != counts["passed_tests"] + counts["failed_tests"] + counts["skipped_tests"]:
                _fail("test_counts total does not equal passed, failed, and skipped")
            if counts["failed_tests"] != 0:
                _fail("valid wall observations cannot contain failed tests")
            normalized.update(counts)
            normalized.update({"valid": True, "measured": True, "return_code": 0})
            wall_rows.append(normalized)

    if seen != expected_cells:
        _fail("observations are missing one or more phase/suite/mode/repeat cells")
    for suite in SUITES:
        if len(app_calls_by_suite[suite]) != 1:
            _fail(f"app_calls must be stable across the {suite} App observations")
        if len(syntax_calls_by_suite[suite]) != 1:
            _fail(f"syntax_only_calls must be stable across the {suite} App observations")

    return {
        "created_utc": created,
        "completed_utc": completed,
        "app_rows": app_rows,
        "wall_rows": wall_rows,
        "app_calls_by_suite": {suite: next(iter(values)) for suite, values in app_calls_by_suite.items()},
        "syntax_calls_by_suite": {suite: next(iter(values)) for suite, values in syntax_calls_by_suite.items()},
    }


def _expected_stage_modes() -> list[tuple[str, str]]:
    return [(mode, stage) for mode in MODES for stage in STAGES
            if mode == "direct" or stage != "before-cache"]


def _validate_prior_rows(prior: Any, key: str, metric: str) -> list[dict[str, Any]]:
    if not isinstance(prior, dict) or not isinstance(prior.get(key), list):
        _fail(f"prior evidence needs a {key} array")
    rows = prior[key]
    expected = {
        (suite, stage, mode, repeat)
        for suite in SUITES
        for mode, stage in _expected_stage_modes()
        for repeat in REPEATS
    }
    found: dict[tuple[str, str, str, int], dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            _fail(f"{key} contains a non-object row")
        suite, stage, mode, repeat = (
            row.get("suite"), row.get("stage"), row.get("mode"), row.get("repeat")
        )
        if suite not in SUITES or stage not in STAGES or mode not in MODES or type(repeat) is not int:
            _fail(f"{key} contains an unknown suite, stage, mode, or repeat")
        cell = (suite, stage, mode, repeat)
        if cell in found:
            _fail(f"{key} contains a duplicate cell")
        found[cell] = row
        if metric not in row:
            _fail(f"{key} row is missing {metric}")
        _finite_nonnegative(row[metric], f"{key}.{metric}")
        _public_date(row.get("measurement_date"), f"{key}.measurement_date")
    if set(found) != expected:
        _fail(f"{key} does not contain the complete historical chart matrix")
    return rows


def combine_rows(prior: Any, updated: dict[str, Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Replace only the prior Protobuf rows, preserving every other control row."""
    prior_app = _validate_prior_rows(prior, "app_plot_rows", "app_elapsed_ms")
    prior_wall = _validate_prior_rows(prior, "wall_plot_rows", "elapsed_s")
    app_rows = [row for row in prior_app if row["stage"] != "protobuf"] + updated["app_rows"]
    wall_rows = [row for row in prior_wall if row["stage"] != "protobuf"] + updated["wall_rows"]
    _validate_combined(app_rows, "app_elapsed_ms")
    _validate_combined(wall_rows, "elapsed_s")
    return app_rows, wall_rows


def _validate_combined(rows: list[dict[str, Any]], metric: str) -> None:
    expected = {
        (suite, stage, mode, repeat)
        for suite in SUITES
        for mode, stage in _expected_stage_modes()
        for repeat in REPEATS
    }
    found: set[tuple[str, str, str, int]] = set()
    for row in rows:
        key = (row["suite"], row["stage"], row["mode"], row["repeat"])
        if key in found:
            _fail("combined chart data contains a duplicate cell")
        found.add(key)
        _finite_nonnegative(row[metric], f"combined.{metric}")
    if found != expected:
        _fail("combined chart data has a missing cell")


def _cell_values(rows: list[dict[str, Any]], suite: str, mode: str, stage: str, metric: str) -> list[float]:
    selected = [row for row in rows if row["suite"] == suite and row["mode"] == mode
                and row["stage"] == stage]
    selected.sort(key=lambda row: row["repeat"])
    if len(selected) != len(REPEATS):
        _fail(f"incomplete chart cell for {suite}/{mode}/{stage}")
    return [float(row[metric]) for row in selected]


def _fmt_range(values: list[float], scale: float, unit: str, digits: int) -> str:
    return (f"{statistics.median(values) / scale:.{digits}f} {unit} "
            f"({min(values) / scale:.{digits}f} to {max(values) / scale:.{digits}f})")


def _source_table(
    prior_app: list[dict[str, Any]], prior_wall: list[dict[str, Any]], updated: dict[str, Any]
) -> str:
    dates_by_stage: dict[str, set[str]] = {stage: set() for stage in STAGES if stage != "protobuf"}
    for rows in (prior_app, prior_wall):
        for row in rows:
            if row["stage"] != "protobuf":
                dates_by_stage[row["stage"]].add(str(row.get("measurement_date", "date not recorded")))
    historical = []
    for stage in ("before-cache", "ccache-text", "flatbuffers"):
        dates = ", ".join(sorted(dates_by_stage[stage])) or "date not recorded"
        historical.append(
            f'<tr><th scope="row">{html.escape(STAGE_LABELS[stage])}</th>'
            f'<td>Reused historical controls</td><td>{html.escape(dates)}</td></tr>'
        )
    return (
        '<table class="sources"><thead><tr><th>Measurements</th><th>Source</th><th>Session dates</th></tr></thead><tbody>'
        '<tr><th scope="row">Protobuf</th><td>Updated four-repeat session</td>'
        f'<td>Created {html.escape(format_utc(updated["created_utc"]))}<br>'
        f'Completed {html.escape(format_utc(updated["completed_utc"]))}</td></tr>'
        + "".join(historical) +
        '</tbody></table>'
    )


def _sha256(value: Any, label: str) -> str:
    if not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{64}", value):
        _fail(f"{label} must be a lowercase SHA-256 digest")
    return value


def validate_javascript_workload_provenance(updated_payload: Any) -> dict[str, Any]:
    if not isinstance(updated_payload, dict):
        _fail("JavaScript workload provenance needs a results object")
    sources = updated_payload.get("sources")
    protobuf = sources.get("protobuf") if isinstance(sources, dict) else None
    if not isinstance(protobuf, dict):
        _fail("JavaScript workload provenance needs the Protobuf source manifest")
    try:
        baseline = _REFRESH_SUPPORT.load_cohort_baseline()
    except (RuntimeError, KeyError, AttributeError) as error:
        _fail(f"pinned JavaScript source manifest is invalid: {error}")
    if protobuf.get("javascript_test_sources") != baseline["javascript_test_sources"]:
        _fail("JavaScript source manifest differs from the complete frozen test-source hashes")
    repositories = protobuf.get("repositories")
    clava = repositories.get("clava") if isinstance(repositories, dict) else None
    revision = clava.get("revision") if isinstance(clava, dict) else None
    try:
        expected = _REFRESH_SUPPORT.expected_js_workload_overlay(revision)
    except (RuntimeError, KeyError, AttributeError) as error:
        _fail(f"JavaScript test workload source policy is invalid: {error}")
    if protobuf.get("javascript_workload_overlay") != expected:
        _fail("JavaScript test workload provenance differs from the pinned source policy")
    return expected


def validate_release_provenance(
    updated_payload: Any, manifest: Any, manifest_sha256: str
) -> dict[str, Any]:
    """Validate the safe runtime and wire metadata against the captured manifest."""
    if not isinstance(updated_payload, dict) or not isinstance(manifest, dict):
        _fail("release provenance needs result and manifest objects")
    protobuf = updated_payload.get("sources", {}).get("protobuf", {})
    selected = protobuf.get("selected_release", {})
    selected_manifest_sha = _sha256(
        selected.get("manifest_sha256"), "selected release manifest hash"
    )
    manifest_sha256 = _sha256(manifest_sha256, "release manifest hash")
    if selected_manifest_sha != manifest_sha256:
        _fail("release manifest does not match the selected measurement release")

    protocol = manifest.get("protocol")
    if not isinstance(protocol, dict) or protocol.get("id") != "clava-ast-wire":
        _fail("release manifest has an unknown wire protocol")
    major, minor = protocol.get("major"), protocol.get("minor")
    if type(major) is not int or major < 0 or type(minor) is not int or minor < 0:
        _fail("release manifest protocol version must contain non-negative integers")
    schema_sha = _sha256(protocol.get("schema_sha256"), "protocol schema hash")
    descriptor_sha = _sha256(protocol.get("descriptor_sha256"), "protocol descriptor hash")
    assets = manifest.get("assets")
    if not isinstance(assets, list):
        _fail("release manifest needs an assets list")
    protocol_asset_hashes = {
        asset.get("filename"): _sha256(asset.get("sha256"), "protocol asset hash")
        for asset in assets
        if isinstance(asset, dict) and asset.get("kind") == "protocol"
    }
    if protocol_asset_hashes.get("clang-dumper-ast-wire.proto") != schema_sha:
        _fail("wire schema hash does not match the protocol asset")
    if protocol_asset_hashes.get("clang-dumper-ast-wire.pb") != descriptor_sha:
        _fail("wire descriptor hash does not match the protocol asset")

    tool_sha = _sha256(selected.get("tool_sha256"), "selected tool hash")
    native_sha = _sha256(
        updated_payload.get("native_sha256", {}).get("protobuf"), "measured native tool hash"
    )
    if tool_sha != native_sha:
        _fail("selected tool hash does not match the measured native tool")
    tool_asset = selected.get("tool_asset")
    if not isinstance(tool_asset, dict) or tool_asset.get("kind") != "tool":
        _fail("selected tool asset must identify a manifest tool")
    tool_filename = tool_asset.get("filename")
    tool_platform = tool_asset.get("platform")
    tool_arch = tool_asset.get("arch")
    if (not isinstance(tool_filename, str) or not tool_filename
            or Path(tool_filename).name != tool_filename or "\\" in tool_filename
            or not isinstance(tool_platform, str) or not tool_platform
            or not isinstance(tool_arch, str) or not tool_arch):
        _fail("selected tool asset needs a safe filename, platform, and architecture")
    selected_asset_sha = _sha256(tool_asset.get("sha256"), "selected tool asset hash")
    if selected_asset_sha != tool_sha:
        _fail("selected tool asset does not match the measured native tool")
    matching_tools = [
        asset for asset in assets
        if isinstance(asset, dict)
        and asset.get("kind") == "tool"
        and asset.get("filename") == tool_filename
        and asset.get("platform") == tool_platform
        and asset.get("arch") == tool_arch
    ]
    if len(matching_tools) != 1:
        _fail("selected tool asset is missing or ambiguous in the release manifest")
    manifest_tool_sha = _sha256(matching_tools[0].get("sha256"), "manifest tool hash")
    if manifest_tool_sha != tool_sha:
        _fail("release manifest selected tool does not match the measured native tool")

    repositories = protobuf.get("repositories")
    if not isinstance(repositories, dict):
        _fail("results need nested source repository revisions")
    repository_labels = {
        "clava": "Clava",
        "specs-java-libs": "specs-java-libs",
        "lara-framework": "Lara framework",
        "clang-dumper": "clang-dumper",
    }
    revisions = {}
    for name, label in repository_labels.items():
        repository = repositories.get(name)
        if not isinstance(repository, dict):
            _fail(f"results are missing the {name} source revision")
        revision = repository.get("revision")
        if not isinstance(revision, str) or not re.fullmatch(r"[0-9a-f]{40}", revision):
            _fail(f"{name} source revision must be a full commit id")
        revisions[label] = revision
    native_repository = protobuf.get("native_repository")
    if not isinstance(native_repository, dict) or native_repository.get("revision") != revisions["clang-dumper"]:
        _fail("native source revision disagrees with the clang-dumper revision")
    workload_overlay = validate_javascript_workload_provenance(updated_payload)

    return {
        "revisions": revisions,
        "protocol_id": "clava-ast-wire",
        "protocol_version": f"{major}.{minor}",
        "schema_sha256": schema_sha,
        "descriptor_sha256": descriptor_sha,
        "tool_sha256": tool_sha,
        "manifest_sha256": manifest_sha256,
        "clava_source_tree_sha256": _sha256(
            protobuf.get("clang_ast_parser_source_tree_sha256"), "Clava parser source tree hash"
        ),
        "javascript_workload_overlay": workload_overlay,
    }


def _provenance_section(provenance: dict[str, Any]) -> str:
    rows = []
    for label, revision in provenance["revisions"].items():
        rows.append(
            f'<tr><th scope="row">{html.escape(label)} source</th>'
            f'<td><code>{html.escape(revision)}</code></td><td>Repository revision</td></tr>'
        )
    rows.extend([
        '<tr><th scope="row">Clava parser source snapshot</th>'
        f'<td><code>{html.escape(provenance["clava_source_tree_sha256"])}</code></td>'
        '<td>Source tree SHA-256</td></tr>',
        f'<tr><th scope="row">Wire protocol</th><td>{html.escape(provenance["protocol_id"])} '
        f'{html.escape(provenance["protocol_version"])}</td><td>Protocol version</td></tr>',
        f'<tr><th scope="row">clang-dumper-ast-wire.proto</th><td><code>{html.escape(provenance["schema_sha256"])}</code></td>'
        '<td>Schema SHA-256</td></tr>',
        f'<tr><th scope="row">clang-dumper-ast-wire.pb</th><td><code>{html.escape(provenance["descriptor_sha256"])}</code></td>'
        '<td>Descriptor SHA-256</td></tr>',
        f'<tr><th scope="row">Native producer</th><td><code>{html.escape(provenance["tool_sha256"])}</code></td>'
        '<td>Executable SHA-256</td></tr>',
        f'<tr><th scope="row">Release manifest</th><td><code>{html.escape(provenance["manifest_sha256"])}</code></td>'
        '<td>Manifest SHA-256</td></tr>',
    ])
    return (
        '<details><summary>Runtime source and wire provenance</summary>'
        '<p>These identifiers come from the measured source snapshot and the release manifest '
        'whose hash is recorded with the results. The manifest schema and descriptor hashes '
        'and native tool hash are checked against that measurement record.</p>'
        '<table class="sources"><thead><tr><th>Component</th><th>Identifier</th><th>Meaning</th></tr></thead>'
        f'<tbody>{"".join(rows)}</tbody></table></details>'
    )


def _javascript_workload_section(overlay: dict[str, Any]) -> str:
    rows = []
    changed = []
    for relative, hashes in overlay["files"].items():
        current_hash = hashes["original_current_sha256"]
        staged_hash = hashes["staged_sha256"]
        if current_hash != staged_hash:
            changed.append(relative)
        rows.append(
            f'<tr><th scope="row"><code>{html.escape(relative)}</code></th>'
            f'<td><code>{html.escape(current_hash)}</code></td>'
            f'<td><code>{html.escape(staged_hash)}</code></td></tr>'
        )
    if changed:
        summary = (
            "The current snapshot has different test bytes for "
            + ", ".join(f"<code>{html.escape(path)}</code>" for path in changed)
            + ". The benchmark staged the frozen test fixture into its isolated snapshot. "
              "The measured Clava implementation and generated runtime artifacts remain from "
              "the current Clava revision listed above."
        )
    else:
        summary = (
            "The current snapshot's selected test bytes already match the frozen fixtures. "
            "The benchmark still staged and checked all selected files in its isolated snapshot."
        )
    return (
        '<details><summary>JavaScript test workload provenance</summary>'
        f'<p>{summary}</p>'
        f'<p>All {overlay["file_count"]} selected test files were staged from frozen workload revision '
        f'<code>{html.escape(overlay["frozen_source_revision"])}</code>. '
        f'The fixture manifest SHA-256 is <code>{html.escape(overlay["staged_manifest_sha256"])}</code>.</p>'
        '<table class="sources"><thead><tr><th>Test file</th><th>Current source SHA-256</th>'
        '<th>Measured staged SHA-256</th></tr></thead><tbody>'
        f'{"".join(rows)}</tbody></table></details>'
    )


def _summary_table(app_rows: list[dict[str, Any]], wall_rows: list[dict[str, Any]]) -> str:
    out = ['<table class="summary"><thead><tr><th>Suite</th><th>Cache state</th><th>Format</th>'
           '<th>App time, median (range)</th><th>Command wall time, median (range)</th>'
           '<th>Tests, total/pass/fail/skip</th><th>Source</th></tr></thead><tbody>']
    for suite in SUITES:
        stages_by_mode = {
            "direct": STAGES,
            "cold": STAGES[1:],
            "warm": STAGES[1:],
        }
        for mode in MODES:
            for stage in stages_by_mode[mode]:
                app = _cell_values(app_rows, suite, mode, stage, "app_elapsed_ms")
                wall = _cell_values(wall_rows, suite, mode, stage, "elapsed_s")
                test_runs = sorted(
                    (row for row in wall_rows if row["suite"] == suite and row["mode"] == mode
                     and row["stage"] == stage), key=lambda row: row["repeat"]
                )
                per_repeat_counts = [
                    f'{row.get("total_tests", "?")}/{row.get("passed_tests", "?")}/'
                    f'{row.get("failed_tests", "?")}/{row.get("skipped_tests", "?")}'
                    for row in test_runs
                ]
                if len(set(per_repeat_counts)) == 1:
                    test_counts = f"{per_repeat_counts[0]} (all four)"
                else:
                    test_counts = "; ".join(
                        f"r{row['repeat']} {counts}"
                        for row, counts in zip(test_runs, per_repeat_counts)
                    )
                source = "New session" if stage == "protobuf" else "Reused control"
                out.append(
                    f'<tr><th scope="row">{html.escape(SUITE_LABELS[suite])}</th>'
                    f'<td>{html.escape(MODE_LABELS[mode])}</td>'
                    f'<td>{html.escape(STAGE_LABELS[stage])}</td>'
                    f'<td>{html.escape(_fmt_range(app, 1000.0, "s", 2))}</td>'
                    f'<td>{html.escape(_fmt_range(wall, 1.0, "s", 2))}</td>'
                    f'<td>{html.escape(test_counts)}</td>'
                    f'<td>{source}</td></tr>'
                )
    return "".join(out) + "</tbody></table>"


def _app_charts(app_rows: list[dict[str, Any]], updated: dict[str, Any]) -> str:
    totals: dict[tuple[str, str, str], list[float]] = {}
    for suite in SUITES:
        for mode, stage in _expected_stage_modes():
            totals[(suite, mode, stage)] = _cell_values(
                app_rows, suite, mode, stage, "app_elapsed_ms"
            )
    data = {
        "ids_by_suite": {
            suite: range(updated["app_calls_by_suite"][suite]) for suite in SUITES
        }
    }
    charts = []
    for suite in SUITES:
        chart = _APP_CHARTS._chart(data, totals, suite)
        charts.append(
            f'<figure><figcaption>{html.escape(SUITE_LABELS[suite])}, '
            f'{updated["app_calls_by_suite"][suite]} timed App calls</figcaption>{chart}</figure>'
        )
    return "".join(charts)


def _wall_charts(wall_rows: list[dict[str, Any]]) -> str:
    original_validator = _WALL_CHARTS.is_valid_run

    def valid_run(row: dict[str, Any], suite: str) -> bool:
        if row.get("source_session") == "updated-protobuf":
            return row.get("valid") is True and row.get("failed_tests") == 0
        return original_validator(row, suite)

    _WALL_CHARTS.is_valid_run = valid_run
    try:
        charts = []
        for suite in SUITES:
            chart = _WALL_CHARTS.candle(suite, wall_rows)
            charts.append(
                f'<figure class="wall-chart"><figcaption>{html.escape(SUITE_LABELS[suite])}, '
                f'all cache states</figcaption>{chart}</figure>'
            )
        return "".join(charts)
    finally:
        _WALL_CHARTS.is_valid_run = original_validator


def _csv_data(app_rows: list[dict[str, Any]], wall_rows: list[dict[str, Any]], updated: dict[str, Any]) -> list[list[Any]]:
    rows: list[list[Any]] = [[
        "suite", "mode", "stage", "repeat", "metric", "value", "unit", "source",
        "measurement_date", "app_calls", "syntax_only_calls", "total_tests", "passed_tests",
        "failed_tests", "skipped_tests",
    ]]
    for source_rows, metric, unit in (
        (app_rows, "app_elapsed_ms", "ms"),
        (wall_rows, "elapsed_s", "s"),
    ):
        for row in sorted(source_rows, key=lambda item: (
                SUITES.index(item["suite"]), MODES.index(item["mode"]),
                STAGES.index(item["stage"]), item["repeat"])):
            new_session = row.get("source_session") == "updated-protobuf"
            rows.append([
                row["suite"], row["mode"], row["stage"], row["repeat"], metric,
                row[metric], unit, "updated Protobuf session" if new_session else "reused control",
                row.get("measurement_date", ""), row.get("app_calls", ""),
                row.get("syntax_only_calls", ""), row.get("total_tests", ""),
                row.get("passed_tests", ""), row.get("failed_tests", ""),
                row.get("skipped_tests", ""),
            ])
            if new_session:
                rows[-1].extend([
                    format_utc(updated["created_utc"]),
                    format_utc(updated["completed_utc"]),
                ])
            else:
                rows[-1].extend(["", ""])
    rows[0].extend(["session_created_utc", "session_completed_utc"])
    return rows


def _memory_section(payload: Any) -> str:
    summaries = _validate_memory_inputs(payload)
    if not summaries:
        return ""
    out = [
        '<details><summary>Separate memory diagnostics</summary>',
        '<p>These Protobuf-only probes ran in separate JVM sessions with G1, a 4 GiB heap limit, and explicit GC at each measurement phase. They have no fresh Text or FlatBuffers memory controls.</p>',
        '<p>For each JVM, the table reports the first and last retained heap readings after App release, the 20-reading range, and the fitted slope per cycle. A flat or falling series does not prove that no leak exists. The probe also checks App collection and parser resource cleanup.</p>',
    ]
    for workload in summaries:
        out.append(
            f'<h3>{html.escape(workload["label"])}</h3>'
            f'<p>Probe session created {html.escape(format_utc(workload["created_utc"]))}. '
            'All rows show the same AST node count for this workload.</p>'
            '<table class="summary memory"><thead><tr><th>JVM repeat</th><th>AST nodes</th>'
            '<th>Live heap after GC with App alive, median / max</th>'
            '<th>Retained heap after release, first / last</th>'
            '<th>Retained range, 20 cycles</th><th>Fitted slope (MiB/cycle)</th>'
            '<th>JVM VmHWM max</th><th>GNU time max RSS</th></tr></thead><tbody>'
        )
        for probe in workload["probes"]:
            mib = 1024 * 1024
            slope = probe["retained_slope_bytes"] / mib
            out.append(
                f'<tr><th scope="row">{probe["repeat"]}</th><td>{probe["nodes"]:,}</td>'
                f'<td>{statistics.median(probe["live_heap_bytes"]) / mib:.2f} / '
                f'{max(probe["live_heap_bytes"]) / mib:.2f} MiB</td>'
                f'<td>{probe["retained_first_bytes"] / mib:.2f} / {probe["retained_last_bytes"] / mib:.2f} MiB</td>'
                f'<td>{probe["retained_min_bytes"] / mib:.2f} to {probe["retained_max_bytes"] / mib:.2f} MiB</td>'
                f'<td>{slope:+.4f} MiB</td>'
                f'<td>{probe["jvm_peak_rss_bytes"] / mib:.2f} MiB</td>'
                f'<td>{probe["gnu_max_rss_kb"] / 1024:.2f} MiB</td></tr>'
            )
        out.append('</tbody></table>')
    out.append(
        '<p>Across all 120 phase rows, the weak-referenced App was collected. '
        'Open parser files, mapped work paths, leftover Clang temporary folders, and unexpected parser file descriptors were all zero or empty. '
        'Live heap was measured after GC while the App remained alive; retained heap was measured after release and GC. '
        'JVM VmHWM and GNU time max RSS are separate process high-water readings.</p></details>'
    )
    return "".join(out)


def _validate_memory_inputs(memory_inputs: Any) -> list[dict[str, Any]]:
    if memory_inputs is None:
        return []
    if not isinstance(memory_inputs, list) or len(memory_inputs) != 2:
        _fail("memory evidence must contain separate NAS+ and C++ templates inputs")
    summaries = []
    seen_labels: set[str] = set()
    for label, payload in memory_inputs:
        if not isinstance(label, str) or not re.fullmatch(r"[A-Za-z0-9+ ._-]{1,40}", label):
            _fail("memory workload labels must be short plain text")
        if label in seen_labels:
            _fail("memory workload labels must be unique")
        seen_labels.add(label)
        if not isinstance(payload, dict) or not isinstance(payload.get("observations"), list):
            _fail("each memory input needs an observations array")
        if type(payload.get("repeats")) is not int or payload["repeats"] != 3:
            _fail(f"{label} memory evidence must declare three JVM repeats")
        if payload.get("failed") != []:
            _fail(f"{label} memory evidence must have an empty failed list")
        if "explicit GC" not in str(payload.get("retained_heap_contract", "")):
            _fail(f"{label} memory evidence must identify its explicit-GC retained-heap contract")
        if payload.get("peak_rss_contract") != "GNU time max_rss_kb":
            _fail(f"{label} memory evidence must identify GNU time peak RSS separately")
        created = _utc(payload.get("created_utc"), f"{label}.created_utc")
        observations = payload["observations"]
        if len(observations) != 3:
            _fail(f"{label} memory evidence must contain three observations")
        probe_rows = []
        seen_repeats: set[int] = set()
        for observation in observations:
            if not isinstance(observation, dict):
                _fail(f"{label} memory observations must be objects")
            repeat = observation.get("repeat")
            if type(repeat) is not int or repeat not in (1, 2, 3) or repeat in seen_repeats:
                _fail(f"{label} memory repeats must be exactly 1, 2, and 3")
            seen_repeats.add(repeat)
            if type(observation.get("return_code")) is not int or observation["return_code"] != 0:
                _fail(f"{label} memory repeat {repeat} did not exit cleanly")
            gnu_time = observation.get("gnu_time")
            if (not isinstance(gnu_time, dict) or type(gnu_time.get("exit_status")) is not int
                    or gnu_time["exit_status"] != 0):
                _fail(f"{label} memory repeat {repeat} has failed GNU time status")
            gnu_rss = _nonnegative_integer(gnu_time.get("max_rss_kb"), "gnu_time.max_rss_kb")
            phases = observation.get("heap_phases")
            if not isinstance(phases, list) or len(phases) != 20:
                _fail(f"{label} memory repeat {repeat} must contain twenty phase rows")
            phases = sorted(phases, key=lambda row: row.get("repeat", 0) if isinstance(row, dict) else 0)
            if [phase.get("repeat") if isinstance(phase, dict) else None for phase in phases] != list(range(1, 21)):
                _fail(f"{label} memory repeat {repeat} needs phase indices 1 through 20")
            live, retained, jvm_hwm, nodes = [], [], [], []
            for phase in phases:
                if phase.get("phase") != "parse_released":
                    _fail(f"{label} memory phases must represent parse release")
                if phase.get("app_collected") is not True:
                    _fail(f"{label} memory repeat {repeat} did not collect the released App")
                for key in ("open_parser_files", "mapped_paths_under_work", "leftover_clang_temp_folders"):
                    if _nonnegative_integer(phase.get(key), key) != 0:
                        _fail(f"{label} memory repeat {repeat} retained {key}")
                for key in ("observer_open_parser_fd_targets", "observed_open_parser_fd_targets"):
                    targets = phase.get(key)
                    if not isinstance(targets, dict) or len(targets) != 3:
                        _fail(f"{label} memory repeat {repeat} has incomplete {key}")
                unexpected_fds = phase.get("unexpected_open_parser_fd_targets")
                if not isinstance(unexpected_fds, dict) or unexpected_fds:
                    _fail(f"{label} memory repeat {repeat} retained unexpected parser file descriptors")
                live.append(_finite_nonnegative(phase.get("live_heap_bytes"), "live_heap_bytes"))
                retained.append(_finite_nonnegative(phase.get("retained_heap_bytes"), "retained_heap_bytes"))
                jvm_hwm.append(_finite_nonnegative(phase.get("jvm_peak_rss_bytes"), "jvm_peak_rss_bytes"))
                nodes.append(_nonnegative_integer(phase.get("nodes"), "nodes"))
            if len(set(nodes)) != 1:
                _fail(f"{label} memory repeat {repeat} changed AST node count during the probe")
            retained_mean = statistics.mean(retained)
            x_mean = 10.5
            slope = sum((x - x_mean) * (y - retained_mean) for x, y in enumerate(retained, 1)) / sum(
                (x - x_mean) ** 2 for x in range(1, 21)
            )
            probe_rows.append({
                "repeat": repeat,
                "nodes": nodes[0],
                "live_heap_bytes": live,
                "retained_first_bytes": retained[0],
                "retained_last_bytes": retained[-1],
                "retained_min_bytes": min(retained),
                "retained_max_bytes": max(retained),
                "retained_slope_bytes": slope,
                "jvm_peak_rss_bytes": max(jvm_hwm),
                "gnu_max_rss_kb": gnu_rss,
            })
        if seen_repeats != {1, 2, 3}:
            _fail(f"{label} memory evidence is missing a JVM repeat")
        probe_rows.sort(key=lambda row: row["repeat"])
        if len({probe["nodes"] for probe in probe_rows}) != 1:
            _fail(f"{label} memory repeats disagree on AST node count")
        summaries.append({"label": label, "created_utc": created, "probes": probe_rows})
    if seen_labels != {"NAS+", "C++ templates"}:
        _fail("memory evidence labels must be NAS+ and C++ templates")
    summaries.sort(key=lambda item: (item["label"] != "NAS+", item["label"]))
    return summaries


def render_report(
    prior: Any,
    updated_payload: Any,
    memory_payload: Any = None,
    release_manifest: Any = None,
    release_manifest_sha256: str | None = None,
) -> str:
    updated = validate_updated_manifest(updated_payload)
    workload_overlay = validate_javascript_workload_provenance(updated_payload)
    prior_app_rows = _validate_prior_rows(prior, "app_plot_rows", "app_elapsed_ms")
    prior_wall_rows = _validate_prior_rows(prior, "wall_plot_rows", "elapsed_s")
    app_rows, wall_rows = combine_rows(prior, updated)
    provenance = None
    if release_manifest is not None or release_manifest_sha256 is not None:
        if release_manifest is None or release_manifest_sha256 is None:
            _fail("release manifest JSON and its SHA-256 must be supplied together")
        provenance = validate_release_provenance(
            updated_payload, release_manifest, release_manifest_sha256
        )
    csv_json = json.dumps(_csv_data(app_rows, wall_rows, updated), ensure_ascii=False)
    csv_json = (csv_json.replace("<", "\\u003c").replace(">", "\\u003e")
                .replace("&", "\\u0026").replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029"))
    return f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Clava Protobuf benchmark update</title>
<style>
html {{ color-scheme:light; --report-page:#f7f9fc; --report-panel:#fff; --report-ink:#202a34; --report-muted:#56636e; --report-line:#d9e0e5; --report-grid:#e1e7ec; --report-surface:#fff; --report-accent:#6d45c0; --report-code:#5630a2; }}
html.dark {{ color-scheme:dark; --report-page:#101722; --report-panel:#182231; --report-ink:#e8edf5; --report-muted:#a7b4c7; --report-line:#354357; --report-grid:#2b394c; --report-surface:#182231; --report-accent:#8d68df; --report-code:#d7c6ff; }}
* {{ box-sizing:border-box; }}
body {{ max-width:1480px; margin:0 auto; padding:28px; background:var(--report-page); color:var(--report-ink); font:15px/1.55 system-ui,sans-serif; }}
h1,h2,h3 {{ line-height:1.2; }} h1 {{ font-size:30px; margin:0 0 8px; }} h2 {{ font-size:22px; margin:30px 0 10px; }}
p {{ color:var(--report-muted); }} .lede {{ max-width:1000px; }} .note {{ border-left:3px solid #7c3aed; padding:8px 14px; background:var(--report-panel); }}
button {{ background:var(--report-accent); color:white; border:0; border-radius:6px; padding:9px 13px; cursor:pointer; font:inherit; }}
.sources,.summary {{ border-collapse:collapse; width:100%; margin:12px 0 18px; font-size:13px; }}
th,td {{ padding:7px 9px; text-align:left; border-bottom:1px solid var(--report-line); vertical-align:top; }}
th {{ color:var(--report-ink); font-weight:650; }} .sources th:first-child {{ min-width:150px; }}
.panels {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(350px,1fr)); gap:14px; }}
figure {{ background:var(--report-panel); border:1px solid var(--report-line); border-radius:9px; margin:0; padding:14px; min-width:0; }}
figcaption {{ font-weight:650; margin-bottom:7px; }} svg {{ max-width:100%; height:auto; }}
.app-chart {{ display:block; width:100%; max-width:520px; }}
.grid,.grid-line {{ stroke:var(--report-grid); }} .divider {{ stroke:var(--report-muted); }}
.tick,.label,.mode,.foot,.axis-text,.stage-text,.empty-lane {{ fill:var(--report-muted); }}
.value,.median-label {{ fill:var(--report-ink); }} .mode {{ font-weight:650; }}
.wall-chart .tick,.wall-chart .label,.wall-chart .value {{ font-size:12px; }}
.wall-chart .mode {{ font-size:13px; }} .wall-chart .foot {{ font-size:11px; }}
.app-chart .tick {{ fill:var(--report-muted); font:13px system-ui,sans-serif; }}
.app-chart .label,.app-chart .mode {{ fill:var(--report-muted); font:15px system-ui,sans-serif; }}
.app-chart .mode {{ font-weight:650; }}
.app-chart .value {{ fill:var(--report-ink); font:14px ui-monospace,monospace; }}
.app-chart .foot {{ fill:var(--report-muted); font:11px system-ui,sans-serif; }}
.panels figure svg {{ width:100%; }} .app-chart {{ max-width:520px; }}
.details {{ color:var(--report-muted); }}
details {{ margin:14px 0; background:var(--report-panel); border:1px solid var(--report-line); border-radius:7px; padding:10px 13px; }}
summary {{ cursor:pointer; color:var(--report-ink); font-weight:650; }} code {{ color:var(--report-code); }}
@media(max-width:700px) {{ body {{ padding:16px; }} .panels {{ grid-template-columns:1fr; }} .summary {{ display:block; overflow-x:auto; }} }}
</style>
</head>
<body>
<header>
<h1>Clava Protobuf benchmark update</h1>
<p class="lede">The charts replace the earlier Protobuf rows with a new four-repeat session. Text and FlatBuffers rows remain the frozen historical controls. The sessions have separate dates, so the page shows distributions and does not calculate cross-session round pairs or ratios.</p>
{_source_table(prior_app_rows, prior_wall_rows, updated)}
<p class="note">App elapsed time and full-command wall time are separate measurements. App time is summed over timed parser calls. Syntax-only calls are excluded from App time.</p>
<button type="button" id="download-csv">Download chart rows as CSV</button>
</header>
<section>
<h2>App construction time</h2>
<p>Each dot is one of four suite repeats. The chart sums completed App-call durations for that repeat. Its timer covers parsing, protobuf or text decoding, cross-translation-unit linking, final transformations, and parser-state release. Parser/context setup before the timed method, assertions, output, and syntax-only calls are outside this timer. Natural garbage collection remains enabled.</p>
<div class="panels">{_app_charts(app_rows, updated)}</div>
</section>
<section>
<h2>Full-command wall time</h2>
<p>Each candle covers four separate full-suite commands. A monotonic process timer measures each command wall span, separate from the App-call timer. Direct disables ccache; cold starts with an isolated empty cache; warm restores the same complete-suite cache seed before measurement. Java used one worker with a 512 MiB heap. Clava-JS used one Vitest worker, with file parallelism and isolation disabled; its generated configuration used Vitest's runner loader. Its default worker JVM heap limit was about 7.55 GiB. The sessions ran serially after compilation and resource setup.</p>
<div class="panels">{_wall_charts(wall_rows)}</div>
</section>
<section>
<h2>Measurements</h2>
<p>Values are medians and observed min-to-max ranges across four repeats within each chart cell. The test-count column lists each distinct total/pass/fail/skip result in repeat order. The source column distinguishes the new Protobuf session from controls reused from earlier evidence. These values do not establish a matched per-repeat change across sessions.</p>
{_summary_table(app_rows, wall_rows)}
</section>
{_memory_section(memory_payload)}
{_provenance_section(provenance) if provenance is not None else ''}
{_javascript_workload_section(workload_overlay)}
<details><summary>Measurement notes</summary>
<p>The App timer starts at the three-argument <code>ParallelCodeParser.parse</code> entry and stops after the final App is returned. It includes native parsing and protobuf or text reading, cross-file linking, postprocessing, parser-state release, and final TextParser/text transforms. Parser/context construction before entry, suite setup and assertions, printing, and syntax-only validation are excluded. Natural garbage collection is included, with no forced-GC policy override, coverage agents, or execution-info logging.</p>
<p>The Java suite uses its original 116 test identities, one worker, and a 512 MiB heap. The Clava-JS suite retains 164 selected test identities, uses one Vitest worker with file parallelism and isolation disabled, and uses Vitest's runner config loader for generated configurations. It records the actual per-run test counts above. Its worker JVM used the default heap limit, observed at about 7.55 GiB. App timing covers {updated["app_calls_by_suite"]["java"]} Java and {updated["app_calls_by_suite"]["clava-js"]} Clava-JS timed calls. Syntax-only calls are excluded from App time ({updated["syntax_calls_by_suite"]["java"]} Java and {updated["syntax_calls_by_suite"]["clava-js"]} Clava-JS).</p>
<p>Four repeats ran serially. Direct runs disabled ccache; cold runs began from isolated empty caches; warm runs restored an identical complete-suite cache seed before timing. Full-command wall measurements used a monotonic process timer and were collected separately from App-timing runs. GNU time peak RSS appears only in the optional memory diagnostic. Compilation and resource preparation finished before timing. Text and FlatBuffers measurements were reused from their earlier sessions.</p>
</details>
<script type="application/json" id="csv-data">{csv_json}</script>
<script>
document.getElementById('download-csv').addEventListener('click', () => {{
  const rows = JSON.parse(document.getElementById('csv-data').textContent);
  const quote = value => '"' + String(value ?? '').replaceAll('"', '""') + '"';
  const csv = rows.map(row => row.map(quote).join(',')).join('\\r\\n');
  const link = document.createElement('a');
  const url = URL.createObjectURL(new Blob([csv], {{type:'text/csv;charset=utf-8'}}));
  link.href = url;
  link.download = 'clava-protobuf-benchmark-rows.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}});
</script>
</body>
</html>'''


def _load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _parse_memory_spec(value: str) -> tuple[str, Path]:
    label, separator, path = value.partition("=")
    if not separator or not label or not path:
        raise argparse.ArgumentTypeError("--memory must use LABEL=PATH")
    return label, Path(path)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prior-evidence", type=Path, required=True)
    parser.add_argument("--updated-results", type=Path, required=True)
    parser.add_argument("--release-manifest", type=Path,
                        help="matched native release manifest for source and wire hashes")
    parser.add_argument("--memory", action="append", type=_parse_memory_spec,
                        metavar="LABEL=PATH", help="add a separate memory probe, repeat twice")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        prior = _load_json(args.prior_evidence)
        updated = _load_json(args.updated_results)
        memory = [(label, _load_json(path)) for label, path in args.memory] if args.memory else None
        release_manifest = None
        release_manifest_sha256 = None
        if args.release_manifest is not None:
            manifest_bytes = args.release_manifest.read_bytes()
            release_manifest = json.loads(manifest_bytes)
            release_manifest_sha256 = hashlib.sha256(manifest_bytes).hexdigest()
        rendered = render_report(
            prior, updated, memory, release_manifest, release_manifest_sha256
        )
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    except (OSError, json.JSONDecodeError, ValueError) as error:
        print(f"render_updated_benchmark.py: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
