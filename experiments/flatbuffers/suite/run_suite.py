#!/usr/bin/env python3
"""Run the production Clava-JS suite with isolated cache and runtime state.

The runner records one JSON result from Vitest, GNU time resource metrics,
ccache statistics, environment/revision metadata, and flattened per-test
durations. It never writes outside this experiment's ignored ``results`` tree
except for the Vitest process itself, which runs against the checked-out suite.

The parser uses the production eager FlatBuffers path selected by its release
tag. This runner has no wire-format switch. Use one invocation per cache state.
``cold`` creates a new cache and measures
the first run. ``warm`` reuses an existing cache root supplied with
``--cache-root``; this lets a previous cold run populate it without hiding the
population run inside the measured observation. ``bypass`` sets CCACHE_DISABLE.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
from typing import Any, Iterable
from urllib.parse import quote
from urllib.request import urlopen


EXPERIMENT_ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = EXPERIMENT_ROOT.parents[1]
CLAVA_JS_ROOT = CLAVA_ROOT / "Clava-JS"
RESULTS_ROOT = EXPERIMENT_ROOT / "suite" / "results"
DEFAULT_RUNTIME = CLAVA_ROOT / "ClavaWeaver" / "build" / "install" / "ClavaWeaver"
JS_TEST_FILTER = (
    r"^(?!(?:CxxTest OmpThreadsExplore|CudaTest Cuda|CudaTest CudaMatrixMul|"
    r"CudaTest CudaQuery)$).*$"
)
EXPECTED_TEST_COUNTS = {
    "total_tests": 164,
    "passed_tests": 158,
    "failed_tests": 0,
    "pending_tests": 6,
}
PINNED_FLATBUFFERS_VERSION = "25.12.19"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("bypass", "cold", "warm"), required=True)
    parser.add_argument(
        "--cache-root",
        type=Path,
        help="XDG cache root. Cold creates it; warm must point at a populated root.",
    )
    parser.add_argument(
        "--runtime-root", type=Path, default=DEFAULT_RUNTIME,
        help="Prebuilt ClavaWeaver lib directory; its parser JAR carries the selected release tag.",
    )
    parser.add_argument(
        "--local-release-dir", type=Path,
        help="Explicit local release directory. The staged parser JAR tag is set to this path.",
    )
    parser.add_argument("--output-root", type=Path, default=RESULTS_ROOT)
    parser.add_argument(
        "--vitest-arg",
        action="append",
        default=[],
        help="Additional scheduling/output argument for `vitest run`; test selection stays fixed.",
    )
    return parser.parse_args()


def require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise SystemExit(f"{label} does not exist or is not a file: {path}")


def run_checked(command: list[str], *, cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, check=True, text=True, capture_output=True)


def stage_java_runtime(source: Path, destination: Path, release_dir: Path | None) -> Path:
    """Copy the built distribution, with an optional audited local release override."""

    if destination.exists():
        raise SystemExit(f"Refusing to overwrite runtime staging directory: {destination}")
    shutil.copytree(source, destination)
    parser_jar = destination / "lib" / "ClangAstParser.jar"
    require_file(parser_jar, "staged ClangAstParser.jar")

    import zipfile

    temporary_jar = parser_jar.with_suffix(".patched.jar")
    if release_dir is None:
        return destination

    # LocalBuild points at a published-contract release directory containing
    # the manifest, schema bundle, and native executable.
    tag = (str(release_dir.resolve()) + "\n").encode()
    with zipfile.ZipFile(parser_jar) as original, zipfile.ZipFile(
        temporary_jar, "w", compression=zipfile.ZIP_DEFLATED
    ) as patched:
        for entry in original.infolist():
            data = tag if entry.filename == "clang-dumper-release.tag" else original.read(entry)
            patched.writestr(entry, data)
    temporary_jar.replace(parser_jar)
    return destination


def release_metadata(runtime: Path, local_release_dir: Path | None) -> dict[str, Any]:
    """Record the tag and the selected local manifest when one is available."""

    import zipfile

    parser_jar = runtime / "lib" / "ClangAstParser.jar"
    require_file(parser_jar, "runtime ClangAstParser.jar")
    with zipfile.ZipFile(parser_jar) as archive:
        try:
            tag = archive.read("clang-dumper-release.tag").decode().strip()
        except KeyError as error:
            raise SystemExit("ClangAstParser.jar has no clang-dumper-release.tag") from error
    release_root = local_release_dir.resolve() if local_release_dir else None
    if release_root is None:
        candidate = Path(tag)
        if candidate.is_absolute():
            release_root = candidate
    result: dict[str, Any] = {"release_tag": tag, "local_release_dir": str(release_root) if release_root else None}
    if release_root is None:
        result["manifest"] = None
        result["schema_sha256"] = None
        result["tool_sha256"] = None
        return result

    if release_root is not None:
        manifest_path = release_root / "clang-dumper-release-manifest.json"
        require_file(manifest_path, "selected local clang-dumper release manifest")
        manifest_bytes = manifest_path.read_bytes()
        manifest_source = str(manifest_path)
    else:
        manifest_url = (
            "https://github.com/specs-feup/clang-dumper/releases/download/"
            f"{quote(tag, safe='')}/clang-dumper-release-manifest.json"
        )
        try:
            with urlopen(manifest_url, timeout=30) as response:
                manifest_bytes = response.read()
        except OSError as error:
            raise SystemExit(f"could not obtain the manifest for selected release {tag}: {error}") from error
        manifest_path = None
        manifest_source = manifest_url
    manifest = json.loads(manifest_bytes)
    wire_schema = manifest.get("wire_schema", {})
    flatbuffers = manifest.get("flatbuffers", {})
    schema_asset = str(wire_schema.get("asset", ""))
    schema_asset_path = release_root / schema_asset if release_root and schema_asset else None
    schema_asset_hash = wire_schema.get("asset_sha256")
    schema_hash = wire_schema.get("sha256")
    entrypoint = str(wire_schema.get("entrypoint", ""))
    if not entrypoint:
        raise SystemExit("selected release manifest has no wire_schema.entrypoint")
    if manifest.get("schema_version") != 2 or wire_schema.get("version") != 2:
        raise SystemExit("selected release is not schema v2")
    if flatbuffers.get("version") != wire_schema.get("flatbuffers_version"):
        raise SystemExit("release manifest FlatBuffers compiler/runtime versions disagree")
    if flatbuffers.get("version") != PINNED_FLATBUFFERS_VERSION:
        raise SystemExit(
            f"release FlatBuffers version {flatbuffers.get('version')!r} does not match pin "
            f"{PINNED_FLATBUFFERS_VERSION}"
        )
    actual_schema_asset_hash = None
    actual_schema_hash = schema_hash
    if schema_asset_path is not None:
        require_file(schema_asset_path, "selected release schema bundle")
        actual_schema_asset_hash = sha256_file(schema_asset_path)
        if schema_asset_hash != actual_schema_asset_hash:
            raise SystemExit(
                f"schema asset SHA-256 {actual_schema_asset_hash} does not match manifest {schema_asset_hash}"
            )
        import zipfile
        try:
            with zipfile.ZipFile(schema_asset_path) as schema_bundle:
                schema_bytes = schema_bundle.read(entrypoint)
        except (KeyError, zipfile.BadZipFile) as error:
            raise SystemExit(f"schema bundle is missing {entrypoint}: {error}") from error
        actual_schema_hash = hashlib.sha256(schema_bytes).hexdigest()
        if schema_hash != actual_schema_hash:
            raise SystemExit(
                f"schema entrypoint SHA-256 {actual_schema_hash} does not match manifest {schema_hash}"
            )
    tool_record = manifest.get("tool", {})
    if not tool_record:
        assets = manifest.get("assets", [])
        tool_record = next((asset for asset in assets
                            if str(asset.get("filename", asset.get("asset", ""))) != schema_asset), {})
    tool: Path | None = None
    tool_hash = None
    if release_root is not None:
        tool = release_root / "tool"
        if not tool.is_file():
            tool = release_root / "tool.exe"
        require_file(tool, "selected local clang-dumper executable")
        tool_hash = sha256_file(tool)
    manifest_tool_hash = tool_record.get("sha256") or manifest.get("tool_sha256")
    if manifest_tool_hash and tool_hash and manifest_tool_hash != tool_hash:
        raise SystemExit(f"local release tool SHA-256 {tool_hash} does not match manifest {manifest_tool_hash}")
    result.update({
        "manifest": manifest_source,
        "manifest_sha256": hashlib.sha256(manifest_bytes).hexdigest(),
        "manifest_schema_version": manifest.get("schema_version"),
        "wire_schema_version": wire_schema.get("version"),
        "schema_entrypoint": entrypoint,
        "schema_asset": schema_asset,
        "schema_asset_sha256": actual_schema_asset_hash or schema_asset_hash,
        "schema_sha256": actual_schema_hash,
        "flatbuffers_version": flatbuffers.get("version") or wire_schema.get("flatbuffers_version"),
        "flatbuffers_commit": flatbuffers.get("commit"),
        "tool": str(tool) if tool else None,
        "tool_sha256": tool_hash,
        "manifest_tool_sha256": manifest_tool_hash,
    })
    return result


def host_state() -> dict[str, Any]:
    """Capture host pressure beside each observation, without changing it."""

    state: dict[str, Any] = {}
    try:
        state["loadavg"] = Path("/proc/loadavg").read_text().strip()
    except OSError:
        pass
    try:
        memory: dict[str, int] = {}
        for line in Path("/proc/meminfo").read_text().splitlines():
            key, _, value = line.partition(":")
            if key in {"MemTotal", "MemAvailable", "SwapTotal", "SwapFree"}:
                memory[key] = int(value.strip().split()[0])
        state["meminfo_kib"] = memory
    except OSError:
        pass
    return state


def cache_dir(xdg_root: Path) -> Path:
    return xdg_root / "@specs-feup" / "clava" / "clang-dumper-ccache"


def ccache(command: str, cache: Path) -> str:
    completed = subprocess.run(
        ["ccache", "-d", str(cache), command],
        cwd=CLAVA_JS_ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    output = completed.stdout + completed.stderr
    if completed.returncode != 0:
        raise RuntimeError(f"ccache {command} failed ({completed.returncode}):\n{output}")
    return output


def parse_ccache_stats(text: str) -> dict[str, int | float | str]:
    values: dict[str, int | float | str] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        match = re.match(r"([^:]+):\s+([0-9][0-9,]*(?:\.[0-9]+)?)(?:\s+.*)?$", line)
        if not match:
            continue
        key = re.sub(r"[^a-z0-9]+", "_", match.group(1).strip().lower()).strip("_")
        raw_value = match.group(2).replace(",", "")
        values[key] = float(raw_value) if "." in raw_value else int(raw_value)
    return values


def parse_time_metrics(path: Path) -> dict[str, float | int]:
    metrics: dict[str, float | int] = {}
    for line in path.read_text().splitlines():
        key, _, value = line.partition("=")
        if not _:
            continue
        metrics[key] = float(value) if key not in {"max_rss_kb", "exit_status"} else int(float(value))
    return metrics


def iter_assertions(value: Any, suite_file: str = "") -> Iterable[tuple[str, dict[str, Any]]]:
    """Yield (suite file, assertion) for Vitest/Jest JSON variants."""

    if isinstance(value, dict):
        current_file = str(value.get("name") or value.get("filepath") or suite_file)
        assertions = value.get("assertionResults")
        if isinstance(assertions, list):
            for assertion in assertions:
                if isinstance(assertion, dict):
                    yield current_file, assertion
        for key in ("testResults", "files", "children"):
            children = value.get(key)
            if isinstance(children, list):
                for child in children:
                    yield from iter_assertions(child, current_file)
    elif isinstance(value, list):
        for child in value:
            yield from iter_assertions(child, suite_file)


def flatten_tests(report: dict[str, Any], csv_path: Path, report_path: Path, log_path: Path) -> dict[str, int]:
    rows: list[dict[str, str | int | float]] = []
    for ordinal, (suite_file, assertion) in enumerate(iter_assertions(report), start=1):
        status = str(assertion.get("status", "unknown"))
        duration = assertion.get("duration")
        duration_ms = "" if duration is None else float(duration)
        title = str(assertion.get("fullName") or assertion.get("name") or assertion.get("title") or "")
        rows.append(
            {
                "test_ordinal": ordinal,
                "suite_file": suite_file,
                "test_name": title,
                "status": status,
                "duration_ms": duration_ms,
                "raw_json_path": str(report_path),
                "raw_log_path": str(log_path),
            }
        )
    with csv_path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=[
            "test_ordinal", "suite_file", "test_name", "status", "duration_ms",
            "raw_json_path", "raw_log_path",
        ])
        writer.writeheader()
        writer.writerows(rows)

    counts = {"total_tests": len(rows), "passed_tests": 0, "failed_tests": 0, "pending_tests": 0}
    for row in rows:
        status = str(row["status"]).lower()
        if status in {"passed", "pass"}:
            counts["passed_tests"] += 1
        elif status in {"skipped", "pending", "todo"}:
            counts["pending_tests"] += 1
        else:
            counts["failed_tests"] += 1
    reported_files = report.get("testResults")
    counts["total_files"] = len(reported_files) if isinstance(reported_files, list) else len({str(row["suite_file"]) for row in rows})
    return counts


def git_revision(repo: Path) -> str:
    completed = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"], text=True, capture_output=True, check=False
    )
    return completed.stdout.strip() if completed.returncode == 0 else "unknown"


def git_status(repo: Path) -> dict[str, Any]:
    """Capture the exact checkout state used by a run, including dirtiness."""

    completed = subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True,
        capture_output=True,
        check=False,
    )
    lines = completed.stdout.splitlines()
    return {
        "revision": git_revision(repo),
        "dirty": bool(lines) or completed.returncode != 0,
        "porcelain": lines,
        "status_return_code": completed.returncode,
    }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def filesystem_metadata(path: Path) -> dict[str, Any]:
    """Record the mounted filesystem containing a run-owned temporary path."""

    completed = subprocess.run(
        ["df", "-P", str(path)], text=True, capture_output=True, check=False
    )
    lines = completed.stdout.splitlines()
    if completed.returncode != 0 or len(lines) < 2:
        return {"path": str(path), "df_return_code": completed.returncode, "df_output": lines}
    fields = lines[-1].split()
    if len(fields) < 6:
        return {"path": str(path), "df_return_code": completed.returncode, "df_output": lines}
    return {
        "device": fields[0],
        "blocks_kb": int(fields[1]),
        "used_kb": int(fields[2]),
        "available_kb": int(fields[3]),
        "use_percent": fields[4],
        "mount": " ".join(fields[5:]),
    }


def prepare_temp_environment(path: Path) -> None:
    """Make Python staging and all descendants use the run-owned temp path."""

    path.mkdir(parents=True, exist_ok=True)
    value = str(path.resolve())
    os.environ.update({"TMPDIR": value, "TMP": value, "TEMP": value})
    tempfile.tempdir = value


def runtime_jar_manifest(root: Path) -> dict[str, Any]:
    """Hash packaged runtime jars using a canonical, order-independent manifest."""

    entries = {
        str(path.relative_to(root)): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }
    canonical = json.dumps(entries, sort_keys=True, separators=(",", ":")).encode()
    return {
        "root": str(root.resolve()),
        "jar_count": len(entries),
        "files": entries,
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
    }


def write_svg(summary_root: Path, svg_path: Path) -> None:
    """Create a dependency-free grouped-bar report from this runner's summaries."""

    summaries = [json.loads(path.read_text()) for path in sorted(summary_root.glob("*/summary.json"))]
    if not summaries:
        return
    width, row_height, left, chart_width = 1000, 110, 220, 700
    height = 90 + row_height * len(summaries)
    max_wall = max(float(item.get("wall_s", 0)) for item in summaries) or 1.0
    max_rss = max(float(item.get("max_rss_kb", 0)) for item in summaries) or 1.0
    colors = {"bypass": "#b35c44", "cold": "#2f6f9f", "warm": "#4f8a5b"}
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             '<style>text{font-family:system-ui,sans-serif;fill:#202124}.small{font-size:12px}.title{font-size:20px;font-weight:600}</style>',
             '<rect width="100%" height="100%" fill="white"/>', '<text x="20" y="32" class="title">Clava-JS suite observations</text>',
             '<text x="20" y="54" class="small">Wall time (blue scale) and peak RSS (green scale)</text>']
    for index, item in enumerate(summaries):
        y = 80 + index * row_height
        mode = str(item.get("mode", "unknown"))
        color = colors.get(mode, "#777")
        wall = float(item.get("wall_s", 0))
        rss = float(item.get("max_rss_kb", 0))
        wall_width = chart_width * wall / max_wall
        rss_width = chart_width * rss / max_rss
        parts.extend([
            f'<text x="20" y="{y + 18}">{mode}</text>',
            f'<rect x="{left}" y="{y + 4}" width="{wall_width:.2f}" height="20" fill="{color}"/>',
            f'<text x="{left + wall_width + 8:.2f}" y="{y + 19}" class="small">{wall:.2f}s</text>',
            f'<rect x="{left}" y="{y + 38}" width="{rss_width:.2f}" height="20" fill="#74a47a"/>',
            f'<text x="{left + rss_width + 8:.2f}" y="{y + 53}" class="small">{rss / 1024:.0f} MiB RSS</text>',
            f'<text x="20" y="{y + 53}" class="small">{item.get("total_tests", "?")} tests, {item.get("passed_tests", "?")} passed</text>',
        ])
    parts.append("</svg>")
    svg_path.write_text("\n".join(parts))


