#!/usr/bin/env python3
"""Measure per-parse Text/Protobuf runs with warm AST-dump ccache.

Every run uses the same Clava checkout, native dumper, and staged Java runtime.
The raw parse CSV records one event per completed ClangAstDumper.parsePrivate
invocation. The run CSV separately records the full suite command wall time,
including any heap-logging/System.gc activity outside parsePrivate.
"""

from __future__ import annotations

import argparse
from collections import Counter
import csv
import datetime as dt
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import statistics
import subprocess
import sys
import time
from typing import Any
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True

import run_dual_ab as ab


SCRIPT_ROOT = Path(__file__).resolve().parent
SUITES = ("clava-js", "java")
POLICIES = ("normal", "disabled")
STAGES = ab.STAGES
CCACHE_NAMESPACE = "clang-dumper-protobuf-ccache-v1"
MIN_WARM_CACHE_HIT_RATE = 0.90
PARSE_BOUNDARY = (
    "ClangAstDumper.parsePrivate entry through TranslationUnit construction; "
    "excludes caller-side heap logging/System.gc"
)
DISABLE_EXPLICIT_GC = {"normal": False, "disabled": True}
GC_VM_FLAGS = {"normal": "-XX:-DisableExplicitGC", "disabled": "-XX:+DisableExplicitGC"}

PARSE_FIELDS = (
    "run_id", "pair_group_id", "pair_id", "suite", "phase", "measured", "cache_mode",
    "gc_policy", "protocol", "repeat", "invocation_index", "source_identity", "parse_pair_key",
    "pair_available", "identity_quality",
    "test_id", "tester_invocation", "parse_pass", "resource_key", "source_path", "parse_id",
    "source_content_sha256", "parse_args_sha256", "parse_args_original_sha256",
    "parse_elapsed_ms", "parse_timing_boundary",
    "native_ms", "ccache_invoke_ms", "read_ms", "decode_ms", "record_ms", "reference_ms", "ast_ms",
    "frames", "records", "nodes", "files", "encoded_bytes", "dump_bytes", "compressed",
    "cache_enabled", "ccache_cache_dir", "ccache_observation_scope", "ccache_disabled",
    "explicit_gc_disabled", "ccache_cacheable_calls", "ccache_hits", "ccache_misses",
    "ccache_hit_rate", "ccache_uncacheable_calls", "ccache_adapter_events",
    "ccache_event_counter_match", "driver_elapsed_s", "run_valid", "event_valid",
    "validity_reason",
)
RUN_FIELDS = (
    "run_id", "pair_group_id", "pair_id", "suite", "phase", "measured", "cache_mode", "gc_policy",
    "protocol", "repeat", "return_code", "elapsed_s", "parse_event_count", "expected_event_count",
    "ccache_disabled", "ccache_cacheable_calls", "ccache_hits", "ccache_misses", "ccache_hit_rate",
    "ccache_uncacheable_calls", "ccache_adapter_events", "ccache_event_counter_match", "cache_dir",
    "test_total", "test_passed", "test_failed", "test_skipped",
    "gc_policy_verified", "identity_multiset_match", "source_content_match", "parse_args_match",
    "source_args_match", "cache_distribution_match", "valid",
    "validity_reason", "run_dir",
)
EVENT_PREFIXES = ("PROTOBUF_METRIC ", "CLAVA_AST_METRIC ")
WOVEN_WORKSPACE_COMPONENT = re.compile(
    r"^__clava_woven_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_(?P<suffix>[^/]+)$",
    re.IGNORECASE,
)
JUNIT_TEMP_COMPONENT = re.compile(r"^junit[0-9]+$")


def output_root_for(path: Path | None) -> Path:
    if path is not None:
        root = path.resolve()
    else:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        root = (SCRIPT_ROOT / "results" / f"per-parse-ab-{stamp}").resolve()
    if root.exists():
        raise SystemExit(f"refusing to reuse output root: {root}")
    root.mkdir(parents=True)
    return root


def run_id(suite: str, policy: str, protocol: str, phase: str, repeat: int) -> str:
    if phase == "seed":
        return f"seed-{suite}-{policy}-{protocol}"
    return f"{suite}-{policy}-{protocol}-r{repeat:02d}"


def pair_group_id(policy: str, repeat: int, phase: str = "measured") -> str:
    return f"{phase}-warm-gc-{policy}-r{repeat:02d}"


def pair_id(suite: str, policy: str, repeat: int, phase: str = "measured") -> str:
    return f"{phase}-{suite}-warm-gc-{policy}-r{repeat:02d}"


def measured_order(repeat: int) -> tuple[tuple[dict[str, Any], str], ...]:
    """Alternate protocol and suite order so neither wire always runs first."""
    stages = STAGES if repeat % 2 else STAGES[::-1]
    suites = SUITES if repeat % 4 in (1, 2) else SUITES[::-1]
    return tuple((stage, suite) for stage in stages for suite in suites)


