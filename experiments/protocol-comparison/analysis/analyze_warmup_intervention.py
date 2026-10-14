#!/usr/bin/env python3
"""Validate and summarize the two-round warm-cache prefix intervention.

This analyzer is deliberately separate from the primary six-round A/B report.
It joins each observation to its schedule, checks the batch-run and ccache
proofs, then compares the two treatments only within matching suite, protocol,
and round cells.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import html
import json
import math
from pathlib import Path
import re
import statistics
import uuid
from typing import Any


SUITES = ("clava-js", "java")
PROTOCOLS = ("text", "protobuf")
TREATMENTS = ("control", "prefix-java-nas")
REPEATS = (1, 2)
GROUP_COUNTS = {"clava-js": 300, "java": 216}
PREFIX_IDS = tuple(f"java-group-{ordinal:04d}" for ordinal in range(3, 11))
KEY_GROUP_IDS = {
    "clava-js": ("js-group-0002", "js-group-0026", "js-group-0028"),
    "java": ("java-group-0001", "java-group-0004", "java-group-0008",
             "java-group-0009", "java-group-0010"),
}
EXPECTED_ELIGIBLE_CALLS = {
    ("control", "clava-js"): 166,
    ("control", "java"): 208,
    ("prefix-java-nas", "clava-js"): 174,
    ("prefix-java-nas", "java"): 216,
}
GC_POLICY = "no explicit/forced GC options or calls in runner; JVM automatic GC allowed"
HASH_RE = re.compile(r"^[0-9a-fA-F]{64}$")
INPUT_ID_RE = {
    "clava-js": re.compile(r"^js-group-(\d{4})$"),
    "java": re.compile(r"^java-group-(\d{4})$"),
}


class AnalysisError(ValueError):
    """Raised when the prepared intervention does not meet its run contract."""


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        raise AnalysisError(f"cannot read {path.name}: {error}") from error
    for line_number, line in enumerate(lines, 1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as error:
            raise AnalysisError(f"{path.name}:{line_number}: malformed JSON") from error
        if not isinstance(row, dict):
            raise AnalysisError(f"{path.name}:{line_number}: row is not an object")
        result.append(row)
    return result


def _jsonl_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AnalysisError(f"cannot read {path.name}: {error}") from error
    if not isinstance(value, dict):
        raise AnalysisError(f"{path.name}: manifest must be an object")
    return value


def _is_sha256(value: Any) -> bool:
    return isinstance(value, str) and HASH_RE.fullmatch(value) is not None


def _positive_number(value: Any, field: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise AnalysisError(f"{field} must be numeric")
    result = float(value)
    if not math.isfinite(result) or result <= 0:
        raise AnalysisError(f"{field} must be finite and positive")
    return result


def _safe_source_label(value: Any) -> str:
    if not isinstance(value, str) or not value.strip():
        raise AnalysisError("scheduled source_label is missing")
    label = value.strip()
    if label == "<empty source group>":
        return label
    parts = re.split(r"([;,]\s*)", label.replace("\\", "/"))
    for index in range(0, len(parts), 2):
        piece = parts[index].strip()
        if "/" in piece:
            parts[index] = piece.rsplit("/", 1)[-1]
    sanitized = "".join(parts)
    if any(marker in sanitized for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("source label contains a local path")
    return sanitized


def _group_ids(suite: str, count: int) -> list[str]:
    prefix = "js" if suite == "clava-js" else "java"
    return [f"{prefix}-group-{ordinal:04d}" for ordinal in range(1, count + 1)]


def _schedule_filename(treatment: str, suite: str, protocol: str, repeat: int) -> str:
    prefix = "measure" if treatment == "control" else "prefix-java-nas"
    return f"{prefix}-{suite}-{protocol}-warm-r{repeat:02d}.jsonl"


def _cell_name(treatment: str, suite: str, protocol: str, repeat: int) -> str:
    prefix = "control" if treatment == "control" else "prefix"
    return f"{prefix}-{suite}-{protocol}-warm-r{repeat:02d}"


def _observation_filename(treatment: str, suite: str, protocol: str, repeat: int) -> str:
    return f"{_cell_name(treatment, suite, protocol, repeat)}.jsonl"


def _manifest_filename(treatment: str, suite: str, protocol: str, repeat: int) -> str:
    return f"run-{_cell_name(treatment, suite, protocol, repeat)}.json"


def _counter_filename(treatment: str, suite: str, protocol: str, repeat: int) -> str:
    return f"counterproof-{_cell_name(treatment, suite, protocol, repeat)}.jsonl"


def _cell_keys() -> list[tuple[str, str, str, int]]:
    return [
        (treatment, suite, protocol, repeat)
        for treatment in TREATMENTS
        for suite in SUITES
        for protocol in PROTOCOLS
        for repeat in REPEATS
    ]


def _validate_schedule_flags(row: dict[str, Any], source: str, protocol: str) -> None:
    required = {
        "cache_mode": "warm",
        "compression_policy": "raw_control",
        "wire_format": protocol,
        "compression_enabled": False,
        "disable_compression": True,
        "expected_compressed": False,
        "expected_ccache_disabled": False,
        "cache_policy_requested": "enabled_for_eligible_sources",
        "ccache_output_compression": "disabled_by_CCACHE_NOCOMPRESS",
    }
    for field, expected in required.items():
        if row.get(field) != expected:
            raise AnalysisError(f"{source}: {field} must be {expected!r}")
    parser_config = row.get("parser_config")
    if not isinstance(parser_config, dict):
        raise AnalysisError(f"{source}: parser_config is missing")
    if parser_config.get("show_exec_info") is not False:
        raise AnalysisError(f"{source}: SHOW_EXEC_INFO must be false")
    if parser_config.get("ast_dump_cache") is not True:
        raise AnalysisError(f"{source}: warm parser cache must be enabled")
    calls = row.get("expected_native_calls")
    if not isinstance(calls, list):
        raise AnalysisError(f"{source}: expected_native_calls must be a list")
    if type(row.get("expected_native_count")) is not int or row["expected_native_count"] != len(calls):
        raise AnalysisError(f"{source}: expected_native_count disagrees with expected_native_calls")
    for call in calls:
        if not isinstance(call, dict):
            raise AnalysisError(f"{source}: expected native call must be an object")
        if type(call.get("cache_enabled")) is not bool:
            raise AnalysisError(f"{source}: native call cache_enabled must be boolean")
        if (call.get("ccache_disabled") is not False
                or call.get("compressed") is not False
                or call.get("wire_format") != protocol):
            raise AnalysisError(f"{source}: native call is not warm/raw/{protocol}")
        if call["cache_enabled"] and call.get("ccache_nocompress") is not True:
            raise AnalysisError(f"{source}: cacheable native call must disable ccache compression")
        if not call["cache_enabled"] and type(call.get("ccache_nocompress")) is not bool:
            raise AnalysisError(f"{source}: non-cacheable native call has invalid ccache policy")


def _validate_manifest(
    manifest: dict[str, Any], *, treatment: str, suite: str,
    protocol: str, repeat: int, source_schedule_path: Path,
    executed_schedule_path: Path, observation_path: Path, counterproof_path: Path,
) -> dict[str, Any]:
    source = _manifest_filename(treatment, suite, protocol, repeat)
    expected_hits = EXPECTED_ELIGIBLE_CALLS[(treatment, suite)]
    expected = {
        "schema_version": 1,
        "name": _cell_name(treatment, suite, protocol, repeat),
        "phase": "measure",
        "treatment": "control" if treatment == "control" else "prefix",
        "suite": suite,
        "protocol": protocol,
        "repeat": repeat,
        "exit_code": 0,
        "valid": True,
        "profiled": False,
        "show_exec_info": False,
        "runner_has_no_explicit_gc_call": True,
        "explicit_gc_policy_flags": [],
        "gc_policy": GC_POLICY,
        "counter_reset_recorded": True,
    }
    for field, value in expected.items():
        if manifest.get(field) != value:
            raise AnalysisError(f"{source}: {field} does not match the required run proof")
    expected_paths = {
        "schedule_path": source_schedule_path,
        "executed_schedule_path": executed_schedule_path,
        "observation_path": observation_path,
        "runner_output_path": observation_path,
        "counterproof_path": counterproof_path,
    }
    for field, path in expected_paths.items():
        value = manifest.get(field)
        if not isinstance(value, str) or Path(value).name != path.name:
            raise AnalysisError(f"{source}: {field} does not name {path.name}")
    expected_hashes = {
        "schedule_sha256": source_schedule_path,
        "executed_schedule_sha256": executed_schedule_path,
        "observation_sha256": observation_path,
        "runner_output_sha256": observation_path,
        "counterproof_sha256": counterproof_path,
    }
    for field, path in expected_hashes.items():
        value = manifest.get(field)
        if not _is_sha256(value) or value.lower() != _jsonl_sha256(path):
            raise AnalysisError(f"{source}: {field} does not match {path.name}")

    policy = manifest.get("env_policy")
    if not isinstance(policy, dict):
        raise AnalysisError(f"{source}: env_policy proof is missing")
    policy_expected = {
        "automatic_gc_allowed": True,
        "ccache_disable": "unset",
        "forced_gc_policy_flags": [],
        "java_agent": "none",
        "runner_has_no_explicit_gc_call": True,
        "show_exec_info": False,
    }
    for field, value in policy_expected.items():
        if policy.get(field) != value:
            raise AnalysisError(f"{source}: env_policy.{field} does not match the required run proof")
    removed_vars = policy.get("removed_inherited_option_vars")
    expected_removed_vars = (
        "CCACHE_DISABLE", "CLAVA_AST_CORPUS_CAPTURE_DIR", "CLAVA_AST_CORPUS_SUITE",
        "GRADLE_OPTS", "JAVA_OPTS", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
    )
    if not isinstance(removed_vars, dict) or any(removed_vars.get(name) is not False for name in expected_removed_vars):
        raise AnalysisError(f"{source}: inherited environment option policy is not clean")
    java_argv = manifest.get("java_argv")
    if not isinstance(java_argv, list) or not java_argv or not all(
        isinstance(argument, str) for argument in java_argv
    ):
        raise AnalysisError(f"{source}: java_argv is missing")
    if any(re.search(r"-XX:[^\s]*gc", argument, re.IGNORECASE) for argument in java_argv):
        raise AnalysisError(f"{source}: java_argv contains an explicit GC policy flag")
    if not isinstance(manifest.get("cwd"), str) or not manifest["cwd"]:
        raise AnalysisError(f"{source}: cwd is missing")
    if manifest.get("java_agent") != "none":
        raise AnalysisError(f"{source}: unexpected Java agent in a non-profiled run")
    if not isinstance(manifest.get("cache_directory"), str) or not manifest["cache_directory"]:
        raise AnalysisError(f"{source}: cache_directory is missing")
    for field in ("runner_class_sha256", "overlay_class_manifest_sha256",
                  "runtime_jar_manifest_sha256", "reader_class_sha256",
                  "compat_proto_types_sha256", "native_tool_sha256"):
        if not _is_sha256(manifest.get(field)):
            raise AnalysisError(f"{source}: {field} must be a SHA-256 digest")
    for field in ("started_utc", "finished_utc"):
        value = manifest.get(field)
        if not isinstance(value, str) or not value:
            raise AnalysisError(f"{source}: {field} is missing")
    if type(manifest.get("expected_eligible_hits")) is not int or manifest["expected_eligible_hits"] != expected_hits:
        raise AnalysisError(f"{source}: expected_eligible_hits differs from the prepared schedule")
    counters = manifest.get("ccache_counters")
    expected_counters = {"cacheable_calls": expected_hits, "hits": expected_hits, "misses": 0}
    if not isinstance(counters, dict) or counters != expected_counters:
        raise AnalysisError(f"{source}: ccache_counters differ from the expected warm-cache proof")
    payload_hashes = [manifest.get(field) for field in (
        "cache_seed_payload_sha256", "cache_payload_sha256_after_zero",
        "cache_payload_sha256_after_run",
    )]
    if not all(_is_sha256(value) for value in payload_hashes) or len(set(payload_hashes)) != 1:
        raise AnalysisError(f"{source}: cache payload changed or lacks a SHA-256 proof")
    return {
        "suite": suite,
        "protocol": protocol,
        "treatment": treatment,
        "repeat": repeat,
        "schedule_sha256": manifest["schedule_sha256"].lower(),
        "executed_schedule_sha256": manifest["executed_schedule_sha256"].lower(),
        "observation_sha256": manifest["observation_sha256"].lower(),
        "counterproof_sha256": manifest["counterproof_sha256"].lower(),
        "runner_class_sha256": manifest["runner_class_sha256"].lower(),
        "overlay_class_manifest_sha256": manifest["overlay_class_manifest_sha256"].lower(),
        "runtime_jar_manifest_sha256": manifest["runtime_jar_manifest_sha256"].lower(),
        "reader_class_sha256": manifest["reader_class_sha256"].lower(),
        "compat_proto_types_sha256": manifest["compat_proto_types_sha256"].lower(),
        "native_tool_sha256": manifest["native_tool_sha256"].lower(),
        "cache_payload_sha256": payload_hashes[0].lower(),
        "cacheable_calls": expected_hits,
        "cache_hits": expected_hits,
        "cache_misses": 0,
        "no_explicit_gc": True,
        "show_exec_info": False,
    }


def _counter_value(output: str, label: str, source: str) -> int:
    matches = re.findall(rf"^\s*{re.escape(label)}:\s*([\d,]+)", output, re.MULTILINE)
    if len(matches) != 1:
        raise AnalysisError(f"{source}: expected one {label} counter, found {len(matches)}")
    return int(matches[0].replace(",", ""))


def _validate_counterproof(
    rows: list[dict[str, Any]], *, source: str, treatment: str, suite: str,
    protocol: str, repeat: int, eligible_calls: int, cache_directory: str,
    manifest_counters: dict[str, Any],
) -> dict[str, int]:
    if len(rows) != 2 or any(row.get("record_type") != "ccache" for row in rows):
        raise AnalysisError(f"{source}: expected only ccache reset and stats proof rows")
    operations = [row.get("operation") for row in rows]
    if operations != ["ccache_zero", "ccache_stats"]:
        raise AnalysisError(f"{source}: expected ccache_zero then ccache_stats")
    zero, stats = rows
    expected_hits = EXPECTED_ELIGIBLE_CALLS[(treatment, suite)]
    for row in rows:
        if (row.get("input_id") != "__ccache__" or row.get("command_status") != 0
                or row.get("valid") is not True or row.get("suite") != suite
                or row.get("protocol") != protocol or row.get("cache_mode") != "warm"
                or row.get("repeat") != repeat or row.get("cache_directory") != cache_directory):
            raise AnalysisError(f"{source}: ccache command attribution or status is invalid")
    if not isinstance(zero.get("output"), str) or "Statistics zeroed" not in zero["output"]:
        raise AnalysisError(f"{source}: ccache statistics were not confirmed zeroed")
    output = stats.get("output")
    if not isinstance(output, str):
        raise AnalysisError(f"{source}: ccache stats output is missing")
    stats_sections = output.split("Local storage:")
    if len(stats_sections) != 2:
        raise AnalysisError(f"{source}: ccache stats must contain one Local storage section")
    totals, local_storage = stats_sections
    cacheable = _counter_value(totals, "Cacheable calls", source)
    hits = _counter_value(totals, "Hits", source)
    misses = _counter_value(totals, "Misses", source)
    if (_counter_value(local_storage, "Hits", source) != hits
            or _counter_value(local_storage, "Misses", source) != misses):
        raise AnalysisError(f"{source}: local-storage counters disagree with ccache totals")
    if (eligible_calls, cacheable, hits, misses) != (expected_hits, expected_hits, expected_hits, 0):
        raise AnalysisError(
            f"{source}: ccache proof expected {expected_hits} eligible hits and zero misses; "
            f"schedule={eligible_calls}, stats={cacheable}/{hits}/{misses}"
        )
    if manifest_counters != {"cacheable_calls": cacheable, "hits": hits, "misses": misses}:
        raise AnalysisError(f"{source}: ccache output disagrees with run manifest counters")
    return {"cacheable_calls": cacheable, "hits": hits, "misses": misses}


def _validate_cell(
    *, treatment: str, suite: str, protocol: str, repeat: int,
    schedule_rows: list[dict[str, Any]], observation_rows: list[dict[str, Any]],
    manifest: dict[str, Any], manifest_summary: dict[str, Any],
    counterproof_rows: list[dict[str, Any]], expected_group_counts: dict[str, int],
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    schedule_name = _cell_name(treatment, suite, protocol, repeat) + ".jsonl"
    observation_name = _observation_filename(treatment, suite, protocol, repeat)
    is_prefix = treatment == "prefix-java-nas"
    expected_ids = _group_ids(suite, expected_group_counts[suite])
    expected_phases = (["warmup"] * len(PREFIX_IDS) + ["measure"] * len(expected_ids)
                       if is_prefix else ["measure"] * len(expected_ids))
    if len(schedule_rows) != len(expected_phases):
        raise AnalysisError(f"{schedule_name}: expected {len(expected_phases)} schedule rows")
    if len(observation_rows) != len(schedule_rows):
        raise AnalysisError(f"{observation_name}: row count differs from its schedule")

    warmup_rows = schedule_rows[:len(PREFIX_IDS)] if is_prefix else []
    if is_prefix and [row.get("input_id") for row in warmup_rows] != list(PREFIX_IDS):
        raise AnalysisError(f"{schedule_name}: prefix IDs must be exactly java-group-0003..0010 in order")
    if [row.get("input_id") for row in schedule_rows if row.get("phase") == "measure"] != expected_ids:
        raise AnalysisError(f"{schedule_name}: measured input ID set/order is incomplete")

    eligible_calls = 0
    output_measurements: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()
    for line_number, (scheduled, observed, expected_phase) in enumerate(
        zip(schedule_rows, observation_rows, expected_phases), start=1
    ):
        source = f"{schedule_name}:{line_number}"
        if scheduled.get("phase") != expected_phase:
            raise AnalysisError(f"{source}: unexpected schedule phase")
        row_suite = "java" if expected_phase == "warmup" else suite
        if (scheduled.get("suite") != row_suite
                or scheduled.get("protocol") != protocol
                or scheduled.get("repeat") != (0 if expected_phase == "warmup" else repeat)):
            raise AnalysisError(f"{source}: suite/protocol/repeat attribution differs")
        input_id = scheduled.get("input_id")
        id_suite = "java" if expected_phase == "warmup" else suite
        match = INPUT_ID_RE[id_suite].fullmatch(input_id) if isinstance(input_id, str) else None
        if not match:
            raise AnalysisError(f"{source}: invalid input_id")
        ordinal = int(match.group(1))
        if expected_phase == "warmup":
            if input_id not in PREFIX_IDS:
                raise AnalysisError(f"{source}: unexpected unmeasured prefix ID")
        elif ordinal < 1 or ordinal > expected_group_counts[suite]:
            raise AnalysisError(f"{source}: measured input_id is out of range")
        identity_key = (expected_phase, input_id)
        if identity_key in seen:
            raise AnalysisError(f"{source}: duplicate phase/input_id")
        seen.add(identity_key)

        for field in ("event_id", "group_id"):
            try:
                uuid.UUID(str(scheduled.get(field)))
            except (ValueError, TypeError, AttributeError) as error:
                raise AnalysisError(f"{source}: {field} must be a UUID") from error
        for field in ("source_sha256", "args_sha256", "options_sha256"):
            if not _is_sha256(scheduled.get(field)):
                raise AnalysisError(f"{source}: {field} must be a SHA-256 digest")
        _safe_source_label(scheduled.get("source_label"))
        source_paths = scheduled.get("source_paths")
        if not isinstance(source_paths, list) or any(not isinstance(path, str) for path in source_paths):
            raise AnalysisError(f"{source}: source_paths must be a list of strings")
        _validate_schedule_flags(scheduled, source, protocol)
        eligible_calls += sum(call["cache_enabled"] is True
                              for call in scheduled["expected_native_calls"])

        if not isinstance(observed, dict) or observed.get("record_type") != "parse":
            raise AnalysisError(f"{observation_name}:{line_number}: expected record_type=parse")
        if observed.get("valid") is not True:
            raise AnalysisError(f"{observation_name}:{line_number}: invalid row")
        expected_observation = {
            "phase": expected_phase,
            "suite": row_suite,
            "protocol": protocol,
            "cache_mode": "warm",
            "repeat": 0 if expected_phase == "warmup" else repeat,
            "input_id": input_id,
            "event_id": scheduled["event_id"],
            "source_sha256": scheduled["source_sha256"],
            "args_sha256": scheduled["args_sha256"],
            "options_sha256": scheduled["options_sha256"],
            "schedule_line": line_number,
        }
        for field, expected in expected_observation.items():
            if observed.get(field) != expected:
                raise AnalysisError(
                    f"{observation_name}:{line_number}: {field} differs from schedule"
                )
        if "group_id" in observed and observed["group_id"] != scheduled["group_id"]:
            raise AnalysisError(f"{observation_name}:{line_number}: group_id differs from schedule")
        if observed.get("show_exec_info") is not False:
            raise AnalysisError(f"{observation_name}:{line_number}: SHOW_EXEC_INFO must be false")
        if observed.get("compression_policy") != "raw_control" or observed.get("expected_compressed") is not False:
            raise AnalysisError(f"{observation_name}:{line_number}: observation is not warm/raw")
        if ("source_label" in observed
                and observed["source_label"] != scheduled.get("source_label")):
            raise AnalysisError(f"{observation_name}:{line_number}: source_label differs from schedule")
        if type(observed.get("app_returned_null")) is not bool:
            raise AnalysisError(f"{observation_name}:{line_number}: app_returned_null must be boolean")
        if expected_phase == "measure":
            elapsed_ms = _positive_number(
                observed.get("elapsed_ms"), f"{observation_name}:{line_number}.elapsed_ms"
            )
        else:
            # Prefix rows are deliberately unmeasured and never enter totals.
            # Some runners may omit elapsed_ms or emit null for that phase.
            elapsed_ms = observed.get("elapsed_ms")
            if elapsed_ms is not None and (
                isinstance(elapsed_ms, bool)
                or not isinstance(elapsed_ms, (int, float))
                or not math.isfinite(float(elapsed_ms))
                or elapsed_ms < 0
            ):
                raise AnalysisError(f"{observation_name}:{line_number}: invalid unmeasured elapsed_ms")
        if expected_phase == "measure":
            output_measurements.append({
                "suite": suite,
                "input_id": input_id,
                "event_id": scheduled["event_id"],
                "source_label": _safe_source_label(scheduled["source_label"]),
                "source_count": len(source_paths),
                "source_sha256": scheduled["source_sha256"].lower(),
                "args_sha256": scheduled["args_sha256"].lower(),
                "options_sha256": scheduled["options_sha256"].lower(),
                "treatment": treatment,
                "protocol": protocol,
                "repeat": repeat,
                "elapsed_ms": elapsed_ms,
            })

    if len(output_measurements) != expected_group_counts[suite]:
        raise AnalysisError(f"{observation_name}: measured row count is incomplete")
    expected_eligible = EXPECTED_ELIGIBLE_CALLS[(treatment, suite)]
    if eligible_calls != expected_eligible:
        raise AnalysisError(
            f"{schedule_name}: expected {expected_eligible} eligible native calls, found {eligible_calls}"
        )
    if is_prefix:
        prefix_eligible = sum(
            call["cache_enabled"] is True
            for row in warmup_rows for call in row["expected_native_calls"]
        )
        if prefix_eligible != len(PREFIX_IDS):
            raise AnalysisError(f"{schedule_name}: prefix does not contain eight eligible NAS calls")

    run_summary = _validate_manifest(
        manifest, treatment=treatment, suite=suite, protocol=protocol, repeat=repeat,
        source_schedule_path=manifest_summary["source_schedule_path"],
        executed_schedule_path=manifest_summary["executed_schedule_path"],
        observation_path=manifest_summary["observation_path"],
        counterproof_path=manifest_summary["counterproof_path"],
    )
    counter_summary = _validate_counterproof(
        counterproof_rows, source=_counter_filename(treatment, suite, protocol, repeat),
        treatment=treatment, suite=suite, protocol=protocol, repeat=repeat,
        eligible_calls=eligible_calls, cache_directory=manifest["cache_directory"],
        manifest_counters=manifest["ccache_counters"],
    )
    return output_measurements, {**run_summary, **counter_summary}


def _median_delta_rows(rows: list[dict[str, Any]], *, keys: tuple[str, ...]) -> list[dict[str, Any]]:
    groups: dict[tuple[Any, ...], list[dict[str, Any]]] = {}
    for row in rows:
        groups.setdefault(tuple(row[key] for key in keys), []).append(row)
    result = []
    for key_values, group in sorted(groups.items(), key=lambda item: tuple(map(str, item[0]))):
        deltas = [row["delta_ms"] for row in group]
        pcts = [row["relative_pct"] for row in group]
        result.append({
            **dict(zip(keys, key_values)),
            "round_count": len(group),
            "round_deltas_ms": deltas,
            "median_delta_ms": statistics.median(deltas),
            "min_delta_ms": min(deltas),
            "max_delta_ms": max(deltas),
            "round_relative_pct": pcts,
            "median_relative_pct": statistics.median(pcts),
        })
    return result


def _median_field_rows(
    rows: list[dict[str, Any]], *, keys: tuple[str, ...], value_field: str,
) -> list[dict[str, Any]]:
    groups: dict[tuple[Any, ...], list[dict[str, Any]]] = {}
    for row in rows:
        groups.setdefault(tuple(row[key] for key in keys), []).append(row)
    result = []
    for key_values, group in sorted(groups.items(), key=lambda item: tuple(map(str, item[0]))):
        values = [row[value_field] for row in group]
        result.append({
            **dict(zip(keys, key_values)),
            "round_count": len(group),
            "round_values_ms": values,
            "median_ms": statistics.median(values),
            "min_ms": min(values),
            "max_ms": max(values),
        })
    return result


def _build_summary(
    measured: list[dict[str, Any]], *, group_counts: dict[str, int],
    run_proofs: list[dict[str, Any]], prefix_rows: int,
) -> dict[str, Any]:
    indexes = {
        (row["treatment"], row["suite"], row["protocol"], row["repeat"], row["input_id"]): row
        for row in measured
    }
    if len(indexes) != len(measured):
        raise AnalysisError("duplicate measured treatment/suite/protocol/repeat/input_id")
    expected_keys = {
        (treatment, suite, protocol, repeat, input_id)
        for treatment in TREATMENTS for suite in SUITES for protocol in PROTOCOLS
        for repeat in REPEATS for input_id in _group_ids(suite, group_counts[suite])
    }
    if set(indexes) != expected_keys:
        raise AnalysisError("measured tails do not cover the complete 16-cell intervention")

    group_rounds = sorted(measured, key=lambda row: (
        row["suite"], row["input_id"], row["treatment"], row["protocol"], row["repeat"]
    ))
    cell_totals: list[dict[str, Any]] = []
    totals = {}
    for treatment in TREATMENTS:
        for suite in SUITES:
            for protocol in PROTOCOLS:
                for repeat in REPEATS:
                    total = math.fsum(
                        indexes[(treatment, suite, protocol, repeat, input_id)]["elapsed_ms"]
                        for input_id in _group_ids(suite, group_counts[suite])
                    )
                    row = {
                        "treatment": treatment,
                        "suite": suite,
                        "protocol": protocol,
                        "repeat": repeat,
                        "group_count": group_counts[suite],
                        "sum_elapsed_ms": total,
                        "sum_elapsed_s": total / 1000.0,
                    }
                    cell_totals.append(row)
                    totals[(treatment, suite, protocol, repeat)] = total

    format_deltas = []
    for treatment in TREATMENTS:
        for suite in SUITES:
            for repeat in REPEATS:
                text_sum = totals[(treatment, suite, "text", repeat)]
                proto_sum = totals[(treatment, suite, "protobuf", repeat)]
                format_deltas.append({
                    "treatment": treatment,
                    "suite": suite,
                    "repeat": repeat,
                    "text_sum_ms": text_sum,
                    "protobuf_sum_ms": proto_sum,
                    "delta_ms": proto_sum - text_sum,
                    "relative_pct": 100.0 * (proto_sum / text_sum - 1.0),
                })

    treatment_deltas = []
    for suite in SUITES:
        for protocol in PROTOCOLS:
            for repeat in REPEATS:
                control_sum = totals[("control", suite, protocol, repeat)]
                prefix_sum = totals[("prefix-java-nas", suite, protocol, repeat)]
                treatment_deltas.append({
                    "suite": suite,
                    "protocol": protocol,
                    "repeat": repeat,
                    "control_sum_ms": control_sum,
                    "prefix_sum_ms": prefix_sum,
                    "delta_ms": prefix_sum - control_sum,
                    "relative_pct": 100.0 * (prefix_sum / control_sum - 1.0),
                })

    difference_in_differences = []
    for suite in SUITES:
        for repeat in REPEATS:
            control_text = totals[("control", suite, "text", repeat)]
            control_proto = totals[("control", suite, "protobuf", repeat)]
            prefix_text = totals[("prefix-java-nas", suite, "text", repeat)]
            prefix_proto = totals[("prefix-java-nas", suite, "protobuf", repeat)]
            text_treatment_delta = prefix_text - control_text
            proto_treatment_delta = prefix_proto - control_proto
            difference_in_differences.append({
                "suite": suite,
                "repeat": repeat,
                "control_text_ms": control_text,
                "control_protobuf_ms": control_proto,
                "prefix_text_ms": prefix_text,
                "prefix_protobuf_ms": prefix_proto,
                "text_treatment_delta_ms": text_treatment_delta,
                "protobuf_treatment_delta_ms": proto_treatment_delta,
                "difference_in_differences_ms": proto_treatment_delta - text_treatment_delta,
            })

    group_summaries = []
    for treatment in TREATMENTS:
        for suite in SUITES:
            for protocol in PROTOCOLS:
                for input_id in _group_ids(suite, group_counts[suite]):
                    samples = [indexes[(treatment, suite, protocol, repeat, input_id)]
                               for repeat in REPEATS]
                    first = samples[0]
                    group_summaries.append({
                        "treatment": treatment,
                        "suite": suite,
                        "protocol": protocol,
                        "input_id": input_id,
                        "source_label": first["source_label"],
                        "source_count": first["source_count"],
                        "event_id": first["event_id"],
                        "source_sha256": first["source_sha256"],
                        "args_sha256": first["args_sha256"],
                        "options_sha256": first["options_sha256"],
                        "round_count": len(samples),
                        "round_ms": [sample["elapsed_ms"] for sample in samples],
                        "median_ms": statistics.median(sample["elapsed_ms"] for sample in samples),
                    })

    key_group_rows = []
    for suite in SUITES:
        present = set(_group_ids(suite, group_counts[suite]))
        for input_id in KEY_GROUP_IDS[suite]:
            if input_id not in present:
                continue
            for repeat in REPEATS:
                values = {
                    (treatment, protocol): indexes[(treatment, suite, protocol, repeat, input_id)]
                    for treatment in TREATMENTS for protocol in PROTOCOLS
                }
                text_control = values[("control", "text")]
                proto_control = values[("control", "protobuf")]
                text_prefix = values[("prefix-java-nas", "text")]
                proto_prefix = values[("prefix-java-nas", "protobuf")]
                key_group_rows.append({
                    "suite": suite,
                    "input_id": input_id,
                    "source_label": text_control["source_label"],
                    "source_count": text_control["source_count"],
                    "source_sha256": text_control["source_sha256"],
                    "repeat": repeat,
                    "control_text_ms": text_control["elapsed_ms"],
                    "control_protobuf_ms": proto_control["elapsed_ms"],
                    "control_format_delta_ms": proto_control["elapsed_ms"] - text_control["elapsed_ms"],
                    "prefix_text_ms": text_prefix["elapsed_ms"],
                    "prefix_protobuf_ms": proto_prefix["elapsed_ms"],
                    "prefix_format_delta_ms": proto_prefix["elapsed_ms"] - text_prefix["elapsed_ms"],
                    "text_treatment_delta_ms": text_prefix["elapsed_ms"] - text_control["elapsed_ms"],
                    "protobuf_treatment_delta_ms": proto_prefix["elapsed_ms"] - proto_control["elapsed_ms"],
                })

    return {
        "schema_version": 1,
        "evidence_label": "validated measured intervention files",
        "primary_comparison_rounds": 6,
        "intervention_rounds": list(REPEATS),
        "group_counts": group_counts,
        "treatments": list(TREATMENTS),
        "protocols": list(PROTOCOLS),
        "cache_mode": "warm",
        "compression_policy": "raw_control",
        "timing_boundary": "outer CodeParser.parse(List<File>, compiler_options)",
        "validation": {
            "schedule_cells": len(_cell_keys()),
            "measured_observation_count": len(group_rounds),
            "unmeasured_prefix_rows": prefix_rows,
            "invalid_rows": 0,
            "measured_tail_identity_match": True,
            "run_manifests": len(run_proofs),
            "ccache_proofs": len(run_proofs),
            "all_runs_no_explicit_gc": all(row["no_explicit_gc"] for row in run_proofs),
            "all_runs_show_exec_info_false": all(not row["show_exec_info"] for row in run_proofs),
        },
        "run_proofs": sorted(run_proofs, key=lambda row: (
            row["treatment"], row["suite"], row["protocol"], row["repeat"]
        )),
        "cell_totals": cell_totals,
        "paired_format_deltas": format_deltas,
        "paired_format_delta_summary": _median_delta_rows(
            format_deltas, keys=("treatment", "suite")
        ),
        "same_format_treatment_deltas": treatment_deltas,
        "same_format_treatment_delta_summary": _median_delta_rows(
            treatment_deltas, keys=("suite", "protocol")
        ),
        "difference_in_differences": difference_in_differences,
        "difference_in_differences_summary": _median_field_rows(
            difference_in_differences, keys=("suite",),
            value_field="difference_in_differences_ms",
        ),
        "group_rounds": group_rounds,
        "group_summaries": group_summaries,
        "key_group_rounds": key_group_rows,
    }


def _normalized_schedule_row(row: dict[str, Any]) -> dict[str, Any]:
    """Ignore only the relocatable dumper output path when checking copied schedules."""
    normalized = dict(row)
    normalized.pop("dumper_folder", None)
    parser_config = normalized.get("parser_config")
    if isinstance(parser_config, dict):
        parser_config = dict(parser_config)
        parser_config.pop("dumper_folder", None)
        normalized["parser_config"] = parser_config
    return normalized


def _validate_schedule_copy(
    source_rows: list[dict[str, Any]], executed_rows: list[dict[str, Any]], source: str,
) -> None:
    if len(source_rows) != len(executed_rows):
        raise AnalysisError(f"{source}: executed schedule row count differs from source schedule")
    if [_normalized_schedule_row(row) for row in source_rows] != [
        _normalized_schedule_row(row) for row in executed_rows
    ]:
        raise AnalysisError(f"{source}: executed schedule differs beyond dumper-folder relocation")


def analyze_warmup_intervention(
    run_root: Path, *, expected_group_counts: dict[str, int] | None = None,
) -> dict[str, Any]:
    """Load the exact 16 prepared batches and require their run/counter proofs."""
    root = Path(run_root)
    expected_group_counts = expected_group_counts or GROUP_COUNTS
    if set(expected_group_counts) != set(SUITES) or any(
        type(value) is not int or value < 1 for value in expected_group_counts.values()
    ):
        raise AnalysisError("expected_group_counts must contain positive counts for both suites")
    source_schedules: dict[tuple[str, str, str, int], list[dict[str, Any]]] = {}
    schedules: dict[tuple[str, str, str, int], list[dict[str, Any]]] = {}
    observations: dict[tuple[str, str, str, int], list[dict[str, Any]]] = {}
    manifests: dict[tuple[str, str, str, int], dict[str, Any]] = {}
    proofs: dict[tuple[str, str, str, int], list[dict[str, Any]]] = {}
    manifest_inputs: dict[tuple[str, str, str, int], dict[str, Path]] = {}
    for cell in _cell_keys():
        treatment, suite, protocol, repeat = cell
        stem = _cell_name(*cell)
        source_schedule_path = root.parent / "schedules" / _schedule_filename(*cell)
        executed_schedule_path = root / "run-schedules" / f"{stem}.jsonl"
        observation_path = root / "observations" / _observation_filename(*cell)
        manifest_path = root / "run-manifests" / _manifest_filename(*cell)
        proof_path = root / "warm-counters" / _counter_filename(*cell)
        source_schedules[cell] = _read_jsonl(source_schedule_path)
        schedules[cell] = _read_jsonl(executed_schedule_path)
        observations[cell] = _read_jsonl(observation_path)
        manifests[cell] = _read_json(manifest_path)
        proofs[cell] = _read_jsonl(proof_path)
        manifest_inputs[cell] = {
            "source_schedule_path": source_schedule_path,
            "executed_schedule_path": executed_schedule_path,
            "observation_path": observation_path,
            "counterproof_path": proof_path,
        }

    for cell in _cell_keys():
        _validate_schedule_copy(
            source_schedules[cell], schedules[cell], _cell_name(*cell)
        )

    # First validate schedule-level treatment equality before looking at elapsed values.
    for suite in SUITES:
        for protocol in PROTOCOLS:
            for repeat in REPEATS:
                control_key = ("control", suite, protocol, repeat)
                prefix_key = ("prefix-java-nas", suite, protocol, repeat)
                control_tail = schedules[control_key]
                prefix_tail = [row for row in schedules[prefix_key] if row.get("phase") == "measure"]
                if control_tail != prefix_tail:
                    raise AnalysisError(
                        f"measured schedule tail differs between treatments for {suite}/{protocol}/r{repeat}"
                    )

    # Require stable input/event/group/hash attribution across every measured
    # condition, not only between the two treatments in one round.
    schedule_identity: dict[tuple[str, str], tuple[str, ...]] = {}
    event_to_input: dict[str, tuple[str, str]] = {}
    group_to_input: dict[str, tuple[str, str]] = {}
    for (_, suite, _, _), rows in schedules.items():
        for row in rows:
            if row.get("phase") != "measure":
                continue
            input_id = row.get("input_id")
            identity = tuple(str(row.get(field)) for field in (
                "event_id", "group_id", "source_sha256", "args_sha256", "options_sha256"
            ))
            key = (suite, str(input_id))
            if key in schedule_identity and schedule_identity[key] != identity:
                raise AnalysisError(f"schedule identity changes across cells for {suite}/{input_id}")
            schedule_identity[key] = identity
            prior_event = event_to_input.setdefault(identity[0], key)
            prior_group = group_to_input.setdefault(identity[1], key)
            if prior_event != key or prior_group != key:
                raise AnalysisError("event_id or group_id is reused by a different input")

    # The eight prefix rows are the same frozen NAS input groups that appear
    # later in the measured Java tail; only their phase differs.
    for protocol in PROTOCOLS:
        for repeat in REPEATS:
            prefix_rows = schedules[("prefix-java-nas", "clava-js", protocol, repeat)][:len(PREFIX_IDS)]
            java_rows = schedules[("control", "java", protocol, repeat)]
            java_by_id = {row["input_id"]: row for row in java_rows}
            for prefix_row in prefix_rows:
                java_row = java_by_id.get(prefix_row.get("input_id"))
                fields = ("event_id", "group_id", "source_sha256", "args_sha256", "options_sha256")
                if java_row is None or any(prefix_row.get(field) != java_row.get(field) for field in fields):
                    raise AnalysisError(
                        f"prefix identity differs from measured Java input {prefix_row.get('input_id')}"
                    )

    measured: list[dict[str, Any]] = []
    run_proofs: list[dict[str, Any]] = []
    for cell in _cell_keys():
        treatment, suite, protocol, repeat = cell
        batch, run_summary = _validate_cell(
            treatment=treatment, suite=suite, protocol=protocol, repeat=repeat,
            schedule_rows=schedules[cell], observation_rows=observations[cell],
            manifest=manifests[cell],
            manifest_summary=manifest_inputs[cell],
            counterproof_rows=proofs[cell], expected_group_counts=expected_group_counts,
        )
        measured.extend(batch)
        run_proofs.append(run_summary)

    # Check identity fields on both treatment tails, not only the JSON schedules.
    obs_index = {
        (row["treatment"], row["suite"], row["protocol"], row["repeat"], row["input_id"]): row
        for row in measured
    }
    for suite in SUITES:
        for protocol in PROTOCOLS:
            for repeat in REPEATS:
                for input_id in _group_ids(suite, expected_group_counts[suite]):
                    control = obs_index[("control", suite, protocol, repeat, input_id)]
                    prefix = obs_index[("prefix-java-nas", suite, protocol, repeat, input_id)]
                    fields = ("event_id", "source_sha256", "args_sha256", "options_sha256")
                    if any(control[field] != prefix[field] for field in fields):
                        raise AnalysisError(
                            f"measured observation identity differs across treatments for {suite}/{input_id}"
                        )

    prefix_rows = sum(
        row.get("phase") == "warmup"
        for treatment, suite, protocol, repeat in _cell_keys()
        if treatment == "prefix-java-nas"
        for row in schedules[(treatment, suite, protocol, repeat)]
    )
    expected_prefix_rows = len(PREFIX_IDS) * len(SUITES) * len(PROTOCOLS) * len(REPEATS)
    if prefix_rows != expected_prefix_rows:
        raise AnalysisError(f"expected {expected_prefix_rows} unmeasured prefix rows, found {prefix_rows}")
    return _build_summary(
        measured, group_counts=expected_group_counts, run_proofs=run_proofs,
        prefix_rows=prefix_rows,
    )


def _fmt_seconds(seconds: float) -> str:
    return f"{seconds:.1f} s"


def _svg_suite_totals(summary: dict[str, Any], suite: str) -> str:
    counts = summary["group_counts"]
    rows = [row for row in summary["cell_totals"] if row["suite"] == suite]
    maximum_s = max(row["sum_elapsed_s"] for row in rows)
    if not math.isfinite(maximum_s) or maximum_s <= 0:
        raise AnalysisError(f"no positive round totals for {suite}")
    axis_max = maximum_s * 1.04
    left, right = 138.0, 318.0
    project = lambda value: left + value * (right - left) / axis_max
    y_positions = {
        ("control", "text"): 78,
        ("control", "protobuf"): 132,
        ("prefix-java-nas", "text"): 201,
        ("prefix-java-nas", "protobuf"): 255,
    }
    output = [
        '<svg class="wi-chart" viewBox="0 0 360 360" role="img" '
        f'aria-label="{html.escape(suite)} warm-cache intervention, two rounds, summed parse times in seconds">',
        '<desc>Each row is one treatment and protocol. Filled dots are round one, open dots round two. '
        'The linear seconds axis is shared across all four conditions in this suite.</desc>',
    ]
    for tick in range(4):
        value = axis_max * tick / 3
        x = project(value)
        output.extend([
            f'<line class="wi-grid" x1="{x:.2f}" x2="{x:.2f}" y1="48" y2="278"/>',
            f'<text class="wi-label" x="{x:.2f}" y="36" text-anchor="middle">{_fmt_seconds(value)}</text>',
        ])
    for treatment, protocol in (("control", "text"), ("control", "protobuf"),
                                ("prefix-java-nas", "text"), ("prefix-java-nas", "protobuf")):
        y = y_positions[(treatment, protocol)]
        treatment_label = "No prefix" if treatment == "control" else "8 NAS prefix"
        protocol_label = "Text" if protocol == "text" else "Proto"
        rows_here = sorted((row for row in rows
                            if row["treatment"] == treatment and row["protocol"] == protocol),
                           key=lambda row: row["repeat"])
        if [row["repeat"] for row in rows_here] != list(REPEATS):
            raise AnalysisError(f"{suite}/{treatment}/{protocol} must contain rounds 1 and 2")
        first_x, second_x = (project(row["sum_elapsed_s"]) for row in rows_here)
        color = "#32658e" if protocol == "text" else "#147d64"
        output.extend([
            f'<text class="wi-label" x="3" y="{y - 3}">{html.escape(treatment_label)}</text>',
            f'<text class="wi-label" x="3" y="{y + 15}">{html.escape(protocol_label)}</text>',
            f'<line stroke="{color}" stroke-width="2" x1="{first_x:.2f}" x2="{second_x:.2f}" y1="{y}" y2="{y}"/>',
            f'<circle cx="{first_x:.2f}" cy="{y - 4}" r="6" fill="{color}"><title>Round 1: {_fmt_seconds(rows_here[0]["sum_elapsed_s"])}</title></circle>',
            f'<circle cx="{second_x:.2f}" cy="{y + 4}" r="6" fill="white" stroke="{color}" stroke-width="2"><title>Round 2: {_fmt_seconds(rows_here[1]["sum_elapsed_s"])}</title></circle>',
        ])
    output.extend([
        '<line class="wi-separator" x1="2" x2="356" y1="169" y2="169"/>',
        '<circle cx="128" cy="305" r="6" fill="#32658e"/>',
        '<text class="wi-label" x="140" y="310">Round 1</text>',
        '<circle cx="237" cy="305" r="6" fill="white" stroke="#32658e" stroke-width="2"/>',
        '<text class="wi-label" x="249" y="310">Round 2</text>',
        '<text class="wi-label" x="228" y="345" text-anchor="middle">Summed parse time (seconds)</text>',
        '</svg>',
    ])
    return "".join(output)


def render_warmup_intervention_html(summary: dict[str, Any]) -> str:
    """Render a compact responsive fragment. Synthetic test evidence is watermarked."""
    if summary.get("schema_version") != 1:
        raise AnalysisError("unsupported intervention summary schema")
    if summary.get("evidence_label") != "validated measured intervention files":
        label = html.escape(str(summary.get("evidence_label", "Synthetic test data")))
        return f'<section class="warmup-intervention"><p>{label}; not measured report data.</p></section>'
    counts = summary["group_counts"]
    charts = "".join(
        '<figure class="wi-figure"><figcaption>'
        f'{html.escape("Clava-JS" if suite == "clava-js" else "Java")} · {counts[suite]} groups'
        f'</figcaption>{_svg_suite_totals(summary, suite)}</figure>'
        for suite in SUITES
    )
    total_rows = []
    for row in summary["cell_totals"]:
        total_rows.append(
            f'<tr><th scope="row">{html.escape("Clava-JS" if row["suite"] == "clava-js" else "Java")}</th>'
            f'<td data-label="Condition">{html.escape("No prefix" if row["treatment"] == "control" else "8 NAS prefix")} · '
            f'{html.escape("Text" if row["protocol"] == "text" else "Proto")}</td>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Groups">{row["group_count"]}</td>'
            f'<td data-label="Summed parse time">{_fmt_seconds(row["sum_elapsed_s"])}</td></tr>'
        )
    format_rows = []
    for row in summary["paired_format_deltas"]:
        format_rows.append(
            f'<tr><th scope="row">{html.escape("Clava-JS" if row["suite"] == "clava-js" else "Java")}</th>'
            f'<td data-label="Treatment">{html.escape("No prefix" if row["treatment"] == "control" else "8 NAS prefix")}</td>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Proto minus Text">{row["delta_ms"] / 1000:+.3f} s</td>'
            f'<td data-label="Relative change">{row["relative_pct"]:+.2f}%</td></tr>'
        )
    treatment_rows = []
    for row in summary["same_format_treatment_deltas"]:
        treatment_rows.append(
            f'<tr><th scope="row">{html.escape("Clava-JS" if row["suite"] == "clava-js" else "Java")}</th>'
            f'<td data-label="Protocol">{html.escape("Text" if row["protocol"] == "text" else "Proto")}</td>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Prefix minus control">{row["delta_ms"] / 1000:+.3f} s</td>'
            f'<td data-label="Relative change">{row["relative_pct"]:+.2f}%</td></tr>'
        )
    did_rows = []
    for row in summary["difference_in_differences"]:
        did_rows.append(
            f'<tr><th scope="row">{html.escape("Clava-JS" if row["suite"] == "clava-js" else "Java")}</th>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Control Proto minus Text">{(row["control_protobuf_ms"] - row["control_text_ms"]) / 1000:+.3f} s</td>'
            f'<td data-label="Prefix Proto minus Text">{(row["prefix_protobuf_ms"] - row["prefix_text_ms"]) / 1000:+.3f} s</td>'
            f'<td data-label="Difference in differences">{row["difference_in_differences_ms"] / 1000:+.3f} s</td></tr>'
        )
    key_rows = []
    for row in summary["key_group_rounds"]:
        key_rows.append(
            f'<tr><th scope="row">{html.escape(row["input_id"])}</th>'
            f'<td data-label="Input">{html.escape(row["source_label"])}</td>'
            f'<td data-label="Round">{row["repeat"]}</td>'
            f'<td data-label="Control Text">{row["control_text_ms"]:.2f} ms</td>'
            f'<td data-label="Control Proto">{row["control_protobuf_ms"]:.2f} ms</td>'
            f'<td data-label="Prefix Text">{row["prefix_text_ms"]:.2f} ms</td>'
            f'<td data-label="Prefix Proto">{row["prefix_protobuf_ms"]:.2f} ms</td></tr>'
        )
    fragment = f'''<section class="warmup-intervention" aria-labelledby="wi-title">
<style>
section.warmup-intervention{{max-width:980px;margin:28px auto;color:inherit}}
section.warmup-intervention *{{box-sizing:border-box}}
section.warmup-intervention h2{{font-size:clamp(20px,3vw,26px);line-height:1.2;margin:20px 0 8px}}
section.warmup-intervention h3{{font-size:18px;margin:18px 0 7px}}
section.warmup-intervention p{{line-height:1.45}}
section.warmup-intervention .wi-note{{color:var(--muted,#536174);font-size:14px}}
section.warmup-intervention .wi-grid{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}}
section.warmup-intervention .wi-figure{{min-width:0;margin:0;padding:8px;border:1px solid var(--line,#d5dfe8);border-radius:8px;background:var(--panel,#f4f7fa)}}
section.warmup-intervention figcaption{{font-size:15px;font-weight:650;margin:0 0 3px}}
section.warmup-intervention .wi-chart{{display:block;width:100%;height:auto;max-width:360px;margin:0 auto}}
section.warmup-intervention svg text{{fill:currentColor;font:15px system-ui,sans-serif}}
section.warmup-intervention .wi-grid{{stroke:var(--line,#d5dfe8);stroke-dasharray:3 4}}
section.warmup-intervention .wi-separator{{stroke:var(--line,#d5dfe8)}}
section.warmup-intervention .wi-table{{width:100%;table-layout:fixed;border-collapse:collapse}}
section.warmup-intervention th,section.warmup-intervention td{{padding:6px 8px;text-align:left;vertical-align:top;border-bottom:1px solid var(--line,#d5dfe8);overflow-wrap:anywhere}}
section.warmup-intervention th[scope="col"]{{font-size:12px;color:var(--muted,#536174)}}
@media(max-width:700px){{section.warmup-intervention .wi-grid{{grid-template-columns:1fr}}}}
@media(max-width:560px){{section.warmup-intervention .wi-table,section.warmup-intervention .wi-table tbody,section.warmup-intervention .wi-table tr,section.warmup-intervention .wi-table th,section.warmup-intervention .wi-table td{{display:block;width:100%}}section.warmup-intervention .wi-table thead{{display:none}}section.warmup-intervention .wi-table tr{{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));padding:5px 0;border-bottom:1px solid var(--line,#d5dfe8)}}section.warmup-intervention .wi-table th[scope="row"]{{grid-column:1/-1;border:0}}section.warmup-intervention .wi-table td{{border:0;min-width:0}}section.warmup-intervention .wi-table td::before{{content:attr(data-label);display:block;color:var(--muted,#536174);font-size:12px;font-weight:650;margin-bottom:2px}}}}
</style>
<h2 id="wi-title">Warm-cache prefix intervention</h2>
<p>Two rounds, separate from the six-round primary. These unprofiled runs use warm ccache and raw protocol output, with eight NAS groups placed before each measured batch. The measured calls stay in the same order. Totals include only the 300 Clava-JS or 216 Java measured groups, not prefix calls. Values sum outer CodeParser.parse wall time, not reader-only time. No explicit or forced GC; automatic JVM GC remains allowed.</p>
<p><a href="warmup-intervention-rounds.csv" download>Download paired round totals (CSV)</a> · <a href="warmup-intervention-groups.csv" download>Download paired group timings and labels (CSV)</a></p>
<div class="wi-grid">{charts}</div>
<details><summary>Per-cell totals, paired deltas, and selected input timings</summary>
<h3>Summed parse time by suite, treatment, protocol, and round</h3>
<div class="wi-table-wrap"><table class="wi-table"><thead><tr><th scope="col">Suite</th><th scope="col">Condition</th><th scope="col">Round</th><th scope="col">Groups</th><th scope="col">Sum</th></tr></thead><tbody>{''.join(total_rows)}</tbody></table></div>
<h3>Paired format delta, Proto minus Text</h3>
<div class="wi-table-wrap"><table class="wi-table"><thead><tr><th scope="col">Suite</th><th scope="col">Treatment</th><th scope="col">Round</th><th scope="col">Delta</th><th scope="col">Change</th></tr></thead><tbody>{''.join(format_rows)}</tbody></table></div>
<h3>Same-format treatment delta, prefix minus no prefix</h3>
<div class="wi-table-wrap"><table class="wi-table"><thead><tr><th scope="col">Suite</th><th scope="col">Protocol</th><th scope="col">Round</th><th scope="col">Delta</th><th scope="col">Change</th></tr></thead><tbody>{''.join(treatment_rows)}</tbody></table></div>
<h3>Difference in differences, format-gap change after prefix</h3>
<div class="wi-table-wrap"><table class="wi-table"><thead><tr><th scope="col">Suite</th><th scope="col">Round</th><th scope="col">Control Proto minus Text</th><th scope="col">Prefix Proto minus Text</th><th scope="col">Change in gap</th></tr></thead><tbody>{''.join(did_rows)}</tbody></table></div>
<h3>Selected input groups, each round shown separately</h3>
<div class="wi-table-wrap"><table class="wi-table"><thead><tr><th scope="col">Group</th><th scope="col">Input</th><th scope="col">Round</th><th scope="col">Control Text</th><th scope="col">Control Proto</th><th scope="col">Prefix Text</th><th scope="col">Prefix Proto</th></tr></thead><tbody>{''.join(key_rows)}</tbody></table></div>
</details>
</section>'''
    if any(marker in fragment for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("HTML fragment contains a local path")
    return fragment


def write_intervention_outputs(summary: dict[str, Any], output_dir: Path) -> dict[str, Path]:
    """Write report-ready outputs only for validated measured-run input files."""
    if summary.get("evidence_label") != "validated measured intervention files":
        raise AnalysisError("refusing to write synthetic or unvalidated timings as report data")
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    json_text = json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if any(marker in json_text for marker in ("/home/", "/private/", "/tmp/", "file://")):
        raise AnalysisError("summary JSON contains a local path")
    html_text = render_warmup_intervention_html(summary)
    json_path = output_dir / "warmup-intervention-analysis.json"
    html_path = output_dir / "warmup-intervention-review.html"
    round_csv_path = output_dir / "warmup-intervention-rounds.csv"
    group_csv_path = output_dir / "warmup-intervention-groups.csv"
    json_path.write_text(json_text, encoding="utf-8")
    html_path.write_text(html_text, encoding="utf-8")

    total_index = {
        (row["treatment"], row["suite"], row["protocol"], row["repeat"]): row
        for row in summary["cell_totals"]
    }
    round_fields = (
        "suite", "repeat", "group_count", "control_text_s", "control_protobuf_s",
        "prefix_text_s", "prefix_protobuf_s", "control_format_delta_s",
        "prefix_format_delta_s", "text_treatment_delta_s",
        "protobuf_treatment_delta_s", "difference_in_differences_s",
    )
    with round_csv_path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=round_fields)
        writer.writeheader()
        did_index = {(row["suite"], row["repeat"]): row
                     for row in summary["difference_in_differences"]}
        for suite in SUITES:
            for repeat in REPEATS:
                get_seconds = lambda treatment, protocol: (
                    total_index[(treatment, suite, protocol, repeat)]["sum_elapsed_s"]
                )
                control_text = get_seconds("control", "text")
                control_proto = get_seconds("control", "protobuf")
                prefix_text = get_seconds("prefix-java-nas", "text")
                prefix_proto = get_seconds("prefix-java-nas", "protobuf")
                writer.writerow({
                    "suite": suite,
                    "repeat": repeat,
                    "group_count": summary["group_counts"][suite],
                    "control_text_s": f"{control_text:.9f}",
                    "control_protobuf_s": f"{control_proto:.9f}",
                    "prefix_text_s": f"{prefix_text:.9f}",
                    "prefix_protobuf_s": f"{prefix_proto:.9f}",
                    "control_format_delta_s": f"{control_proto - control_text:.9f}",
                    "prefix_format_delta_s": f"{prefix_proto - prefix_text:.9f}",
                    "text_treatment_delta_s": f"{prefix_text - control_text:.9f}",
                    "protobuf_treatment_delta_s": f"{prefix_proto - control_proto:.9f}",
                    "difference_in_differences_s": (
                        f"{did_index[(suite, repeat)]['difference_in_differences_ms'] / 1000:.9f}"
                    ),
                })

    group_index = {
        (row["treatment"], row["suite"], row["protocol"], row["repeat"], row["input_id"]): row
        for row in summary["group_rounds"]
    }
    group_fields = (
        "suite", "input_id", "source_label", "source_count", "source_sha256", "repeat",
        "control_text_ms", "control_protobuf_ms", "prefix_text_ms", "prefix_protobuf_ms",
        "control_format_delta_ms", "prefix_format_delta_ms",
        "text_treatment_delta_ms", "protobuf_treatment_delta_ms",
        "difference_in_differences_ms",
    )
    with group_csv_path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=group_fields)
        writer.writeheader()
        for suite in SUITES:
            for input_id in _group_ids(suite, summary["group_counts"][suite]):
                for repeat in REPEATS:
                    get_ms = lambda treatment, protocol: group_index[(
                        treatment, suite, protocol, repeat, input_id
                    )]["elapsed_ms"]
                    control_text = get_ms("control", "text")
                    control_proto = get_ms("control", "protobuf")
                    prefix_text = get_ms("prefix-java-nas", "text")
                    prefix_proto = get_ms("prefix-java-nas", "protobuf")
                    source = group_index[("control", suite, "text", repeat, input_id)]
                    writer.writerow({
                        "suite": suite,
                        "input_id": input_id,
                        "source_label": source["source_label"],
                        "source_count": source["source_count"],
                        "source_sha256": source["source_sha256"],
                        "repeat": repeat,
                        "control_text_ms": f"{control_text:.6f}",
                        "control_protobuf_ms": f"{control_proto:.6f}",
                        "prefix_text_ms": f"{prefix_text:.6f}",
                        "prefix_protobuf_ms": f"{prefix_proto:.6f}",
                        "control_format_delta_ms": f"{control_proto - control_text:.6f}",
                        "prefix_format_delta_ms": f"{prefix_proto - prefix_text:.6f}",
                        "text_treatment_delta_ms": f"{prefix_text - control_text:.6f}",
                        "protobuf_treatment_delta_ms": f"{prefix_proto - control_proto:.6f}",
                        "difference_in_differences_ms": f"{(prefix_proto - control_proto) - (prefix_text - control_text):.6f}",
                    })

    for path in (round_csv_path, group_csv_path):
        if any(marker in path.read_text(encoding="utf-8")
               for marker in ("/home/", "/private/", "/tmp/", "file://")):
            raise AnalysisError(f"{path.name} contains a local path")
    return {"summary": json_path, "html": html_path,
            "rounds_csv": round_csv_path, "groups_csv": group_csv_path}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args(argv)
    summary = analyze_warmup_intervention(args.run_root)
    paths = write_intervention_outputs(summary, args.output_dir)
    print(json.dumps({name: str(path) for name, path in paths.items()}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
