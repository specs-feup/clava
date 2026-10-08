#!/usr/bin/env python3
"""Validate and summarize the six-round Java heap-limit intervention."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import statistics
from typing import Any


PROTOCOLS = ("text", "protobuf")
TREATMENTS = ("default", "xmx512m")
REPEATS = tuple(range(1, 7))
EXPECTED_GROUP_IDS = [f"java-group-{ordinal:04d}" for ordinal in range(1, 217)]
EXPECTED_CACHE_HITS = 208
HASH_FIELDS = (
    "runner_class_sha256", "overlay_class_manifest_sha256", "runtime_jar_manifest_sha256",
    "reader_class_sha256", "compat_proto_types_sha256", "native_tool_sha256",
)


class HeapAnalysisError(ValueError):
    """Raised when a heap run or paired attribution fails validation."""


def _json(path: Path) -> dict[str, Any]:
    try:
        result = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise HeapAnalysisError(f"cannot read {path.name}: {error}") from error
    if not isinstance(result, dict):
        raise HeapAnalysisError(f"{path.name} must contain an object")
    return result


def _jsonl(path: Path) -> list[dict[str, Any]]:
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        raise HeapAnalysisError(f"cannot read {path.name}: {error}") from error
    result = []
    for line_number, line in enumerate(lines, 1):
        try:
            row = json.loads(line)
        except json.JSONDecodeError as error:
            raise HeapAnalysisError(f"{path.name}:{line_number}: malformed JSON") from error
        if not isinstance(row, dict):
            raise HeapAnalysisError(f"{path.name}:{line_number}: expected object")
        result.append(row)
    return result


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as content:
        for chunk in iter(lambda: content.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _cell_name(protocol: str, treatment: str, repeat: int) -> str:
    return f"java-{protocol}-{treatment}-warm-r{repeat:02d}"


def _counter(output: str, section: str, label: str, name: str) -> int:
    pieces = output.split("Local storage:")
    if len(pieces) != 2:
        raise HeapAnalysisError(f"{name}: expected one Local storage section")
    matches = re.findall(rf"^\s*{re.escape(label)}:\s*([\d,]+)", pieces[section], re.MULTILINE)
    if len(matches) != 1:
        raise HeapAnalysisError(f"{name}: expected one {label} counter in section {section}")
    return int(matches[0].replace(",", ""))


def _normalized_argv(argv: list[str], heap: str) -> tuple[str, ...]:
    result = []
    index = 0
    while index < len(argv):
        argument = argv[index]
        if argument == "-Xmx512m":
            if heap != "xmx512m":
                raise HeapAnalysisError("default JVM unexpectedly has -Xmx512m")
            index += 1
            continue
        if argument.startswith("-Xmx"):
            raise HeapAnalysisError(f"unexpected JVM heap option {argument}")
        if argument.startswith("-Djava.io.tmpdir="):
            result.append("-Djava.io.tmpdir=<per-cell>")
        elif argument == "--schedule" or argument == "--output":
            result.append(argument)
            result.append("<per-cell>")
            index += 1
        else:
            result.append(argument)
        index += 1
    if heap == "default" and any(arg == "-Xmx512m" for arg in argv):
        raise HeapAnalysisError("default JVM unexpectedly has -Xmx512m")
    return tuple(result)


def _validate_proof(
    root: Path, *, name: str, manifest: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], dict[str, Any]]:
    manifest_path = root / "run-manifests" / f"run-{name}.json"
    schedule_path = root / "run-schedules" / f"{name}.jsonl"
    observation_path = root / "observations" / f"{name}.jsonl"
    proof_path = root / "warm-counters" / f"counterproof-{name}.jsonl"
    expected_paths = {
        "executed_schedule_path": schedule_path,
        "observation_path": observation_path,
        "runner_output_path": observation_path,
        "counterproof_path": proof_path,
    }
    for field, path in expected_paths.items():
        value = manifest.get(field)
        if not isinstance(value, str) or Path(value).name != path.name:
            raise HeapAnalysisError(f"{name}: {field} does not name {path.name}")
    for field, path in (
        ("executed_schedule_sha256", schedule_path),
        ("observation_sha256", observation_path),
        ("runner_output_sha256", observation_path),
        ("counterproof_sha256", proof_path),
    ):
        if manifest.get(field) != _sha256(path):
            raise HeapAnalysisError(f"{name}: {field} does not match {path.name}")
    source_schedule_path = Path(manifest.get("schedule_path", ""))
    if not source_schedule_path.is_file() or manifest.get("schedule_sha256") != _sha256(source_schedule_path):
        raise HeapAnalysisError(f"{name}: source schedule SHA-256 does not match")

    observations = _jsonl(observation_path)
    schedule = _jsonl(schedule_path)
    proof = _jsonl(proof_path)
    if len(observations) != 216 or len(schedule) != 216:
        raise HeapAnalysisError(f"{name}: expected 216 scheduled and observed rows")
    if [row.get("input_id") for row in schedule] != EXPECTED_GROUP_IDS:
        raise HeapAnalysisError(f"{name}: schedule does not contain the 216 stable Java IDs in order")
    obs_by_id = {row.get("input_id"): row for row in observations}
    if len(obs_by_id) != 216 or set(obs_by_id) != set(EXPECTED_GROUP_IDS):
        raise HeapAnalysisError(f"{name}: observation IDs are missing or duplicated")
    for scheduled in schedule:
        input_id = scheduled["input_id"]
        observed = obs_by_id[input_id]
        if (observed.get("record_type") != "parse" or observed.get("phase") != "measure"
                or observed.get("valid") is not True or observed.get("protocol") != manifest["protocol"]
                or observed.get("repeat") != manifest["repeat"]):
            raise HeapAnalysisError(f"{name}/{input_id}: observation is invalid or misattributed")
        for field in ("event_id", "source_sha256", "args_sha256", "options_sha256"):
            if not scheduled.get(field) or observed.get(field) != scheduled[field]:
                raise HeapAnalysisError(f"{name}/{input_id}: {field} differs from schedule")
        if "group_id" in observed and observed["group_id"] != scheduled.get("group_id"):
            raise HeapAnalysisError(f"{name}/{input_id}: group_id differs from schedule")
        elapsed = observed.get("elapsed_ms")
        if isinstance(elapsed, bool) or not isinstance(elapsed, (int, float)) or not math.isfinite(elapsed) or elapsed <= 0:
            raise HeapAnalysisError(f"{name}/{input_id}: elapsed_ms is invalid")

    if len(proof) != 2 or [row.get("operation") for row in proof] != ["ccache_zero", "ccache_stats"]:
        raise HeapAnalysisError(f"{name}: expected ccache reset then stats proof")
    for row in proof:
        if (row.get("record_type") != "ccache" or row.get("input_id") != "__ccache__"
                or row.get("valid") is not True or row.get("command_status") != 0
                or row.get("suite") != "java" or row.get("protocol") != manifest["protocol"]
                or row.get("repeat") != manifest["repeat"]):
            raise HeapAnalysisError(f"{name}: ccache proof attribution/status is invalid")
    if "Statistics zeroed" not in proof[0].get("output", ""):
        raise HeapAnalysisError(f"{name}: ccache reset is not confirmed")
    stats = proof[1].get("output")
    if not isinstance(stats, str):
        raise HeapAnalysisError(f"{name}: ccache stats output is missing")
    counters = {
        "cacheable_calls": _counter(stats, 0, "Cacheable calls", name),
        "hits": _counter(stats, 0, "Hits", name),
        "misses": _counter(stats, 0, "Misses", name),
    }
    if (counters != {"cacheable_calls": EXPECTED_CACHE_HITS, "hits": EXPECTED_CACHE_HITS, "misses": 0}
            or _counter(stats, 1, "Hits", name) != EXPECTED_CACHE_HITS
            or _counter(stats, 1, "Misses", name) != 0
            or manifest.get("ccache_counters") != counters
            or manifest.get("expected_eligible_hits") != EXPECTED_CACHE_HITS):
        raise HeapAnalysisError(f"{name}: ccache proof is not 208 hits and zero misses")
    return schedule, observations, counters


def analyze_java_heap_intervention(initial_root: Path, extension_root: Path) -> dict[str, Any]:
    """Validate 24 cells from rounds 1–2 and 3–6, then compute paired deltas."""
    roots_by_round = {
        **{repeat: Path(initial_root) for repeat in (1, 2)},
        **{repeat: Path(extension_root) for repeat in (3, 4, 5, 6)},
    }
    cells: dict[tuple[str, str, int], dict[str, Any]] = {}
    manifests: dict[tuple[str, str, int], dict[str, Any]] = {}
    source_identity: dict[str, tuple[str, str, str, str]] = {}
    class_hashes: dict[str, str] = {}
    argv_templates: dict[str, tuple[str, ...]] = {}
    schedule_sha_per_format_round: dict[tuple[str, int], str] = {}
    reset_root_counts = {Path(initial_root): 0, Path(extension_root): 0}

    expected_names = set()
    for protocol in PROTOCOLS:
        for treatment in TREATMENTS:
            for repeat in REPEATS:
                root = roots_by_round[repeat]
                name = _cell_name(protocol, treatment, repeat)
                expected_names.add(name)
                manifest_path = root / "run-manifests" / f"run-{name}.json"
                manifest = _json(manifest_path)
                expected_phase = "heap" if repeat <= 2 else "measure"
                expected_heap = None if treatment == "default" else "512m"
                if (manifest.get("name") != name or manifest.get("suite") != "java"
                        or manifest.get("protocol") != protocol or manifest.get("treatment") != treatment
                        or manifest.get("repeat") != repeat or manifest.get("phase") != expected_phase
                        or manifest.get("valid") is not True or manifest.get("exit_code") != 0
                        or manifest.get("profiled") is not False or manifest.get("heap_limit") != expected_heap):
                    raise HeapAnalysisError(f"{name}: manifest cell attribution/status/heap limit is invalid")
                if manifest.get("runner_has_no_explicit_gc_call") is not True or manifest.get("show_exec_info") is not False:
                    raise HeapAnalysisError(f"{name}: GC or SHOW_EXEC_INFO policy proof is invalid")
                if manifest.get("explicit_gc_policy_flags") != [] or manifest.get("java_agent") != "none":
                    raise HeapAnalysisError(f"{name}: unexpected GC option or Java agent")
                env_policy = manifest.get("env_policy", {})
                if (env_policy.get("automatic_gc_allowed") is not True
                        or env_policy.get("ccache_disable") != "unset"
                        or env_policy.get("forced_gc_policy_flags") != []
                        or env_policy.get("java_agent") != "none"
                        or env_policy.get("runner_has_no_explicit_gc_call") is not True
                        or env_policy.get("show_exec_info") is not False):
                    raise HeapAnalysisError(f"{name}: environment policy does not prove non-profiled/no-forced-GC run")
                for field in HASH_FIELDS:
                    value = manifest.get(field)
                    if not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{64}", value):
                        raise HeapAnalysisError(f"{name}: {field} is missing or invalid")
                    prior = class_hashes.setdefault(field, value)
                    if prior != value:
                        raise HeapAnalysisError(f"{name}: {field} differs across the six-round run set")

                argv = manifest.get("java_argv")
                if not isinstance(argv, list) or not argv or not all(isinstance(arg, str) for arg in argv):
                    raise HeapAnalysisError(f"{name}: java_argv is missing")
                if (argv.count("-Xmx512m") != (1 if treatment == "xmx512m" else 0)
                        or any(re.search(r"-XX:[^\s]*gc", arg, re.IGNORECASE) for arg in argv)):
                    raise HeapAnalysisError(f"{name}: JVM argv does not isolate only -Xmx512m")
                template = _normalized_argv(argv, treatment)
                previous_template = argv_templates.setdefault(treatment, template)
                if previous_template != template:
                    raise HeapAnalysisError(f"{name}: non-heap JVM argv differs across cells")

                schedule, observations, counters = _validate_proof(root, name=name, manifest=manifest)
                schedule_sha = manifest["schedule_sha256"]
                schedule_key = (protocol, repeat)
                prior_sha = schedule_sha_per_format_round.setdefault(schedule_key, schedule_sha)
                if prior_sha != schedule_sha:
                    raise HeapAnalysisError(f"{name}: default and Xmx source schedules differ")
                for row in schedule:
                    input_id = row["input_id"]
                    identity = tuple(str(row.get(field)) for field in (
                        "event_id", "source_sha256", "args_sha256", "options_sha256"
                    ))
                    prior_identity = source_identity.setdefault(input_id, identity)
                    if prior_identity != identity:
                        raise HeapAnalysisError(f"{name}/{input_id}: source/event/options identity changes")
                total_s = math.fsum(float(row["elapsed_ms"]) for row in observations) / 1000.0
                cells[(protocol, treatment, repeat)] = {
                    "protocol": protocol, "treatment": treatment, "repeat": repeat,
                    "group_count": len(observations), "sum_elapsed_s": total_s,
                    "cacheable_calls": counters["cacheable_calls"],
                    "cache_hits": counters["hits"], "cache_misses": counters["misses"],
                }
                manifests[(protocol, treatment, repeat)] = manifest
                reset_root_counts[root] += 1

    expected_count_by_root = {Path(initial_root): 8, Path(extension_root): 16}
    if reset_root_counts != expected_count_by_root:
        raise HeapAnalysisError("not all 8 initial and 16 extension manifests were consumed exactly once")
    if len(cells) != 24 or len(source_identity) != 216:
        raise HeapAnalysisError("the complete 24-cell, 216-input paired matrix is not present")

    rounds = []
    deltas = {"text": [], "protobuf": [], "difference_in_differences": []}
    for repeat in REPEATS:
        default_text = cells[("text", "default", repeat)]["sum_elapsed_s"]
        default_proto = cells[("protobuf", "default", repeat)]["sum_elapsed_s"]
        xmx_text = cells[("text", "xmx512m", repeat)]["sum_elapsed_s"]
        xmx_proto = cells[("protobuf", "xmx512m", repeat)]["sum_elapsed_s"]
        text_delta = xmx_text - default_text
        proto_delta = xmx_proto - default_proto
        default_gap = default_proto - default_text
        xmx_gap = xmx_proto - xmx_text
        did = xmx_gap - default_gap
        row = {
            "repeat": repeat,
            "default_text_s": default_text,
            "default_protobuf_s": default_proto,
            "xmx512m_text_s": xmx_text,
            "xmx512m_protobuf_s": xmx_proto,
            "text_xmx_minus_default_s": text_delta,
            "protobuf_xmx_minus_default_s": proto_delta,
            "default_format_gap_proto_minus_text_s": default_gap,
            "xmx512m_format_gap_proto_minus_text_s": xmx_gap,
            "heap_format_gap_difference_in_differences_s": did,
        }
        rounds.append(row)
        deltas["text"].append(text_delta)
        deltas["protobuf"].append(proto_delta)
        deltas["difference_in_differences"].append(did)

    summaries = {}
    for name, values in deltas.items():
        summaries[name] = {
            "round_count": len(values),
            "round_deltas_s": values,
            "median_paired_delta_s": statistics.median(values),
            "min_paired_delta_s": min(values),
            "max_paired_delta_s": max(values),
        }
    return {
        "schema_version": 1,
        "evidence_label": "validated measured six-round Java heap intervention",
        "comparison": "Java default heap versus -Xmx512m; six matched rounds; one JVM per suite/format/heap/round cell",
        "timing_boundary": "outer CodeParser.parse(List<File>, compiler_options)",
        "protocols": list(PROTOCOLS),
        "treatments": list(TREATMENTS),
        "repeat_count": len(REPEATS),
        "groups_per_cell": 216,
        "validation": {
            "cells": len(cells),
            "observations": len(cells) * 216,
            "unique_stable_input_identities": len(source_identity),
            "eligible_calls_per_cell": EXPECTED_CACHE_HITS,
            "all_cache_hits": all(row["cache_hits"] == EXPECTED_CACHE_HITS for row in cells.values()),
            "all_cache_misses_zero": all(row["cache_misses"] == 0 for row in cells.values()),
            "same_runner_runtime_reader_schema_and_native_hashes": True,
            "only_added_jvm_heap_option": "-Xmx512m",
            "profiled_cells": 0,
            "explicit_gc_policy_flags": [],
            "automatic_gc_allowed": True,
            "class_hashes": class_hashes,
        },
        "rounds": rounds,
        "paired_delta_summaries": summaries,
        "cell_totals": [cells[key] for key in sorted(cells)],
    }


def write_outputs(summary: dict[str, Any], output_dir: Path) -> tuple[Path, Path]:
    if summary.get("evidence_label") != "validated measured six-round Java heap intervention":
        raise HeapAnalysisError("refusing to write unvalidated heap measurements")
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / "java-heap-intervention-analysis.json"
    csv_path = output_dir / "java-heap-intervention-rounds.csv"
    json_text = json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if any(marker in json_text for marker in ("/home/", "/tmp/", "file://")):
        raise HeapAnalysisError("summary contains a local path")
    json_path.write_text(json_text, encoding="utf-8")
    fields = (
        "repeat", "default_text_s", "default_protobuf_s", "xmx512m_text_s",
        "xmx512m_protobuf_s", "text_xmx_minus_default_s", "protobuf_xmx_minus_default_s",
        "default_format_gap_proto_minus_text_s", "xmx512m_format_gap_proto_minus_text_s",
        "heap_format_gap_difference_in_differences_s",
    )
    with csv_path.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        for row in summary["rounds"]:
            writer.writerow({key: (row[key] if key == "repeat" else f"{row[key]:.9f}")
                             for key in fields})
    csv_text = csv_path.read_text(encoding="utf-8")
    if any(marker in csv_text for marker in ("/home/", "/tmp/", "file://")):
        raise HeapAnalysisError("CSV contains a local path")
    return json_path, csv_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--initial-root", type=Path, required=True)
    parser.add_argument("--extension-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args(argv)
    summary = analyze_java_heap_intervention(args.initial_root, args.extension_root)
    json_path, csv_path = write_outputs(summary, args.output_dir)
    print(json.dumps({"summary": str(json_path), "rounds_csv": str(csv_path)}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