def parse_metric_events(text: str) -> list[dict[str, Any]]:
    events = []
    for line in text.splitlines():
        for prefix in EVENT_PREFIXES:
            position = line.find(prefix)
            if position < 0:
                continue
            try:
                event = json.loads(line[position + len(prefix):])
            except json.JSONDecodeError:
                break
            if isinstance(event, dict):
                events.append(event)
            break
    return events


def java_events(log_path: Path, junit_root: Path) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    if junit_root.is_dir():
        for result_xml in sorted(junit_root.glob("*.xml")):
            try:
                root = ET.parse(result_xml).getroot()
            except ET.ParseError:
                continue
            classname = root.attrib.get("name", "")
            file_events: list[dict[str, Any]] = []
            for testcase in root.findall("testcase"):
                test_id = f"{testcase.attrib.get('classname', classname)}#{testcase.attrib.get('name', '')}"
                for output in testcase.findall("system-out") + testcase.findall("system-err"):
                    for event in parse_metric_events(output.text or ""):
                        event["test_case"] = test_id
                        file_events.append(event)
            if file_events:
                events.extend(file_events)
                continue
            # Gradle/JUnit versions may place output at the suite root instead.
            for output in root.findall("system-out") + root.findall("system-err"):
                file_events.extend(parse_metric_events(output.text or ""))
            events.extend(file_events)
    return events or parse_metric_events(log_path.read_text(errors="replace") if log_path.is_file() else "")


def source_label(event: dict[str, Any], suite: str, clava_root: Path, js_workspace: Path,
                 temp_root: Path | None = None) -> tuple[str, str]:
    """Return a stable resource-like path and a best-effort parse pass."""
    registered_resource = event.get("resource_key")
    registered_pass = event.get("parse_pass")
    if isinstance(registered_resource, str) and registered_resource:
        return registered_resource.replace("\\", "/"), str(registered_pass or "registered")

    raw_path = str(event.get("source_path") or "")
    path = Path(raw_path)
    if temp_root is not None:
        try:
            # Protocol-specific run roots must be removed before the broader
            # Clava checkout-relative case below. Clava-JS creates randomized
            # __clava_woven_<UUID>_<owner> workspaces inside these roots.
            relative = path.resolve().relative_to(temp_root.resolve())
            parts = list(relative.parts)
            if parts:
                match = WOVEN_WORKSPACE_COMPONENT.fullmatch(parts[0])
                if match:
                    parts[0] = f"__clava_woven_<uuid>_{match.group('suffix')}"
                elif JUNIT_TEMP_COMPONENT.fullmatch(parts[0]):
                    parts[0] = "junit<temp>"
            return (Path("generated", *parts)).as_posix(), "source"
        except (OSError, ValueError):
            pass
    try:
        return path.resolve().relative_to(js_workspace.resolve()).as_posix(), "source"
    except (OSError, ValueError):
        pass
    try:
        # Clava-JS can parse resources owned by the enclosing Clava checkout.
        # Keep their checkout-relative identity instead of collapsing to basename.
        return path.resolve().relative_to(js_workspace.resolve().parent).as_posix(), "source"
    except (OSError, ValueError):
        pass
    try:
        return path.resolve().relative_to(clava_root.resolve()).as_posix(), "source"
    except (OSError, ValueError):
        pass
    parts = list(path.parts)
    for index, component in enumerate(parts):
        if component.startswith("temp-clang-ast-"):
            relative = parts[index + 1:]
            parse_pass = "original"
            if relative and relative[0] in {"outputFirst", "outputSecond"}:
                parse_pass = "roundtrip" if relative[0] == "outputFirst" else "roundtrip-2"
                relative = relative[1:]
            return Path(*relative).as_posix() if relative else path.name, parse_pass

    for marker in ("test-resources", "test_resources", "fixtures"):
        if marker in parts:
            index = parts.index(marker)
            return Path(*parts[index + 1:]).as_posix(), "source"
    # Unknown absolute locations are intentionally not collapsed to a basename.
    # If they differ across wire runs, pairing must fail visibly.
    return "unresolved/" + path.resolve().as_posix().lstrip("/"), "fallback"