def main() -> int:
    args = parse_args()
    if not args.runtime_root.is_dir():
        raise SystemExit(f"runtime distribution does not exist: {args.runtime_root}")
    if args.local_release_dir is not None and not args.local_release_dir.is_dir():
        raise SystemExit(f"local release directory does not exist: {args.local_release_dir}")
    if args.mode == "warm" and args.cache_root is None:
        raise SystemExit("--mode warm requires --cache-root from a previous cold run")

    output_root = args.output_root.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    run_name = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ") + f"-{args.mode}"
    run_dir = output_root / run_name
    run_dir.mkdir()
    temp_root = run_dir / "tmp"
    prepare_temp_environment(temp_root)
    temp_metadata = filesystem_metadata(temp_root)

    if args.cache_root is None:
        xdg_root = Path(tempfile.mkdtemp(prefix="xdg-", dir=output_root))
    else:
        xdg_root = args.cache_root.resolve()
        if args.mode in {"bypass", "cold"} and xdg_root.exists():
            raise SystemExit(
                f"{args.mode} mode requires a new cache root, already exists: {xdg_root}"
            )
        xdg_root.mkdir(parents=True, exist_ok=True)
    cache = cache_dir(xdg_root)
    cache.parent.mkdir(parents=True, exist_ok=True)
    if args.mode == "cold":
        cache.mkdir(parents=True, exist_ok=True)
    elif args.mode == "warm" and not cache.is_dir():
        raise SystemExit(f"Warm cache directory does not exist: {cache}")

    runtime_root = run_dir / "runtime"
    stage_java_runtime(args.runtime_root.resolve(), runtime_root, args.local_release_dir)
    runtime_manifest = runtime_jar_manifest(runtime_root)
    release = release_metadata(runtime_root, args.local_release_dir)
    clava_git = git_status(CLAVA_ROOT)

    stats_before = ccache("--show-stats", cache) if cache.is_dir() else ""
    ccache("--zero-stats", cache)
    report_path = run_dir / "vitest.json"
    log_path = run_dir / "vitest.log"
    time_path = run_dir / "time.txt"
    command = [
        "/usr/bin/time", "-f", "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x",
        "-o", str(time_path), "--", "npm", "exec", "--workspace", "@specs-feup/clava", "--",
        "vitest", "run", "--config", str(EXPERIMENT_ROOT / "suite" / "vitest.suite.config.ts"),
        "--reporter=json", "--outputFile", str(report_path),
        "--testNamePattern", JS_TEST_FILTER, *args.vitest_arg,
    ]
    forbidden_filters = {"-t", "--testNamePattern", "--testNamePattern=", "--exclude", "--include"}
    if any(argument in forbidden_filters or argument.startswith("--testNamePattern=") for argument in args.vitest_arg):
        raise SystemExit("--vitest-arg cannot change the fixed Clava-JS test selection")
    environment = os.environ.copy()
    java_options = environment.get("JAVA_TOOL_OPTIONS", "")
    if re.search(r"(?<!\S)-Dclava\.astWire=\S+", java_options) or any(
        key in environment for key in ("AST_WIRE_FLAT", "AST_WIRE_DENSE_TEXT")
    ):
        raise SystemExit("remove obsolete AST wire-selection overrides before running the eager suite")
    environment["JAVA_TOOL_OPTIONS"] = java_options + " -Djava.io.tmpdir=" + str(temp_root) + " -Dclava.astWireMetrics=true"
    environment["XDG_CACHE_HOME"] = str(xdg_root)
    environment["CLAVA_SUITE_JAR_PATH"] = str(runtime_root)
    if args.mode == "bypass":
        environment["CCACHE_DISABLE"] = "true"
    else:
        environment.pop("CCACHE_DISABLE", None)
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=CLAVA_JS_ROOT, env=environment, stdout=log, stderr=subprocess.STDOUT)
    wall_s = time.perf_counter() - started

    stats_after = ccache("--show-stats", cache)
    stats = parse_ccache_stats(stats_after)
    report = json.loads(report_path.read_text()) if report_path.is_file() else {}
    test_counts = flatten_tests(report, run_dir / "per_test_timings.csv", report_path, log_path)
    test_counts_valid = all(test_counts.get(key) == value for key, value in EXPECTED_TEST_COUNTS.items())
    time_metrics = parse_time_metrics(time_path) if time_path.is_file() else {}
    parse_metrics = []
    for match in re.finditer(r'CLAVA_AST_METRIC (\{[^\n]+\})', log_path.read_text(errors="replace")):
        try:
            parse_metrics.append(json.loads(match.group(1)))
        except json.JSONDecodeError:
            pass
    (run_dir / "parse-metrics.json").write_text(json.dumps(parse_metrics, indent=2) + "\n")
    identity_fields = ("format", "wire_schema_version", "wire_schema_hash", "dumper_sha256",
                       "flatbuffers_version", "flatbuffers_commit")
    runtime_identities = [
        {key: metric[key] for key in identity_fields if key in metric}
        for metric in parse_metrics
        if any(key in metric for key in identity_fields)
    ]
    if runtime_identities:
        identities = {json.dumps(identity, sort_keys=True) for identity in runtime_identities}
        if len(identities) != 1:
            raise SystemExit("parser runs used inconsistent selected release identities")
        runtime_identity = runtime_identities[0]
        if runtime_identity.get("format", "flatbuffers-v2") != "flatbuffers-v2":
            raise SystemExit(f"production parser reported non-eager protocol: {runtime_identity}")
        runtime_schema_hash = runtime_identity.get("wire_schema_hash")
        runtime_tool_hash = runtime_identity.get("dumper_sha256")
        if release.get("schema_sha256") and runtime_schema_hash \
                and release["schema_sha256"] != runtime_schema_hash:
            raise SystemExit("runtime schema hash differs from the selected release manifest")
        if release.get("tool_sha256") and runtime_tool_hash \
                and release["tool_sha256"] != runtime_tool_hash:
            raise SystemExit("runtime native tool hash differs from the selected local release")
        release["wire_protocol"] = "flatbuffers-eager"
        release["schema_sha256"] = runtime_schema_hash or release.get("schema_sha256")
        release["tool_sha256"] = runtime_tool_hash or release.get("tool_sha256")
        release.update(runtime_identity)
    if not release.get("tool_sha256"):
        raise SystemExit("parser metrics did not identify the native dumper executable used")
    if not release.get("schema_sha256"):
        raise SystemExit("parser metrics did not identify the wire schema used")
    summary = {
        "mode": args.mode,
        "wire_protocol": "flatbuffers-eager",
        "js_test_filter": JS_TEST_FILTER,
        "expected_test_counts": EXPECTED_TEST_COUNTS,
        "test_counts_valid": test_counts_valid,
        "parse_count": len(parse_metrics),
        "parse_aggregate": {key: sum(m.get(key,0) for m in parse_metrics) for key in
            ["native_ms","read_ms","tu_ms","dump_bytes","nodes","deferred","materialized_at_tu"]},
        "return_code": process.returncode,
        "wall_s": wall_s,
        **time_metrics,
        **test_counts,
        "cache_root": str(xdg_root),
        "ccache_dir": str(cache),
        "ccache_stats": stats,
        "ccache_stats_before": parse_ccache_stats(stats_before),
        "clava_revision": git_revision(CLAVA_ROOT),
        "runtime_source": str(args.runtime_root.resolve()),
        "release": release,
        "runtime_jar_manifest": runtime_manifest,
        "host_state": host_state(),
        "temp_root": str(temp_root),
        "temp_filesystem": temp_metadata,
        "git": {"clava": clava_git},
        "vitest_success": report.get("success"),
        "vitest_total_test_suites": report.get("numTotalTestSuites"),
        "vitest_passed_test_suites": report.get("numPassedTestSuites"),
        "vitest_failed_test_suites": report.get("numFailedTestSuites"),
        "ccache_cacheable_calls": stats.get("cacheable_calls", 0),
        "ccache_direct_hits": stats.get("direct", 0),
        "ccache_misses": stats.get("misses", 0),
        "ccache_uncacheable_calls": stats.get("uncacheable_calls", 0),
        "command": command,
        "environment": {
            key: environment[key]
            for key in ("TMPDIR", "TMP", "TEMP", "XDG_CACHE_HOME", "CLAVA_SUITE_JAR_PATH",
                        "CLAVA_SUITE_SOURCE_ROOT", "CCACHE_DISABLE", "JAVA_TOOL_OPTIONS")
            if key in environment
        },
    }
    (run_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    (run_dir / "ccache.stats").write_text(stats_after)
    (run_dir / "ccache.stats.before").write_text(stats_before)
    write_svg(output_root, output_root / "suite-observations.svg")
    print(json.dumps({"run_dir": str(run_dir), **summary}, indent=2))
    return 0 if process.returncode == 0 and summary["test_counts_valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