def identify_events(events: list[dict[str, Any]], suite: str, clava_root: Path,
                    js_workspace: Path, temp_root: Path | None = None) -> list[dict[str, Any]]:
    prepared: list[dict[str, Any]] = []
    seen: Counter[str] = Counter()
    totals: Counter[str] = Counter()
    for event in events:
        resource, fallback_pass = source_label(event, suite, clava_root, js_workspace, temp_root)
        test_id = event.get("test_id") or event.get("test_case")
        tester_invocation = event.get("tester_invocation")
        parse_pass = event.get("parse_pass") or fallback_pass
        parse_id = str(event.get("parse_id") or "")
        registered = bool(event.get("test_id") and event.get("tester_invocation") is not None
                          and event.get("parse_pass") and event.get("resource_key"))
        identity_base = json.dumps(
            [test_id, tester_invocation, parse_pass, resource, parse_id],
            ensure_ascii=False, separators=(",", ":"),
        )
        source_sha = str(event.get("source_content_sha256") or "")
        args_sha = str(event.get("parse_args_sha256") or "")
        parse_pair_key = json.dumps(
            [test_id, tester_invocation, parse_pass, resource, parse_id, source_sha, args_sha],
            ensure_ascii=False, separators=(",", ":"),
        )
        occurrence = seen[parse_pair_key]
        seen[parse_pair_key] += 1
        totals[parse_pair_key] += 1
        row = dict(event)
        row.update({
            "source_identity": identity_base,
            "identity_base": identity_base,
            "parse_pair_key": parse_pair_key,
            "invocation_index": occurrence,
            "identity_quality": "registered" if registered else "fallback",
            "pair_available": bool(source_sha and args_sha),
            "resource_key": resource,
            "test_id": test_id,
            "tester_invocation": tester_invocation,
            "parse_pass": parse_pass,
        })
        prepared.append(row)
    for row in prepared:
        if totals[row["parse_pair_key"]] > 1:
            row["identity_quality"] = "ambiguous_duplicate"
            row["pair_available"] = False
    return prepared


def cache_directories(temp_root: Path) -> list[Path]:
    """Find the adapter's actual namespace below this run's isolated temp root."""
    if not temp_root.is_dir():
        return []
    return sorted(path.resolve() for path in temp_root.rglob(CCACHE_NAMESPACE) if path.is_dir())


def reported_cache_directory(events: list[dict[str, Any]], temp_root: Path) -> Path | None:
    reported = {str(event.get("ccache_cache_dir")) for event in events if event.get("ccache_cache_dir")}
    if len(reported) != 1:
        return None
    cache_dir = Path(next(iter(reported))).resolve()
    try:
        cache_dir.relative_to(temp_root.resolve())
    except ValueError:
        return None
    if cache_dir.name != CCACHE_NAMESPACE or cache_directories(temp_root) != [cache_dir]:
        return None
    if any(not event_cache_path_matches(event, cache_dir) for event in events):
        return None
    return cache_dir


def event_cache_path_matches(event: dict[str, Any], cache_dir: Path | None) -> bool:
    event_cache_dir = event.get("ccache_cache_dir")
    if not event_cache_dir:
        return event.get("cache_enabled") is False
    return (
        cache_dir is not None
        and Path(str(event_cache_dir)).resolve() == cache_dir
        and event.get("cache_enabled") is True
    )


def prepare_environment(stage: dict[str, Any], suite: str, policy: str, native_tool: Path,
                        temp_root: Path, debug_args: bool = False) -> dict[str, str]:
    temp_root.mkdir(parents=True, exist_ok=True)
    environment = os.environ.copy()
    environment.pop("CCACHE_DISABLE", None)
    environment.update({
        "TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(temp_root), "CLANG_DUMPER_TOOL": str(native_tool.resolve()),
    })
    retained = [
        option for option in shlex.split(environment.get("JAVA_TOOL_OPTIONS", ""))
        if not option.startswith((
            "-Djava.io.tmpdir=", "-Dclava.astAbWire=", "-Dclava.astWireMetrics=",
            "-Dclava.astWireMetrics.debugArgs=",
            "-Dclava.astTestGcPolicy=", "-XX:+DisableExplicitGC", "-XX:-DisableExplicitGC",
        ))
    ]
    retained.extend((
        f"-Djava.io.tmpdir={temp_root}", f"-Dclava.astAbWire={stage['wire']}",
        "-Dclava.astWireMetrics=true", f"-Dclava.astTestGcPolicy={policy}",
    ))
    if debug_args:
        retained.append("-Dclava.astWireMetrics.debugArgs=true")
    # Java-suite worker flags are set by java-suite.init.gradle. Clava-JS starts
    # its parser JVM from Node, so that JVM inherits the explicit policy here.
    if suite == "clava-js":
        retained.append(GC_VM_FLAGS[policy])
    environment["JAVA_TOOL_OPTIONS"] = shlex.join(retained)
    return environment


def clear_ccache_stats(cache_dir: Path) -> None:
    cache_dir.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        ["ccache", "--zero-stats"],
        env={**os.environ, "CCACHE_DIR": str(cache_dir), "LC_ALL": "C"},
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"could not reset ccache stats at {cache_dir}: {result.stderr.strip()}")


def ccache_stats(cache_dir: Path) -> dict[str, int]:
    return ab.base.read_ccache_stats(cache_dir) or {}


def execute_suite(stage: dict[str, Any], suite: str, policy: str, phase: str, repeat: int,
                  clava_root: Path, native_tool: Path, js_workspace: Path, runtime: Path,
                  output_root: Path) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    measured = phase == "measured"
    rid = run_id(suite, policy, stage["wire"], phase, repeat)
    group_id = pair_group_id(policy, repeat, phase)
    pid = pair_id(suite, policy, repeat, phase)
    run_dir = output_root / "runs" / suite / policy / stage["wire"] / (f"r{repeat:02d}" if measured else "seed")
    run_dir.mkdir(parents=True)
    temp_root = output_root / "temp" / suite / policy / stage["wire"]
    environment = prepare_environment(stage, suite, policy, native_tool, temp_root, debug_args=phase == "seed")
    prior_cache_dirs = cache_directories(temp_root)
    if len(prior_cache_dirs) > 1:
        raise RuntimeError(f"expected at most one ccache namespace below {temp_root}, found {prior_cache_dirs}")
    missing_warm_seed = measured and not prior_cache_dirs
    if prior_cache_dirs:
        clear_ccache_stats(prior_cache_dirs[0])

    result_dir = clava_root / "ClangAstParser" / "build" / "test-results" / "test"
    if suite == "java" and result_dir.exists():
        shutil.rmtree(result_dir)

    if suite == "java":
        command = [
            "gradle", "--no-daemon", "--offline", f"-PclangDumperRoot={native_tool.parent.parent}",
            f"-PclavaAbGcPolicy={policy}", "-p", str(clava_root / "ClangAstParser"),
            "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"), "--rerun-tasks", "test",
        ]
        log_path = run_dir / "run.log"
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=clava_root, env=environment,
                                     stdout=log, stderr=subprocess.STDOUT, check=False)
        elapsed = time.perf_counter() - started
        counts = ab.base.java_counts(result_dir)
        events = java_events(log_path, result_dir)
        expected = ab.EXPECTED_SUITE_COUNTS["java"]
        counts_match = all(counts.get(name) == value for name, value in expected.items())
        log_text = log_path.read_text(errors="replace")
        worker_flag = GC_VM_FLAGS[policy]
        gc_worker_verified = (
            f"CLAVA_AB_TEST_GC_POLICY={policy} JVM_ARGS=" in log_text and worker_flag in log_text
        )
    else:
        config_path = run_dir / "vitest.config.ts"
        ab.write_js_config(config_path, js_workspace, runtime)
        report_path = run_dir / "vitest.json"
        log_path = run_dir / "run.log"
        command = [
            "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
            "--config", str(config_path), "--reporter=json", "--outputFile", str(report_path),
            "-t", ab.base.JS_TEST_FILTER,
        ]
        environment.update({"CLAVA_SUITE_JAR_PATH": str(runtime), "XDG_CACHE_HOME": str(temp_root)})
        started = time.perf_counter()
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=js_workspace, env=environment,
                                     stdout=log, stderr=subprocess.STDOUT, check=False)
        elapsed = time.perf_counter() - started
        report = json.loads(report_path.read_text()) if report_path.is_file() else {}
        counts = ab.base.js_counts(report)
        events = parse_metric_events(log_path.read_text(errors="replace"))
        expected = ab.EXPECTED_SUITE_COUNTS["clava-js"]
        counts_match = all(counts.get(name) == value for name, value in expected.items())
        gc_worker_verified = True  # Per-event HotSpot option is the authority for this suite.

    events = identify_events(events, suite, clava_root, js_workspace, temp_root)
    resolved_cache_dir = reported_cache_directory(events, temp_root)
    stats = ccache_stats(resolved_cache_dir) if resolved_cache_dir is not None else {}
    expected_events = ab.EXPECTED_METRIC_EVENTS[suite]
    event_count_match = len(events) == expected_events
    event_formats_match = all(event.get("format") == stage["wire"] for event in events)
    event_cache_match = (
        all(event.get("ccache_disabled") is False for event in events)
        and all(isinstance(event.get("cache_enabled"), bool) for event in events)
        and any(event.get("cache_enabled") is True for event in events)
    )
    event_cache_path_match = resolved_cache_dir is not None
    event_gc_match = all(event.get("explicit_gc_disabled") is DISABLE_EXPLICIT_GC[policy] for event in events)
    event_times_valid = all(
        isinstance(event.get("parse_elapsed_ms"), (int, float)) and event["parse_elapsed_ms"] > 0
        for event in events
    )
    event_identity_valid = all(
        event.get("source_content_sha256") and event.get("parse_args_sha256") for event in events
    )
    calls = int(stats.get("cacheable_calls", 0))
    hits = int(stats.get("hits", 0))
    misses = int(stats.get("misses", 0))
    uncacheable = int(stats.get("uncacheable_calls", 0))
    adapter_events = sum(bool(event.get("ccache_cache_dir")) for event in events)
    event_counter_match = calls + uncacheable == adapter_events
    hit_rate = hits / (hits + misses) if hits + misses else 0.0
    if measured:
        cache_valid = (
            calls > 0 and hits > 0 and hit_rate >= MIN_WARM_CACHE_HIT_RATE
            and not missing_warm_seed and "stats_error" not in stats
        )
    else:
        cache_valid = calls > 0 and misses > 0 and "stats_error" not in stats
    local_valid = (
        process.returncode == 0 and counts_match and event_count_match and event_formats_match
        and event_cache_match and event_cache_path_match and event_gc_match and event_times_valid and event_identity_valid
        and event_counter_match and cache_valid and gc_worker_verified
    )
    reasons = []
    if process.returncode != 0:
        reasons.append(f"command_exit_{process.returncode}")
    if not counts_match:
        reasons.append("suite_test_counts_mismatch")
    if not event_count_match:
        reasons.append(f"parse_event_count_{len(events)}_expected_{expected_events}")
    if not event_formats_match:
        reasons.append("wire_format_mismatch")
    if not event_cache_match:
        reasons.append("ccache_disabled_or_unknown_in_event")
    if not event_cache_path_match:
        reasons.append("actual_ccache_directory_missing_or_outside_isolated_temp_root")
    if not event_counter_match:
        reasons.append(f"ccache_event_counter_mismatch_{adapter_events}_events_{calls + uncacheable}_calls")
    if missing_warm_seed:
        reasons.append("warm_cache_seed_directory_missing")
    if not event_gc_match:
        reasons.append("effective_explicit_gc_policy_mismatch")
    if not event_times_valid:
        reasons.append("missing_or_invalid_parse_elapsed")
    if not event_identity_valid:
        reasons.append("missing_source_or_compile_identity_digest")
    if not cache_valid:
        reasons.append("ccache_counters_did_not_verify_expected_warm_state")
        if measured and hit_rate < MIN_WARM_CACHE_HIT_RATE:
            reasons.append(f"warm_ccache_hit_rate_below_{MIN_WARM_CACHE_HIT_RATE:.0%}")
    if not gc_worker_verified:
        reasons.append("Java_test_worker_gc_policy_unverified")

    run = {
        "run_id": rid, "pair_group_id": group_id, "pair_id": pid, "suite": suite,
        "phase": phase, "measured": measured, "cache_mode": "warm", "gc_policy": policy,
        "protocol": stage["wire"], "repeat": repeat, "return_code": process.returncode,
        "elapsed_s": elapsed, "parse_event_count": len(events), "expected_event_count": expected_events,
        "ccache_disabled": False, "ccache_cacheable_calls": calls, "ccache_hits": hits,
        "ccache_misses": misses, "ccache_uncacheable_calls": uncacheable,
        "ccache_hit_rate": hit_rate, "ccache_adapter_events": adapter_events,
        "ccache_event_counter_match": event_counter_match,
        "cache_dir": str(resolved_cache_dir) if resolved_cache_dir is not None else "",
        "test_total": counts.get("total_tests"), "test_passed": counts.get("passed_tests"),
        "test_failed": counts.get("failed_tests"), "test_skipped": counts.get("skipped_tests"),
        "gc_policy_verified": gc_worker_verified and event_gc_match,
        "identity_multiset_match": None, "source_content_match": None, "parse_args_match": None,
        "source_args_match": None,
        "cache_distribution_match": None, "valid": local_valid,
        "validity_reason": "valid" if local_valid else ";".join(reasons), "run_dir": str(run_dir),
        "command": command, "metrics": events,
    }
    parse_rows = []
    for event in events:
        event_cache_path_valid = (
            event_cache_path_matches(event, resolved_cache_dir)
        )
        row_valid = (
            event.get("format") == stage["wire"] and event.get("ccache_disabled") is False
            and event_cache_path_valid
            and event.get("explicit_gc_disabled") is DISABLE_EXPLICIT_GC[policy]
            and isinstance(event.get("parse_elapsed_ms"), (int, float))
            and event["parse_elapsed_ms"] > 0 and bool(event.get("source_content_sha256"))
            and bool(event.get("parse_args_sha256"))
        )
        parse_rows.append({
            "run_id": rid, "pair_group_id": group_id, "pair_id": pid, "suite": suite,
            "phase": phase, "measured": measured, "cache_mode": "warm", "gc_policy": policy,
            "protocol": stage["wire"], "repeat": repeat, "invocation_index": event["invocation_index"],
            "source_identity": event["source_identity"], "identity_quality": event["identity_quality"],
            "parse_pair_key": event["parse_pair_key"], "pair_available": event["pair_available"],
            "test_id": event.get("test_id"), "tester_invocation": event.get("tester_invocation"),
            "parse_pass": event.get("parse_pass"), "resource_key": event.get("resource_key"),
            "source_path": event.get("source_path"), "parse_id": event.get("parse_id"),
            "source_content_sha256": event.get("source_content_sha256"),
            "parse_args_sha256": event.get("parse_args_sha256"),
            "parse_args_original_sha256": event.get("parse_args_original_sha256"),
            "parse_elapsed_ms": event.get("parse_elapsed_ms"), "parse_timing_boundary": PARSE_BOUNDARY,
            "native_ms": event.get("native_ms"), "ccache_invoke_ms": event.get("ccache_invoke_ms"),
            "read_ms": event.get("read_ms"), "decode_ms": event.get("decode_ms"),
            "record_ms": event.get("record_ms"), "reference_ms": event.get("reference_ms"),
            "ast_ms": event.get("ast_ms"), "frames": event.get("frames"),
            "records": event.get("records"), "nodes": event.get("nodes"), "files": event.get("files"),
            "encoded_bytes": event.get("encoded_bytes"), "dump_bytes": event.get("dump_bytes"),
            "compressed": event.get("compressed"), "cache_enabled": event.get("cache_enabled"),
            "ccache_cache_dir": event.get("ccache_cache_dir"),
            "ccache_observation_scope": "whole-suite-run",
            "ccache_disabled": event.get("ccache_disabled"),
            "explicit_gc_disabled": event.get("explicit_gc_disabled"),
            "ccache_cacheable_calls": calls, "ccache_hits": hits, "ccache_misses": misses,
            "ccache_hit_rate": hit_rate, "ccache_adapter_events": adapter_events,
            "ccache_event_counter_match": event_counter_match,
            "ccache_uncacheable_calls": uncacheable, "driver_elapsed_s": elapsed,
            "run_valid": local_valid, "event_valid": row_valid,
            "validity_reason": "valid" if row_valid else "parse_event_validation_failed",
        })
    return run, parse_rows


def event_fingerprint(events: list[dict[str, Any]]) -> tuple[
    Counter[str], Counter[tuple[str, str]], Counter[tuple[str, str]], Counter[tuple[str, str, str]]
]:
    counts: Counter[str] = Counter()
    source_digests: Counter[tuple[str, str]] = Counter()
    argument_digests: Counter[tuple[str, str]] = Counter()
    source_argument_pairs: Counter[tuple[str, str, str]] = Counter()
    for event in events:
        identity = str(event.get("identity_base") or event["source_identity"])
        source_sha = str(event.get("source_content_sha256") or "")
        args_sha = str(event.get("parse_args_sha256") or "")
        counts[identity] += 1
        source_digests[(identity, source_sha)] += 1
        argument_digests[(identity, args_sha)] += 1
        source_argument_pairs[(identity, source_sha, args_sha)] += 1
    return counts, source_digests, argument_digests, source_argument_pairs


def validate_wire_pair(text_run: dict[str, Any], text_rows: list[dict[str, Any]],
                       proto_run: dict[str, Any], proto_rows: list[dict[str, Any]]) -> bool:
    text_counts, text_sources, text_arguments, text_source_args = event_fingerprint(text_run["metrics"])
    proto_counts, proto_sources, proto_arguments, proto_source_args = event_fingerprint(proto_run["metrics"])
    identity_match = text_counts == proto_counts
    source_match = text_sources == proto_sources
    arguments_match = text_arguments == proto_arguments
    source_args_match = text_source_args == proto_source_args
    cache_fields = (
        "ccache_cacheable_calls", "ccache_hits", "ccache_misses", "ccache_uncacheable_calls",
        "ccache_adapter_events", "ccache_event_counter_match",
    )
    cache_match = (
        text_run.get("cache_dir") != proto_run.get("cache_dir")
        and all(text_run.get(field) == proto_run.get(field) for field in cache_fields)
    )
    pair_match = identity_match and source_match and arguments_match and source_args_match and cache_match
    text_run["identity_multiset_match"] = identity_match
    proto_run["identity_multiset_match"] = identity_match
    text_run["source_content_match"] = source_match
    proto_run["source_content_match"] = source_match
    text_run["parse_args_match"] = arguments_match
    proto_run["parse_args_match"] = arguments_match
    text_run["source_args_match"] = source_args_match
    proto_run["source_args_match"] = source_args_match
    text_run["cache_distribution_match"] = cache_match
    proto_run["cache_distribution_match"] = cache_match
    if not pair_match:
        reasons = []
        if not identity_match:
            reasons.append("paired_parse_identity_multiset_mismatch")
        if not source_match:
            reasons.append("paired_source_content_digest_mismatch")
        if not arguments_match:
            reasons.append("paired_compile_arguments_digest_mismatch")
        if not source_args_match:
            reasons.append("paired_source_and_compile_arguments_association_mismatch")
        if not cache_match:
            reasons.append("paired_ccache_hit_miss_distribution_or_isolation_mismatch")
        reason = ";".join(reasons)
        for run in (text_run, proto_run):
            run["valid"] = False
            run["validity_reason"] = ";".join(filter(
                None, (run["validity_reason"] if run["validity_reason"] != "valid" else "", reason),
            ))
        for row in text_rows + proto_rows:
            row["run_valid"] = False
            row["validity_reason"] = reason if row["validity_reason"] == "valid" else row["validity_reason"] + ";" + reason
    return pair_match


def validate_seed_pairs(policies: tuple[str, ...], suites: tuple[str, ...],
                        by_run_id: dict[str, dict[str, Any]],
                        parse_rows_by_run: dict[str, list[dict[str, Any]]]) -> bool:
    """Validate every seed pair so one mismatch does not hide later diagnostics."""
    all_pairs_match = True
    for policy in policies:
        for suite in suites:
            text = by_run_id[run_id(suite, policy, "text", "seed", 0)]
            proto = by_run_id[run_id(suite, policy, "protobuf", "seed", 0)]
            pair_match = validate_wire_pair(
                text, parse_rows_by_run[text["run_id"]],
                proto, parse_rows_by_run[proto["run_id"]],
            )
            print(f"seed pair {suite} {policy}: valid={pair_match}, "
                  f"identity={text.get('identity_multiset_match')}, "
                  f"source={text.get('source_content_match')}, "
                  f"arguments={text.get('parse_args_match')}, "
                  f"source+arguments={text.get('source_args_match')}, "
                  f"cache={text.get('cache_distribution_match')}", flush=True)
            all_pairs_match = pair_match and all_pairs_match
    return all_pairs_match


def write_csv(path: Path, fields: tuple[str, ...], rows: list[dict[str, Any]]) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    with temporary.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
    temporary.replace(path)


def write_outputs(output_root: Path, runs: list[dict[str, Any]], parses: list[dict[str, Any]],
                  complete: bool = False) -> None:
    write_csv(output_root / "runs.csv", RUN_FIELDS, runs)
    write_csv(output_root / "parses.csv", PARSE_FIELDS, parses)
    ab.save_json(output_root / "results.json", {
        "experiment": "warm ccache per-parse Text/Protobuf A/B",
        "parse_timing_boundary": PARSE_BOUNDARY,
        "runs": [{key: value for key, value in run.items() if key != "metrics"} for run in runs],
        "complete": complete,
    })


def run(root: Path, clava_root: Path, native_tool: Path, js_workspace: Path,
        runtime: Path, repeat_count: int, suites: tuple[str, ...]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    all_runs: list[dict[str, Any]] = []
    all_parses: list[dict[str, Any]] = []
    by_run_id: dict[str, dict[str, Any]] = {}
    parse_rows_by_run: dict[str, list[dict[str, Any]]] = {}

    # Prime each suite/policy/protocol cache once. Protocol roots are separate,
    # but both receive the same warm-cache workflow before measured repeats.
    all_seed_runs_valid = True
    for policy in POLICIES:
        for stage in STAGES:
            for suite in suites:
                run_row, parse_rows = execute_suite(
                    stage, suite, policy, "seed", 0, clava_root, native_tool, js_workspace, runtime, root,
                )
                all_runs.append(run_row)
                all_parses.extend(parse_rows)
                by_run_id[run_row["run_id"]] = run_row
                parse_rows_by_run[run_row["run_id"]] = parse_rows
                write_outputs(root, all_runs, all_parses)
                print(f"seed {suite} {policy} {stage['wire']}: valid={run_row['valid']}, "
                      f"cache hits={run_row['ccache_hits']} misses={run_row['ccache_misses']}", flush=True)
                if not run_row["valid"]:
                    all_seed_runs_valid = False

    all_seed_pairs_valid = validate_seed_pairs(POLICIES, suites, by_run_id, parse_rows_by_run)
    write_outputs(root, all_runs, all_parses)
    if not all_seed_runs_valid or not all_seed_pairs_valid:
        return all_runs, all_parses

    for repeat in range(1, repeat_count + 1):
        policies = POLICIES if repeat % 2 else POLICIES[::-1]
        for policy_index, policy in enumerate(policies):
            group_runs: dict[tuple[str, str], dict[str, Any]] = {}
            group_parse_rows: dict[tuple[str, str], list[dict[str, Any]]] = {}
            for stage, suite in measured_order(repeat + policy_index):
                if suite not in suites:
                    continue
                run_row, parse_rows = execute_suite(
                    stage, suite, policy, "measured", repeat, clava_root,
                    native_tool, js_workspace, runtime, root,
                )
                all_runs.append(run_row)
                all_parses.extend(parse_rows)
                by_run_id[run_row["run_id"]] = run_row
                parse_rows_by_run[run_row["run_id"]] = parse_rows
                group_runs[(suite, stage["wire"])] = run_row
                group_parse_rows[(suite, stage["wire"])] = parse_rows
                write_outputs(root, all_runs, all_parses)
                print(f"repeat {repeat} {policy} {suite} {stage['wire']}: "
                      f"{run_row['elapsed_s']:.2f}s, valid={run_row['valid']}, "
                      f"cache hits={run_row['ccache_hits']} misses={run_row['ccache_misses']}", flush=True)
                if not run_row["valid"]:
                    return all_runs, all_parses
            for suite in suites:
                text = group_runs[(suite, "text")]
                proto = group_runs[(suite, "protobuf")]
                validate_wire_pair(text, group_parse_rows[(suite, "text")],
                                    proto, group_parse_rows[(suite, "protobuf")])
            write_outputs(root, all_runs, all_parses)
            if any(not group_runs[(suite, protocol)]["valid"]
                   for suite in suites for protocol in ("text", "protobuf")):
                return all_runs, all_parses

    current_sources = ab.source_metadata(clava_root, native_tool, js_workspace)
    recorded_sources = json.loads((root / "plan.json").read_text())["sources"]
    if not ab.source_metadata_matches(current_sources, recorded_sources):
        raise SystemExit("source or native executable identity changed during measurement")
    return all_runs, all_parses


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preflight-result", type=Path, required=True,
                        help="passing run_dual_ab preflight with the instrumented runtime")
    parser.add_argument("--output-root", type=Path,
                        help="new results directory; defaults under experiments/protocol-comparison/results")
    parser.add_argument("--repeat-count", type=int, default=6)
    parser.add_argument("--suite", choices=("all", "clava-js", "java"), default="all")
    parser.add_argument("--clava-root", type=Path, default=ab.CLAVA_ROOT)
    parser.add_argument("--native-tool", type=Path, default=ab.DEFAULT_NATIVE_TOOL)
    parser.add_argument("--js-workspace", type=Path, default=ab.DEFAULT_JS_WORKSPACE)
    args = parser.parse_args()
    if args.repeat_count < 2:
        parser.error("--repeat-count must be at least two")

    clava_root = args.clava_root.resolve()
    native_tool = args.native_tool.resolve()
    js_workspace = args.js_workspace.resolve()
    suites = SUITES if args.suite == "all" else (args.suite,)
    dependencies = ab.configure_gradle_dependency_roots(js_workspace)
    ab.validate_inputs(clava_root, native_tool, js_workspace, dependencies, require_native=True)
    if shutil.which("ccache") is None:
        raise SystemExit("ccache is required for the warm-cache comparison")
    sources = ab.source_metadata(clava_root, native_tool, js_workspace)
    runtime, gate = ab.validate_preflight(args.preflight_result.resolve(), sources, native_tool)
    root = output_root_for(args.output_root)
    plan = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "experiment": "same-revision Text+ccache versus Protobuf+ccache per-parse A/B",
        "output_root": str(root), "suite": args.suite, "repeat_count": args.repeat_count,
        "cache_mode": "warm", "ccache_disabled": False,
        "min_measured_warm_cache_hit_rate": MIN_WARM_CACHE_HIT_RATE,
        "cache_namespace_per_protocol": CCACHE_NAMESPACE,
        "cache_root_isolation": "separate java.io.tmpdir/DUMPER_FOLDER for each suite, GC policy, and protocol",
        "gc_policies": {
            "normal": "effective HotSpot DisableExplicitGC=false",
            "disabled": "effective HotSpot DisableExplicitGC=true",
        },
        "parse_timing_boundary": PARSE_BOUNDARY,
        "full_command_timing_boundary": "monotonic wall time around the complete Gradle or Vitest command",
        "excluded_tests": {
            "java": ["ClangResourcesTest", "CudaResourcesTest", "CxxCudaTest", "GeneratedParseRootTest",
                     "ClangCcacheAdapterTest", "CXXConversionDeclTest", "ClangAstDumperTest",
                     "ClangAstDumperArgumentsTest", "AstWireFidelitySnapshotTest",
                     "pt.up.fe.specs.clang.wire.AstWireBenchmarkIdentityTest",
                     "pt.up.fe.specs.clang.wire.*"],
            "clava-js": "same Vitest filter as run_dual_ab.py; 164 total, 158 pass, 6 host-dependent skips",
        },
        "expected_parse_events": ab.EXPECTED_METRIC_EVENTS,
        "expected_suite_counts": ab.EXPECTED_SUITE_COUNTS,
        "fidelity_gate": gate,
        "runtime_parser_jar_sha256": ab.base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        "sources": sources,
        "order": {
            str(repeat): {
                policy: [(stage["wire"], suite)
                         for stage, suite in measured_order(repeat + policy_index)]
                for policy_index, policy in enumerate(
                    POLICIES if repeat % 2 else POLICIES[::-1]
                )
            }
            for repeat in range(1, args.repeat_count + 1)
        },
    }
    ab.save_json(root / "plan.json", plan)
    all_runs, all_parses = run(root, clava_root, native_tool, js_workspace,
                               runtime, args.repeat_count, suites)
    all_valid = len(all_runs) == len(POLICIES) * (len(STAGES) * len(suites) + args.repeat_count * len(STAGES) * len(suites)) \
        and all(run_row["valid"] for run_row in all_runs)
    write_outputs(root, all_runs, all_parses, complete=all_valid)
    summary = {
        "complete": all_valid, "valid": all_valid, "run_count": len(all_runs),
        "parse_event_count": len(all_parses), "parse_timing_boundary": PARSE_BOUNDARY,
        "suite_gc_protocol": {},
    }
    for suite in suites:
        summary["suite_gc_protocol"][suite] = {}
        for policy in POLICIES:
            summary["suite_gc_protocol"][suite][policy] = {}
            for protocol in ("text", "protobuf"):
                rows = [row for row in all_runs if row["suite"] == suite and row["gc_policy"] == policy
                        and row["protocol"] == protocol and row["measured"]]
                summary["suite_gc_protocol"][suite][policy][protocol] = {
                    "measured_run_count": len(rows),
                    "median_full_command_elapsed_s": statistics.median(
                        float(row["elapsed_s"]) for row in rows
                    ) if rows else None,
                    "all_valid": len(rows) == args.repeat_count and all(row["valid"] for row in rows),
                }
    ab.save_json(root / "summary.json", summary)
    print(json.dumps(summary, indent=2), flush=True)
    return 0 if all_valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
